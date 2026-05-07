package com.capstone.excavator;

import android.content.Context;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

public abstract class ScaledAppCompatActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(UiScaleConfig.wrap(newBase));
    }

    protected final void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
