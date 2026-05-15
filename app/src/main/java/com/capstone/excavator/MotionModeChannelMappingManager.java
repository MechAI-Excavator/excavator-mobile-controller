package com.capstone.excavator;

import android.content.Context;
import android.util.Log;

import com.skydroid.rcsdk.common.callback.CompletionCallback;
import com.skydroid.rcsdk.common.error.SkyException;

/**
 * 运动模式到遥控器通道配置的切换层。
 */
public final class MotionModeChannelMappingManager {

    private static final String TAG = "MotionModeChannel";

    private MotionModeChannelMappingManager() {}

    public static void applyForMode(Context context, int motionModeIndex, CompletionCallback done) {
        if (context == null) {
            if (done != null) done.onResult(new SkyException(-1, "null context"));
            return;
        }

        switch (motionModeIndex) {
            case MotionModeSegmentView.INDEX_CHASSIS:
                Log.i(TAG, "apply chassis default channel mapping");
                JoystickChannelMappingApplier.applyDefaultMapping(context, done);
                break;
            case MotionModeSegmentView.INDEX_BUCKET:
                Log.i(TAG, "apply saved bucket channel mapping");
                JoystickChannelMappingApplier.applySavedBucketModeMapping(context, done);
                break;
            case MotionModeSegmentView.INDEX_STOP:
            default:
                Log.i(TAG, "skip channel mapping for motion mode " + motionModeIndex);
                if (done != null) done.onResult(null);
                break;
        }
    }
}
