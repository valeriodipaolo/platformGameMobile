package com.valeriodipaolo.platformgame.core3d;

/** Matrici 4x4 in ordine "per colonne" (come OpenGL). Mondo: x a destra, y in alto, z in avanti. */
public final class Mat4 {
    private Mat4() {}

    public static float[] identity() {
        float[] m = new float[16];
        m[0] = m[5] = m[10] = m[15] = 1;
        return m;
    }

    public static void setIdentity(float[] m) {
        for (int i = 0; i < 16; i++) m[i] = 0;
        m[0] = m[5] = m[10] = m[15] = 1;
    }

    /** o = a * b (o non deve coincidere con a o b). */
    public static void mul(float[] o, float[] a, float[] b) {
        for (int c = 0; c < 4; c++) {
            for (int r = 0; r < 4; r++) {
                o[c * 4 + r] = a[r] * b[c * 4] + a[4 + r] * b[c * 4 + 1] + a[8 + r] * b[c * 4 + 2] + a[12 + r] * b[c * 4 + 3];
            }
        }
    }

    public static void perspective(float[] m, float fovyDeg, float aspect, float near, float far) {
        float t = (float) (1.0 / Math.tan(Math.toRadians(fovyDeg) / 2.0));
        for (int i = 0; i < 16; i++) m[i] = 0;
        m[0] = t / aspect;
        m[5] = t;
        m[10] = (far + near) / (near - far);
        m[11] = -1;
        m[14] = 2 * far * near / (near - far);
    }

    /** Vista per un mondo con x a destra e z in avanti (mano sinistra): davanti alla camera la z di vista è negativa. */
    public static void lookAt(float[] m, float ex, float ey, float ez, float cx, float cy, float cz) {
        float fx = cx - ex, fy = cy - ey, fz = cz - ez;
        float fl = (float) Math.sqrt(fx * fx + fy * fy + fz * fz);
        fx /= fl; fy /= fl; fz /= fl;
        // destra = su x avanti (con su = 0,1,0)
        float sx = fz, sy = 0, sz = -fx;
        float sl = (float) Math.sqrt(sx * sx + sz * sz);
        sx /= sl; sz /= sl;
        // su = avanti x destra
        float ux = fy * sz - fz * sy, uy = fz * sx - fx * sz, uz = fx * sy - fy * sx;
        m[0] = sx; m[4] = sy; m[8] = sz;
        m[1] = ux; m[5] = uy; m[9] = uz;
        m[2] = -fx; m[6] = -fy; m[10] = -fz;
        m[3] = 0; m[7] = 0; m[11] = 0;
        m[12] = -(sx * ex + sy * ey + sz * ez);
        m[13] = -(ux * ex + uy * ey + uz * ez);
        m[14] = fx * ex + fy * ey + fz * ez;
        m[15] = 1;
    }

    // Le operazioni seguenti moltiplicano a destra: m = m * T.

    public static void translate(float[] m, float x, float y, float z) {
        for (int r = 0; r < 4; r++) m[12 + r] += m[r] * x + m[4 + r] * y + m[8 + r] * z;
    }

    public static void scale(float[] m, float sx, float sy, float sz) {
        for (int r = 0; r < 4; r++) {
            m[r] *= sx;
            m[4 + r] *= sy;
            m[8 + r] *= sz;
        }
    }

    /** Angolo positivo: l'asse z (avanti) ruota verso x (destra). */
    public static void rotateY(float[] m, float a) {
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        for (int r = 0; r < 4; r++) {
            float x = m[r], z = m[8 + r];
            m[r] = x * c - z * s;
            m[8 + r] = x * s + z * c;
        }
    }

    /** Angolo positivo: un arto che pende verso il basso oscilla all'indietro. */
    public static void rotateX(float[] m, float a) {
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        for (int r = 0; r < 4; r++) {
            float y = m[4 + r], z = m[8 + r];
            m[4 + r] = y * c + z * s;
            m[8 + r] = -y * s + z * c;
        }
    }

    /** Angolo positivo: un arto che pende verso il basso si alza verso destra. */
    public static void rotateZ(float[] m, float a) {
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        for (int r = 0; r < 4; r++) {
            float x = m[r], y = m[4 + r];
            m[r] = x * c + y * s;
            m[4 + r] = -x * s + y * c;
        }
    }
}
