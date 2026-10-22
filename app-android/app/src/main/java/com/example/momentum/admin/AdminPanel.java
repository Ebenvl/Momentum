package com.example.momentum.admin;

import com.example.momentum.activities.BaseActivity;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;


import com.example.momentum.data.ApiCallback;
import com.example.momentum.data.Session;
import com.example.momentum.model.ActivityLog;
import com.example.momentum.model.AdminStats;
import com.example.momentum.service.AdminService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminPanel extends BaseActivity {

    private final int NAVY = Color.rgb(7, 29, 53);
    private final int ORANGE = Color.rgb(228, 108, 40);
    private final int GOLD = Color.rgb(244, 185, 66);
    private final int PURPLE = Color.rgb(118, 102, 232);
    private final int TEAL = Color.rgb(53, 184, 165);
    private final int WHITE = Color.WHITE;
    private final int MUTED = Color.rgb(104, 119, 131);

    private LinearLayout content;

    // Backend connection
    private final AdminService adminService = new AdminService();
    private TextView totalUsersStat;
    private TextView activeCoursesStat;
    private TextView completionStat;
    private TextView engagementStat;
    private LinearLayout activityCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // Only administrators get in. The backend refuses admin data to anyone else too.
        if (!Session.isLoggedIn() || !Session.getUser().isAdmin()) {
            Toast.makeText(this, "Administrators only", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        showDashboard();
    }

    private void showDashboard() {
        PatternBackground background = new PatternBackground();
        setContentView(background);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        background.addView(scroll);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(32), dp(20), dp(24));
        scroll.addView(content);

        // Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout.LayoutParams headerParams =
                new LinearLayout.LayoutParams(-1, -2);
        headerParams.bottomMargin = dp(12);
        content.addView(header, headerParams);

        // Container for admin logo
        LinearLayout logoContainer = new LinearLayout(this);
        logoContainer.setGravity(Gravity.CENTER);
        logoContainer.setPadding(dp(5), dp(5), dp(5), dp(5));
        logoContainer.setBackground(round(WHITE, 8));

        LinearLayout.LayoutParams logoContainerParams =
                new LinearLayout.LayoutParams(dp(64), dp(58));
        logoContainerParams.setMargins(0, 0, dp(12), 0);
        header.addView(logoContainer, logoContainerParams);

        // Admin Logo
        ImageView logo = new ImageView(this);
        logo.setImageResource(
                com.example.momentum.R.drawable.admin_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setAdjustViewBounds(true);

        logoContainer.addView(
                logo, new LinearLayout.LayoutParams(-1, -1));

        // Dashboard heading
        LinearLayout headingContainer = new LinearLayout(this);
        headingContainer.setOrientation(LinearLayout.VERTICAL);
        headingContainer.setGravity(Gravity.CENTER_VERTICAL);

        header.addView(headingContainer,
                new LinearLayout.LayoutParams(0, -2, 1));

        TextView brand = text("LEARNTECH / ADMIN", 10, GOLD, true);
        headingContainer.addView(brand);

        TextView heading = text("Admin Dashboard", 22, WHITE, true);
        LinearLayout.LayoutParams headingParams =
                new LinearLayout.LayoutParams(-1, -2);
        headingParams.topMargin = dp(5);
        headingContainer.addView(heading, headingParams);

        if (Session.isLoggedIn()) {
            TextView adminName = text(
                    Session.getUser().fullName + " • Admin",
                    12, Color.rgb(205, 216, 229), false);
            headingContainer.addView(adminName);
        }

        TextView subtitle = text(
                "Manage your learning platform",
                14, Color.rgb(205, 216, 229), false);

        LinearLayout.LayoutParams subtitleParams =
                new LinearLayout.LayoutParams(-1, -2);
        subtitleParams.topMargin = dp(4);
        content.addView(subtitle, subtitleParams);

        // Statistics
        LinearLayout statsRow1 = row();
        content.addView(statsRow1);

        totalUsersStat = addStat(statsRow1, "Total users", "—", PURPLE);
        activeCoursesStat = addStat(statsRow1, "Active courses", "—", TEAL);


        LinearLayout statsRow2 = row();

        LinearLayout.LayoutParams rowParams =
                new LinearLayout.LayoutParams(-1, -2);
        rowParams.topMargin = dp(12);
        content.addView(statsRow2, rowParams);

        completionStat = addStat(statsRow2, "Completion rate", "—", GOLD);
        engagementStat = addStat(statsRow2, "Active this week", "—", ORANGE);


        addSectionTitle("Quick actions");

        //Actions for tabs
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        content.addView(actions);

        addAction(actions,
                "Manage users",
                "View user management",
                PURPLE,
                () -> {
                    Intent intent = new Intent(
                            AdminPanel.this, UserTab.class);
                    startActivity(intent);
                });

        addAction(actions,
                "Course management",
                "Points and expected time per course",
                TEAL,
                () -> startActivity(
                        new Intent(
                                AdminPanel.this,
                                CourseManagement.class
                        )
                ));



        // Badges, the LearnTech connection and data export
        addAction(actions,
                "Badge management",
                "Badges, their rules and points",
                PURPLE,
                () -> startActivity(new Intent(AdminPanel.this, BadgeManagement.class)));

        addAction(actions,
                "LearnTech connection",
                "Platform address, API key and test connection",
                TEAL,
                () -> startActivity(new Intent(AdminPanel.this, LearnTechSettings.class)));

        addAction(actions,
                "Export data",
                "Save tables as CSV for backup or reporting",
                GOLD,
                () -> startActivity(new Intent(AdminPanel.this, DataExport.class)));

        addAction(actions,
                "System activity",
                "View system events",
                TEAL,
                () -> startActivity(
                        new Intent(AdminPanel.this, SystemActivity.class)
                ));
        addAction(actions,
                "Engagement analytics",
                "Explore engagement data",
                GOLD,
                () -> startActivity(
                        new Intent(
                                AdminPanel.this,
                                EngagementAnalytics.class
                        )
                ));

        //
        addSectionTitle("Recent Activity");

        activityCard = card();
        content.addView(activityCard);

        TextView empty = text(
                "No activity yet.",
                14, MUTED, false);

        empty.setGravity(Gravity.CENTER);
        empty.setPadding(dp(12), dp(24), dp(12), dp(24));
        activityCard.addView(empty);

        // Footer
        TextView footer = text(
                "LEARNTECH ADMIN",
                10, Color.rgb(205, 216, 229), true);

        footer.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams footerParams =
                new LinearLayout.LayoutParams(-1, -2);
        footerParams.topMargin = dp(24);
        content.addView(footer, footerParams);

        loadDashboard();
    }

    // Fills the stats and the recent activity from the backend
    private void loadDashboard() {
        adminService.getStats(new ApiCallback<AdminStats>() {
            @Override
            public void onSuccess(AdminStats stats) {
                totalUsersStat.setText(String.valueOf(stats.totalUsers));
                activeCoursesStat.setText(String.valueOf(stats.activeCourses));
                completionStat.setText(stats.completionRate + "%");
                engagementStat.setText(stats.engagement + "%");
            }

            @Override
            public void onError(String message) {
                Toast.makeText(AdminPanel.this, message, Toast.LENGTH_LONG).show();
            }
        });

        adminService.getActivityLogs(new ApiCallback<List<ActivityLog>>() {
            @Override
            public void onSuccess(List<ActivityLog> logs) {
                if (logs.isEmpty()) {
                    return;
                }
                activityCard.removeAllViews();

                // The 3 newest entries, like the mockup's activity feed
                for (int i = 0; i < Math.min(3, logs.size()); i++) {
                    activityCard.addView(activityRow(logs.get(i)));
                }
            }

            @Override
            public void onError(String message) {
                // Keeps the "no activity" message
            }
        });
    }

    // One entry in the activity feed: a coloured marker, what happened, and how long ago
    private LinearLayout activityRow(ActivityLog log) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));

        View marker = new View(this);
        marker.setBackground(round("error".equals(log.level) ? ORANGE : TEAL, 8));
        LinearLayout.LayoutParams markerParams =
                new LinearLayout.LayoutParams(dp(28), dp(28));
        markerParams.rightMargin = dp(10);
        row.addView(marker, markerParams);

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text(log.action, 13, NAVY, true));
        labels.addView(text(log.level, 11, MUTED, false));
        row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));

        row.addView(text(timeAgo(log.createdAt), 11, MUTED, false));
        return row;
    }

    // "2026-10-18 14:07:38" becomes "10 mins ago", "4 hours ago" or "3 days ago"
    private String timeAgo(String dateTime) {
        try {
            Date then = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(dateTime);
            long minutes = (System.currentTimeMillis() - then.getTime()) / 60000;
            if (minutes < 1) return "just now";
            if (minutes < 60) return minutes + " mins ago";
            if (minutes < 60 * 24) return (minutes / 60) + " hours ago";
            return (minutes / (60 * 24)) + " days ago";
        } catch (Exception e) {
            return "";
        }
    }

    private void addSectionTitle(String title) {
        TextView label = text(title, 12, GOLD, true);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(28);
        params.bottomMargin = dp(12);

        content.addView(label, params);
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        return row;
    }

    private TextView addStat(LinearLayout row, String label,
                         String value, int accent) {

        LinearLayout box = card();
        box.setBackground(round(WHITE, 18));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(0, dp(112), 1);
        params.setMargins(dp(4), 0, dp(4), 0);
        row.addView(box, params);

        View stripe = new View(this);
        stripe.setBackground(round(accent, 4));
        box.addView(stripe,
                new LinearLayout.LayoutParams(dp(34), dp(4)));

        TextView number = text(value, 27, NAVY, true);

        LinearLayout.LayoutParams numberParams =
                new LinearLayout.LayoutParams(-1, -2);
        numberParams.topMargin = dp(12);
        box.addView(number, numberParams);

        TextView title = text(label, 12, MUTED, false);

        LinearLayout.LayoutParams titleParams =
                new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(4);
        box.addView(title, titleParams);

        return number;
    }

    private void addAction(LinearLayout parent,
                           String title,
                           String description,
                           int accent,
                           Runnable action) {

        LinearLayout box = card();
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setBackground(round(WHITE, 16));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(10);
        parent.addView(box, params);

        View marker = new View(this);
        marker.setBackground(round(accent, 8));

        LinearLayout.LayoutParams markerParams =
                new LinearLayout.LayoutParams(dp(5), dp(42));
        markerParams.setMargins(dp(14), 0, dp(12), 0);
        box.addView(marker, markerParams);

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setPadding(0, dp(14), 0, dp(14));

        box.addView(labels,
                new LinearLayout.LayoutParams(0, -2, 1));

        labels.addView(text(title, 15, NAVY, true));

        TextView detail = text(description, 12, MUTED, false);

        LinearLayout.LayoutParams detailParams =
                new LinearLayout.LayoutParams(-1, -2);
        detailParams.topMargin = dp(4);
        labels.addView(detail, detailParams);

        TextView arrow = text("›", 28, accent, true);
        arrow.setPadding(dp(10), 0, dp(16), 0);
        box.addView(arrow);

        box.setClickable(true);
        box.setFocusable(true);
        box.setOnClickListener(v -> action.run());
    }

    private LinearLayout card() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(14), dp(16), dp(14));
        box.setBackground(round(WHITE, 18));
        return box;
    }

    private TextView text(String value, int size, int color,
                          boolean bold) {

        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setFontFeatureSettings("kern");

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

    // Dashboard background
    private class PatternBackground extends LinearLayout {

        private final Paint paint =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        PatternBackground() {
            super(AdminPanel.this);
            setOrientation(VERTICAL);
            setBackgroundColor(NAVY);
            setWillNotDraw(false);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            paint.setColor(Color.rgb(13, 48, 73));
            paint.setStrokeWidth(dp(2));

            for (int i = -2; i < 8; i++) {
                float startX =
                        getWidth() * 0.65f + dp(i * 32);

                Path path = new Path();
                path.moveTo(startX, 0);
                path.lineTo(startX - dp(150), dp(210));

                canvas.drawPath(path, paint);
            }

            paint.setColor(ORANGE);
            paint.setStrokeWidth(dp(5));

            for (int i = 0; i < 4; i++) {
                float x = getWidth() - dp(20 + i * 32);
                canvas.drawLine(
                        x, 0, x - dp(130), dp(150), paint);
            }

            paint.setColor(GOLD);

            for (int i = 0; i < 18; i++) {
                float x = dp(20 + (i % 6) * 24);
                float y = dp(90 + (i / 6) * 26);

                canvas.drawCircle(x, y, dp(2), paint);
            }
        }
    }
}