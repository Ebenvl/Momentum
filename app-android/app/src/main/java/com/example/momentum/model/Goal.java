package com.example.momentum.model;

// A personal goal a learner sets. Matches the Goals table.
public class Goal {
    public int id;
    public String title;
    public String goalType;         // e.g. "complete_specific_course", "free_text"
    public Integer targetValue;     // can be null
    public Integer targetCourseId;  // can be null
    public String targetDate;       // can be null, format yyyy-MM-dd
    public String status;           // "active", "completed", "failed", "cancelled"
    public String createdAt;
    public String completedAt;      // null until completed
}
