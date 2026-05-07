package com.capstone.excavator;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class DitchSideWorkSettingActivity extends ScaledAppCompatActivity {

    private View btnBack;
    private View btnHelp;
    private View btnPrev;
    private View btnNext;

    private HelpTooltip helpTooltip;
    private NumpadView numpad;

    private TextView tvWaveParam1;
    private TextView tvWaveParam2;
    private TextView tvWaveParam3;
    private TextView tvWaveParam4;

    private static final int NUMPAD_SCREEN_X = 1260;
    private static final int NUMPAD_SCREEN_Y = 278;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFullScreenMode();
        setContentView(R.layout.activity_ditch_side_work_setting);

        btnBack = findViewById(R.id.btnLevelBack);
        btnHelp = findViewById(R.id.btnLevelHelp);
        btnPrev = findViewById(R.id.btnDitchPrev);
        btnNext = findViewById(R.id.btnDitchNext);

        tvWaveParam1 = findViewById(R.id.tvWaveParam1);
        tvWaveParam2 = findViewById(R.id.tvWaveParam2);
        tvWaveParam3 = findViewById(R.id.tvWaveParam3);
        tvWaveParam4 = findViewById(R.id.tvWaveParam4);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        helpTooltip = new HelpTooltip(this, "这里是帮助提示内容，你可以在此解释横波/侧向参数的含义。");
        helpTooltip.attach(btnHelp);

        numpad = new NumpadView(this);
        setupInputs();

        if (btnPrev != null) {
            btnPrev.setOnClickListener(v -> {
                startActivity(new Intent(this, DitchWorkSettingActivity.class));
                finish();
            });
        }

        if (btnNext != null) {
            btnNext.setOnClickListener(v ->
                    startActivity(new Intent(this, DitchPrecheckActivity.class)));
        }
    }

    private void setupInputs() {
        setupOneInput(tvWaveParam1);
        setupOneInput(tvWaveParam2);
        setupOneInput(tvWaveParam3);
        setupOneInput(tvWaveParam4);
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

