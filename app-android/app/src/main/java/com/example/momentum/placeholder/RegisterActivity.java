package com.example.momentum.placeholder;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import com.example.momentum.R;
import com.example.momentum.data.ApiCallback;
import com.example.momentum.model.User;
import com.example.momentum.service.AuthService;

// TEMPORARY register screen.
public class RegisterActivity extends BaseActivity {

    private final AuthService authService = new AuthService();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.temp_register);
        applyInsets(findViewById(R.id.root));

        EditText nameInput = findViewById(R.id.nameInput);
        EditText emailInput = findViewById(R.id.emailInput);
        EditText passwordInput = findViewById(R.id.passwordInput);
        Button registerButton = findViewById(R.id.registerButton);
        Button backButton = findViewById(R.id.backButton);

        registerButton.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString();

            authService.register(email, password, name, new ApiCallback<User>() {
                @Override
                public void onSuccess(User user) {
                    toast("Account created, you can log in now");
                    finish();
                }

                @Override
                public void onError(String message) {
                    toast(message);
                }
            });
        });

        backButton.setOnClickListener(v -> finish());
    }
}
