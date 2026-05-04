package com.capstone.excavator;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

/**
 * 三档运动状态：停止 / 底盘 / 铲斗。样式由 {@code view_motion_mode_segment.xml} 与
 * {@code styles_motion_mode.xml}、drawable、colors 描述；外部通过 {@link #setSelectedIndex(int)} 切换选中项。
 */
public class MotionModeSegmentView extends FrameLayout {

    public static final int INDEX_STOP = 0;
    public static final int INDEX_CHASSIS = 1;
    public static final int INDEX_BUCKET = 2;

    private int selectedIndex = INDEX_STOP;

    private LinearLayout[] rows;
    private View[] dots;
    private TextView[] labels;

    @Nullable
    private OnIndexChangeListener indexChangeListener;

    public interface OnIndexChangeListener {
        void onIndexChanged(int newIndex);
    }

    public MotionModeSegmentView(Context context) {
        this(context, null);
    }

    public MotionModeSegmentView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MotionModeSegmentView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.view_motion_mode_segment, this, true);
        bind();
        wireClicks();
        applySelection();
    }

    private void bind() {
        rows = new LinearLayout[]{
                findViewById(R.id.motion_mode_row0),
                findViewById(R.id.motion_mode_row1),
                findViewById(R.id.motion_mode_row2),
        };
        dots = new View[]{
                findViewById(R.id.motion_mode_dot0),
                findViewById(R.id.motion_mode_dot1),
                findViewById(R.id.motion_mode_dot2),
        };
        labels = new TextView[]{
                findViewById(R.id.motion_mode_label0),
                findViewById(R.id.motion_mode_label1),
                findViewById(R.id.motion_mode_label2),
        };
    }

    private void wireClicks() {
        for (int i = 0; i < rows.length; i++) {
            final int idx = i;
            rows[i].setOnClickListener(v -> setSelectedIndex(idx));
        }
    }

    /** 当前选中下标：0 停止，1 底盘，2 铲斗。 */
    public void setSelectedIndex(int index) {
        if (index < 0 || index > INDEX_BUCKET) {
            index = INDEX_STOP;
        }
        if (selectedIndex == index) {
            return;
        }
        selectedIndex = index;
        applySelection();
        if (indexChangeListener != null) {
            indexChangeListener.onIndexChanged(selectedIndex);
        }
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setOnIndexChangeListener(@Nullable OnIndexChangeListener listener) {
        this.indexChangeListener = listener;
    }

    private void applySelection() {
        Context ctx = getContext();
        int idle = ContextCompat.getColor(ctx, R.color.motion_mode_idle);
        for (int i = 0; i < 3; i++) {
            boolean sel = (i == selectedIndex);
            rows[i].setBackgroundResource(rowBackgroundRes(sel, i));
            int dotRes = sel ? dotDrawableRes(i) : R.drawable.motion_dot_idle;
            dots[i].setBackgroundResource(dotRes);
            int textColor = sel ? ContextCompat.getColor(ctx, accentColorRes(i)) : idle;
            labels[i].setTextColor(textColor);
        }
    }

    private static int rowBackgroundRes(boolean selected, int rowIndex) {
        if (!selected) {
            return R.drawable.motion_mode_row_unselected;
        }
        switch (rowIndex) {
            case INDEX_STOP:
                return R.drawable.motion_mode_row_selected_stop;
            case INDEX_CHASSIS:
                return R.drawable.motion_mode_row_selected_chassis;
            case INDEX_BUCKET:
            default:
                return R.drawable.motion_mode_row_selected_bucket;
        }
    }

    private static int dotDrawableRes(int rowIndex) {
        switch (rowIndex) {
            case INDEX_STOP:
                return R.drawable.motion_dot_stop;
            case INDEX_CHASSIS:
                return R.drawable.motion_dot_chassis;
            case INDEX_BUCKET:
            default:
                return R.drawable.motion_dot_bucket;
        }
    }

    private static int accentColorRes(int rowIndex) {
        switch (rowIndex) {
            case INDEX_STOP:
                return R.color.motion_mode_stop;
            case INDEX_CHASSIS:
                return R.color.motion_mode_chassis;
            case INDEX_BUCKET:
            default:
                return R.color.motion_mode_bucket;
        }
    }
}
