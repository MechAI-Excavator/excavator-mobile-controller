package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.util.Locale;

/**
 * 设置页面 - 视频流地址、3D 大臂/小臂长度比例（持久化 + 下发 WebView）
 */
public class SettingsActivity extends AppCompatActivity {

    private static final float MIN_ARM_SCALE = 0.1f;
    private static final float MAX_ARM_SCALE = 10f;

    private EditText etSettingsVideoUrl;
    private EditText etArmBoomScale;
    private EditText etArmStickScale;
    private Button btnSaveVideoUrl;
    private TextView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 全屏模式
        setFullScreenMode();

        setContentView(R.layout.activity_settings);

        etSettingsVideoUrl = findViewById(R.id.etSettingsVideoUrl);
        etArmBoomScale = findViewById(R.id.etArmBoomScale);
        etArmStickScale = findViewById(R.id.etArmStickScale);
        btnSaveVideoUrl = findViewById(R.id.btnSaveVideoUrl);
        btnBack = findViewById(R.id.btnBack);

        // 获取当前视频地址并显示
        String currentUrl = getIntent().getStringExtra("current_url");
        if (currentUrl != null && !currentUrl.isEmpty()) {
            etSettingsVideoUrl.setText(currentUrl);
        }

        float boom = ArmLengthPreferences.getBoomScale(this);
        float stick = ArmLengthPreferences.getStickScale(this);
        etArmBoomScale.setText(formatScaleForEdit(boom));
        etArmStickScale.setText(formatScaleForEdit(stick));

        // 返回按钮 — 不写回，仅关闭
        btnBack.setOnClickListener(v -> finish());

        // 保存：地址 + 臂长比例
        btnSaveVideoUrl.setOnClickListener(v -> {
            String newUrl = etSettingsVideoUrl.getText().toString().trim();

            float boomScale = parseScale(etArmBoomScale.getText().toString(),
                    ArmLengthPreferences.DEFAULT_SCALE);
            float stickScale = parseScale(etArmStickScale.getText().toString(),
                    ArmLengthPreferences.DEFAULT_SCALE);

            if (boomScale < MIN_ARM_SCALE || boomScale > MAX_ARM_SCALE
                    || stickScale < MIN_ARM_SCALE || stickScale > MAX_ARM_SCALE) {
                Toast.makeText(this,
                        "大臂、小臂比例须在 " + MIN_ARM_SCALE + "～" + MAX_ARM_SCALE + " 之间",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            ArmLengthPreferences.save(this, boomScale, stickScale);

            Intent resultIntent = new Intent();
            if (!newUrl.isEmpty()) {
                resultIntent.putExtra("video_url", newUrl);
            }
            resultIntent.putExtra("arm_boom_scale", boomScale);
            resultIntent.putExtra("arm_stick_scale", stickScale);
            setResult(RESULT_OK, resultIntent);

            if (newUrl.isEmpty()) {
                Toast.makeText(this, "臂长比例已保存（视频地址未修改）", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "已保存并应用", Toast.LENGTH_SHORT).show();
            }
            finish();
        });
    }

    private static String formatScaleForEdit(float v) {
        if (Math.abs(v - Math.round(v)) < 1e-6f) {
            return String.valueOf(Math.round(v));
        }
        return String.format(Locale.US, "%.4g", v);
    }

    private static float parseScale(String s, float defaultValue) {
        if (s == null) {
            return defaultValue;
        }
        String t = s.trim().replace(',', '.');
        if (t.isEmpty()) {
            return defaultValue;
        }
        try {
            float v = Float.parseFloat(t);
            return Float.isFinite(v) ? v : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 设置全屏模式（隐藏状态栏和导航栏）
     */
    private void setFullScreenMode() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.hide(WindowInsetsCompat.Type.systemBars());
            controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            setFullScreenMode();
        }
    }
}
