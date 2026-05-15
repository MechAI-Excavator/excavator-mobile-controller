package com.capstone.excavator;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

/**
 * 机械臂姿态卡片组件。
 *
 * 包含：
 *  - 顶部标题 + 2D/3D 切换胶囊
 *  - ExcavatorPostureView（WebView 渲染机械臂示意图）
 *  - 大臂 / 斗杆 / 铲斗 实时角度三行文字
 *
 * Public API（与 ExcavatorPostureView 对齐，供 MainActivity 直接调用）：
 *   setAngles(cabinPitch, cabinRoll, boom, stick, bucket)
 *   setBucketAngleOffsetDeg(offset)
 *   setLengthScales(boom, stick)
 *   setArmLengthsFromMm(boomMm, stickMm, bucketMm)
 *
 * XML 用法：
 *   <com.capstone.excavator.PostureCardView
 *       android:id="@+id/postureCardView"
 *       android:layout_width="165dp"
 *       android:layout_height="wrap_content" />
 */
public class PostureCardView extends LinearLayout {

    private ExcavatorPostureView excavatorPosture2DView;
    private ExcavatorPostureView excavatorPostureView;

    // 底部三行角度文字
    private TextView tvBoom;
    private TextView tvStick;
    private TextView tvBucket;

    // 2D / 3D Web 页面容器
    private View    view2D;
    private View    view3D;

    private TextView btn2D;
    private TextView btn3D;

    private boolean is3D = false;

    // ── Constructors ─────────────────────────────────────────────────

    public PostureCardView(Context context) {
        this(context, null);
    }

    public PostureCardView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PostureCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);
        // Padding is applied in XML to specific rows so the 2D/3D canvas can be edge-to-edge.
        setPadding(0, 0, 0, 0);

        setBackground(context.getResources().getDrawable(R.drawable.map_card_bg, context.getTheme()));
        setClipToOutline(true);
        setOutlineProvider(android.view.ViewOutlineProvider.BACKGROUND);

        inflate(context, R.layout.view_posture_card, this);

        excavatorPosture2DView = findViewById(R.id.excavatorPosture2DView);
        excavatorPostureView = findViewById(R.id.excavatorPostureView);
//        tvBoom               = findViewById(R.id.tvPostureBoomAngle);
//        tvStick              = findViewById(R.id.tvPostureStickAngle);
//        tvBucket             = findViewById(R.id.tvPostureBucketAngle);
        btn2D                = findViewById(R.id.btn2D);
        btn3D                = findViewById(R.id.btn3D);

        view2D           = findViewById(R.id.view2D);
        view3D           = findViewById(R.id.view3D);

        // ExcavatorPostureView 已嵌入卡片，不需要自己的卡片背景和圆角裁切
        if (excavatorPosture2DView != null) {
            excavatorPosture2DView.setEmbedded(true);
            excavatorPosture2DView.setDisplayMode(false);
        }
        if (excavatorPostureView != null) {
            excavatorPostureView.setEmbedded(true);
            excavatorPostureView.setDisplayMode(true);
        }

        // 初始状态：2D 可见，3D 隐藏；同步把 3D WebView 真正挂起，避免 three.js
        // requestAnimationFrame 在 GONE 状态下仍以 60Hz 抢 GPU，与 FPV TextureView 抢 vsync。
        applyModeVisibility();
        setupToggle();
    }

    // ── Public API ───────────────────────────────────────────────────

    /**
     * 更新全部角度（5 参数版，与 ExcavatorPostureView.setAngles 对齐）。
     * 仅向当前可见的 WebView 下发 payload；隐藏的那一个已经被 {@link #applyModeVisibility} 调用 pause()，
     * 这里再额外跳过一次 evaluateJavascript，避免 1Hz 节拍的无效唤醒。
     */
    public void setAngles(float cabinPitch, float cabinRoll,
                          float boom, float stick, float bucket) {
        if (is3D) {
            if (excavatorPostureView != null) {
                excavatorPostureView.setAngles(cabinPitch, cabinRoll, boom, stick, bucket);
            }
        } else {
            if (excavatorPosture2DView != null) {
                excavatorPosture2DView.setAngles(cabinPitch, cabinRoll, boom, stick, bucket);
            }
        }
        updateAngleText(boom, stick, bucket);
    }

    public void setBucketAngleOffsetDeg(float offsetDeg) {
        if (excavatorPostureView != null) {
            excavatorPostureView.setBucketAngleOffsetDeg(offsetDeg);
        }
        if (excavatorPosture2DView != null) {
            excavatorPosture2DView.setBucketAngleOffsetDeg(offsetDeg);
        }
    }

    public void setLengthScales(float boomScale, float stickScale) {
        if (excavatorPostureView != null) {
            excavatorPostureView.setLengthScales(boomScale, stickScale);
        }
        if (excavatorPosture2DView != null) {
            excavatorPosture2DView.setLengthScales(boomScale, stickScale);
        }
    }

    public void setArmLengthsFromMm(double boomMm, double stickMm, double bucketMm) {
        if (excavatorPostureView != null) {
            excavatorPostureView.setArmLengthsFromMm(boomMm, stickMm, bucketMm);
        }
        if (excavatorPosture2DView != null) {
            excavatorPosture2DView.setArmLengthsFromMm(boomMm, stickMm, bucketMm);
        }
    }

    // ── Internal ────────────────────────────────────────────────────

    private void updateAngleText(float boom, float stick, float bucket) {
        String boomStr   = String.format(Locale.getDefault(), "%.2f°", boom);
        String stickStr  = String.format(Locale.getDefault(), "%.2f°", stick);
        String bucketStr = String.format(Locale.getDefault(), "%.2f°", bucket);

        // 底部三行
        if (tvBoom   != null) tvBoom.setText(boomStr);
        if (tvStick  != null) tvStick.setText(stickStr);
        if (tvBucket != null) tvBucket.setText(bucketStr);

    }

    private void setupToggle() {
        if (btn2D == null || btn3D == null) return;
        btn2D.setOnClickListener(v -> setMode(false));
        btn3D.setOnClickListener(v -> setMode(true));
        applyToggleStyle();
    }

    private void setMode(boolean threeDimensional) {
        is3D = threeDimensional;
        if (excavatorPostureView != null) {
            excavatorPostureView.setDisplayMode(is3D);
        }
        if (excavatorPosture2DView != null) {
            excavatorPosture2DView.setDisplayMode(false);
        }
        applyModeVisibility();
        applyToggleStyle();
    }

    /**
     * 切换 2D FrameLayout 和 3D WebView 的可见性。
     *
     * <p>同时把另一侧 WebView 真正 {@link ExcavatorPostureView#pause()}：
     * Android 的 {@code View.GONE} 不会暂停 WebView 内的 JS / requestAnimationFrame，
     * 否则 3D（three.js animate loop）会在用户根本看不到的情况下持续以 60fps 渲染并合成，
     * 与 FPV 的 TextureView 抢同一份 GPU/合成预算 → 表现为 RTSP 直播规律性掉帧。
     */
    private void applyModeVisibility() {
        if (view2D != null) {
            view2D.setVisibility(is3D ? View.GONE : View.VISIBLE);
        }
        if (view3D != null) {
            view3D.setVisibility(is3D ? View.VISIBLE : View.GONE);
        }
        if (is3D) {
            if (excavatorPostureView != null) excavatorPostureView.resume();
            if (excavatorPosture2DView != null) excavatorPosture2DView.pause();
        } else {
            if (excavatorPosture2DView != null) excavatorPosture2DView.resume();
            if (excavatorPostureView != null) excavatorPostureView.pause();
        }
    }

    /** 由 Activity#onPause 触发：把卡片内全部 WebView 都挂起。 */
    public void onActivityPause() {
        if (excavatorPosture2DView != null) excavatorPosture2DView.onActivityPause();
        if (excavatorPostureView != null) excavatorPostureView.onActivityPause();
    }

    /** 由 Activity#onResume 触发：恢复当前可见模式对应的 WebView。 */
    public void onActivityResume() {
        // 仅恢复当前可见模式对应的 WebView；另一侧 onActivityPause 后保持 paused。
        if (is3D) {
            if (excavatorPostureView != null) excavatorPostureView.onActivityResume();
        } else {
            if (excavatorPosture2DView != null) excavatorPosture2DView.onActivityResume();
        }
    }

    private void applyToggleStyle() {
        if (btn2D == null || btn3D == null) return;
        final int selectedText = 0xFFFFFFFF;
        final int unselectedText = 0x80FFFFFF; // #66545435
        if (is3D) {
            btn2D.setBackground(null);
            btn2D.setTextColor(unselectedText);
            btn3D.setBackgroundResource(R.drawable.mode_toggle_selected_bg);
            btn3D.setTextColor(selectedText);
        } else {
            btn2D.setBackgroundResource(R.drawable.mode_toggle_selected_bg);
            btn2D.setTextColor(selectedText);
            btn3D.setBackground(null);
            btn3D.setTextColor(unselectedText);
        }
    }

    private static int dp(Context ctx, int value) {
        return Math.round(value * ctx.getResources().getDisplayMetrics().density);
    }
}
