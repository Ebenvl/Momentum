package com.example.momentum.placeholder;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.momentum.R;
import com.example.momentum.data.ApiCallback;
import com.example.momentum.data.Session;
import com.example.momentum.model.Stats;
import com.example.momentum.model.User;
import com.example.momentum.service.LeaderboardService;

// TEMPORARY home screen. Links to every other screen.
public class HomeActivity extends BaseActivity {

    private final LeaderboardService leaderboardService = new LeaderboardService();
    private TextView statsText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!requireLogin()) return;
        setContentView(R.layout.temp_home);
        applyInsets(findViewById(R.id.root));

        User user = Session.getUser();
        TextView welcomeText = findViewById(R.id.welcomeText);
        welcomeText.setText("Hi, " + user.fullName);
        statsText = findViewById(R.id.statsText);

        openOnClick(R.id.coursesButton, CoursesActivity.class);
        openOnClick(R.id.goalsButton, GoalsActivity.class);
        openOnClick(R.id.leaderboardButton, LeaderboardActivity.class);
        openOnClick(R.id.profileButton, ProfileActivity.class);

        Button adminButton = findViewById(R.id.adminButton);
        if (user.isAdmin()) {
            adminButton.setVisibility(View.VISIBLE);
            openOnClick(R.id.adminButton, AdminActivity.class);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Session.getUser() == null) return;
        leaderboardService.getStats(Session.getUser().id, new ApiCallback<Stats>() {
            @Override
            public void onSuccess(Stats stats) {
                statsText.setText(
                        "Points this month: " + stats.pointsThisMonth + "\n"
                        + "Total points: " + stats.totalPoints + "\n"
                        + "Current streak: " + stats.currentStreak + " days\n"
                        + "Badges: " + stats.badges);
            }

            @Override
            public void onError(String message) {
                statsText.setText(message);
            }
        });
    }

    private void openOnClick(int buttonId, Class<?> screen) {
        findViewById(buttonId).setOnClickListener(v -> startActivity(new Intent(this, screen)));
    }
}
