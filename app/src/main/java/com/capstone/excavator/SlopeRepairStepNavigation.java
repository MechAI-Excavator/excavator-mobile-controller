package com.capstone.excavator;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * 修坡作业设置五步进度条与各页跳转。
 * <p>
 * Step1 修坡类型 → Step2 A点 → Step3 B点 → Step4 坡面参数 → Step5 作业前检查。
 */
public final class SlopeRepairStepNavigation {

    public static final int STEP_TYPE = 1;
    public static final int STEP_POINT_A = 2;
    public static final int STEP_POINT_B = 3;
    public static final int STEP_THIRD = 4;
    public static final int STEP_PRECHECK = 5;

    private static final int[] STEP_VIEW_IDS = {
            R.id.step1,
            R.id.step2,
            R.id.step3,
            R.id.step4,
            R.id.step5
    };

    private SlopeRepairStepNavigation() {}

    public static Class<? extends Activity> activityClassForStep(int step) {
        switch (step) {
            case STEP_TYPE:
                return SlopeRepairSettingActivity.class;
            case STEP_POINT_A:
                return SlopeRepairSecondSettingActivity.class;
            case STEP_POINT_B:
                return SlopeRepairSecondSettingActivityPointB.class;
            case STEP_THIRD:
                return SlopeRepairThirdSettingActivity.class;
            case STEP_PRECHECK:
                return SlopeRepairPrecheckActivity.class;
            default:
                return SlopeRepairSettingActivity.class;
        }
    }

    public static int logicalStepForBarIndex(int barIndex) {
        switch (barIndex) {
            case 1:
                return STEP_TYPE;
            case 2:
                return STEP_POINT_A;
            case 3:
                return STEP_POINT_B;
            case 4:
                return STEP_THIRD;
            case 5:
            default:
                return STEP_PRECHECK;
        }
    }

    public static int barIndexForLogicalStep(int logicalStep) {
        switch (logicalStep) {
            case STEP_TYPE:
                return 1;
            case STEP_POINT_A:
                return 2;
            case STEP_POINT_B:
                return 3;
            case STEP_THIRD:
                return 4;
            case STEP_PRECHECK:
            default:
                return 5;
        }
    }

    public static int previousLogicalStep(int logicalStep) {
        switch (logicalStep) {
            case STEP_POINT_A:
                return STEP_TYPE;
            case STEP_POINT_B:
                return STEP_POINT_A;
            case STEP_THIRD:
                return STEP_POINT_B;
            case STEP_PRECHECK:
                return STEP_THIRD;
            default:
                return STEP_TYPE;
        }
    }

    public static int nextLogicalStep(int logicalStep) {
        switch (logicalStep) {
            case STEP_TYPE:
                return STEP_POINT_A;
            case STEP_POINT_A:
                return STEP_POINT_B;
            case STEP_POINT_B:
                return STEP_THIRD;
            case STEP_THIRD:
                return STEP_PRECHECK;
            default:
                return STEP_TYPE;
        }
    }

    public static void goToStep(Activity from, int targetStep) {
        if (from == null) return;
        int current = logicalStepForActivity(from);
        if (current == targetStep) return;
        Intent intent = new Intent(from, activityClassForStep(targetStep));
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        from.startActivity(intent);
        from.finish();
    }

    public static void goToNext(Activity from, int currentStep) {
        goToStep(from, nextLogicalStep(currentStep));
    }

    public static void goToPrevious(Activity from, int currentStep) {
        goToStep(from, previousLogicalStep(currentStep));
    }

    public static void goToNext(Activity from) {
        if (from == null) return;
        goToNext(from, logicalStepForActivity(from));
    }

    public static void goToPrevious(Activity from) {
        if (from == null) return;
        goToPrevious(from, logicalStepForActivity(from));
    }

    public static int logicalStepForActivity(Activity activity) {
        if (activity instanceof SlopeRepairSettingActivity) return STEP_TYPE;
        if (activity instanceof SlopeRepairSecondSettingActivity) return STEP_POINT_A;
        if (activity instanceof SlopeRepairSecondSettingActivityPointB) return STEP_POINT_B;
        if (activity instanceof SlopeRepairThirdSettingActivity) return STEP_THIRD;
        if (activity instanceof SlopeRepairPrecheckActivity) return STEP_PRECHECK;
        return STEP_TYPE;
    }

    public static void bindStepBar(Activity activity) {
        if (activity == null) return;
        bindStepBar(activity, logicalStepForActivity(activity));
    }

    public static void bindStepBar(Activity activity, int logicalStep) {
        if (activity == null) return;
        int activeBarIndex = barIndexForLogicalStep(logicalStep);
        for (int i = 0; i < STEP_VIEW_IDS.length; i++) {
            View stepView = activity.findViewById(STEP_VIEW_IDS[i]);
            if (stepView == null) continue;
            int barIndex = i + 1;
            boolean active = barIndex <= activeBarIndex;
            stepView.setBackgroundResource(active
                    ? R.drawable.level_step_active
                    : R.drawable.level_step_inactive);
            final int targetStep = logicalStepForBarIndex(barIndex);
            stepView.setClickable(true);
            stepView.setFocusable(true);
            stepView.setOnClickListener(v -> goToStep(activity, targetStep));
        }
    }

    public static void bindBackToMain(@Nullable View backButton, Activity activity) {
        if (backButton == null || activity == null) return;
        backButton.setOnClickListener(v -> {
            SlopeRepairTaskState.reset();
            if (activity instanceof ScaledAppCompatActivity) {
                ((ScaledAppCompatActivity) activity).navigateToMain();
            } else {
                activity.finish();
            }
        });
    }
}
