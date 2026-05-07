package com.capstone.excavator;

import android.content.Context;
import android.content.res.Configuration;
import android.util.DisplayMetrics;

final class UiScaleConfig {

    private static final float DESIGN_WIDTH_PX = 1920f;
    private static final float DESIGN_HEIGHT_PX = 1200f;
    private static final float TARGET_WIDTH_PX = 1800f;
    private static final float TARGET_HEIGHT_PX = 960f;

    static final float DP_SCALE = Math.min(
            TARGET_WIDTH_PX / DESIGN_WIDTH_PX,
            TARGET_HEIGHT_PX / DESIGN_HEIGHT_PX
    );

    private UiScaleConfig() {
    }

    static Context wrap(Context context) {
        if (DP_SCALE == 1f) {
            return context;
        }

        Configuration config = new Configuration(context.getResources().getConfiguration());
        int baseDensityDpi = config.densityDpi;
        if (baseDensityDpi == Configuration.DENSITY_DPI_UNDEFINED) {
            baseDensityDpi = context.getResources().getDisplayMetrics().densityDpi;
        }
        if (baseDensityDpi == DisplayMetrics.DENSITY_DEFAULT) {
            baseDensityDpi = Math.round(context.getResources().getDisplayMetrics().density * DisplayMetrics.DENSITY_DEFAULT);
        }

        config.densityDpi = Math.round(baseDensityDpi * DP_SCALE);
        return context.createConfigurationContext(config);
    }
}
