package com.valeriodipaolo.platformgame.core3d;

/**
 * Nemico che fa la ronda avanti e indietro lungo un asse: scarabeo, riccio (spinoso: non si schiaccia),
 * corvo (vola a onde) o ghianda (procede a balzi).
 */
public final class Enemy3 {
    public static final int BEETLE = 0, HOG = 1, CROW = 2, ACORN = 3;
    public int type;
    /** Quota di partenza (ghianda) o quota media del volo (corvo). */
    public float baseY, hopT;
    public float x, y, z;
    /** true: si muove lungo il corridoio (z); false: di traverso (x). */
    public final boolean alongZ;
    public final float lo, hi;
    public float dir = 1, anim;
    public boolean dead, flung;
    public float flatTime, vx, vy, vz;

    public Enemy3(float x, float z, boolean alongZ, float lo, float hi) {
        this.x = x;
        this.z = z;
        this.alongZ = alongZ;
        this.lo = lo;
        this.hi = hi;
    }
}
