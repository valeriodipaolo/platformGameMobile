package com.valeriodipaolo.platformgame.core;

/**
 * Dove si nasconde il gettone d'oro in ogni livello 2D. Quattro modi, mai in bella vista:
 * <ul>
 * <li>dentro la base di una piramide di pietra, dal lato che scende: una finta parete, ci si entra tornando indietro contro il gradino più basso;</li>
 * <li>dentro un mattoncino qualunque, di solito vuoto (va colpito da sotto);</li>
 * <li>addosso a un nemico preciso, che ogni tanto luccica: va eliminato;</li>
 * <li>in alto, dove si arriva solo con un salto difficile.</li>
 * </ul>
 * I vecchi mattoncini segreti (casella SECRET) diventano mattoncini normali: il nascondiglio lo decide solo questa tabella.
 */
public final class Coins2D {
    private Coins2D() {}

    public static void place(Level lv, int id) {
        for (int y = 0; y < lv.h; y++) for (int x = 0; x < lv.w; x++) if (lv.get(x, y) == Level.SECRET) lv.set(x, y, Level.BRICK);
        lv.coin = null;
        lv.coinFoe = -1;
        int nb = 0;                                    // una cassa ogni tre diventa rinforzata (si apre solo con la giravolta)
        for (int x = 0; x < lv.w; x++) for (int y = 0; y < lv.h; y++) if (lv.get(x, y) == Level.BONUS && nb++ % 3 == 2) lv.set(x, y, Level.STEEL);
        switch (id) {
            case 1: carve(lv, 96); break;          // la prima piramide
            case 3: lv.coinFoe = 1; break;         // il primo corvo
            case 5: carve(lv, 190); break;         // la piramide doppia verso la fine
            case 7: brick(lv, 204, 7); break;      // il mattoncino a sinistra dell'ultima cassa
            case 9: lv.coin = new float[] {45f, 0.5f}; break;    // in cima al primo burrone, sopra l'arco di gemme: trampolino e planata in giravolta
            case 11: carve(lv, 184); break;        // la piramide sopra le nuvole
            case 13: lv.coinFoe = 22; break;       // il corvo sopra la piramide doppia
            case 15: brick(lv, 179, 6); break;     // l'ultimo mattoncino della fila lunga
            case 17: lv.coinFoe = 10; break;       // il secondo pupazzo di neve
            case 19: carve(lv, 220); break;        // l'arco di pietra in mezzo alla lava
            case 21: brick(lv, 113, 4); break;     // la fila alta di casse
            case 23: carve(lv, 274); break;        // le due piramidi dopo il ponte
            case 25: lv.coinFoe = 4; break;        // il corvo sopra il primo fosso
            case 27: brick(lv, 72, 5); break;      // il mattoncino a destra della cassa sopra le gallerie
            case 29: carve(lv, 215); break;        // il tetto di pietra sul campo di gemme
            case 31: lv.coinFoe = 11; break;       // il primo gufo
            case 33: lv.coin = new float[] {205f, 0f}; break;    // sopra l'arco di piattaforme fragili, tra i soli
            default: break;
        }
    }

    /** Riga del terreno sotto la colonna x. */
    static int ground(Level lv, int x) {
        for (int y = 0; y < lv.h; y++) if (lv.get(x, y) == Level.GROUND) return y;
        return lv.h - 2;
    }

    /**
     * Prima piramide dopo la colonna x0 che scende a gradini verso destra su terreno solido: le due righe più basse delle sue tre
     * colonne di base sul lato destro diventano finta parete, con il gettone in fondo. Ci si entra solo tornando indietro dopo
     * averla scavalcata, camminando contro il gradino più basso.
     */
    static void carve(Level lv, int x0) {
        for (int x = x0; x < x0 + 40; x++) {
            int g = ground(lv, x);
            if (lv.get(x, g - 1) != Level.STONE || !lv.solid(x + 1, g) || !lv.solid(x + 2, g)) continue;
            boolean open = true;                                  // davanti all'ingresso c'è spazio aperto (non un pozzo chiuso)
            for (int y = g - 4; y < g; y++) if (lv.get(x + 1, y) != Level.EMPTY) open = false;
            for (int y = g - 2; y < g; y++) if (lv.get(x + 2, y) != Level.EMPTY) open = false;
            if (!open) continue;
            if (lv.get(x - 1, g - 1) != Level.STONE || lv.get(x - 2, g - 1) != Level.STONE || lv.get(x - 2, g - 3) != Level.STONE) continue;
            for (int k = 0; k < 3; k++) for (int y = g - 2; y < g; y++) if (lv.get(x - k, y) == Level.STONE) lv.set(x - k, y, Level.FAKE);
            lv.coin = new float[] {x - 2, g - 1};
            return;
        }
        throw new IllegalStateException("livello " + lv.id + ": nessuna piramide dopo la colonna " + x0);
    }

    /** Il mattoncino più vicino a (x, y) sulla stessa riga diventa quello segreto. */
    static void brick(Level lv, int x, int y) {
        for (int d = 0; d <= 4; d++) for (int s = -1; s <= 1; s += 2) {
            if (lv.get(x + s * d, y) == Level.BRICK) { lv.set(x + s * d, y, Level.SECRET); return; }
        }
        throw new IllegalStateException("livello " + lv.id + ": vicino a (" + x + "," + y + ") non c'è un mattoncino");
    }
}
