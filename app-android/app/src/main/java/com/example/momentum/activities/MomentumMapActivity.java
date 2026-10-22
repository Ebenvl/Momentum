
package com.example.momentum.activities;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.example.momentum.R;
import com.example.momentum.data.Session;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class MomentumMapActivity extends BaseActivity {

    private TextView tvMonth;
    private TextView tvStudyStreak;
    private TextView tvStudyDays;
    private TextView tvSelectedDate;

    private GridLayout calendarGrid;

    private Button btnBack;
    private Button btnPreviousMonth;
    private Button btnNextMonth;

    private SharedPreferences preferences;

    private Set<String> studyDates = new HashSet<>();

    private Calendar displayedMonth;
    private Calendar selectedDate;

    private final SimpleDateFormat dateKeyFormat =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private final SimpleDateFormat readableDateFormat =
            new SimpleDateFormat(
                    "EEEE, d MMMM yyyy",
                    Locale.getDefault()
            );

    private final String[] weekdays = {
            "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"
    };

    private final int blue = Color.rgb(118, 102, 232);
    private final int green = Color.rgb(53, 184, 165);
    private final int lightBlue = Color.rgb(237, 234, 255);
    private final int muted = Color.rgb(104, 119, 131);
    private final int border = Color.rgb(225, 230, 240);
    private final int dark = Color.rgb(7, 29, 53);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_momentum_map);

        if (!Session.isLoggedIn()) {
            Toast.makeText(
                    this,
                    "Please log in first",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        int userId = Session.getUser().id;

        // Use the same preferences as the course details screen.
        preferences = getSharedPreferences(
                "StudyStreakPrefs_" + userId,
                MODE_PRIVATE
        );

        // Connect the XML views.
        btnBack = findViewById(R.id.btnBack);
        tvMonth = findViewById(R.id.tvMonth);
        tvStudyStreak = findViewById(R.id.tvStudyStreak);
        tvStudyDays = findViewById(R.id.tvStudyDays);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);

        calendarGrid = findViewById(R.id.calendarGrid);

        btnPreviousMonth = findViewById(R.id.btnPreviousMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);

        // Back button.
        btnBack.setOnClickListener(v -> finish());

        // Start with the current month.
        displayedMonth = Calendar.getInstance();
        displayedMonth.set(Calendar.DAY_OF_MONTH, 1);

        selectedDate = Calendar.getInstance();

        tvSelectedDate.setText(
                readableDateFormat.format(selectedDate.getTime())
        );

        // Previous month.
        btnPreviousMonth.setOnClickListener(v -> {
            displayedMonth.add(Calendar.MONTH, -1);
            renderCalendar();
        });

        // Next month, but never beyond the current month.
        btnNextMonth.setOnClickListener(v -> {
            if (isBeforeCurrentMonth(displayedMonth)) {
                displayedMonth.add(Calendar.MONTH, 1);
                renderCalendar();
            }
        });

        refreshStudyData();
        renderCalendar();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (preferences != null) {
            refreshStudyData();
            renderCalendar();
        }
    }

    // Load the saved study history and streak.
    private void refreshStudyData() {

        Set<String> savedDates = preferences.getStringSet(
                "studyDates",
                new HashSet<>()
        );

        // Always copy the saved set before using it.
        studyDates = new HashSet<>(savedDates);

        int streak = preferences.getInt("studyStreak", 0);

        String lastStudyDate =
                preferences.getString("lastStudyDate", "");

        String today = dateKeyFormat.format(new Date());

        // A streak is current only if the last study day
        // was today or yesterday.
        if (!lastStudyDate.isEmpty()
                && !lastStudyDate.equals(today)) {

            try {
                Date lastDate = dateKeyFormat.parse(lastStudyDate);
                Date todayDate = dateKeyFormat.parse(today);

                if (lastDate != null && todayDate != null) {

                    Calendar lastCalendar = Calendar.getInstance();
                    lastCalendar.setTime(lastDate);

                    Calendar todayCalendar = Calendar.getInstance();
                    todayCalendar.setTime(todayDate);

                    long difference = dayNumber(todayCalendar)
                            - dayNumber(lastCalendar);

                    if (difference > 1 || difference < 0) {
                        streak = 0;
                    }
                }

            } catch (Exception e) {
                streak = 0;
            }
        }

        tvStudyStreak.setText(
                streak + (streak == 1 ? " day" : " days")
        );

        tvStudyDays.setText(
                studyDates.size() + " recorded study days"
        );
    }

    // Draw the monthly calendar.
    private void renderCalendar() {

        if (calendarGrid == null) {
            return;
        }

        calendarGrid.removeAllViews();

        tvMonth.setText(
                new SimpleDateFormat(
                        "MMMM yyyy",
                        Locale.getDefault()
                ).format(displayedMonth.getTime())
        );

        // Weekday headings, Monday to Sunday.
        for (String weekday : weekdays) {

            TextView heading = new TextView(this);

            heading.setText(weekday);
            heading.setTextColor(muted);
            heading.setTextSize(12);
            heading.setGravity(Gravity.CENTER);
            heading.setTypeface(null, Typeface.BOLD);

            GridLayout.LayoutParams params =
                    new GridLayout.LayoutParams();

            params.width = 0;
            params.height = dp(36);

            params.columnSpec =
                    GridLayout.spec(GridLayout.UNDEFINED, 1f);

            params.setMargins(dp(2), dp(2), dp(2), dp(2));

            calendarGrid.addView(heading, params);
        }

        Calendar firstDay = (Calendar) displayedMonth.clone();
        firstDay.set(Calendar.DAY_OF_MONTH, 1);

        // Convert Sunday-first Calendar numbering to Monday-first.
        int offset =
                (firstDay.get(Calendar.DAY_OF_WEEK) + 5) % 7;

        int daysInMonth =
                firstDay.getActualMaximum(Calendar.DAY_OF_MONTH);

        Calendar today = Calendar.getInstance();

        // Six rows allow every month to fit.
        for (int i = 0; i < 42; i++) {

            int dayNumber = i - offset + 1;

            TextView dayCell = new TextView(this);

            GridLayout.LayoutParams params =
                    new GridLayout.LayoutParams();

            params.width = 0;
            params.height = dp(42);

            params.columnSpec =
                    GridLayout.spec(GridLayout.UNDEFINED, 1f);

            params.setMargins(dp(2), dp(2), dp(2), dp(2));

            if (dayNumber >= 1 && dayNumber <= daysInMonth) {

                Calendar cellDate =
                        (Calendar) displayedMonth.clone();

                cellDate.set(Calendar.DAY_OF_MONTH, dayNumber);

                String key = dateKeyFormat.format(
                        cellDate.getTime()
                );

                boolean studied = studyDates.contains(key);
                boolean isToday = sameDay(cellDate, today);
                boolean isSelected = sameDay(cellDate, selectedDate);
                boolean isFuture = dayNumberIsFuture(cellDate, today);

                dayCell.setText(String.valueOf(dayNumber));
                dayCell.setGravity(Gravity.CENTER);
                dayCell.setTextSize(14);

                if (studied) {
                    // Green: study activity recorded.
                    dayCell.setTextColor(Color.WHITE);

                    dayCell.setBackground(makeBackground(
                            green,
                            isSelected ? dark : green,
                            10
                    ));

                } else if (isSelected) {
                    // Blue: selected date.
                    dayCell.setTextColor(blue);

                    dayCell.setBackground(makeBackground(
                            lightBlue,
                            blue,
                            10
                    ));

                } else {
                    // White: no study activity recorded.
                    dayCell.setTextColor(
                            isFuture ? muted : dark
                    );

                    dayCell.setBackground(makeBackground(
                            Color.WHITE,
                            isToday ? blue : border,
                            10
                    ));
                }

                if (isFuture) {
                    dayCell.setAlpha(0.45f);
                }

                dayCell.setOnClickListener(v -> {

                    selectedDate = (Calendar) cellDate.clone();

                    tvSelectedDate.setText(
                            readableDateFormat.format(
                                    cellDate.getTime()
                            )
                    );

                    if (isFuture) {
                        Toast.makeText(
                                this,
                                "This date has not arrived yet.",
                                Toast.LENGTH_SHORT
                        ).show();

                    } else if (studied) {
                        Toast.makeText(
                                this,
                                "Study activity was recorded on this day.",
                                Toast.LENGTH_SHORT
                        ).show();

                    } else {
                        Toast.makeText(
                                this,
                                "No study activity was recorded on this day.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    renderCalendar();
                });

            } else {
                dayCell.setText("");
                dayCell.setBackgroundColor(Color.TRANSPARENT);
            }

            calendarGrid.addView(dayCell, params);
        }

        // Disable the next-month button in the current month.
        boolean canGoForward =
                isBeforeCurrentMonth(displayedMonth);

        btnNextMonth.setEnabled(canGoForward);
        btnNextMonth.setAlpha(canGoForward ? 1f : 0.4f);
    }

    // Check whether a date is in the future.
    private boolean dayNumberIsFuture(
            Calendar date,
            Calendar today
    ) {
        return dayNumber(date) > dayNumber(today);
    }

    // Convert a date to a calendar-day number.
    // This avoids errors caused by daylight-saving changes.
    private long dayNumber(Calendar calendar) {

        Calendar copy = (Calendar) calendar.clone();

        copy.set(Calendar.HOUR_OF_DAY, 0);
        copy.set(Calendar.MINUTE, 0);
        copy.set(Calendar.SECOND, 0);
        copy.set(Calendar.MILLISECOND, 0);

        return copy.getTimeInMillis() / (24L * 60L * 60L * 1000L);
    }

    private boolean isBeforeCurrentMonth(Calendar month) {

        Calendar current = Calendar.getInstance();
        current.set(Calendar.DAY_OF_MONTH, 1);

        return month.before(current);
    }

    private boolean sameDay(Calendar first, Calendar second) {

        return first.get(Calendar.YEAR)
                == second.get(Calendar.YEAR)
                && first.get(Calendar.DAY_OF_YEAR)
                == second.get(Calendar.DAY_OF_YEAR);
    }

    // Create rounded calendar date backgrounds.
    private GradientDrawable makeBackground(
            int fillColor,
            int strokeColor,
            int radius
    ) {

        GradientDrawable drawable = new GradientDrawable();

        drawable.setColor(fillColor);
        drawable.setCornerRadius(dp(radius));
        drawable.setStroke(dp(1), strokeColor);

        return drawable;
    }

    private int dp(float value) {

        return (int) (
                value * getResources()
                        .getDisplayMetrics().density + 0.5f
        );
    }
}
