import com.valeriodipaolo.platformgame.core.*;
import com.valeriodipaolo.platformgame.core3d.*;

/** Robot 3D per i livelli 14..24: sceglie la corsia più libera davanti a sé, salta i burroni corti, usa i fiori a molla per quelli lunghi. Non conta i colpi dei nemici (ha punti vita infiniti), ma le cadute. */
public class Check3B {
    static int fails;
    static void ok(boolean c, String m) { System.out.println((c ? "OK   " : "FALLITO ") + m); if (!c) fails++; }
    static int xi(float x) { return (int) Math.floor(x + World3D.HALF); }
    static int zi(float z) { return (int) Math.floor(z); }

    static int base; static boolean rel;
    static boolean blocked(World3D w, int c, int z) {
        if (z < 0 || z >= w.length) return false;
        int h = w.cell(c, z);
        if (h == 99) return true;
        if (h == World3D.PIT) return false;
        if (h - base >= 1) return true;
        if (rel && w.quickAt(c - World3D.HALF + 0.5f, z + 0.5f)) return true;      // le sabbie mobili si evitano, se si può
        if (w.crate[z * World3D.W + c]) return true;
        for (float[] t : w.thorns) if (xi(t[0]) == c && zi(t[1]) == z) return true;
        return false;
    }

    static int pitRun(World3D w, int c, int z) { int n = 0; while (z + n < w.length && w.cell(c, z + n) == World3D.PIT) n++; return n; }

    /** distanza libera (in caselle) davanti alla colonna c. */
    static float freeRun(World3D w, int c, float pz) {
        int z0 = zi(pz) + 1;
        for (int z = z0; z < z0 + 14; z++) {
            if (blocked(w, c, z)) return z - z0;
            if (w.cell(c, z) == World3D.PIT && (w.cell(c, z - 1) != World3D.PIT || z == z0)) {
                int zs = z; while (zs > 0 && w.cell(c, zs - 1) == World3D.PIT) zs--;
                int run = (z - zs) + pitRun(w, c, z);
                boolean spring = false;
                for (float[] q : w.springs) if (xi(q[0]) == c && zi(q[1]) >= zs - 3 && zi(q[1]) < zs) spring = true;
                if (run > 5 && !spring) return z - z0;
            }
        }
        return 14;
    }

    static float mazeTime;

    /** Nel labirinto: centro della prossima casella sulla strada più breve verso l'uscita (ricerca in ampiezza), null se fuori. */
    static float[] mazeStep(World3D b) {
        float[] m = null;
        for (float[] q : b.mazes) if (b.pz >= q[0] - 0.5f && b.pz < q[1]) m = q;
        if (m == null) return null;
        int z0 = (int) m[0] - 1, z1 = (int) m[1], W = World3D.W, sx = xi(b.px), sz = zi(b.pz);
        int n = (z1 - z0 + 1) * W;
        int[] prev = new int[n];
        java.util.Arrays.fill(prev, -2);
        java.util.ArrayDeque<Integer> q = new java.util.ArrayDeque<Integer>();
        int s = (sz - z0) * W + sx;
        if (s < 0 || s >= n) return null;
        prev[s] = -1; q.add(s);
        int goal = -1;
        while (!q.isEmpty()) {
            int u = q.poll(), ux = u % W, uz = u / W + z0;
            if (uz >= z1) { goal = u; break; }
            int[][] d = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] e : d) {
                int vx = ux + e[0], vz = uz + e[1];
                if (vx < 0 || vx >= W || vz < z0 || vz > z1) continue;
                int h = b.cell(vx, vz);
                if (h == World3D.PIT || h >= 1) continue;
                int v = (vz - z0) * W + vx;
                if (prev[v] != -2) continue;
                prev[v] = u; q.add(v);
            }
        }
        if (goal < 0) return null;
        int step = goal;
        while (prev[step] != s && prev[step] >= 0) step = prev[step];
        if (step == s) return new float[] {sx - World3D.HALF + 0.5f, z1 + 1f};
        return new float[] {step % W - World3D.HALF + 0.5f, step / W + z0 + 0.5f};
    }

    static void run(int id) {
        rel = id > 24;            // la terza serie ha piani rialzati: gli ostacoli si misurano rispetto al piano su cui si corre
        World3D b = new World3D(new MemorySave(), id);
        int lives0 = b.lives;
        float stuck = 0, lastZ = b.pz, lastX = b.px, jt = 0, time = 0;
        mazeTime = 0;
        int cur = 2;
        boolean won = false;
        for (time = 0; time < 700; time += 1f / 60f) {
            b.hp = 99; b.invuln = 5f;
            if (b.state == World3D.PLAYING) {
                int bh = b.cell(xi(b.px), zi(b.pz)); base = bh == World3D.PIT || !rel ? 0 : bh;
                int best = cur; float bs = -1e9f;
                for (int c = 0; c < World3D.W; c++) {
                    float fr = freeRun(b, c, b.pz);
                    float s = fr - 0.4f * Math.abs(c - cur) + (c == cur ? 0.6f : 0);
                    // un fiore a molla davanti attira (molto, se davanti c'è un muro alto da bordo a bordo)
                    for (float[] q : b.springs) if (xi(q[0]) == c && q[1] - b.pz > 0 && q[1] - b.pz < 8) s += fr < 10 ? 2f : 0.3f;
                    // una cassa a molla davanti attira: è l'unico modo di passare un muro alto o un fiume largo
                    if (b.crateKind != null) for (int z = zi(b.pz) + 1; z <= zi(b.pz) + 6 && z < b.length; z++) if (b.crate[z * World3D.W + c] && b.crateKind[z * World3D.W + c] == World3D.C_BOUNCE) s += 6f;
                    if (s > bs) { bs = s; best = c; }
                }
                cur = best;
            }
            float tx = cur - World3D.HALF + 0.5f;
            float jx = Math.max(-1f, Math.min(1f, (tx - b.px) * 2.5f));
            float[] mw = b.state == World3D.PLAYING ? mazeStep(b) : null;     // nel labirinto: la strada più breve verso l'uscita
            if (mw != null) {
                jx = Math.max(-1f, Math.min(1f, (mw[0] - b.px) * 3f));
                float jy = Math.max(-1f, Math.min(1f, (mw[1] - b.pz) * 3f));
                b.setInput(jx, jy, false, false);
                int lv0m = b.lives;
                b.update(1f / 60f);
                mazeTime += 1f / 60f;
                if (b.state == World3D.WON) { won = true; break; }
                if (Math.abs(b.pz - lastZ) < 0.005f && Math.abs(b.px - lastX) < 0.005f) { stuck += 1f / 60f; if (stuck > 6f) { System.out.printf("     bloccato nel labirinto a x %.2f z %.2f%n", b.px, b.pz); break; } } else { stuck = 0; lastZ = b.pz; lastX = b.px; }
                continue;
            }
            boolean jump = false;
            if (b.state == World3D.PLAYING) {
                boolean pit = b.cell(xi(b.px), zi(b.pz + 0.45f)) == World3D.PIT && b.cell(xi(b.px), zi(b.pz)) != World3D.PIT;
                boolean thorn = false;
                for (float[] q : b.thorns) if (Math.abs(q[0] - b.px) < 0.9f && q[1] - b.pz > 0 && q[1] - b.pz < 1.0f) thorn = true;
                boolean pad = false;
                for (float[] q : b.springs) if (q[1] - b.pz > -0.6f && q[1] - b.pz < 2.5f && Math.abs(q[0] - b.px) < 2.6f) pad = true;
                float ahead = b.topAt(b.px, b.pz + 0.65f);
                boolean step = ahead > b.py + 0.3f && ahead <= b.py + 1.7f;
                if (b.onGround && (pit || thorn || step) && !pad && jt <= 0) { int pl = pit ? pitRun(b, xi(b.px), zi(b.pz + 0.45f)) : 9; jt = pit && pl <= 1 ? 0.2f : pit && pl <= 2 ? (rel ? 0.12f : 0.33f) : 0.6f;
                    if (rel && step && !pit && b.crateKind != null && b.crate[Math.min(b.length - 1, zi(b.pz + 0.95f)) * World3D.W + xi(b.px)]) jt = 0.15f; }
                if (jt > 0) { jump = true; jt -= 1f / 60f; }
            }
            float fwd = 1f;                                  // davanti a una cassa a molla rallenta, per atterrarci sopra
            if (rel && b.crateKind != null) for (int z = zi(b.pz); z <= zi(b.pz) + 2 && z < b.length; z++) { int k = z * World3D.W + xi(b.px); if (k >= 0 && k < b.crate.length && b.crate[k] && b.crateKind[k] == World3D.C_BOUNCE && b.time - b.crateT[k] > 0.5f) fwd = 0.3f; }
            b.setInput(jx, fwd, jump, false);
            if (System.getProperty("tr") != null && b.pz > Float.parseFloat(System.getProperty("tr")) && b.pz < Float.parseFloat(System.getProperty("tr")) + 20 && ((int) (time * 60)) % 6 == 0) System.out.printf("z %.2f x %.2f cur %d y %.2f og %b jt %.2f%n", b.pz, b.px, cur, b.py, b.onGround, jt);
            int lv0 = b.lives;
            b.update(1f / 60f);
            if (b.lives < lv0 && System.getProperty("dbg") != null) { System.out.printf("MORTE z %.2f x %.2f py %.2f%n", b.pz, b.px, b.py); for (int z = (int) b.pz + 10; z >= (int) b.pz - 4; z--) { StringBuilder sb = new StringBuilder(); for (int c = 0; c < 5; c++) { int h = b.cell(c, z); sb.append(h == World3D.PIT ? '~' : b.crate[z * 5 + c] ? 'C' : h > 0 ? (char) ('0' + h) : '.'); } System.out.println("  " + z + " " + sb + (z == (int) b.pz ? " <" : "")); } if (lv0 - b.lives > 0 && lives0 - b.lives >= 3) { System.out.println("... fermo"); break; } }
            if (b.state == World3D.WON) { won = true; break; }
            if (Math.abs(b.pz - lastZ) < 0.005f && b.state == World3D.PLAYING) { stuck += 1f / 60f; if (stuck > 6f) break; } else { stuck = 0; lastZ = b.pz; }
        }
        if (mazeTime > 0) System.out.printf("     tempo nei labirinti: %.1f s%n", mazeTime);
        System.out.println("     livello " + id + ": lunghezza " + b.length + ", morti " + (lives0 - b.lives) + ", z finale " + b.pz + ", vinto " + won + ", t " + time + ", nemici " + b.enemies.size());
        ok(won, "il robot automatico completa il livello " + id);
        ok(lives0 - b.lives <= 2, "al massimo 2 cadute sul livello " + id + ": " + (lives0 - b.lives));
    }

    public static void main(String[] a) {
        for (int id : new int[] {14, 16, 18, 20, 22, 24, 26, 28, 30, 32, 34, 35, 36, 37, 38, 39, 40}) {
            if (a.length > 0 && !a[0].equals("" + id)) continue;
            run(id);
        }
        System.out.println(fails == 0 ? "TUTTO OK" : fails + " FALLITI");
    }
}
