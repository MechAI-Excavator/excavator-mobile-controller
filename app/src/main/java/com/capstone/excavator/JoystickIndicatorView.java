package com.capstone.excavator;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

/**
 * 摇杆方向指示器：{@code cusor_around} 圆盘底图 + 中心白色光点。
 * 由外层容器（{@code joystick_glass_bg}）提供毛玻璃圆角矩形背景，
 * 本视图负责底图与指示点。
 */
public class JoystickIndicatorView extends View {

    private static final int MAX_VALUE = 450;

    private Drawable ringBackground;
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int xValue = 0; // -450 ~ 450, 右正左负
    private int yValue = 0; // -450 ~ 450, 上正下负

    public JoystickIndicatorView(Context context) {
        super(context);
        init(context);
    }

    public JoystickIndicatorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        ringBackground = ContextCompat.getDrawable(context, R.drawable.cusor_around);
        if (ringBackground != null) {
            ringBackground.setFilterBitmap(true);
        }

        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(Color.WHITE);

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
        float radius = Math.min(w, h) / 2f;

        if (ringBackground != null) {
            int left = Math.round(cx - radius);
            int top = Math.round(cy - radius);
            int right = Math.round(cx + radius);
            int bottom = Math.round(cy + radius);
            ringBackground.setBounds(left, top, right, bottom);
            ringBackground.draw(canvas);
        }

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
