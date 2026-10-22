package com.example.momentum.model;

// The learner's position on one day, from the ProgressSnapshots table.
// The backend saves one at every sync and sends back the last one of each day.
public class ProgressSnapshot {
    public String day;             // yyyy-MM-dd
    public int totalPoints;
    public int coursesCompleted;
    public int totalMinutes;
}
