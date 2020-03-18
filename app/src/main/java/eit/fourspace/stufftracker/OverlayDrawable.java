package eit.fourspace.stufftracker;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.LinkedList;

import androidx.annotation.NonNull;
import eit.fourspace.stufftracker.calculationflow.ItemRenderer;
import eit.fourspace.stufftracker.calculationflow.ObjectWrapper;
import eit.fourspace.stufftracker.calculationflow.OrbitWrapper;

import static eit.fourspace.stufftracker.calculationflow.ObjectClass.PAYLOAD;
import static eit.fourspace.stufftracker.calculationflow.ObjectClass.ROCKET_BODY;
import static eit.fourspace.stufftracker.calculationflow.ObjectClass.STATION;

public class OverlayDrawable extends Drawable {
    private Paint redPaint, greenPaint, greyPaint, yellowPaint, bluePaint;
    private ItemRenderer renderer;
    public static final String TAG = "OverlayDrawable";
    private static final int BASE_RADIUS = 15;

    OverlayDrawable(ItemRenderer renderer) {
        this.renderer = renderer;
        redPaint = new Paint();
        redPaint.setARGB(125, 255, 0, 0);
        greenPaint = new Paint();
        greenPaint.setARGB(125, 0, 255, 0);
        greyPaint = new Paint();
        greyPaint.setARGB(125, 175, 175, 175);
        yellowPaint = new Paint();
        yellowPaint.setARGB(125, 175, 175, 0);
        bluePaint = new Paint();
        bluePaint.setARGB(125, 0, 0, 175);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        renderer.screenHeight = getBounds().height();
        renderer.screenWidth = getBounds().width();
        ArrayList<ObjectWrapper> objects = renderer.getObjects();
        for (int i = 0; i < objects.size(); i++) {
            ObjectWrapper obj = objects.get(i);
            if (!obj.visible || obj.filtered) continue;
            Paint paint;
            int radius = BASE_RADIUS;
            switch (obj.objectClass) {
                case PAYLOAD:
                    paint = redPaint;
                    break;
                case ROCKET_BODY:
                    paint = greenPaint;
                    break;
                case STATION:
                    paint = yellowPaint;
                    radius = BASE_RADIUS*2;
                    break;
                default:
                    paint = greyPaint;
                    break;
            }
            canvas.drawCircle((float)obj.projection.getX(), (float)obj.projection.getY(), radius, paint);
            if (obj.selected) {
                Paint roundPaint = new Paint(paint);
                roundPaint.setStyle(Paint.Style.STROKE);
                roundPaint.setStrokeWidth(4);
                canvas.drawCircle((float)obj.projection.getX(), (float)obj.projection.getY(), radius+6, roundPaint);
            }
        }
        LinkedList<OrbitWrapper> orbits = renderer.getOrbits();
        for (OrbitWrapper orbit : orbits) {
            if (!orbit.initialized || !orbit.rendered) continue;
            Paint paint = bluePaint;
            paint.setStrokeWidth(4);
            for (int j = 0; j < OrbitWrapper.NUM_POINTS; j++) {
                if (orbit.rotatedPositions[j].getZ() >= 0 || orbit.rotatedPositions[(j + 1) % OrbitWrapper.NUM_POINTS].getZ() >= 0) continue;
                canvas.drawLine((float)orbit.projections[j].getX(),
                        (float)orbit.projections[j].getY(),
                        (float)orbit.projections[(j+1) % OrbitWrapper.NUM_POINTS].getX(),
                        (float)orbit.projections[(j+1) % OrbitWrapper.NUM_POINTS].getY(),
                        paint);
            }
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
