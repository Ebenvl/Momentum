package com.example.momentum.model;

// A personal goal a learner sets. Matches the Goals table.
public class Goal {
    public int id;
    public String title;
    public String goalType;     // e.g. "complete_n_courses", "free_text"
    public Integer targetValue; // can be null
    public String targetDate;   // can be null, format yyyy-MM-dd
    public String status;       // "active", "completed", "failed", "cancelled"
    public int checkIns;

    public Goal(int id, String title, String goalType, Integer targetValue,
                String targetDate, String status, int checkIns) {
        this.id = id;
        this.title = title;
        this.goalType = goalType;
        this.targetValue = targetValue;
        this.targetDate = targetDate;
        this.status = status;
        this.checkIns = checkIns;
    }
}
