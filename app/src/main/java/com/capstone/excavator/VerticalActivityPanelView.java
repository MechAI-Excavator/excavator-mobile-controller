package com.capstone.excavator;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

/**
 * 垂直独立进度条容器（顶部标题 + 单向进度）。
 * 通过 {@link #getGauge()} 拿到内部 {@link SingleBarGaugeView}，
 * 再调用 {@code setRangeMax} / {@code setValue} 驱动 UI。
 */
public class VerticalActivityPanelView extends FrameLayout {

    public VerticalActivityPanelView(Context context) {
        this(context, null);
    }

    public VerticalActivityPanelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.view_vertical_activity_panel, this, true);
    }

    /** 返回内部 {@link SingleBarGaugeView}，供外部调用 {@code setRangeMax} / {@code setValue}。 */
    public SingleBarGaugeView getGauge() {
        return findViewById(R.id.verticalActivityGauge);
    }
}
