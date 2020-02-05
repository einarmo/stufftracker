package eit.fourspace.stufftracker;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;

import androidx.fragment.app.Fragment;

public class SensorHandler extends Fragment implements SensorEventListener {
    private SensorManager sensorManager;
    private final float[] gravityReading = new float[3];
    private final float[] magnetometerReading = new float[3];
    private static final String TAG = "SensorHandler";

    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Try to get gravity, use pure accelerometer as backup
        Sensor gravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        if (gravity == null) {
            gravity = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        if (gravity != null) {
            sensorManager.registerListener(this, gravity, SensorManager.SENSOR_DELAY_GAME, SensorManager.SENSOR_DELAY_UI);
        }
        Sensor magnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (magnetic != null) {
            sensorManager.registerListener(this, magnetic, SensorManager.SENSOR_DELAY_GAME, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        Log.println(Log.WARN, TAG, "Create sensor handler fragment");
    }
    private String printMatrix(float[] data) {
        String fin = "[";
        for (int i = 0; i < 3; i++) {
            fin += String.valueOf(data[i]) + (i == 2 ? "]" : ", ");
        }
        return fin;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GRAVITY || event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            synchronized (this) {
                System.arraycopy(event.values, 0, gravityReading, 0, gravityReading.length);
            }
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            synchronized (this) {
                System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.length);
            }
        }
        // Log.println(Log.WARN, TAG, printMatrix(event.values));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public synchronized void getRotationMatrix(float[] target) {
        if (target.length != 9) throw new IllegalArgumentException();

        SensorManager.getRotationMatrix(target, null, gravityReading, magnetometerReading);
    }
}
