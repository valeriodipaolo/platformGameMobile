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
    // per vertice: x y z w u v r g b fog
    static final int A = 10;
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

    public void end() {}

    void raster(Mesh m, float[] mvp, float[] model, float[] mv, float tr, float tg, float tb, float ta, float emissive,
                boolean depthTest, boolean fog) {
        BufferedImage t = m.tex >= 0 ? tex[m.tex] : null;
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
                float nx = model[0] * v[o + 3] + model[4] * v[o + 4] + model[8] * v[o + 5];
                float ny = model[1] * v[o + 3] + model[5] * v[o + 4] + model[9] * v[o + 5];
                float nz = model[2] * v[o + 3] + model[6] * v[o + 4] + model[10] * v[o + 5];
                float nl = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                float dif = Math.max(0, (nx * LX + ny * LY + nz * LZ) / Math.max(nl, 1e-6f));
                float l = (AMBIENT + DIFFUSE * dif) * (1 - emissive) + emissive;
                d[4] = v[o + 6]; d[5] = v[o + 7];
                d[6] = v[o + 8] * tr * l; d[7] = v[o + 9] * tg * l; d[8] = v[o + 10] * tb * l;
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
                float al = ta;
                if (t != null) {
                    int sx = Math.floorMod((int) Math.floor(u * tw), tw), sy = Math.floorMod((int) Math.floor(vv * th), th);
                    int argb = t.getRGB(sx, sy);
                    al *= ((argb >>> 24) & 255) / 255f;
                    r *= ((argb >> 16) & 255) / 255f; g *= ((argb >> 8) & 255) / 255f; bl *= (argb & 255) / 255f;
                }
                if (!blend) { r *= al; g *= al; bl *= al; al = 1; }
                if (al < 0.02f) continue;
                r = r + (fr - r) * fo; g = g + (fg - g) * fo; bl = bl + (fb - bl) * fo;
                if (blend) {
                    int d = color[idx];
                    r = r * al + ((d >> 16) & 255) / 255f * (1 - al);
                    g = g * al + ((d >> 8) & 255) / 255f * (1 - al);
                    bl = bl * al + (d & 255) / 255f * (1 - al);
                } else if (depthTest) {
                    depth[idx] = z;
                }
                color[idx] = 0xFF000000 | (c8(r) << 16) | (c8(g) << 8) | c8(bl);
            }
        }
    }

    static int c8(float f) { return Math.max(0, Math.min(255, Math.round(f * 255))); }

    BufferedImage image() {
        BufferedImage b = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        b.setRGB(0, 0, W, H, color, 0, W);
        return b;
    }
}
