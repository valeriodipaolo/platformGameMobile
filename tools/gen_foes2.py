#!/usr/bin/env python3
"""Sprite dei nemici 2D della seconda serie di livelli (13-23): forme tonde, contorno scuro, occhi espressivi. Tutti guardano a sinistra.

Uso:  python3 tools/gen_foes2.py
"""
import math
import os

from PIL import Image, ImageDraw

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "res", "drawable-nodpi")
SS = 4
INK = (38, 30, 44, 255)


class C:
    def __init__(self, w, h):
        self.w, self.h = w, h
        self.im = Image.new("RGBA", (w * SS, h * SS), (0, 0, 0, 0))
        self.d = ImageDraw.Draw(self.im)

    def e(self, cx, cy, rx, ry, fill, ol=3):
        if ol:
            self.d.ellipse([(cx - rx - ol) * SS, (cy - ry - ol) * SS, (cx + rx + ol) * SS, (cy + ry + ol) * SS], fill=INK)
        self.d.ellipse([(cx - rx) * SS, (cy - ry) * SS, (cx + rx) * SS, (cy + ry) * SS], fill=fill)

    def rr(self, x0, y0, x1, y1, r, fill, ol=3):
        if ol:
            self.d.rounded_rectangle([(x0 - ol) * SS, (y0 - ol) * SS, (x1 + ol) * SS, (y1 + ol) * SS], radius=(r + ol) * SS, fill=INK)
        self.d.rounded_rectangle([x0 * SS, y0 * SS, x1 * SS, y1 * SS], radius=r * SS, fill=fill)

    def poly(self, pts, fill, ol=3):
        p = [(x * SS, y * SS) for x, y in pts]
        if ol:
            self.d.line(p + [p[0]], fill=INK, width=ol * 2 * SS, joint="curve")
        self.d.polygon(p, fill=fill)

    def line(self, pts, fill, w):
        self.d.line([(x * SS, y * SS) for x, y in pts], fill=fill, width=int(w * SS), joint="curve")

    def eye(self, x, y, r, look=-1, angry=0, iris=(30, 24, 40, 255)):
        self.e(x, y, r, r, (255, 255, 255, 255), 2)
        self.e(x + look * r * 0.3, y + r * 0.1, r * 0.48, r * 0.5, iris, 0)
        self.e(x + look * r * 0.45, y - r * 0.15, r * 0.16, r * 0.16, (255, 255, 255, 255), 0)
        if angry:
            self.line([(x - angry * r * 1.2, y - r * 1.5), (x + angry * r * 1.0, y - r * 0.7)], INK, 4)

    def save(self, name):
        self.im.resize((self.w, self.h), Image.LANCZOS).save(os.path.join(OUT, name + ".png"), optimize=True)


def mole():
    c = C(112, 112)
    c.e(56, 70, 34, 40, (122, 86, 62, 255))
    c.e(56, 82, 24, 26, (176, 138, 104, 255), 0)
    for sx in (-1, 1):
        c.e(56 + sx * 36, 84, 12, 9, (240, 190, 170, 255))
    c.e(40, 52, 10, 10, (20, 20, 30, 255))                       # occhialoni scuri
    c.e(66, 52, 10, 10, (20, 20, 30, 255))
    c.e(37, 49, 3, 3, (255, 255, 255, 255), 0); c.e(63, 49, 3, 3, (255, 255, 255, 255), 0)
    c.line([(50, 52), (56, 52)], INK, 4)
    c.e(50, 68, 9, 7, (244, 150, 160, 255))
    c.rr(44, 78, 50, 88, 2, (255, 255, 255, 255), 2); c.rr(52, 78, 58, 88, 2, (255, 255, 255, 255), 2)
    c.e(56, 114, 54, 18, (120, 84, 50, 255))
    c.save("fx_mole")
    c = C(112, 48)
    c.e(56, 50, 52, 30, (120, 84, 50, 255))
    c.e(40, 30, 10, 6, (150, 110, 70, 255), 0); c.e(70, 36, 12, 6, (96, 66, 40, 255), 0)
    c.save("fx_mound")


def snail():
    c = C(128, 96)
    c.rr(10, 62, 104, 88, 13, (238, 206, 120, 255))
    c.e(26, 52, 15, 17, (238, 206, 120, 255))
    for x in (16, 32):
        c.line([(x, 42), (x - 3, 22)], INK, 7); c.line([(x, 42), (x - 3, 22)], (238, 206, 120, 255), 3)
        c.eye(x - 3, 18, 7, -1)
    c.line([(16, 60), (26, 64)], INK, 3)
    c.e(76, 46, 36, 36, (204, 92, 78, 255))
    c.e(76, 46, 25, 25, (236, 134, 104, 255), 2); c.e(76, 46, 13, 13, (250, 190, 140, 255), 2)
    c.save("fx_snail")
    c = C(96, 80)
    c.e(48, 44, 36, 34, (204, 92, 78, 255))
    c.e(48, 44, 25, 24, (236, 134, 104, 255), 2); c.e(48, 44, 13, 12, (250, 190, 140, 255), 2)
    c.save("fx_shell")


def chestnut():
    c = C(112, 112)
    for i in range(18):
        a = i * math.tau / 18
        c.poly([(56 + math.cos(a - 0.14) * 34, 58 + math.sin(a - 0.14) * 34), (56 + math.cos(a) * 52, 58 + math.sin(a) * 52),
                (56 + math.cos(a + 0.14) * 34, 58 + math.sin(a + 0.14) * 34)], (150, 176, 66, 255), 2)
    c.e(56, 58, 37, 37, (128, 158, 56, 255))
    c.e(48, 50, 22, 18, (168, 196, 84, 255), 0)
    c.eye(44, 58, 9, -1, 1); c.eye(68, 58, 9, -1, -1)
    c.line([(46, 78), (56, 74), (66, 78)], INK, 3)
    c.save("fx_chestnut")


def owl():
    for k, up in enumerate((True, False)):
        c = C(128, 112)
        for sx in (-1, 1):
            if up:
                c.poly([(64 + sx * 22, 54), (64 + sx * 60, 20), (64 + sx * 52, 62), (64 + sx * 26, 76)], (120, 86, 60, 255))
            else:
                c.poly([(64 + sx * 22, 54), (64 + sx * 58, 78), (64 + sx * 44, 96), (64 + sx * 24, 80)], (120, 86, 60, 255))
        c.e(64, 64, 30, 36, (150, 110, 76, 255))
        c.e(64, 76, 19, 20, (228, 200, 156, 255), 0)
        c.poly([(40, 36), (44, 18), (56, 32)], (150, 110, 76, 255)); c.poly([(88, 36), (84, 18), (72, 32)], (150, 110, 76, 255))
        c.eye(51, 52, 12, -1, 1, (240, 150, 20, 255)); c.eye(77, 52, 12, -1, -1, (240, 150, 20, 255))
        c.e(49, 53, 4, 4, INK, 0); c.e(75, 53, 4, 4, INK, 0)
        c.poly([(58, 62), (70, 62), (64, 74)], (250, 190, 60, 255), 2)
        c.save("fx_owl_%d" % k)


def penguin():
    c = C(120, 96)                                              # scivola sulla pancia, testa a sinistra
    c.e(66, 56, 46, 30, (40, 48, 72, 255))
    c.e(62, 66, 38, 18, (248, 250, 255, 255), 0)
    c.e(28, 44, 22, 22, (40, 48, 72, 255))
    c.e(26, 50, 14, 12, (248, 250, 255, 255), 0)
    c.eye(22, 40, 7, -1, 1)
    c.poly([(8, 48), (-4, 54), (10, 58)], (250, 170, 50, 255), 2)
    c.poly([(70, 34), (100, 16), (92, 42)], (40, 48, 72, 255))
    for y in (70, 82):
        c.e(110, y - 8, 9, 6, (250, 170, 50, 255))
    c.save("fx_penguin")


def snowman():
    c = C(112, 160)
    c.e(56, 116, 38, 38, (244, 248, 255, 255))
    c.e(56, 62, 28, 28, (244, 248, 255, 255))
    c.e(48, 106, 20, 18, (255, 255, 255, 255), 0)
    c.rr(30, 14, 82, 38, 6, (60, 60, 90, 255)); c.rr(20, 34, 92, 42, 4, (60, 60, 90, 255))
    c.rr(30, 28, 82, 34, 0, (220, 70, 70, 255), 0)
    c.eye(46, 58, 7, -1, 1); c.eye(68, 58, 7, -1, -1)
    c.poly([(54, 64), (22, 70), (54, 74)], (250, 150, 40, 255), 2)
    c.rr(28, 84, 84, 94, 5, (220, 70, 70, 255))
    for y in (104, 120, 136):
        c.e(58, y, 4, 4, INK, 0)
    c.line([(22, 104), (2, 78)], (110, 76, 50, 255), 5)
    c.e(4, 74, 11, 11, (255, 255, 255, 255))
    c.save("fx_snowman")
    c = C(40, 40)
    c.e(20, 20, 15, 15, (250, 252, 255, 255)); c.e(16, 15, 6, 5, (255, 255, 255, 255), 0); c.e(24, 26, 6, 4, (206, 222, 244, 255), 0)
    c.save("fx_snowball")


def lapillo():
    c = C(96, 128)
    c.poly([(48, 126), (22, 84), (34, 92), (48, 60), (62, 92), (74, 84)], (255, 120, 30, 220), 0)
    c.e(48, 44, 34, 34, (255, 96, 24, 255))
    c.e(48, 46, 25, 25, (255, 176, 40, 255), 0); c.e(48, 48, 14, 14, (255, 236, 130, 255), 0)
    c.eye(36, 40, 8, -1, 1); c.eye(60, 40, 8, -1, -1)
    c.line([(38, 60), (48, 55), (58, 60)], INK, 3)
    c.save("fx_lapillo")


def salamander():
    for k, name in enumerate(("fx_sala", "fx_sala_b")):
        c = C(152, 88)
        c.line([(110, 56), (134, 50), (148, 30)], INK, 16); c.line([(110, 56), (134, 50), (148, 30)], (232, 72, 40, 255), 10)
        for x in (52, 100):
            c.rr(x - 6, 62, x + 8, 84, 6, (200, 54, 34, 255))
        c.rr(34, 34, 122, 70, 18, (232, 72, 40, 255))
        for x in (58, 80, 102):
            c.e(x, 44, 7, 6, (255, 196, 40, 255), 0)
        c.e(30, 46, 26, 22 + k * 5, (232, 72, 40, 255))
        c.eye(26, 36, 8, -1, 1, (255, 200, 30, 255)); c.e(24, 37, 3, 3, INK, 0)
        if k:
            c.e(10, 56, 11, 10, (90, 20, 20, 255), 2); c.e(8, 56, 6, 5, (255, 190, 40, 255), 0)
        else:
            c.line([(8, 56), (26, 60)], INK, 3)
        c.save(name)
    c = C(176, 64)
    for i, (x, r) in enumerate(((150, 12), (124, 18), (94, 24), (60, 28), (30, 22))):
        c.e(x, 32, r, r * 0.9, (255, 110 + i * 8, 24, 230), 0)
    for i, (x, r) in enumerate(((150, 6), (124, 10), (94, 15), (60, 18), (32, 12))):
        c.e(x, 32, r, r * 0.9, (255, 226, 110, 240), 0)
    c.save("fx_breath")


def frog():
    c = C(112, 80)
    c.e(84, 62, 22, 14, (70, 150, 60, 255))
    c.e(56, 50, 40, 26, (104, 190, 78, 255))
    c.e(52, 62, 28, 13, (220, 240, 170, 255), 0)
    c.e(30, 24, 13, 13, (104, 190, 78, 255)); c.e(62, 22, 13, 13, (104, 190, 78, 255))
    c.eye(28, 23, 9, -1, 1); c.eye(60, 21, 9, -1, -1)
    c.line([(20, 48), (40, 54), (62, 50)], INK, 3)
    c.e(30, 72, 13, 6, (70, 150, 60, 255))
    c.save("fx_frog")
    c = C(112, 112)
    c.line([(74, 60), (98, 86), (86, 106)], INK, 15); c.line([(74, 60), (98, 86), (86, 106)], (70, 150, 60, 255), 9)
    c.line([(36, 58), (22, 84)], INK, 13); c.line([(36, 58), (22, 84)], (70, 150, 60, 255), 7)
    c.e(54, 46, 36, 24, (104, 190, 78, 255))
    c.e(50, 56, 24, 12, (220, 240, 170, 255), 0)
    c.e(30, 22, 13, 13, (104, 190, 78, 255)); c.e(60, 20, 13, 13, (104, 190, 78, 255))
    c.eye(28, 21, 9, -1, 1); c.eye(58, 19, 9, -1, -1)
    c.e(30, 46, 9, 7, (150, 40, 50, 255), 2)
    c.save("fx_frog_j")


def plant():
    def head(c, cy, open_):
        c.e(52, cy, 34, 28, (214, 60, 90, 255))
        for x, y in ((34, cy - 12), (62, cy - 16), (72, cy + 2), (40, cy + 8)):
            c.e(x, y, 5, 5, (255, 236, 240, 255), 0)
        if open_:
            c.poly([(20, cy - 6), (52, cy + 2), (84, cy - 6), (70, cy - 34), (34, cy - 34)], (90, 16, 40, 255), 2)
            for x in (30, 44, 58, 72):
                c.poly([(x - 5, cy - 4), (x + 5, cy - 4), (x, cy - 16)], (255, 255, 255, 255), 0)
                c.poly([(x - 5, cy - 34), (x + 5, cy - 34), (x, cy - 22)], (255, 255, 255, 255), 0)
        else:
            c.line([(22, cy + 2), (36, cy + 8), (52, cy + 2), (68, cy + 8), (82, cy + 2)], INK, 4)
            c.eye(38, cy - 8, 7, -1, 1); c.eye(64, cy - 8, 7, -1, -1)
    c = C(104, 104)
    c.line([(52, 104), (52, 70)], INK, 14); c.line([(52, 104), (52, 70)], (70, 150, 60, 255), 8)
    c.e(26, 92, 20, 9, (86, 170, 66, 255)); c.e(78, 92, 20, 9, (86, 170, 66, 255))
    head(c, 50, False)
    c.save("fx_plant")
    c = C(104, 200)
    c.line([(52, 200), (46, 150), (58, 100), (52, 70)], INK, 14); c.line([(52, 200), (46, 150), (58, 100), (52, 70)], (70, 150, 60, 255), 8)
    c.e(26, 188, 20, 9, (86, 170, 66, 255)); c.e(78, 188, 20, 9, (86, 170, 66, 255))
    head(c, 56, True)
    c.save("fx_plant_o")


def star():
    c = C(144, 144)
    for i in range(9):                                           # scia in alto a destra: la stella cade verso sinistra
        t = i / 8
        c.e(60 + t * 70, 84 - t * 70, 22 - t * 18, 22 - t * 18, (255, 240, 150, int(200 * (1 - t))), 0)
    pts = []
    for i in range(10):
        a = -math.pi / 2 + i * math.pi / 5
        r = 40 if i % 2 == 0 else 19
        pts.append((52 + math.cos(a) * r, 92 + math.sin(a) * r))
    c.poly(pts, (255, 214, 60, 255), 4)
    c.e(52, 94, 16, 14, (255, 236, 130, 255), 0)
    c.eye(44, 90, 6, -1, 1); c.eye(60, 90, 6, -1, -1)
    c.line([(46, 104), (52, 101), (58, 104)], INK, 3)
    c.save("fx_star")


def sunny():
    c = C(128, 128)
    for i in range(12):
        a = i * math.tau / 12
        c.poly([(64 + math.cos(a - 0.2) * 38, 64 + math.sin(a - 0.2) * 38), (64 + math.cos(a) * 60, 64 + math.sin(a) * 60),
                (64 + math.cos(a + 0.2) * 38, 64 + math.sin(a + 0.2) * 38)], (255, 150, 30, 255), 2)
    c.e(64, 64, 40, 40, (255, 206, 50, 255))
    c.e(56, 54, 24, 20, (255, 232, 120, 255), 0)
    c.eye(50, 60, 9, -1, 1); c.eye(78, 60, 9, -1, -1)
    c.line([(52, 84), (64, 78), (76, 84)], INK, 4)
    c.save("fx_sunny")


if __name__ == "__main__":
    for f in (snail, chestnut, owl, penguin, snowman, lapillo, salamander, frog, plant, star, sunny):
        f()
    print("ok")
