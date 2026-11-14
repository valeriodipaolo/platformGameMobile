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
            + "uniform mat4 uMV;\n"
            + "uniform vec4 uTint;\n"
            + "uniform vec3 uFog;\n"                       // inizio, 1/ampiezza, massimo
            + "attribute vec3 aPos;\n"
            + "attribute vec3 aNormal;\n"
            + "attribute vec2 aUV;\n"
            + "attribute vec3 aColor;\n"
            + "varying vec2 vUV;\n"
            + "varying vec3 vCol;\n"
            + "varying vec3 vN;\n"
            + "varying vec3 vP;\n"
            + "varying float vFog;\n"
            + "void main() {\n"
            + "  vec4 vp = uMV * vec4(aPos, 1.0);\n"
            + "  vP = vp.xyz;\n"
            + "  vN = (uMV * vec4(aNormal, 0.0)).xyz;\n"
            + "  vCol = aColor * uTint.rgb;\n"
            + "  vFog = clamp((-vp.z - uFog.x) * uFog.y, 0.0, uFog.z);\n"
            + "  vUV = aUV;\n"
            + "  gl_Position = uMVP * vec4(aPos, 1.0);\n"
            + "}\n";

    // Le texture arrivano da Android con il colore già moltiplicato per la trasparenza.
    // Oggetti opachi: luce in spazio lineare (sole, cielo, terra, riflesso, bordo luminoso), foschia, tono filmico.
    private static final String FRAGMENT =
            "precision mediump float;\n"
            + "uniform sampler2D uTex;\n"
            + "uniform vec3 uFogColor;\n"
            + "uniform vec3 uLightV;\n"                     // verso il sole, nello spazio della camera
            + "uniform vec3 uUpV;\n"                        // l'alto, nello spazio della camera
            + "uniform vec2 uSpec;\n"                       // forza e durezza del riflesso
            + "uniform float uOpaque;\n"
            + "uniform float uCut;\n"
            + "uniform float uEmissive;\n"
            + "uniform float uAlpha;\n"
            + "varying vec2 vUV;\n"
            + "varying vec3 vCol;\n"
            + "varying vec3 vN;\n"
            + "varying vec3 vP;\n"
            + "varying float vFog;\n"
            + "void main() {\n"
            + "  vec4 t = texture2D(uTex, vUV);\n"
            + "  vec3 n = normalize(vN);\n"
            + "  if (uOpaque < 0.5) {\n"                     // trasparenze semplici: ombre, anelli, bandiera
            + "    float l = mix(0.6 + 0.4 * max(dot(n, uLightV), 0.0), 1.0, uEmissive);\n"
            + "    float a = t.a * uAlpha;\n"
            + "    if (a < 0.01) discard;\n"
            + "    vec3 rgb = t.rgb * vCol * l * uAlpha;\n"
            + "    gl_FragColor = vec4(mix(rgb, uFogColor * a, vFog), a);\n"
            + "    return;\n"
            + "  }\n"
            + "  if (uCut > 0.5 && t.a < 0.45) discard;\n"
            + "  vec3 tex = t.a > 0.001 ? t.rgb / t.a : t.rgb;\n"
            + "  vec3 srgb = min(tex * vCol, vec3(1.0));\n"
            + "  vec3 alb = srgb * srgb;\n"
            + "  vec3 v = -normalize(vP);\n"
            + "  float nl = max(dot(n, uLightV), 0.0);\n"
            + "  float hemi = dot(n, uUpV) * 0.5 + 0.5;\n"
            + "  vec3 amb = mix(vec3(" + GND_R + ", " + GND_G + ", " + GND_B + "), vec3(" + SKY_R + ", " + SKY_G + ", " + SKY_B + "), hemi);\n"
            + "  vec3 sun = vec3(" + SUN_R + ", " + SUN_G + ", " + SUN_B + ");\n"
            + "  vec3 c = alb * (sun * nl + amb);\n"
            + "  if (uSpec.x > 0.0) {\n"
            + "    vec3 h = normalize(uLightV + v);\n"
            + "    float fres = pow(1.0 - max(dot(n, v), 0.0), 3.0);\n"
            + "    c += sun * uSpec.x * pow(max(dot(n, h), 0.0), uSpec.y) * nl;\n"
            + "    c += vec3(" + SKY_R + ", " + SKY_G + ", " + SKY_B + ") * fres * uSpec.x * 1.4;\n"
            + "  }\n"
            + "  vec3 fogc = uFogColor * uFogColor;\n"
            + "  c = mix(c, fogc, vFog);\n"
            + "  c = (c * (2.51 * c + 0.03)) / (c * (2.43 * c + 0.59) + 0.14);\n"
            + "  c = sqrt(clamp(c, 0.0, 1.0));\n"
            + "  c = mix(c, srgb, uEmissive);\n"
            + "  gl_FragColor = vec4(c, 1.0);\n"
            + "}\n";

    private final Bitmap[] bitmaps;
    private final World3D world;
    private final Level3DView host;
    private final Scene3D scene = new Scene3D();

    private int program, generation, whiteTex;
    private int aPos, aNormal, aUV, aColor;
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
        java.util.Arrays.fill(textures, 0);
        program = buildProgram();
        whiteTex = makeWhite();
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glClearColor(0.745f, 0.847f, 0.933f, 1f);
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
        // sole e "alto" nello spazio della camera (parte rotatoria della matrice di vista)
        GLES20.glUniform3f(uLightV, view[0] * LX + view[4] * LY + view[8] * LZ,
                view[1] * LX + view[5] * LY + view[9] * LZ, view[2] * LX + view[6] * LY + view[10] * LZ);
        GLES20.glUniform3f(uUpV, view[4], view[5], view[6]);
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
        Mat4.mul(mv, this.view, ident);
        submit(m, ident, ident, 1, 1, 1, 1, 1, false);
        GLES20.glDepthMask(true);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    @Override
    public void draw(Mesh m, float[] model, float r, float g, float b, float a, float emissive) {
        Mat4.mul(mvp, vp, model);
        Mat4.mul(mv, this.view, model);
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
