package com.capstone.excavator;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

/**
 * 横向中心度量面板容器：inflate {@link R.layout#view_center_activity_panel}，
 * 刻度由 {@link CenterGaugeStripView} 绘制。
 */
public class CenterActivityPanelView extends FrameLayout {

    private CenterGaugeStripView gaugeStrip;


    public CenterActivityPanelView(Context context) {
        this(context, null);
    }

    public CenterActivityPanelView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CenterActivityPanelView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setBackgroundResource(R.drawable.center_activity_panel_bg);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setClipToOutline(true);
            setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        }

        LayoutInflater.from(context).inflate(R.layout.view_center_activity_panel, this, true);
        gaugeStrip = findViewById(R.id.centerGaugeStrip);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CenterGaugeStripView, defStyleAttr, 0);
            try {
                if (a.hasValue(R.styleable.CenterGaugeStripView_centerGaugeLeftLabel)) {
                    String fl = a.getString(R.styleable.CenterGaugeStripView_centerGaugeLeftLabel);
                    gaugeStrip.setLeftLabel(fl != null ? fl : "左");
                }
                if (a.hasValue(R.styleable.CenterGaugeStripView_centerGaugeRightLabel)) {
                    String fr = a.getString(R.styleable.CenterGaugeStripView_centerGaugeRightLabel);
                    gaugeStrip.setRightLabel(fr != null ? fr : "右");
                }
                if (a.hasValue(R.styleable.CenterGaugeStripView_centerGaugeRangeMax)) {
                    gaugeStrip.setRangeMax(a.getFloat(R.styleable.CenterGaugeStripView_centerGaugeRangeMax, 1f));
                }
            } finally {
                a.recycle();
            }
        }
    }

    /** 供需要直接操作刻度层时使用 */
    public CenterGaugeStripView getGaugeStrip() {
        return gaugeStrip;
    }

    public void setRangeMax(float maxAbs) {
        gaugeStrip.setRangeMax(maxAbs);
    }

    public float getRangeMax() {
        return gaugeStrip.getRangeMax();
    }

    public void setValue(float v) {
        gaugeStrip.setValue(v);
    }

    public float getValue() {
        return gaugeStrip.getValue();
    }


    public void setLeftLabel(String text) {
        gaugeStrip.setLeftLabel(text);
    }

    public void setRightLabel(String text) {
        gaugeStrip.setRightLabel(text);
    }
}
