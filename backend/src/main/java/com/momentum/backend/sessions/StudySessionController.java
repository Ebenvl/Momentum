package com.momentum.backend.sessions;

import com.momentum.backend.auth.CurrentUser;
import com.momentum.backend.db.CourseRepository;
import com.momentum.backend.db.StudySessionRepository;
import com.momentum.backend.model.StudySessionRequest;
import io.javalin.http.Context;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

// Handles the learner's weekly study timetable.
public class StudySessionController {

    private static final List<String> DAYS = List.of(
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");

    private final StudySessionRepository studySessionRepository;
    private final CourseRepository courseRepository;

    public StudySessionController(StudySessionRepository studySessionRepository,
                                  CourseRepository courseRepository) {
        this.studySessionRepository = studySessionRepository;
        this.courseRepository = courseRepository;
    }

    // -----------------------------------------------------
    // GET /study-sessions
    // -----------------------------------------------------
    public void getAll(Context ctx) {

        Integer userId = CurrentUser.id(ctx);
        if (userId == null) {
            ctx.status(401).json(Map.of("error", "please log in"));
            return;
        }

        try {
            ctx.json(studySessionRepository.findSessionsForUser(userId));
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "something went wrong, try again"));
        }
    }

    // -----------------------------------------------------
    // POST /study-sessions
    // -----------------------------------------------------
    public void create(Context ctx) {

        Integer userId = CurrentUser.id(ctx);
        if (userId == null) {
            ctx.status(401).json(Map.of("error", "please log in"));
            return;
        }

        StudySessionRequest request;
        try {
            request = ctx.bodyAsClass(StudySessionRequest.class);
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", "invalid request"));
            return;
        }

        String problem = checkSession(request);
        if (problem != null) {
            ctx.status(400).json(Map.of("error", problem));
            return;
        }

        try {
            // The course typed in has to exist in our catalogue.
            if (courseRepository.findCourseByName(request.course.trim()) == null) {
                ctx.status(400).json(Map.of("error", "that course doesn't exist"));
                return;
            }

            boolean saved = studySessionRepository.insertSession(userId, request);
            if (saved) {
                ctx.status(201).json(Map.of("message", "session added"));
            } else {
                ctx.status(500).json(Map.of("error", "could not add session"));
            }
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "something went wrong, try again"));
        }
    }

    // -----------------------------------------------------
    // PUT /study-sessions/{id}
    // -----------------------------------------------------
    public void update(Context ctx) {

        Integer userId = CurrentUser.id(ctx);
        if (userId == null) {
            ctx.status(401).json(Map.of("error", "please log in"));
            return;
        }

        Integer sessionId = readId(ctx);
        if (sessionId == null) {
            return;
        }

        StudySessionRequest request;
        try {
            request = ctx.bodyAsClass(StudySessionRequest.class);
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", "invalid request"));
            return;
        }

        String problem = checkSession(request);
        if (problem != null) {
            ctx.status(400).json(Map.of("error", problem));
            return;
        }

        try {
            // The course typed in has to exist in our catalogue.
            if (courseRepository.findCourseByName(request.course.trim()) == null) {
                ctx.status(400).json(Map.of("error", "that course doesn't exist"));
                return;
            }

            boolean saved = studySessionRepository.updateSession(sessionId, userId, request);
            if (saved) {
                ctx.json(Map.of("message", "session updated"));
            } else {
                ctx.status(404).json(Map.of("error", "session not found"));
            }
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "something went wrong, try again"));
        }
    }

    // -----------------------------------------------------
    // DELETE /study-sessions/{id}
    // -----------------------------------------------------
    public void delete(Context ctx) {

        Integer userId = CurrentUser.id(ctx);
        if (userId == null) {
            ctx.status(401).json(Map.of("error", "please log in"));
            return;
        }

        Integer sessionId = readId(ctx);
        if (sessionId == null) {
            return;
        }

        try {
            boolean deleted = studySessionRepository.deleteSession(sessionId, userId);
            if (deleted) {
                ctx.json(Map.of("message", "session deleted"));
            } else {
                ctx.status(404).json(Map.of("error", "session not found"));
            }
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "something went wrong, try again"));
        }
    }

    // -----------------------------------------------------
    // Helpers
    // -----------------------------------------------------

    // Checks a session the app sent. Gives back what is wrong, or null if it's fine.
    private String checkSession(StudySessionRequest request) {
        if (request.day == null || !DAYS.contains(request.day)) {
            return "day must be Monday to Sunday";
        }
        if (request.time == null || request.time.isBlank()) {
            return "time is required";
        }
        if (request.course == null || request.course.isBlank()) {
            return "course is required";
        }
        if (request.task == null || request.task.isBlank()) {
            return "task is required";
        }
        if (request.time.length() > 30 || request.course.length() > 255 || request.task.length() > 255) {
            return "one of the fields is too long";
        }
        return null;
    }

    // Reads the {id} from the address. Replies 400 and gives back null if it isn't a number.
    private Integer readId(Context ctx) {
        try {
            return Integer.parseInt(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            ctx.status(400).json(Map.of("error", "id must be a number"));
            return null;
        }
    }
}
