package eit.fourspace.stufftracker.config;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

import java.util.HashSet;
import java.util.Locale;

import androidx.lifecycle.Observer;
import eit.fourspace.stufftracker.R;
import eit.fourspace.stufftracker.calculationflow.DataManager;
import eit.fourspace.stufftracker.calculationflow.ObjectDataModel;

public class ConfigViewManager {
    private ToggleButton showAll, trueNorth, favPoint;
    private Button clearFavorites;
    private EditText cameraRatio;
    public ConfigViewManager(ConfigData dataModel, ObjectDataModel objectDataModel, LinearLayout container) {
        showAll = container.findViewById(R.id.show_all);
        trueNorth = container.findViewById(R.id.true_north);
        cameraRatio = container.findViewById(R.id.camera_ratio);
        ToggleButton showSatellites = container.findViewById(R.id.show_satellite);
        ToggleButton showRocketBodies = container.findViewById(R.id.show_rocket_body);
        ToggleButton showDebris = container.findViewById(R.id.show_debris);
        EditText filterString = container.findViewById(R.id.filter_text);
        clearFavorites = container.findViewById(R.id.clear_favorites);
        favPoint = container.findViewById(R.id.point_fav);

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
        favPoint.setOnCheckedChangeListener((view, active) -> {
            if (dataModel != null) {
                dataModel.setPointFav(active);
            }
        });
        clearFavorites.setOnClickListener(ignore -> {
            if (dataModel != null && objectDataModel != null) {
                HashSet<String> favorites = dataModel.getFavorites().getValue();
                DataManager dataManager = objectDataModel.getDataManager().getValue();
                if (favorites == null || dataManager == null) return;
                for (int i = 0; i < dataManager.objects.size(); i++) {
                    if (dataManager.objects.get(i).favorite) {
                        dataManager.objects.get(i).favorite = false;
                    }
                }
                dataModel.clearFavorites();
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
        dataModel.getFavorites().observeForever(set -> {
            if (set == null) return;
            clearFavorites.setEnabled(set.size() > 0);
        });
        dataModel.getPointFav().observeForever(new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean == null) return;
                favPoint.setChecked(aBoolean);
                dataModel.getPointFav().removeObserver(this);
            }
        });
    }
}
