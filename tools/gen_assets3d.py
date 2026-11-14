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
    base = lerp(col((24, 36, 14)), soil, np.clip((fine - 0.55) * 3.0, 0, 0.5)[..., None])
    im = to_img(base)
    layers = [
        blade_layer(n, 5200, 211, (col((26, 44, 16)), col((60, 84, 28))), (14, 30), (1.6, 2.6)),
        blade_layer(n, 5200, 212, (col((44, 68, 22)), col((98, 124, 42))), (12, 32), (1.4, 2.4)),
        blade_layer(n, 3000, 213, (col((70, 96, 30)), col((142, 156, 62))), (10, 26), (1.2, 2.0)),
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
    arr = np.clip((gray + (arr - gray) * 0.8) * 1.0, 0, 1)
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
    mm = smooth((fbm(n, n, beta=2.2, seed=225) - 0.52) * 7)
    mfine = fbm(n, n, beta=0.8, seed=226)
    moss = lerp(col((34, 62, 22)), col((88, 124, 44)), mfine[..., None])
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


def gen_branch():
    ss = 2
    W, H = 512 * ss, 256 * ss
    r = rng(261)
    im = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    cy = H // 2

    def stem_y(x):
        return cy + math.sin(x / W * 3.1) * 7 * ss * (x / W)

    dark, light, tip = col((16, 38, 22)), col((52, 92, 40)), col((118, 158, 66))
    # rametti laterali, dal fondo (vicino al tronco, a sinistra) alla punta
    twigs = []
    x = 30 * ss
    while x < W - 28 * ss:
        t = x / W
        ln = (1.0 - t * 0.78) * 118 * ss * r.uniform(0.82, 1.1)
        for side in (-1, 1):
            ang = side * r.uniform(0.7, 1.05)
            twigs.append((x, ln, ang, t))
        x += r.uniform(9, 14) * ss
    # fusto
    d.line([(x0, stem_y(x0)) for x0 in range(0, W, 8 * ss)], fill=(66, 46, 30, 255), width=4 * ss)
    for x0, ln, ang, t in twigs:
        sy = stem_y(x0)
        ex = x0 + math.cos(ang * 0.5) * ln * 0.55
        ey = sy + math.sin(ang) * ln
        d.line([(x0, sy), (ex, ey)], fill=(70, 50, 32, 255), width=int(2 * ss))
        # aghi lungo il rametto
        nn = int(26 + ln / ss * 0.3)
        for i in range(nn):
            u = r.uniform(0.05, 1.0)
            px = x0 + (ex - x0) * u
            py = sy + (ey - sy) * u
            for sgn in (-1, 1):
                a = math.atan2(ey - sy, ex - x0) + sgn * r.uniform(0.7, 1.3)
                nl = r.uniform(9, 17) * ss * (1.0 - 0.35 * u)
                k = r.uniform(0, 1) ** 1.5 * (0.3 + 0.7 * u)
                c = lerp(dark, light, r.uniform(0.2, 1.0)) if k < 0.6 else lerp(light, tip, (k - 0.6) / 0.4)
                fill = tuple(int(v * 255) for v in c) + (255,)
                d.line([(px, py), (px + math.cos(a) * nl, py + math.sin(a) * nl)], fill=fill, width=int(1.4 * ss))
    # punta del ramo
    for i in range(40):
        a = r.uniform(-0.7, 0.7)
        nl = r.uniform(10, 20) * ss
        c = lerp(light, tip, r.uniform(0.3, 1))
        fill = tuple(int(v * 255) for v in c) + (255,)
        px, py = W - 30 * ss, stem_y(W - 30 * ss)
        d.line([(px, py), (px + math.cos(a) * nl, py + math.sin(a) * nl)], fill=fill, width=int(1.4 * ss))
    # leggero ombreggiamento verso il centro (profondità)
    arr = from_img(im)
    a = arr[..., 3]
    sh = ndi.gaussian_filter(a, 5 * ss)
    arr[..., :3] *= (0.55 + 0.6 * (1 - np.clip(sh * 1.4, 0, 1)))[..., None]
    out = Image.fromarray((np.clip(arr, 0, 1) * 255).astype(np.uint8), "RGBA")
    finish_card(out, "r3_branch", ss)


def gen_tuft():
    ss = 2
    S = 256 * ss
    r = rng(271)
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    dark, light = col((30, 56, 20)), col((124, 150, 58))
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
    ss = 2
    S = 256 * ss
    r = rng(281)
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    dark, light = col((26, 62, 24)), col((92, 140, 48))
    for f in range(7):
        ang = -math.pi / 2 + (f - 3) * 0.34 + r.uniform(-0.08, 0.08)
        ln = r.uniform(0.62, 0.95) * S * 0.95
        droop = r.uniform(0.15, 0.45) * (1 if f >= 3 else -1) * (abs(f - 3) / 3 + 0.2)
        pts = []
        for i in range(21):
            t = i / 20
            a = ang + droop * t * t
            pts.append((S / 2 + math.cos(a) * ln * t * 0.9 + (math.cos(ang) * 0), S - 6 + math.sin(a) * ln * t))
        # ricalcolo cumulativo per una curva credibile
        px, py = S / 2, S - 6
        pts = [(px, py)]
        for i in range(1, 21):
            t = i / 20
            a = ang + droop * t
            px += math.cos(a) * ln / 20
            py += math.sin(a) * ln / 20
            pts.append((px, py))
        d.line(pts, fill=(48, 70, 30, 255), width=ss * 2)
        for i in range(2, 21):
            t = i / 20
            (x0, y0) = pts[i]
            (xa, ya) = pts[i - 1]
            ta = math.atan2(y0 - ya, x0 - xa)
            leaf = (1 - t) ** 0.7 * 46 * ss * r.uniform(0.85, 1.1) + 6 * ss
            for sgn in (-1, 1):
                la = ta + sgn * 1.15
                ex, ey = x0 + math.cos(la) * leaf, y0 + math.sin(la) * leaf
                nx, ny = -math.sin(la), math.cos(la)
                wd = leaf * 0.2
                c = lerp(dark, light, r.uniform(0.2, 1.0) * (0.5 + 0.5 * t))
                fill = tuple(int(v * 255) for v in c) + (255,)
                d.polygon([(x0, y0), (x0 + math.cos(la) * leaf * 0.5 + nx * wd, y0 + math.sin(la) * leaf * 0.5 + ny * wd),
                           (ex, ey),
                           (x0 + math.cos(la) * leaf * 0.5 - nx * wd, y0 + math.sin(la) * leaf * 0.5 - ny * wd)], fill=fill)
    finish_card(im, "r3_fern", ss)


def gen_flowers():
    ss = 2
    S = 256 * ss
    r = rng(291)
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    kinds = [((246, 244, 236), (236, 196, 50)), ((250, 214, 60), (200, 140, 20)), ((196, 120, 220), (240, 220, 120)), ((250, 250, 250), (240, 200, 60))]
    for _ in range(9):
        x = r.uniform(0.1, 0.9) * S
        top = r.uniform(0.18, 0.55) * S
        bend = r.uniform(-18, 18) * ss
        pts = [(x, S - 2), (x + bend * 0.5, (S + top) / 2), (x + bend, top)]
        d.line(pts, fill=(52, 96, 36, 255), width=max(2, int(2.2 * ss)))
        # foglie sullo stelo
        ly = S - r.uniform(18, 60) * ss
        d.polygon([(x, ly), (x - 22 * ss, ly - 14 * ss), (x - 6 * ss, ly - 3 * ss)], fill=(54, 108, 38, 255))
        petal, center = kinds[r.integers(0, len(kinds))]
        rad = r.uniform(11, 17) * ss
        for k in range(8):
            a = k * math.pi / 4 + r.uniform(-0.1, 0.1)
            cx, cy = x + bend + math.cos(a) * rad * 0.8, top + math.sin(a) * rad * 0.8
            d.ellipse([cx - rad * 0.55, cy - rad * 0.55, cx + rad * 0.55, cy + rad * 0.55], fill=petal + (255,))
        d.ellipse([x + bend - rad * 0.45, top - rad * 0.45, x + bend + rad * 0.45, top + rad * 0.45], fill=center + (255,))
    finish_card(im, "r3_flowers", ss)


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
def gen_sky():
    w, h = 1024, 512
    y = np.linspace(0, 1, h)[:, None]
    zen, mid, hor = col((38, 92, 176)), col((110, 164, 220)), col((206, 224, 238))
    t = y ** 0.8
    grad = np.where(t[..., None] < 0.6, lerp(zen, mid, (t / 0.6)[..., None]), lerp(mid, hor, ((t - 0.6) / 0.4)[..., None]))
    sky = np.broadcast_to(grad, (h, w, 3)).copy()
    # sole: bagliore caldo in alto a destra
    yy, xx = np.mgrid[0:h, 0:w]
    sd = np.sqrt(((xx - 0.78 * w) / w) ** 2 * 1.0 + ((yy - 0.16 * h) / h) ** 2 * 1.6)
    glow = np.exp(-sd * 7.5)[..., None] * col((1.0, 0.86, 0.56)) * 0.55 + np.exp(-sd * 22)[..., None] * col((1.0, 0.95, 0.8)) * 0.8
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
    save("r3_sky", np.clip(sky, 0, 1))


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
    "sky": gen_sky, "mountains": gen_mountains,
}

if __name__ == "__main__":
    only = sys.argv[1:] or list(GENS)
    for k in only:
        GENS[k]()
        print("ok", k)
