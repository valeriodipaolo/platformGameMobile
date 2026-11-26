package com.valeriodipaolo.platformgame.core3d;

/** Gettone d'oro 3D: disco con bordo rialzato e stella in rilievo su entrambe le facce. Asse del disco = y (si gira in disegno). */
final class CoinModel {
    private CoinModel() {}

    static Mesh build() {
        MeshBuilder b = new MeshBuilder().color(184, 118, 18);
        b.cylinder(0, -0.04f, 0.04f, 0, 0.30f, 0.30f, 32, true, true);
        b.color(222, 160, 40);                                   // bordo rialzato
        b.cylinder(0, -0.058f, 0.058f, 0, 0.305f, 0.305f, 32, false, false);
        b.ring(0, 0.058f, 0, 0.245f, 0.305f, 32);
        b.ring(0, -0.058f, 0, 0.245f, 0.305f, 32);
        b.cylinder(0, 0.04f, 0.058f, 0, 0.245f, 0.245f, 32, false, false);
        b.cylinder(0, -0.058f, -0.04f, 0, 0.245f, 0.245f, 32, false, false);
        b.color(236, 178, 56);
        star(b, 0.04f, 0.075f, 0.17f, 0.07f);
        star(b, -0.04f, -0.075f, 0.17f, 0.07f);
        Mesh m = b.build(-1, false, true);
        m.spec = 0.3f;
        m.shine = 18f;
        return m;
    }

    /** Stella a cinque punte estrusa da y0 (sul disco) a y1, raggi esterno e interno. */
    private static void star(MeshBuilder b, float y0, float y1, float ro, float ri) {
        float ny = y1 > y0 ? 1 : -1;
        float[] px = new float[10], pz = new float[10];
        for (int i = 0; i < 10; i++) {
            double a = Math.PI / 2 + i * Math.PI / 5;
            float r = i % 2 == 0 ? ro : ri;
            px[i] = (float) Math.cos(a) * r;
            pz[i] = (float) Math.sin(a) * r;
        }
        for (int i = 0; i < 10; i++) {
            int j = (i + 1) % 10;
            // faccia: punta leggermente bombata al centro
            b.vert(0, y1 + 0.012f * ny, 0, 0, ny, 0, 0.5f, 0.5f);
            b.vert(px[i], y1, pz[i], px[i] * 0.6f, ny, pz[i] * 0.6f, 0.5f, 0.5f);
            b.vert(px[j], y1, pz[j], px[j] * 0.6f, ny, pz[j] * 0.6f, 0.5f, 0.5f);
            // fianco
            float ex = pz[j] - pz[i], ez = -(px[j] - px[i]), l = (float) Math.sqrt(ex * ex + ez * ez);
            if (ex * (px[i] + px[j]) + ez * (pz[i] + pz[j]) < 0) { ex = -ex; ez = -ez; }
            b.quad(px[i], y0, pz[i], px[j], y0, pz[j], px[j], y1, pz[j], px[i], y1, pz[i], ex / l, 0, ez / l, 0.5f, 0.5f, 0.5f, 0.5f);
        }
    }
}
