import com.valeriodipaolo.platformgame.core.*;
import com.valeriodipaolo.platformgame.core3d.*;

/** Strumento da PC: schermate del livello 4 (bosco d'autunno) in vari punti. */
public class Scene6 {
    static void shot(String name, String out, float x, float z, float t, boolean spin) throws Exception {
        World3D w = new World3D(new MemorySave(), 6);
        Scene3D sc = new Scene3D();
        Scene3.warp(w, x, z);
        Scene3.run(w, 0.3f, 0, 0, false, false);
        Scene3.run(w, t, 0, 1, false, spin);
        Scene3.shot(w, sc, name, out, true);
    }

    public static void main(String[] a) throws Exception {
        String res = a[0], out = a[1];
        for (int i = 0; i < Img.COUNT; i++) Preview.imgs[i] = javax.imageio.ImageIO.read(new java.io.File(res, Img.NAMES[i] + ".png"));
        shot("b_start.png", out, 0, 1.5f, 0.2f, false);
        shot("b_slab.png", out, 0, 16, 0.2f, false);
        shot("b_diag.png", out, 0, 25.5f, 0.2f, false);
        shot("b_lake.png", out, 2, 71, 0.2f, false);
        shot("b_lake2.png", out, 2, 80, 0.2f, false);
        shot("b_cave.png", out, 1, 94.5f, 0.2f, false);
        shot("b_zig.png", out, 2, 107, 0.2f, false);
        shot("b_grove.png", out, 0, 130, 0.3f, false);
        shot("b_end.png", out, 0, 160, 0.3f, false);
    }
}
