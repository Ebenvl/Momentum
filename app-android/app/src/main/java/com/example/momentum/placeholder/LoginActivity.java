package com.example.momentum.placeholder;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import com.example.momentum.R;
import com.example.momentum.data.ApiCallback;
import com.example.momentum.model.User;
import com.example.momentum.service.AuthService;

// TEMPORARY login screen.
public class LoginActivity extends BaseActivity {

    private final AuthService authService = new AuthService();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.temp_login);
        applyInsets(findViewById(R.id.root));

        EditText emailInput = findViewById(R.id.emailInput);
        EditText passwordInput = findViewById(R.id.passwordInput);
        Button loginButton = findViewById(R.id.loginButton);
        Button goRegisterButton = findViewById(R.id.goRegisterButton);

        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString();

            authService.login(email, password, new ApiCallback<User>() {
                @Override
                public void onSuccess(User user) {
                    startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                    finish();
                }

                @Override
                public void onError(String message) {
                    toast(message);
                }
            });
        });

        goRegisterButton.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }
}
