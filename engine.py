"""Local ComfyUI Inpainting and Editing engine connection and execution."""
import io
import json
import math
import random
import subprocess
import time
import uuid
from pathlib import Path
from PIL import Image
import requests

import config

SESSION = requests.Session()
SESSION.trust_env = False


def ensure_backend():
    """Verify backend is running on port 8189, or start it."""
    try:
        res = SESSION.get(f"{config.BACKEND_URL}/system_stats", timeout=3)
        if res.status_code == 200:
            return True
    except requests.RequestException:
        pass

    bootstrap = config.IMAGE_AI_ROOT / "engine_bootstrap.py"
    if not bootstrap.exists():
        raise RuntimeError(f"Engine bootstrap script not found at {bootstrap}")

    for folder in ("engine-input", "engine-output", "engine-user", "engine-temp", "logs"):
        (config.IMAGE_AI_ROOT / folder).mkdir(exist_ok=True)

    log_file = (config.IMAGE_AI_ROOT / "logs" / "engine.log").open("a", encoding="utf-8")
    subprocess.Popen(
        [
            str(config.PYTHON_EXE),
            "-u",
            str(bootstrap),
            "--listen", "127.0.0.1",
            "--port", "8189",
            "--lowvram",
            "--reserve-vram", "0.8",
            "--use-pytorch-cross-attention",
            "--preview-method", "none",
            "--offline",
            "--disable-all-custom-nodes",
            "--disable-auto-launch",
            "--extra-model-paths-config", str(config.WORKSPACE_ROOT / "extra_models.yaml"),
            "--input-directory", str(config.IMAGE_AI_ROOT / "engine-input"),
            "--output-directory", str(config.IMAGE_AI_ROOT / "engine-output"),
            "--user-directory", str(config.IMAGE_AI_ROOT / "engine-user"),
            "--temp-directory", str(config.IMAGE_AI_ROOT / "engine-temp"),
        ],
        cwd=config.COMFYUI_DIR,
        stdin=subprocess.DEVNULL,
        stdout=log_file,
        stderr=subprocess.STDOUT,
        creationflags=subprocess.CREATE_NO_WINDOW | subprocess.CREATE_NEW_PROCESS_GROUP | subprocess.DETACHED_PROCESS,
        close_fds=True
    )
    log_file.close()

    deadline = time.monotonic() + 180
    while time.monotonic() < deadline:
        try:
            res = SESSION.get(f"{config.BACKEND_URL}/system_stats", timeout=4)
            if res.status_code == 200:
                return True
        except requests.RequestException:
            time.sleep(2)

    raise RuntimeError("Timed out waiting for local image engine to start on port 8189.")


def upload_image(path: Path) -> str:
    """Upload image to local ComfyUI input folder."""
    with path.open("rb") as stream:
        res = SESSION.post(
            f"{config.BACKEND_URL}/upload/image",
            files={"image": (path.name, stream, "image/png")},
            data={"subfolder": "AIDressChanger", "type": "input", "overwrite": "true"},
            timeout=60
        )
    res.raise_for_status()
    data = res.json()
    return f"{data['subfolder']}/{data['name']}" if data.get("subfolder") else data["name"]


def calculate_working_dimensions(orig_w: int, orig_h: int, target_size: int = 768):
    """
    Scale source dimensions preserving aspect ratio with pixel area equal to target_size^2.
    Rounded to multiples of 16 for optimal inpainting and VAE compatibility.
    """
    scale = target_size / math.sqrt(orig_w * orig_h)
    w = max(64, round(orig_w * scale / 16) * 16)
    h = max(64, round(orig_h * scale / 16) * 16)
    return w, h


def build_flux_klein_graph(
    source_subpath: str,
    prompt: str,
    model_cfg: dict,
    width: int = 768,
    height: int = 1024,
    mode: str = "face_studio",
    seed: int = None
):
    """
    Flux.2 Klein 4B with ReferenceLatent & Unrestricted Qwen3 Heretic text encoder.
    Takes any face/portrait image as reference and generates any prompt (outfit, pose,
    scene, style) with 100% facial identity preservation, zero censorship, and zero body deformation.
    """
    if seed is None:
        seed = random.randint(1, 2147483647)

    clean_prompt = prompt.strip()
    if mode == "face_studio":
        if "maintain" not in clean_prompt.lower() and "face" not in clean_prompt.lower():
            formatted_prompt = (
                f"{clean_prompt}. "
                "Maintain the person's exact face, eyes, smile, and facial identity from the reference image. "
                "Photorealistic 8k uhd, masterpiece, natural skin texture, sharp focus."
            )
        else:
            formatted_prompt = clean_prompt
    else:
        formatted_prompt = (
            f"Replace the outfit with {clean_prompt}. "
            "Maintain the person's exact face, eyes, facial expression, hairstyle, body pose, jewelry, and background environment. "
            "Highly detailed photorealistic fashion photography, 8k uhd, sharp focus."
        )

    graph = {
        "model": {
            "class_type": "UNETLoader",
            "inputs": {
                "unet_name": model_cfg.get("unet", "flux-2-klein-4b-fp8.safetensors"),
                "weight_dtype": "default"
            }
        },
        "clip": {
            "class_type": "CLIPLoader",
            "inputs": {
                "clip_name": model_cfg.get("clip", "qwen3-4b-heretic_fp8_e4m3fn.safetensors"),
                "type": "flux2",
                "device": "default"
            }
        },
        "vae": {
            "class_type": "VAELoader",
            "inputs": {
                "vae_name": model_cfg.get("vae", "flux2-vae.safetensors")
            }
        },
        "source": {
            "class_type": "LoadImage",
            "inputs": {
                "image": source_subpath
            }
        },
        "resize": {
            "class_type": "ImageScale",
            "inputs": {
                "image": ["source", 0],
                "upscale_method": "lanczos",
                "width": width,
                "height": height,
                "crop": "disabled"
            }
        },
        "text": {
            "class_type": "CLIPTextEncode",
            "inputs": {
                "clip": ["clip", 0],
                "text": formatted_prompt
            }
        },
        "reference": {
            "class_type": "VAEEncode",
            "inputs": {
                "pixels": ["resize", 0],
                "vae": ["vae", 0]
            }
        },
        "condition": {
            "class_type": "ReferenceLatent",
            "inputs": {
                "conditioning": ["text", 0],
                "latent": ["reference", 0]
            }
        },
        "latent": {
            "class_type": "EmptyFlux2LatentImage",
            "inputs": {
                "width": width,
                "height": height,
                "batch_size": 1
            }
        },
        "guider": {
            "class_type": "BasicGuider",
            "inputs": {
                "model": ["model", 0],
                "conditioning": ["condition", 0]
            }
        },
        "schedule": {
            "class_type": "Flux2Scheduler",
            "inputs": {
                "steps": model_cfg.get("steps", 4),
                "width": width,
                "height": height
            }
        },
        "sampler": {
            "class_type": "KSamplerSelect",
            "inputs": {
                "sampler_name": model_cfg.get("sampler", "euler")
            }
        },
        "noise": {
            "class_type": "RandomNoise",
            "inputs": {
                "noise_seed": seed
            }
        },
        "sample": {
            "class_type": "SamplerCustomAdvanced",
            "inputs": {
                "noise": ["noise", 0],
                "guider": ["guider", 0],
                "sampler": ["sampler", 0],
                "sigmas": ["schedule", 0],
                "latent_image": ["latent", 0]
            }
        },
        "decode": {
            "class_type": "VAEDecodeTiled",
            "inputs": {
                "samples": ["sample", 0],
                "vae": ["vae", 0],
                "tile_size": 512,
                "overlap": 64,
                "temporal_size": 64,
                "temporal_overlap": 8
            }
        },
        "save": {
            "class_type": "SaveImage",
            "inputs": {
                "images": ["decode", 0],
                "filename_prefix": "DressChanger_Flux"
            }
        }
    }
    return graph, seed


def build_sd_txt2img_graph(
    prompt: str,
    negative_prompt: str = None,
    model_key: str = "realvis_xl",
    model_cfg: dict = None,
    width: int = 768,
    height: int = 1024,
    seed: int = None
):
    """
    Text-to-Image scene and outfit generation for SDXL (RealVisXL) and SD1.5 (Serenity).
    Generates high-fidelity fashion, body, and scene at target resolution without mask seams.
    Face identity lock is subsequently applied to transfer 100% exact facial likeness.
    """
    if seed is None:
        seed = random.randint(1, 2147483647)

    is_sdxl = model_cfg.get("type") == "sdxl"
    clean_prompt = prompt.strip()
    prompt_weight = 1.2 if is_sdxl else 1.25
    positive_prompt = (
        f"({clean_prompt}:{prompt_weight}), "
        "photorealistic portrait, beautiful face, natural skin texture, masterpiece, "
        "8k uhd, sharp focus, professional photography, studio lighting"
    )
    if not negative_prompt or not negative_prompt.strip():
        negative_prompt = config.DEFAULT_NEGATIVE_PROMPT
    else:
        negative_prompt = negative_prompt.strip()

    cfg = model_cfg.get("cfg", 4.5 if is_sdxl else 6.0)
    steps = model_cfg.get("steps", 16)

    graph = {
        "checkpoint": {
            "class_type": "CheckpointLoaderSimple",
            "inputs": {"ckpt_name": model_cfg["checkpoint"]}
        },
        "positive": {
            "class_type": "CLIPTextEncode",
            "inputs": {"clip": ["checkpoint", 1], "text": positive_prompt}
        },
        "negative": {
            "class_type": "CLIPTextEncode",
            "inputs": {"clip": ["checkpoint", 1], "text": negative_prompt}
        },
        "latent": {
            "class_type": "EmptyLatentImage",
            "inputs": {
                "width": width,
                "height": height,
                "batch_size": 1
            }
        }
    }

    if is_sdxl:
        vae_ref = ["checkpoint", 2]
    else:
        graph["vae"] = {
            "class_type": "VAELoader",
            "inputs": {"vae_name": model_cfg.get("vae", config.SD_VAE)}
        }
        vae_ref = ["vae", 0]

    graph["sample"] = {
        "class_type": "KSampler",
        "inputs": {
            "model": ["checkpoint", 0],
            "positive": ["positive", 0],
            "negative": ["negative", 0],
            "latent_image": ["latent", 0],
            "seed": seed,
            "steps": steps,
            "cfg": cfg,
            "sampler_name": model_cfg.get("sampler", "dpmpp_2m"),
            "scheduler": model_cfg.get("scheduler", "karras"),
            "denoise": 1.0
        }
    }

    graph["decode"] = {
        "class_type": "VAEDecodeTiled",
        "inputs": {
            "samples": ["sample", 0],
            "vae": vae_ref,
            "tile_size": 512,
            "overlap": 64,
            "temporal_size": 64,
            "temporal_overlap": 8
        }
    }

    graph["save"] = {
        "class_type": "SaveImage",
        "inputs": {
            "images": ["decode", 0],
            "filename_prefix": f"DressChanger_{model_key}"
        }
    }

    return graph, seed


def build_workflow_graph(
    source_subpath: str,
    mask_subpath: str = None,
    prompt: str = "",
    negative_prompt: str = None,
    model_key: str = "flux_klein",
    mode: str = "face_studio",
    width: int = 768,
    height: int = 1024,
    seed: int = None
):
    model_cfg = config.MODELS.get(model_key, config.MODELS["flux_klein"])

    # If Flux.2 Klein model is chosen:
    if model_cfg.get("type") == "flux":
        return build_flux_klein_graph(
            source_subpath=source_subpath,
            prompt=prompt,
            model_cfg=model_cfg,
            width=width,
            height=height,
            mode=mode,
            seed=seed
        )

    # If Face Studio mode or no mask is provided, use Text-to-Image for SDXL / SD1.5:
    if mode == "face_studio" or mask_subpath is None:
        return build_sd_txt2img_graph(
            prompt=prompt,
            negative_prompt=negative_prompt,
            model_key=model_key,
            model_cfg=model_cfg,
            width=width,
            height=height,
            seed=seed
        )

    # Inpainting for SD1.5 (Serenity) and SDXL (RealVisXL)
    if seed is None:
        seed = random.randint(1, 2147483647)

    is_sdxl = model_cfg.get("type") == "sdxl"
    clean_prompt = prompt.strip()
    prompt_weight = 1.2 if is_sdxl else 1.25
    positive_prompt = (
        f"({clean_prompt}:{prompt_weight}), "
        "photorealistic clothing, worn on body, correct fit, natural fabric folds, "
        "sharp seams, detailed clothing texture, elegant lighting, 8k uhd, photorealistic, high quality"
    )
    if not negative_prompt or not negative_prompt.strip():
        negative_prompt = config.DEFAULT_NEGATIVE_PROMPT
    else:
        negative_prompt = negative_prompt.strip()

    # Prevent sheer/nude lining, bra artifacts, and bridal bleed when colors/styles are requested
    anti_sheer = "nude lining, beige bra, bra cups, underwear, lingerie"
    prompt_lower = clean_prompt.lower()
    bridal_neg = "" if any(w in prompt_lower for w in ("white", "bridal", "wedding", "cream")) else "white dress, wedding dress, bridal, "
    negative_prompt = f"{anti_sheer}, {bridal_neg}{negative_prompt}"

    cfg = model_cfg.get("cfg", 5.5 if is_sdxl else 7.0)

    graph = {
        "checkpoint": {
            "class_type": "CheckpointLoaderSimple",
            "inputs": {"ckpt_name": model_cfg["checkpoint"]}
        },
        "positive": {
            "class_type": "CLIPTextEncode",
            "inputs": {"clip": ["checkpoint", 1], "text": positive_prompt}
        },
        "negative": {
            "class_type": "CLIPTextEncode",
            "inputs": {"clip": ["checkpoint", 1], "text": negative_prompt}
        },
        "source": {
            "class_type": "LoadImage",
            "inputs": {"image": source_subpath}
        },
        "resize_source": {
            "class_type": "ImageScale",
            "inputs": {
                "image": ["source", 0],
                "upscale_method": "lanczos",
                "width": width,
                "height": height,
                "crop": "disabled"
            }
        },
        "mask_source": {
            "class_type": "LoadImage",
            "inputs": {"image": mask_subpath}
        },
        "resize_mask": {
            "class_type": "ImageScale",
            "inputs": {
                "image": ["mask_source", 0],
                "upscale_method": "bilinear",
                "width": width,
                "height": height,
                "crop": "disabled"
            }
        },
        "mask": {
            "class_type": "ImageToMask",
            "inputs": {"image": ["resize_mask", 0], "channel": "red"}
        }
    }

    if is_sdxl:
        # SDXL uses internal checkpoint VAE
        vae_ref = ["checkpoint", 2]
        
        # Load Fooocus SDXL Inpaint Patch for anatomically perfect, non-distorted clothing inpainting
        graph["inpaint_latent"] = {
            "class_type": "VAEEncodeForInpaint",
            "inputs": {
                "pixels": ["resize_source", 0],
                "vae": vae_ref,
                "mask": ["mask", 0],
                "grow_mask_by": 4
            }
        }
        graph["inpaint_patch"] = {
            "class_type": "INPAINT_LoadFooocusInpaint",
            "inputs": {
                "head": "fooocus_inpaint_head.pth",
                "patch": "inpaint_v26.fooocus.patch"
            }
        }
        graph["patched_model"] = {
            "class_type": "INPAINT_ApplyFooocusInpaint",
            "inputs": {
                "model": ["checkpoint", 0],
                "patch": ["inpaint_patch", 0],
                "latent": ["inpaint_latent", 0]
            }
        }
        model_ref = ["patched_model", 0]
        pos_ref = ["positive", 0]
        neg_ref = ["negative", 0]
    else:
        # SD1.5 loads high-quality MSE VAE
        graph["vae"] = {
            "class_type": "VAELoader",
            "inputs": {"vae_name": model_cfg.get("vae", config.SD_VAE)}
        }
        vae_ref = ["vae", 0]

        # Use SD1.5 Inpaint ControlNet for anatomically guided clothing transformation
        graph["controlnet"] = {
            "class_type": "ControlNetLoader",
            "inputs": {"control_net_name": "control_v11p_sd15_inpaint_fp16.safetensors"}
        }
        graph["control_image"] = {
            "class_type": "StudioInpaintControlImage",
            "inputs": {"image": ["resize_source", 0], "mask": ["mask", 0]}
        }
        graph["apply_controlnet"] = {
            "class_type": "ControlNetApplyAdvanced",
            "inputs": {
                "positive": ["positive", 0],
                "negative": ["negative", 0],
                "control_net": ["controlnet", 0],
                "image": ["control_image", 0],
                "strength": 0.90,
                "start_percent": 0.0,
                "end_percent": 1.0,
                "vae": vae_ref
            }
        }
        graph["inpaint_latent"] = {
            "class_type": "VAEEncodeForInpaint",
            "inputs": {
                "pixels": ["resize_source", 0],
                "vae": vae_ref,
                "mask": ["mask", 0],
                "grow_mask_by": 4
            }
        }
        model_ref = ["checkpoint", 0]
        pos_ref = ["apply_controlnet", 0]
        neg_ref = ["apply_controlnet", 1]

    graph["sample"] = {
        "class_type": "KSampler",
        "inputs": {
            "model": model_ref,
            "positive": pos_ref,
            "negative": neg_ref,
            "latent_image": ["inpaint_latent", 0],
            "seed": seed,
            "steps": model_cfg.get("steps", 16),
            "cfg": cfg,
            "sampler_name": model_cfg.get("sampler", "dpmpp_2m"),
            "scheduler": model_cfg.get("scheduler", "karras"),
            "denoise": 1.0
        }
    }

    graph["decode"] = {
        "class_type": "VAEDecodeTiled",
        "inputs": {
            "samples": ["sample", 0],
            "vae": vae_ref,
            "tile_size": 512,
            "overlap": 64,
            "temporal_size": 64,
            "temporal_overlap": 8
        }
    }

    graph["save"] = {
        "class_type": "SaveImage",
        "inputs": {
            "images": ["decode", 0],
            "filename_prefix": f"DressChanger_{model_key}"
        }
    }

    return graph, seed


def execute_workflow(graph: dict, progress_cb=None) -> Image.Image:
    """Submit workflow graph to ComfyUI, wait for completion, and return resulting PIL image."""
    client_id = str(uuid.uuid4())
    res = SESSION.post(
        f"{config.BACKEND_URL}/prompt",
        json={"prompt": graph, "client_id": client_id},
        timeout=30
    )
    if res.status_code != 200:
        raise RuntimeError(f"Engine rejected workflow: {res.text[:1000]}")
    prompt_id = res.json()["prompt_id"]

    start_time = time.monotonic()
    while True:
        elapsed = round(time.monotonic() - start_time)
        if elapsed > 300:
            raise TimeoutError("Engine workflow execution timed out after 300 seconds.")
        if progress_cb:
            progress_cb(f"Rendering photorealistic outfit... ({elapsed}s)")

        res = SESSION.get(f"{config.BACKEND_URL}/history/{prompt_id}", timeout=15)
        res.raise_for_status()
        data = res.json().get(prompt_id)

        if data:
            status = data.get("status", {})
            if status.get("status_str") == "error":
                messages = status.get("messages", [])
                err = next((m[1].get("exception_message") for m in reversed(messages) if m[0] == "execution_error"), "Engine error")
                raise RuntimeError(err)

            save_output = data.get("outputs", {}).get("save", {})
            images = save_output.get("images", [])
            if not images:
                raise RuntimeError("No image returned from engine.")

            img_res = SESSION.get(f"{config.BACKEND_URL}/view", params=images[0], timeout=60)
            img_res.raise_for_status()
            with Image.open(io.BytesIO(img_res.content)) as generated:
                return generated.copy()

        time.sleep(1)


def composite_and_finish(
    source: Image.Image,
    rendered: Image.Image,
    mask: Image.Image = None
) -> Image.Image:
    """
    Composite rendered dress back into source at original resolution.
    If mask is provided (SD1.5 inpainting), blends using the mask.
    If no mask is provided (Flux.2 Klein), returns the rendered image scaled to source dimensions.
    """
    source_rgb = source.convert("RGB")
    rendered_scaled = rendered.convert("RGB").resize(source.size, Image.Resampling.LANCZOS)

    if mask is None:
        return rendered_scaled

    from PIL import ImageFilter
    mask_scaled = mask.convert("L").resize(source.size, Image.Resampling.BILINEAR)
    mask_soft = mask_scaled.filter(ImageFilter.GaussianBlur(3.0))
    return Image.composite(rendered_scaled, source_rgb, mask_soft)
