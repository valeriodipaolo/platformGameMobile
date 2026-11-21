import com.valeriodipaolo.platformgame.core.*;
import com.valeriodipaolo.platformgame.core3d.*;

import java.io.*;
import java.util.IdentityHashMap;

/**
 * Strumento da PC: registra tutto quello che la scena 3D disegna in un fotogramma (mesh, matrici, colori) in un file, insieme agli
 * shader del telefono; tools/glreplay.py lo ridisegna con OpenGL ES vero (Mesa), così l'anteprima è identica al telefono.
 * Uso: GlDump outFile livello z [x] [personaggio]
 */
public class GlDump implements Gfx3D {
    private final DataOutputStream out;
    private final IdentityHashMap<Mesh, Integer> ids = new IdentityHashMap<Mesh, Integer>();

    GlDump(DataOutputStream out) { this.out = out; }

    static void str(DataOutputStream o, String s) throws IOException {
        byte[] b = s.getBytes("UTF-8");
        o.writeInt(b.length);
        o.write(b);
    }

    private int mesh(Mesh m) {
        Integer id = ids.get(m);
        if (id != null) return id;
        id = ids.size();
        ids.put(m, id);
        try {
            out.writeByte(1);
            out.writeInt(id);
            out.writeInt(m.count);
            str(out, m.tex < 0 ? "" : Img.NAMES[m.tex]);
            out.writeByte(m.blend ? 1 : 0);
            out.writeByte(m.cutout ? 1 : 0);
            out.writeByte(m.fog ? 1 : 0);
            out.writeByte(m.castShadow ? 1 : 0);
            out.writeFloat(m.cx); out.writeFloat(m.cy); out.writeFloat(m.cz); out.writeFloat(m.radius);
            out.writeFloat(m.spec);
            out.writeFloat(m.shine);
            int n = m.v == null ? 0 : Math.min(m.v.length, m.count * Mesh.STRIDE);
            out.writeInt(n);
            for (int i = 0; i < n; i++) out.writeFloat(m.v[i]);
        } catch (IOException e) { throw new RuntimeException(e); }
        return id;
    }

    private void floats(float[] a) throws IOException { for (int i = 0; i < 16; i++) out.writeFloat(a[i]); }

    /** Camera ravvicinata facoltativa (-Dcam=ex,ey,ez,tx,ty,tz): per guardare da vicino un nemico o un oggetto. */
    static float[] CAM;

    @Override public void begin(float[] view, float[] proj, float fr, float fg, float fb, float fs, float fe, float fm) {
        if (CAM != null) {
            view = new float[16]; proj = new float[16];
            Mat4.lookAt(view, CAM[0], CAM[1], CAM[2], CAM[3], CAM[4], CAM[5]);
            Mat4.perspective(proj, 28f, 1280f / 576f, 0.1f, 400f);
        }
        try { out.writeByte(2); floats(view); floats(proj); out.writeFloat(fr); out.writeFloat(fg); out.writeFloat(fb); out.writeFloat(fs); out.writeFloat(fe); out.writeFloat(fm);
              float[] l = new float[16]; GlShaders.lightMatrix(view, l); floats(l); }
        catch (IOException e) { throw new RuntimeException(e); }
    }

    @Override public void backdrop(Mesh m) {
        int id = mesh(m);
        try { out.writeByte(3); out.writeInt(id); } catch (IOException e) { throw new RuntimeException(e); }
    }

    @Override public void draw(Mesh m, float[] model, float r, float g, float b, float a, float em) {
        if (m == null || m.count == 0) return;
        int id = mesh(m);
        try { out.writeByte(4); out.writeInt(id); floats(model); out.writeFloat(r); out.writeFloat(g); out.writeFloat(b); out.writeFloat(a); out.writeFloat(em); }
        catch (IOException e) { throw new RuntimeException(e); }
    }

    @Override public void time(float t) { try { out.writeByte(5); out.writeFloat(t); } catch (IOException e) { throw new RuntimeException(e); } }

    @Override public void grade(float r, float g, float b) {
        try { out.writeByte(6); out.writeFloat(r); out.writeFloat(g); out.writeFloat(b); } catch (IOException e) { throw new RuntimeException(e); }
    }

    @Override public void end() { try { out.writeByte(7); } catch (IOException e) { throw new RuntimeException(e); } }

    public static void main(String[] a) throws Exception {
        int id = Integer.parseInt(a[1]);
        float z = Float.parseFloat(a[2]), x = a.length > 3 ? Float.parseFloat(a[3]) : 0f;
        HeroModel.character = a.length > 4 ? Integer.parseInt(a[4]) : 0;
        String cam = System.getProperty("cam");
        if (cam != null) { String[] p = cam.split(","); CAM = new float[6]; for (int i = 0; i < 6; i++) CAM[i] = Float.parseFloat(p[i]); }
        World3D w = new World3D(new MemorySave(), id);
        Scene3D sc = new Scene3D();
        w.px = x; w.pz = z; w.py = Math.max(0, w.topAt(x, z)) + 0.01f;
        for (int i = 0; i < 40; i++) { w.setInput(0, i > 25 ? 0.6f : 0f, false, false); w.hp = 99; w.update(1f / 60f); }
        if (System.getProperty("foes") != null) for (Enemy3 e : w.enemies) System.out.println("nemico " + e.type + " " + e.x + " " + e.y + " " + e.z + " modo " + e.mode);
        DataOutputStream o = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(a[0]), 1 << 20));
        o.writeInt(0x474C4431);
        str(o, GlShaders.VERTEX);
        str(o, GlShaders.FRAGMENT);
        for (String sh : new String[] {GlShaders.SHADOW_VERTEX, GlShaders.SHADOW_FRAGMENT, GlShaders.POST_VERTEX, GlShaders.BRIGHT_FRAGMENT, GlShaders.BLUR_FRAGMENT, GlShaders.COMPOSITE_FRAGMENT}) str(o, sh);
        o.writeInt(GlShaders.SHADOW_SIZE);
        o.writeFloat(GlShaders.SHADOW_RADIUS);
        o.writeFloat(Gfx3D.LX); o.writeFloat(Gfx3D.LY); o.writeFloat(Gfx3D.LZ);
        GlDump d = new GlDump(o);
        sc.render(w, d, 1280f / 576f);       // un primo giro prepara le mesh (alcune nascono al primo disegno)
        o.writeByte(9);                      // 9 = da qui inizia il fotogramma da mostrare
        sc.render(w, d, 1280f / 576f);
        o.writeByte(0);
        o.close();
    }
}
