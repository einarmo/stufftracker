package eit.fourspace.stufftracker;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.util.Log;

import org.hipparchus.geometry.euclidean.twod.Vector2D;
import org.hipparchus.util.FastMath;

import java.util.ArrayList;
import java.util.LinkedList;

import androidx.annotation.NonNull;
import eit.fourspace.stufftracker.calculationflow.ItemRenderer;
import eit.fourspace.stufftracker.calculationflow.ObjectWrapper;
import eit.fourspace.stufftracker.calculationflow.OrbitWrapper;
import eit.fourspace.stufftracker.config.ConfigData;

public class OverlayDrawable extends Drawable {
    private Paint redPaint, greenPaint, greyPaint, yellowPaint, bluePaint, purplePaint, densePaint;
    private ItemRenderer renderer;
    private ConfigData config;
    public static final String TAG = "OverlayDrawable";
    private static final int BASE_RADIUS = 15;

    OverlayDrawable(ItemRenderer renderer, ConfigData config) {
        this.renderer = renderer;
        this.config = config;
        redPaint = new Paint();
        redPaint.setARGB(125, 255, 0, 0);
        greenPaint = new Paint();
        greenPaint.setARGB(125, 0, 255, 0);
        greyPaint = new Paint();
        greyPaint.setARGB(125, 175, 175, 175);
        yellowPaint = new Paint();
        yellowPaint.setARGB(125, 175, 175, 0);
        bluePaint = new Paint();
        bluePaint.setStrokeWidth(4);
        bluePaint.setARGB(125, 0, 0, 175);
        purplePaint = new Paint();
        purplePaint.setARGB(125, 139, 0, 139);
        purplePaint.setStrokeWidth(6);
        densePaint = new Paint();
        densePaint.setStrokeWidth(8);
        densePaint.setARGB(255, 180, 0, 180);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        int height = getBounds().height();
        int width = getBounds().width();
        renderer.screenHeight = height;
        renderer.screenWidth = width;
        ArrayList<ObjectWrapper> objects = renderer.getObjects();
        boolean pointToFav = renderer.pointToFav;
        for (int i = 0; i < objects.size(); i++) {
            ObjectWrapper obj = objects.get(i);

            if ((!obj.visible || obj.filtered) && !((obj.favorite || obj.selected) && pointToFav)) continue;
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
            float posx = (float)obj.projection.getX();
            float posy = (float)obj.projection.getY();
            if (pointToFav && (obj.favorite || obj.selected)
                    && (posx > width + radius || posy > height + radius
                    || posx < -radius || posy < -radius || obj.rotatedPosition.getZ() > 0)) {
                Paint linePaint = densePaint;
                Vector2D rel = obj.projection.add(new Vector2D(-width / 2.0, -height / 2.0));
                if (rel.getNorm() == 0) continue;
                Vector2D inc = rel.normalize();
                float xStart = obj.rotatedPosition.getZ() > 0 ? (posx > width / 2.0 ? width : 0) : FastMath.max(FastMath.min(posx, width), 0);
                float yStart = obj.rotatedPosition.getZ() > 0 ? (posy > height / 2.0 ? height : 0) : FastMath.max(FastMath.min(posy, height), 0);
                float xEnd = (float)(xStart - inc.getX() * 50f);
                float yEnd = (float)(yStart - inc.getY() * 50f);
                canvas.drawLine(xStart, yStart, xEnd, yEnd, linePaint);
                continue;
            }
            if (!obj.visible || obj.filtered) continue;;
            canvas.drawCircle(posx, posy, radius, paint);
            if (obj.selected) {
                Paint roundPaint = new Paint(paint);
                roundPaint.setStyle(Paint.Style.STROKE);
                roundPaint.setStrokeWidth(4);
                canvas.drawCircle(posx, posy, radius+6, roundPaint);
            }
            if (obj.favorite) {
                Paint linePaint = purplePaint;
                canvas.drawLine(posx - radius, posy - radius, posx + radius, posy + radius, linePaint);
                canvas.drawLine(posx - radius, posy + radius, posx + radius, posy - radius, linePaint);

            }

        }
        LinkedList<OrbitWrapper> orbits = renderer.getOrbits();
        for (OrbitWrapper orbit : orbits) {
            if (!orbit.initialized || !orbit.rendered) continue;
            Paint paint = bluePaint;
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
