package com.valeriodipaolo.platformgame.core3d;

import com.valeriodipaolo.platformgame.core.Img;

import java.util.Random;

/** Costruisce i modelli del livello 3D e disegna un fotogramma su un Gfx3D. */
public final class Scene3D {
    private static final float FOG_R = 0.745f, FOG_G = 0.847f, FOG_B = 0.933f;
    private static final float PIT_DEPTH = 5f;
    private static final float SEAM = 3.4f;                    // distanza dal sentiero a cui il prato lascia il posto al sottobosco
    private static final int CH = 16;                          // il paesaggio è diviso in blocchi lunghi 16 m: si disegnano solo quelli vicini

    // tipi di mesh a blocchi
    static final int K_GRASS = 0, K_DIRT = 1, K_STONE = 2, K_BANK = 3, K_FOREST = 4, K_TRUNK = 5, K_CROWN = 6,
            K_TUFT = 7, K_FERN = 8, K_FLOWER = 9, K_ROCK = 10, K_RAY = 11, K_FIR = 12, K_BLOB = 13, K_PROP = 14, K_GLOW = 15, K_TRAIL = 16, K_N = 17;
    /** Sentiero di terra battuta che serpeggia in mezzo al prato: mezza larghezza e posizione del centro. */
    static final float TRAIL_HW = 0.8f;

    static float trailX(float z) {
        return 1.25f * (float) Math.sin(z * 0.19f) + 0.55f * (float) Math.sin(z * 0.071f + 1.3f);
    }
    private static final int[] K_TEX_SUMMER = {Img.R3_GRASS, Img.R3_DIRT, Img.R3_ROCK, Img.R3_GRASS, Img.R3_FOREST, Img.R3_BARK,
            Img.R3_BRANCH, Img.R3_TUFT, Img.R3_FERN, Img.R3_FLOWERS, Img.R3_ROCK, Img.R3_RAY, Img.R3_FIRTREE, Img.R3A_FOLIAGE};
    private static final int[] K_TEX_AUTUMN = {Img.R3A_GRASS, Img.R3_DIRT, Img.R3_ROCK, Img.R3A_GRASS, Img.R3A_FOREST, Img.R3_BARK,
            Img.R3A_CROWN, Img.R3A_TUFT, Img.R3_FERN, Img.R3A_FLOWERS, Img.R3_ROCK, Img.R3_RAY, Img.R3A_TREE, Img.R3A_FOLIAGE};
    private static final int[] K_TEX_SPRING = {Img.R3D_GRASS, Img.R3_DIRT, Img.R3_ROCK, Img.R3D_GRASS, Img.R3D_FOREST, Img.R3_BARK,
            Img.R3D_CROWN, Img.R3D_TUFT, Img.R3_FERN, Img.R3D_FLOWERS, Img.R3_ROCK, Img.R3_RAY, Img.R3D_TREE, Img.R3D_FOLIAGE};
    private static final int[] K_TEX_AURORA = {Img.R3B_GRASS, Img.R3B_ICE, Img.R3_ROCK, Img.R3B_GRASS, Img.R3B_FOREST, Img.R3_BARK,
            Img.R3B_BRANCH, Img.R3B_TUFT, Img.R3B_TUFT, Img.R3B_FLOWERS, Img.R3_ROCK, Img.R3_RAY, Img.R3B_TREE, Img.R3B_ICE};
    private static final int[] K_TEX_FIRE = {Img.R3C_GRASS, Img.R3C_LAVA, Img.R3_ROCK, Img.R3C_GRASS, Img.R3C_FOREST, Img.R3_BARK,
            Img.R3C_BRANCH, Img.R3C_TUFT, Img.R3C_TUFT, Img.R3C_FLOWERS, Img.R3_ROCK, Img.R3_RAY, Img.R3C_TREE, Img.R3C_BRANCH};
    private static final int[] K_TEX_JUNGLE = {Img.R3J_GRASS, Img.R3J_SAND, Img.R3J_TEMPLE, Img.R3J_GRASS, Img.R3J_FOREST, Img.R3_BARK,
            Img.R3J_CROWN, Img.R3J_TUFT, Img.R3_FERN, Img.R3J_FLOWERS, Img.R3_ROCK, Img.R3_RAY, Img.R3J_TREE, Img.R3J_FOLIAGE};
    /** Chiome della giungla: verdi profondi, qualche verde acido e un albero in fiore. */
    static final float[][] JUNGLE_TINT = {{0.55f, 0.95f, 0.5f}, {0.4f, 0.8f, 0.42f}, {0.7f, 1.0f, 0.45f}, {0.45f, 0.9f, 0.6f}, {0.62f, 0.86f, 0.38f}, {1.0f, 0.62f, 0.5f}};
    private static final int[] K_TEX_DESERT = {Img.R3S_SAND, Img.R3S_DUNE, Img.R3S_STONE, Img.R3S_SAND, Img.R3S_DUNE, Img.R3_BARK,
            Img.R3J_CROWN, Img.R3J_TUFT, Img.R3_FERN, Img.R3J_FLOWERS, Img.R3_ROCK, Img.R3_RAY, Img.R3J_TREE, Img.R3J_FOLIAGE};
    private static final int[] K_TEX_FACTORY = {Img.R3F_FLOOR, Img.R3F_PANEL, Img.R3F_BLOCK, Img.R3F_PANEL, Img.R3F_PANEL, Img.R3_BARK,
            Img.R3J_CROWN, Img.R3J_TUFT, Img.R3_FERN, Img.R3J_FLOWERS, Img.R3F_PANEL, Img.R3_RAY, Img.R3J_TREE, Img.R3J_FOLIAGE};
    private static final int[] K_TEX_MINE = {Img.R3M_ROCK, Img.R3M_WALL, Img.R3M_BLOCK, Img.R3M_ROCK, Img.R3M_WALL, Img.R3_BARK,
            Img.R3J_CROWN, Img.R3J_TUFT, Img.R3_FERN, Img.R3J_FLOWERS, Img.R3M_WALL, Img.R3_RAY, Img.R3J_TREE, Img.R3J_FOLIAGE};
    private static final int[] K_TEX_HEAVEN = {Img.R3H_CLOUD, Img.R3H_BODY, Img.R3H_MARBLE, Img.R3H_CLOUD, Img.R3H_SEA, Img.R3_BARK,
            Img.R3D_CROWN, Img.R3D_TUFT, Img.R3_FERN, Img.R3D_FLOWERS, Img.R3_ROCK, Img.R3_RAY, Img.R3D_TREE, Img.R3D_FOLIAGE};
    /** Tinte delle chiome d'autunno: arancio, rosso, giallo, ruggine, verde, zucca. */
    static final float[][] AUTUMN_TINT = {{1.0f, 0.76f, 0.42f}, {1.0f, 0.50f, 0.38f}, {1.0f, 0.96f, 0.46f},
            {0.96f, 0.66f, 0.36f}, {0.76f, 0.9f, 0.40f}, {1.0f, 0.60f, 0.28f}};

    /** Tinte delle chiome di primavera: ciliegi rosa e bianchi, qualche albero verde tenero. */
    static final float[][] SPRING_TINT = {{1.0f, 0.78f, 0.88f}, {1.0f, 0.92f, 0.96f}, {1.0f, 0.70f, 0.82f},
            {0.80f, 1.0f, 0.60f}, {1.0f, 0.96f, 0.98f}, {0.72f, 1.0f, 0.55f}};
    /** Estate: cespugli di un verde naturale, più scuro e meno acido. */
    static final float[][] SUMMER_TINT = {{0.62f, 0.74f, 0.50f}, {0.56f, 0.70f, 0.52f}, {0.70f, 0.76f, 0.46f}, {0.52f, 0.66f, 0.48f}};
    float[][] tints() { return jungle ? JUNGLE_TINT : spring ? SPRING_TINT : ktex == K_TEX_SUMMER ? SUMMER_TINT : AUTUMN_TINT; }

    private static final class Tree {
        float x, y, z, h, rad, tint;
        int row;
    }

    private final float[] view = new float[16], proj = new float[16], m = new float[16], m2 = new float[16];
    private final float[] ident = Mat4.identity();

    private Mesh seaPlane, sky, mountains, pitFloor, pitWater, padDome, padTop, thornMesh, beeBody, beeWings, hiveMesh, hiveTree, hiveBranch, hiveRope;
    private Mesh[][] land;                       // [tipo][blocco]
    private MeshBuilder[][] mbs;
    private int zBase, nChunks;
    private Tree[] trees;
    private World3D lvl;
    private float[] fadeG;
    private int fadeZ0, fadeNz;
    private Mesh leaf, crateStar;
    private final FoeModels foes = new FoeModels();
    boolean autumn, aurora, fire, spring, heaven, jungle;
    /** Isola tropicale: 14 giungla di giorno, 16 tempio al tramonto, 18 fiume di notte. */
    int isle;
    /** Mondi senza bosco: 20 deserto, 22 fabbrica, 24 miniera (0 negli altri). */
    int dry;
    private final Decor decor = new Decor(this);
    private Mesh wing;
    private Mesh flake, curtain;
    private int[] ktex = K_TEX_SUMMER;
    private float pitD = PIT_DEPTH;
    private float fogR = FOG_R, fogG = FOG_G, fogB = FOG_B;
    /** Quanto il bordo dei burroni si arrotonda verso il basso (0 = spigolo vivo). */
    private float lip;
    private Mesh flame, ember, logMesh, vent;
    private Mesh crate, gem, battery, batteryGlow, shadow, ring, gateStone, gateDoor, catapult, gateLight;
    private final HeroModel hero = new HeroModel();
    private int builtFor = -1;
    private float camX, camY = 0, camZ, camEye;
    private boolean camEyeInit;
    private float backK, nearZ, farZ;
    private boolean back;
    private boolean camInit;
    final float[] lt = new float[3];

    // ------------------------------------------------------------------ rumore e luce calcolata in anticipo

    private static float hash(int x, int z, int s) {
        int h = x * 374761393 + z * 668265263 + s * 1274126177;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFFFF) / 16777215f;
    }

    static float vnoise(float x, float z, int s) {
        int xi = (int) Math.floor(x), zi = (int) Math.floor(z);
        float fx = x - xi, fz = z - zi;
        fx = fx * fx * (3 - 2 * fx);
        fz = fz * fz * (3 - 2 * fz);
        float a = hash(xi, zi, s), b = hash(xi + 1, zi, s), c = hash(xi, zi + 1, s), d = hash(xi + 1, zi + 1, s);
        return (a + (b - a) * fx) * (1 - fz) + (c + (d - c) * fx) * fz;
    }

    /**
     * Moltiplicatore di colore in (x, z): variazione dell'erba, ombre degli alberi proiettate dal sole (più fredde e azzurre,
     * perché lì arriva solo la luce del cielo) con macchie di luce tra le foglie. Risultato in lt[0..2].
     */
    void lightAt(float x, float z, float base) {
        float macro = 0.84f + 0.32f * vnoise(x * 0.17f, z * 0.17f, 9) + 0.08f * (vnoise(x * 0.9f, z * 0.9f, 4) - 0.5f);
        float dry = vnoise(x * 0.11f + 30, z * 0.11f, 3);
        float r = macro * (1f + 0.22f * dry), g = macro * (1f + 0.04f * dry), b = macro * (1f - 0.28f * dry);
        // ombre
        float sdx = -Gfx3D.LX, sdz = -Gfx3D.LZ, sl = (float) Math.sqrt(sdx * sdx + sdz * sdz);
        sdx /= sl;
        sdz /= sl;
        float sunlit = 1f;
        int lo = firstTree(z - 16f);
        for (int k = lo; k < trees.length; k++) {
            Tree t = trees[k];
            if (t.z > z + 6f) break;
            float yc = t.h * 0.55f;
            float cx = t.x - Gfx3D.LX / Gfx3D.LY * yc, cz = t.z - Gfx3D.LZ / Gfx3D.LY * yc;
            float dx = x - cx, dz = z - cz;
            float along = dx * sdx + dz * sdz, perp = -dx * sdz + dz * sdx;
            float major = t.rad * 1.2f + t.h * 0.42f, minor = t.rad * 1.3f;
            float d2 = (along / major) * (along / major) + (perp / minor) * (perp / minor);
            if (d2 >= 1f) continue;
            float cover = 1f - Math.max(0f, (float) Math.sqrt(d2) - 0.5f) * 2f;
            cover = Math.max(0f, Math.min(1f, cover));
            cover *= 0.45f + 0.55f * vnoise(x * 1.3f + 11, z * 1.3f, 5);          // spiragli di luce tra le foglie
            sunlit *= 1f - cover * 0.92f;
        }
        float sh = 1f - sunlit;
        r *= (1f - sh) + sh * 0.50f;
        g *= (1f - sh) + sh * 0.58f;
        b *= (1f - sh) + sh * 0.72f;
        if (this.dry == 24) {                                     // in miniera la luce viene solo dalle lanterne, una ogni sette passi
            float dz = ((z - 2f) % 7f + 7f) % 7f;
            dz = Math.min(dz, 7f - dz);
            float k = 0.22f + 0.85f * (float) Math.exp(-dz * dz / 4f) * (float) Math.exp(-Math.max(0f, Math.abs(x) - 2.5f) * 0.4f);
            r *= k * 1.15f; g *= k * 0.92f; b *= k * 0.7f;
        }
        lt[0] = r * base;
        lt[1] = g * base;
        lt[2] = b * base;
    }

    private int firstTree(float z) {
        int lo = 0, hi = trees.length;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (trees[mid].z < z) lo = mid + 1; else hi = mid;
        }
        return lo;
    }

    /** Occlusione ambientale ai vertici del prato: più scuro accanto ai blocchi e ai burroni. */
    private static float aoAt(World3D w, float x, float z) {
        int cx = (int) Math.floor(x + World3D.HALF + 1e-3f), cz = (int) Math.floor(z + 1e-3f);
        float ao = 1f;
        for (int dx = -1; dx <= 0; dx++) {
            for (int dz = -1; dz <= 0; dz++) {
                int xi = cx + dx, zi = cz + dz;
                if (xi < 0 || xi >= World3D.W) continue;
                int h = vis(w, xi, zi);
                if (h == World3D.PIT) ao -= 0.10f;
                else if (h > 0) ao -= 0.13f * Math.min(h, 2);
            }
        }
        return Math.max(0.4f, ao);
    }

    MeshBuilder mb(int kind, float z) {
        int c = Math.max(0, Math.min(nChunks - 1, (int) Math.floor((z - zBase) / CH)));
        if (mbs[kind][c] == null) mbs[kind][c] = new MeshBuilder();
        return mbs[kind][c];
    }

    /** Vertice con colore dell'ombra/luce calcolata. */
    private void gv(MeshBuilder b, float x, float y, float z, float nx, float ny, float nz, float u, float v, float base, float ao) {
        lightAt(x, z, base);
        b.colorf(lt[0] * ao, lt[1] * ao, lt[2] * ao);
        b.vert(x, y, z, nx, ny, nz, u, v);
    }

    // ---- forma del terreno

    /** Quanto il prato del sentiero può ondulare in ogni punto: 0 accanto a burroni e blocchi, 1 in campo aperto. */
    private void buildFade(World3D w, int z0, int z1) {
        int nx = World3D.W * 2 + 1;
        fadeZ0 = z0;
        fadeNz = (z1 - z0) * 2 + 1;
        fadeG = new float[nx * fadeNz];
        for (int iz = 0; iz < fadeNz; iz++) {
            for (int ix = 0; ix < nx; ix++) {
                float x = -World3D.HALF + ix * 0.5f, z = z0 + iz * 0.5f, best = 9f;
                int cx = (int) Math.floor(x + World3D.HALF), cz = (int) Math.floor(z);
                for (int xi = cx - 2; xi <= cx + 2; xi++) {
                    if (xi < 0 || xi >= World3D.W) continue;
                    for (int zi = cz - 2; zi <= cz + 2; zi++) {
                        if (vis(w, xi, zi) == 0) continue;
                        float x0 = xi - World3D.HALF;
                        float dx = Math.max(0, Math.max(x0 - x, x - (x0 + 1))), dz = Math.max(0, Math.max(zi - z, z - (zi + 1)));
                        best = Math.min(best, (float) Math.sqrt(dx * dx + dz * dz));
                    }
                }
                float t = Math.min(1f, best / 1.5f);
                fadeG[iz * nx + ix] = t * t * (3 - 2 * t);
            }
        }
    }

    float fadeAt(float x, float z) {
        int nx = World3D.W * 2 + 1;
        float gx = (Math.max(-World3D.HALF, Math.min(World3D.HALF, x)) + World3D.HALF) * 2f;
        float gz = Math.max(0, Math.min(fadeNz - 1.001f, (z - fadeZ0) * 2f));
        int ix = Math.min(nx - 2, (int) gx), iz = (int) gz;
        float fx = gx - ix, fz = gz - iz;
        float a = fadeG[iz * nx + ix], b = fadeG[iz * nx + ix + 1], c = fadeG[(iz + 1) * nx + ix], d = fadeG[(iz + 1) * nx + ix + 1];
        return (a + (b - a) * fx) * (1 - fz) + (c + (d - c) * fx) * fz;
    }

    /** Ondulazione del prato (solo estetica: le regole del gioco restano su terreno piano). */
    private float bumpAt(float x, float z) {
        if (fadeG == null) return 0;
        return fadeAt(x, z) * 0.13f * ((vnoise(x * 0.35f, z * 0.35f, 31) - 0.5f) * 1.2f + (vnoise(x * 0.9f, z * 0.9f, 32) - 0.5f) * 0.8f);
    }

    private static float sm(float t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    /** Altezza del terreno: sentiero ondulato, sponde che salgono con una curva dolce, bosco a dossi. */
    float groundAt(float x, float z) {
        float y = groundBase(x, z);
        if (lip <= 0) return y;
        return y - lipDrop(x, z);
    }

    /** Il bordo dei burroni si piega verso il basso con un quarto di cerchio (solo estetica), seguendo il contorno frastagliato. */
    private float lipDrop(float x, float z) {
        float f = edgeField(x, z);
        if (f >= lip) return 0f;
        if (f <= 0f) return lip;
        float k = lip - f;
        return lip - (float) Math.sqrt(Math.max(0f, lip * lip - k * k));
    }

    /**
     * Distanza dal bordo frastagliato dei burroni: negativa dentro il vuoto. Il contorno vero (quello del gioco) è il quadrato
     * della casella; quello disegnato si mangia da 0 a ~0,4 m di terreno con un rumore, così le voragini non sono rettangoli.
     * Solo grafica: chi gioca non cade prima, al massimo vede il vuoto un po' più largo.
     */
    float edgeField(float x, float z) {
        if (heaven) return 2f;                                             // in cielo i bordi restano quelli delle nuvole
        int cx = (int) Math.floor(x + World3D.HALF), cz = (int) Math.floor(z);
        cx = Math.max(-1, Math.min(World3D.W, cx));
        float best = 2f;
        for (int xi = cx - 2; xi <= cx + 2; xi++) {
            if (xi < 0 || xi >= World3D.W) continue;
            for (int zi = cz - 2; zi <= cz + 2; zi++) {
                if (vis(lvl, xi, zi) != World3D.PIT) continue;
                float x0 = xi - World3D.HALF;
                float dx = Math.max(0, Math.max(x0 - x, x - (x0 + 1))), dz = Math.max(0, Math.max(zi - z, z - (zi + 1)));
                best = Math.min(best, (float) Math.sqrt(dx * dx + dz * dz));
            }
        }
        if (best >= 2f) return best;
        float j = jag(x, z);
        // passerelle strette (vuoto da due lati opposti): il bordo resta quasi quello vero, se no la passerella sparirebbe
        int gx = (int) Math.floor(x + World3D.HALF);
        if (gx >= 0 && gx < World3D.W && vis(lvl, gx, cz) != World3D.PIT) {
            boolean narrow = (gx > 0 && gx < World3D.W - 1 && vis(lvl, gx - 1, cz) == World3D.PIT && vis(lvl, gx + 1, cz) == World3D.PIT)
                    || (vis(lvl, gx, cz - 1) == World3D.PIT && vis(lvl, gx, cz + 1) == World3D.PIT)
                    || ((gx == 0 || gx == World3D.W - 1) && vis(lvl, gx == 0 ? 1 : gx - 1, cz) == World3D.PIT);
            if (narrow) j = Math.min(j, 0.1f);
        }
        return best - j;
    }

    /** Quanto terreno si mangia il burrone in quel punto (0..~0,8): dentro il corridoio e anche nei prati ai lati. */
    private float jag(float x, float z) {
        float n = 0.5f * vnoise(x * 0.9f, z * 0.9f, 60) + 0.32f * vnoise(x * 2.1f, z * 2.1f, 61) + 0.18f * vnoise(x * 5f + 7, z * 5f, 62);
        float k = Math.max(0f, Math.min(1f, (n - 0.22f) / 0.56f));
        k = k * k * (3 - 2 * k);
        // dove si salta (in mezzo al corridoio) il bordo resta vicino a quello vero; verso i lati il burrone si allarga e si
        // sfrangia molto, fuori dal corridoio diventa una spaccatura irregolare che entra nel prato
        float e = Math.max(0f, Math.min(1f, (Math.abs(x) - (World3D.HALF - 1.3f)) / 1.3f));
        float amp = 0.45f + 0.75f * e + (Math.abs(x) > World3D.HALF ? 0.35f : 0f);
        return 0.03f + amp * k + 0.25f * e;
    }

    private float groundBase(float x, float z) {
        float d = Math.abs(x) - World3D.HALF, y = bumpAt(x, z);
        if (d <= 0) return y;
        if (heaven) return y - 4.5f * sm(d / 0.9f) + 0.7f * (vnoise(x * 0.09f + 3, z * 0.09f, 31) - 0.5f) * sm(d / 5f);
        if (dry == 24) return y + 7.5f * sm((d - 1.6f) / 2.6f) + 1.2f * (vnoise(x * 0.4f, z * 0.3f, 41) - 0.5f) * sm(d / 2f);     // la gola della miniera: pareti a picco
        if (dry == 22) return y + (d > 1.2f ? 0.6f : 0f) + sm((d - 6f) / 3f) * 2.5f;                                                  // fabbrica: marciapiede e muro di cinta
        if (dry == 20) return y + sm(d / 6f) * 2.6f * vnoise(x * 0.06f + 2, z * 0.05f, 42) + 0.5f * sm(d / 2f);                          // dune
        y += sm(d / SEAM) * (1.2f + 0.7f * vnoise(x * 0.07f + 5, z * 0.07f, 21));
        y += sm(d / 7f) * 1.8f * (vnoise(x * 0.11f, z * 0.11f, 22) - 0.45f);
        y += Math.min(1f, d / 1.5f) * 0.16f * (vnoise(x * 0.7f, z * 0.7f, 23) - 0.5f);
        return y;
    }

    /** Vertice di terreno: altezza e normale dalla forma del terreno, colore = luce calcolata per tinta. */
    private void ground(MeshBuilder b, float x, float z, float u, float v, float tr, float tg, float tb) {
        float e = 0.25f;
        float nx = groundAt(x - e, z) - groundAt(x + e, z), nz = groundAt(x, z - e) - groundAt(x, z + e), ny = 2 * e;
        float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        lightAt(x, z, 1f);
        b.colorf(lt[0] * tr, lt[1] * tg, lt[2] * tb);
        b.vert(x, groundAt(x, z), z, nx / l, ny / l, nz / l, u, v);
    }

    // ------------------------------------------------------------------ modelli fissi

    private void genTrees(int z0, int z1) {
        java.util.ArrayList<Tree> list = new java.util.ArrayList<>();
        Random r = new Random(7);
        if (heaven || dry != 0) { trees = new Tree[0]; return; }
        for (int side = -1; side <= 1; side += 2) {
            for (int row = 0; row < 5; row++) {
                for (float z = z0; z < z1; z += (autumn && row < 2 ? 3.3f + row * 0.6f : 2.1f + row * 0.5f)) {
                    Tree t = new Tree();
                    t.row = row;
                    t.x = side * (World3D.HALF + (lvl.theme == 4 || lvl.theme >= 10 ? 4.4f : 3.0f) + row * 2.6f + r.nextFloat() * 2.0f);
                    t.z = z + r.nextFloat() * 1.8f;
                    t.h = 5.0f + r.nextFloat() * 4.0f + row * 0.5f;
                    t.rad = t.h * 0.25f * (0.85f + 0.3f * r.nextFloat());
                    t.tint = 0.8f + r.nextFloat() * 0.3f;
                    t.y = groundAt(t.x, t.z) - 0.15f;
                    list.add(t);
                }
            }
        }
        trees = list.toArray(new Tree[0]);
        java.util.Arrays.sort(trees, new java.util.Comparator<Tree>() {
            public int compare(Tree a, Tree b) { return Float.compare(a.z, b.z); }
        });
    }

    private void build(World3D w) {
        builtFor = w.length;
        lvl = w;
        heaven = w.theme == 12;
        pitD = heaven ? 2.2f : PIT_DEPTH;
        spring = w.theme == 10;
        jungle = w.theme == 14 || w.theme == 16 || w.theme == 18;
        isle = jungle ? w.theme : 0;
        dry = w.theme == 20 || w.theme == 22 || w.theme == 24 ? w.theme : 0;
        autumn = w.theme == 4 || spring || jungle;       // la primavera (e la giungla) usano gli alberi a chioma larga dell'autunno, con altri colori
        aurora = w.theme == 6;
        fire = w.theme == 8;
        ktex = dry == 20 ? K_TEX_DESERT : dry == 22 ? K_TEX_FACTORY : dry == 24 ? K_TEX_MINE : jungle ? K_TEX_JUNGLE : heaven ? K_TEX_HEAVEN : spring ? K_TEX_SPRING : fire ? K_TEX_FIRE : aurora ? K_TEX_AURORA : autumn ? K_TEX_AUTUMN : K_TEX_SUMMER;
        if (heaven) { fogR = 0.86f; fogG = 0.93f; fogB = 1.0f; lip = 0.5f; }
        else if (spring) { fogR = 0.80f; fogG = 0.90f; fogB = 0.97f; lip = 0.5f; }
        else if (autumn) { fogR = 0.90f; fogG = 0.79f; fogB = 0.64f; lip = 0.5f; }
        if (aurora) { fogR = 0.05f; fogG = 0.10f; fogB = 0.22f; lip = 0.5f; }
        if (fire) { fogR = 0.36f; fogG = 0.15f; fogB = 0.08f; lip = 0.5f; }
        if (lip == 0f) lip = 0.4f;                                    // bordo dei burroni arrotondato ovunque, non a spigolo vivo
        if (isle == 14) { fogR = 0.70f; fogG = 0.88f; fogB = 0.86f; }
        if (isle == 16) { fogR = 0.86f; fogG = 0.56f; fogB = 0.44f; }
        if (isle == 18) { fogR = 0.06f; fogG = 0.10f; fogB = 0.18f; }
        if (dry == 20) { fogR = 0.96f; fogG = 0.86f; fogB = 0.68f; }
        if (dry == 22) { fogR = 0.56f; fogG = 0.44f; fogB = 0.38f; }
        if (dry == 24) { fogR = 0.05f; fogG = 0.04f; fogB = 0.05f; }
        MeshBuilder b;
        int z0 = -8, z1 = w.length + 40;
        zBase = z0;
        nChunks = (z1 - z0) / CH + 1;
        mbs = new MeshBuilder[K_N][nChunks];
        buildFade(w, z0, z1);
        genTrees(z0, z1);

        b = new MeshBuilder();                                         // cielo: occupa la parte alta dello schermo, con la foschia all'orizzonte
        float skyLow = heaven ? -1f : 0.18f;                           // in cielo il fondale riempie tutto lo schermo, anche sotto l'orizzonte
        b.quad(-1, skyLow, 0.999f, 1, skyLow, 0.999f, 1, 1, 0.999f, -1, 1, 0.999f, 0, 0, -1, 0, 0, 1, 1);
        sky = b.build(dry == 20 ? Img.R3S_SKY : dry == 22 ? Img.R3F_SKY : dry == 24 ? Img.R3M_SKY : isle == 16 ? Img.R3J_DUSK : isle == 18 ? Img.R3J_NIGHT : jungle ? Img.R3J_SKY : heaven ? Img.R3H_SKY : spring ? Img.R3D_SKY : fire ? Img.R3C_SKY : aurora ? Img.R3B_SKY : autumn ? Img.R3A_SKY : Img.R3_SKY, false, false);

        b = new MeshBuilder();                                         // montagne lontane
        b.quad(-330, -4, 0, 330, -4, 0, 330, 78, 0, -330, 78, 0, 0, 0, -1, 0, 0, 2, 1);
        mountains = b.build(dry == 20 ? Img.R3S_MOUNTAINS : dry == 22 ? Img.R3F_MOUNTAINS : dry == 24 ? Img.R3M_MOUNTAINS : jungle ? Img.R3J_MOUNTAINS : heaven ? Img.R3H_MOUNTAINS : spring ? Img.R3D_MOUNTAINS : fire ? Img.R3C_MOUNTAINS : aurora ? Img.R3B_MOUNTAINS : autumn ? Img.R3A_MOUNTAINS : Img.R3_MOUNTAINS, true, false);
        b = new MeshBuilder();                                         // tenda d'aurora, molto alta e lontana
        b.quad(-350, 0, 0, 350, 0, 0, 350, 105, 0, -350, 105, 0, 0, 0, -1, 0, 0, 2.4f, 1);
        curtain = b.build(Img.R3B_AURORA, true, false);

        int[] order = {0, 1, 2, 0, 2, 3};
        // ---- corridoio: prato ondulato a quadrati di mezzo metro, pietra e pareti di terra
        for (int z = z0; z < z1; z++) {
            for (int x = 0; x < World3D.W; x++) {
                int h = vis(w, x, z);
                float fx = x - World3D.HALF;
                if (h == World3D.PIT) continue;
                if (h == 0) {
                    MeshBuilder g = mb(K_GRASS, z);
                    boolean nearPit = false;
                    if (lip > 0) {
                        for (int ax = x - 2; ax <= x + 2; ax++) for (int az = z - 2; az <= z + 2; az++) {
                            if (ax >= 0 && ax < World3D.W && vis(w, ax, az) == World3D.PIT) nearPit = true;
                        }
                    }
                    if (nearPit) jaggedCell(w, g, fx, z);
                    else {
                        int sub = 2;
                        float e = 1f / sub;
                        for (int sx = 0; sx < sub; sx++) {
                            for (int sz = 0; sz < sub; sz++) {
                                float qx = fx + sx * e, qz = z + sz * e;
                                float[][] c = {{qx, qz}, {qx + e, qz}, {qx + e, qz + e}, {qx, qz + e}};
                                for (int k : order) {
                                    float ex = Math.abs(c[k][0]) / World3D.HALF;
                                    float t = (1.08f - 0.10f * ex * ex) * aoAt(w, c[k][0], c[k][1]);
                                    ground(g, c[k][0], c[k][1], c[k][0] * 0.5f, c[k][1] * 0.5f, t, t, t);
                                }
                            }
                        }
                    }
                    trailCell(w, x, z);
                } else {
                    // il terreno continua sotto ai massi (in ombra): dagli spigoli arrotondati non si deve vedere il vuoto
                    MeshBuilder gs = mb(K_GRASS, z);
                    float[][] gc = {{fx, z}, {fx + 1, z}, {fx + 1, z + 1}, {fx, z + 1}};
                    for (int k : order) ground(gs, gc[k][0], gc[k][1], gc[k][0] * 0.5f, gc[k][1] * 0.5f, 0.42f, 0.42f, 0.42f);
                    MeshBuilder s = mb(K_STONE, z);
                    boolean round = true;                                  // nessuno spigolo vivo: al minimo uno smusso
                    float rr = autumn || aurora || fire || heaven ? 0.2f : dry == 22 ? 0.06f : dry == 0 && !jungle ? 0.07f : 0.11f;
                    blockTop(w, x, z, h);
                    s.colorf(0.5f, 0.47f, 0.43f);
                    s.box(fx + 0.5f, -0.2f, z + 0.5f, 0.94f, 0.7f, 0.94f);
                    for (int k = 0; k < h; k++) {
                        // masso sepolto: coperto sopra e su tutti e quattro i lati, non si vede mai (nelle torri alte sono la maggior parte)
                        if (k + 1 < h && x > 0 && x + 1 < World3D.W && vis(w, x - 1, z) > k && vis(w, x + 1, z) > k
                                && z > 0 && vis(w, x, z - 1) > k && vis(w, x, z + 1) > k) continue;
                        s.colorf(0.96f + 0.08f * hash(x, z, k), 0.96f + 0.06f * hash(x, z, k + 7), 0.96f);
                        int faces = 63 & ~8;                              // la faccia di sotto poggia sempre su qualcosa
                        if (x + 1 < World3D.W && vis(w, x + 1, z) > k) faces &= ~1;
                        if (x > 0 && vis(w, x - 1, z) > k) faces &= ~2;
                        if (k + 1 < h) faces &= ~4;
                        if (vis(w, x, z + 1) > k) faces &= ~16;
                        if (z > 0 && vis(w, x, z - 1) > k) faces &= ~32;
                        int mk = s.mark();
                        if (round) s.roundBox(fx + 0.5f, k + 0.5f, z + 0.5f, 0.98f, 0.98f, 0.98f, dry == 22 ? rr : rr + 0.1f, faces, 2);
                        if (dry != 22) s.rough(mk, heaven ? 0.08f : 0.17f, 1.3f, 5);              // pietre sbozzate, non cubi (la fabbrica resta squadrata)
                        else s.box(fx + 0.5f, k + 0.5f, z + 0.5f, 1, 1, 1);
                        if (round) {       // nucleo scuro e giunti pieni: tra un masso arrotondato e l'altro non si vede attraverso
                            // giunti tra le pietre: pietra in ombra, non nero (con le pietre irregolari se ne vedono molti)
                            if (heaven) s.colorf(0.78f, 0.78f, 0.8f); else s.colorf(0.58f, 0.55f, 0.5f);
                            s.box(fx + 0.5f, k + 0.5f, z + 0.5f, 0.8f, 0.8f, 0.8f);
                            if (x + 1 < World3D.W && vis(w, x + 1, z) > k) s.box(fx + 1f, k + 0.5f, z + 0.5f, 0.42f, 0.8f, 0.8f);
                            if (vis(w, x, z + 1) > k) s.box(fx + 0.5f, k + 0.5f, z + 1f, 0.8f, 0.8f, 0.42f);
                            if (k + 1 < h) {
                                s.box(fx + 0.5f, k + 1f, z + 0.5f, 0.8f, 0.42f, 0.8f);
                                s.quad(fx + 0.1f, k + 1f, z + 0.1f, fx + 0.9f, k + 1f, z + 0.1f, fx + 0.9f, k + 1f, z + 0.9f, fx + 0.1f, k + 1f, z + 0.9f, 0, 1, 0, 0, 0, 1, 1);
                            }
                        }
                    }
                }
                MeshBuilder d = mb(K_DIRT, z);
                if (h == 0 && lip > 0) continue;                       // il prato ha già la sua parete frastagliata
                if (vis(w, x, z - 1) == World3D.PIT) wallZ(d, fx, z, 1);
                if (vis(w, x, z + 1) == World3D.PIT) wallZ(d, fx, z + 1, -1);
                if (x > 0 && vis(w, x - 1, z) == World3D.PIT) wallX(d, fx, z, 1);
                if (x < World3D.W - 1 && vis(w, x + 1, z) == World3D.PIT) wallX(d, fx + 1, z, -1);
            }
            MeshBuilder d = mb(K_DIRT, z);
            if (heaven) wallX(d, -World3D.HALF, z, 1);                 // altrove la parete segue il bordo frastagliato
            if (heaven) wallX(d, World3D.HALF, z, -1);
        }

        b = fire ? new MeshBuilder().color(255, 96, 22) : new MeshBuilder().color(10, 18, 14);   // fondo dei burroni (lava nell'incendio)
        float fw = World3D.HALF + 1.4f;                                 // più largo del corridoio: il vuoto frastagliato entra nei lati
        b.quad(-fw, -PIT_DEPTH, z0, fw, -PIT_DEPTH, z0, fw, -PIT_DEPTH, z1, -fw, -PIT_DEPTH, z1,
               0, 1, 0, 0.5f, 0.5f, 0.5f, 0.5f);
        pitFloor = b.build(-1, false, true);
        if (heaven) {                                              // sotto le nuvole: un mare di nuvole lontano
            float zl = (z1 - z0) / 6f;
            b = new MeshBuilder();
            b.quad(-World3D.HALF, -PIT_DEPTH, z0, World3D.HALF, -PIT_DEPTH, z0, World3D.HALF, -PIT_DEPTH, z1, -World3D.HALF, -PIT_DEPTH, z1, 0, 1, 0, 0, 0, 1.2f, zl);
            pitFloor = b.build(Img.R3H_SEA, false, true);
            b = new MeshBuilder();                                 // il mare di nuvole fino all'orizzonte
            b.quad(-420, -PIT_DEPTH + 0.4f, z0 - 30, 420, -PIT_DEPTH + 0.4f, z0 - 30, 420, -PIT_DEPTH + 0.4f, z1 + 330, -420, -PIT_DEPTH + 0.4f, z1 + 330,
                   0, 1, 0, 0, 0, 90f, 150f);
            seaPlane = b.build(Img.R3H_SEA, false, true);
        }
        if (spring || jungle) {                                    // nei burroni scorre un fiume: due piani d'acqua, quello sopra trasparente e mosso
            float wy = -0.75f, zl = (z1 - z0) / 3f;
            b = new MeshBuilder();
            float ww = World3D.HALF + 1.4f;
            b.quad(-ww, wy, z0, ww, wy, z0, ww, wy, z1, -ww, wy, z1, 0, 1, 0, 0, 0, 2.2f, zl);
            pitFloor = b.build(jungle ? Img.R3J_WATER : Img.R3D_WATER, false, true);
            b = new MeshBuilder();
            b.quad(-World3D.HALF, 0, z0, World3D.HALF, 0, z0, World3D.HALF, 0, z1, -World3D.HALF, 0, z1, 0, 1, 0, 0.3f, 0.2f, 2.0f, zl * 1.3f);
            pitWater = b.build(jungle ? Img.R3J_WATER : Img.R3D_WATER, true, true);
        }

        // ---- lati: un'unica superficie continua dal bordo del sentiero al bosco. Il prato sfuma nel sottobosco:
        //      le due texture si incontrano con lo stesso colore medio, così non si vede lo stacco.
        float[] D = {0, 0.35f, 0.75f, 1.2f, 1.7f, 2.3f, SEAM, 4.6f, 6f, 8f, 11f, 15f, 20f, 27f, 36f};
        float[] seamG = autumn || aurora || fire || heaven ? new float[] {1f, 1f, 1f} : new float[] {1.214f, 1.064f, 1.073f};
        float[] seamF = autumn || aurora || fire || heaven ? new float[] {1f, 1f, 1f} : new float[] {0.850f, 0.943f, 0.936f};
        for (int z = z0; z < (heaven ? z0 : z1); z++) {                    // in cielo non c'è terreno ai lati: si vede solo il cielo
            for (int side = -1; side <= 1; side += 2) {
                boolean pitNear = false;
                for (int dz = -2; dz <= 2 && !heaven; dz++)
                    for (int xi = 0; xi < 2; xi++) if (vis(w, side < 0 ? xi : World3D.W - 1 - xi, z + dz) == World3D.PIT) pitNear = true;
                for (int k = 0; k + 1 < D.length; k++) {
                    boolean grassy = D[k + 1] <= SEAM + 0.01f;
                    MeshBuilder g = mb(grassy ? K_BANK : K_FOREST, z);
                    if (pitNear && D[k] < 2.4f && grassy) {                 // il burrone entra un po' nel prato: niente parete dritta
                        final float sd = side;
                        clipGrid(g, side * (World3D.HALF + D[k]), side * (World3D.HALF + D[k + 1]), z, z + 1,
                                Math.max(2, Math.round((D[k + 1] - D[k]) * 8)), 8, z, new Shade() {
                            public void vert(MeshBuilder g, float x, float zz) {
                                float d = Math.abs(x) - World3D.HALF, t = d / SEAM, bs = (0.98f - 0.13f * t) * (1f - 0.7f * lipDrop(x, zz) / lip * 0.45f);
                                ground(g, x, zz, x * 0.5f, zz * 0.5f, bs * (1 + (seamG[0] - 1) * t), bs * (1 + (seamG[1] - 1) * t), bs * (1 + (seamG[2] - 1) * t));
                            }
                        });
                        continue;
                    }
                    float[][] c = {{D[k], z}, {D[k + 1], z}, {D[k + 1], z + 1}, {D[k], z + 1}};
                    for (int q : order) {
                        float d = c[q][0], x = side * (World3D.HALF + d), zz = c[q][1];
                        if (grassy) {
                            float t = d / SEAM, bs = 0.98f - 0.13f * t;
                            ground(g, x, zz, x * 0.5f, zz * 0.5f, bs * (1 + (seamG[0] - 1) * t), bs * (1 + (seamG[1] - 1) * t), bs * (1 + (seamG[2] - 1) * t));
                        } else {
                            float t = Math.min(1f, (d - SEAM) / 4f), bs = 0.85f - 0.15f * t;
                            ground(g, x, zz, x * 0.34f, zz * 0.34f, bs * (seamF[0] + (1 - seamF[0]) * t), bs * (seamF[1] + (1 - seamF[1]) * t), bs * (seamF[2] + (1 - seamF[2]) * t));
                        }
                    }
                }
            }
        }

        buildTrees();
        if (heaven) buildPuffs(w, z0, z1);
        else if (dry != 0) buildDry(w, z0, z1);
        else {
            buildUndergrowth(w, z0, z1);
            decor.build(w, z0, z1);
        }
        if (dry == 0) buildRays(z0, z1);
        buildObjects();

        // ---- da builder a mesh, con materiale
        land = new Mesh[K_N][nChunks];
        for (int k = 0; k < K_N; k++) {
            for (int c = 0; c < nChunks; c++) {
                if (mbs[k][c] == null) continue;
                Mesh mesh = mbs[k][c].build(k == K_TRAIL ? Img.R3_TRAIL : k < ktex.length ? ktex[k] : -1, k == K_RAY || k == K_TRAIL, true);
                mbs[k][c] = null;                       // il builder non serve più: la memoria si libera subito, non a fine costruzione
                mesh.releasable = true;
                if (k == K_CROWN || k == K_TUFT || k == K_FERN || k == K_FLOWER || k == K_FIR) mesh.cutout = true;
                if (k == K_GRASS || k == K_DIRT || k == K_BANK || k == K_TUFT || k == K_FLOWER || k == K_RAY || k == K_TRAIL || k == K_GLOW) mesh.castShadow = false;
                switch (k) {
                    case K_GRASS: case K_BANK: mesh.spec = 0.05f; mesh.shine = 10f; break;
                    case K_STONE: case K_ROCK: mesh.spec = 0.07f; mesh.shine = 14f; break;
                    case K_PROP: mesh.spec = 0.12f; mesh.shine = 22f; break;
                    case K_GLOW: mesh.spec = 0.9f; mesh.shine = 50f; break;
                    // il fogliame si vede da entrambi i lati: niente riflesso, che sul rovescio lo farebbe apparire grigio
                    default: break;
                }
                land[k][c] = mesh;
            }
        }
        mbs = null;
    }

    /** Tratto di sentiero battuto sopra una casella di prato: quattro strisce per lato, appoggiate al terreno. */
    private void trailCell(World3D w, int x, int z) {
        if (heaven || dry != 0) return;
        float fx = x - World3D.HALF;
        MeshBuilder t = mb(K_TRAIL, z);
        float[] tint = spring ? new float[] {1.0f, 0.97f, 0.9f} : fire ? new float[] {0.75f, 0.6f, 0.5f} : aurora ? new float[] {1.25f, 1.7f, 2.75f} : autumn ? new float[] {0.92f, 0.82f, 0.74f} : new float[] {1f, 0.96f, 0.9f};
        int[] order = {0, 1, 2, 0, 2, 3};
        for (int sz = 0; sz < 2; sz++) {
            float za = z + sz * 0.5f, zb = za + 0.5f;
            float ca = trailX(za), cb = trailX(zb);
            for (int k = 0; k < 4; k++) {
                float u0 = k / 4f, u1 = (k + 1) / 4f;
                float xa0 = ca + (u0 * 2 - 1) * TRAIL_HW, xa1 = ca + (u1 * 2 - 1) * TRAIL_HW, xb0 = cb + (u0 * 2 - 1) * TRAIL_HW, xb1 = cb + (u1 * 2 - 1) * TRAIL_HW;
                float lo = Math.max(fx, Math.min(xa0, xb0)), hi = Math.min(fx + 1, Math.max(xa1, xb1));
                if (hi <= lo) continue;
                // ritaglio sulla casella: i vertici restano dentro [fx, fx+1]
                float[][] c = {{Math.max(fx, Math.min(fx + 1, xa0)), za, 0}, {Math.max(fx, Math.min(fx + 1, xa1)), za, 0},
                        {Math.max(fx, Math.min(fx + 1, xb1)), zb, 0}, {Math.max(fx, Math.min(fx + 1, xb0)), zb, 0}};
                c[0][2] = (c[0][0] - ca) / (2 * TRAIL_HW) + 0.5f; c[1][2] = (c[1][0] - ca) / (2 * TRAIL_HW) + 0.5f;
                c[2][2] = (c[2][0] - cb) / (2 * TRAIL_HW) + 0.5f; c[3][2] = (c[3][0] - cb) / (2 * TRAIL_HW) + 0.5f;
                boolean overVoid = false;
                for (float[] cc : c) if (edgeField(cc[0], cc[1]) < 0.08f) overVoid = true;
                if (overVoid) continue;                                 // il sentiero si ferma prima del bordo frastagliato
                for (int q : order) {
                    float px = c[q][0], pz = c[q][1];
                    lightAt(px, pz, 1f);
                    float ao = aoAt(w, px, pz);
                    t.colorf(lt[0] * tint[0] * ao, lt[1] * tint[1] * ao, lt[2] * tint[2] * ao);
                    t.vert(px, groundAt(px, pz) + 0.03f, pz, 0, 1, 0, c[q][2], pz * 0.5f);
                }
            }
        }
    }

    /** Nel cielo il sentiero è una lastra di nuvola sospesa: ciuffi sotto ai bordi, un orlo soffice sopra e, molto in basso, qualche nuvola lontana per dare l'altezza. */
    private void buildPuffs(World3D w, int z0, int z1) {
        Random r = new Random(11);
        for (float z = z0; z < z1; z += 1.5f) {                       // pancia della lastra: bolle sotto ai due bordi e sotto ai ponti
            for (int side = -1; side <= 1; side += 2) {
                float rad = 0.9f + 0.7f * r.nextFloat();
                float px = side * (World3D.HALF - 0.3f + 0.5f * r.nextFloat()), pz = z + r.nextFloat();
                MeshBuilder p = mb(K_PROP, (int) pz);
                lightAt(px, pz, 1f);
                float sh = 0.9f + 0.1f * r.nextFloat();
                p.colorf(lt[0] * sh, lt[1] * sh, lt[2] * sh * 1.04f);
                p.ellipsoid(px, -pitD + 0.1f, pz, rad * 1.3f, rad * 0.8f, rad * 1.1f, 10, 7);
            }
        }
        for (int side = -1; side <= 1; side += 2) {
            for (float z = z0; z < z1; z += 0.9f) {                    // orlo di bolle lungo il bordo del sentiero, dove non c'è il vuoto
                int zi = (int) Math.floor(z);
                if (vis(w, side < 0 ? 0 : World3D.W - 1, zi) == World3D.PIT) continue;
                float rad = 0.4f + 0.25f * r.nextFloat();
                float px = side * (World3D.HALF + 0.02f), pz = z + 0.4f * r.nextFloat();
                float py = groundAt(px, pz) - 0.12f + rad * 0.2f;
                MeshBuilder p = mb(K_PROP, zi);
                lightAt(px, pz, 1f);
                p.colorf(lt[0] * 1.12f, lt[1] * 1.12f, lt[2] * 1.12f);
                p.ellipsoid(px, py, pz, rad * 1.2f, rad * 0.8f, rad, 8, 6);
            }
            for (float z = z0; z < z1; z += 7f + 6f * r.nextFloat()) {  // nuvole lontane, tanti metri più in basso
                float rad = 5f + 8f * r.nextFloat();
                float px = side * (World3D.HALF + 14f + 70f * r.nextFloat() * r.nextFloat()), pz = z + 4f * r.nextFloat();
                MeshBuilder p = mb(K_PROP, (int) pz);
                p.colorf(0.96f, 0.98f, 1f);
                p.ellipsoid(px, -22f - 40f * r.nextFloat(), pz, rad * 1.5f, rad * 0.55f, rad, 12, 8);
            }
        }
    }

    /** In cima ai massi del percorso: neve nel livello dell'aurora, altrimenti ciuffi d'erba corta e muschio. */
    private void blockTop(World3D w, int x, int z, int h) {
        float fx = x - World3D.HALF;
        Random r = new Random(x * 131 + z * 17);
        if (aurora || heaven) {
            MeshBuilder p = mb(K_PROP, z);
            lightAt(fx + 0.5f, z + 0.5f, 1f);
            if (heaven) p.colorf(lt[0] * 1.15f, lt[1] * 1.15f, lt[2] * 1.15f); else
            p.colorf(lt[0] * 2.3f, lt[1] * 2.35f, lt[2] * 2.4f);
            p.roundBox(fx + 0.5f, h + 0.02f, z + 0.5f, 1.03f, 0.16f, 1.03f, 0.075f);
            return;
        }
        for (int k = 0; k < 3; k++) {
            float tx = fx + 0.12f + 0.76f * r.nextFloat(), tz = z + 0.12f + 0.76f * r.nextFloat();
            tuft(K_TUFT, tx, h - 0.02f, tz, 0.13f + 0.1f * r.nextFloat(), 3, 0.3f, r);
        }
        // ciuffi al piede delle facce libere: il masso sembra affondare nell'erba
        int[][] nb = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] d : nb) {
            int ax = x + d[0], az = z + d[1];
            if (ax < 0 || ax >= World3D.W || vis(w, ax, az) != 0) continue;
            for (int k = 0; k < 2; k++) {
                float o = 0.15f + 0.7f * r.nextFloat();
                float tx = fx + 0.5f + d[0] * 0.56f + (d[0] == 0 ? o - 0.5f : 0), tz = z + 0.5f + d[1] * 0.56f + (d[1] == 0 ? o - 0.5f : 0);
                tuft(K_TUFT, tx, groundAt(tx, tz), tz, 0.2f + 0.16f * r.nextFloat(), 3, 0.3f, r);
            }
        }
    }

    // ---- alberi

    private static void bq(MeshBuilder b, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz,
                           float dx, float dy, float dz, float nx, float ny, float nz, float u0, float u1,
                           float sa, float sb, float sw1, float sw2) {
        // a-b alla base (u0), c-d in punta (u1); sa/sb = ombreggiatura alla base e in punta, sw = peso del vento
        b.sway(sw1);
        b.vert(ax, ay, az, nx, ny, nz, u0, 0);
        b.vert(bx, by, bz, nx, ny, nz, u0, 1);
        b.sway(sw2);
        b.vert(cx, cy, cz, nx, ny, nz, u1, 1);
        b.sway(sw1);
        b.vert(ax, ay, az, nx, ny, nz, u0, 0);
        b.sway(sw2);
        b.vert(cx, cy, cz, nx, ny, nz, u1, 1);
        b.vert(dx, dy, dz, nx, ny, nz, u1, 0);
        b.sway(0);
    }

    /** Un ramo: una o due falde di cartoncino (la seconda piega più verso il basso), con la texture di un ramo di abete. */
    private void bough(MeshBuilder cb, float x, float y, float z, float a, float R, float d1, float d2, float ws, float tone,
                       float swk, boolean two, Random r) {
        float ca = (float) Math.cos(a), sa = (float) Math.sin(a);
        float roll = (r.nextFloat() - 0.5f) * 1.0f;
        float lx = -sa, ly = roll * 0.55f, lz = ca, ll = (float) Math.sqrt(lx * lx + ly * ly + lz * lz);
        lx /= ll; ly /= ll; lz /= ll;
        float p0x = x + ca * 0.05f, p0y = y, p0z = z + sa * 0.05f;
        float L1 = two ? 0.55f * R : R, n1 = (float) Math.sqrt(1 + d1 * d1);
        float p1x = p0x + ca / n1 * L1, p1y = p0y - d1 / n1 * L1, p1z = p0z + sa / n1 * L1;
        float nx = ca * 0.4f + (r.nextFloat() - 0.5f) * 0.3f, ny = 0.9f, nz = sa * 0.4f + (r.nextFloat() - 0.5f) * 0.3f;
        float nn = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        nx /= nn; ny /= nn; nz /= nn;
        float t = tone * (0.95f + 0.1f * r.nextFloat());
        // rami vicini: oltre alla falda orizzontale, una seconda falda di traverso che pende sotto il ramo (volume da ogni lato)
        int sheets = two ? 2 : 1;
        for (int sh = 0; sh < sheets; sh++) {
            float ws2 = ws;
            float qx = lx, qy = ly, qz = lz, oy = 0f;
            if (sh == 1) {
                qx = lx * 0.35f; qy = -0.94f; qz = lz * 0.35f;
                float ql = (float) Math.sqrt(qx * qx + qy * qy + qz * qz);
                qx /= ql; qy /= ql; qz /= ql;
                ws2 = ws * 0.62f;
                oy = -ws2 * 0.55f;                                   // appesa sotto il ramo, non a metà
            }
            emitBough(cb, p0x, p0y + oy * 0.4f, p0z, p1x, p1y + oy, p1z, ca, sa, R, d2, qx, qy, qz, ws2, nx, ny, nz, t, swk, two);
        }
        cb.sway(0);
    }

    private void emitBough(MeshBuilder cb, float p0x, float p0y, float p0z, float p1x, float p1y, float p1z, float ca, float sa, float R, float d2,
                           float lx, float ly, float lz, float ws, float nx, float ny, float nz, float t, float swk, boolean two) {
        float u1 = two ? 0.5f : 1f, w0 = ws * 0.45f, w1 = ws;
        cb.colorf(t * 0.55f, t * 0.58f, t * 0.5f);
        float[] dark = {t * 0.55f, t * 0.58f, t * 0.5f}, lit = {t, t * 1.02f, t * 0.92f};
        // base scura, punta chiara: il colore cambia da un lato all'altro del quad, quindi emetto i vertici uno a uno
        float[][] q = {
            {p0x - lx * w0, p0y - ly * w0, p0z - lz * w0, 0, 0}, {p0x + lx * w0, p0y + ly * w0, p0z + lz * w0, 0, 1},
            {p1x + lx * w1, p1y + ly * w1, p1z + lz * w1, u1, 1}, {p1x - lx * w1, p1y - ly * w1, p1z - lz * w1, u1, 0}};
        float[] swv = {0, 0, swk * (two ? 0.45f : 1f), swk * (two ? 0.45f : 1f)};
        float[][] col = {dark, dark, lit, lit};
        int[] ord = {0, 1, 2, 0, 2, 3};
        for (int k : ord) {
            cb.colorf(col[k][0], col[k][1], col[k][2]);
            cb.sway(swv[k]);
            cb.vert(q[k][0], q[k][1], q[k][2], nx, ny, nz, q[k][3], q[k][4]);
        }
        if (two) {
            float L2 = 0.5f * R, n2 = (float) Math.sqrt(1 + d2 * d2);
            float p2x = p1x + ca / n2 * L2, p2y = p1y - d2 / n2 * L2, p2z = p1z + sa / n2 * L2, w2 = ws * 0.85f;
            float[][] q2 = {
                {p1x - lx * w1, p1y - ly * w1, p1z - lz * w1, 0.5f, 0}, {p1x + lx * w1, p1y + ly * w1, p1z + lz * w1, 0.5f, 1},
                {p2x + lx * w2, p2y + ly * w2, p2z + lz * w2, 1, 1}, {p2x - lx * w2, p2y - ly * w2, p2z - lz * w2, 1, 0}};
            float[] sw2 = {swk * 0.45f, swk * 0.45f, swk, swk};
            float[][] c2 = {lit, lit, lit, lit};
            for (int k : ord) {
                cb.colorf(c2[k][0], c2[k][1], c2[k][2]);
                cb.sway(sw2[k]);
                cb.vert(q2[k][0], q2[k][1], q2[k][2], nx, ny * 0.9f, nz, q2[k][3], q2[k][4]);
            }
        }
        cb.sway(0);
    }

    /** Abete con rami veri: tronco in vista in basso, poi livelli di rami che pendono. detailed = ogni ramo piegato in due falde. */
    private void fir(float x, float y0, float z, float h, float rad, float tint, boolean detailed, Random r) {
        MeshBuilder tb = mb(K_TRUNK, z), cb = mb(K_CROWN, z);
        float k = 0.8f + 0.2f * tint;
        tb.colorf(k, k * 0.96f, k * 0.92f);
        float base = y0 - 0.3f, topY = y0 + h * 0.97f, r0 = 0.09f + 0.012f * h;
        int pieces = Math.max(1, (int) Math.ceil((topY - base) / 2.4f));
        for (int p = 0; p < pieces; p++) {
            float ya = base + (topY - base) * p / pieces, yb = base + (topY - base) * (p + 1) / pieces;
            tb.cylinder(x, ya, yb, z, r0 * (1f - 0.92f * p / pieces), r0 * (1f - 0.92f * (p + 1) / pieces), 7, false, false);
        }
        int whorls = detailed ? 18 : 9;
        float swBase = Math.min(1.6f, 0.5f + h * 0.14f);
        for (int i = 0; i < whorls; i++) {
            float f = i / (whorls - 1f);
            float yb = y0 + h * (0.2f + 0.8f * f);
            int per = Math.max(3, Math.round((detailed ? 9.5f : 5.4f) - (detailed ? 4.2f : 3.2f) * f));
            float R = Math.max(0.35f, rad * (1.12f - 0.94f * f) + 0.15f);
            float rot0 = r.nextFloat() * 6.283f;
            boolean top = i == whorls - 1;
            float tone = tint * (0.86f + 0.2f * f);
            for (int q = 0; q < per; q++) {
                float a = rot0 + q * 6.283f / per + (r.nextFloat() - 0.5f) * 0.7f;
                if (top) bough(cb, x, yb, z, a, R * 0.9f + 0.2f, -1.7f, -1.7f, R * 0.3f + 0.08f, tone, swBase * 0.8f, false, r);
                else bough(cb, x, yb + (r.nextFloat() - 0.5f) * 0.18f, z, a, R * (0.85f + 0.3f * r.nextFloat()),
                        0.10f + 0.12f * (1f - f), 0.55f + 0.3f * (1f - f), R * (detailed ? 0.25f : 0.3f), tone, swBase * (0.55f + 0.7f * f), detailed, r);
            }
        }
    }

    private boolean lcN;
    private float lcNx, lcNy, lcNz;

    /** Un cartoncino di fogliame, centrato in (x,y,z), ruotato e inclinato; più scuro in basso. */
    private void leafCard(MeshBuilder cb, float x, float y, float z, float size, float yaw, float tilt, float[] tc, float tone, float swayK) {
        float ca = (float) Math.cos(yaw), sa = (float) Math.sin(yaw), ct = (float) Math.cos(tilt), st = (float) Math.sin(tilt);
        float h = size / 2;
        // asse orizzontale del cartoncino (ca,0,sa); asse "su" inclinato di tilt verso la normale
        float ux = -sa * st, uy = ct, uz = ca * st;
        float[][] q = {{-h, -h}, {h, -h}, {h, h}, {-h, h}};
        float[][] uv = {{0, 1}, {1, 1}, {1, 0}, {0, 0}};
        int[] ord = {0, 1, 2, 0, 2, 3};
        float nx = sa * ct, ny = st, nz = -ca * ct;
        if (lcN) { nx = lcNx; ny = lcNy; nz = lcNz; }                  // normale della chioma: la fronda prende la luce come il lobo a cui appartiene
        for (int k : ord) {
            float a = q[k][0], b = q[k][1];
            float low = 0.84f + 0.16f * (b + h) / size;
            cb.colorf(tc[0] * tone * low, tc[1] * tone * low, tc[2] * tone * low);
            cb.sway(swayK * (0.5f + 0.5f * (b + h) / size));
            cb.vert(x + ca * a + ux * b, y + uy * b, z + sa * a + uz * b, nx, ny + 0.3f, nz, uv[k][0], uv[k][1]);
        }
        cb.sway(0);
    }

    /**
     * Lobo di fogliame: un ellissoide pieno, irregolare, con normali morbide e un mosaico di foglie come texture.
     * Più scuro in basso (ombra interna), più chiaro in cima; il vento lo fa oscillare in alto.
     */
    void foliageLobe(MeshBuilder bb, float cx, float cy, float cz, float rx, float ry, float rz, int segs, int rings,
                             float[] tc, float tone, float swayK, Random r) {
        float[] ph = {r.nextFloat() * 6.28f, r.nextFloat() * 6.28f, r.nextFloat() * 6.28f, r.nextFloat() * 6.28f};
        int n = (rings + 1) * (segs + 1);
        float[][] p = new float[n][8];            // x y z nx ny nz u v
        for (int i = 0; i <= rings; i++) {
            double a = Math.PI * i / rings;
            for (int j = 0; j <= segs; j++) {
                double b = 2 * Math.PI * j / segs;
                float ux = (float) (Math.sin(a) * Math.cos(b)), uy = (float) Math.cos(a), uz = (float) (Math.sin(a) * Math.sin(b));
                float d = 1f + 0.17f * (float) Math.sin(ux * 3.3 + uz * 1.7 + ph[0]) + 0.12f * (float) Math.sin(uy * 4.1 + ux * 2.3 + ph[1])
                        + 0.08f * (float) Math.sin(uz * 6.1 + uy * 3.7 + ph[2]);
                float[] q = p[i * (segs + 1) + j];
                q[0] = cx + ux * rx * d; q[1] = cy + uy * ry * d * (uy < 0 ? 0.8f : 1f); q[2] = cz + uz * rz * d;
                float nxx = ux / rx, nyy = uy / ry, nzz = uz / rz, l = (float) Math.sqrt(nxx * nxx + nyy * nyy + nzz * nzz);
                q[3] = nxx / l; q[4] = nyy / l * 0.8f + 0.25f; q[5] = nzz / l;
                q[6] = j / (float) segs * (6.2832f * Math.max(rx, rz) * (float) Math.sin(a)) / 2.6f;
                q[7] = i / (float) rings * (3.1416f * ry) / 2.6f;
            }
        }
        for (int i = 0; i < rings; i++) {
            for (int j = 0; j < segs; j++) {
                float[][] qs = {p[i * (segs + 1) + j], p[(i + 1) * (segs + 1) + j], p[(i + 1) * (segs + 1) + j + 1], p[i * (segs + 1) + j + 1]};
                int[] ord = {0, 1, 2, 0, 2, 3};
                // proiezione piana secondo l'asse dominante della faccia: niente schiacciamento ai poli
                float anx = 0, any = 0, anz = 0;
                for (float[] q : qs) { anx += q[3]; any += q[4] - 0.25f; anz += q[5]; }
                int axis = Math.abs(any) >= Math.abs(anx) && Math.abs(any) >= Math.abs(anz) ? 1 : (Math.abs(anx) > Math.abs(anz) ? 0 : 2);
                for (int k : ord) {
                    float[] q = qs[k];
                    float up = (q[1] - (cy - ry)) / (2 * ry);
                    float shade = (0.6f + 0.55f * Math.max(0f, Math.min(1f, up))) * tone;
                    bb.colorf(tc[0] * shade, tc[1] * shade, tc[2] * shade);
                    bb.sway(swayK * (0.4f + 0.6f * Math.max(0f, Math.min(1f, up))));
                    float u = (axis == 0 ? q[2] : q[0]) / 2.6f + ph[3], v = (axis == 1 ? q[2] : q[1]) / 2.6f;
                    bb.vert(q[0], q[1], q[2], q[3], q[4], q[5], u, v);
                }
            }
        }
        bb.sway(0);
    }

    /** Albero a foglie larghe d'autunno: tronco con rami, chioma fatta di lobi pieni e foglie sciolte sul contorno. */
    private void broadleaf(float x, float y0, float z, float h, float rad, float tint, int lobes, Random r) {
        MeshBuilder tb = mb(K_TRUNK, z), cb = mb(K_CROWN, z), bb = mb(K_BLOB, z);
        float[] tc = tints()[r.nextInt(tints().length)];
        float k = 0.8f + 0.2f * tint;
        tb.colorf(k, k * 0.92f, k * 0.84f);
        float crownY = y0 + h * 0.62f, r0 = 0.1f + 0.016f * h, top = crownY + h * 0.05f;
        tb.cylinder(x, y0 - 0.3f, y0 + 0.7f, z, r0 * 1.5f, r0 * 1.1f, 8, false, false);
        tb.cylinder(x, y0 + 0.7f, top, z, r0 * 1.1f, r0 * 0.45f, 7, false, false);
        float rh = rad * 1.3f + 0.3f, rv = h * 0.26f;
        float wind = Math.min(1.4f, 0.4f + h * 0.1f);
        int segs = lobes >= 4 ? 11 : 8, rings = lobes >= 4 ? 7 : 5;      // abbastanza per la sagoma, le fronde nascondono gli spigoli
        float[][] centers = new float[lobes][3];
        for (int i = 0; i < lobes; i++) {
            float a = r.nextFloat() * 6.283f + i * 2.4f;
            float rr = i == 0 ? 0f : rh * (0.30f + 0.32f * r.nextFloat());
            float cx = x + (float) Math.cos(a) * rr, cz = z + (float) Math.sin(a) * rr;
            float cy = crownY + (i == 0 ? rv * 0.5f : rv * (r.nextFloat() * 1.0f - 0.35f));
            float lr = rh * (i == 0 ? 0.86f : 0.58f + 0.2f * r.nextFloat());
            centers[i][0] = cx; centers[i][1] = cy; centers[i][2] = cz;
            if (i > 0) FoeModels.tube(tb, x, crownY - rv * 0.35f, z, cx, cy - lr * 0.2f, cz, r0 * 0.5f, r0 * 0.22f, 5, FoeModels.flat(new float[] {k, k * 0.92f, k * 0.84f}));
            float[] ct = {tc[0] * (0.9f + 0.2f * r.nextFloat()), tc[1] * (0.88f + 0.24f * r.nextFloat()), tc[2] * (0.88f + 0.24f * r.nextFloat())};
            float tone = (0.92f + 0.2f * r.nextFloat()) * (0.9f + 0.15f * tint);
            foliageLobe(bb, cx, cy, cz, lr, lr * 1.0f, lr, segs, rings, ct, tone, wind, r);
            // foglie sciolte sul contorno del lobo
            // fronde sul contorno: grappoli di foglie che rompono il profilo del lobo e ne nascondono le facce
            int fringe = lobes >= 4 ? 44 : 16;
            for (int f = 0; f < fringe; f++) {
                float fa = r.nextFloat() * 6.283f, fu = r.nextFloat() * 1.75f - 0.75f, fl = (float) Math.sqrt(1 - fu * fu);
                float out = 0.9f + 0.2f * r.nextFloat();
                float fx = cx + (float) Math.cos(fa) * fl * lr * out, fy = cy + fu * lr * 0.9f * out, fz = cz + (float) Math.sin(fa) * fl * lr * out;
                float ts = (0.8f + 0.42f * (fu * 0.5f + 0.5f)) * (0.86f + 0.26f * r.nextFloat());
                float[] lt2 = {ct[0] * (0.92f + 0.16f * r.nextFloat()), ct[1] * (0.88f + 0.22f * r.nextFloat()), ct[2] * (0.85f + 0.25f * r.nextFloat())};
                float ox = fx - cx, oy = fy - cy + 0.25f * lr, oz = fz - cz, ol = (float) Math.sqrt(ox * ox + oy * oy + oz * oz) + 1e-4f;
                lcN = true; lcNx = ox / ol; lcNy = oy / ol - 0.3f; lcNz = oz / ol;
                leafCard(cb, fx, fy, fz, lr * (0.4f + 0.3f * r.nextFloat()), r.nextFloat() * 3.14f, (r.nextFloat() - 0.5f) * 1.4f, lt2, ts, wind);
                lcN = false;
            }
        }
    }

    /** Albero lontano: tre cartoncini incrociati con l'immagine di un abete intero. */
    private void impostor(float x, float y0, float z, float h, float tint, Random r) {
        MeshBuilder cb = mb(K_FIR, z);
        float top = y0 + h * 1.04f, bot = y0 - 0.15f, hw = h * 1.04f * 0.5f * (autumn ? 0.8f : 0.92f), rot = r.nextFloat() * 3.14f;
        float t = tint * 0.95f, sw = Math.min(1.6f, 0.4f + h * 0.11f) * 0.9f;
        float[] tc = autumn ? tints()[Math.abs((int) (x * 7.3f + z * 3.1f)) % tints().length] : new float[] {1, 1, 1};
        float cr = tc[0], cg = tc[1], cbl = tc[2];
        for (int k = 0; k < 3; k++) {
            float a = rot + k * 1.0472f, cx = (float) Math.cos(a) * hw, cz = (float) Math.sin(a) * hw;
            float nx = (float) Math.sin(a) * 0.5f, nz = -(float) Math.cos(a) * 0.5f;
            cb.colorf(t * 0.75f * cr, t * 0.78f * cg, t * 0.7f * cbl);
            cb.sway(0);
            cb.vert(x - cx, bot, z - cz, nx, 0.85f, nz, 0, 1);
            cb.vert(x + cx, bot, z + cz, nx, 0.85f, nz, 1, 1);
            cb.colorf(t * cr, t * 1.02f * cg, t * 0.95f * cbl);
            cb.sway(sw);
            cb.vert(x + cx, top, z + cz, nx, 0.85f, nz, 1, 0);
            cb.colorf(t * 0.75f * cr, t * 0.78f * cg, t * 0.7f * cbl);
            cb.sway(0);
            cb.vert(x - cx, bot, z - cz, nx, 0.85f, nz, 0, 1);
            cb.colorf(t * cr, t * 1.02f * cg, t * 0.95f * cbl);
            cb.sway(sw);
            cb.vert(x + cx, top, z + cz, nx, 0.85f, nz, 1, 0);
            cb.vert(x - cx, top, z - cz, nx, 0.85f, nz, 0, 0);
            cb.sway(0);
        }
    }

    private void buildTrees() {
        Random r = new Random(11);
        for (Tree t : trees) {
            if (t.row == 0) decor.roots(t.x, t.y, t.z, 0.1f + 0.014f * t.h, 3, r);
            if (autumn) {
                if (t.row == 0) broadleaf(t.x, t.y, t.z, t.h, t.rad, t.tint, 4, r);
                else if (t.row == 1) broadleaf(t.x, t.y, t.z, t.h, t.rad, t.tint, 3, r);
                else impostor(t.x, t.y, t.z, t.h, t.tint, r);
            } else if (t.row == 0) fir(t.x, t.y, t.z, t.h, t.rad, t.tint, true, r);
            else if (t.row == 1) fir(t.x, t.y, t.z, t.h, t.rad, t.tint, false, r);
            else impostor(t.x, t.y, t.z, t.h, t.tint, r);
        }
    }

    // ---- sottobosco

    /** Felce: rosetta di fronde che si alzano dal centro e ricadono verso l'esterno. */
    private void fern(float x, float y, float z, float size, Random r) {
        MeshBuilder b = mb(K_FERN, z);
        lightAt(x, z, 1f);
        float lr = lt[0], lg = lt[1], lb = lt[2];
        if (autumn && !spring && !jungle) { lr *= 1.25f; lg *= 0.8f; lb *= 0.45f; }
        int nf = 9 + r.nextInt(4), segs = 3;
        float rot = r.nextFloat() * 6.283f;
        for (int k = 0; k < nf; k++) {
            float az = rot + k * 6.283f / nf + (r.nextFloat() - 0.5f) * 0.5f;
            float L = size * (0.8f + 0.4f * r.nextFloat()), lift = 0.45f + 0.3f * r.nextFloat();
            float ca = (float) Math.cos(az), sa = (float) Math.sin(az), hw = L * 0.105f;
            float tone = 0.9f + 0.3f * r.nextFloat();
            float[][] pl = new float[segs + 1][3], pr = new float[segs + 1][3];
            for (int i = 0; i <= segs; i++) {
                float t = i / (float) segs;
                float hr = L * 0.85f * t, yv = y + L * lift * (float) Math.sin(t * 2.5f);
                float w = hw * (i == segs ? 0.55f : 1f);
                pl[i][0] = x + ca * hr + sa * w; pl[i][1] = yv; pl[i][2] = z + sa * hr - ca * w;
                pr[i][0] = x + ca * hr - sa * w; pr[i][1] = yv; pr[i][2] = z + sa * hr + ca * w;
            }
            for (int i = 0; i < segs; i++) {
                float t0 = i / (float) segs, t1 = (i + 1) / (float) segs;
                float s0 = (0.5f + 0.5f * t0) * tone, s1 = (0.5f + 0.5f * t1) * tone;
                float nx = ca * 0.3f, nz = sa * 0.3f;
                b.sway(t0 * 0.7f);
                b.colorf(lr * s0, lg * s0, lb * s0);
                b.vert(pl[i][0], pl[i][1], pl[i][2], nx, 0.95f, nz, t0, 0);
                b.vert(pr[i][0], pr[i][1], pr[i][2], nx, 0.95f, nz, t0, 1);
                b.sway(t1 * 0.7f);
                b.colorf(lr * s1, lg * s1, lb * s1);
                b.vert(pr[i + 1][0], pr[i + 1][1], pr[i + 1][2], nx, 0.95f, nz, t1, 1);
                b.sway(t0 * 0.7f);
                b.colorf(lr * s0, lg * s0, lb * s0);
                b.vert(pl[i][0], pl[i][1], pl[i][2], nx, 0.95f, nz, t0, 0);
                b.sway(t1 * 0.7f);
                b.colorf(lr * s1, lg * s1, lb * s1);
                b.vert(pr[i + 1][0], pr[i + 1][1], pr[i + 1][2], nx, 0.95f, nz, t1, 1);
                b.vert(pl[i + 1][0], pl[i + 1][1], pl[i + 1][2], nx, 0.95f, nz, t1, 0);
            }
        }
        b.sway(0);
    }

    /** Ciuffo d'erba (o di fiori): cartoncini disposti a raggiera che si aprono verso l'esterno. */
    private void tuft(int kind, float x, float y, float z, float size, int cards, float lean, Random r) {
        if (lip > 0 && edgeField(x, z) < 0.05f) return;                // sopra il vuoto frastagliato non cresce niente
        MeshBuilder b = mb(kind, z);
        lightAt(x, z, 1f);
        float lr = lt[0], lg = lt[1], lb = lt[2];
        float rot = r.nextFloat() * 6.283f, hw = size * 0.5f;
        for (int k = 0; k < cards; k++) {
            float a = rot + k * 6.283f / cards, ca = (float) Math.cos(a), sa = (float) Math.sin(a);
            float bx = x + ca * size * 0.06f, bz = z + sa * size * 0.06f;
            float tx = bx + ca * size * lean, tz = bz + sa * size * lean;
            float px = -sa * hw, pz = ca * hw, tone = 0.95f + 0.2f * r.nextFloat();
            float dk = 0.5f * tone;
            b.sway(0);
            b.colorf(lr * dk, lg * dk, lb * dk);
            b.vert(bx - px, y - 0.03f, bz - pz, ca * 0.3f, 0.95f, sa * 0.3f, 0, 1);
            b.vert(bx + px, y - 0.03f, bz + pz, ca * 0.3f, 0.95f, sa * 0.3f, 1, 1);
            b.sway(1);
            b.colorf(lr * tone, lg * tone, lb * tone);
            b.vert(tx + px, y + size, tz + pz, ca * 0.3f, 0.95f, sa * 0.3f, 1, 0);
            b.sway(0);
            b.colorf(lr * dk, lg * dk, lb * dk);
            b.vert(bx - px, y - 0.03f, bz - pz, ca * 0.3f, 0.95f, sa * 0.3f, 0, 1);
            b.sway(1);
            b.colorf(lr * tone, lg * tone, lb * tone);
            b.vert(tx + px, y + size, tz + pz, ca * 0.3f, 0.95f, sa * 0.3f, 1, 0);
            b.vert(tx - px, y + size, tz - pz, ca * 0.3f, 0.95f, sa * 0.3f, 0, 0);
        }
        b.sway(0);
    }

    private void buildUndergrowth(World3D w, int z0, int z1) {
        Random r = new Random(21);
        // erba bassa e fitta su tutto il sentiero (tranne vicino a burroni e blocchi), con qualche fiore
        for (float z = 0; z < w.length; z += 0.4f) {
            for (float xx = -World3D.HALF; xx < World3D.HALF; xx += 0.4f) {
                float x = xx + r.nextFloat() * 0.4f, zz = z + r.nextFloat() * 0.4f;
                int xi = (int) Math.floor(x + World3D.HALF), zi = (int) Math.floor(zz);
                if (xi < 0 || xi >= World3D.W || vis(w, xi, zi) != 0 || fadeAt(x, zz) < 0.36f) continue;
                if (Math.abs(x - trailX(zz)) < TRAIL_HW * (0.5f + 0.35f * r.nextFloat())) continue;
                float e = Math.abs(x) / World3D.HALF;
                float size = 0.22f + 0.14f * r.nextFloat() + 0.2f * e * e * r.nextFloat();
                tuft(K_TUFT, x, groundAt(x, zz), zz, size, 3, 0.35f, r);
                if (r.nextFloat() < 0.07f) tuft(K_FLOWER, x, groundAt(x, zz), zz, 0.26f + 0.12f * r.nextFloat(), 3, 0.12f, r);
            }
        }
        for (int side = -1; side <= 1; side += 2) {
            // lati: densissimo vicino al sentiero, poi sempre più rado, con macchie di fiori
            for (float z = z0; z < z1; z += 0.34f + r.nextFloat() * 0.25f) {
                for (int k = 0; k < 4; k++) {
                    float d = 0.02f + (float) Math.pow(r.nextFloat(), 1.9f) * 10f;
                    float x = side * (World3D.HALF + d), y = groundAt(x, z), q = r.nextFloat();
                    boolean patch = vnoise(x * 0.45f + 9, z * 0.45f, 41) > 0.6f;
                    if (patch && q < 0.55f) tuft(K_FLOWER, x, y, z, 0.38f + r.nextFloat() * 0.3f, 3, 0.15f, r);
                    else if (d > 0.9f && q < 0.2f) fern(x, y, z, 0.5f + r.nextFloat() * 0.5f, r);
                    else tuft(K_TUFT, x, y, z, 0.3f + r.nextFloat() * 0.45f + 0.03f * d, d < 2f ? 3 : 4, 0.32f, r);
                }
            }
            // abeti giovani
            for (float z = z0; z < z1; z += 3.5f + r.nextFloat() * 5f) {
                float d = 1.4f + r.nextFloat() * 3.2f, x = side * (World3D.HALF + d), h = 1.0f + r.nextFloat() * 1.6f;
                if (jungle) palm(x, groundAt(x, z) - 0.05f, z, h * 1.6f + 1.4f, r);
                else if (autumn) broadleaf(x, groundAt(x, z) - 0.05f, z, h + 0.6f, h * 0.35f, 0.95f + 0.2f * r.nextFloat(), 3, r);
                else fir(x, groundAt(x, z) - 0.05f, z, h, h * 0.3f, 0.95f + 0.2f * r.nextFloat(), false, r);
            }
            // massi muschiati: appoggiati sulla quota più bassa della maglia di terreno sotto di loro e affondati
            for (float z = z0; z < z1; z += 3.5f + r.nextFloat() * 4f) {
                float d = 0.3f + r.nextFloat() * 3.5f, rad = 0.3f + r.nextFloat() * 0.5f;
                float rx = rad * 1.25f, ry = rad * 1.0f, rz = rad * 1.1f;
                d = Math.max(d, rx * 1.32f + 0.08f);                   // il masso (con le sue gobbe) resta fuori dal sentiero percorribile
                float x = side * (World3D.HALF + d);
                float low = meshAt(x, z);
                for (int k = 0; k < 14; k++) {
                    double a = k * Math.PI / 6;
                    low = Math.min(low, meshAt(x + (float) Math.cos(a) * rx * 1.1f, z + (float) Math.sin(a) * rz * 1.1f));
                    low = Math.min(low, meshAt(x + (float) Math.cos(a) * rx * 0.5f, z + (float) Math.sin(a) * rz * 0.5f));
                }
                float y = low + ry * 0.5f;
                MeshBuilder rb = mb(K_ROCK, z);
                lightAt(x, z, 1f);
                rb.colorf(lt[0] * 0.95f, lt[1] * 0.95f, lt[2] * 0.95f);
                rb.rock(x, y, z, rx, ry, rz, (int) (z * 31) + side, 10, 7);
            }
        }
    }

    private static final float[] SIDE_D = {0, 0.35f, 0.75f, 1.2f, 1.7f, 2.3f, SEAM, 4.6f, 6f, 8f, 11f, 15f, 20f, 27f, 36f};

    /** Altezza della superficie realmente disegnata ai lati del sentiero (interpolazione della maglia), non quella teorica. */
    float meshAt(float x, float z) {
        if (lip > 0 && edgeField(x, z) < 0.05f) return groundAt(x, z) - pitD;   // nel vuoto frastagliato: la decorazione finisce giù, invisibile
        float d = Math.abs(x) - World3D.HALF;
        if (d <= 0) return groundAt(x, z);
        float sg = Math.signum(x);
        int k = 0;
        while (k + 2 < SIDE_D.length && d >= SIDE_D[k + 1]) k++;
        float d0 = SIDE_D[k], d1 = SIDE_D[k + 1], fz0 = (float) Math.floor(z);
        float tx = Math.max(0, Math.min(1, (d - d0) / (d1 - d0))), tz = z - fz0;
        float a = groundAt(sg * (World3D.HALF + d0), fz0), b = groundAt(sg * (World3D.HALF + d1), fz0);
        float c = groundAt(sg * (World3D.HALF + d1), fz0 + 1), e = groundAt(sg * (World3D.HALF + d0), fz0 + 1);
        float bil = (a + (b - a) * tx) * (1 - tz) + (e + (c - e) * tx) * tz;
        return Math.min(bil, Math.min(Math.min(a, b), Math.min(c, e)) + 0.5f * (bil - Math.min(Math.min(a, b), Math.min(c, e))));
    }

    private void buildRays(int z0, int z1) {
        Random r = new Random(33);
        for (float z = z0; z < z1; z += 5f + r.nextFloat() * 5f) {
            MeshBuilder b = mb(K_RAY, z);
            float x = (r.nextFloat() < 0.5f ? -1 : 1) * (World3D.HALF - 1f + r.nextFloat() * 6.5f);
            float H = 9f + r.nextFloat() * 3f, t = H / Gfx3D.LY;
            float tx = x, ty = H, tz = z;
            float bx = x - Gfx3D.LX * t, bz = z - Gfx3D.LZ * t;
            float wd = 0.5f + r.nextFloat() * 0.7f;
            b.colorf(1, 1, 1);
            b.quad(bx - wd, 0, bz, bx + wd, 0, bz, tx + wd, ty, tz, tx - wd, ty, tz, 0, 1, 0, 0, 0, 1, 1);
            b.quad(bx, 0, bz - wd, bx, 0, bz + wd, tx, ty, tz + wd, tx, ty, tz - wd, 0, 1, 0, 0, 0, 1, 1);
        }
    }

    /** Altezza da disegnare: prima dell'inizio e dopo la fine il prato continua. */
    static int vis(World3D w, int x, int z) {
        if (z < 0 || z >= w.length) return 0;
        return w.height[z * World3D.W + x];
    }

    /** Parete di terra che scende nel vuoto e si scurisce verso il fondo. Vertici: a-b in alto, poi giù. */
    private void wall(MeshBuilder d, float ax, float az, float bx, float bz, float nx, float nz) {
        float top = -lip;
        d.colorf(1f, 0.95f, 0.9f);
        d.vert(ax, top, az, nx, 0, nz, 0, 0);
        d.vert(bx, top, bz, nx, 0, nz, 1, 0);
        if (heaven) d.colorf(0.80f, 0.88f, 1f); else d.colorf(0.04f, 0.05f, 0.045f);
        d.vert(bx, -pitD, bz, nx, 0, nz, 1, pitD);
        d.vert(bx, -pitD, bz, nx, 0, nz, 1, pitD);
        d.vert(ax, -pitD, az, nx, 0, nz, 0, pitD);
        d.colorf(1f, 0.95f, 0.9f);
        d.vert(ax, top, az, nx, 0, nz, 0, 0);
    }

    /**
     * Casella di prato accanto a un burrone: griglia fitta, ogni quadratino ritagliato dove il contorno frastagliato passa
     * (il pezzo nel vuoto non si disegna) e, lungo il taglio, una parete di terra irregolare che scende.
     */
    private void jaggedCell(final World3D w, MeshBuilder g, float fx, int z) {
        clipGrid(g, fx, fx + 1, z, z + 1, 8, 8, z, new Shade() {
            public void vert(MeshBuilder g, float x, float zz) {
                float ex = Math.abs(x) / World3D.HALF;
                float t = (1.08f - 0.10f * ex * ex) * aoAt(w, x, zz) * (1f - 0.7f * lipDrop(x, zz) / lip * 0.45f);
                ground(g, x, zz, x * 0.5f, zz * 0.5f, t, t, t);
            }
        });
    }

    /** Come colorare un vertice di terreno (prato del corridoio o prato dei lati). */
    private interface Shade {
        void vert(MeshBuilder g, float x, float z);
    }

    /** Rettangolo di terreno diviso in nx x nz quadratini, ritagliati sul contorno frastagliato, con la parete lungo il taglio. */
    private void clipGrid(MeshBuilder g, float xa, float xb, float za, float zb, int nxs, int nzs, int chunkZ, Shade sh) {
        float[] px = new float[8], pz = new float[8];
        boolean[] cut = new boolean[8];
        for (int sx = 0; sx < nxs; sx++) {
            for (int sz = 0; sz < nzs; sz++) {
                float qx0 = xa + (xb - xa) * sx / nxs, qx1 = xa + (xb - xa) * (sx + 1) / nxs;
                float qz0 = za + (zb - za) * sz / nzs, qz1 = za + (zb - za) * (sz + 1) / nzs;
                float[][] c = {{qx0, qz0}, {qx1, qz0}, {qx1, qz1}, {qx0, qz1}};
                if (xb < xa) {                                          // lato sinistro: stesso giro dei vertici
                    c = new float[][] {{qx1, qz0}, {qx0, qz0}, {qx0, qz1}, {qx1, qz1}};
                }
                float[] fv = new float[4];
                boolean any = false, all = true;
                for (int k = 0; k < 4; k++) {
                    fv[k] = edgeField(c[k][0], c[k][1]);
                    if (fv[k] >= 0) any = true; else all = false;
                }
                if (!any) continue;
                int n = 0;
                for (int k = 0; k < 4; k++) {                       // ritaglio del quadratino: resta la parte con terreno
                    int k2 = (k + 1) & 3;
                    if (fv[k] >= 0) { px[n] = c[k][0]; pz[n] = c[k][1]; cut[n] = false; n++; }
                    if ((fv[k] >= 0) != (fv[k2] >= 0)) {
                        float t = fv[k] / (fv[k] - fv[k2]);
                        px[n] = c[k][0] + (c[k2][0] - c[k][0]) * t; pz[n] = c[k][1] + (c[k2][1] - c[k][1]) * t; cut[n] = true; n++;
                    }
                }
                for (int k = 1; k + 1 < n; k++) {
                    sh.vert(g, px[0], pz[0]);
                    sh.vert(g, px[k], pz[k]);
                    sh.vert(g, px[k + 1], pz[k + 1]);
                }
                if (all) continue;
                for (int k = 0; k < n; k++) {                       // il lato tra due punti di taglio è il bordo: lì scende la parete
                    int k2 = (k + 1) % n;
                    if (cut[k] && cut[k2]) cliff(mb(K_DIRT, chunkZ), px[k], pz[k], px[k2], pz[k2]);
                }
            }
        }
    }

    /** Spostamento orizzontale della parete a una certa profondità: sporgenze e rientranze di roccia, uguali per pezzi vicini. */
    private float cliffDx(float x, float z, int row) {
        return row == 0 ? 0f : (vnoise(x * 2.2f + row * 5.3f, z * 2.2f, 71) - 0.5f) * 0.55f;
    }

    private float cliffDz(float x, float z, int row) {
        return row == 0 ? 0f : (vnoise(x * 2.2f, z * 2.2f + row * 5.3f, 72) - 0.5f) * 0.55f;
    }

    /**
     * Erba che pende dal bordo del burrone (ciuffi capovolti, un po' sporgenti) e ogni tanto un cespuglio a cavallo del bordo:
     * nasconde il punto dove il prato si piega e la parete comincia.
     */
    private void fringe(float ax, float az, float bx, float bz, float nx, float nz) {
        if (heaven || dry == 22 || dry == 24) return;
        float mx = (ax + bx) * 0.5f, mz = (az + bz) * 0.5f;
        float ex = (bx - ax) * 0.45f, ez = (bz - az) * 0.45f;                // un po' più larga del segmento: la frangia non ha buchi
        float len = 0.3f + 0.55f * vnoise(mx * 3.1f, mz * 3.1f, 81);
        float top = groundAt(mx, mz) + 0.04f;
        MeshBuilder t = mb(K_TUFT, (int) Math.floor(mz));
        lightAt(mx, mz, 1f);
        for (int layer = 0; layer < 2; layer++) {
            float out = 0.03f + layer * 0.07f, lean = 0.12f + layer * 0.12f, l2 = len * (1f - 0.3f * layer);
            float tone = 0.9f + 0.2f * vnoise(mx * 7f + layer, mz * 7f, 82), dk = 0.45f * tone;
            float x0 = mx + nx * out, z0 = mz + nz * out, x1 = mx + nx * (out + lean), z1 = mz + nz * (out + lean);
            float[][] p = {{x0 - ex * 1.6f, top, z0 - ez * 1.6f, 0, 1}, {x0 + ex * 1.6f, top, z0 + ez * 1.6f, 1, 1},
                    {x1 + ex * 1.9f, top - l2, z1 + ez * 1.9f, 1, 0}, {x1 - ex * 1.9f, top - l2, z1 - ez * 1.9f, 0, 0}};
            int[] ord = {0, 1, 2, 0, 2, 3};
            for (int k : ord) {
                boolean tip = k >= 2;
                float c = tip ? dk : tone;                                   // in alto alla luce, le punte che pendono in ombra
                t.colorf(lt[0] * c, lt[1] * c, lt[2] * c);
                t.sway(tip ? 0.6f : 0f);
                t.vert(p[k][0], p[k][1], p[k][2], nx * 0.5f, 0.85f, nz * 0.5f, p[k][3], p[k][4]);
            }
        }
        t.sway(0);
        // cespuglio a cavallo del bordo, ogni tanto
        float hsh = vnoise(mx * 9.3f, mz * 9.3f, 83);
        if (dry == 0 && Math.abs(mx) > 1.3f && hsh > 0.78f && Math.abs(Math.sin(mx * 12.9898f + mz * 78.233f) * 43758.5453f) % 1f < 0.07f) {
            float rad = 0.22f + 0.2f * vnoise(mx * 2f, mz * 2f, 84);
            float[] tc = tints()[(int) (hsh * 997) % tints().length];
            float[] col = {tc[0] * lt[0] * 0.62f, tc[1] * lt[1] * 0.62f, tc[2] * lt[2] * 0.62f};
            foliageLobe(mb(K_BLOB, (int) Math.floor(mz)), mx + nx * rad * 0.35f, top - rad * 0.35f, mz + nz * rad * 0.35f, rad * 1.2f, rad, rad * 1.2f, 7, 4,
                    col, 0.95f, 0.3f, new Random((long) (mx * 1000 + mz * 7)));
        }
    }

    /** Parete frastagliata sotto il segmento di bordo a-b: quattro fasce che sporgono e rientrano, faccette come roccia. */
    private void cliff(MeshBuilder d, float ax, float az, float bx, float bz) {
        float[] ys = {0f, -0.9f, -2.0f, -pitD};
        float ta = groundAt(ax, az), tb = groundAt(bx, bz);
        for (int r = 0; r < 3; r++) {
            float ya0 = r == 0 ? ta : ys[r], yb0 = r == 0 ? tb : ys[r], y1 = ys[r + 1];
            float[][] p = {
                {ax + cliffDx(ax, az, r), ya0, az + cliffDz(ax, az, r)},
                {bx + cliffDx(bx, bz, r), yb0, bz + cliffDz(bx, bz, r)},
                {bx + cliffDx(bx, bz, r + 1), y1, bz + cliffDz(bx, bz, r + 1)},
                {ax + cliffDx(ax, az, r + 1), y1, az + cliffDz(ax, az, r + 1)}};
            // normale della faccetta, girata verso il vuoto (dalla parte dove il campo è negativo)
            float ux = p[1][0] - p[0][0], uy = p[1][1] - p[0][1], uz = p[1][2] - p[0][2];
            float vx = p[3][0] - p[0][0], vy = p[3][1] - p[0][1], vz = p[3][2] - p[0][2];
            float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
            float nl = (float) Math.sqrt(nx * nx + ny * ny + nz * nz) + 1e-6f;
            nx /= nl; ny /= nl; nz /= nl;
            float mx = (ax + bx) * 0.5f, mz = (az + bz) * 0.5f;
            if (edgeField(mx + nx * 0.05f, mz + nz * 0.05f) > edgeField(mx - nx * 0.05f, mz - nz * 0.05f)) { nx = -nx; ny = -ny; nz = -nz; }
            if (r == 0) {
                float hl = (float) Math.sqrt(nx * nx + nz * nz) + 1e-6f;
                fringe(ax, az, bx, bz, nx / hl, nz / hl);
            }
            float la = (float) Math.sqrt((bx - ax) * (bx - ax) + (bz - az) * (bz - az));
            float ua = ax + az, ub = ua + la;
            float[] u = {ua, ub, ub, ua};
            int[] ord = {0, 1, 2, 0, 2, 3};
            for (int k : ord) {
                float depth = -p[k][1] / pitD;
                float c = heaven ? 1f - 0.2f * depth : 1f - 0.96f * (float) Math.pow(Math.max(0f, Math.min(1f, depth * 1.15f)), 0.7f);
                if (heaven) d.colorf(c * 0.85f, c * 0.92f, c); else d.colorf(c, c * 0.95f, c * 0.9f);
                d.vert(p[k][0], p[k][1], p[k][2], nx, ny, nz, u[k] * 0.5f, -p[k][1] * 0.5f);
            }
        }
    }

    /** Parete lungo x alla quota z; dir = +1 se il vuoto sta verso z minore. */
    private void wallZ(MeshBuilder d, float fx, float z, int dir) {
        wall(d, fx, z, fx + 1, z, 0, -1);
    }

    private void wallX(MeshBuilder d, float x, float z, int dir) {
        wall(d, x, z, x, z + 1, dir > 0 ? -1 : 1, 0);
    }

    private static void pumpVert(MeshBuilder b, double a, double ph, float rx, float ry, float cy) {
        float ux = (float) (Math.sin(a) * Math.cos(ph)), uy = (float) Math.cos(a), uz = (float) (Math.sin(a) * Math.sin(ph));
        float rib = 1f + 0.07f * (float) Math.cos(10 * ph);
        float py = cy + uy * ry * (1f - 0.16f * (float) Math.pow(Math.abs(uy), 8));
        float nx = ux / rx, ny = uy / ry, nz = uz / rx;
        float wave = 0.7f * (float) Math.sin(10 * ph) * (float) Math.sin(a);
        nx += -uz * wave / Math.max(0.2f, (float) Math.sin(a)) * 0.3f;
        nz += ux * wave / Math.max(0.2f, (float) Math.sin(a)) * 0.3f;
        float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        float k = 0.80f + 0.2f * (0.5f + 0.5f * (float) Math.cos(10 * ph)), g = 0.88f + 0.12f * uy;
        b.colorf(0.98f * k * g, 0.54f * k * g, 0.15f * k * g);
        b.vert(ux * rx * rib, py, uz * rx * rib, nx / l, ny / l, nz / l, 0.5f, 0.5f);
    }

    /** Cassa bonus d'autunno: una zucca a spicchi con una stella luminosa intagliata sui quattro lati. */
    private void buildPumpkin() {
        MeshBuilder b = new MeshBuilder();
        int segs = 30, rings = 14;
        float rx = 0.5f, ry = 0.45f, cy = 0.46f;
        for (int i = 0; i < rings; i++) {
            double a0 = Math.PI * i / rings, a1 = Math.PI * (i + 1) / rings;
            for (int j = 0; j < segs; j++) {
                double p0 = 2 * Math.PI * j / segs, p1 = 2 * Math.PI * (j + 1) / segs;
                pumpVert(b, a0, p0, rx, ry, cy); pumpVert(b, a1, p0, rx, ry, cy); pumpVert(b, a1, p1, rx, ry, cy);
                pumpVert(b, a0, p0, rx, ry, cy); pumpVert(b, a1, p1, rx, ry, cy); pumpVert(b, a0, p1, rx, ry, cy);
            }
        }
        b.colorf(0.40f, 0.52f, 0.18f);
        b.cylinder(0.02f, cy + ry * 0.82f, cy + ry + 0.2f, 0, 0.075f, 0.05f, 7, true, false);
        crate = b.build(-1, false, true);
        crate.spec = 0.3f; crate.shine = 26f;
        MeshBuilder st = new MeshBuilder();
        for (int f = 0; f < 4; f++) {
            float ph = f * (float) Math.PI / 2, dx = (float) Math.cos(ph), dz = (float) Math.sin(ph), tx = -dz, tz = dx;
            float[][] pts = new float[10][2];
            for (int i = 0; i < 10; i++) {
                float ang = (float) (-Math.PI / 2 + i * Math.PI / 5), rr = i % 2 == 0 ? 0.2f : 0.085f;
                pts[i][0] = (float) Math.cos(ang) * rr;
                pts[i][1] = (float) Math.sin(ang) * rr;
            }
            st.colorf(1f, 0.88f, 0.32f);
            for (int i = 0; i < 10; i++) {
                float[][] tri = {{0, 0}, pts[i], pts[(i + 1) % 10]};
                for (float[] v : tri) {
                    float t = v[0], hgt = v[1];
                    float out = rx * (float) Math.sqrt(Math.max(0.02f, 1 - (t / rx) * (t / rx) - (hgt / ry) * (hgt / ry))) * (1f + 0.07f * (float) Math.cos(10 * ph)) + 0.02f;
                    st.vert(dx * out + tx * t, cy + hgt, dz * out + tz * t, dx, 0, dz, 0.5f, 0.5f);
                }
            }
        }
        crateStar = st.build(-1, false, true);
    }

    private void buildObjects() {
        MeshBuilder b = new MeshBuilder();
        b.quad(-0.5f, -0.5f, 0, 0.5f, -0.5f, 0, 0.5f, 0.5f, 0, -0.5f, 0.5f, 0, 0, 0, 1, 0, 0, 1, 1);
        b.quad(0.5f, -0.5f, 0, -0.5f, -0.5f, 0, -0.5f, 0.5f, 0, 0.5f, 0.5f, 0, 0, 0, -1, 0, 0, 1, 1);
        leaf = b.build(Img.R3_LEAF, false, true);
        leaf.cutout = true;
        b = new MeshBuilder();
        b.roundBox(0, 0.5f, 0, 0.96f, 0.96f, 0.96f, 0.07f, 63, 3);              // spigoli smussati come una cassa vera
        crate = b.build(Img.BONUS, false, true);
        crate.spec = 0.1f; crate.shine = 20f;
        if ((autumn && !spring && !jungle) || aurora) buildPumpkin();
        if (jungle || dry != 0) buildIsland();
        buildBoom();
        if (spring || heaven) {                                        // casse gialle con la stella rosa; fiori a molla e rovi
            b = new MeshBuilder();
            b.roundBox(0, 0.5f, 0, 0.96f, 0.96f, 0.96f, 0.16f, 63, 3);
            crate = b.build(heaven ? Img.HV_BONUS : Img.SP_BONUS, false, true);
            crate.spec = 0.3f; crate.shine = 24f;
            b = new MeshBuilder().color(heaven ? 250 : 86, heaven ? 246 : 168, heaven ? 238 : 62);   // base del fiore a molla (nuvola in cielo), con anelli rosa o dorati
            b.cylinder(0, 0, 0.17f, 0, 0.68f, 0.5f, 14, true, true);
            if (heaven) b.colorf(1f, 0.82f, 0.3f); else
            b.colorf(0.95f, 0.38f, 0.6f);
            b.cylinder(0, 0.17f, 0.24f, 0, 0.52f, 0.56f, 14, true, true);
            padDome = b.build(-1, false, true);
            padDome.spec = 0.25f; padDome.shine = 18f;
            b = new MeshBuilder();                                     // corolla vista dall'alto
            b.quad(-0.82f, 0, -0.82f, -0.82f, 0, 0.82f, 0.82f, 0, 0.82f, 0.82f, 0, -0.82f, 0, 1, 0, 0, 0, 1, 1);
            padTop = b.build(heaven ? Img.R3H_SPRING : Img.R3D_SPRING, true, true);
            thornMesh = FoeModels4.bramble();                          // cespuglio di rovo in fiore, spine vere
            b = new MeshBuilder().color(255, 198, 24);                 // ape: addome giallo a strisce nere, testa scura (asse lungo y)
            b.ellipsoid(0, 0, 0, 0.1f, 0.17f, 0.1f, 10, 7);
            b.colorf(0.1f, 0.07f, 0.03f);
            b.cylinder(0, -0.085f, -0.035f, 0, 0.098f, 0.1f, 10, false, false);
            b.cylinder(0, 0.03f, 0.08f, 0, 0.1f, 0.088f, 10, false, false);
            b.ellipsoid(0, 0.2f, 0, 0.052f, 0.05f, 0.052f, 8, 6);
            b.bipyramid(0, -0.2f, 0, 0.035f, 0.05f, 5);
            beeBody = b.build(-1, false, true);
            beeBody.spec = 0.1f; beeBody.shine = 10f;
            b = new MeshBuilder().color(235, 245, 255);                // ali: due piccole lame traslucide
            b.quad(0.04f, 0.1f, 0.02f, 0.3f, 0.12f, 0.1f, 0.3f, -0.02f, 0.1f, 0.04f, -0.02f, 0.02f, 0, 1, 0, 0, 0, 1, 1);
            b.quad(0.04f, -0.02f, 0.02f, 0.3f, -0.02f, 0.1f, 0.3f, 0.12f, 0.1f, 0.04f, 0.1f, 0.02f, 0, -1, 0, 0, 0, 1, 1);
            b.quad(-0.04f, 0.1f, 0.02f, -0.04f, -0.02f, 0.02f, -0.3f, -0.02f, 0.1f, -0.3f, 0.12f, 0.1f, 0, 1, 0, 0, 0, 1, 1);
            b.quad(-0.04f, -0.02f, 0.02f, -0.04f, 0.1f, 0.02f, -0.3f, 0.12f, 0.1f, -0.3f, -0.02f, 0.1f, 0, -1, 0, 0, 0, 1, 1);
            beeWings = b.build(-1, true, true);
            b = new MeshBuilder().color(226, 182, 104);                // nido: cesta di paglia a fasce, con l'ingresso scuro sul davanti (verso chi arriva)
            float[] hr = {0.5f, 0.62f, 0.67f, 0.56f, 0.38f};
            for (int i = 0; i < 5; i++) {
                if (i % 2 == 0) b.color(226, 182, 104); else b.color(190, 140, 70);
                b.ellipsoid(0, -0.62f + i * 0.31f, 0, hr[i], 0.2f, hr[i], 12, 6);
            }
            b.color(150, 108, 56);
            b.ellipsoid(0, 0.95f, 0, 0.1f, 0.1f, 0.1f, 8, 5);
            b.color(36, 24, 12);
            b.ellipsoid(0, -0.18f, -0.6f, 0.19f, 0.15f, 0.09f, 10, 6);
            hiveMesh = b.build(-1, false, true);
            hiveMesh.spec = 0.05f; hiveMesh.shine = 6f;
            b = new MeshBuilder().color(98, 68, 44);                   // ciliegio del nido: tronco e ramo, con grandi chiome di fiori
            b.cylinder(0, 0, 7.2f, 0, 0.52f, 0.36f, 10, true, true);
            b.colorf(1.0f, 0.76f, 0.84f);
            b.ellipsoid(0, 7.2f, 0, 2.3f, 1.4f, 2.3f, 12, 7);
            b.colorf(0.96f, 0.64f, 0.78f);
            b.ellipsoid(-1.3f, 6.5f, 0.6f, 1.5f, 1.0f, 1.5f, 10, 6);
            b.colorf(1.0f, 0.86f, 0.9f);
            b.ellipsoid(0.9f, 6.9f, -0.8f, 1.5f, 1.0f, 1.5f, 10, 6);
            hiveTree = b.build(-1, false, true);
            b = new MeshBuilder().color(98, 68, 44);
            b.cylinder(0, 0, 3.7f, 0, 0.22f, 0.13f, 8, true, true);
            hiveBranch = b.build(-1, false, true);
            b = new MeshBuilder().color(150, 120, 80);
            b.cylinder(0, 0, 1.0f, 0, 0.035f, 0.035f, 6, false, false);
            hiveRope = b.build(-1, false, true);
        }
        if (fire) {                                                    // casse di roccia incandescente
            b = new MeshBuilder();
            b.roundBox(0, 0.5f, 0, 0.96f, 0.96f, 0.96f, 0.16f, 63, 3);
            crate = b.build(Img.R3C_LAVA, false, true);
            crate.spec = 0.2f; crate.shine = 20f;
            b = new MeshBuilder();                                     // fiamma e brace: due piani incrociati, visibili da entrambi i lati
            b.quad(-0.5f, 0, 0, 0.5f, 0, 0, 0.5f, 1, 0, -0.5f, 1, 0, 0, 0, 1, 0, 0, 1, 1);
            b.quad(0.5f, 0, 0, -0.5f, 0, 0, -0.5f, 1, 0, 0.5f, 1, 0, 0, 0, -1, 0, 0, 1, 1);
            flame = b.build(Img.R3C_FLAME, true, true);
            b = new MeshBuilder();
            b.quad(-0.5f, -0.5f, 0, 0.5f, -0.5f, 0, 0.5f, 0.5f, 0, -0.5f, 0.5f, 0, 0, 0, 1, 0, 0, 1, 1);
            b.quad(0.5f, -0.5f, 0, -0.5f, -0.5f, 0, -0.5f, 0.5f, 0, 0.5f, 0.5f, 0, 0, 0, -1, 0, 0, 1, 1);
            ember = b.build(Img.R3C_EMBER, true, true);
            b = new MeshBuilder().color(150, 105, 80);                 // tronco in fiamme che rotola (asse lungo z)
            b.cylinder(0, -0.65f, 0.65f, 0, 0.42f, 0.42f, 12, true, true);
            logMesh = b.build(Img.R3_BARK, false, true);
            b = new MeshBuilder();                                     // bocca dei getti: disco piatto sul terreno
            b.quad(-0.95f, 0, -0.95f, -0.95f, 0, 0.95f, 0.95f, 0, 0.95f, 0.95f, 0, -0.95f, 0, 1, 0, 0, 0, 1, 1);
            vent = b.build(Img.R3C_VENT, true, true);
            logMesh.spec = 0.05f; logMesh.shine = 8f;
        }
        if (aurora) {                                                  // casse di ghiaccio con la stella luminosa
            b = new MeshBuilder();
            b.roundBox(0, 0.5f, 0, 0.96f, 0.96f, 0.96f, 0.16f, 63, 3);
            crate = b.build(Img.R3B_ICE, false, true);
            crate.spec = 0.7f; crate.shine = 40f;
            b = new MeshBuilder();
            b.quad(-0.5f, -0.5f, 0, 0.5f, -0.5f, 0, 0.5f, 0.5f, 0, -0.5f, 0.5f, 0, 0, 0, 1, 0, 0, 1, 1);
            b.quad(0.5f, -0.5f, 0, -0.5f, -0.5f, 0, -0.5f, 0.5f, 0, 0.5f, 0.5f, 0, 0, 0, -1, 0, 0, 1, 1);
            flake = b.build(Img.R3B_FLAKE, true, true);
        }

        b = new MeshBuilder().color(46, 214, 204);
        b.bipyramid(0, 0, 0, 0.2f, 0.36f, 6);
        gem = b.build(-1, false, true);
        gem.spec = 0.9f; gem.shine = 60f;

        b = new MeshBuilder().color(236, 192, 66);                            // chiave dorata: anello in alto, gambo, due denti
        for (int i = 0; i < 12; i++) {
            double an = i * Math.PI * 2 / 12;
            b.ellipsoid((float) Math.cos(an) * 0.13f, 0.34f + (float) Math.sin(an) * 0.13f, 0, 0.055f, 0.055f, 0.045f, 8, 5);
        }
        b.box(0, 0.02f, 0, 0.07f, 0.44f, 0.07f);
        b.color(252, 214, 92);
        b.box(0, 0.2f, 0, 0.14f, 0.05f, 0.09f);
        b.color(236, 192, 66);
        b.box(0.065f, -0.12f, 0, 0.12f, 0.07f, 0.07f);
        b.box(0.05f, -0.01f, 0, 0.09f, 0.06f, 0.07f);
        battery = b.build(-1, false, true);
        battery.spec = 0.95f; battery.shine = 60f;
        b = new MeshBuilder().color(255, 240, 170);
        for (int i = 0; i < 12; i++) {
            double an = i * Math.PI * 2 / 12;
            b.ellipsoid((float) Math.cos(an) * 0.13f, 0.34f + (float) Math.sin(an) * 0.13f, 0, 0.03f, 0.03f, 0.052f, 6, 4);
        }
        batteryGlow = b.build(-1, false, true);

        b = new MeshBuilder();
        b.quad(-0.5f, 0, -0.5f, 0.5f, 0, -0.5f, 0.5f, 0, 0.5f, -0.5f, 0, 0.5f, 0, 1, 0, 0, 0, 1, 1);
        shadow = b.build(Img.SHADOW, true, true);

        b = new MeshBuilder().color(225, 244, 255);
        b.ring(0, 0, 0, 0.86f, 1f, 28);
        ring = b.build(-1, true, true);

        // portone del traguardo: una muraglia di pietre irregolari altissima (la cima non si vede) e larga oltre il bosco,
        // l'arco del portone, e dietro i battenti un corridoio buio che finisce in una luce bianca: oltre non si vede nulla
        b = new MeshBuilder();
        {
            java.util.Random rg = new java.util.Random(77);
            float y = -0.6f;
            int row = 0;
            while (y < 42f) {
                float sh = y < 6f ? 0.8f + 0.25f * rg.nextFloat() : 1.3f + 0.6f * rg.nextFloat();   // in alto pietre più grandi (non servono dettagli)
                float x = -16f - rg.nextFloat() * 1.2f;
                while (x < 16f) {
                    float sw = sh * (1.1f + 0.9f * rg.nextFloat());
                    float cx = x + sw / 2;
                    boolean door = Math.abs(cx) < 1.3f + sw / 2 - 0.05f && y < 3.7f;              // vano del portone
                    if (!door) {
                        float t = 0.82f + 0.24f * rg.nextFloat();
                        b.colorf(t, t * (0.96f + 0.06f * rg.nextFloat()), t * (0.92f + 0.06f * rg.nextFloat()));
                        int mk = b.mark();
                        float d = 0.9f + 0.4f * rg.nextFloat();
                        if (y < 7f && Math.abs(cx) < 8f) b.roundBox(cx, y + sh / 2, 0.15f * rg.nextFloat(), sw * 0.97f, sh * 0.96f, d, Math.min(sw, sh) * 0.18f, 63, 1);
                        else b.box(cx, y + sh / 2, 0.2f * rg.nextFloat(), sw * 0.97f, sh * 0.96f, d);
                        b.rough(mk, Math.min(sw, sh) * 0.09f, 1.3f, 300 + row);
                    }
                    x += sw;
                }
                y += sh;
                row++;
            }
            // malta scura dietro le pietre (niente buchi verso il cielo)
            b.colorf(0.42f, 0.4f, 0.37f);
            b.box(-9.3f, 21f, 0.55f, 13.4f, 44f, 0.4f);
            b.box(9.3f, 21f, 0.55f, 13.4f, 44f, 0.4f);
            b.box(0, 23f, 0.55f, 5.2f, 40f, 0.4f);
            // arco: pilastri e architrave più chiari
            b.colorf(1.02f, 1.0f, 0.96f);
            for (int side = -1; side <= 1; side += 2) for (int k = 0; k < 4; k++) { int mk = b.mark(); b.roundBox(side * 1.6f, 0.45f + k * 0.9f, -0.1f, 0.62f, 0.88f, 1.2f, 0.1f, 63, 1); b.rough(mk, 0.04f, 2f, 400 + k + side * 9); }
            for (int i = 0; i < 5; i++) { int mk = b.mark(); b.roundBox(-1.68f + i * 0.84f, 3.95f, -0.1f, 0.82f, 0.8f, 1.25f, 0.1f, 63, 1); b.rough(mk, 0.04f, 2f, 420 + i); }
            // corridoio dietro i battenti: pareti e volta scure
            b.colorf(0.16f, 0.15f, 0.14f);
            b.box(-1.45f, 1.9f, 3.2f, 0.3f, 3.8f, 6.4f);
            b.box(1.45f, 1.9f, 3.2f, 0.3f, 3.8f, 6.4f);
            b.box(0, 3.75f, 3.2f, 2.9f, 0.3f, 6.4f);
            b.box(0, -0.05f, 3.2f, 2.9f, 0.1f, 6.4f);
        }
        gateStone = b.build(Img.R3_ROCK, false, true);
        gateStone.castShadow = false;
        b = new MeshBuilder().color(255, 248, 230);                          // la luce in fondo al corridoio
        b.quad(-1.3f, 0, 6.3f, 1.3f, 0, 6.3f, 1.3f, 3.6f, 6.3f, -1.3f, 3.6f, 6.3f, 0, 0, -1, 0, 0, 1, 1);
        gateLight = b.build(-1, false, false);
        gateStone.spec = 0.07f; gateStone.shine = 14f;
        b = new MeshBuilder();                                                // battente: cardine sull'origine, si estende verso x positivo
        b.box(0.65f, 1.65f, 0, 1.3f, 3.6f, 0.12f);
        gateDoor = b.build(Img.R3_WOOD, false, true);
        b = new MeshBuilder();                                                // catapulta: base di legno, montanti e braccio inclinato
        b.colorf(0.55f, 0.36f, 0.2f);
        b.roundBox(0, 0.2f, 0, 2.0f, 0.4f, 1.6f, 0.08f);
        b.box(-0.8f, 0.9f, 0, 0.18f, 1.2f, 1.2f);
        b.box(0.8f, 0.9f, 0, 0.18f, 1.2f, 1.2f);
        b.box(0, 1.3f, 0, 1.8f, 0.16f, 0.16f);
        b.colorf(0.45f, 0.28f, 0.14f);
        b.box(0, 1.5f, -0.45f, 0.22f, 0.22f, 1.6f);
        b.colorf(0.85f, 0.85f, 0.2f);
        b.ellipsoid(0, 1.7f, 0.5f, 0.38f, 0.16f, 0.38f, 14, 8);
        catapult = b.build(Img.R3_WOOD, false, true);
        catapult.spec = 0.1f; catapult.shine = 16f;
        gateDoor.spec = 0.12f; gateDoor.shine = 18f;

    }

    /** Punta a tre facce: base centrata in P, direzione D, lunghezza len, mezza larghezza w. */
    private static void spike(MeshBuilder b, float px, float py, float pz, float dx, float dy, float dz, float len, float w, float[] c0, float[] c1) {
        float l = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        dx /= l; dy /= l; dz /= l;
        // due assi perpendicolari a D
        float ax = Math.abs(dy) < 0.9f ? 0 : 1, ay = Math.abs(dy) < 0.9f ? 1 : 0;
        float e1x = dy * 0 - dz * ay, e1y = dz * ax - dx * 0, e1z = dx * ay - dy * ax;
        float el = (float) Math.sqrt(e1x * e1x + e1y * e1y + e1z * e1z);
        e1x /= el; e1y /= el; e1z /= el;
        float e2x = dy * e1z - dz * e1y, e2y = dz * e1x - dx * e1z, e2z = dx * e1y - dy * e1x;
        float[][] base = new float[3][3];
        for (int i = 0; i < 3; i++) {
            float a = i * 2.0944f, c = (float) Math.cos(a) * w, s2 = (float) Math.sin(a) * w;
            base[i][0] = px + e1x * c + e2x * s2; base[i][1] = py + e1y * c + e2y * s2; base[i][2] = pz + e1z * c + e2z * s2;
        }
        float tx = px + dx * len, ty = py + dy * len, tz = pz + dz * len;
        for (int i = 0; i < 3; i++) {
            float[] a = base[i], c = base[(i + 1) % 3];
            float ux = c[0] - a[0], uy = c[1] - a[1], uz = c[2] - a[2], vx = tx - a[0], vy = ty - a[1], vz = tz - a[2];
            float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx, nl = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            nx /= nl; ny /= nl; nz /= nl;
            float mx = (a[0] + c[0]) / 2 - px, my = (a[1] + c[1]) / 2 - py, mz = (a[2] + c[2]) / 2 - pz;
            if (nx * mx + ny * my + nz * mz < 0) { nx = -nx; ny = -ny; nz = -nz; }
            b.colorf(c0[0], c0[1], c0[2]);
            b.vert(a[0], a[1], a[2], nx, ny, nz, 0.5f, 0.5f);
            b.vert(c[0], c[1], c[2], nx, ny, nz, 0.5f, 0.5f);
            b.colorf(c1[0], c1[1], c1[2]);
            b.vert(tx, ty, tz, nx, ny, nz, 0.5f, 0.5f);
        }
    }

    private static float[] rgb(int r, int g, int b) {
        return new float[] {r / 255f, g / 255f, b / 255f};
    }

    private static float[] mix(float[] a, float[] b, float t, float k) {
        return new float[] {(a[0] + (b[0] - a[0]) * t) * k, (a[1] + (b[1] - a[1]) * t) * k, (a[2] + (b[2] - a[2]) * t) * k};
    }

    /** Ellissoide con sfumatura dal basso (bot) all'alto (top) e normali morbide. */
    private static void gball(MeshBuilder b, float cx, float cy, float cz, float rx, float ry, float rz, int segs, int rings, float[] bot, float[] top) {
        float[][] p = new float[(rings + 1) * (segs + 1)][6];
        for (int i = 0; i <= rings; i++) {
            double a = Math.PI * i / rings;
            for (int j = 0; j <= segs; j++) {
                double t = 2 * Math.PI * j / segs;
                float ux = (float) (Math.sin(a) * Math.cos(t)), uy = (float) Math.cos(a), uz = (float) (Math.sin(a) * Math.sin(t));
                float[] q = p[i * (segs + 1) + j];
                q[0] = cx + ux * rx; q[1] = cy + uy * ry; q[2] = cz + uz * rz;
                float nx = ux / rx, ny = uy / ry, nz = uz / rz, l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                q[3] = nx / l; q[4] = ny / l; q[5] = nz / l;
            }
        }
        for (int i = 0; i < rings; i++) {
            for (int j = 0; j < segs; j++) {
                float[][] qs = {p[i * (segs + 1) + j], p[(i + 1) * (segs + 1) + j], p[(i + 1) * (segs + 1) + j + 1], p[i * (segs + 1) + j + 1]};
                int[] ord = {0, 1, 2, 0, 2, 3};
                for (int k : ord) {
                    float[] q = qs[k];
                    float t = sm((q[4] + 0.6f) / 1.6f);
                    b.colorf(bot[0] + (top[0] - bot[0]) * t, bot[1] + (top[1] - bot[1]) * t, bot[2] + (top[2] - bot[2]) * t);
                    b.vert(q[0], q[1], q[2], q[3], q[4], q[5], 0.5f, 0.5f);
                }
            }
        }
    }

    /** Penna piatta: striscia che parte da (ox,oy,oz) in direzione angolo ang nel piano XZ, si assottiglia e si solleva in punta. */
    private static void feather(MeshBuilder b, float ox, float oy, float oz, float ang, float len, float wid, float rise, float[] c0, float[] c1) {
        float dx = (float) Math.cos(ang), dz = (float) Math.sin(ang), px = -dz, pz = dx;
        int n = 4;
        float[][] l = new float[n + 1][3], r = new float[n + 1][3];
        for (int i = 0; i <= n; i++) {
            float t = i / (float) n, w = wid * (float) Math.sqrt(Math.max(0.02f, 1f - t * t * 0.9f)) * (0.55f + 0.45f * (1 - t * 0.3f));
            float cx = ox + dx * len * t, cz = oz + dz * len * t, cy = oy + rise * t * t;
            l[i][0] = cx + px * w; l[i][1] = cy; l[i][2] = cz + pz * w;
            r[i][0] = cx - px * w; r[i][1] = cy; r[i][2] = cz - pz * w;
        }
        for (int i = 0; i < n; i++) {
            float t0 = i / (float) n, t1 = (i + 1) / (float) n;
            float[][] q = {l[i], r[i], r[i + 1], l[i + 1]};
            float[] cc = {t0, t0, t1, t1};
            int[] ord = {0, 1, 2, 0, 2, 3};
            for (int k : ord) {
                float[] col = mix(c0, c1, cc[k], 1f);
                b.colorf(col[0], col[1], col[2]);
                b.vert(q[k][0], q[k][1], q[k][2], 0, 1, 0, 0.5f, 0.5f);
            }
        }
    }

    // ------------------------------------------------------------------ fotogramma

    public void render(World3D w, Gfx3D g, float aspect) {
        if (builtFor != w.length) build(w);

        // camera dietro le spalle: segue il giocatore, un po' meno di traverso, e l'altezza del terreno con dolcezza
        float tx = w.px * 0.6f, tz = w.pz;
        boolean wantBack = w.pz < w.camBackUntil;
        if (!camInit) { camX = tx; camZ = tz; camY = w.groundY; camInit = true; backK = wantBack ? 1 : 0; }
        backK += ((wantBack ? 1f : 0f) - backK) * 0.035f;            // si gira con calma alla fine della fuga
        back = backK > 0.5f;
        camX += (tx - camX) * 0.25f;
        camZ = tz;
        // la camera sta sopra anche a ciò che c'è sotto di lei (6 m dietro il robot): niente terrazze che le passano attraverso
        float behind = Math.max(0f, w.topAt(Math.max(-2.4f, Math.min(2.4f, camX)), camZ + (back ? 16f : -6.0f)));
        float base = Math.max(0, w.groundY);
        // lo sguardo segue il terreno del robot (non la struttura alle spalle); l'occhio invece sale quanto basta perché la
        // linea fino alla testa del robot passi sopra le terrazze rimaste dietro: si guarda giù oltre il bordo, il robot resta visibile
        camY += ((back ? Math.max(base, behind) : base) - camY) * 0.06f;
        float head = base + 1.0f, eye = base + 3.3f;
        if (!back) {
            for (float d = 0.5f; d <= 6.01f; d += 0.5f) {
                float sx = w.px + (camX - w.px) * d / 6f;
                float h = Math.max(0f, w.topAt(Math.max(-2.4f, Math.min(2.4f, sx)), w.pz - d));
                if (h > base + 0.2f) eye = Math.max(eye, head + (h + 0.45f - head) * 6f / d);
            }
            eye = Math.min(eye, base + 11f);
        }
        if (!camEyeInit) { camEye = eye; camEyeInit = true; }
        camEye += (eye - camEye) * (eye > camEye ? 0.18f : 0.06f);       // sale in fretta (mai coperto), scende con calma
        float lift = Math.max(0, w.py - camY) * 0.35f;
        float bk = backK * backK * (3 - 2 * backK);
        float eyeY = back ? camY + 3.3f : camEye;
        // davanti al robot, più in alto, guardando indietro: si vede il masso che arriva e il robot che corre verso lo schermo
        // nella fuga la camera sta lontana e alta davanti al robot: gli ostacoli verso cui corre si vedono con 2-3 secondi d'anticipo
        Mat4.lookAt(view, camX * (1 - 0.6f * bk), eyeY + lift + 5.2f * bk, camZ - 6.0f + 21f * bk, camX * 0.9f * (1 - 0.6f * bk), camY + 1.0f + lift - 0.3f * bk, camZ + 5f - 2.5f * bk);
        nearZ = back ? w.pz - 60f : w.pz - 4f;
        farZ = back ? w.pz + 34f : w.pz + 120f;                       // tutto quello che c'è tra il robot e la camera va disegnato
        Mat4.perspective(proj, 58f, aspect, 0.3f, 400f);
        boolean dark = isle == 18 || dry == 24;
        g.begin(view, proj, fogR, fogG, fogB, dark ? 14f : 30f, dark ? 62f : 115f, dark ? 0.97f : 0.9f);
        g.grade(1.04f, 0.95f, 0.86f);                                   // estate: verde meno acido, luce più calda
        if (heaven) g.grade(1.08f, 1.04f, 0.98f);
        else if (spring) g.grade(1.04f, 0.97f, 0.90f);
        else if (autumn) g.grade(1.07f, 0.96f, 0.82f);
        if (aurora) g.grade(0.34f, 0.52f, 0.88f);
        if (fire) g.grade(1.0f, 0.74f, 0.58f);
        if (isle == 14) g.grade(1.02f, 1.0f, 0.92f);
        if (isle == 16) g.grade(1.2f, 0.86f, 0.66f);
        if (isle == 18) g.grade(0.24f, 0.32f, 0.58f);
        if (dry == 20) g.grade(1.1f, 1.0f, 0.84f);
        if (dry == 22) g.grade(0.98f, 0.9f, 0.82f);
        if (dry == 24) g.grade(0.75f, 0.66f, 0.58f);
        g.time(w.time);

        g.backdrop(sky);
        if (aurora) {
            for (int k = 0; k < 3; k++) {                  // tre tende che ondeggiano e pulsano lentamente
                float tt = w.time * (0.12f + 0.05f * k) + k * 2.1f;
                Mat4.setIdentity(m);
                Mat4.translate(m, camX * 0.3f + 50f * (float) Math.sin(tt) + (k - 1) * 90f, 2f + k * 4f, camZ + 262f + k * 6f);
                Mat4.scale(m, 1f + 0.18f * (float) Math.sin(tt * 1.7f + k), 0.7f + 0.12f * (float) Math.sin(tt * 1.3f + 1.0f + k), 1f);
                float a = 0.5f + 0.3f * (float) Math.sin(w.time * 0.5f + k * 1.9f);
                g.draw(curtain, m, 1, 1, 1, a, 1);
            }
        }
        Mat4.setIdentity(m);
        Mat4.translate(m, camX * 0.3f, heaven ? -34f : 0f, camZ + (back ? -230f : 230f));
        if (aurora) Mat4.scale(m, 1f, 0.5f, 1f);          // montagne basse: lasciano vedere l'aurora tra gli alberi
        g.draw(mountains, m, 1, 1, 1, 1, aurora ? 0.35f : isle == 18 || dry == 24 ? 0.15f : 1);
        // (le montagne sono già ritoccate nella texture)

        if (!heaven) g.draw(pitFloor, ident, 1, 1, 1, 1, isle == 18 ? 0.1f : spring || jungle ? 0.45f : 1);
        if (spring || jungle) {
            Mat4.setIdentity(m);
            Mat4.translate(m, 0.18f * (float) Math.sin(w.time * 0.7f), -0.7f + 0.03f * (float) Math.sin(w.time * 1.9f), 0.25f * (float) Math.sin(w.time * 0.5f));
            g.draw(pitWater, m, 1, 1, 1, 0.5f, isle == 18 ? 0.2f : 0.7f);
        }
        float vz0 = back ? camZ - 105f : camZ - 10f, vz1 = back ? camZ + 14f : camZ + 105f;
        for (int c = 0; c < nChunks; c++) {
            float cz0 = zBase + c * CH, cz1 = cz0 + CH;
            if (cz1 < vz0 || cz0 > vz1) continue;
            boolean farChunk = back ? cz1 < camZ - 68f : cz0 > camZ + 68f;          // in lontananza erba, fiori e piccoli oggetti non si distinguono: non si disegnano
            for (int k = 0; k < K_N; k++) {
                if (farChunk && (k == K_TUFT || k == K_FERN || k == K_FLOWER || k == K_PROP || k == K_GLOW)) continue;
                if (k != K_RAY && k != K_TRAIL && land[k][c] != null) g.draw(land[k][c], ident, 1, 1, 1, 1, k == K_GLOW ? 0.6f : fire && k == K_DIRT ? 0.45f : 0);
            }
        }

        for (int c = 0; c < nChunks; c++) {
            float cz0 = zBase + c * CH;
            if (land[K_TRAIL][c] != null && cz0 + CH >= vz0 && cz0 <= vz1) g.draw(land[K_TRAIL][c], ident, 1, 1, 1, 1, 0);
        }

        float near = nearZ, far = farZ;      // gli oggetti tra la camera e il robot finirebbero enormi in primo piano
        for (int z = Math.max(0, (int) near); z < Math.min(w.length, (int) far); z++) {
            for (int x = 0; x < World3D.W; x++) {
                if (!w.crate[z * World3D.W + x]) continue;
                Mat4.setIdentity(m);
                Mat4.translate(m, x - World3D.HALF + 0.5f, bumpAt(x - World3D.HALF + 0.5f, z + 0.5f) - 0.03f, z + 0.5f);
                if (w.crateKind != null) { drawIslandCrate(w, g, z * World3D.W + x); continue; }
                g.draw(crate, m, 1, 1, 1, 1, fire ? 0.3f : spring || heaven ? 0.1f : autumn ? 0 : aurora ? 0.12f : 0.15f);
                if (crateStar != null) g.draw(crateStar, m, 1, 1, 1, 1, 0.92f);
            }
        }

        Mat4.setIdentity(m);
        Mat4.translate(m, -1.75f, 0, w.catZ + 0.6f);                 // piccola e defilata, ai bordi del percorso
        Mat4.rotateY(m, 0.25f);
        Mat4.scale(m, 0.62f, 0.62f, 0.62f);
        g.draw(catapult, m, 1, 1, 1, 1, 0);
        // traguardo
        // traguardo: il portone si apre quando il livello è completato
        float open = sm(w.gateOpen) * 1.9f, gz = w.flagZ + 0.5f;
        Mat4.setIdentity(m);
        Mat4.translate(m, 0, 0, gz);
        g.draw(gateStone, m, 1, 1, 1, 1, 0);
        g.draw(gateLight, m, 1, 1, 1, 1, 1);
        Mat4.setIdentity(m);
        Mat4.translate(m, -1.3f, 0, gz);
        Mat4.rotateY(m, -open);
        g.draw(gateDoor, m, 1, 1, 1, 1, 0);
        Mat4.setIdentity(m);
        Mat4.translate(m, 1.3f, 0, gz);
        Mat4.rotateY(m, (float) Math.PI + open);
        g.draw(gateDoor, m, 1, 1, 1, 1, 0);
        if (fire && open > 0.05f) {                    // dietro al portone che si apre non c'è altro che fuoco: la fine della vita del robot
            float up = Math.min(1f, open / 1.2f);
            for (int row = 0; row < 3; row++) {
                for (int i = -4; i <= 4; i++) {
                    float fl = 0.85f + 0.3f * (float) Math.sin(w.time * (7f + row * 2f + (i & 3)) + i * 1.7f + row);
                    Mat4.setIdentity(m);
                    Mat4.translate(m, i * 0.62f + 0.2f * row, 0f, gz + 0.25f + row * 0.7f);
                    Mat4.rotateY(m, w.time * 0.5f + i + row);
                    Mat4.scale(m, 1.5f * up, (3.2f + 0.9f * row) * fl * up, 1.5f * up);
                    g.draw(flame, m, 1f, 1f, 1f, 0.95f, 1f);
                }
            }
        }

        for (float[] c : w.gemsList) {
            if (c[2] < near || c[2] > far) continue;
            Mat4.setIdentity(m);
            Mat4.translate(m, c[0], c[1] + bumpAt(c[0], c[2]) + 0.06f * (float) Math.sin(w.time * 3f + c[2]), c[2]);
            Mat4.rotateY(m, w.time * 2.4f + c[2]);
            g.draw(gem, m, 1, 1, 1, 1, 0.45f);
        }
        for (float[] c : w.pops) {
            Mat4.setIdentity(m);
            Mat4.translate(m, c[0], c[1], c[2]);
            Mat4.rotateY(m, c[3] * 14f);
            Mat4.scale(m, 1.3f, 1.3f, 1.3f);
            g.draw(gem, m, 1, 1, 1, 1, 0.7f);
        }
        if (w.battery != null) {
            Mat4.setIdentity(m);
            Mat4.translate(m, w.battery[0], w.battery[1] + 0.07f * (float) Math.sin(w.time * 3f) - 0.05f, w.battery[2]);
            Mat4.rotateY(m, w.time * 1.8f);
            Mat4.scale(m, 1.45f, 1.45f, 1.45f);
            g.draw(battery, m, 1, 1, 1, 1, 0.1f);
            g.draw(batteryGlow, m, 1, 1, 1, 1, 0.9f);
        }

        for (Enemy3 e : w.enemies) {
            if (e.dead || e.z < near || e.z > far) continue;
            Mat4.setIdentity(m);
            Mat4.translate(m, e.x, e.y + bumpAt(e.x, e.z), e.z);
            float yawE = e.alongZ ? (e.dir > 0 ? 0 : (float) Math.PI) : (e.dir > 0 ? (float) Math.PI / 2 : -(float) Math.PI / 2);
            if (e.type == Enemy3.OWL) yawE = e.yaw;
            if (e.type == Enemy3.PLANT || (e.type == Enemy3.FROG && e.skin == 0)) yawE = e.yaw;   // trappola e rana guardano il robot
            if (e.type == Enemy3.SNOWMAN && e.skin == 0) yawE = e.face == 0 ? (float) Math.PI : e.face * (float) Math.PI / 2;
            Mat4.rotateY(m, yawE);
            if (e.type == Enemy3.OWL && !e.flung) { Mat4.translate(m, 0, 0.3f, 0); Mat4.rotateX(m, e.pitch); Mat4.rotateZ(m, e.bank); Mat4.translate(m, 0, -0.3f, 0); }
            if (e.flung) Mat4.rotateX(m, e.anim * 3f);
            if (e.flatTime > 0) Mat4.scale(m, 1.15f, 0.22f, 1.15f);
            foes.draw(g, m, e);
        }

        for (Enemy3 e : w.enemies) {                                   // le liane dei ricci restano anche dopo la caduta
            if (e.type != Enemy3.CHESTNUT || e.skin != 0 || e.z < near || e.z > far) continue;
            Mat4.setIdentity(m);
            Mat4.translate(m, 0, e.y0 + bumpAt(0, e.z), e.z);
            foes.drawLiana(g, m);
        }
        for (float[] sh : w.shots) { Mat4.setIdentity(m); foes.drawShot(g, m, sh); }
        drawRobot(w, g);
        drawLeaves(w, g);

        // ---- trasparenze: ombre, anelli, bandiera
        if (w.topAt(w.px, w.pz) > -500 && w.state != World3D.DYING) {       // niente ombra sopra il vuoto
            float sh = Math.max(0.35f, 1f - (w.py - w.groundY) * 0.14f);
            Mat4.setIdentity(m);
            Mat4.translate(m, w.px, w.groundY + bumpAt(w.px, w.pz) + 0.03f, w.pz);
            Mat4.scale(m, 1.25f * sh, 1, 1.25f * sh);
            g.draw(shadow, m, 1, 1, 1, 1, 1);
        }
        for (Enemy3 e : w.enemies) {
            if (e.dead || e.flung || e.z < near || e.z > far) continue;
            Mat4.setIdentity(m);
            float sy = e.flies() ? 0 : e.type == Enemy3.ACORN ? e.baseY : e.y;
            float ss = 1.5f * (e.type == Enemy3.ACORN || e.flies() ? Math.max(0.5f, 1f - (e.y - sy) * 0.2f) : 1f);
            Mat4.translate(m, e.x, sy + bumpAt(e.x, e.z) + 0.03f, e.z);
            Mat4.scale(m, ss, 1, ss);
            g.draw(shadow, m, 1, 1, 1, 0.8f, 1);
        }
        if (fire) { drawTreeFlames(w, g); drawFireHazards(w, g); }
        if (spring || heaven) drawSpringProps(w, g);
        if (jungle || dry != 0) drawIslandProps(w, g);
        if (dry != 0) drawDryProps(w, g);
        drawBooms(w, g);
        if (w.chase == 1) { drawHive(w, g); drawBees(w, g); }
        for (int c = 0; c < nChunks; c++) {
            float cz0 = zBase + c * CH;
            if (!back && land[K_RAY][c] != null && cz0 + CH > camZ - 6f && cz0 < camZ + 60f) g.draw(land[K_RAY][c], ident, 1, 1, 1, 0.15f, 1);
        }
        if (w.spinning()) {                             // dieci anelli tenui, stessa estensione di prima
            for (int i = 0; i < 10; i++) {
                float wob = (float) Math.sin(w.time * 45f + i * 1.3f);
                float k = (0.62f + 0.14f * (float) Math.sin(Math.PI * (i + 0.5f) / 10f)) * (1f + 0.07f * wob);
                Mat4.setIdentity(m);
                Mat4.translate(m, w.px, w.py + bumpAt(w.px, w.pz) + 0.2f + i * 0.1f, w.pz);
                Mat4.scale(m, k, 1, k);
                g.draw(ring, m, 1, 1, 1, 0.45f, 1);
            }
        }
        if (w.djTime > 0) {
            float f = w.djTime / 0.3f, k = 1.3f - 0.7f * f;
            Mat4.setIdentity(m);
            Mat4.translate(m, w.djX, w.djY, w.djZ);
            Mat4.scale(m, k, 1, k);
            g.draw(ring, m, 1, 1, 1, f * 0.9f, 1);
        }
        g.end();
    }

    private static final float[][] PETAL_TINT = {{1f, 0.78f, 0.88f}, {1f, 0.9f, 0.95f}, {0.98f, 0.66f, 0.8f}, {1f, 0.96f, 0.98f}};
    private static final float[][] GOLD_TINT = {{1f, 0.88f, 0.4f}, {1f, 0.97f, 0.75f}, {1f, 0.8f, 0.35f}, {1f, 1f, 0.9f}};
    private static final float[][] LEAF_TINT = {{1f, 0.55f, 0.16f}, {1f, 0.86f, 0.2f}, {0.6f, 0.88f, 0.22f}, {0.9f, 0.34f, 0.12f}, {0.98f, 0.7f, 0.12f}};

    /** Foglie che cadono dagli alberi, portate dal vento: si ripetono ogni 34 m attorno al giocatore. */
    private void drawLeaves(World3D w, Gfx3D g) {
        if (fire) { drawEmbers(w, g); return; }
        if (isle == 18) { drawFireflies(w, g); return; }
        if (dry == 20) { drawDust(w, g); return; }
        if (dry != 0) return;
        final int n = aurora ? 260 : heaven ? 120 : spring ? 150 : autumn ? 190 : 90;
        final float span = 34f;
        for (int i = 0; i < n; i++) {
            float hx = hash(i, 1, 101), hy = hash(i, 2, 102), hz = hash(i, 3, 103), hs = hash(i, 4, 104), hv = hash(i, 5, 105);
            float fall = 0.7f + 0.9f * hv, cyc = 9f / fall;
            float t = (w.time + hy * cyc * 7f) / cyc;
            t -= (float) Math.floor(t);
            float zr = hz * span;
            float z = zr + span * (float) Math.floor((camZ - 4f - zr) / span + 1f);
            float x = (hx * 2f - 1f) * 9f + 2.6f * t + 0.7f * (float) Math.sin(w.time * 1.1f + hs * 6.283f);
            float y = groundAt(x, z) + 0.12f + 9f * (1f - t);
            Mat4.setIdentity(m);
            Mat4.translate(m, x, y, z);
            Mat4.rotateY(m, w.time * (1.2f + hs) + hs * 6.283f);
            Mat4.rotateX(m, 1.0f * (float) Math.sin(w.time * 2.4f + hx * 6f) + 1.2f);
            Mat4.rotateZ(m, w.time * 1.7f * hv + hz * 6.283f);
            if (aurora) {                                   // fiocchi di neve: piccoli, lenti, bianchi
                float fz = 0.07f + 0.08f * hs;
                Mat4.scale(m, fz, fz, fz);
                g.draw(flake, m, 0.96f, 0.98f, 1f, 0.9f, 0.7f);
                continue;
            }
            float sz = (spring ? 0.1f : 0.15f) + (spring ? 0.07f : 0.11f) * hs;
            Mat4.scale(m, sz, sz, sz);
            float[] c = jungle ? JUNGLE_TINT[(int) (hv * 4.999f)] : heaven ? GOLD_TINT[(int) (hv * 3.999f)] : spring ? PETAL_TINT[(int) (hv * 3.999f)] : LEAF_TINT[(int) (hv * 4.999f)];
            g.draw(leaf, m, c[0], c[1], c[2], 1, 0.15f);
        }
    }

    /** Lucciole sul fiume di notte: puntini che si accendono e si spengono, vagando lenti attorno al giocatore. */
    private void drawFireflies(World3D w, Gfx3D g) {
        final float span = 30f;
        for (int i = 0; i < 90; i++) {
            float hx = hash(i, 1, 301), hy = hash(i, 2, 302), hz = hash(i, 3, 303), hs = hash(i, 4, 304);
            float zr = hz * span, z = zr + span * (float) Math.floor(((back ? camZ - 26f : camZ - 4f) - zr) / span + 1f);
            float x = (hx * 2f - 1f) * 7f + 0.6f * (float) Math.sin(w.time * 0.7f + hs * 9f);
            float y = Math.max(0f, groundAt(Math.max(-2.4f, Math.min(2.4f, x)), z)) + 0.4f + 2.2f * hy + 0.3f * (float) Math.sin(w.time * 1.1f + hs * 7f);
            float on = Math.max(0f, (float) Math.sin(w.time * (1.2f + hs) + hs * 40f));
            if (on < 0.05f) continue;
            Mat4.setIdentity(m);
            Mat4.translate(m, x, y, z);
            float sz = 0.05f + 0.04f * on;
            Mat4.scale(m, sz, sz, sz);
            g.draw(boomBall, m, 0.8f, 1f, 0.4f, on, 1f);
        }
    }

    /** Braci che salgono dal bosco in fiamme, portate dal vento: si ripetono ogni 34 m attorno al giocatore. */
    private void drawEmbers(World3D w, Gfx3D g) {
        final float span = 34f;
        for (int i = 0; i < 150; i++) {
            float hx = hash(i, 1, 201), hy = hash(i, 2, 202), hz = hash(i, 3, 203), hs = hash(i, 4, 204), hv = hash(i, 5, 205);
            float rise = 0.8f + 0.9f * hv, cyc = 9f / rise;
            float t = (w.time + hy * cyc * 7f) / cyc;
            t -= (float) Math.floor(t);
            float zr = hz * span;
            float z = zr + span * (float) Math.floor((camZ - 4f - zr) / span + 1f);
            float x = (hx * 2f - 1f) * 9f + 1.8f * t + 0.8f * (float) Math.sin(w.time * 1.3f + hs * 6.283f);
            float y = groundAt(x, z) + 0.2f + 9f * t;
            Mat4.setIdentity(m);
            Mat4.translate(m, x, y, z);
            Mat4.rotateY(m, w.time * 0.9f + hs * 6.283f);
            float sz = (0.10f + 0.12f * hs) * (1f - 0.6f * t);
            Mat4.scale(m, sz, sz, sz);
            g.draw(ember, m, 1f, 0.75f + 0.2f * hs, 0.4f, 1f, 1f);
        }
    }

    /** Muro di fuoco alle spalle, getti di fiamma a tempo e tronchi in fiamme che rotolano. */
    /** Fiori a molla (si schiacciano e rimbalzano quando ci si salta sopra) e rovi. */
    private void drawSpringProps(World3D w, Gfx3D g) {
        float near = w.pz - 8, far = w.pz + 70;
        for (int i = 0; i < w.springs.size(); i++) {
            float[] sp = w.springs.get(i);
            if (sp[1] < near || sp[1] > far) continue;
            float gy = groundAt(sp[0], sp[1]);
            float k = i == w.springIdx ? Math.max(0f, 1f - (w.time - w.springAt) / 0.35f) : 0f;       // appena lanciato: si schiaccia
            float sq = 1f - 0.45f * (float) Math.sin(Math.PI * (1f - k)) * (k > 0 ? 1 : 0);
            float bob = 1f + 0.05f * (float) Math.sin(w.time * 3f + sp[1]);
            Mat4.setIdentity(m);
            Mat4.translate(m, sp[0], gy, sp[1]);
            Mat4.scale(m, 1f, sq * bob, 1f);
            g.draw(padDome, m, 1, 1, 1, 1, 0.12f);
            Mat4.setIdentity(m);
            Mat4.translate(m, sp[0], gy + 0.25f * sq * bob + 0.02f, sp[1]);
            Mat4.rotateY(m, w.time * 0.5f + sp[1]);
            Mat4.scale(m, 1f + 0.08f * (float) Math.sin(w.time * 4f + sp[1]), 1, 1f + 0.08f * (float) Math.sin(w.time * 4f + sp[1]));
            g.draw(padTop, m, 1, 1, 1, 1, 0.45f);
        }
        for (float[] t : w.thorns) {
            if (t[1] < near || t[1] > far) continue;
            Mat4.setIdentity(m);
            Mat4.translate(m, t[0], groundAt(t[0], t[1]) - 0.02f, t[1]);
            Mat4.rotateY(m, t[0] * 5f + t[1] * 3f);
            Mat4.scale(m, 1.15f, 1.15f, 1.15f);
            g.draw(thornMesh, m, 1, 1, 1, 1, 0f);
        }
    }

    /** Una singola ape alla posizione (x, y, z), con il muso verso (dx, dz) e le ali che battono. */
    private void drawBee(float x, float y, float z, float dx, float dz, float t, float size) {
        float yaw = (float) Math.atan2(dx, dz);
        Mat4.setIdentity(m);
        Mat4.translate(m, x, y, z);
        Mat4.rotateY(m, yaw);
        Mat4.rotateX(m, 1.25f);                                  // l'asse dell'ape passa da verticale a orizzontale, muso in avanti
        Mat4.scale(m, size, size, size);
        gfxBee.draw(beeBody, m, 1f, 1f, 1f, 1f, 0.15f);
        float flap = 0.35f + 0.65f * Math.abs((float) Math.sin(t * 38f));
        Mat4.scale(m, flap, 1f, 1f);
        gfxBee.draw(beeWings, m, 1f, 1f, 1f, 0.42f, 0.6f);
    }

    private Gfx3D gfxBee;

    /** Il ciliegio con il nido d'api: il nido pende sul sentiero finché il robot non lo supera, poi cade e si schianta a terra. */
    private void drawHive(World3D w, Gfx3D g) {
        if (Float.isNaN(w.hiveZ) || Math.abs(w.hiveZ - w.pz) > 70f) return;
        float z = w.hiveZ, gy = groundAt(0, z), gyT = groundAt(3.4f, z);
        Mat4.setIdentity(m);
        Mat4.translate(m, 3.4f, gyT - 0.1f, z);
        g.draw(hiveTree, m, 1, 1, 1, 1, 0);
        Mat4.setIdentity(m);
        Mat4.translate(m, 3.3f, gy + 5.8f, z);
        Mat4.rotateZ(m, (float) Math.PI / 2);
        g.draw(hiveBranch, m, 1, 1, 1, 1, 0);
        float hang = gy + 4.5f, y = hang, sx = 1f, sy = 1f, tilt = 0.06f * (float) Math.sin(w.time * 2f);
        boolean released = w.hiveAt >= 0;
        if (released) {
            float t = w.time - w.hiveAt, land = gy + 0.62f;
            y = Math.max(land, hang - 15f * t * t);
            if (y <= land) {                                       // schianto: si schiaccia e resta inclinato
                float k = Math.min(1f, (t - (float) Math.sqrt((hang - land) / 15f)) * 6f);
                sx = 1f + 0.18f * k; sy = 1f - 0.25f * k; tilt = 0.4f * k;
            } else tilt = 0.5f * (float) Math.sin(t * 9f);
        } else {
            Mat4.setIdentity(m);
            Mat4.translate(m, 0, hang + 0.95f, z);
            Mat4.scale(m, 1f, 1.2f, 1f);
            g.draw(hiveRope, m, 1, 1, 1, 1, 0);
        }
        Mat4.setIdentity(m);
        Mat4.translate(m, 0, y, z);
        Mat4.rotateZ(m, tilt);
        Mat4.scale(m, sx, sy, sx);
        g.draw(hiveMesh, m, 1, 1, 1, 1, 0);
        if (!released) {                                           // le api attorno al nido, ancora nervose
            gfxBee = g;
            for (int i = 0; i < 9; i++) {
                float t = w.time * (3f + 0.5f * i) + i * 2.1f, r = 0.9f + 0.1f * i;
                drawBee(r * (float) Math.cos(t), hang + 0.2f * (i % 4) + 0.2f * (float) Math.sin(t * 1.3f), z + r * 0.8f * (float) Math.sin(t),
                        -(float) Math.sin(t), (float) Math.cos(t) * 0.8f, w.time + i, 0.8f);
            }
        }
    }

    /** Lo sciame che insegue: una nuvola di api che vorticano attorno a fireZ, con le più coraggiose che arrivano fin quasi al robot. */
    private void drawBees(World3D w, Gfx3D g) {
        if (Float.isNaN(w.fireZ) || w.fireZ < camZ - 14f) return;
        gfxBee = g;
        float camPos = camZ - 6f, span = Math.max(1f, Math.min(14f, (w.pz - w.fireZ) - 2.4f));
        for (int i = 0; i < 150; i++) {
            float h1 = hash(i, 3, 11), h2 = hash(i, 7, 13), h3 = hash(i, 5, 17), h4 = hash(i, 9, 19);
            boolean scout = i >= 85;
            float depth = scout ? (float) Math.pow(h3, 1.7f) * span : -2.4f + h3 * 4.2f;        // massa compatta o api sparse davanti
            float spread = scout ? 2.3f : 4.2f;
            float bx = (h1 - 0.5f) * 2f * spread, by = 0.5f + h2 * (scout ? 2.6f : 3.6f);
            float rad = 0.35f + h4 * 0.6f, om = 2.5f + h4 * 3.5f, ph = i * 1.7f;
            float t = w.time * om + ph;
            float x = bx + rad * (float) Math.cos(t), z = w.fireZ + 1.2f + depth + rad * 0.8f * (float) Math.sin(t);
            float y = by + 0.3f * (float) Math.sin(t * 1.3f + ph);
            if (z < camPos + 0.8f) continue;                      // dietro la camera
            float gy = groundAt(Math.max(-2.4f, Math.min(2.4f, x)), Math.max(0f, z));
            drawBee(x, gy + y, z, -(float) Math.sin(t) * rad, (float) Math.cos(t) * rad * 0.8f, w.time + ph, scout ? 0.95f : 0.8f);
        }
    }

    private void drawFireHazards(World3D w, Gfx3D g) {
        float near = w.pz - 8, far = w.pz + 70;
        for (float[] l : w.logs) {
            if (l[0] < near || l[0] > far) continue;
            float gyp = groundAt(-2.45f, l[0]);
            for (int k = 0; k < 3; k++) {                  // pila di legna da cui esce il tronco
                Mat4.setIdentity(m);
                Mat4.translate(m, k < 2 ? -3.55f + k * 0.78f : -3.16f, gyp + (k < 2 ? 0.4f : 1.02f), l[0]);
                Mat4.rotateX(m, (float) Math.PI / 2);
                Mat4.rotateY(m, k * 1.7f);
                Mat4.scale(m, 0.95f, 1.0f + 0.1f * k, 0.95f);
                g.draw(logMesh, m, 0.8f, 0.7f, 0.62f, 1f, 0.15f);
            }
            float[] lp = World3D.logPose(l, w.time);
            if (lp[3] < 0.5f) continue;
            float gy = groundAt(Math.max(-2.4f, Math.min(2.4f, lp[0])), l[0]);
            float sc = lp[2];
            Mat4.setIdentity(m);
            Mat4.translate(m, lp[0], gy + 0.42f * sc + lp[1], l[0]);
            Mat4.rotateX(m, (float) Math.PI / 2);
            Mat4.rotateY(m, lp[0] * 2.4f);
            Mat4.scale(m, sc, 1, sc);
            g.draw(logMesh, m, 1f, 0.85f, 0.7f, 1f, 0.3f);
            if (lp[4] > 0.5f) for (int k = 0; k < 2; k++) {                  // fiamme sul dorso, solo finché rotola
                float fl = 0.8f + 0.35f * (float) Math.sin(w.time * (9f + k * 4f) + l[0] + k);
                Mat4.setIdentity(m);
                Mat4.translate(m, lp[0], gy + 0.7f * sc, l[0] + (k - 0.5f) * 0.55f);
                Mat4.rotateY(m, w.time * 0.7f + k * 1.5f);
                Mat4.scale(m, 0.95f * fl * sc, 1.25f * fl * sc, 0.95f * fl * sc);
                g.draw(flame, m, 1f, 1f, 1f, 0.92f, 1f);
            }
            if (lp[4] < 0.5f && lp[1] > -1.5f) {         // cade nella lava: fiammata e braci
                Mat4.setIdentity(m);
                Mat4.translate(m, lp[0], gy + 0.1f, l[0]);
                Mat4.rotateY(m, w.time * 2f);
                Mat4.scale(m, 1.3f, 1.0f + 1.6f * Math.min(1f, -lp[1]), 1.3f);
                g.draw(flame, m, 1f, 1f, 1f, 0.8f, 1f);
            }
        }
        for (float[] j : w.jets) {
            if (j[1] < near || j[1] > far) continue;
            float pw = World3D.jetPower(j, w.time), warn = World3D.jetWarn(j, w.time);
            Mat4.setIdentity(m);                            // la bocca si vede sempre, e si accende prima del getto
            Mat4.translate(m, j[0], groundAt(j[0], j[1]) + 0.04f, j[1]);
            float vs = 1f + 0.12f * warn + 0.05f * (float) Math.sin(w.time * 5f + j[1]);
            Mat4.scale(m, vs, 1, vs);
            g.draw(vent, m, 1f, 1f - 0.2f * warn, 1f - 0.5f * warn, 1f, 0.9f + 0.1f * Math.max(warn, pw));
            if (hgtBase(pw, warn) <= 0) {                   // spento: due fiammelle basse sempre accese e braci che salgono, così la bocca non passa inosservata
                for (int k = 0; k < 2; k++) {
                    float fl = 0.8f + 0.3f * (float) Math.sin(w.time * (8f + k * 5f) + j[1] * 3f + k);
                    Mat4.setIdentity(m);
                    Mat4.translate(m, j[0] + (k - 0.5f) * 0.3f, groundAt(j[0], j[1]) + 0.02f, j[1] + (0.5f - k) * 0.2f);
                    Mat4.rotateY(m, k * 1.3f + w.time * 0.4f);
                    Mat4.scale(m, 0.5f, 0.28f * fl, 0.5f);
                    g.draw(flame, m, 1f, 1f, 1f, 0.75f, 1f);
                }
                float ph = (w.time * 0.9f + j[1] * 0.37f) % 1f;
                Mat4.setIdentity(m);
                Mat4.translate(m, j[0] + 0.25f * (float) Math.sin(w.time * 3f + j[1]), groundAt(j[0], j[1]) + 0.2f + ph * 1.1f, j[1]);
                Mat4.scale(m, 0.1f, 0.1f, 0.1f);
                g.draw(ember, m, 1f, 0.8f, 0.4f, 1f - ph, 1f);
            }
            float hgt = pw > 0 ? 2.5f * (0.45f + 0.55f * pw) : warn > 0 ? 0.25f + 0.45f * warn : 0f;
            if (hgt <= 0) continue;
            float gy = groundAt(j[0], j[1]);
            for (int k = 0; k < 2; k++) {
                Mat4.setIdentity(m);
                Mat4.translate(m, j[0], gy, j[1]);
                Mat4.rotateY(m, k * 0.9f + w.time * 0.5f);
                Mat4.scale(m, 1.2f * (pw > 0 ? 1f : 0.6f), hgt, 1.2f * (pw > 0 ? 1f : 0.6f));
                g.draw(flame, m, 1f, 1f, 1f, pw > 0 ? 0.95f : 0.45f + 0.45f * warn, 1f);
            }
        }
        if (!Float.isNaN(w.fireZ) && w.fireZ > camZ - 12f) {
            for (int i = -4; i <= 4; i++) {
                float fl = 0.85f + 0.3f * (float) Math.sin(w.time * (6f + i * 0.7f) + i * 1.9f);
                Mat4.setIdentity(m);
                Mat4.translate(m, i * 1.3f, groundAt(Math.max(-2.4f, Math.min(2.4f, i * 1.3f)), Math.max(0f, w.fireZ)) - 0.3f, w.fireZ + 0.3f * (i % 2));
                Mat4.scale(m, 3.2f, 6.5f * fl, 3.2f);
                g.draw(flame, m, 1f, 1f, 1f, 0.95f, 1f);
            }
        }
    }

    // ------------------------------------------------------------------ isola tropicale

    private Mesh[] crateKinds;
    private Mesh boomBall, boomRing, crusherBlock, crusherChain, raftMesh, boulderMesh;

    private void buildBoom() {
        if (boomBall != null) return;
        MeshBuilder b = new MeshBuilder().color(255, 190, 70);
        b.ellipsoid(0, 0, 0, 1, 1, 1, 18, 12);
        boomBall = b.build(-1, true, false);
        b = new MeshBuilder().color(255, 240, 200);
        b.cylinder(0, -0.04f, 0.04f, 0, 1f, 0.82f, 24, true, false);
        boomRing = b.build(-1, true, false);
    }

    private void buildIsland() {
        crateKinds = new Mesh[5];
        int[] tex = {Img.R3J_CRATE, Img.R3J_TNT, Img.R3J_NITRO, Img.R3J_BOUNCE, Img.R3J_CHECK};
        for (int k = 0; k < 5; k++) {
            MeshBuilder b = new MeshBuilder();
            b.roundBox(0, 0.5f, 0, 0.94f, 0.94f, 0.94f, 0.07f, 63, 3);
            crateKinds[k] = b.build(tex[k], false, true);
            crateKinds[k].spec = 0.15f; crateKinds[k].shine = 18f;
        }
        buildBoom();
        MeshBuilder b = new MeshBuilder();                                          // pestello: blocco di pietra scolpita con la faccia di un idolo
        b.roundBox(0, 0.8f, 0, 0.98f, 1.6f, 0.98f, 0.08f);
        crusherBlock = b.build(dry == 22 ? Img.R3F_BLOCK : Img.R3J_TEMPLE, false, true);
        crusherBlock.spec = 0.1f; crusherBlock.shine = 12f;
        b = new MeshBuilder().color(70, 66, 60);
        b.cylinder(0, 0, 9f, 0, 0.06f, 0.06f, 6, false, false);
        crusherChain = b.build(-1, false, true);
        b = new MeshBuilder();                                          // zattera: quattro tronchi legati
        for (int i = 0; i < 4; i++) {
            float x = -0.36f + i * 0.24f;
            b.colorf(0.62f + 0.06f * (i % 2), 0.44f, 0.26f);
            FoeModels.tube(b, new float[][] {{x, 0.08f, -0.46f}, {x, 0.1f, 0f}, {x, 0.08f, 0.46f}}, new float[] {0.12f, 0.13f, 0.12f}, 8, FoeModels.flat(new float[] {0.62f + 0.06f * (i % 2), 0.44f, 0.26f}));
        }
        b.colorf(0.86f, 0.78f, 0.5f);
        b.box(0, 0.2f, -0.28f, 0.9f, 0.05f, 0.08f);
        b.box(0, 0.2f, 0.28f, 0.9f, 0.05f, 0.08f);
        raftMesh = b.build(-1, false, true);
        raftMesh.spec = 0.05f; raftMesh.shine = 8f;
        b = new MeshBuilder();                                          // il masso del tempio: pietra sbozzata, bitorzoluta, con muschio nelle conche
        b.ellipsoid(0, 0, 0, 1, 1, 1, 44, 30);
        b.rough(0, 0.13f, 1.4f, 31);
        b.rough(0, 0.035f, 4.5f, 32);
        b.shadeByRelief(0.82f, 1.12f);
        boulderMesh = b.build(Img.R3_ROCK, false, true);
        boulderMesh.spec = 0.08f; boulderMesh.shine = 10f;
    }

    /** Palma: tronco ad arco a anelli e una corona di fronde che ricadono, fatte di foglioline. */
    private void palm(float x, float y, float z, float h, Random r) {
        MeshBuilder tb = mb(K_TRUNK, z), fb = mb(K_BLOB, z);
        float lean = (x > 0 ? 1 : -1) * (0.4f + 0.5f * r.nextFloat()), rz = (r.nextFloat() - 0.5f) * 0.8f;
        int n = 7;
        float[][] c = new float[n][];
        float[] rad = new float[n];
        for (int i = 0; i < n; i++) {
            float t = i / (float) (n - 1);
            c[i] = new float[] {x + lean * t * t * h * 0.35f, y + t * h, z + rz * t * t};
            rad[i] = 0.16f - 0.06f * t;
        }
        lightAt(x, z, 1f);
        float[] bark = {lt[0] * 1.15f, lt[1] * 1.0f, lt[2] * 0.8f};
        FoeModels.tube(tb, c, rad, 7, FoeModels.flat(bark));
        float[] top = c[n - 1];
        int fronds = 7 + r.nextInt(3);
        float rot = r.nextFloat() * 6.283f;
        for (int k = 0; k < fronds; k++) {
            float az = rot + k * 6.283f / fronds, ca = (float) Math.cos(az), sa = (float) Math.sin(az);
            float len = 1.6f + 0.6f * r.nextFloat(), up = 0.5f + 0.4f * r.nextFloat();
            int seg = 8;
            float tone = 0.8f + 0.35f * r.nextFloat();
            for (int i = 0; i < seg; i++) {
                float t0 = i / (float) seg, t1 = (i + 1) / (float) seg;
                float x0 = top[0] + ca * len * t0, z0 = top[2] + sa * len * t0, y0 = top[1] + up * t0 - 1.5f * t0 * t0 * len * 0.5f;
                float x1 = top[0] + ca * len * t1, z1 = top[2] + sa * len * t1, y1 = top[1] + up * t1 - 1.5f * t1 * t1 * len * 0.5f;
                float wl = 0.5f * (1 - t0 * 0.7f);
                for (int sgn = -1; sgn <= 1; sgn += 2) {                 // fogliolina pendente da ogni lato del rachide
                    float px2 = (x0 + x1) / 2 - sa * sgn * wl, pz2 = (z0 + z1) / 2 + ca * sgn * wl, py2 = (y0 + y1) / 2 - wl * 0.7f;
                    fb.colorf(0.32f * tone * lt[0] * 2.4f, 0.62f * tone * lt[1] * 2.0f, 0.24f * tone * lt[2] * 2.2f);
                    fb.sway(0.25f + 0.7f * t0);
                    fb.vert(x0, y0, z0, 0, 1, 0, 0.1f, 0.1f);
                    fb.vert(x1, y1, z1, 0, 1, 0, 0.9f, 0.1f);
                    fb.vert(px2, py2, pz2, 0, 1, 0, 0.5f, 0.9f);
                }
            }
        }
        fb.sway(0);
    }

    private void drawIslandCrate(World3D w, Gfx3D g, int k) {
        int kind = w.crateKind[k];
        float t = w.crateT[k];
        if (kind == World3D.C_NITRO) {                                  // l'instabile saltella e trema da sola
            float j = (float) Math.max(0, Math.sin(w.time * 7f + k * 1.3f));
            Mat4.translate(m, 0, 0.08f * j * j * j, 0);
            Mat4.rotateY(m, 0.08f * (float) Math.sin(w.time * 31f + k));
            g.draw(crateKinds[kind], m, 1, 1, 1, 1, 0.35f + 0.25f * (float) Math.sin(w.time * 9f));
        } else if (kind == World3D.C_TNT && t > 0) {                    // accesa: lampeggia sempre più in fretta e si gonfia
            float rate = 4f + 10f * t / World3D.TNT_FUSE, fl = (float) Math.sin(t * rate * 6.283f) > 0 ? 1f : 0f, sw = 1f + 0.12f * t / World3D.TNT_FUSE;
            Mat4.scale(m, sw, sw, sw);
            g.draw(crateKinds[kind], m, 1, 1, 1, 1, 0.15f + 0.6f * fl);
        } else if (kind == World3D.C_BOUNCE && t > 0 && w.time - t < 0.3f) {
            float u = (w.time - t) / 0.3f, sq = 1f - 0.3f * (float) Math.sin(Math.PI * u);
            Mat4.scale(m, 1f + (1f - sq) * 0.5f, sq, 1f + (1f - sq) * 0.5f);
            g.draw(crateKinds[kind], m, 1, 1, 1, 1, 0.1f);
        } else {
            g.draw(crateKinds[kind], m, 1, 1, 1, 1, kind == World3D.C_CHECK ? 0.25f : 0.08f);
        }
    }

    private void drawIslandProps(World3D w, Gfx3D g) {
        for (float[] c : w.crushers) {
            float z = c[1] + 0.5f;
            if (z < nearZ || z > farZ + (back ? 0 : 4)) continue;
            float floor = Math.max(0, w.height[(int) c[1] * World3D.W + (int) c[0]]), bot = floor + World3D.crusherBottom(c, w.time), warn = World3D.crusherWarn(c, w.time);
            float x = c[0] - World3D.HALF + 0.5f + 0.03f * warn * (float) Math.sin(w.time * 60f);
            Mat4.setIdentity(m);
            Mat4.translate(m, x, bot, z);
            g.draw(crusherBlock, m, 1, 1, 1, 1, 0.05f + 0.25f * warn);
            Mat4.setIdentity(m);
            Mat4.translate(m, x, bot + 1.6f, z);
            g.draw(crusherChain, m, 1, 1, 1, 1, 0);
            Mat4.setIdentity(m);                                         // ombra che si stringe mentre il blocco scende
            Mat4.translate(m, x, groundAt(x, z) + 0.03f, z);
            float sc = 1.6f - 0.25f * Math.min(1f, (bot - floor) / 3.1f);
            Mat4.scale(m, sc, 1, sc);
            g.draw(shadow, m, 1, 1, 1, 0.9f, 1);
        }
        for (float[] r : w.rafts) {
            float z = r[1] + 0.5f;
            if (z < nearZ || z > farZ + 4) continue;
            float sink = World3D.raftSink(r, w.time);
            if (sink >= 0.999f) continue;
            Mat4.setIdentity(m);
            Mat4.translate(m, r[0] - World3D.HALF + 0.5f, -0.18f - 0.9f * sink + 0.04f * (float) Math.sin(w.time * 2f + z), z);
            Mat4.rotateZ(m, 0.04f * (float) Math.sin(w.time * 1.6f + z * 0.7f) + (r[2] >= 0 ? 0.1f * (float) Math.sin(w.time * 30f) * (1 - sink) : 0));
            g.draw(raftMesh, m, 1, 1, 1, 1, isle == 18 ? 0.25f : 0);
        }
        if (w.chase == 2) drawBoulder(w, g);
    }

    /** Esplosioni (casse TNT, ricci di castagno che cadono). */
    private void drawBooms(World3D w, Gfx3D g) {
        if (boomBall == null) return;
        for (float[] bm : w.booms) {                                    // esplosione: palla di fuoco che si gonfia e svanisce, anello d'urto a terra
            float u = bm[2] / 0.7f, r = (0.4f + 1.9f * (float) Math.sqrt(u)) * (bm.length > 4 && bm[4] == 1 ? 0.72f : 1f);   // il riccio scoppia più piccolo
            Mat4.setIdentity(m);
            Mat4.translate(m, bm[0], (bm.length > 3 ? bm[3] : 0f) + 0.6f, bm[1]);
            Mat4.scale(m, r, r * 0.85f, r);
            g.draw(boomBall, m, 1, 1f - 0.5f * u, 0.6f - 0.5f * u, 1f - u, 1);
            Mat4.setIdentity(m);
            Mat4.translate(m, bm[0], (bm.length > 3 ? bm[3] : 0f) + 0.08f, bm[1]);
            Mat4.scale(m, r * 1.3f, 1, r * 1.3f);
            g.draw(boomRing, m, 1, 1, 1, 0.7f * (1f - u), 1);
        }
    }

    /** Il masso del tempio: rotola alle spalle del robot (che lo vede arrivare, con la camera girata). */
    private void drawBoulder(World3D w, Gfx3D g) {
        if (Float.isNaN(w.fireZ) || w.fireZ < w.pz - 40f) return;
        float rad = 2.3f, z = w.fireZ - rad * 0.3f;
        Mat4.setIdentity(m);
        Mat4.translate(m, 0, Math.max(0f, groundAt(0, Math.max(0f, z))) + rad - 0.15f, z);
        Mat4.rotateX(m, w.fireZ / rad);
        Mat4.scale(m, rad, rad, rad);
        g.draw(boulderMesh, m, 1, 1, 1, 1, 0.05f);
        Mat4.setIdentity(m);
        Mat4.translate(m, 0, Math.max(0f, groundAt(0, Math.max(0f, z))) + 0.05f, z);
        Mat4.scale(m, rad * 2.2f, 1, rad * 1.6f);
        g.draw(shadow, m, 1, 1, 1, 1, 1);
    }

    private static float hgtBase(float pw, float warn) { return pw > 0 ? 1f : warn > 0 ? 0.5f : 0f; }

    /** Fiamme sulle chiome degli alberi più vicini al sentiero. */
    private void drawTreeFlames(World3D w, Gfx3D g) {
        for (int i = 0; i < trees.length; i++) {
            Tree t = trees[i];
            if (t.row > 1 || t.z < w.pz - 4 || t.z > w.pz + 70 || (i * 7 + (int) t.z) % 3 != 0) continue;
            for (int k = 0; k < 2; k++) {
                float fl = 0.85f + 0.3f * (float) Math.sin(w.time * (7f + k * 3f) + i * 1.7f + k * 2f);
                float h = t.h * (0.42f + 0.18f * k) * fl;
                Mat4.setIdentity(m);
                Mat4.translate(m, t.x + (k - 0.5f) * 0.5f, t.y + t.h * (0.35f + 0.2f * k), t.z + (0.5f - k) * 0.4f);
                Mat4.rotateY(m, i * 0.9f + k * 1.2f + 0.15f * (float) Math.sin(w.time * 2f + i));
                Mat4.scale(m, h * 0.62f, h, h * 0.62f);
                g.draw(flame, m, 1f, 1f, 1f, 0.95f, 1f);
            }
        }
    }

    private void drawRobot(World3D w, Gfx3D g) {
        boolean blink = w.state == World3D.PLAYING && w.invuln > 0 && !w.spinning() && ((int) (w.time * 14f) % 2 == 0);
        if (blink) return;
        float[] b = m;
        Mat4.setIdentity(b);
        Mat4.translate(b, w.px, w.py + bumpAt(w.px, w.pz) - (w.onGround && w.quickAt(w.px, w.pz) ? 0.08f + 0.3f * w.sinkT : 0f), w.pz);
        Mat4.rotateY(b, w.yaw + (w.spinning() ? w.spinAngle : 0));
        if (w.state == World3D.DYING) Mat4.rotateX(b, -w.stateTime * 5f);
        if (cartMesh != null && w.inCart() && w.state == World3D.PLAYING) {      // il carrello sotto il robot, che dondola sui binari
            System.arraycopy(b, 0, a2, 0, 16);
            Mat4.rotateZ(a2, 0.05f * (float) Math.sin(w.time * 9f));
            g.draw(cartMesh, a2, 1, 1, 1, 1, 0);
        }
        hero.draw(g, b, w);
    }

    private final float[] a2 = new float[16];

    // ------------------------------------------------------------------ deserto, fabbrica, miniera

    private Mesh cartMesh, beltBase, chevron, laserBeam, laserPost, quickPool, cactus, dustMote;

    /** Contorno dei mondi senza bosco: cactus e massi nel deserto, tubi e serbatoi in fabbrica, travi e lanterne in miniera, binari. */
    private void buildDry(World3D w, int z0, int z1) {
        Random r = new Random(4242 + dry);
        for (int side = -1; side <= 1; side += 2) {
            for (float z = z0; z < z1; z += 1.5f + 2.5f * r.nextFloat()) {
                float d = 0.6f + 9f * r.nextFloat() * r.nextFloat(), x = side * (World3D.HALF + d), y = groundAt(x, z);
                lightAt(x, z, 1f);
                if (dry == 20) {
                    if (r.nextFloat() < 0.45f) cactusAt(mb(K_PROP, z), x, y, z, 0.9f + 1.4f * r.nextFloat(), r);
                    else mb(K_ROCK, z).rock(x, y, z, 0.3f + 0.6f * r.nextFloat(), 0.25f + 0.4f * r.nextFloat(), 0.3f + 0.6f * r.nextFloat(), r.nextInt(999), 9, 6);
                } else if (dry == 22) {
                    MeshBuilder p = mb(K_PROP, z);
                    float k = r.nextFloat();
                    if (k < 0.35f) {                                    // serbatoio
                        p.colorf(0.46f * lt[0], 0.5f * lt[1], 0.55f * lt[2]);
                        float rr = 0.5f + 0.5f * r.nextFloat();
                        p.cylinder(x + side * rr, y, y + 1.5f + 2f * r.nextFloat(), z, rr, rr, 14, true, false);
                    } else if (k < 0.6f) {                              // cassone con le strisce
                        { MeshBuilder ms = mb(K_STONE, z); int mk = ms.mark(); ms.roundBox(x + side * 0.5f, y + 0.5f, z, 1f, 1f, 1f, 0.12f, 63, 2); ms.rough(mk, 0.17f, 1.3f, 5); }
                    } else {                                             // ciminiera
                        p.colorf(0.5f * lt[0], 0.3f * lt[1], 0.26f * lt[2]);
                        p.cylinder(x + side * 2f, y, y + 4f + 4f * r.nextFloat(), z, 0.4f, 0.32f, 10, true, false);
                    }
                } else {
                    if (r.nextFloat() < 0.5f) mb(K_ROCK, z).rock(x, y, z, 0.4f + 0.5f * r.nextFloat(), 0.4f + 0.8f * r.nextFloat(), 0.4f + 0.5f * r.nextFloat(), r.nextInt(999), 9, 6);
                    else {                                              // cristallo che brilla nella roccia
                        MeshBuilder p = mb(K_GLOW, z);
                        p.colorf(0.45f, 0.8f, 1.0f);
                        p.bipyramid(x, y + 0.3f, z, 0.12f + 0.12f * r.nextFloat(), 0.35f + 0.3f * r.nextFloat(), 6);
                    }
                }
            }
        }
        if (dry == 22) {                                                // tubature lungo i due lati, all'altezza della testa
            for (int side = -1; side <= 1; side += 2) for (int z = z0; z < z1; z += 4) {
                MeshBuilder p = mb(K_PROP, z);
                float x = side * (World3D.HALF + 1.6f), y = groundAt(x, z) + 1.6f;
                lightAt(x, z, 1f);
                p.colorf(0.7f * lt[0] * 1.8f, 0.42f * lt[1] * 1.8f, 0.2f * lt[2] * 1.8f);
                FoeModels.tube(p, new float[][] {{x, y, z}, {x, y, z + 4.02f}}, new float[] {0.16f, 0.16f}, 8, FoeModels.flat(new float[] {0.75f, 0.45f, 0.22f}));
                p.colorf(0.3f, 0.3f, 0.32f);
                p.cylinder(x, y - 0.22f, y + 0.22f, z, 0.21f, 0.21f, 8, false, false);
            }
        }
        if (dry == 24) {                                                // armature di legno della galleria, con una lanterna appesa
            for (int z = z0 + 2; z < z1; z += 7) {
                MeshBuilder p = mb(K_PROP, z);
                lightAt(0, z, 1f);
                p.colorf(0.55f * lt[0], 0.38f * lt[1], 0.22f * lt[2]);
                for (int side = -1; side <= 1; side += 2) p.box(side * (World3D.HALF + 0.35f), groundAt(side * (World3D.HALF + 0.35f), z) + 2.6f, z, 0.3f, 5.2f, 0.3f);
                p.box(0, 5.3f, z, World3D.W + 1.2f, 0.32f, 0.34f);
                MeshBuilder gl = mb(K_GLOW, z);
                gl.colorf(1f, 0.75f, 0.3f);
                gl.ellipsoid(-1.6f, 4.75f, z, 0.14f, 0.2f, 0.14f, 8, 6);
            }
        }
        // binari nei tratti in carrello: due rotaie per corsia e le traversine
        for (float[] c : w.carts) {
            for (int z = Math.max(z0, (int) c[0]); z < Math.min(z1, (int) c[1]); z++) for (int x = 0; x < World3D.W; x++) {
                if (w.height[z * World3D.W + x] == World3D.PIT) continue;
                float cx = x - World3D.HALF + 0.5f, y = Math.max(0, w.height[z * World3D.W + x]);
                MeshBuilder p = mb(K_PROP, z);
                p.colorf(0.42f, 0.28f, 0.16f);
                p.box(cx, y + 0.03f, z + 0.5f, 0.8f, 0.06f, 0.16f);
                p.colorf(0.62f, 0.64f, 0.7f);
                p.box(cx - 0.24f, y + 0.08f, z + 0.5f, 0.06f, 0.06f, 1.0f);
                p.box(cx + 0.24f, y + 0.08f, z + 0.5f, 0.06f, 0.06f, 1.0f);
            }
        }
        // meshes dinamiche
        MeshBuilder b = new MeshBuilder();
        b.colorf(0.45f, 0.36f, 0.3f);                                   // carrello: cassone di legno cerchiato di ferro su quattro ruote
        b.box(0, 0.35f, 0, 0.9f, 0.5f, 1.1f);
        b.colorf(0.3f, 0.3f, 0.34f);
        b.box(0, 0.62f, 0.56f, 0.94f, 0.08f, 0.06f);
        b.box(0, 0.62f, -0.56f, 0.94f, 0.08f, 0.06f);
        b.box(0.46f, 0.62f, 0, 0.06f, 0.08f, 1.1f);
        b.box(-0.46f, 0.62f, 0, 0.06f, 0.08f, 1.1f);
        b.colorf(0.18f, 0.18f, 0.2f);
        for (int i = 0; i < 4; i++) b.ellipsoid((i < 2 ? -1 : 1) * 0.36f, 0.12f, (i % 2 == 0 ? 0.36f : -0.36f), 0.06f, 0.13f, 0.13f, 10, 6);
        cartMesh = b.build(-1, false, true);
        cartMesh.spec = 0.3f; cartMesh.shine = 20f;
        b = new MeshBuilder();
        b.quad(-0.5f, 0.02f, -0.5f, 0.5f, 0.02f, -0.5f, 0.5f, 0.02f, 0.5f, -0.5f, 0.02f, 0.5f, 0, 1, 0, 0, 0, 1, 1);
        beltBase = b.build(Img.R3F_BELT, false, true);
        b = new MeshBuilder().color(250, 200, 40);
        b.quad(-0.28f, 0.04f, -0.08f, 0f, 0.04f, 0.14f, 0.28f, 0.04f, -0.08f, 0f, 0.04f, 0.02f, 0, 1, 0, 0, 0, 1, 1);
        chevron = b.build(-1, false, true);
        b = new MeshBuilder().color(255, 60, 50);
        b.box(0, 0, 0, World3D.W + 0.2f, 0.07f, 0.07f);
        laserBeam = b.build(-1, true, false);
        b = new MeshBuilder();
        b.colorf(0.3f, 0.3f, 0.34f);
        b.box(0, 0.6f, 0, 0.3f, 1.2f, 0.3f);
        b.colorf(1f, 0.3f, 0.25f);
        b.box(0, 0.55f, 0, 0.34f, 0.12f, 0.34f);
        laserPost = b.build(-1, false, true);
        b = new MeshBuilder();
        b.quad(-0.5f, 0, -0.5f, 0.5f, 0, -0.5f, 0.5f, 0, 0.5f, -0.5f, 0, 0.5f, 0, 1, 0, 0, 0, 1, 1);
        quickPool = b.build(Img.R3S_QUICK, false, true);
        b = new MeshBuilder();
        cactusAt(b, 0, 0, 0, 1f, new Random(5));
        cactus = b.build(-1, false, true);
        b = new MeshBuilder().color(236, 210, 160);
        b.ellipsoid(0, 0, 0, 1, 1, 1, 6, 4);
        dustMote = b.build(-1, true, false);
    }

    /** Cactus a candelabro: fusto a coste e due bracci che salgono. */
    private void cactusAt(MeshBuilder p, float x, float y, float z, float h, Random r) {
        float[] green = {0.32f, 0.56f, 0.3f};
        FoeModels.tube(p, new float[][] {{x, y, z}, {x, y + h * 0.5f, z}, {x, y + h, z}}, new float[] {0.22f * h, 0.2f * h, 0.17f * h}, 10, FoeModels.flat(green));
        p.colorf(green[0], green[1], green[2]);
        p.ellipsoid(x, y + h, z, 0.17f * h, 0.12f * h, 0.17f * h, 10, 5);
        for (int s2 = -1; s2 <= 1; s2 += 2) {
            if (r.nextFloat() < 0.25f) continue;
            float hy = y + h * (0.35f + 0.2f * r.nextFloat()), ax = s2 * 0.42f * h;
            FoeModels.tube(p, new float[][] {{x, hy, z}, {x + ax, hy + 0.05f * h, z}, {x + ax, hy + 0.45f * h, z}}, new float[] {0.11f * h, 0.11f * h, 0.1f * h}, 8, FoeModels.flat(green));
            p.ellipsoid(x + ax, hy + 0.45f * h, z, 0.1f * h, 0.07f * h, 0.1f * h, 8, 4);
        }
    }

    private void drawDryProps(World3D w, Gfx3D g) {
        int z0 = (int) Math.max(0, nearZ), z1 = (int) Math.min(w.length - 1, farZ + 1);
        if (w.belt != null) for (int z = z0; z <= Math.min(z1, (int) w.pz + 45); z++) for (int x = 0; x < World3D.W; x++) {
            int d = w.belt[z * World3D.W + x];
            if (d == 0) continue;
            float cx = x - World3D.HALF + 0.5f, y = Math.max(0, w.height[z * World3D.W + x]);
            float ang = d == 1 ? 1.5708f : d == 2 ? -1.5708f : d == 3 ? 0f : 3.1416f;
            Mat4.setIdentity(m);
            Mat4.translate(m, cx, y, z + 0.5f);
            Mat4.rotateY(m, ang);
            g.draw(beltBase, m, 1, 1, 1, 1, 0.05f);
            for (int i = 0; i < 2; i++) {                                // frecce che scorrono nel verso del nastro
                float u = ((w.time * World3D.BELT_SPEED * 0.9f + i * 0.5f) % 1f + 1f) % 1f;
                Mat4.setIdentity(m);
                Mat4.translate(m, cx, y, z + 0.5f);
                Mat4.rotateY(m, ang);
                Mat4.translate(m, 0, 0, -0.45f + 0.9f * u);
                g.draw(chevron, m, 1, 1, 1, 1, 0.6f);
            }
        }
        if (w.quick != null) for (int z = z0; z <= Math.min(z1, (int) w.pz + 45); z++) for (int x = 0; x < World3D.W; x++) {
            if (!w.quick[z * World3D.W + x]) continue;
            Mat4.setIdentity(m);
            Mat4.translate(m, x - World3D.HALF + 0.5f, 0.03f, z + 0.5f);
            Mat4.rotateY(m, ((x + z) % 4) * 1.5708f);
            float pul = 1f + 0.01f * (float) Math.sin(w.time * 2f + x + z);
            Mat4.scale(m, pul, 1, pul);
            g.draw(quickPool, m, 1, 1, 1, 1, 0.05f);
        }
        for (float[] l : w.lasers) {
            float z = l[0] + 0.5f;
            if (z < nearZ || z > farZ) continue;
            float base = Math.max(0, w.height[(int) l[0] * World3D.W + 2]);
            for (int sd = -1; sd <= 1; sd += 2) {
                Mat4.setIdentity(m);
                Mat4.translate(m, sd * (World3D.HALF + 0.2f), base, z);
                g.draw(laserPost, m, 1, 1, 1, 1, 0.3f);
            }
            float on = World3D.laserOn(l, w.time);
            if (on <= 0) continue;
            for (int k = 0; k < 2; k++) {                                // due raggi, alle ginocchia e alla vita
                Mat4.setIdentity(m);
                Mat4.translate(m, 0, base + 0.3f + k * 0.3f, z);
                if (l[3] >= 0) {                                         // varco spento: il raggio è spezzato in due
                    float gx = l[3] - World3D.HALF + 0.5f;
                    float left = gx - 0.5f + World3D.HALF + 0.1f, right = World3D.HALF + 0.1f - (gx + 0.5f);
                    if (left > 0.05f) { Mat4.setIdentity(m); Mat4.translate(m, -World3D.HALF - 0.1f + left / 2, base + 0.3f + k * 0.3f, z); Mat4.scale(m, left / (World3D.W + 0.2f), 1, 1); g.draw(laserBeam, m, 1, 1, 1, on, 1); }
                    if (right > 0.05f) { Mat4.setIdentity(m); Mat4.translate(m, World3D.HALF + 0.1f - right / 2, base + 0.3f + k * 0.3f, z); Mat4.scale(m, right / (World3D.W + 0.2f), 1, 1); g.draw(laserBeam, m, 1, 1, 1, on, 1); }
                } else g.draw(laserBeam, m, 1, 1, 1, on, 1);
            }
        }
        if (dry == 20) for (float[] t : w.thorns) {                    // nel deserto le spine sono cactus
            if (t[1] < nearZ || t[1] > farZ) continue;
            Mat4.setIdentity(m);
            Mat4.translate(m, t[0], groundAt(t[0], t[1]), t[1]);
            Mat4.scale(m, 0.85f, 0.85f, 0.85f);
            g.draw(cactus, m, 1, 1, 1, 1, 0);
        }
    }

    /** Sabbia sollevata dal vento: sbuffi che strisciano di lato, molto più fitti e veloci durante le raffiche. */
    private void drawDust(World3D w, Gfx3D g) {
        float push = w.windPush(w.pz, w.time) / World3D.GUST_PUSH;
        for (int i = 0; i < 120; i++) {
            float hx = hash(i, 1, 501), hy = hash(i, 2, 502), hz = hash(i, 3, 503);
            float span = 24f, zr = hz * span, z = zr + span * (float) Math.floor((camZ - 2f - zr) / span + 1f);
            float spd = 0.6f + 9f * Math.abs(push);
            float xr = ((hx * 16f + w.time * spd * (push < 0 ? -1 : 1)) % 16f + 16f) % 16f - 8f;
            float y = 0.2f + 2.4f * hy;
            Mat4.setIdentity(m);
            Mat4.translate(m, xr, groundAt(Math.max(-2.4f, Math.min(2.4f, xr)), z) + y, z);
            float st = 0.04f + 0.2f * Math.abs(push);
            Mat4.scale(m, st + 0.03f, 0.03f, 0.03f);
            g.draw(dustMote, m, 1, 1, 1, 0.35f + 0.4f * Math.abs(push), 1);
        }
    }
}
