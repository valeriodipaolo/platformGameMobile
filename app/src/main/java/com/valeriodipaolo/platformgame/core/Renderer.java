package com.valeriodipaolo.platformgame.core;

/** Disegna una scena completa del gioco su un Painter. */
public final class Renderer {
    private static final int WHITE = 0xFFFFFFFF;

    public void draw(Game g, Painter p, float sw, float sh) {
        float s = sh / 12f;                        // pixel per casella: sempre 12 caselle in altezza
        g.viewW = sw / s;
        float cam = g.camX;
        Level lv = g.level;

        // ---- sfondi in parallasse
        layer(p, Img.SKY, cam * s * 0.03f, 0, sh, sw);
        layer(p, Img.MOUNTAINS, cam * s * 0.12f, 3.9f * s, 6.6f * s, sw);
        layer(p, Img.HILLS, cam * s * 0.35f, 6.2f * s, 5.8f * s, sw);

        // ---- traguardo (dietro alle caselle)
        float poleX = (lv.flagX + 0.5f - cam) * s;
        if (poleX > -2 * s && poleX < sw + 2 * s) {
            for (int ty = 2; ty < lv.flagBaseY; ty++) {
                p.image(Img.POLE, poleX - 0.105f * s, Math.round(ty * s), 0.21f * s, s + 1, false, 1);
            }
            p.image(Img.POLE_TOP, poleX - 0.21f * s, 1.66f * s, 0.42f * s, 0.42f * s, false, 1);
            float t = g.state == Game.WON ? Math.min(1f, g.stateTime / 1.0f) : 0;
            float fy = 2.15f + t * (lv.flagBaseY - 1.3f - 2.15f);
            p.image(Img.FLAG, poleX + 0.06f * s, fy * s, 1.33f * s, 0.875f * s, false, 1);
        }

        // ---- caselle
        int tx0 = (int) Math.floor(cam), tx1 = (int) Math.floor(cam + g.viewW) + 1;
        for (int ty = 0; ty < lv.h; ty++) {
            for (int tx = tx0; tx <= tx1; tx++) {
                if (tx < 0 || tx >= lv.w) continue;
                char c = lv.get(tx, ty);
                if (c == Level.EMPTY) continue;
                int img;
                switch (c) {
                    case Level.GROUND: img = lv.get(tx, ty - 1) == Level.GROUND ? Img.DIRT : Img.GRASS; break;
                    case Level.BRICK: img = Img.BRICK; break;
                    case Level.BONUS: img = Img.BONUS; break;
                    case Level.USED: img = Img.USED; break;
                    default: img = Img.STONE; break;
                }
                float off = 0;
                if (tx == g.bumpTx && ty == g.bumpTy) {
                    off = -(float) Math.sin(Math.PI * g.bumpAge / Game.BUMP_TIME) * 0.3f * s;
                }
                float x0 = Math.round((tx - cam) * s), x1 = Math.round((tx + 1 - cam) * s);
                float y0 = Math.round(ty * s), y1 = Math.round((ty + 1) * s);
                p.image(img, x0, y0 + off, x1 - x0, y1 - y0, false, 1);
                if (img == Img.GRASS && !lv.solid(tx, ty - 1)) {
                    p.image(Img.FRINGE, x0, y0 - 0.27f * s, x1 - x0, 0.29f * s, false, 1);
                }
            }
        }

        // ---- monete
        int frame = (int) (g.time * 10f) % 6;
        for (float[] c : g.coins) {
            float x = (c[0] - cam) * s;
            if (x < -s || x > sw) continue;
            float bob = (float) Math.sin(g.time * 3f + c[0]) * 0.05f;
            p.image(Img.COIN + frame, x + 0.19f * s, (c[1] + 0.19f + bob) * s, 0.62f * s, 0.62f * s, false, 1);
        }
        for (float[] c : g.pops) {
            int f = (int) (c[3] * 30f) % 6;
            float a = Math.min(1f, (0.5f - c[3]) * 6f);
            p.image(Img.COIN + f, (c[0] - cam + 0.19f) * s, (c[1] + 0.19f) * s, 0.62f * s, 0.62f * s, false, a);
        }

        // ---- nemici
        for (Enemy e : g.enemies) {
            if (e.dead) continue;
            float x = (e.x + e.w / 2 - cam) * s;
            if (x < -2 * s || x > sw + 2 * s) continue;
            int img = e.flatTime > 0 ? Img.ENEMY_FLAT : Img.ENEMY + ((int) (e.anim * 7f) % 2);
            float w = 1.083f * s, h = 0.875f * s;
            p.image(img, x - w / 2, (e.y + e.h + 0.03f) * s - h, w, h, e.dir > 0, 1);
        }

        // ---- giocatore
        Body pl = g.player;
        int img;
        if (g.state == Game.DYING || (!pl.onGround && g.state == Game.PLAYING)) img = Img.HERO_JUMP;
        else if (Math.abs(pl.vx) > 0.3f) img = Img.HERO_RUN + ((int) g.runPhase % 4);
        else img = Img.HERO_IDLE;
        float hw = 1.167f * s, hh = 1.5f * s;
        p.image(img, (pl.x + pl.w / 2 - cam) * s - hw / 2, (pl.y + pl.h + 0.03f) * s - hh, hw, hh, g.facing < 0, 1);

        // ---- punteggio
        float m = 0.06f * sw, ts = 0.5f * s, base = 0.95f * s;
        p.image(Img.COIN, m, 0.42f * s, 0.62f * s, 0.62f * s, false, 1);
        p.text("× " + g.coinCount, m + 0.8f * s, base, ts, Painter.ALIGN_LEFT, WHITE);
        p.text("PUNTI " + g.score, sw / 2, base, ts, Painter.ALIGN_CENTER, WHITE);
        p.text("VITE " + g.lives, sw - m, base, ts, Painter.ALIGN_RIGHT, WHITE);

        // ---- pulsanti
        float bs = Controls.size(sw, sh), by = Controls.btnY(sw, sh);
        p.image(Img.BTN_LEFT, Controls.leftX(sw, sh), by, bs, bs, false, 1);
        p.image(Img.BTN_RIGHT, Controls.rightX(sw, sh), by, bs, bs, false, 1);
        p.image(Img.BTN_JUMP, Controls.jumpX(sw, sh), by, bs, bs, false, 1);

        // ---- fine partita
        if (g.state == Game.GAME_OVER || (g.state == Game.WON && g.stateTime > 1.4f)) {
            p.rect(0, 0, sw, sh, 0x99000000);
            String title = g.state == Game.WON ? "LIVELLO COMPLETATO!" : "PARTITA FINITA";
            p.text(title, sw / 2, 5.0f * s, 1.1f * s, Painter.ALIGN_CENTER, WHITE);
            p.text("Punti: " + g.score + "   Monete: " + g.coinCount, sw / 2, 6.4f * s, 0.6f * s, Painter.ALIGN_CENTER, WHITE);
            if (g.stateTime > 1.2f) {
                p.text("Tocca lo schermo per rigiocare", sw / 2, 7.6f * s, 0.5f * s, Painter.ALIGN_CENTER, 0xFFFFE082);
            }
        }
    }

    /** Striscia di sfondo ripetuta in orizzontale, spostata di scroll pixel. */
    private static void layer(Painter p, int img, float scroll, float y, float h, float sw) {
        float w = (float) Math.floor(h * p.aspect(img));
        float x = -(scroll % w);
        for (; x < sw; x += w) {
            p.image(img, (float) Math.floor(x), y, w + 1, h, false, 1);
        }
    }
}
