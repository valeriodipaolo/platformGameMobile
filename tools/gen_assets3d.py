#!/usr/bin/env python3
"""Texture realistiche per il livello 3D (erba, sottobosco, roccia, corteccia, rami di abete, felci, cielo, montagne).

Uso:  python3 tools/gen_assets3d.py [nome ...]
Richiede: numpy, scipy, Pillow (e gen_assets.py accanto a questo file per il rumore frattale).
Le immagini finiscono in app/src/main/res/drawable-nodpi con il prefisso r3_.
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter
from scipy import ndimage as ndi

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_assets import fbm, fbm1, lerp, lit_from_height, smooth, OUT, save  # noqa: E402


def rng(seed):
    return np.random.default_rng(seed)


def col(c):
    return np.array(c, dtype=float) / 255.0


def to_img(rgb, a=None):
    arr = np.clip(rgb, 0, 1)
    if a is not None:
        arr = np.dstack([arr, np.clip(a, 0, 1)])
    mode = "RGBA" if arr.shape[2] == 4 else "RGB"
    return Image.fromarray((arr * 255 + 0.5).astype(np.uint8), mode)


def from_img(im):
    return np.asarray(im).astype(float) / 255.0


def wrap_line(d, pts, width, fill, size, wrap=True):
    """Linea spezzata che riappare dall'altro lato se esce dal bordo (texture ripetibile)."""
    d.line(pts, fill=fill, width=max(1, int(round(width))), joint="curve")
    if not wrap:
        return
    xs = [p[0] for p in pts]
    ys = [p[1] for p in pts]
    ox = [0]
    oy = [0]
    if min(xs) < 0: ox.append(size)
    if max(xs) >= size: ox.append(-size)
    if min(ys) < 0: oy.append(size)
    if max(ys) >= size: oy.append(-size)
    for a in ox:
        for b in oy:
            if a or b:
                d.line([(x + a, y + b) for x, y in pts], fill=fill, width=max(1, int(round(width))), joint="curve")


def blade_layer(size, count, seed, palette, length=(14, 40), width=(1.5, 3.0), wrap=True, curl=0.35, ss=2):
    """Strato di fili d'erba visti dall'alto, disegnati dal più scuro al più chiaro."""
    r = rng(seed)
    S = size * ss
    im = Image.new("RGB", (S, S), (0, 0, 0))
    d = ImageDraw.Draw(im)
    items = []
    for _ in range(count):
        k = r.beta(2.2, 1.6)
        items.append(k)
    items.sort()
    for k in items:
        x, y = r.uniform(0, S), r.uniform(0, S)
        a = r.uniform(0, 2 * math.pi)
        ln = r.uniform(*length) * ss
        c = r.uniform(-curl, curl)
        pts = []
        for i in range(4):
            t = i / 3.0
            ang = a + c * t
            pts.append((x + math.cos(ang) * ln * t, y + math.sin(ang) * ln * t))
        c0 = lerp(palette[0], palette[1], k) * r.uniform(0.9, 1.1)
        fill = tuple(int(max(0, min(255, v * 255))) for v in c0)
        wrap_line(d, pts, r.uniform(*width) * ss, fill, S, wrap)
    return im


# ---------------------------------------------------------------- terreno
def gen_grass():
    n = 512
    macro = fbm(n, n, beta=2.6, seed=201)
    mid = fbm(n, n, beta=1.8, seed=202)
    fine = fbm(n, n, beta=0.9, seed=203)
    soil = col((62, 48, 30))
    # fondo scuro con un po' di terra tra i fili
    base = lerp(col((20, 52, 12)), soil, np.clip((fine - 0.55) * 3.0, 0, 0.5)[..., None])
    im = to_img(base)
    layers = [
        blade_layer(n, 5200, 211, (col((30, 78, 20)), col((78, 140, 34))), (14, 30), (1.6, 2.6)),
        blade_layer(n, 5200, 212, (col((60, 118, 28)), col((120, 188, 50))), (12, 32), (1.4, 2.4)),
        blade_layer(n, 3000, 213, (col((100, 160, 40)), col((176, 224, 76))), (10, 26), (1.2, 2.0)),
    ]
    arr = from_img(im)
    for L in layers:
        l = from_img(L.resize((n, n), Image.LANCZOS))
        m = (l.sum(axis=2, keepdims=True) > 0.02).astype(float)
        m = ndi.gaussian_filter(m[..., 0], 0.6)[..., None]
        arr = arr * (1 - m) + l * m
    # variazione ampia: zone più secche/giallastre e zone più scure
    dry = smooth((macro - 0.5) * 3.2)[..., None]
    arr = arr * (1 - 0.35 * dry) + arr * col((1.32, 1.18, 0.62)) * 0.35 * dry
    arr *= (0.62 + 0.8 * mid)[..., None]
    # ombre reciproche tra i fili, a bassa frequenza
    ao = ndi.gaussian_filter(arr.mean(axis=2), 3.0)
    arr *= (0.78 + 0.5 * (arr.mean(axis=2) - ao + 0.15))[..., None] ** 1.0
    # trifogli e piccole macchie chiare
    r = rng(214)
    im2 = to_img(np.clip(arr, 0, 1))
    d = ImageDraw.Draw(im2)
    for _ in range(90):
        cx, cy = r.uniform(0, n), r.uniform(0, n)
        rr = r.uniform(3, 6)
        g = r.uniform(0.85, 1.15)
        c = (int(52 * g), int(98 * g), int(38 * g))
        for k in range(3):
            a = k * 2.094 + r.uniform(0, 1)
            d.ellipse([cx + math.cos(a) * rr - rr * 0.8, cy + math.sin(a) * rr - rr * 0.8,
                       cx + math.cos(a) * rr + rr * 0.8, cy + math.sin(a) * rr + rr * 0.8], fill=c)
    arr = from_img(im2)
    gray = arr.mean(axis=2, keepdims=True)
    arr = np.clip((gray + (arr - gray) * 1.15) * 1.12, 0, 1)
    save("r3_grass", arr)


def gen_forest_floor():
    n = 512
    r = rng(221)
    low = fbm(n, n, beta=2.4, seed=222)
    fine = fbm(n, n, beta=1.0, seed=223)
    base = lerp(col((46, 34, 22)), col((86, 62, 38)), low[..., None])
    arr = base * (0.7 + 0.5 * fine)[..., None]
    # tappeto di aghi e foglie
    needles = blade_layer(n, 9000, 224, (col((58, 40, 24)), col((150, 102, 52))), (6, 16), (1.0, 1.7), curl=0.15)
    l = from_img(needles.resize((n, n), Image.LANCZOS))
    m = ndi.gaussian_filter((l.sum(axis=2) > 0.02).astype(float), 0.5)[..., None]
    arr = arr * (1 - 0.85 * m) + l * 0.85 * m
    # muschio
    mm = smooth((fbm(n, n, beta=2.2, seed=225) - 0.44) * 5)
    mfine = fbm(n, n, beta=0.8, seed=226)
    moss = lerp(col((40, 96, 26)), col((110, 176, 52)), mfine[..., None])
    arr = arr * (1 - 0.8 * mm[..., None]) + moss * 0.8 * mm[..., None]
    # rametti
    im = to_img(np.clip(arr, 0, 1))
    d = ImageDraw.Draw(im)
    for _ in range(26):
        x, y = r.uniform(0, n), r.uniform(0, n)
        a = r.uniform(0, 6.28)
        ln = r.uniform(30, 80)
        pts = [(x, y), (x + math.cos(a) * ln * 0.5 + r.uniform(-4, 4), y + math.sin(a) * ln * 0.5 + r.uniform(-4, 4)),
               (x + math.cos(a) * ln, y + math.sin(a) * ln)]
        c = int(r.uniform(34, 62))
        wrap_line(d, pts, r.uniform(2, 3.5), (c + 14, c, c - 10), n)
    arr = from_img(im)
    diff, _ = lit_from_height(ndi.gaussian_filter(arr.mean(axis=2), 1.0) * 6, wrap=True)
    arr *= (0.78 + 0.45 * diff)[..., None]
    save("r3_forest", np.clip(arr, 0, 1))


def gen_dirt():
    n = 512
    low = fbm(n, n, beta=2.2, seed=231)
    fine = fbm(n, n, beta=1.0, seed=232)
    vfine = fbm(n, n, beta=0.5, seed=233)
    base = lerp(col((84, 58, 36)), col((132, 98, 64)), low[..., None])
    hmap = low * 20 + fine * 7 + vfine * 2
    r = rng(234)
    pe = np.zeros((n, n))
    pcol = np.zeros((n, n, 3))
    for _ in range(190):
        cx, cy = r.uniform(0, n), r.uniform(0, n)
        rx, ry = r.uniform(3, 11), r.uniform(2.5, 8)
        ang = r.uniform(0, 3.14)
        yy, xx = np.mgrid[-14:15, -14:15]
        u = (xx * math.cos(ang) + yy * math.sin(ang)) / rx
        v = (-xx * math.sin(ang) + yy * math.cos(ang)) / ry
        dd = 1 - np.sqrt(u * u + v * v)
        h = np.clip(dd, 0, 1) ** 0.5
        g = r.uniform(0.7, 1.2)
        c = col((142 * g, 130 * g, 116 * g)) * r.uniform(0.7, 1.1)
        for dy in range(29):
            for dx in range(29):
                if h[dy, dx] > 0:
                    px, py = (int(cx) + dx - 14) % n, (int(cy) + dy - 14) % n
                    if h[dy, dx] * 8 > pe[py, px]:
                        pe[py, px] = h[dy, dx] * 8
                        pcol[py, px] = c
    mask = (pe > 0).astype(float)
    hmap = hmap + pe * 1.6
    diff, spec = lit_from_height(ndi.gaussian_filter(hmap, 0.8), wrap=True)
    arr = base * (0.55 + 0.7 * fine[..., None])
    arr = arr * (1 - mask[..., None]) + pcol * mask[..., None]
    arr = arr * (0.5 + 0.8 * diff)[..., None]
    save("r3_dirt", np.clip(arr, 0, 1))


def voronoi(n, count, seed):
    r = rng(seed)
    pts = r.uniform(0, n, (count, 2))
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    d1 = np.full((n, n), 1e9, np.float32)
    d2 = np.full((n, n), 1e9, np.float32)
    for px, py in pts:
        for ox in (-n, 0, n):
            for oy in (-n, 0, n):
                dd = np.sqrt((xx - px - ox) ** 2 + (yy - py - oy) ** 2)
                nd = np.minimum(d2, np.maximum(d1, dd))
                d1 = np.minimum(d1, dd)
                d2 = nd
    return d1, d2


def gen_rock():
    n = 512
    low = fbm(n, n, beta=2.4, seed=241)
    mid = fbm(n, n, beta=1.6, seed=242)
    fine = fbm(n, n, beta=0.8, seed=243)
    d1, d2 = voronoi(n, 11, 244)
    crack = np.clip(1 - (d2 - d1) / 6.0, 0, 1)
    crack *= smooth((fbm(n, n, beta=1.8, seed=245) - 0.35) * 4)
    yy = np.arange(n)[:, None] / n
    strata = 0.5 + 0.5 * np.sin(yy * 2 * math.pi * 7 + low * 9 + mid * 3)
    base = lerp(col((96, 94, 90)), col((148, 142, 132)), (0.5 * low + 0.3 * strata + 0.2 * fine)[..., None])
    warm = smooth((mid - 0.5) * 3)[..., None]
    base = base * (1 - 0.25 * warm) + base * col((1.12, 1.0, 0.82)) * 0.25 * warm
    hmap = low * 16 + mid * 8 + fine * 3 - crack * 7
    diff, spec = lit_from_height(ndi.gaussian_filter(hmap, 0.7), wrap=True)
    arr = base * (0.45 + 0.85 * diff)[..., None] + 0.05 * spec[..., None] ** 10
    arr *= (1 - 0.65 * crack)[..., None]
    # puntini scuri/chiari del granito
    sp = fbm(n, n, beta=0.1, seed=246)
    arr *= (0.9 + 0.3 * (sp - 0.5))[..., None]
    # muschio sul bordo superiore e nelle fessure
    yv = np.arange(n)[:, None] / n
    mchance = smooth((fbm(n, n, beta=2.0, seed=247) - 0.45) * 6) * np.clip(1.3 - yv * 2.2, 0, 1) + crack * 0.35
    mchance = np.clip(mchance, 0, 1) * smooth((fine - 0.3) * 3.5)
    moss = lerp(col((34, 60, 22)), col((96, 128, 46)), fbm(n, n, beta=0.7, seed=248)[..., None]) * (0.6 + 0.5 * diff)[..., None]
    arr = arr * (1 - 0.85 * mchance[..., None]) + moss * 0.85 * mchance[..., None]
    # bordo del blocco: smussatura e ombra di contatto
    e = np.minimum(np.minimum(np.arange(n)[None, :], n - 1 - np.arange(n)[None, :]),
                   np.minimum(np.arange(n)[:, None], n - 1 - np.arange(n)[:, None])).astype(float)
    arr *= (0.55 + 0.45 * smooth(e / 22.0))[..., None]
    arr += 0.07 * np.exp(-np.abs(e - 24) / 3.5)[..., None]
    save("r3_rock", np.clip(arr, 0, 1))


def gen_bark():
    w, h = 256, 512
    r = rng(251)
    wob = ndi.gaussian_filter(r.standard_normal((h, w)), (14, 3))
    wob = wob / wob.std()
    xx = np.arange(w)[None, :].astype(float)
    ridge = 0.5 + 0.5 * np.sin(xx / w * 2 * math.pi * 9 + wob * 2.2)
    ridge2 = 0.5 + 0.5 * np.sin(xx / w * 2 * math.pi * 23 + wob * 3.1 + 1.3)
    f1 = fbm(h, w, beta=1.6, seed=252, ax=2.5, ay=0.25)
    f2 = fbm(h, w, beta=0.8, seed=253, ax=1.5, ay=0.5)
    plates = smooth(ridge * 1.2 + 0.3 * ridge2 + 0.3 * f1 - 0.35)
    hm = plates * 10 + f2 * 3
    diff, _ = lit_from_height(ndi.gaussian_filter(hm, 0.8), wrap=True)
    base = lerp(col((44, 33, 25)), col((120, 94, 72)), np.clip(0.2 + 0.7 * plates + 0.3 * (f1 - 0.5), 0, 1)[..., None])
    arr = base * (0.45 + 0.8 * diff)[..., None]
    arr *= (0.55 + 0.6 * plates)[..., None]
    # licheni verdi
    lich = smooth((fbm(h, w, beta=2.0, seed=254) - 0.58) * 8)
    lc = lerp(col((74, 96, 52)), col((130, 150, 84)), fbm(h, w, beta=0.8, seed=255)[..., None])
    arr = arr * (1 - 0.55 * lich[..., None]) + lc * 0.55 * lich[..., None] * (0.5 + 0.6 * diff)[..., None]
    save("r3_bark", np.clip(arr, 0, 1))


# ---------------------------------------------------------------- vegetazione (con trasparenza)
def finish_card(im, name, ss):
    w, h = im.size[0] // ss, im.size[1] // ss
    im = im.resize((w, h), Image.LANCZOS)
    # PNG con trasparenza "diritta": è Android a premoltiplicare quando carica l'immagine
    save(name, from_img(im))


def rgba(c, a=255):
    return tuple(int(max(0, min(255, v * 255))) for v in c) + (a,)


def needle_palette():
    return col((16, 58, 26)), col((36, 112, 38)), col((72, 164, 52)), col((148, 208, 78))


def gen_branch():
    """Un singolo ramo di abete (fusto lungo l'asse orizzontale, attacco al tronco a sinistra), con rametti e aghi."""
    ss = 2
    W, H = 512 * ss, 256 * ss
    r = rng(261)
    im = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    dark, mid, light, tip = needle_palette()
    cy = H / 2

    def stem(x):
        return cy + math.sin(x / W * 2.6) * 10 * ss * (x / W)

    d.line([(x, stem(x)) for x in range(int(0.02 * W), int(0.97 * W), 6 * ss)], fill=(60, 42, 28, 255), width=int(3.2 * ss))
    twigs = []
    x = 0.07 * W
    while x < 0.93 * W:
        t = x / W
        for side in (-1, 1):
            reach = H * 0.47 * (0.35 + 0.65 * math.sin(math.pi * min(1, t * 1.05 + 0.08))) * (1 - 0.35 * t) * r.uniform(0.8, 1.1)
            twigs.append((x + r.uniform(-3, 3) * ss, side, reach, r.uniform(0.85, 1.25)))
        x += r.uniform(9, 13) * ss
    for x0, side, reach, ang in twigs:
        sy = stem(x0)
        ex, ey = x0 + math.cos(ang) * reach * 0.62, sy + side * math.sin(ang) * reach
        d.line([(x0, sy), (ex, ey)], fill=(66, 46, 30, 255), width=int(1.8 * ss))
        ta = math.atan2(ey - sy, ex - x0)
        n = int(18 + reach / ss * 0.35)
        for i in range(n):
            u = r.uniform(0.08, 1.0)
            px, py = x0 + (ex - x0) * u, sy + (ey - sy) * u
            for sgn in (-1, 1):
                a = ta + sgn * r.uniform(0.55, 1.2)
                nl = r.uniform(11, 19) * ss * (1.05 - 0.35 * u)
                q = r.uniform(0, 1) ** 1.3 * (0.35 + 0.65 * u)
                c = lerp(dark, mid, r.uniform(0.2, 1)) if q < 0.45 else (lerp(mid, light, (q - 0.45) / 0.35) if q < 0.8 else lerp(light, tip, (q - 0.8) / 0.2))
                d.line([(px, py), (px + math.cos(a) * nl, py + math.sin(a) * nl)], fill=rgba(c), width=int(2.3 * ss))
    for i in range(60):
        a = r.uniform(-0.8, 0.8)
        nl = r.uniform(12, 24) * ss
        px, py = 0.95 * W, stem(0.95 * W)
        d.line([(px, py), (px + math.cos(a) * nl, py + math.sin(a) * nl)], fill=rgba(lerp(light, tip, r.uniform(0.3, 1))), width=int(2.3 * ss))
    arr = from_img(im)
    a = arr[..., 3]
    sh = ndi.gaussian_filter(a, 6 * ss)
    arr[..., :3] *= (0.6 + 0.55 * (1 - np.clip(sh * 1.5, 0, 1)))[..., None]
    finish_card(Image.fromarray((np.clip(arr, 0, 1) * 255).astype(np.uint8), "RGBA"), "r3_branch", ss)


def gen_firtree():
    """Abete intero (per gli alberi lontani): tronco, rami che pendono a livelli, aghi, luce dall'alto."""
    ss = 2
    W, H = 256 * ss, 512 * ss
    r = rng(341)
    im = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    dark, mid, light, tip = needle_palette()
    cx = W / 2
    top, bottom = 0.03 * H, 0.97 * H
    # tronco
    d.polygon([(cx - 8 * ss, bottom), (cx + 8 * ss, bottom), (cx + 1.5 * ss, top * 2), (cx - 1.5 * ss, top * 2)], fill=(74, 54, 38, 255))
    for k in range(18):
        yk = bottom - r.uniform(0, 0.4) * (bottom - top)
        d.line([(cx - 5 * ss, yk), (cx - 3 * ss, yk + 14 * ss)], fill=(46, 32, 22, 255), width=ss)
    levels = 30
    crown0 = 0.74 * H
    for li in range(levels):
        f = li / (levels - 1)                       # 0 in basso, 1 in cima
        y = crown0 - (crown0 - top - 0.04 * H) * f
        L = (92 * (1 - 0.86 * f) + 8) * ss
        for side in (-1, 1):
            for rep in range(2 if f < 0.85 else 1):
                ll = L * r.uniform(0.75, 1.05)
                droop = (0.34 + 0.3 * (1 - f)) * r.uniform(0.8, 1.2)
                yy = y + r.uniform(-4, 4) * ss
                # massa piena del ramo, così da lontano la chioma è folta
                body = [(cx, yy - 4 * ss)]
                for st in range(1, 9):
                    t = st / 8
                    body.append((cx + side * ll * t, yy + ll * droop * t + ll * 0.35 * t * t - (1 - t) * 6 * ss))
                for st in range(8, 0, -1):
                    t = st / 8
                    body.append((cx + side * ll * t, yy + ll * droop * t + ll * 0.35 * t * t + (14 * (1 - 0.6 * t) + 4) * ss))
                d.polygon(body, fill=rgba(lerp(mid, light, 0.35) * (0.78 + 0.3 * (1 - f))))
                prev = (cx, yy)
                steps = 14
                for st in range(1, steps + 1):
                    t = st / steps
                    px = cx + side * ll * t
                    py = yy + ll * droop * t + ll * 0.35 * t * t
                    d.line([prev, (px, py)], fill=(58, 42, 28, 255), width=int(1.6 * ss))
                    prev = (px, py)
                    nn = 5 if t > 0.12 else 1
                    for _ in range(nn):
                        a = (math.pi / 2 + r.uniform(-1.0, 1.0))
                        nl = (r.uniform(8, 16) + ll / ss * 0.06) * ss * (1.0 - 0.3 * t)
                        up = r.uniform(0, 1) < 0.5
                        shade = (0.78 + 0.5 * (1 - f) * 0.0) if up else 0.55
                        q = r.uniform(0, 1)
                        c = lerp(dark, mid, q) if q < 0.5 else (lerp(mid, light, (q - 0.5) / 0.35) if q < 0.85 else lerp(light, tip, (q - 0.85) / 0.15))
                        c = c * (0.8 + 0.4 * (1 - 0.6 * t)) * (1.35 if up else 0.85)
                        dy = -abs(math.sin(a)) * nl * (0.5 if up else -0.5)
                        d.line([(px, py), (px + math.cos(a) * nl * 0.9, py + dy + math.sin(a) * nl * 0.3)], fill=rgba(c), width=int(2.4 * ss))
    # cima
    for i in range(50):
        a = -math.pi / 2 + r.uniform(-0.55, 0.55)
        nl = r.uniform(10, 30) * ss
        px, py = cx, top + 0.09 * H
        d.line([(px, py), (px + math.cos(a) * nl, py + math.sin(a) * nl)], fill=rgba(lerp(light, tip, r.uniform(0.2, 1))), width=int(2.4 * ss))
    arr = from_img(im)
    # ombra interna verso il centro e più scuro in basso
    a = arr[..., 3]
    sh = ndi.gaussian_filter(a, 9 * ss)
    yy = np.linspace(0, 1, H)[:, None]
    arr[..., :3] *= ((0.82 + 0.4 * (1 - np.clip(sh * 1.3, 0, 1))) * (0.9 + 0.2 * (1 - yy)))[..., None]
    finish_card(Image.fromarray((np.clip(arr, 0, 1) * 255).astype(np.uint8), "RGBA"), "r3_firtree", ss)


def gen_leaf():
    """Foglia chiara con nervature: il colore (verde, giallo, arancio) lo dà il tono applicato quando si disegna."""
    ss = 4
    S = 64 * ss
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    pts_l, pts_r = [], []
    for i in range(41):
        t = i / 40
        wd = (math.sin(math.pi * t ** 0.75) ** 0.9) * 0.34 * S * (1 - 0.25 * t)
        y = S * 0.92 - t * S * 0.84
        pts_l.append((S / 2 - wd, y))
        pts_r.append((S / 2 + wd, y))
    d.polygon(pts_l + pts_r[::-1], fill=(214, 206, 160, 255))
    d.line([(S / 2, S * 0.95), (S / 2, S * 0.1)], fill=(150, 120, 70, 255), width=2 * ss)
    for k in range(1, 8):
        t = k / 9
        y = S * 0.92 - t * S * 0.84
        wd = (math.sin(math.pi * t ** 0.75) ** 0.9) * 0.34 * S * (1 - 0.25 * t)
        for sg in (-1, 1):
            d.line([(S / 2, y), (S / 2 + sg * wd * 0.9, y - S * 0.07)], fill=(170, 140, 90, 255), width=max(1, ss // 2))
    finish_card(im, "r3_leaf", ss)


def gen_tuft():
    ss = 2
    S = 256 * ss
    r = rng(271)
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    dark, light = col((40, 100, 26)), col((156, 214, 72))
    blades = []
    for _ in range(44):
        bx = S / 2 + r.normal(0, S * 0.12)
        h = r.uniform(0.35, 0.95) * S
        lean = r.normal(0, 0.28) * S * 0.4 + (bx - S / 2) * 0.7
        bend = r.uniform(-0.3, 0.3) * S * 0.2
        wd = r.uniform(5, 9) * ss / 2
        k = r.uniform(0, 1)
        blades.append((h, bx, lean, bend, wd, k))
    blades.sort(key=lambda b: b[0])
    for h, bx, lean, bend, wd, k in blades:
        pts_l, pts_r = [], []
        for i in range(9):
            t = i / 8
            x = bx + lean * t * t + bend * math.sin(t * 3)
            y = S - 2 - h * t
            wt = wd * (1 - t) ** 0.8
            pts_l.append((x - wt, y))
            pts_r.append((x + wt, y))
        c = lerp(dark, light, k) * r.uniform(0.85, 1.1)
        fill = tuple(int(max(0, min(255, v * 255))) for v in c) + (255,)
        d.polygon(pts_l + pts_r[::-1], fill=fill)
        # nervatura più chiara
        mid = [((a[0] + b[0]) / 2, (a[1] + b[1]) / 2) for a, b in zip(pts_l, pts_r)]
        c2 = tuple(int(min(255, v * 255 * 1.25)) for v in c) + (255,)
        d.line(mid[1:-1], fill=c2, width=max(1, ss // 2))
    arr = from_img(im)
    # base più scura
    yy = np.linspace(0, 1, S)[:, None]
    arr[..., :3] *= (0.55 + 0.5 * (1 - yy) ** 0.7)[..., None]
    out = Image.fromarray((np.clip(arr, 0, 1) * 255).astype(np.uint8), "RGBA")
    finish_card(out, "r3_tuft", ss)


def gen_fern():
    """Una sola fronda di felce, distesa da sinistra (attacco) a destra (punta)."""
    ss = 2
    W, H = 512 * ss, 128 * ss
    r = rng(281)
    im = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    dark, light = col((36, 106, 34)), col((126, 200, 62))
    cy = H / 2
    n = 46
    for i in range(n):
        t = i / (n - 1)
        x = (0.04 + 0.92 * t) * W
        leaf = (math.sin(math.pi * min(1.0, t * 1.25 + 0.12)) ** 0.8) * (1 - 0.55 * t) * H * 0.52 + 3 * ss
        wd = W * 0.92 / n * 0.62
        for sgn in (-1, 1):
            lean = 0.28 * leaf
            ex, ey = x + lean, cy + sgn * leaf
            c = lerp(dark, light, r.uniform(0.25, 1.0)) * (0.8 + 0.3 * t)
            fill = tuple(int(max(0, min(255, v * 255))) for v in c) + (255,)
            d.polygon([(x - wd, cy), (x + wd, cy), (x + wd * 0.9 + lean * 0.6, cy + sgn * leaf * 0.6), (ex, ey),
                       (x - wd * 0.6 + lean * 0.5, cy + sgn * leaf * 0.55)], fill=fill)
            c2 = tuple(int(min(255, v * 1.2)) for v in fill[:3]) + (255,)
            d.line([(x, cy), (ex, ey)], fill=c2, width=max(1, ss // 2))
    d.line([(0.02 * W, cy), (0.97 * W, cy)], fill=(70, 96, 40, 255), width=int(2.5 * ss))
    finish_card(im, "r3_fern", ss)


def gen_wood():
    """Portone: assi verticali di legno scuro con fasce e borchie di ferro."""
    w, h = 256, 512
    r = rng(331)
    grain = fbm(h, w, beta=1.8, seed=332, ax=6.0, ay=0.35)
    fine = fbm(h, w, beta=1.0, seed=333, ax=3.0, ay=0.6)
    xx = np.arange(w)[None, :]
    yy = np.arange(h)[:, None]
    plank = xx // 43
    pk = np.array([r.uniform(0.82, 1.12) for _ in range(8)])[plank]
    base = lerp(col((74, 48, 28)), col((142, 98, 58)), np.clip(0.15 + 0.9 * grain + 0.3 * (fine - 0.5), 0, 1)[..., None])
    base = base * pk[..., None] * np.ones((h, 1, 1))
    gap = np.minimum(xx % 43, 42 - xx % 43)
    hm = np.clip(gap / 3.0, 0, 1) * 6 + grain * 3
    hm = hm * np.ones((h, 1))
    diff, _ = lit_from_height(ndi.gaussian_filter(hm, 0.7))
    arr = base * (0.5 + 0.7 * diff)[..., None]
    arr *= (0.45 + 0.55 * np.clip(gap / 2.5, 0, 1))[..., None] * np.ones((h, 1, 1))
    # fasce di ferro
    for by in (70, 256, 442):
        band = (np.abs(yy - by) < 22) * np.ones((1, w))
        iron = (0.17 + 0.1 * fbm(h, w, beta=1.2, seed=334 + by)) [..., None] * col((150, 156, 170)) * 1.6
        edge = np.clip((22 - np.abs(yy - by)) / 4.0, 0, 1) * np.ones((1, w))
        iron = iron * (0.6 + 0.5 * edge)[..., None]
        arr = arr * (1 - band[..., None]) + iron * band[..., None]
        for bx in range(21, w, 43):
            dd = np.sqrt((xx - bx) ** 2 + (yy - by) ** 2)
            stud = np.clip((8 - dd) / 2.0, 0, 1)
            hi = np.clip(1 - np.sqrt((xx - bx + 2.5) ** 2 + (yy - by + 2.5) ** 2) / 5.0, 0, 1)
            arr = arr * (1 - stud[..., None]) + (col((96, 100, 112)) * (0.6 + 1.2 * hi[..., None])) * stud[..., None]
    save("r3_wood", np.clip(arr, 0, 1))


def gen_flowers(name="r3_flowers", kinds=None, stem=(48, 128, 40, 255), leaf=(60, 150, 44, 255)):
    """Prato fiorito: tanti fiori di colori diversi (rosso, rosa, arancio, giallo, viola, blu, bianco), con foglie e steli."""
    ss = 2
    S = 256 * ss
    r = rng(291)
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    kinds = kinds or [((236, 40, 48), (255, 214, 60)), ((255, 96, 160), (255, 236, 120)), ((255, 140, 30), (120, 60, 10)),
             ((255, 218, 40), (200, 120, 10)), ((150, 70, 230), (255, 236, 130)), ((60, 120, 255), (255, 240, 150)),
             ((252, 252, 252), (255, 205, 40)), ((255, 70, 110), (90, 30, 40)), ((190, 110, 255), (255, 255, 200))]
    order = list(range(len(kinds))) * 2
    r.shuffle(order)
    for n, k in enumerate(order):
        x = (0.07 + 0.86 * ((n * 0.618) % 1.0)) * S + r.uniform(-8, 8) * ss
        top = r.uniform(0.12, 0.62) * S
        bend = r.uniform(-16, 16) * ss
        pts = [(x, S - 2), (x + bend * 0.5, (S + top) / 2), (x + bend, top)]
        d.line(pts, fill=stem, width=max(2, int(2.4 * ss)))
        ly = S - r.uniform(14, 70) * ss
        for sg in (-1, 1):
            d.polygon([(x, ly), (x + sg * 24 * ss, ly - 16 * ss), (x + sg * 6 * ss, ly - 2 * ss)], fill=leaf)
        petal, center = kinds[k]
        rad = r.uniform(10, 17) * ss
        np_ = r.choice([5, 6, 8])
        for q in range(np_):
            a = q * 2 * math.pi / np_ + r.uniform(-0.1, 0.1)
            cx, cy = x + bend + math.cos(a) * rad * 0.8, top + math.sin(a) * rad * 0.8
            c2 = tuple(int(min(255, v * (0.9 + 0.2 * r.random()))) for v in petal) + (255,)
            d.ellipse([cx - rad * 0.56, cy - rad * 0.56, cx + rad * 0.56, cy + rad * 0.56], fill=c2)
        d.ellipse([x + bend - rad * 0.42, top - rad * 0.42, x + bend + rad * 0.42, top + rad * 0.42], fill=center + (255,))
    finish_card(im, name, ss)


def gen_ray():
    w, h = 64, 256
    x = np.linspace(-1, 1, w)[None, :]
    y = np.linspace(0, 1, h)[:, None]
    a = np.exp(-(x * 1.7) ** 2 * 2.5) * smooth(y * 6) * smooth((1 - y) * 4)
    a *= 0.5 + 0.5 * fbm(h, w, beta=1.6, seed=301, ax=0.5, ay=3.0)
    col_ = np.dstack([np.ones((h, w)) * 1.0, np.ones((h, w)) * 0.93, np.ones((h, w)) * 0.75])
    a = np.clip(a, 0, 1)[..., None] * 0.9
    save("r3_ray", np.concatenate([col_, a], axis=2))


# ---------------------------------------------------------------- cielo e montagne
def gen_sky(name="r3_sky", pal=None, sun=(1.0, 0.86, 0.56)):
    w, h = 1024, 512
    y = np.linspace(0, 1, h)[:, None]
    zen, mid, hor = pal or (col((38, 92, 176)), col((110, 164, 220)), col((206, 224, 238)))
    t = y ** 0.8
    grad = np.where(t[..., None] < 0.6, lerp(zen, mid, (t / 0.6)[..., None]), lerp(mid, hor, ((t - 0.6) / 0.4)[..., None]))
    sky = np.broadcast_to(grad, (h, w, 3)).copy()
    # sole: bagliore caldo in alto a destra
    yy, xx = np.mgrid[0:h, 0:w]
    sd = np.sqrt(((xx - 0.78 * w) / w) ** 2 * 1.0 + ((yy - 0.16 * h) / h) ** 2 * 1.6)
    glow = np.exp(-sd * 7.5)[..., None] * np.array(sun) * 0.55 + np.exp(-sd * 22)[..., None] * col((1.0, 0.95, 0.8)) * 0.8
    sky += glow
    # nuvole: densità frattale più fitta verso l'orizzonte, illuminate dal sole
    den = fbm(h, w, beta=2.8, seed=311, ax=2.2, ay=1.0)
    den2 = fbm(h, w, beta=1.6, seed=312, ax=1.6, ay=1.0)
    cover = 0.66 - 0.16 * y
    c = smooth((den * 1.0 + 0.3 * (den2 - 0.5) - cover) * 5.0)
    c = c * (0.35 + 0.65 * y)
    dn = ndi.gaussian_filter(den, 5)
    gx = np.roll(dn, 4, 1) - np.roll(dn, -4, 1)
    lit = np.clip(0.8 + gx * 60 + (den2 - 0.5) * 0.6, 0.55, 1.2)
    cloud = lerp(col((172, 184, 206)), col((255, 253, 248)), np.clip((lit - 0.55) / 0.65, 0, 1)[..., None])
    cloud = cloud * (0.9 + 0.2 * den2)[..., None] + glow * 0.25
    a = np.clip(c, 0, 0.96)[..., None]
    sky = sky * (1 - a) + cloud * a
    # foschia all'orizzonte
    hz = smooth((y - 0.78) * 4.5)
    sky = sky * (1 - 0.65 * hz[..., None]) + hor[None, None, :] * 0.65 * hz[..., None]
    save(name, np.clip(sky, 0, 1))


def ridge_profile(n, seed, base, amp, octaves=5, sharp=1.0):
    r = rng(seed)
    x = np.linspace(0, 1, n, endpoint=False)
    h = np.zeros(n)
    a = 1.0
    for o in range(octaves):
        f = 2 ** o * 2
        ph = r.uniform(0, 2 * math.pi, 3)
        v = np.sin(2 * math.pi * f * x + ph[0]) * 0.6 + np.sin(2 * math.pi * f * 1.7 * x + ph[1]) * 0.4
        v = 1 - np.abs(v)
        h += (v ** sharp) * a
        a *= 0.5
    h = h / h.max()
    h += 0.12 * (fbm1(n, beta=1.6, seed=seed + 5) - 0.5)
    return base + amp * h


def gen_mountains():
    w, h = 2048, 512
    out = np.zeros((h, w, 4))
    haze = col((184, 208, 232))
    layers = [  # (seed, base, amp, haze, snowline, forest)
        (321, 330, 170, 0.74, 0.95, 0.0),
        (322, 270, 150, 0.52, 1.35, 0.3),
        (323, 220, 110, 0.26, 2.0, 0.9),
    ]
    yy = np.arange(h)[:, None].astype(float)
    xx = np.arange(w)[None, :].astype(float)
    for li, (seed, base, amp, hz, snow, forest) in enumerate(layers):
        top = ridge_profile(w, seed, base, amp, 6, 1.4)
        top = h - top  # y del crinale (verso il basso cresce y)
        depth = np.clip((yy - top[None, :]) / 200.0, 0, 3)
        inside = (yy >= top[None, :])
        det = fbm(h, w, beta=3.0, seed=seed + 10)
        det2 = fbm(h, w, beta=2.0, seed=seed + 11)
        dl, _ = lit_from_height(ndi.gaussian_filter(det * 70 + det2 * 22, 1.2), wrap=True)
        # pendenza dal profilo: luce da destra
        slope = np.roll(top, 1) - np.roll(top, -1)
        sl = ndi.gaussian_filter1d(slope, 2.0)[None, :]
        light = np.clip(0.30 + 0.95 * dl + sl * 0.02, 0.2, 1.2)
        rock = lerp(col((104, 98, 94)), col((176, 166, 152)), np.clip(det2 + (det - 0.5), 0, 1)[..., None]) * light[..., None]
        alt = np.clip((top.max() - yy) / (top.max() - top.min() + 1) * 0, 0, 1)
        relh = (h - yy) / h  # quota relativa
        # neve in cima, con confine frastagliato
        snowmask = smooth(((relh - (snow * 0.55 + (det - 0.5) * 0.25)) * 16)) if snow < 1.5 else np.zeros_like(det)
        snowc = lerp(col((214, 224, 238)), col((255, 255, 255)), np.clip(light - 0.4, 0, 1)[..., None])
        col_ = rock * (1 - snowmask[..., None]) + snowc * np.clip(light, 0.5, 1.1)[..., None] * snowmask[..., None]
        if forest > 0:
            fm = smooth((0.42 - relh * 0.9 + (det - 0.5) * 0.5 - (1 - forest) * 0.6) * 8) * forest
            tr = fbm(h, w, beta=1.0, seed=seed + 12)
            fcol = lerp(col((22, 44, 26)), col((54, 92, 46)), tr[..., None]) * (0.55 + 0.6 * light)[..., None]
            col_ = col_ * (1 - fm[..., None]) + fcol * fm[..., None]
        # prospettiva atmosferica: i piani lontani sfumano nella foschia
        col_ = col_ * (1 - hz) + haze[None, None, :] * hz
        a = inside.astype(float)
        a = np.clip(ndi.gaussian_filter(a, 0.7), 0, 1)
        out[..., :3] = out[..., :3] * (1 - a[..., None]) + col_ * a[..., None]
        out[..., 3] = np.maximum(out[..., 3], a)
    # base sfumata verso la foschia (l'orizzonte non ha un taglio netto)
    fade = smooth((yy / h - 0.80) * 5.0)
    out[..., :3] = out[..., :3] * (1 - 0.8 * fade[..., None]) + haze[None, None, :] * 0.8 * fade[..., None]
    out[..., 3] *= 1 - smooth((yy / h - 0.93) * 14)
    save("r3_mountains", out)


GENS = {
    "grass": gen_grass, "forest": gen_forest_floor, "dirt": gen_dirt, "rock": gen_rock, "bark": gen_bark,
    "branch": gen_branch, "tuft": gen_tuft, "fern": gen_fern, "flowers": gen_flowers, "ray": gen_ray,
    "sky": gen_sky, "mountains": gen_mountains, "wood": gen_wood, "firtree": gen_firtree, "leaf": gen_leaf,
}

if __name__ == "__main__":
    only = sys.argv[1:] or list(GENS)
    for k in only:
        GENS[k]()
        print("ok", k)
