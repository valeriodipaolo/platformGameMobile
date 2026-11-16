#!/usr/bin/env python3
"""Pelo a strati: sei sezioni orizzontali di un tappeto di peli (fur_shell_0..5), dalla radice alla punta.

Ogni pelo è un punto che si assottiglia salendo e si piega nel verso del pelo; il gioco disegna il corpo sei volte, ogni volta un po' più
gonfio, con la sezione corrispondente: il contorno dell'animale diventa peloso invece che liscio. Uso:  python3 tools/gen_fur.py
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_assets3d as G3  # noqa: E402
from gen_assets import fbm  # noqa: E402

N = 128
L = 6


def main():
    r = np.random.default_rng(3301)
    n = 2600
    px, py = r.uniform(0, N, n), r.uniform(0, N, n)
    hgt = np.clip(r.normal(0.8, 0.16, n), 0.35, 1.0)
    tone = r.uniform(0.72, 1.0, n)
    rad = r.uniform(0.9, 1.35, n)
    clump = fbm(N, N, beta=2.2, seed=3302)
    yy, xx = np.mgrid[0:N, 0:N].astype(float)
    for k in range(L):
        t = (k + 1) / (L + 1)                      # quota dello strato
        a = np.zeros((N, N))
        c = np.zeros((N, N))
        lean = 3.2 * t * t                          # i peli si piegano all'indietro salendo
        for i in range(n):
            if hgt[i] < t:
                continue
            rr = rad[i] * (1.0 - 0.75 * t / hgt[i]) + 0.25
            cx, cy = px[i], (py[i] + lean) % N
            x0, y0 = int(cx) - 3, int(cy) - 3
            for dy in range(7):
                for dx in range(7):
                    X, Y = (x0 + dx) % N, (y0 + dy) % N
                    ddx = min(abs(X - cx), N - abs(X - cx)); ddy = min(abs(Y - cy), N - abs(Y - cy))
                    v = np.clip(1.25 - np.hypot(ddx, ddy) / rr, 0, 1)
                    if v > a[Y, X]:
                        a[Y, X] = v; c[Y, X] = tone[i]
        v = np.where(a > 0, c, 0.8) * (0.9 + 0.2 * clump)
        G3.save("fur_shell_%d" % k, np.dstack([v, v * 0.985, v * 0.965, np.clip(a * 1.6, 0, 1)]))
        print("ok", k, round(float((a > 0.3).mean()), 2), flush=True)


if __name__ == "__main__":
    main()
