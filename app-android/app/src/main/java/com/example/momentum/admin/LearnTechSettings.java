package com.example.momentum.admin;

import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.example.momentum.data.ApiCallback;
import com.example.momentum.model.PlatformSettings;
import com.example.momentum.service.AdminService;

// The LearnTech connection: the platform address, API key and secret, and a
// "test connection" button. The key and secret are sent to the backend, which
// keeps them encrypted. The phone only ever gets the masked version back.
public class LearnTechSettings extends AdminScreen {

    private final AdminService adminService = new AdminService();

    @Override
    protected String title() {
        return "LearnTech Connection";
    }

    @Override
    protected String subtitle() {
        return "The learning platform the app reads courses and learners from";
    }

    @Override
    protected void load() {
        content.removeAllViews();
        note("Loading settings...");

        adminService.getSettings(new ApiCallback<PlatformSettings>() {
            @Override
            public void onSuccess(PlatformSettings settings) {
                content.removeAllViews();
                showSettings(settings);
            }

            @Override
            public void onError(String message) {
                content.removeAllViews();
                note(message);
            }
        });
    }

    private void showSettings(PlatformSettings settings) {
        // What's in use right now
        LinearLayout current = card();
        current.addView(text("In use now", 15, NAVY, true));
        current.addView(text("Address: " + settings.platformUrl, 13, MUTED, false));
        current.addView(text("API key: " + settings.apiKey, 13, MUTED, false));
        current.addView(text("Secret: " + (settings.apiSecret != null ? settings.apiSecret : "none"), 13, MUTED, false));
        current.addView(text("Last tested: " + (settings.lastTestedAt != null ? settings.lastTestedAt : "never"),
                13, MUTED, false));
        current.addView(text(settings.savedByAdmin ? "Saved from this screen"
                : "From the backend's config file", 13, MUTED, false));

        button("Test connection", false, v -> testConnection());

        // Changing them
        heading("Change the connection");
        content.addView(text("Leave the key or secret empty to keep the current one. "
                + "A new address needs the key typed in again.", 13, MUTED, false));

        EditText address = field("Platform address, https://...", settings.platformUrl, false);
        EditText key = field("New API key", null, false);
        key.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        EditText secret = field("New secret (optional)", null, false);
        secret.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        LinearLayout fields = card();
        fields.addView(address);
        fields.addView(key);
        fields.addView(secret);

        button("Save", true, v -> {
            String url = address.getText().toString().trim();
            if (url.isEmpty()) {
                toast("Enter the platform address");
                return;
            }
            String newKey = key.getText().toString().trim();
            String newSecret = secret.getText().toString().trim();
            adminService.saveSettings(url, newKey.isEmpty() ? null : newKey,
                    newSecret.isEmpty() ? null : newSecret, new ApiCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            toast("Settings saved");
                            load();
                        }

                        @Override
                        public void onError(String message) {
                            toast(message);
                        }
                    });
        });
    }

    // Asks the backend to try LearnTech with the settings in use, and says how it went.
    private void testConnection() {
        toast("Testing...");
        adminService.testConnection(new ApiCallback<Integer>() {
            @Override
            public void onSuccess(Integer courses) {
                toast("Connected. LearnTech has " + courses + " courses.");
                load();
            }

            @Override
            public void onError(String message) {
                toast(message);
            }
        });
    }
}
