package com.example.momentum.activities;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import com.example.momentum.R;
import com.example.momentum.data.ApiCallback;
import com.example.momentum.data.Session;
import com.example.momentum.model.StudySession;
import com.example.momentum.service.StudyService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StudyTimetableActivity extends BaseActivity {

    private Spinner spinnerDay;
    private EditText editStudyTime;
    private EditText editStudyCourse;
    private EditText editStudyTask;

    private Button btnAddStudySession;
    private Button btnCancelEdit;

    private LinearLayout layoutStudySessions;

    private final StudyService studyService = new StudyService();
    private Integer editingSessionId = null;

    private final List<String> days = Arrays.asList(
            "Monday",
            "Tuesday",
            "Wednesday",
            "Thursday",
            "Friday",
            "Saturday",
            "Sunday"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_timetable);

        spinnerDay = findViewById(R.id.spinnerDay);
        editStudyTime = findViewById(R.id.editStudyTime);
        editStudyCourse = findViewById(R.id.editStudyCourse);
        editStudyTask = findViewById(R.id.editStudyTask);

        btnAddStudySession = findViewById(R.id.btnAddStudySession);
        btnCancelEdit = findViewById(R.id.btnCancelEdit);
        layoutStudySessions = findViewById(R.id.layoutStudySessions);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                days
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerDay.setAdapter(adapter);

        if (!Session.isLoggedIn()) {
            Toast.makeText(
                    this,
                    "Please log in first.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        btnAddStudySession.setOnClickListener(
                v -> saveStudySession()
        );

        btnCancelEdit.setOnClickListener(v -> clearForm());

        loadStudySessions();
    }

    // Add a new session or update the session currently being edited.
    private void saveStudySession() {

        String day = spinnerDay.getSelectedItem().toString();
        String time = editStudyTime.getText().toString().trim();
        String course = editStudyCourse.getText().toString().trim();
        String task = editStudyTask.getText().toString().trim();

        if (time.isEmpty() || course.isEmpty() || task.isEmpty()) {
            Toast.makeText(
                    this,
                    "Please fill in the time, course and study task.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        btnAddStudySession.setEnabled(false);

        boolean editing = editingSessionId != null;

        studyService.saveSession(editingSessionId, day, time, course, task,
                new ApiCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Toast.makeText(
                                StudyTimetableActivity.this,
                                editing
                                        ? "Study session updated!"
                                        : "Study session added successfully!",
                                Toast.LENGTH_SHORT
                        ).show();

                        clearForm();
                        loadStudySessions();
                    }

                    @Override
                    public void onError(String error) {
                        btnAddStudySession.setEnabled(true);

                        Toast.makeText(
                                StudyTimetableActivity.this,
                                (editing
                                        ? "Could not update session: "
                                        : "Could not save session: ")
                                        + error,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    // Load the signed-in user's sessions and organise them by weekday and time.
    private void loadStudySessions() {

        layoutStudySessions.removeAllViews();

        TextView loading = createText(
                "Loading your study plan...",
                15,
                Color.rgb(104, 119, 131)
        );

        layoutStudySessions.addView(loading);

        studyService.getSessions(new ApiCallback<List<StudySession>>() {
            @Override
            public void onSuccess(List<StudySession> result) {

                    layoutStudySessions.removeAllViews();

                    List<StudySession> sessions =
                            new ArrayList<>(result);

                    Collections.sort(
                            sessions,
                            new Comparator<StudySession>() {
                                @Override
                                public int compare(
                                        StudySession first,
                                        StudySession second
                                ) {
                                    String firstDay =
                                            first.day;

                                    String secondDay =
                                            second.day;

                                    int firstDayIndex =
                                            days.indexOf(firstDay);

                                    int secondDayIndex =
                                            days.indexOf(secondDay);

                                    if (firstDayIndex < 0) {
                                        firstDayIndex = 7;
                                    }

                                    if (secondDayIndex < 0) {
                                        secondDayIndex = 7;
                                    }

                                    if (firstDayIndex != secondDayIndex) {
                                        return Integer.compare(
                                                firstDayIndex,
                                                secondDayIndex
                                        );
                                    }

                                    String firstTime =
                                            first.time;

                                    String secondTime =
                                            second.time;

                                    return Integer.compare(
                                            getStartTime(firstTime),
                                            getStartTime(secondTime)
                                    );
                                }
                            }
                    );

                    if (sessions.isEmpty()) {
                        TextView emptyMessage = createText(
                                "No study sessions yet.\n"
                                        + "Add your first session above.",
                                16,
                                Color.rgb(104, 119, 131)
                        );

                        emptyMessage.setPadding(16, 20, 16, 20);
                        emptyMessage.setBackground(
                                createBackground(
                                        Color.WHITE,
                                        16
                                )
                        );

                        layoutStudySessions.addView(emptyMessage);
                        return;
                    }

                    String currentDay = "";

                    for (StudySession document : sessions) {

                        String day = document.day;

                        if (day == null) {
                            day = "Other";
                        }

                        if (!day.equals(currentDay)) {
                            currentDay = day;

                            TextView dayHeading = createText(
                                    currentDay,
                                    19,
                                    Color.rgb(118, 102, 232)
                            );

                            dayHeading.setTypeface(
                                    null,
                                    Typeface.BOLD
                            );

                            dayHeading.setPadding(4, 16, 4, 8);

                            layoutStudySessions.addView(dayHeading);
                        }

                        addSessionCard(document);
                    }
            }

            @Override
            public void onError(String error) {
                    layoutStudySessions.removeAllViews();

                    TextView errorMessage = createText(
                            "Could not load your study plan. "
                                    + "Please check your connection and try again.",
                            15,
                            Color.rgb(228, 108, 40)
                    );

                    layoutStudySessions.addView(errorMessage);
            }
        });
    }

    // Create a card for an individual study session.
    private void addSessionCard(StudySession document) {

        String day = document.day;
        String time = document.time;
        String course = document.course;
        String task = document.task;

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(16, 16, 16, 16);
        card.setBackground(createBackground(Color.WHITE, 16));

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(0, 0, 0, 12);
        card.setLayoutParams(cardParams);

        TextView timeText = createText(
                time == null ? "Time not set" : time,
                17,
                Color.rgb(118, 102, 232)
        );

        timeText.setTypeface(null, Typeface.BOLD);

        TextView courseText = createText(
                course == null ? "Course not set" : course,
                16,
                Color.rgb(7, 29, 53)
        );

        courseText.setTypeface(null, Typeface.BOLD);
        courseText.setPadding(0, 8, 0, 4);

        TextView taskText = createText(
                task == null ? "No task description" : task,
                14,
                Color.rgb(104, 119, 131)
        );

        taskText.setPadding(0, 4, 0, 12);

        card.addView(timeText);
        card.addView(courseText);
        card.addView(taskText);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);

        Button editButton = new Button(this);
        editButton.setText("Edit");
        editButton.setTextColor(Color.WHITE);
        editButton.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        Color.rgb(118, 102, 232)
                )
        );

        Button deleteButton = new Button(this);
        deleteButton.setText("Delete");
        deleteButton.setTextColor(Color.WHITE);
        deleteButton.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        Color.rgb(228, 108, 40)
                )
        );

        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );

        buttonParams.setMargins(4, 0, 4, 0);

        buttons.addView(editButton, buttonParams);
        buttons.addView(deleteButton, buttonParams);

        card.addView(buttons);
        layoutStudySessions.addView(card);

        editButton.setOnClickListener(v ->
                startEditing(document)
        );

        deleteButton.setOnClickListener(v ->
                confirmDelete(document)
        );
    }

    // Put the selected session into the form for editing.
    private void startEditing(StudySession document) {

        editingSessionId = document.id;

        String day = document.day;
        String time = document.time;
        String course = document.course;
        String task = document.task;

        int dayPosition = days.indexOf(day);

        if (dayPosition >= 0) {
            spinnerDay.setSelection(dayPosition);
        }

        editStudyTime.setText(time == null ? "" : time);
        editStudyCourse.setText(course == null ? "" : course);
        editStudyTask.setText(task == null ? "" : task);

        btnAddStudySession.setText("Save Changes");
        btnCancelEdit.setVisibility(View.VISIBLE);

        // Move the form back into view.
        spinnerDay.requestFocus();

        Toast.makeText(
                this,
                "Edit the details and tap Save Changes.",
                Toast.LENGTH_LONG
        ).show();
    }

    // Ask for confirmation before deleting a session.
    private void confirmDelete(StudySession document) {

        String course = document.course;

        new AlertDialog.Builder(this)
                .setTitle("Delete Study Session")
                .setMessage(
                        "Are you sure you want to delete the session for "
                                + (course == null ? "this course" : course)
                                + "?"
                )
                .setNegativeButton("Cancel", (dialog, which) ->
                        dialog.dismiss())
                .setPositiveButton("Delete", (dialog, which) -> {

                    int sessionId = document.id;

                    studyService.deleteSession(sessionId, new ApiCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {

                            if (editingSessionId != null
                                    && editingSessionId == sessionId) {
                                clearForm();
                            }

                            Toast.makeText(
                                    StudyTimetableActivity.this,
                                    "Study session deleted.",
                                    Toast.LENGTH_SHORT
                            ).show();

                            loadStudySessions();
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(
                                    StudyTimetableActivity.this,
                                    "Could not delete session: "
                                            + error,
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    });
                })
                .show();
    }

    // Reset the form after saving or cancelling an edit.
    private void clearForm() {

        editingSessionId = null;

        editStudyTime.setText("");
        editStudyCourse.setText("");
        editStudyTask.setText("");

        spinnerDay.setSelection(0);

        btnAddStudySession.setText("Add Study Session");
        btnAddStudySession.setEnabled(true);

        btnCancelEdit.setVisibility(View.GONE);
    }

    // Convert the beginning of a 24-hour time string into minutes for sorting.
    // Example: "14:00 - 15:00" becomes 840.
    private int getStartTime(String time) {

        if (time == null) {
            return Integer.MAX_VALUE;
        }

        Pattern pattern = Pattern.compile("(\\d{1,2}):(\\d{2})");
        Matcher matcher = pattern.matcher(time);

        if (matcher.find()) {
            try {
                int hour = Integer.parseInt(matcher.group(1));
                int minute = Integer.parseInt(matcher.group(2));

                if (hour >= 0 && hour <= 23
                        && minute >= 0 && minute <= 59) {
                    return hour * 60 + minute;
                }
            } catch (NumberFormatException ignored) {
                // Unrecognised time format: put this session last.
            }
        }

        return Integer.MAX_VALUE;
    }

    private TextView createText(
            String text,
            int size,
            int color
    ) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(size);
        textView.setTextColor(color);

        return textView;
    }

    private GradientDrawable createBackground(
            int color,
            int cornerRadius
    ) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(
                cornerRadius * getResources().getDisplayMetrics().density
        );
        return background;
    }
}