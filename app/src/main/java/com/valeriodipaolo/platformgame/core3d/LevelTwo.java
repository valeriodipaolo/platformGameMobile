package com.valeriodipaolo.platformgame.core3d;

/** Secondo livello: un corridoio nel bosco, largo 5 caselle, visto da dietro le spalle. */
public final class LevelTwo {
    private LevelTwo() {}

    private static final int W = World3D.W;

    public static void build(World3D w) {
        int len = 150;
        w.length = len;
        w.height = new int[len * W];
        w.crate = new boolean[len * W];
        w.gemsList.clear();
        w.enemies.clear();

        // --- avvio: prato libero e una fila di gemme
        gemLine(w, 0, 5, 9);
        // --- primo burrone, con gemme in volo
        pit(w, 15, 17, 0, 4);
        gem(w, 0, 2.3f, 15.5f); gem(w, 0, 2.7f, 16.5f); gem(w, 0, 2.3f, 17.5f);

        // --- muretti di pietra da aggirare o scavalcare
        stones(w, 22, new int[] {1, 1, 0, 1, 1});
        gem(w, -2, 1.8f, 22.5f); gem(w, 2, 1.8f, 22.5f);
        crate(w, 2, 25);
        stones(w, 28, new int[] {0, 1, 1, 1, 0});
        gem(w, 0, 1.8f, 28.5f);
        lateral(w, 31.5f);
        crate(w, 0, 33); crate(w, 4, 33);

        // --- burrone con un ponticello al centro
        pit(w, 36, 39, 0, 1);
        pit(w, 36, 39, 3, 4);
        gemLine(w, 0, 36, 39);

        // --- radura: cella energetica in bella vista, casse e scarabei
        forward(w, -1.5f, 42, 48);
        forward(w, 1.5f, 44, 50);
        crate(w, 1, 45); crate(w, 3, 47);
       
        gemLine(w, -2, 54, 57);
        gemLine(w, 2, 54, 57);

        // --- secondo burrone
        pit(w, 60, 62, 0, 4);
        gem(w, -1, 2.4f, 61.5f); gem(w, 1, 2.4f, 61.5f);

        // --- gradinata e terrazza di pietra
        for (int x = 0; x < W; x++) {
            set(w, x, 67, 1);
            for (int z = 68; z <= 73; z++) set(w, x, z, 2);
            set(w, x, 74, 1);
        }
        gemLine3(w, 0, 2.6f, 69, 72);
        lateralAt(w, 71.5f, 2f);
        crate(w, 2, 78);
        lateral(w, 81.5f);

        // --- terzo burrone
        pit(w, 85, 87, 0, 4);
        gem(w, 0, 2.5f, 86.5f);

        // --- sentiero stretto a zig-zag sopra il vuoto
        pit(w, 91, 97, 2, 4);
        pit(w, 100, 106, 0, 2);
        pit(w, 109, 114, 0, 0);
        pit(w, 109, 114, 4, 4);
        gemLine(w, -2, 92, 96);
        gemLine(w, 2, 101, 105);
        gemLine(w, 0, 110, 113);
        crate(w, 4, 98); crate(w, 0, 107);

        // --- tratto finale
        forward(w, 0, 118, 126);
        lateral(w, 123.5f);
        crate(w, 1, 128); crate(w, 2, 128); crate(w, 3, 128);
        stones(w, 133, new int[] {1, 0, 0, 0, 1});
        stones(w, 134, new int[] {1, 0, 0, 0, 1});
        lateral(w, 137.5f);
        gemLine(w, 0, 139, 142);
        w.flagZ = 145;
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

    /** Gemma alla colonna cx (da -2 a 2, 0 = centro) e quota y. */
    private static void gem(World3D w, float cx, float y, float z) {
        w.gemsList.add(new float[] {cx, y, z});
    }

    private static void gemLine(World3D w, float cx, int z0, int z1) {
        for (int z = z0; z <= z1; z++) gem(w, cx, 0.7f, z + 0.5f);
    }

    private static void gemLine3(World3D w, float cx, float y, int z0, int z1) {
        for (int z = z0; z <= z1; z++) gem(w, cx, y, z + 0.5f);
    }

    private static void lateral(World3D w, float z) {
        lateralAt(w, z, 0);
    }

    private static void lateralAt(World3D w, float z, float y) {
        Enemy3 e = new Enemy3(0, z, false, -2f, 2f);
        e.y = y;
        w.enemies.add(e);
    }

    private static void forward(World3D w, float x, float z0, float z1) {
        w.enemies.add(new Enemy3(x, z0, true, z0, z1));
    }
}
