package com.example.momentum.data;

import com.example.momentum.model.User;

// Remembers who is logged in while the app is open,
// and the login token the backend gave them.
// OfflineStore also saves it on the phone, so the next time the app opens
// they're still logged in, even without signal.
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
