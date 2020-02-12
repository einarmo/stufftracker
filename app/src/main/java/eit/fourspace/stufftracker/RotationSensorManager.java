package eit.fourspace.stufftracker;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import org.hipparchus.linear.RealMatrix;

public class RotationSensorManager implements SensorEventListener {
    private final float[] gravityReading = new float[3];
    private final float[] magnetometerReading = new float[3];
    private final float[] localMatrix = new float[9];
    private SensorManager sensorManager;

    private boolean changedSinceLast = true;

    void onPause() {
        sensorManager.unregisterListener(this);
    }

    void onResume() {
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

    RotationSensorManager(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GRAVITY || event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            synchronized (this) {
                System.arraycopy(event.values, 0, gravityReading, 0, gravityReading.length);
            }
            changedSinceLast = true;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            synchronized (this) {
                System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.length);
            }
            changedSinceLast = true;
        }
        // Log.println(Log.WARN, TAG, "Receive sensor event");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public synchronized void getRotationMatrix(RealMatrix res) {
        if (!changedSinceLast) return;
        android.hardware.SensorManager.getRotationMatrix(localMatrix, null, gravityReading, magnetometerReading);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                res.setEntry(i, j, localMatrix[i*3 + j]);
            }
        }
        changedSinceLast = false;
    }
}
