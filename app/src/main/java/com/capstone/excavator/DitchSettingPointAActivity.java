package com.capstone.excavator;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;


/**
 * 挖沟 Step2：A 点参考点与 AB 距离。
 */
public class DitchSettingPointAActivity extends ScaledAppCompatActivity {

    private View btnBack;
    private View btnPrev;
    private View btnNext;
    private View btnHelp;
    private HelpTooltip helpTooltip;
    private NumpadView numpad;

    private LinearLayout cardRefLeftA;
    private LinearLayout cardRefMiddleA;
    private LinearLayout cardRefRightA;
    private TextView tvRefLeftA;
    private TextView tvRefMiddleA;
    private TextView tvRefRightA;
    private TextView tvAbDistance;
    private int selectedRefA = DitchTaskState.REF_MIDDLE;
    private ImageView imgDitchSectionType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFullScreenMode();
        setContentView(R.layout.activity_ditch_setting_pointa);

        bindViews();
        restoreFromState();
        setupRefCards();
        setupInputs();
        setupActions();
        imgDitchSectionType = findViewById(R.id.imgDitchSectionType);
        DitchStepNavigation.bindStepBar(this);

        applyDitchSectionTypeImage();
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnLevelBack);
        btnPrev = findViewById(R.id.btnDitchPrev);
        btnNext = findViewById(R.id.btnLevelNext);
        btnHelp = findViewById(R.id.btnLevelHelp);

        cardRefLeftA = findViewById(R.id.cardRefLeftA);
        cardRefMiddleA = findViewById(R.id.cardRefMiddleA);
        cardRefRightA = findViewById(R.id.cardRefRightA);
        tvRefLeftA = findViewById(R.id.tvRefLeftA);
        tvRefMiddleA = findViewById(R.id.tvRefMiddleA);
        tvRefRightA = findViewById(R.id.tvRefRightA);
        tvAbDistance = findViewById(R.id.tvCurrentRef);

        numpad = new NumpadView(this);
    }

    private void applyDitchSectionTypeImage() {
        if (imgDitchSectionType == null) {
            return;
        }
        imgDitchSectionType.setImageResource(DitchTaskState.isSquareDitch()
                ? R.drawable.ditch_step2a1
                : R.drawable.ditch_step2a);
    }

    private void restoreFromState() {
        selectedRefA = DitchTaskState.getRefA();
        if (tvAbDistance != null && !DitchTaskState.getAbDistance().isEmpty()) {
            tvAbDistance.setText(DitchTaskState.getAbDistance());
        }
    }

    private void setupRefCards() {
        if (cardRefLeftA != null) cardRefLeftA.setOnClickListener(v -> setRefA(DitchTaskState.REF_LEFT));
        if (cardRefMiddleA != null) cardRefMiddleA.setOnClickListener(v -> setRefA(DitchTaskState.REF_MIDDLE));
        if (cardRefRightA != null) cardRefRightA.setOnClickListener(v -> setRefA(DitchTaskState.REF_RIGHT));
        applyRefSelectionA();
    }

    private void setRefA(int index) {
        selectedRefA = index;
        applyRefSelectionA();
    }

    private void applyRefSelectionA() {
        applyOneRefGroup(selectedRefA, cardRefLeftA, cardRefMiddleA, cardRefRightA,
                tvRefLeftA, tvRefMiddleA, tvRefRightA);
    }

    private void applyOneRefGroup(
            int selected,
            View leftCard, View midCard, View rightCard,
            TextView leftTv, TextView midTv, TextView rightTv
    ) {
        if (leftCard != null) {
            leftCard.setBackground(getDrawable(selected == DitchTaskState.REF_LEFT
                    ? R.drawable.level_card_selected_bg : R.drawable.level_card_normal_bg));
        }
        if (midCard != null) {
            midCard.setBackground(getDrawable(selected == DitchTaskState.REF_MIDDLE
                    ? R.drawable.level_card_selected_bg : R.drawable.level_card_normal_bg));
        }
        if (rightCard != null) {
            rightCard.setBackground(getDrawable(selected == DitchTaskState.REF_RIGHT
                    ? R.drawable.level_card_selected_bg : R.drawable.level_card_normal_bg));
        }
        if (leftTv != null) {
            leftTv.setTextColor(getColor(selected == DitchTaskState.REF_LEFT
                    ? R.color.level_selected : R.color.level_unselected));
        }
        if (midTv != null) {
            midTv.setTextColor(getColor(selected == DitchTaskState.REF_MIDDLE
                    ? R.color.level_selected : R.color.level_unselected));
        }
        if (rightTv != null) {
            rightTv.setTextColor(getColor(selected == DitchTaskState.REF_RIGHT
                    ? R.color.level_selected : R.color.level_unselected));
        }
    }

    private void setupInputs() {
        if (tvAbDistance == null) return;
        tvAbDistance.setOnClickListener(v -> {
            if (numpad != null && numpad.isShowing()) {
                numpad.dismiss();
                return;
            }
            numpad.setOnConfirmListener(tvAbDistance::setText);
            numpad.showForAtScreen(tvAbDistance, tvAbDistance,
                    NumpadPositionConfig.SCREEN_X, NumpadPositionConfig.SCREEN_Y);
        });
    }

    private void setupActions() {
        DitchStepNavigation.bindBackToMain(btnBack, this);

        helpTooltip = new HelpTooltip(this, "选择 A 点参考斗尖，并输入 AB 距离。");
        helpTooltip.attach(btnHelp);

        if (btnPrev != null) {
            btnPrev.setOnClickListener(v -> {
                saveCurrentState();
                DitchStepNavigation.goToPrevious(this);
            });
        }
        if (btnNext != null) {
            btnNext.setOnClickListener(v -> {
                saveCurrentState();
                DitchStepNavigation.goToNext(this);
            });
        }
    }

    private void saveCurrentState() {
        DitchTaskState.updateBase(
                DitchTaskState.getDitchType(),
                selectedRefA,
                DitchTaskState.getRefB(),
                textOf(tvAbDistance)
        );
    }

    private static String textOf(TextView tv) {
        return tv == null ? "" : tv.getText().toString();
    }

    @Override
    protected void onStop() {
        saveCurrentState();
        super.onStop();
        if (helpTooltip != null) helpTooltip.dismiss();
        if (numpad != null && numpad.isShowing()) numpad.dismiss();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) setFullScreenMode();
    }

    private void setFullScreenMode() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.hide(WindowInsetsCompat.Type.systemBars());
            controller.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
    }
}
