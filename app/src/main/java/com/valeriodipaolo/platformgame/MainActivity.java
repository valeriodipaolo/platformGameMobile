package com.valeriodipaolo.platformgame;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class MainActivity extends Activity {
    private FrameLayout root;
    private View current;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= 28) {      // disegna anche nella zona della tacca
            WindowManager.LayoutParams lp = w.getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            w.setAttributes(lp);
        }
        root = new FrameLayout(this);
        setContentView(root);
        showMenu();
    }

    private void show(View v) {
        root.removeAllViews();
        current = v;
        root.addView(v, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        hideSystemBars();
    }

    private void showMenu() {
        show(new MenuView(this, new MenuView.Listener() {
            @Override
            public void onLevel(int level) {
                startLevel(level);
            }
        }));
    }

    /** Livelli 1, 3, 5 e 7 sono 2D, 2, 4, 6 e 8 sono 3D. Finito un livello si passa al successivo; dopo l'ultimo si torna al menu. */
    private void startLevel(final int level) {
        Runnable next = new Runnable() {
            @Override
            public void run() {
                if (level < 8) startLevel(level + 1); else showMenu();
            }
        };
        if (level % 2 == 1) show(new GameView(this, level, next));
        else show(new Level3DView(this, level, next));
    }

    /** Il tasto indietro riporta al menu; dal menu chiude il gioco. */
    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (current instanceof MenuView) super.onBackPressed();
        else showMenu();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (current instanceof Level3DView) ((Level3DView) current).pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (current instanceof Level3DView) ((Level3DView) current).resume();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemBars();
    }

    @SuppressWarnings("deprecation")
    private void hideSystemBars() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }
}
