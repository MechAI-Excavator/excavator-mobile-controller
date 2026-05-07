package com.capstone.excavator;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class SlopeRepairSettingActivity extends ScaledAppCompatActivity {

    private View btnBack;
    private View btnHelp;
    private View btnNext;

    private View cardTopLine;
    private View cardBottomLine;
    private TextView tvTopLine;
    private TextView tvBottomLine;
    private ImageView imgSlopePreview;

    private HelpTooltip helpTooltip;
    private int selectedType = 0; // 0=上开口线 1=下开口线
    private boolean discardStateOnStop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFullScreenMode();
        setContentView(R.layout.activity_slope_repair_setting);

        bindViews();
        selectedType = SlopeRepairTaskState.getRepairType();
        setupCards();
        setupActions();
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnLevelBack);
        btnHelp = findViewById(R.id.btnLevelHelp);
        btnNext = findViewById(R.id.btnLevelNext);

        // Reuse existing ids in layout (named after ditch cards)
        cardTopLine = findViewById(R.id.cardDitchSquare);
        cardBottomLine = findViewById(R.id.cardDitchTrapezoid);
        tvTopLine = findViewById(R.id.tvDitchSquare);
        tvBottomLine = findViewById(R.id.tvDitchTrapezoid);
        imgSlopePreview = findViewById(R.id.imgSlopePreview);
    }

    private void setupCards() {
        if (cardTopLine != null) cardTopLine.setOnClickListener(v -> setSelectedType(0));
        if (cardBottomLine != null) cardBottomLine.setOnClickListener(v -> setSelectedType(1));
        applySelection();
    }

    private void setSelectedType(int type) {
        selectedType = type;
        applySelection();
    }

    private void applySelection() {
        if (cardTopLine != null) {
            cardTopLine.setBackground(getDrawable(
                    selectedType == 0 ? R.drawable.level_card_selected_bg : R.drawable.level_card_normal_bg
            ));
        }
        if (cardBottomLine != null) {
            cardBottomLine.setBackground(getDrawable(
                    selectedType == 1 ? R.drawable.level_card_selected_bg : R.drawable.level_card_normal_bg
            ));
        }

        if (tvTopLine != null) {
            tvTopLine.setTextColor(getColor(selectedType == 0 ? R.color.level_selected : R.color.level_unselected));
        }
        if (tvBottomLine != null) {
            tvBottomLine.setTextColor(getColor(selectedType == 1 ? R.color.level_selected : R.color.level_unselected));
        }
        if (imgSlopePreview != null) {
            imgSlopePreview.setImageResource(selectedType == 0
                    ? R.drawable.slope_top
                    : R.drawable.slope_bottom);
        }
    }

    private void setupActions() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                discardStateOnStop = true;
                SlopeRepairTaskState.reset();
                finish();
            });
        }

        helpTooltip = new HelpTooltip(this, "这里是帮助提示内容，你可以在此解释修坡类型与参数含义。");
        helpTooltip.attach(btnHelp);

        if (btnNext != null) {
            btnNext.setOnClickListener(v -> {
                saveCurrentState();
                startActivity(new Intent(this, SlopeRepairSecondSettingActivity.class));
            });
        }
    }

    private void saveCurrentState() {
        SlopeRepairTaskState.updateRepairType(selectedType);
    }

    @Override
    protected void onStop() {
        if (!discardStateOnStop) {
            saveCurrentState();
        }
        super.onStop();
        if (helpTooltip != null) helpTooltip.dismiss();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) setFullScreenMode();
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
}

