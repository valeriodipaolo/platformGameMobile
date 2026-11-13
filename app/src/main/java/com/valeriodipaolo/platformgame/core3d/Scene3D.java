package com.valeriodipaolo.platformgame.core3d;

import com.valeriodipaolo.platformgame.core.Img;

import java.util.Random;

/** Costruisce i modelli del livello 3D e disegna un fotogramma su un Gfx3D. */
public final class Scene3D {
    private static final float FOG_R = 0.745f, FOG_G = 0.847f, FOG_B = 0.933f;
    private static final float PIT_DEPTH = 5f;

    private final float[] view = new float[16], proj = new float[16], m = new float[16], m2 = new float[16];
    private final float[] ident = Mat4.identity();

    private Mesh sky, mountains, grass, dirt, stone, banks, forestFloor, trees, pitFloor;
    private Mesh crate, gem, battery, batteryGlow, shadow, ring, flagPole, flagCloth;
    private Mesh robotBody, robotEyes, robotLeg, robotArm, beetleBody, beetleEyes, beetleLeg;
    private int builtFor = -1;
    private float camX, camY = 0, camZ;
    private boolean camInit;

    // ------------------------------------------------------------------ modelli fissi

    private void build(World3D w) {
        builtFor = w.length;
        MeshBuilder b;

        b = new MeshBuilder();                                         // cielo a tutto schermo
        b.quad(-1, -1, 0.999f, 1, -1, 0.999f, 1, 1, 0.999f, -1, 1, 0.999f, 0, 0, -1, 0, 0, 1, 0.82f);
        sky = b.build(Img.SKY, false, false);

        b = new MeshBuilder();                                         // montagne lontane
        b.quad(-330, -4, 0, 330, -4, 0, 330, 78, 0, -330, 78, 0, 0, 0, -1, 0, 0, 2, 1);
        mountains = b.build(Img.MOUNTAINS, true, false);

        int z0 = -8, z1 = w.length + 40;
        MeshBuilder g = new MeshBuilder(), d = new MeshBuilder(), s = new MeshBuilder();
        d.color(190, 170, 150);
        for (int z = z0; z < z1; z++) {
            for (int x = 0; x < World3D.W; x++) {
                int h = vis(w, x, z);
                float fx = x - World3D.HALF;
                if (h == World3D.PIT) continue;
                if (h == 0) {
                    g.quad(fx, 0, z, fx + 1, 0, z, fx + 1, 0, z + 1, fx, 0, z + 1, 0, 1, 0, 0, 0, 1, 1);
                } else {
                    for (int k = 0; k < h; k++) s.box(fx + 0.5f, k + 0.5f, z + 0.5f, 1, 1, 1);
                }
                // pareti di terra dove il prato confina col vuoto
                if (vis(w, x, z - 1) == World3D.PIT) wallZ(d, fx, z, 1);
                if (vis(w, x, z + 1) == World3D.PIT) wallZ(d, fx, z + 1, -1);
                if (x > 0 && vis(w, x - 1, z) == World3D.PIT) wallX(d, fx, z, 1);
                if (x < World3D.W - 1 && vis(w, x + 1, z) == World3D.PIT) wallX(d, fx + 1, z, -1);
            }
            // pareti sotto le sponde, dove il vuoto arriva al bordo del corridoio
            if (vis(w, 0, z) == World3D.PIT) wallX(d, -World3D.HALF, z, 1);
            if (vis(w, World3D.W - 1, z) == World3D.PIT) wallX(d, World3D.HALF, z, -1);
        }
        grass = g.build(Img.TEX_GRASS, false, true);
        dirt = d.build(Img.DIRT, false, true);
        stone = s.build(Img.STONE, false, true);

        b = new MeshBuilder().color(10, 18, 14);                       // fondo dei burroni
        b.quad(-World3D.HALF, -PIT_DEPTH, z0, World3D.HALF, -PIT_DEPTH, z0, World3D.HALF, -PIT_DEPTH, z1, -World3D.HALF, -PIT_DEPTH, z1,
               0, 1, 0, 0.5f, 0.5f, 0.5f, 0.5f);
        pitFloor = b.build(-1, false, true);

        // sponde erbose che salgono ai lati e sottobosco
        MeshBuilder bk = new MeshBuilder().color(150, 176, 132), ff = new MeshBuilder().color(96, 124, 88);
        float bw = 1.9f, bh = 1.5f, nl = (float) Math.sqrt(bh * bh + bw * bw);
        for (int z = z0; z < z1; z++) {
            float xl = -World3D.HALF, xr = World3D.HALF;
            bk.quad(xl - bw, bh, z, xl, 0, z, xl, 0, z + 1, xl - bw, bh, z + 1, bh / nl, bw / nl, 0, 0, 0, 1, 1);
            bk.quad(xr, 0, z, xr + bw, bh, z, xr + bw, bh, z + 1, xr, 0, z + 1, -bh / nl, bw / nl, 0, 0, 0, 1, 1);
        }
        for (int z = z0; z < z1; z += 8) {
            float xl = -World3D.HALF - bw, xr = World3D.HALF + bw;
            ff.quad(xl - 48, bh, z, xl, bh, z, xl, bh, z + 8, xl - 48, bh, z + 8, 0, 1, 0, 0, 0, 12, 2);
            ff.quad(xr, bh, z, xr + 48, bh, z, xr + 48, bh, z + 8, xr, bh, z + 8, 0, 1, 0, 0, 0, 12, 2);
        }
        banks = bk.build(Img.TEX_GRASS, false, true);
        forestFloor = ff.build(Img.TEX_GRASS, false, true);

        // bosco di conifere ai due lati
        MeshBuilder t = new MeshBuilder();
        Random r = new Random(7);
        for (int side = -1; side <= 1; side += 2) {
            for (int row = 0; row < 5; row++) {
                for (float z = z0; z < z1; z += 2.1f + row * 0.5f) {
                    float x = side * (World3D.HALF + bw + 1.6f + row * 2.6f + r.nextFloat() * 1.6f);
                    float tz = z + r.nextFloat() * 1.6f;
                    float hgt = 3.6f + r.nextFloat() * 3.2f + row * 0.5f, rad = 0.95f + r.nextFloat() * 0.6f;
                    float k = 0.8f + r.nextFloat() * 0.35f;
                    t.color(96, 66, 42);
                    t.cylinder(x, bh, bh + hgt * 0.22f, tz, 0.16f, 0.14f, 5, false, false);
                    t.color((int) (40 * k), (int) (98 * k), (int) (48 * k));
                    t.cylinder(x, bh + hgt * 0.16f, bh + hgt * 0.62f, tz, rad, 0, 7, false, true);
                    t.color((int) (58 * k), (int) (124 * k), (int) (56 * k));
                    t.cylinder(x, bh + hgt * 0.42f, bh + hgt * 0.84f, tz, rad * 0.74f, 0, 7, false, true);
                    t.color((int) (82 * k), (int) (150 * k), (int) (62 * k));
                    t.cylinder(x, bh + hgt * 0.66f, bh + hgt, tz, rad * 0.48f, 0, 7, false, true);
                }
            }
        }
        trees = t.build(-1, false, true);

        buildObjects();
    }

    /** Altezza da disegnare: prima dell'inizio e dopo la fine il prato continua. */
    private static int vis(World3D w, int x, int z) {
        if (z < 0 || z >= w.length) return 0;
        return w.height[z * World3D.W + x];
    }

    /** Parete di terra che scende nel vuoto e si scurisce verso il fondo. Vertici: a-b in alto, poi giù. */
    private static void wall(MeshBuilder d, float ax, float az, float bx, float bz, float nx, float nz) {
        d.color(196, 176, 156);
        d.vert(ax, 0, az, nx, 0, nz, 0, 0);
        d.vert(bx, 0, bz, nx, 0, nz, 1, 0);
        d.color(10, 14, 12);
        d.vert(bx, -PIT_DEPTH, bz, nx, 0, nz, 1, PIT_DEPTH);
        d.vert(bx, -PIT_DEPTH, bz, nx, 0, nz, 1, PIT_DEPTH);
        d.vert(ax, -PIT_DEPTH, az, nx, 0, nz, 0, PIT_DEPTH);
        d.color(196, 176, 156);
        d.vert(ax, 0, az, nx, 0, nz, 0, 0);
    }

    /** Parete lungo x alla quota z; dir = +1 se il vuoto sta verso z minore. */
    private static void wallZ(MeshBuilder d, float fx, float z, int dir) {
        wall(d, fx, z, fx + 1, z, 0, -1);
    }

    private static void wallX(MeshBuilder d, float x, float z, int dir) {
        wall(d, x, z, x, z + 1, dir > 0 ? -1 : 1, 0);
    }

    private void buildObjects() {
        MeshBuilder b = new MeshBuilder();
        b.box(0, 0.5f, 0, 0.96f, 0.96f, 0.96f);
        crate = b.build(Img.BONUS, false, true);

        b = new MeshBuilder().color(46, 214, 204);
        b.bipyramid(0, 0, 0, 0.2f, 0.36f, 6);
        gem = b.build(-1, false, true);

        b = new MeshBuilder().color(62, 68, 80);
        b.cylinder(0, -0.24f, 0.24f, 0, 0.17f, 0.17f, 12, true, true);
        b.color(196, 202, 212);
        b.cylinder(0, 0.24f, 0.31f, 0, 0.08f, 0.08f, 8, true, false);
        battery = b.build(-1, false, true);
        b = new MeshBuilder().color(120, 236, 66);
        b.cylinder(0, -0.15f, 0.15f, 0, 0.176f, 0.176f, 12, false, false);
        batteryGlow = b.build(-1, false, true);

        b = new MeshBuilder();
        b.quad(-0.5f, 0, -0.5f, 0.5f, 0, -0.5f, 0.5f, 0, 0.5f, -0.5f, 0, 0.5f, 0, 1, 0, 0, 0, 1, 1);
        shadow = b.build(Img.SHADOW, true, true);

        b = new MeshBuilder().color(225, 244, 255);
        b.ring(0, 0, 0, 0.86f, 1f, 28);
        ring = b.build(-1, true, true);

        b = new MeshBuilder().color(176, 182, 192);
        b.cylinder(0, 0, 6.2f, 0, 0.07f, 0.07f, 8, false, false);
        b.color(238, 186, 40);
        b.ellipsoid(0, 6.3f, 0, 0.17f, 0.17f, 0.17f, 10, 6);
        flagPole = b.build(-1, false, true);
        b = new MeshBuilder();
        b.quad(0.08f, 0, 0, 1.88f, 0, 0, 1.88f, 1.18f, 0, 0.08f, 1.18f, 0, 0, 0, -1, 0, 0, 1, 1);
        flagCloth = b.build(Img.FLAG, true, true);

        // --- robot (guarda verso z positivo; la camera lo vede di spalle)
        b = new MeshBuilder().color(234, 236, 240);
        b.ellipsoid(0, 0.62f, 0, 0.25f, 0.30f, 0.21f, 14, 9);                 // busto
        b.color(238, 118, 28);
        b.cylinder(0, 0.46f, 0.52f, 0, 0.245f, 0.25f, 14, false, false);      // cintura
        b.color(204, 96, 24);
        b.box(0, 0.66f, -0.24f, 0.30f, 0.40f, 0.16f);                         // zaino
        b.color(234, 236, 240);
        b.ellipsoid(0, 1.07f, 0, 0.33f, 0.25f, 0.29f, 16, 10);                // testa
        b.color(20, 26, 40);
        b.ellipsoid(0, 1.06f, 0.13f, 0.25f, 0.13f, 0.2f, 14, 8);              // visiera
        b.color(52, 56, 66);
        b.box(0, 1.08f, -0.26f, 0.26f, 0.2f, 0.06f);                          // pannello dietro la testa
        b.color(150, 154, 164);
        b.cylinder(0, 1.3f, 1.46f, 0, 0.014f, 0.014f, 5, false, false);       // antenna
        b.color(232, 48, 48);
        b.ellipsoid(0, 1.49f, 0, 0.05f, 0.05f, 0.05f, 8, 5);
        robotBody = b.build(-1, false, true);
        b = new MeshBuilder().color(120, 236, 255);
        b.ellipsoid(-0.085f, 1.06f, 0.305f, 0.04f, 0.065f, 0.03f, 8, 5);
        b.ellipsoid(0.085f, 1.06f, 0.305f, 0.04f, 0.065f, 0.03f, 8, 5);
        b.box(0, 0.7f, 0.205f, 0.12f, 0.08f, 0.03f);                          // lampada sul petto
        robotEyes = b.build(-1, false, true);
        b = new MeshBuilder().color(64, 68, 78);                              // gamba: origine all'anca
        b.cylinder(0, -0.26f, 0, 0, 0.06f, 0.065f, 8, false, false);
        b.color(238, 118, 28);
        b.box(0, -0.31f, 0.04f, 0.15f, 0.11f, 0.26f);
        robotLeg = b.build(-1, false, true);
        b = new MeshBuilder().color(64, 68, 78);                              // braccio: origine alla spalla
        b.cylinder(0, -0.3f, 0, 0, 0.048f, 0.052f, 8, false, false);
        b.color(234, 236, 240);
        b.ellipsoid(0, -0.34f, 0, 0.075f, 0.075f, 0.075f, 8, 5);
        robotArm = b.build(-1, false, true);

        // --- scarabeo (guarda verso z positivo)
        b = new MeshBuilder().color(86, 44, 132);
        b.ellipsoid(0, 0.36f, -0.04f, 0.42f, 0.30f, 0.50f, 16, 9);
        b.color(52, 26, 82);
        b.box(0, 0.38f, -0.04f, 0.02f, 0.6f, 0.98f);                          // giunzione della corazza
        b.color(52, 38, 62);
        b.ellipsoid(0, 0.26f, 0.5f, 0.18f, 0.16f, 0.17f, 10, 6);              // testa
        b.color(30, 22, 34);
        b.box(-0.07f, 0.2f, 0.7f, 0.03f, 0.03f, 0.14f);                       // tenaglie
        b.box(0.07f, 0.2f, 0.7f, 0.03f, 0.03f, 0.14f);
        beetleBody = b.build(-1, false, true);
        b = new MeshBuilder().color(250, 214, 60);
        b.ellipsoid(-0.1f, 0.33f, 0.62f, 0.05f, 0.05f, 0.04f, 8, 5);
        b.ellipsoid(0.1f, 0.33f, 0.62f, 0.05f, 0.05f, 0.04f, 8, 5);
        beetleEyes = b.build(-1, false, true);
        b = new MeshBuilder().color(30, 22, 34);
        b.box(0, -0.12f, 0, 0.05f, 0.24f, 0.05f);
        beetleLeg = b.build(-1, false, true);
    }

    // ------------------------------------------------------------------ fotogramma

    public void render(World3D w, Gfx3D g, float aspect) {
        if (builtFor != w.length) build(w);

        // camera dietro le spalle: segue il giocatore, un po' meno di traverso, e l'altezza del terreno con dolcezza
        float tx = w.px * 0.6f, tz = w.pz;
        if (!camInit) { camX = tx; camZ = tz; camY = w.groundY; camInit = true; }
        camX += (tx - camX) * 0.25f;
        camZ = tz;
        camY += (Math.max(0, w.groundY) - camY) * 0.06f;
        float lift = Math.max(0, w.py - camY) * 0.35f;
        Mat4.lookAt(view, camX, camY + 3.3f + lift, camZ - 6.0f, camX * 0.9f, camY + 1.0f + lift, camZ + 5f);
        Mat4.perspective(proj, 58f, aspect, 0.3f, 400f);
        g.begin(view, proj, FOG_R, FOG_G, FOG_B, 30f, 115f, 0.9f);

        g.backdrop(sky);
        Mat4.setIdentity(m);
        Mat4.translate(m, camX * 0.3f, 0, camZ + 230f);
        g.draw(mountains, m, 1, 1, 1, 1, 1);

        g.draw(pitFloor, ident, 1, 1, 1, 1, 1);
        g.draw(grass, ident, 1, 1, 1, 1, 0);
        g.draw(dirt, ident, 1, 1, 1, 1, 0);
        g.draw(stone, ident, 1, 1, 1, 1, 0);
        g.draw(banks, ident, 1, 1, 1, 1, 0);
        g.draw(forestFloor, ident, 1, 1, 1, 1, 0);
        g.draw(trees, ident, 1, 1, 1, 1, 0);

        float near = w.pz - 4, far = w.pz + 120;      // gli oggetti tra la camera e il robot finirebbero enormi in primo piano
        for (int z = Math.max(0, (int) near); z < Math.min(w.length, (int) far); z++) {
            for (int x = 0; x < World3D.W; x++) {
                if (!w.crate[z * World3D.W + x]) continue;
                Mat4.setIdentity(m);
                Mat4.translate(m, x - World3D.HALF + 0.5f, 0, z + 0.5f);
                g.draw(crate, m, 1, 1, 1, 1, 0.15f);
            }
        }

        // traguardo
        Mat4.setIdentity(m);
        Mat4.translate(m, 0, 0, w.flagZ + 0.5f);
        g.draw(flagPole, m, 1, 1, 1, 1, 0);

        for (float[] c : w.gemsList) {
            if (c[2] < near || c[2] > far) continue;
            Mat4.setIdentity(m);
            Mat4.translate(m, c[0], c[1] + 0.06f * (float) Math.sin(w.time * 3f + c[2]), c[2]);
            Mat4.rotateY(m, w.time * 2.4f + c[2]);
            g.draw(gem, m, 1, 1, 1, 1, 0.45f);
        }
        for (float[] c : w.pops) {
            Mat4.setIdentity(m);
            Mat4.translate(m, c[0], c[1], c[2]);
            Mat4.rotateY(m, c[3] * 14f);
            Mat4.scale(m, 1.3f, 1.3f, 1.3f);
            g.draw(gem, m, 1, 1, 1, 1, 0.7f);
        }
        if (w.battery != null) {
            Mat4.setIdentity(m);
            Mat4.translate(m, w.battery[0], w.battery[1] + 0.07f * (float) Math.sin(w.time * 3f), w.battery[2]);
            Mat4.rotateY(m, w.time * 1.8f);
            Mat4.scale(m, 1.5f, 1.5f, 1.5f);
            g.draw(battery, m, 1, 1, 1, 1, 0.1f);
            g.draw(batteryGlow, m, 1, 1, 1, 1, 0.9f);
        }

        for (Enemy3 e : w.enemies) {
            if (e.dead || e.z < near || e.z > far) continue;
            Mat4.setIdentity(m);
            Mat4.translate(m, e.x, e.y, e.z);
            float yawE = e.alongZ ? (e.dir > 0 ? 0 : (float) Math.PI) : (e.dir > 0 ? (float) Math.PI / 2 : -(float) Math.PI / 2);
            Mat4.rotateY(m, yawE);
            if (e.flung) Mat4.rotateX(m, e.anim * 3f);
            if (e.flatTime > 0) Mat4.scale(m, 1.15f, 0.22f, 1.15f);
            g.draw(beetleBody, m, 1, 1, 1, 1, 0);
            g.draw(beetleEyes, m, 1, 1, 1, 1, 0.9f);
            for (int i = 0; i < 6; i++) {
                float lx = (i % 2 == 0 ? -1 : 1) * 0.36f, lz = -0.3f + (i / 2) * 0.28f;
                System.arraycopy(m, 0, m2, 0, 16);
                Mat4.translate(m2, lx, 0.24f, lz);
                Mat4.rotateX(m2, (float) Math.sin(e.anim * 14f + i * 2.1f) * 0.5f);
                g.draw(beetleLeg, m2, 1, 1, 1, 1, 0);
            }
        }

        drawRobot(w, g);

        // ---- trasparenze: ombre, anelli, bandiera
        if (w.topAt(w.px, w.pz) > -500 && w.state != World3D.DYING) {       // niente ombra sopra il vuoto
            float sh = Math.max(0.35f, 1f - (w.py - w.groundY) * 0.14f);
            Mat4.setIdentity(m);
            Mat4.translate(m, w.px, w.groundY + 0.03f, w.pz);
            Mat4.scale(m, 1.25f * sh, 1, 1.25f * sh);
            g.draw(shadow, m, 1, 1, 1, 1, 1);
        }
        for (Enemy3 e : w.enemies) {
            if (e.dead || e.flung || e.z < near || e.z > far) continue;
            Mat4.setIdentity(m);
            Mat4.translate(m, e.x, e.y + 0.03f, e.z);
            Mat4.scale(m, 1.5f, 1, 1.5f);
            g.draw(shadow, m, 1, 1, 1, 0.8f, 1);
        }
        if (w.spinning()) {
            for (int i = 0; i < 3; i++) {
                float k = (0.62f + 0.14f * (i == 1 ? 1 : 0)) * (1f + 0.07f * (float) Math.sin(w.time * 40f + i));
                Mat4.setIdentity(m);
                Mat4.translate(m, w.px, w.py + 0.25f + i * 0.42f, w.pz);
                Mat4.scale(m, k, 1, k);
                g.draw(ring, m, 1, 1, 1, 0.75f, 1);
            }
        }
        if (w.djTime > 0) {
            float f = w.djTime / 0.3f, k = 1.3f - 0.7f * f;
            Mat4.setIdentity(m);
            Mat4.translate(m, w.djX, w.djY, w.djZ);
            Mat4.scale(m, k, 1, k);
            g.draw(ring, m, 1, 1, 1, f * 0.9f, 1);
        }
        float drop = w.state == World3D.WON ? Math.min(1f, w.stateTime / 1.0f) : 0;
        Mat4.setIdentity(m);
        Mat4.translate(m, 0, 4.9f - drop * 3.9f, w.flagZ + 0.5f);
        g.draw(flagCloth, m, 1, 1, 1, 1, 0.6f);

        g.end();
    }

    private void drawRobot(World3D w, Gfx3D g) {
        boolean blink = w.state == World3D.PLAYING && w.invuln > 0 && !w.spinning() && ((int) (w.time * 14f) % 2 == 0);
        if (blink) return;
        float[] b = m;
        Mat4.setIdentity(b);
        Mat4.translate(b, w.px, w.py, w.pz);
        Mat4.rotateY(b, w.yaw + (w.spinning() ? w.spinAngle : 0));
        if (w.state == World3D.DYING) Mat4.rotateX(b, -w.stateTime * 5f);
        g.draw(robotBody, b, 1, 1, 1, 1, 0);
        g.draw(robotEyes, b, 1, 1, 1, 1, 1);

        float legA, legB, armA, armB, spread = 0.12f;
        if (w.spinning()) {
            legA = 0.15f; legB = -0.15f; armA = armB = 0; spread = 1.45f;
        } else if (!w.onGround) {
            legA = 0.7f; legB = -0.5f; armA = armB = -2.3f; spread = 0.3f;
        } else {
            float sp = (float) Math.sqrt(w.vx * w.vx + w.vz * w.vz);
            float amp = Math.min(1f, sp / 3f) * 0.85f;
            legA = (float) Math.sin(w.runPhase) * amp;
            legB = -legA;
            armA = -legA * 0.8f;
            armB = -legB * 0.8f;
        }
        limb(g, b, robotLeg, -0.11f, 0.37f, 0, 0, legA);
        limb(g, b, robotLeg, 0.11f, 0.37f, 0, 0, legB);
        limb(g, b, robotArm, -0.27f, 0.8f, 0, -spread, armA);
        limb(g, b, robotArm, 0.27f, 0.8f, 0, spread, armB);
    }

    private void limb(Gfx3D g, float[] base, Mesh mesh, float x, float y, float z, float rz, float rx) {
        System.arraycopy(base, 0, m2, 0, 16);
        Mat4.translate(m2, x, y, z);
        if (rz != 0) Mat4.rotateZ(m2, rz);
        if (rx != 0) Mat4.rotateX(m2, rx);
        g.draw(mesh, m2, 1, 1, 1, 1, 0);
    }
}
