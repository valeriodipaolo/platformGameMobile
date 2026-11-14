package com.valeriodipaolo.platformgame.core3d;

import java.util.Random;

/**
 * Dettagli che arricchiscono i livelli 3D: funghi, tronchi caduti, ceppi, radici, cespugli con bacche, sassolini,
 * mucchi di foglie, cumuli di neve e cristalli di ghiaccio. Solo estetica: non toccano le regole del gioco.
 */
final class Decor {
    private static final float PI = (float) Math.PI;
    private final Scene3D s;
    private final float[] l = new float[3];

    Decor(Scene3D s) {
        this.s = s;
    }

    // ------------------------------------------------------------------ attrezzi

    private void light(float x, float z) {
        s.lightAt(x, z, 1f);
        l[0] = Math.min(1.5f, s.lt[0]); l[1] = Math.min(1.5f, s.lt[1]); l[2] = Math.min(1.5f, s.lt[2]);
    }

    private float[] lit(float r, float g, float b, float k) {
        return new float[] {r / 255f * l[0] * k, g / 255f * l[1] * k, b / 255f * l[2] * k};
    }

    /** Dischetto (ventaglio di triangoli) centrato in c, rivolto verso n: centro di colore c0, bordo c1. */
    static void disc(MeshBuilder b, float cx, float cy, float cz, float nx, float ny, float nz, float rad, int sides, float[] c0, float[] c1) {
        float nl = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        nx /= nl; ny /= nl; nz /= nl;
        float ax = Math.abs(ny) < 0.9f ? 0 : 1, ay = Math.abs(ny) < 0.9f ? 1 : 0, az = 0;
        float ux = ay * nz - az * ny, uy = az * nx - ax * nz, uz = ax * ny - ay * nx;
        float ul = (float) Math.sqrt(ux * ux + uy * uy + uz * uz);
        ux /= ul; uy /= ul; uz /= ul;
        float vx = ny * uz - nz * uy, vy = nz * ux - nx * uz, vz = nx * uy - ny * ux;
        for (int i = 0; i < sides; i++) {
            double a0 = 2 * Math.PI * i / sides, a1 = 2 * Math.PI * (i + 1) / sides;
            float c0a = (float) Math.cos(a0) * rad, s0a = (float) Math.sin(a0) * rad, c1a = (float) Math.cos(a1) * rad, s1a = (float) Math.sin(a1) * rad;
            b.colorf(c0[0], c0[1], c0[2]);
            b.vert(cx, cy, cz, nx, ny, nz, 0.5f, 0.5f);
            b.colorf(c1[0], c1[1], c1[2]);
            b.vert(cx + ux * c0a + vx * s0a, cy + uy * c0a + vy * s0a, cz + uz * c0a + vz * s0a, nx, ny, nz, 0.5f, 0.5f);
            b.vert(cx + ux * c1a + vx * s1a, cy + uy * c1a + vy * s1a, cz + uz * c1a + vz * s1a, nx, ny, nz, 0.5f, 0.5f);
        }
    }

    /** Quota più bassa della superficie disegnata sotto un'impronta circolare: gli oggetti ci si appoggiano senza restare sospesi. */
    private float rest(float x, float z, float rad) {
        float low = s.meshAt(x, z);
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 4;
            low = Math.min(low, s.meshAt(x + (float) Math.cos(a) * rad, z + (float) Math.sin(a) * rad));
        }
        return low;
    }

    // ------------------------------------------------------------------ pezzi

    /** Fungo: gambo che si allarga alla base, cappello a cupola con lamelle chiare sotto e puntini sopra. */
    private void mushroom(float x, float y, float z, float h, float capR, final float[] cap, final float[] dots, Random r) {
        MeshBuilder b = s.mb(Scene3D.K_PROP, z);
        float lean = (r.nextFloat() - 0.5f) * h * 0.5f, lz = (r.nextFloat() - 0.5f) * h * 0.4f;
        float tx = x + lean, tz = z + lz;
        final float[] stem0 = lit(206, 196, 170, 0.75f), stem1 = lit(246, 240, 222, 1f);
        FoeModels.tube(b, new float[][] {{x, y - 0.04f, z}, {x + lean * 0.3f, y + h * 0.4f, z + lz * 0.3f}, {tx, y + h, tz}},
                new float[] {capR * 0.36f, capR * 0.24f, capR * 0.22f}, 6, FoeModels.along(stem0, stem1));
        final float[] gill = lit(232, 220, 190, 0.7f), rim = {cap[0] * 0.72f, cap[1] * 0.72f, cap[2] * 0.72f};
        final float[] topc = {Math.min(1.4f, cap[0] * 1.18f), Math.min(1.4f, cap[1] * 1.18f), Math.min(1.4f, cap[2] * 1.18f)};
        float[] pr = {0.2f, 0.62f, 1f, 0.96f, 0.76f, 0.44f, 0f}, ph = {0.02f, -0.06f, 0f, 0.2f, 0.46f, 0.66f, 0.74f};
        float[][] c = new float[pr.length][];
        float[] rr = new float[pr.length];
        for (int i = 0; i < pr.length; i++) {
            c[i] = new float[] {tx, y + h - capR * 0.1f + ph[i] * capR, tz};
            rr[i] = pr[i] * capR;
        }
        FoeModels.loft(b, c, rr, rr, 0, 0, 1, 8, 1f, new FoeModels.Paint() {
            @Override void col(float t, float a, float px, float py, float pz, float[] o) {
                if (t < 0.25f) { o[0] = gill[0]; o[1] = gill[1]; o[2] = gill[2]; }
                else FoeModels.mix(rim, topc, FoeModels.sm((t - 0.3f) / 0.6f), o);
            }
        }, 0, 0);
        if (dots != null) {
            for (int k = 0; k < 4; k++) {
                float a = k * 1.57f + r.nextFloat() * 0.6f, up = 0.25f + 0.6f * r.nextFloat();
                float sr = (float) Math.sqrt(1 - up * up);
                float nx = (float) Math.cos(a) * sr, nz = (float) Math.sin(a) * sr;
                float rad = capR * (0.94f - 0.2f * up), hy = capR * 0.76f * up;
                disc(b, tx + nx / sr * rad * sr + nx * 0.006f, y + h - capR * 0.1f + hy + 0.012f, tz + nz / sr * rad * sr + nz * 0.006f,
                        nx, up + 0.3f, nz, capR * (0.1f + 0.08f * r.nextFloat()), 5, dots, dots);
            }
        }
    }

    private void mushrooms(float x, float z, Random r) {
        int n = 2 + r.nextInt(3);
        int kind = r.nextInt(3);
        for (int i = 0; i < n; i++) {
            float mx = x + (r.nextFloat() - 0.5f) * 0.7f, mz = z + (r.nextFloat() - 0.5f) * 0.7f;
            float h = 0.16f + 0.26f * r.nextFloat(), cr = 0.1f + 0.14f * r.nextFloat();
            light(mx, mz);
            float[] cap, dots = null;
            if (s.aurora) {                                   // funghi azzurri che sembrano brillare nel buio
                cap = kind == 0 ? lit(90, 200, 255, 1.25f) : lit(170, 120, 255, 1.25f);
                dots = lit(240, 255, 255, 1.6f);
            } else if (kind == 0) {
                cap = lit(214, 36, 30, 1.05f);
                dots = lit(255, 250, 236, 1.1f);
            } else if (kind == 1) {
                cap = s.autumn ? lit(226, 130, 40, 1f) : lit(150, 96, 52, 1f);
            } else {
                cap = s.autumn ? lit(190, 96, 150, 1f) : lit(232, 190, 96, 1f);
                dots = s.autumn ? lit(250, 226, 240, 1f) : null;
            }
            mushroom(mx, s.meshAt(mx, mz), mz, h, cr, cap, dots, r);
        }
    }

    /** Tronco caduto: corteccia con muschio sul dorso, tagli chiari con gli anelli alle due estremità, un mozzicone di ramo. */
    private void log(float x, float z, float len, float rad, float ang, Random r) {
        float dx = (float) Math.cos(ang), dz = (float) Math.sin(ang);
        float ax = x - dx * len / 2, az = z - dz * len / 2, bx = x + dx * len / 2, bz = z + dz * len / 2;
        float ya = rest(ax, az, rad) + rad * 0.72f, yb = rest(bx, bz, rad) + rad * 0.72f;
        float ym = Math.max((ya + yb) / 2, rest(x, z, rad) + rad * 0.72f);
        light(x, z);
        final float[] bark = lit(250, 236, 220, 0.95f), moss = s.aurora ? lit(236, 244, 255, 1.5f) : s.autumn ? lit(190, 170, 70, 1.1f) : lit(120, 190, 70, 1.2f);
        final float[] under = lit(150, 140, 130, 0.6f);
        MeshBuilder tb = s.mb(Scene3D.K_TRUNK, z);
        int n = 6;
        float[][] c = new float[n][];
        float[] rr = new float[n];
        for (int i = 0; i < n; i++) {
            float t = i / (float) (n - 1);
            float yy = (1 - t) * (1 - t) * ya + 2 * t * (1 - t) * ym + t * t * yb;
            c[i] = new float[] {ax + (bx - ax) * t, yy, az + (bz - az) * t};
            rr[i] = rad * (1f - 0.22f * t) * (1f + 0.06f * (float) Math.sin(t * 9f + ang));
        }
        final int seed = (int) (x * 13 + z * 7);
        FoeModels.loft(tb, c, rr, rr, 0, 1, 0, 9, 1f, new FoeModels.Paint() {
            @Override void col(float t, float a, float px, float py, float pz, float[] o) {
                float up = (float) Math.sin(a);
                if (up < -0.2f) { FoeModels.mix(bark, under, FoeModels.sm((-up - 0.2f) / 0.6f), o); return; }
                float m = FoeModels.sm((up - 0.35f + 0.5f * (Scene3D.vnoise(px * 2.2f, pz * 2.2f, seed) - 0.5f)) / 0.4f);
                FoeModels.mix(bark, moss, m * 0.85f, o);
            }
        }, 2f, 0.6f);
        MeshBuilder pb = s.mb(Scene3D.K_PROP, z);
        float[] ring0 = lit(150, 104, 62, 0.9f), ring1 = lit(226, 190, 136, 1f), rimc = lit(96, 70, 48, 0.9f);
        disc(pb, c[0][0] - dx * 0.004f, c[0][1], c[0][2] - dz * 0.004f, -dx, 0, -dz, rr[0] * 0.9f, 10, ring0, ring1);
        disc(pb, c[0][0] - dx * 0.002f, c[0][1], c[0][2] - dz * 0.002f, -dx, 0, -dz, rr[0] * 1.0f, 10, rimc, rimc);
        disc(pb, c[n - 1][0] + dx * 0.004f, c[n - 1][1], c[n - 1][2] + dz * 0.004f, dx, 0, dz, rr[n - 1] * 0.9f, 10, ring0, ring1);
        disc(pb, c[n - 1][0] + dx * 0.002f, c[n - 1][1], c[n - 1][2] + dz * 0.002f, dx, 0, dz, rr[n - 1] * 1.0f, 10, rimc, rimc);
        // mozzicone di ramo
        float t = 0.3f + 0.4f * r.nextFloat(), sx = ax + (bx - ax) * t, sz = az + (bz - az) * t, sy = ya + (yb - ya) * t;
        float side = r.nextBoolean() ? 1 : -1;
        tb.colorf(bark[0], bark[1], bark[2]);
        FoeModels.tube(tb, sx, sy + rad * 0.5f, sz, sx - dz * side * rad * 1.2f, sy + rad * 1.9f, sz + dx * side * rad * 1.2f, rad * 0.24f, rad * 0.13f, 5,
                FoeModels.flat(bark));
        if (r.nextFloat() < 0.7f) mushrooms(x - dz * side * (rad + 0.25f), z + dx * side * (rad + 0.25f), r);
    }

    /** Ceppo: base che si allarga nelle radici, taglio in cima con gli anelli. */
    private void stump(float x, float z, float rad, float h, Random r) {
        float y = rest(x, z, rad * 1.5f);
        light(x, z);
        final float[] bark = lit(250, 236, 220, 0.95f), dark = lit(170, 150, 130, 0.7f);
        MeshBuilder tb = s.mb(Scene3D.K_TRUNK, z);
        float[] hs = {-0.25f, 0.04f, 0.2f, 0.6f, 1f}, ws = {1.75f, 1.45f, 1.12f, 1.0f, 0.96f};
        float[][] c = new float[hs.length][];
        float[] rr = new float[hs.length];
        for (int i = 0; i < hs.length; i++) {
            c[i] = new float[] {x, y + hs[i] * h, z};
            rr[i] = ws[i] * rad;
        }
        FoeModels.loft(tb, c, rr, rr, 0, 0, 1, 10, 1f, FoeModels.along(dark, bark), 2f, 0.8f);
        MeshBuilder pb = s.mb(Scene3D.K_PROP, z);
        float[] ring0 = lit(140, 98, 58, 0.9f), ring1 = lit(230, 196, 140, 1.05f), rimc = lit(96, 70, 48, 0.9f);
        disc(pb, x, y + h + 0.003f, z, 0, 1, 0, rad * 0.96f, 10, rimc, rimc);
        disc(pb, x, y + h + 0.006f, z, 0, 1, 0, rad * 0.84f, 10, ring0, ring1);
        if (s.aurora) snow(x, y + h + 0.02f, z, rad * 0.9f, 0.1f, rad * 0.9f);
        else if (r.nextFloat() < 0.6f) mushrooms(x + rad * 1.6f, z - rad * 0.6f, r);
    }

    /** Radici in vista ai piedi di un albero. */
    void roots(float x, float y, float z, float r0, int n, Random r) {
        MeshBuilder tb = s.mb(Scene3D.K_TRUNK, z);
        light(x, z);
        final float[] c0 = lit(240, 226, 210, 0.9f), c1 = lit(190, 176, 160, 0.7f);
        float rot = r.nextFloat() * 6.283f;
        for (int i = 0; i < n; i++) {
            float a = rot + i * 6.283f / n + (r.nextFloat() - 0.5f) * 0.6f, ca = (float) Math.cos(a), sa = (float) Math.sin(a);
            float len = r0 * (3.2f + 2.4f * r.nextFloat());
            float ex = x + ca * len, ez = z + sa * len, mx = x + ca * len * 0.5f, mz = z + sa * len * 0.5f;
            FoeModels.loft(tb, new float[][] {{x + ca * r0 * 0.3f, y + 0.75f, z + sa * r0 * 0.3f}, {x + ca * r0 * 1.2f, y + 0.36f, z + sa * r0 * 1.2f},
                            {mx, s.meshAt(mx, mz) + r0 * 0.25f, mz}, {ex, s.meshAt(ex, ez) - 0.1f, ez}},
                    new float[] {r0 * 0.6f, r0 * 0.55f, r0 * 0.36f, r0 * 0.1f}, new float[] {r0 * 0.75f, r0 * 0.6f, r0 * 0.38f, r0 * 0.1f},
                    0, 1, 0, 4, 1f, FoeModels.along(c0, c1), 1f, 0.7f);
        }
    }

    /** Cespuglio basso con bacche. */
    private void bush(float x, float z, float size, Random r) {
        float y = s.meshAt(x, z);
        light(x, z);
        MeshBuilder bb = s.mb(Scene3D.K_BLOB, z), pb = s.mb(Scene3D.K_PROP, z);
        float[] tc = s.autumn ? new float[] {1.0f * l[0], 0.62f * l[1], 0.32f * l[2]} : new float[] {0.62f * l[0], 1.0f * l[1], 0.5f * l[2]};
        float[] berry = s.autumn ? lit(250, 150, 30, 1.3f) : lit(236, 40, 60, 1.3f);
        int lobes = 3 + r.nextInt(2);
        for (int i = 0; i < lobes; i++) {
            float a = r.nextFloat() * 6.283f, d = i == 0 ? 0 : size * (0.45f + 0.35f * r.nextFloat());
            float cx = x + (float) Math.cos(a) * d, cz = z + (float) Math.sin(a) * d, lr = size * (i == 0 ? 0.75f : 0.5f + 0.2f * r.nextFloat());
            float cy = s.meshAt(cx, cz) + lr * 0.55f;
            s.foliageLobe(bb, cx, cy, cz, lr, lr * 0.85f, lr, 6, 3, tc, 0.95f + 0.2f * r.nextFloat(), 0.35f, r);
            pb.colorf(berry[0], berry[1], berry[2]);
            for (int k = 0; k < 4; k++) {
                float ba = r.nextFloat() * 6.283f, bu = 0.1f + 0.75f * r.nextFloat(), bs = (float) Math.sqrt(1 - bu * bu);
                pb.bipyramid(cx + (float) Math.cos(ba) * bs * lr * 1.02f, cy + bu * lr * 0.85f, cz + (float) Math.sin(ba) * bs * lr * 1.02f, 0.035f, 0.035f, 4);
            }
        }
    }

    /** Mucchio di foglie (o cumulo basso) che copre il terreno. */
    private void leafPile(float x, float z, float size, Random r) {
        light(x, z);
        float[] tc = Scene3D.AUTUMN_TINT[r.nextInt(Scene3D.AUTUMN_TINT.length)];
        s.foliageLobe(s.mb(Scene3D.K_BLOB, z), x, rest(x, z, size) + size * 0.05f, z, size, size * 0.24f, size * (0.7f + 0.3f * r.nextFloat()), 6, 3,
                new float[] {tc[0] * l[0], tc[1] * l[1], tc[2] * l[2]}, 1.25f, 0f, r);
    }

    /** Cumulo di neve tondeggiante. */
    private void snow(float x, float y, float z, float rx, float ry, float rz) {
        light(x, z);
        final float[] lo = lit(170, 196, 236, 1.5f), hi = lit(250, 252, 255, 2.4f);
        FoeModels.ballY(s.mb(Scene3D.K_PROP, z), x, y, z, rx, ry, rz, 9, 5, new FoeModels.Paint() {
            @Override void col(float t, float a, float px, float py, float pz, float[] o) { FoeModels.mix(lo, hi, FoeModels.sm(t * 1.3f - 0.2f), o); }
        }, 0, 0);
    }

    /** Ciuffo di cristalli di ghiaccio luminosi: prismi a sei facce con la punta. */
    private void crystals(float x, float z, Random r) {
        MeshBuilder b = s.mb(Scene3D.K_GLOW, z);
        float y = s.meshAt(x, z) - 0.08f;
        int n = 3 + r.nextInt(3);
        boolean violet = r.nextFloat() < 0.35f;
        for (int i = 0; i < n; i++) {
            float a = r.nextFloat() * 6.283f, tilt = i == 0 ? 0.08f : 0.25f + 0.45f * r.nextFloat();
            float dx = (float) (Math.cos(a) * Math.sin(tilt)), dy = (float) Math.cos(tilt), dz = (float) (Math.sin(a) * Math.sin(tilt));
            float len = (i == 0 ? 1.0f : 0.45f + 0.4f * r.nextFloat()) * (0.7f + 0.6f * r.nextFloat()), rad = len * 0.13f;
            float bx = x + dx * 0.12f, bz = z + dz * 0.12f;
            // base perpendicolare alla direzione
            float ux = dy, uy = -dx, uz = 0, ul = (float) Math.sqrt(ux * ux + uy * uy);
            if (ul < 1e-3f) { ux = 1; uy = 0; ul = 1; }
            ux /= ul; uy /= ul;
            float vx = dy * uz - dz * uy, vy = dz * ux - dx * uz, vz = dx * uy - dy * ux;
            float[] c0 = violet ? new float[] {0.36f, 0.24f, 0.75f} : new float[] {0.1f, 0.46f, 0.72f};
            float[] c1 = violet ? new float[] {0.86f, 0.72f, 1f} : new float[] {0.62f, 0.98f, 1f};
            float body = len * 0.72f;
            for (int k = 0; k < 6; k++) {
                double a0 = k * Math.PI / 3, a1 = (k + 1) * Math.PI / 3, am = (a0 + a1) / 2;
                float p0x = (float) Math.cos(a0) * ux + (float) Math.sin(a0) * vx, p0y = (float) Math.cos(a0) * uy + (float) Math.sin(a0) * vy, p0z = (float) Math.cos(a0) * uz + (float) Math.sin(a0) * vz;
                float p1x = (float) Math.cos(a1) * ux + (float) Math.sin(a1) * vx, p1y = (float) Math.cos(a1) * uy + (float) Math.sin(a1) * vy, p1z = (float) Math.cos(a1) * uz + (float) Math.sin(a1) * vz;
                float nx = (float) Math.cos(am) * ux + (float) Math.sin(am) * vx, ny = (float) Math.cos(am) * uy + (float) Math.sin(am) * vy, nz = (float) Math.cos(am) * uz + (float) Math.sin(am) * vz;
                float sh = 0.7f + 0.3f * (k % 2);
                float tx0 = bx + dx * body, ty0 = y + dy * body, tz0 = bz + dz * body;
                b.colorf(c0[0] * sh, c0[1] * sh, c0[2] * sh);
                b.vert(bx + p0x * rad, y + p0y * rad, bz + p0z * rad, nx, ny, nz, 0, 0);
                b.vert(bx + p1x * rad, y + p1y * rad, bz + p1z * rad, nx, ny, nz, 0, 0);
                b.colorf(c1[0] * sh, c1[1] * sh, c1[2] * sh);
                b.vert(tx0 + p1x * rad, ty0 + p1y * rad, tz0 + p1z * rad, nx, ny, nz, 0, 0);
                b.colorf(c0[0] * sh, c0[1] * sh, c0[2] * sh);
                b.vert(bx + p0x * rad, y + p0y * rad, bz + p0z * rad, nx, ny, nz, 0, 0);
                b.colorf(c1[0] * sh, c1[1] * sh, c1[2] * sh);
                b.vert(tx0 + p1x * rad, ty0 + p1y * rad, tz0 + p1z * rad, nx, ny, nz, 0, 0);
                b.vert(tx0 + p0x * rad, ty0 + p0y * rad, tz0 + p0z * rad, nx, ny, nz, 0, 0);
                // punta
                b.vert(tx0 + p0x * rad, ty0 + p0y * rad, tz0 + p0z * rad, nx, ny + 0.4f, nz, 0, 0);
                b.vert(tx0 + p1x * rad, ty0 + p1y * rad, tz0 + p1z * rad, nx, ny + 0.4f, nz, 0, 0);
                b.colorf(1f, 1f, 1f);
                b.vert(bx + dx * len, y + dy * len, bz + dz * len, nx, ny + 0.4f, nz, 0, 0);
            }
        }
    }

    // ------------------------------------------------------------------ disposizione

    void build(World3D w, int z0, int z1) {
        Random r = new Random(77);
        float half = World3D.HALF;
        for (int side = -1; side <= 1; side += 2) {
            for (float z = z0 + 2; z < z1; z += 4f + r.nextFloat() * 4f) {
                float x = side * (half + 0.5f + r.nextFloat() * 4.2f);
                mushrooms(x, z + r.nextFloat(), r);
            }
            for (float z = z0 + 6 + (side > 0 ? 7 : 0); z < z1; z += 15f + r.nextFloat() * 12f) {
                float rad = 0.2f + 0.14f * r.nextFloat();
                log(side * (half + 1.6f + r.nextFloat() * 2.4f), z, 2.2f + 1.6f * r.nextFloat(), rad, r.nextFloat() * 3.14f, r);
            }
            for (float z = z0 + 12 + (side > 0 ? 0 : 9); z < z1; z += 19f + r.nextFloat() * 14f) {
                stump(side * (half + 1.2f + r.nextFloat() * 3f), z, 0.26f + 0.16f * r.nextFloat(), 0.35f + 0.3f * r.nextFloat(), r);
            }
            if (s.aurora) {
                for (float z = z0; z < z1; z += 3.2f + r.nextFloat() * 3.5f) {
                    float x = side * (half + 0.4f + r.nextFloat() * 5f), rad = 0.5f + 0.7f * r.nextFloat();
                    snow(x, rest(x, z, rad) + rad * 0.05f, z, rad, rad * (0.3f + 0.2f * r.nextFloat()), rad * (0.7f + 0.4f * r.nextFloat()));
                }
                for (float z = z0 + 4; z < z1; z += 7f + r.nextFloat() * 7f) crystals(side * (half + 0.7f + r.nextFloat() * 3.6f), z, r);
            } else {
                for (float z = z0 + 3; z < z1; z += 6f + r.nextFloat() * 5f) {
                    bush(side * (half + 1.0f + r.nextFloat() * 3.8f), z, 0.42f + 0.3f * r.nextFloat(), r);
                }
            }
            if (s.autumn) {
                for (float z = z0; z < z1; z += 4.5f + r.nextFloat() * 4f) {
                    leafPile(side * (half + 0.2f + r.nextFloat() * 4.5f), z, 0.5f + 0.5f * r.nextFloat(), r);
                }
            }
        }
        // sassolini ai bordi del sentiero battuto
        for (float z = 0; z < w.length; z += 0.9f + r.nextFloat() * 1.2f) {
            float sd = r.nextBoolean() ? 1 : -1, x = Scene3D.trailX(z) + sd * (Scene3D.TRAIL_HW * (0.7f + 0.5f * r.nextFloat()));
            int xi = (int) Math.floor(x + half), zi = (int) Math.floor(z);
            if (xi < 0 || xi >= World3D.W || Scene3D.vis(w, xi, zi) != 0 || s.fadeAt(x, z) < 0.5f) continue;
            float rad = 0.05f + 0.07f * r.nextFloat();
            light(x, z);
            MeshBuilder rb = s.mb(Scene3D.K_ROCK, z);
            rb.colorf(l[0] * 1.05f, l[1] * 1.05f, l[2] * 1.05f);
            rb.rock(x, s.groundAt(x, z) + rad * 0.25f, z, rad * 1.3f, rad * 0.8f, rad, (int) (z * 17), 6, 4);
        }
    }
}
