package eit.fourspace.stufftracker;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

public class OverlayDrawable extends Drawable {
    Paint redPaint, greenPaint;
    private int i = 0;
    OverlayDrawable() {
        redPaint = new Paint();
        redPaint.setARGB(125, 255, 0, 0);
        greenPaint = new Paint();
        greenPaint.setARGB(125, 0, 255, 0);
    }

    @Override
    public void draw(Canvas canvas) {
        // Get the drawable's bounds
        int width = getBounds().width();
        int height = getBounds().height();
        float radius = Math.min(width, height) / 2;
        // Draw a red circle in the center
        if (i++ % 2 == 1) {
            canvas.drawCircle(width/2, height/2, radius, redPaint);
        } else {
            canvas.drawCircle(width / 2, height / 2, radius, greenPaint);
        }
    }
    @Override
    public void setAlpha(int alpha) {}

    @Override
    public void setColorFilter(ColorFilter filter) {}

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSPARENT;
    }
}
