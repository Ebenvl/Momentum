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
        // TODO: check email and password are not empty before sending
        api.login(email, password, new ApiCallback<User>() {
            @Override
            public void onSuccess(User user) {
                Session.setUser(user);
                callback.onSuccess(user);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void register(String email, String password, String fullName, ApiCallback<User> callback) {
        // TODO: check nothing is empty, the email looks valid, and the password is long enough
        api.register(email, password, fullName, callback);
    }

    public void logout() {
        Session.logout();
    }
}
