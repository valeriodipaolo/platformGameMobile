#!/usr/bin/env python3
"""Texture "realistiche stilizzate" del mondo estivo 3D: prato, terra del sentiero, roccia.

Colori naturali (niente verde acido), profondità data da ombre reciproche e da una mappa delle altezze illuminata.
Tutte si ripetono senza cuciture.

Uso:  python3 tools/gen_realistic.py [grass] [dirt] [rock] [tuft] [branch] [firtree] [fern] [bark] [sky] [mountains]
Le immagini sostituiscono r3_grass, r3_dirt, r3_rock in app/src/main/res/drawable-nodpi.
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw
from scipy import ndimage as ndi

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_assets import fbm, lerp, lit_from_height, smooth, save  # noqa: E402

N = 512


def col(*c):
    return np.array(c, dtype=float) / 255.0


def wrapped(draw_fn, S, x0, y0, x1, y1):
    """Ripete il disegno dall'altro lato quando esce dal bordo."""
    for ox in (0, S, -S):
        if (ox == S and x0 >= 0) or (ox == -S and x1 < S):
            continue
        for oy in (0, S, -S):
            if (oy == S and y0 >= 0) or (oy == -S and y1 < S):
                continue
            draw_fn(ox, oy)


def blur_wrap(a, s):
    return ndi.gaussian_filter(a, s, mode="wrap")


def desat(arr, k):
    g = arr.mean(axis=2, keepdims=True)
    return g + (arr - g) * k


# ------------------------------------------------------------------ prato
def blades(S, count, seed, length, width, colors, tint):
    """Uno strato di fili d'erba visti dall'alto: base scura, punta chiara. Restituisce colore e copertura."""
    r = np.random.default_rng(seed)
    im = Image.new("RGB", (S, S), (0, 0, 0))
    cov = Image.new("L", (S, S), 0)
    d, dc = ImageDraw.Draw(im), ImageDraw.Draw(cov)
    for _ in range(count):
        x, y = r.uniform(0, S), r.uniform(0, S)
        a = r.uniform(0, 2 * math.pi)
        ln = r.uniform(*length)
        bend = r.uniform(-0.5, 0.5)
        w = r.uniform(*width)
        c = colors[r.integers(len(colors))] * r.uniform(0.85, 1.12) * tint(x / S, y / S)
        pts = [(x + math.cos(a + bend * t * t) * ln * t, y + math.sin(a + bend * t * t) * ln * t) for t in np.linspace(0, 1, 5)]
        xs, ys = [p[0] for p in pts], [p[1] for p in pts]
        for i in range(4):                                      # quattro tratti: dal piede in ombra alla punta in luce
            k = 0.55 + 0.17 * i
            fill = tuple(int(np.clip(v * k, 0, 1) * 255) for v in c)
            ww = max(1, int(round(w * (1.0 - 0.18 * i))))
            seg = [pts[i], pts[i + 1]]

            def draw(ox, oy, seg=seg, fill=fill, ww=ww):
                s2 = [(px + ox, py + oy) for px, py in seg]
                d.line(s2, fill=fill, width=ww)
                dc.line(s2, fill=255, width=ww)
            wrapped(draw, S, min(xs), min(ys), max(xs), max(ys))
    return np.asarray(im).astype(float) / 255.0, np.asarray(cov).astype(float) / 255.0


def gen_grass():
    ss = 2
    S = N * ss
    macro = fbm(S, S, beta=3.0, seed=501)
    hue = fbm(S, S, beta=2.4, seed=502)
    fine = fbm(S, S, beta=1.0, seed=503)
    mi = lambda u, v: (int(v * S) % S, int(u * S) % S)

    def tint(u, v):
        h = hue[mi(u, v)]
        # zone più fresche (blu-verdi) e zone più secche (verde oliva)
        return lerp(col(205, 245, 235), col(270, 245, 175), smooth((h - 0.3) * 2.5))

    # sotto i fili: terra scura e paglia vecchia
    soil = lerp(col(46, 38, 26), col(74, 62, 40), fine[..., None])
    arr = soil.copy()
    straw, straw_c = blades(S, 1500, 511, (30, 70), (3.0, 4.6), [col(112, 96, 60), col(90, 80, 52), col(128, 112, 74)], lambda u, v: 1.0)
    layers = [
        (straw, straw_c),
        blades(S, 3600, 512, (34, 80), (3.8, 5.6), [col(38, 66, 30), col(46, 74, 34), col(54, 70, 32)], tint),
        blades(S, 3600, 513, (34, 84), (3.4, 5.0), [col(62, 94, 40), col(72, 100, 44), col(58, 88, 46)], tint),
        blades(S, 2200, 514, (30, 72), (3.0, 4.4), [col(92, 120, 52), col(104, 128, 60), col(86, 118, 64)], tint),
        blades(S, 450, 515, (26, 60), (2.6, 3.8), [col(136, 128, 78), col(120, 110, 66)], lambda u, v: 1.0),   # fili secchi in cima
    ]
    height = np.zeros((S, S))
    for i, (c, m) in enumerate(layers):
        # gli strati sopra fanno ombra a quelli sotto, un po' spostata verso la luce opposta
        sh = blur_wrap(np.roll(np.roll(m, 3 * ss, 0), 2 * ss, 1), 2.2 * ss)
        arr *= (1 - 0.32 * sh)[..., None]
        mm = np.clip(m * 1.1, 0, 1)[..., None]
        arr = arr * (1 - mm) + c * mm
        height = np.where(m > 0.5, i + 1, height)
    # occlusione: dove ci sono meno strati sopra, più buio
    ao = np.clip(0.68 + 0.07 * height, 0, 1.05)
    arr *= blur_wrap(ao, 1.2 * ss)[..., None]
    # variazione ampia, tenue
    arr *= (0.82 + 0.36 * macro)[..., None]
    img = Image.fromarray((np.clip(arr, 0, 1) * 255).astype(np.uint8)).resize((N, N), Image.LANCZOS)
    out = np.asarray(img).astype(float) / 255.0
    out = desat(out, 0.95) * np.array([0.86, 1.0, 1.08])                      # sotto il sole caldo del gioco tende al giallo: un po' più fresco
    lum = (out @ np.array([0.2126, 0.7152, 0.0722])).mean()
    save("r3_grass", np.clip(out * (0.31 / lum), 0, 1))                       # luminosità media fissa: sotto il sole del gioco


# ------------------------------------------------------------------ terra battuta
def gen_dirt():
    ss = 2
    S = N * ss
    low = fbm(S, S, beta=2.6, seed=601)
    mid = fbm(S, S, beta=1.7, seed=602)
    fine = fbm(S, S, beta=0.9, seed=603)
    wet = smooth((fbm(S, S, beta=2.9, seed=604) - 0.45) * 3.0)
    h = low * 6 + mid * 3.5 + fine * 1.2
    base = lerp(col(92, 70, 48), col(128, 102, 72), mid[..., None])
    base = lerp(base, col(70, 54, 40), (wet * 0.6)[..., None])                  # zone più umide e scure
    colr = base * (0.86 + 0.28 * fine)[..., None]
    r = np.random.default_rng(605)
    # sassolini mezzi sepolti: rilievo morbido, colori grigio-beige
    peb = np.zeros((S, S))
    pcol = np.zeros((S, S, 3))
    pm = np.zeros((S, S))
    for _ in range(380):
        cx, cy = r.uniform(0, S), r.uniform(0, S)
        big = r.random() < 0.12
        rx = r.uniform(5, 13) * ss if big else r.uniform(1.5, 5) * ss
        ry = rx * r.uniform(0.55, 0.95)
        a = r.uniform(0, math.pi)
        R = int(rx + 3)
        yy, xx = np.mgrid[-R:R + 1, -R:R + 1]
        u = (xx * math.cos(a) + yy * math.sin(a)) / rx
        v = (-xx * math.sin(a) + yy * math.cos(a)) / ry
        dd = np.clip(1 - (u * u + v * v), 0, 1)
        bump = np.sqrt(dd) * (rx / ss) * 0.35
        g = r.uniform(0.75, 1.15)
        c = lerp(col(132, 124, 110), col(112, 98, 80), r.random()) * g
        ys = (np.arange(-R, R + 1) + int(cy)) % S
        xs = (np.arange(-R, R + 1) + int(cx)) % S
        sub = np.ix_(ys, xs)
        take = bump > peb[sub]
        peb[sub] = np.where(take, bump, peb[sub])
        pm[sub] = np.maximum(pm[sub], (dd > 0.02).astype(float))
        pc = pcol[sub]
        pc[take] = c
        pcol[sub] = pc
    # rametti e pezzetti di foglia secca
    twig = Image.new("L", (S, S), 0)
    dt = ImageDraw.Draw(twig)
    for _ in range(70):
        x, y, a, ln = r.uniform(0, S), r.uniform(0, S), r.uniform(0, 6.3), r.uniform(10, 40) * ss
        w = int(r.uniform(1.2, 2.6) * ss)
        x2, y2 = x + math.cos(a) * ln, y + math.sin(a) * ln

        def draw(ox, oy):
            dt.line([(x + ox, y + oy), (x2 + ox, y2 + oy)], fill=255, width=w)
        wrapped(draw, S, min(x, x2), min(y, y2), max(x, x2), max(y, y2))
    tw = np.asarray(twig).astype(float) / 255.0
    h = h + peb + tw * 0.8
    diff, _ = lit_from_height(blur_wrap(h, 0.7 * ss), wrap=True)
    ao = np.clip(1 - (blur_wrap(h, 4 * ss) - h) * 0.25, 0.55, 1.1)            # buio ai piedi dei sassi
    colr = colr * (1 - pm[..., None]) + pcol * pm[..., None]
    colr = colr * (1 - tw[..., None]) + col(84, 64, 42) * tw[..., None]
    out = colr * (0.48 + 0.72 * diff)[..., None] * ao[..., None]
    img = Image.fromarray((np.clip(out, 0, 1) * 255).astype(np.uint8)).resize((N, N), Image.LANCZOS)
    save("r3_dirt", np.asarray(img).astype(float) / 255.0)


# ------------------------------------------------------------------ roccia
def gen_rock():
    S = N
    yy, xx = np.mgrid[0:S, 0:S].astype(float)
    warp = (fbm(S, S, beta=2.5, seed=701) - 0.5) * 60, (fbm(S, S, beta=2.5, seed=702) - 0.5) * 60
    r = np.random.default_rng(703)
    pts = r.uniform(0, S, (26, 2))
    # celle di Voronoi deformate: crepe irregolari, non segmenti dritti
    d1 = np.full((S, S), 1e9)
    d2 = np.full((S, S), 1e9)
    px, py = xx + warp[0], yy + warp[1]
    for (cx, cy) in pts:
        for ox in (-S, 0, S):
            for oy in (-S, 0, S):
                d = np.hypot(px - cx - ox, py - cy - oy)
                d2 = np.where(d < d1, d1, np.minimum(d2, d))
                d1 = np.minimum(d1, d)
    edge = d2 - d1
    crack = np.exp(-edge / 3.0)
    big = fbm(S, S, beta=2.8, seed=704)
    mid = fbm(S, S, beta=1.9, seed=705)
    fine = fbm(S, S, beta=1.0, seed=706)
    strata = 0.5 + 0.5 * np.sin((yy + warp[1] * 1.5) / S * 2 * math.pi * 7)   # leggere venature
    h = big * 9 + mid * 4 + fine * 1.4 + strata * 0.8 - crack * 5 + np.minimum(edge, 25) * 0.08
    diff, _ = lit_from_height(blur_wrap(h, 0.6), wrap=True)
    base = lerp(col(104, 100, 92), col(150, 144, 132), mid[..., None])
    base = lerp(base, col(122, 108, 90), (smooth((big - 0.5) * 3) * 0.5)[..., None])    # zone più calde, ossidate
    # licheni chiari a macchie e muschio nelle crepe
    lich = smooth((fbm(S, S, beta=1.4, seed=707) - 0.68) * 8)
    base = lerp(base, col(168, 166, 132), (lich * 0.55)[..., None])
    moss = np.clip(crack * 1.6, 0, 1) * smooth((fbm(S, S, beta=2.2, seed=708) - 0.4) * 3)
    base = lerp(base, col(62, 78, 40), (moss * 0.75)[..., None])
    ao = np.clip(1 - crack * 0.55, 0.3, 1)
    out = base * (0.42 + 0.78 * diff)[..., None] * ao[..., None] * (0.9 + 0.2 * fine)[..., None]
    save("r3_rock", np.clip(out, 0, 1))



# ------------------------------------------------------------------ abeti: rami, sagome lontane, felci, corteccia
NEEDLE = [col(26, 48, 34), col(34, 60, 40), col(44, 72, 44), col(58, 88, 50)]
TIP = [col(84, 116, 58), col(96, 126, 64)]


def needle_twig(d, x, y, ang, ln, ss, r, shade, needle_len=7.0, density=1.0):
    """Rametto coperto di aghi: gli aghi puntano in avanti, i più giovani in punta sono più chiari."""
    steps = max(3, int(ln / (1.6 * ss) * density))
    ca, sa = math.cos(ang), math.sin(ang)
    for i in range(steps):
        t = i / (steps - 1)
        px, py = x + ca * ln * t, y + sa * ln * t
        nl = needle_len * ss * (1.0 - 0.45 * t) * r.uniform(0.75, 1.15)
        tip = t > 0.78 and r.random() < 0.7
        for side in (-1, 1):
            a = ang + side * r.uniform(0.55, 1.05)
            c = (TIP[r.integers(2)] if tip else NEEDLE[r.integers(4)]) * shade * r.uniform(0.85, 1.12)
            fill = tuple(int(np.clip(v, 0, 1) * 255) for v in c) + (255,)
            d.line([(px, py), (px + math.cos(a) * nl, py + math.sin(a) * nl)], fill=fill, width=max(2, int(1.25 * ss)))
    d.line([(x, y), (x + ca * ln, y + sa * ln)], fill=(58, 44, 32, 255), width=max(1, int(0.9 * ss)))


def card(name, im, ss):
    """Rimpicciolisce e rende netto il contorno (le foglie si ritagliano con soglia nel gioco)."""
    w, h = im.size
    out = im.resize((w // ss, h // ss), Image.LANCZOS)
    a = np.asarray(out).astype(float) / 255.0
    rgb, al = a[..., :3], a[..., 3]
    # colore senza alone scuro ai bordi: dove l'alfa è basso si prende il colore dei vicini
    w8 = ndi.gaussian_filter(al, 2.0)[..., None] + 1e-4
    fill = np.dstack([ndi.gaussian_filter(rgb[..., i] * al, 2.0) for i in range(3)]) / w8
    rgb = np.where(al[..., None] > 0.5, rgb / np.maximum(al[..., None], 1e-3), fill)
    al = np.clip((al - 0.25) * 1.6, 0, 1)
    save(name, np.dstack([np.clip(rgb, 0, 1), al]))


def gen_branch():
    """Ramo d'abete per le chiome vicine: fusto da sinistra a destra, rametti laterali fitti, punta a destra."""
    ss = 2
    W, H = 512 * ss, 256 * ss
    r = np.random.default_rng(901)
    im = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    cy = H / 2

    def half(t):                                                     # metà altezza della chioma lungo il ramo
        return (0.18 + 0.82 * math.sin(min(1.0, t / 0.35) * math.pi / 2)) * (1 - 0.72 * max(0.0, (t - 0.45) / 0.55)) * 88 * ss

    def stem(x):
        return cy + math.sin(x / W * 2.6) * 8 * ss * (x / W)
    # rametti dietro (scuri) poi davanti (chiari): profondità
    for layer, shade in ((0, 0.62), (1, 0.82), (2, 1.0)):
        x = 0.04 * W
        while x < 0.95 * W:
            t = x / W
            hh = half(t)
            for side in (-1, 1):
                if r.random() < 0.15:
                    continue
                ln = hh * r.uniform(0.75, 1.1) / math.sin(0.95)
                ang = side * r.uniform(0.75, 1.15) + r.uniform(-0.1, 0.1)
                y0 = stem(x) + r.uniform(-4, 4) * ss + layer * side * 3 * ss
                needle_twig(d, x, y0, ang if side > 0 else ang, ln, ss, r, shade * (0.85 if side > 0 else 1.0))
            x += r.uniform(9, 14) * ss
    d.line([(x, stem(x)) for x in range(int(0.02 * W), int(0.97 * W), 6 * ss)], fill=(62, 46, 32, 255), width=int(3.0 * ss))
    for x in np.linspace(0.6 * W, 0.97 * W, 30):                      # punta giovane
        needle_twig(d, x, stem(x), r.uniform(-0.4, 0.4), r.uniform(10, 18) * ss, ss, r, 1.15, 5.0)
    card("r3_branch", im, ss)


def gen_firtree():
    """Abete intero visto di lato, per le file lontane: palchi di rami che scendono, più scuri dentro e in basso."""
    ss = 2
    W, H = 256 * ss, 512 * ss
    r = np.random.default_rng(911)
    im = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    cx = W / 2
    top, bot = 0.035 * H, 0.94 * H
    d.line([(cx, bot - 20 * ss), (cx, H)], fill=(66, 50, 36, 255), width=int(6 * ss))
    y = top + 4 * ss
    while y < bot:
        t = (y - top) / (bot - top)
        reach = (0.06 + 0.94 * t ** 0.85) * 0.5 * W * r.uniform(0.85, 1.08)
        shade = 0.7 + 0.35 * (1 - t) ** 0.5
        for side in (-1, 1):
            if r.random() < 0.1:
                continue
            ang = math.pi / 2 - side * (math.pi / 2 - r.uniform(0.25, 0.55))     # rami che scendono un po'
            ex, ey = cx + math.cos(ang) * reach, y + math.sin(ang) * reach
            n = max(2, int(reach / (6 * ss)))
            for k in range(n + 1):
                u = k / (n + 1)
                bx, by = cx + (ex - cx) * u, y + (ey - y) * u + math.sin(u * math.pi) * 3 * ss
                needle_twig(d, bx, by, ang + r.uniform(-0.9, 0.9), reach * 0.32 * (1.1 - u * 0.6), ss, r, shade * r.uniform(0.85, 1.05), 5.0, 0.8)
        y += r.uniform(5, 9) * ss
    card("r3_firtree", im, ss)


def gen_fern():
    """Fronda di felce: foglioline a lobi, nervatura centrale; picciolo nudo a destra."""
    ss = 2
    W, H = 512 * ss, 128 * ss
    r = np.random.default_rng(921)
    im = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    cy = H / 2
    tip, base = 0.03 * W, 0.68 * W
    d.line([(tip, cy), (W, cy)], fill=(70, 78, 40, 255), width=int(2.4 * ss))
    x = tip
    while x < base:
        t = (x - tip) / (base - tip)                                    # 0 punta, 1 base
        ln = (0.12 + 0.88 * math.sin(min(1.0, t / 0.75) * math.pi / 2) * (1 - 0.3 * max(0, (t - 0.75) / 0.25))) * 0.47 * H
        for side in (-1, 1):
            ang = side * (math.pi / 2 - 0.55)
            g = r.uniform(0.85, 1.1)
            base_c = lerp(col(54, 92, 36), col(84, 120, 46), r.random()) * g
            # fogliolina: lama stretta che si assottiglia, bordo a dentelli, metà in luce e metà in ombra
            dx, dy = -abs(math.cos(ang)), math.sin(ang)
            nx_, ny_ = -dy, dx
            wmax = 3.6 * ss * (0.6 + 0.4 * min(1.0, ln / (0.4 * H)))
            left, right = [], []
            m = 12
            for k in range(m + 1):
                u = k / m
                w = wmax * (1 - u) ** 0.8 * (0.75 + 0.25 * (k % 2))
                cxp, cyp = x + dx * ln * u, cy + dy * ln * u
                left.append((cxp + nx_ * w, cyp + ny_ * w))
                right.append((cxp - nx_ * w, cyp - ny_ * w))
            c1 = tuple(int(np.clip(v * 1.05, 0, 1) * 255) for v in base_c) + (255,)
            c2 = tuple(int(np.clip(v * 0.72, 0, 1) * 255) for v in base_c) + (255,)
            mid = [(x + dx * ln * k / m, cy + dy * ln * k / m) for k in range(m + 1)]
            d.polygon(left + mid[::-1], fill=c1)
            d.polygon(right + mid[::-1], fill=c2)
            d.line([(x, cy), (x + dx * ln, cy + dy * ln)], fill=tuple(int(v * 0.6 * 255) for v in base_c) + (255,), width=max(1, int(0.7 * ss)))
        x += 5.5 * ss
    card("r3_fern", im, ss)


def gen_bark():
    """Corteccia d'abete: placche verticali grigio-brune separate da fessure scure. Si ripete in verticale e in orizzontale."""
    W, H = 256, 512
    yy, xx = np.mgrid[0:H, 0:W].astype(float)
    r = np.random.default_rng(931)
    pts = r.uniform(0, 1, (70, 2)) * [W, H]
    wx = (fbm(H, W, beta=2.4, seed=932) - 0.5) * 14
    wy = (fbm(H, W, beta=2.4, seed=933) - 0.5) * 30
    px, py = xx + wx, (yy + wy) * 0.33                                   # celle allungate in verticale
    d1 = np.full((H, W), 1e9)
    d2 = np.full((H, W), 1e9)
    for (cx, cy) in pts:
        for ox in (-W, 0, W):
            for oy in (-H, 0, H):
                dd = np.hypot(px - cx - ox, py - (cy + oy) * 0.33)
                d2 = np.where(dd < d1, d1, np.minimum(d2, dd))
                d1 = np.minimum(d1, dd)
    edge = d2 - d1
    fiss = np.exp(-edge / 2.2)
    fine = fbm(H, W, beta=1.1, seed=934, ay=0.35)
    mid = fbm(H, W, beta=2.0, seed=935)
    h = np.minimum(edge, 9) * 0.9 + fine * 3 - fiss * 4
    diff, _ = lit_from_height(ndi.gaussian_filter(h, 0.6, mode="wrap"), wrap=True)
    base = lerp(col(78, 64, 52), col(118, 104, 90), mid[..., None])
    base = lerp(base, col(150, 140, 124), (smooth((fine - 0.6) * 4) * 0.4)[..., None])   # scaglie chiare
    lich = smooth((fbm(H, W, beta=1.5, seed=936) - 0.7) * 8)
    base = lerp(base, col(120, 132, 96), (lich * 0.5)[..., None])
    out = base * (0.42 + 0.75 * diff)[..., None] * (1 - fiss * 0.6)[..., None]
    save("r3_bark", np.clip(out, 0, 1))


# ------------------------------------------------------------------ cielo e montagne
HAZE = col(190, 216, 238)            # = colore della foschia del mondo estivo (Scene3D.FOG_*)


def gen_sky():
    """Cielo che riempie la parte alta dello schermo: azzurro profondo in alto, velato all'orizzonte, cumuli visti in prospettiva."""
    W, H = 1024, 512
    v = np.linspace(0, 1, H)[:, None]                                   # 0 in alto, 1 all'orizzonte
    sky = lerp(col(40, 98, 182), col(98, 154, 220), np.clip(v * 1.3, 0, 1)[..., None])
    sky = lerp(sky, HAZE, (smooth((v - 0.5) / 0.5) ** 1.3)[..., None])
    # piano delle nuvole visto in prospettiva: ogni riga guarda più lontano, le nuvole si rimpiccioliscono in entrambe le direzioni
    NZ, NX = 1024, 1024
    noise = fbm(NZ, NX, beta=2.6, seed=950) * 0.78 + fbm(NZ, NX, beta=1.4, seed=951) * 0.22
    elev = (1 - v[:, 0]) * 1.0 + 0.09                                   # angolo sopra l'orizzonte (radianti)
    dist = 1.0 / np.tan(elev)
    s_ = 260.0
    rows = (dist * s_).astype(int) % NZ
    xs = (np.arange(W) - W / 2) / (W / 2) * 1.1
    cols_ = (xs[None, :] * dist[:, None] * s_).astype(int) % NX
    dens = noise[rows[:, None], cols_]
    far = smooth((dist[:, None] - 3.0) / 6.0)                           # lontano il rumore si confonderebbe: si va verso la media
    dens = lerp(dens, ndi.uniform_filter(dens, 9), far)
    c = smooth(np.clip((dens - 0.55) * 5.0, 0, 1))
    c *= 1 - smooth((v - 0.82) / 0.18)                                  # all'orizzonte si perdono nella foschia
    soft = ndi.gaussian_filter(c, 1.2)
    belly = np.clip((ndi.gaussian_filter(c, 4) - np.roll(ndi.gaussian_filter(c, 4), 6, 0)) * 3 + 0.5, 0, 1)   # pancia in ombra
    cloud = lerp(col(150, 166, 190), col(252, 250, 246), (1 - belly * 0.8)[..., None])
    cloud = lerp(cloud, HAZE, (v * 0.6)[..., None])
    out = sky * (1 - soft[..., None]) + cloud * soft[..., None]
    save("r3_sky", np.clip(out, 0, 1))


def gen_mountains():
    """Catene di montagne disegnate davvero: un terreno frattale visto in prospettiva (come i vecchi simulatori di volo),
    con rocce illuminate dal sole, neve in alto, boschi in basso e foschia che cresce con la distanza.
    Si ripete in orizzontale; in basso diventa trasparente."""
    W, H = 2048, 512
    NZ = 1024                       # righe del terreno, 8 unità ciascuna
    # rumore a bassa risoluzione ingrandito: rilievi larghi chilometri, non aghi
    wide = lambda a: ndi.zoom(a, (8, 4), order=3, mode="grid-wrap")[:NZ, :W]
    n1 = wide(fbm(NZ // 8, W // 4, beta=4.0, seed=980))
    n2 = wide(fbm(NZ // 8, W // 4, beta=3.4, seed=981))
    ridged = (1 - np.abs(2 * n2 - 1)) ** 2
    det = wide(fbm(NZ // 8, W // 4, beta=2.8, seed=982))
    z_of_row = np.arange(NZ) * 8.0 + 200
    amp = (np.clip((z_of_row - 200) / 4000, 0, 1) ** 0.7 * 2600 + 260)[:, None]   # lontano le vette, vicino le colline
    T = amp * (0.55 * n1 + 0.45 * ridged + 0.05 * det) - 0.25 * amp
    T += (amp * 0.28 * smooth((z_of_row[:, None] - 5000) / 2500)) * (0.7 + 0.6 * n2)   # catena di fondo continua
    gz, gx = np.gradient(ndi.gaussian_filter(T, (1.5, 2.5), mode="wrap"), 8.0, 8.0)                                   # una colonna = 16 unità di terreno
    L = np.array([0.55, 0.62, -0.56]); L /= np.linalg.norm(L)            # sole da destra e alto (x, su, verso la camera)
    nrm = np.sqrt(gx * gx + gz * gz + 1)
    diff = np.clip((-gx * L[0] + L[1] - gz * L[2]) / nrm, 0, 1)
    slope = np.sqrt(gx * gx + gz * gz)
    snow_h = amp * 0.62 + (det - 0.5) * amp * 0.3
    snow = smooth((T - snow_h) / (amp * 0.1 + 1)) * smooth((1.15 - slope) / 0.5)
    forest = smooth((amp * 0.18 - T) / (amp * 0.12 + 1)) * smooth((1.0 - slope) / 0.6)
    rock = lerp(col(92, 88, 84), col(140, 132, 122), det[..., None])
    colr = lerp(rock, lerp(col(28, 46, 32), col(52, 72, 44), n1[..., None]), forest[..., None])
    colr = lerp(colr, col(240, 244, 250), snow[..., None])
    lit = colr * (0.3 + 0.95 * diff)[..., None] + colr * snow[..., None] * 0.1
    horizon, f, h0 = 430.0, 1500.0, 700.0                                 # camera alta: si vedono le valli e le catene una dietro l'altra
    out = np.zeros((H, W, 3))
    alpha = np.zeros((H, W))
    ybuf = np.full(W, H, dtype=float)
    rowsY = np.arange(H)[:, None]
    xs = np.arange(W)
    for zi in range(NZ - 1, 0, -1):                                     # dal più lontano al più vicino
        z = z_of_row[zi]
        ys = horizon - f * (T[zi] - h0) / z
        ys = np.clip(ys, 0, H)
        haze = 1 - np.exp(-z / 5200.0)
        c = lerp(lit[zi], HAZE, haze * 0.85)
        m = rowsY >= ys[None, :]
        out[m] = np.broadcast_to(c[None, :, :], (H, W, 3))[m]
        alpha[m] = 1
    # venature della roccia lungo le pareti (nello spazio dell'immagine): niente strisce tutte uguali
    vein = ndi.gaussian_filter(fbm(H, W, beta=1.6, seed=983), (2.0, 0.6))
    out *= (0.86 + 0.28 * vein)[..., None]
    out = lerp(out, HAZE * 0.92, (smooth((rowsY - 430) / 60) * 0.85)[..., None])
    alpha = alpha * (1 - smooth((rowsY - 476) / 36))
    save("r3_mountains", np.dstack([np.clip(out, 0, 1), np.clip(alpha, 0, 1)]))


# ------------------------------------------------------------------ ciuffi d'erba (carte verticali)
def gen_tuft():
    """Ricolora i ciuffi con la stessa tavolozza del prato: stessa forma, verde naturale invece che acido."""
    path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "res", "drawable-nodpi", "r3_tuft.png")
    src = os.path.join(os.path.dirname(path), "r3_tuft.png")
    a = np.asarray(Image.open(src).convert("RGBA")).astype(float) / 255.0
    rgb, al = a[..., :3], a[..., 3:]
    lum = rgb @ np.array([0.2126, 0.7152, 0.0722])
    m = al[..., 0] > 0.2
    lo, hi = np.percentile(lum[m], 3), np.percentile(lum[m], 97)
    t = np.clip((lum - lo) / (hi - lo + 1e-6), 0, 1)[..., None]
    dark, midc, light = col(30, 50, 24), col(66, 94, 42), col(128, 140, 80)
    out = np.where(t < 0.5, lerp(dark, midc, t * 2), lerp(midc, light, t * 2 - 1))
    hue = fbm(a.shape[0], a.shape[1], beta=2.0, seed=801)[..., None]
    out = out * lerp(col(235, 255, 265), col(268, 255, 205), hue)
    save("r3_tuft", np.dstack([np.clip(out, 0, 1), al[..., 0]]))


if __name__ == "__main__":
    which = sys.argv[1:] or ["grass", "dirt", "rock"]           # "tuft" a parte: ricolora l'immagine esistente, una volta sola
    for w in which:
        {"grass": gen_grass, "dirt": gen_dirt, "rock": gen_rock, "tuft": gen_tuft,
         "branch": gen_branch, "firtree": gen_firtree, "fern": gen_fern, "bark": gen_bark,
         "sky": gen_sky, "mountains": gen_mountains}[w]()
        print("fatto", w)
