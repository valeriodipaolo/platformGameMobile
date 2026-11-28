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

    private static final int W = World3D.W5;

    public static int theme(int id) { return id == 39 ? 22 : id == 40 ? 24 : id == 41 ? 2 : id == 42 ? 6 : id == 43 ? 12 : id == 44 ? 6 : id == 45 ? 8 : id == 46 ? 10 : 20; }

    /** Livelli di corsa a piedi (si corre da soli, si sterza a destra e a sinistra): la corsa tra le nuvole e le tre fughe. */
    public static boolean isRun(int id) { return id >= 43 && id <= 46; }

    /** Lunghezza delle corse: lunghe, con tre tratti di difficoltà crescente. */
    static final int RUN_LEN = 660;

    /** Varianti della miniera nei temi principali: lo stesso viaggio in carrello nel bosco d'estate, nella neve e sulle nuvole. */
    static final int RAIL_LEN = 400;

    public static void build(World3D w, int id) {
        int len = id == 38 ? 236 : id == 39 ? 244 : id == 40 ? MINE_LEN : isRun(id) ? RUN_LEN : RAIL_LEN;
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
            case 40: cartRun(w, MINE_LEN, 4040, true); break;
            case 41: cartRun(w, RAIL_LEN, 4141, false); break;
            case 42: cartRun(w, RAIL_LEN, 4242, false); break;
            case 43: runCourse(w, RUN_LEN, 4343, 0); break;                 // corsa tra le nuvole: niente inseguitore
            case 44: runCourse(w, RUN_LEN, 4444, 3); break;                 // fuga dalla valanga
            case 45: runCourse(w, RUN_LEN, 4545, 4); break;                 // fuga dalla lava
            case 46: runCourse(w, RUN_LEN, 4646, 1); break;                 // fuga dallo sciame
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
        scorpion(w, 184);
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
        gemLine(w, 0, 152, 170); robot(w, 174);        // 4. l'esame: pressa, laser con varco, nastro di traverso verso il vuoto, droni
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
    private static void cartRun(World3D w, int total, long seed, boolean mine) {
        int end = total - 12;
        cart(w, 0, end);                                   // si parte già dentro: il carrello c'è dal primo istante
        gemLine(w, 0, 6, 14);
        java.util.Random r = new java.util.Random(seed);
        int z = 24, n = mine ? 0 : (int) (seed % 5), nextRest = 100;      // le varianti partono da un punto diverso della sequenza
        int s1 = total * 240 / MINE_LEN, s2 = total * 500 / MINE_LEN, coinAfter = mine ? 380 : total / 4;   // i tre tratti in proporzione alla lunghezza
        mineCoinZ = -1;
        while (z < end - 24) {
            if (z >= nextRest) {                                   // rettilineo sgombro: si riprende fiato (e si salva la ripartenza)
                gemLine(w, 0, z + 3, z + 9);
                z += 18;
                nextRest += 95 + r.nextInt(15);
                continue;
            }
            int stage = z < s1 ? 0 : z < s2 ? 1 : 2;
            int lane = 1 + r.nextInt(3), other = lane == 2 ? (r.nextBoolean() ? 1 : 3) : 2;
            int free = 6 - lane - other;                          // la terza corsia
            int kind = stage == 0 ? EASY[n % EASY.length] : stage == 1 ? MID[n % MID.length] : HARD[n % HARD.length];
            int len = 2;
            if (kind == 2 || kind == 8) z += 4;                    // prima di un salto un tratto in più: le gemme di rincorsa non si sovrappongono all'ostacolo di prima
            switch (kind) {
                case 0:                                            // un binario interrotto
                    pit(w, z, z + 2, lane, lane); gemLine(w, other - 2, z - 1, z + 2); len = 3; break;
                case 1:                                            // due binari interrotti: resta una corsia sola
                    pit(w, z, z + 2, lane, lane); pit(w, z, z + 2, other, other); gemLine(w, free - 2, z - 1, z + 2); len = 3; break;
                case 2: {                                          // fossa su tutta la larghezza: si salta
                    pit(w, z, z + 2, 0, 4);
                    boolean coinPit = mineCoinZ < 0 && z > coinAfter;      // la fossa del gettone: gemme sul binario di sinistra, sotto il gettone
                    if (coinPit) mineCoinZ = z;
                    len = jumpGems(w, z, coinPit ? -1f : 0f) - z;
                    break;
                }
                case 3:                                            // cassa di nitroglicerina su un binario
                    crateK(w, lane, z, World3D.C_NITRO); gem(w, other - 2, 0.7f, z + 0.5f); len = 1; break;
                case 4:                                            // due casse di nitroglicerina: una corsia sola
                    crateK(w, lane, z, World3D.C_NITRO); crateK(w, other, z, World3D.C_NITRO); gemLine(w, free - 2, z - 1, z + 1); len = 1; break;
                case 5:                                            // pipistrello che svolazza di traverso
                    flyer(w, z + 0.5f, mine); len = 1; break;
                case 6:                                            // topo che attraversa i binari
                    walker(w, lane - 2f, z + 0.5f, mine); len = 1; break;
                case 7:                                            // binario rotto e cassa sull'altro: passa solo la terza corsia
                    pit(w, z, z + 2, lane, lane); crateK(w, other, z + 1, World3D.C_NITRO); gemLine(w, free - 2, z - 1, z + 2); len = 3; break;
                case 8: {                                          // salto, poi (a distanza di sicurezza) una dinamite sul binario di lato
                    pit(w, z, z + 1, 0, 4);
                    int after = jumpGems(w, z, 0f), side = r.nextBoolean() ? 1 : 3;
                    crateK(w, side, after + 4, World3D.C_NITRO);       // le gemme dell'atterraggio restano al centro, lontano dalla dinamite
                    len = after + 5 - z;
                    break;
                }
                case 9:                                            // due binari rotti, poi un pipistrello
                    pit(w, z, z + 2, lane, lane); pit(w, z, z + 2, other, other); gemLine(w, free - 2, z - 1, z + 2);
                    flyer(w, z + 11.5f, mine); len = 12; break;
                default:                                           // cassa sul binario, e subito dopo un topo che attraversa
                    crateK(w, lane, z, World3D.C_NITRO); gem(w, other - 2, 0.7f, z + 0.5f);
                    walker(w, other - 2f, z + 9.5f, mine); len = 10; break;
            }
            n++;
            int gap = stage == 0 ? 10 + r.nextInt(3) : stage == 1 ? 9 + r.nextInt(2) : 8 + r.nextInt(2);
            z += len + gap - 2;
        }
        gemLine(w, 0, end + 2, end + 8);
        w.flagZ = total - 2;
        if (mineCoinZ > 0) w.coin = new float[] {-1f, 4.0f, mineCoinZ - 1f + 0.8f * w.cartSpeed(mineCoinZ)};
    }


    // ------------------------------------------------------------------ corse a piedi (43-46)
    /**
     * Corsa a piedi: il robot corre da solo sempre più veloce e si sterza tra le tre corsie centrali (solo destra e sinistra), si
     * salta. Tre tratti di difficoltà crescente, con rettilinei di respiro ogni tanto (dove si salva la ripartenza). Oltre agli
     * ostacoli delle corse in carrello: il ponte a zig-zag (una sola corsia che cambia lato), il burrone lungo da passare col doppio
     * salto e lo slalom tra gli ostacoli. chase: 0 nessuno, 1 sciame, 3 valanga, 4 lava; l'inseguitore resta a qualche metro dietro
     * e ogni colpo preso (che fa inciampare il robot) lo avvicina: due errori ravvicinati e raggiunge il robot.
     */
    private static void runCourse(World3D w, int total, long seed, int chase) {
        int end = total - 12;
        cart(w, 0, end);                                   // si parte già dentro: il carrello c'è dal primo istante
        gemLine(w, 0, 6, 14);
        java.util.Random r = new java.util.Random(seed);
        int z = 26, n = (int) (seed % 7), nextRest = 110, coinZ = -1;
        int s1 = total * 30 / 100, s2 = total * 65 / 100;
        while (z < end - 30) {
            if (z >= nextRest) {
                gemLine(w, 0, z + 3, z + 9);
                z += 18;
                nextRest += 105 + r.nextInt(20);
                continue;
            }
            int stage = z < s1 ? 0 : z < s2 ? 1 : 2;
            int lane = 1 + r.nextInt(3), other = lane == 2 ? (r.nextBoolean() ? 1 : 3) : 2, free = 6 - lane - other;
            int kind = stage == 0 ? RUN_EASY[n % RUN_EASY.length] : stage == 1 ? RUN_MID[n % RUN_MID.length] : RUN_HARD[n % RUN_HARD.length];
            int len;
            if (kind == 2 || kind == 8 || kind == 12) z += 4;
            switch (kind) {
                case 0: pit(w, z, z + 2, lane, lane); gemLine(w, other - 2, z - 1, z + 2); len = 3; break;
                case 1: pit(w, z, z + 2, lane, lane); pit(w, z, z + 2, other, other); gemLine(w, free - 2, z - 1, z + 2); len = 3; break;
                case 2: pit(w, z, z + 2, 0, 4); len = jumpGems(w, z, 0f) - z; break;
                case 3: crateK(w, lane, z, World3D.C_NITRO); gem(w, other - 2, 0.7f, z + 0.5f); len = 1; break;
                case 4: crateK(w, lane, z, World3D.C_NITRO); crateK(w, other, z, World3D.C_NITRO); gemLine(w, free - 2, z - 1, z + 1); len = 1; break;
                case 5: flyer(w, z + 0.5f, false); len = 1; break;
                case 6: walker(w, lane - 2f, z + 0.5f, false); len = 1; break;
                case 7: pit(w, z, z + 2, lane, lane); crateK(w, other, z + 1, World3D.C_NITRO); gemLine(w, free - 2, z - 1, z + 2); len = 3; break;
                case 8: {
                    pit(w, z, z + 1, 0, 4);
                    int after = jumpGems(w, z, 0f), side = r.nextBoolean() ? 1 : 3;
                    crateK(w, side, after + 4, World3D.C_NITRO);
                    len = after + 5 - z;
                    break;
                }
                case 9: pit(w, z, z + 2, lane, lane); pit(w, z, z + 2, other, other); gemLine(w, free - 2, z - 1, z + 2); flyer(w, z + 11.5f, false); len = 12; break;
                case 11: {                                        // ponte a zig-zag: resta una corsia sola, che si sposta di una alla volta
                    int[] seq = r.nextBoolean() ? new int[] {1, 2, 3, 2, 1} : new int[] {3, 2, 1, 2, 3};
                    int segs = stage == 0 ? 3 : stage == 1 ? 4 : 5, seg = stage == 2 ? 4 : 5;
                    for (int k = 0; k < segs; k++) {
                        int open = seq[k], zs = z + k * seg;
                        for (int q = zs; q < zs + seg; q++) for (int c = 1; c <= 3; c++) {
                            boolean overlap = k > 0 && c == seq[k - 1] && q < zs + 2;      // al cambio di lato le due corsie restano aperte insieme per due caselle
                            if (c != open && !overlap) pit(w, q, q, c, c);
                        }
                        pit(w, zs, zs + seg - 1, 0, 0); pit(w, zs, zs + seg - 1, 4, 4);
                        for (int q = zs + 1; q < zs + seg - 1; q += 2) gem(w, open - 2, 0.7f, q + 0.5f);
                    }
                    len = segs * seg;
                    break;
                }
                case 12: {                                        // burrone lungo: col salto semplice non basta, serve il doppio salto
                    float spd = w.cartSpeed(z);
                    int L = Math.max(9, Math.round(0.95f * spd));
                    pit(w, z, z + L - 1, 0, 4);
                    gemLine(w, 0, z - 6, z - 3);
                    for (int k = 1; k < L; k += 2) {
                        float u = k / (float) L;
                        gem(w, 0, 1.6f + 2.2f * 4f * u * (1f - u), z + k + 0.5f);
                    }
                    if (coinZ < 0 && z > total / 3) {             // il gettone: in alto e di lato, sopra il primo burrone lungo dopo un terzo di corsa
                        coinZ = z;
                        w.coin = new float[] {-1f, 4.6f, z + L * 0.5f};
                    }
                    int land = z + L + 1;
                    gemLine(w, 0, land, land + 3);
                    len = land + 4 - z;
                    break;
                }
                case 13: {                                        // slalom: in ogni fila resta libera una corsia sola, sempre diversa
                    int[] frees = r.nextBoolean() ? new int[] {3, 1, 2, 3} : new int[] {1, 3, 2, 1};
                    int rows = stage == 2 ? 4 : 3, gapZ = stage == 2 ? 6 : 7;
                    for (int k = 0; k < rows; k++) {
                        int zz = z + k * gapZ;
                        for (int c = 1; c <= 3; c++) if (c != frees[k]) crateK(w, c, zz, World3D.C_NITRO);
                        gem(w, frees[k] - 2, 0.7f, zz + 0.5f);
                    }
                    len = rows * gapZ;
                    break;
                }
                case 14: flyer(w, z + 0.5f, false); flyer(w, z + 6.5f, false); len = 7; break;
                default:
                    crateK(w, lane, z, World3D.C_NITRO); gem(w, other - 2, 0.7f, z + 0.5f);
                    walker(w, other - 2f, z + 9.5f, false); len = 10; break;
            }
            n++;
            int gap = stage == 0 ? 10 + r.nextInt(3) : stage == 1 ? 9 + r.nextInt(2) : 8 + r.nextInt(2);
            z += len + gap - 2;
        }
        gemLine(w, 0, end + 2, end + 8);
        w.flagZ = total - 2;
        if (chase != 0) {
            w.chase = chase;
            w.fireStart = -14f;
            w.fireZ = -14f;
            w.fireZones = new float[][] {{0, World3D.CART_SPEED - 1.2f, RUN_GAP}};
        }
    }

    /** Distanza massima dell'inseguitore dietro al robot nelle fughe. */
    static final float RUN_GAP = 9f;

    /** Sequenze degli ostacoli nelle corse a piedi (vedi runCourse; 11 zig-zag, 12 burrone lungo, 13 slalom, 14 due volatili). */
    private static final int[] RUN_EASY = {0, 5, 3, 11, 6, 2, 0, 4, 13, 5, 1, 6, 12, 3};
    private static final int[] RUN_MID = {11, 7, 12, 5, 2, 13, 9, 4, 11, 10, 6, 12, 1, 14, 7};
    private static final int[] RUN_HARD = {12, 11, 13, 9, 14, 7, 12, 4, 10, 11, 8, 13, 12, 1, 14, 6, 11};

    /** Volatile che svolazza di traverso all'altezza del robot: pipistrello in miniera, altrove l'uccello della stagione. */
    private static void flyer(World3D w, float z, boolean mine) {
        Enemy3 e = add(w, Enemy3.CROW, 0, z, false, -1f, 1f, 0.9f);
        if (mine) e.skin = Enemy3.SKIN_BAT;
    }

    /** Animaletto che attraversa i binari: topo in miniera, altrove quello della stagione. */
    private static void walker(World3D w, float x, float z, boolean mine) {
        if (w.theme == 12) { add(w, Enemy3.WHIRL, x, z, false, -1f, 1f, 0); return; }     // tra le nuvole: un mulinello di vento
        Enemy3 e = add(w, Enemy3.BEETLE, x, z, false, -1f, 1f, 0);
        if (mine) e.skin = Enemy3.SKIN_MOUSE;
    }

    /**
     * Gemme di una fossa da saltare, tutte a terra e sullo stesso binario (cx): quattro prima, per mettersi in posizione, e cinque
     * dopo, che cominciano oltre il punto dove atterra anche chi fa il doppio salto. Sopra la fossa niente: non si perde nulla
     * comunque si salti. Restituisce la riga dopo l'ultima gemma: fino a lì niente ostacoli, l'atterraggio è sempre sgombro.
     */
    private static int jumpGems(World3D w, int z, float cx) {
        float spd = w.cartSpeed(z);
        gemLine(w, cx, z - 6, z - 3);
        int land = (int) Math.ceil(z - 1 + 1.3f * spd);
        gemLine(w, cx, land, land + 4);
        return land + 5;
    }

    /**
     * Ordine degli ostacoli nei tre tratti: 0 un binario rotto, 1 due rotti, 2 salto, 3 una cassa, 4 due casse, 5 pipistrello,
     * 6 topo, 7 binario rotto + cassa, 8 salto + cassa più avanti, 9 due rotti + pipistrello, 10 cassa + topo. I salti sono pochi
     * (uno o due per giro), il resto si risolve cambiando binario.
     */
    private static final int[] EASY = {0, 5, 3, 6, 2, 0, 4, 5, 1, 6, 3, 0};
    private static final int[] MID = {7, 5, 2, 6, 1, 3, 9, 4, 0, 10, 7, 5, 1, 6};
    private static final int[] HARD = {7, 4, 5, 9, 2, 10, 1, 6, 7, 3, 0, 8, 5, 9, 4, 10};

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
