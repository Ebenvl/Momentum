package com.momentum.backend.model;

// One user, as read from the Users table.
public class User {

    public int userId;
    public Integer idst;    // LearnTech learner id, null means not linked yet
    public String email;
    public String passwordHash;
    public String fullName;
    public String role;
    public String status;
    public String lastLoginAt;   // null if they never logged in
    public Integer courseCount;  // only filled in for the admin's user list

}
