"""Draws the desktop app's icons (same shapes as the Android vector icons in app/src/main/res).

Run from the repo root:  python desktop/icons/make_icons.py
Writes desktop/src/main/resources/icon-playdown.png and icon-midnight.png (window/taskbar icons)
and desktop/icons/VideoDownloader.ico (the .exe icon, Play-down). Needs Pillow.
"""
from PIL import Image, ImageDraw

BIG = 1024           # drawn large, then shrunk, for smooth edges
VIEW = (10, 98)      # the part of Android's 108-unit canvas the tile shows
RADIUS = 0.24        # tile corner, as a share of its size


def s(v):
    """Android canvas units -> pixels on the big image."""
    return (v - VIEW[0]) * BIG / (VIEW[1] - VIEW[0])


def tile_mask():
    m = Image.new("L", (BIG, BIG), 0)
    ImageDraw.Draw(m).rounded_rectangle((0, 0, BIG - 1, BIG - 1), radius=int(BIG * RADIUS), fill=255)
    return m


def gradient(c1, c2, diagonal=True, y0=0.0, y1=1.0):
    """A colour fade from c1 to c2: top-left to bottom-right, or top to bottom between y0 and y1."""
    g = Image.new("RGB", (BIG, BIG))
    px = g.load()
    for y in range(BIG):
        for x in range(BIG):
            t = (x + y) / (2 * BIG) if diagonal else min(1, max(0, (y / BIG - y0) / (y1 - y0)))
            px[x, y] = tuple(round(a + (b - a) * t) for a, b in zip(c1, c2))
    return g


def round_line(d, points, width, fill=255):
    """A polyline with round caps and joins."""
    w = s(width) - s(0)
    pts = [(s(x), s(y)) for x, y in points]
    d.line(pts, fill=fill, width=int(w))
    for x, y in pts:
        d.ellipse((x - w / 2, y - w / 2, x + w / 2, y + w / 2), fill=fill)


def play_down():
    img = Image.new("RGBA", (BIG, BIG), (0, 0, 0, 0))
    img.paste(gradient((0x7B, 0x2F, 0xF7), (0xF1, 0x07, 0xA3)), mask=tile_mask())
    d = ImageDraw.Draw(img)
    white = (255, 255, 255, 255)
    tri = [(36, 36), (72, 36), (54, 62)]
    d.polygon([(s(x), s(y)) for x, y in tri], fill=white)
    round_line(d, tri + [tri[0]], 8, white)
    d.rounded_rectangle((s(36), s(70), s(72), s(77)), radius=(s(77) - s(70)) / 2, fill=white)
    return img


def midnight():
    img = Image.new("RGBA", (BIG, BIG), (0, 0, 0, 0))
    img.paste(Image.new("RGB", (BIG, BIG), (0x0F, 0x17, 0x2A)), mask=tile_mask())
    strokes = Image.new("L", (BIG, BIG), 0)
    d = ImageDraw.Draw(strokes)
    round_line(d, [(54, 32), (54, 59)], 9)
    round_line(d, [(42, 48), (54, 60), (66, 48)], 9)
    # The tray: down, a rounded corner, across, a rounded corner, up.
    w = int(s(7) - s(0))
    round_line(d, [(36, 66), (36, 69)], 7)
    round_line(d, [(72, 66), (72, 69)], 7)
    round_line(d, [(42, 75), (66, 75)], 7)
    # Pillow draws an arc's width inwards from its box, so the box is radius 6 + half the width.
    d.arc((s(32.5), s(59.5), s(51.5), s(78.5)), 90, 180, fill=255, width=w)
    d.arc((s(56.5), s(59.5), s(75.5), s(78.5)), 0, 90, fill=255, width=w)
    top, bottom = s(27) / BIG, s(80) / BIG
    img.paste(gradient((0xA3, 0xE6, 0x35), (0x22, 0xD3, 0xEE), diagonal=False, y0=top, y1=bottom), mask=strokes)
    return img


if __name__ == "__main__":
    res = "desktop/src/main/resources"
    pd = play_down()
    pd.resize((256, 256), Image.LANCZOS).save(f"{res}/icon-playdown.png")
    midnight().resize((256, 256), Image.LANCZOS).save(f"{res}/icon-midnight.png")
    pd.resize((256, 256), Image.LANCZOS).save(
        "desktop/icons/VideoDownloader.ico", sizes=[(16, 16), (24, 24), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)])
    print("ok")
