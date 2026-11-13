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

    // comandi: scritti dal thread dei tocchi, letti dal thread grafico
    private volatile float joyX, joyY;
    private volatile boolean inJump, inSpin, tapped, exited;
    private final AtomicBoolean latchJump = new AtomicBoolean(), latchSpin = new AtomicBoolean();

    Level3DView(Context ctx, Runnable onExit) {
        super(ctx);
        this.onExit = onExit;
        world = new World3D(new AndroidSave(ctx));
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
        w.setInput(joyX, joyY, inJump || j, inSpin || s);
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
            hud.draw(world, painter, getWidth(), getHeight(), joyId != -1, joyCx, joyCy, joyDx, joyDy, renderer.error);
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
                    joyId = e.getPointerId(idx);
                    joyCx = Math.max(radius, e.getX(idx));
                    joyCy = Math.max(radius, Math.min(h - radius * 0.6f, e.getY(idx)));
                }
            }
            int lifted = (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) ? idx : -1;
            if (lifted != -1 && e.getPointerId(lifted) == joyId) joyId = -1;

            boolean j = false, s = false;
            float dx = 0, dy = 0;
            for (int i = 0; i < e.getPointerCount(); i++) {
                if (i == lifted) continue;
                if (e.getPointerId(i) == joyId) {
                    dx = (e.getX(i) - joyCx) / radius;
                    dy = (e.getY(i) - joyCy) / radius;
                    float len = (float) Math.sqrt(dx * dx + dy * dy);
                    if (len > 1f) {                  // se il dito esce dal cerchio, il joystick lo segue
                        joyCx += (len - 1f) * radius * dx / len;
                        joyCy += (len - 1f) * radius * dy / len;
                        dx /= len;
                        dy /= len;
                    }
                } else if (e.getX(i) >= w * 0.5f) {
                    int zone = Controls.zone(e.getX(i), e.getY(i), w, h);
                    if (zone == Controls.JUMP) j = true;
                    else if (zone == Controls.SPIN) s = true;
                }
            }
            joyDx = dx;
            joyDy = dy;
            // zona morta piccola e piena velocità già a poco più di metà corsa: risposta pronta
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len < 0.12f) {
                joyX = joyY = 0;
            } else {
                float k = Math.min(1f, len / 0.6f) / len;
                joyX = dx * k;
                joyY = -dy * k;
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
