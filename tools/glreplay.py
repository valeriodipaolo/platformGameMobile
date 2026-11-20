#!/usr/bin/env python3
"""Ridisegna un fotogramma registrato da tools/preview/GlDump con OpenGL ES 2 vero (Mesa, senza schermo), con gli stessi shader
e la stessa logica di GlRenderer sul telefono.

Uso:  EGL_PLATFORM=surfaceless python3 tools/glreplay.py dump.bin out.png [larghezza altezza]
"""
import ctypes
import os
import struct
import sys

import numpy as np
from PIL import Image

RES = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "res", "drawable-nodpi")
os.environ.setdefault("EGL_PLATFORM", "surfaceless")
egl = ctypes.CDLL("libEGL.so.1")
gl = ctypes.CDLL("libGLESv2.so.2")
vp_ = ctypes.c_void_p


def make_context(w, h):
    egl.eglGetDisplay.restype = vp_
    d = egl.eglGetDisplay(vp_(0))
    egl.eglInitialize(vp_(d), None, None)
    attrs = (ctypes.c_int * 15)(0x3033, 0x0001, 0x3040, 0x0004, 0x3024, 8, 0x3023, 8, 0x3022, 8, 0x3021, 8, 0x3025, 24, 0x3038)
    cfg, n = vp_(), ctypes.c_int()
    egl.eglChooseConfig(vp_(d), attrs, ctypes.byref(cfg), 1, ctypes.byref(n))
    egl.eglBindAPI(0x30A0)
    egl.eglCreateContext.restype = vp_
    egl.eglCreatePbufferSurface.restype = vp_
    ctx = egl.eglCreateContext(vp_(d), cfg, vp_(0), (ctypes.c_int * 3)(0x3098, 2, 0x3038))
    surf = egl.eglCreatePbufferSurface(vp_(d), cfg, (ctypes.c_int * 5)(0x3057, w, 0x3056, h, 0x3038))
    egl.eglMakeCurrent(vp_(d), vp_(surf), vp_(surf), vp_(ctx))


GL_TRIANGLES, GL_FLOAT, GL_ARRAY_BUFFER, GL_STATIC_DRAW = 4, 0x1406, 0x8892, 0x88E4
GL_TEXTURE_2D, GL_RGBA, GL_UNSIGNED_BYTE = 0x0DE1, 0x1908, 0x1401
GL_DEPTH_TEST, GL_BLEND, GL_LEQUAL, GL_ONE, GL_ONE_MINUS_SRC_ALPHA = 0x0B71, 0x0BE2, 0x0203, 1, 0x0303


class Reader:
    def __init__(self, data):
        self.d, self.p = data, 0

    def i(self):
        v = struct.unpack_from(">i", self.d, self.p)[0]; self.p += 4; return v

    def b(self):
        v = self.d[self.p]; self.p += 1; return v

    def f(self, n=1):
        v = struct.unpack_from(">%df" % n, self.d, self.p); self.p += 4 * n
        return v if n > 1 else v[0]

    def s(self):
        n = self.i(); v = self.d[self.p:self.p + n].decode("utf-8"); self.p += n; return v


def compile_shader(kind, src):
    sh = gl.glCreateShader(kind)
    b = ctypes.c_char_p(src.encode())
    gl.glShaderSource(sh, 1, ctypes.byref(b), None)
    gl.glCompileShader(sh)
    ok = ctypes.c_int()
    gl.glGetShaderiv(sh, 0x8B81, ctypes.byref(ok))
    if not ok.value:
        log = ctypes.create_string_buffer(4096)
        gl.glGetShaderInfoLog(sh, 4096, None, log)
        raise SystemExit("errore shader:\n" + log.value.decode())
    return sh


def pot(n):
    p = 1
    while p < n:
        p <<= 1
    return p


ATTR = {"aPos": 0, "aNormal": 1, "aUV": 2, "aColor": 3, "aSway": 4}


def program(vs, fs):
    prog = gl.glCreateProgram()
    gl.glAttachShader(prog, compile_shader(0x8B31, vs))
    gl.glAttachShader(prog, compile_shader(0x8B30, fs))
    for n, i in ATTR.items():
        gl.glBindAttribLocation(prog, i, n.encode())
    gl.glLinkProgram(prog)
    ok = ctypes.c_int()
    gl.glGetProgramiv(prog, 0x8B82, ctypes.byref(ok))
    if not ok.value:
        log = ctypes.create_string_buffer(4096)
        gl.glGetProgramInfoLog(prog, 4096, None, log)
        raise SystemExit("errore link:\n" + log.value.decode())
    return prog


def loc(p, n):
    return gl.glGetUniformLocation(p, n.encode())


def target(w, h, depth):
    """Texture di colore (e, se serve, profondità) su cui disegnare invece che sullo schermo."""
    t, fb = ctypes.c_uint(), ctypes.c_uint()
    gl.glGenTextures(1, ctypes.byref(t))
    gl.glBindTexture(GL_TEXTURE_2D, t)
    gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, None)
    filt = 0x2600 if depth == "shadow" else 0x2601
    for p, v in ((0x2801, filt), (0x2800, filt), (0x2802, 0x812F), (0x2803, 0x812F)):
        gl.glTexParameteri(GL_TEXTURE_2D, p, v)
    gl.glGenFramebuffers(1, ctypes.byref(fb))
    gl.glBindFramebuffer(0x8D40, fb)
    gl.glFramebufferTexture2D(0x8D40, 0x8CE0, GL_TEXTURE_2D, t, 0)
    if depth:
        rb = ctypes.c_uint()
        gl.glGenRenderbuffers(1, ctypes.byref(rb))
        gl.glBindRenderbuffer(0x8D41, rb)
        gl.glRenderbufferStorage(0x8D41, 0x81A5 if depth == "shadow" else 0x81A6, w, h)   # 16 bit per le ombre, 24 per la scena
        gl.glFramebufferRenderbuffer(0x8D40, 0x8D00, 0x8D41, rb)
    st = gl.glCheckFramebufferStatus(0x8D40)
    if st != 0x8CD5:
        raise SystemExit("framebuffer incompleto %x" % st)
    return fb.value, t.value


class Replay:
    def __init__(self, vs, fs):
        prog = program(vs, fs)
        self.prog = prog
        gl.glUseProgram(prog)
        self.u = {}
        for n in "uMVP uMV uTint uFog uTime uTex uFogColor uLightV uUpV uSpec uOpaque uCut uEmissive uAlpha uGrade uLightMVP uShadow uShadowOn".split():
            self.u[n] = gl.glGetUniformLocation(prog, n.encode())
        self.a = dict(ATTR)
        self.meshes, self.tex = {}, {}
        self.white = self.make_tex(np.full((1, 1, 4), 255, np.uint8), False)

    def make_tex(self, rgba, p2):
        t = ctypes.c_uint()
        gl.glGenTextures(1, ctypes.byref(t))
        gl.glBindTexture(GL_TEXTURE_2D, t)
        h, w = rgba.shape[:2]
        gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, np.ascontiguousarray(rgba).ctypes.data_as(vp_))
        gl.glTexParameteri(GL_TEXTURE_2D, 0x2800, 0x2601)
        if p2:
            gl.glGenerateMipmap(GL_TEXTURE_2D)
            gl.glTexParameteri(GL_TEXTURE_2D, 0x2801, 0x2703)
            gl.glTexParameteri(GL_TEXTURE_2D, 0x2802, 0x2901)
            gl.glTexParameteri(GL_TEXTURE_2D, 0x2803, 0x2901)
        else:
            gl.glTexParameteri(GL_TEXTURE_2D, 0x2801, 0x2601)
            gl.glTexParameteri(GL_TEXTURE_2D, 0x2802, 0x812F)
            gl.glTexParameteri(GL_TEXTURE_2D, 0x2803, 0x812F)
        return t.value

    def texture(self, name):
        if name in self.tex:
            return self.tex[name]
        im = Image.open(os.path.join(RES, name + ".png")).convert("RGBA")
        w, h = im.size
        p2 = (w & (w - 1)) == 0 and (h & (h - 1)) == 0
        if not p2 and w <= 512 and h <= 512:        # come sul telefono: le immagini piccole vanno a potenza di due
            im = im.resize((pot(w), pot(h)), Image.BILINEAR)
            p2 = True
        a = np.asarray(im).astype(np.float32)
        a[..., :3] *= a[..., 3:4] / 255.0            # Android carica le immagini con il colore moltiplicato per l'alfa
        self.tex[name] = self.make_tex(a.astype(np.uint8), p2)
        return self.tex[name]

    def add_mesh(self, mid, count, tex, blend, cut, fog, cast, spec, shine, data):
        buf = ctypes.c_uint()
        gl.glGenBuffers(1, ctypes.byref(buf))
        gl.glBindBuffer(GL_ARRAY_BUFFER, buf)
        arr = np.asarray(data, np.float32)
        gl.glBufferData(GL_ARRAY_BUFFER, arr.nbytes, arr.ctypes.data_as(vp_), GL_STATIC_DRAW)
        self.meshes[mid] = dict(buf=buf.value, count=count, tex=tex, blend=blend, cut=cut, fog=fog, cast=cast, spec=spec, shine=shine)

    def begin(self, view, proj, fog):
        self.view = np.array(view, np.float32).reshape(4, 4).T
        self.proj = np.array(proj, np.float32).reshape(4, 4).T
        self.vp = self.proj @ self.view
        self.fog = fog
        v = view
        L = self.L
        u = self.u
        gl.glUseProgram(self.prog)
        gl.glUniform3f(u["uLightV"], ctypes.c_float(v[0] * L[0] + v[4] * L[1] + v[8] * L[2]), ctypes.c_float(v[1] * L[0] + v[5] * L[1] + v[9] * L[2]),
                       ctypes.c_float(v[2] * L[0] + v[6] * L[1] + v[10] * L[2]))
        gl.glUniform3f(u["uUpV"], ctypes.c_float(v[4]), ctypes.c_float(v[5]), ctypes.c_float(v[6]))
        gl.glUniform3f(u["uFogColor"], *[ctypes.c_float(x) for x in fog[:3]])
        gl.glUniform3f(u["uGrade"], ctypes.c_float(1), ctypes.c_float(1), ctypes.c_float(1))
        gl.glUniform1i(u["uTex"], 0)
        for n in self.a.values():
            if n >= 0:
                gl.glEnableVertexAttribArray(n)
        gl.glDisable(GL_BLEND)
        gl.glDepthMask(1)

    def submit(self, m, mvp, mv, tint, alpha, em, fog):
        if m["count"] == 0:
            return
        gl.glBindBuffer(GL_ARRAY_BUFFER, m["buf"])
        st = 12 * 4
        for name, size, off in (("aPos", 3, 0), ("aNormal", 3, 12), ("aUV", 2, 24), ("aColor", 3, 32), ("aSway", 1, 44)):
            if self.a[name] >= 0:
                gl.glVertexAttribPointer(self.a[name], size, GL_FLOAT, 0, st, vp_(off))
        gl.glBindTexture(GL_TEXTURE_2D, self.white if not m["tex"] else self.texture(m["tex"]))
        u = self.u
        gl.glUniformMatrix4fv(u["uMVP"], 1, 0, np.ascontiguousarray(mvp.T, np.float32).ctypes.data_as(vp_))
        gl.glUniformMatrix4fv(u["uMV"], 1, 0, np.ascontiguousarray(mv.T, np.float32).ctypes.data_as(vp_))
        f = ctypes.c_float
        gl.glUniform4f(u["uTint"], f(tint[0]), f(tint[1]), f(tint[2]), f(1))
        gl.glUniform1f(u["uAlpha"], f(alpha))
        gl.glUniform1f(u["uEmissive"], f(em))
        gl.glUniform1f(u["uOpaque"], f(0 if m["blend"] else 1))
        gl.glUniform1f(u["uCut"], f(1 if m["cut"] else 0))
        gl.glUniform2f(u["uSpec"], f(m["spec"]), f(m["shine"]))
        if fog:
            gl.glUniform3f(u["uFog"], f(self.fog[3]), f(1.0 / (self.fog[4] - self.fog[3])), f(self.fog[5]))
        else:
            gl.glUniform3f(u["uFog"], f(0), f(0), f(0))
        gl.glDrawArrays(GL_TRIANGLES, 0, m["count"])

    def draw(self, mid, model, rgba, em):
        m = self.meshes[mid]
        M = np.array(model, np.float32).reshape(4, 4).T
        gl.glUniformMatrix4fv(self.u["uLightMVP"], 1, 0, np.ascontiguousarray((self.light @ M).T, np.float32).ctypes.data_as(vp_))
        if m["blend"]:
            gl.glEnable(GL_BLEND)
            gl.glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA)
            gl.glDepthMask(0)
        self.submit(m, self.vp @ M, self.view @ M, rgba, rgba[3], em, m["fog"])
        if m["blend"]:
            gl.glDisable(GL_BLEND)
            gl.glDepthMask(1)

    def backdrop(self, mid):
        gl.glDisable(GL_DEPTH_TEST)
        gl.glDepthMask(0)
        I = np.eye(4, dtype=np.float32)
        self.submit(self.meshes[mid], I, self.view, (1, 1, 1), 1, 1, False)
        gl.glDepthMask(1)
        gl.glEnable(GL_DEPTH_TEST)


POST_ATTRS = None


def in_area(light, model, sph, R):
    """Come GlShaders.inShadowArea."""
    cx, cy, cz, rad = sph
    if rad < 0:
        return True
    M = np.array(model, np.float32).reshape(4, 4).T
    p = M @ np.array([cx, cy, cz, 1], np.float32)
    sc = max(np.abs(M[:3, 0]).sum(), np.abs(M[:3, 1]).sum(), np.abs(M[:3, 2]).sum())
    r = rad * sc / R
    q = light @ p
    return -1 - r < q[0] < 1 + r and -1 - r < q[1] < 1 + r


class Pipeline:
    """Come GlRenderer.end(): ombre dal sole, scena su texture, bagliore, correzione colore."""

    def __init__(self, rp, shaders, size, W, H, fx, radius):
        self.rp, self.S, self.W, self.H, self.fx = rp, size, W, H, fx
        self.R = radius
        svs, sfs, pvs, bright, blur, comp = shaders
        self.sprog = program(svs, sfs)
        self.bprog, self.lprog, self.cprog = program(pvs, bright), program(pvs, blur), program(pvs, comp)
        if fx:
            self.sfb, self.stex = target(size, size, "shadow")
            self.mfb, self.mtex = target(W, H, True)
            self.bw, self.bh = max(1, W // 4), max(1, H // 4)
            self.b1, self.b1t = target(self.bw, self.bh, False)
            self.b2, self.b2t = target(self.bw, self.bh, False)
        q = np.array([-1, -1, 1, -1, -1, 1, -1, 1, 1, -1, 1, 1], np.float32)
        b = ctypes.c_uint()
        gl.glGenBuffers(1, ctypes.byref(b))
        gl.glBindBuffer(GL_ARRAY_BUFFER, b)
        gl.glBufferData(GL_ARRAY_BUFFER, q.nbytes, q.ctypes.data_as(vp_), GL_STATIC_DRAW)
        self.quad = b.value

    def attribs(self, idx):
        for i in range(5):
            (gl.glEnableVertexAttribArray if i in idx else gl.glDisableVertexAttribArray)(i)

    def run(self, ops, begin, t, grade, light):
        rp, f = self.rp, ctypes.c_float
        rp.light = np.array(light, np.float32).reshape(4, 4).T
        if self.fx:
            gl.glBindFramebuffer(0x8D40, self.sfb)
            gl.glViewport(0, 0, self.S, self.S)
            gl.glClearColor(f(1), f(1), f(1), f(1))
            gl.glClear(0x4000 | 0x0100)
            gl.glUseProgram(self.sprog)
            self.attribs((0, 2, 4))
            gl.glUniform1f(loc(self.sprog, "uTime"), f(t))
            gl.glUniform1i(loc(self.sprog, "uTex"), 0)
            gl.glActiveTexture(0x84C0)
            for op in ops:
                if op[0] != "d":
                    continue
                m = rp.meshes[op[1]]
                if m["blend"] or not m["cast"] or op[4] >= 0.5 or m["count"] == 0 or not in_area(rp.light, op[2], m["sphere"], self.R):
                    continue
                M = np.array(op[2], np.float32).reshape(4, 4).T
                gl.glBindBuffer(GL_ARRAY_BUFFER, m["buf"])
                gl.glVertexAttribPointer(0, 3, GL_FLOAT, 0, 48, vp_(0))
                gl.glVertexAttribPointer(2, 2, GL_FLOAT, 0, 48, vp_(24))
                gl.glVertexAttribPointer(4, 1, GL_FLOAT, 0, 48, vp_(44))
                gl.glBindTexture(GL_TEXTURE_2D, rp.white if not m["tex"] else rp.texture(m["tex"]))
                gl.glUniform1f(loc(self.sprog, "uCut"), f(1 if m["cut"] else 0))
                gl.glUniformMatrix4fv(loc(self.sprog, "uMVP"), 1, 0, np.ascontiguousarray((rp.light @ M).T, np.float32).ctypes.data_as(vp_))
                gl.glDrawArrays(GL_TRIANGLES, 0, m["count"])
            gl.glBindFramebuffer(0x8D40, self.mfb)
        else:
            gl.glBindFramebuffer(0x8D40, 0)
        gl.glViewport(0, 0, self.W, self.H)
        gl.glClearColor(f(0.745), f(0.847), f(0.933), f(1))
        gl.glClear(0x4000 | 0x0100)
        self.attribs((0, 1, 2, 3, 4))
        rp.begin(*begin)
        gl.glUniform1f(rp.u["uTime"], f(t))
        gl.glUniform3f(rp.u["uGrade"], *[f(x) for x in grade])
        gl.glUniform1i(rp.u["uShadow"], 1)
        gl.glUniform1f(rp.u["uShadowOn"], f(1 if self.fx else 0))
        gl.glActiveTexture(0x84C1)
        gl.glBindTexture(GL_TEXTURE_2D, self.stex if self.fx else rp.white)
        gl.glActiveTexture(0x84C0)
        for op in ops:
            if op[0] == "b":
                rp.backdrop(op[1])
            else:
                rp.draw(op[1], op[2], op[3], op[4])
        if not self.fx:
            return
        gl.glDisable(GL_DEPTH_TEST)
        gl.glDisable(GL_BLEND)
        gl.glBindBuffer(GL_ARRAY_BUFFER, self.quad)
        self.attribs((0,))
        gl.glVertexAttribPointer(0, 2, GL_FLOAT, 0, 8, vp_(0))

        def pas(prog, fb, w, h, tex, texel, tex2=None):
            gl.glBindFramebuffer(0x8D40, fb)
            gl.glViewport(0, 0, w, h)
            gl.glUseProgram(prog)
            gl.glUniform1i(loc(prog, "uTex"), 0)
            gl.glUniform2f(loc(prog, "uTexel"), f(texel[0]), f(texel[1]))
            if tex2:
                gl.glUniform1i(loc(prog, "uBloom"), 1)
                gl.glActiveTexture(0x84C1)
                gl.glBindTexture(GL_TEXTURE_2D, tex2)
                gl.glActiveTexture(0x84C0)
            gl.glBindTexture(GL_TEXTURE_2D, tex)
            gl.glDrawArrays(GL_TRIANGLES, 0, 6)

        pas(self.bprog, self.b1, self.bw, self.bh, self.mtex, (1.0 / self.W, 1.0 / self.H))
        pas(self.lprog, self.b2, self.bw, self.bh, self.b1t, (1.0 / self.bw, 0))
        pas(self.lprog, self.b1, self.bw, self.bh, self.b2t, (0, 1.0 / self.bh))
        pas(self.cprog, 0, self.W, self.H, self.mtex, (1.0 / self.W, 1.0 / self.H), self.b1t)
        gl.glEnable(GL_DEPTH_TEST)


def main():
    data = open(sys.argv[1], "rb").read()
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    fx = "--nofx" not in sys.argv
    W, H = (int(args[2]), int(args[3])) if len(args) > 3 else (1280, 576)
    make_context(W, H)
    r = Reader(data)
    assert r.i() == 0x474C4431
    vs, fs = r.s(), r.s()
    shaders = [r.s() for _ in range(6)]
    size = r.i()
    radius = r.f()
    rp = Replay(vs, fs)
    rp.L = r.f(3)
    gl.glEnable(GL_DEPTH_TEST)
    gl.glDepthFunc(GL_LEQUAL)
    gl.glDisable(0x0B44)
    pipe = Pipeline(rp, shaders, size, W, H, fx, radius)
    live, ops, t, grade, begin, light = False, [], 0.0, (1, 1, 1), None, None
    while True:
        op = r.b()
        if op == 0:
            break
        if op == 1:
            mid, count, tex = r.i(), r.i(), r.s()
            blend, cut, fog, cast = r.b(), r.b(), r.b(), r.b()
            sphere = r.f(4)
            spec, shine = r.f(), r.f()
            n = r.i()
            vals = struct.unpack_from(">%df" % n, r.d, r.p); r.p += 4 * n
            rp.add_mesh(mid, count if n else 0, tex, blend, cut, fog, cast, spec, shine, vals)
            rp.meshes[mid]["sphere"] = sphere
        elif op == 2:
            view, proj, fog, lm = r.f(16), r.f(16), r.f(6), r.f(16)
            if live:
                begin, light = (view, proj, fog), lm
        elif op == 3:
            mid = r.i()
            if live:
                ops.append(("b", mid))
        elif op == 4:
            mid, model, rgba, em = r.i(), r.f(16), r.f(4), r.f()
            if live:
                ops.append(("d", mid, model, rgba, em))
        elif op == 5:
            t = r.f()
        elif op == 6:
            grade = r.f(3)
        elif op == 7:
            if live:
                pipe.run(ops, begin, t, grade, light)
                print("oggetti disegnati:", len(ops), "che fanno ombra:", sum(1 for o in ops if o[0] == "d" and rp.meshes[o[1]]["cast"] and not rp.meshes[o[1]]["blend"] and o[4] < 0.5 and in_area(rp.light, o[2], rp.meshes[o[1]]["sphere"], pipe.R)))
                break
        elif op == 9:
            live = True
    buf = (ctypes.c_ubyte * (W * H * 4))()
    gl.glFinish()
    gl.glReadPixels(0, 0, W, H, GL_RGBA, GL_UNSIGNED_BYTE, buf)
    img = Image.frombuffer("RGBA", (W, H), bytes(buf), "raw", "RGBA", 0, -1).convert("RGB")
    img.save(args[1])
    print("ok", args[1])


if __name__ == "__main__":
    main()
