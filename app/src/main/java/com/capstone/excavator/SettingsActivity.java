package com.capstone.excavator;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.slider.Slider;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * 设置页面。
 *
 * 左侧导航栏：4 个 menu item（IMU 安装角度 / 尺寸信息 / 摇杆通道映射 / 通用设置）。
 * 右侧 FrameLayout：每个 menu 对应一个 <include> 子页，切换时 VISIBLE/GONE 互换。
 */
public class SettingsActivity extends AppCompatActivity {

    private static final float MIN_ARM_SCALE = 0.1f;
    private static final float MAX_ARM_SCALE = 10f;

    private static final String PREFS_GENERAL_UI = "general_ui_prefs";
    private static final String KEY_BRIGHTNESS_PERCENT = "brightness_percent";
    private static final String KEY_LANGUAGE = "language";

    /** 尺寸页 / IMU 页数字键盘在屏幕上的固定锚点（与旧版 IMU 行为一致）。 */
    private static final int NUMPAD_SCREEN_X = 1260;
    private static final int NUMPAD_SCREEN_Y = 278;

    // ── 导航项 ───────────────────────────────────────────────────────────────
    private LinearLayout navImu;
    private LinearLayout navDimensions;
    private LinearLayout navJoystick;
    private LinearLayout navGeneral;

    // ── 内容页根视图 ─────────────────────────────────────────────────────────
    private View pageImu;
    private View pageDimensions;
    private View pageJoystick;
    private View pageGeneral;

    private int currentPage = 0; // 0=IMU, 1=尺寸, 2=摇杆, 3=通用

    // ── IMU page views ───────────────────────────────────────────────────────
    private TextView tvImuBoomOffsetPreview;
    private TextView tvImuStickOffsetPreview;
    private TextView tvImuBucketOffsetPreview;

    // ── General page views ───────────────────────────────────────────────────
    private EditText etSettingsVideoUrl;
    private EditText etArmBoomScale;
    private EditText etArmStickScale;

    private Button btnLangZhHans;
    private Button btnLangZhHant;
    private Button btnLangEn;
    private Slider sbBrightness;
    private TextView tvBrightnessPercent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFullScreenMode();
        setContentView(R.layout.activity_settings);

        bindNavItems();
        bindPages();
        bindTopBar();
        bindImuPage();
        bindDimensionsPage();
        bindJoystickPage();
        bindGeneralPage();

        showPage(0);
    }

    // ── Navigation ───────────────────────────────────────────────────────────

    private void bindNavItems() {
        navImu        = findViewById(R.id.navImu);
        navDimensions = findViewById(R.id.navDimensions);
        navJoystick   = findViewById(R.id.navJoystick);
        navGeneral    = findViewById(R.id.navGeneral);

        navImu.setOnClickListener(v        -> showPage(0));
        navDimensions.setOnClickListener(v -> showPage(1));
        navJoystick.setOnClickListener(v   -> showPage(2));
        navGeneral.setOnClickListener(v    -> showPage(3));
    }

    private void bindPages() {
        pageImu        = findViewById(R.id.pageImu);
        pageDimensions = findViewById(R.id.pageDimensions);
        pageJoystick   = findViewById(R.id.pageJoystick);
        pageGeneral    = findViewById(R.id.pageGeneral);
    }

    private void showPage(int index) {
        currentPage = index;

        pageImu.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        pageDimensions.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        pageJoystick.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        pageGeneral.setVisibility(index == 3 ? View.VISIBLE : View.GONE);

        updateNavHighlight(index);
    }

    /** 选中项用 nav_item_selected_bg + 蓝色文字，其余恢复默认背景。 */
    private void updateNavHighlight(int index) {
        LinearLayout[] navItems = { navImu, navDimensions, navJoystick, navGeneral };
        for (int i = 0; i < navItems.length; i++) {
            LinearLayout item = navItems[i];
            boolean selected = (i == index);
            item.setBackgroundResource(selected
                    ? R.drawable.nav_item_selected_bg
                    : android.R.color.transparent);

            // 标题 TextView 是 item 的第二个子 view 的第一个子 view
            try {
                LinearLayout textCol = (LinearLayout) item.getChildAt(1);
                TextView title = (TextView) textCol.getChildAt(0);
                TextView sub   = (TextView) textCol.getChildAt(1);
                if (selected) {
                    title.setTextColor(0xFF1D4ED8);
                    sub.setTextColor(0xFF6B7280);
                } else {
                    title.setTextColor(0xFF374151);
                    sub.setTextColor(0xFF9CA3AF);
                }
                // icon emoji color
                TextView icon = (TextView) item.getChildAt(0);
                icon.setTextColor(selected ? 0xFF2563EB : 0xFF9CA3AF);
            } catch (ClassCastException | NullPointerException ignored) {
            }
        }
    }

    // ── Top bar ──────────────────────────────────────────────────────────────

    private void bindTopBar() {
        TextView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        View btnSave = findViewById(R.id.btnSaveVideoUrl);
        btnSave.setOnClickListener(v -> saveAll());
    }

    private void saveAll() {
        // 只有通用设置页才有视频流 & 臂长字段
        String newUrl = etSettingsVideoUrl != null
                ? etSettingsVideoUrl.getText().toString().trim() : "";

        float boomScale  = parseScale(etArmBoomScale  != null ? etArmBoomScale.getText().toString()  : "", ArmLengthPreferences.DEFAULT_SCALE);
        float stickScale = parseScale(etArmStickScale != null ? etArmStickScale.getText().toString() : "", ArmLengthPreferences.DEFAULT_SCALE);

        if (boomScale < MIN_ARM_SCALE || boomScale > MAX_ARM_SCALE
                || stickScale < MIN_ARM_SCALE || stickScale > MAX_ARM_SCALE) {
            Toast.makeText(this,
                    "大臂、小臂比例须在 " + MIN_ARM_SCALE + "～" + MAX_ARM_SCALE + " 之间",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        ArmLengthPreferences.save(this, boomScale, stickScale);

        Intent resultIntent = new Intent();
        if (!newUrl.isEmpty()) resultIntent.putExtra("video_url", newUrl);
        resultIntent.putExtra("arm_boom_scale", boomScale);
        resultIntent.putExtra("arm_stick_scale", stickScale);
        setResult(RESULT_OK, resultIntent);

        Toast.makeText(this, newUrl.isEmpty() ? "臂长比例已保存" : "已保存并应用", Toast.LENGTH_SHORT).show();
        finish();
    }

    // ── IMU page ─────────────────────────────────────────────────────────────

    private void bindImuPage() {
        tvImuBoomOffsetPreview   = pageImu.findViewById(R.id.tvImuBoomOffsetPreview);
        tvImuStickOffsetPreview  = pageImu.findViewById(R.id.tvImuStickOffsetPreview);
        tvImuBucketOffsetPreview = pageImu.findViewById(R.id.tvImuBucketOffsetPreview);

        refreshImuPreviewLabels();

        // 点击偏移量预览值 → 弹出数字键盘
        bindImuNumpad(tvImuBoomOffsetPreview,   "boomImuOffsetDeg");
        bindImuNumpad(tvImuStickOffsetPreview,  "stickImuOffsetDeg");
        bindImuNumpad(tvImuBucketOffsetPreview, "bucketImuOffsetDeg");

//        Button btnOpenImuDialog = pageImu.findViewById(R.id.btnOpenImuDialog);
//        if (btnOpenImuDialog != null) {
//            btnOpenImuDialog.setOnClickListener(v -> showImuSettingsDialog());
//        }
    }

    /**
     * 给 IMU 偏移预览 TextView 绑定数字键盘。
     * 点击 → 弹出键盘，确认 → 立即持久化该字段并刷新显示。
     *
     * @param tv        偏移量预览 TextView
     * @param fieldKey  对应字段名（仅用于注释/调试，实际由 lambda 区分）
     */
    private void bindImuNumpad(TextView tv, String fieldKey) {
        if (tv == null) return;
        tv.setClickable(true);
        tv.setFocusable(true);
        tv.setOnClickListener(v -> {
            ImuPreferences.Params current = ImuPreferences.load(this);
            double initialValue;
            switch (fieldKey) {
                case "boomImuOffsetDeg":   initialValue = current.boomImuOffsetDeg;   break;
                case "stickImuOffsetDeg":  initialValue = current.stickImuOffsetDeg;  break;
                case "bucketImuOffsetDeg": initialValue = current.bucketImuOffsetDeg; break;
                default: initialValue = 0; break;
            }

            NumpadView numpad = new NumpadView(this);
            numpad.setValue(ImuPreferences.fmt(initialValue));
            numpad.setOnConfirmListener(value -> {
                double parsed = ImuPreferences.parseOrDefault(value, initialValue);
                ImuPreferences.Params p = ImuPreferences.load(this);
                switch (fieldKey) {
                    case "boomImuOffsetDeg":   p.boomImuOffsetDeg   = parsed; break;
                    case "stickImuOffsetDeg":  p.stickImuOffsetDeg  = parsed; break;
                    case "bucketImuOffsetDeg": p.bucketImuOffsetDeg = parsed; break;
                }
                ImuPreferences.save(this, p);
                refreshImuPreviewLabels();
            });
            // 定位：使用 window 绝对坐标（你可自行调整 x/y）
            numpad.showForAtScreen(tv, tv, NUMPAD_SCREEN_X, NUMPAD_SCREEN_Y);
        });
    }

    // ── Dimensions page ─────────────────────────────────────────────────────

    private void bindDimensionsPage() {
        if (pageDimensions == null) return;

        TextView tabSelect = pageDimensions.findViewById(R.id.tabDimSelectModel);
        TextView tabCustom = pageDimensions.findViewById(R.id.tabDimCustom);
        View underlineSelect = pageDimensions.findViewById(R.id.dimUnderlineSelect);
        View underlineCustom = pageDimensions.findViewById(R.id.dimUnderlineCustom);
        View panelSelect = pageDimensions.findViewById(R.id.dimPanelSelect);
        View panelCustom = pageDimensions.findViewById(R.id.dimPanelCustom);

        TextView tvDimLb = pageDimensions.findViewById(R.id.tvDimLb);
        TextView tvDimLs = pageDimensions.findViewById(R.id.tvDimLs);
        TextView tvDimLinkL1 = pageDimensions.findViewById(R.id.tvDimLinkL1);
        TextView tvDimLinkL2 = pageDimensions.findViewById(R.id.tvDimLinkL2);
        TextView tvDimLinkL3 = pageDimensions.findViewById(R.id.tvDimLinkL3);
        TextView tvDimLinkL4 = pageDimensions.findViewById(R.id.tvDimLinkL4);
        TextView tvDimLinkAngle = pageDimensions.findViewById(R.id.tvDimLinkAngle);
        TextView tvDimChassisH = pageDimensions.findViewById(R.id.tvDimChassisH);
        TextView tvDimTrackW = pageDimensions.findViewById(R.id.tvDimTrackW);
        TextView tvDimCabinH = pageDimensions.findViewById(R.id.tvDimCabinH);

        wireModelCard(R.id.cardDimCAT303, R.id.checkDimCAT303, DimensionPreferences.MODEL_CAT_303);
        wireModelCard(R.id.cardDimCAT312, R.id.checkDimCAT312, DimensionPreferences.MODEL_CAT_312);
        wireModelCard(R.id.cardDimCAT320, R.id.checkDimCAT320, DimensionPreferences.MODEL_CAT_320);
        wireModelCard(R.id.cardDimCAT330, R.id.checkDimCAT330, DimensionPreferences.MODEL_CAT_330);
        wireModelCard(R.id.cardDimCAT336, R.id.checkDimCAT336, DimensionPreferences.MODEL_CAT_336);

        View.OnClickListener tabSelectListener = v -> showDimensionsSubTab(
                tabSelect, tabCustom, underlineSelect, underlineCustom, panelSelect, panelCustom, 0);
        View.OnClickListener tabCustomListener = v -> showDimensionsSubTab(
                tabSelect, tabCustom, underlineSelect, underlineCustom, panelSelect, panelCustom, 1);

        pageDimensions.findViewById(R.id.wrapTabDimSelectModel).setOnClickListener(tabSelectListener);
        tabSelect.setOnClickListener(tabSelectListener);
        pageDimensions.findViewById(R.id.wrapTabDimCustom).setOnClickListener(tabCustomListener);
        tabCustom.setOnClickListener(tabCustomListener);

        bindDimensionNumpad(tvDimLb, "boomM");
        bindDimensionNumpad(tvDimLs, "stickM");
        bindDimensionNumpad(tvDimLinkL1, "linkL1");
        bindDimensionNumpad(tvDimLinkL2, "linkL2");
        bindDimensionNumpad(tvDimLinkL3, "linkL3");
        bindDimensionNumpad(tvDimLinkL4, "linkL4");
        bindDimensionNumpad(tvDimLinkAngle, "linkAngleDeg");
        bindDimensionNumpad(tvDimChassisH, "chassisM");
        bindDimensionNumpad(tvDimTrackW, "trackM");
        bindDimensionNumpad(tvDimCabinH, "cabinM");

        DimensionPreferences.Params p = DimensionPreferences.load(this);
        refreshDimensionModelSelectionUi(p.selectedModel);
        refreshDimensionValueLabels();
        showDimensionsSubTab(tabSelect, tabCustom, underlineSelect, underlineCustom,
                panelSelect, panelCustom, 0);
    }

    private void wireModelCard(int cardId, int checkId, String modelId) {
        View card = pageDimensions.findViewById(cardId);
        if (card == null) return;
        card.setOnClickListener(v -> {
            DimensionPreferences.applyModelPreset(this, modelId);
            refreshDimensionModelSelectionUi(modelId);
            refreshDimensionValueLabels();
        });
    }

    private void showDimensionsSubTab(
            TextView tabSelect,
            TextView tabCustom,
            View underlineSelect,
            View underlineCustom,
            View panelSelect,
            View panelCustom,
            int index) {
        boolean selectModel = (index == 0);
        panelSelect.setVisibility(selectModel ? View.VISIBLE : View.GONE);
        panelCustom.setVisibility(selectModel ? View.GONE : View.VISIBLE);

        int active = 0xFF2563EB;
        int inactiveText = 0xFF9CA3AF;
        int inactiveLine = 0xFFE5E7EB;

        tabSelect.setTextColor(selectModel ? active : inactiveText);
        tabSelect.setTypeface(null, selectModel ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        underlineSelect.setBackgroundColor(selectModel ? active : inactiveLine);

        tabCustom.setTextColor(selectModel ? inactiveText : active);
        tabCustom.setTypeface(null, selectModel ? android.graphics.Typeface.NORMAL : android.graphics.Typeface.BOLD);
        underlineCustom.setBackgroundColor(selectModel ? inactiveLine : active);
    }

    private void refreshDimensionModelSelectionUi(String selectedModel) {
        int[] cardIds = {
                R.id.cardDimCAT303, R.id.cardDimCAT312, R.id.cardDimCAT320,
                R.id.cardDimCAT330, R.id.cardDimCAT336
        };
        int[] checkIds = {
                R.id.checkDimCAT303, R.id.checkDimCAT312, R.id.checkDimCAT320,
                R.id.checkDimCAT330, R.id.checkDimCAT336
        };
        String[] modelIds = {
                DimensionPreferences.MODEL_CAT_303, DimensionPreferences.MODEL_CAT_312,
                DimensionPreferences.MODEL_CAT_320, DimensionPreferences.MODEL_CAT_330,
                DimensionPreferences.MODEL_CAT_336
        };
        float d = getResources().getDisplayMetrics().density;
        for (int i = 0; i < cardIds.length; i++) {
            View card = pageDimensions.findViewById(cardIds[i]);
            TextView check = pageDimensions.findViewById(checkIds[i]);
            if (card == null || check == null) continue;
            boolean sel = modelIds[i].equals(selectedModel);
            card.setBackgroundResource(sel ? R.drawable.model_card_selected_bg : R.drawable.card_light_bg);
            card.setElevation(sel ? 2f * d : 1f * d);
            check.setVisibility(sel ? View.VISIBLE : View.GONE);
        }
    }

    private void refreshDimensionValueLabels() {
        DimensionPreferences.Params p = DimensionPreferences.load(this);
        TextView tvDimLb = pageDimensions.findViewById(R.id.tvDimLb);
        TextView tvDimLs = pageDimensions.findViewById(R.id.tvDimLs);
        TextView tvDimLinkL1 = pageDimensions.findViewById(R.id.tvDimLinkL1);
        TextView tvDimLinkL2 = pageDimensions.findViewById(R.id.tvDimLinkL2);
        TextView tvDimLinkL3 = pageDimensions.findViewById(R.id.tvDimLinkL3);
        TextView tvDimLinkL4 = pageDimensions.findViewById(R.id.tvDimLinkL4);
        TextView tvDimLinkAngle = pageDimensions.findViewById(R.id.tvDimLinkAngle);
        TextView tvDimChassisH = pageDimensions.findViewById(R.id.tvDimChassisH);
        TextView tvDimTrackW = pageDimensions.findViewById(R.id.tvDimTrackW);
        TextView tvDimCabinH = pageDimensions.findViewById(R.id.tvDimCabinH);

        if (tvDimLb != null) tvDimLb.setText(String.format(Locale.US, "%.2f", p.boomM));
        if (tvDimLs != null) tvDimLs.setText(String.format(Locale.US, "%.2f", p.stickM));
        if (tvDimLinkL1 != null) tvDimLinkL1.setText(String.format(Locale.US, "%.2f", p.linkL1));
        if (tvDimLinkL2 != null) tvDimLinkL2.setText(String.format(Locale.US, "%.2f", p.linkL2));
        if (tvDimLinkL3 != null) tvDimLinkL3.setText(String.format(Locale.US, "%.2f", p.linkL3));
        if (tvDimLinkL4 != null) tvDimLinkL4.setText(String.format(Locale.US, "%.2f", p.linkL4));
        if (tvDimLinkAngle != null) tvDimLinkAngle.setText(String.format(Locale.US, "%.1f", p.linkAngleDeg));
        if (tvDimChassisH != null) tvDimChassisH.setText(String.format(Locale.US, "%.2f", p.chassisHeightM));
        if (tvDimTrackW != null) tvDimTrackW.setText(String.format(Locale.US, "%.2f", p.trackWidthM));
        if (tvDimCabinH != null) tvDimCabinH.setText(String.format(Locale.US, "%.2f", p.cabinHeightM));
    }

    /**
     * 自定义尺寸数值：点击预览 TextView → 与 IMU 页相同的屏幕坐标弹出数字键盘。
     */
    private void bindDimensionNumpad(TextView tv, String fieldKey) {
        if (tv == null) return;
        tv.setClickable(true);
        tv.setFocusable(true);
        tv.setOnClickListener(v -> {
            DimensionPreferences.Params current = DimensionPreferences.load(this);
            double initialValue;
            switch (fieldKey) {
                case "boomM": initialValue = current.boomM; break;
                case "stickM": initialValue = current.stickM; break;
                case "linkL1": initialValue = current.linkL1; break;
                case "linkL2": initialValue = current.linkL2; break;
                case "linkL3": initialValue = current.linkL3; break;
                case "linkL4": initialValue = current.linkL4; break;
                case "linkAngleDeg": initialValue = current.linkAngleDeg; break;
                case "chassisM": initialValue = current.chassisHeightM; break;
                case "trackM": initialValue = current.trackWidthM; break;
                case "cabinM": initialValue = current.cabinHeightM; break;
                default: initialValue = 0; break;
            }

            NumpadView numpad = new NumpadView(this);
            if ("linkAngleDeg".equals(fieldKey)) {
                numpad.setValue(String.format(Locale.US, "%.1f", initialValue));
            } else {
                numpad.setValue(String.format(Locale.US, "%.2f", initialValue));
            }
            numpad.setOnConfirmListener(value -> {
                double parsed = ImuPreferences.parseOrDefault(value, initialValue);
                DimensionPreferences.Params p = DimensionPreferences.load(this);
                switch (fieldKey) {
                    case "boomM": p.boomM = parsed; break;
                    case "stickM": p.stickM = parsed; break;
                    case "linkL1": p.linkL1 = parsed; break;
                    case "linkL2": p.linkL2 = parsed; break;
                    case "linkL3": p.linkL3 = parsed; break;
                    case "linkL4": p.linkL4 = parsed; break;
                    case "linkAngleDeg": p.linkAngleDeg = parsed; break;
                    case "chassisM": p.chassisHeightM = parsed; break;
                    case "trackM": p.trackWidthM = parsed; break;
                    case "cabinM": p.cabinHeightM = parsed; break;
                }
                DimensionPreferences.save(this, p);
                refreshDimensionValueLabels();
            });
            numpad.showForAtScreen(tv, tv, NUMPAD_SCREEN_X, NUMPAD_SCREEN_Y);
        });
    }

    // ── Joystick page ───────────────────────────────────────────────────────

    private void bindJoystickPage() {
        if (pageJoystick == null) return;

        AppCompatAutoCompleteTextView leftAB = pageJoystick.findViewById(R.id.spJoyLeftAB);
        AppCompatAutoCompleteTextView leftCD = pageJoystick.findViewById(R.id.spJoyLeftCD);
        AppCompatAutoCompleteTextView rightEF = pageJoystick.findViewById(R.id.spJoyRightEF);
        AppCompatAutoCompleteTextView rightGH = pageJoystick.findViewById(R.id.spJoyRightGH);

        String[] options = new String[] { "大臂", "小臂", "铲斗", "回旋" };
        bindJoystickDropdown(leftAB, options);
        bindJoystickDropdown(leftCD, options);
        bindJoystickDropdown(rightEF, options);
        bindJoystickDropdown(rightGH, options);
    }

    private void bindJoystickDropdown(AppCompatAutoCompleteTextView tv, String[] options) {
        if (tv == null) return;

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                R.layout.dropdown_item_with_divider,
                R.id.tvDropdownText,
                options
        ) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View row = super.getView(position, convertView, parent);
                View divider = row.findViewById(R.id.vDivider);
                if (divider != null) {
                    divider.setVisibility(position == getCount() - 1 ? View.GONE : View.VISIBLE);
                }
                return row;
            }
        };
        tv.setAdapter(adapter);

        // 下拉弹窗背景（更像卡片）
        tv.setDropDownBackgroundResource(R.drawable.card_light_bg);

        // 右侧箭头改为灰色，避免白色“斜切块”视觉突兀
        Drawable arrow = AppCompatResources.getDrawable(this, R.drawable.droparrowdown);
        if (arrow != null) {
            Drawable wrapped = DrawableCompat.wrap(arrow.mutate());
            DrawableCompat.setTint(wrapped, 0xFF9CA3AF);
            int sizePx = (int) (10 * getResources().getDisplayMetrics().density);
            wrapped.setBounds(0, 0, sizePx, sizePx);
            tv.setCompoundDrawablesRelative(null, null, wrapped, null);
            tv.setCompoundDrawablePadding((int) (4 * getResources().getDisplayMetrics().density));
        }

        // 更像「选择器」：不可编辑，点击即弹出
        tv.setKeyListener(null);
        tv.setCursorVisible(false);
        tv.setOnClickListener(v -> tv.showDropDown());
        tv.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) tv.showDropDown();
        });
    }

    private void refreshImuPreviewLabels() {
        ImuPreferences.Params p = ImuPreferences.load(this);
        String fmt = "%.2f°";
        if (tvImuBoomOffsetPreview != null)
            tvImuBoomOffsetPreview.setText(String.format(Locale.US, fmt, p.boomImuOffsetDeg));
        if (tvImuStickOffsetPreview != null)
            tvImuStickOffsetPreview.setText(String.format(Locale.US, fmt, p.stickImuOffsetDeg));
        if (tvImuBucketOffsetPreview != null)
            tvImuBucketOffsetPreview.setText(String.format(Locale.US, fmt, p.bucketImuOffsetDeg));
    }

    // ── General page ─────────────────────────────────────────────────────────

    private void bindGeneralPage() {
        etSettingsVideoUrl = pageGeneral.findViewById(R.id.etSettingsVideoUrl);
        etArmBoomScale     = pageGeneral.findViewById(R.id.etArmBoomScale);
        etArmStickScale    = pageGeneral.findViewById(R.id.etArmStickScale);

        btnLangZhHans = pageGeneral.findViewById(R.id.btnLangZhHans);
        btnLangZhHant = pageGeneral.findViewById(R.id.btnLangZhHant);
        btnLangEn     = pageGeneral.findViewById(R.id.btnLangEn);
        sbBrightness  = pageGeneral.findViewById(R.id.sbBrightness);
        tvBrightnessPercent = pageGeneral.findViewById(R.id.tvBrightnessPercent);

        SharedPreferences sp = getSharedPreferences(PREFS_GENERAL_UI, MODE_PRIVATE);

        // Language UI only (no actual locale switching yet)
        String lang = sp.getString(KEY_LANGUAGE, "zh-Hans");
        applyLanguageUi(lang);
        View.OnClickListener langClick = v -> {
            String next = "zh-Hans";
            if (v == btnLangZhHant) next = "zh-Hant";
            else if (v == btnLangEn) next = "en";
            sp.edit().putString(KEY_LANGUAGE, next).apply();
            applyLanguageUi(next);
        };
        if (btnLangZhHans != null) btnLangZhHans.setOnClickListener(langClick);
        if (btnLangZhHant != null) btnLangZhHant.setOnClickListener(langClick);
        if (btnLangEn != null) btnLangEn.setOnClickListener(langClick);

        // Brightness (apply to this Activity window)
        int percent = sp.getInt(KEY_BRIGHTNESS_PERCENT, 50);
        percent = Math.max(1, Math.min(100, percent));
        applyBrightnessPercent(percent);
        if (sbBrightness != null) {
            sbBrightness.setValueFrom(1f);
            sbBrightness.setValueTo(100f);
            sbBrightness.setStepSize(1f);
            sbBrightness.setValue((float) percent);
            sbBrightness.addOnChangeListener((slider, value, fromUser) -> {
                int p = Math.max(1, Math.min(100, Math.round(value)));
                applyBrightnessPercent(p);
                sp.edit().putInt(KEY_BRIGHTNESS_PERCENT, p).apply();
            });
        }

        String currentUrl = getIntent().getStringExtra("current_url");
        if (currentUrl != null && !currentUrl.isEmpty() && etSettingsVideoUrl != null) {
            etSettingsVideoUrl.setText(currentUrl);
        }

        float boom  = ArmLengthPreferences.getBoomScale(this);
        float stick = ArmLengthPreferences.getStickScale(this);
        if (etArmBoomScale  != null) etArmBoomScale.setText(formatScaleForEdit(boom));
        if (etArmStickScale != null) etArmStickScale.setText(formatScaleForEdit(stick));
    }

    private void applyBrightnessPercent(int percent) {
        if (tvBrightnessPercent != null) tvBrightnessPercent.setText(percent + "%");
        Window window = getWindow();
        if (window == null) return;
        WindowManager.LayoutParams lp = window.getAttributes();
        // 0..1, keep a tiny minimum to avoid "black screen" feeling.
        float b = Math.max(0.05f, Math.min(1f, percent / 100f));
        lp.screenBrightness = b;
        window.setAttributes(lp);
    }

    private void applyLanguageUi(String lang) {
        boolean isHans = "zh-Hans".equals(lang);
        boolean isHant = "zh-Hant".equals(lang);
        boolean isEn = "en".equals(lang);
        setLangButtonState(btnLangZhHans, isHans);
        setLangButtonState(btnLangZhHant, isHant);
        setLangButtonState(btnLangEn, isEn);
    }

    private void setLangButtonState(Button b, boolean selected) {
        if (b == null) return;
        b.setBackgroundResource(selected ? R.drawable.lang_chip_selected_bg : R.drawable.lang_chip_unselected_bg);
        b.setTextColor(selected ? 0xFF2563EB : 0xFF9CA3AF);
    }

    // ── IMU dialog ───────────────────────────────────────────────────────────

    private void showImuSettingsDialog() {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_imu_settings);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels  * 0.88f),
                    (int) (getResources().getDisplayMetrics().heightPixels * 0.82f)
            );
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }

        ImageView ivDiagram = dialog.findViewById(R.id.ivJointDiagram);
        try {
            InputStream is = getAssets().open("images/preview.jpg");
            Bitmap bmp = BitmapFactory.decodeStream(is);
            ivDiagram.setImageBitmap(bmp);
            is.close();
        } catch (IOException e) {
            ivDiagram.setVisibility(View.GONE);
        }

        ImuPreferences.Params p = ImuPreferences.load(this);

        FieldRow rowBoomOffset   = new FieldRow(dialog, R.id.rowBoomOffset,       "大臂 IMU 偏移角 (°)",   ImuPreferences.fmt(p.boomImuOffsetDeg));
        FieldRow rowStickOffset  = new FieldRow(dialog, R.id.rowStickOffset,      "小臂 IMU 偏移角 (°)",   ImuPreferences.fmt(p.stickImuOffsetDeg));
        FieldRow rowBucketOffset = new FieldRow(dialog, R.id.rowBucketOffset,     "铲斗 IMU 偏移角 (°)",   ImuPreferences.fmt(p.bucketImuOffsetDeg));
        FieldRow rowBoomLength   = new FieldRow(dialog, R.id.rowBoomLength,       "大臂长度",               ImuPreferences.fmt(p.boomLength));
        FieldRow rowStickLength  = new FieldRow(dialog, R.id.rowStickLength,      "小臂长度",               ImuPreferences.fmt(p.stickLength));
        FieldRow rowBucketLength = new FieldRow(dialog, R.id.rowBucketLength,     "铲斗长度",               ImuPreferences.fmt(p.bucketLength));
        FieldRow rowBucketAO     = new FieldRow(dialog, R.id.rowBucketAngleOffset,"铲斗角偏移 (°)",         ImuPreferences.fmt(p.bucketAngleOffsetDeg));

        FieldRow rowBoomL2  = new FieldRow(dialog, R.id.rowBoomL2,  "boomL2",  ImuPreferences.fmt(p.boomL2));
        FieldRow rowBoomL3  = new FieldRow(dialog, R.id.rowBoomL3,  "boomL3",  ImuPreferences.fmt(p.boomL3));
        FieldRow rowBoomL4  = new FieldRow(dialog, R.id.rowBoomL4,  "boomL4",  ImuPreferences.fmt(p.boomL4));
        FieldRow rowBoomL5  = new FieldRow(dialog, R.id.rowBoomL5,  "boomL5",  ImuPreferences.fmt(p.boomL5));
        FieldRow rowBoomL6  = new FieldRow(dialog, R.id.rowBoomL6,  "boomL6",  ImuPreferences.fmt(p.boomL6));
        FieldRow rowBoomL7  = new FieldRow(dialog, R.id.rowBoomL7,  "boomL7",  ImuPreferences.fmt(p.boomL7));

        FieldRow rowStickL2 = new FieldRow(dialog, R.id.rowStickL2, "stickL2", ImuPreferences.fmt(p.stickL2));
        FieldRow rowStickL3 = new FieldRow(dialog, R.id.rowStickL3, "stickL3", ImuPreferences.fmt(p.stickL3));
        FieldRow rowStickL4 = new FieldRow(dialog, R.id.rowStickL4, "stickL4", ImuPreferences.fmt(p.stickL4));
        FieldRow rowStickL5 = new FieldRow(dialog, R.id.rowStickL5, "stickL5", ImuPreferences.fmt(p.stickL5));
        FieldRow rowStickL6 = new FieldRow(dialog, R.id.rowStickL6, "stickL6", ImuPreferences.fmt(p.stickL6));
        FieldRow rowStickL7 = new FieldRow(dialog, R.id.rowStickL7, "stickL7", ImuPreferences.fmt(p.stickL7));

        FieldRow rowBucketL2  = new FieldRow(dialog, R.id.rowBucketL2,  "bucketL2",  ImuPreferences.fmt(p.bucketL2));
        FieldRow rowBucketL3  = new FieldRow(dialog, R.id.rowBucketL3,  "bucketL3",  ImuPreferences.fmt(p.bucketL3));
        FieldRow rowBucketL4  = new FieldRow(dialog, R.id.rowBucketL4,  "bucketL4",  ImuPreferences.fmt(p.bucketL4));
        FieldRow rowBucketL5  = new FieldRow(dialog, R.id.rowBucketL5,  "bucketL5",  ImuPreferences.fmt(p.bucketL5));
        FieldRow rowBucketL6  = new FieldRow(dialog, R.id.rowBucketL6,  "bucketL6",  ImuPreferences.fmt(p.bucketL6));
        FieldRow rowBucketL7  = new FieldRow(dialog, R.id.rowBucketL7,  "bucketL7",  ImuPreferences.fmt(p.bucketL7));
        FieldRow rowBucketL9  = new FieldRow(dialog, R.id.rowBucketL9,  "bucketL9",  ImuPreferences.fmt(p.bucketL9));
        FieldRow rowBucketL10 = new FieldRow(dialog, R.id.rowBucketL10, "bucketL10", ImuPreferences.fmt(p.bucketL10));

        dialog.findViewById(R.id.btnImuDialogCancel).setOnClickListener(v -> dialog.dismiss());

        dialog.findViewById(R.id.btnImuDialogReset).setOnClickListener(v -> {
            ImuPreferences.Params def = new ImuPreferences.Params();
            rowBoomOffset.set(ImuPreferences.fmt(def.boomImuOffsetDeg));
            rowStickOffset.set(ImuPreferences.fmt(def.stickImuOffsetDeg));
            rowBucketOffset.set(ImuPreferences.fmt(def.bucketImuOffsetDeg));
            rowBoomLength.set(ImuPreferences.fmt(def.boomLength));
            rowStickLength.set(ImuPreferences.fmt(def.stickLength));
            rowBucketLength.set(ImuPreferences.fmt(def.bucketLength));
            rowBucketAO.set(ImuPreferences.fmt(def.bucketAngleOffsetDeg));
            rowBoomL2.set(ImuPreferences.fmt(def.boomL2));
            rowBoomL3.set(ImuPreferences.fmt(def.boomL3));
            rowBoomL4.set(ImuPreferences.fmt(def.boomL4));
            rowBoomL5.set(ImuPreferences.fmt(def.boomL5));
            rowBoomL6.set(ImuPreferences.fmt(def.boomL6));
            rowBoomL7.set(ImuPreferences.fmt(def.boomL7));
            rowStickL2.set(ImuPreferences.fmt(def.stickL2));
            rowStickL3.set(ImuPreferences.fmt(def.stickL3));
            rowStickL4.set(ImuPreferences.fmt(def.stickL4));
            rowStickL5.set(ImuPreferences.fmt(def.stickL5));
            rowStickL6.set(ImuPreferences.fmt(def.stickL6));
            rowStickL7.set(ImuPreferences.fmt(def.stickL7));
            rowBucketL2.set(ImuPreferences.fmt(def.bucketL2));
            rowBucketL3.set(ImuPreferences.fmt(def.bucketL3));
            rowBucketL4.set(ImuPreferences.fmt(def.bucketL4));
            rowBucketL5.set(ImuPreferences.fmt(def.bucketL5));
            rowBucketL6.set(ImuPreferences.fmt(def.bucketL6));
            rowBucketL7.set(ImuPreferences.fmt(def.bucketL7));
            rowBucketL9.set(ImuPreferences.fmt(def.bucketL9));
            rowBucketL10.set(ImuPreferences.fmt(def.bucketL10));
            Toast.makeText(this, "已填入默认值，点击[保存并应用]生效", Toast.LENGTH_SHORT).show();
        });

        dialog.findViewById(R.id.btnImuDialogSave).setOnClickListener(v -> {
            ImuPreferences.Params np = new ImuPreferences.Params();
            np.boomImuOffsetDeg   = rowBoomOffset.get(p.boomImuOffsetDeg);
            np.stickImuOffsetDeg  = rowStickOffset.get(p.stickImuOffsetDeg);
            np.bucketImuOffsetDeg = rowBucketOffset.get(p.bucketImuOffsetDeg);
            np.boomLength           = rowBoomLength.get(p.boomLength);
            np.stickLength          = rowStickLength.get(p.stickLength);
            np.bucketLength         = rowBucketLength.get(p.bucketLength);
            np.bucketAngleOffsetDeg = rowBucketAO.get(p.bucketAngleOffsetDeg);
            np.boomL2 = rowBoomL2.get(p.boomL2); np.boomL3 = rowBoomL3.get(p.boomL3);
            np.boomL4 = rowBoomL4.get(p.boomL4); np.boomL5 = rowBoomL5.get(p.boomL5);
            np.boomL6 = rowBoomL6.get(p.boomL6); np.boomL7 = rowBoomL7.get(p.boomL7);
            np.stickL2 = rowStickL2.get(p.stickL2); np.stickL3 = rowStickL3.get(p.stickL3);
            np.stickL4 = rowStickL4.get(p.stickL4); np.stickL5 = rowStickL5.get(p.stickL5);
            np.stickL6 = rowStickL6.get(p.stickL6); np.stickL7 = rowStickL7.get(p.stickL7);
            np.bucketL2  = rowBucketL2.get(p.bucketL2);  np.bucketL3  = rowBucketL3.get(p.bucketL3);
            np.bucketL4  = rowBucketL4.get(p.bucketL4);  np.bucketL5  = rowBucketL5.get(p.bucketL5);
            np.bucketL6  = rowBucketL6.get(p.bucketL6);  np.bucketL7  = rowBucketL7.get(p.bucketL7);
            np.bucketL9  = rowBucketL9.get(p.bucketL9);  np.bucketL10 = rowBucketL10.get(p.bucketL10);

            ImuPreferences.save(this, np);
            refreshImuPreviewLabels();

            Intent result = new Intent();
            result.putExtra("imu_config_updated", true);
            setResult(RESULT_OK, result);

            Toast.makeText(this, "IMU 参数已保存并应用", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    // ── FieldRow helper ──────────────────────────────────────────────────────

    private static final class FieldRow {
        private final EditText editText;

        FieldRow(Dialog dialog, int rowId, String label, String value) {
            LinearLayout row = dialog.findViewById(rowId);
            TextView tv = row.findViewById(R.id.tvFieldLabel);
            editText    = row.findViewById(R.id.etFieldValue);
            tv.setText(label);
            editText.setText(value);
        }

        void set(String value) { editText.setText(value); }

        double get(double fallback) {
            return ImuPreferences.parseOrDefault(editText.getText().toString(), fallback);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static String formatScaleForEdit(float v) {
        if (Math.abs(v - Math.round(v)) < 1e-6f) return String.valueOf(Math.round(v));
        return String.format(Locale.US, "%.4g", v);
    }

    private static float parseScale(String s, float defaultValue) {
        if (s == null) return defaultValue;
        String t = s.trim().replace(',', '.');
        if (t.isEmpty()) return defaultValue;
        try {
            float v = Float.parseFloat(t);
            return Float.isFinite(v) ? v : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void setFullScreenMode() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.hide(WindowInsetsCompat.Type.systemBars());
            controller.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) setFullScreenMode();
    }
}
