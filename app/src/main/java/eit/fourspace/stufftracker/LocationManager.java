package eit.fourspace.stufftracker;

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

    private DataManager manager;

    private FusedLocationProviderClient locationClient;
    private HandlerThread handlerThread;
    private LocationRequest request = new LocationRequest();
    private LocationCallback callback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult == null) {
                return;
            }
            Location location = locationResult.getLastLocation();
            Log.w(TAG, location.toString());
        }
    };
    LocationManager(Context context, DataManager manager) {
        locationClient = LocationServices.getFusedLocationProviderClient(context);
        handlerThread = new HandlerThread("LocationUpdateHandler", 5);
        this.manager = manager;
    }

    void onResume() {
        locationClient.requestLocationUpdates(request, callback, handlerThread.getLooper());
    }
    void onPause() {
        locationClient.removeLocationUpdates(callback);
    }


}
