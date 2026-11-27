package com.valeriodipaolo.platformgame.core3d;

/**
 * Dove si nasconde il gettone d'oro in ogni livello 3D. Mai in bella vista:
 * <ul>
 * <li>in una nicchia sul bordo del sentiero, chiusa da tre blocchi di pietra alti: da lontano il blocco davanti la copre, e per
 * entrare e uscire serve il doppio salto;</li>
 * <li>in alto sopra una collina o un blocco, dove si arriva solo con il doppio salto dalla cima;</li>
 * <li>addosso a un nemico preciso, che ogni tanto luccica: va eliminato;</li>
 * <li>dentro una cassa qualunque, oppure (isola) dentro una cassa a molla, che di solito non si rompe e non dà nulla: la giravolta
 * la spacca.</li>
 * </ul>
 */
final class Coins3D {
    private Coins3D() {}

    private static final int W = World3D.W;

    static void place(World3D w) {
        switch (w.levelNo) {
            case 2: nook(w, 4, 56); break;
            case 4: w.coinFoeIdx = 2; break;              // il primo corvo
            case 6: crate(w, 134); break;
            case 8: sky(w, 0, 116, 4.0f); break;          // sopra il muro di pietra, con il fuoco alle spalle
            case 10: nook(w, 0, 180); break;
            case 12: w.coinFoeIdx = 3; break;             // il falco sopra le colonne
            case 14: crate(w, 240); break;
            case 16: sky(w, 4, 97, 4.0f); break;          // sopra la collina
            case 18: w.coinFoeIdx = 22; break;            // il pupazzo sulla collina
            case 20: nook(w, 4, 160); break;
            case 22: crate(w, 136); break;
            case 24: w.coinFoeIdx = 6; break;             // il falco tra le colonne
            case 26: nook(w, 0, 185); break;
            case 28: w.coin = new float[] {0f, 6.1f, 139.5f}; break;   // altissimo dopo la terza molla: ci arriva solo chi plana in giravolta
            case 30: w.coinFoeIdx = 5; break;             // una pianta carnivora
            case 32: crate(w, 115); break;
            case 34: sky(w, 0, 85, 4.0f); break;          // sopra l'altopiano più alto
            case 35: bounceCrate(w, 212); break;      // la cassa a molla sul bordo, verso la fine
            case 36: w.coinFoeIdx = 2; break;             // il totem nel cortile
            case 37: w.coinFoeIdx = 15; break;            // il totem dopo l'ultimo fiume
            case 38: w.coinFoeIdx = 1; break;             // il primo avvoltoio
            case 39: w.coinFoeIdx = 1; break;             // il primo drone
            // 40-43 (in carrello): il gettone lo mette il percorso stesso, sopra il binario delle gemme di una fossa
            default: break;
        }
    }

    /** Nicchia sul bordo (corsia 0 o 4) alla riga z: blocchi alti tre davanti, dietro e verso il sentiero; il gettone dentro, a terra. */
    static void nook(World3D w, int lane, int z) {
        int in = lane == 0 ? 1 : W - 2;
        int[][] cells = {{lane, z - 1}, {lane, z}, {lane, z + 1}, {in, z}};
        for (int[] c : cells) {
            int k = c[1] * W + c[0];
            if (w.height[k] != 0 || w.crate[k]) throw new IllegalStateException("livello " + w.levelNo + ": nicchia su terreno non libero in " + c[0] + "," + c[1]);
        }
        w.height[(z - 1) * W + lane] = 3;
        w.height[(z + 1) * W + lane] = 3;
        w.height[z * W + in] = 3;
        w.enemies.removeIf(e -> Math.abs(e.z - z - 0.5f) < 1.6f && World3D.xi(e.x) == lane);
        w.gemsList.removeIf(g -> Math.abs(g[2] - z - 0.5f) < 1.6f && (World3D.xi(g[0]) == lane || World3D.xi(g[0]) == in));
        w.coin = new float[] {lane - World3D.HALF + 0.5f, 0.6f, z + 0.5f};
    }

    /** Sospeso dy sopra la cima della colonna (corsia, z). */
    static void sky(World3D w, int lane, int z, float dy) {
        int h = Math.max(0, w.height[z * W + lane]);
        w.coin = new float[] {lane - World3D.HALF + 0.5f, h + dy, z + 0.5f};
    }

    /** La cassa più vicina alla riga z contiene il gettone al posto della gemma. */
    static void crate(World3D w, int z) {
        w.coinCrate = nearest(w, z, false);
    }

    /** La cassa a molla più vicina alla riga z: la giravolta la rompe e dentro c'è il gettone. */
    static void bounceCrate(World3D w, int z) {
        w.coinCrate = nearest(w, z, true);
    }

    private static int nearest(World3D w, int z, boolean bounce) {
        int best = -1;
        float bd = 1e9f;
        for (int k = 0; k < w.crate.length; k++) {
            if (!w.crate[k]) continue;
            boolean b = w.crateKind != null && w.crateKind[k] == World3D.C_BOUNCE;
            boolean plain = w.crateKind == null || w.crateKind[k] == World3D.C_WOOD;
            if (bounce ? !b : !plain) continue;
            float d = Math.abs(k / W - z) + (k % W == 2 ? 0.5f : 0);      // a parità, meglio una cassa sul bordo
            if (d < bd) { bd = d; best = k; }
        }
        if (best < 0) throw new IllegalStateException("livello " + w.levelNo + ": nessuna cassa vicino a " + z);
        return best;
    }
}
