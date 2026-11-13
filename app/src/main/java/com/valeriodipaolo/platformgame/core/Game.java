package com.valeriodipaolo.platformgame.core;

import java.util.ArrayList;
import java.util.List;

/** Stato e regole del gioco. Unità: caselle e secondi. */
public final class Game {
    public static final int PLAYING = 0, DYING = 1, WON = 2, GAME_OVER = 3;

    private static final float STEP = 1f / 120f;
    // Comandi: accelerazione e frenata molto rapide, così il personaggio parte e si ferma quando lo vuoi tu.
    private static final float MAX_SPEED = 8f, ACCEL = 110f, AIR_ACCEL = 80f, FRICTION = 110f, AIR_FRICTION = 30f;
    // Salto: tenendo premuto sale a circa 5 caselle, con un tocco breve a circa 2,5.
    private static final float JUMP_V = 19.5f, GRAVITY = 75f, GRAVITY_HOLD = 38f, MAX_FALL = 22f;
    public static final int MAX_HP = 3;
    private static final float INVULN = 2f;
    private static final float ENEMY_SPEED = 2.0f;
    public static final float BUMP_TIME = 0.18f;

    public Level level;
    public final Body player = new Body();
    public final List<Enemy> enemies = new ArrayList<Enemy>();
    /** Monete ancora da raccogliere: {x, y} della casella. */
    public final List<float[]> coins = new ArrayList<float[]>();
    /** Monete che schizzano fuori dalle casse: {x, y, vy, età}. */
    public final List<float[]> pops = new ArrayList<float[]>();
    public int bumpTx = -1, bumpTy = -1;
    public float bumpAge;

    public int state, coinCount, score, lives, hp;
    /** Secondi di invulnerabilità rimasti dopo un colpo (o dopo la ricomparsa). */
    public float invuln;
    private float checkpointX;
    public float stateTime, time, camX, runPhase;
    public float viewW = 20;
    public int facing = 1;

    private boolean left, right, jump, jumpWas;
    private float jumpBuffer, coyote, acc;

    public Game() {
        newGame();
    }

    public void newGame() {
        score = 0;
        coinCount = 0;
        lives = 3;
        loadLevel();
    }

    private void loadLevel() {
        level = LevelOne.build();
        enemies.clear();
        for (float[] e : level.enemies) enemies.add(new Enemy(e[0], e[1]));
        coins.clear();
        for (float[] c : level.coins) coins.add(new float[] {c[0], c[1]});
        pops.clear();
        bumpTx = bumpTy = -1;
        player.w = 0.62f;
        player.h = 1.2f;
        checkpointX = level.startX;
        place(level.startX);
        hp = MAX_HP;
        invuln = 0;
    }

    private void place(float x) {
        player.x = x;
        player.y = level.startY;
        player.vx = player.vy = 0;
        player.onGround = false;
        facing = 1;
        state = PLAYING;
        stateTime = 0;
        jumpBuffer = coyote = 0;
        camX = Math.max(0, Math.min(x - viewW * 0.4f, level.w - viewW));
    }

    /** Dopo aver perso una vita si riparte dall'ultimo punto sicuro, tenendo monete e nemici già eliminati. */
    private void respawn() {
        hp = MAX_HP;
        place(checkpointX);
        invuln = 2.5f;
    }

    public void setInput(boolean left, boolean right, boolean jump) {
        this.left = left;
        this.right = right;
        this.jump = jump;
    }

    /** Tocco sullo schermo: serve a ricominciare a fine partita. */
    public void tap() {
        if ((state == WON || state == GAME_OVER) && stateTime > 1.2f) newGame();
    }

    public void update(float dt) {
        acc += Math.min(dt, 0.05f);
        while (acc >= STEP) {
            step(STEP);
            acc -= STEP;
        }
    }

    private void step(float dt) {
        time += dt;
        stateTime += dt;
        if (bumpTx >= 0) {
            bumpAge += dt;
            if (bumpAge >= BUMP_TIME) bumpTx = bumpTy = -1;
        }
        for (int i = pops.size() - 1; i >= 0; i--) {
            float[] p = pops.get(i);
            p[2] += 40f * dt;
            p[1] += p[2] * dt;
            p[3] += dt;
            if (p[3] > 0.5f) pops.remove(i);
        }

        if (invuln > 0) invuln -= dt;
        if (state == PLAYING) {
            stepPlayer(dt);
            updateCheckpoint();
            stepEnemies(dt);
            pickCoins();
            if (state == PLAYING && player.x + player.w >= level.flagX + 0.45f) win();
        } else if (state == DYING) {
            player.vy += 60f * dt;
            player.y += player.vy * dt;
            stepEnemies(dt);
            if (stateTime > 1.8f) {
                if (lives > 0) {
                    respawn();
                } else {
                    state = GAME_OVER;
                    stateTime = 0;
                }
            }
        } else if (state == WON) {
            float target = level.flagBaseY - player.h;
            if (player.y < target) player.y = Math.min(target, player.y + 6f * dt);
        }
        jumpWas = jump;

        float target = player.x + player.w / 2 - viewW * 0.4f;
        camX += (target - camX) * Math.min(1f, 10f * dt);
        camX = Math.max(0, Math.min(camX, level.w - viewW));
    }

    private void stepPlayer(float dt) {
        Body p = player;
        float dir = (right ? 1 : 0) - (left ? 1 : 0);
        if (dir != 0) {
            float a = p.onGround ? ACCEL : AIR_ACCEL;
            if (dir * p.vx < 0) a *= 2;                    // inversione più reattiva
            p.vx += dir * a * dt;
            facing = dir > 0 ? 1 : -1;
        } else {
            float f = (p.onGround ? FRICTION : AIR_FRICTION) * dt;
            if (Math.abs(p.vx) <= f) p.vx = 0; else p.vx -= Math.signum(p.vx) * f;
        }
        p.vx = Math.max(-MAX_SPEED, Math.min(MAX_SPEED, p.vx));

        // salto: un po' di tolleranza se si preme poco prima di toccare terra o poco dopo il bordo
        jumpBuffer = (jump && !jumpWas) ? 0.2f : jumpBuffer - dt;
        coyote = p.onGround ? 0.14f : coyote - dt;
        if (jumpBuffer > 0 && coyote > 0) {
            p.vy = -JUMP_V;
            jumpBuffer = coyote = 0;
        }
        // tenendo premuto si sale più in alto
        p.vy = Math.min(MAX_FALL, p.vy + ((p.vy < 0 && jump) ? GRAVITY_HOLD : GRAVITY) * dt);

        p.move(level, dt);
        if (p.headTx >= 0) hitBlock(p.headTx, p.headTy);
        if (p.onGround && Math.abs(p.vx) > 0.3f) runPhase += Math.abs(p.vx) * dt * 1.6f;
        if (p.y > level.h + 1) die();
    }

    /** Ricorda l'ultimo punto di terreno solido e largo, lontano dai burroni, da cui ripartire. */
    private void updateCheckpoint() {
        Body p = player;
        if (!p.onGround || p.x <= checkpointX + 2f) return;
        int fx = (int) Math.floor(p.x + p.w / 2), row = (int) Math.floor(p.y + p.h + 0.01f);
        for (int dx = -2; dx <= 2; dx++) {
            if (!level.solid(fx + dx, row) || level.solid(fx + dx, row - 1)) return;
        }
        checkpointX = p.x;
    }

    /** Colpo subito da un nemico: toglie un punto vita; a zero si perde una vita. */
    private void hurt() {
        if (invuln > 0 || state != PLAYING) return;
        hp--;
        if (hp <= 0) {
            die();
        } else {
            invuln = INVULN;
            player.vy = -10f;
            player.vx = -facing * 5f;
        }
    }

    private void hitBlock(int tx, int ty) {
        char c = level.get(tx, ty);
        if (c != Level.BONUS && c != Level.BRICK) return;
        bumpTx = tx;
        bumpTy = ty;
        bumpAge = 0;
        if (c == Level.BONUS) {
            level.set(tx, ty, Level.USED);
            coinCount++;
            score += 200;
            pops.add(new float[] {tx, ty - 1, -13f, 0});
        }
        for (Enemy e : enemies) {                          // chi sta sopra la casella colpita vola via
            if (e.dead || e.flung || e.flatTime > 0) continue;
            if (Math.abs(e.y + e.h - ty) < 0.1f && e.x + e.w > tx && e.x < tx + 1) {
                e.flung = true;
                e.vy = -11f;
                e.vx = e.dir * 2f;
                score += 100;
            }
        }
    }

    private void stepEnemies(float dt) {
        for (Enemy e : enemies) {
            if (e.dead) continue;
            if (e.flatTime > 0) {
                e.flatTime -= dt;
                if (e.flatTime <= 0) e.dead = true;
                continue;
            }
            if (e.flung) {
                e.vy += GRAVITY * dt;
                e.x += e.vx * dt;
                e.y += e.vy * dt;
                if (e.y > level.h + 2) e.dead = true;
                continue;
            }
            if (!e.active) {
                if (e.x < camX + viewW + 1.5f) e.active = true; else continue;
            }
            e.vx = e.dir * ENEMY_SPEED;
            e.vy = Math.min(MAX_FALL, e.vy + GRAVITY * dt);
            e.move(level, dt);
            if (e.hitWall) e.dir = -e.dir;
            e.anim += dt;
            if (e.y > level.h + 2) { e.dead = true; continue; }

            if (state == PLAYING && player.overlaps(e)) {
                if (player.vy > 0 && player.y + player.h - e.y < 0.75f) {
                    e.flatTime = 0.45f;
                    player.vy = jump ? -15f : -10f;
                    score += 100;
                } else {
                    hurt();
                }
            }
        }
    }

    private void pickCoins() {
        Body p = player;
        for (int i = coins.size() - 1; i >= 0; i--) {
            float[] c = coins.get(i);
            if (p.x < c[0] + 0.8f && p.x + p.w > c[0] + 0.2f && p.y < c[1] + 0.8f && p.y + p.h > c[1] + 0.2f) {
                coins.remove(i);
                coinCount++;
                score += 50;
            }
        }
    }

    private void die() {
        if (state != PLAYING) return;
        state = DYING;
        stateTime = 0;
        hp = 0;
        lives--;
        player.vx = 0;
        player.vy = -15f;
    }

    private void win() {
        state = WON;
        stateTime = 0;
        float height = Math.max(0, level.flagBaseY - (player.y + player.h));
        score += 500 + (int) height * 200;
        player.vx = player.vy = 0;
        player.x = level.flagX + 0.45f - player.w;
        facing = 1;
    }
}
