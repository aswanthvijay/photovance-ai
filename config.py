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
        "name": "Photovance Ultra-Portrait™",
        "desc": "Fast 8K Studio Lighting with micro-detail rendering and 100% facial identity lock.",
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
        "name": "Editorial Couture Master™",
        "desc": "High-fashion runway fidelity with micro-weave fabric texture and studio lighting.",
        "type": "flux",
        "unet": "flux-2-klein-4b.safetensors",
        "clip": "qwen3-4b-heretic_fp8_e4m3fn.safetensors",
        "vae": FLUX_VAE,
        "target_size": 896,
        "steps": 6,
        "sampler": "euler"
    },
    "realvis_xl": {
        "id": "realvis_xl",
        "name": "Editorial Luxury Studio (DSLR 85mm)™",
        "desc": "High-fashion Vogue aesthetic, 85mm portrait telephoto lens, creamy bokeh, and authentic golden skin tones.",
        "type": "sdxl",
        "checkpoint": "RealVisXL_V5.0_fp16.safetensors",
        "target_size": 896,
        "cfg": 4.5,
        "steps": 14,
        "sampler": "dpmpp_2m",
        "scheduler": "karras"
    },
    "serenity": {
        "id": "serenity",
        "name": "Studio Glamour Soft™",
        "desc": "Gentle portrait illumination with elegant soft skin tones.",
        "type": "sd15",
        "checkpoint": "serenity_v21Safetensors.safetensors",
        "vae": SD_VAE,
        "target_size": 768,
        "cfg": 6.0,
        "steps": 16,
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
    "open mouth, teeth, forced smile, grinning, wide angle distortion, selfie, flat flash, fisheye, "
    "cartoon, painting, illustration, 3d render, CGI, plastic skin, doll, airbrushed, "
    "porcelain skin, smooth plastic, mannequin, wax figure, fake eyes, artificial, "
    "oversaturated, blurry, out of focus, bad anatomy, deformed, mutated hands, "
    "extra fingers, fused fingers, extra limbs, asymmetric eyes, bad proportions, "
    "watermark, signature, text, logo, distorted face"
)
