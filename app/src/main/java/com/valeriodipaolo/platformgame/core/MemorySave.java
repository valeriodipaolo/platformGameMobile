package com.valeriodipaolo.platformgame.core;

import java.util.HashSet;
import java.util.Set;

/** Salvataggio in memoria, per i test e lo strumento di anteprima sul PC. */
public final class MemorySave implements Save {
    private final Set<String> keys = new HashSet<String>();

    @Override
    public boolean has(String key) { return keys.contains(key); }

    @Override
    public void set(String key) { keys.add(key); }
}
