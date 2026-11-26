package com.valeriodipaolo.platformgame.core;

/**
 * Quinto livello, la tundra sotto l'aurora boreale: notte polare, passerelle di ghiaccio sopra i burroni, pietre come appigli,
 * torri di massi e molti corvi. Il terreno base occupa le righe 10 e 11.
 */
public final class LevelFive {
    private LevelFive() {}

    private static final int H = 12, GROUND_Y = 10;

    public static Level build() {
        Level lv = new Level(252, H);
        lv.id = "l5";
        lv.theme = 2;
        ground(lv, 0, 38);
        ground(lv, 43, 60);
        ground(lv, 66, 95);
        ground(lv, 100, 130);
        ground(lv, 136, 170);
        ground(lv, 176, 251);
        lv.startX = 3;
        lv.startY = GROUND_Y - 1.2f;

        // --- avvio: pianura innevata
        shards(lv, 7, 9, 4);
        row(lv, 11, 7, "B?B");
        enemy(lv, 17, Enemy.ACORN, 0, 0);
        hill(lv, 22, 31, 2);
        shards(lv, 24, 6, 5);
        enemy(lv, 28, Enemy.HOG, 0, 0);
        crow(lv, 33, 6, 28, 38);

        // --- primo burrone (4 caselle) e salita a gradoni
        shard(lv, 39, 7); shard(lv, 40, 6.2f); shard(lv, 41, 6.2f); shard(lv, 42, 7);
        stairsUp(lv, 45, 3);
        row(lv, 49, 5, "?B?");
        enemy(lv, 54, Enemy.BEETLE, 0, 0);
        enemy(lv, 57, Enemy.ACORN, 0, 0);

        // --- burrone largo cinque: un solo mattone di ghiaccio come appiglio
        lv.set(63, 8, Level.BRICK);
        shard(lv, 61, 7); shard(lv, 62, 6.2f); shard(lv, 63, 6.6f); shard(lv, 64, 6.2f); shard(lv, 65, 7);

        // --- altopiano di ghiaccio con passerella di mattoni
        hill(lv, 68, 79, 3);
        row(lv, 71, 5, "?B?");
        crow(lv, 74, 4.5f, 68, 84);
        enemy(lv, 81, Enemy.HOG, 0, 0);
        enemy(lv, 85, Enemy.BEETLE, 0, 0);
        row(lv, 84, 6, "BBHBB");
        row(lv, 90, 4, "B?B");

        // --- secondo burrone
        shard(lv, 96, 7); shard(lv, 97, 6); shard(lv, 98, 6); shard(lv, 99, 7);

        // --- cresta di massi
        column(lv, 102, 2);
        column(lv, 104, 3);
        column(lv, 106, 4);
        shards(lv, 103, 6, 3);
        enemy(lv, 110, Enemy.HOG, 0, 0);
        crow(lv, 114, 6, 108, 122);
        enemy(lv, 116, Enemy.ACORN, 0, 0);
        enemy(lv, 119, Enemy.ACORN, 0, 0);
        row(lv, 121, 7, "B?B?B");
        crow(lv, 126, 5, 118, 132);
        column(lv, 128, 2);

        // --- terzo burrone (cinque): due pilastri di pietra come appigli
        column(lv, 132, 1);
        column(lv, 134, 1);
        shard(lv, 131, 7); shard(lv, 132, 6.2f); shard(lv, 133, 5.8f); shard(lv, 134, 6.2f); shard(lv, 135, 7);

        // --- pianura dei corvi
        enemy(lv, 140, Enemy.HOG, 0, 0);
        enemy(lv, 143, Enemy.BEETLE, 0, 0);
        crow(lv, 147, 6.2f, 141, 155);
        crow(lv, 151, 4.6f, 141, 155);
        row(lv, 148, 7, "?B?B?");
        hill(lv, 156, 166, 2);
        enemy(lv, 160, Enemy.ACORN, 0, 0);
        shards(lv, 158, 6, 5);

        // --- quarto burrone
        shard(lv, 171, 7); shard(lv, 172, 6); shard(lv, 173, 6); shard(lv, 174, 7);

        // --- tratto finale
        enemy(lv, 180, Enemy.ACORN, 0, 0);
        enemy(lv, 183, Enemy.HOG, 0, 0);
        row(lv, 185, 7, "B?B");
        stairsUp(lv, 190, 4);
        shards(lv, 191, 5, 4);
        stairsDown(lv, 196, 4);
        crow(lv, 202, 5.5f, 198, 212);
        enemy(lv, 205, Enemy.HOG, 0, 0);
        enemy(lv, 208, Enemy.ACORN, 0, 0);
        enemy(lv, 210, Enemy.BEETLE, 0, 0);
        row(lv, 211, 7, "?B?B?");
        hill(lv, 218, 228, 3);
        enemy(lv, 223, Enemy.HOG, 0, 0);
        crow(lv, 230, 6, 224, 236);
        shards(lv, 231, 9, 4);

        lv.flagX = 244;
        lv.flagBaseY = GROUND_Y;
        return lv;
    }

    private static void ground(Level lv, int x0, int x1) {
        for (int x = x0; x <= x1; x++) {
            for (int y = GROUND_Y; y < H; y++) lv.set(x, y, Level.GROUND);
        }
    }

    /** Collina: il terreno si alza a gradini bassi verso il centro, come una gobba. */
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

    private static void enemy(Level lv, float x, int type, float lo, float hi) {
        lv.enemies.add(new float[] {x, standRow(lv, (int) x), type, lo, hi});
    }

    private static void crow(Level lv, float x, float y, float lo, float hi) {
        lv.enemies.add(new float[] {x, y, Enemy.CROW, lo, hi});
    }
}
