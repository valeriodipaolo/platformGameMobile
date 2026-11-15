#!/usr/bin/env python3
"""Texture realistiche dei nemici della seconda serie: pelle umida, granulosa, guscio, neve, piumino, crosta di lava, plasma, foglia.

Le prime cinque sono chiare e quasi neutre (il colore lo danno i vertici); crosta e plasma sono a colori. Uso:  python3 tools/gen_foes3.py
"""
import os
import sys

import numpy as np
from scipy import ndimage as ndi
from scipy.spatial import cKDTree

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_assets3d as G3  # noqa: E402
from gen_assets import fbm  # noqa: E402
from gen_foes import relief  # noqa: E402

N = 256


def voronoi(n_pts, seed):
    """Distanze dal bordo cella (F2-F1) e id cella, ripetibili."""
    r = np.random.default_rng(seed)
    p = r.random((n_pts, 2)) * N
    P = np.vstack([p + [dx * N, dy * N] for dx in (-1, 0, 1) for dy in (-1, 0, 1)])
    ids = np.tile(np.arange(n_pts), 9)
    yy, xx = np.mgrid[0:N, 0:N]
    q = np.dstack([xx, yy]).reshape(-1, 2).astype(float)
    d, i = cKDTree(P).query(q, k=2)
    return (d[:, 1] - d[:, 0]).reshape(N, N), ids[i[:, 0]].reshape(N, N), d[:, 0].reshape(N, N)


def skin():
    """Pelle di anfibio: pori, piccole verruche, riflessi umidi (valori alti a macchie)."""
    hm = 0.5 * fbm(N, N, beta=2.4, seed=1001) + 0.5 * fbm(N, N, beta=1.2, seed=1002)
    edge, cid, dist = voronoi(900, 1003)
    warts = np.clip(1 - dist / 5.5, 0, 1) ** 0.8 * (0.5 + 0.5 * np.random.default_rng(1004).random(900)[cid])
    h = ndi.gaussian_filter(0.6 * hm + 0.5 * warts, 0.8, mode="wrap")
    lit = relief(h, 5.0)
    wet = ndi.gaussian_filter(fbm(N, N, beta=2.8, seed=1005), 2, mode="wrap")
    v = np.clip(0.78 * lit + 0.16 * (wet - 0.5) * 2, 0.35, 1.0)
    G3.save("foe_skin", np.dstack([v, v * 0.99, v * 0.97, np.ones((N, N))]))


def scale():
    """Pelle fine e granulosa di salamandra."""
    edge, cid, dist = voronoi(1600, 1011)
    h = np.clip(edge / 3.0, 0, 1) ** 0.5 * 0.7 + 0.3 * fbm(N, N, beta=2.0, seed=1012)
    lit = relief(ndi.gaussian_filter(h, 0.6, mode="wrap"), 4.0)
    v = np.clip(0.8 * lit, 0.4, 1.0)
    G3.save("foe_scale", np.dstack([v, v, v, np.ones((N, N))]))


def shell():
    """Guscio di lumaca: bande lungo u (spirale), linee di crescita fini e venature."""
    y, x = np.mgrid[0:N, 0:N].astype(float)
    growth = 0.5 + 0.5 * np.sin(y / N * 2 * np.pi * 46 + 1.5 * fbm(N, N, beta=2, seed=1021))
    band = 0.5 + 0.5 * np.sin(x / N * 2 * np.pi * 5 + 3 * fbm(N, N, beta=2.4, seed=1022))
    hm = 0.6 * growth + 0.4 * fbm(N, N, beta=1.4, seed=1023)
    lit = relief(ndi.gaussian_filter(hm, 0.6, mode="wrap"), 2.2)
    v = np.clip((0.62 + 0.3 * band) * lit * (0.9 + 0.1 * growth), 0.3, 1.0)
    G3.save("foe_shell", np.dstack([v, v * 0.96, v * 0.9, np.ones((N, N))]))


def snow():
    """Neve compatta: grumi, piccoli cristalli luccicanti."""
    hm = 0.6 * fbm(N, N, beta=2.6, seed=1031) + 0.4 * fbm(N, N, beta=1.4, seed=1032)
    lit = relief(ndi.gaussian_filter(hm, 1.0, mode="wrap"), 3.2)
    sp = (np.random.default_rng(1033).random((N, N)) > 0.992).astype(float)
    sp = ndi.gaussian_filter(sp, 0.7, mode="wrap") * 6
    v = np.clip(0.88 * lit + sp, 0.5, 1.0)
    G3.save("foe_snow", np.dstack([v * 0.97, v * 0.99, v, np.ones((N, N))]))


def down():
    """Piumino corto di pinguino/uccello: barbe fitte e lucide verso il basso."""
    s = fbm(N, N, beta=0.9, seed=1041, ax=0.5, ay=14.0)
    c = fbm(N, N, beta=2.0, seed=1042, ax=0.8, ay=3.0)
    hm = 0.7 * s + 0.4 * c
    v = np.clip((0.82 + 0.5 * (s - 0.5) + 0.2 * (c - 0.5)) * relief(hm, 1.4), 0.45, 1.0)
    G3.save("foe_down", np.dstack([v, v, v, np.ones((N, N))]))


def ember():
    """Crosta di basalto scuro con crepe incandescenti (a colori)."""
    edge, cid, dist = voronoi(70, 1051)
    wob = fbm(N, N, beta=2.4, seed=1052)
    crack = np.clip(1 - (edge + (wob - 0.5) * 9) / 5.0, 0, 1) ** 1.5
    crust = 0.5 * fbm(N, N, beta=1.6, seed=1053) + 0.5 * fbm(N, N, beta=2.8, seed=1054)
    lit = relief(ndi.gaussian_filter(crust, 0.8, mode="wrap"), 3.0)
    base = np.clip(0.12 + 0.2 * lit * (0.6 + crust), 0, 0.5)
    glow = ndi.gaussian_filter(crack, 1.2, mode="wrap")
    heat = np.clip(glow * 1.3, 0, 1)
    r = base * (1 - heat) + heat * 1.0
    g = base * 0.9 * (1 - heat) + heat * (0.28 + 0.5 * heat ** 2)
    b = base * 0.8 * (1 - heat) + heat * (0.02 + 0.2 * heat ** 4)
    G3.save("foe_ember", np.dstack([np.clip(r, 0, 1), np.clip(g, 0, 1), np.clip(b, 0, 1), np.ones((N, N))]))


def plasma():
    """Superficie solare: granulazione, macchie, zone più brillanti (a colori)."""
    edge, cid, dist = voronoi(220, 1061)
    gran = np.clip(edge / 7.0, 0, 1)
    big = fbm(N, N, beta=2.2, seed=1062)
    heat = np.clip(0.55 + 0.45 * gran - 0.5 * np.clip((0.4 - big) * 5, 0, 1) * 0.6 + 0.2 * (big - 0.5), 0, 1)
    r = np.clip(0.98 + 0.02 * heat, 0, 1)
    g = np.clip(0.30 + 0.62 * heat ** 1.4, 0, 1)
    b = np.clip(0.02 + 0.45 * heat ** 4, 0, 1)
    G3.save("foe_plasma", np.dstack([r, g, b, np.ones((N, N))]))


def petal():
    """Foglia/trappola carnivora: nervature ramificate, superficie leggermente cerosa."""
    y, x = np.mgrid[0:N, 0:N].astype(float)
    veins = np.abs(np.sin(x / N * 2 * np.pi * 7 + 2.5 * fbm(N, N, beta=2.2, seed=1071))) ** 0.35
    cell = fbm(N, N, beta=1.8, seed=1072)
    hm = 0.45 * veins + 0.4 * cell
    lit = relief(ndi.gaussian_filter(hm, 0.8, mode="wrap"), 2.4)
    v = np.clip((0.7 + 0.2 * veins + 0.1 * (cell - 0.5)) * lit, 0.35, 1.0)
    G3.save("foe_petal", np.dstack([v, v, v * 0.96, np.ones((N, N))]))


def mahogany():
    """Buccia lucida di castagna: venature lunghe e scure su fondo rossiccio."""
    y, x = np.mgrid[0:N, 0:N].astype(float)
    g = fbm(N, N, beta=1.3, seed=1081, ax=0.5, ay=8.0)
    st = 0.5 + 0.5 * np.sin(y / N * 2 * np.pi * 9 + 6 * g)
    lit = relief(ndi.gaussian_filter(0.6 * st + 0.4 * g, 0.7, mode="wrap"), 1.6)
    v = np.clip((0.62 + 0.38 * st) * lit * (0.9 + 0.2 * (g - 0.5)), 0.35, 1.0)
    G3.save("foe_mahogany", np.dstack([v, v * 0.98, v * 0.96, np.ones((N, N))]))


if __name__ == "__main__":
    for f in (skin, scale, shell, snow, down, ember, plasma, petal, mahogany):
        f()
        print("ok", f.__name__, flush=True)
