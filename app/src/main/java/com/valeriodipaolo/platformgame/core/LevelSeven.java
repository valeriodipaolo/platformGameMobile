package com.valeriodipaolo.platformgame.core;

/**
 * Settimo livello, il prato in fiamme: l'incendio ha spaccato il terreno in burroni di lava. Colline bruciate, torri di massi
 * come appigli, corvi che volano nel fumo e nemici ovunque. Il terreno base occupa le righe 10 e 11.
 */
public final class LevelSeven {
    private LevelSeven() {}

    private static final int H = 12, GROUND_Y = 10;

    public static Level build() {
        Level lv = new Level(252, H);
        lv.id = "l7";
        lv.theme = 3;
        ground(lv, 0, 34);
        ground(lv, 39, 58);
        ground(lv, 63, 92);
        ground(lv, 98, 128);
        ground(lv, 134, 168);
        ground(lv, 174, 251);
        lv.startX = 3;
        lv.startY = GROUND_Y - 1.2f;

        // --- avvio: prato bruciacchiato
        shards(lv, 7, 9, 4);
        row(lv, 11, 7, "B?B");
        enemy(lv, 16, Enemy.BEETLE, 0, 0);
        hill(lv, 20, 29, 2);
        shards(lv, 22, 6, 5);
        enemy(lv, 26, Enemy.ACORN, 0, 0);
        crow(lv, 31, 6, 26, 36);

        // --- primo burrone di lava (4 caselle)
        shard(lv, 35, 7); shard(lv, 36, 6.2f); shard(lv, 37, 6.2f); shard(lv, 38, 7);
        stairsUp(lv, 41, 3);
        row(lv, 45, 5, "?B?");
        enemy(lv, 50, Enemy.HOG, 0, 0);
        enemy(lv, 54, Enemy.ACORN, 0, 0);

        // --- burrone largo cinque: un mattone come appiglio
        lv.set(60, 8, Level.BRICK);
        shard(lv, 59, 7); shard(lv, 60, 6.2f); shard(lv, 61, 6.6f); shard(lv, 62, 7);

        // --- collina bruciata con passerella di mattoni
        hill(lv, 65, 76, 3);
        row(lv, 68, 5, "?B?");
        crow(lv, 71, 4.5f, 65, 81);
        enemy(lv, 78, Enemy.BEETLE, 0, 0);
        enemy(lv, 82, Enemy.HOG, 0, 0);
        row(lv, 81, 6, "BBHBB");
        row(lv, 87, 4, "B?B");
        lv.battery = new float[] {88, 2.6f};

        // --- secondo burrone
        shard(lv, 93, 7); shard(lv, 94, 6); shard(lv, 95, 6); shard(lv, 96, 7); shard(lv, 97, 7);

        // --- cresta di massi arroventati
        column(lv, 100, 2);
        column(lv, 102, 3);
        column(lv, 104, 4);
        shards(lv, 101, 6, 3);
        enemy(lv, 108, Enemy.HOG, 0, 0);
        crow(lv, 112, 6, 106, 120);
        enemy(lv, 114, Enemy.ACORN, 0, 0);
        enemy(lv, 117, Enemy.BEETLE, 0, 0);
        row(lv, 119, 7, "B?B?B");
        crow(lv, 124, 5, 116, 130);
        column(lv, 126, 2);

        // --- terzo burrone (cinque): due pilastri come appigli
        column(lv, 130, 1);
        column(lv, 132, 1);
        shard(lv, 129, 7); shard(lv, 130, 6.2f); shard(lv, 131, 5.8f); shard(lv, 132, 6.2f); shard(lv, 133, 7);

        // --- pianura dei corvi
        enemy(lv, 138, Enemy.HOG, 0, 0);
        enemy(lv, 141, Enemy.BEETLE, 0, 0);
        crow(lv, 145, 6.2f, 139, 153);
        crow(lv, 149, 4.6f, 139, 153);
        row(lv, 146, 7, "?B?B?");
        hill(lv, 154, 164, 2);
        enemy(lv, 158, Enemy.ACORN, 0, 0);
        shards(lv, 156, 6, 5);

        // --- quarto burrone
        shard(lv, 169, 7); shard(lv, 170, 6); shard(lv, 171, 6); shard(lv, 172, 6); shard(lv, 173, 7);

        // --- tratto finale
        enemy(lv, 178, Enemy.ACORN, 0, 0);
        enemy(lv, 181, Enemy.HOG, 0, 0);
        row(lv, 183, 7, "B?B");
        stairsUp(lv, 188, 4);
        shards(lv, 189, 5, 4);
        stairsDown(lv, 194, 4);
        crow(lv, 200, 5.5f, 196, 210);
        enemy(lv, 203, Enemy.HOG, 0, 0);
        enemy(lv, 206, Enemy.BEETLE, 0, 0);
        enemy(lv, 208, Enemy.ACORN, 0, 0);
        row(lv, 209, 7, "?B?B?");
        hill(lv, 216, 226, 3);
        enemy(lv, 221, Enemy.HOG, 0, 0);
        crow(lv, 228, 6, 222, 234);
        shards(lv, 229, 9, 4);

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
