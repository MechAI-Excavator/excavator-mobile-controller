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
 * 挖沟 Step3：B 点参考点。
 */
public class DitchSettingPointBActivity extends ScaledAppCompatActivity {

    private View btnBack;
    private View btnPrev;
    private View btnNext;
    private View btnHelp;
    private HelpTooltip helpTooltip;

    private LinearLayout cardRefLeftB;
    private LinearLayout cardRefMiddleB;
    private LinearLayout cardRefRightB;
    private TextView tvRefLeftB;
    private TextView tvRefMiddleB;
    private TextView tvRefRightB;
    private int selectedRefB = DitchTaskState.REF_MIDDLE;
    private ImageView imgDitchSectionType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFullScreenMode();
        setContentView(R.layout.activity_ditch_setting_pointb);

        bindViews();
        restoreFromState();
        setupRefCards();
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

        cardRefLeftB = findViewById(R.id.cardRefLeftA);
        cardRefMiddleB = findViewById(R.id.cardRefMiddleA);
        cardRefRightB = findViewById(R.id.cardRefRightA);
        tvRefLeftB = findViewById(R.id.tvRefLeftA);
        tvRefMiddleB = findViewById(R.id.tvRefMiddleA);
        tvRefRightB = findViewById(R.id.tvRefRightA);
    }

    private void restoreFromState() {
        selectedRefB = DitchTaskState.getRefB();
    }

    private void setupRefCards() {
        if (cardRefLeftB != null) cardRefLeftB.setOnClickListener(v -> setRefB(DitchTaskState.REF_LEFT));
        if (cardRefMiddleB != null) cardRefMiddleB.setOnClickListener(v -> setRefB(DitchTaskState.REF_MIDDLE));
        if (cardRefRightB != null) cardRefRightB.setOnClickListener(v -> setRefB(DitchTaskState.REF_RIGHT));
        applyRefSelectionB();
    }

    private void setRefB(int index) {
        selectedRefB = index;
        applyRefSelectionB();
    }

    private void applyRefSelectionB() {
        applyOneRefGroup(selectedRefB, cardRefLeftB, cardRefMiddleB, cardRefRightB,
                tvRefLeftB, tvRefMiddleB, tvRefRightB);
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

    private void setupActions() {
        DitchStepNavigation.bindBackToMain(btnBack, this);

        helpTooltip = new HelpTooltip(this, "选择 B 点参考斗尖。");
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


    private void applyDitchSectionTypeImage() {
        if (imgDitchSectionType == null) {
            return;
        }
        imgDitchSectionType.setImageResource(DitchTaskState.isSquareDitch()
                ? R.drawable.ditch_step2b1
                : R.drawable.ditch_step2b);
    }

    private void saveCurrentState() {
        DitchTaskState.updateBase(
                DitchTaskState.getDitchType(),
                DitchTaskState.getRefA(),
                selectedRefB,
                DitchTaskState.getAbDistance()
        );
    }

    @Override
    protected void onStop() {
        saveCurrentState();
        super.onStop();
        if (helpTooltip != null) helpTooltip.dismiss();
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
