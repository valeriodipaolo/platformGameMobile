import com.valeriodipaolo.platformgame.core.*;
import com.valeriodipaolo.platformgame.core3d.*;

/** Controlli sulla primavera: fiori a molla, rovi, ripartenza dopo la morte e percorribilità dei livelli 9 e 10 con un robot automatico. */
public class CheckSpring {
    static int fails;

    static void ok(boolean c, String m) {
        System.out.println((c ? "OK   " : "FALLITO ") + m);
        if (!c) fails++;
    }

    static boolean embedded(Game g) {
        Body p = g.player;
        for (int ty = (int) Math.floor(p.y + 0.02f); ty <= (int) Math.floor(p.y + p.h - 0.02f); ty++)
            for (int tx = (int) Math.floor(p.x + 0.02f); tx <= (int) Math.floor(p.x + p.w - 0.02f); tx++)
                if (g.level.solid(tx, ty)) return true;
        return false;
    }

    /** Robot 2D: corre a destra, salta davanti ai buchi, ai rovi e ai muri. Restituisce {morti, x finale, secondi}. */
    static float[] bot2d(Game g, float maxSec, float stopX) {
        g.enemies.clear();
        g.batteryOwned = true;                       // la cella è la chiave del portone: senza, il robot si fermerebbe davanti
        int deaths = 0, lives0 = g.lives;
        float jt = 0, rel = 0, stuck = 0, lastX = g.player.x, at = 0; boolean wide = false;
        for (float t = 0; t < maxSec; t += 1f / 60f) {
            Body p = g.player;
            boolean jump = false;
            if (g.state == Game.PLAYING) {
                int row = (int) Math.floor(p.y + p.h + 0.05f);
                boolean gap = false;
                int gw = 0;
                for (int dx = 1; dx <= 12 && !g.level.solid((int) Math.floor(p.x + p.w) + dx, row) && !g.level.solid((int) Math.floor(p.x + p.w) + dx, row + 1) && !g.level.solid((int) Math.floor(p.x + p.w) + dx, row + 2) && !g.level.solid((int) Math.floor(p.x + p.w) + dx, row + 3) && !g.level.solid((int) Math.floor(p.x + p.w) + dx, row - 1); dx++) gw++;
                float gd0 = gw >= 5 ? 0.1f : 0.9f;
                if (gw >= 5) { int cx = (int) Math.floor(p.x + 0.4f); if (!g.level.solid(cx, row) && !g.level.solid(cx, row + 1) && !g.level.solid(cx, row + 2)) gap = true; gd0 = 5f; }
                for (float d = gd0; d <= gd0 + 0.2f; d += 0.2f) if (!g.level.solid((int) Math.floor(p.x + p.w + d), row) && !g.level.solid((int) Math.floor(p.x + p.w + d), row + 1) && !g.level.solid((int) Math.floor(p.x + p.w + d), row + 2) && !g.level.solid((int) Math.floor(p.x + p.w + d), row + 3) && !g.level.solid((int) Math.floor(p.x + p.w + d), row - 1)) gap = true;
                boolean thorn = false;
                for (float[] th : g.level.thorns) if (th[0] - p.x > -0.1f && th[0] - p.x < 1.8f) thorn = true;
                boolean pad = false;
                for (int d = 0; d <= 2; d++) if (g.level.get((int) Math.floor(p.x + p.w / 2) + d, 10) == Level.SPRING) pad = true;
                if (p.onGround && ((gap && !pad && (row >= 9 || g.level.get((int) Math.floor(p.x + 0.1f), row) == Level.CRUMBLE)) || thorn || p.hitWall) && jt <= 0 && rel <= 0) {
                    int land = 99, x1 = (int) Math.floor(p.x + p.w);
                    for (int dx = 2; dx <= 12 && land == 99; dx++)
                        for (int ry = row - 1; ry <= row + 2; ry++) if (g.level.solid(x1 + dx, ry)) land = dx;
                    jt = thorn || p.hitWall ? 0.5f : land <= 5 ? 0.1f : 1.4f; wide = !(thorn || p.hitWall) && land > 5 && gw >= 7;
                }
                if (p.onGround) { if (at > 0.2f && g.level.get((int) Math.floor(p.x + 0.1f), (int) Math.floor(p.y + p.h + 0.05f)) != Level.CRUMBLE) { jt = 0; rel = 0.05f; } at = 0; } else at += 1f / 60f;
                if (jt > 0) { jump = !(wide && at >= 0.4f && at < 0.45f); jt -= 1f / 60f; if (jt <= 0) rel = 0.08f; } else if (rel > 0) rel -= 1f / 60f;
                else if (!p.onGround && p.vy > 0 && !g.level.solid((int) Math.floor(p.x + p.w / 2), row + 1) && !g.level.solid((int) Math.floor(p.x + p.w / 2), row + 2)) jump = false;
            }
            g.setInput(false, true, jump);
            if (System.getProperty("trace") != null && g.player.x > Float.parseFloat(System.getProperty("trace")) && g.player.x < Float.parseFloat(System.getProperty("trace")) + Float.parseFloat(System.getProperty("tw", "12"))) System.out.printf("x %.2f y %.2f vy %.1f og %b jump %b at %.2f jt %.2f wide %b lives %d%n", g.player.x, g.player.y, g.player.vy, g.player.onGround, jump, at, jt, wide, g.lives);
            g.update(1f / 60f);
            if (g.state == Game.WON || g.player.x >= stopX) return new float[] {lives0 - g.lives, g.player.x, t};
            if (Math.abs(g.player.x - lastX) < 0.01f && g.state == Game.PLAYING) { stuck += 1f / 60f; if (stuck > 5f) return new float[] {lives0 - g.lives, g.player.x, -t}; } else { stuck = 0; lastX = g.player.x; }
        }
        return new float[] {lives0 - g.lives, g.player.x, maxSec};
    }

    public static void main(String[] a) {
        // ---- fiore a molla 2D
        Game g = new Game(new MemorySave(), 9);
        g.enemies.clear();
        g.player.x = 19.2f; g.player.y = 8.8f;
        g.setInput(false, false, false);
        float top = 99;
        for (int i = 0; i < 90; i++) { g.update(1f / 60f); top = Math.min(top, g.player.y); }
        ok(top < 3.0f, "il fiore a molla lancia in alto: quota minima " + top);
        // ---- rovi 2D
        Game t = new Game(new MemorySave(), 9);
        t.enemies.clear();
        float[] th = t.level.thorns.get(0);
        t.player.x = th[0] + 0.1f; t.player.y = th[1] + 1 - 1.2f;
        t.invuln = 0;
        int hp0 = t.hp;
        for (int i = 0; i < 10; i++) t.update(1f / 60f);
        ok(t.hp < hp0, "i rovi feriscono");
        // ---- ripartenza alla quota del punto sicuro (non incastrati nel terreno)
        Game r = new Game(new MemorySave(), 9);
        float[] res = bot2d(r, 200, 125f);
        System.out.println("     robot fino a x=125: morti " + (int) res[0] + ", x " + res[1] + ", t " + res[2]);
        r.player.y = 20f;
        for (int i = 0; i < 60 * 4; i++) { r.setInput(false, false, false); r.update(1f / 60f); }
        ok(r.state == Game.PLAYING && !embedded(r), "dopo la morte si riparte fuori dal terreno (x=" + r.player.x + ", y=" + r.player.y + ")");
        // ---- percorribilità del livello 9
        Game l9 = new Game(new MemorySave(), 9);
        float[] r9 = bot2d(l9, 400, 1e9f);
        System.out.println("     livello 9: morti " + (int) r9[0] + ", x finale " + r9[1] + ", stato " + l9.state + ", t " + r9[2]);
        ok(l9.state == Game.WON, "il robot automatico completa il livello 9");
        ok((int) r9[0] <= 2, "al massimo 2 morti del robot sul livello 9: " + (int) r9[0]);
        // oggetti fuori dal terreno
        Level lv = LevelNine.build();
        int bad = 0;
        for (float[] c : lv.shards) if (lv.solid((int) c[0], (int) c[1])) { bad++; System.out.println("   gemma nel terreno " + c[0] + "," + c[1]); }
        for (float[] e : lv.enemies) if (e[2] != Enemy.CROW && lv.solid((int) e[0], (int) e[1])) { bad++; System.out.println("   nemico nel terreno " + e[0] + "," + e[1]); }
        for (float[] x : lv.thorns) if (lv.solid((int) x[0], (int) x[1]) || !lv.solid((int) x[0], (int) x[1] + 1)) { bad++; System.out.println("   rovo sospeso " + x[0] + "," + x[1]); }
        ok(bad == 0, "livello 9: oggetti ben posati");
        int tight = 0;
        for (int y = 0; y < lv.h; y++) for (int x = 0; x < lv.w; x++) {
            char c = lv.get(x, y);
            if (c != Level.BRICK && c != Level.BONUS && c != Level.SECRET) continue;
            int tt = y + 1;
            while (tt < lv.h && !(lv.get(x, tt) == Level.GROUND || lv.get(x, tt) == Level.STONE)) tt++;
            if (tt < lv.h && tt - y - 1 < 2) { tight++; System.out.println("   cassa troppo bassa " + x + "," + y); }
        }
        ok(tight == 0, "livello 9: casse raggiungibili");

        // ---- livello 11 (cielo, 2D): il robot automatico lo completa su nuvole, ponti che cedono e trampolini
        Game l11 = new Game(new MemorySave(), 11);
        float[] r11 = bot2d(l11, 400, 1e9f);
        System.out.println("     livello 11: morti " + (int) r11[0] + ", x finale " + r11[1] + ", stato " + l11.state + ", t " + r11[2]);
        ok(l11.state == Game.WON, "il robot automatico completa il livello 11");
        ok(r11[0] <= 2, "al massimo 2 morti del robot sul livello 11: " + (int) r11[0]);
        // sotto ogni mattone o cassa del livello 11 il robot passa e può colpirlo da sotto: nessun ostacolo nelle due caselle sotto
        int blocked11 = 0;
        for (int tx = 0; tx < l11.level.w; tx++) for (int ty = 0; ty < l11.level.h - 3; ty++) {
            char c = l11.level.get(tx, ty);
            if ((c == Level.BRICK || c == Level.BONUS || c == Level.SECRET) && (l11.level.solid(tx, ty + 1) || l11.level.solid(tx, ty + 2))) { blocked11++; System.out.println("  blocco ostruito x=" + tx + " y=" + ty); }
        }
        ok(blocked11 == 0, "livello 11: nessun ostacolo sotto mattoni e casse (" + blocked11 + ")");
        // ---- 3D: fiore a molla e rovi
        World3D w = new World3D(new MemorySave(), 10);
        w.enemies.clear();
        ok(w.springs.size() >= 5 && w.thorns.size() >= 10, "livello 10: " + w.springs.size() + " fiori a molla, " + w.thorns.size() + " rovi");
        float[] sp = w.springs.get(0);
        w.setInput(0, 0, false, false);
        placeAt(w, sp[0], sp[1] - 0.3f);
        float apex = 0;
        for (int i = 0; i < 120; i++) { w.update(1f / 60f); apex = Math.max(apex, w.py); }
        ok(apex > 3.5f, "3D: il fiore a molla lancia in alto: " + apex);
        World3D wt = new World3D(new MemorySave(), 10);
        wt.enemies.clear();
        float[] tt3 = wt.thorns.get(0);
        placeAt(wt, tt3[0], tt3[1]);
        int h0 = wt.hp;
        for (int i = 0; i < 10; i++) wt.update(1f / 60f);
        ok(wt.hp < h0, "3D: i rovi feriscono");
        bot3dCheck(10);
        bot3dCheck(12);
        // ---- livello 10, seconda parte: il nido cade e parte lo sciame
        World3D e = new World3D(new MemorySave(), 10);
        e.enemies.clear();
        ok(e.chase == 1 && Float.isNaN(e.fireZ) && !Float.isNaN(e.hiveZ), "livello 10: lo sciame non c'è finché non cade il nido");
        placeAt(e, 0f, e.hiveZ + 2f);
        e.setInput(0, 0, false, false);
        e.update(1f / 60f);
        ok(e.hiveAt >= 0 && e.fireZ < e.hiveZ, "superato il ciliegio il nido cade e lo sciame parte alle spalle (z " + e.fireZ + ")");
        int livesE = e.lives;
        for (int i = 0; i < 60 * 14; i++) e.update(1f / 60f);
        ok(e.lives < livesE, "fermi abbastanza a lungo lo sciame raggiunge e punge");
        ok(e.gemsList.size() >= 60 && e.gemsList.size() <= 140, "livello 10 intero: " + e.gemsList.size() + " gemme");
        ok(e.flagZ > e.hiveZ + 100, "il traguardo è dopo la fuga (z " + e.flagZ + ")");
        ok(Levels.ORDER.length == 40 && Levels.next(24) == 25 && Levels.next(33) == 34 && Levels.next(34) == 35 && Levels.next(37) == 38 && Levels.next(40) == 0 && Levels.is3D(35) && Levels.is3D(39) && Levels.ORDER[0] == 1 && Levels.next(2) == 13 && Levels.next(14) == 3 && Levels.next(6) == 17 && Levels.next(18) == 9 && Levels.next(10) == 21 && Levels.next(22) == 7 && Levels.next(8) == 19 && Levels.next(20) == 11 && Levels.next(12) == 23 && Levels.is3D(24) && !Levels.is3D(23) && !Levels.is3D(9) && !Levels.is3D(11) && Levels.is3D(12),
                "ordine dei livelli: estate, autunno, inverno, primavera, incendio, cielo, poi la terza serie e l'isola");
        System.out.println(fails == 0 ? "TUTTO OK" : fails + " FALLITI");
    }

    static void bot3dCheck(int levelNo) {
        World3D b = new World3D(new MemorySave(), levelNo);
        b.batteryOwned = true;
        b.enemies.clear();
        int lives0 = b.lives;
        float stuck = 0, lastZ = b.pz, jt = 0;
        boolean won = false;
        for (float time = 0; time < 300; time += 1f / 60f) {
            float tx = xTarget(levelNo, b.pz + 0.9f);
            float jx = Math.max(-1f, Math.min(1f, (tx - b.px) * 2f));
            boolean jump = false;
            if (b.state == World3D.PLAYING) {
                boolean pit = b.cell(xi(b.px), zi(b.pz + 0.9f)) == World3D.PIT && b.cell(xi(b.px), zi(b.pz)) != World3D.PIT;
                boolean thorn = false;
                for (float[] q : b.thorns) if (Math.abs(q[0] - b.px) < 0.9f && q[1] - b.pz > 0 && q[1] - b.pz < 1.0f) thorn = true;
                boolean pad = false;
                for (float[] q : b.springs) if (q[1] - b.pz > -0.6f && q[1] - b.pz < 2.5f && Math.abs(q[0] - b.px) < 2.6f) pad = true;
                float ahead = b.topAt(b.px, b.pz + 0.65f);
                boolean step = ahead > b.py + 0.3f && ahead <= b.py + 1.7f;      // gradino o terrazza rialzata da scalare
                if (b.onGround && (pit || thorn || step) && !pad && jt <= 0) jt = 0.5f;
                if (jt > 0) { jump = true; jt -= 1f / 60f; }
            }
            b.setInput(jx, 1f, jump, false);
            b.update(1f / 60f);
            if (b.state == World3D.WON) { won = true; break; }
            if (Math.abs(b.pz - lastZ) < 0.005f && b.state == World3D.PLAYING) { stuck += 1f / 60f; if (stuck > 6f) break; } else { stuck = 0; lastZ = b.pz; }
        }
        System.out.println("     livello " + levelNo + ": morti " + (lives0 - b.lives) + ", z finale " + b.pz + ", vinto " + won);
        ok(won, "il robot automatico completa il livello " + levelNo);
        ok(lives0 - b.lives <= 2, "al massimo 2 morti del robot sul livello " + levelNo + ": " + (lives0 - b.lives));
    }

    static int xi(float x) { return (int) Math.floor(x + World3D.HALF); }
    static int zi(float z) { return (int) Math.floor(z); }

    static void placeAt(World3D w, float x, float z) {
        try {
            for (String f : new String[] {"px", "pz", "py", "vy"}) { /* campi pubblici */ }
            w.px = x; w.pz = z; w.py = 0.01f; w.vy = 0; w.vx = 0; w.vz = 0;
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    /** Colonna di mondo verso cui puntare a ogni quota z (caselle libere dei campi di rovi, isolotti dei fiumi). */
    static float xTarget(int levelNo, float z) {
        if (levelNo == 12) {
            if (z >= 39 && z < 43) return 2f;
            if (z >= 43 && z < 47) return -2f;
            if (z >= 47 && z < 51) return 1f;
            if (z >= 51 && z < 55) return -1f;
            if (z >= 169 && z < 173) return 0f;
            if (z >= 173 && z < 177) return -2f;
            if (z >= 177 && z < 181) return 2f;
            return 0f;
        }
        if ((z >= 35 && z < 48) || (z >= 128 && z < 141)) return -1f;    // corsia libera dei campi di rovi
        return 0f;
    }
}
