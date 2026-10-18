package com.example.momentum.service;

import com.example.momentum.data.Api;
import com.example.momentum.data.ApiCallback;
import com.example.momentum.data.MomentumApi;
import com.example.momentum.model.ActivityLog;
import com.example.momentum.model.AdminStats;
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
}
