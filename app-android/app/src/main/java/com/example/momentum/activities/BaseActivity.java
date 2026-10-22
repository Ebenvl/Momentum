package com.example.momentum.activities;

import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.momentum.data.AppSettings;
import com.example.momentum.data.OfflineStore;
import com.example.momentum.data.Session;

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


        // Logs the saved user back in when the app opens, so their data shows
        // straight away, with or without signal.
        OfflineStore.init(this);
        if (!Session.isLoggedIn()) {
            OfflineStore.restoreLogin();
        }
    }
}
