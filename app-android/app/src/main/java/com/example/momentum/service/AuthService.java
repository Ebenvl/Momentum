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

    public void register(String email, String password, String fullName, ApiCallback<Void> callback) {
        api.register(email, password, fullName, callback);
    }

    public void logout() {
        Session.logout();
    }
}
