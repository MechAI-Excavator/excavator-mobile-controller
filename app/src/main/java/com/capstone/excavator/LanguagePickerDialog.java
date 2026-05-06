package com.capstone.excavator;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * 首次运行的全屏语言选择页。
 *
 * - 使用全屏无标题主题，完整覆盖状态栏以下所有区域。
 * - 按钮用 FrameLayout+TextView 实现，避免 Material3 覆盖自定义背景/颜色。
 * - active 状态：背景 #FF2A6AFF，文字白色，右下角显示 selected_icon badge。
 * - 默认选中简体中文；点击任意语言即立即保存并关闭。
 * - 读写通过 {@link LanguageManager}，与设置页共享同一 SharedPreferences 槽。
 */
public class LanguagePickerDialog {

    public interface OnLanguagePickedListener {
        void onPicked(String langCode);
    }

    public static void show(Context ctx, OnLanguagePickedListener listener) {
        Dialog dialog = new Dialog(ctx, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setCancelable(false);

        View root = LayoutInflater.from(ctx).inflate(R.layout.dialog_language_picker, null);
        dialog.setContentView(root);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT);
        }

        FrameLayout btnHans = root.findViewById(R.id.btnPickZhHans);
        FrameLayout btnHant = root.findViewById(R.id.btnPickZhHant);
        FrameLayout btnEn   = root.findViewById(R.id.btnPickEn);

        // 默认选中简体中文
        applyState(btnHans, true);
        applyState(btnHant, false);
        applyState(btnEn,   false);

        View.OnClickListener langClick = v -> {
            String lang;
            if (v == btnHans)      lang = LanguageManager.LANG_ZH_HANS;
            else if (v == btnHant) lang = LanguageManager.LANG_ZH_HANT;
            else                   lang = LanguageManager.LANG_EN;

            applyState(btnHans, v == btnHans);
            applyState(btnHant, v == btnHant);
            applyState(btnEn,   v == btnEn);

            LanguageManager.setLanguage(ctx, lang);
            dialog.dismiss();
            if (listener != null) listener.onPicked(lang);
        };

        btnHans.setOnClickListener(langClick);
        btnHant.setOnClickListener(langClick);
        btnEn.setOnClickListener(langClick);

        dialog.show();
    }

    private static void applyState(FrameLayout container, boolean selected) {
        if (container == null) return;
        TextView  label = (TextView)  container.getChildAt(0);
        ImageView badge = (ImageView) container.getChildAt(1);

        label.setBackgroundResource(
                selected ? R.drawable.lang_chip_selected_bg : R.drawable.lang_chip_unselected_bg);
        label.setTextColor(selected ? 0xFFFFFFFF : 0xFF9CA3AF);
        badge.setVisibility(selected ? View.VISIBLE : View.GONE);
    }
}
