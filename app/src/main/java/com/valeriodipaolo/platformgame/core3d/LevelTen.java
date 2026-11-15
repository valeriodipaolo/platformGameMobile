package com.valeriodipaolo.platformgame.core3d;

/**
 * Decimo livello, il giardino di primavera e la fuga dalle api: ciliegi in fiori, tre fiumi larghi dieci metri (più del massimo di un doppio salto) che si
 * superano solo rimbalzando sulla fila di fiori a molla in riva, due balconi di pietra a quattro metri (con la cella energetica) raggiungibili
 * dai fiori, e due campi di rovi a zigzag. I rovi sono pochi e lasciano sempre una corsia libera; ogni gemma sta in un punto sicuro,
 * lontano dai rovi e dall'acqua. Passato l'ultimo ciliegio il nido d'api cade e si parte senza stacchi con la fuga (LevelEleven). Difficoltà come i livelli 6 e 8 al massimo.
 */
public final class LevelTen {
    private LevelTen() {}

    private static final int W = World3D.W;

    public static void build(World3D w) {
        int len = 176 + 172;                       // il giardino (fino al ciliegio con il nido) e subito dopo la fuga dalle api
        w.length = len;
        w.height = new int[len * W];
        w.crate = new boolean[len * W];
        w.gemsList.clear();
        w.enemies.clear();
        w.jets.clear();
        w.logs.clear();
        w.springs.clear();
        w.thorns.clear();
        w.chase = 0;
        w.fireStart = Float.NaN;
        w.fireZones = new float[0][];

        // --- prato d'ingresso: poche gemme, e un fiore che lancia sul balcone di pietra
        gemLine3(w, 0, 0.7f, 4, 6);
        crate(w, 1, 9); crate(w, 3, 9);
        padRow(w, 14, 1, 3);
        balcony(w, 20, 29);
        gem(w, 0, 4.7f, 23.5f); gem(w, -1, 4.7f, 25.5f); gem(w, 1, 4.7f, 27.5f);
        acorn(w, 0f, 34);

        // --- primo campo di rovi: tre file, la corsia centrale-sinistra (colonna 1) è sempre libera
        thornRow(w, 37, 0, 2);
        thornRow(w, 41, 2, 4);
        thornRow(w, 45, 0, 2);
        gem(w, -1, 0.7f, 39.5f); gem(w, 1, 0.7f, 43.5f);
        hog(w, 0, 49, false, -2f, 2f);

        // --- primo fiume: la fila di fiori in riva lancia dall'altra parte, le gemme seguono la traiettoria
        padRow(w, 52, 0, 4);
        pit(w, 53, 62, 0, 4);
        flight(w, 52);
        gemLine3(w, 0, 0.7f, 64, 66);

        // --- il ciliegio alto: un fiore lancia sul balcone di pietra con la cella energetica
        crate(w, 1, 69); crate(w, 3, 69);
        acorn(w, 0f, 71);
        padRow(w, 74, 1, 3);
        balcony(w, 80, 89);
        gem(w, -1, 4.7f, 82.5f); gem(w, 1, 4.7f, 87.5f);
        w.battery = new float[] {0, 5.3f, 85.5f};
        hog(w, 0, 94, false, -2f, 2f);

        // --- secondo fiume
        padRow(w, 99, 0, 4);
        pit(w, 100, 109, 0, 4);
        flight(w, 99);
        gemLine3(w, 0, 0.7f, 111, 113);

        // --- radura con ciliegi e secondo campo di rovi
        crate(w, 1, 117); crate(w, 3, 117);
        hog(w, 0, 121, false, -2f, 2f);
        acorn(w, 0f, 125);
        thornRow(w, 130, 2, 4);
        thornRow(w, 134, 0, 2);
        thornRow(w, 138, 2, 4);
        gem(w, 1, 0.7f, 132.5f); gem(w, -1, 0.7f, 136.5f);
        hog(w, 0, 143, false, -2f, 2f);

        // --- ultimo fiume e il portone
        padRow(w, 148, 0, 4);
        pit(w, 149, 158, 0, 4);
        flight(w, 148);
        gemLine3(w, 0, 0.7f, 160, 162);
        acorn(w, 0f, 166);
        gem(w, 0, 0.7f, 170.5f);
        w.hiveZ = 172;
        LevelEleven.append(w, 176);
    }

    /** Fila di fiori a molla sulle colonne c0..c1 alla riga z. */
    private static void padRow(World3D w, int z, int c0, int c1) {
        for (int c = c0; c <= c1; c++) spring(w, c, z);
    }

    /** Balcone di pietra alto 4 m sulle colonne 1..3, da z0 a z1, con due gradini di discesa in fondo. */
    private static void balcony(World3D w, int z0, int z1) {
        for (int z = z0; z <= z1; z++) for (int x = 1; x <= 3; x++) set(w, x, z, 4);
        for (int x = 1; x <= 3; x++) { set(w, x, z1 + 1, 2); set(w, x, z1 + 2, 1); }
    }

    /** Fila di rovi: due cespugli alle colonne ca e cb. */
    private static void thornRow(World3D w, int z, int ca, int cb) {
        thorn(w, ca, z);
        thorn(w, cb, z);
    }

    /** Gemme sulla traiettoria misurata di un rimbalzo dai fiori alla riga z: alte, mai sopra l'acqua a pelo. */
    private static void flight(World3D w, int z) {
        float[][] g = {{2.7f, 3.9f}, {5.3f, 5.2f}, {7.9f, 5.6f}, {10.5f, 4.5f}};
        for (float[] q : g) gem(w, 0, q[1], z + 0.5f + q[0]);
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
