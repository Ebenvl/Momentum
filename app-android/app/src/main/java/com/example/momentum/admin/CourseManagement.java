package com.example.momentum.admin;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CourseManagement extends AppCompatActivity {

    private final int NAVY = Color.rgb(7, 29, 53);
    private final int GOLD = Color.rgb(244, 185, 66);
    private final int TEAL = Color.rgb(53, 184, 165);
    private final int WHITE = Color.WHITE;
    private final int MUTED = Color.rgb(104, 119, 131);
    private final int PALE = Color.rgb(244, 246, 250);

    private LinearLayout content;
    private LinearLayout courseList;
    private EditText searchInput;
    private EditText titleInput;
    private EditText descriptionInput;
    private EditText categoryInput;
    private TextView emptyMessage;

    private String selectedFilter = "All";
    private boolean showingForm = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildScreen();
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(PALE);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(20), dp(24), dp(20), dp(22));
        header.setBackgroundColor(NAVY);
        root.addView(header);

        TextView back = text("‹  Back to Dashboard", 14, GOLD, true);
        back.setPadding(0, 0, 0, dp(18));
        back.setOnClickListener(v -> finish());
        header.addView(back);

        header.addView(text("Course Management", 24, WHITE, true));

        TextView subtitle = text(
                "Manage courses on your learning platform",
                13, Color.rgb(205, 216, 229), false);

        LinearLayout.LayoutParams subParams =
                new LinearLayout.LayoutParams(-1, -2);
        subParams.topMargin = dp(7);
        header.addView(subtitle, subParams);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(20), dp(16), dp(24));
        scroll.addView(content);

        setContentView(root);
        showCourseList();
    }

    private void showCourseList() {
        showingForm = false;
        content.removeAllViews();

        TextView heading = text("Courses", 20, NAVY, true);
        content.addView(heading);

        TextView description = text(
                "Create and manage learning courses.",
                13, MUTED, false);
        addTopMargin(description, 6);

        Button createButton = button("+  Create Course", NAVY, WHITE);
        addTopMargin(createButton, 18);
        createButton.setOnClickListener(v -> showCreateForm());

        TextView overviewHeading =
                text("Course overview", 15, NAVY, true);
        addTopMargin(overviewHeading, 24);

        TextView overview = text("Total courses: —", 14, MUTED, false);
        overview.setPadding(dp(16), dp(16), dp(16), dp(16));
        overview.setBackground(round(WHITE, 14));
        addTopMargin(overview, 10);

        TextView searchLabel = text("Search courses", 14, NAVY, true);
        addTopMargin(searchLabel, 22);

        searchInput = new EditText(this);
        searchInput.setSingleLine(true);
        searchInput.setTextSize(14);
        searchInput.setHint("Search by course title...");
        searchInput.setPadding(dp(14), dp(12), dp(14), dp(12));
        searchInput.setBackground(round(WHITE, 12));
        addTopMargin(searchInput, 8);

        TextView filterLabel = text("Filter courses", 14, NAVY, true);
        addTopMargin(filterLabel, 18);

        LinearLayout filters = new LinearLayout(this);
        filters.setOrientation(LinearLayout.HORIZONTAL);
        addTopMargin(filters, 8);

        addFilter(filters, "All");
        addFilter(filters, "Published");
        addFilter(filters, "Draft");

        TextView listHeading = text("Course list", 16, NAVY, true);
        addTopMargin(listHeading, 24);

        courseList = new LinearLayout(this);
        courseList.setOrientation(LinearLayout.VERTICAL);
        addTopMargin(courseList, 10);

        emptyMessage = text(
                "No courses available yet.\n\n"
                        + "Courses will appear here once they are "
                        + "created and connected to the backend.",
                14, MUTED, false);
        emptyMessage.setGravity(Gravity.CENTER);
        emptyMessage.setPadding(dp(18), dp(28), dp(18), dp(28));
        emptyMessage.setBackground(round(WHITE, 14));
        courseList.addView(emptyMessage);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s, int start, int before, int count) {
                updateEmptyMessage();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void addFilter(LinearLayout parent, String filter) {
        TextView chip = text(
                filter,
                12,
                selectedFilter.equals(filter) ? WHITE : NAVY,
                true);

        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(12), dp(10), dp(12), dp(10));
        chip.setBackground(round(
                selectedFilter.equals(filter) ? NAVY : WHITE, 20));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(0, -2, 1);
        params.setMargins(dp(3), 0, dp(3), 0);
        parent.addView(chip, params);

        chip.setOnClickListener(v -> {
            selectedFilter = filter;
            showCourseList();
        });
    }

    private void updateEmptyMessage() {
        if (emptyMessage == null || searchInput == null) {
            return;
        }

        String query = searchInput.getText().toString().trim();

        if (!query.isEmpty()) {
            emptyMessage.setText(
                    "No matching courses.\n\n"
                            + "There are currently no course records "
                            + "to search.");
        } else if (!selectedFilter.equals("All")) {
            emptyMessage.setText(
                    "No " + selectedFilter.toLowerCase()
                            + " courses available.\n\n"
                            + "Course records will appear here when "
                            + "real data is connected.");
        } else {
            emptyMessage.setText(
                    "No courses available yet.\n\n"
                            + "Courses will appear here once they are "
                            + "created and connected to the backend.");
        }
    }

    private void showCreateForm() {
        showingForm = true;
        content.removeAllViews();

        TextView back = text("‹  Back to Courses", 14, NAVY, true);
        content.addView(back);
        back.setOnClickListener(v -> showCourseList());

        TextView heading = text("Create Course", 22, NAVY, true);
        addTopMargin(heading, 22);

        TextView subtitle = text(
                "Enter the details for the new course.",
                13, MUTED, false);
        addTopMargin(subtitle, 6);

        titleInput = addField(
                "Course title", "Enter course title");

        descriptionInput = addField(
                "Description", "Describe the course");
        descriptionInput.setMinLines(3);
        descriptionInput.setGravity(Gravity.TOP | Gravity.START);
        descriptionInput.setSingleLine(false);

        categoryInput = addField(
                "Category", "Enter course category");

        TextView note = text(
                "This is a front-end preview. Course details "
                        + "will not be saved until the backend is connected.",
                12, MUTED, false);
        note.setPadding(dp(14), dp(14), dp(14), dp(14));
        note.setBackground(round(WHITE, 12));
        addTopMargin(note, 18);

        Button saveButton = button("Validate Course", NAVY, WHITE);
        addTopMargin(saveButton, 18);

        Button cancelButton = button("Cancel", WHITE, NAVY);
        addTopMargin(cancelButton, 8);
        cancelButton.setOnClickListener(v -> showCourseList());

        saveButton.setOnClickListener(v -> {
            String title = titleInput.getText().toString().trim();
            String description = descriptionInput.getText()
                    .toString().trim();
            String category = categoryInput.getText()
                    .toString().trim();

            if (title.isEmpty()) {
                titleInput.setError("Course title is required");
                titleInput.requestFocus();
                return;
            }

            if (description.isEmpty()) {
                descriptionInput.setError(
                        "Course description is required");
                descriptionInput.requestFocus();
                return;
            }

            if (category.isEmpty()) {
                categoryInput.setError("Category is required");
                categoryInput.requestFocus();
                return;
            }

            Toast.makeText(
                    this,
                    "Details validated. Nothing was saved because "
                            + "the backend is not connected.",
                    Toast.LENGTH_LONG
            ).show();
        });
    }

    private EditText addField(String label, String hint) {
        TextView labelView = text(label, 14, NAVY, true);
        addTopMargin(labelView, 18);

        EditText field = new EditText(this);
        field.setTextSize(14);
        field.setHint(hint);
        field.setPadding(dp(14), dp(12), dp(14), dp(12));
        field.setBackground(round(WHITE, 12));
        field.setSingleLine(true);

        addTopMargin(field, 8);
        return field;
    }

    private Button button(
            String label, int background, int foreground) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(foreground);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setBackground(round(background, 12));
        return button;
    }

    private void addTopMargin(View view, int margin) {
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(margin);
        content.addView(view, params);
    }

    private TextView text(
            String value, int size, int color, boolean bold) {
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