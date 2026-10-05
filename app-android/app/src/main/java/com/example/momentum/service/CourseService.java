package com.example.momentum.service;

import com.example.momentum.data.Api;
import com.example.momentum.data.ApiCallback;
import com.example.momentum.data.MomentumApi;
import com.example.momentum.model.Course;

import java.util.List;

// The learner's enrolled courses.
public class CourseService {

    private final MomentumApi api = Api.get();

    public void getCourses(int userId, ApiCallback<List<Course>> callback) {
        // TODO: search and filter courses (FR-10)
        api.getCourses(userId, callback);
    }
}
