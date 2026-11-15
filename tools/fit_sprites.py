#!/usr/bin/env python3
"""Ritaglia gli sprite 2D renderizzati da FoeSprites e li adatta alle dimensioni dei file del gioco (centrati in basso, margine minimo).

Uso:  python3 tools/fit_sprites.py build/spr [nome ...]   (scrive in app/src/main/res/drawable-nodpi)
"""
import os
import sys

from PIL import Image

RES = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "res", "drawable-nodpi")
# dimensioni finali (se il file non esiste ancora) e margine (frazione) per nome
SIZE = {"fx_plant_o": (104, 104)}
PAD = {"fx_snowball": 0.06}


def main(src, names):
    files = [f[:-4] for f in sorted(os.listdir(src)) if f.endswith(".png")]
    for n in files:
        if names and n not in names:
            continue
        im = Image.open(os.path.join(src, n + ".png")).convert("RGBA")
        bb = im.getchannel("A").point(lambda v: 255 if v > 24 else 0).getbbox()
        if not bb:
            print("vuoto", n)
            continue
        im = im.crop(bb)
        dst = os.path.join(RES, n + ".png")
        W, H = SIZE.get(n) or Image.open(dst).size
        pad = PAD.get(n, 0.02)
        sc = min(W * (1 - 2 * pad) / im.width, H * (1 - pad) / im.height)
        tw, th = max(1, round(im.width * sc)), max(1, round(im.height * sc))
        im = im.resize((tw, th), Image.LANCZOS)
        out = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        out.paste(im, ((W - tw) // 2, H - th - round(H * pad * 0.3)), im)
        out.save(dst, optimize=True)
        print("ok", n, (W, H))


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2:])
