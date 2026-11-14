package com.valeriodipaolo.platformgame.core3d;

import com.valeriodipaolo.platformgame.core.Img;

import java.util.Random;

/** Costruisce i modelli del livello 3D e disegna un fotogramma su un Gfx3D. */
public final class Scene3D {
    private static final float FOG_R = 0.745f, FOG_G = 0.847f, FOG_B = 0.933f;
    private static final float PIT_DEPTH = 5f;
    private static final float BW = 1.9f, BH = 1.5f;          // larghezza e altezza delle sponde erbose
    private static final int CH = 16;                          // il paesaggio è diviso in blocchi lunghi 16 m: si disegnano solo quelli vicini

    // tipi di mesh a blocchi
    private static final int K_GRASS = 0, K_DIRT = 1, K_STONE = 2, K_BANK = 3, K_FOREST = 4, K_TRUNK = 5, K_CROWN = 6,
            K_TUFT = 7, K_FERN = 8, K_FLOWER = 9, K_ROCK = 10, K_RAY = 11, K_N = 12;
    private static final int[] K_TEX = {Img.R3_GRASS, Img.R3_DIRT, Img.R3_ROCK, Img.R3_GRASS, Img.R3_FOREST, Img.R3_BARK,
            Img.R3_BRANCH, Img.R3_TUFT, Img.R3_FERN, Img.R3_FLOWERS, Img.R3_ROCK, Img.R3_RAY};

    private static final class Tree {
        float x, z, h, rad, tint;
        int row;
    }

    private final float[] view = new float[16], proj = new float[16], m = new float[16], m2 = new float[16];
    private final float[] ident = Mat4.identity();

    private Mesh sky, mountains, pitFloor;
    private Mesh[][] land;                       // [tipo][blocco]
    private MeshBuilder[][] mbs;
    private int zBase, nChunks;
    private Tree[] trees;
    private Mesh crate, gem, battery, batteryGlow, shadow, ring, flagPole, flagCloth;
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

    // ------------------------------------------------------------------ modelli fissi

    private void genTrees(int z0, int z1) {
        java.util.ArrayList<Tree> list = new java.util.ArrayList<>();
        Random r = new Random(7);
        for (int side = -1; side <= 1; side += 2) {
            for (int row = 0; row < 5; row++) {
                for (float z = z0; z < z1; z += 2.1f + row * 0.5f) {
                    Tree t = new Tree();
                    t.row = row;
                    t.x = side * (World3D.HALF + BW + 2.2f + row * 2.6f + r.nextFloat() * 1.6f);
                    t.z = z + r.nextFloat() * 1.6f;
                    t.h = 4.6f + r.nextFloat() * 3.6f + row * 0.5f;
                    t.rad = 1.05f + r.nextFloat() * 0.6f;
                    t.tint = 0.78f + r.nextFloat() * 0.34f;
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
        MeshBuilder b;
        int z0 = -8, z1 = w.length + 40;
        zBase = z0;
        nChunks = (z1 - z0) / CH + 1;
        mbs = new MeshBuilder[K_N][nChunks];
        genTrees(z0, z1);

        b = new MeshBuilder();                                         // cielo: occupa la parte alta dello schermo, con la foschia all'orizzonte
        b.quad(-1, 0.18f, 0.999f, 1, 0.18f, 0.999f, 1, 1, 0.999f, -1, 1, 0.999f, 0, 0, -1, 0, 0, 1, 1);
        sky = b.build(Img.R3_SKY, false, false);

        b = new MeshBuilder();                                         // montagne lontane
        b.quad(-330, -4, 0, 330, -4, 0, 330, 78, 0, -330, 78, 0, 0, 0, -1, 0, 0, 2, 1);
        mountains = b.build(Img.R3_MOUNTAINS, true, false);

        // ---- corridoio: prato a quadrati di mezzo metro (luce e ombre per vertice), pietra e pareti di terra
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
                            int[] order = {0, 1, 2, 0, 2, 3};
                            for (int k : order) {
                                float ex = Math.abs(c[k][0]) / World3D.HALF;
                                gv(g, c[k][0], 0, c[k][1], 0, 1, 0, c[k][0] * 0.5f, c[k][1] * 0.5f, 1.12f - 0.22f * ex * ex, aoAt(w, c[k][0], c[k][1]));
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

        // ---- sponde erbose e sottobosco
        float nl = (float) Math.sqrt(BH * BH + BW * BW);
        for (int z = z0; z < z1; z++) {
            MeshBuilder bk = mb(K_BANK, z);
            for (int side = -1; side <= 1; side += 2) {
                for (int k = 0; k < 2; k++) {
                    float t0 = k * 0.5f, t1 = (k + 1) * 0.5f;
                    float xa = side * (World3D.HALF + BW * t0), xb = side * (World3D.HALF + BW * t1);
                    float ya = BH * t0, yb = BH * t1;
                    float nx = -side * BH / nl, ny = BW / nl;
                    float[][] c = {{xa, ya, z}, {xb, yb, z}, {xb, yb, z + 1}, {xa, ya, z + 1}};
                    int[] order = {0, 1, 2, 0, 2, 3};
                    for (int q : order) {
                        gv(bk, c[q][0], c[q][1], c[q][2], nx, ny, 0, c[q][0] * 0.5f, c[q][2] * 0.5f, 0.8f, 1f);
                    }
                }
            }
        }
        for (int z = z0; z < z1; z += 3) {
            MeshBuilder ff = mb(K_FOREST, z);
            for (int side = -1; side <= 1; side += 2) {
                for (float x = World3D.HALF + BW; x < World3D.HALF + BW + 36; x += 3) {
                    float[][] c = {{x, z}, {x + 3, z}, {x + 3, z + 3}, {x, z + 3}};
                    int[] order = {0, 1, 2, 0, 2, 3};
                    for (int q : order) {
                        float xx = side * c[q][0];
                        gv(ff, xx, BH, c[q][1], 0, 1, 0, xx * 0.34f, c[q][1] * 0.34f, 0.62f, 1f);
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
                if (k == K_CROWN || k == K_TUFT || k == K_FERN || k == K_FLOWER) mesh.cutout = true;
                switch (k) {
                    case K_GRASS: case K_BANK: mesh.spec = 0.05f; mesh.shine = 10f; break;
                    case K_STONE: case K_ROCK: mesh.spec = 0.07f; mesh.shine = 14f; break;
                    case K_CROWN: mesh.spec = 0.14f; mesh.shine = 10f; break;
                    case K_TUFT: case K_FERN: case K_FLOWER: mesh.spec = 0.08f; mesh.shine = 8f; break;
                    default: break;
                }
                land[k][c] = mesh;
            }
        }
        mbs = null;
    }

    private void buildTrees() {
        Random r = new Random(11);
        for (Tree t : trees) {
            // tronco: pezzi lunghi circa 2,4 m, così la corteccia mantiene le proporzioni
            MeshBuilder tb = mb(K_TRUNK, t.z);
            float topY = BH + t.h * 0.97f, r0 = 0.17f + 0.05f * r.nextFloat();
            int pieces = Math.max(1, (int) Math.ceil((topY - BH) / 2.4f));
            lightAt(t.x, t.z, 1f);
            for (int p = 0; p < pieces; p++) {
                float ya = BH + (topY - BH) * p / pieces, yb = BH + (topY - BH) * (p + 1) / pieces;
                float ra = r0 * (1f - 0.93f * p / pieces), rb = r0 * (1f - 0.93f * (p + 1) / pieces);
                float k = 0.8f + 0.2f * t.tint;
                tb.colorf(k, k * 0.96f, k * 0.92f);
                tb.cylinder(t.x, ya, yb, t.z, ra, rb, 7, false, false);
            }
            // rami: ogni "foglia" è un cartoncino con la texture di un ramo di abete
            int whorls = t.row < 2 ? 12 : (t.row < 3 ? 9 : 7), per = t.row < 2 ? 7 : (t.row < 3 ? 5 : 4);
            MeshBuilder cb = mb(K_CROWN, t.z);
            for (int i = 0; i < whorls; i++) {
                float f = i / (whorls - 1f);
                float y = BH + t.h * (0.15f + 0.85f * f);
                float len = Math.max(0.45f, t.rad * (1.05f - 0.8f * f) * (0.9f + 0.25f * r.nextFloat()));
                float rot0 = r.nextFloat() * 6.283f;
                boolean top = i == whorls - 1;
                for (int k = 0; k < per; k++) {
                    float a = rot0 + k * 6.283f / per + (r.nextFloat() - 0.5f) * 0.7f;
                    float droop = top ? -1.4f : 0.30f + 0.28f * (1f - f) + r.nextFloat() * 0.14f;
                    float L = top ? len * 0.8f : len;
                    float dx = (float) Math.cos(a), dz = (float) Math.sin(a), dy = -droop;
                    float dl = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                    dx /= dl; dy /= dl; dz /= dl;
                    float sx = -(float) Math.sin(a), sz = (float) Math.cos(a);     // laterale orizzontale
                    float roll = (r.nextFloat() - 0.5f) * 0.9f, cr = (float) Math.cos(roll), sr = (float) Math.sin(roll);
                    // laterale ruotato attorno alla direzione del ramo: s' = s cos + (d × s) sin
                    float cxv = dy * sz, cyv = dz * sx - dx * sz, czv = -dy * sx;
                    float lx = sx * cr + cxv * sr, ly = cyv * sr, lz = sz * cr + czv * sr;
                    float wd = L * 0.78f * 0.5f;
                    float px = t.x + dx * 0.05f, py = y + (r.nextFloat() - 0.5f) * 0.25f, pz = t.z + dz * 0.05f;
                    float ex = px + dx * L, ey = py + dy * L, ez = pz + dz * L;
                    float nx = dx * 0.55f, ny = 0.88f, nz = dz * 0.55f, nn = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                    nx /= nn; ny /= nn; nz /= nn;
                    float tone = t.tint * (0.95f + 0.5f * f);
                    float tr = tone * (0.95f + 0.1f * r.nextFloat()), tg = tone, tbl = tone * 0.92f;
                    lightAt(t.x, t.z, 1f);
                    // la parte vicina al tronco è più scura (ombra dei rami sopra), la punta più chiara
                    cb.colorf(tr * 0.50f, tg * 0.50f, tbl * 0.50f);
                    cb.vert(px - lx * wd * 0.35f, py - ly * wd * 0.35f, pz - lz * wd * 0.35f, nx, ny, nz, 0, 0);
                    cb.vert(px + lx * wd * 0.35f, py + ly * wd * 0.35f, pz + lz * wd * 0.35f, nx, ny, nz, 0, 1);
                    cb.colorf(tr, tg, tbl);
                    cb.vert(ex + lx * wd, ey + ly * wd, ez + lz * wd, nx, ny, nz, 1, 1);
                    cb.colorf(tr * 0.50f, tg * 0.50f, tbl * 0.50f);
                    cb.vert(px - lx * wd * 0.35f, py - ly * wd * 0.35f, pz - lz * wd * 0.35f, nx, ny, nz, 0, 0);
                    cb.colorf(tr, tg, tbl);
                    cb.vert(ex + lx * wd, ey + ly * wd, ez + lz * wd, nx, ny, nz, 1, 1);
                    cb.vert(ex - lx * wd, ey - ly * wd, ez - lz * wd, nx, ny, nz, 1, 0);
                }
            }
        }
    }

    /** Aggiunge un ciuffo di vegetazione fatto da tre cartoncini incrociati. */
    private void plant(int kind, float x, float y, float z, float size, Random r) {
        MeshBuilder b = mb(kind, z);
        lightAt(x, z, 1f);
        float lr = lt[0], lg = lt[1], lb = lt[2];
        float rot = r.nextFloat() * 3.14f, hw = size * (kind == K_FERN ? 0.62f : 0.5f);
        for (int k = 0; k < 3; k++) {
            float a = rot + k * 1.0472f, cx = (float) Math.cos(a) * hw, cz = (float) Math.sin(a) * hw;
            b.colorf(lr * 0.55f, lg * 0.55f, lb * 0.55f);
            b.vert(x - cx, y, z - cz, 0, 1, 0, 0, 1);
            b.vert(x + cx, y, z + cz, 0, 1, 0, 1, 1);
            b.colorf(lr, lg, lb);
            b.vert(x + cx, y + size, z + cz, 0, 1, 0, 1, 0);
            b.colorf(lr * 0.55f, lg * 0.55f, lb * 0.55f);
            b.vert(x - cx, y, z - cz, 0, 1, 0, 0, 1);
            b.colorf(lr, lg, lb);
            b.vert(x + cx, y + size, z + cz, 0, 1, 0, 1, 0);
            b.vert(x - cx, y + size, z - cz, 0, 1, 0, 0, 0);
        }
    }

    private void buildUndergrowth(World3D w, int z0, int z1) {
        Random r = new Random(21);
        for (int side = -1; side <= 1; side += 2) {
            for (float z = z0; z < z1; z += 0.8f + r.nextFloat() * 0.7f) {
                for (int k = 0; k < 2; k++) {
                    float d = 0.4f + r.nextFloat() * r.nextFloat() * (BW + 2.8f);
                    float x = side * (World3D.HALF + d), y = d < BW ? BH * d / BW : BH;
                    float q = r.nextFloat();
                    if (q < 0.5f) plant(K_TUFT, x, y, z, 0.7f + r.nextFloat() * 0.7f, r);
                    else if (q < 0.78f) plant(K_FERN, x, y, z, 0.9f + r.nextFloat() * 0.8f, r);
                    else plant(K_FLOWER, x, y, z, 0.55f + r.nextFloat() * 0.45f, r);
                }
            }
            // ciuffi bassi lungo il bordo del sentiero, solo dove c'è prato su entrambi i lati
            for (float z = 0; z < w.length; z += 0.9f + r.nextFloat() * 0.8f) {
                int zi = (int) z;
                if (vis(w, 0, zi) != 0 || vis(w, World3D.W - 1, zi) != 0) continue;
                plant(K_TUFT, side * (World3D.HALF + 0.02f + r.nextFloat() * 0.1f), 0, z, 0.26f + r.nextFloat() * 0.16f, r);
            }
            // massi muschiati ai piedi delle sponde
            for (float z = z0; z < z1; z += 3.5f + r.nextFloat() * 4f) {
                float d = 0.1f + r.nextFloat() * (BW + 0.8f);
                float rad = 0.3f + r.nextFloat() * 0.5f;
                float x = side * (World3D.HALF + d), y = (d < BW ? BH * d / BW : BH) + rad * 0.15f;
                MeshBuilder rb = mb(K_ROCK, z);
                lightAt(x, z, 1f);
                rb.colorf(lt[0] * 0.95f, lt[1] * 0.95f, lt[2] * 0.95f);
                rb.rock(x, y, z, rad * 1.3f, rad * 0.8f, rad * 1.1f, (int) (z * 31) + side, 9, 6);
            }
        }
    }

    /** Raggi di sole che attraversano il bosco: strisce chiare e sfumate nella direzione della luce. */
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

        b = new MeshBuilder().color(176, 182, 192);
        b.cylinder(0, 0, 6.2f, 0, 0.07f, 0.07f, 8, false, false);
        b.color(238, 186, 40);
        b.ellipsoid(0, 6.3f, 0, 0.17f, 0.17f, 0.17f, 10, 6);
        flagPole = b.build(-1, false, true);
        b = new MeshBuilder();
        b.quad(0.08f, 0, 0, 1.88f, 0, 0, 1.88f, 1.18f, 0, 0.08f, 1.18f, 0, 0, 0, -1, 0, 0, 1, 1);
        flagCloth = b.build(Img.FLAG, true, true);

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
                Mat4.translate(m, x - World3D.HALF + 0.5f, 0, z + 0.5f);
                g.draw(crate, m, 1, 1, 1, 1, 0.15f);
            }
        }

        // traguardo
        Mat4.setIdentity(m);
        Mat4.translate(m, 0, 0, w.flagZ + 0.5f);
        g.draw(flagPole, m, 1, 1, 1, 1, 0);

        for (float[] c : w.gemsList) {
            if (c[2] < near || c[2] > far) continue;
            Mat4.setIdentity(m);
            Mat4.translate(m, c[0], c[1] + 0.06f * (float) Math.sin(w.time * 3f + c[2]), c[2]);
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
            Mat4.translate(m, e.x, e.y, e.z);
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

        // ---- trasparenze: ombre, anelli, bandiera
        if (w.topAt(w.px, w.pz) > -500 && w.state != World3D.DYING) {       // niente ombra sopra il vuoto
            float sh = Math.max(0.35f, 1f - (w.py - w.groundY) * 0.14f);
            Mat4.setIdentity(m);
            Mat4.translate(m, w.px, w.groundY + 0.03f, w.pz);
            Mat4.scale(m, 1.25f * sh, 1, 1.25f * sh);
            g.draw(shadow, m, 1, 1, 1, 1, 1);
        }
        for (Enemy3 e : w.enemies) {
            if (e.dead || e.flung || e.z < near || e.z > far) continue;
            Mat4.setIdentity(m);
            Mat4.translate(m, e.x, e.y + 0.03f, e.z);
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
                Mat4.translate(m, w.px, w.py + 0.25f + i * 0.42f, w.pz);
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
        float drop = w.state == World3D.WON ? Math.min(1f, w.stateTime / 1.0f) : 0;
        Mat4.setIdentity(m);
        Mat4.translate(m, 0, 4.9f - drop * 3.9f, w.flagZ + 0.5f);
        g.draw(flagCloth, m, 1, 1, 1, 1, 0.6f);

        g.end();
    }

    private void drawRobot(World3D w, Gfx3D g) {
        boolean blink = w.state == World3D.PLAYING && w.invuln > 0 && !w.spinning() && ((int) (w.time * 14f) % 2 == 0);
        if (blink) return;
        float[] b = m;
        Mat4.setIdentity(b);
        Mat4.translate(b, w.px, w.py, w.pz);
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
