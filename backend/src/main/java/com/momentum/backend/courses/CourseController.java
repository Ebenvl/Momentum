package com.momentum.backend.courses;

import com.momentum.backend.auth.CurrentUser;
import com.momentum.backend.db.CourseRepository;
import com.momentum.backend.db.EnrolmentRepository;
import io.javalin.http.Context;

import java.sql.SQLException;
import java.util.Map;

// Handles the course catalogue for learners.
public class CourseController {

    private final CourseRepository courseRepository;
    private final EnrolmentRepository enrolmentRepository;

    public CourseController(CourseRepository courseRepository, EnrolmentRepository enrolmentRepository) {
        this.courseRepository = courseRepository;
        this.enrolmentRepository = enrolmentRepository;
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

    // -----------------------------------------------------
    // GET /me/courses   the learner's own courses and their state, from the last sync
    // -----------------------------------------------------
    public void getMyCourses(Context ctx) {

        Integer userId = CurrentUser.id(ctx);
        if (userId == null) {
            ctx.status(401).json(Map.of("error", "please log in"));
            return;
        }

        try {
            ctx.json(enrolmentRepository.findEnrolmentsForUser(userId));
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "something went wrong, try again"));
        }
    }
}
