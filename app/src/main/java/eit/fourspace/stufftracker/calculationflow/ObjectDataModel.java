package eit.fourspace.stufftracker.calculationflow;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class ObjectDataModel extends AndroidViewModel {
    private final MutableLiveData<DataManager> dataManager = new MutableLiveData<>();
    private final MutableLiveData<Boolean> ready = new MutableLiveData<>();
    private final MutableLiveData<Double> elevation = new MutableLiveData<>();
    private final MutableLiveData<Double> azimuth = new MutableLiveData<>();

    public ObjectDataModel(Application context) {
        super(context);
        ready.setValue(null);
        elevation.setValue(null);
        azimuth.setValue(null);
    }
    public LiveData<DataManager> getDataManager() {
        return dataManager;
    }
    public LiveData<Boolean> getReady() {
        return ready;
    }
    public LiveData<Double> getElevation() { return elevation; }
    public LiveData<Double> getAzimuth() { return azimuth; }
    public void setDataManager(DataManager manager) {
        dataManager.setValue(manager);
    }
    public void setReady(boolean nReady) {
        ready.postValue(nReady);
    }
    public void setElevation(double nElevation) { elevation.postValue(nElevation); }
    public void setAzimuth(double nAzimuth) { azimuth.postValue(nAzimuth); }
}
