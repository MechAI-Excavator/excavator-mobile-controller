package com.capstone.excavator;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.webkit.WebSettings;

import java.util.Locale;

public class ExcavatorMapWeb extends ExcavatorPostureView {
    private static final String WEB_ENTRY_URL_2D =
            "https://appassets.androidplatform.net/assets/web/excavator-map/index.html";

    public ExcavatorMapWeb(Context context) {
        this(context, null);
    }

    public ExcavatorMapWeb(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ExcavatorMapWeb(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // 与 PostureCardView 内 Web 一致：去掉 gauge_card_bg 叠层，让底层 BlurView 毛玻璃露在留白处
        setEmbedded(true);
    }

    @Override
    protected String getWebEntryUrl() {
        return WEB_ENTRY_URL_2D;
    }

    @Override
    protected void applyExtraWebSettings(WebSettings settings) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
    }

    /**
     * 与 {@link MapView#setFixedLocation} 对应：通知 assets 内天地图页更新标记位置。
     */
    public void setFixedLocation(double lat, double lon) {
        if (!Double.isFinite(lat) || !Double.isFinite(lon)) {
            return;
        }
        String js = String.format(Locale.US,
                "(function(){if(window.TiandituBridge&&window.TiandituBridge.setVehiclePose){"
                        + "window.TiandituBridge.setVehiclePose(%f,%f,0);}})();",
                lat, lon);
        postJavascriptToWebView(js);
    }
}
