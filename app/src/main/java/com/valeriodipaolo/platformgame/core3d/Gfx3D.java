package com.valeriodipaolo.platformgame.core3d;

/** Chi disegna la scena 3D: OpenGL sul telefono, un disegnatore software sul PC per le anteprime. */
public interface Gfx3D {
    /** Direzione verso la luce (normalizzata) e intensità, uguali per tutti i disegnatori. */
    float LX = 0.37f, LY = 0.82f, LZ = -0.44f;
    float AMBIENT = 0.46f, DIFFUSE = 0.64f;
    /** Illuminazione fisica approssimata (spazio lineare): sole caldo, cielo azzurro sopra, terra verde sotto. */
    float SUN_R = 2.4f, SUN_G = 2.1f, SUN_B = 1.7f;
    float SKY_R = 0.25f, SKY_G = 0.34f, SKY_B = 0.50f;
    float GND_R = 0.09f, GND_G = 0.11f, GND_B = 0.06f;

    void begin(float[] view, float[] proj, float fogR, float fogG, float fogB, float fogStart, float fogEnd, float fogMax);

    /** Sfondo a tutto schermo (mesh già in coordinate dello schermo, da -1 a 1), senza profondità. */
    void backdrop(Mesh m);

    /** emissive: 0 = illuminato normalmente, 1 = colore pieno come se emettesse luce. */
    void draw(Mesh m, float[] model, float r, float g, float b, float a, float emissive);

    void end();
}
