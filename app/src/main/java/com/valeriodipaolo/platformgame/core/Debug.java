package com.valeriodipaolo.platformgame.core;

/**
 * Aiuti di debug, attivabili dal menu principale (selettore DEBUG): a schermo quante gemme si sono lasciate indietro, se il gettone
 * d'oro è stato saltato, e un bagliore dove si trova il gettone. Solo per provare i livelli: in gioco normale restano spenti.
 */
public final class Debug {
    private Debug() {}

    /** Aiuti di debug accesi. */
    public static volatile boolean on;

    /** Quanto indietro (in caselle) una gemma o il gettone contano come "saltati". */
    public static final float BEHIND = 2f;
}
