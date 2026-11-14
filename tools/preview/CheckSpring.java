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
        int deaths = 0, lives0 = g.lives;
        float jt = 0, stuck = 0, lastX = g.player.x;
        for (float t = 0; t < maxSec; t += 1f / 60f) {
            Body p = g.player;
            boolean jump = false;
            if (g.state == Game.PLAYING) {
                int row = (int) Math.floor(p.y + p.h + 0.05f);
                boolean gap = false;
                for (float d = 0.9f; d <= 1.1f; d += 0.2f) if (!g.level.solid((int) Math.floor(p.x + p.w + d), row)) gap = true;
                boolean thorn = false;
                for (float[] th : g.level.thorns) if (th[0] - p.x > -0.1f && th[0] - p.x < 1.8f) thorn = true;
                boolean pad = false;
                for (int d = 0; d <= 2; d++) if (g.level.get((int) Math.floor(p.x + p.w / 2) + d, 10) == Level.SPRING) pad = true;
                if (p.onGround && ((gap && !pad && row >= 10) || thorn || p.hitWall) && jt <= 0) jt = 0.5f;
                if (jt > 0) { jump = true; jt -= 1f / 60f; }
                else if (!p.onGround && p.vy > 0 && !g.level.solid((int) Math.floor(p.x + p.w / 2), row + 1) && !g.level.solid((int) Math.floor(p.x + p.w / 2), row + 2)) jump = false;
            }
            g.setInput(false, true, jump);
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
        // ---- percorribilità 3D con robot a punti di passaggio
        World3D b = new World3D(new MemorySave(), 10);
        b.enemies.clear();
        int lives0 = b.lives;
        float stuck = 0, lastZ = b.pz, jt = 0;
        boolean won = false;
        for (float time = 0; time < 300; time += 1f / 60f) {
            float tx = xTarget(b.pz + 0.9f);
            float jx = Math.max(-1f, Math.min(1f, (tx - b.px) * 2f));
            boolean jump = false;
            if (b.state == World3D.PLAYING) {
                boolean pit = b.cell(xi(b.px), zi(b.pz + 0.9f)) == World3D.PIT && b.cell(xi(b.px), zi(b.pz)) != World3D.PIT;
                boolean thorn = false;
                for (float[] q : b.thorns) if (Math.abs(q[0] - b.px) < 0.9f && q[1] - b.pz > 0 && q[1] - b.pz < 1.0f) thorn = true;
                boolean pad = false;
                for (float[] q : b.springs) if (q[1] - b.pz > -0.6f && q[1] - b.pz < 2.5f && Math.abs(q[0] - b.px) < 2.6f) pad = true;
                if (b.onGround && (pit || thorn) && !pad && jt <= 0) jt = 0.5f;
                if (jt > 0) { jump = true; jt -= 1f / 60f; }
            }
            b.setInput(jx, 1f, jump, false);
            b.update(1f / 60f);
            if (b.state == World3D.WON) { won = true; break; }
            if (Math.abs(b.pz - lastZ) < 0.005f && b.state == World3D.PLAYING) { stuck += 1f / 60f; if (stuck > 6f) break; } else { stuck = 0; lastZ = b.pz; }
        }
        System.out.println("     livello 10: morti " + (lives0 - b.lives) + ", z finale " + b.pz + ", vinto " + won);
        ok(won, "il robot automatico completa il livello 10");
        ok(lives0 - b.lives <= 2, "al massimo 2 morti del robot sul livello 10: " + (lives0 - b.lives));
        System.out.println(fails == 0 ? "TUTTO OK" : fails + " FALLITI");
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
    static float xTarget(float z) {
        if ((z >= 8 && z < 34) || (z >= 68 && z < 94)) return -1.8f;     // si aggira il balcone, senza salire
        if ((z >= 35 && z < 48) || (z >= 128 && z < 141)) return -1f;    // corsia libera dei campi di rovi
        return 0f;
    }
}
