package com.example.momentum.data;

import com.example.momentum.model.ActivityLog;
import com.example.momentum.model.AdminStats;
import com.example.momentum.model.Course;
import com.example.momentum.model.Enrolment;
import com.example.momentum.model.Goal;
import com.example.momentum.model.LeaderboardEntry;
import com.example.momentum.model.StudySession;
import com.example.momentum.model.User;

import java.util.List;

// Everything the app can ask the backend for.
// The logged in user is known from the token in Session,
// so most calls don't need a user id.
public interface MomentumApi {

    void login(String email, String password, ApiCallback<User> callback);

    void register(String email, String password, String fullName, ApiCallback<Void> callback);

    void getCatalogue(ApiCallback<List<Course>> callback);

    void getMyCourses(ApiCallback<List<Enrolment>> callback);

    void getGoals(ApiCallback<List<Goal>> callback);

    void createGoal(String title, Integer targetCourseId, String targetDate, ApiCallback<Void> callback);

    void setGoalStatus(int goalId, String status, ApiCallback<Void> callback);

    void deleteGoal(int goalId, ApiCallback<Void> callback);

    void getLeaderboard(ApiCallback<List<LeaderboardEntry>> callback);

    void getStudySessions(ApiCallback<List<StudySession>> callback);

    // A null id adds a new session, otherwise that session is updated.
    void saveStudySession(Integer id, String day, String time, String course, String task, ApiCallback<Void> callback);

    void deleteStudySession(int id, ApiCallback<Void> callback);

    void getAdminStats(ApiCallback<AdminStats> callback);

    void getAllUsers(ApiCallback<List<User>> callback);

    void deleteUser(int userId, ApiCallback<Void> callback);

    void getActivityLogs(ApiCallback<List<ActivityLog>> callback);
}
