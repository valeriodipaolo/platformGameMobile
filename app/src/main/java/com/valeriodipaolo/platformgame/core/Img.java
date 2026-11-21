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
    public static final int FR_CRUMBLE = 157, FR_VENT = 158, R3C_VENT = 159;
    // livelli di primavera (tools/gen_spring.py)
    public static final int SP_SKY = 160, SP_MOUNTAINS = 161, SP_HILLS = 162, SP_GRASS = 163, SP_GRASS_L = 164, SP_GRASS_R = 165,
            SP_DIRT = 166, SP_DIRT_L = 167, SP_DIRT_R = 168, SP_FRINGE = 169, SP_BRICK = 170, SP_BONUS = 171, SP_USED = 172, SP_STONE = 173,
            SP_SPRING = 174, SP_THORN = 175, SP_WATER = 176, SP_PETAL = 177;
    public static final int R3D_GRASS = 178, R3D_FOREST = 179, R3D_TUFT = 180, R3D_FLOWERS = 181, R3D_CROWN = 182, R3D_TREE = 183,
            R3D_FOLIAGE = 184, R3D_SKY = 185, R3D_MOUNTAINS = 186, R3D_WATER = 187, R3D_SPRING = 188;
    // livelli del cielo (tools/gen_heaven.py)
    public static final int HV_SKY = 189, HV_MOUNTAINS = 190, HV_HILLS = 191, HV_GRASS = 192, HV_GRASS_L = 193, HV_GRASS_R = 194,
            HV_DIRT = 195, HV_DIRT_L = 196, HV_DIRT_R = 197, HV_FRINGE = 198, HV_BRICK = 199, HV_BONUS = 200, HV_USED = 201, HV_STONE = 202,
            HV_SPRING = 203, HV_CRUMBLE = 204, HV_SEA = 205, HV_SPARK = 206;
    public static final int R3H_CLOUD = 207, R3H_BODY = 208, R3H_SEA = 209, R3H_MARBLE = 210, R3H_SKY = 211, R3H_MOUNTAINS = 212, R3H_SPRING = 213;
    public static final int HV_STORM = 214, HV_STORM_C = 215, HV_WHIRL = 216, HV_WHIRL_B = 217, HV_BOLT = 218, HV_SUN = 219, HV_PAD = 220;
    public static final int FX_MOLE = 221, FX_MOUND = 222, FX_SNAIL = 223, FX_SHELL = 224, FX_CHESTNUT = 225, FX_OWL_0 = 226, FX_OWL_1 = 227, FX_PENGUIN = 228, FX_SNOWMAN = 229, FX_SNOWBALL = 230, FX_LAPILLO = 231, FX_SALA = 232, FX_SALA_B = 233, FX_BREATH = 234, FX_FROG = 235, FX_FROG_J = 236, FX_PLANT = 237, FX_PLANT_O = 238, FX_STAR = 239, FX_SUNNY = 240, FX_CATAPULT = 241, FX_CATAPULT_B = 242;
    public static final int FOE_SKIN = 243, FOE_SCALE = 244, FOE_SHELL = 245, FOE_SNOW = 246, FOE_DOWN = 247, FOE_EMBER = 248, FOE_PLASMA = 249, FOE_PETAL = 250, FOE_MAHOGANY = 251;
    public static final int HERO_METAL = 252, HERO_LEATHER = 253, FX_SLUG = 254;
    /** Ciclo di corsa fluido a 24 fotogrammi: i primi quattro sono HERO_RUN, gli altri venti seguono da HERO_RUN_X (hero_run_4 .. hero_run_23). */
    public static final int RUN_FRAMES = 24, HERO_RUN_X = 255;
    // isola tropicale (tools/gen_jungle.py)
    public static final int R3J_GRASS = 275, R3J_FOREST = 276, R3J_TUFT = 277, R3J_FLOWERS = 278, R3J_CROWN = 279, R3J_TREE = 280, R3J_FOLIAGE = 281,
            R3J_SKY = 282, R3J_MOUNTAINS = 283, R3J_SAND = 284, R3J_WATER = 285, R3J_TEMPLE = 286, R3J_DUSK = 287, R3J_NIGHT = 288,
            R3J_CRATE = 289, R3J_TNT = 290, R3J_NITRO = 291, R3J_BOUNCE = 292, R3J_CHECK = 293, R3J_FROND = 294;
    // deserto, fabbrica, miniera (tools/gen_worlds.py)
    public static final int R3S_SAND = 295, R3S_DUNE = 296, R3S_STONE = 297, R3S_QUICK = 298, R3S_SKY = 299, R3S_MOUNTAINS = 300, R3F_FLOOR = 301, R3F_PANEL = 302, R3F_BLOCK = 303, R3F_BELT = 304, R3F_SKY = 305, R3F_MOUNTAINS = 306, R3M_ROCK = 307, R3M_WALL = 308, R3M_BLOCK = 309, R3M_SKY = 310, R3M_MOUNTAINS = 311;
    /** Nemici realistici (tools/gen_foes_real.py): piumaggio dell'allocco, remiganti, piumino del pinguino. */
    public static final int FOE_OWL = 312, FOE_OWLWING = 313, FOE_PENGFUR = 314;
    public static final int COUNT = 315;


    /** Fotogramma j (0..23) del ciclo di corsa. */
    public static int runFrame(int j) {
        return j < 4 ? HERO_RUN + j : HERO_RUN_X + (j - 4);
    }


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
        "fr_crumble", "fr_vent", "r3c_vent",
        "sp_sky", "sp_mountains", "sp_hills", "sp_grass", "sp_grass_l", "sp_grass_r", "sp_dirt", "sp_dirt_l", "sp_dirt_r", "sp_fringe",
        "sp_brick", "sp_bonus", "sp_used", "sp_stone", "sp_spring", "sp_thorn", "sp_water", "sp_petal",
        "r3d_grass", "r3d_forest", "r3d_tuft", "r3d_flowers", "r3d_crown", "r3d_tree", "r3d_foliage", "r3d_sky", "r3d_mountains", "r3d_water", "r3d_spring",
        "hv_sky", "hv_mountains", "hv_hills", "hv_grass", "hv_grass_l", "hv_grass_r", "hv_dirt", "hv_dirt_l", "hv_dirt_r", "hv_fringe",
        "hv_brick", "hv_bonus", "hv_used", "hv_stone", "hv_spring", "hv_crumble", "hv_sea", "hv_spark",
        "r3h_cloud", "r3h_body", "r3h_sea", "r3h_marble", "r3h_sky", "r3h_mountains", "r3h_spring", "hv_storm", "hv_storm_c", "hv_whirl", "hv_whirl_b", "hv_bolt", "hv_sun", "hv_pad",
        "fx_mole", "fx_mound", "fx_snail", "fx_shell", "fx_chestnut", "fx_owl_0", "fx_owl_1", "fx_penguin", "fx_snowman", "fx_snowball", "fx_lapillo", "fx_sala", "fx_sala_b", "fx_breath", "fx_frog", "fx_frog_j", "fx_plant", "fx_plant_o", "fx_star", "fx_sunny", "fx_catapult", "fx_catapult_b",
        "foe_skin", "foe_scale", "foe_shell", "foe_snow", "foe_down", "foe_ember", "foe_plasma", "foe_petal", "foe_mahogany",
        "hero_metal", "hero_leather", "fx_slug",
        "hero_run_4", "hero_run_5", "hero_run_6", "hero_run_7", "hero_run_8", "hero_run_9", "hero_run_10", "hero_run_11", "hero_run_12", "hero_run_13", "hero_run_14", "hero_run_15", "hero_run_16", "hero_run_17", "hero_run_18", "hero_run_19", "hero_run_20", "hero_run_21", "hero_run_22", "hero_run_23",
        "r3j_grass", "r3j_forest", "r3j_tuft", "r3j_flowers", "r3j_crown", "r3j_tree", "r3j_foliage", "r3j_sky", "r3j_mountains", "r3j_sand",
        "r3j_water", "r3j_temple", "r3j_dusk", "r3j_night", "r3j_crate", "r3j_tnt", "r3j_nitro", "r3j_bounce", "r3j_check", "r3j_frond",
        "r3s_sand", "r3s_dune", "r3s_stone", "r3s_quick", "r3s_sky", "r3s_mountains", "r3f_floor", "r3f_panel", "r3f_block", "r3f_belt", "r3f_sky", "r3f_mountains", "r3m_rock", "r3m_wall", "r3m_block", "r3m_sky", "r3m_mountains",
        "foe_owl", "foe_owlwing", "foe_pengfur"
    };
}
