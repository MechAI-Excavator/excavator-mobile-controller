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

/** 横向刻度条（纯绘制）：左/右标签、色段、中心虚线。 */
public class CenterGaugeStripView extends View {

    private static final int RECTS_PER_HALF = 18;
    private static final int SLOTS_PER_HALF = RECTS_PER_HALF + 1;

    private static final int INACTIVE = Color.rgb(0x42, 0x42, 0x42);

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint endLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dashPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path triPath = new Path();
    private final RectF rectTmp = new RectF();

    private String leftLabel = "左";
    private String rightLabel = "右";
    private float rangeMax = 1f;
    private float value;

    private float dp1;
    private float gap;
    private float padH;
    private float padV;
    private float gapLabelToTrack;
    private float bandH;
    private float cornerRx;

    private float trackInnerL;
    private float trackInnerR;
    private float centerX;
    private float slotW;
    private float barCy;
    private float halfBand;
    private float contentHeight;

    public CenterGaugeStripView(Context context) {
        this(context, null);
    }

    public CenterGaugeStripView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CenterGaugeStripView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setBackgroundColor(Color.TRANSPARENT);

        Resources res = context.getResources();
        dp1 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, res.getDisplayMetrics());
        gap = dp1 * 1.2f;
        padH = dp1 * 6f;
        padV = dp1 * 4f;
        gapLabelToTrack = dp1 * 6f;
        bandH = dp1 * 10f;
        halfBand = bandH * 0.5f;
        cornerRx = bandH * 0.22f;

        endLabelPaint.setColor(Color.WHITE);
        endLabelPaint.setTextAlign(Paint.Align.LEFT);
        endLabelPaint.setAntiAlias(true);
        endLabelPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 9.5f,
                res.getDisplayMetrics()));

        dashPaint.setStyle(Paint.Style.STROKE);
        dashPaint.setColor(Color.argb(220, 255, 255, 255));
        dashPaint.setStrokeWidth(Math.max(1f, dp1 * 0.55f));
        dashPaint.setAntiAlias(true);
        dashPaint.setPathEffect(new DashPathEffect(new float[]{dp1 * 1.6f, dp1 * 1.6f}, 0));

        fillPaint.setStyle(Paint.Style.FILL);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CenterGaugeStripView, defStyleAttr, 0);
            try {
                String fl = a.getString(R.styleable.CenterGaugeStripView_centerGaugeLeftLabel);
                if (fl != null && !fl.isEmpty()) leftLabel = fl;
                String fr = a.getString(R.styleable.CenterGaugeStripView_centerGaugeRightLabel);
                if (fr != null && !fr.isEmpty()) rightLabel = fr;
                rangeMax = a.getFloat(R.styleable.CenterGaugeStripView_centerGaugeRangeMax, 1f);
            } finally {
                a.recycle();
            }
        }
        if (rangeMax <= 0f) {
            rangeMax = 1f;
        }
    }

    public void setRangeMax(float maxAbs) {
        if (maxAbs <= 0f) maxAbs = 1f;
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

    public void setLeftLabel(String text) {
        leftLabel = text != null ? text : "左";
        requestLayout();
        invalidate();
    }

    public void setRightLabel(String text) {
        rightLabel = text != null ? text : "右";
        requestLayout();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        layoutGeometry(w, h);
    }

    private void layoutGeometry(int w, @SuppressWarnings("unused") int h) {
        float leftW = endLabelPaint.measureText(leftLabel);
        float rightW = endLabelPaint.measureText(rightLabel);

        trackInnerL = padH + leftW + gapLabelToTrack;
        trackInnerR = w - padH - rightW - gapLabelToTrack;
        if (trackInnerR - trackInnerL < dp1 * 40f) {
            trackInnerR = trackInnerL + dp1 * 40f;
        }

        centerX = (trackInnerL + trackInnerR) * 0.5f;
        float leftHalfW = centerX - trackInnerL - gap * 0.5f;
        float rightHalfW = trackInnerR - centerX - gap * 0.5f;
        float halfW = Math.min(leftHalfW, rightHalfW);
        float minSlot = dp1 * 1.2f;
        slotW = (halfW - gap * (SLOTS_PER_HALF - 1)) / SLOTS_PER_HALF;
        if (slotW < minSlot) {
            slotW = minSlot;
        }

        barCy = padV + halfBand;
        contentHeight = barCy + halfBand + padV;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        layoutGeometry(w, 0);

        int hMode = MeasureSpec.getMode(heightMeasureSpec);
        int hSize = MeasureSpec.getSize(heightMeasureSpec);
        int h;
        if (hMode == MeasureSpec.EXACTLY) {
            h = hSize;
        } else {
            h = (int) Math.ceil(contentHeight);
            if (hMode == MeasureSpec.AT_MOST) {
                h = Math.min(h, hSize);
            }
        }
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        layoutGeometry(w, h);
        float yOff = Math.max(0f, (h - contentHeight) * 0.5f);

        canvas.save();
        canvas.translate(0f, yOff);

        Paint.FontMetrics fmEnd = endLabelPaint.getFontMetrics();
        float labelBaseline = barCy - (fmEnd.ascent + fmEnd.descent) * 0.5f;
        endLabelPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(leftLabel, padH, labelBaseline, endLabelPaint);
        endLabelPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(rightLabel, w - padH, labelBaseline, endLabelPaint);

        float norm = clamp(value / rangeMax, -1f, 1f);
        int activeLeft = norm < 0f ? (int) Math.ceil((-norm) * SLOTS_PER_HALF) : 0;
        int activeRight = norm > 0f ? (int) Math.ceil(norm * SLOTS_PER_HALF) : 0;
        activeLeft = Math.min(SLOTS_PER_HALF, activeLeft);
        activeRight = Math.min(SLOTS_PER_HALF, activeRight);

        float topY = barCy - halfBand;
        float botY = barCy + halfBand;

        for (int i = 0; i < SLOTS_PER_HALF; i++) {
            float slotRight = centerX - i * (slotW + gap);
            float slotLeft = slotRight - slotW;
            boolean lit = i < activeLeft;
            float cx = (slotLeft + slotRight) * 0.5f;
            int color = lit ? spectrumLeft(cx) : INACTIVE;
            fillPaint.setColor(color);
            if (i == 0) {
                drawLeftArrow(canvas, centerX, barCy, slotW, topY, botY, fillPaint);
            } else {
                rectTmp.set(slotLeft, topY, slotRight, botY);
                canvas.drawRoundRect(rectTmp, cornerRx, cornerRx, fillPaint);
            }
        }

        for (int i = 0; i < SLOTS_PER_HALF; i++) {
            float slotLeft = centerX + i * (slotW + gap);
            float slotRight = slotLeft + slotW;
            boolean lit = i < activeRight;
            float cx = (slotLeft + slotRight) * 0.5f;
            int color = lit ? spectrumRight(cx) : INACTIVE;
            fillPaint.setColor(color);
            if (i == 0) {
                drawRightArrow(canvas, centerX, barCy, slotW, topY, botY, fillPaint);
            } else {
                rectTmp.set(slotLeft, topY, slotRight, botY);
                canvas.drawRoundRect(rectTmp, cornerRx, cornerRx, fillPaint);
            }
        }

        canvas.drawLine(centerX, topY - dp1 * 0.8f, centerX, botY + dp1 * 0.8f, dashPaint);

        canvas.restore();
    }

    private int spectrumLeft(float x) {
        float span = centerX - trackInnerL;
        float t = span > 1e-3f ? (centerX - x) / span : 0.5f;
        t = clamp(t, 0f, 1f);
        float hue = 120f * (1f - t);
        return Color.HSVToColor(new float[]{hue, 0.88f, 1f});
    }

    private int spectrumRight(float x) {
        float span = trackInnerR - centerX;
        float t = span > 1e-3f ? (x - centerX) / span : 0.5f;
        t = clamp(t, 0f, 1f);
        float hue = 120f * (1f - t);
        return Color.HSVToColor(new float[]{hue, 0.88f, 1f});
    }

    private void drawLeftArrow(Canvas c, float cx, float cy, float slotW, float topY, float botY, Paint p) {
        float left = cx - slotW;
        triPath.rewind();
        triPath.moveTo(cx, cy);
        triPath.lineTo(left, topY);
        triPath.lineTo(left, botY);
        triPath.close();
        c.drawPath(triPath, p);
    }

    private void drawRightArrow(Canvas c, float cx, float cy, float slotW, float topY, float botY, Paint p) {
        float right = cx + slotW;
        triPath.rewind();
        triPath.moveTo(cx, cy);
        triPath.lineTo(right, topY);
        triPath.lineTo(right, botY);
        triPath.close();
        c.drawPath(triPath, p);
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
