# AI Dress & Outfit Changer 👗

A dedicated, effortless 100% offline image model application to automatically change clothes and dresses using **Realistic Vision** and **CyberRealistic** inpainting models on your local NVIDIA RTX 3060 GPU.

---

## 🚀 Quick Start

1. Double-click **`AI Dress Changer`** on your **Desktop**  
   *(Or run `start_dress_changer.bat` in `D:\image mod`)*
2. Your browser will automatically open to:  
   👉 **http://127.0.0.1:7860**
3. **Upload or drag-and-drop** any photo of a person (or click **"✨ Try Sample Model"**).
4. Type your prompt for the new outfit (e.g. `red silk evening gown` or `floral summer sundress`).
5. Click **"✨ Change Dress"** (or press `Ctrl + Enter`).
6. In **~9 to 20 seconds**, preview the transformation with the interactive slider and download your high-resolution image!

---

## 💎 Installed Local Models

All models are downloaded and running completely offline on your local machine:

| Model | Purpose | Speed on RTX 3060 |
| :--- | :--- | :--- |
| **Realistic Vision v6.0 Inpainting** | Photorealistic clothing textures, natural light, balanced contrast *(Recommended)* | ~9–15 seconds |
| **CyberRealistic Final Inpainting** | Hyper-realistic fabric micro-details, silk/leather/lace rendering | ~15–22 seconds |
| **CyberRealistic v9.0 Inpainting** | Latest CyberRealistic release with rich shadows and drape folds | ~15–22 seconds |
| **SegFormer B2 Clothes Parser** | Deep-learning semantic segmentation isolating garments from skin | < 1 second |
| **YuNet Face Detector** | Automatic facial detection preserving identity, expression & skin tone | Instant |

---

## ✨ Features

- **No Manual Brushing Needed**: SegFormer automatically detects dresses, shirts, skirts, jackets, and pants.
- **100% Face & Skin Protection**: Automatically shields faces, hair, and limbs from inpainting distortion.
- **Interactive Split Slider**: Drag between the before and after image directly in the web UI.
- **One-Click Presets**: Quick buttons for popular dress styles (Emerald Silk Gown, Floral Sundress, Little Black Dress, Denim Jacket, Gold Sequin Party Dress, Silk Kimono).
- **Iterative Editing**: Click *"Use as Next Starting Image"* to make successive edits without re-uploading.
- **Complete Privacy**: Zero cloud APIs, zero external telemetry; runs entirely on your local machine.
