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
        int th = lv.theme;                          // 0 estate, 1 autunno, 2 aurora boreale, 3 incendio, 4 primavera, 5 cielo
        boolean au = th >= 1;
        layer(p, th == 5 ? Img.HV_SKY : th == 4 ? Img.SP_SKY : th == 3 ? Img.FR_SKY : th == 2 ? Img.AR_SKY : th == 1 ? Img.AU_SKY : Img.SKY, cam * s * 0.03f, 0, sh, sw);
        if (th == 5) p.image(Img.HV_SUN, sw * 0.17f - 3.4f * s, 0.2f * s, 6.8f * s, 6.8f * s, false, 1f);       // il sole sta fermo nel cielo
        layer(p, th == 5 ? Img.HV_MOUNTAINS : th == 4 ? Img.SP_MOUNTAINS : th == 3 ? Img.FR_MOUNTAINS : th == 2 ? Img.AR_MOUNTAINS : th == 1 ? Img.AU_MOUNTAINS : Img.MOUNTAINS, cam * s * 0.12f, 3.9f * s, 6.6f * s, sw);
        layer(p, th == 5 ? Img.HV_HILLS : th == 4 ? Img.SP_HILLS : th == 3 ? Img.FR_HILLS : th == 2 ? Img.AR_HILLS : th == 1 ? Img.AU_HILLS : Img.HILLS, cam * s * 0.35f, 6.2f * s, 5.8f * s, sw);

        // ---- fiume: l'acqua scorre sotto ai burroni (le caselle del terreno la coprono altrove)
        if (th == 4 || th == 5) {
            float wh = (th == 5 ? 2.2f : 1.6f) * s, ww = wh * 256f / 96f, wy = (th == 5 ? 10.2f : 10.45f) * s;
            for (int k = 0; k < 2; k++) {
                float drift = (g.time * (0.35f + 0.25f * k) * s + cam * s * (1f - 0.15f * k) + k * 0.4f * ww) % ww;
                for (float x = -drift - ww; x < sw + ww; x += ww) p.image(th == 5 ? Img.HV_SEA : Img.SP_WATER, x, wy + k * 0.12f * s, ww + 1, wh, k == 1, k == 0 ? 1f : 0.55f);
            }
        }

        // ---- traguardo: portone di pietra (dietro alle caselle); i battenti si aprono quando il livello è finito
        float gx = (lv.flagX + 0.5f - 1.5f - cam) * s, gy = (lv.flagBaseY - 4f) * s;
        if (gx > -3.5f * s && gx < sw + 0.5f * s) {
            p.image(Img.GATE, gx, gy, 3f * s, 4f * s, false, 1);
            float t = Math.max(0f, Math.min(1f, g.gateOpen));
            float lw = 0.95f * s * (1f - 0.88f * t * t * (3f - 2f * t)), ly = (lv.flagBaseY - 3.05f) * s;
            p.image(Img.GATE_DOOR, gx + 0.55f * s, ly, lw, 3.05f * s, false, 1);
            p.image(Img.GATE_DOOR, gx + 2.45f * s - lw, ly, lw, 3.05f * s, true, 1);
        }

        // ---- catapulta vicino al portone: con la trottola riporta all'inizio del livello
        float kx = (lv.catX - cam) * s;
        if (kx > -4f * s && kx < sw + s) {
            boolean shot = g.launching() && g.launchT < 0.6f;
            p.image(shot ? Img.FX_CATAPULT_B : Img.FX_CATAPULT, kx, (lv.flagBaseY - 2.05f) * s, 3f * s, 2.05f * s, false, 1);
        }

        // ---- caselle
        int tx0 = (int) Math.floor(cam), tx1 = (int) Math.floor(cam + g.viewW) + 1;
        for (int ty = 0; ty < lv.h; ty++) {
            for (int tx = tx0; tx <= tx1; tx++) {
                if (tx < 0 || tx >= lv.w) continue;
                char c = lv.get(tx, ty);
                if (c == Level.EMPTY || c == Level.DOOR) continue;
                int img;
                boolean softEdge = false;
                switch (c) {
                    case Level.GROUND:
                        boolean under = lv.get(tx, ty - 1) == Level.GROUND;
                        if (au) {      // d'autunno gli spigoli del terreno sono arrotondati dove finisce il burrone o la collina
                            boolean el = !lv.solid(tx - 1, ty), er = !lv.solid(tx + 1, ty);
                            img = under ? (el ? Img.AU_DIRT_L : er ? Img.AU_DIRT_R : Img.AU_DIRT)
                                    : (el ? Img.AU_GRASS_L : er ? Img.AU_GRASS_R : Img.AU_GRASS);
                            if (th == 2) img = img == Img.AU_DIRT_L ? Img.AR_DIRT_L : img == Img.AU_DIRT_R ? Img.AR_DIRT_R : img == Img.AU_DIRT ? Img.AR_DIRT
                                    : img == Img.AU_GRASS_L ? Img.AR_GRASS_L : img == Img.AU_GRASS_R ? Img.AR_GRASS_R : Img.AR_GRASS;
                            if (th == 3) img = img == Img.AU_DIRT_L ? Img.FR_DIRT_L : img == Img.AU_DIRT_R ? Img.FR_DIRT_R : img == Img.AU_DIRT ? Img.FR_DIRT
                                    : img == Img.AU_GRASS_L ? Img.FR_GRASS_L : img == Img.AU_GRASS_R ? Img.FR_GRASS_R : Img.FR_GRASS;
                            if (th == 4) img = img == Img.AU_DIRT_L ? Img.SP_DIRT_L : img == Img.AU_DIRT_R ? Img.SP_DIRT_R : img == Img.AU_DIRT ? Img.SP_DIRT
                                    : img == Img.AU_GRASS_L ? Img.SP_GRASS_L : img == Img.AU_GRASS_R ? Img.SP_GRASS_R : Img.SP_GRASS;
                            if (th == 5) img = img == Img.AU_DIRT_L ? Img.HV_DIRT_L : img == Img.AU_DIRT_R ? Img.HV_DIRT_R : img == Img.AU_DIRT ? Img.HV_DIRT
                                    : img == Img.AU_GRASS_L ? Img.HV_GRASS_L : img == Img.AU_GRASS_R ? Img.HV_GRASS_R : Img.HV_GRASS;
                            softEdge = el || er;
                        } else {
                            img = under ? Img.DIRT : Img.GRASS;
                        }
                        break;
                    case Level.BRICK:
                    case Level.SECRET: img = th == 5 ? Img.HV_BRICK : th == 4 ? Img.SP_BRICK : th == 3 ? Img.FR_BRICK : th == 2 ? Img.AR_BRICK : au ? Img.AU_BRICK : Img.BRICK; break;
                    case Level.CRUMBLE: img = th == 5 ? Img.HV_CRUMBLE : Img.FR_CRUMBLE; break;
                    case Level.SPRING: img = th == 5 ? Img.HV_SPRING : Img.SP_SPRING; break;
                    case Level.BONUS: img = th == 5 ? Img.HV_BONUS : th == 4 ? Img.SP_BONUS : th == 3 ? Img.FR_BONUS : th == 2 ? Img.AR_BONUS : au ? Img.AU_BONUS : Img.BONUS; break;
                    case Level.USED: img = th == 5 ? Img.HV_USED : th == 4 ? Img.SP_USED : th == 3 ? Img.FR_USED : th == 2 ? Img.AR_USED : au ? Img.AU_USED : Img.USED; break;
                    default: img = th == 5 ? Img.HV_STONE : th == 4 ? Img.SP_STONE : th == 3 ? Img.FR_STONE : th == 2 ? Img.AR_STONE : au ? Img.AU_STONE : Img.STONE; break;
                }
                float off = 0;
                if (tx == g.bumpTx && ty == g.bumpTy) {
                    off = -(float) Math.sin(Math.PI * g.bumpAge / Game.BUMP_TIME) * 0.3f * s;
                }
                float x0 = Math.round((tx - cam) * s), x1 = Math.round((tx + 1 - cam) * s);
                float y0 = Math.round(ty * s), y1 = Math.round((ty + 1) * s);
                float ca = 1f, cj = 0f;
                if (c == Level.CRUMBLE) {                  // sta per cedere: trema e svanisce
                    float age = g.crumbleAge(tx, ty);
                    if (age >= 0) {
                        cj = (float) Math.sin(g.time * 90f) * 0.05f * s;
                        ca = 1f - 0.5f * age / Game.CRUMBLE_DELAY;
                    }
                }
                if (c == Level.SPRING && tx == g.springTx && ty == g.springTy && g.springAge < 0.32f) {      // il fiore si schiaccia e rimbalza
                    float k = (float) Math.sin(Math.PI * g.springAge / 0.32f);
                    float hh = (y1 - y0) * (1f - 0.3f * k), ww2 = (x1 - x0) * (1f + 0.12f * k);
                    p.image(img, x0 - (ww2 - (x1 - x0)) / 2, y1 - hh, ww2, hh, false, 1);
                    continue;
                }
                p.image(img, x0 + cj, y0 + off, x1 - x0, y1 - y0, false, ca);
                if (img == Img.GRASS && !lv.solid(tx, ty - 1)) {
                    p.image(Img.FRINGE, x0, y0 - 0.27f * s, x1 - x0, 0.29f * s, false, 1);
                } else if ((img == Img.AU_GRASS || img == Img.AR_GRASS || img == Img.FR_GRASS || img == Img.SP_GRASS || img == Img.HV_GRASS) && !lv.solid(tx, ty - 1)) {
                    p.image(th == 5 ? Img.HV_FRINGE : th == 4 ? Img.SP_FRINGE : th == 3 ? Img.FR_FRINGE : th == 2 ? Img.AR_FRINGE : Img.AU_FRINGE, x0, y0 - 0.27f * s, x1 - x0, 0.29f * s, false, 1);
                }
            }
        }

        // ---- braci che salgono nel livello dell'incendio
        if (th == 3) {
            for (int i = 0; i < 46; i++) {
                float ph = (g.time * (0.07f + 0.05f * (i % 5)) + i * 0.137f) % 1f;
                float ex = ((i * 97 % 211) / 211f * (g.viewW + 6f) - cam * 0.6f % (g.viewW + 6f) + (g.viewW + 6f)) % (g.viewW + 6f) - 1f;
                float ey = (1f - ph) * 12.5f - 0.3f;
                float sz = (0.06f + 0.04f * (i % 3)) * s;
                p.rect((ex + 0.35f * (float) Math.sin(g.time * 1.3f + i)) * s, ey * s, sz, sz, i % 2 == 0 ? 0xFFFFA030 : 0xFFFF5A18);
            }
        }

        // ---- rovi
        for (float[] t : lv.thorns) {
            float tx2 = (t[0] - cam) * s;
            if (tx2 < -2 * s || tx2 > sw + s) continue;
            p.image(Img.SP_THORN, tx2 - 0.12f * s, (t[1] + 1f) * s - 1.0f * s, 1.24f * s, 1.0f * s, false, 1);
        }

        // ---- petali (primavera) o scintille dorate (cielo) che cadono
        if (th == 4 || th == 5) {
            for (int i = 0; i < 38; i++) {
                float ph = (g.time * (0.05f + 0.03f * (i % 5)) + i * 0.173f) % 1f;
                float ex = ((i * 89 % 197) / 197f * (g.viewW + 8f) - cam * 0.5f % (g.viewW + 8f) + (g.viewW + 8f)) % (g.viewW + 8f) - 1f + 1.6f * ph;
                float ey = ph * 12.4f - 0.3f;
                float sz = (0.16f + 0.07f * (i % 3)) * s;
                p.image(th == 5 ? Img.HV_SPARK : Img.SP_PETAL, (ex + 0.4f * (float) Math.sin(g.time * 1.4f + i)) * s, ey * s, sz * (0.55f + 0.45f * (float) Math.abs(Math.sin(g.time * 2f + i))), sz, i % 2 == 0, 0.9f);
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
            int fr = (int) (e.anim * (e.type == Enemy.CROW ? 9f : e.type == Enemy.WHIRL ? 12f : 7f)) % 2;
            int img;
            float w = 1.083f * s, h = 0.875f * s;
            switch (e.type) {
                case Enemy.HOG: img = Img.HOG + fr; w = 1.25f * s; h = 0.85f * s; break;
                case Enemy.CROW: img = Img.CROW + fr; w = 1.25f * s; h = 1.0f * s; break;
                case Enemy.STORM:
                    img = Game.stormPhase(e.hop) > 0 && e.flatTime <= 0 ? Img.HV_STORM_C : Img.HV_STORM;
                    w = 1.7f * s; h = 1.36f * s;
                    break;
                case Enemy.WHIRL: img = Img.HV_WHIRL + fr; w = 1.2f * s; h = 1.38f * s; break;
                case Enemy.SNAIL: img = e.mode == 1 ? Img.FX_SHELL : Img.FX_SNAIL; w = (e.mode == 1 ? 0.78f : 1.2f) * s; h = (e.mode == 1 ? 0.65f : 0.9f) * s; break;
                case Enemy.CHESTNUT: img = Img.FX_CHESTNUT; w = 1.05f * s; h = 1.05f * s; if (e.mode == 1) x += 0.06f * s * (float) Math.sin(g.time * 45f); break;
                case Enemy.OWL: img = Img.FX_OWL_0 + (e.mode == 1 ? 0 : fr); w = 1.5f * s; h = 1.3f * s; break;
                case Enemy.PENGUIN: img = Img.FX_PENGUIN; w = 1.25f * s; h = 1.0f * s; break;
                case Enemy.SNOWMAN: img = Img.FX_SNOWMAN; w = 1.05f * s; h = 1.5f * s; break;
                case Enemy.LAPILLO: img = Img.FX_LAPILLO; w = 0.9f * s; h = 1.2f * s; break;
                case Enemy.SALAMANDER: img = e.mode > 0 ? Img.FX_SALA_B : Img.FX_SALA; w = 1.6f * s; h = 0.93f * s; break;
                case Enemy.FROG: img = e.onGround ? Img.FX_FROG : Img.FX_FROG_J; w = 1.1f * s; h = (e.onGround ? 0.8f : 1.1f) * s; break;
                case Enemy.PLANT:
                    img = e.mode == 2 && e.flatTime <= 0 ? Img.FX_PLANT_O : Img.FX_PLANT; w = 1.0f * s; h = (img == Img.FX_PLANT_O ? 1.92f : 1.0f) * s;
                    if (e.mode == 1) x += 0.05f * s * (float) Math.sin(g.time * 50f);
                    break;
                case Enemy.STAR: img = Img.FX_STAR; w = 1.5f * s; h = 1.5f * s; break;
                case Enemy.SUNNY: img = Img.FX_SUNNY; w = 1.25f * s; h = 1.25f * s; break;
                case Enemy.ACORN:
                    img = e.flatTime > 0 ? Img.ACORN_FLAT : Img.ACORN + (e.onGround ? 0 : 1);
                    w = 0.95f * s; h = 1.05f * s;
                    break;
                default: img = e.flatTime > 0 ? Img.ENEMY_FLAT : Img.ENEMY + fr; break;
            }
            if ((e.type == Enemy.CROW || e.type == Enemy.STORM || e.type >= Enemy.MOLE) && e.flatTime > 0) h *= 0.35f;      // schiacciato
            if (e.type == Enemy.LAPILLO && e.mode == 0) continue;                                    // dentro la lava non si vede
            if (e.type == Enemy.SALAMANDER && e.mode == 2 && e.flatTime <= 0) {                      // la fiammata davanti al muso
                float fx0 = e.dir > 0 ? e.x + e.w : e.x - 2.2f;
                p.image(Img.FX_BREATH, (fx0 - cam) * s, (e.y - 0.25f) * s, 2.2f * s, 0.8f * s, e.dir > 0, 0.85f + 0.15f * (float) Math.sin(g.time * 40f));
            }
            if (e.type == Enemy.STORM && e.flatTime <= 0 && Game.stormPhase(e.hop) > 0) {            // fulmine: prima un avviso tremolante, poi la scarica
                float bx = (e.x + e.w / 2 - cam) * s, by = (e.y + e.h) * s, bot = g.boltBottom(e) * s;
                boolean strike = Game.stormPhase(e.hop) == 2;
                float a = strike ? 1f : 0.2f + 0.2f * (float) Math.sin(g.time * 40f);
                float bw = strike ? 0.9f * s : 0.5f * s;
                p.image(Img.HV_BOLT, bx - bw / 2, by - 0.1f * s, bw, bot - by + 0.1f * s, ((int) (g.time * 30f) & 1) == 0, Math.max(0.05f, a));
            }
            p.image(img, x - w / 2, (e.y + e.h + 0.03f) * s - h, w, h, e.dir > 0, 1);
        }

        for (float[] q : g.shots) p.image(Img.FX_SNOWBALL, (q[0] - cam - 0.25f) * s, (q[1] - 0.25f) * s, 0.5f * s, 0.5f * s, false, 1);

        // ---- getti di fiamma a tempo e muro di fuoco
        for (float[] j : lv.jets) {
            float jx = (j[0] - cam) * s;
            if (jx < -2 * s || jx > sw + s) continue;
            float pw = Game.jetPower(j, g.time), warn = Game.jetWarn(j, g.time);
            float pulse = 0.5f + 0.5f * (float) Math.sin(g.time * 4f + j[0] * 1.7f);
            p.image(Img.R3C_EMBER, jx - 1.0f * s, j[1] * s - 1.15f * s, 2.6f * s, 1.7f * s, false, 0.3f + 0.25f * pulse + 0.4f * Math.max(pw, warn));   // alone rosso: si nota da lontano
            p.image(Img.FR_VENT, jx - 0.25f * s, j[1] * s - 0.52f * s, 1.5f * s, 0.62f * s, false, 1);      // la bocca si vede sempre
            if (pw > 0) {
                float hgt = 3.0f * s * (0.45f + 0.55f * pw);
                p.image(Img.R3C_FLAME, jx - 0.45f * s, j[1] * s - hgt, 1.9f * s, hgt, false, 0.95f);
            } else {
                float k = warn > 0 ? 0.35f + 0.9f * warn : 0.3f;           // spento: fiammelle basse sempre accese, più alte nell'avviso
                float hgt = k * s * (0.8f + 0.2f * (float) Math.sin(g.time * 22f + j[0]));
                p.image(Img.R3C_FLAME, jx + 0.0f * s, j[1] * s - hgt - 0.05f * s, 0.55f * s, hgt, false, 0.8f);
                p.image(Img.R3C_FLAME, jx + 0.45f * s, j[1] * s - hgt * 0.8f - 0.05f * s, 0.55f * s, hgt * 0.8f, true, 0.8f);
            }
        }
        if (!Float.isNaN(g.fireX)) {
            float dgap = g.player.x - g.fireX;
            if (dgap < 16f) {                           // il fuoco è vicino: bagliore rosso sul bordo sinistro
                float k = (float) Math.pow(1f - Math.max(0f, dgap) / 16f, 1.5f);
                for (int i = 0; i < 8; i++) p.rect(i * 0.18f * s, 0, 0.18f * s, sh, ((int) (k * (150 - i * 17)) << 24) | 0xFF4A10);
            }
            float fx = (g.fireX - cam) * s;
            if (fx > -4 * s) {
                p.rect(Math.min(0f, fx - 6 * s), 0, Math.max(fx + 0.4f * s, 0f) - Math.min(0f, fx - 6 * s), sh, 0xC8A01806);
                for (int i = 0; i < 9; i++) {
                    float fl = 0.85f + 0.3f * (float) Math.sin(g.time * (6f + i) + i * 1.9f);
                    float fh = 5.2f * s * fl, fwid = 2.8f * s;
                    float fy = sh - fh + (i % 3) * 0.2f * s + 0.8f * s;
                    p.image(Img.R3C_FLAME, fx - 1.8f * s + (i % 2) * 0.5f * s, Math.max(-2f * s, fy - (i / 3) * 2.2f * s), fwid, fh, i % 2 == 1, 0.95f);
                }
                p.image(Img.R3C_FLAME, fx - 2.4f * s, 0, 3.2f * s, sh * 0.55f, false, 0.7f);
            }
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
        if (th == 5) for (float bxx : new float[] {Controls.leftX(sw, sh), Controls.rightX(sw, sh), Controls.jumpX(sw, sh), Controls.spinX(sw, sh)}) {
            p.image(Img.HV_PAD, bxx - 0.1f * bs, bty - 0.1f * bs, 1.2f * bs, 1.2f * bs, false, 1);       // fondo scuro: sulle nuvole i comandi bianchi non si vedrebbero
        }
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
                p.text(won ? Levels.wonText(g.levelNo) : "Tocca lo schermo per riprovare", sw / 2, 9.4f * s, 0.5f * s, Painter.ALIGN_CENTER, 0xFFFFE082);
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
