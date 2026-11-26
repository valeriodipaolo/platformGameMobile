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
        ctrl = Math.max(0, Math.min(2, prefs.getInt("ctrlmode", 0)));
        painter = new AndroidPainter(ctx);
    }

    private static final int LEVELS = Levels.ORDER.length, PER_ROW = 15;

    private float btnW() { return getWidth() * 0.0595f; }
    private float btnH() { return getHeight() * 0.14f; }
    private float btnY(int i) { return getHeight() * (0.335f + 0.158f * (i / PER_ROW)); }
    private float btnX(int i) { return getWidth() * 0.5f + (i % PER_ROW - PER_ROW / 2f) * (btnW() + getWidth() * 0.0055f) + getWidth() * 0.003f; }

    /** In basso: i comandi dei livelli 3D (levetta analogica, levetta digitale, croce direzionale). */
    private int ctrl;
    private float heroW() { return getHeight() * 0.11f; }
    private float heroY() { return getHeight() * 0.87f; }
    private float ctrlW() { return getWidth() * 0.15f; }
    private float ctrlX(int i) { return getWidth() * (0.5f + 0.165f * (i - 1)) - ctrlW() / 2; }
    private float ctrlY() { return getHeight() * 0.885f; }
    private static final String[] CTRL = {"ANALOGICO", "DIGITALE", "CROCE"};
    private static final String[] CTRL_HINT = {"più spingi, più corri", "levetta a otto direzioni, sempre a piena velocità", "croce direzionale, sempre a piena velocità"};

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
        String[] subById = Levels.NAMES;
        for (int i = 0; i < LEVELS; i++) {
            float x = btnX(i), y = btnY(i);
            boolean green = !Levels.seasonal(Levels.ORDER[i]);          // mondi senza stagione, in coda: verdi
            painter.rect(x, y, btnW(), btnH(), green ? 0xCC14482A : 0xCC14324A);
            painter.rect(x, y, btnW(), 0.08f * s, green ? 0xFF66BB6A : 0xFF4FC3F7);
            painter.text("LIVELLO " + (i + 1), x + btnW() / 2, y + btnH() * 0.47f, 0.24f * s, Painter.ALIGN_CENTER, 0xFFFFFFFF);
            painter.text(subById[Levels.ORDER[i]], x + btnW() / 2, y + btnH() * 0.8f, 0.15f * s, Painter.ALIGN_CENTER, green ? 0xFFC8E6C9 : 0xFFB3E5FC);
        }
        painter.text("Comandi 3D: " + CTRL_HINT[ctrl], w / 2, heroY() - h * 0.015f, 0.3f * s, Painter.ALIGN_CENTER, 0xFFFFFFFF);
        for (int i = 0; i < 3; i++) {
            boolean on = i == ctrl;
            painter.rect(ctrlX(i), ctrlY(), ctrlW(), heroW() * 0.7f, on ? 0xFF4FC3F7 : 0xCC14324A);
            painter.text(CTRL[i], ctrlX(i) + ctrlW() / 2, ctrlY() + heroW() * 0.47f, 0.36f * s, Painter.ALIGN_CENTER, on ? 0xFF0B2233 : 0xFFFFFFFF);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_UP) {
            for (int i = 0; i < 3; i++) {
                if (e.getX() >= ctrlX(i) && e.getX() <= ctrlX(i) + ctrlW() && e.getY() >= ctrlY() - heroW() * 0.1f && e.getY() <= ctrlY() + heroW() * 0.8f) {
                    ctrl = i;
                    prefs.edit().putInt("ctrlmode", i).apply();
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
