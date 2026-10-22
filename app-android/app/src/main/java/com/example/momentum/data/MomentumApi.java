package com.example.momentum.data;

import com.example.momentum.model.ActivityLog;
import com.example.momentum.model.AdminStats;
import com.example.momentum.model.Badge;
import com.example.momentum.model.CheckIn;
import com.example.momentum.model.Course;
import com.example.momentum.model.CourseStat;
import com.example.momentum.model.Enrolment;
import com.example.momentum.model.Goal;
import com.example.momentum.model.LeaderboardEntry;
import com.example.momentum.model.PlatformSettings;
import com.example.momentum.model.StudySession;
import com.example.momentum.model.User;

import java.util.List;

// Everything the app can ask the backend for.
// The logged in user is known from the token in Session,
// so most calls don't need a user id.
public interface MomentumApi {

    // Gives back the user, or null if two-step login is on and a code was emailed.
    void login(String email, String password, ApiCallback<User> callback);

    // The second step of a login: the code from the email.
    void verifyLogin(String email, String code, ApiCallback<User> callback);

    // Emails a password reset code, if the email has an account.
    void forgotPassword(String email, ApiCallback<Void> callback);

    void resetPassword(String email, String code, String newPassword, ApiCallback<Void> callback);

    // Two-step login: emails a code, turns it on with that code, or off with the password.
    void startTwoFactor(ApiCallback<Void> callback);

    void confirmTwoFactor(String code, ApiCallback<Void> callback);

    void disableTwoFactor(String password, ApiCallback<Void> callback);

    void register(String email, String password, String fullName, ApiCallback<Void> callback);

    // The learner's own profile, with their all-time points, streak and whether they're linked.
    void getMe(ApiCallback<User> callback);

    // Changes the user's own name.
    void updateName(String fullName, ApiCallback<Void> callback);

    // Changes the user's own password. The current one has to be right.
    void changePassword(String currentPassword, String newPassword, ApiCallback<Void> callback);

    // The badges the learner has earned, newest first.
    void getMyBadges(ApiCallback<List<Badge>> callback);

    void getCatalogue(ApiCallback<List<Course>> callback);

    void getMyCourses(ApiCallback<List<Enrolment>> callback);

    void getGoals(ApiCallback<List<Goal>> callback);

    void setGoalStatus(int goalId, String status, ApiCallback<Void> callback);

    void deleteGoal(int goalId, ApiCallback<Void> callback);

    // A null id adds a new goal, otherwise that goal is updated.
    // goalType is free_text, complete_specific_course, complete_n_courses, spend_minutes or active_days.
    void saveGoal(Integer goalId, String title, String goalType, Integer targetValue,
                  Integer targetCourseId, String targetDate, ApiCallback<Void> callback);

    void logCheckIn(int goalId, String note, Integer valueLogged, ApiCallback<Void> callback);

    void getCheckIns(int goalId, ApiCallback<List<CheckIn>> callback);

    // This month's rankings, or all time when allTime is true.
    void getLeaderboard(boolean allTime, ApiCallback<List<LeaderboardEntry>> callback);

    void getStudySessions(ApiCallback<List<StudySession>> callback);

    // A null id adds a new session, otherwise that session is updated.
    void saveStudySession(Integer id, String day, String time, String course, String task, ApiCallback<Void> callback);

    void deleteStudySession(int id, ApiCallback<Void> callback);

    void getAdminStats(ApiCallback<AdminStats> callback);

    void getAllUsers(ApiCallback<List<User>> callback);

    void deleteUser(int userId, ApiCallback<Void> callback);

    void getActivityLogs(ApiCallback<List<ActivityLog>> callback);

    // Admin: enrolled and completed per course, for the engagement screen.
    void getCourseStats(ApiCallback<List<CourseStat>> callback);

    // Admin: accounts
    void createUser(String email, String password, String fullName, String role, ApiCallback<Void> callback);

    void updateUser(int userId, String fullName, String role, ApiCallback<Void> callback);

    // status is active or suspended
    void setUserStatus(int userId, String status, ApiCallback<Void> callback);

    // A null idst unlinks the account.
    void linkUser(int userId, Integer idst, ApiCallback<Void> callback);

    // Admin: courses. Either value can be null, which clears it.
    void updateCourse(int courseId, Integer pointsValue, Integer expectedMinutes, ApiCallback<Void> callback);

    // Admin: badges. A null id adds a new badge, otherwise that badge is updated.
    void getAllBadges(ApiCallback<List<Badge>> callback);

    void saveBadge(Integer badgeId, Badge badge, ApiCallback<Void> callback);

    void deleteBadge(int badgeId, ApiCallback<Void> callback);

    // Admin: the LearnTech connection. An empty key or secret keeps the saved one.
    void getSettings(ApiCallback<PlatformSettings> callback);

    void saveSettings(String platformUrl, String apiKey, String apiSecret, ApiCallback<Void> callback);

    // Gives back how many courses LearnTech has, if it connected.
    void testConnection(ApiCallback<Integer> callback);

    // Admin: a whole table as CSV text, for example "users" or "points".
    void exportTable(String table, ApiCallback<String> callback);
}
