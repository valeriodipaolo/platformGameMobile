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
    public static final int R3A_GRASS = 88, R3A_FOREST = 89, R3A_TUFT = 90, R3A_FLOWERS = 91, R3A_CROWN = 92, R3A_SKY = 93, R3A_MOUNTAINS = 94, R3A_TREE = 95, R3A_FOLIAGE = 96, GATE = 97, GATE_DOOR = 98;
    public static final int AR_SKY = 99, AR_MOUNTAINS = 100, AR_HILLS = 101, AR_GRASS = 102, AR_GRASS_L = 103, AR_GRASS_R = 104,
            AR_DIRT = 105, AR_DIRT_L = 106, AR_DIRT_R = 107, AR_FRINGE = 108, AR_BRICK = 109, AR_BONUS = 110, AR_USED = 111, AR_STONE = 112;
    public static final int R3B_GRASS = 113, R3B_FOREST = 114, R3B_TUFT = 115, R3B_FLOWERS = 116, R3B_BRANCH = 117, R3B_TREE = 118,
            R3B_SKY = 119, R3B_MOUNTAINS = 120, R3B_AURORA = 121, R3B_FLAKE = 122, R3B_ICE = 123;
    /** Texture dei nemici 3D (chiare: il colore lo danno i vertici). */
    public static final int FOE_FUR = 124, FOE_PLUME = 125, FOE_CARD = 126, FOE_NUT = 127, FOE_CAP = 128, FOE_CHITIN = 129;
    /** Dettagli dei livelli 3D e stoffa del personaggio. */
    public static final int R3_TRAIL = 130, R3_CLOTH = 131;
    // livelli con l'incendio (tools/gen_fire.py)
    public static final int FR_SKY = 132, FR_MOUNTAINS = 133, FR_HILLS = 134, FR_GRASS = 135, FR_GRASS_L = 136, FR_GRASS_R = 137,
            FR_DIRT = 138, FR_DIRT_L = 139, FR_DIRT_R = 140, FR_FRINGE = 141, FR_BRICK = 142, FR_BONUS = 143, FR_USED = 144, FR_STONE = 145;
    public static final int R3C_GRASS = 146, R3C_FOREST = 147, R3C_TUFT = 148, R3C_FLOWERS = 149, R3C_BRANCH = 150, R3C_TREE = 151,
            R3C_SKY = 152, R3C_MOUNTAINS = 153, R3C_LAVA = 154, R3C_EMBER = 155, R3C_FLAME = 156;
    public static final int FR_CRUMBLE = 157;
    public static final int COUNT = 158;

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
        "r3a_grass", "r3a_forest", "r3a_tuft", "r3a_flowers", "r3a_crown", "r3a_sky", "r3a_mountains", "r3a_tree", "r3a_foliage", "gate", "gate_door",
        "ar_sky", "ar_mountains", "ar_hills", "ar_grass", "ar_grass_l", "ar_grass_r", "ar_dirt", "ar_dirt_l", "ar_dirt_r", "ar_fringe",
        "ar_brick", "ar_bonus", "ar_used", "ar_stone",
        "r3b_grass", "r3b_forest", "r3b_tuft", "r3b_flowers", "r3b_branch", "r3b_tree", "r3b_sky", "r3b_mountains", "r3b_aurora", "r3b_flake", "r3b_ice",
        "foe_fur", "foe_plume", "foe_card", "foe_nut", "foe_cap", "foe_chitin",
        "r3_trail", "r3_cloth",
        "fr_sky", "fr_mountains", "fr_hills", "fr_grass", "fr_grass_l", "fr_grass_r", "fr_dirt", "fr_dirt_l", "fr_dirt_r", "fr_fringe",
        "fr_brick", "fr_bonus", "fr_used", "fr_stone",
        "r3c_grass", "r3c_forest", "r3c_tuft", "r3c_flowers", "r3c_branch", "r3c_tree", "r3c_sky", "r3c_mountains", "r3c_lava", "r3c_ember", "r3c_flame",
        "fr_crumble",
    };
}
