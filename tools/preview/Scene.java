import com.valeriodipaolo.platformgame.core.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/** Strumento da PC: salva alcune scene fisse (cella, trottola, segreto, fine livello) per controllare la grafica. */
public class Scene {
    static void shot(Game g, String file, String out) throws Exception {
        int W = 2400, H = 1080;
        BufferedImage b = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Preview.AwtPainter p = new Preview.AwtPainter();
        p.g = b.createGraphics();
        p.g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        p.g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        new Renderer().draw(g, p, W, H);
        p.g.dispose();
        ImageIO.write(b, "png", new File(out, file));
    }

    static void run(Game g, float sec, boolean r, boolean j, boolean s) {
        g.setInput(false, r, j, s);
        for (float t = 0; t < sec; t += 1f / 60f) g.update(1f / 60f);
    }

    public static void main(String[] a) throws Exception {
        String res = a[0], out = a[1];
        for (int i = 0; i < Img.COUNT; i++) Preview.imgs[i] = ImageIO.read(new File(res, Img.NAMES[i] + ".png"));
        Game g = new Game();
        g.viewW = 2400 / (1080 / 12f);
        g.player.x = 52; g.player.y = 8.8f; g.camX = 48;
        run(g, 0.5f, false, false, false);
        shot(g, "s_battery.png", out);

        g.enemies.clear();
        Enemy e = new Enemy(54.6f, 9); e.active = true; g.enemies.add(e);
        g.player.x = 52.4f; g.camX = 48; g.invuln = 0;
        run(g, 0.2f, true, false, true);
        shot(g, "s_spin.png", out);

        Game h = new Game();
        h.viewW = 2400 / (1080 / 12f);
        h.player.x = 84.5f; h.player.y = 8.8f; h.camX = 78;
        run(h, 0.3f, false, false, false);
        h.player.y = 8.9f; h.player.vy = -9f;
        run(h, 0.25f, false, true, false);
        shot(h, "s_secret.png", out);

        Game w = new Game();
        w.viewW = 2400 / (1080 / 12f);
        w.hitBlock(86, 7);
        w.player.x = 58.5f; w.player.y = 8.8f; w.camX = 190;
        w.player.x = w.level.flagX - 0.5f;
        run(w, 4.0f, true, false, false);
        shot(w, "s_end.png", out);
    }
}
