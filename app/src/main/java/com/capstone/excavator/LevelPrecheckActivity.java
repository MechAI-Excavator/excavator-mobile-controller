package com.capstone.excavator;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class LevelPrecheckActivity extends ScaledAppCompatActivity {

    private View btnBack;
    private TextView btnPrev;
    private TextView btnStart;
    private TextView tvPrecheckRef;
    private TextView tvPrecheckMode;
    private TextView tvPrecheckTarget;
    private TextView tvPrecheckFillCut;
    private TextView iconPrecheckRtk;
    private TextView tvPrecheckRtkStatus;
    private TextView tvPrecheckRtkDesc;
    private TextView iconPrecheckImu;
    private TextView tvPrecheckImuStatus;
    private TextView tvPrecheckImuDesc;
    private HelpTooltip helpTooltip;
    private final RtkState.OnRtkChangeListener rtkChangeListener =
            (lat, lon, valid) -> runOnUiThread(this::refreshPrecheckInfo);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFullScreenMode();
        setContentView(R.layout.activity_level_precheck);

        btnBack = findViewById(R.id.btnPrecheckBack);
        btnPrev = findViewById(R.id.btnPrecheckPrev);
        btnStart = findViewById(R.id.btnPrecheckStart);
        tvPrecheckRef = findViewById(R.id.tvPrecheckRef);
        tvPrecheckMode = findViewById(R.id.tvPrecheckMode);
        tvPrecheckTarget = findViewById(R.id.tvPrecheckTarget);
        tvPrecheckFillCut = findViewById(R.id.tvPrecheckFillCut);
        iconPrecheckRtk = findViewById(R.id.iconPrecheckRtk);
        tvPrecheckRtkStatus = findViewById(R.id.tvPrecheckRtkStatus);
        tvPrecheckRtkDesc = findViewById(R.id.tvPrecheckRtkDesc);
        iconPrecheckImu = findViewById(R.id.iconPrecheckImu);
        tvPrecheckImuStatus = findViewById(R.id.tvPrecheckImuStatus);
        tvPrecheckImuDesc = findViewById(R.id.tvPrecheckImuDesc);

        View help = findViewById(R.id.btnLevelHelp);
        helpTooltip = new HelpTooltip(
                this,
                "这里是帮助提示内容，你可以在此解释该页面的检查项含义。"
        );
        helpTooltip.attach(help);

        if (btnBack != null) btnBack.setOnClickListener(v -> navigateToMain());
        if (btnPrev != null) btnPrev.setOnClickListener(v -> finish());
        if (btnStart != null) {
            btnStart.setOnClickListener(v -> {
                TaskTypeState.getInstance().setType(TaskTypeState.Type.LEVEL);
                WorkRunState.getInstance().setState(WorkRunState.State.RUNNING);
                Toast.makeText(this, "开始作业", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, MainActivity.class));
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        RtkState.addListener(rtkChangeListener);
        refreshPrecheckInfo();
    }

    @Override
    protected void onStop() {
        RtkState.removeListener(rtkChangeListener);
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

    private void refreshPrecheckInfo() {
        if (tvPrecheckRef != null) {
            tvPrecheckRef.setText("参考点: " + LevelTaskState.getReferencePointText());
        }
        if (tvPrecheckMode != null) {
            tvPrecheckMode.setText("目标方式: " + LevelTaskState.getModeText());
        }
        if (tvPrecheckTarget != null) {
            tvPrecheckTarget.setText(buildTargetText());
        }
        if (tvPrecheckFillCut != null) {
            tvPrecheckFillCut.setText("填挖量: " + valueOrPlaceholder(LevelTaskState.getFillCut()) + " m");
        }
        applyRtkStatus();
        applyImuStatus();
    }

    private String buildTargetText() {
        if (LevelTaskState.isHeightMode()) {
            return "目标高度: " + valueOrPlaceholder(LevelTaskState.getTargetHeight()) + " m";
        }
        return "目标坐标: 经度 " + valueOrPlaceholder(LevelTaskState.getTargetLon())
                + " / 纬度 " + valueOrPlaceholder(LevelTaskState.getTargetLat())
                + " / 高程 " + valueOrPlaceholder(LevelTaskState.getTargetZ());
    }

    private void applyRtkStatus() {
        boolean valid = RtkState.isValid();
        int color = valid ? Color.parseColor("#FF22C55E") : Color.parseColor("#FFFF6B6B");
        if (iconPrecheckRtk != null) {
            iconPrecheckRtk.setBackgroundResource(valid ? R.drawable.check_green_bg : R.drawable.check_red_bg);
            iconPrecheckRtk.setText(valid ? "✓" : "!");
        }
        if (tvPrecheckRtkStatus != null) {
            tvPrecheckRtkStatus.setText(valid ? "已连接" : "无数据");
            tvPrecheckRtkStatus.setTextColor(color);
        }
        if (tvPrecheckRtkDesc != null) {
            tvPrecheckRtkDesc.setText(valid
                    ? "当前信号良好，符合作业要求"
                    : "未获取到 RTK 数据，请检查定位状态");
        }
    }

    private void applyImuStatus() {
        int onlineCount = ImuStatusState.getOnlineCount();
        boolean allOnline = ImuStatusState.isAllOnline();
        int color = allOnline ? Color.parseColor("#FF22C55E") : Color.parseColor("#FFFF6B6B");
        if (iconPrecheckImu != null) {
            iconPrecheckImu.setBackgroundResource(allOnline ? R.drawable.check_green_bg : R.drawable.check_red_bg);
            iconPrecheckImu.setText(allOnline ? "✓" : "!");
        }
        if (tvPrecheckImuStatus != null) {
            tvPrecheckImuStatus.setText(allOnline ? "数据正常" : "IMU " + onlineCount + "/" + ImuStatusState.TOTAL_COUNT);
            tvPrecheckImuStatus.setTextColor(color);
        }
        if (tvPrecheckImuDesc != null) {
            tvPrecheckImuDesc.setText(allOnline
                    ? "IMU 数据已识别，数据正常"
                    : "IMU 数据不完整，请检查传感器状态");
        }
    }

    private static String valueOrPlaceholder(String value) {
        return value == null || value.trim().isEmpty() ? "--" : value.trim();
    }
}

