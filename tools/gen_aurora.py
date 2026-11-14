#!/usr/bin/env python3
"""Immagini dei livelli con l'aurora boreale: notte polare, neve e ghiaccio (2D e 3D).

Uso:  python3 tools/gen_aurora.py [nome ...]      (nessun nome = tutto)
Richiede numpy, scipy, Pillow e gli script gen_assets.py / gen_assets3d.py / gen_autumn.py accanto a questo file.
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

SNOW = np.array([238, 246, 255]) / 255.0
SNOW_SH = np.array([150, 178, 222]) / 255.0


def col(c):
    return np.array(c, dtype=float) / 255.0


def sky_array(w, h, seed, aurora_amp=1.0, star_n=900, horizon_glow=True):
    """Cielo notturno che si ripete in orizzontale: sfumatura, stelle e nastri d'aurora."""
    y = np.linspace(0, 1, h)[:, None]
    x = np.linspace(0, 1, w, endpoint=False)[None, :]
    zen, mid, hor = col((4, 8, 30)), col((14, 34, 78)), col((44, 92, 128))
    t = y ** 0.9
    grad = np.where(t[..., None] < 0.55, lerp(zen, mid, (t / 0.55)[..., None]), lerp(mid, hor, ((t - 0.55) / 0.45)[..., None]))
    sky = np.broadcast_to(grad, (h, w, 3)).copy()
    r = np.random.default_rng(seed)
    # stelle: punti di luminosità varia, qualcuna più grande con un bagliore
    st = np.zeros((h, w))
    for _ in range(star_n):
        px, py = r.integers(0, w), int(r.uniform(0, 0.78) ** 1.2 * h)
        b = r.uniform(0.25, 1.0) ** 2
        st[py, px] = max(st[py, px], b)
    big = np.zeros((h, w))
    for _ in range(star_n // 12):
        px, py = r.integers(2, w - 2), int(r.uniform(0, 0.6) * h)
        big[py, px] = r.uniform(0.7, 1.0)
    st = np.clip(st + ndi.gaussian_filter(big, 1.1) * 14 + ndi.gaussian_filter(big, 3.0) * 20, 0, 1)
    sky += st[..., None] * col((235, 240, 255)) * (1 - 0.5 * y[..., None])
    # nastri d'aurora: bordo basso netto, svanisce verso l'alto, striature verticali
    ys = y * np.ones((1, w))
    for k, (yc, amp, thick, c_lo, c_hi, ph) in enumerate((
            (0.50, 0.09, 0.19, (60, 255, 150), (150, 90, 240), 0.3),
            (0.36, 0.08, 0.15, (90, 255, 210), (200, 90, 220), 2.1),
            (0.60, 0.06, 0.10, (50, 240, 120), (80, 200, 230), 4.0))):
        ctr = yc + amp * np.sin(2 * np.pi * (1 * x + ph / 6.28)) + 0.5 * amp * np.sin(2 * np.pi * (2 * x + ph)) + 0.25 * amp * np.sin(2 * np.pi * (5 * x + 2 * ph))
        u = ys - ctr                                            # positivo sotto il bordo basso del nastro
        prof = np.exp(-np.clip(-u, 0, None) / thick) * (1 - smooth(u / 0.02))
        stripes = 0.3 + 0.7 * np.sin(2 * np.pi * (37 + 6 * k) * x + 3 * np.sin(2 * np.pi * 3 * x + ph)) ** 2
        mod = 0.55 + 0.45 * np.sin(2 * np.pi * (2 + k) * x + ph * 3) ** 2
        inten = np.clip(prof * stripes * mod, 0, 1)
        tcol = np.clip(-u / (thick * 1.6), 0, 1)[..., None]
        colr = lerp(col(c_lo), col(c_hi), tcol)
        sky += colr * inten[..., None] * 0.62 * aurora_amp
        sky += colr * ndi.gaussian_filter(inten, 8)[..., None] * 0.28 * aurora_amp
    if horizon_glow:
        hz = smooth((y - 0.8) * 5)
        sky = sky * (1 - 0.4 * hz[..., None]) + col((70, 140, 170)) * 0.4 * hz[..., None]
    return np.clip(sky, 0, 1)


def ar_sky():
    G.save("ar_sky", sky_array(1536, 768, 701))


def night_lum(rgba, dark=(10, 22, 52), mid=(70, 110, 160), light=(226, 240, 255)):
    """Ricolora un'immagine in toni di blu notte / ghiaccio in base alla luminosità."""
    rgb = rgba[..., :3]
    lum = np.clip(rgb @ np.array([0.3, 0.59, 0.11]), 0, 1)
    lum = np.clip((lum - 0.15) / 0.8, 0, 1)
    out = np.where(lum[..., None] < 0.55, lerp(col(dark), col(mid), (lum / 0.55)[..., None]), lerp(col(mid), col(light), ((lum - 0.55) / 0.45)[..., None]))
    return np.dstack([np.clip(out, 0, 1), rgba[..., 3]])


def ar_mountains():
    a = A.capture(G, G.gen_mountains)
    G.save("ar_mountains", night_lum(a))


def pine(d, x, base, h, w, ss, body, snow, tiers=5):
    """Abete stilizzato: tronco e livelli triangolari con la neve sul dorso."""
    d.rectangle([(x - w * 0.06) * ss, (base - h * 0.12) * ss, (x + w * 0.06) * ss, (base + 2) * ss], fill=(26, 22, 28, 255))
    for i in range(tiers):
        f0, f1 = i / tiers, (i + 1) / tiers
        ytop = base - h * (0.12 + 0.88 * f1) - h * 0.04
        ybot = base - h * (0.12 + 0.88 * f0) + h * 0.05
        wd = w * (1.0 - 0.8 * f0)
        d.polygon([(x * ss, ytop * ss), ((x + wd / 2) * ss, ybot * ss), ((x - wd / 2) * ss, ybot * ss)], fill=body)
        d.polygon([(x * ss, ytop * ss), ((x + wd * 0.38) * ss, (ytop + (ybot - ytop) * 0.72) * ss), ((x + wd * 0.1) * ss, (ytop + (ybot - ytop) * 0.62) * ss),
                   ((x - wd * 0.12) * ss, (ytop + (ybot - ytop) * 0.78) * ss), ((x - wd * 0.38) * ss, (ytop + (ybot - ytop) * 0.7) * ss)], fill=snow)


def ar_hills():
    """Pineta innevata in tre piani: lontana azzurrina, vicina scura con neve sui rami."""
    h, w, ss = 560, 2048, 2
    im = Image.new("RGBA", (w * ss, h * ss), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    r = np.random.default_rng(711)
    layers = [(60, 62, 46, 24, (46, 84, 120, 255), (150, 190, 222, 255), 46, 0.55),
              (150, 100, 78, 34, (22, 58, 84, 255), (190, 218, 238, 255), 34, 0.3),
              (260, 160, 120, 52, (10, 34, 52, 255), (226, 238, 252, 255), 26, 0.0)]
    for y0, amp, th, tw, body, snow, count, _ in layers:
        ridge = y0 + amp * fbm1(w, beta=2.4, seed=int(y0))
        pts = [(x * ss, ridge[x] * ss) for x in range(0, w, 8)]
        d.polygon(pts + [(w * ss, h * ss), (0, h * ss)], fill=body)
        for _ in range(count * 3):
            x = r.uniform(0, w)
            hh = th * r.uniform(0.6, 1.25) * 1.6
            for off in (-w, 0, w):
                if -tw <= x + off <= w + tw:
                    pine(d, x + off, ridge[int(x) % w] + 8, hh, tw * r.uniform(0.8, 1.2), ss, body, snow)
    arr = np.asarray(im.resize((w, h), Image.LANCZOS)).astype(float) / 255.0
    grain = 0.9 + 0.2 * fbm(h, w, beta=0.9, seed=719)
    arr[..., :3] = np.clip(arr[..., :3] * grain[..., None], 0, 1)
    G.save("ar_hills", arr)


def ar_ground():
    s = 2
    n = T * s
    dirt = G.dirt_tex(s) * np.array([0.72, 0.84, 1.12]) * 0.9
    r = np.random.default_rng(744)
    edge = 32 + 12 * (fbm1(n, beta=2.0, seed=47) - 0.5)
    yy = np.arange(n)[:, None].astype(float)
    yb = edge[None, :] * s
    fine = fbm(n, n, beta=0.8, seed=747)
    drift = fbm(n, n, beta=2.2, seed=748)
    snow = lerp(SNOW_SH, SNOW, np.clip(0.55 + 0.9 * (drift - 0.5) + 0.3 * (fine - 0.5), 0, 1)[..., None])
    glint = (r.random((n, n)) > 0.9985).astype(float)
    snow = np.clip(snow + ndi.gaussian_filter(glint, 0.8)[..., None] * 3.0, 0, 1)
    light = 1.0 + 0.12 * np.exp(-yy / (7.0 * s))
    snow = np.clip(snow * light[..., None], 0, 1)
    # ghiaccio nel terreno: venature chiare
    im = Image.new("L", (n, n), 0)
    dr = ImageDraw.Draw(im)
    for _ in range(5):
        x, y = r.uniform(0, n), r.uniform(edge.max() * s + 20, n)
        pts = [(x, y)]
        a = r.uniform(0, 6.28)
        for _ in range(6):
            a += r.uniform(-0.8, 0.8)
            x, y = x + math.cos(a) * 18, y + math.sin(a) * 18
            pts.append((x, y))
        dr.line(pts, fill=255, width=3)
    vein = ndi.gaussian_filter(np.array(im).astype(float) / 255.0, 1.3)
    dirt = np.clip(dirt + vein[..., None] * np.array([0.25, 0.4, 0.55]), 0, 1)
    a = np.clip((yb - yy) / 1.6, 0, 1)
    shadow = 1 - 0.5 * np.exp(-np.clip(yy - yb, 0, None) / (5.0 * s)) * (1 - a)
    rgb = dirt * shadow[..., None]
    rgb = snow * a[..., None] + rgb * (1 - a[..., None])
    top = down(rgb, s)
    dt = down(dirt, s)
    G.save("ar_grass", top)
    G.save("ar_dirt", dt)
    for kind, nm in (("l", "l"), ("r", "r")):
        G.save("ar_grass_" + nm, A.side_variant(top, kind, True))
        G.save("ar_dirt_" + nm, A.side_variant(dt, kind, False))
    # frangia: cumuli di neve sopra il bordo, con qualche ciuffo gelato
    fw, fh = T, 28
    cv = Cv(fw, fh, 3)
    for _ in range(26):
        x = r.uniform(0, fw)
        rx, ry = r.uniform(4, 11), r.uniform(3, 8)
        for off in (-fw, 0, fw):
            m = cv.ell(x + off, fh - 1, rx, ry)
            cv.add(m, (232, 243, 255), bevel=4, spec=0.2)
    for _ in range(14):
        x = r.uniform(0, fw)
        hgt = r.uniform(8, 22)
        lean = r.uniform(-4, 4)
        for off in (-fw, 0, fw):
            cv.add(cv.poly([(x - 2 + off, fh), (x + 2 + off, fh), (x + lean + off, fh - hgt)]), (170, 214, 232), bevel=1.5, spec=0.5)
    G.save("ar_fringe", cv.result())


def ar_blocks():
    n = T * 3
    cv = Cv(T, T, 3)
    tex = 0.88 + 0.22 * fbm(n, n, beta=1.6, seed=751)
    cv.add(cv.rrect(3, 3, T - 3, T - 3, 26), (126, 196, 240), bevel=17, spec=0.55, shin=30, tex=tex)
    cv.add(cv.capsule(16, T * 0.52, T - 16, T * 0.52, 1.4), (70, 130, 190), bevel=1.4, spec=0.1)
    cv.add(cv.capsule(T * 0.5, 16, T * 0.5, T * 0.5 - 6, 1.2), (70, 130, 190), bevel=1.2, spec=0.1)
    cv.add(cv.ell(30, 12, 22, 7) & cv.rrect(3, 3, T - 3, T - 3, 26), (244, 250, 255), bevel=4, spec=0.2)          # neve sul bordo alto
    cv.add(cv.ell(70, 11, 14, 6) & cv.rrect(3, 3, T - 3, T - 3, 26), (236, 246, 255), bevel=4, spec=0.2)
    cv.add(cv.capsule(22, 66, 40, 50, 2.2), (214, 240, 255), bevel=2, spec=0.7)                                       # riflessi
    G.save("ar_brick", cv.result())

    def crystal(used):
        cv = Cv(T, T, 3)
        base = (60, 84, 168) if not used else (88, 98, 124)
        cv.add(cv.rrect(3, 3, T - 3, T - 3, 26), base, bevel=17, spec=0.3 if not used else 0.1, shin=24, tex=0.9 + 0.2 * fbm(n, n, beta=1.5, seed=752))
        st = cv.poly(G.star_pts(48, 50, 24, 10))
        if used:
            cv.add(st, (40, 44, 64), ao=0.3, bevel=3, spec=0.0)
        else:
            cv.glow(st, (120, 255, 235), radius=9, strength=0.95)
            cv.add(st, (150, 255, 240), ao=0.5, bevel=6, spec=0.9, shin=16)
            cv.add(cv.ell(48, 50, 6, 6), (255, 255, 255), bevel=3, spec=0.5)
        cv.add(cv.ell(30, 12, 18, 6) & cv.rrect(3, 3, T - 3, T - 3, 26), (236, 246, 255), bevel=4, spec=0.2)
        return cv.result()
    G.save("ar_bonus", crystal(False))
    G.save("ar_used", crystal(True))

    cv = Cv(T, T, 3)
    tex = 0.74 + 0.40 * fbm(n, n, beta=1.9, seed=761) + 0.25 * (fbm(n, n, beta=0.5, seed=762) - 0.5)
    cv.add(cv.rrect(1.5, 1.5, T - 1.5, T - 1.5, 30), (116, 128, 150), bevel=16, spec=0.2, tex=tex)
    for (x, y, rx, ry) in ((26, 12, 20, 8), (66, 9, 24, 8), (88, 24, 9, 9), (12, 44, 8, 7)):
        cv.add(cv.ell(x, y, rx, ry) & cv.rrect(1.5, 1.5, T - 1.5, T - 1.5, 30), (240, 248, 255), bevel=4, spec=0.2)
    cv.add(cv.capsule(60, 40, 76, 62, 1.2), (60, 70, 92), bevel=1.0, spec=0.0)
    G.save("ar_stone", cv.result())


# ------------------------------------------------------------------ 3D
def snow_ground():
    n = 512
    drift = fbm(n, n, beta=2.3, seed=801)
    fine = fbm(n, n, beta=0.8, seed=802)
    rip = np.sin((np.arange(n)[None, :] * 2 * np.pi * 4 / n) + 6.0 * drift) * 0.5 + 0.5
    t = np.clip(0.52 + 0.85 * (drift - 0.5) + 0.25 * (fine - 0.5) + 0.06 * rip, 0, 1)
    snow = lerp(SNOW_SH, SNOW, t[..., None])
    r = np.random.default_rng(803)
    glint = (r.random((n, n)) > 0.9975).astype(float)
    snow = np.clip(snow + ndi.gaussian_filter(glint, 0.7)[..., None] * 3.0, 0, 1)
    forest = lerp(col((84, 108, 150)), col((196, 214, 240)), np.clip(0.45 + 0.9 * (fbm(n, n, beta=2.1, seed=804) - 0.5) + 0.3 * (fine - 0.5), 0, 1)[..., None])
    im = Image.new("RGBA", (n * 2, n * 2), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    for _ in range(900):                                   # aghi di pino caduti sulla neve del sottobosco
        x, y = r.uniform(0, n), r.uniform(0, n)
        a = r.uniform(0, 6.28)
        ln = r.uniform(4, 9)
        c = (int(r.uniform(18, 46)), int(r.uniform(46, 78)), int(r.uniform(42, 70)), 255)
        for ox in (-n, 0, n):
            for oy in (-n, 0, n):
                if -10 <= x + ox <= n + 10 and -10 <= y + oy <= n + 10:
                    d.line([((x + ox) * 2, (y + oy) * 2), ((x + ox + math.cos(a) * ln) * 2, (y + oy + math.sin(a) * ln) * 2)], fill=c, width=2)
    lit = np.asarray(im.resize((n, n), Image.LANCZOS)).astype(float) / 255.0
    forest = forest * (1 - lit[..., 3:4]) + lit[..., :3] * lit[..., 3:4]
    m = (snow.mean((0, 1)) + forest.mean((0, 1))) / 2
    G3.save("r3b_grass", np.clip(snow * (m / snow.mean((0, 1))), 0, 1))
    G3.save("r3b_forest", np.clip(forest * (m / forest.mean((0, 1))), 0, 1))


def frost_tuft():
    t = A.capture(G3, G3.gen_tuft)
    rgb = night_lum(t, (34, 78, 90), (120, 180, 190), (240, 250, 255))[..., :3]
    G3.save("r3b_tuft", np.dstack([rgb, t[..., 3]]))


def ice_flowers():
    kinds = [((110, 230, 255), (255, 255, 255)), ((190, 150, 255), (255, 240, 200)), ((255, 150, 220), (255, 250, 220)),
             ((120, 255, 200), (255, 255, 255)), ((255, 255, 255), (130, 220, 255)), ((150, 190, 255), (255, 250, 200)),
             ((255, 200, 120), (130, 90, 40)), ((100, 255, 240), (255, 255, 255)), ((220, 170, 255), (255, 255, 220))]
    G3.gen_flowers("r3b_flowers", kinds, stem=(70, 130, 140, 255), leaf=(84, 150, 160, 255))


def snowify(rgba, seed, upper_bias=0.6, thr=0.46):
    h, w = rgba.shape[:2]
    n = fbm(h, w, beta=1.2, seed=seed)
    n2 = fbm(h, w, beta=0.7, seed=seed + 1)
    up = (1 - np.linspace(0, 1, h))[:, None] ** upper_bias
    m = np.clip((n + 0.35 * (n2 - 0.5) + 0.25 * up - thr) * 5.0, 0, 1) * 0.92
    lit = lerp(SNOW_SH, SNOW, np.clip(0.6 + 0.6 * (n2 - 0.5), 0, 1)[..., None])
    rgb = lerp(rgba[..., :3], lit, m[..., None])
    return np.dstack([np.clip(rgb, 0, 1), rgba[..., 3]])


def snow_fir():
    old = G3.needle_palette
    G3.needle_palette = lambda: (col((10, 36, 46)), col((22, 70, 74)), col((48, 116, 106)), col((130, 190, 180)))
    try:
        b = A.capture(G3, G3.gen_branch)
        f = A.capture(G3, G3.gen_firtree)
    finally:
        G3.needle_palette = old
    G3.save("r3b_branch", snowify(b, 811, 0.5, 0.44))
    G3.save("r3b_tree", snowify(f, 813, 0.2, 0.5))


def night_sky3():
    G3.save("r3b_sky", sky_array(1024, 512, 821, 1.0, 700))


def night_mountains3():
    a = A.capture(G3, G3.gen_mountains)
    G3.save("r3b_mountains", night_lum(a, (8, 18, 44), (64, 104, 156), (224, 238, 255)))


def aurora_curtain():
    """Tenda d'aurora per il cielo 3D: nastro con striature verticali, bordo basso netto e dissolvenza verso l'alto."""
    w, h = 512, 256
    y = np.linspace(0, 1, h)[:, None]
    x = np.linspace(0, 1, w, endpoint=False)[None, :]
    d = 0.78 - y                                         # y=0 in alto; bordo basso a 0.78
    ctr = 0.04 * np.sin(2 * np.pi * x * 2)
    d = d + ctr
    prof = smooth(d / 0.03 + 0.2) * np.exp(-np.clip(1 - y - 0.22 - ctr, 0, None) * 0) * np.clip(y / 0.78, 0, 1) ** 1.6
    prof = smooth(d / 0.04) * np.clip(1 - np.abs(y - 0.45) / 0.55, 0, 1) ** 1.3 * (y < 0.8)
    stripes = 0.5 + 0.5 * np.sin(2 * np.pi * (23 * x) + 2.5 * np.sin(2 * np.pi * 3 * x)) ** 2
    mod = 0.55 + 0.45 * np.sin(2 * np.pi * 3 * x + 0.7) ** 2
    inten = np.clip(prof * (0.45 + 0.8 * stripes) * mod, 0, 1)
    tcol = np.clip((0.78 - y) / 0.6, 0, 1)
    colr = lerp(col((70, 255, 160)), col((170, 100, 240)), tcol[..., None] ** 1.2)
    G3.save("r3b_aurora", np.dstack([np.clip(colr * (0.7 + 0.5 * inten[..., None]), 0, 1), np.clip(inten * 0.9, 0, 1)]))


def flake():
    n = 64
    y, x = np.mgrid[0:n, 0:n].astype(float)
    r = np.sqrt((x - n / 2 + 0.5) ** 2 + (y - n / 2 + 0.5) ** 2) / (n / 2)
    a = np.clip(1 - r, 0, 1) ** 1.6
    G3.save("r3b_flake", np.dstack([np.ones((n, n, 3)), a]))


def ice_tex():
    n = 256
    base = lerp(col((120, 186, 232)), col((214, 238, 255)), np.clip(0.5 + 1.1 * (fbm(n, n, beta=2.0, seed=851) - 0.5), 0, 1)[..., None])
    r = np.random.default_rng(852)
    im = Image.new("L", (n * 2, n * 2), 0)
    d = ImageDraw.Draw(im)
    for _ in range(7):
        x, y = r.uniform(0, n), r.uniform(0, n)
        a = r.uniform(0, 6.28)
        pts = [(x, y)]
        for _ in range(9):
            a += r.uniform(-0.7, 0.7)
            x, y = x + math.cos(a) * 14, y + math.sin(a) * 14
            pts.append((x, y))
        for ox in (-n, 0, n):
            for oy in (-n, 0, n):
                d.line([((px + ox) * 2, (py + oy) * 2) for px, py in pts], fill=255, width=2)
    crack = ndi.gaussian_filter(np.asarray(im.resize((n, n), Image.LANCZOS)).astype(float) / 255.0, 0.8)
    base = lerp(base, col((246, 252, 255)), np.clip(crack * 1.4, 0, 1)[..., None] * 0.8)
    G3.save("r3b_ice", base)


GENS = {
    "ar_sky": ar_sky, "ar_mountains": ar_mountains, "ar_hills": ar_hills, "ar_ground": ar_ground, "ar_blocks": ar_blocks,
    "snow_ground": snow_ground, "frost_tuft": frost_tuft, "ice_flowers": ice_flowers, "snow_fir": snow_fir, "night_sky3": night_sky3,
    "night_mountains3": night_mountains3, "aurora_curtain": aurora_curtain, "flake": flake, "ice_tex": ice_tex,
}

if __name__ == "__main__":
    only = sys.argv[1:] or list(GENS)
    for k in only:
        GENS[k]()
        print("ok", k, flush=True)
