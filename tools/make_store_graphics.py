"""Render Google Play listing graphics for WhoStarts and WhoStarts Pro.

Outputs (in launch/graphics/):
  icon-512.png                      hi-res store icon, matches the launcher icon
  feature-graphic-free.png          1024x500 feature graphic for WhoStarts
  feature-graphic-pro.png           1024x500 feature graphic for WhoStarts Pro
  phone-framed/N-*.png              1080x1920 phone screenshots with captions

Usage: python3 tools/make_store_graphics.py
Needs Pillow and the Inter / Inter Display fonts (falls back to DejaVu Sans Bold).
"""
import random
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "launch" / "graphics"
SHOTS = ROOT / "store" / "screenshots" / "phone"

FELT_DARK = (11, 47, 34)
FELT = (20, 67, 47)
FELT_LIGHT = (28, 90, 61)
RAIL = (9, 42, 30)
BRASS = (201, 162, 75)
BRASS_LIGHT = (224, 192, 112)
CREAM = (243, 239, 226)
BALL = (26, 26, 26)
SS = 4  # supersampling factor for smooth edges


def font(size, weight="Bold", display=True):
    fam = "InterDisplay" if display else "Inter"
    for p in (f"/usr/share/fonts/opentype/inter/{fam}-{weight}.otf",
              f"/usr/share/fonts/opentype/inter/Inter-{weight}.otf",
              "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"):
        if Path(p).exists():
            return ImageFont.truetype(p, size)
    return ImageFont.load_default()


def felt(w, h, seed=7):
    """Radial felt gradient with a little fibre noise, so it doesn't look flat."""
    small = Image.new("RGB", (w // 4, h // 4))
    px = small.load()
    cx, cy = small.width * 0.42, small.height * 0.5
    rmax = ((small.width) ** 2 + (small.height) ** 2) ** 0.5 * 0.6
    for y in range(small.height):
        for x in range(small.width):
            t = min(1.0, (((x - cx) ** 2 + (y - cy) ** 2) ** 0.5) / rmax)
            t = t ** 1.4
            px[x, y] = tuple(int(FELT_LIGHT[i] * (1 - t) + FELT_DARK[i] * t) for i in range(3))
    img = small.resize((w, h), Image.BICUBIC)
    rnd = random.Random(seed)
    noise = Image.new("L", (w, h))
    noise.putdata([rnd.randint(118, 138) for _ in range(w * h)])
    noise = noise.filter(ImageFilter.GaussianBlur(0.6))
    return Image.blend(img, Image.merge("RGB", (noise, noise, noise)), 0.06)


def draw_eight_ball(img, cx, cy, r, shadow=True):
    """The app's eight-ball mark (same proportions as ic_launcher_foreground.xml)."""
    big = Image.new("RGBA", (img.width * SS, img.height * SS), (0, 0, 0, 0))
    d = ImageDraw.Draw(big)
    k = r * SS / 30.0  # launcher vector uses a radius-30 ball
    X, Y = cx * SS, cy * SS

    def ell(dx, dy, rx, ry, fill):
        # Draw each shape on its own layer and composite, so translucent fills
        # (the gloss) blend with the ball instead of punching through to the felt.
        nonlocal big
        layer = Image.new("RGBA", big.size, (0, 0, 0, 0))
        ImageDraw.Draw(layer).ellipse([X + (dx - rx) * k, Y + (dy - ry) * k, X + (dx + rx) * k, Y + (dy + ry) * k], fill=fill)
        big = Image.alpha_composite(big, layer)

    if shadow:
        sh = Image.new("RGBA", big.size, (0, 0, 0, 0))
        ImageDraw.Draw(sh).ellipse([X - 27 * k, Y + (32 - 7) * k, X + 27 * k, Y + (32 + 7) * k], fill=(0, 0, 0, 110))
        big = Image.alpha_composite(big, sh.filter(ImageFilter.GaussianBlur(3 * k)))
    ell(0, 0, 30, 30, BALL + (255,))
    ell(-11, -14, 8, 5, (255, 255, 255, 51))
    ell(0, 0, 14, 14, CREAM + (255,))
    ell(0, -6, 5.5, 5, BALL + (255,))
    ell(0, 6.5, 6.5, 6, BALL + (255,))
    small = big.resize(img.size, Image.LANCZOS)
    img.paste(small, (0, 0), small)


def draw_cue_ball(img, cx, cy, r):
    big = Image.new("RGBA", (img.width * SS, img.height * SS), (0, 0, 0, 0))
    d = ImageDraw.Draw(big)
    X, Y, R = cx * SS, cy * SS, r * SS
    sh = Image.new("RGBA", big.size, (0, 0, 0, 0))
    ImageDraw.Draw(sh).ellipse([X - R * 0.9, Y + R * 0.85, X + R * 0.9, Y + R * 1.2], fill=(0, 0, 0, 100))
    big = Image.alpha_composite(big, sh.filter(ImageFilter.GaussianBlur(R * 0.1)))
    d = ImageDraw.Draw(big)
    d.ellipse([X - R, Y - R, X + R, Y + R], fill=(236, 231, 214, 255))
    d.ellipse([X - R * 0.92, Y - R * 0.95, X + R * 0.8, Y + R * 0.7], fill=CREAM + (255,))
    gloss = Image.new("RGBA", big.size, (0, 0, 0, 0))
    ImageDraw.Draw(gloss).ellipse([X - R * 0.62, Y - R * 0.66, X - R * 0.16, Y - R * 0.34], fill=(255, 255, 255, 170))
    big = Image.alpha_composite(big, gloss)
    small = big.resize(img.size, Image.LANCZOS)
    img.paste(small, (0, 0), small)


def diamond(d, cx, cy, s, fill):
    d.polygon([(cx, cy - s), (cx + s, cy), (cx, cy + s), (cx - s, cy)], fill=fill)


def rails(img, band):
    """Dark rails top and bottom with brass sight diamonds, like a table edge."""
    d = ImageDraw.Draw(img)
    w, h = img.size
    for y0 in (0, h - band):
        d.rectangle([0, y0, w, y0 + band], fill=RAIL)
    d.line([0, band, w, band], fill=BRASS, width=2)
    d.line([0, h - band - 1, w, h - band - 1], fill=BRASS, width=2)
    for i in range(1, 8):
        x = w * i / 8
        diamond(d, x, band / 2, band * 0.22, BRASS)
        diamond(d, x, h - band / 2, band * 0.22, BRASS)


def icon():
    """512x512 store icon: the launcher icon's visible area, scaled up."""
    S = 512
    big = Image.new("RGB", (S * SS, S * SS), FELT_DARK)
    d = ImageDraw.Draw(big)
    # Launcher viewport is 108 units; the visible (unmasked) part is about 16..92.
    v0, v1 = 16.0, 92.0
    k = S * SS / (v1 - v0)

    def m(x, y):
        return ((x - v0) * k, (y - v0) * k)

    d.rectangle([*m(0, 0), *m(108, 14)], fill=(9, 42, 30))
    d.rectangle([*m(0, 94), *m(108, 108)], fill=(9, 42, 30))
    img = big.resize((S, S), Image.LANCZOS)
    draw_eight_ball(img, (54 - v0) * S / (v1 - v0), (54 - v0) * S / (v1 - v0), 30 * S / (v1 - v0))
    img.save(OUT / "icon-512.png", optimize=True)


def feature(pro):
    W, H = 1024, 500
    img = felt(W, H, seed=11 if pro else 7)
    rails(img, 34)
    draw_cue_ball(img, 300, 300, 62)
    draw_eight_ball(img, 205, 250, 118)
    d = ImageDraw.Draw(img)
    tx, ty = 382, 150
    title = font(104)
    if pro:  # shrink the title until the PRO pill fits inside the right margin
        size = 104
        while size > 60:
            title = font(size)
            if tx + d.textlength("WhoStarts", font=title) + 18 + d.textlength("PRO", font=font(40)) + 36 <= W - 36:
                break
            size -= 2
    d.text((tx + 3, ty + 4), "WhoStarts", font=title, fill=(0, 0, 0))
    d.text((tx, ty), "WhoStarts", font=title, fill=CREAM)
    tw = d.textlength("WhoStarts", font=title)
    if pro:
        pf = font(40)
        px0 = tx + tw + 18
        bbox = d.textbbox((0, 0), "WhoStarts", font=title)
        py0 = ty + (bbox[1] + bbox[3]) / 2 - 31
        pw = d.textlength("PRO", font=pf) + 36
        d.rounded_rectangle([px0, py0, px0 + pw, py0 + 62], radius=31, fill=BRASS)
        pb = d.textbbox((0, 0), "PRO", font=pf)
        d.text((px0 + 18, py0 + 31 - (pb[1] + pb[3]) / 2), "PRO", font=pf, fill=(36, 26, 0))
    sub = font(38, "SemiBold")
    d.text((tx + 4, ty + 138), "Track the rivalry.", font=sub, fill=BRASS_LIGHT)
    d.text((tx + 4, ty + 188), "Flip for the break." if not pro else "No ads. Just pool.", font=sub, fill=BRASS_LIGHT)
    img.save(OUT / f"feature-graphic-{'pro' if pro else 'free'}.png", optimize=True)


def rounded(im, r):
    mask = Image.new("L", (im.width * SS, im.height * SS), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, mask.width - 1, mask.height - 1], radius=r * SS, fill=255)
    mask = mask.resize(im.size, Image.LANCZOS)
    out = Image.new("RGBA", im.size)
    out.paste(im.convert("RGBA"), (0, 0), mask)
    return out


CAPTIONS = [
    ("1-matchups.png", "Every rivalry,", "one tap away"),
    ("2-history.png", "See who's", "really on top"),
    ("3-coin-flip.png", "Settle the break", "with a coin flip"),
    ("4-wheel-spin.png", "A whole crew?", "Spin the wheel"),
]


def framed():
    out = OUT / "phone-framed"
    out.mkdir(parents=True, exist_ok=True)
    W, H = 1080, 1920
    for i, (name, l1, l2) in enumerate(CAPTIONS):
        src = SHOTS / name
        if not src.exists():
            continue
        img = felt(W, H, seed=20 + i).convert("RGBA")
        d = ImageDraw.Draw(img)
        f = font(84)
        for j, (line, col) in enumerate(((l1, CREAM), (l2, BRASS))):
            lw = d.textlength(line, font=f)
            d.text(((W - lw) / 2, 120 + j * 100), line, font=f, fill=col)
        shot = Image.open(src).convert("RGB")
        sw = int(W * 0.74)
        sh_ = int(shot.height * sw / shot.width)
        shot = rounded(shot.resize((sw, sh_), Image.LANCZOS), 44)
        x0, y0 = (W - sw) // 2, 370
        shadow = Image.new("RGBA", img.size, (0, 0, 0, 0))
        ImageDraw.Draw(shadow).rounded_rectangle([x0 + 6, y0 + 18, x0 + sw + 6, y0 + sh_ + 18], radius=44, fill=(0, 0, 0, 150))
        img = Image.alpha_composite(img, shadow.filter(ImageFilter.GaussianBlur(22)))
        border = Image.new("RGBA", img.size, (0, 0, 0, 0))
        ImageDraw.Draw(border).rounded_rectangle([x0 - 3, y0 - 3, x0 + sw + 3, y0 + sh_ + 3], radius=47, outline=BRASS + (200,), width=3)
        img = Image.alpha_composite(img, border)
        img.paste(shot, (x0, y0), shot)
        img.convert("RGB").save(out / name, optimize=True)


if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    icon()
    feature(False)
    feature(True)
    framed()
    for p in sorted(OUT.rglob("*.png")):
        print(p.relative_to(ROOT), Image.open(p).size)
