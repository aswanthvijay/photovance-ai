"""AI Dress Changer Web Application Server."""
import base64
import email
import io
import json
import mimetypes
import os
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent
sys.path.insert(0, str(ROOT))

vendor_dir = Path(r"D:\Image AI L model\vendor")
if vendor_dir.exists() and str(vendor_dir) not in sys.path:
    sys.path.insert(1, str(vendor_dir))

site_packages = Path(r"D:\Image AI\StabilityMatrix-win-x64\Data\Packages\ComfyUI\venv\Lib\site-packages")
if site_packages.exists() and str(site_packages) not in sys.path:
    sys.path.append(str(site_packages))

import base64
import email
import io
import json
import mimetypes
import time
import uuid
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from PIL import Image

import config
from engine import (
    ensure_backend,
    upload_image,
    calculate_working_dimensions,
    build_workflow_graph,
    execute_workflow,
    composite_and_finish
)
from segmentation import generate_dress_mask
from face_lock import apply_face_lock

from security import RateLimiter, ImageSanitizer, SafePath, TTLCacheCleaner, MAX_PAYLOAD_SIZE

HISTORY = []
RATE_LIMITER = RateLimiter(max_requests=8, window_seconds=60)

# Start 24-hour background ephemeral cache cleaner
TTLCacheCleaner.start([config.INPUTS_DIR, config.OUTPUTS_DIR], max_age_seconds=86400)


class DressChangerHandler(BaseHTTPRequestHandler):
    def end_headers(self):
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type, Authorization")
        self.send_header("X-Content-Type-Options", "nosniff")
        self.send_header("X-Frame-Options", "DENY")
        super().end_headers()

    def do_OPTIONS(self):
        self.send_response(200)
        self.end_headers()

    def send_json(self, data, status=200):
        body = json.dumps(data).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def serve_file(self, file_path: Path):
        if not file_path.is_file():
            self.send_response(404)
            self.end_headers()
            return
        mime_type, _ = mimetypes.guess_type(str(file_path))
        if not mime_type:
            mime_type = "application/octet-stream"
        content = file_path.read_bytes()
        self.send_response(200)
        self.send_header("Content-Type", mime_type)
        self.send_header("Content-Length", str(len(content)))
        self.end_headers()
        self.wfile.write(content)

    def do_GET(self):
        path = self.path.split("?")[0]

        if path == "/" or path == "/index.html":
            self.serve_file(config.WORKSPACE_ROOT / "static" / "index.html")
            return

        if path == "/privacy" or path == "/privacy.html":
            self.serve_file(config.WORKSPACE_ROOT / "static" / "privacy.html")
            return

        if path == "/api/status":
            self.send_json({
                "ready": True,
                "models": config.MODELS,
                "default_negative_prompt": config.DEFAULT_NEGATIVE_PROMPT
            })
            return

        if path == "/api/sample":
            sample_path = config.SAMPLES_DIR / "sample_model.png"
            with Image.open(sample_path) as im:
                w, h = im.size
            self.send_json({
                "image_id": "samples/sample_model.png",
                "image_url": "/samples/sample_model.png",
                "width": w,
                "height": h
            })
            return

        if path == "/api/history":
            self.send_json(HISTORY[-15:])
            return

        if path.startswith("/inputs/"):
            filename = Path(path[len("/inputs/"):].strip("/")).name
            safe = SafePath.sanitize(config.INPUTS_DIR, filename)
            if safe and safe.is_file():
                self.serve_file(safe)
            else:
                self.send_response(404); self.end_headers()
            return

        if path.startswith("/outputs/"):
            filename = Path(path[len("/outputs/"):].strip("/")).name
            safe = SafePath.sanitize(config.OUTPUTS_DIR, filename)
            if safe and safe.is_file():
                self.serve_file(safe)
            else:
                self.send_response(404); self.end_headers()
            return

        if path.startswith("/samples/"):
            filename = Path(path[len("/samples/"):].strip("/")).name
            safe = SafePath.sanitize(config.SAMPLES_DIR, filename)
            if safe and safe.is_file():
                self.serve_file(safe)
            else:
                self.send_response(404); self.end_headers()
            return

        self.send_response(404)
        self.end_headers()

    def do_POST(self):
        client_ip = self.client_address[0]
        path = self.path.split("?")[0]

        if path == "/api/upload":
            # Rate limiting check
            allowed, wait_sec = RATE_LIMITER.is_allowed(f"upload_{client_ip}")
            if not allowed:
                self.send_json({"error": f"Upload rate limit exceeded. Please wait {wait_sec}s."}, status=429)
                return

            content_length = int(self.headers.get("content-length", 0))
            if content_length > MAX_PAYLOAD_SIZE:
                self.send_json({"error": "Payload exceeds maximum allowed size (10MB)."}, status=413)
                return

            raw_body = self.rfile.read(content_length)
            content_type = self.headers.get("content-type", "")

            image_bytes = None
            if "application/json" in content_type:
                try:
                    payload = json.loads(raw_body.decode("utf-8"))
                    data_uri = payload.get("data", "")
                    if "," in data_uri:
                        _, b64 = data_uri.split(",", 1)
                        image_bytes = base64.b64decode(b64)
                except Exception as e:
                    self.send_json({"error": f"Invalid JSON payload: {e}"}, status=400)
                    return
            else:
                # Parse multipart
                msg = email.message_from_bytes(f"Content-Type: {content_type}\r\n\r\n".encode("utf-8") + raw_body)
                for part in msg.walk():
                    if part.get_filename():
                        image_bytes = part.get_payload(decode=True)
                        break

            if not image_bytes:
                self.send_json({"error": "No image data received"}, status=400)
                return

            # Cybersecurity: Sanitize image bytes, strip metadata, verify magic bytes
            valid, clean_bytes, err = ImageSanitizer.sanitize(image_bytes)
            if not valid:
                self.send_json({"error": err}, status=400)
                return

            image_id = f"upload_{uuid.uuid4().hex[:12]}.jpg"
            target_path = config.INPUTS_DIR / image_id
            target_path.write_bytes(clean_bytes)

            with Image.open(io.BytesIO(clean_bytes)) as img:
                w, h = img.size

            self.send_json({
                "image_id": image_id,
                "image_url": f"/inputs/{image_id}",
                "width": w,
                "height": h
            })
            return

        if path == "/api/change-dress":
            # Rate limiting check (protect GPU compute)
            allowed, wait_sec = RATE_LIMITER.is_allowed(f"render_{client_ip}")
            if not allowed:
                self.send_json({"error": f"Studio GPU busy. Please wait {wait_sec}s before next creation."}, status=429)
                return

            try:
                content_length = int(self.headers.get("content-length", 0))
                if content_length > 64 * 1024:  # JSON prompt payload shouldn't exceed 64KB
                    self.send_json({"error": "Request body too large."}, status=413)
                    return

                body = self.rfile.read(content_length).decode("utf-8")
                data = json.loads(body)

                image_id = data.get("image_id", "")
                prompt = data.get("prompt", "").strip()
                negative_prompt = data.get("negative_prompt", config.DEFAULT_NEGATIVE_PROMPT)
                model_key = data.get("model_key", "flux_klein")
                mode = data.get("mode", "face_studio")
                aspect_ratio = data.get("aspect_ratio", "original")
                target = data.get("target", "dress")
                expand = int(data.get("expand", 5))
                keep_face = bool(data.get("keep_face", True))

                if not image_id or not prompt:
                    self.send_json({"error": "Image and prompt are required."}, status=400)
                    return

                # Safe file resolution (Path Traversal Defense)
                raw_filename = Path(image_id).name
                if image_id.startswith("samples/"):
                    source_path = SafePath.sanitize(config.SAMPLES_DIR, raw_filename)
                elif image_id.startswith("outputs/"):
                    source_path = SafePath.sanitize(config.OUTPUTS_DIR, raw_filename)
                else:
                    source_path = SafePath.sanitize(config.INPUTS_DIR, raw_filename)

                if not source_path or not source_path.exists():
                    self.send_json({"error": "Source image not found or access denied."}, status=404)
                    return

                start_time = time.monotonic()
                source_img = Image.open(source_path)
                model_info = config.MODELS.get(model_key, config.MODELS["flux_klein"])
                is_flux = model_info.get("type") == "flux"
                is_sdxl = model_info.get("type") == "sdxl"
                is_sd15 = model_info.get("type") == "sd15"

                job_id = uuid.uuid4().hex[:12]
                result_filename = f"result_{job_id}.png"
                preview_filename = f"preview_{job_id}.png"
                result_path = config.OUTPUTS_DIR / result_filename
                preview_path = config.OUTPUTS_DIR / preview_filename

                ensure_backend()
                src_sub = upload_image(source_path)

                if mode == "face_studio":
                    # Unrestricted Face Studio: Generate ANY prompt, outfit, scene, or pose preserving face
                    # Compute dimensions based on model architecture and aspect ratio
                    if aspect_ratio == "portrait":
                        w, h = (896, 1152) if (is_flux or is_sdxl) else (576, 832)
                    elif aspect_ratio == "square":
                        w, h = (896, 896) if (is_flux or is_sdxl) else (640, 640)
                    elif aspect_ratio == "landscape":
                        w, h = (1152, 896) if (is_flux or is_sdxl) else (832, 576)
                    elif aspect_ratio == "story":
                        w, h = (768, 1344) if (is_flux or is_sdxl) else (512, 896)
                    else:
                        target_size = model_info.get("target_size", 896 if (is_flux or is_sdxl) else 704)
                        w, h = calculate_working_dimensions(source_img.width, source_img.height, target_size=target_size)

                    # Save source as reference preview
                    source_img.save(preview_path, "PNG")
                    coverage = 1.0

                    graph, seed = build_workflow_graph(
                        src_sub,
                        None,
                        prompt=prompt,
                        negative_prompt=negative_prompt,
                        model_key=model_key,
                        mode="face_studio",
                        width=w,
                        height=h
                    )
                    rendered = execute_workflow(graph)
                    # Apply 100% exact facial identity lock & GFPGAN HD restoration
                    rendered = apply_face_lock(source_img, rendered)
                    rendered.save(result_path, "PNG")

                else:
                    # In-Place Outfit Swap: Detect clothing mask and transform within original photo
                    mask, preview_overlay, face_boxes, coverage = generate_dress_mask(
                        source_img,
                        target=target,
                        keep_face=keep_face,
                        expand_pixels=expand
                    )
                    mask_filename = f"mask_{job_id}.png"
                    mask_path = config.INPUTS_DIR / mask_filename
                    mask.save(mask_path)
                    preview_overlay.save(preview_path)

                    msk_sub = upload_image(mask_path)
                    target_size = model_info.get("target_size", 896 if (is_flux or is_sdxl) else 704)
                    w, h = calculate_working_dimensions(source_img.width, source_img.height, target_size=target_size)

                    graph, seed = build_workflow_graph(
                        src_sub,
                        msk_sub,
                        prompt=prompt,
                        negative_prompt=negative_prompt,
                        model_key=model_key,
                        mode="in_place",
                        width=w,
                        height=h
                    )
                    rendered = execute_workflow(graph)
                    finished = composite_and_finish(source_img, rendered, None if is_flux else mask)
                    if keep_face:
                        finished = apply_face_lock(source_img, finished)
                    finished.save(result_path, "PNG")

                elapsed = round(time.monotonic() - start_time, 1)
                model_name = model_info.get("name", model_key)

                record = {
                    "id": job_id,
                    "url": f"/outputs/{result_filename}",
                    "prompt": prompt,
                    "model": model_name,
                    "mode": mode,
                    "elapsed": elapsed
                }
                HISTORY.append(record)

                self.send_json({
                    "success": True,
                    "result_url": f"/outputs/{result_filename}",
                    "preview_url": f"/outputs/{preview_filename}",
                    "elapsed": elapsed,
                    "model_name": model_name,
                    "mode": mode,
                    "coverage": round(coverage * 100, 1)
                })
                return
            except Exception as e:
                import traceback
                traceback.print_exc()
                self.send_json({"error": str(e)}, status=500)
                return

        self.send_response(404)
        self.end_headers()


def run_server(host=None, port=None):
    if host is None:
        host = getattr(config, "HOST", "0.0.0.0")
    if port is None:
        port = getattr(config, "PORT", 7860)

    ThreadingHTTPServer.allow_reuse_address = True
    server = ThreadingHTTPServer((host, port), DressChangerHandler)
    print(f"[AI Face Studio] Local URL:   http://127.0.0.1:{port}", flush=True)
    print(f"[AI Face Studio] Network URL: http://{host}:{port}", flush=True)
    server.serve_forever()


if __name__ == "__main__":
    run_server(config.HOST, config.PORT)
