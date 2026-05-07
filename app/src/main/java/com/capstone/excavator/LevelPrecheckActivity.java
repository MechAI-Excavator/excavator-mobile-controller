package com.capstone.excavator;

import android.content.Intent;
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
    private HelpTooltip helpTooltip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFullScreenMode();
        setContentView(R.layout.activity_level_precheck);

        btnBack = findViewById(R.id.btnPrecheckBack);
        btnPrev = findViewById(R.id.btnPrecheckPrev);
        btnStart = findViewById(R.id.btnPrecheckStart);

        View help = findViewById(R.id.btnLevelHelp);
        helpTooltip = new HelpTooltip(
                this,
                "这里是帮助提示内容，你可以在此解释该页面的检查项含义。"
        );
        helpTooltip.attach(help);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
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
    protected void onStop() {
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

