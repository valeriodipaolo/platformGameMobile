package com.valeriodipaolo.platformgame;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.os.Process;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.valeriodipaolo.platformgame.core.Controls;
import com.valeriodipaolo.platformgame.core.Game;
import com.valeriodipaolo.platformgame.core.Renderer;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

/** Superficie di gioco: un thread aggiorna la partita e la ridisegna a ogni fotogramma. */
final class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private final Game game;
    private final Renderer renderer = new Renderer();
    private final AndroidPainter painter;

    private Thread thread;
    private volatile boolean running;
    private volatile boolean inLeft, inRight, inJump, inSpin, tapped;
    // Un tocco molto breve può iniziare e finire tra due fotogrammi: la pressione viene "memorizzata" finché il gioco non la legge.
    private final AtomicBoolean latchJump = new AtomicBoolean(), latchSpin = new AtomicBoolean();
    private boolean prevJump, prevSpin;
    private boolean hardwareCanvas = Build.VERSION.SDK_INT >= 26;

    private final Runnable onLevelComplete;
    private boolean completed;

    /** onLevelComplete: chiamato (sul thread dell'interfaccia) quando si tocca lo schermo a livello completato. */
    GameView(Context ctx, int levelNo, Runnable onLevelComplete) {
        super(ctx);
        Renderer.character = Math.max(0, Math.min(4, ctx.getSharedPreferences("platformgame", Context.MODE_PRIVATE).getInt("hero", 0)));
        this.onLevelComplete = onLevelComplete;
        game = new Game(new AndroidSave(ctx), levelNo);
        painter = new AndroidPainter(ctx);
        getHolder().addCallback(this);
        setKeepScreenOn(true);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        running = true;
        thread = new Thread(this, "game-loop");
        thread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (Build.VERSION.SDK_INT >= 30) {          // chiede la frequenza massima dello schermo: meno ritardo tra tocco e immagine
            try {
                holder.getSurface().setFrameRate(120f, Surface.FRAME_RATE_COMPATIBILITY_DEFAULT);
            } catch (RuntimeException ignored) {
                // non supportato: si resta alla frequenza normale
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (Build.VERSION.SDK_INT >= 29) {          // i gesti di sistema ai bordi (es. "indietro") non devono rubare i tocchi ai pulsanti
            setSystemGestureExclusionRects(Collections.singletonList(new Rect(0, 0, r - l, b - t)));
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        running = false;
        if (thread != null) {
            try {
                thread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            thread = null;
        }
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY);
        SurfaceHolder holder = getHolder();
        long last = System.nanoTime();
        while (running) {
            Canvas c = null;
            try {
                c = lock(holder);
                if (c == null) {
                    sleep(10);
                    last = System.nanoTime();
                    continue;
                }
                long now = System.nanoTime();
                float dt = (now - last) / 1e9f;
                last = now;

                if (tapped) {
                    tapped = false;
                    if (game.state == Game.WON && game.stateTime > 1.6f) {
                        if (!completed) {
                            completed = true;
                            post(onLevelComplete);
                        }
                    } else {
                        game.tap();
                    }
                }
                boolean pressedJump = latchJump.getAndSet(false), pressedSpin = latchSpin.getAndSet(false);
                game.setInput(inLeft, inRight, inJump || pressedJump, inSpin || pressedSpin);
                game.update(dt);
                painter.begin(c);
                renderer.draw(game, painter, c.getWidth(), c.getHeight());
            } finally {
                if (c != null) {
                    try {
                        holder.unlockCanvasAndPost(c);
                    } catch (RuntimeException ignored) {
                        // la superficie è stata chiusa nel frattempo
                    }
                }
            }
        }
    }

    /** Preferisce il canvas accelerato dalla GPU; se il dispositivo non lo supporta ripiega su quello software. */
    private Canvas lock(SurfaceHolder holder) {
        if (hardwareCanvas) {
            try {
                return holder.lockHardwareCanvas();
            } catch (RuntimeException e) {
                hardwareCanvas = false;
            }
        }
        try {
            return holder.lockCanvas();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int action = e.getActionMasked();
        boolean l = false, r = false, j = false, sp = false;
        if (action != MotionEvent.ACTION_CANCEL) {
            int lifted = (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP)
                    ? e.getActionIndex() : -1;
            for (int i = 0; i < e.getPointerCount(); i++) {
                if (i == lifted) continue;
                int zone = Controls.zone(e.getX(i), e.getY(i), getWidth(), getHeight());
                if (zone == Controls.LEFT) l = true;
                else if (zone == Controls.RIGHT) r = true;
                else if (zone == Controls.JUMP) j = true;
                else if (zone == Controls.SPIN) sp = true;
            }
        }
        inLeft = l;
        inRight = r;
        inJump = j;
        inSpin = sp;
        if (j && !prevJump) latchJump.set(true);
        if (sp && !prevSpin) latchSpin.set(true);
        prevJump = j;
        prevSpin = sp;
        if (action == MotionEvent.ACTION_DOWN) {
            tapped = true;
            requestUnbufferedDispatch(e);           // consegna i tocchi subito, senza raggrupparli per fotogramma
        }
        return true;
    }
}
