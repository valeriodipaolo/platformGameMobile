package com.valeriodipaolo.platformgame.core3d;

import com.valeriodipaolo.platformgame.core.Save;

import java.util.ArrayList;
import java.util.List;

/**
 * Stato e regole del livello 3D a corridoio. Unità: caselle e secondi; x di traverso (da -2,5 a 2,5),
 * y in alto, z in avanti. Il terreno è una griglia di colonne larga W caselle.
 */
public final class World3D {
    public static final int PLAYING = 0, DYING = 1, WON = 2, GAME_OVER = 3;
    public static final int W = 5;
    public static final float HALF = W / 2f;
    public static final int PIT = -99;
    public static final float R = 0.3f, PLAYER_H = 1.25f;
    public static final int MAX_HP = 3;

    private static final float STEP = 1f / 120f;
    private static final float MAX_SPEED = 7f, ACCEL = 70f, FRICTION = 70f, AIR_ACCEL = 22f, AIR_FRICTION = 3f;
    private static final float JUMP_V = 10.5f, AIR_JUMP_V = 9.5f, GRAVITY = 45f, GRAVITY_HOLD = 24f, MAX_FALL = 18f;
    private static final float GLIDE_FALL = 1.6f;
    public static final float SPIN_MIN = 0.4f, SPIN_MAX = 1.3f, SPIN_COOLDOWN = 0.4f;
    private static final float SPIN_RATE = 19f, SPIN_REACH = 0.4f, ENEMY_SPEED = 1.6f, INVULN = 2f;

    public final Save save;
    /** 2 = bosco d'estate, 4 = bosco d'autunno. */
    public final int levelNo;
    public final String id;
    public int length;
    /** Altezza di ogni colonna [z * W + x]: PIT = vuoto, 0 = prato, 1 o più = blocchi di pietra impilati. */
    public int[] height;
    /** Casse bonus ancora intere, per casella. */
    public boolean[] crate;
    public int flagZ;
    public final List<float[]> gemsList = new ArrayList<float[]>();
    public final List<Enemy3> enemies = new ArrayList<Enemy3>();
    /** Gemme che escono dalle casse rotte: {x, y, z, età}. */
    public final List<float[]> pops = new ArrayList<float[]>();
    public float[] battery;
    /**
     * Incendio (livello 8): muro di fuoco che insegue da dietro, getti di fiamma a tempo {x, z, periodo, fase} e tronchi in fiamme
     * che rotolano di traverso {z, periodo, fase}. fireZones = {{z, velocità}, ...}: vale la zona più avanzata raggiunta.
     */
    public float fireStart = Float.NaN, fireZ = Float.NaN;
    public float[][] fireZones = new float[0][];
    public final List<float[]> jets = new ArrayList<float[]>();
    public final List<float[]> logs = new ArrayList<float[]>();

    /** Comandi a carro armato: joyX gira il robot, joyY lo fa avanzare o indietreggiare nella direzione in cui guarda. */
    public boolean tank;
    public static final float TANK_TURN = 3.4f, TANK_BACK = 0.6f;
    public float px, py, pz, vx, vy, vz, yaw, runPhase;
    public boolean onGround;
    public static final int START_LIVES = 100;
    public int state, score, lives, hp, gems, gemTotal;
    public boolean batteryOwned, gemOwned;
    public String toast;
    public float toastTime, invuln, stateTime, time, spinT, spinCool, spinAngle, djTime, djX, djY, djZ;
    /** Altezza del terreno sotto il giocatore (per l'ombra e la camera). */
    public float groundY;

    private float joyX, joyY, acc, jumpBuffer, coyote, cpX, cpZ;
    private boolean jump, jumpWas, spin, spinWas, spinActive;
    private int airJumps;

    public World3D(Save save) {
        this(save, 2);
    }

    public World3D(Save save, int levelNo) {
        this.save = save;
        this.levelNo = levelNo;
        this.id = "l" + levelNo;
        newGame();
    }

    public void newGame() {
        score = 0;
        gems = 0;
        lives = START_LIVES;
        if (levelNo == 8) LevelEight.build(this); else if (levelNo == 6) LevelSix.build(this); else if (levelNo == 4) LevelFour.build(this); else LevelTwo.build(this);
        gemTotal = gemsList.size();
        for (boolean c : crate) if (c) gemTotal++;
        batteryOwned = gemOwned = false;
        pops.clear();
        toast = null;
        toastTime = 0;
        cpX = 0;
        cpZ = 1.5f;
        fireZ = fireStart;
        hp = MAX_HP;
        invuln = 0;
        place();
    }

    private void place() {
        px = cpX;
        pz = cpZ;
        py = Math.max(0, topAt(px, pz)) + 0.01f;
        vx = vy = vz = 0;
        yaw = 0;
        onGround = false;
        state = PLAYING;
        stateTime = 0;
        jumpBuffer = coyote = 0;
        spinActive = false;
        spinT = spinCool = 0;
        airJumps = 1;
        djTime = 0;
        groundY = py;
    }

    /** joyX: -1 sinistra .. 1 destra; joyY: -1 indietro .. 1 avanti. */
    public void setInput(float joyX, float joyY, boolean jump, boolean spin) {
        float l = (float) Math.sqrt(joyX * joyX + joyY * joyY);
        if (l > 1) { joyX /= l; joyY /= l; }
        this.joyX = joyX;
        this.joyY = joyY;
        this.jump = jump;
        this.spin = spin;
    }

    public boolean spinning() {
        return spinActive;
    }

    /** Tocco a fine partita: dopo la sconfitta ricomincia il livello. Restituisce true se ha ricominciato. */
    public boolean tapRestart() {
        if (state == GAME_OVER && stateTime > 1.2f) {
            newGame();
            return true;
        }
        return false;
    }

    public boolean wonAndReady() {
        return state == WON && stateTime > 1.6f;
    }

    // ------------------------------------------------------------------ terreno

    /** Altezza della colonna (cassa compresa); fuori dai lati c'è un muro, oltre la fine il prato continua. */
    public int cell(int xi, int zi) {
        if (xi < 0 || xi >= W || zi < 0) return 99;
        if (zi >= length) return 0;
        int h = height[zi * W + xi];
        if (h == 0 && crate[zi * W + xi]) return 1;
        return h;
    }

    static int xi(float x) { return (int) Math.floor(x + HALF); }
    static int zi(float z) { return (int) Math.floor(z); }

    /** Quota più alta sotto l'impronta del giocatore; molto negativa se sotto c'è solo vuoto. */
    public float topAt(float x, float z) {
        int best = PIT;
        for (int cz = zi(z - R); cz <= zi(z + R); cz++) {
            for (int cx = xi(x - R); cx <= xi(x + R); cx++) {
                int h = cell(cx, cz);
                if (h != 99 && h > best) best = h;
            }
        }
        return best == PIT ? -1000f : best;
    }

    private boolean blocked(float x, float z, float feet) {
        for (int cz = zi(z - R); cz <= zi(z + R); cz++) {
            for (int cx = xi(x - R); cx <= xi(x + R); cx++) {
                int h = cell(cx, cz);
                if (h != PIT && h > feet + 0.05f) return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ aggiornamento

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
        if (invuln > 0) invuln -= dt;
        if (djTime > 0) djTime -= dt;
        for (int i = pops.size() - 1; i >= 0; i--) {
            float[] p = pops.get(i);
            p[1] += 2.2f * dt;
            p[3] += dt;
            if (p[3] > 0.6f) pops.remove(i);
        }
        if (state == PLAYING) {
            stepPlayer(dt);
            stepFire(dt);
            stepEnemies(dt);
            pickups();
            if (state == PLAYING && pz >= flagZ - 0.4f) win();
        } else if (state == DYING) {
            vy -= 30f * dt;
            py += vy * dt;
            stepEnemies(dt);
            if (stateTime > 1.6f) {
                if (lives > 0) {
                    hp = MAX_HP;
                    place();
                    invuln = 2.5f;
                    if (!Float.isNaN(fireZ)) fireZ = Math.min(fireZ, cpZ - 14f);
                } else {
                    state = GAME_OVER;
                    stateTime = 0;
                }
            }
        } else if (state == WON) {
            vy = Math.max(-MAX_FALL, vy - GRAVITY * dt);
            py += vy * dt;
            float g = topAt(px, pz);
            if (py <= g) { py = g; vy = 0; onGround = true; }
        }
        jumpWas = jump;
        spinWas = spin;
    }

    /** Quanto è acceso un getto (0..1), e quanto manca all'accensione (avviso). */
    public static float jetPower(float[] j, float t) {
        float per = j[2], ph = (t + j[3]) % per, on = per * 0.4f;
        if (ph < on) return Math.min(1f, Math.min(ph / 0.12f, (on - ph) / 0.2f + 0.3f));
        return 0f;
    }

    public static float jetWarn(float[] j, float t) {
        float per = j[2], ph = (t + j[3]) % per, left = per - ph;
        return left < 0.7f ? 1f - left / 0.7f : 0f;
    }

    /** Posizione di traverso di un tronco che rotola: attraversa il corridoio e ricompare dall'altro lato. */
    public static float logX(float[] l, float t) {
        float u = ((t + l[2]) % l[1]) / l[1];
        return -HALF - 0.9f + (W + 1.8f) * u;
    }

    private void stepFire(float dt) {
        if (!Float.isNaN(fireZ)) {
            float speed = 0, gap = 1e9f;
            for (float[] z : fireZones) if (pz >= z[0]) { speed = z[1]; gap = z.length > 2 ? z[2] : 1e9f; }
            fireZ += speed * dt;
            if (gap < 1e8f) fireZ = Math.max(fireZ, pz - gap);          // il fuoco non resta mai troppo indietro
            if (pz < fireZ + 0.2f) { die(); return; }
        }
        for (float[] j : jets) {
            if (Math.abs(j[1] - pz) > 3f) continue;
            if (jetPower(j, time) > 0.5f && Math.abs(px - j[0]) < 0.62f && Math.abs(pz - j[1]) < 0.62f && py < topAt(j[0], j[1]) + 2.4f) hurt();
        }
        for (float[] l : logs) {
            if (Math.abs(l[0] - pz) > 3f) continue;
            float lx = logX(l, time);
            if (Math.abs(px - lx) < 0.75f && Math.abs(pz - l[0]) < 0.65f && py < topAt(lx, l[0]) + 0.85f) hurt();
        }
    }

    private void stepPlayer(float dt) {
        // giravolta: un tocco dura almeno SPIN_MIN, tenendo premuto continua fino a SPIN_MAX
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

        // movimento: la velocità insegue quella chiesta dal joystick; in aria si corregge molto meno
        float max = spinActive ? MAX_SPEED * 1.2f : MAX_SPEED;
        boolean steering = joyX != 0 || joyY != 0;
        float tx = joyX * max, tz = joyY * max;
        if (tank) {
            if (onGround) yaw += joyX * TANK_TURN * dt;     // in aria non si gira
            float fwd = joyY > 0 ? joyY : joyY * TANK_BACK;
            tx = (float) Math.sin(yaw) * fwd * max;
            tz = (float) Math.cos(yaw) * fwd * max;
            steering = joyY != 0;
        }
        float rate = onGround ? (steering ? ACCEL : FRICTION) : (steering ? AIR_ACCEL : AIR_FRICTION);
        float dx = tx - vx, dz = tz - vz;
        float dl = (float) Math.sqrt(dx * dx + dz * dz), stepV = rate * dt;
        if (dl <= stepV) { vx = tx; vz = tz; } else { vx += dx / dl * stepV; vz += dz / dl * stepV; }

        // a terra il robot si gira verso dove va; in aria non si volta
        float sp = (float) Math.sqrt(vx * vx + vz * vz);
        if (!tank && onGround && sp > 0.5f) {
            float want = (float) Math.atan2(vx, vz), d = want - yaw;
            while (d > Math.PI) d -= 2 * Math.PI;
            while (d < -Math.PI) d += 2 * Math.PI;
            yaw += d * Math.min(1f, 16f * dt);
        }

        boolean press = jump && !jumpWas;
        jumpBuffer = press ? 0.2f : jumpBuffer - dt;
        coyote = onGround ? 0.14f : coyote - dt;
        if (onGround) airJumps = 1;
        if (jumpBuffer > 0 && coyote > 0) {
            vy = JUMP_V;
            jumpBuffer = coyote = 0;
            onGround = false;
        } else if (press && !onGround && airJumps > 0 && vy > -0.6f) {
            vy = AIR_JUMP_V;                          // doppio salto: solo in salita o sulla cima
            airJumps--;
            jumpBuffer = 0;
            djX = px; djY = py; djZ = pz;
            djTime = 0.3f;
        }

        if (spinActive && !onGround && vy <= 0) {     // planata
            if (vy < -GLIDE_FALL) vy = Math.min(-GLIDE_FALL, vy + 40f * dt);
            else vy = Math.max(-GLIDE_FALL, vy - GRAVITY * dt);
        } else {
            vy = Math.max(-MAX_FALL, vy - ((vy > 0 && jump) ? GRAVITY_HOLD : GRAVITY) * dt);
        }

        // spostamento: un asse alla volta, fermandosi contro blocchi e casse più alti dei piedi
        float nx = px + vx * dt;
        if (blocked(nx, pz, py)) vx = 0; else px = nx;
        float nz = pz + vz * dt;
        if (blocked(px, nz, py)) vz = 0; else pz = nz;

        py += vy * dt;
        float g = topAt(px, pz);
        onGround = false;
        if (py <= g && vy <= 0 && py > g - 0.6f) {
            py = g;
            vy = 0;
            onGround = true;
        }
        if (g > -500f) groundY = g;
        if (onGround && sp > 0.3f) runPhase += sp * dt * 2.1f;

        if (onGround && g == 0) updateCheckpoint();
        if (py < -6f) die();
    }

    /** Ultimo punto di prato sicuro: niente vuoto né ostacoli nelle caselle attorno. */
    private void updateCheckpoint() {
        if (pz < cpZ + 2f) return;
        for (int cz = zi(pz) - 1; cz <= zi(pz) + 1; cz++) {
            for (int cx = xi(px) - 1; cx <= xi(px) + 1; cx++) {
                if (cx < 0 || cx >= W) continue;
                if (cell(cx, cz) != 0) return;
            }
        }
        cpX = px;
        cpZ = pz;
    }

    public static final float ENEMY_R = 0.42f;

    /**
     * true se il nemico, spostato in (x, z), entrerebbe in un blocco, in una cassa o nel muro laterale, oppure (se cammina)
     * resterebbe senza terra sotto i piedi. Il corvo vola: lo ostacola solo ciò che è più alto della sua quota più bassa.
     */
    public boolean enemyBlocked(Enemy3 e, float x, float z) {
        float floor = Float.isNaN(e.floor) ? (e.type == Enemy3.ACORN ? e.baseY : e.y) : e.floor;
        float feet = e.type == Enemy3.CROW ? e.baseY - 0.75f : floor;
        for (int cz = zi(z - ENEMY_R); cz <= zi(z + ENEMY_R); cz++) {
            for (int cx = xi(x - ENEMY_R); cx <= xi(x + ENEMY_R); cx++) {
                int h = cell(cx, cz);
                if (h == 99) return true;
                if (h != PIT && h > feet + 0.05f) return true;
            }
        }
        if (e.type != Enemy3.CROW) {
            int hc = cell(xi(x), zi(z));
            if (hc == PIT || hc < floor - 0.05f) return true;
        }
        return false;
    }

    /** Fa avanzare solo i nemici (serve ai controlli automatici). */
    public void advanceEnemies(float dt) {
        stepEnemies(dt);
    }

    private void stepEnemies(float dt) {
        for (Enemy3 e : enemies) {
            if (e.dead) continue;
            if (e.flatTime > 0) {
                e.flatTime -= dt;
                if (e.flatTime <= 0) e.dead = true;
                continue;
            }
            if (e.flung) {
                e.vy -= GRAVITY * dt;
                e.x += e.vx * dt;
                e.y += e.vy * dt;
                e.z += e.vz * dt;
                e.anim += dt * 3;
                if (e.y < -6) e.dead = true;
                continue;
            }
            if (Math.abs(e.z - pz) > 45) continue;
            e.anim += dt;
            float speed = e.type == Enemy3.HOG ? ENEMY_SPEED * 0.8f : e.type == Enemy3.CROW ? ENEMY_SPEED * 1.5f : ENEMY_SPEED;
            if (e.type == Enemy3.ACORN) {                       // la ghianda aspetta ferma, poi balza in avanti
                e.hopT += dt;
                float t = e.hopT % 1.7f;
                if (t < 0.9f) {
                    e.y = e.baseY;
                    speed = 0;
                } else {
                    float u = (t - 0.9f) / 0.8f;
                    e.y = e.baseY + 1.5f * 4f * u * (1f - u);
                    speed = ENEMY_SPEED * 1.7f;
                }
            } else if (e.type == Enemy3.CROW) {
                e.y = e.baseY + 0.45f * (float) Math.sin(e.anim * 2.4f);
            }
            if (Float.isNaN(e.floor)) e.floor = e.type == Enemy3.ACORN ? e.baseY : e.y;
            float nx = e.x, nz = e.z;
            if (e.alongZ) nz += e.dir * speed * dt; else nx += e.dir * speed * dt;
            if (enemyBlocked(e, nx, nz)) {
                e.dir = -e.dir;                                 // un blocco, una cassa, un muro o il bordo di un burrone: fa dietrofront
            } else {
                e.x = nx;
                e.z = nz;
            }
            if (e.alongZ) {
                if (e.z > e.hi) { e.z = e.hi; e.dir = -1; } else if (e.z < e.lo) { e.z = e.lo; e.dir = 1; }
            } else {
                if (e.x > e.hi) { e.x = e.hi; e.dir = -1; } else if (e.x < e.lo) { e.x = e.lo; e.dir = 1; }
            }
            if (state != PLAYING) continue;
            float ax = Math.abs(e.x - px), az = Math.abs(e.z - pz);
            boolean vertical = py < e.y + 0.6f && py + PLAYER_H > e.y;
            if (spinActive && ax < 0.75f + SPIN_REACH && az < 0.75f + SPIN_REACH && vertical) {
                e.flung = true;                       // la giravolta lo fa volare via
                float l = Math.max(0.01f, (float) Math.sqrt((e.x - px) * (e.x - px) + (e.z - pz) * (e.z - pz)));
                e.vx = (e.x - px) / l * 6f;
                e.vz = (e.z - pz) / l * 6f + 3f;
                e.vy = 8f;
                score += 100;
            } else if (ax < 0.75f && az < 0.75f && vertical) {
                if (e.type != Enemy3.HOG && vy < 0 && py > e.y + 0.25f) {     // schiacciato dall'alto (il riccio punge)
                    e.flatTime = 0.45f;
                    vy = jump ? 9f : 6.5f;
                    airJumps = 1;
                    score += 100;
                } else {
                    hurt();
                }
            }
        }
    }

    private void pickups() {
        for (int i = gemsList.size() - 1; i >= 0; i--) {
            float[] g = gemsList.get(i);
            if (Math.abs(g[0] - px) < 0.65f && Math.abs(g[2] - pz) < 0.65f && g[1] > py - 0.4f && g[1] < py + PLAYER_H + 0.3f) {
                gemsList.remove(i);
                gems++;
                score += 50;
                checkGem();
            }
        }
        if (battery != null && Math.abs(battery[0] - px) < 0.7f && Math.abs(battery[2] - pz) < 0.7f
                && battery[1] > py - 0.4f && battery[1] < py + PLAYER_H + 0.3f) {
            battery = null;
            batteryOwned = true;
            save.set(id + "_battery");
            score += 500;
            showToast("Cella energetica trovata!");
        }
        if (spinActive) {                             // la giravolta rompe le casse vicine
            float m = R + SPIN_REACH;
            for (int cz = zi(pz - m); cz <= zi(pz + m); cz++) {
                for (int cx = xi(px - m); cx <= xi(px + m); cx++) {
                    if (cx < 0 || cx >= W || cz < 0 || cz >= length) continue;
                    int k = cz * W + cx;
                    if (crate[k] && py < 1.2f) {
                        crate[k] = false;
                        gems++;
                        score += 200;
                        pops.add(new float[] {cx - HALF + 0.5f, 0.8f, cz + 0.5f, 0});
                        checkGem();
                    }
                }
            }
        }
    }

    private void checkGem() {
        if (gems >= gemTotal && !gemOwned) {
            gemOwned = true;
            save.set(id + "_gem");
            score += 1000;
            showToast("Gemma completa!");
        }
    }

    private void showToast(String s) {
        toast = s;
        toastTime = 2.6f;
    }

    private void hurt() {
        if (invuln > 0 || state != PLAYING) return;
        hp--;
        if (hp <= 0) {
            die();
        } else {
            invuln = INVULN;
            vy = 6f;
            vz = -4f;
            vx = 0;
            onGround = false;
        }
    }

    private void die() {
        if (state != PLAYING) return;
        state = DYING;
        stateTime = 0;
        hp = 0;
        lives--;
        vx = vz = 0;
        vy = 9f;
    }

    private void win() {
        state = WON;
        stateTime = 0;
        score += 500;
        vx = vz = 0;
        spinActive = false;
    }
}
