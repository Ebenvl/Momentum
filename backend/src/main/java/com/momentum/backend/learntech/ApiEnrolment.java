package com.momentum.backend.learntech;

// One course a learner is on, as nested inside a learner in GET /learners.
// Anything that can be null is Integer or an object, never int (API point 6).
public class ApiEnrolment {

    public int courseId;
    public String code;               // display only, never a key (API point 3)
    public String courseName;
    public String category;
    public String state;              // not_started, in_progress or completed
    public Integer expectedMinutes;   // can be null (API point 4)
    public int timeSpentMinutes;
    public ApiAssessment assessment;  // null when the course has no assessment
    public String dateEnrolled;
    public String firstAccess;
    public String dateCompleted;
    public String lastActivity;

}
