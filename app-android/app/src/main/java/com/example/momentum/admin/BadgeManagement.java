package com.example.momentum.admin;

import android.app.AlertDialog;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.example.momentum.data.ApiCallback;
import com.example.momentum.model.Badge;
import com.example.momentum.service.AdminService;

import java.util.Arrays;
import java.util.List;

// Badge Management: the admin defines badges, the rule that earns each one,
// and the points it's worth. The backend awards them by itself after every
// sync and check-in.
public class BadgeManagement extends AdminScreen {

    // The rules a badge can use, and how they're shown.
    private static final String[] RULES = {"courses_completed", "streak_days", "total_points"};
    private static final String[] RULE_NAMES = {"Complete a number of courses",
            "Reach a streak of a number of days", "Earn a number of points in total"};

    private final AdminService adminService = new AdminService();

    @Override
    protected String title() {
        return "Badge Management";
    }

    @Override
    protected String subtitle() {
        return "Badges learners earn, and what they're worth";
    }

    @Override
    protected void load() {
        content.removeAllViews();
        button("+  Add Badge", true, v -> showBadgeForm(null));

        adminService.getAllBadges(new ApiCallback<List<Badge>>() {
            @Override
            public void onSuccess(List<Badge> badges) {
                heading(badges.size() + (badges.size() == 1 ? " badge" : " badges"));
                if (badges.isEmpty()) {
                    note("No badges yet. Add one, and learners earn it as soon as they reach its rule.");
                }
                for (Badge badge : badges) {
                    showBadge(badge);
                }
            }

            @Override
            public void onError(String message) {
                note(message);
            }
        });
    }

    // One badge card: name, rule, points, and whether it can be earned right now.
    private void showBadge(Badge badge) {
        LinearLayout box = card();
        box.addView(text(badge.name + (badge.isActive ? "" : "  (turned off)"), 15, NAVY, true));
        box.addView(text(ruleText(badge) + "  •  +" + badge.pointsAwarded + " XP", 13, MUTED, false));
        if (badge.description != null && !badge.description.isEmpty()) {
            box.addView(text(badge.description, 13, MUTED, false));
        }
        box.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle(badge.name)
                .setItems(new String[]{"Edit badge", "Delete badge"}, (dialog, which) -> {
                    if (which == 0) {
                        showBadgeForm(badge);
                    } else {
                        confirmDelete(badge);
                    }
                })
                .show());
    }

    // The rule in words, like "Complete 5 courses".
    private String ruleText(Badge badge) {
        if ("courses_completed".equals(badge.ruleType)) {
            return "Complete " + badge.ruleValue + (badge.ruleValue == 1 ? " course" : " courses");
        } else if ("streak_days".equals(badge.ruleType)) {
            return badge.ruleValue + "-day streak";
        } else if ("total_points".equals(badge.ruleType)) {
            return "Earn " + badge.ruleValue + " points";
        }
        return "No rule";
    }

    // The form for adding a badge, or editing one when a badge is passed in.
    private void showBadgeForm(Badge existing) {
        EditText name = field("Badge name", existing != null ? existing.name : null, false);
        EditText description = field("Description (optional)", existing != null ? existing.description : null, false);
        Spinner rule = new Spinner(this);
        rule.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, RULE_NAMES));
        EditText target = field("Number to reach", existing != null ? String.valueOf(existing.ruleValue) : null, true);
        EditText points = field("Points it gives", existing != null ? String.valueOf(existing.pointsAwarded) : null, true);
        CheckBox active = new CheckBox(this);
        active.setText("Learners can earn it");
        active.setChecked(existing == null || existing.isActive);
        if (existing != null) {
            rule.setSelection(Math.max(0, Arrays.asList(RULES).indexOf(existing.ruleType)));
        }

        new AlertDialog.Builder(this)
                .setTitle(existing == null ? "New badge" : "Edit badge")
                .setView(form(name, description, rule, target, points, active))
                .setPositiveButton("Save", (dialog, which) -> {
                    Badge badge = new Badge();
                    badge.name = name.getText().toString().trim();
                    badge.description = description.getText().toString().trim();
                    badge.ruleType = RULES[rule.getSelectedItemPosition()];
                    Integer targetValue = numberFrom(target);
                    Integer pointsValue = numberFrom(points);
                    badge.isActive = active.isChecked();

                    if (badge.name.isEmpty()) {
                        toast("Enter a badge name");
                        return;
                    }
                    if (targetValue == null || targetValue <= 0) {
                        toast("Enter a number to reach, more than 0");
                        return;
                    }
                    badge.ruleValue = targetValue;
                    badge.pointsAwarded = pointsValue != null ? pointsValue : 0;

                    adminService.saveBadge(existing != null ? existing.id : null, badge, reloadAfter("Badge saved"));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDelete(Badge badge) {
        new AlertDialog.Builder(this)
                .setTitle("Delete " + badge.name + "?")
                .setMessage("Learners who earned it will lose it.")
                .setPositiveButton("Delete", (dialog, which) ->
                        adminService.deleteBadge(badge.id, reloadAfter("Badge deleted")))
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Shows a message and reloads the list when a change worked.
    private ApiCallback<Void> reloadAfter(String message) {
        return new ApiCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                toast(message);
                load();
            }

            @Override
            public void onError(String error) {
                toast(error);
            }
        };
    }
}
