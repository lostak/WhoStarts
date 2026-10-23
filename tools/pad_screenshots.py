"""Pad screenshots to an exact 9:16 (portrait) or 16:9 (landscape) ratio for Google Play."""
import math, sys
from pathlib import Path
from PIL import Image

for path in sorted(Path(sys.argv[1]).glob("*.png")):
    im = Image.open(path).convert("RGB")
    w, h = im.size
    if h >= w:
        k = math.ceil(max(w / 9, h / 16)); W, H = 9 * k, 16 * k
        fill = im.getpixel((w // 2, h - 4))
    else:
        k = math.ceil(max(w / 16, h / 9)); W, H = 16 * k, 9 * k
        fill = im.getpixel((4, h // 2))
    if (W, H) != (w, h):
        canvas = Image.new("RGB", (W, H), fill)
        canvas.paste(im, ((W - w) // 2, (H - h) // 2))
        im = canvas
    im.save(path, optimize=True)
    print(f"{path.name}: {w}x{h} -> {W}x{H}")
