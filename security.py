"""
Cybersecurity & Anti-Abuse Protection Layer for Photovance AI Server.
Includes:
- Leaky-Bucket Rate Limiter (per IP & UID) to prevent DoS / GPU compute exhaustion
- Image Payload Sanitizer (Magic byte verification, 10MB ceiling, Pillow raw pixel re-encoding, EXIF stripping)
- Path Traversal Defense (Strict directory sandboxing)
- 24-Hour Ephemeral Cache Cleaner (Automated background TTL purging)
"""
import io
import os
import time
import threading
from pathlib import Path
from PIL import Image

# 10 MB maximum request payload ceiling
MAX_PAYLOAD_SIZE = 10 * 1024 * 1024

# Allowed image magic bytes
MAGIC_HEADERS = {
    b"\xFF\xD8\xFF": "jpeg",
    b"\x89PNG\r\n\x1a\n": "png",
    b"RIFF": "webp",  # WebP files start with RIFF
}

class RateLimiter:
    """Thread-safe in-memory leaky bucket rate limiter."""
    def __init__(self, max_requests: int = 6, window_seconds: int = 60):
        self.max_requests = max_requests
        self.window_seconds = window_seconds
        self.history = {}
        self.lock = threading.Lock()

    def is_allowed(self, client_id: str) -> tuple[bool, int]:
        """Returns (is_allowed, remaining_seconds_until_reset)."""
        now = time.time()
        with self.lock:
            # Clean expired timestamps
            if client_id in self.history:
                self.history[client_id] = [t for t in self.history[client_id] if now - t < self.window_seconds]
            else:
                self.history[client_id] = []

            if len(self.history[client_id]) >= self.max_requests:
                oldest = self.history[client_id][0]
                wait_time = int(self.window_seconds - (now - oldest)) + 1
                return False, max(1, wait_time)

            self.history[client_id].append(now)
            return True, 0


class ImageSanitizer:
    """Sanitizes image uploads to prevent polyglot malware and memory bombs."""
    @staticmethod
    def sanitize(raw_bytes: bytes, max_dimension: int = 4096) -> tuple[bool, bytes, str]:
        """
        Validates magic bytes, checks dimensions, strips EXIF metadata,
        and re-encodes raw pixels to neutralize any embedded scripts/malware.
        Returns: (success, clean_bytes, error_message)
        """
        if len(raw_bytes) > MAX_PAYLOAD_SIZE:
            return False, b"", f"Payload too large. Maximum size is {MAX_PAYLOAD_SIZE // (1024 * 1024)}MB."

        # Verify magic bytes
        is_valid_magic = False
        for magic in MAGIC_HEADERS:
            if raw_bytes.startswith(magic):
                is_valid_magic = True
                break
        if not is_valid_magic:
            return False, b"", "Security rejection: Invalid image file header."

        try:
            with Image.open(io.BytesIO(raw_bytes)) as img:
                # Protect against decompression bomb attacks
                img.verify()

            # Re-open for actual processing (verify closes stream in PIL)
            with Image.open(io.BytesIO(raw_bytes)) as img:
                # Enforce dimension bounds
                w, h = img.size
                if w > max_dimension or h > max_dimension:
                    scale = min(max_dimension / w, max_dimension / h)
                    new_size = (int(w * scale), int(h * scale))
                    img = img.resize(new_size, Image.Resampling.LANCZOS)

                # Convert to clean RGB (neutralizes malformed palettes / CMYK exploits)
                if img.mode != "RGB":
                    img = img.convert("RGB")

                # Strip all EXIF / metadata by creating a clean in-memory buffer
                clean_buffer = io.BytesIO()
                img.save(clean_buffer, format="JPEG", quality=95, optimize=True)
                clean_bytes = clean_buffer.getvalue()

                return True, clean_bytes, ""
        except Exception as e:
            return False, b"", f"Security rejection: Image decoding failed ({str(e)})."


class SafePath:
    """Prevents directory traversal attacks (../../)."""
    @staticmethod
    def sanitize(base_dir: Path, untrusted_filename: str) -> Path | None:
        filename = os.path.basename(untrusted_filename).strip()
        if not filename or filename.startswith("."):
            return None
        safe_path = (base_dir / filename).resolve()
        # Verify the resolved path stays within base_dir
        if not str(safe_path).startswith(str(base_dir.resolve())):
            return None
        return safe_path


class TTLCacheCleaner:
    """Background daemon thread that deletes temporary files older than 24 hours."""
    @staticmethod
    def start(directories: list[Path], max_age_seconds: int = 86400, interval_seconds: int = 3600):
        def worker():
            while True:
                try:
                    now = time.time()
                    for directory in directories:
                        if not directory.exists():
                            continue
                        for file_path in directory.iterdir():
                            if file_path.is_file():
                                try:
                                    if now - file_path.stat().st_mtime > max_age_seconds:
                                        file_path.unlink()
                                except Exception:
                                    pass
                except Exception:
                    pass
                time.sleep(interval_seconds)

        thread = threading.Thread(target=worker, daemon=True, name="PhotovanceTTLCleaner")
        thread.start()
