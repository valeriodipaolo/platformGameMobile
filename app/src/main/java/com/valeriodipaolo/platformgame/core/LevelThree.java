package com.valeriodipaolo.platformgame.core;

/**
 * Terzo livello, il prato d'autunno: colline arrotondate, burroni più larghi, nemici nuovi (riccio, corvo, ghianda)
 * e piattaforme sospese. Il terreno base occupa le righe 10 e 11.
 */
public final class LevelThree {
    private LevelThree() {}

    private static final int H = 12, GROUND_Y = 10;

    public static Level build() {
        Level lv = new Level(246, H);
        lv.id = "l3";
        lv.theme = 1;
        ground(lv, 0, 44);
        ground(lv, 49, 92);
        ground(lv, 97, 147);
        ground(lv, 152, 245);
        lv.startX = 3;
        lv.startY = GROUND_Y - 1.2f;

        // --- avvio: prato, primi nemici nuovi uno alla volta
        shards(lv, 8, 9, 4);
        row(lv, 13, 7, "B?B");
        enemy(lv, 21, Enemy.ACORN, 0, 0);
        shard(lv, 24, 7); shard(lv, 25, 6.3f); shard(lv, 26, 6); shard(lv, 27, 6.3f); shard(lv, 28, 7);
        crow(lv, 33, 5.8f, 29, 40);
        row(lv, 34, 7, "?");
        enemy(lv, 39, Enemy.HOG, 0, 0);
        column(lv, 42, 2);

        // --- primo burrone, largo quattro caselle: gemme ad arco
        shard(lv, 45, 7); shard(lv, 46, 6.2f); shard(lv, 47, 6.2f); shard(lv, 48, 7);

        // --- collina morbida con scatole in cima
        hill(lv, 53, 63, 3);
        row(lv, 56, 5, "?B?");
        enemy(lv, 58, Enemy.ACORN, 0, 0);
        enemy(lv, 66, Enemy.BEETLE, 0, 0);
        enemy(lv, 69, Enemy.BEETLE, 0, 0);
        row(lv, 71, 7, "BBHBB");                          // (il vecchio mattoncino segreto: ora il nascondiglio del gettone lo decide Coins2D)
        shards(lv, 71, 5, 5);
        crow(lv, 78, 6, 74, 86);
        enemy(lv, 81, Enemy.HOG, 0, 0);
        row(lv, 84, 6, "BBB");
        row(lv, 88, 4, "B?B");

        // --- secondo burrone
        shard(lv, 93, 7); shard(lv, 94, 6); shard(lv, 95, 5.5f); shard(lv, 96, 6);

        // --- salita a gradoni di massi
        stairsUp(lv, 99, 4);
        shards(lv, 100, 5, 3);
        enemy(lv, 104, Enemy.HOG, 0, 0);
        stairsDown(lv, 107, 4);
        hill(lv, 112, 120, 2);
        enemy(lv, 114, Enemy.ACORN, 0, 0);
        row(lv, 114, 5, "?");
        crow(lv, 121, 6, 117, 130);
        crow(lv, 125, 4.5f, 117, 130);
        row(lv, 126, 7, "?B?B?");
        enemy(lv, 129, Enemy.ACORN, 0, 0);
        enemy(lv, 133, Enemy.BEETLE, 0, 0);
        enemy(lv, 135, Enemy.BEETLE, 0, 0);
        column(lv, 138, 3);
        shards(lv, 140, 9, 5);

        // --- terzo burrone
        shard(lv, 148, 7); shard(lv, 149, 6); shard(lv, 150, 5.5f); shard(lv, 151, 6);

        // --- tratto finale
        hill(lv, 156, 166, 3);
        row(lv, 159, 5, "B?B");
        enemy(lv, 161, Enemy.HOG, 0, 0);
        enemy(lv, 169, Enemy.ACORN, 0, 0);
        enemy(lv, 172, Enemy.ACORN, 0, 0);
        enemy(lv, 175, Enemy.ACORN, 0, 0);
        row(lv, 176, 7, "B?BB?B");
        crow(lv, 182, 6, 176, 190);
        enemy(lv, 184, Enemy.BEETLE, 0, 0);
        shards(lv, 186, 9, 4);
        column(lv, 192, 3);
        enemy(lv, 195, Enemy.HOG, 0, 0);
        enemy(lv, 198, Enemy.HOG, 0, 0);
        row(lv, 200, 7, "?");
        stairsUp(lv, 203, 6);
        stairsDown(lv, 209, 3);
        crow(lv, 216, 5.5f, 211, 224);
        enemy(lv, 218, Enemy.ACORN, 0, 0);
        shards(lv, 220, 9, 4);

        lv.flagX = 238;
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
