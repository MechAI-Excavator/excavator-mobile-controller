package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.webkit.WebViewAssetLoader;

import org.json.JSONException;
import org.json.JSONObject;

public class ExcavatorPostureView extends FrameLayout {
    private static final String WEB_ENTRY_URL =
            "https://appassets.androidplatform.net/assets/web/index.html";

    private final WebView webView;
    private final WebViewAssetLoader assetLoader;

    private float boomAngle = 0f;
    private float stickAngle = 0f;
    private float bucketAngle = 0f;
    private float cabinPitchAngle = 0f;

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
        setBackgroundColor(Color.TRANSPARENT);

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
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);

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

        webView.loadUrl(WEB_ENTRY_URL);
    }

    public void setAngles(float cabinPitch, float boom, float stick, float bucket) {
        this.cabinPitchAngle = cabinPitch;
        this.boomAngle = boom;
        this.stickAngle = stick;
        this.bucketAngle = bucket;
        postCurrentPayload();
    }

    public void setAngles(float boom, float stick, float bucket) {
        setAngles(0f, boom, stick, bucket);
    }

    public void setArmLengthsFromMm(double boomMm, double stickMm, double bucketMm) {
        if (boomMm <= 0.0 || stickMm <= 0.0 || bucketMm <= 0.0) {
            return;
        }
        boomLengthScale = 1f;
        stickLengthScale = (float) (stickMm / boomMm);
        postCurrentPayload();
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
