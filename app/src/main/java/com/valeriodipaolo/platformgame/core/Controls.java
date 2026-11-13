package com.valeriodipaolo.platformgame.core;

/** Posizione dei pulsanti a schermo e zona toccata da un dito. */
public final class Controls {
    private Controls() {}

    public static final int LEFT = 1, RIGHT = 2, JUMP = 3;

    public static float size(float sw, float sh) { return 0.21f * sh; }
    public static float leftX(float sw, float sh) { return 0.05f * sw; }
    public static float rightX(float sw, float sh) { return leftX(sw, sh) + size(sw, sh) * 1.2f; }
    public static float jumpX(float sw, float sh) { return sw - 0.05f * sw - size(sw, sh); }
    public static float btnY(float sw, float sh) { return sh - 0.06f * sh - size(sw, sh); }

    /** Metà destra dello schermo = salto; metà sinistra divisa tra sinistra e destra. */
    public static int zone(float px, float py, float sw, float sh) {
        if (px >= sw * 0.5f) return JUMP;
        float split = leftX(sw, sh) + size(sw, sh) * 1.1f;
        return px < split ? LEFT : RIGHT;
    }
}
