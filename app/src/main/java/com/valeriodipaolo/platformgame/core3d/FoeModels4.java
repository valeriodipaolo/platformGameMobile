package com.valeriodipaolo.platformgame.core3d;

import com.valeriodipaolo.platformgame.core.Img;

/**
 * Gli ultimi nemici rifatti realistici: scoiattolo (autunno, salta come la ghianda), brace di legno (incendio), bomba di lava,
 * salamandra pezzata, nuvola temporalesca, tromba d'aria, meteora (al posto della stella cadente) e fulmine globulare (al posto
 * del solleone). In più il cespuglio di rovo in fiore della primavera. Ogni modello guarda verso +z con i piedi a y = 0.
 * Si costruiscono solo quando servono la prima volta.
 */
final class FoeModels4 {
    private static final float PI = (float) Math.PI;
    private final float[] a = new float[16], b2 = new float[16];

    private static float[] rgb(int r, int g, int b) { return new float[] {r / 255f, g / 255f, b / 255f}; }

    private static Mesh fin(MeshBuilder b, int tex, float spec, float shine) {
        Mesh m = b.build(tex, false, true);
        m.spec = spec; m.shine = shine;
        return m;
    }

    private static float nz(float x, float y, float z, int seed) { return MeshBuilder.noise3(x, y, z, seed); }

    /** Pietra o tizzone: crosta scura con crepe incandescenti dove il rumore passa per una sottile fascia. */
    private static FoeModels.Paint crust(final float[] dark, final float[] dark2, final float[] hot, final float[] core, final float freq, final float crack, final int seed) {
        return new FoeModels.Paint() {
            @Override void col(float t, float an, float x, float y, float z, float[] o) {
                float n = nz(x * freq, y * freq, z * freq, seed);
                FoeModels.mix(dark, dark2, nz(x * freq * 3f, y * freq * 3f, z * freq * 3f, seed + 7), o);
                float c = 1f - Math.abs(n - 0.5f) / crack;                     // vicino alla linea di livello 0,5: crepa
                if (c > 0) {
                    FoeModels.mix(o, hot, FoeModels.sm(c * 1.6f), o);
                    FoeModels.mix(o, core, FoeModels.sm((c - 0.7f) * 3.3f), o);
                }
            }
        };
    }

    // ================================================================== scoiattolo
    private Mesh sqBody, sqTail, sqHind, sqFore, sqEyes;

    private void buildSquirrel() {
        final float[] rust = rgb(156, 74, 30), rust2 = rgb(186, 98, 44), cream = rgb(236, 222, 196), dark = rgb(92, 44, 20);
        FoeModels.Paint fur = new FoeModels.Paint() {
            @Override void col(float t, float an, float x, float y, float z, float[] o) {
                FoeModels.mix(rust, rust2, nz(x * 30f, y * 30f, z * 30f, 3), o);
                FoeModels.mix(o, cream, FoeModels.sm(-(float) Math.sin(an) * 2.2f - 0.3f), o);   // ventre e gola crema
            }
        };
        MeshBuilder b = new MeshBuilder();
        // corpo raccolto, schiena arcuata, petto alto
        FoeModels3.body(b, new float[][] {{0, 0.17f, -0.2f}, {0, 0.21f, -0.12f}, {0, 0.24f, 0.0f}, {0, 0.27f, 0.1f}, {0, 0.3f, 0.17f}},
                new float[] {0.03f, 0.1f, 0.105f, 0.085f, 0.06f}, new float[] {0.03f, 0.1f, 0.11f, 0.09f, 0.06f}, 18, 16, fur, 2f, 2f);
        // testa con muso corto
        FoeModels.ballZ(b, 0, 0.35f, 0.22f, 0.07f, 0.068f, 0.085f, 14, 10, fur, 2f, 2f);
        FoeModels.ballZ(b, 0, 0.33f, 0.29f, 0.04f, 0.038f, 0.045f, 10, 7, fur, 1f, 1f);
        b.colorf(0.16f, 0.1f, 0.08f); b.ellipsoid(0, 0.338f, 0.334f, 0.012f, 0.009f, 0.007f, 6, 4);    // naso
        // orecchie a punta con il ciuffo scuro
        for (int s = -1; s <= 1; s += 2) {
            FoeModels.quill(b, s * 0.035f, 0.395f, 0.2f, s * 0.25f, 1f, -0.2f, 0.07f, 0.022f, rust, rust2, dark);
            FoeModels.quill(b, s * 0.04f, 0.43f, 0.195f, s * 0.2f, 1f, -0.35f, 0.07f, 0.012f, dark, dark, rgb(60, 30, 14));
        }
        sqBody = fin(b, Img.FOE_PENGFUR, 0.08f, 10f);
        b = new MeshBuilder();
        for (int s = -1; s <= 1; s += 2) {
            b.colorf(0.04f, 0.03f, 0.03f); b.ellipsoid(s * 0.052f, 0.365f, 0.255f, 0.018f, 0.02f, 0.016f, 8, 6);
            b.colorf(0.9f, 0.9f, 0.9f); b.ellipsoid(s * 0.06f, 0.374f, 0.265f, 0.004f, 0.004f, 0.003f, 4, 3);
            b.colorf(0.86f, 0.8f, 0.7f); b.ellipsoid(s * 0.046f, 0.36f, 0.25f, 0.022f, 0.005f, 0.018f, 6, 3);   // anello chiaro attorno all'occhio
        }
        sqEyes = fin(b, -1, 0.9f, 80f);
        // coda folta che sale a S dietro la schiena
        b = new MeshBuilder();
        final float[] tail = rgb(140, 64, 26), tail2 = rgb(178, 92, 40);
        FoeModels.Paint tp = new FoeModels.Paint() {
            @Override void col(float t, float an, float x, float y, float z, float[] o) {
                FoeModels.mix(tail, tail2, nz(x * 40f, y * 40f, z * 40f, 9), o);
                float k = 0.85f + 0.15f * (float) Math.sin(an * 7f + t * 30f);           // ciocche
                o[0] *= k; o[1] *= k; o[2] *= k;
            }
        };
        int m0 = b.mark();
        float[][] tc = {{0, 0.2f, -0.17f}, {0, 0.25f, -0.3f}, {0, 0.36f, -0.37f}, {0, 0.49f, -0.35f}, {0, 0.58f, -0.26f}, {0, 0.61f, -0.15f}, {0, 0.58f, -0.06f}};
        float[] tr = {0.04f, 0.09f, 0.13f, 0.145f, 0.13f, 0.1f, 0.03f};
        FoeModels3.body(b, tc, tr, new float[] {0.04f, 0.08f, 0.11f, 0.12f, 0.11f, 0.085f, 0.03f}, 22, 14, tp, 2f, 2f);
        // ciocche di pelo che escono dal profilo della coda
        java.util.Random rr = new java.util.Random(4);
        for (int i = 0; i < 70; i++) {
            float f = 0.12f + 0.8f * rr.nextFloat() * (tc.length - 1);
            int k = Math.min(tc.length - 2, (int) f);
            float u = f - k;
            float px = 0, py = tc[k][1] + (tc[k + 1][1] - tc[k][1]) * u, pz = tc[k][2] + (tc[k + 1][2] - tc[k][2]) * u;
            float rad = (tr[k] + (tr[k + 1] - tr[k]) * u) * 0.75f;
            float an = rr.nextFloat() * 2 * PI;
            float ox = (float) Math.cos(an), oy = (float) Math.sin(an) * 0.8f, oz = (float) Math.sin(an) * 0.6f;
            FoeModels.quill(b, px + ox * rad, py + oy * rad, pz + oz * rad * 0.5f, ox, oy + 0.2f, oz - 0.5f, 0.09f, 0.03f, tail, tail2, rgb(200, 130, 70));
        }
        b.rough(m0, 0.03f, 14f, 5, false);
        sqTail = fin(b, Img.FOE_PENGFUR, 0.05f, 8f);
        // zampa posteriore: coscia grossa, piede lungo (anca all'origine)
        b = new MeshBuilder();
        FoeModels.ballZ(b, 0, 0, 0, 0.05f, 0.075f, 0.085f, 10, 8, fur, 1f, 1f);
        FoeModels.tube(b, new float[][] {{0, -0.04f, -0.04f}, {0, -0.12f, -0.02f}, {0, -0.19f, 0.03f}}, new float[] {0.03f, 0.02f, 0.016f}, 7, FoeModels.along(rust, dark));
        FoeModels.ballZ(b, 0, -0.195f, 0.07f, 0.02f, 0.012f, 0.06f, 8, 5, FoeModels.flat(dark), 1f, 1f);
        sqHind = fin(b, Img.FOE_PENGFUR, 0.05f, 8f);
        b = new MeshBuilder();
        FoeModels.tube(b, new float[][] {{0, 0, 0}, {0, -0.07f, 0.02f}, {0, -0.13f, 0.03f}}, new float[] {0.032f, 0.022f, 0.016f}, 6, FoeModels.along(rust2, rust));
        FoeModels.ballZ(b, 0, -0.135f, 0.045f, 0.014f, 0.01f, 0.026f, 6, 4, FoeModels.flat(dark), 1f, 1f);
        sqFore = fin(b, Img.FOE_PENGFUR, 0.05f, 8f);
    }

    void drawSquirrel(Gfx3D g, float[] m, Enemy3 e) {
        if (sqBody == null) buildSquirrel();
        float air = Math.max(0f, Math.min(1f, (e.y - e.baseY) / 0.6f));
        float crouch = air > 0.02f ? 0 : 0.5f + 0.5f * (float) Math.sin(e.anim * 9f);
        System.arraycopy(m, 0, a, 0, 16);
        Mat4.scale(a, 1.5f, 1.5f, 1.5f);
        Mat4.translate(a, 0, 0.02f * air - 0.015f * crouch, 0);
        Mat4.rotateX(a, -0.3f * air + 0.06f * crouch);                     // in salto il muso punta in avanti, la coda si stende
        g.draw(sqBody, a, 1, 1, 1, 1, 0);
        g.draw(sqEyes, a, 1, 1, 1, 1, 0.05f);
        System.arraycopy(a, 0, b2, 0, 16);
        Mat4.translate(b2, 0, 0.19f, -0.18f);
        Mat4.rotateX(b2, 0.5f * air - 0.1f * (float) Math.sin(e.anim * 4f));
        Mat4.translate(b2, 0, -0.19f, 0.18f);
        g.draw(sqTail, b2, 1, 1, 1, 1, 0);
        for (int s = -1; s <= 1; s += 2) {
            System.arraycopy(a, 0, b2, 0, 16);
            Mat4.translate(b2, s * 0.075f, 0.19f, -0.1f);
            Mat4.rotateX(b2, 1.2f * air);                                   // le zampe dietro spingono e si allungano indietro
            g.draw(sqHind, b2, 1, 1, 1, 1, 0);
            System.arraycopy(a, 0, b2, 0, 16);
            Mat4.translate(b2, s * 0.045f, 0.14f, 0.13f);
            Mat4.rotateX(b2, -0.9f * air + 0.2f * crouch);                  // quelle davanti si tendono verso l'atterraggio
            g.draw(sqFore, b2, 1, 1, 1, 1, 0);
        }
    }

    // ================================================================== brace e bomba di lava
    private Mesh ember, lavaBomb;

    void drawEmber(Gfx3D g, float[] m, Enemy3 e) {
        if (ember == null) {
            MeshBuilder b = new MeshBuilder();
            int m0 = b.mark();
            // tizzone: un pezzo di ramo spezzato, carbonizzato a scaglie, con le crepe che ardono
            FoeModels3.body(b, new float[][] {{0, 0.2f, -0.3f}, {0, 0.21f, -0.15f}, {0, 0.2f, 0.05f}, {0, 0.19f, 0.25f}, {0, 0.19f, 0.3f}},
                    new float[] {0.02f, 0.17f, 0.19f, 0.16f, 0.02f}, new float[] {0.02f, 0.15f, 0.17f, 0.15f, 0.02f}, 26, 30,
                    crust(rgb(16, 14, 13), rgb(40, 34, 30), rgb(255, 80, 10), rgb(255, 200, 90), 13f, 0.035f, 21), 1f, 1f);
            b.rough(m0, 0.035f, 7f, 4);
            ember = fin(b, -1, 0.15f, 12f);
        }
        float air = Math.max(0f, e.y - e.baseY);
        System.arraycopy(m, 0, a, 0, 16);
        if (air > 0.05f) { Mat4.translate(a, 0, 0.2f, 0); Mat4.rotateX(a, e.anim * 7f); Mat4.translate(a, 0, -0.2f, 0); }   // in volo rotola
        float glow = 0.85f + 0.15f * (float) Math.sin(e.anim * 9f) + 0.06f * (float) Math.sin(e.anim * 23f);
        g.draw(ember, a, glow, glow, glow, 1, 0.6f);
    }

    void drawLavaBomb(Gfx3D g, float[] m, Enemy3 e) {
        if (lavaBomb == null) {
            MeshBuilder b = new MeshBuilder();
            int m0 = b.mark();
            // bomba vulcanica: roccia fusa a buccia di pane, crosta che si spacca mostrando il magma
            FoeModels.ballY(b, 0, 0, 0, 0.34f, 0.32f, 0.36f, 34, 22,
                    crust(rgb(26, 20, 18), rgb(56, 42, 36), rgb(255, 76, 10), rgb(255, 210, 110), 7.5f, 0.04f, 33), 1f, 1f);
            b.rough(m0, 0.06f, 5f, 8);
            lavaBomb = fin(b, -1, 0.2f, 14f);
        }
        if (e.mode == 0) return;
        System.arraycopy(m, 0, a, 0, 16);
        Mat4.translate(a, 0, 0.36f, 0);
        Mat4.rotateX(a, e.anim * 3f);
        Mat4.rotateZ(a, e.anim * 1.7f);
        float f = 0.9f + 0.1f * (float) Math.sin(e.anim * 30f);
        g.draw(lavaBomb, a, f, f, f, 1, 0.6f);
    }

    // ================================================================== salamandra pezzata
    private Mesh salBody, salLeg, salEyes;

    private void buildSalamander() {
        final float[] black = rgb(18, 16, 18), black2 = rgb(40, 36, 38), yellow = rgb(250, 196, 24), orange = rgb(236, 120, 20);
        FoeModels.Paint skin = new FoeModels.Paint() {
            @Override void col(float t, float an, float x, float y, float z, float[] o) {
                FoeModels.mix(black, black2, FoeModels.sm((float) Math.sin(an) * 1.5f), o);
                float n = nz(x * 7f, y * 4f, z * 7f, 11);                              // macchie irregolari, due file lungo la schiena
                float sp = FoeModels.sm((n - 0.58f) * 14f) * FoeModels.sm((float) Math.sin(an) * 3f + 0.5f);
                FoeModels.mix(o, n > 0.7f ? orange : yellow, sp, o);
            }
        };
        MeshBuilder b = new MeshBuilder();
        // corpo lungo e basso, testa larga e piatta, coda che si assottiglia e ondeggia di lato
        FoeModels3.body(b, new float[][] {{0.0f, 0.1f, -1.1f}, {0.06f, 0.11f, -0.8f}, {0.0f, 0.13f, -0.45f}, {0, 0.17f, -0.15f}, {0, 0.19f, 0.15f}, {0, 0.19f, 0.4f}, {0, 0.18f, 0.52f}, {0, 0.17f, 0.66f}, {0, 0.15f, 0.76f}},
                new float[] {0.01f, 0.045f, 0.09f, 0.15f, 0.16f, 0.13f, 0.12f, 0.15f, 0.05f}, new float[] {0.01f, 0.05f, 0.08f, 0.11f, 0.115f, 0.09f, 0.075f, 0.075f, 0.03f}, 34, 18, skin, 2f, 2f);
        // ghiandole parotoidi gialle dietro gli occhi
        for (int s = -1; s <= 1; s += 2) { b.colorf(0.96f, 0.74f, 0.1f); b.ellipsoid(s * 0.1f, 0.24f, 0.52f, 0.045f, 0.03f, 0.07f, 8, 5); }
        b.colorf(0.25f, 0.2f, 0.18f); b.ellipsoid(0, 0.12f, 0.73f, 0.07f, 0.005f, 0.012f, 8, 3);      // bocca
        salBody = fin(b, Img.FOE_FROG, 0.6f, 50f);
        b = new MeshBuilder();
        for (int s = -1; s <= 1; s += 2) {
            b.colorf(0.03f, 0.03f, 0.03f); b.ellipsoid(s * 0.11f, 0.24f, 0.66f, 0.04f, 0.04f, 0.04f, 10, 7);
            b.colorf(0.8f, 0.8f, 0.8f); b.ellipsoid(s * 0.125f, 0.26f, 0.685f, 0.007f, 0.007f, 0.005f, 4, 3);
        }
        salEyes = fin(b, -1, 1f, 90f);
        // zampa divaricata: braccio verso il fuori, avambraccio giù, quattro dita (anca all'origine, lato destro)
        b = new MeshBuilder();
        FoeModels.tube(b, new float[][] {{0, 0, 0}, {0.1f, 0.0f, 0.02f}, {0.17f, -0.05f, 0.04f}, {0.2f, -0.15f, 0.06f}}, new float[] {0.045f, 0.035f, 0.03f, 0.025f}, 8, FoeModels.along(black2, black));
        b.colorf(0.95f, 0.72f, 0.12f); b.ellipsoid(0.1f, 0.025f, 0.02f, 0.03f, 0.015f, 0.025f, 6, 4);
        for (int k = 0; k < 4; k++) {
            float an = -0.6f + k * 0.4f;
            FoeModels.tube(b, 0.2f, -0.15f, 0.06f, 0.2f + 0.07f * (float) Math.sin(an), -0.165f, 0.06f + 0.07f * (float) Math.cos(an), 0.012f, 0.008f, 5, FoeModels.flat(black2));
        }
        salLeg = fin(b, Img.FOE_FROG, 0.5f, 40f);
    }

    /** mode 0 cammina, 1 si gonfia (la gola arde), 2 sputa fuoco (la fiamma la disegna chi chiama). */
    void drawSalamander(Gfx3D g, float[] m, Enemy3 e) {
        if (salBody == null) buildSalamander();
        float an = e.anim, walk = e.mode == 0 ? an * 8f : 0f;
        System.arraycopy(m, 0, a, 0, 16);
        float sw = e.mode == 1 ? 1f + 0.08f * (float) Math.sin(an * 30f) : 1f;
        Mat4.scale(a, sw, sw, 1f);
        Mat4.rotateY(a, 0.12f * (float) Math.sin(walk));                       // il corpo serpeggia mentre cammina
        float hot = e.mode == 1 ? 0.25f : e.mode == 2 ? 0.15f : 0f;
        g.draw(salBody, a, 1f + hot, 1f, 1f - hot * 0.5f, 1, hot);
        g.draw(salEyes, a, 1, 1, 1, 1, 0.05f);
        float[][] hips = {{0.12f, 0.15f, 0.42f}, {0.12f, 0.15f, -0.2f}};
        for (int i = 0; i < 4; i++) {
            int s = i % 2 == 0 ? 1 : -1, k = i / 2;
            float ph = walk + (s > 0 ? 0 : PI) + k * PI;
            System.arraycopy(a, 0, b2, 0, 16);
            Mat4.translate(b2, s * hips[k][0], hips[k][1], hips[k][2]);
            if (s < 0) Mat4.scale(b2, -1, 1, 1);
            Mat4.rotateY(b2, (k == 0 ? -0.3f : 0.4f) + 0.45f * (float) Math.sin(ph));
            Mat4.rotateZ(b2, 0.15f * Math.max(0f, (float) Math.cos(ph)));
            g.draw(salLeg, b2, 1, 1, 1, 1, 0);
        }
    }

    // ================================================================== nuvola temporalesca
    private Mesh cloud, cloudDark;

    private void buildCloud() {
        final float[] base = rgb(34, 38, 50), mid = rgb(78, 84, 100), top = rgb(150, 154, 168);
        FoeModels.Paint shade = new FoeModels.Paint() {
            @Override void col(float t, float an, float x, float y, float z, float[] o) {
                float h = FoeModels.sm((y - 0.05f) / 0.75f);
                if (h < 0.5f) FoeModels.mix(base, mid, h * 2f, o); else FoeModels.mix(mid, top, (h - 0.5f) * 2f, o);
                float n = 0.88f + 0.24f * nz(x * 6f, y * 6f, z * 6f, 2);
                o[0] *= n; o[1] *= n; o[2] *= n;
            }
        };
        MeshBuilder b = new MeshBuilder();
        int m0 = b.mark();
        // base piatta e scura, poi cumuli che si accavallano e salgono a torre
        FoeModels.ballY(b, 0, 0.16f, 0, 0.78f, 0.16f, 0.6f, 20, 8, shade, 1f, 1f);
        float[][] puffs = {{-0.45f, 0.3f, 0.1f, 0.34f}, {0.42f, 0.3f, 0.05f, 0.36f}, {0.05f, 0.34f, 0.25f, 0.34f}, {-0.1f, 0.34f, -0.25f, 0.36f},
                {0.0f, 0.55f, 0.0f, 0.4f}, {-0.3f, 0.6f, -0.05f, 0.28f}, {0.3f, 0.62f, 0.08f, 0.3f}, {0.08f, 0.85f, -0.02f, 0.28f}, {-0.12f, 0.95f, 0.05f, 0.2f}};
        for (float[] p : puffs) FoeModels.ballY(b, p[0], p[1], p[2], p[3], p[3] * 0.9f, p[3], 14, 9, shade, 1f, 1f);
        b.rough(m0, 0.07f, 4.5f, 12, false);
        cloud = fin(b, -1, 0.05f, 6f);
        // velo di pioggia sotto la base: strisce verticali trasparenti
        b = new MeshBuilder();
        b.colorf(0.55f, 0.6f, 0.7f);
        for (int i = 0; i < 6; i++) {
            float an = i * PI / 6f, cx = (float) Math.cos(an) * 0.5f, cz = (float) Math.sin(an) * 0.5f;
            b.quad(-cx, -0.9f, -cz, cx, -0.9f, cz, cx, 0.05f, cz, -cx, 0.05f, -cz, cz, 0, -cx, 0, 0, 1, 1);
        }
        cloudDark = b.build(Img.HV_BOLT, true, true);
    }

    /** ph 0 vaga, 1 si carica (lampi dentro la nuvola), 2 scarica (il fulmine lo disegna chi chiama). */
    void drawStorm(Gfx3D g, float[] m, Enemy3 e, int ph) {
        if (cloud == null) buildCloud();
        System.arraycopy(m, 0, a, 0, 16);
        Mat4.translate(a, 0, -0.3f, 0);
        Mat4.rotateY(a, 0.15f * (float) Math.sin(e.anim * 0.4f));
        float flash = 0f;
        if (ph == 1) flash = Math.max(0f, (float) Math.sin(e.anim * 37f) * (float) Math.sin(e.anim * 11f)) * 0.6f;
        if (ph == 2) flash = 0.5f;
        g.draw(cloud, a, 1f + flash * 0.4f, 1f + flash * 0.45f, 1f + flash * 0.6f, 1, flash * 0.5f);
    }

    // ================================================================== tromba d'aria
    private Mesh funnel, dustBase, debris;

    private void buildTornado() {
        final float[] dust = rgb(150, 138, 120), light = rgb(236, 232, 224), dark = rgb(70, 64, 56);
        MeshBuilder b = new MeshBuilder();
        int rings = 16, segs = 18;
        for (int i = 0; i < rings; i++) {
            float t0 = i / (float) rings, t1 = (i + 1f) / rings;
            for (int j = 0; j < segs; j++) {
                float a0 = 2 * PI * j / segs, a1 = 2 * PI * (j + 1) / segs;
                float[][] q = new float[4][];
                float[] ts = {t0, t0, t1, t1}, as = {a0, a1, a1, a0};
                for (int k = 0; k < 4; k++) {
                    float t = ts[k], an = as[k];
                    float r = 0.07f + 0.6f * (float) Math.pow(t, 1.7f);
                    float twist = an + t * 5f;                                   // le strisce di polvere si avvitano salendo
                    float wob = 1f + 0.18f * (nz((float) Math.cos(an) * 2f, t * 6f, (float) Math.sin(an) * 2f, 4) - 0.5f);
                    float sx = 0.06f * (float) Math.sin(t * 7f);                // il fusto ondeggia
                    q[k] = new float[] {sx + (float) Math.cos(an) * r * wob, t * 1.45f, (float) Math.sin(an) * r * wob, twist, t};
                }
                int[] ord = {0, 1, 2, 0, 2, 3};
                for (int k : ord) {
                    float tw = q[k][3], t = q[k][4];
                    float st = 0.5f + 0.5f * (float) Math.sin(tw * 3f) * (float) Math.sin(tw * 1.3f + 1f);
                    float[] c = new float[3];
                    FoeModels.mix(dark, light, st, c);
                    FoeModels.mix(c, dust, 0.15f + 0.35f * (1f - t), c);
                    b.colorf(c[0], c[1], c[2]);
                    float an = (float) Math.atan2(q[k][2], q[k][0]);
                    b.vert(q[k][0], q[k][1], q[k][2], (float) Math.cos(an), 0.3f, (float) Math.sin(an), 0, 0);
                }
            }
        }
        funnel = b.build(-1, true, true);
        b = new MeshBuilder();
        int m0 = b.mark();
        FoeModels.ballY(b, 0, 0.1f, 0, 0.55f, 0.16f, 0.55f, 16, 7, FoeModels.ud(dust, light), 1f, 1f);
        b.rough(m0, 0.08f, 5f, 6, false);
        dustBase = b.build(-1, true, true);
        // detriti che girano: foglie secche, rametti, zolle
        b = new MeshBuilder();
        java.util.Random r = new java.util.Random(7);
        for (int i = 0; i < 16; i++) {
            float an = r.nextFloat() * 2 * PI, t = r.nextFloat();
            float rad = 0.12f + 0.62f * (float) Math.pow(t, 1.7f) + 0.06f, y = t * 1.4f;
            float x = (float) Math.cos(an) * rad, z = (float) Math.sin(an) * rad;
            if (i % 3 == 0) { b.colorf(0.35f, 0.25f, 0.15f); b.box(x, y, z, 0.12f, 0.015f, 0.015f); }
            else { b.colorf(0.55f + 0.2f * r.nextFloat(), 0.4f, 0.16f); int mm = b.mark(); b.ellipsoid(x, y, z, 0.04f, 0.008f, 0.03f, 6, 3); b.rough(mm, 0.01f, 30f, i); }
        }
        debris = fin(b, -1, 0.1f, 8f);
    }

    void drawWhirl(Gfx3D g, float[] m, Enemy3 e) {
        if (funnel == null) buildTornado();
        System.arraycopy(m, 0, a, 0, 16);
        Mat4.rotateY(a, e.anim * 6f);
        g.draw(funnel, a, 1, 1, 1, 0.55f, 0.1f);
        System.arraycopy(m, 0, a, 0, 16);
        Mat4.rotateY(a, -e.anim * 3f + 1f);
        Mat4.scale(a, 1.12f, 0.98f, 1.12f);
        g.draw(funnel, a, 1, 1, 1, 0.3f, 0.1f);
        System.arraycopy(m, 0, a, 0, 16);
        Mat4.rotateY(a, e.anim * 2f);
        Mat4.scale(a, 1f + 0.08f * (float) Math.sin(e.anim * 5f), 1f, 1f + 0.08f * (float) Math.sin(e.anim * 5f + 1f));
        g.draw(dustBase, a, 1, 1, 1, 0.45f, 0.05f);
        System.arraycopy(m, 0, a, 0, 16);
        Mat4.rotateY(a, e.anim * 8f);
        g.draw(debris, a, 1, 1, 1, 1, 0);
    }

    // ================================================================== meteora
    private Mesh meteor, trail;

    private void buildMeteor() {
        MeshBuilder b = new MeshBuilder();
        int m0 = b.mark();
        final float[] rock = rgb(48, 40, 36), rock2 = rgb(84, 70, 60), hot = rgb(255, 120, 30), white = rgb(255, 236, 190);
        // il fronte (verso +z) è fuso e bianco, il dietro è roccia che si raffredda
        FoeModels.ballZ(b, 0, 0, 0, 0.2f, 0.18f, 0.22f, 16, 12, new FoeModels.Paint() {
            @Override void col(float t, float an, float x, float y, float z, float[] o) {
                FoeModels.mix(rock, rock2, nz(x * 14f, y * 14f, z * 14f, 5), o);
                FoeModels.mix(o, hot, FoeModels.sm(z / 0.2f + 0.6f), o);
                FoeModels.mix(o, white, FoeModels.sm(z / 0.15f - 0.2f), o);
            }
        }, 1f, 1f);
        b.rough(m0, 0.04f, 9f, 3);
        meteor = fin(b, -1, 0.1f, 10f);
        // scia di fuoco: cono lungo dietro, dal giallo-bianco al rosso
        b = new MeshBuilder();
        final float[] c0 = rgb(255, 244, 200), c1 = rgb(255, 150, 40), c2 = rgb(200, 50, 20);
        FoeModels.loft(b, new float[][] {{0, 0, -0.05f}, {0, 0, -0.2f}, {0, 0, -0.55f}, {0, 0, -1f}, {0, 0, -1.6f}},
                new float[] {0.12f, 0.2f, 0.17f, 0.1f, 0.01f}, new float[] {0.11f, 0.18f, 0.15f, 0.09f, 0.01f}, 0, 1, 0, 12, 1f, new FoeModels.Paint() {
                    @Override void col(float t, float an, float x, float y, float z, float[] o) {
                        if (t < 0.4f) FoeModels.mix(c0, c1, t / 0.4f, o); else FoeModels.mix(c1, c2, (t - 0.4f) / 0.6f, o);
                    }
                }, 0, 0);
        trail = b.build(-1, true, true);
    }

    void drawMeteor(Gfx3D g, float[] m, Enemy3 e) {
        if (meteor == null) buildMeteor();
        float vx = e.lo != 0 ? e.lo : 3.2f, vy = e.hi != 0 ? e.hi : -4.6f;
        System.arraycopy(m, 0, a, 0, 16);
        Mat4.rotateX(a, -(float) Math.atan2(vy, vx));                        // il muso nella direzione del volo
        g.draw(meteor, a, 1, 1, 1, 1, 0.6f);
        float f = 1f + 0.12f * (float) Math.sin(e.anim * 40f);
        System.arraycopy(a, 0, b2, 0, 16);
        Mat4.scale(b2, f, f, 1f + 0.15f * (float) Math.sin(e.anim * 27f));
        g.draw(trail, b2, 1, 1, 1, 0.8f, 1f);
        Mat4.scale(b2, 1.7f, 1.7f, 1.25f);
        g.draw(trail, b2, 1, 0.8f, 0.6f, 0.25f, 1f);
    }

    // ================================================================== fulmine globulare
    private Mesh orbCore, orbHalo;
    private final Mesh[] arcs = new Mesh[3];

    private void buildOrb() {
        MeshBuilder b = new MeshBuilder();
        b.colorf(0.95f, 0.97f, 1f); b.ellipsoid(0, 0, 0, 0.13f, 0.13f, 0.13f, 12, 8);
        orbCore = fin(b, -1, 0f, 1f);
        b = new MeshBuilder();
        b.colorf(0.55f, 0.7f, 1f); b.ellipsoid(0, 0, 0, 0.3f, 0.3f, 0.3f, 16, 10);
        orbHalo = b.build(-1, true, true);
        // scariche: filamenti a zig-zag che partono dal centro, tre varianti che si alternano
        java.util.Random r = new java.util.Random(19);
        for (int v = 0; v < 3; v++) {
            b = new MeshBuilder();
            for (int k = 0; k < 6; k++) {
                float th = r.nextFloat() * 2 * PI, ph = (float) Math.acos(2 * r.nextFloat() - 1);
                float dx = (float) (Math.sin(ph) * Math.cos(th)), dy = (float) Math.cos(ph), dz = (float) (Math.sin(ph) * Math.sin(th));
                int n = 6;
                float[][] c = new float[n][];
                float[] rr = new float[n];
                float len = 0.32f + 0.2f * r.nextFloat();
                for (int i = 0; i < n; i++) {
                    float t = i / (n - 1f), j = i == 0 ? 0 : 0.06f;
                    c[i] = new float[] {dx * len * t + (r.nextFloat() - 0.5f) * j, dy * len * t + (r.nextFloat() - 0.5f) * j, dz * len * t + (r.nextFloat() - 0.5f) * j};
                    rr[i] = 0.012f * (1f - t * 0.7f);
                }
                FoeModels.tube(b, c, rr, 4, FoeModels.along(rgb(240, 246, 255), rgb(140, 170, 255)));
            }
            arcs[v] = fin(b, -1, 0f, 1f);
        }
    }

    void drawOrb(Gfx3D g, float[] m, Enemy3 e) {
        if (orbCore == null) buildOrb();
        System.arraycopy(m, 0, a, 0, 16);
        Mat4.translate(a, 0, 0.3f, 0);
        float p = 1f + 0.1f * (float) Math.sin(e.anim * 33f) + 0.05f * (float) Math.sin(e.anim * 71f);
        System.arraycopy(a, 0, b2, 0, 16);
        Mat4.scale(b2, p, p, p);
        g.draw(orbCore, b2, 1, 1, 1, 1, 1f);
        g.draw(orbHalo, b2, 1, 1, 1, 0.35f, 1f);
        Mat4.scale(b2, 1.5f, 1.5f, 1.5f);
        g.draw(orbHalo, b2, 0.7f, 0.8f, 1f, 0.12f, 1f);
        int v = ((int) (e.anim * 14f)) % 3;
        System.arraycopy(a, 0, b2, 0, 16);
        Mat4.rotateY(b2, e.anim * 5f);
        Mat4.rotateX(b2, e.anim * 3f);
        g.draw(arcs[v], b2, 1, 1, 1, 1, 1f);
    }

    // ================================================================== rovo in fiore (primavera)

    /** Fogliolina ovale seghettata lungo dir, a partire da (px,py,pz), con la piega della nervatura. */
    private static void leaflet(MeshBuilder b, float px, float py, float pz, float dx, float dy, float dz, float len, float w, float[] c0, float[] c1) {
        float l = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        dx /= l; dy /= l; dz /= l;
        float sx = dz, sy = 0, sz = -dx;                                   // di traverso, orizzontale
        float sl = (float) Math.sqrt(sx * sx + sz * sz);
        if (sl < 1e-3f) { sx = 1; sz = 0; sl = 1; }
        sx /= sl; sz /= sl;
        float nx = sy * dz - sz * dy, ny = sz * dx - sx * dz, nzz = sx * dy - sy * dx;
        if (ny < 0) { nx = -nx; ny = -ny; nzz = -nzz; }
        int n = 7;
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < n; i++) {
                float t0 = i / (float) n, t1 = (i + 1f) / n;
                float w0 = w * (float) Math.sin(PI * Math.pow(t0, 0.75)) * (i % 2 == 0 ? 1f : 0.86f);
                float w1 = w * (float) Math.sin(PI * Math.pow(t1, 0.75)) * ((i + 1) % 2 == 0 ? 1f : 0.86f);
                float fold = 0.35f;                                         // i lembi scendono un poco dalla nervatura
                float ax = px + dx * len * t0, ay = py + dy * len * t0, az = pz + dz * len * t0;
                float bx = px + dx * len * t1, by = py + dy * len * t1, bz = pz + dz * len * t1;
                float ex0 = ax + side * sx * w0 - nx * w0 * fold, ey0 = ay - ny * w0 * fold, ez0 = az + side * sz * w0 - nzz * w0 * fold;
                float ex1 = bx + side * sx * w1 - nx * w1 * fold, ey1 = by - ny * w1 * fold, ez1 = bz + side * sz * w1 - nzz * w1 * fold;
                float nnx = nx + side * sx * fold, nny = ny, nnz = nzz + side * sz * fold;
                b.colorf(c1[0], c1[1], c1[2]); b.vert(ax, ay, az, nnx, nny, nnz, 0, 0); b.vert(bx, by, bz, nnx, nny, nnz, 0, 0);
                b.colorf(c0[0], c0[1], c0[2]); b.vert(ex1, ey1, ez1, nnx, nny, nnz, 0, 0);
                b.colorf(c1[0], c1[1], c1[2]); b.vert(ax, ay, az, nnx, nny, nnz, 0, 0);
                b.colorf(c0[0], c0[1], c0[2]); b.vert(ex1, ey1, ez1, nnx, nny, nnz, 0, 0); b.vert(ex0, ey0, ez0, nnx, nny, nnz, 0, 0);
            }
        }
    }

    /** Fiore di rovo: cinque petali bianco-rosati attorno al ciuffo di stami, rivolto verso (nx,ny,nz). */
    private static void flower(MeshBuilder b, float px, float py, float pz, float nx, float ny, float nz, float r) {
        float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        nx /= l; ny /= l; nz /= l;
        float ux = -nz, uy = 0, uz = nx;
        float ul = (float) Math.sqrt(ux * ux + uz * uz);
        if (ul < 1e-3f) { ux = 1; uz = 0; ul = 1; }
        ux /= ul; uz /= ul;
        float vx = ny * uz - nz * uy, vy = nz * ux - nx * uz, vz = nx * uy - ny * ux;
        float[] white = rgb(250, 244, 246), pink = rgb(240, 196, 214);
        for (int k = 0; k < 5; k++) {
            float an = k * 2 * PI / 5;
            float cx = (float) Math.cos(an), cy = (float) Math.sin(an);
            float dx = ux * cx + vx * cy, dy = uy * cx + vy * cy, dz = uz * cx + vz * cy;
            leaflet(b, px, py, pz, dx + nx * 0.25f, dy + ny * 0.25f, dz + nz * 0.25f, r, r * 0.45f, white, pink);
        }
        b.colorf(0.86f, 0.82f, 0.4f); b.ellipsoid(px + nx * 0.01f, py + ny * 0.01f, pz + nz * 0.01f, r * 0.3f, r * 0.3f, r * 0.3f, 6, 4);
    }

    /** Cespuglio di rovo selvatico in primavera: polloni arcuati pieni di spine, foglie composte, fiori e more acerbe. */
    static Mesh bramble() {
        MeshBuilder b = new MeshBuilder();
        java.util.Random r = new java.util.Random(31);
        final float[] caneA = rgb(96, 46, 40), caneB = rgb(84, 112, 50), thornC = rgb(150, 70, 60), thornT = rgb(230, 200, 170);
        final float[] leafD = rgb(46, 92, 36), leafL = rgb(86, 136, 54), leafY = rgb(120, 160, 70);
        int canes = 14;
        for (int k = 0; k < canes; k++) {
            float ang = k * 2 * PI / canes + (r.nextFloat() - 0.5f) * 0.5f;
            float dx = (float) Math.cos(ang), dz = (float) Math.sin(ang);
            float L = 0.42f + 0.25f * r.nextFloat(), h = k % 2 == 0 ? 0.62f + 0.2f * r.nextFloat() : 0.32f + 0.18f * r.nextFloat();
            int n = 8;
            float[][] c = new float[n][];
            float[] rr = new float[n];
            for (int i = 0; i < n; i++) {
                float t = i / (n - 1f);
                float y = h * (float) Math.sin(PI * 0.8f * t) + 0.02f;
                float side = 0.08f * (float) Math.sin(t * 5f + k);
                c[i] = new float[] {dx * (0.04f + L * t) - dz * side, y, dz * (0.04f + L * t) + dx * side};
                rr[i] = 0.024f * (1f - 0.65f * t);
            }
            FoeModels.tube(b, c, rr, 6, FoeModels.along(caneA, caneB));
            // spine ricurve all'indietro lungo il pollone
            for (int i = 1; i < n - 1; i++) for (int q = 0; q < 3; q++) {
                float t = (i + q / 3f) / (n - 1f);
                int i0 = Math.min(n - 2, (int) (t * (n - 1)));
                float f = t * (n - 1) - i0;
                float px = c[i0][0] + (c[i0 + 1][0] - c[i0][0]) * f, py = c[i0][1] + (c[i0 + 1][1] - c[i0][1]) * f, pz = c[i0][2] + (c[i0 + 1][2] - c[i0][2]) * f;
                float tx = c[i0 + 1][0] - c[i0][0], ty = c[i0 + 1][1] - c[i0][1], tz = c[i0 + 1][2] - c[i0][2];
                float sa = q * 2.1f + i;
                float ox = (float) Math.cos(sa) * dz * -1f, oy = (float) Math.sin(sa), oz = (float) Math.cos(sa) * dx;
                FoeModels.quill(b, px, py, pz, ox * 1f - tx * 4f, oy - ty * 4f, oz - tz * 4f, 0.04f, 0.009f, thornC, thornC, thornT);
            }
            // foglie composte da tre (o cinque) foglioline, alternate
            for (int i = 1; i < n - 1; i++) {
                float px = c[i][0], py = c[i][1], pz = c[i][2];
                float sgn = i % 2 == 0 ? 1 : -1;
                float lx = -dz * sgn + dx * 0.3f, lz = dx * sgn + dz * 0.3f;
                float ex = px + lx * 0.06f, ey = py + 0.03f, ez = pz + lz * 0.06f;
                FoeModels.tube(b, px, py, pz, ex, ey, ez, 0.006f, 0.005f, 4, FoeModels.flat(caneB));
                float[] col = r.nextFloat() < 0.3f ? leafL : leafD;
                leaflet(b, ex, ey, ez, lx, 0.25f, lz, 0.17f, 0.065f, rgb(30, 70, 26), col);
                leaflet(b, ex, ey, ez, lx * 0.6f - lz * 0.8f, 0.15f, lz * 0.6f + lx * 0.8f, 0.12f, 0.045f, rgb(30, 70, 26), col);
                leaflet(b, ex, ey, ez, lx * 0.6f + lz * 0.8f, 0.15f, lz * 0.6f - lx * 0.8f, 0.12f, 0.045f, rgb(30, 70, 26), leafY);
            }
            // in punta un mazzetto di fiori o qualche mora ancora verde e rossa
            float[] tip = c[n - 2];
            if (k % 3 == 0) {
                flower(b, tip[0], tip[1] + 0.03f, tip[2], dx * 0.4f, 1f, dz * 0.4f, 0.045f);
                flower(b, c[n - 3][0] - dz * 0.05f, c[n - 3][1] + 0.05f, c[n - 3][2] + dx * 0.05f, -dz, 0.8f, dx, 0.04f);
            } else {
                for (int q = 0; q < 3; q++) {
                    float bx = tip[0] + (r.nextFloat() - 0.5f) * 0.08f, by = tip[1] - 0.02f - r.nextFloat() * 0.04f, bz = tip[2] + (r.nextFloat() - 0.5f) * 0.08f;
                    float[] bc = q == 0 ? rgb(170, 30, 40) : q == 1 ? rgb(120, 150, 60) : rgb(60, 14, 30);
                    for (int d = 0; d < 5; d++) {
                        float a2 = d * 1.256f;
                        b.colorf(bc[0], bc[1], bc[2]);
                        b.ellipsoid(bx + 0.013f * (float) Math.cos(a2), by + (d == 4 ? 0.015f : 0f), bz + 0.013f * (float) Math.sin(a2), 0.014f, 0.014f, 0.014f, 6, 4);
                    }
                }
            }
        }
        // ciuffo di foglie basse al piede, per nascondere l'attacco dei polloni
        for (int k = 0; k < 7; k++) {
            float an = k * 0.9f;
            leaflet(b, 0, 0.03f, 0, (float) Math.cos(an), 0.35f, (float) Math.sin(an), 0.2f, 0.07f, rgb(30, 64, 24), leafD);
        }
        Mesh m = b.build(-1, false, true);
        m.spec = 0.2f; m.shine = 20f;
        return m;
    }
}
