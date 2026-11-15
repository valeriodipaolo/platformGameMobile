#!/usr/bin/env python3
"""Fogliame realistico per autunno e primavera: foglie lobate o ovate con nervature, sfumature, ombre portate e variazioni di colore.

Rigenera r3a_foliage / r3a_crown / r3a_tree (autunno) e r3d_foliage / r3d_crown / r3d_tree (primavera: fiori di ciliegio tra foglie tenere).
Le texture restano chiare e calde: in gioco le tinge il colore dei vertici. Uso:  python3 tools/gen_foliage.py
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_assets3d as G3  # noqa: E402

SS = 2


def leaf_sprite(kind, size, rng, tone):
    """Foglia singola, RGBA a 4x: kind 0 ovata (faggio), 1 lobata (quercia), 2 a cinque punte (acero). Punta a destra."""
    S = int(size * 4)
    W, H = S, int(S * 0.8)
    cx, cy = W * 0.5, H * 0.5
    im = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    pts = []
    n = 90
    for i in range(n):
        t = i / n * 2 * math.pi
        c, s = math.cos(t), math.sin(t)
        if kind == 0:                                   # ovata, punta aguzza, base tonda
            rx = 0.5 * (1 + 0.18 * c) * (1 - 0.12 * abs(s) ** 3)
            ry = 0.3 * (1 - 0.3 * c) * (1 + 0.04 * math.sin(t * 14))
            r_ = 1.0
            x, y = c * rx * W * 0.95, s * ry * H * 0.95
            if c > 0.8: x += (c - 0.8) * 0.5 * W * 0.3
        elif kind == 1:                                 # lobi tondi alternati
            lobes = 0.55 + 0.45 * abs(math.sin(t * 3.5)) ** 0.7
            x = c * W * 0.47 * (0.9 + 0.1 * lobes)
            y = s * H * 0.46 * lobes * (1 - 0.25 * max(0, -c))
        else:                                           # acero: cinque punte
            ang = t
            k = 0.5 + 0.5 * abs(math.cos(2.5 * ang)) ** 1.6
            x = c * W * 0.46 * k
            y = s * H * 0.5 * k
        pts.append((cx + x, cy + y))
    d = ImageDraw.Draw(im)
    base = np.array([255, 238, 150]) * tone
    d.polygon(pts, fill=tuple(int(min(255, v)) for v in base) + (255,))
    # sfumatura lungo la foglia e nervature
    arr = np.asarray(im).astype(float)
    a = arr[..., 3] / 255.0
    xs = np.linspace(0, 1, W)[None, :]
    ys = np.linspace(-1, 1, H)[:, None]
    shade = 0.80 + 0.28 * xs - 0.16 * np.abs(ys)
    arr[..., :3] *= shade[..., None]
    vein = np.zeros((H, W))
    vi = Image.new("L", (W, H), 0)
    vd = ImageDraw.Draw(vi)
    vd.line([(W * 0.06, cy), (W * 0.9, cy)], fill=255, width=max(2, S // 28))
    for k in range(1, 6):
        x0 = W * (0.12 + 0.14 * k)
        for sgn in (-1, 1):
            vd.line([(x0, cy), (x0 + W * 0.18, cy + sgn * H * 0.34)], fill=255, width=max(1, S // 60))
    vein = np.asarray(vi.filter(ImageFilter.GaussianBlur(S / 90))).astype(float) / 255.0
    arr[..., :3] *= (1 - 0.32 * vein)[..., None]
    arr[..., 3] = a * 255
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")


def petal_flower(size, rng, tone):
    """Fiore di ciliegio visto da davanti: cinque petali con incavo, centro con stami."""
    S = int(size * 4)
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    c = S / 2
    ph = rng.uniform(0, 6.28)
    for i in range(5):
        a = ph + i * 2 * math.pi / 5
        pts = []
        for j in range(24):
            t = j / 23 * math.pi
            r = S * 0.30 * math.sin(t)
            x = math.cos(a) * (S * 0.04 + S * 0.40 * (1 - math.cos(t)) / 2) + math.cos(a + math.pi / 2) * r * 0.55
            y = math.sin(a) * (S * 0.04 + S * 0.40 * (1 - math.cos(t)) / 2) + math.sin(a + math.pi / 2) * r * 0.55
            pts.append((c + x, c + y))
        d.polygon(pts, fill=tuple(int(min(255, v * tone)) for v in (255, 240, 244)) + (255,))
        d.line([(c, c), (c + math.cos(a) * S * 0.36, c + math.sin(a) * S * 0.36)], fill=(235, 190, 205, 255), width=max(1, S // 70))
    for i in range(10):
        a = rng.uniform(0, 6.28)
        r = S * rng.uniform(0.04, 0.12)
        d.ellipse([c + math.cos(a) * r - S * 0.02, c + math.sin(a) * r - S * 0.02, c + math.cos(a) * r + S * 0.02, c + math.sin(a) * r + S * 0.02], fill=(240, 196, 90, 255))
    arr = np.asarray(im).astype(float)
    yy, xx = np.mgrid[0:S, 0:S]
    rr = np.hypot(xx - c, yy - c) / (S / 2)
    arr[..., :3] *= (0.78 + 0.3 * np.clip(rr * 1.4, 0, 1))[..., None] * np.array([1, 0.97, 0.98])
    arr[..., :3] = np.where(rr[..., None] < 0.18, arr[..., :3] * np.array([1, 0.78, 0.55]), arr[..., :3])
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")


def paste_wrapped(canvas, sprite, x, y, ang, wrap):
    sp = sprite.rotate(math.degrees(ang), expand=True, resample=Image.BICUBIC)
    w, h = canvas.size
    a = sp.getchannel("A")
    sh = a.filter(ImageFilter.GaussianBlur(3.5 * SS))
    sh = sh.point(lambda v: int(v * 0.5))
    offs = [(0, 0)] if not wrap else [(ox, oy) for ox in (-w, 0, w) for oy in (-h, 0, h)]
    for ox, oy in offs:
        px, py = int(x * SS + ox - sp.width / 2), int(y * SS + oy - sp.height / 2)
        if px > w or py > h or px + sp.width < 0 or py + sp.height < 0:
            continue
        canvas.paste((18, 14, 8, 255), (px + 3 * SS, py + 4 * SS), sh)        # ombra portata
        canvas.paste(sp, (px, py), sp)


def down(sp, size):
    return sp.resize((max(1, int(sp.width * size / (sp.width / 4 / SS * SS) / 4 * 1.0)), max(1, int(sp.height * size / (sp.width / 4) / 4))), Image.LANCZOS)


def build_sprites(rng, count, size, kinds, flowers=False):
    out = []
    for i in range(count):
        tone = rng.uniform(0.78, 1.06)
        if flowers:
            out.append(petal_flower(size, rng, tone))
        else:
            out.append(leaf_sprite(kinds[i % len(kinds)], size, rng, tone))
    return out


def mosaic(name, W, count, size, kinds, seed, bg, flowers=False):
    """Mosaico opaco e ripetibile per le chiome tridimensionali."""
    rng = np.random.default_rng(seed)
    sprites = build_sprites(rng, 24, size * SS, kinds, flowers)
    canvas = Image.new("RGBA", (W * SS, W * SS), bg + (255,))
    for i in range(count):
        sp = sprites[int(rng.integers(len(sprites)))]
        sc = rng.uniform(0.8, 1.2) / 4
        sp2 = sp.resize((max(2, int(sp.width * sc)), max(2, int(sp.height * sc))), Image.LANCZOS)
        paste_wrapped(canvas, sp2, rng.uniform(0, W), rng.uniform(0, W), rng.uniform(0, 6.28), True)
    out = canvas.resize((W, W), Image.LANCZOS).convert("RGBA")
    arr = np.asarray(out).astype(float) / 255.0
    G3.save(name, np.dstack([arr[..., :3], np.ones((W, W))]))


def card(name, W, H, region, count, size, kinds, seed, trunk=None, flowers=False):
    """Scheda con trasparenza (grappolo di foglie o fiori) per cartoncini sul contorno e per gli alberi lontani."""
    rng = np.random.default_rng(seed)
    sprites = build_sprites(rng, 24, size * SS, kinds, flowers)
    canvas = Image.new("RGBA", (W * SS, H * SS), (0, 0, 0, 0))
    if trunk:
        trunk(ImageDraw.Draw(canvas), SS)
    pts = []
    while len(pts) < count:
        x, y = rng.uniform(0, W), rng.uniform(0, H)
        if region(x, y, rng):
            pts.append((x, y))
    pts.sort(key=lambda p: p[1])
    for x, y in pts:
        sp = sprites[int(rng.integers(len(sprites)))]
        up = 1.0 - y / H
        k = rng.uniform(0.82, 1.1) * (0.72 + 0.4 * up)
        sc = rng.uniform(0.85, 1.15) / 4
        sp2 = sp.resize((max(2, int(sp.width * sc)), max(2, int(sp.height * sc))), Image.LANCZOS)
        arr = np.asarray(sp2).astype(float)
        arr[..., :3] *= k
        sp2 = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")
        paste_wrapped(canvas, sp2, x, y, rng.uniform(0, 6.28), False)
    G3.finish_card(canvas, name, SS)


def trunk_fn(d, ss):
    d.polygon([(116 * ss, 512 * ss), (140 * ss, 512 * ss), (134 * ss, 270 * ss), (122 * ss, 270 * ss)], fill=(96, 74, 54, 255))
    for (x0, y0, x1, y1) in ((128, 330, 72, 230), (128, 310, 190, 220), (128, 280, 128, 150)):
        d.line([(x0 * ss, y0 * ss), (x1 * ss, y1 * ss)], fill=(90, 68, 50, 255), width=5 * ss)


def crown_region(x, y, r):
    q = ((x - 128) / 118) ** 2 + ((y - 128) / 112) ** 2
    return q < 1.0 and r.random() < 0.95 - 0.3 * q


def tree_region(x, y, r):
    q = ((x - 128) / 122) ** 2 + ((y - 190) / 170) ** 2
    return q < 1.0 and y < 340 and r.random() < 0.97 - 0.25 * q


def main():
    kinds = [0, 1, 2, 1, 0]
    mosaic("r3a_foliage", 256, 700, 30, kinds, 477, (40, 30, 14))
    card("r3a_crown", 256, 256, crown_region, 360, 32, kinds, 421)
    card("r3a_tree", 256, 512, tree_region, 900, 28, kinds, 431, trunk_fn)
    mosaic("r3d_foliage", 256, 900, 17, [0], 1241, (70, 90, 40), flowers=True)
    card("r3d_crown", 256, 256, crown_region, 420, 18, [0], 1221, flowers=True)
    card("r3d_tree", 256, 512, tree_region, 1100, 16, [0], 1231, trunk_fn, flowers=True)
    print("ok")


if __name__ == "__main__":
    main()
