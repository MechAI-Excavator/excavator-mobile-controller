package com.capstone.excavator;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 持久化「摇杆通道映射」基准值：每个挖机动作（{@link ControllerLocalSettings#JOYSTICK_KEY_BOOM}
 * 等）在云卓控制器 {@code ChannelSettings.ChannelItem.mapping} 上的原始整型码。
 *
 * <p>由 {@link JoystickChannelMappingApplier} 在首次成功读取 {@code ChannelSettings} 时
 * 一次性快照——假定此时控制器仍为默认布局：
 * <pre>
 *   channels[0] (右摇杆左右, GH) → 铲斗 (bucket)
 *   channels[1] (右摇杆上下, EF) → 大臂 (boom)
 *   channels[2] (左摇杆上下, AB) → 小臂 (stick)
 *   channels[3] (左摇杆左右, CD) → 回旋 (swing)
 * </pre>
 *
 * <p>之后再修改映射时只需读出现有 {@code ChannelSettings}，按用户新的轴→动作分配，
 * 将 {@code channels[i].mapping} 替换为对应动作的基准码即可，无需关心绝对整型值。
 */
public final class JoystickMappingChannelCodes {

    private static final String PREFS_NAME = "joystick_mapping_codes";

    private static final String KEY_HAS_BASELINE = "has_baseline";
    private static final String KEY_CODE_BOOM = "code_boom";
    private static final String KEY_CODE_STICK = "code_stick";
    private static final String KEY_CODE_BUCKET = "code_bucket";
    private static final String KEY_CODE_SWING = "code_swing";

    public static final class Baseline {
        public final int bucketCode;
        public final int boomCode;
        public final int stickCode;
        public final int swingCode;

        public Baseline(int bucketCode, int boomCode, int stickCode, int swingCode) {
            this.bucketCode = bucketCode;
            this.boomCode = boomCode;
            this.stickCode = stickCode;
            this.swingCode = swingCode;
        }

        /** 根据稳定 key（{@link ControllerLocalSettings#JOYSTICK_KEY_BOOM} 等）取对应 mapping 整型码。 */
        public int codeFor(String motionKey) {
            if (motionKey == null) return -1;
            switch (motionKey) {
                case ControllerLocalSettings.JOYSTICK_KEY_BOOM:   return boomCode;
                case ControllerLocalSettings.JOYSTICK_KEY_STICK:  return stickCode;
                case ControllerLocalSettings.JOYSTICK_KEY_BUCKET: return bucketCode;
                case ControllerLocalSettings.JOYSTICK_KEY_SWING:  return swingCode;
                default: return -1;
            }
        }
    }

    private JoystickMappingChannelCodes() {}

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static boolean hasBaseline(Context context) {
        return prefs(context).getBoolean(KEY_HAS_BASELINE, false);
    }

    public static Baseline load(Context context) {
        SharedPreferences sp = prefs(context);
        return new Baseline(
                sp.getInt(KEY_CODE_BUCKET, -1),
                sp.getInt(KEY_CODE_BOOM, -1),
                sp.getInt(KEY_CODE_STICK, -1),
                sp.getInt(KEY_CODE_SWING, -1));
    }

    public static void saveBaseline(Context context, Baseline baseline) {
        if (baseline == null) return;
        prefs(context).edit()
                .putBoolean(KEY_HAS_BASELINE, true)
                .putInt(KEY_CODE_BUCKET, baseline.bucketCode)
                .putInt(KEY_CODE_BOOM, baseline.boomCode)
                .putInt(KEY_CODE_STICK, baseline.stickCode)
                .putInt(KEY_CODE_SWING, baseline.swingCode)
                .apply();
    }

    /** 仅用于调试 / 配套设置页「重新校准」按钮。日常不要清除。 */
    public static void clear(Context context) {
        prefs(context).edit().clear().apply();
    }
}
