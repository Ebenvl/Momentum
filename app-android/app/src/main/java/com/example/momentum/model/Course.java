package com.example.momentum.model;

// A course the learner is enrolled on, as synced from LearnTech.
public class Course {
    public int courseId;
    public String name;
    public String state;             // "not_started", "in_progress" or "completed"
    public Integer expectedMinutes;  // can be null
    public int timeSpentMinutes;     // display only, never used for points

    public Course(int courseId, String name, String state, Integer expectedMinutes, int timeSpentMinutes) {
        this.courseId = courseId;
        this.name = name;
        this.state = state;
        this.expectedMinutes = expectedMinutes;
        this.timeSpentMinutes = timeSpentMinutes;
    }

    // Readable label for the state. There is no per-course percentage.
    public String stateLabel() {
        if ("completed".equals(state)) return "Completed";
        if ("in_progress".equals(state)) return "In progress";
        return "Not started";
    }
}
