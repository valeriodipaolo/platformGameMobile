import com.valeriodipaolo.platformgame.core.*;
import com.valeriodipaolo.platformgame.core3d.*;

/** Portone di fine livello: si apre da solo, muro altissimo, uscita libera dopo l'ingresso, dissolvenza nel 3D. */
public class CheckGate {
    static int fails;
    static void ok(boolean c, String m) { System.out.println((c ? "OK   " : "FALLITO ") + m); if (!c) fails++; }

    static void gate2d(int id) {
        Game g = new Game(new MemorySave(), id);
        g.enemies.clear();
        g.fireX = Float.NaN;
        Level lv = g.level;
        ok(lv.get(lv.flagX, lv.flagBaseY - 1) == Level.DOOR && lv.get(lv.flagX, 1) == Level.STONE, "livello " + id + ": portone chiuso e muro altissimo");
        // da lontano il portone è chiuso; avvicinandosi si apre da solo (non servono chiavi)
        g.player.x = lv.flagX - 12; g.player.y = lv.flagBaseY - g.player.h; g.player.vy = 0;
        for (int i = 0; i < 10; i++) { g.setInput(false, false, false); g.update(1f / 60f); }
        ok(g.gateOpen == 0, "livello " + id + ": da lontano il portone è chiuso");
        g.player.x = lv.flagX - 3; g.player.y = lv.flagBaseY - g.player.h; g.player.vy = 0;
        boolean freeAtGate = false;
        for (int i = 0; i < 600 && g.state == Game.PLAYING; i++) { g.setInput(false, true, false); g.update(1f / 60f); if (g.player.x > lv.flagX + 0.5f && g.state == Game.PLAYING) freeAtGate = true; }
        ok(freeAtGate && g.state == Game.WON, "livello " + id + ": il portone si apre da solo, lo si attraversa e si vince oltre l'ingresso");
    }

    static void gate3d(int id) {
        World3D w = new World3D(new MemorySave(), id);
        w.enemies.clear(); w.fireZ = Float.NaN; w.fireStart = Float.NaN; w.hiveZ = Float.NaN; w.thorns.clear(); w.jets.clear(); w.logs.clear();
        ok(w.cell(2, w.flagZ) == 99 && w.cell(0, w.flagZ) == 99, "livello " + id + ": portone chiuso, muro invalicabile");
        w.pz = w.flagZ - 40; w.px = 0; w.py = Math.max(0, w.topAt(w.px, w.pz)) + 0.01f; w.invuln = 99;
        for (int i = 0; i < 10; i++) { w.setInput(0, 0, false, false); w.update(1f / 60f); }
        ok(w.gateOpen == 0 && w.fade() == 0, "livello " + id + ": molto lontano il portone è ancora chiuso");
        w.pz = w.flagZ - 20;
        while (w.topAt(w.px, w.pz) < 0 && w.pz < w.flagZ - 8) w.pz += 1f;          // un punto con il pavimento sotto
        w.py = Math.max(0, w.topAt(w.px, w.pz)) + 0.01f;
        for (int i = 0; i < 120; i++) { w.setInput(0, 0, false, false); w.update(1f / 60f); }
        ok(w.gateOpen > 0.6f && w.fade() == 0, "livello " + id + ": quando lo si vede arrivando, il portone è già aperto");
        w.pz = w.flagZ - 3; w.px = 0; w.py = Math.max(0, w.topAt(w.px, w.pz)) + 0.01f;
        float fadeIn = 0;
        for (int i = 0; i < 900 && w.state == World3D.PLAYING; i++) { w.setInput(0, 1, false, false); w.update(1f / 60f); fadeIn = Math.max(fadeIn, w.fade()); }
        ok(w.state == World3D.WON && w.pz > w.flagZ + 1, "livello " + id + ": il portone si apre da solo, lo si passa e si vince oltre");
        ok(fadeIn > 0.5f, "livello " + id + ": entrando nel portone lo schermo sfuma nel buio (" + fadeIn + ")");
        for (int i = 0; i < 60; i++) w.update(1f / 60f);
        ok(w.fade() >= 0.999f, "livello " + id + ": a livello finito il buio copre tutto");
    }

    public static void main(String[] a) {
        for (int id : new int[] {1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 25, 27, 29, 31, 33}) gate2d(id);
        for (int id : new int[] {2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30, 32, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46}) gate3d(id);
        System.out.println(fails == 0 ? "TUTTO OK" : fails + " FALLITI");
        if (fails > 0) System.exit(1);
    }
}
