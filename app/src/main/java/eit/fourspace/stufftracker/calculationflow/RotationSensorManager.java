package eit.fourspace.stufftracker.calculationflow;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import org.hipparchus.linear.RealMatrix;

public class RotationSensorManager implements SensorEventListener {
    private static final int READING_COUNT = 32;
    private int currentReadingM = 0;
    private int currentReadingG = 0;

    private final float[][] gravityReading2 = new float[READING_COUNT][3];
    private final float[][] magnetometerReading2 = new float[READING_COUNT][3];

    private final float[] gravityReadingL = new float[3];
    private final float[] magnetometerReadingL = new float[3];

    private final float[] gravityReading = new float[3];
    private final float[] magnetometerReading = new float[3];
    private final float[] localMatrix = new float[9];
    private SensorManager sensorManager;

    private final float[] rotVectorReading = new float[4];
    private final float[] rotVectorReadingL = new float[4];

    private Butterworth filterMag = new Butterworth(new double[] {0.5069731667, -2.8782914219, 6.5626122971, -7.5141824113, 4.3225961268}, 3, 1.094980613e+05);
    private Butterworth filterGrav = new Butterworth(new double[] {0.5069731667, -2.8782914219, 6.5626122971, -7.5141824113, 4.3225961268}, 3, 1.094980613e+05);

    //private Butterworth filterMag = new Butterworth(new double[] { 0.6011158229, -3.3110475620, 7.3120812802, -8.0940554178, 4.4918309651 }, 3, 4.271694293e+05);
    //private Butterworth filterGrav = new Butterworth(new double[] { 0.6011158229, -3.3110475620, 7.3120812802, -8.0940554178, 4.4918309651 }, 3, 4.271694293e+05);

    //private Butterworth filterMag = new Butterworth(new double[] {0.8441170973, -4.3636080283, 9.0254524705, -9.3365274216, 4.8305655200}, 3, 8.840143939e+07);
    //private Butterworth filterGrav = new Butterworth(new double[] {0.8441170973, -4.3636080283, 9.0254524705, -9.3365274216, 4.8305655200}, 3, 8.840143939e+07);

    private boolean changedSinceLast = true;

    public void onPause() {
        sensorManager.unregisterListener(this);
    }

    public void onResume() {
        /*Sensor gravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        if (gravity == null) {
            gravity = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        if (gravity != null) {
            sensorManager.registerListener(this, gravity, SensorManager.SENSOR_DELAY_FASTEST, SensorManager.SENSOR_DELAY_FASTEST);
        }
        Sensor magnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (magnetic != null) {
            sensorManager.registerListener(this, magnetic, SensorManager.SENSOR_DELAY_FASTEST, SensorManager.SENSOR_DELAY_FASTEST);
        }*/
        Sensor rotVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if (rotVector != null) {
            sensorManager.registerListener(this, rotVector, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    public RotationSensorManager(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        /*if (event.sensor.getType() == Sensor.TYPE_GRAVITY || event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            synchronized (this) {
                System.arraycopy(event.values, 0, gravityReading, 0, 3);
                //System.arraycopy(event.values, 0, gravityReading2[++currentReadingG % READING_COUNT], 0, 3);
                //applyLowPassFilter(event.values.clone(), gravityReading2[++currentReadingG % READING_COUNT], 0.05f);
            }
            changedSinceLast = true;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            synchronized (this) {
                System.arraycopy(event.values, 0, magnetometerReading, 0, 3);
                //System.arraycopy(event.values, 0, magnetometerReading2[++currentReadingM % READING_COUNT], 0, 3);
                //applyLowPassFilter(event.values.clone(), magnetometerReading2[++currentReadingM % READING_COUNT], 0.05f);
            }
            changedSinceLast = true;
        } else if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {*/
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            synchronized (this) {
                System.arraycopy(event.values, 0, rotVectorReading, 0, 4);
            }
            changedSinceLast = true;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    synchronized void getRotationMatrix(RealMatrix res) {
        // if (!changedSinceLast) return;

        //applyAverageFiltering(gravityReading2, gravityReading);
        //applyAverageFiltering(magnetometerReading2, magnetometerReading);
        /*applyButterworthFilter(gravityReading, gravityReadingL, filterGrav);
        applyButterworthFilter(magnetometerReading, magnetometerReadingL, filterMag);

        android.hardware.SensorManager.getRotationMatrix(localMatrix, null, gravityReadingL, magnetometerReadingL);*/
        //applyButterworthFilter(rotVectorReading, rotVectorReadingL, filterGrav);
        //applyLowPassFilter(rotVectorReading, rotVectorReadingL, 0.05f);
        android.hardware.SensorManager.getRotationMatrixFromVector(localMatrix, rotVectorReading);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                // Intentionally insert them transposed
                res.setEntry(j, i, localMatrix[i*3 + j]);
            }
        }
        changedSinceLast = false;
    }

    private void applyButterworthFilter(float[] input, float[] output, Butterworth filter) {
        for (int i = 0; i < 3; i++) {
            output[i] = filter.iterate(i, input[i]);
        }
    }

    private void applyAverageFiltering(float[][] input, float[] output) {
        for (int i = 0; i < 3; i++) {
            output[i] = 0;
        }
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < READING_COUNT; j++) {
                output[i] += input[j][i];
            }
            output[i] /= READING_COUNT;
        }
    }

    private static final float ALPHA = 0.06f;

    private void applyLowPassFilter(float[] input, float[] output, float alpha) {
        if ( output == null ) return;

        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + alpha * (input[i] - output[i]);
        }
    }
    private class Butterworth {
        int n = 5;
        double[] gains;
        double[][] xv;
        double[][] yv;
        double gain;
        Butterworth(double[] gains, int dim, double gain) {
            this.gains = gains;
            xv = new double[dim][n+1];
            yv = new double[dim][n+1];
            this.gain = gain;
        }
        float iterate(int dim, float input) {
            xv[dim][0] = xv[dim][1]; xv[dim][1] = xv[dim][2]; xv[dim][2] = xv[dim][3]; xv[dim][3] = xv[dim][4]; xv[dim][4] = xv[dim][5];
            xv[dim][5] = input/gain;
            yv[dim][0] = yv[dim][1]; yv[dim][1] = yv[dim][2]; yv[dim][2] = yv[dim][3]; yv[dim][3] = yv[dim][4]; yv[dim][4] = yv[dim][5];
            yv[dim][5] =   (xv[dim][0] + xv[dim][5]) + 5 * (xv[dim][1] + xv[dim][4]) + 10 * (xv[dim][2] + xv[dim][3])
                    + (gains[0] * yv[dim][0]) + (gains[1] * yv[dim][1])
                    + (gains[2] * yv[dim][2]) + (gains[3] * yv[dim][3])
                    + (gains[4] * yv[dim][4]);
            return (float)yv[dim][5];
        }
    }
}
