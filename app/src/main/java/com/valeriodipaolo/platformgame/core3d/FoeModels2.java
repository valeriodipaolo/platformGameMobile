package com.valeriodipaolo.platformgame.core3d;

import com.valeriodipaolo.platformgame.core.Img;

/**
 * Dodici nemici della seconda serie di livelli, tutti di forme morbide (ellissoidi, nessuno spigolo): talpa e lumaca (estate), castagna e gufo
 * (autunno), pinguino e pupazzo di neve (inverno), lapillo e salamandra (fuoco), rana e pianta carnivora (primavera), stella e solleone (cielo).
 * Orientati verso +z, con l'origine ai piedi.
 */
final class FoeModels2 {
    private static float[] rgb(int r, int g, int b) { return FoeModels.rgb(r, g, b); }

    private Mesh snailBody, snailShell, snailEyes, chestBody, chestEyes, owlBody, owlWing, owlEyes,
            pengBody, pengBelly, pengBits, pengWing, snowBody, snowBits, snowball, lapBody, lapEyes, salaBody, salaEyes, flame,
            frogBody, frogEyes, frogLeg, plantStem, plantHead, plantJaw, plantEyes, starBody, sunBody, sunRay, sunEyes;
    private final float[] a = new float[16], b2 = new float[16];

    private static Mesh fin(MeshBuilder b, float spec, float shine) {
        Mesh m = b.build(-1, false, true);
        m.spec = spec; m.shine = shine;
        return m;
    }



    /** Lumaca di giardino: piede lungo, umido e rugoso con coda a punta, testa che si alza e quattro tentacoli, guscio a spirale rigato. */
    private void buildSnail() {
        final float PI = (float) Math.PI;
        final float[] skinD = rgb(176, 160, 136), skinL = rgb(230, 220, 202);
        MeshBuilder b = new MeshBuilder();
        // corpo chiaro con collo sollevato e testa piccola (lumaca di vigna)
        float[][] c = {{0, .02f, -.60f}, {0, .07f, -.48f}, {0, .12f, -.30f}, {0, .16f, -.08f}, {0, .17f, .12f}, {0, .20f, .30f}, {0, .27f, .46f}, {0, .36f, .58f}, {0, .42f, .66f}};
        float[] rx = {0, .05f, .13f, .19f, .20f, .17f, .13f, .11f, 0}, ry = {0, .06f, .12f, .17f, .17f, .15f, .12f, .11f, 0};
        // pelle a rete di piccole creste, senza peluria
        FoeModels.loft(b, c, rx, ry, 0, 1, 0, 22, 0.8f, new FoeModels.Paint() {
            @Override void col(float t, float an, float x, float y, float z, float[] o) {
                float u = (float) Math.sin(t * 74f + an * 6f), v = (float) Math.sin(t * 74f - an * 6f);
                FoeModels.mix(skinD, skinL, FoeModels.sm(Math.abs(u * v) * 5f), o);
                float sh = 0.86f + 0.14f * (float) Math.sin(an + 1.57f);
                for (int k = 0; k < 3; k++) o[k] *= sh;
            }
        }, 2f, 4f);
        FoeModels.Paint tent = FoeModels.along(rgb(150, 136, 116), rgb(226, 216, 198));
        for (int sd = -1; sd <= 1; sd += 2) {
            // tentacoli superiori lunghi con l'occhio in punta, inferiori corti
            FoeModels.tube(b, new float[][] {{sd * .05f, .47f, .62f}, {sd * .08f, .62f, .70f}, {sd * .105f, .80f, .84f}}, new float[] {.016f, .011f, .008f}, 8, tent);
            FoeModels.tube(b, sd * .06f, .38f, .68f, sd * .10f, .43f, .84f, .012f, .006f, 6, tent);
        }
        snailBody = fin2(b, -1, 0.5f, 26f);

        b = new MeshBuilder();
        int n = 64;
        float[][] sc = new float[n][3];
        float[] sr = new float[n];
        float thMax = 2.2f * PI, g = 0.24f, rho0 = 0.095f;
        for (int i = 0; i < n; i++) {
            float th = thMax * i / (n - 1);
            float rho = rho0 * (float) Math.exp(g * th), R = rho * 0.6f;
            float phi = -0.55f * PI - (thMax - th);   // la bocca (giro più grande) sta davanti, sul collo
            sc[i] = new float[] {0, 0.62f + R * (float) Math.sin(phi) * 0.9f, -0.10f + R * (float) Math.cos(phi)};
            sr[i] = rho;
            int k = i - (n - 7);   // chiusura arrotondata della bocca: niente taglio netto
            if (k > 0) sr[i] = rho * (1f - 0.55f * (k / 6f) * (k / 6f));
        }
        final float[] car = rgb(158, 90, 42), cre = rgb(212, 166, 100), drk = rgb(74, 40, 20), lip = rgb(238, 228, 200);
        FoeModels.loft(b, sc, sr, sr, 1, 0, 0, 22, 1f, new FoeModels.Paint() {
            @Override void col(float t, float an, float x, float y, float z, float[] o) {
                float sn = (float) Math.sin(an * 1.0f + 0.7f);
                FoeModels.mix(car, cre, FoeModels.sm(sn * 1.4f + 0.3f), o);
                if (sn < -0.85f) FoeModels.mix(o, drk, 0.8f, o);   // fascia scura lungo la spirale
                float ridge = 0.93f + 0.07f * (float) Math.sin(t * 150f);   // linee di accrescimento
                for (int k = 0; k < 3; k++) o[k] *= ridge * (0.82f + 0.18f * t);
            }
        }, 3f, 2f);
        snailShell = fin2(b, Img.FOE_SHELL, 0.7f, 40f);

        b = new MeshBuilder();
        for (int sd = -1; sd <= 1; sd += 2) FoeModels.ball(b, sd * .105f, .81f, .845f, .019f, .019f, .019f, rgb(46, 38, 34));
        snailEyes = fin2(b, -1, 0.9f, 48f);
    }

    private static Mesh fin2(MeshBuilder b, int tex, float spec, float shine) {
        Mesh m = b.build(tex, false, true);
        m.spec = spec; m.shine = shine;
        return m;
    }

    /** Due occhi con sclera, pupilla e (se angry > 0) sopracciglia aggrottate. */
    private static void eyes(MeshBuilder b, float y, float z, float sp, float r, float angry) {
        for (int s = -1; s <= 1; s += 2) {
            b.colorf(1f, 1f, 1f);
            b.ellipsoid(s * sp, y, z, r, r * 1.1f, r * 0.55f, 10, 7);
            b.colorf(0.04f, 0.04f, 0.1f);
            b.ellipsoid(s * sp * 0.92f, y - r * 0.05f, z + r * 0.42f, r * 0.5f, r * 0.58f, r * 0.3f, 8, 6);
            if (angry > 0) {
                b.colorf(0.1f, 0.06f, 0.05f);
                b.ellipsoid(s * sp, y + r * 1.25f, z + r * 0.1f, r * 1.1f, r * 0.2f, r * 0.2f, 8, 5);
            }
        }
    }

    private Mesh chestStem, chestNuts, owlFace, owlTalon;

    /**
     * Riccio di castagno: guscio verde-bruno coperto di aculei sottili, spaccato in quattro in cima con le castagne lucide che
     * spuntano; appeso a un rametto con due foglie seghettate (il rametto sale verso la chioma e resta lì quando il riccio cade).
     */
    private void buildBurr() {
        MeshBuilder b = new MeshBuilder();
        final float R = 0.3f;
        FoeModels.Paint husk = new FoeModels.Paint() {
            @Override void col(float t, float an, float x, float y, float z, float[] o) {
                FoeModels.mix(rgb(78, 88, 34), rgb(110, 118, 44), 0.5f + 0.5f * (float) Math.sin(an * 5f + t * 9f), o);
            }
        };
        FoeModels.ballZ(b, 0, 0.35f, 0, R * 0.92f, R * 0.88f, R * 0.92f, 16, 12, husk, 0, 0);
        // aculei: punte sottili verso l'esterno, base verde e punta bruna; in cima (dove il guscio è aperto) niente aculei
        java.util.Random r = new java.util.Random(7);
        FoeModels.Paint spine = FoeModels.along(rgb(150, 158, 64), rgb(176, 150, 80));
        for (int i = 0; i < 950; i++) {
            float u = r.nextFloat() * 2 - 1, ph = r.nextFloat() * 6.283f, sq = (float) Math.sqrt(1 - u * u);
            float nx = sq * (float) Math.cos(ph), ny = u, nz = sq * (float) Math.sin(ph);
            if (ny > 0.66f) continue;
            float L = 0.07f + 0.05f * r.nextFloat();
            FoeModels.tube(b, nx * R * 0.92f, 0.35f + ny * R * 0.9f, nz * R * 0.92f, nx * (R + L), 0.35f + ny * (R * 0.95f + L), nz * (R + L), 0.01f, 0.0015f, 3, spine);
        }
        chestBody = fin(b, 0.15f, 12f);
        b = new MeshBuilder();
        b.colorf(0.40f, 0.20f, 0.09f);                                     // castagne lucide con la punta chiara
        for (int k = 0; k < 3; k++) {
            float an = k * 2.094f;
            float cx = (float) Math.cos(an) * 0.07f, cz = (float) Math.sin(an) * 0.07f;
            b.colorf(0.42f, 0.21f, 0.09f); b.ellipsoid(cx * 1.3f, 0.35f + R * 0.7f, cz * 1.3f, 0.1f, 0.11f, 0.1f, 10, 7);
            b.colorf(0.86f, 0.78f, 0.58f); b.ellipsoid(cx * 1.3f, 0.35f + R * 0.7f + 0.1f, cz * 1.3f, 0.03f, 0.022f, 0.03f, 6, 4);
        }
        chestNuts = fin(b, 0.9f, 70f);
        // rametto dal riccio fino alla chioma (3 m), con due foglie lunghe e seghettate
        b = new MeshBuilder();
        FoeModels.Paint wood = FoeModels.along(rgb(92, 70, 46), rgb(70, 54, 38));
        FoeModels.tube(b, new float[][] {{0, 0.66f, 0}, {0.02f, 1.0f, -0.01f}, {-0.01f, 1.33f, 0.01f}, {0, 1.66f, 0}}, new float[] {0.018f, 0.02f, 0.02f, 0.022f}, 6, wood);
        for (int sd = -1; sd <= 1; sd += 2) {
            float[][] c = {{0, 0.9f, 0}, {sd * 0.12f, 0.95f, 0.04f}, {sd * 0.26f, 0.93f, 0.07f}, {sd * 0.38f, 0.86f, 0.08f}, {sd * 0.46f, 0.78f, 0.08f}};
            float[] rx = {0.005f, 0.05f, 0.06f, 0.04f, 0.004f}, ry = {0.003f, 0.006f, 0.006f, 0.005f, 0.002f};
            FoeModels.loft(b, c, rx, ry, 0, 1, 0, 8, 1f, new FoeModels.Paint() {
                @Override void col(float t, float an, float x, float y, float z, float[] o) {
                    FoeModels.mix(rgb(170, 130, 40), rgb(130, 80, 30), t, o);   // foglia d'autunno
                    float saw = 0.9f + 0.1f * (float) Math.sin(t * 60f);
                    o[0] *= saw; o[1] *= saw; o[2] *= saw;
                }
            }, 0, 0);
        }
        chestStem = fin(b, 0.05f, 8f);
        b = new MeshBuilder(); chestEyes = fin(b, 0f, 8f);                 // un riccio non ha occhi
    }

    /** Altezza della liana sopra il centro del riccio appeso, alla coordinata x del sentiero (più bassa al centro). */
    static float lianaY(float x) { return LIANA_H + LIANA_SAG * (x / LIANA_HALF) * (x / LIANA_HALF); }
    static final float LIANA_H = 1.0f, LIANA_SAG = 0.9f, LIANA_HALF = 4.6f;
    private Mesh liana;

    /** Liana tesa da un lato all'altro del sentiero, un po' cadente, con qualche foglia: ci sono appesi i ricci. */
    void drawLiana(Gfx3D g, float[] m) {
        if (liana == null) {
            MeshBuilder b = new MeshBuilder();
            int n = 24;
            float[][] c = new float[n][];
            float[] r = new float[n];
            for (int i = 0; i < n; i++) {
                float x = -LIANA_HALF + 2 * LIANA_HALF * i / (n - 1f);
                c[i] = new float[] {x, lianaY(x) + 0.02f * (float) Math.sin(i * 1.7f), 0.03f * (float) Math.sin(i * 0.9f)};
                r[i] = 0.035f + 0.012f * (float) Math.sin(i * 2.3f);
            }
            FoeModels.Paint vine = new FoeModels.Paint() {
                @Override void col(float t, float an, float x, float y, float z, float[] o) {
                    FoeModels.mix(rgb(70, 60, 40), rgb(96, 84, 52), 0.5f + 0.5f * (float) Math.sin(t * 90f + an * 2f), o);   // fibre ritorte
                }
            };
            FoeModels.tube(b, c, r, 7, vine);
            java.util.Random rr = new java.util.Random(5);
            for (int k = 0; k < 16; k++) {                                     // foglie secche lungo la liana
                float x = -LIANA_HALF + 0.3f + rr.nextFloat() * (2 * LIANA_HALF - 0.6f), y = lianaY(x);
                float sd = rr.nextBoolean() ? 1 : -1, l = 0.12f + 0.06f * rr.nextFloat();
                FoeModels.loft(b, new float[][] {{x, y, 0}, {x + 0.03f, y - l * 0.5f, sd * 0.05f}, {x + 0.02f, y - l, sd * 0.07f}},
                        new float[] {0.004f, 0.035f, 0.004f}, new float[] {0.003f, 0.004f, 0.002f}, 0, 0, 1, 6, 1f,
                        FoeModels.along(rgb(150, 112, 40), rgb(120, 72, 30)), 0, 0);
            }
            liana = fin(b, 0.05f, 8f);
        }
        g.draw(liana, m, 1, 1, 1, 1, 0);
    }

    /**
     * Allocco: corpo a pera coperto di piume striate, disco facciale chiaro bordato di scuro, occhi neri profondi, becco
     * adunco giallastro, ciuffi alle orecchie; ali a remiganti separate e barrate; zampe piumate con artigli neri.
     */
    private void buildOwl() {
        MeshBuilder b = new MeshBuilder();
        final float[] dark = rgb(84, 60, 40), mid = rgb(140, 104, 68), light = rgb(196, 168, 128);
        FoeModels.Paint plumage = new FoeModels.Paint() {
            @Override void col(float t, float an, float x, float y, float z, float[] o) {
                boolean front = z > 0.04f;
                // gocce scure allungate sul petto, barre sul dorso: piume che si vedono anche da lontano
                float drop = (float) Math.sin(y * 70f) * (float) Math.sin(x * 55f + y * 9f);
                float barB = (float) Math.sin(y * 48f + (float) Math.sin(x * 30f) * 1.5f);
                if (front) FoeModels.mix(light, dark, FoeModels.sm((drop - 0.45f) * 4f) * 0.85f, o);
                else FoeModels.mix(mid, dark, FoeModels.sm(barB * 2f + 0.3f), o);
                float sc = 0.92f + 0.08f * (float) Math.sin(t * 90f + an * 9f);
                o[0] *= sc; o[1] *= sc; o[2] *= sc;
            }
        };
        float[][] c = {{0, -0.18f, -0.02f}, {0, -0.12f, 0}, {0, 0.0f, 0.01f}, {0, 0.14f, 0.02f}, {0, 0.28f, 0.02f}, {0, 0.38f, 0.01f}, {0, 0.48f, 0}, {0, 0.58f, -0.01f}, {0, 0.64f, -0.01f}};
        float[] rx = {0.02f, 0.14f, 0.22f, 0.25f, 0.24f, 0.21f, 0.22f, 0.18f, 0.02f}, ry = {0.02f, 0.13f, 0.2f, 0.22f, 0.21f, 0.19f, 0.2f, 0.16f, 0.02f};
        // tante sezioni: le piume sono dipinte sui vertici, servono fitti
        int nn = 40;
        float[][] c2 = new float[nn][3];
        float[] rx2 = new float[nn], ry2 = new float[nn];
        for (int i = 0; i < nn; i++) {
            float f = i / (nn - 1f) * (c.length - 1);
            int k = Math.min(c.length - 2, (int) f);
            float u = f - k, us = u * u * (3 - 2 * u);
            for (int q = 0; q < 3; q++) c2[i][q] = c[k][q] + (c[k + 1][q] - c[k][q]) * u;
            rx2[i] = rx[k] + (rx[k + 1] - rx[k]) * us;
            ry2[i] = ry[k] + (ry[k + 1] - ry[k]) * us;
        }
        FoeModels.loft(b, c2, rx2, ry2, 0, 0, 1, 40, 1f, plumage, 3f, 4f);
        // coda corta
        FoeModels.tube(b, 0, -0.06f, -0.12f, 0, -0.2f, -0.24f, 0.06f, 0.03f, 8, FoeModels.along(mid, dark));
        // ciuffi
        for (int sd = -1; sd <= 1; sd += 2) FoeModels.tube(b, sd * 0.12f, 0.58f, 0.02f, sd * 0.16f, 0.67f, -0.03f, 0.05f, 0.012f, 6, FoeModels.along(mid, dark));
        // mantello di piume vere: file sovrapposte di piumette a goccia, con la punta che si stacca dal corpo e il colore
        // del piumaggio (gocce sul petto, barre sul dorso); così la sagoma non è più liscia come plastica
        float[] fc = new float[3];
        for (int i = 4; i < nn - 3; i++) {
            float cy = c2[i][1], cz0 = c2[i][2], ra = rx2[i], rb = ry2[i];
            int around = 22;
            for (int k = 0; k < around; k++) {
                float h1 = (float) Math.abs(Math.sin(i * 91.7f + k * 47.3f)), h2 = (float) Math.abs(Math.sin(i * 13.1f + k * 7.9f + 2f));
                double a = (k + (i % 2) * 0.5 + (h1 - 0.5f) * 0.7f) * 2 * Math.PI / around;     // file sfalsate e irregolari
                float ca = (float) Math.cos(a), sa = (float) Math.sin(a);
                float px = ra * ca, pz = cz0 + rb * sa;
                if (sa > 0.55f && cy > 0.32f && cy < 0.56f) continue;          // lì c'è il disco facciale
                float nx = ca / ra, nz = sa / rb, nl = (float) Math.sqrt(nx * nx + nz * nz);
                nx /= nl; nz /= nl;
                float slope = (ry2[Math.min(nn - 1, i + 1)] - ry2[Math.max(0, i - 1)]) * 2f;   // il corpo si stringe o allarga: la piuma segue
                float ny = -slope * 0.5f;
                float sx = -nz, sz = nx;                                        // laterale (tangente orizzontale)
                plumage.col(i / (float) nn, (float) a, px, cy, pz, fc);
                float tone = 0.9f + 0.2f * h2;
                FoeModels.mix(fc, mid, 0.3f, fc);                                  // contrasto più morbido: niente scacchiera
                b.colorf(Math.min(1f, fc[0] * tone), Math.min(1f, fc[1] * tone), Math.min(1f, fc[2] * tone));
                float w = (0.018f + 0.01f * (ra / 0.25f)) * (0.75f + 0.5f * h2), len = 0.04f + 0.025f * h1;
                float cyk = cy + (h2 - 0.5f) * 0.012f;
                float tw = (h1 - 0.5f) * 0.02f;                                     // punta un po' storta
                float bx = px + nx * 0.002f, by = cyk + 0.02f, bz = pz + nz * 0.002f;          // radice (nascosta sotto la fila di sopra)
                float tx = px + nx * 0.016f + sx * tw, ty = cyk - len * 0.7f + ny * 0.01f, tz = pz + nz * 0.016f + sz * tw;   // punta staccata dal corpo
                float mx = px + nx * 0.011f, my = cyk - len * 0.2f, mz = pz + nz * 0.011f;
                b.vert(bx, by, bz, nx, 0.2f, nz, 0, 0); b.vert(mx + sx * w, my, mz + sz * w, nx, 0.2f, nz, 0, 0); b.vert(tx, ty, tz, nx, 0.2f, nz, 0, 0);
                b.vert(bx, by, bz, nx, 0.2f, nz, 0, 0); b.vert(tx, ty, tz, nx, 0.2f, nz, 0, 0); b.vert(mx - sx * w, my, mz - sz * w, nx, 0.2f, nz, 0, 0);
                b.colorf(fc[0] * 0.55f, fc[1] * 0.5f, fc[2] * 0.45f);                       // rachide scuro al centro della piuma
                b.vert(mx + sx * 0.003f, my, mz + sz * 0.003f, nx, 0.2f, nz, 0, 0); b.vert(tx + nx * 0.001f, ty, tz + nz * 0.001f, nx, 0.2f, nz, 0, 0);
                b.vert(bx + nx * 0.004f, by - 0.01f, bz + nz * 0.004f, nx, 0.2f, nz, 0, 0);
            }
        }
        owlBody = fin2(b, Img.FOE_OWL, 0.04f, 6f);
        // disco facciale: due conche chiare con il bordo scuro, becco in mezzo
        b = new MeshBuilder();
        for (int sd = -1; sd <= 1; sd += 2) {
            b.colorf(0.46f, 0.34f, 0.23f); b.ellipsoid(sd * 0.09f, 0.43f, 0.165f, 0.115f, 0.125f, 0.035f, 14, 8);
            b.colorf(0.72f, 0.60f, 0.44f); b.ellipsoid(sd * 0.088f, 0.43f, 0.172f, 0.1f, 0.11f, 0.03f, 14, 8);
        }
        for (int sd = -1; sd <= 1; sd += 2) {                               // piumette sottili che si irraggiano dagli occhi
            float ex = sd * 0.088f, ey = 0.43f;
            for (int k = 0; k < 26; k++) {
                double a = k * 2 * Math.PI / 26;
                float ca = (float) Math.cos(a), sa = (float) Math.sin(a), r0 = 0.035f, r1 = 0.098f + 0.01f * (k % 2);
                float z0 = 0.2f, z1 = 0.185f;
                if (k % 2 == 0) b.colorf(0.5f, 0.38f, 0.26f); else b.colorf(0.82f, 0.7f, 0.52f);
                float px = -sa * 0.006f, py = ca * 0.006f;
                b.vert(ex + ca * r0 - px, ey + sa * r0 - py, z0, 0, 0, 1, 0, 0); b.vert(ex + ca * r0 + px, ey + sa * r0 + py, z0, 0, 0, 1, 0, 0);
                b.vert(ex + ca * r1, ey + sa * r1 * 1.08f, z1, 0, 0, 1, 0, 0);
            }
            for (int k = 0; k < 34; k++) {                                   // orlo scuro del disco: corona di piumette appuntite
                double a = k * 2 * Math.PI / 34;
                float ca = (float) Math.cos(a), sa = (float) Math.sin(a), r0 = 0.1f, r1 = 0.128f + 0.008f * (k % 3);
                if (k % 2 == 0) b.colorf(0.3f, 0.2f, 0.13f); else b.colorf(0.42f, 0.3f, 0.2f);
                float px = -sa * 0.009f, py = ca * 0.009f;
                b.vert(ex + ca * r0 - px, ey + (sa * r0 - py) * 1.08f, 0.182f, 0, 0, 1, 0, 0); b.vert(ex + ca * r0 + px, ey + (sa * r0 + py) * 1.08f, 0.182f, 0, 0, 1, 0, 0);
                b.vert(ex + ca * r1, ey + sa * r1 * 1.08f, 0.165f, 0, 0, 1, 0, 0);
            }
        }
        b.colorf(0.62f, 0.50f, 0.36f); b.ellipsoid(0, 0.44f, 0.19f, 0.035f, 0.1f, 0.03f, 8, 6);       // costola di piume tra gli occhi
        FoeModels.tube(b, new float[][] {{0, 0.41f, 0.2f}, {0, 0.38f, 0.255f}, {0, 0.335f, 0.25f}}, new float[] {0.03f, 0.02f, 0.004f}, 8, FoeModels.along(rgb(200, 180, 110), rgb(70, 62, 50)));
        owlFace = fin2(b, Img.FOE_OWL, 0.03f, 6f);
        b = new MeshBuilder();
        for (int sd = -1; sd <= 1; sd += 2) {
            b.colorf(0.03f, 0.025f, 0.02f); b.ellipsoid(sd * 0.085f, 0.445f, 0.196f, 0.034f, 0.036f, 0.018f, 12, 8);
            b.colorf(0.95f, 0.95f, 0.9f); b.ellipsoid(sd * 0.085f + 0.01f, 0.457f, 0.217f, 0.007f, 0.007f, 0.003f, 6, 4);   // riflesso
        }
        owlEyes = fin(b, 0.9f, 80f);
        // ala: dalla spalla verso +x, copritrici in alto, remiganti separate e barrate sotto
        b = new MeshBuilder();
        FoeModels.Paint bar = new FoeModels.Paint() {
            @Override void col(float t, float an, float x, float y, float z, float[] o) {
                FoeModels.mix(mid, dark, FoeModels.sm(0.5f + 2f * (float) Math.sin(t * 22f)), o);
            }
        };
        float[][] cw = {{0.0f, 0, 0}, {0.14f, 0.01f, -0.02f}, {0.3f, 0.0f, -0.05f}, {0.42f, -0.02f, -0.08f}};
        FoeModels.loft(b, cw, new float[] {0.05f, 0.2f, 0.18f, 0.09f}, new float[] {0.02f, 0.03f, 0.022f, 0.012f}, 0, 1, 0, 12, 1f, FoeModels.along(mid, dark), 2f, 2f);
        for (int k = 0; k < 9; k++) {                                       // remiganti a ventaglio
            float an = -0.15f - k * 0.15f;
            float x0 = 0.1f + k * 0.04f, z0 = -0.05f - k * 0.012f, L = 0.34f + 0.035f * (4 - Math.abs(k - 4));
            float x1 = x0 + (float) Math.cos(an) * L, z1 = z0 + (float) Math.sin(an) * L;
            float[][] cf = {{x0, -0.005f * k, z0}, {(x0 + x1) / 2, -0.01f - 0.005f * k, (z0 + z1) / 2}, {x1, -0.015f - 0.005f * k, z1}};
            FoeModels.loft(b, cf, new float[] {0.07f, 0.075f, 0.03f}, new float[] {0.007f, 0.007f, 0.003f}, 0, 1, 0, 6, 1f, FoeModels.along(mid, mid), 1f, 1.5f);
        }
        // copritrici: tre file di piumette sovrapposte sul dorso dell'ala, barrate, che coprono la radice delle remiganti
        for (int row = 0; row < 3; row++) for (int k = 0; k < 9 - row; k++) {
            float x = 0.03f + k * 0.045f + row * 0.02f, z = -0.01f - row * 0.045f, y = 0.022f - row * 0.004f;
            float h = (float) Math.abs(Math.sin(k * 37.1f + row * 11.3f));
            float[] col = new float[3];
            FoeModels.mix(row == 0 ? light : mid, dark, ((k + row) % 3 == 0 ? 0.6f : 0.2f) + 0.2f * h, col);
            b.colorf(col[0], col[1], col[2]);
            float w = 0.026f, l = 0.06f + 0.015f * h;
            b.vert(x, y, z + 0.015f, 0, 1, 0, 0, 0); b.vert(x + w, y - 0.002f, z - l * 0.4f, 0, 1, 0, 0, 0); b.vert(x + 0.004f, y + 0.004f, z - l, 0, 1, 0, 0, 0);
            b.vert(x, y, z + 0.015f, 0, 1, 0, 0, 0); b.vert(x + 0.004f, y + 0.004f, z - l, 0, 1, 0, 0, 0); b.vert(x - w, y - 0.002f, z - l * 0.4f, 0, 1, 0, 0, 0);
        }
        owlWing = fin2(b, Img.FOE_OWLWING, 0.03f, 6f);
        // zampe piumate con quattro dita e artigli neri
        b = new MeshBuilder();
        for (int sd = -1; sd <= 1; sd += 2) {
            FoeModels.tube(b, sd * 0.08f, -0.08f, 0.04f, sd * 0.08f, -0.22f, 0.06f, 0.05f, 0.035f, 8, FoeModels.along(light, mid));
            for (int t = 0; t < 4; t++) {
                float an = (t == 3 ? 3.14f : -0.5f + t * 0.5f) + 1.5708f;
                float tx = sd * 0.08f + (float) Math.cos(an) * 0.07f, tz = 0.06f + (float) Math.sin(an) * 0.07f;
                FoeModels.tube(b, sd * 0.08f, -0.23f, 0.06f, tx, -0.25f, tz, 0.014f, 0.01f, 5, FoeModels.along(rgb(170, 150, 90), rgb(150, 130, 80)));
                FoeModels.tube(b, tx, -0.25f, tz, tx + (float) Math.cos(an) * 0.03f, -0.28f, tz + (float) Math.sin(an) * 0.03f, 0.009f, 0.002f, 4, FoeModels.along(rgb(30, 26, 22), rgb(10, 8, 8)));
            }
        }
        owlTalon = fin(b, 0.3f, 30f);
    }

    private Mesh hareBody, hareEars, hareEyes, hareLegs;
    private Mesh pengFeet, snowArmL, snowArmR, snowHeld;

    /** Pinguino di Adelia: dorso e testa nero-bluastri, petto bianco che sale fino al becco, anello bianco attorno all'occhio, piedi rosa. */
    private void buildPenguin() {
        MeshBuilder b = new MeshBuilder();
        final float[] back = rgb(22, 26, 36), white = rgb(236, 238, 242);
        FoeModels.Paint coat = new FoeModels.Paint() {
            @Override void col(float t, float an, float x, float y, float z, float[] o) {
                // bianco davanti (verso +z), con il confine che sale sul collo e lascia la testa nera
                float front = z / (0.05f + (float) Math.sqrt(x * x + z * z));
                float lim = y > 0.7f ? 2f : 0.45f - 0.15f * (float) Math.sin(t * 3.14f);
                FoeModels.mix(back, white, FoeModels.sm((front - lim) * 6f), o);
                float fe = 0.95f + 0.05f * (float) Math.sin(y * 120f + x * 40f);   // piumino fitto
                o[0] *= fe; o[1] *= fe; o[2] *= fe;
            }
        };
        float[][] c = {{0, 0.04f, -0.02f}, {0, 0.12f, 0}, {0, 0.25f, 0.01f}, {0, 0.4f, 0.01f}, {0, 0.55f, 0}, {0, 0.66f, 0}, {0, 0.74f, 0.01f}, {0, 0.84f, 0.02f}, {0, 0.92f, 0.03f}, {0, 0.96f, 0.03f}};
        float[] rx = {0.04f, 0.2f, 0.25f, 0.25f, 0.21f, 0.15f, 0.13f, 0.13f, 0.1f, 0.02f}, ry = {0.04f, 0.19f, 0.24f, 0.24f, 0.2f, 0.15f, 0.14f, 0.14f, 0.11f, 0.02f};
        int nn = 34;
        float[][] c2 = new float[nn][3];
        float[] rx2 = new float[nn], ry2 = new float[nn];
        for (int i = 0; i < nn; i++) {
            float f = i / (nn - 1f) * (c.length - 1);
            int k = Math.min(c.length - 2, (int) f);
            float u = f - k, us = u * u * (3 - 2 * u);
            for (int q = 0; q < 3; q++) c2[i][q] = c[k][q] + (c[k + 1][q] - c[k][q]) * u;
            rx2[i] = rx[k] + (rx[k + 1] - rx[k]) * us; ry2[i] = ry[k] + (ry[k + 1] - ry[k]) * us;
        }
        FoeModels.loft(b, c2, rx2, ry2, 0, 0, 1, 32, 1f, coat, 4f, 5f);
        FoeModels.tube(b, 0, 0.1f, -0.16f, 0, 0.03f, -0.3f, 0.07f, 0.02f, 6, FoeModels.along(back, back));   // coda rigida
        pengBody = fin2(b, Img.FOE_PENGFUR, 0.03f, 6f);
        b = new MeshBuilder();
        for (int sd = -1; sd <= 1; sd += 2) {                              // anello bianco, occhio scuro lucido
            b.colorf(0.95f, 0.95f, 0.96f); b.ellipsoid(sd * 0.085f, 0.84f, 0.085f, 0.035f, 0.035f, 0.02f, 10, 6);
            b.colorf(0.05f, 0.04f, 0.04f); b.ellipsoid(sd * 0.092f, 0.84f, 0.095f, 0.018f, 0.018f, 0.012f, 8, 5);
        }
        FoeModels.tube(b, new float[][] {{0, 0.82f, 0.12f}, {0, 0.81f, 0.18f}, {0, 0.795f, 0.22f}}, new float[] {0.03f, 0.018f, 0.004f}, 8, FoeModels.along(rgb(40, 34, 30), rgb(120, 60, 40)));
        pengBelly = fin(b, 0.6f, 50f);
        b = new MeshBuilder(); pengBits = fin(b, 0f, 8f);
        b = new MeshBuilder();                                             // pinna: lama piatta nera sopra, chiara sotto
        float[][] cw = {{0, 0, 0}, {0.02f, -0.12f, 0}, {0.03f, -0.26f, 0.01f}, {0.03f, -0.36f, 0.02f}};
        FoeModels.loft(b, cw, new float[] {0.012f, 0.016f, 0.014f, 0.004f}, new float[] {0.06f, 0.07f, 0.05f, 0.01f}, 0, 0, 1, 10, 1f, new FoeModels.Paint() {
            @Override void col(float t, float an, float x, float y, float z, float[] o) { FoeModels.mix(back, white, 0.08f + 0.5f * FoeModels.sm((t - 0.85f) * 8f), o); }
        }, 0, 0);
        pengWing = fin(b, 0.08f, 10f);
        b = new MeshBuilder();
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int t = -1; t <= 1; t++) FoeModels.tube(b, sd * 0.09f, 0.02f, 0.04f, sd * 0.09f + t * 0.045f, 0.01f, 0.15f, 0.025f, 0.012f, 6, FoeModels.along(rgb(220, 140, 140), rgb(190, 110, 110)));
        }
        pengFeet = fin(b, 0.3f, 20f);
    }

    /**
     * Pupazzo di neve fatto davvero: tre palle di neve irregolari e granulose, occhi e bottoni di carbone, naso di carota
     * rugosa, sciarpa rossa a maglia, secchio rovesciato in testa, braccia di rametti con le dita.
     */
    private void buildSnowman() {
        MeshBuilder b = new MeshBuilder();
        b.colorf(0.95f, 0.97f, 1f);
        int mk = b.mark();
        b.ellipsoid(0, 0.36f, 0, 0.4f, 0.36f, 0.4f, 26, 16);
        b.ellipsoid(0, 0.86f, 0, 0.29f, 0.27f, 0.29f, 22, 14);
        b.ellipsoid(0, 1.25f, 0, 0.21f, 0.2f, 0.21f, 20, 12);
        b.rough(mk, 0.025f, 6f, 41, false);                                        // neve pressata a mano: bozze e avvallamenti
        b.shadeByRelief(0.86f, 1.06f);
        snowBody = fin(b, 0.25f, 14f);
        b = new MeshBuilder();
        b.colorf(0.08f, 0.08f, 0.08f);
        for (int sd = -1; sd <= 1; sd += 2) {                             // occhi di carbone
            int m2 = b.mark(); b.ellipsoid(sd * 0.075f, 1.31f, 0.185f, 0.032f, 0.03f, 0.025f, 7, 5); b.rough(m2, 0.008f, 40f, 42);
        }
        for (int k = 0; k < 5; k++) {                                     // bocca a sorriso storto di sassolini
            float an = -0.6f + k * 0.3f;
            b.ellipsoid((float) Math.sin(an) * 0.09f, 1.19f - (float) Math.cos(an) * 0.03f, 0.195f - Math.abs(an) * 0.02f, 0.014f, 0.014f, 0.012f, 5, 4);
        }
        for (int k = 0; k < 3; k++) {                                     // bottoni
            int m2 = b.mark(); b.ellipsoid(0, 0.98f - k * 0.13f, 0.27f - k * 0.008f, 0.03f, 0.03f, 0.022f, 7, 5); b.rough(m2, 0.008f, 40f, 43 + k);
        }
        // carota rugosa
        FoeModels.Paint carrot = new FoeModels.Paint() {
            @Override void col(float t, float an, float x, float y, float z, float[] o) {
                FoeModels.mix(rgb(230, 110, 30), rgb(200, 90, 24), t, o);
                float ring = 0.85f + 0.15f * (float) Math.sin(t * 50f);
                o[0] *= ring; o[1] *= ring; o[2] *= ring;
            }
        };
        FoeModels.tube(b, new float[][] {{0, 1.26f, 0.18f}, {0.005f, 1.255f, 0.27f}, {0.01f, 1.245f, 0.36f}, {0.012f, 1.24f, 0.4f}}, new float[] {0.035f, 0.025f, 0.012f, 0.002f}, 8, carrot);
        // sciarpa: anello al collo e un capo che pende
        FoeModels.Paint knit = new FoeModels.Paint() {
            @Override void col(float t, float an, float x, float y, float z, float[] o) {
                FoeModels.mix(rgb(170, 30, 34), rgb(214, 60, 50), 0.5f + 0.5f * (float) Math.sin(an * 24f + t * 30f), o);
                if (((int) (t * 8 + 100)) % 3 == 0) FoeModels.mix(o, rgb(236, 226, 210), 0.7f, o);   // righe chiare
            }
        };
        float[][] ring = new float[17][];
        for (int k = 0; k <= 16; k++) { float an = k * 6.283f / 16; ring[k] = new float[] {(float) Math.cos(an) * 0.2f, 1.07f + 0.01f * (float) Math.sin(an * 3), (float) Math.sin(an) * 0.2f}; }
        float[] rr = new float[17]; java.util.Arrays.fill(rr, 0.04f);
        FoeModels.tube(b, ring, rr, 8, knit);
        FoeModels.loft(b, new float[][] {{0.12f, 1.05f, 0.16f}, {0.16f, 0.95f, 0.2f}, {0.17f, 0.82f, 0.22f}, {0.17f, 0.74f, 0.2f}},
                new float[] {0.06f, 0.065f, 0.065f, 0.06f}, new float[] {0.022f, 0.022f, 0.02f, 0.018f}, 1, 0, 0, 8, 1f, knit, 0, 0);
        // secchio di latta rovesciato, un po' storto
        FoeModels.Paint tin = FoeModels.along(rgb(120, 126, 132), rgb(80, 86, 92));
        FoeModels.tube(b, new float[][] {{0.02f, 1.39f, -0.01f}, {0.03f, 1.48f, -0.015f}, {0.04f, 1.56f, -0.02f}}, new float[] {0.17f, 0.15f, 0.13f}, 16, tin);
        b.colorf(0.32f, 0.34f, 0.36f); b.ellipsoid(0.04f, 1.565f, -0.02f, 0.13f, 0.012f, 0.13f, 14, 4);
        snowBits = fin(b, 0.15f, 14f);
        snowArmL = arm(-1);
        snowArmR = arm(1);
        b = new MeshBuilder(); b.colorf(1f, 1f, 1f); int m3 = b.mark(); b.ellipsoid(0, 0, 0, 0.11f, 0.11f, 0.11f, 10, 7); b.rough(m3, 0.012f, 18f, 44);
        snowHeld = fin(b, 0.2f, 12f);
    }

    /** Braccio di rametto dalla spalla verso il lato sd, con tre dita sottili. Origine alla spalla. */
    private static Mesh arm(int sd) {
        MeshBuilder b = new MeshBuilder();
        FoeModels.Paint wood = FoeModels.along(rgb(84, 62, 44), rgb(64, 48, 36));
        FoeModels.tube(b, new float[][] {{0, 0, 0}, {sd * 0.18f, 0.06f, 0.02f}, {sd * 0.36f, 0.14f, 0.0f}, {sd * 0.5f, 0.22f, 0.02f}}, new float[] {0.022f, 0.018f, 0.014f, 0.008f}, 6, wood);
        for (int k = -1; k <= 1; k++) FoeModels.tube(b, sd * 0.5f, 0.22f, 0.02f, sd * 0.6f, 0.26f + k * 0.07f, 0.02f + k * 0.03f, 0.008f, 0.003f, 4, wood);
        FoeModels.tube(b, sd * 0.3f, 0.12f, 0, sd * 0.36f, 0.24f, 0.02f, 0.008f, 0.003f, 4, wood);   // rametto laterale
        Mesh m = b.build(-1, false, true);
        m.spec = 0.05f; m.shine = 8f;
        return m;
    }

    /** Lepre bianca d'inverno: corpo raccolto, zampe posteriori lunghe, orecchie lunghe con la punta nera, occhi scuri, pelo fitto. */
    private void buildHare() {
        MeshBuilder b = new MeshBuilder();
        final float[] fur = rgb(238, 240, 244), shade = rgb(206, 210, 218);
        FoeModels.Paint coat = new FoeModels.Paint() {
            @Override void col(float t, float an, float x, float y, float z, float[] o) { FoeModels.mix(shade, fur, FoeModels.sm(y * 3f), o); }
        };
        float[][] c = {{0, 0.2f, -0.3f}, {0, 0.24f, -0.24f}, {0, 0.27f, -0.12f}, {0, 0.28f, 0f}, {0, 0.3f, 0.1f}, {0, 0.34f, 0.16f}, {0, 0.38f, 0.2f}};
        FoeModels.loft(b, c, new float[] {0.02f, 0.13f, 0.16f, 0.15f, 0.12f, 0.08f, 0.02f}, new float[] {0.02f, 0.12f, 0.15f, 0.14f, 0.11f, 0.08f, 0.02f}, 0, 1, 0, 20, 0.85f, coat, 3f, 3f);
        FoeModels.ballZ(b, 0, 0.42f, 0.27f, 0.09f, 0.085f, 0.11f, 16, 10, coat, 2f, 2f);          // testa
        b.colorf(0.97f, 0.97f, 1f); b.ellipsoid(0, 0.26f, -0.31f, 0.05f, 0.05f, 0.04f, 8, 6);       // coda a batuffolo
        hareBody = fin2(b, Img.FOE_PENGFUR, 0.02f, 6f);
        b = new MeshBuilder();
        for (int sd = -1; sd <= 1; sd += 2) {
            float[][] ce = {{sd * 0.04f, 0.48f, 0.24f}, {sd * 0.06f, 0.6f, 0.2f}, {sd * 0.075f, 0.72f, 0.15f}, {sd * 0.085f, 0.8f, 0.12f}};
            FoeModels.loft(b, ce, new float[] {0.025f, 0.032f, 0.028f, 0.004f}, new float[] {0.01f, 0.012f, 0.01f, 0.003f}, 0, 0, 1, 8, 1f, new FoeModels.Paint() {
                @Override void col(float t, float an, float x, float y, float z, float[] o) { FoeModels.mix(fur, rgb(30, 28, 30), FoeModels.sm((t - 0.78f) * 8f), o); }
            }, 1f, 2f);
        }
        hareEars = fin2(b, Img.FOE_PENGFUR, 0.02f, 6f);
        b = new MeshBuilder();
        for (int sd = -1; sd <= 1; sd += 2) { b.colorf(0.06f, 0.05f, 0.05f); b.ellipsoid(sd * 0.06f, 0.45f, 0.32f, 0.018f, 0.02f, 0.015f, 8, 5); }
        b.colorf(0.85f, 0.6f, 0.62f); b.ellipsoid(0, 0.42f, 0.375f, 0.014f, 0.01f, 0.01f, 6, 4);
        hareEyes = fin(b, 0.8f, 60f);
        b = new MeshBuilder();
        for (int sd = -1; sd <= 1; sd += 2) {
            FoeModels.tube(b, sd * 0.1f, 0.2f, -0.16f, sd * 0.11f, 0.03f, -0.06f, 0.06f, 0.03f, 8, FoeModels.along(fur, shade));   // coscia
            FoeModels.tube(b, sd * 0.11f, 0.03f, -0.06f, sd * 0.11f, 0.015f, 0.08f, 0.028f, 0.022f, 6, FoeModels.along(shade, fur)); // piede lungo
            FoeModels.tube(b, sd * 0.06f, 0.24f, 0.14f, sd * 0.06f, 0.02f, 0.17f, 0.025f, 0.018f, 6, FoeModels.along(fur, shade));  // zampa anteriore
        }
        hareLegs = fin2(b, Img.FOE_PENGFUR, 0.02f, 6f);
    }

    /** Lepre che salta: in volo si allunga con le zampe distese, a terra raccolta con le orecchie che oscillano. */
    void drawHare(Gfx3D g, float[] m, Enemy3 e) {
        if (hareBody == null) buildHare();
        boolean air = e.y > e.baseY + 0.05f;
        System.arraycopy(m, 0, a, 0, 16);
        if (air) { Mat4.scale(a, 0.95f, 0.95f, 1.15f); Mat4.rotateX(a, -0.25f); }
        g.draw(hareBody, a, 1, 1, 1, 1, 0);
        g.draw(hareEyes, a, 1, 1, 1, 1, 0.05f);
        System.arraycopy(a, 0, b2, 0, 16);
        Mat4.translate(b2, 0, 0.48f, 0.24f);
        Mat4.rotateX(b2, air ? -0.5f : -0.1f * (float) Math.sin(e.anim * 6f));
        Mat4.translate(b2, 0, -0.48f, -0.24f);
        g.draw(hareEars, b2, 1, 1, 1, 1, 0);
        System.arraycopy(a, 0, b2, 0, 16);
        if (air) { Mat4.translate(b2, 0, 0.2f, -0.1f); Mat4.rotateX(b2, 0.7f); Mat4.translate(b2, 0, -0.2f, 0.1f); }
        g.draw(hareLegs, b2, 1, 1, 1, 1, 0);
    }

    /**
     * Rana verde vera: corpo schiacciato a goccia con muso appuntito, dorso verde con macchie scure e due creste chiare,
     * ventre crema, occhi sporgenti dorati con pupilla orizzontale, timpano, zampe posteriori lunghe ripiegate e barrate,
     * piedi palmati, zampe anteriori corte.
     */
    private void buildFrog() {
        MeshBuilder b = new MeshBuilder();
        final float[] back = rgb(92, 140, 58), back2 = rgb(120, 150, 60), belly = rgb(226, 220, 176), ridge = rgb(170, 176, 96);
        FoeModels.Paint skin = new FoeModels.Paint() {
            @Override void col(float t, float an, float x, float y, float z, float[] o) {
                float s = (float) Math.sin(an), c = (float) Math.cos(an);
                FoeModels.mix(back, back2, 0.5f + 0.5f * (float) Math.sin(x * 30f + z * 21f), o);
                if (s > 0.2f && Math.abs(Math.abs(c) - 0.62f) < 0.1f && t > 0.2f && t < 0.8f) FoeModels.mix(o, ridge, 0.7f, o);   // creste dorso-laterali
                FoeModels.mix(o, belly, FoeModels.sm(-s * 3f - 0.1f), o);
            }
        };
        float[][] c = {{0, 0.17f, -0.3f}, {0, 0.19f, -0.24f}, {0, 0.21f, -0.14f}, {0, 0.22f, -0.03f}, {0, 0.22f, 0.07f}, {0, 0.215f, 0.15f}, {0, 0.205f, 0.22f}, {0, 0.19f, 0.28f}, {0, 0.18f, 0.32f}};
        float[] rx = {0.03f, 0.15f, 0.2f, 0.21f, 0.2f, 0.18f, 0.14f, 0.08f, 0.02f}, ry = {0.025f, 0.1f, 0.12f, 0.12f, 0.11f, 0.09f, 0.075f, 0.05f, 0.015f};
        int nn = 30;
        float[][] c2 = new float[nn][3];
        float[] rx2 = new float[nn], ry2 = new float[nn];
        for (int i = 0; i < nn; i++) {
            float f = i / (nn - 1f) * (c.length - 1);
            int k = Math.min(c.length - 2, (int) f);
            float u = f - k, us = u * u * (3 - 2 * u);
            for (int q = 0; q < 3; q++) c2[i][q] = c[k][q] + (c[k + 1][q] - c[k][q]) * u;
            rx2[i] = rx[k] + (rx[k + 1] - rx[k]) * us; ry2[i] = ry[k] + (ry[k + 1] - ry[k]) * us;
        }
        FoeModels.loft(b, c2, rx2, ry2, 0, 1, 0, 28, 0.8f, skin, 3f, 3f);
        // palpebre (cupole verdi sopra gli occhi) e timpani
        for (int sd = -1; sd <= 1; sd += 2) {
            FoeModels.ballZ(b, sd * 0.1f, 0.28f, 0.17f, 0.065f, 0.05f, 0.07f, 12, 8, skin, 1f, 1f);
            b.colorf(0.5f, 0.42f, 0.26f); b.ellipsoid(sd * 0.165f, 0.245f, 0.09f, 0.012f, 0.035f, 0.035f, 10, 6);
        }
        b.colorf(0.25f, 0.3f, 0.16f);                                       // rima della bocca
        for (int sd = -1; sd <= 1; sd += 2) FoeModels.tube(b, new float[][] {{sd * 0.16f, 0.19f, 0.06f}, {sd * 0.12f, 0.185f, 0.2f}, {sd * 0.05f, 0.18f, 0.3f}, {0, 0.178f, 0.32f}}, new float[] {0.006f, 0.006f, 0.005f, 0.004f}, 5, FoeModels.along(rgb(60, 70, 40), rgb(60, 70, 40)));
        frogBody = fin2(b, Img.FOE_FROG, 0.35f, 40f);                         // pelle umida: un po' di riflesso
        b = new MeshBuilder();
        for (int sd = -1; sd <= 1; sd += 2) {
            b.colorf(0.86f, 0.66f, 0.22f); b.ellipsoid(sd * 0.115f, 0.29f, 0.18f, 0.05f, 0.048f, 0.05f, 12, 8);          // iride dorata
            b.colorf(0.03f, 0.03f, 0.03f); b.ellipsoid(sd * 0.135f, 0.292f, 0.2f, 0.03f, 0.014f, 0.025f, 10, 6);          // pupilla orizzontale
        }
        frogEyes = fin(b, 0.9f, 90f);
        // zampa posteriore destra (sx si specchia): coscia, tibia, piede, cinque dita palmate
        b = new MeshBuilder();
        FoeModels.Paint barred = new FoeModels.Paint() {
            @Override void col(float t, float an, float x, float y, float z, float[] o) {
                FoeModels.mix(back, rgb(50, 70, 34), FoeModels.sm((float) Math.sin(t * 18f) * 2f), o);
                FoeModels.mix(o, belly, FoeModels.sm(-(float) Math.sin(an) * 2f - 0.5f) * 0.7f, o);
            }
        };
        FoeModels.tube(b, new float[][] {{0.15f, 0.17f, -0.17f}, {0.22f, 0.15f, -0.06f}, {0.27f, 0.1f, 0.06f}}, new float[] {0.075f, 0.06f, 0.045f}, 10, barred);
        FoeModels.tube(b, new float[][] {{0.27f, 0.1f, 0.06f}, {0.25f, 0.08f, -0.08f}, {0.21f, 0.06f, -0.2f}}, new float[] {0.045f, 0.035f, 0.025f}, 10, barred);
        FoeModels.tube(b, new float[][] {{0.21f, 0.06f, -0.2f}, {0.24f, 0.03f, -0.06f}, {0.27f, 0.02f, 0.04f}}, new float[] {0.025f, 0.02f, 0.016f}, 8, barred);
        for (int k = 0; k < 5; k++) {
            float an = -0.5f + k * 0.28f, L = 0.08f + 0.03f * (2 - Math.abs(k - 2.5f) / 1.5f);
            float tx = 0.27f + (float) Math.sin(an) * L, tz = 0.04f + (float) Math.cos(an) * L;
            FoeModels.tube(b, 0.27f, 0.02f, 0.04f, tx, 0.012f, tz, 0.01f, 0.006f, 5, FoeModels.along(back, back2));
            b.colorf(0.8f, 0.78f, 0.6f); b.ellipsoid(tx, 0.012f, tz, 0.011f, 0.007f, 0.011f, 6, 4);
        }
        b.colorf(0.55f, 0.66f, 0.4f);                                       // membrana tra le dita
        float[][] web = new float[6][];
        for (int k = 0; k < 5; k++) { float an = -0.5f + k * 0.28f; web[k] = new float[] {0.27f + (float) Math.sin(an) * 0.07f, 0.014f, 0.04f + (float) Math.cos(an) * 0.07f}; }
        for (int k = 0; k < 4; k++) {
            b.vert(0.27f, 0.016f, 0.04f, 0, 1, 0, 0, 0); b.vert(web[k][0], web[k][1], web[k][2], 0, 1, 0, 1, 0); b.vert(web[k + 1][0], web[k + 1][1], web[k + 1][2], 0, 1, 0, 1, 1);
            b.vert(0.27f, 0.016f, 0.04f, 0, -1, 0, 0, 0); b.vert(web[k + 1][0], web[k + 1][1], web[k + 1][2], 0, -1, 0, 1, 1); b.vert(web[k][0], web[k][1], web[k][2], 0, -1, 0, 1, 0);
        }
        FoeModels.ballZ(b, 0.27f, 0.1f, 0.06f, 0.046f, 0.046f, 0.046f, 10, 6, barred, 1f, 1f);      // ginocchio
        FoeModels.ballZ(b, 0.21f, 0.06f, -0.2f, 0.027f, 0.027f, 0.027f, 8, 5, barred, 1f, 1f);     // tallone
        FoeModels.ballZ(b, 0.15f, 0.17f, -0.17f, 0.08f, 0.07f, 0.09f, 10, 6, barred, 1f, 1f);      // anca
        // zampa anteriore
        FoeModels.tube(b, new float[][] {{0.12f, 0.17f, 0.15f}, {0.16f, 0.1f, 0.2f}, {0.15f, 0.03f, 0.24f}}, new float[] {0.035f, 0.026f, 0.02f}, 8, barred);
        for (int k = 0; k < 4; k++) {
            float an = -0.3f + k * 0.35f;
            FoeModels.tube(b, 0.15f, 0.03f, 0.24f, 0.15f + (float) Math.sin(an) * 0.05f, 0.01f, 0.24f + (float) Math.cos(an) * 0.05f, 0.008f, 0.005f, 4, FoeModels.along(back, back2));
        }
        frogLeg = fin2(b, Img.FOE_FROG, 0.3f, 36f);
    }

    /**
     * Dionea (pianta carnivora): rosetta di foglie a terra, picciolo, e la trappola: due lobi a conchiglia, verdi fuori e
     * rosso vivo dentro, con le ciglia lunghe sul margine. plantHead = lobo di sopra, plantJaw = lobo di sotto (cerniera
     * sull'asse x, aprono verso +z).
     */
    private void buildTrap() {
        MeshBuilder b = new MeshBuilder();
        FoeModels.Paint stem = FoeModels.along(rgb(96, 138, 56), rgb(120, 160, 64));
        FoeModels.tube(b, new float[][] {{0, 0, 0}, {0.02f, 0.25f, -0.03f}, {0, 0.5f, -0.04f}, {0, 0.68f, -0.02f}, {0, 0.75f, 0.0f}}, new float[] {0.05f, 0.04f, 0.035f, 0.035f, 0.03f}, 8, stem);
        for (int k = 0; k < 7; k++) {                                      // rosetta: foglie a spatola con una piccola trappola rossa in punta
            float an = k * 0.9f, ca = (float) Math.cos(an), sa = (float) Math.sin(an), L = 0.32f + 0.08f * (k % 3);
            FoeModels.loft(b, new float[][] {{0, 0.02f, 0}, {ca * L * 0.4f, 0.06f, sa * L * 0.4f}, {ca * L * 0.8f, 0.07f, sa * L * 0.8f}, {ca * L, 0.06f, sa * L}},
                    new float[] {0.01f, 0.025f, 0.05f, 0.01f}, new float[] {0.004f, 0.005f, 0.006f, 0.004f}, 0, 1, 0, 8, 1f, stem, 1f, 1f);
            b.colorf(0.62f, 0.2f, 0.16f); b.ellipsoid(ca * (L + 0.05f), 0.08f, sa * (L + 0.05f), 0.05f, 0.025f, 0.05f, 8, 5);
        }
        plantStem = fin2(b, Img.FOE_TRAP, 0.15f, 16f);
        plantHead = lobe(1);
        plantJaw = lobe(-1);
        b = new MeshBuilder(); plantEyes = fin(b, 0f, 8f);                   // una pianta non ha occhi
    }

    /** Un lobo della trappola: semicerchio di 0,64 m con la cerniera sull'asse x, incavato a conchiglia, con le ciglia. */
    private static Mesh lobe(int up) {
        MeshBuilder b = new MeshBuilder();
        final float W = 0.32f, D = 0.44f, C = 0.1f;
        int nu = 10, ns = 16;
        float[][][] P = new float[nu + 1][ns + 1][];
        for (int i = 0; i <= nu; i++) {
            float u = i / (float) nu;
            for (int j = 0; j <= ns; j++) {
                float ph = (float) Math.PI * j / ns;                       // da +x a -x passando dalla punta
                float rr = u;                                              // 0 cerniera, 1 margine
                float x = (float) Math.cos(ph) * W * rr, z = (float) Math.sin(ph) * D * rr;
                float y = up * C * (float) Math.sin(rr * Math.PI * 0.85f) * (0.4f + 0.6f * (float) Math.sin(ph));
                P[i][j] = new float[] {x, y, z};
            }
        }
        for (int side = 0; side < 2; side++) {                             // fuori verde, dentro rosso
            boolean outer = side == 0;
            for (int i = 0; i < nu; i++) for (int j = 0; j < ns; j++) {
                float[][] q = {P[i][j], P[i + 1][j], P[i + 1][j + 1], P[i][j + 1]};
                float ux = q[1][0] - q[0][0], uy = q[1][1] - q[0][1], uz = q[1][2] - q[0][2];
                float vx = q[3][0] - q[0][0], vy = q[3][1] - q[0][1], vz = q[3][2] - q[0][2];
                if (i == 0) { vx = q[2][0] - q[1][0]; vy = q[2][1] - q[1][1]; vz = q[2][2] - q[1][2]; }
                float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
                float nl = (float) Math.sqrt(nx * nx + ny * ny + nz * nz) + 1e-6f;
                nx /= nl; ny /= nl; nz /= nl;
                if ((ny * up > 0) != outer) { nx = -nx; ny = -ny; nz = -nz; }
                int[] ord = {0, 1, 2, 0, 2, 3};
                for (int k : ord) {
                    int ii = i + (k == 1 || k == 2 ? 1 : 0), jj = j + (k == 2 || k == 3 ? 1 : 0);
                    float u = ii / (float) nu;
                    if (outer) b.colorf(0.42f + 0.1f * u, 0.62f, 0.26f);
                    else { float rim = Math.max(0f, (u - 0.85f) / 0.15f); b.colorf(0.82f - 0.3f * rim * 0, 0.16f + 0.4f * rim, 0.14f + 0.05f * rim); }
                    float off = 0.008f;                                        // le due facce staccate di un soffio: niente sfarfallio
                    b.vert(q[k][0] + nx * off, q[k][1] + ny * off, q[k][2] + nz * off, nx, ny, nz, jj / (float) ns, u);
                }
            }
        }
        // ciglia: lunghe punte sul margine, che si incrociano quando la trappola si chiude
        FoeModels.Paint cil = FoeModels.along(rgb(150, 170, 70), rgb(190, 110, 70));
        for (int j = 1; j < ns * 2; j++) {
            float ph = (float) Math.PI * j / (ns * 2);
            float x = (float) Math.cos(ph) * W, z = (float) Math.sin(ph) * D;
            float y = up * C * (float) Math.sin(Math.PI * 0.85f) * (0.4f + 0.6f * (float) Math.sin(ph));
            float ox = (float) Math.cos(ph) * 0.13f, oz = (float) Math.sin(ph) * 0.13f;
            FoeModels.tube(b, x, y, z, x + ox, y + up * 0.06f, z + oz, 0.007f, 0.002f, 4, cil);
        }
        return fin2(b, Img.FOE_TRAP, 0.12f, 24f);
    }

    FoeModels2() {
        MeshBuilder b = new MeshBuilder();

        buildSnail();

        buildBurr();
        buildOwl();

        buildPenguin();
        buildSnowman();
        b = new MeshBuilder(); b.colorf(1f, 1f, 1f); b.ellipsoid(0, 0, 0, 0.17f, 0.17f, 0.17f, 10, 7);
        snowball = fin(b, 0.3f, 20f);

        // lapillo: sfera incandescente
        b = new MeshBuilder();
        b.colorf(1f, 0.38f, 0.05f); b.ellipsoid(0, 0, 0, 0.36f, 0.36f, 0.36f, 14, 10);
        b.colorf(1f, 0.78f, 0.2f); b.ellipsoid(0, 0.02f, 0.05f, 0.24f, 0.24f, 0.24f, 12, 8);
        b.colorf(0.95f, 0.25f, 0.05f); b.ellipsoid(0, 0.4f, -0.1f, 0.1f, 0.12f, 0.1f, 8, 5);
        lapBody = fin(b, 0.2f, 10f);
        b = new MeshBuilder(); eyes(b, 0.06f, 0.3f, 0.12f, 0.06f, 1f); lapEyes = fin(b, 0.4f, 20f);

        // salamandra: corpo allungato rosso, macchie gialle, coda
        b = new MeshBuilder();
        b.colorf(0.88f, 0.28f, 0.14f); b.ellipsoid(0, 0.3f, 0, 0.26f, 0.24f, 0.55f, 14, 10); b.ellipsoid(0, 0.34f, 0.5f, 0.2f, 0.18f, 0.2f, 12, 8);
        b.ellipsoid(0, 0.22f, -0.62f, 0.14f, 0.12f, 0.28f, 10, 7); b.ellipsoid(0, 0.18f, -0.9f, 0.08f, 0.07f, 0.2f, 8, 5);
        b.colorf(0.98f, 0.82f, 0.22f);
        for (int i = 0; i < 4; i++) { b.ellipsoid(0, 0.52f, -0.35f + i * 0.2f, 0.07f, 0.04f, 0.06f, 8, 4); }
        b.colorf(0.96f, 0.6f, 0.4f); b.ellipsoid(0, 0.12f, 0.12f, 0.2f, 0.1f, 0.4f, 10, 6);
        for (int s = -1; s <= 1; s += 2) { b.colorf(0.80f, 0.24f, 0.12f); b.ellipsoid(s * 0.27f, 0.1f, 0.28f, 0.07f, 0.07f, 0.13f, 8, 5); b.ellipsoid(s * 0.27f, 0.1f, -0.25f, 0.07f, 0.07f, 0.13f, 8, 5); }
        salaBody = fin(b, 0.4f, 28f);
        b = new MeshBuilder(); eyes(b, 0.44f, 0.6f, 0.1f, 0.05f, 1f); salaEyes = fin(b, 0.6f, 30f);
        b = new MeshBuilder();
        b.colorf(1f, 0.35f, 0.05f); b.ellipsoid(0, 0, 0.55f, 0.2f, 0.2f, 0.65f, 10, 7);
        b.colorf(1f, 0.62f, 0.12f); b.ellipsoid(0, 0, 0.95f, 0.26f, 0.26f, 0.7f, 10, 7);
        b.colorf(1f, 0.9f, 0.4f); b.ellipsoid(0, 0, 0.45f, 0.1f, 0.1f, 0.4f, 8, 5);
        flame = fin(b, 0f, 1f);

        buildFrog();
        buildTrap();

        // stella: centro e cinque punte arrotondate
        b = new MeshBuilder();
        b.colorf(1f, 0.88f, 0.28f); b.ellipsoid(0, 0, 0, 0.26f, 0.26f, 0.16f, 12, 8);
        for (int i = 0; i < 5; i++) {
            double an = Math.PI / 2 + i * 2 * Math.PI / 5;
            b.colorf(1f, 0.8f, 0.2f);
            b.ellipsoid((float) Math.cos(an) * 0.27f, (float) Math.sin(an) * 0.27f, 0, 0.1f, 0.1f, 0.12f, 8, 6);
            b.ellipsoid((float) Math.cos(an) * 0.16f, (float) Math.sin(an) * 0.16f, 0, 0.12f, 0.12f, 0.14f, 8, 6);
        }
        b.colorf(1f, 0.98f, 0.8f); b.ellipsoid(0, 0.02f, 0.1f, 0.14f, 0.14f, 0.08f, 8, 6);
        b.colorf(0.9f, 0.5f, 0.1f); b.ellipsoid(-0.07f, 0.04f, 0.17f, 0.03f, 0.04f, 0.02f, 6, 4); b.ellipsoid(0.07f, 0.04f, 0.17f, 0.03f, 0.04f, 0.02f, 6, 4);
        starBody = fin(b, 0.2f, 10f);

        // solleone: faccia tonda con raggi che girano
        b = new MeshBuilder();
        b.colorf(1f, 0.62f, 0.12f); b.ellipsoid(0, 0, 0, 0.34f, 0.34f, 0.28f, 14, 9);
        b.colorf(1f, 0.82f, 0.3f); b.ellipsoid(0, 0.02f, 0.06f, 0.26f, 0.26f, 0.24f, 12, 8);
        sunBody = fin(b, 0.2f, 10f);
        b = new MeshBuilder(); b.colorf(1f, 0.5f, 0.08f); b.ellipsoid(0, 0.5f, 0, 0.09f, 0.17f, 0.09f, 8, 6);
        sunRay = fin(b, 0.2f, 10f);
        b = new MeshBuilder(); eyes(b, 0.06f, 0.2f, 0.11f, 0.05f, 1f); sunEyes = fin(b, 0.6f, 30f);
    }

    void drawShot(Gfx3D g, float[] m, float[] s) {
        System.arraycopy(m, 0, a, 0, 16);
        Mat4.translate(a, s[0], s[1], s[2]);
        g.draw(snowball, a, 1, 1, 1, 1, 0.1f);
    }

    /** Modelli realistici che sostituiscono quelli vecchi (li passa FoeModels). */
    FoeModels4 s4;
    private Mesh plantNeck;

    void draw(Gfx3D g, float[] m, Enemy3 e) {
        float an = e.anim;
        switch (e.type) {
            case Enemy3.SNAIL: {
                boolean shell = e.mode == 1;
                System.arraycopy(m, 0, a, 0, 16);
                if (shell) { Mat4.translate(a, 0, -0.1f, 0); Mat4.scale(a, 0.85f, 0.85f, 0.85f); } else Mat4.scale(a, 1f + 0.04f * (float) Math.sin(an * 3f), 1f, 1f + 0.03f * (float) Math.sin(an * 3f + 1f));
                if (!shell) { g.draw(snailBody, a, 1, 1, 1, 1, 0); g.draw(snailEyes, a, 1, 1, 1, 1, 0); }
                if (e.mode != 2) g.draw(snailShell, a, 1, 1, 1, 1, 0);      // senza guscio resta la lumaca nuda
                return;
            }
            case Enemy3.CHESTNUT: {
                System.arraycopy(m, 0, a, 0, 16);
                Mat4.translate(a, 0, -0.35f, 0);
                if (e.mode < 2) {                                      // picciolo dal riccio fino alla liana che attraversa il sentiero
                    System.arraycopy(a, 0, b2, 0, 16);
                    if (e.mode == 1) Mat4.rotateZ(b2, 0.05f * (float) Math.sin(an * 40f));
                    float len = lianaY(e.x) - 0.31f;
                    Mat4.translate(b2, 0, 0.66f, 0);
                    Mat4.scale(b2, 1, len, 1);
                    Mat4.translate(b2, 0, -0.66f, 0);
                    g.draw(chestStem, b2, 1, 1, 1, 1, 0);
                }
                if (e.mode == 1) { Mat4.translate(a, 0, 0.66f, 0); Mat4.rotateZ(a, 0.09f * (float) Math.sin(an * 45f)); Mat4.translate(a, 0, -0.66f, 0); }
                if (e.mode == 2) Mat4.rotateX(a, an * 6f);
                if (e.mode == 3) Mat4.rotateX(a, an * 9f * e.dir);
                g.draw(chestBody, a, 1, 1, 1, 1, 0);
                g.draw(chestNuts, a, 1, 1, 1, 1, 0);
                return;
            }
            case Enemy3.OWL: {
                System.arraycopy(m, 0, a, 0, 16);
                Mat4.translate(a, 0, -0.1f, 0);
                g.draw(owlBody, a, 1, 1, 1, 1, 0);
                g.draw(owlFace, a, 1, 1, 1, 1, 0);
                g.draw(owlEyes, a, 1, 1, 1, 1, 0.05f);
                // ali: in attesa battito ampio e lento; in picchiata ali semichiuse a V, quasi ferme; in fondo alla picchiata
                // si aprono avanti a frenare; in risalita colpi ampi e rapidi; al ritorno battito regolare
                float t = e.t, flap, sweep, lift = 0f;
                if (e.mode == 1) {
                    float u = Math.min(1f, t / 0.85f);
                    flap = 0.55f + 0.05f * (float) Math.sin(an * 20f) - 0.9f * Math.max(0f, (u - 0.75f) / 0.25f);
                    sweep = 0.75f * (1f - Math.max(0f, (u - 0.75f) / 0.25f));
                } else if (e.mode == 2) {
                    flap = 0.9f * (float) Math.sin(t * 16f) + 0.1f; sweep = -0.15f; lift = 0.1f;
                } else if (e.mode == 3) {
                    flap = 0.7f * (float) Math.sin(an * 9f) + 0.1f; sweep = 0.1f;
                } else {
                    flap = 0.8f * (float) Math.sin(an * 5.5f) - 0.05f; sweep = 0f;
                }
                System.arraycopy(a, 0, b2, 0, 16);
                if (e.mode == 1 && t > 0.6f) { Mat4.translate(b2, 0, -0.04f, 0.1f); Mat4.rotateX(b2, -0.8f); }   // artigli avanti all'impatto
                g.draw(owlTalon, b2, 1, 1, 1, 1, 0);
                for (int s = -1; s <= 1; s += 2) {
                    System.arraycopy(a, 0, b2, 0, 16);
                    Mat4.translate(b2, s * 0.2f, 0.36f + lift, -0.02f);
                    if (s < 0) Mat4.scale(b2, -1, 1, 1);
                    Mat4.rotateY(b2, sweep);
                    Mat4.rotateZ(b2, flap);
                    g.draw(owlWing, b2, 1, 1, 1, 1, 0);
                }
                return;
            }
            case Enemy3.PENGUIN: {
                System.arraycopy(m, 0, a, 0, 16);
                boolean slide = e.mode == 1;
                float w = (float) Math.sin(an * (e.alongZ ? 9f : 16f));
                if (slide) {                                           // sdraiato sulla pancia, pinne aperte all'indietro
                    Mat4.translate(a, 0, 0.24f, -0.1f);
                    Mat4.rotateX(a, 1.45f);
                    Mat4.translate(a, 0, -0.24f, 0);
                } else {
                    Mat4.rotateZ(a, 0.12f * w);                       // dondola da un piede all'altro
                    if (e.mode == 2) Mat4.rotateX(a, 0.4f * (1f - Math.min(1f, e.t / 0.6f)));   // si rialza
                }
                g.draw(pengBody, a, 1, 1, 1, 1, 0);
                g.draw(pengBelly, a, 1, 1, 1, 1, 0.05f);
                g.draw(pengFeet, a, 1, 1, 1, 1, 0);
                for (int s = -1; s <= 1; s += 2) {
                    System.arraycopy(a, 0, b2, 0, 16);
                    Mat4.translate(b2, s * 0.22f, 0.62f, 0);
                    if (s < 0) Mat4.scale(b2, -1, 1, 1);
                    Mat4.rotateZ(b2, slide ? 0.9f : 0.25f + 0.25f * Math.abs(w));
                    if (slide) Mat4.rotateX(b2, -0.4f);
                    g.draw(pengWing, b2, 1, 1, 1, 1, 0);
                }
                return;
            }
            case Enemy3.SNOWMAN: {
                System.arraycopy(m, 0, a, 0, 16);
                boolean wind = e.mode == 1;
                if (wind) Mat4.rotateZ(a, 0.03f * (float) Math.sin(an * 30f));
                g.draw(snowBody, a, 1, 1, 1, 1, 0);
                g.draw(snowBits, a, 1, 1, 1, 1, 0);
                System.arraycopy(a, 0, b2, 0, 16);
                Mat4.translate(b2, -0.24f, 0.9f, 0);
                Mat4.rotateZ(b2, -0.15f);
                g.draw(snowArmL, b2, 1, 1, 1, 1, 0);
                // braccio destro: a riposo basso; in carica si alza dietro la testa con la palla in mano
                float k = wind ? Math.min(1f, e.t / 0.35f) : 0f;
                System.arraycopy(a, 0, b2, 0, 16);
                Mat4.translate(b2, 0.24f, 0.9f, 0);
                Mat4.rotateZ(b2, -0.35f + 1.9f * k);
                Mat4.rotateY(b2, 0.9f * k);
                g.draw(snowArmR, b2, 1, 1, 1, 1, 0);
                if (wind) { Mat4.translate(b2, 0.6f, 0.26f, 0.02f); g.draw(snowHeld, b2, 1, 1, 1, 1, 0.05f); }
                return;
            }
            case Enemy3.LAPILLO: {
                if (s4 != null) { s4.drawLavaBomb(g, m, e); return; }
                if (e.mode == 0) return;
                System.arraycopy(m, 0, a, 0, 16);
                Mat4.translate(a, 0, 0.36f, 0);
                float f = 1f + 0.08f * (float) Math.sin(an * 30f);
                Mat4.scale(a, f, f, f);
                g.draw(lapBody, a, 1.2f, 1f, 0.9f, 1, 0.85f);
                g.draw(lapEyes, a, 1, 1, 1, 1, 0.5f);
                return;
            }
            case Enemy3.SALAMANDER: {
                if (s4 != null) {
                    s4.drawSalamander(g, m, e);
                    if (e.mode == 2) {                                 // la fiammata dalla bocca
                        System.arraycopy(m, 0, b2, 0, 16);
                        Mat4.translate(b2, 0, 0.16f, 0.3f);
                        float k = 1f + 0.1f * (float) Math.sin(an * 40f);
                        Mat4.scale(b2, 0.8f * k, 0.8f * k, 1f);
                        g.draw(flame, b2, 1, 1, 1, 1, 1f);
                    }
                    return;
                }
                System.arraycopy(m, 0, a, 0, 16);
                float sw = e.mode == 1 ? 1f + 0.12f * (float) Math.sin(an * 30f) : 1f;
                Mat4.scale(a, sw, sw, 1f);
                if (e.mode == 0) Mat4.rotateY(a, 0.1f * (float) Math.sin(an * 8f));
                g.draw(salaBody, a, e.mode == 1 ? 1.3f : 1f, e.mode == 1 ? 1f : 1f, e.mode == 1 ? 0.8f : 1f, 1, e.mode == 1 ? 0.25f : 0f);
                g.draw(salaEyes, a, 1, 1, 1, 1, 0.3f);
                if (e.mode == 2) {
                    System.arraycopy(m, 0, b2, 0, 16);
                    Mat4.translate(b2, 0, 0.34f, 0.5f);
                    float k = 1f + 0.1f * (float) Math.sin(an * 40f);
                    Mat4.scale(b2, k, k, 1f);
                    g.draw(flame, b2, 1, 1, 1, 1, 1f);
                }
                return;
            }
            case Enemy3.FROG: {
                System.arraycopy(m, 0, a, 0, 16);
                boolean air = e.mode == 1;
                Mat4.scale(a, 1.25f, 1.25f, 1.25f);
                if (air) Mat4.rotateX(a, -0.3f);
                else Mat4.scale(a, 1f, 1f + 0.03f * (float) Math.sin(an * 5f), 1f);   // la gola che pulsa
                g.draw(frogBody, a, 1, 1, 1, 1, 0);
                g.draw(frogEyes, a, 1, 1, 1, 1, 0.3f);
                for (int s = -1; s <= 1; s += 2) {
                    System.arraycopy(a, 0, b2, 0, 16);
                    if (s < 0) Mat4.scale(b2, -1, 1, 1);
                    if (air) { Mat4.translate(b2, 0.13f, 0.17f, -0.18f); Mat4.rotateX(b2, 1.7f); Mat4.translate(b2, -0.13f, -0.17f, 0.18f); }   // in salto le zampe si distendono all'indietro
                    g.draw(frogLeg, b2, 1, 1, 1, 1, 0);
                }
                return;
            }
            case Enemy3.PLANT: {
                g.draw(plantStem, m, 1, 1, 1, 1, 0);
                // a riposo la trappola è aperta e respira; quando il robot si avvicina freme; poi la testa scatta in avanti
                // spalancata, si chiude di colpo alla massima distanza e torna indietro
                float open = e.mode == 2 ? (e.t < 0.2f ? 0.7f : 0.03f) : e.mode == 1 ? 0.6f + 0.06f * (float) Math.sin(an * 55f) : 0.45f + 0.05f * (float) Math.sin(an * 2f);
                float r = e.reach;
                if (r > 0.02f) {                                       // il collo che si allunga
                    if (plantNeck == null) {
                        MeshBuilder nb = new MeshBuilder();
                        nb.colorf(0.36f, 0.56f, 0.2f);
                        nb.cylinder(0, 0, 1, 0, 0.055f, 0.045f, 8, false, false);
                        plantNeck = fin(nb, 0.2f, 16f);
                    }
                    System.arraycopy(m, 0, b2, 0, 16);
                    Mat4.translate(b2, 0, 0.72f, 0);
                    Mat4.rotateX(b2, (float) Math.PI / 2 - 0.15f);
                    Mat4.scale(b2, 1, r, 1);
                    g.draw(plantNeck, b2, 1, 1, 1, 1, 0);
                }
                System.arraycopy(m, 0, a, 0, 16);
                Mat4.translate(a, 0, 0.75f + r * 0.15f, r);
                if (e.mode == 1) Mat4.translate(a, 0.03f * (float) Math.sin(an * 60f), 0, -0.08f);   // si ritrae prima dello scatto
                Mat4.rotateX(a, (e.mode == 2 ? -1.1f : -0.7f) + 0.06f * (float) Math.sin(an * 1.6f));   // in avanti, la bocca punta il robot
                System.arraycopy(a, 0, b2, 0, 16);
                Mat4.rotateX(b2, open);
                g.draw(plantJaw, b2, 1, 1, 1, 1, 0);
                System.arraycopy(a, 0, b2, 0, 16);
                Mat4.rotateX(b2, -open);
                g.draw(plantHead, b2, 1, 1, 1, 1, 0);
                return;
            }
            case Enemy3.STAR: {
                if (s4 != null) { s4.drawMeteor(g, m, e); return; }
                System.arraycopy(m, 0, a, 0, 16);
                Mat4.rotateZ(a, an * 4f);
                g.draw(starBody, a, 1f, 0.95f, 0.8f, 1, 0.9f);
                return;
            }
            default: {
                if (s4 != null) { s4.drawOrb(g, m, e); return; }
                System.arraycopy(m, 0, a, 0, 16);
                Mat4.translate(a, 0, 0.3f, 0);
                g.draw(sunBody, a, 1, 1, 1, 1, 0.8f);
                g.draw(sunEyes, a, 1, 1, 1, 1, 0.4f);
                for (int i = 0; i < 8; i++) {
                    System.arraycopy(a, 0, b2, 0, 16);
                    Mat4.rotateZ(b2, an * 2f + i * 0.7854f);
                    g.draw(sunRay, b2, 1, 1, 1, 1, 0.8f);
                }
            }
        }
    }
}
