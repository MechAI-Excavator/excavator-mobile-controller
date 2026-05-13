package com.capstone.excavator;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 尺寸信息页：所选机型与每个机型对应的自定义几何参数（米 / 度）。
 *
 * <p>缓存结构：
 * <ul>
 *   <li>{@code selected_model}：当前选中的机型 id。</li>
 *   <li>{@code <model>.boom_m / stick_m / link_l1 ...}：每个机型独立保存的「微调」值。</li>
 * </ul>
 *
 * <p>Tab2「自定义尺寸」对当前选中的机型生效；切换机型只是切换 {@code selected_model}，
 * 之前各机型已经微调的值都会被保留。第一次选某机型时，会用机型预设 + 通用默认初始化一份缓存。
 */
public final class DimensionPreferences {

    private static final String PREFS_NAME = "dimension_prefs";

    public static final String MODEL_CAT_301 = "cat_301";
    public static final String MODEL_CAT_320 = "cat_320";
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

    private static final String KEY_SELECTED_MODEL = "selected_model";

    private static final String F_BOOM_M = "boom_m";
    private static final String F_STICK_M = "stick_m";
    private static final String F_LINK_L1 = "link_l1";
    private static final String F_LINK_L2 = "link_l2";
    private static final String F_LINK_L3 = "link_l3";
    private static final String F_LINK_L4 = "link_l4";
    private static final String F_LINK_ANGLE_DEG = "link_angle_deg";
    private static final String F_CHASSIS_H_M = "chassis_h_m";
    private static final String F_TRACK_W_M = "track_w_m";
    private static final String F_CABIN_H_M = "cabin_h_m";

    private static final String[] ALL_FIELD_KEYS = {
            F_BOOM_M, F_STICK_M,
            F_LINK_L1, F_LINK_L2, F_LINK_L3, F_LINK_L4, F_LINK_ANGLE_DEG,
            F_CHASSIS_H_M, F_TRACK_W_M, F_CABIN_H_M
    };

    public static final class Params {
        public String selectedModel = MODEL_CAT_301;
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
        String selected = sp.getString(KEY_SELECTED_MODEL, MODEL_CAT_301);
        migrateLegacyIfNeeded(sp, selected);

        Params p = new Params();
        p.selectedModel = selected;
        String prefix = selected + ".";
        p.boomM = getDouble(sp, prefix + F_BOOM_M, presetBoomM(selected));
        p.stickM = getDouble(sp, prefix + F_STICK_M, presetStickM(selected));
        p.linkL1 = getDouble(sp, prefix + F_LINK_L1, D_LINK_L1);
        p.linkL2 = getDouble(sp, prefix + F_LINK_L2, D_LINK_L2);
        p.linkL3 = getDouble(sp, prefix + F_LINK_L3, D_LINK_L3);
        p.linkL4 = getDouble(sp, prefix + F_LINK_L4, D_LINK_L4);
        p.linkAngleDeg = getDouble(sp, prefix + F_LINK_ANGLE_DEG, D_LINK_ANGLE_DEG);
        p.chassisHeightM = getDouble(sp, prefix + F_CHASSIS_H_M, D_CHASSIS_H);
        p.trackWidthM = getDouble(sp, prefix + F_TRACK_W_M, D_TRACK_W);
        p.cabinHeightM = getDouble(sp, prefix + F_CABIN_H_M, D_CABIN_H);
        return p;
    }

    public static void save(Context context, Params p) {
        SharedPreferences.Editor e = prefs(context).edit();
        String model = p.selectedModel == null ? MODEL_CAT_301 : p.selectedModel;
        e.putString(KEY_SELECTED_MODEL, model);
        String prefix = model + ".";
        putDouble(e, prefix + F_BOOM_M, p.boomM);
        putDouble(e, prefix + F_STICK_M, p.stickM);
        putDouble(e, prefix + F_LINK_L1, p.linkL1);
        putDouble(e, prefix + F_LINK_L2, p.linkL2);
        putDouble(e, prefix + F_LINK_L3, p.linkL3);
        putDouble(e, prefix + F_LINK_L4, p.linkL4);
        putDouble(e, prefix + F_LINK_ANGLE_DEG, p.linkAngleDeg);
        putDouble(e, prefix + F_CHASSIS_H_M, p.chassisHeightM);
        putDouble(e, prefix + F_TRACK_W_M, p.trackWidthM);
        putDouble(e, prefix + F_CABIN_H_M, p.cabinHeightM);
        e.apply();
    }

    /** 机型卡片上的大臂 / 小臂参考长度（米），数据来自 {@link DimensionModelCatalog}。 */
    public static double presetBoomM(String modelId) {
        DimensionModelCatalog.Entry e = DimensionModelCatalog.findByModelId(modelId);
        return e != null ? e.boomPresetM : 2.9;
    }

    public static double presetStickM(String modelId) {
        DimensionModelCatalog.Entry e = DimensionModelCatalog.findByModelId(modelId);
        return e != null ? e.stickPresetM : 1.5;
    }

    /**
     * 选中一个机型：
     * 1. 把 {@code selected_model} 切到 {@code modelId}；
     * 2. 如果该机型还没有任何缓存字段，则用机型预设 + 通用默认初始化一份并落盘；
     * 3. 已有的缓存（用户之前微调过）则保持不变。
     */
    public static void applyModelPreset(Context context, String modelId) {
        if (modelId == null) modelId = MODEL_CAT_301;
        SharedPreferences sp = prefs(context);
        SharedPreferences.Editor e = sp.edit();
        e.putString(KEY_SELECTED_MODEL, modelId);

        if (!hasAnyModelEntry(sp, modelId)) {
            String prefix = modelId + ".";
            putDouble(e, prefix + F_BOOM_M, presetBoomM(modelId));
            putDouble(e, prefix + F_STICK_M, presetStickM(modelId));
            putDouble(e, prefix + F_LINK_L1, D_LINK_L1);
            putDouble(e, prefix + F_LINK_L2, D_LINK_L2);
            putDouble(e, prefix + F_LINK_L3, D_LINK_L3);
            putDouble(e, prefix + F_LINK_L4, D_LINK_L4);
            putDouble(e, prefix + F_LINK_ANGLE_DEG, D_LINK_ANGLE_DEG);
            putDouble(e, prefix + F_CHASSIS_H_M, D_CHASSIS_H);
            putDouble(e, prefix + F_TRACK_W_M, D_TRACK_W);
            putDouble(e, prefix + F_CABIN_H_M, D_CABIN_H);
        }
        e.apply();
    }

    private static boolean hasAnyModelEntry(SharedPreferences sp, String modelId) {
        String prefix = modelId + ".";
        for (String k : ALL_FIELD_KEYS) {
            if (sp.contains(prefix + k)) return true;
        }
        return false;
    }

    /**
     * 兼容旧版本：旧 key 没有机型前缀，把它当成当前选中机型的缓存搬过来。
     * 仅在「该机型还没有任何缓存 + 存在旧 key」时迁移一次。
     */
    private static void migrateLegacyIfNeeded(SharedPreferences sp, String selectedModel) {
        if (hasAnyModelEntry(sp, selectedModel)) return;
        boolean anyLegacy = false;
        for (String k : ALL_FIELD_KEYS) {
            if (sp.contains(k)) { anyLegacy = true; break; }
        }
        if (!anyLegacy) return;

        String prefix = selectedModel + ".";
        SharedPreferences.Editor e = sp.edit();
        for (String k : ALL_FIELD_KEYS) {
            String v = sp.getString(k, null);
            if (v != null) {
                e.putString(prefix + k, v);
                e.remove(k);
            }
        }
        e.apply();
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
