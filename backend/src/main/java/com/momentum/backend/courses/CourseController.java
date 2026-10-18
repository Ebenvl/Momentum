package com.momentum.backend.courses;

import com.momentum.backend.auth.CurrentUser;
import com.momentum.backend.db.CourseRepository;
import io.javalin.http.Context;

import java.sql.SQLException;
import java.util.Map;

// Handles the course catalogue for learners.
public class CourseController {

    private final CourseRepository courseRepository;

    public CourseController(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    // -----------------------------------------------------
    // GET /courses   every course, for picking one when setting a goal
    // -----------------------------------------------------
    public void getCatalogue(Context ctx) {

        if (CurrentUser.id(ctx) == null) {
            ctx.status(401).json(Map.of("error", "please log in"));
            return;
        }

        try {
            ctx.json(courseRepository.findAllCourses());
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "something went wrong, try again"));
        }
    }
}
