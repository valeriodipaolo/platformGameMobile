#!/usr/bin/env python3
"""Nemici 2D di base, stilizzati come quelli della seconda serie (contorno scuro, forme tonde, occhi espressivi), con un
animale diverso per ogni stagione e lo stesso comportamento:

  tema 0 estate:    cervo volante, istrice, calabrone, cavalletta      (lumaca -> tartaruga)
  tema 1 autunno:   topo selvatico, riccio, cornacchia, scoiattolo
  tema 2 inverno:   ermellino, riccio di ghiaccio, gazza, lepre bianca
  tema 3 incendio:  cardinale (coleottero rosso), riccio di brace, pipistrello, tizzone
  tema 4 primavera: coccinella, processionaria, rondine, coniglio      (lumaca)
  tema 5 cielo:     gabbiano, riccio di nuvola, falco, batuffolo

Per ogni tema: s2_<t>_0 .. s2_<t>_9 = camminatore (2 passi + schiacciato), spinoso (2), volatile (2), saltatore (terra, aria,
schiacciato). Lumache: s2_sn_0..2 (tartaruga: cammina, nel guscio, ribaltata), s2_sn_3..5 (lumaca: cammina, guscio, nuda).
Tutti guardano a sinistra.

Uso:  python3 tools/gen_foes4.py
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_foes2 import C, INK, OUT  # noqa: E402
from PIL import Image  # noqa: E402

W = (255, 255, 255, 255)
BLK = (34, 28, 30, 255)


def col(r, g, b, a=255):
    return (r, g, b, a)


def leg(c, x, y, len_, ang, fill, w=7):
    x1, y1 = x + math.sin(ang) * len_, y + math.cos(ang) * len_
    c.line([(x, y), (x1, y1)], INK, w + 4)
    c.line([(x, y), (x1, y1)], fill, w)
    return x1, y1


def spots(c, pts, r, fill):
    for x, y in pts:
        c.e(x, y, r, r, fill, 0)


def squash(src, name, keep=0.4):
    """Versione schiacciata (per i nemici pestati): stessa tela, alta il 40%, appoggiata in basso."""
    im = Image.open(os.path.join(OUT, src + ".png"))
    w, h = im.size
    s = im.resize((int(w * 1.1), max(1, int(h * keep))), Image.LANCZOS)
    o = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    o.alpha_composite(s.crop(((s.width - w) // 2, 0, (s.width - w) // 2 + w, s.height)), (0, h - s.height))
    o.save(os.path.join(OUT, name + ".png"), optimize=True)


# ------------------------------------------------------------------ camminatori (104x84)

def stag(c, f):
    sw = 6 if f else -6
    for i, x in enumerate((40, 56, 72)):
        leg(c, x, 60, 20, (0.5 if (i + f) % 2 else -0.4), BLK, 6)
    c.e(62, 50, 30, 20, col(122, 58, 26))                         # elitre castane
    c.line([(62, 32), (64, 68)], INK, 3)
    c.e(54, 42, 9, 5, col(180, 104, 60), 0)                       # riflesso
    c.e(30, 52, 14, 12, col(40, 32, 30))                          # testa nera
    for dy, s in ((-1, 1), (1, -1)):                             # mandibole a palco
        c.line([(22, 50 + dy * 4), (8, 40 + dy * 6 + sw * 0.3), (6, 30 + dy * 10)], INK, 9)
        c.line([(22, 50 + dy * 4), (8, 40 + dy * 6 + sw * 0.3), (6, 30 + dy * 10)], col(150, 70, 34), 5)
    c.eye(28, 48, 5, -1, 1)


def mouse(c, f):
    c.line([(88, 62), (100, 50), (98, 36)], INK, 6); c.line([(88, 62), (100, 50), (98, 36)], col(214, 160, 156), 3)   # coda
    for i, x in enumerate((44, 74)):
        leg(c, x, 66, 12, 0.45 if (i + f) % 2 else -0.35, col(214, 160, 156), 6)
    c.e(62, 56, 28, 20, col(150, 116, 86))
    c.e(58, 64, 18, 10, col(226, 206, 180), 0)
    c.e(38, 32, 12, 12, col(150, 116, 86)); c.e(38, 32, 7, 7, col(232, 170, 170), 0)   # orecchio
    c.e(28, 54, 18, 15, col(150, 116, 86))
    c.e(12, 58, 4, 4, col(220, 110, 120), 2)                      # naso
    c.eye(26, 50, 6, -1, 1)
    for k in (-1, 1):
        c.line([(14, 58), (2, 56 + k * 5)], INK, 1.5)


def ermine(c, f):
    c.line([(84, 56), (96, 48), (100, 40)], INK, 12); c.line([(84, 56), (96, 48), (100, 40)], W, 7)
    c.e(100, 38, 6, 6, BLK, 2)                                    # punta della coda nera
    for i, x in enumerate((40, 50, 70, 80)):
        leg(c, x, 66, 12, 0.4 if (i + f) % 2 else -0.3, col(236, 236, 240), 6)
    c.rr(30, 46, 90, 70, 12, col(246, 246, 250))
    c.e(26, 50, 18, 15, col(246, 246, 250))
    c.e(32, 34, 6, 6, col(246, 246, 250)); c.e(32, 34, 3, 3, col(240, 200, 200), 0)
    c.e(10, 54, 3, 3, BLK, 2)
    c.eye(22, 47, 6, -1, 1)


def cardinal(c, f):
    for i, x in enumerate((40, 56, 72)):
        leg(c, x, 60, 18, (0.5 if (i + f) % 2 else -0.4), BLK, 5)
    c.e(62, 50, 30, 18, col(222, 48, 30))
    c.line([(62, 33), (63, 67)], INK, 3)
    c.e(54, 42, 9, 4, col(255, 140, 110), 0)
    c.e(32, 52, 12, 11, col(40, 30, 30))
    for dy in (-1, 1):                                           # antenne a pettine
        pts = [(24, 46 + dy * 2), (12, 36 + dy * 6), (4, 30 + dy * 10)]
        c.line(pts, INK, 3)
        for k in range(3):
            c.line([(18 - k * 5, 42 + dy * (3 + k * 3)), (16 - k * 5, 46 + dy * (3 + k * 3) - 8)], INK, 2)
    c.eye(30, 49, 5, -1, 1)


def ladybug(c, f):
    for i, x in enumerate((44, 58, 72)):
        leg(c, x, 62, 14, (0.5 if (i + f) % 2 else -0.4), BLK, 5)
    c.e(62, 52, 30, 22, col(226, 34, 30))
    c.line([(62, 30), (62, 74)], INK, 3)
    spots(c, [(50, 44), (74, 44), (48, 62), (76, 62), (62, 54)], 6, BLK)
    c.e(52, 40, 6, 3, col(255, 150, 140), 0)
    c.e(32, 54, 13, 12, BLK)
    c.e(26, 60, 3, 3, W, 0); c.e(38, 60, 3, 3, W, 0)
    c.eye(28, 50, 5, -1, 1)


def gull(c, f):
    leg(c, 54, 62, 18, 0.3 if f else -0.2, col(240, 150, 60), 5)
    leg(c, 64, 62, 18, -0.2 if f else 0.3, col(240, 150, 60), 5)
    c.poly([(80, 48), (102, 40), (98, 56)], col(70, 74, 84))      # coda
    c.e(60, 50, 26, 16, W)
    c.poly([(48, 42), (88, 40), (96, 54), (60, 56)], col(160, 168, 182))   # ala grigia
    c.poly([(84, 44), (98, 46), (96, 54)], BLK, 2)
    c.e(32, 36, 13, 12, W)
    c.poly([(20, 36), (4, 40), (20, 42)], col(250, 200, 50), 2)   # becco giallo
    c.e(9, 40, 2, 2, col(220, 40, 30), 0)
    c.eye(30, 33, 5, -1, 1)


WALKERS = [stag, mouse, ermine, cardinal, ladybug, gull]


# ------------------------------------------------------------------ spinosi (128x88)

def spiky(c, f, body, spine, tip, face, quill_len=26, nose=col(40, 30, 30)):
    bob = 2 if f else 0
    for i, x in enumerate((50, 86)):
        leg(c, x, 72, 10, 0.4 if (i + f) % 2 else -0.3, face, 7)
    cx, cy = 74, 52 + bob
    for k in range(17):                                          # aculei a raggiera sul dorso
        a = math.pi * (0.05 + 0.9 * k / 16)
        ln = quill_len * (0.8 + 0.25 * ((k * 7) % 3) / 2)
        x0, y0 = cx - math.cos(a) * 26, cy - math.sin(a) * 16
        x1, y1 = cx - math.cos(a) * (26 + ln), cy - math.sin(a) * (16 + ln * 0.8)
        c.line([(x0, y0), (x1, y1)], INK, 8)
        c.line([(x0, y0), (x1, y1)], spine, 5)
        c.line([((x0 + 2 * x1) / 3, (y0 + 2 * y1) / 3), (x1, y1)], tip, 4)
    c.e(cx, cy, 34, 20, body)
    c.e(34, 58 + bob, 18, 14, face)                               # muso chiaro
    c.e(16, 62 + bob, 5, 4, nose, 2)
    c.eye(32, 54 + bob, 6, -1, 1)


def porcupine(c, f): spiky(c, f, col(50, 44, 40), col(240, 236, 226), BLK, col(90, 76, 66), 34)
def hedgehog(c, f): spiky(c, f, col(120, 84, 56), col(150, 108, 70), col(240, 222, 190), col(214, 184, 146))
def icehog(c, f): spiky(c, f, col(170, 200, 230), col(214, 236, 255), W, col(236, 244, 252), 26, col(80, 110, 160))
def magmahog(c, f): spiky(c, f, col(60, 40, 36), col(70, 44, 36), col(255, 140, 30), col(120, 70, 50))
def cloudhog(c, f): spiky(c, f, col(220, 228, 240), W, col(200, 220, 255), col(250, 250, 255), 18, col(120, 140, 180))


def caterpillar(c, f):
    wave = 4 if f else -4
    for k in range(6):
        x = 100 - k * 14
        y = 58 + (wave if k % 2 else -wave) * 0.6
        for a in range(-3, 4):                                   # peli lunghi chiari
            ang = a * 0.35
            c.line([(x, y - 8), (x + math.sin(ang) * 20, y - 8 - math.cos(ang) * 22)], col(230, 210, 170), 2)
        c.e(x, y, 11, 12, col(120, 96, 80))
        c.e(x, y - 6, 4, 3, col(220, 110, 40), 0)
        leg(c, x, y + 10, 6, 0, BLK, 5)
    c.e(26, 56, 13, 12, col(60, 46, 40))
    c.eye(24, 52, 5, -1, 1)


SPIKIES = [porcupine, hedgehog, icehog, magmahog, caterpillar, cloudhog]


# ------------------------------------------------------------------ volatili (120x96)

def bird(c, f, back, belly, wingc, beak, tail="fan", bars=False, white_patch=False, cheek=None):
    up = f == 0
    if tail == "fork":
        c.poly([(76, 52), (114, 40), (98, 54), (116, 64), (76, 60)], back)
    elif tail == "long":
        c.poly([(76, 50), (116, 56), (114, 64), (76, 60)], back)
    else:
        c.poly([(76, 48), (108, 44), (110, 62), (76, 60)], back)
    if not up:                                                   # ala in basso (dietro il corpo)
        c.poly([(50, 54), (86, 52), (72, 88), (60, 86)], wingc)
    c.e(58, 54, 26, 17, back)
    c.e(52, 60, 18, 10, belly, 0)
    if bars:
        for k in range(4):
            c.line([(40 + k * 7, 56), (44 + k * 7, 66)], col(150, 110, 70), 2)
    c.e(30, 40, 15, 14, back)
    if cheek:
        c.e(28, 48, 8, 6, cheek, 0)
    c.poly([(18, 38), (2, 44), (18, 48)], beak, 2)
    c.eye(28, 37, 6, -1, 1)
    if up:                                                       # ala alzata
        c.poly([(48, 46), (90, 44), (96, 6), (70, 14)], wingc)
        if white_patch:
            c.poly([(76, 14), (94, 10), (90, 30)], W, 0)
        for k in range(3):
            c.line([(66 + k * 8, 40), (78 + k * 6, 14)], INK, 2)


def hornet(c, f):
    c.e(84, 56, 22, 14, col(230, 170, 30))                        # addome a strisce
    for k in range(3):
        c.line([(74 + k * 10, 44), (76 + k * 10, 68)], BLK, 5)
    c.poly([(104, 56), (116, 58), (104, 60)], BLK, 0)
    c.e(54, 52, 14, 12, col(120, 60, 30))
    c.e(32, 50, 13, 12, col(230, 170, 30))
    c.eye(30, 46, 6, -1, 1)
    c.line([(26, 40), (16, 24), (8, 22)], INK, 3)
    wy = 10 if f == 0 else 30
    for dx in (0, 12):
        c.e(56 + dx, wy + 8, 12, 18, col(220, 236, 250, 180), 2)
    for i, x in enumerate((46, 56)):
        leg(c, x, 62, 14, -0.4, BLK, 4)


def bat(c, f):
    up = f == 0
    yw = 4 if up else 70
    for s in (-1, 1):
        x0 = 60
        tipx = 60 + s * 56
        pts = [(x0, 46), (tipx, yw), (60 + s * 46, yw + 14 if up else yw - 14), (60 + s * 34, yw + 6 if up else yw - 6), (60 + s * 22, 56)]
        c.poly(pts, col(70, 50, 60))
    c.e(60, 54, 18, 16, col(90, 64, 70))
    c.poly([(48, 42), (44, 22), (56, 38)], col(90, 64, 70)); c.poly([(72, 42), (76, 22), (64, 38)], col(90, 64, 70))
    c.eye(52, 50, 5, -1, 1); c.eye(68, 50, 5, -1, 0)
    c.poly([(54, 62), (57, 68), (60, 62)], W, 0); c.poly([(62, 62), (65, 68), (68, 62)], W, 0)


def crowb(c, f): bird(c, f, col(36, 36, 48), col(54, 56, 74), col(30, 30, 42), col(50, 46, 46), "fan")
def magpie(c, f): bird(c, f, col(28, 30, 40), W, col(40, 60, 110), col(40, 36, 36), "long", white_patch=True)
def swallow(c, f): bird(c, f, col(32, 44, 100), col(244, 236, 222), col(30, 36, 80), col(30, 26, 26), "fork", cheek=col(200, 60, 40))
def hawk(c, f): bird(c, f, col(140, 96, 58), col(236, 220, 190), col(120, 82, 50), col(240, 200, 60), "fan", bars=True)


FLYERS = [hornet, crowb, magpie, bat, swallow, hawk]


# ------------------------------------------------------------------ saltatori (96x106): f 0 a terra, 1 in aria

def grasshopper(c, f):
    air = f == 1
    if air:
        leg(c, 66, 70, 30, 0.9, col(110, 150, 50), 6)
    else:
        c.line([(66, 72), (86, 50), (88, 100)], INK, 10); c.line([(66, 72), (86, 50), (88, 100)], col(110, 150, 50), 6)
    c.e(56, 74, 30, 13, col(130, 176, 64))
    c.poly([(40, 64), (90, 66), (84, 76)], col(100, 140, 50))      # ala chiusa
    for i, x in enumerate((36, 48)):
        leg(c, x, 84, 16 if not air else 12, -0.3 if not air else 0.6, col(110, 150, 50), 4)
    c.e(26, 70, 13, 14, col(140, 186, 70))
    c.eye(22, 66, 6, -1, 1)
    c.line([(26, 58), (40, 30), (60, 20)], INK, 3)


def squirrel(c, f):
    air = f == 1
    c.e(74, 50 if not air else 60, 20, 30, col(176, 84, 36))      # coda folta
    c.e(76, 44 if not air else 56, 12, 20, col(206, 112, 54), 0)
    c.e(52, 78, 22, 20, col(186, 92, 40))
    c.e(46, 84, 12, 12, col(244, 228, 200), 0)
    if air:
        leg(c, 60, 92, 16, 1.0, col(176, 84, 36), 8)
    else:
        c.e(60, 96, 14, 6, col(176, 84, 36))
    c.e(30, 60, 16, 15, col(186, 92, 40))
    c.poly([(30, 46), (34, 30), (40, 46)], col(186, 92, 40))      # orecchio con ciuffo
    c.e(16, 64, 3, 3, BLK, 2)
    c.eye(28, 57, 6, -1, 1)


def hare(c, f, body=col(248, 248, 252), inner=col(240, 200, 206), tipb=True):
    air = f == 1
    c.e(76, 80, 8, 8, body)                                      # codino
    c.e(54, 80, 26, 18, body)
    if air:
        leg(c, 66, 92, 18, 1.2, body, 9)
    else:
        c.e(64, 98, 16, 5, body)
    leg(c, 36, 92, 10, -0.2 if not air else -0.8, body, 6)
    for dx in (0, 9):                                            # orecchie lunghe
        c.e(34 + dx, 34, 6, 22, body); c.e(34 + dx, 34, 3, 16, inner, 0)
        if tipb:
            c.e(34 + dx, 14, 4, 4, BLK, 0)
    c.e(30, 62, 15, 14, body)
    c.e(16, 66, 3, 3, col(220, 120, 130), 2)
    c.eye(28, 59, 6, -1, 1)


def ember(c, f):
    air = f == 1
    cy = 66 if not air else 58
    c.poly([(18, cy + 30), (14, cy), (30, cy - 30), (64, cy - 34), (84, cy - 8), (80, cy + 30)], col(48, 36, 34))
    for p in ([(24, cy + 10), (40, cy), (52, cy + 16)], [(56, cy - 20), (66, cy - 4), (76, cy + 6)], [(30, cy - 14), (38, cy - 24)]):
        c.line(p, col(255, 110, 20), 5); c.line(p, col(255, 220, 120), 2)
    c.eye(36, cy - 6, 7, -1, 1, iris=col(200, 40, 10)); c.eye(56, cy - 6, 7, -1, 0, iris=col(200, 40, 10))


def bunny(c, f): hare(c, f, col(156, 120, 90), col(220, 170, 160), False)


def puff(c, f):
    air = f == 1
    cy = 74 if not air else 62
    for dx, dy, r in ((0, 0, 24), (-16, 6, 16), (16, 6, 16), (-6, -14, 14), (10, -12, 14)):
        c.e(48 + dx, cy + dy, r, r * 0.9, col(250, 250, 255))
    c.eye(38, cy - 2, 6, -1, 1); c.eye(56, cy - 2, 6, -1, 0)


HOPPERS = [grasshopper, squirrel, hare, ember, bunny, puff]


# ------------------------------------------------------------------ tartaruga e lumaca

def tortoise(c, mode):
    if mode == 2:                                                # ribaltata, zampe all'aria
        c.e(64, 62, 40, 24, col(196, 160, 80))
        for x in (40, 56, 72, 88):
            leg(c, x, 46, 12, math.pi, col(130, 120, 80), 7)
        c.e(22, 58, 11, 10, col(140, 130, 90))
        c.line([(16, 54), (24, 62)], INK, 2); c.line([(24, 54), (16, 62)], INK, 2)
        return
    if mode == 0:
        for x in (40, 84):
            leg(c, x, 74, 14, 0.2, col(140, 130, 90), 9)
    c.e(62 if mode == 0 else 48, 60 if mode == 0 else 46, 40 if mode == 0 else 36, 28 if mode == 0 else 26, col(120, 92, 40))
    cx, cy = (62, 56) if mode == 0 else (48, 42)
    for dx, dy in ((0, -6), (-18, 2), (18, 2), (-10, -16), (10, -16)):
        c.e(cx + dx, cy + dy, 9, 7, col(210, 176, 90), 2)
    if mode == 0:
        c.e(16, 64, 13, 11, col(150, 140, 96))
        c.eye(14, 61, 5, -1, 1)


def snail(c, mode):
    if mode != 1:
        c.poly([(10, 82), (20, 60), (40, 70), (114, 82), (114, 88), (10, 88)], col(214, 196, 168))
        for dx in (0, 8):
            c.line([(24 + dx, 64), (16 + dx * 1.5, 40)], INK, 5); c.line([(24 + dx, 64), (16 + dx * 1.5, 40)], col(214, 196, 168), 3)
            c.e(16 + dx * 1.5, 40, 4, 4, BLK, 2)
        c.eye(28, 70, 5, -1, 1)
    if mode == 2:
        return
    cx, cy = (78, 56) if mode == 0 else (48, 42)
    c.e(cx, cy, 30, 28, col(196, 120, 60))
    for r, cc in ((22, col(220, 160, 90)), (14, col(180, 104, 50)), (6, col(230, 180, 110))):
        c.e(cx - (28 - r) * 0.3, cy + (28 - r) * 0.2, r, r, cc, 2)


def main():
    sizes = [(104, 84), (128, 88), (120, 96), (96, 106)]
    for t in range(6):
        base = "s2_%d_" % t
        for k in range(2):
            c = C(*sizes[0]); WALKERS[t](c, k); c.save(base + str(k))
        squash(base + "0", base + "2")
        for k in range(2):
            c = C(*sizes[1]); SPIKIES[t](c, k); c.save(base + str(3 + k))
        for k in range(2):
            c = C(*sizes[2]); FLYERS[t](c, k); c.save(base + str(5 + k))
        for k in range(2):
            c = C(*sizes[3]); HOPPERS[t](c, k); c.save(base + str(7 + k))
        squash(base + "7", base + "9")
    for m in range(3):
        c = C(128, 96) if m != 1 else C(96, 80); tortoise(c, m); c.save("s2_sn_%d" % m)
        c = C(128, 96) if m != 1 else C(96, 80); snail(c, m); c.save("s2_sn_%d" % (3 + m))


if __name__ == "__main__":
    main()
