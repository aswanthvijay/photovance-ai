import sys
sys.path.insert(0, r"d:\image mod")
from pathlib import Path
from PIL import Image
import config
import engine

img1_path = Path(r"C:\Users\Aswanth Vijay\.gemini\antigravity\brain\a85f4511-27eb-48cb-9eea-6faf8206cea6\.user_uploaded\media_1791983382766_4485fadf.jpg")
sub1 = engine.upload_image(img1_path)
im1 = Image.open(img1_path)
w1, h1 = engine.calculate_working_dimensions(im1.width, im1.height, 896)

cfg = config.MODELS["flux_klein"]

graph, _ = engine.build_flux_klein_graph(
    source_subpath=sub1,
    prompt="gold chainmail dress",
    model_cfg=cfg,
    width=w1,
    height=h1,
    seed=2024
)
graph["text"]["inputs"]["text"] = (
    "A cinematic high-fashion portrait of this woman wearing a glamorous backless gold chainmail dress, "
    "standing on the teak deck of a luxury superyacht in Monaco during golden hour sunset, warm ocean breeze. "
    "Maintain the person's exact face, eyes, smile, and facial identity. "
    "Photorealistic 8k uhd, masterpiece."
)

print("Testing Flux.2 Klein on full scene + outfit generation...")
res = engine.execute_workflow(graph)
out_path = Path(r"C:\Users\Aswanth Vijay\.gemini\antigravity\brain\a85f4511-27eb-48cb-9eea-6faf8206cea6\flux_yacht_scene_test.png")
res.save(out_path)
print("Saved to:", out_path)
