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

    private Handler asyncMessageHandler;

    public ObjectDataModel(Application context) {
        super(context);
        ready.setValue(null);
        asyncMessageHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message message) {
                switch(message.what) {
                    case DataManager.TLE_DATA_NOT_AVAILABLE:
                        Toast.makeText(context, "Unable to retrieve TLE data", Toast.LENGTH_LONG).show();
                        ready.postValue(false);
                        break;
                    case DataManager.TLE_DATA_READY:
                        Toast.makeText(context, "TLE data ready to use", Toast.LENGTH_LONG).show();
                        ready.postValue(true);
                        break;
                    default:
                        super.handleMessage(message);
                }
            }
        };
        dataManager.setValue(new DataManager(context, asyncMessageHandler));
    }
    public LiveData<DataManager> getDataManager() {
        return dataManager;
    }
    public LiveData<Boolean> getReady() {
        return ready;
    }
}
