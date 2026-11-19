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
    /** Apertura del portone di fine livello (0..1): si apre solo con la cella di energia; la catapulta (t >= 0) riporta all'inizio. */
    public float gateOpen, launchT = -1;
    private float lfx, lfy;
    /** Posizione del muro di fuoco (NaN = nessun fuoco in questo livello). */
    public float fireX = Float.NaN;
    /** Piattaforme che crollano: {x, y, tempo trascorso da quando ci si sta sopra}; e quelle già crollate, da rimettere alla ripartenza. */
    private final List<float[]> crumbling = new ArrayList<float[]>();
    private final List<int[]> collapsed = new ArrayList<int[]>();
    public static final float CRUMBLE_DELAY = 0.55f;
    /** Lancio dei fiori a molla (caselle/s verso l'alto) e ultima molla usata, per l'animazione: posizione e secondi trascorsi. */
    private static final float SPRING_V = 24.5f, SPRING_VX = 11f;
    public int springTx = -1, springTy = -1;
    public float springAge = 9f;
    private float springBoost;
    public int bumpTx = -1, bumpTy = -1;
    public float bumpAge;

    public static final int START_LIVES = 100;
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
    private float jumpBuffer, coyote, acc, checkpointX, checkpointY;
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
        lives = START_LIVES;
        loadLevel();
    }

    private void loadLevel() {
        level = levelNo > 24 ? LevelsC.build(levelNo) : levelNo > 12 ? LevelsB.build(levelNo) : levelNo == 11 ? LevelEleven.build() : levelNo == 9 ? LevelNine.build() : levelNo == 7 ? LevelSeven.build() : levelNo == 5 ? LevelFive.build() : levelNo == 3 ? LevelThree.build() : LevelOne.build();
        level.sealEnd();
        gateOpen = 0;
        launchT = -1;
        enemies.clear();
        for (float[] e : level.enemies) {
            enemies.add(e.length > 2 ? new Enemy(e[0], e[1], (int) e[2], e[3], e[4]) : new Enemy(e[0], e[1]));
            Enemy last = enemies.get(enemies.size() - 1);
            if (last.type == Enemy.CROW) {                      // il corvo vola basso: a poco più di un'altezza d'uomo sopra il terreno sotto di lui
                int gy = level.h;                                // il punto più alto del terreno lungo il suo percorso (i burroni non contano)
                for (int cx = (int) Math.floor(last.lo); cx <= (int) Math.floor(last.hi); cx++)
                    for (int y = Math.max(8, (int) Math.floor(last.baseY)); y < level.h - 1; y++) if (level.solid(cx, y)) { gy = Math.min(gy, y); break; }
                if (gy >= level.h) gy = 10;
                last.baseY = Math.max(last.baseY, gy - 2.6f);
                last.y = last.baseY;
            }
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
        checkpointY = level.startY;
        fireX = level.fireStart;
        crumbling.clear();
        collapsed.clear();
        shots.clear();
        springBoost = 0;
        springAge = 9f;
        place(level.startX, level.startY);
        hp = MAX_HP;
        invuln = 0;
    }

    private void place(float x, float y) {
        player.x = x;
        player.y = y;
        player.vx = player.vy = 0;
        player.onGround = false;
        facing = 1;
        state = PLAYING;
        stateTime = 0;
        jumpBuffer = coyote = 0;
        springBoost = 0;
        spinActive = false;
        spinT = spinCool = 0;
        airJumps = 1;
        djTime = 0;
        camX = Math.max(0, Math.min(x - viewW * 0.4f, level.w - viewW));
    }

    /** Dopo aver perso una vita si riparte dall'ultimo punto sicuro, tenendo gemme e nemici già eliminati. */
    private void respawn() {
        hp = MAX_HP;
        place(checkpointX, checkpointY);       // alla quota del punto sicuro, non a quella della partenza: su un rialzo si finirebbe dentro il terreno
        invuln = 2.5f;
        for (int[] c : collapsed) level.set(c[0], c[1], Level.CRUMBLE);      // i ponti crollati si ricostruiscono
        collapsed.clear();
        crumbling.clear();
        if (!Float.isNaN(fireX)) fireX = Math.min(fireX, checkpointX - 12f);
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
            if (launchT >= 0) {
                stepLaunch(dt);
            } else {
                stepPlayer(dt);
                stepFire(dt);
                updateCheckpoint();
                stepEnemies(dt);
                pickups();
                stepGate(dt);
            }
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
        spinWas = spin;

        float target = player.x + player.w / 2 - viewW * 0.4f;
        camX += (target - camX) * Math.min(1f, 10f * dt);
        camX = Math.max(0, Math.min(camX, level.w - viewW));
    }

    /** Jet attivo (acceso) al tempo t: fase 0..1 del ciclo; acceso nel primo 40%, avviso negli ultimi secondi. */
    public static float jetPower(float[] j, float t) {
        float per = j[2], ph = (t + j[3]) % per, on = per * 0.4f;
        if (ph < on) return Math.min(1f, Math.min(ph / 0.12f, (on - ph) / 0.2f + 0.3f));
        return 0f;
    }

    /** 0..1: quanto manca all'accensione (avviso con braci), solo negli ultimi 0,7 s prima. */
    public static float jetWarn(float[] j, float t) {
        float per = j[2], ph = (t + j[3]) % per, left = per - ph;
        return left < 1.0f ? 1f - left / 1.0f : 0f;
    }

    /** Muro di fuoco, getti di fiamma e piattaforme che crollano. */
    private void stepFire(float dt) {
        Body p = player;
        if (!Float.isNaN(fireX)) {
            float speed = 0, gap = 1e9f;
            for (float[] z : level.fireZones) if (p.x >= z[0]) { speed = z[1]; gap = z.length > 2 ? z[2] : 1e9f; }
            fireX += speed * dt;
            if (gap < 1e8f) fireX = Math.max(fireX, p.x - gap);          // il fuoco non resta mai troppo indietro
            if (p.x + p.w * 0.5f < fireX) { die(); return; }
        }
        for (float[] t : level.thorns) {                              // rovi: pungono chi li tocca
            if (Math.abs(t[0] - p.x) > 2f) continue;
            if (p.x + p.w > t[0] + 0.1f && p.x < t[0] + 0.9f && p.y + p.h > t[1] + 0.3f && p.y < t[1] + 1f) hurt();
        }
        for (float[] j : level.jets) {
            if (Math.abs(j[0] - p.x) > 3f) continue;
            if (jetPower(j, time) > 0.5f && p.x + p.w > j[0] + 0.2f && p.x < j[0] + 0.8f && p.y + p.h > j[1] - 2.7f && p.y < j[1]) hurt();
        }
        // piattaforme che crollano: chi ci sta sopra le fa cedere dopo un attimo
        if (p.onGround) {
            int fy = (int) Math.floor(p.y + p.h + 0.02f);
            for (int tx = (int) Math.floor(p.x); tx <= (int) Math.floor(p.x + p.w - 0.001f); tx++) {
                if (level.get(tx, fy) != Level.CRUMBLE) continue;
                boolean known = false;
                for (float[] c : crumbling) if ((int) c[0] == tx && (int) c[1] == fy) known = true;
                if (!known) crumbling.add(new float[] {tx, fy, 0});
            }
        }
        for (int i = crumbling.size() - 1; i >= 0; i--) {
            float[] c = crumbling.get(i);
            c[2] += dt;
            if (c[2] >= CRUMBLE_DELAY) {
                level.set((int) c[0], (int) c[1], Level.EMPTY);
                collapsed.add(new int[] {(int) c[0], (int) c[1]});
                crumbling.remove(i);
            }
        }
    }

    /** Tempo trascorso (secondi) da quando la piattaforma è stata toccata, oppure -1 se intatta. */
    public float crumbleAge(int tx, int ty) {
        for (float[] c : crumbling) if ((int) c[0] == tx && (int) c[1] == ty) return c[2];
        return -1f;
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
        boolean flung = springBoost > 0 && !p.onGround;               // lanciato dal fiore: tiene la velocità fino all'atterraggio
        if (flung) maxSpeed = Math.max(maxSpeed, SPRING_VX);

        float dir = (right ? 1 : 0) - (left ? 1 : 0);
        if (dir != 0) {
            float a = p.onGround ? ACCEL : AIR_ACCEL;
            if (dir * p.vx < 0) a *= p.onGround ? 2f : 1.5f;      // inversione più reattiva
            p.vx += dir * a * dt;
            if (p.onGround) facing = dir > 0 ? 1 : -1;            // in aria il personaggio non si volta
        } else if (!flung) {
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
            p.vy = Math.min(MAX_FALL, p.vy + ((p.vy < 0 && (jump || springBoost > 0)) ? GRAVITY_HOLD : GRAVITY) * dt);
        }

        p.move(level, dt);
        if (p.headTx >= 0) hitBlock(p.headTx, p.headTy);
        if (springBoost > 0) springBoost -= dt;
        springAge += dt;
        if (p.onGround) {                                           // fiore a molla: rimbalzo alto, tenendo la salita lenta come con il salto premuto
            int fy = (int) Math.floor(p.y + p.h + 0.02f);
            for (int tx = (int) Math.floor(p.x); tx <= (int) Math.floor(p.x + p.w - 0.001f); tx++) {
                if (level.get(tx, fy) != Level.SPRING) continue;
                p.vy = -SPRING_V;
                p.vx = facing * SPRING_VX;                                 // il fiore lancia in avanti, nella direzione in cui guarda il robot
                p.onGround = false;
                coyote = jumpBuffer = 0;
                airJumps = 1;
                springBoost = 1.2f;
                springTx = tx;
                springTy = fy;
                springAge = 0;
                break;
            }
        }
        if (p.onGround && Math.abs(p.vx) > 0.3f) runPhase += Math.abs(p.vx) * dt * 1.0f;
        if (p.y > level.h + 1) die();
    }

    /** Ricorda l'ultimo punto di terreno solido e largo, lontano dai burroni, da cui ripartire. */
    private void updateCheckpoint() {
        Body p = player;
        if (!p.onGround || p.x <= checkpointX + 2f) return;
        int fx = (int) Math.floor(p.x + p.w / 2), row = (int) Math.floor(p.y + p.h + 0.01f);
        for (int dx = -2; dx <= 2; dx++) {
            if (!level.solid(fx + dx, row) || level.solid(fx + dx, row - 1) || level.get(fx + dx, row) == Level.CRUMBLE || level.get(fx + dx, row) == Level.SPRING) return;
        }
        for (float[] t : level.thorns) if (Math.abs(t[0] - p.x) < 3.5f) return;       // mai vicino ai rovi
        checkpointX = p.x;
        checkpointY = p.y;
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
            if (e.dead || e.flung || e.flatTime > 0 || (e.type == Enemy.SNAIL && e.mode < 2)) continue;
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
            if (e.type >= Enemy.MOLE) {
                stepNew(e, dt);
                if (e.dead) continue;
            } else if (e.type == Enemy.STORM) {                         // la nuvola temporalesca vaga lenta, poi si ferma, si carica e scaglia un fulmine in verticale
                e.hop += dt;
                if (stormPhase(e.hop) == 0) {
                    e.x += e.dir * 1.3f * dt;
                    if (e.x > e.hi) { e.x = e.hi; e.dir = -1; } else if (e.x < e.lo) { e.x = e.lo; e.dir = 1; }
                }
                e.y = e.baseY + (float) Math.sin(e.anim * 1.8f) * 0.25f;
                e.anim += dt;
            } else if (e.type == Enemy.CROW) {                          // il corvo vola a onde sopra il percorso, senza urtare nulla
                e.x += e.dir * 4.6f * dt;
                if (e.x > e.hi) { e.x = e.hi; e.dir = -1; } else if (e.x < e.lo) { e.x = e.lo; e.dir = 1; }
                e.y = e.baseY + (float) Math.sin(e.anim * 3.6f) * 0.8f;
                e.anim += dt;
            } else {
                float speed = e.type == Enemy.HOG ? ENEMY_SPEED * 0.75f : e.type == Enemy.WHIRL ? ENEMY_SPEED * 1.7f : ENEMY_SPEED;
                e.vx = e.dir * speed;
                if (e.type == Enemy.ACORN) {                       // la ghianda sta ferma, poi spicca un balzo in avanti
                    e.hop -= dt;
                    if (e.onGround && e.hop <= 0) { e.vy = -15.5f; e.hop = 0.4f; }      // balzi ravvicinati e rapidi
                    e.vx = e.onGround ? 0 : e.dir * 3.4f;
                }
                e.vy = Math.min(MAX_FALL, e.vy + GRAVITY * dt);
                e.move(level, dt);
                if (e.hitWall) e.dir = -e.dir;
                if (e.type == Enemy.WHIRL) {                       // il mulinello resta sul suo tratto: torna indietro ai bordi e ai limiti
                    if (e.onGround && !level.solid((int) Math.floor(e.x + e.w / 2 + e.dir * 0.6f), (int) Math.floor(e.y + e.h + 0.1f))) e.dir = -e.dir;
                    if (e.x > e.hi) e.dir = -1; else if (e.x < e.lo) e.dir = 1;
                }
                e.anim += dt;
                if (e.y > level.h + 2) { e.dead = true; continue; }
            }

            if (state != PLAYING) continue;
            if (harmless(e)) continue;
            boolean snail = e.type == Enemy.SNAIL;
            if (e.grace > 0) e.grace -= dt;
            // la lumaca col guscio è invulnerabile alla giravolta; chiusa nel guscio (dopo il salto in testa) la giravolta la uccide
            if (snail && e.mode == 0 && spinActive && touches(player, e, SPIN_REACH) && !(player.vy > 0 && player.y + player.h - e.y < 0.75f)) continue;      // la giravolta contro il guscio non fa male né a lei né al robot
            boolean spinHit = spinActive && spinnable(e) && !(snail && e.mode == 0) && touches(player, e, SPIN_REACH);
            if (spinHit) {                             // saltata in testa e poi travolta nel guscio: muore con due colpi                      // la trottola travolge i nemici
                if (e.grace <= 0) {
                    e.flung = true;
                    e.vy = -11f;
                    e.vx = facing * 5f;
                    score += 100;
                }
            } else if (player.overlaps(e)) {
                if (stompable(e) && player.vy > 0 && player.y + player.h - e.y < 0.75f) {
                    if (snail && e.mode < 2) { e.mode = 1; e.t = 4f; } else e.flatTime = 0.45f;     // la lumaca si chiude nel guscio
                    player.vy = jump ? -15f : -10f;
                    airJumps = 1;                       // rimbalzando su un nemico si può rifare il doppio salto
                    score += snail && e.mode == 1 ? 0 : 100;
                } else if (!(snail && (e.mode == 1 || e.grace > 0))) {
                    hurt();
                }
            } else if (e.type == Enemy.STORM && stormPhase(e.hop) == 2) {
                float bx = e.x + e.w / 2;
                if (player.x + player.w > bx - 0.4f && player.x < bx + 0.4f && player.y + player.h > e.y + e.h && player.y < boltBottom(e)) hurt();
            } else if (e.type == Enemy.SALAMANDER && e.mode == 2) {              // la fiammata: due caselle davanti al muso
                float fx0 = e.dir > 0 ? e.x + e.w : e.x - 2.2f;
                if (player.x + player.w > fx0 && player.x < fx0 + 2.2f && player.y + player.h > e.y - 0.3f && player.y < e.y + e.h) hurt();
            } else if (e.type == Enemy.PLANT && e.mode == 2) {                   // il morso: una casella sopra la pianta
                if (player.x + player.w > e.x - 0.1f && player.x < e.x + e.w + 0.1f && player.y + player.h > e.y - 0.9f && player.y < e.y + e.h) hurt();
            }
        }
        for (int i = shots.size() - 1; i >= 0; i--) {              // palle di neve
            float[] q = shots.get(i);
            q[3] += 30f * dt;
            q[0] += q[2] * dt;
            q[1] += q[3] * dt;
            if (level.solid((int) Math.floor(q[0]), (int) Math.floor(q[1])) || q[1] > level.h + 2) { shots.remove(i); continue; }
            if (state == PLAYING && q[0] > player.x - 0.25f && q[0] < player.x + player.w + 0.25f && q[1] > player.y - 0.25f && q[1] < player.y + player.h + 0.25f) {
                shots.remove(i);
                hurt();
            }
        }
    }

    /** Proiettili in volo: {x, y, vx, vy}. */
    public final List<float[]> shots = new ArrayList<float[]>();

    /** Non fa male in questo momento: lapillo dentro la lava. */
    private static boolean harmless(Enemy e) {
        return e.type == Enemy.LAPILLO && e.mode == 0;
    }

    private static boolean stompable(Enemy e) {
        switch (e.type) {
            case Enemy.HOG: case Enemy.WHIRL: case Enemy.CHESTNUT: case Enemy.LAPILLO: case Enemy.STAR: case Enemy.SUNNY: return false;
            case Enemy.PLANT: return e.mode != 2;
            default: return true;
        }
    }

    private static boolean spinnable(Enemy e) {
        switch (e.type) {
            case Enemy.LAPILLO: case Enemy.STAR: return false;
            case Enemy.CHESTNUT: return e.mode == 3;
            case Enemy.PLANT: return e.mode != 2;
            default: return true;
        }
    }

    private boolean edgeAhead(Enemy e) {
        return e.onGround && !level.solid((int) Math.floor(e.x + e.w / 2 + e.dir * 0.6f), (int) Math.floor(e.y + e.h + 0.1f));
    }

    private void walk(Enemy e, float speed, float dt, boolean keepOnLedge) {
        e.vx = e.dir * speed;
        e.vy = Math.min(MAX_FALL, e.vy + GRAVITY * dt);
        e.move(level, dt);
        if (e.hitWall) e.dir = -e.dir;
        else if (keepOnLedge && edgeAhead(e)) e.dir = -e.dir;
        if (e.hi > e.lo) { if (e.x > e.hi) e.dir = -1; else if (e.x < e.lo) e.dir = 1; }
        if (e.y > level.h + 2) e.dead = true;
    }

    /** Comportamento dei nemici della seconda serie. */
    private void stepNew(Enemy e, float dt) {
        float pcx = player.x + player.w / 2, ecx = e.x + e.w / 2, dx = pcx - ecx;
        e.anim += dt;
        e.t += dt;
        switch (e.type) {
            case Enemy.SNAIL:
                if (e.mode == 1) {                           // chiusa nel guscio: ferma e innocua; se non la si travolge in tempo, riesce (il cronometro qui scende)
                    e.t -= 2 * dt;
                    if (e.t <= 0) { e.mode = 0; e.t = 0; }
                    walk(e, 0, dt, false);
                } else walk(e, e.mode == 2 ? 1.5f : 0.9f, dt, true);      // senza guscio è più svelta e vulnerabile
                break;
            case Enemy.CHESTNUT:
                if (e.mode == 0) { if (dx > -2.4f && dx < 1.4f && player.y > e.y) { e.mode = 1; e.t = 0; } }
                else if (e.mode == 1) { if (e.t > 0.35f) e.mode = 2; }            // dondola un attimo, poi si stacca
                else if (e.mode == 2) {
                    walk(e, 0, dt, false);
                    if (e.onGround) { e.mode = 3; e.dir = dx >= 0 ? 1 : -1; }
                } else walk(e, 3.0f, dt, false);                                 // a terra rotola verso il robot
                break;
            case Enemy.OWL:
                if (e.mode == 0) {                           // sorvola il suo tratto; se il robot passa sotto, picchia
                    e.x += e.dir * 1.5f * dt;
                    if (e.x > e.hi) { e.x = e.hi; e.dir = -1; } else if (e.x < e.lo) { e.x = e.lo; e.dir = 1; }
                    e.y = e.baseY + 0.2f * (float) Math.sin(e.anim * 2f);
                    if (Math.abs(dx) < 5f && player.y > e.baseY + 1f) { e.mode = 1; e.t = 0; e.dir = dx >= 0 ? 1 : -1; e.hop = Math.max(1f, player.y - e.baseY + 0.3f); }
                } else if (e.mode == 1) {
                    float u = e.t / 1.5f;
                    e.x += e.dir * 3.4f * dt;
                    e.y = e.baseY + e.hop * (float) Math.sin(Math.PI * Math.min(1f, u));
                    if (u >= 1f) { e.mode = 2; e.t = 0; }
                } else {                                     // risale e riprende fiato
                    e.y = e.baseY;
                    if (e.t > 1.3f) e.mode = 0;
                }
                break;
            case Enemy.PENGUIN:
                walk(e, 5.2f, dt, true);
                break;
            case Enemy.SNOWMAN:
                walk(e, 0, dt, false);
                e.dir = dx >= 0 ? 1 : -1;
                if (e.t > 2.3f && Math.abs(dx) < 11f) {      // lancia una palla di neve a parabola dove si trova il robot
                    e.t = 0;
                    float T = 1.1f;
                    shots.add(new float[] {ecx, e.y + 0.2f, Math.max(-9f, Math.min(9f, dx / T)), (player.y + 0.5f - e.y) / T - 15f * T});
                }
                break;
            case Enemy.LAPILLO: {                            // salta fuori dal burrone a intervalli regolari
                float per = e.lo > 0 ? e.lo : 2.6f, c = e.t % per, u = c / 1.5f;
                e.mode = u < 1f ? 1 : 0;
                e.y = e.baseY - (u < 1f ? e.hi * 4f * u * (1f - u) : 0f);
                break;
            }
            case Enemy.SALAMANDER: {
                float c = e.t % 3.0f;
                e.mode = c < 2.0f ? 0 : c < 2.4f ? 1 : 2;    // cammina, si gonfia (avviso), sputa fuoco
                walk(e, e.mode == 0 ? ENEMY_SPEED : 0, dt, true);
                break;
            }
            case Enemy.FROG:
                e.hop -= dt;
                if (e.onGround && e.hop <= 0 && Math.abs(dx) < 7f) { e.dir = dx >= 0 ? 1 : -1; e.vy = -13f; e.hop = 1.4f; e.onGround = false; }
                e.vx = e.onGround ? 0 : e.dir * 4.2f;
                e.vy = Math.min(MAX_FALL, e.vy + GRAVITY * dt);
                e.move(level, dt);
                if (e.y > level.h + 2) e.dead = true;
                break;
            case Enemy.PLANT: {
                float c = e.t % 3.0f;
                e.mode = c < 1.4f ? 0 : c < 1.8f ? 1 : 2;    // chiusa, trema (avviso), scatta in alto e morde
                break;
            }
            case Enemy.STAR:                                 // cade in diagonale lungo sempre la stessa scia, poi ricompare in alto
                e.x += e.lo * dt;
                e.y += e.hi * dt;
                if (e.y > level.h + 1) { e.x = e.x0; e.y = e.y0; }
                break;
            case Enemy.SUNNY:                                // gira in cerchio attorno al suo punto
                e.x = e.x0 + e.hi * (float) Math.cos(e.anim * e.lo);
                e.y = e.y0 + e.hi * (float) Math.sin(e.anim * e.lo);
                break;
            default: break;
        }
    }

    /** Ciclo della nuvola temporalesca: 0 vaga, 1 si carica (avviso), 2 fulmine. */
    public static int stormPhase(float t) {
        float c = t % 3.6f;
        return c < 2.4f ? 0 : c < 3.1f ? 1 : 2;
    }

    /** Riga (in caselle) fin dove arriva il fulmine di una nuvola: la prima casella solida sotto di lei. */
    public float boltBottom(Enemy e) {
        int tx = (int) Math.floor(e.x + e.w / 2);
        int ty = Math.max(0, (int) Math.floor(e.y + e.h));
        while (ty < level.h && !level.solid(tx, ty)) ty++;
        return ty;
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
                showToast("Chiave trovata!");
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

    /** Portone: con la cella di energia si apre all'avvicinarsi; il livello finisce qualche passo dopo l'ingresso. */
    private void stepGate(float dt) {
        float fx = level.flagX;
        if (batteryOwned && player.x > fx - 6f && gateOpen < 1f) {
            gateOpen = Math.min(1f, gateOpen + dt);
            if (gateOpen > 0.6f) for (int y = level.flagBaseY - 3; y < level.flagBaseY; y++) if (level.get(level.flagX, y) == Level.DOOR) level.set(level.flagX, y, Level.EMPTY);
        } else if (!batteryOwned && player.x > fx - 4.5f && toastTime <= 0) {
            toast = "Serve la chiave per aprire il portone";
            toastTime = 2.4f;
        }
        if (state == PLAYING && gateOpen > 0.6f && player.x + player.w / 2 >= fx + 1.5f) win();      // il robot entra nel varco e sparisce nell'oscurità del portone
        // catapulta: sopra la sua piattaforma, la trottola lancia il robot all'inizio del livello
        float cx = level.catX;
        if (state == PLAYING && spin && !spinWas && player.onGround && player.x + player.w > cx + 0.7f && player.x < cx + 2.45f && Math.abs(player.y + player.h - level.flagBaseY) < 0.2f) {
            launchT = 0;
            lfx = player.x;
            lfy = player.y;
            spinActive = false;
            spinCool = 0;
            player.vx = player.vy = 0;
        }
    }

    private void stepLaunch(float dt) {
        float dur = 2.4f;
        launchT += dt;
        float u = Math.min(1f, launchT / dur);
        player.x = lfx + (level.startX - lfx) * u;
        player.y = lfy + (level.startY - lfy) * u - 10f * 4f * u * (1f - u);
        facing = -1;
        invuln = Math.max(invuln, 0.3f);
        if (u >= 1f) {
            launchT = -1;
            checkpointX = level.startX;
            checkpointY = level.startY;
            respawn();
            invuln = 1.5f;
        }
    }

    /** true mentre il robot vola verso l'inizio del livello. */
    public boolean launching() { return launchT >= 0; }

    private void win() {
        state = WON;
        stateTime = 0;
        score += 1000;
        player.vx = player.vy = 0;
        facing = 1;
    }
}
