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


    MenuView(Context ctx, Listener listener) {
        super(ctx);
        this.listener = listener;
        prefs = ctx.getSharedPreferences("platformgame", Context.MODE_PRIVATE);
        run2d = Math.max(0, Math.min(4, prefs.getInt("run2d", 0)));
        run3d = Math.max(0, Math.min(4, prefs.getInt("run3d", 0)));
        painter = new AndroidPainter(ctx);
    }

    private static final int LEVELS = 40, PER_ROW = 14;

    private float btnW() { return getWidth() * 0.064f; }
    private float btnH() { return getHeight() * 0.14f; }
    private float btnY(int i) { return getHeight() * (0.335f + 0.158f * (i / PER_ROW)); }
    private float btnX(int i) { return getWidth() * 0.5f + (i % PER_ROW - PER_ROW / 2f) * (btnW() + getWidth() * 0.0055f) + getWidth() * 0.003f; }

    /** Stili di corsa: due gruppi di cinque quadratini in basso, Corsa 2D a sinistra e Corsa 3D a destra. */
    private int run2d, run3d;
    private float runW() { return getHeight() * 0.09f; }
    private float runX(int row, int i) { return getWidth() * (row == 0 ? 0.27f : 0.73f) + (i - 2.5f) * runW() * 1.22f; }
    private float runY() { return getHeight() * 0.875f; }

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
        for (int row = 0; row < 2; row++) {
            int sel = row == 0 ? run2d : run3d;
            painter.text((row == 0 ? "Corsa 2D: " : "Corsa 3D: ") + Img.RUN_STYLE[sel], (runX(row, 0) + runX(row, 4) + runW()) / 2, runY() - h * 0.018f, 0.3f * s, Painter.ALIGN_CENTER, 0xFFFFFFFF);
            for (int i = 0; i < 5; i++) {
                boolean on = i == sel;
                painter.rect(runX(row, i), runY(), runW(), runW(), on ? 0xFF4FC3F7 : 0xCC14324A);
                painter.text(String.valueOf(i + 1), runX(row, i) + runW() / 2, runY() + runW() * 0.7f, 0.55f * s, Painter.ALIGN_CENTER, on ? 0xFF0B2233 : 0xFFFFFFFF);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_UP) {
            for (int row = 0; row < 2; row++) for (int i = 0; i < 5; i++) {
                float pad = runW() * 0.1f;
                if (e.getX() >= runX(row, i) - pad && e.getX() <= runX(row, i) + runW() + pad && e.getY() >= runY() - pad && e.getY() <= runY() + runW() + pad) {
                    if (row == 0) { run2d = i; prefs.edit().putInt("run2d", i).apply(); }
                    else { run3d = i; prefs.edit().putInt("run3d", i).apply(); }
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
