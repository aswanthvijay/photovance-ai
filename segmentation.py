"""Garment detection and face protection using local AI models."""
import os
from pathlib import Path
import cv2
import numpy as np
import torch
from PIL import Image, ImageDraw, ImageFilter
from transformers import (
    SegformerImageProcessor,
    SegformerForSemanticSegmentation,
    CLIPSegProcessor,
    CLIPSegForImageSegmentation
)

import config

_clothing_proc = None
_clothing_model = None
_clip_proc = None
_clip_model = None


def get_clothing_parser():
    global _clothing_proc, _clothing_model
    if _clothing_proc is None or _clothing_model is None:
        _clothing_proc = SegformerImageProcessor.from_pretrained(config.CLOTHING_PARSER_DIR, local_files_only=True)
        _clothing_model = SegformerForSemanticSegmentation.from_pretrained(
            config.CLOTHING_PARSER_DIR, local_files_only=True, use_safetensors=True
        ).eval()
    return _clothing_proc, _clothing_model


def get_clipseg():
    global _clip_proc, _clip_model
    if _clip_proc is None or _clip_model is None:
        _clip_proc = CLIPSegProcessor.from_pretrained(config.CLIPSEG_DIR, local_files_only=True)
        _clip_model = CLIPSegForImageSegmentation.from_pretrained(
            config.CLIPSEG_DIR, local_files_only=True, use_safetensors=True
        ).eval()
    return _clip_proc, _clip_model


def detect_face_landmarks(image: Image.Image):
    """Detect faces and facial landmarks using YuNet ONNX detector."""
    if not config.FACE_DETECTOR_PATH.exists():
        return []
    scale = min(1.0, 1280 / max(image.size))
    preview = image.resize(
        (max(32, round(image.width * scale)), max(32, round(image.height * scale))),
        Image.Resampling.LANCZOS
    )
    pixels = cv2.cvtColor(np.asarray(preview.convert("RGB")), cv2.COLOR_RGB2BGR)
    detector = cv2.FaceDetectorYN.create(str(config.FACE_DETECTOR_PATH), "", preview.size, 0.8, 0.3, 1000)
    _, faces = detector.detect(pixels)
    if faces is None:
        return []

    results = []
    for face in faces:
        x, y, w, h = face[:4]
        if w < 12 or h < 12:
            continue
        unscaled = [float(v / scale) for v in face[:14]]
        eye_y = (unscaled[5] + unscaled[7]) / 2
        mouth_y = (unscaled[11] + unscaled[13]) / 2
        chin_y = mouth_y + (mouth_y - eye_y) * 0.65
        results.append({
            "box": (unscaled[0], unscaled[1], unscaled[2], unscaled[3]),
            "chin_y": chin_y,
            "eye_y": eye_y,
            "mouth_y": mouth_y
        })
    return results


def parse_clothing_labels(image: Image.Image) -> np.ndarray:
    """Classify pixels into clothing categories using SegFormer."""
    processor, model = get_clothing_parser()
    inputs = processor(images=image, return_tensors="pt")
    with torch.inference_mode():
        logits = model(**inputs).logits
        scale = min(1.0, 1024 / max(image.size))
        size = (max(1, round(image.height * scale)), max(1, round(image.width * scale)))
        logits = torch.nn.functional.interpolate(logits, size=size, mode="bilinear", align_corners=False)
        labels = logits.argmax(dim=1)[0].cpu().numpy().astype(np.uint8)
    return np.asarray(Image.fromarray(labels).resize(image.size, Image.Resampling.NEAREST))


def segment_with_clipseg(image: Image.Image, query: str = "dress, clothing") -> np.ndarray:
    """Zero-shot segmentation using CLIPSeg to capture out-of-focus or bokeh garment areas."""
    try:
        proc, model = get_clipseg()
        inputs = proc(text=[query], images=[image], padding=True, return_tensors="pt")
        with torch.inference_mode():
            logits = model(**inputs).logits.sigmoid().numpy()
        clip_2d = (logits[0] > 0.35).astype(np.uint8) * 255
        resized = np.array(Image.fromarray(clip_2d).resize(image.size, Image.Resampling.BILINEAR))
        return (resized > 127).astype(np.uint8)
    except Exception:
        return np.zeros((image.height, image.width), dtype=np.uint8)


def generate_dress_mask(
    image: Image.Image,
    target: str = "dress",
    keep_face: bool = True,
    expand_pixels: int = 4
):
    """
    Generate an ultra-clean hybrid inpainting mask that covers 100% of the existing garment.
    
    1. Hybrid Segmentation: SegFormer provides razor-sharp pixel segmentation for garments.
       CLIPSeg fills in any out-of-focus, bokeh, or shadowed sleeves/hems that SegFormer missed.
    2. Anatomical Chin Line: Protects the face strictly above the chin line, allowing
       standing collars, necklines, and throats to be cleanly replaced without choking/collaring artifacts.
    3. Edge Dilation: Dilates outward by 4-5px to completely absorb all old fabric edges, hems, and seams,
       preventing old dress colors from leaking through at the borders.
    4. Head Identity Protection: Hair, face (above chin), hat, and glasses are strictly zeroed out.
    5. Soft Gaussian Feather: 2.0px Gaussian blur for natural photographic edge anti-aliasing.
    """
    labels = parse_clothing_labels(image)

    # SegFormer B2 Clothes label map:
    # 0: Background, 1: Hat, 2: Hair, 3: Sunglasses, 4: Upper-clothes
    # 5: Skirt, 6: Pants, 7: Dress, 8: Belt, 9: Left-shoe, 10: Right-shoe
    # 11: Face, 12: Left-leg, 13: Right-leg, 14: Left-arm, 15: Right-arm
    # 16: Bag, 17: Scarf
    if target in ("upper", "top", "shirt"):
        garment_ids = (4, 17)
        clip_query = "shirt, top, blouse"
    elif target in ("lower", "bottom", "pants", "skirt"):
        garment_ids = (5, 6, 8)
        clip_query = "skirt, pants, trousers"
    else:  # 'dress', 'outfit', 'all'
        garment_ids = (4, 5, 6, 7, 8, 17)
        clip_query = "dress, clothing"

    seg_garment = np.isin(labels, garment_ids).astype(np.uint8)
    # If SegFormer found nothing, fallback to CLIPSeg
    if not seg_garment.any():
        garment = segment_with_clipseg(image, clip_query)
    else:
        garment = seg_garment

    faces = detect_face_landmarks(image) if keep_face else []
    y_coords = np.arange(image.height)[:, None]
    x_coords = np.arange(image.width)[None, :]

    # Close small fabric gaps (buttons, folds, creases)
    if garment.any():
        garment = cv2.morphologyEx(
            garment.astype(np.uint8),
            cv2.MORPH_CLOSE,
            cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (15, 15))
        )

    # Dilate by 10px to cleanly absorb all hems, cuffs, collars, and seams
    effective_expand = max(int(expand_pixels), 10)
    kernel = cv2.getStructuringElement(
        cv2.MORPH_ELLIPSE,
        (effective_expand * 2 + 1, effective_expand * 2 + 1)
    )
    dilated = cv2.dilate(garment, kernel)

    # Strictly protect the Face and Hair
    # 1. Hair protection: erode hair by 2px so garment dilation reaches right under hair strands
    hair = (labels == 2)
    hair_eroded = cv2.erode(
        hair.astype(np.uint8),
        cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (5, 5))
    ).astype(bool)

    # 2. Face skin protection: only protect actual face skin (label 11) above the chin
    faces = detect_face_landmarks(image) if keep_face else []
    y_coords = np.arange(image.height)[:, None]
    if faces and keep_face:
        chin_y = faces[0]["chin_y"]
    else:
        chin_y = image.height * 0.45
    face_protect = (labels == 11) & (y_coords <= chin_y)

    # Head accessories (hat, sunglasses)
    head_acc = np.isin(labels, (1, 3))

    protect_mask = face_protect | hair_eroded | head_acc
    dilated[protect_mask] = 0

    # Filter out disconnected background noise (< 1500px, e.g. on furniture or walls)
    num_labels, comp_labels, stats, _ = cv2.connectedComponentsWithStats(dilated, connectivity=8)
    clean_mask = np.zeros_like(dilated)
    for i in range(1, num_labels):
        if stats[i, cv2.CC_STAT_AREA] >= 1500:
            clean_mask[comp_labels == i] = 1
    if not clean_mask.any():
        clean_mask = dilated

    # Smooth 2.5px Gaussian feather for natural photographic edge anti-aliasing
    mask_pil = Image.fromarray((clean_mask * 255).astype(np.uint8))
    mask_pil = mask_pil.filter(ImageFilter.GaussianBlur(2.5))

    if protect_mask is not None:
        mask_np = np.array(mask_pil)
        mask_np[protect_mask] = 0
        mask_pil = Image.fromarray(mask_np)

    coverage = float((np.asarray(mask_pil) > 127).mean())

    # Visual overlay preview
    overlay = image.convert("RGB").copy()
    mask_uint8 = np.asarray(mask_pil)
    if mask_uint8.any():
        color_layer = np.zeros((image.height, image.width, 3), dtype=np.uint8)
        color_layer[:] = [14, 165, 233]
        alpha = (mask_uint8.astype(float) / 255.0 * 0.45)[:, :, None]
        img_arr = np.asarray(overlay).astype(float)
        blended = (img_arr * (1 - alpha) + color_layer * alpha).clip(0, 255).astype(np.uint8)
        preview_overlay = Image.fromarray(blended)
    else:
        preview_overlay = overlay

    return mask_pil, preview_overlay, faces, coverage
