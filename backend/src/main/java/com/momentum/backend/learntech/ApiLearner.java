package com.momentum.backend.learntech;

import java.util.List;

// One learner, exactly as LearnTech's GET /learners sends it (API v1.0).
// The field names match the API's names, written in Java style, so
// "course_id" in the JSON becomes courseId here. LearnTechClient does that.
public class ApiLearner {

    public int idst;
    public String username;
    public String firstname;
    public String lastname;
    public String email;
    public ApiSummary summary;
    public List<ApiEnrolment> courses;

}
