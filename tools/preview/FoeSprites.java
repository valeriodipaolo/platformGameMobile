import com.valeriodipaolo.platformgame.core.*;
import com.valeriodipaolo.platformgame.core3d.*;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Strumento da PC: ricava gli sprite 2D dei nemici dai modelli 3D (stessa grafica nei due modi di gioco). Ogni sprite si disegna in grande;
 * la trasparenza viene dalla profondita (e dalla luminosita per le parti sfumate). tools/fit_sprites.py li ritaglia e li adatta ai file del gioco.
 * Uso: FoeSprites res outDir
 */
public class FoeSprites {
    static final int S = 768;
    static final float R = 4.71239f, F = 3.14159f;

    // nome, tipo, modo, anim, rotazione, schiacciamento verticale, quota (salto)
    static Object[][] LIST = {
        {"enemy_0", Enemy3.BEETLE, 0, 0.30f, R, 1f, 0f}, {"enemy_1", Enemy3.BEETLE, 0, 0.62f, R, 1f, 0f}, {"enemy_flat", Enemy3.BEETLE, 0, 0.30f, R, 0.22f, 0f},
        {"hog_0", Enemy3.HOG, 0, 0.30f, R, 1f, 0f}, {"hog_1", Enemy3.HOG, 0, 0.62f, R, 1f, 0f},
        {"crow_0", Enemy3.CROW, 0, 0.20f, R, 1f, 0f}, {"crow_1", Enemy3.CROW, 0, 0.55f, R, 1f, 0f},
        {"acorn_0", Enemy3.ACORN, 0, 0.30f, R, 1f, 0f}, {"acorn_1", Enemy3.ACORN, 0, 0.62f, R, 1f, 0.6f}, {"acorn_flat", Enemy3.ACORN, 0, 0.30f, R, 0.22f, 0f},
        {"hv_storm", Enemy3.STORM, 0, 0.30f, F, 1f, 0f}, {"hv_storm_c", Enemy3.STORM, 1, 0.30f, F, 1f, 0f},
        {"hv_whirl", Enemy3.WHIRL, 0, 0.10f, F, 1f, 0f}, {"hv_whirl_b", Enemy3.WHIRL, 0, 0.35f, F, 1f, 0f},
        {"fx_snail", Enemy3.SNAIL, 0, 0.3f, R, 1f, 0f}, {"fx_shell", Enemy3.SNAIL, 1, 0.3f, R, 1f, 0f}, {"fx_slug", Enemy3.SNAIL, 2, 0.3f, R, 1f, 0f},
        {"fx_chestnut", Enemy3.CHESTNUT, 0, 0.3f, F, 1f, 0f},
        {"fx_owl_0", Enemy3.OWL, 0, 0.4f, F, 1f, 0f}, {"fx_owl_1", Enemy3.OWL, 0, 1.0f, F, 1f, 0f},
        {"fx_penguin", Enemy3.PENGUIN, 0, 0.25f, F, 1f, 0f},
        {"fx_snowman", Enemy3.SNOWMAN, 0, 0.3f, F, 1f, 0f},
        {"fx_lapillo", Enemy3.LAPILLO, 1, 0.3f, F, 1f, 0f},
        {"fx_sala", Enemy3.SALAMANDER, 0, 0.3f, R, 1f, 0f}, {"fx_sala_b", Enemy3.SALAMANDER, 1, 0.3f, R, 1f, 0f},
        {"fx_frog", Enemy3.FROG, 0, 0.3f, R, 1f, 0f}, {"fx_frog_j", Enemy3.FROG, 1, 0.3f, R, 1f, 0f},
        {"fx_plant", Enemy3.PLANT, 0, 0.3f, F, 1f, 0f}, {"fx_plant_o", Enemy3.PLANT, 2, 0.3f, F, 1f, 0f},
        {"fx_star", Enemy3.STAR, 0, 0.3f, F, 1f, 0f}, {"fx_sunny", Enemy3.SUNNY, 3, 0.3f, F, 1f, 0f},
        {"fx_snowball", -1, 0, 0f, F, 1f, 0f},
    };

    public static void main(String[] a) throws Exception {
        String res = a[0], out = a[1];
        String only = a.length > 2 ? "," + a[2] + "," : null;
        for (int i = 0; i < Img.COUNT; i++) Preview.imgs[i] = ImageIO.read(new File(res, Img.NAMES[i] + ".png"));
        FoeModels fm = new FoeModels();
        for (Object[] sp : LIST) {
            String name = (String) sp[0];
            if (only != null && !only.contains("," + name + ",")) continue;
            int type = (Integer) sp[1], mode = (Integer) sp[2];
            float anim = (Float) sp[3], rot = (Float) sp[4], sy = (Float) sp[5], lift = (Float) sp[6];
            Soft3D g = new Soft3D(S, S, Preview.imgs);
            float[] view = new float[16], proj = new float[16], m = Mat4.identity();
            float dist = 9f, ext = 3.6f;
            float camY = type == Enemy3.SNAIL ? 1.7f : type == Enemy3.OWL ? 3.6f : type == Enemy3.PLANT ? 1.8f : 0.9f;
            Mat4.lookAt(view, 0, camY, -dist, 0, 0.9f, 0);
            Mat4.perspective(proj, 2f * (float) Math.toDegrees(Math.atan(ext / 2f / dist)), 1f, 0.2f, 50f);
            g.begin(view, proj, 0f, 0f, 0f, 1000f, 2000f, 0f);
            Mat4.rotateY(m, rot);
            Mat4.scale(m, 1f, sy, 1f);
            if (type < 0) {
                fm.drawShot(g, Mat4.identity(), new float[] {0, 0.9f, 0});
            } else {
                Enemy3 e = new Enemy3(0, 0, true, 0, 1);
                e.type = type; e.mode = mode; e.anim = anim; e.dir = 1;
                e.baseY = 0; e.y = lift;
                if (type == Enemy3.STORM) { e.hopT = mode == 1 ? 2.6f : 0.5f; e.y = 0.8f; }
                if (type == Enemy3.OWL || type == Enemy3.STAR || type == Enemy3.SUNNY || type == Enemy3.CROW) Mat4.translate(m, 0, e.y > 0 ? 0 : 0.35f, 0);
                if (type == Enemy3.SUNNY || type == Enemy3.STAR || type == Enemy3.CROW) { }
                fm.draw(g, m, e);
            }
            g.end();
            BufferedImage col = g.image();
            BufferedImage o = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
                int p = col.getRGB(x, y);
                float r = ((p >> 16) & 255) / 255f, gg = ((p >> 8) & 255) / 255f, b = (p & 255) / 255f;
                float al = g.covered(x, y) ? 1f : Math.min(1f, Math.max(r, Math.max(gg, b)) * 1.2f);
                if (al < 0.02f) { o.setRGB(x, y, 0); continue; }
                float k = g.covered(x, y) ? 1f : al;
                int R8 = Math.min(255, (int) (r / k * 255 + .5f)), G8 = Math.min(255, (int) (gg / k * 255 + .5f)), B8 = Math.min(255, (int) (b / k * 255 + .5f));
                o.setRGB(x, y, ((int) (al * 255 + .5f) << 24) | (R8 << 16) | (G8 << 8) | B8);
            }
            ImageIO.write(o, "png", new File(out, name + ".png"));
        }
    }
}
