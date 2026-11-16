package com.valeriodipaolo.platformgame.core3d;

/**
 * Seconda serie dei livelli 3D (14, 16, 18, 20, 22, 24): stesso aspetto delle controparti (2, 4, 6, 8, 10, 12), lunghi il doppio, con più
 * ostacoli e due nemici propri ciascuno. Ogni burrone ha un motivo (un nemico lo sorveglia, un arrivo da scegliere, una molla da trovare) e
 * ogni nemico sta dove costringe a un gesto preciso: aspettare, schiacciare, girare attorno, scattare.
 */
public final class Levels3B {
    private Levels3B() {}

    private static final int W = World3D.W;

    public static void build(World3D w, int id) {
        int len = id == 14 ? 300 : id == 16 ? 316 : id == 18 ? 324 : id == 20 ? 336 : id == 22 ? 332 : 352;
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
        w.battery = null;
        switch (id) {
            case 16: autumn(w); break;
            case 18: winter(w); break;
            case 20: fire(w); break;
            case 22: spring(w); break;
            case 24: heaven(w); break;
            default: summer(w); break;
        }
    }

    // ------------------------------------------------------------------ 14: il bosco d'estate — talpe e lumache
    private static void summer(World3D w) {
        // avvio: prato libero, e la prima lumaca che insegna a schiacciarla (ritira il guscio, non muore)
        gemLine(w, 0, 0, 5); crate(w, 1, 9); crate(w, 3, 9);
        snail(w, 1, 12, 8, 20); gemLine(w, -2, 14, 18);
        // primo burrone corto: una talpa abita proprio dove si atterra
        pit(w, 26, 28, 0, 4); arc(w, 0, 26, 28); gemLine(w, -1, 30, 33);
        // il campo delle talpe: tre tane sfasate tra le casse, le gemme stanno nel mezzo
        crate(w, 1, 36); crate(w, 3, 36);
        gem(w, 1, 0.7f, 40); gem(w, 3, 0.7f, 42); gem(w, 1, 0.7f, 44);
        // muretti di pietra con una lumaca che pattuglia tra l'uno e l'altro
        stones(w, 50, new int[] {1, 1, 0, 1, 1}); stones(w, 58, new int[] {1, 0, 1, 1, 1});
        snail(w, 2, 54, 51, 57); hog(w, -1, 62, false, -2f, 2f); gem(w, 0, 1.8f, 50.5f); gem(w, -1, 1.8f, 58.5f);
        // burrone con ponte stretto: la talpa fa da dogana a metà ponte
        pit(w, 66, 72, 0, 1); pit(w, 66, 72, 3, 4); gemLine(w, 2, 66, 68); gemLine(w, 2, 70, 72);
        // radura con la cella energetica sopra una terrazza, sorvegliata da un corvo
        for (int x = 1; x <= 3; x++) { set(w, x, 80, 1); for (int z = 81; z <= 86; z++) set(w, x, z, 2); set(w, x, 87, 1); }
        w.battery = new float[] {0, 2.6f, 83.5f}; crow(w, 0, 83, false, -2f, 2f, 3.6f); gemLine3(w, 0, 2.7f, 81, 82); gemLine3(w, 0, 2.7f, 85, 86);
        snail(w, -2, 90, 88, 96); snail(w, 2, 94, 90, 99); crate(w, 0, 98); crate(w, 4, 98);
        // secondo campo di talpe, questa volta con ghiande tra le tane
        acorn(w, 0f, 108); acorn(w, 1f, 118); gemLine(w, 2, 103, 119);
        // doppio burrone, isolotto in mezzo con una lumaca immobile nella tana
        pit(w, 124, 126, 0, 4); pit(w, 130, 132, 0, 4); snail(w, 2, 128, 127, 129); arc(w, 0, 124, 126); arc(w, 0, 130, 132);
        // scalinata e terrazza lunga: in cima due lumache e una talpa
        for (int x = 0; x < W; x++) { set(w, x, 138, 1); for (int z = 139; z <= 150; z++) set(w, x, z, 2); set(w, x, 151, 1); }
        snail(w, -1, 144, 140, 149, 2f); snail(w, 1, 147, 140, 149, 2f); gemLine3(w, 0, 2.7f, 140, 149);
        // corsa dei muri: ogni muro ha il varco dal lato opposto; i nemici stanno proprio nei varchi
        crate(w, 1, 156); crate(w, 3, 156);
        wall(w, 160, 4); wall(w, 164, 0); wall(w, 168, 3); wall(w, 172, 1); wall(w, 176, 4);
        snail(w, -2, 166, 164, 168); hog(w, 0, 170, false, -2f, 2f);
        gem(w, 2, 0.7f, 160.5f); gem(w, -2, 0.7f, 164.5f); gem(w, 1, 0.7f, 168.5f); gem(w, -1, 0.7f, 172.5f); gem(w, 2, 0.7f, 176.5f);
        // zig-zag sopra il vuoto, come nel livello di prima ma più stretto, con corvi e lumache sul ciglio
        pit(w, 184, 192, 2, 4); pit(w, 195, 203, 0, 2); pit(w, 206, 214, 0, 0); pit(w, 206, 214, 4, 4); pit(w, 217, 225, 0, 4);
        for (int z = 217; z <= 225; z++) set(w, 2, z, 0);
        gemLine(w, -2, 185, 191); gemLine(w, 2, 196, 202); gemLine(w, 0, 207, 213); gemLine(w, 2, 218, 224);
        crow(w, 0, 188, true, 184f, 192f, 2.3f); crow(w, 2, 199, true, 195f, 203f, 2.3f); snail(w, 2, 210, 206, 214);
        // fattoria di casse: sei casse piene di gemme, ognuna con un guardiano
        for (int x = 0; x < W; x += 2) { crate(w, x, 236); crate(w, x, 240); }
        snail(w, 1, 238, 233, 243); hog(w, 2, 249, false, -2f, 2f);
        // gli ultimi tre burroni, a distanza di due caselle: serve ritmo, e al terzo aspetta una talpa
        pit(w, 256, 258, 0, 4); pit(w, 262, 264, 0, 4); pit(w, 268, 270, 0, 4); arc(w, 0, 256, 258); arc(w, 0, 262, 264); arc(w, 0, 268, 270);
        // prato finale: lumache a ritroso e una barriera di pietra con le talpe dietro
        snail(w, 0, 276, 273, 281); snail(w, 4, 280, 273, 283);
        stones(w, 285, new int[] {1, 1, 1, 0, 1}); stones(w, 289, new int[] {1, 0, 1, 1, 1});
        gemLine(w, 0, 292, 295);
        w.flagZ = 296;
    }

    // ------------------------------------------------------------------ 16: il bosco d'autunno — castagne e gufi
    private static void autumn(World3D w) {
        gemLine(w, 0, 0, 5); crate(w, 1, 9); crate(w, 3, 9);
        // sotto la prima quercia pende una castagna: cade appena ci si passa sotto, e rotola verso chi scappa
        chest(w, 2, 16, 3.4f); gemLine(w, 2, 11, 19); acorn(w, -1f, 22);
        // burrone con il gufo che lo pattuglia alto: si passa quando si è girato
        pit(w, 28, 31, 0, 4); owl(w, 0, 24, 36, 3.4f); arc(w, 0, 28, 31);
        // il filare: tre castagne in fila, una per corsia, a cadere a ritmi diversi se si rallenta
        chest(w, 1, 40, 3.4f); chest(w, 3, 44, 3.4f); chest(w, 0, 48, 3.4f); chest(w, 2, 52, 3.4f); hog(w, 2, 46, false, -2f, 2f);
        gem(w, 0, 0.7f, 41); gem(w, 4, 0.7f, 45); gem(w, 2, 0.7f, 49); gem(w, 1, 0.7f, 53);
        // muro di pietra e gufo: dietro al muro il gufo si tuffa su chi sbuca
        stones(w, 58, new int[] {1, 1, 1, 0, 1}); owl(w, 1, 56, 66, 3.2f); stones(w, 64, new int[] {1, 0, 1, 1, 1}); crate(w, 2, 61);
        // sentiero nel bosco: due burroni stretti e una castagna sopra il secondo
        pit(w, 72, 74, 0, 4); chest(w, 2, 79, 3.4f); pit(w, 82, 84, 0, 4); gemLine(w, -1, 76, 80); arc(w, 0, 72, 74); arc(w, 0, 82, 84);
        // terrazza con la cella energetica e un gufo di guardia
        for (int x = 1; x <= 3; x++) { set(w, x, 92, 1); for (int z = 93; z <= 99; z++) set(w, x, z, 2); set(w, x, 100, 1); }
        w.battery = new float[] {0, 2.6f, 96f}; owl(w, 0, 93, 100, 4.1f); gemLine3(w, 0, 2.7f, 94, 99);
        // la strada di foglie: ghiande e castagne alternate
        acorn(w, 0f, 106); chest(w, 3, 109, 3.4f); acorn(w, 1f, 112); chest(w, 1, 115, 3.4f); acorn(w, 2f, 118); hog(w, 0, 121, false, -2f, 2f);
        gemLine(w, -2, 105, 122);
        // due gufi che si alternano su un tratto stretto sopra il vuoto
        pit(w, 128, 142, 0, 0); pit(w, 128, 142, 4, 4); owl(w, 1, 128, 135, 3.2f); owl(w, 3, 135, 142, 3.2f); gemLine(w, 0, 128, 142);
        // gradinata: castagne che cadono dall'alto delle scale
        for (int x = 0; x < W; x++) { set(w, x, 148, 1); for (int z = 149; z <= 156; z++) set(w, x, z, 2); set(w, x, 157, 1); }
        chest(w, 1, 151, 5f); chest(w, 3, 154, 5f); gemLine3(w, 0, 2.7f, 149, 156);
        // muri con varco: gufi che sorvegliano i varchi
        wall(w, 164, 2); wall(w, 168, 0); wall(w, 172, 4); wall(w, 176, 1);
        owl(w, 2, 160, 170, 3.2f); owl(w, 0, 170, 178, 3.2f); gem(w, 0, 0.7f, 164.5f); gem(w, -2, 0.7f, 168.5f); gem(w, 2, 0.7f, 172.5f); gem(w, -1, 0.7f, 176.5f);
        // il fiume di foglie: tre burroni di fila, isolotti con castagna
        pit(w, 184, 187, 0, 4); chest(w, 2, 190, 3.6f); pit(w, 193, 196, 0, 4); chest(w, 2, 199, 3.6f); pit(w, 202, 205, 0, 4);
        arc(w, 0, 184, 187); arc(w, 0, 193, 196); arc(w, 0, 202, 205);
        // il boschetto di funghi: casse con guardiani
        for (int x = 0; x < W; x += 2) { crate(w, x, 212); crate(w, x, 216); }
        owl(w, 2, 210, 220, 3.4f); hog(w, 1, 214, false, -2f, 2f); acorn(w, 3f, 223);
        // tratto stretto a zig-zag
        pit(w, 230, 238, 2, 4); pit(w, 241, 249, 0, 2); pit(w, 252, 260, 0, 0); pit(w, 252, 260, 4, 4);
        gemLine(w, -2, 231, 237); gemLine(w, 2, 242, 248); gemLine(w, 0, 253, 259);
        chest(w, 1, 234, 3.4f); chest(w, 3, 245, 3.4f); owl(w, 2, 253, 260, 3.2f);
        // l'ultima quercia: due castagne che cadono insieme e un gufo dopo di esse
        chest(w, 1, 268, 3.6f); chest(w, 3, 268, 3.6f); owl(w, 2, 272, 282, 3.2f); gem(w, 2, 0.7f, 268); gem(w, 0, 0.7f, 276);
        stones(w, 288, new int[] {1, 1, 0, 1, 1}); chest(w, 2, 292, 3.4f); stones(w, 296, new int[] {1, 0, 1, 1, 1}); owl(w, 1, 298, 306, 3.4f);
        gemLine(w, 0, 303, 308);
        w.flagZ = 312;
    }

    // ------------------------------------------------------------------ 18: il lago gelato — pinguini e pupazzi
    private static void winter(World3D w) {
        gemLine(w, 0, 0, 5); crate(w, 1, 9); crate(w, 3, 9);
        // il primo pinguino scivola da parte a parte: si passa dietro di lui
        peng(w, 12, -2f, 2f); gemLine(w, -2, 11, 16);
        // un pupazzo sul ciglio di un burrone: le palle di neve costringono a muoversi lungo il bordo
        pit(w, 24, 27, 0, 4); snowman(w, 2, 31); arc(w, 0, 24, 27); gemLine(w, -1, 29, 33);
        // lastre di ghiaccio: tre pinguini a velocità diverse, lanciati a ritroso lungo la pista
        peng(w, 38, -2f, 2f); peng(w, 42, -2f, 2f); peng(w, 46, -2f, 2f); crate(w, 0, 40); crate(w, 4, 44);
        gem(w, 0, 0.7f, 39); gem(w, 2, 0.7f, 43); gem(w, -2, 0.7f, 47);
        // muretti di neve con due pupazzi che coprono i varchi
        stones(w, 54, new int[] {1, 1, 0, 1, 1}); snowman(w, 2, 58); stones(w, 62, new int[] {1, 0, 1, 1, 1}); snowman(w, -2, 66);
        // ponte di ghiaccio stretto sul vuoto: un pinguino ne pattuglia la lunghezza
        pit(w, 72, 86, 0, 1); pit(w, 72, 86, 3, 4); peng2(w, 2, 74, 85); gemLine(w, 2, 72, 86);
        // terrazza con la cella energetica: un pupazzo ai due lati
        for (int x = 1; x <= 3; x++) { set(w, x, 94, 1); for (int z = 95; z <= 101; z++) set(w, x, z, 2); set(w, x, 102, 1); }
        w.battery = new float[] {0, 2.6f, 98f}; snowman(w, -2, 98, 0f); gemLine3(w, 0, 2.7f, 96, 100);
        // la discesa: tre pinguini in fila che partono insieme, due burroni sul fondo
        peng(w, 108, -2f, 2f); peng(w, 111, -2f, 2f); pit(w, 116, 118, 0, 4); pit(w, 123, 125, 0, 4); snowman(w, 2, 120); arc(w, 0, 116, 118); arc(w, 0, 123, 125);
        // tratto di iceberg: isolotti sparsi, ognuno con un pinguino
        pit(w, 130, 156, 0, 4); island(w, 130, 134, 1, 3); island(w, 138, 142, 0, 2); island(w, 146, 150, 2, 4); island(w, 154, 158, 1, 3);
        peng2(w, 2, 131, 133); gem(w, 0, 0.7f, 140); gem(w, 2, 0.7f, 148); gem(w, 0, 0.7f, 156); peng2(w, 1, 139, 141); crow(w, 0, 144, false, -2f, 2f, 2.6f);
        // muri con varchi e pupazzi all'ingresso di ognuno
        wall(w, 164, 2); wall(w, 168, 0); wall(w, 172, 4); wall(w, 176, 1); wall(w, 180, 3);
        snowman(w, 4, 160); snowman(w, 0, 170, 0f); peng(w, 174, -2f, 2f);
        gem(w, 0, 0.7f, 164.5f); gem(w, -2, 0.7f, 168.5f); gem(w, 2, 0.7f, 172.5f); gem(w, -1, 0.7f, 176.5f); gem(w, 1, 0.7f, 180.5f);
        // il campo di fessure: tre burroni di una casella nel ghiaccio, lunghi e stretti
        pit(w, 190, 190, 0, 4); pit(w, 195, 195, 0, 4); pit(w, 200, 200, 0, 4); pit(w, 205, 207, 0, 4); snowman(w, 2, 211);
        gemLine(w, 0, 189, 210);
        // pista dei pinguini: quattro pinguini con casse in mezzo
        for (int x = 0; x < W; x += 2) { crate(w, x, 214); crate(w, x, 224); }
        peng(w, 217, -2f, 2f); peng(w, 219, -2f, 2f); peng(w, 221, -2f, 2f);
        // sentiero a zig-zag con i pupazzi come cecchini
        pit(w, 232, 240, 2, 4); pit(w, 243, 251, 0, 2); pit(w, 254, 262, 0, 0); pit(w, 254, 262, 4, 4);
        gemLine(w, -2, 233, 239); gemLine(w, 2, 244, 250); gemLine(w, 0, 255, 261);
        snowman(w, 0, 237, 0f); snowman(w, 4, 248, 0f); peng2(w, 2, 255, 261);
        // l'ultima distesa: pupazzi su isolotti, pinguini che passano sotto di essi
        for (int x = 0; x < W; x++) { set(w, x, 270, 1); for (int z = 271; z <= 278; z++) set(w, x, z, 2); set(w, x, 279, 1); }
        snowman(w, -1, 273, 2f); snowman(w, 1, 277, 2f); gemLine3(w, 0, 2.7f, 271, 278);
        peng(w, 284, -2f, 2f); peng(w, 288, -2f, 2f); crate(w, 1, 292); crate(w, 3, 292); snowman(w, 2, 298);
        stones(w, 304, new int[] {1, 1, 0, 1, 1}); snowman(w, 0, 308); stones(w, 312, new int[] {1, 0, 1, 1, 1});
        gemLine(w, 0, 314, 318);
        w.flagZ = 320;
    }

    // ------------------------------------------------------------------ 20: il bosco in fiamme — lapilli e salamandre
    private static void fire(World3D w) {
        w.fireStart = -14f;
        w.fireZones = new float[][] {{10, 1.8f, 20}, {70, 2.2f, 20}, {140, 0f}, {156, 2.6f, 18}, {230, 2.9f, 16}, {290, 3.1f, 14}};
        gemLine(w, 0, 0, 4); gemLine(w, 2, 5, 9); crate(w, 1, 10); crate(w, 3, 10);
        // la prima salamandra cammina incontro e sputa fuoco ogni tanto: si passa quando ha appena finito
        sala(w, 1, 15, 12, 26); gemLine(w, -2, 14, 22);
        // burrone di lava con il lapillo che schizza al centro: si salta mentre ricade
        pit(w, 32, 36, 0, 4); lap(w, 2, 34, 2.4f, 3.2f); arc(w, 0, 32, 36);
        // i tronchi in fiamme e, tra un tronco e l'altro, un getto
        log(w, 44, 3.8f, 0f); log(w, 49, 3.8f, 1.9f); log(w, 54, 3.6f, 0.5f); jet(w, 2, 46, 2.8f, 0f); jet(w, 2, 52, 2.8f, 1.4f);
        gem(w, 0, 0.7f, 46.5f); gem(w, 0, 0.7f, 51.5f); gem(w, 0, 0.7f, 56.5f); sala(w, 0, 62, 58, 66);
        // slalom di muri bruciati con una salamandra all'imbocco di ogni varco
        wall(w, 72, 4); wall(w, 76, 0); wall(w, 80, 3); wall(w, 84, 1);
        sala(w, 4, 70, 69, 72); jet(w, 2, 74, 2.8f, 0f); sala(w, 0, 78, 77, 80); jet(w, 2, 82, 2.8f, 1.4f);
        gem(w, 2, 0.7f, 72.5f); gem(w, -2, 0.7f, 76.5f); gem(w, 1, 0.7f, 80.5f); gem(w, -1, 0.7f, 84.5f);
        // il ponte sulla lava: tre lapilli lo attraversano a tempi diversi
        pit(w, 92, 112, 0, 1); pit(w, 92, 112, 3, 4); lap(w, 1, 98, 2.6f, 3.4f); lap(w, 3, 104, 2.2f, 3.4f); lap(w, 1, 110, 2.8f, 3.4f); gemLine(w, 2, 92, 112);
        // radura con la cella energetica sopra una scala di tronchi
        for (int x = 1; x <= 3; x++) { set(w, x, 120, 1); for (int z = 121; z <= 127; z++) set(w, x, z, 2); set(w, x, 128, 1); }
        w.battery = new float[] {0, 2.6f, 124f}; sala(w, 0, 123, 121, 127, 2f); gemLine3(w, 0, 2.7f, 121, 127);
        // sosta: il fuoco si ferma qui (zona a velocità zero) e si ascoltano i tronchi
        gemLine(w, 0, 132, 138); crate(w, 1, 134); crate(w, 3, 134);
        // braciere a scacchiera: getti fitti e due salamandre sul fondo
        for (int i = 0; i < 7; i++) { int a = (i * 2) % 5, b = (a + 2) % 5; float ph = (i % 2) * 1.4f; jet(w, a, 146 + i * 3, 2.8f, ph); jet(w, b, 146 + i * 3, 2.8f, ph + 1.4f); }
        sala(w, 2, 170, 166, 174); sala(w, 0, 176, 174, 180);
        // fiume di lava con lapilli su tre isolotti
        pit(w, 184, 206, 0, 4); island(w, 184, 188, 1, 3); island(w, 192, 196, 0, 2); island(w, 200, 204, 2, 4); island(w, 208, 212, 1, 3);
        lap(w, 2, 190, 2.4f, 3.4f); lap(w, 1, 198, 2.6f, 3.4f); lap(w, 3, 206, 2.2f, 3.4f); gem(w, 0, 0.7f, 186); gem(w, 1, 0.7f, 194); gem(w, 3, 0.7f, 202); gem(w, 2, 0.7f, 210);
        // muri con varco e getti
        wall(w, 220, 2); wall(w, 224, 0); wall(w, 228, 4); wall(w, 232, 1);
        jet(w, 2, 222, 2.8f, 0f); sala(w, 4, 226, 224, 228); jet(w, 3, 230, 2.8f, 1.4f);
        gem(w, 0, 0.7f, 220.5f); gem(w, -2, 0.7f, 224.5f); gem(w, 2, 0.7f, 228.5f); gem(w, -1, 0.7f, 232.5f);
        // il rogo finale a due strisce di lava: lapilli sui due bordi, salamandre sulla via di mezzo
        pit(w, 244, 262, 0, 0); pit(w, 244, 262, 4, 4); lap(w, 0, 248, 2.4f, 3f); lap(w, 4, 254, 2.4f, 3f); lap(w, 0, 260, 2.4f, 3f);
        sala(w, 2, 252, 246, 258); gemLine(w, 2, 244, 262);
        log(w, 268, 3.4f, 0f); log(w, 272, 3.4f, 1.7f); log(w, 276, 3.4f, 0.9f); jet(w, 1, 270, 2.6f, 0f); jet(w, 3, 274, 2.6f, 1.3f);
        sala(w, 2, 282, 278, 288); pit(w, 292, 295, 0, 4); lap(w, 2, 294, 2.2f, 3.4f); arc(w, 0, 292, 295);
        sala(w, 1, 300, 297, 306); sala(w, 3, 304, 297, 310); jet(w, 2, 312, 2.4f, 0.8f); jet(w, 2, 316, 2.4f, 2.0f);
        gemLine(w, 0, 318, 328);
        w.flagZ = 332;
    }

    // ------------------------------------------------------------------ 22: la primavera — rane e piante carnivore
    private static void spring(World3D w) {
        gemLine(w, 0, 0, 5); crate(w, 1, 9); crate(w, 3, 9);
        // l'aiuola d'ingresso: la pianta si schiaccia solo da chiusa; quando trema sta per scattare
        plant(w, 2, 14); gemLine(w, -2, 11, 18); thorn(w, 0, 20); thorn(w, 4, 20);
        // primo fiume con il fiore a molla: sull'altra sponda una rana aspetta chi atterra
        padRow(w, 26, 0, 4); pit(w, 27, 36, 0, 4); flight(w, 26); frog(w, 2, 40); gemLine(w, 0, 38, 42);
        // il roseto: rovi e piante alternati, le gemme sopra le piante chiuse
        thorn(w, 1, 46); thorn(w, 3, 46); plant(w, 0, 48); plant(w, 4, 48); thorn(w, 2, 50); plant(w, 1, 52); plant(w, 3, 52);
        gem(w, 0, 1.2f, 48); gem(w, 4, 1.2f, 48); gem(w, 1, 1.2f, 52); gem(w, 3, 1.2f, 52);
        // ruscello stretto: le rane lo attraversano a balzi, una per sponda
        pit(w, 58, 61, 0, 4); frog(w, 1, 56); frog(w, 3, 64); arc(w, 0, 58, 61);
        // la terrazza fiorita con la cella energetica; il fiore a molla porta in cima
        padRow(w, 70, 1, 3);
        for (int x = 1; x <= 3; x++) { for (int z = 76; z <= 84; z++) set(w, x, z, 3); set(w, x, 85, 2); set(w, x, 86, 1); }
        w.battery = new float[] {0, 3.6f, 80f}; plant(w, 1, 78, 3f); gemLine3(w, 0, 3.7f, 76, 84);
        // sentiero con rovi alle due estremità e una rana al centro che sbarra la via
        for (int z = 92; z <= 106; z += 2) { thorn(w, 0, z); thorn(w, 4, z + 1); }
        frog(w, 2, 98); frog(w, 2, 104); gemLine(w, 2, 92, 106);
        // secondo fiume largo dieci: serve il fiore a molla, due piante a guardia dei fiori
        padRow(w, 112, 0, 4); pit(w, 113, 122, 0, 4); flight(w, 112); plant(w, 0, 110); plant(w, 4, 110);
        // radura di casse con rane in agguato
        for (int x = 0; x < W; x += 2) { crate(w, x, 130); crate(w, x, 134); }
        frog(w, 1, 132); frog(w, 3, 138); plant(w, 2, 142);
        // muri di cespugli con varco: pianta nel varco di ognuno
        wall(w, 150, 4); wall(w, 154, 0); wall(w, 158, 3); wall(w, 162, 1);
        plant(w, 4, 148); frog(w, 2, 156); plant(w, 3, 160); frog(w, 0, 164);
        gem(w, 2, 0.7f, 150.5f); gem(w, -2, 0.7f, 154.5f); gem(w, 1, 0.7f, 158.5f); gem(w, -1, 0.7f, 162.5f);
        // il delta: tre rami d'acqua con isolotti di fiori
        pit(w, 170, 194, 0, 4); island(w, 170, 174, 1, 3); island(w, 178, 182, 0, 2); island(w, 186, 190, 2, 4); island(w, 194, 198, 1, 3);
        frog(w, 2, 172); plant(w, 1, 180); frog(w, 3, 188); gem(w, 0, 0.7f, 176); gem(w, 1, 0.7f, 184); gem(w, 3, 0.7f, 192);
        padRow(w, 169, 1, 3);
        // campo di rovi con corsia libera che cambia lato
        for (int z = 204; z <= 224; z++) { int lane = ((z - 204) / 7) % 2 == 0 ? 0 : 4; for (int x = 0; x < W; x++) if (x != lane && (z % 2 == 0) == (x % 2 == 0)) thorn(w, x, z); }
        frog(w, 2, 214); plant(w, 4, 220); gemLine(w, 0, 204, 210); gemLine(w, 4, 211, 224);
        // zig-zag sull'acqua con piante sulle sponde
        pit(w, 232, 240, 2, 4); pit(w, 243, 251, 0, 2); pit(w, 254, 262, 0, 0); pit(w, 254, 262, 4, 4);
        gemLine(w, -2, 233, 239); gemLine(w, 2, 244, 250); gemLine(w, 0, 255, 261); plant(w, 0, 236); plant(w, 4, 247); frog(w, 2, 258);
        // il terzo fiume con le molle nascoste dietro un cespuglio di rovi
        padRow(w, 268, 0, 4); pit(w, 269, 278, 0, 4); flight(w, 268); thorn(w, 2, 266); frog(w, 1, 282); frog(w, 3, 286);
        // tratto finale: piante a coppie ai lati, rana in mezzo
        plant(w, 0, 292); plant(w, 4, 292); frog(w, 2, 295); plant(w, 1, 300); plant(w, 3, 300); frog(w, 2, 304);
        stones(w, 310, new int[] {1, 1, 0, 1, 1}); frog(w, 2, 314); stones(w, 318, new int[] {1, 0, 1, 1, 1});
        gemLine(w, 0, 320, 326);
        w.flagZ = 328;
    }

    // ------------------------------------------------------------------ 24: il cielo — stelle e solleoni
    private static void heaven(World3D w) {
        gemLine3(w, 0, 0.7f, 4, 7); crate(w, 1, 9); crate(w, 3, 9);
        // nuvola d'ingresso e scalinata di marmo; il primo solleone gira attorno alla cima
        for (int x = 1; x <= 3; x++) { set(w, x, 13, 1); set(w, x, 14, 2); set(w, x, 15, 2); set(w, x, 16, 1); }
        sun(w, 0f, 3.2f, 14.5f, 1.4f, 2.2f); gem(w, -2, 0.7f, 14.5f); whirl(w, 0, 22, false, -2f, 2f);
        // ponte di nuvola: tre caselle sul vuoto, una stella cadente lo taglia di traverso
        pit(w, 28, 41, 0, 4); for (int z = 28; z <= 41; z++) for (int x = 1; x <= 3; x++) set(w, x, z, 0);
        star(w, 4, 5f, 32, -3f, -3.2f); star(w, 4, 5f, 38, -3.4f, -3.4f); gem(w, 0, 0.7f, 30.5f); gem(w, 0, 0.7f, 36.5f);
        // corridoio di muri di marmo: i solleoni orbitano sui varchi
        wall(w, 46, 4); wall(w, 50, 0); wall(w, 54, 3); wall(w, 58, 1);
        sun(w, 4, 1.8f, 46, 1.0f, 2.6f); sun(w, 0, 1.8f, 50, 1.0f, -2.6f); crow(w, 0, 56, false, -2f, 2f, 2.4f);
        gem(w, 2, 0.7f, 46.5f); gem(w, -2, 0.7f, 50.5f); gem(w, 1, 0.7f, 54.5f); gem(w, -1, 0.7f, 58.5f);
        // primo vuoto da dieci metri: nuvole a molla e una stella che scende proprio lungo la traiettoria
        padRow(w, 64, 0, 4); pit(w, 65, 74, 0, 4); flight(w, 64); star(w, 3, 7f, 70, -2.4f, -2.6f);
        gemLine3(w, 0, 0.7f, 76, 78);
        // balcone con la cella energetica, raggiungibile dalle nuvole a molla: un solleone ne custodisce la cima
        crate(w, 1, 80); crate(w, 3, 80); padRow(w, 83, 1, 3);
        for (int z = 89; z <= 98; z++) for (int x = 1; x <= 3; x++) set(w, x, z, 4);
        for (int x = 1; x <= 3; x++) { set(w, x, 99, 2); set(w, x, 100, 1); }
        w.battery = new float[] {0, 5.3f, 94f}; sun(w, 0f, 6.2f, 94f, 1.5f, 2.0f); gem(w, -1, 4.7f, 91.5f); gem(w, 1, 4.7f, 96.5f);
        // sentiero stretto sul vuoto con stelle a due quote
        pit(w, 106, 126, 0, 0); pit(w, 106, 126, 4, 4); star(w, 3, 5f, 110, -2.6f, -2.8f); star(w, -3, 5f, 118, 2.6f, -2.8f); storm(w, 0, 114, true, 108f, 122f, 2.9f);
        for (int z = 106; z <= 126; z += 3) gem(w, 0, 0.7f, z + 0.5f);
        // secondo vuoto da dieci metri, con due solleoni alla partenza
        padRow(w, 132, 0, 4); pit(w, 133, 142, 0, 4); flight(w, 132); sun(w, -1.5f, 2.2f, 130, 0.9f, 3.0f); sun(w, 1.5f, 2.2f, 130, 0.9f, -3.0f);
        gemLine3(w, 0, 0.7f, 144, 146);
        // arcipelago di isolotti con stelle e un mulinello
        crate(w, 1, 149); crate(w, 3, 149);
        pit(w, 152, 183, 0, 4); island(w, 152, 155, 1, 3); island(w, 159, 162, 2, 3); island(w, 166, 169, 1, 2); island(w, 173, 176, 0, 2); island(w, 180, 183, 1, 3);
        gem(w, 0, 0.7f, 153.5f); gem(w, 1, 0.7f, 160.5f); gem(w, -1, 0.7f, 167.5f); gem(w, -1, 0.7f, 174.5f);
        star(w, 4, 6f, 157, -2.4f, -2.4f); star(w, 4, 6f, 171, -2.4f, -2.4f); storm(w, 0, 164, false, -1.5f, 1.5f, 3.4f); whirl(w, 2, 181, false, 1f, 3f);
        // i quattro muri alti con le stelle che scendono nei varchi
        crate(w, 1, 190); crate(w, 3, 190); wall(w, 194, 2); wall(w, 198, 0); wall(w, 202, 4); wall(w, 206, 1);
        star(w, 3, 5f, 196, -2.6f, -3f); sun(w, 0, 1.8f, 200, 1.0f, 2.8f); crow(w, 0, 204, false, -2f, 2f, 2.1f);
        gem(w, 0, 0.7f, 194.5f); gem(w, -2, 0.7f, 198.5f); gem(w, 2, 0.7f, 202.5f); gem(w, -1, 0.7f, 206.5f);
        // la scalinata alta: tre solleoni in fila come lanterne
        for (int x = 0; x < W; x++) { set(w, x, 214, 1); for (int z = 215; z <= 230; z++) set(w, x, z, 2); set(w, x, 231, 1); }
        sun(w, -1f, 3.2f, 218, 1.2f, 2.4f); sun(w, 1f, 3.2f, 223, 1.2f, -2.4f); sun(w, 0f, 3.2f, 228, 1.2f, 2.6f); gemLine3(w, 0, 2.7f, 215, 230);
        // terzo vuoto da dieci metri con la stella finale che attraversa il volo
        padRow(w, 238, 0, 4); pit(w, 239, 248, 0, 4); flight(w, 238); star(w, 4, 7f, 244, -2.6f, -2.6f); star(w, -4, 7f, 247, 2.4f, -2.6f);
        gemLine3(w, 0, 0.7f, 250, 252);
        // il lungo ponte stretto con sei isolotti a zig-zag
        pit(w, 258, 292, 0, 0); pit(w, 258, 292, 4, 4);
        for (int z = 258; z <= 292; z++) { int lane = ((z - 258) / 6) % 2 == 0 ? 1 : 3; for (int x = 1; x <= 3; x++) if (x != lane && z % 6 == 2) set(w, x, z, World3D.PIT); }
        star(w, 4, 5f, 264, -2.6f, -2.8f); star(w, -4, 5f, 272, 2.6f, -2.8f); sun(w, 2, 2.2f, 280, 1.2f, 2.4f); storm(w, 0, 288, true, 284f, 292f, 2.9f);
        for (int z = 258; z <= 292; z += 3) gem(w, 0, 0.7f, z + 0.5f);
        // ultimi muri e il portone
        crate(w, 1, 298); crate(w, 3, 298); wall(w, 302, 2); wall(w, 306, 0); wall(w, 310, 4); wall(w, 314, 1);
        sun(w, 4, 1.8f, 304, 1.0f, 2.6f); star(w, 3, 5f, 308, -2.6f, -3f); crow(w, 0, 312, false, -2f, 2f, 2.1f); whirl(w, 0, 326, false, -2f, 2f);
        gem(w, 0, 0.7f, 302.5f); gem(w, -2, 0.7f, 306.5f); gem(w, 2, 0.7f, 310.5f); gem(w, -1, 0.7f, 314.5f);
        gemLine3(w, 0, 0.7f, 322, 332);
        w.flagZ = 340;
    }

    // ------------------------------------------------------------------ attrezzi
    private static void set(World3D w, int x, int z, int h) { w.height[z * W + x] = h; }

    private static void pit(World3D w, int z0, int z1, int x0, int x1) {
        for (int z = z0; z <= z1; z++) for (int x = x0; x <= x1; x++) set(w, x, z, World3D.PIT);
    }

    private static void island(World3D w, int z0, int z1, int x0, int x1) {
        for (int z = z0; z <= z1; z++) for (int x = x0; x <= x1; x++) set(w, x, z, 0);
    }

    private static void stones(World3D w, int z, int[] h) { for (int x = 0; x < W; x++) set(w, x, z, h[x]); }

    private static void wall(World3D w, int z, int gap) { for (int x = 0; x < W; x++) set(w, x, z, x == gap ? 0 : 1); }

    private static void crate(World3D w, int x, int z) { w.crate[z * W + x] = true; }

    private static void gem(World3D w, float cx, float y, float z) { w.gemsList.add(new float[] {cx, y, z}); }

    private static void gemLine(World3D w, float cx, int z0, int z1) { for (int z = z0; z <= z1; z++) gem(w, cx, 0.7f, z + 0.5f); }

    private static void gemLine3(World3D w, float cx, float y, int z0, int z1) { for (int z = z0; z <= z1; z++) gem(w, cx, y, z + 0.5f); }

    /** Arco di gemme sopra un burrone da z0 a z1. */
    private static void arc(World3D w, float cx, int z0, int z1) {
        int n = z1 - z0 + 1;
        for (int i = 0; i < n; i++) { float u = (i + 0.5f) / n; gem(w, cx, 0.9f + 1.5f * 4f * u * (1f - u), z0 + i + 0.5f); }
    }

    private static void flight(World3D w, int z) {
        float[][] g = {{2.7f, 3.9f}, {5.3f, 5.2f}, {7.9f, 5.6f}, {10.5f, 4.5f}};
        for (float[] q : g) gem(w, 0, q[1], z + 0.5f + q[0]);
    }

    private static void padRow(World3D w, int z, int c0, int c1) {
        for (int c = c0; c <= c1; c++) w.springs.add(new float[] {c - World3D.HALF + 0.5f, z + 0.5f});
    }

    private static void thorn(World3D w, int xi, int z) { w.thorns.add(new float[] {xi - World3D.HALF + 0.5f, z + 0.5f}); }

    private static void jet(World3D w, int xi, int z, float period, float phase) { w.jets.add(new float[] {xi - World3D.HALF + 0.5f, z + 0.5f, period, phase}); }

    private static void log(World3D w, int z, float period, float phase) {
        pit(w, z, z, 4, 4);
        w.logs.add(new float[] {z + 0.5f, period, phase});
    }

    private static Enemy3 add(World3D w, int type, float x, float z, boolean alongZ, float lo, float hi, float y) {
        Enemy3 e = new Enemy3(x, z, alongZ, lo, hi);
        e.type = type; e.y = y; e.baseY = y; e.y0 = y;
        w.enemies.add(e);
        return e;
    }

    /** x è una colonna 0..4: le colonne dei livelli sono indicate da -2 a 2 nelle gemme e nei pattugliamenti, qui da 0 a 4. */
    private static float cx(int col) { return col - 2f; }

    private static void hog(World3D w, float x, float z, boolean alongZ, float lo, float hi) { add(w, Enemy3.HOG, x, z, alongZ, lo, hi, 0); }

    private static void acorn(World3D w, float x, float z) { add(w, Enemy3.ACORN, x, z, false, -2f, 2f, 0); }

    private static void crow(World3D w, float x, float z, boolean alongZ, float lo, float hi, float y) { add(w, Enemy3.CROW, x, z, alongZ, lo, hi, y); }

    private static void storm(World3D w, float x, float z, boolean alongZ, float lo, float hi, float y) { add(w, Enemy3.STORM, x, z, alongZ, lo, hi, y); }

    private static void whirl(World3D w, float x, float z, boolean alongZ, float lo, float hi) { add(w, Enemy3.WHIRL, x, z, alongZ, lo, hi, 0); }



    private static void snail(World3D w, int col, int z, int z0, int z1) { snail(w, col, z, z0, z1, 0f); }

    private static void snail(World3D w, int col, int z, int z0, int z1, float y) { add(w, Enemy3.SNAIL, cx(col), z + 0.5f, true, z0 + 0.5f, z1 + 0.5f, y); }

    private static void chest(World3D w, int col, int z, float y) { add(w, Enemy3.CHESTNUT, cx(col), z + 0.5f, true, z, z, y); }

    private static void owl(World3D w, int col, int z0, int z1, float y) { add(w, Enemy3.OWL, cx(col), (z0 + z1) / 2f, true, z0 + 0.5f, z1 + 0.5f, y); }

    /** Pinguino che scivola di traverso nella corsia, tra le colonne lo e hi. */
    private static void peng(World3D w, int z, float lo, float hi) { add(w, Enemy3.PENGUIN, 0, z + 0.5f, false, lo, hi, 0); }

    /** Pinguino che scivola lungo il corridoio tra z0 e z1 nella colonna col. */
    private static void peng2(World3D w, int col, int z0, int z1) { add(w, Enemy3.PENGUIN, cx(col), (z0 + z1) / 2f + 0.5f, true, z0 + 0.5f, z1 + 0.5f, 0); }

    private static void snowman(World3D w, int col, int z) { snowman(w, col, z, 0f); }

    private static void snowman(World3D w, int col, int z, float y) { add(w, Enemy3.SNOWMAN, cx(col), z + 0.5f, false, cx(col), cx(col), y); }

    private static void lap(World3D w, int col, int z, float period, float height) { add(w, Enemy3.LAPILLO, cx(col), z + 0.5f, false, period, height, 0); }

    private static void sala(World3D w, int col, int z, int z0, int z1) { sala(w, col, z, z0, z1, 0f); }

    private static void sala(World3D w, int col, int z, int z0, int z1, float y) { add(w, Enemy3.SALAMANDER, cx(col), z + 0.5f, true, z0 + 0.5f, z1 + 0.5f, y); }

    private static void frog(World3D w, int col, int z) { add(w, Enemy3.FROG, cx(col), z + 0.5f, false, -2f, 2f, 0); }

    private static void plant(World3D w, int col, int z) { plant(w, col, z, 0f); }

    private static void plant(World3D w, int col, int z, float y) { add(w, Enemy3.PLANT, cx(col), z + 0.5f, false, cx(col), cx(col), y); }

    /** Stella cadente: parte da (x, y, z) e si muove con velocità (vx, vy); lo si dà in colonne -4..4. */
    private static void star(World3D w, float x, float y, float z, float vx, float vy) { add(w, Enemy3.STAR, x, z, false, vx, vy, y); }

    private static void sun(World3D w, int col, float y, float z, float r, float speed) { sun(w, cx(col), y, z, r, speed); }

    private static void sun(World3D w, float x, float y, float z, float r, float speed) {
        Enemy3 e = add(w, Enemy3.SUNNY, x, z, false, speed, r, y);
        e.x0 = x; e.y0 = y;
    }
}
