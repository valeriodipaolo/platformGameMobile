import com.valeriodipaolo.platformgame.core.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/** Strumento da PC: fine livello 2D (ingresso nel portone, catapulta) e giravolta, per controllare la grafica. */
public class SceneEnd {
    static BufferedImage shot(Game g) {
        int W = 1200, H = 540;
        BufferedImage b = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Preview.AwtPainter p = new Preview.AwtPainter();
        p.g = b.createGraphics();
        p.g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        new Renderer().draw(g, p, W, H);
        p.g.dispose();
        return b;
    }
    public static void main(String[] a) throws Exception {
        for (int i = 0; i < Img.COUNT; i++) Preview.imgs[i] = ImageIO.read(new File(a[0], Img.NAMES[i] + ".png"));
        Game g = new Game(new MemorySave(), 1);
        g.viewW = 1200 / (540 / 12f);
        g.enemies.clear(); g.batteryOwned = true; g.fireX = Float.NaN;
        Level lv = g.level;
        g.player.x = lv.flagX - 6f; g.player.y = lv.flagBaseY - g.player.h; g.camX = lv.flagX - 14;
        BufferedImage out = new BufferedImage(1200, 540 * 3, BufferedImage.TYPE_INT_RGB);
        int[] steps = {1, 1, 1};
        while (g.player.x < lv.flagX - 0.4f) { g.setInput(false, true, false); g.update(1f / 60f); }
        for (int k = 0; k < 3; k++) {
            for (int i = 0; i < 9; i++) { g.setInput(false, true, false); g.update(1f / 60f); }
            out.getGraphics().drawImage(shot(g), 0, k * 540, null);
        }
        ImageIO.write(out, "png", new File(a[1], "end2d.png"));
        Game sp = new Game(new MemorySave(), 1);
        sp.viewW = g.viewW; sp.enemies.clear(); sp.fireX = Float.NaN;
        sp.player.x = 6; sp.player.y = sp.level.startY; sp.camX = 0;
        for (int i = 0; i < 40; i++) { sp.setInput(false, false, false); sp.update(1f / 60f); }
        sp.setInput(false, false, false, true); for (int i = 0; i < 6; i++) sp.update(1f / 60f);
        ImageIO.write(shot(sp), "png", new File(a[1], "spin2d.png"));
        // catapulta + giravolta
        Game c = new Game(new MemorySave(), 1);
        c.viewW = g.viewW; c.enemies.clear(); c.batteryOwned = true; c.fireX = Float.NaN;
        c.player.x = c.level.catX + 1.2f; c.player.y = c.level.flagBaseY - c.player.h; c.camX = c.level.catX - 8;
        for (int i = 0; i < 10; i++) { c.setInput(false, false, false); c.update(1f / 60f); }
        BufferedImage o2 = new BufferedImage(1200, 540 * 2, BufferedImage.TYPE_INT_RGB);
        o2.getGraphics().drawImage(shot(c), 0, 0, null);
        c.setInput(false, false, false, true); for (int i = 0; i < 5; i++) c.update(1f / 60f);
        o2.getGraphics().drawImage(shot(c), 0, 540, null);
        ImageIO.write(o2, "png", new File(a[1], "cat2d.png"));
    }
}
