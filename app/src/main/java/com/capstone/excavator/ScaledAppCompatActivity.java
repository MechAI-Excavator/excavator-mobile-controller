package com.capstone.excavator;

import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;

public abstract class ScaledAppCompatActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(UiScaleConfig.wrap(newBase));
    }
}
