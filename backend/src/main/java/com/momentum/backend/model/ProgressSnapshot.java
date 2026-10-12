package com.momentum.backend.model;

// One dated snapshot of a learner's position, from the ProgressSnapshots table.
public class ProgressSnapshot {

    public int snapshotId;
    public int userId;
    public String snapshotAt;
    public int coursesEnrolled;
    public int coursesCompleted;
    public int totalMinutes;
    public int totalPoints;
    public int currentStreak;

}
