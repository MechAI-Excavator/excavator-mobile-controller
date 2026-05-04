package com.capstone.excavator;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

/** 主界面右侧活动条容器，内容由 {@code R.layout.view_right_acitivty_panel} 定义。 */
public class RightActivityPanelView extends FrameLayout {

    public RightActivityPanelView(Context context) {
        this(context, null);
    }

    public RightActivityPanelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.view_right_acitivty_panel, this, true);
    }
}
