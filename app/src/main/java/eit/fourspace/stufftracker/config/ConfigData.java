package eit.fourspace.stufftracker.config;

import android.app.Application;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashSet;

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
    private final MutableLiveData<Integer> fileNo = new MutableLiveData<>(null);
    private final MutableLiveData<HashSet<String>> favorites = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> favPoint = new MutableLiveData<>(null);
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
                fileNo.postValue(0);
                ready.postValue(true);
                SaveData(context, 1.0, false, true, 0, true);
            } else {
                filterString.postValue("");
                try {
                    double lCameraRatio = data.getDouble("cameraRatio");
                    cameraRatio.postValue(lCameraRatio);
                } catch (JSONException ignore) {
                    cameraRatio.postValue(1.0);
                }
                try {
                    boolean lShowAll = data.getBoolean("showAll");
                    showAll.postValue(lShowAll);
                } catch (JSONException e) {
                    showAll.postValue(false);
                }
                try {
                    boolean lTrueNorth = data.getBoolean("trueNorth");
                    trueNorth.postValue(lTrueNorth);
                } catch (JSONException e) {
                    trueNorth.postValue(true);
                }
                try {
                    int lFileNo = data.getInt("fileNo");
                    fileNo.postValue(lFileNo);
                } catch (JSONException e) {
                    fileNo.postValue(0);
                }
                try {
                    boolean lPointFav = data.getBoolean("pointFav");
                    favPoint.postValue(lPointFav);
                } catch (JSONException e) {
                    favPoint.postValue(true);
                }
                try {
                    JSONArray lFavorites = data.getJSONArray("favorites");
                    HashSet<String> values = new HashSet<>();
                    for (int i = 0; i < lFavorites.length(); i++) {
                        String val = lFavorites.getString(i);
                        if (val != null && !val.equals("")) {
                            values.add(val);
                        }
                    }
                    favorites.postValue(values);
                    Log.w(TAG, "Loaded " + values.size() + " favorites");
                } catch (JSONException e) {
                    favorites.postValue(new HashSet<>());
                }
            }
            ready.postValue(true);
            Log.w(TAG, "Finish load config");
        });
    }
    private synchronized void SaveData(Context context, Double lCameraRatio, Boolean lShowAll, Boolean lTrueNorth, Integer lFileNo, Boolean lPointFav) {
        JSONObject data = new JSONObject();
        try {
            data.put("cameraRatio", lCameraRatio == null ? cameraRatio.getValue() : lCameraRatio);
            data.put("showAll", lShowAll == null ? showAll.getValue() : lShowAll);
            data.put("trueNorth", lTrueNorth == null ? trueNorth.getValue() : lTrueNorth);
            data.put("fileNo", lFileNo == null ? fileNo.getValue() : lFileNo);
            data.put("pointFav", lPointFav == null ? favPoint.getValue() : lPointFav);
            HashSet<String> lFavorites = favorites.getValue();
            data.put("favorites", lFavorites == null ? new JSONArray() : new JSONArray(lFavorites));
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
        AsyncTask.execute(() -> SaveData(context, nRatio, null ,null, null, null));
    }
    public LiveData<Double> getCameraRatio() {
        return cameraRatio;
    }
    void setShowAll(boolean nShowAll) {
        showAll.postValue(nShowAll);
        AsyncTask.execute(() -> SaveData(context, null, nShowAll, null, null, null));
    }
    public LiveData<Boolean> getShowAll() {
        return showAll;
    }
     void setTrueNorth(boolean nTrueNorth) {
        trueNorth.postValue(nTrueNorth);
        AsyncTask.execute(() -> SaveData(context, null, null, nTrueNorth, null, null));
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
    public void setFileNo(int nFileNo) {
        fileNo.postValue(nFileNo);
        Log.w(TAG, "Save file no: " + nFileNo);
        AsyncTask.execute(() -> SaveData(context, null, null, null, nFileNo, null));
    }
    public LiveData<Integer> getFileNo() {
        return fileNo;
    }
    public LiveData<HashSet<String>> getFavorites() { return favorites; }

    public void addFavorite(String nFavorite) {
        if (favorites.getValue() == null) return;
        if (favorites.getValue().add(nFavorite)) {
            favorites.postValue(favorites.getValue());
            AsyncTask.execute(() -> SaveData(context, null, null, null, null, null));
        }
    }

    public void removeFavorite(String nFavorite) {
        if (favorites.getValue() == null) return;
        if (favorites.getValue().remove(nFavorite)) {
            favorites.postValue(favorites.getValue());
            AsyncTask.execute(() -> SaveData(context, null, null, null, null, null));
        }
    }
    void clearFavorites() {
        if (favorites.getValue() == null || favorites.getValue().size() == 0) return;
        favorites.getValue().clear();
        favorites.postValue(favorites.getValue());
        AsyncTask.execute(() -> SaveData(context, null, null, null, null, null));
    }
    public LiveData<Boolean> getPointFav() { return favPoint; }
    void setPointFav(boolean nPointFav) {
        favPoint.postValue(nPointFav);
        AsyncTask.execute(() -> SaveData(context, null, null, null, null, nPointFav));
    }
}
