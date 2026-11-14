package com.valeriodipaolo.platformgame.core3d;

/**
 * Decimo livello, il giardino di primavera: ciliegi in fiore, un campo di rovi da attraversare a zigzag, tre fiumi (isolotti con fiori a molla,
 * un guado di pietre), una passerella alta tra le chiome raggiungibile solo rimbalzando e un ultimo campo di rovi prima del portone.
 * Difficoltà come i livelli 6 e 8 al massimo: i rovi si evitano camminando, i fiumi hanno sempre un appoggio vicino.
 */
public final class LevelTen {
    private LevelTen() {}

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
        w.springs.clear();
        w.thorns.clear();
        w.fireStart = Float.NaN;
        w.fireZones = new float[0][];

        // --- prato d'ingresso: il primo fiore a molla con un arco di gemme sopra
        gemLine(w, 0, 0, 5);
        crate(w, 1, 8); crate(w, 3, 8);
        spring(w, 2, 12);
        arc(w, 0, 11, 19, 4.4f);
        acorn(w, -1f, 22);

        // --- campo di rovi a zigzag: si passa sempre dalle caselle libere, che cambiano a ogni fila
        int[] rowsA = {24, 27, 30, 33, 36};
        for (int i = 0; i < rowsA.length; i++) {
            boolean odd = i % 2 == 1;
            for (int x = 0; x < W; x++) {
                boolean thorn = odd ? (x % 2 == 0) : (x == 1 || x == 3);
                if (thorn) thorn(w, x, rowsA[i]);
                else gem(w, x - 2, 0.7f, rowsA[i] + 0.5f);
            }
        }
        hog(w, 0, 40, false, -2f, 2f);
        gemLine(w, 0, 38, 42);

        // --- primo fiume: due isolotti con un fiore a molla al centro
        pit(w, 44, 57, 0, 4);
        island(w, 47, 48, 1, 3);
        island(w, 52, 53, 1, 3);
        spring(w, 2, 47);
        spring(w, 2, 52);
        arc(w, 0, 45, 51, 4.0f);
        arc(w, 0, 50, 57, 4.4f);
        crow(w, 0, 50, false, -2f, 2f, 3.3f);

        // --- il ciliegio alto: da un fiore si rimbalza sulla passerella di pietre (tre metri) con la cella energetica
        spring(w, 2, 64);
        gem(w, 0, 1.8f, 65.5f); gem(w, 0, 3.4f, 66.5f); gem(w, 0, 4.3f, 67.5f); gem(w, 0, 4.5f, 68.5f); gem(w, 0, 4.3f, 69.5f);
        for (int z = 69; z <= 80; z++) for (int x = 1; x <= 3; x++) set(w, x, z, 3);
        set(w, 2, 81, 2); set(w, 2, 82, 1);
        gemLine3(w, 0, 3.7f, 70, 79);
        w.battery = new float[] {0, 4.3f, 75.5f};
        thorn(w, 0, 72); thorn(w, 4, 75); thorn(w, 0, 78);
        acorn(w, 1f, 86);
        gemLine(w, 0, 88, 96);

        // --- secondo fiume: il guado, tre isolotti di prato larghi e distanti tre caselle, a zigzag
        pit(w, 98, 117, 0, 4);
        island(w, 100, 102, 1, 3);
        island(w, 106, 108, 0, 2);
        island(w, 112, 114, 2, 4);
        gem(w, 0f, 0.7f, 101.5f); gem(w, -1f, 0.7f, 107.5f); gem(w, 1f, 0.7f, 113.5f);
        gem(w, -0.5f, 2.3f, 104.5f); gem(w, 0f, 2.3f, 110.5f);
        crow(w, 0, 121, false, -2f, 2f, 2.4f);

        // --- radura con ciliegi: riposo
        crate(w, 1, 122); crate(w, 3, 122);
        hog(w, 0, 126, false, -2f, 2f);
        acorn(w, 0f, 131);
        gemLine(w, 0, 120, 136);

        // --- l'ultimo campo di rovi: più fitto, con caselle libere a zigzag
        for (int i = 0; i < 7; i++) {
            int z = 140 + i * 2;
            boolean odd = i % 2 == 1;
            for (int x = 0; x < W; x++) {
                boolean thorn = odd ? (x % 2 == 0) : (x == 1 || x == 3);
                if (thorn) thorn(w, x, z);
                else gem(w, x - 2, 0.7f, z + 0.5f);
            }
        }
        crow(w, 0, 147, false, -2f, 2f, 2.4f);
        hog(w, 0, 156, false, -2f, 2f);
        gemLine(w, 0, 154, 160);

        // --- ultimo fiume e il portone
        pit(w, 162, 170, 0, 4);
        island(w, 165, 166, 1, 3);
        spring(w, 2, 165);
        arc(w, 0, 163, 169, 4.2f);
        acorn(w, 0f, 173);
        w.flagZ = 175;
    }

    /** Isolotto di prato (altezza 0) nelle caselle x0..x1 delle righe z0..z1, dentro un fiume. */
    private static void island(World3D w, int z0, int z1, int x0, int x1) {
        for (int z = z0; z <= z1; z++) for (int x = x0; x <= x1; x++) set(w, x, z, 0);
    }

    /** Fiore a molla al centro della casella (xi, z). */
    private static void spring(World3D w, int xi, int z) {
        w.springs.add(new float[] {xi - World3D.HALF + 0.5f, z + 0.5f});
    }

    /** Cespuglio di rovi al centro della casella (xi, z). */
    private static void thorn(World3D w, int xi, int z) {
        w.thorns.add(new float[] {xi - World3D.HALF + 0.5f, z + 0.5f});
    }

    /** Gemme su un arco che segue un rimbalzo: da z0 a z1 alla colonna di mondo cx, con il vertice alla quota apex. */
    private static void arc(World3D w, float cx, int zFrom, int zTo, float apex) {
        int n = zTo - zFrom + 1;
        for (int i = 0; i < n; i++) {
            float u = n == 1 ? 0.5f : (float) i / (n - 1);
            gem(w, cx, 0.7f + (apex - 0.7f) * 4f * u * (1f - u), zFrom + i + 0.5f);
        }
    }

    private static void set(World3D w, int x, int z, int h) {
        w.height[z * W + x] = h;
    }

    private static void pit(World3D w, int z0, int z1, int x0, int x1) {
        for (int z = z0; z <= z1; z++) for (int x = x0; x <= x1; x++) set(w, x, z, World3D.PIT);
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

    /** Riccio: lungo il corridoio (alongZ) tra z lo e hi, oppure di traverso tra x lo e hi. */
    private static void hog(World3D w, float x, float z, boolean alongZ, float lo, float hi) {
        Enemy3 e = new Enemy3(x, z, alongZ, lo, hi);
        e.type = Enemy3.HOG;
        w.enemies.add(e);
    }

    private static void acorn(World3D w, float x, float z) {
        Enemy3 e = new Enemy3(x, z, false, -2f, 2f);
        e.type = Enemy3.ACORN;
        w.enemies.add(e);
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
