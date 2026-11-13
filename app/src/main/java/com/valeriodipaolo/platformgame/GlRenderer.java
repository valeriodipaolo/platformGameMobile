package com.valeriodipaolo.platformgame;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;

import com.valeriodipaolo.platformgame.core.Img;
import com.valeriodipaolo.platformgame.core3d.Gfx3D;
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

    private static final String VERTEX =
            "uniform mat4 uMVP;\n"
            + "uniform mat4 uModel;\n"
            + "uniform mat4 uView;\n"
            + "uniform vec3 uLight;\n"
            + "uniform vec4 uTint;\n"
            + "uniform float uEmissive;\n"
            + "uniform vec3 uFog;\n"                       // inizio, 1/ampiezza, massimo
            + "attribute vec3 aPos;\n"
            + "attribute vec3 aNormal;\n"
            + "attribute vec2 aUV;\n"
            + "attribute vec3 aColor;\n"
            + "varying vec2 vUV;\n"
            + "varying vec4 vColor;\n"
            + "varying float vFog;\n"
            + "void main() {\n"
            + "  vec4 world = uModel * vec4(aPos, 1.0);\n"
            + "  vec3 n = normalize((uModel * vec4(aNormal, 0.0)).xyz);\n"
            + "  float d = max(dot(n, uLight), 0.0);\n"
            + "  float l = mix(" + AMBIENT + " + " + DIFFUSE + " * d, 1.0, uEmissive);\n"
            + "  vColor = vec4(aColor * uTint.rgb * l, uTint.a);\n"
            + "  vec4 vp = uView * world;\n"
            + "  vFog = clamp((-vp.z - uFog.x) * uFog.y, 0.0, uFog.z);\n"
            + "  vUV = aUV;\n"
            + "  gl_Position = uMVP * vec4(aPos, 1.0);\n"
            + "}\n";

    // Le texture arrivano da Android con il colore già moltiplicato per la trasparenza.
    private static final String FRAGMENT =
            "precision mediump float;\n"
            + "uniform sampler2D uTex;\n"
            + "uniform vec3 uFogColor;\n"
            + "uniform float uOpaque;\n"
            + "varying vec2 vUV;\n"
            + "varying vec4 vColor;\n"
            + "varying float vFog;\n"
            + "void main() {\n"
            + "  vec4 t = texture2D(uTex, vUV);\n"
            + "  float a = mix(t.a * vColor.a, 1.0, uOpaque);\n"
            + "  vec3 rgb = t.rgb * vColor.rgb * mix(vColor.a, 1.0, uOpaque);\n"
            + "  if (a < 0.01) discard;\n"
            + "  gl_FragColor = vec4(mix(rgb, uFogColor * a, vFog), a);\n"
            + "}\n";

    private final Bitmap[] bitmaps;
    private final World3D world;
    private final Level3DView host;
    private final Scene3D scene = new Scene3D();

    private int program, generation, whiteTex;
    private int aPos, aNormal, aUV, aColor;
    private int uMVP, uModel, uView, uLight, uTint, uEmissive, uFog, uFogColor, uOpaque, uTex;
    private final int[] textures = new int[Img.COUNT];
    private final float[] view = new float[16], vp = new float[16], mvp = new float[16], ident = Mat4.identity();
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
        java.util.Arrays.fill(textures, 0);
        program = buildProgram();
        whiteTex = makeWhite();
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glClearColor(0.41f, 0.65f, 0.89f, 1f);
        last = 0;
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        aspect = width / (float) Math.max(1, height);
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        long now = System.nanoTime();
        float dt = last == 0 ? 0 : (now - last) / 1e9f;
        last = now;
        host.step(world, dt);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        if (program == 0) return;
        scene.render(world, this, aspect);
    }

    // ------------------------------------------------------------------ Gfx3D

    @Override
    public void begin(float[] view, float[] proj, float fogR, float fogG, float fogB, float fogStart, float fogEnd, float fogMax) {
        System.arraycopy(view, 0, this.view, 0, 16);
        Mat4.mul(vp, proj, view);
        this.fogStart = fogStart;
        this.fogEnd = fogEnd;
        this.fogMax = fogMax;
        GLES20.glUseProgram(program);
        GLES20.glUniformMatrix4fv(uView, 1, false, this.view, 0);
        GLES20.glUniform3f(uLight, LX, LY, LZ);
        GLES20.glUniform3f(uFogColor, fogR, fogG, fogB);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glUniform1i(uTex, 0);
        GLES20.glEnableVertexAttribArray(aPos);
        GLES20.glEnableVertexAttribArray(aNormal);
        GLES20.glEnableVertexAttribArray(aUV);
        GLES20.glEnableVertexAttribArray(aColor);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glDepthMask(true);
    }

    @Override
    public void backdrop(Mesh m) {
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthMask(false);
        submit(m, ident, ident, 1, 1, 1, 1, 1, false);
        GLES20.glDepthMask(true);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    @Override
    public void draw(Mesh m, float[] model, float r, float g, float b, float a, float emissive) {
        Mat4.mul(mvp, vp, model);
        if (m.blend) {
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
            GLES20.glDepthMask(false);
        }
        submit(m, mvp, model, r, g, b, a, emissive, m.fog);
        if (m.blend) {
            GLES20.glDisable(GLES20.GL_BLEND);
            GLES20.glDepthMask(true);
        }
    }

    @Override
    public void end() {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    private void submit(Mesh m, float[] mvpM, float[] model, float r, float g, float b, float a, float emissive, boolean fog) {
        if (m.count == 0) return;
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffer(m));
        int stride = Mesh.STRIDE * 4;
        GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, stride, 0);
        GLES20.glVertexAttribPointer(aNormal, 3, GLES20.GL_FLOAT, false, stride, 12);
        GLES20.glVertexAttribPointer(aUV, 2, GLES20.GL_FLOAT, false, stride, 24);
        GLES20.glVertexAttribPointer(aColor, 3, GLES20.GL_FLOAT, false, stride, 32);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, m.tex < 0 ? whiteTex : texture(m.tex));
        GLES20.glUniformMatrix4fv(uMVP, 1, false, mvpM, 0);
        GLES20.glUniformMatrix4fv(uModel, 1, false, model, 0);
        GLES20.glUniform4f(uTint, r, g, b, a);
        GLES20.glUniform1f(uEmissive, emissive);
        GLES20.glUniform1f(uOpaque, m.blend ? 0f : 1f);
        if (fog) GLES20.glUniform3f(uFog, fogStart, 1f / (fogEnd - fogStart), fogMax);
        else GLES20.glUniform3f(uFog, 0f, 0f, 0f);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, m.count);
    }

    // ------------------------------------------------------------------ risorse

    /** Buffer dei vertici sulla scheda grafica, creato la prima volta che la mesh viene disegnata. */
    private int buffer(Mesh m) {
        if (m.handle instanceof int[]) {
            int[] h = (int[]) m.handle;
            if (h[0] == generation) return h[1];
        }
        int[] id = new int[1];
        GLES20.glGenBuffers(1, id, 0);
        FloatBuffer fb = ByteBuffer.allocateDirect(m.v.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        fb.put(m.v).position(0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, id[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, m.v.length * 4, fb, GLES20.GL_STATIC_DRAW);
        m.handle = new int[] {generation, id[0]};
        return id[0];
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

    private int buildProgram() {
        int vs = compile(GLES20.GL_VERTEX_SHADER, VERTEX), fs = compile(GLES20.GL_FRAGMENT_SHADER, FRAGMENT);
        if (vs == 0 || fs == 0) return 0;
        int p = GLES20.glCreateProgram();
        GLES20.glAttachShader(p, vs);
        GLES20.glAttachShader(p, fs);
        GLES20.glLinkProgram(p);
        int[] ok = new int[1];
        GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, ok, 0);
        if (ok[0] == 0) {
            error = "Errore grafica 3D (link): " + GLES20.glGetProgramInfoLog(p);
            return 0;
        }
        aPos = GLES20.glGetAttribLocation(p, "aPos");
        aNormal = GLES20.glGetAttribLocation(p, "aNormal");
        aUV = GLES20.glGetAttribLocation(p, "aUV");
        aColor = GLES20.glGetAttribLocation(p, "aColor");
        uMVP = GLES20.glGetUniformLocation(p, "uMVP");
        uModel = GLES20.glGetUniformLocation(p, "uModel");
        uView = GLES20.glGetUniformLocation(p, "uView");
        uLight = GLES20.glGetUniformLocation(p, "uLight");
        uTint = GLES20.glGetUniformLocation(p, "uTint");
        uEmissive = GLES20.glGetUniformLocation(p, "uEmissive");
        uFog = GLES20.glGetUniformLocation(p, "uFog");
        uFogColor = GLES20.glGetUniformLocation(p, "uFogColor");
        uOpaque = GLES20.glGetUniformLocation(p, "uOpaque");
        uTex = GLES20.glGetUniformLocation(p, "uTex");
        return p;
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
