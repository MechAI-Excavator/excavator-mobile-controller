package com.capstone.excavator;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * 摇杆方向指示器：透明圆环 + 中心白色光点。
 * 由外层容器（{@code joystick_glass_bg}）提供毛玻璃圆角矩形背景，
 * 本视图只负责绘制圆环和指示点。
 */
public class JoystickIndicatorView extends View {

    private static final int MAX_VALUE = 450;

    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int xValue = 0; // -450 ~ 450, 右正左负
    private int yValue = 0; // -450 ~ 450, 上正下负

    public JoystickIndicatorView(Context context) {
        super(context);
        init();
    }

    public JoystickIndicatorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // 圆环：半透明白色描边
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setColor(Color.argb(180, 255, 255, 255));

        // 圆环内填充：极轻微的高亮，营造"透亮"感
        ringFillPaint.setStyle(Paint.Style.FILL);
        ringFillPaint.setColor(Color.argb(40, 255, 255, 255));

        // 指示光点
        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(Color.WHITE);

        // 光点外侧的柔光
        dotGlowPaint.setStyle(Paint.Style.FILL);
        dotGlowPaint.setColor(Color.argb(80, 255, 255, 255));
    }

    /** 外部调用：更新摇杆值，x 左右，y 上下（上正） */
    public void setValues(int x, int y) {
        xValue = clamp(x, -MAX_VALUE, MAX_VALUE);
        yValue = clamp(y, -MAX_VALUE, MAX_VALUE);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float radius = Math.min(w, h) / 2f * 0.78f;

        ringPaint.setStrokeWidth(Math.max(1.5f, radius * 0.04f));
        canvas.drawCircle(cx, cy, radius, ringFillPaint);
        canvas.drawCircle(cx, cy, radius, ringPaint);

        float maxOffset = radius * 0.78f;
        float dotX = cx + (xValue / (float) MAX_VALUE) * maxOffset;
        float dotY = cy - (yValue / (float) MAX_VALUE) * maxOffset; // y 上正，canvas 向下为正
        float dotRadius = Math.max(3f, radius * 0.12f);
        canvas.drawCircle(dotX, dotY, dotRadius * 1.85f, dotGlowPaint);
        canvas.drawCircle(dotX, dotY, dotRadius, dotPaint);
    }

    private static int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }
}
