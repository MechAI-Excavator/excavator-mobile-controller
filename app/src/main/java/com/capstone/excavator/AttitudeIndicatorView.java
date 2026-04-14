package com.capstone.excavator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * 姿态仪视图：以 cursor.png 作为蓝色雷达底图，用 point.png 黄色光标指示俯仰/横滚角。
 *
 * 坐标映射规则：
 *   • pitch 正值（抬头）  → 光标向上（Y 负方向）
 *   • roll  正值（右倾）  → 光标向右（X 正方向）
 *   • MAX_ANGLE_DEG (30°) 对应光标移动到可活动圆的边缘
 */
public class AttitudeIndicatorView extends View {

    private static final float MAX_ANGLE_DEG = 30f;

    /** 光标可活动圆半径相对于视图半径的比例（0~1），防止光标超出雷达圆圈 */
    private static final float ACTIVE_ZONE_RATIO = 0.72f;

    /** point.png 绘制尺寸相对于视图宽度的比例 */
    private static final float POINT_SIZE_RATIO = 0.22f;

    private Bitmap cursorBitmap;
    private Bitmap pointBitmap;
    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    private float pitch = 0f;
    private float roll  = 0f;

    public AttitudeIndicatorView(Context context) {
        this(context, null);
    }

    public AttitudeIndicatorView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AttitudeIndicatorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        cursorBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.cursor);
        pointBitmap  = BitmapFactory.decodeResource(context.getResources(), R.drawable.point);
    }

    /** 更新俯仰/横滚角（单位：°），触发重绘 */
    public void setAngles(float pitch, float roll) {
        this.pitch = pitch;
        this.roll  = roll;
        invalidate();
    }

    // ── Measure: 保持正方形 ──────────────────────────────────────────

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // 固定 50dp × 50dp
        int size = Math.round(50 * getResources().getDisplayMetrics().density);
        setMeasuredDimension(size, size);
    }

    // ── Draw ────────────────────────────────────────────────────────

    @Override
    protected void onDraw(Canvas canvas) {
        if (cursorBitmap == null || pointBitmap == null) return;

        int w = getWidth();
        int h = getHeight();
        float cx = w * 0.5f;
        float cy = h * 0.5f;

        // 1. 画雷达底图（铺满）
        Rect src = new Rect(0, 0, cursorBitmap.getWidth(), cursorBitmap.getHeight());
        RectF dst = new RectF(0, 0, w, h);
        canvas.drawBitmap(cursorBitmap, src, dst, bitmapPaint);

        // 2. 计算光标偏移
        float activeRadius = cx * ACTIVE_ZONE_RATIO;

        float rawOffsetX =  (roll  / MAX_ANGLE_DEG) * activeRadius;
        float rawOffsetY = -(pitch / MAX_ANGLE_DEG) * activeRadius;

        // 超出活动圆边缘时贴边
        float dist = (float) Math.sqrt(rawOffsetX * rawOffsetX + rawOffsetY * rawOffsetY);
        float offsetX, offsetY;
        if (dist > activeRadius) {
            offsetX = rawOffsetX / dist * activeRadius;
            offsetY = rawOffsetY / dist * activeRadius;
        } else {
            offsetX = rawOffsetX;
            offsetY = rawOffsetY;
        }

        // 3. 画黄色光标（以偏移后的点为中心）
        float pointSize = w * POINT_SIZE_RATIO;
        float left   = cx + offsetX - pointSize * 0.5f;
        float top    = cy + offsetY - pointSize * 0.5f;
        float right  = left + pointSize;
        float bottom = top  + pointSize;
        canvas.drawBitmap(pointBitmap, null, new RectF(left, top, right, bottom), bitmapPaint);
    }
}
