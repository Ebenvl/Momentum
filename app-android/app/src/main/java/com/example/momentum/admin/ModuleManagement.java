package com.example.momentum.admin;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ModuleManagement extends AppCompatActivity {

    private final int NAVY = Color.rgb(7, 29, 53);
    private final int GOLD = Color.rgb(244, 185, 66);
    private final int WHITE = Color.WHITE;
    private final int MUTED = Color.rgb(104, 119, 131);
    private final int PALE = Color.rgb(244, 246, 250);

    private LinearLayout content;

    private EditText courseInput;
    private EditText moduleInput;
    private EditText descriptionInput;
    private EditText lessonInput;
    private EditText lessonContentInput;

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

        TextView back = text(
                "‹  Back to Dashboard", 14, GOLD, true);
        back.setPadding(0, 0, 0, dp(18));
        back.setOnClickListener(v -> finish());
        header.addView(back);

        header.addView(text(
                "Module Management", 24, WHITE, true));

        TextView subtitle = text(
                "Prepare course modules and lessons",
                13, Color.rgb(205, 216, 229), false);

        LinearLayout.LayoutParams subtitleParams =
                new LinearLayout.LayoutParams(-1, -2);
        subtitleParams.topMargin = dp(7);
        header.addView(subtitle, subtitleParams);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        root.addView(scroll,
                new LinearLayout.LayoutParams(-1, 0, 1));

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(20), dp(16), dp(24));
        scroll.addView(content);

        setContentView(root);
        showModuleList();
    }

    private void showModuleList() {
        content.removeAllViews();

        content.addView(text(
                "Modules and Lessons", 20, NAVY, true));

        TextView description = text(
                "Prepare learning content for your courses.",
                13, MUTED, false);
        addTopMargin(description, 6);

        Button createButton = button(
                "+  Create Module", NAVY, WHITE);
        addTopMargin(createButton, 18);
        createButton.setOnClickListener(v -> showCreateForm());

        TextView overview = text(
                "Total modules: —", 14, MUTED, false);
        overview.setPadding(dp(16), dp(16), dp(16), dp(16));
        overview.setBackground(round(WHITE, 14));
        addTopMargin(overview, 24);

        TextView listHeading = text(
                "Module list", 16, NAVY, true);
        addTopMargin(listHeading, 24);

        TextView emptyMessage = text(
                "No modules available yet.\n\n"
                        + "Modules and lessons will appear here "
                        + "when real course data is connected.",
                14, MUTED, false);

        emptyMessage.setGravity(Gravity.CENTER);
        emptyMessage.setPadding(
                dp(18), dp(28), dp(18), dp(28));
        emptyMessage.setBackground(round(WHITE, 14));

        addTopMargin(emptyMessage, 10);
    }

    private void showCreateForm() {
        content.removeAllViews();

        TextView back = text(
                "‹  Back to Modules", 14, NAVY, true);
        content.addView(back);
        back.setOnClickListener(v -> showModuleList());

        TextView heading = text(
                "Create Module", 22, NAVY, true);
        addTopMargin(heading, 22);

        TextView subtitle = text(
                "Enter the module and lesson details.",
                13, MUTED, false);
        addTopMargin(subtitle, 6);

        courseInput = addField(
                "Course title",
                "Enter the course title");

        moduleInput = addField(
                "Module title",
                "Enter module title");

        descriptionInput = addField(
                "Module description",
                "Describe the module");
        descriptionInput.setSingleLine(false);
        descriptionInput.setMinLines(3);
        descriptionInput.setGravity(
                Gravity.TOP | Gravity.START);

        lessonInput = addField(
                "Lesson title",
                "Enter lesson title");

        lessonContentInput = addField(
                "Lesson content",
                "Enter lesson content");
        lessonContentInput.setSingleLine(false);
        lessonContentInput.setMinLines(4);
        lessonContentInput.setGravity(
                Gravity.TOP | Gravity.START);

        TextView note = text(
                "Front-end preview only. Module and lesson "
                        + "details will not be saved until the "
                        + "backend is connected.",
                12, MUTED, false);
        note.setPadding(dp(14), dp(14), dp(14), dp(14));
        note.setBackground(round(WHITE, 12));
        addTopMargin(note, 18);

        Button validateButton = button(
                "Validate Module", NAVY, WHITE);
        addTopMargin(validateButton, 18);
        validateButton.setOnClickListener(
                v -> validateModule());

        Button cancelButton = button(
                "Cancel", WHITE, NAVY);
        addTopMargin(cancelButton, 8);
        cancelButton.setOnClickListener(
                v -> showModuleList());
    }

    private void validateModule() {
        if (courseInput.getText().toString().trim().isEmpty()) {
            courseInput.setError("Course title is required");
            courseInput.requestFocus();
            return;
        }

        if (moduleInput.getText().toString().trim().isEmpty()) {
            moduleInput.setError("Module title is required");
            moduleInput.requestFocus();
            return;
        }

        if (descriptionInput.getText().toString()
                .trim().isEmpty()) {
            descriptionInput.setError(
                    "Module description is required");
            descriptionInput.requestFocus();
            return;
        }

        if (lessonInput.getText().toString().trim().isEmpty()) {
            lessonInput.setError("Lesson title is required");
            lessonInput.requestFocus();
            return;
        }

        if (lessonContentInput.getText().toString()
                .trim().isEmpty()) {
            lessonContentInput.setError(
                    "Lesson content is required");
            lessonContentInput.requestFocus();
            return;
        }

        Toast.makeText(
                this,
                "Details validated. Nothing was saved because "
                        + "the backend is not connected.",
                Toast.LENGTH_LONG
        ).show();
    }

    private EditText addField(String label, String hint) {
        TextView labelView = text(label, 14, NAVY, true);
        addTopMargin(labelView, 18);

        EditText field = new EditText(this);
        field.setTextSize(14);
        field.setHint(hint);
        field.setPadding(
                dp(14), dp(12), dp(14), dp(12));
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
                value * getResources()
                        .getDisplayMetrics().density + 0.5f);
    }
}