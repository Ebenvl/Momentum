package com.example.momentum.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatDelegate;

// The app's appearance settings: dark mode and text size. Saved on the phone,
// so they stay the same between visits and for whoever uses this phone.
public class AppSettings {

    private static final String FILE = "app_settings";
    private static final String THEME = "theme";
    private static final String TEXT_SIZE = "text_size";

    // Theme choices
    public static final String SYSTEM = "system";
    public static final String LIGHT = "light";
    public static final String DARK = "dark";

    // Text size choices, as how much bigger than normal. They're added on top of
    // the phone's own text size setting, so that still works too.
    public static final float[] TEXT_SIZES = {0.9f, 1.0f, 1.15f, 1.3f};
    public static final String[] TEXT_SIZE_NAMES = {"Small", "Normal", "Large", "Extra large"};

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static String getTheme(Context context) {
        return prefs(context).getString(THEME, SYSTEM);
    }

    public static void setTheme(Context context, String theme) {
        prefs(context).edit().putString(THEME, theme).apply();
        applyTheme(context);
    }

    // Switches the whole app to light, dark, or whatever the phone is set to.
    public static void applyTheme(Context context) {
        String theme = getTheme(context);
        int mode = DARK.equals(theme) ? AppCompatDelegate.MODE_NIGHT_YES
                : LIGHT.equals(theme) ? AppCompatDelegate.MODE_NIGHT_NO
                : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        if (AppCompatDelegate.getDefaultNightMode() != mode) {
            AppCompatDelegate.setDefaultNightMode(mode);
        }
    }

    public static float getTextSize(Context context) {
        return prefs(context).getFloat(TEXT_SIZE, 1.0f);
    }

    public static void setTextSize(Context context, float size) {
        prefs(context).edit().putFloat(TEXT_SIZE, size).apply();
    }

    // Gives a screen a version of the phone's settings with our text size applied.
    // Every screen uses this when it starts (see BaseActivity).
    public static Context withTextSize(Context base) {
        float size = getTextSize(base);
        if (size == 1.0f) {
            return base;
        }
        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.fontScale = config.fontScale * size;
        return base.createConfigurationContext(config);
    }
}
