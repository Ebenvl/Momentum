package com.example.momentum.placeholder;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.example.momentum.R;
import com.example.momentum.data.ApiCallback;
import com.example.momentum.data.Session;
import com.example.momentum.model.Goal;
import com.example.momentum.service.GoalService;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

// TEMPORARY goals screen.
// Tap a goal to check in, long press to delete.
public class GoalsActivity extends BaseActivity {

    private final GoalService goalService = new GoalService();
    private final List<Goal> goals = new ArrayList<>();
    private ListView list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!requireLogin()) return;
        setContentView(R.layout.temp_goals);
        applyInsets(findViewById(R.id.root));

        EditText goalInput = findViewById(R.id.goalInput);
        Button addButton = findViewById(R.id.addGoalButton);
        list = findViewById(R.id.list);

        addButton.setOnClickListener(v -> {
            // TODO: let the learner choose the goal type, target and date
            Goal goal = new Goal(0, goalInput.getText().toString().trim(),
                    "free_text", null, null, "active", 0);
            goalService.createGoal(Session.getUser().id, goal, new ApiCallback<Goal>() {
                @Override
                public void onSuccess(Goal saved) {
                    goalInput.setText("");
                    toast("Goal added");
                    loadGoals();
                }

                @Override
                public void onError(String message) {
                    toast(message);
                }
            });
        });

        list.setOnItemClickListener((parent, view, position, id) -> checkIn(goals.get(position)));

        list.setOnItemLongClickListener((parent, view, position, id) -> {
            confirmDelete(goals.get(position));
            return true;
        });

        loadGoals();
    }

    private void loadGoals() {
        goalService.getGoals(Session.getUser().id, new ApiCallback<List<Goal>>() {
            @Override
            public void onSuccess(List<Goal> result) {
                goals.clear();
                goals.addAll(result);
                List<String> rows = new ArrayList<>();
                for (Goal g : goals) {
                    String due = g.targetDate == null ? "" : "  ·  due " + g.targetDate;
                    rows.add(g.title + "\nCheck-ins: " + g.checkIns + due);
                }
                list.setAdapter(new ArrayAdapter<>(GoalsActivity.this,
                        android.R.layout.simple_list_item_1, rows));
            }

            @Override
            public void onError(String message) {
                toast(message);
            }
        });
    }

    private void checkIn(Goal goal) {
        goalService.logCheckIn(goal.id, null, new ApiCallback<Goal>() {
            @Override
            public void onSuccess(Goal updated) {
                toast("Checked in");
                loadGoals();
            }

            @Override
            public void onError(String message) {
                toast(message);
            }
        });
    }

    private void confirmDelete(Goal goal) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete goal?")
                .setMessage(goal.title)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) ->
                        goalService.deleteGoal(goal.id, new ApiCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                toast("Goal deleted");
                                loadGoals();
                            }

                            @Override
                            public void onError(String message) {
                                toast(message);
                            }
                        }))
                .show();
    }
}
