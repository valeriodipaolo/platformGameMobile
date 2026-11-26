package com.valeriodipaolo.platformgame.core3d;

import static com.valeriodipaolo.platformgame.core3d.Levels3B.*;

/**
 * Tre mondi senza bosco, ognuno con una regola che altrove non esiste.
 * <ul>
 * <li>38, le dune: le sabbie mobili rallentano e accorciano il salto, e chi ci resta troppo sprofonda; le raffiche di vento spingono
 * di lato verso i burroni (la sabbia sollevata le annuncia); scorpioni che non si possono schiacciare, avvoltoi in picchiata, cactus.</li>
 * <li>39, la fabbrica: nastri trasportatori che aiutano, spingono verso il vuoto o remano contro; barriere laser a tempo da saltare
 * (a volte con un varco spento); presse che battono sul nastro; robottini e droni.</li>
 * <li>40, la miniera: tutta in carrello su tre binari, lunga il triplo; si cambia corsia davanti ai binari rotti e si salta dove manca tutto;
 * il percorso è disseminato di fosse, pipistrelli, topi e casse di nitroglicerina, sempre più fitti.</li>
 * </ul>
 */
public final class Levels3W {
    private Levels3W() {}

    private static final int W = World3D.W;

    public static int theme(int id) { return id == 39 ? 22 : id == 40 ? 24 : 20; }

    public static void build(World3D w, int id) {
        int len = id == 38 ? 236 : id == 39 ? 244 : MINE_LEN;
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
        skin(add(w, Enemy3.CROW, 0, 50.5f, false, -1f, 1f, 3.2f), Enemy3.SKIN_VULTURE);
        scorpion(w, 64); thorn(w, 2, 68);
        // la mesa della chiave: arenaria alta tre blocchi, si sale con la cassa a molla; sotto, sabbie mobili tutt'attorno
        crateK(w, 2, 74, World3D.C_BOUNCE); quick(w, 75, 77, 0, 4);
        for (int z = 78; z <= 85; z++) for (int x = 1; x <= 3; x++) set(w, x, z, 3);
        gemLine3(w, 0, 3.7f, 79, 81); gemLine3(w, 0, 3.7f, 83, 84);
        quick(w, 79, 82, 0, 0); quick(w, 79, 82, 4, 4); quick(w, 86, 87, 0, 4);
        crateK(w, 2, 94, World3D.C_CHECK);
        // 3. il colpo di scena: il vento cambia verso a ogni raffica, su una lingua di sabbia larga una corsia sola
        pit(w, 102, 126, 0, 1); pit(w, 102, 126, 3, 4); wind(w, 100, 113, 2.8f, 0f, 1f); wind(w, 113, 128, 2.8f, 1.4f, -1f);
        gemLine(w, 0, 102, 126); vultureZ(w, 114, 3f);
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
        arc(w, 0, 200, 203); skin(add(w, Enemy3.CROW, 0, 196.5f, false, -1f, 1f, 3f), Enemy3.SKIN_VULTURE); skin(add(w, Enemy3.CROW, 0, 208.5f, false, -1f, 1f, 3f), Enemy3.SKIN_VULTURE); gemLine(w, 0, 204, 214);
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
        gemLine3(w, 2, 3.7f, 106, 112);
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

    /** La miniera è tutta in carrello: tre binari (corsie 1-3, il carrello non ne esce), dall'inizio alla fine. */
    static final int MINE_LEN = 790;
    /** Riga della fossa dopo la quale è sospeso il gettone d'oro (sopra il binario di sinistra, si prende solo col doppio salto). */
    static int mineCoinZ;

    /**
     * Il percorso è disseminato di fosse, nemici e casse di nitroglicerina, in tre tratti sempre più fitti; c'è sempre una corsia
     * libera o un salto semplice, e mai due manovre a meno di un secondo l'una dall'altra. Ogni novanta metri circa un rettilineo
     * sgombro, dove si salva il punto di ripartenza.
     */
    private static void mine(World3D w) {
        int end = MINE_LEN - 12;
        cart(w, 2, end);
        gemLine(w, 0, 6, 14);
        java.util.Random r = new java.util.Random(4040);
        int z = 24, n = 0, nextRest = 100;
        mineCoinZ = -1;
        while (z < end - 24) {
            if (z >= nextRest) {                                   // rettilineo sgombro: si riprende fiato (e si salva la ripartenza)
                gemLine(w, 0, z + 3, z + 9);
                z += 18;
                nextRest += 95 + r.nextInt(15);
                continue;
            }
            int stage = z < 240 ? 0 : z < 500 ? 1 : 2;
            int lane = 1 + r.nextInt(3), other = lane == 2 ? (r.nextBoolean() ? 1 : 3) : 2;
            int free = 6 - lane - other;                          // la terza corsia
            int kind = stage == 0 ? EASY[n % EASY.length] : stage == 1 ? MID[n % MID.length] : HARD[n % HARD.length];
            int len = 2;
            switch (kind) {
                case 0:                                            // un binario interrotto
                    pit(w, z, z + 2, lane, lane); gemLine(w, other - 2, z - 1, z + 2); len = 3; break;
                case 1:                                            // due binari interrotti: resta una corsia sola
                    pit(w, z, z + 2, lane, lane); pit(w, z, z + 2, other, other); gemLine(w, free - 2, z - 1, z + 2); len = 3; break;
                case 2:                                            // fossa su tutta la larghezza: si salta
                    pit(w, z, z + 2, 0, 4); cartArc(w, z);
                    if (mineCoinZ < 0 && z > 380) mineCoinZ = z;
                    len = 3; break;
                case 3:                                            // cassa di nitroglicerina su un binario
                    crateK(w, lane, z, World3D.C_NITRO); gem(w, other - 2, 0.7f, z + 0.5f); len = 1; break;
                case 4:                                            // due casse di nitroglicerina: una corsia sola
                    crateK(w, lane, z, World3D.C_NITRO); crateK(w, other, z, World3D.C_NITRO); gemLine(w, free - 2, z - 1, z + 1); len = 1; break;
                case 5:                                            // pipistrello che svolazza di traverso
                    skin(add(w, Enemy3.CROW, 0, z + 0.5f, false, -1f, 1f, 0.9f), Enemy3.SKIN_BAT); len = 1; break;
                case 6:                                            // topo che attraversa i binari
                    skin(add(w, Enemy3.BEETLE, lane - 2f, z + 0.5f, false, -1f, 1f, 0), Enemy3.SKIN_MOUSE); len = 1; break;
                case 7:                                            // binario rotto e cassa sull'altro: passa solo la terza corsia
                    pit(w, z, z + 2, lane, lane); crateK(w, other, z + 1, World3D.C_NITRO); gemLine(w, free - 2, z - 1, z + 2); len = 3; break;
                case 8:                                            // salto, e appena atterrati una cassa da schivare
                    pit(w, z, z + 1, 0, 4); cartArc(w, z); crateK(w, lane, z + 9, World3D.C_NITRO); gem(w, other - 2, 0.7f, z + 9.5f); len = 10; break;
                case 9:                                            // due binari rotti, poi un pipistrello
                    pit(w, z, z + 2, lane, lane); pit(w, z, z + 2, other, other); gemLine(w, free - 2, z - 1, z + 2);
                    skin(add(w, Enemy3.CROW, 0, z + 11.5f, false, -1f, 1f, 0.9f), Enemy3.SKIN_BAT); len = 12; break;
                default:                                           // cassa sul binario, e subito dopo un topo che attraversa
                    crateK(w, lane, z, World3D.C_NITRO); gem(w, other - 2, 0.7f, z + 0.5f);
                    skin(add(w, Enemy3.BEETLE, other - 2f, z + 9.5f, false, -1f, 1f, 0), Enemy3.SKIN_MOUSE); len = 10; break;
            }
            n++;
            int gap = stage == 0 ? 10 + r.nextInt(3) : stage == 1 ? 9 + r.nextInt(2) : 8 + r.nextInt(2);
            z += len + gap - 2;
        }
        gemLine(w, 0, end + 2, end + 8);
        w.flagZ = MINE_LEN - 2;
    }

    /**
     * Arco di gemme sul binario centrale che segue la parabola vera del carrello lanciato (salto spiccato poco prima della fossa):
     * l'arco dei livelli a piedi è troppo corto per la velocità del carrello e le prime gemme si perdevano.
     */
    private static void cartArc(World3D w, int z) {
        float spd = w.cartSpeed(z);
        for (int i = 0; i < 6; i++) {
            float d = -0.5f + i * spd * 0.13f, t = (d + 1.2f) / spd, y = 10.5f * t - 12f * t * t;
            gem(w, 0, Math.max(0.7f, y + 0.55f), z + d);
        }
    }

    /**
     * Il gettone d'oro della miniera: alto sopra il binario di sinistra, appena oltre una fossa; ci arriva solo chi la salta
     * stando a sinistra e rilancia il doppio salto in cima alla parabola.
     */
    static void mineCoin(World3D w) {
        if (mineCoinZ > 0) w.coin = new float[] {-1f, 4.0f, mineCoinZ + 5.5f};
    }

    /**
     * Ordine degli ostacoli nei tre tratti: 0 un binario rotto, 1 due rotti, 2 salto, 3 una cassa, 4 due casse, 5 pipistrello,
     * 6 topo, 7 binario rotto + cassa, 8 salto + cassa, 9 due rotti + pipistrello, 10 cassa + topo.
     */
    private static final int[] EASY = {0, 5, 3, 6, 2, 0, 4, 5, 1, 6, 3, 2};
    private static final int[] MID = {7, 5, 2, 6, 1, 3, 9, 4, 0, 10, 2, 5, 8, 6};
    private static final int[] HARD = {7, 8, 5, 4, 9, 2, 10, 1, 6, 7, 3, 8, 5, 9, 2, 10};

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

    /** Avvoltoio sopra una lingua di terra larga una corsia: fa la ronda avanti e indietro, non di traverso. */
    private static void vultureZ(World3D w, int z, float y) { skin(add(w, Enemy3.CROW, 0, z + 0.5f, true, z - 5f, z + 5f, y), Enemy3.SKIN_VULTURE); }

    private static void batNarrow(World3D w, int z) { skin(add(w, Enemy3.CROW, 0, z + 0.5f, false, -1f, 1f, 2.6f), Enemy3.SKIN_BAT); }

    private static void bat(World3D w, int z) { skin(add(w, Enemy3.CROW, 0, z + 0.5f, false, -2f, 2f, 2.6f), Enemy3.SKIN_BAT); }

    private static void robot(World3D w, int z) { skin(add(w, Enemy3.BEETLE, 0, z + 0.5f, false, -2f, 2f, 0), Enemy3.SKIN_ROBOT); }

    private static void drone(World3D w, int z) { skin(add(w, Enemy3.OWL, 0, z + 0.5f, true, z - 4.5f, z + 5.5f, 3.4f), Enemy3.SKIN_DRONE); }

    private static void rock(World3D w, int col, int z) { rockAt(w, col, z, 3.6f); }

    private static void rockAt(World3D w, int col, int z, float y) { skin(add(w, Enemy3.CHESTNUT, col - 2f, z + 0.5f, true, z, z, y), Enemy3.SKIN_ROCK); }
}
