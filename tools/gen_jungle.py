#!/usr/bin/env python3
"""Immagini dell'isola tropicale (livelli 3D 35, 36, 37): giungla, spiaggia, tempio sepolto, fiume notturno, e le casse speciali.

Uso:  python3 tools/gen_jungle.py [nome ...]      (nessun nome = tutto)
Richiede numpy, scipy, Pillow e gli script gen_assets.py / gen_assets3d.py / gen_autumn.py accanto a questo file.
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFont
from scipy import ndimage as ndi

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_assets3d as G3  # noqa: E402
import gen_autumn as A  # noqa: E402
from gen_assets import fbm, lerp  # noqa: E402


def col(c):
    return np.array(c, dtype=float) / 255.0


def tonemap(img, mul, add=0.0):
    out = img.copy()
    out[..., :3] = np.clip(img[..., :3] * np.array(mul) + add, 0, 1)
    return out


# ---------------------------------------------------------------- terreno e fondali

def j_ground():
    g = A.capture(G3, G3.gen_grass)
    f = A.capture(G3, G3.gen_forest_floor)
    g = tonemap(g, (0.62, 1.12, 0.52), 0.01)            # erba tropicale: verde profondo e saturo
    f = tonemap(f, (0.55, 0.92, 0.48))
    mg, mf = g[..., :3].mean((0, 1)), f[..., :3].mean((0, 1))
    m = (mg + mf) / 2
    G3.save("r3j_grass", np.clip(g[..., :3] * (m / mg), 0, 1))
    G3.save("r3j_forest", np.clip(f[..., :3] * (m / mf), 0, 1))


def j_tuft():
    t = A.capture(G3, G3.gen_tuft)
    G3.save("r3j_tuft", np.dstack([tonemap(t, (0.6, 1.1, 0.5))[..., :3], t[..., 3]]))


def j_flowers():
    kinds = [((255, 60, 80), (255, 220, 60)), ((255, 140, 20), (255, 236, 120)), ((250, 70, 170), (255, 240, 150)),
             ((255, 230, 60), (220, 90, 10)), ((255, 255, 255), (255, 190, 30)), ((200, 40, 200), (255, 220, 90))]
    G3.gen_flowers("r3j_flowers", kinds, stem=(30, 110, 30, 255), leaf=(26, 130, 40, 255))


def j_sky():
    c = G3.col
    G3.gen_sky("r3j_sky", (c((20, 110, 210)), c((90, 186, 236)), c((214, 240, 236))), (1.0, 0.96, 0.8))


def j_dusk():
    c = G3.col
    G3.gen_sky("r3j_dusk", (c((60, 44, 120)), c((214, 104, 86)), c((255, 196, 120))), (1.0, 0.62, 0.3))


def j_night():
    c = G3.col
    sky = A.capture(G3, G3.gen_sky, "x", (c((6, 10, 30)), c((18, 34, 70)), c((40, 70, 90))), (0.5, 0.6, 0.9))
    sky = sky[..., :3] * np.array([0.55, 0.62, 0.8])
    r = np.random.default_rng(3701)
    h, w = sky.shape[:2]
    for _ in range(900):                                 # stelle
        x, y = int(r.uniform(0, w)), int(r.uniform(0, h * 0.7))
        b = r.uniform(0.4, 1.0)
        sky[y, x] = np.maximum(sky[y, x], b)
    yy, xx = np.mgrid[0:h, 0:w]
    d = np.sqrt(((xx - 0.22 * w) / w) ** 2 + ((yy - 0.2 * h) / h) ** 2 * 1.6)
    moon = np.clip((0.035 - d) * 400, 0, 1)
    sky = sky * (1 - moon[..., None]) + col((250, 246, 220)) * moon[..., None] + (np.exp(-d * 9) * 0.25)[..., None] * col((180, 200, 255))
    G3.save("r3j_night", np.clip(sky, 0, 1))


def j_mountains():
    """Isole vulcaniche lontane ricoperte di giungla, con la foschia del mare."""
    a = A.capture(G3, G3.gen_mountains)
    h = a.shape[0]
    f = (np.linspace(0, 1, h)[:, None, None]) ** 1.4
    rgb = a[..., :3] * np.array([0.55, 0.95, 0.6])
    rgb = lerp(rgb, col((150, 210, 200)), 0.15 + 0.35 * f)
    G3.save("r3j_mountains", np.dstack([np.clip(rgb, 0, 1), a[..., 3]]))


def j_sand():
    """Sabbia bagnata e terra rossiccia per le pareti dei burroni e la spiaggia."""
    n = 256
    h = fbm(n, n, beta=2.0, seed=3711)
    h2 = fbm(n, n, beta=1.2, seed=3712)
    rgb = lerp(col((176, 132, 84)), col((226, 196, 140)), np.clip(0.4 + 1.2 * (h - 0.5), 0, 1)[..., None])
    rgb = rgb * (0.9 + 0.2 * h2)[..., None]
    G3.save("r3j_sand", np.clip(rgb, 0, 1))


def j_water():
    """Acqua tropicale: turchese con riflessi; versione scura per la notte si ottiene tingendo in gioco."""
    n = 256
    h = fbm(n, n, beta=2.2, seed=3721)
    h2 = fbm(n, n, beta=1.4, seed=3722)
    base = lerp(col((10, 120, 140)), col((60, 210, 200)), np.clip(0.5 + 1.1 * (h - 0.5), 0, 1)[..., None])
    glint = np.clip(1 - np.abs(h2 - 0.5) * 9, 0, 1) ** 2
    G3.save("r3j_water", np.clip(base + glint[..., None] * 0.3, 0, 1))


def j_temple():
    """Pietra del tempio: blocchi squadrati con un fregio inciso, muschio negli incavi."""
    n = 256
    h = fbm(n, n, beta=2.4, seed=3731)
    rgb = lerp(col((96, 92, 76)), col((170, 162, 132)), np.clip(0.3 + 1.2 * (h - 0.5), 0, 1)[..., None])
    im = Image.fromarray((np.clip(rgb, 0, 1) * 255).astype(np.uint8))
    d = ImageDraw.Draw(im)
    dark = (58, 54, 44)
    d.rectangle([4, 4, n - 5, n - 5], outline=dark, width=6)
    d.rectangle([40, 40, n - 41, n - 41], outline=dark, width=5)
    for k in range(4):                                       # spirale a gradini al centro
        o = 64 + k * 16
        d.rectangle([o, o, n - o, n - o], outline=dark if k % 2 == 0 else (120, 114, 92), width=4)
    arr = np.asarray(im).astype(float) / 255.0
    moss = np.clip((fbm(n, n, beta=1.8, seed=3732) - 0.55) * 4, 0, 1)
    arr = arr * (1 - 0.6 * moss[..., None]) + col((60, 110, 40)) * 0.6 * moss[..., None]
    G3.save("r3j_temple", np.clip(arr, 0, 1))


# ---------------------------------------------------------------- vegetazione (si tinge in gioco)

def leaf_card(W, H, region, count, seed, size, name, trunk=None):
    """Scheda di grandi foglie lanceolate, con nervatura e ombre interne."""
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
        L = r.uniform(*size)
        a = r.uniform(0, math.pi * 2)
        k = r.uniform(0.75, 1.15) * (0.85 + 0.3 * (1 - y / H))
        c = (int(min(255, 60 * k)), int(min(255, 150 * k)), int(min(255, 54 * k)), 255)
        ca, sa = math.cos(a), math.sin(a)
        wv = L * 0.32
        poly = [(x, y), (x + ca * L * 0.5 - sa * wv, y + sa * L * 0.5 + ca * wv), (x + ca * L, y + sa * L),
                (x + ca * L * 0.5 + sa * wv, y + sa * L * 0.5 - ca * wv)]
        d.polygon([(px * ss, py * ss) for px, py in poly], fill=c)
        d.line([(x * ss, y * ss), ((x + ca * L) * ss, (y + sa * L) * ss)], fill=(int(c[0] * 0.6), int(c[1] * 0.7), int(c[2] * 0.6), 255), width=ss)
    arr = np.asarray(im).astype(float) / 255.0
    dens = ndi.gaussian_filter(arr[..., 3], 7 * ss)
    arr[..., :3] *= (0.8 + 0.3 * (1 - np.clip(dens * 1.2, 0, 1)))[..., None]
    G3.finish_card(Image.fromarray((np.clip(arr, 0, 1) * 255).astype(np.uint8), "RGBA"), name, ss)


def j_crown():
    leaf_card(256, 256, lambda x, y, r: ((x - 128) / 118) ** 2 + ((y - 128) / 112) ** 2 < 1.0, 260, 3741, (26, 46), "r3j_crown")


def j_tree():
    def trunk(d, ss):
        d.polygon([(120 * ss, 512 * ss), (136 * ss, 512 * ss), (132 * ss, 250 * ss), (124 * ss, 250 * ss)], fill=(86, 66, 46, 255))
    leaf_card(256, 512, lambda x, y, r: ((x - 128) / 122) ** 2 + ((y - 180) / 160) ** 2 < 1.0 and y < 320, 560, 3742, (26, 50), "r3j_tree", trunk)


def j_foliage():
    """Mosaico di foglie senza trasparenza, ripetibile, per le chiome tridimensionali."""
    W, ss = 256, 2
    r = np.random.default_rng(3743)
    im = Image.new("RGB", (W * ss, W * ss), (40, 96, 36))
    d = ImageDraw.Draw(im)
    for _ in range(700):
        x, y = r.uniform(0, W), r.uniform(0, W)
        L, a, k = r.uniform(18, 34), r.uniform(0, 6.28), r.uniform(0.7, 1.2)
        c = (int(60 * k), int(150 * k), int(52 * k))
        ca, sa, wv = math.cos(a), math.sin(a), 0.3
        for ox in (-W, 0, W):
            for oy in (-W, 0, W):
                px, py = x + ox, y + oy
                if -L <= px <= W + L and -L <= py <= W + L:
                    poly = [(px, py), (px + ca * L * 0.5 - sa * L * wv, py + sa * L * 0.5 + ca * L * wv), (px + ca * L, py + sa * L),
                            (px + ca * L * 0.5 + sa * L * wv, py + sa * L * 0.5 - ca * L * wv)]
                    d.polygon([(q[0] * ss, q[1] * ss) for q in poly], fill=c)
    out = np.asarray(im.resize((W, W), Image.LANCZOS)).astype(float) / 255.0
    G3.save("r3j_foliage", out)


def j_frond():
    """Fronda di palma: rachide lungo l'asse orizzontale (attacco a sinistra) e foglioline pendenti ai due lati."""
    ss = 2
    W, H = 512, 128
    im = Image.new("RGBA", (W * ss, H * ss), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    r = np.random.default_rng(3751)
    cy = H * 0.5
    for i in range(46):
        t = i / 46
        x = 14 + t * (W - 30)
        L = (1 - t * 0.75) * 58 * r.uniform(0.85, 1.1)
        for s in (-1, 1):
            k = r.uniform(0.8, 1.1)
            c = (int(70 * k), int(160 * k), int(50 * k), 255)
            ex, ey = x + L * 0.55, cy + s * L * 0.85
            d.polygon([(x * ss, cy * ss), ((x + 6) * ss, (cy + s * 3) * ss), (ex * ss, ey * ss), ((ex - 5) * ss, (ey - s * 2) * ss)], fill=c)
    d.line([(10 * ss, cy * ss), ((W - 10) * ss, cy * ss)], fill=(110, 120, 50, 255), width=4 * ss)
    G3.finish_card(im, "r3j_frond", ss)


# ---------------------------------------------------------------- casse

def crate_base(seed, plank=(196, 136, 70), edge=(120, 74, 34)):
    n = 128
    im = Image.new("RGB", (n, n), plank)
    d = ImageDraw.Draw(im)
    r = np.random.default_rng(seed)
    for y in range(0, n, 32):                                    # assi con venature
        k = r.uniform(0.88, 1.08)
        d.rectangle([0, y, n, y + 31], fill=tuple(int(min(255, c * k)) for c in plank))
        d.line([(0, y), (n, y)], fill=edge, width=2)
        for _ in range(6):
            yy = y + r.uniform(4, 28)
            d.line([(0, yy), (n, yy + r.uniform(-3, 3))], fill=tuple(int(c * 0.86) for c in plank), width=1)
    d.rectangle([0, 0, n - 1, n - 1], outline=edge, width=12)    # cornice
    d.line([(10, 10), (n - 10, n - 10)], fill=edge, width=9)     # traversa diagonale
    for (x, y) in ((8, 8), (n - 9, 8), (8, n - 9), (n - 9, n - 9)):
        d.ellipse([x - 3, y - 3, x + 3, y + 3], fill=(70, 70, 76))
    return im, d


def font(size):
    for f in ("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", "/usr/share/fonts/dejavu/DejaVuSans-Bold.ttf"):
        if os.path.exists(f):
            return ImageFont.truetype(f, size)
    return ImageFont.load_default()


def label(d, text, size, fill, stroke=(0, 0, 0)):
    f = font(size)
    w = d.textlength(text, font=f)
    d.text((64 - w / 2, 64 - size * 0.62), text, font=f, fill=fill, stroke_width=3, stroke_fill=stroke)


def save_img(im, name):
    G3.save(name, np.asarray(im).astype(float) / 255.0)


def j_crates():
    im, d = crate_base(3761)
    save_img(im, "r3j_crate")
    im, d = crate_base(3762, plank=(226, 92, 30), edge=(110, 40, 14))   # esplosiva: si accende saltandoci sopra, scoppia dopo tre secondi
    d.rectangle([20, 44, 108, 84], fill=(250, 210, 60))
    label(d, "TNT", 34, (170, 20, 10), (250, 210, 60))
    save_img(im, "r3j_tnt")
    im, d = crate_base(3763, plank=(80, 210, 70), edge=(20, 90, 30))    # instabile: scoppia appena la si tocca
    d.ellipse([34, 34, 94, 94], fill=(16, 60, 20))
    label(d, "!", 50, (180, 255, 120), (16, 60, 20))
    save_img(im, "r3j_nitro")
    im, d = crate_base(3764, plank=(206, 160, 90))                      # a molla: rimbalza in alto, non si rompe
    d.polygon([(64, 18), (104, 64), (80, 64), (80, 108), (48, 108), (48, 64), (24, 64)], fill=(250, 240, 220), outline=(90, 60, 30))
    save_img(im, "r3j_bounce")
    im, d = crate_base(3765, plank=(70, 130, 220), edge=(20, 50, 110))  # traguardo intermedio: si riparte da qui
    label(d, "C", 64, (255, 255, 255), (20, 50, 110))
    save_img(im, "r3j_check")


GENS = {"j_ground": j_ground, "j_tuft": j_tuft, "j_flowers": j_flowers, "j_sky": j_sky, "j_dusk": j_dusk, "j_night": j_night,
        "j_mountains": j_mountains, "j_sand": j_sand, "j_water": j_water, "j_temple": j_temple, "j_crown": j_crown, "j_tree": j_tree,
        "j_foliage": j_foliage, "j_frond": j_frond, "j_crates": j_crates}

if __name__ == "__main__":
    names = sys.argv[1:] or list(GENS)
    for n in names:
        GENS[n]()
        print("ok", n)
