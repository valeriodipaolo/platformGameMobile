package com.valeriodipaolo.platformgame.core3d;

import com.valeriodipaolo.platformgame.core.Img;

/**
 * Dodici nemici della seconda serie di livelli, tutti di forme morbide (ellissoidi, nessuno spigolo): talpa e lumaca (estate), castagna e gufo
 * (autunno), pinguino e pupazzo di neve (inverno), lapillo e salamandra (fuoco), rana e pianta carnivora (primavera), stella e solleone (cielo).
 * Orientati verso +z, con l'origine ai piedi.
 */
final class FoeModels2 {
    private static float[] rgb(int r, int g, int b) { return FoeModels.rgb(r, g, b); }

    private Mesh[] moleFur;
    private Mesh moleEyes, moleBits, mound, snailBody, snailShell, snailEyes, chestBody, chestEyes, owlBody, owlWing, owlEyes,
            pengBody, pengBelly, pengBits, pengWing, snowBody, snowBits, snowball, lapBody, lapEyes, salaBody, salaEyes, flame,
            frogBody, frogEyes, frogLeg, plantStem, plantHead, plantJaw, plantEyes, starBody, sunBody, sunRay, sunEyes;
    private final float[] a = new float[16], b2 = new float[16];

    private static Mesh fin(MeshBuilder b, float spec, float shine) {
        Mesh m = b.build(-1, false, true);
        m.spec = spec; m.shine = shine;
        return m;
    }


    // ---- talpa: forma tonda e piena come nella foto di riferimento; le texture sono ritagli della foto (tools/gen_mole.py)
    private static final float PC = (float) Math.cos(0.45f), PS = (float) Math.sin(0.45f);
    private static final float MCY = 0.42f, MRX = 0.46f, MRY = 0.37f, MRZ = 0.43f;
    /** Dove cade il naso sul davanti (in frazioni dei raggi): la foto è presa leggermente di lato. */
    private static final float NOSE_X = 0.36f, NOSE_Y = 0.07f;

    /** Punto della superficie della talpa: lon 0 = davanti (+z), lat = quota. Sul davanti il muso sporge, con il naso in punta. */
    private static void molePoint(float lon, float lat, float[] o) {
        float cl = (float) Math.cos(lat), x = cl * (float) Math.sin(lon), y = (float) Math.sin(lat), z = cl * (float) Math.cos(lon);
        float bump = 0f;
        if (z > 0) {
            float dx = x - NOSE_X, dy = y - NOSE_Y, d2 = dx * dx + dy * dy;
            bump = (0.13f * (float) Math.exp(-d2 / 0.09f) + 0.045f * (float) Math.exp(-d2 / 0.012f)) * FoeModels.sm(z * 2.5f);
        }
        float flat = y < -0.55f ? 1f - 0.5f * FoeModels.sm((-0.55f - y) / 0.45f) : 1f;          // pancia appoggiata
        o[0] = x * MRX; o[1] = MCY + y * MRY * (y < 0 ? flat : 1f); o[2] = z * MRZ + bump;
    }

    /** Metà della pelle della talpa (davanti o dietro), gonfiata di off lungo la normale. Davanti la foto è proiettata dritta, dietro il pelo si ripete. */
    private static Mesh moleSkin(boolean front, float off, int tex, float lum, boolean cut) {
        MeshBuilder b = new MeshBuilder();
        b.colorf(lum, lum, lum);
        int nu = 22, nv = 20;
        float[][][] P = new float[nu + 1][nv + 1][], Nn = new float[nu + 1][nv + 1][];
        float[] p0 = new float[3], p1 = new float[3], p2 = new float[3], p3 = new float[3];
        float e = 0.02f, lon0 = front ? -1.5708f : 1.5708f;
        for (int i = 0; i <= nu; i++) for (int j = 0; j <= nv; j++) {
            float lon = lon0 + 3.14159f * i / nu, lat = -1.5708f + 3.14159f * j / nv;
            float[] p = new float[5];
            molePoint(lon, lat, p);
            molePoint(lon - e, lat, p0); molePoint(lon + e, lat, p1);
            molePoint(lon, Math.max(lat - e, -1.57f), p2); molePoint(lon, Math.min(lat + e, 1.57f), p3);
            float ax = p1[0] - p0[0], ay = p1[1] - p0[1], az = p1[2] - p0[2], bx = p3[0] - p2[0], by = p3[1] - p2[1], bz = p3[2] - p2[2];
            float nx = bz * ay - by * az, ny = bx * az - bz * ax, nz = by * ax - bx * ay, nl = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (nl < 1e-6f || j == 0 || j == nv) { nx = 0; ny = j == 0 ? -1 : 1; nz = 0; nl = 1; }
            nx /= nl; ny /= nl; nz /= nl;
            // texture aperta sulla mezza sfera: centro = davanti (o dietro), raggio = angolo. Davanti è la foto, dietro il pelo ripetuto due volte
            float cl = (float) Math.cos(lat), ux = cl * (float) Math.sin(lon), uy = (float) Math.sin(lat), uz = cl * (float) Math.cos(lon);
            if (!front) { ux = -ux; uz = -uz; }
            float th = (float) Math.acos(Math.max(-1f, Math.min(1f, uz))), sn = (float) Math.sin(th), r = th / 1.5708f, rep = front ? 1f : 2f;
            p[3] = rep * (0.5f + (sn < 1e-4f ? 0f : 0.5f * r * ux / sn));
            p[4] = rep * (0.5f - (sn < 1e-4f ? 0f : 0.5f * r * uy / sn));
            // la talpa esce dal buco con il muso all'insù: tutto il corpo è inclinato all'indietro attorno al suo centro
            float py = p[1] - MCY, pz = p[2];
            p[1] = MCY + py * PC - pz * -PS; p[2] = pz * PC + py * -PS;
            float qy = ny, qz = nz;
            ny = qy * PC + qz * PS; nz = qz * PC - qy * PS;
            p[0] += nx * off; p[1] += ny * off; p[2] += nz * off;
            P[i][j] = p; Nn[i][j] = new float[] {nx, ny, nz};
        }
        for (int i = 0; i < nu; i++) for (int j = 0; j < nv; j++) {
            int[][] q = {{i, j}, {i + 1, j}, {i + 1, j + 1}, {i, j}, {i + 1, j + 1}, {i, j + 1}};
            for (int[] k : q) {
                float[] p = P[k[0]][k[1]], n = Nn[k[0]][k[1]];
                b.vert(p[0], p[1], p[2], n[0], n[1], n[2], p[3], p[4]);
            }
        }
        Mesh m = b.build(tex, false, true);
        m.spec = 0.04f; m.shine = 10f; m.cutout = cut;
        return m;
    }

    /** Mano a pala dalla foto: un cartoncino scontornato, in tre fogli ravvicinati per dargli spessore, appoggiato sul bordo del cumulo. */
    private static Mesh moleHand(int sd) {
        MeshBuilder b = new MeshBuilder();
        b.colorf(0.25f, 0.215f, 0.21f);
        float cx = sd * 0.41f, cy = 0.35f, cz = 0.31f, hw = 0.23f, hh = 0.23f;
        float yaw = sd * 0.32f, tilt = 0.45f;                                // aperta verso l'esterno, il polso affondato nel pelo del fianco
        float cyw = (float) Math.cos(yaw), syw = (float) Math.sin(yaw), ct = (float) Math.cos(tilt), st = (float) Math.sin(tilt);
        float rx = cyw, rz = -syw;                                           // destra del cartoncino
        float ux = syw * st, uy = ct, uz = cyw * st * -1f;                   // su del cartoncino (in basso sporge in avanti)
        float nx = 0f, ny = 0.6f, nz = 0.8f;                                 // stessa luce per le due mani
        for (int k = 0; k < 3; k++) {
            float o = (k - 1) * 0.012f, px = cx + nx * o, py = cy + ny * o, pz = cz + nz * o;
            float[][] q = {{-1, -1}, {1, -1}, {1, 1}, {-1, -1}, {1, 1}, {-1, 1}};
            for (float[] c : q)
                b.vert(px + rx * c[0] * hw + ux * c[1] * hh, py + uy * c[1] * hh, pz + rz * c[0] * hw + uz * c[1] * hh, nx, ny, nz, 0.5f + 0.5f * c[0], 0.5f - 0.5f * c[1]);
        }
        Mesh m = b.build(sd < 0 ? Img.MOLE_HAND_L : Img.MOLE_HAND_R, false, true);
        m.spec = 0.04f; m.shine = 16f; m.cutout = true;
        return m;
    }

    private void buildMole() {
        MeshBuilder b = new MeshBuilder();
        // cumulo di terra smossa attorno al buco
        FoeModels.Paint dirt = FoeModels.ud(rgb(64, 46, 32), rgb(118, 90, 64));
        FoeModels.ballY(b, 0, 0.02f, 0, 0.70f, 0.2f, 0.70f, 20, 8, dirt, 3f, 3f);
        for (int i = 0; i < 11; i++) {
            float an = i * 0.62f + 0.4f * (float) Math.sin(i * 2.3f), rr = 0.56f + 0.08f * (float) Math.sin(i * 1.7f);
            float lr = 0.09f + 0.05f * (0.5f + 0.5f * (float) Math.sin(i * 3.1f));
            FoeModels.ballY(b, (float) Math.cos(an) * rr, 0.09f, (float) Math.sin(an) * rr, lr, lr * 0.7f, lr, 8, 5, dirt, 1f, 1f);
        }
        FoeModels.ball(b, 0, 0.17f, 0, 0.40f, 0.03f, 0.40f, rgb(12, 9, 7));
        mound = fin2(b, Img.R3_DIRT, 0.05f, 6f);

        int L = Img.MOLE_LAYERS;
        moleFur = new Mesh[2 * (L + 1) + 2];
        int n = 0;
        float lum = 0.46f;                                  // le texture sono foto (già scure): si attenuano per non bruciarle sotto il sole del gioco
        moleFur[n++] = moleSkin(true, 0f, Img.MOLE_FRONT, lum, false);
        moleFur[n++] = moleSkin(false, 0f, Img.MOLE_FUR, lum, false);
        for (int k = 0; k < L; k++) {
            float off = (k + 1) * 0.0055f;
            moleFur[n++] = moleSkin(true, off, Img.MOLE_SHELL + k, lum, true);
            moleFur[n++] = moleSkin(false, off, Img.MOLE_BSHELL + k, lum, true);
        }
        moleFur[n++] = moleHand(-1);
        moleFur[n] = moleHand(1);
    }

    /** Raggi di una sezione gonfiati di off (non dove la forma si chiude a punta). */
    private static float[] swell(float[] r, float off) {
        float[] o = new float[r.length];
        for (int i = 0; i < r.length; i++) o[i] = r[i] + off * Math.min(1f, r[i] / 0.08f);
        return o;
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

    FoeModels2() {
        MeshBuilder b = new MeshBuilder();
        buildMole();

        // lumaca: corpo lungo e chiaro con le antenne, guscio a spirale arancio
        b = new MeshBuilder();
        b.colorf(0.82f, 0.80f, 0.55f); b.ellipsoid(0, 0.14f, -0.05f, 0.17f, 0.14f, 0.52f, 12, 8);
        b.ellipsoid(0, 0.26f, 0.45f, 0.14f, 0.15f, 0.16f, 10, 7);
        b.colorf(0.72f, 0.70f, 0.42f); b.ellipsoid(0, 0.35f, 0.5f, 0.02f, 0.14f, 0.02f, 6, 4);
        snailBody = fin(b, 0.4f, 24f);
        b = new MeshBuilder();
        b.colorf(0.86f, 0.44f, 0.22f); b.ellipsoid(0, 0.46f, -0.12f, 0.27f, 0.30f, 0.30f, 14, 9);
        b.colorf(0.96f, 0.72f, 0.42f); b.ellipsoid(0, 0.47f, -0.09f, 0.22f, 0.24f, 0.31f, 12, 8);
        b.colorf(0.82f, 0.38f, 0.18f); b.ellipsoid(0, 0.48f, -0.06f, 0.16f, 0.17f, 0.32f, 12, 8);
        b.colorf(0.96f, 0.72f, 0.42f); b.ellipsoid(0, 0.48f, -0.03f, 0.09f, 0.1f, 0.33f, 10, 6);
        snailShell = fin(b, 0.7f, 50f);
        b = new MeshBuilder(); eyes(b, 0.48f, 0.55f, 0.07f, 0.04f, 0f); snailEyes = fin(b, 0.6f, 30f);

        // castagna: sfera lucida con la macchia chiara e due occhi truci
        b = new MeshBuilder();
        b.colorf(0.46f, 0.22f, 0.10f); b.ellipsoid(0, 0.35f, 0, 0.34f, 0.36f, 0.34f, 14, 10);
        b.colorf(0.62f, 0.36f, 0.18f); b.ellipsoid(0, 0.62f, 0, 0.17f, 0.1f, 0.17f, 10, 6);
        b.colorf(0.9f, 0.82f, 0.62f); b.ellipsoid(0, 0.1f, 0, 0.17f, 0.07f, 0.17f, 10, 6);
        chestBody = fin(b, 0.8f, 60f);
        b = new MeshBuilder(); eyes(b, 0.4f, 0.27f, 0.12f, 0.06f, 1f); chestEyes = fin(b, 0.6f, 30f);

        // gufo: corpo a goccia, grandi occhi gialli, becco arancio; ala come ellissoide piatto
        b = new MeshBuilder();
        b.colorf(0.50f, 0.38f, 0.28f); b.ellipsoid(0, 0.1f, 0, 0.30f, 0.38f, 0.28f, 14, 9);
        b.colorf(0.86f, 0.78f, 0.62f); b.ellipsoid(0, 0.02f, 0.1f, 0.2f, 0.28f, 0.2f, 12, 8);
        b.colorf(0.42f, 0.30f, 0.22f); b.ellipsoid(-0.17f, 0.46f, 0, 0.07f, 0.12f, 0.07f, 8, 5); b.ellipsoid(0.17f, 0.46f, 0, 0.07f, 0.12f, 0.07f, 8, 5);
        b.colorf(0.98f, 0.62f, 0.12f); b.ellipsoid(0, 0.18f, 0.28f, 0.05f, 0.07f, 0.09f, 8, 5);
        owlBody = fin(b, 0.3f, 18f);
        b = new MeshBuilder();
        b.colorf(0.95f, 0.85f, 0.25f); b.ellipsoid(-0.13f, 0.28f, 0.22f, 0.11f, 0.11f, 0.05f, 10, 6); b.ellipsoid(0.13f, 0.28f, 0.22f, 0.11f, 0.11f, 0.05f, 10, 6);
        b.colorf(0.04f, 0.04f, 0.06f); b.ellipsoid(-0.13f, 0.28f, 0.265f, 0.05f, 0.06f, 0.02f, 8, 5); b.ellipsoid(0.13f, 0.28f, 0.265f, 0.05f, 0.06f, 0.02f, 8, 5);
        owlEyes = fin(b, 0.7f, 40f);
        b = new MeshBuilder(); b.colorf(0.40f, 0.30f, 0.22f); b.ellipsoid(0.26f, 0, 0, 0.30f, 0.05f, 0.2f, 10, 6);
        owlWing = fin(b, 0.2f, 12f);

        // pinguino
        b = new MeshBuilder();
        b.colorf(0.12f, 0.14f, 0.26f); b.ellipsoid(0, 0.42f, 0, 0.30f, 0.44f, 0.27f, 14, 10);
        b.colorf(0.12f, 0.14f, 0.26f); b.ellipsoid(0, 0.78f, 0.02f, 0.2f, 0.16f, 0.2f, 12, 8);
        pengBody = fin(b, 0.4f, 30f);
        b = new MeshBuilder(); b.colorf(0.96f, 0.96f, 0.98f); b.ellipsoid(0, 0.4f, 0.1f, 0.22f, 0.36f, 0.2f, 12, 8);
        b.ellipsoid(0, 0.77f, 0.1f, 0.12f, 0.1f, 0.1f, 10, 6);
        pengBelly = fin(b, 0.3f, 24f);
        b = new MeshBuilder(); eyes(b, 0.8f, 0.17f, 0.09f, 0.04f, 1f);
        b.colorf(0.98f, 0.62f, 0.12f); b.ellipsoid(0, 0.72f, 0.27f, 0.07f, 0.04f, 0.1f, 8, 5);
        b.ellipsoid(-0.12f, 0.03f, 0.1f, 0.1f, 0.03f, 0.15f, 8, 5); b.ellipsoid(0.12f, 0.03f, 0.1f, 0.1f, 0.03f, 0.15f, 8, 5);
        pengBits = fin(b, 0.5f, 30f);
        b = new MeshBuilder(); b.colorf(0.12f, 0.14f, 0.26f); b.ellipsoid(0.12f, -0.16f, 0, 0.05f, 0.22f, 0.12f, 8, 6);
        pengWing = fin(b, 0.3f, 20f);

        // pupazzo di neve: tre sfere, naso a carota, cappello
        b = new MeshBuilder();
        b.colorf(0.96f, 0.97f, 1f); b.ellipsoid(0, 0.32f, 0, 0.36f, 0.32f, 0.36f, 14, 9); b.ellipsoid(0, 0.78f, 0, 0.27f, 0.25f, 0.27f, 14, 9); b.ellipsoid(0, 1.15f, 0, 0.2f, 0.2f, 0.2f, 12, 8);
        snowBody = fin(b, 0.4f, 28f);
        b = new MeshBuilder(); eyes(b, 1.2f, 0.16f, 0.08f, 0.035f, 1f);
        b.colorf(0.98f, 0.5f, 0.12f); b.ellipsoid(0, 1.13f, 0.28f, 0.04f, 0.04f, 0.16f, 8, 5);
        b.colorf(0.14f, 0.14f, 0.18f); b.ellipsoid(0, 1.36f, 0, 0.17f, 0.12f, 0.17f, 10, 6); b.ellipsoid(0, 1.3f, 0, 0.26f, 0.04f, 0.26f, 12, 5);
        b.ellipsoid(0, 0.8f, 0.26f, 0.04f, 0.04f, 0.03f, 6, 4); b.ellipsoid(0, 0.68f, 0.27f, 0.04f, 0.04f, 0.03f, 6, 4);
        snowBits = fin(b, 0.5f, 30f);
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

        // rana
        b = new MeshBuilder();
        b.colorf(0.30f, 0.66f, 0.22f); b.ellipsoid(0, 0.26f, 0, 0.34f, 0.24f, 0.4f, 14, 9);
        b.colorf(0.88f, 0.9f, 0.62f); b.ellipsoid(0, 0.2f, 0.1f, 0.24f, 0.14f, 0.3f, 12, 7);
        b.colorf(0.34f, 0.72f, 0.26f); b.ellipsoid(-0.17f, 0.46f, 0.22f, 0.11f, 0.11f, 0.11f, 10, 7); b.ellipsoid(0.17f, 0.46f, 0.22f, 0.11f, 0.11f, 0.11f, 10, 7);
        b.colorf(0.9f, 0.4f, 0.4f); b.ellipsoid(0, 0.3f, 0.36f, 0.2f, 0.02f, 0.05f, 8, 4);
        frogBody = fin(b, 0.5f, 34f);
        b = new MeshBuilder(); eyes(b, 0.48f, 0.3f, 0.17f, 0.065f, 0f); frogEyes = fin(b, 0.7f, 40f);
        b = new MeshBuilder(); b.colorf(0.28f, 0.60f, 0.2f); b.ellipsoid(0.1f, 0.12f, -0.05f, 0.14f, 0.17f, 0.3f, 10, 6); b.ellipsoid(0.12f, 0.03f, 0.2f, 0.1f, 0.04f, 0.22f, 8, 5);
        frogLeg = fin(b, 0.4f, 26f);

        // pianta carnivora: gambo, testa rossa con macchie chiare, mascella inferiore
        b = new MeshBuilder();
        b.colorf(0.22f, 0.56f, 0.18f); b.ellipsoid(0, 0.3f, 0, 0.09f, 0.34f, 0.09f, 8, 7); b.ellipsoid(-0.18f, 0.18f, 0, 0.2f, 0.04f, 0.1f, 8, 5); b.ellipsoid(0.18f, 0.2f, 0, 0.2f, 0.04f, 0.1f, 8, 5);
        plantStem = fin(b, 0.2f, 12f);
        b = new MeshBuilder();
        b.colorf(0.84f, 0.12f, 0.20f); b.ellipsoid(0, 0.12f, 0.02f, 0.34f, 0.24f, 0.34f, 14, 9);
        b.colorf(0.98f, 0.9f, 0.82f); for (int i = 0; i < 4; i++) b.ellipsoid(-0.18f + i * 0.12f, 0.3f, 0.12f, 0.05f, 0.03f, 0.05f, 6, 4);
        plantHead = fin(b, 0.5f, 34f);
        b = new MeshBuilder();
        b.colorf(0.72f, 0.08f, 0.16f); b.ellipsoid(0, -0.08f, 0.02f, 0.32f, 0.16f, 0.32f, 14, 8);
        b.colorf(1f, 1f, 0.95f); for (int i = 0; i < 5; i++) b.ellipsoid(-0.24f + i * 0.12f, 0.06f, 0.28f, 0.035f, 0.07f, 0.035f, 6, 4);
        plantJaw = fin(b, 0.5f, 34f);
        b = new MeshBuilder(); eyes(b, 0.3f, 0.26f, 0.12f, 0.05f, 1f); plantEyes = fin(b, 0.6f, 30f);

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

    void draw(Gfx3D g, float[] m, Enemy3 e) {
        float an = e.anim;
        switch (e.type) {
            case Enemy3.MOLE: {
                g.draw(mound, m, 1, 1, 1, 1, 0);
                if (e.mode == 0) return;
                float up = e.mode == 1 ? 0.25f + 0.05f * (float) Math.sin(an * 50f) : 1f;
                System.arraycopy(m, 0, a, 0, 16);
                Mat4.translate(a, 0, -0.5f + 0.5f * up, 0);
                for (Mesh fm : moleFur) g.draw(fm, a, 1, 1, 1, 1, 0);
                return;
            }
            case Enemy3.SNAIL: {
                boolean shell = e.mode == 1;
                System.arraycopy(m, 0, a, 0, 16);
                if (shell) { Mat4.scale(a, 0.8f, 0.8f, 0.8f); } else Mat4.scale(a, 1f + 0.05f * (float) Math.sin(an * 3f), 1f, 1f);
                if (!shell) { g.draw(snailBody, a, 1, 1, 1, 1, 0); g.draw(snailEyes, a, 1, 1, 1, 1, 0.3f); }
                g.draw(snailShell, a, 1, 1, 1, 1, 0);
                return;
            }
            case Enemy3.CHESTNUT: {
                System.arraycopy(m, 0, a, 0, 16);
                Mat4.translate(a, 0, -0.35f, 0);
                if (e.mode == 1) Mat4.translate(a, 0.04f * (float) Math.sin(an * 60f), 0, 0);
                if (e.mode == 3) Mat4.rotateX(a, an * 9f * e.dir);
                g.draw(chestBody, a, 1, 1, 1, 1, 0);
                if (e.mode != 3) g.draw(chestEyes, a, 1, 1, 1, 1, 0.3f);
                return;
            }
            case Enemy3.OWL: {
                System.arraycopy(m, 0, a, 0, 16);
                Mat4.translate(a, 0, -0.1f, 0);
                if (e.mode == 1) Mat4.rotateX(a, 0.6f);
                g.draw(owlBody, a, 1, 1, 1, 1, 0);
                g.draw(owlEyes, a, 1, 1, 1, 1, 0.5f);
                float flap = 0.7f * (float) Math.sin(an * (e.mode == 0 ? 7f : 14f));
                for (int s = -1; s <= 1; s += 2) {
                    System.arraycopy(a, 0, b2, 0, 16);
                    Mat4.translate(b2, s * 0.22f, 0.15f, 0);
                    if (s < 0) Mat4.scale(b2, -1, 1, 1);
                    Mat4.rotateZ(b2, flap);
                    g.draw(owlWing, b2, 1, 1, 1, 1, 0);
                }
                return;
            }
            case Enemy3.PENGUIN: {
                System.arraycopy(m, 0, a, 0, 16);
                float w = (float) Math.sin(an * 18f);
                Mat4.rotateZ(a, 0.1f * w);
                g.draw(pengBody, a, 1, 1, 1, 1, 0);
                g.draw(pengBelly, a, 1, 1, 1, 1, 0);
                g.draw(pengBits, a, 1, 1, 1, 1, 0.2f);
                for (int s = -1; s <= 1; s += 2) {
                    System.arraycopy(a, 0, b2, 0, 16);
                    Mat4.translate(b2, s * 0.27f, 0.6f, 0);
                    if (s < 0) Mat4.scale(b2, -1, 1, 1);
                    Mat4.rotateZ(b2, -0.5f - 0.3f * w);
                    g.draw(pengWing, b2, 1, 1, 1, 1, 0);
                }
                return;
            }
            case Enemy3.SNOWMAN: {
                System.arraycopy(m, 0, a, 0, 16);
                float charge = e.t > 1.8f ? 0.06f * (float) Math.sin(an * 40f) : 0f;
                Mat4.scale(a, 1f + charge, 1f, 1f + charge);
                g.draw(snowBody, a, 1, 1, 1, 1, 0);
                g.draw(snowBits, a, 1, 1, 1, 1, 0.1f);
                return;
            }
            case Enemy3.LAPILLO: {
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
                float cr = air ? 1.12f : 0.92f + 0.04f * (float) Math.sin(an * 3f);
                Mat4.scale(a, air ? 0.92f : 1.04f, cr, air ? 1.1f : 1f);
                if (air) Mat4.rotateX(a, -0.35f);
                g.draw(frogBody, a, 1, 1, 1, 1, 0);
                g.draw(frogEyes, a, 1, 1, 1, 1, 0.3f);
                for (int s = -1; s <= 1; s += 2) {
                    System.arraycopy(a, 0, b2, 0, 16);
                    if (s < 0) Mat4.scale(b2, -1, 1, 1);
                    if (air) Mat4.rotateX(b2, 0.9f);
                    g.draw(frogLeg, b2, 1, 1, 1, 1, 0);
                }
                return;
            }
            case Enemy3.PLANT: {
                g.draw(plantStem, m, 1, 1, 1, 1, 0);
                System.arraycopy(m, 0, a, 0, 16);
                float open = e.mode == 2 ? 0.8f : e.mode == 1 ? 0.12f + 0.1f * (float) Math.sin(an * 45f) : 0f;
                float yaw = e.mode == 0 ? 0 : 0f;
                Mat4.translate(a, 0, 0.72f, 0);
                Mat4.rotateX(a, -0.25f + 0.1f * (float) Math.sin(an * 2f));
                g.draw(plantJaw, a, 1, 1, 1, 1, 0);
                System.arraycopy(a, 0, b2, 0, 16);
                Mat4.translate(b2, 0, 0.0f, -0.28f);
                Mat4.rotateX(b2, -open);
                Mat4.translate(b2, 0, 0.0f, 0.28f);
                g.draw(plantHead, b2, e.mode == 2 ? 1.2f : 1f, 1f, 1f, 1, 0);
                g.draw(plantEyes, b2, 1, 1, 1, 1, 0.3f);
                return;
            }
            case Enemy3.STAR: {
                System.arraycopy(m, 0, a, 0, 16);
                Mat4.rotateZ(a, an * 4f);
                g.draw(starBody, a, 1f, 0.95f, 0.8f, 1, 0.9f);
                return;
            }
            default: {
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
