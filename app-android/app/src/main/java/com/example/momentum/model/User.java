package com.example.momentum.model;

// A user of the app. Matches the Users table.
public class User {
    public int id;
    public Integer idst;        // LearnTech learner ID, null means not linked yet
    public String email;
    public String fullName;
    public String role;         // "learner" or "administrator"
    public String status;       // "active" or "suspended"
    public String lastLoginAt;  // only in the admin's user list, null if never
    public Integer courseCount; // only in the admin's user list
    public Integer totalPoints;   // only in the learner's own profile (/me)
    public Integer currentStreak; // only in the learner's own profile (/me)
    public Integer longestStreak; // only in the learner's own profile (/me)

    public User(int id, Integer idst, String email, String fullName, String role, String status) {
        this.id = id;
        this.idst = idst;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.status = status;
    }

    public boolean isAdmin() {
        return "administrator".equals(role);
    }

    public boolean isLinked() {
        return idst != null;
    }
}
