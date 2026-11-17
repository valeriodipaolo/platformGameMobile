import com.valeriodipaolo.platformgame.core.*;
import com.valeriodipaolo.platformgame.core3d.*;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Strumento da PC: ricava gli sprite 2D del protagonista dal modello 3D (stessa grafica nei due modi di gioco): fermo, quattro passi di corsa,
 * salto e le dodici pose della giravolta. Li disegna in grande (4x); tools/fit_hero.py li riduce alla misura del gioco (112x144).
 * Uso: HeroSprites res outDir
 */
public class HeroSprites {
    static final int W = 448, H = 576;

    public static void main(String[] a) throws Exception {
        String res = a[0], out = a[1];
        for (int i = 0; i < Img.COUNT; i++) Preview.imgs[i] = ImageIO.read(new File(res, Img.NAMES[i] + ".png"));
        String[] names = new String[18];
        names[0] = "hero_idle"; names[5] = "hero_jump";
        for (int i = 0; i < 4; i++) names[1 + i] = "hero_run_" + i;
        for (int i = 0; i < 12; i++) names[6 + i] = "hero_spin_" + i;
        for (int n = 0; n < 18; n++) {
            World3D w = new World3D(new MemorySave());
            HeroModel hm = new HeroModel();
            w.time = 3.1f + n * 0.07f;
            w.onGround = true;
            float rot = (float) Math.PI * 0.5f;                       // profilo, rivolto a destra
            if (n >= 1 && n <= 4) { w.vz = 5f; w.runPhase = (n - 1) * (float) Math.PI * 0.5f / HeroModel.phaseScale(HeroModel.style); }      // quattro fasi uguali del ciclo
            if (n == 5) { w.onGround = false; w.vy = 3f; w.vz = 2.5f; }
            if (n >= 6) {
                w.setInput(0, 0, false, true); w.update(1f / 60f); w.time = 3.1f;
                rot += (n - 6) * (float) Math.PI / 6f;
            }
            Soft3D g = new Soft3D(W, H, Preview.imgs);
            float[] view = new float[16], proj = new float[16], m = Mat4.identity();
            float dist = 14f, ext = 1.5f;
            Mat4.lookAt(view, 0, 0.9f, -dist, 0, 0.73f, 0);
            Mat4.perspective(proj, 2f * (float) Math.toDegrees(Math.atan(ext / 2f / dist)), W / (float) H, 0.2f, 50f);
            g.begin(view, proj, 0f, 0f, 0f, 1000f, 2000f, 0f);
            Mat4.rotateY(m, rot);
            hm.draw(g, m, w);
            g.end();
            BufferedImage col = g.image();
            BufferedImage o = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < H; y++) for (int x = 0; x < W; x++)
                o.setRGB(x, y, g.covered(x, y) ? (0xFF000000 | (col.getRGB(x, y) & 0xFFFFFF)) : 0);
            ImageIO.write(o, "png", new File(out, names[n] + ".png"));
        }
    }
}
