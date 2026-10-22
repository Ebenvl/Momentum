package com.example.momentum.admin;

import com.example.momentum.activities.BaseActivity;
import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import com.example.momentum.data.ApiCallback;
import com.example.momentum.model.User;
import com.example.momentum.service.AdminService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UserTab extends BaseActivity {

    private final int NAVY = Color.rgb(7, 29, 53);
    private final int ORANGE = Color.rgb(228, 108, 40);
    private final int GOLD = Color.rgb(244, 185, 66);
    private final int PURPLE = Color.rgb(118, 102, 232);
    private final int TEAL = Color.rgb(53, 184, 165);
    private final int WHITE = Color.WHITE;
    private final int MUTED = Color.rgb(104, 119, 131);
    private final int PALE = Color.rgb(244, 246, 250);

    private LinearLayout userList;
    private EditText searchInput;
    private TextView allFilter;
    private TextView activeFilter;
    private TextView inactiveFilter;
    private TextView emptyMessage;
    private TextView resultCount;

    private String selectedFilter = "All";

    // Backend connection
    private final AdminService adminService = new AdminService();
    private final ArrayList<User> users = new ArrayList<>();
    private TextView totalStat;
    private TextView activeStat;
    private TextView inactiveStat;
    private TextView dataNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showUsersScreen();
    }

    private void showUsersScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(PALE);
        setContentView(root);

        // Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(20), dp(24), dp(20), dp(22));
        header.setBackground(round(NAVY, 0));
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));

        TextView backButton = text(
                "‹   Back to Dashboard", 14, WHITE, true);
        backButton.setPadding(0, dp(4), 0, dp(16));
        backButton.setOnClickListener(v -> finish());
        header.addView(backButton);

        header.addView(text(
                "LEARNTECH  /  ADMINISTRATION", 11, GOLD, true));

        TextView title = text(
                "User Management", 27, WHITE, true);

        LinearLayout.LayoutParams titleParams =
                new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(8);
        header.addView(title, titleParams);

        TextView subtitle = text(
                "View and manage platform accounts",
                14, Color.rgb(205, 216, 229), false);

        LinearLayout.LayoutParams subtitleParams =
                new LinearLayout.LayoutParams(-1, -2);
        subtitleParams.topMargin = dp(6);
        header.addView(subtitle, subtitleParams);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(20), dp(18), dp(28));
        scroll.addView(content);

        // Overview
        addSectionTitle(content, "USERS OVERVIEW");

        LinearLayout statsRow = new LinearLayout(this);
        statsRow.setOrientation(LinearLayout.HORIZONTAL);
        content.addView(statsRow);

        totalStat = addStat(statsRow, "—", "Total users", PURPLE);
        activeStat = addStat(statsRow, "—", "Active users", TEAL);
        inactiveStat = addStat(statsRow, "—", "Inactive users", ORANGE);

        dataNote = text(
                "Statistics will appear when real user data is connected.",
                12, MUTED, false);

        LinearLayout.LayoutParams noteParams =
                new LinearLayout.LayoutParams(-1, -2);
        noteParams.topMargin = dp(10);
        content.addView(dataNote, noteParams);

        // User directory
        addSectionTitle(content, "User Directory");

        LinearLayout directoryCard = new LinearLayout(this);
        directoryCard.setOrientation(LinearLayout.VERTICAL);
        directoryCard.setPadding(dp(14), dp(16), dp(14), dp(16));
        directoryCard.setBackground(round(WHITE, 16));

        LinearLayout.LayoutParams directoryParams =
                new LinearLayout.LayoutParams(-1, -2);
        directoryParams.topMargin = dp(2);
        content.addView(directoryCard, directoryParams);

        directoryCard.addView(text(
                "Search users", 14, NAVY, true));

        searchInput = new EditText(this);
        searchInput.setSingleLine(true);
        searchInput.setTextSize(14);
        searchInput.setTextColor(NAVY);
        searchInput.setHintTextColor(MUTED);
        searchInput.setHint("Search by name or email");
        searchInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        searchInput.setBackground(round(PALE, 10));

        LinearLayout.LayoutParams searchParams =
                new LinearLayout.LayoutParams(-1, dp(48));
        searchParams.topMargin = dp(10);
        directoryCard.addView(searchInput, searchParams);

        TextView filterLabel = text(
                "Account status", 13, NAVY, true);

        LinearLayout.LayoutParams filterLabelParams =
                new LinearLayout.LayoutParams(-1, -2);
        filterLabelParams.topMargin = dp(18);
        directoryCard.addView(filterLabel, filterLabelParams);

        LinearLayout filters = new LinearLayout(this);
        filters.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout.LayoutParams filtersParams =
                new LinearLayout.LayoutParams(-1, -2);
        filtersParams.topMargin = dp(10);
        directoryCard.addView(filters, filtersParams);

        allFilter = createFilter("All");
        activeFilter = createFilter("Active");
        inactiveFilter = createFilter("Inactive");

        addFilter(filters, allFilter);
        addFilter(filters, activeFilter);
        addFilter(filters, inactiveFilter);

        updateFilterAppearance();

        allFilter.setOnClickListener(v -> selectFilter("All"));
        activeFilter.setOnClickListener(v -> selectFilter("Active"));
        inactiveFilter.setOnClickListener(v -> selectFilter("Inactive"));

        // Create User button
        Button createButton = new Button(this);
        createButton.setText("+  Create User");
        createButton.setTextColor(WHITE);
        createButton.setTextSize(14);
        createButton.setAllCaps(false);
        createButton.setBackground(round(NAVY, 10));

        LinearLayout.LayoutParams createParams =
                new LinearLayout.LayoutParams(-1, dp(48));
        createParams.topMargin = dp(18);
        directoryCard.addView(createButton, createParams);

        createButton.setOnClickListener(v -> showCreateUserDialog());

        // List heading
        LinearLayout listHeader = new LinearLayout(this);
        listHeader.setOrientation(LinearLayout.HORIZONTAL);
        listHeader.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout.LayoutParams listHeaderParams =
                new LinearLayout.LayoutParams(-1, -2);
        listHeaderParams.topMargin = dp(24);
        content.addView(listHeader, listHeaderParams);

        TextView listTitle = text(
                "Platform users", 17, NAVY, true);
        listHeader.addView(listTitle,
                new LinearLayout.LayoutParams(0, -2, 1));

        resultCount = text("0 users", 12, MUTED, false);
        listHeader.addView(resultCount);

        // User list
        userList = new LinearLayout(this);
        userList.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams userListParams =
                new LinearLayout.LayoutParams(-1, -2);
        userListParams.topMargin = dp(12);
        content.addView(userList, userListParams);

        emptyMessage = text(
                "No user data available yet.\n\n"
                        + "When the backend is connected, real user "
                        + "accounts will appear here.",
                14, MUTED, false);

        emptyMessage.setGravity(Gravity.CENTER);
        emptyMessage.setPadding(
                dp(18), dp(30), dp(18), dp(30));
        emptyMessage.setBackground(round(WHITE, 16));

        userList.addView(emptyMessage,
                new LinearLayout.LayoutParams(-1, -2));

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s, int start, int before, int count) {
                updateEmptyState();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        updateEmptyState();

        TextView footer = text(
                "LEARNTECH ADMIN • DEVELOPMENT PREVIEW",
                10, MUTED, true);
        footer.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams footerParams =
                new LinearLayout.LayoutParams(-1, -2);
        footerParams.topMargin = dp(24);
        content.addView(footer, footerParams);

        loadUsers();
    }

    // Loads every account from the backend
    private void loadUsers() {
        adminService.getAllUsers(new ApiCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> result) {
                users.clear();
                users.addAll(result);

                int active = 0;
                for (User user : users) {
                    if ("active".equals(user.status)) active++;
                }
                totalStat.setText(String.valueOf(users.size()));
                activeStat.setText(String.valueOf(active));
                inactiveStat.setText(String.valueOf(users.size() - active));
                dataNote.setVisibility(View.GONE);

                updateEmptyState();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(UserTab.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    // One user in the list. Tapping it asks to delete the account.
    private LinearLayout userRow(User user) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(14), dp(14), dp(14));
        row.setBackground(round(WHITE, 14));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);

        // Name with a Learner or Admin badge next to it
        LinearLayout nameRow = new LinearLayout(this);
        nameRow.setOrientation(LinearLayout.HORIZONTAL);
        nameRow.setGravity(Gravity.CENTER_VERTICAL);
        nameRow.addView(text(user.fullName, 15, NAVY, true));

        TextView role = text(user.isAdmin() ? "Admin" : "Learner", 10,
                user.isAdmin() ? PURPLE : TEAL, true);
        role.setPadding(dp(8), dp(2), dp(8), dp(2));
        role.setBackground(round(user.isAdmin()
                ? Color.rgb(238, 231, 255) : Color.rgb(219, 255, 242), 10));
        LinearLayout.LayoutParams roleParams = new LinearLayout.LayoutParams(-2, -2);
        roleParams.leftMargin = dp(6);
        nameRow.addView(role, roleParams);
        info.addView(nameRow);

        info.addView(text(user.email, 12, MUTED, false));

        // How many courses, and when they last logged in
        int courses = user.courseCount == null ? 0 : user.courseCount;
        TextView meta = text(courses + (courses == 1 ? " Course" : " Courses")
                + "  •  " + lastActive(user.lastLoginAt), 11, MUTED, false);
        meta.setPadding(0, dp(4), 0, 0);
        info.addView(meta);

        row.addView(info, new LinearLayout.LayoutParams(0, -2, 1));

        boolean active = "active".equals(user.status);
        TextView status = text(active ? "Active" : "Inactive", 11, WHITE, true);
        status.setPadding(dp(10), dp(4), dp(10), dp(4));
        status.setBackground(round(active ? TEAL : ORANGE, 10));
        row.addView(status);

        row.setOnClickListener(v -> showUserOptions(user));

        return row;
    }


    // Everything an admin can do to an account, from tapping it in the list.
    private void showUserOptions(User user) {
        boolean active = "active".equals(user.status);
        String[] options = {
                "Edit name and role",
                active ? "Suspend account" : "Reactivate account",
                user.isLinked() ? "Change LearnTech link (now " + user.idst + ")" : "Link to a LearnTech learner",
                "Delete account"};

        new AlertDialog.Builder(this)
                .setTitle(user.fullName)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showEditUserDialog(user);
                    } else if (which == 1) {
                        if (active) {
                            adminService.suspendUser(user.id, reloadAfter(user.fullName + " was suspended"));
                        } else {
                            adminService.reactivateUser(user.id, reloadAfter(user.fullName + " was reactivated"));
                        }
                    } else if (which == 2) {
                        showLinkDialog(user);
                    } else {
                        confirmDeleteUser(user.fullName, () ->
                                adminService.deleteUser(user.id, reloadAfter(user.fullName + " was deleted")));
                    }
                })
                .show();
    }

    // Change someone's name, and whether they're a learner or an admin.
    private void showEditUserDialog(User user) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(4), dp(8), dp(4), dp(4));

        EditText nameInput = formInput("Full name", InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        nameInput.setText(user.fullName);
        form.addView(nameInput);

        Spinner roleSpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, new String[]{"Learner", "Admin"});
        roleSpinner.setAdapter(adapter);
        roleSpinner.setSelection(user.isAdmin() ? 1 : 0);
        form.addView(roleSpinner);

        new AlertDialog.Builder(this)
                .setTitle("Edit " + user.fullName)
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = nameInput.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Enter a name", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String role = roleSpinner.getSelectedItemPosition() == 1 ? "administrator" : "learner";
                    adminService.updateUser(user.id, name, role, reloadAfter(name + " was updated"));
                })
                .show();
    }

    // Link an account to a LearnTech learner by their id (idst), for when the
    // emails don't match. An empty box unlinks it.
    private void showLinkDialog(User user) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(4), dp(8), dp(4), dp(4));

        EditText idstInput = formInput("LearnTech learner id (empty to unlink)", InputType.TYPE_CLASS_NUMBER);
        if (user.idst != null) {
            idstInput.setText(String.valueOf(user.idst));
        }
        form.addView(idstInput);

        new AlertDialog.Builder(this)
                .setTitle("LearnTech link")
                .setMessage("Their courses and points arrive on the next sync.")
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    String typed = idstInput.getText().toString().trim();
                    Integer idst;
                    try {
                        idst = typed.isEmpty() ? null : Integer.parseInt(typed);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "The id must be a number", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    adminService.linkUser(user.id, idst, reloadAfter(idst == null
                            ? user.fullName + " was unlinked" : user.fullName + " was linked"));
                })
                .show();
    }

    // Shows a message and reloads the list when a change worked.
    private ApiCallback<Void> reloadAfter(String message) {
        return new ApiCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(UserTab.this, message, Toast.LENGTH_SHORT).show();
                loadUsers();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(UserTab.this, error, Toast.LENGTH_LONG).show();
            }
        };
    }

    // "Active today", "Active yesterday", "Active 6 days ago" or "Never logged in"
    private String lastActive(String lastLoginAt) {
        if (lastLoginAt == null || lastLoginAt.length() < 10) {
            return "Never logged in";
        }
        try {
            java.util.Date then = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    .parse(lastLoginAt.substring(0, 10));
            java.util.Date today = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    .parse(new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
                            .format(new java.util.Date()));
            long days = (today.getTime() - then.getTime()) / (24L * 60 * 60 * 1000);
            if (days <= 0) return "Active today";
            if (days == 1) return "Active yesterday";
            return "Active " + days + " days ago";
        } catch (Exception e) {
            return "";
        }
    }

    // Create User form
    private void showCreateUserDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(4), dp(8), dp(4), dp(4));

        EditText nameInput = formInput(
                "Full name", InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        EditText emailInput = formInput(
                "Email address", InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        EditText passwordInput = formInput(
                "Password", InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        EditText confirmInput = formInput(
                "Confirm password", InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        form.addView(nameInput);
        form.addView(emailInput);
        form.addView(passwordInput);
        form.addView(confirmInput);

        Spinner roleSpinner = new Spinner(this);
        String[] roles = {"Learner", "Admin"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, roles);
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(adapter);
        form.addView(roleSpinner);

        TextView note = text(
                "They can log in with this email and password straight away.",
                12, MUTED, false);

        LinearLayout.LayoutParams noteParams =
                new LinearLayout.LayoutParams(-1, -2);
        noteParams.topMargin = dp(12);
        form.addView(note, noteParams);

        ScrollView formScroll = new ScrollView(this);
        formScroll.addView(form);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Create User")
                .setView(formScroll)
                .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                .setPositiveButton("Create", null)
                .create();

        dialog.setOnShowListener(ignored ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(v -> {
                            String name = nameInput.getText()
                                    .toString().trim();
                            String email = emailInput.getText()
                                    .toString().trim();
                            String password = passwordInput.getText()
                                    .toString();
                            String confirm = confirmInput.getText()
                                    .toString();

                            if (name.isEmpty()) {
                                nameInput.setError("Enter full name");
                                nameInput.requestFocus();
                                return;
                            }

                            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                emailInput.setError("Enter a valid email");
                                emailInput.requestFocus();
                                return;
                            }

                            if (password.length() < 8) {
                                passwordInput.setError(
                                        "Use at least 8 characters");
                                passwordInput.requestFocus();
                                return;
                            }

                            if (!password.equals(confirm)) {
                                confirmInput.setError(
                                        "Passwords do not match");
                                confirmInput.requestFocus();
                                return;
                            }




                            // Saves the account. The form only closes once it worked.
                            String role = "Admin".equals(roleSpinner.getSelectedItem())
                                    ? "administrator" : "learner";
                            adminService.createUser(email, password, name, role, new ApiCallback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    dialog.dismiss();
                                    Toast.makeText(UserTab.this, name + " was created", Toast.LENGTH_SHORT).show();
                                    loadUsers();
                                }

                                @Override
                                public void onError(String message) {
                                    Toast.makeText(UserTab.this, message, Toast.LENGTH_LONG).show();
                                }
                            });
                        }));

        dialog.show();
    }

    private EditText formInput(String hint, int inputType) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setSingleLine(true);
        input.setTextSize(14);
        input.setTextColor(NAVY);
        input.setHintTextColor(MUTED);
        input.setInputType(inputType);
        input.setPadding(dp(10), dp(8), dp(10), dp(8));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, dp(52));
        params.bottomMargin = dp(8);
        input.setLayoutParams(params);

        return input;
    }

    // Reusable confirmation dialog for future user records.

    private void confirmDeleteUser(
            String userName, Runnable onConfirmedDelete) {

        new AlertDialog.Builder(this)
                .setTitle("Delete User")
                .setMessage("Are you sure you want to delete "
                        + userName + "? This action cannot be undone.")
                .setNegativeButton("Cancel", (dialog, which) ->
                        dialog.dismiss())
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (onConfirmedDelete != null) {
                        onConfirmedDelete.run();
                    }
                })
                .show();
    }

    private void addSectionTitle(
            LinearLayout parent, String title) {
        TextView label = text(title, 12, ORANGE, true);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(12);
        parent.addView(label, params);
    }

    private TextView addStat(
            LinearLayout parent, String value,
            String label, int accent) {

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(10), dp(14), dp(8), dp(12));
        card.setBackground(round(WHITE, 14));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(0, dp(100), 1);
        params.setMargins(dp(3), 0, dp(3), 0);
        parent.addView(card, params);

        View stripe = new View(this);
        stripe.setBackground(round(accent, 4));
        card.addView(stripe,
                new LinearLayout.LayoutParams(dp(28), dp(4)));

        TextView number = text(value, 24, NAVY, true);
        LinearLayout.LayoutParams numberParams =
                new LinearLayout.LayoutParams(-1, -2);
        numberParams.topMargin = dp(10);
        card.addView(number, numberParams);

        TextView caption = text(label, 11, MUTED, false);
        LinearLayout.LayoutParams captionParams =
                new LinearLayout.LayoutParams(-1, -2);
        captionParams.topMargin = dp(4);
        card.addView(caption, captionParams);

        return number;
    }

    private TextView createFilter(String label) {
        TextView filter = text(label, 12, MUTED, true);
        filter.setGravity(Gravity.CENTER);
        filter.setPadding(dp(8), dp(10), dp(8), dp(10));
        filter.setClickable(true);
        filter.setFocusable(true);
        return filter;
    }

    private void addFilter(
            LinearLayout parent, TextView filter) {
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(0, -2, 1);
        params.setMargins(dp(2), 0, dp(2), 0);
        parent.addView(filter, params);
    }

    private void selectFilter(String filter) {
        selectedFilter = filter;
        updateFilterAppearance();
        updateEmptyState();
    }

    private void updateFilterAppearance() {
        styleFilter(allFilter, "All".equals(selectedFilter));
        styleFilter(activeFilter, "Active".equals(selectedFilter));
        styleFilter(inactiveFilter, "Inactive".equals(selectedFilter));
    }

    private void styleFilter(TextView view, boolean selected) {
        if (selected) {
            view.setTextColor(WHITE);
            view.setBackground(round(NAVY, 9));
        } else {
            view.setTextColor(MUTED);
            view.setBackground(round(PALE, 9));
        }
    }

    private void updateEmptyState() {
        if (searchInput == null || resultCount == null
                || emptyMessage == null) {
            return;
        }

        String query = searchInput.getText().toString().trim()
                .toLowerCase(Locale.ROOT);

        // Shows the users that match the search and the status filter
        userList.removeAllViews();
        int shown = 0;
        for (User user : users) {
            boolean active = "active".equals(user.status);
            if ("Active".equals(selectedFilter) && !active) continue;
            if ("Inactive".equals(selectedFilter) && active) continue;
            if (!query.isEmpty()
                    && !user.fullName.toLowerCase(Locale.ROOT).contains(query)
                    && !user.email.toLowerCase(Locale.ROOT).contains(query)) continue;

            LinearLayout.LayoutParams rowParams =
                    new LinearLayout.LayoutParams(-1, -2);
            rowParams.bottomMargin = dp(10);
            userList.addView(userRow(user), rowParams);
            shown++;
        }
        resultCount.setText(shown + (shown == 1 ? " user" : " users"));

        if (shown > 0) {
            return;
        }
        userList.addView(emptyMessage,
                new LinearLayout.LayoutParams(-1, -2));

        if (users.isEmpty() && query.isEmpty() && "All".equals(selectedFilter)) {
            emptyMessage.setText(
                    "No user data available yet.\n\n"
                            + "When the backend is connected, real user "
                            + "accounts will appear here.");
        } else {
            emptyMessage.setText(
                    "No matching users.\n\n"
                            + "User records will appear when real data "
                            + "is connected.");
        }
    }

    private TextView text(
            String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);

        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
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
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }
}