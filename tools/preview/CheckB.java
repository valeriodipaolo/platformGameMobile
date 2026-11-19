import com.valeriodipaolo.platformgame.core.*;
import com.valeriodipaolo.platformgame.core3d.*;
public class CheckB {
    static int fails;
    static void ok(boolean c, String m) { System.out.println((c ? "OK   " : "FALLITO ") + m); if (!c) fails++; }
    public static void main(String[] a) {
        for (int id : (a.length > 0 ? new int[]{Integer.parseInt(a[0])} : new int[]{13,15,17,19,21,23,25,27,29,31,33})) {
            Game g = new Game(new MemorySave(), id);
            Level lv = g.level;
            int bad=0;
            for (float[] c : lv.shards) if (lv.solid((int) c[0], (int) c[1])) { bad++; System.out.println("  gemma nel terreno "+c[0]+","+c[1]); }
            for (float[] e : lv.enemies) if (e[2] < Enemy.CROW || e[2]==Enemy.MOLE||e[2]==Enemy.SNAIL||e[2]==Enemy.PENGUIN||e[2]==Enemy.SNOWMAN||e[2]==Enemy.SALAMANDER||e[2]==Enemy.FROG||e[2]==Enemy.PLANT) if (lv.solid((int) e[0], (int) e[1])) { bad++; System.out.println("  nemico nel terreno "+e[0]+","+e[1]+" t"+e[2]); }
            int tight=0;
            for (int y=0;y<lv.h;y++) for(int x=0;x<lv.w;x++){ char c=lv.get(x,y); if(c!=Level.BRICK&&c!=Level.BONUS&&c!=Level.SECRET)continue;
              if (lv.solid(x,y+1)||lv.solid(x,y+2)){ tight++; System.out.println("  blocco ostruito "+x+","+y);} }
            for (int x=0;x<lv.w;x++) for(int y=0;y<lv.h-2;y++){ if(lv.get(x,y)!=Level.STONE||lv.solid(x,y+1)) continue; int run=0; while(y+1+run<lv.h&&!lv.solid(x,y+1+run)) run++; if(run==1&&y+2<lv.h&&lv.solid(x,y+2)){ tight++; System.out.println("  fessura stretta "+x+","+y);} }
            float[] r = CheckSpring.bot2d(g, 600, 1e9f);
            ok(g.state == Game.WON && (int) r[0] <= 2 && bad == 0 && tight == 0, "livello 2D " + id);
            System.out.println("livello "+id+": w "+lv.w+" gemme "+lv.shards.size()+" nemici "+lv.enemies.size()+" morti "+(int)r[0]+" x "+r[1]+" stato "+g.state+" t "+r[2]+" brutti "+bad+" tight "+tight);
        }
        System.out.println(fails == 0 ? "TUTTO OK" : fails + " FALLITI");
    }
}
