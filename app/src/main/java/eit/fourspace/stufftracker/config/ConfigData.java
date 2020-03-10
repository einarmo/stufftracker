package eit.fourspace.stufftracker.config;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class ConfigData {
    public String filterString;
    private double cameraRatio;
    private boolean showAll;
    private Context context;
    private boolean trueNorth;
    private static final String TAG = "CONFIG_DATA";

    ConfigData(Context context) {
        this.context = context;
    }

    void Init() {
        AsyncTask.execute(() -> {
            JSONObject data;
            try {
                InputStream inputStream = context.openFileInput("config.json");
                if (inputStream != null) {
                    InputStreamReader input = new InputStreamReader(inputStream);
                    BufferedReader reader = new BufferedReader(input);
                    String receiveString;
                    StringBuilder stringBuilder = new StringBuilder();

                    while ((receiveString = reader.readLine()) != null) {
                        stringBuilder.append("\n").append(receiveString);
                    }

                    inputStream.close();
                    String ret = stringBuilder.toString();
                    try {
                        data = new JSONObject(ret);
                    } catch (JSONException e) {
                        data = null;
                    }
                } else {
                    data = null;
                }
            } catch (IOException e) {
                data = null;
            }
            if (data == null) {
                filterString = "";
                cameraRatio = 1;
                showAll = false;
                trueNorth = false;
                SaveData(context);
            } else {
                filterString = "";
                try {
                    cameraRatio = data.getDouble("cameraRatio");
                    showAll = data.getBoolean("showAll");
                    trueNorth = data.getBoolean("trueNorth");
                } catch (JSONException e) {
                    cameraRatio = 1;
                    showAll = false;
                }
            }
        });
    }
    private synchronized void SaveData(Context context) {
        JSONObject data = new JSONObject();
        try {
            data.put("cameraRatio", cameraRatio);
            data.put("showAll", showAll);
            data.put("trueNorth", trueNorth);
            OutputStreamWriter writer = new OutputStreamWriter(context.openFileOutput("config.json", Context.MODE_PRIVATE));
            writer.write(data.toString());
            writer.flush();
            writer.close();
        }
        catch (IOException e) {
            Log.e(TAG, "Unable to open config file", e);
        }
        catch (JSONException e) {
            Log.e(TAG, "Unable to write config data", e);
        }
    }

    public void setCameraRatio(double nRatio) {
        cameraRatio = nRatio;
        AsyncTask.execute(() -> SaveData(context));
    }
    public double getCameraRatio() {
        return cameraRatio;
    }
    public void setShowAll(boolean nShowAll) {
        showAll = nShowAll;
        AsyncTask.execute(() -> SaveData(context));
    }
    public boolean getShowAll() {
        return showAll;
    }
    public void setTrueNorth(boolean nTrueNorth) {
        trueNorth = nTrueNorth;
        AsyncTask.execute(() -> SaveData(context));
    }
    public boolean getTrueNorth() {
        return trueNorth;
    }
}
