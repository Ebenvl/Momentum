package com.example.momentum.activities;
import com.example.momentum.R;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.momentum.data.ApiCallback;
import com.example.momentum.model.User;
import com.example.momentum.service.AuthService;

public class SignupActivity extends AppCompatActivity {

    private EditText editName, editEmail, editPassword;
    private Button btnSignup;
    private ProgressBar progressBar;
    private final AuthService authService = new AuthService();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        editName = findViewById(R.id.editName);
        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        btnSignup = findViewById(R.id.btnSignup);
        progressBar = findViewById(R.id.progressBar);
        TextView tvGoToLogin = findViewById(R.id.tvGoToLogin);

        btnSignup.setOnClickListener(v -> attemptSignup());

        tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void attemptSignup() {
        String name = editName.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            editName.setError("Name is required");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            editEmail.setError("Email is required");
            return;
        }
        // The backend needs at least 8 characters
        if (password.length() < 8) {
            editPassword.setError("Password must be at least 8 characters");
            return;
        }

        setLoading(true);

        authService.register(email, password, name, new ApiCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                logInNewUser(email, password);
            }

            @Override
            public void onError(String error) {
                setLoading(false);
                Toast.makeText(SignupActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    // Logs the new account straight in, so the learner doesn't have to type it again.
    private void logInNewUser(String email, String password) {
        authService.login(email, password, new ApiCallback<User>() {
            @Override
            public void onSuccess(User user) {
                setLoading(false);
                Toast.makeText(SignupActivity.this, "Account created!", Toast.LENGTH_SHORT).show();
                goToDashboard();
            }

            @Override
            public void onError(String error) {
                setLoading(false);
                Toast.makeText(SignupActivity.this, "Account created, please log in.",
                        Toast.LENGTH_LONG).show();
                startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                finish();
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSignup.setEnabled(!loading);
    }

    // Goes back to the main screens, which load the learner's data.
    private void goToDashboard() {
        finish();
    }
}
