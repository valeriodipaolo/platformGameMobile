package com.valeriodipaolo.platformgame.core;

/**
 * Settimo livello, il prato in fiamme: una corsa per salvarsi dal rogo. Un muro di fuoco insegue il robot a velocità
 * crescente; getti di fiamma a tempo, ponti di legno bruciato che crollano appena li si tocca, una radura dove il fuoco
 * si ferma e una scelta tra il terreno incandescente e la torre di tronchi. Il terreno base occupa le righe 10 e 11.
 */
public final class LevelSeven {
    private LevelSeven() {}

    private static final int H = 12, GROUND_Y = 10;

    public static Level build() {
        Level lv = new Level(232, H);
        lv.id = "l7";
        lv.theme = 3;
        ground(lv, 0, 32);
        ground(lv, 59, 170);
        ground(lv, 187, 231);
        lv.startX = 3;
        lv.startY = GROUND_Y - 1.2f;

        // il fuoco parte dietro e si accende davvero solo dopo l'avvio; il terzo valore è la distanza massima che tiene dal robot
        lv.fireStart = -10f;
        lv.fireZones = new float[][] {{12, 1.9f, 22}, {58, 2.3f, 20}, {96, 0f}, {126, 2.7f, 18}, {170, 3.1f, 16}, {188, 3.3f, 15}};

        // --- avvio: corsa nel fumo, i primi getti
        shards(lv, 7, 9, 4);
        row(lv, 11, 7, "B?B");
        enemy(lv, 15, Enemy.BEETLE, 0, 0);
        jet(lv, 20, 0f, 3.4f);
        jet(lv, 25, 1.5f, 3.4f);
        shards(lv, 21, 6, 3);
        enemy(lv, 28, Enemy.ACORN, 0, 0);

        // --- il ponte che crolla: tavole bruciate sopra al burrone, ognuna cede mezzo secondo dopo il tocco
        int[][] planks = {{34, 9}, {37, 8}, {40, 9}, {43, 7}, {46, 8}, {49, 9}, {52, 8}, {55, 9}};
        for (int[] pl : planks) {
            lv.set(pl[0], pl[1], Level.CRUMBLE);
            shard(lv, pl[0], pl[1] - 2);
        }
        crow(lv, 45, 5f, 38, 54);

        // --- l'onda di fuoco: sette getti accesi uno dopo l'altro; correndo alla velocità giusta si passa tra le fiamme
        for (int i = 0; i < 7; i++) jet(lv, 64 + i * 4, i * 0.45f, 3.2f);
        shards(lv, 66, 7, 3);
        enemy(lv, 62, Enemy.BEETLE, 0, 0);
        row(lv, 74, 6, "B?B");
        enemy(lv, 92, Enemy.HOG, 0, 0);

        // --- la radura: qui il fuoco si ferma. Un'altura di terra con la cella energetica in cima
        hill(lv, 100, 114, 3);
        row(lv, 102, 5, "?B?B?");
        shards(lv, 98, 8, 3);
        enemy(lv, 118, Enemy.ACORN, 0, 0);
        enemy(lv, 121, Enemy.BEETLE, 0, 0);

        // --- il bosco bruciato: tronchi di diversa altezza con un getto tra l'uno e l'altro
        column(lv, 131, 2);
        column(lv, 135, 3);
        column(lv, 139, 2);
        column(lv, 143, 4);
        jet(lv, 133, 0.0f, 2.8f);
        jet(lv, 137, 1.1f, 2.8f);
        jet(lv, 141, 2.0f, 2.8f);
        shards(lv, 132, 7, 1); shards(lv, 136, 6, 1); shards(lv, 140, 7, 1);
        enemy(lv, 146, Enemy.HOG, 0, 0);

        // --- bivio: a terra i getti, in alto la torre di tavole che crollano (più gemme, meno fuoco)
        jet(lv, 154, 0.0f, 2.6f);
        jet(lv, 157, 0.9f, 2.6f);
        jet(lv, 160, 1.8f, 2.6f);
        lv.set(152, 8, Level.CRUMBLE); shard(lv, 152, 6);
        lv.set(155, 6, Level.CRUMBLE); shard(lv, 155, 4);
        lv.set(158, 4, Level.CRUMBLE); lv.set(159, 4, Level.CRUMBLE); shard(lv, 158, 2.8f); shard(lv, 159, 2.8f);
        lv.set(162, 6, Level.CRUMBLE); shard(lv, 162, 4);
        lv.set(165, 8, Level.CRUMBLE); shard(lv, 165, 6);
        enemy(lv, 167, Enemy.ACORN, 0, 0);

        // --- la lava: sedici caselle di vuoto, si attraversano su tavole sospese mentre il fuoco accelera
        int[][] over = {{172, 9}, {175, 8}, {178, 7}, {181, 8}, {184, 9}};
        for (int[] pl : over) {
            lv.set(pl[0], pl[1], Level.CRUMBLE);
            shard(lv, pl[0], pl[1] - 2);
        }
        crow(lv, 178, 4.6f, 172, 186);

        // --- corsa finale: getti ravvicinati con ritmi diversi
        for (int i = 0; i < 6; i++) jet(lv, 191 + i * 4, (i % 3) * 0.8f, 2.4f);
        enemy(lv, 193, Enemy.HOG, 0, 0);
        row(lv, 201, 7, "B?B");
        enemy(lv, 213, Enemy.ACORN, 0, 0);
        enemy(lv, 217, Enemy.BEETLE, 0, 0);
        shards(lv, 205, 8, 4);

        lv.flagX = 224;
        lv.flagBaseY = GROUND_Y;
        return lv;
    }

    /** Getto di fiamma che spunta dal terreno (o dalla cima di un'altura) alla colonna x. */
    private static void jet(Level lv, int x, float phase, float period) {
        lv.jets.add(new float[] {x, standRow(lv, x) + 1, period, phase});
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
