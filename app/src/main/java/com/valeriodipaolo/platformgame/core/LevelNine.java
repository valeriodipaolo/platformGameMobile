package com.valeriodipaolo.platformgame.core;

/**
 * Nono livello, il prato di primavera: ciliegi in fiori, tre fiumi larghi 10 caselle (più di un doppio salto) che si superano solo
 * con il fiore a molla all'ultima casella della riva, e due balconi in alto (sul prato d'ingresso e tra le chiome) raggiungibili
 * rimbalzando. I rovi sono pochi, isolati, e si scavalcano con un salto. Ogni gemma sta su un percorso sicuro (mai sull'acqua in basso,
 * mai accanto ai rovi). Difficoltà come i livelli 7 e 8 al massimo.
 */
public final class LevelNine {
    private LevelNine() {}

    private static final int H = 12, GROUND_Y = 10;

    public static Level build() {
        Level lv = new Level(240, H);
        lv.id = "l9";
        lv.theme = 4;
        ground(lv, 0, 36);           // il fiore a molla sta sull'ultima casella di ogni riva
        ground(lv, 47, 96);
        ground(lv, 107, 190);
        ground(lv, 201, 239);
        lv.startX = 3;
        lv.startY = GROUND_Y - 1.2f;

        // --- prato d'ingresso: un fiore lancia sul balcone di pietra (gemme), rovi isolati più avanti
        shards(lv, 7, 9, 3);
        row(lv, 11, 7, "B?B");
        enemy(lv, 14, Enemy.BEETLE, 0, 0);
        pad(lv, 19);
        for (int x = 25; x <= 30; x++) lv.set(x, 3, Level.STONE);
        shards(lv, 26, 2, 4);
        enemy(lv, 28, Enemy.ACORN, 0, 0);
        thorn(lv, 22);
        thorn(lv, 23);
        shards(lv, 22, 6, 2);

        // --- primo fiume (10 caselle): si salta dal fiore a metà arco, le gemme seguono la traiettoria
        pad(lv, 36);
        flight(lv, 36);

        // --- bosco di ciliegi: un'altura e il balcone tra le chiome, con la batteria, che si raggiunge dal fiore
        shards(lv, 49, 9, 3);
        hill(lv, 58, 66, 3);
        row(lv, 60, 5, "?B?");
        enemy(lv, 62, Enemy.ACORN, 0, 0);
        pad(lv, 68);
        for (int x = 74; x <= 80; x++) lv.set(x, 3, Level.STONE);
        shards(lv, 75, 2, 2);
        shards(lv, 78, 2, 2);
        lv.battery = new float[] {77, 1.9f};
        enemy(lv, 75, Enemy.HOG, 0, 0);
        thorn(lv, 85);
        thorn(lv, 86);
        shards(lv, 85, 6, 2);
        row(lv, 89, 7, "B?B");

        // --- secondo fiume
        pad(lv, 96);
        flight(lv, 96);

        // --- prato di sosta con ciliegi
        shards(lv, 109, 9, 3);
        hill(lv, 118, 130, 3);
        row(lv, 120, 5, "?B?B?");
        enemy(lv, 134, Enemy.BEETLE, 0, 0);
        enemy(lv, 137, Enemy.ACORN, 0, 0);

        // --- rovi: tre coppie ben separate, ognuna con tre gemme proprio sopra (si raccolgono scavalcandole)
        thorn(lv, 147);
        thorn(lv, 148);
        shards(lv, 147, 6, 2);
        thorn(lv, 156);
        thorn(lv, 157);
        shards(lv, 156, 6, 2);
        thorn(lv, 165);
        thorn(lv, 166);
        shards(lv, 165, 6, 2);
        enemy(lv, 174, Enemy.HOG, 0, 0);
        row(lv, 176, 7, "B?B");
        enemy(lv, 182, Enemy.BEETLE, 0, 0);
        shards(lv, 178, 9, 3);

        // --- terzo fiume e il portone
        pad(lv, 190);
        flight(lv, 190);
        shards(lv, 203, 9, 3);
        enemy(lv, 209, Enemy.ACORN, 0, 0);
        thorn(lv, 216);
        thorn(lv, 217);
        shards(lv, 216, 6, 2);
        enemy(lv, 224, Enemy.BEETLE, 0, 0);

        lv.flagX = 232;
        lv.flagBaseY = GROUND_Y;
        return lv;
    }

    private static void ground(Level lv, int x0, int x1) {
        for (int x = x0; x <= x1; x++) {
            for (int y = GROUND_Y; y < H; y++) lv.set(x, y, Level.GROUND);
        }
    }

    /** Fiore a molla a filo del terreno (o in cima a un'altura) alla colonna x. */
    private static void pad(Level lv, int x) {
        lv.set(x, standRow(lv, x) + 1, Level.SPRING);
    }

    /** Cespuglio di rovi che poggia sul terreno alla colonna x. */
    private static void thorn(Level lv, int x) {
        lv.thorns.add(new float[] {x, standRow(lv, x)});
    }

    /** Gemme lungo la traiettoria misurata di un rimbalzo dal fiore in x: sempre in alto, mai vicino all'acqua. */
    private static void flight(Level lv, int x) {
        float[][] g = {{2f, 4.7f}, {3.5f, 3.0f}, {5f, 2.0f}, {6.4f, 1.6f}, {8f, 2.2f}, {9.4f, 4.1f}};
        for (float[] q : g) shard(lv, x + 0.5f + q[0], q[1]);
    }

    private static void hill(Level lv, int x0, int x1, int maxRise) {
        for (int x = x0; x <= x1; x++) {
            int rise = Math.max(1, Math.round(maxRise * (float) Math.sin(Math.PI * (x - x0 + 0.5) / (x1 - x0 + 1))));
            for (int i = 1; i <= rise; i++) lv.set(x, GROUND_Y - i, Level.GROUND);
        }
    }

    /**
     * Fila di mattoni/casse. La quota viene abbassata se serve: tra il terreno sotto (colline, massi) e la fila
     * devono restare almeno due caselle libere, altrimenti il personaggio non passa e non può colpirla.
     */
    private static void row(Level lv, int x, int y, String s) {
        int top = H;
        for (int i = 0; i < s.length(); i++) top = Math.min(top, topSolid(lv, x + i));
        y = Math.min(y, top - 3);
        for (int i = 0; i < s.length(); i++) lv.set(x + i, y, s.charAt(i));
    }

    private static int topSolid(Level lv, int x) {
        for (int y = 0; y < H; y++) {
            char c = lv.get(x, y);
            if (c == Level.GROUND || c == Level.STONE) return y;
        }
        return H;
    }

    private static void shard(Level lv, float x, float y) {
        lv.shards.add(new float[] {x, y});
    }

    private static void shards(Level lv, int x, int y, int n) {
        for (int i = 0; i < n; i++) shard(lv, x + i, y);
    }

    /** Riga della casella libera più alta sopra al terreno (o sopra alle colline) nella colonna x. */
    private static int standRow(Level lv, int x) {
        int y = 0;
        while (y < H && !lv.solid(x, y)) y++;
        return y - 1;
    }

    private static void enemy(Level lv, float x, int type, float lo, float hi) {
        lv.enemies.add(new float[] {x, standRow(lv, (int) x), type, lo, hi});
    }

    private static void crow(Level lv, float x, float y, float lo, float hi) {
        lv.enemies.add(new float[] {x, y, Enemy.CROW, lo, hi});
    }
}
