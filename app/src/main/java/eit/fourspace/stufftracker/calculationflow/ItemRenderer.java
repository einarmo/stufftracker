package eit.fourspace.stufftracker.calculationflow;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.twod.Vector2D;
import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.linear.RealMatrix;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;

public class ItemRenderer {
    private TLEManager tleManager;
    private RotationSensorManager rotationManager;
    private View canvasView;

    private static final String TAG = "ItemRenderer";

    private Array2DRowRealMatrix rotationMatrix = new Array2DRowRealMatrix(3, 3);

    private Handler renderWorker;

    private boolean paused;

    private int cnt = 0;

    private Runnable renderRunnable = new Runnable() {
        @Override
        public void run() {
            if (!paused) {
                renderWorker.postDelayed(this, 17);
            }
            ArrayList<ObjectWrapper> objects = tleManager.getObjects();
            // TODO: Fetch rotation data, then do projection transform.
            // Depending on what we end up with, we may do time-based transformation here as well.
            // Might as well, really, not like doing one more transformation in the same matrix costs any more

            if (objects.size() == 0) return;

            rotationManager.getRotationMatrix(rotationMatrix);
            Transform tf = new Transform(AbsoluteDate.JAVA_EPOCH, new Rotation(rotationMatrix.getData(), 1e-4));

            for (int i = 0; i < objects.size(); i++) {
                ObjectWrapper obj = objects.get(i);
                if (!obj.visible) continue;
                obj.rotatedPosition = tf.transformPosition(obj.position);
                obj.visible = obj.rotatedPosition.getZ() < 0;
            }

            // Log.w(TAG, objects.get(0).rotatedPosition.toString() + ", " + objects.get(0).visible);


            /*objects.get(0).visible = true;
            objects.get(0).filtered = false;
            objects.get(0).projection = new Vector2D(cnt % 500, cnt % 500);
            cnt++;
            canvasView.invalidate();*/
        }
    };

    public ItemRenderer(Context context, TLEManager tleManager, RotationSensorManager rotationManager, View canvasView) {
        this.tleManager = tleManager;
        this.rotationManager = rotationManager;
        this.canvasView = canvasView;
        HandlerThread renderWorkerThread = new HandlerThread("RenderWorker", 2);
        renderWorkerThread.start();
        renderWorker = new Handler(renderWorkerThread.getLooper());
    }

    public void onPause() {
        paused = true;
        renderWorker.removeCallbacks(renderRunnable);
    }

    public void onResume() {
        paused = false;
        renderWorker.postDelayed(renderRunnable,1000);
    }

    public ArrayList<ObjectWrapper> getObjects() {
        return tleManager.getObjects();
    }
}
