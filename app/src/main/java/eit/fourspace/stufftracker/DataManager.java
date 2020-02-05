package eit.fourspace.stufftracker;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DataManager {
    private static final String TAG = "DataManager";
    private JSONArray rawData;
    public static final int TLE_DATA_NOT_AVAILABLE = 10;
    public static final int TLE_DATA_READY = 11;

    public DataManager(Context context, Handler asyncMessageHandler) {
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
            Message msg = asyncMessageHandler.obtainMessage(TLE_DATA_READY);
            msg.sendToTarget();
        });
    }

    private JSONArray retrieveTLEData(Context context) {
        RequestQueue queue = Volley.newRequestQueue(context);
        String url = "https://folk.ntnu.no/magnuhr/tle_test.json";

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
            OutputStreamWriter writer = new OutputStreamWriter(context.openFileOutput("tle.json", Context.MODE_PRIVATE));
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
            InputStream inputStream = context.openFileInput("tle.json");

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
}
