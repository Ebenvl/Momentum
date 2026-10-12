package com.momentum.backend.model;

// Holds an admin's change to a course: the points override (FR-15)
// or the expected duration (FR-27). Either can be null to clear it.
public class CourseUpdateRequest {

    public Integer pointsValue;
    public Integer expectedMinutes;

}
