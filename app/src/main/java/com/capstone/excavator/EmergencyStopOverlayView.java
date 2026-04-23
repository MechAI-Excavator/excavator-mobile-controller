package com.capstone.excavator;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
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

    /** 进度条推进帧间隔（毫秒），约等于 60fps */
    private static final long TICK_INTERVAL_MS = 16L;

    private View progressFill;
    private final Handler holdHandler = new Handler(Looper.getMainLooper());
    private Runnable tickRunnable;
    private long holdStartTime = 0L;
    private float holdStartScale = 0f;
    private OnDismissListener onDismissListener;

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
        holdStartTime = SystemClock.uptimeMillis();
        tickRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - holdStartTime;
                float progress = holdStartScale
                        + (1f - holdStartScale) * (elapsed / (float) HOLD_DURATION_MS);
                if (progress >= 1f) {
                    progressFill.setScaleX(1f);
                    tickRunnable = null;
                    triggerRelease();
                    return;
                }
                progressFill.setScaleX(progress);
                holdHandler.postDelayed(this, TICK_INTERVAL_MS);
            }
        };
        holdHandler.post(tickRunnable);
    }

    private void cancelHold() {
        if (tickRunnable != null) {
            holdHandler.removeCallbacks(tickRunnable);
            tickRunnable = null;
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
        if (tickRunnable != null) {
            holdHandler.removeCallbacks(tickRunnable);
            tickRunnable = null;
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
