#!/usr/bin/env python3
"""Texture realistiche per i nemici: piumaggio dell'allocco (corpo e remiganti) e piumino del pinguino.
Sono quasi neutre: il colore (petto chiaro, dorso bruno, nero e bianco del pinguino) lo mettono i vertici del modello,
la texture aggiunge piume, barbe, barrature e ombre tra una piuma e l'altra. Si ripetono senza cuciture.

Uso: python3 tools/gen_foes_real.py
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw
from scipy import ndimage as ndi

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_assets import fbm, save  # noqa: E402


def feathers(S, cols, rows, seed, bar=0.0, streak=0.0, contrast=1.0):
    """Squame di piume sovrapposte a file sfalsate: ogni piuma ha rachide, barbe, punta più scura e fa ombra su quella sotto."""
    r = np.random.default_rng(seed)
    ss = 2
    W = S * ss
    lum = np.full((W, W), 0.55)
    im = Image.fromarray((lum * 255).astype(np.uint8))
    d = ImageDraw.Draw(im)
    fw, fh = W / cols, W / rows
    # dall'alto in basso: le piume sotto coprono quelle sopra (come sul corpo, punta verso il basso)
    for j in range(rows + 2):
        for i in range(cols + 1):
            cx = (i + (0.5 if j % 2 else 0) + r.uniform(-0.15, 0.15)) * fw
            cy = (j - 0.5 + r.uniform(-0.1, 0.1)) * fh
            w, h = fw * r.uniform(0.62, 0.75), fh * r.uniform(1.15, 1.4)
            base = r.uniform(0.62, 0.86)
            for k in range(14, 0, -1):                      # dal bordo scuro al centro chiaro
                t = k / 14
                v = base * (0.55 + 0.45 * (1 - t) ** 0.6)
                if bar > 0:
                    v *= 1 - bar * (0.5 + 0.5 * math.cos(t * math.pi * 5))
                for ox in (-W, 0, W):
                    for oy in (-W, 0, W):
                        x0, y0 = cx + ox - w * t, cy + oy - h * 0.15 * t
                        d.ellipse([x0, y0, cx + ox + w * t, cy + oy + h * t], fill=int(np.clip(v, 0, 1) * 255))
            # rachide e barbe
            for ox in (-W, 0, W):
                for oy in (-W, 0, W):
                    d.line([(cx + ox, cy + oy), (cx + ox, cy + oy + h * 0.9)], fill=int(base * 0.6 * 255), width=max(1, ss))
                    for q in range(9):
                        yy = cy + oy + h * (0.1 + 0.09 * q)
                        for sd in (-1, 1):
                            d.line([(cx + ox, yy), (cx + ox + sd * w * 0.8, yy + h * 0.18)], fill=int(base * 0.82 * 255), width=1)
            if streak > 0 and r.random() < 0.6:          # goccia scura in punta (petto dell'allocco)
                for ox in (-W, 0, W):
                    for oy in (-W, 0, W):
                        d.ellipse([cx + ox - w * 0.12, cy + oy + h * 0.45, cx + ox + w * 0.12, cy + oy + h * 0.95], fill=int(base * (1 - streak) * 255))
    a = np.asarray(im.resize((S, S), Image.LANCZOS)).astype(float) / 255.0
    a = 0.5 + (a - 0.5) * contrast
    a *= 0.9 + 0.2 * fbm(S, S, beta=1.0, seed=seed + 1)
    return np.clip(a, 0, 1)


def to_rgb(l, tint):
    return np.clip(l[..., None] * np.array(tint)[None, None, :], 0, 1)


def plumage(S, seed, tips=1400, tip_len=(10, 22), streaks=0.5, drops=0.0):
    """Piumaggio visto da vicino: venature fitte nel verso delle piume, punte morbide a mezzaluna (luce sopra, ombra
    sotto), qualche goccia scura. Niente forme geometriche ripetute."""
    r = np.random.default_rng(seed)
    l = 0.62 + 0.25 * (fbm(S, S, beta=2.2, seed=seed) - 0.5)
    vein = fbm(S, S, beta=1.4, seed=seed + 1, ax=3.0, ay=0.35)               # strie verticali sottili
    l += streaks * 0.5 * (vein - 0.5)
    lay = Image.new("L", (S, S), 128)
    d = ImageDraw.Draw(lay)
    for _ in range(tips):
        x, y = r.uniform(0, S), r.uniform(0, S)
        w = r.uniform(*tip_len)
        h = w * r.uniform(0.35, 0.55)
        for ox in (-S, 0, S):
            for oy in (-S, 0, S):
                d.arc([x + ox - w / 2, y + oy - h, x + ox + w / 2, y + oy + h], 20, 160, fill=70, width=2)    # ombra sotto la punta
                d.arc([x + ox - w / 2, y + oy - h - 2, x + ox + w / 2, y + oy + h - 2], 30, 150, fill=190, width=1)  # bordo chiaro
        if drops > 0 and r.random() < drops:
            for ox in (-S, 0, S):
                for oy in (-S, 0, S):
                    d.ellipse([x + ox - w * 0.1, y + oy - h * 1.6, x + ox + w * 0.1, y + oy + h * 0.2], fill=40)
    t = (ndi.gaussian_filter(np.asarray(lay).astype(float), 0.7, mode="wrap") - 128) / 128
    l += t * 0.32
    return np.clip(l, 0, 1)


def owl():
    body = plumage(512, 2001, tips=900, tip_len=(18, 36), streaks=0.6, drops=0.1)
    save("foe_owl", to_rgb(body * 1.3, (1.0, 0.97, 0.92)))
    # remiganti: barre scure di traverso (lungo la penna, v) e barbe fitte nel verso della penna
    S = 512
    yy = np.arange(S)[:, None] / S
    bars = 0.5 + 0.5 * np.sin(yy * 2 * math.pi * 7 + (fbm(S, S, beta=2.0, seed=2003) - 0.5) * 3)
    barbs = fbm(S, S, beta=1.0, seed=2004, ax=0.3, ay=3.0)
    wing = 0.7 - 0.32 * (bars ** 2) + 0.18 * (barbs - 0.5)
    save("foe_owlwing", to_rgb(np.clip(wing * 1.3, 0, 1), (1.0, 0.95, 0.88)))


def penguin():
    """Piumino fitto e corto: piumette minuscole e morbide, nessun riflesso netto."""
    l = plumage(512, 2101, tips=5200, tip_len=(4, 8), streaks=0.25)
    fine = fbm(512, 512, beta=0.5, seed=2102)
    save("foe_pengfur", to_rgb(np.clip((l * 0.85 + fine * 0.2) * 1.2, 0, 1), (1.0, 1.0, 1.0)))


if __name__ == "__main__":
    owl()
    penguin()
    print("fatto")
