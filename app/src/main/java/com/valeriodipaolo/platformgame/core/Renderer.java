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
                    case Level.BRICK:
                    case Level.SECRET: img = Img.BRICK; break;
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

        // ---- pezzi di gemma e cella energetica
        int frame = (int) (g.time * 9f) % 6;
        for (float[] c : g.shards) {
            float x = (c[0] - cam) * s;
            if (x < -s || x > sw) continue;
            float bob = (float) Math.sin(g.time * 3f + c[0]) * 0.06f;
            p.image(Img.GEM + (frame + (int) c[0]) % 6, x + 0.14f * s, (c[1] + 0.1f + bob) * s, 0.72f * s, 0.72f * s, false, 1);
        }
        if (g.battery != null) {
            float x = (g.battery[0] - cam) * s;
            if (x > -s && x < sw) {
                float bob = (float) Math.sin(g.time * 3f) * 0.07f;
                p.image(Img.BATTERY, x, (g.battery[1] + bob) * s, 0.9f * s, 0.9f * s, false, 1);
            }
        }
        for (float[] c : g.pops) {
            float a = Math.min(1f, ((c[4] == 1 ? 0.8f : 0.5f) - c[3]) * 6f);
            if (c[4] == 1) {
                p.image(Img.CHIP, (c[0] - cam + 0.05f) * s, (c[1] + 0.05f) * s, 0.9f * s, 0.9f * s, false, a);
            } else {
                p.image(Img.GEM + (int) (c[3] * 24f) % 6, (c[0] - cam + 0.14f) * s, (c[1] + 0.1f) * s, 0.72f * s, 0.72f * s, false, a);
            }
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
        float cx = (pl.x + pl.w / 2 - cam) * s, by = (pl.y + pl.h + 0.03f) * s;
        float al = (g.state == Game.PLAYING && g.invuln > 0 && ((int) (g.time * 14f) % 2 == 0)) ? 0.35f : 1f;
        boolean flip = g.facing < 0;
        if (g.spinning()) {                         // giravolta: il robot ruota attorno al proprio asse
            float ang = g.spinAngle + (g.facing < 0 ? (float) Math.PI : 0f);
            int pose = (int) Math.floor(ang / (2 * Math.PI) * Img.SPIN_POSES) % Img.SPIN_POSES;
            img = Img.HERO_SPIN + pose;
            flip = false;
            al = 1f;
        }
        p.image(img, cx - hw / 2, by - hh, hw, hh, flip, al);
        if (g.spinning()) {
            float k = 1.9f * s * (1f + 0.08f * (float) Math.sin(g.time * 40f));
            p.image(Img.SPIN_FX, cx - k / 2, by - 0.55f * s - k / 2, k, k, false, 0.8f);
        }
        if (g.djTime > 0) {                         // anello sotto i piedi dopo il doppio salto
            float f = g.djTime / 0.3f, w = (2.2f - 1.2f * f) * s, h = 0.55f * s;
            p.image(Img.SPIN_FX, (g.djX - cam) * s - w / 2, g.djY * s - h / 2, w, h, false, f * 0.9f);
        }

        // ---- punteggio e collezionabili
        float m = 0.06f * sw, ts = 0.5f * s, base = 0.95f * s;
        p.image(Img.GEM + 3, m, 0.34f * s, 0.78f * s, 0.78f * s, false, 1);
        p.text(g.gems + "/" + g.gemTotal, m + 0.9f * s, base, ts, Painter.ALIGN_LEFT, WHITE);
        float ix = m, iy = 1.3f * s, isz = 0.85f * s;
        p.image(Img.BATTERY, ix, iy, isz, isz, false, g.batteryOwned ? 1f : 0.4f);
        ix += isz * 1.25f;
        if (g.secretOwned) {
            p.image(Img.CHIP, ix, iy, isz, isz, false, 1);
            ix += isz * 1.25f;
        }
        p.image(Img.GEM_FULL, ix, iy, isz, isz, false, g.gemOwned ? 1f : 0.4f);
        p.text("PUNTI " + g.score, sw / 2, base, ts, Painter.ALIGN_CENTER, WHITE);
        p.text("VITE " + g.lives, sw - m, base, ts, Painter.ALIGN_RIGHT, WHITE);
        float pip = 0.36f * s, gap = 0.12f * s, py = base + 0.22f * s;
        for (int i = 0; i < Game.MAX_HP; i++) {
            float px = sw - m - (Game.MAX_HP - i) * pip - (Game.MAX_HP - 1 - i) * gap;
            p.rect(px - 0.04f * s, py - 0.04f * s, pip + 0.08f * s, pip + 0.08f * s, 0xB0000000);
            p.rect(px, py, pip, pip, i < g.hp ? 0xFFE53935 : 0xFF4A4A4A);
        }
        if (g.toast != null && g.toastTime > 0) {
            int a = (int) (255 * Math.min(1f, g.toastTime * 2.5f));
            p.text(g.toast, sw / 2, 2.6f * s, 0.7f * s, Painter.ALIGN_CENTER, (a << 24) | 0xFFE082);
        }

        // ---- pulsanti
        float bs = Controls.size(sw, sh), bty = Controls.btnY(sw, sh);
        p.image(Img.BTN_LEFT, Controls.leftX(sw, sh), bty, bs, bs, false, 1);
        p.image(Img.BTN_RIGHT, Controls.rightX(sw, sh), bty, bs, bs, false, 1);
        p.image(Img.BTN_JUMP, Controls.jumpX(sw, sh), bty, bs, bs, false, 1);
        p.image(Img.BTN_SPIN, Controls.spinX(sw, sh), bty, bs, bs, false, g.spinning() || g.spinCool > 0 ? 0.45f : 1f);

        // ---- fine partita
        if (g.state == Game.GAME_OVER || (g.state == Game.WON && g.stateTime > 1.4f)) {
            p.rect(0, 0, sw, sh, 0x99000000);
            boolean won = g.state == Game.WON;
            p.text(won ? "LIVELLO COMPLETATO!" : "PARTITA FINITA", sw / 2, 3.3f * s, 1.1f * s, Painter.ALIGN_CENTER, WHITE);
            p.text("Punti: " + g.score + "   Gemme: " + g.gems + "/" + g.gemTotal, sw / 2, 4.6f * s, 0.6f * s, Painter.ALIGN_CENTER, WHITE);
            if (won) {
                float sz = 1.1f * s, step = 3.6f * s, x0 = sw / 2 - step, y0 = 5.3f * s;
                int[] icons = {Img.BATTERY, Img.CHIP, Img.GEM_FULL};
                boolean[] owned = {g.batteryOwned, g.secretOwned, g.gemOwned};
                String[] names = {"Cella", "Chip segreto", "Gemma " + g.gems + "/" + g.gemTotal};
                for (int i = 0; i < 3; i++) {
                    float ix2 = x0 + i * step;
                    p.image(icons[i], ix2 - sz / 2, y0, sz, sz, false, owned[i] ? 1f : 0.25f);
                    p.text(names[i], ix2, y0 + sz + 0.55f * s, 0.4f * s, Painter.ALIGN_CENTER, owned[i] ? WHITE : 0xFF9E9E9E);
                }
            }
            if (g.stateTime > 1.2f) {
                p.text("Tocca lo schermo per rigiocare", sw / 2, 9.4f * s, 0.5f * s, Painter.ALIGN_CENTER, 0xFFFFE082);
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
