package com.valeriodipaolo.platformgame.core;

/**
 * Ordine dei livelli nel gioco. Gli identificativi interni (1..11) restano quelli storici (salvataggi, test); qui si decide solo
 * in che ordine si giocano e quali sono 3D. estate (1, 2), autunno (3, 4), inverno/aurora (5, 6), primavera (9, 10, con la fuga dalle api in coda), incendio (7, 8).
 */
public final class Levels {
    private Levels() {}

    public static final int[] ORDER = {1, 2, 13, 14, 3, 4, 15, 16, 5, 6, 17, 18, 9, 10, 21, 22, 7, 8, 19, 20, 11, 12, 23, 24};

    public static boolean is3D(int id) { return id % 2 == 0; }

    /** Posizione (0..) di un livello nell'ordine di gioco. */
    public static int slot(int id) {
        for (int i = 0; i < ORDER.length; i++) if (ORDER[i] == id) return i;
        return 0;
    }

    /** Identificativo del livello successivo, 0 se è l'ultimo. */
    public static int next(int id) {
        int s = slot(id);
        return s + 1 < ORDER.length ? ORDER[s + 1] : 0;
    }

    /** Testo di fine livello vinto. */
    public static String wonText(int id) {
        int n = next(id);
        return n == 0 ? "Tocca lo schermo per tornare al menu" : "Tocca lo schermo per il livello " + (slot(n) + 1);
    }
}
