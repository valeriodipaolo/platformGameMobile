import com.valeriodipaolo.platformgame.core.*;
import com.valeriodipaolo.platformgame.core3d.*;

/** Strumento da PC: schermate del livello 4 (bosco d'autunno) in vari punti. */
public class Scene4 {
    static void shot(String name, String out, float x, float z, float t, boolean spin) throws Exception {
        World3D w = new World3D(new MemorySave(), 4);
        Scene3D sc = new Scene3D();
        Scene3.warp(w, x, z);
        Scene3.run(w, 0.3f, 0, 0, false, false);
        Scene3.run(w, t, 0, 1, false, spin);
        Scene3.shot(w, sc, name, out, true);
    }

    public static void main(String[] a) throws Exception {
        String res = a[0], out = a[1];
        for (int i = 0; i < Img.COUNT; i++) Preview.imgs[i] = javax.imageio.ImageIO.read(new java.io.File(res, Img.NAMES[i] + ".png"));
        shot("a_start.png", out, 0, 1.5f, 0.2f, false);
        shot("a_acorn.png", out, 0.5f, 21, 0.5f, false);
        shot("a_crow.png", out, 0, 48.5f, 0.2f, false);
        shot("a_terrace.png", out, 0, 68, 0.2f, false);
        shot("a_nook.png", out, 1, 84.5f, 0.2f, false);
        shot("a_bridge.png", out, 0, 96, 0.3f, false);
        shot("a_pits.png", out, 0, 33, 0.2f, false);
        shot("a_gate.png", out, 0, 156, 0.3f, false);
    }
}
