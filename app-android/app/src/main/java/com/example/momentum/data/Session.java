package com.example.momentum.data;

import com.example.momentum.model.User;

// Remembers who is logged in while the app is open,
// and the login token the backend gave them.
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
    }

    public static boolean isLoggedIn() {
        return token != null;
    }

    public static void logout() {
        currentUser = null;
        token = null;
    }
}
