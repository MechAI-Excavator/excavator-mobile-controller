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

    private ExcavatorPostureView excavatorPostureView;

    // 底部三行角度文字
    private TextView tvBoom;
    private TextView tvStick;
    private TextView tvBucket;

    // 2D 示意图叠加角度文字
    private View    view2D;
    private TextView tvOverlay2DBoom;
    private TextView tvOverlay2DStick;
    private TextView tvOverlay2DBucket;

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

        int pv = dp(context, 10);
        int ph = dp(context, 12);
        setPadding(ph, pv, ph, pv);

        setBackground(context.getResources().getDrawable(R.drawable.map_card_bg, context.getTheme()));
        setClipToOutline(true);
        setOutlineProvider(android.view.ViewOutlineProvider.BACKGROUND);

        inflate(context, R.layout.view_posture_card, this);

        excavatorPostureView = findViewById(R.id.excavatorPostureView);
        tvBoom               = findViewById(R.id.tvPostureBoomAngle);
        tvStick              = findViewById(R.id.tvPostureStickAngle);
        tvBucket             = findViewById(R.id.tvPostureBucketAngle);
        btn2D                = findViewById(R.id.btn2D);
        btn3D                = findViewById(R.id.btn3D);

        view2D           = findViewById(R.id.view2D);
        tvOverlay2DBoom   = findViewById(R.id.tvOverlay2DBoom);
        tvOverlay2DStick  = findViewById(R.id.tvOverlay2DStick);
        tvOverlay2DBucket = findViewById(R.id.tvOverlay2DBucket);

        // ExcavatorPostureView 已嵌入卡片，不需要自己的卡片背景和圆角裁切
        if (excavatorPostureView != null) {
            excavatorPostureView.setEmbedded(true);
        }

        // 初始状态：2D 可见，3D 隐藏
        applyModeVisibility();
        setupToggle();
    }

    // ── Public API ───────────────────────────────────────────────────

    /**
     * 更新全部角度（5 参数版，与 ExcavatorPostureView.setAngles 对齐）。
     * 同时更新 WebView 动画和三行角度文字。
     */
    public void setAngles(float cabinPitch, float cabinRoll,
                          float boom, float stick, float bucket) {
        if (excavatorPostureView != null) {
            excavatorPostureView.setAngles(cabinPitch, cabinRoll, boom, stick, bucket);
        }
        updateAngleText(boom, stick, bucket);
    }

    public void setBucketAngleOffsetDeg(float offsetDeg) {
        if (excavatorPostureView != null) {
            excavatorPostureView.setBucketAngleOffsetDeg(offsetDeg);
        }
    }

    public void setLengthScales(float boomScale, float stickScale) {
        if (excavatorPostureView != null) {
            excavatorPostureView.setLengthScales(boomScale, stickScale);
        }
    }

    public void setArmLengthsFromMm(double boomMm, double stickMm, double bucketMm) {
        if (excavatorPostureView != null) {
            excavatorPostureView.setArmLengthsFromMm(boomMm, stickMm, bucketMm);
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

        // 2D 叠加层（同步更新，无论当前是否在 2D 模式）
        if (tvOverlay2DBoom   != null) tvOverlay2DBoom.setText(boomStr);
        if (tvOverlay2DStick  != null) tvOverlay2DStick.setText(stickStr);
        if (tvOverlay2DBucket != null) tvOverlay2DBucket.setText(bucketStr);
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
        applyModeVisibility();
        applyToggleStyle();
    }

    /** 切换 2D FrameLayout 和 3D WebView 的可见性 */
    private void applyModeVisibility() {
        if (view2D != null) {
            view2D.setVisibility(is3D ? View.GONE : View.VISIBLE);
        }
        if (excavatorPostureView != null) {
            excavatorPostureView.setVisibility(is3D ? View.VISIBLE : View.GONE);
        }
    }

    private void applyToggleStyle() {
        if (btn2D == null || btn3D == null) return;
        if (is3D) {
            btn2D.setBackground(null);
            btn2D.setTextColor(0x80FFFFFF);
            btn3D.setBackgroundResource(R.drawable.mode_toggle_selected_bg);
            btn3D.setTextColor(0xFF000000);
        } else {
            btn2D.setBackgroundResource(R.drawable.mode_toggle_selected_bg);
            btn2D.setTextColor(0xFF000000);
            btn3D.setBackground(null);
            btn3D.setTextColor(0x80FFFFFF);
        }
    }

    private static int dp(Context ctx, int value) {
        return Math.round(value * ctx.getResources().getDisplayMetrics().density);
    }
}
