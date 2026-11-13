package com.valeriodipaolo.platformgame.core;

public final class Enemy extends Body {
    public boolean active, dead;
    /** Maggiore di zero mentre resta schiacciato a terra prima di sparire. */
    public float flatTime;
    /** Colpito da sotto: vola via ribaltato senza urtare nulla. */
    public boolean flung;
    public float dir = -1;
    public float anim;

    public Enemy(float tx, float ty) {
        w = 0.8f;
        h = 0.66f;
        x = tx + 0.1f;
        y = ty + 1 - h;
    }
}
