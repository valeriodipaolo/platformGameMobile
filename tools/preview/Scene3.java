import com.valeriodipaolo.platformgame.core.*;
import com.valeriodipaolo.platformgame.core3d.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/** Strumento da PC: salva schermate del livello 3D in vari punti del corridoio. */
public class Scene3 {
    static final int W = 1600, H = 720;

    static void shot(World3D w, Scene3D sc, String file, String out, boolean joy) throws Exception {
        Soft3D g = new Soft3D(W, H, Preview.imgs);
        sc.render(w, g, W / (float) H);
        BufferedImage b = g.image();
        Preview.AwtPainter p = new Preview.AwtPainter();
        p.g = b.createGraphics();
        p.g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        p.g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        new Hud3D().draw(w, p, W, H, false, joy, 300, 520, 0.2f, -0.9f, null);
        p.g.dispose();
        ImageIO.write(b, "png", new File(out, file));
    }

    static void run(World3D w, float sec, float jx, float jy, boolean j, boolean s) {
        w.setInput(jx, jy, j, s);
        for (float t = 0; t < sec; t += 1f / 60f) w.update(1f / 60f);
    }

    static void warp(World3D w, float x, float z) {
        w.px = x; w.pz = z; w.py = Math.max(0, w.topAt(x, z)) + 0.01f; w.vy = 0;
    }

    public static void main(String[] a) throws Exception {
        String res = a[0], out = a[1];
        for (int i = 0; i < Img.COUNT; i++) Preview.imgs[i] = ImageIO.read(new File(res, Img.NAMES[i] + ".png"));
        World3D w = new World3D(new MemorySave());
        Scene3D sc = new Scene3D();
        run(w, 0.3f, 0, 0, false, false);
        run(w, 0.25f, 0.3f, 1, false, false);
        shot(w, sc, "t_start.png", out, true);

        warp(w, -0.5f, 12.2f); sc = new Scene3D();
        run(w, 0.2f, 0, 1, false, false);
        run(w, 0.3f, 0, 1, true, false);
        shot(w, sc, "t_jump.png", out, true);

        w = new World3D(new MemorySave()); sc = new Scene3D();
        warp(w, 0.6f, 29.6f);
        run(w, 0.25f, 0, 0, false, false);
        run(w, 0.2f, 0, 0.6f, false, true);
        shot(w, sc, "t_spin.png", out, true);

        w = new World3D(new MemorySave()); sc = new Scene3D();
        warp(w, 0, 49);
        run(w, 0.3f, 0, 1, false, false);
        shot(w, sc, "t_battery.png", out, true);

        w = new World3D(new MemorySave()); sc = new Scene3D();
        warp(w, 0, 64);
        run(w, 0.3f, 0, 1, false, false);
        shot(w, sc, "t_terrace.png", out, false);

        w = new World3D(new MemorySave()); sc = new Scene3D();
        warp(w, -1.5f, 89.5f);
        run(w, 0.3f, 0, 1, false, false);
        shot(w, sc, "t_zigzag.png", out, true);

        w = new World3D(new MemorySave()); sc = new Scene3D();
        warp(w, 0, 140);
        run(w, 0.3f, 0, 1, false, false);
        shot(w, sc, "t_flag.png", out, true);
    }
}
