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
            K_TUFT = 7, K_FERN = 8, K_FLOWER = 9, K_ROCK = 10, K_RAY = 11, K_FIR = 12, K_N = 13;
    private static final int[] K_TEX = {Img.R3_GRASS, Img.R3_DIRT, Img.R3_ROCK, Img.R3_GRASS, Img.R3_FOREST, Img.R3_BARK,
            Img.R3_BRANCH, Img.R3_TUFT, Img.R3_FERN, Img.R3_FLOWERS, Img.R3_ROCK, Img.R3_RAY, Img.R3_FIRTREE};

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
    private Mesh leaf;
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
                for (float z = z0; z < z1; z += 2.1f + row * 0.5f) {
                    Tree t = new Tree();
                    t.row = row;
                    t.x = side * (World3D.HALF + 3.0f + row * 2.6f + r.nextFloat() * 2.0f);
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
        MeshBuilder b;
        int z0 = -8, z1 = w.length + 40;
        zBase = z0;
        nChunks = (z1 - z0) / CH + 1;
        mbs = new MeshBuilder[K_N][nChunks];
        buildFade(w, z0, z1);
        genTrees(z0, z1);

        b = new MeshBuilder();                                         // cielo: occupa la parte alta dello schermo, con la foschia all'orizzonte
        b.quad(-1, 0.18f, 0.999f, 1, 0.18f, 0.999f, 1, 1, 0.999f, -1, 1, 0.999f, 0, 0, -1, 0, 0, 1, 1);
        sky = b.build(Img.R3_SKY, false, false);

        b = new MeshBuilder();                                         // montagne lontane
        b.quad(-330, -4, 0, 330, -4, 0, 330, 78, 0, -330, 78, 0, 0, 0, -1, 0, 0, 2, 1);
        mountains = b.build(Img.R3_MOUNTAINS, true, false);

        int[] order = {0, 1, 2, 0, 2, 3};
        // ---- corridoio: prato ondulato a quadrati di mezzo metro, pietra e pareti di terra
        for (int z = z0; z < z1; z++) {
            for (int x = 0; x < World3D.W; x++) {
                int h = vis(w, x, z);
                float fx = x - World3D.HALF;
                if (h == World3D.PIT) continue;
                if (h == 0) {
                    MeshBuilder g = mb(K_GRASS, z);
                    for (int sx = 0; sx < 2; sx++) {
                        for (int sz = 0; sz < 2; sz++) {
                            float qx = fx + sx * 0.5f, qz = z + sz * 0.5f, e = 0.5f;
                            float[][] c = {{qx, qz}, {qx + e, qz}, {qx + e, qz + e}, {qx, qz + e}};
                            for (int k : order) {
                                float ex = Math.abs(c[k][0]) / World3D.HALF;
                                float t = (1.08f - 0.10f * ex * ex) * aoAt(w, c[k][0], c[k][1]);
                                ground(g, c[k][0], c[k][1], c[k][0] * 0.5f, c[k][1] * 0.5f, t, t, t);
                            }
                        }
                    }
                } else {
                    MeshBuilder s = mb(K_STONE, z);
                    for (int k = 0; k < h; k++) {
                        s.colorf(0.96f + 0.08f * hash(x, z, k), 0.96f + 0.06f * hash(x, z, k + 7), 0.96f);
                        s.box(fx + 0.5f, k + 0.5f, z + 0.5f, 1, 1, 1);
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
        float[] seamG = {1.214f, 1.064f, 1.073f}, seamF = {0.850f, 0.943f, 0.936f};
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
                Mesh mesh = mbs[k][c].build(K_TEX[k], k == K_RAY, true);
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

    /** Albero lontano: tre cartoncini incrociati con l'immagine di un abete intero. */
    private void impostor(float x, float y0, float z, float h, float tint, Random r) {
        MeshBuilder cb = mb(K_FIR, z);
        float top = y0 + h * 1.04f, bot = y0 - 0.15f, hw = h * 1.04f * 0.5f * 0.92f, rot = r.nextFloat() * 3.14f;
        float t = tint * 0.95f, sw = Math.min(1.6f, 0.4f + h * 0.11f) * 0.9f;
        for (int k = 0; k < 3; k++) {
            float a = rot + k * 1.0472f, cx = (float) Math.cos(a) * hw, cz = (float) Math.sin(a) * hw;
            float nx = (float) Math.sin(a) * 0.5f, nz = -(float) Math.cos(a) * 0.5f;
            cb.colorf(t * 0.75f, t * 0.78f, t * 0.7f);
            cb.sway(0);
            cb.vert(x - cx, bot, z - cz, nx, 0.85f, nz, 0, 1);
            cb.vert(x + cx, bot, z + cz, nx, 0.85f, nz, 1, 1);
            cb.colorf(t, t * 1.02f, t * 0.95f);
            cb.sway(sw);
            cb.vert(x + cx, top, z + cz, nx, 0.85f, nz, 1, 0);
            cb.colorf(t * 0.75f, t * 0.78f, t * 0.7f);
            cb.sway(0);
            cb.vert(x - cx, bot, z - cz, nx, 0.85f, nz, 0, 1);
            cb.colorf(t, t * 1.02f, t * 0.95f);
            cb.sway(sw);
            cb.vert(x + cx, top, z + cz, nx, 0.85f, nz, 1, 0);
            cb.vert(x - cx, top, z - cz, nx, 0.85f, nz, 0, 0);
            cb.sway(0);
        }
    }

    private void buildTrees() {
        Random r = new Random(11);
        for (Tree t : trees) {
            if (t.row == 0) fir(t.x, t.y, t.z, t.h, t.rad, t.tint, true, r);
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
                fir(x, groundAt(x, z) - 0.05f, z, h, h * 0.3f, 0.95f + 0.2f * r.nextFloat(), false, r);
            }
            // massi muschiati
            for (float z = z0; z < z1; z += 3.5f + r.nextFloat() * 4f) {
                float d = 0.3f + r.nextFloat() * 3.5f, rad = 0.3f + r.nextFloat() * 0.5f;
                float x = side * (World3D.HALF + d), y = groundAt(x, z) + rad * 0.15f;
                MeshBuilder rb = mb(K_ROCK, z);
                lightAt(x, z, 1f);
                rb.colorf(lt[0] * 0.95f, lt[1] * 0.95f, lt[2] * 0.95f);
                rb.rock(x, y, z, rad * 1.3f, rad * 0.8f, rad * 1.1f, (int) (z * 31) + side, 9, 6);
            }
        }
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
    private static void wall(MeshBuilder d, float ax, float az, float bx, float bz, float nx, float nz) {
        d.colorf(1f, 0.95f, 0.9f);
        d.vert(ax, 0, az, nx, 0, nz, 0, 0);
        d.vert(bx, 0, bz, nx, 0, nz, 1, 0);
        d.colorf(0.04f, 0.05f, 0.045f);
        d.vert(bx, -PIT_DEPTH, bz, nx, 0, nz, 1, PIT_DEPTH);
        d.vert(bx, -PIT_DEPTH, bz, nx, 0, nz, 1, PIT_DEPTH);
        d.vert(ax, -PIT_DEPTH, az, nx, 0, nz, 0, PIT_DEPTH);
        d.colorf(1f, 0.95f, 0.9f);
        d.vert(ax, 0, az, nx, 0, nz, 0, 0);
    }

    /** Parete lungo x alla quota z; dir = +1 se il vuoto sta verso z minore. */
    private static void wallZ(MeshBuilder d, float fx, float z, int dir) {
        wall(d, fx, z, fx + 1, z, 0, -1);
    }

    private static void wallX(MeshBuilder d, float x, float z, int dir) {
        wall(d, x, z, x, z + 1, dir > 0 ? -1 : 1, 0);
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
        g.begin(view, proj, FOG_R, FOG_G, FOG_B, 30f, 115f, 0.9f);
        g.time(w.time);

        g.backdrop(sky);
        Mat4.setIdentity(m);
        Mat4.translate(m, camX * 0.3f, 0, camZ + 230f);
        g.draw(mountains, m, 1, 1, 1, 1, 1);
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
                g.draw(crate, m, 1, 1, 1, 1, 0.15f);
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
            Mat4.translate(m, e.x, e.y + bumpAt(e.x, e.z) + 0.03f, e.z);
            Mat4.scale(m, 1.5f, 1, 1.5f);
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
        final int n = 90;
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
