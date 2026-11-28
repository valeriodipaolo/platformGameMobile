package com.valeriodipaolo.platformgame;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.valeriodipaolo.platformgame.core.Controls;
import com.valeriodipaolo.platformgame.core3d.Hud3D;
import com.valeriodipaolo.platformgame.core3d.World3D;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

/** Schermata del livello 3D: sotto la scena OpenGL, sopra scritte, pulsanti e joystick. */
final class Level3DView extends FrameLayout {
    private final World3D world;
    private final GLSurfaceView gl;
    private final GlRenderer renderer;
    private final Runnable onExit;
    /** Schema di movimento (1..5, vedi MenuView): fisso = joystick sempre nello stesso punto; 4 direzioni = niente diagonali. */

    // schema 6: angolo entro cui si blocca sull'asse, guadagno sulle diagonali, corsa del joystick per la piena velocità
    /** Comandi digitali: la levetta dà solo otto direzioni, sempre a piena velocità (niente pressione intermedia). */
    private final boolean digital;
    /** Croce direzionale: quattro bracci (le diagonali solo negli angoli), tutto o niente. */
    private final boolean dpad;
    private static final float SNAP_DEG = 20f, DIAG_GAIN = 0.6f, FULL_X = 0.7f, FULL_Y = 0.5f;

    // comandi: scritti dal thread dei tocchi, letti dal thread grafico
    private volatile float joyX, joyY;
    private volatile boolean inJump, inSpin, tapped, exited;
    private final AtomicBoolean latchJump = new AtomicBoolean(), latchSpin = new AtomicBoolean();

    Level3DView(Context ctx, int levelNo, Runnable onExit) {
        super(ctx);
        this.onExit = onExit;
        com.valeriodipaolo.platformgame.core3d.World3D.fastFall = true;     // dopo il salto si torna a terra in fretta
        int mode = ctx.getSharedPreferences("platformgame", Context.MODE_PRIVATE).getInt("ctrlmode", 0);
        digital = mode == 1;
        dpad = mode == 2;
        world = new World3D(new AndroidSave(ctx), levelNo);
        gl = new GLSurfaceView(ctx);
        gl.setEGLContextClientVersion(2);
        gl.setEGLConfigChooser(8, 8, 8, 0, 24, 0);
        gl.setPreserveEGLContextOnPause(true);
        renderer = new GlRenderer(AndroidPainter.bitmaps(ctx), world, this);
        gl.setRenderer(renderer);
        addView(gl, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        addView(new Hud(ctx), new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        setKeepScreenOn(true);
    }

    void pause() {
        gl.onPause();
    }

    void resume() {
        gl.onResume();
    }

    /** Chiamato dal thread grafico a ogni fotogramma: applica i comandi e fa avanzare la partita. */
    void step(World3D w, float dt) {
        if (tapped) {
            tapped = false;
            if (w.wonAndReady()) {
                if (!exited) {
                    exited = true;
                    post(onExit);
                }
            } else {
                w.tapRestart();
            }
        }
        boolean j = latchJump.getAndSet(false), s = latchSpin.getAndSet(false);
        boolean facing = w.pz < w.camBackUntil;          // fuga dal masso: la camera guarda il robot da davanti, i comandi si girano con lei
        w.setInput(facing ? -joyX : joyX, facing ? -joyY : joyY, inJump || j, inSpin || s);
        w.update(dt);
    }

    /** Livello trasparente sopra la scena: disegna l'interfaccia e riceve i tocchi. */
    private final class Hud extends View {
        private final AndroidPainter painter;
        private final Hud3D hud = new Hud3D();
        private int joyId = -1;
        private float joyCx, joyCy, joyDx, joyDy;
        private boolean prevJump, prevSpin;

        Hud(Context ctx) {
            super(ctx);
            painter = new AndroidPainter(ctx);
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            super.onLayout(changed, l, t, r, b);
            if (Build.VERSION.SDK_INT >= 29) {
                setSystemGestureExclusionRects(Collections.singletonList(new Rect(0, 0, r - l, b - t)));
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            painter.begin(canvas);
            hud.dpad = dpad;
            hud.draw(world, painter, getWidth(), getHeight(), dpad || joyId != -1 || !world.carts.isEmpty(), joyCx, joyCy, joyDx, joyDy, renderer.error);
            postInvalidateOnAnimation();
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            int action = e.getActionMasked();
            float w = getWidth(), h = getHeight(), radius = Hud3D.joyRadius(w, h);
            if (action == MotionEvent.ACTION_CANCEL) {
                joyId = -1;
                joyDx = joyDy = 0;
                joyX = joyY = 0;
                inJump = inSpin = false;
                prevJump = prevSpin = false;
                return true;
            }
            int idx = e.getActionIndex();
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                if (action == MotionEvent.ACTION_DOWN) {
                    tapped = true;
                    requestUnbufferedDispatch(e);
                }
                // metà sinistra: il joystick nasce dove si appoggia il dito
                if (e.getX(idx) < w * 0.5f && joyId == -1) {
                    // joystick fisso: si prende se il tocco cade vicino, in una zona ampia attorno al disegno
                    float hx = Hud3D.joyHomeX(w, h), hy = Hud3D.joyHomeY(w, h);
                    float ddx = e.getX(idx) - hx, ddy = e.getY(idx) - hy;
                    if (Math.sqrt(ddx * ddx + ddy * ddy) <= radius * 2.1f) {
                        joyId = e.getPointerId(idx);
                        joyCx = hx;
                        joyCy = hy;
                    }
                }
            }
            int lifted = (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) ? idx : -1;
            if (lifted != -1 && e.getPointerId(lifted) == joyId) { joyId = -1; }
            if (!world.carts.isEmpty()) {                    // carrello e corse: solo destra/sinistra (come nel 2D), salto e giravolta
                boolean l = false, r = false, j = false, sp = false;
                for (int i = 0; i < e.getPointerCount(); i++) {
                    if (i == lifted) continue;
                    int zone = Controls.zone(e.getX(i), e.getY(i), w, h);
                    if (zone == Controls.LEFT) l = true; else if (zone == Controls.RIGHT) r = true;
                    else if (zone == Controls.JUMP) j = true; else if (zone == Controls.SPIN) sp = true;
                }
                joyId = -1;
                joyX = (r ? 1f : 0f) - (l ? 1f : 0f);
                joyY = 0;
                joyDx = joyX; joyDy = 0;
                inJump = j; inSpin = sp;
                return true;
            }

            boolean j = false, s = false;
            float dx = 0, dy = 0;
            boolean[] cross = new boolean[4];
            for (int i = 0; i < e.getPointerCount(); i++) {
                if (i == lifted) continue;
                if (dpad && e.getX(i) < w * 0.5f) {           // croce: si cammina solo con il dito sopra un pulsante
                    int m = Hud3D.crossButtons(e.getX(i), e.getY(i), w, h);      // un dito tra due pulsanti li preme entrambi
                    for (int k = 0; k < 4; k++) if ((m & (1 << k)) != 0) cross[k] = true;
                    continue;
                }
                if (e.getPointerId(i) == joyId) {
                    dx = (e.getX(i) - joyCx) / radius;
                    dy = (e.getY(i) - joyCy) / radius;
                    float len = (float) Math.sqrt(dx * dx + dy * dy);
                    if (len > 1f) {                  // se il dito esce dal cerchio conta solo la direzione
                        dx /= len;
                        dy /= len;
                    }
                } else if (e.getX(i) >= w * 0.5f) {
                    int zone = Controls.zone(e.getX(i), e.getY(i), w, h);
                    if (zone == Controls.JUMP) j = true;
                    else if (zone == Controls.SPIN) s = true;
                }
            }
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            joyDx = dx;
            joyDy = dy;
            // zona morta piccola e piena velocità già a poco più di metà corsa: risposta pronta
            if (dpad) {
                // croce: direzione pura del pulsante sotto il dito (due dita su due pulsanti = diagonale); fuori dai pulsanti fermo
                joyX = (cross[3] ? 1f : 0f) - (cross[2] ? 1f : 0f);
                joyY = (cross[0] ? 1f : 0f) - (cross[1] ? 1f : 0f);
                float jl = (float) Math.sqrt(joyX * joyX + joyY * joyY);
                if (jl > 1f) { joyX /= jl; joyY /= jl; }
                joyDx = joyX;
                joyDy = -joyY;
            } else if (len < (digital ? 0.3f : 0.12f)) {
                joyX = joyY = 0;
            } else if (digital) {
                // otto direzioni, tutto o niente: l'angolo si arrotonda al multiplo di 45° più vicino e la velocità è sempre piena
                double ang = Math.round(Math.atan2(-dy, dx) / (Math.PI / 4)) * (Math.PI / 4);
                joyX = (float) Math.cos(ang);
                joyY = (float) Math.sin(ang);
                if (Math.abs(joyX) < 1e-3f) joyX = 0;
                if (Math.abs(joyY) < 1e-3f) joyY = 0;
            } else {
                // comandi: gli assi principali si "calamitano" e vanno a piena velocità; le diagonali sono più dolci.
                // Il verticale arriva al massimo con poca corsa, l'orizzontale ne chiede di più (movimento laterale più fine).
                float ax = Math.abs(dx), ay = Math.abs(dy);
                float minor = Math.min(ax, ay), major = Math.max(ax, ay);
                float angle = (float) Math.toDegrees(Math.atan2(minor, major));      // 0 = asse puro, 45 = diagonale
                if (angle < SNAP_DEG) {
                    if (ax >= ay) dy = 0; else dx = 0;
                }
                float diag = angle < SNAP_DEG ? 1f : 1f - (1f - DIAG_GAIN) * Math.min(1f, (angle - SNAP_DEG) / (45f - SNAP_DEG));
                joyX = Math.signum(dx) * Math.min(1f, Math.abs(dx) / FULL_X) * diag;
                joyY = -Math.signum(dy) * Math.min(1f, Math.abs(dy) / FULL_Y) * diag;
                joyDx = dx;
                joyDy = dy;
            }
            inJump = j;
            inSpin = s;
            if (j && !prevJump) latchJump.set(true);
            if (s && !prevSpin) latchSpin.set(true);
            prevJump = j;
            prevSpin = s;
            return true;
        }
    }
}
