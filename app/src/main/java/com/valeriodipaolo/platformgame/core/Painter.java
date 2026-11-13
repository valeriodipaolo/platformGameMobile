package com.valeriodipaolo.platformgame.core;

/** Superficie di disegno: la logica di disegno non dipende da Android. Coordinate in pixel dello schermo. */
public interface Painter {
    int ALIGN_LEFT = 0, ALIGN_CENTER = 1, ALIGN_RIGHT = 2;

    /** Rapporto larghezza/altezza dell'immagine. */
    float aspect(int img);

    void image(int img, float x, float y, float w, float h, boolean flipX, float alpha);

    void rect(float x, float y, float w, float h, int argb);

    /** y è la linea di base del testo. */
    void text(String s, float x, float y, float size, int align, int argb);
}
