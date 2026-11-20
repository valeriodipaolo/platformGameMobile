import com.valeriodipaolo.platformgame.core.*;
import com.valeriodipaolo.platformgame.core3d.*;

/** Controlli automatici sulle regole dell'isola tropicale: casse speciali, zattere, pestelli, masso, camera girata. */
public class CheckIsland {
    static int fails;
    static void ok(boolean c, String m) { System.out.println((c ? "OK   " : "FALLITO ") + m); if (!c) fails++; }

    static void run(World3D w, float sec, float jx, float jy, boolean j) {
        for (float t = 0; t < sec; t += 1f / 60f) { w.setInput(jx, jy, j, false); w.update(1f / 60f); }
    }

    static World3D at(int id, float x, float z) {
        World3D w = new World3D(new MemorySave(), id);
        w.enemies.clear();
        w.px = x; w.pz = z; w.py = Math.max(0, w.topAt(x, z)) + 0.01f;
        run(w, 0.3f, 0, 0, false);
        return w;
    }

    static int k(int col, int z) { return z * World3D.W + col; }

    public static void main(String[] a) {
        // cassa a molla davanti al muro (35, z 33): ci si sale e si vola oltre il muro alto tre blocchi
        World3D w = at(35, 0, 31.6f);
        float top = 0;
        for (int i = 0; i < 150; i++) { w.setInput(0, i < 10 ? 0.4f : 0.6f, i < 8, false); w.update(1f / 60f); top = Math.max(top, w.py); }
        ok(top > 3.6f && w.pz > 37.5f && w.state == World3D.PLAYING, "la cassa a molla lancia oltre il muro: quota massima " + top + ", z " + w.pz);
        ok(w.crate[k(2, 33)], "la cassa a molla non si rompe");

        // TNT (35, z 46): ci si salta sopra, si accende, dopo tre secondi scoppia e rompe le casse attorno
        w = at(35, -2f, 43f);
        int gems0 = w.gems;
        w.px = 0; w.pz = 46.5f; w.py = 1.6f; w.vy = 0;
        for (int i = 0; i < 20; i++) { w.setInput(0, 0, false, false); w.update(1f / 60f); }
        ok(w.crateT[k(2, 46)] > 0, "saltandoci sopra la TNT si accende");
        run(w, 0.6f, 0, 1, false);
        run(w, 3.2f, 0, 0, false);
        ok(!w.crate[k(2, 46)] && !w.crate[k(1, 46)] && !w.crate[k(3, 46)], "la TNT scoppia e rompe le casse vicine");
        ok(w.gems >= gems0 + 4, "le casse rotte dall'esplosione contano come gemme: " + (w.gems - gems0));

        // instabile (35, z 86): basta sfiorarla
        w = at(35, 1.0f, 84.6f);
        int hp0 = w.hp;
        run(w, 0.5f, 0, 1, false);
        ok(!w.crate[k(3, 86)] && w.hp < hp0, "l'instabile scoppia al tocco e ferisce");

        // cassa di ripartenza (35, z 75): rompendola si riparte da lì
        w = at(35, 0, 73.5f);
        for (int i = 0; i < 50; i++) { w.setInput(0, 0.45f, i < 8, false); w.update(1f / 60f); }
        ok(!w.crate[k(2, 75)], "la cassa di ripartenza si rompe saltandoci sopra");
        w.py = -7; run(w, 2.0f, 0, 0, false);
        ok(Math.abs(w.pz - 75.5f) < 0.6f, "dopo una caduta si riparte dalla cassa blu: z " + w.pz);

        // zattera (37, z 16-17): regge il robot per poco, poi affonda
        w = at(37, 0, 16.5f);
        float y0 = w.py;
        run(w, 0.4f, 0, 0, false);
        ok(w.onGround && Math.abs(w.py - y0) < 0.05f, "la zattera regge appena saliti");
        run(w, 1.5f, 0, 0, false);
        ok(w.state != World3D.PLAYING || w.py < -1f, "fermi sulla zattera si affonda");

        // pestello (36, z 42): chi resta sotto viene colpito
        w = at(36, 0, 40.0f);
        w.fireZ = Float.NaN;
        hp0 = w.hp;
        w.px = 0; w.pz = 42.5f; w.py = 0;
        run(w, 2.6f, 0, 0, false);
        ok(w.hp < hp0, "il pestello schiaccia chi sta sotto");

        // masso (36): se ci si ferma, raggiunge il robot; la camera guarda da davanti fino a fine fuga
        w = new World3D(new MemorySave(), 36);
        w.enemies.clear();
        int lives0 = w.lives;
        run(w, 6f, 0, 0, false);
        ok(w.lives < lives0, "fermi, il masso travolge il robot");
        ok(w.camBackUntil > 200 && w.chase == 2, "nella fuga dal masso la camera è girata");

        // granchi, totem, piranha: aspetto nuovo con un comportamento noto
        World3D r = new World3D(new MemorySave(), 37);
        int crabs = 0, tikis = 0, fish = 0;
        for (Enemy3 e : r.enemies) { if (e.skin == Enemy3.SKIN_CRAB) crabs++; if (e.skin == Enemy3.SKIN_TIKI) tikis++; if (e.skin == Enemy3.SKIN_FISH) fish++; }
        ok(crabs > 0 && tikis > 0 && fish > 0, "sul fiume ci sono granchi " + crabs + ", totem " + tikis + ", piranha " + fish);
        // sabbie mobili (38, z 30-32 su tutta la larghezza): rallentano e, fermi, si sprofonda
        w = at(38, 0, 30.5f);
        float z0 = w.pz;
        run(w, 0.5f, 0, 1, false);
        ok(w.pz - z0 < 2.2f, "nelle sabbie mobili si arranca: " + (w.pz - z0) + " caselle in mezzo secondo");
        w = at(38, 0, 31.5f); lives0 = w.lives;
        run(w, 3.6f, 0, 0, false);
        ok(w.lives < lives0, "fermi nelle sabbie mobili si sprofonda");
        // vento (38, z 40-60): la raffica sposta di lato
        w = at(38, -1f, 50f);
        float maxPush = 0;
        for (int i = 0; i < 240; i++) { w.setInput(0, 0, false, false); w.update(1f / 60f); maxPush = Math.max(maxPush, w.windPush(w.pz, w.time)); }
        ok(maxPush > 3f, "la raffica spinge di lato: " + maxPush);
        // nastro (39, z 30-44 verso destra): fermi, si viene portati verso il burrone
        w = at(39, -2f, 36f);
        run(w, 0.8f, 0, 0, false);
        ok(w.px > -0.5f, "il nastro di traverso trascina verso destra: x " + w.px);
        // laser (39, z 50): acceso ferisce chi passa a terra
        w = at(39, 0, 48f); hp0 = w.hp;
        boolean hit = false;
        for (int i = 0; i < 300 && !hit; i++) { w.px = 0; w.pz = 50.5f; w.py = 0; w.vz = 0; w.update(1f / 60f); hit = w.hp < hp0; }
        ok(hit, "la barriera laser ferisce");
        // carrello (40, z 32-104): corre da solo anche senza toccare nulla
        w = at(40, 0, 33f); z0 = w.pz;
        run(w, 0.8f, 0, 0, false);
        ok(w.pz - z0 > 5f && w.inCart(), "il carrello corre da solo: " + (w.pz - z0) + " caselle in 0,8 s");
        System.out.println(fails == 0 ? "TUTTO OK" : fails + " FALLITI");
    }
}
