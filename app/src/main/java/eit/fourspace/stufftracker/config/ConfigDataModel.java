package eit.fourspace.stufftracker.config;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class ConfigDataModel extends AndroidViewModel {
    private MutableLiveData<ConfigData> configData = new MutableLiveData<>();
    public ConfigDataModel(Application context) {
        super(context);
        ConfigData data = new ConfigData(context);
        data.Init();
        configData.setValue(data);
    }
    public LiveData<ConfigData> getDataManager() {
        return configData;
    }
}
