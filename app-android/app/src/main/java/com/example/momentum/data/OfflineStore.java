package com.example.momentum.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.widget.Toast;

import com.example.momentum.model.User;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

// Keeps the app usable without signal.
//
// 1. It remembers who is logged in, so the app opens straight to their data.
//    Only the login token is saved (never the password), encrypted with a key
//    in the phone's secure key storage, so it can't be read off the phone.
// 2. It keeps a copy of what the backend last sent for the learner's own
//    screens: profile, courses, goals, check-ins, badges, timetable and the
//    leaderboard. When the backend can't be reached, the app shows that copy,
//    as it was at the last sync.
//
// Everything is in the app's private storage. Log out wipes it.
public class OfflineStore {

    private static final String KEY_NAME = "momentum_saved_login";
    private static final String LOGIN_FILE = "saved_login";

    // The whole app's context, so this works from any screen.
    private static Context app;

    // True while screens are showing saved copies, so the note shows once,
    // not on every screen.
    private static boolean offline;

    // Called by every screen when it opens (see BaseActivity).
    public static void init(Context context) {
        app = context.getApplicationContext();
    }


    // -----------------------------------------------------
    // The saved login
    // -----------------------------------------------------

    // Saves who logged in, with the token encrypted.
    public static void saveLogin(User user, String token) {
        if (app == null) {
            return;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key());
            byte[] encrypted = cipher.doFinal(token.getBytes(StandardCharsets.UTF_8));

            app.getSharedPreferences(LOGIN_FILE, Context.MODE_PRIVATE).edit()
                    .putInt("userId", user.id)
                    .putString("email", user.email)
                    .putString("fullName", user.fullName)
                    .putString("role", user.role)
                    .putString("token", Base64.encodeToString(encrypted, Base64.NO_WRAP))
                    .putString("iv", Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
                    .apply();
        } catch (Exception e) {
            // Without the secure key storage the login just isn't remembered
        }
    }

    // Logs the saved user back in when the app opens. Gives back false if
    // nobody is saved, or the saved login can't be read anymore.
    public static boolean restoreLogin() {
        if (app == null) {
            return false;
        }
        SharedPreferences saved = app.getSharedPreferences(LOGIN_FILE, Context.MODE_PRIVATE);
        if (!saved.contains("token")) {
            return false;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(),
                    new GCMParameterSpec(128, Base64.decode(saved.getString("iv", ""), Base64.NO_WRAP)));
            String token = new String(cipher.doFinal(
                    Base64.decode(saved.getString("token", ""), Base64.NO_WRAP)), StandardCharsets.UTF_8);

            User user = new User(saved.getInt("userId", 0), null, saved.getString("email", ""),
                    saved.getString("fullName", ""), saved.getString("role", "learner"), "active");
            Session.start(user, token);
            return true;
        } catch (Exception e) {
            saved.edit().clear().apply();
            return false;
        }
    }

    // Log out: forgets the login and wipes this user's saved copies.
    public static void forget(int userId) {
        if (app == null) {
            return;
        }
        app.getSharedPreferences(LOGIN_FILE, Context.MODE_PRIVATE).edit().clear().apply();
        copies(userId).edit().clear().apply();
        offline = false;
    }

    // A key in the phone's secure key storage. Made the first time, then reused.
    // Unlike the fingerprint key, it works without a fingerprint check.
    private static SecretKey key() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (keyStore.containsAlias(KEY_NAME)) {
            return (SecretKey) keyStore.getKey(KEY_NAME, null);
        }
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(KEY_NAME,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build());
        return generator.generateKey();
    }


    // -----------------------------------------------------
    // Saved copies of the backend's replies
    // -----------------------------------------------------

    // Saves a reply, for example the text of GET /goals, with the time it came in.
    static void saveCopy(String path, String text) {
        if (app == null || !Session.isLoggedIn()) {
            return;
        }
        copies(Session.getUser().id).edit()
                .putString(path, text)
                .putLong("time " + path, System.currentTimeMillis())
                .apply();
        offline = false;
    }

    // The saved reply for this address, or null if there isn't one.
    // Shows a note the first time the app falls back to saved copies.
    static String loadCopy(String path) {
        if (app == null || !Session.isLoggedIn()) {
            return null;
        }
        SharedPreferences saved = copies(Session.getUser().id);
        String text = saved.getString(path, null);
        if (text != null && !offline) {
            offline = true;
            String when = new SimpleDateFormat("d MMM, HH:mm", Locale.getDefault())
                    .format(new Date(saved.getLong("time " + path, 0)));
            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(app,
                    "No connection. Showing your saved data from " + when + ".",
                    Toast.LENGTH_LONG).show());
        }
        return text;
    }

    // For the reminder, which runs while the app is closed: the saved copy for
    // whoever is still logged in on this phone, or null.
    public static String savedCopy(Context context, String path) {
        SharedPreferences login = context.getSharedPreferences(LOGIN_FILE, Context.MODE_PRIVATE);
        if (!login.contains("token")) {
            return null;
        }
        return context.getSharedPreferences("saved_copies_" + login.getInt("userId", 0), Context.MODE_PRIVATE)
                .getString(path, null);
    }

    // Each user has their own file, so one learner never sees another's copies.
    private static SharedPreferences copies(int userId) {
        return app.getSharedPreferences("saved_copies_" + userId, Context.MODE_PRIVATE);
    }
}
