package com.valeriodipaolo.platformgame.core;

/**
 * Terza serie di livelli 2D (25, 27, 29, 31, 33). Ognuno è costruito attorno a un'unica idea, presentata in quattro tempi:
 * la si impara al sicuro, la si sviluppa, arriva un colpo di scena che la rovescia, e alla fine un esame che mette tutto insieme.
 * Le gemme disegnano sempre la traiettoria giusta; la chiave sta sulla strada più bella, mai su quella obbligata.
 */
public final class LevelsC {
    private LevelsC() {}

    private static final int G = LevelsB.G;

    public static Level build(int id) {
        switch (id) {
            case 27: return snowFort();
            case 29: return twoRoads();
            case 31: return chestnutRain();
            case 33: return skyStair();
            default: return springValley();
        }
    }

    // ------------------------------------------------------------------ 25: la valle delle molle
    // Idea: il fiore a molla. Prima lancia sul prato, dove cadere non costa nulla; poi sopra i fiumi; poi porta su mensole alte che
    // evitano i pericoli di sotto; infine due fiori in fila che rimbalzano il robot sopra un campo di rovi.
    private static Level springValley() {
        LevelsB b = new LevelsB(372, 25, 4);
        b.g(0, 36); b.g(47, 94); b.g(105, 144); b.g(150, 196); b.g(207, 212); b.g(219, 262); b.g(273, 318); b.g(324, 371);
        // 1. il prato: il primo fiore è sul piano, le gemme mostrano il volo e si atterra sull'erba
        b.sh(5, 9, 3); b.row(9, 6, "B?B"); b.pad(15); b.flight(15); b.en(30, Enemy.FROG);
        // primo fiume: stesso volo, ma ora sotto c'è l'acqua
        b.pad(36); b.flight(36);
        // 2. la mensola: il fiore porta su una mensola di pietra con le gemme e il mattone segreto; sotto, il riccio a piedi
        b.sh(49, 9, 2); b.pad(53); b.plat(61, 4, 6); b.sh(61, 3, 6); b.row(62, 0, "BHB"); b.en(64, 9, Enemy.HOG, 57, 71);
        b.row(74, 6, "?B?"); b.en(77, Enemy.PLANT); b.sh(82, 9, 3); b.en(87, Enemy.FROG);
        // secondo fiume: un corvo fa la ronda sotto la traiettoria, punisce solo chi salta corto
        b.pad(94); b.flight(94); b.crow(100, 5.5f, 96, 104);
        // la mensola come scelta: sotto ci sono i rovi, sopra le gemme
        b.sh(107, 9, 2); b.en(113, Enemy.SNAIL, 108, 119); b.row(114, 6, "B?B?B"); b.pad(124); b.plat(132, 4, 8); b.sh(132, 3, 8);
        b.thorn(133); b.thorn(136);
        // ruscello corto: le rane aspettano sull'altra sponda
        b.arc(145, 5); b.en(154, Enemy.FROG);
        b.hill(158, 168, 3); b.en(163, Enemy.PLANT); b.sh(160, 5, 2); b.sh(165, 5, 2); b.en(174, Enemy.HOG, 170, 180);
        b.col(183, 2); b.col(187, 3); b.gem(183, 6); b.gem(187, 5);
        // 3. il colpo di scena: il fiore non porta più sulla terraferma ma su un isolotto, e da lì si salta di nuovo
        b.pad(196); b.flight(196); b.sh(208, 9, 3); b.arc(213, 6);
        // due fiori in fila sopra un campo di rovi: si atterra proprio davanti al secondo e si rimbalza ancora
        b.sh(221, 9, 2); b.pad(224); b.thorn(229); b.thorn(232); b.pad(237); b.thorn(241); b.thorn(244);
        b.flight(224); b.flight(237);
        b.row(253, 6, "?B?"); b.en(256, Enemy.PLANT);
        b.pad(262); b.flight(262); b.crow(267, 6f, 264, 272);
        // 4. l'esame: la chiave è sulla mensola alta, raggiungibile solo dal fiore; sotto una lumaca pattuglia il passaggio
        b.sh(275, 9, 2); b.pad(282); b.plat(290, 4, 5); LevelsB.lvBattery(b, 292.5f, 2.6f); b.gem(290, 3); b.gem(294, 3);
        b.en(294, 9, Enemy.SNAIL, 287, 300); b.en(304, Enemy.FROG); b.thorn(309); b.en(314, Enemy.PLANT);
        b.arc(319, 5); b.en(329, Enemy.FROG);
        b.row(333, 6, "B?B"); b.en(338, Enemy.HOG, 334, 344); b.pad(343); b.flight(343);
        b.lv.flagX = 364; b.lv.flagBaseY = G;
        return b.lv;
    }

    // ------------------------------------------------------------------ 27: il fortino dei pupazzi
    // Idea: i pupazzi di neve tirano palle in linea retta. Si avanza da un riparo all'altro: muretti, avvallamenti, casse.
    // Prima un pupazzo solo e un muretto, poi i pinguini che spingono fuori dal riparo, poi i pupazzi in alto che tirano oltre i muri,
    // infine il fortino con tre torri.
    private static Level snowFort() {
        LevelsB b = new LevelsB(380, 27, 2);
        b.g(0, 40); b.g(45, 92); b.g(98, 150); b.g(156, 200); b.g(206, 252); b.g(258, 262); b.g(268, 318); b.g(324, 379);
        // 1. il primo pupazzo, lontano, e un muretto davanti: ci si ripara, si conta il tiro, si scatta
        b.sh(5, 9, 3); b.row(9, 6, "B?B"); b.col(18, 2); b.sh(13, 9, 3); b.en(30, Enemy.SNOWMAN); b.sh(21, 9, 3); b.row(24, 6, "?");
        b.col(35, 1); b.en(38, Enemy.ACORN);
        // primo crepaccio, e dall'altra parte un riparo subito
        b.arc(41, 4); b.col(48, 2); b.en(58, Enemy.SNOWMAN); b.sh(51, 9, 4);
        // l'avvallamento: due dossi fanno da riparo, il pupazzo sta tra i due
        b.hill(62, 68, 2); b.en(72, Enemy.SNOWMAN); b.hill(76, 82, 2); b.gem(65, 6); b.gem(79, 6); b.row(70, 5, "B?B");
        b.en(87, Enemy.PENGUIN, 84, 92);
        // crepaccio con un lastrone
        b.plat(95, 8, 1); b.gem(95, 6);
        // 2. i pinguini: scivolano avanti e indietro proprio dove ci si ripara, e costringono a uscire allo scoperto
        b.col(102, 2); b.en(108, Enemy.PENGUIN, 103, 114); b.col(115, 2); b.en(124, Enemy.SNOWMAN); b.row(107, 6, "?B?"); b.sh(105, 9, 2);
        b.col(129, 2); b.en(134, Enemy.PENGUIN, 130, 140); b.col(141, 2); b.en(147, Enemy.HOG, 143, 150);
        // crepaccio largo sei, un pupazzo sul ciglio opposto
        b.arc(151, 5); b.en(161, Enemy.SNOWMAN); b.col(158, 1);
        // la galleria: un tetto di ghiaccio protegge dai tiri dall'alto, ma dentro scivola un pinguino
        b.plat(166, 6, 12); b.en(171, 9, Enemy.PENGUIN, 166, 177); b.sh(167, 9, 10); b.en(172, 5, Enemy.SNOWMAN, 0, 0); b.gem(168, 4); b.gem(176, 4);
        b.en(184, Enemy.ACORN); b.row(188, 6, "B?B"); b.en(194, Enemy.PENGUIN, 190, 199);
        // crepaccio con due lastre
        b.plat(202, 8, 1); b.plat(204, 7, 1); b.gem(202, 6); b.gem(204, 5);
        // 3. il colpo di scena: i pupazzi stanno in alto, sulle torri, e tirano sopra i muretti; il riparo ora è sotto le mensole
        b.up(210, 3); b.plat(213, 7, 3); b.en(214, 6, Enemy.SNOWMAN, 0, 0); b.down(216, 3); b.sh(220, 9, 3);
        b.plat(226, 6, 6); b.sh(227, 9, 4); b.en(236, Enemy.SNOWMAN); b.plat(240, 6, 6); b.sh(241, 9, 4); b.en(250, Enemy.PENGUIN, 247, 252);
        b.row(228, 3, "?B?"); b.row(242, 3, "BHB");
        // due crepacci con un isolotto: un pinguino ci scivola sopra
        b.en(260, Enemy.PENGUIN, 258, 262); b.gem(255.5f, 6); b.gem(265.5f, 6);
        // 4. il fortino: tre torri di pietra, un pupazzo su ognuna; la chiave è sulla torre di mezzo, sotto il suo pupazzo
        b.sh(270, 9, 3); b.col(276, 3); b.en(276, 6, Enemy.SNOWMAN, 0, 0); b.en(281, Enemy.PENGUIN, 278, 286);
        b.up(288, 3); b.plat(291, 6, 4); b.down(295, 3); b.en(293, 5, Enemy.SNOWMAN, 0, 0); LevelsB.lvBattery(b, 291.5f, 4.4f);
        b.en(302, Enemy.PENGUIN, 299, 308); b.col(310, 4); b.en(310, 5, Enemy.SNOWMAN, 0, 0); b.gem(306, 6);
        b.arc(319, 5); b.en(330, Enemy.SNOWMAN); b.col(327, 2);
        b.row(336, 6, "B?B"); b.en(342, Enemy.PENGUIN, 336, 348); b.col(350, 2); b.sh(345, 9, 3);
        b.lv.flagX = 372; b.lv.flagBaseY = G;
        return b.lv;
    }

    // ------------------------------------------------------------------ 29: le due strade
    // Idea: per tutto il livello c'è una strada alta (mattoni e mensole, poche gemme ma ben in vista) e una bassa (galleria, più gemme
    // ma più nemici). Ogni bivio è chiaro e le due strade si ritrovano sempre. A metà il colpo di scena: la strada bassa finisce in un
    // burrone e si deve tornare su; alla fine la chiave sta in alto e si raggiunge solo saltando dalla strada alta.
    private static Level twoRoads() {
        LevelsB b = new LevelsB(376, 29, 0);
        b.g(0, 44); b.g(50, 104); b.g(110, 158); b.g(170, 214); b.g(220, 268); b.g(274, 322); b.g(328, 375);
        // 1. il primo bivio, senza pericoli: una scala porta sui mattoni, sotto si cammina
        b.sh(5, 9, 3); b.up(11, 3); b.plat(15, 7, 9); b.sh(15, 6, 9); b.sh(15, 9, 9); b.en(20, 9, Enemy.BEETLE, 0, 0); b.down(25, 3);
        b.row(32, 6, "?B?"); b.en(38, Enemy.SNAIL, 33, 43);
        b.arc(45, 4);
        // 2. la galleria lunga: sopra il tetto un corvo e la strada delle casse, sotto i ricci a guardia delle gemme
        b.up(52, 4); b.plat(57, 6, 18); b.down(76, 4); b.row(62, 2, "B?B"); b.row(70, 2, "?B?"); b.crow(66, 3.5f, 58, 76);
        b.sh(58, 5, 3); b.sh(73, 5, 3);
        b.sh(57, 9, 18); b.en(63, 9, Enemy.HOG, 57, 66); b.en(71, 9, Enemy.HOG, 67, 75);
        b.en(84, Enemy.ACORN); b.hill(88, 96, 3); b.en(92, Enemy.SNAIL, 89, 95); b.sh(90, 5, 5); b.en(101, Enemy.BEETLE);
        b.arc(105, 5);
        // le colonne alte e il passaggio sotto: in alto si saltella da cima a cima, in basso c'è un riccio per ogni spazio
        b.col(114, 4); b.col(118, 4); b.col(122, 4); b.col(126, 4); b.col(130, 4); b.gem(114, 5); b.gem(118, 5); b.gem(122, 5); b.gem(126, 5); b.gem(130, 5);
        b.en(116, Enemy.HOG, 115, 117); b.en(124, Enemy.HOG, 123, 125); b.sh(119, 9, 3); b.sh(127, 9, 3);
        b.row(137, 6, "B?B"); b.en(142, Enemy.SNAIL, 136, 150); b.en(150, Enemy.ACORN);
        // 3. il colpo di scena: niente più strada bassa, sotto c'è un burrone di undici caselle; si passa solo dal ponte, con il corvo
        b.up(152, 3); b.plat(155, 7, 18); b.sh(155, 6, 18); b.crow(164, 4.5f, 156, 172); b.sh(148, 9, 3);
        b.down(173, 3); b.en(180, Enemy.BEETLE); b.row(184, 6, "?B?"); b.en(190, Enemy.HOG, 186, 196); b.hill(198, 206, 3); b.en(202, Enemy.ACORN);
        b.sh(200, 5, 5); b.arc(215, 4);
        // di nuovo due strade, ma ora la bassa ha la scorciatoia e l'alta il mattone segreto
        b.up(222, 4); b.plat(227, 6, 14); b.down(242, 4); b.row(232, 2, "BHB"); b.sh(228, 5, 3); b.sh(237, 5, 3); b.crow(234, 3.5f, 227, 241);
        b.sh(227, 9, 15); b.en(233, 9, Enemy.SNAIL, 226, 241); b.en(238, 9, Enemy.BEETLE, 0, 0);
        b.col(252, 2); b.en(257, Enemy.HOG, 253, 262); b.col(263, 2); b.gem(252, 6); b.gem(263, 6);
        b.arc(269, 5);
        // 4. la torre della chiave: due piani di mensole, la chiave in cima; si sale saltando di piano in piano
        b.sh(276, 9, 2); b.plat(282, 7, 5); b.plat(289, 4, 5); b.plat(296, 1, 4); LevelsB.lvBattery(b, 297.5f, 0f); b.gem(283, 6); b.gem(285, 6);
        b.gem(290, 3); b.gem(292, 3); b.en(287, Enemy.SNAIL, 280, 300); b.en(291, 3, Enemy.SNAIL, 289, 293); b.crow(294, 2.5f, 286, 304);
        b.en(306, Enemy.HOG, 302, 312); b.row(310, 6, "?B?"); b.en(318, Enemy.ACORN);
        b.arc(323, 5);
        b.up(330, 3); b.plat(334, 7, 8); b.down(343, 3); b.sh(334, 6, 8); b.sh(334, 9, 9); b.en(338, 9, Enemy.HOG, 333, 342); b.en(350, Enemy.SNAIL, 347, 356);
        b.lv.flagX = 368; b.lv.flagBaseY = G;
        return b.lv;
    }

    // ------------------------------------------------------------------ 31: la pioggia di ricci
    // Idea: i ricci di castagna pendono sotto mensole e casse e cadono quando ci si passa sotto. Prima uno alla volta, poi in fila
    // (si passa con un ritmo: corri, fermati, corri), poi sotto un tetto basso dove saltare non serve, infine con i gufi che scendono.
    private static Level chestnutRain() {
        LevelsB b = new LevelsB(370, 31, 1);
        b.g(0, 42); b.g(47, 100); b.g(106, 152); b.g(158, 206); b.g(212, 258); b.g(264, 310); b.g(316, 369);
        // 1. un riccio solo, appeso a una cassa alta: lo si vede, lo si fa cadere, lo si scavalca
        b.sh(5, 9, 3); b.row(14, 5, "B?B"); b.chest(15, 6); b.sh(11, 9, 2); b.sh(18, 9, 2); b.en(26, Enemy.BEETLE);
        b.row(32, 5, "?B?"); b.chest(33, 6); b.en(38, Enemy.HOG, 29, 41);
        b.arc(43, 4);
        // 2. il filare: quattro ricci sotto la stessa mensola, distanziati; si passa uno spazio per volta
        b.plat(52, 5, 18); b.chest(54, 6); b.chest(58, 6); b.chest(62, 6); b.chest(66, 6); b.gem(56, 9); b.gem(60, 9); b.gem(64, 9); b.gem(68, 9);
        b.sh(53, 4, 16); b.en(76, Enemy.ACORN); b.hill(80, 90, 3); b.en(85, Enemy.HOG, 82, 88); b.sh(82, 5, 6); b.en(95, Enemy.BEETLE);
        b.owl(103, 4f, 98, 108);
        b.arc(101, 5);
        // il gufo: picchia su chi si ferma; qui ci si deve fermare per i ricci, e allora bisogna farlo nel punto giusto
        b.plat(112, 5, 14); b.chest(114, 6); b.chest(119, 6); b.chest(124, 6); b.owl(119, 2.5f, 112, 126); b.sh(113, 4, 12);
        b.gem(116.5f, 9); b.gem(121.5f, 9);
        b.row(132, 6, "B?B"); b.en(138, Enemy.HOG, 130, 146); b.en(148, Enemy.ACORN);
        b.arc(153, 5);
        // 3. il colpo di scena: la galleria scavata nella roccia; il tetto è basso, non c'è una strada sopra e i ricci cadono in fila
        for (int y = 0; y <= 6; y++) b.plat(164, y, 20);
        for (int x = 167; x <= 181; x += 4) b.chest(x, 7);
        b.gem(169, 9); b.gem(173, 9); b.gem(177, 9); b.gem(181, 9); b.en(190, Enemy.HOG, 186, 196); b.row(194, 6, "?B?"); b.en(201, Enemy.ACORN);
        b.plat(209, 8, 1); b.gem(209, 6);
        // il viale delle casse: un riccio sotto ogni mattone, e i punti interrogativi in mezzo da colpire al volo
        b.sh(214, 9, 2); b.row(220, 5, "B?B?B?B"); b.chest(220, 6); b.chest(222, 6); b.chest(224, 6); b.chest(226, 6); b.hill(230, 238, 2); b.row(233, 4, "BHB");
        b.sh(231, 6, 7); b.en(242, Enemy.BEETLE); b.owl(248, 3.5f, 240, 256); b.en(252, Enemy.HOG, 246, 257);
        b.arc(259, 5);
        // 4. l'esame: la mensola della chiave, con tre ricci sotto e due gufi; la chiave è in cima, sopra i ricci
        b.plat(268, 5, 16); b.chest(270, 6); b.chest(275, 6); b.chest(280, 6); LevelsB.lvBattery(b, 276f, 3.3f); b.sh(269, 4, 4); b.sh(279, 4, 4);
        b.owl(273, 2.5f, 266, 280); b.owl(286, 3f, 280, 294); b.gem(272.5f, 9); b.gem(277.5f, 9);
        b.en(292, Enemy.ACORN); b.row(297, 6, "B?B"); b.chest(298, 7); b.en(305, Enemy.HOG, 300, 310);
        b.arc(311, 5);
        b.plat(322, 5, 12); b.chest(324, 6); b.chest(328, 6); b.chest(332, 6); b.sh(322, 4, 12); b.owl(340, 3.5f, 334, 348); b.en(345, Enemy.BEETLE);
        b.lv.flagX = 362; b.lv.flagBaseY = G;
        return b.lv;
    }

    // ------------------------------------------------------------------ 33: la scala celeste
    // Idea: il gran finale sale in cielo e mette insieme tutto: nuvole che cedono, fiori a molla, stelle cadenti, solleoni, mulinelli.
    // Ogni tratto prende un'idea già vista e la combina con un'altra; la chiave è in cima alla scala di nuvole.
    private static Level skyStair() {
        LevelsB b = new LevelsB(392, 33, 5);
        b.g(0, 34); b.g(70, 110); b.g(121, 160); b.g(186, 226); b.g(232, 234); b.g(240, 284); b.g(291, 327); b.g(356, 391);
        // 1. ripasso: le nuvole che cedono, una stella che taglia la strada
        b.sh(5, 9, 3); b.row(10, 6, "B?B"); b.sun(11, 5.5f, 1.6f, 2.4f); b.whirl(22, 18, 30);
        b.bridge(38, new int[] {9, 8, 7, 8, 9, 8}); b.star(50, -1.5f); b.star(63, -2f);
        // 2. molla e nuvole: il fiore lancia sopra un vuoto lungo, ma l'arrivo è una nuvola che cede: non fermarsi
        b.sh(72, 9, 2); b.en(78, Enemy.BEETLE); b.col(84, 2); b.col(88, 3); b.sun(88, 4.6f, 1.5f, 2.6f); b.gem(84, 6); b.gem(88, 5);
        b.row(95, 6, "?B?"); b.storm(100, 2.6f, 92, 108); b.pad(110); b.flight(110);
        b.sh(123, 9, 2); b.whirl(130, 125, 138); b.hill(140, 150, 3); b.sh(142, 5, 6); b.sun(145, 4f, 1.6f, -2.4f);
        b.pad(160); b.flight(160); b.cr(171, 9, 3); b.cr(176, 8, 3); b.cr(181, 9, 3); b.star(176, -2.5f); b.gem(177, 6);
        // 3. il colpo di scena: la scala di nuvole sale invece di attraversare; in cima la chiave, attorno due solleoni
        b.sh(188, 9, 2); b.cr(194, 7, 2); b.cr(198, 5, 2); b.cr(202, 3, 2); b.cr(206, 5, 2); b.cr(210, 7, 2);
        b.gem(194, 5); b.gem(198, 3); b.gem(206, 3); b.gem(210, 5); LevelsB.lvBattery(b, 203f, 1.4f); b.sun(198.5f, 2f, 1.3f, 2.4f); b.sun(206.5f, 2f, 1.3f, -2.4f);
        b.sh(214, 9, 3); b.storm(220, 2.4f, 212, 228); b.row(220, 6, "B?B");
        b.en(233, Enemy.BEETLE, 232, 234); b.gem(229.5f, 6); b.gem(237.5f, 6); b.star(236, -1f);
        // le colonne di marmo con i solleoni e il mattone segreto
        b.col(246, 2); b.col(250, 3); b.col(254, 4); b.col(258, 3); b.col(262, 2); b.sun(250, 5f, 1.4f, 2.6f); b.sun(258, 5f, 1.4f, -2.6f);
        b.gem(246, 6); b.gem(250, 5); b.gem(254, 4); b.gem(258, 5); b.gem(262, 6); b.row(253, 1, "BHB"); b.whirl(270, 265, 278);
        b.arc(285, 6); b.star(292, -2f);
        // 4. l'esame finale: molla, ponte di nuvole, stelle e solleone tutti insieme
        b.sh(293, 9, 2); b.en(299, Enemy.BEETLE); b.row(304, 6, "?B?"); b.sun(305, 5f, 1.8f, 2.4f); b.whirl(314, 308, 322); b.storm(318, 2.4f, 310, 328);
        b.bridge(331, new int[] {9, 8, 7, 8, 9}); b.star(340, -2f); b.star(349, -1f); b.sun(343, 4f, 1.4f, 2.6f);
        b.sh(358, 9, 2); b.row(364, 6, "B?B"); b.sun(365, 5f, 1.6f, -2.4f); b.whirl(372, 368, 380);
        b.lv.flagX = 384; b.lv.flagBaseY = G;
        return b.lv;
    }
}
