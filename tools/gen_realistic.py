#!/usr/bin/env python3
"""Texture "realistiche stilizzate" del mondo estivo 3D: prato, terra del sentiero, roccia.

Colori naturali (niente verde acido), profondità data da ombre reciproche e da una mappa delle altezze illuminata.
Tutte si ripetono senza cuciture.

Uso:  python3 tools/gen_realistic.py [grass] [dirt] [rock] [tuft]
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
        {"grass": gen_grass, "dirt": gen_dirt, "rock": gen_rock, "tuft": gen_tuft}[w]()
        print("fatto", w)
