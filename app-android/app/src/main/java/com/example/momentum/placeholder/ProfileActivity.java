package com.example.momentum.placeholder;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.example.momentum.R;
import com.example.momentum.data.Session;
import com.example.momentum.model.User;
import com.example.momentum.service.AuthService;

// TEMPORARY profile screen.
public class ProfileActivity extends BaseActivity {

    private final AuthService authService = new AuthService();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!requireLogin()) return;
        setContentView(R.layout.temp_profile);
        applyInsets(findViewById(R.id.root));

        User user = Session.getUser();
        ((TextView) findViewById(R.id.nameText)).setText(user.fullName);
        ((TextView) findViewById(R.id.emailText)).setText(user.email);
        ((TextView) findViewById(R.id.linkText)).setText(user.isLinked()
                ? "Linked to learning account " + user.idst
                : "Not yet linked to a learning account");

        // TODO: save the reminder setting (FR-12)

        findViewById(R.id.logoutButton).setOnClickListener(v -> {
            authService.logout();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}
