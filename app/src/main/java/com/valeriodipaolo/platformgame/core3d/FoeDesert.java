package com.valeriodipaolo.platformgame.core3d;

/**
 * Nemici del deserto modellati "dal vero": lo scorpione (uno scorpione paglierino del deserto, a placche di chitina lucida,
 * zampe articolate, pedipalpi con le chele e la coda a cinque segmenti col pungiglione) e l'avvoltoio (piumaggio a penne
 * sovrapposte, collare di piume chiare, collo e testa nudi e rugosi, becco adunco, ali larghe con le remiganti primarie
 * aperte a dita). Le misure e i punti d'aggancio sono quelli dei modelli di prima: animazioni e collisioni non cambiano.
 */
final class FoeDesert {
    private FoeDesert() { }

    // ------------------------------------------------------------------ utilità

    private static final float[] T = new float[3];

    private static float hash(float x, float y) {
        float h = (float) Math.sin(x * 127.1f + y * 311.7f) * 43758.5453f;
        return h - (float) Math.floor(h);
    }

    private static void col(MeshBuilder b, float[] c, float k) { b.colorf(Math.min(1f, c[0] * k), Math.min(1f, c[1] * k), Math.min(1f, c[2] * k)); }

    private static float[] norm(float x, float y, float z) {
        float l = (float) Math.sqrt(x * x + y * y + z * z);
        if (l < 1e-9f) return new float[] {0, 1, 0};
        return new float[] {x / l, y / l, z / l};
    }

    private static float[] cross(float[] a, float[] c) {
        return new float[] {a[1] * c[2] - a[2] * c[1], a[2] * c[0] - a[0] * c[2], a[0] * c[1] - a[1] * c[0]};
    }

    /**
     * Penna: goccia appiattita che parte da p, va lungo d per len, larga w (lungo s), con la faccia verso n; la radice ha il
     * colore c0 e la punta c1, la rachide scura in mezzo. Due facce (sopra e sotto) così si vede da tutte le parti.
     */
    static void feather(MeshBuilder b, float[] p, float[] d, float[] s, float[] n, float len, float w, float[] c0, float[] c1, float lift) {
        float[] q1 = {p[0] + d[0] * len * 0.38f + s[0] * w, p[1] + d[1] * len * 0.38f + s[1] * w + n[1] * lift * 0.5f, p[2] + d[2] * len * 0.38f + s[2] * w};
        float[] q2 = {p[0] + d[0] * len * 0.38f - s[0] * w, p[1] + d[1] * len * 0.38f - s[1] * w + n[1] * lift * 0.5f, p[2] + d[2] * len * 0.38f - s[2] * w};
        float[] tip = {p[0] + d[0] * len + n[0] * lift, p[1] + d[1] * len + n[1] * lift, p[2] + d[2] * len + n[2] * lift};
        float[] mid = {p[0] + d[0] * len * 0.55f + n[0] * (lift * 0.6f + 0.004f), p[1] + d[1] * len * 0.55f + n[1] * (lift * 0.6f + 0.004f), p[2] + d[2] * len * 0.55f + n[2] * (lift * 0.6f + 0.004f)};
        for (int f = 0; f < 2; f++) {
            float sg = f == 0 ? 1 : -1;
            float nx = n[0] * sg, ny = n[1] * sg, nz = n[2] * sg;
            float[][] tri = {p, q1, mid, p, mid, q2, q1, tip, mid, mid, tip, q2};
            for (int k = 0; k < tri.length; k++) {
                float[] v = tri[k];
                boolean root = v == p, isTip = v == tip;
                float t = root ? 0 : isTip ? 1 : 0.5f;
                b.colorf(c0[0] + (c1[0] - c0[0]) * t, c0[1] + (c1[1] - c0[1]) * t, c0[2] + (c1[2] - c0[2]) * t);
                if (v == mid) b.colorf(c0[0] * 0.7f + c1[0] * 0.15f, c0[1] * 0.7f + c1[1] * 0.15f, c0[2] * 0.7f + c1[2] * 0.15f);   // rachide
                b.vert(v[0], v[1], v[2], nx, ny, nz, 0, 0);
            }
        }
    }

    /** Punto di una curva di Catmull-Rom che passa per i punti c, al parametro u (0..1 su tutta la curva). */
    private static float[] curve(float[][] c, float u) {
        int n = c.length - 1;
        float f = Math.max(0, Math.min(0.9999f, u)) * n;
        int i = (int) f;
        float t = f - i;
        float[] p0 = c[Math.max(0, i - 1)], p1 = c[i], p2 = c[i + 1], p3 = c[Math.min(n, i + 2)];
        float[] o = new float[3];
        for (int k = 0; k < 3; k++) {
            o[k] = 0.5f * (2 * p1[k] + (-p0[k] + p2[k]) * t + (2 * p0[k] - 5 * p1[k] + 4 * p2[k] - p3[k]) * t * t + (-p0[k] + 3 * p1[k] - 3 * p2[k] + p3[k]) * t * t * t);
        }
        return o;
    }

    /** Segmento bombato di chitina lungo la curva tra u0 e u1, raggio massimo r, con un anello scuro all'articolazione. */
    private static void segment(MeshBuilder b, float[][] c, float u0, float u1, float r, float[] col, float[] joint) {
        int n = 6;
        float[][] pts = new float[n][];
        float[] rr = new float[n];
        for (int i = 0; i < n; i++) {
            float t = i / (float) (n - 1);
            pts[i] = curve(c, u0 + (u1 - u0) * t);
            rr[i] = r * (0.8f + 0.2f * (float) Math.sin(Math.PI * Math.min(1f, 0.15f + t * 0.85f)));
        }
        rr[0] *= 0.85f;
        FoeModels.loft(b, pts, rr, rr, 1, 0, 0, 10, 1f, FoeModels.along(joint, col), 0, 0);
    }

    // ------------------------------------------------------------------ scorpione

    private static final float[] STRAW = FoeModels.rgb(196, 152, 82), STRAW_D = FoeModels.rgb(128, 90, 46), STRAW_L = FoeModels.rgb(222, 190, 124),
            RUST = FoeModels.rgb(150, 86, 40), STING = FoeModels.rgb(34, 22, 16);

    /** Corpo: prosoma con gli occhi, sette placche del mesosoma sovrapposte, sterniti chiari sotto, pedipalpi con le chele. */
    static Mesh scorpionBody() {
        MeshBuilder b = new MeshBuilder();
        // prosoma (cefalotorace): scudo piatto un po' più stretto davanti, con il solco mediano e il tubercolo degli occhi
        int mk = b.mark();
        col(b, STRAW, 1f);
        b.ellipsoid(0, 0.245f, 0.2f, 0.135f, 0.055f, 0.13f, 16, 8);
        col(b, STRAW_D, 1f);
        b.ellipsoid(0, 0.293f, 0.2f, 0.012f, 0.008f, 0.1f, 6, 3);
        b.rough(mk, 0.006f, 18f, 31, false);
        col(b, STRAW_D, 0.8f);
        b.ellipsoid(0, 0.3f, 0.215f, 0.03f, 0.018f, 0.03f, 8, 4);
        b.colorf(0.03f, 0.03f, 0.03f);                                      // occhi mediani e i grappoli laterali, neri lucidi
        b.ellipsoid(-0.014f, 0.315f, 0.222f, 0.009f, 0.009f, 0.009f, 6, 4);
        b.ellipsoid(0.014f, 0.315f, 0.222f, 0.009f, 0.009f, 0.009f, 6, 4);
        for (int s = -1; s <= 1; s += 2) for (int k = 0; k < 3; k++) b.ellipsoid(s * (0.085f + 0.012f * k), 0.27f, 0.305f - 0.012f * k, 0.006f, 0.006f, 0.006f, 5, 3);
        // mesosoma: sette tergiti, ognuno con il bordo posteriore più scuro che si sovrappone al successivo
        for (int i = 0; i < 7; i++) {
            float z = 0.075f - i * 0.055f, w = 0.15f - Math.abs(i - 2.5f) * 0.012f - (i > 4 ? (i - 4) * 0.012f : 0);
            float tone = 0.95f + 0.1f * hash(i, 3);
            int m2 = b.mark();
            col(b, STRAW, tone);
            b.ellipsoid(0, 0.24f, z, w, 0.06f, 0.036f, 16, 7);
            col(b, STRAW_D, tone);
            b.ellipsoid(0, 0.248f, z - 0.026f, w * 0.96f, 0.05f, 0.014f, 14, 5);
            col(b, STRAW_D, 0.8f);                                           // carene: tre creste longitudinali
            for (int k = -1; k <= 1; k++) b.ellipsoid(k * w * 0.42f, 0.294f - Math.abs(k) * 0.008f, z, 0.008f, 0.006f, 0.028f, 5, 3);
            b.rough(m2, 0.004f, 22f, 40 + i, false);
        }
        col(b, STRAW_L, 1f);                                                 // ventre: sterniti chiari
        b.ellipsoid(0, 0.2f, -0.08f, 0.12f, 0.03f, 0.2f, 14, 6);
        // pedipalpi: femore, patella, mano bombata e due dita ricurve, la punta delle dita scura
        for (int s = -1; s <= 1; s += 2) {
            float[][] arm = {{s * 0.1f, 0.25f, 0.29f}, {s * 0.2f, 0.27f, 0.34f}, {s * 0.28f, 0.28f, 0.4f}, {s * 0.27f, 0.275f, 0.47f}, {s * 0.24f, 0.27f, 0.52f}};
            FoeModels.loft(b, arm, new float[] {0.022f, 0.028f, 0.02f, 0.03f, 0.028f}, new float[] {0.022f, 0.026f, 0.019f, 0.028f, 0.026f}, 0, 1, 0, 8, 1f, FoeModels.along(STRAW, STRAW_D), 0, 0);
            int m3 = b.mark();
            col(b, STRAW, 0.92f);
            b.ellipsoid(s * 0.235f, 0.27f, 0.585f, 0.062f, 0.042f, 0.085f, 14, 8);    // la mano
            col(b, STRAW_D, 1f);
            for (int k = 0; k < 4; k++) b.ellipsoid(s * (0.205f + 0.02f * k), 0.31f - Math.abs(k - 1.5f) * 0.006f, 0.585f, 0.006f, 0.005f, 0.06f, 5, 3);   // carene della mano
            b.rough(m3, 0.004f, 25f, 60 + s, false);
            float[][] fixedF = {{s * 0.215f, 0.265f, 0.65f}, {s * 0.205f, 0.262f, 0.7f}, {s * 0.19f, 0.258f, 0.75f}, {s * 0.17f, 0.255f, 0.785f}};
            FoeModels.loft(b, fixedF, new float[] {0.02f, 0.015f, 0.01f, 0.003f}, new float[] {0.017f, 0.013f, 0.009f, 0.003f}, 0, 1, 0, 7, 1f, FoeModels.along(STRAW_D, RUST), 0, 0);
            float[][] moveF = {{s * 0.255f, 0.27f, 0.645f}, {s * 0.255f, 0.268f, 0.7f}, {s * 0.235f, 0.262f, 0.75f}, {s * 0.205f, 0.257f, 0.785f}};
            FoeModels.loft(b, moveF, new float[] {0.02f, 0.015f, 0.01f, 0.003f}, new float[] {0.017f, 0.013f, 0.009f, 0.003f}, 0, 1, 0, 7, 1f, FoeModels.along(STRAW_D, RUST), 0, 0);
        }
        return done(b, 0.42f, 34f);
    }

    /** Zampa: coxa, femore che sale, patella, tibia che scende e tarso a punta; si estende verso x positivo dall'origine. */
    static Mesh scorpionLeg() {
        MeshBuilder b = new MeshBuilder();
        float[][] c = {{0, 0, 0}, {0.08f, 0.06f, 0}, {0.17f, 0.1f, 0.008f}, {0.24f, 0.07f, 0.014f}, {0.3f, -0.06f, 0.018f}, {0.34f, -0.2f, 0.02f}};
        FoeModels.loft(b, c, new float[] {0.032f, 0.028f, 0.025f, 0.021f, 0.014f, 0.004f}, new float[] {0.03f, 0.026f, 0.023f, 0.019f, 0.013f, 0.004f}, 0, 1, 0, 7, 1f, FoeModels.along(STRAW, STRAW_L), 0, 0);
        col(b, STRAW_D, 0.9f);                                               // articolazioni più scure
        for (int i = 1; i < 5; i++) b.ellipsoid(c[i][0], c[i][1], c[i][2], 0.016f - i * 0.002f, 0.016f - i * 0.002f, 0.016f - i * 0.002f, 6, 4);
        return done(b, 0.38f, 30f);
    }

    /** Coda (metasoma): cinque segmenti a botte sempre più scuri, la vescicola del veleno e il pungiglione nero ricurvo. */
    static Mesh scorpionTail() {
        MeshBuilder b = new MeshBuilder();
        float[][] c = {{0, 0.25f, -0.32f}, {0, 0.38f, -0.48f}, {0, 0.58f, -0.54f}, {0, 0.78f, -0.44f}, {0, 0.88f, -0.26f}, {0, 0.86f, -0.12f}};
        float[] u = {0f, 0.17f, 0.34f, 0.5f, 0.66f, 0.82f};
        for (int i = 0; i < 5; i++) {
            float[] cc = new float[3];
            FoeModels.mix(STRAW, RUST, i / 5f, cc);
            segment(b, c, u[i] - 0.006f, u[i + 1] + 0.004f, 0.07f - i * 0.005f, cc, STRAW_D);
        }
        // vescicola: bulbo gonfio orientato lungo la curva, poi l'aculeo che piega in giù
        float[] p = curve(c, 0.86f), q = curve(c, 0.97f);
        float[][] ves = {p, {(p[0] + q[0]) / 2, (p[1] + q[1]) / 2 + 0.005f, (p[2] + q[2]) / 2}, q};
        FoeModels.loft(b, ves, new float[] {0.045f, 0.06f, 0.03f}, new float[] {0.045f, 0.058f, 0.03f}, 1, 0, 0, 10, 1f, FoeModels.along(RUST, STRAW_D), 0, 0);
        float[][] sting = {q, {q[0], q[1] - 0.03f, q[2] + 0.05f}, {q[0], q[1] - 0.08f, q[2] + 0.07f}, {q[0], q[1] - 0.12f, q[2] + 0.055f}};
        FoeModels.loft(b, sting, new float[] {0.022f, 0.014f, 0.007f, 0.001f}, new float[] {0.022f, 0.014f, 0.007f, 0.001f}, 1, 0, 0, 7, 1f, FoeModels.along(STRAW_D, STING), 0, 0);
        return done(b, 0.45f, 36f);
    }

    // ------------------------------------------------------------------ avvoltoio

    private static final float[] PLUME_D = FoeModels.rgb(44, 32, 25), PLUME = FoeModels.rgb(78, 58, 42), PLUME_L = FoeModels.rgb(128, 98, 70),
            RUFF = FoeModels.rgb(232, 222, 202), SKIN = FoeModels.rgb(176, 98, 86), SKIN_D = FoeModels.rgb(120, 62, 56), BEAK = FoeModels.rgb(226, 214, 180);

    /** Corpo: piumaggio a penne sovrapposte sul dorso e sui fianchi, collare chiaro, collo e testa nudi, becco adunco, coda a ventaglio. */
    static Mesh vultureBody() {
        MeshBuilder b = new MeshBuilder();
        float[][] c = {{0, 0.01f, -0.42f}, {0, 0.02f, -0.3f}, {0, 0.02f, -0.14f}, {0, 0.01f, 0.04f}, {0, 0.03f, 0.18f}, {0, 0.07f, 0.29f}, {0, 0.1f, 0.34f}};
        float[] rx = {0.03f, 0.15f, 0.24f, 0.27f, 0.24f, 0.17f, 0.08f}, ry = {0.03f, 0.13f, 0.21f, 0.24f, 0.22f, 0.15f, 0.07f};
        FoeModels.loft(b, c, rx, ry, 0, 1, 0, 16, 0.9f, FoeModels.ud(PLUME, PLUME_D), 0, 0);
        // manto di penne: file sfalsate dalla nuca alla coda, rivolte all'indietro, più chiare ai bordi (copritrici)
        for (int i = 1; i < c.length - 1; i++) {
            for (float zi = 0; zi < 1f; zi += 0.34f) {
                float[] cc = {c[i][0], c[i][1] + (c[i + 1][1] - c[i][1]) * zi, c[i][2] + (c[i + 1][2] - c[i][2]) * zi};
                float rxa = rx[i] + (rx[i + 1] - rx[i]) * zi, rya = ry[i] + (ry[i + 1] - ry[i]) * zi;
                int around = 20;
                for (int k = 0; k < around; k++) {
                    float h = hash(i * 7 + zi * 13, k);
                    double a = (k + ((int) (zi * 3) % 2) * 0.5 + (h - 0.5) * 0.5) * 2 * Math.PI / around;
                    float ca = (float) Math.cos(a), sa = (float) Math.sin(a);
                    if (sa < -0.55f) continue;                                   // sotto il ventre niente penne in rilievo
                    float[] p = {cc[0] + ca * rxa * 0.98f, cc[1] + sa * rya * 0.98f, cc[2]};
                    float[] n = norm(ca / Math.max(rxa, 0.01f), sa / Math.max(rya, 0.01f), 0);
                    float[] d = norm(-n[0] * 0.15f, -n[1] * 0.15f - 0.05f, -1f);
                    float[] s = norm(cross(n, d)[0], cross(n, d)[1], cross(n, d)[2]);
                    float len = 0.09f + 0.04f * h + (i < 3 ? 0.04f : 0);
                    float[] c0 = new float[3], c1 = new float[3];
                    float t = 0.15f + 0.6f * (sa * 0.5f + 0.5f);
                    FoeModels.mix(PLUME, PLUME_D, t, c0);
                    FoeModels.mix(c0, PLUME_L, 0.45f + 0.2f * h, c1);
                    feather(b, p, d, s, n, len, 0.03f + 0.012f * h, c0, c1, 0.012f);
                }
            }
        }
        // collare: piumino chiaro arruffato alla base del collo
        for (int k = 0; k < 36; k++) {
            double a = k * 2 * Math.PI / 36;
            float ca = (float) Math.cos(a), sa = (float) Math.sin(a), h = hash(k, 91);
            float[] p = {ca * 0.13f, 0.1f + sa * 0.11f, 0.3f};
            float[] n = norm(ca, sa, 0.2f);
            float[] d = norm(ca * 0.8f, sa * 0.8f, -0.6f);
            float[] s = norm(cross(n, d)[0], cross(n, d)[1], cross(n, d)[2]);
            float[] c0 = new float[3];
            FoeModels.mix(RUFF, PLUME_L, 0.35f * h, c0);
            feather(b, p, d, s, n, 0.09f + 0.04f * h, 0.035f, c0, RUFF, 0.01f);
        }
        // collo nudo e rugoso, testa calva rossastra con le pieghe, occhio scuro sotto l'arcata
        int mk = b.mark();
        float[][] neck = {{0, 0.1f, 0.3f}, {0, 0.16f, 0.37f}, {0, 0.22f, 0.43f}, {0, 0.25f, 0.48f}};
        FoeModels.loft(b, neck, new float[] {0.065f, 0.05f, 0.045f, 0.05f}, new float[] {0.065f, 0.05f, 0.045f, 0.05f}, 0, 1, 0, 10, 1f, FoeModels.along(SKIN_D, SKIN), 0, 0);
        FoeModels.ballZ(b, 0, 0.265f, 0.53f, 0.068f, 0.066f, 0.09f, 14, 9, FoeModels.ud(SKIN_D, SKIN), 0, 0);
        b.rough(mk, 0.007f, 30f, 77, false);
        col(b, SKIN_D, 0.85f);
        for (int s = -1; s <= 1; s += 2) {
            b.ellipsoid(s * 0.048f, 0.3f, 0.545f, 0.02f, 0.012f, 0.03f, 6, 4);          // arcata sopraccigliare
            b.colorf(0.55f, 0.32f, 0.16f);
            b.ellipsoid(s * 0.058f, 0.285f, 0.55f, 0.014f, 0.014f, 0.012f, 8, 5);        // iride bruna
            b.colorf(0.02f, 0.02f, 0.02f);
            b.ellipsoid(s * 0.066f, 0.286f, 0.553f, 0.007f, 0.008f, 0.006f, 6, 4);
        }
        // becco: cera grigia, poi il becco avorio che si incurva in un uncino scuro
        b.colorf(0.5f, 0.46f, 0.44f);
        b.ellipsoid(0, 0.266f, 0.605f, 0.034f, 0.03f, 0.03f, 10, 6);
        float[][] beak = {{0, 0.265f, 0.61f}, {0, 0.266f, 0.65f}, {0, 0.262f, 0.685f}, {0, 0.245f, 0.705f}, {0, 0.222f, 0.7f}};
        FoeModels.loft(b, beak, new float[] {0.026f, 0.021f, 0.016f, 0.01f, 0.003f}, new float[] {0.03f, 0.025f, 0.02f, 0.012f, 0.003f}, 0, 1, 0, 8, 1f, FoeModels.along(BEAK, FoeModels.rgb(70, 60, 50)), 0, 0);
        b.colorf(0.1f, 0.08f, 0.07f);
        for (int s = -1; s <= 1; s += 2) b.ellipsoid(s * 0.017f, 0.272f, 0.625f, 0.004f, 0.006f, 0.01f, 5, 3);   // narici
        // coda: ventaglio di timoniere scure
        for (int k = 0; k < 11; k++) {
            float ang = (k - 5) * 0.075f, h = hash(k, 17);
            float[] p = {(k - 5) * 0.012f, 0.02f + (k % 2) * 0.004f, -0.38f};
            float[] d = norm((float) Math.sin(ang), -0.08f, -(float) Math.cos(ang));
            float[] n = {0, 1, 0}, s = norm(cross(n, d)[0], cross(n, d)[1], cross(n, d)[2]);
            feather(b, p, d, s, n, 0.26f + 0.04f * h, 0.04f, PLUME_D, PLUME, 0.01f);
        }
        // zampe raccolte sotto il ventre, grigio rosate, con gli artigli scuri
        for (int s = -1; s <= 1; s += 2) {
            FoeModels.loft(b, new float[][] {{s * 0.07f, -0.12f, 0.02f}, {s * 0.075f, -0.19f, 0.05f}, {s * 0.075f, -0.22f, 0.09f}}, new float[] {0.025f, 0.02f, 0.016f}, new float[] {0.025f, 0.02f, 0.016f}, 0, 1, 0, 7, 1f, FoeModels.flat(FoeModels.rgb(150, 132, 126)), 0, 0);
            b.colorf(0.12f, 0.1f, 0.09f);
            for (int k = -1; k <= 1; k++) b.ellipsoid(s * 0.075f + k * 0.018f, -0.225f, 0.12f, 0.006f, 0.006f, 0.02f, 5, 3);
        }
        return done(b, 0.04f, 6f);
    }

    /**
     * Ala destra (si estende verso x positivo dall'attaccatura): bordo d'attacco del braccio, una fila di remiganti secondarie
     * verso dietro, sette primarie lunghe e separate a dita alla punta, sopra due file di copritrici più chiare.
     */
    static Mesh vultureWing() {
        MeshBuilder b = new MeshBuilder();
        float[][] arm = {{0, 0, 0.06f}, {0.3f, 0.03f, 0.11f}, {0.62f, 0.06f, 0.1f}, {0.86f, 0.07f, 0.03f}, {1.0f, 0.06f, -0.02f}};
        FoeModels.loft(b, arm, new float[] {0.07f, 0.055f, 0.04f, 0.03f, 0.018f}, new float[] {0.05f, 0.04f, 0.03f, 0.022f, 0.012f}, 0, 1, 0, 8, 1f, FoeModels.ud(PLUME_D, PLUME), 0, 0);
        float[] up = {0, 1, 0};
        float[] tipG = FoeModels.rgb(96, 88, 82);
        // remiganti secondarie: dal corpo al polso, verso dietro, punta grigiastra (le "argentate" degli avvoltoi)
        for (int k = 0; k < 15; k++) {
            float t = k / 14f, h = hash(k, 5);
            float[] p = curve(arm, t * 0.82f);
            float[] d = norm(0.12f * t, -0.02f, -1f);
            float[] s = norm(cross(up, d)[0], cross(up, d)[1], cross(up, d)[2]);
            feather(b, new float[] {p[0], p[1] - 0.01f, p[2] - 0.02f}, d, s, up, 0.4f + 0.04f * h - 0.06f * t, 0.05f, PLUME_D, tipG, -0.015f);
        }
        // primarie: sette penne lunghe che si aprono a ventaglio dal polso, separate in punta
        for (int k = 0; k < 7; k++) {
            float t = k / 6f, h = hash(k, 9);
            float[] p = curve(arm, 0.82f + 0.17f * t);
            double ang = -1.25 + 1.05 * t;                                       // dalla verticale all'indietro fino in avanti verso l'esterno
            float[] d = norm((float) Math.cos(ang), 0.02f * t, (float) Math.sin(ang));
            float[] s = norm(cross(up, d)[0], cross(up, d)[1], cross(up, d)[2]);
            feather(b, p, d, s, up, 0.42f + 0.1f * (float) Math.sin(t * Math.PI) + 0.03f * h, 0.042f, PLUME_D, FoeModels.rgb(30, 24, 20), 0.03f * t);
        }
        // copritrici: due file più corte e più chiare sopra la radice delle remiganti
        for (int row = 0; row < 2; row++) {
            int n = 13 - row * 2;
            for (int k = 0; k < n; k++) {
                float t = k / (float) (n - 1), h = hash(k + row * 31, 2);
                float[] p = curve(arm, t * 0.86f);
                float[] d = norm(0.15f * t, -0.03f, -1f);
                float[] s = norm(cross(up, d)[0], cross(up, d)[1], cross(up, d)[2]);
                float[] c0 = row == 0 ? PLUME : PLUME_D, c1 = new float[3];
                FoeModels.mix(c0, PLUME_L, row == 0 ? 0.5f + 0.2f * h : 0.25f, c1);
                feather(b, new float[] {p[0], p[1] + 0.012f + row * 0.004f, p[2] - row * 0.05f}, d, s, up, (row == 0 ? 0.12f : 0.2f) + 0.03f * h, 0.045f, c0, c1, 0.01f);
            }
        }
        return done(b, 0.04f, 6f);
    }

    // ------------------------------------------------------------------ drone della fabbrica

    private static final float[] SAFETY = FoeModels.rgb(255, 150, 20), CARBON = FoeModels.rgb(34, 36, 40), STEEL = FoeModels.rgb(150, 156, 164);

    /**
     * Drone quadricottero ben visibile: scocca arancione di sicurezza con le bande nere, quattro bracci in carbonio con i motori,
     * pattini d'atterraggio, luci di posizione rossa e verde. (La lente e le eliche sono mesh a parte.)
     */
    static Mesh droneBody() {
        MeshBuilder b = new MeshBuilder();
        col(b, SAFETY, 1f);
        b.roundBox(0, 0, 0, 0.42f, 0.16f, 0.5f, 0.07f);
        col(b, SAFETY, 1.08f);
        b.ellipsoid(0, 0.08f, 0.02f, 0.18f, 0.08f, 0.22f, 14, 7);               // cupola
        col(b, CARBON, 1f);
        for (int k = -1; k <= 1; k += 2) b.box(0, 0.002f, k * 0.12f, 0.43f, 0.1f, 0.04f);    // bande nere
        b.box(0, -0.085f, 0, 0.3f, 0.03f, 0.36f);                                  // pancia con le batterie
        for (int i = 0; i < 4; i++) {
            double a2 = i * Math.PI / 2 + Math.PI / 4;
            float cx = (float) Math.cos(a2), cz = (float) Math.sin(a2);
            col(b, CARBON, 1f);
            FoeModels.tube(b, cx * 0.15f, 0.01f, cz * 0.15f, cx * 0.42f, 0.03f, cz * 0.42f, 0.03f, 0.022f, 8, FoeModels.flat(CARBON));
            col(b, STEEL, 1f);
            b.cylinder(cx * 0.42f, -0.02f, 0.07f, cz * 0.42f, 0.055f, 0.05f, 12, true, true);   // motore
            col(b, SAFETY, 1f);
            b.cylinder(cx * 0.42f, -0.045f, -0.02f, cz * 0.42f, 0.06f, 0.06f, 12, false, true); // ghiera arancione
        }
        col(b, STEEL, 0.8f);                                                       // pattini
        for (int s = -1; s <= 1; s += 2) {
            FoeModels.tube(b, s * 0.12f, -0.09f, 0.1f, s * 0.16f, -0.22f, 0.12f, 0.012f, 0.012f, 6, FoeModels.flat(STEEL));
            FoeModels.tube(b, s * 0.12f, -0.09f, -0.1f, s * 0.16f, -0.22f, -0.12f, 0.012f, 0.012f, 6, FoeModels.flat(STEEL));
            FoeModels.tube(b, s * 0.16f, -0.22f, -0.2f, s * 0.16f, -0.22f, 0.2f, 0.014f, 0.014f, 6, FoeModels.flat(STEEL));
        }
        return done(b, 0.55f, 40f);
    }

    /** Elica: due pale sottili e un disco semitrasparente che dà l'idea della rotazione veloce. */
    static Mesh droneRotor() {
        MeshBuilder b = new MeshBuilder();
        col(b, CARBON, 1f);
        b.box(0, 0, 0, 0.36f, 0.012f, 0.045f);
        col(b, STEEL, 1f);
        b.cylinder(0, -0.01f, 0.02f, 0, 0.018f, 0.018f, 8, true, false);
        return done(b, 0.2f, 10f);
    }

    /** Disco sfocato dell'elica che gira (semitrasparente). */
    static Mesh droneBlur() {
        MeshBuilder b = new MeshBuilder();
        b.colorf(0.8f, 0.82f, 0.86f);
        int seg = 20;
        for (int k = 0; k < seg; k++) {
            double a0 = k * 2 * Math.PI / seg, a1 = (k + 1) * 2 * Math.PI / seg;
            b.vert(0, 0.004f, 0, 0, 1, 0, 0, 0);
            b.vert(0.19f * (float) Math.cos(a0), 0.004f, 0.19f * (float) Math.sin(a0), 0, 1, 0, 0, 0);
            b.vert(0.19f * (float) Math.cos(a1), 0.004f, 0.19f * (float) Math.sin(a1), 0, 1, 0, 0, 0);
        }
        Mesh m = b.build(-1, true, true);
        m.spec = 0; m.shine = 4f;
        return m;
    }

    /** Lente rossa della telecamera puntata in avanti e in basso, più la luce rossa lampeggiante in cima (si disegna luminosa). */
    static Mesh droneEye() {
        MeshBuilder b = new MeshBuilder();
        b.colorf(1f, 0.16f, 0.1f);
        b.ellipsoid(0, -0.06f, 0.24f, 0.085f, 0.075f, 0.06f, 14, 8);
        b.colorf(1f, 0.85f, 0.75f);
        b.ellipsoid(0.02f, -0.045f, 0.29f, 0.022f, 0.02f, 0.012f, 8, 5);         // riflesso
        b.colorf(1f, 0.1f, 0.06f);
        b.ellipsoid(0, 0.16f, 0.02f, 0.035f, 0.035f, 0.035f, 8, 5);              // faro in cima
        b.ellipsoid(-0.215f, 0, 0.2f, 0.025f, 0.025f, 0.025f, 6, 4);
        b.colorf(0.2f, 1f, 0.3f);
        b.ellipsoid(0.215f, 0, 0.2f, 0.025f, 0.025f, 0.025f, 6, 4);
        return done(b, 0.9f, 60f);
    }

    private static Mesh done(MeshBuilder b, float spec, float shine) {
        Mesh m = b.build(-1, false, true);
        m.spec = spec; m.shine = shine;
        return m;
    }
}
