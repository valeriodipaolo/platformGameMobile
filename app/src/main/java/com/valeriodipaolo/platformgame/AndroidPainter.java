package com.valeriodipaolo.platformgame;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;

import com.valeriodipaolo.platformgame.core.Img;
import com.valeriodipaolo.platformgame.core.Painter;

/** Disegna le immagini del gioco su un Canvas Android. */
final class AndroidPainter implements Painter {
    private final Bitmap[] bitmaps = new Bitmap[Img.COUNT];
    private final Paint imagePaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint fillPaint = new Paint();
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF dst = new RectF();
    private Canvas canvas;

    AndroidPainter(Context ctx) {
        Resources res = ctx.getResources();
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inScaled = false;
        for (int i = 0; i < Img.COUNT; i++) {
            int id = res.getIdentifier(Img.NAMES[i], "drawable", ctx.getPackageName());
            bitmaps[i] = BitmapFactory.decodeResource(res, id, opts);
            if (bitmaps[i] == null) throw new IllegalStateException("Immagine mancante: " + Img.NAMES[i]);
        }
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
    }

    void begin(Canvas c) {
        canvas = c;
    }

    @Override
    public float aspect(int img) {
        return bitmaps[img].getWidth() / (float) bitmaps[img].getHeight();
    }

    @Override
    public void image(int img, float x, float y, float w, float h, boolean flipX, float alpha) {
        imagePaint.setAlpha(Math.max(0, Math.min(255, Math.round(alpha * 255f))));
        dst.set(x, y, x + w, y + h);
        if (flipX) {
            canvas.save();
            canvas.scale(-1f, 1f, x + w / 2f, y + h / 2f);
            canvas.drawBitmap(bitmaps[img], null, dst, imagePaint);
            canvas.restore();
        } else {
            canvas.drawBitmap(bitmaps[img], null, dst, imagePaint);
        }
    }

    @Override
    public void rect(float x, float y, float w, float h, int argb) {
        fillPaint.setColor(argb);
        canvas.drawRect(x, y, x + w, y + h, fillPaint);
    }

    @Override
    public void text(String s, float x, float y, float size, int align, int argb) {
        textPaint.setTextSize(size);
        textPaint.setTextAlign(align == ALIGN_CENTER ? Paint.Align.CENTER
                : align == ALIGN_RIGHT ? Paint.Align.RIGHT : Paint.Align.LEFT);
        textPaint.setColor(0xA0000000);
        canvas.drawText(s, x + size * 0.06f, y + size * 0.06f, textPaint);
        textPaint.setColor(argb);
        canvas.drawText(s, x, y, textPaint);
    }
}
