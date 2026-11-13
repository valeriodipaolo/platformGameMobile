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

    MenuView(Context ctx, Listener listener) {
        super(ctx);
        this.listener = listener;
        painter = new AndroidPainter(ctx);
    }

    private float btnW() { return getWidth() * 0.34f; }
    private float btnH() { return getHeight() * 0.3f; }
    private float btnY() { return getHeight() * 0.5f; }
    private float btnX(int i) { return getWidth() * 0.5f + (i == 0 ? -btnW() - getWidth() * 0.03f : getWidth() * 0.03f); }

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
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_UP) {
            for (int i = 0; i < 2; i++) {
                if (e.getX() >= btnX(i) && e.getX() <= btnX(i) + btnW() && e.getY() >= btnY() && e.getY() <= btnY() + btnH()) {
                    listener.onLevel(i + 1);
                }
            }
        }
        return true;
    }
}
