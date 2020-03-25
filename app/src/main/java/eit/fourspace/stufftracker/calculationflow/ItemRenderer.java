package eit.fourspace.stufftracker.calculationflow;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;
import android.view.View;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.euclidean.twod.Vector2D;
import org.hipparchus.linear.Array2DRowRealMatrix;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;
import java.util.LinkedList;

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

                obj.projection = project(obj.rotatedPosition, cameraRatio);

            }

            LinkedList<OrbitWrapper> orbits = tleManager.getOrbits();

            for (int i = 0; i < orbits.size(); i++) {
                OrbitWrapper orbit = orbits.get(i);
                if (orbit.obj.filtered || !orbit.initialized) {
                    continue;
                }
                for (int j = 0; j < OrbitWrapper.NUM_POINTS; j++) {
                    orbit.rotatedPositions[j] = tf.transformPosition(orbit.transformedPositions[j]);
                    if (orbit.rotatedPositions[j].getZ() >= 0) {
                        orbit.projections[j] = new Vector2D(-10, -10);
                        continue;
                    }
                    orbit.projections[j] = project(orbit.rotatedPositions[j], cameraRatio);
                }
                orbit.rendered = true;
            }

            canvasView.invalidate();
        }
    };

    private Vector2D project(Vector3D pos, double cameraRatio) {
        double z0 = pos.getZ();
        double y0 = pos.getY();
        double x0 = pos.getX();

        switch (orientation) {
            case Surface.ROTATION_0:
                return new Vector2D((width/2 - x0/z0*scale)*scale2*cameraRatio + xshift, (height/2 + y0/z0*scale)*scale2*cameraRatio + yshift);
            case Surface.ROTATION_90:
                return new Vector2D((height/2 + y0/z0*scale)*scale2*cameraRatio + yshift, (width/2 + x0/z0*scale)*scale2*cameraRatio + xshift);
            case Surface.ROTATION_180:
                return new Vector2D((width/2 + x0/z0*scale)*scale2*cameraRatio + xshift, (height/2 - y0/z0*scale)*scale2*cameraRatio + yshift);
            case Surface.ROTATION_270:
                return new Vector2D((height/2 - y0/z0*scale)*scale2*cameraRatio + yshift, (width/2 - x0/z0*scale)*scale2*cameraRatio + xshift);
        }
        return new Vector2D(0, 0);
    }

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
    public LinkedList<OrbitWrapper> getOrbits() { return tleManager.getOrbits(); }
}
