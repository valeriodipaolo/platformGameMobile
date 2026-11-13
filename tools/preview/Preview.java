import com.valeriodipaolo.platformgame.core.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Strumento da PC (non fa parte dell'app): fa giocare il livello a un bot e salva alcune
 * schermate, per controllare che il livello sia completabile e come appare la grafica.
 * Uso: tools/preview/run.sh
 */
public class Preview {
    static BufferedImage[] imgs = new BufferedImage[Img.COUNT];
    static float FOE = 1.6f;

    static class AwtPainter implements Painter {
        Graphics2D g;
        public float aspect(int i) { return imgs[i].getWidth() / (float) imgs[i].getHeight(); }
        public void image(int i, float x, float y, float w, float h, boolean flip, float alpha) {
            Composite old = g.getComposite();
            if (alpha < 1) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0, alpha)));
            int x0 = Math.round(x), y0 = Math.round(y), x1 = Math.round(x + w), y1 = Math.round(y + h);
            BufferedImage b = imgs[i];
            if (flip) g.drawImage(b, x1, y0, x0, y1, 0, 0, b.getWidth(), b.getHeight(), null);
            else g.drawImage(b, x0, y0, x1, y1, 0, 0, b.getWidth(), b.getHeight(), null);
            g.setComposite(old);
        }
        public void rect(float x, float y, float w, float h, int argb) {
            g.setColor(new Color(argb, true));
            g.fillRect(Math.round(x), Math.round(y), Math.round(w), Math.round(h));
        }
        public void text(String s, float x, float y, float size, int align, int argb) {
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.round(size)));
            int tw = g.getFontMetrics().stringWidth(s);
            float tx = align == ALIGN_CENTER ? x - tw / 2f : align == ALIGN_RIGHT ? x - tw : x;
            g.setColor(new Color(0, 0, 0, 160));
            g.drawString(s, tx + size * 0.06f, y + size * 0.06f);
            g.setColor(new Color(argb, true));
            g.drawString(s, tx, y);
        }
    }

    public static void main(String[] a) throws Exception {
        String res = a[0], out = a[1];
        if (a.length > 2) FOE = Float.parseFloat(a[2]);
        for (int i = 0; i < Img.COUNT; i++) imgs[i] = ImageIO.read(new File(res, Img.NAMES[i] + ".png"));
        int W = 2400, H = 1080;
        Game g = new Game();
        Renderer r = new Renderer();
        AwtPainter p = new AwtPainter();
        g.viewW = W / (H / 12f);
        boolean jumping = false, heldLast = false;
        int shots = 0, lastShotX = -100;
        float dt = 1f / 60f;
        for (int f = 0; f < 60 * 240; f++) {
            Body pl = g.player;
            Level lv = g.level;
            boolean jump = false;
            if (g.state == Game.PLAYING) {
                int feet = (int) Math.floor(pl.y + pl.h + 0.01f);
                float front = pl.x + pl.w;
                boolean wall = false;
                for (float d = 0.2f; d <= 1.2f && !wall; d += 0.5f) {
                    for (int ty = feet - 1; ty >= feet - 2; ty--) if (lv.solid((int) Math.floor(front + d), ty)) wall = true;
                }
                boolean pit = pl.onGround && !lv.solid((int) Math.floor(front + 0.35f), feet)
                        && !lv.solid((int) Math.floor(front + 0.35f), feet + 1);
                boolean foe = false;
                for (Enemy e : g.enemies) {
                    if (!e.dead && e.flatTime <= 0 && !e.flung && e.x > pl.x && e.x - front < FOE
                            && Math.abs(e.y + e.h - (pl.y + pl.h)) < 1.5f) foe = true;
                }
                if (pl.onGround) jumping = wall || pit || foe;
                jump = jumping && (pl.onGround || pl.vy < 0 || wall);
                if (pl.onGround && heldLast) jump = false;      // rilascia un attimo per poter risaltare
            }
            heldLast = jump;
            g.setInput(false, g.state == Game.PLAYING, jump);
            int livesBefore = g.lives;
            g.update(dt);
            if (g.lives < livesBefore) System.out.printf("morte a x=%.1f y=%.1f (t=%.1fs)%n", pl.x, pl.y, f * dt);
            int px = (int) pl.x;
            boolean shot = (px >= lastShotX + 40 && g.state == Game.PLAYING) || (g.state == Game.WON && g.stateTime > 1.6f && shots < 99);
            if (f == 30 || shot) {
                BufferedImage b = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
                p.g = b.createGraphics();
                p.g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                p.g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                r.draw(g, p, W, H);
                p.g.dispose();
                ImageIO.write(b, "png", new File(out, "shot" + shots + ".png"));
                shots++;
                lastShotX = px;
                if (g.state == Game.WON) shots = 99;
            }
            if (g.state == Game.WON && g.stateTime > 2f) {
                System.out.printf("LIVELLO COMPLETATO in %.1fs, punti=%d monete=%d vite=%d%n", f * dt, g.score, g.coinCount, g.lives);
                return;
            }
            if (g.state == Game.GAME_OVER) { System.out.println("GAME OVER (bot)"); return; }
        }
        System.out.println("tempo scaduto, x=" + g.player.x);
    }
}
