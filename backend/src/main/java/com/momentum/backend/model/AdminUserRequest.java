package com.momentum.backend.model;

// Holds the details an admin sends when creating or editing an account.
// email and password are only used when creating.
public class AdminUserRequest {

    public String email;
    public String password;
    public String fullName;
    public String role;     // learner or administrator

}
