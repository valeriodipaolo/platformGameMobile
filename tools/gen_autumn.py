#!/usr/bin/env python3
"""Immagini dei livelli d'autunno: sfondi, terreno e blocchi arrotondati, nemici nuovi (2D) e texture del bosco (3D).

Uso:  python3 tools/gen_autumn.py [nome ...]      (nessun nome = tutto)
Richiede numpy, scipy, Pillow e gli script gen_assets.py / gen_assets3d.py accanto a questo file.
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
from gen_assets import fbm, fbm1, lerp, smooth, Cv, T, down  # noqa: E402

LEAF_COLORS = [(176, 48, 30), (220, 112, 30), (232, 184, 50), (130, 80, 36), (200, 76, 34), (240, 150, 40)]


def capture(mod, fn, *args, **kw):
    """Esegue un generatore intercettando l'immagine che salverebbe (restituisce l'array)."""
    got = {}
    orig = mod.save

    def sv(name, arr, folder=None):
        got["a"] = np.array(arr)
    mod.save = sv
    try:
        fn(*args, **kw)
    finally:
        mod.save = orig
    return got["a"]


def leaf_poly(x, y, ln, wd, ang):
    pts = [(-0.5, 0), (-0.2, -0.5), (0.25, -0.45), (0.5, 0), (0.25, 0.45), (-0.2, 0.5)]
    ca, sa = math.cos(ang), math.sin(ang)
    return [(x + (px * ln) * ca - (py * wd) * sa, y + (px * ln) * sa + (py * wd) * ca) for px, py in pts]


def scatter_leaves(arr, count, seed, size=(9, 17), ss=2, palette=LEAF_COLORS, yband=None):
    """Foglie cadute sparse su una texture ripetibile (con le copie ai bordi per non lasciare cuciture)."""
    h, w = arr.shape[:2]
    im = Image.fromarray((np.clip(arr[..., :3], 0, 1) * 255).astype(np.uint8), "RGB").resize((w * ss, h * ss), Image.LANCZOS)
    d = ImageDraw.Draw(im)
    r = np.random.default_rng(seed)
    for _ in range(count):
        x = r.uniform(0, w)
        y = r.uniform(*(yband or (0, h)))
        ln = r.uniform(*size)
        wd = ln * r.uniform(0.45, 0.65)
        a = r.uniform(0, 6.28)
        c = np.array(palette[r.integers(len(palette))]) * r.uniform(0.78, 1.1)
        c = tuple(int(min(255, v)) for v in c)
        for ox in (-w, 0, w):
            for oy in (-h, 0, h):
                if -ln <= x + ox <= w + ln and -ln <= y + oy <= h + ln:
                    d.polygon(leaf_poly((x + ox) * ss, (y + oy) * ss, ln * ss, wd * ss, a), fill=c)
                    d.line([((x + ox) * ss - math.cos(a) * ln * ss * 0.4, (y + oy) * ss - math.sin(a) * ln * ss * 0.4),
                            ((x + ox) * ss + math.cos(a) * ln * ss * 0.45, (y + oy) * ss + math.sin(a) * ln * ss * 0.45)],
                           fill=tuple(int(v * 0.6) for v in c), width=max(1, ss // 2))
    out = np.asarray(im.resize((w, h), Image.LANCZOS)).astype(float) / 255.0
    if arr.shape[2] == 4:
        out = np.dstack([out, arr[..., 3]])
    return out


def autumnize(rgb, gold, red, strength=0.9):
    """Porta i verdi verso oro e rosso. gold/red: pesi (0..1) per pixel."""
    r_, g_, b_ = rgb[..., 0], rgb[..., 1], rgb[..., 2]
    amount = np.clip((g_ - np.maximum(r_, b_)) * 5.0, 0, 1) * strength
    gd = np.dstack([np.clip(g_ * 1.15 + r_ * 0.30 + 0.02, 0, 1), np.clip(g_ * 0.78 + r_ * 0.12, 0, 1), np.clip(b_ * 0.4, 0, 1)])
    rd = np.dstack([np.clip(g_ * 1.0 + r_ * 0.4 + 0.05, 0, 1), np.clip(g_ * 0.40, 0, 1), np.clip(b_ * 0.3, 0, 1)])
    tgt = gd * (1 - red[..., None]) + rd * red[..., None]
    k = (amount * gold)[..., None]
    return rgb * (1 - k) + tgt * k


# ------------------------------------------------------------------ 2D: sfondi
def au_sky():
    old = (G.SKY_TOP, G.SKY_MID, G.SKY_HOR)
    G.SKY_TOP, G.SKY_MID, G.SKY_HOR = np.array([62, 112, 176]) / 255.0, np.array([156, 194, 222]) / 255.0, np.array([252, 222, 172]) / 255.0
    try:
        a = capture(G, G.gen_sky)
    finally:
        G.SKY_TOP, G.SKY_MID, G.SKY_HOR = old
    G.save("au_sky", np.clip(a * np.array([1.03, 0.97, 0.88]), 0, 1))


def au_mountains():
    a = capture(G, G.gen_mountains)
    a[..., :3] = np.clip(a[..., :3] * np.array([1.08, 0.90, 0.80]) + np.array([0.03, 0.0, -0.02]), 0, 1)
    G.save("au_mountains", a)


def au_hills():
    """Colline di alberi a chioma tonda: rossi, arancio, gialli e qualche verde."""
    h, w = 560, 2048
    X = np.arange(w)
    Y = np.arange(h)[:, None].astype(float)
    rgb = np.zeros((h, w, 3))
    alpha = np.zeros((h, w))
    haze = np.array([244, 208, 168]) / 255.0
    r = np.random.default_rng(131)
    pal = np.array([(176, 56, 32), (214, 112, 30), (228, 176, 46), (120, 150, 44), (150, 88, 40), (196, 84, 36)]) / 255.0
    layers = [(32, 34, 60, 22, 26, 9, 0.55), (33, 118, 80, 40, 48, 8, 0.28), (34, 232, 70, 76, 92, 12, 0.0)]
    for seed, y0, amp, tw, th, rows, hz in layers:
        ridge = y0 + amp * fbm1(w, beta=2.6, seed=seed)
        for k in range(rows):
            nt = w // tw
            hs = r.uniform(0.5, 1.0, nt)
            ci = r.integers(0, len(pal), nt)
            tint = r.uniform(0.85, 1.12, nt)
            cuts = np.sort(r.uniform(0, w, nt))
            ti = np.searchsorted(cuts, X, side="right") - 1
            left = np.where(ti < 0, cuts[-1] - w, cuts[ti])
            right = np.where(ti < 0, cuts[0], np.where(ti + 1 < nt, cuts[(ti + 1) % nt], cuts[0] + w))
            width = np.maximum(right - left, 1.0)
            u = (X - left) / width
            dome = np.sqrt(np.clip(1 - (2 * u - 1) ** 2, 0, 1)) ** 0.9 * np.clip(width / tw, 0.5, 1.2)
            top = ridge + k * th * 0.42 - th * hs[ti] * dome
            dd = Y - top[None, :]
            inside = np.clip(dd / 1.2, 0, 1)
            g = np.exp(-np.clip(dd, 0, None) / (th * 0.6))
            side = 0.8 + 0.35 * (1 - u)[None, :]
            colv = pal[ci[ti]][None, :, :] * (0.5 + 0.7 * g[..., None] * side[..., None]) * tint[ti][None, :, None]
            if k == rows - 1:
                colv = lerp(colv, np.array([40, 20, 12]) / 255.0, (np.clip(dd / 260.0, 0, 1) * 0.8)[..., None])
            colv = lerp(colv, haze, hz * (1 - 0.3 * k / rows))
            a = inside[..., None]
            rgb = colv * a + rgb * (1 - a)
            alpha = inside + alpha * (1 - inside)
    grain = 0.88 + 0.24 * fbm(h, w, beta=0.9, seed=139)
    G.save("au_hills", np.dstack([np.clip(rgb * grain[..., None], 0, 1), alpha]))


# ------------------------------------------------------------------ 2D: terreno e blocchi
def wav(y):
    return 5.5 + 3.2 * np.sin(2 * np.pi * y / T + 0.8) + 1.8 * np.sin(4 * np.pi * y / T + 2.1)


def side_variant(rgb, kind, top):
    """Tessera con il lato sinistro/destro morbido (ondulato, periodico: le tessere impilate si saldano) e,
    sulla tessera d'erba, lo spigolo alto arrotondato."""
    yy = np.arange(T)[:, None].astype(float)
    xx = np.arange(T)[None, :].astype(float)
    e = wav(yy)
    if top:
        R = 30.0
        cx = R + 5.0
        corner = np.where(yy < R, cx - np.sqrt(np.clip(R * R - (R - yy) ** 2, 0, None)), 0)
        e = np.maximum(e, corner)
    d = xx - e
    alpha = np.clip(d + 0.5, 0, 1)
    shade = 0.74 + 0.26 * smooth(d / 11.0)
    if kind == "r":
        alpha, shade = alpha[:, ::-1], shade[:, ::-1]
    out = rgb * shade[..., None]
    if kind == "r":
        pass
    return np.dstack([np.clip(out, 0, 1), alpha])


def au_ground():
    s = 2
    n = T * s
    dirt = G.dirt_tex(s) * np.array([1.06, 0.95, 0.86])
    r = np.random.default_rng(144)
    edge = 30 + 14 * (fbm1(n, beta=2.0, seed=45) - 0.5)
    blades = np.repeat(r.uniform(0, 9, n // 2), 2)
    yb = (edge + blades)[None, :] * s
    yy = np.arange(n)[:, None].astype(float)
    streak = fbm(n, n, beta=1.6, seed=146, ax=1.0, ay=7.0)
    fine = fbm(n, n, beta=0.8, seed=147)
    patch = smooth((fbm(n, n, beta=2.4, seed=148) - 0.5) * 5.0)
    g_dark, g_light = np.array([86, 70, 20]) / 255.0, np.array([196, 168, 60]) / 255.0
    t = np.clip(0.15 + 0.9 * streak + 0.35 * (fine - 0.5), 0, 1)
    grass = lerp(g_dark, g_light, t[..., None])
    orange = lerp(np.array([120, 56, 20]) / 255.0, np.array([214, 112, 36]) / 255.0, t[..., None])
    grass = lerp(grass, orange, (patch * 0.55)[..., None])
    sun = 1.0 + 0.30 * np.exp(-yy / (6.0 * s))
    deep = 0.62 + 0.38 * np.exp(-np.clip(yy - 8 * s, 0, None) / (22.0 * s))
    grass = np.clip(grass * (sun * deep)[..., None], 0, 1)
    a = np.clip((yb - yy) / 1.5, 0, 1)
    shadow = 1 - 0.55 * np.exp(-np.clip(yy - yb, 0, None) / (5.0 * s)) * (1 - a)
    rgb = dirt * shadow[..., None]
    rgb = grass * a[..., None] + rgb * (1 - a[..., None])
    rgb = scatter_leaves(rgb, 26, 149, size=(11 * s // 2, 19 * s // 2), ss=2, yband=(2, 30 * s))
    grass_t = down(rgb, s)
    dirt_t = down(dirt, s)
    G.save("au_grass", grass_t)
    G.save("au_dirt", dirt_t)
    for kind, nm in (("l", "l"), ("r", "r")):
        G.save("au_grass_" + nm, side_variant(grass_t, kind, True))
        G.save("au_dirt_" + nm, side_variant(dirt_t, kind, False))
    # ciuffi d'erba secca e foglie sopra il bordo
    fw, fh = T, 28
    cv = Cv(fw, fh, 3)
    im = Image.new("RGBA", (cv.w, cv.h), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    pal = [(110, 96, 28), (168, 140, 44), (206, 168, 56), (200, 96, 30), (146, 150, 44)]
    for _ in range(110):
        x = r.uniform(0, fw)
        hgt = r.uniform(7, 24)
        lean = r.uniform(-5, 5)
        wd = r.uniform(1.6, 3.2)
        c = pal[r.integers(len(pal))]
        c = tuple(int(v * r.uniform(0.85, 1.12)) for v in c) + (255,)
        for off in (-fw, 0, fw):
            d.polygon([((x - wd + off) * 3, fh * 3), ((x + wd + off) * 3, fh * 3), ((x + lean + off) * 3, (fh - hgt) * 3)], fill=c)
    arr = np.array(im).astype(float) / 255.0
    arr[..., :3] *= arr[..., 3:4]
    cv.a = arr
    G.save("au_fringe", cv.result())


def au_blocks():
    n = T * 3
    # mattone morbido: cuscino arrotondato terracotta con ciuffi di muschio dorato
    cv = Cv(T, T, 3)
    tex = 0.84 + 0.30 * fbm(n, n, beta=1.6, seed=151) + 0.14 * (fbm(n, n, beta=0.7, seed=152) - 0.5)
    cv.add(cv.rrect(3, 3, T - 3, T - 3, 26), (200, 108, 62), bevel=17, spec=0.16, shin=22, tex=tex)
    cv.add(cv.capsule(16, T * 0.52, T - 16, T * 0.52, 1.5), (120, 58, 36), bevel=1.5, spec=0.04)
    for (x, y, rx, ry) in ((24, 10, 13, 5), (60, 8, 16, 5), (80, 14, 9, 4)):
        cv.add(cv.ell(x, y, rx, ry) & cv.rrect(3, 3, T - 3, T - 3, 26), (176, 168, 60), bevel=4, spec=0.1)
    G.save("au_brick", cv.result())

    def pumpkin(used):
        cv = Cv(T, T, 3)
        base = (238, 130, 36) if not used else (112, 72, 48)
        mid = (214, 104, 26) if not used else (96, 62, 42)
        dark = (176, 78, 20) if not used else (78, 50, 36)
        tx = 0.9 + 0.2 * fbm(n, n, beta=1.4, seed=161)
        cv.add(cv.capsule(50, 22, 46, 9, 5), (94, 112, 40) if not used else (70, 62, 42), bevel=5, spec=0.2)
        for cx, rx, c in ((17, 16, dark), (79, 16, dark), (31, 22, mid), (65, 22, mid), (48, 24, base)):
            cv.add(cv.ell(cx, 58, rx, 34), c, ao=0.25, bevel=15, spec=0.38 if not used else 0.12, shin=20, tex=tx)
        leaf = cv.poly([(50, 20), (66, 8), (80, 14), (66, 20)])
        cv.add(leaf, (104, 146, 44) if not used else (76, 80, 44), bevel=3, spec=0.2)
        st = cv.poly(G.star_pts(48, 60, 20, 8.5))
        if used:
            cv.add(st, (40, 26, 18), ao=0.3, bevel=3, spec=0.0)
        else:
            cv.glow(st, (255, 214, 70), radius=6, strength=0.7)
            cv.add(st, (255, 222, 80), ao=0.5, bevel=5, spec=0.8, shin=14)
        return cv.result()
    G.save("au_bonus", pumpkin(False))
    G.save("au_used", pumpkin(True))

    cv = Cv(T, T, 3)
    tex = 0.76 + 0.40 * fbm(n, n, beta=1.9, seed=171) + 0.30 * (fbm(n, n, beta=0.5, seed=172) - 0.5)
    r = np.random.default_rng(173)
    im = Image.new("L", (n, n), 0)
    d = ImageDraw.Draw(im)
    for _ in range(3):
        x, y = r.uniform(14, T - 14) * 3, r.uniform(14, T - 14) * 3
        pts = [(x, y)]
        ang = r.uniform(0, 6.28)
        for _ in range(8):
            ang += r.uniform(-0.7, 0.7)
            x, y = x + math.cos(ang) * 14, y + math.sin(ang) * 14
            pts.append((x, y))
        d.line(pts, fill=255, width=3)
    crack = ndi.gaussian_filter(np.array(im).astype(float) / 255.0, 1.2)
    tex = tex * (1 - 0.55 * crack)
    cv.add(cv.rrect(1.5, 1.5, T - 1.5, T - 1.5, 30), (146, 134, 122), bevel=16, spec=0.14, tex=tex)
    for (x, y, rx, ry, c) in ((26, 12, 17, 6, (200, 130, 40)), (66, 9, 20, 6, (150, 160, 56)), (88, 22, 8, 7, (190, 100, 40))):
        cv.add(cv.ell(x, y, rx, ry) & cv.rrect(1.5, 1.5, T - 1.5, T - 1.5, 30), c, bevel=4, spec=0.08)
    G.save("au_stone", cv.result())


# ------------------------------------------------------------------ 2D: nemici
def leaf_feather(bx, by, ang, L, W):
    pts = [(0, 0), (0.28, -1.0), (0.72, -0.85), (1.0, 0), (0.72, 0.85), (0.28, 1.0)]
    ca, sa = math.cos(ang), math.sin(ang)
    return [(bx + (px * L) * ca - (py * W) * sa, by + (px * L) * sa + (py * W) * ca) for px, py in pts]


def hog(step):
    """Riccio spinoso, rivolto a sinistra: due strati di aculei a due toni, orecchio, guancia, zampe con piedini."""
    cv = Cv(128, 88, 3)
    for i, bx in enumerate((46, 64, 90, 108)):
        dx = 5 if (i + step) % 2 == 0 else -5
        cv.add(cv.capsule(bx, 66, bx + dx, 80, 3.4), (200, 160, 120), ao=0.3, bevel=3.4, spec=0.2)
        cv.add(cv.ell(bx + dx - 2, 82, 6.5, 3.2), (150, 110, 80), ao=0.2, bevel=3, spec=0.2)
    r = np.random.default_rng(181)
    for layer, (n, lo, hi, cols, off) in enumerate((
            (52, 22, 30, [(58, 38, 26), (74, 48, 32), (50, 32, 22)], 0),
            (46, 15, 22, [(92, 62, 40), (110, 76, 48), (84, 56, 36)], 7))):
        for k in range(n):
            a = math.radians(188 + k * (164 / (n - 1.0)) + r.uniform(-2, 2))
            rim = (72 + (43 - off * 0.5) * math.cos(a), 56 + (27 - off * 0.4) * math.sin(a))
            dirx, diry = math.cos(a), math.sin(a) * 0.85
            ln = math.hypot(dirx, diry)
            dirx, diry = dirx / ln, diry / ln
            L = r.uniform(lo, hi)
            px, py = -diry * 3.0, dirx * 3.0
            tip = (rim[0] + dirx * L, rim[1] + diry * L)
            base = [(rim[0] - dirx * 8 + px, rim[1] - diry * 8 + py), (rim[0] - dirx * 8 - px, rim[1] - diry * 8 - py), tip]
            cv.add(cv.poly(base), cols[k % 3], ao=0.25, bevel=2.6, spec=0.25)
            tip2 = [(tip[0] - dirx * 10 + px * 0.45, tip[1] - diry * 10 + py * 0.45), (tip[0] - dirx * 10 - px * 0.45, tip[1] - diry * 10 - py * 0.45), tip]
            cv.add(cv.poly(tip2), (196, 156, 104) if layer == 0 else (226, 192, 138), bevel=1.8, spec=0.4)
    tex = 0.9 + 0.2 * fbm(cv.h, cv.w, beta=1.3, seed=182)
    cv.add(cv.ell(72, 60, 42, 24), (110, 76, 50), ao=0.4, bevel=22, spec=0.2, tex=tex)
    cv.add(cv.ell(70, 74, 38, 9), (226, 188, 146), ao=0.3, bevel=8, spec=0.2)               # pancia chiara
    cv.add(cv.ell(26, 64, 21, 17), (236, 204, 164), ao=0.4, bevel=14, spec=0.25)
    cv.add(cv.ell(9, 67, 10, 8), (244, 216, 180), bevel=8, spec=0.3)
    cv.add(cv.ell(2.5, 68, 3.9, 3.5), (22, 16, 14), bevel=3, spec=0.9)
    cv.add(cv.ell(1.6, 66.8, 1.2, 1.0), (200, 190, 190), bevel=1, spec=0.0)
    cv.add(cv.ell(33, 50, 6.5, 8), (232, 150, 140), ao=0.3, bevel=5, spec=0.2)              # orecchio
    cv.add(cv.ell(33, 51, 3.4, 5), (246, 190, 176), bevel=3, spec=0.2)
    cv.add(cv.ell(19, 72, 5.5, 3.8), (240, 150, 140), bevel=3, spec=0.15)                   # guancia
    eye = cv.ell(24, 59, 4.8, 5.4)
    cv.add(eye, (22, 16, 14), bevel=4, spec=0.9)
    cv.add(cv.ell(22.6, 57.2, 1.5, 1.7), (255, 255, 255), bevel=1, spec=0.0)
    cv.add(cv.capsule(16, 50, 30, 55, 1.7), (64, 40, 26), bevel=1.7, spec=0.1)          # sopracciglio arrabbiato
    for wy, ty in ((68, 62), (70, 70), (72, 77)):                                       # baffi
        cv.add(cv.capsule(9, wy, 0.5, ty, 0.6), (246, 240, 232), bevel=0.6, spec=0.1)
    return cv.result()


def crow(frame):
    """Corvo con le ali alzate (0) o abbassate (1), rivolto a sinistra: remiganti e coda a penne, becco aperto."""
    cv = Cv(120, 96, 3)
    body, light = (36, 38, 70), (86, 94, 150)
    up = frame == 0
    sh = (72, 50 if up else 54)
    for i in range(9):                                            # ala: penne a ventaglio
        k = i / 8.0
        ang = math.radians((-100 + 78 * k) if up else (100 - 78 * k))
        L = 46 - 6 * abs(k - 0.35)
        col = tuple(int(c * (0.85 + 0.25 * k)) for c in (44, 48, 92))
        cv.add(cv.poly(leaf_feather(sh[0] + 4 * k, sh[1], ang, L, 7.5)), col, ao=0.3, bevel=3, spec=0.5, shin=20)
    for i in range(6):                                            # copritrici
        k = i / 5.0
        ang = math.radians((-92 + 62 * k) if up else (92 - 62 * k))
        cv.add(cv.poly(leaf_feather(sh[0] + 2 * k, sh[1], ang, 24, 7)), (60, 66, 118), ao=0.3, bevel=3, spec=0.5)
    for i in range(5):                                            # coda
        ang = math.radians(8 + i * 11)
        cv.add(cv.poly(leaf_feather(92, 56, ang, 28, 5.2)), (28, 30, 58), ao=0.3, bevel=2.6, spec=0.5)
    cv.add(cv.capsule(60, 70, 58, 82, 2.0), (214, 140, 44), bevel=2, spec=0.3)
    cv.add(cv.capsule(73, 70, 73, 82, 2.0), (214, 140, 44), bevel=2, spec=0.3)
    cv.add(cv.poly([(54, 82), (46, 86), (58, 85)]), (200, 128, 40), bevel=1.5, spec=0.2)
    cv.add(cv.poly([(70, 82), (62, 86), (74, 85)]), (200, 128, 40), bevel=1.5, spec=0.2)
    cv.add(cv.ell(66, 54, 30, 20), body, ao=0.4, bevel=18, spec=0.8, shin=30)
    cv.add(cv.ell(52, 60, 15, 11), (76, 82, 132), ao=0.2, bevel=10, spec=0.8, shin=30)       # petto chiaro
    cv.add(cv.ell(38, 43, 14, 13), (30, 34, 64), ao=0.4, bevel=13, spec=0.8, shin=30)
    cv.add(cv.poly([(26, 38), (4, 41), (26, 45)]), (250, 178, 44), bevel=4, spec=0.5)       # becco superiore
    cv.add(cv.poly([(26, 46), (9, 50), (26, 51)]), (218, 124, 28), bevel=3, spec=0.4)       # becco inferiore
    cv.add(cv.poly([(7, 40.5), (3.6, 47.5), (9.5, 43.5)]), (250, 178, 44), bevel=1.5, spec=0.5)    # punta uncinata
    cv.add(cv.ell(16, 48.6, 5.5, 1.5), (238, 120, 132), bevel=1.2, spec=0.3)                # lingua
    cv.add(cv.capsule(40, 31, 44, 25, 1.2), (60, 66, 128), bevel=1.2, spec=0.3)            # ciuffo sul capo
    cv.add(cv.capsule(36, 32, 38, 24, 1.2), (60, 66, 128), bevel=1.2, spec=0.3)
    cv.add(cv.ell(21, 41.5, 1.1, 0.9), (110, 60, 16), bevel=1, spec=0.0)
    eye = cv.ell(33, 39, 4.8, 5.0)
    cv.add(eye, (252, 214, 64), bevel=4, spec=0.7)
    cv.add(cv.ell(32, 39.4, 2.2, 2.7), (14, 10, 10), bevel=1.5, spec=0.1)
    cv.add(cv.ell(31.2, 38.2, 0.9, 0.9), (255, 255, 255), bevel=0.5, spec=0.0)
    cv.add(cv.capsule(25, 30, 41, 35, 1.8), (16, 16, 32), bevel=1.7, spec=0.1)
    return cv.result()


def acorn(frame):
    cv = Cv(96, 106, 3)
    foot_dy = 0 if frame == 0 else -7
    for fx in (34, 62):
        cv.add(cv.ell(fx + (4 if fx > 48 else -4), 96 + foot_dy, 8.5, 4.5), (74, 48, 30), ao=0.3, bevel=4, spec=0.3)
    cv.add(cv.ell(48, 70, 28, 31), (208, 150, 68), ao=0.4, bevel=24, spec=0.55, shin=24, tex=0.92 + 0.16 * fbm(cv.h, cv.w, beta=1.4, seed=191))
    cv.add(cv.ell(14, 66, 6, 8), (128, 82, 42), ao=0.3, bevel=5, spec=0.3)                      # braccini
    cv.add(cv.ell(82, 66, 6, 8), (128, 82, 42), ao=0.3, bevel=5, spec=0.3)
    cap = cv.ell(48, 46, 36, 25) & cv.rrect(0, 0, 96, 54, 0)
    cv.add(cap, (112, 70, 38), ao=0.5, bevel=18, spec=0.25)
    r = np.random.default_rng(193)
    for row, y in enumerate((24, 32, 40, 48)):                    # cappello a scaglie
        for x in range(10 + (row % 2) * 5, 92, 10):
            m = cv.ell(x, y, 6.2, 4.6) & cap
            tone = r.uniform(0.85, 1.18)
            cv.add(m, tuple(min(255, int(c * tone)) for c in (146, 98, 56)), ao=0.3, bevel=3.5, spec=0.3)
    cv.add(cv.ell(48, 53, 36, 4.5), (70, 44, 24), ao=0.4, bevel=4, spec=0.25)                 # bordo
    for i in range(11):                                           # fila di perline sul bordo del cappello
        t = i / 10.0
        cv.add(cv.ell(13 + 70 * t, 54 + 2.2 * math.sin(math.pi * t), 3.3, 3.0), (176, 124, 70), ao=0.2, bevel=3, spec=0.5)
    cv.add(cv.capsule(48, 26, 53, 11, 3.8), (88, 58, 34), bevel=3.8, spec=0.3)
    cv.add(cv.ell(54, 10, 4.2, 3.2), (130, 92, 50), bevel=3, spec=0.3)
    cv.add(cv.poly([(55, 11), (68, 4), (62, 13)]), (242, 202, 72), bevel=2, spec=0.4)                # foglia sul picciolo
    for ex in (37, 60):
        cv.add(cv.ell(ex, 63, 7.2, 8.4), (250, 246, 236), bevel=5, spec=0.6)
        cv.add(cv.ell(ex + (1.6 if ex < 48 else -1.6), 64.5, 3.4, 4.4), (22, 14, 10), bevel=2, spec=0.4)
        cv.add(cv.ell(ex + (0.6 if ex < 48 else -2.6), 62.0, 1.1, 1.3), (255, 255, 255), bevel=0.5, spec=0.0)
    cv.add(cv.capsule(28, 51, 44, 58, 2.3), (62, 38, 22), bevel=2.0, spec=0.1)                 # sopracciglia
    cv.add(cv.capsule(68, 51, 52, 58, 2.3), (62, 38, 22), bevel=2.0, spec=0.1)
    cv.add(cv.ell(28, 75, 5.5, 3.6), (236, 120, 110), bevel=3, spec=0.15)                      # guance
    cv.add(cv.ell(68, 75, 5.5, 3.6), (236, 120, 110), bevel=3, spec=0.15)
    cv.add(cv.ell(48, 80, 7, 3.4), (96, 30, 30), bevel=2.5, spec=0.3)                          # bocca
    cv.add(cv.poly([(42, 78.5), (46, 78.5), (44, 85)]), (252, 250, 244), bevel=1.5, spec=0.5)         # zanne
    cv.add(cv.poly([(50, 78.5), (54, 78.5), (52, 85)]), (252, 250, 244), bevel=1.5, spec=0.5)
    return cv.result()


def au_enemies():
    for i in (0, 1):
        G.save("hog_%d" % i, hog(i))
        G.save("crow_%d" % i, crow(i))
        G.save("acorn_%d" % i, acorn(i))
    a0 = acorn(0)
    im = Image.fromarray((a0 * 255).astype(np.uint8), "RGBA").resize((96, 32), Image.LANCZOS)
    flat = Image.new("RGBA", (96, 106), (0, 0, 0, 0))
    flat.paste(im, (0, 74))
    G.save("acorn_flat", np.array(flat).astype(float) / 255.0)


# ------------------------------------------------------------------ 3D: texture del bosco d'autunno
def a3_ground():
    n = 512
    g = capture(G3, G3.gen_grass)
    f = capture(G3, G3.gen_forest_floor)
    p = fbm(n, n, beta=2.4, seed=401)
    q = fbm(n, n, beta=2.0, seed=402)
    gold = smooth((p - 0.34) * 6.0)
    red = smooth((q - 0.68) * 9.0) * 0.8
    g = autumnize(g[..., :3], gold, red, 0.95)
    g = scatter_leaves(g, 150, 403, size=(10, 18))
    f = autumnize(f[..., :3], np.ones((n, n)), smooth((q - 0.55) * 6.0), 1.0)
    f = scatter_leaves(f, 520, 404, size=(9, 18))
    mg, mf = g.mean((0, 1)), f.mean((0, 1))
    m = (mg + mf) / 2
    print("media comune", m)
    G3.save("r3a_grass", np.clip(g * (m / mg), 0, 1))
    G3.save("r3a_forest", np.clip(f * (m / mf), 0, 1))


def a3_tuft():
    t = capture(G3, G3.gen_tuft)
    h, w = t.shape[:2]
    gold = 0.55 + 0.45 * np.sin(np.arange(w)[None, :] * 0.11 + np.arange(h)[:, None] * 0.04)
    red = np.clip(0.5 + 0.5 * np.sin(np.arange(w)[None, :] * 0.05 + 2.0), 0, 1) * 0.35
    rgb = autumnize(t[..., :3], np.clip(gold, 0, 1), red, 0.92)
    G3.save("r3a_tuft", np.dstack([rgb, t[..., 3]]))


def a3_flowers():
    kinds = [((222, 90, 20), (255, 210, 60)), ((240, 180, 30), (130, 70, 10)), ((150, 30, 70), (255, 220, 90)),
             ((170, 64, 210), (255, 236, 130)), ((250, 250, 240), (250, 200, 40)), ((235, 60, 80), (255, 220, 100)),
             ((255, 140, 30), (120, 60, 10)), ((120, 60, 200), (255, 240, 150)), ((255, 220, 60), (150, 80, 10))]
    G3.gen_flowers("r3a_flowers", kinds)


def a3_sky():
    c = G3.col
    G3.gen_sky("r3a_sky", (c((58, 106, 176)), c((160, 190, 214)), c((252, 214, 164))), (1.0, 0.72, 0.38))


def a3_mountains():
    a = capture(G3, G3.gen_mountains)
    h, w = a.shape[:2]
    p = fbm(h, w, beta=2.2, seed=411)
    q = fbm(h, w, beta=1.6, seed=412)
    rgb = autumnize(a[..., :3], smooth((p - 0.3) * 6.0), smooth((q - 0.55) * 8.0), 1.0)
    rgb = np.clip(rgb * np.array([1.05, 0.93, 0.80]), 0, 1)
    G3.save("r3a_mountains", np.dstack([rgb, a[..., 3]]))


def leafy_card(S, W, H, region, count, seed, size, name, trunk=None):
    """Scheda di fogliame chiaro (si tinge in gioco): foglie sovrapposte dentro una regione, ombre interne."""
    ss = 2
    r = np.random.default_rng(seed)
    im = Image.new("RGBA", (W * ss, H * ss), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    if trunk:
        trunk(d, ss)
    pts = []
    while len(pts) < count:
        x, y = r.uniform(0, W), r.uniform(0, H)
        if region(x, y, r):
            pts.append((x, y))
    pts.sort(key=lambda p: p[1])
    for x, y in pts:
        ln = r.uniform(*size)
        wd = ln * r.uniform(0.5, 0.7)
        up = 1.0 - y / H
        k = r.uniform(0.82, 1.12) * (0.78 + 0.3 * up)
        c = (min(255, int(255 * k)), min(255, int(222 * k)), min(255, int(104 * k)), 255)
        d.polygon(leaf_poly(x * ss, y * ss, ln * ss, wd * ss, r.uniform(0, 6.28)), fill=c)
    arr = np.asarray(im).astype(float) / 255.0
    a = arr[..., 3]
    dens = ndi.gaussian_filter(a, 7 * ss)
    arr[..., :3] *= (0.76 + 0.4 * (1 - np.clip(dens * 1.2, 0, 1)))[..., None]
    G3.finish_card(Image.fromarray((np.clip(arr, 0, 1) * 255).astype(np.uint8), "RGBA"), name, ss)


def a3_crown():
    leafy_card(256, 256, 256, lambda x, y, r: ((x - 128) / 118) ** 2 + ((y - 128) / 112) ** 2 < 1.0 and r.random() < 0.95 - 0.3 * (((x - 128) / 118) ** 2 + ((y - 128) / 112) ** 2),
               700, 421, (26, 42), "r3a_crown")


def a3_tree():
    def trunk(d, ss):
        d.polygon([(118 * ss, 512 * ss), (138 * ss, 512 * ss), (133 * ss, 270 * ss), (123 * ss, 270 * ss)], fill=(96, 74, 54, 255))
        for (x0, y0, x1, y1) in ((128, 330, 72, 230), (128, 310, 190, 220), (128, 280, 128, 150)):
            d.line([(x0 * ss, y0 * ss), (x1 * ss, y1 * ss)], fill=(90, 68, 50, 255), width=5 * ss)
    leafy_card(256, 256, 512, lambda x, y, r: ((x - 128) / 122) ** 2 + ((y - 190) / 170) ** 2 < 1.0 and y < 340 and r.random() < 0.97 - 0.25 * (((x - 128) / 122) ** 2 + ((y - 190) / 170) ** 2),
               1700, 431, (24, 40), "r3a_tree", trunk)


def a3_foliage():
    """Mosaico di foglie senza trasparenza, ripetibile: serve alle chiome tridimensionali (si tinge in gioco)."""
    W = 256
    ss = 2
    r = np.random.default_rng(477)
    im = Image.new("RGB", (W * ss, W * ss), (70, 58, 24))
    d = ImageDraw.Draw(im)
    pts = [(r.uniform(0, W), r.uniform(0, W)) for _ in range(1500)]
    for x, y in pts:
        ln = r.uniform(17, 30)
        wd = ln * r.uniform(0.5, 0.68)
        k = r.uniform(0.62, 1.12)
        c = (min(255, int(255 * k)), min(255, int(226 * k)), min(255, int(120 * k)))
        a = r.uniform(0, 6.28)
        for ox in (-W, 0, W):
            for oy in (-W, 0, W):
                if -ln <= x + ox <= W + ln and -ln <= y + oy <= W + ln:
                    pl = leaf_poly((x + ox) * ss, (y + oy) * ss, ln * ss, wd * ss, a)
                    d.polygon(pl, fill=c)
                    d.line([((x + ox) * ss - math.cos(a) * ln * ss * 0.4, (y + oy) * ss - math.sin(a) * ln * ss * 0.4),
                            ((x + ox) * ss + math.cos(a) * ln * ss * 0.45, (y + oy) * ss + math.sin(a) * ln * ss * 0.45)],
                           fill=(int(c[0] * 0.62), int(c[1] * 0.62), int(c[2] * 0.6)), width=ss)
    out = np.asarray(im.resize((W, W), Image.LANCZOS)).astype(float) / 255.0
    G3.save("r3a_foliage", np.dstack([out, np.ones((W, W))]))


GENS = {
    "au_sky": au_sky, "au_mountains": au_mountains, "au_hills": au_hills, "au_ground": au_ground, "au_blocks": au_blocks,
    "au_enemies": au_enemies, "a3_ground": a3_ground, "a3_tuft": a3_tuft, "a3_flowers": a3_flowers, "a3_sky": a3_sky,
    "a3_mountains": a3_mountains, "a3_crown": a3_crown, "a3_tree": a3_tree, "a3_foliage": a3_foliage,
}

if __name__ == "__main__":
    only = sys.argv[1:] or list(GENS)
    for k in only:
        GENS[k]()
        print("ok", k, flush=True)
