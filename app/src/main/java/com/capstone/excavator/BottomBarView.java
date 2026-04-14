package com.capstone.excavator;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

/**
 * Bottom status bar component.
 *
 * Data in  (call from MainActivity):
 *   setAngles(), setRtkLatLon(), setDepth(), setDelay(), setSignal(),
 *   setLiveStatus(), setJoystickLeft(), setJoystickRight()
 *
 * Events out (implement interfaces and pass in):
 *   setOnReconnectListener()  – user tapped ↻
 *   setOnSettingsListener()   – user tapped SET
 *   setOnBarToggleListener()  – user tapped ▼ to collapse
 *
 * The parent (MainActivity) owns the floating ▲ button and
 * controls this view's visibility when collapse/expand happens.
 *
 * Usage in XML:
 *   <com.capstone.excavator.BottomBarView
 *       android:id="@+id/bottomBar"
 *       android:layout_width="match_parent"
 *       android:layout_height="wrap_content"
 *       android:layout_gravity="bottom" />
 */
public class BottomBarView extends LinearLayout {

    // ── Event interface ──────────────────────────────────────────────

    public interface OnBarToggleListener {
        /** Called when the user taps ▼. Parent should hide this view and show the float button. */
        void onCollapse();
    }

    // ── Internal views ───────────────────────────────────────────────

    private TextView tvBoomAngle;
    private TextView tvStickAngle;
    private TextView tvBucketAngle;
    private TextView tvCabinPitchAngle;
    private TextView tvCabinRollAngle;
    private TextView tvRtkLatLon;
    private TextView tvDigDepth;
    private TextView tvVideoLink;
    private TextView tvRcSignal;
    private View liveIndicatorDot;
    private TextView tvLiveStatus;
    private JoystickIndicatorView joystickLeft;
    private JoystickIndicatorView joystickRight;

    // ── Callbacks ────────────────────────────────────────────────────

    private Runnable onReconnectListener;
    private Runnable onSettingsListener;
    private OnBarToggleListener onBarToggleListener;

    // ── Constructors ─────────────────────────────────────────────────

    public BottomBarView(Context context) {
        this(context, null);
    }

    public BottomBarView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BottomBarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);
        inflate(context, R.layout.view_bottom_bar, this);

        tvBoomAngle       = findViewById(R.id.tvBoomAngle);
        tvStickAngle      = findViewById(R.id.tvStickAngle);
        tvBucketAngle     = findViewById(R.id.tvBucketAngle);
        tvCabinPitchAngle = findViewById(R.id.tvCabinPitchAngle);
        tvCabinRollAngle  = findViewById(R.id.tvCabinRollAngle);
        tvRtkLatLon       = findViewById(R.id.tvRtkLatLon);
        tvDigDepth        = findViewById(R.id.tvDigDepth);
        tvVideoLink       = findViewById(R.id.tvVideoLink);
        tvRcSignal        = findViewById(R.id.tvRcSignal);
        liveIndicatorDot  = findViewById(R.id.liveIndicatorDot);
        tvLiveStatus      = findViewById(R.id.tvLiveStatus);
        joystickLeft      = findViewById(R.id.joystickLeft);
        joystickRight     = findViewById(R.id.joystickRight);

        View btnReconnect = findViewById(R.id.btnReconnect);
        View btnSettings  = findViewById(R.id.btnSettings);
        View btnToggleBar = findViewById(R.id.btnToggleBar);

        if (btnReconnect != null)
            btnReconnect.setOnClickListener(v -> { if (onReconnectListener != null) onReconnectListener.run(); });
        if (btnSettings != null)
            btnSettings.setOnClickListener(v ->  { if (onSettingsListener != null)  onSettingsListener.run();  });
        if (btnToggleBar != null)
            btnToggleBar.setOnClickListener(v -> { if (onBarToggleListener != null) onBarToggleListener.onCollapse(); });
    }

    // ── Data setters ─────────────────────────────────────────────────

    /** 更新五路角度显示（相对角度，单位 °） */
    public void setAngles(float boom, float stick, float bucket,
                          float cabinPitch, float cabinRoll) {
        setText(tvBoomAngle,       "%.2f°", boom);
        setText(tvStickAngle,      "%.2f°", stick);
        setText(tvBucketAngle,     "%.2f°", bucket);
        setText(tvCabinPitchAngle, "%.2f°", cabinPitch);
        setText(tvCabinRollAngle,  "%.2f°", cabinRoll);
    }

    /** 更新 RTK 经纬度 */
    public void setRtkLatLon(double lat, double lon) {
        if (tvRtkLatLon != null)
            tvRtkLatLon.setText(String.format(Locale.getDefault(), "%.6f, %.6f", lat, lon));
    }

    /** 更新挖掘深度（单位 m） */
    public void setDepth(double depth) {
        if (tvDigDepth != null)
            tvDigDepth.setText(String.format(Locale.getDefault(), "%.2f m", depth));
    }

    /** 更新视频延迟（单位 ms） */
    public void setDelay(int ms) {
        if (tvVideoLink != null) tvVideoLink.setText("⏱ Delay: " + ms + "ms");
    }

    /** 更新遥控信号强度（0‑100） */
    public void setSignal(int percent) {
        if (tvRcSignal != null) tvRcSignal.setText("📶 Signal: " + percent + "%");
    }

    /** 更新 LIVE 指示灯 */
    public void setLiveStatus(boolean connected) {
        if (tvLiveStatus != null) {
            tvLiveStatus.setText(connected ? "LIVE" : "OFFLINE");
            tvLiveStatus.setTextColor(connected ? Color.WHITE : Color.parseColor("#FF6B6B"));
        }
        if (liveIndicatorDot != null) {
            GradientDrawable dot = new GradientDrawable();
            dot.setShape(GradientDrawable.OVAL);
            dot.setColor(connected
                    ? Color.parseColor("#00E676")
                    : Color.parseColor("#FF6B6B"));
            liveIndicatorDot.setBackground(dot);
        }
    }

    /** 更新左摇杆示意图 */
    public void setJoystickLeft(int x, int y) {
        if (joystickLeft != null) joystickLeft.setValues(x, y);
    }

    /** 更新右摇杆示意图 */
    public void setJoystickRight(int x, int y) {
        if (joystickRight != null) joystickRight.setValues(x, y);
    }

    // ── Callback setters ─────────────────────────────────────────────

    public void setOnReconnectListener(Runnable listener) {
        this.onReconnectListener = listener;
    }

    public void setOnSettingsListener(Runnable listener) {
        this.onSettingsListener = listener;
    }

    public void setOnBarToggleListener(OnBarToggleListener listener) {
        this.onBarToggleListener = listener;
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private void setText(TextView tv, String fmt, float value) {
        if (tv != null) tv.setText(String.format(Locale.getDefault(), fmt, value));
    }
}
