import os
from pathlib import Path

# Base Paths
WORKSPACE_ROOT = Path(r"D:\image mod").resolve()
IMAGE_AI_ROOT = Path(r"D:\Image AI L model").resolve()
STABILITY_MATRIX_ROOT = Path(r"D:\Image AI\StabilityMatrix-win-x64").resolve()

# Python Runtime (Prefer ComfyUI virtualenv with Torch, Pillow & Transformers)
COMFY_PYTHON = STABILITY_MATRIX_ROOT / "Data" / "Packages" / "ComfyUI" / "venv" / "Scripts" / "python.exe"
PYTHON_EXE = COMFY_PYTHON if COMFY_PYTHON.exists() else (IMAGE_AI_ROOT / "runtime" / "python.exe")
if not PYTHON_EXE.exists():
    PYTHON_EXE = STABILITY_MATRIX_ROOT / "Data" / "Assets" / "Python" / "cpython-3.12.14-windows-x86_64-none" / "python.exe"

# Backend configuration
COMFYUI_DIR = STABILITY_MATRIX_ROOT / "Data" / "Packages" / "ComfyUI"
BACKEND_URL = "http://127.0.0.1:8189"
HOST = "0.0.0.0"
PORT = 7860

# Model Assets
MODELS_DIR = STABILITY_MATRIX_ROOT / "Data" / "Models" / "StableDiffusion"
DIFFUSION_MODELS_DIR = STABILITY_MATRIX_ROOT / "Data" / "Models" / "DiffusionModels"
TEXT_ENCODERS_DIR = STABILITY_MATRIX_ROOT / "Data" / "Models" / "TextEncoders"
VAE_DIR = STABILITY_MATRIX_ROOT / "Data" / "Models" / "VAE"
CLOTHING_PARSER_DIR = IMAGE_AI_ROOT / "assets" / "clothing-parser"
CLIPSEG_DIR = IMAGE_AI_ROOT / "assets" / "clipseg"
FACE_DETECTOR_PATH = IMAGE_AI_ROOT / "assets" / "face_detection_yunet_2023mar.onnx"

# VAE
SD_VAE = "vae-ft-mse-840000-ema-pruned.safetensors"
FLUX_VAE = "flux2-vae.safetensors"

# Available Models for Dress Editing & Face-Preserved Scene Generation
MODELS = {
    "flux_klein": {
        "id": "flux_klein",
        "name": "🌟 Flux.2 Klein 4B Turbo (Unrestricted Heretic DiT · Recommended)",
        "desc": "Ultra-fast 4-step generation with ReferenceLatent. Unrestricted Heretic text encoder (zero censorship). Generates any scene, outfit, and pose with 100% face likeness.",
        "type": "flux",
        "unet": "flux-2-klein-4b-fp8.safetensors",
        "clip": "qwen3-4b-heretic_fp8_e4m3fn.safetensors",
        "vae": FLUX_VAE,
        "target_size": 896,
        "steps": 4,
        "sampler": "euler"
    },
    "flux_klein_ultra": {
        "id": "flux_klein_ultra",
        "name": "👑 Flux.2 Klein 4B Master (FP16 Full Precision · Unrestricted)",
        "desc": "Full-precision master DiT — highest fidelity fabric micro-textures, authentic silk folds, and studio lighting with 100% facial identity preservation.",
        "type": "flux",
        "unet": "flux-2-klein-4b.safetensors",
        "clip": "qwen3-4b-heretic_fp8_e4m3fn.safetensors",
        "vae": FLUX_VAE,
        "target_size": 896,
        "steps": 6,
        "sampler": "euler"
    },
    "serenity": {
        "id": "serenity",
        "name": "Serenity v2.1 (SD1.5 Photography · Inpainting)",
        "desc": "Photorealistic SD1.5 portrait photography with 100% facial lock & ControlNet inpainting",
        "type": "sd15",
        "checkpoint": "serenity_v21Safetensors.safetensors",
        "vae": SD_VAE,
        "target_size": 768,
        "cfg": 6.0,
        "steps": 16,
        "sampler": "dpmpp_2m",
        "scheduler": "karras"
    },
    "realvis_xl": {
        "id": "realvis_xl",
        "name": "RealVisXL v5.0 (SDXL Photography · Inpainting)",
        "desc": "High-resolution SDXL photorealistic fashion generation with 100% facial lock & Fooocus inpainting",
        "type": "sdxl",
        "checkpoint": "RealVisXL_V5.0_fp16.safetensors",
        "target_size": 896,
        "cfg": 4.5,
        "steps": 14,
        "sampler": "dpmpp_2m",
        "scheduler": "karras"
    }
}

# Directories for inputs, outputs, temp
INPUTS_DIR = WORKSPACE_ROOT / "inputs"
OUTPUTS_DIR = WORKSPACE_ROOT / "outputs"
SAMPLES_DIR = WORKSPACE_ROOT / "samples"

# Default Negative Prompt
DEFAULT_NEGATIVE_PROMPT = (
    "cartoon, painting, illustration, 3d render, CGI, plastic skin, doll, airbrushed, "
    "oversaturated, blurry, out of focus, bad anatomy, deformed, mutated hands, "
    "extra fingers, fused fingers, extra limbs, asymmetric eyes, bad proportions, "
    "watermark, signature, text, logo, distorted face"
)
