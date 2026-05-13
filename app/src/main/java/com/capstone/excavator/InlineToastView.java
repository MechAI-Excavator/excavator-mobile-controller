package com.capstone.excavator;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

/**
 * 通用内联 Toast（叠加在父布局中，无系统 Toast 限制）。
 * <p>
 * 在父布局 XML 中声明（GONE），调用 {@link #showMessage(String)} 或
 * {@link #showMessage(String, long)} 显示短时提示；
 * 下载等长任务使用 {@link #showDownloadProgress(String, String, long)}，样式更醒目。
 * <p>
 * 样式由 {@code view_inline_toast.xml} 与 drawable 维护。
 */
public class InlineToastView extends FrameLayout {

    private static final long DEFAULT_DURATION_MS = 5000L;

    private LinearLayout toastCard;
    private TextView titleView;
    private ProgressBar progressBar;
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
        toastCard = findViewById(R.id.toastCard);
        titleView = findViewById(R.id.toastTitle);
        progressBar = findViewById(R.id.toastProgress);
        messageView = findViewById(R.id.toastMessage);
    }

    /**
     * 显示消息，5 秒后自动隐藏。
     */
    public void showMessage(String message) {
        showMessage(message, DEFAULT_DURATION_MS);
    }

    /**
     * 显示消息，{@code durationMs} 毫秒后自动隐藏。
     */
    public void showMessage(String message, long durationMs) {
        handler.removeCallbacks(hideRunnable);
        applySimpleAppearance();
        if (messageView != null) {
            messageView.setText(message != null ? message : "");
        }
        setVisibility(VISIBLE);
        bringToFront();
        handler.postDelayed(hideRunnable, Math.max(0L, durationMs));
    }

    /**
     * 下载等长任务：标题 + 不确定进度条 + 副文案，卡片更大、对比更强。
     */
    public void showDownloadProgress(String title, String detail, long durationMs) {
        handler.removeCallbacks(hideRunnable);
        applyDownloadAppearance();
        if (titleView != null) {
            titleView.setText(title != null ? title : "");
        }
        if (messageView != null) {
            messageView.setText(detail != null ? detail : "");
        }
        setVisibility(VISIBLE);
        bringToFront();
        handler.postDelayed(hideRunnable, Math.max(0L, durationMs));
    }

    /** 立即隐藏（取消自动隐藏计时器）。 */
    public void hide() {
        handler.removeCallbacks(hideRunnable);
        applySimpleAppearance();
        setVisibility(GONE);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handler.removeCallbacks(hideRunnable);
    }

    private void applySimpleAppearance() {
        if (titleView != null) {
            titleView.setVisibility(GONE);
        }
        if (progressBar != null) {
            progressBar.setVisibility(GONE);
        }
        if (toastCard != null) {
            toastCard.setMinimumWidth(0);
            toastCard.setMinimumHeight(0);
            int w = dpToPx(186f);
            ViewGroup.LayoutParams lp = toastCard.getLayoutParams();
            lp.width = w;
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            toastCard.setLayoutParams(lp);
            toastCard.setBackgroundResource(R.drawable.inline_toast_bg);
            toastCard.setElevation(dpToPx(4f));
        }
        if (messageView != null) {
            setMessageTopMargin(0);
            messageView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
            messageView.setTextColor(Color.WHITE);
        }
    }

    private void applyDownloadAppearance() {
        if (titleView != null) {
            titleView.setVisibility(VISIBLE);
        }
        if (progressBar != null) {
            progressBar.setVisibility(VISIBLE);
        }
        if (toastCard != null) {
            int minW = getResources().getDimensionPixelSize(R.dimen.inline_toast_download_min_width);
            toastCard.setMinimumWidth(minW);
            ViewGroup.LayoutParams lp = toastCard.getLayoutParams();
            lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            toastCard.setLayoutParams(lp);
            toastCard.setBackgroundResource(R.drawable.inline_toast_download_bg);
            toastCard.setElevation(dpToPx(20f));
        }
        if (messageView != null) {
            setMessageTopMargin(dpToPx(10f));
            messageView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
            messageView.setTextColor(Color.parseColor("#E6FFFFFF"));
        }
    }

    private void setMessageTopMargin(int px) {
        if (messageView == null) {
            return;
        }
        ViewGroup.MarginLayoutParams mlp =
                (ViewGroup.MarginLayoutParams) messageView.getLayoutParams();
        if (mlp != null) {
            mlp.topMargin = px;
            messageView.setLayoutParams(mlp);
        }
    }

    private int dpToPx(float dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        ));
    }

}
