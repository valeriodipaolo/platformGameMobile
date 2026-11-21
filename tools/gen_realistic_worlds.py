#!/usr/bin/env python3
"""Texture "realistiche stilizzate" per tutti i mondi 3D oltre all'estate: autunno, primavera, inverno (aurora), incendio,
giungla, deserto, fabbrica, miniera, cielo. Usa i generatori di gen_realistic.py con altre tavolozze, più qualche generatore
proprio (sabbia, neve, ghiaccio, lava, marmo) e un invecchiamento per le superfici costruite (metallo, cemento, templi).

Uso:  python3 tools/gen_realistic_worlds.py [mondo ...]      mondi: autumn spring aurora fire jungle desert factory mine heaven
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw
from scipy import ndimage as ndi

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_assets import fbm, lerp, lit_from_height, smooth, save  # noqa: E402
import gen_realistic as G  # noqa: E402
from gen_realistic import col, blur_wrap, desat  # noqa: E402

RES = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "res", "drawable-nodpi")
N = 512


def fog(r, g, b):
    return np.array([r, g, b])


ORIG = os.path.join(os.path.dirname(os.path.abspath(__file__)), "orig_textures")


def load(name):
    """Le texture da ritoccare (non da rigenerare) si leggono dall'originale messo da parte la prima volta: rilanciare lo
    script non accumula sporco e desaturazione."""
    src = os.path.join(ORIG, name + ".png")
    if not os.path.exists(src):
        os.makedirs(ORIG, exist_ok=True)
        Image.open(os.path.join(RES, name + ".png")).save(src)
    return np.asarray(Image.open(src).convert("RGBA")).astype(float) / 255.0


# ------------------------------------------------------------------ superfici nuove
def gen_sand(name, light, dark, ripple=1.0, pebbles=0, seed=0, lum=None):
    """Sabbia: increspature del vento ondulate, grana fine, qualche sassolino; illuminata dalla sua mappa delle altezze."""
    S = N
    yy, xx = np.mgrid[0:S, 0:S].astype(float)
    warp = (fbm(S, S, beta=2.6, seed=seed + 1) - 0.5) * 60
    ripples = np.sin((yy + warp + (fbm(S, S, beta=2.2, seed=seed + 2) - 0.5) * 18) / S * 2 * math.pi * 26)
    ripples = np.sign(ripples) * np.abs(ripples) ** 0.6                    # creste più nette da un lato
    low = fbm(S, S, beta=2.8, seed=seed + 3)
    grain = fbm(S, S, beta=0.4, seed=seed + 4)
    h = low * 14 + ripples * 1.6 * ripple * (0.5 + low) + grain * 0.8
    diff, _ = lit_from_height(blur_wrap(h, 0.8), wrap=True)
    base = lerp(dark, light, low[..., None]) * (0.9 + 0.2 * grain)[..., None]
    out = base * (0.6 + 0.55 * diff)[..., None]
    r = np.random.default_rng(seed + 5)
    im = Image.fromarray((np.clip(out, 0, 1) * 255).astype(np.uint8))
    d = ImageDraw.Draw(im)
    for _ in range(pebbles):
        x, y, rr = r.uniform(0, S), r.uniform(0, S), r.uniform(1.0, 3.2)
        g = r.uniform(0.55, 0.85)
        c = tuple(int(v * g * 255) for v in dark)
        d.ellipse([x - rr, y - rr * 0.8, x + rr, y + rr * 0.8], fill=c)
    out = np.asarray(im).astype(float) / 255.0
    if lum:
        out *= lum / (out @ np.array([0.2126, 0.7152, 0.0722])).mean()
    save(name, np.clip(out, 0, 1))


def gen_snow(name, seed=0):
    """Neve: cumuli morbidi, ombre azzurre, scintillii; un po' di ghiaccio e qualche filo d'erba secca che spunta."""
    S = N
    low = fbm(S, S, beta=3.0, seed=seed)
    mid = fbm(S, S, beta=2.0, seed=seed + 1)
    fine = fbm(S, S, beta=0.8, seed=seed + 2)
    h = low * 18 + mid * 5 + fine * 0.8
    diff, spec = lit_from_height(blur_wrap(h, 1.0), wrap=True)
    shadow = col(150, 176, 214)
    lit = col(246, 249, 255)
    out = lerp(shadow, lit, np.clip(diff * 1.25 - 0.1, 0, 1)[..., None])
    r = np.random.default_rng(seed + 3)
    sp = np.zeros((S, S))
    idx = r.integers(0, S, (900, 2))
    sp[idx[:, 0], idx[:, 1]] = r.uniform(0.3, 1.0, 900)
    sp = ndi.maximum_filter(sp, 2) * (diff > 0.7)
    out = out + sp[..., None] * 0.35
    out = out * (0.94 + 0.06 * fine)[..., None]
    save(name, np.clip(out, 0, 1))


def gen_ice(name, seed=0):
    """Ghiaccio: azzurro profondo con bolle e crepe bianche, riflessi chiari in superficie."""
    S = N
    deep = fbm(S, S, beta=2.6, seed=seed)
    fine = fbm(S, S, beta=1.2, seed=seed + 1)
    base = lerp(col(70, 140, 200), col(170, 216, 244), deep[..., None])
    yy, xx = np.mgrid[0:S, 0:S].astype(float)
    r = np.random.default_rng(seed + 2)
    pts = r.uniform(0, S, (18, 2))
    wx, wy = (fbm(S, S, beta=2.2, seed=seed + 3) - 0.5) * 50, (fbm(S, S, beta=2.2, seed=seed + 4) - 0.5) * 50
    d1 = np.full((S, S), 1e9); d2 = np.full((S, S), 1e9)
    for cx, cy in pts:
        for ox in (-S, 0, S):
            for oy in (-S, 0, S):
                dd = np.hypot(xx + wx - cx - ox, yy + wy - cy - oy)
                d2 = np.where(dd < d1, d1, np.minimum(d2, dd)); d1 = np.minimum(d1, dd)
    crack = np.exp(-(d2 - d1) / 1.4)
    out = lerp(base, col(236, 246, 255), (crack * 0.75)[..., None])
    out = lerp(out, col(220, 238, 252), (smooth((fine - 0.62) * 6) * 0.35)[..., None])     # brina
    save(name, np.clip(out, 0, 1))


def gen_lava(name, seed=0):
    """Lava: croste nere screpolate che galleggiano su roccia fusa arancio-gialla, più chiara nelle fessure larghe."""
    S = N
    yy, xx = np.mgrid[0:S, 0:S].astype(float)
    r = np.random.default_rng(seed)
    pts = r.uniform(0, S, (40, 2))
    wx, wy = (fbm(S, S, beta=2.4, seed=seed + 1) - 0.5) * 40, (fbm(S, S, beta=2.4, seed=seed + 2) - 0.5) * 40
    d1 = np.full((S, S), 1e9); d2 = np.full((S, S), 1e9)
    for cx, cy in pts:
        for ox in (-S, 0, S):
            for oy in (-S, 0, S):
                dd = np.hypot(xx + wx - cx - ox, yy + wy - cy - oy)
                d2 = np.where(dd < d1, d1, np.minimum(d2, dd)); d1 = np.minimum(d1, dd)
    edge = d2 - d1
    width = 2.5 + 7 * fbm(S, S, beta=2.0, seed=seed + 3)
    molten = np.exp(-edge / width)
    crust_h = np.minimum(edge, 20) * 0.5 + fbm(S, S, beta=1.2, seed=seed + 4) * 6
    diff, _ = lit_from_height(blur_wrap(crust_h, 0.8), wrap=True)
    crust = lerp(col(18, 14, 12), col(62, 50, 44), diff[..., None])
    hot = lerp(col(190, 40, 8), col(255, 214, 90), np.clip(molten * 1.4 - 0.2, 0, 1)[..., None])
    out = lerp(crust, hot, smooth(molten * 1.6 - 0.25)[..., None])
    save(name, np.clip(out, 0, 1))


def gen_marble(name, seed=0):
    """Marmo bianco con venature grigio-dorate morbide."""
    S = N
    yy, xx = np.mgrid[0:S, 0:S].astype(float)
    t = (xx + yy) / S * 2 * math.pi * 2 + (fbm(S, S, beta=2.8, seed=seed) - 0.5) * 9
    veins = 1 - np.exp(-np.abs(np.sin(t)) * 22) * (0.6 + 0.4 * fbm(S, S, beta=2.0, seed=seed + 2))
    fine = fbm(S, S, beta=1.4, seed=seed + 1)
    out = lerp(col(232, 226, 214), col(250, 248, 244), fine[..., None])
    out = lerp(col(208, 200, 186), out, (0.35 + 0.65 * veins)[..., None])
    save(name, np.clip(out, 0, 1))


def weather(name, grime=0.35, scratches=120, moss=0.0, seed=0):
    """Invecchia una texture costruita (metallo, cemento, pietra lavorata): sporco nelle zone basse, graffi, bordi consumati, muschio."""
    a = load(name)
    rgb, al = a[..., :3], a[..., 3]
    H, W = rgb.shape[:2]
    dirt = fbm(H, W, beta=2.2, seed=seed)
    spots = smooth((fbm(H, W, beta=1.5, seed=seed + 1) - 0.55) * 5)
    rgb = rgb * (1 - grime * (0.55 * dirt + 0.45 * spots))[..., None] + col(60, 50, 40) * (grime * 0.25 * spots)[..., None]
    # graffi sottili e poco contrastati: si vedono solo da vicino
    lay = Image.new("L", (W, H), 128)
    d = ImageDraw.Draw(lay)
    r = np.random.default_rng(seed + 2)
    for _ in range(scratches):
        x, y, ang, ln = r.uniform(0, W), r.uniform(0, H), r.uniform(0, 6.3), r.uniform(4, 22)
        d.line([(x, y), (x + math.cos(ang) * ln, y + math.sin(ang) * ln)], fill=170 if r.random() < 0.6 else 90, width=1)
    sc = (np.asarray(lay).astype(float) - 128) / 128 * 0.22
    rgb = rgb * (1 + sc)[..., None]
    if moss > 0:
        m = smooth((fbm(H, W, beta=2.0, seed=seed + 3) - 0.5) * 4) * moss
        rgb = lerp(rgb, col(64, 84, 40) * (0.8 + 0.4 * fbm(H, W, beta=0.8, seed=seed + 4))[..., None], m[..., None])
    rgb = rgb * (0.92 + 0.16 * fbm(H, W, beta=0.9, seed=seed + 5))[..., None]
    save(name, np.dstack([np.clip(rgb, 0, 1), al]) if (al < 0.999).any() else np.clip(rgb, 0, 1))


def naturalize(name, sat=0.8, dark=0.9):
    """Ciuffi e fiori: meno saturi, un po' più scuri (i colori da cartone diventano naturali), stessa forma."""
    a = load(name)
    rgb = desat(a[..., :3], sat) * dark
    save(name, np.dstack([np.clip(rgb, 0, 1), a[..., 3]]))


def leaves_overlay(colors, count, seed, size=(5, 11)):
    """Foglie cadute sparse sopra un prato (per l'autunno)."""
    def f(arr):
        S = arr.shape[0]
        im = Image.fromarray((np.clip(arr, 0, 1) * 255).astype(np.uint8))
        d = ImageDraw.Draw(im)
        r = np.random.default_rng(seed)
        for _ in range(count):
            x, y = r.uniform(0, S), r.uniform(0, S)
            a = r.uniform(0, math.pi)
            ln, w = r.uniform(*size), r.uniform(*size) * 0.55
            c = colors[r.integers(len(colors))] * r.uniform(0.75, 1.1)
            pts = [(x + math.cos(a) * ln, y + math.sin(a) * ln), (x + math.cos(a + 1.57) * w, y + math.sin(a + 1.57) * w),
                   (x - math.cos(a) * ln, y - math.sin(a) * ln), (x - math.cos(a + 1.57) * w, y - math.sin(a + 1.57) * w)]
            d.polygon(pts, fill=tuple(int(np.clip(v, 0, 1) * 255) for v in c))
        return np.asarray(im).astype(float) / 255.0
    return f


def flowers_overlay(colors, count, seed):
    def f(arr):
        S = arr.shape[0]
        im = Image.fromarray((np.clip(arr, 0, 1) * 255).astype(np.uint8))
        d = ImageDraw.Draw(im)
        r = np.random.default_rng(seed)
        for _ in range(count):
            x, y, rr = r.uniform(0, S), r.uniform(0, S), r.uniform(1.2, 2.6)
            c = colors[r.integers(len(colors))]
            d.ellipse([x - rr, y - rr, x + rr, y + rr], fill=tuple(int(v * 255) for v in c))
        return np.asarray(im).astype(float) / 255.0
    return f


# ------------------------------------------------------------------ mondi
def autumn():
    G.gen_grass("r3a_grass", dict(
        l1=[col(54, 62, 30), col(62, 66, 32), col(70, 64, 30)], l2=[col(92, 92, 42), col(104, 96, 44), col(86, 88, 40)],
        l3=[col(128, 116, 56), col(138, 120, 58), col(118, 110, 54)], dry=[col(160, 132, 76), col(150, 120, 64)],
        straw=[col(120, 96, 56), col(104, 82, 50)], balance=[1.0, 1.0, 1.0], lum=0.30,
        extra=leaves_overlay([col(196, 96, 30), col(214, 140, 40), col(160, 60, 30), col(120, 70, 36), col(222, 178, 70)], 300, 1101)))
    naturalize("r3a_tuft", 0.85, 0.92)
    naturalize("r3a_flowers", 0.85, 0.95)
    hz = fog(0.90, 0.79, 0.64) * 255
    G.gen_sky("r3a_sky", dict(top=col(56, 104, 170), mid=col(126, 160, 200), haze=col(*hz), cloud=col(255, 244, 228), belly=col(170, 150, 140), cover=0.52))
    G.gen_mountains("r3a_mountains", dict(haze=col(*hz), seed=1110, forest=(col(120, 60, 24), col(170, 104, 34)), rock=(col(104, 92, 80), col(150, 134, 116)), snow_k=0.6))


def spring():
    G.gen_grass("r3d_grass", dict(
        l1=[col(44, 78, 34), col(52, 86, 36)], l2=[col(74, 116, 44), col(84, 124, 48)], l3=[col(112, 150, 60), col(124, 158, 66)],
        dry=[col(150, 160, 90)], balance=[0.92, 1.0, 1.02], lum=0.36,
        extra=flowers_overlay([col(250, 250, 250), col(250, 222, 80), col(240, 160, 200), col(190, 170, 240)], 260, 1201)))
    naturalize("r3d_tuft", 0.88, 0.95)
    hz = fog(0.80, 0.90, 0.97) * 255
    G.gen_sky("r3d_sky", dict(top=col(52, 118, 200), mid=col(120, 176, 230), haze=col(*hz), cover=0.5))
    G.gen_mountains("r3d_mountains", dict(haze=col(*hz), seed=1210, forest=(col(40, 74, 40), col(78, 110, 54))))


def aurora():
    gen_snow("r3b_grass", 1301)
    gen_ice("r3b_ice", 1311)
    naturalize("r3b_tuft", 0.8, 0.95)
    hz = fog(0.05, 0.10, 0.22) * 255
    G.gen_mountains("r3b_mountains", dict(haze=col(*hz), seed=1320, rock=(col(40, 46, 60), col(70, 78, 96)), snow=col(150, 170, 210),
                                          forest=(col(12, 22, 26), col(22, 34, 38)), height=1.1))


def fire():
    G.gen_grass("r3c_grass", dict(
        l1=[col(30, 24, 18), col(38, 30, 22)], l2=[col(70, 52, 30), col(82, 60, 34)], l3=[col(110, 80, 44), col(96, 72, 40)],
        dry=[col(140, 110, 70)], straw=[col(50, 40, 30), col(30, 26, 22)], soil=(col(20, 16, 14), col(40, 30, 24)),
        tint_lo=col(255, 255, 255), tint_hi=col(255, 255, 255), balance=[1.0, 1.0, 1.0], lum=0.16,
        extra=flowers_overlay([col(255, 120, 30), col(255, 180, 60), col(220, 60, 20)], 160, 1401)))       # braci
    gen_lava("r3c_lava", 1411)
    naturalize("r3c_tuft", 0.85, 0.9)
    hz = fog(0.36, 0.15, 0.08) * 255
    G.gen_mountains("r3c_mountains", dict(haze=col(*hz), seed=1420, rock=(col(34, 28, 26), col(66, 52, 46)), snow=col(255, 120, 40), snow_k=0.35,
                                          forest=(col(20, 16, 14), col(40, 30, 24))))


def jungle():
    G.gen_grass("r3j_grass", dict(
        l1=[col(24, 60, 26), col(30, 70, 30)], l2=[col(46, 100, 38), col(56, 112, 42)], l3=[col(86, 140, 52), col(98, 150, 60)],
        dry=[col(130, 140, 70)], balance=[0.9, 1.0, 1.0], lum=0.30))
    gen_sand("r3j_sand", col(226, 204, 160), col(170, 140, 100), ripple=0.5, pebbles=60, seed=1501)
    weather("r3j_temple", grime=0.4, scratches=40, moss=0.45, seed=1502)
    naturalize("r3j_tuft", 0.85, 0.92)
    naturalize("r3j_flowers", 0.9, 0.95)
    hz = fog(0.70, 0.88, 0.86) * 255
    G.gen_sky("r3j_sky", dict(top=col(30, 110, 190), mid=col(100, 180, 220), haze=col(*hz), cover=0.5))
    G.gen_mountains("r3j_mountains", dict(haze=col(*hz), seed=1520, forest=(col(22, 70, 34), col(50, 104, 44)), forest_k=1.6, snow_k=0.0,
                                          rock=(col(70, 74, 64), col(104, 104, 90)), height=0.8))


def desert():
    gen_sand("r3s_sand", col(236, 206, 150), col(190, 150, 100), ripple=1.0, pebbles=40, seed=1601)
    gen_sand("r3s_dune", col(240, 210, 156), col(196, 156, 106), ripple=1.4, seed=1602)
    G.gen_rock("r3s_stone", dict(base=(col(170, 110, 70), col(214, 156, 104)), warm=col(190, 120, 70), lichen=col(230, 200, 150),
                                 lich_k=0.3, moss_k=0.0))
    hz = fog(0.96, 0.86, 0.68) * 255
    G.gen_sky("r3s_sky", dict(top=col(60, 120, 196), mid=col(140, 180, 220), haze=col(*hz), cover=0.62))
    G.gen_mountains("r3s_mountains", dict(haze=col(*hz), seed=1620, rock=(col(160, 96, 60), col(214, 150, 100)), forest_k=0.0, snow_k=0.0, height=0.7))


def factory():
    weather("r3f_floor", grime=0.45, scratches=260, seed=1701)
    weather("r3f_panel", grime=0.4, scratches=80, seed=1702)
    weather("r3f_block", grime=0.35, scratches=160, seed=1703)


def mine():
    G.gen_dirt("r3m_rock", dict(base=(col(70, 58, 48), col(100, 86, 70)), wet=col(44, 36, 30), pebble=(col(110, 104, 96), col(84, 76, 66)), twig=col(70, 50, 30)))
    G.gen_rock("r3m_wall", dict(base=(col(66, 60, 56), col(104, 96, 88)), warm=col(110, 84, 60), lichen=col(150, 130, 90), lich_k=0.25, moss_k=0.0))
    G.gen_rock("r3m_block", dict(base=(col(84, 76, 68), col(126, 116, 104)), warm=col(130, 100, 70), lichen=col(170, 150, 110), lich_k=0.2, moss_k=0.0))


def heaven():
    gen_marble("r3h_marble", 1901)


WORLDS = dict(autumn=autumn, spring=spring, aurora=aurora, fire=fire, jungle=jungle, desert=desert, factory=factory, mine=mine, heaven=heaven)

if __name__ == "__main__":
    for w in sys.argv[1:] or list(WORLDS):
        WORLDS[w]()
        print("fatto", w)
