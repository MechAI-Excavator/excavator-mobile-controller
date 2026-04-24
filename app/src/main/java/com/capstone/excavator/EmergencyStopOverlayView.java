package com.capstone.excavator;

import android.content.Context;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;

/**
 * 急停全屏覆盖层。
 *
 * 点击急停按钮后调用 {@link #show()} 将本视图设为 VISIBLE；
 * 用户长按屏幕 3 秒后进度条填满，调用 {@link OnDismissListener} 并自动隐藏。
 * 长按过程中松手则进度条回弹到 0。
 */
public class EmergencyStopOverlayView extends FrameLayout {

    /** 长按解除急停所需时长（毫秒） */
    private static final long HOLD_DURATION_MS = 3000L;

    public interface OnDismissListener {
        /** 长按满 3 秒后触发，回调完成后覆盖层已被隐藏。 */
        void onEmergencyStopReleased();
    }

    private View progressFill;
    private long holdStartTime = 0L;
    private float holdStartScale = 0f;
    private OnDismissListener onDismissListener;
    private ValueAnimator holdAnimator;

    public EmergencyStopOverlayView(Context context) {
        this(context, null);
    }

    public EmergencyStopOverlayView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EmergencyStopOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setBackgroundColor(Color.parseColor("#AF0008"));
        setClickable(true);
        setFocusable(true);
        inflate(context, R.layout.view_emergency_stop_overlay, this);
        progressFill = findViewById(R.id.emergencyProgressFill);
    }

    public void setOnDismissListener(OnDismissListener listener) {
        this.onDismissListener = listener;
    }

    /** 显示急停覆盖层并重置进度条。 */
    public void show() {
        resetProgress();
        setVisibility(VISIBLE);
        bringToFront();
    }

    /** 隐藏急停覆盖层并取消进行中的动画。 */
    public void hide() {
        cancelHold();
        resetProgress();
        setVisibility(GONE);
    }

    /** 覆盖层可见时拦截所有触摸事件，避免被内部控件吞掉。 */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return getVisibility() == VISIBLE;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                startHold();
                return true;
            case MotionEvent.ACTION_MOVE:
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                cancelHold();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    private void startHold() {
        cancelHold();
        if (progressFill == null) return;
        holdStartScale = progressFill.getScaleX();
        if (holdStartScale >= 1f) {
            triggerRelease();
            return;
        }
        progressFill.animate().cancel();

        // 用系统动画时钟（基于 Choreographer/VSYNC）推进，避免不同机型上 Handler 16ms 抖动导致卡顿
        holdStartTime = SystemClock.uptimeMillis();
        long remaining = (long) ((1f - holdStartScale) * HOLD_DURATION_MS);
        remaining = Math.max(0L, remaining);

        holdAnimator = ValueAnimator.ofFloat(holdStartScale, 1f);
        holdAnimator.setDuration(remaining);
        holdAnimator.setInterpolator(new LinearInterpolator());
        holdAnimator.addUpdateListener(anim -> {
            if (progressFill == null) return;
            float v = (float) anim.getAnimatedValue();
            progressFill.setScaleX(v);
        });
        holdAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                // 若是被 cancel 导致的 end，不触发释放
                if (holdAnimator == null) return;
                holdAnimator = null;
                if (progressFill != null && progressFill.getScaleX() >= 1f) {
                    triggerRelease();
                }
            }
        });
        holdAnimator.start();
    }

    private void cancelHold() {
        if (holdAnimator != null) {
            holdAnimator.cancel();
            holdAnimator = null;
        }
        if (progressFill != null) {
            progressFill.animate().cancel();
            progressFill.animate()
                    .scaleX(0f)
                    .setDuration(180)
                    .start();
        }
    }

    private void resetProgress() {
        if (holdAnimator != null) {
            holdAnimator.cancel();
            holdAnimator = null;
        }
        if (progressFill != null) {
            progressFill.animate().cancel();
            progressFill.setScaleX(0f);
        }
    }

    private void triggerRelease() {
        hide();
        if (onDismissListener != null) {
            onDismissListener.onEmergencyStopReleased();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cancelHold();
    }
}
