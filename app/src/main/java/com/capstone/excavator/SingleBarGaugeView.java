package com.capstone.excavator;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * 与 {@link VerticalSpectrumGaugeView} 结构完全一致的双侧度量衡指示条，差异仅两处：
 * <ol>
 *   <li>激活色固定为绿色，不做彩虹色映射。</li>
 *   <li>顶部绘制一行 title 文字，不绘制「高」「低」，也无底部 footer。</li>
 * </ol>
 * 通过 {@link #setTitle(String)} / XML {@code app:sbgTitle}、
 * {@link #setRangeMax(float)}、{@link #setValue(float)} 驱动。
 */
public class SingleBarGaugeView extends View {

    /** 单侧矩形段数（不含贴中线的三角），与 VerticalSpectrumGaugeView.RECTS_PER_HALF 语义相同。 */
    private static final int RECTS_PER_HALF = 7;
    private static final int SLOTS_PER_HALF = RECTS_PER_HALF + 1;

    private static final int INACTIVE = Color.argb(128, 255, 255, 255);
    private static final int ACTIVE   = Color.parseColor("#4CFF4C");

    private final Paint fillPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dashPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path  triPath    = new Path();
    private final RectF rectTmp    = new RectF();

    private String title    = "";
    private float  rangeMax = 1f;
    private float  value    = 0f;

    // --- 几何缓存（onSizeChanged 时计算，与 VerticalSpectrumGaugeView 字段一一对应） ---
    private float dp1;
    private float gap;
    private float padX;
    private float segmentTop;
    private float segmentBottom;
    private float centerY;
    private float slotH;
    private float cornerRx;
    private float segInnerL;
    private float segInnerR;
    private float titleBaseline;

    public SingleBarGaugeView(Context context) {
        this(context, null);
    }

    public SingleBarGaugeView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SingleBarGaugeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        dp1  = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, getResources().getDisplayMetrics());
        gap  = dp1 * 1.2f;
        padX = dp1 * 5f;

        fillPaint.setStyle(Paint.Style.FILL);

        // title 字号与 VerticalSpectrumGaugeView 的「高/低」标签一致
        titlePaint.setColor(Color.WHITE);
        titlePaint.setTextAlign(Paint.Align.CENTER);
        titlePaint.setAntiAlias(true);
        titlePaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 8.5f, getResources().getDisplayMetrics()));

        // 虚线，与 VerticalSpectrumGaugeView.dashPaint 完全相同
        dashPaint.setStyle(Paint.Style.STROKE);
        dashPaint.setColor(Color.argb(220, 255, 255, 255));
        dashPaint.setStrokeWidth(Math.max(1f, dp1 * 0.6f));
        dashPaint.setAntiAlias(true);
        dashPaint.setPathEffect(new DashPathEffect(new float[]{dp1 * 1.8f, dp1 * 1.8f}, 0));

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SingleBarGaugeView, defStyleAttr, 0);
            try {
                String t = a.getString(R.styleable.SingleBarGaugeView_sbgTitle);
                title    = t != null ? t : "";
                rangeMax = a.getFloat(R.styleable.SingleBarGaugeView_sbgRangeMax, 1f);
            } finally {
                a.recycle();
            }
        }
        if (rangeMax <= 0f) rangeMax = 1f;
    }

    // -------------------------------------------------------------------------
    // 公开 API（与 VerticalSpectrumGaugeView 对齐）
    // -------------------------------------------------------------------------

    public void setTitle(String t) {
        title = t != null ? t : "";
        invalidate();
    }

    public void setRangeMax(float max) {
        if (max <= 0f) max = 1f;
        if (this.rangeMax != max) { this.rangeMax = max; invalidate(); }
    }

    public float getRangeMax() { return rangeMax; }

    public void setValue(float v) {
        if (this.value != v) { this.value = v; invalidate(); }
    }

    public float getValue() { return value; }

    // -------------------------------------------------------------------------
    // 布局几何
    // 与 VerticalSpectrumGaugeView.layoutGeometry 完全相同，唯一区别：
    //   - topReserve 由 title 高度决定（对应原来的 marginVerticalEdges + labelH + gapLabelToChart）
    //   - bottomReserve 只留边距，不留 footer 或「低」字
    // -------------------------------------------------------------------------

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        layoutGeometry(w, h);
    }

    private void layoutGeometry(int w, int h) {
        segInnerL = padX;
        segInnerR = w - padX;

        Paint.FontMetrics fm = titlePaint.getFontMetrics();
        float titleH     = fm.descent - fm.ascent;
        float marginV    = dp1 * 5f;
        float gapToChart = dp1 * 5f;

        // title 基线
        titleBaseline = marginV - fm.ascent;

        // 刻度区：顶部留给 title，底部只留边距（无「低」字/footer）
        float topReserve    = marginV + titleH + gapToChart;
        float bottomReserve = marginV;

        segmentTop    = topReserve;
        segmentBottom = h - bottomReserve;

        // 与 VerticalSpectrumGaugeView 完全相同的最小高度保护
        float minSeg = SLOTS_PER_HALF * dp1 * 2f + gap * (SLOTS_PER_HALF - 1);
        if (segmentBottom - segmentTop < minSeg) {
            segmentBottom = segmentTop + minSeg;
        }

        centerY = (segmentTop + segmentBottom) * 0.5f;

        float halfSpan = centerY - segmentTop;
        slotH = (halfSpan - gap * (SLOTS_PER_HALF - 1)) / SLOTS_PER_HALF;
        if (slotH < dp1 * 1.2f) slotH = dp1 * 1.2f;
        cornerRx = slotH * 0.22f;
    }

    // -------------------------------------------------------------------------
    // 绘制
    // 与 VerticalSpectrumGaugeView.onDraw 完全相同，差异：
    //   - 绘制顶部 title，不绘制「高」「低」及 footer
    //   - 激活色固定为 ACTIVE（绿），不调用 spectrumAtY
    // -------------------------------------------------------------------------

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        float cx = w * 0.5f;

        // 顶部 title（替代「高/低」文字）
        if (!title.isEmpty()) {
            canvas.drawText(title, cx, titleBaseline, titlePaint);
        }

        // 激活段数计算（与 VerticalSpectrumGaugeView 相同）
        float norm        = clamp(value / rangeMax, -1f, 1f);
        int   activeTop   = norm > 0f ? (int) Math.ceil( norm * SLOTS_PER_HALF) : 0;
        int   activeBot   = norm < 0f ? (int) Math.ceil(-norm * SLOTS_PER_HALF) : 0;
        activeTop = Math.min(SLOTS_PER_HALF, activeTop);
        activeBot = Math.min(SLOTS_PER_HALF, activeBot);

        float triHalfW = (segInnerR - segInnerL) * 0.45f;

        // 上半段（从 centerY 向上），与 VerticalSpectrumGaugeView 上半段循环完全相同
        float slotBottom = centerY;
        for (int i = 0; i < SLOTS_PER_HALF; i++) {
            float slotTop = slotBottom - slotH;
            boolean lit   = i < activeTop;
            fillPaint.setColor(lit ? ACTIVE : INACTIVE);
            if (i == 0) {
                // drawTopArrow：尖朝下，两底角在上
                triPath.rewind();
                triPath.moveTo(cx,            slotBottom);
                triPath.lineTo(cx - triHalfW, slotTop);
                triPath.lineTo(cx + triHalfW, slotTop);
                triPath.close();
                canvas.drawPath(triPath, fillPaint);
            } else {
                rectTmp.set(segInnerL, slotTop, segInnerR, slotBottom);
                canvas.drawRoundRect(rectTmp, cornerRx, cornerRx, fillPaint);
            }
            slotBottom = slotTop - gap;
        }

        // 下半段（从 centerY 向下），与 VerticalSpectrumGaugeView 下半段循环完全相同
        float slotTop2 = centerY;
        for (int i = 0; i < SLOTS_PER_HALF; i++) {
            float slotBot = slotTop2 + slotH;
            boolean lit   = i < activeBot;
            fillPaint.setColor(lit ? ACTIVE : INACTIVE);
            if (i == 0) {
                // drawBottomArrow：尖朝上，两底角在下
                triPath.rewind();
                triPath.moveTo(cx,            slotTop2);
                triPath.lineTo(cx - triHalfW, slotBot);
                triPath.lineTo(cx + triHalfW, slotBot);
                triPath.close();
                canvas.drawPath(triPath, fillPaint);
            } else {
                rectTmp.set(segInnerL, slotTop2, segInnerR, slotBot);
                canvas.drawRoundRect(rectTmp, cornerRx, cornerRx, fillPaint);
            }
            slotTop2 = slotBot + gap;
        }

        // 中线虚线（零点基准，与 VerticalSpectrumGaugeView 完全相同）
        canvas.drawLine(segInnerL, centerY, segInnerR, centerY, dashPaint);
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
