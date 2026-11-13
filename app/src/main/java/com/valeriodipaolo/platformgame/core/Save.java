package com.valeriodipaolo.platformgame.core;

/** Memoria permanente dei collezionabili ottenuti (chiave presente = ottenuto). */
public interface Save {
    boolean has(String key);

    void set(String key);
}
