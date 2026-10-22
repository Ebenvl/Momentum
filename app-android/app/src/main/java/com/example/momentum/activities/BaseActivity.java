package com.example.momentum.activities;

import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.momentum.data.AppSettings;

// Every screen in the app extends this instead of AppCompatActivity, so the
// text size and dark mode from Settings apply everywhere.
public class BaseActivity extends AppCompatActivity {

    // Runs before the screen is built: makes text bigger or smaller if the user chose that.
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(AppSettings.withTextSize(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppSettings.applyTheme(this);
        super.onCreate(savedInstanceState);
    }
}
