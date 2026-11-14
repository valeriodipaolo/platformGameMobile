import com.valeriodipaolo.platformgame.core.*;
import com.valeriodipaolo.platformgame.core3d.*;

/** Strumento da PC: schermate del livello 8 (bosco in fiamme) in vari punti. */
public class Scene10 {
    static void shot(String name, String out, float x, float z, float t, boolean spin) throws Exception {
        World3D w = new World3D(new MemorySave(), 10);
        Scene3D sc = new Scene3D();
        Scene3.warp(w, x, z);
        Scene3.run(w, 0.3f, 0, 0, false, false);
        Scene3.run(w, t, 0, 1, false, spin);
        Scene3.shot(w, sc, name, out, true);
    }

    public static void main(String[] a) throws Exception {
        String res = a[0], out = a[1];
        for (int i = 0; i < Img.COUNT; i++) Preview.imgs[i] = javax.imageio.ImageIO.read(new java.io.File(res, Img.NAMES[i] + ".png"));
        shot("s10_start.png", out, 0, 1.5f, 0.2f, false);
        shot("s10_pad.png", out, 0, 9, 0.8f, false);
        shot("s10_thorns.png", out, 0, 21, 1.0f, false);
        shot("s10_river1.png", out, 0, 41, 1.2f, false);
        shot("s10_canopy.png", out, 0, 62, 1.0f, false);
        shot("s10_river2.png", out, 0, 95, 0.5f, false);
        shot("s10_field.png", out, 0, 137, 0.9f, false);
    }
}
