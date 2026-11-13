import com.valeriodipaolo.platformgame.core.*;

/** Controlli automatici su salto, raggiungibilità dei blocchi, monete e punti vita. */
public class Check {
    static int fails = 0;
    static void ok(boolean c, String m) { System.out.println((c ? "OK   " : "FALLITO ") + m); if (!c) fails++; }

    static Game fresh() { Game g = new Game(); g.viewW = 22; return g; }
    static void run(Game g, float sec, boolean l, boolean r, boolean j) {
        g.setInput(l, r, j);
        for (float t = 0; t < sec; t += 1f / 60f) g.update(1f / 60f);
    }

    /** Prova stacchi diversi: corre a destra, salta, in volo lascia la direzione e vede se atterra sul blocco. */
    static boolean landsOn(float blockX, int blockY) {
        for (float start = blockX - 6; start <= blockX; start += 0.25f) {
            for (float release = 0.15f; release <= 0.6f; release += 0.05f) {
                Game g = fresh();
                g.player.x = start - 3; g.player.y = 8.8f;
                run(g, 0.6f, false, false, false);
                g.player.x = start - 3;
                boolean jumped = false; float tj = 0;
                for (int i = 0; i < 60 * 4; i++) {
                    boolean jump = false, right = true;
                    if (!jumped && g.player.x >= start && g.player.onGround) { jumped = true; tj = 0; }
                    if (jumped) { tj += 1f / 60f; jump = tj < 0.5f; right = tj < release; }
                    g.setInput(false, right, jump);
                    g.update(1f / 60f);
                    if (jumped && g.player.onGround && Math.abs(g.player.y + g.player.h - blockY) < 0.01f) return true;
                    if (jumped && tj > 1.8f) break;
                }
            }
        }
        return false;
    }

    public static void main(String[] a) {
        // altezza massima del salto
        Game g = fresh();
        run(g, 0.5f, false, false, false);
        float y0 = g.player.y;
        float top = y0;
        g.setInput(false, false, true);
        for (int i = 0; i < 120; i++) { g.update(1f / 60f); top = Math.min(top, g.player.y); }
        ok(y0 - top > 4.5f, String.format("salto tenuto premuto: %.2f caselle", y0 - top));
        run(g, 1f, false, false, false);
        y0 = g.player.y; top = y0;
        g.setInput(false, false, true);
        g.update(1f / 60f);
        g.setInput(false, false, false);
        for (int i = 0; i < 120; i++) { g.update(1f / 60f); top = Math.min(top, g.player.y); }
        ok(y0 - top > 2.0f && y0 - top < 3.2f, String.format("salto breve: %.2f caselle", y0 - top));

        // ogni cassa/mattone del livello deve poter essere colpito da terra e ogni gruppo salito sopra
        Level lv = LevelOne.build();
        int unreachable = 0;
        for (int y = 0; y < lv.h; y++) for (int x = 0; x < lv.w; x++) {
            char c = lv.get(x, y);
            if (c != Level.BRICK && c != Level.BONUS) continue;
            int rise = 9 - y;                         // casella sopra la quale c'è il terreno: bordo superiore a y, piedi a 10
            if (y == 4) continue;                     // fila alta: si raggiunge da quella bassa, controllata sotto
            if (10 - y > 4.5f) { unreachable++; System.out.println("blocco troppo alto: " + x + "," + y); }
        }
        ok(unreachable == 0, "blocchi bassi: si può salirci sopra dal terreno (bordo a 3 caselle)");

        // le monete a terra si prendono camminando
        int high = 0;
        for (float[] c : lv.coins) {
            boolean overGround = lv.solid((int) c[0], 10);
            if (overGround && c[1] >= 8 && c[1] < 9) high++;
        }
        ok(high == 0, "nessuna moneta a livello della testa sopra il terreno, irraggiungibile camminando");
        Game w = fresh();
        run(w, 1.4f, false, true, false);
        ok(w.coinCount >= 2, "camminando all'inizio si prendono monete: " + w.coinCount);

        // si può salire sopra il blocco singolo (17,7) e sopra la fila di mattoni (82..89,7)
        ok(landsOn(17, 7), "si riesce a salire sopra il blocco a (17,7)");
        ok(landsOn(85, 7), "si riesce a salire sopra la fila di mattoni a (85,7)");

        // punti vita
        Game d = fresh();
        d.enemies.clear();
        Enemy e = new Enemy(d.player.x + 3, 9);
        d.enemies.add(e);
        for (int k = 1; k <= 3; k++) {
            e.x = d.player.x; e.y = d.player.y + d.player.h - e.h; e.dead = false; e.flatTime = 0; e.active = true;
            d.setInput(false, false, false);
            int livesBefore = d.lives;
            d.invuln = 0;
            d.update(1f / 60f);
            if (k < 3) ok(d.hp == 3 - k && d.lives == livesBefore && d.state == Game.PLAYING, "colpo " + k + ": hp=" + d.hp + " vite=" + d.lives);
            else ok(d.hp == 0 && d.lives == livesBefore - 1 && d.state == Game.DYING, "colpo 3: perde una vita, vite=" + d.lives);
            run(d, 0.1f, false, false, false);
        }
        run(d, 2.5f, false, false, false);
        ok(d.state == Game.PLAYING && d.hp == 3 && d.invuln > 0, "dopo la morte si riparte con 3 punti vita e invulnerabilità");
        System.out.println(fails == 0 ? "TUTTO OK" : fails + " controlli falliti");
        System.exit(fails == 0 ? 0 : 1);
    }
}
