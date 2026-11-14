package com.valeriodipaolo.platformgame.core3d;

/**
 * Ottavo livello, il bosco in fiamme: il fuoco ha aperto fiumi di lava nel sentiero. Burroni stretti, un fiume da attraversare
 * di masso in masso, un ponte a zig-zag, una rientranza segreta con la cella energetica e una ronda di nemici tra gli alberi in fiamme.
 */
public final class LevelEight {
    private LevelEight() {}

    private static final int W = World3D.W;

    public static void build(World3D w) {
        int len = 176;
        w.length = len;
        w.height = new int[len * W];
        w.crate = new boolean[len * W];
        w.gemsList.clear();
        w.enemies.clear();

        // --- avvio: terra bruciata e le prime casse
        gemLine(w, 0, 0, 4);
        gemLine(w, 2, 5, 9);
        crate(w, 1, 11); crate(w, 3, 11);
        acorn(w, -1f, 13);

        // --- primi spacchi di lava: uno per lato, poi uno al centro
        pit(w, 17, 20, 0, 1);
        pit(w, 17, 20, 3, 4);
        gemLine(w, 0, 17, 20);
        hog(w, 0, 24, false, -2f, 2f);
        stones(w, 27, new int[] {0, 1, 1, 1, 0});
        pit(w, 31, 33, 1, 3);
        gem(w, -2, 1.8f, 32.5f); gem(w, 2, 1.8f, 32.5f);

        // --- tronchi in fiamme: barricata di massi a gradini
        stones(w, 38, new int[] {1, 1, 0, 1, 1});
        stones(w, 40, new int[] {2, 1, 0, 1, 2});
        crow(w, 0, 44, true, 42, 52);
        crate(w, 1, 46); crate(w, 3, 46);
        acorn(w, -1f, 49);
        gemLine(w, 2, 50, 54);

        // --- crepaccio di lava a tutta larghezza
        pit(w, 58, 61, 0, 4);
        gem(w, -1, 2.5f, 59.2f); gem(w, 1, 2.5f, 60.2f);
        hog(w, 1, 65, false, -2f, 2f);
        acorn(w, -1f, 68);

        // --- il fiume di lava: massi sparsi nella corrente
        pit(w, 72, 91, 0, 4);
        floe(w, 2, 74); floe(w, 1, 77); floe(w, 3, 80); floe(w, 2, 83); floe(w, 0, 86); floe(w, 2, 89);
        floe(w, 3, 74); floe(w, 0, 80); floe(w, 4, 83);
        for (int i = 0; i < 6; i++) gem(w, new float[] {0, -1, 1, 0, -2, 0}[i], 1.9f, 74.5f + i * 3f);
        crow(w, 0, 81, false, -2f, 2f, 2.8f);

        // --- rientranza segreta: la cella energetica sta su un masso, al riparo dalle fiamme
        set(w, 0, 97, 1); set(w, 0, 98, 2); set(w, 0, 99, 2); set(w, 0, 100, 1);
        set(w, 1, 98, 1); set(w, 1, 99, 1);
        pit(w, 101, 104, 0, 1);
        w.battery = new float[] {-2, 2.7f, 98.5f};
        crate(w, 3, 98);
        hog(w, 1.5f, 102, true, 96, 106.5f);

        // --- ponte a zig-zag sulla lava: una sola casella, che cambia corsia
        pit(w, 108, 127, 0, 4);
        lane(w, 108, 111, 2); lane(w, 112, 115, 3); lane(w, 116, 119, 1); lane(w, 120, 123, 3); lane(w, 124, 127, 2);
        set(w, 2, 112, 0); set(w, 3, 112, 0);
        set(w, 3, 116, 0); set(w, 1, 116, 0); set(w, 2, 116, 0);
        set(w, 1, 120, 0); set(w, 2, 120, 0); set(w, 3, 120, 0);
        set(w, 3, 124, 0); set(w, 2, 124, 0);
        gem(w, 0, 0.7f, 109.5f); gem(w, 1, 0.7f, 113.5f); gem(w, -1, 0.7f, 117.5f); gem(w, 1, 0.7f, 121.5f); gem(w, 0, 0.7f, 125.5f);
        acorn(w, 1, 114, true, 112, 115.5f);

        // --- la foresta che brucia: ronda di nemici
        crate(w, 0, 132); crate(w, 2, 132); crate(w, 4, 132);
        hog(w, -1.5f, 135, true, 131, 141);
        w.enemies.add(patrol(0, 138, false, -2f, 2f));
        crow(w, 0, 141, false, -2f, 2f, 2.3f);
        acorn(w, 1.5f, 144);
        hog(w, 1.5f, 147, true, 142, 150);
        gemLine(w, -2, 134, 140);
        gemLine(w, 2, 134, 140);

        // --- ultimi ostacoli
        stones(w, 152, new int[] {1, 0, 0, 0, 1});
        pit(w, 156, 159, 0, 4);
        floe(w, 2, 157);
        gem(w, 0, 2.2f, 157.5f);
        pit(w, 164, 166, 0, 1);
        pit(w, 164, 166, 3, 4);
        crow(w, 0, 170, true, 168, 172, 1.9f);
        gemLine(w, 0, 164, 167);
        w.flagZ = 172;
    }

    /** Masso nella lava: una casella di terra in mezzo al lago. */
    private static void floe(World3D w, int x, int z) {
        set(w, x, z, 0);
        set(w, x, z + 1, 0);
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
