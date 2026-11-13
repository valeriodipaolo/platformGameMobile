package com.valeriodipaolo.platformgame.core;

import java.util.ArrayList;
import java.util.List;

/** Griglia di caselle del livello più le posizioni iniziali di monete e nemici. Riga 0 in alto. */
public final class Level {
    public static final char EMPTY = '.', GROUND = '#', BRICK = 'B', BONUS = '?', USED = 'U', STONE = 'S';

    public final int w, h;
    private final char[] t;
    public float startX = 3, startY = 8;
    /** Colonna dell'asta del traguardo. */
    public int flagX;
    /** Riga del terreno su cui poggia l'asta (l'asta non è un ostacolo). */
    public int flagBaseY;
    public final List<float[]> coins = new ArrayList<float[]>();
    public final List<float[]> enemies = new ArrayList<float[]>();

    public Level(int w, int h) {
        this.w = w;
        this.h = h;
        t = new char[w * h];
        java.util.Arrays.fill(t, EMPTY);
    }

    public char get(int x, int y) {
        if (x < 0 || x >= w) return STONE;       // muri invisibili ai bordi
        if (y < 0 || y >= h) return EMPTY;       // sopra: cielo; sotto: precipizio
        return t[y * w + x];
    }

    public void set(int x, int y, char c) {
        if (x >= 0 && x < w && y >= 0 && y < h) t[y * w + x] = c;
    }

    public boolean solid(int x, int y) {
        return get(x, y) != EMPTY;
    }
}
