package eit.fourspace.stufftracker;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.PVCoordinates;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DataManager {
    private static final String TAG = "DataManager";
    private JSONArray rawData;
    public static final int TLE_DATA_NOT_AVAILABLE = 10;
    public static final int TLE_DATA_READY = 11;

    private TLEPropagator[] propagators;
    private PVCoordinates[] currentCoordinates;
    private Vector3D[] currentVectors;

    boolean initialized;

    DataManager(Context context, Handler asyncMessageHandler) {
        if (context == null) {
            Log.e(TAG, "Context may not be null");
            return;
        }
        AsyncTask.execute(() -> {
            String res = readJsonFromFile(context);
            boolean getNew = false;
            if (res != null && !res.equals("")) {
                try {
                    rawData = new JSONArray(res);
                } catch (JSONException ex) {
                    Log.e(TAG, "Unable to parse json data");
                    getNew = true;
                }
            } else {
                getNew = true;
            }

            if (getNew) {
                JSONArray data = retrieveTLEData(context);
                if (data == null) {
                    Message msg = asyncMessageHandler.obtainMessage(TLE_DATA_NOT_AVAILABLE);
                    msg.sendToTarget();
                    return;
                }
                rawData = data;
                dumpToFile(data.toString(), context);
            }
            initTLEs();
            Message msg = asyncMessageHandler.obtainMessage(TLE_DATA_READY);
            msg.sendToTarget();
            initialized = true;
        });
    }

    private JSONArray retrieveTLEData(Context context) {
        RequestQueue queue = Volley.newRequestQueue(context);
        String url = "https://folk.ntnu.no/magnuhr/tleonly_all.json";

        RequestFuture<JSONArray> future = RequestFuture.newFuture();
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, new JSONArray(), future, future);
        queue.add(request);
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
            OutputStreamWriter writer = new OutputStreamWriter(context.openFileOutput("tle_all.json", Context.MODE_PRIVATE));
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
            InputStream inputStream = context.openFileInput("tle_all.json");

            if ( inputStream != null ) {
                InputStreamReader input = new InputStreamReader(inputStream);
                BufferedReader reader = new BufferedReader(input);
                String receiveString = "";
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
        Log.w(TAG, "Successfully read json data from file: " + ret);
        return ret;
    }

    private void initTLEs() {
        ArrayList<TLE> parsedTLEs = new ArrayList<>();
        for (int i = 0; i < rawData.length(); i++) {
            try {
                JSONObject obj = rawData.getJSONObject(i);
                parsedTLEs.add(new TLE((String)obj.get("TLE_LINE1"), (String)obj.get("TLE_LINE2")));
            }
            catch (Exception ex) {
                Log.w(TAG, "Failed to parse TLE data, line " + i);
            }
        }

        propagators = new TLEPropagator[parsedTLEs.size()];
        currentCoordinates = new PVCoordinates[parsedTLEs.size()];
        currentVectors = new Vector3D[parsedTLEs.size()];
        for (int i = 0; i < parsedTLEs.size(); i++) {
            try {
                JSONObject obj = rawData.getJSONObject(i);

                propagators[i] = TLEPropagator.selectExtrapolator(parsedTLEs.get(i));
                currentCoordinates[i] = new PVCoordinates();
                currentVectors[i] = new Vector3D(0, 0, 0);
                propagators[i].propagate(new AbsoluteDate(new Date(), TimeScalesFactory.getUTC()));
            }
            catch (JSONException ex) {
                Log.e(TAG, ex.getMessage() + ": failed to parse json TLE data");
            }
        }
    }
    void propogateTLEs() {
        AbsoluteDate current = new AbsoluteDate(new Date(), TimeScalesFactory.getUTC());
        for (int i = 0; i < propagators.length; i++) {
            currentCoordinates[i] = propagators[i].getPVCoordinates(current);
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
}
