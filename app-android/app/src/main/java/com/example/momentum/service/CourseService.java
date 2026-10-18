package com.example.momentum.service;

import com.example.momentum.data.Api;
import com.example.momentum.data.ApiCallback;
import com.example.momentum.data.MomentumApi;
import com.example.momentum.model.Course;
import com.example.momentum.model.Enrolment;

import java.util.List;

// Courses.
public class CourseService {

    private final MomentumApi api = Api.get();

    // Every course on the platform, used to link a goal to a course.
    public void getCatalogue(ApiCallback<List<Course>> callback) {
        api.getCatalogue(callback);
    }

    // The learner's own courses and their state.
    public void getMyCourses(ApiCallback<List<Enrolment>> callback) {
        api.getMyCourses(callback);
    }
}
