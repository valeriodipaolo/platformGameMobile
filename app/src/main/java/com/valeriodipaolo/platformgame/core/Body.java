package com.valeriodipaolo.platformgame.core;

/** Rettangolo che si muove nel livello e urta contro le caselle solide. x,y = angolo in alto a sinistra, in caselle. */
public class Body {
    public float x, y, w, h, vx, vy;
    public boolean onGround, hitWall;
    /** Casella colpita con la testa nell'ultimo passo (-1 se nessuna). */
    public int headTx = -1, headTy = -1;

    private static final float EPS = 0.001f;

    public boolean overlaps(Body o) {
        return x < o.x + o.w && x + w > o.x && y < o.y + o.h && y + h > o.y;
    }

    public void move(Level lv, float dt) {
        hitWall = false;
        headTx = headTy = -1;

        x += vx * dt;
        int y0 = (int) Math.floor(y), y1 = (int) Math.floor(y + h - EPS);
        if (vx > 0) {
            int tx = (int) Math.floor(x + w - EPS);
            for (int ty = y0; ty <= y1; ty++) {
                if (lv.solid(tx, ty)) { x = tx - w; vx = 0; hitWall = true; break; }
            }
        } else if (vx < 0) {
            int tx = (int) Math.floor(x);
            for (int ty = y0; ty <= y1; ty++) {
                if (lv.solid(tx, ty)) { x = tx + 1; vx = 0; hitWall = true; break; }
            }
        }

        float prevBottom = y + h;
        y += vy * dt;
        onGround = false;
        int x0 = (int) Math.floor(x), x1 = (int) Math.floor(x + w - EPS);
        if (vy > 0) {
            int ty = (int) Math.floor(y + h - EPS);
            for (int tx = x0; tx <= x1; tx++) {
                // la finta parete regge chi ci arriva da sopra (sembra pietra), ma di lato ci si passa
                if (lv.solid(tx, ty) || (lv.get(tx, ty) == Level.FAKE && prevBottom <= ty + 0.02f)) { y = ty - h; vy = 0; onGround = true; break; }
            }
        } else if (vy < 0) {
            int ty = (int) Math.floor(y);
            float cx = x + w / 2, best = Float.MAX_VALUE;
            for (int tx = x0; tx <= x1; tx++) {
                if (lv.solid(tx, ty)) {
                    float d = Math.abs(tx + 0.5f - cx);
                    if (d < best) { best = d; headTx = tx; headTy = ty; }
                }
            }
            if (headTx >= 0) {
                int hits = 0;
                for (int tx = x0; tx <= x1; tx++) if (lv.solid(tx, ty)) hits++;
                // spigolo: se tocca solo per un soffio, scivola di lato e continua a salire
                float ov = Math.min(x + w, headTx + 1) - Math.max(x, headTx);
                if (hits == 1 && ov < 0.28f && x1 > x0) {
                    if (headTx == x0) { x = headTx + 1 + EPS; headTx = headTy = -1; }
                    else if (headTx == x1) { x = headTx - w - EPS; headTx = headTy = -1; }
                }
                if (headTx >= 0) { y = ty + 1; vy = 0; }
            }
        }
    }
}
