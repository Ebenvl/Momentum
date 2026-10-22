package com.example.momentum.model;

// One of the learner's own courses and how far they are with it, as of the last sync.
public class Enrolment {
    public int courseId;        // matches Course.courseId in the catalogue
    public String state;        // "not_started", "in_progress" or "completed"
    public int timeSpentMinutes;
    public String completedOn;  // yyyy-MM-dd, null until completed
    public Integer pointsEarned; // what this course gave the learner, null until it gives any
}
