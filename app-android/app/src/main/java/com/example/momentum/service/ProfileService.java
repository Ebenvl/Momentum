package com.example.momentum.service;

import com.example.momentum.data.Api;
import com.example.momentum.data.ApiCallback;
import com.example.momentum.data.MomentumApi;
import com.example.momentum.model.Badge;
import com.example.momentum.model.User;

import java.util.List;

// The learner's own profile.
public class ProfileService {

    private final MomentumApi api = Api.get();

    // Their details, all-time points and streak, and whether they're linked to LearnTech.
    public void getMe(ApiCallback<User> callback) {
        api.getMe(callback);
    }

    public void updateName(String fullName, ApiCallback<Void> callback) {
        api.updateName(fullName, callback);
    }

    // The current password has to be right before it changes.
    public void changePassword(String currentPassword, String newPassword, ApiCallback<Void> callback) {
        api.changePassword(currentPassword, newPassword, callback);
    }

    // Two-step login: emails a code to check the email is theirs, then turns it on.
    public void startTwoFactor(ApiCallback<Void> callback) {
        api.startTwoFactor(callback);
    }

    public void confirmTwoFactor(String code, ApiCallback<Void> callback) {
        api.confirmTwoFactor(code, callback);
    }

    // Turning it off needs their password.
    public void disableTwoFactor(String password, ApiCallback<Void> callback) {
        api.disableTwoFactor(password, callback);
    }

    // The badges they've earned, newest first.
    public void getMyBadges(ApiCallback<List<Badge>> callback) {
        api.getMyBadges(callback);
    }
}
