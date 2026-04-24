package com.capstone.excavator;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 尺寸信息页：所选机型与自定义几何参数（米 / 度）。
 */
public final class DimensionPreferences {

    private static final String PREFS_NAME = "dimension_prefs";

    public static final String MODEL_CAT_303 = "cat_303";
    public static final String MODEL_CAT_312 = "cat_312";
    public static final String MODEL_CAT_320 = "cat_320";
    public static final String MODEL_CAT_330 = "cat_330";
    public static final String MODEL_CAT_336 = "cat_336";

    /** 四连杆与驾驶舱默认值（与设置页设计稿一致）。 */
    public static final double D_LINK_L1 = 0.45;
    public static final double D_LINK_L2 = 0.38;
    public static final double D_LINK_L3 = 0.52;
    public static final double D_LINK_L4 = 0.41;
    public static final double D_LINK_ANGLE_DEG = 24.5;
    public static final double D_CHASSIS_H = 0.95;
    public static final double D_TRACK_W = 2.4;
    public static final double D_CABIN_H = 2.8;

    public static final class Params {
        public String selectedModel = MODEL_CAT_303;
        public double boomM = 2.9;
        public double stickM = 1.5;
        public double linkL1 = D_LINK_L1;
        public double linkL2 = D_LINK_L2;
        public double linkL3 = D_LINK_L3;
        public double linkL4 = D_LINK_L4;
        public double linkAngleDeg = D_LINK_ANGLE_DEG;
        public double chassisHeightM = D_CHASSIS_H;
        public double trackWidthM = D_TRACK_W;
        public double cabinHeightM = D_CABIN_H;
    }

    private DimensionPreferences() {}

    public static Params load(Context context) {
        SharedPreferences sp = prefs(context);
        Params p = new Params();
        p.selectedModel = sp.getString("selected_model", MODEL_CAT_303);
        p.boomM = getDouble(sp, "boom_m", presetBoomM(p.selectedModel));
        p.stickM = getDouble(sp, "stick_m", presetStickM(p.selectedModel));
        p.linkL1 = getDouble(sp, "link_l1", D_LINK_L1);
        p.linkL2 = getDouble(sp, "link_l2", D_LINK_L2);
        p.linkL3 = getDouble(sp, "link_l3", D_LINK_L3);
        p.linkL4 = getDouble(sp, "link_l4", D_LINK_L4);
        p.linkAngleDeg = getDouble(sp, "link_angle_deg", D_LINK_ANGLE_DEG);
        p.chassisHeightM = getDouble(sp, "chassis_h_m", D_CHASSIS_H);
        p.trackWidthM = getDouble(sp, "track_w_m", D_TRACK_W);
        p.cabinHeightM = getDouble(sp, "cabin_h_m", D_CABIN_H);
        return p;
    }

    public static void save(Context context, Params p) {
        SharedPreferences.Editor e = prefs(context).edit();
        e.putString("selected_model", p.selectedModel == null ? MODEL_CAT_303 : p.selectedModel);
        putDouble(e, "boom_m", p.boomM);
        putDouble(e, "stick_m", p.stickM);
        putDouble(e, "link_l1", p.linkL1);
        putDouble(e, "link_l2", p.linkL2);
        putDouble(e, "link_l3", p.linkL3);
        putDouble(e, "link_l4", p.linkL4);
        putDouble(e, "link_angle_deg", p.linkAngleDeg);
        putDouble(e, "chassis_h_m", p.chassisHeightM);
        putDouble(e, "track_w_m", p.trackWidthM);
        putDouble(e, "cabin_h_m", p.cabinHeightM);
        e.apply();
    }

    /** 机型卡片上的大臂 / 小臂参考长度（米）。 */
    public static double presetBoomM(String modelId) {
        if (modelId == null) return 2.9;
        switch (modelId) {
            case MODEL_CAT_303: return 2.9;
            case MODEL_CAT_312: return 4.8;
            case MODEL_CAT_320: return 5.8;
            case MODEL_CAT_330: return 6.4;
            case MODEL_CAT_336: return 6.8;
            default: return 2.9;
        }
    }

    public static double presetStickM(String modelId) {
        if (modelId == null) return 1.5;
        switch (modelId) {
            case MODEL_CAT_303: return 1.5;
            case MODEL_CAT_312: return 2.4;
            case MODEL_CAT_320: return 2.9;
            case MODEL_CAT_330: return 3.2;
            case MODEL_CAT_336: return 3.4;
            default: return 1.5;
        }
    }

    public static void applyModelPreset(Context context, String modelId) {
        Params p = load(context);
        p.selectedModel = modelId;
        p.boomM = presetBoomM(modelId);
        p.stickM = presetStickM(modelId);
        save(context, p);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static double getDouble(SharedPreferences sp, String key, double def) {
        String v = sp.getString(key, null);
        if (v == null) return def;
        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static void putDouble(SharedPreferences.Editor e, String key, double val) {
        e.putString(key, String.valueOf(val));
    }
}
