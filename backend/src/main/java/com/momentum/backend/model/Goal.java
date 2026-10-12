package com.momentum.backend.model;

// One goal, as read from the Goals table.
public class Goal {

    public int goalId;
    public int userId;
    public String title;
    public String goalType;
    public Integer targetValue;
    public Integer targetCourseId;
    public String targetDate;
    public String status;
    public boolean pointsEligible;
    public String createdAt;
    public String completedAt;

}
