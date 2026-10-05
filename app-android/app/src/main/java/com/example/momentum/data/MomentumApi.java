package com.example.momentum.data;

import com.example.momentum.model.Course;
import com.example.momentum.model.Goal;
import com.example.momentum.model.LeaderboardEntry;
import com.example.momentum.model.Stats;
import com.example.momentum.model.User;

import java.util.List;

// Everything the app can ask the backend for.
// FakeMomentumApi answers with sample data for now.
// A real version that calls the backend will replace it later.
public interface MomentumApi {

    void login(String email, String password, ApiCallback<User> callback);

    void register(String email, String password, String fullName, ApiCallback<User> callback);

    void getStats(int userId, ApiCallback<Stats> callback);

    void getCourses(int userId, ApiCallback<List<Course>> callback);

    void getGoals(int userId, ApiCallback<List<Goal>> callback);

    void createGoal(int userId, Goal goal, ApiCallback<Goal> callback);

    void deleteGoal(int goalId, ApiCallback<Void> callback);

    void logCheckIn(int goalId, String note, ApiCallback<Goal> callback);

    void getLeaderboard(ApiCallback<List<LeaderboardEntry>> callback);

    void getAllUsers(ApiCallback<List<User>> callback);

    void setUserStatus(int userId, String status, ApiCallback<User> callback);
}
