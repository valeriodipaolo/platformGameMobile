package com.valeriodipaolo.platformgame.core3d;

/** Modelli della miniera: il carrello del robot e il fascio di dinamite con la miccia accesa (l'ostacolo che esplode). */
final class MineModels {
    private MineModels() {}

    /** Altezza della testa delle rotaie (Scene3D le disegna a 0,24): le ruote ci poggiano sopra. */
    static final float RAIL_TOP = 0.24f;
    /** Di quanto il robot sta più in alto dentro il carrello (i piedi sul fondo della vasca, non sui binari). */
    static final float RIDE = 0.36f;

    /**
     * Carrello da miniera, muso verso +z. Quattro ruote con bordino sulle rotaie (scartamento 0,5 m), telaio con assi e boccole,
     * respingenti e gancio davanti e dietro, vasca d'acciaio svasata con chiazze di ruggine, costole orizzontali, angolari sugli
     * spigoli, bordo superiore arrotolato, pareti interne scure e leva del freno sul fianco.
     */
    static void cart(MeshBuilder b) {
        float wr = 0.13f, wy = RAIL_TOP + wr, gauge = 0.25f;
        float yb = 0.44f, yt = 1.0f, wb = 0.38f, wt = 0.5f, lb = 0.52f, lt = 0.64f;
        float[][] bot = {{-wb, yb, -lb}, {wb, yb, -lb}, {wb, yb, lb}, {-wb, yb, lb}}, top = {{-wt, yt, -lt}, {wt, yt, -lt}, {wt, yt, lt}, {-wt, yt, lt}};
        // pareti esterne: griglia 6x4 colorata con rumore (acciaio brunito e ruggine a chiazze, più scura in basso)
        for (int k = 0; k < 4; k++) {
            float[] a = bot[k], c = bot[(k + 1) % 4], d = top[(k + 1) % 4], e = top[k];
            float nx = (a[0] + c[0]) * 0.5f, nz = (a[2] + c[2]) * 0.5f, nl = (float) Math.sqrt(nx * nx + nz * nz);
            nx /= nl; nz /= nl;
            int nu = 6, nv = 4;
            for (int i = 0; i < nu; i++) for (int j = 0; j < nv; j++) {
                float u0 = i / (float) nu, u1 = (i + 1) / (float) nu, v0 = j / (float) nv, v1 = (j + 1) / (float) nv;
                float[] p00 = bil(a, c, d, e, u0, v0), p10 = bil(a, c, d, e, u1, v0), p11 = bil(a, c, d, e, u1, v1), p01 = bil(a, c, d, e, u0, v1);
                float cx = (p00[0] + p11[0]) * 0.5f, cy = (p00[1] + p11[1]) * 0.5f, cz = (p00[2] + p11[2]) * 0.5f;
                float n = MeshBuilder.noise3(cx * 5f, cy * 5f, cz * 5f, 77), rust = Math.max(0f, Math.min(1f, (n - 0.35f) * 2.2f));
                float sh = 0.7f + 0.3f * v1;
                b.colorf(sh * (0.2f + 0.14f * rust), sh * (0.2f + 0.0f * rust), sh * (0.21f - 0.1f * rust));
                b.quad(p00[0], p00[1], p00[2], p10[0], p10[1], p10[2], p11[0], p11[1], p11[2], p01[0], p01[1], p01[2], nx, 0.2f, nz, 0, 0, 1, 1);
            }
            // pareti interne, scure, leggermente rientrate
            float in = 0.05f;
            b.colorf(0.12f, 0.1f, 0.09f);
            b.quad(c[0] * (1 - in * 2), c[1] + 0.06f, c[2] * (1 - in * 2), a[0] * (1 - in * 2), a[1] + 0.06f, a[2] * (1 - in * 2),
                    e[0] - Math.signum(e[0]) * in, e[1] - 0.01f, e[2] - Math.signum(e[2]) * in, d[0] - Math.signum(d[0]) * in, d[1] - 0.01f, d[2] - Math.signum(d[2]) * in,
                    -nx, 0.3f, -nz, 0, 0, 1, 1);
        }
        b.colorf(0.09f, 0.08f, 0.07f);                                       // fondo
        b.quad(-wb, yb + 0.06f, -lb, wb, yb + 0.06f, -lb, wb, yb + 0.06f, lb, -wb, yb + 0.06f, lb, 0, 1, 0, 0, 0, 1, 1);
        b.colorf(0.24f, 0.22f, 0.2f);
        b.quad(-wb, yb, -lb, -wb, yb, lb, wb, yb, lb, wb, yb, -lb, 0, -1, 0, 0, 0, 1, 1);
        // bordo arrotolato: tubi lungo i quattro lati
        b.colorf(0.36f, 0.35f, 0.34f);
        b.box(0, yt, lt, wt * 2 + 0.08f, 0.07f, 0.07f);
        b.box(0, yt, -lt, wt * 2 + 0.08f, 0.07f, 0.07f);
        b.box(wt, yt, 0, 0.07f, 0.07f, lt * 2 + 0.08f);
        b.box(-wt, yt, 0, 0.07f, 0.07f, lt * 2 + 0.08f);
        // costole orizzontali sui fianchi e angolari sugli spigoli
        b.colorf(0.17f, 0.16f, 0.16f);
        for (float t : new float[] {0.33f, 0.66f}) {
            float y = yb + (yt - yb) * t, xw = wb + (wt - wb) * t, zl = lb + (lt - lb) * t;
            b.box(xw + 0.02f, y, 0, 0.04f, 0.05f, zl * 2);
            b.box(-xw - 0.02f, y, 0, 0.04f, 0.05f, zl * 2);
            b.box(0, y, zl + 0.02f, xw * 2, 0.05f, 0.04f);
            b.box(0, y, -zl - 0.02f, xw * 2, 0.05f, 0.04f);
        }
        b.colorf(0.27f, 0.25f, 0.24f);
        for (int k = 0; k < 4; k++) {
            float[] a = bot[k], e = top[k];
            float sx = Math.signum(a[0]), sz = Math.signum(a[2]);
            for (int q = 0; q < 6; q++) {                                 // angolare: blocchetti lungo lo spigolo inclinato
                float t = (q + 0.5f) / 6f;
                b.box(a[0] + (e[0] - a[0]) * t + sx * 0.012f, a[1] + (e[1] - a[1]) * t, a[2] + (e[2] - a[2]) * t + sz * 0.012f, 0.06f, (yt - yb) / 6f + 0.01f, 0.06f);
            }
        }
        for (int k = 0; k < 8; k++) {                                     // ribattini sulle costole dei fianchi
            float z = -lt + 0.12f + k * (lt * 2 - 0.24f) / 7f;
            b.colorf(0.6f, 0.56f, 0.5f);
            for (int s = -1; s <= 1; s += 2) b.ellipsoid(s * (wt * 0.9f + 0.03f), yb + (yt - yb) * 0.66f, z * 0.95f, 0.014f, 0.014f, 0.014f, 4, 3);
        }
        // telaio: longheroni, traverse, respingenti e ganci
        b.colorf(0.13f, 0.12f, 0.12f);
        for (int s = -1; s <= 1; s += 2) b.box(s * 0.3f, yb - 0.05f, 0, 0.07f, 0.1f, lt * 2 + 0.1f);
        for (int s = -1; s <= 1; s += 2) {
            float z = s * (lt + 0.05f);
            b.box(0, yb - 0.04f, z, 0.74f, 0.12f, 0.08f);
            b.colorf(0.26f, 0.25f, 0.24f);
            for (int q = -1; q <= 1; q += 2) cylZ(b, q * 0.26f, yb - 0.04f, z, z + s * 0.1f, 0.055f);
            b.colorf(0.45f, 0.43f, 0.4f);
            for (int q = -1; q <= 1; q += 2) cylZ(b, q * 0.26f, yb - 0.04f, z + s * 0.1f, z + s * 0.12f, 0.075f);
            b.colorf(0.2f, 0.19f, 0.18f);
            b.box(0, yb - 0.08f, z + s * 0.09f, 0.05f, 0.04f, 0.16f);                 // gancio
            b.colorf(0.13f, 0.12f, 0.12f);
        }
        // assi, boccole e ruote con razze e bordino (dentro la rotaia)
        for (int s = -1; s <= 1; s += 2) {
            float z = s * 0.36f;
            b.colorf(0.18f, 0.17f, 0.17f);
            b.box(0, wy, z, gauge * 2 + 0.1f, 0.04f, 0.04f);
            for (int q = -1; q <= 1; q += 2) {
                float x = q * gauge;
                b.colorf(0.24f, 0.23f, 0.22f);
                wheel(b, x, wy, z, wr, 0.035f);
                b.colorf(0.34f, 0.33f, 0.32f);
                wheel(b, x - q * 0.045f, wy, z, wr + 0.035f, 0.012f);
                b.colorf(0.42f, 0.4f, 0.37f);
                for (int k = 0; k < 6; k++) {
                    double an = k * Math.PI / 3;
                    float sy = (float) Math.cos(an) * wr * 0.5f, sz = (float) Math.sin(an) * wr * 0.5f;
                    b.box(x + q * 0.04f, wy + sy, z + sz, 0.012f, 0.025f + Math.abs(sy) * 1.6f, 0.025f + Math.abs(sz) * 1.6f);
                }
                b.colorf(0.4f, 0.24f, 0.13f);
                b.box(q * 0.31f, wy + 0.02f, z, 0.08f, 0.11f, 0.12f);         // boccola
            }
        }
        // leva del freno sul fianco destro
        b.colorf(0.2f, 0.19f, 0.18f);
        beamY(b, wt + 0.04f, yb - 0.05f, 0.2f, wt + 0.1f, yt + 0.12f, 0.34f, 0.022f);
        b.colorf(0.62f, 0.12f, 0.08f);
        b.ellipsoid(wt + 0.1f, yt + 0.14f, 0.345f, 0.04f, 0.04f, 0.04f, 6, 4);
    }

    /** Fascio di candelotti di dinamite legati con lo spago, su un bancale; la miccia esce in alto (scintilla a parte). */
    static Mesh dynamite() {
        MeshBuilder b = new MeshBuilder();
        b.colorf(0.5f, 0.36f, 0.22f);                                   // bancale
        for (int k = -1; k <= 1; k++) b.box(k * 0.3f, 0.04f, 0, 0.12f, 0.08f, 0.78f);
        b.colorf(0.62f, 0.46f, 0.28f);
        for (int k = -2; k <= 2; k++) b.box(0, 0.105f, k * 0.16f, 0.8f, 0.05f, 0.13f);
        float y0 = 0.13f, y1 = 0.86f, r = 0.075f;
        float[][] pos = {{0, 0}, {0.16f, 0}, {-0.16f, 0}, {0.08f, 0.14f}, {-0.08f, 0.14f}, {0.08f, -0.14f}, {-0.08f, -0.14f},
                {0.24f, 0.14f}, {-0.24f, 0.14f}, {0.24f, -0.14f}, {-0.24f, -0.14f}, {0, 0.28f}, {0, -0.28f}};
        for (int i = 0; i < pos.length; i++) {
            float x = pos[i][0], z = pos[i][1], h = y1 - 0.03f * (i % 3);
            float tone = 0.9f + 0.1f * ((i * 37) % 5) / 4f;
            b.colorf(0.78f * tone, 0.11f * tone, 0.08f * tone);
            b.cylinder(x, y0, h, z, r, r, 10, false, true);
            b.colorf(0.86f, 0.78f, 0.6f);                               // tappo di carta
            b.cylinder(x, h, h + 0.025f, z, r * 0.98f, r * 0.85f, 10, true, false);
            b.colorf(0.95f, 0.92f, 0.85f);                              // fascetta con la scritta (bianca)
            b.cylinder(x, y0 + 0.42f, y0 + 0.52f, z, r + 0.002f, r + 0.002f, 10, false, false);
        }
        b.colorf(0.72f, 0.6f, 0.38f);                                   // due giri di spago
        for (float y : new float[] {0.3f, 0.68f}) {
            float[][] ring = new float[16][];
            for (int i = 0; i < 16; i++) { double a = i * Math.PI * 2 / 16; ring[i] = new float[] {(float) Math.cos(a) * 0.335f, y, (float) Math.sin(a) * 0.37f}; }
            for (int i = 0; i < 16; i++) beamY(b, ring[i][0], y, ring[i][2], ring[(i + 1) % 16][0], y, ring[(i + 1) % 16][2], 0.018f);
        }
        b.colorf(0.12f, 0.1f, 0.08f);                                   // miccia: esce dal candelotto centrale e si piega
        float[][] fuse = {{0, y1 + 0.02f, 0}, {0.03f, y1 + 0.12f, 0.02f}, {0.09f, y1 + 0.2f, 0.05f}, {0.16f, y1 + 0.24f, 0.07f}};
        for (int i = 0; i + 1 < fuse.length; i++) beamY(b, fuse[i][0], fuse[i][1], fuse[i][2], fuse[i + 1][0], fuse[i + 1][1], fuse[i + 1][2], 0.014f);
        Mesh m = b.build(-1, false, true);
        m.spec = 0.25f;
        m.shine = 16f;
        return m;
    }

    /** Punta della miccia rispetto al centro del bancale. */
    static final float FUSE_X = 0.16f, FUSE_Y = 1.1f, FUSE_Z = 0.07f;

    /** Scintilla della miccia: nucleo e raggi, da disegnare molto emissiva e tremolante. */
    static Mesh spark() {
        MeshBuilder b = new MeshBuilder().colorf(1f, 0.9f, 0.5f);
        b.ellipsoid(0, 0, 0, 0.065f, 0.065f, 0.065f, 8, 5);
        b.colorf(1f, 0.6f, 0.2f);
        for (int k = 0; k < 7; k++) {
            double a = k * 2.4, e = (k % 3 - 1) * 0.6;
            float dx = (float) (Math.cos(a) * Math.cos(e)), dy = (float) Math.sin(e) + 0.4f, dz = (float) (Math.sin(a) * Math.cos(e));
            beamY(b, 0, 0, 0, dx * 0.2f, dy * 0.2f, dz * 0.2f, 0.011f);
        }
        return b.build(-1, false, true);
    }

    private static float[] bil(float[] a, float[] c, float[] d, float[] e, float u, float v) {
        float[] r = new float[3];
        for (int i = 0; i < 3; i++) {
            float lo = a[i] + (c[i] - a[i]) * u, hi = e[i] + (d[i] - e[i]) * u;
            r[i] = lo + (hi - lo) * v;
        }
        return r;
    }

    /** Ruota con l'asse lungo x: disco di raggio r e spessore 2w. */
    static void wheel(MeshBuilder b, float cx, float cy, float cz, float r, float w) {
        int n = 16;
        for (int i = 0; i < n; i++) {
            double a0 = 2 * Math.PI * i / n, a1 = 2 * Math.PI * (i + 1) / n;
            float y0 = cy + r * (float) Math.cos(a0), z0 = cz + r * (float) Math.sin(a0), y1 = cy + r * (float) Math.cos(a1), z1 = cz + r * (float) Math.sin(a1);
            float ny = (float) Math.cos((a0 + a1) / 2), nz = (float) Math.sin((a0 + a1) / 2);
            b.quad(cx - w, y0, z0, cx + w, y0, z0, cx + w, y1, z1, cx - w, y1, z1, 0, ny, nz, 0, 0, 1, 1);
            for (int s = -1; s <= 1; s += 2) {
                b.vert(cx + s * w, cy, cz, s, 0, 0, 0, 0); b.vert(cx + s * w, y0, z0, s, 0, 0, 0, 0); b.vert(cx + s * w, y1, z1, s, 0, 0, 0, 0);
            }
        }
    }

    /** Cilindro con l'asse lungo z, da z0 a z1, con i due tappi. */
    static void cylZ(MeshBuilder b, float x, float y, float z0, float z1, float r) {
        int n = 12;
        float s = Math.signum(z1 - z0);
        for (int i = 0; i < n; i++) {
            double a0 = 2 * Math.PI * i / n, a1 = 2 * Math.PI * (i + 1) / n;
            float x0 = x + r * (float) Math.cos(a0), y0 = y + r * (float) Math.sin(a0), x1 = x + r * (float) Math.cos(a1), y1 = y + r * (float) Math.sin(a1);
            float nx = (float) Math.cos((a0 + a1) / 2), ny = (float) Math.sin((a0 + a1) / 2);
            b.quad(x0, y0, z0, x1, y1, z0, x1, y1, z1, x0, y0, z1, nx, ny, 0, 0, 0, 1, 1);
            b.vert(x, y, z1, 0, 0, s, 0, 0); b.vert(x0, y0, z1, 0, 0, s, 0, 0); b.vert(x1, y1, z1, 0, 0, s, 0, 0);
            b.vert(x, y, z0, 0, 0, -s, 0, 0); b.vert(x1, y1, z0, 0, 0, -s, 0, 0); b.vert(x0, y0, z0, 0, 0, -s, 0, 0);
        }
    }

    /** Asta a sezione quadra da un punto all'altro. */
    static void beamY(MeshBuilder b, float x0, float y0, float z0, float x1, float y1, float z1, float h) {
        float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0, l = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (l < 1e-5f) return;
        dx /= l; dy /= l; dz /= l;
        float ux = -dz, uz = dx, ul = (float) Math.sqrt(ux * ux + uz * uz);
        if (ul < 1e-3f) { ux = 1; uz = 0; ul = 1; }
        ux /= ul; uz /= ul;
        float vx = dy * uz, vy = dz * ux - dx * uz, vz = -dy * ux;
        float[][] o = {{ux + vx, vy, uz + vz}, {-ux + vx, vy, -uz + vz}, {-ux - vx, -vy, -uz - vz}, {ux - vx, -vy, uz - vz}};
        for (int k = 0; k < 4; k++) {
            float[] a = o[k], c = o[(k + 1) % 4];
            float nx = a[0] + c[0], ny = a[1] + c[1], nz = a[2] + c[2], nl = (float) Math.sqrt(nx * nx + ny * ny + nz * nz) + 1e-6f;
            b.quad(x0 + a[0] * h, y0 + a[1] * h, z0 + a[2] * h, x0 + c[0] * h, y0 + c[1] * h, z0 + c[2] * h, x1 + c[0] * h, y1 + c[1] * h, z1 + c[2] * h,
                    x1 + a[0] * h, y1 + a[1] * h, z1 + a[2] * h, nx / nl, ny / nl, nz / nl, 0, 0, 1, 1);
        }
    }

    /** Il robot sta in piedi sulla nuvola, con i piedi un poco affondati nel soffice. */
    static final float CLOUD_RIDE = 0.34f;

    /**
     * Nuvola da cavalcare al posto del carrello: un cumulo di sbuffi bianchi più grandi al centro e piccoli ai bordi, piatto sotto
     * e gonfio sopra, con un velo azzurrino in ombra; davanti e dietro si sfrangia in batuffoli più piccoli.
     */
    static Mesh cloud() {
        MeshBuilder b = new MeshBuilder();
        java.util.Random r = new java.util.Random(4343);
        float[][] puffs = {
            {0, 0.2f, 0, 0.42f}, {-0.3f, 0.17f, 0.18f, 0.3f}, {0.3f, 0.17f, 0.18f, 0.3f}, {-0.3f, 0.17f, -0.2f, 0.3f}, {0.3f, 0.17f, -0.2f, 0.3f},
            {0, 0.22f, 0.4f, 0.3f}, {0, 0.2f, -0.45f, 0.32f}, {-0.48f, 0.12f, 0, 0.22f}, {0.48f, 0.12f, 0, 0.22f},
            {-0.18f, 0.12f, 0.62f, 0.18f}, {0.2f, 0.11f, 0.6f, 0.17f}, {-0.2f, 0.12f, -0.68f, 0.2f}, {0.18f, 0.1f, -0.75f, 0.16f}, {0, 0.1f, -0.92f, 0.12f}
        };
        for (float[] p : puffs) {
            int mk = b.mark();
            float t = 0.94f + 0.06f * r.nextFloat();
            b.colorf(t, t * 0.84f, t * 0.66f);                                     // dorata dal sole: si stacca dal pavimento di nuvole bianche
            b.ellipsoid(p[0], p[1], p[2], p[3], p[3] * 0.72f, p[3], 14, 9);
            b.rough(mk, p[3] * 0.08f, 6f, 90 + (int) (p[3] * 100), false);
        }
        b.colorf(0.86f, 0.62f, 0.56f);                                             // pancia in ombra, rosata
        b.ellipsoid(0, 0.06f, -0.05f, 0.55f, 0.08f, 0.82f, 16, 6);
        Mesh m = b.build(-1, false, true);
        m.spec = 0.02f; m.shine = 4f;
        return m;
    }

    /** Nuvoletta temporalesca bassa sul percorso (prende il posto della dinamite in cielo): sbuffi grigio piombo, pancia scura. */
    static Mesh thunderCloud() {
        MeshBuilder b = new MeshBuilder();
        java.util.Random r = new java.util.Random(77);
        float[][] puffs = {{0, 0.55f, 0, 0.34f}, {-0.26f, 0.48f, 0.05f, 0.24f}, {0.27f, 0.5f, -0.04f, 0.25f}, {0.05f, 0.7f, 0.06f, 0.24f}, {-0.1f, 0.5f, -0.22f, 0.2f}, {0.12f, 0.48f, 0.22f, 0.2f}};
        for (float[] p : puffs) {
            int mk = b.mark();
            float t = 0.36f + 0.1f * r.nextFloat();
            b.colorf(t, t * 1.02f, t * 1.1f);
            b.ellipsoid(p[0], p[1], p[2], p[3], p[3] * 0.75f, p[3], 12, 8);
            b.rough(mk, p[3] * 0.1f, 6f, 300 + (int) (p[3] * 100), false);
        }
        b.colorf(0.2f, 0.21f, 0.25f);
        b.ellipsoid(0, 0.36f, 0, 0.42f, 0.08f, 0.36f, 14, 6);
        Mesh m = b.build(-1, false, true);
        m.spec = 0.02f; m.shine = 4f;
        return m;
    }

    /** Fulmine a zig-zag sotto la nuvoletta, fino al pavimento (si disegna luminoso, a lampi). */
    static Mesh bolt() {
        MeshBuilder b = new MeshBuilder().color(255, 240, 120);
        float[][] p = {{0, 0.36f, 0}, {0.08f, 0.25f, 0}, {-0.05f, 0.18f, 0.02f}, {0.07f, 0.08f, 0}, {0, 0f, 0}};
        for (int i = 0; i + 1 < p.length; i++) {
            float[] a = p[i], c = p[i + 1];
            float w = 0.03f * (1 - i * 0.18f);
            b.vert(a[0] - w, a[1], a[2], 0, 0, 1, 0, 0); b.vert(a[0] + w, a[1], a[2], 0, 0, 1, 0, 0); b.vert(c[0], c[1], c[2], 0, 0, 1, 0, 0);
            b.vert(a[0], a[1], a[2] - w, 1, 0, 0, 0, 0); b.vert(a[0], a[1], a[2] + w, 1, 0, 0, 0, 0); b.vert(c[0], c[1], c[2], 1, 0, 0, 0, 0);
        }
        return b.build(-1, false, false);
    }

    /** Ostacolo della valanga: un mucchio di lame di ghiaccio azzurro che spuntano dalla neve. */
    static Mesh iceSpikes() {
        MeshBuilder b = new MeshBuilder();
        java.util.Random r = new java.util.Random(61);
        b.colorf(0.93f, 0.96f, 1f);
        b.ellipsoid(0, 0.08f, 0, 0.5f, 0.16f, 0.45f, 14, 6);                            // mucchietto di neve alla base
        for (int k = 0; k < 9; k++) {
            float a = k * 0.7f + r.nextFloat(), d = 0.1f + 0.28f * r.nextFloat(), h = 0.45f + 0.55f * r.nextFloat();
            float t = 0.75f + 0.25f * r.nextFloat();
            b.colorf(0.62f * t, 0.84f * t, 1f * t);
            float x = d * (float) Math.cos(a), z = d * (float) Math.sin(a);
            b.bipyramid(x, 0.12f + h * 0.4f, z, 0.07f + 0.05f * r.nextFloat(), h * 0.55f, 5);
        }
        Mesh m = b.build(-1, false, true);
        m.spec = 0.8f; m.shine = 60f;
        return m;
    }

    /** Ostacolo della lava: masso di lava raffreddata, nero, con le crepe incandescenti (le crepe si disegnano luminose a parte). */
    static Mesh lavaRock() {
        MeshBuilder b = new MeshBuilder();
        b.colorf(0.16f, 0.12f, 0.11f);
        b.rock(0, 0.32f, 0, 0.5f, 0.42f, 0.45f, 7171, 9, 7);
        b.rock(0.28f, 0.18f, 0.2f, 0.24f, 0.2f, 0.22f, 7272, 7, 5);
        Mesh m = b.build(-1, false, true);
        m.spec = 0.25f; m.shine = 20f;
        return m;
    }

    static Mesh lavaGlow() {
        MeshBuilder b = new MeshBuilder();
        java.util.Random r = new java.util.Random(73);
        for (int k = 0; k < 7; k++) {
            float a = k * 0.9f + r.nextFloat();
            b.colorf(1f, 0.45f + 0.25f * r.nextFloat(), 0.08f);
            float x = 0.43f * (float) Math.cos(a), z = 0.4f * (float) Math.sin(a), y = 0.12f + 0.4f * r.nextFloat();
            b.ellipsoid(x, y, z, 0.05f, 0.09f + 0.05f * r.nextFloat(), 0.05f, 6, 4);
        }
        b.colorf(1f, 0.55f, 0.1f);
        b.ellipsoid(0, 0.02f, 0, 0.62f, 0.04f, 0.56f, 14, 4);                           // pozza incandescente sotto
        return b.build(-1, false, false);
    }

    /** Ostacolo dello sciame: un roveto spinoso e fitto, con qualche fiore. */
    static Mesh bramble() {
        MeshBuilder b = new MeshBuilder();
        java.util.Random r = new java.util.Random(83);
        for (int k = 0; k < 7; k++) {
            float t = 0.7f + 0.3f * r.nextFloat();
            b.colorf(0.2f * t, 0.38f * t, 0.14f * t);
            int mk = b.mark();
            b.ellipsoid((r.nextFloat() - 0.5f) * 0.6f, 0.25f + 0.25f * r.nextFloat(), (r.nextFloat() - 0.5f) * 0.5f, 0.26f, 0.24f, 0.24f, 10, 7);
            b.rough(mk, 0.05f, 6f, 90 + k);
        }
        b.colorf(0.42f, 0.28f, 0.16f);
        for (int k = 0; k < 26; k++) {                                                     // spine
            float a = r.nextFloat() * 6.283f, e = (r.nextFloat() - 0.2f) * 1.2f, rr = 0.42f;
            float x = rr * (float) Math.cos(a) * (float) Math.cos(e), y = 0.32f + rr * 0.8f * (float) Math.sin(e), z = rr * (float) Math.sin(a) * (float) Math.cos(e);
            b.bipyramid(x, y, z, 0.02f, 0.07f, 4);
        }
        for (int k = 0; k < 5; k++) {
            b.colorf(1f, 0.55f + 0.4f * r.nextFloat(), 0.75f);
            float a = r.nextFloat() * 6.283f;
            b.ellipsoid(0.38f * (float) Math.cos(a), 0.45f + 0.15f * r.nextFloat(), 0.34f * (float) Math.sin(a), 0.05f, 0.04f, 0.05f, 6, 4);
        }
        Mesh m = b.build(-1, false, true);
        m.spec = 0.08f; m.shine = 8f;
        return m;
    }

    /** Fronte della valanga: un muro di sbuffi di neve, più fitti e grossi in basso, largo quanto il sentiero e oltre. */
    static Mesh snowWall() {
        MeshBuilder b = new MeshBuilder();
        java.util.Random r = new java.util.Random(91);
        for (int k = 0; k < 70; k++) {
            float y = (float) Math.pow(r.nextFloat(), 1.6f) * 4.6f, rad = 0.55f + 0.9f * r.nextFloat() * (1.2f - y / 6f);
            float x = (r.nextFloat() - 0.5f) * 8.4f, z = (r.nextFloat() - 0.7f) * 1.6f - y * 0.18f;
            float t = 0.9f + 0.1f * r.nextFloat();
            b.colorf(t * 0.95f, t * 0.97f, t);
            int mk = b.mark();
            b.ellipsoid(x, y + rad * 0.4f, z, rad, rad * 0.85f, rad, 10, 7);
            b.rough(mk, rad * 0.12f, 3f, 500 + k);
        }
        Mesh m = b.build(-1, false, true);
        m.spec = 0.05f; m.shine = 6f;
        return m;
    }

    /** Sbuffo di neve polverosa che la valanga spara davanti a sé (semitrasparente). */
    static Mesh snowPuff() {
        MeshBuilder b = new MeshBuilder();
        b.colorf(1f, 1f, 1f);
        b.ellipsoid(0, 0, 0, 1f, 0.8f, 1f, 10, 7);
        return b.build(-1, true, true);
    }

    /** Lingua di lava che avanza: una lastra incandescente con qualche crosta scura (si disegna luminosa). */
    static Mesh lavaSheet() {
        MeshBuilder b = new MeshBuilder();
        java.util.Random r = new java.util.Random(97);
        b.colorf(1f, 0.42f, 0.06f);
        b.quad(-4.2f, 0, -40f, 4.2f, 0, -40f, 4.2f, 0, 0.4f, -4.2f, 0, 0.4f, 0, 1, 0, 0, 0, 1, 1);
        for (int k = 0; k < 9; k++) {                                              // fronte gonfio
            b.colorf(1f, 0.55f + 0.2f * r.nextFloat(), 0.1f);
            b.ellipsoid(-4f + k * 1f, 0.15f, 0.2f, 0.6f, 0.35f, 0.5f, 10, 6);
        }
        b.colorf(0.25f, 0.1f, 0.05f);
        for (int k = 0; k < 40; k++) b.ellipsoid((r.nextFloat() - 0.5f) * 8f, 0.03f, -r.nextFloat() * 38f, 0.2f + 0.4f * r.nextFloat(), 0.04f, 0.2f + 0.5f * r.nextFloat(), 6, 3);
        return b.build(-1, false, false);
    }
}
