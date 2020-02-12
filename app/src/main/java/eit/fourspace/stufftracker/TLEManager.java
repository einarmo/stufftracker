package eit.fourspace.stufftracker;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

public class TLEManager {
    private HandlerThread tleWorkerThread;
    private boolean paused = false;
    private Handler tleWorker;

    private static final String TAG = "TLEManager";
    DataManager manager;

    private Runnable tleRunnable = new Runnable() {
        @Override
        public void run() {
            if (!paused) {
                tleWorker.postDelayed(this, 1000);
            }
            if (manager.initialized) {
                manager.propogateTLEs();
                manager.refreshPositions();
                Log.w(TAG, manager.getPositions()[0].toString());
            }
        }
    };

    TLEManager(Context context, DataManager manager) {
        this.manager = manager;
        tleWorkerThread = new HandlerThread("TLEWorker", 5);
        tleWorkerThread.start();
        tleWorker = new Handler(tleWorkerThread.getLooper());
    }

    void onPause() {
        paused = true;
        tleWorker.removeCallbacks(tleRunnable);
    }

    void onResume() {
        paused = false;
        tleWorker.postDelayed(tleRunnable,1000);
    }
}
