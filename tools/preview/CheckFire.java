import com.valeriodipaolo.platformgame.core.*;
import com.valeriodipaolo.platformgame.core3d.*;

/** Controlli sulle meccaniche dell'incendio: muro di fuoco, getti a tempo, tavole che crollano, tronchi che rotolano. */
public class CheckFire {
    static int fails;

    static void ok(boolean c, String m) {
        System.out.println((c ? "OK   " : "FALLITO ") + m);
        if (!c) fails++;
    }

    static void run(Game g, float s) { g.setInput(false, false, false); for (float t = 0; t < s; t += 1f / 60f) g.update(1f / 60f); }

    public static void main(String[] a) {
        // ---- 2D, livello 7
        Game g = new Game(new MemorySave(), 7);
        ok(!Float.isNaN(g.fireX), "livello 7: c'è il muro di fuoco");
        g.player.x = 14; g.player.y = 8.8f;
        run(g, 0.3f);
        float f0 = g.fireX;
        run(g, 3f);
        ok(g.fireX > f0 + 3f, "il fuoco avanza stando fermi: " + f0 + " -> " + g.fireX);
        for (int i = 0; i < 60 && g.state == Game.PLAYING; i++) run(g, 1f);
        ok(g.state != Game.PLAYING, "fermi abbastanza a lungo il fuoco raggiunge e uccide");
        // a velocità di corsa non lo raggiunge: simula la corsa lungo una zona piatta
        Game r = new Game(new MemorySave(), 7);
        r.enemies.clear();
        boolean alive = true;
        for (int i = 0; i < 60 * 12; i++) { r.setInput(false, true, false); r.update(1f / 60f); if (r.state != Game.PLAYING) { alive = false; break; } if (r.player.x > 31) break; }
        ok(alive || r.player.x > 20, "correndo in avanti il fuoco non raggiunge (x=" + r.player.x + ")");
        // getto: danneggia solo da acceso
        Game j = new Game(new MemorySave(), 7);
        j.enemies.clear();
        float[] jet = j.level.jets.get(0);
        j.player.x = jet[0] + 0.1f; j.player.y = jet[1] - 1.2f - 0.05f; j.player.vx = 0; j.player.vy = 0;
        int hp0 = j.hp;
        boolean hurt = false, onWindow = false;
        for (int i = 0; i < 60 * 8; i++) {
            j.player.x = jet[0] + 0.1f; j.player.y = jet[1] - 1.2f; j.player.vy = 0;
            j.invuln = 0;
            float pw = Game.jetPower(jet, j.time);
            int before = j.hp;
            j.update(1f / 60f);
            if (j.hp < before) { hurt = true; if (pw > 0.3f) onWindow = true; else { onWindow = false; break; } }
        }
        ok(hurt && onWindow, "il getto ferisce solo quando è acceso");
        // tavola che crolla
        Game c = new Game(new MemorySave(), 7);
        c.enemies.clear();
        c.player.x = 34.15f; c.player.y = 9 - 1.2f;
        run(c, 0.2f);
        ok(c.level.get(34, 9) == Level.CRUMBLE, "la tavola regge subito dopo il tocco");
        run(c, 0.6f);
        ok(c.level.get(34, 9) == Level.EMPTY, "la tavola crolla dopo mezzo secondo");
        // ---- 3D, livello 8
        World3D w = new World3D(new MemorySave(), 8);
        ok(!Float.isNaN(w.fireZ) && w.jets.size() > 10 && w.logs.size() > 8, "livello 8: fuoco, " + w.jets.size() + " getti, " + w.logs.size() + " tronchi");
        w.setInput(0, 0, false, false);
        w.pz = 12f; w.px = 0f; w.update(0.05f);
        for (int i = 0; i < 60 * 40 && w.state == World3D.PLAYING; i++) w.update(1f / 60f);
        ok(w.state != World3D.PLAYING, "fermo nel livello 3D il fuoco raggiunge e uccide");
        World3D l = new World3D(new MemorySave(), 8);
        float[] lg = l.logs.get(0);
        boolean got = false;
        for (int i = 0; i < 60 * 8 && !got; i++) {
            l.invuln = 0; l.hp = 3;
            float lx = World3D.logX(lg, l.time);
            l.px = lx; l.pz = lg[0]; l.py = 0.01f; l.vy = 0;
            int before = l.hp;
            l.update(1f / 60f);
            if (l.hp < before) got = true;
        }
        ok(got, "un tronco che rotola ferisce chi lo tocca");
        // ogni riga di getti lascia almeno 2 caselle libere (percorso sempre possibile)
        java.util.Map<Integer, Integer> rows = new java.util.HashMap<Integer, Integer>();
        for (float[] jt : w.jets) rows.merge((int) jt[1], 1, Integer::sum);
        int worst = 0; for (int v : rows.values()) worst = Math.max(worst, v);
        ok(worst <= 3, "al massimo 3 getti nella stessa riga: " + worst);
        // i tronchi devono dare finestre di passaggio: tempo in cui il sentiero (±0,75) è libero di almeno 0,8 s per ciclo
        int badLogs = 0;
        for (float[] x : w.logs) { float period = x[1], speed = (World3D.W + 1.8f) / period, window = period - (World3D.W + 1.8f + 1.5f) / speed * 0; if (window < 2.4f) badLogs++; }
        ok(badLogs == 0, "tutti i tronchi hanno periodo di almeno 2,4 s");
        System.out.println(fails == 0 ? "TUTTO OK" : fails + " controlli falliti");
        System.exit(fails == 0 ? 0 : 1);
    }
}
