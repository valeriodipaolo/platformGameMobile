import com.valeriodipaolo.platformgame.core.*;

import java.io.File;

/** Strumento da PC: schermate del livello 9 (primavera). */
public class Scene9 {
    static void at(float x, String file, String out) throws Exception {
        Game g = new Game(new MemorySave(), 9);
        g.viewW = 2400 / (1080 / 12f);
        g.player.x = x; g.player.y = 8.8f; g.camX = x - 6;
        Scene.run(g, 0.5f, false, false, false);
        Scene.shot(g, file, out);
    }

    public static void main(String[] a) throws Exception {
        String res = a[0], out = a[1];
        for (int i = 0; i < Img.COUNT; i++) Preview.imgs[i] = javax.imageio.ImageIO.read(new File(res, Img.NAMES[i] + ".png"));
        at(4, "s9_start.png", out);
        at(17, "s9_pad.png", out);
        at(38, "s9_river1.png", out);
        at(66, "s9_cherry.png", out);
        at(88, "s9_river2.png", out);
        at(150, "s9_thorns.png", out);
    }
}
