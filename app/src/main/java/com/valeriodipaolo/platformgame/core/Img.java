package com.valeriodipaolo.platformgame.core;

/** Elenco delle immagini del gioco: l'indice è la posizione in NAMES (nome del file PNG senza estensione). */
public final class Img {
    private Img() {}

    public static final int SKY = 0, MOUNTAINS = 1, HILLS = 2;
    public static final int GRASS = 3, DIRT = 4, FRINGE = 5, BRICK = 6, BONUS = 7, USED = 8, STONE = 9;
    public static final int GEM = 10;           // pezzo di gemma, 6 fotogrammi: GEM .. GEM+5
    public static final int HERO_IDLE = 16;
    public static final int HERO_RUN = 17;      // 4 fotogrammi
    public static final int HERO_JUMP = 21;
    public static final int ENEMY = 22;         // 2 fotogrammi
    public static final int ENEMY_FLAT = 24;
    public static final int POLE = 25, POLE_TOP = 26, FLAG = 27;
    public static final int BTN_LEFT = 28, BTN_RIGHT = 29, BTN_JUMP = 30;
    public static final int BTN_SPIN = 31;
    public static final int BATTERY = 32, CHIP = 33, GEM_FULL = 34, SPIN_FX = 35;
    public static final int HERO_SPIN = 36;     // 12 pose della giravolta: HERO_SPIN .. HERO_SPIN+11
    public static final int SPIN_POSES = 12;
    public static final int TEX_GRASS = 48, SHADOW = 49, JOY_BASE = 50, JOY_KNOB = 51;
    // texture realistiche del livello 3D (tools/gen_assets3d.py)
    public static final int R3_GRASS = 52, R3_FOREST = 53, R3_DIRT = 54, R3_ROCK = 55, R3_BARK = 56, R3_BRANCH = 57;
    public static final int R3_TUFT = 58, R3_FERN = 59, R3_FLOWERS = 60, R3_RAY = 61, R3_SKY = 62, R3_MOUNTAINS = 63;
    public static final int COUNT = 64;

    public static final String[] NAMES = {
        "bg_sky", "bg_mountains", "bg_hills",
        "tile_grass", "tile_dirt", "grass_fringe", "tile_brick", "tile_bonus", "tile_used", "tile_stone",
        "gem_0", "gem_1", "gem_2", "gem_3", "gem_4", "gem_5",
        "hero_idle", "hero_run_0", "hero_run_1", "hero_run_2", "hero_run_3", "hero_jump",
        "enemy_0", "enemy_1", "enemy_flat",
        "pole", "pole_top", "flag",
        "btn_left", "btn_right", "btn_jump", "btn_spin",
        "battery", "chip", "gem_full", "spin_fx",
        "hero_spin_0", "hero_spin_1", "hero_spin_2", "hero_spin_3", "hero_spin_4", "hero_spin_5",
        "hero_spin_6", "hero_spin_7", "hero_spin_8", "hero_spin_9", "hero_spin_10", "hero_spin_11",
        "tex_grass", "shadow", "joy_base", "joy_knob",
        "r3_grass", "r3_forest", "r3_dirt", "r3_rock", "r3_bark", "r3_branch",
        "r3_tuft", "r3_fern", "r3_flowers", "r3_ray", "r3_sky", "r3_mountains",
    };
}
