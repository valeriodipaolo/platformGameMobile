#!/usr/bin/env python3
"""Texture per i dettagli dei livelli 3D e per il personaggio: sentiero battuto (r3_trail) e stoffa (r3_cloth).

Uso:  python3 tools/gen_decor.py
Richiede: numpy, scipy, Pillow (e gen_assets.py accanto a questo file).
"""
import math
import os
import sys

import numpy as np
from scipy import ndimage as ndi

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_assets import fbm, lerp, lit_from_height, smooth, save  # noqa: E402


def col(c):
    return np.array(c, dtype=float) / 255.0


def gen_trail():
    """Striscia di terra battuta con sassolini: u attraversa il sentiero, v lo percorre (si ripete). Bordi frastagliati."""
    w, h = 256, 512
    low = fbm(h, w, beta=2.2, seed=901)
    mid = fbm(h, w, beta=1.4, seed=902)
    fine = fbm(h, w, beta=0.6, seed=903)
    r = np.random.default_rng(904)
    hm = low * 10 + mid * 5 + fine * 2
    base = lerp(col((126, 96, 62)), col((186, 152, 104)), np.clip(0.55 * low + 0.3 * mid + 0.15 * fine, 0, 1)[..., None])
    # sassolini: dischi chiari con ombra
    peb = np.zeros((h, w))
    tone = np.zeros((h, w))
    yy, xx = np.mgrid[0:h, 0:w]
    for _ in range(150):
        cx, cy, rad = r.uniform(30, w - 30), r.uniform(0, h), r.uniform(2.0, 6.0)
        el = r.uniform(0.6, 1.0)
        for oy in (-h, 0, h):
            d = np.sqrt(((xx - cx) / rad) ** 2 + ((yy - cy - oy) / (rad * el)) ** 2)
            m = np.clip(1.25 - d, 0, 1)
            sel = m > peb
            peb = np.where(sel, m, peb)
            tone = np.where(sel, r.uniform(0.75, 1.15), tone)
    pm = smooth(peb * 2.2)
    hm = hm + pm * 9
    diff, spec = lit_from_height(ndi.gaussian_filter(hm, 0.8), wrap=True)
    stone = lerp(col((118, 114, 108)), col((206, 200, 188)), np.clip(tone - 0.2, 0, 1)[..., None])
    arr = base * (1 - pm[..., None]) + stone * pm[..., None]
    arr = arr * (0.55 + 0.7 * diff)[..., None] + 0.04 * spec[..., None] ** 8
    # solchi leggeri lungo il cammino
    u = (np.arange(w)[None, :] + 0.5) / w
    rut = 0.5 + 0.5 * np.cos((u - 0.5) * 2 * math.pi * 2.0 + low * 2.0)
    arr *= (0.9 + 0.14 * rut)[..., None]
    # bordo: la terra si dirada in chiazze dentro l'erba
    edge = 1 - np.abs(u - 0.5) * 2                      # 1 al centro, 0 ai bordi
    a = smooth((edge - 0.16 + 0.42 * (low - 0.5) + 0.25 * (mid - 0.5)) * 4.2)
    a = ndi.gaussian_filter(a, 1.0)
    a[:, :3] = 0
    a[:, -3:] = 0
    arr *= (0.78 + 0.22 * smooth(a * 1.5))[..., None]   # più scura dove tocca l'erba
    save("r3_trail", np.dstack([np.clip(arr, 0, 1), np.clip(a, 0, 1)]))


def gen_cloth():
    """Tela chiara a trama fitta, con pieghe morbide: il colore lo danno i vertici."""
    n = 128
    yy, xx = np.mgrid[0:n, 0:n]
    warp = 0.5 + 0.5 * np.sin(xx * 2 * math.pi / 4.0)
    weft = 0.5 + 0.5 * np.sin(yy * 2 * math.pi / 4.0)
    weave = 0.5 * warp * (0.6 + 0.4 * weft) + 0.5 * weft * (0.6 + 0.4 * warp)
    fold = fbm(n, n, beta=2.6, seed=911)
    fuzz = fbm(n, n, beta=0.3, seed=912)
    v = 0.86 + 0.07 * weave + 0.07 * (fold - 0.5) * 2 + 0.03 * (fuzz - 0.5)
    arr = np.dstack([v, v * 0.995, v * 0.985])
    save("r3_cloth", np.clip(arr, 0, 1))


if __name__ == "__main__":
    gen_trail()
    gen_cloth()
    print("ok")
