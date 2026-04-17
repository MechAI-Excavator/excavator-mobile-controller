package com.capstone.excavator;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import android.util.Log;
import androidx.core.content.ContextCompat;
import androidx.webkit.WebViewAssetLoader;

import org.json.JSONException;
import org.json.JSONObject;

public class ExcavatorPostureView extends FrameLayout {
    private static final String WEB_ENTRY_URL =
            "https://appassets.androidplatform.net/assets/web/index.html";

    /** 与 gauge_card_bg 一致，便于与主界面侧栏卡片统一 */
    private static final float CORNER_RADIUS_DP = 10f;

    private final WebView webView;
    private final WebViewAssetLoader assetLoader;

    private float boomAngle = 0f;
    private float stickAngle = 0f;
    private float bucketAngle = 0f;
    private float cabinPitchAngle = 0f;
    private float cabinRollAngle = 0f;

    private float boomLengthScale = 1f;
    private float stickLengthScale = 1f;
    private float bucketAngleOffsetDeg = 0f;
    private boolean pageReady = false;

    public ExcavatorPostureView(Context context) {
        this(context, null);
    }

    public ExcavatorPostureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ExcavatorPostureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        float cornerRadiusPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                CORNER_RADIUS_DP,
                context.getResources().getDisplayMetrics()
        );
        Drawable cardBg = ContextCompat.getDrawable(context, R.drawable.gauge_card_bg);
        if (cardBg != null) {
            setBackground(cardBg.mutate());
        } else {
            setBackgroundColor(Color.TRANSPARENT);
        }
        setClipToOutline(true);
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                int w = view.getWidth();
                int h = view.getHeight();
                if (w <= 0 || h <= 0) {
                    return;
                }
                outline.setRoundRect(0, 0, w, h, cornerRadiusPx);
            }
        });

        assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(context))
                .addPathHandler("/res/", new WebViewAssetLoader.ResourcesPathHandler(context))
                .build();

        webView = new WebView(context);
        initWebView();
        addView(webView, new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView() {
        webView.clearCache(true);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.setOverScrollMode(OVER_SCROLL_NEVER);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(
                    WebView view, WebResourceRequest request
            ) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                pageReady = true;
                postCurrentPayload();
            }
        });

        webView.loadUrl(WEB_ENTRY_URL + "?v=" + System.currentTimeMillis());
    }

    /**
     * 只更新动臂相对角度；座舱 pitch/roll 保持当前值（默认均为 0）。
     */
    public void setAngles(float boom, float stick, float bucket) {
        this.boomAngle = boom;
        this.stickAngle = stick;
        this.bucketAngle = bucket;
        postCurrentPayload();
    }

    /**
     * 座舱姿态（IMU）+ 动臂相对角度，与 {@link MainActivity#updateAngles()} 入参顺序一致。
     */
    public void setAngles(float cabinPitch, float cabinRoll, float boom, float stick, float bucket) {
        this.cabinPitchAngle = cabinPitch;
        this.cabinRollAngle = cabinRoll;
        this.boomAngle = boom;
        this.stickAngle = stick;
        this.bucketAngle = bucket;
        postCurrentPayload();
    }

    public void setArmLengthsFromMm(double boomMm, double stickMm, double bucketMm) {
        if (boomMm <= 0.0 || stickMm <= 0.0 || bucketMm <= 0.0) {
            return;
        }
        setLengthScales(1f, (float) (stickMm / boomMm));
    }

    /**
     * 与 Web 端 {@code state.lengths} 一致（对应 {@code setLengths} / {@code applyExcavatorPayload}）。
     */
    public void setLengthScales(float boomScale, float stickScale) {
        boomLengthScale = clampLengthScale(boomScale);
        stickLengthScale = clampLengthScale(stickScale);
        postCurrentPayload();
    }

    private static float clampLengthScale(float v) {
        if (!Float.isFinite(v)) {
            return 1f;
        }
        return Math.max(0.1f, Math.min(10f, v));
    }

    public void setBucketAngleOffsetDeg(float offsetDeg) {
        this.bucketAngleOffsetDeg = offsetDeg;
        postCurrentPayload();
    }

    private void postCurrentPayload() {
        if (!pageReady) {
            return;
        }
        if (!webView.isAttachedToWindow()) {
            return;
        }

        JSONObject payload = new JSONObject();
        try {
            JSONObject main = new JSONObject();
            main.put("pitch", cabinPitchAngle);
            main.put("roll", cabinRollAngle);
            payload.put("main", main);

            JSONObject lengths = new JSONObject();
            lengths.put("boom", boomLengthScale);
            lengths.put("stick", stickLengthScale);
            payload.put("lengths", lengths);

            JSONObject joints = new JSONObject();
            joints.put("boom", new JSONObject().put("z", boomAngle));
            joints.put("stick", new JSONObject().put("z", stickAngle));
            joints.put("bucket", new JSONObject().put("z", bucketAngle + bucketAngleOffsetDeg));
            payload.put("joints", joints);
            Log.d("ExcavatorPostureView", "payload: " + payload.toString());
        } catch (JSONException ignored) {
            return;
        }

        String payloadStr = payload.toString()
                .replace("\\", "\\\\")
                .replace("'", "\\'");
        String js = "window.applyExcavatorPayload && window.applyExcavatorPayload(JSON.parse('"
                + payloadStr + "'));";
        webView.post(() -> webView.evaluateJavascript(js, null));
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        pageReady = false;
        webView.stopLoading();
        webView.destroy();
    }
}
