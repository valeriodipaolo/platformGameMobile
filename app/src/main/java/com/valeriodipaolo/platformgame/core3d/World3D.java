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
    /** Animazioni scattanti: dopo il salto il robot torna a terra più in fretta. */
    public static boolean fastFall;
    private static final float FAST_FALL_G = 1.6f;
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
    /** Tema grafico (2, 4, 6, 8, 10, 12): i livelli della seconda serie (14..24) hanno lo stesso aspetto delle controparti. */
    public final int theme;
    public final String id;
    public int length;
    /** Altezza di ogni colonna [z * W + x]: PIT = vuoto, 0 = prato, 1 o più = blocchi di pietra impilati. */
    public int[] height;
    /** Casse bonus ancora intere, per casella. */
    public boolean[] crate;
    // ---- isola tropicale (livelli 35-37)
    /** Tipo di ogni cassa (null = tutte normali, come nei livelli di prima): legno, esplosiva a tempo, instabile, a molla, ripartenza. */
    public byte[] crateKind;
    public static final byte C_WOOD = 0, C_TNT = 1, C_NITRO = 2, C_BOUNCE = 3, C_CHECK = 4;
    /** Esplosiva accesa: secondi dall'accensione (0 = spenta). Cassa a molla: istante dell'ultimo rimbalzo. */
    public float[] crateT;
    public static final float TNT_FUSE = 3f, BOOM_R = 1.75f;
    /** Esplosioni in corso: {x, z, età}. */
    public final List<float[]> booms = new ArrayList<float[]>();
    /** Pestelli di pietra del tempio: {colonna 0..4, z, periodo, fase}. */
    public final List<float[]> crushers = new ArrayList<float[]>();
    /** Zattere sul fiume: {colonna, z, istante in cui qualcuno ci è salito (-1 = mai), periodo di riemersione}. */
    public final List<float[]> rafts = new ArrayList<float[]>();
    private int[] raftIdx;
    /** La camera guarda il robot da davanti (fuga dal masso) finché pz < camBackUntil. */
    public float camBackUntil = -1f;
    private float bounceBoost;
    // ---- deserto, fabbrica, miniera (livelli 38-40)
    /** Nastri trasportatori: direzione per casella (0 nessuno, 1 verso destra, 2 sinistra, 3 avanti, 4 indietro). */
    public byte[] belt;
    public static final float BELT_SPEED = 3.4f;
    /** Barriere laser: {z, periodo, fase, colonna spenta (-1 nessuna)}; accese nella prima metà del periodo, all'altezza delle ginocchia. */
    public final List<float[]> lasers = new ArrayList<float[]>();
    /** Sabbie mobili per casella: rallentano, il salto è più corto, e chi ci resta troppo affonda. */
    public boolean[] quick;
    public float sinkT;
    public static final float SINK_TIME = 3.0f;
    /** Raffiche di vento: {z0, z1, periodo, fase, verso (+1 destra, -1 sinistra)}. */
    public final List<float[]> winds = new ArrayList<float[]>();
    public static final float GUST = 1.7f, GUST_PUSH = 4.6f;
    /** Tratti in carrello: {z0, z1}; il carrello corre da solo, si sterza e si salta. */
    public final List<float[]> carts = new ArrayList<float[]>();
    public static final float CART_SPEED = 9.5f;
    public int flagZ;
    /** Portone di fine livello: si apre (0..1) solo con la cella di energia; la catapulta (launchT >= 0) riporta all'inizio. */
    public float gateOpen, launchT = -1;
    public int catZ;
    private boolean sealed;
    private float lx, ly, lz;
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
    /** Cosa insegue da dietro: 0 = muro di fuoco, 1 = sciame di api (livello 11). */
    public int chase;
    /** Nido d'api appeso al ciliegio alla riga hiveZ (NaN = nessuno): quando il robot lo supera cade e parte lo sciame; hiveAt = istante del distacco. */
    public float hiveZ = Float.NaN, hiveAt = -1f;
    public final List<float[]> jets = new ArrayList<float[]>();
    public final List<float[]> logs = new ArrayList<float[]>();
    /** Fiori a molla {x, z} (lanciano in alto chi ci salta sopra) e cespugli di rovi {x, z} (pungono); livelli di primavera. */
    public final List<float[]> springs = new ArrayList<float[]>();
    public final List<float[]> thorns = new ArrayList<float[]>();
    private static final float SPRING_V = 15.5f, SPRING_H = 10f;
    private float springDx, springDz;
    /** Ultimo lancio da un fiore a molla: indice nella lista e istante, per l'animazione. */
    public int springIdx = -1;
    public float springAt = -9f;
    private float springBoost;

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
        this.theme = levelNo > 37 ? Levels3W.theme(levelNo) : levelNo > 34 ? Levels3J.theme(levelNo) : levelNo > 24 ? Levels3C.theme(levelNo) : levelNo > 12 ? levelNo - 12 : levelNo;
        this.id = "l" + levelNo;
        newGame();
    }

    public void newGame() {
        score = 0;
        gems = 0;
        lives = START_LIVES;
        sealed = false;
        crateKind = null; crateT = null; booms.clear(); crushers.clear(); rafts.clear(); raftIdx = null; camBackUntil = -1f;
        belt = null; lasers.clear(); quick = null; sinkT = 0; winds.clear(); carts.clear();
        if (levelNo > 37) Levels3W.build(this, levelNo); else if (levelNo > 34) Levels3J.build(this, levelNo); else if (levelNo > 24) Levels3C.build(this, levelNo); else if (levelNo > 12) Levels3B.build(this, levelNo); else if (levelNo == 12) LevelTwelve.build(this); else if (levelNo == 10) LevelTen.build(this); else if (levelNo == 8) LevelEight.build(this); else if (levelNo == 6) LevelSix.build(this); else if (levelNo == 4) LevelFour.build(this); else LevelTwo.build(this);
        if (!rafts.isEmpty()) {                          // prima delle rifiniture: le zattere a galla contano come terreno
            raftIdx = new int[length * W];
            java.util.Arrays.fill(raftIdx, -1);
            for (int i = 0; i < rafts.size(); i++) raftIdx[(int) rafts.get(i)[1] * W + (int) rafts.get(i)[0]] = i;
        }
        LevelFix.apply(this);
        gateOpen = 0;
        launchT = -1;
        sealed = true;
        sealEnd();
        gemTotal = gemsList.size();
        for (int k = 0; k < crate.length; k++) if (crate[k] && (crateKind == null || crateKind[k] != C_BOUNCE)) gemTotal++;
        if (crateKind != null) crateT = new float[crate.length];
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
        springBoost = 0;
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
        if (sealed && zi == flagZ && (gateOpen < 0.6f || xi == 0 || xi == W - 1)) return 99;      // portone chiuso e pilastri: muro altissimo
        if (zi >= length) return 0;
        int h = height[zi * W + xi];
        if (h == 0 && crate[zi * W + xi]) return 1;
        if (h == PIT && raftIdx != null && raftIdx[zi * W + xi] >= 0 && raftSink(rafts.get(raftIdx[zi * W + xi]), time) < 0.5f) return 0;
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
        if (state == PLAYING && launchT >= 0) {
            stepLaunch(dt);
        } else if (state == PLAYING) {
            stepPlayer(dt);
            stepFire(dt);
            stepIsland(dt);
            stepEnemies(dt); stepShots(dt);
            pickups();
            stepGate(dt);
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

    /** Zattera: 0 a galla, poi affonda (0..1 durante 0,9 s dopo che ci si è saliti), 1 sott'acqua; riemerge dopo il suo periodo. */
    public static float raftSink(float[] r, float t) {
        if (r[2] < 0) return 0;
        float a = t - r[2];
        if (a < 0.55f) return 0;
        if (a < 0.95f) return (a - 0.55f) / 0.4f;
        if (a < r[3]) return 1;
        if (a < r[3] + 0.5f) return 1 - (a - r[3]) / 0.5f;
        r[2] = -1;
        return 0;
    }

    public boolean inCart() {
        for (float[] c : carts) if (pz >= c[0] && pz < c[1]) return true;
        return false;
    }

    public boolean quickAt(float x, float z) {
        if (quick == null) return false;
        int xi = xi(x), zi = zi(z);
        return xi >= 0 && xi < W && zi >= 0 && zi < length && quick[zi * W + xi];
    }

    /** Laser: 1 acceso, 0 spento; nell'ultimo mezzo secondo prima di accendersi lampeggia (valori tra 0 e 1). */
    public static float laserOn(float[] l, float t) {
        float per = l[1], ph = ((t + l[2]) % per + per) % per, on = per * 0.5f;
        if (ph < on) return 1f;
        if (ph > per - 0.5f) return 0.5f * ((int) (ph * 12) % 2);
        return 0f;
    }

    /** Raffica: 0..1 (sale e scende in GUST secondi); prima c'è un preavviso che si legge nella sabbia che si alza. */
    public static float gust(float[] wd, float t) {
        float per = wd[2], ph = ((t + wd[3]) % per + per) % per;
        return ph < GUST ? (float) Math.sin(Math.PI * ph / GUST) : 0f;
    }

    public float windPush(float z, float t) {
        float p = 0;
        for (float[] wd : winds) if (z >= wd[0] && z < wd[1]) p += wd[4] * GUST_PUSH * gust(wd, t);
        return p;
    }

    /** Quota della faccia inferiore del pestello: in alto fermo, cade di colpo, resta giù, risale piano. */
    public static float crusherBottom(float[] c, float t) {
        float per = c[2], ph = ((t + c[3]) % per + per) % per, up = per * 0.5f;
        if (ph < up) return 3.1f;
        ph -= up;
        if (ph < 0.18f) return 3.1f * (1 - ph / 0.18f);
        ph -= 0.18f;
        if (ph < 0.55f) return 0f;
        ph -= 0.55f;
        return Math.min(3.1f, 3.1f * ph / Math.max(0.2f, per - up - 0.73f));
    }

    /** Il pestello sta per cadere (tremolio d'avviso nell'ultimo mezzo secondo in alto). */
    public static float crusherWarn(float[] c, float t) {
        float per = c[2], ph = ((t + c[3]) % per + per) % per, up = per * 0.5f;
        return ph < up && ph > up - 0.5f ? (ph - (up - 0.5f)) / 0.5f : 0f;
    }

    /** Quanto è acceso un getto (0..1), e quanto manca all'accensione (avviso). */
    public static float jetPower(float[] j, float t) {
        float per = j[2], ph = (t + j[3]) % per, on = per * 0.4f;
        if (ph < on) return Math.min(1f, Math.min(ph / 0.12f, (on - ph) / 0.2f + 0.3f));
        return 0f;
    }

    public static float jetWarn(float[] j, float t) {
        float per = j[2], ph = (t + j[3]) % per, left = per - ph;
        return left < 1.0f ? 1f - left / 1.0f : 0f;
    }

    /** Velocità di rotolamento dei tronchi (m/s), partenza dalla pila a sinistra e bordo della fossa a destra. */
    public static final float LOG_SPEED = 2.4f, LOG_X0 = -3.2f, LOG_EDGE = 1.55f;

    /**
     * Posa di un tronco al tempo t: {x, scarto in quota, scala, visibile (0/1), pericoloso (0/1)}. Il tronco esce dalla pila di legna a
     * sinistra, rotola sul sentiero, arriva alla fossa di lava a destra, ci cade dentro e scompare; poi attende il ciclo successivo.
     */
    public static float[] logPose(float[] l, float t) {
        float ph = (t + l[2]) % l[1], x = LOG_X0 + LOG_SPEED * ph, tEdge = (LOG_EDGE - LOG_X0) / LOG_SPEED;
        float dy = 0, sc = Math.min(1f, 0.35f + ph / 0.35f);
        boolean solid = ph < tEdge, vis = true;
        if (ph >= tEdge) {
            float tau = ph - tEdge;
            dy = -9f * tau * tau;
            if (dy < -1.7f) vis = false;
        }
        return new float[] {x, dy, sc, vis ? 1f : 0f, solid ? 1f : 0f};
    }

    private void stepFire(float dt) {
        if (!Float.isNaN(hiveZ) && hiveAt < 0 && pz >= hiveZ + 1.5f) {      // il nido cade: lo sciame esce e inizia l'inseguimento
            hiveAt = time;
            fireZ = hiveZ - 3f;
        }
        if (!Float.isNaN(fireZ)) {
            float speed = 0, gap = 1e9f;
            for (float[] z : fireZones) if (pz >= z[0]) { speed = z[1]; gap = z.length > 2 ? z[2] : 1e9f; }
            fireZ += speed * dt;
            if (gap < 1e8f) fireZ = Math.max(fireZ, pz - gap);          // il fuoco non resta mai troppo indietro
            if (pz < fireZ + 0.2f) { die(); return; }
        }
        for (float[] t : thorns) {                         // rovi
            if (Math.abs(t[1] - pz) > 2f) continue;
            if (Math.abs(px - t[0]) < 0.62f && Math.abs(pz - t[1]) < 0.62f && py < topAt(t[0], t[1]) + 0.55f) hurt();
        }
        for (float[] j : jets) {
            if (Math.abs(j[1] - pz) > 3f) continue;
            if (jetPower(j, time) > 0.5f && Math.abs(px - j[0]) < 0.62f && Math.abs(pz - j[1]) < 0.62f && py < topAt(j[0], j[1]) + 2.4f) hurt();
        }
        for (float[] l : logs) {
            if (Math.abs(l[0] - pz) > 3f) continue;
            float[] lp = logPose(l, time);
            if (lp[4] > 0.5f && Math.abs(px - lp[0]) < 0.75f && Math.abs(pz - l[0]) < 0.65f && py < topAt(Math.max(-2.4f, Math.min(2.4f, lp[0])), l[0]) + 0.85f) hurt();
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
        boolean cart = inCart();
        boolean inQuick = onGround && quickAt(px, pz);
        if (inQuick) max *= 0.38f;                         // sabbie mobili: si arranca
        boolean steering = joyX != 0 || joyY != 0;
        float tx = joyX * max, tz = joyY * max;
        if (cart) { tx = joyX * 5.5f; tz = CART_SPEED; steering = true; }     // il carrello va da solo: si sterza tra i binari
        boolean flung = springBoost > 0 && !onGround;            // lanciato dal fiore: tiene la spinta fino all'atterraggio, sterzando di poco
        if (flung) { tx = springDx + joyX * 3f; tz = springDz + joyY * 3f; steering = true; }
        float rate = onGround ? (steering ? ACCEL : FRICTION) : (steering ? AIR_ACCEL : AIR_FRICTION);
        float dx = tx - vx, dz = tz - vz;
        float dl = (float) Math.sqrt(dx * dx + dz * dz), stepV = rate * dt;
        if (dl <= stepV) { vx = tx; vz = tz; } else { vx += dx / dl * stepV; vz += dz / dl * stepV; }

        // a terra il robot si gira verso dove va; in aria non si volta
        float sp = (float) Math.sqrt(vx * vx + vz * vz);
        if (onGround && sp > 0.5f) {
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
            vy = inQuick ? JUMP_V * 0.72f : JUMP_V;
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
            vy = Math.max(-MAX_FALL * (fastFall ? 1.3f : 1f), vy - ((vy > 0 && (jump || springBoost > 0 || bounceBoost > 0)) ? GRAVITY_HOLD : GRAVITY * (fastFall && vy <= 0 ? FAST_FALL_G : 1f)) * dt);
        }

        // spinte esterne: il nastro sotto i piedi e le raffiche di vento
        float ex = 0, ez = 0;
        if (onGround && belt != null) {
            int bi = Math.max(0, Math.min(length - 1, zi(pz))) * W + Math.max(0, Math.min(W - 1, xi(px)));
            switch (belt[bi]) { case 1: ex = BELT_SPEED; break; case 2: ex = -BELT_SPEED; break; case 3: ez = BELT_SPEED; break; case 4: ez = -BELT_SPEED; break; default: break; }
        }
        ex += windPush(pz, time);
        // spostamento: un asse alla volta, fermandosi contro blocchi e casse più alti dei piedi
        float nx = px + (vx + ex) * dt;
        if (blocked(nx, pz, py)) vx = 0; else px = nx;
        float nz = pz + (vz + ez) * dt;
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
        if (!onGround && quick != null) sinkT = Math.max(0, sinkT - dt * 3f);
        if (springBoost > 0) springBoost -= dt;
        if (bounceBoost > 0) bounceBoost -= dt;
        if (onGround) {                                  // fiore a molla: rimbalzo alto
            if (crateKind != null && g >= 1f && g < 1.5f) landCrate();
            if (quick != null) {
                if (quickAt(px, pz)) { if ((sinkT += dt) > SINK_TIME) { sinkT = 0; die(); return; } }
                else sinkT = Math.max(0, sinkT - dt * 3f);
            }
            if (raftIdx != null) {                           // salire su una zattera la fa affondare poco dopo
                int rc = raftIdx[Math.max(0, Math.min(length - 1, zi(pz))) * W + Math.max(0, Math.min(W - 1, xi(px)))];
                if (rc >= 0 && rafts.get(rc)[2] < 0) rafts.get(rc)[2] = time;
            }
            for (int i = 0; i < springs.size(); i++) {
                float[] sp2 = springs.get(i);
                if (Math.abs(sp2[1] - pz) > 0.8f || Math.abs(sp2[0] - px) > 0.62f || Math.abs(sp2[1] - pz) > 0.62f) continue;
                vy = SPRING_V;
                springDx = (float) Math.sin(yaw) * SPRING_H;              // il fiore lancia in avanti, verso dove guarda il robot
                springDz = (float) Math.cos(yaw) * SPRING_H;
                vx = springDx;
                vz = springDz;
                onGround = false;
                coyote = jumpBuffer = 0;
                airJumps = 1;
                springBoost = 1.2f;
                springIdx = i;
                springAt = time;
                break;
            }
        }
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
        for (float[] t : thorns) if (Math.abs(t[1] - pz) < 3.5f) return;
        for (float[] t : springs) if (Math.abs(t[1] - pz) < 2.5f) return;
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
        float feet = e.type == Enemy3.CROW ? 1e9f : e.flies() ? e.baseY - 0.75f : floor;      // il corvo sale sopra le terrazze: lo ferma solo il muro
        for (int cz = zi(z - ENEMY_R); cz <= zi(z + ENEMY_R); cz++) {
            for (int cx = xi(x - ENEMY_R); cx <= xi(x + ENEMY_R); cx++) {
                int h = cell(cx, cz);
                if (h == 99) return true;
                if (h != PIT && h > feet + 0.05f) return true;
            }
        }
        if (!e.flies()) {
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
            float speed = e.type == Enemy3.HOG ? ENEMY_SPEED * 0.8f : e.type == Enemy3.CROW ? ENEMY_SPEED * 2.7f : e.type == Enemy3.WHIRL ? ENEMY_SPEED * 1.7f
                    : e.type == Enemy3.STORM ? ENEMY_SPEED * 0.7f : e.type == Enemy3.SNAIL ? (e.mode == 1 ? 0f : ENEMY_SPEED * (e.mode == 2 ? 0.65f : 0.4f)) : e.type == Enemy3.PENGUIN ? ENEMY_SPEED * 2.3f
                    : e.type == Enemy3.SALAMANDER ? (e.mode == 0 ? ENEMY_SPEED * 0.8f : 0f) : e.skin == Enemy3.SKIN_CRAB ? ENEMY_SPEED * 1.8f : ENEMY_SPEED;
            if (e.type >= Enemy3.MOLE && stepNew3(e, dt)) continue;
            if (e.type == Enemy3.ACORN) {                       // la ghianda aspetta ferma, poi balza in avanti
                e.hopT += dt;
                float t = e.hopT % 0.85f;                       // balzi ravvicinati e rapidi
                if (t < 0.25f) {
                    e.y = e.baseY;
                    speed = 0;
                } else {
                    float u = (t - 0.25f) / 0.6f;
                    e.y = e.baseY + 1.5f * 4f * u * (1f - u);
                    speed = ENEMY_SPEED * 2.1f;
                }
            } else if (e.type == Enemy3.CROW) {
                // vola bassa rispetto a ciò che ha sotto: segue il profilo del terreno (anche le terrazze davanti a sé) senza attraversarlo
                float ax = e.alongZ ? e.x : e.x + e.dir * 1.3f, az = e.alongZ ? e.z + e.dir * 1.3f : e.z;
                float gnd = Math.max(0f, Math.max(topAt(e.x, e.z), topAt(Math.max(-HALF, Math.min(HALF, ax)), az)));
                if (Float.isNaN(e.floor)) e.t = gnd;
                e.t = gnd > e.t ? gnd : Math.max(gnd, e.t - 4f * dt);        // sale subito, scende gradualmente
                // sale e scende in picchiata fino all'altezza del robot (e poco sopra la sua testa), poi risale
                float mid = Math.max(0.95f, Math.min(e.baseY, 1.7f) * 0.65f), amp = mid - 0.15f;
                e.y = e.t + mid + amp * (float) Math.sin(e.anim * 2.7f);
            } else if (e.type == Enemy3.STORM) {                // la nuvola vaga lenta, poi si ferma, si carica e scaglia un fulmine verso il basso
                e.hopT += dt;
                e.y = e.baseY + 0.2f * (float) Math.sin(e.anim * 1.8f);
                if (Enemy3.stormPhase(e.hopT) != 0) speed = 0;
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
            boolean snail = e.type == Enemy3.SNAIL;
            if (e.grace > 0) e.grace -= dt;
            float ax = Math.abs(e.x - px), az = Math.abs(e.z - pz);
            boolean vertical = py < e.y + 0.6f && py + PLAYER_H > e.y;
            boolean spinNear = spinActive && ax < 0.75f + SPIN_REACH && az < 0.75f + SPIN_REACH && vertical;
            if (spinNear && snail && e.mode == 0 && !(vy < 0 && py > e.y + 0.25f)) continue;      // la giravolta contro il guscio non fa male né a lei né al robot
            if (snail && e.mode == 1 && !spinNear) continue;      // chiusa nel guscio è innocua
            if (spinNear && !(snail && e.mode == 0)) {   // la lumaca col guscio è invulnerabile; chiusa nel guscio la giravolta la uccide
                if (e.grace > 0) continue;
                e.flung = true;                       // la giravolta lo fa volare via
                float l = Math.max(0.01f, (float) Math.sqrt((e.x - px) * (e.x - px) + (e.z - pz) * (e.z - pz)));
                e.vx = (e.x - px) / l * 6f;
                e.vz = (e.z - pz) / l * 6f + 3f;
                e.vy = 8f;
                score += 100;
            } else if (ax < 0.75f && az < 0.75f && vertical) {
                if (stompable3(e) && vy < 0 && py > e.y + 0.25f) {     // schiacciato dall'alto (il riccio punge)
                    if (snail && e.mode < 2) { e.mode = 1; e.t = 4f; } else e.flatTime = 0.45f;
                    vy = jump ? 9f : 6.5f;
                    airJumps = 1;
                    score += snail && e.mode == 1 ? 0 : 100;
                } else if (!(snail && e.grace > 0)) {
                    hurt();
                }
            } else if (e.type == Enemy3.STORM && Enemy3.stormPhase(e.hopT) == 2 && ax < 0.55f && az < 0.55f && py < e.y) {
                hurt();                                  // il fulmine colpisce tutta la colonna sotto la nuvola            } else if (e.type == Enemy3.SALAMANDER && e.mode == 2 && flameHits(e)) {
                hurt();                                  // il getto di fuoco davanti alla salamandra
            }
        }
    }

    /** Lista dei proiettili (palle di neve): {x, y, z, vx, vy, vz}. */
    public final List<float[]> shots = new ArrayList<float[]>();

    private boolean stompable3(Enemy3 e) {
        switch (e.type) {
            case Enemy3.HOG: case Enemy3.WHIRL: case Enemy3.CHESTNUT: case Enemy3.LAPILLO: case Enemy3.STAR: case Enemy3.SUNNY: return false;
            case Enemy3.PLANT: return e.mode != 2;
            case Enemy3.SNAIL: return true;
            default: return true;
        }
    }

    /** true se il getto di fuoco della salamandra (due caselle davanti al muso) tocca il giocatore. */
    public boolean flameHits(Enemy3 e) {
        float fx = e.alongZ ? 0 : e.dir, fz = e.alongZ ? e.dir : 0;
        float cx = e.x + fx * 1.2f, cz = e.z + fz * 1.2f;
        return Math.abs(px - cx) < (fx != 0 ? 1.15f : 0.6f) && Math.abs(pz - cz) < (fz != 0 ? 1.15f : 0.6f) && py < e.y + 1.0f && py + PLAYER_H > e.y;
    }

    private void touch(Enemy3 e, boolean stompable) {
        float ax = Math.abs(e.x - px), az = Math.abs(e.z - pz);
        boolean vertical = py < e.y + 0.6f && py + PLAYER_H > e.y;
        if (spinActive && ax < 0.75f + SPIN_REACH && az < 0.75f + SPIN_REACH && vertical) {
            e.flung = true;
            float l = Math.max(0.01f, (float) Math.sqrt((e.x - px) * (e.x - px) + (e.z - pz) * (e.z - pz)));
            e.vx = (e.x - px) / l * 6f;
            e.vz = (e.z - pz) / l * 6f + 3f;
            e.vy = 8f;
            score += 100;
        } else if (ax < 0.75f && az < 0.75f && vertical) {
            if (stompable && vy < 0 && py > e.y + 0.25f) {
                e.flatTime = 0.45f;
                vy = jump ? 9f : 6.5f;
                airJumps = 1;
                score += 100;
            } else {
                hurt();
            }
        }
    }

    /**
     * Comportamento dei nemici nuovi. Restituisce true se il nemico si muove da sé e il passo generico va saltato; i camminatori
     * (lumaca, pinguino, salamandra) restituiscono false e usano il pattugliamento comune.
     */
    private boolean stepNew3(Enemy3 e, float dt) {
        e.t += dt;
        float dxp = px - e.x, dzp = pz - e.z;
        float floorY = Float.isNaN(e.floor) ? e.y : e.floor;
        switch (e.type) {
            case Enemy3.CHESTNUT: {                            // pende immobile, trema quando si passa sotto, cade e rotola
                if (Float.isNaN(e.floor)) { e.floor = Math.max(0, topAt(e.x, e.z)); e.y0 = e.y; }
                if (e.mode == 0) {
                    e.y = e.y0;
                    if (Math.abs(dzp) < 4.2f && dzp > -1f && Math.abs(dxp) < 2.2f) { e.mode = 1; e.t = 0; }
                } else if (e.mode == 1) {
                    if (e.t > 0.45f) { e.mode = 2; e.vy = 0; }
                } else if (e.mode == 2) {
                    e.vy -= GRAVITY * 0.8f * dt;
                    e.y += e.vy * dt;
                    if (e.y <= e.floor + 0.3f) { e.y = e.floor + 0.3f; e.mode = 3; e.t = 0; e.dir = dzp >= 0 ? 1 : -1; }
                } else {
                    float nz = e.z + e.dir * 4.2f * dt;
                    float h = topAt(e.x, nz);
                    if (h < e.floor - 0.05f || h > e.floor + 0.05f) e.dir = -e.dir; else e.z = nz;
                    if (e.t > 3.2f) { e.dead = true; return true; }
                }
                if (state == PLAYING) touch(e, false);
                return true;
            }
            case Enemy3.OWL: {                                 // ronda alta, poi si tuffa sul giocatore e risale
                if (Float.isNaN(e.floor)) e.floor = e.y;
                if (e.mode == 0) {
                    e.y = e.baseY + 0.3f * (float) Math.sin(e.anim * 2f);
                    float n = e.alongZ ? e.z + e.dir * ENEMY_SPEED * 0.9f * dt : e.x + e.dir * ENEMY_SPEED * 0.9f * dt;
                    if (e.alongZ) { e.z = n; if (e.z > e.hi) { e.z = e.hi; e.dir = -1; } else if (e.z < e.lo) { e.z = e.lo; e.dir = 1; } }
                    else { e.x = n; if (e.x > e.hi) { e.x = e.hi; e.dir = -1; } else if (e.x < e.lo) { e.x = e.lo; e.dir = 1; } }
                    if (Math.abs(dzp) < 4f && Math.abs(dxp) < 1.5f && py < e.baseY - 0.8f) { e.mode = 1; e.t = 0; e.x0 = px; e.z0 = pz + 1.2f; }
                } else if (e.mode == 1) {                      // allerta: si abbassa in avanti
                    e.y -= 3.2f * dt;
                    float k = Math.min(1f, 3.5f * dt);
                    e.x += (e.x0 - e.x) * k; e.z += (e.z0 - e.z) * k;
                    if (e.y <= Math.max(floorY, 0.5f) || e.t > 1.1f) { e.mode = 2; e.t = 0; }
                } else {
                    e.y += 2.2f * dt;
                    if (e.y >= e.baseY) { e.y = e.baseY; e.mode = 0; }
                }
                if (state == PLAYING) touch(e, true);
                return true;
            }
            case Enemy3.SNOWMAN: {                             // fermo, si volta e lancia palle di neve ogni 2,3 s
                if (Float.isNaN(e.floor)) e.floor = e.y;
                e.dir = dzp >= 0 ? 1 : -1;
                if (Math.abs(dzp) < 11f && dzp > -2f && e.t > 2.3f) {
                    e.t = 0;
                    float l = Math.max(0.5f, (float) Math.sqrt(dxp * dxp + dzp * dzp));
                    shots.add(new float[] {e.x, e.y + 1.3f, e.z, dxp / l * 5.5f, 3.2f, dzp / l * 5.5f, e.skin});
                }
                if (state == PLAYING) touch(e, true);
                return true;
            }
            case Enemy3.LAPILLO: {                             // schizza dalla lava (o dal bordo) ritmicamente: nascosto, poi un balzo
                float per = e.lo > 0 ? e.lo : 2.6f, hgt = e.hi > 0 ? e.hi : 3f;
                if (Float.isNaN(e.floor)) e.floor = e.y;
                float c = e.t % per, d = 0.9f;
                if (c < per - d) { e.mode = 0; e.y = e.baseY - 3f; return true; }
                float u = (c - (per - d)) / d;
                e.mode = 1;
                e.y = e.baseY + hgt * 4f * u * (1f - u) - 0.3f;
                if (state == PLAYING && e.y > e.baseY - 0.2f) touch(e, false);
                return true;
            }
            case Enemy3.FROG: {                                // balza verso il giocatore quando è vicino
                if (Float.isNaN(e.floor)) { e.floor = e.y; e.y0 = e.y; }
                if (e.mode == 0) {
                    e.y = e.y0;
                    if (e.t > 1.4f && Math.abs(dzp) < 8f && Math.abs(dxp) < 3f) {
                        e.mode = 1; e.t = 0;
                        float l = Math.max(0.5f, (float) Math.sqrt(dxp * dxp + dzp * dzp));
                        e.vx = dxp / l * 3.6f; e.vz = dzp / l * 3.6f; e.dir = dzp >= 0 ? 1 : -1;
                    }
                } else {
                    float u = e.t / 0.7f;
                    if (u >= 1f) { e.mode = 0; e.t = 0; e.y = e.y0; }
                    else {
                        float nx = e.x + e.vx * dt, nz = e.z + e.vz * dt;
                        if (!enemyBlocked(e, nx, nz)) { e.x = nx; e.z = nz; }
                        e.y = e.y0 + 1.6f * 4f * u * (1f - u);
                    }
                }
                if (state == PLAYING) touch(e, true);
                return true;
            }
            case Enemy3.PLANT: {                               // chiusa 1,4 s, trema 0,4 s, morde 1,2 s
                float c = e.t % 3.0f;
                e.mode = c < 1.4f ? 0 : c < 1.8f ? 1 : 2;
                e.dir = dzp >= 0 ? 1 : -1;
                if (state == PLAYING) touch(e, e.mode != 2);
                return true;
            }
            case Enemy3.STAR: {                                // stella cadente: attraversa il corridoio in diagonale e riparte
                e.x += (e.lo != 0 ? e.lo : 3.2f) * dt; e.y += (e.hi != 0 ? e.hi : -4.6f) * dt;
                if (e.y < -3f || e.x < -6f || e.x > 6f) { e.x = e.x0; e.y = e.y0; e.z = e.z0; }
                if (state == PLAYING) touch(e, false);
                return true;
            }
            case Enemy3.SUNNY: {                               // solleone: gira attorno al suo centro nel piano della sezione
                float r = e.hi > 0 ? e.hi : 1.6f, wv = e.lo != 0 ? e.lo : 2.2f;
                e.x = e.x0 + r * (float) Math.cos(e.t * wv);
                e.y = e.y0 + r * (float) Math.sin(e.t * wv) * 0.8f;
                if (state == PLAYING) touch(e, false);
                return true;
            }
            case Enemy3.SNAIL: {                               // nel guscio resta fermo e innocuo
                if (e.mode == 1) { e.t -= 2 * dt; if (e.t <= 0) e.mode = 0; }
                return false;
            }
            default: return false;
        }
    }

    private void stepShots(float dt) {
        for (int i = shots.size() - 1; i >= 0; i--) {
            float[] s = shots.get(i);
            s[4] -= 14f * dt;
            s[0] += s[3] * dt; s[1] += s[4] * dt; s[2] += s[5] * dt;
            float top = topAt(s[0], s[2]);
            boolean hit = state == PLAYING && Math.abs(s[0] - px) < 0.55f && Math.abs(s[2] - pz) < 0.55f && s[1] > py - 0.15f && s[1] < py + PLAYER_H + 0.15f;
            if (hit) hurt();
            if (hit || s[1] < Math.max(0, top) - 0.1f || Math.abs(s[2] - pz) > 30f) shots.remove(i);
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
            showToast("Chiave trovata!");
        }
        if (spinActive) {                             // la giravolta rompe le casse vicine
            float m = R + SPIN_REACH;
            for (int cz = zi(pz - m); cz <= zi(pz + m); cz++) {
                for (int cx = xi(px - m); cx <= xi(px + m); cx++) {
                    if (cx < 0 || cx >= W || cz < 0 || cz >= length) continue;
                    int k = cz * W + cx;
                    if (crate[k] && py < 1.2f) {
                        byte kind = crateKind == null ? C_WOOD : crateKind[k];
                        if (kind == C_BOUNCE) continue;
                        if (kind == C_TNT || kind == C_NITRO) { explode(k); continue; }    // la giravolta su una cassa esplosiva: scoppia subito
                        breakCrate(k);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ isola tropicale

    /** Cassa rotta (salto o giravolta): gemma, punti, e se è di ripartenza si riparte da qui. */
    private void breakCrate(int k) {
        crate[k] = false;
        gems++;
        score += 200;
        float cx = k % W - HALF + 0.5f, cz = k / W + 0.5f;
        pops.add(new float[] {cx, 0.8f, cz, 0});
        if (crateKind != null && crateKind[k] == C_CHECK) {
            cpX = cx; cpZ = cz;
            if (!Float.isNaN(fireZ)) fireZ = Math.min(fireZ, cz - 14f);
            showToast("Si riparte da qui!");
        }
        checkGem();
    }

    /** Esplosione di una cassa: rompe le vicine (le esplosive scoppiano a catena), ferisce il robot e i nemici nel raggio. */
    private void explode(int k) {
        if (!crate[k]) return;
        crate[k] = false;
        gems++;
        checkGem();
        float cx = k % W - HALF + 0.5f, cz = k / W + 0.5f;
        booms.add(new float[] {cx, cz, 0});
        float dx = px - cx, dz = pz - cz;
        if (dx * dx + dz * dz < BOOM_R * BOOM_R && py < 2.2f) hurt();
        for (Enemy3 e : enemies) {
            float ex = e.x - cx, ez = e.z - cz;
            if (!e.dead && ex * ex + ez * ez < BOOM_R * BOOM_R) { e.dead = true; e.flung = true; e.vy = 6f; score += 100; }
        }
        int kx = k % W, kz = k / W;
        for (int z = kz - 1; z <= kz + 1; z++) for (int x = kx - 1; x <= kx + 1; x++) {
            if (x < 0 || x >= W || z < 0 || z >= length) continue;
            int j = z * W + x;
            if (!crate[j] || crateKind[j] == C_BOUNCE) continue;
            if (crateKind[j] == C_TNT || crateKind[j] == C_NITRO) { if (crateT[j] <= 0 || crateT[j] < TNT_FUSE - 0.15f) crateT[j] = TNT_FUSE - 0.15f; }   // a catena, con un attimo di ritardo
            else breakCrate(j);
        }
    }

    /** Il robot è atterrato sopra una cassa: la rompe (legno, ripartenza), ci rimbalza (molla), la accende (TNT) o la fa scoppiare. */
    private void landCrate() {
        int best = -1; float bd = 9;
        for (int cz = zi(pz - R); cz <= zi(pz + R); cz++) for (int cx = xi(px - R); cx <= xi(px + R); cx++) {
            if (cx < 0 || cx >= W || cz < 0 || cz >= length) continue;
            int k = cz * W + cx;
            if (!crate[k] || height[k] != 0) continue;
            float d = Math.abs(cx - HALF + 0.5f - px) + Math.abs(cz + 0.5f - pz);
            if (d < bd) { bd = d; best = k; }
        }
        if (best < 0) return;
        switch (crateKind[best]) {
            case C_BOUNCE:
                vy = 12.5f; crateT[best] = time; bounceBoost = 0.6f;     // stessa altezza che si tenga premuto il salto o no
                break;
            case C_TNT:
                if (crateT[best] <= 0) crateT[best] = 0.001f;
                vy = 7.5f;
                break;
            case C_NITRO:
                explode(best);
                return;
            default:
                breakCrate(best);
                vy = 7.5f;
                break;
        }
        onGround = false;
        coyote = jumpBuffer = 0;
        airJumps = 1;
    }

    private void stepIsland(float dt) {
        for (int i = booms.size() - 1; i >= 0; i--) if ((booms.get(i)[2] += dt) > 0.7f) booms.remove(i);
        if (crateKind != null) {
            int z0 = Math.max(0, zi(pz) - 30), z1 = Math.min(length - 1, zi(pz) + 30);
            for (int k = z0 * W; k < (z1 + 1) * W; k++) {
                if (!crate[k]) continue;
                if (crateKind[k] == C_TNT || crateKind[k] == C_NITRO) {
                    if (crateT[k] > 0 && (crateT[k] += dt) >= TNT_FUSE) explode(k);
                }
                if (crateKind[k] == C_NITRO && crate[k]) {      // l'instabile scoppia anche solo sfiorandola di lato
                    float dx = Math.abs(k % W - HALF + 0.5f - px), dz = Math.abs(k / W + 0.5f - pz);
                    if (dx < 0.5f + R + 0.04f && dz < 0.5f + R + 0.04f && py < 0.98f) explode(k);
                }
            }
        }
        for (float[] l : lasers) {                           // barriera laser accesa: ferisce chi la attraversa senza saltarla
            if (laserOn(l, time) < 1f || Math.abs(l[0] + 0.5f - pz) > 0.5f + R * 0.6f || py > 0.85f) continue;
            if (l[3] >= 0 && xi(px) == (int) l[3]) continue;
            hurt();
        }
        for (float[] c : crushers) {                         // pestello: schiaccia chi è sotto quando cade
            if (Math.abs(c[1] + 0.5f - pz) > 1.5f) continue;
            float bx = c[0] - HALF + 0.5f, bz = c[1] + 0.5f, bot = Math.max(0, height[(int) c[1] * W + (int) c[0]]) + crusherBottom(c, time);
            if (Math.abs(px - bx) < 0.5f + R - 0.05f && Math.abs(pz - bz) < 0.5f + R - 0.05f && bot < py + PLAYER_H - 0.1f) hurt();
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

    /** Cerca un tratto piano e libero davanti al portone dove mettere la catapulta. */
    private void sealEnd() {
        sealed = false;
        int z = flagZ - 8;
        for (; z > flagZ - 30 && z > 4; z--) {
            boolean ok = true;
            for (int k = 0; k < 3 && ok; k++) for (int x = 0; x <= 2; x++) if (cell(x, z + k) != 0) ok = false;
            if (ok) break;
        }
        catZ = z;
        sealed = true;
    }

    private void stepGate(float dt) {
        if (batteryOwned && pz > flagZ - 6f && gateOpen < 1f) gateOpen = Math.min(1f, gateOpen + dt);
        else if (!batteryOwned && pz > flagZ - 4f && toastTime <= 0) {
            toast = "Serve la chiave per aprire il portone";
            toastTime = 2.4f;
        }
        if (state == PLAYING && gateOpen > 0.6f && pz >= flagZ + 3.4f) win();
        if (state == PLAYING && spin && !spinWas && onGround && Math.abs(px + 1.75f) < 1.0f && pz > catZ - 0.2f && pz < catZ + 1.6f && py < 0.3f) {
            launchT = 0;
            lx = px; ly = py; lz = pz;
            spinActive = false;
            spinCool = 0;
            vx = vy = vz = 0;
        }
    }

    private void stepLaunch(float dt) {
        float dur = 2.2f + Math.min(1.5f, (lz - 1.5f) / 60f);
        launchT += dt;
        float u = Math.min(1f, launchT / dur);
        px = lx * (1 - u);
        pz = lz + (1.5f - lz) * u;
        py = ly + 14f * u * (1f - u);
        yaw = (float) Math.PI;
        invuln = Math.max(invuln, 0.3f);
        if (u >= 1f) {
            launchT = -1;
            cpX = 0;
            cpZ = 1.5f;
            place();
            invuln = 1.5f;
            if (!Float.isNaN(fireZ)) fireZ = Math.min(fireZ, cpZ - 14f);
        }
    }

    public boolean launching() { return launchT >= 0; }

    private void win() {
        state = WON;
        stateTime = 0;
        score += 500;
        vx = vz = 0;
        spinActive = false;
    }
}
