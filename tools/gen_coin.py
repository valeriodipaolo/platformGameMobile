"""Gettone d'oro (collezionabile segreto di ogni livello): disco dorato con bordo zigrinato e stella in rilievo.

Genera app/src/main/res/drawable-nodpi/coin.png (96x96) illuminando una mappa delle altezze, sovracampionata 4x.
"""
import os
import numpy as np
from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "drawable-nodpi")
N, SS = 96, 4
M = N * SS


def star_mask(x, y, r_out, r_in, rot=-np.pi / 2):
    ang = np.arctan2(y, x) - rot
    rad = np.hypot(x, y)
    k = (ang % (2 * np.pi / 5)) / (2 * np.pi / 5)          # 0..1 dentro uno spicchio
    t = np.abs(k - 0.5) * 2                                 # 1 sulle punte, 0 tra le punte
    edge = r_in + (r_out - r_in) * t ** 2.6
    return np.clip((edge - rad) * M * 0.5, 0, 1)


def main():
    c = (M - 1) / 2
    yy, xx = np.mgrid[0:M, 0:M].astype(np.float32)
    x, y = (xx - c) / c, (yy - c) / c
    r = np.hypot(x, y)
    disc = np.clip((0.93 - r) * M * 0.5, 0, 1)
    # altezze: bordo rialzato con zigrinatura, campo ribassato, stella rialzata e bisellata
    rim = np.clip((r - 0.74) / 0.06, 0, 1) * np.clip((0.93 - r) / 0.05, 0, 1)
    knurl = 0.5 + 0.5 * np.cos(np.arctan2(y, x) * 64)
    h = 0.55 * rim + 0.08 * rim * knurl
    h += 0.12 * np.clip((0.74 - r) / 0.05, 0, 1) * np.clip((r - 0.62) / 0.04, 0, 1)      # anello sottile dentro il bordo
    st = star_mask(x, y, 0.54, 0.22)
    bevel = star_mask(x, y, 0.46, 0.17)
    h += 0.30 * st + 0.18 * bevel
    h -= 0.05 * (1 - r)
    # luce dall'alto a sinistra
    gy, gx = np.gradient(h * 40)
    nz = 1.0
    nl = np.sqrt(gx ** 2 + gy ** 2 + nz ** 2)
    lx, ly, lz = -0.55, -0.65, 0.52
    diff = np.clip((-gx * lx - gy * ly + nz * lz) / nl, 0, 1)
    spec = np.clip((-gx * lx - gy * ly + nz * lz) / nl, 0, 1) ** 18
    # oro: dal bruno caldo al giallo chiaro
    dark = np.array([110, 62, 10], np.float32)
    mid = np.array([222, 158, 34], np.float32)
    lite = np.array([255, 232, 140], np.float32)
    t = diff[..., None]
    col = np.where(t < 0.55, dark + (mid - dark) * (t / 0.55), mid + (lite - mid) * ((t - 0.55) / 0.45))
    col = col + spec[..., None] * 120
    col *= (0.92 + 0.08 * (1 - r))[..., None]
    col = np.clip(col, 0, 255)
    # contorno scuro sottile per staccare dallo sfondo
    outline = np.clip((0.97 - r) * M * 0.5, 0, 1) - disc
    col = col * disc[..., None] + np.array([70, 38, 6], np.float32) * outline[..., None]
    alpha = np.clip(disc + outline, 0, 1)
    img = np.dstack([col, alpha[..., None] * 255]).astype(np.uint8)
    im = Image.fromarray(img, "RGBA").resize((N, N), Image.LANCZOS)
    im.save(os.path.join(OUT, "coin.png"))


if __name__ == "__main__":
    main()
