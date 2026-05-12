package com.capstone.excavator;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Locale;

import eightbitlab.com.blurview.BlurTarget;
import eightbitlab.com.blurview.BlurView;

/**
 * 横向胶囊：青铜竖渐变底、左为「右指三角 + 竖线 + 左指三角」、右侧大号速度数；
 * 方向高亮由 XML（自定义 state + ColorStateList / selector）驱动，语义与
 * {@link SpeedDirectionIndicatorView} 的方向常量一致。
 */
public class CapsuleSpeedDirectionView extends FrameLayout {

    public static final int DIRECTION_NEUTRAL = SpeedDirectionIndicatorView.DIRECTION_NEUTRAL;
    public static final int DIRECTION_DOWN_HIGHLIGHT = SpeedDirectionIndicatorView.DIRECTION_DOWN_HIGHLIGHT;
    public static final int DIRECTION_UP_HIGHLIGHT = SpeedDirectionIndicatorView.DIRECTION_UP_HIGHLIGHT;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({DIRECTION_NEUTRAL, DIRECTION_DOWN_HIGHLIGHT, DIRECTION_UP_HIGHLIGHT})
    public @interface DirectionMode {
    }

    private TextView speedValue;
    private BlurView capsuleSpeedPillBlur;

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
        capsuleSpeedPillBlur = findViewById(R.id.capsuleSpeedPillBlur);
        speedValue = findViewById(R.id.capsuleSpeedValue);
        setupBlur();
        syncSpeedTextToView();
        refreshDrawableState();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setupBlur();
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] state = super.onCreateDrawableState(extraSpace + 1);
        if (direction == DIRECTION_DOWN_HIGHLIGHT) {
            mergeDrawableStates(state, new int[]{R.attr.state_capsule_forward});
        } else if (direction == DIRECTION_UP_HIGHLIGHT) {
            mergeDrawableStates(state, new int[]{R.attr.state_capsule_backward});
        }
        return state;
    }

    public void setDirection(@DirectionMode int mode) {
        if (mode != DIRECTION_NEUTRAL && mode != DIRECTION_DOWN_HIGHLIGHT && mode != DIRECTION_UP_HIGHLIGHT) {
            mode = DIRECTION_NEUTRAL;
        }
        if (this.direction != mode) {
            this.direction = mode;
            refreshDrawableState();
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

    private void setupBlur() {
        if (capsuleSpeedPillBlur == null) return;
        capsuleSpeedPillBlur.post(() -> {
            try {
                View targetView = getRootView().findViewById(R.id.blurTarget);
                if (!(targetView instanceof BlurTarget)) return;
                capsuleSpeedPillBlur.setupWith((BlurTarget) targetView)
                        .setBlurRadius(18f)
                        .setOverlayColor(0x4D808080);
                capsuleSpeedPillBlur.setClipToOutline(true);
                capsuleSpeedPillBlur.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
            } catch (Throwable ignored) {
                // Blur is decorative; keep the speed indicator usable if setup fails.
            }
        });
    }
}
