package com.momentum.backend.model;

// One learner on one course, as read from the Enrolments table.
// Dates are kept as text (YYYY-MM-DD), which is how the app shows them anyway.
public class Enrolment {

    public int enrolmentId;
    public int userId;
    public int courseId;
    public String state;              // not_started, in_progress or completed
    public int timeSpentMinutes;      // display only, never used for points
    public String assessmentStatus;   // passed, failed, attempted, or null
    public Double score;
    public Double scoreMax;
    public String firstAccess;
    public String completedOn;
    public String lastSyncedAt;
    public Integer pointsEarned;      // null until the course earns points

}
