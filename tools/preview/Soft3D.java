import com.valeriodipaolo.platformgame.core3d.Gfx3D;
import com.valeriodipaolo.platformgame.core3d.Mat4;
import com.valeriodipaolo.platformgame.core3d.Mesh;

import java.awt.image.BufferedImage;

/** Disegnatore 3D software (solo per le anteprime sul PC): stessa scena e stesse matrici del telefono. */
public class Soft3D implements Gfx3D {
    final int W, H;
    final int[] color;
    final float[] depth;
    final BufferedImage[] tex;
    float[] view, proj;
    final float[] vp = new float[16], mvp = new float[16], mv = new float[16];
    float fr, fg, fb, fogStart, fogEnd, fogMax;
    float lvx, lvy, lvz, upx, upy, upz, spec, shine, emis, alpha;
    boolean cut;
    final java.util.HashMap<BufferedImage, BufferedImage[]> mips = new java.util.HashMap<>();
    // per vertice: x y z w u v r g b fog
    static final int A = 16;    // + nx ny nz (spazio camera) px py pz (spazio camera)
    final float[][] in = new float[3][A], poly = new float[8][A], tmp = new float[8][A];

    Soft3D(int w, int h, BufferedImage[] tex) {
        W = w; H = h; this.tex = tex;
        color = new int[w * h];
        depth = new float[w * h];
    }

    public void begin(float[] view, float[] proj, float fr, float fg, float fb, float fogStart, float fogEnd, float fogMax) {
        this.view = view; this.proj = proj;
        this.fr = fr; this.fg = fg; this.fb = fb; this.fogStart = fogStart; this.fogEnd = fogEnd; this.fogMax = fogMax;
        Mat4.mul(vp, proj, view);
        lvx = view[0] * LX + view[4] * LY + view[8] * LZ;
        lvy = view[1] * LX + view[5] * LY + view[9] * LZ;
        lvz = view[2] * LX + view[6] * LY + view[10] * LZ;
        upx = view[4]; upy = view[5]; upz = view[6];
        java.util.Arrays.fill(depth, Float.MAX_VALUE);
        java.util.Arrays.fill(color, 0xFF000000);
    }

    public void backdrop(Mesh m) {
        raster(m, Mat4.identity(), Mat4.identity(), null, 1, 1, 1, 1, 1, false, false);
    }

    public void draw(Mesh m, float[] model, float r, float g, float b, float a, float emissive) {
        Mat4.mul(mvp, vp, model);
        Mat4.mul(mv, view, model);
        raster(m, mvp, model, mv, r, g, b, a, emissive, true, m.fog);
    }

    private float gr = 1, gg = 1, gb = 1;

    public void grade(float r, float g, float b) { gr = r; gg = g; gb = b; }

    public void time(float t) {}

    public void end() {}

    void raster(Mesh m, float[] mvp, float[] model, float[] mv, float tr, float tg, float tb, float ta, float emissive,
                boolean depthTest, boolean fog) {
        BufferedImage t = m.tex >= 0 ? tex[m.tex] : null;
        spec = m.spec; shine = m.shine; cut = m.cutout; emis = emissive; alpha = ta;
        float[] v = m.v;
        for (int i = 0; i < m.count; i += 3) {
            for (int k = 0; k < 3; k++) {
                int o = (i + k) * Mesh.STRIDE;
                float x = v[o], y = v[o + 1], z = v[o + 2];
                float[] d = in[k];
                d[0] = mvp[0] * x + mvp[4] * y + mvp[8] * z + mvp[12];
                d[1] = mvp[1] * x + mvp[5] * y + mvp[9] * z + mvp[13];
                d[2] = mvp[2] * x + mvp[6] * y + mvp[10] * z + mvp[14];
                d[3] = mvp[3] * x + mvp[7] * y + mvp[11] * z + mvp[15];
                float[] mm = mv != null ? mv : view;
                float[] mmm = mv != null ? mv : Mat4.identity();
                float nx = mmm[0] * v[o + 3] + mmm[4] * v[o + 4] + mmm[8] * v[o + 5];
                float ny = mmm[1] * v[o + 3] + mmm[5] * v[o + 4] + mmm[9] * v[o + 5];
                float nz = mmm[2] * v[o + 3] + mmm[6] * v[o + 4] + mmm[10] * v[o + 5];
                d[4] = v[o + 6]; d[5] = v[o + 7];
                d[6] = v[o + 8] * tr; d[7] = v[o + 9] * tg; d[8] = v[o + 10] * tb;
                d[10] = nx; d[11] = ny; d[12] = nz;
                d[13] = mmm[0] * x + mmm[4] * y + mmm[8] * z + mmm[12];
                d[14] = mmm[1] * x + mmm[5] * y + mmm[9] * z + mmm[13];
                d[15] = mmm[2] * x + mmm[6] * y + mmm[10] * z + mmm[14];
                float f = 0;
                if (fog && mv != null) {
                    float vz = -(mv[2] * x + mv[6] * y + mv[10] * z + mv[14]);
                    f = Math.max(0, Math.min(fogMax, (vz - fogStart) / (fogEnd - fogStart)));
                }
                d[9] = f;
            }
            // taglio sul piano vicino
            int n = 0;
            for (int k = 0; k < 3; k++) {
                float[] a = in[k], b = in[(k + 1) % 3];
                boolean ia = a[3] > 0.05f, ib = b[3] > 0.05f;
                if (ia) System.arraycopy(a, 0, poly[n++], 0, A);
                if (ia != ib) {
                    float s = (0.05f - a[3]) / (b[3] - a[3]);
                    for (int q = 0; q < A; q++) poly[n][q] = a[q] + (b[q] - a[q]) * s;
                    n++;
                }
            }
            for (int k = 1; k + 1 < n; k++) tri(poly[0], poly[k], poly[k + 1], t, ta, depthTest, m.blend);
        }
    }

    void tri(float[] a, float[] b, float[] c, BufferedImage t, float ta, boolean depthTest, boolean blend) {
        float ax = (a[0] / a[3] * 0.5f + 0.5f) * W, ay = (1 - (a[1] / a[3] * 0.5f + 0.5f)) * H;
        float bx = (b[0] / b[3] * 0.5f + 0.5f) * W, by = (1 - (b[1] / b[3] * 0.5f + 0.5f)) * H;
        float cx = (c[0] / c[3] * 0.5f + 0.5f) * W, cy = (1 - (c[1] / c[3] * 0.5f + 0.5f)) * H;
        float area = (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
        if (Math.abs(area) < 1e-6f) return;
        int x0 = Math.max(0, (int) Math.floor(Math.min(ax, Math.min(bx, cx)))), x1 = Math.min(W - 1, (int) Math.ceil(Math.max(ax, Math.max(bx, cx))));
        int y0 = Math.max(0, (int) Math.floor(Math.min(ay, Math.min(by, cy)))), y1 = Math.min(H - 1, (int) Math.ceil(Math.max(ay, Math.max(by, cy))));
        float wa = 1 / a[3], wb = 1 / b[3], wc = 1 / c[3];
        int tw = t == null ? 0 : t.getWidth(), th = t == null ? 0 : t.getHeight();
        BufferedImage[] chain = t == null ? null : mipChain(t);
        // derivate delle coordinate texture (u/w, v/w, 1/w sono lineari sullo schermo)
        float d0x = (by - cy) / area, d0y = (cx - bx) / area, d1x = (cy - ay) / area, d1y = (ax - cx) / area;
        float d2x = -d0x - d1x, d2y = -d0y - d1y;
        float Wx = d0x * wa + d1x * wb + d2x * wc, Wy = d0y * wa + d1y * wb + d2y * wc;
        float Ux = d0x * a[4] * wa + d1x * b[4] * wb + d2x * c[4] * wc, Uy = d0y * a[4] * wa + d1y * b[4] * wb + d2y * c[4] * wc;
        float Vx = d0x * a[5] * wa + d1x * b[5] * wb + d2x * c[5] * wc, Vy = d0y * a[5] * wa + d1y * b[5] * wb + d2y * c[5] * wc;
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                float px = x + 0.5f, py = y + 0.5f;
                float e0 = ((bx - px) * (cy - py) - (by - py) * (cx - px)) / area;
                float e1 = ((cx - px) * (ay - py) - (cy - py) * (ax - px)) / area;
                float e2 = 1 - e0 - e1;
                if (e0 < 0 || e1 < 0 || e2 < 0) continue;
                float iw = e0 * wa + e1 * wb + e2 * wc;
                float z = (e0 * a[2] * wa + e1 * b[2] * wb + e2 * c[2] * wc) / iw;
                int idx = y * W + x;
                if (depthTest && z >= depth[idx]) continue;
                float u = (e0 * a[4] * wa + e1 * b[4] * wb + e2 * c[4] * wc) / iw;
                float vv = (e0 * a[5] * wa + e1 * b[5] * wb + e2 * c[5] * wc) / iw;
                float r = (e0 * a[6] * wa + e1 * b[6] * wb + e2 * c[6] * wc) / iw;
                float g = (e0 * a[7] * wa + e1 * b[7] * wb + e2 * c[7] * wc) / iw;
                float bl = (e0 * a[8] * wa + e1 * b[8] * wb + e2 * c[8] * wc) / iw;
                float fo = (e0 * a[9] * wa + e1 * b[9] * wb + e2 * c[9] * wc) / iw;
                float nx = (e0 * a[10] * wa + e1 * b[10] * wb + e2 * c[10] * wc) / iw;
                float ny = (e0 * a[11] * wa + e1 * b[11] * wb + e2 * c[11] * wc) / iw;
                float nz = (e0 * a[12] * wa + e1 * b[12] * wb + e2 * c[12] * wc) / iw;
                float qx = (e0 * a[13] * wa + e1 * b[13] * wb + e2 * c[13] * wc) / iw;
                float qy = (e0 * a[14] * wa + e1 * b[14] * wb + e2 * c[14] * wc) / iw;
                float qz = (e0 * a[15] * wa + e1 * b[15] * wb + e2 * c[15] * wc) / iw;
                float nlen = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (nlen > 1e-6f) { nx /= nlen; ny /= nlen; nz /= nlen; }
                float ta_ = 1, tr = 1, tg = 1, tb = 1;
                if (t != null) {
                    float dux = (Ux - u * Wx) / iw, duy = (Uy - u * Wy) / iw, dvx = (Vx - vv * Wx) / iw, dvy = (Vy - vv * Wy) / iw;
                    float foot = Math.max(Math.max(Math.abs(dux), Math.abs(duy)) * tw, Math.max(Math.abs(dvx), Math.abs(dvy)) * th);
                    float lod = foot <= 1 ? 0 : (float) (Math.log(foot) / Math.log(2));
                    lod = Math.min(lod, chain.length - 1);
                    int l0 = (int) lod;
                    int l1 = Math.min(chain.length - 1, l0 + 1);
                    float fr0 = lod - l0;
                    int ta0 = sample(chain[l0], u, vv), ta1 = l1 == l0 ? ta0 : sample(chain[l1], u, vv);
                    ta_ = (((ta0 >>> 24) & 255) * (1 - fr0) + ((ta1 >>> 24) & 255) * fr0) / 255f;
                    tr = (((ta0 >> 16) & 255) * (1 - fr0) + ((ta1 >> 16) & 255) * fr0) / 255f;
                    tg = (((ta0 >> 8) & 255) * (1 - fr0) + ((ta1 >> 8) & 255) * fr0) / 255f;
                    tb = ((ta0 & 255) * (1 - fr0) + (ta1 & 255) * fr0) / 255f;
                }
                if (blend) {
                    // trasparenze semplici (ombre, anelli, bandiera)
                    float l = (1 - emis) * (0.6f + 0.4f * Math.max(0, nx * lvx + ny * lvy + nz * lvz)) + emis;
                    float al = ta_ * alpha;
                    if (al < 0.02f) continue;
                    r = r * tr * ta_ * l * alpha; g = g * tg * ta_ * l * alpha; bl = bl * tb * ta_ * l * alpha;
                    r = r + (fr * al - r) * fo; g = g + (fg * al - g) * fo; bl = bl + (fb * al - bl) * fo;
                    int d = color[idx];
                    r = r + ((d >> 16) & 255) / 255f * (1 - al);
                    g = g + ((d >> 8) & 255) / 255f * (1 - al);
                    bl = bl + (d & 255) / 255f * (1 - al);
                    color[idx] = 0xFF000000 | (c8(r) << 16) | (c8(g) << 8) | c8(bl);
                    continue;
                }
                if (cut && ta_ < 0.45f) continue;
                if (ta_ < 0.02f) continue;
                float sr = Math.min(1, tr * r), sg = Math.min(1, tg * g), sb = Math.min(1, tb * bl);
                float ar = sr * sr, ag = sg * sg, ab = sb * sb;
                float pl = (float) Math.sqrt(qx * qx + qy * qy + qz * qz);
                float vx = -qx / pl, vy = -qy / pl, vz = -qz / pl;
                float nl = Math.max(0, nx * lvx + ny * lvy + nz * lvz);
                float hemi = (nx * upx + ny * upy + nz * upz) * 0.5f + 0.5f;
                float cr = ar * (SUN_R * nl + GND_R + (SKY_R - GND_R) * hemi) * gr;
                float cg = ag * (SUN_G * nl + GND_G + (SKY_G - GND_G) * hemi) * gg;
                float cb = ab * (SUN_B * nl + GND_B + (SKY_B - GND_B) * hemi) * gb;
                if (spec > 0) {
                    float hx = lvx + vx, hy = lvy + vy, hz = lvz + vz;
                    float hl = (float) Math.sqrt(hx * hx + hy * hy + hz * hz);
                    float nh = Math.max(0, (nx * hx + ny * hy + nz * hz) / hl);
                    float fres = (float) Math.pow(1 - Math.max(0, nx * vx + ny * vy + nz * vz), 3);
                    float sp = spec * (float) Math.pow(nh, shine) * nl;
                    cr += SUN_R * sp + SKY_R * fres * spec * 1.4f;
                    cg += SUN_G * sp + SKY_G * fres * spec * 1.4f;
                    cb += SUN_B * sp + SKY_B * fres * spec * 1.4f;
                }
                cr += (fr * fr - cr) * fo; cg += (fg * fg - cg) * fo; cb += (fb * fb - cb) * fo;
                r = tone(cr); g = tone(cg); bl = tone(cb);
                float[] sc = {r, g, bl};
                sat(sc);
                r = sc[0]; g = sc[1]; bl = sc[2];
                r += (sr - r) * emis; g += (sg - g) * emis; bl += (sb - bl) * emis;
                if (depthTest) depth[idx] = z;
                color[idx] = 0xFF000000 | (c8(r) << 16) | (c8(g) << 8) | c8(bl);
            }
        }
    }

    BufferedImage[] mipChain(BufferedImage t) {
        BufferedImage[] c = mips.get(t);
        if (c != null) return c;
        java.util.ArrayList<BufferedImage> l = new java.util.ArrayList<>();
        BufferedImage cur = new BufferedImage(t.getWidth(), t.getHeight(), BufferedImage.TYPE_INT_ARGB);
        cur.getGraphics().drawImage(t, 0, 0, null);
        l.add(cur);
        while (cur.getWidth() > 1 && cur.getHeight() > 1) {
            int w = cur.getWidth() / 2, h = cur.getHeight() / 2;
            BufferedImage nx = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int[] p = {cur.getRGB(2 * x, 2 * y), cur.getRGB(2 * x + 1, 2 * y), cur.getRGB(2 * x, 2 * y + 1), cur.getRGB(2 * x + 1, 2 * y + 1)};
                    float a = 0, r = 0, g = 0, b = 0;
                    for (int q : p) {   // media con trasparenza premoltiplicata, come i mipmap veri
                        float al = ((q >>> 24) & 255) / 255f;
                        a += al; r += ((q >> 16) & 255) * al; g += ((q >> 8) & 255) * al; b += (q & 255) * al;
                    }
                    int ia = Math.round(a / 4 * 255);
                    int ir = a > 0 ? Math.round(r / a) : 0, ig = a > 0 ? Math.round(g / a) : 0, ib = a > 0 ? Math.round(b / a) : 0;
                    nx.setRGB(x, y, (ia << 24) | (ir << 16) | (ig << 8) | ib);
                }
            }
            l.add(nx);
            cur = nx;
        }
        c = l.toArray(new BufferedImage[0]);
        mips.put(t, c);
        return c;
    }

    /** Campione bilineare con ripetizione. */
    static int sample(BufferedImage t, float u, float v) {
        int w = t.getWidth(), h = t.getHeight();
        float fx = u * w - 0.5f, fy = v * h - 0.5f;
        int x0 = (int) Math.floor(fx), y0 = (int) Math.floor(fy);
        float ax = fx - x0, ay = fy - y0;
        int c00 = t.getRGB(Math.floorMod(x0, w), Math.floorMod(y0, h)), c10 = t.getRGB(Math.floorMod(x0 + 1, w), Math.floorMod(y0, h));
        int c01 = t.getRGB(Math.floorMod(x0, w), Math.floorMod(y0 + 1, h)), c11 = t.getRGB(Math.floorMod(x0 + 1, w), Math.floorMod(y0 + 1, h));
        int out = 0;
        for (int sh = 24; sh >= 0; sh -= 8) {
            float a = ((c00 >>> sh) & 255) * (1 - ax) + ((c10 >>> sh) & 255) * ax;
            float b = ((c01 >>> sh) & 255) * (1 - ax) + ((c11 >>> sh) & 255) * ax;
            out |= (Math.round(a * (1 - ay) + b * ay) & 255) << sh;
        }
        return out;
    }

    static float tone(float c) {
        c = Math.max(0, c);
        c = (c * (2.51f * c + 0.03f)) / (c * (2.43f * c + 0.59f) + 0.14f);
        return (float) Math.sqrt(Math.max(0, Math.min(1, c)));
    }

    static void sat(float[] c) {
        float g = 0.299f * c[0] + 0.587f * c[1] + 0.114f * c[2];
        for (int i = 0; i < 3; i++) c[i] = Math.max(0, Math.min(1, g + (c[i] - g) * 1.22f));
    }

    static int c8(float f) { return Math.max(0, Math.min(255, Math.round(f * 255))); }

    BufferedImage image() {
        BufferedImage b = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        b.setRGB(0, 0, W, H, color, 0, W);
        return b;
    }
}
