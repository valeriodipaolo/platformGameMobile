import com.valeriodipaolo.platformgame.core.*;
import com.valeriodipaolo.platformgame.core3d.*;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Strumento da PC: andatura e salto del protagonista di profilo (otto fasi della corsa, poi salita, apice, caduta, atterraggio). */
public class HeroGait {
    public static void main(String[] a) throws Exception {
        String res = a[0], out = a[1];
        HeroModel.style = a.length < 3 ? 2 : Integer.parseInt(a[2]);
        for (int i = 0; i < Img.COUNT; i++) Preview.imgs[i] = ImageIO.read(new File(res, Img.NAMES[i] + ".png"));
        int cw = 300, ch = 360, n = 8;
        BufferedImage sheet = new BufferedImage(cw * n, ch * 2, BufferedImage.TYPE_INT_RGB);
        MeshBuilder gb = new MeshBuilder().color(150, 170, 120);
        gb.quad(-3, 0, -3, 3, 0, -3, 3, 0, 3, -3, 0, 3, 0, 1, 0, 0, 0, 1, 1);
        Mesh ground = gb.build(-1, false, true);
        HeroModel hm = new HeroModel();
        World3D w = new World3D(new MemorySave());
        float t = 3f;
        for (int row = 0; row < 2; row++) {
            for (int v = 0; v < n; v++) {
                t += 0.04f;
                w.time = t;
                float py = 0;
                if (row == 0) { w.onGround = true; w.vz = 5.5f; w.vy = 0; w.runPhase = v * (float) Math.PI * 2f / n; }
                else {
                    float[] vy = {0, 9, 6, 1, -2, -6, -9, 0};
                    float[] h = {0, 0.5f, 1.2f, 1.6f, 1.5f, 0.9f, 0.2f, 0};
                    w.onGround = v == 0 || v == 7; w.vy = vy[v]; w.vz = 3f; py = h[v];
                }
                Soft3D g = new Soft3D(cw, ch, Preview.imgs);
                float[] view = new float[16], proj = new float[16], m = Mat4.identity();
                Mat4.lookAt(view, 3.4f, 1.0f, 0f, 0, 0.8f, 0);
                Mat4.perspective(proj, 36f, cw / (float) ch, 0.2f, 50f);
                g.begin(view, proj, 0.62f, 0.74f, 0.9f, 30f, 60f, 0.5f);
                g.draw(ground, Mat4.identity(), 1, 1, 1, 1, 0);
                Mat4.translate(m, 0, py, 0);
                Mat4.rotateY(m, 0f);
                hm.draw(g, m, w);
                g.end();
                sheet.getGraphics().drawImage(g.image(), v * cw, row * ch, null);
            }
        }
        ImageIO.write(sheet, "png", new File(out, "gait_" + HeroModel.style + ".png"));
    }
}
