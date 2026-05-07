package com.capstone.excavator;

import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;


public class DitchSettingActivity extends ScaledAppCompatActivity {

    // ── 其他控件 ─────────────────────────────────────────────
    private View btnLevelBack, btnLevelNext;
    private View btnLevelHelp;
    private HelpTooltip helpTooltip;
    private NumpadView numpad;
    private boolean discardStateOnStop;

    // ── Step1：沟型选择 ───────────────────────────────────────
    private View cardDitchSquare, cardDitchTrapezoid;
    private TextView tvDitchSquare, tvDitchTrapezoid;
    private int selectedDitchType = 0; // 0=方形沟 1=梯形沟

    // ── Step2：A/B 点参考点选择 ───────────────────────────────
    private LinearLayout cardRefLeftA, cardRefMiddleA, cardRefRightA;
    private TextView tvRefLeftA, tvRefMiddleA, tvRefRightA;
    private int selectedRefA = 1;

    private LinearLayout cardRefLeftB, cardRefMiddleB, cardRefRightB;
    private TextView tvRefLeftB, tvRefMiddleB, tvRefRightB;
    private int selectedRefB = 1;
    private TextView tvAbDistance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFullScreenMode();
        setContentView(R.layout.activity_ditch_setting);

        bindViews();
        numpad = new NumpadView(this);
        restoreFromState();
        setupDitchTypeCards();
        setupRefCards();
        setupInputs();
        setupActions();
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
        tvAbDistance = findViewById(R.id.tvCurrentRef);
    }

    private void restoreFromState() {
        selectedDitchType = DitchTaskState.getDitchType();
        selectedRefA = DitchTaskState.getRefA();
        selectedRefB = DitchTaskState.getRefB();
        if (tvAbDistance != null && !DitchTaskState.getAbDistance().isEmpty()) {
            tvAbDistance.setText(DitchTaskState.getAbDistance());
        }
    }

    private void setupDitchTypeCards() {
        if (cardDitchSquare != null) cardDitchSquare.setOnClickListener(v -> setDitchType(0));
        if (cardDitchTrapezoid != null) cardDitchTrapezoid.setOnClickListener(v -> setDitchType(1));
        applyDitchTypeSelection();
    }

    private void setDitchType(int type) {
        selectedDitchType = type;
        applyDitchTypeSelection();
    }

    private void applyDitchTypeSelection() {
        if (cardDitchSquare != null) {
            cardDitchSquare.setBackground(getDrawable(
                    selectedDitchType == 0 ? R.drawable.level_card_selected_bg : R.drawable.level_card_normal_bg
            ));
        }
        if (cardDitchTrapezoid != null) {
            cardDitchTrapezoid.setBackground(getDrawable(
                    selectedDitchType == 1 ? R.drawable.level_card_selected_bg : R.drawable.level_card_normal_bg
            ));
        }
        if (tvDitchSquare != null) tvDitchSquare.setTextColor(getColor(
                selectedDitchType == 0 ? R.color.level_selected : R.color.level_unselected
        ));
        if (tvDitchTrapezoid != null) tvDitchTrapezoid.setTextColor(getColor(
                selectedDitchType == 1 ? R.color.level_selected : R.color.level_unselected
        ));
    }

    // ── 参考点卡片 ────────────────────────────────────────────

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
        if (cardRefLeftA != null)
            cardRefLeftA.setBackground(getDrawable(selectedRefA == 0
                    ? R.drawable.level_card_selected_bg : R.drawable.level_card_normal_bg));
        if (tvRefLeftA != null)
            tvRefLeftA.setTextColor(getColor(selectedRefA == 0 ? R.color.level_selected : R.color.level_unselected));

        if (cardRefMiddleA != null)
            cardRefMiddleA.setBackground(getDrawable(selectedRefA == 1
                    ? R.drawable.level_card_selected_bg : R.drawable.level_card_normal_bg));
        if (tvRefMiddleA != null)
            tvRefMiddleA.setTextColor(getColor(selectedRefA == 1 ? R.color.level_selected : R.color.level_unselected));

        if (cardRefRightA != null)
            cardRefRightA.setBackground(getDrawable(selectedRefA == 2
                    ? R.drawable.level_card_selected_bg : R.drawable.level_card_normal_bg));
        if (tvRefRightA != null)
            tvRefRightA.setTextColor(getColor(selectedRefA == 2 ? R.color.level_selected : R.color.level_unselected));
    }

    private void setRefB(int index) {
        selectedRefB = index;
        applyRefSelectionB();
    }

    private void applyRefSelectionB() {
        if (cardRefLeftB != null)
            cardRefLeftB.setBackground(getDrawable(selectedRefB == 0
                    ? R.drawable.level_card_selected_bg : R.drawable.level_card_normal_bg));
        if (tvRefLeftB != null)
            tvRefLeftB.setTextColor(getColor(selectedRefB == 0 ? R.color.level_selected : R.color.level_unselected));

        if (cardRefMiddleB != null)
            cardRefMiddleB.setBackground(getDrawable(selectedRefB == 1
                    ? R.drawable.level_card_selected_bg : R.drawable.level_card_normal_bg));
        if (tvRefMiddleB != null)
            tvRefMiddleB.setTextColor(getColor(selectedRefB == 1 ? R.color.level_selected : R.color.level_unselected));

        if (cardRefRightB != null)
            cardRefRightB.setBackground(getDrawable(selectedRefB == 2
                    ? R.drawable.level_card_selected_bg : R.drawable.level_card_normal_bg));
        if (tvRefRightB != null)
            tvRefRightB.setTextColor(getColor(selectedRefB == 2 ? R.color.level_selected : R.color.level_unselected));
    }

    private void setupInputs() {
        if (tvAbDistance == null) {
            return;
        }
        tvAbDistance.setOnClickListener(v -> {
            if (numpad != null && numpad.isShowing()) {
                numpad.dismiss();
                return;
            }
            if (numpad == null) {
                return;
            }
            numpad.setOnConfirmListener(tvAbDistance::setText);
            numpad.showForAtScreen(tvAbDistance, tvAbDistance,
                    300, NumpadPositionConfig.SCREEN_Y);
        });
    }

    // ── 按钮动作 ──────────────────────────────────────────────

    private void setupActions() {
        if (btnLevelBack != null) {
            btnLevelBack.setOnClickListener(v -> {
                discardStateOnStop = true;
                DitchTaskState.reset();
                navigateToMain();
            });
        }

        helpTooltip = new HelpTooltip(
                this,
                "这里是帮助提示内容，你可以在此解释该页面的参数含义。"
        );
        helpTooltip.attach(btnLevelHelp);

        if (btnLevelNext != null) {
            btnLevelNext.setOnClickListener(v -> {
                String[] refNames = {"左斗尖", "中斗尖", "右斗尖"};
                String ditchType = selectedDitchType == 0 ? "方形沟" : "梯形沟";
                String refA = refNames[Math.max(0, Math.min(2, selectedRefA))];
                String refB = refNames[Math.max(0, Math.min(2, selectedRefB))];
                saveCurrentState();

                Toast.makeText(this,
                        "沟型：" + ditchType + "\nA点参考：" + refA + "  B点参考：" + refB,
                        Toast.LENGTH_LONG).show();

                startActivity(new Intent(this, DitchWorkSettingActivity.class));
            });
        }
    }

    private void saveCurrentState() {
        DitchTaskState.updateBase(
                selectedDitchType,
                selectedRefA,
                selectedRefB,
                tvAbDistance != null ? tvAbDistance.getText().toString() : ""
        );
    }

    @Override
    protected void onStop() {
        if (!discardStateOnStop) {
            saveCurrentState();
        }
        super.onStop();
        if (helpTooltip != null) helpTooltip.dismiss();
        if (numpad != null && numpad.isShowing()) numpad.dismiss();
    }
}
