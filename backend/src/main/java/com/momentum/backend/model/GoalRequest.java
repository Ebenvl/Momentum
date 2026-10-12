package com.momentum.backend.model;

// Holds the details the app sends when someone creates or edits a goal.
public class GoalRequest {

    public String title;
    public String goalType;
    public Integer targetValue;
    public Integer targetCourseId;
    public String targetDate;     // YYYY-MM-DD, or null for no deadline

}
