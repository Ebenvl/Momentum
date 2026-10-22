package com.example.momentum.activities;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.momentum.R;
import com.example.momentum.data.ApiCallback;
import com.example.momentum.data.AppSettings;
import com.example.momentum.data.Session;
import com.example.momentum.model.User;
import com.example.momentum.service.ProfileService;

// Settings, for learners and admins: their name and password, and how the app
// looks (dark mode and text size).
public class SettingsActivity extends BaseActivity {

    private static final int NAVY = Color.rgb(7, 29, 53);
    private static final int MUTED = Color.rgb(104, 119, 131);
    private static final int GOLD = Color.rgb(244, 185, 66);

    private final ProfileService profileService = new ProfileService();
    private LinearLayout content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!Session.isLoggedIn()) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // The same look as the main screens: the navy background image behind
        // everything, a white title, and white cards.
        FrameLayout root = new FrameLayout(this);
        ImageView background = new ImageView(this);
        background.setImageResource(R.drawable.app_bg);
        background.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(background, new FrameLayout.LayoutParams(-1, -1));

        // fitsSystemWindows keeps the content clear of the phone's top and bottom bars
        ScrollView scroll = new ScrollView(this);
        scroll.setFitsSystemWindows(true);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(16), dp(20), dp(32));
        scroll.addView(content);
        root.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);

        // No see-through strip from Android behind the home and back buttons
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }

        TextView back = new TextView(this);
        back.setText("‹  Back");
        back.setTextSize(14);
        back.setTextColor(GOLD);
        back.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        back.setPadding(0, dp(8), 0, dp(12));
        back.setOnClickListener(v -> finish());
        content.addView(back);

        TextView title = new TextView(this);
        title.setText("Settings");
        title.setTextSize(26);
        title.setTextColor(Color.WHITE);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        content.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Your account and how the app looks");
        subtitle.setTextSize(14);
        subtitle.setTextColor(Color.argb(225, 255, 255, 255));
        content.addView(subtitle);

        addAccountSection();
        addPasswordSection();
        addAppearanceSection();
    }

    // -----------------------------------------------------
    // Account: who they are, and changing their name
    // -----------------------------------------------------
    private void addAccountSection() {
        User user = Session.getUser();
        LinearLayout card = section("Account");
        card.addView(label("Email: " + user.email));
        card.addView(label("Role: " + (user.isAdmin() ? "Administrator" : "Learner")));

        EditText name = input("Full name", false);
        name.setText(user.fullName);
        card.addView(name);

        card.addView(button("Save name", () -> {
            String newName = name.getText().toString().trim();
            if (newName.isEmpty()) {
                toast("Enter your name");
                return;
            }
            profileService.updateName(newName, new ApiCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Session.getUser().fullName = newName;
                    toast("Name saved");
                }

                @Override
                public void onError(String error) {
                    toast(error);
                }
            });
        }));
    }

    // -----------------------------------------------------
    // Password: the current one has to be right first
    // -----------------------------------------------------
    private void addPasswordSection() {
        LinearLayout card = section("Change password");
        EditText current = input("Current password", true);
        EditText newPassword = input("New password (at least 8 characters)", true);
        EditText confirm = input("Confirm new password", true);
        card.addView(current);
        card.addView(newPassword);
        card.addView(confirm);

        card.addView(button("Change password", () -> {
            String next = newPassword.getText().toString();
            if (next.length() < 8) {
                toast("The new password needs at least 8 characters");
                return;
            }
            if (!next.equals(confirm.getText().toString())) {
                toast("The new passwords don't match");
                return;
            }
            profileService.changePassword(current.getText().toString(), next, new ApiCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    current.setText("");
                    newPassword.setText("");
                    confirm.setText("");
                    toast("Password changed");
                }

                @Override
                public void onError(String error) {
                    toast(error);
                }
            });
        }));
    }

    // -----------------------------------------------------
    // Appearance: dark mode and text size, saved on this phone
    // -----------------------------------------------------
    private void addAppearanceSection() {
        LinearLayout card = section("Appearance");

        card.addView(label("Theme"));
        RadioGroup theme = new RadioGroup(this);
        String[] themes = {AppSettings.SYSTEM, AppSettings.LIGHT, AppSettings.DARK};
        String[] themeNames = {"Same as my phone", "Light", "Dark"};
        for (int i = 0; i < themes.length; i++) {
            RadioButton option = radio(themeNames[i]);
            theme.addView(option);
            option.setChecked(themes[i].equals(AppSettings.getTheme(this)));
            String chosen = themes[i];
            // Switching the theme redraws every open screen by itself
            option.setOnClickListener(v -> AppSettings.setTheme(this, chosen));
        }
        card.addView(theme);
        card.addView(label("Dark mode changes the main screens. The extra screens stay light for now."));

        card.addView(label("Text size"));
        RadioGroup size = new RadioGroup(this);
        for (int i = 0; i < AppSettings.TEXT_SIZES.length; i++) {
            RadioButton option = radio(AppSettings.TEXT_SIZE_NAMES[i]);
            size.addView(option);
            float chosen = AppSettings.TEXT_SIZES[i];
            option.setChecked(chosen == AppSettings.getTextSize(this));
            // Text size is read when a screen starts, so this screen redraws itself.
            // The main screens redraw when you go back to them.
            option.setOnClickListener(v -> {
                AppSettings.setTextSize(this, chosen);
                recreate();
            });
        }
        card.addView(size);
        card.addView(label("This adds to your phone's own text size setting."));
    }

    // -----------------------------------------------------
    // Building blocks, in the same style as the other extra screens
    // -----------------------------------------------------

    // A white card with a heading, added to the page.
    private LinearLayout section(String title) {
        TextView heading = new TextView(this);
        heading.setText(title);
        heading.setTextSize(18);
        heading.setTextColor(Color.WHITE);
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heading.setPadding(dp(4), dp(18), 0, dp(8));
        content.addView(heading);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackgroundResource(R.drawable.bg_card);
        content.addView(card);
        return card;
    }

    private TextView label(String value) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(13);
        view.setTextColor(MUTED);
        view.setPadding(0, dp(6), 0, dp(4));
        return view;
    }

    private EditText input(String hint, boolean password) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setSingleLine(true);
        input.setTextColor(NAVY);
        if (password) {
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        return input;
    }

    private RadioButton radio(String label) {
        RadioButton option = new RadioButton(this);
        option.setText(label);
        option.setTextColor(NAVY);
        return option;
    }

    private Button button(String label, Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setOnClickListener(v -> action.run());
        return button;
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
