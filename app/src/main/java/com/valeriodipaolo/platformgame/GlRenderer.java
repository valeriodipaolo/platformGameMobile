package com.valeriodipaolo.platformgame;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;

import com.valeriodipaolo.platformgame.core.Img;
import com.valeriodipaolo.platformgame.core3d.Gfx3D;
import com.valeriodipaolo.platformgame.core3d.GlShaders;
import com.valeriodipaolo.platformgame.core3d.Mat4;
import com.valeriodipaolo.platformgame.core3d.Mesh;
import com.valeriodipaolo.platformgame.core3d.Scene3D;
import com.valeriodipaolo.platformgame.core3d.World3D;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/** Disegna la scena 3D con OpenGL ES 2.0 e fa avanzare la partita a ogni fotogramma. */
final class GlRenderer implements GLSurfaceView.Renderer, Gfx3D {

    private final Bitmap[] bitmaps;
    private final World3D world;
    private final Level3DView host;
    private Scene3D scene = new Scene3D();

    private static final int A_POS = 0, A_NORMAL = 1, A_UV = 2, A_COLOR = 3, A_SWAY = 4;
    private static final String[] ATTRS = {"aPos", "aNormal", "aUV", "aColor", "aSway"};
    private int program, generation, whiteTex, uTime, uGrade, uLightMVP, uShadow, uShadowOn;
    private int shadowProg, brightProg, blurProg, compProg, sMVP, sTime, sTex, sCut, quad;
    private int shadowFbo, shadowTex, sceneFbo, sceneTex, sceneDepth, bloomW, bloomH, screenW = 1, screenH = 1, sizeW, sizeH;
    private final int[] bloomFbo = new int[2], bloomTex = new int[2];
    private boolean highp, depth24;
    private final float[] light = new float[16], lmvp = new float[16];
    private float fogR, fogG, fogB, gradeR = 1, gradeG = 1, gradeB = 1, time;
    private int uMVP, uMV, uTint, uAlpha, uEmissive, uFog, uFogColor, uOpaque, uCut, uTex, uLightV, uUpV, uSpec;
    private final int[] textures = new int[Img.COUNT];
    private final float[] view = new float[16], vp = new float[16], mvp = new float[16], mv = new float[16], ident = Mat4.identity();
    private float fogStart, fogEnd, fogMax, aspect = 2f;
    private long last;
    /** Messaggio da mostrare a schermo se la grafica 3D non riesce a partire. */
    volatile String error;

    GlRenderer(Bitmap[] bitmaps, World3D world, Level3DView host) {
        this.bitmaps = bitmaps;
        this.world = world;
        this.host = host;
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        generation++;                                  // buffer e texture di prima non valgono più
        if (generation > 1) scene = new Scene3D();     // il terreno caricato ha buttato i vertici: si ricostruisce la scena
        java.util.Arrays.fill(textures, 0);
        program = link(GlShaders.VERTEX, GlShaders.FRAGMENT);
        if (program != 0) locate(program);
        shadowProg = link(GlShaders.SHADOW_VERTEX, GlShaders.SHADOW_FRAGMENT);
        brightProg = link(GlShaders.POST_VERTEX, GlShaders.BRIGHT_FRAGMENT);
        blurProg = link(GlShaders.POST_VERTEX, GlShaders.BLUR_FRAGMENT);
        compProg = link(GlShaders.POST_VERTEX, GlShaders.COMPOSITE_FRAGMENT);
        error = program == 0 ? error : null;           // gli effetti sono facoltativi: senza, si gioca lo stesso
        whiteTex = makeWhite();
        // le ombre hanno bisogno di numeri precisi nei pixel: sui telefoni che non li hanno si fa a meno delle ombre
        int[] range = new int[2], prec = new int[1];
        GLES20.glGetShaderPrecisionFormat(GLES20.GL_FRAGMENT_SHADER, GLES20.GL_HIGH_FLOAT, range, 0, prec, 0);
        highp = prec[0] >= 16;
        depth24 = String.valueOf(GLES20.glGetString(GLES20.GL_EXTENSIONS)).contains("GL_OES_depth24");
        quad = makeQuad();
        sizeW = sizeH = 0;
        shadowFbo = sceneFbo = 0;                      // quelle di prima sono sparite con la vecchia superficie
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        last = 0;
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        screenW = width;
        screenH = height;
        aspect = width / (float) Math.max(1, height);
        makeTargets(width, height);
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        long now = System.nanoTime();
        float dt = last == 0 ? 0 : (now - last) / 1e9f;
        last = now;
        host.step(world, dt);
        if (program == 0) {
            GLES20.glClearColor(0.745f, 0.847f, 0.933f, 1f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            return;
        }
        nOps = 0;
        scene.render(world, this, aspect);
    }

    // ------------------------------------------------------------------ Gfx3D
    // La scena viene prima annotata (chi disegna cosa e dove), poi a fine fotogramma disegnata due volte: dal sole per le ombre,
    // e dalla camera su una texture che passa per bagliore e correzione colore prima di arrivare allo schermo.

    /** Un oggetto da disegnare in questo fotogramma (o lo sfondo, se mesh di sfondo). */
    private static final class Op {
        Mesh m;
        boolean back;
        final float[] model = new float[16];
        float r, g, b, a, em;
    }

    private Op[] ops = new Op[512];
    private int nOps;

    private Op op() {
        if (nOps == ops.length) ops = java.util.Arrays.copyOf(ops, nOps * 2);
        Op o = ops[nOps];
        if (o == null) o = ops[nOps] = new Op();
        nOps++;
        return o;
    }

    @Override
    public void begin(float[] view, float[] proj, float fogR, float fogG, float fogB, float fogStart, float fogEnd, float fogMax) {
        System.arraycopy(view, 0, this.view, 0, 16);
        Mat4.mul(vp, proj, view);
        GlShaders.lightMatrix(view, light);
        this.fogR = fogR;
        this.fogG = fogG;
        this.fogB = fogB;
        this.fogStart = fogStart;
        this.fogEnd = fogEnd;
        this.fogMax = fogMax;
        gradeR = gradeG = gradeB = 1f;
    }

    @Override
    public void backdrop(Mesh m) {
        Op o = op();
        o.m = m;
        o.back = true;
    }

    @Override
    public void draw(Mesh m, float[] model, float r, float g, float b, float a, float emissive) {
        if (m == null || m.count == 0) return;
        Op o = op();
        o.m = m;
        o.back = false;
        System.arraycopy(model, 0, o.model, 0, 16);
        o.r = r;
        o.g = g;
        o.b = b;
        o.a = a;
        o.em = emissive;
    }

    @Override
    public void grade(float r, float g, float b) {
        gradeR = r;
        gradeG = g;
        gradeB = b;
    }

    @Override
    public void time(float t) {
        time = t;
    }

    @Override
    public void end() {
        boolean post = sceneFbo != 0 && brightProg != 0 && blurProg != 0 && compProg != 0;
        boolean shadows = post && highp && shadowFbo != 0 && shadowProg != 0;
        if (shadows) shadowPass();
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, post ? sceneFbo : 0);
        GLES20.glViewport(0, 0, screenW, screenH);
        GLES20.glClearColor(0.745f, 0.847f, 0.933f, 1f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        mainPass(shadows);
        if (post) postPass();
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        for (int i = 0; i < nOps; i++) ops[i].m = null;
    }

    private void attribs(int mask) {
        for (int i = 0; i < 5; i++) {
            if ((mask & (1 << i)) != 0) GLES20.glEnableVertexAttribArray(i);
            else GLES20.glDisableVertexAttribArray(i);
        }
    }

    /** Profondità vista dal sole, solo per gli oggetti che gettano ombra. */
    private void shadowPass() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, shadowFbo);
        GLES20.glViewport(0, 0, GlShaders.SHADOW_SIZE, GlShaders.SHADOW_SIZE);
        GLES20.glClearColor(1f, 1f, 1f, 1f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(shadowProg);
        attribs(1 | 4 | 16);
        GLES20.glUniform1f(sTime, time);
        GLES20.glUniform1i(sTex, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        int stride = Mesh.STRIDE * 4;
        for (int i = 0; i < nOps; i++) {
            Op o = ops[i];
            Mesh m = o.m;
            if (o.back || m.blend || !m.castShadow || o.em >= 0.5f || !GlShaders.inShadowArea(light, o.model, m)) continue;
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffer(m));
            GLES20.glVertexAttribPointer(A_POS, 3, GLES20.GL_FLOAT, false, stride, 0);
            GLES20.glVertexAttribPointer(A_UV, 2, GLES20.GL_FLOAT, false, stride, 24);
            GLES20.glVertexAttribPointer(A_SWAY, 1, GLES20.GL_FLOAT, false, stride, 44);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, m.tex < 0 ? whiteTex : texture(m.tex));
            GLES20.glUniform1f(sCut, m.cutout ? 1f : 0f);
            Mat4.mul(mvp, light, o.model);
            GLES20.glUniformMatrix4fv(sMVP, 1, false, mvp, 0);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, m.count);
        }
    }

    private void mainPass(boolean shadows) {
        GLES20.glUseProgram(program);
        attribs(31);
        float[] v = view;
        // sole e "alto" nello spazio della camera (parte rotatoria della matrice di vista)
        GLES20.glUniform3f(uLightV, v[0] * LX + v[4] * LY + v[8] * LZ, v[1] * LX + v[5] * LY + v[9] * LZ, v[2] * LX + v[6] * LY + v[10] * LZ);
        GLES20.glUniform3f(uUpV, v[4], v[5], v[6]);
        GLES20.glUniform3f(uFogColor, fogR, fogG, fogB);
        GLES20.glUniform3f(uGrade, gradeR, gradeG, gradeB);
        GLES20.glUniform1f(uTime, time);
        GLES20.glUniform1i(uTex, 0);
        GLES20.glUniform1i(uShadow, 1);
        GLES20.glUniform1f(uShadowOn, shadows ? 1f : 0f);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, shadows ? shadowTex : whiteTex);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glDepthMask(true);
        for (int i = 0; i < nOps; i++) {
            Op o = ops[i];
            Mesh m = o.m;
            if (o.back) {
                GLES20.glDisable(GLES20.GL_DEPTH_TEST);
                GLES20.glDepthMask(false);
                Mat4.mul(mv, view, ident);
                submit(m, ident, ident, 1, 1, 1, 1, 1, false);
                GLES20.glDepthMask(true);
                GLES20.glEnable(GLES20.GL_DEPTH_TEST);
                continue;
            }
            Mat4.mul(mvp, vp, o.model);
            Mat4.mul(mv, view, o.model);
            Mat4.mul(lmvp, light, o.model);
            GLES20.glUniformMatrix4fv(uLightMVP, 1, false, lmvp, 0);
            if (m.blend) {
                GLES20.glEnable(GLES20.GL_BLEND);
                GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
                GLES20.glDepthMask(false);
            }
            submit(m, mvp, o.model, o.r, o.g, o.b, o.a, o.em, m.fog);
            if (m.blend) {
                GLES20.glDisable(GLES20.GL_BLEND);
                GLES20.glDepthMask(true);
            }
        }
    }

    /** Bagliore: parti luminose rimpicciolite e sfocate; poi tutto insieme sullo schermo con la correzione colore. */
    private void postPass() {
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, quad);
        attribs(1);
        GLES20.glVertexAttribPointer(A_POS, 2, GLES20.GL_FLOAT, false, 8, 0);
        pass(brightProg, bloomFbo[0], bloomW, bloomH, sceneTex, 1f / screenW, 1f / screenH, 0);
        pass(blurProg, bloomFbo[1], bloomW, bloomH, bloomTex[0], 1f / bloomW, 0f, 0);
        pass(blurProg, bloomFbo[0], bloomW, bloomH, bloomTex[1], 0f, 1f / bloomH, 0);
        pass(compProg, 0, screenW, screenH, sceneTex, 1f / screenW, 1f / screenH, bloomTex[0]);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    private void pass(int prog, int fbo, int w, int h, int tex, float tx, float ty, int tex2) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo);
        GLES20.glViewport(0, 0, w, h);
        GLES20.glUseProgram(prog);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(prog, "uTex"), 0);
        GLES20.glUniform2f(GLES20.glGetUniformLocation(prog, "uTexel"), tx, ty);
        if (tex2 != 0) {
            GLES20.glUniform1i(GLES20.glGetUniformLocation(prog, "uBloom"), 1);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex2);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
    }

    // ------------------------------------------------------------------ superfici su cui disegnare

    private void makeTargets(int w, int h) {
        if (w == sizeW && h == sizeH) return;
        sizeW = w;
        sizeH = h;
        int[] t;
        if (shadowFbo == 0) {
            t = target(GlShaders.SHADOW_SIZE, GlShaders.SHADOW_SIZE, GLES20.GL_DEPTH_COMPONENT16, GLES20.GL_NEAREST);
            shadowFbo = t[0];
            shadowTex = t[1];
        }
        if (sceneFbo != 0) {
            GLES20.glDeleteFramebuffers(3, new int[] {sceneFbo, bloomFbo[0], bloomFbo[1]}, 0);
            GLES20.glDeleteTextures(3, new int[] {sceneTex, bloomTex[0], bloomTex[1]}, 0);
            GLES20.glDeleteRenderbuffers(1, new int[] {sceneDepth}, 0);
        }
        t = target(w, h, depth24 ? 0x81A6 : GLES20.GL_DEPTH_COMPONENT16, GLES20.GL_LINEAR);
        sceneFbo = t[0];
        sceneTex = t[1];
        sceneDepth = t[2];
        bloomW = Math.max(1, w / 4);
        bloomH = Math.max(1, h / 4);
        for (int i = 0; i < 2; i++) {
            t = target(bloomW, bloomH, 0, GLES20.GL_LINEAR);
            bloomFbo[i] = t[0];
            bloomTex[i] = t[1];
            if (t[0] == 0) sceneFbo = 0;
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    /** Texture di colore con (se depthFormat != 0) la sua profondità; {0,...} se il telefono non la accetta. */
    private int[] target(int w, int h, int depthFormat, int filter) {
        int[] id = new int[3];
        GLES20.glGenTextures(1, id, 1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id[1]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, w, h, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, filter);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, filter);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glGenFramebuffers(1, id, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, id[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, id[1], 0);
        if (depthFormat != 0) {
            GLES20.glGenRenderbuffers(1, id, 2);
            GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, id[2]);
            GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, depthFormat, w, h);
            GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, id[2]);
        }
        if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) != GLES20.GL_FRAMEBUFFER_COMPLETE) id[0] = 0;
        return id;
    }

    private int makeQuad() {
        float[] q = {-1, -1, 1, -1, -1, 1, -1, 1, 1, -1, 1, 1};
        FloatBuffer fb = dynBuffer(q.length);
        fb.put(q).position(0);
        int[] id = new int[1];
        GLES20.glGenBuffers(1, id, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, id[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, q.length * 4, fb, GLES20.GL_STATIC_DRAW);
        return id[0];
    }

    private void submit(Mesh m, float[] mvpM, float[] model, float r, float g, float b, float a, float emissive, boolean fog) {
        if (m.count == 0) return;
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffer(m));
        int stride = Mesh.STRIDE * 4;
        GLES20.glVertexAttribPointer(A_POS, 3, GLES20.GL_FLOAT, false, stride, 0);
        GLES20.glVertexAttribPointer(A_NORMAL, 3, GLES20.GL_FLOAT, false, stride, 12);
        GLES20.glVertexAttribPointer(A_UV, 2, GLES20.GL_FLOAT, false, stride, 24);
        GLES20.glVertexAttribPointer(A_COLOR, 3, GLES20.GL_FLOAT, false, stride, 32);
        GLES20.glVertexAttribPointer(A_SWAY, 1, GLES20.GL_FLOAT, false, stride, 44);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, m.tex < 0 ? whiteTex : texture(m.tex));
        GLES20.glUniformMatrix4fv(uMVP, 1, false, mvpM, 0);
        GLES20.glUniformMatrix4fv(uMV, 1, false, mv, 0);
        GLES20.glUniform4f(uTint, r, g, b, 1f);
        GLES20.glUniform1f(uAlpha, a);
        GLES20.glUniform1f(uEmissive, emissive);
        GLES20.glUniform1f(uOpaque, m.blend ? 0f : 1f);
        GLES20.glUniform1f(uCut, m.cutout ? 1f : 0f);
        GLES20.glUniform2f(uSpec, m.spec, m.shine);
        if (fog) GLES20.glUniform3f(uFog, fogStart, 1f / (fogEnd - fogStart), fogMax);
        else GLES20.glUniform3f(uFog, 0f, 0f, 0f);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, m.count);
    }

    // ------------------------------------------------------------------ risorse

    /** Buffer dei vertici sulla scheda grafica, creato la prima volta che la mesh viene disegnata. */
    private int buffer(Mesh m) {
        if (m.handle instanceof int[]) {
            int[] h = (int[]) m.handle;
            if (h[0] == generation) {
                if (m.dynamic && h[2] != m.version) {             // stoffa: stessi vertici, posizioni nuove
                    FloatBuffer db = dynBuffer(m.v.length);
                    db.put(m.v).position(0);
                    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, h[1]);
                    GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, m.v.length * 4, db);
                    h[2] = m.version;
                }
                return h[1];
            }
        }
        int[] id = new int[1];
        GLES20.glGenBuffers(1, id, 0);
        FloatBuffer fb = dynBuffer(m.v.length);       // un solo buffer di appoggio, riusato: niente memoria nativa che si accumula
        fb.put(m.v).position(0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, id[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, m.v.length * 4, fb, m.dynamic ? GLES20.GL_DYNAMIC_DRAW : GLES20.GL_STATIC_DRAW);
        m.handle = new int[] {generation, id[0], m.version};
        if (m.releasable && !m.dynamic) m.v = null;
        return id[0];
    }

    private FloatBuffer dyn;

    private FloatBuffer dynBuffer(int floats) {
        if (dyn == null || dyn.capacity() < floats) dyn = ByteBuffer.allocateDirect(floats * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        dyn.clear();
        return dyn;
    }

    private int texture(int img) {
        if (textures[img] != 0) return textures[img];
        Bitmap bmp = bitmaps[img];
        boolean pot = isPot(bmp.getWidth()) && isPot(bmp.getHeight());
        Bitmap scaled = null;
        if (!pot && bmp.getWidth() <= 512 && bmp.getHeight() <= 512) {
            // le immagini piccole vengono portate a potenza di due: servono per ripetizione e sfumatura in lontananza
            scaled = Bitmap.createScaledBitmap(bmp, nextPot(bmp.getWidth()), nextPot(bmp.getHeight()), true);
            pot = true;
        }
        int[] id = new int[1];
        GLES20.glGenTextures(1, id, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id[0]);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, scaled != null ? scaled : bmp, 0);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        if (pot) {
            GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
        } else {
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        }
        if (scaled != null) scaled.recycle();
        textures[img] = id[0];
        return id[0];
    }

    private static boolean isPot(int n) {
        return (n & (n - 1)) == 0;
    }

    private static int nextPot(int n) {
        int p = 1;
        while (p < n) p <<= 1;
        return p;
    }

    private int makeWhite() {
        int[] id = new int[1];
        GLES20.glGenTextures(1, id, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id[0]);
        ByteBuffer px = ByteBuffer.allocateDirect(4);
        px.put((byte) 255).put((byte) 255).put((byte) 255).put((byte) 255).position(0);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 1, 1, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, px);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        return id[0];
    }

    private int link(String vsSrc, String fsSrc) {
        int vs = compile(GLES20.GL_VERTEX_SHADER, vsSrc), fs = compile(GLES20.GL_FRAGMENT_SHADER, fsSrc);
        if (vs == 0 || fs == 0) return 0;
        int p = GLES20.glCreateProgram();
        GLES20.glAttachShader(p, vs);
        GLES20.glAttachShader(p, fs);
        for (int i = 0; i < ATTRS.length; i++) GLES20.glBindAttribLocation(p, i, ATTRS[i]);
        GLES20.glLinkProgram(p);
        int[] ok = new int[1];
        GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, ok, 0);
        if (ok[0] == 0) {
            error = "Errore grafica 3D (link): " + GLES20.glGetProgramInfoLog(p);
            return 0;
        }
        if (vsSrc == GlShaders.SHADOW_VERTEX) {
            sMVP = GLES20.glGetUniformLocation(p, "uMVP");
            sTime = GLES20.glGetUniformLocation(p, "uTime");
            sTex = GLES20.glGetUniformLocation(p, "uTex");
            sCut = GLES20.glGetUniformLocation(p, "uCut");
        }
        return p;
    }

    private void locate(int p) {
        uLightMVP = GLES20.glGetUniformLocation(p, "uLightMVP");
        uShadow = GLES20.glGetUniformLocation(p, "uShadow");
        uShadowOn = GLES20.glGetUniformLocation(p, "uShadowOn");
        uTime = GLES20.glGetUniformLocation(p, "uTime");
        uGrade = GLES20.glGetUniformLocation(p, "uGrade");
        uMVP = GLES20.glGetUniformLocation(p, "uMVP");
        uMV = GLES20.glGetUniformLocation(p, "uMV");
        uLightV = GLES20.glGetUniformLocation(p, "uLightV");
        uUpV = GLES20.glGetUniformLocation(p, "uUpV");
        uSpec = GLES20.glGetUniformLocation(p, "uSpec");
        uCut = GLES20.glGetUniformLocation(p, "uCut");
        uAlpha = GLES20.glGetUniformLocation(p, "uAlpha");
        uTint = GLES20.glGetUniformLocation(p, "uTint");
        uEmissive = GLES20.glGetUniformLocation(p, "uEmissive");
        uFog = GLES20.glGetUniformLocation(p, "uFog");
        uFogColor = GLES20.glGetUniformLocation(p, "uFogColor");
        uOpaque = GLES20.glGetUniformLocation(p, "uOpaque");
        uTex = GLES20.glGetUniformLocation(p, "uTex");
    }

    private int compile(int type, String src) {
        int s = GLES20.glCreateShader(type);
        GLES20.glShaderSource(s, src);
        GLES20.glCompileShader(s);
        int[] ok = new int[1];
        GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, ok, 0);
        if (ok[0] == 0) {
            error = "Errore grafica 3D (shader): " + GLES20.glGetShaderInfoLog(s);
            return 0;
        }
        return s;
    }
}
