package com.example.momentum.admin;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class EngagementAnalytics extends AppCompatActivity {

    private final int NAVY = Color.rgb(7, 29, 53);
    private final int GOLD = Color.rgb(244, 185, 66);
    private final int PURPLE = Color.rgb(118, 102, 232);
    private final int TEAL = Color.rgb(53, 184, 165);
    private final int ORANGE = Color.rgb(228, 108, 40);
    private final int WHITE = Color.WHITE;
    private final int MUTED = Color.rgb(104, 119, 131);
    private final int PALE = Color.rgb(244, 246, 250);

    private LinearLayout content;

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
        addStat(row1, "Active learners", "—", PURPLE);
        addStat(row1, "Course completion", "—", TEAL);

        LinearLayout row2 = makeRow();
        LinearLayout.LayoutParams rowParams =
                new LinearLayout.LayoutParams(-1, -2);
        rowParams.topMargin = dp(12);
        content.addView(row2, rowParams);
        addStat(row2, "Time learning", "—", GOLD);
        addStat(row2, "Participation", "—", ORANGE);

        addSectionTitle("Learner engagement");

        addEmptyCard(
                "No engagement data yet",
                "Learner activity and participation summaries "
                        + "will appear here when real data is available."
        );

        addSectionTitle("Course performance");

        addEmptyCard(
                "No course data yet",
                "Course completion comparisons will appear here "
                        + "once courses and learner activity are connected."
        );

        addSectionTitle("Engagement trends");

        addEmptyCard(
                "No trend data yet",
                "Engagement charts will be available when historical "
                        + "activity data is provided by the backend."
        );

        TextView footer = text(
                "LEARNTECH ADMIN • DEVELOPMENT PREVIEW",
                10, MUTED, true);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footerParams =
                new LinearLayout.LayoutParams(-1, -2);
        footerParams.topMargin = dp(24);
        content.addView(footer, footerParams);

        setContentView(root);
    }

    private LinearLayout makeRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        return row;
    }

    private void addStat(LinearLayout row, String label,
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
    }

    private void addSectionTitle(String title) {
        TextView label = text(title, 14, NAVY, true);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(24);
        params.bottomMargin = dp(12);
        content.addView(label, params);
    }

    private void addEmptyCard(String title, String description) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(18), dp(24), dp(18), dp(24));
        box.setBackground(round(WHITE, 16));
        content.addView(box);

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