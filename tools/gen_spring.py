#!/usr/bin/env python3
"""Immagini dei livelli di primavera: cielo tenue, colline con ciliegi in fiore, prati fioriti, acqua, fiori a molla e rovi (2D e 3D).

Uso:  python3 tools/gen_spring.py [nome ...]      (nessun nome = tutto)
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

PINKS = [(255, 196, 214), (250, 170, 200), (255, 214, 226), (244, 150, 186), (255, 232, 240)]


def col(c):
    return np.array(c, dtype=float) / 255.0


def circle(d, x, y, r, fill):
    d.ellipse([x - r, y - r, x + r, y + r], fill=fill)


# ------------------------------------------------------------------ 2D: sfondi
def sp_sky():
    w, h = 1536, 768
    y = np.linspace(0, 1, h)[:, None]
    x = np.linspace(0, 1, w, endpoint=False)[None, :]
    top, mid, hor = col((70, 150, 228)), col((150, 205, 244)), col((255, 238, 214))
    t = y ** 0.9
    grad = np.where(t[..., None] < 0.62, lerp(top, mid, (t / 0.62)[..., None]), lerp(mid, hor, ((t - 0.62) / 0.38)[..., None]))
    sky = np.broadcast_to(grad, (h, w, 3)).copy()
    r = np.sqrt(((y - 0.2) * 1.0) ** 2 + ((x - 0.25) * 2.0) ** 2)
    sky += col((255, 240, 190)) * (np.exp(-r * 5) * 0.32)[..., None]
    den = ndi.gaussian_filter(fbm(h, w, beta=3.4, seed=1101, ax=2.4, ay=1.0), (3, 3), mode="wrap")
    den2 = ndi.gaussian_filter(fbm(h, w, beta=2.6, seed=1102, ax=1.6, ay=1.0), (2, 2), mode="wrap")
    c = smooth((den + 0.25 * (den2 - 0.5) - (0.56 - 0.1 * y)) * 5.0) * (0.35 + 0.65 * y)
    dn = ndi.gaussian_filter(den, 6)
    lit = np.clip(0.95 + (np.roll(dn, 8, 1) - np.roll(dn, -8, 1)) * 14 + (den2 - 0.5) * 0.2, 0.8, 1.1)
    cloud = lerp(col((212, 226, 244)), col((255, 255, 253)), np.clip((lit - 0.8) / 0.3, 0, 1)[..., None])
    a = np.clip(c, 0, 0.95)[..., None]
    sky = sky * (1 - a) + cloud * a
    G.save("sp_sky", np.clip(sky, 0, 1))


def sp_mountains():
    a = A.capture(G, G.gen_mountains)
    rgb = a[..., :3]
    h = rgb.shape[0]
    f = (np.linspace(0, 1, h)[:, None, None]) ** 1.6
    rgb = lerp(rgb * np.array([0.98, 1.04, 1.1]), col((196, 220, 238)), 0.18 + 0.3 * f)
    G.save("sp_mountains", np.dstack([np.clip(rgb, 0, 1), a[..., 3]]))


def blossom_tree(d, x, base, h, ss, r, kind):
    """Albero con la chioma fatta di tanti cerchi: ciliegio in fiore (rosa/bianco) o albero verde tenero."""
    d.rectangle([(x - h * 0.04) * ss, (base - h * 0.45) * ss, (x + h * 0.04) * ss, (base + 2) * ss], fill=(92, 64, 52, 255))
    if kind == "pink":
        pal = PINKS
    else:
        pal = [(122, 190, 84), (150, 208, 96), (100, 170, 78), (176, 220, 110)]
    cx, cy = x, base - h * 0.62
    for k in range(9):
        ox = r.uniform(-h * 0.3, h * 0.3)
        oy = r.uniform(-h * 0.2, h * 0.18)
        rad = h * r.uniform(0.17, 0.27)
        c = pal[int(r.integers(len(pal)))]
        shade = 0.9 + 0.1 * (oy / (h * 0.2))
        circle(d, (cx + ox) * ss, (cy + oy) * ss, rad * ss, tuple(int(min(255, v * shade)) for v in c) + (255,))
    for k in range(6):                                  # luci sulla chioma
        ox = r.uniform(-h * 0.25, h * 0.2)
        oy = r.uniform(-h * 0.3, -h * 0.05)
        circle(d, (cx + ox) * ss, (cy + oy) * ss, h * 0.07 * ss, (255, 246, 250, 255) if kind == "pink" else (214, 238, 150, 255))


def sp_hills():
    h, w, ss = 560, 2048, 2
    im = Image.new("RGBA", (w * ss, h * ss), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    r = np.random.default_rng(1111)
    layers = [(70, 60, 54, (150, 202, 168, 255), 0.35, 26), (160, 90, 80, (98, 176, 90, 255), 0.55, 24), (270, 120, 108, (70, 152, 62, 255), 0.5, 20)]
    for y0, amp, th, body, pinkp, count in layers:
        ridge = y0 + amp * fbm1(w, beta=2.2, seed=int(y0) + 5)
        d.polygon([(x * ss, ridge[min(x, w - 1)] * ss) for x in list(range(0, w, 8)) + [w]] + [(w * ss, h * ss), (0, h * ss)], fill=body)
        for _ in range(count * 2):
            x = r.uniform(0, w)
            hh = th * r.uniform(0.7, 1.3)
            kind = "pink" if r.random() < pinkp else "green"
            for off in (-w, 0, w):
                if -hh <= x + off <= w + hh:
                    b = ridge[int(x) % w] + 6
                    if body[0] > 120:                    # piano lontano: alberi velati
                        blossom_tree(d, x + off, b, hh * 0.8, ss, r, kind)
                    else:
                        blossom_tree(d, x + off, b, hh, ss, r, kind)
    arr = np.asarray(im.resize((w, h), Image.LANCZOS)).astype(float) / 255.0
    grain = 0.94 + 0.12 * fbm(h, w, beta=0.9, seed=1119)
    arr[..., :3] = np.clip(arr[..., :3] * grain[..., None], 0, 1)
    # velo azzurro sui piani lontani: più chiaro verso l'alto
    yy = np.linspace(0, 1, h)[:, None, None]
    arr[..., :3] = lerp(arr[..., :3], col((190, 222, 240)), np.clip(0.38 - yy * 0.45, 0, 0.38))
    G.save("sp_hills", arr)


def sp_ground():
    s = 2
    n = T * s
    r = np.random.default_rng(1131)
    dirt = np.clip(G.dirt_tex(s) * np.array([1.04, 1.0, 0.94]), 0, 1)
    edge = 30 + 8 * (fbm1(n, beta=2.0, seed=1132) - 0.5)
    yy = np.arange(n)[:, None].astype(float)
    yb = edge[None, :] * s
    p = fbm(n, n, beta=2.0, seed=1133)
    q = fbm(n, n, beta=0.8, seed=1134)
    top = lerp(col((54, 142, 38)), col((132, 206, 64)), np.clip(0.5 + 1.0 * (p - 0.5) + 0.4 * (q - 0.5), 0, 1)[..., None])
    a = np.clip((yb - yy) / 1.6, 0, 1)
    shadow = 1 - 0.5 * np.exp(-np.clip(yy - yb, 0, None) / (5.0 * s)) * (1 - a)
    rgb = dirt * shadow[..., None]
    rgb = top * a[..., None] + rgb * (1 - a[..., None])
    im = Image.fromarray((np.clip(rgb, 0, 1) * 255).astype(np.uint8), "RGB")
    d = ImageDraw.Draw(im)
    for _ in range(26):                                 # fiorellini nel prato
        x = r.uniform(0, n)
        y = r.uniform(4, edge.mean() * s - 8)
        c = [(255, 255, 255), (255, 214, 70), (255, 150, 190), (190, 150, 255)][int(r.integers(4))]
        for ox in (-n, 0, n):
            circle(d, x + ox, y, 3.0, c)
            circle(d, x + ox, y, 1.2, (240, 190, 50))
    rgb = np.asarray(im).astype(float) / 255.0
    t = down(rgb, s)
    dt = down(dirt, s)
    G.save("sp_grass", t)
    G.save("sp_dirt", dt)
    for nm in ("l", "r"):
        G.save("sp_grass_" + nm, A.side_variant(t, nm, True))
        G.save("sp_dirt_" + nm, A.side_variant(dt, nm, False))
    fw, fh = T, 28
    cv = Cv(fw, fh, 3)
    for _ in range(30):
        x = r.uniform(0, fw)
        hgt = r.uniform(9, 22)
        lean = r.uniform(-4, 4)
        c = [(70, 160, 46), (96, 186, 58), (58, 140, 40)][int(r.integers(3))]
        for off in (-fw, 0, fw):
            cv.add(cv.poly([(x - 2 + off, fh), (x + 2 + off, fh), (x + lean + off, fh - hgt)]), c, bevel=1.5, spec=0.15)
    for _ in range(5):
        x = r.uniform(4, fw - 4)
        petal = [(255, 255, 255), (255, 150, 190), (255, 220, 70)][int(r.integers(3))]
        for off in (-fw, 0, fw):
            cv.add(cv.ell(x + off, fh - 14, 4.2, 4.2), petal, bevel=2, spec=0.2)
            cv.add(cv.ell(x + off, fh - 14, 1.6, 1.6), (245, 190, 40), bevel=1, spec=0.1)
    G.save("sp_fringe", cv.result())


def sp_blocks():
    n = T * 3
    cv = Cv(T, T, 3)
    tex = 0.86 + 0.2 * fbm(n, n, beta=1.6, seed=1151)
    cv.add(cv.rrect(3, 3, T - 3, T - 3, 26), (226, 150, 116), bevel=17, spec=0.2, shin=20, tex=tex)
    cv.add(cv.capsule(14, T * 0.5, T - 14, T * 0.5, 1.6), (170, 100, 78), bevel=1.2, spec=0.0)
    cv.add(cv.capsule(T * 0.5, 14, T * 0.5, T * 0.5 - 4, 1.4), (170, 100, 78), bevel=1.2, spec=0.0)
    for k in range(5):
        a = k * 2 * math.pi / 5
        cv.add(cv.ell(30 + math.cos(a) * 5, 70 + math.sin(a) * 5, 4.2, 4.2), (255, 205, 222), bevel=2, spec=0.3)
    cv.add(cv.ell(30, 70, 2.4, 2.4), (250, 200, 70), bevel=1, spec=0.1)
    G.save("sp_brick", cv.result())

    def crate(used):
        cv = Cv(T, T, 3)
        base = (250, 212, 96) if not used else (178, 190, 170)
        cv.add(cv.rrect(3, 3, T - 3, T - 3, 26), base, bevel=17, spec=0.25, shin=20, tex=0.9 + 0.15 * fbm(n, n, beta=1.5, seed=1152))
        st = cv.poly(G.star_pts(48, 50, 24, 10))
        if used:
            cv.add(st, (112, 126, 110), ao=0.3, bevel=3, spec=0.0)
        else:
            cv.glow(st, (255, 120, 170), radius=9, strength=0.8)
            cv.add(st, (255, 120, 170), ao=0.5, bevel=6, spec=0.9, shin=16)
            cv.add(cv.ell(48, 50, 6, 6), (255, 240, 244), bevel=3, spec=0.5)
        return cv.result()
    G.save("sp_bonus", crate(False))
    G.save("sp_used", crate(True))

    cv = Cv(T, T, 3)
    tex = 0.62 + 0.4 * fbm(n, n, beta=1.9, seed=1161) + 0.2 * (fbm(n, n, beta=0.5, seed=1162) - 0.5)
    cv.add(cv.rrect(1.5, 1.5, T - 1.5, T - 1.5, 30), (150, 156, 170), bevel=16, spec=0.15, tex=tex)
    for (x, y, rx, ry) in ((24, 18, 15, 7), (70, 80, 18, 8), (80, 30, 9, 5)):
        cv.add(cv.ell(x, y, rx, ry), (96, 164, 70), bevel=3, spec=0.1)
    G.save("sp_stone", cv.result())


# ------------------------------------------------------------------ 2D: oggetti nuovi
def sp_spring():
    """Fiore a molla: stelo a spirale (molla) con un grande fiore rosa e giallo come piano di rimbalzo."""
    cv = Cv(T, T, 3)
    cv.add(cv.rrect(0, 36, T, T, 5), (74, 150, 56), bevel=9, spec=0.2)          # gambo/bulbo verde
    for k in range(4):                                                                          # spirali della molla
        y = 50 + k * 11
        cv.add(cv.capsule(8, y, T - 8, y + 4, 2.2), (236, 96, 150), bevel=2, spec=0.4)
    cv.add(cv.ell(T / 2, 24, 44, 17), (250, 120, 170), bevel=10, spec=0.35, shin=16)        # corolla
    for k in range(7):
        x = 14 + k * 11.5
        cv.add(cv.ell(x, 22, 8.5, 12), (255, 170, 205), bevel=4, spec=0.3)
    cv.add(cv.ell(T / 2, 18, 17, 9), (255, 214, 70), bevel=5, spec=0.5)
    cv.add(cv.ell(T / 2, 16, 8, 4), (255, 245, 170), bevel=2, spec=0.3)
    G.save("sp_spring", cv.result())


def sp_thorn():
    """Riccio di rovi: palla viola scura con spine bianche a raggiera, occhi arrabbiati e contorno scuro spesso: si legge subito come pericolo."""
    w, h, ss = 96, 76, 4
    im = Image.new("RGBA", (w * ss, h * ss), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    cx, cy, rb = w / 2, h - 27, 19
    dark = (30, 10, 40, 255)
    n = 19
    spikes = []
    for i in range(n):
        ang = math.pi * (-0.06 + 1.12 * i / (n - 1)) + math.pi        # dalla base sinistra, sopra, alla base destra
        ln = 17 if i % 2 == 0 else 12
        bx, by = cx + math.cos(ang) * rb * 0.85, cy + math.sin(ang) * rb * 0.85
        tx, ty = cx + math.cos(ang) * (rb + ln), cy + math.sin(ang) * (rb + ln)
        nx, ny = -math.sin(ang), math.cos(ang)
        spikes.append(((bx + nx * 4.6, by + ny * 4.6), (bx - nx * 4.6, by - ny * 4.6), (tx, ty)))
    for (p1, p2, p3) in spikes:                                       # spine: contorno scuro e interno bianco
        d.polygon([(q[0] * ss, q[1] * ss) for q in (p1, p2, p3)], fill=dark)
    circle(d, cx * ss, cy * ss, (rb + 2.4) * ss, dark)
    for (p1, p2, p3) in spikes:
        mx, my = (p1[0] + p2[0]) / 2, (p1[1] + p2[1]) / 2
        q1 = (mx + (p1[0] - mx) * 0.55, my + (p1[1] - my) * 0.55)
        q2 = (mx + (p2[0] - mx) * 0.55, my + (p2[1] - my) * 0.55)
        q3 = (mx + (p3[0] - mx) * 0.9, my + (p3[1] - my) * 0.9)
        d.polygon([(q1[0] * ss, q1[1] * ss), (q2[0] * ss, q2[1] * ss), (q3[0] * ss, q3[1] * ss)], fill=(250, 246, 232, 255))
    circle(d, cx * ss, cy * ss, rb * ss, (124, 40, 150, 255))
    circle(d, (cx - 5) * ss, (cy - 7) * ss, 7 * ss, (160, 72, 184, 255))
    for sx in (-1, 1):                                                # occhi arrabbiati
        ex = cx + sx * 7.2
        circle(d, ex * ss, (cy - 1) * ss, 4.6 * ss, (255, 255, 255, 255))
        circle(d, (ex - sx * 0.6) * ss, (cy + 0.2) * ss, 2.3 * ss, dark)
        d.line([((ex + sx * 5.6) * ss, (cy - 8.4) * ss), ((ex - sx * 4.2) * ss, (cy - 3.8) * ss)], fill=dark, width=int(2.2 * ss))
    d.rectangle([0, (h - 8) * ss, w * ss, h * ss], fill=(0, 0, 0, 0))   # appoggio netto sul terreno
    arr = np.asarray(im.resize((w, h), Image.LANCZOS)).astype(float) / 255.0
    G.save("sp_thorn", arr)


def sp_water():
    """Striscia d'acqua ripetibile in orizzontale: superficie ondulata chiara e profondità azzurro scuro."""
    w, h = 256, 96
    x = np.arange(w)[None, :].astype(float)
    y = np.arange(h)[:, None].astype(float)
    surf = 8 + 2.2 * np.sin(2 * np.pi * x / w * 3) + 1.4 * np.sin(2 * np.pi * x / w * 7 + 1.0)
    a = np.clip((y - surf) / 1.5, 0, 1)
    t = np.clip((y - surf) / 70.0, 0, 1)
    rgb = lerp(col((96, 200, 226)), col((28, 104, 168)), t[..., None])
    ripple = np.clip(np.sin(2 * np.pi * x / w * 5 + y * 0.35) * np.sin(2 * np.pi * x / w * 2 + y * 0.12) * 1.4 - 0.8, 0, 1)
    rgb = np.clip(rgb + ripple[..., None] * 0.22 * (1 - t[..., None]), 0, 1)
    foam = np.clip(1 - np.abs(y - surf - 2) / 3.0, 0, 1) * 0.65
    rgb = rgb * (1 - foam[..., None]) + col((240, 252, 255)) * foam[..., None]
    G.save("sp_water", np.dstack([rgb, a * (0.78 + 0.22 * t)]))


def sp_petal():
    n = 32
    y, x = np.mgrid[0:n, 0:n].astype(float)
    u = (x - n / 2 + 0.5) / (n / 2)
    v = (y - n / 2 + 0.5) / (n / 2)
    m = (u / 0.62) ** 2 + (v / 0.95) ** 2
    a = np.clip((1 - m) * 6, 0, 1)
    c = lerp(col((255, 222, 232)), col((246, 158, 190)), np.clip(0.5 + 0.6 * v, 0, 1)[..., None])
    G.save("sp_petal", np.dstack([c, a]))


# ------------------------------------------------------------------ 3D
def tonemap(arr, mul, add=0.0):
    return np.clip(arr[..., :3] * np.array(mul) + add, 0, 1)


def sprinkle(arr, count, seed, palette, rad=(2.0, 3.6), ss=2):
    """Piccoli fiori/petali sparsi su una texture ripetibile."""
    h, w = arr.shape[:2]
    im = Image.fromarray((np.clip(arr[..., :3], 0, 1) * 255).astype(np.uint8), "RGB").resize((w * ss, h * ss), Image.LANCZOS)
    d = ImageDraw.Draw(im)
    r = np.random.default_rng(seed)
    for _ in range(count):
        x, y = r.uniform(0, w), r.uniform(0, h)
        c = palette[int(r.integers(len(palette)))]
        rr = r.uniform(*rad)
        for ox in (-w, 0, w):
            for oy in (-h, 0, h):
                if -rr <= x + ox <= w + rr and -rr <= y + oy <= h + rr:
                    circle(d, (x + ox) * ss, (y + oy) * ss, rr * ss, c)
                    circle(d, (x + ox) * ss, (y + oy) * ss, rr * ss * 0.38, (246, 200, 60))
    out = np.asarray(im.resize((w, h), Image.LANCZOS)).astype(float) / 255.0
    return out


def d_ground():
    g = A.capture(G3, G3.gen_grass)
    f = A.capture(G3, G3.gen_forest_floor)
    g = tonemap(g, (1.12, 1.26, 0.8), 0.015)
    f = tonemap(f, (1.04, 1.2, 0.78), 0.01)
    g = sprinkle(g, 220, 1201, [(255, 255, 255), (255, 232, 96), (255, 170, 204), (206, 176, 255)])
    f = sprinkle(f, 260, 1202, [(255, 206, 222), (250, 180, 206), (255, 236, 244)], rad=(2.2, 3.4))
    mg, mf = g.mean((0, 1)), f.mean((0, 1))
    m = (mg + mf) / 2
    G3.save("r3d_grass", np.clip(g * (m / mg), 0, 1))
    G3.save("r3d_forest", np.clip(f * (m / mf), 0, 1))


def d_tuft():
    t = A.capture(G3, G3.gen_tuft)
    G3.save("r3d_tuft", np.dstack([tonemap(t, (1.1, 1.24, 0.8)), t[..., 3]]))


def d_flowers():
    kinds = [((255, 150, 190), (255, 236, 120)), ((255, 255, 255), (255, 205, 40)), ((255, 222, 70), (200, 120, 10)),
             ((196, 150, 255), (255, 240, 150)), ((255, 90, 120), (255, 230, 120)), ((120, 170, 255), (255, 240, 150)),
             ((255, 200, 224), (250, 190, 40)), ((255, 120, 70), (120, 60, 10)), ((240, 240, 255), (255, 210, 60))]
    G3.gen_flowers("r3d_flowers", kinds)


def d_sky():
    c = G3.col
    G3.gen_sky("r3d_sky", (c((60, 140, 226)), c((150, 204, 244)), c((255, 238, 214))), (1.0, 0.94, 0.72))


def d_mountains():
    a = A.capture(G3, G3.gen_mountains)
    h = a.shape[0]
    f = (np.linspace(0, 1, h)[:, None, None]) ** 1.5
    rgb = lerp(a[..., :3] * np.array([0.98, 1.05, 1.1]), col((200, 224, 240)), 0.2 + 0.28 * f)
    G3.save("r3d_mountains", np.dstack([np.clip(rgb, 0, 1), a[..., 3]]))


def blossom_card(W, H, region, count, seed, size, name, trunk=None):
    """Scheda di fiori chiari (si tinge in gioco, bianco-rosato di base) con ombre interne."""
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
        rad = r.uniform(*size)
        up = 1.0 - y / H
        k = r.uniform(0.9, 1.1) * (0.88 + 0.2 * up)
        c = (min(255, int(255 * k)), min(255, int(240 * k)), min(255, int(246 * k)), 255)
        circle(d, x * ss, y * ss, rad * ss, c)
    arr = np.asarray(im).astype(float) / 255.0
    a = arr[..., 3]
    dens = ndi.gaussian_filter(a, 7 * ss)
    arr[..., :3] *= (0.86 + 0.24 * (1 - np.clip(dens * 1.2, 0, 1)))[..., None]
    G3.finish_card(Image.fromarray((np.clip(arr, 0, 1) * 255).astype(np.uint8), "RGBA"), name, ss)


def d_crown():
    blossom_card(256, 256, lambda x, y, r: ((x - 128) / 118) ** 2 + ((y - 128) / 112) ** 2 < 1.0 and r.random() < 0.95 - 0.3 * (((x - 128) / 118) ** 2 + ((y - 128) / 112) ** 2),
                 700, 1221, (11, 19), "r3d_crown")


def d_tree():
    def trunk(d, ss):
        d.polygon([(118 * ss, 512 * ss), (138 * ss, 512 * ss), (133 * ss, 270 * ss), (123 * ss, 270 * ss)], fill=(96, 74, 54, 255))
        for (x0, y0, x1, y1) in ((128, 330, 72, 230), (128, 310, 190, 220), (128, 280, 128, 150)):
            d.line([(x0 * ss, y0 * ss), (x1 * ss, y1 * ss)], fill=(90, 68, 50, 255), width=5 * ss)
    blossom_card(256, 512, lambda x, y, r: ((x - 128) / 122) ** 2 + ((y - 190) / 170) ** 2 < 1.0 and y < 340 and r.random() < 0.97 - 0.25 * (((x - 128) / 122) ** 2 + ((y - 190) / 170) ** 2),
                 1700, 1231, (10, 18), "r3d_tree", trunk)


def d_foliage():
    """Mosaico di fiori senza trasparenza, ripetibile, per le chiome tridimensionali (si tinge in gioco)."""
    W, ss = 256, 2
    r = np.random.default_rng(1241)
    im = Image.new("RGB", (W * ss, W * ss), (226, 196, 212))
    d = ImageDraw.Draw(im)
    for _ in range(1700):
        x, y = r.uniform(0, W), r.uniform(0, W)
        rad = r.uniform(8, 15)
        k = r.uniform(0.86, 1.1)
        c = (min(255, int(255 * k)), min(255, int(238 * k)), min(255, int(244 * k)))
        for ox in (-W, 0, W):
            for oy in (-W, 0, W):
                if -rad <= x + ox <= W + rad and -rad <= y + oy <= W + rad:
                    circle(d, (x + ox) * ss, (y + oy) * ss, rad * ss, c)
    out = np.asarray(im.resize((W, W), Image.LANCZOS)).astype(float) / 255.0
    G3.save("r3d_foliage", np.dstack([out, np.ones((W, W))]))


def d_water():
    """Acqua vista dall'alto: azzurro profondo con riflessi chiari e increspature (periodica)."""
    n = 256
    h = fbm(n, n, beta=2.2, seed=1251)
    h2 = fbm(n, n, beta=1.4, seed=1252)
    base = lerp(col((24, 98, 150)), col((74, 176, 206)), np.clip(0.5 + 1.1 * (h - 0.5), 0, 1)[..., None])
    glint = np.clip(1 - np.abs(h2 - 0.5) * 9, 0, 1) ** 2
    rgb = np.clip(base + glint[..., None] * 0.28, 0, 1)
    G3.save("r3d_water", rgb)


def d_spring():
    """Fiore a molla visto dall'alto: anello verde, petali rosa attorno e centro giallo."""
    n = 128
    y, x = np.mgrid[0:n, 0:n].astype(float)
    u = (x - n / 2 + 0.5) / (n / 2)
    v = (y - n / 2 + 0.5) / (n / 2)
    r = np.sqrt(u * u + v * v)
    ang = np.arctan2(v, u)
    petal = np.clip(0.5 + 0.5 * np.sin(ang * 8), 0, 1)
    pr = 0.9 - 0.2 * (1 - petal)
    inside_p = np.clip((pr - r) * 14, 0, 1)
    rgb = np.zeros((n, n, 3))
    rgb[...] = col((74, 150, 56))
    pk = lerp(col((255, 140, 184)), col((255, 200, 222)), np.clip(r / 0.9, 0, 1)[..., None])
    rgb = rgb * (1 - inside_p[..., None]) + pk * inside_p[..., None]
    ctr = np.clip((0.34 - r) * 12, 0, 1)
    rgb = rgb * (1 - ctr[..., None]) + col((255, 214, 70)) * ctr[..., None]
    ring = np.clip((0.12 - np.abs(r - 0.5)) * 6, 0, 1) * 0.25 * inside_p
    rgb = np.clip(rgb - ring[..., None], 0, 1)
    a = np.clip((0.98 - r) * 10, 0, 1)
    G3.save("r3d_spring", np.dstack([rgb, a]))


GENS = {
    "sp_sky": sp_sky, "sp_mountains": sp_mountains, "sp_hills": sp_hills, "sp_ground": sp_ground, "sp_blocks": sp_blocks,
    "sp_spring": sp_spring, "sp_thorn": sp_thorn, "sp_water": sp_water, "sp_petal": sp_petal,
    "d_ground": d_ground, "d_tuft": d_tuft, "d_flowers": d_flowers, "d_sky": d_sky, "d_mountains": d_mountains,
    "d_crown": d_crown, "d_tree": d_tree, "d_foliage": d_foliage, "d_water": d_water, "d_spring": d_spring,
}

if __name__ == "__main__":
    only = sys.argv[1:] or list(GENS)
    for k in only:
        GENS[k]()
        print("ok", k, flush=True)
