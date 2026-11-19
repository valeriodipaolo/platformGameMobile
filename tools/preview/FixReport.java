import com.valeriodipaolo.platformgame.core.MemorySave;
import com.valeriodipaolo.platformgame.core3d.*;

/** Strumento da PC: riepilogo delle rifiniture applicate a ciascun livello 3D. */
public class FixReport {
    public static void main(String[] a) throws Exception {
        for (int n : new int[] {2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30, 32, 34}) {
            World3D w = new World3D(new MemorySave(), n);
            java.lang.reflect.Field f = Class.forName("com.valeriodipaolo.platformgame.core3d.LevelFix").getDeclaredField("last");
            f.setAccessible(true);
            System.out.println("L" + n + ": " + f.get(null) + "  (gemme " + w.gemsList.size() + ", nemici " + w.enemies.size() + ")");
        }
    }
}
