package com.momentum.backend.auth;

import io.javalin.http.Context;

// Tells a controller who is making the request.
// Gives back null and false when there is no valid login.
public class CurrentUser {

    // The logged in user's id, or null if nobody is logged in.
    public static Integer id(Context ctx) {
        return ctx.attribute("userId");
    }

    // True if the logged in user is an administrator.
    public static boolean isAdmin(Context ctx) {
        return "administrator".equals(ctx.attribute("role"));
    }
}
