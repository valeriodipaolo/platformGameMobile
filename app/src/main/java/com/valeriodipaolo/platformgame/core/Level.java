package com.valeriodipaolo.platformgame.core;

import java.util.ArrayList;
import java.util.List;

/** Griglia di caselle del livello più le posizioni iniziali di monete e nemici. Riga 0 in alto. */
public final class Level {
    public static final char EMPTY = '.', GROUND = '#', BRICK = 'B', BONUS = '?', USED = 'U', STONE = 'S';
    /** Mattoncino identico agli altri, ma colpito da sotto rivela l'oggetto segreto. */
    public static final char SECRET = 'H';
    /** Piattaforma che crolla poco dopo essere stata toccata (livello dell'incendio). */
    public static final char CRUMBLE = 'C', DOOR = 'D';
    /** Fiore a molla: chi ci atterra sopra viene lanciato in alto (livello di primavera). */
    public static final char SPRING = 'P';

    public final int w, h;
    private final char[] t;
    public String id = "l1";
    /** 0 = prato d'estate, 1 = autunno, 2 = aurora boreale, 3 = incendio, 4 = primavera. */
    public int theme;
    public float startX = 3, startY = 8;
    /** Posizione della cella energetica (c'è in tutti i livelli). */
    public float[] battery;
    /** Colonna dell'asta del traguardo. */
    public int flagX;
    /** Riga del terreno su cui poggia l'asta (l'asta non è un ostacolo). */
    public int flagBaseY;
    /** Pezzi di gemma sparsi nel livello: {x, y}. */
    public final List<float[]> shards = new ArrayList<float[]>();
    /** Nemici: {x, y} (scarabeo) oppure {x, y, tipo, da, a} con il tratto percorso. */
    public final List<float[]> enemies = new ArrayList<float[]>();

    /**
     * Muro di fuoco che insegue il giocatore: parte da fireStart (NaN = nessun fuoco) e avanza alla velocità della zona
     * più avanzata raggiunta: fireZones = {{x, velocità in caselle al secondo}, ...} (velocità 0 = il fuoco si ferma).
     */
    public float fireStart = Float.NaN;
    public float[][] fireZones = new float[0][];
    /** Getti di fiamma a tempo: {x, riga del terreno, periodo, fase}. Il getto è acceso il 40% del periodo, preceduto da un avviso. */
    public final List<float[]> jets = new ArrayList<float[]>();
    /** Cespugli di rovi che pungono: {x, riga della casella libera su cui poggiano}. */
    public final List<float[]> thorns = new ArrayList<float[]>();

    public Level(int w, int h) {
        this.w = w;
        this.h = h;
        t = new char[w * h];
        java.util.Arrays.fill(t, EMPTY);
    }

    public char get(int x, int y) {
        if (x < 0 || x >= w) return STONE;       // muri invisibili ai bordi
        if (y < 0 || y >= h) return EMPTY;       // sopra: cielo; sotto: precipizio
        return t[y * w + x];
    }

    public void set(int x, int y, char c) {
        if (x >= 0 && x < w && y >= 0 && y < h) t[y * w + x] = c;
    }

    /** Colonna sinistra della catapulta vicino al portone (riporta a inizio livello). */
    public int catX;

    /**
     * Chiude il fine livello: muro altissimo sopra il portone e fino al bordo (non si scavalca con il salto), battenti chiusi (casella DOOR) e
     * una catapulta sul terreno piano poco prima dell'ingresso.
     */
    public void sealEnd() {
        for (int y = 0; y <= flagBaseY - 4; y++) for (int x = Math.max(0, flagX - 2); x < w; x++) set(x, y, STONE);
        for (int y = flagBaseY - 3; y < flagBaseY; y++) set(flagX, y, DOOR);
        int cx = flagX - 9;
        for (; cx > flagX - 30; cx--) {
            boolean ok = true;
            for (int x = cx; x < cx + 3 && ok; x++) if (!solid(x, flagBaseY) || solid(x, flagBaseY - 1) || solid(x, flagBaseY - 2)) ok = false;
            if (ok) break;
        }
        catX = cx;
    }

    public boolean solid(int x, int y) {
        return get(x, y) != EMPTY;
    }
}
