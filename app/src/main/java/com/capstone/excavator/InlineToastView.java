package com.capstone.excavator;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

/**
 * 通用内联 Toast（叠加在父布局中，无系统 Toast 限制）。
 * <p>
 * 在父布局 XML 中声明（GONE），调用 {@link #showMessage(String)} 或
 * {@link #showMessage(String, long)} 显示，到时后自动隐藏。
 * <p>
 * 样式由 {@code view_inline_toast.xml} 与 {@code inline_toast_bg.xml} 维护。
 */
public class InlineToastView extends FrameLayout {

    private static final long DEFAULT_DURATION_MS = 5000L;

    private TextView messageView;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable hideRunnable = this::hide;

    public InlineToastView(Context context) {
        this(context, null);
    }

    public InlineToastView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public InlineToastView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.view_inline_toast, this, true);
        setVisibility(GONE);
        messageView = findViewById(R.id.toastMessage);
    }

    /**
     * 显示消息，5 秒后自动隐藏。
     *
     * @param message 要显示的文字
     */
    public void showMessage(String message) {
        showMessage(message, DEFAULT_DURATION_MS);
    }

    /**
     * 显示消息，{@code durationMs} 毫秒后自动隐藏。
     *
     * @param message    要显示的文字
     * @param durationMs 显示持续时间（毫秒）
     */
    public void showMessage(String message, long durationMs) {
        handler.removeCallbacks(hideRunnable);
        if (messageView != null) {
            messageView.setText(message != null ? message : "");
        }
        setVisibility(VISIBLE);
        bringToFront();
        handler.postDelayed(hideRunnable, Math.max(0L, durationMs));
    }

    /** 立即隐藏（取消自动隐藏计时器）。 */
    public void hide() {
        handler.removeCallbacks(hideRunnable);
        setVisibility(GONE);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handler.removeCallbacks(hideRunnable);
    }
}
