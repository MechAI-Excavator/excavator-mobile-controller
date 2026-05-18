package com.capstone.excavator;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

/**
 * 尺寸信息页：所选机型与每个机型对应的自定义几何参数（米）。
 *
 * <p>出厂默认值：{@link DimensionModelCatalog#ENTRIES} 中各机型的 {@link DimensionModelCatalog.GeometryPreset}；
 * 运行时以 {@code dimension_prefs} 中 {@code &lt;modelId&gt;.boom_m} 等为准（按机型分 key 缓存）。</p>
 *
 * <p>字段与 {@code settings_page_dimensions.xml} 输入一一对应：
 * 机械臂 Lb/Ls、连杆 L2–L7/L9–L10、铲斗 L11–L14、驾驶舱 H1/W/H2。</p>
 */
public final class DimensionPreferences {

    private static final String PREFS_NAME = "dimension_prefs";

    public static final String MODEL_CAT_301 = "cat_301";
    public static final String MODEL_CAT_320 = "cat_320";
    public static final String MODEL_CAT_336 = "cat_336";

    /** 与布局默认展示一致（米）。 */

    // 机械臂长度
    public static final double D_BOOM_M = 2.9;
    public static final double D_STICK_M = 1.5;
    // 连杆参数
    public static final double D_LINK_L2 = 0.45;
    public static final double D_LINK_L3 = 0.38;
    public static final double D_LINK_L4 = 0.52;
    public static final double D_LINK_L5 = 0.41;
    public static final double D_LINK_L6 = 0.52;
    public static final double D_LINK_L7 = 0.41;
    public static final double D_LINK_L9 = 0.52;
    public static final double D_LINK_L10 = 0.41;
    // 铲斗参数
    public static final double D_LINK_L11 = 0.52;
    public static final double D_LINK_L12 = 0.41;
    public static final double D_LINK_L13 = 0.52;
    public static final double D_LINK_L14 = 0.41;
    // 驾驶舱相对比例
    public static final double D_CHASSIS_H = 0.95;
    public static final double D_TRACK_W = 2.4;
    public static final double D_CABIN_H2 = 2.8;

    private static final String KEY_SELECTED_MODEL = "selected_model";
    private static final String KEY_CATALOG_PRESET_VERSION = "catalog_preset_version";

    private static final String F_BOOM_M = "boom_m";
    private static final String F_STICK_M = "stick_m";
    private static final String F_LINK_L2 = "link_l2";
    private static final String F_LINK_L3 = "link_l3";
    private static final String F_LINK_L4 = "link_l4";
    private static final String F_LINK_L5 = "link_l5";
    private static final String F_LINK_L6 = "link_l6";
    private static final String F_LINK_L7 = "link_l7";
    private static final String F_LINK_L9 = "link_l9";
    private static final String F_LINK_L10 = "link_l10";
    private static final String F_LINK_L11 = "link_l11";
    private static final String F_LINK_L12 = "link_l12";
    private static final String F_LINK_L13 = "link_l13";
    private static final String F_LINK_L14 = "link_l14";
    private static final String F_LINK_ANGLE_DEG = "link_angle_deg";
    private static final String F_CHASSIS_H_M = "chassis_h_m";
    private static final String F_TRACK_W_M = "track_w_m";
    private static final String F_CABIN_H_M = "cabin_h_m";

    /** @deprecated 旧版 link_l1…l4，读取时迁移到 link_l2…l5 */
    private static final String F_LINK_L1_LEGACY = "link_l1";
    private static final String F_LINK_L2_LEGACY = "link_l2";
    private static final String F_LINK_L3_LEGACY = "link_l3";
    private static final String F_LINK_L4_LEGACY = "link_l4";

    private static final String[] ALL_FIELD_KEYS = {
            F_BOOM_M, F_STICK_M,
            F_LINK_L2, F_LINK_L3, F_LINK_L4, F_LINK_L5, F_LINK_L6, F_LINK_L7,
            F_LINK_L9, F_LINK_L10, F_LINK_L11, F_LINK_L12, F_LINK_L13, F_LINK_L14,
            F_LINK_ANGLE_DEG,
            F_CHASSIS_H_M, F_TRACK_W_M, F_CABIN_H_M
    };

    /** 与设置页输入控件对应的参数字段。 */
    public enum Field {
        /** 大臂长度 Lb（tvDimLb） */
        BOOM_M(F_BOOM_M, D_BOOM_M),
        /** 小臂长度 Ls（tvDimLs） */
        STICK_M(F_STICK_M, D_STICK_M),
        LINK_L2(F_LINK_L2, D_LINK_L2),
        LINK_L3(F_LINK_L3, D_LINK_L3),
        LINK_L4(F_LINK_L4, D_LINK_L4),
        LINK_L5(F_LINK_L5, D_LINK_L5),
        LINK_L6(F_LINK_L6, D_LINK_L6),
        LINK_L7(F_LINK_L7, D_LINK_L7),
        LINK_L9(F_LINK_L9, D_LINK_L9),
        LINK_L10(F_LINK_L10, D_LINK_L10),
        LINK_L11(F_LINK_L11, D_LINK_L11),
        LINK_L12(F_LINK_L12, D_LINK_L12),
        LINK_L13(F_LINK_L13, D_LINK_L13),
        LINK_L14(F_LINK_L14, D_LINK_L14),
        /** 底盘高度 H1（tvDimCabinH） */
        CHASSIS_H_M(F_CHASSIS_H_M, D_CHASSIS_H),
        /** 履带宽度 W（tvDimTrackW） */
        TRACK_W_M(F_TRACK_W_M, D_TRACK_W),
        /** 驾驶舱高度 H2（tvDimCabinH2） */
        CABIN_H2_M(F_CABIN_H_M, D_CABIN_H2);

        final String prefKey;
        final double defaultM;

        Field(String prefKey, double defaultM) {
            this.prefKey = prefKey;
            this.defaultM = defaultM;
        }
    }

    public static final class Params {
        public String selectedModel = MODEL_CAT_301;
        public double boomM = D_BOOM_M;
        public double stickM = D_STICK_M;
        public double linkL2 = D_LINK_L2;
        public double linkL3 = D_LINK_L3;
        public double linkL4 = D_LINK_L4;
        public double linkL5 = D_LINK_L5;
        public double linkL6 = D_LINK_L6;
        public double linkL7 = D_LINK_L7;
        public double linkL9 = D_LINK_L9;
        public double linkL10 = D_LINK_L10;
        public double linkL11 = D_LINK_L11;
        public double linkL12 = D_LINK_L12;
        public double linkL13 = D_LINK_L13;
        public double linkL14 = D_LINK_L14;
        public double chassisHeightM = D_CHASSIS_H;
        public double trackWidthM = D_TRACK_W;
        public double cabinHeightM = D_CABIN_H2;
    }

    private DimensionPreferences() {}

    public static double getValue(Params p, Field field) {
        switch (field) {
            case BOOM_M: return p.boomM;
            case STICK_M: return p.stickM;
            case LINK_L2: return p.linkL2;
            case LINK_L3: return p.linkL3;
            case LINK_L4: return p.linkL4;
            case LINK_L5: return p.linkL5;
            case LINK_L6: return p.linkL6;
            case LINK_L7: return p.linkL7;
            case LINK_L9: return p.linkL9;
            case LINK_L10: return p.linkL10;
            case LINK_L11: return p.linkL11;
            case LINK_L12: return p.linkL12;
            case LINK_L13: return p.linkL13;
            case LINK_L14: return p.linkL14;
            case CHASSIS_H_M: return p.chassisHeightM;
            case TRACK_W_M: return p.trackWidthM;
            case CABIN_H2_M: return p.cabinHeightM;
            default: return 0;
        }
    }

    public static void setValue(Params p, Field field, double valueM) {
        switch (field) {
            case BOOM_M: p.boomM = valueM; break;
            case STICK_M: p.stickM = valueM; break;
            case LINK_L2: p.linkL2 = valueM; break;
            case LINK_L3: p.linkL3 = valueM; break;
            case LINK_L4: p.linkL4 = valueM; break;
            case LINK_L5: p.linkL5 = valueM; break;
            case LINK_L6: p.linkL6 = valueM; break;
            case LINK_L7: p.linkL7 = valueM; break;
            case LINK_L9: p.linkL9 = valueM; break;
            case LINK_L10: p.linkL10 = valueM; break;
            case LINK_L11: p.linkL11 = valueM; break;
            case LINK_L12: p.linkL12 = valueM; break;
            case LINK_L13: p.linkL13 = valueM; break;
            case LINK_L14: p.linkL14 = valueM; break;
            case CHASSIS_H_M: p.chassisHeightM = valueM; break;
            case TRACK_W_M: p.trackWidthM = valueM; break;
            case CABIN_H2_M: p.cabinHeightM = valueM; break;
            default: break;
        }
    }

    /** 某机型的出厂默认（来自 {@link DimensionModelCatalog}）。 */
    public static Params factoryParamsForModel(@Nullable String modelId) {
        return DimensionModelCatalog.factoryParamsForModel(modelId);
    }

    public static double defaultForField(String modelId, Field field) {
        return getValue(factoryParamsForModel(modelId), field);
    }

    /**
     * {@link DimensionModelCatalog#CATALOG_PRESET_VERSION} 升高后，将各机型 {@link DimensionModelCatalog} 出厂表写入 storage。
     */
    public static void syncCatalogPresetsIfNeeded(Context context) {
        SharedPreferences sp = prefs(context);
        int storedVer = sp.getInt(KEY_CATALOG_PRESET_VERSION, 0);
        int catalogVer = DimensionModelCatalog.CATALOG_PRESET_VERSION;
        if (storedVer >= catalogVer) {
            return;
        }
        SharedPreferences.Editor e = sp.edit();
        for (DimensionModelCatalog.Entry entry : DimensionModelCatalog.ENTRIES) {
            writeParamsToEditor(e, entry.modelId + ".", factoryParamsForModel(entry.modelId));
        }
        e.putInt(KEY_CATALOG_PRESET_VERSION, catalogVer);
        e.apply();
    }

    public static Params load(Context context) {
        syncCatalogPresetsIfNeeded(context);
        SharedPreferences sp = prefs(context);
        String selected = sp.getString(KEY_SELECTED_MODEL, MODEL_CAT_301);
        migrateLegacyIfNeeded(sp, selected);
        migrateUiLinkKeysIfNeeded(sp, selected);
        return loadFromStorage(sp, selected);
    }

    /** 读取指定机型缓存；无字段时用该机型的 {@link DimensionModelCatalog} 出厂值。 */
    public static Params loadForModel(Context context, String modelId) {
        if (modelId == null) {
            modelId = MODEL_CAT_301;
        }
        SharedPreferences sp = prefs(context);
        migrateLegacyIfNeeded(sp, modelId);
        migrateUiLinkKeysIfNeeded(sp, modelId);
        return loadFromStorage(sp, modelId);
    }

    private static Params loadFromStorage(SharedPreferences sp, String modelId) {
        Params defaults = factoryParamsForModel(modelId);
        Params p = new Params();
        p.selectedModel = modelId;
        String prefix = modelId + ".";
        p.boomM = getDouble(sp, prefix + F_BOOM_M, defaults.boomM);
        p.stickM = getDouble(sp, prefix + F_STICK_M, defaults.stickM);
        p.linkL2 = readLink(sp, prefix, F_LINK_L2, F_LINK_L1_LEGACY, defaults.linkL2);
        p.linkL3 = readLink(sp, prefix, F_LINK_L3, F_LINK_L2_LEGACY, defaults.linkL3);
        p.linkL4 = readLink(sp, prefix, F_LINK_L4, F_LINK_L3_LEGACY, defaults.linkL4);
        p.linkL5 = readLink(sp, prefix, F_LINK_L5, F_LINK_L4_LEGACY, defaults.linkL5);
        p.linkL6 = getDouble(sp, prefix + F_LINK_L6, defaults.linkL6);
        p.linkL7 = getDouble(sp, prefix + F_LINK_L7, defaults.linkL7);
        p.linkL9 = getDouble(sp, prefix + F_LINK_L9, defaults.linkL9);
        p.linkL10 = getDouble(sp, prefix + F_LINK_L10, defaults.linkL10);
        p.linkL11 = getDouble(sp, prefix + F_LINK_L11, defaults.linkL11);
        p.linkL12 = getDouble(sp, prefix + F_LINK_L12, defaults.linkL12);
        p.linkL13 = getDouble(sp, prefix + F_LINK_L13, defaults.linkL13);
        p.linkL14 = getDouble(sp, prefix + F_LINK_L14, defaults.linkL14);
        p.chassisHeightM = getDouble(sp, prefix + F_CHASSIS_H_M, defaults.chassisHeightM);
        p.trackWidthM = getDouble(sp, prefix + F_TRACK_W_M, defaults.trackWidthM);
        p.cabinHeightM = getDouble(sp, prefix + F_CABIN_H_M, defaults.cabinHeightM);
        return p;
    }

    public static void save(Context context, Params p) {
        SharedPreferences.Editor e = prefs(context).edit();
        String model = p.selectedModel == null ? MODEL_CAT_301 : p.selectedModel;
        e.putString(KEY_SELECTED_MODEL, model);
        writeParamsToEditor(e, model + ".", p);
        e.apply();
    }

    /**
     * 选中机型：切换 {@code selected_model}；若该机型尚无缓存，将 {@link DimensionModelCatalog} 出厂配置写入 storage。
     */
    public static void applyModelPreset(Context context, String modelId) {
        if (modelId == null) {
            modelId = MODEL_CAT_301;
        }
        SharedPreferences sp = prefs(context);
        SharedPreferences.Editor e = sp.edit();
        e.putString(KEY_SELECTED_MODEL, modelId);
        if (!hasAnyModelEntry(sp, modelId)) {
            writeParamsToEditor(e, modelId + ".", factoryParamsForModel(modelId));
        }
        e.apply();
    }

    /** 用出厂配置覆盖指定机型的本地缓存（恢复默认）。 */
    public static void resetModelToFactory(Context context, String modelId) {
        if (modelId == null) {
            modelId = MODEL_CAT_301;
        }
        SharedPreferences.Editor e = prefs(context).edit();
        writeParamsToEditor(e, modelId + ".", factoryParamsForModel(modelId));
        e.apply();
    }

    private static void writeParamsToEditor(SharedPreferences.Editor e, String prefix, Params p) {
        putDouble(e, prefix + F_BOOM_M, p.boomM);
        putDouble(e, prefix + F_STICK_M, p.stickM);
        putDouble(e, prefix + F_LINK_L2, p.linkL2);
        putDouble(e, prefix + F_LINK_L3, p.linkL3);
        putDouble(e, prefix + F_LINK_L4, p.linkL4);
        putDouble(e, prefix + F_LINK_L5, p.linkL5);
        putDouble(e, prefix + F_LINK_L6, p.linkL6);
        putDouble(e, prefix + F_LINK_L7, p.linkL7);
        putDouble(e, prefix + F_LINK_L9, p.linkL9);
        putDouble(e, prefix + F_LINK_L10, p.linkL10);
        putDouble(e, prefix + F_LINK_L11, p.linkL11);
        putDouble(e, prefix + F_LINK_L12, p.linkL12);
        putDouble(e, prefix + F_LINK_L13, p.linkL13);
        putDouble(e, prefix + F_LINK_L14, p.linkL14);
        putDouble(e, prefix + F_CHASSIS_H_M, p.chassisHeightM);
        putDouble(e, prefix + F_TRACK_W_M, p.trackWidthM);
        putDouble(e, prefix + F_CABIN_H_M, p.cabinHeightM);
    }

    private static double readLink(
            SharedPreferences sp, String prefix, String key, String legacyKey, double def) {
        if (sp.contains(prefix + key)) {
            return getDouble(sp, prefix + key, def);
        }
        if (sp.contains(prefix + legacyKey)) {
            return getDouble(sp, prefix + legacyKey, def);
        }
        return def;
    }

    private static boolean hasAnyModelEntry(SharedPreferences sp, String modelId) {
        String prefix = modelId + ".";
        for (String k : ALL_FIELD_KEYS) {
            if (sp.contains(prefix + k)) return true;
        }
        for (String legacy : new String[] {
                F_LINK_L1_LEGACY, F_LINK_L2_LEGACY, F_LINK_L3_LEGACY, F_LINK_L4_LEGACY
        }) {
            if (sp.contains(prefix + legacy)) return true;
        }
        return false;
    }

    /**
     * 旧版连杆存为 link_l1…l4（与 UI 标签 L2…L5 错位），一次性迁到 link_l2…l5。
     */
    private static void migrateUiLinkKeysIfNeeded(SharedPreferences sp, String modelId) {
        String prefix = modelId + ".";
        String flag = prefix + "dim_link_ui_v2";
        if (sp.getBoolean(flag, false)) {
            return;
        }
        if (!sp.contains(prefix + F_LINK_L1_LEGACY)) {
            return;
        }
        SharedPreferences.Editor e = sp.edit();
        copyPrefString(sp, e, prefix + F_LINK_L1_LEGACY, prefix + F_LINK_L2);
        copyPrefString(sp, e, prefix + F_LINK_L2_LEGACY, prefix + F_LINK_L3);
        copyPrefString(sp, e, prefix + F_LINK_L3_LEGACY, prefix + F_LINK_L4);
        copyPrefString(sp, e, prefix + F_LINK_L4_LEGACY, prefix + F_LINK_L5);
        e.remove(prefix + F_LINK_L1_LEGACY);
        e.remove(prefix + F_LINK_L2_LEGACY);
        e.remove(prefix + F_LINK_L3_LEGACY);
        e.remove(prefix + F_LINK_L4_LEGACY);
        e.putBoolean(flag, true);
        e.apply();
    }

    private static void copyPrefString(
            SharedPreferences sp, SharedPreferences.Editor e, String fromKey, String toKey) {
        String v = sp.getString(fromKey, null);
        if (v != null) {
            e.putString(toKey, v);
        }
    }

    private static void migrateLegacyIfNeeded(SharedPreferences sp, String selectedModel) {
        if (hasAnyModelEntry(sp, selectedModel)) return;
        boolean anyLegacy = false;
        for (String k : ALL_FIELD_KEYS) {
            if (sp.contains(k)) { anyLegacy = true; break; }
        }
        if (!anyLegacy) {
            for (String legacy : new String[] {
                    F_LINK_L1_LEGACY, F_LINK_L2_LEGACY, F_LINK_L3_LEGACY, F_LINK_L4_LEGACY
            }) {
                if (sp.contains(legacy)) { anyLegacy = true; break; }
            }
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
        for (String legacy : new String[] {
                F_LINK_L1_LEGACY, F_LINK_L2_LEGACY, F_LINK_L3_LEGACY, F_LINK_L4_LEGACY
        }) {
            String v = sp.getString(legacy, null);
            if (v != null) {
                e.putString(prefix + legacy, v);
                e.remove(legacy);
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
