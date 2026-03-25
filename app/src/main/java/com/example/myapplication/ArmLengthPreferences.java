package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 大臂 / 小臂长度比例（与 Web 端 {@code state.lengths} 一致，1 为默认倍率），存于应用私有 SharedPreferences。
 */
public final class ArmLengthPreferences {
    private static final String PREFS_NAME = "excavator_app_prefs";
    private static final String KEY_BOOM = "arm_length_scale_boom";
    private static final String KEY_STICK = "arm_length_scale_stick";

    public static final float DEFAULT_SCALE = 1f;

    private ArmLengthPreferences() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static float getBoomScale(Context context) {
        return prefs(context).getFloat(KEY_BOOM, DEFAULT_SCALE);
    }

    public static float getStickScale(Context context) {
        return prefs(context).getFloat(KEY_STICK, DEFAULT_SCALE);
    }

    public static void save(Context context, float boomScale, float stickScale) {
        prefs(context).edit()
                .putFloat(KEY_BOOM, boomScale)
                .putFloat(KEY_STICK, stickScale)
                .apply();
    }
}
