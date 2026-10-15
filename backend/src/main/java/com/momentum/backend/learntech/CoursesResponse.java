package com.momentum.backend.learntech;

import java.util.List;

// The whole reply from GET /courses.
public class CoursesResponse {

    public boolean success;
    public String generatedAt;
    public int count;
    public List<ApiCourse> courses;

}
