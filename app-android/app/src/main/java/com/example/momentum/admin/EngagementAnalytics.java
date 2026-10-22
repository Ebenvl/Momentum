package com.example.momentum.admin;

import com.example.momentum.activities.BaseActivity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;


import com.example.momentum.data.ApiCallback;
import com.example.momentum.model.AdminStats;
import com.example.momentum.model.CourseStat;
import com.example.momentum.model.LeaderboardEntry;
import com.example.momentum.service.AdminService;
import com.example.momentum.service.LeaderboardService;

import java.util.List;

public class EngagementAnalytics extends BaseActivity {

    private final int NAVY = Color.rgb(7, 29, 53);
    private final int GOLD = Color.rgb(244, 185, 66);
    private final int PURPLE = Color.rgb(118, 102, 232);
    private final int TEAL = Color.rgb(53, 184, 165);
    private final int ORANGE = Color.rgb(228, 108, 40);
    private final int WHITE = Color.WHITE;
    private final int MUTED = Color.rgb(104, 119, 131);
    private final int PALE = Color.rgb(244, 246, 250);

    private LinearLayout content;


    // Filled from the backend once the screen is built
    private final AdminService adminService = new AdminService();
    private final LeaderboardService leaderboardService = new LeaderboardService();
    private TextView activeStat, completionStat, timeStat, participationStat;
    private LinearLayout learnerCard, courseCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildScreen();
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(PALE);

        // Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(20), dp(24), dp(20), dp(22));
        header.setBackgroundColor(NAVY);
        root.addView(header);

        TextView back = text("‹  Back to Dashboard", 14, GOLD, true);
        back.setPadding(0, 0, 0, dp(18));
        back.setOnClickListener(v -> finish());
        header.addView(back);

        header.addView(text("Engagement Analytics", 24, WHITE, true));

        TextView subtitle = text(
                "Understand learner participation and progress",
                13, Color.rgb(205, 216, 229), false);
        LinearLayout.LayoutParams subtitleParams =
                new LinearLayout.LayoutParams(-1, -2);
        subtitleParams.topMargin = dp(7);
        header.addView(subtitle, subtitleParams);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(20), dp(16), dp(24));
        scroll.addView(content);

        addSectionTitle("Overview");

        LinearLayout row1 = makeRow();
        content.addView(row1);
        activeStat = addStat(row1, "Active learners", "—", PURPLE);
        completionStat = addStat(row1, "Course completion", "—", TEAL);

        LinearLayout row2 = makeRow();
        LinearLayout.LayoutParams rowParams =
                new LinearLayout.LayoutParams(-1, -2);
        rowParams.topMargin = dp(12);
        content.addView(row2, rowParams);
        timeStat = addStat(row2, "Time learning", "—", GOLD);
        participationStat = addStat(row2, "Participation", "—", ORANGE);

        addSectionTitle("Learner engagement");

        learnerCard = new LinearLayout(this);
        learnerCard.setOrientation(LinearLayout.VERTICAL);
        content.addView(learnerCard);
        addEmptyCard(learnerCard,
                "No engagement data yet",
                "Learner activity and participation summaries "
                        + "will appear here when real data is available."
        );

        addSectionTitle("Course performance");

        courseCard = new LinearLayout(this);
        courseCard.setOrientation(LinearLayout.VERTICAL);
        content.addView(courseCard);
        addEmptyCard(courseCard,
                "No course data yet",
                "Course completion comparisons will appear here "
                        + "once courses and learner activity are connected."
        );

        TextView footer = text(
                "LEARNTECH ADMIN",
                10, MUTED, true);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footerParams =
                new LinearLayout.LayoutParams(-1, -2);
        footerParams.topMargin = dp(24);
        content.addView(footer, footerParams);

        setContentView(root);
        loadData();
    }


    // Fills the overview, the most engaged learners and the course numbers.
    private void loadData() {
        adminService.getStats(new ApiCallback<AdminStats>() {
            @Override
            public void onSuccess(AdminStats stats) {
                activeStat.setText(String.valueOf(stats.activeLearners));
                completionStat.setText(stats.completionRate + "%");
                timeStat.setText(stats.totalMinutes >= 60
                        ? (stats.totalMinutes / 60) + " h" : stats.totalMinutes + " min");
                participationStat.setText(stats.engagement + "%");
            }

            @Override
            public void onError(String message) {
                Toast.makeText(EngagementAnalytics.this, message, Toast.LENGTH_LONG).show();
            }
        });

        // The learners with the most points of all time, top 5
        leaderboardService.getLeaderboard(true, new ApiCallback<List<LeaderboardEntry>>() {
            @Override
            public void onSuccess(List<LeaderboardEntry> entries) {
                if (entries.isEmpty()) {
                    return;
                }
                learnerCard.removeAllViews();
                LinearLayout box = whiteBox(learnerCard);
                box.addView(text("Most points, all time", 13, MUTED, false));
                for (int i = 0; i < Math.min(5, entries.size()); i++) {
                    LeaderboardEntry entry = entries.get(i);
                    box.addView(numberRow(entry.rank + ".  " + entry.name, entry.points + " XP"));
                }
            }

            @Override
            public void onError(String message) {
                // Keeps the "no data" card
            }
        });

        // Each course: completed out of enrolled
        adminService.getCourseStats(new ApiCallback<List<CourseStat>>() {
            @Override
            public void onSuccess(List<CourseStat> courseStats) {
                if (courseStats.isEmpty()) {
                    return;
                }
                courseCard.removeAllViews();
                LinearLayout box = whiteBox(courseCard);
                box.addView(text("Completed out of enrolled", 13, MUTED, false));
                for (CourseStat courseStat : courseStats) {
                    int percent = Math.round(courseStat.completed * 100f / courseStat.enrolled);
                    box.addView(numberRow(courseStat.courseName,
                            courseStat.completed + " / " + courseStat.enrolled + "  (" + percent + "%)"));
                }
            }

            @Override
            public void onError(String message) {
                // Keeps the "no data" card
            }
        });
    }

    // A white card inside the given section, for a list of rows.
    private LinearLayout whiteBox(LinearLayout parent) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(14), dp(16), dp(14));
        box.setBackground(round(WHITE, 16));
        parent.addView(box);
        return box;
    }

    // One name and number row in a white card.
    private LinearLayout numberRow(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(8), 0, dp(8));
        row.addView(text(label, 14, NAVY, false), new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(text(value, 14, NAVY, true));
        return row;
    }

    private LinearLayout makeRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        return row;
    }

    private TextView addStat(LinearLayout row, String label,
                         String value, int accent) {

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(14), dp(14), dp(10), dp(14));
        box.setBackground(round(WHITE, 16));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(0, dp(112), 1);
        params.setMargins(dp(4), 0, dp(4), 0);
        row.addView(box, params);

        View stripe = new View(this);
        stripe.setBackground(round(accent, 4));
        box.addView(stripe,
                new LinearLayout.LayoutParams(dp(30), dp(4)));

        TextView number = text(value, 25, NAVY, true);
        LinearLayout.LayoutParams numberParams =
                new LinearLayout.LayoutParams(-1, -2);
        numberParams.topMargin = dp(10);
        box.addView(number, numberParams);

        TextView title = text(label, 11, MUTED, false);
        LinearLayout.LayoutParams titleParams =
                new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(5);
        box.addView(title, titleParams);
        return number;
    }

    private void addSectionTitle(String title) {
        TextView label = text(title, 14, NAVY, true);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(24);
        params.bottomMargin = dp(12);
        content.addView(label, params);
    }

    private void addEmptyCard(LinearLayout parent, String title, String description) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(18), dp(24), dp(18), dp(24));
        box.setBackground(round(WHITE, 16));
        parent.addView(box);

        TextView heading = text(title, 15, NAVY, true);
        heading.setGravity(Gravity.CENTER);
        box.addView(heading);

        TextView detail = text(description, 13, MUTED, false);
        detail.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams detailParams =
                new LinearLayout.LayoutParams(-1, -2);
        detailParams.topMargin = dp(8);
        box.addView(detail, detailParams);
    }

    private TextView text(String value, int size, int color,
                          boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);

        if (bold) {
            view.setTypeface(
                    android.graphics.Typeface.DEFAULT,
                    android.graphics.Typeface.BOLD);
        }

        return view;
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private int dp(float value) {
        return (int) (
                value * getResources().getDisplayMetrics().density
                        + 0.5f);
    }
}