package com.valeriodipaolo.platformgame;

import android.content.Context;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;

import com.valeriodipaolo.platformgame.core.Img;
import com.valeriodipaolo.platformgame.core.Levels;
import com.valeriodipaolo.platformgame.core.Painter;

/** Schermata iniziale: scelta del livello. */
final class MenuView extends View {
    interface Listener {
        void onLevel(int level);
    }

    private final AndroidPainter painter;
    private final Listener listener;
    private final android.content.SharedPreferences prefs;
    private int ctrl, anim;
    private static final String[] CTRL_TEXT = {
            "Joystick libero: compare dove tocchi",
            "Joystick fisso in basso a sinistra",
            "Joystick libero, solo 4 direzioni",
            "Joystick fisso, solo 4 direzioni",
            "Carro armato: sinistra/destra ruota, su/giù avanza",
            "Joystick fisso, assi calamitati e diagonali più dolci"};

    MenuView(Context ctx, Listener listener) {
        super(ctx);
        this.listener = listener;
        prefs = ctx.getSharedPreferences("platformgame", Context.MODE_PRIVATE);
        ctrl = Math.max(1, Math.min(6, prefs.getInt("ctrl3d", 2)));
        anim = prefs.getInt("anim3d", 1) == 0 ? 0 : 1;
        run2d = Math.max(0, Math.min(4, prefs.getInt("run2d", 0)));
        run3d = Math.max(0, Math.min(4, prefs.getInt("run3d", 0)));
        painter = new AndroidPainter(ctx);
    }

    private static final int LEVELS = 40, PER_ROW = 14;

    private float btnW() { return getWidth() * 0.064f; }
    private float btnH() { return getHeight() * 0.14f; }
    private float btnY(int i) { return getHeight() * (0.335f + 0.158f * (i / PER_ROW)); }
    private float btnX(int i) { return getWidth() * 0.5f + (i % PER_ROW - PER_ROW / 2f) * (btnW() + getWidth() * 0.0055f) + getWidth() * 0.003f; }

    private float chipW() { return getHeight() * 0.1f; }
    private float chipX(int i) { return getWidth() * 0.5f + (i - 2.5f) * chipW() * 1.25f; }
    private float chipY() { return getHeight() * 0.87f; }
    private float animW() { return getWidth() * 0.1f; }
    private float animX(int i) { return getWidth() * (0.02f + 0.105f * i); }
    /** Stili di corsa: due righe (2D sopra, 3D sotto) di cinque quadratini, a destra. */
    private int run2d, run3d;
    private float runW() { return getHeight() * 0.068f; }
    private float runX(int i) { return getWidth() * 0.985f - (5 - i) * runW() * 1.18f; }
    private float runY(int row) { return getHeight() * (row == 0 ? 0.835f : 0.92f); }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight(), s = h / 12f;
        painter.begin(canvas);
        float sw = Math.max(w, h * painter.aspect(Img.SKY));
        painter.image(Img.SKY, (w - sw) / 2, 0, sw, sw / painter.aspect(Img.SKY), false, 1);
        float mh = h * 0.55f, mw = mh * painter.aspect(Img.MOUNTAINS);
        for (float x = (w - mw) / 2 - mw; x < w; x += mw) painter.image(Img.MOUNTAINS, x, h - mh, mw + 1, mh, false, 1);
        painter.rect(0, 0, w, h, 0x55000000);
        painter.image(Img.HERO_SPIN + 3, w / 2 - 1.0f * s, 0.25f * s, 2.0f * s, 2.6f * s, false, 1);
        painter.text("ROBO JUMP", w / 2, 3.55f * s, 1.0f * s, Painter.ALIGN_CENTER, 0xFFFFFFFF);
        String[] subById = {null, "Il prato", "Bosco (3D)", "Prato d'autunno", "Bosco d'autunno (3D)", "Tundra boreale", "Lago gelato (3D)",
                "Prato in fiamme", "Bosco in fiamme (3D)", "Primavera", "Primavera e api (3D)", "Il cielo", "Sulle nuvole (3D)",
                "Talpe e lumache", "Radure (3D)", "Castagne e gufi", "Querceto (3D)", "Neve e pupazzi", "Ghiaccio (3D)",
                "Brace e lapilli", "Rogo (3D)", "Rane e fiori", "Giardino (3D)", "Stelle e soli", "Cielo alto (3D)",
                "Valle delle molle", "Labirinto (3D)", "Ghiaccio fragile", "Arcipelago (3D)", "Le due strade", "Rovi (3D)",
                "Pioggia di ricci", "Fuga dal rogo (3D)", "Scala celeste", "La vetta (3D)",
                "Isola: le casse", "Isola: il masso", "Isola: il fiume",
                "Le dune", "La fabbrica", "La miniera"};
        for (int i = 0; i < LEVELS; i++) {
            float x = btnX(i), y = btnY(i);
            painter.rect(x, y, btnW(), btnH(), 0xCC14324A);
            painter.rect(x, y, btnW(), 0.08f * s, 0xFF4FC3F7);
            painter.text("LIVELLO " + (i + 1), x + btnW() / 2, y + btnH() * 0.47f, 0.27f * s, Painter.ALIGN_CENTER, 0xFFFFFFFF);
            painter.text(subById[Levels.ORDER[i]], x + btnW() / 2, y + btnH() * 0.8f, 0.15f * s, Painter.ALIGN_CENTER, 0xFFB3E5FC);
        }
        painter.text("Comandi dei livelli 3D: " + CTRL_TEXT[ctrl - 1], w * 0.47f, h * 0.855f, 0.3f * s, Painter.ALIGN_CENTER, 0xFFFFFFFF);
        for (int i = 0; i < 6; i++) {
            boolean on = i + 1 == ctrl;
            painter.rect(chipX(i), chipY(), chipW(), chipW(), on ? 0xFF4FC3F7 : 0xCC14324A);
            painter.text(String.valueOf(i + 1), chipX(i) + chipW() / 2, chipY() + chipW() * 0.7f, 0.6f * s, Painter.ALIGN_CENTER, on ? 0xFF0B2233 : 0xFFFFFFFF);
        }
        painter.text("Animazioni eroe 3D", animX(0) + animW() + w * 0.005f, h * 0.855f, 0.3f * s, Painter.ALIGN_CENTER, 0xFFFFFFFF);
        for (int row = 0; row < 2; row++) {
            int sel = row == 0 ? run2d : run3d;
            painter.text((row == 0 ? "Corsa 2D: " : "Corsa 3D: ") + Img.RUN_STYLE[sel], runX(0) - w * 0.008f, runY(row) + runW() * 0.62f, 0.26f * s, Painter.ALIGN_RIGHT, 0xFFFFFFFF);
            for (int i = 0; i < 5; i++) {
                boolean on = i == sel;
                painter.rect(runX(i), runY(row), runW(), runW(), on ? 0xFF4FC3F7 : 0xCC14324A);
                painter.text(String.valueOf(i + 1), runX(i) + runW() / 2, runY(row) + runW() * 0.72f, 0.42f * s, Painter.ALIGN_CENTER, on ? 0xFF0B2233 : 0xFFFFFFFF);
            }
        }
        String[] an = {"ATTUALI", "NUOVE"};
        for (int i = 0; i < 2; i++) {
            boolean on = i == anim;
            painter.rect(animX(i), chipY(), animW(), chipW(), on ? 0xFF4FC3F7 : 0xCC14324A);
            painter.text(an[i], animX(i) + animW() / 2, chipY() + chipW() * 0.68f, 0.34f * s, Painter.ALIGN_CENTER, on ? 0xFF0B2233 : 0xFFFFFFFF);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_UP) {
            for (int row = 0; row < 2; row++) for (int i = 0; i < 5; i++) {
                float pad = runW() * 0.08f;
                if (e.getX() >= runX(i) - pad && e.getX() <= runX(i) + runW() + pad && e.getY() >= runY(row) - pad && e.getY() <= runY(row) + runW() + pad) {
                    if (row == 0) { run2d = i; prefs.edit().putInt("run2d", i).apply(); }
                    else { run3d = i; prefs.edit().putInt("run3d", i).apply(); }
                    invalidate();
                    return true;
                }
            }
            for (int i = 0; i < 2; i++) {
                if (e.getX() >= animX(i) && e.getX() <= animX(i) + animW() && e.getY() >= chipY() && e.getY() <= chipY() + chipW()) {
                    anim = i;
                    prefs.edit().putInt("anim3d", anim).apply();
                    invalidate();
                    return true;
                }
            }
            for (int i = 0; i < 6; i++) {
                float pad = chipW() * 0.12f;
                if (e.getX() >= chipX(i) - pad && e.getX() <= chipX(i) + chipW() + pad
                        && e.getY() >= chipY() - pad && e.getY() <= chipY() + chipW() + pad) {
                    ctrl = i + 1;
                    prefs.edit().putInt("ctrl3d", ctrl).apply();
                    invalidate();
                    return true;
                }
            }
            for (int i = 0; i < LEVELS; i++) {
                if (e.getX() >= btnX(i) && e.getX() <= btnX(i) + btnW() && e.getY() >= btnY(i) && e.getY() <= btnY(i) + btnH()) {
                    listener.onLevel(i);
                }
            }
        }
        return true;
    }
}
