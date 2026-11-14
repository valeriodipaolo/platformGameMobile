package com.valeriodipaolo.platformgame.core;

/**
 * Nono livello, il prato di primavera: ciliegi in fiore, tre fiumi da attraversare rimbalzando di isolotto in isolotto sui fiori a molla,
 * una passerella di pietre tra le chiome, un campo di rovi da scavalcare (a terra) oppure da sorvolare (sulle pietre in alto). Il terreno
 * base occupa le righe 10 e 11; l'acqua scorre sotto ai burroni. Difficoltà come i livelli 7 e 8 al massimo.
 */
public final class LevelNine {
    private LevelNine() {}

    private static final int H = 12, GROUND_Y = 10;

    public static Level build() {
        Level lv = new Level(240, H);
        lv.id = "l9";
        lv.theme = 4;
        ground(lv, 0, 36);
        ground(lv, 40, 42);          // primo fiume: due isolotti con un fiore a molla al centro
        ground(lv, 47, 49);
        ground(lv, 54, 84);
        ground(lv, 89, 91);          // secondo fiume: tre isolotti
        ground(lv, 97, 99);
        ground(lv, 105, 107);
        ground(lv, 114, 190);
        ground(lv, 195, 197);        // terzo fiume
        ground(lv, 203, 205);
        ground(lv, 212, 239);
        lv.startX = 3;
        lv.startY = GROUND_Y - 1.2f;

        // --- prato d'ingresso: i primi fiori a molla e un campetto di rovi
        shards(lv, 7, 9, 4);
        row(lv, 11, 7, "B?B");
        enemy(lv, 15, Enemy.BEETLE, 0, 0);
        pad(lv, 19);
        arc(lv, 17, 22, 3.2f);
        thorn(lv, 26);
        thorn(lv, 27);
        shards(lv, 25, 7, 4);
        enemy(lv, 31, Enemy.ACORN, 0, 0);
        crow(lv, 33, 5.5f, 28, 36);

        // --- primo fiume: dal prato all'isolotto, poi sul fiore si rimbalza fino al successivo
        arc(lv, 37, 40, 7.2f);
        pad(lv, 41);
        arc(lv, 43, 46, 3.4f);
        pad(lv, 48);
        arc(lv, 50, 53, 4.0f);
        crow(lv, 44, 4.2f, 38, 52);

        // --- bosco di ciliegi: un'altura, rovi e la passerella di pietre tra le chiome
        hill(lv, 58, 66, 3);
        row(lv, 60, 5, "?B?");
        enemy(lv, 70, Enemy.HOG, 0, 0);
        thorn(lv, 69);
        thorn(lv, 73);
        thorn(lv, 77);
        pad(lv, 71);
        for (int x = 72; x <= 80; x++) lv.set(x, 5, Level.STONE);        // passerella di pietre tra le chiome
        shards(lv, 72, 3, 8);
        lv.battery = new float[] {78, 3.9f};
        crow(lv, 74, 2.6f, 70, 82);
        enemy(lv, 62, Enemy.ACORN, 0, 0);

        // --- secondo fiume: tre isolotti a distanza di rimbalzo
        arc(lv, 85, 89, 7.0f);
        pad(lv, 90);
        arc(lv, 92, 96, 3.0f);
        pad(lv, 98);
        arc(lv, 100, 104, 3.0f);
        pad(lv, 106);
        arc(lv, 108, 113, 3.4f);
        crow(lv, 95, 4.6f, 88, 108);

        // --- prato di sosta con ciliegi
        hill(lv, 118, 130, 3);
        row(lv, 120, 5, "?B?B?");
        enemy(lv, 134, Enemy.BEETLE, 0, 0);
        enemy(lv, 137, Enemy.ACORN, 0, 0);
        shards(lv, 115, 9, 3);

        // --- il campo di rovi: a terra si scavalcano uno per volta, in alto una passerella di pietre li sorvola
        pad(lv, 143);
        thorn(lv, 147);
        thorn(lv, 150);
        thorn(lv, 154);
        thorn(lv, 158);
        thorn(lv, 161);
        thorn(lv, 165);
        thorn(lv, 168);
        for (int x : new int[] {147, 148, 149, 153, 154, 155, 159, 160, 161, 165, 166, 167}) lv.set(x, 5, Level.STONE);
        shards(lv, 147, 3, 3);
        shards(lv, 153, 3, 3);
        shards(lv, 159, 3, 3);
        shards(lv, 165, 3, 3);
        enemy(lv, 174, Enemy.HOG, 0, 0);
        crow(lv, 158, 3.0f, 146, 170);
        row(lv, 176, 7, "B?B");
        enemy(lv, 182, Enemy.BEETLE, 0, 0);
        shards(lv, 178, 9, 4);

        // --- terzo fiume e il portone
        arc(lv, 191, 195, 7.0f);
        pad(lv, 196);
        arc(lv, 198, 202, 3.2f);
        pad(lv, 204);
        arc(lv, 206, 211, 3.4f);
        crow(lv, 200, 4.4f, 193, 210);
        enemy(lv, 218, Enemy.ACORN, 0, 0);
        thorn(lv, 222);
        thorn(lv, 223);
        shards(lv, 220, 7, 5);
        enemy(lv, 227, Enemy.BEETLE, 0, 0);

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

    /** Gemme su un arco (come la traiettoria di un rimbalzo) tra le colonne x0 e x1, con il vertice alla quota apex. */
    private static void arc(Level lv, int x0, int x1, float apex) {
        int n = x1 - x0 + 1;
        for (int i = 0; i < n; i++) {
            float u = n == 1 ? 0.5f : (float) i / (n - 1);
            float y = GROUND_Y - 1.2f - (GROUND_Y - 1.2f - apex) * 4f * u * (1f - u);
            shard(lv, x0 + i, y);
        }
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
