import com.valeriodipaolo.platformgame.core.*;
import com.valeriodipaolo.platformgame.core3d.*;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Strumento da PC: primi piani dei quattro nemici 3D, da due angolazioni e in due momenti dell'animazione. */
public class FoeShot {
    public static void main(String[] a) throws Exception {
        String res = a[0], out = a[1];
        for (int i = 0; i < Img.COUNT; i++) Preview.imgs[i] = ImageIO.read(new File(res, Img.NAMES[i] + ".png"));
        int cw = 560, ch = 460;
        FoeModels fm = new FoeModels();
        int[] types = {Enemy3.BEETLE, Enemy3.HOG, Enemy3.CROW, Enemy3.ACORN};
        float[] yaws = {2.5f, 3.9f, 1.57f, 0.6f};       // tre quarti davanti (due lati), profilo, tre quarti dietro
        BufferedImage sheet = new BufferedImage(cw * 4, ch * 4, BufferedImage.TYPE_INT_RGB);
        MeshBuilder gb = new MeshBuilder().color(150, 170, 120);
        gb.quad(-3, 0, -3, 3, 0, -3, 3, 0, 3, -3, 0, 3, 0, 1, 0, 0, 0, 1, 1);
        Mesh ground = gb.build(-1, false, true);
        for (int t = 0; t < 4; t++) {
            for (int v = 0; v < 4; v++) {
                Soft3D g = new Soft3D(cw, ch, Preview.imgs);
                float[] view = new float[16], proj = new float[16], m = Mat4.identity();
                float cy = types[t] == Enemy3.CROW ? 0.05f : 0.38f;
                Mat4.lookAt(view, 0, cy + 0.75f, -2.1f, 0, cy, 0);
                Mat4.perspective(proj, 36f, cw / (float) ch, 0.2f, 50f);
                g.begin(view, proj, 0.62f, 0.74f, 0.9f, 30f, 60f, 0.5f);
                if (types[t] != Enemy3.CROW) g.draw(ground, Mat4.identity(), 1, 1, 1, 1, 0);
                Enemy3 e = new Enemy3(0, 0, true, 0, 1);
                e.type = types[t];
                e.anim = 0.13f + v * 0.37f;
                if (types[t] == Enemy3.ACORN && v == 3) e.y = 0.5f;
                Mat4.translate(m, 0, e.y, 0);
                Mat4.rotateY(m, yaws[v]);
                fm.draw(g, m, e);
                sheet.getGraphics().drawImage(g.image(), v * cw, t * ch, null);
            }
        }
        ImageIO.write(sheet, "png", new File(out, "foes.png"));
    }
}
