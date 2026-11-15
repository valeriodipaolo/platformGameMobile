package com.valeriodipaolo.platformgame.core;

/**
 * Undicesimo livello, il cielo: dopo le quattro stagioni il robot cammina sulle nuvole, tra il sole e la montagna lontana. Tutto è
 * luce e festa, ma il percorso è impegnativo come nel livello 7: isolotti di nuvola da saltare, una radura con un tempio di marmo (la
 * cella energetica sta in cima, si raggiunge rimbalzando), colonne di marmo, tre fiumi di nuvole da attraversare su ponti che cedono
 * poco dopo il tocco, due balzi lunghi con il trampolino e gli uccelli che sorvolano il percorso. Il terreno base occupa le righe 10 e 11.
 */
public final class LevelEleven {
    private LevelEleven() {}

    private static final int H = 12, GROUND_Y = 10;

    public static Level build() {
        Level lv = new Level(252, H);
        lv.id = "l11";
        lv.theme = 5;
        ground(lv, 0, 30);
        ground(lv, 34, 38);          // isolotto di nuvola
        ground(lv, 42, 82);          // il pad sta sull'ultima casella: 82
        ground(lv, 93, 118);
        ground(lv, 145, 170);
        ground(lv, 181, 200);
        ground(lv, 233, 251);
        lv.startX = 3;
        lv.startY = GROUND_Y - 1.2f;

        // --- prato d'ingresso
        shards(lv, 7, 9, 4);
        row(lv, 11, 7, "B?B");
        whirl(lv, 24, 16, 30);
        shards(lv, 21, 7, 3);

        // --- isolotti: salti brevi da una nuvola all'altra
        shards(lv, 35, 8, 3);
        shard(lv, 32, 6f); shard(lv, 40, 6f);

        // --- la radura e il tempio di marmo: un fiore a molla lancia sul tetto con la cella energetica
        whirl(lv, 47, 44, 50);
        hill(lv, 51, 60, 3);
        row(lv, 53, 5, "?B?");
        crow(lv, 58, 4.6f, 49, 64);
        pad(lv, 62);
        for (int x = 66; x <= 73; x++) lv.set(x, 4, Level.STONE);
        column(lv, 66, 5); column(lv, 73, 5);
        lv.battery = new float[] {69.5f, 2.9f};
        shards(lv, 67, 3, 2);
        shards(lv, 71, 3, 2);

        // --- primo balzo lungo: dieci caselle di cielo, si salta dal trampolino
        pad(lv, 82);
        flight(lv, 82);

        // --- bosco di colonne di marmo di varie altezze
        shards(lv, 95, 9, 3);
        column(lv, 100, 2);
        column(lv, 104, 3);
        column(lv, 108, 2);
        column(lv, 112, 4);
        shard(lv, 100, 6.4f); shard(lv, 104, 5.4f); shard(lv, 108, 6.4f); shard(lv, 112, 4.4f);
        whirl(lv, 96, 94, 99);
        storm(lv, 106, 2.6f, 98, 114);

        // --- primo ponte di nuvole: sei nuvole che cedono, larghe tre caselle e spaziate di cinque
        bridge(lv, 121, new int[] {9, 8, 9, 8, 9});
        storm(lv, 132, 3.6f, 122, 141);

        // --- terreno alto: i mattoni sono liberi sopra e sotto; la torre di nuvole instabili è più a destra, con un mulinello a terra
        row(lv, 147, 7, "B?B?B");
        whirl(lv, 150, 147, 156);
        lv.set(155, 8, Level.CRUMBLE); shard(lv, 155, 6);
        lv.set(158, 6, Level.CRUMBLE); shard(lv, 158, 4);
        lv.set(161, 4, Level.CRUMBLE); lv.set(162, 4, Level.CRUMBLE); shard(lv, 161, 2.6f); shard(lv, 162, 2.6f);
        lv.set(165, 6, Level.CRUMBLE); shard(lv, 165, 4);
        crow(lv, 160, 1.6f, 154, 168);
        shards(lv, 168, 9, 2);

        // --- secondo balzo lungo
        pad(lv, 170);
        flight(lv, 170);

        // --- la scalinata di marmo
        shards(lv, 183, 9, 3);
        stairsUp(lv, 188, 4);
        stairsDown(lv, 192, 4);
        shard(lv, 190.5f, 3.8f);
        shards(lv, 196, 9, 3);

        // --- il gran ponte: sei nuvole, salgono e scendono, con due uccelli di guardia
        bridge(lv, 203, new int[] {9, 8, 7, 8, 9, 8});
        storm(lv, 212, 3.6f, 204, 220);
        crow(lv, 225, 4.6f, 216, 231);

        // --- il portone, alla fine del cielo
        row(lv, 238, 7, "B?B");
        whirl(lv, 244, 236, 249);
        shards(lv, 236, 9, 3);
        shards(lv, 241, 9, 3);

        lv.flagX = 246;
        lv.flagBaseY = GROUND_Y;
        return lv;
    }

    /** Ponte di nuvole che cedono: larga tre caselle, una ogni cinque da x0, alla riga indicata; una gemma sopra ognuna. */
    private static void bridge(Level lv, int x0, int[] rows) {
        for (int i = 0; i < rows.length; i++) {
            for (int k = 0; k < 3; k++) lv.set(x0 + i * 5 + k, rows[i], Level.CRUMBLE);
            shard(lv, x0 + i * 5 + 1, rows[i] - 2);
        }
    }

    /** Gemme sulla traiettoria del trampolino in x: alte, mai nel vuoto a pelo. */
    private static void flight(Level lv, int x) {
        float[][] g = {{2f, 4.7f}, {3.5f, 3.0f}, {5f, 2.0f}, {6.4f, 1.6f}, {8f, 2.2f}, {9.4f, 4.1f}};
        for (float[] q : g) shard(lv, x + 0.5f + q[0], q[1]);
    }

    private static void ground(Level lv, int x0, int x1) {
        for (int x = x0; x <= x1; x++) {
            for (int y = GROUND_Y; y < H; y++) lv.set(x, y, Level.GROUND);
        }
    }

    /** Fiore a molla (qui un trampolino di nuvola) a filo del terreno alla colonna x. */
    private static void pad(Level lv, int x) {
        lv.set(x, standRow(lv, x) + 1, Level.SPRING);
    }

    private static void hill(Level lv, int x0, int x1, int maxRise) {
        for (int x = x0; x <= x1; x++) {
            int rise = Math.max(1, Math.round(maxRise * (float) Math.sin(Math.PI * (x - x0 + 0.5) / (x1 - x0 + 1))));
            for (int i = 1; i <= rise; i++) lv.set(x, GROUND_Y - i, Level.GROUND);
        }
    }

    /**
     * Fila di mattoni/casse. La quota viene abbassata se serve: tra il terreno sotto (colline, colonne) e la fila
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

    private static void column(Level lv, int x, int height) {
        for (int i = 1; i <= height; i++) lv.set(x, GROUND_Y - i, Level.STONE);
    }

    private static void stairsUp(Level lv, int x, int steps) {
        for (int i = 0; i < steps; i++) column(lv, x + i, i + 1);
    }

    private static void stairsDown(Level lv, int x, int steps) {
        for (int i = 0; i < steps; i++) column(lv, x + i, steps - i);
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

    private static void storm(Level lv, float x, float y, float lo, float hi) {
        lv.enemies.add(new float[] {x, y, Enemy.STORM, lo, hi});
    }

    private static void whirl(Level lv, float x, float lo, float hi) {
        lv.enemies.add(new float[] {x, standRow(lv, (int) x), Enemy.WHIRL, lo, hi});
    }

    private static void crow(Level lv, float x, float y, float lo, float hi) {
        lv.enemies.add(new float[] {x, y, Enemy.CROW, lo, hi});
    }
}
