package com.valeriodipaolo.platformgame.core3d;

/**
 * Nemico che fa la ronda avanti e indietro lungo un asse: scarabeo, riccio (spinoso: non si schiaccia),
 * corvo (vola a onde) o ghianda (procede a balzi).
 */
public final class Enemy3 {
    public static final int BEETLE = 0, HOG = 1, CROW = 2, ACORN = 3, STORM = 4, WHIRL = 5,
            MOLE = 6 /* non più usato */, SNAIL = 7, CHESTNUT = 8, OWL = 9, PENGUIN = 10, SNOWMAN = 11, LAPILLO = 12, SALAMANDER = 13, FROG = 14, PLANT = 15, STAR = 16, SUNNY = 17;
    public int type;
    /** Aspetto diverso con lo stesso comportamento (isola tropicale): granchio = scarabeo veloce, totem = pupazzo che tira noci di cocco, piranha = lapillo. */
    public int skin;
    public static final int SKIN_CRAB = 1, SKIN_TIKI = 2, SKIN_FISH = 3;
    /** Deserto, fabbrica, miniera: scorpione = riccio (non si schiaccia), avvoltoio e pipistrello = corvo, robottino = scarabeo, drone = gufo, masso = castagna. */
    /** Stagioni: stesso comportamento, aspetto del posto (rana, lepre bianca, brace, riccio di ghiaccio, riccio di lava, pinguino). */
    public static final int SKIN_FROG = 10, SKIN_HARE = 11, SKIN_EMBER = 12, SKIN_ICE = 13, SKIN_MAGMA = 14, SKIN_PENGUIN = 15,
            SKIN_HOPPER = 16, SKIN_PORCUPINE = 17, SKIN_WASP = 18, SKIN_TORTOISE = 19, SKIN_MOUSE = 20, SKIN_MAGPIE = 21,
            SKIN_CATERPILLAR = 22, SKIN_SWALLOW = 23, SKIN_HAWK = 24;
    public static final int SKIN_SCORPION = 4, SKIN_VULTURE = 5, SKIN_ROBOT = 6, SKIN_DRONE = 7, SKIN_BAT = 8, SKIN_ROCK = 9;
    /** Quota di partenza (ghianda) o quota media del volo (corvo). */
    public float baseY, hopT;
    public float x, y, z;
    /** true: si muove lungo il corridoio (z); false: di traverso (x). */
    public final boolean alongZ;
    public final float lo, hi;
    public float dir = 1, anim;
    public boolean dead, flung;
    public float flatTime, vx, vy, vz;
    /** Fase del comportamento dei nemici nuovi (talpa, lumaca, castagna...) e punto di partenza. */
    public int mode;
    /** Pupazzo di neve: verso dove guarda e tira (0 verso lo schermo, 1 a destra, -1 a sinistra). */
    public int face;
    /** Gufo: orientamento morbido (imbardata, beccheggio, rollio) e punti della traiettoria in corso. */
    public float yaw, pitch, bank, sx, sy, sz, ex, ey, ez;
    /** Lumaca appena uscita dal guscio: per un attimo non subisce né fa danno. */
    public float grace;
    public float x0, y0, z0, t;
    /** Quota del suolo su cui cammina (fissata alla prima mossa): niente salti nel vuoto né attraversamenti di blocchi. */
    public float floor = Float.NaN;

    /** Ciclo della nuvola temporalesca: 0 vaga, 1 si carica (avviso), 2 fulmine verticale. */
    public static int stormPhase(float t) {
        float c = t % 3.6f;
        return c < 2.4f ? 0 : c < 3.1f ? 1 : 2;
    }

    /** Vola (corvo, nuvola temporalesca): non ha terreno sotto i piedi. */
    public boolean flies() {
        return type == CROW || type == STORM || type == OWL || type == STAR || type == SUNNY || type == CHESTNUT;
    }

    public Enemy3(float x, float z, boolean alongZ, float lo, float hi) {
        this.x = x;
        this.z = z;
        this.alongZ = alongZ;
        this.x0 = x;
        this.z0 = z;
        this.lo = lo;
        this.hi = hi;
    }
}
