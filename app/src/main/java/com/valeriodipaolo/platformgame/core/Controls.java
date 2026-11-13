package com.valeriodipaolo.platformgame.core;

/** Posizione dei pulsanti a schermo e zona toccata da un dito. */
public final class Controls {
    private Controls() {}

    public static final int LEFT = 1, RIGHT = 2, JUMP = 3, SPIN = 4;

    public static float size(float sw, float sh) { return 0.21f * sh; }
    public static float leftX(float sw, float sh) { return 0.05f * sw; }
    public static float rightX(float sw, float sh) { return leftX(sw, sh) + size(sw, sh) * 1.2f; }
    public static float jumpX(float sw, float sh) { return sw - 0.05f * sw - size(sw, sh); }
    public static float spinX(float sw, float sh) { return jumpX(sw, sh) - size(sw, sh) * 1.2f; }
    public static float btnY(float sw, float sh) { return sh - 0.06f * sh - size(sw, sh); }

    /**
     * Ogni tocco vale per il pulsante più vicino, sull'intera altezza dello schermo (anche sopra o sotto il disegno).
     * Metà sinistra: indietro e avanti. Metà destra: salto, tranne una fascia larga attorno al pulsante giravolta
     * (più larga verso sinistra, dove cade il pollice quando sbaglia).
     */
    public static int zone(float px, float py, float sw, float sh) {
        if (px >= sw * 0.5f) {
            float sx = spinX(sw, sh), sz = size(sw, sh);
            return (px >= sx - 0.4f * sz && px < sx + 1.1f * sz) ? SPIN : JUMP;
        }
        float split = leftX(sw, sh) + size(sw, sh) * 1.1f;
        return px < split ? LEFT : RIGHT;
    }
}
