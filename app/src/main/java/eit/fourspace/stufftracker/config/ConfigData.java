package eit.fourspace.stufftracker.config;

import android.app.Application;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class ConfigData extends AndroidViewModel {
    private final MutableLiveData<String> filterString = new MutableLiveData<>("");
    private final MutableLiveData<Double> cameraRatio = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> showAll = new MutableLiveData<>(null);
    private Context context;
    private final MutableLiveData<Boolean> trueNorth = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> ready = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showSatellite = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> showRocketBody = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> showDebris = new MutableLiveData<>(true);
    private static final String TAG = "CONFIG_DATA";

    public ConfigData(Application context) {
        super(context);
        this.context = context;
        Init();
        Log.w(TAG, "Init config");
    }

    private void Init() {
        AsyncTask.execute(() -> {
            Log.w(TAG, "Begin load config");
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
                filterString.postValue("");
                cameraRatio.postValue(1.0);
                showAll.postValue(false);
                trueNorth.postValue(true);
                ready.postValue(true);
                SaveData(context, 1.0, false, true);
            } else {
                filterString.postValue("");
                try {
                    double lCameraRatio = data.getDouble("cameraRatio");
                    boolean lShowAll = data.getBoolean("showAll");
                    boolean lTrueNorth = data.getBoolean("trueNorth");
                    cameraRatio.postValue(lCameraRatio);
                    showAll.postValue(lShowAll);
                    trueNorth.postValue(lTrueNorth);
                    ready.postValue(true);
                    Log.w(TAG, "Read data: " + lCameraRatio + ", " + lShowAll + "; " + lTrueNorth);
                } catch (JSONException e) {
                    cameraRatio.postValue(1.0);
                    showAll.postValue(false);
                    trueNorth.postValue(true);
                }
            }
            ready.postValue(true);
            Log.w(TAG, "Finish load config");
        });
    }
    private synchronized void SaveData(Context context, Double lCameraRatio, Boolean lShowAll, Boolean lTrueNorth) {
        JSONObject data = new JSONObject();
        try {
            data.put("cameraRatio", lCameraRatio == null ? cameraRatio.getValue() : lCameraRatio);
            data.put("showAll", lShowAll == null ? showAll.getValue() : lShowAll);
            data.put("trueNorth", lTrueNorth == null ? trueNorth.getValue() : lTrueNorth);
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

    void setCameraRatio(double nRatio) {
        cameraRatio.postValue(nRatio);
        AsyncTask.execute(() -> SaveData(context, nRatio, null ,null));
    }
    public LiveData<Double> getCameraRatio() {
        return cameraRatio;
    }
    void setShowAll(boolean nShowAll) {
        showAll.postValue(nShowAll);
        AsyncTask.execute(() -> SaveData(context, null, nShowAll, null));
    }
    public LiveData<Boolean> getShowAll() {
        return showAll;
    }
     void setTrueNorth(boolean nTrueNorth) {
        trueNorth.postValue(nTrueNorth);
        AsyncTask.execute(() -> SaveData(context, null, null, nTrueNorth));
    }
    public LiveData<Boolean> getTrueNorth() {
        return trueNorth;
    }
     void setFilter(String filter) {
        filterString.postValue(filter);
    }
    public LiveData<String> getFilter() {
        return filterString;
    }
    public LiveData<Boolean> getReady() {
        return ready;
    }
    void setShowRocketBody(boolean nShowRocketBody) {
        showRocketBody.postValue(nShowRocketBody);
    }
    public LiveData<Boolean> getShowRocketBody() {
        return showRocketBody;
    }
    void setShowSatelite(boolean nShowSatellite) {
        showSatellite.postValue(nShowSatellite);
    }
    public LiveData<Boolean> getShowSatellite() {
        return showSatellite;
    }
    void setShowDebris(boolean nShowDebris) {
        showDebris.postValue(nShowDebris);
    }
    public LiveData<Boolean> getShowDebris() {
        return showDebris;
    }
}
