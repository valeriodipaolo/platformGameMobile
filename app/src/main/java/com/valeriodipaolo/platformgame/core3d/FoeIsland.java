package com.valeriodipaolo.platformgame.core3d;

/**
 * Nemici dell'isola tropicale, modellati come tutti gli altri (solidi morbidi e colori pieni) ma con un comportamento preso in prestito:
 * il granchio corre di traverso come uno scarabeo veloce, il totem tiki si volta e tira noci di cocco come il pupazzo di neve,
 * il piranha balza fuori dall'acqua come il lapillo dalla lava.
 */
final class FoeIsland {
    private final float[] a = new float[16], b2 = new float[16];
    private final Mesh crabBody, crabEyes, crabLeg, crabClaw, tikiBody, tikiFace, tikiLeaves, fishBody, fishFin, fishTeeth, coconut;
    private static final float PI = (float) Math.PI;

    FoeIsland() {
        // ---- granchio: carapace largo e basso, occhi sugli steli, sei zampe a punta e due chele grosse
        final float[] red = FoeModels.rgb(226, 72, 40), redD = FoeModels.rgb(150, 36, 22), cream = FoeModels.rgb(250, 214, 170);
        MeshBuilder b = new MeshBuilder();
        b.colorf(red[0], red[1], red[2]);
        b.ellipsoid(0, 0.3f, 0, 0.42f, 0.2f, 0.32f, 16, 9);
        b.colorf(redD[0], redD[1], redD[2]);
        b.ellipsoid(0, 0.24f, 0.02f, 0.4f, 0.12f, 0.3f, 14, 6);
        b.colorf(cream[0], cream[1], cream[2]);
        b.ellipsoid(0, 0.2f, 0.12f, 0.26f, 0.08f, 0.16f, 12, 6);
        crabBody = done(b, 0.35f, 22f);
        b = new MeshBuilder();
        for (int s = -1; s <= 1; s += 2) {
            b.colorf(red[0], red[1], red[2]);
            b.cylinder(s * 0.12f, 0.36f, 0.62f, 0.2f, 0.025f, 0.02f, 6, false, false);
            b.colorf(1f, 1f, 1f);
            b.ellipsoid(s * 0.12f, 0.66f, 0.2f, 0.07f, 0.07f, 0.07f, 10, 7);
            b.colorf(0.05f, 0.05f, 0.08f);
            b.ellipsoid(s * 0.12f, 0.67f, 0.26f, 0.035f, 0.035f, 0.02f, 8, 5);
        }
        crabEyes = done(b, 0.6f, 30f);
        b = new MeshBuilder();
        b.colorf(red[0], red[1], red[2]);
        FoeModels.tube(b, new float[][] {{0, 0, 0}, {0.18f, 0.08f, 0}, {0.32f, -0.2f, 0}}, new float[] {0.04f, 0.035f, 0.012f}, 6, FoeModels.flat(red));
        crabLeg = done(b, 0.3f, 20f);
        b = new MeshBuilder();
        FoeModels.tube(b, new float[][] {{0, 0, 0}, {0.12f, 0.06f, 0.12f}, {0.16f, 0.12f, 0.24f}}, new float[] {0.045f, 0.06f, 0.07f}, 8, FoeModels.flat(red));
        b.colorf(red[0], red[1], red[2]);
        b.ellipsoid(0.17f, 0.14f, 0.32f, 0.12f, 0.09f, 0.13f, 12, 7);
        b.colorf(cream[0], cream[1], cream[2]);
        b.ellipsoid(0.13f, 0.11f, 0.43f, 0.035f, 0.04f, 0.08f, 8, 5);
        b.ellipsoid(0.21f, 0.11f, 0.43f, 0.035f, 0.04f, 0.08f, 8, 5);
        crabClaw = done(b, 0.35f, 22f);

        // ---- totem tiki: tronco intagliato con la faccia arrabbiata e un ciuffo di foglie in testa
        final float[] wood = FoeModels.rgb(150, 98, 56), woodD = FoeModels.rgb(96, 58, 30);
        b = new MeshBuilder();
        b.colorf(wood[0], wood[1], wood[2]);
        b.cylinder(0, 0, 1.25f, 0, 0.34f, 0.3f, 14, true, true);
        b.colorf(woodD[0], woodD[1], woodD[2]);
        for (int i = 0; i < 3; i++) b.cylinder(0, 0.18f + i * 0.4f, 0.24f + i * 0.4f, 0, 0.355f, 0.355f, 14, false, false);
        b.colorf(wood[0] * 1.1f, wood[1] * 1.1f, wood[2] * 1.1f);
        b.box(0, 1.05f, 0.28f, 0.16f, 0.32f, 0.14f);                 // naso
        tikiBody = done(b, 0.15f, 12f);
        b = new MeshBuilder();
        b.colorf(0.98f, 0.92f, 0.62f);
        b.box(-0.14f, 1.12f, 0.3f, 0.14f, 0.09f, 0.05f);
        b.box(0.14f, 1.12f, 0.3f, 0.14f, 0.09f, 0.05f);
        b.colorf(0.9f, 0.16f, 0.1f);
        b.box(-0.14f, 1.11f, 0.33f, 0.06f, 0.06f, 0.03f);
        b.box(0.14f, 1.11f, 0.33f, 0.06f, 0.06f, 0.03f);
        b.colorf(0.16f, 0.08f, 0.04f);
        b.box(0, 0.78f, 0.3f, 0.36f, 0.14f, 0.06f);                   // bocca spalancata
        b.colorf(0.98f, 0.96f, 0.86f);
        for (int i = -2; i <= 2; i++) b.box(i * 0.07f, 0.84f, 0.32f, 0.04f, 0.05f, 0.02f);
        b.colorf(0.1f, 0.05f, 0.02f);
        b.box(-0.14f, 1.22f, 0.3f, 0.2f, 0.04f, 0.05f);
        b.box(0.14f, 1.22f, 0.3f, 0.2f, 0.04f, 0.05f);
        tikiFace = done(b, 0.4f, 26f);
        b = new MeshBuilder();
        for (int i = 0; i < 7; i++) {
            float an = i * 2 * PI / 7, ca = (float) Math.cos(an), sa = (float) Math.sin(an);
            FoeModels.tube(b, new float[][] {{0, 1.24f, 0}, {ca * 0.24f, 1.5f, sa * 0.24f}, {ca * 0.5f, 1.38f, sa * 0.5f}},
                    new float[] {0.05f, 0.07f, 0.01f}, 5, FoeModels.flat(FoeModels.rgb(56, 158, 56)));
        }
        tikiLeaves = done(b, 0.2f, 12f);

        // ---- piranha: corpo a goccia verde-oro, pancia rossa, denti fuori
        final float[] green = FoeModels.rgb(70, 120, 90), belly = FoeModels.rgb(232, 70, 50), fin = FoeModels.rgb(46, 84, 70);
        b = new MeshBuilder();
        FoeModels.ballZ(b, 0, 0, 0, 0.26f, 0.32f, 0.42f, 16, 10, FoeModels.flat(green), 0, 0);
        b.colorf(belly[0], belly[1], belly[2]);
        b.ellipsoid(0, -0.12f, 0.05f, 0.2f, 0.2f, 0.32f, 12, 7);
        b.colorf(1f, 0.9f, 0.3f);
        b.ellipsoid(-0.16f, 0.1f, 0.24f, 0.07f, 0.07f, 0.06f, 8, 6);
        b.ellipsoid(0.16f, 0.1f, 0.24f, 0.07f, 0.07f, 0.06f, 8, 6);
        b.colorf(0.05f, 0.03f, 0.03f);
        b.ellipsoid(-0.19f, 0.11f, 0.27f, 0.035f, 0.035f, 0.03f, 6, 4);
        b.ellipsoid(0.19f, 0.11f, 0.27f, 0.035f, 0.035f, 0.03f, 6, 4);
        fishBody = done(b, 0.55f, 30f);
        b = new MeshBuilder();
        b.colorf(fin[0], fin[1], fin[2]);
        b.quad(0, 0.05f, -0.36f, 0, 0.32f, -0.66f, 0, -0.26f, -0.66f, 0, -0.05f, -0.36f, 1, 0, 0, 0, 0, 1, 1);
        b.quad(0, 0.28f, -0.05f, 0, 0.46f, -0.2f, 0, 0.3f, -0.3f, 0, 0.26f, -0.2f, 1, 0, 0, 0, 0, 1, 1);
        fishFin = done(b, 0.2f, 12f);
        b = new MeshBuilder();
        b.colorf(1f, 1f, 0.96f);
        for (int i = -3; i <= 3; i++) {
            b.box(i * 0.045f, 0.0f, 0.4f, 0.03f, 0.06f, 0.03f);
            b.box(i * 0.045f, -0.08f, 0.39f, 0.03f, 0.06f, 0.03f);
        }
        fishTeeth = done(b, 0.6f, 30f);

        b = new MeshBuilder();                                       // noce di cocco
        b.colorf(0.42f, 0.27f, 0.13f);
        b.ellipsoid(0, 0, 0, 0.19f, 0.2f, 0.19f, 12, 8);
        b.colorf(0.18f, 0.1f, 0.05f);
        b.ellipsoid(0.04f, 0.15f, 0.08f, 0.04f, 0.04f, 0.04f, 6, 4);
        b.ellipsoid(-0.05f, 0.15f, 0.07f, 0.04f, 0.04f, 0.04f, 6, 4);
        coconut = done(b, 0.15f, 10f);
    }

    private static Mesh done(MeshBuilder b, float spec, float shine) {
        Mesh m = b.build(-1, false, true);
        m.spec = spec; m.shine = shine;
        return m;
    }

    void drawShot(Gfx3D g, float[] m, float[] s) {
        System.arraycopy(m, 0, a, 0, 16);
        Mat4.translate(a, s[0], s[1], s[2]);
        Mat4.rotateX(a, s[2] * 3f);
        g.draw(coconut, a, 1, 1, 1, 1, 0);
    }

    void draw(Gfx3D g, float[] m, Enemy3 e) {
        float an = e.anim;
        switch (e.skin) {
            case Enemy3.SKIN_CRAB: {
                // il modello guarda verso il robot, ma il granchio cammina di lato: si gira di 90° e muove le zampe a scatti
                System.arraycopy(m, 0, a, 0, 16);
                Mat4.translate(a, 0, 0.02f * Math.abs((float) Math.sin(an * 14f)), 0);
                g.draw(crabBody, a, 1, 1, 1, 1, 0.05f);
                g.draw(crabEyes, a, 1, 1, 1, 1, 0.2f);
                for (int i = 0; i < 6; i++) {
                    int s = i < 3 ? -1 : 1;
                    float z = -0.16f + (i % 3) * 0.14f, sw = (float) Math.sin(an * 14f + i * 2.1f) * 0.35f;
                    System.arraycopy(a, 0, b2, 0, 16);
                    Mat4.translate(b2, s * 0.32f, 0.26f, z);
                    Mat4.rotateY(b2, s > 0 ? sw : PI - sw);
                    g.draw(crabLeg, b2, 1, 1, 1, 1, 0);
                }
                float snap = 0.3f * Math.max(0, (float) Math.sin(an * 5f));
                for (int s = -1; s <= 1; s += 2) {
                    System.arraycopy(a, 0, b2, 0, 16);
                    Mat4.translate(b2, s * 0.26f, 0.24f, 0.18f);
                    if (s < 0) Mat4.scale(b2, -1, 1, 1);
                    Mat4.rotateX(b2, -snap);
                    g.draw(crabClaw, b2, 1, 1, 1, 1, 0.05f);
                }
                break;
            }
            case Enemy3.SKIN_TIKI: {
                System.arraycopy(m, 0, a, 0, 16);
                float throwK = e.t < 0.35f ? (float) Math.sin(e.t / 0.35f * PI) : 0f;      // appena tirato: si piega in avanti
                Mat4.translate(a, 0, 0.05f * Math.abs((float) Math.sin(an * 3f)), 0);
                Mat4.rotateX(a, 0.25f * throwK);
                g.draw(tikiBody, a, 1, 1, 1, 1, 0);
                g.draw(tikiFace, a, 1, 1, 1, 1, 0.3f + 0.4f * throwK);
                g.draw(tikiLeaves, a, 1, 1, 1, 1, 0.05f);
                break;
            }
            default: {                                                  // piranha: si avvita in volo e spalanca la bocca
                System.arraycopy(m, 0, a, 0, 16);
                Mat4.translate(a, 0, 0.3f, 0);
                float per = e.lo > 0 ? e.lo : 2.6f, u = Math.max(0f, Math.min(1f, (e.t % per - (per - 0.9f)) / 0.9f));
                Mat4.rotateX(a, -1.2f * (1f - 2f * u));            // muso in su salendo, in giù ricadendo
                Mat4.rotateZ(a, 0.25f * (float) Math.sin(an * 9f));
                g.draw(fishBody, a, 1, 1, 1, 1, 0.08f);
                g.draw(fishTeeth, a, 1, 1, 1, 1, 0.3f);
                System.arraycopy(a, 0, b2, 0, 16);
                Mat4.rotateY(b2, 0.4f * (float) Math.sin(an * 16f));
                g.draw(fishFin, b2, 1, 1, 1, 1, 0);
                break;
            }
        }
    }
}
