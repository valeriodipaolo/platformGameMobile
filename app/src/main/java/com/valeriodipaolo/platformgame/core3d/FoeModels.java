package com.valeriodipaolo.platformgame.core3d;

import com.valeriodipaolo.platformgame.core.Img;

import java.util.Random;

/**
 * Modelli dei nemici 3D: scarabeo cervo, riccio, corvo e ghianda. Tutti guardano verso z positivo.
 *
 * I corpi sono modellati per sezioni (una pelle tesa lungo una linea, con raggi diversi a ogni sezione e la pancia
 * appiattita), hanno una texture (pelo, piumaggio, guscio, scaglie, corazza) colorata dai vertici, e gli arti sono
 * pezzi separati che si muovono: zampe, ali in due tratti, becco, mandibole, braccia.
 */
public final class FoeModels {
    /** Colore di un vertice: t = posizione lungo il pezzo (0..1), a = angolo attorno alla sezione (sin a > 0: dorso/fronte). */
    abstract static class Paint {
        abstract void col(float t, float a, float x, float y, float z, float[] o);
    }

    private static final float PI = (float) Math.PI;
    /** Coordinate di texture di partenza: servono ai pezzi che devono cadere in una zona precisa dell'immagine. */
    private static float U0, V0;

    private Mesh hogBody, hogSpines, hogGloss, hogBits, hogLeg;
    private Mesh crowBody, crowPlumes, crowBeak, crowJaw, crowBits, crowEyes;
    private final Mesh[] crowArm = new Mesh[2], crowHand = new Mesh[2];
    private Mesh acornBody, acornCap, acornBits, acornEyes, acornArm, acornFoot;
    private Mesh stormBody, stormEyes, stormBolt, whirlTier, whirlEyes;
    private Mesh beetleShell, beetleGloss, beetleEyes, beetleLeg, beetleJaw;
    private final float[] a = new float[16], b2 = new float[16], c2 = new float[16];
    private FoeModels2 more;

    public FoeModels() {
        buildHog();
        buildCrow();
        buildAcorn();
        buildBeetle();
        buildSky();
        more = new FoeModels2();
    }

    /** Palla di neve in volo. */
    public void drawShot(Gfx3D g, float[] m, float[] s) {
        if (s.length > 6 && s[6] == Enemy3.SKIN_TIKI) { if (island == null) island = new FoeIsland(); island.drawShot(g, m, s); return; }
        more.drawShot(g, m, s);
    }

    // ------------------------------------------------------------------ attrezzi

    static float[] rgb(int r, int g, int b) {
        return new float[] {r / 255f, g / 255f, b / 255f};
    }

    static float sm(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }

    static void mix(float[] p, float[] q, float t, float[] o) {
        o[0] = p[0] + (q[0] - p[0]) * t; o[1] = p[1] + (q[1] - p[1]) * t; o[2] = p[2] + (q[2] - p[2]) * t;
    }

    static Paint flat(final float[] c) {
        return new Paint() {
            @Override void col(float t, float a, float x, float y, float z, float[] o) { o[0] = c[0]; o[1] = c[1]; o[2] = c[2]; }
        };
    }

    /** Sfumatura dal ventre (bot) al dorso (top). */
    static Paint ud(final float[] bot, final float[] top) {
        return new Paint() {
            @Override void col(float t, float a, float x, float y, float z, float[] o) { mix(bot, top, sm(((float) Math.sin(a) + 0.5f) / 1.2f), o); }
        };
    }

    /** Sfumatura lungo il pezzo, dall'inizio (c0) alla fine (c1). */
    static Paint along(final float[] c0, final float[] c1) {
        return new Paint() {
            @Override void col(float t, float a, float x, float y, float z, float[] o) { mix(c0, c1, t, o); }
        };
    }

    /**
     * Pelle tesa lungo una linea: a ogni punto c[i] una sezione ellittica di raggi rx[i] (di lato) e ry[i] (verso "su").
     * belly schiaccia la metà inferiore (1 = sezione piena). Le normali sono calcolate dalla forma, quindi morbide.
     */
    static void loft(MeshBuilder b, float[][] c, float[] rx, float[] ry, float ux, float uy, float uz, int segs, float belly,
                     Paint p, float uRep, float vRep) {
        int n = c.length;
        float[][][] P = new float[n][segs + 1][3], C = new float[n][segs + 1][3];
        float[][] T = new float[n][3];
        float[] vv = new float[n];
        float len = 0;
        for (int i = 0; i < n; i++) {
            float[] p0 = c[Math.max(i - 1, 0)], p1 = c[Math.min(i + 1, n - 1)];
            float tx = p1[0] - p0[0], ty = p1[1] - p0[1], tz = p1[2] - p0[2];
            float tl = (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
            tx /= tl; ty /= tl; tz /= tl;
            T[i][0] = tx; T[i][1] = ty; T[i][2] = tz;
            float sx = uy * tz - uz * ty, sy = uz * tx - ux * tz, sz = ux * ty - uy * tx;
            float sl = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
            if (sl < 1e-4f) { sx = 1; sy = 0; sz = 0; sl = 1; }
            sx /= sl; sy /= sl; sz /= sl;
            float wx = ty * sz - tz * sy, wy = tz * sx - tx * sz, wz = tx * sy - ty * sx;
            if (i > 0) {
                float dx = c[i][0] - c[i - 1][0], dy = c[i][1] - c[i - 1][1], dz = c[i][2] - c[i - 1][2];
                len += (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            }
            vv[i] = len;
            for (int j = 0; j <= segs; j++) {
                float an = 2 * PI * (j % segs) / segs;
                float ca = (float) Math.cos(an), sa = (float) Math.sin(an);
                float k = sa < 0 ? belly : 1f;
                float[] q = P[i][j];
                q[0] = c[i][0] + sx * ca * rx[i] + wx * sa * ry[i] * k;
                q[1] = c[i][1] + sy * ca * rx[i] + wy * sa * ry[i] * k;
                q[2] = c[i][2] + sz * ca * rx[i] + wz * sa * ry[i] * k;
                p.col(n > 1 ? i / (float) (n - 1) : 0, an, q[0], q[1], q[2], C[i][j]);
            }
        }
        float[] nr = new float[3];
        float[][][] N = new float[n][segs + 1][3];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j <= segs; j++) {
                int jj = j % segs;
                float[] l = P[i][(jj + segs - 1) % segs], r = P[i][(jj + 1) % segs], d = P[Math.max(i - 1, 0)][jj], u = P[Math.min(i + 1, n - 1)][jj];
                float ax = r[0] - l[0], ay = r[1] - l[1], az = r[2] - l[2], bx = u[0] - d[0], by = u[1] - d[1], bz = u[2] - d[2];
                nr[0] = ay * bz - az * by; nr[1] = az * bx - ax * bz; nr[2] = ax * by - ay * bx;
                float nl = (float) Math.sqrt(nr[0] * nr[0] + nr[1] * nr[1] + nr[2] * nr[2]);
                float ox = P[i][jj][0] - c[i][0], oy = P[i][jj][1] - c[i][1], oz = P[i][jj][2] - c[i][2];
                float ol = (float) Math.sqrt(ox * ox + oy * oy + oz * oz);
                if (ol < 1e-5f) {                    // punta: la normale è lungo la linea
                    float s = i == 0 ? -1 : 1;
                    nr[0] = T[i][0] * s; nr[1] = T[i][1] * s; nr[2] = T[i][2] * s;
                } else if (nl < 1e-9f) {
                    nr[0] = ox / ol; nr[1] = oy / ol; nr[2] = oz / ol;
                } else {
                    nr[0] /= nl; nr[1] /= nl; nr[2] /= nl;
                    if (nr[0] * ox + nr[1] * oy + nr[2] * oz < 0) { nr[0] = -nr[0]; nr[1] = -nr[1]; nr[2] = -nr[2]; }
                }
                N[i][j][0] = nr[0]; N[i][j][1] = nr[1]; N[i][j][2] = nr[2];
            }
        }
        int[] oi = {0, 1, 1, 0, 1, 0}, oj = {0, 0, 1, 0, 1, 1};
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < segs; j++) {
                for (int k = 0; k < 6; k++) {
                    int ii = i + oi[k], jj = j + oj[k];
                    float[] q = P[ii][jj], cc = C[ii][jj], nn = N[ii][jj];
                    b.colorf(cc[0], cc[1], cc[2]);
                    b.vert(q[0], q[1], q[2], nn[0], nn[1], nn[2], U0 + jj / (float) segs * uRep, V0 + vv[ii] * vRep);
                }
            }
        }
    }

    /** Tubo a sezione tonda lungo una linea spezzata. */
    static void tube(MeshBuilder b, float[][] c, float[] r, int segs, Paint p) {
        boolean vertical = Math.abs(c[c.length - 1][1] - c[0][1]) > Math.abs(c[c.length - 1][0] - c[0][0]) + Math.abs(c[c.length - 1][2] - c[0][2]);
        loft(b, c, r, r, vertical ? 0 : 0, vertical ? 0 : 1, vertical ? 1 : 0, segs, 1f, p, 0, 0);
    }

    static void tube(MeshBuilder b, float x0, float y0, float z0, float x1, float y1, float z1, float r0, float r1, int segs, Paint p) {
        tube(b, new float[][] {{x0, y0, z0}, {(x0 + x1) / 2, (y0 + y1) / 2, (z0 + z1) / 2}, {x1, y1, z1}}, new float[] {r0, (r0 + r1) / 2, r1}, segs, p);
    }

    /** Ellissoide con i poli davanti e dietro (asse z). */
    static void ballZ(MeshBuilder b, float cx, float cy, float cz, float rx, float ry, float rz, int segs, int rings, Paint p, float uRep, float vRep) {
        float[][] c = new float[rings + 1][];
        float[] ax = new float[rings + 1], ay = new float[rings + 1];
        for (int i = 0; i <= rings; i++) {
            double t = Math.PI * i / rings;
            c[i] = new float[] {cx, cy, cz - rz * (float) Math.cos(t)};
            ax[i] = rx * (float) Math.sin(t); ay[i] = ry * (float) Math.sin(t);
        }
        ax[0] = ay[0] = ax[rings] = ay[rings] = 0;
        loft(b, c, ax, ay, 0, 1, 0, segs, 1f, p, uRep, vRep);
    }

    /** Ellissoide con i poli sotto e sopra (asse y); sin a > 0 è il davanti. */
    static void ballY(MeshBuilder b, float cx, float cy, float cz, float rx, float ry, float rz, int segs, int rings, Paint p, float uRep, float vRep) {
        float[][] c = new float[rings + 1][];
        float[] ax = new float[rings + 1], az = new float[rings + 1];
        for (int i = 0; i <= rings; i++) {
            double t = Math.PI * i / rings;
            c[i] = new float[] {cx, cy - ry * (float) Math.cos(t), cz};
            ax[i] = rx * (float) Math.sin(t); az[i] = rz * (float) Math.sin(t);
        }
        ax[0] = az[0] = ax[rings] = az[rings] = 0;
        loft(b, c, ax, az, 0, 0, 1, segs, 1f, p, uRep, vRep);
    }

    static void ball(MeshBuilder b, float cx, float cy, float cz, float rx, float ry, float rz, float[] col) {
        ballZ(b, cx, cy, cz, rx, ry, rz, 10, 7, flat(col), 0, 0);
    }

    /** Aculeo a tre facce e tre fasce di colore: radice c0, fascia c1, punta c2. */
    static void quill(MeshBuilder b, float px, float py, float pz, float dx, float dy, float dz, float len, float w, float[] c0, float[] c1, float[] c2) {
        float l = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        dx /= l; dy /= l; dz /= l;
        float ax = dy, ay = -dx, az = 0;
        float al = (float) Math.sqrt(ax * ax + ay * ay);
        if (al < 1e-3f) { ax = 1; ay = 0; al = 1; }
        ax /= al; ay /= al;
        float bx = dy * az - dz * ay, by = dz * ax - dx * az, bz = dx * ay - dy * ax;
        float tx = px + dx * len, ty = py + dy * len, tz = pz + dz * len;
        float mx = px + dx * len * 0.55f, my = py + dy * len * 0.55f, mz = pz + dz * len * 0.55f;
        for (int i = 0; i < 3; i++) {
            double a0 = i * 2 * Math.PI / 3, a1 = (i + 1) * 2 * Math.PI / 3, am = (a0 + a1) / 2;
            float c0x = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1x = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            float e0x = ax * c0x + bx * s0, e0y = ay * c0x + by * s0, e0z = az * c0x + bz * s0;
            float e1x = ax * c1x + bx * s1, e1y = ay * c1x + by * s1, e1z = az * c1x + bz * s1;
            float nx = ax * (float) Math.cos(am) + bx * (float) Math.sin(am), ny = ay * (float) Math.cos(am) + by * (float) Math.sin(am),
                    nz = az * (float) Math.cos(am) + bz * (float) Math.sin(am);
            float wm = w * 0.62f;
            b.colorf(c0[0], c0[1], c0[2]);
            b.vert(px + e0x * w, py + e0y * w, pz + e0z * w, nx, ny, nz, 0.5f, 0.5f);
            b.vert(px + e1x * w, py + e1y * w, pz + e1z * w, nx, ny, nz, 0.5f, 0.5f);
            b.colorf(c1[0], c1[1], c1[2]);
            b.vert(mx + e1x * wm, my + e1y * wm, mz + e1z * wm, nx, ny, nz, 0.5f, 0.5f);
            b.colorf(c0[0], c0[1], c0[2]);
            b.vert(px + e0x * w, py + e0y * w, pz + e0z * w, nx, ny, nz, 0.5f, 0.5f);
            b.colorf(c1[0], c1[1], c1[2]);
            b.vert(mx + e1x * wm, my + e1y * wm, mz + e1z * wm, nx, ny, nz, 0.5f, 0.5f);
            b.vert(mx + e0x * wm, my + e0y * wm, mz + e0z * wm, nx, ny, nz, 0.5f, 0.5f);
            b.vert(mx + e0x * wm, my + e0y * wm, mz + e0z * wm, nx, ny, nz, 0.5f, 0.5f);
            b.vert(mx + e1x * wm, my + e1y * wm, mz + e1z * wm, nx, ny, nz, 0.5f, 0.5f);
            b.colorf(c2[0], c2[1], c2[2]);
            b.vert(tx, ty, tz, nx, ny, nz, 0.5f, 0.5f);
        }
    }

    /**
     * Penna: striscia con la texture di una penna intera (base in basso, punta in alto), leggermente arcuata.
     * (bx,by,bz) base, (lx,ly,lz) vettore lunghezza, hw mezza larghezza. La faccia guarda sempre verso l'alto.
     */
    static void plume(MeshBuilder b, float bx, float by, float bz, float lx, float ly, float lz, float hw, float camber, float[] c0, float[] c1) {
        float wx = -lz, wy = 0, wz = lx;                      // larghezza: di traverso nel piano orizzontale
        float wl = (float) Math.sqrt(wx * wx + wz * wz);
        if (wl < 1e-4f) { wx = 1; wz = 0; wl = 1; }
        wx = wx / wl * hw; wz = wz / wl * hw;
        float nx = wy * lz - wz * ly, ny = wz * lx - wx * lz, nz = wx * ly - wy * lx;
        float nl = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        nx /= nl; ny /= nl; nz /= nl;
        if (ny < 0) { nx = -nx; ny = -ny; nz = -nz; }
        int rows = 3;
        float[] col = new float[3];
        for (int i = 0; i < rows; i++) {
            float t0 = i / (float) rows, t1 = (i + 1) / (float) rows;
            float[][] q = new float[4][];
            float[] ts = {t0, t0, t1, t1};
            float[] us = {0, 1, 1, 0};
            for (int k = 0; k < 4; k++) {
                float t = ts[k], arch = camber * 4 * t * (1 - t), s = us[k] * 2 - 1;
                q[k] = new float[] {bx + lx * t + nx * arch + wx * s, by + ly * t + ny * arch + wy * s - Math.abs(s) * camber * 0.6f, bz + lz * t + nz * arch + wz * s};
            }
            int[] ord = {0, 1, 2, 0, 2, 3};
            for (int k : ord) {
                mix(c0, c1, ts[k], col);
                b.colorf(col[0], col[1], col[2]);
                b.vert(q[k][0], q[k][1], q[k][2], nx, ny, nz, us[k], 1f - ts[k]);
            }
        }
    }

    private static Mesh done(MeshBuilder b, int tex, float spec, float shine) {
        Mesh m = b.build(tex, false, true);
        m.spec = spec; m.shine = shine;
        return m;
    }

    // ------------------------------------------------------------------ riccio

    private void buildHog() {
        final float[] brown = rgb(104, 70, 44), tan = rgb(206, 166, 122), cream = rgb(240, 214, 178), pink = rgb(232, 168, 150);
        final float[] dark = rgb(34, 24, 20), white = {1f, 1f, 0.98f};
        MeshBuilder b = new MeshBuilder();
        // corpo e muso in un'unica pelle, dalla groppa alla punta del naso
        float[][] c = {{0, .30f, -.53f}, {0, .30f, -.49f}, {0, .33f, -.39f}, {0, .35f, -.23f}, {0, .36f, -.05f}, {0, .35f, .13f}, {0, .32f, .29f},
                {0, .285f, .41f}, {0, .26f, .51f}, {0, .24f, .61f}, {0, .222f, .70f}, {0, .21f, .77f}, {0, .205f, .805f}};
        float[] rx = {0, .16f, .30f, .40f, .43f, .41f, .34f, .26f, .205f, .145f, .092f, .056f, 0};
        float[] ry = {0, .14f, .25f, .31f, .33f, .31f, .265f, .22f, .185f, .135f, .088f, .054f, 0};
        Paint fur = new Paint() {
            final float[] top = new float[3];
            @Override void col(float t, float an, float x, float y, float z, float[] o) {
                mix(brown, tan, sm((z - 0.30f) / 0.2f), top);
                mix(cream, top, sm(((float) Math.sin(an) + 0.25f) / 0.8f), o);
                mix(o, pink, 0.7f * sm((z - 0.70f) / 0.09f), o);
            }
        };
        loft(b, c, rx, ry, 0, 1, 0, 22, 0.72f, fur, 3f, 3.2f);
        for (int s = -1; s <= 1; s += 2) {                    // orecchie tonde, pelose fuori
            ballZ(b, s * .15f, .455f, .40f, .074f, .088f, .034f, 10, 6, flat(tan), 1f, 1f);
        }
        tube(b, new float[][] {{0, .27f, -.49f}, {0, .29f, -.56f}, {0, .32f, -.61f}}, new float[] {.04f, .028f, .006f}, 6, flat(tan));   // codina
        hogBody = done(b, Img.FOE_FUR, 0.12f, 12f);

        // aculei: disposti a spirale su dorso e fianchi, chinati all'indietro, a tre fasce
        b = new MeshBuilder();
        Random r = new Random(77);
        int n = 470;
        float[] c0 = new float[3], c1 = new float[3], c2 = new float[3];
        for (int i = 0; i < n; i++) {
            float uy = 1f - i / (float) (n - 1) * 1.28f, ang = i * 2.39996f + r.nextFloat() * 0.25f;
            float rr = (float) Math.sqrt(Math.max(0, 1 - uy * uy)), ux = rr * (float) Math.cos(ang), uz = rr * (float) Math.sin(ang);
            if (uz > 0.56f + 0.34f * Math.max(0, uy)) continue;                      // il muso resta scoperto
            float px = ux * .41f, py = .35f + uy * .315f, pz = -.05f + uz * .49f;
            float nx = ux / .41f, ny = uy / .315f, nz = uz / .49f, nl = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            float lean = 0.85f - 0.55f * Math.max(0, uz);
            float tone = 0.8f + 0.35f * r.nextFloat();
            float len = (0.19f + 0.12f * r.nextFloat() + 0.07f * Math.max(0, uy)) * (uy < 0 ? 0.7f : 1f);
            mix(rgb(52, 34, 24), rgb(84, 56, 36), r.nextFloat(), c0);
            mix(rgb(96, 64, 40), rgb(140, 98, 60), r.nextFloat(), c1);
            mix(rgb(226, 200, 150), rgb(250, 234, 198), r.nextFloat(), c2);
            for (int k = 0; k < 3; k++) { c0[k] *= tone; c1[k] *= tone; c2[k] *= Math.min(1.05f, tone + 0.1f); }
            quill(b, px, py, pz, nx / nl, ny / nl + 0.32f * lean, nz / nl - 0.95f * lean, len, 0.03f, c0, c1, c2);
        }
        hogSpines = done(b, -1, 0.35f, 20f);

        // parti lucide: naso, occhi con riflesso
        b = new MeshBuilder();
        ball(b, 0, .205f, .82f, .052f, .046f, .046f, dark);
        ball(b, -.012f, .222f, .85f, .014f, .010f, .008f, rgb(150, 140, 140));
        for (int s = -1; s <= 1; s += 2) {
            ball(b, s * .112f, .325f, .585f, .052f, .06f, .04f, dark);
            ball(b, s * .098f, .348f, .62f, .016f, .017f, .01f, white);
            ball(b, s * .124f, .312f, .612f, .006f, .006f, .004f, white);
        }
        hogGloss = done(b, -1, 0.95f, 48f);

        // parti opache: interno delle orecchie, guance, sopracciglia, bocca, baffi
        b = new MeshBuilder();
        for (int s = -1; s <= 1; s += 2) {
            ball(b, s * .15f, .455f, .424f, .046f, .058f, .018f, pink);
            ball(b, s * .128f, .238f, .60f, .042f, .03f, .022f, rgb(244, 160, 150));
            tube(b, s * .172f, .405f, .555f, s * .058f, .366f, .632f, .018f, .010f, 6, flat(rgb(62, 40, 26)));
            for (int k = 0; k < 3; k++) {
                tube(b, s * .062f, .205f - k * .012f, .735f, s * (.19f + .01f * k), .225f - k * .05f, .80f - k * .03f, .0045f, .001f, 3, flat(rgb(250, 240, 220)));
            }
        }
        tube(b, new float[][] {{-.052f, .168f, .675f}, {-.022f, .151f, .735f}, {0, .147f, .752f}, {.022f, .151f, .735f}, {.052f, .168f, .675f}},
                new float[] {.004f, .008f, .009f, .008f, .004f}, 5, flat(rgb(70, 30, 30)));
        hogBits = done(b, -1, 0.08f, 10f);

        // zampa: coscia pelosa, piede rosa con tre dita e unghiette (origine all'anca)
        b = new MeshBuilder();
        tube(b, new float[][] {{0, .03f, 0}, {0, -.05f, .01f}, {0, -.115f, .025f}}, new float[] {.07f, .058f, .044f}, 8, along(tan, cream));
        ballZ(b, 0, -.138f, .055f, .058f, .034f, .082f, 10, 6, ud(rgb(206, 140, 126), pink), 0, 0);
        for (int k = -1; k <= 1; k++) {
            ball(b, k * .034f, -.142f, .128f, .02f, .02f, .028f, pink);
            quill(b, k * .034f, -.146f, .148f, k * 0.2f, -0.25f, 1, .035f, .008f, rgb(90, 70, 60), rgb(60, 46, 40), dark);
        }
        hogLeg = done(b, -1, 0.15f, 14f);
    }

    private void drawHog(Gfx3D g, float[] m, Enemy3 e) {
        float walk = e.anim * 10f;
        System.arraycopy(m, 0, a, 0, 16);
        Mat4.translate(a, 0, 0.012f * (float) Math.sin(walk * 2f), 0);
        Mat4.rotateZ(a, 0.035f * (float) Math.sin(walk));                 // dondola camminando
        g.draw(hogBody, a, hr, hg, hb, 1, he);
        g.draw(hogBits, a, hr, hg, hb, 1, he);
        g.draw(hogGloss, a, 1, 1, 1, 1, 0.12f);
        System.arraycopy(a, 0, b2, 0, 16);
        float puff = 1f + 0.025f * (float) Math.sin(e.anim * 4f);          // gli aculei si gonfiano piano
        Mat4.translate(b2, 0, 0.35f, -0.05f);
        Mat4.scale(b2, puff, puff, puff);
        Mat4.translate(b2, 0, -0.35f, 0.05f);
        g.draw(hogSpines, b2, hr, hg, hb, 1, he > 0 ? he : hr < 0.7f ? 0.35f : 0f);   // aculei di lava incandescenti
        for (int i = 0; i < 4; i++) {
            int s = i % 2 == 0 ? -1 : 1;
            boolean front = i >= 2;
            System.arraycopy(m, 0, b2, 0, 16);
            Mat4.translate(b2, s * 0.2f, 0.172f, front ? 0.22f : -0.2f);
            Mat4.rotateX(b2, (float) Math.sin(walk + (s > 0 == front ? 0 : PI)) * 0.55f);
            g.draw(hogLeg, b2, hr, hg, hb, 1, he);
        }
    }

    // ------------------------------------------------------------------ corvo

    private void buildCrow() {
        final float[] black = rgb(26, 28, 46), slate = rgb(58, 64, 98), blue = rgb(52, 84, 150), violet = rgb(96, 62, 150);
        final float[] amber = rgb(252, 184, 52), amberD = rgb(206, 116, 26), white = {1f, 1f, 1f}, jet = rgb(12, 10, 12);
        MeshBuilder b = new MeshBuilder();
        // corpo, collo e testa in un'unica pelle di piume
        float[][] c = {{0, .012f, -.40f}, {0, .004f, -.345f}, {0, -.008f, -.22f}, {0, -.01f, -.06f}, {0, 0, .08f}, {0, .04f, .2f}, {0, .10f, .29f},
                {0, .168f, .36f}, {0, .2f, .43f}, {0, .207f, .5f}, {0, .203f, .548f}, {0, .198f, .568f}};
        float[] rx = {0, .07f, .15f, .2f, .205f, .178f, .132f, .126f, .131f, .102f, .052f, 0};
        float[] ry = {0, .05f, .13f, .185f, .195f, .18f, .137f, .126f, .126f, .097f, .05f, 0};
        Paint sheen = new Paint() {
            final float[] t1 = new float[3];
            @Override void col(float t, float an, float x, float y, float z, float[] o) {
                float s = (float) Math.sin(an);
                mix(slate, black, sm((s + 0.5f) / 1.1f), o);                                   // petto più chiaro, dorso nero
                mix(blue, violet, sm((z - 0.1f) / 0.3f), t1);                                   // riflessi: blu sul dorso, viola sul collo
                float k = 0.55f * sm((s - 0.05f) / 0.6f) * (0.55f + 0.45f * (float) Math.sin(z * 22f + an * 3f));
                mix(o, t1, k, o);
            }
        };
        loft(b, c, rx, ry, 0, 1, 0, 20, 0.9f, sheen, 3f, 9f);
        for (int s = -1; s <= 1; s += 2) {
            ballZ(b, s * .085f, -.13f, -.03f, .062f, .085f, .075f, 10, 6, flat(slate), 1.5f, 3f);           // calzoni di piume
            tube(b, s * .135f, .282f, .415f, s * .066f, .268f, .505f, .024f, .014f, 6, flat(black));       // arcata sopra l'occhio
        }
        crowBody = done(b, Img.FOE_PLUME, 0.4f, 30f);

        // piume disegnate una per una: coda a ventaglio su due strati, gola arruffata, ciuffo sulla nuca
        b = new MeshBuilder();
        for (int i = -4; i <= 4; i++) {
            float an = i * 0.13f, len = .44f - Math.abs(i) * .022f;
            plume(b, i * .014f, .005f - Math.abs(i) * .004f, -.31f, (float) Math.sin(an) * len, -.05f, -(float) Math.cos(an) * len, .05f, .012f, black, rgb(40, 46, 84));
        }
        for (int i = -2; i <= 2; i++) {
            float an = i * 0.16f, len = .22f;
            plume(b, i * .02f, .03f, -.27f, (float) Math.sin(an) * len, -.01f, -(float) Math.cos(an) * len, .045f, .01f, rgb(34, 38, 68), rgb(58, 68, 116));
        }
        for (int i = -3; i <= 3; i++) {                        // gola
            float x = i * .028f;
            plume(b, x, .11f - Math.abs(i) * .008f, .36f, x * 0.9f, -.085f, .05f, .026f, .004f, rgb(36, 40, 66), rgb(70, 78, 120));
        }
        for (int i = -2; i <= 2; i++) {                        // nuca
            plume(b, i * .03f, .30f - Math.abs(i) * .012f, .40f, i * .02f, .05f, -.11f, .024f, .006f, black, rgb(84, 70, 140));
        }
        crowPlumes = done(b, Img.FOE_CARD, 0.15f, 30f);
        crowPlumes.cutout = true;

        // becco superiore a uncino
        b = new MeshBuilder();
        loft(b, new float[][] {{0, .215f, .47f}, {0, .212f, .58f}, {0, .198f, .68f}, {0, .172f, .76f}, {0, .142f, .805f}, {0, .118f, .818f}},
                new float[] {.072f, .06f, .044f, .028f, .013f, 0}, new float[] {.058f, .05f, .04f, .028f, .015f, 0}, 0, 1, 0, 12, 0.25f,
                along(amber, amberD), 0, 0);
        ball(b, -.03f, .228f, .56f, .012f, .007f, .02f, rgb(120, 66, 20));        // narici
        ball(b, .03f, .228f, .56f, .012f, .007f, .02f, rgb(120, 66, 20));
        crowBeak = done(b, -1, 0.7f, 40f);
        // becco inferiore (origine al cardine): si apre quando gracchia
        b = new MeshBuilder();
        loft(b, new float[][] {{0, -.012f, 0}, {0, -.018f, .1f}, {0, -.022f, .2f}, {0, -.02f, .275f}},
                new float[] {.052f, .044f, .028f, 0}, new float[] {.032f, .028f, .02f, 0}, 0, -1, 0, 10, 0.3f, along(amberD, rgb(170, 90, 20)), 0, 0);
        crowJaw = done(b, -1, 0.6f, 36f);

        // lingua, zampe a scaglie con tre dita avanti e una dietro, artigli
        b = new MeshBuilder();
        ball(b, 0, .168f, .57f, .03f, .012f, .08f, rgb(214, 70, 80));
        Paint scaly = along(rgb(170, 120, 70), rgb(96, 70, 50)), claw = along(rgb(120, 86, 56), jet);
        for (int s = -1; s <= 1; s += 2) {
            float fx = s * .082f, fy = -.265f, fz = -.085f;
            tube(b, fx, -.17f, -.035f, fx, fy, fz, .02f, .016f, 6, scaly);
            for (int k = -1; k <= 1; k++) {
                tube(b, new float[][] {{fx, fy, fz}, {fx + k * .035f, fy - .022f, fz + .065f}, {fx + k * .05f, fy - .07f, fz + .095f}},
                        new float[] {.015f, .011f, .002f}, 5, claw);
            }
            tube(b, new float[][] {{fx, fy, fz}, {fx, fy - .02f, fz - .05f}, {fx, fy - .065f, fz - .06f}}, new float[] {.014f, .01f, .002f}, 5, claw);
        }
        crowBits = done(b, -1, 0.35f, 24f);

        // occhi: anello dorato, pupilla, riflesso
        b = new MeshBuilder();
        for (int s = -1; s <= 1; s += 2) {
            ball(b, s * .104f, .236f, .452f, .04f, .04f, .03f, rgb(252, 212, 70));
            ball(b, s * .116f, .236f, .466f, .024f, .026f, .018f, jet);
            ball(b, s * .118f, .248f, .478f, .008f, .008f, .006f, white);
        }
        crowEyes = done(b, -1, 0.95f, 48f);

        for (int k = 0; k < 2; k++) {
            int sx = k == 0 ? 1 : -1;
            // braccio dell'ala: osso piumato, remiganti secondarie e due file di copritrici
            b = new MeshBuilder();
            U0 = 0.5f; V0 = 0.62f;
            loft(b, new float[][] {{0, 0, .06f}, {sx * .17f, .012f, .078f}, {sx * .35f, 0, .05f}}, new float[] {.05f, .04f, .03f},
                    new float[] {.034f, .03f, .024f}, 0, 1, 0, 8, 1f, flat(rgb(40, 44, 76)), 0, 0);
            U0 = V0 = 0;
            for (int i = 0; i < 8; i++) {
                float x = sx * (.02f + i * .046f), len = .30f + .014f * i;
                plume(b, x, -.004f - i * .0012f, .04f, sx * (.03f + .012f * i), -.02f, -len, .043f, .012f, black, rgb(38, 44, 82));
            }
            for (int i = 0; i < 8; i++) {
                plume(b, sx * (.02f + i * .046f), .012f, .06f, sx * .02f, -.004f, -.185f, .04f, .01f, rgb(30, 34, 62), rgb(52, 62, 110));
            }
            for (int i = 0; i < 7; i++) {
                plume(b, sx * (.04f + i * .046f), .024f, .075f, sx * .012f, 0, -.105f, .036f, .008f, rgb(38, 44, 78), rgb(70, 82, 136));
            }
            crowArm[k] = done(b, Img.FOE_CARD, 0.15f, 30f);
            crowArm[k].cutout = true;
            // mano dell'ala: remiganti primarie aperte a dita, più le loro copritrici
            b = new MeshBuilder();
            U0 = 0.5f; V0 = 0.62f;
            loft(b, new float[][] {{0, 0, .05f}, {sx * .14f, 0, .046f}, {sx * .27f, 0, .02f}}, new float[] {.03f, .022f, .008f},
                    new float[] {.024f, .018f, .008f}, 0, 1, 0, 8, 1f, flat(rgb(40, 44, 76)), 0, 0);
            U0 = V0 = 0;
            for (int i = 0; i < 9; i++) {
                float th = 0.10f + i * 0.158f, len = .41f + .06f * (float) Math.sin(PI * i / 8f);
                plume(b, sx * (.01f + i * .03f), -.003f - i * .001f, .034f - i * .003f, sx * (float) Math.sin(th) * len, -.012f, -(float) Math.cos(th) * len,
                        .04f, .012f, black, rgb(36, 42, 80));
            }
            for (int i = 0; i < 7; i++) {
                float th = 0.08f + i * 0.17f;
                plume(b, sx * (.02f + i * .034f), .012f, .05f, sx * (float) Math.sin(th) * .17f, -.003f, -(float) Math.cos(th) * .17f, .036f, .008f,
                        rgb(30, 34, 62), rgb(54, 64, 112));
            }
            crowHand[k] = done(b, Img.FOE_CARD, 0.15f, 30f);
            crowHand[k].cutout = true;
        }
    }

    private void drawCrow(Gfx3D g, float[] m, Enemy3 e) {
        float ph = e.anim * 11f, flap = (float) Math.sin(ph);
        System.arraycopy(m, 0, a, 0, 16);
        Mat4.translate(a, 0, -0.035f * flap, 0);                           // il corpo sale quando le ali spingono in giù
        Mat4.rotateX(a, -0.10f + 0.05f * flap);
        g.draw(crowBody, a, 1, 1, 1, 1, 0);
        g.draw(crowPlumes, a, 1, 1, 1, 1, 0);
        g.draw(crowBits, a, 1, 1, 1, 1, 0);
        g.draw(crowBeak, a, 1, 1, 1, 1, 0);
        g.draw(crowEyes, a, 1, 1, 1, 1, 0.55f);
        System.arraycopy(a, 0, b2, 0, 16);
        Mat4.translate(b2, 0, 0.172f, 0.468f);
        Mat4.rotateX(b2, 0.38f * Math.max(0f, (float) Math.sin(e.anim * 3.1f)));   // gracchia
        g.draw(crowJaw, b2, 1, 1, 1, 1, 0);
        for (int k = 0; k < 2; k++) {
            int s = k == 0 ? 1 : -1;
            System.arraycopy(a, 0, b2, 0, 16);
            Mat4.translate(b2, s * 0.15f, 0.085f, 0.04f);
            Mat4.rotateZ(b2, s * (0.12f + 0.72f * flap));
            g.draw(crowArm[k], b2, 1, 1, 1, 1, 0);
            System.arraycopy(b2, 0, c2, 0, 16);
            Mat4.translate(c2, s * 0.35f, 0, 0);
            Mat4.rotateZ(c2, s * (0.22f + 0.6f * (float) Math.sin(ph - 1.0f)));   // la punta segue in ritardo
            g.draw(crowHand[k], c2, 1, 1, 1, 1, 0);
        }
    }

    // ------------------------------------------------------------------ ghianda

    private void buildAcorn() {
        final float[] gold = rgb(232, 178, 92), nut = rgb(170, 110, 52), nutD = rgb(118, 74, 36), white = {1f, 1f, 0.97f}, jet = rgb(20, 14, 12);
        final float[] twig = rgb(112, 76, 44), twigL = rgb(160, 116, 70);
        MeshBuilder b = new MeshBuilder();
        // guscio: punta in basso, pancia larga, venature del legno
        float[] ys = {.0f, .018f, .045f, .09f, .17f, .27f, .37f, .46f, .53f};
        float[] rs = {0, .03f, .06f, .14f, .225f, .272f, .288f, .28f, .25f};
        float[][] c = new float[ys.length][];
        for (int i = 0; i < ys.length; i++) c[i] = new float[] {0, ys[i], 0};
        Paint shell = new Paint() {
            @Override void col(float t, float an, float x, float y, float z, float[] o) {
                mix(gold, nut, sm((y - 0.08f) / 0.5f), o);
                mix(o, nutD, 0.45f * sm((-(float) Math.sin(an) - 0.1f) / 0.8f), o);        // dietro più scura
            }
        };
        loft(b, c, rs, rs, 0, 0, 1, 24, 1f, shell, 2f, 2.2f);
        acornBody = done(b, Img.FOE_NUT, 0.7f, 34f);

        // cappello a scaglie con orlo in rilievo
        b = new MeshBuilder();
        float[] cy = {.50f, .507f, .535f, .59f, .65f, .70f, .735f, .75f};
        float[] cr = {.262f, .305f, .318f, .305f, .25f, .165f, .075f, 0};
        c = new float[cy.length][];
        for (int i = 0; i < cy.length; i++) c[i] = new float[] {0, cy[i], 0};
        final float[] capD = rgb(96, 64, 36), capL = rgb(176, 130, 78);
        loft(b, c, cr, cr, 0, 0, 1, 24, 1f, along(capD, capL), 3f, 5f);
        for (int i = 0; i < 20; i++) {
            float an = i * 2 * PI / 20;
            ballY(b, (float) Math.cos(an) * .312f, .522f, (float) Math.sin(an) * .312f, .038f, .032f, .038f, 6, 4, along(capD, rgb(150, 108, 62)), 0.3f, 0.3f);
        }
        acornCap = done(b, Img.FOE_CAP, 0.3f, 16f);

        // parti opache: picciolo ricurvo, foglia di quercia, bocca con i denti, sopracciglia, guance
        b = new MeshBuilder();
        tube(b, new float[][] {{0, .735f, 0}, {.012f, .80f, 0}, {.045f, .86f, -.005f}, {.085f, .895f, -.012f}}, new float[] {.042f, .034f, .03f, .026f}, 8, along(twig, twigL));
        ball(b, .092f, .9f, -.013f, .028f, .028f, .028f, rgb(196, 160, 110));
        oakLeaf(b, .05f, .865f, -.005f);
        for (int s = -1; s <= 1; s += 2) {
            tube(b, s * .2f, .455f, .25f, s * .04f, .39f, .30f, .026f, .014f, 6, flat(rgb(66, 40, 22)));
            ball(b, s * .2f, .235f, .195f, .046f, .032f, .02f, rgb(240, 130, 116));
        }
        ballZ(b, 0, .2f, .258f, .095f, .05f, .03f, 12, 6, flat(rgb(96, 24, 28)), 0, 0);                    // bocca aperta
        ball(b, 0, .18f, .276f, .05f, .02f, .014f, rgb(226, 90, 96));                                         // lingua
        for (int k = -1; k <= 1; k += 2) quill(b, k * .04f, .236f, .282f, 0, -1, 0.1f, .045f, .016f, white, white, rgb(230, 230, 220));   // zanne
        acornBits = done(b, -1, 0.1f, 10f);

        // occhi grandi con iride, pupilla e riflessi
        b = new MeshBuilder();
        for (int s = -1; s <= 1; s += 2) {
            ball(b, s * .108f, .345f, .245f, .078f, .092f, .05f, white);
            ball(b, s * .100f, .338f, .279f, .044f, .054f, .024f, rgb(120, 66, 24));
            ball(b, s * .098f, .336f, .293f, .026f, .034f, .016f, jet);
            ball(b, s * .084f, .362f, .304f, .012f, .014f, .007f, white);
            ball(b, s * .112f, .32f, .305f, .006f, .006f, .004f, white);
        }
        acornEyes = done(b, -1, 0.9f, 44f);

        // braccio di rametto con gomito, mano a tre dita (origine alla spalla, lato destro)
        b = new MeshBuilder();
        tube(b, new float[][] {{0, 0, 0}, {.09f, -.055f, .03f}, {.15f, -.015f, .09f}}, new float[] {.032f, .026f, .022f}, 7, along(twig, twigL));
        ball(b, .09f, -.055f, .03f, .03f, .03f, .03f, twig);
        ball(b, .162f, -.008f, .102f, .04f, .038f, .04f, twigL);
        float[][] fd = {{.045f, .035f, .035f}, {.055f, 0, .05f}, {.03f, -.035f, .045f}};
        for (float[] f : fd) tube(b, .165f, -.008f, .105f, .165f + f[0], -.008f + f[1], .105f + f[2], .014f, .008f, 5, flat(twigL));
        acornArm = done(b, -1, 0.15f, 12f);

        // gamba corta e scarpone (origine all'anca)
        b = new MeshBuilder();
        tube(b, 0, .02f, 0, 0, -.1f, .005f, .032f, .028f, 7, along(twig, twigL));
        ballZ(b, 0, -.14f, .045f, .072f, .046f, .112f, 12, 7, ud(rgb(52, 34, 22), rgb(110, 72, 42)), 0, 0);
        ballZ(b, 0, -.172f, .045f, .076f, .016f, .116f, 12, 5, flat(rgb(36, 24, 18)), 0, 0);                   // suola
        for (int k = -1; k <= 1; k++) ball(b, k * .034f, -.128f, .14f, .022f, .022f, .026f, rgb(126, 86, 52));
        acornFoot = done(b, -1, 0.3f, 20f);
    }

    /** Foglia di quercia lobata, attaccata al picciolo: nervatura centrale e lobi alterni. */
    private static void oakLeaf(MeshBuilder b, float ox, float oy, float oz) {
        float[] cA = rgb(214, 120, 36), cB = rgb(236, 176, 60), cV = rgb(130, 76, 30);
        float dx = -0.82f, dy = 0.22f, dz = 0.52f;             // direzione della nervatura
        float wx = 0.52f, wy = 0.1f, wz = 0.84f;               // di traverso
        float nx = dy * wz - dz * wy, ny = dz * wx - dx * wz, nz = dx * wy - dy * wx;
        float nl = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        nx /= nl; ny /= nl; nz /= nl;
        if (ny < 0) { nx = -nx; ny = -ny; nz = -nz; }
        int n = 12;
        float len = 0.3f;
        float[] hw = new float[n + 1];
        for (int i = 0; i <= n; i++) {
            float t = i / (float) n;
            hw[i] = 0.085f * (float) Math.sin(PI * Math.pow(t, 0.8)) * (0.62f + 0.38f * (float) Math.abs(Math.sin(t * PI * 3.5f))) + 0.004f;
        }
        hw[n] = 0;
        float[] col = new float[3];
        for (int i = 0; i < n; i++) {
            for (int s = -1; s <= 1; s += 2) {
                float[][] q = new float[4][];
                float[] tt = {i / (float) n, i / (float) n, (i + 1) / (float) n, (i + 1) / (float) n};
                float[] ww = {0, hw[i], hw[i + 1], 0};
                for (int k = 0; k < 4; k++) {
                    float droop = -0.06f * tt[k] * tt[k] - 0.25f * ww[k];
                    q[k] = new float[] {ox + dx * len * tt[k] + wx * ww[k] * s, oy + dy * len * tt[k] + wy * ww[k] * s + droop, oz + dz * len * tt[k] + wz * ww[k] * s};
                }
                int[] ord = {0, 1, 2, 0, 2, 3};
                for (int k : ord) {
                    mix(cA, cB, tt[k], col);
                    if (ww[k] == 0) mix(col, cV, 0.7f, col);
                    b.colorf(col[0], col[1], col[2]);
                    b.vert(q[k][0], q[k][1], q[k][2], nx, ny, nz, 0.5f, 0.5f);
                }
            }
        }
    }

    private void drawAcorn(Gfx3D g, float[] m, Enemy3 e) {
        float air = Math.max(0f, Math.min(1f, (e.y - e.baseY) / 0.6f));
        float crouch = air > 0.02f ? 0 : 0.5f + 0.5f * (float) Math.sin(e.anim * 9f);
        float sy = 1f + 0.14f * air - 0.07f * crouch, sxz = 1f - 0.07f * air + 0.04f * crouch;      // si allunga in salto, si schiaccia a terra
        System.arraycopy(m, 0, a, 0, 16);
        Mat4.translate(a, 0, 0.12f, 0);
        Mat4.scale(a, sxz, sy, sxz);
        Mat4.rotateZ(a, 0.05f * (float) Math.sin(e.anim * 6f));
        g.draw(acornBody, a, 1, 1, 1, 1, 0);
        g.draw(acornCap, a, 1, 1, 1, 1, 0);
        g.draw(acornBits, a, 1, 1, 1, 1, 0);
        g.draw(acornEyes, a, 1, 1, 1, 1, 0.12f);
        for (int s = -1; s <= 1; s += 2) {
            System.arraycopy(a, 0, b2, 0, 16);
            Mat4.translate(b2, s * 0.262f, 0.33f, 0.03f);
            if (s < 0) Mat4.scale(b2, -1, 1, 1);
            Mat4.rotateZ(b2, 1.0f * air + 0.22f * (float) Math.sin(e.anim * 8f + s));          // braccia in alto quando salta
            g.draw(acornArm, b2, 1, 1, 1, 1, 0);
            System.arraycopy(m, 0, b2, 0, 16);
            Mat4.translate(b2, s * 0.115f, 0.2f - 0.05f * crouch * 0.4f + 0.02f * air, 0);
            if (s < 0) Mat4.scale(b2, -1, 1, 1);
            Mat4.rotateX(b2, 0.7f * air * s * 0.6f + 0.5f * air);                               // gambe che scalciano in aria
            g.draw(acornFoot, b2, 1, 1, 1, 1, 0);
        }
    }

    // ------------------------------------------------------------------ scarabeo cervo

    private void buildBeetle() {
        final float[] under = rgb(34, 22, 50), purple = rgb(112, 58, 170), teal = rgb(44, 160, 156), gold = rgb(220, 176, 74), jet = rgb(22, 16, 28);
        MeshBuilder b = new MeshBuilder();
        // elitre: corazza striata con riflessi cangianti, orlo dorato e giunzione al centro
        float[][] c = {{0, .30f, -.55f}, {0, .30f, -.51f}, {0, .33f, -.41f}, {0, .35f, -.26f}, {0, .36f, -.09f}, {0, .35f, .05f}, {0, .335f, .15f}, {0, .33f, .19f}};
        float[] rx = {0, .18f, .33f, .41f, .435f, .415f, .365f, .30f};
        float[] ry = {0, .13f, .22f, .28f, .30f, .285f, .25f, .20f};
        Paint elytra = new Paint() {
            @Override void col(float t, float an, float x, float y, float z, float[] o) {
                float s = (float) Math.sin(an);
                mix(under, purple, sm((s + 0.05f) / 0.5f), o);
                mix(o, teal, 0.5f * sm((s - 0.45f) / 0.45f) * (0.5f + 0.5f * (float) Math.sin(z * 14f + 1f)), o);
                mix(o, gold, 0.85f * Math.max(0f, 1f - Math.abs(s - 0.04f) / 0.13f), o);
                if (s > 0.985f) mix(o, jet, 0.85f, o);
            }
        };
        loft(b, c, rx, ry, 0, 1, 0, 24, 0.42f, elytra, 2f, 1.6f);
        // scudo del torace, più liscio e scuro
        final float[] navy = rgb(52, 40, 104), navyL = rgb(84, 96, 176);
        Paint shield = new Paint() {
            @Override void col(float t, float an, float x, float y, float z, float[] o) {
                float s = (float) Math.sin(an);
                mix(under, navy, sm((s + 0.05f) / 0.5f), o);
                mix(o, navyL, 0.6f * sm((s - 0.5f) / 0.5f), o);
                mix(o, gold, 0.7f * Math.max(0f, 1f - Math.abs(s - 0.04f) / 0.12f), o);
            }
        };
        loft(b, new float[][] {{0, .325f, .10f}, {0, .33f, .14f}, {0, .345f, .23f}, {0, .34f, .34f}, {0, .315f, .43f}, {0, .29f, .485f}, {0, .28f, .50f}},
                new float[] {0, .31f, .385f, .355f, .27f, .17f, 0}, new float[] {0, .2f, .265f, .25f, .2f, .14f, 0}, 0, 1, 0, 22, 0.45f, shield, 1f, 0.6f);
        // testa
        ballZ(b, 0, .255f, .57f, .175f, .12f, .135f, 14, 8, ud(under, rgb(70, 52, 110)), 1f, 0.5f);
        beetleShell = done(b, Img.FOE_CHITIN, 0.45f, 60f);

        // antenne a clava
        b = new MeshBuilder();
        for (int s = -1; s <= 1; s += 2) {
            tube(b, new float[][] {{s * .07f, .33f, .64f}, {s * .16f, .43f, .74f}, {s * .245f, .41f, .83f}}, new float[] {.014f, .011f, .01f}, 5, flat(jet));
            for (int k = 0; k < 3; k++) ball(b, s * (.25f + k * .022f), .408f - k * .006f, .84f + k * .02f, .012f, .03f, .02f, rgb(120, 80, 40));
        }
        beetleGloss = done(b, -1, 0.6f, 30f);

        // occhi composti gialli
        b = new MeshBuilder();
        for (int s = -1; s <= 1; s += 2) {
            ball(b, s * .145f, .3f, .615f, .05f, .05f, .045f, rgb(252, 216, 64));
            ball(b, s * .16f, .312f, .642f, .016f, .016f, .012f, rgb(255, 255, 240));
        }
        beetleEyes = done(b, -1, 0.9f, 40f);

        // zampa in tre tratti con ginocchio, spine sulla tibia e uncini (origine all'anca, lato destro)
        b = new MeshBuilder();
        Paint legP = along(rgb(60, 40, 84), jet);
        tube(b, new float[][] {{0, 0, 0}, {.09f, .06f, 0}, {.175f, .095f, 0}}, new float[] {.046f, .042f, .034f}, 7, legP);
        ball(b, .18f, .095f, 0, .04f, .04f, .04f, rgb(70, 48, 100));
        tube(b, new float[][] {{.18f, .095f, 0}, {.26f, .02f, 0}, {.335f, -.06f, 0}}, new float[] {.032f, .027f, .02f}, 7, legP);
        for (int k = 0; k < 3; k++) quill(b, .22f + k * .04f, .06f - k * .04f, 0, 0.7f, 0.5f, k % 2 == 0 ? 0.6f : -0.6f, .05f, .01f, jet, jet, rgb(90, 70, 110));
        tube(b, new float[][] {{.335f, -.06f, 0}, {.375f, -.13f, 0}, {.405f, -.195f, 0}}, new float[] {.016f, .012f, .008f}, 5, flat(jet));
        quill(b, .405f, -.19f, 0, 0.8f, -0.4f, 0.5f, .04f, .007f, jet, jet, jet);
        quill(b, .405f, -.19f, 0, 0.8f, -0.4f, -0.5f, .04f, .007f, jet, jet, jet);
        beetleLeg = done(b, -1, 0.7f, 34f);

        // mandibola ricurva con dente interno (origine all'attacco, lato destro)
        b = new MeshBuilder();
        Paint jawP = along(rgb(96, 52, 40), jet);
        tube(b, new float[][] {{0, 0, 0}, {.085f, .02f, .12f}, {.105f, .035f, .26f}, {.05f, .04f, .36f}, {.0f, .04f, .40f}}, new float[] {.046f, .04f, .03f, .018f, .003f}, 8, jawP);
        quill(b, .09f, .03f, .2f, -1, 0, 0.3f, .07f, .018f, rgb(70, 40, 34), jet, jet);
        quill(b, .07f, .038f, .31f, -1, 0, 0.2f, .045f, .012f, jet, jet, jet);
        beetleJaw = done(b, -1, 0.8f, 40f);
    }

    private void drawBeetle(Gfx3D g, float[] m, Enemy3 e) {
        float walk = e.anim * 14f;
        System.arraycopy(m, 0, a, 0, 16);
        Mat4.translate(a, 0, 0.01f * (float) Math.sin(walk * 2f), 0);
        g.draw(beetleShell, a, 1, 1, 1, 1, 0);
        g.draw(beetleGloss, a, 1, 1, 1, 1, 0);
        g.draw(beetleEyes, a, 1, 1, 1, 1, 0.85f);
        float pinch = 0.16f * (float) Math.sin(e.anim * 5f);
        for (int s = -1; s <= 1; s += 2) {
            System.arraycopy(a, 0, b2, 0, 16);
            Mat4.translate(b2, s * 0.075f, 0.245f, 0.63f);
            if (s < 0) Mat4.scale(b2, -1, 1, 1);
            Mat4.rotateY(b2, pinch - 0.05f);
            g.draw(beetleJaw, b2, 1, 1, 1, 1, 0);
        }
        float[] hipZ = {0.32f, 0.06f, -0.22f}, yaw0 = {-0.6f, 0f, 0.65f};
        for (int i = 0; i < 6; i++) {
            int s = i % 2 == 0 ? 1 : -1, k = i / 2;
            float phs = walk + (i % 2 == 0 ? 0 : PI) + k * PI;
            System.arraycopy(m, 0, b2, 0, 16);
            Mat4.translate(b2, s * 0.3f, 0.2f, hipZ[k]);
            if (s < 0) Mat4.scale(b2, -1, 1, 1);
            Mat4.rotateY(b2, yaw0[k] + 0.32f * (float) Math.sin(phs));
            Mat4.rotateZ(b2, 0.14f * Math.max(0f, (float) Math.cos(phs)));       // la zampa si alza mentre torna avanti
            g.draw(beetleLeg, b2, 1, 1, 1, 1, 0);
        }
    }

    // ------------------------------------------------------------------ nemici del cielo

    private void buildSky() {
        MeshBuilder b = new MeshBuilder();                                  // nuvola temporalesca: cumuli grigio-azzurri, il ventre più scuro
        b.colorf(0.34f, 0.37f, 0.48f);
        b.ellipsoid(0, 0.22f, 0, 0.52f, 0.26f, 0.46f, 14, 9);
        b.colorf(0.47f, 0.51f, 0.64f);
        b.ellipsoid(-0.38f, 0.36f, 0.02f, 0.38f, 0.32f, 0.36f, 12, 8);
        b.ellipsoid(0.38f, 0.36f, 0f, 0.36f, 0.30f, 0.34f, 12, 8);
        b.colorf(0.56f, 0.60f, 0.72f);
        b.ellipsoid(0, 0.56f, -0.02f, 0.42f, 0.34f, 0.40f, 12, 8);
        stormBody = done(b, -1, 0.1f, 14f);
        b = new MeshBuilder();                                              // occhi truci e sopracciglia aggrottate
        b.colorf(1f, 1f, 1f);
        b.ellipsoid(-0.17f, 0.40f, 0.40f, 0.10f, 0.10f, 0.05f, 10, 6);
        b.ellipsoid(0.17f, 0.40f, 0.40f, 0.10f, 0.10f, 0.05f, 10, 6);
        b.colorf(0.05f, 0.05f, 0.12f);
        b.ellipsoid(-0.17f, 0.39f, 0.44f, 0.045f, 0.05f, 0.03f, 8, 5);
        b.ellipsoid(0.17f, 0.39f, 0.44f, 0.045f, 0.05f, 0.03f, 8, 5);
        b.box(-0.18f, 0.53f, 0.41f, 0.22f, 0.04f, 0.04f);
        b.box(0.18f, 0.53f, 0.41f, 0.22f, 0.04f, 0.04f);
        stormEyes = done(b, -1, 0.3f, 30f);
        b = new MeshBuilder();                                              // fulmine: due cartoncini incrociati con la texture a zig-zag, da 0 a -1 in y
        b.colorf(1f, 1f, 1f);
        b.quad(-0.45f, -1f, 0, 0.45f, -1f, 0, 0.45f, 0f, 0, -0.45f, 0f, 0, 0, 0, 1, 0, 0, 1, 1);
        b.quad(0, -1f, -0.45f, 0, -1f, 0.45f, 0, 0f, 0.45f, 0, 0f, -0.45f, 1, 0, 0, 0, 0, 1, 1);
        stormBolt = b.build(Img.HV_BOLT, true, true);
        b = new MeshBuilder();                                              // mulinello: un anello schiacciato, ripetuto in pile di raggio crescente
        b.colorf(1f, 1f, 1f);
        b.ellipsoid(0, 0, 0, 1f, 0.22f, 1f, 14, 6);
        whirlTier = done(b, -1, 0.15f, 20f);
        b = new MeshBuilder();
        b.colorf(1f, 1f, 1f);
        b.ellipsoid(-0.14f, 0f, 0.12f, 0.06f, 0.07f, 0.04f, 8, 5);
        b.ellipsoid(0.14f, 0f, 0.12f, 0.06f, 0.07f, 0.04f, 8, 5);
        b.colorf(0.05f, 0.08f, 0.2f);
        b.ellipsoid(-0.14f, -0.005f, 0.155f, 0.03f, 0.04f, 0.02f, 8, 5);
        b.ellipsoid(0.14f, -0.005f, 0.155f, 0.03f, 0.04f, 0.02f, 8, 5);
        whirlEyes = done(b, -1, 0.3f, 30f);
    }

    private void drawStorm(Gfx3D g, float[] m, Enemy3 e) {
        int ph = Enemy3.stormPhase(e.hopT);
        System.arraycopy(m, 0, a, 0, 16);
        Mat4.translate(a, 0, -0.25f, 0);
        if (ph > 0) Mat4.scale(a, 1f + 0.05f * (float) Math.sin(e.anim * 40f), 1f, 1f + 0.05f * (float) Math.sin(e.anim * 40f + 1f));
        g.draw(stormBody, a, ph > 0 ? 1.3f : 1f, ph > 0 ? 1.25f : 1f, ph > 0 ? 0.9f : 1f, 1, ph > 0 ? 0.25f : 0f);
        if (ph > 0) g.draw(stormEyes, a, 1f, 0.85f, 0.2f, 1, 0.9f); else g.draw(stormEyes, a, 1, 1, 1, 1, 0.3f);
        if (ph > 0 && e.flatTime <= 0) {                                    // avviso tremolante, poi la scarica
            System.arraycopy(m, 0, b2, 0, 16);
            Mat4.translate(b2, 0, -0.3f, 0);
            Mat4.scale(b2, ph == 2 ? 1.2f : 0.55f, Math.max(1f, e.y + 0.3f), ph == 2 ? 1.2f : 0.55f);
            g.draw(stormBolt, b2, 1, 1, 1, ph == 2 ? 1f : 0.25f + 0.2f * (float) Math.sin(e.anim * 45f), 1f);
        }
    }

    private void drawWhirl(Gfx3D g, float[] m, Enemy3 e) {
        int n = 7;
        for (int i = 0; i < n; i++) {
            float t = i / (float) (n - 1);
            float ph = e.anim * 9f + i * 0.9f;
            System.arraycopy(m, 0, a, 0, 16);
            Mat4.translate(a, 0.07f * (float) Math.sin(ph) * (1f - t * 0.4f), 0.12f + t * 1.15f, 0.07f * (float) Math.cos(ph));
            Mat4.rotateY(a, e.anim * 10f + i);
            float r = 0.17f + 0.38f * t;
            Mat4.scale(a, r, 1f, r);
            if (i % 2 == 0) g.draw(whirlTier, a, 0.95f, 0.98f, 1f, 1, 0.25f); else g.draw(whirlTier, a, 0.62f, 0.8f, 0.95f, 1, 0.1f);
        }
        System.arraycopy(m, 0, a, 0, 16);
        Mat4.translate(a, 0, 1.0f, 0.12f);
        g.draw(whirlEyes, a, 1, 1, 1, 1, 0.4f);
    }

    /** Aspetto stagionale: si disegna il modello di un altro nemico (o un riccio ritinto) con lo stato del nemico vero. */
    private void drawSeason(Gfx3D g, float[] m, Enemy3 e) {
        int type = e.type, mode = e.mode, skin = e.skin;
        e.skin = 0;
        switch (skin) {
            case Enemy3.SKIN_FROG: e.type = Enemy3.FROG; e.mode = e.y > e.baseY + 0.05f ? 1 : 0; more.draw(g, m, e); break;
            case Enemy3.SKIN_HARE: more.drawHare(g, m, e); break;
            case Enemy3.SKIN_EMBER: { float[] sm = new float[16]; System.arraycopy(m, 0, sm, 0, 16); Mat4.scale(sm, 0.6f, 0.6f, 0.6f); e.type = Enemy3.LAPILLO; e.mode = 1; more.draw(g, sm, e); break; }
            case Enemy3.SKIN_PENGUIN: e.type = Enemy3.PENGUIN; e.mode = 0; more.draw(g, m, e); break;
            case Enemy3.SKIN_ICE: hr = 0.75f; hg = 0.9f; hb = 1.25f; he = 0.15f; drawHog(g, m, e); hr = hg = hb = 1; he = 0; break;
            case Enemy3.SKIN_MAGMA: hr = 0.55f; hg = 0.32f; hb = 0.25f; he = 0f; drawHog(g, m, e); hr = hg = hb = 1; he = 0; break;
            default: break;
        }
        e.type = type; e.mode = mode; e.skin = skin;
    }

    /** Disegna un nemico; m è la matrice già posizionata e orientata (ed eventualmente schiacciata). */
    private FoeIsland island;

    /** Tinta ed emissione applicate al riccio (riccio di ghiaccio, riccio di lava). */
    private float hr = 1, hg = 1, hb = 1, he = 0;

    public void draw(Gfx3D g, float[] m, Enemy3 e) {
        if (e.skin >= Enemy3.SKIN_FROG) { drawSeason(g, m, e); return; }
        if (e.skin != 0) { if (island == null) island = new FoeIsland(); island.draw(g, m, e); return; }
        switch (e.type) {
            case Enemy3.HOG: drawHog(g, m, e); break;
            case Enemy3.CROW: drawCrow(g, m, e); break;
            case Enemy3.STORM: drawStorm(g, m, e); break;
            case Enemy3.WHIRL: drawWhirl(g, m, e); break;
            case Enemy3.ACORN: drawAcorn(g, m, e); break;
            default:
                if (e.type >= Enemy3.MOLE) more.draw(g, m, e); else drawBeetle(g, m, e);
                break;
        }
    }
}
