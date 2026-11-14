#!/usr/bin/env python3
"""Porta d'arrivo dei livelli 2D: arco di pietra con interno luminoso (gate) e battente di legno (gate_door).

Uso:  python3 tools/gen_gate.py
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_assets as G  # noqa: E402
from gen_assets import fbm, Cv, T  # noqa: E402


def gate():
    W, H = 3 * T, 4 * T
    cv = Cv(W, H, 3)
    n3 = (H * 3, W * 3)
    y, x = np.mgrid[0:n3[0], 0:n3[1]].astype(float)
    # interno: bagliore caldo che sale dal basso, con un arco in cima
    ix0, ix1, iy0 = 0.55 * T, 2.45 * T, 0.95 * T
    inner = cv.rrect(ix0, iy0, ix1, H + 4, 0) & ~cv.rrect(ix0 - 1, iy0 - 1, ix1 + 1, iy0 + 0.95 * T, 0) | cv.ell(1.5 * T, iy0 + 0.95 * T, 0.95 * T, 0.95 * T) & cv.rrect(ix0, iy0, ix1, H + 4, 0)
    glow = 0.55 + 0.6 * np.clip((y / 3 - iy0) / (H - iy0), 0, 1) ** 1.4
    cv.add(inner, (255, 200, 110), bevel=1.0, tex=glow)
    cv.glow(inner, (255, 190, 90), radius=10, strength=0.35)
    n = 3 * T
    tex = 0.80 + 0.38 * fbm(H * 3, W * 3, beta=1.7, seed=611) + 0.12 * (fbm(H * 3, W * 3, beta=0.7, seed=612) - 0.5)
    stone = (150, 140, 130)
    # pilastri a conci
    for side, x0 in ((0, 0.0), (1, 2.45 * T)):
        for k in range(8):
            yy = k * 0.5 * T + 0.2 * T
            wd = 0.55 * T + (0.06 * T if k % 2 else 0)
            xs = x0 + (0 if side == 0 else 0.0)
            cv.add(cv.rrect(xs + 1.5, yy + 1.5, xs + wd - 1.5, yy + 0.5 * T - 1.5, 9), stone, ao=0.2, bevel=7, spec=0.12, tex=tex)
    # architrave ad arco
    for i in range(9):
        a0 = np.pi * (1 - i / 9.0) + 0.0
        a1 = np.pi * (1 - (i + 1) / 9.0)
        cx, cy, ro, ri = 1.5 * T, iy0 + 0.95 * T, 1.5 * T + 0.0, 0.95 * T
        pts = []
        for a in np.linspace(a0, a1, 5):
            pts.append((cx + ro * np.cos(a), cy - ro * np.sin(a) * 0.82 - 4))
        for a in np.linspace(a1, a0, 5):
            pts.append((cx + ri * np.cos(a), cy - ri * np.sin(a)))
        cv.add(cv.poly(pts), stone if i % 2 else (160, 150, 138), ao=0.2, bevel=6, spec=0.12, tex=tex)
    cv.add(cv.rrect(0.5 * T - 4, 0.15 * T, 2.5 * T + 4, 0.55 * T, 10), (170, 160, 146), ao=0.25, bevel=10, spec=0.15, tex=tex)   # cornice
    cv.add(cv.rrect(0.28 * T, 3.86 * T, 2.72 * T, 4.0 * T, 6), (120, 112, 104), bevel=5, spec=0.1, tex=tex)                       # soglia
    G.save("gate", cv.result())


def door():
    w, h = int(0.95 * T), int(3.05 * T)
    cv = Cv(w, h, 3)
    tex = 0.86 + 0.3 * fbm(h * 3, w * 3, beta=1.9, seed=621)
    cv.add(cv.rrect(0, 0, w, h, 5), (130, 84, 46), bevel=6, spec=0.12, tex=tex)
    for i in range(1, 4):                                     # assi
        xx = w * i / 4.0
        cv.add(cv.capsule(xx, 4, xx, h - 4, 1.2), (80, 50, 28), bevel=1.2, spec=0.05)
    for yy in (0.2 * h, 0.8 * h):                             # fasce di ferro
        cv.add(cv.rrect(0, yy - 5, w, yy + 5, 3), (74, 78, 88), bevel=4, spec=0.5, shin=26)
        for xx in (8, w - 8):
            cv.add(cv.ell(xx, yy, 3, 3), (150, 156, 168), bevel=2, spec=0.7)
    cv.add(cv.ell(w - 10, h * 0.5, 4.5, 4.5), (230, 190, 70), bevel=3, spec=0.8, shin=30)       # maniglia
    G.save("gate_door", cv.result())


if __name__ == "__main__":
    gate()
    door()
    print("ok gate")
