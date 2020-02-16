package eit.fourspace.stufftracker.calculationflow;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.View;

import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.linear.RealMatrix;

import java.util.ArrayList;

public class ItemRenderer {
    private TLEManager tleManager;
    private RotationSensorManager rotationManager;
    private View canvasView;

    private static final String TAG = "ItemRenderer";

    private RealMatrix rotationMatrix = new Array2DRowRealMatrix(4, 4);

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

            objects.get(0).visible = true;
            objects.get(0).filtered = false;
            // objects.get(0).projection = new Vector2D(cnt % 500, cnt % 500);
            cnt++;
            canvasView.invalidate();
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
