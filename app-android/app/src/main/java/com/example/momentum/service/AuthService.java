package com.example.momentum.service;

import com.example.momentum.data.Api;
import com.example.momentum.data.ApiCallback;
import com.example.momentum.data.MomentumApi;
import com.example.momentum.data.Session;
import com.example.momentum.model.User;

// Login, register and logout.
public class AuthService {

    private final MomentumApi api = Api.get();

    public void login(String email, String password, ApiCallback<User> callback) {
        api.login(email, password, callback);
    }

    // The code from the email, when two-step login is on.
    public void verifyLogin(String email, String code, ApiCallback<User> callback) {
        api.verifyLogin(email, code, callback);
    }

    // Forgot password: emails a code, then the code and a new password reset it.
    public void forgotPassword(String email, ApiCallback<Void> callback) {
        api.forgotPassword(email, callback);
    }

    public void resetPassword(String email, String code, String newPassword, ApiCallback<Void> callback) {
        api.resetPassword(email, code, newPassword, callback);
    }

    public void register(String email, String password, String fullName, ApiCallback<Void> callback) {
        api.register(email, password, fullName, callback);
    }

    public void logout() {
        Session.logout();
    }
}
