package com.capstone.excavator;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * 找平作业设置页。
 *
 * Step1：选择参考点（左斗尖 / 中斗尖 / 右斗尖）
 * Step2：高度定点 / 坐标定点 模式切换 + 输入目标高度 & 填挖量（NumpadView）
 * 右侧：挖机预览图 + 下一步按钮
 */
public class LevelSettingActivity extends AppCompatActivity {

    // ── 参考点选择 ───────────────────────────────────────────
    private LinearLayout cardRefLeft, cardRefMiddle, cardRefRight;
    private TextView tvRefLeft, tvRefMiddle, tvRefRight;
    private int selectedRef = 1; // 0=左 1=中 2=右，默认中斗尖

    // ── 模式切换 ─────────────────────────────────────────────
    private TextView btnModeHeight, btnModeCoord;
    private boolean isHeightMode = true;

    // ── 数值输入 ─────────────────────────────────────────────
    private TextView tvTargetHeight, tvFillCut;
    private NumpadView numpad;

    // ── 距离标注 ─────────────────────────────────────────────
    private TextView tvDepthLabel;

    // ── 其他控件 ─────────────────────────────────────────────
    private View btnLevelBack, btnLevelNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFullScreenMode();
        setContentView(R.layout.activity_level_setting);

        bindViews();
        initNumpad();
        setupRefCards();
        setupModeToggle();
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
        cardRefLeft   = findViewById(R.id.cardRefLeft);
        cardRefMiddle = findViewById(R.id.cardRefMiddle);
        cardRefRight  = findViewById(R.id.cardRefRight);

        tvRefLeft   = findViewById(R.id.tvRefLeft);
        tvRefMiddle = findViewById(R.id.tvRefMiddle);
        tvRefRight  = findViewById(R.id.tvRefRight);

        btnModeHeight = findViewById(R.id.btnModeHeight);
        btnModeCoord  = findViewById(R.id.btnModeCoord);

        tvTargetHeight = findViewById(R.id.tvTargetHeight);
        tvFillCut      = findViewById(R.id.tvFillCut);
        tvDepthLabel   = findViewById(R.id.tvDepthLabel);

        btnLevelBack = findViewById(R.id.btnLevelBack);
        btnLevelNext = findViewById(R.id.btnLevelNext);
    }

    private void initNumpad() {
        numpad = new NumpadView(this);
    }

    // ── 参考点卡片 ────────────────────────────────────────────

    private void setupRefCards() {
        cardRefLeft.setOnClickListener(v -> selectRef(0));
        cardRefMiddle.setOnClickListener(v -> selectRef(1));
        cardRefRight.setOnClickListener(v -> selectRef(2));
        applyRefSelection();
    }

    private void selectRef(int index) {
        selectedRef = index;
        applyRefSelection();
    }

    private void applyRefSelection() {
        cardRefLeft.setBackground(getDrawable(selectedRef == 0
                ? R.drawable.level_card_selected_bg : R.drawable.level_card_normal_bg));
        tvRefLeft.setTextColor(getColor(selectedRef == 0 ? R.color.level_selected : R.color.level_unselected));

        cardRefMiddle.setBackground(getDrawable(selectedRef == 1
                ? R.drawable.level_card_selected_bg : R.drawable.level_card_normal_bg));
        tvRefMiddle.setTextColor(getColor(selectedRef == 1 ? R.color.level_selected : R.color.level_unselected));

        cardRefRight.setBackground(getDrawable(selectedRef == 2
                ? R.drawable.level_card_selected_bg : R.drawable.level_card_normal_bg));
        tvRefRight.setTextColor(getColor(selectedRef == 2 ? R.color.level_selected : R.color.level_unselected));
    }

    // ── 模式切换 ──────────────────────────────────────────────

    private void setupModeToggle() {
        btnModeHeight.setOnClickListener(v -> setMode(true));
        btnModeCoord.setOnClickListener(v -> setMode(false));
        applyModeSelection();
    }

    private void setMode(boolean heightMode) {
        isHeightMode = heightMode;
        applyModeSelection();
    }

    private void applyModeSelection() {
        if (isHeightMode) {
            btnModeHeight.setBackground(getDrawable(R.drawable.level_mode_selected_bg));
            btnModeHeight.setTextColor(getColor(R.color.level_selected));
            btnModeHeight.setTypeface(null, android.graphics.Typeface.BOLD);

            btnModeCoord.setBackground(null);
            btnModeCoord.setTextColor(getColor(R.color.level_unselected));
            btnModeCoord.setTypeface(null, android.graphics.Typeface.NORMAL);
        } else {
            btnModeCoord.setBackground(getDrawable(R.drawable.level_mode_selected_bg));
            btnModeCoord.setTextColor(getColor(R.color.level_selected));
            btnModeCoord.setTypeface(null, android.graphics.Typeface.BOLD);

            btnModeHeight.setBackground(null);
            btnModeHeight.setTextColor(getColor(R.color.level_unselected));
            btnModeHeight.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }

    // ── Numpad 输入 ───────────────────────────────────────────

    private void setupInputs() {
        tvTargetHeight.setOnClickListener(v -> {
            if (numpad.isShowing()) { numpad.dismiss(); return; }
            numpad.setOnConfirmListener(value -> tvTargetHeight.setText(value));
            numpad.showFor(tvTargetHeight, tvTargetHeight, NumpadView.POSITION_ABOVE);
        });

        tvFillCut.setOnClickListener(v -> {
            if (numpad.isShowing()) { numpad.dismiss(); return; }
            numpad.setOnConfirmListener(value -> tvFillCut.setText(value));
            numpad.showFor(tvFillCut, tvFillCut, NumpadView.POSITION_ABOVE);
        });
    }

    // ── 按钮动作 ──────────────────────────────────────────────

    private void setupActions() {
        btnLevelBack.setOnClickListener(v -> finish());

        btnLevelNext.setOnClickListener(v -> {
            String[] refNames = {"左斗尖", "中斗尖", "右斗尖"};
            String ref    = refNames[selectedRef];
            String mode   = isHeightMode ? "高度定点" : "坐标定点";
            String height = tvTargetHeight.getText().toString();
            String fill   = tvFillCut.getText().toString();

            Toast.makeText(this,
                    "参考点：" + ref + "  模式：" + mode +
                    "\n目标高度：" + height + " m  填挖量：" + fill + " m",
                    Toast.LENGTH_LONG).show();
        });
    }
}
