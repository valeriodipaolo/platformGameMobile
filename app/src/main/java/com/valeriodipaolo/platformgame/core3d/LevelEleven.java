package com.valeriodipaolo.platformgame.core3d;

/**
 * Undicesimo livello, la fuga dalle api (primavera, 3D): uno sciame inferocito insegue da dietro e non si ferma. Si corre a perdifiato
 * raccogliendo le gemme lungo il percorso: tre campi di rovi a zigzag con una corsia centrale sempre libera, tre fiumi larghi dieci metri
 * (la fila di fiori a molla in riva lancia dall'altra parte senza perdere un istante) e qualche ghianda e riccio di traverso. Lo sciame accelera
 * piano piano ma resta sempre più lento di chi corre: gli ostacoli sono l'unico modo per farsi raggiungere.
 */
public final class LevelEleven {
    private LevelEleven() {}

    private static final int W = World3D.W;

    public static void build(World3D w) {
        int len = 172;
        w.length = len;
        w.height = new int[len * W];
        w.crate = new boolean[len * W];
        w.gemsList.clear();
        w.enemies.clear();
        w.jets.clear();
        w.logs.clear();
        w.springs.clear();
        w.thorns.clear();
        w.chase = 1;
        w.fireStart = -14f;
        w.fireZones = new float[][] {{0, 2.3f, 16}, {60, 2.8f, 15}, {120, 3.1f, 14}};

        // --- via: una fila di gemme per prendere il ritmo, due casse
        group(w, 0, 5f, 4);
        crate(w, 1, 12); crate(w, 3, 12);
        group(w, 0, 15f, 3);

        // --- primo campo di rovi: a destra, a sinistra, a destra; al centro si passa sempre
        thornRow(w, 22, 0, 1);
        thornRow(w, 27, 3, 4);
        thornRow(w, 32, 0, 1);
        gem(w, 0, 0.7f, 24.5f); gem(w, 1.5f, 0.7f, 24.5f); gem(w, 0, 0.7f, 29.5f); gem(w, -1.5f, 0.7f, 29.5f);

        // --- primo fiume
        padRow(w, 38);
        pit(w, 39, 48, 0, 4);
        flight(w, 38);
        group(w, 0, 51f, 3);
        hog(w, 0, 58, false, -2f, 2f);
        crate(w, 1, 62); crate(w, 3, 62);
        acorn(w, 0f, 67);
        group(w, 0, 69f, 2);

        // --- secondo campo di rovi, quattro file
        thornRow(w, 72, 3, 4);
        thornRow(w, 76, 0, 1);
        thornRow(w, 80, 3, 4);
        thornRow(w, 84, 0, 1);
        gem(w, 0, 0.7f, 74.5f); gem(w, -1.5f, 0.7f, 74.5f); gem(w, 0, 0.7f, 78.5f); gem(w, 1.5f, 0.7f, 78.5f); gem(w, 0, 0.7f, 82.5f);
        group(w, 0, 86.5f, 2);
        crow(w, 0, 90, false, -2f, 2f, 2.4f);
        group(w, 0, 92.5f, 2);

        // --- secondo fiume
        padRow(w, 96);
        pit(w, 97, 106, 0, 4);
        flight(w, 96);
        group(w, 0, 109f, 4);
        crate(w, 1, 115); crate(w, 3, 115);
        hog(w, 0, 119, false, -2f, 2f);
        acorn(w, 0f, 123);
        group(w, 0, 125.5f, 2);

        // --- terzo campo di rovi
        thornRow(w, 128, 0, 1);
        thornRow(w, 132, 3, 4);
        thornRow(w, 136, 0, 1);
        gem(w, 0, 0.7f, 130.5f); gem(w, 1.5f, 0.7f, 130.5f); gem(w, 0, 0.7f, 134.5f); gem(w, -1.5f, 0.7f, 134.5f);
        group(w, 0, 138.5f, 3);
        crow(w, 0, 141, false, -2f, 2f, 2.4f);

        // --- ultimo fiume e il portone
        padRow(w, 146);
        pit(w, 147, 156, 0, 4);
        flight(w, 146);
        group(w, 0, 159f, 3);
        acorn(w, 0f, 164);
        group(w, 0, 165.5f, 2);
        w.flagZ = 168;
    }

    /** Fila di fiori a molla su tutta la larghezza alla riga z. */
    private static void padRow(World3D w, int z) {
        for (int c = 0; c < W; c++) w.springs.add(new float[] {c - World3D.HALF + 0.5f, z + 0.5f});
    }

    /** Due cespugli di rovi nelle colonne ca e cb alla riga z. */
    private static void thornRow(World3D w, int z, int ca, int cb) {
        w.thorns.add(new float[] {ca - World3D.HALF + 0.5f, z + 0.5f});
        w.thorns.add(new float[] {cb - World3D.HALF + 0.5f, z + 0.5f});
    }

    /** Gemme sulla traiettoria di un rimbalzo dai fiori alla riga z: alte, mai sull'acqua a pelo. */
    private static void flight(World3D w, int z) {
        float[][] g = {{2.7f, 3.9f}, {5.3f, 5.2f}, {7.9f, 5.6f}, {10.5f, 4.5f}};
        for (float[] q : g) gem(w, 0, q[1], z + 0.5f + q[0]);
    }

    /** Piccola fila di n gemme a terra lungo la corsia cx, distanti 1,5 m. */
    private static void group(World3D w, float cx, float z0, int n) {
        for (int i = 0; i < n; i++) gem(w, cx, 0.7f, z0 + i * 1.5f);
    }

    private static void pit(World3D w, int z0, int z1, int x0, int x1) {
        for (int z = z0; z <= z1; z++) for (int x = x0; x <= x1; x++) w.height[z * W + x] = World3D.PIT;
    }

    private static void crate(World3D w, int x, int z) {
        w.crate[z * W + x] = true;
    }

    private static void gem(World3D w, float cx, float y, float z) {
        w.gemsList.add(new float[] {cx, y, z});
    }

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

    private static void crow(World3D w, float x, float z, boolean alongZ, float lo, float hi, float y) {
        Enemy3 e = new Enemy3(x, z, alongZ, lo, hi);
        e.type = Enemy3.CROW;
        e.y = y;
        e.baseY = y;
        w.enemies.add(e);
    }
}
