#!/usr/bin/env python3
"""Texture dei nemici 3D: pelo, piumaggio, penna, guscio di ghianda, cappello a scaglie, corazza di scarabeo.

Sono chiare e quasi neutre: il colore lo danno i vertici dei modelli. Uso:  python3 tools/gen_foes.py
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw
from scipy import ndimage as ndi

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_assets3d as G3  # noqa: E402
from gen_assets import fbm, lerp, smooth  # noqa: E402

N = 256


def relief(hm, k=3.0):
    """Luce radente su una mappa di altezze ripetibile: valori attorno a 1."""
    gx = np.roll(hm, -1, 1) - np.roll(hm, 1, 1)
    gy = np.roll(hm, -1, 0) - np.roll(hm, 1, 0)
    return np.clip(1.0 + k * (-0.6 * gx - 0.8 * gy), 0.35, 1.6)


def fur():
    strands = fbm(N, N, beta=1.1, seed=901, ax=0.6, ay=9.0)          # ciocche lungo la verticale
    clump = fbm(N, N, beta=2.2, seed=902, ax=0.7, ay=2.0)
    hm = 0.7 * strands + 0.5 * clump
    v = np.clip((0.82 + 0.5 * (strands - 0.5) + 0.3 * (clump - 0.5)) * relief(hm, 1.6), 0.4, 1.0)
    G3.save("foe_fur", np.dstack([v, v * 0.985, v * 0.96, np.ones((N, N))]))


def tile_draw(fn, ss=3):
    im = Image.new("L", (N * ss, N * ss), 0)
    d = ImageDraw.Draw(im)
    for ox in (-N, 0, N):
        for oy in (-N, 0, N):
            fn(d, ox * ss, oy * ss, ss)
    return np.asarray(im.resize((N, N), Image.LANCZOS)).astype(float) / 255.0


def scallops(cols, rows, round_tip=1.0, seed=0):
    """Mappa di altezze di scaglie/piume embricate: ogni fila copre la metà alta di quella sotto."""
    hm = np.zeros((N, N))
    shaft = np.zeros((N, N))
    y, x = np.mgrid[0:N, 0:N].astype(float)
    cw, rh = N / cols, N / rows
    for r in range(-2, rows + 3):                 # dal basso verso l'alto dell'immagine: le file alte coprono
        cy = N - (r - 0.5) * rh
        off = (r % 2) * cw / 2
        for wrap in (0,):
            u = ((x - off) % cw) / cw - 0.5        # -0.5..0.5 dentro la scaglia
            vv = (y + wrap - cy) / (rh * 2.0)      # 0 in cima, 1 in punta (in basso)
            vc = np.clip(vv, 0, 1)
            inside = (vv >= 0) & (vv <= 1) & (np.abs(u) * 2 <= np.sqrt(np.clip(1 - (vc * round_tip) ** 2.2, 0, 1)) + 1e-6)
            edge = np.sqrt(np.clip(1 - (vc * round_tip) ** 2.2, 0, 1)) - np.abs(u) * 2
            h = 0.5 * np.clip(edge * 3, 0, 1) + 0.35 * vv
            hm = np.where(inside, h, hm)
            shaft = np.where(inside, np.exp(-(u * 14) ** 2) * (1 - 0.3 * vv), shaft)
            barb = np.where(inside, 0.5 + 0.5 * np.sin((np.abs(u) * 34 + vv * 40)), 0)
            shaft = np.where(inside, shaft + 0.18 * barb * np.clip(edge * 4, 0, 1), shaft)
    return hm, shaft


def plume():
    hm, shaft = scallops(6, 10, 1.0)
    lit = relief(ndi.gaussian_filter(hm, 0.8, mode="wrap"), 5.0)
    v = np.clip(0.76 * lit + 0.2 * shaft, 0.3, 1.0)
    sheen = fbm(N, N, beta=2.4, seed=911)
    rgb = np.dstack([v * (0.9 + 0.12 * sheen), v * (0.92 + 0.1 * (1 - sheen)), v * 1.0])
    G3.save("foe_plume", np.dstack([np.clip(rgb, 0, 1), np.ones((N, N))]))


def card():
    """Una penna intera (punta in alto), con trasparenza: rachide chiaro, barbe oblique, bordo sfrangiato."""
    w, h, ss = 64, 256, 4
    y, x = np.mgrid[0:h * ss, 0:w * ss].astype(float)
    u = x / (w * ss) - 0.5
    t = 1 - y / (h * ss)                                   # 0 alla base, 1 in punta
    half = 0.44 * np.clip(t / 0.12, 0, 1) ** 0.7 * np.sqrt(np.clip(1 - np.clip((t - 0.55) / 0.45, 0, 1) ** 2.4, 0, 1))
    half = np.where(u < 0, half * 0.72, half)              # vessillo asimmetrico
    notch = 0.035 * (np.sin(t * 90 + np.sign(u) * 2) > 0.93)
    inside = np.abs(u) < (half - notch)
    barbs = 0.5 + 0.5 * np.sin((np.abs(u) * 60 + t * 110))
    v = 0.78 + 0.18 * barbs - 0.5 * np.abs(u) + 0.1 * t
    v = np.where(np.abs(u) < 0.03 * (1.1 - t), 1.0, v)
    quill = (np.abs(u) < 0.035) & (t < 0.14)
    a = (inside | quill).astype(float)
    rgb = np.dstack([v * 0.94, v * 0.96, v])
    arr = np.dstack([rgb, a])
    arr = arr.reshape(h, ss, w, ss, 4).mean(axis=(1, 3))
    arr[..., :3] = np.clip(arr[..., :3], 0, 1)
    G3.save("foe_card", arr)


def nut():
    grain = fbm(N, N, beta=1.3, seed=921, ax=0.35, ay=12.0)
    broad = fbm(N, N, beta=2.3, seed=922, ax=0.5, ay=3.0)
    x = np.arange(N)[None, :] * np.ones((N, 1))
    rib = 0.5 + 0.5 * np.cos(2 * np.pi * x / N * 16 + 3.0 * (broad - 0.5))
    v = np.clip(0.80 + 0.34 * (grain - 0.5) + 0.2 * (broad - 0.5) + 0.1 * rib, 0.4, 1.0)
    G3.save("foe_nut", np.dstack([v, v * 0.97, v * 0.9, np.ones((N, N))]))


def cap():
    hm, _ = scallops(8, 8, 1.0)
    hm = ndi.gaussian_filter(hm, 1.0, mode="wrap")
    lit = relief(hm, 6.0)
    grit = fbm(N, N, beta=1.0, seed=931)
    v = np.clip(0.74 * lit + 0.2 * (grit - 0.5) + 0.08, 0.25, 1.0)
    G3.save("foe_cap", np.dstack([v, v * 0.96, v * 0.9, np.ones((N, N))]))


def chitin():
    x = np.arange(N)[None, :] * np.ones((N, 1))
    y = np.arange(N)[:, None] * np.ones((1, N))
    ridge = 0.5 + 0.5 * np.cos(2 * np.pi * x / N * 12)
    hm = 0.6 * ridge
    r = np.random.default_rng(941)
    pits = np.zeros((N, N))
    for _ in range(520):
        px, py = r.integers(0, N), r.integers(0, N)
        pits[py, px] = 1
    pits = ndi.gaussian_filter(pits, 1.1, mode="wrap") * 9
    hm = hm - 0.5 * np.clip(pits, 0, 1)
    lit = relief(ndi.gaussian_filter(hm, 0.7, mode="wrap"), 4.0)
    hue = fbm(N, N, beta=2.6, seed=942)
    v = np.clip(0.8 * lit, 0.3, 1.0)
    rgb = np.dstack([v * (0.84 + 0.3 * hue), v * (0.86 + 0.24 * (1 - hue)), v * (0.9 + 0.1 * np.sin(y / N * 2 * np.pi))])
    G3.save("foe_chitin", np.dstack([np.clip(rgb, 0, 1), np.ones((N, N))]))


if __name__ == "__main__":
    for f in (fur, plume, card, nut, cap, chitin):
        f()
        print("ok", f.__name__, flush=True)
