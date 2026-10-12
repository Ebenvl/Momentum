package com.momentum.backend.auth;

import io.javalin.http.Context;

// Tells a controller who is making the request.
//
// The JWT check will read the token the app sends,
// and put the user's id and role on the request. Until that is built, these
// give back null and false, so every protected route replies "please log in".
// That is on purpose: nothing is open by accident while the check is missing.
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
