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
    public boolean twoFactorEnabled;  // an emailed code is needed after the password
    public Integer courseCount;  // only filled in for the admin's user list
    public Integer totalPoints;  // only filled in for the learner's own profile
    public Integer currentStreak;  // only filled in for the learner's own profile
    public Integer longestStreak;  // only filled in for the learner's own profile

}
