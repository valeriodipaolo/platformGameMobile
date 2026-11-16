package com.valeriodipaolo.platformgame.core;

public final class Enemy extends Body {
    /** Scarabeo, riccio (non si schiaccia: si travolge con la giravolta), corvo (vola a onde), ghianda (salta). */
    public static final int BEETLE = 0, HOG = 1, CROW = 2, ACORN = 3, STORM = 4, WHIRL = 5;
    /**
     * Nemici della seconda serie di livelli (due per livello): lumaca (estate; l'id 6, ex talpa, resta riservato), riccio di castagna e gufo (autunno), pinguino e
     * pupazzo di neve (inverno), lapillo e salamandra (incendio), rana e pianta carnivora (primavera), stella cadente e solleone (cielo).
     */
    public static final int MOLE = 6 /* non più usato */, SNAIL = 7, CHESTNUT = 8, OWL = 9, PENGUIN = 10, SNOWMAN = 11, LAPILLO = 12, SALAMANDER = 13,
            FROG = 14, PLANT = 15, STAR = 16, SUNNY = 17;
    /** Stato e cronometro del comportamento, e punto di partenza. */
    public int mode;
    public float t, x0, y0;

    public final int type;
    public boolean active, dead;
    /** Maggiore di zero mentre resta schiacciato a terra prima di sparire. */
    public float flatTime;
    /** Colpito da sotto: vola via ribaltato senza urtare nulla. */
    public boolean flung;
    public float dir = -1;
    public float anim;
    /** Nuvola temporalesca (STORM) e mulinello (WHIRL, non si schiaccia: si travolge con la giravolta). */
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
            case STORM: w = 1.1f; h = 0.8f; break;
            case WHIRL: w = 0.9f; h = 1.1f; break;
            case SNAIL: w = 0.9f; h = 0.65f; break;
            case CHESTNUT: w = 0.8f; h = 0.8f; break;
            case OWL: w = 0.9f; h = 0.8f; break;
            case PENGUIN: w = 0.8f; h = 0.9f; break;
            case SNOWMAN: w = 0.9f; h = 1.3f; break;
            case LAPILLO: w = 0.7f; h = 0.7f; break;
            case SALAMANDER: w = 1.1f; h = 0.6f; break;
            case FROG: w = 0.8f; h = 0.6f; break;
            case PLANT: w = 0.8f; h = 0.8f; break;
            case STAR: w = 0.7f; h = 0.7f; break;
            case SUNNY: w = 0.9f; h = 0.9f; break;
            default: w = 0.8f; h = 0.66f; break;
        }
        x = tx + (1 - w) / 2;
        y = ty + 1 - h;
        if (type == CROW || type == STORM) {
            baseY = ty - h / 2;
            y = baseY;
            x = tx;
        }
        if (type == CHESTNUT || type == OWL || type == LAPILLO || type == STAR || type == SUNNY) {     // sospesi: la quota è quella indicata
            baseY = ty;
            y = ty;
            x = tx;
        }
        x0 = x;
        y0 = y;
        t = (tx * 0.37f) % 3f;                 // sfasati tra loro
        hop = 0.6f;
    }
}
