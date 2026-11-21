package com.valeriodipaolo.platformgame.core;

/**
 * Ordine dei livelli nel gioco. Gli identificativi interni (1..11) restano quelli storici (salvataggi, test); qui si decide solo
 * in che ordine si giocano e quali sono 3D. estate (1, 2), autunno (3, 4), inverno/aurora (5, 6), primavera (9, 10, con la fuga dalle api in coda), incendio (7, 8).
 */
public final class Levels {
    private Levels() {}

    /** Nome di ogni livello, per id. */
    public static final String[] NAMES = {null, "Il prato", "Bosco (3D)", "Prato d'autunno", "Bosco d'autunno (3D)", "Tundra boreale", "Lago gelato (3D)",
            "Prato in fiamme", "Bosco in fiamme (3D)", "Primavera", "Primavera e api (3D)", "Il cielo", "Sulle nuvole (3D)",
            "Talpe e lumache", "Radure (3D)", "Castagne e gufi", "Querceto (3D)", "Neve e pupazzi", "Ghiaccio (3D)",
            "Brace e lapilli", "Rogo (3D)", "Rane e fiori", "Giardino (3D)", "Stelle e soli", "Cielo alto (3D)",
            "Valle delle molle", "Labirinto (3D)", "Ghiaccio fragile", "Arcipelago (3D)", "Le due strade", "Rovi (3D)",
            "Pioggia di ricci", "Fuga dal rogo (3D)", "Scala celeste", "La vetta (3D)",
            "Isola: le casse", "Isola: il masso", "Isola: il fiume",
            "Le dune", "La fabbrica", "La miniera"};

    /** "LIVELLO 7 · Querceto": numero nell'ordine di gioco e nome. */
    public static String title(int id) { return "LIVELLO " + (slot(id) + 1) + "  \u00B7  " + NAMES[id].replace(" (3D)", ""); }

    public static final int[] ORDER = {1, 2, 13, 14, 3, 4, 15, 16, 5, 6, 17, 18, 9, 10, 21, 22, 7, 8, 19, 20, 11, 12, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40};

    public static boolean is3D(int id) { return id % 2 == 0 || id >= 35; }      // l'isola tropicale è tutta in 3D

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
