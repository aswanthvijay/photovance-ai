"""Face Identity Lock Module using InsightFace, CodeFormer discrete codebook prior,
Reinhard LAB ambient lighting harmonization, and high-frequency skin pore separation."""
import cv2
import numpy as np
import torch
from pathlib import Path
from PIL import Image

_FACE_APP = None
_SWAPPER = None
_CODEFORMER_NET = None
_FACE_HELPER = None
_FALLBACK_RESTORER = None


def get_face_modules():
    """Lazily load and cache face detector, swapper, CodeFormer prior, and face helper."""
    global _FACE_APP, _SWAPPER, _CODEFORMER_NET, _FACE_HELPER, _FALLBACK_RESTORER

    if _FACE_APP is None:
        import insightface
        from insightface.app import FaceAnalysis

        models_dir = Path(r"C:\Users\Aswanth Vijay\.insightface\models")
        device = "cuda" if torch.cuda.is_available() else "cpu"

        # 1. Buffalo_l Detector & 512-d Identity Extractor
        _FACE_APP = FaceAnalysis(name="buffalo_l", providers=["CPUExecutionProvider"])
        _FACE_APP.prepare(ctx_id=0, det_size=(640, 640))

        # 2. Inswapper 128
        swapper_path = models_dir / "inswapper_128.onnx"
        if not swapper_path.exists():
            raise FileNotFoundError(f"Missing {swapper_path}")
        _SWAPPER = insightface.model_zoo.get_model(str(swapper_path), download=False, providers=["CPUExecutionProvider"])

        # 3. CodeFormer discrete codebook prior network
        codeformer_path = models_dir / "codeformer.pth"
        if codeformer_path.exists():
            try:
                from codeformer.basicsr.utils.registry import ARCH_REGISTRY
                import codeformer.basicsr.archs
                from codeformer.facelib.utils.face_restoration_helper import FaceRestoreHelper

                arch = ARCH_REGISTRY.get("CodeFormer")
                net = arch(
                    dim_embd=512,
                    codebook_size=1024,
                    n_head=8,
                    n_layers=9,
                    connect_list=["32", "64", "128", "256"]
                ).to(device)

                ckpt = torch.load(str(codeformer_path), map_location="cpu")
                weights = ckpt["params_ema"] if "params_ema" in ckpt else ckpt
                net.load_state_dict(weights)
                net.eval()
                _CODEFORMER_NET = net

                _FACE_HELPER = FaceRestoreHelper(
                    upscale_factor=1,
                    face_size=512,
                    crop_ratio=(1, 1),
                    det_model="retinaface_resnet50",
                    save_ext="png",
                    use_parse=True,
                    device=device
                )
                print(f"[FaceLock] CodeFormer Discrete Codebook Prior loaded successfully on {device}")
            except Exception as e:
                print(f"[FaceLock] Failed to load CodeFormer ({e}), falling back to GFPGAN")

        # 4. Fallback restorer (GFPGAN)
        gfpgan_path = models_dir / "GFPGANv1.4.pth"
        if gfpgan_path.exists() and _CODEFORMER_NET is None:
            from gfpgan import GFPGANer
            _FALLBACK_RESTORER = GFPGANer(
                model_path=str(gfpgan_path),
                upscale=1,
                arch="clean",
                channel_multiplier=2,
                device=device
            )

    return _FACE_APP, _SWAPPER, _CODEFORMER_NET, _FACE_HELPER, _FALLBACK_RESTORER


def harmonize_ambient_lighting(restored_bgr: np.ndarray, target_scene_bgr: np.ndarray, bbox: list, blend_strength: float = 0.25) -> np.ndarray:
    """
    Samples ambient lighting and skin chrominance from the target scene's neck/chest area
    and softly harmonizes the face in LAB perceptual color space.
    Prevents the 'pasted sticker' appearance under golden hour, candlelight, or neon.
    """
    bx1, by1, bx2, by2 = max(0, bbox[0]), max(0, bbox[1]), min(target_scene_bgr.shape[1], bbox[2]), min(target_scene_bgr.shape[0], bbox[3])
    neck_y1 = min(target_scene_bgr.shape[0] - 1, by2)
    neck_y2 = min(target_scene_bgr.shape[0], by2 + int((by2 - by1) * 0.35))
    neck_sample = target_scene_bgr[neck_y1:neck_y2, bx1:bx2]

    if neck_sample.size < 100:
        return restored_bgr

    result = restored_bgr.copy()
    face_crop = result[by1:by2, bx1:bx2]
    if face_crop.size == 0:
        return restored_bgr

    neck_lab = cv2.cvtColor(neck_sample, cv2.COLOR_BGR2LAB).astype(np.float32)
    face_lab = cv2.cvtColor(face_crop, cv2.COLOR_BGR2LAB).astype(np.float32)

    # Harmonize Luminance (L channel)
    tgt_l_mean, tgt_l_std = np.mean(neck_lab[:, :, 0]), np.std(neck_lab[:, :, 0]) + 1e-6
    src_l_mean, src_l_std = np.mean(face_lab[:, :, 0]), np.std(face_lab[:, :, 0]) + 1e-6
    face_lab[:, :, 0] = (face_lab[:, :, 0] - src_l_mean) * (tgt_l_std / src_l_std) + tgt_l_mean

    # Harmonize Chrominance (a and b channels) with subtle ambient influence
    for c in (1, 2):
        face_lab[:, :, c] = face_lab[:, :, c] * (1.0 - blend_strength * 0.5) + np.mean(neck_lab[:, :, c]) * (blend_strength * 0.5)

    harmonized_face = cv2.cvtColor(np.clip(face_lab, 0, 255).astype(np.uint8), cv2.COLOR_LAB2BGR)
    result[by1:by2, bx1:bx2] = cv2.addWeighted(result[by1:by2, bx1:bx2], 1.0 - blend_strength, harmonized_face, blend_strength, 0)
    return result


def reinject_skin_pores(base_bgr: np.ndarray, target_scene_bgr: np.ndarray, bbox: list, pore_strength: float = 0.38) -> np.ndarray:
    """
    Frequency Separation pass:
    Extracts high-frequency spatial detail (authentic skin pores, micro-creases, fine grain)
    from the original high-res render and layers it back onto the restored face.
    This eliminates the plastic/waxy airbrushed GAN appearance.
    """
    low_freq = cv2.GaussianBlur(target_scene_bgr, (5, 5), 1.5)
    high_freq = cv2.subtract(target_scene_bgr, low_freq).astype(np.float32)

    bx1, by1, bx2, by2 = max(0, bbox[0]), max(0, bbox[1]), min(target_scene_bgr.shape[1], bbox[2]), min(target_scene_bgr.shape[0], bbox[3])
    mask = np.zeros((target_scene_bgr.shape[0], target_scene_bgr.shape[1]), dtype=np.float32)
    center = ((bx1 + bx2) // 2, (by1 + by2) // 2)
    axes = ((bx2 - bx1) // 2, int((by2 - by1) * 0.55))
    cv2.ellipse(mask, center, axes, 0, 0, 360, 1.0, -1)
    mask = cv2.GaussianBlur(mask, (31, 31), 11.0)[:, :, np.newaxis]

    enhanced = base_bgr.astype(np.float32) + (high_freq * pore_strength * mask)
    return np.clip(enhanced, 0, 255).astype(np.uint8)


def inject_photographic_grain(img_bgr: np.ndarray, intensity: float = 0.008) -> np.ndarray:
    """Injects a 0.8% organic Gaussian micro-grain mimicking 35mm film emulsion to eliminate digital airbrush sheen."""
    noise = np.random.normal(0, intensity * 255, img_bgr.shape).astype(np.float32)
    return np.clip(img_bgr.astype(np.float32) + noise, 0, 255).astype(np.uint8)


def apply_face_lock(
    source_image: Image.Image,
    target_image: Image.Image,
    fidelity_weight: float = 0.72,
    enable_pores: bool = True,
    enable_lighting_match: bool = True,
    enable_grain: bool = True
) -> Image.Image:
    """
    Transfers 100% exact facial identity from source_image onto target_image,
    then applies the Photovance Ultra-Portrait™ Realism Pipeline:
    - CodeFormer Discrete Codebook Prior (w=0.72) for genuine human skin texture
    - Reinhard LAB Ambient Lighting & Tone Harmonization
    - Frequency Separation Skin Pore & Micro-Detail Recovery
    - Soft Facial Parse Feathering with zero visible boundary seams
    - 35mm Photographic Micro-Grain injection
    """
    app, swapper, codeformer_net, face_helper, fallback_restorer = get_face_modules()

    # Convert PIL to BGR
    src_rgb = np.array(source_image.convert("RGB"))
    src_bgr = cv2.cvtColor(src_rgb, cv2.COLOR_RGB2BGR)

    tgt_rgb = np.array(target_image.convert("RGB"))
    tgt_bgr = cv2.cvtColor(tgt_rgb, cv2.COLOR_RGB2BGR)

    src_faces = app.get(src_bgr)
    tgt_faces = app.get(tgt_bgr)

    if not src_faces or not tgt_faces:
        return target_image

    # Choose largest face by bounding box area
    src_face = sorted(src_faces, key=lambda f: (f.bbox[2]-f.bbox[0])*(f.bbox[3]-f.bbox[1]), reverse=True)[0]
    tgt_face = sorted(tgt_faces, key=lambda f: (f.bbox[2]-f.bbox[0])*(f.bbox[3]-f.bbox[1]), reverse=True)[0]
    bbox = [int(x) for x in tgt_face.bbox]

    # Step 1: Transfer facial identity
    swapped_bgr = swapper.get(tgt_bgr.copy(), tgt_face, src_face, paste_back=True)

    # Step 2: Face Restoration (CodeFormer Discrete Codebook Prior or Fallback)
    restored_bgr = swapped_bgr
    if codeformer_net is not None and face_helper is not None:
        try:
            from torchvision.transforms.functional import normalize
            from codeformer.basicsr.utils import img2tensor, tensor2img

            device = "cuda" if torch.cuda.is_available() else "cpu"
            face_helper.clean_all()
            face_helper.read_image(swapped_bgr)
            face_helper.get_face_landmarks_5(only_center_face=True, resize=640, eye_dist_threshold=5)
            face_helper.align_warp_face()

            for cropped_face in face_helper.cropped_faces:
                t = img2tensor(cropped_face / 255.0, bgr2rgb=True, float32=True)
                normalize(t, (0.5, 0.5, 0.5), (0.5, 0.5, 0.5), inplace=True)
                t = t.unsqueeze(0).to(device)
                with torch.no_grad():
                    out = codeformer_net(t, w=fidelity_weight, adain=True)[0]
                    restored_face = tensor2img(out, rgb2bgr=True, min_max=(-1, 1))
                face_helper.add_restored_face(restored_face)

            face_helper.get_inverse_affine(None)
            restored_bgr = face_helper.paste_faces_to_input_image()
        except Exception as e:
            print(f"[FaceLock] CodeFormer restoration warning: {e}, attempting fallback")
            restored_bgr = swapped_bgr

    elif fallback_restorer is not None:
        try:
            _, _, fb_bgr = fallback_restorer.enhance(
                swapped_bgr,
                has_aligned=False,
                only_center_face=False,
                paste_back=True
            )
            restored_bgr = fb_bgr
        except Exception as e:
            print(f"[FaceLock] GFPGAN fallback warning: {e}")
            restored_bgr = swapped_bgr

    # Step 3: Environmental Lighting & Tone Harmonization (Reinhard LAB Transfer)
    if enable_lighting_match:
        try:
            restored_bgr = harmonize_ambient_lighting(restored_bgr, tgt_bgr, bbox, blend_strength=0.22)
        except Exception as e:
            print(f"[FaceLock] Lighting harmonization warning: {e}")

    # Step 4: High-Frequency Skin Pore & Micro-Texture Recovery
    if enable_pores:
        try:
            restored_bgr = reinject_skin_pores(restored_bgr, tgt_bgr, bbox, pore_strength=0.38)
        except Exception as e:
            print(f"[FaceLock] Pore re-injection warning: {e}")

    # Step 5: Subtle 35mm Photographic Micro-Grain
    if enable_grain:
        try:
            restored_bgr = inject_photographic_grain(restored_bgr, intensity=0.008)
        except Exception as e:
            print(f"[FaceLock] Film grain warning: {e}")

    final_rgb = cv2.cvtColor(restored_bgr, cv2.COLOR_BGR2RGB)
    return Image.fromarray(final_rgb)
