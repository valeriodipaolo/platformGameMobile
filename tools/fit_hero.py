#!/usr/bin/env python3
"""Riduce gli sprite del protagonista ricavati dal modello 3D (HeroSprites, 448x576) alla misura del gioco (112x144).

Uso:  python3 tools/fit_hero.py build/herospr
"""
import glob
import os
import sys

import numpy as np
from PIL import Image

RES = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "res", "drawable-nodpi")

for f in sorted(glob.glob(os.path.join(sys.argv[1], "hero_*.png"))):
    a = np.asarray(Image.open(f).convert("RGBA")).astype(float) / 255.0
    a[..., :3] *= a[..., 3:4]                                   # riduzione con alfa premoltiplicata: niente aloni scuri
    im = Image.fromarray((a * 255).astype(np.uint8), "RGBA").resize((112, 144), Image.LANCZOS)
    b = np.asarray(im).astype(float) / 255.0
    al = np.clip(b[..., 3:4], 1e-4, 1)
    b[..., :3] = np.clip(b[..., :3] / al, 0, 1)
    Image.fromarray((b * 255 + 0.5).astype(np.uint8), "RGBA").save(os.path.join(RES, os.path.basename(f)), optimize=True)
print("ok")
