package eit.fourspace.stufftracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import eit.fourspace.stufftracker.calculationflow.DataManager;
import eit.fourspace.stufftracker.calculationflow.ObjectDataModel;
import eit.fourspace.stufftracker.config.ConfigData;
import eit.fourspace.stufftracker.config.ConfigViewManager;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.FrameLayout;

import com.google.android.material.navigation.NavigationView;


public class MainActivity extends AppCompatActivity implements LifecycleOwner {
    private FrameLayout container;

    private static final int FLAGS_FULLSCREEN =
            View.SYSTEM_UI_FLAG_LOW_PROFILE
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        container = findViewById(R.id.nav_container);

        ViewModelProvider provider = new ViewModelProvider(this);

        ObjectDataModel dataModel = provider.get(ObjectDataModel.class);

        Handler asyncMessageHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message message) {
                switch(message.what) {
                    case DataManager.TLE_DATA_NOT_AVAILABLE:
                        dataModel.setReady(false);
                        break;
                    case DataManager.TLE_DATA_READY:
                        dataModel.setReady(true);
                        break;
                    default:
                        super.handleMessage(message);
                }
            }
        };

        ConfigData configDataModel = provider.get(ConfigData.class);
        dataModel.setDataManager(new DataManager(this, asyncMessageHandler, configDataModel));

        NavigationView navigationView = findViewById(R.id.nav_view);
        if (navigationView != null) {
            navigationView.setOnApplyWindowInsetsListener((v, insets) -> insets);
        }
        new ConfigViewManager(configDataModel, dataModel, findViewById(R.id.config_container));

        container.setOnSystemUiVisibilityChangeListener(visibility -> {
            if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                container.setSystemUiVisibility(FLAGS_FULLSCREEN);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        container.postDelayed(() -> container.setSystemUiVisibility(FLAGS_FULLSCREEN), 500);
    }
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            container.postDelayed(() -> container.setSystemUiVisibility(FLAGS_FULLSCREEN), 500);
        }
    }

}
