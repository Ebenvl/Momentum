package com.example.momentum.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.example.momentum.R;
import com.example.momentum.data.ApiCallback;
import com.example.momentum.data.BiometricLogin;
import com.example.momentum.data.Session;
import com.example.momentum.model.User;
import com.example.momentum.service.AuthService;
import com.example.momentum.service.ProfileService;

public class LoginActivity extends BaseActivity {

    private EditText editEmail;
    private EditText editPassword;
    private Button btnLogin;
    private TextView tvForgotPassword;
    private TextView tvGoToSignup;
    private ProgressBar progressBar;
    private final AuthService authService = new AuthService();
    private final ProfileService profileService = new ProfileService();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvGoToSignup = findViewById(R.id.tvGoToSignup);

        // Keep the Login screen visible when the app opens.
        // Do not automatically navigate to the Dashboard here.
        setLoading(false);

        btnLogin.setOnClickListener(v -> attemptLogin());

        tvForgotPassword.setOnClickListener(v -> resetPassword());

        tvGoToSignup.setOnClickListener(v -> {
            Intent intent = new Intent(
                    LoginActivity.this,
                    SignupActivity.class
            );
            startActivity(intent);
            finish();
        });


        // Fingerprint login: the button only shows once it's turned on, on this phone.
        // The fingerprint prompt also opens by itself, so it's one touch to get in.
        Button btnBiometric = findViewById(R.id.btnBiometric);
        if (BiometricLogin.isEnabled(this) && BiometricLogin.isAvailable(this)) {
            btnBiometric.setVisibility(View.VISIBLE);
            btnBiometric.setOnClickListener(v -> loginWithFingerprint());
            editEmail.setText(BiometricLogin.savedEmail(this));
            // post: waits until the screen is showing, the prompt can't open before that
            btnBiometric.post(this::loginWithFingerprint);
        }
    }


    // Unlocks the saved login with a fingerprint, then checks with the backend
    // that it still works. It runs out after 7 days, then the password is needed.
    private void loginWithFingerprint() {
        BiometricLogin.login(this, new BiometricLogin.Result() {
            @Override
            public void onSuccess() {
                setLoading(true);
                profileService.getMe(new ApiCallback<User>() {
                    @Override
                    public void onSuccess(User me) {
                        setLoading(false);
                        Session.getUser().fullName = me.fullName;
                        Session.getUser().role = me.role;
                        goToDashboard();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        setLoading(false);
                        Session.logout();
                        Toast.makeText(LoginActivity.this,
                                "Your saved login ran out. Log in with your password once.",
                                Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // After a password login: offers to turn on fingerprint login for next time,
    // if the phone can do it and it isn't on for this account already.
    private void offerFingerprintThenContinue(String email) {
        boolean alreadyOn = BiometricLogin.isEnabled(this) && email.equalsIgnoreCase(BiometricLogin.savedEmail(this));
        if (!BiometricLogin.isAvailable(this) || alreadyOn) {
            goToDashboard();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Use your fingerprint next time?")
                .setMessage("Log in with one touch instead of your password. Your password isn't saved.")
                .setPositiveButton("Turn on", (d, w) -> BiometricLogin.enable(this, new BiometricLogin.Result() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(LoginActivity.this, "Fingerprint login is on", Toast.LENGTH_SHORT).show();
                        goToDashboard();
                    }

                    @Override
                    public void onFailure(String message) {
                        goToDashboard();
                    }
                }))
                .setNegativeButton("Not now", (d, w) -> goToDashboard())
                .setCancelable(false)
                .show();
    }

    private void attemptLogin() {
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString();

        if (TextUtils.isEmpty(email)) {
            editEmail.setError("Email is required");
            editEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editEmail.setError("Enter a valid email address");
            editEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            editPassword.setError("Password is required");
            editPassword.requestFocus();
            return;
        }

        setLoading(true);

        authService.login(email, password, new ApiCallback<User>() {
            @Override
            public void onSuccess(User user) {
                setLoading(false);


                // Two-step login is on: ask for the code that was just emailed
                if (user == null) {
                    askForLoginCode(email);
                    return;
                }

                Toast.makeText(
                        LoginActivity.this,
                        "Login successful",
                        Toast.LENGTH_SHORT
                ).show();


                offerFingerprintThenContinue(email);
            }

            @Override
            public void onError(String errorMessage) {
                setLoading(false);

                Toast.makeText(
                        LoginActivity.this,
                        errorMessage,
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    // Forgot password: emails a 6-digit code to the address typed in, then asks
    // for that code and a new password.
    private void resetPassword() {
        EditText emailInput = codeField("Your email", InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailInput.setText(editEmail.getText().toString().trim());

        new AlertDialog.Builder(this)
                .setTitle("Reset your password")
                .setMessage("We'll email you a code to choose a new password.")
                .setView(padded(emailInput))
                .setPositiveButton("Send code", (d, w) -> {
                    String email = emailInput.getText().toString().trim();
                    if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        Toast.makeText(this, "Enter a valid email address", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    authService.forgotPassword(email, new ApiCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            askForResetCode(email);
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // The second half of a reset: the code from the email and the new password.
    private void askForResetCode(String email) {
        EditText code = codeField("6-digit code", InputType.TYPE_CLASS_NUMBER);
        EditText password = codeField("New password (at least 8 characters)",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        LinearLayout form = padded(code);
        form.addView(password);

        new AlertDialog.Builder(this)
                .setTitle("Check your email")
                .setMessage("If " + email + " has an account, a code is on its way. It lasts 10 minutes.")
                .setView(form)
                .setPositiveButton("Reset password", (d, w) -> authService.resetPassword(email,
                        code.getText().toString().trim(), password.getText().toString(), new ApiCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                editEmail.setText(email);
                                editPassword.setText("");
                                Toast.makeText(LoginActivity.this, "Password changed. Log in with the new one.",
                                        Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void onError(String errorMessage) {
                                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            }
                        }))
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Two-step login: the password was right, now the code from the email.
    private void askForLoginCode(String email) {
        EditText code = codeField("6-digit code", InputType.TYPE_CLASS_NUMBER);

        new AlertDialog.Builder(this)
                .setTitle("Check your email")
                .setMessage("We sent a login code to " + email + ". It lasts 10 minutes.")
                .setView(padded(code))
                .setCancelable(false)
                .setPositiveButton("Log in", (d, w) -> {
                    setLoading(true);
                    authService.verifyLogin(email, code.getText().toString().trim(), new ApiCallback<User>() {
                        @Override
                        public void onSuccess(User user) {
                            setLoading(false);
                            offerFingerprintThenContinue(email);
                        }

                        @Override
                        public void onError(String errorMessage) {
                            setLoading(false);
                            Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // A text box for the dialogs above.
    private EditText codeField(String hint, int inputType) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setSingleLine(true);
        input.setInputType(inputType);
        return input;
    }

    // Puts dialog fields in a column with some space around them.
    private LinearLayout padded(View field) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int side = Math.round(24 * getResources().getDisplayMetrics().density);
        form.setPadding(side, 0, side, 0);
        form.addView(field);
        return form;
    }

    private void setLoading(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(
                    loading ? View.VISIBLE : View.GONE
            );
        }

        if (btnLogin != null) {
            btnLogin.setEnabled(!loading);
        }
    }

    // Goes back to the main screens, which load the learner's data.
    private void goToDashboard() {
        finish();
    }
}
