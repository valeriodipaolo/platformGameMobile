package com.valeriodipaolo.platformgame.core3d;

/** Chi disegna la scena 3D: OpenGL sul telefono, un disegnatore software sul PC per le anteprime. */
public interface Gfx3D {
    /** Direzione verso la luce (normalizzata) e intensità, uguali per tutti i disegnatori. */
    float LX = 0.37f, LY = 0.82f, LZ = -0.44f;
    float AMBIENT = 0.46f, DIFFUSE = 0.64f;

    void begin(float[] view, float[] proj, float fogR, float fogG, float fogB, float fogStart, float fogEnd, float fogMax);

    /** Sfondo a tutto schermo (mesh già in coordinate dello schermo, da -1 a 1), senza profondità. */
    void backdrop(Mesh m);

    /** emissive: 0 = illuminato normalmente, 1 = colore pieno come se emettesse luce. */
    void draw(Mesh m, float[] model, float r, float g, float b, float a, float emissive);

    void end();
}
