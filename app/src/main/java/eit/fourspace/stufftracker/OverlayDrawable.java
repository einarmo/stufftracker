package eit.fourspace.stufftracker;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import eit.fourspace.stufftracker.calculationflow.ItemRenderer;
import eit.fourspace.stufftracker.calculationflow.ObjectWrapper;

public class OverlayDrawable extends Drawable {
    private Paint redPaint, greenPaint;
    private ItemRenderer renderer;
    private static final int BASE_RADIUS = 15;

    OverlayDrawable(ItemRenderer renderer) {
        this.renderer = renderer;
        redPaint = new Paint();
        redPaint.setARGB(125, 255, 0, 0);
        greenPaint = new Paint();
        greenPaint.setARGB(125, 0, 255, 0);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        ArrayList<ObjectWrapper> objects = renderer.getObjects();
        for (int i = 0; i < objects.size(); i++) {
            ObjectWrapper obj = objects.get(i);
            if (!obj.visible || obj.filtered) continue;
            canvas.drawCircle((float)obj.projection.getX(), (float)obj.projection.getY(), BASE_RADIUS, redPaint);
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
