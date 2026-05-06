package com.capstone.excavator;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

/**
 * 首次启动配置弹窗控制接口。
 *
 * - 使用 SharedPreferences 记录是否已弹过。
 * - 使用 TextView 作为按钮，避免 Material3 覆盖自定义背景/颜色。
 * - showIfNeeded(...)：只在未弹过时显示。
 * - show(...)：强制显示，便于调试或从其他入口主动触发。
 */
public class FirstRunConfigDialog {

    private static final String PREFS_NAME = "general_ui_prefs";
    private static final String KEY_FIRST_RUN_CONFIG_SHOWN = "first_run_config_shown";

    private FirstRunConfigDialog() {}

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** 是否需要弹出首次启动配置弹窗。 */
    public static boolean shouldShow(Context ctx) {
        return !prefs(ctx).getBoolean(KEY_FIRST_RUN_CONFIG_SHOWN, false);
    }

    /** 标记首次启动配置弹窗已处理。 */
    public static void markShown(Context ctx) {
        prefs(ctx).edit().putBoolean(KEY_FIRST_RUN_CONFIG_SHOWN, true).apply();
    }

    /** 重置弹窗状态，调试或重新引导时可调用。 */
    public static void resetShown(Context ctx) {
        prefs(ctx).edit().putBoolean(KEY_FIRST_RUN_CONFIG_SHOWN, false).apply();
    }

    /** 未处理过时才弹出。 */
    public static void showIfNeeded(Activity activity, Runnable onConfigure) {
        if (shouldShow(activity)) {
            show(activity, onConfigure);
        }
    }

    /** 强制弹出。点击“配置参数”后执行 onConfigure。 */
    public static void show(Activity activity, Runnable onConfigure) {
        Dialog dialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setCancelable(false);

        View root = LayoutInflater.from(activity).inflate(R.layout.dialog_first_run_config, null);
        dialog.setContentView(root);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT);
        }

        TextView btnSkip = root.findViewById(R.id.btnFirstRunSkip);
        View btnConfigure = root.findViewById(R.id.btnFirstRunConfigure);

        btnSkip.setOnClickListener(v -> {
            markShown(activity);
            dialog.dismiss();
        });

        btnConfigure.setOnClickListener(v -> {
            markShown(activity);
            dialog.dismiss();
            if (onConfigure != null) {
                onConfigure.run();
            }
        });

        dialog.show();
    }
}
