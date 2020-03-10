package eit.fourspace.stufftracker.config;

import android.widget.LinearLayout;
import android.widget.Switch;

import eit.fourspace.stufftracker.R;

public class ConfigViewManager {
    private ConfigDataModel dataModel;
    private LinearLayout container;
    public ConfigViewManager(ConfigDataModel dataModel, LinearLayout container) {
        this.dataModel = dataModel;
        this.container = container;

        ConfigData data = dataModel.getDataManager().getValue();

        ((Switch)container.findViewById(R.id.show_all)).setOnCheckedChangeListener((view, active) -> {
            if (data != null) {
                data.setShowAll(active);
            }
        });
        ((Switch)container.findViewById(R.id.true_north)).setOnCheckedChangeListener((view, active) -> {
            if (data != null) {
                data.setTrueNorth(active);
            }
        });
        if (data != null) {
            ((Switch)container.findViewById(R.id.show_all)).setChecked(data.getShowAll());
            ((Switch)container.findViewById(R.id.true_north)).setChecked(data.getTrueNorth());
        }
    }
}
