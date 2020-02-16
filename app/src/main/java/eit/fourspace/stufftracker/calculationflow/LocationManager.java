package eit.fourspace.stufftracker.calculationflow;

import android.content.Context;
import android.location.Location;
import android.os.HandlerThread;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

public class LocationManager {
    private static final String TAG = "LocationManager";

    private FusedLocationProviderClient locationClient;
    private HandlerThread handlerThread;
    private LocationRequest request = new LocationRequest();
    private final double[] locationVector = new double[3];

    private boolean changed = false;

    private final LocationManager self = this;
    private LocationCallback callback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult == null) {
                return;
            }
            Location location = locationResult.getLastLocation();
            synchronized (self) {
                locationVector[0] = location.getLatitude();
                locationVector[1] = location.getLongitude();
                locationVector[2] = location.getAltitude();
            }
            changed = true;

            Log.w(TAG, location.toString());
        }
    };
    public LocationManager(Context context) {
        locationClient = LocationServices.getFusedLocationProviderClient(context);
        handlerThread = new HandlerThread("LocationUpdateHandler", 5);
    }

    public void onResume() {
        locationClient.requestLocationUpdates(request, callback, handlerThread.getLooper());
    }
    public void onPause() {
        locationClient.removeLocationUpdates(callback);
    }
    synchronized void getLocation(double[] loc) {
        if (!changed) return;
        changed = false;
        System.arraycopy(locationVector, 0, loc, 0, 3);
    }
}
