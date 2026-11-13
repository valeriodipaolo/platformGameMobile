package com.valeriodipaolo.platformgame;

import android.content.Context;
import android.content.SharedPreferences;

import com.valeriodipaolo.platformgame.core.Save;

/** Salva i collezionabili nelle preferenze dell'app: restano anche dopo aver chiuso il gioco. */
final class AndroidSave implements Save {
    private final SharedPreferences prefs;

    AndroidSave(Context ctx) {
        prefs = ctx.getSharedPreferences("platformgame", Context.MODE_PRIVATE);
    }

    @Override
    public boolean has(String key) { return prefs.getBoolean(key, false); }

    @Override
    public void set(String key) { prefs.edit().putBoolean(key, true).apply(); }
}
