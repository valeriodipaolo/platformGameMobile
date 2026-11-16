#!/usr/bin/env python3
"""Talpa dalla foto di riferimento (tools/ref/mole.jpg): le texture del modello 3D sono ritagli della foto.

  mole_front    il corpo visto da davanti (muso, naso, denti, baffi), con mani e sfondo sostituiti da pelo
  mole_fur      pelo ripetibile, per fianchi e schiena
  mole_hand_l/r le due mani, scontornate
  mole_shell_k  cinque sezioni di pelo a strati con i colori della foto (niente peli su naso e denti)
Uso:  python3 tools/gen_mole.py
"""
import os
import sys

import cv2
import numpy as np
from PIL import Image
from scipy import ndimage as ndi

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_assets3d as G3  # noqa: E402

HERE = os.path.dirname(os.path.abspath(__file__))
X0, Y0, SZ = 355, 310, 1200          # riquadro del corpo nella foto
N = 512
LAYERS = 5


def tileable(a, m):
    """Rende ripetibile un ritaglio sfumando i bordi (m pixel) con il lato opposto: prima in orizzontale, poi in verticale."""
    h, w = a.shape[:2]
    t = np.linspace(0, 1, m)[None, :, None]
    b = a[:, : w - m].copy()
    b[:, :m] = b[:, :m] * t + a[:, w - m:] * (1 - t)
    t = np.linspace(0, 1, m)[:, None, None]
    c = b[: h - m].copy()
    c[:m] = c[:m] * t + b[h - m:] * (1 - t)
    return c


def main():
    im = np.asarray(Image.open(os.path.join(HERE, "ref", "mole.jpg")).convert("RGB")).astype(np.float32) / 255.0
    lum = im @ np.array([0.3, 0.59, 0.11], np.float32)
    mx, mn = im.max(2), im.min(2)
    sat = (mx - mn) / np.maximum(mx, 1e-3)
    yy, xx = np.mgrid[0:1920, 0:1920]

    # pelo: scuro. mani: chiare e rosate, nei due riquadri. muso: naso, bocca e denti
    fur = ndi.binary_opening(lum < 0.42, iterations=2)
    fur = ndi.binary_fill_holes(ndi.binary_closing(fur, iterations=6))
    lab, n = ndi.label(fur)
    fur = lab == (np.argmax(ndi.sum(fur, lab, range(1, n + 1))) + 1)
    hand_box = [(395, 860, 720, 1300), (1340, 850, 1650, 1250)]
    skin = (lum > 0.43) & ((sat > 0.075) | (lum < 0.80))
    hands = np.zeros_like(fur)
    hmask = []
    for (x0, y0, x1, y1) in hand_box:
        m = np.zeros_like(fur)
        m[y0:y1, x0:x1] = skin[y0:y1, x0:x1]
        m = ndi.binary_opening(m, iterations=1)
        lab, n = ndi.label(ndi.binary_closing(m, iterations=3))
        big = lab == (np.argmax(ndi.sum(m, lab, range(1, n + 1))) + 1)
        m = ndi.binary_fill_holes(big & ndi.binary_dilation(m, iterations=3))
        hmask.append(m)
        hands |= m
    face = np.zeros_like(fur)
    face[800:1045, 1040:1260] = lum[800:1045, 1040:1260] > 0.42
    face = ndi.binary_dilation(ndi.binary_opening(face, iterations=3), iterations=5)

    # pelo ripetibile: un ritaglio della schiena, con la luce spianata
    src = im[520:520 + 320, 640:640 + 320]
    flat = src / np.maximum(cv2.GaussianBlur(src, (0, 0), 30), 1e-3) * src.mean((0, 1))
    tile = tileable(flat, 64)
    tile = np.clip(tile.mean((0, 1)) + (tile - tile.mean((0, 1))) * 0.6, 0, 1)             # peli chiari meno vistosi                                   # 256x256
    fur_mean = im[ndi.binary_erosion(fur, iterations=12)].mean(0)
    tile = np.clip(tile / tile.mean((0, 1)) * fur_mean, 0, 1)
    G3.save("mole_fur", np.dstack([tile, np.ones(tile.shape[:2])]))

    # corpo da davanti: dove non c'è pelo vero (mani, sfondo, ombra) si mette pelo ripetuto, con la luce dei dintorni
    solid = fur & ~ndi.binary_dilation(hands, iterations=10)
    solid &= ~ndi.binary_opening(lum > 0.6, iterations=4) | face          # via il bianco sotto la pancia (i baffi, sottili, restano)
    solid[1330:] = False
    solid = ndi.binary_erosion(solid, iterations=5)
    w = cv2.GaussianBlur(solid.astype(np.float32), (0, 0), 45)
    near = cv2.GaussianBlur(im * solid[..., None], (0, 0), 45) / np.maximum(w, 1e-3)[..., None]      # colore medio del pelo vicino
    far = cv2.GaussianBlur(im * solid[..., None], (0, 0), 160) / np.maximum(cv2.GaussianBlur(solid.astype(np.float32), (0, 0), 160), 1e-3)[..., None]
    k = np.clip(w * 6, 0, 1)[..., None]
    env = near * k + far * (1 - k)
    rep = np.tile(tile, (8, 8, 1))[:1920, :1920] / tile.mean((0, 1)) * env * 0.8
    a = cv2.GaussianBlur(solid.astype(np.float32), (0, 0), 6)[..., None]
    photo = im * a + rep * (1 - a)
    # La texture non è la foto dritta ma la sua "apertura" a densità uniforme sulla mezza sfera davanti: il centro è il punto che guarda in
    # avanti, il raggio è l'angolo (fino a 90 gradi sul bordo). Così il pelo non si stira sui fianchi; oltre i 55-75 gradi, dove la foto ha
    # pochi pixel, subentra il pelo ripetuto (con la luce della foto).
    g = (np.arange(N) + 0.5) / N * 2 - 1
    ga, gb = np.meshgrid(g, g)
    rr = np.maximum(np.hypot(ga, gb), 1e-6)
    th = np.minimum(rr, 1.0) * np.pi / 2
    ux, uy = np.sin(th) * ga / rr, -np.sin(th) * gb / rr
    mapx = (X0 + (0.5 + 0.45 * ux) * SZ).astype(np.float32)
    mapy = (Y0 + (0.5 - 0.354 * uy) * SZ).astype(np.float32)
    ph = cv2.remap(photo, mapx, mapy, cv2.INTER_AREA)
    ev = cv2.remap(env, mapx, mapy, cv2.INTER_LINEAR)
    w2 = np.clip((np.degrees(th) - 68) / 20, 0, 1)[..., None]
    ev = ev * (1 - w2) + fur_mean * w2                                   # sul bordo la luce è quella media: lì comincia la schiena
    tl = np.tile(tile, (2, 2, 1))[:N, :N] / tile.mean((0, 1)) * ev
    wgt = np.clip((np.degrees(th) - 52) / 22, 0, 1)[..., None]
    wgt = wgt * wgt * (3 - 2 * wgt)
    front = np.clip(ph * (1 - wgt) + tl * wgt, 0, 1)
    G3.save("mole_front", np.dstack([front, np.ones((N, N))]))

    # mani scontornate (il colore si allarga oltre il bordo, così il contorno non fa aloni)
    for i, (x0, y0, x1, y1) in enumerate(hand_box):
        m = hmask[i][y0:y1, x0:x1].astype(np.float32)
        c = im[y0:y1, x0:x1]
        wgt = cv2.GaussianBlur(m, (0, 0), 6)
        bleed = cv2.GaussianBlur(c * m[..., None], (0, 0), 6) / np.maximum(wgt, 1e-3)[..., None]
        c = c * m[..., None] + bleed * (1 - m[..., None])
        al = cv2.GaussianBlur(m, (0, 0), 1.2)
        side = max(x1 - x0, y1 - y0)
        pad = np.zeros((side, side, 4), np.float32)
        ox, oy = (side - (x1 - x0)) // 2, (side - (y1 - y0)) // 2
        pad[..., :3] = c.mean((0, 1))
        pad[oy:oy + y1 - y0, ox:ox + x1 - x0, :3] = c
        pad[oy:oy + y1 - y0, ox:ox + x1 - x0, 3] = al
        pad = cv2.resize(pad, (256, 256), interpolation=cv2.INTER_AREA)
        G3.save("mole_hand_" + "lr"[i], np.clip(pad, 0, 1))

    # pelo a strati con i colori della foto
    r = np.random.default_rng(4401)
    hgt = np.where(r.random((N, N)) < 0.15, np.clip(r.normal(0.78, 0.17, (N, N)), 0.3, 1.0), 0)
    nof = cv2.remap(face.astype(np.float32), mapx, mapy, cv2.INTER_LINEAR)
    nof = np.clip(cv2.GaussianBlur(nof, (0, 0), 1.5) * 2.5, 0, 1)
    for kk in range(LAYERS):
        t = (kk + 1) / (LAYERS + 1)
        pts = (hgt >= t).astype(np.float32)
        al = np.clip(cv2.GaussianBlur(pts, (0, 0), 0.95 - 0.45 * t) * (5.0 + 3.0 * t), 0, 1) * (1 - nof)
        col = np.clip(front * (1.0 + 0.08 * t), 0, 1)
        G3.save("mole_shell_%d" % kk, np.dstack([col, al]))
        bt = np.tile(tile, (2, 2, 1))[:256, :256]
        G3.save("mole_bshell_%d" % kk, np.dstack([np.clip(bt * (1.0 + 0.08 * t), 0, 1), al[:256, :256] / np.maximum(1 - nof[:256, :256], 1e-3)]))
        print("strato", kk, round(float((al > 0.45).mean()), 2), flush=True)
    # sprite 2D: la foto stessa, scontornata, che spunta dal cumulo di terra (fx_mound, ricavato dal modello 3D)
    cut = (fur | hands | face).astype(np.float32)
    cut[1262:] = 0
    al = cv2.GaussianBlur(cut, (0, 0), 2.0)
    wg = cv2.GaussianBlur(cut, (0, 0), 5)
    bleed = cv2.GaussianBlur(im * cut[..., None], (0, 0), 5) / np.maximum(wg, 1e-3)[..., None]
    rgba = np.dstack([im * cut[..., None] + bleed * (1 - cut[..., None]), al])
    x0, x1, y0, y1 = 385, 1660, 440, 1262
    S = 4
    sc = 106 * S / (x1 - x0)
    rgba[..., :3] *= rgba[..., 3:4]
    small = cv2.resize(rgba[y0:y1, x0:x1], (106 * S, int((y1 - y0) * sc)), interpolation=cv2.INTER_AREA)
    can = np.zeros((112 * S, 112 * S, 4), np.float32)
    top = 112 * S - 26 * S - small.shape[0]
    can[top:top + small.shape[0], 3 * S:3 * S + small.shape[1]] = small
    mound = Image.open(os.path.join(HERE, "..", "app", "src", "main", "res", "drawable-nodpi", "fx_mound.png")).convert("RGBA").resize((112 * S, 48 * S), Image.LANCZOS)
    md = np.asarray(mound).astype(np.float32) / 255.0
    md[..., :3] *= md[..., 3:4]
    reg = can[64 * S:]
    reg[:] = md + reg * (1 - md[..., 3:4])
    out = cv2.resize(can, (112, 112), interpolation=cv2.INTER_AREA)
    out[..., :3] = out[..., :3] / np.maximum(out[..., 3:4], 1e-3)
    G3.save("fx_mole", np.clip(out, 0, 1))


if __name__ == "__main__":
    main()
