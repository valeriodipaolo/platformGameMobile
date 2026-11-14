import com.valeriodipaolo.platformgame.core.*;
import com.valeriodipaolo.platformgame.core3d.*;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Strumento da PC: primi piani del protagonista da quattro angolazioni, fermo, in corsa, in caduta e nella giravolta. */
public class HeroShot {
    public static void main(String[] a) throws Exception {
        String res = a[0], out = a[1];
        for (int i = 0; i < Img.COUNT; i++) Preview.imgs[i] = ImageIO.read(new File(res, Img.NAMES[i] + ".png"));
        int cw = 520, ch = 560;
        float[] yaws = {0f, 0.75f, 1.57f, 2.6f};            // di spalle, tre quarti dietro, profilo, tre quarti davanti
        BufferedImage sheet = new BufferedImage(cw * 4, ch * 4, BufferedImage.TYPE_INT_RGB);
        MeshBuilder gb = new MeshBuilder().color(150, 170, 120);
        gb.quad(-3, 0, -3, 3, 0, -3, 3, 0, 3, -3, 0, 3, 0, 1, 0, 0, 0, 1, 1);
        Mesh ground = gb.build(-1, false, true);
        for (int st = 0; st < 4; st++) {
            for (int v = 0; v < 4; v++) {
                World3D w = new World3D(new MemorySave());
                HeroModel hm = new HeroModel();
                w.time = 3.1f + v * 0.13f;
                w.onGround = st < 2;
                if (st == 1) { w.vz = 5f; w.runPhase = 1.2f; }
                if (st == 2) { w.vy = -6f; w.vz = 2f; }
                if (st == 3) { w.setInput(0, 0, false, true); w.update(1f / 60f); w.time = 3.1f; }
                Soft3D g = new Soft3D(cw, ch, Preview.imgs);
                float[] view = new float[16], proj = new float[16], m = Mat4.identity();
                Mat4.lookAt(view, 0, 1.35f, -3.0f, 0, 0.72f, 0);
                Mat4.perspective(proj, 36f, cw / (float) ch, 0.2f, 50f);
                g.begin(view, proj, 0.62f, 0.74f, 0.9f, 30f, 60f, 0.5f);
                g.draw(ground, Mat4.identity(), 1, 1, 1, 1, 0);
                Mat4.translate(m, 0, st == 2 ? 0.25f : 0, 0);
                Mat4.rotateY(m, yaws[v]);
                hm.draw(g, m, w);
                g.end();
                sheet.getGraphics().drawImage(g.image(), v * cw, st * ch, null);
            }
        }
        ImageIO.write(sheet, "png", new File(out, "hero.png"));
    }
}
