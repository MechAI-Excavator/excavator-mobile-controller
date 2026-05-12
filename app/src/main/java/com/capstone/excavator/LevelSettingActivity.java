package com.capstone.excavator;

import android.os.Bundle;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

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
public class LevelSettingActivity extends ScaledAppCompatActivity {

    // ── 参考点选择 ───────────────────────────────────────────
    private LinearLayout cardRefLeft, cardRefMiddle, cardRefRight;
    private TextView tvRefLeft, tvRefMiddle, tvRefRight;
    private int selectedRef = 1; // 0=左 1=中 2=右，默认中斗尖

    // ── 模式切换 ─────────────────────────────────────────────
    private TextView btnModeHeight, btnModeCoord;
    private boolean isHeightMode = true;
    private View panelHeightMode, panelCoordMode;

    // ── 数值输入 ─────────────────────────────────────────────
    private TextView tvTargetHeight, tvFillCut;
    private TextView tvCoordX, tvCoordY, tvCoordZ;
    private TextView tvCurrentLatLon;
    private NumpadView numpad;

    // ── 距离标注 ─────────────────────────────────────────────
    private TextView tvDepthLabel;
    private TextView tvCurrentRef;

    // ── 计算/管理状态 ─────────────────────────────────────────
    // 约定：
    // - tvTargetHeight：用户输入“铲斗到地面距离”（通常为正）
    // - tvFillCut：填挖量（通常为负，可自动计算，也允许手动覆盖）
    // - tvCurrentRef / tvDepthLabel：显示“两个数之和”（distance + fillCut）
    private boolean fillCutAuto = true;

    // ── 其他控件 ─────────────────────────────────────────────
    private View btnLevelBack, btnLevelNext;
    private View btnLevelHelp;
    private HelpTooltip helpTooltip;
    private final RtkState.OnRtkChangeListener rtkChangeListener =
            (lat, lon, valid) -> runOnUiThread(() -> updateCurrentLatLon(lat, lon, valid));

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
        restoreFromState();
        refreshDerivedViews();
        setupActions();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) setFullScreenMode();
    }

    @Override
    protected void onStart() {
        super.onStart();
        RtkState.addListener(rtkChangeListener);
        updateCurrentLatLon(RtkState.getLat(), RtkState.getLon(), RtkState.isValid());
    }

    @Override
    protected void onStop() {
        RtkState.removeListener(rtkChangeListener);
        super.onStop();
        if (helpTooltip != null) helpTooltip.dismiss();
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

        panelHeightMode = findViewById(R.id.panelHeightMode);
        panelCoordMode  = findViewById(R.id.panelCoordMode);

        tvTargetHeight = findViewById(R.id.tvTargetHeight);
        tvFillCut      = findViewById(R.id.tvFillCut);
        tvCurrentRef   = findViewById(R.id.tvCurrentRef);
        tvCoordX       = findViewById(R.id.tvCoordX);
        tvCoordY       = findViewById(R.id.tvCoordY);
        tvCoordZ       = findViewById(R.id.tvCoordZ);
        tvCurrentLatLon = findViewById(R.id.tvCurrentLatLon);
        tvDepthLabel   = findViewById(R.id.tvDepthLabel);

        btnLevelBack = findViewById(R.id.btnLevelBack);
        btnLevelNext = findViewById(R.id.btnLevelNext);
        btnLevelHelp = findViewById(R.id.btnLevelHelp);
    }

    private void updateCurrentLatLon(double lat, double lon, boolean valid) {
        if (tvCurrentLatLon == null) {
            return;
        }
        if (valid) {
            tvCurrentLatLon.setText(String.format(Locale.US, "%.9f, %.9f", lat, lon));
        } else {
            tvCurrentLatLon.setText("暂无RTK");
        }
    }

    private void initNumpad() {
        numpad = new NumpadView(this);
    }

    private void restoreFromState() {
        selectedRef = LevelTaskState.getReferencePoint();
        isHeightMode = LevelTaskState.isHeightMode();
        applyRefSelection();
        applyModeSelection();

        if (tvTargetHeight != null && !LevelTaskState.getTargetHeight().isEmpty()) {
            tvTargetHeight.setText(LevelTaskState.getTargetHeight());
        }
        if (tvFillCut != null && !LevelTaskState.getFillCut().isEmpty()) {
            tvFillCut.setText(LevelTaskState.getFillCut());
            fillCutAuto = false;
        }
        if (tvCoordX != null && !LevelTaskState.getTargetLon().isEmpty()) {
            tvCoordX.setText(LevelTaskState.getTargetLon());
        }
        if (tvCoordY != null && !LevelTaskState.getTargetLat().isEmpty()) {
            tvCoordY.setText(LevelTaskState.getTargetLat());
        }
        if (tvCoordZ != null && !LevelTaskState.getTargetZ().isEmpty()) {
            tvCoordZ.setText(LevelTaskState.getTargetZ());
        }
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
        refreshDerivedViews();
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
        refreshDerivedViews();
    }

    private void applyModeSelection() {
        if (isHeightMode) {
            btnModeHeight.setBackground(getDrawable(R.drawable.level_mode_selected_bg));
            btnModeHeight.setTextColor(getColor(R.color.level_selected));
            btnModeHeight.setTypeface(null, android.graphics.Typeface.BOLD);

            btnModeCoord.setBackground(null);
            btnModeCoord.setTextColor(getColor(R.color.level_unselected));
            btnModeCoord.setTypeface(null, android.graphics.Typeface.NORMAL);

            if (panelHeightMode != null) panelHeightMode.setVisibility(View.VISIBLE);
            if (panelCoordMode != null) panelCoordMode.setVisibility(View.GONE);
        } else {
            btnModeCoord.setBackground(getDrawable(R.drawable.level_mode_selected_bg));
            btnModeCoord.setTextColor(getColor(R.color.level_selected));
            btnModeCoord.setTypeface(null, android.graphics.Typeface.BOLD);

            btnModeHeight.setBackground(null);
            btnModeHeight.setTextColor(getColor(R.color.level_unselected));
            btnModeHeight.setTypeface(null, android.graphics.Typeface.NORMAL);

            if (panelHeightMode != null) panelHeightMode.setVisibility(View.GONE);
            if (panelCoordMode != null) panelCoordMode.setVisibility(View.VISIBLE);
        }
    }

    // ── Numpad 输入 ───────────────────────────────────────────

    private void setupInputs() {
        tvTargetHeight.setOnClickListener(v -> {
            if (numpad.isShowing()) { numpad.dismiss(); return; }
            numpad.setOnConfirmListener(value -> {
                tvTargetHeight.setText(value);
                // 只要用户修改“距离”，默认恢复自动计算填挖量
                fillCutAuto = true;
                refreshDerivedViews();
            });
            numpad.showForAtScreen(tvTargetHeight, tvTargetHeight,
                    NumpadPositionConfig.SCREEN_X, NumpadPositionConfig.SCREEN_Y);
        });

        tvFillCut.setOnClickListener(v -> {
            if (numpad.isShowing()) { numpad.dismiss(); return; }
            numpad.setOnConfirmListener(value -> {
                tvFillCut.setText(value);
                fillCutAuto = false; // 手动覆盖
                refreshDerivedViews();
            });
            numpad.showForAtScreen(tvFillCut, tvFillCut,
                    NumpadPositionConfig.SCREEN_X, NumpadPositionConfig.SCREEN_Y);
        });

        tvCoordX.setOnClickListener(v -> {
            if (numpad.isShowing()) { numpad.dismiss(); return; }
            numpad.setOnConfirmListener(value -> tvCoordX.setText(value));
            numpad.showForAtScreen(tvCoordX, tvCoordX,
                    NumpadPositionConfig.SCREEN_X, NumpadPositionConfig.SCREEN_Y);
        });

        tvCoordY.setOnClickListener(v -> {
            if (numpad.isShowing()) { numpad.dismiss(); return; }
            numpad.setOnConfirmListener(value -> tvCoordY.setText(value));
            numpad.showForAtScreen(tvCoordY, tvCoordY,
                    NumpadPositionConfig.SCREEN_X, NumpadPositionConfig.SCREEN_Y);
        });

        tvCoordZ.setOnClickListener(v -> {
            if (numpad.isShowing()) { numpad.dismiss(); return; }
            numpad.setOnConfirmListener(value -> {
                tvCoordZ.setText(value);
                refreshDerivedViews();
            });
            numpad.showForAtScreen(tvCoordZ, tvCoordZ,
                    NumpadPositionConfig.SCREEN_X, NumpadPositionConfig.SCREEN_Y);
        });
    }

    private void refreshDerivedViews() {
        if (tvTargetHeight == null || tvFillCut == null || tvCurrentRef == null || tvDepthLabel == null) {
            return;
        }

        Double distance = parseDoubleOrNull(tvTargetHeight.getText());
        Double fillCut = parseDoubleOrNull(tvFillCut.getText());

        if (fillCutAuto) {
            // 当前工程内没有更上游的传感器/协议计算逻辑，这里给一个稳定且可替换的默认策略：
            // 让填挖量保持“通常为负”的期望，并随着距离变化而变化。
            if (distance != null) {
                fillCut = -Math.abs(distance);
                tvFillCut.setText(formatMetersValue(fillCut));
            }
        }

        if (distance == null || fillCut == null) {
            tvCurrentRef.setText("--");
            tvDepthLabel.setText("-- m");
            cacheState();
            return;
        }

        double sum = distance + fillCut;
        String sumText = formatMetersValue(sum);
        tvCurrentRef.setText(sumText);

        // 预览叠加层显示同一个“和”的数值（带单位）
        tvDepthLabel.setText(sumText + " m");
        tvDepthLabel.setTextColor(sum < 0 ? Color.parseColor("#FFEF4444") : Color.parseColor("#FF22C55E"));

        cacheState();
    }

    private void cacheState() {
        // 页面内随改随存，保证下一页/返回时状态一致
        LevelTaskState.update(
                selectedRef,
                isHeightMode,
                tvTargetHeight == null ? "" : tvTargetHeight.getText().toString(),
                tvFillCut == null ? "" : tvFillCut.getText().toString(),
                tvCoordX == null ? "" : tvCoordX.getText().toString(),
                tvCoordY == null ? "" : tvCoordY.getText().toString(),
                tvCoordZ == null ? "" : tvCoordZ.getText().toString()
        );
    }

    private static Double parseDoubleOrNull(CharSequence text) {
        if (text == null) return null;
        String s = text.toString().trim();
        if (s.isEmpty() || s.equals("--")) return null;
        // 兼容 “−” (U+2212) 负号
        s = s.replace('−', '-');
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String formatMetersValue(double v) {
        // 使用 2 位小数，且把普通 '-' 替换为更像 UI 的 “−”
        String s = String.format(Locale.US, "%.2f", v);
        return s.startsWith("-") ? "−" + s.substring(1) : s;
    }

    // ── 按钮动作 ──────────────────────────────────────────────

    private void setupActions() {
        btnLevelBack.setOnClickListener(v -> navigateToMain());

        helpTooltip = new HelpTooltip(
                this,
                "这里是帮助提示内容，你可以在此解释该页面的参数含义。"
        );
        helpTooltip.attach(btnLevelHelp);

        btnLevelNext.setOnClickListener(v -> {
            String[] refNames = {"左斗尖", "中斗尖", "右斗尖"};
            String ref    = refNames[selectedRef];
            String mode   = isHeightMode ? "高度定点" : "坐标定点";
            String height = tvTargetHeight.getText().toString();
            String fill   = tvFillCut.getText().toString();
            LevelTaskState.update(
                    selectedRef,
                    isHeightMode,
                    height,
                    fill,
                    tvCoordX.getText().toString(),
                    tvCoordY.getText().toString(),
                    tvCoordZ.getText().toString()
            );

            Toast.makeText(this,
                    "参考点：" + ref + "  模式：" + mode +
                    "\n目标高度：" + height + " m  填挖量：" + fill + " m",
                    Toast.LENGTH_LONG).show();

            startActivity(new Intent(this, LevelPrecheckActivity.class));
        });
    }

}
