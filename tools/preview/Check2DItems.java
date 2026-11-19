import com.valeriodipaolo.platformgame.core.*;
import java.util.*;

/** Strumento da PC: gemme e nemici 2D fuori dall'area, dentro il terreno o sopra burroni troppo larghi. */
public class Check2DItems {
    static boolean pitCol(Level lv, int x) {
        if (x < 0 || x >= lv.w) return false;
        for (int y = 0; y < lv.h; y++) if (lv.solid(x, y)) return false;
        return true;
    }
    public static void main(String[] a) {
        int[] ids = {1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 25, 27, 29, 31, 33};
        for (int id : ids) {
            Game g = new Game(new MemorySave(), id);
            Level lv = g.level;
            int out = 0, inside = 0, wide = 0;
            for (float[] s : lv.shards) {
                int x = (int) Math.floor(s[0]), y = (int) Math.floor(s[1]);
                if (x < 0 || x >= lv.w || y < -2 || y >= lv.h) { out++; continue; }
                if (lv.solid(x, y)) { inside++; System.out.println("   dentro il terreno: " + s[0] + "," + s[1]); }
                if (pitCol(lv, x)) {
                    int l = x, r = x;
                    while (pitCol(lv, l - 1)) l--;
                    while (pitCol(lv, r + 1)) r++;
                    if (r - l + 1 > 7) { wide++; System.out.println("   su burrone largo " + (r - l + 1) + ": " + s[0] + "," + s[1]); }
                }
            }
            int eout = 0;
            for (float[] e : lv.enemies) {
                int x = (int) Math.floor(e[0]), y = (int) Math.floor(e[1]);
                if (x < 0 || x >= lv.w || y < 0 || y >= lv.h || lv.solid(x, y)) { eout++; System.out.println("   nemico fuori: t" + e[2] + " " + e[0] + "," + e[1]); }
            }
            System.out.println("livello " + id + ": gemme " + lv.shards.size() + " fuori " + out + " nel terreno " + inside + " su burroni larghi " + wide + "; nemici " + lv.enemies.size() + " fuori " + eout);
        }
    }
}
