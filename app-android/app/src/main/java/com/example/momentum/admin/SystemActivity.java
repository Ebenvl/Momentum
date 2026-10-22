package com.example.momentum.admin;

import com.example.momentum.activities.BaseActivity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import android.widget.Toast;


import com.example.momentum.data.ApiCallback;
import com.example.momentum.model.ActivityLog;
import com.example.momentum.service.AdminService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SystemActivity extends BaseActivity {

    private final int NAVY = Color.rgb(7, 29, 53);
    private final int PURPLE = Color.rgb(118, 102, 232);
    private final int PALE = Color.rgb(244, 246, 250);
    private final int MUTED = Color.rgb(104, 119, 131);
    private final int WHITE = Color.WHITE;

    private String selectedFilter = "All";
    private String searchText = "";

    private LinearLayout root;
    private LinearLayout content;
    private LinearLayout filterRow;
    private TextView emptyMessage;

    // Backend connection
    private final AdminService adminService = new AdminService();
    private final ArrayList<ActivityLog> logs = new ArrayList<>();
    private LinearLayout logList;
    private LinearLayout emptyCard;
    private TextView eventCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildScreen();
    }

    private int dp(float value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private TextView text(String value, int color,
                          float size, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(color);
        view.setTextSize(size);

        if (bold) {
            view.setTypeface(null, Typeface.BOLD);
        }

        return view;
    }

    private void buildScreen() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(PALE);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(20), dp(18), dp(20), dp(20));
        header.setBackgroundColor(NAVY);

        TextView back = text("‹  Back to Dashboard", WHITE, 14, true);
        back.setPadding(0, dp(8), 0, dp(16));
        back.setClickable(true);
        back.setFocusable(true);
        back.setOnClickListener(v -> finish());
        header.addView(back);

        TextView title = text("System Activity", WHITE, 25, true);
        header.addView(title);

        TextView subtitle = text(
                "Monitor activity across LearnTech.",
                Color.LTGRAY, 14, false);
        subtitle.setPadding(0, dp(6), 0, 0);
        header.addView(subtitle);

        root.addView(header);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(18), dp(18), dp(24));

        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(
                -1, 0, 1f));

        setContentView(root);

        addCardTitle("Activity overview");
        eventCount = addStatCard("Recorded events", "—",
                "Available when activity data is connected");

        TextView searchLabel = text("Search activity", NAVY, 16, true);
        LinearLayout.LayoutParams labelParams =
                new LinearLayout.LayoutParams(-1, -2);
        labelParams.topMargin = dp(18);
        labelParams.bottomMargin = dp(8);
        content.addView(searchLabel, labelParams);

        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setTextSize(14);
        search.setTextColor(NAVY);
        search.setHintTextColor(MUTED);
        search.setHint("Search activity logs");
        search.setPadding(dp(14), 0, dp(14), 0);
        search.setBackground(rounded(WHITE, 12));

        content.addView(search, new LinearLayout.LayoutParams(
                -1, dp(50)));

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(
                    CharSequence s, int start, int before, int count) {
                searchText = s.toString();
                updateEmptyState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        TextView filterTitle = text("Activity type", NAVY, 16, true);
        LinearLayout.LayoutParams filterTitleParams =
                new LinearLayout.LayoutParams(-1, -2);
        filterTitleParams.topMargin = dp(20);
        filterTitleParams.bottomMargin = dp(10);
        content.addView(filterTitle, filterTitleParams);

        filterRow = new LinearLayout(this);
        filterRow.setOrientation(LinearLayout.HORIZONTAL);

        String[] filters = {
                "All", "Accounts", "Courses", "Security"
        };

        for (String filter : filters) {
            TextView chip = text(
                    filter,
                    selectedFilter.equals(filter) ? WHITE : NAVY,
                    12, true);

            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(12), dp(10), dp(12), dp(10));
            chip.setBackground(rounded(
                    selectedFilter.equals(filter) ? PURPLE : WHITE, 20));
            chip.setClickable(true);
            chip.setFocusable(true);

            LinearLayout.LayoutParams chipParams =
                    new LinearLayout.LayoutParams(-2, dp(40));
            chipParams.rightMargin = dp(7);

            filterRow.addView(chip, chipParams);

            chip.setOnClickListener(v -> {
                selectedFilter = filter;
                refreshFilters();
                updateEmptyState();
            });
        }

        ScrollView horizontal = new ScrollView(this);
        horizontal.setFillViewport(false);

        android.widget.HorizontalScrollView horizontalScroll =
                new android.widget.HorizontalScrollView(this);
        horizontalScroll.setHorizontalScrollBarEnabled(false);
        horizontalScroll.addView(filterRow);

        content.addView(horizontalScroll, new LinearLayout.LayoutParams(
                -1, dp(44)));

        addCardTitle("Recent activity");

        logList = new LinearLayout(this);
        logList.setOrientation(LinearLayout.VERTICAL);
        content.addView(logList, new LinearLayout.LayoutParams(-1, -2));

        emptyCard = new LinearLayout(this);
        emptyCard.setOrientation(LinearLayout.VERTICAL);
        emptyCard.setGravity(Gravity.CENTER);
        emptyCard.setPadding(dp(18), dp(28), dp(18), dp(28));
        emptyCard.setBackground(rounded(WHITE, 16));

        emptyMessage = text(
                "No activity recorded yet",
                NAVY, 17, true);
        emptyMessage.setGravity(Gravity.CENTER);
        emptyCard.addView(emptyMessage);

        TextView emptySubtitle = text(
                "System events will appear here when real activity "
                        + "data is connected.",
                MUTED, 13, false);
        emptySubtitle.setGravity(Gravity.CENTER);
        emptySubtitle.setPadding(0, dp(8), 0, 0);
        emptyCard.addView(emptySubtitle);

        content.addView(emptyCard, new LinearLayout.LayoutParams(
                -1, -2));

        updateEmptyState();
        loadLogs();
    }

    // Loads the latest activity and errors from the backend
    private void loadLogs() {
        adminService.getActivityLogs(new ApiCallback<List<ActivityLog>>() {
            @Override
            public void onSuccess(List<ActivityLog> result) {
                logs.clear();
                logs.addAll(result);
                eventCount.setText(String.valueOf(logs.size()));
                updateEmptyState();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(SystemActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    // Which activity type filter a log entry belongs to
    private boolean matchesFilter(ActivityLog log) {
        String action = log.action.toLowerCase(Locale.ROOT);
        switch (selectedFilter) {
            case "Accounts":
                return action.contains("user") || action.contains("login")
                        || action.contains("register");
            case "Courses":
                return action.contains("course") || action.contains("sync");
            case "Security":
                return "error".equals(log.level) || "warning".equals(log.level);
            default:
                return true;
        }
    }

    // One log entry in the list
    private LinearLayout logRow(ActivityLog log) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setBackground(rounded(WHITE, 14));

        row.addView(text(log.action, NAVY, 14, true));
        row.addView(text(log.level.toUpperCase(Locale.ROOT) + "  •  " + log.createdAt,
                "error".equals(log.level) ? PURPLE : MUTED, 12, false));
        return row;
    }

    private void addCardTitle(String value) {
        TextView heading = text(value, NAVY, 18, true);
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(12);
        content.addView(heading, params);
    }

    private TextView addStatCard(
            String label, String value, String description) {

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(rounded(WHITE, 16));

        TextView heading = text(label, MUTED, 13, false);
        card.addView(heading);

        TextView number = text(value, NAVY, 27, true);
        number.setPadding(0, dp(8), 0, dp(4));
        card.addView(number);

        card.addView(text(description, MUTED, 12, false));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(12);
        content.addView(card, params);

        return number;
    }

    private void refreshFilters() {
        for (int i = 0; i < filterRow.getChildCount(); i++) {
            TextView chip = (TextView) filterRow.getChildAt(i);
            String value = chip.getText().toString();
            boolean selected = value.equals(selectedFilter);

            chip.setTextColor(selected ? WHITE : NAVY);
            chip.setBackground(rounded(
                    selected ? PURPLE : WHITE, 20));
        }
    }

    private void updateEmptyState() {
        if (emptyMessage == null) return;

        // Shows the log entries that match the search and the type filter
        if (logList != null) {
            logList.removeAllViews();
            String query = searchText.trim().toLowerCase(Locale.ROOT);
            int shown = 0;
            for (ActivityLog log : logs) {
                if (!matchesFilter(log)) continue;
                if (!query.isEmpty()
                        && !log.action.toLowerCase(Locale.ROOT).contains(query)) continue;

                LinearLayout.LayoutParams rowParams =
                        new LinearLayout.LayoutParams(-1, -2);
                rowParams.bottomMargin = dp(10);
                logList.addView(logRow(log), rowParams);
                shown++;
            }
            emptyCard.setVisibility(shown > 0 ? View.GONE : View.VISIBLE);
        }

        if (!searchText.trim().isEmpty()) {
            emptyMessage.setText("No matching activity");
        } else if (!selectedFilter.equals("All")) {
            emptyMessage.setText(
                    "No " + selectedFilter.toLowerCase()
                            + " activity available");
        } else {
            emptyMessage.setText("No activity recorded yet");
        }
    }
}