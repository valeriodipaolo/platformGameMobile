package com.valeriodipaolo.platformgame.core;

/** Primo livello. Il terreno occupa le righe 10 e 11; il giocatore cammina sulla riga 10. */
public final class LevelOne {
    private LevelOne() {}

    private static final int H = 12, GROUND_Y = 10;

    public static Level build() {
        Level lv = new Level(212, H);
        ground(lv, 0, 61);
        ground(lv, 65, 103);
        ground(lv, 107, 140);
        ground(lv, 144, 211);
        lv.startX = 3;
        lv.startY = GROUND_Y - 1.2f;

        // --- primo tratto: monete, casse e primo nemico
        shards(lv, 9, 9, 4);
        row(lv, 17, 7, "?");
        row(lv, 22, 7, "B?B?B");
        row(lv, 24, 4, "?");
        enemy(lv, 27);
        column(lv, 33, 2);
        enemy(lv, 37);
        column(lv, 41, 3);
        shards(lv, 44, 9, 3);
        enemy(lv, 46);
        enemy(lv, 47.5f);
        column(lv, 50, 3);
        shards(lv, 51, 6, 1);
        row(lv, 55, 7, "BB?");

        // --- primo burrone, con un arco di monete
        shard(lv, 62, 6); shard(lv, 63, 5.5f); shard(lv, 64, 6);

        // --- secondo tratto
        row(lv, 70, 7, "?B?");
        enemy(lv, 75);
        enemy(lv, 77);
        row(lv, 82, 7, "BBBBHBBB");          // (il vecchio mattoncino segreto: ora il nascondiglio del gettone lo decide Coins2D)
        shards(lv, 83, 6, 6);
        row(lv, 91, 4, "BB?B");
        enemy(lv, 93);
        stairsUp(lv, 100, 4);                 // scalinata, burrone, scalinata
        stairsDown(lv, 107, 4);

        // --- terzo tratto
        row(lv, 116, 7, "?");
        row(lv, 119, 7, "?");
        row(lv, 122, 7, "?");
        row(lv, 119, 4, "?");
        enemy(lv, 121);
        enemy(lv, 126);
        enemy(lv, 128);
        column(lv, 132, 2);
        shards(lv, 134, 9, 4);
        enemy(lv, 137);
        shard(lv, 141, 5); shard(lv, 142, 4.6f); shard(lv, 143, 5);

        // --- ultimo tratto
        row(lv, 149, 7, "B?B");
        enemy(lv, 153);
        row(lv, 158, 7, "BBB?BBB");
        row(lv, 160, 4, "???");
        enemy(lv, 164);
        enemy(lv, 166);
        column(lv, 171, 2);
        shards(lv, 174, 9, 4);
        stairsUp(lv, 181, 7);
        column(lv, 188, 7);

        // --- traguardo
        lv.flagX = 198;
        lv.flagBaseY = GROUND_Y;
        return lv;
    }

    private static void ground(Level lv, int x0, int x1) {
        for (int x = x0; x <= x1; x++) {
            for (int y = GROUND_Y; y < H; y++) lv.set(x, y, Level.GROUND);
        }
    }

    private static void row(Level lv, int x, int y, String s) {
        for (int i = 0; i < s.length(); i++) lv.set(x + i, y, s.charAt(i));
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

    private static void enemy(Level lv, float x) {
        lv.enemies.add(new float[] {x, GROUND_Y - 1});
    }
}
