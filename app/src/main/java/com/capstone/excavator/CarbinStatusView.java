package com.capstone.excavator;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.Locale;

/**
 * 驾驶舱姿态卡片组件。
 *
 * Props in: setCabinAngles(pitch, roll)
 *
 * Usage in XML:
 *   <com.capstone.excavator.CarbinStatusView
 *       android:id="@+id/carbinStatusView"
 *       android:layout_width="match_parent"
 *       android:layout_height="wrap_content" />
 */
public class CarbinStatusView extends LinearLayout {

    private TextView tvCabinPitchAngle;
    private TextView tvCabinRollAngle;
    private TextView tvCabinLatLon;
    private AttitudeIndicatorView attitudeIndicatorView;

    public CarbinStatusView(Context context) {
        this(context, null);
    }

    public CarbinStatusView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CarbinStatusView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);
        setClipToOutline(true);
        int pv = dp(context, 10);
        int ph = dp(context, 12);
        setPadding(ph, pv, ph, pv);

        inflate(context, R.layout.view_carbin_status, this);

        // inflate 之后才能 find
        tvCabinPitchAngle     = findViewById(R.id.tvCabinPitchAngle);
        tvCabinRollAngle      = findViewById(R.id.tvCabinRollAngle);
        tvCabinLatLon         = findViewById(R.id.tvCabinLatLon);
        attitudeIndicatorView = findViewById(R.id.attitudeIndicatorView);
    }

    // ── Public API ────────────────────────────────────────────────

    /** 更新驾驶舱姿态角度（单位 °） */
    public void setCabinAngles(float pitch, float roll) {
        if (tvCabinPitchAngle != null)
            tvCabinPitchAngle.setText(String.format(Locale.getDefault(), "%.1f°", pitch));
        if (tvCabinRollAngle != null)
            tvCabinRollAngle.setText(String.format(Locale.getDefault(), "%.1f°", roll));
        if (attitudeIndicatorView != null)
            attitudeIndicatorView.setAngles(pitch, roll);
    }

    /** 更新经纬度 */
    public void setCabinLatLon(double lat, double lon) {
        if (lat == 0.0 && lon == 0.0) {
            tvCabinLatLon.setText("--");
            return;
        }

        String latString = String.format(Locale.getDefault(), "%.6f", lat);
        String lonString = String.format(Locale.getDefault(), "%.6f", lon);
        String latFloat = latString.substring(0, 7);
        String lonFloat = lonString.substring(0, 7);
        if (tvCabinLatLon != null)
            tvCabinLatLon.setText(String.format(Locale.getDefault(), "%s, %s", latFloat, lonFloat));
    }

    // ── Helper ────────────────────────────────────────────────────

    private static int dp(Context ctx, int value) {
        return Math.round(value * ctx.getResources().getDisplayMetrics().density);
    }
}
