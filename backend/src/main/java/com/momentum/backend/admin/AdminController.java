package com.momentum.backend.admin;

import com.momentum.backend.auth.CurrentUser;
import com.momentum.backend.db.ActivityLogRepository;
import com.momentum.backend.db.BadgeRepository;
import com.momentum.backend.db.CourseRepository;
import com.momentum.backend.db.UserRepository;
import com.momentum.backend.model.Badge;
import com.momentum.backend.model.BadgeRequest;
import com.momentum.backend.model.Course;
import com.momentum.backend.model.CourseUpdateRequest;
import com.momentum.backend.model.LinkRequest;
import com.momentum.backend.model.StatusRequest;
import com.momentum.backend.model.User;
import io.javalin.http.Context;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

// Handles the administrator's screens: users, courses and badges.
// Every method first checks the person is logged in and is an administrator.
public class AdminController {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final BadgeRepository badgeRepository;
    private final ActivityLogRepository activityLogRepository;

    public AdminController(UserRepository userRepository, CourseRepository courseRepository,
                           BadgeRepository badgeRepository, ActivityLogRepository activityLogRepository) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.badgeRepository = badgeRepository;
        this.activityLogRepository = activityLogRepository;
    }

    // =====================================================
    // Users
    // =====================================================

    // -----------------------------------------------------
    // PUT /admin/users/{id}/status   suspend or reactivate
    // -----------------------------------------------------
    public void setUserStatus(Context ctx) {

        if (!requireAdmin(ctx)) {
            return;
        }

        Integer userId = readId(ctx);
        if (userId == null) {
            return;
        }

        StatusRequest request;
        try {
            request = ctx.bodyAsClass(StatusRequest.class);
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", "invalid request"));
            return;
        }

        if (request.status == null || !List.of("active", "suspended").contains(request.status)) {
            ctx.status(400).json(Map.of("error", "status must be active or suspended"));
            return;
        }

        // An admin can't lock themselves out by accident.
        if (userId.equals(CurrentUser.id(ctx))) {
            ctx.status(400).json(Map.of("error", "you can't change your own status"));
            return;
        }

        try {
            boolean saved = userRepository.updateStatus(userId, request.status);
            if (saved) {
                ctx.json(Map.of("message", "user " + request.status));
            } else {
                ctx.status(404).json(Map.of("error", "user not found"));
            }
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "something went wrong, try again"));
        }
    }

    // -----------------------------------------------------
    // PUT /admin/users/{id}/link   link to a LearnTech learner
    // Send {"idst": null} to unlink.
    // -----------------------------------------------------
    public void linkUser(Context ctx) {

        if (!requireAdmin(ctx)) {
            return;
        }

        Integer userId = readId(ctx);
        if (userId == null) {
            return;
        }

        LinkRequest request;
        try {
            request = ctx.bodyAsClass(LinkRequest.class);
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", "invalid request"));
            return;
        }

        try {
            if (request.idst != null && userRepository.idstTaken(request.idst)) {
                ctx.status(409).json(Map.of("error", "that learner is already linked to another account"));
                return;
            }

            boolean saved = userRepository.updateIdst(userId, request.idst);
            if (saved) {
                ctx.json(Map.of("message", request.idst == null ? "user unlinked" : "user linked"));
            } else {
                ctx.status(404).json(Map.of("error", "user not found"));
            }
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "something went wrong, try again"));
        }
    }

    // -----------------------------------------------------
    // DELETE /admin/users/{id}   remove an account
    // -----------------------------------------------------
    public void deleteUser(Context ctx) {

        if (!requireAdmin(ctx)) {
            return;
        }

        Integer userId = readId(ctx);
        if (userId == null) {
            return;
        }

        if (userId.equals(CurrentUser.id(ctx))) {
            ctx.status(400).json(Map.of("error", "you can't delete your own account"));
            return;
        }

        try {
            boolean deleted = userRepository.deleteUser(userId);
            if (deleted) {
                ctx.json(Map.of("message", "user deleted"));
            } else {
                ctx.status(404).json(Map.of("error", "user not found"));
            }
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "something went wrong, try again"));
        }
    }

    // -----------------------------------------------------
    // GET /admin/users   every account
    // -----------------------------------------------------
    public void getUsers(Context ctx) {

        if (!requireAdmin(ctx)) {
            return;
        }

        try {
            ctx.json(userRepository.findAllUsers());
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "something went wrong, try again"));
        }
    }

    // -----------------------------------------------------
    // GET /admin/users/{id}
    // -----------------------------------------------------
    public void getUser(Context ctx) {

        if (!requireAdmin(ctx)) {
            return;
        }

        Integer userId = readId(ctx);
        if (userId == null) {
            return;
        }

        try {
            User user = userRepository.findUserById(userId);
            if (user == null) {
                ctx.status(404).json(Map.of("error", "user not found"));
                return;
            }
            user.passwordHash = null;
            ctx.json(user);
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "something went wrong, try again"));
        }
    }

    // =====================================================
    // Courses
    // =====================================================

    // -----------------------------------------------------
    // GET /admin/courses   the whole course catalogue
    // -----------------------------------------------------
    public void getCourses(Context ctx) {

        if (!requireAdmin(ctx)) {
            return;
        }

        try {
            ctx.json(courseRepository.findAllCourses());
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "something went wrong, try again"));
        }
    }

    // -----------------------------------------------------
    // GET /admin/courses/{id}
    // -----------------------------------------------------
    public void getCourse(Context ctx) {

        if (!requireAdmin(ctx)) {
            return;
        }

        Integer courseId = readId(ctx);
        if (courseId == null) {
            return;
        }

        try {
            Course course = courseRepository.findCourseById(courseId);
            if (course == null) {
                ctx.status(404).json(Map.of("error", "course not found"));
                return;
            }
            ctx.json(course);
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "something went wrong, try again"));
        }
    }

    // -----------------------------------------------------
    // PUT /admin/courses/{id}
    // Points override and expected duration.
    // Either can be null, which clears it.
    // -----------------------------------------------------
    public void updateCourse(Context ctx) {

        if (!requireAdmin(ctx)) {
            return;
        }

        Integer courseId = readId(ctx);
        if (courseId == null) {
            return;
        }

        CourseUpdateRequest request;
        try {
            request = ctx.bodyAsClass(CourseUpdateRequest.class);
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", "invalid request"));
            return;
        }

        if (request.pointsValue != null && request.pointsValue < 0) {
            ctx.status(400).json(Map.of("error", "points can't be negative"));
            return;
        }
        if (request.expectedMinutes != null && request.expectedMinutes <= 0) {
            ctx.status(400).json(Map.of("error", "expected minutes must be more than 0"));
            return;
        }

        try {
            boolean found = courseRepository.updatePointsValue(courseId, request.pointsValue);
            if (!found) {
                ctx.status(404).json(Map.of("error", "course not found"));
                return;
            }
            courseRepository.updateExpectedMinutes(courseId, request.expectedMinutes);
            ctx.json(Map.of("message", "course updated"));
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "something went wrong, try again"));
        }
    }

    // =====================================================
    // Badges
    // =====================================================

    // -----------------------------------------------------
    // POST /admin/badges
    // -----------------------------------------------------
    public void createBadge(Context ctx) {

        if (!requireAdmin(ctx)) {
            return;
        }

        BadgeRequest request;
        try {
            request = ctx.bodyAsClass(BadgeRequest.class);
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", "invalid request"));
            return;
        }

        String problem = checkBadge(request);
        if (problem != null) {
            ctx.status(400).json(Map.of("error", problem));
            return;
        }

        try {
            boolean saved = badgeRepository.insertBadge(request);
            if (saved) {
                ctx.status(201).json(Map.of("message", "badge created"));
            } else {
                ctx.status(500).json(Map.of("error", "could not create badge"));
            }
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "something went wrong, try again"));
        }
    }

    // -----------------------------------------------------
    // GET /admin/badges   every badge
    // -----------------------------------------------------
    public void getBadges(Context ctx) {

        if (!requireAdmin(ctx)) {
            return;
        }

        try {
            ctx.json(badgeRepository.findAllBadges());
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "something went wrong, try again"));
        }
    }

    // -----------------------------------------------------
    // GET /admin/logs   the latest activity and errors
    // -----------------------------------------------------
    public void getLogs(Context ctx) {

        if (!requireAdmin(ctx)) {
            return;
        }

        try {
            ctx.json(activityLogRepository.findRecentLogs());
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "something went wrong, try again"));
        }
    }

    // -----------------------------------------------------
    // GET /admin/badges/{id}
    // -----------------------------------------------------
    public void getBadge(Context ctx) {

        if (!requireAdmin(ctx)) {
            return;
        }

        Integer badgeId = readId(ctx);
        if (badgeId == null) {
            return;
        }

        try {
            Badge badge = badgeRepository.findBadgeById(badgeId);
            if (badge == null) {
                ctx.status(404).json(Map.of("error", "badge not found"));
                return;
            }
            ctx.json(badge);
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "something went wrong, try again"));
        }
    }

    // -----------------------------------------------------
    // PUT /admin/badges/{id}
    // -----------------------------------------------------
    public void updateBadge(Context ctx) {

        if (!requireAdmin(ctx)) {
            return;
        }

        Integer badgeId = readId(ctx);
        if (badgeId == null) {
            return;
        }

        BadgeRequest request;
        try {
            request = ctx.bodyAsClass(BadgeRequest.class);
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", "invalid request"));
            return;
        }

        String problem = checkBadge(request);
        if (problem != null) {
            ctx.status(400).json(Map.of("error", problem));
            return;
        }

        try {
            boolean saved = badgeRepository.updateBadge(badgeId, request);
            if (saved) {
                ctx.json(Map.of("message", "badge updated"));
            } else {
                ctx.status(404).json(Map.of("error", "badge not found"));
            }
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "something went wrong, try again"));
        }
    }

    // -----------------------------------------------------
    // DELETE /admin/badges/{id}
    // -----------------------------------------------------
    public void deleteBadge(Context ctx) {

        if (!requireAdmin(ctx)) {
            return;
        }

        Integer badgeId = readId(ctx);
        if (badgeId == null) {
            return;
        }

        try {
            boolean deleted = badgeRepository.deleteBadge(badgeId);
            if (deleted) {
                ctx.json(Map.of("message", "badge deleted"));
            } else {
                ctx.status(404).json(Map.of("error", "badge not found"));
            }
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "something went wrong, try again"));
        }
    }

    // =====================================================
    // Helpers
    // =====================================================

    // Replies 401 if nobody is logged in, or 403 if they are not an admin.
    // Gives back true only if the request may carry on.
    private boolean requireAdmin(Context ctx) {
        if (CurrentUser.id(ctx) == null) {
            ctx.status(401).json(Map.of("error", "please log in"));
            return false;
        }
        if (!CurrentUser.isAdmin(ctx)) {
            ctx.status(403).json(Map.of("error", "administrators only"));
            return false;
        }
        return true;
    }

    // Checks a badge the admin sent, and fills in defaults for anything left out.
    // Gives back what is wrong, or null if it's fine.
    private String checkBadge(BadgeRequest request) {
        if (request.name == null || request.name.isBlank()) {
            return "badge name is required";
        }
        if (request.ruleValue == null) {
            request.ruleValue = 0;
        }
        if (request.pointsAwarded == null) {
            request.pointsAwarded = 0;
        }
        if (request.isActive == null) {
            request.isActive = true;
        }
        if (request.ruleValue < 0 || request.pointsAwarded < 0) {
            return "rule value and points can't be negative";
        }
        return null;
    }

    // Reads the {id} from the address, like /admin/users/5.
    // Replies 400 and gives back null if it isn't a number.
    private Integer readId(Context ctx) {
        try {
            return Integer.parseInt(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            ctx.status(400).json(Map.of("error", "id must be a number"));
            return null;
        }
    }
}
