package com.example.momentum.service;

import com.example.momentum.data.Api;
import com.example.momentum.data.ApiCallback;
import com.example.momentum.data.MomentumApi;
import com.example.momentum.model.Goal;

import java.util.List;

// The learner's goals.
public class GoalService {

    private final MomentumApi api = Api.get();

    public void getGoals(ApiCallback<List<Goal>> callback) {
        api.getGoals(callback);
    }

    // targetCourseId and targetDate (yyyy-MM-dd) can be null.
    public void createGoal(String title, Integer targetCourseId, String targetDate, ApiCallback<Void> callback) {
        api.createGoal(title, targetCourseId, targetDate, callback);
    }

    public void completeGoal(int goalId, ApiCallback<Void> callback) {
        api.setGoalStatus(goalId, "completed", callback);
    }

    public void reopenGoal(int goalId, ApiCallback<Void> callback) {
        api.setGoalStatus(goalId, "active", callback);
    }

    public void deleteGoal(int goalId, ApiCallback<Void> callback) {
        api.deleteGoal(goalId, callback);
    }
}
