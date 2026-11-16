#!/usr/bin/env python3
"""Materiali del protagonista: lamiera verniciata e usurata (hero_metal) e cuoio vissuto (hero_leather).

Texture chiare e quasi neutre, ripetibili: il colore lo danno i vertici. Uso:  python3 tools/gen_hero.py
"""
import os
import sys

import numpy as np
from PIL import Image, ImageDraw
from scipy import ndimage as ndi

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_assets3d as G3  # noqa: E402
from gen_assets import fbm  # noqa: E402
from gen_foes import relief  # noqa: E402
from gen_foes3 import voronoi  # noqa: E402

N = 256


def metal():
    """Smalto su lamiera: lieve buccia d'arancia, graffi sottili, scheggiature sugli spigoli del pannello, giunto con rivetti, sporco."""
    r = np.random.default_rng(2101)
    mott = fbm(N, N, beta=2.6, seed=2102)
    peel = fbm(N, N, beta=0.9, seed=2103)
    v = 0.90 + 0.05 * (mott - 0.5) + 0.02 * (peel - 0.5)
    # graffi: righe sottili, quasi tutte lungo due direzioni
    ss = 3
    im = Image.new("L", (N * ss, N * ss), 128)
    d = ImageDraw.Draw(im)
    for _ in range(260):
        x, y = r.uniform(0, N * ss), r.uniform(0, N * ss)
        a = r.choice([0.25, 1.9]) + r.normal(0, 0.35)
        ln = r.uniform(8, 70) * ss
        c = int(128 + r.choice([-1, 1], p=[0.7, 0.3]) * r.uniform(20, 70))
        for ox in (-N * ss, 0, N * ss):
            for oy in (-N * ss, 0, N * ss):
                d.line([(x + ox, y + oy), (x + ox + np.cos(a) * ln, y + oy + np.sin(a) * ln)], fill=c, width=1)
    scr = np.asarray(im.resize((N, N), Image.LANCZOS)).astype(float) / 255.0 - 0.5
    v += 0.5 * scr
    # giunto del pannello sui bordi della mattonella, con una luce sul labbro
    yy, xx = np.mgrid[0:N, 0:N].astype(float)
    ed = np.minimum(np.minimum(xx, N - 1 - xx), np.minimum(yy, N - 1 - yy))
    seam = np.clip(1 - ed / 1.6, 0, 1)
    lip = np.clip(1 - np.abs(ed - 3.0) / 1.4, 0, 1)
    # scheggiature: più fitte vicino al giunto
    chipn = fbm(N, N, beta=1.3, seed=2104)
    chip = np.clip((chipn - (0.80 - 0.22 * np.clip(1 - ed / 14, 0, 1))) * 22, 0, 1)
    chip = ndi.gaussian_filter(chip, 0.5, mode="wrap")
    # rivetti lungo il giunto
    riv = np.zeros((N, N))
    for k in range(8):
        t = (k + 0.5) / 8 * N
        for (cx, cy) in ((t, 9), (t, N - 9), (9, t), (N - 9, t)):
            dd = np.hypot(xx - cx, yy - cy)
            riv += np.clip(1 - dd / 3.2, 0, 1)
    h = 0.5 * peel * 0.15 - seam * 0.9 + riv * 0.8 - chip * 0.25
    lit = relief(ndi.gaussian_filter(h, 0.7, mode="wrap"), 1.6)
    grime = ndi.gaussian_filter(fbm(N, N, beta=2.2, seed=2105), 3, mode="wrap")
    dirt = np.clip((grime - 0.52) * 3.2, 0, 1) * 0.28 + seam * 0.5 + np.clip(1 - ed / 9, 0, 1) * 0.18
    v = np.clip(v * lit, 0, 1.05)
    v = v * (1 - 0.55 * chip) + 0.40 * chip + 0.06 * lip
    rgb = np.dstack([v, v, v])
    rgb *= (1 - dirt[..., None] * np.array([0.30, 0.38, 0.50]))
    G3.save("hero_metal", np.dstack([np.clip(rgb, 0, 1), np.ones((N, N))]))


def leather():
    """Cuoio: grana a celle, pieghe, bordi consumati più chiari, macchie scure."""
    edge, cid, dist = voronoi(5200, 2111)
    grain = np.clip(edge / 1.4, 0, 1) ** 0.6
    cre = 1 - np.abs(2 * fbm(N, N, beta=2.0, seed=2112, ax=1.0, ay=2.5) - 1)
    crease = np.clip((cre - 0.86) * 9, 0, 1)
    h = 0.55 * grain + 0.25 * fbm(N, N, beta=1.6, seed=2113) - 0.7 * crease
    lit = relief(ndi.gaussian_filter(h, 0.8, mode="wrap"), 1.5)
    wear = ndi.gaussian_filter(fbm(N, N, beta=2.4, seed=2114), 2.5, mode="wrap")
    v = np.clip((0.78 + 0.26 * (wear - 0.5)) * lit, 0.3, 1.0)
    rgb = np.dstack([v, v * (0.96 + 0.05 * wear), v * (0.90 + 0.10 * wear)])
    G3.save("hero_leather", np.dstack([np.clip(rgb, 0, 1), np.ones((N, N))]))


if __name__ == "__main__":
    metal()
    leather()
    print("ok")
