package com.momentum.backend.learntech;

// One course from the catalogue, as GET /courses sends it.
public class ApiCourse {

    public int courseId;
    public String code;
    public String courseName;
    public String category;
    public Integer expectedMinutes;
    public boolean hasAssessment;

}
