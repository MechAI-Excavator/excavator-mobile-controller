package com.capstone.excavator;

import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 尺寸设置「选择机型」Tab：卡片 UI + 各机型出厂几何预设（米）。
 *
 * <p>每个 {@link Entry#geometry} 为该机型的完整出厂配置（含连杆 L2–L14 逐项数值）；
 * 首次选中机型时写入 {@code dimension_prefs}，之后以本地缓存为准。
 * 修改 {@code GEOMETRY_CAT_301} / {@code GEOMETRY_CAT_320} / {@code GEOMETRY_CAT_336}
 * 只影响「尚未缓存过」的机型；已缓存可调用 {@link DimensionPreferences#resetModelToFactory}。</p>
 */
public final class DimensionModelCatalog {

    /**
     * 出厂表 {@code GEOMETRY_CAT_*} 变更后请 +1；进入设置页时会用新表覆盖各机型本地缓存。
     * （用户曾在设置里改过的数值也会被更新；若只想改未缓存过的机型，请勿递增此项。）
     */
    public static final int CATALOG_PRESET_VERSION = 2;

    /**
     * 单机型完整尺寸预设，与 {@link DimensionPreferences.Params} 字段一一对应。
     * 在 {@link #ENTRIES} 里为 cat_301 / cat_320 / cat_336 各配一份即可。
     */
    public static final class GeometryPreset {
        public final double boomM;
        public final double stickM;
        public final double linkL2;
        public final double linkL3;
        public final double linkL4;
        public final double linkL5;
        public final double linkL6;
        public final double linkL7;
        public final double linkL9;
        public final double linkL10;
        public final double linkL11;
        public final double linkL12;
        public final double linkL13;
        public final double linkL14;
        public final double chassisHeightM;
        public final double trackWidthM;
        public final double cabinHeightM;

        public GeometryPreset(
                double boomM,
                double stickM,
                double linkL2,
                double linkL3,
                double linkL4,
                double linkL5,
                double linkL6,
                double linkL7,
                double linkL9,
                double linkL10,
                double linkL11,
                double linkL12,
                double linkL13,
                double linkL14,
                double chassisHeightM,
                double trackWidthM,
                double cabinHeightM) {
            this.boomM = boomM;
            this.stickM = stickM;
            this.linkL2 = linkL2;
            this.linkL3 = linkL3;
            this.linkL4 = linkL4;
            this.linkL5 = linkL5;
            this.linkL6 = linkL6;
            this.linkL7 = linkL7;
            this.linkL9 = linkL9;
            this.linkL10 = linkL10;
            this.linkL11 = linkL11;
            this.linkL12 = linkL12;
            this.linkL13 = linkL13;
            this.linkL14 = linkL14;
            this.chassisHeightM = chassisHeightM;
            this.trackWidthM = trackWidthM;
            this.cabinHeightM = cabinHeightM;
        }

        public DimensionPreferences.Params toParams(String modelId) {
            DimensionPreferences.Params p = new DimensionPreferences.Params();
            p.selectedModel = modelId;
            p.boomM = boomM;
            p.stickM = stickM;
            p.linkL2 = linkL2;
            p.linkL3 = linkL3;
            p.linkL4 = linkL4;
            p.linkL5 = linkL5;
            p.linkL6 = linkL6;
            p.linkL7 = linkL7;
            p.linkL9 = linkL9;
            p.linkL10 = linkL10;
            p.linkL11 = linkL11;
            p.linkL12 = linkL12;
            p.linkL13 = linkL13;
            p.linkL14 = linkL14;
            p.chassisHeightM = chassisHeightM;
            p.trackWidthM = trackWidthM;
            p.cabinHeightM = cabinHeightM;
            return p;
        }
    }

    public static final class Entry {
        public final String modelId;
        public final int imageResId;
        public final String displayName;
        public final String tonnageLabel;
        public final String boomCardText;
        public final String stickCardText;
        public final float titleTextSp;
        /** 该机型的完整出厂尺寸；读写缓存见 {@link DimensionPreferences}。 */
        public final GeometryPreset geometry;

        public Entry(
                String modelId,
                int imageResId,
                String displayName,
                String tonnageLabel,
                String boomCardText,
                String stickCardText,
                float titleTextSp,
                GeometryPreset geometry) {
            this.modelId = modelId;
            this.imageResId = imageResId;
            this.displayName = displayName;
            this.tonnageLabel = tonnageLabel;
            this.boomCardText = boomCardText;
            this.stickCardText = stickCardText;
            this.titleTextSp = titleTextSp;
            this.geometry = geometry;
        }

        public double boomPresetM() {
            return geometry.boomM;
        }

        public double stickPresetM() {
            return geometry.stickM;
        }
    }

    // ── 出厂几何：Lb/Ls、连杆 L2–L7/L9–L10、铲斗 L11–L14、底盘 H1、履带 W、驾驶舱 H2（米）──

    private static final GeometryPreset GEOMETRY_CAT_301 = new GeometryPreset(
            /* Lb  */ 1.7654,
            /* Ls  */ 1.0325,
            /* L2  */ 0.1957,
            /* L3  */ 0.7727,
            /* L4  */ 0.8501,
            /* L5  */ 0.2142,
            /* L6  */ 1.0325,
            /* L7  */ 0.2030,
            /* L9  */ 0.0,
            /* L10 */ 0.0,
            /* L11 */ 0.0,
            /* L12 */ 0.0,
            /* L13 */ 0.0,
            /* L14 */ 0.0,
            /* H1  */ 0,
            /* W   */ 0,
            /* H2  */ 0);

    private static final GeometryPreset GEOMETRY_CAT_320 = new GeometryPreset(
            /* Lb  */ 5.6719,
            /* Ls  */ 2.9192,
            /* L2  */ 0.6330,
            /* L3  */ 2.2575,
            /* L4  */ 2.5152,
            /* L5  */ 0.7590,
            /* L6  */ 2.9192,
            /* L7  */ 0.4086,
            /* L9  */ 0.5947,
            /* L10 */ 0.4476,
            /* L11 */ 0.0,
            /* L12 */ 0.0,
            /* L13 */ 0.0,
            /* L14 */ 0.0,
            /* H1  */ 0.0,
            /* W   */ 0.0,
            /* H2  */ 0.0);

    private static final GeometryPreset GEOMETRY_CAT_336 = new GeometryPreset(
            /* Lb  */ 6.5091,
            /* Ls  */ 2.7950,
            /* L2  */ 0.7142,
            /* L3  */ 2.3443,
            /* L4  */ 2.2931,
            /* L5  */ 0.7612,
            /* L6  */ 2.7950,
            /* L7  */ 0.5026,
            /* L9  */ 0.6652,
            /* L10 */ 0.4940,
            /* L11 */ 0.0,
            /* L12 */ 0.0,
            /* L13 */ 0.0,
            /* L14 */ 0.0,
            /* H1  */ 0.0,  
            /* W   */ 0.0,
            /* H2  */ 0.0);

    /** 各机型出厂映射：改上方 {@code GEOMETRY_CAT_*} → 首次选中该机型时写入 storage。 */
    public static final List<Entry> ENTRIES = Collections.unmodifiableList(Arrays.asList(
            new Entry(
                    DimensionPreferences.MODEL_CAT_301,
                    R.drawable.cat_303,
                    "CAT 301.7",
                    "3.4t",
                    "1.7654 m",
                    "1.0325 m",
                    16f,
                    GEOMETRY_CAT_301),
            new Entry(
                    DimensionPreferences.MODEL_CAT_320,
                    R.drawable.cat_312,
                    "CAT 320GC",
                    "13t",
                    "5.6719 m",
                    "2.9192 m",
                    13f,
                    GEOMETRY_CAT_320),
            new Entry(
                    DimensionPreferences.MODEL_CAT_336,
                    R.drawable.cat_320,
                    "CAT 336",
                    "20t",
                    "6.5091 m",
                    "2.7950 m",
                    13f,
                    GEOMETRY_CAT_336)
    ));

    private DimensionModelCatalog() {}

    @Nullable
    public static Entry findByModelId(@Nullable String modelId) {
        if (modelId == null) {
            return null;
        }
        for (Entry e : ENTRIES) {
            if (e.modelId.equals(modelId)) {
                return e;
            }
        }
        return null;
    }

    /** 机型出厂完整参数；未知机型时返回全局默认。 */
    public static DimensionPreferences.Params factoryParamsForModel(@Nullable String modelId) {
        Entry entry = findByModelId(modelId);
        String id = modelId != null ? modelId : DimensionPreferences.MODEL_CAT_301;
        if (entry != null) {
            return entry.geometry.toParams(id);
        }
        DimensionPreferences.Params p = new DimensionPreferences.Params();
        p.selectedModel = id;
        return p;
    }
}
