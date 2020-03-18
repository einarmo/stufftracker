package eit.fourspace.stufftracker.calculationflow;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class ObjectDataModel extends AndroidViewModel {
    private final MutableLiveData<DataManager> dataManager = new MutableLiveData<>();
    private final MutableLiveData<Boolean> ready = new MutableLiveData<>();

    public ObjectDataModel(Application context) {
        super(context);
        ready.setValue(null);

    }
    public LiveData<DataManager> getDataManager() {
        return dataManager;
    }
    public LiveData<Boolean> getReady() {
        return ready;
    }
    public void setDataManager(DataManager manager) {
        dataManager.setValue(manager);
    }
    public void setReady(boolean nReady) {
        ready.postValue(nReady);
    }
}
