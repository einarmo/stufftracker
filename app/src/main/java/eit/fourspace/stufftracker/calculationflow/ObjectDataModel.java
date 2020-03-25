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
    private final MutableLiveData<Double> velocity = new MutableLiveData<>();
    private final MutableLiveData<Double> altitude = new MutableLiveData<>();
    private final MutableLiveData<Double> eccentricity = new MutableLiveData<>();
    private final MutableLiveData<Double> latitude = new MutableLiveData<>();
    private final MutableLiveData<Double> longitude = new MutableLiveData<>();

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
    public LiveData<Double> getVelocity() { return velocity; }
    public LiveData<Double> getAltitude() { return altitude; }
    public LiveData<Double> getEccentricity() { return eccentricity; }
    public LiveData<Double> getLatitude() { return latitude; }
    public LiveData<Double> getLongitude() { return longitude; }

    public void setDataManager(DataManager manager) {
        dataManager.setValue(manager);
    }
    public void setReady(boolean nReady) {
        ready.postValue(nReady);
    }
    void setElevation(double nElevation) { elevation.postValue(nElevation); }
    void setAzimuth(double nAzimuth) { azimuth.postValue(nAzimuth); }
    void setVelocity(double nVelocity) { velocity.postValue(nVelocity); }
    void setAltitude(double nAltitude) { altitude.postValue(nAltitude); }
    void setEccentricity(double nEccentricity) { eccentricity.postValue(nEccentricity); }
    void setLatitude(double nLatitude) { latitude.postValue(nLatitude); }
    void setLongitude(double nLongitude) { longitude.postValue(nLongitude); }
}
