package eit.fourspace.stufftracker.config;

import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.AnimationDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;

import java.util.Locale;

import androidx.lifecycle.Observer;
import eit.fourspace.stufftracker.R;

public class ConfigViewManager {
    private ConfigData dataModel;
    private LinearLayout container;
    private Switch showAll, trueNorth, showSatellites, showRocketBodies, showDebris;
    private EditText cameraRatio, filterString;
    public ConfigViewManager(ConfigData dataModel, LinearLayout container) {
        this.dataModel = dataModel;
        this.container = container;

        showAll = container.findViewById(R.id.show_all);
        trueNorth = container.findViewById(R.id.true_north);
        cameraRatio = container.findViewById(R.id.camera_ratio);
        showSatellites = container.findViewById(R.id.show_satellite);
        showRocketBodies = container.findViewById(R.id.show_rocket_body);
        showDebris = container.findViewById(R.id.show_debris);
        filterString = container.findViewById(R.id.filter_text);

        showAll.setOnCheckedChangeListener((view, active) -> {
            if (dataModel != null) {
                dataModel.setShowAll(active);
            }
        });
        trueNorth.setOnCheckedChangeListener((view, active) -> {
            if (dataModel != null) {
                dataModel.setTrueNorth(active);
            }
        });
        showSatellites.setOnCheckedChangeListener((view, active) -> {
            if (dataModel != null) {
                dataModel.setShowSatelite(active);
            }
        });
        showRocketBodies.setOnCheckedChangeListener((view, active) -> {
            if (dataModel != null) {
                dataModel.setShowRocketBody(active);
            }
        });
        showDebris.setOnCheckedChangeListener((view, active) -> {
            if (dataModel != null) {
                dataModel.setShowDebris(active);
            }
        });
        cameraRatio.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    double ratio = Double.valueOf(s.toString());
                    if (ratio > 0) {
                        dataModel.setCameraRatio(ratio);
                    }
                }
                catch (NumberFormatException ignore) {
                }
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
        filterString.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                dataModel.setFilter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        dataModel.getTrueNorth().observeForever(new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean == null) return;
                trueNorth.setChecked(aBoolean);
                dataModel.getTrueNorth().removeObserver(this);
            }
        });
        dataModel.getShowAll().observeForever(new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean == null) return;
                showAll.setChecked(aBoolean);
                dataModel.getShowAll().removeObserver(this);
            }
        });
        dataModel.getCameraRatio().observeForever(new Observer<Double>() {
            @Override
            public void onChanged(Double aDouble) {
                if (aDouble == null) return;
                cameraRatio.setText(String.format(Locale.ENGLISH,"%.2f", aDouble));
                dataModel.getCameraRatio().removeObserver(this);
            }
        });

        ImageView animTest = container.findViewById(R.id.image_test);
        animTest.setBackgroundResource(R.drawable.ic_icon_loading_animated);
        ((AnimatedVectorDrawable)animTest.getBackground()).start();
    }
}
