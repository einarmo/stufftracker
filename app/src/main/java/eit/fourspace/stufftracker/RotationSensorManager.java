package eit.fourspace.stufftracker;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class RotationSensorManager implements SensorEventListener {
    private final float[] gravityReading = new float[3];
    private final float[] magnetometerReading = new float[3];

    SensorManager sensorManager;

    public void onPause() {
        sensorManager.unregisterListener(this);
    }

    public void onResume() {
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
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            synchronized (this) {
                System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.length);
            }
        }
        // Log.println(Log.WARN, TAG, "Receive sensor event");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public synchronized void getRotationMatrix(float[] target) {
        if (target.length != 9) throw new IllegalArgumentException();

        android.hardware.SensorManager.getRotationMatrix(target, null, gravityReading, magnetometerReading);
    }
}
