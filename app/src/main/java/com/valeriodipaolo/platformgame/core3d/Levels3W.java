package com.valeriodipaolo.platformgame.core3d;

import static com.valeriodipaolo.platformgame.core3d.Levels3B.*;

/**
 * Tre mondi senza bosco, ognuno con una regola che altrove non esiste.
 * <ul>
 * <li>38, le dune: le sabbie mobili rallentano e accorciano il salto, e chi ci resta troppo sprofonda; le raffiche di vento spingono
 * di lato verso i burroni (la sabbia sollevata le annuncia); scorpioni che non si possono schiacciare, avvoltoi in picchiata, cactus.</li>
 * <li>39, la fabbrica: nastri trasportatori che aiutano, spingono verso il vuoto o remano contro; barriere laser a tempo da saltare
 * (a volte con un varco spento); presse che battono sul nastro; robottini e droni.</li>
 * <li>40, la miniera: il carrello corre da solo sui binari, si cambia corsia e si salta i binari interrotti; stalattiti che si staccano,
 * pipistrelli, casse di dinamite.</li>
 * </ul>
 */
public final class Levels3W {
    private Levels3W() {}

    private static final int W = World3D.W;

    public static int theme(int id) { return id == 39 ? 22 : id == 40 ? 24 : 20; }

    public static void build(World3D w, int id) {
        int len = id == 38 ? 236 : id == 39 ? 244 : 262;
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
        w.battery = null;
        switch (id) {
            case 39: factory(w); break;
            case 40: mine(w); break;
            default: dunes(w); break;
        }
    }

    // ------------------------------------------------------------------ 38: le dune
    private static void dunes(World3D w) {
        w.quick = new boolean[w.length * W];
        gemLine(w, 0, 0, 6);
        thorn(w, 0, 8); thorn(w, 4, 10);
        // 1. le sabbie mobili: una pozza in mezzo, le gemme ci passano sopra; ai lati si cammina sul sodo
        quick(w, 15, 18, 1, 3); gemLine(w, 0, 15, 18); scorpion(w, 24);
        // tre righe di sabbie mobili su tutta la larghezza: si attraversano saltellando, il salto è corto
        quick(w, 30, 32, 0, 4); gemLine(w, 0, 29, 33); thorn(w, 1, 36); thorn(w, 3, 36);
        // 2. il vento: un passaggio di tre corsie sul vuoto; la raffica spinge verso destra, la sabbia la annuncia
        pit(w, 42, 58, 4, 4); pit(w, 42, 58, 0, 0); wind(w, 40, 60, 3.4f, 0f, 1f); gemLine(w, -1, 42, 58);
        vulture(w, 50, 3.2f);
        scorpion(w, 64); thorn(w, 2, 68);
        // la mesa della chiave: arenaria alta tre blocchi, si sale con la cassa a molla; sotto, sabbie mobili tutt'attorno
        crateK(w, 2, 74, World3D.C_BOUNCE); quick(w, 75, 77, 0, 4);
        for (int z = 78; z <= 85; z++) for (int x = 1; x <= 3; x++) set(w, x, z, 3);
        w.battery = new float[] {0, 3.6f, 82f}; gemLine3(w, 0, 3.7f, 79, 81); gemLine3(w, 0, 3.7f, 83, 84);
        quick(w, 79, 82, 0, 0); quick(w, 79, 82, 4, 4); quick(w, 86, 87, 0, 4);
        crateK(w, 2, 94, World3D.C_CHECK);
        // 3. il colpo di scena: il vento cambia verso a ogni raffica, su una lingua di sabbia larga una corsia sola
        pit(w, 102, 126, 0, 1); pit(w, 102, 126, 3, 4); wind(w, 100, 113, 2.8f, 0f, 1f); wind(w, 113, 128, 2.8f, 1.4f, -1f);
        gemLine(w, 0, 102, 126); vulture(w, 114, 3f);
        // le dune a gradini: si sale e si scende, con le sabbie mobili negli avvallamenti
        stones(w, 134, new int[] {1, 1, 1, 1, 1}); quick(w, 136, 138, 0, 4); stones(w, 140, new int[] {1, 1, 1, 1, 1}); stones(w, 141, new int[] {2, 2, 2, 2, 2});
        quick(w, 143, 145, 0, 4); scorpion(w, 150); thorn(w, 0, 154); thorn(w, 4, 154);
        // 4. l'esame: labirinto di sabbie mobili e cactus, poi il canyon del vento con gli avvoltoi
        quick(w, 160, 172, 0, 4);
        for (int z = 160; z <= 172; z++) { int lane = z < 164 ? 1 : z < 168 ? 3 : 2; w.quick[z * W + lane] = false; }
        thorn(w, 0, 162); thorn(w, 4, 166); thorn(w, 0, 170);
        gemLine(w, -1, 160, 163); gemLine(w, 1, 164, 167); gemLine(w, 0, 168, 172);
        crateK(w, 2, 178, World3D.C_CHECK); scorpion(w, 184);
        pit(w, 192, 214, 0, 0); pit(w, 192, 214, 4, 4); pit(w, 200, 203, 1, 3); wind(w, 190, 216, 3.0f, 0.5f, -1f);
        arc(w, 0, 200, 203); vulture(w, 196, 3f); vulture(w, 208, 3f); gemLine(w, 0, 204, 214);
        scorpion(w, 222); gemLine(w, 0, 220, 228);
        w.flagZ = 230;
    }

    // ------------------------------------------------------------------ 39: la fabbrica
    private static void factory(World3D w) {
        w.belt = new byte[w.length * W];
        gemLine(w, 0, 0, 6);
        // 1. il nastro amico: spinge in avanti, si corre più veloci
        belt(w, 10, 22, 0, 4, 3); gemLine(w, 0, 10, 22); robot(w, 26);
        // il nastro di traverso: spinge verso il burrone a destra, si corre tenendosi a sinistra
        pit(w, 30, 44, 4, 4); belt(w, 30, 44, 0, 3, 1); gemLine(w, -2, 30, 44);
        // 2. il laser: una barriera che si accende e si spegne; la si salta o si passa quando è spenta
        laser(w, 50, 2.4f, 0f, -1); gemLine(w, 0, 47, 53);
        laser(w, 58, 2.0f, 0.6f, 1); laser(w, 62, 2.0f, 1.2f, 3); gem(w, -1, 0.7f, 58.5f); gem(w, 1, 0.7f, 62.5f);
        drone(w, 68); robot(w, 72);
        // la pressa sul nastro: il nastro porta sotto le presse, bisogna scegliere il momento
        belt(w, 78, 92, 0, 4, 3); crusher(w, 1, 83, 2.2f, 0f); crusher(w, 2, 83, 2.2f, 0.7f); crusher(w, 3, 83, 2.2f, 1.4f);
        set(w, 0, 83, 3); set(w, 4, 83, 3); crusher(w, 2, 89, 2.0f, 0.3f); set(w, 0, 89, 3); set(w, 1, 89, 3); set(w, 3, 89, 3); set(w, 4, 89, 3);
        gemLine(w, 0, 78, 92);
        crateK(w, 2, 96, World3D.C_CHECK);
        // la passerella della chiave: in alto, sopra le presse, si sale con la cassa a molla
        crateK(w, 4, 102, World3D.C_BOUNCE);
        for (int z = 105; z <= 116; z++) set(w, 4, z, 3);
        w.battery = new float[] {2f, 3.6f, 114f}; gemLine3(w, 2, 3.7f, 106, 112);
        for (int z = 106; z <= 114; z += 4) crusher(w, 2, z, 2.4f, z * 0.3f);
        robot(w, 110); gemLine(w, 0, 104, 116);
        // 3. il colpo di scena: il nastro rema contro, e i laser si accendono a turno
        belt(w, 124, 146, 0, 4, 4); laser(w, 128, 2.2f, 0f, -1); laser(w, 134, 2.2f, 0.7f, -1); laser(w, 140, 2.2f, 1.4f, -1);
        gemLine(w, 0, 124, 146); drone(w, 137);
        // nastri a scacchiera che spingono in versi opposti sopra il vuoto laterale
        pit(w, 152, 170, 0, 0); pit(w, 152, 170, 4, 4);
        for (int z = 152; z <= 170; z++) for (int x = 1; x <= 3; x++) w.belt[z * W + x] = (byte) (((z - 152) / 3) % 2 == 0 ? 1 : 2);
        gemLine(w, 0, 152, 170); robot(w, 174); crateK(w, 2, 178, World3D.C_CHECK);
        // 4. l'esame: pressa, laser con varco, nastro di traverso verso il vuoto, droni
        belt(w, 184, 200, 0, 4, 3); crusher(w, 0, 190, 1.8f, 0f); crusher(w, 2, 190, 1.8f, 0.6f); crusher(w, 4, 190, 1.8f, 1.2f);
        laser(w, 196, 1.8f, 0.4f, 2); gemLine(w, 0, 184, 200);
        pit(w, 204, 220, 0, 0); belt(w, 204, 220, 1, 4, 2); laser(w, 212, 2.0f, 0f, -1); drone(w, 210); gemLine(w, 2, 204, 220);
        robot(w, 226); gemLine(w, 0, 224, 234);
        w.flagZ = 238;
    }

    // ------------------------------------------------------------------ 40: la miniera
    private static void mine(World3D w) {
        gemLine(w, 0, 0, 6);
        // 1. a piedi: stalattiti che si staccano quando ci si passa sotto, un pipistrello, casse di dinamite
        rock(w, 1, 10); rock(w, 3, 13); gemLine(w, 0, 9, 14); bat(w, 20);
        crateK(w, 1, 24, World3D.C_TNT); crateK(w, 3, 24, World3D.C_TNT); crateK(w, 2, 25, World3D.C_WOOD);
        // 2. il primo carrello: corre da solo; si cambia corsia per evitare i binari interrotti
        cart(w, 32, 104);
        pit(w, 40, 42, 0, 2); pit(w, 48, 50, 2, 4); pit(w, 56, 58, 0, 1); pit(w, 56, 58, 3, 4);
        gemLine(w, 2, 38, 43); gemLine(w, -2, 46, 51); gemLine(w, 0, 54, 59);
        pit(w, 64, 66, 0, 4); arc(w, 0, 64, 66);                              // binari interrotti su tutta la larghezza: si salta col carrello
        crateK(w, 0, 72, World3D.C_NITRO); crateK(w, 1, 72, World3D.C_NITRO); crateK(w, 3, 76, World3D.C_NITRO); crateK(w, 4, 76, World3D.C_NITRO);
        gemLine(w, 1, 70, 73); gemLine(w, -1, 75, 78); bat(w, 82);
        pit(w, 86, 89, 0, 4); arc(w, 0, 86, 89); pit(w, 94, 96, 1, 3); gemLine(w, -2, 92, 97);
        // 3. a piedi: la galleria della chiave, con le stalattiti in fila e un pipistrello di guardia
        rock(w, 2, 110); rock(w, 1, 113); rock(w, 3, 116); bat(w, 118);
        for (int x = 1; x <= 3; x++) { set(w, x, 121, 1); for (int z = 122; z <= 128; z++) set(w, x, z, 2); set(w, x, 129, 1); }
        w.battery = new float[] {0, 2.6f, 125f}; rockAt(w, 0, 125, 4.4f); gemLine3(w, 0, 2.7f, 123, 124); gemLine3(w, 0, 2.7f, 126, 127);
        crateK(w, 2, 134, World3D.C_CHECK);
        // 4. il secondo carrello, il colpo di scena: binari stretti su un abisso, interruzioni a zig-zag, dinamite e pipistrelli
        cart(w, 140, 236);
        pit(w, 144, 230, 0, 0); pit(w, 144, 230, 4, 4);
        pit(w, 150, 152, 1, 2); pit(w, 158, 160, 2, 3); pit(w, 166, 168, 1, 1); pit(w, 166, 168, 3, 3);
        gemLine(w, 1, 148, 153); gemLine(w, -1, 156, 161); gemLine(w, 0, 164, 169);
        bat(w, 174); pit(w, 178, 181, 1, 3); arc(w, 0, 178, 181);
        crateK(w, 1, 188, World3D.C_NITRO); crateK(w, 2, 192, World3D.C_NITRO); crateK(w, 3, 196, World3D.C_NITRO); crateK(w, 2, 200, World3D.C_NITRO);
        gem(w, 1, 0.7f, 188.5f); gem(w, -1, 0.7f, 192.5f); gem(w, 0, 0.7f, 196.5f); gem(w, 1, 0.7f, 200.5f);
        pit(w, 206, 208, 1, 3); arc(w, 0, 206, 208); bat(w, 212); pit(w, 216, 219, 1, 3); arc(w, 0, 216, 219);
        pit(w, 224, 226, 1, 2); gemLine(w, 1, 222, 228);
        gemLine(w, 0, 238, 250);
        w.flagZ = 254;
    }

    // ------------------------------------------------------------------ attrezzi

    private static void crateK(World3D w, int col, int z, byte kind) {
        crate(w, col, z);
        w.crateKind[z * W + col] = kind;
    }

    private static void quick(World3D w, int z0, int z1, int x0, int x1) {
        for (int z = z0; z <= z1; z++) for (int x = x0; x <= x1; x++) w.quick[z * W + x] = true;
    }

    private static void wind(World3D w, int z0, int z1, float period, float phase, float dir) { w.winds.add(new float[] {z0, z1, period, phase, dir}); }

    private static void belt(World3D w, int z0, int z1, int x0, int x1, int dir) {
        for (int z = z0; z <= z1; z++) for (int x = x0; x <= x1; x++) w.belt[z * W + x] = (byte) dir;
    }

    private static void laser(World3D w, int z, float period, float phase, int gap) { w.lasers.add(new float[] {z, period, phase, gap}); }

    private static void crusher(World3D w, int col, int z, float period, float phase) { w.crushers.add(new float[] {col, z, period, phase}); }

    private static void cart(World3D w, int z0, int z1) { w.carts.add(new float[] {z0, z1}); }

    private static Enemy3 skin(Enemy3 e, int s) { e.skin = s; return e; }

    private static void scorpion(World3D w, int z) { skin(add(w, Enemy3.HOG, 0, z + 0.5f, false, -2f, 2f, 0), Enemy3.SKIN_SCORPION); }

    private static void vulture(World3D w, int z, float y) { skin(add(w, Enemy3.CROW, 0, z + 0.5f, false, -2f, 2f, y), Enemy3.SKIN_VULTURE); }

    private static void bat(World3D w, int z) { skin(add(w, Enemy3.CROW, 0, z + 0.5f, false, -2f, 2f, 2.6f), Enemy3.SKIN_BAT); }

    private static void robot(World3D w, int z) { skin(add(w, Enemy3.BEETLE, 0, z + 0.5f, false, -2f, 2f, 0), Enemy3.SKIN_ROBOT); }

    private static void drone(World3D w, int z) { skin(add(w, Enemy3.OWL, 0, z + 0.5f, true, z - 4.5f, z + 5.5f, 3.4f), Enemy3.SKIN_DRONE); }

    private static void rock(World3D w, int col, int z) { rockAt(w, col, z, 3.6f); }

    private static void rockAt(World3D w, int col, int z, float y) { skin(add(w, Enemy3.CHESTNUT, col - 2f, z + 0.5f, true, z, z, y), Enemy3.SKIN_ROCK); }
}
