package com.example.momentum.activities;

import android.app.AlertDialog;
import android.content.Intent;
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
import com.example.momentum.data.BiometricLogin;
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
        addTwoStepSection();
        addFingerprintSection();
        addRemindersSection();
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
    // Two-step login: after the password, a code emailed to them
    // -----------------------------------------------------
    private void addTwoStepSection() {
        LinearLayout card = section("Two-step login");
        TextView status = label("Checking...");
        card.addView(status);

        profileService.getMe(new ApiCallback<User>() {
            @Override
            public void onSuccess(User me) {
                boolean on = me.twoFactorEnabled;
                status.setText(on
                        ? "On. After your password, you'll also need a code we email to " + me.email + "."
                        : "Off. Turn it on to also need a code from your email when you log in, "
                        + "so your password alone isn't enough.");
                if (on) {
                    card.addView(button("Turn off", () -> turnOffTwoStep()));
                } else {
                    card.addView(button("Turn on", () -> turnOnTwoStep()));
                }
            }

            @Override
            public void onError(String error) {
                status.setText(error);
            }
        });
    }

    // Emails a code to check the address is really theirs, then turns it on with it.
    private void turnOnTwoStep() {
        profileService.startTwoFactor(new ApiCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                EditText code = input("6-digit code", false);
                code.setInputType(InputType.TYPE_CLASS_NUMBER);
                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle("Check your email")
                        .setMessage("We sent a code to " + Session.getUser().email + ". Enter it to turn on two-step login.")
                        .setView(dialogForm(code))
                        .setPositiveButton("Turn on", (d, w) -> profileService.confirmTwoFactor(
                                code.getText().toString().trim(), refreshAfter("Two-step login is on")))
                        .setNegativeButton("Cancel", null)
                        .show();
            }

            @Override
            public void onError(String error) {
                toast(error);
            }
        });
    }

    // Turning it off needs the password, so a phone left logged in can't do it.
    private void turnOffTwoStep() {
        EditText password = input("Your password", true);
        new AlertDialog.Builder(this)
                .setTitle("Turn off two-step login?")
                .setView(dialogForm(password))
                .setPositiveButton("Turn off", (d, w) -> profileService.disableTwoFactor(
                        password.getText().toString(), refreshAfter("Two-step login is off")))
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Shows a message and redraws the screen when a change worked.
    private ApiCallback<Void> refreshAfter(String message) {
        return new ApiCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                toast(message);
                recreate();
            }

            @Override
            public void onError(String error) {
                toast(error);
            }
        };
    }

    // A dialog's fields with some space around them.
    private LinearLayout dialogForm(EditText field) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(24), 0, dp(24), 0);
        form.addView(field);
        return form;
    }

    // -----------------------------------------------------
    // Fingerprint login: turn it on or off for this phone
    // -----------------------------------------------------
    private void addFingerprintSection() {
        if (!BiometricLogin.isAvailable(this)) {
            return;   // this phone has no fingerprint or face sensor set up
        }
        LinearLayout card = section("Fingerprint login");
        boolean on = BiometricLogin.isEnabled(this)
                && Session.getUser().email.equalsIgnoreCase(BiometricLogin.savedEmail(this));
        card.addView(label(on ? "On. You can log in with your fingerprint on this phone."
                : "Off. Turn it on to log in with one touch instead of your password."));

        card.addView(button(on ? "Turn off" : "Turn on", () -> {
            if (on) {
                BiometricLogin.disable(this);
                toast("Fingerprint login is off");
                recreate();
            } else {
                BiometricLogin.enable(this, new BiometricLogin.Result() {
                    @Override
                    public void onSuccess() {
                        toast("Fingerprint login is on");
                        recreate();
                    }

                    @Override
                    public void onFailure(String message) {
                        toast(message);
                    }
                });
            }
        }));
    }

    // -----------------------------------------------------
    // Reminders: the daily study reminder, on this phone
    // -----------------------------------------------------
    private void addRemindersSection() {
        LinearLayout card = section("Reminders");
        card.addView(label("A daily reminder on this phone, at a time you choose."));
        card.addView(button("Set up reminders", () -> startActivity(new Intent(this, RemindersActivity.class))));
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
