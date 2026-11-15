import com.valeriodipaolo.platformgame.core.*;
import com.valeriodipaolo.platformgame.core3d.*;

/** Strumento da PC: schermate del livello 8 (bosco in fiamme) in vari punti. */
public class Scene12 {
    static void shot(String name, String out, float x, float z, float t, boolean spin) throws Exception {
        World3D w = new World3D(new MemorySave(), 12);
        Scene3D sc = new Scene3D();
        Scene3.warp(w, x, z);
        Scene3.run(w, 0.3f, 0, 0, false, false);
        Scene3.run(w, t, 0, 1, false, spin);
        Scene3.shot(w, sc, name, out, true);
    }

    public static void main(String[] a) throws Exception {
        String res = a[0], out = a[1];
        for (int i = 0; i < Img.COUNT; i++) Preview.imgs[i] = javax.imageio.ImageIO.read(new java.io.File(res, Img.NAMES[i] + ".png"));
        shot("s12_start.png", out, 0, 3f, 0.2f, false);
        shot("s12_bridge.png", out, 0, 24f, 0.5f, false);
        shot("s12_pit.png", out, 0, 56f, 0.8f, false);
        shot("s12_balcony.png", out, 0, 86f, 0.5f, false);
        shot("s12_path.png", out, 0, 98f, 0.5f, false);
        shot("s12_islands.png", out, 0, 138f, 0.5f, false);
    }
}

