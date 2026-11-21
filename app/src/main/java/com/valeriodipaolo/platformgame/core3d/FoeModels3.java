package com.valeriodipaolo.platformgame.core3d;

import com.valeriodipaolo.platformgame.core.Img;

/**
 * Nemici di stagione realistici: stesso comportamento di un nemico di base, animale del posto. Ogni modello guarda verso +z
 * con i piedi a y = 0, come gli altri. Si costruiscono solo quando servono la prima volta.
 *
 * Cavalletta (estate, salta come la ghianda), istrice (estate) e processionaria (primavera) al posto del riccio, topo
 * selvatico (autunno) al posto dello scarabeo, tartaruga (estate) al posto della lumaca, calabrone (estate), rondine
 * (primavera), gazza (inverno) e falco (cielo) al posto del corvo.
 */
final class FoeModels3 {
    private final float[] a = new float[16], b2 = new float[16];

    private static float[] rgb(int r, int g, int b) { return new float[] {r / 255f, g / 255f, b / 255f}; }

    private static Mesh fin(MeshBuilder b, int tex, float spec, float shine) {
        Mesh m = b.build(tex, false, true);
        m.spec = spec; m.shine = shine;
        return m;
    }

    /** Corpo morbido lungo z: profilo dato per punti, ricampionato fitto e smussato. */
    static void body(MeshBuilder b, float[][] c, float[] rx, float[] ry, int n, int segs, FoeModels.Paint p, float u, float v) {
        float[][] c2 = new float[n][3];
        float[] rx2 = new float[n], ry2 = new float[n];
        for (int i = 0; i < n; i++) {
            float f = i / (n - 1f) * (c.length - 1);
            int k = Math.min(c.length - 2, (int) f);
            float t = f - k, ts = t * t * (3 - 2 * t);
            for (int q = 0; q < 3; q++) c2[i][q] = c[k][q] + (c[k + 1][q] - c[k][q]) * t;
            rx2[i] = rx[k] + (rx[k + 1] - rx[k]) * ts; ry2[i] = ry[k] + (ry[k + 1] - ry[k]) * ts;
        }
        FoeModels.loft(b, c2, rx2, ry2, 0, 1, 0, segs, 1f, p, u, v);
    }

    /** Dorso di un colore, ventre di un altro (sin a < 0 è sotto). */
    private static FoeModels.Paint twoTone(final float[] top, final float[] top2, final float[] belly, final float freq) {
        return new FoeModels.Paint() {
            @Override void col(float t, float an, float x, float y, float z, float[] o) {
                FoeModels.mix(top, top2, 0.5f + 0.5f * (float) Math.sin(x * freq + z * freq * 0.7f + y * 9f), o);
                FoeModels.mix(o, belly, FoeModels.sm(-(float) Math.sin(an) * 2.5f - 0.2f), o);
            }
        };
    }

    // ================================================================== cavalletta
    private Mesh hopBody, hopEyes, hopLeg, hopWing;

    private void buildHopper() {
        final float[] green = rgb(110, 150, 52), green2 = rgb(140, 168, 70), pale = rgb(196, 204, 120), brown = rgb(120, 104, 58);
        MeshBuilder b = new MeshBuilder();
        FoeModels.Paint skin = twoTone(green, green2, pale, 60f);
        // addome a segmenti (righe chiare), torace a sella, testa alta e lunga
        FoeModels.Paint abdomen = new FoeModels.Paint() {
            @Override void col(float t, float an, float x, float y, float z, float[] o) {
                FoeModels.mix(green, pale, FoeModels.sm((float) Math.sin(t * 44f) * 3f) * 0.35f, o);
                FoeModels.mix(o, pale, FoeModels.sm(-(float) Math.sin(an) * 2.5f), o);
            }
        };
        body(b, new float[][] {{0, 0.16f, -0.44f}, {0, 0.17f, -0.34f}, {0, 0.18f, -0.18f}, {0, 0.19f, -0.04f}}, new float[] {0.02f, 0.06f, 0.075f, 0.075f}, new float[] {0.02f, 0.06f, 0.08f, 0.08f}, 16, 14, abdomen, 2f, 2f);
        body(b, new float[][] {{0, 0.19f, -0.06f}, {0, 0.21f, 0.04f}, {0, 0.22f, 0.14f}, {0, 0.22f, 0.2f}}, new float[] {0.075f, 0.085f, 0.08f, 0.06f}, new float[] {0.08f, 0.095f, 0.09f, 0.07f}, 12, 14, skin, 2f, 2f);
        body(b, new float[][] {{0, 0.22f, 0.18f}, {0, 0.24f, 0.24f}, {0, 0.22f, 0.31f}, {0, 0.17f, 0.34f}, {0, 0.12f, 0.33f}}, new float[] {0.055f, 0.06f, 0.055f, 0.04f, 0.02f}, new float[] {0.07f, 0.07f, 0.065f, 0.05f, 0.02f}, 12, 12, skin, 1f, 1f);
        // antenne lunghe e sottili, piegate all'indietro
        for (int s = -1; s <= 1; s += 2)
            FoeModels.tube(b, new float[][] {{s * 0.025f, 0.29f, 0.3f}, {s * 0.06f, 0.38f, 0.3f}, {s * 0.1f, 0.44f, 0.2f}, {s * 0.14f, 0.45f, 0.05f}}, new float[] {0.006f, 0.005f, 0.004f, 0.002f}, 4, FoeModels.along(green, brown));
        // zampe anteriori e mediane
        for (int s = -1; s <= 1; s += 2) for (int k = 0; k < 2; k++) {
            float z0 = 0.13f - k * 0.1f;
            FoeModels.tube(b, new float[][] {{s * 0.05f, 0.15f, z0}, {s * 0.12f, 0.12f, z0 + 0.04f}, {s * 0.14f, 0.0f, z0 + 0.06f}}, new float[] {0.012f, 0.009f, 0.006f}, 5, FoeModels.along(green, brown));
        }
        hopBody = fin(b, Img.FOE_TRAP, 0.3f, 30f);
        b = new MeshBuilder();
        for (int s = -1; s <= 1; s += 2) {
            b.colorf(0.46f, 0.36f, 0.22f); b.ellipsoid(s * 0.05f, 0.255f, 0.27f, 0.025f, 0.035f, 0.03f, 10, 7);
            b.colorf(0.12f, 0.1f, 0.06f); b.ellipsoid(s * 0.06f, 0.255f, 0.275f, 0.01f, 0.025f, 0.015f, 6, 4);
        }
        hopEyes = fin(b, -1, 0.9f, 60f);
        // ali chiuse: due lame lunghe sopra il corpo, venate
        b = new MeshBuilder();
        for (int s = -1; s <= 1; s += 2)
            FoeModels.loft(b, new float[][] {{s * 0.04f, 0.27f, 0.1f}, {s * 0.06f, 0.25f, -0.1f}, {s * 0.065f, 0.22f, -0.32f}, {s * 0.05f, 0.18f, -0.48f}},
                    new float[] {0.03f, 0.05f, 0.05f, 0.01f}, new float[] {0.004f, 0.005f, 0.005f, 0.003f}, s, 1, 0, 8, 1f, FoeModels.along(rgb(126, 150, 70), rgb(150, 146, 92)), 1f, 3f);
        hopWing = fin(b, Img.FOE_TRAP, 0.25f, 24f);
        // zampa posteriore destra: femore grosso a coscia di pollo che sale, tibia sottile spinosa che scende
        b = new MeshBuilder();
        FoeModels.Paint femur = new FoeModels.Paint() {
            @Override void col(float t, float an, float x, float y, float z, float[] o) {
                FoeModels.mix(green, rgb(80, 110, 40), FoeModels.sm((float) Math.sin(t * 20f + an * 3f) * 2f) * 0.5f, o);   // spina di pesce
            }
        };
        FoeModels.tube(b, new float[][] {{0.07f, 0.17f, -0.02f}, {0.1f, 0.24f, -0.12f}, {0.11f, 0.29f, -0.22f}, {0.11f, 0.3f, -0.27f}}, new float[] {0.035f, 0.045f, 0.03f, 0.018f}, 8, femur);
        FoeModels.tube(b, new float[][] {{0.11f, 0.3f, -0.27f}, {0.115f, 0.18f, -0.18f}, {0.12f, 0.05f, -0.08f}}, new float[] {0.012f, 0.01f, 0.008f}, 5, FoeModels.along(rgb(150, 110, 70), rgb(130, 90, 60)));
        for (int k = 1; k < 6; k++) {                                       // spine rossicce sulla tibia
            float u = k / 6f, x = 0.11f + 0.01f * u, y = 0.3f - 0.25f * u, z = -0.27f + 0.19f * u;
            FoeModels.tube(b, x, y, z, x + 0.02f, y - 0.005f, z - 0.015f, 0.004f, 0.001f, 3, FoeModels.along(rgb(170, 70, 50), rgb(120, 40, 30)));
        }
        FoeModels.tube(b, 0.12f, 0.05f, -0.08f, 0.13f, 0.0f, -0.0f, 0.007f, 0.005f, 4, FoeModels.along(brown, brown));
        hopLeg = fin(b, Img.FOE_TRAP, 0.3f, 30f);
    }

    void drawHopper(Gfx3D g, float[] m, Enemy3 e) {
        if (hopBody == null) buildHopper();
        boolean air = e.y > e.baseY + 0.05f;
        System.arraycopy(m, 0, a, 0, 16);
        Mat4.scale(a, 1.6f, 1.6f, 1.6f);                                     // grande come un gatto: si vede da lontano
        if (air) Mat4.rotateX(a, -0.25f);
        g.draw(hopBody, a, 1, 1, 1, 1, 0);
        g.draw(hopEyes, a, 1, 1, 1, 1, 0.05f);
        g.draw(hopWing, a, 1, 1, 1, 1, 0);
        for (int s = -1; s <= 1; s += 2) {
            System.arraycopy(a, 0, b2, 0, 16);
            if (s < 0) Mat4.scale(b2, -1, 1, 1);
            if (air) { Mat4.translate(b2, 0.07f, 0.17f, -0.02f); Mat4.rotateX(b2, -1.0f); Mat4.translate(b2, -0.07f, -0.17f, 0.02f); }   // in volo la zampa si distende dietro
            else { Mat4.translate(b2, 0.07f, 0.17f, -0.02f); Mat4.rotateX(b2, 0.05f * (float) Math.sin(e.anim * 6f)); Mat4.translate(b2, -0.07f, -0.17f, 0.02f); }
            g.draw(hopLeg, b2, 1, 1, 1, 1, 0);
        }
    }

    // ================================================================== topo selvatico
    private Mesh mouseBody, mouseEyes, mouseTail, mouseFoot;

    private void buildMouse() {
        final float[] fur = rgb(132, 96, 62), fur2 = rgb(150, 112, 74), belly = rgb(222, 206, 180), pink = rgb(214, 150, 146);
        MeshBuilder b = new MeshBuilder();
        FoeModels.Paint coat = twoTone(fur, fur2, belly, 40f);
        body(b, new float[][] {{0, 0.12f, -0.26f}, {0, 0.15f, -0.2f}, {0, 0.17f, -0.08f}, {0, 0.17f, 0.04f}, {0, 0.17f, 0.14f}, {0, 0.16f, 0.22f}, {0, 0.14f, 0.3f}, {0, 0.12f, 0.36f}, {0, 0.11f, 0.39f}},
                new float[] {0.02f, 0.11f, 0.14f, 0.14f, 0.12f, 0.09f, 0.06f, 0.03f, 0.008f}, new float[] {0.02f, 0.1f, 0.13f, 0.13f, 0.11f, 0.09f, 0.06f, 0.03f, 0.008f}, 30, 22, coat, 3f, 3f);
        for (int s = -1; s <= 1; s += 2) {                                 // orecchie grandi, sottili, rosa dentro
            FoeModels.ballY(b, s * 0.07f, 0.27f, 0.2f, 0.045f, 0.05f, 0.012f, 12, 8, FoeModels.along(pink, fur), 1f, 1f);
        }
        b.colorf(0.25f, 0.15f, 0.13f); b.ellipsoid(0, 0.112f, 0.392f, 0.014f, 0.012f, 0.012f, 8, 5);     // naso
        mouseBody = fin(b, Img.FOE_PENGFUR, 0.05f, 8f);
        b = new MeshBuilder();
        for (int s = -1; s <= 1; s += 2) {
            b.colorf(0.03f, 0.02f, 0.02f); b.ellipsoid(s * 0.052f, 0.19f, 0.29f, 0.02f, 0.022f, 0.018f, 10, 7);
            b.colorf(0.9f, 0.9f, 0.9f); b.ellipsoid(s * 0.055f, 0.198f, 0.303f, 0.005f, 0.005f, 0.003f, 5, 3);
            for (int k = -1; k <= 1; k++)                                  // vibrisse
                FoeModels.tube(b, s * 0.02f, 0.12f, 0.37f, s * 0.13f, 0.12f + k * 0.03f, 0.34f + k * 0.02f, 0.002f, 0.001f, 3, FoeModels.along(rgb(60, 50, 40), rgb(200, 190, 180)));
        }
        mouseEyes = fin(b, -1, 0.9f, 80f);
        b = new MeshBuilder();
        FoeModels.Paint ring = new FoeModels.Paint() {
            @Override void col(float t, float an, float x, float y, float z, float[] o) { FoeModels.mix(pink, rgb(180, 120, 116), 0.5f + 0.5f * (float) Math.sin(t * 70f), o); }
        };
        FoeModels.tube(b, new float[][] {{0, 0.12f, -0.24f}, {0.03f, 0.08f, -0.36f}, {0.08f, 0.05f, -0.48f}, {0.06f, 0.06f, -0.6f}, {-0.02f, 0.1f, -0.7f}}, new float[] {0.02f, 0.015f, 0.011f, 0.008f, 0.004f}, 6, ring);
        mouseTail = fin(b, -1, 0.3f, 30f);
        b = new MeshBuilder();
        FoeModels.tube(b, 0, 0.06f, 0, 0, 0.0f, 0.03f, 0.022f, 0.016f, 6, FoeModels.along(fur, pink));
        for (int k = -1; k <= 1; k++) FoeModels.tube(b, 0, 0.005f, 0.03f, k * 0.015f, 0.003f, 0.065f, 0.006f, 0.004f, 4, FoeModels.along(pink, pink));
        mouseFoot = fin(b, -1, 0.2f, 20f);
    }

    void drawMouse(Gfx3D g, float[] m, Enemy3 e) {
        if (mouseBody == null) buildMouse();
        float run = e.anim * 16f;
        System.arraycopy(m, 0, a, 0, 16);
        Mat4.scale(a, 1.35f, 1.35f, 1.35f);
        Mat4.translate(a, 0, 0.012f * Math.abs((float) Math.sin(run)), 0);   // corsa a balzelli
        g.draw(mouseBody, a, 1, 1, 1, 1, 0);
        g.draw(mouseEyes, a, 1, 1, 1, 1, 0.05f);
        System.arraycopy(a, 0, b2, 0, 16);
        Mat4.translate(b2, 0, 0.12f, -0.24f);
        Mat4.rotateY(b2, 0.3f * (float) Math.sin(e.anim * 5f));
        Mat4.translate(b2, 0, -0.12f, 0.24f);
        g.draw(mouseTail, b2, 1, 1, 1, 1, 0);
        float[][] feet = {{0.08f, 0.17f}, {-0.08f, 0.17f}, {0.09f, -0.12f}, {-0.09f, -0.12f}};
        for (int i = 0; i < 4; i++) {
            System.arraycopy(a, 0, b2, 0, 16);
            Mat4.translate(b2, feet[i][0], 0, feet[i][1] + 0.03f * (float) Math.sin(run + (i < 2 ? 0 : 3.1f)));
            g.draw(mouseFoot, b2, 1, 1, 1, 1, 0);
        }
    }

    // ================================================================== istrice
    private Mesh porcBody, porcQuills, porcLeg;

    private void buildPorcupine() {
        final float[] dark = rgb(40, 34, 30), dark2 = rgb(58, 48, 40);
        MeshBuilder b = new MeshBuilder();
        body(b, new float[][] {{0, 0.22f, -0.32f}, {0, 0.26f, -0.2f}, {0, 0.27f, 0f}, {0, 0.25f, 0.18f}, {0, 0.2f, 0.3f}, {0, 0.15f, 0.38f}, {0, 0.13f, 0.42f}},
                new float[] {0.05f, 0.2f, 0.24f, 0.2f, 0.12f, 0.06f, 0.02f}, new float[] {0.05f, 0.18f, 0.2f, 0.17f, 0.11f, 0.06f, 0.02f}, 24, 20, twoTone(dark, dark2, rgb(70, 60, 52), 30f), 3f, 3f);
        b.colorf(0.08f, 0.06f, 0.05f);
        for (int s = -1; s <= 1; s += 2) b.ellipsoid(s * 0.06f, 0.22f, 0.34f, 0.016f, 0.018f, 0.014f, 8, 5);
        b.colorf(0.18f, 0.12f, 0.1f); b.ellipsoid(0, 0.13f, 0.425f, 0.025f, 0.02f, 0.018f, 8, 5);
        porcBody = fin(b, Img.FOE_PENGFUR, 0.05f, 8f);
        // aculei lunghi a fasce bianche e nere, rivolti all'indietro, più lunghi sulla groppa; cresta di setole sul capo
        b = new MeshBuilder();
        java.util.Random r = new java.util.Random(31);
        float[] w = rgb(236, 232, 222), k = rgb(24, 20, 18);
        for (int i = 0; i < 230; i++) {
            float u = r.nextFloat(), ph = (float) Math.PI * (0.08f + 0.84f * r.nextFloat());
            float z = 0.2f - 0.5f * u;
            float rr = 0.2f * (float) Math.sin(Math.PI * (u * 0.8f + 0.15f)) + 0.04f;
            float x = (float) Math.cos(ph) * rr, y = 0.25f + (float) Math.sin(ph) * rr * 0.9f;
            float len = 0.22f + 0.28f * (float) Math.sin(Math.PI * Math.min(1f, u * 1.1f)) * (0.7f + 0.3f * r.nextFloat());
            FoeModels.quill(b, x, y, z, x * 0.6f, (float) Math.sin(ph) * 0.5f + 0.25f, -1f, len, 0.012f, k, r.nextBoolean() ? w : k, w);
        }
        for (int i = 0; i < 26; i++) {
            float ph = (float) Math.PI * (0.25f + 0.5f * i / 25f);
            FoeModels.quill(b, (float) Math.cos(ph) * 0.06f, 0.31f, 0.2f - 0.05f * (i % 3), (float) Math.cos(ph) * 0.2f, 1f, -0.5f, 0.16f, 0.006f, k, w, w);
        }
        porcQuills = fin(b, -1, 0.25f, 24f);
        b = new MeshBuilder();
        FoeModels.tube(b, 0, 0.14f, 0, 0, 0.0f, 0.02f, 0.04f, 0.03f, 6, FoeModels.along(dark, dark));
        porcLeg = fin(b, -1, 0.05f, 8f);
    }

    void drawPorcupine(Gfx3D g, float[] m, Enemy3 e) {
        if (porcBody == null) buildPorcupine();
        float walk = e.anim * 9f;
        System.arraycopy(m, 0, a, 0, 16);
        Mat4.rotateZ(a, 0.03f * (float) Math.sin(walk));
        g.draw(porcBody, a, 1, 1, 1, 1, 0);
        System.arraycopy(a, 0, b2, 0, 16);
        float puff = 1f + 0.06f * Math.max(0f, (float) Math.sin(e.anim * 2.5f));          // ogni tanto rizza gli aculei
        Mat4.translate(b2, 0, 0.25f, 0);
        Mat4.scale(b2, puff, puff, 1f);
        Mat4.translate(b2, 0, -0.25f, 0);
        g.draw(porcQuills, b2, 1, 1, 1, 1, 0);
        for (int i = 0; i < 4; i++) {
            int s = i % 2 == 0 ? -1 : 1;
            boolean front = i >= 2;
            System.arraycopy(m, 0, b2, 0, 16);
            Mat4.translate(b2, s * 0.15f, 0.14f, front ? 0.2f : -0.16f);
            Mat4.rotateX(b2, (float) Math.sin(walk + (s > 0 == front ? 0 : Math.PI)) * 0.5f);
            Mat4.translate(b2, 0, -0.14f, 0);
            g.draw(porcLeg, b2, 1, 1, 1, 1, 0);
        }
    }

    // ================================================================== processionaria (bruco peloso)
    private Mesh catSeg, catHead;

    private void buildCaterpillar() {
        MeshBuilder b = new MeshBuilder();
        final float[] base = rgb(70, 56, 46), band = rgb(206, 120, 46);
        FoeModels.Paint seg = new FoeModels.Paint() {
            @Override void col(float t, float an, float x, float y, float z, float[] o) {
                FoeModels.mix(base, band, FoeModels.sm(((float) Math.sin(an) - 0.75f) * 8f), o);   // fascia arancio sul dorso
            }
        };
        FoeModels.ballZ(b, 0, 0, 0, 0.09f, 0.08f, 0.07f, 14, 9, seg, 1f, 1f);
        java.util.Random r = new java.util.Random(41);
        for (int i = 0; i < 40; i++) {                                     // ciuffi di peli urticanti, chiari in punta
            float ph = (float) Math.PI * (0.1f + 0.8f * r.nextFloat()), z = -0.04f + 0.08f * r.nextFloat();
            float x = (float) Math.cos(ph) * 0.085f, y = (float) Math.sin(ph) * 0.075f;
            float l = 0.07f + 0.06f * r.nextFloat();
            FoeModels.tube(b, x, y, z, x * (1 + l * 10), y + (float) Math.sin(ph) * l, z + (r.nextFloat() - 0.5f) * 0.03f, 0.003f, 0.001f, 3, FoeModels.along(rgb(150, 120, 90), rgb(232, 220, 196)));
        }
        catSeg = fin(b, Img.FOE_PENGFUR, 0.1f, 12f);
        b = new MeshBuilder();
        b.colorf(0.12f, 0.09f, 0.08f); b.ellipsoid(0, 0, 0, 0.075f, 0.07f, 0.065f, 12, 8);
        b.colorf(0.02f, 0.02f, 0.02f); for (int s = -1; s <= 1; s += 2) b.ellipsoid(s * 0.035f, 0.02f, 0.055f, 0.012f, 0.012f, 0.01f, 6, 4);
        catHead = fin(b, -1, 0.6f, 50f);
    }

    void drawCaterpillar(Gfx3D g, float[] m, Enemy3 e) {
        if (catSeg == null) buildCaterpillar();
        for (int i = 0; i < 9; i++) {                                      // nove anelli che ondeggiano: l'onda corre dalla coda alla testa
            float z = 0.32f - i * 0.085f, lift = Math.max(0f, (float) Math.sin(e.anim * 7f - i * 0.8f)) * 0.06f;
            System.arraycopy(m, 0, a, 0, 16);
            Mat4.translate(a, 0.02f * (float) Math.sin(e.anim * 2f + i * 0.5f), 0.08f + lift, z - lift * 0.4f);
            Mat4.scale(a, 1.4f, 1.4f, 1.4f);
            g.draw(catSeg, a, 1, 1, 1, 1, 0);
            if (i == 0) { Mat4.translate(a, 0, 0, 0.09f); g.draw(catHead, a, 1, 1, 1, 1, 0); }
        }
    }

    // ================================================================== tartaruga
    private Mesh tortShell, tortHead, tortLeg;

    private void buildTortoise() {
        MeshBuilder b = new MeshBuilder();
        final float[] scuteC = rgb(196, 160, 70), scuteB = rgb(52, 40, 26);
        FoeModels.Paint shell = new FoeModels.Paint() {
            @Override void col(float t, float an, float x, float y, float z, float[] o) {
                // scudi: celle esagonali (centro giallo, bordo scuro), anelli di crescita
                float gx = x * 9f, gz = z * 9f;
                float cx = Math.round(gx), cz = Math.round(gz + ((int) Math.floor(gx + 0.5f) % 2 == 0 ? 0 : 0.5f));
                float d = (float) Math.sqrt((gx - cx) * (gx - cx) + (gz - cz) * (gz - cz)) * 2f;
                FoeModels.mix(scuteC, scuteB, FoeModels.sm((d - 0.75f) * 6f), o);
                float rings = 0.9f + 0.1f * (float) Math.sin(d * 25f);
                o[0] *= rings; o[1] *= rings; o[2] *= rings;
                if ((float) Math.sin(an) < 0.15f) FoeModels.mix(o, rgb(70, 56, 36), 0.8f, o);     // bordo inferiore
            }
        };
        FoeModels.ballZ(b, 0, 0.12f, 0, 0.27f, 0.22f, 0.32f, 26, 16, shell, 2f, 2f);
        b.colorf(0.72f, 0.62f, 0.4f); b.ellipsoid(0, 0.1f, 0, 0.26f, 0.04f, 0.3f, 18, 6);   // piastrone
        tortShell = fin(b, -1, 0.2f, 18f);
        final float[] scale = rgb(120, 112, 80), scale2 = rgb(150, 140, 100);
        b = new MeshBuilder();
        FoeModels.tube(b, new float[][] {{0, 0.13f, 0.22f}, {0, 0.15f, 0.32f}, {0, 0.17f, 0.38f}}, new float[] {0.05f, 0.045f, 0.045f}, 10, twoTone(scale, scale2, rgb(180, 168, 120), 80f));
        FoeModels.ballZ(b, 0, 0.18f, 0.42f, 0.05f, 0.045f, 0.065f, 12, 8, twoTone(scale, scale2, rgb(180, 168, 120), 80f), 1f, 1f);
        b.colorf(0.05f, 0.04f, 0.03f); for (int s = -1; s <= 1; s += 2) b.ellipsoid(s * 0.035f, 0.195f, 0.45f, 0.01f, 0.01f, 0.008f, 6, 4);
        b.colorf(0.3f, 0.28f, 0.2f); b.ellipsoid(0, 0.17f, 0.48f, 0.02f, 0.008f, 0.012f, 6, 3);   // becco corneo
        tortHead = fin(b, Img.FOE_FROG, 0.2f, 20f);
        b = new MeshBuilder();
        FoeModels.tube(b, 0, 0.1f, 0, 0, 0f, 0.02f, 0.045f, 0.04f, 8, twoTone(scale, scale2, scale, 90f));
        for (int k = -1; k <= 1; k++) { b.colorf(0.25f, 0.22f, 0.16f); b.ellipsoid(k * 0.02f, 0.008f, 0.06f, 0.008f, 0.006f, 0.012f, 5, 3); }
        tortLeg = fin(b, Img.FOE_FROG, 0.1f, 12f);
    }

    void drawTortoise(Gfx3D g, float[] m, Enemy3 e) {
        if (tortShell == null) buildTortoise();
        boolean hide = e.mode == 1, flip = e.mode == 2;
        System.arraycopy(m, 0, a, 0, 16);
        Mat4.scale(a, 1.25f, 1.25f, 1.25f);
        if (flip) { Mat4.translate(a, 0, 0.33f, 0); Mat4.rotateZ(a, (float) Math.PI); }   // rovesciata sul guscio
        g.draw(tortShell, a, 1, 1, 1, 1, 0);
        float out = hide ? 0.3f : 1f;
        float walk = e.anim * 6f;
        System.arraycopy(a, 0, b2, 0, 16);
        Mat4.translate(b2, 0, 0, (out - 1f) * 0.2f + 0.01f * (float) Math.sin(walk));
        if (!hide) g.draw(tortHead, b2, 1, 1, 1, 1, 0);
        float[][] legs = {{0.2f, 0.18f}, {-0.2f, 0.18f}, {0.19f, -0.18f}, {-0.19f, -0.18f}};
        for (int i = 0; i < 4; i++) {
            System.arraycopy(a, 0, b2, 0, 16);
            Mat4.translate(b2, legs[i][0] * (hide ? 0.7f : 1f), hide ? 0.06f : 0f, legs[i][1] + (hide ? 0 : 0.03f * (float) Math.sin(walk + i * 1.6f)));
            if (hide) Mat4.scale(b2, 0.6f, 0.6f, 0.6f);
            g.draw(tortLeg, b2, 1, 1, 1, 1, 0);
        }
    }

    // ================================================================== calabrone
    private Mesh waspBody, waspWing, waspLegs;

    private void buildWasp() {
        final float[] yel = rgb(226, 170, 30), blk = rgb(40, 28, 16), brn = rgb(120, 64, 30);
        MeshBuilder b = new MeshBuilder();
        FoeModels.Paint abd = new FoeModels.Paint() {
            @Override void col(float t, float an, float x, float y, float z, float[] o) { FoeModels.mix(yel, blk, FoeModels.sm((float) Math.sin(t * 22f + 1f) * 4f), o); }
        };
        body(b, new float[][] {{0, 0.0f, -0.12f}, {0, -0.02f, -0.22f}, {0, -0.06f, -0.36f}, {0, -0.1f, -0.46f}, {0, -0.12f, -0.5f}}, new float[] {0.04f, 0.1f, 0.1f, 0.06f, 0.005f}, new float[] {0.04f, 0.1f, 0.09f, 0.05f, 0.005f}, 20, 16, abd, 2f, 2f);
        FoeModels.tube(b, 0, 0.0f, -0.07f, 0, 0.0f, -0.13f, 0.02f, 0.02f, 6, FoeModels.along(blk, blk));               // vita stretta
        FoeModels.ballZ(b, 0, 0.02f, 0.02f, 0.08f, 0.08f, 0.1f, 14, 9, FoeModels.along(brn, blk), 1f, 1f);         // torace bruno
        FoeModels.ballZ(b, 0, 0.03f, 0.15f, 0.065f, 0.06f, 0.05f, 12, 8, FoeModels.along(yel, brn), 1f, 1f);       // testa
        FoeModels.tube(b, 0, -0.12f, -0.5f, 0, -0.14f, -0.55f, 0.006f, 0.001f, 4, FoeModels.along(blk, blk));     // pungiglione
        for (int s = -1; s <= 1; s += 2) {
            b.colorf(0.3f, 0.12f, 0.06f); b.ellipsoid(s * 0.05f, 0.05f, 0.17f, 0.025f, 0.04f, 0.03f, 8, 6);       // occhi composti
            FoeModels.tube(b, new float[][] {{s * 0.02f, 0.07f, 0.19f}, {s * 0.05f, 0.13f, 0.24f}, {s * 0.08f, 0.12f, 0.3f}}, new float[] {0.006f, 0.005f, 0.004f}, 4, FoeModels.along(blk, brn));
        }
        waspBody = fin(b, Img.FOE_PENGFUR, 0.4f, 40f);
        b = new MeshBuilder();
        FoeModels.Paint leg = FoeModels.along(brn, yel);
        for (int s = -1; s <= 1; s += 2) for (int k = 0; k < 3; k++) {
            float z0 = 0.06f - k * 0.05f;
            FoeModels.tube(b, new float[][] {{s * 0.05f, -0.04f, z0}, {s * 0.1f, -0.1f, z0 - 0.02f}, {s * 0.09f, -0.2f, z0 - 0.06f}}, new float[] {0.01f, 0.008f, 0.005f}, 4, leg);
        }
        waspLegs = fin(b, -1, 0.3f, 30f);
        // ala: membrana trasparente fumé con le venature, dall'attaccatura verso +x
        b = new MeshBuilder();
        b.colorf(0.7f, 0.62f, 0.45f);
        int n = 10;
        for (int i = 0; i < n; i++) {
            float t0 = i / (float) n, t1 = (i + 1f) / n;
            float x0 = t0 * 0.42f, x1 = t1 * 0.42f, w0 = 0.075f * (float) Math.sin(Math.PI * Math.min(1f, t0 * 1.1f + 0.05f)), w1 = 0.075f * (float) Math.sin(Math.PI * Math.min(1f, t1 * 1.1f + 0.05f));
            b.vert(x0, 0, w0, 0, 1, 0, t0, 0); b.vert(x1, 0, w1, 0, 1, 0, t1, 0); b.vert(x1, 0, -w1 * 0.6f, 0, 1, 0, t1, 1);
            b.vert(x0, 0, w0, 0, 1, 0, t0, 0); b.vert(x1, 0, -w1 * 0.6f, 0, 1, 0, t1, 1); b.vert(x0, 0, -w0 * 0.6f, 0, 1, 0, t0, 1);
        }
        waspWing = b.build(Img.FOE_TRAP, true, true);
    }

    void drawWasp(Gfx3D g, float[] m, Enemy3 e) {
        if (waspBody == null) buildWasp();
        System.arraycopy(m, 0, a, 0, 16);
        Mat4.scale(a, 1.5f, 1.5f, 1.5f);
        Mat4.translate(a, 0, 0.02f * (float) Math.sin(e.anim * 9f), 0);
        Mat4.rotateX(a, -0.2f);
        g.draw(waspBody, a, 1, 1, 1, 1, 0);
        g.draw(waspLegs, a, 1, 1, 1, 1, 0);
        float flap = 0.7f * (float) Math.sin(e.anim * 80f);              // battito velocissimo: si vede solo un velo
        for (int s = -1; s <= 1; s += 2) for (int k = 0; k < 2; k++) {
            System.arraycopy(a, 0, b2, 0, 16);
            Mat4.translate(b2, s * 0.05f, 0.07f, 0.04f - k * 0.05f);
            if (s < 0) Mat4.scale(b2, -1, 1, 1);
            Mat4.rotateY(b2, 0.35f + k * 0.25f);
            Mat4.rotateZ(b2, 0.2f + flap);
            g.draw(waspWing, b2, 1, 1, 1, 0.45f, 0.05f);
        }
    }

    // ================================================================== uccelli: rondine, gazza, falco
    /** 0 rondine, 1 gazza, 2 falco, 3 cornacchia nera. */
    private final Mesh[] birdBody = new Mesh[4], birdWing = new Mesh[4], birdTail = new Mesh[4], birdEyes = new Mesh[4];

    private void buildBird(final int kind) {
        final float[] back, back2, belly, throat, wingC, beak;
        final float len, span, wingW;
        if (kind == 0) { back = rgb(28, 38, 84); back2 = rgb(40, 52, 110); belly = rgb(236, 226, 206); throat = rgb(170, 44, 30); wingC = rgb(26, 30, 60); beak = rgb(20, 18, 18); len = 0.9f; span = 0.62f; wingW = 0.1f; }
        else if (kind == 1) { back = rgb(18, 20, 26); back2 = rgb(30, 40, 60); belly = rgb(240, 240, 238); throat = rgb(18, 20, 26); wingC = rgb(24, 30, 52); beak = rgb(20, 18, 18); len = 1f; span = 0.5f; wingW = 0.13f; }
        else if (kind == 3) { back = rgb(14, 14, 18); back2 = rgb(30, 34, 58); belly = rgb(22, 22, 28); throat = rgb(14, 14, 18); wingC = rgb(16, 16, 22); beak = rgb(26, 24, 24); len = 1.05f; span = 0.62f; wingW = 0.16f; }
        else { back = rgb(110, 76, 46); back2 = rgb(136, 96, 58); belly = rgb(226, 206, 170); throat = rgb(230, 214, 186); wingC = rgb(100, 70, 44); beak = rgb(220, 180, 60); len = 1f; span = 0.66f; wingW = 0.17f; }
        MeshBuilder b = new MeshBuilder();
        FoeModels.Paint plum = new FoeModels.Paint() {
            @Override void col(float t, float an, float x, float y, float z, float[] o) {
                FoeModels.mix(back, back2, 0.5f + 0.5f * (float) Math.sin(x * 40f + z * 30f), o);
                float under = FoeModels.sm(-(float) Math.sin(an) * 2.5f + 0.2f);
                float[] bl = belly;
                if (kind == 2) { float bar = FoeModels.sm((float) Math.sin(z * 80f) * 3f); bl = new float[] {belly[0] - 0.3f * bar, belly[1] - 0.32f * bar, belly[2] - 0.3f * bar}; }   // petto barrato del falco
                if (kind == 1 && z > 0.12f) under = 0;                       // gazza: testa e petto alto neri
                FoeModels.mix(o, bl, under, o);
                if (kind == 0 && z > 0.14f && z < 0.24f && (float) Math.sin(an) < 0.2f) FoeModels.mix(o, throat, 0.9f, o);   // gola rossa della rondine
                if (kind == 1 && Math.abs(x) > 0.06f && z > -0.05f && z < 0.1f && (float) Math.sin(an) > -0.2f && (float) Math.sin(an) < 0.6f) FoeModels.mix(o, belly, 0.9f, o);   // scapolari bianche
            }
        };
        float L = len * 0.36f;
        body(b, new float[][] {{0, 0.0f, -L}, {0, 0.01f, -L * 0.5f}, {0, 0.02f, 0}, {0, 0.03f, L * 0.45f}, {0, 0.05f, L * 0.7f}, {0, 0.06f, L * 0.85f}, {0, 0.05f, L}},
                new float[] {0.03f, 0.09f, 0.11f, 0.09f, 0.065f, 0.055f, 0.02f}, new float[] {0.03f, 0.08f, 0.1f, 0.085f, 0.065f, 0.055f, 0.02f}, 28, 18, plum, 3f, 3f);
        FoeModels.tube(b, new float[][] {{0, 0.05f, L * 0.98f}, {0, 0.04f, L * 1.08f}, {0, kind == 2 ? 0.0f : kind == 3 ? 0.025f : 0.035f, L * (kind == 2 ? 1.14f : kind == 3 ? 1.24f : 1.16f)}}, new float[] {kind == 3 ? 0.032f : 0.022f, kind == 3 ? 0.018f : 0.012f, 0.002f}, 8, FoeModels.along(beak, kind == 2 ? rgb(40, 36, 30) : beak));
        birdBody[kind] = fin(b, Img.FOE_OWL, 0.08f, 12f);
        b = new MeshBuilder();
        for (int s = -1; s <= 1; s += 2) {
            b.colorf(kind == 2 ? 0.85f : 0.05f, kind == 2 ? 0.65f : 0.04f, kind == 2 ? 0.1f : 0.04f); b.ellipsoid(s * 0.045f, 0.075f, L * 0.82f, 0.016f, 0.016f, 0.014f, 8, 5);
            b.colorf(0.02f, 0.02f, 0.02f); b.ellipsoid(s * 0.05f, 0.075f, L * 0.83f, 0.009f, 0.009f, 0.008f, 6, 4);
        }
        birdEyes[kind] = fin(b, -1, 0.9f, 80f);
        // ala: braccio coperto (copritrici) + remiganti separate; attaccatura all'origine, si estende verso +x
        b = new MeshBuilder();
        FoeModels.Paint wpaint = new FoeModels.Paint() {
            @Override void col(float t, float an, float x, float y, float z, float[] o) {
                FoeModels.mix(wingC, back2, 0.3f, o);
                if (kind == 1 && x > span * 0.55f) FoeModels.mix(o, belly, 0.6f, o);     // punta bianca delle primarie della gazza
                if (kind == 2) { float bar = FoeModels.sm((float) Math.sin(x * 60f) * 3f); o[0] *= 1 - 0.3f * bar; o[1] *= 1 - 0.3f * bar; o[2] *= 1 - 0.3f * bar; }
            }
        };
        FoeModels.loft(b, new float[][] {{0, 0, 0}, {span * 0.3f, 0.01f, -0.01f}, {span * 0.55f, 0, -0.04f}}, new float[] {0.03f, wingW, wingW * 0.8f}, new float[] {0.015f, 0.02f, 0.012f}, 0, 1, 0, 10, 1f, wpaint, 2f, 2f);
        int nf = kind == 2 ? 9 : 7;
        for (int k = 0; k < nf; k++) {
            float u = k / (nf - 1f);
            float x0 = span * (0.25f + 0.32f * u), z0 = -0.04f - 0.02f * u;
            float ang = -0.2f - (kind == 0 ? 0.35f : 0.75f) * u, fl = span * (kind == 0 ? 0.5f : 0.38f) * (0.7f + 0.3f * (float) Math.sin(Math.PI * u));
            float x1 = x0 + (float) Math.cos(ang) * fl, z1 = z0 + (float) Math.sin(ang) * fl;
            FoeModels.loft(b, new float[][] {{x0, -0.003f * k, z0}, {(x0 + x1) / 2, -0.004f * k, (z0 + z1) / 2}, {x1, -0.005f * k, z1}}, new float[] {0.03f, 0.032f, 0.008f}, new float[] {0.004f, 0.004f, 0.002f}, 0, 1, 0, 5, 1f, wpaint, 1f, 1.5f);
        }
        birdWing[kind] = fin(b, Img.FOE_OWLWING, 0.06f, 10f);
        // coda: forcuta lunghissima (rondine), lunga a cuneo (gazza), larga e barrata (falco)
        b = new MeshBuilder();
        int nt = 7;
        for (int k = 0; k < nt; k++) {
            float u = k / (nt - 1f) * 2 - 1;
            float tl = kind == 0 ? 0.22f + 0.32f * u * u * u * u : kind == 1 ? 0.5f - 0.18f * Math.abs(u) : kind == 3 ? 0.34f - 0.06f * u * u : 0.3f;
            float spread = kind == 2 ? 0.07f : 0.04f;
            FoeModels.loft(b, new float[][] {{u * 0.02f, 0, 0}, {u * spread, -0.005f, -tl * 0.5f}, {u * spread * 1.3f, -0.01f, -tl}}, new float[] {0.02f, kind == 0 ? 0.018f : 0.03f, 0.012f}, new float[] {0.004f, 0.004f, 0.003f}, 0, 1, 0, 5, 1f, wpaint, 1f, 2f);
        }
        birdTail[kind] = fin(b, Img.FOE_OWLWING, 0.08f, 12f);
    }

    void drawBird(Gfx3D g, float[] m, Enemy3 e, int kind) {
        if (birdBody[kind] == null) buildBird(kind);
        float ph = e.anim * (kind == 2 ? 6f : 10f), flap = (float) Math.sin(ph);
        float L = (kind == 0 ? 0.9f : 1f) * 0.36f;
        System.arraycopy(m, 0, a, 0, 16);
        Mat4.translate(a, 0, 0.2f - 0.03f * flap, 0);
        Mat4.rotateX(a, -0.06f + 0.04f * flap);
        g.draw(birdBody[kind], a, 1, 1, 1, 1, 0);
        g.draw(birdEyes[kind], a, 1, 1, 1, 1, 0.05f);
        System.arraycopy(a, 0, b2, 0, 16);
        Mat4.translate(b2, 0, 0.01f, -L * 0.9f);
        Mat4.rotateX(b2, 0.1f * (float) Math.sin(ph * 0.5f));
        g.draw(birdTail[kind], b2, 1, 1, 1, 1, 0);
        for (int s = -1; s <= 1; s += 2) {
            System.arraycopy(a, 0, b2, 0, 16);
            Mat4.translate(b2, s * 0.08f, 0.06f, L * 0.2f);
            if (s < 0) Mat4.scale(b2, -1, 1, 1);
            Mat4.rotateZ(b2, (kind == 2 ? 0.25f : 0.1f) + (kind == 2 ? 0.45f : 0.8f) * flap);   // il falco plana, la rondine frulla
            g.draw(birdWing[kind], b2, 1, 1, 1, 1, 0);
        }
    }
}
