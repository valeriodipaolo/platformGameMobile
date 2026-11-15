package com.valeriodipaolo.platformgame.core3d;

import static com.valeriodipaolo.platformgame.core3d.FoeModels.*;

import com.valeriodipaolo.platformgame.core.Img;

/**
 * Dodici nemici della seconda serie di livelli, modellati su animali e oggetti veri: talpa europea e lumaca di giardino (estate), riccio di
 * castagna e gufo (autunno), pinguino imperatore e pupazzo di neve (inverno), bomba di lava e salamandra pezzata (fuoco), raganella e dionea
 * (primavera), cristallo stellare e sole di plasma (cielo). Corpi a sezioni con texture di pelo, pelle, piume, guscio e neve, occhi a perla
 * lucida senza espressioni da fumetto. Orientati verso +z, con l'origine ai piedi.
 */
final class FoeModels2 {
    private static final float PI = (float) Math.PI;

    private Mesh mound, moleBody, moleBits, moleGloss;
    private Mesh snailBody, snailShell, snailGloss;
    private Mesh chestBody, chestSpines, chestNut;
    private Mesh owlBody, owlBits, owlGloss;
    private final Mesh[] owlWing = new Mesh[2], frogLeg = new Mesh[2], pengWing = new Mesh[2];
    private Mesh pengBody, pengBits, pengGloss;
    private Mesh snowBody, snowScarf, snowBits, snowTwigs, snowball;
    private Mesh lapBody;
    private Mesh salaBody, salaGloss, flame;
    private Mesh frogBody, frogGloss;
    private Mesh plantStem, plantTop, plantBottom;
    private Mesh starBody, sunBody, sunProm, sunHalo;
    private final float[] a = new float[16], b2 = new float[16];

    private static Mesh fin(MeshBuilder b, int tex, float spec, float shine) {
        Mesh m = b.build(tex, false, true);
        m.spec = spec;
        m.shine = shine;
        return m;
    }

    private static float h(float x, float y) {
        float s = (float) Math.sin(x * 127.1f + y * 311.7f) * 43758.5453f;
        return s - (float) Math.floor(s);
    }

    /** Occhio a perla: sfera nera lucida con un riflesso e, se serve, un anello di iride colorato. */
    private static void bead(MeshBuilder b, float x, float y, float z, float r, float[] iris) {
        if (iris != null) ball(b, x, y, z, r * 1.3f, r * 1.3f, r * 1.1f, iris);
        ball(b, x, y, z + r * 0.25f, r, r, r * 0.9f, rgb(8, 8, 10));
        ball(b, x - r * 0.35f, y + r * 0.4f, z + r * 0.85f, r * 0.28f, r * 0.24f, r * 0.14f, rgb(255, 255, 255));
    }

    /** Sfera irregolare: i raggi di ogni anello oscillano un poco, così la sagoma non è un cerchio perfetto. */
    static void lump(MeshBuilder b, float cx, float cy, float cz, float r, float sy, int seed, Paint p, float uRep, float vRep) {
        int rings = 12;
        float[][] c = new float[rings + 1][];
        float[] rx = new float[rings + 1], ry = new float[rings + 1];
        for (int i = 0; i <= rings; i++) {
            double t = PI * i / rings;
            float j = 1f + 0.07f * (h(i, seed) - 0.5f) * 2f;
            c[i] = new float[] {cx + 0.02f * r * (h(i, seed + 5) - 0.5f) * 4f, cy - r * sy * (float) Math.cos(t), cz + 0.02f * r * (h(i, seed + 9) - 0.5f) * 4f};
            rx[i] = r * (float) Math.sin(t) * j;
            ry[i] = rx[i];
        }
        rx[0] = ry[0] = rx[rings] = ry[rings] = 0;
        loft(b, c, rx, ry, 0, 0, 1, 18, 1f, p, uRep, vRep);
    }

    /** Pelle tesa lungo un asse verticale (uccelli, pupazzo): sezioni con raggio laterale rx e raggio avanti/dietro rz. */
    private static void bodyY(MeshBuilder b, float[][] c, float[] rx, float[] rz, int segs, Paint p, float uRep, float vRep) {
        loft(b, c, rx, rz, 0, 0, 1, segs, 1f, p, uRep, vRep);
    }

    FoeModels2() {
        buildMole();
        buildSnail();
        buildChestnut();
        buildOwl();
        buildPenguin();
        buildSnowman();
        buildLapillo();
        buildSalamander();
        buildFrog();
        buildPlant();
        buildSky();
    }

    // ------------------------------------------------------------------ talpa

    private void buildMole() {
        final float[] pink = rgb(226, 150, 140), cream = rgb(238, 214, 190);
        MeshBuilder b = new MeshBuilder();
        // cumulo di terra smossa: lumpy, con il buco scuro al centro
        Paint dirt = ud(rgb(70, 52, 38), rgb(112, 86, 62));
        ballY(b, 0, 0.02f, 0, 0.64f, 0.19f, 0.64f, 18, 8, dirt, 3f, 3f);
        for (int i = 0; i < 9; i++) {
            float an = i * 0.7f + h(i, 1), rr = 0.46f + 0.12f * h(i, 2);
            lump(b, (float) Math.cos(an) * rr, 0.07f, (float) Math.sin(an) * rr, 0.09f + 0.05f * h(i, 3), 0.75f, 20 + i, dirt, 2f, 2f);
        }
        ball(b, 0, 0.15f, 0, 0.34f, 0.03f, 0.34f, rgb(14, 10, 8));
        mound = fin(b, Img.R3_DIRT, 0.05f, 6f);

        b = new MeshBuilder();
        // corpo cilindrico dal pelo vellutato nero-grigio, dalla groppa al muso
        float[][] c = {{0, .30f, -.40f}, {0, .32f, -.32f}, {0, .36f, -.15f}, {0, .38f, .05f}, {0, .37f, .22f}, {0, .34f, .34f}, {0, .31f, .44f}, {0, .29f, .52f}, {0, .285f, .58f}};
        float[] rx = {0, .14f, .27f, .34f, .33f, .26f, .17f, .09f, 0}, ry = {0, .12f, .22f, .28f, .27f, .22f, .15f, .08f, 0};
        loft(b, c, rx, ry, 0, 1, 0, 22, 0.8f, ud(rgb(92, 82, 80), rgb(50, 45, 46)), 3f, 3f);
        moleBody = fin(b, Img.FOE_FUR, 0.1f, 10f);

        b = new MeshBuilder();
        // muso rosa appuntito con le narici, mani a pala con cinque artigli, polsi
        loft(b, new float[][] {{0, .29f, .50f}, {0, .29f, .58f}, {0, .285f, .66f}, {0, .28f, .70f}}, new float[] {.085f, .06f, .03f, 0}, new float[] {.07f, .05f, .028f, 0},
                0, 1, 0, 12, 1f, along(pink, rgb(240, 170, 160)), 0, 0);
        for (int s = -1; s <= 1; s += 2) {
            tube(b, s * .27f, .25f, .12f, s * .34f, .15f, .30f, .06f, .045f, 8, flat(rgb(70, 60, 58)));
            ballZ(b, s * .36f, .13f, .38f, .12f, .03f, .15f, 12, 7, along(pink, cream), 1f, 1f);
            for (int k = -2; k <= 2; k++) {
                quill(b, s * .36f + k * .045f, .13f, .50f, k * .12f, -.03f, 1f, .08f, .014f, cream, rgb(246, 230, 210), rgb(250, 244, 236));
            }
        }
        moleBits = fin(b, Img.FOE_SKIN, 0.35f, 24f);

        b = new MeshBuilder();
        ball(b, 0, .282f, .70f, .02f, .018f, .02f, rgb(60, 28, 30));
        for (int s = -1; s <= 1; s += 2) bead(b, s * .1f, .405f, .3f, .015f, null);
        moleGloss = fin(b, Img.FOE_SKIN, 0.9f, 48f);
    }

    // ------------------------------------------------------------------ lumaca

    private void buildSnail() {
        final float[] skinD = rgb(112, 96, 74), skinL = rgb(198, 178, 142);
        MeshBuilder b = new MeshBuilder();
        // piede lungo e umido, coda a punta dietro, testa che si alza davanti; tentacoli oculari e tentacoli corti
        float[][] c = {{0, .012f, -.52f}, {0, .04f, -.42f}, {0, .07f, -.25f}, {0, .085f, -.05f}, {0, .09f, .15f}, {0, .10f, .30f}, {0, .15f, .40f}, {0, .20f, .46f}, {0, .22f, .50f}};
        float[] rx = {0, .035f, .10f, .14f, .15f, .14f, .115f, .085f, 0}, ry = {0, .03f, .06f, .08f, .09f, .095f, .10f, .09f, 0};
        loft(b, c, rx, ry, 0, 1, 0, 18, 0.55f, ud(skinL, skinD), 2f, 4f);
        Paint tent = along(skinD, rgb(150, 130, 100));
        for (int s = -1; s <= 1; s += 2) {
            tube(b, new float[][] {{s * .04f, .24f, .45f}, {s * .06f, .36f, .50f}, {s * .075f, .47f, .56f}}, new float[] {.018f, .013f, .01f}, 7, tent);
            tube(b, s * .06f, .19f, .50f, s * .10f, .25f, .63f, .012f, .006f, 6, tent);
        }
        snailBody = fin(b, Img.FOE_SKIN, 0.55f, 30f);

        b = new MeshBuilder();
        // guscio: tubo che si avvolge a spirale logaritmica, rigato a bande caramello, crema e marrone scuro
        int n = 60;
        float[][] sc = new float[n][3];
        float[] sr = new float[n];
        float thMax = 2.7f * PI, g = 0.185f, rho0 = 0.05f;
        for (int i = 0; i < n; i++) {
            float th = thMax * i / (n - 1);
            float rho = rho0 * (float) Math.exp(g * th), R = rho * 1.3f;
            float phi = -0.95f * PI - (thMax - th);
            sc[i] = new float[] {0, 0.30f + R * (float) Math.sin(phi) * 0.9f, -0.08f + R * (float) Math.cos(phi)};
            sr[i] = rho;
        }
        final float[] car = rgb(172, 112, 54), cre = rgb(228, 200, 148), drk = rgb(72, 42, 24);
        loft(b, sc, sr, sr, 1, 0, 0, 20, 1f, new Paint() {
            @Override void col(float t, float an, float x, float y, float z, float[] o) {
                float s = (float) Math.sin(an * 3f + 0.7f);
                mix(car, cre, sm(s * 2.2f + 0.4f), o);
                if (s < -0.8f) mix(o, drk, 0.85f, o);
                for (int k = 0; k < 3; k++) o[k] *= 0.8f + 0.2f * t;
            }
        }, 3f, 2f);
        snailShell = fin(b, Img.FOE_SHELL, 0.75f, 46f);

        b = new MeshBuilder();
        for (int s = -1; s <= 1; s += 2) bead(b, s * .075f, .47f, .565f, .017f, null);
        snailGloss = fin(b, -1, 0.9f, 48f);
    }

    // ------------------------------------------------------------------ riccio di castagna

    private void buildChestnut() {
        MeshBuilder b = new MeshBuilder();
        float cy = 0.34f, R = 0.28f;
        ballY(b, 0, cy, 0, R, R, R, 18, 12, ud(rgb(86, 84, 36), rgb(110, 108, 46)), 2f, 2f);
        chestBody = fin(b, -1, 0.1f, 8f);

        b = new MeshBuilder();
        java.util.Random r = new java.util.Random(55);
        int n = 420;
        float[] c0 = new float[3], c1 = new float[3], c2 = new float[3];
        for (int i = 0; i < n; i++) {
            float uy = 1f - 2f * (i + 0.5f) / n, ang = i * 2.39996f;
            float rr = (float) Math.sqrt(Math.max(0, 1 - uy * uy)), ux = rr * (float) Math.cos(ang), uz = rr * (float) Math.sin(ang);
            if (uz > 0.60f && uy < 0.45f && uy > -0.55f) continue;                    // la buccia si è aperta: si vede la castagna
            float len = 0.11f + 0.07f * r.nextFloat();
            float t = 0.85f + 0.3f * r.nextFloat();
            mix(rgb(70, 72, 28), rgb(96, 92, 38), r.nextFloat(), c0);
            mix(rgb(120, 108, 50), rgb(160, 136, 68), r.nextFloat(), c1);
            mix(rgb(214, 186, 120), rgb(240, 222, 170), r.nextFloat(), c2);
            for (int k = 0; k < 3; k++) { c0[k] *= t; c1[k] *= t; }
            quill(b, ux * R * 0.96f, cy + uy * R * 0.96f, uz * R * 0.96f, ux + 0.2f * (r.nextFloat() - .5f), uy + 0.2f * (r.nextFloat() - .5f) - 0.05f,
                    uz + 0.2f * (r.nextFloat() - .5f), len, 0.018f, c0, c1, c2);
        }
        chestSpines = fin(b, -1, 0.25f, 18f);

        b = new MeshBuilder();
        // castagna lucida che spunta dalla buccia, con la macchia chiara in basso
        final float[] mah = rgb(124, 56, 26), pale = rgb(222, 198, 160);
        ballY(b, 0, cy + 0.02f, 0.22f, 0.14f, 0.16f, 0.12f, 14, 10, new Paint() {
            @Override void col(float t, float an, float x, float y, float z, float[] o) {
                mix(mah, rgb(70, 30, 14), 0.5f * sm(y - 0.4f), o);
                mix(o, pale, sm((0.31f - y) / 0.06f), o);
            }
        }, 1.5f, 1.5f);
        chestNut = fin(b, Img.FOE_MAHOGANY, 0.85f, 52f);
    }

    // ------------------------------------------------------------------ gufo

    private static Paint owlPaint() {
        final float[] tawny = rgb(128, 92, 58), dark = rgb(66, 46, 32), buff = rgb(226, 204, 166), streak = rgb(92, 66, 46);
        return new Paint() {
            @Override void col(float t, float an, float x, float y, float z, float[] o) {
                float front = sm((float) Math.sin(an) * 2.2f + 0.2f);
                float bars = sm(((float) Math.sin(y * 42f + x * 5f) - 0.3f) * 3f);
                mix(tawny, dark, 0.55f * bars, o);
                float[] fr = new float[3];
                float st = sm(((float) Math.sin(x * 70f + y * 4f) - 0.55f) * 4f) * sm((0.78f - y) * 8f) * sm((y - 0.12f) * 8f);
                mix(buff, streak, 0.75f * st, fr);
                mix(o, fr, front, o);
            }
        };
    }

    private void buildOwl() {
        final float[] horn = rgb(206, 188, 140);
        MeshBuilder b = new MeshBuilder();
        float[][] c = {{0, .10f, -.10f}, {0, .20f, -.06f}, {0, .34f, 0f}, {0, .48f, .01f}, {0, .60f, .03f}, {0, .68f, .05f}, {0, .78f, .07f}, {0, .87f, .08f}, {0, .91f, .08f}};
        float[] rx = {0, .12f, .215f, .25f, .235f, .19f, .175f, .125f, 0}, rz = {0, .10f, .18f, .20f, .18f, .15f, .155f, .115f, 0};
        bodyY(b, c, rx, rz, 22, owlPaint(), 3f, 3f);
        // ciuffi auricolari e gambe piumate
        Paint tuft = along(rgb(120, 86, 54), rgb(60, 42, 30));
        for (int s = -1; s <= 1; s += 2) {
            tube(b, new float[][] {{s * .11f, .88f, .02f}, {s * .13f, .96f, .0f}, {s * .15f, 1.03f, -.02f}}, new float[] {.032f, .02f, .004f}, 6, tuft);
            tube(b, s * .08f, .13f, .04f, s * .08f, .02f, .09f, .055f, .04f, 8, ud(rgb(214, 192, 154), rgb(150, 112, 74)));
        }
        owlBody = fin(b, Img.FOE_PLUME, 0.25f, 16f);

        b = new MeshBuilder();
        // disco facciale chiaro con bordo scuro, becco ad uncino, dita e artigli, piume della coda
        for (int s = -1; s <= 1; s += 2) {
            ballZ(b, s * .082f, .775f, .125f, .105f, .115f, .035f, 14, 8, flat(rgb(96, 70, 48)), 1f, 1f);
            ballZ(b, s * .08f, .78f, .142f, .088f, .098f, .03f, 14, 8, flat(rgb(236, 220, 188)), 1f, 1f);
        }
        loft(b, new float[][] {{0, .765f, .17f}, {0, .755f, .205f}, {0, .73f, .235f}, {0, .70f, .245f}}, new float[] {.028f, .024f, .014f, 0}, new float[] {.034f, .03f, .02f, 0},
                0, 1, 0, 10, 1f, along(horn, rgb(120, 100, 70)), 0, 0);
        Paint claw = along(rgb(206, 190, 150), rgb(40, 34, 30));
        for (int s = -1; s <= 1; s += 2) {
            for (int k = -1; k <= 1; k++) {
                tube(b, new float[][] {{s * .08f, .025f, .09f}, {s * .08f + k * .03f, .012f, .15f}, {s * .08f + k * .042f, .004f, .185f}}, new float[] {.018f, .014f, .004f}, 5, claw);
            }
        }
        for (int i = -2; i <= 2; i++) plume(b, i * .03f, .12f, -.12f, i * .02f, -.1f, -.17f, .04f, .01f, rgb(116, 84, 54), rgb(60, 44, 32));
        owlBits = fin(b, Img.FOE_CARD, 0.15f, 20f);
        owlBits.cutout = true;

        b = new MeshBuilder();
        for (int s = -1; s <= 1; s += 2) bead(b, s * .08f, .795f, .165f, .03f, rgb(214, 150, 40));
        owlGloss = fin(b, -1, 0.95f, 50f);

        for (int k = 0; k < 2; k++) {
            int sx = k == 0 ? 1 : -1;
            b = new MeshBuilder();
            loft(b, new float[][] {{0, 0, 0}, {sx * .2f, .01f, .0f}, {sx * .42f, .0f, -.02f}}, new float[] {.06f, .045f, .03f}, new float[] {.04f, .032f, .022f}, 0, 1, 0, 8, 1f,
                    flat(rgb(120, 88, 56)), 0, 0);
            for (int layer = 0; layer < 2; layer++) {
                int nf = layer == 0 ? 10 : 8;
                for (int i = 0; i < nf; i++) {
                    float t = i / (float) (nf - 1);
                    float an = 0.05f + 1.15f * t * t;
                    float len = (layer == 0 ? 0.36f + 0.2f * t : 0.2f + 0.08f * t);
                    float bx = sx * (0.06f + 0.36f * t), bz = -0.01f - layer * 0.02f;
                    plume(b, bx, -0.006f * layer, bz, sx * (float) Math.sin(an) * len, -0.012f - 0.02f * layer, -(float) Math.cos(an) * len, 0.04f + 0.006f * (1 - t), 0.012f,
                            layer == 0 ? rgb(138, 102, 66) : rgb(150, 112, 74), layer == 0 ? rgb(58, 42, 30) : rgb(100, 72, 48));
                }
            }
            owlWing[k] = fin(b, Img.FOE_CARD, 0.15f, 20f);
            owlWing[k].cutout = true;
        }
    }

    // ------------------------------------------------------------------ pinguino

    private void buildPenguin() {
        final float[] slate = rgb(30, 34, 44), white = rgb(244, 244, 240), yel = rgb(244, 178, 48), pale = rgb(250, 226, 150);
        MeshBuilder b = new MeshBuilder();
        float[][] c = {{0, .06f, -.04f}, {0, .16f, -.03f}, {0, .32f, 0f}, {0, .50f, .01f}, {0, .66f, .02f}, {0, .78f, .05f}, {0, .88f, .07f}, {0, .96f, .08f}, {0, 1.0f, .08f}};
        float[] rx = {0, .16f, .235f, .255f, .22f, .15f, .12f, .09f, 0}, rz = {0, .14f, .20f, .22f, .19f, .13f, .115f, .09f, 0};
        bodyY(b, c, rx, rz, 24, new Paint() {
            @Override void col(float t, float an, float x, float y, float z, float[] o) {
                float s = (float) Math.sin(an);
                float front = sm((s + 0.12f) / 0.3f), head = sm((y - 0.80f) / 0.05f);
                mix(slate, white, front * (1f - head), o);
                float neck = sm((y - 0.58f) / 0.12f) * (1f - sm((y - 0.92f) / 0.03f)) * (0.35f + 0.65f * front);
                float[] n = new float[3];
                mix(pale, yel, sm((y - 0.7f) / 0.15f), n);
                mix(o, n, neck * (1f - 0.7f * head), o);
                float ear = head * sm((0.55f - Math.abs(s)) * 4f) * (1f - front) * sm(((float) Math.cos(an) * (float) Math.cos(an) - 0.5f) * 6f);
                mix(o, yel, ear * 0.9f, o);
            }
        }, 4f, 4f);
        pengBody = fin(b, Img.FOE_DOWN, 0.35f, 26f);

        b = new MeshBuilder();
        // becco lungo e sottile, piedi palmati, coda corta
        loft(b, new float[][] {{0, .915f, .15f}, {0, .905f, .24f}, {0, .885f, .32f}, {0, .86f, .37f}}, new float[] {.032f, .026f, .016f, 0}, new float[] {.035f, .03f, .02f, 0},
                0, 1, 0, 10, 0.5f, ud(rgb(240, 134, 80), rgb(28, 28, 32)), 0, 0);
        for (int s = -1; s <= 1; s += 2) {
            ballZ(b, s * .1f, .022f, .08f, .075f, .022f, .12f, 10, 6, flat(rgb(58, 52, 52)), 1f, 1f);
            for (int k = -1; k <= 1; k++) tube(b, s * .1f + k * .04f, .02f, .15f, s * .1f + k * .05f, .012f, .21f, .012f, .006f, 5, flat(rgb(30, 28, 28)));
        }
        tube(b, new float[][] {{0, .08f, -.12f}, {0, .05f, -.22f}, {0, .03f, -.29f}}, new float[] {.05f, .035f, .006f}, 6, flat(slate));
        pengBits = fin(b, Img.FOE_DOWN, 0.45f, 28f);

        b = new MeshBuilder();
        for (int s = -1; s <= 1; s += 2) bead(b, s * .085f, .935f, .115f, .014f, null);
        pengGloss = fin(b, -1, 0.9f, 48f);

        for (int k = 0; k < 2; k++) {
            int sx = k == 0 ? 1 : -1;
            b = new MeshBuilder();
            loft(b, new float[][] {{0, 0, 0}, {sx * .03f, -.14f, 0}, {sx * .05f, -.29f, .02f}, {sx * .05f, -.35f, .03f}}, new float[] {.02f, .017f, .014f, 0}, new float[] {.05f, .06f, .04f, 0},
                    0, 0, 1, 10, 1f, ud(rgb(70, 74, 86), slate), 0, 0);
            pengWing[k] = fin(b, Img.FOE_DOWN, 0.4f, 26f);
        }
    }

    // ------------------------------------------------------------------ pupazzo di neve

    private void buildSnowman() {
        Paint snow = ud(rgb(214, 226, 240), rgb(250, 252, 255));
        MeshBuilder b = new MeshBuilder();
        lump(b, 0, 0.36f, 0, 0.38f, 0.9f, 1, snow, 3f, 3f);
        lump(b, 0.01f, 0.84f, 0.01f, 0.28f, 0.92f, 2, snow, 2.5f, 2.5f);
        lump(b, 0, 1.22f, 0.02f, 0.21f, 0.95f, 3, snow, 2f, 2f);
        snowBody = fin(b, Img.FOE_SNOW, 0.35f, 22f);

        b = new MeshBuilder();
        // sciarpa di lana a maglia e un lembo che pende
        ballY(b, 0, 1.02f, 0.02f, 0.235f, 0.055f, 0.235f, 18, 7, flat(rgb(172, 30, 34)), 5f, 1f);
        loft(b, new float[][] {{.13f, 1.02f, .2f}, {.15f, .92f, .24f}, {.15f, .8f, .26f}, {.14f, .72f, .26f}}, new float[] {.055f, .05f, .05f, .05f}, new float[] {.02f, .02f, .02f, .02f},
                0, 0, 1, 8, 1f, flat(rgb(180, 36, 38)), 3f, 3f);
        snowScarf = fin(b, Img.FOE_FUR, 0.05f, 6f);

        b = new MeshBuilder();
        // occhi e bottoni di carbone, bocca di sassolini, carota, cappello di feltro
        Paint coal = flat(rgb(26, 24, 24));
        for (int s = -1; s <= 1; s += 2) lump(b, s * .08f, 1.25f, .185f, .03f, 0.8f, 10 + s, coal, 1f, 1f);
        for (int i = 0; i < 5; i++) {
            float x = (i - 2) * .05f;
            lump(b, x, 1.11f - 0.012f * (2 - Math.abs(i - 2)) * -1f - 0.02f, .2f, .016f, 0.8f, 20 + i, coal, 1f, 1f);
        }
        for (int i = 0; i < 3; i++) lump(b, 0, .66f - i * .0f + i * -.14f, .27f - i * .008f, .028f, 0.8f, 30 + i, coal, 1f, 1f);
        loft(b, new float[][] {{0, 1.19f, .2f}, {0, 1.175f, .3f}, {0, 1.15f, .4f}, {0, 1.13f, .46f}}, new float[] {.04f, .034f, .02f, 0}, new float[] {.04f, .034f, .02f, 0},
                0, 1, 0, 10, 1f, along(rgb(238, 128, 26), rgb(190, 70, 12)), 0, 0);
        ballY(b, 0, 1.36f, 0.02f, 0.27f, 0.025f, 0.27f, 18, 5, flat(rgb(36, 34, 38)), 1f, 1f);
        loft(b, new float[][] {{0, 1.36f, .02f}, {0, 1.5f, .02f}, {0, 1.6f, .02f}}, new float[] {.15f, .145f, .14f}, new float[] {.15f, .145f, .14f}, 0, 0, 1, 18, 1f,
                flat(rgb(40, 38, 42)), 1f, 1f);
        ballY(b, 0, 1.4f, 0.02f, 0.158f, 0.035f, 0.158f, 18, 5, flat(rgb(120, 24, 30)), 1f, 1f);
        snowBits = fin(b, Img.FOE_FUR, 0.1f, 10f);

        b = new MeshBuilder();
        Paint bark = along(rgb(88, 64, 44), rgb(60, 44, 32));
        for (int s = -1; s <= 1; s += 2) {
            tube(b, new float[][] {{s * .27f, .86f, 0f}, {s * .45f, 1.0f, .02f}, {s * .66f, 1.14f, .05f}}, new float[] {.022f, .016f, .008f}, 6, bark);
            tube(b, s * .5f, 1.04f, .03f, s * .58f, 1.2f, .04f, .011f, .004f, 5, bark);
            tube(b, s * .56f, 1.09f, .04f, s * .7f, 1.07f, .05f, .01f, .004f, 5, bark);
        }
        snowTwigs = fin(b, Img.R3_BARK, 0.1f, 8f);

        b = new MeshBuilder();
        lump(b, 0, 0, 0, 0.17f, 1f, 40, snow, 2f, 2f);
        snowball = fin(b, Img.FOE_SNOW, 0.3f, 20f);
    }

    // ------------------------------------------------------------------ bomba di lava

    private void buildLapillo() {
        MeshBuilder b = new MeshBuilder();
        lump(b, 0, 0, 0, 0.36f, 0.95f, 50, flat(rgb(255, 255, 255)), 2f, 2f);
        for (int i = 0; i < 4; i++) {
            float an = i * 1.7f + 0.4f;
            lump(b, (float) Math.cos(an) * .3f, 0.1f * (i % 2) - 0.05f, (float) Math.sin(an) * .3f, 0.1f, 0.9f, 60 + i, flat(rgb(255, 255, 255)), 1.5f, 1.5f);
        }
        lapBody = fin(b, Img.FOE_EMBER, 0.15f, 8f);
    }

    // ------------------------------------------------------------------ salamandra pezzata

    private void buildSalamander() {
        final float[] blk = rgb(16, 16, 20), yel = rgb(248, 200, 22), under = rgb(64, 62, 66);
        MeshBuilder b = new MeshBuilder();
        float[][] c = {{0, .06f, -.98f}, {0, .09f, -.80f}, {0, .14f, -.55f}, {0, .18f, -.30f}, {0, .20f, -.05f}, {0, .20f, .18f}, {0, .20f, .36f}, {0, .21f, .50f}, {0, .20f, .60f}, {0, .19f, .68f}};
        float[] rx = {0, .045f, .09f, .13f, .16f, .165f, .15f, .16f, .095f, 0}, ry = {0, .035f, .07f, .10f, .115f, .115f, .105f, .11f, .08f, 0};
        loft(b, c, rx, ry, 0, 1, 0, 20, 0.55f, new Paint() {
            @Override void col(float t, float an, float x, float y, float z, float[] o) {
                float s = (float) Math.sin(an);
                mix(under, blk, sm((s + 0.5f) * 3f), o);
                float f = (float) Math.sin(z * 21f) * 0.6f + (float) Math.sin(an * 2f + z * 7f) * 0.6f + 0.5f * (h((int) (z * 9f), 3f) - 0.5f);
                float k = sm((f - 0.45f) * 6f) * sm((s + 0.1f) * 5f) * sm((z + 0.95f) * 8f);
                mix(o, yel, k, o);
            }
        }, 3f, 4f);
        // zampe con dita, ghiandole parotoidi gialle dietro gli occhi
        Paint leg = ud(rgb(40, 40, 44), blk);
        for (int s = -1; s <= 1; s += 2) {
            for (int p = 0; p < 2; p++) {
                float z0 = p == 0 ? 0.28f : -0.30f;
                tube(b, new float[][] {{s * .14f, .16f, z0}, {s * .2f, .15f, z0 + .005f}, {s * .26f, .12f, z0 + .015f}, {s * .3f, .07f, z0 + .05f}, {s * .32f, .03f, z0 + .08f}}, new float[] {.055f, .05f, .044f, .038f, .034f}, 9, leg);
                for (int k = -1; k <= 2; k++) tube(b, s * .32f, .02f, z0 + .08f, s * (.33f + k * .015f), .01f, z0 + .17f, .014f, .006f, 5, leg);
                ball(b, s * .22f, .17f, z0 + .02f, .03f, .02f, .03f, rgb(214, 170, 20));
            }
            ballZ(b, s * .11f, .27f, .38f, .06f, .05f, .11f, 10, 7, flat(rgb(236, 168, 24)), 1f, 1f);
        }
        salaBody = fin(b, Img.FOE_SKIN, 0.7f, 40f);

        b = new MeshBuilder();
        for (int s = -1; s <= 1; s += 2) bead(b, s * .1f, .27f, .54f, .034f, null);
        salaGloss = fin(b, -1, 0.95f, 52f);

        b = new MeshBuilder();
        // fiamma: tre strati a goccia ondulati, dal rosso al giallo bianco
        float[][] fc = new float[8][3];
        float[] fr = new float[8], fr2 = new float[8];
        for (int layer = 0; layer < 3; layer++) {
            for (int i = 0; i < 8; i++) {
                float t = i / 7f;
                fc[i] = new float[] {0.03f * (float) Math.sin(t * 9f + layer), 0.06f * (float) Math.sin(t * 7f + layer * 2f) + 0.14f * t, 0.1f + t * (1.5f - 0.35f * layer)};
                fr[i] = (0.2f - 0.055f * layer) * (float) Math.sin(Math.min(PI, t * PI * 0.95f + 0.25f)) * (1f - 0.15f * t);
            }
            fr[7] = 0;
            float[] cc = layer == 0 ? rgb(255, 78, 14) : layer == 1 ? rgb(255, 156, 36) : rgb(255, 232, 150);
            loft(b, fc, fr, fr, 0, 1, 0, 12, 1f, flat(cc), 0, 0);
        }
        flame = b.build(-1, true, true);
    }

    // ------------------------------------------------------------------ raganella

    private void buildFrog() {
        final float[] grn = rgb(82, 134, 44), dgr = rgb(46, 84, 30), bel = rgb(232, 228, 192);
        MeshBuilder b = new MeshBuilder();
        float[][] c = {{0, .14f, -.40f}, {0, .19f, -.32f}, {0, .23f, -.15f}, {0, .25f, .02f}, {0, .24f, .18f}, {0, .22f, .30f}, {0, .19f, .40f}, {0, .18f, .46f}};
        float[] rx = {0, .15f, .24f, .26f, .20f, .15f, .09f, 0}, ry = {0, .11f, .17f, .18f, .15f, .12f, .08f, 0};
        loft(b, c, rx, ry, 0, 1, 0, 22, 0.6f, new Paint() {
            @Override void col(float t, float an, float x, float y, float z, float[] o) {
                float s = (float) Math.sin(an);
                float blot = sm(((float) Math.sin(x * 19f + z * 13f) * (float) Math.sin(z * 17f - x * 9f) - 0.15f) * 5f);
                mix(grn, dgr, 0.7f * blot * sm(s * 3f), o);
                mix(o, bel, sm((-s - 0.1f) * 4f), o);
            }
        }, 3f, 3f);
        // zampe anteriori con quattro dita, tondo del timpano, bocca
        Paint lg = ud(rgb(190, 196, 150), grn);
        for (int s = -1; s <= 1; s += 2) {
            tube(b, new float[][] {{s * .15f, .15f, .24f}, {s * .2f, .09f, .30f}, {s * .2f, .02f, .34f}}, new float[] {.04f, .03f, .024f}, 7, lg);
            for (int k = -1; k <= 2; k++) tube(b, s * .2f, .016f, .34f, s * (.2f + k * .03f * 1.0f), .01f, .43f, .012f, .007f, 5, lg);
            ballZ(b, s * .145f, .285f, .125f, .04f, .04f, .012f, 8, 6, flat(rgb(70, 104, 46)), 1f, 1f);
            tube(b, new float[][] {{s * .09f, .205f, .44f}, {s * .15f, .19f, .30f}, {s * .17f, .2f, .16f}}, new float[] {.005f, .005f, .005f}, 4, flat(rgb(50, 44, 30)));
        }
        frogBody = fin(b, Img.FOE_SKIN, 0.6f, 34f);

        b = new MeshBuilder();
        // occhi sporgenti con iride dorata e pupilla orizzontale
        for (int s = -1; s <= 1; s += 2) {
            ball(b, s * .12f, .325f, .215f, .062f, .06f, .06f, rgb(76, 122, 42));
            ball(b, s * .13f, .33f, .255f, .048f, .048f, .03f, rgb(212, 164, 40));
            ball(b, s * .134f, .33f, .278f, .036f, .011f, .012f, rgb(8, 8, 10));
            ball(b, s * .122f, .345f, .285f, .008f, .008f, .005f, rgb(255, 255, 255));
            ball(b, s * .035f, .205f, .46f, .008f, .006f, .008f, rgb(26, 22, 18));
        }
        frogGloss = fin(b, Img.FOE_SKIN, 0.95f, 52f);

        for (int k = 0; k < 2; k++) {
            int sx = k == 0 ? 1 : -1;
            b = new MeshBuilder();
            Paint thigh = ud(rgb(214, 214, 168), grn);
            tube(b, new float[][] {{sx * .17f, .2f, -.30f}, {sx * .30f, .24f, -.14f}, {sx * .33f, .19f, .0f}}, new float[] {.09f, .08f, .06f}, 10, thigh);
            tube(b, new float[][] {{sx * .33f, .19f, .0f}, {sx * .34f, .1f, -.2f}, {sx * .30f, .045f, -.34f}}, new float[] {.06f, .05f, .036f}, 8, ud(rgb(190, 196, 150), grn));
            tube(b, new float[][] {{sx * .30f, .045f, -.34f}, {sx * .31f, .025f, -.18f}, {sx * .31f, .015f, -.02f}}, new float[] {.03f, .024f, .018f}, 7, flat(rgb(176, 184, 136)));
            for (int t = -1; t <= 2; t++) {
                tube(b, sx * .31f, .018f, -.02f, sx * (.31f + t * .045f), .008f, .13f, .012f, .006f, 5, flat(rgb(170, 178, 130)));
                ballZ(b, sx * (.31f + t * .022f), .008f, .05f, .024f, .004f, .06f, 6, 4, flat(rgb(150, 160, 110)), 1f, 1f);
            }
            frogLeg[k] = fin(b, Img.FOE_SKIN, 0.55f, 30f);
        }
    }

    // ------------------------------------------------------------------ dionea

    private Mesh lobe(boolean upper) {
        MeshBuilder b = new MeshBuilder();
        int rings = 12;
        float[][] c = new float[rings + 1][];
        float[] rx = new float[rings + 1], ry = new float[rings + 1];
        for (int i = 0; i <= rings; i++) {
            float t = i / (float) rings;
            c[i] = new float[] {0, 0.0f, 0.6f * t};
            float e = (float) Math.sin(Math.min(PI, t * PI * 1.02f));
            rx[i] = 0.30f * (float) Math.pow(e, 0.8f);
            ry[i] = 0.17f * (float) Math.pow(e, 0.9f);
        }
        rx[0] = rx[rings] = ry[0] = ry[rings] = 0;
        loft(b, c, rx, ry, 0, 1, 0, 22, 0.1f, new Paint() {
            @Override void col(float t, float an, float x, float y, float z, float[] o) {
                float s = (float) Math.sin(an);
                float[] out = new float[3];
                mix(rgb(88, 142, 40), rgb(150, 190, 66), 0.5f + 0.5f * (float) Math.sin(t * 7f), out);
                mix(rgb(168, 26, 36), rgb(122, 14, 28), t, o);
                mix(o, out, sm((s + 0.05f) * 6f), o);
            }
        }, 2f, 2f);
        // ciglia sul bordo: spine lunghe che si incastrano con quelle dell'altra metà
        for (int i = 1; i < rings; i++) {
            for (int sd = -1; sd <= 1; sd += 2) {
                float t = i / (float) rings, e = (float) Math.sin(t * PI);
                float x = sd * 0.30f * (float) Math.pow(e, 0.8f), z = 0.6f * t;
                quill(b, x, 0.0f, z, sd * 0.35f, -0.9f, 0.1f * (t - 0.5f) * 2f, 0.13f, 0.013f, rgb(110, 150, 50), rgb(150, 176, 70), rgb(226, 214, 120));
            }
        }
        quill(b, 0, 0, 0.6f, 0, -0.5f, 1f, 0.11f, 0.013f, rgb(110, 150, 50), rgb(150, 176, 70), rgb(226, 214, 120));
        return fin(b, Img.FOE_PETAL, 0.4f, 28f);
    }

    private void buildPlant() {
        MeshBuilder b = new MeshBuilder();
        Paint st = along(rgb(88, 140, 40), rgb(140, 184, 62));
        tube(b, new float[][] {{0, 0, 0}, {.02f, .22f, -.03f}, {0, .45f, -.08f}, {0, .62f, -.16f}}, new float[] {.06f, .045f, .04f, .042f}, 10, st);
        for (int i = 0; i < 5; i++) {                                 // rosetta di foglie a spatola attorno alla base
            float an = i * 1.2566f + 0.3f, ca = (float) Math.cos(an), sa = (float) Math.sin(an);
            loft(b, new float[][] {{0, .02f, 0}, {ca * .16f, .1f, sa * .16f}, {ca * .32f, .12f, sa * .32f}, {ca * .42f, .06f, sa * .42f}}, new float[] {.02f, .045f, .07f, 0},
                    new float[] {.01f, .012f, .012f, 0}, 0, 1, 0, 8, 1f, along(rgb(70, 124, 36), rgb(120, 170, 54)), 1f, 1f);
        }
        plantStem = fin(b, Img.FOE_PETAL, 0.35f, 24f);
        plantTop = lobe(true);
        plantBottom = lobe(false);
    }

    // ------------------------------------------------------------------ cielo

    private static void tri(MeshBuilder b, float[] p, float[] q, float[] r, float[] col) {
        float ux = q[0] - p[0], uy = q[1] - p[1], uz = q[2] - p[2], vx = r[0] - p[0], vy = r[1] - p[1], vz = r[2] - p[2];
        float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
        float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        nx /= l; ny /= l; nz /= l;
        b.colorf(col[0], col[1], col[2]);
        b.vert(p[0], p[1], p[2], nx, ny, nz, 0.5f, 0.5f);
        b.vert(q[0], q[1], q[2], nx, ny, nz, 0.5f, 0.5f);
        b.vert(r[0], r[1], r[2], nx, ny, nz, 0.5f, 0.5f);
    }

    private void buildSky() {
        // cristallo stellare: stella a cinque punte sfaccettata, ambra e oro
        MeshBuilder b = new MeshBuilder();
        float[] fr = {0, 0, 0.15f}, bk = {0, 0, -0.15f};
        float[][] rim = new float[10][3];
        for (int k = 0; k < 10; k++) {
            double an = PI / 2 + k * PI / 5;
            float rr = k % 2 == 0 ? 0.46f : 0.2f;
            rim[k] = new float[] {(float) Math.cos(an) * rr, (float) Math.sin(an) * rr, 0f};
        }
        for (int k = 0; k < 10; k++) {
            float[] p = rim[k], q = rim[(k + 1) % 10];
            float t1 = 0.78f + 0.2f * h(k, 1), t2 = 0.66f + 0.25f * h(k, 2);
            tri(b, fr, p, q, new float[] {1f * t1, 0.78f * t1, 0.2f * t1});
            tri(b, bk, q, p, new float[] {1f * t2, 0.74f * t2, 0.16f * t2});
        }
        starBody = fin(b, -1, 0.95f, 60f);

        // sole di plasma: sfera granulosa, protuberanze ad arco, alone trasparente
        b = new MeshBuilder();
        ballY(b, 0, 0, 0, 0.34f, 0.34f, 0.34f, 22, 16, flat(rgb(255, 255, 255)), 2f, 1.4f);
        sunBody = fin(b, Img.FOE_PLASMA, 0f, 8f);

        b = new MeshBuilder();
        int np = 16;
        for (int i = 0; i < np; i++) {
            float phi = i * 2f * PI / np + 0.1f * h(i, 3);
            float len = (i % 2 == 0 ? 0.3f : 0.18f) * (0.85f + 0.3f * h(i, 7));
            float ca = (float) Math.cos(phi), sa = (float) Math.sin(phi);
            quill(b, ca * 0.3f, sa * 0.3f, 0, ca - sa * 0.35f, sa + ca * 0.35f, 0, len, 0.045f, rgb(255, 120, 20), rgb(255, 170, 50), rgb(255, 232, 140));
        }
        sunProm = fin(b, Img.FOE_PLASMA, 0f, 8f);

        b = new MeshBuilder();
        ballY(b, 0, 0, 0, 0.5f, 0.5f, 0.5f, 20, 12, flat(rgb(255, 190, 70)), 1f, 1f);
        ballY(b, 0, 0, 0, 0.66f, 0.66f, 0.66f, 20, 12, flat(rgb(255, 170, 50)), 1f, 1f);
        ballY(b, 0, 0, 0, 0.86f, 0.86f, 0.86f, 20, 12, flat(rgb(255, 150, 40)), 1f, 1f);
        sunHalo = b.build(-1, true, true);
    }

    // ------------------------------------------------------------------ disegno

    void drawShot(Gfx3D g, float[] m, float[] s) {
        System.arraycopy(m, 0, a, 0, 16);
        Mat4.translate(a, s[0], s[1], s[2]);
        g.draw(snowball, a, 1, 1, 1, 1, 0.1f);
    }

    void draw(Gfx3D g, float[] m, Enemy3 e) {
        float an = e.anim;
        switch (e.type) {
            case Enemy3.MOLE: {
                g.draw(mound, m, 1, 1, 1, 1, 0);
                if (e.mode == 0) return;
                float up = e.mode == 1 ? 0.25f + 0.05f * (float) Math.sin(an * 50f) : 1f;
                System.arraycopy(m, 0, a, 0, 16);
                Mat4.translate(a, 0, -0.5f + 0.5f * up, 0);
                g.draw(moleBody, a, 1, 1, 1, 1, 0);
                g.draw(moleBits, a, 1, 1, 1, 1, 0);
                g.draw(moleGloss, a, 1, 1, 1, 1, 0);
                return;
            }
            case Enemy3.SNAIL: {
                boolean shell = e.mode == 1;
                System.arraycopy(m, 0, a, 0, 16);
                if (shell) {
                    Mat4.translate(a, 0, -0.1f, 0);
                    Mat4.scale(a, 0.85f, 0.85f, 0.85f);
                } else {
                    Mat4.scale(a, 1f + 0.04f * (float) Math.sin(an * 3f), 1f, 1f + 0.03f * (float) Math.sin(an * 3f + 1f));
                    g.draw(snailBody, a, 1, 1, 1, 1, 0);
                    g.draw(snailGloss, a, 1, 1, 1, 1, 0);
                }
                g.draw(snailShell, a, 1, 1, 1, 1, 0);
                return;
            }
            case Enemy3.CHESTNUT: {
                System.arraycopy(m, 0, a, 0, 16);
                Mat4.translate(a, 0, -0.35f, 0);
                if (e.mode == 1) Mat4.translate(a, 0.04f * (float) Math.sin(an * 60f), 0, 0);
                if (e.mode == 3) {
                    Mat4.translate(a, 0, 0.35f, 0);
                    Mat4.rotateX(a, an * 9f * e.dir);
                    Mat4.translate(a, 0, -0.35f, 0);
                }
                g.draw(chestBody, a, 1, 1, 1, 1, 0);
                g.draw(chestSpines, a, 1, 1, 1, 1, 0);
                g.draw(chestNut, a, 1, 1, 1, 1, 0);
                return;
            }
            case Enemy3.OWL: {
                System.arraycopy(m, 0, a, 0, 16);
                Mat4.translate(a, 0, -0.1f, 0);
                if (e.mode == 1) Mat4.rotateX(a, 0.6f);
                g.draw(owlBody, a, 1, 1, 1, 1, 0);
                g.draw(owlBits, a, 1, 1, 1, 1, 0);
                g.draw(owlGloss, a, 1, 1, 1, 1, 0.3f);
                float flap = 0.7f * (float) Math.sin(an * (e.mode == 0 ? 7f : 14f));
                for (int k = 0; k < 2; k++) {
                    int sx = k == 0 ? 1 : -1;
                    System.arraycopy(a, 0, b2, 0, 16);
                    Mat4.translate(b2, sx * 0.2f, 0.52f, 0.0f);
                    Mat4.rotateZ(b2, flap * sx);
                    g.draw(owlWing[k], b2, 1, 1, 1, 1, 0);
                }
                return;
            }
            case Enemy3.PENGUIN: {
                System.arraycopy(m, 0, a, 0, 16);
                float w = (float) Math.sin(an * 18f);
                Mat4.rotateZ(a, 0.1f * w);
                g.draw(pengBody, a, 1, 1, 1, 1, 0);
                g.draw(pengBits, a, 1, 1, 1, 1, 0);
                g.draw(pengGloss, a, 1, 1, 1, 1, 0.2f);
                for (int k = 0; k < 2; k++) {
                    int sx = k == 0 ? 1 : -1;
                    System.arraycopy(a, 0, b2, 0, 16);
                    Mat4.translate(b2, sx * 0.25f, 0.68f, 0.0f);
                    Mat4.rotateZ(b2, (0.35f + 0.3f * w) * sx);
                    g.draw(pengWing[k], b2, 1, 1, 1, 1, 0);
                }
                return;
            }
            case Enemy3.SNOWMAN: {
                System.arraycopy(m, 0, a, 0, 16);
                float charge = e.t > 1.8f ? 0.06f * (float) Math.sin(an * 40f) : 0f;
                Mat4.scale(a, 1f + charge, 1f, 1f + charge);
                g.draw(snowBody, a, 1, 1, 1, 1, 0);
                g.draw(snowScarf, a, 1, 1, 1, 1, 0);
                g.draw(snowBits, a, 1, 1, 1, 1, 0);
                g.draw(snowTwigs, a, 1, 1, 1, 1, 0);
                return;
            }
            case Enemy3.LAPILLO: {
                if (e.mode == 0) return;
                System.arraycopy(m, 0, a, 0, 16);
                Mat4.translate(a, 0, 0.36f, 0);
                float f = 1f + 0.05f * (float) Math.sin(an * 30f);
                Mat4.scale(a, f, f, f);
                Mat4.rotateY(a, an * 1.5f);
                g.draw(lapBody, a, 1.15f, 1f, 0.95f, 1, 0.55f + 0.2f * (float) Math.sin(an * 12f));
                return;
            }
            case Enemy3.SALAMANDER: {
                System.arraycopy(m, 0, a, 0, 16);
                float sw = e.mode == 1 ? 1f + 0.1f * (float) Math.sin(an * 30f) : 1f;
                Mat4.scale(a, sw, sw, 1f);
                if (e.mode == 0) Mat4.rotateY(a, 0.1f * (float) Math.sin(an * 8f));
                g.draw(salaBody, a, e.mode == 1 ? 1.3f : 1f, 1f, e.mode == 1 ? 0.8f : 1f, 1, e.mode == 1 ? 0.25f : 0f);
                g.draw(salaGloss, a, 1, 1, 1, 1, 0.2f);
                if (e.mode == 2) {
                    System.arraycopy(m, 0, b2, 0, 16);
                    Mat4.translate(b2, 0, 0.19f, 0.68f);
                    float k = 1f + 0.1f * (float) Math.sin(an * 40f);
                    Mat4.scale(b2, k, k, 1f);
                    g.draw(flame, b2, 1, 1, 1, 0.6f, 1f);
                }
                return;
            }
            case Enemy3.FROG: {
                System.arraycopy(m, 0, a, 0, 16);
                boolean air = e.mode == 1;
                float cr = air ? 1.1f : 0.94f + 0.04f * (float) Math.sin(an * 3f);
                Mat4.scale(a, air ? 0.94f : 1.03f, cr, air ? 1.08f : 1f);
                if (air) Mat4.rotateX(a, -0.35f);
                g.draw(frogBody, a, 1, 1, 1, 1, 0);
                g.draw(frogGloss, a, 1, 1, 1, 1, 0.1f);
                for (int k = 0; k < 2; k++) {
                    System.arraycopy(a, 0, b2, 0, 16);
                    if (air) Mat4.rotateX(b2, 0.8f);
                    g.draw(frogLeg[k], b2, 1, 1, 1, 1, 0);
                }
                return;
            }
            case Enemy3.PLANT: {
                g.draw(plantStem, m, 1, 1, 1, 1, 0);
                float open = e.mode == 2 ? 0.8f : e.mode == 1 ? 0.12f + 0.1f * (float) Math.sin(an * 45f) : 0.04f;
                float sway = -0.25f + 0.1f * (float) Math.sin(an * 2f);
                System.arraycopy(m, 0, a, 0, 16);
                Mat4.translate(a, 0, 0.66f, -0.2f);
                Mat4.rotateX(a, sway);
                System.arraycopy(a, 0, b2, 0, 16);
                Mat4.rotateZ(b2, PI);
                Mat4.rotateX(b2, -open * 0.3f);
                g.draw(plantBottom, b2, 1, 1, 1, 1, 0);
                Mat4.rotateX(a, -open);
                g.draw(plantTop, a, e.mode == 2 ? 1.15f : 1f, 1f, 1f, 1, 0);
                return;
            }
            case Enemy3.STAR: {
                System.arraycopy(m, 0, a, 0, 16);
                Mat4.rotateZ(a, an * 4f);
                Mat4.rotateY(a, 0.35f * (float) Math.sin(an * 2f));
                g.draw(starBody, a, 1f, 0.95f, 0.85f, 1, 0.55f);
                return;
            }
            default: {
                System.arraycopy(m, 0, a, 0, 16);
                Mat4.translate(a, 0, 0.3f, 0);
                g.draw(sunBody, a, 1, 1, 1, 1, 1f);
                System.arraycopy(a, 0, b2, 0, 16);
                Mat4.rotateZ(b2, an * 1.2f);
                g.draw(sunProm, b2, 1, 1, 1, 1, 1f);
                float pulse = 0.09f + 0.02f * (float) Math.sin(an * 5f);
                if (e.mode != 3) g.draw(sunHalo, a, 1, 1, 1, pulse, 1f);
            }
        }
    }
}
