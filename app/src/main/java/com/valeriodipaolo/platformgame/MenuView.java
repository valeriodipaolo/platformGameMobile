package com.valeriodipaolo.platformgame;

import android.content.Context;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;

import com.valeriodipaolo.platformgame.core.Img;
import com.valeriodipaolo.platformgame.core.Painter;

/** Schermata iniziale: scelta del livello. */
final class MenuView extends View {
    interface Listener {
        void onLevel(int level);
    }

    private final AndroidPainter painter;
    private final Listener listener;
    private final android.content.SharedPreferences prefs;
    private int ctrl;
    private static final String[] CTRL_TEXT = {
            "Joystick libero: compare dove tocchi",
            "Joystick fisso in basso a sinistra",
            "Joystick libero, solo 4 direzioni",
            "Joystick fisso, solo 4 direzioni",
            "Carro armato: sinistra/destra ruota, su/giù avanza"};

    MenuView(Context ctx, Listener listener) {
        super(ctx);
        this.listener = listener;
        prefs = ctx.getSharedPreferences("platformgame", Context.MODE_PRIVATE);
        ctrl = Math.max(1, Math.min(5, prefs.getInt("ctrl3d", 1)));
        painter = new AndroidPainter(ctx);
    }

    private float btnW() { return getWidth() * 0.34f; }
    private float btnH() { return getHeight() * 0.3f; }
    private float btnY() { return getHeight() * 0.5f; }
    private float btnX(int i) { return getWidth() * 0.5f + (i == 0 ? -btnW() - getWidth() * 0.03f : getWidth() * 0.03f); }

    private float chipW() { return getHeight() * 0.1f; }
    private float chipX(int i) { return getWidth() * 0.5f + (i - 2.5f) * chipW() * 1.25f; }
    private float chipY() { return getHeight() * 0.87f; }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight(), s = h / 12f;
        painter.begin(canvas);
        float sw = Math.max(w, h * painter.aspect(Img.SKY));
        painter.image(Img.SKY, (w - sw) / 2, 0, sw, sw / painter.aspect(Img.SKY), false, 1);
        float mh = h * 0.55f, mw = mh * painter.aspect(Img.MOUNTAINS);
        for (float x = (w - mw) / 2 - mw; x < w; x += mw) painter.image(Img.MOUNTAINS, x, h - mh, mw + 1, mh, false, 1);
        painter.rect(0, 0, w, h, 0x55000000);
        painter.image(Img.HERO_SPIN + 3, w / 2 - 1.4f * s, 0.5f * s, 2.8f * s, 3.6f * s, false, 1);
        painter.text("ROBO JUMP", w / 2, 5.3f * s, 1.3f * s, Painter.ALIGN_CENTER, 0xFFFFFFFF);
        String[] title = {"LIVELLO 1", "LIVELLO 2"}, sub = {"Il prato", "Il corridoio nel bosco (3D)"};
        for (int i = 0; i < 2; i++) {
            float x = btnX(i), y = btnY();
            painter.rect(x, y, btnW(), btnH(), 0xCC14324A);
            painter.rect(x, y, btnW(), 0.08f * s, 0xFF4FC3F7);
            painter.text(title[i], x + btnW() / 2, y + btnH() * 0.5f, 0.9f * s, Painter.ALIGN_CENTER, 0xFFFFFFFF);
            painter.text(sub[i], x + btnW() / 2, y + btnH() * 0.78f, 0.42f * s, Painter.ALIGN_CENTER, 0xFFB3E5FC);
        }
        painter.text("Comandi del livello 2: " + CTRL_TEXT[ctrl - 1], w / 2, h * 0.855f, 0.38f * s, Painter.ALIGN_CENTER, 0xFFFFFFFF);
        for (int i = 0; i < 5; i++) {
            boolean on = i + 1 == ctrl;
            painter.rect(chipX(i), chipY(), chipW(), chipW(), on ? 0xFF4FC3F7 : 0xCC14324A);
            painter.text(String.valueOf(i + 1), chipX(i) + chipW() / 2, chipY() + chipW() * 0.7f, 0.6f * s, Painter.ALIGN_CENTER, on ? 0xFF0B2233 : 0xFFFFFFFF);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_UP) {
            for (int i = 0; i < 5; i++) {
                float pad = chipW() * 0.12f;
                if (e.getX() >= chipX(i) - pad && e.getX() <= chipX(i) + chipW() + pad
                        && e.getY() >= chipY() - pad && e.getY() <= chipY() + chipW() + pad) {
                    ctrl = i + 1;
                    prefs.edit().putInt("ctrl3d", ctrl).apply();
                    invalidate();
                    return true;
                }
            }
            for (int i = 0; i < 2; i++) {
                if (e.getX() >= btnX(i) && e.getX() <= btnX(i) + btnW() && e.getY() >= btnY() && e.getY() <= btnY() + btnH()) {
                    listener.onLevel(i + 1);
                }
            }
        }
        return true;
    }
}
