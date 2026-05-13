package com.capstone.excavator;

import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 尺寸设置「选择机型」Tab：机型卡片展示数据与预设大臂/小臂长度（米）。
 * <p>新增机型时只改 {@link #ENTRIES} 一处，并在 {@link DimensionPreferences} 中增加对应 {@code MODEL_*} 常量（若需要）。</p>
 */
public final class DimensionModelCatalog {

    public static final class Entry {
        /** 与 {@link DimensionPreferences} 中存储的机型 id 一致。 */
        public final String modelId;
        public final int imageResId;
        public final String displayName;
        public final String tonnageLabel;
        /** 卡片上展示的大臂参考长度文案。 */
        public final String boomCardText;
        /** 卡片上展示的小臂参考长度文案。 */
        public final String stickCardText;
        public final float titleTextSp;
        /** 首次选中该机型的默认大臂长度（与历史逻辑一致）。 */
        public final double boomPresetM;
        /** 首次选中该机型的默认小臂长度。 */
        public final double stickPresetM;

        public Entry(
                String modelId,
                int imageResId,
                String displayName,
                String tonnageLabel,
                String boomCardText,
                String stickCardText,
                float titleTextSp,
                double boomPresetM,
                double stickPresetM) {
            this.modelId = modelId;
            this.imageResId = imageResId;
            this.displayName = displayName;
            this.tonnageLabel = tonnageLabel;
            this.boomCardText = boomCardText;
            this.stickCardText = stickCardText;
            this.titleTextSp = titleTextSp;
            this.boomPresetM = boomPresetM;
            this.stickPresetM = stickPresetM;
        }
    }

    public static final List<Entry> ENTRIES = Collections.unmodifiableList(Arrays.asList(
            new Entry(
                    DimensionPreferences.MODEL_CAT_301,
                    R.drawable.cat_303,
                    "CAT 301.7",
                    "3.4t",
                    "1.7654 m",
                    "1.0325 m",
                    16f,
                    2.9,
                    1.5),
            new Entry(
                    DimensionPreferences.MODEL_CAT_320,
                    R.drawable.cat_312,
                    "CAT 320GC",
                    "13t",
                    "5.6719 m",
                    "2.9192 m",
                    13f,
                    5.8,
                    2.9),
            new Entry(
                    DimensionPreferences.MODEL_CAT_336,
                    R.drawable.cat_320,
                    "CAT 336",
                    "20t",
                    "6.5091 m",
                    "2.7950 m",
                    13f,
                    6.8,
                    3.4)
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
}
