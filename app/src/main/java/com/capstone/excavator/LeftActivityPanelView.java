package com.capstone.excavator;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

/** 主界面左侧活动条容器，内容由 {@code R.layout.view_left_acitivty_panel} 定义。 */
public class LeftActivityPanelView extends FrameLayout {

    public LeftActivityPanelView(Context context) {
        this(context, null);
    }

    public LeftActivityPanelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.view_left_acitivty_panel, this, true);
    }
}
