package com.valeriodipaolo.platformgame.core3d;

import static com.valeriodipaolo.platformgame.core3d.Levels3B.*;

/**
 * L'isola tropicale (livelli 35, 36, 37): un mondo nuovo, con regole nuove.
 * <ul>
 * <li>Le casse si rompono anche saltandoci sopra; quelle a molla lanciano in alto, le TNT si accendono e scoppiano dopo tre secondi
 * (rompendo tutto attorno), le instabili scoppiano appena le si sfiora, quelle blu segnano il punto di ripartenza.</li>
 * <li>I nemici: granchi che corrono di traverso, totem tiki che tirano noci di cocco, piranha che saltano fuori dal fiume.</li>
 * <li>35, la giungla: si imparano le casse. 36, il tempio al tramonto: un masso enorme insegue il robot, la camera si gira e lo si vede
 * arrivare mentre si corre verso lo schermo, tra pestelli di pietra che cadono. 37, il fiume di notte: zattere che affondano appena
 * ci si sale, piranha e totem sulle rive.</li>
 * </ul>
 */
public final class Levels3J {
    private Levels3J() {}

    private static final int W = World3D.W;

    public static int theme(int id) { return id == 36 ? 16 : id == 37 ? 18 : 14; }

    public static void build(World3D w, int id) {
        int len = id == 35 ? 240 : id == 36 ? 252 : 244;
        w.length = len;
        w.height = new int[len * W];
        w.crate = new boolean[len * W];
        w.crateKind = new byte[len * W];
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
            case 36: temple(w); break;
            case 37: river(w); break;
            default: crates(w); break;
        }
    }

    // ------------------------------------------------------------------ 35: la giungla delle casse
    private static void crates(World3D w) {
        gemLine(w, 0, 0, 5);
        // 1. le casse di legno si rompono saltandoci sopra: una fila da usare come trampolini, con le gemme sopra
        kc(w, 1, 9, World3D.C_WOOD); kc(w, 2, 11, World3D.C_WOOD); kc(w, 3, 13, World3D.C_WOOD);
        gem(w, -1, 2.2f, 9.5f); gem(w, 0, 2.2f, 11.5f); gem(w, 1, 2.2f, 13.5f);
        // la spiaggia dei granchi: corrono di traverso, svelti; si passa dietro di loro o gli si salta in testa
        crab(w, 20); crab(w, 26); gemLine(w, 0, 18, 28);
        // 2. la cassa a molla: davanti a un muro troppo alto per un salto solo
        kc(w, 2, 33, World3D.C_BOUNCE); wallFull(w, 36, 3); wallFull(w, 37, 3); gem(w, 0, 3.0f, 34.5f); gem(w, 0, 4.6f, 36.5f);
        // 3. la TNT: una sola, in mezzo alle gemme; si accende saltandoci sopra e dopo tre secondi rompe tutto attorno
        kc(w, 2, 46, World3D.C_TNT); kc(w, 1, 46, World3D.C_WOOD); kc(w, 3, 46, World3D.C_WOOD); kc(w, 2, 47, World3D.C_WOOD); kc(w, 2, 45, World3D.C_WOOD);
        gemLine(w, -2, 42, 50); gemLine(w, 2, 42, 50);
        // il ponte delle TNT: una corsia sola sul fiume, tre TNT da scavalcare saltandoci sopra: ognuna si accende, non ci si può fermare
        pit(w, 54, 70, 0, 1); pit(w, 54, 70, 3, 4); kc(w, 2, 57, World3D.C_TNT); kc(w, 2, 61, World3D.C_TNT); kc(w, 2, 65, World3D.C_TNT);
        gemLine(w, 0, 54, 56); gemLine(w, 0, 58, 60); gemLine(w, 0, 62, 64); gemLine(w, 0, 66, 70);
        crab(w, 80);
        // 4. le instabili: file con un solo varco che cambia lato, sfiorarle basta a farle scoppiare
        nitroRow(w, 86, 2); nitroRow(w, 90, 0); nitroRow(w, 94, 4); nitroRow(w, 98, 1);
        gem(w, 0, 0.7f, 86.5f); gem(w, -2, 0.7f, 90.5f); gem(w, 2, 0.7f, 94.5f); gem(w, -1, 0.7f, 98.5f);
        // il ruscello dei piranha: saltano a tempo dall'acqua, si passa mentre ricadono
        pit(w, 106, 110, 0, 4); arc(w, 0, 106, 110); fish(w, 1, 108, 2.4f, 3.6f); fish(w, 3, 108, 2.4f, 3.6f, 1.2f);
        crab(w, 116);
        // la terrazza della chiave: la cassa a molla porta lassù, sotto si gira attorno
        kc(w, 2, 122, World3D.C_BOUNCE);
        for (int z = 125; z <= 132; z++) for (int x = 1; x <= 3; x++) set(w, x, z, 3);
        gemLine3(w, 0, 3.7f, 126, 128); gemLine3(w, 0, 3.7f, 130, 131);
        gemLine(w, -2, 124, 134); gemLine(w, 2, 124, 134);
        // il fortino di casse con la TNT nel mezzo: accenderla è il modo più veloce per aprirlo (e per prendere tutte le gemme)
        for (int z = 142; z <= 144; z++) for (int x = 1; x <= 3; x++) kc(w, x, z, x == 2 && z == 143 ? World3D.C_TNT : World3D.C_WOOD);
        crab(w, 140); crab(w, 148);
        // 5. il colpo di scena: le casse a molla sono l'unico appoggio in mezzo al fiume
        pit(w, 156, 171, 0, 4); island(w, 158, 159, 2, 2); kc(w, 2, 159, World3D.C_BOUNCE); island(w, 165, 166, 2, 2); kc(w, 2, 166, World3D.C_BOUNCE);
        gem(w, 0, 1.6f, 157f); gem(w, 0, 3.6f, 162.5f); gem(w, 0, 3.6f, 169.5f); fish(w, 2, 162, 2.6f, 3.4f); fish(w, 2, 169, 2.6f, 3.4f, 1.3f);
       
        // 6. l'esame: corridoio di muri bassi, instabili ai lati dei varchi, una TNT da scavalcare e i granchi
        wallFull(w, 184, 1); kc(w, 2, 184, World3D.C_WOOD); set(w, 2, 184, 0); nitroRow(w, 188, 3); crab(w, 192);
        kc(w, 2, 196, World3D.C_TNT); nitroRow(w, 200, 1); crab(w, 204); nitroRow(w, 208, 4); kc(w, 0, 212, World3D.C_BOUNCE);
        gemLine(w, 0, 214, 226); crab(w, 220);
        w.flagZ = 232;
    }

    // ------------------------------------------------------------------ 36: il masso del tempio
    private static void temple(World3D w) {
        w.chase = 2;                                         // il masso rotola alle spalle per tutto il livello
        w.fireStart = -12f;
        w.fireZones = new float[][] {{0, 3.6f, 12}, {60, 4.2f, 11}, {120, 4.6f, 10}, {180, 5.0f, 10}};
        w.camBackUntil = 1e9f;                               // la camera guarda il masso finché non si incaglia
        gemLine(w, 0, 0, 7);
        // 1. si scappa: un fosso, due muri del tempio con il varco che cambia lato
        pit(w, 12, 14, 0, 4); arc(w, 0, 12, 14);
        templeWall(w, 20, 1); templeWall(w, 26, 3); gem(w, -1, 0.7f, 20.5f); gem(w, 1, 0.7f, 26.5f);
        pit(w, 32, 35, 0, 4); arc(w, 0, 32, 35);
        // 2. i pestelli: tre in fila sulla stessa riga, sfasati; uno è sempre alzato, bisogna vedere quale mentre il masso spinge
        templeRow(w, 42); crusher(w, 1, 42, 2.4f, 0f); crusher(w, 2, 42, 2.4f, 0.8f); crusher(w, 3, 42, 2.4f, 1.6f);
        gemLine(w, 0, 38, 41); gemLine(w, 0, 44, 48);
        tiki(w, 0, 54); tiki(w, 4, 58); gemLine(w, 0, 52, 60);
        pit(w, 64, 67, 0, 4); arc(w, 0, 64, 67);
        // il corridoio dei pestelli: uno ogni tre caselle sulla corsia centrale, le corsie laterali chiuse da colonne
        for (int z = 72; z <= 88; z++) for (int x = 0; x < W; x++) if (x != 2) set(w, x, z, 3);
        for (int z = 74; z <= 86; z += 4) crusher(w, 2, z, 2.0f, (z - 74) * 0.25f);
        gemLine(w, 0, 72, 88);
        // 3. la chiave: su una colonna a destra, si prende solo con il doppio salto e senza fermarsi
        for (int z = 100; z <= 103; z++) set(w, 4, z, 3);
        gem(w, 2, 3.7f, 100.5f); gem(w, 2, 3.7f, 102.5f);
        templeWall(w, 108, 2); tiki(w, 1, 112); tiki(w, 3, 116);
        pit(w, 122, 125, 0, 4); arc(w, 0, 122, 125);
        // il colpo di scena: il pavimento si stringe sopra il vuoto mentre il masso accelera
        pit(w, 132, 150, 0, 1); pit(w, 132, 150, 3, 4); gemLine(w, 0, 132, 150); crusher(w, 2, 138, 2.2f, 0.5f); crusher(w, 2, 145, 2.2f, 1.6f);
        templeWall(w, 156, 0); templeWall(w, 160, 4); templeWall(w, 164, 2); gem(w, -2, 0.7f, 156.5f); gem(w, 2, 0.7f, 160.5f); gem(w, 0, 0.7f, 164.5f);
        // 4. la salita finale: una rampa senza gradini su cui il masso rotola su; pestelli, tiki e una buca da saltare. In cima due
        //    pilastri massicci stringono il passaggio a una corsia: il masso ci si incaglia e la fuga finisce
        for (int z = 172; z < 226; z++) for (int x = 0; x < W; x++) set(w, x, z, 0);
        pit(w, 206, 209, 0, 4); templeRow3(w, 214);
        for (int z = 220; z <= 221; z++) for (int x = 0; x < W; x++) if (x != 2) set(w, x, z, 7);
        w.makeRamp(172, 226, 0f, 3f);
        crusher(w, 1, 176, 2.2f, 0f); crusher(w, 3, 176, 2.2f, 1.1f); crusher(w, 2, 186, 2.0f, 0.4f); crusher(w, 0, 186, 2.0f, 1.4f); crusher(w, 4, 186, 2.0f, 1.4f);
        tiki(w, 0, 196, w.rampY(196.5f)); tiki(w, 4, 200, w.rampY(200.5f));
        arcAt(w, 206, 209, w.rampY(207.5f)); crusher(w, 1, 214, 2.0f, 0f); crusher(w, 2, 214, 2.0f, 0.7f); crusher(w, 3, 214, 2.0f, 1.4f);
        for (int z = 192; z <= 204; z++) gem(w, 0, w.rampY(z + 0.5f) + 0.7f, z + 0.5f);
        for (int z = 216; z <= 224; z++) if (z < 220 || z > 221) gem(w, 0, w.rampY(z + 0.5f) + 0.7f, z + 0.5f);
        w.boulderStop = 220f - World3D.BOULDER_FRONT - 0.2f;
        for (int z = 226; z < 252; z++) for (int x = 0; x < W; x++) set(w, x, z, 3);
        gemLine3(w, 0, 3.7f, 230, 240);
        w.flagZ = 246;
        w.makeBreakable(0, 219);                             // muri, colonne e pestelli: il masso li travolge, non ci passa attraverso (i pilastri finali no)
    }

    // ------------------------------------------------------------------ 37: il fiume delle lucciole
    private static void river(World3D w) {
        gemLine(w, 0, 0, 5); kc(w, 1, 8, World3D.C_WOOD); kc(w, 3, 8, World3D.C_WOOD);
        // 1. la prima zattera, in mezzo a un fiume troppo largo per un salto: ci si sale, affonda subito dopo, quindi si salta via
        pit(w, 14, 19, 0, 4); raft(w, 2, 16); raft(w, 2, 17); gem(w, 0, 1.2f, 16.5f);
        tiki(w, 4, 24); gemLine(w, -1, 20, 26);
        // la fila di zattere: si corre senza fermarsi, ogni zattera cede sotto i piedi
        pit(w, 30, 44, 0, 4); for (int z = 30; z <= 44; z++) raft(w, 2, z); gemLine(w, 0, 30, 44);
        crab(w, 48);
        // 2. due file sfalsate e i piranha che saltano dove la fila si interrompe
        pit(w, 54, 74, 0, 4);
        for (int z = 54; z <= 74; z++) raft(w, ((z - 54) / 5) % 2 == 0 ? 1 : 3, z);
        fish(w, 1, 60, 2.2f, 3.4f); fish(w, 3, 66, 2.2f, 3.4f, 1.1f); fish(w, 1, 71, 2.2f, 3.4f, 0.5f);
        for (int z = 54; z <= 74; z += 2) gem(w, ((z - 54) / 5) % 2 == 0 ? -1 : 1, 0.9f, z + 0.5f);
        tiki(w, 0, 84); tiki(w, 4, 88); gemLine(w, 0, 80, 90);
        // 3. l'isolotto della chiave: a sinistra, oltre una fila di zattere a parte; la strada principale va dritta
        pit(w, 96, 116, 0, 4); for (int z = 96; z <= 116; z++) raft(w, 3, z);
        island(w, 104, 108, 0, 0); raft(w, 2, 100); raft(w, 1, 100); for (int z = 100; z <= 103; z++) raft(w, 0, z);
        raft(w, 0, 109); raft(w, 1, 109); raft(w, 2, 109);
        gem(w, -2, 0.7f, 104.5f); gem(w, -2, 0.7f, 107.5f);
        fish(w, 2, 106, 2.4f, 3.6f); gemLine(w, 1, 96, 116);
        // le casse a molla sulle isolette in mezzo al fiume largo
        crab(w, 122);
        pit(w, 128, 146, 0, 4); island(w, 130, 131, 2, 2); kc(w, 2, 131, World3D.C_BOUNCE); island(w, 137, 138, 2, 2); kc(w, 2, 138, World3D.C_BOUNCE);
        for (int z = 142; z <= 146; z++) raft(w, 2, z);
        gem(w, 0, 2.6f, 130.5f); gem(w, 0, 3.6f, 135.5f); gem(w, 0, 3.4f, 140.5f); fish(w, 1, 135, 2.6f, 3.4f); fish(w, 3, 141, 2.6f, 3.4f, 1.3f);
       
        // 4. il colpo di scena: il fiume diventa una rapida a tre corsie di zattere, con le TNT a galla sulle isole
        pit(w, 160, 200, 0, 4);
        for (int z = 160; z <= 200; z++) { raft(w, 0, z); raft(w, 4, z); if ((z / 8) % 2 == 0) raft(w, 2, z); }
        for (int z = 164; z <= 196; z += 8) { island(w, z, z, 2, 2); kc(w, 2, z, World3D.C_TNT); }
        fish(w, 1, 170, 2.0f, 3.2f); fish(w, 3, 178, 2.0f, 3.2f, 1.0f); fish(w, 1, 186, 2.0f, 3.2f, 0.5f); fish(w, 3, 194, 2.0f, 3.2f, 1.5f);
        gemLine(w, -2, 160, 200); gemLine(w, 2, 160, 200);
        tiki(w, 1, 206); tiki(w, 3, 210); crab(w, 214); nitroRow(w, 218, 2); crab(w, 222);
        gemLine(w, 0, 224, 234);
        w.flagZ = 238;
    }

    // ------------------------------------------------------------------ attrezzi

    private static void kc(World3D w, int col, int z, byte kind) {
        crate(w, col, z);
        w.crateKind[z * W + col] = kind;
    }

    private static void nitroRow(World3D w, int z, int gap) {
        for (int x = 0; x < W; x++) if (x != gap) kc(w, x, z, World3D.C_NITRO);
    }

    private static void wallFull(World3D w, int z, int h) { for (int x = 0; x < W; x++) set(w, x, z, h); }

    /** Muro del tempio alto tre blocchi con un varco. */
    private static void templeWall(World3D w, int z, int gap) { for (int x = 0; x < W; x++) set(w, x, z, x == gap ? 0 : 3); }

    /** Riga di pestelli: le colonne laterali sono pietra piena, al centro si passa solo sotto i blocchi. */
    private static void templeRow(World3D w, int z) { set(w, 0, z, 3); set(w, 4, z, 3); }

    private static void templeRow3(World3D w, int z) { set(w, 0, z, 6); set(w, 4, z, 6); }

    private static void arcAt(World3D w, int z0, int z1, float base) {
        int n = z1 - z0 + 1;
        for (int i = 0; i < n; i++) { float u = (i + 0.5f) / n; gem(w, 0, base + 0.9f + 1.5f * 4f * u * (1f - u), z0 + i + 0.5f); }
    }

    private static void crab(World3D w, int z) {
        Enemy3 e = add(w, Enemy3.BEETLE, 0, z + 0.5f, false, -2f, 2f, 0);
        e.skin = Enemy3.SKIN_CRAB;
    }

    private static void tiki(World3D w, int col, int z) { tiki(w, col, z, 0f); }

    private static void tiki(World3D w, int col, int z, float y) {
        Enemy3 e = add(w, Enemy3.SNOWMAN, col - 2f, z + 0.5f, false, col - 2f, col - 2f, y);
        e.skin = Enemy3.SKIN_TIKI;
    }

    private static void fish(World3D w, int col, int z, float period, float height) { fish(w, col, z, period, height, 0f); }

    private static void fish(World3D w, int col, int z, float period, float height, float phase) {
        Enemy3 e = add(w, Enemy3.LAPILLO, col - 2f, z + 0.5f, false, period, height, 0);
        e.skin = Enemy3.SKIN_FISH;
        e.t = phase;
    }

    private static void crusher(World3D w, int col, int z, float period, float phase) { w.crushers.add(new float[] {col, z, period, phase}); }

    private static void raft(World3D w, int col, int z) {
        set(w, col, z, World3D.PIT);
        for (float[] r : w.rafts) if (r[0] == col && r[1] == z) return;
        w.rafts.add(new float[] {col, z, -1f, 3.2f});
    }
}
