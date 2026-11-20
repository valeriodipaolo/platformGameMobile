package com.valeriodipaolo.platformgame.core3d;

/** Triangoli pronti da disegnare. Ogni vertice: posizione (3), normale (3), coordinate texture (2), colore (3), peso del vento (1). */
public final class Mesh {
    public static final int STRIDE = 12;

    public float[] v;
    /** Dopo il caricamento sulla scheda grafica i vertici si possono buttare (i pezzi di terreno, che sono la gran parte della memoria). */
    public boolean releasable;
    /** Numero di vertici (multiplo di 3). */
    public final int count;
    /** Indice dell'immagine in Img, oppure -1 per nessuna texture (solo colore). */
    public final int tex;
    /** Trasparenza sfumata (ombre, bandiera, anelli): si disegna dopo gli oggetti opachi. */
    public final boolean blend;
    /** La foschia in lontananza si applica a questa mesh. */
    public final boolean fog;
    /** Materiale: forza e durezza del riflesso (spec 0 = opaco), e se i bordi trasparenti vengono scartati (fogliame). */
    public float spec = 0f, shine = 16f;
    public boolean cutout;
    /** Getta un'ombra vera (il terreno, l'erba e i fiori no: non servono e costano). */
    public boolean castShadow = true;
    /** Sfera che contiene la mesh (nelle sue coordinate): serve a saltare nelle ombre quello che è lontano. */
    public float cx, cy, cz, radius = -1f;
    /** I vertici cambiano a ogni fotogramma (il mantello): chi disegna li ricarica quando version cambia. */
    public boolean dynamic;
    public int version;
    /** Spazio libero per chi disegna (per esempio l'identificativo del buffer sulla scheda grafica). */
    public Object handle;

    public Mesh(float[] v, int count, int tex, boolean blend, boolean fog) {
        this.v = v;
        this.count = count;
        this.tex = tex;
        this.blend = blend;
        this.fog = fog;
        if (v != null && count > 0) {
            float x0 = Float.MAX_VALUE, y0 = x0, z0 = x0, x1 = -x0, y1 = -x0, z1 = -x0;
            for (int i = 0; i < count * STRIDE; i += STRIDE) {
                x0 = Math.min(x0, v[i]); x1 = Math.max(x1, v[i]);
                y0 = Math.min(y0, v[i + 1]); y1 = Math.max(y1, v[i + 1]);
                z0 = Math.min(z0, v[i + 2]); z1 = Math.max(z1, v[i + 2]);
            }
            cx = (x0 + x1) * 0.5f; cy = (y0 + y1) * 0.5f; cz = (z0 + z1) * 0.5f;
            radius = 0.5f * (float) Math.sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0) + (z1 - z0) * (z1 - z0)) + 0.5f;
        }
    }
}
