package com.valeriodipaolo.platformgame.core3d;

/**
 * Dodicesimo livello, il cielo: il robot cammina sulle nuvole, sotto il sole e davanti alla montagna lontana, come nel prato d'estate ma visto da molto più
 * in alto. Tutto è luce e festa, ma il percorso è impegnativo come nel livello 8: una scalinata di marmo, un ponte di nuvola stretto, un corridoio di muri con un
 * solo varco, due vuoti da dieci metri che si superano rimbalzando sulle nuvole a molla, un balcone con la cella energetica, un sentiero con il vuoto ai lati, un arcipelago
 * di isolotti e gli uccelli che sorvolano il percorso. Le gemme stanno sempre in punti sicuri.
 */
public final class LevelTwelve {
    private LevelTwelve() {}

    private static final int W = World3D.W5;

    public static void build(World3D w) {
        int len = 192;
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

        // --- nuvola d'ingresso e scalinata di marmo
        gemLine3(w, 0, 0.7f, 4, 7);
        crate(w, 1, 9); crate(w, 3, 9);
        for (int x = 1; x <= 3; x++) { set(w, x, 13, 1); set(w, x, 14, 2); set(w, x, 15, 2); set(w, x, 16, 1); }
        gem(w, 0, 2.7f, 14.5f); gem(w, 0, 2.7f, 15.5f);
        whirl(w, 0, 20, false, -2f, 2f);

        // --- ponte di nuvola: tre caselle di larghezza sul vuoto, con un uccello di guardia
        pit(w, 24, 35, 0, 4);
        for (int z = 24; z <= 35; z++) for (int x = 1; x <= 3; x++) set(w, x, z, 0);
        storm(w, 0, 29, true, 26f, 33f, 2.9f);
        gem(w, 0, 0.7f, 26.5f); gem(w, 0, 0.7f, 31.5f); gem(w, 1, 0.7f, 34f);

        // --- corridoio di muri di marmo: ognuno ha un solo varco, sempre dal lato opposto al precedente
        crate(w, 1, 38); crate(w, 3, 38);
        wall(w, 41, 4);
        wall(w, 45, 0);
        wall(w, 49, 3);
        wall(w, 53, 1);
        whirl(w, 0, 43, false, -2f, 2f);
        crow(w, 0, 51, false, -2f, 2f, 2.1f);
        gem(w, 2, 0.7f, 41.5f); gem(w, -2, 0.7f, 45.5f); gem(w, 1, 0.7f, 49.5f); gem(w, -1, 0.7f, 53.5f);

        // --- primo vuoto da dieci metri: si supera rimbalzando sulle nuvole a molla
        padRow(w, 58, 0, 4);
        pit(w, 59, 68, 0, 4);
        flight(w, 58);
        gemLine3(w, 0, 0.7f, 70, 72);

        // --- il balcone di marmo con la cella energetica, raggiungibile dalle nuvole a molla
        crate(w, 1, 74); crate(w, 3, 74);
        padRow(w, 77, 1, 3);
        for (int z = 83; z <= 92; z++) for (int x = 1; x <= 3; x++) set(w, x, z, 4);
        for (int x = 1; x <= 3; x++) { set(w, x, 93, 2); set(w, x, 94, 1); }
        gem(w, -1, 4.7f, 85.5f); gem(w, 1, 4.7f, 90.5f);
       

        // --- sentiero stretto con il vuoto ai lati e due uccelli
        pit(w, 99, 116, 0, 0);
        pit(w, 99, 116, 4, 4);
        storm(w, 0, 103, true, 101f, 112f, 2.9f);
        whirl(w, 0, 112, false, -1f, 1f);
        wall(w, 106, 2);
        for (int z = 99; z <= 116; z += 3) if (z != 105 && z != 108) gem(w, 0, 0.7f, z + 0.5f);

        // --- secondo vuoto da dieci metri
        padRow(w, 120, 0, 4);
        pit(w, 121, 130, 0, 4);
        flight(w, 120);
        gemLine3(w, 0, 0.7f, 132, 134);

        // --- arcipelago: isolotti di nuvola a tre metri l'uno dall'altro (si salta con il doppio salto)
        crate(w, 1, 137); crate(w, 3, 137);
        pit(w, 140, 163, 0, 4);
        island(w, 140, 143, 1, 3);
        island(w, 147, 150, 2, 3);
        island(w, 154, 157, 1, 2);
        island(w, 161, 164, 1, 3);
        gem(w, 0, 0.7f, 141.5f); gem(w, 1, 0.7f, 148.5f); gem(w, -1, 0.7f, 155.5f); gem(w, 0, 0.7f, 162.5f);
        gem(w, 0.5f, 2.8f, 145.5f); gem(w, -0.5f, 2.8f, 152f);
        crow(w, 0, 145, false, -2f, 2f, 3.0f);
        storm(w, 0, 152, false, -1.5f, 1.5f, 3.4f);

        // --- ultimi muri e il portone
        crate(w, 1, 168); crate(w, 3, 168);
        wall(w, 171, 2);
        wall(w, 175, 0);
        wall(w, 179, 4);
        crow(w, 0, 173, false, -2f, 2f, 2.1f);
        whirl(w, 0, 183, false, -2f, 2f);
        storm(w, 0, 184, false, -2f, 2f, 2.9f);
        gem(w, 0, 0.7f, 171.5f); gem(w, -2, 0.7f, 175.5f); gem(w, 2, 0.7f, 179.5f);
        gemLine3(w, 0, 0.7f, 182, 185);
        w.flagZ = 188;
    }

    /** Muro basso di marmo (una casella, così non nasconde nulla alla camera), con un solo varco. */
    private static void wall(World3D w, int z, int gap) {
        for (int x = 0; x < W; x++) set(w, x, z, x == gap ? 0 : 1);
    }

    private static void padRow(World3D w, int z, int c0, int c1) {
        for (int c = c0; c <= c1; c++) w.springs.add(new float[] {c - World3D.HALF5 + 0.5f, z + 0.5f});
    }

    /** Gemme sulla traiettoria misurata di un rimbalzo dalle nuvole a molla alla riga z. */
    private static void flight(World3D w, int z) {
        float[][] g = {{2.7f, 3.9f}, {5.3f, 5.2f}, {7.9f, 5.6f}, {10.5f, 4.5f}};
        for (float[] q : g) gem(w, 0, q[1], z + 0.5f + q[0]);
    }

    private static void island(World3D w, int z0, int z1, int x0, int x1) {
        for (int z = z0; z <= z1; z++) for (int x = x0; x <= x1; x++) set(w, x, z, 0);
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

    private static void gemLine3(World3D w, float cx, float y, int z0, int z1) {
        for (int z = z0; z <= z1; z++) gem(w, cx, y, z + 0.5f);
    }

    /** Nuvola temporalesca: vaga lenta sopra al sentiero, poi si carica e scaglia un fulmine verso il basso. */
    private static void storm(World3D w, float x, float z, boolean alongZ, float lo, float hi, float y) {
        Enemy3 e = new Enemy3(x, z, alongZ, lo, hi);
        e.type = Enemy3.STORM;
        e.y = y;
        e.baseY = y;
        w.enemies.add(e);
    }

    /** Mulinello di vento: corre veloce, non si schiaccia (si salta o si travolge con la giravolta). */
    private static void whirl(World3D w, float x, float z, boolean alongZ, float lo, float hi) {
        Enemy3 e = new Enemy3(x, z, alongZ, lo, hi);
        e.type = Enemy3.WHIRL;
        w.enemies.add(e);
    }

    /** Uccello: vola a onde sopra al sentiero, a quota media y. */
    private static void crow(World3D w, float x, float z, boolean alongZ, float lo, float hi, float y) {
        Enemy3 e = new Enemy3(x, z, alongZ, lo, hi);
        e.type = Enemy3.CROW;
        e.y = y;
        e.baseY = y;
        w.enemies.add(e);
    }
}
