package eit.fourspace.stufftracker.calculationflow;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.util.ArrayList;

public class TLEManager {
    private boolean paused = false;
    private Handler tleWorker;

    private static final String TAG = "TLEManager";
    private DataManager dataManager;
    private LocationManager locManager;

    private Runnable tleRunnable = new Runnable() {
        @Override
        public void run() {
            if (!paused) {
                tleWorker.postDelayed(this, 1000);
            }
            if (dataManager.initialized) {
                dataManager.propagateTLEs();
                dataManager.refreshPositions();
                Log.w(TAG, dataManager.getPositions()[0].toString());
                // TODO: filter list, then use location data to do the necessary transformations.
            }
        }
    };

    public TLEManager(Context context, DataManager dataManager, LocationManager locManager) {
        this.dataManager = dataManager;
        this.locManager = locManager;
        HandlerThread tleWorkerThread = new HandlerThread("TLEWorker", 5);
        tleWorkerThread.start();
        tleWorker = new Handler(tleWorkerThread.getLooper());
    }

    public void onPause() {
        paused = true;
        tleWorker.removeCallbacks(tleRunnable);
    }

    public void onResume() {
        paused = false;
        tleWorker.postDelayed(tleRunnable,1000);
        dataManager.resetIteratorCount();
    }

    ArrayList<ObjectWrapper> getObjects() {
        return dataManager.objects;
    }
}
