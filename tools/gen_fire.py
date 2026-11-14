#!/usr/bin/env python3
"""Immagini dei livelli con l'incendio: fumo, bagliori arancio, terreno bruciato e braci (2D e 3D).

Uso:  python3 tools/gen_fire.py [nome ...]      (nessun nome = tutto)
Parte dalle immagini già presenti (prato, bosco) e le "brucia": scurisce, aggiunge crepe incandescenti e fiamme.
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
import gen_autumn as A  # noqa: E402
from gen_assets import fbm, fbm1, lerp, smooth, Cv, T, down  # noqa: E402

RES = G.OUT


def col(c):
    return np.array(c, dtype=float) / 255.0


def load(name):
    return np.asarray(Image.open(os.path.join(RES, name + ".png")).convert("RGBA")).astype(float) / 255.0


def lum_of(rgb):
    return np.clip(rgb @ np.array([0.3, 0.59, 0.11]), 0, 1)


def scorch(rgba, seed, glow=0.55, dark=(14, 10, 10), mid=(66, 40, 30), light=(150, 92, 52), thr=0.62):
    """Ricolora in toni di carbone/cenere e aggiunge venature di brace dove il rumore supera una soglia."""
    rgb = rgba[..., :3]
    h, w = rgb.shape[:2]
    lum = np.clip((lum_of(rgb) - 0.08) / 0.8, 0, 1)
    out = np.where(lum[..., None] < 0.5, lerp(col(dark), col(mid), (lum / 0.5)[..., None]), lerp(col(mid), col(light), ((lum - 0.5) / 0.5)[..., None]))
    if glow > 0:
        n = fbm(h, w, beta=2.0, seed=seed)
        ridge = 1 - np.abs(n - thr + 0.0) * 7.0
        vein = np.clip(ridge, 0, 1) ** 2 * glow
        ember = np.stack([1.0 * np.ones_like(vein), 0.42 + 0.35 * vein, 0.08 * np.ones_like(vein)], -1)
        out = out * (1 - vein[..., None] * 0.9) + ember * vein[..., None] * 1.1
    return np.dstack([np.clip(out, 0, 1), rgba[..., 3]])


def flame_blob(d, x, y, w, h, ss):
    """Una fiamma: goccia arancio con cuore giallo, disegnata nell'immagine supersampled."""
    for k, c in enumerate(((220, 50, 10, 235), (255, 120, 20, 240), (255, 205, 70, 245))):
        f = 1 - k * 0.3
        pts = [(x * ss, (y - h * f) * ss), ((x + w * 0.5 * f) * ss, (y - h * 0.3 * f) * ss), ((x + w * 0.3 * f) * ss, y * ss),
               ((x - w * 0.3 * f) * ss, y * ss), ((x - w * 0.5 * f) * ss, (y - h * 0.3 * f) * ss)]
        d.polygon(pts, fill=c)


# ------------------------------------------------------------------ 2D
def sky_array(w, h, seed):
    y = np.linspace(0, 1, h)[:, None]
    x = np.linspace(0, 1, w, endpoint=False)[None, :]
    top, mid, hor = col((26, 14, 16)), col((120, 44, 22)), col((255, 150, 40))
    t = y ** 1.1
    grad = np.where(t[..., None] < 0.6, lerp(top, mid, (t / 0.6)[..., None]), lerp(mid, hor, ((t - 0.6) / 0.4)[..., None]))
    sky = np.broadcast_to(grad, (h, w, 3)).copy()
    # nubi di fumo: rumore periodico, più dense in alto
    s1 = fbm(h, w, beta=2.2, seed=seed)
    s2 = fbm(h, w, beta=1.4, seed=seed + 1)
    smoke = np.clip((s1 + 0.4 * (s2 - 0.5) - 0.42) * 3.2, 0, 1) * (0.55 + 0.45 * (1 - y))
    sky = sky * (1 - smoke[..., None] * 0.75) + col((38, 30, 30)) * smoke[..., None] * 0.75
    # il sole rosso velato dal fumo
    cy, cx = 0.74, 0.3
    r = np.sqrt(((y - cy) * 1.0) ** 2 + ((x - cx) * 0.5) ** 2)
    sky += col((255, 110, 30)) * (np.exp(-r * 9) * 0.55)[..., None] * (1 - smoke[..., None] * 0.6)
    sky += col((255, 220, 140)) * (smooth((0.05 - r) * 40) * 0.5)[..., None] * (1 - smoke[..., None] * 0.8)
    # bagliore sull'orizzonte
    hz = smooth((y - 0.62) * 3.0)
    sky += col((255, 90, 10)) * (hz * 0.3 * (0.7 + 0.3 * np.sin(2 * np.pi * 7 * x)))[..., None]
    return np.clip(sky, 0, 1)


def fr_sky():
    G.save("fr_sky", sky_array(1536, 768, 901))


def fr_mountains():
    a = A.capture(G, G.gen_mountains)
    out = scorch(a, 902, glow=0.0, dark=(30, 16, 16), mid=(96, 42, 26), light=(214, 110, 50))
    # fumo rosso in basso: lontananza
    h = out.shape[0]
    f = (np.linspace(0, 1, h)[:, None, None]) ** 2
    out[..., :3] = np.clip(lerp(out[..., :3], col((190, 80, 30)), f * 0.35), 0, 1)
    G.save("fr_mountains", out)


def tree_fire(d, x, base, h, w, ss, body, flames, r):
    d.rectangle([(x - w * 0.05) * ss, (base - h * 0.2) * ss, (x + w * 0.05) * ss, (base + 2) * ss], fill=(18, 12, 12, 255))
    for i in range(4):
        f0, f1 = i / 4, (i + 1) / 4
        ytop = base - h * (0.15 + 0.85 * f1)
        ybot = base - h * (0.15 + 0.85 * f0) + h * 0.04
        wd = w * (1.0 - 0.75 * f0)
        d.polygon([(x * ss, ytop * ss), ((x + wd / 2) * ss, ybot * ss), ((x - wd / 2) * ss, ybot * ss)], fill=body)
    if flames:
        for _ in range(int(h / 9)):
            fx = x + r.uniform(-w * 0.28, w * 0.28)
            fy = base - h * r.uniform(0.2, 0.9)
            flame_blob(d, fx, fy, r.uniform(6, 12), r.uniform(12, 24), ss)


def fr_hills():
    """Pineta in fiamme in tre piani: lontana velata di fumo, vicina nera con le chiome incendiate."""
    h, w, ss = 560, 2048, 2
    im = Image.new("RGBA", (w * ss, h * ss), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    r = np.random.default_rng(911)
    layers = [(60, 62, 46, 24, (96, 46, 36, 255), 0.0, 46),
              (150, 100, 78, 34, (52, 26, 22, 255), 0.35, 34),
              (260, 160, 120, 52, (20, 14, 14, 255), 0.8, 26)]
    for y0, amp, th, tw, body, fl, count in layers:
        ridge = y0 + amp * fbm1(w, beta=2.4, seed=int(y0) + 3)
        pts = [(x * ss, ridge[x] * ss) for x in range(0, w, 8)]
        d.polygon(pts + [(w * ss, h * ss), (0, h * ss)], fill=body)
        for _ in range(count * 3):
            x = r.uniform(0, w)
            hh = th * r.uniform(0.6, 1.25) * 1.6
            for off in (-w, 0, w):
                if -tw <= x + off <= w + tw:
                    tree_fire(d, x + off, ridge[int(x) % w] + 8, hh, tw * r.uniform(0.8, 1.2), ss, body, r.random() < fl, r)
    arr = np.asarray(im.resize((w, h), Image.LANCZOS)).astype(float) / 255.0
    grain = 0.9 + 0.2 * fbm(h, w, beta=0.9, seed=919)
    arr[..., :3] = np.clip(arr[..., :3] * grain[..., None], 0, 1)
    G.save("fr_hills", arr)


def fr_ground():
    s = 2
    n = T * s
    dirt = scorch(np.dstack([G.dirt_tex(s), np.ones((n, n))]), 944, glow=0.9, dark=(18, 10, 8), mid=(70, 36, 24), light=(130, 70, 40), thr=0.6)[..., :3]
    r = np.random.default_rng(944)
    edge = 32 + 12 * (fbm1(n, beta=2.0, seed=47) - 0.5)
    yy = np.arange(n)[:, None].astype(float)
    yb = edge[None, :] * s
    fine = fbm(n, n, beta=0.8, seed=947)
    ash = fbm(n, n, beta=2.2, seed=948)
    top = lerp(col((38, 30, 26)), col((92, 74, 58)), np.clip(0.5 + 0.9 * (ash - 0.5) + 0.3 * (fine - 0.5), 0, 1)[..., None])
    # braci sparse sull'erba bruciata
    spark = (r.random((n, n)) > 0.9975).astype(float)
    top = np.clip(top + ndi.gaussian_filter(spark, 1.0)[..., None] * col((255, 120, 20)) * 6.0, 0, 1)
    a = np.clip((yb - yy) / 1.6, 0, 1)
    shadow = 1 - 0.5 * np.exp(-np.clip(yy - yb, 0, None) / (5.0 * s)) * (1 - a)
    rgb = dirt * shadow[..., None]
    rgb = top * a[..., None] + rgb * (1 - a[..., None])
    t = down(rgb, s)
    dt = down(dirt, s)
    G.save("fr_grass", t)
    G.save("fr_dirt", dt)
    for nm in ("l", "r"):
        G.save("fr_grass_" + nm, A.side_variant(t, nm, True))
        G.save("fr_dirt_" + nm, A.side_variant(dt, nm, False))
    # frangia: ciuffi d'erba carbonizzati con qualche fiammella
    fw, fh = T, 28
    cv = Cv(fw, fh, 3)
    for _ in range(26):
        x = r.uniform(0, fw)
        hgt = r.uniform(8, 22)
        lean = r.uniform(-4, 4)
        for off in (-fw, 0, fw):
            cv.add(cv.poly([(x - 2 + off, fh), (x + 2 + off, fh), (x + lean + off, fh - hgt)]), (44, 34, 28), bevel=1.5, spec=0.1)
    for _ in range(6):
        x = r.uniform(0, fw)
        for off in (-fw, 0, fw):
            m = cv.ell(x + off, fh - 3, 2.5, 4)
            cv.glow(m, (255, 110, 20), radius=4, strength=0.8)
            cv.add(m, (255, 190, 60), bevel=1.5, spec=0.3)
    G.save("fr_fringe", cv.result())


def fr_blocks():
    n = T * 3
    cv = Cv(T, T, 3)
    tex = 0.8 + 0.3 * fbm(n, n, beta=1.6, seed=951)
    cv.add(cv.rrect(3, 3, T - 3, T - 3, 26), (92, 52, 38), bevel=17, spec=0.25, shin=20, tex=tex)
    cv.add(cv.capsule(16, T * 0.52, T - 16, T * 0.52, 1.6), (255, 130, 30), bevel=1.2, spec=0.0)
    cv.add(cv.capsule(T * 0.5, 16, T * 0.5, T * 0.5 - 6, 1.4), (255, 130, 30), bevel=1.2, spec=0.0)
    cv.add(cv.capsule(22, 66, 40, 50, 2.0), (150, 96, 70), bevel=2, spec=0.4)
    G.save("fr_brick", cv.result())

    def crate(used):
        cv = Cv(T, T, 3)
        base = (72, 40, 40) if not used else (62, 52, 50)
        cv.add(cv.rrect(3, 3, T - 3, T - 3, 26), base, bevel=17, spec=0.2, shin=20, tex=0.85 + 0.2 * fbm(n, n, beta=1.5, seed=952))
        st = cv.poly(G.star_pts(48, 50, 24, 10))
        if used:
            cv.add(st, (30, 26, 26), ao=0.3, bevel=3, spec=0.0)
        else:
            cv.glow(st, (255, 140, 30), radius=9, strength=0.95)
            cv.add(st, (255, 200, 80), ao=0.5, bevel=6, spec=0.9, shin=16)
            cv.add(cv.ell(48, 50, 6, 6), (255, 255, 230), bevel=3, spec=0.5)
        return cv.result()
    G.save("fr_bonus", crate(False))
    G.save("fr_used", crate(True))

    cv = Cv(T, T, 3)       # piattaforma che crolla: legno bruciato spaccato, brace nelle fessure
    wood = 0.55 + 0.45 * fbm(n, n, beta=1.2, seed=971)
    cv.add(cv.rrect(3, 3, T - 3, T - 3, 14), (66, 44, 34), bevel=10, spec=0.1, tex=wood)
    for k in range(4):
        y = 12 + k * 22
        cv.add(cv.capsule(8, y, T - 8, y + 2, 1.5), (30, 20, 18), bevel=1.2, spec=0.0)
    for (x0, y0, x1, y1) in ((30, 8, 40, 40), (60, 50, 52, 88), (74, 14, 82, 36)):
        cv.add(cv.capsule(x0, y0, x1, y1, 1.8), (255, 120, 24), bevel=1.0, spec=0.0)
    cv.glow(cv.capsule(34, 12, 44, 36, 1.8), (255, 120, 24), radius=5, strength=0.5)
    G.save("fr_crumble", cv.result())

    cv = Cv(T, T, 3)
    tex = 0.6 + 0.4 * fbm(n, n, beta=1.9, seed=961) + 0.25 * (fbm(n, n, beta=0.5, seed=962) - 0.5)
    cv.add(cv.rrect(1.5, 1.5, T - 1.5, T - 1.5, 30), (70, 62, 66), bevel=16, spec=0.15, tex=tex)
    cv.add(cv.capsule(60, 40, 76, 62, 1.3), (255, 110, 20), bevel=1.0, spec=0.0)
    cv.add(cv.capsule(20, 70, 34, 56, 1.1), (255, 110, 20), bevel=1.0, spec=0.0)
    G.save("fr_stone", cv.result())


# ------------------------------------------------------------------ 3D
def burn3(src, dst, seed, glow=0.5, **kw):
    G.save(dst, scorch(load(src), seed, glow=glow, **kw))


def c_grass():
    burn3("r3_grass", "r3c_grass", 1001, 0.5, dark=(12, 10, 9), mid=(48, 40, 34), light=(98, 80, 58))


def c_forest():
    burn3("r3_forest", "r3c_forest", 1002, 0.6, dark=(10, 7, 7), mid=(40, 28, 22), light=(86, 56, 36))


def c_tuft():
    burn3("r3_tuft", "r3c_tuft", 1003, 0.0, dark=(22, 18, 14), mid=(76, 62, 38), light=(138, 110, 58))


def c_flowers():
    burn3("r3_flowers", "r3c_flowers", 1004, 0.0, dark=(30, 14, 10), mid=(150, 44, 20), light=(230, 130, 50))


def c_branch():
    burn3("r3_branch", "r3c_branch", 1005, 0.0, dark=(14, 10, 10), mid=(50, 36, 30), light=(120, 80, 40))


def c_tree():
    burn3("r3_firtree", "r3c_tree", 1006, 0.0, dark=(12, 9, 9), mid=(44, 30, 26), light=(110, 70, 36))


def c_sky():
    G.save("r3c_sky", sky_array(1024, 512, 1011))


def c_mountains():
    a = A.capture(G, G.gen_mountains) if False else load("r3_mountains")
    out = scorch(a, 1012, glow=0.0, dark=(30, 16, 16), mid=(96, 42, 26), light=(214, 110, 50))
    G.save("r3c_mountains", out)


def c_lava():
    """Lava per le casse/le bande luminose: crosta scura con crepe incandescenti (periodica)."""
    n = 256
    h = fbm(n, n, beta=2.0, seed=1021)
    crack = np.clip(1 - np.abs(h - 0.5) * 11, 0, 1) ** 1.8
    crust = lerp(col((18, 14, 14)), col((52, 38, 32)), np.clip(0.5 + 1.0 * (fbm(n, n, beta=1.2, seed=1022) - 0.5), 0, 1)[..., None])
    glow = lerp(col((220, 50, 10)), col((255, 210, 80)), crack[..., None])
    G.save("r3c_lava", crust * (1 - crack[..., None]) + glow * crack[..., None])


def c_ember():
    n = 64
    y, x = np.mgrid[0:n, 0:n].astype(float)
    r = np.sqrt((x - n / 2 + 0.5) ** 2 + (y - n / 2 + 0.5) ** 2) / (n / 2)
    a = np.clip(1 - r, 0, 1) ** 1.8
    rgb = np.stack([np.ones((n, n)), 0.55 + 0.4 * (1 - r).clip(0, 1), 0.12 + 0.5 * (1 - r).clip(0, 1) ** 3], -1)
    G.save("r3c_ember", np.dstack([rgb, a]))


def c_flame():
    """Fiamma per gli alberi in fiamme: striscia verticale con cuore giallo e bordo rosso."""
    w, h = 64, 128
    y, x = np.mgrid[0:h, 0:w].astype(float)
    u = (x - w / 2 + 0.5) / (w / 2)
    v = y / h
    wd = (0.15 + 0.85 * v) ** 0.8 * (1 - 0.15 * np.sin(v * 14 + 1.0))
    a = np.clip(1 - np.abs(u) / np.clip(wd, 0.05, 1), 0, 1) ** 0.7 * np.clip(v * 8, 0, 1) * np.clip((1 - v) * 1.4 + 0.1, 0, 1)
    a = np.clip(a * (0.85 + 0.3 * fbm(h, w, beta=1.2, seed=1031)), 0, 1)
    core = np.clip(a * 1.4 - 0.4, 0, 1)
    rgb = np.stack([np.ones_like(a), 0.25 + 0.7 * core, 0.05 + 0.5 * core ** 2], -1)
    G.save("r3c_flame", np.dstack([rgb, a]))


def vent2d():
    """Bocca di un getto di fuoco, vista di lato: griglia scura incassata con fessure incandescenti."""
    w, h = 96, 40
    cv = Cv(w, h, 3)
    cv.add(cv.rrect(8, 14, w - 8, h - 2, 10), (36, 28, 28), bevel=5, spec=0.2)
    for k in range(5):
        x = 20 + k * 14
        cv.glow(cv.rrect(x, 18, x + 6, h - 6, 3), (255, 110, 20), radius=6, strength=0.9)
        cv.add(cv.rrect(x, 18, x + 6, h - 6, 3), (255, 170, 50), bevel=1, spec=0.0)
    cv.add(cv.ell(w / 2, 14, 38, 6), (24, 18, 18), bevel=2, spec=0.1)
    G.save("fr_vent", cv.result())


def vent3d():
    """Bocca di un getto vista dall'alto: anello di roccia nera e centro incandescente con raggi di brace."""
    n = 128
    y, x = np.mgrid[0:n, 0:n].astype(float)
    r = np.sqrt((x - n / 2 + 0.5) ** 2 + (y - n / 2 + 0.5) ** 2) / (n / 2)
    ang = np.arctan2(y - n / 2 + 0.5, x - n / 2 + 0.5)
    rays = 0.5 + 0.5 * np.sin(ang * 7 + 3 * fbm(n, n, beta=1.5, seed=1041))
    core = np.clip(1 - r / 0.45, 0, 1) ** 0.8
    ring = np.clip(1 - np.abs(r - 0.62) / 0.16, 0, 1)
    glow = np.clip(core + 0.5 * ring * rays + 0.35 * np.clip(1 - np.abs(r - 0.82) / 0.1, 0, 1) * rays, 0, 1)
    rgb = np.zeros((n, n, 3))
    rock = lerp(col((6, 4, 4)), col((26, 18, 16)), np.clip(0.5 + fbm(n, n, beta=1.6, seed=1042) - 0.5, 0, 1)[..., None])
    hot = lerp(col((200, 36, 4)), col((255, 150, 30)), (core ** 1.5)[..., None])
    rgb = rock * (1 - glow[..., None]) + hot * glow[..., None]
    a = np.clip((0.97 - r) * 8, 0, 1)
    G.save("r3c_vent", np.dstack([np.clip(rgb, 0, 1), a]))


GENS = {
    "vent2d": vent2d, "vent3d": vent3d,
    "fr_sky": fr_sky, "fr_mountains": fr_mountains, "fr_hills": fr_hills, "fr_ground": fr_ground, "fr_blocks": fr_blocks,
    "c_grass": c_grass, "c_forest": c_forest, "c_tuft": c_tuft, "c_flowers": c_flowers, "c_branch": c_branch, "c_tree": c_tree,
    "c_sky": c_sky, "c_mountains": c_mountains, "c_lava": c_lava, "c_ember": c_ember, "c_flame": c_flame,
}

if __name__ == "__main__":
    only = sys.argv[1:] or list(GENS)
    for k in only:
        GENS[k]()
        print("ok", k, flush=True)
