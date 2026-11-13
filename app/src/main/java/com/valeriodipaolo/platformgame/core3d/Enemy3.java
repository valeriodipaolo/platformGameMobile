package com.valeriodipaolo.platformgame.core3d;

/** Scarabeo che fa la ronda avanti e indietro lungo un asse. */
public final class Enemy3 {
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
