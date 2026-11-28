package com.valeriodipaolo.platformgame.core3d;

import com.valeriodipaolo.platformgame.core.Controls;
import com.valeriodipaolo.platformgame.core.Img;
import com.valeriodipaolo.platformgame.core.Painter;

/** Scritte, pulsanti e joystick sopra la scena 3D. */
public final class Hud3D {
    private static final int WHITE = 0xFFFFFFFF;

    /** Raggio del joystick in pixel. */
    public static float joyRadius(float sw, float sh) { return 0.17f * sh; }
    public static float joyHomeX(float sw, float sh) { return 0.05f * sw + joyRadius(sw, sh); }
    public static float joyHomeY(float sw, float sh) { return sh - 0.07f * sh - joyRadius(sw, sh); }

    /** Al posto del joystick si disegna una croce direzionale. */
    public boolean dpad;

    /**
     * joyActive: un dito sta usando il joystick, con centro (joyCx, joyCy) e spostamento (joyDx, joyDy) da -1 a 1.
     * error: messaggio da mostrare se la grafica 3D non parte.
     */
    public void draw(World3D w, Painter p, float sw, float sh, boolean joyActive, float joyCx, float joyCy,
                     float joyDx, float joyDy, String error) {
        float s = sh / 12f;
        float fd = w.fade();                        // dissolvenza nel buio del portone
        if (fd > 0) p.rect(0, 0, sw, sh, ((int) (fd * 255) << 24));
        float m = 0.06f * sw, ts = 0.5f * s, base = 0.95f * s;
        p.image(Img.GEM + 3, m, 0.34f * s, 0.78f * s, 0.78f * s, false, 1);
        p.text(w.gems + "/" + w.gemTotal, m + 0.9f * s, base, ts, Painter.ALIGN_LEFT, WHITE);
        float ix = m, iy = 1.3f * s, isz = 0.85f * s;
        p.image(Img.COIN, ix, iy, isz, isz, false, w.coinOwned ? 1f : 0.35f);
        p.image(Img.GEM_FULL, ix + isz * 1.25f, iy, isz, isz, false, w.gemOwned ? 1f : 0.4f);
        p.text(com.valeriodipaolo.platformgame.core.Levels.title(w.levelNo), sw / 2, base - 0.52f * s, 0.3f * s, Painter.ALIGN_CENTER, 0xFFFFE082);   // numero e nome del livello
        p.text("PUNTI " + w.score, sw / 2, base, ts, Painter.ALIGN_CENTER, WHITE);
        if (com.valeriodipaolo.platformgame.core.Debug.on) {
            int cs = w.debugCoinState();
            p.text("DEBUG  ·  gemme saltate: " + w.debugSkipped() + "  ·  gettone: " + (cs == 2 ? "preso" : cs == 1 ? "SALTATO" : "davanti"),
                    sw / 2, base + 0.7f * ts, 0.6f * ts, Painter.ALIGN_CENTER, cs == 1 ? 0xFFFF5252 : 0xFFFFEB3B);
        }
        p.text("VITE " + w.lives, sw - m, base, ts, Painter.ALIGN_RIGHT, WHITE);
        float pip = 0.36f * s, gap = 0.12f * s, py = base + 0.22f * s;
        for (int i = 0; i < World3D.MAX_HP; i++) {
            float px = sw - m - (World3D.MAX_HP - i) * pip - (World3D.MAX_HP - 1 - i) * gap;
            p.rect(px - 0.04f * s, py - 0.04f * s, pip + 0.08f * s, pip + 0.08f * s, 0xB0000000);
            p.rect(px, py, pip, pip, i < w.hp ? 0xFFE53935 : 0xFF4A4A4A);
        }
        if (!Float.isNaN(w.fireZ)) {               // il fuoco è alle spalle: bagliore rosso in basso quando si avvicina
            float d = w.pz - w.fireZ;
            if (d < 20f) {
                float k = (float) Math.pow(1f - Math.max(0f, d) / 20f, 1.5f);
                boolean bees = w.chase == 1;
                for (int i = 0; i < 8; i++) p.rect(0, sh - (i + 1) * 0.16f * s, sw, 0.16f * s, ((int) (k * (150 - i * 17)) << 24) | (bees ? 0xE0A800 : w.chase == 2 ? 0x6A5A44 : 0xFF4A10));
                if (d < 12f) p.text(bees ? "API!" : w.chase == 2 ? "IL MASSO!" : "FUOCO!", sw / 2, sh - 0.6f * s, 0.55f * s, Painter.ALIGN_CENTER, ((int) (k * 255) << 24) | (bees ? 0xFFF176 : 0xFFD54F));
            }
        }
        if (w.toast != null && w.toastTime > 0) {
            int a = (int) (255 * Math.min(1f, w.toastTime * 2.5f));
            p.text(w.toast, sw / 2, 2.6f * s, 0.7f * s, Painter.ALIGN_CENTER, (a << 24) | 0xFFE082);
        }

        // fuga: quando l'inseguitore si avvicina, i bordi dello schermo si arrossano (più forte quanto più è vicino)
        if (!w.carts.isEmpty() && !Float.isNaN(w.fireZ) && w.state == World3D.PLAYING) {
            float chaseGap = w.pz - w.fireZ, k = Math.max(0f, Math.min(1f, (8.5f - chaseGap) / 5.5f));
            if (k > 0) {
                float pulse = 0.75f + 0.25f * (float) Math.sin(w.time * 9f);
                for (int i = 0; i < 6; i++) {
                    int a = (int) (k * pulse * (110 - i * 17));
                    if (a <= 0) continue;
                    float t = (i + 1) * 0.035f * sh;
                    int c = (a << 24) | 0xC01010;
                    p.rect(0, sh - t, sw, t, c); p.rect(0, 0, 0.4f * t, sh, c); p.rect(sw - 0.4f * t, 0, 0.4f * t, sh, c);
                }
            }
        }

        // joystick: compare dove si appoggia il dito, altrimenti resta nella posizione di riposo
        float jr = joyRadius(sw, sh);
        float cx = joyHomeX(sw, sh), cy = joyHomeY(sw, sh);           // il joystick resta fermo nella sua posizione
        boolean sky = w.theme == 12;                       // sulle nuvole i comandi bianchi non si vedrebbero: fondo scuro
        if (sky && !dpad && !w.inCart()) p.image(Img.HV_PAD, cx - jr * 1.05f, cy - jr * 1.05f, 2.1f * jr, 2.1f * jr, false, 1);
        if (sky && dpad && !w.inCart()) {
            float e = crossOff(sw, sh) + crossSize(sw, sh) * 0.6f;
            p.image(Img.HV_PAD, crossX(sw, sh) - e, crossY(sw, sh) - e, 2 * e, 2 * e, false, 1);
        }
        boolean steer = w.inCart() && w.state == World3D.PLAYING;   // sul carrello o di corsa: si sterza soltanto, con destra e sinistra come nel 2D
        float bs0 = Controls.size(sw, sh), bty0 = Controls.btnY(sw, sh);
        if (steer) {
            if (sky) for (float bxx : new float[] {Controls.leftX(sw, sh), Controls.rightX(sw, sh)}) p.image(Img.HV_PAD, bxx - 0.1f * bs0, bty0 - 0.1f * bs0, 1.2f * bs0, 1.2f * bs0, false, 1);
            p.image(Img.BTN_LEFT, Controls.leftX(sw, sh), bty0, bs0, bs0, false, joyActive && joyDx < -0.3f ? 1f : 0.8f);
            p.image(Img.BTN_RIGHT, Controls.rightX(sw, sh), bty0, bs0, bs0, false, joyActive && joyDx > 0.3f ? 1f : 0.8f);
        } else if (dpad) drawCross(p, sw, sh, joyActive ? joyDx : 0, joyActive ? joyDy : 0);
        else {
            p.image(Img.JOY_BASE, cx - jr, cy - jr, 2 * jr, 2 * jr, false, joyActive ? 1f : 0.7f);
            float kr = jr * 0.46f;
            p.image(Img.JOY_KNOB, cx + (joyActive ? joyDx : 0) * jr * 0.75f - kr, cy + (joyActive ? joyDy : 0) * jr * 0.75f - kr,
                    2 * kr, 2 * kr, false, 1);
        }

        float bs = Controls.size(sw, sh), bty = Controls.btnY(sw, sh);
        if (sky) for (float bxx : new float[] {Controls.jumpX(sw, sh), Controls.spinX(sw, sh)}) p.image(Img.HV_PAD, bxx - 0.1f * bs, bty - 0.1f * bs, 1.2f * bs, 1.2f * bs, false, 1);
        p.image(Img.BTN_JUMP, Controls.jumpX(sw, sh), bty, bs, bs, false, 1);
        p.image(Img.BTN_SPIN, Controls.spinX(sw, sh), bty, bs, bs, false, w.spinning() || w.spinCool > 0 ? 0.45f : 1f);

        if (w.state == World3D.GAME_OVER || (w.state == World3D.WON && w.stateTime > 1.4f)) {
            p.rect(0, 0, sw, sh, 0x99000000);
            boolean won = w.state == World3D.WON;
            p.text(won ? "LIVELLO COMPLETATO!" : "PARTITA FINITA", sw / 2, 3.3f * s, 1.1f * s, Painter.ALIGN_CENTER, WHITE);
            p.text("Punti: " + w.score + "   Gemme: " + w.gems + "/" + w.gemTotal, sw / 2, 4.6f * s, 0.6f * s, Painter.ALIGN_CENTER, WHITE);
            if (won) {
                float sz = 1.1f * s, step = 3.6f * s, y0 = 5.3f * s;
                int[] icons = {Img.COIN, Img.GEM_FULL};
                boolean[] owned = {w.coinOwned, w.gemOwned};
                String[] names = {"Gettone d'oro", "Gemma " + w.gems + "/" + w.gemTotal};
                for (int i = 0; i < 2; i++) {
                    float x = sw / 2 + (i - 0.5f) * step;
                    p.image(icons[i], x - sz / 2, y0, sz, sz, false, owned[i] ? 1f : 0.25f);
                    p.text(names[i], x, y0 + sz + 0.55f * s, 0.4f * s, Painter.ALIGN_CENTER, owned[i] ? WHITE : 0xFF9E9E9E);
                }
            }
            if (w.stateTime > 1.2f) {
                p.text(won ? com.valeriodipaolo.platformgame.core.Levels.wonText(w.levelNo) : "Tocca lo schermo per riprovare",
                        sw / 2, 9.4f * s, 0.5f * s, Painter.ALIGN_CENTER, 0xFFFFE082);
            }
        }
        if (error != null) {
            p.rect(0, sh * 0.4f, sw, sh * 0.2f, 0xCC000000);
            p.text(error, sw / 2, sh * 0.52f, 0.4f * s, Painter.ALIGN_CENTER, 0xFFFF8A80);
        }
    }

    /** Geometria della croce: pulsanti grandi quasi come salto e giravolta, centro della croce in basso a sinistra. */
    public static float crossSize(float sw, float sh) { return 0.19f * sh; }
    public static float crossOff(float sw, float sh) { return 0.155f * sh; }
    public static float crossX(float sw, float sh) { return 0.04f * sw + crossOff(sw, sh) + crossSize(sw, sh) / 2; }
    public static float crossY(float sw, float sh) { return sh - 0.04f * sh - crossOff(sw, sh) - crossSize(sw, sh) / 2; }

    /**
     * Pulsanti della croce sotto il punto (x, y), come maschera di bit: 1 su, 2 giù, 4 sinistra, 8 destra. L'area di ogni pulsante
     * sborda un poco oltre il disegno, così tra due pulsanti vicini (su e destra, per esempio) il pollice li preme entrambi e il
     * robot va in obliquo; al centro della croce e fuori dai pulsanti non si preme niente.
     */
    public static int crossButtons(float x, float y, float sw, float sh) {
        float cx = crossX(sw, sh), cy = crossY(sw, sh), off = crossOff(sw, sh), rr = 0.135f * sh;
        float[][] c = {{cx, cy - off}, {cx, cy + off}, {cx - off, cy}, {cx + off, cy}};
        int mask = 0;
        for (int i = 0; i < 4; i++) {
            float dx = x - c[i][0], dy = y - c[i][1];
            if (dx * dx + dy * dy <= rr * rr) mask |= 1 << i;
        }
        return mask;
    }

    /** Croce direzionale: quattro pulsanti tondi come salto e giravolta, a croce; quello premuto è pieno, gli altri velati. */
    private static void drawCross(Painter p, float sw, float sh, float dx, float dy) {
        float bs = crossSize(sw, sh), off = crossOff(sw, sh), cx = crossX(sw, sh), cy = crossY(sw, sh);
        p.image(Img.DPAD_UP, cx - bs / 2, cy - off - bs / 2, bs, bs, false, dy < -0.3f ? 1f : 0.7f);
        p.image(Img.DPAD_DOWN, cx - bs / 2, cy + off - bs / 2, bs, bs, false, dy > 0.3f ? 1f : 0.7f);
        p.image(Img.DPAD_LEFT, cx - off - bs / 2, cy - bs / 2, bs, bs, false, dx < -0.3f ? 1f : 0.7f);
        p.image(Img.DPAD_RIGHT, cx + off - bs / 2, cy - bs / 2, bs, bs, false, dx > 0.3f ? 1f : 0.7f);
    }
}
