package com.capstone.excavator;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class SlopeRepairSecondSettingActivity extends AppCompatActivity {

    private View btnBack;
    private View btnHelp;
    private View btnPrev;
    private View btnNext;

    private HelpTooltip helpTooltip;
    private NumpadView numpad;

    private LinearLayout cardRefLeftA, cardRefMiddleA, cardRefRightA;
    private TextView tvRefLeftA, tvRefMiddleA, tvRefRightA;
    private int selectedRefA = 1;

    private LinearLayout cardRefLeftB, cardRefMiddleB, cardRefRightB;
    private TextView tvRefLeftB, tvRefMiddleB, tvRefRightB;
    private int selectedRefB = 1;

    private TextView tvAbDistance;
    private TextView tvAbLift;
    private TextView tvAbHeightDiff;

    // Fixed popup position for landscape UI
    private static final int NUMPAD_SCREEN_X = 1260;
    private static final int NUMPAD_SCREEN_Y = 278;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFullScreenMode();
        setContentView(R.layout.activity_slope_repair_second_setting);

        bindViews();
        setupHelp();
        setupRefCards();
        setupInputs();
        setupActions();
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnLevelBack);
        btnHelp = findViewById(R.id.btnLevelHelp);
        btnPrev = findViewById(R.id.btnDitchPrev);
        btnNext = findViewById(R.id.btnSlopeRepairNext);

        cardRefLeftA = findViewById(R.id.cardRefLeftA);
        cardRefMiddleA = findViewById(R.id.cardRefMiddleA);
        cardRefRightA = findViewById(R.id.cardRefRightA);
        tvRefLeftA = findViewById(R.id.tvRefLeftA);
        tvRefMiddleA = findViewById(R.id.tvRefMiddleA);
        tvRefRightA = findViewById(R.id.tvRefRightA);

        cardRefLeftB = findViewById(R.id.cardRefLeftB);
        cardRefMiddleB = findViewById(R.id.cardRefMiddleB);
        cardRefRightB = findViewById(R.id.cardRefRightB);
        tvRefLeftB = findViewById(R.id.tvRefLeftB);
        tvRefMiddleB = findViewById(R.id.tvRefMiddleB);
        tvRefRightB = findViewById(R.id.tvRefRightB);

        tvAbDistance = findViewById(R.id.tvAbDistance);
        tvAbLift = findViewById(R.id.tvAbLift);
        tvAbHeightDiff = findViewById(R.id.tvAbHeightDiff);

        numpad = new NumpadView(this);
    }

    private void setupHelp() {
        helpTooltip = new HelpTooltip(this, "这里是帮助提示内容，你可以在此解释修坡 B 点与 AB 参数含义。");
        helpTooltip.attach(btnHelp);
    }

    private void setupRefCards() {
        if (cardRefLeftA != null) cardRefLeftA.setOnClickListener(v -> setRefA(0));
        if (cardRefMiddleA != null) cardRefMiddleA.setOnClickListener(v -> setRefA(1));
        if (cardRefRightA != null) cardRefRightA.setOnClickListener(v -> setRefA(2));
        applyRefSelectionA();

        if (cardRefLeftB != null) cardRefLeftB.setOnClickListener(v -> setRefB(0));
        if (cardRefMiddleB != null) cardRefMiddleB.setOnClickListener(v -> setRefB(1));
        if (cardRefRightB != null) cardRefRightB.setOnClickListener(v -> setRefB(2));
        applyRefSelectionB();
    }

    private void setRefA(int index) {
        selectedRefA = index;
        applyRefSelectionA();
    }

    private void applyRefSelectionA() {
        applyOneRefGroup(selectedRefA, cardRefLeftA, cardRefMiddleA, cardRefRightA,
                tvRefLeftA, tvRefMiddleA, tvRefRightA);
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
        if (leftCard != null) leftCard.setBackground(getDrawable(selected == 0
                ? R.drawable.level_card_selected_bg : R.drawable.level_card_normal_bg));
        if (midCard != null) midCard.setBackground(getDrawable(selected == 1
                ? R.drawable.level_card_selected_bg : R.drawable.level_card_normal_bg));
        if (rightCard != null) rightCard.setBackground(getDrawable(selected == 2
                ? R.drawable.level_card_selected_bg : R.drawable.level_card_normal_bg));

        if (leftTv != null) leftTv.setTextColor(getColor(selected == 0 ? R.color.level_selected : R.color.level_unselected));
        if (midTv != null) midTv.setTextColor(getColor(selected == 1 ? R.color.level_selected : R.color.level_unselected));
        if (rightTv != null) rightTv.setTextColor(getColor(selected == 2 ? R.color.level_selected : R.color.level_unselected));
    }

    private void setupInputs() {
        setupOneInput(tvAbDistance);
        setupOneInput(tvAbLift);
        setupOneInput(tvAbHeightDiff);
    }

    private void setupOneInput(TextView tv) {
        if (tv == null) return;
        tv.setOnClickListener(v -> {
            if (numpad != null && numpad.isShowing()) {
                numpad.dismiss();
                return;
            }
            if (numpad == null) return;
            numpad.setOnConfirmListener(tv::setText);
            numpad.showForAtScreen(tv, tv, NUMPAD_SCREEN_X, NUMPAD_SCREEN_Y);
        });
    }

    private void setupActions() {
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (btnPrev != null) {
            btnPrev.setOnClickListener(v -> finish());
        }

        if (btnNext != null) {
            btnNext.setOnClickListener(v -> {
                startActivity(new Intent(this, SlopeRepairThirdSettingActivity.class));
            });
        }
    }

    @Override
    protected void onStop() {
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

