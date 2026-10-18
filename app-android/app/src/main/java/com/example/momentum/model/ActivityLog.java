package com.example.momentum.model;

// One entry in the system activity log.
public class ActivityLog {
    public int id;
    public Integer userId;   // null for events not tied to a user
    public String action;
    public String level;     // "info", "warning" or "error"
    public String createdAt;
}
