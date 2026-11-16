package com.valeriodipaolo.platformgame.core3d;

import com.valeriodipaolo.platformgame.core.Img;

/**
 * Dodici nemici della seconda serie di livelli, tutti di forme morbide (ellissoidi, nessuno spigolo): talpa e lumaca (estate), castagna e gufo
 * (autunno), pinguino e pupazzo di neve (inverno), lapillo e salamandra (fuoco), rana e pianta carnivora (primavera), stella e solleone (cielo).
 * Orientati verso +z, con l'origine ai piedi.
 */
final class FoeModels2 {
    private static float[] rgb(int r, int g, int b) { return FoeModels.rgb(r, g, b); }

    private Mesh snailBody, snailShell, snailEyes, chestBody, chestEyes, owlBody, owlWing, owlEyes,
            pengBody, pengBelly, pengBits, pengWing, snowBody, snowBits, snowball, lapBody, lapEyes, salaBody, salaEyes, flame,
            frogBody, frogEyes, frogLeg, plantStem, plantHead, plantJaw, plantEyes, starBody, sunBody, sunRay, sunEyes;
    private final float[] a = new float[16], b2 = new float[16];

    private static Mesh fin(MeshBuilder b, float spec, float shine) {
        Mesh m = b.build(-1, false, true);
        m.spec = spec; m.shine = shine;
        return m;
    }



    /** Lumaca di giardino: piede lungo, umido e rugoso con coda a punta, testa che si alza e quattro tentacoli, guscio a spirale rigato. */
    private void buildSnail() {
        final float PI = (float) Math.PI;
        final float[] skinD = rgb(104, 90, 70), skinL = rgb(196, 176, 140);
        MeshBuilder b = new MeshBuilder();
        float[][] c = {{0, .03f, -.54f}, {0, .09f, -.44f}, {0, .14f, -.27f}, {0, .17f, -.05f}, {0, .18f, .15f}, {0, .18f, .30f}, {0, .21f, .41f}, {0, .27f, .47f}, {0, .30f, .515f}};
        float[] rx = {0, .06f, .15f, .21f, .22f, .20f, .16f, .12f, 0}, ry = {0, .07f, .14f, .19f, .20f, .20f, .19f, .16f, 0};
        FoeModels.loft(b, c, rx, ry, 0, 1, 0, 20, 0.8f, FoeModels.ud(skinL, skinD), 2f, 4f);
        FoeModels.Paint tent = FoeModels.along(skinD, rgb(146, 126, 98));
        for (int sd = -1; sd <= 1; sd += 2) {
            FoeModels.tube(b, new float[][] {{sd * .045f, .27f, .48f}, {sd * .075f, .40f, .54f}, {sd * .095f, .52f, .60f}}, new float[] {.02f, .015f, .011f}, 8, tent);
            FoeModels.tube(b, sd * .07f, .21f, .52f, sd * .115f, .27f, .66f, .014f, .007f, 6, tent);
        }
        snailBody = fin2(b, Img.FOE_SKIN, 0.55f, 30f);

        b = new MeshBuilder();
        int n = 64;
        float[][] sc = new float[n][3];
        float[] sr = new float[n];
        float thMax = 2.7f * PI, g = 0.185f, rho0 = 0.058f;
        for (int i = 0; i < n; i++) {
            float th = thMax * i / (n - 1);
            float rho = rho0 * (float) Math.exp(g * th), R = rho * 1.3f;
            float phi = -0.95f * PI - (thMax - th);
            sc[i] = new float[] {0, 0.40f + R * (float) Math.sin(phi) * 0.9f, -0.12f + R * (float) Math.cos(phi)};
            sr[i] = rho;
            int k = i - (n - 7);   // chiusura arrotondata della bocca: niente taglio netto
            if (k > 0) sr[i] = rho * (float) Math.sqrt(Math.max(0f, 1f - (k / 6f) * (k / 6f)));
        }
        final float[] car = rgb(168, 108, 52), cre = rgb(226, 198, 146), drk = rgb(70, 40, 22);
        FoeModels.loft(b, sc, sr, sr, 1, 0, 0, 22, 1f, new FoeModels.Paint() {
            @Override void col(float t, float an, float x, float y, float z, float[] o) {
                float sn = (float) Math.sin(an * 3f + 0.7f);
                FoeModels.mix(car, cre, FoeModels.sm(sn * 2.2f + 0.4f), o);
                if (sn < -0.8f) FoeModels.mix(o, drk, 0.85f, o);
                for (int k = 0; k < 3; k++) o[k] *= 0.8f + 0.2f * t;
            }
        }, 3f, 2f);
        snailShell = fin2(b, Img.FOE_SHELL, 0.75f, 46f);

        b = new MeshBuilder();
        for (int sd = -1; sd <= 1; sd += 2) FoeModels.ball(b, sd * .095f, .525f, .605f, .022f, .022f, .022f, rgb(10, 8, 8));
        snailEyes = fin2(b, -1, 0.9f, 48f);
    }

    private static Mesh fin2(MeshBuilder b, int tex, float spec, float shine) {
        Mesh m = b.build(tex, false, true);
        m.spec = spec; m.shine = shine;
        return m;
    }

    /** Due occhi con sclera, pupilla e (se angry > 0) sopracciglia aggrottate. */
    private static void eyes(MeshBuilder b, float y, float z, float sp, float r, float angry) {
        for (int s = -1; s <= 1; s += 2) {
            b.colorf(1f, 1f, 1f);
            b.ellipsoid(s * sp, y, z, r, r * 1.1f, r * 0.55f, 10, 7);
            b.colorf(0.04f, 0.04f, 0.1f);
            b.ellipsoid(s * sp * 0.92f, y - r * 0.05f, z + r * 0.42f, r * 0.5f, r * 0.58f, r * 0.3f, 8, 6);
            if (angry > 0) {
                b.colorf(0.1f, 0.06f, 0.05f);
                b.ellipsoid(s * sp, y + r * 1.25f, z + r * 0.1f, r * 1.1f, r * 0.2f, r * 0.2f, 8, 5);
            }
        }
    }

    FoeModels2() {
        MeshBuilder b = new MeshBuilder();

        buildSnail();

        // castagna: sfera lucida con la macchia chiara e due occhi truci
        b = new MeshBuilder();
        b.colorf(0.46f, 0.22f, 0.10f); b.ellipsoid(0, 0.35f, 0, 0.34f, 0.36f, 0.34f, 14, 10);
        b.colorf(0.62f, 0.36f, 0.18f); b.ellipsoid(0, 0.62f, 0, 0.17f, 0.1f, 0.17f, 10, 6);
        b.colorf(0.9f, 0.82f, 0.62f); b.ellipsoid(0, 0.1f, 0, 0.17f, 0.07f, 0.17f, 10, 6);
        chestBody = fin(b, 0.8f, 60f);
        b = new MeshBuilder(); eyes(b, 0.4f, 0.27f, 0.12f, 0.06f, 1f); chestEyes = fin(b, 0.6f, 30f);

        // gufo: corpo a goccia, grandi occhi gialli, becco arancio; ala come ellissoide piatto
        b = new MeshBuilder();
        b.colorf(0.50f, 0.38f, 0.28f); b.ellipsoid(0, 0.1f, 0, 0.30f, 0.38f, 0.28f, 14, 9);
        b.colorf(0.86f, 0.78f, 0.62f); b.ellipsoid(0, 0.02f, 0.1f, 0.2f, 0.28f, 0.2f, 12, 8);
        b.colorf(0.42f, 0.30f, 0.22f); b.ellipsoid(-0.17f, 0.46f, 0, 0.07f, 0.12f, 0.07f, 8, 5); b.ellipsoid(0.17f, 0.46f, 0, 0.07f, 0.12f, 0.07f, 8, 5);
        b.colorf(0.98f, 0.62f, 0.12f); b.ellipsoid(0, 0.18f, 0.28f, 0.05f, 0.07f, 0.09f, 8, 5);
        owlBody = fin(b, 0.3f, 18f);
        b = new MeshBuilder();
        b.colorf(0.95f, 0.85f, 0.25f); b.ellipsoid(-0.13f, 0.28f, 0.22f, 0.11f, 0.11f, 0.05f, 10, 6); b.ellipsoid(0.13f, 0.28f, 0.22f, 0.11f, 0.11f, 0.05f, 10, 6);
        b.colorf(0.04f, 0.04f, 0.06f); b.ellipsoid(-0.13f, 0.28f, 0.265f, 0.05f, 0.06f, 0.02f, 8, 5); b.ellipsoid(0.13f, 0.28f, 0.265f, 0.05f, 0.06f, 0.02f, 8, 5);
        owlEyes = fin(b, 0.7f, 40f);
        b = new MeshBuilder(); b.colorf(0.40f, 0.30f, 0.22f); b.ellipsoid(0.26f, 0, 0, 0.30f, 0.05f, 0.2f, 10, 6);
        owlWing = fin(b, 0.2f, 12f);

        // pinguino
        b = new MeshBuilder();
        b.colorf(0.12f, 0.14f, 0.26f); b.ellipsoid(0, 0.42f, 0, 0.30f, 0.44f, 0.27f, 14, 10);
        b.colorf(0.12f, 0.14f, 0.26f); b.ellipsoid(0, 0.78f, 0.02f, 0.2f, 0.16f, 0.2f, 12, 8);
        pengBody = fin(b, 0.4f, 30f);
        b = new MeshBuilder(); b.colorf(0.96f, 0.96f, 0.98f); b.ellipsoid(0, 0.4f, 0.1f, 0.22f, 0.36f, 0.2f, 12, 8);
        b.ellipsoid(0, 0.77f, 0.1f, 0.12f, 0.1f, 0.1f, 10, 6);
        pengBelly = fin(b, 0.3f, 24f);
        b = new MeshBuilder(); eyes(b, 0.8f, 0.17f, 0.09f, 0.04f, 1f);
        b.colorf(0.98f, 0.62f, 0.12f); b.ellipsoid(0, 0.72f, 0.27f, 0.07f, 0.04f, 0.1f, 8, 5);
        b.ellipsoid(-0.12f, 0.03f, 0.1f, 0.1f, 0.03f, 0.15f, 8, 5); b.ellipsoid(0.12f, 0.03f, 0.1f, 0.1f, 0.03f, 0.15f, 8, 5);
        pengBits = fin(b, 0.5f, 30f);
        b = new MeshBuilder(); b.colorf(0.12f, 0.14f, 0.26f); b.ellipsoid(0.12f, -0.16f, 0, 0.05f, 0.22f, 0.12f, 8, 6);
        pengWing = fin(b, 0.3f, 20f);

        // pupazzo di neve: tre sfere, naso a carota, cappello
        b = new MeshBuilder();
        b.colorf(0.96f, 0.97f, 1f); b.ellipsoid(0, 0.32f, 0, 0.36f, 0.32f, 0.36f, 14, 9); b.ellipsoid(0, 0.78f, 0, 0.27f, 0.25f, 0.27f, 14, 9); b.ellipsoid(0, 1.15f, 0, 0.2f, 0.2f, 0.2f, 12, 8);
        snowBody = fin(b, 0.4f, 28f);
        b = new MeshBuilder(); eyes(b, 1.2f, 0.16f, 0.08f, 0.035f, 1f);
        b.colorf(0.98f, 0.5f, 0.12f); b.ellipsoid(0, 1.13f, 0.28f, 0.04f, 0.04f, 0.16f, 8, 5);
        b.colorf(0.14f, 0.14f, 0.18f); b.ellipsoid(0, 1.36f, 0, 0.17f, 0.12f, 0.17f, 10, 6); b.ellipsoid(0, 1.3f, 0, 0.26f, 0.04f, 0.26f, 12, 5);
        b.ellipsoid(0, 0.8f, 0.26f, 0.04f, 0.04f, 0.03f, 6, 4); b.ellipsoid(0, 0.68f, 0.27f, 0.04f, 0.04f, 0.03f, 6, 4);
        snowBits = fin(b, 0.5f, 30f);
        b = new MeshBuilder(); b.colorf(1f, 1f, 1f); b.ellipsoid(0, 0, 0, 0.17f, 0.17f, 0.17f, 10, 7);
        snowball = fin(b, 0.3f, 20f);

        // lapillo: sfera incandescente
        b = new MeshBuilder();
        b.colorf(1f, 0.38f, 0.05f); b.ellipsoid(0, 0, 0, 0.36f, 0.36f, 0.36f, 14, 10);
        b.colorf(1f, 0.78f, 0.2f); b.ellipsoid(0, 0.02f, 0.05f, 0.24f, 0.24f, 0.24f, 12, 8);
        b.colorf(0.95f, 0.25f, 0.05f); b.ellipsoid(0, 0.4f, -0.1f, 0.1f, 0.12f, 0.1f, 8, 5);
        lapBody = fin(b, 0.2f, 10f);
        b = new MeshBuilder(); eyes(b, 0.06f, 0.3f, 0.12f, 0.06f, 1f); lapEyes = fin(b, 0.4f, 20f);

        // salamandra: corpo allungato rosso, macchie gialle, coda
        b = new MeshBuilder();
        b.colorf(0.88f, 0.28f, 0.14f); b.ellipsoid(0, 0.3f, 0, 0.26f, 0.24f, 0.55f, 14, 10); b.ellipsoid(0, 0.34f, 0.5f, 0.2f, 0.18f, 0.2f, 12, 8);
        b.ellipsoid(0, 0.22f, -0.62f, 0.14f, 0.12f, 0.28f, 10, 7); b.ellipsoid(0, 0.18f, -0.9f, 0.08f, 0.07f, 0.2f, 8, 5);
        b.colorf(0.98f, 0.82f, 0.22f);
        for (int i = 0; i < 4; i++) { b.ellipsoid(0, 0.52f, -0.35f + i * 0.2f, 0.07f, 0.04f, 0.06f, 8, 4); }
        b.colorf(0.96f, 0.6f, 0.4f); b.ellipsoid(0, 0.12f, 0.12f, 0.2f, 0.1f, 0.4f, 10, 6);
        for (int s = -1; s <= 1; s += 2) { b.colorf(0.80f, 0.24f, 0.12f); b.ellipsoid(s * 0.27f, 0.1f, 0.28f, 0.07f, 0.07f, 0.13f, 8, 5); b.ellipsoid(s * 0.27f, 0.1f, -0.25f, 0.07f, 0.07f, 0.13f, 8, 5); }
        salaBody = fin(b, 0.4f, 28f);
        b = new MeshBuilder(); eyes(b, 0.44f, 0.6f, 0.1f, 0.05f, 1f); salaEyes = fin(b, 0.6f, 30f);
        b = new MeshBuilder();
        b.colorf(1f, 0.35f, 0.05f); b.ellipsoid(0, 0, 0.55f, 0.2f, 0.2f, 0.65f, 10, 7);
        b.colorf(1f, 0.62f, 0.12f); b.ellipsoid(0, 0, 0.95f, 0.26f, 0.26f, 0.7f, 10, 7);
        b.colorf(1f, 0.9f, 0.4f); b.ellipsoid(0, 0, 0.45f, 0.1f, 0.1f, 0.4f, 8, 5);
        flame = fin(b, 0f, 1f);

        // rana
        b = new MeshBuilder();
        b.colorf(0.30f, 0.66f, 0.22f); b.ellipsoid(0, 0.26f, 0, 0.34f, 0.24f, 0.4f, 14, 9);
        b.colorf(0.88f, 0.9f, 0.62f); b.ellipsoid(0, 0.2f, 0.1f, 0.24f, 0.14f, 0.3f, 12, 7);
        b.colorf(0.34f, 0.72f, 0.26f); b.ellipsoid(-0.17f, 0.46f, 0.22f, 0.11f, 0.11f, 0.11f, 10, 7); b.ellipsoid(0.17f, 0.46f, 0.22f, 0.11f, 0.11f, 0.11f, 10, 7);
        b.colorf(0.9f, 0.4f, 0.4f); b.ellipsoid(0, 0.3f, 0.36f, 0.2f, 0.02f, 0.05f, 8, 4);
        frogBody = fin(b, 0.5f, 34f);
        b = new MeshBuilder(); eyes(b, 0.48f, 0.3f, 0.17f, 0.065f, 0f); frogEyes = fin(b, 0.7f, 40f);
        b = new MeshBuilder(); b.colorf(0.28f, 0.60f, 0.2f); b.ellipsoid(0.1f, 0.12f, -0.05f, 0.14f, 0.17f, 0.3f, 10, 6); b.ellipsoid(0.12f, 0.03f, 0.2f, 0.1f, 0.04f, 0.22f, 8, 5);
        frogLeg = fin(b, 0.4f, 26f);

        // pianta carnivora: gambo, testa rossa con macchie chiare, mascella inferiore
        b = new MeshBuilder();
        b.colorf(0.22f, 0.56f, 0.18f); b.ellipsoid(0, 0.3f, 0, 0.09f, 0.34f, 0.09f, 8, 7); b.ellipsoid(-0.18f, 0.18f, 0, 0.2f, 0.04f, 0.1f, 8, 5); b.ellipsoid(0.18f, 0.2f, 0, 0.2f, 0.04f, 0.1f, 8, 5);
        plantStem = fin(b, 0.2f, 12f);
        b = new MeshBuilder();
        b.colorf(0.84f, 0.12f, 0.20f); b.ellipsoid(0, 0.12f, 0.02f, 0.34f, 0.24f, 0.34f, 14, 9);
        b.colorf(0.98f, 0.9f, 0.82f); for (int i = 0; i < 4; i++) b.ellipsoid(-0.18f + i * 0.12f, 0.3f, 0.12f, 0.05f, 0.03f, 0.05f, 6, 4);
        plantHead = fin(b, 0.5f, 34f);
        b = new MeshBuilder();
        b.colorf(0.72f, 0.08f, 0.16f); b.ellipsoid(0, -0.08f, 0.02f, 0.32f, 0.16f, 0.32f, 14, 8);
        b.colorf(1f, 1f, 0.95f); for (int i = 0; i < 5; i++) b.ellipsoid(-0.24f + i * 0.12f, 0.06f, 0.28f, 0.035f, 0.07f, 0.035f, 6, 4);
        plantJaw = fin(b, 0.5f, 34f);
        b = new MeshBuilder(); eyes(b, 0.3f, 0.26f, 0.12f, 0.05f, 1f); plantEyes = fin(b, 0.6f, 30f);

        // stella: centro e cinque punte arrotondate
        b = new MeshBuilder();
        b.colorf(1f, 0.88f, 0.28f); b.ellipsoid(0, 0, 0, 0.26f, 0.26f, 0.16f, 12, 8);
        for (int i = 0; i < 5; i++) {
            double an = Math.PI / 2 + i * 2 * Math.PI / 5;
            b.colorf(1f, 0.8f, 0.2f);
            b.ellipsoid((float) Math.cos(an) * 0.27f, (float) Math.sin(an) * 0.27f, 0, 0.1f, 0.1f, 0.12f, 8, 6);
            b.ellipsoid((float) Math.cos(an) * 0.16f, (float) Math.sin(an) * 0.16f, 0, 0.12f, 0.12f, 0.14f, 8, 6);
        }
        b.colorf(1f, 0.98f, 0.8f); b.ellipsoid(0, 0.02f, 0.1f, 0.14f, 0.14f, 0.08f, 8, 6);
        b.colorf(0.9f, 0.5f, 0.1f); b.ellipsoid(-0.07f, 0.04f, 0.17f, 0.03f, 0.04f, 0.02f, 6, 4); b.ellipsoid(0.07f, 0.04f, 0.17f, 0.03f, 0.04f, 0.02f, 6, 4);
        starBody = fin(b, 0.2f, 10f);

        // solleone: faccia tonda con raggi che girano
        b = new MeshBuilder();
        b.colorf(1f, 0.62f, 0.12f); b.ellipsoid(0, 0, 0, 0.34f, 0.34f, 0.28f, 14, 9);
        b.colorf(1f, 0.82f, 0.3f); b.ellipsoid(0, 0.02f, 0.06f, 0.26f, 0.26f, 0.24f, 12, 8);
        sunBody = fin(b, 0.2f, 10f);
        b = new MeshBuilder(); b.colorf(1f, 0.5f, 0.08f); b.ellipsoid(0, 0.5f, 0, 0.09f, 0.17f, 0.09f, 8, 6);
        sunRay = fin(b, 0.2f, 10f);
        b = new MeshBuilder(); eyes(b, 0.06f, 0.2f, 0.11f, 0.05f, 1f); sunEyes = fin(b, 0.6f, 30f);
    }

    void drawShot(Gfx3D g, float[] m, float[] s) {
        System.arraycopy(m, 0, a, 0, 16);
        Mat4.translate(a, s[0], s[1], s[2]);
        g.draw(snowball, a, 1, 1, 1, 1, 0.1f);
    }

    void draw(Gfx3D g, float[] m, Enemy3 e) {
        float an = e.anim;
        switch (e.type) {
            case Enemy3.SNAIL: {
                boolean shell = e.mode == 1;
                System.arraycopy(m, 0, a, 0, 16);
                if (shell) { Mat4.translate(a, 0, -0.1f, 0); Mat4.scale(a, 0.85f, 0.85f, 0.85f); } else Mat4.scale(a, 1f + 0.04f * (float) Math.sin(an * 3f), 1f, 1f + 0.03f * (float) Math.sin(an * 3f + 1f));
                if (!shell) { g.draw(snailBody, a, 1, 1, 1, 1, 0); g.draw(snailEyes, a, 1, 1, 1, 1, 0); }
                g.draw(snailShell, a, 1, 1, 1, 1, 0);
                return;
            }
            case Enemy3.CHESTNUT: {
                System.arraycopy(m, 0, a, 0, 16);
                Mat4.translate(a, 0, -0.35f, 0);
                if (e.mode == 1) Mat4.translate(a, 0.04f * (float) Math.sin(an * 60f), 0, 0);
                if (e.mode == 3) Mat4.rotateX(a, an * 9f * e.dir);
                g.draw(chestBody, a, 1, 1, 1, 1, 0);
                if (e.mode != 3) g.draw(chestEyes, a, 1, 1, 1, 1, 0.3f);
                return;
            }
            case Enemy3.OWL: {
                System.arraycopy(m, 0, a, 0, 16);
                Mat4.translate(a, 0, -0.1f, 0);
                if (e.mode == 1) Mat4.rotateX(a, 0.6f);
                g.draw(owlBody, a, 1, 1, 1, 1, 0);
                g.draw(owlEyes, a, 1, 1, 1, 1, 0.5f);
                float flap = 0.7f * (float) Math.sin(an * (e.mode == 0 ? 7f : 14f));
                for (int s = -1; s <= 1; s += 2) {
                    System.arraycopy(a, 0, b2, 0, 16);
                    Mat4.translate(b2, s * 0.22f, 0.15f, 0);
                    if (s < 0) Mat4.scale(b2, -1, 1, 1);
                    Mat4.rotateZ(b2, flap);
                    g.draw(owlWing, b2, 1, 1, 1, 1, 0);
                }
                return;
            }
            case Enemy3.PENGUIN: {
                System.arraycopy(m, 0, a, 0, 16);
                float w = (float) Math.sin(an * 18f);
                Mat4.rotateZ(a, 0.1f * w);
                g.draw(pengBody, a, 1, 1, 1, 1, 0);
                g.draw(pengBelly, a, 1, 1, 1, 1, 0);
                g.draw(pengBits, a, 1, 1, 1, 1, 0.2f);
                for (int s = -1; s <= 1; s += 2) {
                    System.arraycopy(a, 0, b2, 0, 16);
                    Mat4.translate(b2, s * 0.27f, 0.6f, 0);
                    if (s < 0) Mat4.scale(b2, -1, 1, 1);
                    Mat4.rotateZ(b2, -0.5f - 0.3f * w);
                    g.draw(pengWing, b2, 1, 1, 1, 1, 0);
                }
                return;
            }
            case Enemy3.SNOWMAN: {
                System.arraycopy(m, 0, a, 0, 16);
                float charge = e.t > 1.8f ? 0.06f * (float) Math.sin(an * 40f) : 0f;
                Mat4.scale(a, 1f + charge, 1f, 1f + charge);
                g.draw(snowBody, a, 1, 1, 1, 1, 0);
                g.draw(snowBits, a, 1, 1, 1, 1, 0.1f);
                return;
            }
            case Enemy3.LAPILLO: {
                if (e.mode == 0) return;
                System.arraycopy(m, 0, a, 0, 16);
                Mat4.translate(a, 0, 0.36f, 0);
                float f = 1f + 0.08f * (float) Math.sin(an * 30f);
                Mat4.scale(a, f, f, f);
                g.draw(lapBody, a, 1.2f, 1f, 0.9f, 1, 0.85f);
                g.draw(lapEyes, a, 1, 1, 1, 1, 0.5f);
                return;
            }
            case Enemy3.SALAMANDER: {
                System.arraycopy(m, 0, a, 0, 16);
                float sw = e.mode == 1 ? 1f + 0.12f * (float) Math.sin(an * 30f) : 1f;
                Mat4.scale(a, sw, sw, 1f);
                if (e.mode == 0) Mat4.rotateY(a, 0.1f * (float) Math.sin(an * 8f));
                g.draw(salaBody, a, e.mode == 1 ? 1.3f : 1f, e.mode == 1 ? 1f : 1f, e.mode == 1 ? 0.8f : 1f, 1, e.mode == 1 ? 0.25f : 0f);
                g.draw(salaEyes, a, 1, 1, 1, 1, 0.3f);
                if (e.mode == 2) {
                    System.arraycopy(m, 0, b2, 0, 16);
                    Mat4.translate(b2, 0, 0.34f, 0.5f);
                    float k = 1f + 0.1f * (float) Math.sin(an * 40f);
                    Mat4.scale(b2, k, k, 1f);
                    g.draw(flame, b2, 1, 1, 1, 1, 1f);
                }
                return;
            }
            case Enemy3.FROG: {
                System.arraycopy(m, 0, a, 0, 16);
                boolean air = e.mode == 1;
                float cr = air ? 1.12f : 0.92f + 0.04f * (float) Math.sin(an * 3f);
                Mat4.scale(a, air ? 0.92f : 1.04f, cr, air ? 1.1f : 1f);
                if (air) Mat4.rotateX(a, -0.35f);
                g.draw(frogBody, a, 1, 1, 1, 1, 0);
                g.draw(frogEyes, a, 1, 1, 1, 1, 0.3f);
                for (int s = -1; s <= 1; s += 2) {
                    System.arraycopy(a, 0, b2, 0, 16);
                    if (s < 0) Mat4.scale(b2, -1, 1, 1);
                    if (air) Mat4.rotateX(b2, 0.9f);
                    g.draw(frogLeg, b2, 1, 1, 1, 1, 0);
                }
                return;
            }
            case Enemy3.PLANT: {
                g.draw(plantStem, m, 1, 1, 1, 1, 0);
                System.arraycopy(m, 0, a, 0, 16);
                float open = e.mode == 2 ? 0.8f : e.mode == 1 ? 0.12f + 0.1f * (float) Math.sin(an * 45f) : 0f;
                float yaw = e.mode == 0 ? 0 : 0f;
                Mat4.translate(a, 0, 0.72f, 0);
                Mat4.rotateX(a, -0.25f + 0.1f * (float) Math.sin(an * 2f));
                g.draw(plantJaw, a, 1, 1, 1, 1, 0);
                System.arraycopy(a, 0, b2, 0, 16);
                Mat4.translate(b2, 0, 0.0f, -0.28f);
                Mat4.rotateX(b2, -open);
                Mat4.translate(b2, 0, 0.0f, 0.28f);
                g.draw(plantHead, b2, e.mode == 2 ? 1.2f : 1f, 1f, 1f, 1, 0);
                g.draw(plantEyes, b2, 1, 1, 1, 1, 0.3f);
                return;
            }
            case Enemy3.STAR: {
                System.arraycopy(m, 0, a, 0, 16);
                Mat4.rotateZ(a, an * 4f);
                g.draw(starBody, a, 1f, 0.95f, 0.8f, 1, 0.9f);
                return;
            }
            default: {
                System.arraycopy(m, 0, a, 0, 16);
                Mat4.translate(a, 0, 0.3f, 0);
                g.draw(sunBody, a, 1, 1, 1, 1, 0.8f);
                g.draw(sunEyes, a, 1, 1, 1, 1, 0.4f);
                for (int i = 0; i < 8; i++) {
                    System.arraycopy(a, 0, b2, 0, 16);
                    Mat4.rotateZ(b2, an * 2f + i * 0.7854f);
                    g.draw(sunRay, b2, 1, 1, 1, 1, 0.8f);
                }
            }
        }
    }
}
