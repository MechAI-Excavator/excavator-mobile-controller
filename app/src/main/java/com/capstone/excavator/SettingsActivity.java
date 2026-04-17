package com.capstone.excavator;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * 设置页面 - 视频流地址、3D 大臂/小臂长度比例、IMU 数值配置
 */
public class SettingsActivity extends AppCompatActivity {

    private static final float MIN_ARM_SCALE = 0.1f;
    private static final float MAX_ARM_SCALE = 10f;

    private EditText etSettingsVideoUrl;
    private EditText etArmBoomScale;
    private EditText etArmStickScale;
    private android.view.View btnSaveVideoUrl;
    private TextView btnBack;

    // IMU preview labels on the card
    private TextView tvImuBoomOffsetPreview;
    private TextView tvImuStickOffsetPreview;
    private TextView tvImuBucketOffsetPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFullScreenMode();
        setContentView(R.layout.activity_settings);

        etSettingsVideoUrl = findViewById(R.id.etSettingsVideoUrl);
        etArmBoomScale     = findViewById(R.id.etArmBoomScale);
        etArmStickScale    = findViewById(R.id.etArmStickScale);
        btnSaveVideoUrl    = findViewById(R.id.btnSaveVideoUrl);
        btnBack            = findViewById(R.id.btnBack);

        tvImuBoomOffsetPreview   = findViewById(R.id.tvImuBoomOffsetPreview);
        tvImuStickOffsetPreview  = findViewById(R.id.tvImuStickOffsetPreview);
        tvImuBucketOffsetPreview = findViewById(R.id.tvImuBucketOffsetPreview);

        // 加载当前视频地址
        String currentUrl = getIntent().getStringExtra("current_url");
        if (currentUrl != null && !currentUrl.isEmpty()) {
            etSettingsVideoUrl.setText(currentUrl);
        }

        float boom  = ArmLengthPreferences.getBoomScale(this);
        float stick = ArmLengthPreferences.getStickScale(this);
        etArmBoomScale.setText(formatScaleForEdit(boom));
        etArmStickScale.setText(formatScaleForEdit(stick));

        refreshImuPreviewLabels();

        btnBack.setOnClickListener(v -> finish());

        btnSaveVideoUrl.setOnClickListener(v -> {
            String newUrl = etSettingsVideoUrl.getText().toString().trim();

            float boomScale  = parseScale(etArmBoomScale.getText().toString(),  ArmLengthPreferences.DEFAULT_SCALE);
            float stickScale = parseScale(etArmStickScale.getText().toString(), ArmLengthPreferences.DEFAULT_SCALE);

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

            Toast.makeText(this, newUrl.isEmpty() ? "臂长比例已保存" : "已保存并应用", Toast.LENGTH_SHORT).show();
            finish();
        });

        // IMU 配置入口
        Button btnOpenImuDialog = findViewById(R.id.btnOpenImuDialog);
        if (btnOpenImuDialog != null) {
            btnOpenImuDialog.setOnClickListener(v -> showImuSettingsDialog());
        }
    }

    // ── IMU preview ──────────────────────────────────────────────────────────

    private void refreshImuPreviewLabels() {
        ImuPreferences.Params p = ImuPreferences.load(this);
        String fmt = "%.2f°";
        if (tvImuBoomOffsetPreview != null)
            tvImuBoomOffsetPreview.setText(String.format(Locale.US, fmt, p.boomImuOffsetDeg));
        if (tvImuStickOffsetPreview != null)
            tvImuStickOffsetPreview.setText(String.format(Locale.US, fmt, p.stickImuOffsetDeg));
        if (tvImuBucketOffsetPreview != null)
            tvImuBucketOffsetPreview.setText(String.format(Locale.US, fmt, p.bucketImuOffsetDeg));
    }

    // ── IMU Dialog ───────────────────────────────────────────────────────────

    private void showImuSettingsDialog() {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_imu_settings);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels  * 0.88f),
                    (int) (getResources().getDisplayMetrics().heightPixels * 0.82f)
            );
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Load joint diagram from assets
        ImageView ivDiagram = dialog.findViewById(R.id.ivJointDiagram);
        try {
            InputStream is = getAssets().open("images/preview.jpg");
            Bitmap bmp = BitmapFactory.decodeStream(is);
            ivDiagram.setImageBitmap(bmp);
            is.close();
        } catch (IOException e) {
            ivDiagram.setVisibility(View.GONE);
        }

        // Load current params
        ImuPreferences.Params p = ImuPreferences.load(this);

        // ── Bind all field rows ──
        FieldRow rowBoomOffset      = new FieldRow(dialog, R.id.rowBoomOffset,      "大臂 IMU 偏移角 (°)",  ImuPreferences.fmt(p.boomImuOffsetDeg));
        FieldRow rowStickOffset     = new FieldRow(dialog, R.id.rowStickOffset,     "小臂 IMU 偏移角 (°)",  ImuPreferences.fmt(p.stickImuOffsetDeg));
        FieldRow rowBucketOffset    = new FieldRow(dialog, R.id.rowBucketOffset,    "铲斗 IMU 偏移角 (°)",  ImuPreferences.fmt(p.bucketImuOffsetDeg));

        FieldRow rowBoomLength      = new FieldRow(dialog, R.id.rowBoomLength,      "大臂长度",              ImuPreferences.fmt(p.boomLength));
        FieldRow rowStickLength     = new FieldRow(dialog, R.id.rowStickLength,     "小臂长度",              ImuPreferences.fmt(p.stickLength));
        FieldRow rowBucketLength    = new FieldRow(dialog, R.id.rowBucketLength,    "铲斗长度",              ImuPreferences.fmt(p.bucketLength));
        FieldRow rowBucketAO        = new FieldRow(dialog, R.id.rowBucketAngleOffset,"铲斗角偏移 (°)",       ImuPreferences.fmt(p.bucketAngleOffsetDeg));

        FieldRow rowBoomL2 = new FieldRow(dialog, R.id.rowBoomL2, "boomL2", ImuPreferences.fmt(p.boomL2));
        FieldRow rowBoomL3 = new FieldRow(dialog, R.id.rowBoomL3, "boomL3", ImuPreferences.fmt(p.boomL3));
        FieldRow rowBoomL4 = new FieldRow(dialog, R.id.rowBoomL4, "boomL4", ImuPreferences.fmt(p.boomL4));
        FieldRow rowBoomL5 = new FieldRow(dialog, R.id.rowBoomL5, "boomL5", ImuPreferences.fmt(p.boomL5));
        FieldRow rowBoomL6 = new FieldRow(dialog, R.id.rowBoomL6, "boomL6", ImuPreferences.fmt(p.boomL6));
        FieldRow rowBoomL7 = new FieldRow(dialog, R.id.rowBoomL7, "boomL7", ImuPreferences.fmt(p.boomL7));

        FieldRow rowStickL2 = new FieldRow(dialog, R.id.rowStickL2, "stickL2", ImuPreferences.fmt(p.stickL2));
        FieldRow rowStickL3 = new FieldRow(dialog, R.id.rowStickL3, "stickL3", ImuPreferences.fmt(p.stickL3));
        FieldRow rowStickL4 = new FieldRow(dialog, R.id.rowStickL4, "stickL4", ImuPreferences.fmt(p.stickL4));
        FieldRow rowStickL5 = new FieldRow(dialog, R.id.rowStickL5, "stickL5", ImuPreferences.fmt(p.stickL5));
        FieldRow rowStickL6 = new FieldRow(dialog, R.id.rowStickL6, "stickL6", ImuPreferences.fmt(p.stickL6));
        FieldRow rowStickL7 = new FieldRow(dialog, R.id.rowStickL7, "stickL7", ImuPreferences.fmt(p.stickL7));

        FieldRow rowBucketL2  = new FieldRow(dialog, R.id.rowBucketL2,  "bucketL2",  ImuPreferences.fmt(p.bucketL2));
        FieldRow rowBucketL3  = new FieldRow(dialog, R.id.rowBucketL3,  "bucketL3",  ImuPreferences.fmt(p.bucketL3));
        FieldRow rowBucketL4  = new FieldRow(dialog, R.id.rowBucketL4,  "bucketL4",  ImuPreferences.fmt(p.bucketL4));
        FieldRow rowBucketL5  = new FieldRow(dialog, R.id.rowBucketL5,  "bucketL5",  ImuPreferences.fmt(p.bucketL5));
        FieldRow rowBucketL6  = new FieldRow(dialog, R.id.rowBucketL6,  "bucketL6",  ImuPreferences.fmt(p.bucketL6));
        FieldRow rowBucketL7  = new FieldRow(dialog, R.id.rowBucketL7,  "bucketL7",  ImuPreferences.fmt(p.bucketL7));
        FieldRow rowBucketL9  = new FieldRow(dialog, R.id.rowBucketL9,  "bucketL9",  ImuPreferences.fmt(p.bucketL9));
        FieldRow rowBucketL10 = new FieldRow(dialog, R.id.rowBucketL10, "bucketL10", ImuPreferences.fmt(p.bucketL10));

        // ── Buttons ──
        dialog.findViewById(R.id.btnImuDialogCancel).setOnClickListener(v -> dialog.dismiss());

        dialog.findViewById(R.id.btnImuDialogReset).setOnClickListener(v -> {
            ImuPreferences.Params def = new ImuPreferences.Params(); // all defaults
            rowBoomOffset.set(ImuPreferences.fmt(def.boomImuOffsetDeg));
            rowStickOffset.set(ImuPreferences.fmt(def.stickImuOffsetDeg));
            rowBucketOffset.set(ImuPreferences.fmt(def.bucketImuOffsetDeg));
            rowBoomLength.set(ImuPreferences.fmt(def.boomLength));
            rowStickLength.set(ImuPreferences.fmt(def.stickLength));
            rowBucketLength.set(ImuPreferences.fmt(def.bucketLength));
            rowBucketAO.set(ImuPreferences.fmt(def.bucketAngleOffsetDeg));
            rowBoomL2.set(ImuPreferences.fmt(def.boomL2));
            rowBoomL3.set(ImuPreferences.fmt(def.boomL3));
            rowBoomL4.set(ImuPreferences.fmt(def.boomL4));
            rowBoomL5.set(ImuPreferences.fmt(def.boomL5));
            rowBoomL6.set(ImuPreferences.fmt(def.boomL6));
            rowBoomL7.set(ImuPreferences.fmt(def.boomL7));
            rowStickL2.set(ImuPreferences.fmt(def.stickL2));
            rowStickL3.set(ImuPreferences.fmt(def.stickL3));
            rowStickL4.set(ImuPreferences.fmt(def.stickL4));
            rowStickL5.set(ImuPreferences.fmt(def.stickL5));
            rowStickL6.set(ImuPreferences.fmt(def.stickL6));
            rowStickL7.set(ImuPreferences.fmt(def.stickL7));
            rowBucketL2.set(ImuPreferences.fmt(def.bucketL2));
            rowBucketL3.set(ImuPreferences.fmt(def.bucketL3));
            rowBucketL4.set(ImuPreferences.fmt(def.bucketL4));
            rowBucketL5.set(ImuPreferences.fmt(def.bucketL5));
            rowBucketL6.set(ImuPreferences.fmt(def.bucketL6));
            rowBucketL7.set(ImuPreferences.fmt(def.bucketL7));
            rowBucketL9.set(ImuPreferences.fmt(def.bucketL9));
            rowBucketL10.set(ImuPreferences.fmt(def.bucketL10));
            Toast.makeText(this, "已填入默认值，点击[保存并应用]生效", Toast.LENGTH_SHORT).show();
        });

        dialog.findViewById(R.id.btnImuDialogSave).setOnClickListener(v -> {
            ImuPreferences.Params np = new ImuPreferences.Params();

            np.boomImuOffsetDeg   = rowBoomOffset.get(p.boomImuOffsetDeg);
            np.stickImuOffsetDeg  = rowStickOffset.get(p.stickImuOffsetDeg);
            np.bucketImuOffsetDeg = rowBucketOffset.get(p.bucketImuOffsetDeg);

            np.boomLength           = rowBoomLength.get(p.boomLength);
            np.stickLength          = rowStickLength.get(p.stickLength);
            np.bucketLength         = rowBucketLength.get(p.bucketLength);
            np.bucketAngleOffsetDeg = rowBucketAO.get(p.bucketAngleOffsetDeg);

            np.boomL2 = rowBoomL2.get(p.boomL2);
            np.boomL3 = rowBoomL3.get(p.boomL3);
            np.boomL4 = rowBoomL4.get(p.boomL4);
            np.boomL5 = rowBoomL5.get(p.boomL5);
            np.boomL6 = rowBoomL6.get(p.boomL6);
            np.boomL7 = rowBoomL7.get(p.boomL7);

            np.stickL2 = rowStickL2.get(p.stickL2);
            np.stickL3 = rowStickL3.get(p.stickL3);
            np.stickL4 = rowStickL4.get(p.stickL4);
            np.stickL5 = rowStickL5.get(p.stickL5);
            np.stickL6 = rowStickL6.get(p.stickL6);
            np.stickL7 = rowStickL7.get(p.stickL7);

            np.bucketL2  = rowBucketL2.get(p.bucketL2);
            np.bucketL3  = rowBucketL3.get(p.bucketL3);
            np.bucketL4  = rowBucketL4.get(p.bucketL4);
            np.bucketL5  = rowBucketL5.get(p.bucketL5);
            np.bucketL6  = rowBucketL6.get(p.bucketL6);
            np.bucketL7  = rowBucketL7.get(p.bucketL7);
            np.bucketL9  = rowBucketL9.get(p.bucketL9);
            np.bucketL10 = rowBucketL10.get(p.bucketL10);

            ImuPreferences.save(this, np);
            refreshImuPreviewLabels();

            // Signal MainActivity to re-apply IMU config
            Intent result = new Intent();
            result.putExtra("imu_config_updated", true);
            setResult(RESULT_OK, result);

            Toast.makeText(this, "IMU 参数已保存并应用", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    // ── FieldRow helper ──────────────────────────────────────────────────────

    /** Wraps a single label+EditText row inflated via <include>. */
    private static final class FieldRow {
        private final EditText editText;

        FieldRow(Dialog dialog, int rowId, String label, String value) {
            LinearLayout row = dialog.findViewById(rowId);
            TextView tv = row.findViewById(R.id.tvFieldLabel);
            editText    = row.findViewById(R.id.etFieldValue);
            tv.setText(label);
            editText.setText(value);
        }

        void set(String value) {
            editText.setText(value);
        }

        double get(double fallback) {
            return ImuPreferences.parseOrDefault(editText.getText().toString(), fallback);
        }
    }

    // ── Shared helpers ───────────────────────────────────────────────────────

    private static String formatScaleForEdit(float v) {
        if (Math.abs(v - Math.round(v)) < 1e-6f) {
            return String.valueOf(Math.round(v));
        }
        return String.format(Locale.US, "%.4g", v);
    }

    private static float parseScale(String s, float defaultValue) {
        if (s == null) return defaultValue;
        String t = s.trim().replace(',', '.');
        if (t.isEmpty()) return defaultValue;
        try {
            float v = Float.parseFloat(t);
            return Float.isFinite(v) ? v : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
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

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) setFullScreenMode();
    }
}
