import sys
sys.path.insert(0, r"d:\image mod")
from pathlib import Path
from PIL import Image
import config
import engine

img1_path = Path(r"C:\Users\Aswanth Vijay\.gemini\antigravity\brain\a85f4511-27eb-48cb-9eea-6faf8206cea6\.user_uploaded\media_1791983382766_4485fadf.jpg")
img2_path = Path(r"C:\Users\Aswanth Vijay\.gemini\antigravity\brain\a85f4511-27eb-48cb-9eea-6faf8206cea6\.user_uploaded\media_1791983382799_a26b4d7e.jpg")
out_dir = Path(r"C:\Users\Aswanth Vijay\.gemini\antigravity\brain\a85f4511-27eb-48cb-9eea-6faf8206cea6")

engine.ensure_backend()

# Test Image 1
sub1 = engine.upload_image(img1_path)
im1 = Image.open(img1_path)
w1, h1 = engine.calculate_working_dimensions(im1.width, im1.height, 896)

graph1, _ = engine.build_flux_klein_graph(
    source_subpath=sub1,
    prompt="emerald green off-shoulder silk evening gown, luxury fabric, elegant draping",
    model_cfg=config.MODELS["flux_klein"],
    width=w1,
    height=h1,
    seed=100
)
graph1["text"]["inputs"]["text"] = (
    "Replace outfit with an elegant emerald green off-shoulder silk evening gown with luxurious fabric folds. "
    "Maintain the person's exact face, eyes, smile, wet hairstyle, necklace, and bedroom background. "
    "Photorealistic fashion photography, 8k uhd, highly detailed."
)
res1 = engine.execute_workflow(graph1)
res1.save(out_dir / "flux_instruct_img1.png")
print("Instructed Flux Img 1 complete.")

# Test Image 2
sub2 = engine.upload_image(img2_path)
im2 = Image.open(img2_path)
w2, h2 = engine.calculate_working_dimensions(im2.width, im2.height, 896)

graph2, _ = engine.build_flux_klein_graph(
    source_subpath=sub2,
    prompt="crimson red sleeveless cocktail dress, tailored fit",
    model_cfg=config.MODELS["flux_klein"],
    width=w2,
    height=h2,
    seed=100
)
graph2["text"]["inputs"]["text"] = (
    "Replace the blue shirt dress with a stylish crimson red sleeveless cocktail dress. "
    "Keep the person's exact face, expression, long hair, body posture, shoulder bag, and background shutter door. "
    "Photorealistic high fashion photography, 8k uhd."
)
res2 = engine.execute_workflow(graph2)
res2.save(out_dir / "flux_instruct_img2.png")
print("Instructed Flux Img 2 complete.")
