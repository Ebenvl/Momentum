package com.momentum.backend.model;

// Holds an admin's change to a course: the points override
// or the expected duration. Either can be null to clear it.
public class CourseUpdateRequest {

    public Integer pointsValue;
    public Integer expectedMinutes;

}
