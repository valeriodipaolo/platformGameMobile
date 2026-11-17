#!/usr/bin/env python3
"""Genera tutta la grafica del gioco (PNG) con texture procedurali.

Uso:  python3 tools/gen_assets.py
Richiede: numpy, scipy, Pillow.
Le immagini finiscono in app/src/main/res/drawable-nodpi (e l'icona in mipmap-xxxhdpi).
Per sostituire un'immagine con una foto o un disegno fatto a mano basta
rimpiazzare il PNG mantenendo lo stesso nome e le stesse proporzioni.
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw
from scipy import ndimage as ndi

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "app/src/main/res/drawable-nodpi")
ICON_OUT = os.path.join(ROOT, "app/src/main/res/mipmap-xxxhdpi")
T = 96  # pixel per casella

LIGHT = np.array([-0.45, -0.65, 0.62])
LIGHT = LIGHT / np.linalg.norm(LIGHT)
HALF = LIGHT + np.array([0, 0, 1.0])
HALF = HALF / np.linalg.norm(HALF)


# ---------------------------------------------------------------- rumore
def fbm(h, w, beta=2.0, seed=0, ax=1.0, ay=1.0):
    """Rumore frattale 2D ripetibile (tileable) in entrambe le direzioni, valori 0..1."""
    r = np.random.default_rng(seed)
    F = np.fft.fft2(r.standard_normal((h, w)))
    fy = np.fft.fftfreq(h)[:, None] * ay
    fx = np.fft.fftfreq(w)[None, :] * ax
    f = np.sqrt(fx * fx + fy * fy)
    f[0, 0] = 1.0
    F = F / f ** (beta / 2.0)
    F[0, 0] = 0
    n = np.real(np.fft.ifft2(F))
    n = (n - n.mean()) / (n.std() + 1e-9)
    return np.clip(0.5 + n / 5.0, 0, 1)


def fbm1(w, beta=2.0, seed=0):
    r = np.random.default_rng(seed)
    F = np.fft.fft(r.standard_normal(w))
    f = np.abs(np.fft.fftfreq(w))
    f[0] = 1.0
    F = F / f ** (beta / 2.0)
    F[0] = 0
    n = np.real(np.fft.ifft(F))
    n = (n - n.mean()) / (n.std() + 1e-9)
    return np.clip(0.5 + n / 5.0, 0, 1)


def smooth(t):
    t = np.clip(t, 0, 1)
    return t * t * (3 - 2 * t)


def lerp(a, b, t):
    return a + (b - a) * t


def lit_from_height(hmap, wrap=False):
    if wrap:
        gx = (np.roll(hmap, -1, 1) - np.roll(hmap, 1, 1)) / 2
        gy = (np.roll(hmap, -1, 0) - np.roll(hmap, 1, 0)) / 2
    else:
        gy, gx = np.gradient(hmap)
    nx, ny, nz = -gx, -gy, np.ones_like(hmap)
    ln = np.sqrt(nx * nx + ny * ny + nz * nz)
    nx, ny, nz = nx / ln, ny / ln, nz / ln
    diff = np.clip(nx * LIGHT[0] + ny * LIGHT[1] + nz * LIGHT[2], 0, 1)
    spec = np.clip(nx * HALF[0] + ny * HALF[1] + nz * HALF[2], 0, 1)
    return diff, spec


def shade(mask, color, bevel=8.0, amb=0.38, dif=0.78, spec=0.35, shin=24, tex=None):
    """Da una maschera booleana a un livello RGBA con volume, luce e riflesso."""
    inside = ndi.distance_transform_edt(mask)
    t = np.clip(inside / bevel, 0, 1)
    hgt = bevel * np.sqrt(1 - (1 - t) ** 2)
    hgt = ndi.gaussian_filter(hgt, 1.0)
    diff, sp = lit_from_height(hgt)
    base = np.array(color, dtype=float)[None, None, :] / 255.0
    if tex is not None:
        base = base * tex[..., None]
    rgb = base * (amb + dif * diff)[..., None] + (spec * sp ** shin)[..., None]
    alpha = np.clip(ndi.gaussian_filter(mask.astype(float), 0.8) * 1.6 - 0.3, 0, 1)
    return np.dstack([np.clip(rgb, 0, 1), alpha])


class Cv:
    """Tela con supersampling; le coordinate sono sempre in pixel finali."""

    def __init__(self, w, h, ss=3):
        self.ss, self.fw, self.fh = ss, w, h
        self.w, self.h = w * ss, h * ss
        self.a = np.zeros((self.h, self.w, 4))  # premoltiplicato

    def _mask(self, fn):
        im = Image.new("L", (self.w, self.h), 0)
        fn(ImageDraw.Draw(im), self.ss)
        return np.array(im) > 127

    def ell(self, cx, cy, rx, ry):
        return self._mask(lambda d, s: d.ellipse(
            [(cx - rx) * s, (cy - ry) * s, (cx + rx) * s, (cy + ry) * s], fill=255))

    def rrect(self, x0, y0, x1, y1, r):
        return self._mask(lambda d, s: d.rounded_rectangle(
            [x0 * s, y0 * s, x1 * s, y1 * s], radius=r * s, fill=255))

    def capsule(self, x0, y0, x1, y1, r):
        def fn(d, s):
            d.line([x0 * s, y0 * s, x1 * s, y1 * s], fill=255, width=int(round(2 * r * s)))
            for (x, y) in ((x0, y0), (x1, y1)):
                d.ellipse([(x - r) * s, (y - r) * s, (x + r) * s, (y + r) * s], fill=255)
        return self._mask(fn)

    def poly(self, pts):
        return self._mask(lambda d, s: d.polygon([(x * s, y * s) for x, y in pts], fill=255))

    def over(self, layer, opacity=1.0):
        a = layer[..., 3:4] * opacity
        self.a[..., :3] = layer[..., :3] * a + self.a[..., :3] * (1 - a)
        self.a[..., 3:4] = a + self.a[..., 3:4] * (1 - a)

    def add(self, mask, color, ao=0.0, **kw):
        kw["bevel"] = kw.get("bevel", 8.0) * self.ss
        if ao > 0:  # ombra di contatto morbida attorno alla forma
            sh = ndi.gaussian_filter(mask.astype(float), 2.0 * self.ss) * ao
            self.over(np.dstack([np.zeros(mask.shape + (3,)), sh]))
        self.over(shade(mask, color, **kw))

    def glow(self, mask, color, radius=4.0, strength=0.8):
        g = ndi.gaussian_filter(mask.astype(float), radius * self.ss) * strength
        c = np.array(color, dtype=float) / 255.0
        self.a[..., :3] = np.clip(self.a[..., :3] + c[None, None, :] * g[..., None], 0, 1)
        self.a[..., 3] = np.clip(self.a[..., 3] + g, 0, 1)

    def result(self):
        s = self.ss
        a = self.a.reshape(self.fh, s, self.fw, s, 4).mean(axis=(1, 3))
        al = a[..., 3:4]
        rgb = np.where(al > 1e-6, a[..., :3] / np.maximum(al, 1e-6), 0)
        return np.dstack([np.clip(rgb, 0, 1), np.clip(al, 0, 1)])


def save(name, arr, folder=None):
    folder = folder or OUT
    os.makedirs(folder, exist_ok=True)
    a = np.clip(arr * 255.0 + 0.5, 0, 255).astype(np.uint8)
    mode = "RGBA" if a.shape[2] == 4 else "RGB"
    Image.fromarray(a, mode).save(os.path.join(folder, name + ".png"), optimize=True)


def down(arr, s):
    h, w = arr.shape[0] // s, arr.shape[1] // s
    return arr.reshape(h, s, w, s, arr.shape[2]).mean(axis=(1, 3))


# ---------------------------------------------------------------- sfondi
SKY_TOP = np.array([34, 92, 184]) / 255.0
SKY_MID = np.array([104, 166, 228]) / 255.0
SKY_HOR = np.array([206, 228, 242]) / 255.0


def gen_sky():
    h, w = 768, 1536
    y = np.linspace(0, 1, h)[:, None, None]
    col = lerp(SKY_TOP, SKY_MID, smooth(y / 0.55))
    col = lerp(col, SKY_HOR, smooth((y - 0.4) / 0.6))
    col = np.broadcast_to(col, (h, w, 3)).copy()
    n = fbm(h, w, beta=2.7, seed=11, ax=2.2, ay=1.0)
    n = n + 0.16 * (fbm(h, w, beta=1.9, seed=12, ax=1.5) - 0.5)
    yy = np.linspace(0, 1, h)[:, None]
    band = smooth((yy - 0.02) / 0.12) * (1 - smooth((yy - 0.42) / 0.3))
    dens = smooth((n - 0.5) / 0.2) * band
    ns = ndi.gaussian_filter(n, 5.0, mode="wrap")
    gy = (np.roll(ns, -6, 0) - np.roll(ns, 6, 0))
    lit = np.clip(0.80 + gy * 3.2, 0.5, 1.0)
    lit = lit * (1 - 0.22 * smooth((n - 0.62) / 0.25))
    shadow = np.array([138, 156, 188]) / 255.0
    white = np.array([255, 253, 248]) / 255.0
    ccol = lerp(shadow, white, smooth((lit - 0.5) / 0.5)[..., None])
    a = (dens * 0.96)[..., None]
    col = ccol * a + col * (1 - a)
    save("bg_sky", col)


def gen_mountains():
    h, w = 512, 2048
    X = np.arange(w)[None, :].astype(float)
    Y = np.arange(h)[:, None].astype(float)
    out = np.zeros((h, w, 4))
    haze = np.array([190, 216, 238]) / 255.0
    layers = [(21, 6, 0.55, 0.97, 0.42, (104, 112, 134), 0.70),
              (22, 9, 0.28, 0.60, 0.22, (88, 92, 104), 0.90)]
    for seed, npk, hmin, hmax, hz, rock, snow_alt in layers:
        r = np.random.default_rng(seed)
        px = (np.arange(npk) + r.uniform(0.2, 0.8, npk)) * w / npk
        ph = r.uniform(hmin, hmax, npk) * h
        sl_l = r.uniform(0.45, 0.95, npk)
        sl_r = r.uniform(0.45, 0.95, npk)
        jag = (fbm1(w, beta=1.9, seed=seed) - 0.5) * 80 + (fbm1(w, beta=1.0, seed=seed + 1) - 0.5) * 12
        xs = X[0]

        def cone(i):
            dx = ((xs - px[i] + w / 2) % w) - w / 2
            return ph[i] - np.where(dx < 0, -dx * sl_l[i], dx * sl_r[i])
        cones = np.stack([cone(i) for i in range(npk)])
        owner = np.argmax(cones, axis=0)
        prof = np.maximum(cones.max(axis=0), 0.10 * h) + jag
        yr = (h - prof)[None, :]
        d = Y - yr
        sdx = (((xs - px[owner] + w / 2) % w) - w / 2)[None, :]
        ytop = (h - ph[owner])[None, :]
        warp = (fbm(h, w, beta=2.3, seed=seed + 3) - 0.5) * 90
        sd = sdx - (Y - ytop) * 0.30 - warp
        litside = smooth(-sd / 10.0 + 0.5)
        gull = fbm(h, w, beta=2.2, seed=seed + 5, ax=1.0, ay=3.5)
        diff, _ = lit_from_height(gull * 55, wrap=True)
        grain = 0.92 + 0.16 * fbm(h, w, beta=1.4, seed=seed + 9)
        bright = lerp(0.40, 1.02, litside) * (0.62 + 0.62 * diff) * grain
        rgb = (np.array(rock) / 255.0)[None, None, :] * bright[..., None]
        rgb = rgb * lerp(np.array([0.84, 0.93, 1.12]), np.array([1.06, 1.0, 0.94]), litside[..., None])
        alt = 1 - Y / h
        line = snow_alt + 0.07 * (gull - 0.5) * 2 + 0.03 * (1 - litside)
        snow = smooth((alt - line) / 0.035)
        s_lit = np.array([252, 253, 255]) / 255.0
        s_sh = np.array([148, 170, 208]) / 255.0
        srgb = lerp(s_sh, s_lit, np.clip(litside * (0.55 + 0.6 * diff), 0, 1)[..., None])
        rgb = lerp(rgb, srgb, snow[..., None])
        hzf = np.clip(hz + 0.42 * (Y / h) ** 2, 0, 0.95) * np.ones((1, w))
        rgb = lerp(np.clip(rgb, 0, 1), haze, hzf[..., None])
        a = np.clip(d, 0, 1)[..., None]
        out[..., :3] = rgb * a + out[..., :3] * (1 - a)
        out[..., 3:4] = a + out[..., 3:4] * (1 - a)
    save("bg_mountains", out)


def gen_hills():
    """Colline boscose: file di conifere dipinte da dietro in avanti."""
    h, w = 560, 2048
    X = np.arange(w)
    Y = np.arange(h)[:, None].astype(float)
    rgb = np.zeros((h, w, 3))
    alpha = np.zeros((h, w))
    haze = np.array([178, 206, 224]) / 255.0
    r = np.random.default_rng(31)
    layers = [  # seed, y cresta, ampiezza, larghezza albero, altezza, file, foschia, colori
        (32, 34, 60, 16, 20, 9, 0.50, (58, 104, 62), (20, 50, 38)),
        (33, 118, 80, 32, 40, 8, 0.24, (74, 128, 54), (16, 44, 26)),
        (34, 232, 70, 64, 78, 12, 0.0, (92, 150, 52), (12, 34, 18)),
    ]
    for seed, y0, amp, tw, th, rows, hz, tip, basec in layers:
        ridge = y0 + amp * fbm1(w, beta=2.6, seed=seed)
        tip, basec = np.array(tip) / 255.0, np.array(basec) / 255.0
        for k in range(rows):
            ntree = w // tw
            hs = r.uniform(0.45, 1.0, ntree)
            tint = r.uniform(0.75, 1.15, ntree)
            cuts = np.sort(r.uniform(0, w, ntree))
            ti = np.searchsorted(cuts, X, side="right") - 1      # -1 = albero a cavallo del bordo
            left = np.where(ti < 0, cuts[-1] - w, cuts[ti])
            right = np.where(ti < 0, cuts[0], np.where(ti + 1 < ntree, cuts[(ti + 1) % ntree], cuts[0] + w))
            width = np.maximum(right - left, 1.0)
            u = (X - left) / width
            tri = (1 - np.abs(2 * u - 1)) ** 1.25 * np.clip(width / tw, 0.5, 1.1)
            top = ridge + k * th * 0.42 - th * hs[ti] * tri
            dd = Y - top[None, :]
            inside = np.clip(dd / 1.2, 0, 1)
            side = 0.72 + 0.5 * (1 - u)[None, :]          # lato in luce a sinistra
            g = np.exp(-np.clip(dd, 0, None) / (th * 0.55))
            col = lerp(basec, tip, (g * side * tint[ti][None, :])[..., None].clip(0, 1))
            if k == rows - 1:  # sotto l'ultima fila il bosco sfuma nel buio
                deep = np.clip(dd / 260.0, 0, 1)[..., None]
                col = lerp(col, np.array([6, 14, 12]) / 255.0, deep * 0.8)
            col = lerp(col, haze, hz * (1 - 0.3 * k / rows))
            a = inside[..., None]
            rgb = col * a + rgb * (1 - a)
            alpha = inside + alpha * (1 - inside)
    grain = 0.9 + 0.2 * fbm(h, w, beta=1.0, seed=39)
    save("bg_hills", np.dstack([np.clip(rgb * grain[..., None], 0, 1), alpha]))


# ---------------------------------------------------------------- caselle
def dirt_tex(s=2):
    n = T * s
    low = fbm(n, n, beta=2.0, seed=41)
    fine = fbm(n, n, beta=1.0, seed=42)
    diff, _ = lit_from_height(low * 26 + fine * 7, wrap=True)
    base = np.array([116, 80, 50]) / 255.0
    tex = 0.72 + 0.4 * low + 0.22 * (fine - 0.5)
    rgb = base[None, None, :] * (tex * (0.5 + 0.75 * diff))[..., None]
    # sassolini
    r = np.random.default_rng(43)
    cv = Cv(T, T, s)
    cv.a = np.dstack([rgb, np.ones((n, n))])
    for _ in range(7):
        cx, cy = r.uniform(12, T - 12), r.uniform(12, T - 12)
        rx, ry = r.uniform(3, 7), r.uniform(2.5, 5)
        g = r.uniform(0.8, 1.15)
        cv.add(cv.ell(cx, cy, rx, ry), (150 * g, 140 * g, 128 * g), ao=0.5, bevel=3.5, spec=0.12)
    return cv.a[..., :3]


def gen_ground():
    s = 2
    n = T * s
    dirt = dirt_tex(s)
    save("tile_dirt", down(dirt, s))
    r = np.random.default_rng(44)
    edge = 30 + 14 * (fbm1(n, beta=2.0, seed=45) - 0.5)
    blades = np.repeat(r.uniform(0, 9, n // 2), 2)
    yb = (edge + blades)[None, :] * s
    yy = np.arange(n)[:, None].astype(float)
    streak = fbm(n, n, beta=1.6, seed=46, ax=1.0, ay=7.0)
    fine = fbm(n, n, beta=0.8, seed=47)
    g_dark = np.array([40, 96, 26]) / 255.0
    g_light = np.array([128, 182, 58]) / 255.0
    t = np.clip(0.15 + 0.9 * streak + 0.35 * (fine - 0.5), 0, 1)
    grass = lerp(g_dark, g_light, t[..., None])
    sun = 1.0 + 0.30 * np.exp(-yy / (6.0 * s))
    deep = 0.62 + 0.38 * np.exp(-np.clip(yy - 8 * s, 0, None) / (22.0 * s))
    grass = np.clip(grass * (sun * deep)[..., None], 0, 1)
    a = np.clip((yb - yy) / 1.5, 0, 1)
    shadow = 1 - 0.55 * np.exp(-np.clip(yy - yb, 0, None) / (5.0 * s)) * (1 - a)
    rgb = dirt * shadow[..., None]
    rgb = grass * a[..., None] + rgb * (1 - a[..., None])
    save("tile_grass", down(rgb, s))

    # ciuffi d'erba che sporgono sopra il terreno
    fw, fh = T, 28
    cv = Cv(fw, fh, 3)
    im = Image.new("RGBA", (cv.w, cv.h), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    for _ in range(120):
        x = r.uniform(0, fw)
        hgt = r.uniform(7, 25)
        lean = r.uniform(-5, 5)
        wd = r.uniform(1.6, 3.2)
        k = r.uniform(0, 1)
        col = tuple(int(v) for v in lerp(np.array([44, 104, 28]), np.array([140, 196, 64]), k)) + (255,)
        for off in (-fw, 0, fw):
            pts = [((x - wd + off) * 3, fh * 3), ((x + wd + off) * 3, fh * 3),
                   ((x + lean + off) * 3, (fh - hgt) * 3)]
            d.polygon(pts, fill=col)
    arr = np.array(im).astype(float) / 255.0
    arr[..., :3] *= arr[..., 3:4]
    cv.a = arr
    save("grass_fringe", cv.result())


def gen_brick():
    s = 2
    n = T * s
    big = Image.new("L", (n * 3, n * 3), 0)
    tint = Image.new("L", (n * 3, n * 3), 128)
    d, dt = ImageDraw.Draw(big), ImageDraw.Draw(tint)
    bw, bh, m = 48 * s, 24 * s, 2 * s
    r = np.random.default_rng(51)
    tints = r.integers(95, 165, size=(4, 2))
    for row in range(-1, 13):
        off = (row % 2) * (bw // 2)
        for col in range(-1, 8):
            x0, y0 = col * bw + off - bw // 2 * 0, row * bh
            box = [x0 + m, y0 + m, x0 + bw - m, y0 + bh - m]
            d.rounded_rectangle(box, radius=3 * s, fill=255)
            dt.rectangle([x0, y0, x0 + bw, y0 + bh], fill=int(tints[row % 4][col % 2]))
    mask = np.array(big) > 127
    tn = np.array(tint).astype(float) / 128.0
    tex = np.tile(0.78 + 0.34 * fbm(n, n, beta=1.3, seed=52) + 0.2 * (fbm(n, n, beta=0.6, seed=53) - 0.5), (3, 3))
    layer = shade(mask, (188, 104, 66), bevel=4.0 * s, spec=0.10, tex=tex * (0.8 + 0.2 * tn))
    mortar = np.array([128, 118, 106]) / 255.0 * np.tile(0.7 + 0.4 * fbm(n, n, beta=1.0, seed=54), (3, 3))[..., None]
    ao = 1 - 0.5 * ndi.gaussian_filter(mask.astype(float), 2.0 * s)
    a = layer[..., 3:4]
    rgb = layer[..., :3] * a + mortar * ao[..., None] * (1 - a)
    save("tile_brick", down(rgb[n:2 * n, n:2 * n], s))


def star_pts(cx, cy, R, r, k=1.0, rot=-90):
    pts = []
    for i in range(10):
        ang = math.radians(rot + i * 36)
        rad = R if i % 2 == 0 else r
        pts.append((cx + math.cos(ang) * rad * k, cy + math.sin(ang) * rad))
    return pts


def crate(used):
    cv = Cv(T, T, 3)
    n = cv.w
    grain = 0.72 + 0.5 * fbm(n, n, beta=1.8, seed=61, ax=9.0, ay=1.0) + 0.15 * (fbm(n, n, beta=0.8, seed=62) - 0.5)
    if used:
        grain = grain * 0.5
    for i in range(4):
        y0 = 6 + i * 21
        cv.add(cv.rrect(5, y0, T - 5, y0 + 20, 2.5), (164, 110, 60), bevel=2.5, spec=0.08, tex=grain)
    ring = cv.rrect(0.5, 0.5, T - 0.5, T - 0.5, 8) & ~cv.rrect(9, 9, T - 9, T - 9, 4)
    steel = (70, 74, 82) if used else (104, 110, 122)
    cv.add(ring, steel, ao=0.5, bevel=4.5, spec=0.55, shin=18,
           tex=0.85 + 0.3 * fbm(n, n, beta=1.2, seed=63))
    for (x, y) in ((6, 6), (T - 6, 6), (6, T - 6), (T - 6, T - 6)):
        cv.add(cv.ell(x, y, 3.2, 3.2), (150, 156, 168) if not used else (96, 100, 108), bevel=3.2, spec=0.7)
    if used:
        cv.add(cv.rrect(30, 30, T - 30, T - 30, 6), (44, 30, 20), ao=0.4, bevel=3, spec=0.05)
    else:
        st = cv.poly(star_pts(T / 2, T / 2 + 2, 27, 11.5))
        cv.glow(st, (255, 200, 60), radius=6, strength=0.55)
        cv.add(st, (250, 196, 44), ao=0.6, bevel=7, spec=0.85, shin=14)
    return cv.result()


def gen_blocks():
    save("tile_bonus", crate(False))
    save("tile_used", crate(True))
    cv = Cv(T, T, 3)
    n = cv.w
    tex = 0.74 + 0.36 * fbm(n, n, beta=1.9, seed=71) + 0.34 * (fbm(n, n, beta=0.5, seed=72) - 0.5)
    r = np.random.default_rng(73)
    im = Image.new("L", (n, n), 0)
    d = ImageDraw.Draw(im)
    for _ in range(4):
        x, y = r.uniform(10, T - 10) * 3, r.uniform(10, T - 10) * 3
        pts = [(x, y)]
        ang = r.uniform(0, 6.28)
        for _ in range(9):
            ang += r.uniform(-0.7, 0.7)
            x, y = x + math.cos(ang) * 14, y + math.sin(ang) * 14
            pts.append((x, y))
        d.line(pts, fill=255, width=3)
    crack = ndi.gaussian_filter(np.array(im).astype(float) / 255.0, 1.2)
    tex = tex * (1 - 0.6 * crack)
    cv.a[...] = np.array([0.16, 0.16, 0.17, 1.0])
    cv.add(cv.rrect(1, 1, T - 1, T - 1, 7), (136, 138, 144), bevel=8, spec=0.14, tex=tex)
    out = cv.result()
    save("tile_stone", out[..., :3])


# ---------------------------------------------------------------- oggetti
def sparkle(cv, cx, cy, k):
    if k <= 0.05:
        return
    R, r = 13 * k, 2.6 * k
    pts = [(cx, cy - R), (cx + r, cy - r), (cx + R, cy), (cx + r, cy + r),
           (cx, cy + R), (cx - r, cy + r), (cx - R, cy), (cx - r, cy - r)]
    m = cv.poly(pts)
    cv.glow(m, (255, 255, 255), radius=2.5, strength=0.9)
    cv.add(m, (255, 255, 255), bevel=1, amb=1.0, dif=0.0, spec=0.0)


def facets(cv, polys, base, mults, bevel=2.5):
    for poly, k in zip(polys, mults):
        col = tuple(int(max(0, min(255, c * k))) for c in base)
        cv.add(cv.poly(poly), col, bevel=bevel, spec=0.55, shin=10, amb=0.55, dif=0.55)


AQUA = (46, 214, 204)


def gem_shard(phase):
    cv = Cv(64, 64, 3)
    P = [(32, 4), (49, 21), (45, 50), (31, 61), (17, 50), (14, 22)]
    C = (33, 33)
    cv.glow(cv.poly(P), (60, 240, 225), radius=5, strength=0.35)
    polys = [[P[i], P[(i + 1) % 6], C] for i in range(6)]
    facets(cv, [polys[5], polys[0], polys[1], polys[2], polys[3], polys[4]],
           AQUA, [1.30, 1.08, 0.82, 0.55, 0.70, 0.96])
    sparkle(cv, 24, 17, phase)
    return cv.result()


def gen_gem():
    for i, k in enumerate([0.0, 0.35, 0.8, 1.0, 0.55, 0.15]):
        save("gem_%d" % i, gem_shard(k))
    cv = Cv(96, 96, 3)
    cv.glow(cv.poly([(24, 16), (72, 16), (92, 38), (48, 90), (4, 38)]), (60, 240, 225), radius=6, strength=0.4)
    polys = [[(24, 16), (72, 16), (62, 38), (34, 38)],
             [(24, 16), (4, 38), (34, 38)], [(72, 16), (92, 38), (62, 38)],
             [(4, 38), (34, 38), (48, 90)], [(34, 38), (62, 38), (48, 90)], [(62, 38), (92, 38), (48, 90)]]
    facets(cv, polys, AQUA, [1.35, 1.05, 0.85, 0.72, 0.98, 0.55], bevel=3)
    sparkle(cv, 30, 26, 1.1)
    save("gem_full", cv.result())


def gen_battery():
    """Chiave dorata (il file resta battery.png): serve ad aprire il portone di fine livello."""
    cv = Cv(64, 64, 3)
    gold, dark = (236, 192, 66), (150, 108, 30)
    bow = cv.ell(32, 17, 13, 13)
    hole = cv.ell(32, 17, 6, 6)
    cv.glow(bow, (255, 226, 120), radius=5, strength=0.5)
    cv.add(cv.rrect(28.5, 28, 35.5, 58, 2.5), gold, ao=0.4, bevel=3, spec=0.9, shin=30)
    for (y0, y1, w) in [(44, 50, 12), (52, 58, 9)]:
        cv.add(cv.rrect(33, y0, 33 + w, y1, 1.8), gold, bevel=2.2, spec=0.9, shin=30)
    cv.add(cv.rrect(25, 27, 39, 32, 2.2), (252, 214, 92), bevel=2.4, spec=0.9)
    cv.add(bow & ~hole, gold, ao=0.4, bevel=5, spec=0.95, shin=34)
    cv.add(cv.ell(32, 17, 12.4, 12.4) & ~cv.ell(32, 17, 6.6, 6.6) & cv.ell(28, 12, 9, 7), (255, 246, 190), bevel=1, amb=1.0, dif=0.0, spec=0.0)
    save("battery", cv.result())


def gen_chip():
    cv = Cv(64, 64, 3)
    gold = (232, 190, 62)
    for k in range(4):
        o = 17 + k * 9.5
        cv.add(cv.rrect(3, o, 14, o + 4.5, 1.6), gold, bevel=1.8, spec=0.8)
        cv.add(cv.rrect(50, o, 61, o + 4.5, 1.6), gold, bevel=1.8, spec=0.8)
        cv.add(cv.rrect(o, 3, o + 4.5, 14, 1.6), gold, bevel=1.8, spec=0.8)
        cv.add(cv.rrect(o, 50, o + 4.5, 61, 1.6), gold, bevel=1.8, spec=0.8)
    cv.add(cv.rrect(11, 11, 53, 53, 6), (34, 40, 54), ao=0.45, bevel=6, spec=0.7, shin=24)
    for (x0, y0, x1, y1) in [(17, 20, 28, 20), (28, 20, 28, 28), (47, 44, 36, 44), (36, 44, 36, 36), (17, 44, 24, 44), (47, 20, 40, 20)]:
        cv.add(cv.capsule(x0, y0, x1, y1, 1.2), (70, 220, 250), bevel=1.2, amb=1.0, dif=0.0, spec=0.0)
    core = cv.rrect(25, 25, 39, 39, 3)
    cv.glow(core, (90, 230, 255), radius=4, strength=0.6)
    cv.add(core, (96, 110, 134), bevel=3, spec=0.8)
    cv.add(cv.ell(32, 32, 3.4, 3.4), (120, 240, 255), bevel=3, amb=1.0, dif=0.0, spec=0.0)
    save("chip", cv.result())


def gen_spinfx():
    cv = Cv(128, 128, 3)
    for i in range(10):                      # dieci anelli sottili e tenui, stessa altezza e larghezza di prima
        cy = 18 + i * 10.2
        sc = 0.8 + 0.2 * math.sin(math.pi * (i + 0.5) / 10)
        rx, ry = 56 * sc, 11 * sc
        ring = cv.ell(64, cy, rx, ry) & ~cv.ell(64, cy, rx - 2.6, ry - 1.9)
        cv.glow(ring, (150, 215, 255), radius=2.0, strength=0.5)
        cv.glow(ring, (235, 247, 255), radius=0.6, strength=0.9)
    save("spin_fx", cv.result())


# colori del viandante: gli stessi del modello 3D
WHITE_C, GRAPHITE, ORANGE_C, METAL = (248, 248, 252), (64, 68, 78), (238, 118, 28), (150, 156, 168)
GOLD, BLUE_C, LINING = (238, 184, 62), (62, 112, 190), (92, 142, 218)
HIDE, HIDE_LT, HIDE_DK = (156, 96, 48), (206, 148, 90), (112, 66, 34)
BLANKET, BLANKET_LT = (52, 104, 176), (112, 162, 222)


def cloth_tex(cv, ang, period, ph, amp=0.12):
    """Pieghe di stoffa: bande chiare e scure perpendicolari alla direzione ang (radianti)."""
    yy, xx = np.mgrid[0:cv.h, 0:cv.w]
    d = (xx * -math.sin(ang) + yy * math.cos(ang)) / cv.ss
    return 1.0 - amp * (0.5 + 0.5 * np.sin(d * 2 * math.pi / period + ph))


def pleat_tex(cv, period, ph=0.0, amp=0.16):
    xx = np.arange(cv.w)[None, :] / cv.ss
    return np.broadcast_to(1.0 - amp * (0.5 + 0.5 * np.cos(xx * 2 * math.pi / period + ph)), (cv.h, cv.w)).copy()


def cape(cv, A, B, T1, T2, sag, ph, wave=2.4):
    """Mantello: striscia di stoffa dal colletto (A) e dalla vita (B) fino alle due punte libere (T1 in alto, T2 in basso)."""
    def curve(P, Q, bend, rip, n=14):
        dx, dy = Q[0] - P[0], Q[1] - P[1]
        L = math.hypot(dx, dy) or 1.0
        nx, ny = -dy / L, dx / L
        pts = []
        for i in range(n + 1):
            t = i / n
            off = math.sin(math.pi * t) * bend + rip * math.sin(t * math.pi * 3 + ph) * math.sin(math.pi * t)
            pts.append((P[0] + dx * t + nx * off, P[1] + dy * t + ny * off))
        return pts
    top = curve(A, T1, sag, wave)
    free = curve(T1, T2, -sag * 0.5, wave * 1.5)
    bot = curve(T2, B, -sag * 0.6, wave)
    poly = top + free[1:] + bot[1:]
    cv.add(cv.poly([(x - 2.5, y + 3.2) for x, y in poly]), LINING, bevel=3, spec=0.2)               # fodera azzurra
    ang = math.atan2(T1[1] - A[1], T1[0] - A[0])
    cv.add(cv.poly(poly), WHITE_C, ao=0.35, bevel=5, spec=0.18, shin=18, tex=cloth_tex(cv, ang, 9, ph))
    for (x0, y0), (x1, y1) in zip(free[:-1], free[1:]):                                                # orlo dorato
        cv.add(cv.capsule(x0, y0, x1, y1, 1.7), GOLD, bevel=1.7, spec=0.5)


def backpack(cv, dy):
    """Zaino di cuoio con patta, cinghie e fibbie, tasca, coperta arrotolata in cima e lanterna appesa."""
    cv.add(cv.rrect(21, 63 + dy, 45, 106 + dy, 8), HIDE, ao=0.35, bevel=8, spec=0.25)
    cv.add(cv.rrect(21, 63 + dy, 45, 81 + dy, 7), HIDE_DK, bevel=6, spec=0.2)
    for x in (28, 38):
        cv.add(cv.rrect(x - 1.6, 66 + dy, x + 1.6, 93 + dy, 1), HIDE_LT, bevel=1.5, spec=0.2)
        cv.add(cv.rrect(x - 3, 82 + dy, x + 3, 88 + dy, 2), GOLD, bevel=2, spec=0.7)
    cv.add(cv.rrect(26, 90 + dy, 41, 105 + dy, 4.5), HIDE_DK, ao=0.3, bevel=4.5, spec=0.2)           # tasca
    cv.add(cv.ell(33.5, 94 + dy, 2.2, 2.2), GOLD, bevel=2, spec=0.7)
    roll = cv.ell(33, 63 + dy, 10.5, 9.2)
    cv.add(roll, BLANKET, ao=0.35, bevel=9, spec=0.35)                                                # coperta arrotolata
    cv.add(cv.ell(33, 63 + dy, 6, 5.2), BLANKET_LT, bevel=5, spec=0.2)
    cv.add(cv.ell(33, 63 + dy, 2.4, 2.0), BLANKET, bevel=2, spec=0.1)
    cv.add(cv.capsule(19, 92 + dy, 19, 98 + dy, 0.9), METAL, bevel=1, spec=0.4)
    cv.add(cv.rrect(14, 98 + dy, 24, 111 + dy, 2.5), (46, 50, 60), ao=0.25, bevel=2.5, spec=0.4)    # lanterna
    glass = cv.rrect(16, 100.5 + dy, 22, 109 + dy, 1.5)
    cv.add(glass, (255, 214, 110), bevel=1.5, spec=0.3)
    cv.glow(glass, (255, 200, 90), radius=3, strength=0.5)


def robe(cv, dy):
    """Veste bianca: gonna a pieghe con l'orlo blu e oro, busto, cintura con fibbia e borsello, colletto, gemma sul petto."""
    pts = [(41, 88 + dy), (75, 88 + dy), (79, 108 + dy), (83, 117 + dy), (33, 116 + dy), (37, 108 + dy)]
    sk = cv.poly(pts)
    cv.add(sk, WHITE_C, ao=0.4, bevel=7, spec=0.2, tex=pleat_tex(cv, 7.5))
    cv.add(cv.poly([(33.4, 112.6 + dy), (83.2, 113.4 + dy), (83.6, 117.4 + dy), (33, 116.6 + dy)]) & sk, GOLD, bevel=2, spec=0.6)
    cv.add(cv.poly([(34, 110 + dy), (82.6, 110.8 + dy), (83, 112.8 + dy), (33.6, 112 + dy)]) & sk, BLUE_C, bevel=1.5, spec=0.3)
    cv.add(cv.rrect(37, 58 + dy, 76, 99 + dy, 13), WHITE_C, ao=0.4, bevel=13, spec=0.5, shin=30)
    cv.add(cv.rrect(37, 89 + dy, 76, 97 + dy, 2.5), HIDE, ao=0.2, bevel=3, spec=0.3)
    cv.add(cv.rrect(64, 88.4 + dy, 73.5, 97.6 + dy, 2), GOLD, bevel=2.5, spec=0.8)
    cv.add(cv.rrect(45, 95 + dy, 57, 108 + dy, 3.5), HIDE_DK, ao=0.3, bevel=4, spec=0.2)
    cv.add(cv.ell(56, 59 + dy, 17.5, 6.6), (238, 242, 250), ao=0.3, bevel=6, spec=0.4)
    gem = cv.poly([(65, 67 + dy), (69.6, 73 + dy), (65, 79.5 + dy), (60.4, 73 + dy)])
    cv.add(gem, (60, 214, 240), ao=0.2, bevel=3, spec=0.9)
    cv.glow(gem, (80, 225, 255), radius=3, strength=0.4)
    cv.add(cv.capsule(41, 65 + dy, 53, 62.5 + dy, 2.2), HIDE_LT, bevel=2.2, spec=0.3)               # spallaccio dello zaino
    cv.add(cv.capsule(53, 62.5 + dy, 63, 86 + dy, 2.2), HIDE_LT, bevel=2.2, spec=0.3)


def sleeve_arm(cv, shx, shy, ang):
    a = math.radians(ang)
    sx, sy = math.sin(a), math.cos(a)
    ex, ey = shx + sx * 15, shy + sy * 15
    cv.add(cv.capsule(shx, shy, ex, ey, 6.8), WHITE_C, ao=0.35, bevel=6.8, spec=0.4)               # manica
    cv.add(cv.capsule(ex - sx, ey - sy, ex + sx * 2.5, ey + sy * 2.5, 7.4), GOLD, bevel=3, spec=0.7)  # polsino
    fx, fy = ex + sx * 10, ey + sy * 10
    cv.add(cv.capsule(ex, ey, fx, fy, 3.8), GRAPHITE, bevel=3.8, spec=0.5)
    cv.add(cv.ell(fx + sx * 3, fy + sy * 3, 5.6, 5.6), (216, 220, 230), bevel=5.6, spec=0.6)


def boot_leg(cv, hipx, hipy, ang, lift):
    a = math.radians(ang)
    ln = 29 - lift
    fx, fy = hipx + math.sin(a) * ln, hipy + math.cos(a) * ln
    cv.add(cv.capsule(hipx, hipy, fx, fy - 7, 4.8), GRAPHITE, ao=0.35, bevel=4.8, spec=0.5)
    cv.add(cv.rrect(fx - 8, fy - 2, fx + 15, fy + 9.5, 5), HIDE, ao=0.35, bevel=5, spec=0.3)         # stivale
    cv.add(cv.rrect(fx - 7.5, fy - 9, fx + 7.5, fy - 1, 3), HIDE_LT, bevel=3, spec=0.3)             # risvolto
    cv.add(cv.rrect(fx - 8.5, fy + 8, fx + 15.5, fy + 11.5, 2), (44, 36, 30), bevel=2, spec=0.2)    # suola


CAPES = {
    "idle": ((10, 98), (19, 129), 3.0, 0.0, 1.0),
    "run0": ((0, 64), (1, 106), -5.0, 0.5, 3.0),
    "run1": ((1, 74), (3, 114), 4.0, 1.8, 3.0),
    "run2": ((0, 62), (1, 103), -4.0, 3.1, 3.0),
    "run3": ((1, 76), (4, 116), 5.0, 4.4, 3.0),
    "jump": ((2, 40), (0, 90), 6.0, 2.2, 3.4),
    "icon": ((0, 58), (1, 112), 6.0, 0.9, 3.0),
}


def hero(a1, a2, b1, b2, dy=0, l1=0, l2=0, cp="idle"):
    """Viandante robot, rivolto a destra: veste bianca, zaino di cuoio, mantello che fluttua. Angoli in gradi dalla verticale."""
    W, H = 112, 144
    cv = Cv(W, H, 3)
    hipx, hipy, shx, shy = 56, 101 + dy, 56, 73 + dy
    t1, t2, sag, ph, wv = CAPES[cp]
    cape(cv, (48, 64 + dy), (42, 100 + dy), (t1[0], t1[1] + dy), (t2[0], t2[1] + dy), sag, ph, wv)
    sleeve_arm(cv, shx, shy, b2)
    boot_leg(cv, hipx, hipy, a2, l2)
    backpack(cv, dy)
    boot_leg(cv, hipx, hipy, a1, l1)
    robe(cv, dy)
    sleeve_arm(cv, shx, shy, b1)
    head(cv, dy)
    return cv.result()


def head(cv, dy, ax=46, ay=20):
    cv.add(cv.capsule(ax + 2, ay + dy, ax - 2, 8 + dy, 1.6), METAL, bevel=1.6, spec=0.6)             # antenna
    ball = cv.ell(ax - 2, 7 + dy, 4.2, 4.2)
    cv.add(ball, (232, 48, 48), bevel=4.2, spec=0.9)
    cv.glow(ball, (255, 60, 60), radius=3, strength=0.3)
    cv.add(cv.rrect(28, 18 + dy, 88, 62 + dy, 19), WHITE_C, ao=0.45, bevel=19, spec=0.7, shin=34)    # casco
    cv.add(cv.capsule(44, 20.5 + dy, 68, 20 + dy, 1.5), ORANGE_C, bevel=1.5, spec=0.4)               # cresta
    cv.add(cv.rrect(54, 27 + dy, 86, 52 + dy, 10), (18, 24, 38), ao=0.3, bevel=9, spec=0.95, shin=40)  # visiera
    eyes = cv.ell(67, 40 + dy, 3.6, 6.2) | cv.ell(79, 40 + dy, 3.6, 6.2)
    cv.add(eyes, (120, 236, 255), bevel=3, spec=0.5)
    cv.glow(eyes, (80, 220, 255), radius=3.5, strength=0.55)
    cv.add(cv.ell(47, 47 + dy, 6.6, 7.4), ORANGE_C, ao=0.3, bevel=6, spec=0.5)                       # auricolare
    cv.add(cv.ell(47, 47 + dy, 3, 3.4), METAL, bevel=3, spec=0.8)


def gen_hero():
    save("hero_idle", hero(7, -7, 10, -10))
    save("hero_run_0", hero(38, -34, -42, 42, cp="run0"))
    save("hero_run_1", hero(12, -10, -10, 12, dy=-3, l2=8, cp="run1"))
    save("hero_run_2", hero(-34, 38, 42, -42, cp="run2"))
    save("hero_run_3", hero(-10, 12, 12, -10, dy=-3, l1=8, cp="run3"))
    save("hero_jump", hero(52, -42, 150, -62, dy=-2, l1=7, cp="jump"))
    return hero(7, -7, 10, -10)


SPIN_FRAMES = 12


def hero_spin(phi):
    """Robot che ruota attorno all'asse verticale, con il mantello aperto a campana. phi=0: di profilo verso destra; 90 gradi: di fronte."""
    W, H = 112, 144
    cv = Cv(W, H, 3)
    cx, dy = 56, -2
    c, sn = math.cos(phi), math.sin(phi)          # sn > 0: la faccia guarda verso chi gioca

    def proj(rx, rz):                              # mezza larghezza vista di una sezione ellittica
        return math.sqrt((rx * c) ** 2 + (rz * sn) ** 2)

    def px(u, v):                                  # x e profondità di un punto (u avanti, v lateralmente)
        return cx + u * c - v * sn, u * sn + v * c

    shy = 74 + dy
    # mantello: campana bianca dietro a tutto, con la fodera azzurra e l'orlo dorato
    hem = 37
    wtop = proj(13, 17)
    bell = [(cx - wtop, 66 + dy), (cx + wtop, 66 + dy), (cx + hem, 111 + dy), (cx + hem * 0.62, 121 + dy), (cx - hem * 0.62, 121 + dy), (cx - hem, 111 + dy)]
    cv.add(cv.poly([(x, y + 3.5) for x, y in bell]), LINING, bevel=3, spec=0.2)
    cv.add(cv.poly(bell), WHITE_C, ao=0.3, bevel=6, spec=0.15, tex=cloth_tex(cv, math.pi / 2, 11, phi * 2))
    for (x0, y0), (x1, y1) in zip(bell[2:5], bell[3:6]):
        cv.add(cv.capsule(x0, y0, x1, y1, 1.8), GOLD, bevel=1.8, spec=0.5)
    behind, front = [], []

    def put(z, fn):
        (behind if z < -0.5 else front).append((z, fn))

    for v in (-9, 9):                              # stivali
        x, z = px(0, v)
        def leg(x=x):
            cv.add(cv.capsule(x, 115, x, 125, 4.8), GRAPHITE, ao=0.35, bevel=4.8, spec=0.5)
            bw = proj(12, 8)
            fx = x + 5 * c
            cv.add(cv.rrect(fx - bw, 121, fx + bw, 134, 5), HIDE, ao=0.35, bevel=5, spec=0.3)
            cv.add(cv.rrect(fx - bw * 0.62, 117, fx + bw * 0.62, 123, 2.5), HIDE_LT, bevel=3, spec=0.3)
        put(z, leg)

    for v in (-1, 1):                              # braccia aperte come una trottola
        x0, z0 = px(0, v * 20)
        x1, z1 = px(0, v * 47)
        def arm(x0=x0, x1=x1):
            xm = x0 + (x1 - x0) * 0.55
            ym = shy - 7 * 0.55
            cv.add(cv.capsule(x0, shy, xm, ym, 6.2), WHITE_C, ao=0.35, bevel=6.2, spec=0.4)
            cv.add(cv.ell(xm, ym, 5.0, 7.2), GOLD, bevel=3, spec=0.7)
            cv.add(cv.capsule(xm, ym, x1, shy - 7, 3.6), GRAPHITE, bevel=3.6, spec=0.5)
            cv.add(cv.ell(x1, shy - 7, 6, 6), (216, 220, 230), bevel=6, spec=0.6)
        put((z0 + z1) / 2, arm)

    bx, bz = px(-25, 0)                            # zaino con la coperta in cima
    def pack():
        hwp = proj(10, 18)
        cv.add(cv.rrect(bx - hwp, 64 + dy, bx + hwp, 104 + dy, min(8, hwp - 0.5)), HIDE, ao=0.3, bevel=7, spec=0.25)
        cv.add(cv.rrect(bx - hwp, 64 + dy, bx + hwp, 80 + dy, min(7, hwp - 0.5)), HIDE_DK, bevel=6, spec=0.2)
        rw = proj(11, 22)
        cv.add(cv.rrect(bx - rw, 54 + dy, bx + rw, 70 + dy, 7.5), BLANKET, ao=0.3, bevel=7.5, spec=0.35)
        cv.add(cv.rrect(bx - rw * 0.7, 58 + dy, bx + rw * 0.7, 66 + dy, 4), BLANKET_LT, bevel=4, spec=0.2)
    put(bz, pack)

    for z, fn in sorted(behind, key=lambda t: t[0]):
        fn()
    ht = proj(19, 23)                              # veste: gonna a pieghe, busto, cintura
    skw = ht * 1.22
    sk = cv.poly([(cx - ht * 0.95, 88 + dy), (cx + ht * 0.95, 88 + dy), (cx + skw, 117 + dy), (cx - skw, 117 + dy)])
    cv.add(sk, WHITE_C, ao=0.4, bevel=7, spec=0.2, tex=pleat_tex(cv, 7.5, phi * 3))
    cv.add(cv.poly([(cx - skw - 0.2, 112.6 + dy), (cx + skw + 0.2, 112.6 + dy), (cx + skw + 0.4, 117.4 + dy), (cx - skw - 0.4, 117.4 + dy)]) & sk, GOLD, bevel=2, spec=0.6)
    cv.add(cv.rrect(cx - ht, 58 + dy, cx + ht, 99 + dy, min(13, ht - 0.5)), WHITE_C, ao=0.4, bevel=13, spec=0.5, shin=30)
    cv.add(cv.rrect(cx - ht, 89 + dy, cx + ht, 97 + dy, 2.5), HIDE, ao=0.2, bevel=3, spec=0.3)
    for z, fn in sorted(front, key=lambda t: t[0]):
        fn()
    if sn > -0.2:                                  # fibbia e gemma sul petto
        lx = cx + 18 * c
        lw = 6 * (0.4 + 0.6 * abs(sn))
        cv.add(cv.rrect(lx - lw, 88.4 + dy, lx + lw, 97.6 + dy, 2), GOLD, bevel=2.5, spec=0.8)
        gem = cv.poly([(lx, 67 + dy), (lx + lw * 0.8, 73 + dy), (lx, 79.5 + dy), (lx - lw * 0.8, 73 + dy)])
        cv.add(gem, (60, 214, 240), bevel=3, spec=0.9)
        cv.glow(gem, (80, 225, 255), radius=3, strength=0.3)
    cv.add(cv.ell(cx, 59 + dy, max(8, ht * 0.95), 6.4), (238, 242, 250), ao=0.3, bevel=6, spec=0.4)   # colletto

    hh = proj(30, 27)                              # testa
    ears = []
    for v in (-25, 25):
        ex, ez = px(0, v)
        ears.append((ez, ex))
    def ear(ex, ez):
        k = 0.4 + 0.6 * abs(c)
        cv.add(cv.ell(ex, 47 + dy, 6.6 * k + 1.4, 7.4), ORANGE_C, ao=0.3, bevel=5, spec=0.5)
        cv.add(cv.ell(ex + (2 if ez > 0 else -2) * 0, 47 + dy, 3 * k + 0.6, 3.4), METAL, bevel=2.5, spec=0.8)
    for ez, ex in ears:
        if ez < 0: ear(ex, ez)
    cv.add(cv.rrect(cx - hh, 18 + dy, cx + hh, 62 + dy, min(19, hh - 0.5)), WHITE_C, ao=0.45, bevel=19, spec=0.7, shin=34)
    if sn > -0.25:
        vw = min(12 + 9 * abs(sn), hh - 4)
        vx = min(cx + 16 * c, cx + hh - vw - 3) if c > 0 else max(cx + 16 * c, cx - hh + vw + 3)
        cv.add(cv.rrect(vx - vw, 27 + dy, vx + vw, 52 + dy, 10), (18, 24, 38), ao=0.3, bevel=9, spec=0.95, shin=40)
        e = 7 * (0.3 + 0.7 * abs(sn))
        ew = 3.8 * (0.55 + 0.45 * abs(sn))
        if abs(sn) < 0.4:                          # di profilo si vede un occhio solo
            eyes = cv.ell(vx + 5 * c, 40 + dy, 3.4, 6.2)
        else:
            eyes = cv.ell(vx - e, 40 + dy, ew, 6.2) | cv.ell(vx + e, 40 + dy, ew, 6.2)
        cv.add(eyes, (120, 236, 255), bevel=3, spec=0.5)
        cv.glow(eyes, (80, 220, 255), radius=3.5, strength=0.55)
    else:                                          # visto da dietro: pannello con prese d'aria
        pxc = cx - 12 * c
        pw = min(15, hh - 5)
        cv.add(cv.rrect(pxc - pw, 28 + dy, pxc + pw, 52 + dy, 6), (48, 52, 62), ao=0.3, bevel=6, spec=0.5)
        for k in range(3):
            cv.add(cv.rrect(pxc - pw + 4, 33 + dy + k * 6, pxc + pw - 4, 35.5 + dy + k * 6, 1), (96, 102, 114), bevel=1, spec=0.3)
    for ez, ex in ears:
        if ez >= 0: ear(ex, ez)
    ax = cx - 12 * c                               # antenna
    cv.add(cv.capsule(ax, 20 + dy, ax - 1, 8 + dy, 1.6), METAL, bevel=1.6, spec=0.6)
    ball = cv.ell(ax - 1, 7 + dy, 4.2, 4.2)
    cv.add(ball, (232, 48, 48), bevel=4.2, spec=0.9)
    cv.glow(ball, (255, 60, 60), radius=3, strength=0.3)
    return cv.result()


def gen_spin_hero():
    for i in range(SPIN_FRAMES):
        save("hero_spin_%d" % i, hero_spin(2 * math.pi * i / SPIN_FRAMES))


def shell_field(cv, mask_bbox, top, mid, low):
    """Campo di colore (h, w, 3) che sfuma dall'alto al basso: l'iridescenza della corazza."""
    y0, y1 = mask_bbox
    yy = (np.arange(cv.h)[:, None] / cv.ss - y0) / max(1e-6, (y1 - y0))
    t = np.clip(yy, 0, 1)
    top, mid, low = (np.array(c, dtype=float) / 255.0 for c in (top, mid, low))
    col = np.where(t[..., None] < 0.55, lerp(top, mid, np.clip(t / 0.55, 0, 1)[..., None]), lerp(mid, low, np.clip((t - 0.55) / 0.45, 0, 1)[..., None]))
    return np.broadcast_to(col, (cv.h, cv.w, 3)).copy()


def add_field(cv, mask, field, **kw):
    """Come cv.add, ma con un colore diverso in ogni punto."""
    kw["bevel"] = kw.get("bevel", 8.0) * cv.ss
    ao = kw.pop("ao", 0.0)
    if ao > 0:
        sh = ndi.gaussian_filter(mask.astype(float), 2.0 * cv.ss) * ao
        cv.over(np.dstack([np.zeros(mask.shape + (3,)), sh]))
    res = shade(mask, (255, 255, 255), **kw)
    res[..., :3] = np.clip(res[..., :3] * field, 0, 1)
    cv.over(res)


def beetle(step):
    """Cervo volante corazzato, rivolto a sinistra: corazza striata e cangiante con l'orlo dorato, grandi mandibole, antenne, sei zampe."""
    W, H = 104, 84
    cv = Cv(W, H, 3)
    LEG, LEG_DK = (86, 52, 130), (52, 32, 82)
    # zampe: tre paia, in tre tratti (coscia, tibia con spine, piede); quelle dall'altra parte del corpo sono più scure
    def leg(bx, far, flip):
        k = (-1 if (flip % 2) else 1)
        dx = 7 * k
        col = LEG_DK if far else LEG
        x0, y0 = bx + (6 if far else 0), 60
        kx, ky = x0 + dx * 0.55 - 6, 70
        ax, ay = x0 + dx - 9, 82
        cv.add(cv.capsule(x0, y0, kx, ky, 3.6), col, ao=0.3, bevel=3.4, spec=0.5)
        cv.add(cv.ell(kx, ky, 3.6, 3.6), col, bevel=3.4, spec=0.6)
        cv.add(cv.capsule(kx, ky, ax, ay - 2, 2.5), col, ao=0.2, bevel=2.4, spec=0.5)
        for t in (0.3, 0.6):
            sx, sy = kx + (ax - kx) * t, ky + (ay - ky) * t
            cv.add(cv.poly([(sx + 1, sy - 1.2), (sx + 4.2, sy - 0.2), (sx + 1, sy + 1.2)]), (40, 26, 60), bevel=1, spec=0.3)
        cv.add(cv.ell(ax - 1, ay - 0.4, 4.2, 2.2), (34, 24, 46), bevel=2.2, spec=0.3)
    for i, bx in enumerate((56, 76, 94)):
        leg(bx - 18, True, i + step + 1)
    # testa e mandibole (quelle lontane prima, scure)
    def mandible(dx, dy, base, tip):
        pts = [(15, 63), (8, 65), (3, 62), (0.8, 55), (3.5, 48)]
        rad = [4.6, 4.0, 3.3, 2.6, 1.5]
        for (x0, y0), (x1, y1), r0, r1 in zip(pts[:-1], pts[1:], rad[:-1], rad[1:]):
            m = cv.capsule(x0 + dx, y0 + dy, x1 + dx, y1 + dy, (r0 + r1) / 2)
            cv.add(m, tip if y1 < 56 else base, ao=0.2, bevel=(r0 + r1) / 2, spec=0.6, shin=18)
        cv.add(cv.poly([(5.2 + dx, 58 + dy), (10 + dx, 56 + dy), (7.5 + dx, 61.5 + dy)]), tip, bevel=1.4, spec=0.4)
    mandible(2, -7, (92, 48, 32), (140, 86, 58))
    cv.add(cv.ell(18, 62, 11, 9.5), (46, 34, 66), ao=0.4, bevel=9, spec=0.6)
    mandible(0, 0, (122, 64, 40), (184, 116, 76))
    # corazza: cupola bluviola con strisce longitudinali, riflesso e orlo dorato
    cx, cy, rx, ry = 66, 45, 33, 27
    shell = cv.ell(cx, cy, rx, ry) & cv.rrect(0, 0, W, 69, 0)
    field = shell_field(cv, (cy - ry, 69), (70, 104, 214), (122, 54, 182), (206, 98, 112))
    yy = np.arange(cv.h)[:, None] / cv.ss
    stripes = 0.84 + 0.16 * np.cos((yy - cy) * 2 * math.pi / 5.2)
    tex_noise = 0.92 + 0.14 * fbm(cv.h, cv.w, beta=1.6, seed=81)
    add_field(cv, shell, field * (stripes * tex_noise)[..., None], ao=0.5, bevel=28, spec=0.95, shin=36)
    rim = shell & ~(cv.ell(cx, cy - 2.4, rx, ry))
    cv.add(rim, (240, 190, 70), bevel=1.6, spec=0.7)
    cv.add(cv.capsule(cx + 2, cy - ry + 3, cx + 2, 66, 0.9), (44, 22, 74), bevel=1, spec=0.2)          # linea tra le elitre
    # torace: cupola più piccola e più scura davanti alla corazza
    thorax = cv.ell(35, 49, 17, 15) & cv.rrect(0, 0, W, 66, 0)
    add_field(cv, thorax, shell_field(cv, (34, 66), (58, 70, 190), (82, 48, 168), (140, 80, 150)) * (0.94 + 0.1 * fbm(cv.h, cv.w, beta=1.2, seed=83))[..., None],
              ao=0.45, bevel=15, spec=0.9, shin=30)
    # antenne con la clava e occhio giallo
    cv.add(cv.capsule(21, 53, 23, 41, 1.0), (60, 40, 40), bevel=1, spec=0.3)
    cv.add(cv.capsule(23, 41, 29, 36, 1.0), (60, 40, 40), bevel=1, spec=0.3)
    cv.add(cv.ell(30, 35, 3.6, 2.6), (222, 150, 58), bevel=2.6, spec=0.5)
    eye = cv.ell(15, 57, 4.4, 4.8)
    cv.add(eye, (252, 214, 60), bevel=4.4, spec=0.9)
    cv.glow(eye, (255, 220, 80), radius=3, strength=0.3)
    cv.add(cv.ell(14, 57.6, 1.7, 2.4), (20, 14, 10), bevel=1.5, spec=0.2)
    for i, bx in enumerate((56, 76, 94)):                   # zampe vicine, davanti al corpo
        leg(bx - 18, False, i + step)
    return cv.result()


def gen_enemy():
    f0 = beetle(0)
    save("enemy_0", f0)
    save("enemy_1", beetle(1))
    im = Image.fromarray((f0 * 255).astype(np.uint8), "RGBA").resize((104, 24), Image.LANCZOS)
    flat = Image.new("RGBA", (104, 84), (0, 0, 0, 0))
    flat.paste(im, (0, 60))
    save("enemy_flat", np.array(flat).astype(float) / 255.0)


def gen_flag():
    w, h = 20, T
    x = (np.arange(w) + 0.5) / w * 2 - 1
    nz = np.sqrt(np.clip(1 - x * x, 0, 1))
    diff = np.clip(x * LIGHT[0] + nz * LIGHT[2], 0, 1)
    spec = np.clip(x * HALF[0] + nz * HALF[2], 0, 1) ** 30
    c = np.array([176, 182, 192]) / 255.0
    row = c[None, :] * (0.3 + 0.8 * diff)[:, None] + 0.7 * spec[:, None]
    a = np.clip((1 - np.abs(x)) * w / 2, 0, 1)
    save("pole", np.broadcast_to(np.dstack([np.clip(row, 0, 1)[None], a[None, :, None]]), (h, w, 4)).copy())

    cv = Cv(40, 40, 3)
    cv.add(cv.ell(20, 20, 16, 16), (238, 186, 40), ao=0.4, bevel=16, spec=0.9, shin=16)
    save("pole_top", cv.result())

    fw, fh = 128, 84
    cv = Cv(fw, fh, 3)
    top = [(x, 6 + 3.5 * math.sin(x / 15.0)) for x in range(0, 121, 4)]
    tail = [(120, 6 + 3.5 * math.sin(8)), (98, 40), (122, 74)]
    bot = [(x, 74 + 3.5 * math.sin(x / 15.0 + 1.2)) for x in range(120, -1, -4)]
    m = cv.poly(top + tail + bot)
    xs = np.arange(cv.w)[None, :] / 3.0
    ys = np.arange(cv.h)[:, None] / 3.0
    wave = 0.82 + 0.26 * np.sin(xs / 13.0 + ys / 40.0) + 0.1 * (fbm(cv.h, cv.w, beta=1.2, seed=91) - 0.5)
    cv.add(m, (214, 44, 40), ao=0.3, bevel=2, spec=0.12, tex=wave)
    cv.add(cv.poly(star_pts(46, 40, 19, 8)) & m, (250, 244, 232), bevel=1.5, spec=0.1, tex=wave)
    save("flag", cv.result())


def gen_3d():
    """Texture per il livello 3D: erba vista dall'alto, ombra, joystick."""
    n = 256
    streak = fbm(n, n, beta=1.5, seed=101)
    fine = fbm(n, n, beta=0.7, seed=102)
    low = fbm(n, n, beta=2.4, seed=103)
    diff, _ = lit_from_height(streak * 22 + fine * 8, wrap=True)
    g_dark = np.array([44, 100, 30]) / 255.0
    g_light = np.array([126, 180, 60]) / 255.0
    t = np.clip(0.1 + 0.75 * streak + 0.45 * (fine - 0.5) + 0.35 * (low - 0.5), 0, 1)
    rgb = lerp(g_dark, g_light, t[..., None]) * (0.62 + 0.6 * diff)[..., None]
    save("tex_grass", down(np.clip(rgb, 0, 1), 2))

    y, x = np.mgrid[0:64, 0:64]
    d = np.sqrt((x - 31.5) ** 2 + (y - 31.5) ** 2) / 31.5
    a = np.clip((1 - d) * 2.4, 0, 1) * 0.58
    save("shadow", np.dstack([np.zeros((64, 64, 3)), a]))

    S, ss = 256, 3
    im = Image.new("RGBA", (S * ss, S * ss), (0, 0, 0, 0))
    d2 = ImageDraw.Draw(im)
    c, r = S * ss / 2, 120 * ss
    d2.ellipse([c - r, c - r, c + r, c + r], fill=(255, 255, 255, 34), outline=(255, 255, 255, 140), width=5 * ss)
    for ang in (0, 90, 180, 270):
        ca, sa = math.cos(math.radians(ang)), math.sin(math.radians(ang))
        tip = (c + ca * 104 * ss, c + sa * 104 * ss)
        b1 = (c + ca * 86 * ss - sa * 13 * ss, c + sa * 86 * ss + ca * 13 * ss)
        b2 = (c + ca * 86 * ss + sa * 13 * ss, c + sa * 86 * ss - ca * 13 * ss)
        d2.polygon([tip, b1, b2], fill=(255, 255, 255, 150))
    im.resize((S, S), Image.LANCZOS).save(os.path.join(OUT, "joy_base.png"), optimize=True)
    im = Image.new("RGBA", (128 * ss, 128 * ss), (0, 0, 0, 0))
    d2 = ImageDraw.Draw(im)
    d2.ellipse([6 * ss, 6 * ss, 122 * ss, 122 * ss], fill=(255, 255, 255, 120), outline=(255, 255, 255, 220), width=4 * ss)
    im.resize((128, 128), Image.LANCZOS).save(os.path.join(OUT, "joy_knob.png"), optimize=True)


def gen_buttons():
    S, ss = 192, 3
    for name in ("left", "right", "jump", "spin"):
        im = Image.new("RGBA", (S * ss, S * ss), (0, 0, 0, 0))
        d = ImageDraw.Draw(im)
        c, r = S * ss / 2, 88 * ss
        d.ellipse([c - r, c - r, c + r, c + r], fill=(255, 255, 255, 44), outline=(255, 255, 255, 150), width=5 * ss)
        g = (255, 255, 255, 225)
        if name == "spin":
            r0 = 38 * ss
            d.arc([c - r0, c - r0, c + r0, c + r0], start=35, end=330, fill=g, width=15 * ss)
            th = math.radians(330)
            px, py = 96 + 38 * math.cos(th), 96 + 38 * math.sin(th)
            tx, ty = -math.sin(th), math.cos(th)
            nx, ny = math.cos(th), math.sin(th)
            tip = (px + tx * 20, py + ty * 20)
            b1 = (px - nx * 17, py - ny * 17)
            b2 = (px + nx * 17, py + ny * 17)
            d.polygon([(x * ss, y * ss) for x, y in (tip, b1, b2)], fill=g)
            pts = []
        elif name == "jump":
            pts = [(96, 44), (146, 104), (114, 104), (114, 148), (78, 148), (78, 104), (46, 104)]
        else:
            pts = [(50, 96), (126, 46), (126, 146)]
            if name == "right":
                pts = [(S - x, y) for x, y in pts]
        if pts:
            d.polygon([(x * ss, y * ss) for x, y in pts], fill=g)
        im = im.resize((S, S), Image.LANCZOS)
        im.save(os.path.join(OUT, "btn_%s.png" % name), optimize=True)


def gen_icon(hero_img):
    S = 192
    y = np.linspace(0, 1, S)[:, None, None]
    bg = np.broadcast_to(lerp(SKY_TOP, SKY_HOR, y), (S, S, 3)).copy()
    bg[150:, :, :] = np.array([70, 140, 40]) / 255.0
    bgi = Image.fromarray((bg * 255).astype(np.uint8), "RGB").convert("RGBA")
    h = Image.fromarray((hero_img * 255).astype(np.uint8), "RGBA").resize((126, 162), Image.LANCZOS)
    bgi.alpha_composite(h, (33, 8))
    mask = Image.new("L", (S * 2, S * 2), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, S * 2 - 1, S * 2 - 1], radius=72, fill=255)
    bgi.putalpha(mask.resize((S, S), Image.LANCZOS))
    os.makedirs(ICON_OUT, exist_ok=True)
    bgi.save(os.path.join(ICON_OUT, "ic_launcher.png"), optimize=True)


if __name__ == "__main__":
    only = set(sys.argv[1:])
    os.makedirs(OUT, exist_ok=True)

    def want(k):
        return not only or k in only

    if want("sky"): gen_sky()
    if want("mountains"): gen_mountains()
    if want("hills"): gen_hills()
    if want("ground"): gen_ground()
    if want("brick"): gen_brick()
    if want("blocks"): gen_blocks()
    if want("gem"): gen_gem()
    if want("items"):
        gen_battery()
        gen_chip()
        gen_spinfx()
    if want("hero"):
        gen_hero()
        gen_icon(hero(7, -7, 10, -10, cp="icon"))
    if want("spin"): gen_spin_hero()
    if want("enemy"): gen_enemy()
    if want("flag"): gen_flag()
    if want("buttons"): gen_buttons()
    if want("3d"): gen_3d()
    print("ok")
