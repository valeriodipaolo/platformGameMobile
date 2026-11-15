#!/usr/bin/env python3
"""Immagini dei livelli del cielo (livelli 11 e 12): nuvole illuminate dal sole, cielo radioso, la montagna in lontananza (2D e 3D).

Uso:  python3 tools/gen_heaven.py [nome ...]      (nessun nome = tutto)
Richiede numpy, scipy, Pillow e gli script gen_assets.py / gen_assets3d.py / gen_autumn.py / gen_spring.py accanto a questo file.
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw
from scipy import ndimage as ndi

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_assets as G  # noqa: E402
import gen_assets3d as G3  # noqa: E402
import gen_autumn as A  # noqa: E402
from gen_assets import fbm, fbm1, lerp, smooth, Cv, T, down  # noqa: E402

WARM = np.array([1.0, 0.975, 0.90])
LIGHT = np.array([1.0, 0.99, 0.96])
MID = np.array([0.90, 0.93, 0.99])
SHADE = np.array([0.70, 0.76, 0.92])
PINK = np.array([1.0, 0.86, 0.90])


def col(c):
    return np.array(c, dtype=float) / 255.0


def circle(d, x, y, r, fill):
    d.ellipse([x - r, y - r, x + r, y + r], fill=fill)


def lumps(n, seed, count, rmin, rmax, wrap=True, ymin=0.0, ymax=1.0):
    """Campo d'altezza di rigonfiamenti morbidi (periodico) da cui si ricava una nuvola illuminata dall'alto a sinistra."""
    r = np.random.default_rng(seed)
    y, x = np.mgrid[0:n, 0:n].astype(float)
    h = np.zeros((n, n))
    for _ in range(count):
        cx, cy = r.uniform(0, n), r.uniform(ymin * n, ymax * n)
        rad = r.uniform(rmin, rmax) * n
        for ox in (-n, 0, n):
            for oy in (-n, 0, n):
                if not wrap and (ox or oy):
                    continue
                d2 = ((x - cx - ox) ** 2 + (y - cy - oy) ** 2) / (rad * rad)
                h = np.maximum(h, np.sqrt(np.clip(1 - d2, 0, None)))
    return h


def cloud_rgb(h, seed, contrast=1.0, warm=1.0):
    """Colore di una nuvola dal campo d'altezza: bianco caldo dove la luce arriva, azzurro-lilla nelle conche, sfumature rosa."""
    hs = ndi.gaussian_filter(h, 2.6 * h.shape[0] / 192.0, mode="wrap")
    gy, gx = np.gradient(hs)
    lit = np.clip(0.66 + (-gx * 0.9 - gy * 1.3) * 5.0 * contrast * (h.shape[0] / 192.0) + hs * 0.26, 0, 1)
    pk = np.clip(fbm(h.shape[0], h.shape[1], beta=2.4, seed=seed) - 0.5, -0.5, 0.5) * 2
    rgb = lerp(SHADE, MID, np.clip(lit * 1.5, 0, 1)[..., None])
    rgb = lerp(rgb, LIGHT * (1 - 0.06 * warm + 0.06 * warm * WARM / WARM), np.clip((lit - 0.55) * 2.2, 0, 1)[..., None])
    rgb = rgb + (PINK - 1.0) * (np.clip(pk, 0, 1) * 0.22 * (1 - np.clip(lit, 0, 1)))[..., None]
    rgb = rgb * (1 + (WARM - 1.0) * np.clip(lit - 0.4, 0, 1)[..., None] * warm)
    return np.clip(rgb, 0, 1)


def sparkles(rgb, count, seed, rad=(1.0, 2.2), ss=2):
    h, w = rgb.shape[:2]
    im = Image.fromarray((np.clip(rgb, 0, 1) * 255).astype(np.uint8), "RGB").resize((w * ss, h * ss), Image.LANCZOS)
    d = ImageDraw.Draw(im)
    r = np.random.default_rng(seed)
    for _ in range(count):
        x, y = r.uniform(0, w), r.uniform(0, h)
        rr = r.uniform(*rad)
        for ox in (-w, 0, w):
            for oy in (-h, 0, h):
                if -rr <= x + ox <= w + rr and -rr <= y + oy <= h + rr:
                    circle(d, (x + ox) * ss, (y + oy) * ss, rr * ss, (255, 238, 170))
                    circle(d, (x + ox) * ss, (y + oy) * ss, rr * ss * 0.45, (255, 255, 240))
    return np.asarray(im.resize((w, h), Image.LANCZOS)).astype(float) / 255.0


# ------------------------------------------------------------------ 2D: sfondi
def hv_sky():
    w, h = 1536, 768
    y = np.linspace(0, 1, h)[:, None]
    x = np.linspace(0, 1, w, endpoint=False)[None, :]
    top, mid, hor = col((38, 128, 236)), col((126, 200, 252)), col((255, 244, 214))
    t = y ** 0.9
    grad = np.where(t[..., None] < 0.62, lerp(top, mid, (t / 0.62)[..., None]), lerp(mid, hor, ((t - 0.62) / 0.38)[..., None]))
    sky = np.broadcast_to(grad, (h, w, 3)).copy()
    r = np.sqrt(((y - 0.22) * 1.0) ** 2 + ((x - 0.28) * 2.0) ** 2)
    sky += col((255, 236, 170)) * (np.exp(-r * 4.2) * 0.45)[..., None]            # alone del sole
    sky += col((255, 250, 220)) * (np.exp(-r * 14) * 0.5)[..., None]
    den = ndi.gaussian_filter(fbm(h, w, beta=3.4, seed=2101, ax=2.4, ay=1.0), (3, 3), mode="wrap")
    den2 = ndi.gaussian_filter(fbm(h, w, beta=2.6, seed=2102, ax=1.6, ay=1.0), (2, 2), mode="wrap")
    c = smooth((den + 0.25 * (den2 - 0.5) - (0.60 - 0.1 * y)) * 5.0) * (0.30 + 0.6 * y)
    dn = ndi.gaussian_filter(den, 6)
    lit = np.clip(0.95 + (np.roll(dn, 8, 1) - np.roll(dn, -8, 1)) * 14 + (den2 - 0.5) * 0.2, 0.8, 1.1)
    cloud = lerp(col((222, 230, 248)), col((255, 252, 244)), np.clip((lit - 0.8) / 0.3, 0, 1)[..., None])
    a = np.clip(c, 0, 0.95)[..., None]
    sky = sky * (1 - a) + cloud * a
    G.save("hv_sky", np.clip(sky, 0, 1))


def hv_mountains():
    """Le montagne dell'estate, viste da lontano e da molto in alto: più chiare, avvolte dalla foschia dorata."""
    a = A.capture(G, G.gen_mountains)
    rgb = a[..., :3]
    h = rgb.shape[0]
    f = (np.linspace(0, 1, h)[:, None, None]) ** 1.4
    rgb = lerp(rgb * np.array([1.0, 1.03, 1.1]), col((226, 232, 248)), 0.26 + 0.34 * f)
    rgb = rgb * np.array([1.03, 1.0, 0.96])
    G.save("hv_mountains", np.dstack([np.clip(rgb, 0, 1), a[..., 3]]))


def cloud_bank(d, w, ss, base_y, lumps_n, rmin, rmax, lit, shade, rng, spread):
    """Banco di nuvole: cumuli sovrapposti, ognuno con la sua ombra azzurrina sotto e la luce sopra (periodico in orizzontale)."""
    items = []
    for _ in range(lumps_n):
        x = rng.uniform(0, w)
        rad = rng.uniform(rmin, rmax)
        y = base_y + rng.uniform(-spread, spread) - rad * 0.2
        items.append((y, x, rad))
    items.sort()
    for y, x, rad in items:
        for off in (-w, 0, w):
            if -rad <= x + off <= w + rad:
                circle(d, (x + off) * ss, (y + rad * 0.10) * ss, rad * ss, shade)
                circle(d, (x + off) * ss, (y - rad * 0.06) * ss, rad * 0.93 * ss, lit)


def hv_hills():
    h, w, ss = 560, 2048, 2
    im = Image.new("RGBA", (w * ss, h * ss), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    r = np.random.default_rng(2111)
    layers = [(120, 46, 22, 56, (236, 243, 252, 255), (206, 222, 244, 255), 70),
              (230, 60, 30, 76, (246, 248, 254, 255), (206, 214, 240, 255), 70),
              (360, 72, 40, 100, (255, 252, 246, 255), (196, 204, 236, 255), 64)]
    for base, spread, rmin, rmax, lit, shade, n in layers:
        d.rectangle([0, (base + spread * 0.4) * ss, w * ss, h * ss], fill=shade)
        cloud_bank(d, w, ss, base, n, rmin, rmax, lit, shade, r, spread)
        d.rectangle([0, (base + spread * 0.9 + rmax * 0.5) * ss, w * ss, h * ss], fill=lit)
    arr = np.asarray(im.resize((w, h), Image.LANCZOS)).astype(float) / 255.0
    yy = np.linspace(0, 1, h)[:, None, None]
    glow = np.clip(1.0 - yy * 1.2, 0, 1) * 0.35                                  # velo dorato sui piani lontani
    arr[..., :3] = np.clip(lerp(arr[..., :3], col((255, 240, 200)), glow * 0.5), 0, 1)
    G.save("hv_hills", arr)


# ------------------------------------------------------------------ 2D: terreno di nuvole
def hv_ground():
    s = 2
    n = T * s
    h = lumps(n, 2131, 22, 0.10, 0.22)
    body = cloud_rgb(h, 2132, 1.0)
    body = body * (0.90 + 0.10 * np.linspace(1, 0.78, n)[:, None, None])           # più azzurro verso il basso
    h2 = lumps(n, 2133, 20, 0.09, 0.18)
    topc = cloud_rgb(h2, 2134, 0.8, 1.2)
    topc = lerp(topc, np.ones(3), 0.55)
    edge = 28 + 7 * (fbm1(n, beta=2.0, seed=2135) - 0.5)
    yy = np.arange(n)[:, None].astype(float)
    yb = edge[None, :] * s
    a = np.clip((yb - yy) / 2.0, 0, 1)
    shadow = 1 - 0.20 * np.exp(-np.clip(yy - yb, 0, None) / (7.0 * s)) * (1 - a)
    rgb = topc * a[..., None] + body * shadow[..., None] * (1 - a[..., None])
    rgb = sparkles(rgb, 16, 2136, (1.2, 2.4))
    t = down(rgb, s)
    dt = down(body, s)
    dt = sparkles(dt, 6, 2137, (1.0, 2.0), 3)
    G.save("hv_grass", t)
    G.save("hv_dirt", dt)
    for nm in ("l", "r"):
        G.save("hv_grass_" + nm, A.side_variant(t, nm, True))
        G.save("hv_dirt_" + nm, A.side_variant(dt, nm, False))
    fw, fh, ss = T, 28, 4
    im = Image.new("RGBA", (fw * ss, fh * ss), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    r = np.random.default_rng(2138)
    puffs = [(r.uniform(0, fw), fh - r.uniform(4, 9), r.uniform(6, 11)) for _ in range(13)]
    puffs.sort(key=lambda p: -p[1])
    for x, y, rad in puffs:
        for off in (-fw, 0, fw):
            circle(d, (x + off) * ss, (y + 1.2) * ss, rad * ss, (214, 224, 246, 255))
            circle(d, (x + off) * ss, y * ss, rad * 0.92 * ss, (255, 253, 247, 255))
    for _ in range(4):
        x, y = r.uniform(6, fw - 6), r.uniform(8, fh - 10)
        for off in (-fw, 0, fw):
            circle(d, (x + off) * ss, y * ss, 2.4 * ss, (255, 226, 130, 255))
            circle(d, (x + off) * ss, y * ss, 1.0 * ss, (255, 255, 236, 255))
    G.save("hv_fringe", np.asarray(im.resize((fw, fh), Image.LANCZOS)).astype(float) / 255.0)


def hv_blocks():
    n = T * 3
    gold_hi, gold_lo = (255, 226, 112), (224, 160, 48)
    cv = Cv(T, T, 3)
    tex = 0.9 + 0.14 * fbm(n, n, beta=1.6, seed=2151)
    cv.add(cv.rrect(3, 3, T - 3, T - 3, 26), gold_hi, bevel=17, spec=0.55, shin=24, tex=tex)
    cv.add(cv.capsule(14, T * 0.5, T - 14, T * 0.5, 1.6), gold_lo, bevel=1.2, spec=0.0)
    cv.add(cv.capsule(T * 0.5, 14, T * 0.5, T * 0.5 - 4, 1.4), gold_lo, bevel=1.2, spec=0.0)
    cv.add(cv.poly(G.star_pts(30, 70, 11, 5)), (255, 250, 232), bevel=2, spec=0.6)
    G.save("hv_brick", cv.result())

    def crate(used):
        cv = Cv(T, T, 3)
        base = (255, 232, 140) if not used else (204, 208, 224)
        cv.add(cv.rrect(3, 3, T - 3, T - 3, 26), base, bevel=17, spec=0.5, shin=24, tex=0.92 + 0.12 * fbm(n, n, beta=1.5, seed=2152))
        st = cv.poly(G.star_pts(48, 50, 24, 10))
        if used:
            cv.add(st, (160, 168, 196), ao=0.3, bevel=3, spec=0.0)
        else:
            cv.glow(st, (255, 250, 200), radius=10, strength=0.9)
            cv.add(st, (255, 250, 214), ao=0.4, bevel=6, spec=0.9, shin=16)
            cv.add(cv.ell(48, 50, 6, 6), (255, 255, 255), bevel=3, spec=0.5)
        return cv.result()
    G.save("hv_bonus", crate(False))
    G.save("hv_used", crate(True))

    cv = Cv(T, T, 3)
    tex = 0.84 + 0.2 * fbm(n, n, beta=1.9, seed=2161) + 0.1 * (fbm(n, n, beta=0.5, seed=2162) - 0.5)
    cv.add(cv.rrect(1.5, 1.5, T - 1.5, T - 1.5, 30), (240, 240, 250), bevel=16, spec=0.4, shin=22, tex=tex)
    cv.add(cv.capsule(10, 22, T - 12, 70, 1.4), (232, 196, 110), bevel=1.0, spec=0.2)
    cv.add(cv.capsule(60, 10, 30, T - 12, 1.1), (232, 196, 110), bevel=1.0, spec=0.2)
    G.save("hv_stone", cv.result())


def hv_spring():
    """Trampolino di nuvola: molla dorata sotto, soffice cuscino bianco con una fascia d'arcobaleno come piano di rimbalzo."""
    cv = Cv(T, T, 3)
    cv.add(cv.rrect(0, 38, T, T, 5), (246, 248, 255), bevel=9, spec=0.3)
    for k in range(4):
        y = 52 + k * 10
        cv.add(cv.capsule(8, y, T - 8, y + 4, 2.2), (255, 206, 70), bevel=2, spec=0.55)
    cv.add(cv.ell(T / 2, 26, 46, 18), (255, 255, 255), bevel=10, spec=0.35, shin=16)
    for k, c in enumerate([(255, 110, 120), (255, 176, 90), (255, 232, 100), (140, 214, 120), (110, 180, 255), (170, 130, 240)]):
        cv.add(cv.capsule(14 + k * 3, 24 - k * 1.5, T - 14 - k * 3, 24 - k * 1.5, 1.7), c, bevel=1.2, spec=0.3)
    cv.add(cv.ell(T / 2, 14, 30, 7), (255, 255, 255), bevel=4, spec=0.3)
    G.save("hv_spring", cv.result())


def hv_crumble():
    """Nuvola evanescente: soffice e un po' più trasparente, con crepe dorate: cede un istante dopo il tocco."""
    n = T * 2
    h = lumps(n, 2171, 30, 0.07, 0.16)
    rgb = cloud_rgb(h, 2172, 1.1)
    rgb = lerp(rgb, col((220, 214, 246)), 0.25)
    im = Image.fromarray((np.clip(rgb, 0, 1) * 255).astype(np.uint8), "RGB")
    d = ImageDraw.Draw(im)
    for (x0, y0, x1, y1) in ((52, 20, 70, 84), (140, 28, 124, 96), (96, 120, 112, 176)):
        d.line([(x0, y0), (x1, y1)], fill=(246, 190, 80), width=4)
    t = down(np.asarray(im).astype(float) / 255.0, 2)
    yy, xx = np.mgrid[0:T, 0:T].astype(float)
    r = 26.0
    dx = np.maximum(np.maximum(r - xx, xx - (T - 1 - r)), 0)
    dy = np.maximum(np.maximum(r - yy, yy - (T - 1 - r)), 0)
    a = np.clip((r - np.sqrt(dx * dx + dy * dy)) / 1.5 + 0.5, 0, 1) * 0.94
    G.save("hv_crumble", np.dstack([t, a]))


def hv_sea():
    """Striscia di mare di nuvole ripetibile in orizzontale: cumuli chiari sopra e foschia azzurra sotto."""
    w, h, ss = 256, 96, 3
    im = Image.new("RGBA", (w * ss, h * ss), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    r = np.random.default_rng(2181)
    d.rectangle([0, 34 * ss, w * ss, h * ss], fill=(214, 224, 248, 255))
    cloud_bank(d, w, ss, 24, 20, 12, 22, (255, 253, 246, 255), (204, 216, 244, 255), r, 8)
    d.rectangle([0, 44 * ss, w * ss, h * ss], fill=(214, 226, 250, 255))
    arr = np.asarray(im.resize((w, h), Image.LANCZOS)).astype(float) / 255.0
    yy = np.linspace(0, 1, h)[:, None]
    arr[..., 3] *= np.clip(1.0 - 0.18 * yy, 0, 1)
    arr[..., :3] = lerp(arr[..., :3], col((186, 204, 242)), (np.clip(yy - 0.45, 0, 1) * 0.9)[..., None])
    G.save("hv_sea", arr)


def hv_spark():
    n = 48
    y, x = np.mgrid[0:n, 0:n].astype(float)
    u = (x - n / 2 + 0.5) / (n / 2)
    v = (y - n / 2 + 0.5) / (n / 2)
    r = np.sqrt(u * u + v * v)
    star = np.clip(1 - (np.abs(u) ** 0.6 + np.abs(v) ** 0.6) ** (1 / 0.6) * 1.1, 0, 1)
    glow = np.exp(-r * 4.0) * 0.5
    a = np.clip(star * 1.4 + glow, 0, 1)
    c = lerp(col((255, 214, 90)), col((255, 255, 240)), np.clip(star * 1.6, 0, 1)[..., None])
    G.save("hv_spark", np.dstack([c, a]))


# ------------------------------------------------------------------ 3D
def h_cloud():
    n = 512
    h = lumps(n, 2201, 70, 0.05, 0.12)
    rgb = cloud_rgb(h, 2202, 0.7, 1.0)
    rgb = lerp(rgb, np.ones(3), 0.35)
    rgb = sparkles(rgb, 130, 2203, (1.4, 2.8))
    G3.save("r3h_cloud", np.clip(rgb, 0, 1))


def h_body():
    n = 256
    h = lumps(n, 2211, 40, 0.08, 0.18)
    rgb = cloud_rgb(h, 2212, 1.1)
    rgb = lerp(rgb, col((196, 208, 242)), 0.25)
    G3.save("r3h_body", np.clip(rgb, 0, 1))


def h_sea():
    n = 512
    h = lumps(n, 2221, 60, 0.06, 0.14)
    rgb = cloud_rgb(h, 2222, 0.9, 1.2)
    rgb = lerp(rgb, col((206, 222, 250)), 0.2)
    rgb = sparkles(rgb, 60, 2223, (1.5, 3.0))
    G3.save("r3h_sea", np.clip(rgb, 0, 1))


def h_marble():
    n = 256
    base = 0.93 + 0.07 * fbm(n, n, beta=2.0, seed=2231)
    vein = np.clip(1 - np.abs(fbm(n, n, beta=2.6, seed=2232) - 0.5) * 16, 0, 1) ** 2
    rgb = base[..., None] * np.array([1.0, 0.99, 0.98])
    rgb = lerp(rgb, col((236, 200, 112)), (vein * 0.55)[..., None])
    G3.save("r3h_marble", np.clip(rgb, 0, 1))


def h_sky():
    c = G3.col
    G3.gen_sky("r3h_sky", (c((38, 128, 238)), c((130, 202, 252)), c((255, 246, 222))), (1.0, 0.97, 0.80))


def h_mountains():
    a = A.capture(G3, G3.gen_mountains)
    h = a.shape[0]
    f = (np.linspace(0, 1, h)[:, None, None]) ** 1.4
    rgb = lerp(a[..., :3] * np.array([1.0, 1.04, 1.1]), col((228, 234, 250)), 0.40 + 0.30 * f)
    rgb = rgb * np.array([1.04, 1.02, 0.97])
    G3.save("r3h_mountains", np.dstack([np.clip(rgb, 0, 1), a[..., 3]]))


def h_spring():
    """Trampolino visto dall'alto: anello d'arcobaleno attorno a una stella dorata su un cuscino di nuvola."""
    n = 128
    y, x = np.mgrid[0:n, 0:n].astype(float)
    u = (x - n / 2 + 0.5) / (n / 2)
    v = (y - n / 2 + 0.5) / (n / 2)
    r = np.sqrt(u * u + v * v)
    ang = np.arctan2(v, u)
    rgb = np.zeros((n, n, 3))
    rgb[...] = col((250, 252, 255))
    bands = [(255, 110, 120), (255, 176, 90), (255, 232, 100), (140, 214, 120), (110, 180, 255), (170, 130, 240)]
    for k, c in enumerate(bands):
        r0 = 0.88 - k * 0.07
        m = np.clip((0.036 - np.abs(r - r0)) * 40, 0, 1)
        rgb = rgb * (1 - m[..., None]) + col(c) * m[..., None]
    star = np.clip(0.34 + 0.12 * np.cos(ang * 5) - r, 0, 1) * 8
    star = np.clip(star, 0, 1)
    rgb = rgb * (1 - star[..., None]) + col((255, 214, 70)) * star[..., None]
    ctr = np.clip((0.14 - r) * 14, 0, 1)
    rgb = rgb * (1 - ctr[..., None]) + col((255, 250, 226)) * ctr[..., None]
    a = np.clip((0.98 - r) * 10, 0, 1)
    G3.save("r3h_spring", np.dstack([rgb, a]))


GENS = {
    "hv_sky": hv_sky, "hv_mountains": hv_mountains, "hv_hills": hv_hills, "hv_ground": hv_ground, "hv_blocks": hv_blocks,
    "hv_spring": hv_spring, "hv_crumble": hv_crumble, "hv_sea": hv_sea, "hv_spark": hv_spark,
    "h_cloud": h_cloud, "h_body": h_body, "h_sea": h_sea, "h_marble": h_marble, "h_sky": h_sky, "h_mountains": h_mountains,
    "h_spring": h_spring,
}

if __name__ == "__main__":
    only = sys.argv[1:] or list(GENS)
    for k in only:
        GENS[k]()
        print("ok", k, flush=True)
