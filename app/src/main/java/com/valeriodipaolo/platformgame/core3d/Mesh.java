package com.valeriodipaolo.platformgame.core3d;

/** Triangoli pronti da disegnare. Ogni vertice: posizione (3), normale (3), coordinate texture (2), colore (3), peso del vento (1). */
public final class Mesh {
    public static final int STRIDE = 12;

    public final float[] v;
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
    }
}
