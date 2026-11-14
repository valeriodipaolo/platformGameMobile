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
    public static final int R3_WOOD = 64, R3_FIRTREE = 65, R3_LEAF = 66;
    // livelli d'autunno (tools/gen_autumn.py)
    public static final int AU_SKY = 67, AU_MOUNTAINS = 68, AU_HILLS = 69, AU_GRASS = 70, AU_GRASS_L = 71, AU_GRASS_R = 72;
    public static final int AU_DIRT = 73, AU_DIRT_L = 74, AU_DIRT_R = 75, AU_FRINGE = 76, AU_BRICK = 77, AU_BONUS = 78, AU_USED = 79, AU_STONE = 80;
    public static final int HOG = 81, CROW = 83, ACORN = 85, ACORN_FLAT = 87;      // ricci, corvo e ghianda: 2 fotogrammi ciascuno
    public static final int R3A_GRASS = 88, R3A_FOREST = 89, R3A_TUFT = 90, R3A_FLOWERS = 91, R3A_CROWN = 92, R3A_SKY = 93, R3A_MOUNTAINS = 94, R3A_TREE = 95, R3A_FOLIAGE = 96;
    public static final int COUNT = 97;

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
        "r3_wood", "r3_firtree", "r3_leaf",
        "au_sky", "au_mountains", "au_hills", "au_grass", "au_grass_l", "au_grass_r", "au_dirt", "au_dirt_l", "au_dirt_r",
        "au_fringe", "au_brick", "au_bonus", "au_used", "au_stone",
        "hog_0", "hog_1", "crow_0", "crow_1", "acorn_0", "acorn_1", "acorn_flat",
        "r3a_grass", "r3a_forest", "r3a_tuft", "r3a_flowers", "r3a_crown", "r3a_sky", "r3a_mountains", "r3a_tree", "r3a_foliage",
    };
}
