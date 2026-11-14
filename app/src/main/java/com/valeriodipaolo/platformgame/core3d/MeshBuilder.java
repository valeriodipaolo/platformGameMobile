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
    public void roundBox(float cx, float cy, float cz, float sx, float sy, float sz, float r) {
        float hx = sx / 2, hy = sy / 2, hz = sz / 2, ax = hx - r, ay = hy - r, az = hz - r;
        float[] gx = {-hx, -ax, ax, hx}, gy = {-hy, -ay, ay, hy}, gz = {-hz, -az, az, hz};
        for (int face = 0; face < 6; face++) {
            int axis = face / 2;
            float sg = (face % 2 == 0) ? 1 : -1;
            float[] ga = axis == 0 ? gy : gx, gb = axis == 2 ? gy : gz;
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
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
        for (int i = 0; i < rings; i++) {
            for (int j = 0; j < segs; j++) {
                float[] a = p[i * (segs + 1) + j], b = p[(i + 1) * (segs + 1) + j];
                float[] c = p[(i + 1) * (segs + 1) + j + 1], e = p[i * (segs + 1) + j + 1];
                float ux = (a[0] + b[0] + c[0] + e[0]) / 4 - cx, uy = (a[1] + b[1] + c[1] + e[1]) / 4 - cy, uz = (a[2] + b[2] + c[2] + e[2]) / 4 - cz;
                float ax = b[0] - a[0], ay = b[1] - a[1], az = b[2] - a[2], bx = e[0] - a[0], by = e[1] - a[1], bz = e[2] - a[2];
                float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
                float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (l < 1e-9f) continue;
                if (nx * ux + ny * uy + nz * uz < 0) { nx = -nx; ny = -ny; nz = -nz; }
                nx /= l; ny /= l; nz /= l;
                float u0 = j / (float) segs * 2f, u1 = (j + 1) / (float) segs * 2f, v0 = i / (float) rings * 1.4f, v1 = (i + 1) / (float) rings * 1.4f;
                vert(a[0], a[1], a[2], nx, ny, nz, u0, v0);
                vert(b[0], b[1], b[2], nx, ny, nz, u0, v1);
                vert(c[0], c[1], c[2], nx, ny, nz, u1, v1);
                vert(a[0], a[1], a[2], nx, ny, nz, u0, v0);
                vert(c[0], c[1], c[2], nx, ny, nz, u1, v1);
                vert(e[0], e[1], e[2], nx, ny, nz, u1, v0);
            }
        }
    }

    public Mesh build(int tex, boolean blend, boolean fog) {
        return new Mesh(java.util.Arrays.copyOf(d, n), n / Mesh.STRIDE, tex, blend, fog);
    }
}
