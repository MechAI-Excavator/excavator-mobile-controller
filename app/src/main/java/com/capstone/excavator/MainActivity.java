package com.capstone.excavator;

import android.app.AlertDialog;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.skydroid.rcsdk.RCSDKManager;
import com.skydroid.rcsdk.KeyManager;
import com.skydroid.rcsdk.comm.CommListener;
import com.skydroid.rcsdk.common.pipeline.Pipeline;
import com.skydroid.rcsdk.PipelineManager;
import com.skydroid.rcsdk.common.error.SkyException;
import com.skydroid.rcsdk.SDKManagerCallBack;
import com.skydroid.rcsdk.key.RemoteControllerKey;
import com.skydroid.rcsdk.key.AirLinkKey;
import com.skydroid.rcsdk.common.callback.KeyListener;
import com.skydroid.rcsdk.common.callback.CompletionCallbackWith;

import com.skydroid.fpvplayer.FPVWidget;
import com.skydroid.fpvplayer.OnPlayerStateListener;
import com.skydroid.fpvplayer.PlayerType;
import com.skydroid.fpvplayer.ReConnectInterceptor;
import com.skydroid.fpvplayer.RtspTransport;
import android.widget.RelativeLayout;

import eightbitlab.com.blurview.BlurTarget;
import eightbitlab.com.blurview.BlurView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    
    // ── Components ───────────────────────────────────────────────────
    private HeaderBarView headerBar;
    private BottomBarView bottomBar;
    // ── Other UI ─────────────────────────────────────────────────────
    private PostureCardView postureCardView;
    private BlurView postureCardBlur;
    private FPVWidget fpvWidget;
    private MapView mapView;
    private View mapCardContainer;
    private BlurView rightPanelBlur;
    private View videoPlaceholder;
    private TextView btnFloatingToggle;
    private View rightPanelHeader;
    private View rightPanelBody;
    private View rightPanelCollapseArrow;
    private BlurView livePillBlur;
    private View livePillDot;
    private TextView livePillText;
    private ObjectAnimator liveDotBreathAnimator;
    private EmergencyStopOverlayView emergencyStopOverlay;

    // 驾驶模式状态
    private boolean isManualMode = true;
    
    // 数据更新Handler
    private Handler handler;
    private Runnable updateRunnable;
    
    // 摇杆值更新Handler（独立更新）
    private Handler joystickHandler;
    private Runnable joystickUpdateRunnable;
    
    // 模拟数据
    private Random random = new Random();
    private int angleIndex = 0;
    private int angleUpdateCounter = 0; // 用于控制角度更新频率
    
    // 机械臂角度轮换数据
    private List<AngleSet> angleSets = new ArrayList<>();

    // IMU angle solver config (fill in linkage dimensions and IMU offsets if available)
    private final ImuAngleConverter.Config imuAngleConfig = ImuAngleConverter.createDefaultConfig();
    
    // UDP相关
    private Pipeline udpPipeline;
    private boolean useRealData = false; // 是否使用真实UDP数据
    private float realBoomAngle = 0f;
    private float realStickAngle = 0f;
    private float realBucketAngle = 0f;
    private float realCabinPitchAngle = 0f;
    private float realCabinRollAngle = 0f;
    // RTK?????????
    private double realRtkLat = 0.0;
    private double realRtkLon = 0.0;

    private float relativeBoomAngle = 0f; // 结算后角度
    private float relativeStickAngle = 0f;
    private float relativeBucketAngle = 0f;
    
    // UDP数据接收超时相关
    private Handler udpTimeoutHandler;
    private Runnable udpTimeoutRunnable;
    private static final long UDP_TIMEOUT_MS = 5000; // 5秒没收到数据就切换回模拟数据
    private long lastDataReceiveTime = 0;
    // 心跳 RTT 测量相关
    private Handler heartbeatHandler;
    private Runnable heartbeatRunnable;
    private long heartbeatSendTime = 0;           // 最近一次心跳的发送时间戳
    private static final long HEARTBEAT_INTERVAL_MS = 1000; // 心跳间隔 1 秒
    // 心跳帧格式：0xFB 0xFB + 1字节类型(0x01) + 8字节时间戳 + 19字节填充 + CRC(2字节) + 0xFF = 33字节
    private static final byte HEARTBEAT_HEADER_1 = (byte) 0xFB;
    private static final byte HEARTBEAT_HEADER_2 = (byte) 0xFB;
    private static final byte HEARTBEAT_TYPE     = (byte) 0x01;
    
    // 摇杆值
    private int ch1Value = 0; // 右摇杆左右
    private int ch2Value = 0; // 右摇杆上下
    private int ch3Value = 0; // 左摇杆上下
    private int ch4Value = 0; // 左摇杆左右

    // 信号强度相关
    private KeyListener<Integer> keySignalQualityListener;
    private int currentSignalStrength = 0; // 当前信号强度（0-100）
    
    // 视频流地址
    private String currentVideoUrl = "rtsp://192.168.144.25:8554/main.264";
    private static final int REQUEST_SETTINGS = 1001;
    
    // 角度数据类
    private static class AngleSet {
        float boom;
        float stick;
        float bucket;
        
        AngleSet(float boom, float stick, float bucket) {
            this.boom = boom;
            this.stick = stick;
            this.bucket = bucket;
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        // 设置全屏模式
        setFullScreenMode();
        setContentView(R.layout.activity_main);
        initViews();
        initMap();
        initImuAngleConfig();
        applyStoredArmLengthScalesToWebView();
        initAngleSets();
        initSDK();
        startDataUpdates();
        initVideoPlayer();
    }
    
    /**
     * 设置全屏模式（隐藏状态栏和导航栏）
     */
    private void setFullScreenMode() {
        // 使用 WindowCompat 和 WindowInsetsControllerCompat 实现兼容性全屏
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        
        if (windowInsetsController != null) {
            // 隐藏状态栏和导航栏
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
            // 设置沉浸式模式，让内容可以延伸到系统栏区域
            windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
        }

    }
    
    @Override
    public void onBackPressed() {
        if (emergencyStopOverlay != null
                && emergencyStopOverlay.getVisibility() == View.VISIBLE) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // 当窗口获得焦点时，确保全屏模式
            setFullScreenMode();
        }
    }
    
    private void initViews() {
        postureCardView = findViewById(R.id.postureCardView);
        postureCardBlur = findViewById(R.id.postureCardBlur);
        fpvWidget            = findViewById(R.id.fpvWidget);
        mapView              = findViewById(R.id.mapView);
        mapCardContainer     = findViewById(R.id.mapCardContainer);
        rightPanelBlur       = findViewById(R.id.rightPanelBlur);
        rightPanelHeader     = findViewById(R.id.rightPanelHeader);
        rightPanelBody       = findViewById(R.id.rightPanelBody);
        rightPanelCollapseArrow = findViewById(R.id.rightPanelCollapseArrow);
        videoPlaceholder     = findViewById(R.id.videoPlaceholder);
        livePillBlur         = findViewById(R.id.livePillBlur);
        livePillDot          = findViewById(R.id.livePillDot);
        livePillText         = findViewById(R.id.livePillText);

        // Sync right map card height to posture card height after layout.
        if (postureCardView != null && mapCardContainer != null) {
            postureCardView.post(() -> {
                int h = postureCardView.getHeight();
                if (h <= 0) return;
                ViewGroup.LayoutParams lp = mapCardContainer.getLayoutParams();
                if (lp != null && lp.height != h) {
                    lp.height = h;
                    mapCardContainer.setLayoutParams(lp);
                }
            });
        }

        setupOverlayBlurs();
        setupCollapsibleCards();

        // ── Header component ────────────────────────────────────────
        headerBar = findViewById(R.id.headerBar);
        headerBar.setMode(isManualMode ? "手动模式" : "自动模式");

        // ── 急停覆盖层 ───────────────────────────────────────────────
        emergencyStopOverlay = findViewById(R.id.emergencyStopOverlay);
        headerBar.setOnEmergencyStopListener(() -> {
            if (emergencyStopOverlay != null) emergencyStopOverlay.show();
        });
        if (emergencyStopOverlay != null) {
            emergencyStopOverlay.setOnDismissListener(() ->
                    Toast.makeText(this, "急停已解除", Toast.LENGTH_SHORT).show());
        }

        // ── Bottom bar component ─────────────────────────────────────
        bottomBar = findViewById(R.id.bottomBar);

        bottomBar.setOnReconnectListener(this::reconnectVideo);

        bottomBar.setOnSettingsListener(() -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            intent.putExtra("current_url", currentVideoUrl);
            startActivityForResult(intent, REQUEST_SETTINGS);
        });

        bottomBar.setOnLevelListener(() -> {
            Toast.makeText(this, "找平", Toast.LENGTH_SHORT).show();
        });

        bottomBar.setOnTrenchListener(() -> {
            Toast.makeText(this, "挖沟", Toast.LENGTH_SHORT).show();
        });

        bottomBar.setOnSlopeListener(() -> {
            Toast.makeText(this, "修坡", Toast.LENGTH_SHORT).show();
        });

        bottomBar.setOnBarToggleListener(() -> {
            bottomBar.setVisibility(View.GONE);
            if (btnFloatingToggle != null) btnFloatingToggle.setVisibility(View.VISIBLE);
        });

        if (btnFloatingToggle != null) {
            btnFloatingToggle.setOnClickListener(v -> {
                bottomBar.setVisibility(View.VISIBLE);
                btnFloatingToggle.setVisibility(View.GONE);
            });
        }

        // 初始状态：未连接
        setVideoConnected(false);
    }

    private void setupOverlayBlurs() {
        View t = findViewById(R.id.blurTarget);
        if (!(t instanceof BlurTarget)) return;
        BlurTarget target = (BlurTarget) t;

        final float cardRadius = 18f;
        final int cardOverlay = 0x4D808080; // glass gray overlay
        final float liveRadius = 14f;
        final int liveOverlay = 0xAA000000; // black blur overlay

        if (postureCardBlur != null) {
            postureCardBlur.post(() -> postureCardBlur.setupWith(target)
                    .setBlurRadius(cardRadius)
                    .setOverlayColor(cardOverlay));
        }
        if (rightPanelBlur != null) {
            rightPanelBlur.post(() -> rightPanelBlur.setupWith(target)
                    .setBlurRadius(cardRadius)
                    .setOverlayColor(cardOverlay));
        }
        if (livePillBlur != null) {
            livePillBlur.post(() -> livePillBlur.setupWith(target)
                    .setBlurRadius(liveRadius)
                    .setOverlayColor(liveOverlay));
        }
    }

    private void setupCollapsibleCards() {
        // Left: PostureCardView internal header/body
        if (postureCardBlur != null && postureCardView != null) {
            View header = postureCardView.findViewById(R.id.postureHeaderRow);
            View body = postureCardView.findViewById(R.id.postureBody);
            View arrow = postureCardView.findViewById(R.id.postureCollapseArrow);
            setupOneCollapsible(postureCardBlur, header, body, arrow);
        }

        // Right panel
        if (rightPanelBlur != null && rightPanelHeader != null && rightPanelBody != null) {
            setupOneCollapsible(rightPanelBlur, rightPanelHeader, rightPanelBody, rightPanelCollapseArrow);
        }
    }

    private void setupOneCollapsible(View container, View header, View body, View arrow) {
        if (container == null || header == null || body == null) return;

        final boolean[] collapsed = { false };
        final int[] expandedH = { -1 };
        final int[] collapsedH = { -1 };

        container.post(() -> {
            expandedH[0] = container.getHeight();
            collapsedH[0] = header.getHeight();
        });

        Runnable expand = () -> {
            if (!collapsed[0]) return;
            collapsed[0] = false;
            body.setVisibility(View.VISIBLE);
            int from = container.getLayoutParams().height > 0 ? container.getLayoutParams().height : collapsedH[0];
            int to = expandedH[0] > 0 ? expandedH[0] : container.getHeight();
            animateHeight(container, from, to, () -> {});
            if (arrow != null) arrow.animate().rotation(0f).setDuration(180).start();
        };

        Runnable collapse = () -> {
            if (collapsed[0]) return;
            collapsed[0] = true;
            int from = container.getHeight();
            int to = collapsedH[0] > 0 ? collapsedH[0] : header.getHeight();
            animateHeight(container, from, to, () -> body.setVisibility(View.GONE));
            if (arrow != null) arrow.animate().rotation(180f).setDuration(180).start();
        };

        if (arrow != null) {
            arrow.setOnClickListener(v -> {
                if (collapsed[0]) expand.run();
                else collapse.run();
            });
        }

        header.setOnClickListener(v -> {
            if (collapsed[0]) expand.run();
        });
    }

    private void animateHeight(View v, int from, int to, Runnable endAction) {
        if (from == to) {
            if (endAction != null) endAction.run();
            return;
        }
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        ValueAnimator animator = ValueAnimator.ofInt(from, to);
        animator.setDuration(220);
        animator.addUpdateListener(a -> {
            lp.height = (int) a.getAnimatedValue();
            v.setLayoutParams(lp);
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator animation) {
                if (endAction != null) endAction.run();
            }
        });
        animator.start();
    }

    private void initMap() {
        if (mapView == null) return;
        // Fixed GPS location (requirement #1)
        //
        mapView.setFixedLocation(22.87502952106135, 113.48885581740602);
        OverpassMapHelper.loadOfflineMap(this, mapView);
    }

    private void initAngleSets() {
        // 初始化机械臂角度数据，使用绝对值系统
        // 0度=水平，正值=向上，负值=向下
        // 铲斗：正值=展开，负值=收回

        angleSets.add(new AngleSet(- 27.85f, -158.33f, -95.09f));
        angleSets.add(new AngleSet(- 37.85f, -158.33f, -95.09f));
        angleSets.add(new AngleSet(- 47.85f, -158.33f, -95.09f));
        angleSets.add(new AngleSet(- 57.85f, -158.33f, -95.09f));
        angleSets.add(new AngleSet(- 67.85f, -158.33f, -95.09f));
        angleSets.add(new AngleSet(- 77.85f, -158.33f, -95.09f));
        angleSets.add(new AngleSet(- 87.85f, -158.33f, -95.09f));
        angleSets.add(new AngleSet(- 97.85f, -158.33f, -95.09f));
        angleSets.add(new AngleSet(-107.85f, -158.33f, -95.09f));
        angleSets.add(new AngleSet(-117.85f, -158.33f, -95.09f));
        angleSets.add(new AngleSet(-127.85f, -158.33f, -95.09f));
        angleSets.add(new AngleSet(-137.85f, -158.33f, -95.09f));
        angleSets.add(new AngleSet(-147.85f, -158.33f, -95.09f));
        angleSets.add(new AngleSet(-157.85f, -158.33f, -95.09f));
        angleSets.add(new AngleSet(-167.85f, -158.33f, -95.09f));
        angleSets.add(new AngleSet(-177.85f, -158.33f, -95.09f));
        angleSets.add(new AngleSet( 177.85f, -158.33f, -95.09f));
        angleSets.add(new AngleSet( 167.85f, -158.33f, -95.09f));
        angleSets.add(new AngleSet( 157.85f, -158.33f, -95.09f));
        angleSets.add(new AngleSet( 147.85f, -158.33f, -95.09f));
        angleSets.add(new AngleSet( 137.85f, -158.33f, -95.09f));
        angleSets.add(new AngleSet( 127.85f, -158.33f, -95.09f));
        angleSets.add(new AngleSet( 117.85f, -158.33f, -95.09f));
        angleSets.add(new AngleSet( 107.85f, -158.33f, -95.09f));
        angleSets.add(new AngleSet(  97.85f, -158.33f, -95.09f));
        angleSets.add(new AngleSet(  87.85f, -158.33f, -95.09f));
        angleSets.add(new AngleSet(  77.85f, -158.33f, -95.09f));
        angleSets.add(new AngleSet(  67.85f, -158.33f, -95.09f));
        angleSets.add(new AngleSet(  57.85f, -158.33f, -95.09f));
        angleSets.add(new AngleSet(  47.85f, -158.33f, -95.09f));
        angleSets.add(new AngleSet(  37.85f, -158.33f, -95.09f));

        angleSets.add(new AngleSet(- 58.33f,- 27.85f,  -95.09f));
        angleSets.add(new AngleSet(- 58.33f,- 37.85f,  -95.09f));
        angleSets.add(new AngleSet(- 58.33f,- 47.85f,  -95.09f));
        angleSets.add(new AngleSet(- 58.33f,- 57.85f,  -95.09f));
        angleSets.add(new AngleSet(- 58.33f,- 67.85f,  -95.09f));
        angleSets.add(new AngleSet(- 58.33f,- 77.85f,  -95.09f));
        angleSets.add(new AngleSet(- 58.33f,- 87.85f,  -95.09f));
        angleSets.add(new AngleSet(- 58.33f,- 97.85f,  -95.09f));
        angleSets.add(new AngleSet(- 58.33f,-107.85f,  -95.09f));
        angleSets.add(new AngleSet(- 58.33f,-117.85f,  -95.09f));
        angleSets.add(new AngleSet(- 58.33f,-127.85f,  -95.09f));
        angleSets.add(new AngleSet(- 58.33f,-137.85f,  -95.09f));
        angleSets.add(new AngleSet(- 58.33f,-147.85f,  -95.09f));
        angleSets.add(new AngleSet(- 58.33f,-157.85f,  -95.09f));
        angleSets.add(new AngleSet(- 58.33f,-167.85f,  -95.09f));
        angleSets.add(new AngleSet(- 58.33f,-177.85f,  -95.09f));
        angleSets.add(new AngleSet(- 58.33f, 177.85f,  -95.09f));
        angleSets.add(new AngleSet(- 58.33f, 167.85f,  -95.09f));
        angleSets.add(new AngleSet(- 58.33f, 157.85f,  -95.09f));
        angleSets.add(new AngleSet(- 58.33f, 147.85f,  -95.09f));
        angleSets.add(new AngleSet(- 58.33f, 137.85f,  -95.09f));
        angleSets.add(new AngleSet(- 58.33f, 127.85f,  -95.09f));
        angleSets.add(new AngleSet(- 58.33f, 117.85f,  -95.09f));
        angleSets.add(new AngleSet(- 58.33f, 107.85f,  -95.09f));
        angleSets.add(new AngleSet(- 58.33f,  97.85f,  -95.09f));
        angleSets.add(new AngleSet(- 58.33f,  87.85f,  -95.09f));
        angleSets.add(new AngleSet(- 58.33f,  77.85f,  -95.09f));
        angleSets.add(new AngleSet(- 58.33f,  67.85f,  -95.09f));
        angleSets.add(new AngleSet(- 58.33f,  57.85f,  -95.09f));
        angleSets.add(new AngleSet(- 58.33f,  47.85f,  -95.09f));
        angleSets.add(new AngleSet(- 58.33f,  37.85f,  -95.09f));

        angleSets.add(new AngleSet(- 58.33f,  95.09f,- 27.85f));
        angleSets.add(new AngleSet(- 58.33f,  95.09f,- 37.85f));
        angleSets.add(new AngleSet(- 58.33f,  95.09f,- 47.85f));
        angleSets.add(new AngleSet(- 58.33f,  95.09f,- 57.85f));
        angleSets.add(new AngleSet(- 58.33f,  95.09f,- 67.85f));
        angleSets.add(new AngleSet(- 58.33f,  95.09f,- 77.85f));
        angleSets.add(new AngleSet(- 58.33f,  95.09f,- 87.85f));
        angleSets.add(new AngleSet(- 58.33f,  95.09f,- 97.85f));
        angleSets.add(new AngleSet(- 58.33f,  95.09f,-107.85f));
        angleSets.add(new AngleSet(- 58.33f,  95.09f,-117.85f));
        angleSets.add(new AngleSet(- 58.33f,  95.09f,-127.85f));
        angleSets.add(new AngleSet(- 58.33f,  95.09f,-137.85f));
        angleSets.add(new AngleSet(- 58.33f,  95.09f,-147.85f));
        angleSets.add(new AngleSet(- 58.33f,  95.09f,-157.85f));
        angleSets.add(new AngleSet(- 58.33f,  95.09f,-167.85f));
        angleSets.add(new AngleSet(- 58.33f,  95.09f,-177.85f));
        angleSets.add(new AngleSet(- 58.33f,  95.09f, 177.85f));
        angleSets.add(new AngleSet(- 58.33f,  95.09f, 167.85f));
        angleSets.add(new AngleSet(- 58.33f,  95.09f, 157.85f));
        angleSets.add(new AngleSet(- 58.33f,  95.09f, 147.85f));
        angleSets.add(new AngleSet(- 58.33f,  95.09f, 137.85f));
        angleSets.add(new AngleSet(- 58.33f,  95.09f, 127.85f));
        angleSets.add(new AngleSet(- 58.33f,  95.09f, 117.85f));
        angleSets.add(new AngleSet(- 58.33f,  95.09f, 107.85f));
        angleSets.add(new AngleSet(- 58.33f,  95.09f,  97.85f));
        angleSets.add(new AngleSet(- 58.33f,  95.09f,  87.85f));
        angleSets.add(new AngleSet(- 58.33f,  95.09f,  77.85f));
        angleSets.add(new AngleSet(- 58.33f,  95.09f,  67.85f));
        angleSets.add(new AngleSet(- 58.33f,  95.09f,  57.85f));
        angleSets.add(new AngleSet(- 58.33f,  95.09f,  47.85f));
        angleSets.add(new AngleSet(- 58.33f,  95.09f,  37.85f));
        
        // angleSets.add(new AngleSet(0f, 0f, 0f));        // 第1组：初始位置（所有角度0度）
        
        // // 挖掘动作（向下时铲斗展开）
        // angleSets.add(new AngleSet(-25f, -40f, 25f));   // 第2组：挖掘位置（大臂向下25度，小臂向下40度，铲斗展开25度）
        // angleSets.add(new AngleSet(-30f, -50f, 30f));   // 第3组：深挖位置（大臂向下30度，小臂向下50度，铲斗展开30度）
        // angleSets.add(new AngleSet(-15f, -30f, 20f));   // 第4组：浅挖位置（大臂向下15度，小臂向下30度，铲斗展开20度）
        // angleSets.add(new AngleSet(-20f, -35f, 28f));   // 第5组：挖掘并展开（大臂向下20度，小臂向下35度，铲斗展开28度）
        
        // // 举升动作（向上时铲斗收回）
        // angleSets.add(new AngleSet(15f, 20f, -15f));    // 第6组：举升位置（大臂向上15度，小臂向上20度，铲斗收回15度）
        // angleSets.add(new AngleSet(10f, 25f, -10f));    // 第7组：伸展位置（大臂向上10度，小臂向上25度，铲斗收回10度）
        // angleSets.add(new AngleSet(20f, 15f, -20f));    // 第8组：高举位置（大臂向上20度，小臂向上15度，铲斗收回20度）
        // angleSets.add(new AngleSet(5f, 30f, -5f));      // 第9组：前伸位置（大臂向上5度，小臂向上30度，铲斗收回5度）
        
        // // 过渡动作
        // angleSets.add(new AngleSet(-10f, -20f, 15f));   // 第10组：准备挖掘（大臂向下10度，小臂向下20度，铲斗展开15度）
        // angleSets.add(new AngleSet(8f, 18f, -12f));     // 第11组：准备倾倒（大臂向上8度，小臂向上18度，铲斗收回12度）
        // angleSets.add(new AngleSet(-18f, -32f, 22f));   // 第12组：持续挖掘（大臂向下18度，小臂向下32度，铲斗展开22度）
        // angleSets.add(new AngleSet(12f, 22f, -18f));    // 第13组：举升倾倒（大臂向上12度，小臂向上22度，铲斗收回18度）
    }
    
    /**
     * 初始化IMU解算配置
     */
    private void initImuAngleConfig() {
        imuAngleConfig.name = "cat303cr";
        imuAngleConfig.type = "cat303cr";

        ImuPreferences.Params p = ImuPreferences.load(this);

        ImuAngleConverter.Dimensions dim = imuAngleConfig.dimensions;
        dim.chassisWidth  = 0.0;
        dim.chassisLength = 0.0;
        dim.trackWidth    = 0.0;
        dim.boomLength           = p.boomLength;
        dim.stickLength          = p.stickLength;
        dim.bucketLength         = p.bucketLength;
        dim.bucketAngleOffsetDeg = p.bucketAngleOffsetDeg;

        ImuAngleConverter.CylinderJointMapDimensions cyl = imuAngleConfig.cylinder;
        cyl.boomL2 = p.boomL2;
        cyl.boomL3 = p.boomL3;
        cyl.boomL4 = p.boomL4;
        cyl.boomL5 = p.boomL5;
        cyl.boomL6 = p.boomL6;
        cyl.boomL7 = p.boomL7;

        cyl.stickL2 = p.stickL2;
        cyl.stickL3 = p.stickL3;
        cyl.stickL4 = p.stickL4;
        cyl.stickL5 = p.stickL5;
        cyl.stickL6 = p.stickL6;
        cyl.stickL7 = p.stickL7;

        cyl.bucketL2  = p.bucketL2;
        cyl.bucketL3  = p.bucketL3;
        cyl.bucketL4  = p.bucketL4;
        cyl.bucketL5  = p.bucketL5;
        cyl.bucketL6  = p.bucketL6;
        cyl.bucketL7  = p.bucketL7;
        cyl.bucketL9  = p.bucketL9;
        cyl.bucketL10 = p.bucketL10;

        ImuAngleConverter.ImuInstallationOffset offsets = imuAngleConfig.imuOffsets;
        offsets.boomImuOffsetDeg   = p.boomImuOffsetDeg;
        offsets.stickImuOffsetDeg  = p.stickImuOffsetDeg;
        offsets.bucketImuOffsetDeg = p.bucketImuOffsetDeg;

        if (postureCardView != null) {
            postureCardView.setBucketAngleOffsetDeg((float) dim.bucketAngleOffsetDeg);
        }
    }

    /** 从 SharedPreferences 恢复臂长比例，WebView onPageFinished 后会随 payload 下发。 */
    private void applyStoredArmLengthScalesToWebView() {
        if (postureCardView == null) {
            return;
        }
        float boom = ArmLengthPreferences.getBoomScale(this);
        float stick = ArmLengthPreferences.getStickScale(this);
        postureCardView.setLengthScales(boom, stick);
    }

    /**
     * 初始化视频播放器
     */
    private void initVideoPlayer() {
        if (fpvWidget == null) return;

        fpvWidget.setUsingMediaCodec(true);
        fpvWidget.setUrl(currentVideoUrl);
        fpvWidget.setPlayerType(PlayerType.ONLY_SKY);
        fpvWidget.setRtspTranstype(RtspTransport.AUTO);

        // 禁止自动重连：连接失败后不再自动重试
        fpvWidget.setReConnectInterceptor(() -> false);

        // 监听连接状态，更新 LIVE 指示器
        fpvWidget.setOnPlayerStateListener(new OnPlayerStateListener() {
            @Override
            public void onConnected() {
                runOnUiThread(() -> setVideoConnected(true));
            }

            @Override
            public void onDisconnect() {
                runOnUiThread(() -> setVideoConnected(false));
            }

            @Override
            public void onReadFrame(com.skydroid.fpvplayer.ffmpeg.FrameInfo frameInfo) {
            }
        });

        fpvWidget.start();
    }

    private void setVideoConnected(boolean connected) {
        if (bottomBar != null) bottomBar.setLiveStatus(connected);
        if (headerBar != null) headerBar.setConnected(connected);
        if (videoPlaceholder != null)
            videoPlaceholder.setVisibility(connected ? View.GONE : View.VISIBLE);
        updateLivePill(connected);
    }

    private void updateLivePill(boolean connected) {
        if (livePillDot == null || livePillText == null) return;
        if (connected) {
            livePillDot.setBackgroundResource(R.drawable.live_dot_red);
            livePillText.setText("实时 · 主相机");
            startLiveDotBreathing();
        } else {
            stopLiveDotBreathing();
            livePillDot.setAlpha(1f);
            livePillDot.setBackgroundResource(R.drawable.live_dot_off);
            livePillText.setText("离线 · 主相机");
        }
    }

    private void startLiveDotBreathing() {
        if (livePillDot == null) return;
        if (liveDotBreathAnimator != null) return;
        liveDotBreathAnimator = ObjectAnimator.ofFloat(livePillDot, View.ALPHA, 0.35f, 1f);
        liveDotBreathAnimator.setDuration(900);
        liveDotBreathAnimator.setRepeatCount(ValueAnimator.INFINITE);
        liveDotBreathAnimator.setRepeatMode(ValueAnimator.REVERSE);
        liveDotBreathAnimator.start();
    }

    private void stopLiveDotBreathing() {
        if (liveDotBreathAnimator != null) {
            liveDotBreathAnimator.cancel();
            liveDotBreathAnimator = null;
        }
    }

    private void reconnectVideo() {
        if (fpvWidget == null) return;
        fpvWidget.stop();
        fpvWidget.setUrl(currentVideoUrl);
        fpvWidget.start();
        Toast.makeText(this, "正在重新连接...", Toast.LENGTH_SHORT).show();
    }
    
    
    /**
     * 更新视频地址
     */
    private void updateVideoUrl(String url) {
        if (fpvWidget != null) {
            try {
                // 停止当前播放
                fpvWidget.stop();
                
                // 记录并设置新地址
                currentVideoUrl = url;
                fpvWidget.setUrl(url);
                
                // 重新开始播放
                fpvWidget.start();
                
                Toast.makeText(this, "视频地址已更新", Toast.LENGTH_SHORT).show();
                Log.d("MainActivity", "视频地址更新为: " + url);
            } catch (Exception e) {
                Log.e("MainActivity", "更新视频地址失败: " + e.getMessage(), e);
                Toast.makeText(this, "更新失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void startDataUpdates() {
        // 主数据更新Handler（1秒更新一次）
        handler = new Handler(Looper.getMainLooper());
        
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateAllData();
                handler.postDelayed(this, 50); // 每秒更新一次
            }
        };
        
        handler.post(updateRunnable);
        
        // 摇杆值更新Handler（100ms更新一次）
        joystickHandler = new Handler(Looper.getMainLooper());
        
        joystickUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                updateJoystickValues(); // 只更新摇杆值
                joystickHandler.postDelayed(this, 50); // 每50ms更新一次
            }
        };
        
        joystickHandler.post(joystickUpdateRunnable);
    }
    
    private void updateAllData() {
        // 更新连接信息
        updateConnectionInfo();
        
        // 更新机械臂角度（无论是否有接收机数据，都走统一的解算与显示）
        updateAngles();
        
        // 注意：摇杆值更新已独立到100ms循环中，不在这里更新
        
        // 更新定位信息（带小幅随机波动）
        updatePositioning();
        
        // 更新挖掘深度（带小幅随机波动）
        updateDigDepth();
    }
    
    private void updateConnectionInfo() {
        // 真实数据模式下延迟由 onReadData 的包间隔实时更新，无需在此覆盖
        if (!useRealData) {
            if (bottomBar != null) bottomBar.setDelay(-1);
        }
    }
    
    /**
     * 更新信号强度显示
     */
    private void updateSignalDisplay() {
        if (bottomBar != null) bottomBar.setSignal(currentSignalStrength);
    }

    private void updateAngles() {
        float rawBoom;
        float rawStick;
        float rawBucket;
        float rawCabinPitch;
        float rawCabinRoll;

        if (useRealData) {
            // 使用真实UDP数据（原始IMU角度）
            rawBoom = realBoomAngle;
            rawStick = realStickAngle;
            rawBucket = realBucketAngle;
            rawCabinPitch = realCabinPitchAngle;
            rawCabinRoll = realCabinRollAngle;
        }
        //  else {
        //     if (angleSets.isEmpty()) {
        //         return;
        //     }
        //     // 使用模拟数据（每1秒切换一次角度）
        //     angleUpdateCounter++;
        //     if (angleUpdateCounter >= 1) {
        //         angleIndex = (angleIndex + 1) % angleSets.size();
        //         angleUpdateCounter = 0;
        //     }

        //     // 轮换角度数据（原始IMU角度）
        //     AngleSet currentSet = angleSets.get(angleIndex);
        //     rawBoom = currentSet.boom;
        //     rawStick = currentSet.stick;
        //     rawBucket = currentSet.bucket;

        //     // 模拟座舱 IMU：与 angleIndex 同步平滑变化，幅值不超过 ±45°（再大易像翻车）
        //     double phase = angleIndex * 0.18;
        //     rawCabinPitch = (float) (45.0 * Math.sin(phase));
        //     rawCabinRoll = (float) (45.0 * Math.cos(phase * 1.07));
        // }

        // 解算相对角度（统一入口，模拟/真实都使用）
        ImuAngleConverter.RelativeAngles relative = ImuAngleConverter.toRelativeAngles(
                rawBoom = 0,
                rawStick = 0,
                rawBucket = 0,
                rawCabinPitch = 0,
                rawCabinRoll = 0,
                imuAngleConfig
        );
        relativeBoomAngle = relative.boomDeg;
        relativeStickAngle = relative.stickDeg;
        relativeBucketAngle = relative.bucketDeg;

        // 更新挖机姿态（使用相对角度）
        if (postureCardView != null) {
            postureCardView.setAngles(
                    rawCabinPitch,
                    rawCabinRoll,
                    relativeBoomAngle,
                    relativeStickAngle,
                    relativeBucketAngle
            );
        }
        // 推送到 BottomBarView 组件
        if (bottomBar != null) {
            bottomBar.setAngles(relativeBoomAngle, relativeStickAngle, relativeBucketAngle,
                    rawCabinPitch, rawCabinRoll);
        }
    }
    
    private void updatePositioning() {
        // RTK
        double lat;
        double lon;

        if (useRealData) {
            lat = realRtkLat;
            lon = realRtkLon;
        } else {
            // RTK默认位置
            lat = 28.2416021;
            lon = 113.0938459;
        }

        if (mapView != null) mapView.setFixedLocation(lat, lon);
        if (bottomBar != null) bottomBar.setRtkLatLon(lat, lon);
    }

    private void updateDigDepth() {
        double depth = 3.0 + random.nextDouble() * 0.5;
        if (bottomBar != null) bottomBar.setDepth(depth);
    }
    
    /**
     * 更新摇杆值
     */
    private void updateJoystickValues() {
        KeyManager.INSTANCE.get(RemoteControllerKey.INSTANCE.getKeyChannels(), 
            new CompletionCallbackWith<int[]>() {
                @Override
                public void onSuccess(int[] value) {
                    // 区间【-450，450】
                    // value 是摇杆值数组
                    if (value != null && value.length >= 4) {
                        // 减去1500作为初始值
                        ch1Value = value[0] - 1500; // 右摇杆左右
                        ch2Value = value[1] - 1500; // 右摇杆上下
                        ch3Value = value[2] - 1500; // 左摇杆上下
                        ch4Value = value[3] - 1500; // 左摇杆左右

                        runOnUiThread(() -> {
                            if (bottomBar != null) {
                                bottomBar.setJoystickLeft(ch4Value, ch3Value);
                                bottomBar.setJoystickRight(ch1Value, -ch2Value);
                            }
                        });
                    }
                }

                @Override
                public void onFailure(SkyException e) {
                    Log.e("MainActivity", "摇杆值获取失败: " + (e != null ? e.getMessage() : "未知错误"));
                }
            });
    }
    
    /**
     * 初始化SDK
     */
    private void initSDK() {
        // TODO 初始化SDK,初始化一次即可
        RCSDKManager.INSTANCE.initSDK(this, new SDKManagerCallBack() {
            @Override
            public void onRcConnected() {
                Log.d("MainActivity", "遥控器连接成功");
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "遥控器连接成功", Toast.LENGTH_SHORT).show();
                });
                // 遥控器连接成功后，创建UDP管道
                createUDPPipeline();
            }
            
            @Override
            public void onRcConnectFail(SkyException e) {
                Log.e("MainActivity", "遥控器连接失败: " + (e != null ? e.getMessage() : "未知错误"));
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "遥控器连接失败", Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onRcDisconnect() {
                Log.e("MainActivity", "遥控器断开连接");
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "遥控器断开连接", Toast.LENGTH_SHORT).show();
                });
            }
        });
        
        // 设置在主线程回调
        RCSDKManager.INSTANCE.setMainThreadCallBack(true);
        
        // 连接到遥控器
        RCSDKManager.INSTANCE.connectToRC();
        
        // 注册信号强度监听器
        keySignalQualityListener = new KeyListener<Integer>() {
            @Override
            public void onValueChange(Integer oldValue, Integer newValue) {
                // newValue 是信号强度百分比 (0-100)
                currentSignalStrength = newValue != null ? newValue : 0;
                // 在主线程更新UI
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateSignalDisplay();
                    }
                });
            }
        };
        KeyManager.INSTANCE.listen(AirLinkKey.INSTANCE.getKeySignalQuality(), keySignalQualityListener);
    }
    
    /**
     * 创建UDP管道
     */
    private void createUDPPipeline() {
        // 创建UDP管道：本地端口14551，发送到127.0.0.1:14552
        udpPipeline = PipelineManager.INSTANCE.createUDPPipeline(14551, "127.0.0.1", 14552);
        
        if (udpPipeline != null) {
            // 设置通信监听器
            udpPipeline.setOnCommListener(new CommListener() {
                @Override
                public void onConnectSuccess() {
                    Log.d("UDP", "UDP管道连接成功");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // 连接成功但不立即切换，等待收到数据后再切换
                            // useRealData 保持 false，直到收到第一个数据包
                            lastDataReceiveTime = System.currentTimeMillis();
                            Toast.makeText(MainActivity.this, "UDP连接成功，等待数据...", Toast.LENGTH_SHORT).show();
                            startHeartbeat(); // 开始定时发送心跳帧测量 RTT
                        }
                    });
                }
                
                @Override
                public void onConnectFail(SkyException e) {
                    Log.e("UDP", "UDP管道连接失败: " + (e != null ? e.getMessage() : "未知错误"));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            useRealData = false; // 连接失败，使用模拟数据
                            Toast.makeText(MainActivity.this, "UDP连接失败，使用模拟数据", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                
                @Override
                public void onDisconnect() {
                    Log.d("UDP", "UDP管道断开连接");
                    stopHeartbeat();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            useRealData = false; // 切换回模拟数据
                            Toast.makeText(MainActivity.this, "UDP断开连接", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                
                @Override
                public void onReadData(byte[] data) {
                    // 接收到的UDP数据（33字节）
                    if (data != null) {
                        Log.d("UDP", "收到数据，长度: " + data.length);

                        // 优先判断是否为心跳回包（header = 0xFB 0xFB）
                        if (data.length == 33
                                && data[0] == HEARTBEAT_HEADER_1
                                && data[1] == HEARTBEAT_HEADER_2
                                && data[2] == HEARTBEAT_TYPE) {
                            // 从帧中还原发送时间戳（big-endian，frame[3..10]）
                            long sendTs = 0;
                            for (int i = 0; i < 8; i++) {
                                sendTs = (sendTs << 8) | (data[3 + i] & 0xFF);
                            }
                            if (sendTs > 0) {
                                int rtt = (int) (System.currentTimeMillis() - sendTs);
                                Log.d("RTT", "收到心跳回包，RTT=" + rtt + "ms");
                                runOnUiThread(() -> {
                                    if (bottomBar != null) bottomBar.setDelay(rtt);
                                });
                            }
                            return; // 心跳回包不进入业务数据解析
                        }
                        
                        if (data.length == 33) {
                            // 更新最后接收数据的时间
                            long now = System.currentTimeMillis();
                            lastDataReceiveTime = now;
                            
                            // 如果之前是模拟数据，切换到真实数据
                            if (!useRealData) {
                                useRealData = true;
                                Log.d("UDP", "收到UDP数据，切换到真实数据模式");
                            }
                            
                            // 取消之前的超时检查
                            if (udpTimeoutHandler != null && udpTimeoutRunnable != null) {
                                udpTimeoutHandler.removeCallbacks(udpTimeoutRunnable);
                            }
                            
                            // 重新启动超时检查
                            startUDPTimeoutCheck();
                            
                            // 解析数据
                            IMUDataParser.parseData(data, new IMUDataParser.ParseResultCallbackV2() {
                                @Override
                                public void onParseSuccess(IMUDataParser.ParsedData parsed) {
                                    // ????IMU?????????????
                                    realBoomAngle = parsed.boomAngle;
                                    realStickAngle = parsed.stickAngle;
                                    realBucketAngle = parsed.bucketAngle;
                                    realCabinPitchAngle = parsed.cabinPitchAngle;
                                    realCabinRollAngle = parsed.cabinRollAngle;
                                    // ??RTK?????????
                                    realRtkLat = parsed.rtkLat;
                                    realRtkLon = parsed.rtkLon;
                                }
                                @Override
                                public void onParseError(String error) {
                                    Log.e("UDP", "数据解析失败: " + error);
                                }
                            });
                        } else {
                            Log.w("UDP", "数据长度不正确，期望33字节，实际: " + data.length);
                        }
                    }
                }
            });
            
            // 连接UDP管道
            PipelineManager.INSTANCE.connectPipeline(udpPipeline);
        } else {
            Log.e("UDP", "创建UDP管道失败");
            useRealData = false; // 创建失败，使用模拟数据
            Toast.makeText(this, "创建UDP管道失败，使用模拟数据", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 构建心跳帧（33字节）。
     * 格式：0xFB 0xFB | type(1) | timestamp(8, big-endian) | padding(19) | CRC16(2) | 0xFF
     * 机载端识别到 header=0xFB 0xFB 且 type=0x01 时，原样将整帧回传给 App。
     */
    private byte[] buildHeartbeatFrame(long timestamp) {
        byte[] frame = new byte[33];
        frame[0] = HEARTBEAT_HEADER_1;
        frame[1] = HEARTBEAT_HEADER_2;
        // data 区（28字节，偏移2~29）
        frame[2] = HEARTBEAT_TYPE;
        // 8字节时间戳（big-endian），放在 data[1..8]（即 frame[3..10]）
        for (int i = 0; i < 8; i++) {
            frame[3 + i] = (byte) ((timestamp >> (56 - 8 * i)) & 0xFF);
        }
        // 剩余 19 字节填充 0（frame[11..29] 已为 0）
        // CRC 计算范围：data 区 28 字节（frame[2..29]）
        byte[] dataForCrc = new byte[28];
        System.arraycopy(frame, 2, dataForCrc, 0, 28);
        int crc = CRC16Modbus.calculateCRC16Modbus(dataForCrc);
        byte[] crcBytes = CRC16Modbus.crcToBytes(crc);
        frame[30] = crcBytes[0];
        frame[31] = crcBytes[1];
        frame[32] = (byte) 0xFF;
        return frame;
    }

    /**
     * 启动心跳定时器，每秒向机载端发送一次心跳帧。
     * 机载端收到后原样回传，App 在 onReadData 中计算 RTT。
     */
    private void startHeartbeat() {
        if (heartbeatHandler == null) {
            heartbeatHandler = new Handler(Looper.getMainLooper());
        }
        stopHeartbeat();
        heartbeatRunnable = new Runnable() {
            @Override
            public void run() {
                if (udpPipeline != null && udpPipeline.isConnected()) {
                    heartbeatSendTime = System.currentTimeMillis();
                    udpPipeline.writeData(buildHeartbeatFrame(heartbeatSendTime));
                    Log.d("RTT", "心跳已发送，时间戳: " + heartbeatSendTime);
                }
                heartbeatHandler.postDelayed(this, HEARTBEAT_INTERVAL_MS);
            }
        };
        heartbeatHandler.postDelayed(heartbeatRunnable, HEARTBEAT_INTERVAL_MS);
    }

    /** 停止心跳定时器并清零发送时间戳 */
    private void stopHeartbeat() {
        if (heartbeatHandler != null && heartbeatRunnable != null) {
            heartbeatHandler.removeCallbacks(heartbeatRunnable);
        }
        heartbeatSendTime = 0;
    }

    /**
     * 启动UDP数据接收超时检查
     * 如果长时间没收到数据，自动切换回模拟数据
     */
    private void startUDPTimeoutCheck() {
        if (udpTimeoutHandler == null) {
            udpTimeoutHandler = new Handler(Looper.getMainLooper());
        }
        
        if (udpTimeoutRunnable != null) {
            udpTimeoutHandler.removeCallbacks(udpTimeoutRunnable);
        }
        
        udpTimeoutRunnable = new Runnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                if (useRealData && (currentTime - lastDataReceiveTime) > UDP_TIMEOUT_MS) {
                    // 超过5秒没收到数据，切换回模拟数据
                    useRealData = false;
                    stopHeartbeat(); // 停止心跳，不再测量延迟
                    Log.w("UDP", "UDP数据接收超时，切换回模拟数据");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "UDP数据超时，使用模拟数据", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else if (useRealData) {
                    // 继续检查
                    udpTimeoutHandler.postDelayed(this, 1000); // 每秒检查一次
                }
            }
        };
        
        udpTimeoutHandler.postDelayed(udpTimeoutRunnable, UDP_TIMEOUT_MS);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SETTINGS && resultCode == RESULT_OK && data != null) {
            String newUrl = data.getStringExtra("video_url");
            if (newUrl != null && !newUrl.isEmpty()) {
                updateVideoUrl(newUrl);
            }
            applyStoredArmLengthScalesToWebView();
            // Re-load IMU config from SharedPreferences whenever settings are saved
            initImuAngleConfig();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // 停止视频播放
        if (fpvWidget != null) {
            fpvWidget.stop();
        }
        
        // 停止主数据更新
        if (handler != null && updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
        }
        
        // 停止摇杆值更新
        if (joystickHandler != null && joystickUpdateRunnable != null) {
            joystickHandler.removeCallbacks(joystickUpdateRunnable);
        }
        
        // 停止UDP超时检查
        if (udpTimeoutHandler != null && udpTimeoutRunnable != null) {
            udpTimeoutHandler.removeCallbacks(udpTimeoutRunnable);
        }

        // 停止心跳
        stopHeartbeat();
        
        // 断开UDP管道
        if (udpPipeline != null) {
            PipelineManager.INSTANCE.disconnectPipeline(udpPipeline);
            udpPipeline = null;
        }
        
        // 断开遥控器连接
        RCSDKManager.INSTANCE.disconnectRC();
        
        // 取消信号强度监听
        if (keySignalQualityListener != null) {
            KeyManager.INSTANCE.cancelListen(keySignalQualityListener);
            keySignalQualityListener = null;
        }
    }
}
