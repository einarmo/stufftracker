package eit.fourspace.stufftracker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import eit.fourspace.stufftracker.calculationflow.ObjectDataModel;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;


public class MainActivity extends AppCompatActivity implements LifecycleOwner {
    private FrameLayout container;
    private ObjectDataModel dataModel;

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
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        container = findViewById(R.id.nav_container);

        dataModel = new ViewModelProvider(this).get(ObjectDataModel.class);
    }

    @Override
    protected void onResume() {
        super.onResume();
        container.postDelayed(() -> {
            container.setSystemUiVisibility(FLAGS_FULLSCREEN);
        }, 500);
    }

}
