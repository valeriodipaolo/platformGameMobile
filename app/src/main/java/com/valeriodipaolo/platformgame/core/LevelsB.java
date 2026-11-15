package com.valeriodipaolo.platformgame.core;

/**
 * Seconda serie di livelli 2D (13, 15, 17, 19, 21, 23): stesso aspetto delle controparti (1, 3, 5, 7, 9, 11), lunghi il doppio e più
 * impegnativi. Ogni livello ha due nemici solo suoi e ogni burrone ha un motivo: un nemico che lo sorveglia, un appiglio da conquistare,
 * un atterraggio da scegliere. Il terreno base occupa le righe 10 e 11.
 */
public final class LevelsB {
    private static final int H = 12, G = 10;
    private final Level lv;

    private LevelsB(int w, int id, int theme) {
        lv = new Level(w, H);
        lv.id = "l" + id;
        lv.theme = theme;
        lv.startX = 3;
        lv.startY = G - 1.2f;
    }

    public static Level build(int id) {
        switch (id) {
            case 15: return autumn();
            case 17: return winter();
            case 19: return fire();
            case 21: return spring();
            case 23: return heaven();
            default: return summer();
        }
    }

    // ------------------------------------------------------------------ 13: il prato d'estate — talpe e lumache
    private static Level summer() {
        LevelsB b = new LevelsB(486, 13, 0);
        b.g(0, 40); b.g(45, 80); b.g(86, 130); b.g(136, 180); b.g(184, 187); b.g(191, 240); b.g(248, 300); b.g(306, 362);
        b.g(367, 369); b.g(374, 430); b.g(436, 485);
        // prato d'ingresso: la prima lumaca insegna che saltarle sopra la chiude solo nel guscio
        b.sh(6, 9, 4); b.row(10, 6, "B?B"); b.en(18, Enemy.SNAIL); b.sh(22, 7, 3); b.en(30, Enemy.BEETLE); b.en(36, Enemy.SNAIL, 32, 39);
        // primo burrone, corto: dall'altra parte una talpa sbuca proprio dove si atterra di slancio
        b.arc(41, 4); b.en(47, Enemy.MOLE);
        // il campo delle talpe: tre tane sotto una fila di casse; le gemme a terra stanno tra una tana e l'altra
        b.row(52, 6, "?B?B?"); b.en(52, Enemy.MOLE); b.en(56, Enemy.MOLE); b.sh(54, 9, 1); b.sh(58, 9, 2);
        b.hill(62, 72, 3); b.en(67, Enemy.BEETLE); b.sh(65, 5, 4); b.en(76, Enemy.SNAIL, 73, 79);
        // burrone largo sei con un solo masso sospeso come appiglio: sopra vola un corvo
        b.plat(83, 8, 1); b.gem(83, 6); b.crow(84, 4.5f, 80, 88);
        // altopiano e galleria: sopra una lumaca su un passaggio stretto, sotto due talpe a guardia delle gemme
        b.up(89, 3); b.plat(94, 6, 18); b.down(113, 3); b.en(100, 5, Enemy.SNAIL, 95, 110); b.row(101, 2, "B?B"); b.sh(96, 4, 3); b.sh(106, 4, 3);
        b.sh(95, 9, 16); b.en(98, Enemy.MOLE); b.en(106, Enemy.MOLE); b.en(120, Enemy.HOG); b.sh(123, 9, 3);
        // burrone di cinque e bosco di colonne: tra una colonna e l'altra un riccio, quindi si resta in alto
        b.arc(131, 5);
        b.col(140, 2); b.col(145, 3); b.col(150, 4); b.col(155, 3); b.col(160, 2);
        b.en(143, Enemy.HOG, 141, 144); b.en(148, Enemy.HOG, 146, 149); b.en(158, Enemy.HOG, 156, 159);
        b.gem(140, 6); b.gem(145, 5); b.gem(150, 4); b.gem(155, 5); b.gem(160, 6); b.crow(150, 2.4f, 141, 160);
        b.row(167, 6, "B?B"); b.en(172, Enemy.SNAIL, 164, 179); b.en(177, Enemy.BEETLE);
        // isolotto tra due burroni: la talpa obbliga a scegliere il momento del salto
        b.en(185, Enemy.MOLE); b.gem(182, 6); b.gem(189, 6);
        // il recinto: due lumache e uno scarabeo chiusi tra due colonne; in mezzo le casse e il mattone segreto
        b.sh(193, 9, 3); b.col(199, 2); b.col(216, 2); b.en(203, Enemy.SNAIL, 200, 215); b.en(211, Enemy.SNAIL, 200, 215); b.en(207, Enemy.BEETLE);
        b.row(204, 6, "B?H?B"); b.sh(205, 4, 3); b.hill(222, 232, 2); b.en(227, Enemy.ACORN); b.en(236, Enemy.MOLE);
        // burrone di sette con una mensola a metà: una talpa ci abita
        b.plat(243, 9, 3); b.en(244, 8, Enemy.MOLE, 0, 0); b.gem(241, 6); b.gem(246, 6);
        // tre dossi: in cima le ghiande, nelle conche (dove si atterra) le talpe
        b.hill(250, 258, 3); b.hill(263, 271, 4); b.hill(276, 284, 3); b.en(254, Enemy.ACORN); b.en(267, Enemy.ACORN); b.en(280, Enemy.ACORN);
        b.en(260, Enemy.MOLE); b.en(273, Enemy.MOLE); b.sh(252, 5, 5); b.sh(265, 4, 5); b.sh(278, 5, 5); b.row(290, 6, "?B?"); b.en(294, Enemy.SNAIL, 287, 299);
        // burrone di sei, poi la piramide: la cella energetica è sulle casse più alte, sorvegliate da un corvo
        b.arc(301, 5);
        b.up(312, 5); b.down(318, 5); b.plat(317, 5, 1); b.row(316, 1, "B?B"); lvBattery(b, 317, 3.6f); b.crow(317, 2.2f, 309, 326);
        b.en(309, Enemy.HOG, 307, 311); b.en(326, Enemy.MOLE); b.sh(328, 9, 4); b.en(336, Enemy.SNAIL, 330, 345); b.en(342, Enemy.BEETLE);
        b.row(338, 6, "B?B"); b.col(348, 2); b.col(352, 3); b.en(356, Enemy.MOLE); b.gem(348, 6); b.gem(352, 5);
        // due burroni e un isolotto stretto, con un corvo che fa la spola
        b.crow(368, 5f, 362, 375); b.gem(368, 7); b.gem(364.5f, 6); b.gem(371.5f, 6);
        // l'ultima corsa: talpa, lumaca, riccio, talpa, separati da muretti da scavalcare
        b.en(378, Enemy.MOLE); b.col(382, 2); b.en(386, Enemy.SNAIL, 383, 391); b.col(392, 2); b.en(397, Enemy.HOG, 393, 401); b.col(402, 3);
        b.en(406, Enemy.MOLE); b.en(410, Enemy.MOLE); b.sh(385, 9, 5); b.sh(395, 6, 3); b.sh(404, 6, 6); b.row(415, 6, "B?B?B"); b.en(420, Enemy.SNAIL, 413, 429);
        b.en(425, Enemy.ACORN);
        // ultimo burrone: sei caselle, una talpa sull'altra sponda
        b.arc(431, 5); b.en(439, Enemy.MOLE);
        b.sh(444, 9, 5); b.row(452, 6, "B?B"); b.en(458, Enemy.SNAIL, 450, 468); b.en(464, Enemy.BEETLE);
        b.lv.flagX = 478; b.lv.flagBaseY = G;
        return b.lv;
    }

    private static void lvBattery(LevelsB b, float x, float y) {
        b.lv.battery = new float[] {x, y};
    }

    // ------------------------------------------------------------------ 15: il prato d'autunno — ricci di castagna e gufi
    private static Level autumn() {
        LevelsB b = new LevelsB(490, 15, 1);
        b.g(0, 34); b.g(39, 70); b.g(76, 120); b.g(126, 160); b.g(168, 215); b.g(221, 224); b.g(230, 233); b.g(239, 290); b.g(296, 345);
        b.g(351, 400); b.g(406, 440); b.g(446, 489);
        // viale d'ingresso: i ricci di castagna pendono sotto le casse, proprio sopra le gemme a terra
        b.sh(6, 9, 3); b.row(11, 6, "B?B"); b.chest(12, 7); b.sh(11, 9, 3); b.en(19, Enemy.ACORN); b.row(24, 6, "?B?"); b.chest(26, 7); b.en(30, Enemy.HOG, 22, 33);
        // primo burrone: un gufo lo sorvola e picchia su chi esita sul bordo
        b.arc(35, 4); b.owl(37, 4f, 32, 42);
        b.hill(44, 54, 3); b.en(49, Enemy.ACORN); b.sh(46, 5, 6); b.chest(58, 5); b.chest(62, 5); b.plat(57, 4, 7); b.sh(58, 2, 5); b.en(66, Enemy.BEETLE);
        // burrone di sei con due appigli: sopra ognuno una castagna pronta a cadere
        b.plat(72, 8, 1); b.plat(75, 7, 1); b.chest(72, 4.5f); b.gem(72, 6); b.gem(75, 5);
        // la scalinata e il ponte di mattoni: i gufi difendono la cella energetica in cima
        b.up(80, 4); b.plat(84, 6, 12); b.down(97, 4); b.row(88, 2, "B?B"); b.owl(90, 0.8f, 84, 96); b.sh(85, 4, 3); b.sh(92, 4, 3);
        b.sh(85, 9, 11); b.en(88, Enemy.HOG, 85, 95); b.en(93, Enemy.ACORN); b.chest(104, 6); b.chest(109, 6); b.plat(103, 5, 8); b.sh(104, 3, 6);
        b.sh(104, 9, 6); b.en(115, Enemy.BEETLE); b.en(118, Enemy.HOG, 112, 119);
        // burrone di cinque: sull'altra sponda il castagno lascia cadere due ricci in fila
        b.arc(121, 5); b.chest(128, 5.5f); b.chest(132, 5.5f); b.plat(127, 4, 7);
        b.col(138, 3); b.col(143, 4); b.col(148, 3); b.gem(138, 5); b.gem(143, 4); b.gem(148, 5); b.owl(143, 1.5f, 137, 150); b.en(141, Enemy.HOG, 139, 142);
        b.en(146, Enemy.HOG, 144, 147); b.row(153, 6, "B?B"); b.en(157, Enemy.ACORN);
        // burrone largo sette: una mensola nel mezzo, e il gufo che fa la ronda proprio lì sopra
        b.plat(163, 9, 2); b.owl(164, 4.5f, 159, 169); b.gem(161, 6); b.gem(166, 6);
        // il bosco fitto: file di casse basse con le castagne appese; si passa chinandosi tra una caduta e l'altra
        b.sh(170, 9, 3); b.row(175, 6, "B?B?B"); b.chest(176, 7); b.chest(178, 7); b.en(183, Enemy.ACORN); b.hill(186, 196, 4); b.en(191, Enemy.HOG, 189, 194);
        b.sh(188, 4, 6); b.row(200, 6, "?B?"); b.chest(201, 7); b.en(206, Enemy.BEETLE); b.en(210, Enemy.ACORN); b.owl(205, 3.2f, 198, 214);
        // l'arcipelago: due isolotti di quattro caselle; sul primo una ghianda, sul secondo cade una castagna
        b.en(222, Enemy.ACORN, 221, 224); b.plat(230, 5, 4); b.chest(231, 6); b.gem(217.5f, 6); b.gem(226.5f, 6); b.gem(235.5f, 6); b.sh(230, 3, 4);
        b.owl(227, 3.4f, 219, 236);
        // radura: mattone segreto in mezzo ai ricci
        b.sh(241, 9, 4); b.col(247, 2); b.en(251, Enemy.HOG, 248, 256); b.col(257, 2); b.row(250, 5, "B?H?B"); b.sh(250, 3, 5); b.chest(262, 6); b.chest(266, 6);
        b.chest(270, 6); b.plat(261, 5, 11); b.sh(262, 3, 9); b.sh(262, 9, 9); b.en(276, Enemy.ACORN); b.hill(279, 288, 3); b.en(284, Enemy.BEETLE);
        // burrone di sei, con un gufo; poi la torre a gradoni
        b.arc(291, 5); b.owl(293, 4.5f, 288, 299);
        b.up(302, 5); b.plat(307, 5, 6); b.down(313, 5); b.owl(310, 0.6f, 303, 317); b.sh(307, 3, 6); b.row(308, 1, "?B?"); b.en(322, Enemy.HOG, 319, 327);
        b.chest(330, 6); b.chest(334, 6); b.chest(338, 6); b.plat(329, 5, 11); b.sh(330, 3, 9); b.sh(330, 9, 9); b.en(342, Enemy.ACORN);
        // burrone di sei con un appiglio e una castagna sopra
        b.plat(348, 8, 2); b.chest(349, 4.5f); b.gem(348.5f, 6);
        b.sh(354, 9, 3); b.col(360, 3); b.col(364, 4); b.col(368, 4); b.col(372, 4); b.col(376, 3); b.owl(368, 0.6f, 360, 377); b.gem(360, 5); b.gem(364, 4);
        b.gem(368, 3); b.gem(372, 4); b.gem(376, 5); b.en(362, Enemy.HOG, 361, 363); b.en(374, Enemy.HOG, 373, 375); lvBattery(b, 368, 1.6f);
        b.row(383, 6, "B?B"); b.chest(384, 7); b.en(389, Enemy.ACORN); b.en(395, Enemy.BEETLE);
        // burrone di sei: due gufi in controtempo
        b.arc(401, 5); b.owl(402, 4f, 398, 406); b.owl(408, 3f, 404, 412);
        b.hill(410, 420, 3); b.en(415, Enemy.ACORN); b.chest(424, 6); b.chest(428, 6); b.chest(432, 6); b.plat(423, 5, 11); b.sh(424, 3, 9); b.sh(424, 9, 9);
        b.en(436, Enemy.HOG, 430, 439);
        // ultimo burrone e viale finale
        b.arc(441, 5); b.chest(448, 5.5f); b.plat(447, 4, 3);
        b.sh(452, 9, 4); b.row(458, 6, "B?B"); b.en(463, Enemy.ACORN); b.owl(466, 3.4f, 456, 474); b.en(470, Enemy.HOG, 462, 475);
        b.lv.flagX = 482; b.lv.flagBaseY = G;
        return b.lv;
    }

    // ------------------------------------------------------------------ 17: la tundra boreale — pinguini e pupazzi di neve
    private static Level winter() {
        LevelsB b = new LevelsB(494, 17, 2);
        b.g(0, 36); b.g(41, 76); b.g(82, 128); b.g(134, 176); b.g(182, 226); b.g(232, 236); b.g(242, 292); b.g(298, 346); b.g(352, 402);
        b.g(408, 446); b.g(452, 493);
        // pianura innevata: il primo pinguino scivola avanti e indietro tra due massi; si salta a tempo
        b.sh(6, 9, 3); b.col(10, 1); b.col(28, 1); b.en(18, Enemy.PENGUIN, 11, 27); b.row(17, 6, "B?B"); b.sh(13, 7, 2); b.sh(22, 7, 2); b.en(33, Enemy.ACORN);
        // primo burrone: sull'altra sponda un pupazzo di neve tira a chi sta per saltare
        b.arc(37, 4); b.en(46, Enemy.SNOWMAN);
        b.sh(50, 9, 3); b.hill(54, 64, 3); b.en(59, Enemy.HOG, 57, 62); b.sh(56, 5, 6); b.row(68, 6, "?B?"); b.en(72, Enemy.PENGUIN, 66, 75);
        // burrone di sei con un mattone di ghiaccio come appiglio: il pupazzo aspetta in cima ai gradoni, più in alto di chi salta
        b.plat(79, 8, 1); b.gem(79, 6); b.up(85, 3); b.en(88, 6, Enemy.SNOWMAN, 0, 0); b.plat(88, 7, 4); b.sh(89, 5, 3);
        // la pista: lungo altopiano di ghiaccio dove due pinguini si incrociano; sotto, la galleria con le gemme
        b.plat(96, 6, 22); b.en(100, 5, Enemy.PENGUIN, 96, 117); b.en(112, 5, Enemy.PENGUIN, 96, 117); b.row(105, 2, "B?B"); b.sh(98, 4, 4); b.sh(110, 4, 4);
        b.sh(97, 9, 20); b.en(104, Enemy.HOG, 97, 116); b.crow(122, 5f, 116, 128); b.sh(121, 7, 3);
        // burrone di cinque e cresta di massi con un pupazzo in vetta
        b.arc(129, 5);
        b.col(138, 2); b.col(142, 3); b.col(146, 4); b.col(147, 4); b.col(151, 3); b.col(155, 2); b.en(146, 5, Enemy.SNOWMAN, 0, 0); b.gem(138, 6); b.gem(142, 5);
        b.gem(151, 5); b.gem(155, 6); b.en(140, Enemy.HOG, 139, 141); b.en(153, Enemy.HOG, 152, 154); b.row(161, 6, "B?B"); b.en(167, Enemy.PENGUIN, 157, 175);
        b.en(172, Enemy.ACORN);
        // burrone di sei: due lastre sospese, e un corvo nel mezzo
        b.plat(178, 8, 1); b.plat(181, 7, 1); b.gem(178, 6); b.gem(181, 5); b.crow(180, 3f, 176, 184);
        // il fortino: pupazzo dietro un muretto, pinguino davanti; le casse stanno sopra la linea di tiro
        b.sh(185, 9, 3); b.en(192, Enemy.PENGUIN, 186, 199); b.col(200, 2); b.en(203, Enemy.SNOWMAN); b.row(194, 5, "?B?B?"); b.col(207, 2); b.hill(211, 221, 3);
        b.en(216, Enemy.ACORN); b.sh(213, 5, 6); b.en(224, Enemy.HOG, 222, 225);
        // due burroni con un isolotto di quattro caselle: ci scivola un pinguino
        b.en(234, Enemy.PENGUIN, 233, 236); b.gem(229.5f, 6); b.gem(239.5f, 6); b.sh(234, 6, 2);
        // il lago gelato: tre pinguini su un lungo piano, mattone segreto sopra il più veloce
        b.sh(245, 9, 3); b.col(250, 1); b.col(280, 1); b.en(256, Enemy.PENGUIN, 251, 264); b.en(270, Enemy.PENGUIN, 266, 279); b.col(265, 2);
        b.row(255, 6, "B?H?B"); b.row(270, 6, "?B?"); b.sh(256, 4, 3); b.en(286, Enemy.SNOWMAN); b.sh(283, 7, 2);
        // burrone di sei sotto il tiro incrociato: un pupazzo per sponda (quello dietro è già superato: basta non voltarsi)
        b.arc(293, 5); b.en(301, Enemy.SNOWMAN);
        b.up(306, 5); b.plat(311, 5, 5); b.down(316, 5); b.row(312, 1, "B?B"); lvBattery(b, 313, 3.6f); b.crow(313, 2.2f, 304, 322); b.en(324, Enemy.PENGUIN, 321, 335);
        b.col(336, 2); b.en(340, Enemy.HOG, 337, 345); b.sh(326, 7, 3); b.sh(338, 6, 3);
        // burrone di sei con due appigli a salire
        b.plat(348, 8, 1); b.plat(351, 6, 1); b.gem(348, 6); b.gem(351, 4);
        // discesa a balze: su ogni balza un pinguino, in fondo un pupazzo che copre tutta la discesa
        b.plat(353, 6, 8); b.plat(361, 7, 8); b.en(356, 5, Enemy.PENGUIN, 353, 360); b.en(364, 6, Enemy.PENGUIN, 361, 368); b.sh(354, 4, 5); b.sh(362, 6, 5);
        b.sh(354, 9, 14); b.en(376, Enemy.SNOWMAN); b.row(381, 6, "?B?"); b.en(386, Enemy.ACORN); b.col(391, 3); b.en(396, Enemy.PENGUIN, 392, 401); b.gem(391, 5);
        // burrone di sei, corvo nel mezzo
        b.arc(403, 5); b.crow(406, 4.5f, 401, 411);
        b.sh(411, 9, 3); b.en(417, Enemy.SNOWMAN); b.col(421, 2); b.en(427, Enemy.PENGUIN, 422, 434); b.row(425, 6, "B?B?B"); b.col(435, 2); b.en(440, Enemy.SNOWMAN);
        b.sh(437, 7, 2);
        // ultimo burrone: cinque caselle, e un pupazzo che aspetta in cima all'ultimo dosso
        b.arc(447, 5); b.hill(456, 466, 3); b.en(461, Enemy.SNOWMAN); b.sh(454, 7, 2); b.en(472, Enemy.PENGUIN, 468, 480); b.row(472, 6, "B?B");
        b.lv.flagX = 486; b.lv.flagBaseY = G;
        return b.lv;
    }

    // ------------------------------------------------------------------ 19: il prato in fiamme — lapilli e salamandre
    private static Level fire() {
        LevelsB b = new LevelsB(476, 19, 3);
        b.g(0, 34); b.g(40, 72); b.g(97, 150); b.g(156, 200); b.g(207, 250); b.g(273, 330); b.g(336, 339); b.g(345, 348); b.g(354, 420); b.g(426, 475);
        b.lv.fireStart = -10f;
        b.lv.fireZones = new float[][] {{12, 1.9f, 24}, {72, 2.3f, 22}, {150, 2.6f, 20}, {226, 0f}, {254, 2.8f, 19}, {330, 3.0f, 18}, {400, 3.3f, 16}};
        // avvio nel fumo: una salamandra cammina incontro e ogni tanto sputa fuoco; si salta quando ha appena finito
        b.sh(6, 9, 3); b.row(11, 6, "B?B"); b.en(20, Enemy.SALAMANDER, 14, 26); b.sh(18, 6, 3); b.jet(29, 0f, 3.4f); b.en(32, Enemy.BEETLE);
        // primo burrone di lava: un lapillo salta dal centro, si passa mentre ricade
        b.lap(37, 2.6f, 7f); b.arc(35, 5);
        b.jet(44, 0f, 3.2f); b.jet(48, 1.1f, 3.2f); b.jet(52, 2.2f, 3.2f); b.sh(45, 7, 3); b.row(56, 6, "?B?"); b.en(62, Enemy.SALAMANDER, 55, 70); b.en(68, Enemy.ACORN);
        // il ponte che crolla: otto tavole bruciate; due lapilli saltano tra le tavole, a tempi diversi
        b.bridge(75, new int[] {9, 8, 9, 8}); b.lap(78.6f, 2.4f, 6.5f); b.lap(83.6f, 2.8f, 6.5f); b.lap(88.6f, 2.5f, 6.5f);
        // l'onda di fuoco, con una salamandra che cammina tra i getti
        for (int i = 0; i < 6; i++) b.jet(102 + i * 4, i * 0.45f, 3.2f);
        b.en(112, Enemy.SALAMANDER, 104, 122); b.sh(104, 7, 3); b.row(114, 6, "B?B"); b.en(128, Enemy.HOG, 125, 133); b.col(134, 2); b.col(138, 3); b.col(142, 2);
        b.gem(134, 6); b.gem(138, 5); b.gem(142, 6); b.crow(138, 2.5f, 132, 146); b.en(147, Enemy.BEETLE);
        // burrone di cinque con il lapillo: due salamandre fanno la guardia all'arrivo, una per parte del muretto
        b.lap(153, 2.2f, 7f); b.arc(151, 5);
        b.en(160, Enemy.SALAMANDER, 157, 164); b.col(165, 2); b.en(170, Enemy.SALAMANDER, 166, 176); b.row(168, 5, "?B?"); b.jet(179, 0f, 3.0f); b.jet(183, 1.5f, 3.0f);
        b.hill(186, 196, 3); b.en(191, Enemy.ACORN); b.sh(188, 5, 6);
        // burrone di sei: un'unica tavola che crolla nel mezzo, e un lapillo per parte
        b.cr(203, 9, 2); b.lap(201.6f, 2.6f, 6f); b.lap(205.6f, 2.6f, 6f); b.gem(203.5f, 7);
        // salita alla radura dove il fuoco si ferma: la cella energetica è in cima alla scala di massi
        b.sh(209, 9, 3); b.en(214, Enemy.SALAMANDER, 209, 220); b.up(222, 4); b.plat(226, 6, 6); b.down(232, 4); lvBattery(b, 228.5f, 3.4f); b.row(227, 2, "B?B");
        b.sh(238, 9, 4); b.row(240, 6, "BHB"); b.en(246, Enemy.BEETLE);
        // il grande ponte: tavole e lapilli alternati; alla fine una salamandra aspetta sulla terraferma
        b.bridge(252, new int[] {9, 8, 7, 8}); b.lap(255.6f, 2.5f, 6.5f); b.lap(260.6f, 2.9f, 7.5f); b.lap(265.6f, 2.3f, 6.5f); b.en(280, Enemy.SALAMANDER, 275, 288);
        // il braciere: getti fitti con le casse sopra, poi colonne con i corvi
        for (int i = 0; i < 7; i++) b.jet(292 + i * 3, (i % 3) * 1.0f, 3.0f);
        b.row(296, 6, "?B?B?"); b.sh(293, 7, 2); b.en(322, Enemy.HOG, 319, 327); b.col(310, 2); b.col(314, 3); b.gem(310, 6); b.gem(314, 5); b.crow(312, 2.6f, 306, 320);
        // l'arcipelago di cenere: due isolotti, lapilli in ogni braccio di lava
        b.lap(333, 2.4f, 7f); b.lap(342, 2.7f, 7f); b.lap(351, 2.2f, 7f); b.en(337, Enemy.SALAMANDER, 336, 339); b.gem(337.5f, 7); b.gem(346.5f, 7); b.jet(347, 0f, 2.8f);
        // la corsa finale: salamandre e getti alternati, il fuoco è veloce
        b.sh(356, 9, 3); b.en(363, Enemy.SALAMANDER, 358, 370); b.jet(372, 0f, 3.0f); b.jet(376, 1.0f, 3.0f); b.jet(380, 2.0f, 3.0f); b.row(374, 6, "B?B");
        b.en(388, Enemy.SALAMANDER, 383, 395); b.col(397, 2); b.col(401, 3); b.col(405, 2); b.gem(397, 6); b.gem(401, 5); b.gem(405, 6); b.en(411, Enemy.ACORN);
        b.jet(414, 0f, 2.8f); b.jet(417, 1.4f, 2.8f);
        // ultimo burrone: due lapilli sfasati
        b.lap(422, 2.4f, 7f); b.lap(424, 2.4f, 7f); b.arc(421, 5);
        b.en(432, Enemy.SALAMANDER, 428, 440); b.sh(430, 6, 4); b.jet(444, 0f, 2.8f); b.jet(448, 1.4f, 2.8f); b.row(452, 6, "B?B"); b.en(458, Enemy.SALAMANDER, 451, 464);
        b.lv.flagX = 469; b.lv.flagBaseY = G;
        return b.lv;
    }

    // ------------------------------------------------------------------ 21: la primavera — rane e piante carnivore
    private static Level spring() {
        LevelsB b = new LevelsB(486, 21, 4);
        b.g(0, 36); b.g(47, 90); b.g(96, 140); b.g(151, 200); b.g(206, 210); b.g(216, 262); b.g(273, 320); b.g(327, 372); b.g(383, 430); b.g(436, 485);
        // giardino d'ingresso: la pianta carnivora si schiaccia quando è chiusa; quando trema sta per scattare
        b.sh(6, 9, 3); b.row(10, 6, "B?B"); b.en(16, Enemy.PLANT); b.sh(15, 5, 3); b.en(23, Enemy.BEETLE); b.thorn(27); b.en(31, Enemy.FROG);
        // primo fiume: dal fiore a molla si vola dall'altra parte, dove una rana aspetta chi atterra
        b.pad(36); b.flight(36); b.en(53, Enemy.FROG); b.sh(49, 9, 2);
        b.hill(57, 67, 3); b.en(62, Enemy.ACORN); b.sh(59, 5, 6); b.en(70, Enemy.PLANT); b.thorn(73); b.en(76, Enemy.PLANT); b.row(71, 5, "?B?B?"); b.sh(72, 3, 3);
        b.en(82, Enemy.HOG, 79, 88); b.sh(84, 7, 3);
        // ruscello stretto: le rane lo attraversano a balzi, una per sponda
        b.arc(91, 5); b.en(88, Enemy.FROG); b.en(99, Enemy.FROG);
        // il roseto: rovi e piante alternati, le gemme stanno sopra le piante (si prendono solo quando sono chiuse)
        b.thorn(104); b.en(107, Enemy.PLANT); b.thorn(110); b.en(113, Enemy.PLANT); b.thorn(116); b.gem(107, 7); b.gem(113, 7); b.row(106, 4, "B?B?B?B");
        b.col(121, 2); b.col(125, 3); b.col(129, 2); b.gem(121, 6); b.gem(125, 5); b.gem(129, 6); b.crow(125, 2.5f, 119, 133); b.en(134, Enemy.FROG);
        // secondo fiume: fiore a molla; sulla sponda d'arrivo due piante lasciano libera solo la casella in mezzo
        b.pad(140); b.flight(140); b.en(153, Enemy.PLANT); b.en(158, Enemy.PLANT); b.sh(155, 9, 2);
        // la collina dei ciliegi: balcone di casse con la cella energetica, raggiungibile dal fiore a molla sotto
        b.sh(162, 9, 3); b.pad(168); b.plat(174, 4, 8); lvBattery(b, 178, 2.6f); b.sh(175, 2, 3); b.sh(179, 2, 3); b.en(176, Enemy.FROG); b.thorn(180);
        b.en(184, Enemy.HOG, 181, 190); b.row(186, 6, "B?B"); b.en(194, Enemy.PLANT); b.thorn(190);
        // due ruscelli e un isolotto di quattro caselle con una pianta al centro
        b.en(208.5f, Enemy.PLANT); b.gem(203.5f, 6); b.gem(213.5f, 6); b.gem(208.5f, 6);
        // lo stagno delle rane: tre rane su un tratto piano chiuso da due muretti
        b.sh(219, 9, 3); b.col(224, 2); b.en(228, Enemy.FROG); b.en(234, Enemy.FROG); b.en(240, Enemy.FROG); b.col(244, 2); b.row(231, 5, "B?H?B"); b.sh(232, 3, 3);
        b.thorn(248); b.en(252, Enemy.PLANT); b.thorn(256); b.sh(250, 6, 5);
        // terzo fiume: all'arrivo una pianta; chi atterra corto la trova aperta
        b.pad(262); b.flight(262); b.en(276, Enemy.PLANT); b.sh(279, 9, 3);
        b.up(285, 4); b.plat(289, 6, 8); b.down(297, 4); b.en(292, 5, Enemy.FROG, 0, 0); b.row(291, 2, "?B?"); b.sh(290, 4, 6); b.crow(293, 1.2f, 285, 301);
        b.thorn(304); b.en(307, Enemy.PLANT); b.thorn(310); b.en(314, Enemy.ACORN); b.sh(305, 6, 5);
        // ruscello di sei con un sasso nel mezzo: una rana sul sasso
        b.plat(323, 9, 2); b.en(323.5f, 8, Enemy.FROG, 0, 0); b.gem(321.5f, 6); b.gem(325.5f, 6);
        // doppia molla: il primo fiore lancia sul secondo, sospeso; tra i due un campo di rovi con le piante
        b.sh(329, 9, 3); b.pad(334); b.thorn(338); b.en(341, Enemy.PLANT); b.thorn(344); b.thorn(347); b.plat(350, 7, 4); b.sh(350, 5, 4); b.en(356, Enemy.FROG);
        b.row(360, 6, "B?B"); b.en(366, Enemy.PLANT); b.thorn(362);
        // quarto fiume
        b.pad(372); b.flight(372); b.en(387, Enemy.FROG); b.en(392, Enemy.FROG); b.sh(389, 6, 3);
        b.hill(397, 407, 4); b.en(402, Enemy.PLANT); b.sh(399, 4, 2); b.sh(404, 4, 2); b.thorn(410); b.en(413, Enemy.HOG, 411, 420); b.row(414, 6, "?B?"); b.en(424, Enemy.PLANT);
        b.thorn(420);
        // ultimo ruscello: sei caselle, rana all'arrivo, poi il viale delle piante
        b.arc(431, 6); b.en(440, Enemy.FROG);
        b.en(446, Enemy.PLANT); b.thorn(449); b.en(452, Enemy.PLANT); b.thorn(455); b.en(458, Enemy.PLANT); b.row(447, 4, "B?B?B?B"); b.gem(446, 7); b.gem(452, 7);
        b.gem(458, 7); b.en(466, Enemy.FROG); b.sh(462, 9, 3);
        b.lv.flagX = 478; b.lv.flagBaseY = G;
        return b.lv;
    }

    // ------------------------------------------------------------------ 23: il cielo — stelle cadenti e solleoni
    private static Level heaven() {
        LevelsB b = new LevelsB(500, 23, 5);
        b.g(0, 32); b.g(37, 70); b.g(100, 142); b.g(153, 190); b.g(222, 262); b.g(269, 272); b.g(279, 320); b.g(331, 366); b.g(398, 440); b.g(447, 499);
        // nuvola d'ingresso: il primo solleone gira attorno a una cassa; si prende quando è dall'altra parte
        b.sh(6, 9, 3); b.row(12, 6, "B?B"); b.sun(13, 5.5f, 1.6f, 2.6f); b.whirl(22, 18, 30); b.sh(24, 7, 3);
        // primo vuoto: una stella cadente lo attraversa sempre lungo la stessa scia
        b.arc(33, 4); b.star(40, -1.5f); b.en(44, Enemy.BEETLE);
        b.hill(47, 57, 3); b.sh(49, 5, 6); b.storm(52, 2.4f, 46, 60); b.col(62, 2); b.col(66, 3); b.gem(62, 6); b.gem(66, 5); b.sun(64, 5f, 2.0f, 2.4f);
        // primo ponte di nuvole: sei nuvole che cedono, due scie di stelle che lo tagliano in punti diversi
        b.bridge(73, new int[] {9, 8, 9, 8, 9}); b.star(86, -2.5f); b.star(97, -0.5f);
        // tempio di marmo: solleoni attorno alle colonne, la cella energetica sul tetto raggiungibile dal trampolino
        b.sh(102, 9, 3); b.pad(107); b.plat(112, 4, 8); b.col(112, 5); b.col(119, 5); lvBattery(b, 115.5f, 2.9f); b.sh(113, 3, 2); b.sh(117, 3, 2);
        b.sun(124, 7f, 1.8f, 2.2f); b.whirl(128, 122, 134); b.col(136, 2); b.gem(136, 6); b.row(128, 5, "?B?");
        // balzo lungo con il trampolino; in volo si passa sopra una stella
        b.pad(142); b.flight(142); b.star(150, -3f); b.en(157, Enemy.BEETLE);
        // bosco di colonne: un solleone ogni due colonne, a quote diverse
        b.col(161, 2); b.col(165, 3); b.col(169, 4); b.col(173, 3); b.col(177, 2); b.sun(163, 5.5f, 1.4f, 2.8f); b.sun(171, 4f, 1.4f, -2.8f); b.sun(179, 6f, 1.6f, 2.4f);
        b.gem(161, 6); b.gem(165, 5); b.gem(169, 4); b.gem(173, 5); b.gem(177, 6); b.storm(169, 1.2f, 160, 180); b.sh(183, 9, 3); b.whirl(186, 182, 189);
        // il grande ponte: sei nuvole a onda, una nuvola temporalesca e due scie di stelle
        b.bridge(193, new int[] {9, 8, 7, 8, 9, 8}); b.storm(205, 3.2f, 196, 216); b.star(203, -2f); b.star(216, -1f);
        // terrazza: mattone segreto custodito da due solleoni in controrotazione
        b.sh(224, 9, 3); b.row(231, 6, "B?H?B"); b.sun(231, 5.5f, 2.0f, 2.2f); b.sun(235, 5.5f, 2.0f, -2.2f); b.sh(232, 4, 3); b.whirl(242, 238, 250);
        b.hill(248, 258, 3); b.sh(250, 5, 6); b.storm(253, 2.6f, 247, 260);
        // due vuoti e un isolotto: la stella cade proprio sull'isolotto, si salta appena è passata
        b.star(274, -1.5f); b.gem(265.5f, 6); b.gem(275.5f, 6); b.sh(270, 7, 2);
        // scalinata con i solleoni sui pianerottoli
        b.sh(281, 9, 2); b.up(285, 5); b.down(291, 5); b.plat(290, 5, 1); b.sun(290, 3f, 1.8f, 2.6f); b.row(289, 0, "?"); b.gem(290, 3.4f);
        b.whirl(300, 297, 306); b.col(308, 3); b.sun(308, 5.4f, 1.5f, 3f); b.gem(308, 5); b.en(313, Enemy.BEETLE); b.sh(314, 7, 3);
        // secondo balzo lungo
        b.pad(320); b.flight(320); b.star(326, -2f);
        // torre di nuvole instabili con un solleone al centro
        b.sh(333, 9, 3); b.cr(340, 8, 2); b.cr(343, 6, 2); b.cr(346, 4, 2); b.cr(350, 6, 2); b.cr(353, 8, 2); b.sun(346.5f, 6.4f, 1.3f, 2.4f);
        b.gem(340, 6); b.gem(343, 4); b.gem(346, 2.6f); b.gem(347, 2.6f); b.gem(350, 4); b.gem(353, 6); b.row(338, 6, "B"); b.whirl(360, 356, 365);
        // l'ultimo ponte: sei nuvole, tre scie di stelle, un solleone a metà
        b.bridge(369, new int[] {9, 8, 9, 8, 9, 9}); b.star(378, -2.5f); b.star(386, -0.5f); b.star(396, -1.5f); b.sun(383.5f, 4.6f, 1.5f, 2.6f);
        b.sh(400, 9, 3); b.storm(406, 3f, 399, 414); b.col(410, 2); b.col(414, 3); b.col(418, 2); b.gem(410, 6); b.gem(414, 5); b.gem(418, 6); b.sun(414, 3.2f, 1.6f, 2.8f);
        b.whirl(425, 420, 432); b.row(426, 6, "?B?"); b.sh(434, 7, 3);
        // ultimo vuoto: sei caselle, stella nel mezzo
        b.arc(441, 6); b.star(448, -2f);
        b.sh(452, 9, 3); b.sun(460, 6f, 2.0f, 2.4f); b.sun(468, 6f, 2.0f, -2.4f); b.row(463, 6, "B?B"); b.whirl(476, 470, 484); b.storm(472, 2.6f, 456, 484);
        b.lv.flagX = 492; b.lv.flagBaseY = G;
        return b.lv;
    }

    // ------------------------------------------------------------------ attrezzi

    private void g(int x0, int x1) {
        for (int x = x0; x <= x1; x++) for (int y = G; y < H; y++) lv.set(x, y, Level.GROUND);
    }

    private void hill(int x0, int x1, int maxRise) {
        for (int x = x0; x <= x1; x++) {
            int rise = Math.max(1, Math.round(maxRise * (float) Math.sin(Math.PI * (x - x0 + 0.5) / (x1 - x0 + 1))));
            for (int i = 1; i <= rise; i++) lv.set(x, G - i, Level.GROUND);
        }
    }

    private void col(int x, int h) { for (int i = 1; i <= h; i++) lv.set(x, G - i, Level.STONE); }
    private void up(int x, int n) { for (int i = 0; i < n; i++) col(x + i, i + 1); }
    private void down(int x, int n) { for (int i = 0; i < n; i++) col(x + i, n - i); }
    private void plat(int x, int y, int n) { for (int i = 0; i < n; i++) lv.set(x + i, y, Level.STONE); }
    private void cr(int x, int y, int n) { for (int i = 0; i < n; i++) lv.set(x + i, y, Level.CRUMBLE); }
    private void row(int x, int y, String s) { for (int i = 0; i < s.length(); i++) lv.set(x + i, y, s.charAt(i)); }
    private void gem(float x, float y) { lv.shards.add(new float[] {x, y}); }
    private void sh(int x, float y, int n) { for (int i = 0; i < n; i++) gem(x + i, y); }

    /** Gemme ad arco sopra un burrone che comincia in x ed è largo n: indicano il salto. */
    private void arc(int x, int n) {
        for (int i = 0; i < n; i++) {
            float u = (i + 0.5f) / n;
            gem(x + i, 7.2f - 1.4f * 4f * u * (1f - u));
        }
    }

    private int stand(int x) {
        int y = 0;
        while (y < H && !lv.solid(x, y)) y++;
        return y - 1;
    }

    private void en(float x, int type) { en(x, type, 0, 0); }
    private void en(float x, int type, float lo, float hi) { lv.enemies.add(new float[] {x, stand((int) x), type, lo, hi}); }
    private void en(float x, int row, int type, float lo, float hi) { lv.enemies.add(new float[] {x, row, type, lo, hi}); }
    private void crow(float x, float y, float lo, float hi) { lv.enemies.add(new float[] {x, y, Enemy.CROW, lo, hi}); }
    private void storm(float x, float y, float lo, float hi) { lv.enemies.add(new float[] {x, y, Enemy.STORM, lo, hi}); }
    private void whirl(float x, float lo, float hi) { en(x, Enemy.WHIRL, lo, hi); }
    private void owl(float x, float y, float lo, float hi) { lv.enemies.add(new float[] {x, y, Enemy.OWL, lo, hi}); }
    /** Riccio di castagna appeso alla quota y (di solito subito sotto una cassa o una mensola). */
    private void chest(float x, float y) { lv.enemies.add(new float[] {x + 0.1f, y, Enemy.CHESTNUT, 0, 0}); }
    /** Lapillo che salta dal burrone alla colonna x: periodo in secondi e altezza del salto in caselle. */
    private void lap(float x, float period, float height) { lv.enemies.add(new float[] {x + 0.15f, 11.6f, Enemy.LAPILLO, period, height}); }
    /** Stella cadente: parte sopra lo schermo alla colonna x e scende in diagonale verso sinistra. */
    private void star(float x, float y) { lv.enemies.add(new float[] {x, y, Enemy.STAR, -3.2f, 4.6f}); }
    /** Solleone che gira attorno a (x, y) con raggio r e velocità angolare w. */
    private void sun(float x, float y, float r, float w) { lv.enemies.add(new float[] {x, y, Enemy.SUNNY, w, r}); }

    private void jet(int x, float phase, float period) { lv.jets.add(new float[] {x, stand(x) + 1, period, phase}); }
    private void thorn(int x) { lv.thorns.add(new float[] {x, stand(x)}); }
    private void pad(int x) { lv.set(x, stand(x) + 1, Level.SPRING); }

    private void flight(int x) {
        float[][] q = {{2f, 4.7f}, {3.5f, 3.0f}, {5f, 2.0f}, {6.4f, 1.6f}, {8f, 2.2f}, {9.4f, 4.1f}};
        for (float[] p : q) gem(x + 0.5f + p[0], p[1]);
    }

    /** Ponte di nuvole o tavole che cedono: larghe tre caselle, una ogni cinque da x0, alla riga indicata; una gemma sopra ognuna. */
    private void bridge(int x0, int[] rows) {
        for (int i = 0; i < rows.length; i++) {
            cr(x0 + i * 5, rows[i], 3);
            gem(x0 + i * 5 + 1, rows[i] - 2);
        }
    }
}
