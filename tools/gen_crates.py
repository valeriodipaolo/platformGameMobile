"""Casse di legno dell'isola per tutti i livelli: la cassa rinforzata (3D e 2D) e le casse dei livelli 2D.

- r3j_steel: cassa rinforzata 3D, legno con angolari, fasce e borchie d'acciaio (si rompe solo con la giravolta);
- crate2d, crate2d_used, crate2d_steel: le stesse casse per le caselle 2D (piena, vuota dopo il colpo, rinforzata).
"""
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, os.path.dirname(__file__))
import gen_jungle as J  # noqa: E402

OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "drawable-nodpi")


def steel_overlay(im):
    """Angolari, fasce a croce e borchie d'acciaio sopra una cassa di legno 128x128."""
    n = im.size[0]
    d = ImageDraw.Draw(im)
    steel, dark, light = (128, 132, 140), (62, 64, 70), (196, 200, 208)
    band = 12
    for y in (n // 2 - band // 2,):                               # fascia orizzontale e verticale
        d.rectangle([0, y, n, y + band], fill=steel)
        d.line([(0, y), (n, y)], fill=light, width=2)
        d.line([(0, y + band), (n, y + band)], fill=dark, width=2)
    for x in (n // 2 - band // 2,):
        d.rectangle([x, 0, x + band, n], fill=steel)
        d.line([(x, 0), (x, n)], fill=light, width=2)
        d.line([(x + band, 0), (x + band, n)], fill=dark, width=2)
    c = 34                                                        # angolari a L
    for (x0, y0, sx, sy) in ((0, 0, 1, 1), (n, 0, -1, 1), (0, n, 1, -1), (n, n, -1, -1)):
        pts = [(x0, y0), (x0 + sx * c, y0), (x0 + sx * c, y0 + sy * 14), (x0 + sx * 14, y0 + sy * 14), (x0 + sx * 14, y0 + sy * c), (x0, y0 + sy * c)]
        d.polygon(pts, fill=steel, outline=dark)
        d.line([(x0 + sx * 2, y0 + sy * 2), (x0 + sx * (c - 2), y0 + sy * 2)], fill=light, width=2)
        for (bx, by) in ((x0 + sx * 7, y0 + sy * 7), (x0 + sx * 24, y0 + sy * 7), (x0 + sx * 7, y0 + sy * 24)):
            d.ellipse([bx - 3, by - 3, bx + 3, by + 3], fill=light, outline=dark)
    m = n // 2                                                    # borchia centrale
    d.rectangle([m - 13, m - 13, m + 13, m + 13], fill=steel, outline=dark, width=2)
    for (bx, by) in ((m - 7, m - 7), (m + 7, m - 7), (m - 7, m + 7), (m + 7, m + 7)):
        d.ellipse([bx - 3, by - 3, bx + 3, by + 3], fill=light, outline=dark)
    return im


def tile(im, size=96):
    """Da texture 3D a casella 2D: ridimensiona, aggiunge un contorno scuro e una leggera ombra in basso a destra."""
    t = im.convert("RGBA").resize((size, size), Image.LANCZOS)
    a = np.asarray(t).astype(np.float32)
    yy, xx = np.mgrid[0:size, 0:size] / (size - 1)
    shade = 1.08 - 0.22 * (0.5 * xx + 0.5 * yy)                   # luce dall'alto a sinistra
    a[..., :3] *= shade[..., None]
    a[:2, :, :3] *= 0.35; a[-2:, :, :3] *= 0.35; a[:, :2, :3] *= 0.35; a[:, -2:, :3] *= 0.35
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


def used(im):
    """Cassa già aperta: legno scurito e un vano vuoto al centro."""
    t = im.copy()
    a = np.asarray(t).astype(np.float32)
    a[..., :3] *= 0.55
    t = Image.fromarray(a.astype(np.uint8), "RGBA")
    d = ImageDraw.Draw(t)
    s = t.size[0]
    d.rounded_rectangle([s * 0.24, s * 0.24, s * 0.76, s * 0.76], radius=s * 0.06, fill=(30, 20, 14, 255), outline=(14, 9, 6, 255), width=3)
    d.line([(s * 0.26, s * 0.3), (s * 0.74, s * 0.3)], fill=(60, 42, 28, 255), width=2)
    return t


def main():
    wood, _ = J.crate_base(3761)
    steel, _ = J.crate_base(3761)
    steel = steel_overlay(steel)
    J.save_img(steel, "r3j_steel")
    tile(wood).save(os.path.join(OUT, "crate2d.png"))
    used(tile(wood)).save(os.path.join(OUT, "crate2d_used.png"))
    tile(steel).save(os.path.join(OUT, "crate2d_steel.png"))


if __name__ == "__main__":
    main()
