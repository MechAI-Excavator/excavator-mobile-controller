package com.capstone.excavator;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Header bar component.
 *
 * Props in  : setMode(String), setConnected(boolean)
 * Self-driven: clock (starts on attach, stops on detach)
 *
 * Usage in XML:
 *   <com.capstone.excavator.HeaderBarView
 *       android:id="@+id/headerBar"
 *       android:layout_width="match_parent"
 *       android:layout_height="wrap_content"
 *       android:layout_gravity="top"
 *       android:elevation="10dp" />
 */
public class HeaderBarView extends LinearLayout {

    private TextView tvModeStatus;
    private TextView tvHeaderConnection;
    private TextView tvCurrentTime;
    private View connectionDot;
    private TextView btnEmergencyStop;
    private Runnable onEmergencyStopListener;

    private final Handler clockHandler = new Handler(Looper.getMainLooper());
    private final Runnable clockRunnable = new Runnable() {
        @Override
        public void run() {
            if (tvCurrentTime != null) {
                tvCurrentTime.setText(
                        new SimpleDateFormat("HH:mm", Locale.getDefault())
                                .format(new Date()));
            }
            clockHandler.postDelayed(this, 1000);
        }
    };

    public HeaderBarView(Context context) {
        this(context, null);
    }

    public HeaderBarView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HeaderBarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);
        setBackgroundColor(0x4D000000);
        inflate(context, R.layout.view_header_bar, this);
        tvCurrentTime      = findViewById(R.id.tvCurrentTime);
        btnEmergencyStop   = findViewById(R.id.btnEmergencyStop);
        if (btnEmergencyStop != null) {
            btnEmergencyStop.setOnClickListener(v -> {
                if (onEmergencyStopListener != null) onEmergencyStopListener.run();
            });
        }
    }

    /** 外部注册急停按钮点击回调。 */
    public void setOnEmergencyStopListener(Runnable listener) {
        this.onEmergencyStopListener = listener;
    }

    // ── Public API ──────────────────────────────────────────────────

    /** 更新模式文字，例如 "手动模式" / "自动模式" */
    public void setMode(String text) {
        if (tvModeStatus != null) tvModeStatus.setText(text);
    }

    /** 更新连接状态显示（绿色已连接 / 红色未连接） */
    public void setConnected(boolean connected) {
        int color = connected ? 0xFF4CAF50 : 0xFFFF6B6B;
        if (tvHeaderConnection != null) {
            tvHeaderConnection.setText(connected ? "已连接" : "未连接");
            tvHeaderConnection.setTextColor(color);
        }
        if (connectionDot != null) {
            GradientDrawable dot = new GradientDrawable();
            dot.setShape(GradientDrawable.OVAL);
            dot.setColor(color);
            connectionDot.setBackground(dot);
        }
    }

    // ── Lifecycle ────────────────────────────────────────────────────

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        clockHandler.post(clockRunnable);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        clockHandler.removeCallbacks(clockRunnable);
    }
}
