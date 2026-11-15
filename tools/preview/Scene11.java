import com.valeriodipaolo.platformgame.core.*;
import com.valeriodipaolo.platformgame.core3d.*;

/** Strumento da PC: schermate del livello 11 (fuga dalle api). */
public class Scene11 {
    static void shot(String name, String out, float x, float z, float t, boolean spin) throws Exception {
        World3D w = new World3D(new MemorySave(), 11);
        Scene3D sc = new Scene3D();
        Scene3.warp(w, x, z);
        w.fireZ = z - (name.contains("close") ? 4f : name.contains("run") ? 10f : 14f);
        Scene3.run(w, 0.3f, 0, 0, false, false);
        Scene3.run(w, t, 0, 1, false, spin);
        Scene3.shot(w, sc, name, out, true);
    }

    public static void main(String[] a) throws Exception {
        String res = a[0], out = a[1];
        for (int i = 0; i < Img.COUNT; i++) Preview.imgs[i] = javax.imageio.ImageIO.read(new java.io.File(res, Img.NAMES[i] + ".png"));
        shot("s11_start.png", out, 0, 1.5f, 0.2f, false);
        shot("s11_run.png", out, 0, 20, 0.3f, false);
        shot("s11_close.png", out, 0, 30, 0.2f, false);
    }
}
