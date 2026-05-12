package com.capstone.excavator;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.View;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Locale;

/**
 * 速度 + 方向示意：界面由 {@code R.layout.view_speed_direction_indicator} 声明；
 * 速度数字的字体/颜色/粗细见 {@code R.style.SpeedDirectionIndicator_SpeedValue}（{@code res/values/styles_speed_direction_indicator.xml}），
 * 其余背景 / 边距 / 权重等在布局与 drawable 中维护。
 * <p>
 * {@code green_angle} 为朝下、{@code gray_angle} 为朝上；箭头旋转与 {@link #setDirection(int)} 在代码中同步。
 * <p>
 * 使用 {@link #setDirection(int)}、{@link #setSpeed(float)} 或 {@link #setSpeedText(String)} 更新数据。
 */
public class SpeedDirectionIndicatorView extends FrameLayout {

    private static final float SPEED_TEXT_SIZE_NORMAL_SP = 42f;
    private static final float SPEED_TEXT_SIZE_THREE_DIGITS_SP = 28f;
    private static final float SPEED_TEXT_SIZE_LONG_SP = 22f;

    /** 无明确方向：三角与横线均为灰色。 */
    public static final int DIRECTION_NEUTRAL = 0;
    /** 下三角与中间横线为绿色（示意一种行进方向，如前进）。 */
    public static final int DIRECTION_DOWN_HIGHLIGHT = 1;
    /** 上三角与中间横线为绿色（示意相反方向，如后退）。 */
    public static final int DIRECTION_UP_HIGHLIGHT = 2;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({DIRECTION_NEUTRAL, DIRECTION_DOWN_HIGHLIGHT, DIRECTION_UP_HIGHLIGHT})
    public @interface DirectionMode {
    }

    private ImageView arrowTop;
    private ImageView arrowBottom;
    private View speedLine;
    private TextView speedValue;

    private @DirectionMode int direction = DIRECTION_NEUTRAL;
    private String speedText = "0";

    public SpeedDirectionIndicatorView(Context context) {
        this(context, null);
    }

    public SpeedDirectionIndicatorView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SpeedDirectionIndicatorView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.view_speed_direction_indicator, this, true);
        bindViews();
        applyVisualState();
        syncSpeedTextToView();
    }

    private void bindViews() {
        arrowTop = findViewById(R.id.speedArrowTop);
        arrowBottom = findViewById(R.id.speedArrowBottom);
        speedLine = findViewById(R.id.speedLine);
        speedValue = findViewById(R.id.speedValue);
    }

    /** 设置运动方向高亮侧，取 {@link #DIRECTION_NEUTRAL} 等常量。 */
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

    /**
     * 设置速度数值显示；整数显示为不带小数，否则保留一位小数。
     */
    public void setSpeed(float speed) {
        float a = Math.abs(speed);
        if (a < 0.05f) {
            speedText = "0";
        } else if (a >= 10f) {
            // 两位数及以上优先显示整数，避免 "123.4" 这种长文本挤占箭头区。
            speedText = String.valueOf(Math.round(speed));
        } else if (Math.abs(speed - Math.round(speed)) < 0.05f) {
            speedText = String.valueOf(Math.round(speed));
        } else {
            speedText = String.format(Locale.US, "%.1f", speed);
        }
        syncSpeedTextToView();
    }

    /** 直接设置速度文案（例如单位后缀由外部拼好时可传入整串）。 */
    public void setSpeedText(@Nullable String text) {
        speedText = text != null && !text.isEmpty() ? text : "0";
        syncSpeedTextToView();
    }

    public String getSpeedText() {
        return speedText;
    }

    private void syncSpeedTextToView() {
        if (speedValue != null) {
            speedValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, resolveSpeedTextSizeSp(speedText));
            speedValue.setText(speedText);
        }
    }

    private static float resolveSpeedTextSizeSp(String text) {
        int digits = countDigits(text);
        if (digits <= 2) {
            return SPEED_TEXT_SIZE_NORMAL_SP;
        }
        if (digits == 3) {
            return SPEED_TEXT_SIZE_THREE_DIGITS_SP;
        }
        return SPEED_TEXT_SIZE_LONG_SP;
    }

    private static int countDigits(@Nullable String text) {
        if (text == null) {
            return 0;
        }
        int digits = 0;
        for (int i = 0; i < text.length(); i++) {
            if (Character.isDigit(text.charAt(i))) {
                digits++;
            }
        }
        return digits;
    }

    /**
     * 与原先 Canvas 版一致：上槽朝下、下槽朝上；绿/灰资源朝向见类注释。
     */
    private void applyVisualState() {
        if (arrowTop == null || arrowBottom == null || speedLine == null) {
            return;
        }

        boolean downOn = direction == DIRECTION_DOWN_HIGHLIGHT;
        boolean upOn = direction == DIRECTION_UP_HIGHLIGHT;
        boolean lineOn = downOn || upOn;

        if (downOn) {
            arrowTop.setImageResource(R.drawable.green_angle);
            arrowTop.setRotation(0f);
        } else {
            arrowTop.setImageResource(R.drawable.gray_angle);
            arrowTop.setRotation(180f);
        }

        if (upOn) {
            arrowBottom.setImageResource(R.drawable.green_angle);
            arrowBottom.setRotation(180f);
        } else {
            arrowBottom.setImageResource(R.drawable.gray_angle);
            arrowBottom.setRotation(0f);
        }

        speedLine.setBackgroundResource(lineOn
                ? R.drawable.speed_indicator_line_active
                : R.drawable.speed_indicator_line_inactive);
    }
}
