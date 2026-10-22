package com.example.momentum.service;

import com.example.momentum.data.Api;
import com.example.momentum.data.ApiCallback;
import com.example.momentum.data.MomentumApi;
import com.example.momentum.model.ActivityLog;
import com.example.momentum.model.AdminStats;
import com.example.momentum.model.Badge;
import com.example.momentum.model.CourseStat;
import com.example.momentum.model.PlatformSettings;
import com.example.momentum.model.User;

import java.util.List;

// Administrator features.
public class AdminService {

    private final MomentumApi api = Api.get();

    public void getStats(ApiCallback<AdminStats> callback) {
        api.getAdminStats(callback);
    }

    public void getAllUsers(ApiCallback<List<User>> callback) {
        api.getAllUsers(callback);
    }

    public void deleteUser(int userId, ApiCallback<Void> callback) {
        api.deleteUser(userId, callback);
    }

    public void getActivityLogs(ApiCallback<List<ActivityLog>> callback) {
        api.getActivityLogs(callback);
    }

    // Enrolled and completed per course, most enrolled first.
    public void getCourseStats(ApiCallback<List<CourseStat>> callback) {
        api.getCourseStats(callback);
    }

    // Accounts

    public void createUser(String email, String password, String fullName, String role,
                           ApiCallback<Void> callback) {
        api.createUser(email, password, fullName, role, callback);
    }

    public void updateUser(int userId, String fullName, String role, ApiCallback<Void> callback) {
        api.updateUser(userId, fullName, role, callback);
    }

    public void suspendUser(int userId, ApiCallback<Void> callback) {
        api.setUserStatus(userId, "suspended", callback);
    }

    public void reactivateUser(int userId, ApiCallback<Void> callback) {
        api.setUserStatus(userId, "active", callback);
    }

    // A null idst unlinks the account from LearnTech.
    public void linkUser(int userId, Integer idst, ApiCallback<Void> callback) {
        api.linkUser(userId, idst, callback);
    }

    // Courses: the points override and expected minutes. Null clears either one.
    public void updateCourse(int courseId, Integer pointsValue, Integer expectedMinutes,
                             ApiCallback<Void> callback) {
        api.updateCourse(courseId, pointsValue, expectedMinutes, callback);
    }

    // Badges

    public void getAllBadges(ApiCallback<List<Badge>> callback) {
        api.getAllBadges(callback);
    }

    // A null badgeId adds a new badge, otherwise that badge is updated.
    public void saveBadge(Integer badgeId, Badge badge, ApiCallback<Void> callback) {
        api.saveBadge(badgeId, badge, callback);
    }

    public void deleteBadge(int badgeId, ApiCallback<Void> callback) {
        api.deleteBadge(badgeId, callback);
    }

    // The LearnTech connection

    public void getSettings(ApiCallback<PlatformSettings> callback) {
        api.getSettings(callback);
    }

    public void saveSettings(String platformUrl, String apiKey, String apiSecret, ApiCallback<Void> callback) {
        api.saveSettings(platformUrl, apiKey, apiSecret, callback);
    }

    public void testConnection(ApiCallback<Integer> callback) {
        api.testConnection(callback);
    }

    // A table as CSV text, for example "users" or "points".
    public void exportTable(String table, ApiCallback<String> callback) {
        api.exportTable(table, callback);
    }
}
