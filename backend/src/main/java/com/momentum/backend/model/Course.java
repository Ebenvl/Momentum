package com.momentum.backend.model;

// One course, as read from the Courses table.
// expectedMinutes and pointsValue are Integer, not int, because they can be
// null (API v1.0, point 4: never treat a missing duration as zero).
public class Course {

    public int courseId;
    public int platformCourseId;
    public String courseName;
    public Integer expectedMinutes;
    public Integer pointsValue;

}
