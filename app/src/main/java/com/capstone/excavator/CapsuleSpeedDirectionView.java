package com.capstone.excavator;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.core.widget.ImageViewCompat;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Locale;

/**
 * 横向胶囊：青铜竖渐变底、左为「右指三角 + 竖线 + 左指三角」、右侧大号速度数；
 * 语义与 {@link SpeedDirectionIndicatorView} 的方向常量一致。
 */
public class CapsuleSpeedDirectionView extends FrameLayout {

    private static final int RED = Color.parseColor("#E53935");
    private static final int ARROW_DIM = Color.parseColor("#5D4A42");
    private static final int DIVIDER_DIM = Color.parseColor("#4A3A33");

    public static final int DIRECTION_NEUTRAL = SpeedDirectionIndicatorView.DIRECTION_NEUTRAL;
    public static final int DIRECTION_DOWN_HIGHLIGHT = SpeedDirectionIndicatorView.DIRECTION_DOWN_HIGHLIGHT;
    public static final int DIRECTION_UP_HIGHLIGHT = SpeedDirectionIndicatorView.DIRECTION_UP_HIGHLIGHT;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({DIRECTION_NEUTRAL, DIRECTION_DOWN_HIGHLIGHT, DIRECTION_UP_HIGHLIGHT})
    public @interface DirectionMode {
    }

    private ImageView arrowForward;
    private ImageView arrowBack;
    private View divider;
    private TextView speedValue;

    private @DirectionMode int direction = DIRECTION_NEUTRAL;
    private String speedText = "0";

    public CapsuleSpeedDirectionView(Context context) {
        this(context, null);
    }

    public CapsuleSpeedDirectionView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CapsuleSpeedDirectionView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.view_capsule_speed_direction, this, true);
        bindViews();
        applyVisualState();
        syncSpeedTextToView();
    }

    private void bindViews() {
        arrowForward = findViewById(R.id.capsuleArrowForward);
        arrowBack = findViewById(R.id.capsuleArrowBack);
        divider = findViewById(R.id.capsuleDivider);
        speedValue = findViewById(R.id.capsuleSpeedValue);
    }

    public void setDirection(@DirectionMode int mode) {
        if (mode != DIRECTION_NEUTRAL && mode != DIRECTION_DOWN_HIGHLIGHT && mode != DIRECTION_UP_HIGHLIGHT) {
            mode = DIRECTION_NEUTRAL;
        }
        if (this.direction != mode) {
            this.direction = mode;
            applyVisualState();
        }
    }

    public int getDirection() {
        return direction;
    }

    public void setSpeed(float speed) {
        float a = Math.abs(speed);
        if (a < 0.05f) {
            speedText = "0";
        } else if (Math.abs(speed - Math.round(speed)) < 0.05f) {
            speedText = String.valueOf(Math.round(speed));
        } else {
            speedText = String.format(Locale.US, "%.1f", speed);
        }
        syncSpeedTextToView();
    }

    public void setSpeedText(@Nullable String text) {
        speedText = text != null && !text.isEmpty() ? text : "0";
        syncSpeedTextToView();
    }

    public String getSpeedText() {
        return speedText;
    }

    private void syncSpeedTextToView() {
        if (speedValue != null) {
            speedValue.setText(speedText);
        }
    }

    /**
     * {@link #DIRECTION_DOWN_HIGHLIGHT}：右指三角 + 分隔线为红，左指三角为暗色（示意一侧为主方向）。<br>
     * {@link #DIRECTION_UP_HIGHLIGHT}：对称高亮左指侧。<br>
     * {@link #DIRECTION_NEUTRAL}：三角与分隔线均为暗色。
     */
    private void applyVisualState() {
        if (arrowForward == null || arrowBack == null || divider == null) {
            return;
        }

        boolean forwardOn = direction == DIRECTION_DOWN_HIGHLIGHT;
        boolean backOn = direction == DIRECTION_UP_HIGHLIGHT;
        boolean lineOn = forwardOn || backOn;

        ImageViewCompat.setImageTintList(arrowForward,
                android.content.res.ColorStateList.valueOf(forwardOn ? RED : ARROW_DIM));
        ImageViewCompat.setImageTintList(arrowBack,
                android.content.res.ColorStateList.valueOf(backOn ? RED : ARROW_DIM));
        divider.setBackgroundColor(lineOn ? RED : DIVIDER_DIM);
    }
}
