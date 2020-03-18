package eit.fourspace.stufftracker.calculationflow;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import androidx.lifecycle.Observer;
import eit.fourspace.stufftracker.config.ConfigData;

public class DataManager {
    private static final String TAG = "DataManager";
    private JSONObject rawData = null;
    public static final int TLE_DATA_NOT_AVAILABLE = 10;
    public static final int TLE_DATA_READY = 11;
    private static final double TLE_SIM_STEP = 0.2;
    private static final int TLE_SIM_COUNT = (int)Math.round(60/TLE_SIM_STEP);
    private int tleItCount = 0;

    private TLEPropagator[] propagators;
    private PVCoordinates[] currentCoordinates;
    private Vector3D[] currentVectors;
    private ConfigData config;
    private Handler asyncMessageHandler;
    ArrayList<ObjectWrapper> objects = new ArrayList<>();

    // Reference frame used by TLEs according to celestrak
    private Frame TEME;
    // International Terrestrial Reference Frame. Apparently extremely accurate.
    Frame ITRF;

    boolean initialized;

    public DataManager(Context context, Handler asyncMessageHandler, ConfigData config) {
        if (context == null) {
            Log.e(TAG, "Context may not be null");
            return;
        }
        this.asyncMessageHandler = asyncMessageHandler;
        this.config = config;
        AsyncTask.execute(() -> initTLEData(context));
    }

    private void initTLEData(Context context) {
        String res = readJsonFromFile(context);
        if (res != null && !res.equals("")) {
            try {
                rawData = new JSONObject(res);
            } catch (JSONException ex) {
                Log.e(TAG, "Unable to parse json data");
            }
        }
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(() -> {
            config.getFileNo().observeForever(
                    new Observer<Integer>() {
                        @Override
                        public void onChanged(Integer integer) {
                            if (integer == null) return;
                            config.getFileNo().removeObserver(this);
                            AsyncTask.execute(() -> finalizeTLELoad(context, integer));
                        }
                    }
            );
        });
    }
    private void finalizeTLELoad(Context context, int fileNo) {
        JSONObject newObjects = retrieveTLEByFileNo(context, fileNo);
        if (newObjects == null && rawData == null) {
            Message msg = asyncMessageHandler.obtainMessage(TLE_DATA_NOT_AVAILABLE);
            msg.sendToTarget();
            return;
        } else if (rawData == null) {
            rawData = new JSONObject();
            Iterator<String> keys = newObjects.keys();
            int maxFileNo = 0;
            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject newObj;
                try {
                    newObj = newObjects.getJSONObject(key);
                    boolean decayed = false;
                    try {
                        decayed = newObj.getInt("DECAYED") == 1;
                    } catch (JSONException ignore) {}

                    if (!decayed) {
                        rawData.put(key, newObj);
                    }
                    int lFileNo = newObj.getInt("FILE");
                    if (lFileNo > maxFileNo) {
                        maxFileNo = lFileNo;
                    }
                } catch (JSONException ex) {
                    Log.w(TAG, "Failed to parse TLE data: " + key + ", " + ex.getMessage());
                }
            }
        } else if (newObjects != null){
            Iterator<String> keys = newObjects.keys();
            int maxFileNo = 0;
            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject newObj;
                try {
                    newObj = newObjects.getJSONObject(key);
                    boolean decayed = false;
                    try {
                        decayed = newObj.getInt("DECAYED") == 1;
                    } catch (JSONException ignore) {}
                    if (decayed) {
                        rawData.remove(key);
                    } else {
                        rawData.put(key, newObj);
                    }
                    int lFileNo = newObj.getInt("FILE");
                    if (lFileNo > maxFileNo) {
                        maxFileNo = lFileNo;
                    }
                } catch (JSONException ignore) {
                    Log.w(TAG, "Failed to parse TLE data: " + key);
                }
            }
            config.setFileNo(maxFileNo);
        }
        AsyncTask.execute(() -> dumpToFile(rawData.toString(), context));
        Log.w(TAG, "Begin TLE Init");
        initTLEs();
        Log.w(TAG, "TLE data initialized");
        Message msg = asyncMessageHandler.obtainMessage(TLE_DATA_READY);
        msg.sendToTarget();
        initialized = true;
    }

    private JSONObject retrieveTLEByFileNo(Context context, int fileNo) {
        RequestQueue queue = Volley.newRequestQueue(context);
        String url = "https://folk.ntnu.no/dinaro/sat/?FILE=" + fileNo;
        RequestFuture<JSONObject> future = RequestFuture.newFuture();
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, new JSONObject(), future, future);
        queue.add(request);
        Log.w(TAG, "Retrieve data for file number: " + fileNo);
        try {
            return future.get(30, TimeUnit.SECONDS);
        }
        catch (InterruptedException | ExecutionException | TimeoutException ex) {
            Log.e(TAG, ex.getMessage() + ": failed to fetch data");
        }
        return null;
    }

    private void dumpToFile(String json, Context context) {
        try {
            OutputStreamWriter writer = new OutputStreamWriter(context.openFileOutput("tle_data.json", Context.MODE_PRIVATE));
            writer.write(json);
            writer.flush();
            writer.close();
        }
        catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage() + ": json file not found");
        }
        catch (IOException e) {
            Log.e(TAG, e.getMessage() + ": failed to write to json file");
        }
        Log.w(TAG, "Successfully written json data");
    }

    private String readJsonFromFile(Context context) {
        String ret = "";

        try {
            InputStream inputStream = context.openFileInput("tle_data.json");

            if ( inputStream != null ) {
                InputStreamReader input = new InputStreamReader(inputStream);
                BufferedReader reader = new BufferedReader(input);
                String receiveString;
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = reader.readLine()) != null ) {
                    stringBuilder.append("\n").append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage() + ": json file not found");
            return null;
        }
        catch (IOException e) {
            Log.e(TAG, e.getMessage() + ": failed to read from json file");
            return null;
        }
        return ret;
    }

    private void initTLEs() {
        ArrayList<TLE> parsedTLEs = new ArrayList<>();
        Iterator<String> keys = rawData.keys();

        int count = 0;
        while (keys.hasNext()) {
            String key = keys.next();
            try {
                JSONObject obj = rawData.getJSONObject(key);
                TLE newTLE = new TLE((String)obj.get("TLE_LINE1"), (String)obj.get("TLE_LINE2"));
                if (FastMath.abs(newTLE.getE()) < 1.0) {
                    parsedTLEs.add(newTLE);
                    String name, type, id;
                    try {
                        name = obj.getString("OBJECT_NAME");
                    } catch (JSONException ignore) {
                        name = "MISSING";
                    }
                    try {
                        type = obj.getString("OBJECT_TYPE");
                    } catch (JSONException ignore) {
                        type = "UNKNOWN";
                    }
                    try {
                        id = obj.getString("OBJECT_ID");
                    } catch (JSONException ignore) {
                        id = "";
                    }
                    objects.add(new ObjectWrapper(name, type, id));
                    //objects.add(new ObjectWrapper("DUMMY", "PAYLOAD", "1959-007A"));
                } else {
                    Log.w(TAG, "Ecc: " + newTLE.getE() + ", " + key);
                }
            }
            catch (Exception ex) {
                Log.w(TAG, "Failed to parse TLE data " + key + ", " + ex.getMessage());
                count++;
            }
        }
        Log.w(TAG, "Failed to parse TLE for " + count + " elements");

        propagators = new TLEPropagator[parsedTLEs.size()];
        currentCoordinates = new PVCoordinates[parsedTLEs.size()];
        currentVectors = new Vector3D[parsedTLEs.size()];
        Log.w(TAG, "Load propogators for " + parsedTLEs.size() + " TLEs");
        for (int i = 0; i < parsedTLEs.size(); i++) {
            propagators[i] = TLEPropagator.selectExtrapolator(parsedTLEs.get(i));
            currentCoordinates[i] = new PVCoordinates();
            currentVectors[i] = new Vector3D(0, 0, 0);
        }
        ITRF = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        TEME = FramesFactory.getTEME();
    }
    void propagateTLEs() {
        if (tleItCount++ % TLE_SIM_COUNT == 0) {
            AbsoluteDate current = new AbsoluteDate(new Date(), TimeScalesFactory.getUTC());
            Transform tf = TEME.getTransformTo(ITRF, current);
            for (int i = 0; i < propagators.length; i++) {
                if (objects.get(i).filtered || objects.get(i).invalid) continue;
                try {
                    currentCoordinates[i] = tf.transformPVCoordinates(propagators[i].getPVCoordinates(current));
                } catch (Exception ex) {
                    Log.w(TAG, "Failed to propogate: " + i);
                    objects.get(i).invalid = true;
                }
            }
        } else {
            for (int i = 0; i < currentCoordinates.length; i++) {
                if (objects.get(i).filtered) continue;
                currentCoordinates[i] = currentCoordinates[i].shiftedBy(TLE_SIM_STEP);
            }
            // 60 seconds accumulates about ~8km error. Orbital velocity is about the same, so it shouldn't be any more noticeable.
            // Time shifting is also 4-5 times faster, meaning that while we should still limit ourselves to <= 10/s, we can safely turn up the rate quite a bit.
            // How fast this goes should be adjusted based on how much movement this actually corresponds to
            // Some quick maths: at 150km, 10km/s, one second corresponds to about 3.6 degrees rotation, or a movement of ~3cm at arms length, which is significant.
            // More than 1cm might be noticeable, so at least 5 per second is likely to be needed, ideally even more. (wow satellites are fast!) This is an upper estimate
            // (in reality we're looking at 200km 8km/s, more likely, which gives only 0.6 degrees per second, or .5cm at arms length, 2-3 per second should give smooth movement in that case).
            /* PVCoordinates testIt = propagators[0].getPVCoordinates(current);
            Log.w(TAG, testIt.getPosition().toString());
            Log.w(TAG, currentCoordinates[0].getPosition().toString()); */
        }
    }

    void initializeOrbit(OrbitWrapper wrapper) {
        int index;
        for (index = 0; index < objects.size(); index++) {
            if (wrapper.obj == objects.get(index)) break;
        }
        TLEPropagator prop = propagators[index];
        AbsoluteDate current = new AbsoluteDate(new Date(), TimeScalesFactory.getUTC());
        double period = prop.propagate(current).getKeplerianPeriod();
        Transform tf = TEME.getTransformTo(ITRF, current);
        for (int i = 0; i < OrbitWrapper.NUM_POINTS; i++) {
            try {
                wrapper.positions[i] = tf.transformPVCoordinates(prop.getPVCoordinates(current)).getPosition();
            } catch (Exception ignore) {
                wrapper.positions[i] = new Vector3D(0, 0, 0);
            }
            current = current.shiftedBy(period / OrbitWrapper.NUM_POINTS);
        }
    }

    synchronized void refreshPositions() {

        for (int i = 0; i < currentCoordinates.length; i++) {
            currentVectors[i] = currentCoordinates[i].getPosition();
        }
    }
    synchronized Vector3D[] getPositions() {
        return currentVectors.clone();
    }
    void resetIteratorCount() {
        tleItCount = 0;
    }
}
