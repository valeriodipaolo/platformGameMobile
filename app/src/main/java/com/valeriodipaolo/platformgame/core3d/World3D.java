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
    /** Larghezza standard del sentiero (corsie) nei livelli 3D; il labirinto ne usa una sua, più ampia. */
    public static final int W5 = 5;
    /** Larghezza del sentiero di questo livello (corsie) e sua metà: di solito 5, il labirinto è più largo. */
    public int W = W5;
    public float HALF = W5 / 2f;
    public static final float HALF5 = W5 / 2f;

    /** Corsia della x (sentiero standard largo cinque). */
    public static int xi5(float x) { return (int) Math.floor(x + HALF5); }

    /** Imposta la larghezza del sentiero: va fatto prima di allocare le caselle del livello. */
    public void setWidth(int w) { W = w; HALF = w / 2f; }
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
    public static final byte C_WOOD = 0, C_TNT = 1, C_NITRO = 2, C_BOUNCE = 3, C_CHECK = 4, C_STEEL = 5;
    /** Esplosiva accesa: secondi dall'accensione (0 = spenta). Cassa a molla: istante dell'ultimo rimbalzo. */
    public float[] crateT;
    public static final float TNT_FUSE = 3f, BOOM_R = 1.75f;
    /** Esplosioni in corso: {x, z, età}. */
    public final List<float[]> booms = new ArrayList<float[]>();
    /**
     * Masso del tempio che distrugge: per ogni casella abbattibile l'altezza originale (0 = non abbattibile) e la quota del
     * pavimento su cui resta quando il masso la travolge. I detriti {x, y, z, vx, vy, vz, angolo, velocità angolare, vita, misura}
     * volano via e rimbalzano; i pestelli travolti si ricordano per rimetterli se il robot riparte da più indietro.
     */
    public byte[] breakH, breakFloor;
    /**
     * Rampa a tutta larghezza (il tempio): tra rampZ0 e rampZ1 il pavimento sale senza gradini da rampH0 a rampH1. Le caselle della
     * rampa tengono in height la quota intera più bassa; quella vera, continua, la dà rampY.
     */
    public float rampZ0 = Float.NaN, rampZ1, rampH0, rampH1;
    /**
     * Il masso si incaglia quando il suo filo arriva a boulderStop (tra due pilastri che non riesce a sfondare); da lì parte la breve
     * scena in cui la camera torna dietro al robot: per CINE secondi il giocatore non ha i comandi.
     */
    public float boulderStop = Float.NaN, cineT = -1f;
    public static final float CINE = 1.9f;
    public boolean[] rampCell;
    public final List<float[]> debris = new ArrayList<float[]>();
    private final List<float[]> smashedCrushers = new ArrayList<float[]>();
    private int smashedTo;
    public float shake;
    /** Raggio dello scoppio del riccio di castagno. */
    static final float BURR_R = 1.75f;
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
    /** I nastri che spingono di lato vanno più piano: a 3,4 m/s portavano il robot giù dal bordo prima di poter reagire. */
    public static final float BELT_SIDE = 1.6f;
    /** Barriere laser: {z, periodo, fase, colonna spenta (-1 nessuna)}; accese nella prima metà del periodo, all'altezza delle ginocchia. */
    public final List<float[]> lasers = new ArrayList<float[]>();
    /** Sabbie mobili per casella: rallentano, il salto è più corto, e chi ci resta troppo affonda. */
    public boolean[] quick;
    public float sinkT;
    public static final float SINK_TIME = 3.0f;
    /** Raffiche di vento: {z0, z1, periodo, fase, verso (+1 destra, -1 sinistra)}. */
    public final List<float[]> winds = new ArrayList<float[]>();
    public static final float GUST = 1.7f, GUST_PUSH = 1.3f;     // raffica leggera: sposta di poco, si corregge senza fatica
    /** Tratti in carrello: {z0, z1}; il carrello corre da solo, si sterza e si salta. */
    public final List<float[]> carts = new ArrayList<float[]>();
    /** Tratti a labirinto {z0, z1}: muri alti, la camera sale e segue il robot nel corridoio. */
    public final List<float[]> mazes = new ArrayList<float[]>();
    /** Velocità del carrello: parte da CART_SPEED e cresce fino a CART_SPEED + CART_RAMP alla fine del tratto. */
    public static final float CART_SPEED = 10f, CART_RAMP = 3f, CART_X = 1.05f;
    public int flagZ;
    /** Portone di fine livello: si apre (0..1) da solo all'arrivo. */
    public float gateOpen;
    private boolean sealed;
    public final List<float[]> gemsList = new ArrayList<float[]>();
    public final List<Enemy3> enemies = new ArrayList<Enemy3>();
    /** Gemme che escono dalle casse rotte: {x, y, z, età}. */
    public final List<float[]> pops = new ArrayList<float[]>();
    /**
     * Gettone d'oro segreto, uno per livello: in un punto nascosto o difficile {x, y, z} (coin), dentro una cassa qualunque
     * (coinCrate = indice della casella, -1 nessuna) oppure addosso a un nemico (coinFoeIdx = indice in enemies, al caricamento coinFoe).
     */
    public float[] coin;
    public int coinCrate = -1, coinFoeIdx = -1;
    public Enemy3 coinFoe;
    /** Gettone che schizza fuori appena trovato: {x, y, z, età}, oppure null. */
    public float[] coinPop;
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
    public boolean coinOwned, gemOwned;
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
        crateKind = null; crateT = null; booms.clear(); crushers.clear(); rafts.clear(); breakH = null; breakFloor = null; rampCell = null; rampZ0 = Float.NaN; boulderStop = Float.NaN; cineT = -1f; debris.clear(); smashedCrushers.clear(); smashedTo = 0; shake = 0; raftIdx = null; camBackUntil = -1f;
        belt = null; lasers.clear(); quick = null; sinkT = 0; winds.clear(); carts.clear(); mazes.clear();
        coin = null; coinCrate = -1; coinFoeIdx = -1; coinFoe = null; coinPop = null;
        setWidth(levelNo == 26 ? Levels3C.MAZE_W : W5);                 // il labirinto è largo, gli altri livelli hanno cinque corsie
        if (levelNo > 37) Levels3W.build(this, levelNo); else if (levelNo > 34) Levels3J.build(this, levelNo); else if (levelNo > 24) Levels3C.build(this, levelNo); else if (levelNo > 12) Levels3B.build(this, levelNo); else if (levelNo == 12) LevelTwelve.build(this); else if (levelNo == 10) LevelTen.build(this); else if (levelNo == 8) LevelEight.build(this); else if (levelNo == 6) LevelSix.build(this); else if (levelNo == 4) LevelFour.build(this); else LevelTwo.build(this);
        if (!rafts.isEmpty()) {                          // prima delle rifiniture: le zattere a galla contano come terreno
            raftIdx = new int[length * W];
            java.util.Arrays.fill(raftIdx, -1);
            for (int i = 0; i < rafts.size(); i++) raftIdx[(int) rafts.get(i)[1] * W + (int) rafts.get(i)[0]] = i;
        }
        LevelFix.apply(this);
        gateOpen = 0;
        sealed = true;
        noSidePaths();
        gemsOffThorns();
        seasonSkins();
        if (crateKind == null) crateKind = new byte[crate.length];      // le casse dell'isola in tutti i livelli: si rompono anche saltandoci sopra
        reinforce();
        Coins3D.place(this);
        if (coinFoeIdx >= 0 && coinFoeIdx < enemies.size()) coinFoe = enemies.get(coinFoeIdx);
        gemsList.removeIf(g -> g[2] < 1.5f + 0.6f && g[1] < 2.5f);     // niente gemme dietro (o sotto) il punto di partenza: si parte e si va avanti
        gemTotal = gemsList.size();
        for (int k = 0; k < crate.length; k++) if (crate[k] && k != coinCrate && (crateKind == null || crateKind[k] != C_BOUNCE) && !hazardOnly(k)) gemTotal++;
        if (crateKind != null) crateT = new float[crate.length];
        coinOwned = gemOwned = false;
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
        if (cineLocked()) { joyX = 0; joyY = 0; jump = false; spin = false; }
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

    public int xi(float x) { return (int) Math.floor(x + HALF); }
    static int zi(float z) { return (int) Math.floor(z); }

    /**
     * Buca di una casella chiusa tutt'intorno (come la nicchia del gettone): l'impronta dei piedi è più larga della buca, quindi
     * camminandoci sopra si resterebbe appoggiati ai bordi. Se il centro del robot è sopra la buca, lo si accompagna verso il suo
     * centro e ci cade dentro. Solo per buche con il fondo (non i burroni) circondate su tutti i lati da bordi alti almeno quanto
     * i piedi: così non si scivola dai gradini né giù dal ciglio dei burroni.
     */
    private void pocket(float dt) {
        int cx = xi(px), cz = zi(pz);
        if (cx < 0 || cx >= W || cz < 1 || cz >= length - 1) return;
        int h = cell(cx, cz);
        if (h == PIT || h >= 99 || isRamp(cx, cz) || h > py - 0.9f) return;
        int[][] nb = {{cx - 1, cz}, {cx + 1, cz}, {cx, cz - 1}, {cx, cz + 1}};
        for (int[] n : nb) {
            int hn = n[0] < 0 || n[0] >= W ? 99 : cell(n[0], n[1]);
            if (hn == PIT || hn < py - 0.05f) return;
        }
        float tx = cx - HALF + 0.5f, tz = cz + 0.5f, step = 6f * dt;
        px += Math.max(-step, Math.min(step, tx - px));
        pz += Math.max(-step, Math.min(step, tz - pz));
    }

    /** Quota della rampa al punto z (fuori dalla rampa: la quota del suo estremo più vicino). */
    public float rampY(float z) {
        float u = (z - rampZ0) / (rampZ1 - rampZ0);
        return rampH0 + (rampH1 - rampH0) * Math.max(0f, Math.min(1f, u));
    }

    /** Trasforma le righe [z0, z1) in rampa (le buche restano buche). */
    public void makeRamp(int z0, int z1, float h0, float h1) {
        rampZ0 = z0; rampZ1 = z1; rampH0 = h0; rampH1 = h1;
        if (rampCell == null) rampCell = new boolean[height.length];
        for (int z = z0; z < z1; z++) for (int x = 0; x < W; x++) {
            int i = z * W + x;
            if (height[i] != 0) continue;                    // buche e ostacoli restano quello che sono
            rampCell[i] = true;
            height[i] = (int) Math.floor(rampY(z) + 1e-4f);
        }
    }

    /** Quota del pavimento al centro della casella (sulla rampa quella continua). */
    public float floorAt(int cx, int cz) {
        if (isRamp(cx, cz)) return rampY(cz + 0.5f);
        return Math.max(0, height[cz * W + cx]);
    }

    /** È una casella di rampa? */
    public boolean isRamp(int cx, int cz) {
        return rampCell != null && cx >= 0 && cx < W && cz >= 0 && cz < length && rampCell[cz * W + cx];
    }

    /** Quota della superficie della casella (cx, cz) nel punto z: continua sulla rampa, a gradino altrove. */
    private float surface(int cx, int cz, float z) {
        int h = cell(cx, cz);
        if (h != PIT && h != 99 && isRamp(cx, cz)) return rampY(Math.max(cz, Math.min(cz + 1f, z)));
        return h;
    }

    /**
     * Quota su cui poggiano i piedi: la più alta sotto l'impronta tra quelle non più alte di poco dei piedi. Accostati a un muro
     * (l'ingombro contro i muri è più stretto dell'impronta) il muro non conta: si resta sul pavimento.
     */
    public float footAt(float x, float z, float feet) {
        float best = PIT;
        for (int cz = zi(z - R); cz <= zi(z + R); cz++) {
            for (int cx = xi(x - R); cx <= xi(x + R); cx++) {
                int c = cell(cx, cz);
                if (c == 99 || c == PIT) continue;
                float h = surface(cx, cz, z);
                if (h > best && h <= feet + 0.6f) best = h;
            }
        }
        return best == PIT ? -1000f : best;
    }

    /** Quota più alta sotto l'impronta del giocatore; molto negativa se sotto c'è solo vuoto. */
    public float topAt(float x, float z) {
        float best = PIT;
        for (int cz = zi(z - R); cz <= zi(z + R); cz++) {
            for (int cx = xi(x - R); cx <= xi(x + R); cx++) {
                int c = cell(cx, cz);
                if (c == 99 || c == PIT) continue;
                float h = surface(cx, cz, z);
                if (h > best) best = h;
            }
        }
        return best == PIT ? -1000f : best;
    }

    /**
     * Ingombro del robot contro muri e blocchi: un po' più stretto del raggio con cui poggia i piedi, così nei varchi larghi una
     * casella si passa anche senza essere perfettamente centrati (una leggera compenetrazione con lo spigolo non si nota).
     */
    public static final float R_WALL = 0.2f;
    /** Di quanto, al massimo, il robot viene accompagnato di lato verso un varco quando sbatte contro il suo spigolo. */
    static final float NUDGE = 0.4f, NUDGE_SPEED = 4f;

    private boolean blocked(float x, float z, float feet) {
        final float R = R_WALL;
        for (int cz = zi(z - R); cz <= zi(z + R); cz++) {
            for (int cx = xi(x - R); cx <= xi(x + R); cx++) {
                int h = cell(cx, cz);
                if (h == PIT) continue;
                if (h != 99 && isRamp(cx, cz)) { if (rampY(Math.min(cz + 1f, z + R)) > feet + 0.3f) return true; continue; }   // la rampa si sale camminando
                if (h > feet + 0.05f) return true;
            }
        }
        return false;
    }

    /**
     * Il robot sbatte contro lo spigolo di un varco: se spostandolo di poco di lato (sull'altro asse) il passo sarebbe libero, lo si
     * accompagna verso il varco invece di fermarlo. Restituisce lo spostamento laterale di questo fotogramma (0 se non c'è varco).
     */
    private float slide(float x, float z, boolean alongZ, float dt) {
        float feet = py;
        for (float off = 0.05f; off <= NUDGE + 1e-4f; off += 0.05f) {
            for (int sgn = -1; sgn <= 1; sgn += 2) {
                float o = off * sgn;
                boolean free = alongZ ? !blocked(x + o, z, feet) && !blocked(px + o, pz, feet) : !blocked(x, z + o, feet) && !blocked(px, pz + o, feet);
                if (free) return Math.signum(o) * Math.min(off, NUDGE_SPEED * dt);
            }
        }
        return 0;
    }

    // ------------------------------------------------------------------ aggiornamento

    /** Durante la scena del masso incagliato i comandi sono tolti al giocatore. */
    public boolean cineLocked() { return cineT >= 0 && cineT < CINE; }

    /** Il masso si è incagliato: la camera può tornare dietro al robot. */
    public boolean boulderStuck() { return cineT >= 0; }

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
        if (coinPop != null) {
            coinPop[1] += 1.6f * dt;
            if ((coinPop[3] += dt) > 1.0f) coinPop = null;
        }
        if (state == PLAYING) {
            stepPlayer(dt);
            stepFire(dt);
            if (!debris.isEmpty() || shake > 0) stepDebris(dt);
            stepIsland(dt);
            stepEnemies(dt); stepShots(dt);
            pickups();
            stepGate(dt);
        } else if (state == DYING) {
            if (!debris.isEmpty() || shake > 0) stepDebris(dt);
            vy -= 30f * dt;
            py += vy * dt;
            stepEnemies(dt);
            if (stateTime > 1.6f) {
                if (lives > 0) {
                    hp = MAX_HP;
                    place();
                    invuln = 2.5f;
                    if (!Float.isNaN(fireZ)) fireZ = Math.min(fireZ, cpZ - 14f);
                    if (breakH != null) unsmash();
                } else {
                    state = GAME_OVER;
                    stateTime = 0;
                }
            }
        } else if (state == WON) {
            vy = Math.max(-MAX_FALL, vy - GRAVITY * dt);
            py += vy * dt;
            if (stateTime < 1.2f) { pz += vz * dt; yaw = 0; runPhase += vz * dt * 2.1f; }
            float g = footAt(px, pz, py);
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

    /** Le esplosive (dinamite) sono solo ostacoli, in tutti i livelli: farle scoppiare non dà gemme e non contano nel totale. */
    private boolean hazardOnly(int k) {
        return crateKind != null && (crateKind[k] == C_TNT || crateKind[k] == C_NITRO);
    }

    /** Una cassa di legno ogni tre diventa rinforzata: ci si sale sopra senza romperla, la apre solo la giravolta. */
    private void reinforce() {
        int n = 0;
        for (int k = 0; k < crate.length; k++) if (crate[k] && crateKind[k] == C_WOOD && n++ % 3 == 2) crateKind[k] = C_STEEL;
    }

    /** Velocità del carrello alla riga z: il ritmo cresce man mano che si avanza. */
    public float cartSpeed(float z) {
        for (float[] c : carts) if (z >= c[0] && z < c[1]) return CART_SPEED + CART_RAMP * Math.max(0f, Math.min(1f, (z - c[0]) / (c[1] - c[0])));
        return CART_SPEED;
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

    /** Il davanti del masso rispetto alla linea che uccide (il masso ha raggio 2,3 ed è disegnato poco dietro). */
    public static final float BOULDER_FRONT = 1.4f;

    /** Rende abbattibili dal masso tutte le caselle più alte del pavimento della loro riga. */
    public void makeBreakable(int z0, int z1) {
        if (breakH == null) { breakH = new byte[height.length]; breakFloor = new byte[height.length]; }
        for (int z = Math.max(0, z0); z < Math.min(length, z1); z++) {
            int floor = 99;                                   // pavimento: la casella più bassa della riga (il varco nei muri)
            for (int x = 0; x < W; x++) { int h = height[z * W + x]; if (h != PIT) floor = Math.min(floor, Math.max(0, h)); }
            if (floor == 99) continue;
            for (int x = 0; x < W; x++) {
                int i = z * W + x, h = height[i];
                if (h != PIT && h > floor && h < 99) { breakH[i] = (byte) h; breakFloor[i] = (byte) floor; }
            }
        }
    }

    /** Il masso travolge tutto quello che ha davanti fino a front: muri e colonne crollano in detriti, pestelli e nemici volano via. */
    private void smash(float front) {
        int to = (int) Math.floor(front);
        for (int z = Math.max(0, smashedTo); z <= Math.min(length - 1, to); z++) {
            for (int x = 0; x < W; x++) {
                int i = z * W + x;
                if (breakH == null || breakH[i] == 0 || height[i] <= breakFloor[i]) continue;
                int h = height[i], f = breakFloor[i];
                height[i] = f;
                for (int k = 0; k < (h - f) * 3; k++) spawnDebris(x - HALF + 0.5f, f + 0.4f + k * 0.33f, z + 0.5f, 1f);
                shake = Math.max(shake, 0.6f);
            }
            for (int c = crushers.size() - 1; c >= 0; c--) {
                float[] cr = crushers.get(c);
                if ((int) cr[1] != z) continue;
                crushers.remove(c);
                smashedCrushers.add(cr);
                float f = floorAt((int) cr[0], (int) cr[1]);
                for (int k = 0; k < 6; k++) spawnDebris(cr[0] - HALF + 0.5f, f + 3.2f + k * 0.25f, cr[1] + 0.5f, 1.2f);
                shake = Math.max(shake, 0.8f);
            }
        }
        smashedTo = Math.max(smashedTo, to + 1);
        for (Enemy3 e : enemies) {
            if (e.dead || e.z > front) continue;
            if (e == coinFoe) coinFoe = null;                   // travolto dal masso: il gettone che aveva addosso è perso
            e.dead = true; e.flung = true; e.vy = 9f; e.vz = 7f; e.vx = e.x >= 0 ? 3f : -3f;
        }
    }

    private float rnd = 0.5f;

    private float rnd() { rnd = (rnd * 9301f + 0.49297f) % 1f; return rnd; }

    private void spawnDebris(float x, float y, float z, float size) {
        if (debris.size() > 160) debris.remove(0);
        float a = rnd() * 6.283f;
        debris.add(new float[] {x + (rnd() - 0.5f) * 0.7f, y, z + (rnd() - 0.5f) * 0.4f,
                (float) Math.cos(a) * (1.5f + 3f * rnd()), 3f + 5f * rnd(), 5f + 6f * rnd(), rnd() * 6.283f, (rnd() - 0.5f) * 14f, 2.2f + rnd(), size * (0.18f + 0.22f * rnd())});
    }

    /** Detriti: gravità, rimbalzo smorzato sul terreno, e svaniscono. */
    private void stepDebris(float dt) {
        shake = Math.max(0, shake - dt * 1.8f);
        for (int i = debris.size() - 1; i >= 0; i--) {
            float[] d = debris.get(i);
            d[8] -= dt;
            if (d[8] <= 0) { debris.remove(i); continue; }
            d[4] -= 25f * dt;
            d[0] += d[3] * dt; d[1] += d[4] * dt; d[2] += d[5] * dt; d[6] += d[7] * dt;
            int xi = Math.max(0, Math.min(W - 1, xi(d[0]))), zi = Math.max(0, Math.min(length - 1, zi(d[2])));
            float g = Math.abs(d[0]) < HALF ? Math.max(0, height[zi * W + xi]) : 0f;
            if (height[zi * W + xi] == PIT && Math.abs(d[0]) < HALF) g = -50f;
            if (d[1] < g + d[9] * 0.5f && d[4] < 0) { d[1] = g + d[9] * 0.5f; d[4] *= -0.35f; d[3] *= 0.6f; d[5] *= 0.6f; d[7] *= 0.6f; }
        }
    }

    /** Il robot riparte da più indietro: muri, colonne e pestelli davanti al masso tornano al loro posto. */
    private void unsmash() {
        int from = Float.isNaN(fireZ) ? 0 : (int) Math.floor(fireZ + BOULDER_FRONT) + 1;
        for (int z = Math.max(0, from); z < length; z++) for (int x = 0; x < W; x++) {
            int i = z * W + x;
            if (breakH[i] != 0) height[i] = breakH[i];
        }
        for (int c = smashedCrushers.size() - 1; c >= 0; c--) {
            float[] cr = smashedCrushers.get(c);
            if (cr[1] >= from) { crushers.add(cr); smashedCrushers.remove(c); }
        }
        smashedTo = Math.min(smashedTo, from);
        debris.clear();
    }

    private void stepFire(float dt) {
        if (!Float.isNaN(hiveZ) && hiveAt < 0 && pz >= hiveZ + 1.5f) {      // il nido cade: lo sciame esce e inizia l'inseguimento
            hiveAt = time;
            fireZ = hiveZ - 3f;
        }
        if (!Float.isNaN(fireZ)) {
            float speed = 0, gap = 1e9f;
            for (float[] z : fireZones) if (pz >= z[0]) { speed = z[1]; gap = z.length > 2 ? z[2] : 1e9f; }
            if (cineT >= 0) speed = 0;                                   // incagliato: non si muove più
            fireZ += speed * dt;
            if (gap < 1e8f && cineT < 0) fireZ = Math.max(fireZ, pz - gap);   // il fuoco non resta mai troppo indietro
            if (chase == 2 && !Float.isNaN(boulderStop) && fireZ >= boulderStop) {
                fireZ = boulderStop;
                if (cineT < 0) { cineT = 0; shake = 1.2f; for (int k = 0; k < 14; k++) spawnDebris((k % 5) - HALF + 0.5f, 1f + k * 0.3f, boulderStop + BOULDER_FRONT + 0.3f, 1.1f); }
            }
            if (cineT >= 0) { cineT += dt; if (cineLocked()) { joyX = 0; joyY = 0; jump = false; spin = false; } }
            if (chase == 2) smash(fireZ + BOULDER_FRONT);
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
        if (cart) { tx = joyX * 6f; tz = cartSpeed(pz); steering = true; }     // il carrello va da solo: si sterza tra i binari
        boolean flung = springBoost > 0 && !onGround;            // lanciato dal fiore: tiene la spinta fino all'atterraggio, sterzando di poco
        if (flung) { tx = springDx + joyX * 3f; tz = springDz + joyY * 3f; steering = true; }
        float rate = onGround ? (steering ? ACCEL : FRICTION) : (steering ? AIR_ACCEL : AIR_FRICTION);
        float dx = tx - vx, dz = tz - vz;
        float dl = (float) Math.sqrt(dx * dx + dz * dz), stepV = rate * dt;
        if (dl <= stepV) { vx = tx; vz = tz; } else { vx += dx / dl * stepV; vz += dz / dl * stepV; }

        // a terra il robot si gira verso dove va; in aria non si volta
        float sp = (float) Math.sqrt(vx * vx + vz * vz);
        if (onGround && sp > 0.5f) {
            float want = (float) Math.atan2(vx, vz) * (cart ? 0.35f : 1f), d = want - yaw;      // in carrello guarda avanti, si piega appena
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
            switch (belt[bi]) { case 1: ex = BELT_SIDE; break; case 2: ex = -BELT_SIDE; break; case 3: ez = BELT_SPEED; break; case 4: ez = -BELT_SPEED; break; default: break; }
        }
        ex += windPush(pz, time);
        // spostamento: un asse alla volta, fermandosi contro blocchi e casse più alti dei piedi
        float nx = px + (vx + ex) * dt;
        if (cart && Math.abs(nx) > CART_X) { nx = Math.signum(nx) * CART_X; vx = 0; }   // il carrello resta sui tre binari centrali
        if (!blocked(nx, pz, py)) px = nx;
        else if (Math.abs(vx) > 0.5f && !cart) { float d = slide(nx, pz, false, dt); if (d != 0) pz += d; else vx = 0; }
        else vx = 0;
        float nz = pz + (vz + ez) * dt;
        if (!blocked(px, nz, py)) pz = nz;
        else if (Math.abs(vz) > 0.5f && !cart) { float d = slide(px, nz, true, dt); if (d != 0) px += d; else vz = 0; }
        else vz = 0;

        py += vy * dt;
        float g = footAt(px, pz, py);
        boolean wasGround = onGround;
        onGround = false;
        if (wasGround && vy <= 0 && py > g && py - g < 0.12f) py = g;     // in discesa sulla rampa i piedi restano attaccati
        if (py <= g && vy <= 0 && py > g - 0.6f) {
            py = g;
            vy = 0;
            onGround = true;
        }
        if (g > -500f) groundY = g;
        if (onGround && !cart) pocket(dt);
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
        if (inCart()) {                                  // in carrello si riparte lanciati: davanti serve un bel tratto sgombro
            for (int cz = zi(pz) - 1; cz <= zi(pz) + 12 && cz < length; cz++) for (int cx = 0; cx < W; cx++) {
                if (cell(cx, cz) == PIT || (crate != null && crate[cz * W + cx])) return;
            }
            for (Enemy3 e : enemies) if (!e.dead && e.z > pz - 2f && e.z < pz + 14f) return;
            cpX = 0;
            cpZ = pz;
            return;
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
                    : e.type == Enemy3.STORM ? ENEMY_SPEED * 0.7f : e.type == Enemy3.SNAIL ? (e.mode == 1 ? 0f : ENEMY_SPEED * (e.mode == 2 ? 0.65f : 0.4f)) : e.type == Enemy3.PENGUIN ? (e.alongZ ? ENEMY_SPEED * 0.6f : ENEMY_SPEED * 2.3f)
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
            boolean big = e.skin == Enemy3.SKIN_TORTOISE;                // la tartaruga è grossa: più alta, e più facile da prendere dall'alto
            boolean vertical = py < e.y + (big ? 0.9f : 0.6f) && py + PLAYER_H > e.y;
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
            } else if (big && ax < 1.0f && az < 1.0f && vertical && vy < 0 && py > e.y + 0.4f && stompable3(e)) {
                if (e.mode < 2) { e.mode = 1; e.t = 4f; } else e.flatTime = 0.45f;   // sul guscio grande si atterra anche di striscio
                vy = JUMP_V;
                airJumps = 1;
            } else if (ax < (big ? 0.85f : 0.75f) && az < (big ? 0.85f : 0.75f) && vertical) {
                if (stompable3(e) && vy < 0 && py > e.y + 0.25f) {     // schiacciato dall'alto (il riccio punge)
                    if (snail && e.mode < 2) { e.mode = 1; e.t = 4f; } else e.flatTime = 0.45f;
                    vy = JUMP_V;                               // rimbalzo come un salto singolo (più alto tenendo premuto)
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

    /** Nemico innocuo al contatto: si può schiacciare o far volare con la giravolta, ma toccarlo non ferisce. */
    private void touchSoft(Enemy3 e) {
        float ax = Math.abs(e.x - px), az = Math.abs(e.z - pz);
        boolean vertical = py < e.y + 1.5f && py + PLAYER_H > e.y;
        if (spinActive && ax < 0.75f + SPIN_REACH && az < 0.75f + SPIN_REACH && vertical) {
            e.flung = true;
            float l = Math.max(0.01f, (float) Math.sqrt((e.x - px) * (e.x - px) + (e.z - pz) * (e.z - pz)));
            e.vx = (e.x - px) / l * 6f;
            e.vz = (e.z - pz) / l * 6f + 3f;
            e.vy = 8f;
            score += 100;
        } else if (ax < 0.7f && az < 0.7f && vy < 0 && py > e.y + 1.2f && py < e.y + 1.9f) {
            e.flatTime = 0.45f;
            vy = JUMP_V;                               // rimbalzo come un salto singolo (più alto tenendo premuto)
            airJumps = 1;
            score += 100;
        }
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
                vy = JUMP_V;                               // rimbalzo come un salto singolo (più alto tenendo premuto)
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
    /** Pianta carnivora: distanza a cui scatta e lunghezza massima del morso. */
    static final float PLANT_RANGE = 2.6f, PLANT_REACH = 1.9f;

    /** Ruota l'angolo a verso b di al più step radianti. */
    static float turnTo(float a, float b, float step) {
        float d = b - a;
        while (d > Math.PI) d -= 2 * Math.PI;
        while (d < -Math.PI) d += 2 * Math.PI;
        return a + Math.max(-step, Math.min(step, d));
    }

    private boolean stepNew3(Enemy3 e, float dt) {
        e.t += dt;
        float dxp = px - e.x, dzp = pz - e.z;
        float floorY = Float.isNaN(e.floor) ? e.y : e.floor;
        switch (e.type) {
            case Enemy3.CHESTNUT: {                            // riccio di castagno appeso a un ramo: quando passi sotto trema, cade e scoppia
                if (Float.isNaN(e.floor)) { e.floor = Math.max(0, topAt(e.x, e.z)); e.y0 = e.y; }
                boolean burr = e.skin == 0;                    // il masso (miniera) invece rotola come prima
                if (e.mode == 0) {
                    e.y = e.y0;
                    if (burr ? (dzp > -4.2f && dzp < 0.8f && Math.abs(dxp) < 2.6f) : (Math.abs(dzp) < 4.2f && dzp > -1f && Math.abs(dxp) < 2.2f)) { e.mode = 1; e.t = 0; }
                } else if (e.mode == 1) {
                    if (e.t > (burr ? 0.3f : 0.45f)) { e.mode = 2; e.vy = 0; }
                } else if (e.mode == 2) {
                    e.vy -= GRAVITY * (burr ? 1f : 0.8f) * dt;
                    e.y += e.vy * dt;
                    if (e.y <= e.floor + 0.3f) {
                        e.y = e.floor + 0.3f;
                        if (burr) {                            // a terra scoppia: chi è nel raggio si fa male
                            booms.add(new float[] {e.x, e.z, 0, e.floor, 1});      // 1: scoppio di riccio, ferisce mentre si allarga
                            e.dead = true;
                            e.flung = false;
                            return true;
                        }
                        e.mode = 3; e.t = 0; e.dir = dzp >= 0 ? 1 : -1;
                    }
                } else {
                    float nz = e.z + e.dir * 4.2f * dt;
                    float h = topAt(e.x, nz);
                    if (h < e.floor - 0.05f || h > e.floor + 0.05f) e.dir = -e.dir; else e.z = nz;
                    if (e.t > 3.2f) { e.dead = true; return true; }
                }
                if (state == PLAYING && !burr) touch(e, false);          // il riccio fa male solo scoppiando
                return true;
            }
            case Enemy3.OWL: {
                // sospeso davanti al robot, rivolto verso di lui, ondeggia di lato. Se il robot è a tiro piomba su di lui con
                // una curva (prima scende ripido, poi si raddrizza radente), lo sorvola, risale in una larga virata di lato e
                // torna al posto. Di spalle solo durante la virata, e allora non vede.
                if (Float.isNaN(e.floor)) { e.floor = 0; e.x0 = e.x; e.z0 = (e.lo + e.hi) * 0.5f; e.z = e.z0; e.yaw = (float) Math.PI; }
                float homeX = Math.max(-1.8f, Math.min(1.8f, e.x0 + 1.4f * (float) Math.sin(e.anim * 0.9f)));
                float ox = e.x, oy = e.y, oz = e.z;
                if (e.mode == 0) {
                    e.x += (homeX - e.x) * Math.min(1f, 2f * dt);
                    e.z += (e.z0 - e.z) * Math.min(1f, 2f * dt);
                    e.y += (e.baseY + 0.25f * (float) Math.sin(e.anim * 2.2f) - e.y) * Math.min(1f, 4f * dt);
                    boolean sees = dzp < -1.2f && dzp > -8f && Math.abs(dxp) < 2.8f;
                    if (sees && e.t > 1.6f) {                  // una picchiata ogni tanto, non a raffica
                        e.mode = 1; e.t = 0;
                        e.sx = e.x; e.sy = e.y; e.sz = e.z;
                        e.ex = px; e.ey = Math.max(0, topAt(px, pz)) + 0.7f; e.ez = pz - 0.4f;     // punta poco davanti al robot: spostandosi di lato la si schiva
                    }
                } else if (e.mode == 1) {                      // picchiata: 0,7 s
                    float u = Math.min(1f, e.t / 0.85f), uh = u * (2 - u), uv = 1 - (1 - u) * (1 - u) * (1 - u);
                    e.x = e.sx + (e.ex - e.sx) * uh; e.z = e.sz + (e.ez - e.sz) * uh; e.y = e.sy + (e.ey - e.sy) * uv;
                    if (u >= 1f) {
                        e.mode = 2; e.t = 0;
                        e.sx = e.x; e.sy = e.y; e.sz = e.z;
                        float side = e.x > 0 ? -1 : 1;
                        e.ex = e.x + side * 2.6f; e.ey = e.baseY + 1.2f; e.ez = e.z - 3.2f;     // vertice della virata: di lato e alto
                    }
                } else if (e.mode == 2) {                      // sorvola e risale virando: 0,8 s
                    float u = Math.min(1f, e.t / 0.8f), us = u * u * (3 - 2 * u);
                    e.x = e.sx + (e.ex - e.sx) * us; e.z = e.sz + (e.ez - e.sz) * (float) Math.sin(u * 1.5708f); e.y = e.sy + (e.ey - e.sy) * us;
                    if (u >= 1f) { e.mode = 3; e.t = 0; e.sx = e.x; e.sy = e.y; e.sz = e.z; }
                } else {                                       // ritorno al posto con un arco: 1,2 s
                    float u = Math.min(1f, e.t / 1.2f), us = u * u * (3 - 2 * u);
                    float cxp = (e.sx + homeX) * 0.5f + (e.sx > homeX ? 1.2f : -1.2f);
                    float a1 = 1 - us;
                    e.x = a1 * a1 * e.sx + 2 * a1 * us * cxp + us * us * homeX;
                    e.z = e.sz + (e.z0 - e.sz) * us;
                    e.y = e.sy + (e.baseY - e.sy) * us;
                    if (u >= 1f) { e.mode = 0; e.t = 0; }
                }
                // orientamento morbido nel verso del moto (in attesa: verso il robot); inclinazione e rollio dalla virata
                float vxs = (e.x - ox) / Math.max(dt, 1e-4f), vys = (e.y - oy) / Math.max(dt, 1e-4f), vzs = (e.z - oz) / Math.max(dt, 1e-4f);
                float hs = (float) Math.sqrt(vxs * vxs + vzs * vzs);
                float wantYaw = e.mode == 0 || hs < 0.8f ? (float) Math.PI : (float) Math.atan2(vxs, vzs);
                float dyaw = wantYaw - e.yaw;
                while (dyaw > Math.PI) dyaw -= 2 * Math.PI;
                while (dyaw < -Math.PI) dyaw += 2 * Math.PI;
                float k = Math.min(1f, 6f * dt);
                e.yaw += dyaw * k;
                e.bank += (Math.max(-0.7f, Math.min(0.7f, -dyaw * 1.2f)) - e.bank) * k;
                e.pitch += ((hs > 0.8f ? Math.max(-0.9f, Math.min(0.6f, (float) Math.atan2(-vys, hs))) : 0f) - e.pitch) * k;
                if (state == PLAYING) touch(e, true);
                return true;
            }
            case Enemy3.PENGUIN: {                             // lungo il sentiero: zampetta su e giù, quando vede il robot si butta sulla pancia
                if (!e.alongZ) return false;                   // di traverso: va avanti e indietro come sempre
                if (e.mode == 0) {
                    if (e.t > 0.8f && Math.abs(dxp) < 1.3f && Math.abs(dzp) < 7.5f) { e.dir = dzp > 0 ? 1 : -1; e.mode = 1; e.t = 0; }
                    else if (e.z < e.lo - 0.05f || e.z > e.hi + 0.05f) {   // dopo una scivolata fuori ronda torna indietro a piedi
                        e.dir = e.z < e.lo ? 1 : -1;
                        e.z += e.dir * ENEMY_SPEED * 0.6f * dt;
                        if (state == PLAYING) touch(e, true);
                        return true;
                    }
                    return false;                              // ronda normale tra lo e hi
                }
                if (e.mode == 1) {                             // scivolata lunga e dritta, anche oltre la sua ronda: si ferma solo contro un ostacolo
                    float nz = e.z + e.dir * ENEMY_SPEED * 4.6f * dt;
                    if (e.t > 2.2f || enemyBlocked(e, e.x, nz) || nz < 0.5f || nz > length - 0.5f) { e.mode = 2; e.t = 0; }
                    else e.z = nz;
                } else if (e.t > 0.7f) {                       // rialzato: torna alla sua ronda camminando
                    e.mode = 0; e.t = 0;
                    e.dir = e.z > (e.lo + e.hi) * 0.5f ? -1 : 1;
                }
                if (state == PLAYING) touch(e, true);
                return true;
            }
            case Enemy3.SNOWMAN: {
                if (Float.isNaN(e.floor)) {
                    e.floor = e.y;
                    if (e.skin == 0) e.face = e.x < -0.6f ? 1 : e.x > 0.6f ? -1 : 0;   // ai lati guarda verso il centro, in mezzo verso il robot che arriva
                }
                if (e.skin != 0) {                             // totem: si volta e tira noci di cocco ogni 2,3 s
                    e.dir = dzp >= 0 ? 1 : -1;
                    if (Math.abs(dzp) < 11f && dzp > -2f && e.t > 2.3f) {
                        e.t = 0;
                        float l = Math.max(0.5f, (float) Math.sqrt(dxp * dxp + dzp * dzp));
                        shots.add(new float[] {e.x, e.y + 1.3f, e.z, dxp / l * 5.5f, 3.2f, dzp / l * 5.5f, e.skin});
                    }
                    if (state == PLAYING) touch(e, true);
                    return true;
                }
                // pupazzo: fermo, guarda sempre dalla stessa parte; se il robot entra nella sua linea di tiro carica il braccio
                // (0,7 s, si vede) e tira dritto davanti a sé. Colpito mentre carica, il tiro salta (e lui va in pezzi).
                e.dir = -1;
                boolean inLine = e.face == 0 ? (dzp < -1f && dzp > -11f && Math.abs(dxp) < 2.6f)
                        : (dxp * e.face > 0.5f && dxp * e.face < 6.5f && Math.abs(dzp) < 1.4f);
                if (e.mode == 0) {
                    if (inLine && e.t > 1.4f) { e.mode = 1; e.t = 0; }
                } else if (e.t > 0.7f) {
                    e.mode = 0; e.t = 0;
                    float dist = e.face == 0 ? Math.max(1.5f, -dzp) : Math.max(1f, Math.abs(dxp));
                    float sp = 7.5f, tt = dist / sp, h = e.y + 1.35f - (py + 0.7f);
                    float vyt = (0.5f * 14f * tt * tt - h) / tt;                     // arriva all'altezza del petto del robot
                    float vx = e.face == 0 ? Math.max(-1.5f, Math.min(1.5f, dxp / tt)) : e.face * sp;
                    float vz = e.face == 0 ? -sp : 0f;
                    float hx = e.face == 0 ? 0.25f : e.face * 0.35f, hz = e.face == 0 ? -0.3f : 0f;
                    shots.add(new float[] {e.x + hx, e.y + 1.35f, e.z + hz, vx, vyt, vz, 0, enemies.indexOf(e)});
                }
                if (state == PLAYING) {
                    touchSoft(e);                              // toccarlo non fa male: fanno male solo le palle di neve
                    if (e.flatTime > 0 || e.flung) {           // colpito: il tiro in preparazione non parte, le palle in volo si sbriciolano
                        e.mode = 0;
                        int id = enemies.indexOf(e);
                        for (int i = shots.size() - 1; i >= 0; i--) if (shots.get(i).length > 7 && shots.get(i)[7] == id) shots.remove(i);
                    }
                }
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
                    e.yaw = turnTo(e.yaw, (float) Math.atan2(dxp, dzp), 5f * dt);     // si gira verso il robot: salta sempre di fronte
                    if (e.t > 1.4f && Math.abs(dzp) < 8f && Math.abs(dxp) < 3f) {
                        e.mode = 1; e.t = 0;
                        float l = Math.max(0.5f, (float) Math.sqrt(dxp * dxp + dzp * dzp));
                        e.vx = (float) Math.sin(e.yaw) * 3.6f; e.vz = (float) Math.cos(e.yaw) * 3.6f; e.dir = dzp >= 0 ? 1 : -1;
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
            case Enemy3.PLANT: {                               // a bocca aperta aspetta; se il robot si avvicina trema un attimo e scatta a mordere
                float dist = (float) Math.sqrt(dxp * dxp + dzp * dzp);
                e.dir = dzp >= 0 ? 1 : -1;
                if (e.mode == 0) {
                    e.reach = Math.max(0f, e.reach - 4f * dt);
                    e.yaw = turnTo(e.yaw, (float) Math.atan2(dxp, dzp), 6f * dt);
                    if (e.t > 0.7f && dist < PLANT_RANGE && Math.abs(py - e.y) < 1.6f) { e.mode = 1; e.t = 0; }
                } else if (e.mode == 1) {                      // freme e prende la mira: poi non corregge più
                    e.yaw = turnTo(e.yaw, (float) Math.atan2(dxp, dzp), 10f * dt);
                    if (e.t > 0.32f) { e.mode = 2; e.t = 0; e.hopT = Math.min(PLANT_REACH, dist + 0.4f); }
                } else {
                    float u = e.t / 0.6f;                      // la testa si allunga verso il robot, schiocca, torna indietro
                    e.reach = e.hopT * (u < 0.35f ? (1f - (1f - u / 0.35f) * (1f - u / 0.35f)) : u < 0.55f ? 1f : Math.max(0f, 1f - (u - 0.55f) / 0.45f));
                    if (state == PLAYING && u < 0.6f) {
                        float hx = e.x + (float) Math.sin(e.yaw) * e.reach, hz = e.z + (float) Math.cos(e.yaw) * e.reach;
                        boolean head = py < e.y + 1.4f && py + PLAYER_H > e.y + 0.3f;
                        if (spinActive && head && Math.abs(hx - px) < 0.55f + SPIN_REACH && Math.abs(hz - pz) < 0.55f + SPIN_REACH) {
                            e.flung = true;                    // la giravolta stende anche la testa allungata verso il robot
                            float l = Math.max(0.01f, (float) Math.sqrt((e.x - px) * (e.x - px) + (e.z - pz) * (e.z - pz)));
                            e.vx = (e.x - px) / l * 6f;
                            e.vz = (e.z - pz) / l * 6f + 3f;
                            e.vy = 8f;
                            e.reach = 0;
                            score += 100;
                            return true;
                        }
                        if (Math.abs(hx - px) < 0.55f && Math.abs(hz - pz) < 0.55f && head) hurt();
                    }
                    if (u >= 1f) { e.mode = 0; e.t = 0; e.reach = 0; }
                }
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

    /**
     * Nemici del posto giusto: i nemici di base (ghianda, riccio, scarabeo, corvo) prendono l'aspetto della stagione, con lo
     * stesso comportamento. D'estate e in primavera la ghianda (frutto d'autunno) diventa una rana che saltella; d'inverno
     * una lepre bianca, il riccio un riccio di ghiaccio, lo scarabeo un pinguino; nell'incendio una brace che salta, un
     * riccio di lava, il corvo un pipistrello.
     */
    /**
     * Le strutture alte (3 blocchi o più) vanno da un bordo all'altro: se una colonna laterale resta più bassa della vicina
     * alta, la si alza alla stessa quota, così non si può girare attorno alla struttura camminando ai lati.
     */
    public boolean inMaze(float z) {
        for (float[] m : mazes) if (z >= m[0] && z <= m[1]) return true;
        return false;
    }

    /** Gemme in basso sulle corsie laterali della riga z: quel passaggio è voluto, non va riempito (le gemme finirebbero sepolte). */
    private boolean gemsBeside(int z, int h) {
        for (float[] g : gemsList) if ((int) Math.floor(g[2]) == z && Math.abs(g[0]) > HALF - 1f && g[1] < h) return true;
        return false;
    }

    private void noSidePaths() {
        for (int z = 0; z < length; z++) {
            if (inMaze(z + 0.5f)) continue;                     // nel labirinto i corridoi laterali sono voluti
            int h = height[z * W + 1];
            if (h == PIT || h < 3) continue;
            boolean flat = true;
            for (int x = 1; x < W - 1; x++) if (height[z * W + x] != h) flat = false;
            int l = height[z * W], r = height[z * W + W - 1];
            // solo il "balcone": tutto il centro alla stessa quota e entrambi i lati più bassi (le corsie a zig-zag restano)
            if (flat && (l == PIT || l < h) && (r == PIT || r < h) && !gemsBeside(z, h)) { height[z * W] = h; height[z * W + W - 1] = h; }
        }
    }

    private void seasonSkins() {
        for (Enemy3 e : enemies) {
            if (e.skin != 0) continue;
            switch (theme) {
                case 2:                                        // estate: cavalletta, istrice, calabrone, tartaruga
                    if (e.type == Enemy3.ACORN) e.skin = Enemy3.SKIN_HOPPER;
                    else if (e.type == Enemy3.HOG) e.skin = Enemy3.SKIN_PORCUPINE;
                    else if (e.type == Enemy3.CROW) e.skin = Enemy3.SKIN_WASP;
                    else if (e.type == Enemy3.SNAIL) e.skin = Enemy3.SKIN_TORTOISE;
                    break;
                case 4:                                        // autunno: il topo selvatico al posto dello scarabeo
                    if (e.type == Enemy3.BEETLE) e.skin = Enemy3.SKIN_MOUSE;
                    break;
                case 10:                                       // primavera: rana, processionaria, rondine
                    if (e.type == Enemy3.ACORN) e.skin = Enemy3.SKIN_FROG;
                    else if (e.type == Enemy3.HOG) e.skin = Enemy3.SKIN_CATERPILLAR;
                    else if (e.type == Enemy3.CROW) e.skin = Enemy3.SKIN_SWALLOW;
                    break;
                case 12:                                       // cielo: il falco al posto del corvo
                    if (e.type == Enemy3.CROW) e.skin = Enemy3.SKIN_HAWK;
                    break;
                case 6:
                    if (e.type == Enemy3.ACORN) e.skin = Enemy3.SKIN_HARE;
                    else if (e.type == Enemy3.HOG) e.skin = Enemy3.SKIN_ICE;
                    else if (e.type == Enemy3.BEETLE) e.skin = Enemy3.SKIN_PENGUIN;
                    else if (e.type == Enemy3.CROW) e.skin = Enemy3.SKIN_MAGPIE;
                    break;
                case 8:
                    if (e.type == Enemy3.ACORN) e.skin = Enemy3.SKIN_EMBER;
                    else if (e.type == Enemy3.HOG) e.skin = Enemy3.SKIN_MAGMA;
                    else if (e.type == Enemy3.CROW) e.skin = Enemy3.SKIN_BAT;
                    break;
                default: break;
            }
        }
    }

    /**
     * Una gemma sopra (o attaccata a) un rovo non si può prendere senza farsi male: la si sposta sulla colonna libera più vicina
     * alla stessa z, alla stessa altezza dal terreno. Se tutta la fila è spinosa, la si alza a mezz'aria: si prende saltando.
     */
    private void gemsOffThorns() {
        if (thorns.isEmpty()) return;
        for (float[] g : gemsList) {
            if (!nearThorn(g[0], g[2])) continue;
            float lift = g[1] - Math.max(0, topAt(g[0], g[2]));
            float best = Float.MAX_VALUE, bx = g[0], bz = g[2];
            float[] dzs = {0, 0.5f, -0.5f, 1f, -1f, 1.5f, -1.5f, 2f, -2f};
            for (float dz : dzs) {                                   // stessa riga se possibile, poi poco avanti o indietro
                float z = g[2] + dz;
                for (int xi = 0; xi < W; xi++) {
                    float x = xi - HALF + 0.5f;
                    int c = cell(xi, (int) Math.floor(z));
                    if (c == PIT || c < 0 || nearThorn(x, z) || Math.abs(topAt(x, z) - topAt(g[0], g[2])) > 1.01f) continue;
                    float d = Math.abs(x - g[0]) + Math.abs(dz) * 1.5f;
                    if (d < best) { best = d; bx = x; bz = z; }
                }
                if (best < 1.6f) break;
            }
            if (best < Float.MAX_VALUE) { g[0] = bx; g[2] = bz; g[1] = Math.max(0, topAt(bx, bz)) + Math.max(0.5f, lift); }
            else g[1] = Math.max(0, topAt(g[0], g[2])) + 1.9f;
        }
    }

    /** In questo punto chi raccoglie una gemma toccherebbe un rovo? (raggio di raccolta 0,65 + raggio del rovo 0,62) */
    private boolean nearThorn(float x, float z) {
        for (float[] t : thorns) { float dx = t[0] - x, dz = t[1] - z; if (dx * dx + dz * dz < 1.3f * 1.3f) return true; }   // raccolta 0,65 + rovo 0,62, con margine
        return false;
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
        if (coin != null && Math.abs(coin[0] - px) < 0.7f && Math.abs(coin[2] - pz) < 0.7f
                && coin[1] > py - 0.45f && coin[1] < py + PLAYER_H + 0.35f) {
            gotCoin(coin[0], coin[1], coin[2]);
            coin = null;
        }
        if (coinFoe != null && (coinFoe.flung || coinFoe.flatTime > 0)) {      // il nemico che aveva addosso il gettone: eliminato
            gotCoin(coinFoe.x, Math.max(coinFoe.y, 0) + 0.8f, coinFoe.z);
            coinFoe = null;
        } else if (coinFoe != null && coinFoe.dead) coinFoe = null;
        if (spinActive) {                             // la giravolta rompe le casse vicine
            float m = R + SPIN_REACH;
            for (int cz = zi(pz - m); cz <= zi(pz + m); cz++) {
                for (int cx = xi(px - m); cx <= xi(px + m); cx++) {
                    if (cx < 0 || cx >= W || cz < 0 || cz >= length) continue;
                    int k = cz * W + cx;
                    if (crate[k] && py < 1.2f) {
                        byte kind = crateKind == null ? C_WOOD : crateKind[k];
                        if (kind == C_BOUNCE && k != coinCrate) continue;      // la cassa a molla non si rompe, tranne quella del gettone
                        if (kind == C_TNT || kind == C_NITRO) { explode(k); continue; }    // la giravolta su una cassa esplosiva: scoppia subito
                        breakCrate(k);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ isola tropicale

    /** Gettone d'oro trovato: schizza in alto e vale per il record anche se poi si ricomincia. */
    private void gotCoin(float x, float y, float z) {
        coinPop = new float[] {x, y, z, 0};
        if (coinOwned) return;
        coinOwned = true;
        save.set(id + "_coin");
        score += 1000;
        showToast("Gettone d'oro trovato!");
    }

    /** Cassa rotta (salto o giravolta): gemma, punti, e se è di ripartenza si riparte da qui. */
    private void breakCrate(int k) {
        crate[k] = false;
        float cx = k % W - HALF + 0.5f, cz = k / W + 0.5f;
        if (k == coinCrate) { gotCoin(cx, 0.8f, cz); return; }      // la cassa del gettone: niente gemma
        gems++;
        score += 200;
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
        float cx = k % W - HALF + 0.5f, cz = k / W + 0.5f;
        if (k == coinCrate) gotCoin(cx, 0.8f, cz);
        else if (!hazardOnly(k)) { gems++; checkGem(); }
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
            case C_STEEL:                                  // rinforzata: regge come un blocco, saltandoci sopra non si apre
                return;
            case C_BOUNCE:
                vy = 12.5f; crateT[best] = time; bounceBoost = 0.6f;     // stessa altezza che si tenga premuto il salto o no
                break;
            case C_TNT:
                if (crateT[best] <= 0) crateT[best] = 0.001f;
                vy = JUMP_V;
                break;
            case C_NITRO:
                explode(best);
                return;
            default:
                breakCrate(best);
                vy = JUMP_V;                                 // rimbalzo come un salto singolo
                break;
        }
        onGround = false;
        coyote = jumpBuffer = 0;
        airJumps = 1;
    }

    private void stepIsland(float dt) {
        for (int i = booms.size() - 1; i >= 0; i--) {
            float[] bm = booms.get(i);
            if ((bm[2] += dt) > 0.7f) { booms.remove(i); continue; }
            // riccio: l'onda parte piccola e si allarga in 0,3 s fino a BURR_R; ferisce solo chi è a terra dentro l'onda in quel momento
            if (bm.length > 4 && bm[4] == 1 && bm[2] < 0.22f && state == PLAYING) {
                float r = 0.6f + (BURR_R - 0.6f) * bm[2] / 0.22f, dx = px - bm[0], dz = pz - bm[1];
                if (dx * dx + dz * dz < r * r && py < bm[3] + 0.5f) hurt();
            }
        }
        if (crateKind != null) {
            boolean cart = inCart();
            int z0 = Math.max(0, zi(pz) - 30), z1 = Math.min(length - 1, zi(pz) + 30);
            for (int k = z0 * W; k < (z1 + 1) * W; k++) {
                if (!crate[k]) continue;
                if (crateKind[k] == C_TNT || crateKind[k] == C_NITRO) {
                    if (crateT[k] > 0 && (crateT[k] += dt) >= TNT_FUSE) explode(k);
                }
                if (crateKind[k] == C_NITRO && crate[k]) {      // l'instabile scoppia anche solo sfiorandola di lato
                    float dx = Math.abs(k % W - HALF + 0.5f - px), dz = Math.abs(k / W + 0.5f - pz);
                    float mx = cart ? 0.5f + R - 0.07f : 0.5f + R + 0.04f;     // in carrello conta solo l'urto vero: cambiare binario accanto non la fa scoppiare
                    if (dx < mx && dz < 0.5f + R + 0.04f && py < 0.98f) explode(k);
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
            float bx = c[0] - HALF + 0.5f, bz = c[1] + 0.5f, bot = floorAt((int) c[0], (int) c[1]) + crusherBottom(c, time);
            float ox = 0.5f + R - 0.05f - Math.abs(px - bx), oz = 0.5f + R - 0.05f - Math.abs(pz - bz);
            if (ox <= 0 || oz <= 0 || bot >= py + PLAYER_H - 0.1f) continue;
            boolean falling = crusherBottom(c, time) < crusherBottom(c, time - 0.03f) - 1e-4f;
            if (falling && bot > py + 0.3f) hurt();            // fa male solo piombando dall'alto sulla testa
            else if (ox < oz) px += Math.signum(px - bx == 0 ? 1 : px - bx) * ox;   // fermo o in risalita è un blocco di pietra: ci si sbatte contro
            else pz += Math.signum(pz - bz == 0 ? -1 : pz - bz) * oz;
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

    private void stepGate(float dt) {
        if (pz > flagZ - 30f && gateOpen < 1f) gateOpen = Math.min(1f, gateOpen + dt * 0.8f);      // si apre già quando lo si vede da lontano
        if (state == PLAYING && gateOpen > 0.6f && pz >= flagZ + 3.4f) win();
    }

    /**
     * Dissolvenza nel buio del portone (0..1): cresce mentre il robot attraversa la soglia e a livello finito copre tutto,
     * come nel 2D dove il robot sparisce nell'oscurità.
     */
    public float fade() {
        if (state == WON) return Math.min(1f, 0.8f + stateTime / 0.6f * 0.2f);
        if (state != PLAYING || gateOpen < 0.6f) return 0f;
        float u = (pz - (flagZ + 0.2f)) / 3.2f;
        return u <= 0 ? 0f : Math.min(0.8f, u * u * (3 - 2 * u) * 0.8f);
    }

    private void win() {
        state = WON;
        stateTime = 0;
        score += 500;
        vx = 0;
        vz = 2.5f;                                        // continua a camminare nel buio
        spinActive = false;
    }
}
