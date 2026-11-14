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
    private static final int K_GRASS = 0, K_DIRT = 1, K_STONE = 2, K_BANK = 3, K_FOREST = 4, K_TRUNK = 5, K_CROWN = 6,
            K_TUFT = 7, K_FERN = 8, K_FLOWER = 9, K_ROCK = 10, K_RAY = 11, K_FIR = 12, K_BLOB = 13, K_N = 14;
    private static final int[] K_TEX_SUMMER = {Img.R3_GRASS, Img.R3_DIRT, Img.R3_ROCK, Img.R3_GRASS, Img.R3_FOREST, Img.R3_BARK,
            Img.R3_BRANCH, Img.R3_TUFT, Img.R3_FERN, Img.R3_FLOWERS, Img.R3_ROCK, Img.R3_RAY, Img.R3_FIRTREE, Img.R3A_FOLIAGE};
    private static final int[] K_TEX_AUTUMN = {Img.R3A_GRASS, Img.R3_DIRT, Img.R3_ROCK, Img.R3A_GRASS, Img.R3A_FOREST, Img.R3_BARK,
            Img.R3A_CROWN, Img.R3A_TUFT, Img.R3_FERN, Img.R3A_FLOWERS, Img.R3_ROCK, Img.R3_RAY, Img.R3A_TREE, Img.R3A_FOLIAGE};
    private static final int[] K_TEX_AURORA = {Img.R3B_GRASS, Img.R3B_ICE, Img.R3_ROCK, Img.R3B_GRASS, Img.R3B_FOREST, Img.R3_BARK,
            Img.R3B_BRANCH, Img.R3B_TUFT, Img.R3B_TUFT, Img.R3B_FLOWERS, Img.R3_ROCK, Img.R3_RAY, Img.R3B_TREE, Img.R3B_ICE};
    /** Tinte delle chiome d'autunno: arancio, rosso, giallo, ruggine, verde, zucca. */
    private static final float[][] AUTUMN_TINT = {{1.0f, 0.76f, 0.42f}, {1.0f, 0.50f, 0.38f}, {1.0f, 0.96f, 0.46f},
            {0.96f, 0.66f, 0.36f}, {0.76f, 0.9f, 0.40f}, {1.0f, 0.60f, 0.28f}};

    private static final class Tree {
        float x, y, z, h, rad, tint;
        int row;
    }

    private final float[] view = new float[16], proj = new float[16], m = new float[16], m2 = new float[16];
    private final float[] ident = Mat4.identity();

    private Mesh sky, mountains, pitFloor;
    private Mesh[][] land;                       // [tipo][blocco]
    private MeshBuilder[][] mbs;
    private int zBase, nChunks;
    private Tree[] trees;
    private World3D lvl;
    private float[] fadeG;
    private int fadeZ0, fadeNz;
    private Mesh leaf, crateStar;
    private Mesh hogBody, hogSpines, hogEyes, hogLeg, crowBody, crowWing, crowWingL, crowEyes, acornBody, acornCap, acornEyes, acornFoot;
    private boolean autumn, aurora;
    private Mesh flake, curtain;
    private int[] ktex = K_TEX_SUMMER;
    private float fogR = FOG_R, fogG = FOG_G, fogB = FOG_B;
    /** Quanto il bordo dei burroni si arrotonda verso il basso (0 = spigolo vivo). */
    private float lip;
    private Mesh crate, gem, battery, batteryGlow, shadow, ring, gateStone, gateDoor;
    private Mesh robotBody, robotEyes, robotLeg, robotArm, beetleBody, beetleEyes, beetleLeg;
    private int builtFor = -1;
    private float camX, camY = 0, camZ;
    private boolean camInit;
    private final float[] lt = new float[3];

    // ------------------------------------------------------------------ rumore e luce calcolata in anticipo

    private static float hash(int x, int z, int s) {
        int h = x * 374761393 + z * 668265263 + s * 1274126177;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFFFF) / 16777215f;
    }

    private static float vnoise(float x, float z, int s) {
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
    private void lightAt(float x, float z, float base) {
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

    private MeshBuilder mb(int kind, float z) {
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

    private float fadeAt(float x, float z) {
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
    private float groundAt(float x, float z) {
        float y = groundBase(x, z);
        return lip > 0 ? y - lipDrop(x, z) : y;
    }

    /** Il bordo dei burroni si piega verso il basso con un quarto di cerchio (solo estetica). */
    private float lipDrop(float x, float z) {
        int cx = (int) Math.floor(x + World3D.HALF), cz = (int) Math.floor(z);
        float best = lip;
        for (int xi = cx - 1; xi <= cx + 1; xi++) {
            if (xi < 0 || xi >= World3D.W) continue;
            for (int zi = cz - 1; zi <= cz + 1; zi++) {
                if (vis(lvl, xi, zi) != World3D.PIT) continue;
                float x0 = xi - World3D.HALF;
                float dx = Math.max(0, Math.max(x0 - x, x - (x0 + 1))), dz = Math.max(0, Math.max(zi - z, z - (zi + 1)));
                best = Math.min(best, (float) Math.sqrt(dx * dx + dz * dz));
            }
        }
        if (best >= lip) return 0f;
        float k = lip - best;
        return lip - (float) Math.sqrt(Math.max(0f, lip * lip - k * k));
    }

    private float groundBase(float x, float z) {
        float d = Math.abs(x) - World3D.HALF, y = bumpAt(x, z);
        if (d <= 0) return y;
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
        for (int side = -1; side <= 1; side += 2) {
            for (int row = 0; row < 5; row++) {
                for (float z = z0; z < z1; z += (autumn && row < 2 ? 3.3f + row * 0.6f : 2.1f + row * 0.5f)) {
                    Tree t = new Tree();
                    t.row = row;
                    t.x = side * (World3D.HALF + (lvl.levelNo == 4 ? 4.4f : 3.0f) + row * 2.6f + r.nextFloat() * 2.0f);
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
        autumn = w.levelNo == 4;
        aurora = w.levelNo == 6;
        ktex = aurora ? K_TEX_AURORA : autumn ? K_TEX_AUTUMN : K_TEX_SUMMER;
        if (autumn) { fogR = 0.90f; fogG = 0.79f; fogB = 0.64f; lip = 0.5f; }
        if (aurora) { fogR = 0.05f; fogG = 0.10f; fogB = 0.22f; lip = 0.5f; }
        MeshBuilder b;
        int z0 = -8, z1 = w.length + 40;
        zBase = z0;
        nChunks = (z1 - z0) / CH + 1;
        mbs = new MeshBuilder[K_N][nChunks];
        buildFade(w, z0, z1);
        genTrees(z0, z1);

        b = new MeshBuilder();                                         // cielo: occupa la parte alta dello schermo, con la foschia all'orizzonte
        b.quad(-1, 0.18f, 0.999f, 1, 0.18f, 0.999f, 1, 1, 0.999f, -1, 1, 0.999f, 0, 0, -1, 0, 0, 1, 1);
        sky = b.build(aurora ? Img.R3B_SKY : autumn ? Img.R3A_SKY : Img.R3_SKY, false, false);

        b = new MeshBuilder();                                         // montagne lontane
        b.quad(-330, -4, 0, 330, -4, 0, 330, 78, 0, -330, 78, 0, 0, 0, -1, 0, 0, 2, 1);
        mountains = b.build(aurora ? Img.R3B_MOUNTAINS : autumn ? Img.R3A_MOUNTAINS : Img.R3_MOUNTAINS, true, false);
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
                        for (int ax = x - 1; ax <= x + 1; ax++) for (int az = z - 1; az <= z + 1; az++) {
                            if (ax >= 0 && ax < World3D.W && vis(w, ax, az) == World3D.PIT) nearPit = true;
                        }
                    }
                    int sub = nearPit ? 4 : 2;
                    float e = 1f / sub;
                    for (int sx = 0; sx < sub; sx++) {
                        for (int sz = 0; sz < sub; sz++) {
                            float qx = fx + sx * e, qz = z + sz * e;
                            float[][] c = {{qx, qz}, {qx + e, qz}, {qx + e, qz + e}, {qx, qz + e}};
                            for (int k : order) {
                                float ex = Math.abs(c[k][0]) / World3D.HALF;
                                float t = (1.08f - 0.10f * ex * ex) * aoAt(w, c[k][0], c[k][1]);
                                if (lip > 0) t *= 1f - 0.7f * lipDrop(c[k][0], c[k][1]) / lip * 0.45f;
                                ground(g, c[k][0], c[k][1], c[k][0] * 0.5f, c[k][1] * 0.5f, t, t, t);
                            }
                        }
                    }
                } else {
                    MeshBuilder s = mb(K_STONE, z);
                    for (int k = 0; k < h; k++) {
                        s.colorf(0.96f + 0.08f * hash(x, z, k), 0.96f + 0.06f * hash(x, z, k + 7), 0.96f);
                        if (autumn || aurora) s.roundBox(fx + 0.5f, k + 0.5f, z + 0.5f, 0.98f, 0.98f, 0.98f, 0.2f);
                        else s.box(fx + 0.5f, k + 0.5f, z + 0.5f, 1, 1, 1);
                    }
                }
                MeshBuilder d = mb(K_DIRT, z);
                if (vis(w, x, z - 1) == World3D.PIT) wallZ(d, fx, z, 1);
                if (vis(w, x, z + 1) == World3D.PIT) wallZ(d, fx, z + 1, -1);
                if (x > 0 && vis(w, x - 1, z) == World3D.PIT) wallX(d, fx, z, 1);
                if (x < World3D.W - 1 && vis(w, x + 1, z) == World3D.PIT) wallX(d, fx + 1, z, -1);
            }
            MeshBuilder d = mb(K_DIRT, z);
            if (vis(w, 0, z) == World3D.PIT) wallX(d, -World3D.HALF, z, 1);
            if (vis(w, World3D.W - 1, z) == World3D.PIT) wallX(d, World3D.HALF, z, -1);
        }

        b = new MeshBuilder().color(10, 18, 14);                       // fondo dei burroni
        b.quad(-World3D.HALF, -PIT_DEPTH, z0, World3D.HALF, -PIT_DEPTH, z0, World3D.HALF, -PIT_DEPTH, z1, -World3D.HALF, -PIT_DEPTH, z1,
               0, 1, 0, 0.5f, 0.5f, 0.5f, 0.5f);
        pitFloor = b.build(-1, false, true);

        // ---- lati: un'unica superficie continua dal bordo del sentiero al bosco. Il prato sfuma nel sottobosco:
        //      le due texture si incontrano con lo stesso colore medio, così non si vede lo stacco.
        float[] D = {0, 0.35f, 0.75f, 1.2f, 1.7f, 2.3f, SEAM, 4.6f, 6f, 8f, 11f, 15f, 20f, 27f, 36f};
        float[] seamG = autumn || aurora ? new float[] {1f, 1f, 1f} : new float[] {1.214f, 1.064f, 1.073f};
        float[] seamF = autumn || aurora ? new float[] {1f, 1f, 1f} : new float[] {0.850f, 0.943f, 0.936f};
        for (int z = z0; z < z1; z++) {
            for (int side = -1; side <= 1; side += 2) {
                for (int k = 0; k + 1 < D.length; k++) {
                    boolean grassy = D[k + 1] <= SEAM + 0.01f;
                    MeshBuilder g = mb(grassy ? K_BANK : K_FOREST, z);
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
        buildUndergrowth(w, z0, z1);
        buildRays(z0, z1);
        buildObjects();

        // ---- da builder a mesh, con materiale
        land = new Mesh[K_N][nChunks];
        for (int k = 0; k < K_N; k++) {
            for (int c = 0; c < nChunks; c++) {
                if (mbs[k][c] == null) continue;
                Mesh mesh = mbs[k][c].build(ktex[k], k == K_RAY, true);
                if (k == K_CROWN || k == K_TUFT || k == K_FERN || k == K_FLOWER || k == K_FIR) mesh.cutout = true;
                switch (k) {
                    case K_GRASS: case K_BANK: mesh.spec = 0.05f; mesh.shine = 10f; break;
                    case K_STONE: case K_ROCK: mesh.spec = 0.07f; mesh.shine = 14f; break;
                    // il fogliame si vede da entrambi i lati: niente riflesso, che sul rovescio lo farebbe apparire grigio
                    default: break;
                }
                land[k][c] = mesh;
            }
        }
        mbs = null;
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
        int whorls = detailed ? 14 : 9;
        float swBase = Math.min(1.6f, 0.5f + h * 0.14f);
        for (int i = 0; i < whorls; i++) {
            float f = i / (whorls - 1f);
            float yb = y0 + h * (0.2f + 0.8f * f);
            int per = Math.max(3, Math.round((detailed ? 7.4f : 5.4f) - 3.2f * f));
            float R = Math.max(0.35f, rad * (1.12f - 0.94f * f) + 0.15f);
            float rot0 = r.nextFloat() * 6.283f;
            boolean top = i == whorls - 1;
            float tone = tint * (0.86f + 0.2f * f);
            for (int q = 0; q < per; q++) {
                float a = rot0 + q * 6.283f / per + (r.nextFloat() - 0.5f) * 0.7f;
                if (top) bough(cb, x, yb, z, a, R * 0.9f + 0.2f, -1.7f, -1.7f, R * 0.3f + 0.08f, tone, swBase * 0.8f, false, r);
                else bough(cb, x, yb + (r.nextFloat() - 0.5f) * 0.18f, z, a, R * (0.85f + 0.3f * r.nextFloat()),
                        0.10f + 0.12f * (1f - f), 0.55f + 0.3f * (1f - f), R * 0.3f, tone, swBase * (0.55f + 0.7f * f), detailed, r);
            }
        }
    }

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
    private void foliageLobe(MeshBuilder bb, float cx, float cy, float cz, float rx, float ry, float rz, int segs, int rings,
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
        float[] tc = AUTUMN_TINT[r.nextInt(AUTUMN_TINT.length)];
        float k = 0.8f + 0.2f * tint;
        tb.colorf(k, k * 0.92f, k * 0.84f);
        float crownY = y0 + h * 0.62f, r0 = 0.1f + 0.016f * h, top = crownY + h * 0.05f;
        tb.cylinder(x, y0 - 0.3f, y0 + 0.7f, z, r0 * 1.5f, r0 * 1.1f, 8, false, false);
        tb.cylinder(x, y0 + 0.7f, top, z, r0 * 1.1f, r0 * 0.45f, 7, false, false);
        float rh = rad * 1.3f + 0.3f, rv = h * 0.26f;
        float wind = Math.min(1.4f, 0.4f + h * 0.1f);
        int segs = lobes >= 4 ? 9 : 7, rings = lobes >= 4 ? 6 : 4;
        float[][] centers = new float[lobes][3];
        for (int i = 0; i < lobes; i++) {
            float a = r.nextFloat() * 6.283f + i * 2.4f;
            float rr = i == 0 ? 0f : rh * (0.30f + 0.32f * r.nextFloat());
            float cx = x + (float) Math.cos(a) * rr, cz = z + (float) Math.sin(a) * rr;
            float cy = crownY + (i == 0 ? rv * 0.5f : rv * (r.nextFloat() * 1.0f - 0.35f));
            float lr = rh * (i == 0 ? 0.86f : 0.58f + 0.2f * r.nextFloat());
            centers[i][0] = cx; centers[i][1] = cy; centers[i][2] = cz;
            float[] ct = {tc[0] * (0.9f + 0.2f * r.nextFloat()), tc[1] * (0.88f + 0.24f * r.nextFloat()), tc[2] * (0.88f + 0.24f * r.nextFloat())};
            float tone = (0.92f + 0.2f * r.nextFloat()) * (0.9f + 0.15f * tint);
            foliageLobe(bb, cx, cy, cz, lr, lr * 1.0f, lr, segs, rings, ct, tone, wind, r);
            // foglie sciolte sul contorno del lobo
            int fringe = lobes >= 4 ? 6 : 3;
            for (int f = 0; f < fringe; f++) {
                float fa = r.nextFloat() * 6.283f, fu = r.nextFloat() * 1.6f - 0.7f, fl = (float) Math.sqrt(1 - fu * fu);
                float fx = cx + (float) Math.cos(fa) * fl * lr * 1.02f, fy = cy + fu * lr * 0.85f, fz = cz + (float) Math.sin(fa) * fl * lr * 1.02f;
                float ts = (0.95f + 0.3f * (fu * 0.5f + 0.5f)) * (0.92f + 0.15f * r.nextFloat());
                leafCard(cb, fx, fy, fz, 0.55f + 0.35f * r.nextFloat(), r.nextFloat() * 3.14f, (r.nextFloat() - 0.5f) * 1.2f, ct, ts, wind);
            }
        }
    }

    /** Albero lontano: tre cartoncini incrociati con l'immagine di un abete intero. */
    private void impostor(float x, float y0, float z, float h, float tint, Random r) {
        MeshBuilder cb = mb(K_FIR, z);
        float top = y0 + h * 1.04f, bot = y0 - 0.15f, hw = h * 1.04f * 0.5f * (autumn ? 0.8f : 0.92f), rot = r.nextFloat() * 3.14f;
        float t = tint * 0.95f, sw = Math.min(1.6f, 0.4f + h * 0.11f) * 0.9f;
        float[] tc = autumn ? AUTUMN_TINT[Math.abs((int) (x * 7.3f + z * 3.1f)) % AUTUMN_TINT.length] : new float[] {1, 1, 1};
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
        if (autumn) { lr *= 1.25f; lg *= 0.8f; lb *= 0.45f; }
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
                if (autumn) broadleaf(x, groundAt(x, z) - 0.05f, z, h + 0.6f, h * 0.35f, 0.95f + 0.2f * r.nextFloat(), 3, r);
                else fir(x, groundAt(x, z) - 0.05f, z, h, h * 0.3f, 0.95f + 0.2f * r.nextFloat(), false, r);
            }
            // massi muschiati: appoggiati sulla quota più bassa della maglia di terreno sotto di loro e affondati
            for (float z = z0; z < z1; z += 3.5f + r.nextFloat() * 4f) {
                float d = 0.3f + r.nextFloat() * 3.5f, rad = 0.3f + r.nextFloat() * 0.5f;
                float x = side * (World3D.HALF + d);
                float rx = rad * 1.25f, ry = rad * 1.0f, rz = rad * 1.1f;
                float low = meshAt(x, z);
                for (int k = 0; k < 12; k++) {
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
    private float meshAt(float x, float z) {
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
    private static int vis(World3D w, int x, int z) {
        if (z < 0 || z >= w.length) return 0;
        return w.height[z * World3D.W + x];
    }

    /** Parete di terra che scende nel vuoto e si scurisce verso il fondo. Vertici: a-b in alto, poi giù. */
    private void wall(MeshBuilder d, float ax, float az, float bx, float bz, float nx, float nz) {
        float top = -lip;
        d.colorf(1f, 0.95f, 0.9f);
        d.vert(ax, top, az, nx, 0, nz, 0, 0);
        d.vert(bx, top, bz, nx, 0, nz, 1, 0);
        d.colorf(0.04f, 0.05f, 0.045f);
        d.vert(bx, -PIT_DEPTH, bz, nx, 0, nz, 1, PIT_DEPTH);
        d.vert(bx, -PIT_DEPTH, bz, nx, 0, nz, 1, PIT_DEPTH);
        d.vert(ax, -PIT_DEPTH, az, nx, 0, nz, 0, PIT_DEPTH);
        d.colorf(1f, 0.95f, 0.9f);
        d.vert(ax, top, az, nx, 0, nz, 0, 0);
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
        b.box(0, 0.5f, 0, 0.96f, 0.96f, 0.96f);
        crate = b.build(Img.BONUS, false, true);
        crate.spec = 0.1f; crate.shine = 20f;
        if (autumn || aurora) buildPumpkin();
        if (aurora) {                                                  // casse di ghiaccio con la stella luminosa
            b = new MeshBuilder();
            b.roundBox(0, 0.5f, 0, 0.96f, 0.96f, 0.96f, 0.16f);
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

        b = new MeshBuilder().color(62, 68, 80);
        b.cylinder(0, -0.24f, 0.24f, 0, 0.17f, 0.17f, 12, true, true);
        b.color(196, 202, 212);
        b.cylinder(0, 0.24f, 0.31f, 0, 0.08f, 0.08f, 8, true, false);
        battery = b.build(-1, false, true);
        battery.spec = 0.55f; battery.shine = 40f;
        b = new MeshBuilder().color(120, 236, 66);
        b.cylinder(0, -0.15f, 0.15f, 0, 0.176f, 0.176f, 12, false, false);
        batteryGlow = b.build(-1, false, true);

        b = new MeshBuilder();
        b.quad(-0.5f, 0, -0.5f, 0.5f, 0, -0.5f, 0.5f, 0, 0.5f, -0.5f, 0, 0.5f, 0, 1, 0, 0, 0, 1, 1);
        shadow = b.build(Img.SHADOW, true, true);

        b = new MeshBuilder().color(225, 244, 255);
        b.ring(0, 0, 0, 0.86f, 1f, 28);
        ring = b.build(-1, true, true);

        // portone del traguardo: pilastri e architrave di pietra, muro ai lati, due battenti di legno
        b = new MeshBuilder();
        for (int side = -1; side <= 1; side += 2) {
            for (int k = 0; k < 4; k++) {
                b.colorf(0.95f + 0.1f * hash(side, k, 3), 0.95f + 0.08f * hash(side, k, 5), 0.95f);
                b.box(side * 1.7f, -0.15f + 0.45f + k * 0.9f, 0, 0.8f, 0.9f, 0.9f);
            }
            for (int i = 0; i < 5; i++) {
                for (int k = 0; k < 4; k++) {
                    b.colorf(0.88f + 0.12f * hash(side * (i + 2), k, 9), 0.9f + 0.08f * hash(side * (i + 2), k, 11), 0.9f);
                    b.box(side * (2.6f + i), -0.9f + 0.45f + k * 0.9f, 0, 1f, 0.9f, 0.7f);
                }
            }
        }
        for (int i = 0; i < 5; i++) {
            b.colorf(0.95f + 0.1f * hash(i, 1, 13), 0.96f, 0.95f);
            b.box(-1.68f + i * 0.84f, 3.45f + 0.4f, 0, 0.84f, 0.8f, 1.0f);
        }
        for (int i = -1; i <= 1; i++) b.box(i * 1.6f, 4.25f + 0.25f, 0, 0.6f, 0.5f, 0.8f);
        gateStone = b.build(Img.R3_ROCK, false, true);
        gateStone.spec = 0.07f; gateStone.shine = 14f;
        b = new MeshBuilder();                                                // battente: cardine sull'origine, si estende verso x positivo
        b.box(0.65f, 1.65f, 0, 1.3f, 3.6f, 0.12f);
        gateDoor = b.build(Img.R3_WOOD, false, true);
        gateDoor.spec = 0.12f; gateDoor.shine = 18f;

        // --- robot (guarda verso z positivo; la camera lo vede di spalle)
        b = new MeshBuilder().color(234, 236, 240);
        b.ellipsoid(0, 0.62f, 0, 0.25f, 0.30f, 0.21f, 14, 9);                 // busto
        b.color(238, 118, 28);
        b.cylinder(0, 0.46f, 0.52f, 0, 0.245f, 0.25f, 14, false, false);      // cintura
        b.color(204, 96, 24);
        b.box(0, 0.66f, -0.24f, 0.30f, 0.40f, 0.16f);                         // zaino
        b.color(234, 236, 240);
        b.ellipsoid(0, 1.07f, 0, 0.33f, 0.25f, 0.29f, 16, 10);                // testa
        b.color(20, 26, 40);
        b.ellipsoid(0, 1.06f, 0.13f, 0.25f, 0.13f, 0.2f, 14, 8);              // visiera
        b.color(52, 56, 66);
        b.box(0, 1.08f, -0.26f, 0.26f, 0.2f, 0.06f);                          // pannello dietro la testa
        b.color(150, 154, 164);
        b.cylinder(0, 1.3f, 1.46f, 0, 0.014f, 0.014f, 5, false, false);       // antenna
        b.color(232, 48, 48);
        b.ellipsoid(0, 1.49f, 0, 0.05f, 0.05f, 0.05f, 8, 5);
        robotBody = b.build(-1, false, true);
        robotBody.spec = 0.34f; robotBody.shine = 44f;
        b = new MeshBuilder().color(120, 236, 255);
        b.ellipsoid(-0.085f, 1.06f, 0.305f, 0.04f, 0.065f, 0.03f, 8, 5);
        b.ellipsoid(0.085f, 1.06f, 0.305f, 0.04f, 0.065f, 0.03f, 8, 5);
        b.box(0, 0.7f, 0.205f, 0.12f, 0.08f, 0.03f);                          // lampada sul petto
        robotEyes = b.build(-1, false, true);
        b = new MeshBuilder().color(64, 68, 78);                              // gamba: origine all'anca
        b.cylinder(0, -0.26f, 0, 0, 0.06f, 0.065f, 8, false, false);
        b.color(238, 118, 28);
        b.box(0, -0.31f, 0.04f, 0.15f, 0.11f, 0.26f);
        robotLeg = b.build(-1, false, true);
        robotLeg.spec = 0.2f; robotLeg.shine = 30f;
        b = new MeshBuilder().color(64, 68, 78);                              // braccio: origine alla spalla
        b.cylinder(0, -0.3f, 0, 0, 0.048f, 0.052f, 8, false, false);
        b.color(234, 236, 240);
        b.ellipsoid(0, -0.34f, 0, 0.075f, 0.075f, 0.075f, 8, 5);
        robotArm = b.build(-1, false, true);
        robotArm.spec = 0.3f; robotArm.shine = 40f;

        // --- scarabeo (guarda verso z positivo)
        b = new MeshBuilder().color(86, 44, 132);
        b.ellipsoid(0, 0.36f, -0.04f, 0.42f, 0.30f, 0.50f, 16, 9);
        b.color(52, 26, 82);
        b.box(0, 0.38f, -0.04f, 0.02f, 0.6f, 0.98f);                          // giunzione della corazza
        b.color(52, 38, 62);
        b.ellipsoid(0, 0.26f, 0.5f, 0.18f, 0.16f, 0.17f, 10, 6);              // testa
        b.color(30, 22, 34);
        b.box(-0.07f, 0.2f, 0.7f, 0.03f, 0.03f, 0.14f);                       // tenaglie
        b.box(0.07f, 0.2f, 0.7f, 0.03f, 0.03f, 0.14f);
        beetleBody = b.build(-1, false, true);
        beetleBody.spec = 0.5f; beetleBody.shine = 36f;
        b = new MeshBuilder().color(250, 214, 60);
        b.ellipsoid(-0.1f, 0.33f, 0.62f, 0.05f, 0.05f, 0.04f, 8, 5);
        b.ellipsoid(0.1f, 0.33f, 0.62f, 0.05f, 0.05f, 0.04f, 8, 5);
        beetleEyes = b.build(-1, false, true);
        b = new MeshBuilder().color(30, 22, 34);
        b.box(0, -0.12f, 0, 0.05f, 0.24f, 0.05f);
        beetleLeg = b.build(-1, false, true);
        buildFoes();
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

    /** Ala del corvo: remiganti e copritrici a ventaglio. sx = 1 destra, -1 sinistra (specchiata). */
    private static Mesh buildWing(int sx, float[] navy, float[] navy2) {
        MeshBuilder b = new MeshBuilder();
        gball(b, sx * 0.1f, 0f, 0.02f, 0.13f, 0.05f, 0.14f, 8, 5, navy2, navy);
        for (int i = 0; i < 8; i++) {
            float ang = -0.05f - i * 0.1f;
            feather(b, sx * 0.08f, 0.0f, 0.1f - i * 0.045f, sx > 0 ? ang : (float) Math.PI - ang, 0.72f - i * 0.03f, 0.075f, 0.07f, navy, rgb(74, 82, 134));
        }
        for (int i = 0; i < 6; i++) {
            float ang = -0.04f - i * 0.12f;
            feather(b, sx * 0.07f, 0.012f, 0.12f - i * 0.05f, sx > 0 ? ang : (float) Math.PI - ang, 0.38f - i * 0.03f, 0.085f, 0.03f, rgb(30, 34, 70), rgb(96, 104, 156));
        }
        Mesh m = b.build(-1, false, true);
        m.spec = 0.6f; m.shine = 24f;
        return m;
    }

    /** Nemici d'autunno: riccio (guarda verso z positivo), corvo e ghianda. */
    private void buildFoes() {
        Random r = new Random(77);
        float[] cream = rgb(238, 205, 164), brown = rgb(110, 72, 44), pink = rgb(236, 150, 140), black = rgb(18, 14, 14);
        float[] white = {1f, 1f, 0.97f};
        // ---------------- riccio
        MeshBuilder b = new MeshBuilder();
        gball(b, 0, 0.34f, -0.06f, 0.42f, 0.31f, 0.52f, 18, 11, rgb(214, 176, 134), rgb(70, 46, 30));               // corpo: pancia chiara, dorso scuro
        gball(b, 0, 0.22f, 0.44f, 0.2f, 0.18f, 0.27f, 14, 9, rgb(244, 214, 176), cream);                  // testa
        spike(b, 0, 0.21f, 0.62f, 0, -0.06f, 1, 0.22f, 0.105f, cream, rgb(246, 220, 186));                // muso a punta
        gball(b, 0, 0.2f, 0.84f, 0.05f, 0.045f, 0.045f, 8, 6, black, rgb(60, 50, 50));                    // naso lucido
        gball(b, -0.15f, 0.19f, 0.5f, 0.055f, 0.045f, 0.04f, 8, 5, pink, pink);                           // guance rosa
        gball(b, 0.15f, 0.19f, 0.5f, 0.055f, 0.045f, 0.04f, 8, 5, pink, pink);
        gball(b, -0.15f, 0.4f, 0.28f, 0.07f, 0.085f, 0.045f, 8, 6, pink, cream);                          // orecchie
        gball(b, 0.15f, 0.4f, 0.28f, 0.07f, 0.085f, 0.045f, 8, 6, pink, cream);
        gball(b, 0, 0.12f, -0.52f, 0.06f, 0.05f, 0.08f, 8, 5, cream, cream);                              // codina
        hogBody = b.build(-1, false, true);
        hogBody.spec = 0.25f; hogBody.shine = 18f;
        b = new MeshBuilder();
        for (int i = 0; i < 400; i++) {                                  // aculei a due toni, scuri alla radice e chiari in punta
            float a = r.nextFloat() * 6.283f, el = 0.0f + r.nextFloat() * 1.5f;
            float ux = (float) (Math.cos(a) * Math.cos(el)), uy = (float) Math.sin(el), uz = (float) (Math.sin(a) * Math.cos(el));
            if (uz > 0.5f) { if (r.nextFloat() < 0.8f) continue; uz *= 0.3f; }
            float px = ux * 0.41f, py = 0.34f + uy * 0.3f, pz = -0.06f + uz * 0.51f;
            float tone = 0.78f + 0.4f * r.nextFloat(), ln = 0.26f + 0.16f * r.nextFloat();
            float[] c0 = mix(rgb(52, 32, 22), rgb(80, 52, 34), r.nextFloat(), tone);
            float[] c1 = mix(rgb(150, 104, 64), rgb(214, 178, 122), r.nextFloat(), tone * (0.75f + 0.3f * uy));
            spike(b, px, py, pz, ux * 0.8f, uy + 0.15f, uz * 0.8f - 0.55f, ln, 0.052f, c0, c1);
        }
        hogSpines = b.build(-1, false, true);
        hogSpines.spec = 0.3f; hogSpines.shine = 16f;
        b = new MeshBuilder();
        for (int s2 = -1; s2 <= 1; s2 += 2) {                            // occhi: perline nere con riflesso e sopracciglio severo
            gball(b, s2 * 0.085f, 0.3f, 0.58f, 0.048f, 0.055f, 0.036f, 8, 6, black, black);
            gball(b, s2 * 0.075f, 0.318f, 0.612f, 0.014f, 0.016f, 0.01f, 6, 4, white, white);
            spike(b, s2 * 0.14f, 0.385f, 0.585f, -s2 * 1f, -0.55f, 0, 0.1f, 0.018f, rgb(70, 46, 30), rgb(70, 46, 30));
        }
        hogEyes = b.build(-1, false, true);
        hogEyes.spec = 0.9f; hogEyes.shine = 40f;
        b = new MeshBuilder();
        gball(b, 0, -0.07f, 0, 0.055f, 0.13f, 0.055f, 8, 6, cream, rgb(210, 170, 130));                  // zampa
        gball(b, 0, -0.18f, 0.04f, 0.075f, 0.04f, 0.1f, 8, 5, rgb(150, 110, 80), rgb(210, 170, 130));   // piedino
        hogLeg = b.build(-1, false, true);

        // ---------------- corvo
        float[] navy = rgb(22, 26, 52), navy2 = rgb(52, 58, 104), beak = rgb(250, 168, 40), beak2 = rgb(214, 120, 24);
        b = new MeshBuilder();
        gball(b, 0, 0, 0, 0.21f, 0.2f, 0.36f, 16, 10, rgb(64, 68, 108), navy);                           // corpo con riflessi bluastri
        gball(b, 0, -0.04f, 0.16f, 0.15f, 0.14f, 0.16f, 12, 8, rgb(92, 98, 140), rgb(56, 62, 100));     // petto
        gball(b, 0, 0.17f, 0.35f, 0.14f, 0.135f, 0.14f, 12, 8, rgb(52, 58, 96), rgb(24, 28, 56));       // testa
        spike(b, 0, 0.16f, 0.46f, 0, -0.1f, 1, 0.27f, 0.07f, beak2, beak);                                // becco superiore
        spike(b, 0, 0.115f, 0.46f, 0, -0.02f, 1, 0.19f, 0.045f, rgb(190, 100, 20), beak2);               // becco inferiore
        for (int i = -2; i <= 2; i++) feather(b, i * 0.025f, 0.0f, -0.3f, (float) (-Math.PI / 2 + i * 0.16), 0.46f - Math.abs(i) * 0.03f, 0.065f, -0.035f, navy, rgb(70, 76, 124));   // coda a ventaglio
        b.color(0, 0, 0);
        for (int s2 = -1; s2 <= 1; s2 += 2) {                            // zampe raccolte
            gball(b, s2 * 0.08f, -0.2f, 0.0f, 0.03f, 0.08f, 0.03f, 6, 5, rgb(120, 78, 40), rgb(160, 100, 50));
            spike(b, s2 * 0.08f, -0.26f, 0.0f, 0, -0.2f, 1, 0.12f, 0.025f, rgb(130, 82, 40), rgb(130, 82, 40));
        }
        crowBody = b.build(-1, false, true);
        crowBody.spec = 0.75f; crowBody.shine = 30f;
        crowWing = buildWing(1, navy, navy2);
        crowWingL = buildWing(-1, navy, navy2);
        b = new MeshBuilder();
        for (int s2 = -1; s2 <= 1; s2 += 2) {                            // occhi: anello dorato, pupilla, riflesso
            gball(b, s2 * 0.085f, 0.215f, 0.445f, 0.036f, 0.036f, 0.025f, 8, 5, rgb(250, 210, 70), rgb(255, 232, 110));
            gball(b, s2 * 0.09f, 0.215f, 0.462f, 0.02f, 0.021f, 0.012f, 6, 4, black, black);
            gball(b, s2 * 0.083f, 0.225f, 0.47f, 0.007f, 0.007f, 0.005f, 4, 3, white, white);
        }
        crowEyes = b.build(-1, false, true);
        crowEyes.spec = 0.9f; crowEyes.shine = 40f;

        // ---------------- ghianda
        float[] nut0 = rgb(124, 78, 38), nut1 = rgb(222, 158, 74), cap0 = rgb(70, 44, 24), cap1 = rgb(136, 92, 52);
        b = new MeshBuilder();
        gball(b, 0, 0.27f, 0, 0.255f, 0.27f, 0.255f, 14, 9, nut0, nut1);                                  // parte bassa
        gball(b, 0, 0.44f, 0, 0.285f, 0.2f, 0.285f, 14, 8, nut0, nut1);                                   // spalle
        gball(b, -0.17f, 0.3f, 0.2f, 0.055f, 0.04f, 0.03f, 8, 5, rgb(240, 120, 110), rgb(250, 150, 130)); // guance
        gball(b, 0.17f, 0.3f, 0.2f, 0.055f, 0.04f, 0.03f, 8, 5, rgb(240, 120, 110), rgb(250, 150, 130));
        gball(b, -0.3f, 0.3f, 0.05f, 0.06f, 0.075f, 0.06f, 8, 6, rgb(90, 56, 30), nut0);                  // braccini
        gball(b, 0.3f, 0.3f, 0.05f, 0.06f, 0.075f, 0.06f, 8, 6, rgb(90, 56, 30), nut0);
        acornBody = b.build(-1, false, true);
        acornBody.spec = 0.45f; acornBody.shine = 22f;
        b = new MeshBuilder();
        gball(b, 0, 0.6f, 0, 0.35f, 0.2f, 0.35f, 16, 8, cap0, cap1);                                      // cappello a scaglie
        for (int i = 0; i < 44; i++) {
            float a = i * 2.399f, el = 0.15f + (i % 4) * 0.33f;
            float ux = (float) (Math.cos(a) * Math.cos(el)), uy = (float) Math.sin(el), uz = (float) (Math.sin(a) * Math.cos(el));
            float tone = 0.8f + 0.4f * r.nextFloat();
            gball(b, ux * 0.34f, 0.6f + uy * 0.19f, uz * 0.34f, 0.075f, 0.05f, 0.075f, 6, 4, mix(cap0, cap1, 0.2f, tone), mix(cap0, cap1, 0.9f, tone));
        }
        gball(b, 0, 0.5f, 0, 0.345f, 0.04f, 0.345f, 14, 4, rgb(60, 38, 20), rgb(100, 66, 38));           // bordo
        b.color(88, 58, 34);
        b.cylinder(0, 0.74f, 0.93f, 0, 0.05f, 0.032f, 6, true, false);                                    // picciolo
        gball(b, 0.02f, 0.94f, 0, 0.04f, 0.03f, 0.04f, 6, 4, rgb(110, 76, 42), rgb(150, 108, 60));
        acornCap = b.build(-1, false, true);
        acornCap.spec = 0.25f; acornCap.shine = 14f;
        b = new MeshBuilder();
        for (int s2 = -1; s2 <= 1; s2 += 2) {                            // occhi grandi, pupille, riflessi, sopracciglia arrabbiate
            gball(b, s2 * 0.1f, 0.4f, 0.235f, 0.075f, 0.09f, 0.04f, 10, 6, white, white);
            gball(b, s2 * 0.095f, 0.395f, 0.268f, 0.036f, 0.048f, 0.022f, 8, 5, black, black);
            gball(b, s2 * 0.083f, 0.415f, 0.285f, 0.01f, 0.012f, 0.006f, 4, 3, white, white);
            spike(b, s2 * 0.2f, 0.5f, 0.24f, -s2 * 1f, -0.5f, 0, 0.14f, 0.025f, rgb(60, 36, 20), rgb(60, 36, 20));
        }
        gball(b, 0, 0.255f, 0.245f, 0.065f, 0.03f, 0.02f, 8, 5, rgb(70, 24, 24), rgb(120, 40, 40));       // bocca
        acornEyes = b.build(-1, false, true);
        acornEyes.spec = 0.8f; acornEyes.shine = 36f;
        b = new MeshBuilder();
        gball(b, 0, 0.05f, 0.05f, 0.1f, 0.06f, 0.15f, 8, 5, rgb(60, 40, 26), rgb(104, 72, 46));
        acornFoot = b.build(-1, false, true);
    }

    /** Disegna un nemico d'autunno; m è già posizionata e orientata. */
    private void drawFoe(Gfx3D g, Enemy3 e) {
        switch (e.type) {
            case Enemy3.HOG:
                g.draw(hogBody, m, 1, 1, 1, 1, 0);
                g.draw(hogSpines, m, 1, 1, 1, 1, 0);
                g.draw(hogEyes, m, 1, 1, 1, 1, 0.2f);
                for (int i = 0; i < 4; i++) {
                    System.arraycopy(m, 0, m2, 0, 16);
                    Mat4.translate(m2, (i % 2 == 0 ? -1 : 1) * 0.2f, 0.15f, i < 2 ? -0.18f : 0.22f);
                    Mat4.rotateX(m2, (float) Math.sin(e.anim * 10f + i * 1.7f) * 0.6f);
                    g.draw(hogLeg, m2, 1, 1, 1, 1, 0);
                }
                break;
            case Enemy3.CROW:
                g.draw(crowBody, m, 1, 1, 1, 1, 0);
                g.draw(crowEyes, m, 1, 1, 1, 1, 0.9f);
                for (int side = -1; side <= 1; side += 2) {
                    System.arraycopy(m, 0, m2, 0, 16);
                    Mat4.translate(m2, side * 0.16f, 0.06f, 0.02f);
                    Mat4.rotateZ(m2, side * ((float) Math.sin(e.anim * 13f) * 0.95f + 0.15f));
                    g.draw(side > 0 ? crowWing : crowWingL, m2, 1, 1, 1, 1, 0);
                }
                break;
            default:
                g.draw(acornBody, m, 1, 1, 1, 1, 0);
                g.draw(acornCap, m, 1, 1, 1, 1, 0);
                g.draw(acornEyes, m, 1, 1, 1, 1, 0.1f);
                for (int i = -1; i <= 1; i += 2) {
                    System.arraycopy(m, 0, m2, 0, 16);
                    Mat4.translate(m2, i * 0.14f, e.y > e.baseY + 0.05f ? 0.12f : 0f, 0);
                    g.draw(acornFoot, m2, 1, 1, 1, 1, 0);
                }
                break;
        }
    }

    // ------------------------------------------------------------------ fotogramma

    public void render(World3D w, Gfx3D g, float aspect) {
        if (builtFor != w.length) build(w);

        // camera dietro le spalle: segue il giocatore, un po' meno di traverso, e l'altezza del terreno con dolcezza
        float tx = w.px * 0.6f, tz = w.pz;
        if (!camInit) { camX = tx; camZ = tz; camY = w.groundY; camInit = true; }
        camX += (tx - camX) * 0.25f;
        camZ = tz;
        camY += (Math.max(0, w.groundY) - camY) * 0.06f;
        float lift = Math.max(0, w.py - camY) * 0.35f;
        Mat4.lookAt(view, camX, camY + 3.3f + lift, camZ - 6.0f, camX * 0.9f, camY + 1.0f + lift, camZ + 5f);
        Mat4.perspective(proj, 58f, aspect, 0.3f, 400f);
        g.begin(view, proj, fogR, fogG, fogB, 30f, 115f, 0.9f);
        if (autumn) g.grade(1.07f, 0.96f, 0.82f);
        if (aurora) g.grade(0.34f, 0.52f, 0.88f);
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
        Mat4.translate(m, camX * 0.3f, 0, camZ + 230f);
        if (aurora) Mat4.scale(m, 1f, 0.5f, 1f);          // montagne basse: lasciano vedere l'aurora tra gli alberi
        g.draw(mountains, m, 1, 1, 1, 1, aurora ? 0.35f : 1);
        // (le montagne sono già ritoccate nella texture)

        g.draw(pitFloor, ident, 1, 1, 1, 1, 1);
        float vz0 = camZ - 10f, vz1 = camZ + 105f;
        for (int c = 0; c < nChunks; c++) {
            float cz0 = zBase + c * CH, cz1 = cz0 + CH;
            if (cz1 < vz0 || cz0 > vz1) continue;
            for (int k = 0; k < K_N; k++) {
                if (k != K_RAY && land[k][c] != null) g.draw(land[k][c], ident, 1, 1, 1, 1, 0);
            }
        }

        float near = w.pz - 4, far = w.pz + 120;      // gli oggetti tra la camera e il robot finirebbero enormi in primo piano
        for (int z = Math.max(0, (int) near); z < Math.min(w.length, (int) far); z++) {
            for (int x = 0; x < World3D.W; x++) {
                if (!w.crate[z * World3D.W + x]) continue;
                Mat4.setIdentity(m);
                Mat4.translate(m, x - World3D.HALF + 0.5f, bumpAt(x - World3D.HALF + 0.5f, z + 0.5f) - 0.03f, z + 0.5f);
                g.draw(crate, m, 1, 1, 1, 1, autumn ? 0 : aurora ? 0.12f : 0.15f);
                if (crateStar != null) g.draw(crateStar, m, 1, 1, 1, 1, 0.92f);
            }
        }

        // traguardo
        // traguardo: il portone si apre quando il livello è completato
        float open = w.state == World3D.WON ? sm(w.stateTime / 1.1f) * 1.9f : 0, gz = w.flagZ + 0.5f;
        Mat4.setIdentity(m);
        Mat4.translate(m, 0, 0, gz);
        g.draw(gateStone, m, 1, 1, 1, 1, 0);
        Mat4.setIdentity(m);
        Mat4.translate(m, -1.3f, 0, gz);
        Mat4.rotateY(m, -open);
        g.draw(gateDoor, m, 1, 1, 1, 1, 0);
        Mat4.setIdentity(m);
        Mat4.translate(m, 1.3f, 0, gz);
        Mat4.rotateY(m, (float) Math.PI + open);
        g.draw(gateDoor, m, 1, 1, 1, 1, 0);

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
            Mat4.translate(m, w.battery[0], w.battery[1] + 0.07f * (float) Math.sin(w.time * 3f), w.battery[2]);
            Mat4.rotateY(m, w.time * 1.8f);
            Mat4.scale(m, 1.5f, 1.5f, 1.5f);
            g.draw(battery, m, 1, 1, 1, 1, 0.1f);
            g.draw(batteryGlow, m, 1, 1, 1, 1, 0.9f);
        }

        for (Enemy3 e : w.enemies) {
            if (e.dead || e.z < near || e.z > far) continue;
            Mat4.setIdentity(m);
            Mat4.translate(m, e.x, e.y + bumpAt(e.x, e.z), e.z);
            float yawE = e.alongZ ? (e.dir > 0 ? 0 : (float) Math.PI) : (e.dir > 0 ? (float) Math.PI / 2 : -(float) Math.PI / 2);
            Mat4.rotateY(m, yawE);
            if (e.flung) Mat4.rotateX(m, e.anim * 3f);
            if (e.flatTime > 0) Mat4.scale(m, 1.15f, 0.22f, 1.15f);
            if (e.type != Enemy3.BEETLE) { drawFoe(g, e); continue; }
            g.draw(beetleBody, m, 1, 1, 1, 1, 0);
            g.draw(beetleEyes, m, 1, 1, 1, 1, 0.9f);
            for (int i = 0; i < 6; i++) {
                float lx = (i % 2 == 0 ? -1 : 1) * 0.36f, lz = -0.3f + (i / 2) * 0.28f;
                System.arraycopy(m, 0, m2, 0, 16);
                Mat4.translate(m2, lx, 0.24f, lz);
                Mat4.rotateX(m2, (float) Math.sin(e.anim * 14f + i * 2.1f) * 0.5f);
                g.draw(beetleLeg, m2, 1, 1, 1, 1, 0);
            }
        }

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
            float sy = e.type == Enemy3.CROW ? 0 : e.type == Enemy3.ACORN ? e.baseY : e.y;
            float ss = 1.5f * (e.type == Enemy3.ACORN || e.type == Enemy3.CROW ? Math.max(0.5f, 1f - (e.y - sy) * 0.2f) : 1f);
            Mat4.translate(m, e.x, sy + bumpAt(e.x, e.z) + 0.03f, e.z);
            Mat4.scale(m, ss, 1, ss);
            g.draw(shadow, m, 1, 1, 1, 0.8f, 1);
        }
        for (int c = 0; c < nChunks; c++) {
            float cz0 = zBase + c * CH;
            if (land[K_RAY][c] != null && cz0 + CH > camZ - 6f && cz0 < camZ + 60f) g.draw(land[K_RAY][c], ident, 1, 1, 1, 0.15f, 1);
        }
        if (w.spinning()) {
            for (int i = 0; i < 3; i++) {
                float k = (0.62f + 0.14f * (i == 1 ? 1 : 0)) * (1f + 0.07f * (float) Math.sin(w.time * 40f + i));
                Mat4.setIdentity(m);
                Mat4.translate(m, w.px, w.py + bumpAt(w.px, w.pz) + 0.25f + i * 0.42f, w.pz);
                Mat4.scale(m, k, 1, k);
                g.draw(ring, m, 1, 1, 1, 0.75f, 1);
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

    private static final float[][] LEAF_TINT = {{1f, 0.55f, 0.16f}, {1f, 0.86f, 0.2f}, {0.6f, 0.88f, 0.22f}, {0.9f, 0.34f, 0.12f}, {0.98f, 0.7f, 0.12f}};

    /** Foglie che cadono dagli alberi, portate dal vento: si ripetono ogni 34 m attorno al giocatore. */
    private void drawLeaves(World3D w, Gfx3D g) {
        final int n = aurora ? 260 : autumn ? 190 : 90;
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
            float sz = 0.15f + 0.11f * hs;
            Mat4.scale(m, sz, sz, sz);
            float[] c = LEAF_TINT[(int) (hv * 4.999f)];
            g.draw(leaf, m, c[0], c[1], c[2], 1, 0.15f);
        }
    }

    private void drawRobot(World3D w, Gfx3D g) {
        boolean blink = w.state == World3D.PLAYING && w.invuln > 0 && !w.spinning() && ((int) (w.time * 14f) % 2 == 0);
        if (blink) return;
        float[] b = m;
        Mat4.setIdentity(b);
        Mat4.translate(b, w.px, w.py + bumpAt(w.px, w.pz), w.pz);
        Mat4.rotateY(b, w.yaw + (w.spinning() ? w.spinAngle : 0));
        if (w.state == World3D.DYING) Mat4.rotateX(b, -w.stateTime * 5f);
        g.draw(robotBody, b, 1, 1, 1, 1, 0);
        g.draw(robotEyes, b, 1, 1, 1, 1, 1);

        float legA, legB, armA, armB, spread = 0.12f;
        if (w.spinning()) {
            legA = 0.15f; legB = -0.15f; armA = armB = 0; spread = 1.45f;
        } else if (!w.onGround) {
            legA = 0.7f; legB = -0.5f; armA = armB = -2.3f; spread = 0.3f;
        } else {
            float sp = (float) Math.sqrt(w.vx * w.vx + w.vz * w.vz);
            float amp = Math.min(1f, sp / 3f) * 0.85f;
            legA = (float) Math.sin(w.runPhase) * amp;
            legB = -legA;
            armA = -legA * 0.8f;
            armB = -legB * 0.8f;
        }
        limb(g, b, robotLeg, -0.11f, 0.37f, 0, 0, legA);
        limb(g, b, robotLeg, 0.11f, 0.37f, 0, 0, legB);
        limb(g, b, robotArm, -0.27f, 0.8f, 0, -spread, armA);
        limb(g, b, robotArm, 0.27f, 0.8f, 0, spread, armB);
    }

    private void limb(Gfx3D g, float[] base, Mesh mesh, float x, float y, float z, float rz, float rx) {
        System.arraycopy(base, 0, m2, 0, 16);
        Mat4.translate(m2, x, y, z);
        if (rz != 0) Mat4.rotateZ(m2, rz);
        if (rx != 0) Mat4.rotateX(m2, rx);
        g.draw(mesh, m2, 1, 1, 1, 1, 0);
    }
}
