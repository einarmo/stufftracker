package eit.fourspace.stufftracker.calculationflow;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.View;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.twod.Vector2D;
import org.hipparchus.linear.Array2DRowRealMatrix;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;

import eit.fourspace.stufftracker.config.ConfigData;

public class ItemRenderer {
    private TLEManager tleManager;
    private RotationSensorManager rotationManager;
    private View canvasView;

    private static final String TAG = "ItemRenderer";

    private Array2DRowRealMatrix rotationMatrix = new Array2DRowRealMatrix(3, 3);

    private Handler renderWorker;

    private ConfigData configDataModel;

    private boolean paused;

    public double width;
    public double height;

    public double physWidth;
    public double physHeight;
    public double physFocal;

    public int orientation;

    public double screenWidth;
    public double screenHeight;

    private double scale;
    private double scale2;
    private double xshift;
    private double yshift;

    private Runnable renderRunnable = new Runnable() {
        @Override
        public void run() {
            if (!paused) {
                renderWorker.postDelayed(this, 17);
            }
            ArrayList<ObjectWrapper> objects = tleManager.getObjects();
            // Depending on what we end up with, we may do time-based transformation here as well.
            // Might as well, really, not like doing one more transformation in the same matrix costs any more

            if (objects.size() == 0) return;


            double cameraRatio = 1;
            if (configDataModel != null && configDataModel.getCameraRatio().getValue() != null) {
                cameraRatio = configDataModel.getCameraRatio().getValue();
            }

            rotationManager.getRotationMatrix(rotationMatrix);
            Transform tf = new Transform(AbsoluteDate.JAVA_EPOCH, new Rotation(rotationMatrix.getData(), 1e-4));

            for (int i = 0; i < objects.size(); i++) {
                ObjectWrapper obj = objects.get(i);
                obj.visible = obj.baseVisible;
                if (!obj.baseVisible || obj.filtered) continue;
                obj.rotatedPosition = tf.transformPosition(obj.position);
                obj.visible = obj.rotatedPosition.getZ() < 0;
                if (!obj.visible || obj.filtered) continue;

                double z0 = obj.rotatedPosition.getZ();
                double y0 = obj.rotatedPosition.getY();
                double x0 = obj.rotatedPosition.getX();

                switch (orientation) {
                    case Surface.ROTATION_0:
                        obj.projection = new Vector2D((width/2 - x0/z0*scale)*scale2*cameraRatio + xshift, (height/2 + y0/z0*scale)*scale2*cameraRatio + yshift);
                        break;
                    case Surface.ROTATION_90:
                        obj.projection = new Vector2D((height/2 + y0/z0*scale)*scale2*cameraRatio + yshift, (width/2 + x0/z0*scale)*scale2*cameraRatio + xshift);
                        break;
                    case Surface.ROTATION_180:
                        obj.projection = new Vector2D((width/2 + x0/z0*scale)*scale2*cameraRatio + xshift, (height/2 - y0/z0*scale)*scale2*cameraRatio + yshift);
                        break;
                    case Surface.ROTATION_270:
                        obj.projection = new Vector2D((height/2 - y0/z0*scale)*scale2*cameraRatio + yshift, (width/2 - x0/z0*scale)*scale2*cameraRatio + xshift);
                        break;
                }
            }



            canvasView.invalidate();
        }
    };

    public ItemRenderer(Context context, TLEManager tleManager, RotationSensorManager rotationManager, View canvasView, ConfigData configDataModel) {
        this.tleManager = tleManager;
        this.rotationManager = rotationManager;
        this.canvasView = canvasView;
        HandlerThread renderWorkerThread = new HandlerThread("RenderWorker", 2);
        renderWorkerThread.start();
        renderWorker = new Handler(renderWorkerThread.getLooper());
        this.configDataModel = configDataModel;
    }

    public void onPause() {
        paused = true;
        renderWorker.removeCallbacks(renderRunnable);
    }

    public void onResume() {
        paused = false;
        renderWorker.postDelayed(renderRunnable,1000);
    }

    public void updateConstants() {
        scale = physFocal * width / physWidth;
        scale2 = screenHeight / height;
        xshift = - (scale2*width - screenWidth)/2;
        yshift = - (scale2*height - screenHeight)/2;
    }

    public ArrayList<ObjectWrapper> getObjects() {
        return tleManager.getObjects();
    }
}
