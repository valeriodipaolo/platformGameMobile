package com.valeriodipaolo.platformgame.core3d;

/**
 * Gli altri quattro personaggi giocabili, costruiti sullo stesso scheletro del robot (piedi a y = 0, anche a y 0,37 e x ±0,1,
 * spalle a y 0,79 e x ±0,275, mano a circa 0,42 sotto la spalla): così corsa, salto e giravolta valgono per tutti.
 * Ognuno è fatto di quattro pezzi: corpo e testa [0], parti lucide [1], braccio, gamba.
 */
final class HeroSkins {
    private HeroSkins() {}

    static final String[] NAMES = {"Robot", "Volpe", "Pinguino", "Cavaliere", "Astronauta"};

    /** Pezzi del personaggio c (1..4): {corpo, lucido} , braccio, gamba. */
    static Mesh[][] build(int c) {
        switch (c) {
            case 2: return penguin();
            case 3: return knight();
            case 4: return astronaut();
            default: return fox();
        }
    }

    private static float[] rgb(int r, int g, int b) { return FoeModels.rgb(r, g, b); }

    private static void col(MeshBuilder b, float[] c) { b.colorf(c[0], c[1], c[2]); }

    private static Mesh done(MeshBuilder b, float spec, float shine) {
        Mesh m = b.build(-1, false, true);
        m.spec = spec; m.shine = shine;
        return m;
    }

    /** Occhi a mandorla con iride e riflesso, guardano verso z positivo. */
    private static void eyes(MeshBuilder b, float y, float z, float dx, float r, float[] iris) {
        for (int s = -1; s <= 1; s += 2) {
            col(b, rgb(250, 250, 246));
            b.ellipsoid(s * dx, y, z, r, r * 1.2f, r * 0.6f, 12, 8);
            col(b, iris);
            b.ellipsoid(s * dx, y - r * 0.05f, z + r * 0.45f, r * 0.6f, r * 0.72f, r * 0.25f, 10, 6);
            col(b, rgb(16, 14, 18));
            b.ellipsoid(s * dx, y - r * 0.05f, z + r * 0.58f, r * 0.32f, r * 0.4f, r * 0.15f, 8, 5);
            col(b, rgb(255, 255, 255));
            b.ellipsoid(s * dx + r * 0.2f, y + r * 0.35f, z + r * 0.66f, r * 0.12f, r * 0.12f, r * 0.06f, 6, 4);
        }
    }

    /** Sciarpa annodata al collo con le due code che scendono sulla schiena. */
    private static void scarf(MeshBuilder b, float y, float r, float[] c, float[] dk) {
        col(b, c);
        b.cylinder(0, y - 0.035f, y + 0.035f, 0, r, r * 0.92f, 18, false, false);
        b.ellipsoid(0, y, 0, r * 1.02f, 0.045f, r * 1.02f, 16, 5);
        FoeModels.tube(b, new float[][] {{0.05f, y, -r}, {0.08f, y - 0.12f, -r - 0.05f}, {0.1f, y - 0.24f, -r - 0.07f}}, new float[] {0.04f, 0.035f, 0.03f}, 6, FoeModels.flat(c));
        FoeModels.tube(b, new float[][] {{-0.02f, y, -r}, {-0.02f, y - 0.1f, -r - 0.06f}, {-0.03f, y - 0.19f, -r - 0.08f}}, new float[] {0.04f, 0.035f, 0.03f}, 6, FoeModels.flat(dk));
    }

    // ------------------------------------------------------------------ volpe: pelo arancio, muso e petto chiari, orecchie a punta, coda folta

    private static Mesh[][] fox() {
        final float[] fur = rgb(226, 112, 36), furDk = rgb(170, 74, 24), cream = rgb(250, 236, 214), dark = rgb(54, 34, 26),
                scarf = rgb(56, 150, 84), scarfDk = rgb(36, 108, 60), belt = rgb(110, 70, 40), shorts = rgb(90, 96, 60);
        MeshBuilder b = new MeshBuilder();
        // busto, petto chiaro, pantaloncini e cintura
        col(b, fur); b.ellipsoid(0, 0.62f, 0, 0.2f, 0.27f, 0.17f, 18, 12);
        col(b, cream); b.ellipsoid(0, 0.66f, 0.07f, 0.13f, 0.19f, 0.11f, 14, 10);
        col(b, shorts); b.ellipsoid(0, 0.42f, 0, 0.205f, 0.11f, 0.175f, 18, 8);
        col(b, belt); b.cylinder(0, 0.475f, 0.505f, 0, 0.19f, 0.185f, 18, false, false);
        col(b, rgb(220, 180, 70)); b.box(0, 0.49f, 0.185f, 0.06f, 0.04f, 0.02f);
        // testa: cranio, guance chiare, muso allungato, naso
        col(b, fur); b.ellipsoid(0, 1.08f, 0, 0.2f, 0.18f, 0.18f, 18, 12);
        col(b, cream);
        b.ellipsoid(-0.1f, 1.0f, 0.08f, 0.1f, 0.075f, 0.09f, 12, 8);
        b.ellipsoid(0.1f, 1.0f, 0.08f, 0.1f, 0.075f, 0.09f, 12, 8);
        b.ellipsoid(0, 1.02f, 0.18f, 0.075f, 0.065f, 0.12f, 12, 8);
        col(b, dark); b.ellipsoid(0, 1.05f, 0.3f, 0.038f, 0.03f, 0.03f, 10, 6);
        eyes(b, 1.11f, 0.14f, 0.075f, 0.048f, rgb(70, 150, 70));
        col(b, furDk);                                                 // sopracciglia decise
        b.box(-0.08f, 1.175f, 0.16f, 0.08f, 0.02f, 0.02f);
        b.box(0.08f, 1.175f, 0.16f, 0.08f, 0.02f, 0.02f);
        // orecchie a punta con la punta scura
        for (int s = -1; s <= 1; s += 2) {
            FoeModels.tube(b, s * 0.11f, 1.2f, -0.01f, s * 0.17f, 1.4f, -0.03f, 0.075f, 0.012f, 10, FoeModels.flat(fur));
            FoeModels.tube(b, s * 0.155f, 1.35f, -0.025f, s * 0.172f, 1.41f, -0.031f, 0.03f, 0.008f, 8, FoeModels.flat(dark));
            col(b, cream); b.ellipsoid(s * 0.13f, 1.27f, 0.025f, 0.035f, 0.07f, 0.012f, 8, 5);
        }
        // ciuffo sulla fronte
        FoeModels.tube(b, 0, 1.24f, 0.08f, 0.02f, 1.3f, 0.16f, 0.04f, 0.01f, 6, FoeModels.flat(fur));
        scarf(b, 0.89f, 0.14f, scarf, scarfDk);
        // coda folta che sale, con la punta bianca
        FoeModels.tube(b, new float[][] {{0, 0.44f, -0.14f}, {0, 0.46f, -0.32f}, {0, 0.58f, -0.48f}, {0, 0.76f, -0.55f}},
                new float[] {0.05f, 0.11f, 0.12f, 0.08f}, 12, FoeModels.flat(fur));
        col(b, cream); b.ellipsoid(0, 0.82f, -0.56f, 0.075f, 0.09f, 0.075f, 10, 7);
        Mesh body = done(b, 0.08f, 12f);
        MeshBuilder sh = new MeshBuilder();
        col(sh, rgb(250, 250, 250)); sh.ellipsoid(0, 1.07f, 0.305f, 0.012f, 0.01f, 0.006f, 6, 4);
        Mesh shiny = done(sh, 0.8f, 50f);
        // braccio: pelo arancio, guanto scuro
        MeshBuilder a = new MeshBuilder();
        FoeModels.ballY(a, 0, 0, 0, 0.07f, 0.065f, 0.07f, 10, 6, FoeModels.flat(fur), 1f, 1f);
        FoeModels.tube(a, 0, 0, 0, 0, -0.3f, 0, 0.058f, 0.05f, 10, FoeModels.flat(fur));
        FoeModels.ballY(a, 0, -0.35f, 0.01f, 0.058f, 0.06f, 0.05f, 10, 6, FoeModels.flat(dark), 1f, 1f);
        FoeModels.tube(a, 0.04f, -0.34f, 0.02f, 0.06f, -0.38f, 0.04f, 0.02f, 0.015f, 6, FoeModels.flat(dark));
        Mesh arm = done(a, 0.06f, 10f);
        // gamba: pelo e stivaletto di cuoio
        MeshBuilder l = new MeshBuilder();
        FoeModels.tube(l, 0, 0.02f, 0, 0, -0.24f, 0, 0.07f, 0.055f, 10, FoeModels.flat(fur));
        FoeModels.ballZ(l, 0, -0.29f, 0.04f, 0.075f, 0.065f, 0.13f, 12, 8, FoeModels.flat(belt), 1f, 1f);
        col(l, dark); l.roundBox(0, -0.355f, 0.04f, 0.15f, 0.03f, 0.26f, 0.012f);
        Mesh leg = done(l, 0.12f, 16f);
        return new Mesh[][] {{body, shiny}, {arm}, {leg}};
    }

    // ------------------------------------------------------------------ pinguino: corpo a uovo, pancia bianca, berretto con il pompon

    private static Mesh[][] penguin() {
        final float[] black = rgb(36, 40, 52), white = rgb(246, 246, 240), orange = rgb(250, 150, 40), red = rgb(206, 40, 46), redDk = rgb(150, 26, 34),
                hat = rgb(50, 110, 200), hatDk = rgb(34, 80, 160);
        MeshBuilder b = new MeshBuilder();
        col(b, black); b.ellipsoid(0, 0.66f, -0.01f, 0.26f, 0.38f, 0.23f, 22, 14);
        col(b, white); b.ellipsoid(0, 0.62f, 0.08f, 0.2f, 0.31f, 0.16f, 18, 12);
        col(b, black); b.ellipsoid(0, 1.08f, 0, 0.2f, 0.18f, 0.19f, 18, 12);
        col(b, white);
        b.ellipsoid(-0.075f, 1.08f, 0.11f, 0.1f, 0.11f, 0.09f, 12, 8);
        b.ellipsoid(0.075f, 1.08f, 0.11f, 0.1f, 0.11f, 0.09f, 12, 8);
        eyes(b, 1.11f, 0.17f, 0.07f, 0.045f, rgb(60, 100, 170));
        col(b, orange); b.ellipsoid(0, 1.03f, 0.24f, 0.055f, 0.035f, 0.08f, 12, 7);
        col(b, rgb(220, 120, 30)); b.ellipsoid(0, 1.01f, 0.235f, 0.045f, 0.02f, 0.07f, 10, 5);
        col(b, rgb(250, 160, 170));                                   // guance rosate
        b.ellipsoid(-0.13f, 1.04f, 0.13f, 0.035f, 0.02f, 0.02f, 8, 5);
        b.ellipsoid(0.13f, 1.04f, 0.13f, 0.035f, 0.02f, 0.02f, 8, 5);
        // berretto di lana con risvolto e pompon
        col(b, hat); b.ellipsoid(0, 1.19f, -0.01f, 0.205f, 0.13f, 0.2f, 18, 10);
        col(b, hatDk); b.cylinder(0, 1.14f, 1.2f, -0.01f, 0.205f, 0.205f, 20, false, false);
        col(b, white); b.ellipsoid(0, 1.34f, -0.02f, 0.065f, 0.065f, 0.065f, 12, 8);
        scarf(b, 0.92f, 0.17f, red, redDk);
        // codina
        col(b, black); b.ellipsoid(0, 0.36f, -0.2f, 0.09f, 0.05f, 0.08f, 10, 6);
        Mesh body = done(b, 0.15f, 20f);
        MeshBuilder sh = new MeshBuilder();
        col(sh, rgb(255, 210, 120)); sh.ellipsoid(0.02f, 1.04f, 0.29f, 0.012f, 0.008f, 0.006f, 6, 4);
        Mesh shiny = done(sh, 0.8f, 50f);
        // pinna: piatta e un po' ricurva
        MeshBuilder a = new MeshBuilder();
        FoeModels.tube(a, new float[][] {{0, 0.02f, 0}, {0.01f, -0.15f, 0.01f}, {0.0f, -0.33f, 0.02f}}, new float[] {0.06f, 0.07f, 0.03f}, 10, FoeModels.flat(black));
        Mesh flipper = done(a, 0.15f, 20f);
        flatten(flipper, 0.5f);
        // gamba corta arancione e piede palmato
        MeshBuilder l = new MeshBuilder();
        FoeModels.tube(l, 0, 0.0f, 0, 0, -0.3f, 0, 0.05f, 0.04f, 8, FoeModels.flat(orange));
        col(l, orange); l.ellipsoid(0, -0.345f, 0.08f, 0.1f, 0.028f, 0.15f, 14, 6);
        for (int t = -1; t <= 1; t++) { col(l, rgb(230, 130, 30)); l.ellipsoid(t * 0.055f, -0.343f, 0.2f, 0.03f, 0.022f, 0.04f, 8, 4); }
        Mesh leg = done(l, 0.1f, 14f);
        return new Mesh[][] {{body, shiny}, {flipper}, {leg}};
    }

    /** Schiaccia una mesh lungo l'asse x (per le pinne). */
    private static void flatten(Mesh m, float k) {
        for (int i = 0; i < m.count; i++) m.v[i * Mesh.STRIDE] *= k;
    }

    // ------------------------------------------------------------------ cavaliere: armatura, sopravveste blu con la croce d'oro, elmo col pennacchio, scudo sulla schiena

    private static Mesh[][] knight() {
        final float[] steel = rgb(176, 182, 194), steelDk = rgb(104, 110, 122), blue = rgb(40, 70, 150), gold = rgb(222, 176, 60), red = rgb(196, 36, 40),
                leather = rgb(100, 62, 36);
        MeshBuilder b = new MeshBuilder(), m = new MeshBuilder();
        // sopravveste: tunica blu con l'orlo d'oro
        col(b, blue);
        b.cylinder(0, 0.33f, 0.62f, 0, 0.23f, 0.2f, 20, true, false);
        col(b, gold); b.cylinder(0, 0.33f, 0.36f, 0, 0.232f, 0.232f, 20, false, false);
        b.box(0, 0.6f, 0.2f, 0.045f, 0.2f, 0.02f);
        b.box(0, 0.64f, 0.2f, 0.16f, 0.045f, 0.02f);
        col(b, leather); b.cylinder(0, 0.48f, 0.52f, 0, 0.215f, 0.21f, 20, false, false);
        col(b, red);                                                   // pennacchio rosso che ricade all'indietro
        FoeModels.tube(b, new float[][] {{0, 1.34f, 0}, {0, 1.46f, -0.08f}, {0, 1.44f, -0.24f}, {0, 1.3f, -0.34f}}, new float[] {0.03f, 0.07f, 0.06f, 0.02f}, 10, FoeModels.flat(red));
        Mesh body = done(b, 0.05f, 10f);
        // corazza, elmo, spallacci, scudo: acciaio lucido
        col(m, steel); m.ellipsoid(0, 0.7f, 0, 0.21f, 0.22f, 0.17f, 18, 12);
        col(m, steelDk); m.cylinder(0, 0.88f, 0.94f, 0, 0.13f, 0.12f, 16, false, false);
        col(m, steel); m.cylinder(0, 0.94f, 1.24f, 0, 0.165f, 0.17f, 20, false, false);
        m.ellipsoid(0, 1.24f, 0, 0.17f, 0.11f, 0.172f, 18, 8);
        col(m, steelDk);
        m.box(0, 1.13f, 0.165f, 0.22f, 0.03f, 0.03f);                  // fessura della visiera
        m.box(0, 1.06f, 0.168f, 0.03f, 0.1f, 0.03f);
        for (int i = 0; i < 4; i++) m.box(-0.045f + i * 0.03f, 1.02f, 0.168f, 0.012f, 0.05f, 0.02f);   // forellini per respirare
        col(m, gold); m.cylinder(0, 1.235f, 1.255f, 0, 0.174f, 0.174f, 20, false, false);
        for (int s = -1; s <= 1; s += 2) {
            col(m, steel); m.ellipsoid(s * 0.27f, 0.83f, 0, 0.11f, 0.075f, 0.11f, 12, 8);
            col(m, gold); m.ellipsoid(s * 0.27f, 0.86f, 0, 0.06f, 0.03f, 0.06f, 8, 5);
        }
        col(m, blue); m.roundBox(0, 0.66f, -0.21f, 0.34f, 0.42f, 0.05f, 0.05f);
        col(m, gold); m.roundBox(0, 0.66f, -0.237f, 0.08f, 0.3f, 0.01f, 0.004f);
        m.roundBox(0, 0.7f, -0.237f, 0.24f, 0.06f, 0.01f, 0.004f);
        col(m, steel); m.roundBox(0, 0.66f, -0.205f, 0.37f, 0.45f, 0.03f, 0.04f);
        Mesh shiny = done(m, 0.75f, 50f);
        // braccio: maglia scura e guanto d'arme
        MeshBuilder a = new MeshBuilder();
        FoeModels.tube(a, 0, 0, 0, 0, -0.3f, 0, 0.06f, 0.055f, 10, FoeModels.flat(steelDk));
        FoeModels.tube(a, 0, -0.18f, 0, 0, -0.3f, 0, 0.068f, 0.066f, 10, FoeModels.flat(steel));
        FoeModels.ballY(a, 0, -0.35f, 0.01f, 0.065f, 0.06f, 0.055f, 10, 6, FoeModels.flat(steel), 1f, 1f);
        Mesh arm = done(a, 0.7f, 46f);
        // gamba: schiniere, ginocchiera, scarpa d'acciaio
        MeshBuilder l = new MeshBuilder();
        FoeModels.tube(l, 0, 0.02f, 0, 0, -0.26f, 0, 0.07f, 0.06f, 10, FoeModels.flat(steel));
        FoeModels.ballZ(l, 0, -0.12f, 0.03f, 0.065f, 0.055f, 0.055f, 10, 6, FoeModels.flat(steelDk), 1f, 1f);
        FoeModels.ballZ(l, 0, -0.3f, 0.05f, 0.08f, 0.06f, 0.14f, 12, 8, FoeModels.flat(steel), 1f, 1f);
        col(l, leather); l.roundBox(0, -0.355f, 0.05f, 0.15f, 0.03f, 0.27f, 0.012f);
        Mesh leg = done(l, 0.7f, 46f);
        return new Mesh[][] {{body, shiny}, {arm}, {leg}};
    }

    // ------------------------------------------------------------------ astronauta: tuta bianca imbottita, casco tondo con visiera dorata, zaino con l'antenna

    private static Mesh[][] astronaut() {
        final float[] white = rgb(236, 238, 242), gray = rgb(150, 156, 168), dk = rgb(70, 74, 84), orange = rgb(240, 120, 40);
        MeshBuilder b = new MeshBuilder(), m = new MeshBuilder();
        col(b, white); b.ellipsoid(0, 0.63f, 0, 0.23f, 0.28f, 0.2f, 20, 12);
        col(b, white); b.ellipsoid(0, 0.42f, 0, 0.215f, 0.12f, 0.19f, 18, 8);
        col(b, orange); b.cylinder(0, 0.5f, 0.54f, 0, 0.215f, 0.22f, 20, false, false);
        col(b, gray); b.roundBox(0, 0.67f, 0.18f, 0.17f, 0.11f, 0.05f, 0.02f);
        float[][] btn = {{-0.05f, 0.69f}, {0f, 0.69f}, {0.05f, 0.69f}, {-0.03f, 0.645f}, {0.03f, 0.645f}};
        float[][] bc = {{230, 60, 50}, {60, 200, 90}, {60, 140, 240}, {250, 200, 60}, {240, 240, 240}};
        for (int i = 0; i < btn.length; i++) { col(b, rgb((int) bc[i][0], (int) bc[i][1], (int) bc[i][2])); b.box(btn[i][0], btn[i][1], 0.21f, 0.028f, 0.028f, 0.02f); }
        col(b, gray); b.cylinder(0, 0.86f, 0.93f, 0, 0.17f, 0.17f, 20, false, false);      // anello del collo
        col(b, white); b.ellipsoid(0, 1.1f, 0, 0.225f, 0.22f, 0.225f, 22, 14);             // casco
        // zaino con le bombole e l'antenna con la lucina
        col(b, white); b.roundBox(0, 0.7f, -0.25f, 0.38f, 0.44f, 0.16f, 0.05f);
        col(b, gray);
        b.cylinder(-0.1f, 0.5f, 0.92f, -0.34f, 0.055f, 0.055f, 12, true, true);
        b.cylinder(0.1f, 0.5f, 0.92f, -0.34f, 0.055f, 0.055f, 12, true, true);
        col(b, dk); b.cylinder(0.15f, 0.92f, 1.35f, -0.26f, 0.012f, 0.01f, 6, false, true);
        col(b, orange); b.box(0, 0.8f, -0.33f, 0.2f, 0.03f, 0.02f);
        Mesh body = done(b, 0.2f, 22f);
        col(m, rgb(220, 170, 60)); m.ellipsoid(0, 1.1f, 0.075f, 0.18f, 0.15f, 0.165f, 20, 12);   // visiera a specchio
        col(m, rgb(255, 60, 50)); m.ellipsoid(0.15f, 1.37f, -0.26f, 0.025f, 0.025f, 0.025f, 8, 5);
        Mesh shiny = done(m, 0.95f, 80f);
        // braccio imbottito con gli anelli e il guanto
        MeshBuilder a = new MeshBuilder();
        FoeModels.ballY(a, 0, 0, 0, 0.08f, 0.075f, 0.08f, 10, 6, FoeModels.flat(white), 1f, 1f);
        FoeModels.tube(a, 0, 0, 0, 0, -0.3f, 0, 0.072f, 0.065f, 10, FoeModels.flat(white));
        col(a, gray); a.cylinder(0, -0.16f, -0.13f, 0, 0.074f, 0.074f, 12, false, false);
        col(a, orange); a.cylinder(0, -0.3f, -0.27f, 0, 0.068f, 0.068f, 12, false, false);
        FoeModels.ballY(a, 0, -0.35f, 0.01f, 0.065f, 0.065f, 0.058f, 10, 6, FoeModels.flat(gray), 1f, 1f);
        Mesh arm = done(a, 0.2f, 22f);
        // gamba imbottita e scarpone
        MeshBuilder l = new MeshBuilder();
        FoeModels.tube(l, 0, 0.02f, 0, 0, -0.25f, 0, 0.082f, 0.072f, 10, FoeModels.flat(white));
        col(l, gray); l.cylinder(0, -0.14f, -0.11f, 0, 0.084f, 0.084f, 12, false, false);
        FoeModels.ballZ(l, 0, -0.3f, 0.04f, 0.09f, 0.07f, 0.14f, 12, 8, FoeModels.flat(gray), 1f, 1f);
        col(l, dk); l.roundBox(0, -0.355f, 0.04f, 0.17f, 0.035f, 0.28f, 0.012f);
        Mesh leg = done(l, 0.2f, 22f);
        return new Mesh[][] {{body, shiny}, {arm}, {leg}};
    }
}
