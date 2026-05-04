package com.capstone.excavator;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 通用二次确认弹窗（全屏 FrameLayout 覆盖层）。
 * <p>
 * 在父布局 XML 中声明（GONE），调用 {@link #show(Config)} 显示，
 * 用户点击按钮后自动隐藏并回调。
 * <p>
 * 样式由 {@code view_confirm_dialog.xml}、{@code confirm_dialog_*.xml} drawable 维护。
 */
public class ConfirmDialogView extends FrameLayout {

    // ── 配置 ─────────────────────────────────────────────────────────

    /**
     * 弹窗配置。通过 Builder 构建。
     * <ul>
     *   <li>{@link #title}：主标题（必填）</li>
     *   <li>{@link #subtitle}：副标题，为 null 或空串时不显示</li>
     *   <li>{@link #confirmText}：左按钮文字，默认「确认退出」</li>
     *   <li>{@link #cancelText}：右按钮文字，默认「取消」</li>
     *   <li>{@link #onConfirm}：点「确认」回调</li>
     *   <li>{@link #onCancel}：点「取消」或蒙层回调</li>
     *   <li>{@link #dismissOnScrim}：点蒙层是否关闭（默认 true）</li>
     * </ul>
     */
    public static class Config {
        public final String title;
        @Nullable public final String subtitle;
        public final String confirmText;
        public final String cancelText;
        @Nullable public final Runnable onConfirm;
        @Nullable public final Runnable onCancel;
        public final boolean dismissOnScrim;

        private Config(Builder b) {
            this.title          = b.title;
            this.subtitle       = b.subtitle;
            this.confirmText    = b.confirmText;
            this.cancelText     = b.cancelText;
            this.onConfirm      = b.onConfirm;
            this.onCancel       = b.onCancel;
            this.dismissOnScrim = b.dismissOnScrim;
        }

        public static class Builder {
            private final String title;
            private String subtitle;
            private String confirmText  = "确认退出";
            private String cancelText   = "取消";
            private Runnable onConfirm;
            private Runnable onCancel;
            private boolean dismissOnScrim = true;

            public Builder(@NonNull String title) { this.title = title; }

            public Builder subtitle(@Nullable String s)    { this.subtitle       = s;  return this; }
            public Builder confirmText(@NonNull String t)  { this.confirmText    = t;  return this; }
            public Builder cancelText(@NonNull String t)   { this.cancelText     = t;  return this; }
            public Builder onConfirm(@Nullable Runnable r) { this.onConfirm      = r;  return this; }
            public Builder onCancel(@Nullable Runnable r)  { this.onCancel       = r;  return this; }
            public Builder dismissOnScrim(boolean v)       { this.dismissOnScrim = v;  return this; }

            public Config build() { return new Config(this); }
        }
    }

    // ── 内部视图 ──────────────────────────────────────────────────────

    private View   scrim;
    private TextView title;
    private TextView subtitle;
    private TextView btnConfirm;
    private TextView btnCancel;

    private Config pendingConfig;

    // ── 构造 ──────────────────────────────────────────────────────────

    public ConfirmDialogView(Context context) {
        this(context, null);
    }

    public ConfirmDialogView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ConfirmDialogView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.view_confirm_dialog, this, true);
        setVisibility(GONE);
        bindViews();
    }

    private void bindViews() {
        scrim     = findViewById(R.id.dialogScrim);
        title     = findViewById(R.id.dialogTitle);
        subtitle  = findViewById(R.id.dialogSubtitle);
        btnConfirm = findViewById(R.id.dialogBtnConfirm);
        btnCancel  = findViewById(R.id.dialogBtnCancel);
    }

    // ── 公共 API ──────────────────────────────────────────────────────

    /** 显示弹窗，应用 {@link Config} 中的内容与回调。 */
    public void show(@NonNull Config config) {
        this.pendingConfig = config;
        applyConfig(config);
        setVisibility(VISIBLE);
        bringToFront();
    }

    /** 隐藏弹窗（无回调）。 */
    public void dismiss() {
        setVisibility(GONE);
    }

    // ── 内部 ──────────────────────────────────────────────────────────

    private void applyConfig(Config cfg) {
        title.setText(cfg.title);

        if (cfg.subtitle != null && !cfg.subtitle.isEmpty()) {
            subtitle.setText(cfg.subtitle);
            subtitle.setVisibility(VISIBLE);
        } else {
            subtitle.setVisibility(GONE);
        }

        btnConfirm.setText(cfg.confirmText);
        btnCancel.setText(cfg.cancelText);

        btnConfirm.setOnClickListener(v -> {
            dismiss();
            if (cfg.onConfirm != null) cfg.onConfirm.run();
        });

        btnCancel.setOnClickListener(v -> {
            dismiss();
            if (cfg.onCancel != null) cfg.onCancel.run();
        });

        scrim.setOnClickListener(cfg.dismissOnScrim ? v -> {
            dismiss();
            if (cfg.onCancel != null) cfg.onCancel.run();
        } : null);
        scrim.setClickable(cfg.dismissOnScrim);
    }
}
