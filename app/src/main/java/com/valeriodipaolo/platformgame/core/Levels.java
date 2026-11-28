package com.valeriodipaolo.platformgame.core;

/**
 * Ordine dei livelli nel gioco. Gli identificativi interni (1..11) restano quelli storici (salvataggi, test); qui si decide solo
 * in che ordine si giocano e quali sono 3D.
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
            "Le dune", "La fabbrica", "La miniera",
            "Carrello nel bosco (3D)", "Carrello nella neve (3D)", "Corsa tra le nuvole (3D)",
            "Fuga dalla valanga (3D)", "Fuga dalla lava (3D)", "Fuga dallo sciame (3D)"};

    /** "LIVELLO 7 · Querceto": numero nell'ordine di gioco e nome. */
    public static String title(int id) { return "LIVELLO " + (slot(id) + 1) + "  \u00B7  " + NAMES[id].replace(" (3D)", ""); }

    /**
     * Ordine di gioco, per tema: estate, autunno, inverno, primavera, incendio, cielo (in ogni tema 2D e 3D si alternano); in coda
     * i mondi senza stagione (isola tropicale, dune, fabbrica, miniera), che nel menu sono verdi.
     */
    public static final int[] ORDER = {1, 2, 13, 14, 29, 41,
            3, 4, 15, 16, 31, 26,
            5, 6, 17, 18, 27, 28, 42, 44,
            9, 10, 25, 22, 21, 30, 46,
            7, 8, 19, 20, 32, 45,
            11, 12, 23, 24, 33, 34, 43,
            35, 36, 37, 38, 39, 40};

    /** Ha uno dei temi delle stagioni (estate, autunno, inverno, primavera, incendio, cielo); gli altri stanno in coda. */
    public static boolean seasonal(int id) { return id < 35 || id > 40; }

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
