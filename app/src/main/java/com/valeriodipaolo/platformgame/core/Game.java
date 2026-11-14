package com.valeriodipaolo.platformgame.core;

import java.util.ArrayList;
import java.util.List;

/** Stato e regole del gioco. Unità: caselle e secondi. */
public final class Game {
    public static final int PLAYING = 0, DYING = 1, WON = 2, GAME_OVER = 3;

    private static final float STEP = 1f / 120f;
    // Comandi: accelerazione e frenata molto rapide, così il personaggio parte e si ferma quando lo vuoi tu.
    private static final float MAX_SPEED = 8f, ACCEL = 110f, FRICTION = 110f;
    // In aria lo spostamento è molto più limitato: si conserva lo slancio e si corregge poco.
    private static final float AIR_ACCEL = 30f, AIR_FRICTION = 4f;
    // Salto: tenendo premuto sale a circa 5 caselle, con un tocco breve a circa 2,5.
    private static final float JUMP_V = 19.5f, GRAVITY = 75f, GRAVITY_HOLD = 38f, MAX_FALL = 22f;
    // Doppio salto: una seconda pressione mentre si sale; anch'esso più alto tenendo premuto.
    private static final float AIR_JUMP_V = 17.5f;
    // Planata: con la giravolta in aria si scende lentamente.
    private static final float GLIDE_FALL = 2.5f;
    public static final int MAX_HP = 3;
    private static final float INVULN = 2f;
    private static final float ENEMY_SPEED = 2.0f;
    public static final float BUMP_TIME = 0.18f;
    /**
     * Giravolta: un tocco dura almeno SPIN_MIN secondi, tenendo premuto continua fino a SPIN_MAX; poi serve
     * SPIN_COOLDOWN prima di rifarla. In aria fa planare.
     */
    public static final float SPIN_MIN = 0.4f, SPIN_MAX = 1.3f, SPIN_COOLDOWN = 0.4f;
    private static final float SPIN_SPEED = 1.35f, SPIN_REACH = 0.3f, SPIN_RATE = 19f;

    public final Save save;
    /** Numero del livello 2D in corso: 1 (prato) o 3 (autunno). */
    public final int levelNo;
    public Level level;
    public final Body player = new Body();
    public final List<Enemy> enemies = new ArrayList<Enemy>();
    /** Pezzi di gemma ancora da raccogliere: {x, y} della casella. */
    public final List<float[]> shards = new ArrayList<float[]>();
    /** Oggetti che schizzano fuori dalle casse: {x, y, vy, età, tipo (0 gemma, 1 chip)}. */
    public final List<float[]> pops = new ArrayList<float[]>();
    /** Cella energetica ancora sul terreno: {x, y}, oppure null se già presa in questa partita. */
    public float[] battery;
    public int bumpTx = -1, bumpTy = -1;
    public float bumpAge;

    public int state, score, lives, hp;
    /** Pezzi di gemma presi in questa partita, e quanti ce ne sono in tutto (monete sparse più casse bonus). */
    public int gems, gemTotal;
    /**
     * Collezionabili ottenuti in questa partita: si azzerano quando si ricomincia il livello (non quando si perde una vita
     * e si riparte dal punto sicuro). Il record di quelli mai ottenuti resta in {@link #save}.
     */
    public boolean batteryOwned, secretOwned, gemOwned;
    public String toast;
    public float toastTime;
    /** Secondi di invulnerabilità rimasti dopo un colpo (o dopo la ricomparsa). */
    public float invuln;
    public float stateTime, time, camX, runPhase, spinT, spinCool, spinAngle;
    /** Ultimo doppio salto: posizione dei piedi e secondi rimasti per l'effetto grafico. */
    public float djX, djY, djTime;
    public float viewW = 20;
    public int facing = 1;

    private boolean left, right, jump, jumpWas, spin, spinWas;
    private float jumpBuffer, coyote, acc, checkpointX;
    private boolean spinActive;
    private int airJumps;

    public Game() {
        this(new MemorySave(), 1);
    }

    public Game(Save save) {
        this(save, 1);
    }

    public Game(Save save, int levelNo) {
        this.save = save;
        this.levelNo = levelNo;
        newGame();
    }

    public void newGame() {
        score = 0;
        gems = 0;
        lives = 3;
        loadLevel();
    }

    private void loadLevel() {
        level = levelNo == 5 ? LevelFive.build() : levelNo == 3 ? LevelThree.build() : LevelOne.build();
        enemies.clear();
        for (float[] e : level.enemies) {
            enemies.add(e.length > 2 ? new Enemy(e[0], e[1], (int) e[2], e[3], e[4]) : new Enemy(e[0], e[1]));
        }
        shards.clear();
        for (float[] c : level.shards) shards.add(new float[] {c[0], c[1]});
        gemTotal = shards.size();
        for (int y = 0; y < level.h; y++) {
            for (int x = 0; x < level.w; x++) if (level.get(x, y) == Level.BONUS) gemTotal++;
        }
        battery = level.battery == null ? null : new float[] {level.battery[0], level.battery[1]};
        batteryOwned = secretOwned = gemOwned = false;      // ricominciando il livello si riparte da zero
        pops.clear();
        toast = null;
        toastTime = 0;
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
        spinActive = false;
        spinT = spinCool = 0;
        airJumps = 1;
        djTime = 0;
        camX = Math.max(0, Math.min(x - viewW * 0.4f, level.w - viewW));
    }

    /** Dopo aver perso una vita si riparte dall'ultimo punto sicuro, tenendo gemme e nemici già eliminati. */
    private void respawn() {
        hp = MAX_HP;
        place(checkpointX);
        invuln = 2.5f;
    }

    public void setInput(boolean left, boolean right, boolean jump) {
        setInput(left, right, jump, false);
    }

    public void setInput(boolean left, boolean right, boolean jump, boolean spin) {
        this.left = left;
        this.right = right;
        this.jump = jump;
        this.spin = spin;
    }

    /** Tocco sullo schermo: dopo la sconfitta ricomincia il livello (a livello completato decide chi ospita il gioco). */
    public void tap() {
        if (state == GAME_OVER && stateTime > 1.2f) newGame();
    }

    public boolean spinning() {
        return spinActive;
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
        if (toastTime > 0) toastTime -= dt;
        if (djTime > 0) djTime -= dt;
        if (invuln > 0) invuln -= dt;
        if (bumpTx >= 0) {
            bumpAge += dt;
            if (bumpAge >= BUMP_TIME) bumpTx = bumpTy = -1;
        }
        for (int i = pops.size() - 1; i >= 0; i--) {
            float[] p = pops.get(i);
            p[2] += 40f * dt;
            p[1] += p[2] * dt;
            p[3] += dt;
            if (p[3] > (p[4] == 1 ? 0.8f : 0.5f)) pops.remove(i);
        }

        if (state == PLAYING) {
            stepPlayer(dt);
            updateCheckpoint();
            stepEnemies(dt);
            pickups();
            if (state == PLAYING && player.x + player.w >= level.flagX - 0.45f) win();
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
            float door = level.flagX + 0.5f - player.w / 2;          // il robot entra nel portone che si apre
            if (stateTime > 0.5f && player.x < door) player.x = Math.min(door, player.x + 2.2f * dt);
        }
        jumpWas = jump;
        spinWas = spin;

        float target = player.x + player.w / 2 - viewW * 0.4f;
        camX += (target - camX) * Math.min(1f, 10f * dt);
        camX = Math.max(0, Math.min(camX, level.w - viewW));
    }

    private void stepPlayer(float dt) {
        Body p = player;

        // giravolta: parte con un tocco, dura almeno SPIN_MIN e continua finché si tiene premuto (al massimo SPIN_MAX)
        if (spinActive) {
            spinT += dt;
            spinAngle += SPIN_RATE * dt;
            if (spinT >= SPIN_MAX || (spinT >= SPIN_MIN && !spin)) {
                spinActive = false;
                spinCool = SPIN_COOLDOWN;
            }
        } else if (spinCool > 0) {
            spinCool -= dt;
        } else if (spin && !spinWas) {
            spinActive = true;
            spinT = 0;
            spinAngle = 0;
        }
        float maxSpeed = spinActive ? MAX_SPEED * SPIN_SPEED : MAX_SPEED;

        float dir = (right ? 1 : 0) - (left ? 1 : 0);
        if (dir != 0) {
            float a = p.onGround ? ACCEL : AIR_ACCEL;
            if (dir * p.vx < 0) a *= p.onGround ? 2f : 1.5f;      // inversione più reattiva
            p.vx += dir * a * dt;
            if (p.onGround) facing = dir > 0 ? 1 : -1;            // in aria il personaggio non si volta
        } else {
            float f = (p.onGround ? FRICTION : AIR_FRICTION) * dt;
            if (Math.abs(p.vx) <= f) p.vx = 0; else p.vx -= Math.signum(p.vx) * f;
        }
        p.vx = Math.max(-maxSpeed, Math.min(maxSpeed, p.vx));

        // salto: un po' di tolleranza se si preme poco prima di toccare terra o poco dopo il bordo
        boolean press = jump && !jumpWas;
        jumpBuffer = press ? 0.2f : jumpBuffer - dt;
        coyote = p.onGround ? 0.14f : coyote - dt;
        if (p.onGround) airJumps = 1;
        if (jumpBuffer > 0 && coyote > 0) {
            p.vy = -JUMP_V;
            jumpBuffer = coyote = 0;
        } else if (press && !p.onGround && airJumps > 0 && p.vy < 1f) {
            // doppio salto: vale solo in salita (o proprio sulla cima), non quando si sta già scendendo
            p.vy = -AIR_JUMP_V;
            airJumps--;
            jumpBuffer = 0;
            djX = p.x + p.w / 2;
            djY = p.y + p.h;
            djTime = 0.3f;
        }

        if (spinActive && !p.onGround && p.vy >= 0) {              // planata
            if (p.vy > GLIDE_FALL) p.vy = Math.max(GLIDE_FALL, p.vy - 60f * dt);
            else p.vy = Math.min(GLIDE_FALL, p.vy + GRAVITY * dt);
        } else {
            // tenendo premuto si sale più in alto
            p.vy = Math.min(MAX_FALL, p.vy + ((p.vy < 0 && jump) ? GRAVITY_HOLD : GRAVITY) * dt);
        }

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

    /** Casella colpita da sotto con la testa. */
    public void hitBlock(int tx, int ty) {
        char c = level.get(tx, ty);
        if (c != Level.BONUS && c != Level.BRICK && c != Level.SECRET) return;
        bumpTx = tx;
        bumpTy = ty;
        bumpAge = 0;
        if (c == Level.BONUS) {
            level.set(tx, ty, Level.USED);
            gems++;
            score += 200;
            pops.add(new float[] {tx, ty - 1, -13f, 0, 0});
            checkGem();
        } else if (c == Level.SECRET) {
            level.set(tx, ty, Level.USED);
            pops.add(new float[] {tx, ty - 1, -14f, 0, 1});
            if (!secretOwned) {
                secretOwned = true;
                save.set(level.id + "_secret");
                score += 1000;
                showToast("Chip segreto trovato!");
            }
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
            if (e.type == Enemy.CROW) {                          // il corvo vola a onde sopra il percorso, senza urtare nulla
                e.x += e.dir * 2.3f * dt;
                if (e.x > e.hi) { e.x = e.hi; e.dir = -1; } else if (e.x < e.lo) { e.x = e.lo; e.dir = 1; }
                e.y = e.baseY + (float) Math.sin(e.anim * 2.6f) * 1.15f;
                e.anim += dt;
            } else {
                float speed = e.type == Enemy.HOG ? ENEMY_SPEED * 0.75f : ENEMY_SPEED;
                e.vx = e.dir * speed;
                if (e.type == Enemy.ACORN) {                       // la ghianda sta ferma, poi spicca un balzo in avanti
                    e.hop -= dt;
                    if (e.onGround && e.hop <= 0) { e.vy = -14f; e.hop = 1.5f; }
                    e.vx = e.onGround ? 0 : e.dir * 2.6f;
                }
                e.vy = Math.min(MAX_FALL, e.vy + GRAVITY * dt);
                e.move(level, dt);
                if (e.hitWall) e.dir = -e.dir;
                e.anim += dt;
                if (e.y > level.h + 2) { e.dead = true; continue; }
            }

            if (state != PLAYING) continue;
            if (spinActive && touches(player, e, SPIN_REACH)) {      // la trottola travolge i nemici
                e.flung = true;
                e.vy = -11f;
                e.vx = facing * 5f;
                score += 100;
            } else if (player.overlaps(e)) {
                if (e.type != Enemy.HOG && player.vy > 0 && player.y + player.h - e.y < 0.75f) {
                    e.flatTime = 0.45f;
                    player.vy = jump ? -15f : -10f;
                    airJumps = 1;                       // rimbalzando su un nemico si può rifare il doppio salto
                    score += 100;
                } else {
                    hurt();
                }
            }
        }
    }

    private static boolean touches(Body a, Body b, float m) {
        return a.x - m < b.x + b.w && a.x + a.w + m > b.x && a.y - m < b.y + b.h && a.y + a.h + m > b.y;
    }

    private void pickups() {
        Body p = player;
        for (int i = shards.size() - 1; i >= 0; i--) {
            float[] c = shards.get(i);
            if (p.x < c[0] + 0.8f && p.x + p.w > c[0] + 0.2f && p.y < c[1] + 0.8f && p.y + p.h > c[1] + 0.2f) {
                shards.remove(i);
                gems++;
                score += 50;
                checkGem();
            }
        }
        if (battery != null && p.x < battery[0] + 0.9f && p.x + p.w > battery[0] && p.y < battery[1] + 0.9f && p.y + p.h > battery[1]) {
            battery = null;
            if (!batteryOwned) {
                batteryOwned = true;
                save.set(level.id + "_battery");
                score += 500;
                showToast("Cella energetica trovata!");
            }
        }
    }

    /** Con l'ultimo pezzo di gemma si ottiene la gemma completa. */
    private void checkGem() {
        if (gems >= gemTotal && !gemOwned) {
            gemOwned = true;
            save.set(level.id + "_gem");
            score += 1000;
            showToast("Gemma completa!");
        }
    }

    private void showToast(String s) {
        toast = s;
        toastTime = 2.6f;
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
        score += 1000;
        player.vx = player.vy = 0;
        facing = 1;
    }
}
