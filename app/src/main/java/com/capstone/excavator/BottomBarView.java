package com.capstone.excavator;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;

import eightbitlab.com.blurview.BlurTarget;
import eightbitlab.com.blurview.BlurView;

/**
 * 底部覆盖层：左右两侧分立的毛玻璃摇杆指示器，中央是 dock 风格的快捷动作按钮
 * （重连 / 找平 / 挖沟 / 修坡 / 设置）。
 *
 * 数据相关的 setter（角度、深度、信号、延迟、LIVE 等）已不再有对应的 UI，
 * 但保留为 no-op 以避免外部调用者编译失败。如后续需要重新展示，
 * 可在布局中加回相应控件并在 {@link #onFinishInflate()} 内绑定。
 */
public class BottomBarView extends FrameLayout {

    // ── Event interfaces ─────────────────────────────────────────────

    /** 兼容历史调用：当前布局已没有折叠按钮，触发逻辑由调用方决定。 */
    public interface OnBarToggleListener {
        void onCollapse();
    }

    // ── Internal views ───────────────────────────────────────────────

    private JoystickIndicatorView joystickLeft;
    private JoystickIndicatorView joystickRight;

    private BlurView blurReconnect;
    private BlurView blurWorkGroup;
    private BlurView blurSettings;
    private BlurView blurJoystickRight;
    private BlurView blurJoystickLeft;

    // ── Callbacks ────────────────────────────────────────────────────

    private Runnable onReconnectListener;
    private Runnable onLevelListener;
    private Runnable onTrenchListener;
    private Runnable onSlopeListener;
    private Runnable onSettingsListener;
    private OnBarToggleListener onBarToggleListener; // 保留接口；当前布局不再触发

    // ── Constructors ─────────────────────────────────────────────────

    public BottomBarView(Context context) {
        this(context, null);
    }

    public BottomBarView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BottomBarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.view_bottom_bar, this);

        joystickLeft  = findViewById(R.id.joystickLeft);
        joystickRight = findViewById(R.id.joystickRight);

        blurReconnect = findViewById(R.id.btnReconnect);
        blurWorkGroup = findViewById(R.id.blurWorkGroup);
        blurSettings  = findViewById(R.id.btnSettings);
        blurJoystickRight = findViewById(R.id.blurJoystickRight);
        blurJoystickLeft = findViewById(R.id.blurJoystickLeft);

        bindDockButton(R.id.btnReconnect, () -> onReconnectListener);
        bindDockButton(R.id.btnLevel,     () -> onLevelListener);
        bindDockButton(R.id.btnTrench,    () -> onTrenchListener);
        bindDockButton(R.id.btnSlope,     () -> onSlopeListener);
        bindDockButton(R.id.btnSettings,  () -> onSettingsListener);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // 延迟到视图树完成布局后再初始化 BlurView，避免 onAttachedToWindow 时还没测量好
        post(this::setupDockBlur);
    }

    private void setupDockBlur() {
        try {
            View root = getRootView();
            View t = root != null ? root.findViewById(R.id.blurTarget) : null;
            if (!(t instanceof BlurTarget)) return;
            BlurTarget target = (BlurTarget) t;

            final float radius = 18f;
            final int overlay = 0x4D808080;

            setupOneBlur(blurReconnect, target, radius, overlay);
            setupOneBlur(blurWorkGroup, target, radius, overlay);
            setupOneBlur(blurSettings,  target, radius, overlay);
            setupOneBlur(blurJoystickRight, target, radius, overlay);
            setupOneBlur(blurJoystickLeft, target, radius, overlay);
        } catch (Throwable ignored) {
            // Blur 是装饰，失败不应阻塞 UI
        }
    }

    private void setupOneBlur(BlurView blurView, BlurTarget target, float radius, int overlayColor) {
        if (blurView == null) return;
        blurView.setupWith(target)
                .setBlurRadius(radius)
                .setOverlayColor(overlayColor);
        blurView.setClipToOutline(true);
        blurView.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
    }

    private void bindDockButton(int id, RunnableSupplier supplier) {
        View v = findViewById(id);
        if (v == null) return;
        v.setOnClickListener(view -> {
            Runnable r = supplier.get();
            if (r != null) r.run();
        });
    }

    private interface RunnableSupplier {
        Runnable get();
    }

    // ── Joystick setters ─────────────────────────────────────────────

    /** 更新左摇杆指示位置（x：左右，y：上下，范围 -450~450） */
    public void setJoystickLeft(int x, int y) {
        if (joystickLeft != null) joystickLeft.setValues(x, y);
    }

    /** 更新右摇杆指示位置（x：左右，y：上下，范围 -450~450） */
    public void setJoystickRight(int x, int y) {
        if (joystickRight != null) joystickRight.setValues(x, y);
    }

    // ── Dock action listeners ────────────────────────────────────────

    public void setOnReconnectListener(Runnable listener) { this.onReconnectListener = listener; }
    public void setOnLevelListener(Runnable listener)     { this.onLevelListener     = listener; }
    public void setOnTrenchListener(Runnable listener)    { this.onTrenchListener    = listener; }
    public void setOnSlopeListener(Runnable listener)     { this.onSlopeListener     = listener; }
    public void setOnSettingsListener(Runnable listener)  { this.onSettingsListener  = listener; }

    public void setOnBarToggleListener(OnBarToggleListener listener) {
        this.onBarToggleListener = listener;
    }

    // ── Legacy data setters (no-op, 当前布局不再展示这些数据) ─────────

    public void setAngles(float boom, float stick, float bucket,
                          float cabinPitch, float cabinRoll) {
        // no-op: 当前 UI 不再显示传感器角度，姿态卡片承担显示职责
    }

    public void setRtkLatLon(double lat, double lon) {
        // no-op
    }

    public void setDepth(double depth) {
        // no-op
    }

    public void setDelay(int ms) {
        // no-op
    }

    public void setSignal(int percent) {
        // no-op
    }

    public void setLiveStatus(boolean connected) {
        // no-op: LIVE/OFFLINE 状态由 HeaderBarView 显示
    }
}
