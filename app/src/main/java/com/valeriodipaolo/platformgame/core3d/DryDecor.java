package com.valeriodipaolo.platformgame.core3d;

import java.util.Random;

/**
 * Arredo del deserto e della fabbrica (livelli 38 e 39), al livello degli altri mondi.
 * <ul>
 * <li>Deserto: saguari a coste con bracci curvi, fichi d'India, cactus a barile fioriti, cespugli secchi, alberi morti, teschi e
 * costole sbiancate, rocce di arenaria stratificata e, lontano, mesa a gradoni.</li>
 * <li>Fabbrica: passerelle con corrimano gialli, portali di travi d'acciaio con lampade industriali, serbatoi con cupola, scala e
 * valvola, ciminiere di mattoni ad anelli, macchinari con quadri comandi luminosi, tubazioni con flange e gomiti, capannoni sullo
 * sfondo con le finestre accese.</li>
 * </ul>
 */
final class DryDecor {
    /** Metà larghezza del sentiero del livello che si sta costruendo (la imposta Scene3D). */
    static float half = World3D.HALF5;

    private DryDecor() {}

    private static final float H = half;

    // ------------------------------------------------------------------ deserto

    static void desert(Scene3D s, World3D w, int z0, int z1, Random r) {
        for (int side = -1; side <= 1; side += 2) {
            for (float z = z0 + r.nextFloat() * 2f; z < z1; z += 1.2f + 2.2f * r.nextFloat()) {
                float d = 0.8f + 11f * r.nextFloat() * r.nextFloat(), x = side * (H + d), y = s.groundAt(x, z);
                s.lightAt(x, z, 1f);
                float k = r.nextFloat();
                if (k < 0.22f) saguaro(s.mb(Scene3D.K_PROP, z), s.lt, x, y, z, 1.6f + 2.2f * r.nextFloat(), r);
                else if (k < 0.34f) opuntia(s.mb(Scene3D.K_PROP, z), s.lt, x, y, z, r);
                else if (k < 0.44f) barrel(s.mb(Scene3D.K_PROP, z), s.lt, x, y, z, 0.3f + 0.2f * r.nextFloat(), r);
                else if (k < 0.64f) shrub(s.mb(Scene3D.K_PROP, z), s.lt, x, y, z, 0.35f + 0.35f * r.nextFloat(), r);
                else if (k < 0.7f && d > 2f) deadTree(s.mb(Scene3D.K_TRUNK, z), s.lt, x, y, z, 1.6f + 1.4f * r.nextFloat(), r);
                else if (k < 0.74f) skull(s.mb(Scene3D.K_PROP, z), s.lt, x, y, z, r);
                else if (k < 0.77f && d > 1.5f) ribs(s.mb(Scene3D.K_PROP, z), s.lt, x, y, z, r);
                else sandstone(s.mb(Scene3D.K_ROCK, z), s.lt, x, y, z, 0.3f + 0.7f * r.nextFloat(), r);
            }
        }
        // mesa e torrioni di arenaria lontani, a gradoni rossi e ocra
        for (int side = -1; side <= 1; side += 2) for (float z = z0 + 10f * r.nextFloat(); z < z1; z += 18f + 22f * r.nextFloat()) {
            float x = side * (H + 18f + 14f * r.nextFloat());
            mesa(s.mb(Scene3D.K_ROCK, z), x, s.groundAt(x, z) - 0.5f, z, 3f + 4f * r.nextFloat(), 4f + 6f * r.nextFloat(), r);
        }
    }

    /** Saguaro: fusto a coste (sezione stellata), bracci che escono di lato e salgono curvi, punte arrotondate, spine chiare. */
    static void saguaro(MeshBuilder b, float[] lt, float x, float y, float z, float h, Random r) {
        float rad = 0.17f + 0.05f * h / 3f;
        ribbed(b, lt, x, y - 0.1f, z, x, y + h, z, rad, rad * 0.88f);
        int arms = r.nextFloat() < 0.2f ? 0 : 1 + r.nextInt(2);
        float a0 = r.nextFloat() * 6.283f;
        for (int i = 0; i < arms; i++) {
            float an = a0 + i * 3.1416f + (r.nextFloat() - 0.5f) * 0.8f, ca = (float) Math.cos(an), sa = (float) Math.sin(an);
            float hy = y + h * (0.35f + 0.25f * r.nextFloat()), out = 0.45f + 0.25f * r.nextFloat(), up = h * (0.3f + 0.25f * r.nextFloat());
            float ar = rad * 0.7f;
            ribbed(b, lt, x + ca * rad * 0.6f, hy, z + sa * rad * 0.6f, x + ca * out, hy + 0.12f, z + sa * out, ar, ar);
            ribbed(b, lt, x + ca * out, hy + 0.05f, z + sa * out, x + ca * (out + 0.05f), hy + up, z + sa * (out + 0.05f), ar, ar * 0.85f);
        }
    }

    /** Tronco a coste tra due punti: dodici coste verdi scure con i solchi più scuri e una calotta in cima. */
    private static void ribbed(MeshBuilder b, float[] lt, float x0, float y0, float z0, float x1, float y1, float z1, float r0, float r1) {
        float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0, l = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        dx /= l; dy /= l; dz /= l;
        float ux = -dz, uz = dx, ul = (float) Math.sqrt(ux * ux + uz * uz);
        float uy = 0;
        if (ul < 1e-3f) { ux = 1; uz = 0; ul = 1; }
        ux /= ul; uz /= ul;
        float vx = dy * uz - dz * uy, vy = dz * ux - dx * uz, vz = dx * uy - dy * ux;
        int n = 24, seg = Math.max(2, (int) (l / 0.35f));
        for (int sgi = 0; sgi < seg; sgi++) {
            float t0 = sgi / (float) seg, t1 = (sgi + 1) / (float) seg;
            float ra = r0 + (r1 - r0) * t0, rb = r0 + (r1 - r0) * t1;
            for (int i = 0; i < n; i++) {
                double a0 = i * 2 * Math.PI / n, a1 = (i + 1) * 2 * Math.PI / n;
                float k0 = i % 2 == 0 ? 1f : 0.84f, k1 = i % 2 == 0 ? 0.84f : 1f;          // costa e solco alternati
                float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
                float tone = i % 2 == 0 ? 1f : 0.7f;
                b.colorf(0.22f * tone * lt[0] * 1.6f, 0.4f * tone * lt[1] * 1.5f, 0.2f * tone * lt[2] * 1.5f);
                float nx = (c0 + c1) * 0.5f * ux + (s0 + s1) * 0.5f * vx, ny = (s0 + s1) * 0.5f * vy, nz = (c0 + c1) * 0.5f * uz + (s0 + s1) * 0.5f * vz;
                b.quad(x0 + dx * l * t0 + (c0 * ux + s0 * vx) * ra * k0, y0 + dy * l * t0 + s0 * vy * ra * k0, z0 + dz * l * t0 + (c0 * uz + s0 * vz) * ra * k0,
                        x0 + dx * l * t0 + (c1 * ux + s1 * vx) * ra * k1, y0 + dy * l * t0 + s1 * vy * ra * k1, z0 + dz * l * t0 + (c1 * uz + s1 * vz) * ra * k1,
                        x0 + dx * l * t1 + (c1 * ux + s1 * vx) * rb * k1, y0 + dy * l * t1 + s1 * vy * rb * k1, z0 + dz * l * t1 + (c1 * uz + s1 * vz) * rb * k1,
                        x0 + dx * l * t1 + (c0 * ux + s0 * vx) * rb * k0, y0 + dy * l * t1 + s0 * vy * rb * k0, z0 + dz * l * t1 + (c0 * uz + s0 * vz) * rb * k0,
                        nx, ny, nz, 0, 0, 1, 1);
            }
        }
        b.colorf(0.26f * lt[0] * 1.6f, 0.44f * lt[1] * 1.5f, 0.22f * lt[2] * 1.5f);
        b.ellipsoid(x1, y1, z1, r1 * 0.95f, r1 * 0.75f, r1 * 0.95f, 12, 6);
        b.colorf(0.95f * lt[0] * 1.3f, 0.9f * lt[1] * 1.3f, 0.75f * lt[2] * 1.3f);                  // areole di spine chiare lungo le coste
        for (int sgi = 1; sgi < seg * 2; sgi++) for (int i = 0; i < n; i += 4) {
            float t = sgi / (float) (seg * 2), rr = (r0 + (r1 - r0) * t) * 1.02f;
            double a = i * 2 * Math.PI / n;
            float c = (float) Math.cos(a), sn = (float) Math.sin(a);
            b.box(x0 + dx * l * t + (c * ux + sn * vx) * rr, y0 + dy * l * t + sn * vy * rr, z0 + dz * l * t + (c * uz + sn * vz) * rr, 0.018f, 0.018f, 0.018f);
        }
    }

    /** Fico d'India: pale ovali piatte che si impilano storte, con qualche frutto rosso sul bordo. */
    static void opuntia(MeshBuilder b, float[] lt, float x, float y, float z, Random r) {
        int pads = 4 + r.nextInt(5);
        float[][] p = new float[pads][];
        p[0] = new float[] {x, y + 0.25f, z, r.nextFloat() * 3.14f};
        for (int i = 1; i < pads; i++) {
            float[] q = p[r.nextInt(i)];
            float an = q[3] + (r.nextFloat() - 0.5f) * 1.6f;
            p[i] = new float[] {q[0] + (float) Math.cos(an) * 0.18f, q[1] + 0.28f + 0.06f * r.nextFloat(), q[2] + (float) Math.sin(an) * 0.18f, an + 1.2f * (r.nextFloat() - 0.5f)};
        }
        for (float[] q : p) {
            float tone = 0.85f + 0.3f * r.nextFloat();
            b.colorf(0.3f * tone * lt[0] * 1.6f, 0.52f * tone * lt[1] * 1.5f, 0.24f * tone * lt[2] * 1.5f);
            float ca = (float) Math.cos(q[3]), sa = (float) Math.sin(q[3]);
            int mk = b.mark();
            b.ellipsoid(0, 0, 0, 0.2f, 0.26f, 0.05f, 12, 8);
            rotateY(b, mk, q[3], q[0], q[1], q[2]);
            if (r.nextFloat() < 0.4f) {
                b.colorf(0.8f * lt[0] * 1.4f, 0.15f * lt[1], 0.3f * lt[2] * 1.2f);
                b.ellipsoid(q[0] + ca * 0.12f, q[1] + 0.25f, q[2] + sa * 0.12f, 0.045f, 0.06f, 0.045f, 6, 4);
            }
        }
    }

    /** Cactus a barile: palla a coste schiacciata, corona di fiori gialli in cima. */
    static void barrel(MeshBuilder b, float[] lt, float x, float y, float z, float rad, Random r) {
        int n = 20;
        for (int i = 0; i < n; i++) {
            double a0 = i * 2 * Math.PI / n, a1 = (i + 1) * 2 * Math.PI / n;
            float tone = i % 2 == 0 ? 1f : 0.72f;
            b.colorf(0.28f * tone * lt[0] * 1.6f, 0.46f * tone * lt[1] * 1.5f, 0.2f * tone * lt[2] * 1.5f);
            for (int j = 0; j < 6; j++) {
                double e0 = -0.3 + j * Math.PI * 0.85 / 6 - Math.PI / 2 + 0.6, e1 = -0.3 + (j + 1) * Math.PI * 0.85 / 6 - Math.PI / 2 + 0.6;
                float k0 = i % 2 == 0 ? 1f : 0.9f;
                float[] p00 = sph(x, y + rad * 0.8f, z, rad * k0, rad * 0.85f, a0, e0), p10 = sph(x, y + rad * 0.8f, z, rad * k0, rad * 0.85f, a1, e0);
                float[] p11 = sph(x, y + rad * 0.8f, z, rad * k0, rad * 0.85f, a1, e1), p01 = sph(x, y + rad * 0.8f, z, rad * k0, rad * 0.85f, a0, e1);
                float nx = (float) Math.cos((a0 + a1) / 2), nz = (float) Math.sin((a0 + a1) / 2);
                b.quad(p00[0], p00[1], p00[2], p10[0], p10[1], p10[2], p11[0], p11[1], p11[2], p01[0], p01[1], p01[2], nx, 0.3f, nz, 0, 0, 1, 1);
            }
        }
        b.colorf(1f * lt[0] * 1.4f, 0.82f * lt[1] * 1.4f, 0.2f * lt[2]);
        for (int i = 0; i < 7; i++) {
            double a = i * 0.9;
            b.ellipsoid(x + (float) Math.cos(a) * rad * 0.35f, y + rad * 1.55f, z + (float) Math.sin(a) * rad * 0.35f, 0.05f, 0.03f, 0.05f, 6, 3);
        }
    }

    private static float[] sph(float cx, float cy, float cz, float rh, float rv, double a, double e) {
        return new float[] {cx + rh * (float) (Math.cos(e) * Math.cos(a)), cy + rv * (float) Math.sin(e), cz + rh * (float) (Math.cos(e) * Math.sin(a))};
    }

    /** Cespuglio secco: tanti rametti sottili grigio-bruni che partono da terra a ventaglio. */
    static void shrub(MeshBuilder b, float[] lt, float x, float y, float z, float h, Random r) {
        int n = 14 + r.nextInt(8);
        for (int i = 0; i < n; i++) {
            float an = r.nextFloat() * 6.283f, tilt = 0.3f + 0.7f * r.nextFloat(), l = h * (0.6f + 0.6f * r.nextFloat());
            float ex = x + (float) Math.cos(an) * tilt * l * 0.7f, ez = z + (float) Math.sin(an) * tilt * l * 0.7f, ey = y + l * (1.1f - tilt * 0.5f);
            float tone = 0.8f + 0.4f * r.nextFloat();
            b.colorf(0.45f * tone * lt[0] * 1.3f, 0.36f * tone * lt[1] * 1.3f, 0.26f * tone * lt[2] * 1.3f);
            MineModels.beamY(b, x, y - 0.05f, z, ex, ey, ez, 0.012f);
            if (r.nextFloat() < 0.6f) {                                 // rametto secondario
                float mx = (x + ex) / 2, my = (y + ey) / 2, mz = (z + ez) / 2;
                MineModels.beamY(b, mx, my, mz, mx + (r.nextFloat() - 0.5f) * 0.3f, my + 0.15f + 0.1f * r.nextFloat(), mz + (r.nextFloat() - 0.5f) * 0.3f, 0.008f);
            }
        }
    }

    /** Albero morto: tronco contorto sbiancato dal sole, tre o quattro rami spogli. */
    static void deadTree(MeshBuilder b, float[] lt, float x, float y, float z, float h, Random r) {
        float[] bark = {0.62f * lt[0] * 1.3f, 0.55f * lt[1] * 1.3f, 0.46f * lt[2] * 1.3f};
        float lx = (r.nextFloat() - 0.5f) * 0.6f, lz = (r.nextFloat() - 0.5f) * 0.6f;
        FoeModels.tube(b, new float[][] {{x, y - 0.1f, z}, {x + lx * 0.3f, y + h * 0.5f, z + lz * 0.3f}, {x + lx, y + h, z + lz}}, new float[] {0.13f, 0.09f, 0.05f}, 7, FoeModels.flat(bark));
        int br = 3 + r.nextInt(2);
        for (int i = 0; i < br; i++) {
            float t = 0.45f + 0.45f * r.nextFloat(), an = r.nextFloat() * 6.283f, l = h * (0.3f + 0.25f * r.nextFloat());
            float bx = x + lx * t * t, by = y + h * t, bz = z + lz * t * t;
            float ex = bx + (float) Math.cos(an) * l, ez = bz + (float) Math.sin(an) * l, ey = by + l * (0.4f + 0.5f * r.nextFloat());
            FoeModels.tube(b, new float[][] {{bx, by, bz}, {(bx + ex) / 2, (by + ey) / 2 + 0.1f, (bz + ez) / 2}, {ex, ey, ez}}, new float[] {0.05f, 0.035f, 0.015f}, 5, FoeModels.flat(bark));
        }
    }

    /** Teschio di bovino sbiancato, mezzo affondato, con le corna. */
    static void skull(MeshBuilder b, float[] lt, float x, float y, float z, Random r) {
        float yaw = r.nextFloat() * 6.283f;
        int mk = b.mark();
        b.colorf(0.92f * lt[0] * 1.3f, 0.88f * lt[1] * 1.3f, 0.8f * lt[2] * 1.3f);
        b.ellipsoid(0, 0.1f, 0, 0.16f, 0.12f, 0.2f, 10, 7);
        b.ellipsoid(0, 0.07f, 0.2f, 0.09f, 0.07f, 0.12f, 8, 5);
        b.colorf(0.15f * lt[0], 0.12f * lt[1], 0.1f * lt[2]);
        for (int s2 = -1; s2 <= 1; s2 += 2) b.ellipsoid(s2 * 0.07f, 0.15f, 0.13f, 0.035f, 0.03f, 0.03f, 6, 4);
        b.colorf(0.85f * lt[0] * 1.3f, 0.8f * lt[1] * 1.3f, 0.68f * lt[2] * 1.3f);
        for (int s2 = -1; s2 <= 1; s2 += 2) FoeModels.tube(b, new float[][] {{s2 * 0.13f, 0.17f, -0.05f}, {s2 * 0.3f, 0.2f, -0.08f}, {s2 * 0.38f, 0.33f, -0.02f}}, new float[] {0.035f, 0.025f, 0.008f}, 6,
                FoeModels.flat(new float[] {0.85f * lt[0] * 1.3f, 0.8f * lt[1] * 1.3f, 0.68f * lt[2] * 1.3f}));
        rotateY(b, mk, yaw, x, y - 0.05f, z);
    }

    /** Costole sbiancate che spuntano dalla sabbia ad archi. */
    static void ribs(MeshBuilder b, float[] lt, float x, float y, float z, Random r) {
        float yaw = r.nextFloat() * 6.283f;
        int mk = b.mark();
        float[] bone = {0.9f * lt[0] * 1.3f, 0.86f * lt[1] * 1.3f, 0.76f * lt[2] * 1.3f};
        FoeModels.tube(b, new float[][] {{0, 0.02f, -0.5f}, {0, 0.06f, 0}, {0, 0.03f, 0.5f}}, new float[] {0.04f, 0.045f, 0.035f}, 6, FoeModels.flat(bone));
        for (int i = 0; i < 6; i++) {
            float zz = -0.4f + i * 0.16f, hh = 0.35f + 0.1f * (float) Math.sin(i);
            for (int s2 = -1; s2 <= 1; s2 += 2)
                FoeModels.tube(b, new float[][] {{0, 0.06f, zz}, {s2 * 0.25f, hh, zz}, {s2 * 0.38f, 0.0f, zz + 0.05f}}, new float[] {0.025f, 0.02f, 0.012f}, 5, FoeModels.flat(bone));
        }
        rotateY(b, mk, yaw, x, y - 0.06f, z);
    }

    /** Masso di arenaria: blocco irregolare a strati orizzontali rossi e ocra. */
    static void sandstone(MeshBuilder b, float[] lt, float x, float y, float z, float sz, Random r) {
        int layers = 2 + r.nextInt(3);
        float yy = y - 0.15f;
        for (int i = 0; i < layers; i++) {
            float lh = sz * (0.35f + 0.25f * r.nextFloat()), shrink = 1f - i * 0.18f;
            float tone = i % 2 == 0 ? 1f : 0.82f;
            b.colorf(1.15f * tone * lt[0], 0.78f * tone * lt[1], 0.55f * tone * lt[2]);
            b.rock(x + (r.nextFloat() - 0.5f) * 0.1f * sz, yy + lh * 0.5f, z, sz * shrink * (0.9f + 0.3f * r.nextFloat()), lh * 0.6f, sz * shrink * (0.8f + 0.3f * r.nextFloat()), r.nextInt(9999), 9, 5);
            yy += lh * 0.8f;
        }
    }

    /** Mesa a gradoni lontana: tre o quattro piani di roccia stratificata, il più alto piatto. */
    static void mesa(MeshBuilder b, float x, float y, float z, float w, float h, Random r) {
        int steps = 3 + r.nextInt(2);
        float yy = y;
        for (int i = 0; i < steps; i++) {
            float sh = h / steps * (i == steps - 1 ? 0.7f : 1f), ww = w * (1f - i * 0.16f);
            float tone = i % 2 == 0 ? 1f : 0.86f;
            b.colorf(1.25f * tone, 0.82f * tone, 0.6f * tone);
            b.rock(x, yy + sh * 0.45f, z, ww, sh * 0.62f, ww * 0.85f, (int) (x * 13 + z * 7 + i * 31), 16, 9);
            yy += sh * 0.85f;
        }
    }

    // ------------------------------------------------------------------ fabbrica

    /** Ciminiere costruite: i loro comignoli, per il fumo che sale (x, y in cima, z). */
    static void factory(Scene3D s, World3D w, int z0, int z1, Random r, java.util.List<float[]> stacks) {
        for (int side = -1; side <= 1; side += 2) {
            // passerella con corrimano giallo: ritti ogni metro, due correnti, zoccolo con strisce di pericolo
            for (int z = z0; z < z1; z++) {
                float x = side * (H + 0.95f), y = s.groundAt(x, z);
                s.lightAt(x, z, 1f);
                MeshBuilder p = s.mb(Scene3D.K_PROP, z);
                p.colorf(0.95f * s.lt[0] * 1.4f, 0.75f * s.lt[1] * 1.4f, 0.1f * s.lt[2]);
                p.box(x, y + 0.55f, z, 0.05f, 1.1f, 0.05f);
                p.box(x, y + 1.08f, z + 0.5f, 0.05f, 0.05f, 1.0f);
                p.box(x, y + 0.6f, z + 0.5f, 0.04f, 0.04f, 1.0f);
                p.colorf((z % 2 == 0 ? 0.95f : 0.12f) * s.lt[0] * 1.3f, (z % 2 == 0 ? 0.78f : 0.12f) * s.lt[1] * 1.3f, (z % 2 == 0 ? 0.1f : 0.12f) * s.lt[2]);
                p.box(x, y + 0.06f, z + 0.5f, 0.06f, 0.12f, 1.0f);
            }
            // tubazioni lungo il muro di fondo, a tre altezze, con flange e gomiti che scendono a terra
            for (int lane = 0; lane < 3; lane++) {
                float px = side * (H + 3.6f + lane * 0.45f), py = 1.0f + lane * 0.9f;
                float[] c = lane == 0 ? new float[] {0.75f, 0.2f, 0.15f} : lane == 1 ? new float[] {0.3f, 0.5f, 0.65f} : new float[] {0.55f, 0.55f, 0.52f};
                for (int z = z0; z < z1; z += 4) {
                    MeshBuilder p = s.mb(Scene3D.K_PROP, z);
                    float gy = s.groundAt(px, z) + py;
                    s.lightAt(px, z, 1f);
                    p.colorf(c[0] * s.lt[0] * 1.5f, c[1] * s.lt[1] * 1.5f, c[2] * s.lt[2] * 1.5f);
                    MineModels.cylZ(p, px, gy, z, z + 4.02f, 0.13f - lane * 0.02f);
                    p.colorf(0.35f * s.lt[0], 0.35f * s.lt[1], 0.37f * s.lt[2]);
                    MineModels.cylZ(p, px, gy, z - 0.04f, z + 0.04f, 0.18f - lane * 0.02f);       // flangia
                    if ((z / 4 + lane) % 5 == 0) {                                            // gomito che scende a terra con valvola
                        p.colorf(c[0] * s.lt[0] * 1.5f, c[1] * s.lt[1] * 1.5f, c[2] * s.lt[2] * 1.5f);
                        p.cylinder(px, s.groundAt(px, z + 2), gy, z + 2f, 0.1f, 0.1f, 10, false, false);
                        p.colorf(0.8f * s.lt[0] * 1.4f, 0.1f * s.lt[1], 0.08f * s.lt[2]);
                        p.box(px - side * 0.16f, gy * 0.5f, z + 2f, 0.04f, 0.04f, 0.3f);
                        p.box(px - side * 0.16f, gy * 0.5f, z + 2f, 0.04f, 0.3f, 0.04f);
                    }
                }
            }
            // macchinari, serbatoi e ciminiere oltre la passerella
            for (float z = z0 + r.nextFloat() * 4f; z < z1; z += 5f + 6f * r.nextFloat()) {
                float k = r.nextFloat();
                if (k < 0.4f) tank(s, side * (H + 6.2f + 1.5f * r.nextFloat()), z, 0.9f + 0.6f * r.nextFloat(), 2.2f + 2.2f * r.nextFloat(), r);
                else if (k < 0.75f) machine(s, side * (H + 2.4f), z, side, r);
                else {
                    float x = side * (H + 7.5f + 3f * r.nextFloat()), y = s.groundAt(x, z), hh = 7f + 5f * r.nextFloat();
                    chimney(s.mb(Scene3D.K_PROP, z), s, x, y, z, hh);
                    if (stacks != null) stacks.add(new float[] {x, y + hh, z});
                    z += 4f;
                }
            }
            // capannoni sullo sfondo: facciate di pannelli con file di finestre accese e tetti a shed
            for (float z = z0; z < z1; z += 12f) {
                float x = side * (H + 14f), y = s.groundAt(x, z) - 0.2f, hh = 6f + 3f * ((int) (z / 12) % 3);
                MeshBuilder st = s.mb(Scene3D.K_DIRT, z);                                    // pannelli di cemento
                st.colorf(0.82f, 0.8f, 0.78f);
                st.box(x + side * 3f, y + hh / 2, z + 6f, 6f, hh, 11.6f);
                for (int t = 0; t < 4; t++) {                                             // tetto a denti di sega
                    float tz = z + 1.5f + t * 2.9f;
                    st.colorf(0.55f, 0.55f, 0.58f);
                    st.quad(x, y + hh, tz - 1.45f, x + side * 6f, y + hh, tz - 1.45f, x + side * 6f, y + hh + 1.2f, tz + 1.2f, x, y + hh + 1.2f, tz + 1.2f, 0, 0.7f, -0.7f, 0, 0, 1, 1);
                }
                MeshBuilder gl = s.mb(Scene3D.K_GLOW, z);
                for (int row = 0; row < 2; row++) for (int c = 0; c < 6; c++) {
                    if (Math.floorMod((int) z * 7 + row * 3 + c * 5, 7) == 0) continue;                  // qualche finestra spenta
                    float wy = y + 1.6f + row * 2.2f, wz = z + 1.2f + c * 1.8f;
                    gl.colorf(1f, 0.82f, 0.45f);
                    gl.quad(x - side * 0.02f, wy, wz, x - side * 0.02f, wy, wz + 1.0f, x - side * 0.02f, wy + 1.2f, wz + 1.0f, x - side * 0.02f, wy + 1.2f, wz, -side, 0, 0, 0, 0, 1, 1);
                }
            }
        }
        // portali di travi d'acciaio a doppia T sopra il corridoio, con lampade industriali appese
        for (int z = z0 + ((3 - z0) % 10 + 10) % 10; z < z1; z += 10) {
            MeshBuilder p = s.mb(Scene3D.K_PROP, z);
            s.lightAt(0, z, 1f);
            float hx = H + 1.3f, top = 5.6f;
            for (int side = -1; side <= 1; side += 2) ibeam(p, s.lt, side * hx, s.groundAt(side * hx, z), z, side * hx, top, z, true);
            ibeam(p, s.lt, -hx - 0.2f, top, z, hx + 0.2f, top, z, false);
            for (int side = -1; side <= 1; side += 2) {                                   // lampada a campana con la luce accesa
                float lx = side * 1.6f;
                p.colorf(0.2f, 0.2f, 0.22f);
                p.box(lx, top - 0.5f, z, 0.02f, 0.9f, 0.02f);
                p.colorf(0.3f * s.lt[0], 0.45f * s.lt[1], 0.35f * s.lt[2]);
                p.cylinder(lx, top - 1.05f, top - 0.9f, z, 0.35f, 0.08f, 12, true, false);
                MeshBuilder gl = s.mb(Scene3D.K_GLOW, z);
                gl.colorf(1f, 0.95f, 0.78f);
                gl.ellipsoid(lx, top - 1.07f, z, 0.12f, 0.06f, 0.12f, 8, 4);
            }
        }
    }

    /** Trave a doppia T: anima e due ali; verticale (ritto) oppure orizzontale lungo x. */
    private static void ibeam(MeshBuilder p, float[] lt, float x0, float y0, float z, float x1, float y1, float z1, boolean vertical) {
        p.colorf(0.95f * lt[0] * 1.3f, 0.6f * lt[1] * 1.3f, 0.1f * lt[2]);
        if (vertical) {
            float h = y1 - y0, cy = (y0 + y1) / 2;
            p.box(x0, cy, z, 0.06f, h, 0.3f);
            p.box(x0 - 0.12f, cy, z, 0.04f, h, 0.34f);
            p.box(x0 + 0.12f, cy, z, 0.04f, h, 0.34f);
        } else {
            float l = x1 - x0, cx = (x0 + x1) / 2;
            p.box(cx, y0, z, l, 0.3f, 0.06f);
            p.box(cx, y0 + 0.15f, z, l, 0.04f, 0.3f);
            p.box(cx, y0 - 0.15f, z, l, 0.04f, 0.3f);
        }
    }

    /** Serbatoio: corpo cilindrico con fasce, cupola, scala alla marinara, tubo e targa colorata. */
    static void tank(Scene3D s, float x, float z, float rad, float h, Random r) {
        float y = s.groundAt(x, z);
        s.lightAt(x, z, 1f);
        MeshBuilder p = s.mb(Scene3D.K_PROP, z);
        float[] lt = s.lt;
        float tone = 0.85f + 0.3f * r.nextFloat();
        p.colorf(0.62f * tone * lt[0] * 1.3f, 0.65f * tone * lt[1] * 1.3f, 0.68f * tone * lt[2] * 1.3f);
        p.cylinder(x, y, y + h, z, rad, rad, 20, false, false);
        p.ellipsoid(x, y + h, z, rad, rad * 0.45f, rad, 16, 6);
        p.colorf(0.38f * lt[0], 0.4f * lt[1], 0.42f * lt[2]);
        for (float t = 0.2f; t < 1f; t += 0.27f) p.cylinder(x, y + h * t, y + h * t + 0.08f, z, rad + 0.02f, rad + 0.02f, 20, false, false);
        p.colorf(0.25f * lt[0], 0.25f * lt[1], 0.27f * lt[2]);
        p.cylinder(x, y, y + 0.25f, z, rad + 0.08f, rad + 0.08f, 20, true, false);        // basamento
        float[][] lbl = {{0.85f, 0.15f, 0.12f}, {0.15f, 0.45f, 0.8f}, {0.95f, 0.75f, 0.1f}};
        float[] c = lbl[r.nextInt(3)];
        p.colorf(c[0] * lt[0] * 1.4f, c[1] * lt[1] * 1.4f, c[2] * lt[2] * 1.4f);
        p.cylinder(x, y + h * 0.55f, y + h * 0.7f, z, rad + 0.015f, rad + 0.015f, 20, false, false);
        float sx = x - Math.signum(x) * (rad + 0.08f);                                     // scala verso il corridoio
        p.colorf(0.3f * lt[0], 0.3f * lt[1], 0.32f * lt[2]);
        p.box(sx, y + h / 2, z - 0.18f, 0.03f, h, 0.03f);
        p.box(sx, y + h / 2, z + 0.18f, 0.03f, h, 0.03f);
        for (float yy = y + 0.3f; yy < y + h; yy += 0.3f) p.box(sx, yy, z, 0.025f, 0.025f, 0.36f);
    }

    /** Ciminiera di mattoni: fusto rastremato con anelli scuri e la bocca annerita. */
    static void chimney(MeshBuilder p, Scene3D s, float x, float y, float z, float h) {
        s.lightAt(x, z, 1f);
        float[] lt = s.lt;
        int n = 8;
        for (int i = 0; i < n; i++) {
            float t0 = i / (float) n, t1 = (i + 1) / (float) n, tone = 0.9f + 0.15f * (i % 2);
            p.colorf(0.62f * tone * lt[0] * 1.3f, 0.3f * tone * lt[1] * 1.3f, 0.22f * tone * lt[2] * 1.3f);
            p.cylinder(x, y + h * t0, y + h * t1, z, 0.75f - 0.3f * t0, 0.75f - 0.3f * t1, 14, false, false);
            p.colorf(0.3f * lt[0], 0.2f * lt[1], 0.16f * lt[2]);
            p.cylinder(x, y + h * t1 - 0.06f, y + h * t1 + 0.06f, z, 0.77f - 0.3f * t1, 0.77f - 0.3f * t1, 14, false, false);
        }
        p.colorf(0.08f, 0.07f, 0.07f);
        p.cylinder(x, y + h, y + h + 0.35f, z, 0.5f, 0.52f, 14, true, false);
    }

    /** Macchinario: cassone a strisce di pericolo con un quadro comandi a lucine verdi, rosse e gialle. */
    static void machine(Scene3D s, float x, float z, int side, Random r) {
        float y = s.groundAt(x, z), w = 0.9f + 0.4f * r.nextFloat(), h = 1.0f + 0.8f * r.nextFloat();
        MeshBuilder st = s.mb(Scene3D.K_STONE, z);
        st.colorf(1f, 1f, 1f);
        st.roundBox(x + side * w * 0.5f, y + h / 2, z, w, h, 1.2f, 0.05f, 63, 2);
        s.lightAt(x, z, 1f);
        MeshBuilder p = s.mb(Scene3D.K_PROP, z);
        p.colorf(0.18f * s.lt[0], 0.2f * s.lt[1], 0.22f * s.lt[2]);
        p.box(x - side * 0.02f, y + h * 0.65f, z, 0.04f, h * 0.45f, 0.8f);                 // pannello
        MeshBuilder gl = s.mb(Scene3D.K_GLOW, z);
        float[][] cols = {{0.3f, 1f, 0.4f}, {1f, 0.25f, 0.2f}, {1f, 0.85f, 0.2f}};
        for (int i = 0; i < 6; i++) {
            float[] c = cols[Math.floorMod(i + (int) z, 3)];
            gl.colorf(c[0], c[1], c[2]);
            gl.box(x - side * 0.05f, y + h * 0.55f + (i / 3) * 0.18f, z - 0.25f + (i % 3) * 0.25f, 0.02f, 0.08f, 0.08f);
        }
    }

    /** Ruota attorno all'asse y (angolo a) i vertici aggiunti dopo mark, poi li sposta in (tx, ty, tz). */
    static void rotateY(MeshBuilder b, int mark, float a, float tx, float ty, float tz) {
        float c = (float) Math.cos(a), sn = (float) Math.sin(a);
        float[] d = b.d;
        for (int i = mark; i < b.n; i += Mesh.STRIDE) {
            float x = d[i], z = d[i + 2], nx = d[i + 3], nz = d[i + 5];
            d[i] = x * c - z * sn + tx; d[i + 1] += ty; d[i + 2] = x * sn + z * c + tz;
            d[i + 3] = nx * c - nz * sn; d[i + 5] = nx * sn + nz * c;
        }
    }
}
