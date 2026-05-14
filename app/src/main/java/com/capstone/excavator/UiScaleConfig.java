package com.capstone.excavator;

import android.content.Context;
import android.content.res.Configuration;
import android.util.DisplayMetrics;

final class UiScaleConfig {

    private static final float DESIGN_WIDTH_PX = 1920f;
    private static final float DESIGN_HEIGHT_PX = 1200f;
    private static final float TARGET_WIDTH_PX = 1800f;
    private static final float TARGET_HEIGHT_PX = 960f;

    /** 全局 UI 相对设计分辨率的缩放（当前为 0.8）。图传等子树可用 {@link #unscaledContext} 恢复物理 density。 */
    public static final float DP_SCALE = Math.min(
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

    /**
     * 在仍使用「已缩放」的 Activity 的前提下，为<strong>局部子树</strong>（如图传 Surface）提供
     * 近似<strong>物理屏幕 density</strong> 的 Context，用于 {@link android.view.LayoutInflater#from(Context)}，
     * 减轻 FPV 解码输出与 View 测量/合成链因全局 0.8 dpi 带来的额外缩放与掉帧。
     *
     * <p>仅影响使用该 Context inflate 出来的 View；其余界面仍走 {@link #wrap} 后的 Activity。
     */
    public static Context unscaledContext(Context scaledContext) {
        if (DP_SCALE == 1f) {
            return scaledContext;
        }
        Configuration config = new Configuration(scaledContext.getResources().getConfiguration());
        int scaledDpi = config.densityDpi;
        if (scaledDpi == Configuration.DENSITY_DPI_UNDEFINED) {
            scaledDpi = scaledContext.getResources().getDisplayMetrics().densityDpi;
        }
        int restored = Math.round(scaledDpi / DP_SCALE);
        if (restored < DisplayMetrics.DENSITY_LOW) {
            restored = scaledDpi;
        }
        config.densityDpi = restored;
        return scaledContext.createConfigurationContext(config);
    }
}
