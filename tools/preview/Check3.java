import com.valeriodipaolo.platformgame.core.MemorySave;
import com.valeriodipaolo.platformgame.core3d.*;

/** Controlli automatici sul livello 3D: fisica, collezionabili e un bot che percorre tutto il corridoio. */
public class Check3 {
    static int fails = 0;
    static void ok(boolean c, String m) { System.out.println((c ? "OK   " : "FALLITO ") + m); if (!c) fails++; }
    static World3D fresh() {
        World3D w = new World3D(new MemorySave(), Integer.getInteger("lv", 2));
        if (w.levelNo == 8 && System.getProperty("haz") == null) {      // il bot non sa schivare fuoco, getti e tronchi: controlla solo la geometria
            w.jets.clear(); w.logs.clear(); w.fireStart = Float.NaN; w.fireZ = Float.NaN;
        }
        return w;
    }
    static void run(World3D w, float sec, float jx, float jy, boolean j, boolean s) {
        w.setInput(jx, jy, j, s);
        for (float t = 0; t < sec; t += 1f / 60f) w.update(1f / 60f);
    }
    static float rise(float[][] presses) {
        World3D w = fresh();
        run(w, 0.3f, 0, 0, false, false);
        float top = 0;
        for (float t = 0; t < 2.5f; t += 1f / 60f) {
            boolean j = false;
            for (float[] p : presses) if (t >= p[0] && t < p[1]) j = true;
            w.setInput(0, 0, j, false);
            w.update(1f / 60f);
            top = Math.max(top, w.py);
        }
        return top;
    }

    /** Ogni nemico parte in un punto libero, non resta bloccato e in 40 secondi di ronda non entra mai in un blocco, in una cassa o nel vuoto. */
    static void enemyChecks() {
        int n = fresh().enemies.size();
        for (int i = 0; i < n; i++) {
            World3D w = fresh();
            Enemy3 e = w.enemies.get(i);
            String id = "nemico " + i + " (tipo " + e.type + ", x=" + e.x + " z=" + e.z + ")";
            ok(!w.enemyBlocked(e, e.x, e.z), id + " parte in un punto libero");
            w.state = World3D.WON;                          // niente colpi al giocatore durante la prova
            float x0 = e.x, z0 = e.z, minA = 1e9f, maxA = -1e9f;
            boolean clip = false;
            for (int f = 0; f < 60 * 40; f++) {
                w.pz = e.z;
                w.advanceEnemies(1f / 60f);
                float a = e.alongZ ? e.z : e.x;
                minA = Math.min(minA, a); maxA = Math.max(maxA, a);
                float floor = e.floor, hw = 0.3f;
                for (float dx = -hw; dx <= hw && !clip; dx += hw)
                    for (float dz = -hw; dz <= hw; dz += hw) {
                        int h = w.cell((int) Math.floor(e.x + dx + World3D.HALF), (int) Math.floor(e.z + dz));
                        float feet = e.type == Enemy3.CROW ? e.y - 0.1f : floor;
                        if (h == 99 || (h != World3D.PIT && h > feet + 0.05f)) { clip = true; break; }
                    }
                int hc = w.cell((int) Math.floor(e.x + World3D.HALF), (int) Math.floor(e.z));
                if (e.type != Enemy3.CROW && (hc == World3D.PIT || hc < e.floor - 0.05f)) clip = true;
            }
            ok(!clip, id + " non attraversa blocchi, casse o burroni");
            ok(maxA - minA > 0.8f, id + " si muove (percorso " + (maxA - minA) + " m)");
        }
    }

    /** Bot: va avanti scegliendo una colonna percorribile, salta burroni e gradini, usa la giravolta sui nemici e sulle casse. */
    static boolean bot(boolean verbose) {
        World3D w = fresh();
        boolean jump = false, held = false;
        float jumpTimer = 0;
        for (int f = 0; f < 60 * 240; f++) {
            float jx = 0, jy = 1;
            boolean spin = false;
            if (w.state == World3D.PLAYING) {
                int zi = (int) Math.floor(w.pz), xi = (int) Math.floor(w.px + World3D.HALF);
                // colonna migliore: quella col percorso libero più lungo davanti, a parità la più vicina
                int best = xi; float bestScore = -1e9f;
                for (int c = 0; c < World3D.W; c++) {
                    int free = 0;
                    for (int k = 1; k <= 7; k++) { int h = w.cell(c, zi + k); if (h == World3D.PIT) break; free++; }
                    boolean reachable = true;
                    for (int cc = Math.min(c, xi); cc <= Math.max(c, xi); cc++) if (w.cell(cc, zi) == World3D.PIT && w.onGround) reachable = false;
                    float sc = free * 2 - Math.abs(c - xi) * 1.1f - Math.abs(c - 2) * 0.2f - (reachable ? 0 : 100);
                    if (sc > bestScore) { bestScore = sc; best = c; }
                }
                float targetX = best - World3D.HALF + 0.5f;
                jx = Math.max(-1, Math.min(1, (targetX - w.px) * 2.5f));
                boolean aligned = Math.abs(targetX - w.px) < 0.35f;
                int here = w.cell(xi, zi) == World3D.PIT ? 0 : w.cell(xi, zi);
                boolean pitAhead = w.cell(best, zi + 1) == World3D.PIT && (w.pz - zi) > 0.55f;
                boolean stepAhead = false;
                for (int k = 1; k <= 2; k++) { int h = w.cell(best, zi + k); if (h != World3D.PIT && h > Math.max(0, (int) Math.floor(w.py + 0.01f))) stepAhead = true; }
                if (!aligned && (pitAhead || w.cell(xi, zi + 1) == World3D.PIT) && w.onGround) jy = 0.15f;
                if (w.onGround && (pitAhead || stepAhead) && (aligned || stepAhead)) { jump = true; jumpTimer = 0; }
                if (jump) { jumpTimer += 1f / 60f; if (jumpTimer > 0.55f || (w.onGround && jumpTimer > 0.15f)) jump = false; }
                for (Enemy3 e : w.enemies) {
                    if (!e.dead && !e.flung && e.flatTime <= 0 && Math.abs(e.z - w.pz) < 2.2f && Math.abs(e.x - w.px) < 2.2f && Math.abs(e.y - w.py) < 1.2f) spin = true;
                }
                for (int k = 0; k <= 1; k++) for (int c = xi - 1; c <= xi + 1; c++) {
                    if (c >= 0 && c < World3D.W && zi + k < w.length && w.crate[(zi + k) * World3D.W + c]) spin = true;
                }
            }
            boolean j = jump && !(w.onGround && held);
            held = j;
            int lives = w.lives;
            w.setInput(jx, jy, j, spin && (f % 90 < 80));
            w.update(1f / 60f);
            if (w.lives < lives && verbose) System.out.printf("  bot: vita persa a x=%.1f z=%.1f y=%.1f%n", w.px, w.pz, w.py);
            if (w.state == World3D.WON) {
                if (verbose) System.out.printf("  bot: livello completato in %.1fs, gemme %d/%d, vite %d%n", f / 60f, w.gems, w.gemTotal, w.lives);
                return true;
            }
            if (w.state == World3D.GAME_OVER) { if (verbose) System.out.println("  bot: partita finita a z=" + w.pz); return false; }
        }
        return false;
    }

    public static void main(String[] a) {
        if (System.getProperty("only") != null) { enemyChecks(); System.out.println(fails == 0 ? "NEMICI OK" : fails + " controlli falliti"); System.exit(fails == 0 ? 0 : 1); }
        float hold = rise(new float[][] {{0, 2}}), tap = rise(new float[][] {{0, 0.05f}});
        ok(hold > 2.1f && hold < 2.6f, String.format("salto tenuto premuto: %.2f caselle", hold));
        ok(tap > 0.9f && tap < 1.6f, String.format("salto breve: %.2f caselle", tap));
        float dbl = rise(new float[][] {{0, 0.1f}, {0.15f, 2}});
        ok(dbl > hold + 0.8f, String.format("doppio salto in salita: %.2f caselle", dbl));
        float late = rise(new float[][] {{0, 0.4f}, {0.75f, 2}}), first = rise(new float[][] {{0, 0.4f}});
        ok(Math.abs(late - first) < 0.05f, "seconda pressione in discesa: nessun doppio salto");

        World3D w = fresh();
        run(w, 0.3f, 0, 0, false, false);
        run(w, 0.5f, 1, 0, false, false);
        ok(w.px > 1.5f && w.px <= World3D.HALF - World3D.R + 0.01f, String.format("joystick a destra: va a destra e si ferma al bordo (x=%.2f)", w.px));
        float z0 = w.pz;
        run(w, 0.5f, 0, 1, false, false);
        ok(w.pz > z0 + 2.5f, "joystick in avanti: avanza nel corridoio");
        ok(Math.abs(w.yaw) < 0.3f, "andando avanti il robot guarda in avanti");
        run(w, 0.4f, 0, -1, false, false);
        ok(Math.abs(Math.abs(w.yaw) - Math.PI) < 0.4f, "andando indietro a terra si gira");

        // in aria: non si volta e si muove meno
        w = fresh();
        run(w, 0.3f, 0, 1, false, false);
        float yaw = w.yaw;
        w.setInput(0, 1, true, false); w.update(1f / 60f);
        run(w, 0.3f, -1, -1, true, false);
        ok(!w.onGround && Math.abs(w.yaw - yaw) < 0.01f, "in salto il robot non si volta");


        // burrone: cadendo si perde una vita e si riparte dal punto sicuro
        w = fresh();
        w.px = 0; w.pz = 15.5f; w.py = 0.5f;
        run(w, 1.2f, 0, 0, false, false);
        ok(w.state == World3D.DYING && w.lives == World3D.START_LIVES - 1, "cadendo nel burrone si perde una vita");
        run(w, 2f, 0, 0, false, false);
        ok(w.state == World3D.PLAYING && w.hp == 3 && w.pz < 15, "poi si riparte dal punto sicuro prima del burrone");

        // un salto con rincorsa supera il burrone di 3 caselle
        w = fresh();
        w.px = 0; w.pz = 11;
        boolean crossed = false;
        for (int i = 0; i < 240 && !crossed; i++) {
            boolean j = w.pz > 14.5f && w.pz < 17.5f;
            w.setInput(0, 1, j, false);
            w.update(1f / 60f);
            if (w.onGround && w.pz > 18) crossed = true;
        }
        ok(crossed && w.lives == World3D.START_LIVES, "il salto con rincorsa supera il primo burrone");

        // blocchi: contro un muretto ci si ferma, saltando ci si sale
        w = fresh();
        w.px = -2; w.pz = 20; 
        run(w, 1f, 0, 1, false, false);
        ok(w.pz < 22 - World3D.R + 0.01f, String.format("il muretto di pietra ferma il robot (z=%.2f)", w.pz));
        run(w, 0.25f, 0, 1, true, false);
        run(w, 0.6f, 0, 1, false, false);
        ok(w.pz > 22, "saltando si scavalca il muretto");

        // cassa: solida, la giravolta la rompe e dà una gemma
        w = fresh();
        w.px = 0; w.pz = 23.5f;
        run(w, 0.6f, 0, 1, false, false);
        ok(w.pz < 25 - World3D.R + 0.01f && w.crate[25 * World3D.W + 2], "la cassa ferma il robot");
        int g0 = w.gems;
        run(w, 0.3f, 0, 1, false, true);
        ok(!w.crate[25 * World3D.W + 2] && w.gems == g0 + 1, "la giravolta rompe la cassa e dà un pezzo di gemma");

        // giravolta su un nemico e planata
        w = fresh();
        w.px = 0; w.pz = 30.3f;
        Enemy3 e = w.enemies.get(0);
        e.x = 0.2f; e.z = 31.5f;
        run(w, 0.05f, 0, 0, false, false);
        run(w, 0.2f, 0, 1, false, true);
        ok(e.flung && w.hp == 3, "la giravolta fa volare via lo scarabeo senza danni");
        w = fresh();
        e = w.enemies.get(0);
        w.px = 0; w.pz = 30.6f; e.x = 0.1f; e.z = 31.2f;
        run(w, 0.3f, 0, 1, false, false);
        ok(w.hp == 2, "toccando lo scarabeo senza giravolta si perde un punto vita");
        w = fresh();
        run(w, 0.3f, 0, 0, false, false);
        run(w, 0.45f, 0, 0, true, false);
        run(w, 0.35f, 0, 0, false, true);
        ok(w.spinning() && !w.onGround && w.vy <= 0 && w.vy >= -1.7f, String.format("giravolta in aria: plana (velocità di discesa %.1f)", -w.vy));

        // gettone d'oro: nella nicchia di pietra sul bordo destro
        w = fresh();
        ok(w.coin != null && w.cell(4, (int) w.coin[2] - 1) == 3 && w.cell(4, (int) w.coin[2] + 1) == 3 && w.cell(3, (int) w.coin[2]) == 3, "il gettone sta in una nicchia chiusa da tre blocchi alti");
        w.px = w.coin[0]; w.pz = w.coin[2]; w.py = 0;
        run(w, 0.1f, 0, 0, false, false);
        ok(w.coinOwned && w.coin == null, "entrando nella nicchia si prende il gettone");
        w = fresh();
        int crates = 0; for (boolean c : w.crate) if (c) crates++;
        ok(w.gemTotal == w.gemsList.size() + crates && crates > 0, "totale gemme = pezzi sparsi + casse: " + w.gemTotal);
        for (float[] g : w.gemsList) {
            int cx = (int) Math.floor(g[0] + World3D.HALF), cz = (int) Math.floor(g[2]);
            int h = w.cell(cx, cz);
            boolean overPit = h == World3D.PIT;
            float clearance = g[1] - (overPit ? 0 : h);
            if (clearance > 2.9f || (!overPit && clearance < 0.3f)) ok(false, "gemma fuori portata a x=" + g[0] + " z=" + g[2]);
        }
        ok(true, "tutte le gemme sono a un'altezza raggiungibile");

        enemyChecks();
        ok(bot(true), "il bot percorre tutto il corridoio fino al traguardo");
        System.out.println(fails == 0 ? "TUTTO OK" : fails + " controlli falliti");
        System.exit(fails == 0 ? 0 : 1);
    }
}
