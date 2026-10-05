package com.example.momentum.service;

import com.example.momentum.data.Api;
import com.example.momentum.data.ApiCallback;
import com.example.momentum.data.MomentumApi;
import com.example.momentum.model.Goal;

import java.util.List;

// Goals and check-ins.
public class GoalService {

    private final MomentumApi api = Api.get();

    public void getGoals(int userId, ApiCallback<List<Goal>> callback) {
        api.getGoals(userId, callback);
    }

    public void createGoal(int userId, Goal goal, ApiCallback<Goal> callback) {
        // TODO: check the title is not empty and the target makes sense
        api.createGoal(userId, goal, callback);
    }

    public void deleteGoal(int goalId, ApiCallback<Void> callback) {
        api.deleteGoal(goalId, callback);
    }

    public void logCheckIn(int goalId, String note, ApiCallback<Goal> callback) {
        // TODO: update the streak after a check-in
        api.logCheckIn(goalId, note, callback);
    }
}
