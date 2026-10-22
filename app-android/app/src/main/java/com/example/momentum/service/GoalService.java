package com.example.momentum.service;

import com.example.momentum.data.Api;
import com.example.momentum.data.ApiCallback;
import com.example.momentum.data.MomentumApi;
import com.example.momentum.model.CheckIn;
import com.example.momentum.model.Goal;

import java.util.List;

// The learner's goals.
public class GoalService {

    private final MomentumApi api = Api.get();

    public void getGoals(ApiCallback<List<Goal>> callback) {
        api.getGoals(callback);
    }

    // A null goalId adds a new goal, otherwise that goal is updated.
    // targetValue, targetCourseId and targetDate (yyyy-MM-dd) can be null.
    public void saveGoal(Integer goalId, String title, String goalType, Integer targetValue,
                         Integer targetCourseId, String targetDate, ApiCallback<Void> callback) {
        api.saveGoal(goalId, title, goalType, targetValue, targetCourseId, targetDate, callback);
    }

    // Logs progress on a goal. Also counts towards the learner's streak.
    // Gives back the names of any easter eggs it unlocked.
    public void logCheckIn(int goalId, String note, Integer valueLogged, ApiCallback<List<String>> callback) {
        api.logCheckIn(goalId, note, valueLogged, callback);
    }

    public void getCheckIns(int goalId, ApiCallback<List<CheckIn>> callback) {
        api.getCheckIns(goalId, callback);
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
