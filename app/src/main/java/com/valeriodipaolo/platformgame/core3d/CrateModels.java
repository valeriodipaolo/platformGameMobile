package com.valeriodipaolo.platformgame.core3d;

/** Al posto della cassa con la freccia: una grossa molla d'acciaio su un basamento di legno, con il cuscinetto di gomma rossa. */
final class CrateModels {
    private CrateModels() {}

    /** Molla da trampolino alta quasi un blocco (la cima del cuscinetto a 0,95 m, dove si atterra). */
    static Mesh spring() {
        MeshBuilder b = new MeshBuilder();
        b.colorf(0.55f, 0.38f, 0.22f);                                      // basamento di assi con la cornice scura
        b.roundBox(0, 0.07f, 0, 0.9f, 0.14f, 0.9f, 0.03f, 63, 1);
        b.colorf(0.34f, 0.22f, 0.12f);
        for (int s = -1; s <= 1; s += 2) { b.box(s * 0.43f, 0.15f, 0, 0.05f, 0.03f, 0.9f); b.box(0, 0.15f, s * 0.43f, 0.9f, 0.03f, 0.05f); }
        b.colorf(0.3f, 0.3f, 0.33f);
        b.cylinder(0, 0.14f, 0.2f, 0, 0.36f, 0.36f, 20, true, false);         // piatto d'acciaio della molla
        b.colorf(0.78f, 0.8f, 0.84f);                                        // spirale: quattro giri di filo spesso
        int seg = 64;
        float r = 0.3f, y0 = 0.2f, y1 = 0.74f;
        for (int i = 0; i < seg; i++) {
            double a0 = i * Math.PI * 8 / seg, a1 = (i + 1) * Math.PI * 8 / seg;
            float ya = y0 + (y1 - y0) * i / seg, yb = y0 + (y1 - y0) * (i + 1) / seg;
            MineModels.beamY(b, (float) Math.cos(a0) * r, ya, (float) Math.sin(a0) * r, (float) Math.cos(a1) * r, yb, (float) Math.sin(a1) * r, 0.035f);
        }
        b.colorf(0.3f, 0.3f, 0.33f);
        b.cylinder(0, 0.74f, 0.78f, 0, 0.38f, 0.38f, 20, true, true);         // piatto superiore
        b.colorf(0.82f, 0.14f, 0.1f);                                        // cuscinetto di gomma rossa, bombato
        b.cylinder(0, 0.78f, 0.9f, 0, 0.42f, 0.42f, 24, false, true);
        b.cylinder(0, 0.9f, 0.95f, 0, 0.42f, 0.34f, 24, true, false);
        b.colorf(0.96f, 0.92f, 0.88f);                                       // bordo bianco del cuscinetto
        b.cylinder(0, 0.84f, 0.87f, 0, 0.425f, 0.425f, 24, false, false);
        Mesh m = b.build(-1, false, true);
        m.spec = 0.45f;
        m.shine = 28f;
        return m;
    }
}
