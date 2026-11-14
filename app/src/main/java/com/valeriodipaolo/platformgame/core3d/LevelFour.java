package com.valeriodipaolo.platformgame.core3d;

/**
 * Quarto livello, il bosco d'autunno: sentieri stretti tra i burroni, un ponte di una sola casella, nemici nuovi
 * (riccio spinoso, corvo in volo, ghianda saltellante) e un'insenatura nascosta con la cella energetica.
 */
public final class LevelFour {
    private LevelFour() {}

    private static final int W = World3D.W;

    public static void build(World3D w) {
        int len = 168;
        w.length = len;
        w.height = new int[len * W];
        w.crate = new boolean[len * W];
        w.gemsList.clear();
        w.enemies.clear();

        // --- avvio: sentiero di foglie, le prime zucche
        gemLine(w, 0, 4, 8);
        crate(w, 1, 11); crate(w, 3, 11);

        // --- primo burrone: restano due corsie laterali
        pit(w, 16, 19, 1, 3);
        gemLine(w, -2, 16, 19);
        gemLine(w, 2, 16, 19);

        // --- primi nemici nuovi, uno alla volta
        acorn(w, -1f, 24);
        stones(w, 28, new int[] {1, 1, 0, 0, 1});
        gem(w, 1, 1.8f, 28.5f);
        hog(w, 0, 31, false, -2f, 2f);
        crate(w, 2, 33);

        // --- slalom tra tre burroni a scacchi
        pit(w, 36, 38, 0, 2);
        pit(w, 41, 43, 2, 4);
        pit(w, 46, 48, 0, 2);
        gemLine(w, 2, 36, 38);
        gemLine(w, -2, 41, 43);
        gemLine(w, 2, 46, 48);

        // --- il corvo attraversa il sentiero
        crow(w, 0, 53, true, 51, 62);
        crate(w, 1, 55); crate(w, 3, 55);
        gemLine(w, 0, 57, 60);

        // --- burrone a tutta larghezza
        pit(w, 64, 67, 0, 4);
        gem(w, -1, 2.5f, 65f); gem(w, 1, 2.5f, 66f);

        // --- gradinata di massi con un riccio sulla terrazza
        for (int x = 0; x < W; x++) {
            set(w, x, 71, 1);
            for (int z = 72; z <= 77; z++) set(w, x, z, 2);
            set(w, x, 78, 1);
        }
        hogAt(w, 0, 74, 2f, true, 72.2f, 76.8f);
        gemLine3(w, -2, 2.6f, 72, 76);
        gemLine3(w, 2, 2.6f, 72, 76);
        acorn(w, 1f, 82);

        // --- insenatura segreta: la cella energetica sta su un masso, dentro una rientranza
        set(w, 0, 87, 1); set(w, 0, 88, 2); set(w, 0, 89, 2); set(w, 0, 90, 1);
        set(w, 1, 88, 1); set(w, 1, 89, 1);
        pit(w, 91, 94, 0, 1);
        w.battery = new float[] {-2, 2.7f, 88.5f};
        crate(w, 3, 88);
        hog(w, 3, 92, true, 86, 96.5f);

        // --- il ponte: una sola casella, con una ghianda che balza in mezzo
        pit(w, 98, 111, 0, 1);
        pit(w, 98, 111, 3, 4);
        acorn(w, 0, 103, true, 101, 108);
        gemLine(w, 0, 99, 101);
        gemLine(w, 0, 109, 111);
        crate(w, 2, 96);

        // --- due corvi a quote diverse
        crow(w, 0, 117, false, -2f, 2f);
        crow(w, 0, 121, false, -2f, 2f, 2.4f);
        crate(w, 0, 124); crate(w, 2, 124); crate(w, 4, 124);

        // --- radura della ronda: riccio, scarabei e ghiande
        hog(w, -1.5f, 128, true, 127, 137);
        w.enemies.add(patrol(0, 130, false, -2f, 2f));
        acorn(w, 1.5f, 133);
        hog(w, 1.5f, 138, true, 136, 143);
        gemLine(w, -2, 130, 134);
        gemLine(w, 2, 130, 134);

        // --- ultimi burroni
        stones(w, 144, new int[] {1, 0, 0, 0, 1});
        pit(w, 148, 150, 0, 4);
        gem(w, 0, 2.6f, 149.5f);
        pit(w, 154, 156, 0, 1);
        pit(w, 154, 156, 3, 4);
        crow(w, 0, 160, true, 158, 162, 1.9f);
        gemLine(w, 0, 154, 157);
        w.flagZ = 163;
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
