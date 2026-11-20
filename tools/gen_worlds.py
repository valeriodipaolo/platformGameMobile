#!/usr/bin/env python3
"""Immagini dei tre mondi senza bosco (livelli 3D 38, 39, 40): il deserto, la fabbrica, la miniera.

Uso:  python3 tools/gen_worlds.py [nome ...]      (nessun nome = tutto)
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_assets3d as G3  # noqa: E402
import gen_autumn as A  # noqa: E402
from gen_assets import fbm, lerp  # noqa: E402


def col(c):
    return np.array(c, dtype=float) / 255.0


def ramp(h, c0, c1, k=1.2, off=0.5):
    return lerp(col(c0), col(c1), np.clip(off + k * (h - 0.5), 0, 1)[..., None])


def tile(n, beta, seed):
    return fbm(n, n, beta=beta, seed=seed)


def ripples(n, seed, freq=14, warp=0.06):
    """Increspature delle dune: onde parallele deformate dal rumore."""
    yy, xx = np.mgrid[0:n, 0:n] / n
    w = tile(n, 2.2, seed)
    return 0.5 + 0.5 * np.sin(2 * math.pi * (freq * (yy + warp * (w - 0.5) * 6) + 0.6 * xx))


def save(name, rgb):
    G3.save(name, np.clip(rgb, 0, 1))


# ---------------------------------------------------------------- deserto

def desert():
    n = 256
    h, r = tile(n, 2.0, 3801), ripples(n, 3802)
    save("r3s_sand", ramp(h, (206, 160, 98), (240, 206, 142)) * (0.9 + 0.12 * r)[..., None])
    save("r3s_dune", ramp(tile(n, 2.2, 3803), (196, 146, 86), (232, 194, 128)) * (0.9 + 0.12 * ripples(n, 3804, 9))[..., None])
    # arenaria a strati per i blocchi
    yy = np.mgrid[0:n, 0:n][0] / n
    strata = 0.5 + 0.5 * np.sin(yy * 2 * math.pi * 7 + 3 * (tile(n, 2.4, 3805) - 0.5))
    stone = ramp(tile(n, 2.4, 3806), (168, 104, 62), (214, 150, 96)) * (0.85 + 0.2 * strata)[..., None]
    im = Image.fromarray((np.clip(stone, 0, 1) * 255).astype(np.uint8))
    ImageDraw.Draw(im).rectangle([2, 2, n - 3, n - 3], outline=(120, 70, 40), width=5)
    G3.save("r3s_stone", np.asarray(im).astype(float) / 255.0)
    # sabbie mobili: più scure e umide, con un vortice
    yy, xx = np.mgrid[0:n, 0:n] / n - 0.5
    ang, rad = np.arctan2(yy, xx), np.sqrt(xx * xx + yy * yy)
    swirl = 0.5 + 0.5 * np.sin(ang * 3 + rad * 40 + 4 * tile(n, 2.0, 3807))
    save("r3s_quick", ramp(tile(n, 1.8, 3808), (120, 86, 50), (168, 124, 74)) * (0.8 + 0.25 * swirl)[..., None])
    c = G3.col
    G3.gen_sky("r3s_sky", (c((60, 120, 200)), c((150, 196, 230)), c((250, 226, 180))), (1.0, 0.9, 0.7))
    # mesas lontane: si parte dalle montagne generiche e si colorano di rosso
    a = A.capture(G3, G3.gen_mountains)
    hh = a.shape[0]
    f = (np.linspace(0, 1, hh)[:, None, None]) ** 1.3
    rgb = a[..., :3].mean(axis=2, keepdims=True) * np.array([1.25, 0.72, 0.48])
    rgb = lerp(rgb, col((236, 200, 160)), 0.2 + 0.35 * f)
    G3.save("r3s_mountains", np.dstack([np.clip(rgb, 0, 1), a[..., 3]]))


# ---------------------------------------------------------------- fabbrica

def factory():
    n = 256
    # lamiera a mandorle
    h = tile(n, 2.4, 3901)
    base = ramp(h, (92, 98, 106), (138, 146, 156), 0.8)
    yy, xx = np.mgrid[0:n, 0:n]
    for k in range(2):
        u = (xx + (yy // 32 % 2) * 16 + k * 0) % 32 - 16
        v = (yy % 32) - 16
        d = np.abs(u * 0.9 + v * 0.5) + np.abs(v * 0.25) * 0
        bump = np.clip(1 - np.abs((u * 0.7 + v * 0.7) / 3.0), 0, 1) * (np.abs(u) < 10) * (np.abs(v) < 10)
        base = base + bump[..., None] * 0.08
    im = Image.fromarray((np.clip(base, 0, 1) * 255).astype(np.uint8))
    d = ImageDraw.Draw(im)
    d.rectangle([1, 1, n - 2, n - 2], outline=(60, 64, 70), width=4)
    for (x, y) in ((10, 10), (n - 11, 10), (10, n - 11), (n - 11, n - 11)):
        d.ellipse([x - 5, y - 5, x + 5, y + 5], fill=(170, 176, 184), outline=(70, 74, 80))
    G3.save("r3f_floor", np.asarray(im).astype(float) / 255.0)
    # pannelli dei lati: cemento macchiato con le strisce gialle e nere ogni tanto
    con = ramp(tile(n, 2.0, 3902), (96, 96, 98), (140, 138, 134))
    save("r3f_panel", con * (0.9 + 0.15 * tile(n, 1.4, 3903))[..., None])
    # blocco d'acciaio con le strisce di pericolo
    im = Image.new("RGB", (n, n), (120, 126, 134))
    d = ImageDraw.Draw(im)
    for i in range(-n, n * 2, 48):
        d.polygon([(i, 0), (i + 24, 0), (i + 24 - n, n), (i - n, n)], fill=(236, 186, 30))
    d.rectangle([30, 30, n - 31, n - 31], fill=(116, 122, 130), outline=(60, 64, 70), width=6)
    for (x, y) in ((44, 44), (n - 45, 44), (44, n - 45), (n - 45, n - 45)):
        d.ellipse([x - 7, y - 7, x + 7, y + 7], fill=(176, 182, 190))
    G3.save("r3f_block", np.asarray(im).astype(float) / 255.0)
    # nastro trasportatore: gomma scura con le traversine (scorre in gioco)
    im = Image.new("RGB", (n, n), (34, 34, 38))
    d = ImageDraw.Draw(im)
    for y in range(0, n, 32):
        d.rectangle([0, y, n, y + 6], fill=(70, 72, 78))
        d.polygon([(n * 0.3, y + 26), (n * 0.5, y + 12), (n * 0.7, y + 26)], fill=(220, 180, 30))
    d.rectangle([0, 0, 14, n], fill=(150, 154, 160))
    d.rectangle([n - 15, 0, n, n], fill=(150, 154, 160))
    G3.save("r3f_belt", np.asarray(im).astype(float) / 255.0)
    c = G3.col
    G3.gen_sky("r3f_sky", (c((70, 64, 70)), c((170, 110, 80)), c((236, 170, 110))), (1.0, 0.7, 0.45))
    # profilo di ciminiere e capannoni lontani
    W, H = 1024, 256
    im = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    r = np.random.default_rng(3904)
    x = 0
    while x < W:
        w = int(r.uniform(40, 120)); hgt = int(r.uniform(50, 150))
        d.rectangle([x, H - hgt, x + w, H], fill=(70, 66, 72, 255))
        if r.uniform() < 0.6:
            cx = x + int(r.uniform(8, w - 8)); ch = int(r.uniform(40, 100))
            d.rectangle([cx - 6, H - hgt - ch, cx + 6, H - hgt], fill=(60, 56, 62, 255))
        for k in range(int(w / 14)):
            if r.uniform() < 0.4:
                wx = x + 6 + k * 14
                d.rectangle([wx, H - hgt + 14, wx + 6, H - hgt + 22], fill=(250, 200, 110, 255))
        x += w + int(r.uniform(0, 20))
    G3.save("r3f_mountains", np.asarray(im).astype(float) / 255.0)


# ---------------------------------------------------------------- miniera

def mine():
    n = 256
    h = tile(n, 2.2, 4001)
    save("r3m_rock", ramp(h, (70, 58, 50), (124, 104, 86)) * (0.85 + 0.2 * tile(n, 1.3, 4002))[..., None])
    # parete di roccia con le vene di cristallo
    wall = ramp(tile(n, 2.0, 4003), (58, 50, 48), (110, 94, 82))
    vein = np.clip(1 - np.abs(tile(n, 1.6, 4004) - 0.5) * 30, 0, 1)
    wall = wall * (1 - vein[..., None] * 0.6) + vein[..., None] * col((120, 200, 255)) * 0.6
    save("r3m_wall", wall)
    im = Image.fromarray((np.clip(ramp(tile(n, 2.4, 4005), (84, 72, 64), (140, 120, 100)), 0, 1) * 255).astype(np.uint8))
    d = ImageDraw.Draw(im)
    d.rectangle([3, 3, n - 4, n - 4], outline=(46, 38, 34), width=6)
    d.line([(40, 60), (120, 110), (200, 90)], fill=(46, 38, 34), width=4)
    G3.save("r3m_block", np.asarray(im).astype(float) / 255.0)
    # volta della caverna: quasi nera, con cristalli che luccicano
    W, H = 1024, 512
    rgb = np.zeros((H, W, 3)) + col((14, 12, 18))
    rr = np.random.default_rng(4006)
    for _ in range(500):
        x, y = int(rr.uniform(0, W)), int(rr.uniform(0, H * 0.8))
        c = col((120, 200, 255)) if rr.uniform() < 0.7 else col((200, 120, 255))
        rgb[y, x] = c * rr.uniform(0.4, 1)
    g = np.linspace(0, 1, H)[:, None, None]
    rgb = rgb + g * col((40, 28, 22)) * 0.8
    save("r3m_sky", rgb)
    # fondo della caverna con le stalattiti
    im = Image.new("RGBA", (W, 256), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    for i in range(80):
        x = rr.uniform(0, W); w = rr.uniform(10, 40); hgt = rr.uniform(60, 220)
        d.polygon([(x - w, 256), (x, 256 - hgt), (x + w, 256)], fill=(46, 38, 40, 255))
    d.rectangle([0, 230, W, 256], fill=(40, 34, 34, 255))
    G3.save("r3m_mountains", np.asarray(im).astype(float) / 255.0)


GENS = {"desert": desert, "factory": factory, "mine": mine}

if __name__ == "__main__":
    for nme in sys.argv[1:] or list(GENS):
        GENS[nme]()
        print("ok", nme)
