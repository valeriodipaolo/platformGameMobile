package com.valeriodipaolo.platformgame.core3d;

/**
 * Ottavo livello, il bosco in fiamme: la fuga attraverso il rogo. Un muro di fuoco insegue da dietro; tronchi in fiamme rotolano
 * di traverso, un slalom tra muri di tronchi bruciati, un braciere con getti di fiamma a scacchiera, un ponte sulla lava
 * attraversato dai tronchi, una radura dove il fuoco si ferma e il rogo finale tra due strisce di lava.
 */
public final class LevelEight {
    private LevelEight() {}

    private static final int W = World3D.W;

    public static void build(World3D w) {
        int len = 178;
        w.length = len;
        w.height = new int[len * W];
        w.crate = new boolean[len * W];
        w.gemsList.clear();
        w.enemies.clear();
        w.jets.clear();
        w.logs.clear();

        w.fireStart = -14f;
        w.fireZones = new float[][] {{10, 1.7f, 20}, {50, 2.1f, 20}, {108, 0f}, {124, 2.5f, 18}, {150, 2.9f, 15}};

        // --- avvio
        gemLine(w, 0, 0, 4);
        gemLine(w, 2, 5, 9);
        crate(w, 1, 10); crate(w, 3, 10);

        // --- tronchi che rotolano: quattro tronchi in fiamme attraversano il sentiero a ritmi diversi
        log(w, 16, 3.8f, 0f);
        log(w, 21, 3.8f, 1.9f);
        log(w, 26, 3.2f, 0.5f);
        log(w, 31, 3.2f, 2.1f);
        gem(w, 0, 0.7f, 18.5f); gem(w, 0, 0.7f, 23.5f); gem(w, 0, 0.7f, 28.5f); gem(w, 0, 0.7f, 33f);
        acorn(w, -1f, 34);

        // --- slalom tra muri di tronchi bruciati: ognuno ha un solo varco, sempre dal lato opposto al precedente
        wall(w, 40, 4);
        wall(w, 44, 0);
        wall(w, 48, 3);
        wall(w, 52, 1);
        wall(w, 56, 4);
        jet(w, 2, 42, 2.8f, 0f);
        jet(w, 2, 46, 2.8f, 1.4f);
        hog(w, 0, 50, false, -2f, 2f);
        jet(w, 3, 54, 2.8f, 0.7f);
        crow(w, 0, 58, false, -2f, 2f, 2.1f);
        gem(w, 2, 0.7f, 40.5f); gem(w, -2, 0.7f, 44.5f); gem(w, 1, 0.7f, 48.5f); gem(w, -1, 0.7f, 52.5f); gem(w, 2, 0.7f, 56.5f);

        // --- braciere: righe di getti a scacchiera, due per riga, accesi a turno
        for (int i = 0; i < 7; i++) {
            int a = (i * 2) % 5, b = (a + 2) % 5;
            float ph = (i % 2) * 1.4f;
            jet(w, a, 66 + i * 3, 2.8f, ph);
            jet(w, b, 66 + i * 3, 2.8f, ph + 1.4f);
        }
        crow(w, 0, 76, false, -2f, 2f, 2.2f);
        acorn(w, 0f, 87);
        gemLine(w, 2, 64, 86);

        // --- ponte sulla lava: tre caselle di larghezza, attraversato dai tronchi in fiamme
        pit(w, 90, 111, 0, 4);
        for (int z = 90; z <= 111; z++) for (int x = 1; x <= 3; x++) set(w, x, z, 0);
        log(w, 94, 3.4f, 0f);
        log(w, 99, 3.4f, 1.7f);
        log(w, 104, 3.0f, 0.8f);
        gem(w, 0, 0.7f, 96.5f); gem(w, 0, 0.7f, 101.5f); gem(w, 1, 0.7f, 107f); gem(w, -1, 0.7f, 109f);

        // --- radura: qui il fuoco si ferma. Una scala di massi porta alla cella energetica
        set(w, 0, 114, 1); set(w, 0, 115, 2); set(w, 0, 116, 3); set(w, 0, 117, 3); set(w, 0, 118, 2);
        w.battery = new float[] {-2, 4.3f, 116.5f};
        crate(w, 3, 119); crate(w, 4, 119); crate(w, 2, 121);
        gemLine(w, 1, 112, 118);

        // --- il rogo: due strisce di lava ai lati stringono il sentiero a tre caselle
        pit(w, 128, 151, 0, 0);
        pit(w, 128, 151, 4, 4);
        log(w, 130, 3.0f, 0f);
        log(w, 134, 3.0f, 1.5f);
        jet(w, 2, 138, 2.4f, 0f);
        jet(w, 1, 141, 2.4f, 1.2f);
        jet(w, 3, 141, 2.4f, 0f);
        jet(w, 2, 144, 2.4f, 1.2f);
        hog(w, 0, 147, true, 145, 151);
        acorn(w, 1f, 136);
        gemLine(w, 0, 130, 150);

        // --- ultimi muri e tronchi
        wall(w, 156, 2);
        wall(w, 160, 0);
        wall(w, 164, 4);
        jet(w, 3, 158, 2.6f, 0f);
        jet(w, 1, 162, 2.6f, 1.3f);
        log(w, 168, 2.6f, 0f);
        log(w, 171, 2.6f, 1.3f);
        crate(w, 1, 153); crate(w, 3, 153);
        gem(w, 0, 0.7f, 157.5f); gem(w, -2, 0.7f, 161.5f); gem(w, 2, 0.7f, 165.5f);
        w.flagZ = 175;
    }

    /** Muro basso di tronchi bruciati (una casella, così non nasconde nulla alla camera), con un solo varco in corrispondenza della casella gap. */
    private static void wall(World3D w, int z, int gap) {
        for (int x = 0; x < W; x++) set(w, x, z, x == gap ? 0 : 1);
    }

    /** Getto di fiamma nella casella (x, z): acceso il 40% del periodo, con un breve avviso prima. */
    private static void jet(World3D w, int xi, int z, float period, float phase) {
        w.jets.add(new float[] {xi - World3D.HALF + 0.5f, z + 0.5f, period, phase});
    }

    /** Tronco in fiamme che rotola da un lato all'altro del sentiero alla quota z. */
    private static void log(World3D w, int z, float period, float phase) {
        w.logs.add(new float[] {z + 0.5f, period, phase});
    }

    /** Corsia di una casella dentro un'area di burrone. */
    private static void lane(World3D w, int z0, int z1, int x) {
        for (int z = z0; z <= z1; z++) set(w, x, z, 0);
    }

    private static void set(World3D w, int x, int z, int h) {
        w.height[z * W + x] = h;
    }

    private static void pit(World3D w, int z0, int z1, int x0, int x1) {
        for (int z = z0; z <= z1; z++) for (int x = x0; x <= x1; x++) set(w, x, z, World3D.PIT);
    }

    private static void stones(World3D w, int z, int[] h) {
        for (int x = 0; x < W; x++) set(w, x, z, h[x]);
    }

    private static void crate(World3D w, int x, int z) {
        w.crate[z * W + x] = true;
    }

    private static void gem(World3D w, float cx, float y, float z) {
        w.gemsList.add(new float[] {cx, y, z});
    }

    private static void gemLine(World3D w, float cx, int z0, int z1) {
        for (int z = z0; z <= z1; z++) gem(w, cx, 0.7f, z + 0.5f);
    }

    private static void gemLine3(World3D w, float cx, float y, int z0, int z1) {
        for (int z = z0; z <= z1; z++) gem(w, cx, y, z + 0.5f);
    }

    private static Enemy3 patrol(float x, float z, boolean alongZ, float lo, float hi) {
        Enemy3 e = new Enemy3(x, z, alongZ, lo, hi);
        return e;
    }

    /** Riccio: lungo il corridoio (alongZ) tra z lo e hi, oppure di traverso tra x lo e hi. */
    private static void hog(World3D w, float x, float z, boolean alongZ, float lo, float hi) {
        Enemy3 e = new Enemy3(x, z, alongZ, lo, hi);
        e.type = Enemy3.HOG;
        w.enemies.add(e);
    }

    private static void hogAt(World3D w, float x, float z, float y, boolean alongZ, float lo, float hi) {
        Enemy3 e = new Enemy3(x, z, alongZ, lo, hi);
        e.type = Enemy3.HOG;
        e.y = y;
        e.baseY = y;
        w.enemies.add(e);
    }

    /** Ghianda che balza di traverso nella corsia. */
    private static void acorn(World3D w, float x, float z) {
        acorn(w, x, z, false, -2f, 2f);
    }

    private static void acorn(World3D w, float x, float z, boolean alongZ, float lo, float hi) {
        Enemy3 e = new Enemy3(x, z, alongZ, lo, hi);
        e.type = Enemy3.ACORN;
        w.enemies.add(e);
    }

    private static void crow(World3D w, float x, float z, boolean alongZ, float lo, float hi) {
        crow(w, x, z, alongZ, lo, hi, 1.9f);
    }

    /** Corvo: vola a onde sopra al sentiero, a quota media y. */
    private static void crow(World3D w, float x, float z, boolean alongZ, float lo, float hi, float y) {
        Enemy3 e = new Enemy3(x, z, alongZ, lo, hi);
        e.type = Enemy3.CROW;
        e.y = y;
        e.baseY = y;
        w.enemies.add(e);
    }
}
