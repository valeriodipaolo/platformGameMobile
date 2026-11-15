import com.valeriodipaolo.platformgame.core.*;
import com.valeriodipaolo.platformgame.core3d.*;

/** Strumento da PC: schermate di un livello 3D in punti scelti. Argomenti: cartella immagini, cartella uscita, livello, z1 z2 ... */
public class SceneEnv {
    public static void main(String[] a) throws Exception {
        for (int i = 0; i < Img.COUNT; i++) Preview.imgs[i] = javax.imageio.ImageIO.read(new java.io.File(a[0], Img.NAMES[i] + ".png"));
        int id = Integer.parseInt(a[2]);
        for (int k = 3; k < a.length; k++) {
            float z = Float.parseFloat(a[k]);
            World3D w = new World3D(new MemorySave(), id);
            w.enemies.clear();
            Scene3D sc = new Scene3D();
            Scene3.warp(w, 0, z);
            Scene3.run(w, 0.3f, 0, 0, false, false);
            Scene3.run(w, 0.4f, 0, 1, false, false);
            Scene3.shot(w, sc, "e" + id + "_" + (int) z + ".png", a[1], true);
        }
    }
}
