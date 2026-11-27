package com.valeriodipaolo.platformgame.core;

/**
 * Primo livello, "Il prato": un percorso tutto suo, che insegna una cosa alla volta le abilità del robot. Niente tubi, niente file di
 * mattoni a mezz'aria e niente scalinata finale: colline erbose, ruscelli da saltare, sporgenze di roccia, il fiore a molla che lancia
 * in alto, il doppio salto e la planata, le casse di legno (quelle rinforzate si aprono con la giravolta). Il terreno occupa le righe
 * 10 e 11; il robot cammina sulla riga 10.
 */
public final class LevelOne {
    private LevelOne() {}

    public static Level build() {
        LevelsB b = new LevelsB(228, 1, 0);
        b.g(0, 44); b.g(49, 90); b.g(95, 124); b.g(131, 227);

        // 1. il prato d'ingresso: si cammina raccogliendo gemme, la prima cassa si apre saltandoci sotto o sopra
        b.sh(6, 9, 4);
        b.row(17, 7, "?");
        b.en(24, Enemy.SNAIL, 21, 27);                        // la lumaca: saltandole sopra si chiude nel guscio
        b.hill(28, 37, 2); b.sh(30, 7, 6);                    // la prima collina, con le gemme sul crinale
        b.en(41, Enemy.BEETLE, 38, 43);

        // 2. il ruscello: un salto corto, le gemme ad arco indicano dove andare
        b.arc(45, 4);

        // 3. il fiore a molla: lancia sulla sporgenza alta, dove aspettano gemme e una cassa
        b.sh(51, 9, 2);
        b.pad(54);
        b.plat(56, 3, 6); b.sh(57, 2, 4); b.row(61, 2, "?");
        // sotto la sporgenza si passa anche a piedi, con una ghianda che saltella
        b.en(59, 9, Enemy.ACORN, 0, 0);

        // 4. le terrazze di roccia: si sale con due salti (o un doppio salto) fino al grappolo di gemme in alto
        b.plat(66, 7, 3); b.plat(71, 5, 3); b.sh(71, 3, 3); b.plat(76, 7, 2);
        b.en(73, 9, Enemy.BEETLE, 66, 79);

        // 5. il boschetto delle casse: tre casse in fila, ci si sale sopra; il riccio non si schiaccia, si salta o si stende con la giravolta
        b.row(84, 7, "???");
        b.sh(83, 9, 6);
        b.en(87, Enemy.HOG, 80, 90);

        // 6. il torrente largo: col doppio salto lo si passa comodi
        b.arc(91, 4);

        // 7. lo sperone di roccia: ci si sale sopra (gemme in cima); dentro, dietro una finta parete, il gettone
        for (int x = 104; x <= 107; x++) b.col(x, 3);
        b.sh(104, 6, 4);
        b.en(100, Enemy.SNAIL, 96, 102);
        b.en(112, Enemy.ACORN);
        b.hill(115, 122, 2); b.sh(116, 7, 5);

        // 8. lo stagno: largo sei, si passa con il doppio salto e la planata; le gemme scendono piano verso l'altra sponda
        b.lv.shards.add(new float[] {125.5f, 6.0f}); b.lv.shards.add(new float[] {127f, 5.6f}); b.lv.shards.add(new float[] {128.5f, 6.0f}); b.lv.shards.add(new float[] {130f, 6.8f});
        b.crow(128f, 4.5f, 123, 134);

        // 9. il prato fiorito: casse a coppie, un'altra collina, ghiande e scarabei
        b.row(138, 6, "??"); b.sh(137, 9, 4);
        b.en(143, Enemy.BEETLE, 140, 147);
        b.hill(149, 160, 3); b.sh(151, 6, 8);
        b.en(155, Enemy.ACORN);
        b.en(164, Enemy.HOG, 162, 168);

        // 10. il secondo fiore a molla porta sul sentiero alto delle gemme; sotto, chi resta a terra incontra le lumache
        b.pad(170);
        b.plat(172, 4, 9); b.sh(173, 3, 7);
        b.en(176, 9, Enemy.SNAIL, 172, 181); b.sh(174, 9, 6);

        // 11. l'ultima collina e il prato davanti al portone
        b.hill(188, 199, 3); b.sh(190, 6, 8); b.en(194, Enemy.BEETLE, 189, 199);
        b.row(204, 6, "?"); b.sh(207, 9, 4);

        b.lv.flagX = 216;
        b.lv.flagBaseY = LevelsB.G;
        return b.lv;
    }
}
