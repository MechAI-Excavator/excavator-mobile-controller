package com.capstone.excavator;

import android.content.Context;
import android.content.res.Resources;
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
 * 垂直「度量衡」指示条：中间虚线为零点，上下向中心收拢为三角箭头；
 * 数值在 {@code [-rangeMax, rangeMax]} 内从中间向一侧逐段点亮，
 * 每段颜色按纵向位置映射为红→黄→绿→青→蓝→紫，未点亮为灰。
 */
public class VerticalSpectrumGaugeView extends View {

    /** 单侧矩形段数（不含贴中线的三角）。 */
    private static final int RECTS_PER_HALF = 18;
    private static final int SLOTS_PER_HALF = RECTS_PER_HALF + 1;

    private static final int INACTIVE = Color.argb(128, 255, 255, 255);

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint footerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dashPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path triPath = new Path();
    private final RectF rectTmp = new RectF();

    private String footerText = "";
    private float rangeMax = 1f;
    private float value;

    private float dp1;
    private float segmentTop;
    private float segmentBottom;
    private float centerY;
    private float slotH;
    private float gap;
    private float padX;
    private float segInnerL;
    private float segInnerR;
    private float cornerRx;

    private float highBaseline;
    private float lowBaseline;
    private float footerBaseline = -1f;

    /** 顶/底文字与视图边缘的竖直距离（原 marginV）。 */
    private float marginVerticalEdges;
    /** 「高」「低」与彩色刻度区之间的竖直间距（原 gapLabel）。 */
    private float gapLabelToChart;
    /** 「低」与 footer 之间的额外竖直留白。 */
    private float gapLowToFooterSpacing;

    public VerticalSpectrumGaugeView(Context context) {
        this(context, null);
    }

    public VerticalSpectrumGaugeView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VerticalSpectrumGaugeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        dp1 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, getResources().getDisplayMetrics());
        gap = dp1 * 1.2f;
        padX = dp1 * 5f;

        fillPaint.setStyle(Paint.Style.FILL);

        float labelSp = 8.5f;
        labelPaint.setColor(Color.WHITE);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setAntiAlias(true);
        labelPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, labelSp,
                getResources().getDisplayMetrics()));

        footerPaint.setColor(Color.WHITE);
        footerPaint.setTextAlign(Paint.Align.CENTER);
        footerPaint.setAntiAlias(true);
        footerPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 7f,
                getResources().getDisplayMetrics()));

        dashPaint.setStyle(Paint.Style.STROKE);
        dashPaint.setColor(Color.argb(220, 255, 255, 255));
        dashPaint.setStrokeWidth(Math.max(1f, dp1 * 0.6f));
        dashPaint.setAntiAlias(true);
        dashPaint.setPathEffect(new DashPathEffect(new float[]{dp1 * 1.8f, dp1 * 1.8f}, 0));

        Resources res = context.getResources();
        marginVerticalEdges = res.getDimension(R.dimen.gauge_vertical_edge_margin);
        gapLabelToChart = res.getDimension(R.dimen.gauge_label_to_chart_gap);
        gapLowToFooterSpacing = res.getDimension(R.dimen.gauge_low_to_footer_spacing);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.VerticalSpectrumGaugeView, defStyleAttr, 0);
            try {
                String ft = a.getString(R.styleable.VerticalSpectrumGaugeView_gaugeFooterText);
                footerText = ft != null ? ft : "";
                rangeMax = a.getFloat(R.styleable.VerticalSpectrumGaugeView_gaugeRangeMax, 1f);
                marginVerticalEdges = a.getDimension(
                        R.styleable.VerticalSpectrumGaugeView_gaugeVerticalEdgeMargin, marginVerticalEdges);
                gapLabelToChart = a.getDimension(
                        R.styleable.VerticalSpectrumGaugeView_gaugeLabelToChartGap, gapLabelToChart);
                gapLowToFooterSpacing = a.getDimension(
                        R.styleable.VerticalSpectrumGaugeView_gaugeLowToFooterSpacing, gapLowToFooterSpacing);
            } finally {
                a.recycle();
            }
        }
        if (rangeMax <= 0f) {
            rangeMax = 1f;
        }
    }

    public void setRangeMax(float maxAbs) {
        if (maxAbs <= 0f) {
            maxAbs = 1f;
        }
        if (this.rangeMax != maxAbs) {
            this.rangeMax = maxAbs;
            invalidate();
        }
    }

    public float getRangeMax() {
        return rangeMax;
    }

    public void setValue(float v) {
        if (this.value != v) {
            this.value = v;
            invalidate();
        }
    }

    public float getValue() {
        return value;
    }

    public void setFooterText(String text) {
        footerText = text != null ? text : "";
        requestLayout();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        layoutGeometry(w, h);
    }

    /**
     * 竖直排版（与 {@link #onDraw} 里三行字对应）：
     * <ul>
     *   <li>{@link #marginVerticalEdges}：视图顶 ↔「高」、视图底 ↔ footer 的边距</li>
     *   <li>{@link #gapLabelToChart}：「高」下沿 ↔ 刻度上沿；「低」上沿 ↔ 刻度下沿</li>
     *   <li>{@link #gapLowToFooterSpacing}：有 footer 时，「低」与 footer 之间的额外间距</li>
     * </ul>
     * 调间距请改 {@code res/values/dimens.xml}，或对单个 View 设 {@code app:gaugeVerticalEdgeMargin} 等。
     */
    private void layoutGeometry(int w, int h) {
        segInnerL = padX;
        segInnerR = w - padX;

        Paint.FontMetrics fm = labelPaint.getFontMetrics();
        float labelH = fm.descent - fm.ascent;
        Paint.FontMetrics ffm = footerPaint.getFontMetrics();
        float footerH = footerText.isEmpty() ? 0f : (ffm.descent - ffm.ascent);

        float topReserve = marginVerticalEdges + labelH + gapLabelToChart;
        float bottomReserve = marginVerticalEdges + labelH + gapLabelToChart;
        if (!footerText.isEmpty()) {
            bottomReserve += footerH + gapLowToFooterSpacing;
        }

        segmentTop = topReserve;
        segmentBottom = h - bottomReserve;
        float minSeg = SLOTS_PER_HALF * dp1 * 2f + gap * (SLOTS_PER_HALF - 1);
        if (segmentBottom - segmentTop < minSeg) {
            segmentBottom = segmentTop + minSeg;
        }

        centerY = (segmentTop + segmentBottom) * 0.5f;
        float halfSpan = centerY - segmentTop;
        slotH = (halfSpan - gap * (SLOTS_PER_HALF - 1)) / SLOTS_PER_HALF;
        if (slotH < dp1 * 1.2f) {
            slotH = dp1 * 1.2f;
        }
        cornerRx = slotH * 0.22f;

        highBaseline = segmentTop - gapLabelToChart - fm.descent;
        lowBaseline = segmentBottom + gapLabelToChart - fm.ascent;
        if (!footerText.isEmpty()) {
            footerBaseline = h - marginVerticalEdges - ffm.descent;
        } else {
            footerBaseline = -1f;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        float cx = w * 0.5f;
        canvas.drawText("高", cx, highBaseline, labelPaint);
        canvas.drawText("低", cx, lowBaseline, labelPaint);
        if (!footerText.isEmpty() && footerBaseline > 0f) {
            canvas.drawText(footerText, cx, footerBaseline, footerPaint);
        }

        float norm = clamp(value / rangeMax, -1f, 1f);
        int activeTop = norm > 0f ? (int) Math.ceil(norm * SLOTS_PER_HALF) : 0;
        int activeBottom = norm < 0f ? (int) Math.ceil((-norm) * SLOTS_PER_HALF) : 0;
        activeTop = Math.min(SLOTS_PER_HALF, activeTop);
        activeBottom = Math.min(SLOTS_PER_HALF, activeBottom);

        float triHalfW = (segInnerR - segInnerL) * 0.45f;

        float slotBottom = centerY;
        for (int i = 0; i < SLOTS_PER_HALF; i++) {
            float slotTop = slotBottom - slotH;
            boolean lit = i < activeTop;
            int color = lit ? spectrumAtY((slotTop + slotBottom) * 0.5f, segmentTop, segmentBottom) : INACTIVE;
            fillPaint.setColor(color);
            if (i == 0) {
                drawTopArrow(canvas, cx, slotTop, slotBottom, triHalfW, fillPaint);
            } else {
                rectTmp.set(segInnerL, slotTop, segInnerR, slotBottom);
                canvas.drawRoundRect(rectTmp, cornerRx, cornerRx, fillPaint);
            }
            slotBottom = slotTop - gap;
        }

        float slotTop2 = centerY;
        for (int i = 0; i < SLOTS_PER_HALF; i++) {
            float slotBot = slotTop2 + slotH;
            boolean lit = i < activeBottom;
            int color = lit ? spectrumAtY((slotTop2 + slotBot) * 0.5f, segmentTop, segmentBottom) : INACTIVE;
            fillPaint.setColor(color);
            if (i == 0) {
                drawBottomArrow(canvas, cx, slotTop2, slotBot, triHalfW, fillPaint);
            } else {
                rectTmp.set(segInnerL, slotTop2, segInnerR, slotBot);
                canvas.drawRoundRect(rectTmp, cornerRx, cornerRx, fillPaint);
            }
            slotTop2 = slotBot + gap;
        }

        canvas.drawLine(segInnerL, centerY, segInnerR, centerY, dashPaint);
    }

    private void drawTopArrow(Canvas c, float cx, float top, float bottom, float halfW, Paint p) {
        triPath.rewind();
        triPath.moveTo(cx, bottom);
        triPath.lineTo(cx - halfW, top);
        triPath.lineTo(cx + halfW, top);
        triPath.close();
        c.drawPath(triPath, p);
    }

    private void drawBottomArrow(Canvas c, float cx, float top, float bottom, float halfW, Paint p) {
        triPath.rewind();
        triPath.moveTo(cx, top);
        triPath.lineTo(cx - halfW, bottom);
        triPath.lineTo(cx + halfW, bottom);
        triPath.close();
        c.drawPath(triPath, p);
    }

    private static int spectrumAtY(float y, float segTop, float segBottom) {
        float span = segBottom - segTop;
        float t = span > 1e-3f ? (y - segTop) / span : 0.5f;
        t = clamp(t, 0f, 1f);
        float hue = t * 280f;
        return Color.HSVToColor(new float[]{hue, 0.88f, 1f});
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
