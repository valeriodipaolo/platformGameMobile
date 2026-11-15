import com.valeriodipaolo.platformgame.core.*;
import java.io.File;

/** Strumento da PC: schermate dei livelli 2D della seconda serie con i nemici nuovi. Argomenti: cartella immagini, cartella uscita, livello, x1 x2 ... */
public class SceneB {
    public static void main(String[] a) throws Exception {
        for (int i = 0; i < Img.COUNT; i++) Preview.imgs[i] = javax.imageio.ImageIO.read(new File(a[0], Img.NAMES[i] + ".png"));
        int id = Integer.parseInt(a[2]);
        for (int k = 3; k < a.length; k++) {
            float x = Float.parseFloat(a[k]);
            Game g = new Game(new MemorySave(), id);
            g.player.x = x; g.player.y = 8.8f;
            for (Enemy e : g.enemies) e.active = true;
            Scene.run(g, 1.0f + 0.5f * k, false, false, false);
            Scene.shot(g, "b" + id + "_" + (int) x + ".png", a[1]);
        }
    }
}
