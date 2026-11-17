import com.valeriodipaolo.platformgame.core.MemorySave;
import com.valeriodipaolo.platformgame.core3d.*;

/** Strumento da PC: stampa la pianta di un livello 3D (altezze per riga, P = vuoto, c = cassa, g = gemma, e = nemico). */
public class DumpMap {
    public static void main(String[] a) {
        int[] lv = a.length > 0 ? new int[] {Integer.parseInt(a[0])} : new int[] {2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24};
        for (int n : lv) {
            World3D w = new World3D(new MemorySave(), n);
            System.out.println("=== livello " + n + " lunghezza " + w.length + " gemme " + w.gemsList.size() + " nemici " + w.enemies.size());
            for (int z = 0; z < w.length; z++) {
                StringBuilder sb = new StringBuilder();
                for (int x = 0; x < World3D.W; x++) {
                    int h = w.cell(x, z);
                    sb.append(h == World3D.PIT ? 'P' : w.crate[z * World3D.W + x] ? 'c' : (char) ('0' + h));
                }
                int g = 0; for (float[] q : w.gemsList) if ((int) Math.floor(q[2]) == z) g++;
                int e = 0; for (Enemy3 q : w.enemies) if ((int) Math.floor(q.z) == z) e++;
                System.out.println(String.format("%3d %s %s%s", z, sb, g > 0 ? "g" + g + " " : "", e > 0 ? "e" + e : ""));
            }
        }
    }
}
