package com.valeriodipaolo.platformgame.core3d;

import static com.valeriodipaolo.platformgame.core3d.Levels3B.*;

/**
 * Terza serie di livelli 3D (26, 28, 30, 32, 34). Come per i 2D, ogni livello ruota attorno a un'idea sola, in quattro tempi:
 * la si impara al sicuro, la si sviluppa, arriva il colpo di scena, poi l'esame finale. La chiave è sempre su un percorso facoltativo
 * ma ben visibile, e le gemme indicano la corsia giusta.
 */
public final class Levels3C {
    private Levels3C() {}

    private static final int W = World3D.W;

    /** Aspetto (tema 3D) di ciascun livello. */
    public static int theme(int id) {
        switch (id) {
            case 28: return 6;
            case 30: return 10;
            case 32: return 8;
            case 34: return 12;
            default: return 4;
        }
    }

    public static void build(World3D w, int id) {
        int len = id == 26 ? 236 : id == 28 ? 244 : id == 30 ? 240 : id == 32 ? 252 : 268;
        w.length = len;
        w.height = new int[len * W];
        w.crate = new boolean[len * W];
        w.gemsList.clear();
        w.enemies.clear();
        w.jets.clear();
        w.logs.clear();
        w.springs.clear();
        w.thorns.clear();
        w.chase = 0;
        w.fireStart = Float.NaN;
        w.fireZones = new float[0][];
        w.hiveZ = Float.NaN;
        switch (id) {
            case 28: archipelago(w); break;
            case 30: thorns(w); break;
            case 32: blaze(w); break;
            case 34: summit(w); break;
            default: maze(w); break;
        }
    }

    // ------------------------------------------------------------------ 26: il labirinto del querceto
    // Idea: tre veri labirinti, con muri di pietra alti cinque blocchi (neanche il doppio salto li scavalca). In ognuno c'è una sola
    // strada che porta all'uscita: tutte le altre finiscono in un vicolo cieco. Le gemme sono sparse in tutti i corridoi allo stesso
    // modo, così non tradiscono la strada giusta; i vicoli ciechi più lunghi premiano chi esplora con una cassa, e nel più profondo
    // dell'ultimo labirinto c'è il gettone. Tra un labirinto e l'altro, all'aperto, una radura e i burroni da saltare.
    static final int MAZE_H = 5;

    private static void maze(World3D w) {
        gemLine(w, 0, 0, 5); crate(w, 1, 9); crate(w, 3, 9);
        // 1. il primo labirinto, corto: si impara che le strade si biforcano e che si può tornare indietro
        gemLine(w, 0, 11, 15);
        carveMaze(w, 18, 8, 1, MAZE_A, false);
        // la radura della chiave: una terrazza nel mezzo, con il gufo di guardia
        for (int x = 1; x <= 3; x++) { set(w, x, 40, 1); for (int z = 41; z <= 47; z++) set(w, x, z, 2); set(w, x, 48, 1); }
        owl(w, 2, 41, 48, 4.1f); gemLine3(w, 0, 2.7f, 42, 43); gemLine3(w, 0, 2.7f, 45, 46);
        crate(w, 0, 53); crate(w, 4, 53); hog(w, 0, 56, false, -2f, 2f);
        // 2. il secondo labirinto, più lungo, con qualche nemico nei corridoi dritti
        carveMaze(w, 64, 16, 0, MAZE_B, true);
        // 3. all'aperto: dopo il varco c'è il vuoto e il varco successivo è dall'altra parte, si sterza in volo
        wallH(w, 100, 2, 3); pit(w, 101, 103, 0, 4); wallH(w, 106, 0, 3); pit(w, 107, 109, 0, 4); wallH(w, 112, 4, 3); pit(w, 113, 115, 0, 4);
        arc(w, -1, 101, 103); arc(w, 1, 107, 109); gem(w, 0, 0.7f, 104.5f); gem(w, 1, 0.7f, 110.5f); gem(w, 2, 0.7f, 116.5f);
        snail(w, 2, 120, 117, 124);
        // gradini di pietra: alti uno e due, si sale e si scende a zig-zag
        stones(w, 128, new int[] {1, 2, 1, 2, 1}); stones(w, 129, new int[] {1, 2, 1, 2, 1}); chest(w, 2, 132, 3.6f);
        stones(w, 136, new int[] {2, 1, 1, 1, 2}); stones(w, 137, new int[] {2, 2, 1, 2, 2}); owl(w, 2, 134, 141, 3.6f);
        // 4. il labirinto grande: il più lungo, con più nemici; nel vicolo cieco più profondo il gettone
        carveMaze(w, 146, 24, 2, MAZE_C, true);
        // l'uscita: un burrone largo e la discesa verso il portone
        for (int x = 0; x < W; x += 2) crate(w, x, 199);
        pit(w, 204, 208, 0, 4); arc(w, 0, 204, 208); hog(w, 0, 213, false, -2f, 2f);
        pit(w, 217, 219, 0, 4); arc(w, 0, 217, 219); gemLine(w, 0, 222, 228);
        w.flagZ = 232;
    }

    /** I tre labirinti: {seme, colonna d'uscita}, scelti perché la strada giusta serpeggi e i vicoli ciechi siano lunghi e tanti. */
    static final long[] MAZE_A = {1160, 2}, MAZE_B = {884, 1}, MAZE_C = {5, 2};

    /**
     * Albero dei passaggi di un labirinto perfetto di tre colonne per rows righe (cella i = riga * 3 + colonna), costruito "a
     * crescita" partendo dalla cella d'ingresso: un po' prosegue l'ultimo corridoio aperto (corridoi lunghi), un po' riparte da un
     * punto a caso (tante biforcazioni). parent[i] è la cella da cui si è arrivati in i, -1 per l'ingresso.
     */
    static int[] mazeTree(int rows, int cin, long seed) {
        int n = rows * 3;
        int[] parent = new int[n];
        java.util.Arrays.fill(parent, -2);
        java.util.Random rnd = new java.util.Random(seed * 7919L + rows);
        java.util.ArrayList<Integer> active = new java.util.ArrayList<Integer>();
        parent[cin] = -1;
        active.add(cin);
        int[] nb = new int[4];
        while (!active.isEmpty()) {
            int ai = rnd.nextFloat() < 0.65f ? active.size() - 1 : rnd.nextInt(active.size());
            int c = active.get(ai), r = c / 3, col = c % 3, k = 0;
            if (r > 0 && parent[c - 3] == -2) nb[k++] = c - 3;
            if (r < rows - 1 && parent[c + 3] == -2) nb[k++] = c + 3;
            if (col > 0 && parent[c - 1] == -2) nb[k++] = c - 1;
            if (col < 2 && parent[c + 1] == -2) nb[k++] = c + 1;
            if (k == 0) { active.remove(ai); continue; }
            int v = nb[rnd.nextInt(k)];
            parent[v] = c;
            active.add(v);
        }
        return parent;
    }

    /**
     * Scava un labirinto a partire dalla riga z0: celle libere alle colonne 0, 2, 4 e alle righe z0, z0+2, ...; muri alti MAZE_H
     * tutto intorno, con l'ingresso nella fila z0-1 (colonna cin) e l'uscita nella fila z0+2*rows-1. Gemme sparse in modo uniforme
     * in tutti i corridoi; casse in fondo ai vicoli ciechi più lunghi; nemici nei corridoi dritti; il gettone, se richiesto, nel
     * vicolo cieco più profondo (solo nell'ultimo labirinto).
     */
    private static void carveMaze(World3D w, int z0, int rows, int cin, long[] spec, boolean foes) {
        int cout = (int) spec[1];
        int[] par = mazeTree(rows, cin, spec[0]);
        int n = rows * 3, zEnd = z0 + 2 * rows - 1;
        for (int z = z0 - 1; z <= zEnd; z++) for (int x = 0; x < W; x++) set(w, x, z, MAZE_H);
        for (int i = 0; i < n; i++) set(w, 2 * (i % 3), z0 + 2 * (i / 3), 0);
        for (int i = 0; i < n; i++) if (par[i] >= 0) {
            int a = i, b = par[i];
            set(w, (a % 3) + (b % 3), z0 + (a / 3) + (b / 3), 0);         // la casella fra le due celle
        }
        set(w, 2 * cin, z0 - 1, 0);
        set(w, 2 * cout, zEnd, 0);
        w.mazes.add(new float[] {z0 - 1, zEnd + 1});
        // vie: strada giusta e distanza di ogni cella dalla strada
        boolean[] onPath = new boolean[n];
        for (int i = (rows - 1) * 3 + cout; i >= 0; i = par[i]) onPath[i] = true;
        int[] deg = new int[n];
        for (int i = 0; i < n; i++) if (par[i] >= 0) { deg[i]++; deg[par[i]]++; }
        int[] depth = new int[n];
        for (int pass = 0; pass < n; pass++) for (int i = 0; i < n; i++) if (!onPath[i] && par[i] >= 0) depth[i] = depth[par[i]] + 1;
        // gemme: una cella ogni tre lungo l'ordine di righe, sia sulla strada giusta sia nei vicoli: non dicono dove andare
        for (int i = 0; i < n; i++) if ((i / 3 + (i % 3) * 2) % 3 == 1) gem(w, 2 * (i % 3) - 2f, 0.7f, z0 + 2 * (i / 3) + 0.5f);
        // in fondo ai vicoli ciechi: il più profondo dell'ultimo labirinto tiene il gettone, i tre più lunghi dopo una cassa
        java.util.ArrayList<Integer> tips = new java.util.ArrayList<Integer>();
        for (int i = 0; i < n; i++) if (!onPath[i] && deg[i] == 1) tips.add(i);
        tips.sort((p, q) -> depth[q] - depth[p]);
        int t0 = 0;
        if (spec == MAZE_C && !tips.isEmpty()) {
            int i = tips.get(0);
            float x = 2 * (i % 3) - 2f, z = z0 + 2 * (i / 3) + 0.5f;
            w.gemsList.removeIf(g -> Math.abs(g[0] - x) < 0.3f && Math.abs(g[2] - z) < 0.3f);
            w.coin = new float[] {x, 0.6f, z};
            t0 = 1;
        }
        for (int k = t0; k < Math.min(tips.size(), t0 + (rows >= 16 ? 3 : 1)); k++) {
            int i = tips.get(k);
            if (depth[i] < 2) break;
            final float x = 2 * (i % 3) - 2f, z = z0 + 2 * (i / 3) + 0.5f;
            w.gemsList.removeIf(g -> Math.abs(g[0] - x) < 0.3f && Math.abs(g[2] - z) < 0.3f);
            crate(w, 2 * (i % 3), z0 + 2 * (i / 3));
        }
        if (!foes) return;
        // nemici: nei corridoi dritti lungo z di almeno tre celle, uno ogni due corridoi, mai vicino all'ingresso
        int placed = 0, every = 0;
        for (int col = 0; col < 3; col++) {
            int r = 1;
            while (r < rows) {
                int r1 = r;
                while (r1 + 1 < rows && linked(par, (r1 + 1) * 3 + col, r1 * 3 + col)) r1++;
                if (r1 - r >= 2 && r >= 2) {
                    if (every++ % 2 == 0) {
                        int za = z0 + 2 * r, zb = z0 + 2 * r1;
                        if (placed % 2 == 0) hog(w, cx(2 * col), (za + zb) / 2f + 0.5f, true, za + 0.5f, zb + 0.5f);
                        else snail(w, 2 * col, (za + zb) / 2, za, zb);
                        placed++;
                    }
                }
                r = r1 + 1;
            }
        }
    }

    private static boolean linked(int[] par, int a, int b) { return par[a] == b || par[b] == a; }

    // ------------------------------------------------------------------ 28: l'arcipelago gelato
    // Idea: il lago è fatto di isole e i fiori a molla di neve sono i ponti. Prima un salto con molla su un'isola grande; poi isole sempre
    // più piccole con i pinguini; poi la molla che lancia su un isolotto stretto da cui si deve saltare ancora; infine i pupazzi che
    // tirano da un'isola all'altra.
    private static void archipelago(World3D w) {
        gemLine(w, 0, 0, 5); crate(w, 1, 9); crate(w, 3, 9);
        // 1. la prima molla: un'isola grande e larga, si atterra comodi
        padRow(w, 16, 0, 4); pit(w, 17, 26, 0, 4); flight(w, 16); gemLine(w, 0, 28, 32);
        peng(w, 36, -2f, 2f); gem(w, 2, 0.7f, 38);
        // 2. isole che si restringono: quattro, poi tre, poi due corsie; su ognuna un pinguino
        pit(w, 44, 47, 0, 4); arc(w, 0, 44, 47); island(w, 48, 53, 0, 3); pit(w, 48, 53, 4, 4); peng2(w, 1, 48, 53);
        pit(w, 54, 57, 0, 4); arc(w, 0, 54, 57); pit(w, 58, 64, 0, 0); pit(w, 58, 64, 4, 4); peng2(w, 2, 58, 64);
        pit(w, 65, 68, 0, 4); arc(w, 0, 65, 68); pit(w, 69, 75, 0, 1); pit(w, 69, 75, 4, 4); gemLine(w, 0, 69, 75); snowman(w, 3, 78);
        pit(w, 76, 76, 0, 4);
        // l'isola della chiave: la si raggiunge con la molla, in cima a una terrazza alta
        crate(w, 1, 84); crate(w, 3, 84); padRow(w, 88, 1, 3);
        for (int z = 94; z <= 101; z++) for (int x = 1; x <= 3; x++) set(w, x, z, 3);
        for (int x = 1; x <= 3; x++) { set(w, x, 102, 2); set(w, x, 103, 1); }
        gemLine3(w, 0, 3.7f, 95, 97); peng(w, 108, -2f, 2f);
        // 3. il colpo di scena: la molla non porta sulla riva ma su un isolotto, e dall'isolotto si salta di nuovo
        padRow(w, 114, 0, 4); pit(w, 115, 133, 0, 4); island(w, 125, 128, 0, 4); flight(w, 114); arc(w, 0, 129, 133);
        snowman(w, 2, 138); gemLine(w, -2, 135, 140);
        padRow(w, 146, 1, 3); pit(w, 147, 165, 0, 4); island(w, 157, 160, 1, 3); flight(w, 146); arc(w, 0, 161, 165); peng(w, 170, -2f, 2f);
        // 4. l'esame: arcipelago sparso, un pupazzo su ogni isola e i pinguini sulle lingue di ghiaccio
        pit(w, 176, 212, 0, 4); island(w, 176, 180, 1, 3); island(w, 184, 188, 0, 2); island(w, 192, 196, 2, 4); island(w, 200, 204, 1, 3); island(w, 208, 212, 0, 2);
        snowman(w, 1, 186); snowman(w, 3, 194); peng2(w, 2, 200, 204); snowman(w, 0, 210);
        gem(w, 0, 0.7f, 178); gem(w, -1, 0.7f, 186); gem(w, 1, 0.7f, 194); gem(w, 0, 0.7f, 202); gem(w, -1, 0.7f, 210);
        padRow(w, 218, 0, 4); pit(w, 219, 228, 0, 4); flight(w, 218); snowman(w, 2, 234); gemLine(w, 0, 230, 238);
        w.flagZ = 240;
    }

    // ------------------------------------------------------------------ 30: il sentiero dei rovi
    // Idea: i rovi disegnano un sentiero che cambia lato. Prima un sentiero dritto, poi a zig-zag, poi le rane che sbarrano il sentiero
    // e costringono a schiacciarle o aspettarle, poi rovi e acqua insieme; la chiave in cima a una terrazza fiorita.
    private static void thorns(World3D w) {
        gemLine(w, 0, 0, 5); crate(w, 1, 9); crate(w, 3, 9);
        // 1. il sentiero dritto: rovi ai lati, corsia di mezzo libera
        for (int z = 14; z <= 26; z += 2) { thorn(w, 0, z); thorn(w, 1, z); thorn(w, 3, z); thorn(w, 4, z); }
        gemLine(w, 0, 14, 26); plant(w, 2, 30);
        // 2. il sentiero che svolta: tre curve, le gemme le segnano
        path(w, 36, 47, 1); path(w, 48, 59, 3); path(w, 60, 71, 0); gemLine(w, -1, 36, 47); gemLine(w, 1, 48, 59); gemLine(w, -2, 60, 71);
        frog(w, 2, 75); pit(w, 80, 83, 0, 4); arc(w, 0, 80, 83);
        // la terrazza della chiave: il fiore a molla sale sui ciliegi, sotto c'è il roseto
        padRow(w, 90, 1, 3);
        for (int x = 1; x <= 3; x++) { for (int z = 96; z <= 103; z++) set(w, x, z, 3); set(w, x, 104, 2); set(w, x, 105, 1); }
        plant(w, 2, 98, 3f); gemLine3(w, 0, 3.7f, 96, 97); thorn(w, 0, 99); thorn(w, 4, 101);
        // 3. il colpo di scena: le rane abitano proprio il sentiero; si salta loro in testa o si aspetta che balzino via
        path(w, 112, 125, 2); frog(w, 2, 116); frog(w, 2, 122); gemLine(w, 0, 112, 125);
        path(w, 126, 137, 4); plant(w, 4, 131); gemLine(w, 2, 126, 137);
        // rovi e acqua: il sentiero passa su un ponte stretto con una pianta in mezzo
        pit(w, 144, 158, 0, 1); pit(w, 144, 158, 3, 4); plant(w, 2, 151); gemLine(w, 0, 144, 149); gemLine(w, 0, 153, 158); thorn(w, 0, 160); thorn(w, 4, 160);
        // 4. l'esame: sentiero a zig-zag stretto con rane e piante, poi un fiume con la molla
        path(w, 166, 175, 0); path(w, 176, 185, 2); path(w, 186, 195, 4); path(w, 196, 205, 1);
        frog(w, 0, 170); plant(w, 2, 181); frog(w, 4, 190); plant(w, 1, 200);
        gemLine(w, -2, 166, 175); gemLine(w, 0, 176, 185); gemLine(w, 2, 186, 195); gemLine(w, -1, 196, 205);
        padRow(w, 212, 0, 4); pit(w, 213, 222, 0, 4); flight(w, 212); frog(w, 1, 226); frog(w, 3, 229); gemLine(w, 0, 224, 234);
        w.flagZ = 236;
    }

    // ------------------------------------------------------------------ 32: la fuga dal rogo
    // Idea: il fuoco insegue senza sosta, tranne in una radura dove si ferma ed è lì la chiave. Prima il fuoco lento su un percorso
    // semplice, poi più veloce con getti e tronchi, poi il colpo di scena: la strada si stringe sopra la lava proprio quando corre
    // di più, infine lo scatto finale tra lapilli e salamandre.
    private static void blaze(World3D w) {
        w.fireStart = -16f;
        w.fireZones = new float[][] {{8, 1.6f, 22}, {60, 2.1f, 20}, {104, 0f}, {124, 2.5f, 18}, {180, 3.0f, 15}};
        gemLine(w, 0, 0, 6); crate(w, 1, 10); crate(w, 3, 10);
        // 1. il fuoco lento: un burrone, una salamandra, due getti a ritmo regolare
        pit(w, 18, 21, 0, 4); arc(w, 0, 18, 21); sala(w, 1, 28, 24, 34); gemLine(w, 1, 24, 32);
        jet(w, 2, 38, 2.8f, 0f); jet(w, 0, 42, 2.8f, 1.4f); jet(w, 4, 42, 2.8f, 1.4f); gem(w, 0, 0.7f, 40); gem(w, 0, 0.7f, 44);
        pit(w, 48, 52, 0, 4); lap(w, 2, 50, 2.4f, 3.2f); arc(w, -1, 48, 52);
        // 2. più veloce: i tronchi rotolano di traverso, tra un tronco e l'altro i getti
        log(w, 62, 3.6f, 0f); jet(w, 2, 65, 2.6f, 0.6f); log(w, 68, 3.6f, 1.8f); jet(w, 1, 71, 2.6f, 1.9f); log(w, 74, 3.4f, 0.9f);
        gem(w, 0, 0.7f, 64.5f); gem(w, 0, 0.7f, 70.5f); gem(w, 0, 0.7f, 76.5f);
        wall(w, 82, 3); wall(w, 86, 1); sala(w, 3, 80, 79, 82); jet(w, 1, 88, 2.6f, 0f); gem(w, 1, 0.7f, 82.5f); gem(w, -1, 0.7f, 86.5f);
        pit(w, 94, 97, 0, 4); arc(w, 0, 94, 97);
        // la radura: il fuoco si ferma, la chiave sta sopra la scala di tronchi
        for (int x = 1; x <= 3; x++) { set(w, x, 106, 1); for (int z = 107; z <= 113; z++) set(w, x, z, 2); set(w, x, 114, 1); }
        gemLine3(w, 0, 2.7f, 107, 109); gemLine3(w, 0, 2.7f, 111, 113); crate(w, 0, 118); crate(w, 4, 118);
        // 3. il colpo di scena: il fuoco riparte più veloce e la strada si stringe sopra la lava, con i lapilli a tempo
        pit(w, 126, 150, 0, 1); pit(w, 126, 150, 3, 4); lap(w, 1, 132, 2.4f, 3.4f); lap(w, 3, 139, 2.2f, 3.4f); lap(w, 1, 146, 2.6f, 3.4f);
        gemLine(w, 0, 126, 150); sala(w, 2, 156, 152, 162);
        pit(w, 166, 190, 0, 4); island(w, 166, 170, 1, 3); island(w, 174, 178, 0, 2); island(w, 182, 186, 2, 4); island(w, 190, 194, 1, 3);
        lap(w, 2, 172, 2.4f, 3.4f); lap(w, 1, 180, 2.6f, 3.4f); lap(w, 3, 188, 2.2f, 3.4f);
        gem(w, 0, 0.7f, 168); gem(w, -1, 0.7f, 176); gem(w, 1, 0.7f, 184); gem(w, 0, 0.7f, 192);
        // 4. lo scatto finale: getti a scacchiera, tronchi e due salamandre, il fuoco alle spalle è il più veloce del gioco
        for (int i = 0; i < 5; i++) { int a = (i * 2) % 5; float ph = (i % 2) * 1.3f; jet(w, a, 200 + i * 3, 2.6f, ph); jet(w, (a + 2) % 5, 200 + i * 3, 2.6f, ph + 1.3f); }
        log(w, 218, 3.2f, 0f); log(w, 222, 3.2f, 1.6f); sala(w, 1, 230, 226, 236); sala(w, 3, 234, 226, 240);
        pit(w, 240, 242, 0, 4); arc(w, 0, 240, 242); gemLine(w, 0, 244, 246);
        w.flagZ = 248;
    }

    // ------------------------------------------------------------------ 34: la vetta del cielo
    // Idea: il gran finale sale. Il percorso è una scala di marmo che va sempre più in alto, e ogni piano aggiunge un'idea già vista:
    // le stelle cadenti, i solleoni, i mulinelli, le molle. In cima, il portone.
    private static void summit(World3D w) {
        gemLine3(w, 0, 0.7f, 3, 7); crate(w, 1, 9); crate(w, 3, 9);
        // 1. il primo piano: un gradino, una stella cadente che taglia la strada, un vuoto corto
        rise(w, 14, 1); star(w, 4, 6f, 22, -3f, -3.2f); gemLine3(w, 0, 1.7f, 16, 24);
        pit(w, 28, 31, 0, 4); arc(w, 0, 28, 31); for (int z = 32; z < 44; z++) for (int x = 0; x < W; x++) set(w, x, z, 1); gemLine3(w, 0, 1.7f, 33, 38);
        // 2. il secondo piano: i solleoni girano sopra i gradini, un mulinello sul pianerottolo
        rise(w, 44, 2); sun(w, 0f, 3.6f, 50f, 1.4f, 2.4f); whirl(w, 0, 56, false, -2f, 2f); gemLine3(w, -2, 2.7f, 46, 60);
        wallH(w, 64, 1, 3); wallH(w, 68, 3, 3); sun(w, 1, 3.2f, 66, 1.0f, -2.6f); gem(w, -1, 2.7f, 64.5f); gem(w, 1, 2.7f, 68.5f);
        for (int z = 64; z <= 68; z++) for (int x = 0; x < W; x++) if (w.height[z * W + x] == 0) set(w, x, z, 2);
        // il balcone della chiave: una molla sul pianerottolo porta ancora più su
        padRow(w, 74, 1, 3);
        for (int z = 80; z <= 88; z++) for (int x = 1; x <= 3; x++) set(w, x, z, 5);
        for (int x = 1; x <= 3; x++) { set(w, x, 89, 4); set(w, x, 90, 3); }
        sun(w, 0f, 7.2f, 84f, 1.5f, 2.0f); gem(w, -1, 5.7f, 82.5f); gem(w, 1, 5.7f, 86.5f);
        // 3. il colpo di scena: dal terzo piano la scala scende nel vuoto; il ponte di nuvole è sottile e le stelle cadono di traverso
        rise(w, 96, 3); pit(w, 102, 124, 0, 4); for (int z = 102; z <= 124; z++) for (int x = 1; x <= 3; x++) if (((z - 102) / 6) % 2 == 0 ? x != 3 : x != 1) set(w, x, z, 3);
        star(w, 4, 7f, 108, -2.6f, -2.8f); star(w, -4, 7f, 116, 2.6f, -2.8f); storm(w, 0, 120, true, 112f, 124f, 5f);
        for (int z = 102; z <= 124; z += 2) gem(w, 0, 3.7f, z + 0.5f);
        for (int z = 125; z < 140; z++) for (int x = 0; x < W; x++) set(w, x, z, 3);
        sun(w, 2, 4.8f, 132, 1.2f, 2.6f); gemLine3(w, 0, 3.7f, 127, 138);
        // la molla dall'alto: dal terzo piano si vola sopra un vuoto lungo e si atterra al quarto
        padRow(w, 140, 0, 4); for (int x = 0; x < W; x++) set(w, x, 140, 3); pit(w, 141, 150, 0, 4); flight3(w, 140, 3f);
        for (int z = 151; z < 176; z++) for (int x = 0; x < W; x++) set(w, x, z, 4);
        star(w, 4, 9f, 160, -2.6f, -2.8f); whirl(w, 0, 166, false, -2f, 2f); gemLine3(w, 0, 4.7f, 152, 172);
        // 4. la vetta: muri alti con i varchi, i solleoni nei varchi, l'ultima scala e il portone
        for (int z = 176; z < 240; z++) for (int x = 0; x < W; x++) set(w, x, z, 4);
        wallH(w, 180, 2, 7); wallH(w, 186, 0, 7); wallH(w, 192, 4, 7); wallH(w, 198, 1, 7);
        sun(w, 2, 5.8f, 180, 0.9f, 2.6f); sun(w, 0, 5.8f, 186, 0.9f, -2.6f); star(w, 4, 9f, 190, -2.6f, -2.8f); sun(w, 1, 5.8f, 198, 0.9f, 2.8f);
        gem(w, 0, 4.7f, 180.5f); gem(w, -2, 4.7f, 186.5f); gem(w, 2, 4.7f, 192.5f); gem(w, -1, 4.7f, 198.5f);
        pit(w, 206, 210, 0, 4); arc4(w, 206, 210); pit(w, 216, 220, 0, 4); arc4(w, 216, 220); star(w, -4, 9f, 222, 2.6f, -2.8f);
        storm(w, 0, 228, false, -1.5f, 1.5f, 6.8f); gemLine3(w, 0, 4.7f, 224, 236);
        for (int z = 240; z < 268; z++) for (int x = 0; x < W; x++) set(w, x, z, 4);
        whirl(w, 0, 248, false, -2f, 2f); gemLine3(w, 0, 4.7f, 252, 258);
        w.flagZ = 262;
    }

    // ------------------------------------------------------------------ attrezzi propri di questa serie

    /** Muro di altezza h su tutta la larghezza, con un varco alla colonna gap (0..4). Il varco resta all'altezza del terreno vicino. */
    private static void wallH(World3D w, int z, int gap, int h) {
        int base = w.height[(z - 1) * W + 2];
        if (base < 0) base = 0;
        for (int x = 0; x < W; x++) set(w, x, z, x == gap ? base : base + h);
    }

    /** Rovi su tutta la fila tranne la corsia libera, una fila sì e una no, tra z0 e z1. */
    private static void path(World3D w, int z0, int z1, int lane) {
        for (int z = z0; z <= z1; z += 2) for (int x = 0; x < W; x++) if (x != lane) thorn(w, x, z);
    }

    /** Gradino di un blocco: da qui in avanti il terreno è all'altezza h, per tutta la larghezza e fino a fine livello. */
    private static void rise(World3D w, int z, int h) {
        for (int zz = z; zz < w.length; zz++) for (int x = 0; x < W; x++) set(w, x, zz, h);
    }

    /** Gemme del volo della molla partendo da un piano alto. */
    private static void flight3(World3D w, int z, float base) {
        float[][] g = {{2.7f, 3.9f}, {5.3f, 5.2f}, {7.9f, 5.6f}, {10.5f, 4.5f}};
        for (float[] q : g) gem(w, 0, q[1] + base, z + 0.5f + q[0]);
    }

    /** Arco di gemme sopra un burrone, al quarto piano. */
    private static void arc4(World3D w, int z0, int z1) {
        int n = z1 - z0 + 1;
        for (int i = 0; i < n; i++) { float u = (i + 0.5f) / n; gem(w, 0, 4.9f + 1.5f * 4f * u * (1f - u), z0 + i + 0.5f); }
    }
}
