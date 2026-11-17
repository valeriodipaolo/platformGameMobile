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

    private final Mesh robe, glow, cape;
    private final Mesh[] gear, arm, leg;
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

    /** Scala della testa rispetto al disegno originale: più piccola, per proporzioni meno da giocattolo. */
    private static final float HS = 0.84f;
    private static float hy(float y) { return 0.93f + (y - 0.93f) * HS; }

    /** Veste di lino grezzo: tunica stretta in vita che si apre in una gonna a pieghe, orlo ricamato e impolverato, colletto arrotolato. */
    private static Mesh buildRobe() {
        MeshBuilder b = new MeshBuilder();
        final float[] white = rgb(206, 200, 186), shade = rgb(140, 138, 136), gold = rgb(150, 116, 50), blue = rgb(62, 92, 132), dust = rgb(170, 150, 120);
        float[] ys = {0.300f, 0.318f, 0.336f, 0.40f, 0.485f, 0.56f, 0.66f, 0.76f, 0.83f, 0.875f};
        float[] rx = {0.262f, 0.260f, 0.256f, 0.232f, 0.186f, 0.212f, 0.238f, 0.246f, 0.205f, 0.120f};
        float[] rz = {0.236f, 0.234f, 0.230f, 0.206f, 0.160f, 0.178f, 0.196f, 0.192f, 0.165f, 0.110f};
        float[][] c = new float[ys.length][];
        for (int i = 0; i < ys.length; i++) c[i] = new float[] {0, ys[i], 0};
        FoeModels.loft(b, c, rx, rz, 0, 0, 1, 28, 1f, new FoeModels.Paint() {
            @Override void col(float t, float a, float x, float y, float z, float[] o) {
                if (y < 0.317f) { o[0] = gold[0]; o[1] = gold[1]; o[2] = gold[2]; return; }
                if (y < 0.337f) { o[0] = blue[0]; o[1] = blue[1]; o[2] = blue[2]; return; }
                float pleat = 0.5f + 0.5f * (float) Math.cos(a * 10f);
                float k = (1f - FoeModels.sm((y - 0.34f) / 0.16f)) * (1f - pleat) * 0.75f;
                k = Math.max(k, 0.55f * (1f - FoeModels.sm(Math.abs(y - 0.485f) / 0.06f)));          // ombra dove la cintura stringe
                FoeModels.mix(white, shade, k, o);
                float d = (1f - FoeModels.sm((y - 0.34f) / 0.12f)) * 0.55f;                           // polvere del sentiero verso l'orlo
                o[0] += (dust[0] - o[0]) * d; o[1] += (dust[1] - o[1]) * d; o[2] += (dust[2] - o[2]) * d;
            }
        }, 4f, 6f);
        // colletto arrotolato attorno al collo
        float[][] cc = {{0, 0.855f, 0}, {0, 0.875f, 0}, {0, 0.905f, 0}, {0, 0.93f, 0}, {0, 0.94f, 0}};
        FoeModels.loft(b, cc, new float[] {0.13f, 0.168f, 0.172f, 0.14f, 0.08f}, new float[] {0.12f, 0.152f, 0.156f, 0.126f, 0.075f}, 0, 0, 1, 16, 1f,
                FoeModels.along(shade, white), 3f, 6f);
        // coperta arrotolata in cima allo zaino, legata con due cinghie
        final float[] hideDk = rgb(74, 46, 28), blanket = rgb(58, 82, 112), blanketLt = rgb(98, 122, 148);
        float[][] rc = new float[7][];
        float[] rr = new float[7];
        for (int i = 0; i < 7; i++) {
            float t = i / 6f;
            rc[i] = new float[] {-0.23f + 0.46f * t, 0.905f, -0.335f};
            rr[i] = (i == 0 || i == 6) ? 0.062f : 0.078f;
        }
        FoeModels.loft(b, rc, rr, rr, 0, 1, 0, 12, 1f, new FoeModels.Paint() {
            @Override void col(float t, float a, float x, float y, float z, float[] o) {
                if (Math.abs(Math.abs(x) - 0.11f) < 0.018f) { o[0] = hideDk[0]; o[1] = hideDk[1]; o[2] = hideDk[2]; return; }
                FoeModels.mix(blanket, blanketLt, 0.5f + 0.5f * (float) Math.sin(a * 3f + x * 30f), o);
            }
        }, 3f, 3f);
        Decor.disc(b, -0.231f, 0.905f, -0.335f, -1, 0, 0, 0.062f, 10, blanketLt, blanket);
        Decor.disc(b, 0.231f, 0.905f, -0.335f, 1, 0, 0, 0.062f, 10, blanketLt, blanket);
        Mesh m = b.build(Img.R3_CLOTH, false, true);
        m.spec = 0.03f; m.shine = 8f;
        return m;
    }

    /** Tutto ciò che non è stoffa, diviso per materiale: lamiera smaltata, cuoio, parti scure e lucide. */
    private static Mesh[] buildGear() {
        MeshBuilder mt = new MeshBuilder(), lt = new MeshBuilder(), gl = new MeshBuilder();
        final float[] shell = rgb(228, 226, 218), shellLo = rgb(150, 152, 156), orange = rgb(190, 92, 34), dark = rgb(30, 32, 38);
        final float[] steel = rgb(126, 130, 138), brass = rgb(178, 142, 70);
        final float[] hide = rgb(122, 76, 42), hideDk = rgb(78, 48, 28), hideLt = rgb(158, 108, 64);

        // sotto la gonna: fondo scuro, così non si vede dentro
        Decor.disc(gl, 0, 0.306f, 0, 0, -1, 0, 0.235f, 14, dark, dark);
        // collo: soffietto di gomma
        FoeModels.tube(gl, 0, 0.9f, 0, 0, hy(1.0f), 0, 0.07f, 0.06f, 8, FoeModels.flat(dark));
        // testa: casco di lamiera smaltata a pannelli, con una striscia verniciata e il bordo inferiore annerito
        FoeModels.ballY(mt, 0, hy(1.08f), 0, 0.33f * HS, 0.25f * HS, 0.29f * HS, 36, 20, new FoeModels.Paint() {
            @Override void col(float t, float a, float x, float y, float z, float[] o) {
                if (Math.abs(x) < 0.026f && y > hy(1.1f) && z < 0.2f * HS) { o[0] = orange[0]; o[1] = orange[1]; o[2] = orange[2]; return; }
                FoeModels.mix(shellLo, shell, FoeModels.sm((y - hy(0.84f)) / 0.14f), o);
            }
        }, 4f, 2f);
        gl.colorf(0.035f, 0.045f, 0.07f);
        gl.ellipsoid(0, hy(1.07f), 0.13f * HS, 0.25f * HS, 0.13f * HS, 0.2f * HS, 24, 12);   // visiera di vetro scuro
        for (int sd = -1; sd <= 1; sd += 2) {                                                // auricolari
            FoeModels.ballZ(mt, sd * 0.318f * HS, hy(1.07f), 0, 0.05f * HS, 0.085f * HS, 0.085f * HS, 14, 8, FoeModels.flat(orange), 1f, 1f);
            FoeModels.ballZ(mt, sd * 0.352f * HS, hy(1.07f), 0, 0.026f * HS, 0.04f * HS, 0.04f * HS, 10, 6, FoeModels.flat(steel), 1f, 1f);
        }
        gl.colorf(dark[0], dark[1], dark[2]);
        gl.roundBox(0, hy(1.08f), -0.262f * HS, 0.24f * HS, 0.17f * HS, 0.08f * HS, 0.03f * HS);   // pannello dietro la testa
        FoeModels.tube(mt, 0, hy(1.3f), 0, 0, hy(1.47f), 0, 0.012f, 0.008f, 6, FoeModels.flat(steel)); // antenna
        FoeModels.ballY(mt, 0, hy(1.325f), 0, 0.036f, 0.02f, 0.036f, 10, 5, FoeModels.flat(steel), 1f, 1f);

        // cintura di cuoio con fibbia d'ottone e borsello
        float[][] bc = {{0, 0.455f, 0}, {0, 0.465f, 0}, {0, 0.505f, 0}, {0, 0.515f, 0}};
        FoeModels.loft(lt, bc, new float[] {0.19f, 0.204f, 0.204f, 0.19f}, new float[] {0.164f, 0.178f, 0.178f, 0.164f}, 0, 0, 1, 18, 1f, FoeModels.flat(hide), 6f, 0.4f);
        mt.colorf(brass[0], brass[1], brass[2]);
        mt.roundBox(0, 0.485f, 0.178f, 0.085f, 0.075f, 0.03f, 0.012f);
        lt.colorf(hideDk[0], hideDk[1], hideDk[2]);
        lt.roundBox(0.17f, 0.445f, 0.085f, 0.07f, 0.1f, 0.09f, 0.025f);
        mt.colorf(brass[0], brass[1], brass[2]);
        mt.box(0.205f, 0.455f, 0.085f, 0.012f, 0.025f, 0.03f);

        // zaino: corpo, patta con due cinghie e fibbie, tasche laterali
        FoeModels.Paint leather = new FoeModels.Paint() {
            @Override void col(float t, float a, float x, float y, float z, float[] o) { FoeModels.mix(hideDk, hideLt, FoeModels.sm((y - 0.46f) / 0.42f), o); }
        };
        lt.colorf(hide[0], hide[1], hide[2]);
        lt.roundBox(0, 0.655f, -0.33f, 0.36f, 0.37f, 0.2f, 0.07f);
        lt.colorf(hideDk[0], hideDk[1], hideDk[2]);
        lt.roundBox(0, 0.775f, -0.337f, 0.38f, 0.16f, 0.225f, 0.07f);                        // patta
        for (int sd = -1; sd <= 1; sd += 2) {
            lt.colorf(hideLt[0], hideLt[1], hideLt[2]);
            lt.box(sd * 0.09f, 0.70f, -0.452f, 0.04f, 0.2f, 0.012f);                          // cinghia della patta
            mt.colorf(brass[0], brass[1], brass[2]);
            mt.roundBox(sd * 0.09f, 0.635f, -0.458f, 0.058f, 0.05f, 0.018f, 0.008f);         // fibbia
            lt.colorf(hideDk[0], hideDk[1], hideDk[2]);
            lt.roundBox(sd * 0.205f, 0.585f, -0.33f, 0.08f, 0.19f, 0.14f, 0.035f);           // tasca
            lt.colorf(hideLt[0], hideLt[1], hideLt[2]);
            lt.box(sd * 0.246f, 0.63f, -0.33f, 0.008f, 0.03f, 0.1f);
            // spallacci: dalla cima dello zaino, sopra la spalla, fin sotto il braccio
            FoeModels.loft(lt, new float[][] {{sd * 0.125f, 0.82f, -0.25f}, {sd * 0.135f, 0.885f, -0.1f}, {sd * 0.14f, 0.86f, 0.06f},
                            {sd * 0.15f, 0.74f, 0.17f}, {sd * 0.17f, 0.6f, 0.13f}, {sd * 0.19f, 0.52f, -0.06f}, {sd * 0.15f, 0.5f, -0.24f}},
                    new float[] {0.012f, 0.012f, 0.012f, 0.012f, 0.012f, 0.012f, 0.012f}, new float[] {0.03f, 0.032f, 0.032f, 0.03f, 0.028f, 0.026f, 0.026f},
                    1, 0, 0, 6, 1f, leather, 0.5f, 4f);
        }
        // lanterna appesa allo zaino
        gl.colorf(dark[0], dark[1], dark[2]);
        gl.box(-0.262f, 0.535f, -0.4f, 0.05f, 0.014f, 0.05f);
        gl.box(-0.262f, 0.455f, -0.4f, 0.05f, 0.014f, 0.05f);
        FoeModels.tube(mt, -0.262f, 0.54f, -0.4f, -0.25f, 0.62f, -0.38f, 0.006f, 0.006f, 4, FoeModels.flat(steel));
        Mesh metal = mt.build(Img.HERO_METAL, false, true), hideM = lt.build(Img.HERO_LEATHER, false, true), gloss = gl.build(-1, false, true);
        metal.spec = 0.5f; metal.shine = 46f;
        hideM.spec = 0.10f; hideM.shine = 14f;
        gloss.spec = 1.0f; gloss.shine = 90f;
        return new Mesh[] {metal, hideM, gloss};
    }

    /** Parti accese: occhi, gemma del fermaglio, punta dell'antenna, luci dietro il casco, vetro della lanterna. */
    private static Mesh buildGlow() {
        MeshBuilder b = new MeshBuilder().color(120, 236, 255);
        b.ellipsoid(-0.085f * HS, hy(1.07f), 0.305f * HS, 0.036f * HS, 0.058f * HS, 0.03f * HS, 10, 6);
        b.ellipsoid(0.085f * HS, hy(1.07f), 0.305f * HS, 0.036f * HS, 0.058f * HS, 0.03f * HS, 10, 6);
        b.bipyramid(0, 0.885f, 0.166f, 0.03f, 0.04f, 6);
        b.box(-0.06f * HS, hy(1.08f), -0.304f * HS, 0.05f * HS, 0.03f * HS, 0.01f);
        b.box(0.06f * HS, hy(1.08f), -0.304f * HS, 0.05f * HS, 0.03f * HS, 0.01f);
        b.color(255, 70, 60);
        b.ellipsoid(0, hy(1.49f), 0, 0.03f, 0.03f, 0.03f, 8, 5);
        b.color(255, 214, 110);
        b.box(-0.262f, 0.495f, -0.4f, 0.04f, 0.066f, 0.04f);
        return b.build(-1, false, true);
    }

    /** Braccio (origine alla spalla): manica di lino a campana [0], polsino d'ottone, avambraccio d'acciaio e mano [1]. */
    private static Mesh[] buildArm() {
        MeshBuilder b = new MeshBuilder(), mt = new MeshBuilder();
        final float[] white = rgb(206, 200, 186), shade = rgb(142, 140, 138), gold = rgb(160, 126, 60), dark = rgb(70, 74, 82), hand = rgb(176, 180, 188);
        FoeModels.ballY(b, 0, 0, 0, 0.078f, 0.07f, 0.078f, 10, 6, FoeModels.flat(white), 2f, 2f);
        float[][] c = {{0, 0.02f, 0}, {0, -0.08f, 0}, {0, -0.17f, 0}, {0, -0.205f, 0}, {0, -0.215f, 0}};
        float[] r = {0.062f, 0.07f, 0.09f, 0.1f, 0.1f};
        FoeModels.loft(b, c, r, r, 0, 0, 1, 12, 1f, new FoeModels.Paint() {
            @Override void col(float t, float a, float x, float y, float z, float[] o) {
                if (y < -0.19f) { o[0] = gold[0]; o[1] = gold[1]; o[2] = gold[2]; return; }
                FoeModels.mix(white, shade, 0.4f * (0.5f + 0.5f * (float) Math.cos(a * 5f)), o);
            }
        }, 2f, 2f);
        Decor.disc(mt, 0, -0.213f, 0, 0, -1, 0, 0.096f, 10, dark, dark);
        FoeModels.loft(mt, new float[][] {{0, -0.19f, 0}, {0, -0.25f, 0}, {0, -0.30f, 0}, {0, -0.32f, 0}}, new float[] {0.036f, 0.044f, 0.04f, 0.03f},
                new float[] {0.036f, 0.044f, 0.04f, 0.03f}, 0, 0, 1, 10, 1f, FoeModels.flat(dark), 1f, 0.5f);
        // mano: palmo e tre dita tozze di metallo
        FoeModels.ballY(mt, 0, -0.345f, 0, 0.05f, 0.045f, 0.036f, 10, 6, FoeModels.flat(hand), 1f, 1f);
        for (int f = -1; f <= 1; f++)
            FoeModels.tube(mt, f * 0.026f, -0.365f, 0.004f, f * 0.03f, -0.415f, 0.014f, 0.014f, 0.011f, 6, FoeModels.flat(hand));
        FoeModels.tube(mt, 0.04f, -0.34f, 0.01f, 0.058f, -0.375f, 0.03f, 0.013f, 0.01f, 6, FoeModels.flat(hand));
        Mesh cloth = b.build(Img.R3_CLOTH, false, true), metal = mt.build(Img.HERO_METAL, false, true);
        cloth.spec = 0.03f; cloth.shine = 8f;
        metal.spec = 0.5f; metal.shine = 46f;
        return new Mesh[] {cloth, metal};
    }

    /** Gamba (origine all'anca): stinco d'acciaio e ginocchio [0], stivale di cuoio con risvolto e suola [1]. */
    private static Mesh[] buildLeg() {
        MeshBuilder mt = new MeshBuilder(), b = new MeshBuilder();
        final float[] dark = rgb(70, 74, 82), joint = rgb(132, 136, 144), hide = rgb(112, 70, 40), hideLt = rgb(150, 104, 62), sole = rgb(34, 30, 28);
        FoeModels.loft(mt, new float[][] {{0, 0.02f, 0}, {0, -0.08f, 0}, {0, -0.16f, 0}, {0, -0.24f, 0}}, new float[] {0.05f, 0.054f, 0.048f, 0.044f},
                new float[] {0.05f, 0.054f, 0.048f, 0.044f}, 0, 0, 1, 10, 1f, FoeModels.flat(dark), 1f, 0.6f);
        FoeModels.ballZ(mt, 0, -0.12f, 0.01f, 0.06f, 0.052f, 0.056f, 10, 6, FoeModels.flat(joint), 1f, 1f);
        float[][] c = {{0, -0.19f, 0}, {0, -0.205f, 0}, {0, -0.225f, 0}, {0, -0.3f, 0.005f}, {0, -0.345f, 0.01f}};
        float[] r = {0.07f, 0.084f, 0.07f, 0.068f, 0.072f};
        FoeModels.loft(b, c, r, r, 0, 0, 1, 12, 1f, new FoeModels.Paint() {
            @Override void col(float t, float a, float x, float y, float z, float[] o) { FoeModels.mix(hide, hideLt, y > -0.228f ? 1f : 0f, o); }
        }, 2f, 1f);
        FoeModels.ballZ(b, 0, -0.318f, 0.05f, 0.078f, 0.056f, 0.13f, 12, 8, FoeModels.ud(hide, hideLt), 1.5f, 1.5f);
        b.colorf(sole[0], sole[1], sole[2]);
        b.roundBox(0, -0.362f, 0.05f, 0.15f, 0.03f, 0.262f, 0.014f);
        Mesh metal = mt.build(Img.HERO_METAL, false, true), boot = b.build(Img.HERO_LEATHER, false, true);
        metal.spec = 0.5f; metal.shine = 46f;
        boot.spec = 0.14f; boot.shine = 18f;
        return new Mesh[] {metal, boot};
    }

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
                        if (layer == 1) { r = 0.24f; g = 0.36f; b = 0.52f; }
                        else if (i == CR - 1) { r = 0.59f; g = 0.455f; b = 0.2f; }
                        else { r = 0.81f - 0.16f * fold; g = 0.785f - 0.15f * fold; b = 0.73f - 0.12f * fold; }
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

    // stato dell'animazione: i valori si inseguono dolcemente, così passare da corsa a salto non è uno scatto
    /** Animazioni scelte dal menu: 0 = originali, 2 = nuove (corsa lenta e molto marcata, salto scattante con caduta più rapida); 1 non è più offerto. */
    public static int style = 2;
    private float animT = -1, landT = 0, liftT = 0;
    private float sLegA, sLegB, sArmA, sArmB, sSpread, sLegSpread, sTuck;
    private boolean wasGround = true;
    private final float[] mb = new float[16], mt = new float[16];

    private static float lerp(float a, float b, float k) { return a + (b - a) * k; }
    private static float pick(int st, float a, float b, float c) { return st == 0 ? a : st == 1 ? b : c; }

    /** base = posizione e orientamento del robot nel mondo. */
    public void draw(Gfx3D g, float[] base, World3D w) {
        updateCape(w);
        boolean first = animT < 0 || w.time < animT;
        float dt = first ? 1f : Math.min(0.1f, w.time - animT);
        animT = w.time;
        float sp = (float) Math.sqrt(w.vx * w.vx + w.vz * w.vz);
        boolean spin = w.spinning(), air = !w.onGround && !spin;
        final int st = style;
        final boolean nu = st >= 1;
        // coefficienti: 1 = animazione originale; le nuove sono più rapide e accentuate, le scattanti ancora di più
        final float phK = pick(st, 1f, 1.5f, 0.72f), swK = pick(st, 1f, 1.4f, 2.3f), bobK = pick(st, 1f, 2f, 3.6f), leanK = pick(st, 1f, 1.8f, 2.8f),
                rollK = pick(st, 1f, 2f, 3f), twK = pick(st, 1f, 2f, 3f), liftK = pick(st, 1f, 2.4f, 5.5f), airK = pick(st, 1f, 1.3f, 1.55f),
                landDur = pick(st, 0.22f, 0.17f, 0.12f), landSq = pick(st, 1f, 1.8f, 2.6f), liftDur = pick(st, 0.14f, 0.1f, 0.07f);
        if (!wasGround && w.onGround) landT = landDur;        // appena atterrato: ammortizza piegandosi
        if (wasGround && !w.onGround && w.vy > 3f) liftT = liftDur;   // appena staccato: si allunga
        wasGround = w.onGround;
        landT = Math.max(0, landT - dt);
        liftT = Math.max(0, liftT - dt);

        float amp = Math.min(1f, sp / 4.2f), ph = w.runPhase * phK;
        float tLegA, tLegB, tArmA, tArmB, tSpread = 0.16f, tLegSpread = 0f, tTuck = 0f;
        if (spin) {
            tLegA = 0.15f; tLegB = -0.15f; tArmA = tArmB = 0; tSpread = 1.45f;
        } else if (air) {
            float up = Math.max(0f, Math.min(1f, (w.vy + 6f) / 12f));          // 1 = in salita, 0 = in caduta
            boolean dj = w.djTime > 0;
            if (dj) { tLegA = 1.25f; tLegB = 1.05f; tArmA = tArmB = -0.9f; tSpread = 0.9f; tTuck = 1f; }
            else {
                tLegA = lerp(0.3f, 1.0f, up); tLegB = lerp(-0.1f, -0.45f, up);       // salita: ginocchio avanti; discesa: gambe tese
                tArmA = tArmB = lerp(-0.35f, -2.6f, up);                              // salita: braccia in alto; discesa: aperte
                tSpread = lerp(1.15f, 0.45f, up);
                tLegSpread = lerp(0.2f, 0.05f, up);
                tTuck = up * 0.5f;
            }
            if (nu) {                                          // salto più espressivo: ginocchia e braccia più decisi
                tLegA *= airK; tLegB *= airK; tArmA *= pick(st, 1f, 1.15f, 1.3f); tArmB *= pick(st, 1f, 1.15f, 1.3f);
                if (w.vy > 0) { tLegA += pick(st, 0f, 0.25f, 0.45f); tSpread += pick(st, 0f, 0.15f, 0.3f); }
            }
        } else {
            float swing = (float) Math.sin(ph) * (0.18f + 0.82f * amp) * (amp > 0.03f ? 1f : 0f) * swK;
            tLegA = swing * 0.95f;
            tLegB = -tLegA;
            tArmA = -tLegA * (0.75f + 0.25f * amp);
            tArmB = -tLegB * (0.75f + 0.25f * amp);
            tSpread = 0.16f + 0.05f * amp + 0.02f * (float) Math.sin(w.time * 2.2f) * (1f - amp);
        }
        float k = first ? 1f : Math.min(1f, dt * (st == 2 ? (air || spin ? 55f : 60f) : nu ? (air || spin ? 34f : 40f) : (air || spin ? 16f : 22f)));
        sLegA = lerp(sLegA, tLegA, k); sLegB = lerp(sLegB, tLegB, k);
        sArmA = lerp(sArmA, tArmA, k); sArmB = lerp(sArmB, tArmB, k);
        sSpread = lerp(sSpread, tSpread, k); sLegSpread = lerp(sLegSpread, tLegSpread, k); sTuck = lerp(sTuck, tTuck, k);

        // corpo: rimbalzo a ogni passo, inclinazione in avanti con la velocità, ondeggio di bacino e spalle
        float bob = 0, lean = 0, roll = 0, twist = 0, sy = 1f, sxz = 1f;
        if (!air && !spin) {
            float step = (float) Math.abs(Math.sin(ph)) * amp;
            bob = 0.055f * bobK * step + 0.007f * (nu ? 2.5f : 1f) * (float) Math.sin(w.time * (nu ? 3.2f : 2.2f)) * (1f - amp);
            lean = 0.17f * leanK * amp;
            roll = (float) Math.sin(ph) * 0.06f * rollK * amp;
            twist = (float) Math.sin(ph) * 0.14f * twK * amp;
        } else if (air) {
            lean = (0.08f + 0.12f * (1f - Math.max(0f, Math.min(1f, (w.vy + 6f) / 12f)))) * (nu ? 1.6f : 1f);
        }
        if (landT > 0) {                                      // schiacciamento all'atterraggio
            float u = (float) Math.sin(Math.PI * (1f - landT / landDur));
            sy = 1f - 0.13f * landSq * u; sxz = 1f + 0.07f * landSq * u;
            bob -= 0.02f * landSq * u;
            lean += 0.1f * landSq * u;
        } else if (liftT > 0) {
            float u = liftT / liftDur;
            sy = 1f + 0.07f * landSq * u; sxz = 1f - 0.04f * landSq * u;
        }
        System.arraycopy(base, 0, mb, 0, 16);                 // mb = bacino: rimbalzo e schiacciamento, le gambe partono da qui
        Mat4.translate(mb, 0, bob, 0);
        Mat4.scale(mb, sxz, sy, sxz);
        System.arraycopy(mb, 0, mt, 0, 16);                   // mt = busto: si inclina e ruota attorno ai fianchi
        Mat4.translate(mt, 0, 0.4f, 0);
        Mat4.rotateY(mt, twist);
        Mat4.rotateX(mt, lean);
        Mat4.rotateZ(mt, roll);
        Mat4.translate(mt, 0, -0.4f, 0);

        g.draw(robe, mt, 1, 1, 1, 1, 0);
        for (Mesh gm : gear) g.draw(gm, mt, 1, 1, 1, 1, 0);
        g.draw(glow, mt, 1, 1, 1, 1, 1);
        g.draw(cape, mt, 1, 1, 1, 1, 0);
        // gambe: la gamba che avanza si solleva, quella che spinge resta a terra
        float liftA = (!air && !spin) ? Math.max(0f, (float) Math.cos(ph)) * 0.05f * liftK * amp : 0f;
        float liftB = (!air && !spin) ? Math.max(0f, -(float) Math.cos(ph)) * 0.05f * liftK * amp : 0f;
        limb(g, mb, leg, -0.1f, 0.37f + liftA, 0, -sLegSpread, sLegA);
        limb(g, mb, leg, 0.1f, 0.37f + liftB, 0, sLegSpread, sLegB);
        limb(g, mt, arm, -0.275f, 0.79f, 0, -sSpread, sArmA);
        limb(g, mt, arm, 0.275f, 0.79f, 0, sSpread, sArmB);
    }

    private void limb(Gfx3D g, float[] base, Mesh[] mesh, float x, float y, float z, float rz, float rx) {
        System.arraycopy(base, 0, m2, 0, 16);
        Mat4.translate(m2, x, y, z);
        if (rz != 0) Mat4.rotateZ(m2, rz);
        if (rx != 0) Mat4.rotateX(m2, rx);
        for (Mesh lm : mesh) g.draw(lm, m2, 1, 1, 1, 1, 0);
    }
}
