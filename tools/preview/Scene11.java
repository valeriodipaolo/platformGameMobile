import com.valeriodipaolo.platformgame.core.*;

import java.io.File;

/** Strumento da PC: schermate del livello 11 (il cielo) in vari punti, con le nuvole temporalesche e i mulinelli. */
public class Scene11 {
    static void at(String name, String out, float x, float t) throws Exception {
        Game g = new Game(new MemorySave(), 11);
        g.player.x = x; g.player.y = 8.8f;
        for (Enemy e : g.enemies) e.active = true;
        Scene.run(g, t, false, false, false);
        Scene.shot(g, name, out);
    }

    public static void main(String[] a) throws Exception {
        for (int i = 0; i < Img.COUNT; i++) Preview.imgs[i] = javax.imageio.ImageIO.read(new File(a[0], Img.NAMES[i] + ".png"));
        at("s11_start.png", a[1], 20f, 0.2f);
        at("s11_storm.png", a[1], 104f, 3.6f);
        at("s11_bridge.png", a[1], 120f, 2.7f);
        at("s11_end.png", a[1], 236f, 0.3f);
    }
}
