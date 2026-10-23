package com.example.momentum.data;

import com.example.momentum.model.User;

// Who is logged in and their token. OfflineStore saves it, so they stay logged in.
public class Session {

    private static User currentUser;
    private static String token;

    public static User getUser() {
        return currentUser;
    }

    public static String getToken() {
        return token;
    }

    public static void start(User user, String newToken) {
        currentUser = user;
        token = newToken;
        OfflineStore.saveLogin(user, newToken);
    }

    public static boolean isLoggedIn() {
        return token != null;
    }

    public static void logout() {
        if (currentUser != null) {
            OfflineStore.forget(currentUser.id);
        }
        currentUser = null;
        token = null;
    }
}
