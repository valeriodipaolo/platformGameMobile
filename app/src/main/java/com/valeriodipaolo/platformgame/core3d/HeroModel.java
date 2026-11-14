package com.valeriodipaolo.platformgame.core3d;

import com.valeriodipaolo.platformgame.core.Img;

/**
 * Il protagonista: un robot viandante con veste bianca, cintura, zaino di cuoio sulle spalle e mantello bianco che
 * fluttua. Guarda verso z positivo (la camera lo vede di spalle); i piedi stanno a y = 0.
 * Il mantello è una griglia di stoffa ricalcolata a ogni fotogramma: si alza quando il robot corre o cade, si apre a
 * ruota nella giravolta e ondeggia sempre un poco.
 */
public final class HeroModel {
    private static final float PI = (float) Math.PI;
    private static final int CC = 8, CR = 9;                    // colonne e righe di quadrati del mantello
    private static final float CAPE_LEN = 0.66f;

    private final Mesh robe, gear, glow, arm, leg, cape;
    private final float[] m2 = new float[16];
    private final float[][][] cp = new float[CR + 1][CC + 1][3], cn = new float[CR + 1][CC + 1][3];
    private float lift, flare, lastTime = -1;

    public HeroModel() {
        robe = buildRobe();
        gear = buildGear();
        glow = buildGlow();
        arm = buildArm();
        leg = buildLeg();
        cape = new Mesh(new float[CC * CR * 6 * 2 * Mesh.STRIDE], CC * CR * 6 * 2, Img.R3_CLOTH, false, true);
        cape.dynamic = true;
        cape.spec = 0.06f; cape.shine = 12f;
    }

    private static float[] rgb(int r, int g, int b) {
        return FoeModels.rgb(r, g, b);
    }

    // ------------------------------------------------------------------ pezzi fissi

    /** Veste bianca: tunica stretta in vita che si apre in una gonna a pieghe con l'orlo dorato, colletto arrotolato. */
    private static Mesh buildRobe() {
        MeshBuilder b = new MeshBuilder();
        final float[] white = rgb(250, 250, 252), shade = rgb(196, 202, 216), gold = rgb(236, 180, 62), blue = rgb(70, 120, 190);
        float[] ys = {0.300f, 0.318f, 0.336f, 0.40f, 0.485f, 0.56f, 0.66f, 0.76f, 0.83f, 0.875f};
        float[] rx = {0.262f, 0.260f, 0.256f, 0.232f, 0.186f, 0.212f, 0.238f, 0.246f, 0.205f, 0.120f};
        float[] rz = {0.236f, 0.234f, 0.230f, 0.206f, 0.160f, 0.178f, 0.196f, 0.192f, 0.165f, 0.110f};
        float[][] c = new float[ys.length][];
        for (int i = 0; i < ys.length; i++) c[i] = new float[] {0, ys[i], 0};
        FoeModels.loft(b, c, rx, rz, 0, 0, 1, 20, 1f, new FoeModels.Paint() {
            @Override void col(float t, float a, float x, float y, float z, float[] o) {
                if (y < 0.317f) { o[0] = gold[0]; o[1] = gold[1]; o[2] = gold[2]; return; }
                if (y < 0.337f) { o[0] = blue[0]; o[1] = blue[1]; o[2] = blue[2]; return; }
                float pleat = 0.5f + 0.5f * (float) Math.cos(a * 10f);
                float k = (1f - FoeModels.sm((y - 0.34f) / 0.16f)) * (1f - pleat) * 0.75f;
                k = Math.max(k, 0.5f * (1f - FoeModels.sm(Math.abs(y - 0.485f) / 0.06f)));          // ombra dove la cintura stringe
                FoeModels.mix(white, shade, k, o);
            }
        }, 4f, 6f);
        // colletto arrotolato attorno al collo
        float[][] cc = {{0, 0.855f, 0}, {0, 0.875f, 0}, {0, 0.905f, 0}, {0, 0.93f, 0}, {0, 0.94f, 0}};
        FoeModels.loft(b, cc, new float[] {0.13f, 0.168f, 0.172f, 0.14f, 0.08f}, new float[] {0.12f, 0.152f, 0.156f, 0.126f, 0.075f}, 0, 0, 1, 16, 1f,
                FoeModels.along(shade, white), 3f, 6f);
        Mesh m = b.build(Img.R3_CLOTH, false, true);
        m.spec = 0.05f; m.shine = 10f;
        return m;
    }

    /** Testa, cintura, zaino con coperta arrotolata e cinghie: tutto ciò che non è stoffa. */
    private static Mesh buildGear() {
        MeshBuilder b = new MeshBuilder();
        final float[] shell = rgb(240, 242, 246), shellLo = rgb(188, 194, 208), orange = rgb(240, 124, 30), dark = rgb(46, 50, 60);
        final float[] metal = rgb(150, 156, 168), gold = rgb(240, 186, 60);
        final float[] hide = rgb(156, 96, 50), hideDk = rgb(112, 66, 34), hideLt = rgb(196, 134, 78), blanket = rgb(52, 104, 176), blanketLt = rgb(96, 150, 214);

        // sotto la gonna: fondo scuro, così non si vede dentro
        Decor.disc(b, 0, 0.306f, 0, 0, -1, 0, 0.235f, 14, dark, dark);
        // collo
        FoeModels.tube(b, 0, 0.9f, 0, 0, 1.0f, 0, 0.07f, 0.06f, 8, FoeModels.flat(dark));
        // testa: casco bianco con una cresta arancione e il bordo inferiore più scuro
        FoeModels.ballY(b, 0, 1.08f, 0, 0.33f, 0.25f, 0.29f, 20, 12, new FoeModels.Paint() {
            @Override void col(float t, float a, float x, float y, float z, float[] o) {
                if (Math.abs(x) < 0.034f && y > 1.1f && z < 0.2f) { o[0] = orange[0]; o[1] = orange[1]; o[2] = orange[2]; return; }
                FoeModels.mix(shellLo, shell, FoeModels.sm((y - 0.86f) / 0.2f), o);
            }
        }, 0, 0);
        b.colorf(0.07f, 0.09f, 0.14f);
        b.ellipsoid(0, 1.07f, 0.13f, 0.25f, 0.13f, 0.2f, 16, 9);                             // visiera
        for (int sd = -1; sd <= 1; sd += 2) {                                                // auricolari
            FoeModels.ballZ(b, sd * 0.318f, 1.07f, 0, 0.05f, 0.085f, 0.085f, 10, 6, FoeModels.flat(orange), 0, 0);
            FoeModels.ballZ(b, sd * 0.352f, 1.07f, 0, 0.026f, 0.04f, 0.04f, 8, 5, FoeModels.flat(metal), 0, 0);
        }
        b.colorf(dark[0], dark[1], dark[2]);
        b.roundBox(0, 1.08f, -0.262f, 0.24f, 0.17f, 0.08f, 0.03f);                           // pannello dietro la testa
        FoeModels.tube(b, 0, 1.3f, 0, 0, 1.47f, 0, 0.016f, 0.011f, 6, FoeModels.flat(metal)); // antenna
        FoeModels.ballY(b, 0, 1.325f, 0, 0.04f, 0.022f, 0.04f, 8, 4, FoeModels.flat(metal), 0, 0);

        // cintura di cuoio con fibbia dorata e borsello
        float[][] bc = {{0, 0.455f, 0}, {0, 0.465f, 0}, {0, 0.505f, 0}, {0, 0.515f, 0}};
        FoeModels.loft(b, bc, new float[] {0.19f, 0.204f, 0.204f, 0.19f}, new float[] {0.164f, 0.178f, 0.178f, 0.164f}, 0, 0, 1, 18, 1f, FoeModels.flat(hide), 0, 0);
        b.colorf(gold[0], gold[1], gold[2]);
        b.roundBox(0, 0.485f, 0.178f, 0.085f, 0.075f, 0.03f, 0.012f);
        b.colorf(hideDk[0], hideDk[1], hideDk[2]);
        b.roundBox(0.17f, 0.445f, 0.085f, 0.07f, 0.1f, 0.09f, 0.025f);
        b.colorf(gold[0], gold[1], gold[2]);
        b.box(0.205f, 0.455f, 0.085f, 0.012f, 0.025f, 0.03f);

        // zaino: corpo, patta con due cinghie e fibbie, tasche laterali, coperta arrotolata in cima
        FoeModels.Paint leather = new FoeModels.Paint() {
            @Override void col(float t, float a, float x, float y, float z, float[] o) { FoeModels.mix(hideDk, hideLt, FoeModels.sm((y - 0.46f) / 0.42f), o); }
        };
        b.colorf(hide[0], hide[1], hide[2]);
        b.roundBox(0, 0.655f, -0.33f, 0.36f, 0.37f, 0.2f, 0.07f);
        b.colorf(hideDk[0], hideDk[1], hideDk[2]);
        b.roundBox(0, 0.775f, -0.337f, 0.38f, 0.16f, 0.225f, 0.07f);                         // patta
        for (int sd = -1; sd <= 1; sd += 2) {
            b.colorf(hideLt[0], hideLt[1], hideLt[2]);
            b.box(sd * 0.09f, 0.70f, -0.452f, 0.04f, 0.2f, 0.012f);                           // cinghia della patta
            b.colorf(gold[0], gold[1], gold[2]);
            b.roundBox(sd * 0.09f, 0.635f, -0.458f, 0.058f, 0.05f, 0.018f, 0.008f);          // fibbia
            b.colorf(hideDk[0], hideDk[1], hideDk[2]);
            b.roundBox(sd * 0.205f, 0.585f, -0.33f, 0.08f, 0.19f, 0.14f, 0.035f);            // tasca
            b.colorf(hideLt[0], hideLt[1], hideLt[2]);
            b.box(sd * 0.246f, 0.63f, -0.33f, 0.008f, 0.03f, 0.1f);
            // spallacci: dalla cima dello zaino, sopra la spalla, fin sotto il braccio
            FoeModels.loft(b, new float[][] {{sd * 0.125f, 0.82f, -0.25f}, {sd * 0.135f, 0.885f, -0.1f}, {sd * 0.14f, 0.86f, 0.06f},
                            {sd * 0.15f, 0.74f, 0.17f}, {sd * 0.17f, 0.6f, 0.13f}, {sd * 0.19f, 0.52f, -0.06f}, {sd * 0.15f, 0.5f, -0.24f}},
                    new float[] {0.012f, 0.012f, 0.012f, 0.012f, 0.012f, 0.012f, 0.012f}, new float[] {0.03f, 0.032f, 0.032f, 0.03f, 0.028f, 0.026f, 0.026f},
                    1, 0, 0, 6, 1f, leather, 0, 0);
        }
        // coperta arrotolata, legata con due cinghie
        float[][] rc = new float[7][];
        float[] rr = new float[7];
        for (int i = 0; i < 7; i++) {
            float t = i / 6f;
            rc[i] = new float[] {-0.23f + 0.46f * t, 0.905f, -0.335f};
            rr[i] = (i == 0 || i == 6) ? 0.062f : 0.078f;
        }
        FoeModels.loft(b, rc, rr, rr, 0, 1, 0, 10, 1f, new FoeModels.Paint() {
            @Override void col(float t, float a, float x, float y, float z, float[] o) {
                if (Math.abs(Math.abs(x) - 0.11f) < 0.018f) { o[0] = hideDk[0]; o[1] = hideDk[1]; o[2] = hideDk[2]; return; }
                FoeModels.mix(blanket, blanketLt, 0.5f + 0.5f * (float) Math.sin(a * 3f + x * 30f), o);
            }
        }, 0, 0);
        Decor.disc(b, -0.231f, 0.905f, -0.335f, -1, 0, 0, 0.062f, 10, blanketLt, blanket);
        Decor.disc(b, 0.231f, 0.905f, -0.335f, 1, 0, 0, 0.062f, 10, blanketLt, blanket);
        // lanterna appesa al fianco dello zaino (il vetro acceso sta nella mesh luminosa)
        b.colorf(dark[0], dark[1], dark[2]);
        b.box(-0.262f, 0.535f, -0.4f, 0.05f, 0.014f, 0.05f);
        b.box(-0.262f, 0.455f, -0.4f, 0.05f, 0.014f, 0.05f);
        FoeModels.tube(b, -0.262f, 0.54f, -0.4f, -0.25f, 0.62f, -0.38f, 0.006f, 0.006f, 4, FoeModels.flat(metal));
        Mesh m = b.build(-1, false, true);
        m.spec = 0.3f; m.shine = 40f;
        return m;
    }

    /** Parti accese: occhi, gemma del fermaglio, punta dell'antenna, luci dietro il casco, vetro della lanterna. */
    private static Mesh buildGlow() {
        MeshBuilder b = new MeshBuilder().color(120, 236, 255);
        b.ellipsoid(-0.085f, 1.07f, 0.305f, 0.04f, 0.065f, 0.03f, 8, 5);
        b.ellipsoid(0.085f, 1.07f, 0.305f, 0.04f, 0.065f, 0.03f, 8, 5);
        b.bipyramid(0, 0.885f, 0.166f, 0.034f, 0.045f, 6);
        b.box(-0.06f, 1.08f, -0.304f, 0.05f, 0.03f, 0.01f);
        b.box(0.06f, 1.08f, -0.304f, 0.05f, 0.03f, 0.01f);
        b.color(255, 70, 60);
        b.ellipsoid(0, 1.49f, 0, 0.045f, 0.045f, 0.045f, 8, 5);
        b.color(255, 214, 110);
        b.box(-0.262f, 0.495f, -0.4f, 0.04f, 0.066f, 0.04f);
        return b.build(-1, false, true);
    }

    /** Braccio (origine alla spalla): manica bianca a campana con il polsino dorato, avambraccio di metallo, mano. */
    private static Mesh buildArm() {
        MeshBuilder b = new MeshBuilder();
        final float[] white = rgb(248, 248, 252), shade = rgb(200, 206, 220), gold = rgb(236, 180, 62), dark = rgb(60, 64, 76), hand = rgb(216, 220, 230);
        FoeModels.ballY(b, 0, 0, 0, 0.078f, 0.07f, 0.078f, 10, 6, FoeModels.flat(white), 0, 0);
        float[][] c = {{0, 0.02f, 0}, {0, -0.08f, 0}, {0, -0.17f, 0}, {0, -0.205f, 0}, {0, -0.215f, 0}};
        float[] r = {0.062f, 0.07f, 0.09f, 0.1f, 0.1f};
        FoeModels.loft(b, c, r, r, 0, 0, 1, 10, 1f, new FoeModels.Paint() {
            @Override void col(float t, float a, float x, float y, float z, float[] o) {
                if (y < -0.19f) { o[0] = gold[0]; o[1] = gold[1]; o[2] = gold[2]; return; }
                FoeModels.mix(white, shade, 0.35f * (0.5f + 0.5f * (float) Math.cos(a * 5f)), o);
            }
        }, 0, 0);
        Decor.disc(b, 0, -0.213f, 0, 0, -1, 0, 0.096f, 10, dark, dark);
        FoeModels.tube(b, 0, -0.19f, 0, 0, -0.32f, 0, 0.042f, 0.046f, 8, FoeModels.flat(dark));
        FoeModels.ballY(b, 0, -0.345f, 0, 0.06f, 0.062f, 0.06f, 10, 6, FoeModels.flat(hand), 0, 0);
        Mesh m = b.build(-1, false, true);
        m.spec = 0.2f; m.shine = 30f;
        return m;
    }

    /** Gamba (origine all'anca): stinco di metallo, ginocchio, stivale di cuoio con risvolto e suola. */
    private static Mesh buildLeg() {
        MeshBuilder b = new MeshBuilder();
        final float[] dark = rgb(60, 64, 76), joint = rgb(150, 156, 168), hide = rgb(150, 92, 48), hideLt = rgb(204, 146, 88), sole = rgb(40, 34, 30);
        FoeModels.tube(b, 0, 0.02f, 0, 0, -0.24f, 0, 0.052f, 0.046f, 8, FoeModels.flat(dark));
        FoeModels.ballY(b, 0, -0.12f, 0, 0.058f, 0.055f, 0.058f, 8, 5, FoeModels.flat(joint), 0, 0);
        float[][] c = {{0, -0.19f, 0}, {0, -0.205f, 0}, {0, -0.225f, 0}, {0, -0.3f, 0.005f}, {0, -0.345f, 0.01f}};
        float[] r = {0.07f, 0.084f, 0.07f, 0.068f, 0.072f};
        FoeModels.loft(b, c, r, r, 0, 0, 1, 10, 1f, new FoeModels.Paint() {
            @Override void col(float t, float a, float x, float y, float z, float[] o) { FoeModels.mix(hide, hideLt, y > -0.228f ? 1f : 0f, o); }
        }, 0, 0);
        FoeModels.ballZ(b, 0, -0.318f, 0.05f, 0.078f, 0.056f, 0.13f, 10, 7, FoeModels.ud(hide, hideLt), 0, 0);
        b.colorf(sole[0], sole[1], sole[2]);
        b.roundBox(0, -0.362f, 0.05f, 0.15f, 0.03f, 0.262f, 0.014f);
        Mesh m = b.build(-1, false, true);
        m.spec = 0.22f; m.shine = 30f;
        return m;
    }

    // ------------------------------------------------------------------ mantello

    private static float sm(float t) {
        return FoeModels.sm(t);
    }

    /** Ricalcola la stoffa: catena di segmenti per ogni colonna, fissata al colletto e libera sotto lo zaino. */
    private void updateCape(World3D w) {
        boolean first = lastTime < 0 || w.time < lastTime;
        float dt = first ? 1f : Math.min(0.1f, w.time - lastTime);
        lastTime = w.time;
        float sp = (float) Math.sqrt(w.vx * w.vx + w.vz * w.vz);
        float tLift, tFlare = 0.2f;
        if (w.spinning()) { tLift = 1.0f; tFlare = 1.0f; }
        else if (!w.onGround) tLift = Math.max(0.12f, Math.min(1.7f, 0.8f - w.vy * 0.16f));
        else tLift = 0.1f + 1.15f * Math.min(1f, sp / 4.5f);
        float k = first ? 1f : Math.min(1f, dt * 9f);
        lift += (tLift - lift) * k;
        flare += (tFlare - flare) * k;
        float t = w.time, wave = 0.08f + 0.24f * Math.min(1f, lift), seg = CAPE_LEN / CR;
        for (int j = 0; j <= CC; j++) {
            float u = j / (float) CC * 2f - 1f, phi = u * 1.95f;
            float sx = (float) Math.sin(phi), cz = -(float) Math.cos(phi);
            float x = 0.218f * sx, y = 0.868f - 0.035f * Math.abs(u), z = 0.185f * cz - 0.012f;
            // dietro le spalle la stoffa passa sotto lo zaino, sui fianchi è libera da subito
            float back = sm((0.9f - Math.abs(u)) / 0.5f);
            for (int i = 0; i <= CR; i++) {
                float s = i / (float) CR;
                float[] p = cp[i][j];
                p[0] = x; p[1] = y; p[2] = z;
                if (i == CR) break;
                float s1 = (i + 1) / (float) CR;
                float free = sm((s1 - 0.42f * back) / (1f - 0.42f * back) * 1.25f);
                float rip = (float) Math.sin(t * 8.5f - s1 * 5.2f + u * 2.3f) + 0.5f * (float) Math.sin(t * 13.3f - s1 * 8.1f - u * 3.7f + 1.1f);
                float th = free * (lift * (0.55f + 0.45f * back) + wave * rip);
                float fl = (0.1f + 0.75f * flare * free) * (0.5f + 0.5f * s1);          // quanto si allarga a campana
                float dy = -(float) Math.cos(th), dzb = -(float) Math.sin(th);
                float ox = sx * fl + 0.035f * free * (float) Math.sin(t * 6.1f - s1 * 4f + u * 4.4f), oz = cz * fl;
                float nl = (float) Math.sqrt(ox * ox + dy * dy + (oz + dzb) * (oz + dzb));
                x += ox / nl * seg; y += dy / nl * seg; z += (oz + dzb) / nl * seg;
            }
        }
        for (int i = 0; i <= CR; i++) {
            for (int j = 0; j <= CC; j++) {
                float[] a = cp[i][Math.max(j - 1, 0)], b = cp[i][Math.min(j + 1, CC)], c = cp[Math.max(i - 1, 0)][j], d = cp[Math.min(i + 1, CR)][j];
                float ax = b[0] - a[0], ay = b[1] - a[1], az = b[2] - a[2], bx = d[0] - c[0], by = d[1] - c[1], bz = d[2] - c[2];
                float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
                float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (l < 1e-9f) { nx = 0; ny = 0; nz = -1; l = 1; }
                float[] n = cn[i][j];
                n[0] = nx / l; n[1] = ny / l; n[2] = nz / l;
            }
        }
        // i triangoli: lato esterno bianco con l'orlo dorato, fodera azzurra appena sotto
        float[] v = cape.v;
        int o = 0;
        int[] oi = {0, 1, 1, 0, 1, 0}, oj = {0, 0, 1, 0, 1, 1};
        for (int layer = 0; layer < 2; layer++) {
            float sg = layer == 0 ? 1f : -1f, off = layer == 0 ? 0f : -0.007f;
            for (int i = 0; i < CR; i++) {
                for (int j = 0; j < CC; j++) {
                    for (int q = 0; q < 6; q++) {
                        int ii = i + oi[q], jj = j + oj[q];
                        float[] p = cp[ii][jj], n = cn[ii][jj];
                        float fold = 0.5f + 0.5f * (float) Math.sin(jj * 2.4f + ii * 0.35f);
                        float r, g, b;
                        if (layer == 1) { r = 0.36f; g = 0.56f; b = 0.86f; }
                        else if (i == CR - 1) { r = 0.93f; g = 0.71f; b = 0.25f; }
                        else { r = 0.99f - 0.14f * fold; g = 0.99f - 0.12f * fold; b = 1.0f - 0.07f * fold; }
                        v[o] = p[0] + n[0] * off; v[o + 1] = p[1] + n[1] * off; v[o + 2] = p[2] + n[2] * off;
                        v[o + 3] = n[0] * sg; v[o + 4] = n[1] * sg; v[o + 5] = n[2] * sg;
                        v[o + 6] = jj / (float) CC * 5f; v[o + 7] = ii / (float) CR * 4f;
                        v[o + 8] = r; v[o + 9] = g; v[o + 10] = b; v[o + 11] = 0;
                        o += Mesh.STRIDE;
                    }
                }
            }
        }
        cape.version++;
    }

    // ------------------------------------------------------------------ disegno

    /** base = posizione e orientamento del robot nel mondo. */
    public void draw(Gfx3D g, float[] base, World3D w) {
        updateCape(w);
        g.draw(robe, base, 1, 1, 1, 1, 0);
        g.draw(gear, base, 1, 1, 1, 1, 0);
        g.draw(glow, base, 1, 1, 1, 1, 1);
        g.draw(cape, base, 1, 1, 1, 1, 0);

        float legA, legB, armA, armB, spread = 0.16f;
        if (w.spinning()) {
            legA = 0.15f; legB = -0.15f; armA = armB = 0; spread = 1.45f;
        } else if (!w.onGround) {
            legA = 0.7f; legB = -0.5f; armA = armB = -2.3f; spread = 0.34f;
        } else {
            float sp = (float) Math.sqrt(w.vx * w.vx + w.vz * w.vz);
            float amp = Math.min(1f, sp / 3f) * 0.85f;
            legA = (float) Math.sin(w.runPhase) * amp;
            legB = -legA;
            armA = -legA * 0.8f;
            armB = -legB * 0.8f;
        }
        limb(g, base, leg, -0.1f, 0.37f, 0, 0, legA);
        limb(g, base, leg, 0.1f, 0.37f, 0, 0, legB);
        limb(g, base, arm, -0.275f, 0.79f, 0, -spread, armA);
        limb(g, base, arm, 0.275f, 0.79f, 0, spread, armB);
    }

    private void limb(Gfx3D g, float[] base, Mesh mesh, float x, float y, float z, float rz, float rx) {
        System.arraycopy(base, 0, m2, 0, 16);
        Mat4.translate(m2, x, y, z);
        if (rz != 0) Mat4.rotateZ(m2, rz);
        if (rx != 0) Mat4.rotateX(m2, rx);
        g.draw(mesh, m2, 1, 1, 1, 1, 0);
    }
}
