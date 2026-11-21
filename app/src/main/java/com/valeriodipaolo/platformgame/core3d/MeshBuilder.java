package com.valeriodipaolo.platformgame.core3d;

/** Costruisce mesh da forme semplici: scatole, ellissoidi, cilindri, coni. */
public final class MeshBuilder {
    private float[] d = new float[4096];
    private int n;
    private float cr = 1, cg = 1, cb = 1, sw;

    public MeshBuilder color(int r, int g, int b) {
        cr = r / 255f;
        cg = g / 255f;
        cb = b / 255f;
        return this;
    }

    /** Colore con decimali (per le ombre e l'occlusione calcolate in anticipo sui vertici). */
    public MeshBuilder colorf(float r, float g, float b) {
        cr = r;
        cg = g;
        cb = b;
        return this;
    }

    /** Peso con cui il vento muove i vertici successivi (0 = fermi, 1 = oscillano al massimo). */
    public MeshBuilder sway(float s) {
        sw = s;
        return this;
    }

    /** Quanti numeri sono già stati scritti (per deformare poi solo l'ultimo pezzo aggiunto). */
    public int mark() { return n; }

    /**
     * Rende irregolare l'ultimo pezzo (da mark in poi): ogni vertice si sposta secondo un rumore che dipende dalla sua posizione
     * nel mondo (così due pezzi che si toccano restano attaccati), poi le normali si ricalcolano per faccetta, come pietra sbozzata.
     */
    public void rough(int mark, float amp, float freq, int seed) {
        for (int i = mark; i < n; i += Mesh.STRIDE) {
            float x = d[i], y = d[i + 1], z = d[i + 2];
            d[i] += amp * (noise3(x * freq, y * freq, z * freq, seed) - 0.5f) * 2f + amp * 0.4f * (noise3(x * freq * 2.7f, y * freq * 2.7f, z * freq * 2.7f, seed + 3) - 0.5f) * 2f;
            d[i + 1] += amp * 0.35f * (noise3(x * freq, y * freq, z * freq, seed + 1) - 0.5f) * 2f;
            d[i + 2] += amp * (noise3(x * freq, y * freq, z * freq, seed + 2) - 0.5f) * 2f + amp * 0.4f * (noise3(x * freq * 2.7f, y * freq * 2.7f, z * freq * 2.7f, seed + 4) - 0.5f) * 2f;
        }
        for (int i = mark; i + 3 * Mesh.STRIDE <= n; i += 3 * Mesh.STRIDE) {
            int a = i, b = i + Mesh.STRIDE, c = i + 2 * Mesh.STRIDE;
            float ux = d[b] - d[a], uy = d[b + 1] - d[a + 1], uz = d[b + 2] - d[a + 2];
            float vx = d[c] - d[a], vy = d[c + 1] - d[a + 1], vz = d[c + 2] - d[a + 2];
            float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
            float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (l < 1e-9f) continue;
            nx /= l; ny /= l; nz /= l;
            // stesso verso della normale originale (le facce non hanno un giro fisso dei vertici)
            if (nx * d[a + 3] + ny * d[a + 4] + nz * d[a + 5] < 0) { nx = -nx; ny = -ny; nz = -nz; }
            for (int k = a; k <= c; k += Mesh.STRIDE) { d[k + 3] = nx; d[k + 4] = ny; d[k + 5] = nz; }
        }
    }

    /**
     * Scurisce le conche e schiarisce le gobbe di un oggetto tondeggiante attorno all'origine (occlusione finta):
     * il colore di ogni vertice va da lo (più vicino al centro della media) a hi (più sporgente).
     */
    public void shadeByRelief(float lo, float hi) {
        float sum = 0; int cnt = 0;
        for (int i = 0; i < n; i += Mesh.STRIDE) { sum += (float) Math.sqrt(d[i] * d[i] + d[i + 1] * d[i + 1] + d[i + 2] * d[i + 2]); cnt++; }
        float avg = cnt > 0 ? sum / cnt : 1f;
        for (int i = 0; i < n; i += Mesh.STRIDE) {
            float r = (float) Math.sqrt(d[i] * d[i] + d[i + 1] * d[i + 1] + d[i + 2] * d[i + 2]);
            float t = Math.max(0f, Math.min(1f, (r - avg) / (avg * 0.12f) * 0.5f + 0.5f));
            float k = lo + (hi - lo) * t;
            float moss = Math.max(0f, 0.5f - t) * 0.5f;                       // nelle conche un velo di muschio
            d[i + 8] *= k * (1f - moss * 0.5f); d[i + 9] *= k * (1f + moss * 0.15f); d[i + 10] *= k * (1f - moss * 0.6f);
        }
    }

    private static float hash3(int x, int y, int z, int seed) {
        int h = x * 374761393 + y * 668265263 + z * 1274126177 + seed * 1442695041;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0xffffff) / (float) 0xffffff;
    }

    /** Rumore 3D morbido (0..1). */
    static float noise3(float x, float y, float z, int seed) {
        int xi = (int) Math.floor(x), yi = (int) Math.floor(y), zi = (int) Math.floor(z);
        float fx = x - xi, fy = y - yi, fz = z - zi;
        fx = fx * fx * (3 - 2 * fx); fy = fy * fy * (3 - 2 * fy); fz = fz * fz * (3 - 2 * fz);
        float r = 0;
        for (int k = 0; k < 8; k++) {
            int dx = k & 1, dy = (k >> 1) & 1, dz = k >> 2;
            r += hash3(xi + dx, yi + dy, zi + dz, seed) * (dx == 1 ? fx : 1 - fx) * (dy == 1 ? fy : 1 - fy) * (dz == 1 ? fz : 1 - fz);
        }
        return r;
    }

    public void vert(float x, float y, float z, float nx, float ny, float nz, float u, float v) {
        if (n + Mesh.STRIDE > d.length) d = java.util.Arrays.copyOf(d, d.length * 2);
        d[n++] = x; d[n++] = y; d[n++] = z;
        d[n++] = nx; d[n++] = ny; d[n++] = nz;
        d[n++] = u; d[n++] = v;
        d[n++] = cr; d[n++] = cg; d[n++] = cb;
        d[n++] = sw;
    }

    /**
     * Quadrilatero p0..p3 in ordine attorno al bordo. Texture: p0=(u0,v1) p1=(u1,v1) p2=(u1,v0) p3=(u0,v0),
     * cioè p0-p1 è il lato inferiore dell'immagine.
     */
    public void quad(float x0, float y0, float z0, float x1, float y1, float z1,
                     float x2, float y2, float z2, float x3, float y3, float z3,
                     float nx, float ny, float nz, float u0, float v0, float u1, float v1) {
        vert(x0, y0, z0, nx, ny, nz, u0, v1);
        vert(x1, y1, z1, nx, ny, nz, u1, v1);
        vert(x2, y2, z2, nx, ny, nz, u1, v0);
        vert(x0, y0, z0, nx, ny, nz, u0, v1);
        vert(x2, y2, z2, nx, ny, nz, u1, v0);
        vert(x3, y3, z3, nx, ny, nz, u0, v0);
    }

    /** Scatola centrata in (cx,cy,cz) con dimensioni sx,sy,sz; ogni faccia mostra l'intera texture. */
    public void box(float cx, float cy, float cz, float sx, float sy, float sz) {
        float x0 = cx - sx / 2, x1 = cx + sx / 2, y0 = cy - sy / 2, y1 = cy + sy / 2, z0 = cz - sz / 2, z1 = cz + sz / 2;
        quad(x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, 0, 0, -1, 0, 0, 1, 1);   // verso la camera
        quad(x1, y0, z1, x0, y0, z1, x0, y1, z1, x1, y1, z1, 0, 0, 1, 0, 0, 1, 1);    // lato lontano
        quad(x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1, -1, 0, 0, 0, 0, 1, 1);   // sinistra
        quad(x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, 1, 0, 0, 0, 0, 1, 1);    // destra
        quad(x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, 0, 1, 0, 0, 0, 1, 1);    // sopra
        quad(x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, 0, -1, 0, 0, 0, 1, 1);   // sotto
    }

    /**
     * Cubo con gli spigoli arrotondati (raggio r). Ogni faccia è una griglia 3x3 i cui bordi si piegano sugli spigoli, con
     * normali morbide. La texture si ripete una volta per metro.
     */
    public void roundBox(float cx, float cy, float cz, float sx, float sy, float sz, float r) { roundBox(cx, cy, cz, sx, sy, sz, r, 63); }

    /** Come sopra, ma disegna solo le facce indicate dai bit di faces (0 +x, 1 -x, 2 +y, 3 -y, 4 +z, 5 -z): quelle coperte non servono. */
    public void roundBox(float cx, float cy, float cz, float sx, float sy, float sz, float r, int faces) { roundBox(cx, cy, cz, sx, sy, sz, r, faces, 1); }

    /** seg = quanti spicchi per ogni spigolo arrotondato (1 = smusso con normali morbide, 3 = curva vera). */
    public void roundBox(float cx, float cy, float cz, float sx, float sy, float sz, float r, int faces, int seg) {
        float hx = sx / 2, hy = sy / 2, hz = sz / 2, ax = hx - r, ay = hy - r, az = hz - r;
        float[] gx = grid(hx, ax, seg), gy = grid(hy, ay, seg), gz = grid(hz, az, seg);
        int n = gx.length - 1;
        for (int face = 0; face < 6; face++) {
            if ((faces & (1 << face)) == 0) continue;
            int axis = face / 2;
            float sg = (face % 2 == 0) ? 1 : -1;
            float[] ga = axis == 0 ? gy : gx, gb = axis == 2 ? gy : gz;
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    int[][] q = {{i, j}, {i + 1, j}, {i + 1, j + 1}, {i, j}, {i + 1, j + 1}, {i, j + 1}};
                    for (int[] k : q) {
                        float a = ga[k[0]], b = gb[k[1]];
                        float px, py, pz;
                        if (axis == 0) { px = sg * hx; py = a; pz = b; }
                        else if (axis == 1) { px = a; py = sg * hy; pz = b; }
                        else { px = a; py = b; pz = sg * hz; }
                        float qx = Math.max(-ax, Math.min(ax, px)), qy = Math.max(-ay, Math.min(ay, py)), qz = Math.max(-az, Math.min(az, pz));
                        float dx = px - qx, dy = py - qy, dz = pz - qz, l = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                        dx /= l; dy /= l; dz /= l;
                        float x = qx + dx * r, y = qy + dy * r, z = qz + dz * r;
                        float u = axis == 0 ? z : x, v = axis == 1 ? z : y;
                        vert(cx + x, cy + y, cz + z, dx, dy, dz, u + 0.5f, v + 0.5f);
                    }
                }
            }
        }
    }

    /** Punti della griglia di una faccia: fitti sulla curva degli spigoli, uno solo sulla parte piatta. */
    private static float[] grid(float h, float a, int seg) {
        float[] g = new float[2 * seg + 2];
        for (int k = 0; k <= seg; k++) {
            float t = k / (float) seg;
            // sul quarto di cerchio i punti vanno distribuiti per angolo: dopo la proiezione gli spicchi vengono uguali
            float e = (float) Math.tan(t * Math.PI / 4) * (h - a);
            g[k] = -a - (h - a) + e;
            g[2 * seg + 1 - k] = a + (h - a) - e;
        }
        return g;
    }

    public void ellipsoid(float cx, float cy, float cz, float rx, float ry, float rz, int segs, int rings) {
        for (int i = 0; i < rings; i++) {
            double a0 = Math.PI * i / rings, a1 = Math.PI * (i + 1) / rings;
            for (int j = 0; j < segs; j++) {
                double b0 = 2 * Math.PI * j / segs, b1 = 2 * Math.PI * (j + 1) / segs;
                ev(cx, cy, cz, rx, ry, rz, a0, b0);
                ev(cx, cy, cz, rx, ry, rz, a1, b0);
                ev(cx, cy, cz, rx, ry, rz, a1, b1);
                ev(cx, cy, cz, rx, ry, rz, a0, b0);
                ev(cx, cy, cz, rx, ry, rz, a1, b1);
                ev(cx, cy, cz, rx, ry, rz, a0, b1);
            }
        }
    }

    private void ev(float cx, float cy, float cz, float rx, float ry, float rz, double a, double b) {
        float ux = (float) (Math.sin(a) * Math.cos(b)), uy = (float) Math.cos(a), uz = (float) (Math.sin(a) * Math.sin(b));
        float nx = ux / rx, ny = uy / ry, nz = uz / rz;
        float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        vert(cx + ux * rx, cy + uy * ry, cz + uz * rz, nx / l, ny / l, nz / l, 0.5f, 0.5f);
    }

    /** Cilindro o tronco di cono lungo l'asse verticale; con r1 = 0 è un cono. */
    public void cylinder(float cx, float y0, float y1, float cz, float r0, float r1, int segs, boolean capTop, boolean capBottom) {
        float slope = (r0 - r1) / (y1 - y0);
        for (int j = 0; j < segs; j++) {
            double b0 = 2 * Math.PI * j / segs, b1 = 2 * Math.PI * (j + 1) / segs;
            float c0 = (float) Math.cos(b0), s0 = (float) Math.sin(b0), c1 = (float) Math.cos(b1), s1 = (float) Math.sin(b1);
            float l = (float) Math.sqrt(1 + slope * slope);
            float u0 = j / (float) segs, u1 = (j + 1) / (float) segs;
            vert(cx + c0 * r0, y0, cz + s0 * r0, c0 / l, slope / l, s0 / l, u0, 1);
            vert(cx + c1 * r0, y0, cz + s1 * r0, c1 / l, slope / l, s1 / l, u1, 1);
            vert(cx + c1 * r1, y1, cz + s1 * r1, c1 / l, slope / l, s1 / l, u1, 0);
            vert(cx + c0 * r0, y0, cz + s0 * r0, c0 / l, slope / l, s0 / l, u0, 1);
            vert(cx + c1 * r1, y1, cz + s1 * r1, c1 / l, slope / l, s1 / l, u1, 0);
            vert(cx + c0 * r1, y1, cz + s0 * r1, c0 / l, slope / l, s0 / l, u0, 0);
            if (capTop && r1 > 0) {
                vert(cx, y1, cz, 0, 1, 0, 0.5f, 0.5f);
                vert(cx + c0 * r1, y1, cz + s0 * r1, 0, 1, 0, 0.5f, 0.5f);
                vert(cx + c1 * r1, y1, cz + s1 * r1, 0, 1, 0, 0.5f, 0.5f);
            }
            if (capBottom) {
                vert(cx, y0, cz, 0, -1, 0, 0.5f, 0.5f);
                vert(cx + c1 * r0, y0, cz + s1 * r0, 0, -1, 0, 0.5f, 0.5f);
                vert(cx + c0 * r0, y0, cz + s0 * r0, 0, -1, 0, 0.5f, 0.5f);
            }
        }
    }

    /** Cristallo: doppia piramide a facce piatte. */
    public void bipyramid(float cx, float cy, float cz, float r, float halfH, int sides) {
        for (int j = 0; j < sides; j++) {
            double b0 = 2 * Math.PI * j / sides, b1 = 2 * Math.PI * (j + 1) / sides, bm = (b0 + b1) / 2;
            float x0 = cx + (float) Math.cos(b0) * r, z0 = cz + (float) Math.sin(b0) * r;
            float x1 = cx + (float) Math.cos(b1) * r, z1 = cz + (float) Math.sin(b1) * r;
            float k = r / halfH, l = (float) Math.sqrt(1 + k * k);
            float nx = (float) Math.cos(bm) / l, nz = (float) Math.sin(bm) / l, ny = k / l;
            vert(x0, cy, z0, nx, ny, nz, 0.5f, 0.5f);
            vert(x1, cy, z1, nx, ny, nz, 0.5f, 0.5f);
            vert(cx, cy + halfH, cz, nx, ny, nz, 0.5f, 0.5f);
            vert(x1, cy, z1, nx, -ny, nz, 0.5f, 0.5f);
            vert(x0, cy, z0, nx, -ny, nz, 0.5f, 0.5f);
            vert(cx, cy - halfH, cz, nx, -ny, nz, 0.5f, 0.5f);
        }
    }

    /** Anello orizzontale piatto. */
    public void ring(float cx, float cy, float cz, float rIn, float rOut, int segs) {
        for (int j = 0; j < segs; j++) {
            double b0 = 2 * Math.PI * j / segs, b1 = 2 * Math.PI * (j + 1) / segs;
            float c0 = (float) Math.cos(b0), s0 = (float) Math.sin(b0), c1 = (float) Math.cos(b1), s1 = (float) Math.sin(b1);
            quad(cx + c0 * rIn, cy, cz + s0 * rIn, cx + c0 * rOut, cy, cz + s0 * rOut,
                 cx + c1 * rOut, cy, cz + s1 * rOut, cx + c1 * rIn, cy, cz + s1 * rIn, 0, 1, 0, 0.5f, 0.5f, 0.5f, 0.5f);
        }
    }

    /** Masso irregolare: sfera schiacciata con la superficie deformata da rumore (seed per variare i sassi). */
    public void rock(float cx, float cy, float cz, float rx, float ry, float rz, int seed, int segs, int rings) {
        float[][] p = new float[(rings + 1) * (segs + 1)][3];
        java.util.Random r = new java.util.Random(seed);
        float[] ph = {r.nextFloat() * 6.28f, r.nextFloat() * 6.28f, r.nextFloat() * 6.28f};
        for (int i = 0; i <= rings; i++) {
            double a = Math.PI * i / rings;
            for (int j = 0; j <= segs; j++) {
                double b = 2 * Math.PI * j / segs;
                float ux = (float) (Math.sin(a) * Math.cos(b)), uy = (float) Math.cos(a), uz = (float) (Math.sin(a) * Math.sin(b));
                float d = 1f + 0.18f * (float) Math.sin(ux * 3.1 + ph[0]) * (float) Math.cos(uz * 2.7 + ph[1])
                        + 0.12f * (float) Math.sin(uy * 5.3 + ux * 2.1 + ph[2]);
                float[] q = p[i * (segs + 1) + j];
                q[0] = cx + ux * rx * d;
                q[1] = cy + uy * ry * d;
                q[2] = cz + uz * rz * d;
            }
        }
        // normali morbide: prodotto dei due lati della griglia attorno a ogni vertice
        float[][] nr = new float[(rings + 1) * (segs + 1)][3];
        for (int i = 0; i <= rings; i++) {
            for (int j = 0; j <= segs; j++) {
                int jj = j % segs;
                float[] q = p[i * (segs + 1) + jj];
                float[] lf = p[i * (segs + 1) + (jj + segs - 1) % segs], rt = p[i * (segs + 1) + (jj + 1) % segs];
                float[] dn = p[Math.max(i - 1, 0) * (segs + 1) + jj], up = p[Math.min(i + 1, rings) * (segs + 1) + jj];
                float ax = rt[0] - lf[0], ay = rt[1] - lf[1], az = rt[2] - lf[2], bx = up[0] - dn[0], by = up[1] - dn[1], bz = up[2] - dn[2];
                float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
                float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                float ox = (q[0] - cx) / rx, oy = (q[1] - cy) / ry, oz = (q[2] - cz) / rz, ol = (float) Math.sqrt(ox * ox + oy * oy + oz * oz);
                if (l < 1e-7f || i == 0 || i == rings) { nx = ox / ol; ny = oy / ol; nz = oz / ol; }
                else {
                    nx /= l; ny /= l; nz /= l;
                    if (nx * ox + ny * oy + nz * oz < 0) { nx = -nx; ny = -ny; nz = -nz; }
                }
                float[] o = nr[i * (segs + 1) + j];
                o[0] = nx; o[1] = ny; o[2] = nz;
            }
        }
        int[] oi = {0, 1, 1, 0, 1, 0}, oj = {0, 0, 1, 0, 1, 1};
        for (int i = 0; i < rings; i++) {
            for (int j = 0; j < segs; j++) {
                for (int k = 0; k < 6; k++) {
                    int ii = i + oi[k], jj = j + oj[k];
                    float[] q = p[ii * (segs + 1) + jj], o = nr[ii * (segs + 1) + jj];
                    vert(q[0], q[1], q[2], o[0], o[1], o[2], jj / (float) segs * 2f, ii / (float) rings * 1.4f);
                }
            }
        }
    }

    public Mesh build(int tex, boolean blend, boolean fog) {
        // coordinate texture riportate vicino allo zero (di un numero intero: con la texture che si ripete non cambia niente).
        // Sui telefoni i pixel calcolano con pochi decimali: a 270 m dall'inizio del livello u = 135 perderebbe la parte
        // fine e la texture verrebbe "a strisce", stirata.
        float mu = Float.MAX_VALUE, mv = Float.MAX_VALUE;
        for (int i = 0; i < n; i += Mesh.STRIDE) { mu = Math.min(mu, d[i + 6]); mv = Math.min(mv, d[i + 7]); }
        float su = n > 0 ? (float) (int) mu : 0, sv = n > 0 ? (float) (int) mv : 0;   // tra -1 e 1 non si tocca (texture non ripetute)
        if (su != 0 || sv != 0) for (int i = 0; i < n; i += Mesh.STRIDE) { d[i + 6] -= su; d[i + 7] -= sv; }
        return new Mesh(java.util.Arrays.copyOf(d, n), n / Mesh.STRIDE, tex, blend, fog);
    }
}
