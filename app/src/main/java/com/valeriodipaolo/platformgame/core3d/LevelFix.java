package com.valeriodipaolo.platformgame.core3d;

import java.util.Iterator;

/**
 * Rifiniture comuni a tutti i livelli 3D, applicate subito dopo la costruzione: terrazze larghe quanto il corridoio (niente cadute ai lati),
 * niente gemme o nemici fuori dall'area percorribile e niente gemme sopra burroni troppo lunghi da attraversare.
 */
final class LevelFix {
    private LevelFix() {}

    /** Un burrone più lungo di così (in righe) non si salta: le gemme sopra di esso sono irraggiungibili. */
    static final int MAX_PIT = 4;
    /** Riepilogo dell'ultima applicazione (per gli strumenti da PC). */
    static String last = "";
    static boolean DEBUG = System.getProperty("fixdebug") != null;

    static void apply(World3D w) {
        int widened = widen(w);
        int gemsOut = 0, gemsPit = 0, gemsRaised = 0, foesOut = 0;
        float edge = World3D.HALF - 0.1f;
        for (Iterator<float[]> it = w.gemsList.iterator(); it.hasNext(); ) {
            float[] g = it.next();
            int xi = World3D.xi(g[0]), zi = World3D.zi(g[2]);
            if (DEBUG && pitRun(w, Math.max(0, Math.min(4, World3D.xi(g[0]))), Math.max(0, Math.min(w.length - 1, World3D.zi(g[2])))) > MAX_PIT) System.out.println("  - gemma x" + g[0] + " y" + g[1] + " z" + g[2]);
            if (g[0] < -edge || g[0] > edge || g[2] < 0 || g[2] >= w.length || xi < 0 || xi >= World3D.W) { it.remove(); gemsOut++; continue; }
            int h = w.cell(xi, zi);
            if (h == World3D.PIT) {
                if (pitRun(w, xi, zi) > MAX_PIT) { it.remove(); gemsPit++; }
            } else if (h < 99 && g[1] < h + 0.3f) {      // incastrata in un blocco (per esempio dopo l'allargamento di una terrazza): va sopra
                g[1] = h + 0.7f;
                gemsRaised++;
            } else if (h == 99) { it.remove(); gemsOut++; }
        }
        for (Iterator<Enemy3> it = w.enemies.iterator(); it.hasNext(); ) {
            Enemy3 e = it.next();
            int xi = World3D.xi(e.x), zi = World3D.zi(e.z);
            // chi fa la ronda di traverso può partire fuori dal corridoio (le stelle cadenti): conta il tratto che percorre, non il punto di partenza
            boolean out = e.alongZ ? (e.x < -edge || e.x > edge) : (Math.max(e.lo, e.hi) < -edge || Math.min(e.lo, e.hi) > edge);
            out |= e.z < 0 || e.z >= w.length;
            if (!out && !e.flies() && e.type != Enemy3.LAPILLO) {      // il lapillo nasce dalla lava: parte per forza nel vuoto
                xi = Math.max(0, Math.min(World3D.W - 1, xi));
                int h = w.cell(xi, zi);
                out = h == World3D.PIT || h == 99;
            }
            if (out) { if (DEBUG) System.out.println("  - nemico t" + e.type + " x" + e.x + " z" + e.z + " cella " + w.cell(xi, zi)); it.remove(); foesOut++; }
        }
        last = "terrazze allargate: " + widened + " righe; gemme fuori area: " + gemsOut + ", su burroni lunghi: " + gemsPit + ", alzate: " + gemsRaised + "; nemici fuori area: " + foesOut;
    }

    /** Lunghezza (in righe) della striscia di vuoto che contiene la casella, lungo il corridoio. */
    private static int pitRun(World3D w, int xi, int zi) {
        int a = zi, b = zi;
        while (a - 1 >= 0 && w.cell(xi, a - 1) == World3D.PIT) a--;
        while (b + 1 < w.length && w.cell(xi, b + 1) == World3D.PIT) b++;
        return b - a + 1;
    }

    /**
     * Terrazze e gradinate rialzate più strette del corridoio: si allargano fino ai muri laterali. Sono le sequenze di almeno tre righe senza vuoti, alte al massimo due blocchi, con
     * almeno tre colonne rialzate in ogni riga (i muretti con un varco, di una riga sola, restano com'erano).
     */
    private static int widen(World3D w) {
        int rows = 0, z = 0;
        while (z < w.length) {
            if (!platformRow(w, z)) { z++; continue; }
            int e = z;
            while (e < w.length && platformRow(w, e)) e++;
            int top = 0;
            for (int r = z; r < e; r++) for (int x = 0; x < World3D.W; x++) top = Math.max(top, w.height[r * World3D.W + x]);
            if (e - z >= 3 && top <= 2) {      // solo terrazze su cui si sale con un salto; i torrioni alti (3 o più) restano ostacoli da aggirare dai lati
                for (int r = z; r < e; r++) {
                    int m = 0;
                    for (int x = 0; x < World3D.W; x++) m = Math.max(m, w.height[r * World3D.W + x]);
                    boolean changed = false;
                    for (int x = 0; x < World3D.W; x++) {
                        int i = r * World3D.W + x;
                        if (w.height[i] == 0 && !w.crate[i]) { w.height[i] = m; changed = true; }
                    }
                    if (changed) rows++;
                }
            }
            z = e;
        }
        return rows;
    }

    private static boolean platformRow(World3D w, int z) {
        int raised = 0;
        for (int x = 0; x < World3D.W; x++) {
            int h = w.height[z * World3D.W + x];
            if (h == World3D.PIT) return false;
            if (h >= 1 && h < 99) raised++;
        }
        return raised >= 3 && z != w.flagZ;
    }
}
