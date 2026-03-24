package com.example.myapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    
    // UI组件
    private TextView tvBoomAngle;
    private TextView tvStickAngle;
    private TextView tvBucketAngle;
    private TextView tvCabinPitchAngle;
    private TextView tvCabinRollAngle;
    private TextView tvLatitude;
    private TextView tvLongitude;
    private TextView tvDigDepth;
    private TextView tvVideoLink;
    private TextView tvRcSignal;
    
    // private ProgressBar progressDigDepth;
    
    private ExcavatorPostureView excavatorPostureView;
    private FPVWidget fpvWidget;
    private MapView mapView;

    // 视频连接状态 UI
    private View liveIndicatorDot;
    private TextView tvLiveStatus;
    private TextView btnReconnect;
    private View videoPlaceholder;
    
    // 角度仪表盘
    private AngleGaugeView gaugeBoom;
    private AngleGaugeView gaugeStick;
    private AngleGaugeView gaugeBucket;
    
    private Button btnLights;
    private Button btnHorn;
    private Button btnModeSwitch;
    private Button btnHome;
    private Button btnStop;
    
    // 视频地址编辑相关
    private EditText etVideoUrl;
    private Button btnUpdateVideoUrl;
    
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
    private float relativeBoomAngle = 0f; // 结算后角度
    private float relativeStickAngle = 0f;
    private float relativeBucketAngle = 0f;
    
    // UDP数据接收超时相关
    private Handler udpTimeoutHandler;
    private Runnable udpTimeoutRunnable;
    private static final long UDP_TIMEOUT_MS = 5000; // 5秒没收到数据就切换回模拟数据
    private long lastDataReceiveTime = 0;
    
    // 摇杆值
    private int ch1Value = 0; // 右摇杆左右
    private int ch2Value = 0; // 右摇杆上下
    private int ch3Value = 0; // 左摇杆上下
    private int ch4Value = 0; // 左摇杆左右

    // 摇杆示意图
    private JoystickIndicatorView joystickLeft;
    private JoystickIndicatorView joystickRight;
    
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
        super.onCreate(savedInstanceState);
        
        // 设置全屏模式
        setFullScreenMode();

        setContentView(R.layout.activity_main);
        
        initViews();
        initMap();
        initImuAngleConfig();
        initAngleSets();
//        initButtons();
        initSDK();
        startDataUpdates();
        initVideoPlayer();
        initVideoUrlEditor();
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
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // 当窗口获得焦点时，确保全屏模式
            setFullScreenMode();
        }
    }
    
    private void initViews() {
        tvBoomAngle = findViewById(R.id.tvBoomAngle);
        tvStickAngle = findViewById(R.id.tvStickAngle);
        tvBucketAngle = findViewById(R.id.tvBucketAngle);
        tvCabinPitchAngle = findViewById(R.id.tvCabinPitchAngle);
        tvCabinRollAngle = findViewById(R.id.tvCabinRollAngle);
        tvDigDepth = findViewById(R.id.tvDigDepth);
        tvVideoLink = findViewById(R.id.tvVideoLink);
        tvRcSignal = findViewById(R.id.tvRcSignal);
        
        // progressDigDepth = findViewById(R.id.progressDigDepth);
        
        excavatorPostureView = findViewById(R.id.excavatorPostureView);
        fpvWidget = findViewById(R.id.fpvWidget);
        mapView = findViewById(R.id.mapView);
        
        // 视频地址编辑相关
        etVideoUrl = findViewById(R.id.etVideoUrl);
        btnUpdateVideoUrl = findViewById(R.id.btnUpdateVideoUrl);
        
        // 角度仪表盘
        gaugeBoom = findViewById(R.id.gaugeBoom);
        gaugeStick = findViewById(R.id.gaugeStick);
        gaugeBucket = findViewById(R.id.gaugeBucket);

        // 摇杆示意图
        joystickLeft = findViewById(R.id.joystickLeft);
        joystickRight = findViewById(R.id.joystickRight);

        // 视频连接状态 UI
        liveIndicatorDot = findViewById(R.id.liveIndicatorDot);
        tvLiveStatus = findViewById(R.id.tvLiveStatus);
        btnReconnect = findViewById(R.id.btnReconnect);
        videoPlaceholder = findViewById(R.id.videoPlaceholder);
        if (btnReconnect != null) {
            btnReconnect.setOnClickListener(v -> reconnectVideo());
        }
        // 初始状态设为未连接，等播放器回调后再更新
        setVideoConnected(false);

        // 设置按钮
        View btnSettings = findViewById(R.id.btnSettings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                intent.putExtra("current_url", currentVideoUrl);
                startActivityForResult(intent, REQUEST_SETTINGS);
            });
        }

        // 设置仪表盘标签
        if (gaugeBoom != null) gaugeBoom.setLabel("BOOM:");
        if (gaugeStick != null) gaugeStick.setLabel("STICK:");
        if (gaugeBucket != null) gaugeBucket.setLabel("BUCKET:");
    }

    private void initMap() {
        if (mapView == null) return;
        // Fixed GPS location (requirement #1)
        mapView.setFixedLocation(22.7372757, 113.5264751);
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

        ImuAngleConverter.Dimensions dim = imuAngleConfig.dimensions;
        dim.chassisWidth = 0.0;
        dim.chassisLength = 0.0;
        dim.trackWidth = 0.0;
        dim.boomLength = 2207.86;
        dim.stickLength = 1261.0;
        dim.bucketLength = 709.164447;
        dim.bucketAngleOffsetDeg = Math.toDegrees(-1.566985);

        ImuAngleConverter.CylinderJointMapDimensions cyl = imuAngleConfig.cylinder;
        cyl.boomL2 = 1191.0;
        cyl.boomL3 = 308.0;
        cyl.boomL4 = 275.0;
        cyl.boomL5 = 140.0;
        cyl.boomL6 = 2207.86;
        cyl.boomL7 = 1079.0;

        cyl.stickL2 = 1104.0;
        cyl.stickL3 = 286.0;
        cyl.stickL4 = 1464.0;
        cyl.stickL5 = 2207.86;
        cyl.stickL6 = 1261.0;
        cyl.stickL7 = 1521.37;

        cyl.bucketL2 = 281.0;
        cyl.bucketL3 = 1082.0;
        cyl.bucketL4 = 1085.0;
        cyl.bucketL5 = 261.57;
        cyl.bucketL6 = 1261.0;
        cyl.bucketL7 = 176.0;
        cyl.bucketL9 = 256.0;
        cyl.bucketL10 = 200.0;

        ImuAngleConverter.ImuInstallationOffset offsets = imuAngleConfig.imuOffsets;
        offsets.boomImuOffsetDeg = Math.toDegrees(-0.4578);
        offsets.stickImuOffsetDeg = 0.0;
        offsets.bucketImuOffsetDeg = 0.0;

        if (excavatorPostureView != null) {
            excavatorPostureView.setArmLengthsFromMm(
                    dim.boomLength,
                    dim.stickLength,
                    dim.bucketLength
            );
            excavatorPostureView.setBucketAngleOffsetDeg((float) dim.bucketAngleOffsetDeg);
        }
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
        if (tvLiveStatus != null) {
            tvLiveStatus.setText(connected ? "LIVE" : "OFFLINE");
            tvLiveStatus.setTextColor(connected ? Color.WHITE : Color.parseColor("#FF6B6B"));
        }
        if (liveIndicatorDot != null) {
            GradientDrawable dot = new GradientDrawable();
            dot.setShape(GradientDrawable.OVAL);
            dot.setColor(connected ? Color.parseColor("#00E676") : Color.parseColor("#FF6B6B"));
            liveIndicatorDot.setBackground(dot);
        }
        if (videoPlaceholder != null) {
            videoPlaceholder.setVisibility(connected ? View.GONE : View.VISIBLE);
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
     * 初始化视频地址编辑器
     */
    private void initVideoUrlEditor() {
        // 设置初始地址
        if (etVideoUrl != null) {
            etVideoUrl.setText("rtsp://192.168.144.100:554/stream1");
        }
        
        // 更新按钮点击事件
        if (btnUpdateVideoUrl != null) {
            btnUpdateVideoUrl.setOnClickListener(v -> {
                String newUrl = etVideoUrl.getText().toString().trim();
                if (!newUrl.isEmpty()) {
                    updateVideoUrl(newUrl);
                    // 隐藏键盘
                    hideKeyboard();
                } else {
                    Toast.makeText(this, "请输入有效的RTSP地址", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        // EditText输入完成事件（按回车或完成键）
        if (etVideoUrl != null) {
            etVideoUrl.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                    String newUrl = etVideoUrl.getText().toString().trim();
                    if (!newUrl.isEmpty()) {
                        updateVideoUrl(newUrl);
                        hideKeyboard();
                    }
                    return true;
                }
                return false;
            });
        }
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
    
    /**
     * 隐藏键盘
     */
    private void hideKeyboard() {
        if (etVideoUrl != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(etVideoUrl.getWindowToken(), 0);
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
                handler.postDelayed(this, 500); // 每秒更新一次
            }
        };
        
        handler.post(updateRunnable);
        
        // 摇杆值更新Handler（100ms更新一次）
        joystickHandler = new Handler(Looper.getMainLooper());
        
        joystickUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                updateJoystickValues(); // 只更新摇杆值
                joystickHandler.postDelayed(this, 100); // 每100ms更新一次
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
        // 连接延迟: 45-60ms之间波动
        int delay = 45 + random.nextInt(15);
        tvVideoLink.setText("⏱ Delay: " + delay + "ms");
    }
    
    /**
     * 更新信号强度显示
     */
    private void updateSignalDisplay() {
        if (tvRcSignal != null) {
            tvRcSignal.setText("📶 Signal: " + currentSignalStrength + "%");
        }
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
        } else {
            // 模拟数据没有座舱姿态，先用0占位
            rawCabinPitch = 0f;
            rawCabinRoll = 0f;

            if (angleSets.isEmpty()) {
                return;
            }
            // 使用模拟数据（每1秒切换一次角度）
            angleUpdateCounter++;
            if (angleUpdateCounter >= 1) {
                angleIndex = (angleIndex + 1) % angleSets.size();
                angleUpdateCounter = 0;
            }

            // 轮换角度数据（原始IMU角度）
            AngleSet currentSet = angleSets.get(angleIndex);
            rawBoom = currentSet.boom;
            rawStick = currentSet.stick;
            rawBucket = currentSet.bucket;
        }

        // 解算相对角度（统一入口，模拟/真实都使用）
        ImuAngleConverter.RelativeAngles relative = ImuAngleConverter.toRelativeAngles(
                rawBoom,
                rawStick,
                rawBucket,
                rawCabinPitch,
                rawCabinRoll,
                imuAngleConfig
        );
        relativeBoomAngle = relative.boomDeg;
        relativeStickAngle = relative.stickDeg;
        relativeBucketAngle = relative.bucketDeg;

        // 更新挖机姿态（使用相对角度）
        excavatorPostureView.setAngles(0f, relativeBoomAngle, relativeStickAngle, relativeBucketAngle);

        // 更新文本显示（相对角度 + 座舱姿态）
        tvBoomAngle.setText(String.format(Locale.getDefault(), "BOOM: %.2f°", relativeBoomAngle));
        tvStickAngle.setText(String.format(Locale.getDefault(), "STICK: %.2f°", relativeStickAngle));
        tvBucketAngle.setText(String.format(Locale.getDefault(), "BUCKET: %.2f°", relativeBucketAngle));
        if (tvCabinPitchAngle != null) {
            tvCabinPitchAngle.setText(String.format(Locale.getDefault(), "CABIN PITCH: %.2f°", rawCabinPitch));
        }
        if (tvCabinRollAngle != null) {
            tvCabinRollAngle.setText(String.format(Locale.getDefault(), "CABIN ROLL: %.2f°", rawCabinRoll));
        }

        // 更新角度仪表盘（仍使用原始IMU角度）
        if (gaugeBoom != null) gaugeBoom.setAngle(rawBoom);
        if (gaugeStick != null) gaugeStick.setAngle(rawStick);
        if (gaugeBucket != null) gaugeBucket.setAngle(rawBucket);
    }
    
    private void updatePositioning() {
        // 经纬度在小范围内波动
        double lat = 22.4269593;
        double lng = 114.2089099;

    }
    
    private void updateDigDepth() {
        // 挖掘深度在3.0-3.5米之间波动
        double depth = 3.0 + random.nextDouble() * 0.5;
        tvDigDepth.setText(String.format(Locale.getDefault(), "%.2f m", depth));
        
        // 进度条（0-10米范围）
        int progress = (int) (depth * 100); // 转换为整数进度
//        progressDigDepth.setProgress(progress);
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

                        // 更新摇杆示意图（切换回主线程绘制）
                        runOnUiThread(() -> {
                            if (joystickLeft != null) joystickLeft.setValues(ch4Value, ch3Value);
                            if (joystickRight != null) joystickRight.setValues(ch1Value, -ch2Value);
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
                    // 接收到的UDP数据（47字节）
                    if (data != null) {
                        Log.d("UDP", "收到数据，长度: " + data.length);
                        
                        if (data.length == 47) {
                            // 更新最后接收数据的时间
                            lastDataReceiveTime = System.currentTimeMillis();
                            
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
                                    // 保存原始IMU角度与座舱姿态（解析线程）
                                    realBoomAngle = parsed.boomAngle;
                                    realStickAngle = parsed.stickAngle;
                                    realBucketAngle = parsed.bucketAngle;
                                    realCabinPitchAngle = parsed.cabinPitchAngle;
                                    realCabinRollAngle = parsed.cabinRollAngle;
                                }

                                @Override
                                public void onParseError(String error) {
                                    Log.e("UDP", "数据解析失败: " + error);
                                }
                            });
                        } else {
                            Log.w("UDP", "数据长度不正确，期望47字节，实际: " + data.length);
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
