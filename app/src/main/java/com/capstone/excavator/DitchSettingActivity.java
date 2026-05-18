package com.capstone.excavator;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * 挖沟 Step1：选择沟型（方形 / 梯形）。
 */
public class DitchSettingActivity extends ScaledAppCompatActivity {

    private View btnLevelBack;
    private View btnLevelNext;
    private View btnLevelHelp;
    private HelpTooltip helpTooltip;
    private boolean discardStateOnStop;

    private View cardDitchSquare;
    private View cardDitchTrapezoid;
    private TextView tvDitchSquare;
    private TextView tvDitchTrapezoid;
    private int selectedDitchType = DitchTaskState.DITCH_SQUARE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFullScreenMode();
        setContentView(R.layout.activity_ditch_setting);

        bindViews();
        restoreFromState();
        setupDitchTypeCards();
        setupActions();
        DitchStepNavigation.bindStepBar(this);
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

    private void bindViews() {
        btnLevelBack = findViewById(R.id.btnLevelBack);
        btnLevelNext = findViewById(R.id.btnLevelNext);
        btnLevelHelp = findViewById(R.id.btnLevelHelp);
        cardDitchSquare = findViewById(R.id.cardDitchSquare);
        cardDitchTrapezoid = findViewById(R.id.cardDitchTrapezoid);
        tvDitchSquare = findViewById(R.id.tvDitchSquare);
        tvDitchTrapezoid = findViewById(R.id.tvDitchTrapezoid);
    }

    private void restoreFromState() {
        selectedDitchType = DitchTaskState.getDitchType();
    }

    private void setupDitchTypeCards() {
        if (cardDitchSquare != null) {
            cardDitchSquare.setOnClickListener(v -> setDitchType(DitchTaskState.DITCH_SQUARE));
        }
        if (cardDitchTrapezoid != null) {
            cardDitchTrapezoid.setOnClickListener(v -> setDitchType(DitchTaskState.DITCH_TRAPEZOID));
        }
        applyDitchTypeSelection();
    }

    private void setDitchType(int type) {
        selectedDitchType = type;
        applyDitchTypeSelection();
    }

    private void applyDitchTypeSelection() {
        if (cardDitchSquare != null) {
            cardDitchSquare.setBackground(getDrawable(
                    selectedDitchType == DitchTaskState.DITCH_SQUARE
                            ? R.drawable.level_card_selected_bg
                            : R.drawable.level_card_normal_bg));
        }
        if (cardDitchTrapezoid != null) {
            cardDitchTrapezoid.setBackground(getDrawable(
                    selectedDitchType == DitchTaskState.DITCH_TRAPEZOID
                            ? R.drawable.level_card_selected_bg
                            : R.drawable.level_card_normal_bg));
        }
        if (tvDitchSquare != null) {
            tvDitchSquare.setTextColor(getColor(
                    selectedDitchType == DitchTaskState.DITCH_SQUARE
                            ? R.color.level_selected
                            : R.color.level_unselected));
        }
        if (tvDitchTrapezoid != null) {
            tvDitchTrapezoid.setTextColor(getColor(
                    selectedDitchType == DitchTaskState.DITCH_TRAPEZOID
                            ? R.color.level_selected
                            : R.color.level_unselected));
        }
    }

    private void setupActions() {
        DitchStepNavigation.bindBackToMain(btnLevelBack, this);

        helpTooltip = new HelpTooltip(
                this,
                "这里是帮助提示内容，你可以在此解释该页面的参数含义。"
        );
        helpTooltip.attach(btnLevelHelp);

        if (btnLevelNext != null) {
            btnLevelNext.setOnClickListener(v -> {
                saveCurrentState();
                DitchStepNavigation.goToNext(this);
            });
        }
    }

    private void saveCurrentState() {
        DitchTaskState.updateBase(
                selectedDitchType,
                DitchTaskState.getRefA(),
                DitchTaskState.getRefB(),
                DitchTaskState.getAbDistance()
        );
    }

    @Override
    protected void onStop() {
        if (!discardStateOnStop) {
            saveCurrentState();
        }
        super.onStop();
        if (helpTooltip != null) helpTooltip.dismiss();
    }
}
