package com.example.momentum.service;

import com.example.momentum.data.Api;
import com.example.momentum.data.ApiCallback;
import com.example.momentum.data.MomentumApi;
import com.example.momentum.model.User;

import java.util.List;

// Administrator features.
public class AdminService {

    private final MomentumApi api = Api.get();

    public void getAllUsers(ApiCallback<List<User>> callback) {
        api.getAllUsers(callback);
    }

    // Suspends an active user, or reactivates a suspended one.
    public void toggleSuspended(User user, ApiCallback<User> callback) {
        String newStatus = "suspended".equals(user.status) ? "active" : "suspended";
        api.setUserStatus(user.id, newStatus, callback);
    }

    // TODO: override course points, define badges, view the activity log
}
