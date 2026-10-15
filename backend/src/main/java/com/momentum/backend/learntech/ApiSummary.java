package com.momentum.backend.learntech;

// A learner's totals across all their courses (the "summary" in GET /learners).
public class ApiSummary {

    public int coursesEnrolled;
    public int coursesCompleted;
    public int percentComplete;     // the only percentage that exists
    public int totalTimeMinutes;    // display only, never for points
    public String lastActivity;

}
