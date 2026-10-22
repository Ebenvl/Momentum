package com.example.momentum.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.example.momentum.R;
import com.example.momentum.data.ApiCallback;
import com.example.momentum.model.User;
import com.example.momentum.service.AuthService;

public class LoginActivity extends BaseActivity {

    private EditText editEmail;
    private EditText editPassword;
    private Button btnLogin;
    private TextView tvForgotPassword;
    private TextView tvGoToSignup;
    private ProgressBar progressBar;
    private final AuthService authService = new AuthService();

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

                Toast.makeText(
                        LoginActivity.this,
                        "Login successful",
                        Toast.LENGTH_SHORT
                ).show();

                goToDashboard();
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

    // Passwords are reset by an administrator, the app does not send emails.
    private void resetPassword() {
        Toast.makeText(
                LoginActivity.this,
                "Forgot your password? Ask an administrator to reset it for you.",
                Toast.LENGTH_LONG
        ).show();
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
