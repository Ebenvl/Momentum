package com.example.momentum.admin;

import com.example.momentum.activities.BaseActivity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;


import com.example.momentum.data.Session;

// The shared layout for the admin screens (header, title, scrolling page),
// which also keeps out anyone who isn't an administrator.
public abstract class AdminScreen extends BaseActivity {

    protected static final int NAVY = Color.rgb(7, 29, 53);
    protected static final int GOLD = Color.rgb(244, 185, 66);
    protected static final int TEAL = Color.rgb(53, 184, 165);
    protected static final int ORANGE = Color.rgb(228, 108, 40);
    protected static final int PURPLE = Color.rgb(118, 102, 232);
    protected static final int MUTED = Color.rgb(104, 119, 131);
    protected static final int PALE = Color.rgb(244, 246, 250);
    protected static final int WHITE = Color.WHITE;

    // Where each screen puts its own content.
    protected LinearLayout content;

    // The screen's title and subtitle in the navy header.
    protected abstract String title();

    protected abstract String subtitle();

    // Called once the screen is built, to load and show its data.
    protected abstract void load();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!Session.isLoggedIn() || !Session.getUser().isAdmin()) {
            Toast.makeText(this, "Administrators only", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

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
        header.addView(text(title(), 24, WHITE, true));
        TextView sub = text(subtitle(), 13, Color.rgb(205, 216, 229), false);
        sub.setPadding(0, dp(7), 0, 0);
        header.addView(sub);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(20), dp(16), dp(24));
        scroll.addView(content);

        setContentView(root);
        load();
    }

    // Building blocks for the screens

    protected TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        return view;
    }

    // A white rounded card, added to the page.
    protected LinearLayout card() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(14), dp(16), dp(14));
        box.setBackground(round(WHITE, 14));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(10);
        content.addView(box, params);
        return box;
    }

    // A full-width button in the frontend's navy or white style, added to the page.
    protected Button button(String label, boolean filled, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(filled ? WHITE : NAVY);
        button.setBackground(round(filled ? NAVY : WHITE, 12));
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(50));
        params.topMargin = dp(12);
        content.addView(button, params);
        return button;
    }

    // A heading between sections of the page.
    protected void heading(String value) {
        TextView label = text(value, 16, NAVY, true);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(20);
        content.addView(label, params);
    }

    // A text box for forms in dialogs.
    protected EditText field(String hint, String value, boolean number) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint(hint);
        if (value != null) {
            input.setText(value);
        }
        if (number) {
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
        }
        return input;
    }

    // A vertical form holding the given fields, for a dialog.
    protected LinearLayout form(View... fields) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(24), dp(8), dp(24), 0);
        for (View field : fields) {
            form.addView(field);
        }
        return form;
    }

    // A grey message in a white card, for "nothing here yet" and errors.
    protected void note(String message) {
        TextView view = text(message, 14, MUTED, false);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(18), dp(24), dp(18), dp(24));
        view.setBackground(round(WHITE, 14));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(10);
        content.addView(view, params);
    }

    protected void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // Reads a whole number from a text box. Null if it's empty or not a number.
    protected Integer numberFrom(EditText input) {
        try {
            return Integer.parseInt(input.getText().toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    protected GradientDrawable round(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    protected int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
