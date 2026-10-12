package com.momentum.backend.model;

// One entry in the activity and error log, from the ActivityLogs table.
public class ActivityLog {

    public int logId;
    public Integer userId;
    public String action;
    public String level;      // info, warning or error
    public String createdAt;

}
