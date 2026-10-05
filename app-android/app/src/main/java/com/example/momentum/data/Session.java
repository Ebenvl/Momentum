package com.example.momentum.data;

import com.example.momentum.model.User;

// Remembers who is logged in while the app is open.
public class Session {

    private static User currentUser;

    public static User getUser() {
        return currentUser;
    }

    public static void setUser(User user) {
        currentUser = user;
    }

    public static void logout() {
        currentUser = null;
    }
}
