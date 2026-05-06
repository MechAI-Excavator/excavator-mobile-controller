package com.capstone.excavator;

import android.content.Context;
import android.util.AttributeSet;

public class Excavator2DPostureView extends ExcavatorPostureView {
    private static final String WEB_ENTRY_URL_2D =
            "https://appassets.androidplatform.net/assets/web/twoDExcavator/index.html";

    public Excavator2DPostureView(Context context) {
        super(context);
    }

    public Excavator2DPostureView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Excavator2DPostureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected String getWebEntryUrl() {
        return WEB_ENTRY_URL_2D;
    }
}
