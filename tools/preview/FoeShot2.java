import com.valeriodipaolo.platformgame.core.*;
import com.valeriodipaolo.platformgame.core3d.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Strumento da PC: primi piani dei 12 nemici della seconda serie (id 6..17) e dei 4 della prima (2..5), tre quarti davanti e profilo. */
public class FoeShot2 {
    public static void main(String[] a) throws Exception {
        String res = a[0], out = a[1];
        for (int i = 0; i < Img.COUNT; i++) Preview.imgs[i] = ImageIO.read(new File(res, Img.NAMES[i] + ".png"));
        int cw = 420, ch = 380, cols = 6;
        int[] ids = a.length > 2 ? java.util.Arrays.stream(a[2].split(",")).mapToInt(Integer::parseInt).toArray() : new int[] {6,7,8,9,10,11,12,13,14,15,16,17};
        BufferedImage sheet = new BufferedImage(cw * cols, ch * ((ids.length * 2 + cols - 1) / cols), BufferedImage.TYPE_INT_RGB);
        FoeModels fm = new FoeModels();
        MeshBuilder gb = new MeshBuilder().color(150, 170, 120);
        gb.quad(-3, 0, -3, 3, 0, -3, 3, 0, 3, -3, 0, 3, 0, 1, 0, 0, 0, 1, 1);
        Mesh ground = gb.build(-1, false, true);
        int k = 0;
        for (int id : ids) for (int v = 0; v < 2; v++, k++) {
            Soft3D g = new Soft3D(cw, ch, Preview.imgs);
            float[] view = new float[16], proj = new float[16], m = Mat4.identity();
            Mat4.lookAt(view, 0, 1.2f, -3.2f, 0, 0.55f, 0);
            Mat4.perspective(proj, 36f, cw / (float) ch, 0.2f, 50f);
            g.begin(view, proj, 0.62f, 0.74f, 0.9f, 30f, 60f, 0.5f);
            g.draw(ground, Mat4.identity(), 1, 1, 1, 1, 0);
            Enemy3 e = new Enemy3(0, 0, true, 0, 1);
            e.type = id; e.mode = 2; e.anim = 0.3f + v * 0.2f;
            if (id == Enemy3.OWL || id == Enemy3.STAR || id == Enemy3.SUNNY) e.y = 0.3f;
            Mat4.translate(m, 0, e.y, 0);
            Mat4.rotateY(m, v == 0 ? 2.6f : 1.57f);
            fm.draw(g, m, e);
            g.end();
            sheet.getGraphics().drawImage(g.image(), (k % cols) * cw, (k / cols) * ch, null);
        }
        ImageIO.write(sheet, "png", new File(out, "foes2.png"));
    }
}
