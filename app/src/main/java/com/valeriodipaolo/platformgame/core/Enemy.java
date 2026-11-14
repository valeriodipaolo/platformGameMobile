package com.valeriodipaolo.platformgame.core;

public final class Enemy extends Body {
    /** Scarabeo, riccio (non si schiaccia: si travolge con la giravolta), corvo (vola a onde), ghianda (salta). */
    public static final int BEETLE = 0, HOG = 1, CROW = 2, ACORN = 3;

    public final int type;
    public boolean active, dead;
    /** Maggiore di zero mentre resta schiacciato a terra prima di sparire. */
    public float flatTime;
    /** Colpito da sotto: vola via ribaltato senza urtare nulla. */
    public boolean flung;
    public float dir = -1;
    public float anim;
    /** Corvo: quota media e tratto di volo. Ghianda: tempo al prossimo salto. */
    public float baseY, lo, hi, hop;

    public Enemy(float tx, float ty) {
        this(tx, ty, BEETLE, 0, 0);
    }

    public Enemy(float tx, float ty, int type, float lo, float hi) {
        this.type = type;
        this.lo = lo;
        this.hi = hi;
        switch (type) {
            case HOG: w = 0.95f; h = 0.6f; break;
            case CROW: w = 0.8f; h = 0.6f; break;
            case ACORN: w = 0.7f; h = 0.8f; break;
            default: w = 0.8f; h = 0.66f; break;
        }
        x = tx + (1 - w) / 2;
        y = ty + 1 - h;
        if (type == CROW) {
            baseY = ty - h / 2;
            y = baseY;
            x = tx;
        }
        hop = 0.6f;
    }
}
