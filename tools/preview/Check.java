import com.valeriodipaolo.platformgame.core.*;

/** Controlli automatici su salto, raggiungibilità dei blocchi, monete e punti vita. */
public class Check {
    static int fails = 0;
    static void ok(boolean c, String m) { System.out.println((c ? "OK   " : "FALLITO ") + m); if (!c) fails++; }

    static Game fresh() { Game g = new Game(); g.viewW = 22; return g; }
    static void runS(Game g, float sec, boolean l, boolean r, boolean j, boolean sp) {
        g.setInput(l, r, j, sp);
        for (float t = 0; t < sec; t += 1f / 60f) g.update(1f / 60f);
    }
    static void run(Game g, float sec, boolean l, boolean r, boolean j) {
        g.setInput(l, r, j);
        for (float t = 0; t < sec; t += 1f / 60f) g.update(1f / 60f);
    }

    /** Prova stacchi e rincorse diversi: salta, in volo lascia la direzione e vede se atterra sul blocco. */
    static boolean landsOn(float blockX, int blockY) {
        for (float runup : new float[] {0f, 1.5f, 3f}) {
            for (float start = blockX - 6; start <= blockX; start += 0.25f) {
                for (float release = 0.1f; release <= 0.7f; release += 0.05f) {
                    Game g = fresh();
                    g.player.x = start - runup; g.player.y = 8.8f;
                    run(g, 0.3f, false, false, false);
                    g.player.x = start - runup;
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
        }
        return false;
    }

    /** Segue un salto con la sequenza di pressioni indicata: ogni coppia {da, a} in secondi è una pressione. */
    static float maxRise(float[][] presses, boolean spinHold) {
        Game g = fresh();
        run(g, 0.5f, false, false, false);
        float y0 = g.player.y, top = y0;
        for (float t = 0; t < 2.5f; t += 1f / 60f) {
            boolean j = false;
            for (float[] pr : presses) if (t >= pr[0] && t < pr[1]) j = true;
            g.setInput(false, false, j, spinHold && t > 0.6f);
            g.update(1f / 60f);
            top = Math.min(top, g.player.y);
        }
        return y0 - top;
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
        for (float[] c : lv.shards) {
            boolean overGround = lv.solid((int) c[0], 10);
            if (overGround && c[1] >= 8 && c[1] < 9) high++;
        }
        ok(high == 0, "nessuna moneta a livello della testa sopra il terreno, irraggiungibile camminando");
        // livello 3: sotto ogni cassa/mattone ci devono essere almeno due caselle libere, e nessun oggetto dentro il terreno
        int tight = 0, buried = 0;
        for (Level l3 : new Level[] {LevelThree.build(), LevelFive.build(), LevelSeven.build()}) {
        for (int y = 0; y < l3.h; y++) for (int x = 0; x < l3.w; x++) {
            char c = l3.get(x, y);
            if (c != Level.BRICK && c != Level.BONUS && c != Level.SECRET) continue;
            int t = y + 1;
            while (t < l3.h && !(l3.get(x, t) == Level.GROUND || l3.get(x, t) == Level.STONE)) t++;
            if (t < l3.h && t - y - 1 < 2) { tight++; System.out.println("L3 cassa troppo bassa: " + x + "," + y); }
        }
        for (float[] c : l3.shards) if (l3.solid((int) c[0], (int) c[1])) { buried++; System.out.println("L3 gemma nel terreno: " + c[0] + "," + c[1]); }
        for (float[] e : l3.enemies) if (e[2] != Enemy.CROW && l3.solid((int) e[0], (int) e[1])) { buried++; System.out.println("L3 nemico nel terreno: " + e[0] + "," + e[1]); }
        }
        ok(tight == 0 && buried == 0, "livelli 3, 5 e 7: casse raggiungibili e oggetti fuori dal terreno");
        Game w = fresh();
        run(w, 1.4f, false, true, false);
        ok(w.gems >= 2, "camminando all'inizio si prendono pezzi di gemma: " + w.gems);

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
        // gettone d'oro del livello 1: dentro la base della prima piramide, dietro una finta parete
        Game b = fresh();
        ok(b.coin != null && b.level.get((int) b.coin[0], (int) b.coin[1]) == Level.FAKE, "il gettone è nascosto dentro la finta parete: " + (b.coin == null ? "-" : (int) b.coin[0]));
        b.enemies.clear();
        b.player.x = b.coin[0] + 5; b.player.y = 8.8f;
        run(b, 1.2f, true, false, false);
        ok(b.coinOwned && b.coin == null, "tornando indietro contro il gradino più basso si entra nella finta parete e si prende il gettone");
        Game ft = fresh();
        int fx = -1; for (int x = 0; x < ft.level.w && fx < 0; x++) if (ft.level.get(x, 9) == Level.FAKE) fx = x;
        ft.player.x = fx + 0.2f; ft.player.y = 6.5f; ft.player.vy = 0;
        run(ft, 0.6f, false, false, false);
        ok(ft.player.onGround && ft.player.y + ft.player.h <= 9.05f, "sopra la finta parete si sta in piedi come sulla pietra: piedi a " + (ft.player.y + ft.player.h));

        // gettone nel mattoncino (livello 7): solo colpendo da sotto quello giusto
        Game sg = new Game(new MemorySave(), 7);
        int secrets = 0, sx = -1, sy = -1;
        for (int y = 0; y < sg.level.h; y++) for (int x = 0; x < sg.level.w; x++) if (sg.level.get(x, y) == Level.SECRET) { secrets++; sx = x; sy = y; }
        ok(secrets == 1 && sg.coin == null, "nel livello 7 c'è un solo mattoncino segreto: (" + sx + "," + sy + ")");
        for (int y = 0; y < sg.level.h; y++) for (int x = 0; x < sg.level.w; x++) {
            if (sg.level.get(x, y) == Level.BRICK) sg.hitBlock(x, y);
        }
        ok(!sg.coinOwned, "colpire gli altri mattoncini non dà il gettone");
        sg.player.x = sx + 0.19f; sg.player.y = sy + 1.0f; sg.player.vy = -8f;       // testa sotto il mattoncino, in salita
        for (int i = 0; i < 30; i++) { sg.setInput(false, false, true); sg.update(1f / 60f); }
        ok(sg.coinOwned && sg.level.get(sx, sy) == Level.USED, "dando la capocciata al mattoncino segreto si ottiene il gettone");

        // gettone addosso a un nemico (livello 3, il primo corvo): eliminandolo schizza fuori
        Game fg = new Game(new MemorySave(), 3);
        ok(fg.coinFoe != null && fg.coinFoe.type == Enemy.CROW, "nel livello 3 il gettone ce l'ha un corvo");
        fg.coinFoe.flatTime = 0.45f;
        fg.update(1f / 60f);
        ok(fg.coinOwned && fg.coinFoe == null, "schiacciando il corvo giusto si ottiene il gettone");
        for (int id : new int[] {1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 25, 27, 29, 31, 33}) {
            Game q = new Game(new MemorySave(), id);
            int sec = 0; for (int y = 0; y < q.level.h; y++) for (int x = 0; x < q.level.w; x++) if (q.level.get(x, y) == Level.SECRET) sec++;
            int ways = (q.coin != null ? 1 : 0) + (q.coinFoe != null ? 1 : 0) + sec;
            ok(ways == 1, "livello " + id + ": un gettone d'oro e un solo nascondiglio");
        }

        // gemma completa: tutti i pezzi sparsi più le casse bonus
        Game gm = fresh();
        ok(gm.gemTotal > gm.shards.size(), "il totale delle gemme comprende anche le casse bonus: " + gm.gemTotal);
        java.util.List<float[]> all = new java.util.ArrayList<float[]>(gm.shards);
        for (float[] c : all) {
            gm.player.x = c[0]; gm.player.y = c[1]; gm.player.vy = 0;
            gm.update(1f / 120f);
        }
        ok(gm.shards.isEmpty() && !gm.gemOwned, "presi tutti i pezzi sparsi ma non le casse, la gemma non è ancora completa");
        int steel = 0;
        for (int y = 0; y < gm.level.h; y++) for (int x = 0; x < gm.level.w; x++) if (gm.level.get(x, y) == Level.BONUS) gm.hitBlock(x, y); else if (gm.level.get(x, y) == Level.STEEL) steel++;
        ok(steel > 0 && !gm.gemOwned, "le casse rinforzate (" + steel + ") non si aprono con la capocciata: gemma non ancora completa");
        // la giravolta contro la cassa rinforzata la apre
        for (int y = 0; y < gm.level.h; y++) for (int x = 0; x < gm.level.w; x++) if (gm.level.get(x, y) == Level.STEEL) {
            for (int i = 0; i < 120; i++) { gm.setInput(false, false, false, false); gm.update(1f / 60f); }      // finita la giravolta di prima
            gm.hitBlock(x, y);
            ok(gm.level.get(x, y) == Level.STEEL, "capocciata semplice sulla cassa rinforzata: resta chiusa");
            gm.player.x = x - 0.7f; gm.player.y = y + 0.1f; gm.player.vx = 0; gm.player.vy = 0;
            gm.setInput(false, false, false, true); gm.update(1f / 60f); gm.setInput(false, false, false, false);
            for (int i = 0; i < 10; i++) gm.update(1f / 120f);
        }
        ok(gm.gemOwned && gm.gems == gm.gemTotal, "con la giravolta anche le rinforzate si aprono e si ottiene la gemma completa");

        // ricominciando il livello i collezionabili ripartono da zero; il record resta salvato
        MemorySave ms = new MemorySave();
        Game s1 = new Game(ms);
        float[] cpos = s1.coin.clone();
        s1.player.x = cpos[0]; s1.player.y = cpos[1] - 0.3f;
        s1.update(1f / 60f);
        ok(s1.coinOwned && ms.has("l1_coin"), "gettone ottenuto e registrato");
        s1.newGame();
        ok(!s1.coinOwned && !s1.gemOwned && s1.gems == 0, "ricominciando il livello i collezionabili sono azzerati");
        ok(s1.coin != null, "e il gettone è di nuovo al suo posto");
        ok(ms.has("l1_coin"), "il record dei collezionabili mai ottenuti resta salvato");
        Game s2 = new Game(ms);
        ok(!s2.coinOwned, "anche riaprendo il gioco si riparte da zero");

        // trottola: travolge i nemici senza perdere punti vita, poi ha un tempo di ricarica
        Game sp = fresh();
        sp.enemies.clear();
        Enemy en = new Enemy(20, 9); en.active = true;
        sp.enemies.add(en);
        sp.player.x = 3; sp.player.y = 8.8f;
        run(sp, 0.3f, false, false, false);
        sp.setInput(false, false, false, true);
        sp.update(1f / 60f);
        ok(sp.spinning(), "premendo la trottola il robot gira");
        en.x = sp.player.x + 0.5f; en.y = 10 - en.h; en.dead = false; en.flung = false;
        for (int i = 0; i < 10; i++) { sp.setInput(false, false, false, true); sp.update(1f / 60f); }
        ok(en.flung && sp.hp == 3, "la trottola travolge il nemico senza fare danno: hp=" + sp.hp);
        run(sp, 0.9f, false, false, false);
        ok(!sp.spinning(), "la trottola finisce dopo meno di un secondo");
        sp.setInput(false, false, false, true);
        sp.update(1f / 60f);
        ok(!sp.spinning() || sp.spinCool <= 0, "senza rilasciare il pulsante non riparte di continuo");
        run(sp, 0.1f, false, false, false);
        run(sp, 0.8f, false, false, false);
        sp.setInput(false, false, false, true);
        sp.update(1f / 60f);
        ok(sp.spinning(), "dopo la ricarica si può rifare");

        // ---- doppio salto
        float single = maxRise(new float[][] {{0, 2}}, false);
        float dbl = maxRise(new float[][] {{0, 0.1f}, {0.2f, 2}}, false);
        ok(dbl > single + 1.5f, String.format("doppio salto in salita: %.2f caselle contro %.2f del salto singolo", dbl, single));
        float tooLate = maxRise(new float[][] {{0, 0.45f}, {0.7f, 2}}, false);
        float onlyFirst = maxRise(new float[][] {{0, 0.45f}}, false);
        ok(Math.abs(tooLate - onlyFirst) < 0.05f, String.format("seconda pressione in discesa: nessun doppio salto (%.2f = %.2f)", tooLate, onlyFirst));
        float triple = maxRise(new float[][] {{0, 0.1f}, {0.2f, 0.3f}, {0.4f, 2}}, false);
        float twice = maxRise(new float[][] {{0, 0.1f}, {0.2f, 0.3f}}, false);
        ok(Math.abs(triple - twice) < 0.6f, "non esiste il triplo salto");
        float dblShort = maxRise(new float[][] {{0, 0.1f}, {0.2f, 0.25f}}, false);
        ok(dblShort < dbl - 0.8f, String.format("anche il secondo salto è più alto tenendo premuto: %.2f contro %.2f", dbl, dblShort));
        // doppio salto appena dopo essere scesi da un bordo (tolleranza) e dopo aver rimbalzato su un nemico
        Game ds = fresh();
        run(ds, 0.5f, false, false, false);
        ds.setInput(false, false, true); ds.update(1f / 60f);
        ds.setInput(false, false, false); run(ds, 0.05f, false, false, false);
        float vyBefore = ds.player.vy;
        ds.setInput(false, false, true); ds.update(1f / 60f);
        ok(vyBefore < 0 && ds.player.vy < -15f, "seconda pressione mentre sale: la velocità verso l'alto riparte (vy=" + ds.player.vy + ")");

        // ---- direzione bloccata in aria e movimento limitato
        Game fc = fresh();
        run(fc, 0.3f, false, true, false);
        ok(fc.facing == 1, "a terra va a destra, guarda a destra");
        fc.setInput(false, true, true); fc.update(1f / 60f);
        for (int i = 0; i < 30; i++) { fc.setInput(true, false, true); fc.update(1f / 60f); }
        ok(!fc.player.onGround && fc.facing == 1, "in salto, premendo indietro il personaggio non si volta");
        run(fc, 3f, false, false, false);
        run(fc, 0.3f, true, false, false);
        ok(fc.facing == -1, "a terra si volta normalmente");

        Game ga = fresh();
        run(ga, 0.5f, false, false, false);
        run(ga, 0.1f, false, true, false);
        float groundV = ga.player.vx;
        Game gb = fresh();
        run(gb, 0.5f, false, false, false);
        gb.setInput(false, false, true); gb.update(1f / 60f);
        runS(gb, 0.1f, false, true, true, false);
        ok(gb.player.vx < groundV * 0.6f, String.format("in aria si accelera molto meno: %.1f contro %.1f a terra", gb.player.vx, groundV));
        Game gc = fresh();
        run(gc, 0.5f, false, true, false);
        gc.setInput(false, true, true); gc.update(1f / 60f);
        float airV = gc.player.vx;
        run(gc, 0.3f, false, false, true);
        ok(gc.player.vx > airV * 0.7f, "in aria lasciando la direzione si conserva lo slancio");

        // ---- giravolta: tenendo premuto dura di più; in aria fa planare
        Game sh = fresh();
        run(sh, 0.5f, false, false, false);
        sh.setInput(false, false, false, true); sh.update(1f / 60f);
        runS(sh, 0.3f, false, false, false, true);
        ok(sh.spinning(), "giravolta tenuta premuta: dopo 0,3 s gira ancora");
        runS(sh, 0.6f, false, false, false, true);
        ok(sh.spinning(), "giravolta tenuta premuta: dopo 0,9 s gira ancora");
        runS(sh, 0.6f, false, false, false, true);
        ok(!sh.spinning(), "giravolta tenuta premuta: dopo 1,5 s è finita comunque");
        Game st = fresh();
        run(st, 0.5f, false, false, false);
        st.setInput(false, false, false, true); st.update(1f / 60f);
        run(st, 0.2f, false, false, false);
        ok(st.spinning(), "giravolta con un tocco: dopo 0,2 s gira ancora (durata minima)");
        run(st, 0.35f, false, false, false);
        ok(!st.spinning(), "giravolta con un tocco: dopo 0,55 s è finita");

        Game gl = fresh();
        run(gl, 0.5f, false, false, false);
        runS(gl, 0.45f, false, false, true, false);              // salto tenuto premuto fino quasi alla cima
        Game gf = fresh();
        run(gf, 0.5f, false, false, false);
        runS(gf, 0.45f, false, false, true, false);
        runS(gf, 0.3f, false, false, false, false);
        float fallFree = gf.player.vy;
        runS(gl, 0.3f, false, false, false, true);               // in cima: giravolta tenuta premuta
        ok(gl.spinning() && !gl.player.onGround && gl.player.vy > 0 && gl.player.vy <= 2.6f && fallFree > 8f,
           String.format("giravolta in aria: plana a %.1f caselle/s invece di %.1f", gl.player.vy, fallFree));
        float yGl = gl.player.y;
        runS(gl, 0.3f, false, false, false, true);
        ok(gl.player.y - yGl < 1.0f, String.format("durante la planata scende di %.2f caselle in 0,3 s", gl.player.y - yGl));
        runS(gl, 1.0f, false, false, false, false);
        ok(gl.player.onGround, "finita la giravolta il robot torna a cadere normalmente e atterra");

        // zone dei pulsanti su uno schermo largo 2400x1080
        ok(Controls.zone(100, 900, 2400, 1080) == Controls.LEFT, "zona sinistra");
        ok(Controls.zone(500, 900, 2400, 1080) == Controls.RIGHT, "zona destra");
        ok(Controls.zone(Controls.jumpX(2400, 1080) + 100, 900, 2400, 1080) == Controls.JUMP, "zona salto");
        ok(Controls.zone(Controls.spinX(2400, 1080) + 100, 900, 2400, 1080) == Controls.SPIN, "zona trottola");
        float sz = Controls.size(2400, 1080), spx = Controls.spinX(2400, 1080), jpx = Controls.jumpX(2400, 1080);
        ok(Controls.zone(spx - 0.3f * sz, 900, 2400, 1080) == Controls.SPIN, "un tocco 0,3 pulsanti a sinistra della giravolta vale ancora giravolta");
        ok(Controls.zone(spx + 1.05f * sz, 900, 2400, 1080) == Controls.SPIN, "e anche appena a destra, nello spazio tra giravolta e salto");
        ok(Controls.zone(jpx - 0.05f * sz, 900, 2400, 1080) == Controls.JUMP, "il bordo sinistro del salto vale salto");
        ok(Controls.zone(2399, 1079, 2400, 1080) == Controls.JUMP && Controls.zone(spx + 0.5f * sz, 5, 2400, 1080) == Controls.SPIN, "vale sull'intera altezza, anche in alto e agli angoli");
        ok(Controls.zone(1300, 300, 2400, 1080) == Controls.JUMP, "il salto resta valido su tutta la metà destra tranne la fascia della trottola");

        System.out.println(fails == 0 ? "TUTTO OK" : fails + " controlli falliti");
        System.exit(fails == 0 ? 0 : 1);
    }
}
