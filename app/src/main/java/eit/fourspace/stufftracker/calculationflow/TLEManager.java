package eit.fourspace.stufftracker.calculationflow;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.frames.Transform;
import org.orekit.models.earth.ReferenceEllipsoid;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class TLEManager {
    private boolean paused = false;
    private Handler tleWorker;

    private static final String TAG = "TLEManager";
    private DataManager dataManager;
    private LocationManager locManager;
    private final double[] locationVector = new double[3];

    private ReferenceEllipsoid earth = null;

    private Runnable tleRunnable = new Runnable() {
        @Override
        public void run() {
            if (!paused) {
                tleWorker.postDelayed(this, 1000);
            }
            if (dataManager.initialized) {
                Date start = new Date();
                if (earth == null) {
                    earth = ReferenceEllipsoid.getWgs84(dataManager.ITRF);
                }
                dataManager.propagateTLEs();
                dataManager.refreshPositions();
                Vector3D[] positions = dataManager.getPositions();

                ArrayList<ObjectWrapper> objects = dataManager.objects;

                locManager.getLocation(locationVector);
                GeodeticPoint location = new GeodeticPoint(FastMath.toRadians(locationVector[0]), FastMath.toRadians(locationVector[1]), locationVector[2]);
                TopocentricFrame localFrame = new TopocentricFrame(earth, location, "CurrentLocation");

                Transform tf = dataManager.ITRF.getTransformTo(localFrame, new AbsoluteDate(new Date(), TimeScalesFactory.getUTC()));

                for (int i = 0; i < positions.length; i++) {
                    ObjectWrapper obj = dataManager.objects.get(i);
                    obj.position = tf.transformPosition(positions[i]);
                    obj.visible = obj.position.getZ() > 0;
                    if (obj.position.getZ() > 0 && Math.abs(obj.position.getX()) < 100000 && Math.abs(obj.position.getY()) < 100000) {
                        Log.w(TAG, "OVERHEAD: " + obj.name + ", " + obj.position.toString());
                    }
                }

                Date end = new Date();
                Log.w(TAG, "TLE Calculations took " + TimeUnit.MILLISECONDS.convert(end.getTime() - start.getTime(), TimeUnit.MILLISECONDS) + " milliseconds");

                Log.w(TAG, dataManager.objects.get(0).position.toString() + ", " + dataManager.objects.get(0).visible + ", " + dataManager.objects.get(0).position.getZ());
                // TODO: filtering
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
