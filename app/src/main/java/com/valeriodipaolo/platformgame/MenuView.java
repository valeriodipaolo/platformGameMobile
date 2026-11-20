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
        hero = Math.max(0, Math.min(4, prefs.getInt("hero", 0)));
        digital = prefs.getInt("ctrlmode", 0) == 1;
        painter = new AndroidPainter(ctx);
    }

    private static final int LEVELS = 40, PER_ROW = 14;

    private float btnW() { return getWidth() * 0.064f; }
    private float btnH() { return getHeight() * 0.14f; }
    private float btnY(int i) { return getHeight() * (0.335f + 0.158f * (i / PER_ROW)); }
    private float btnX(int i) { return getWidth() * 0.5f + (i % PER_ROW - PER_ROW / 2f) * (btnW() + getWidth() * 0.0055f) + getWidth() * 0.003f; }

    /** In basso: a sinistra il personaggio (cinque riquadri con la sua figura), a destra i comandi dei livelli 3D. */
    private int hero;
    private boolean digital;
    private float heroW() { return getHeight() * 0.11f; }
    private float heroX(int i) { return getWidth() * 0.3f + (i - 2.5f) * heroW() * 1.15f; }
    private float heroY() { return getHeight() * 0.87f; }
    private float ctrlW() { return getWidth() * 0.13f; }
    private float ctrlX(int i) { return getWidth() * (0.585f + 0.14f * i); }
    private float ctrlY() { return getHeight() * 0.885f; }
    private static final String[] CTRL = {"ANALOGICO", "DIGITALE"};
    private static final String[] CTRL_HINT = {"più spingi, più corri", "solo acceso o spento, sempre a piena velocità"};

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight(), s = h / 12f;
        painter.begin(canvas);
        float sw = Math.max(w, h * painter.aspect(Img.SKY));
        painter.image(Img.SKY, (w - sw) / 2, 0, sw, sw / painter.aspect(Img.SKY), false, 1);
        float mh = h * 0.55f, mw = mh * painter.aspect(Img.MOUNTAINS);
        for (float x = (w - mw) / 2 - mw; x < w; x += mw) painter.image(Img.MOUNTAINS, x, h - mh, mw + 1, mh, false, 1);
        painter.rect(0, 0, w, h, 0x55000000);
        painter.image(Img.heroSpin(hero, 3), w / 2 - 1.0f * s, 0.25f * s, 2.0f * s, 2.6f * s, false, 1);
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
        painter.text("Personaggio: " + com.valeriodipaolo.platformgame.core3d.HeroModel.characterName(hero), (heroX(0) + heroX(4) + heroW()) / 2, heroY() - h * 0.015f, 0.3f * s, Painter.ALIGN_CENTER, 0xFFFFFFFF);
        for (int i = 0; i < 5; i++) {
            boolean on = i == hero;
            painter.rect(heroX(i), heroY(), heroW(), heroW(), on ? 0xFF4FC3F7 : 0xCC14324A);
            float ih = heroW() * 0.94f, iw = ih * 112f / 144f;
            painter.image(Img.heroIdle(i), heroX(i) + (heroW() - iw) / 2, heroY() + heroW() * 0.03f, iw, ih, false, 1);
        }
        painter.text("Comandi 3D: " + CTRL_HINT[digital ? 1 : 0], ctrlX(0) + ctrlW() + getWidth() * 0.005f, heroY() - h * 0.015f, 0.3f * s, Painter.ALIGN_CENTER, 0xFFFFFFFF);
        for (int i = 0; i < 2; i++) {
            boolean on = (i == 1) == digital;
            painter.rect(ctrlX(i), ctrlY(), ctrlW(), heroW() * 0.7f, on ? 0xFF4FC3F7 : 0xCC14324A);
            painter.text(CTRL[i], ctrlX(i) + ctrlW() / 2, ctrlY() + heroW() * 0.47f, 0.36f * s, Painter.ALIGN_CENTER, on ? 0xFF0B2233 : 0xFFFFFFFF);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_UP) {
            for (int i = 0; i < 5; i++) {
                float pad = heroW() * 0.07f;
                if (e.getX() >= heroX(i) - pad && e.getX() <= heroX(i) + heroW() + pad && e.getY() >= heroY() - pad && e.getY() <= heroY() + heroW() + pad) {
                    hero = i;
                    prefs.edit().putInt("hero", i).apply();
                    invalidate();
                    return true;
                }
            }
            for (int i = 0; i < 2; i++) {
                if (e.getX() >= ctrlX(i) && e.getX() <= ctrlX(i) + ctrlW() && e.getY() >= ctrlY() - heroW() * 0.1f && e.getY() <= ctrlY() + heroW() * 0.8f) {
                    digital = i == 1;
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
