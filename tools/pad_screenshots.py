"""Pad screenshots to an exact 9:16 (portrait) or 16:9 (landscape) ratio for Google Play."""
import math, sys
from pathlib import Path
from PIL import Image

for path in sorted(Path(sys.argv[1]).glob("*.png")):
    im = Image.open(path).convert("RGB")
    w, h = im.size
    if h >= w:
        k = math.ceil(max(w / 9, h / 16)); W, H = 9 * k, 16 * k
    else:
        k = math.ceil(max(w / 16, h / 9)); W, H = 16 * k, 9 * k
    if (W, H) != (w, h):
        x0, y0 = (W - w) // 2, (H - h) // 2
        # Extend each edge with its own color: status bar on top, app background elsewhere.
        canvas = Image.new("RGB", (W, H), im.getpixel((w // 2, h - 2)))
        if y0:
            canvas.paste(im.getpixel((w // 2, 2)), (0, 0, W, y0 + 1))
        if x0:
            canvas.paste(im.getpixel((2, h // 2)), (0, y0 + 60, x0, H))
            canvas.paste(im.getpixel((w - 3, h // 2)), (x0 + w, y0 + 60, W, H))
            canvas.paste(im.getpixel((w // 2, 2)), (0, 0, W, y0 + 60))
        canvas.paste(im, (x0, y0))
        im = canvas
    im.save(path, optimize=True)
    print(f"{path.name}: {w}x{h} -> {W}x{H}")
