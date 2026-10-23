package com.example.momentum.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.momentum.model.User;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

// Fingerprint or face login. Saves the login token (never the password),
// encrypted with a key in the phone's secure key storage that only unlocks
// after a fingerprint or face check. A new fingerprint on the phone, or a
// token older than 7 days, means the password is needed once more.
public class BiometricLogin {

    // What the learner did with the fingerprint prompt.
    public interface Result {
        void onSuccess();
        void onFailure(String message);
    }

    private static final String KEY_NAME = "momentum_biometric_login";
    private static final String FILE = "biometric_login";

    // Can this phone do it? It needs a strong fingerprint or face sensor that's set up.
    public static boolean isAvailable(Context context) {
        return BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                == BiometricManager.BIOMETRIC_SUCCESS;
    }

    // Has the learner turned it on, on this phone?
    public static boolean isEnabled(Context context) {
        return prefs(context).contains("token");
    }

    // Whose account it's turned on for, to show on the login screen.
    public static String savedEmail(Context context) {
        return prefs(context).getString("email", "");
    }

    // Turns it off, and forgets the saved login.
    public static void disable(Context context) {
        prefs(context).edit().clear().apply();
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            keyStore.deleteEntry(KEY_NAME);
        } catch (Exception ignored) {
            // nothing saved to remove
        }
    }

    // Turns it on for whoever is logged in now: asks for a fingerprint, then
    // saves their token encrypted.
    public static void enable(FragmentActivity activity, Result result) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, createKey());

            prompt(activity, "Turn on fingerprint login", cipher, new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult auth) {
                    try {
                        Cipher unlocked = auth.getCryptoObject().getCipher();
                        byte[] encrypted = unlocked.doFinal(Session.getToken().getBytes(StandardCharsets.UTF_8));
                        User user = Session.getUser();
                        prefs(activity).edit()
                                .putString("token", Base64.encodeToString(encrypted, Base64.NO_WRAP))
                                .putString("iv", Base64.encodeToString(unlocked.getIV(), Base64.NO_WRAP))
                                .putInt("userId", user.id)
                                .putString("email", user.email)
                                .putString("fullName", user.fullName)
                                .putString("role", user.role)
                                .apply();
                        result.onSuccess();
                    } catch (Exception e) {
                        result.onFailure("Could not turn on fingerprint login");
                    }
                }

                @Override
                public void onAuthenticationError(int code, CharSequence message) {
                    result.onFailure(message.toString());
                }
            });
        } catch (Exception e) {
            result.onFailure("Fingerprint login isn't available on this phone");
        }
    }

    // Logs in with a fingerprint: unlocks the saved token and starts the session with it.
    // The caller should then check the token still works (it may have run out).
    public static void login(FragmentActivity activity, Result result) {
        try {
            SharedPreferences saved = prefs(activity);
            byte[] iv = Base64.decode(saved.getString("iv", ""), Base64.NO_WRAP);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, existingKey(), new GCMParameterSpec(128, iv));

            prompt(activity, "Log in with your fingerprint", cipher, new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult auth) {
                    try {
                        byte[] encrypted = Base64.decode(saved.getString("token", ""), Base64.NO_WRAP);
                        String token = new String(auth.getCryptoObject().getCipher().doFinal(encrypted),
                                StandardCharsets.UTF_8);
                        User user = new User(saved.getInt("userId", 0), null, saved.getString("email", ""),
                                saved.getString("fullName", ""), saved.getString("role", "learner"), "active");
                        Session.start(user, token);
                        result.onSuccess();
                    } catch (Exception e) {
                        result.onFailure("Could not log in with your fingerprint");
                    }
                }

                @Override
                public void onAuthenticationError(int code, CharSequence message) {
                    result.onFailure(message.toString());
                }
            });
        } catch (KeyPermanentlyInvalidatedException e) {
            // A new fingerprint was added to the phone, so the old key is gone for good
            disable(activity);
            result.onFailure("Your phone's fingerprints changed. Log in with your password and turn it on again.");
        } catch (Exception e) {
            disable(activity);
            result.onFailure("Please log in with your password");
        }
    }

    // Shows Android's own fingerprint screen. The cipher only works after it succeeds.
    private static void prompt(FragmentActivity activity, String title, Cipher cipher,
                               BiometricPrompt.AuthenticationCallback callback) {
        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle("LearnTech Momentum")
                .setNegativeButtonText("Use password")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build();
        new BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback)
                .authenticate(info, new BiometricPrompt.CryptoObject(cipher));
    }

    // A new key in the phone's secure storage, that only works after a fingerprint
    // or face check, and is thrown away if the phone's fingerprints change.
    private static SecretKey createKey() throws Exception {
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(KEY_NAME,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(true)
                .build());
        return generator.generateKey();
    }

    private static SecretKey existingKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        return (SecretKey) keyStore.getKey(KEY_NAME, null);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }
}
