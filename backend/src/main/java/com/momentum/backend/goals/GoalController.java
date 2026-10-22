package com.momentum.backend.goals;

import com.momentum.backend.auth.CurrentUser;
import com.momentum.backend.db.CheckInRepository;
import com.momentum.backend.db.CourseRepository;
import com.momentum.backend.db.GoalRepository;
import com.momentum.backend.db.StreakRepository;
import com.momentum.backend.model.CheckInRequest;
import com.momentum.backend.model.Goal;
import com.momentum.backend.model.GoalRequest;
import com.momentum.backend.model.StatusRequest;
import com.momentum.backend.model.Streak;
import com.momentum.backend.rewards.BadgeService;
import com.momentum.backend.rewards.GoalChecker;
import io.javalin.http.Context;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// Handles goals and check-ins.
public class GoalController {

    // The five goal types the Goals table allows.
    private static final List<String> GOAL_TYPES = List.of(
        "complete_n_courses", "complete_specific_course", "spend_minutes", "active_days", "free_text");

    private final GoalRepository goalRepository;
    private final CheckInRepository checkInRepository;
    private final CourseRepository courseRepository;
    private final StreakRepository streakRepository;
    private final GoalChecker goalChecker;
    private final BadgeService badgeService;

    public GoalController(GoalRepository goalRepository, CheckInRepository checkInRepository,
                          CourseRepository courseRepository, StreakRepository streakRepository,
                          GoalChecker goalChecker, BadgeService badgeService) {
        this.goalRepository = goalRepository;
        this.checkInRepository = checkInRepository;
        this.courseRepository = courseRepository;
        this.streakRepository = streakRepository;
        this.goalChecker = goalChecker;
        this.badgeService = badgeService;
    }

    // -----------------------------------------------------
    // POST /goals
    // -----------------------------------------------------
    public void create(Context ctx) {

        Integer userId = CurrentUser.id(ctx);
        if (userId == null) {
            ctx.status(401).json(Map.of("error", "please log in"));
            return;
        }

        GoalRequest request;
        try {
            request = ctx.bodyAsClass(GoalRequest.class);
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", "invalid request"));
            return;
        }

        String problem = checkGoal(request);
        if (problem != null) {
            ctx.status(400).json(Map.of("error", problem));
            return;
        }

        try {
            // Free text goals carry no points, the app can't check them.
            // The course picked for the goal has to exist in our catalogue.
            if (request.targetCourseId != null
                    && courseRepository.findCourseById(request.targetCourseId) == null) {
                ctx.status(400).json(Map.of("error", "that course doesn't exist"));
                return;
            }

            boolean pointsEligible = !request.goalType.equals("free_text");

            boolean saved = goalRepository.insertGoal(userId, request, pointsEligible);
            if (saved) {
                ctx.status(201).json(Map.of("message", "goal created"));
            } else {
                ctx.status(500).json(Map.of("error", "could not create goal"));
            }
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "something went wrong, try again"));
        }
    }

    // -----------------------------------------------------
    // GET /goals   all of the learner's goals, newest first
    // -----------------------------------------------------
    public void getAll(Context ctx) {

        Integer userId = CurrentUser.id(ctx);
        if (userId == null) {
            ctx.status(401).json(Map.of("error", "please log in"));
            return;
        }

        try {
            ctx.json(goalRepository.findGoalsForUser(userId));
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "something went wrong, try again"));
        }
    }

    // -----------------------------------------------------
    // GET /goals/{id}
    // -----------------------------------------------------
    public void getOne(Context ctx) {

        Integer userId = CurrentUser.id(ctx);
        if (userId == null) {
            ctx.status(401).json(Map.of("error", "please log in"));
            return;
        }

        Integer goalId = readId(ctx);
        if (goalId == null) {
            return;
        }

        try {
            Goal goal = goalRepository.findGoal(goalId, userId);
            if (goal == null) {
                ctx.status(404).json(Map.of("error", "goal not found"));
                return;
            }
            ctx.json(goal);
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "something went wrong, try again"));
        }
    }

    // -----------------------------------------------------
    // PUT /goals/{id}
    // -----------------------------------------------------
    public void update(Context ctx) {

        Integer userId = CurrentUser.id(ctx);
        if (userId == null) {
            ctx.status(401).json(Map.of("error", "please log in"));
            return;
        }

        Integer goalId = readId(ctx);
        if (goalId == null) {
            return;
        }

        GoalRequest request;
        try {
            request = ctx.bodyAsClass(GoalRequest.class);
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", "invalid request"));
            return;
        }

        String problem = checkGoal(request);
        if (problem != null) {
            ctx.status(400).json(Map.of("error", problem));
            return;
        }

        try {
            // The course picked for the goal has to exist in our catalogue.
            if (request.targetCourseId != null
                    && courseRepository.findCourseById(request.targetCourseId) == null) {
                ctx.status(400).json(Map.of("error", "that course doesn't exist"));
                return;
            }

            boolean pointsEligible = !request.goalType.equals("free_text");

            boolean saved = goalRepository.updateGoal(goalId, userId, request, pointsEligible);
            if (saved) {
                ctx.json(Map.of("message", "goal updated"));
            } else {
                ctx.status(404).json(Map.of("error", "goal not found"));
            }
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "something went wrong, try again"));
        }
    }

    // -----------------------------------------------------
    // PUT /goals/{id}/status
    // A learner can cancel a goal, or reopen it. They can only mark a goal
    // completed themselves if it is free text, because every other type is
    // checked by the system from their LearnTech progress.
    // -----------------------------------------------------
    public void updateStatus(Context ctx) {

        Integer userId = CurrentUser.id(ctx);
        if (userId == null) {
            ctx.status(401).json(Map.of("error", "please log in"));
            return;
        }

        Integer goalId = readId(ctx);
        if (goalId == null) {
            return;
        }

        StatusRequest request;
        try {
            request = ctx.bodyAsClass(StatusRequest.class);
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", "invalid request"));
            return;
        }

        if (request.status == null || !List.of("active", "cancelled", "completed").contains(request.status)) {
            ctx.status(400).json(Map.of("error", "status must be active, cancelled or completed"));
            return;
        }

        try {
            Goal goal = goalRepository.findGoal(goalId, userId);
            if (goal == null) {
                ctx.status(404).json(Map.of("error", "goal not found"));
                return;
            }

            if (request.status.equals("completed") && !goal.goalType.equals("free_text")) {
                ctx.status(400).json(Map.of("error", "this goal is completed automatically from your courses"));
                return;
            }

            goalRepository.updateGoalStatus(goalId, userId, request.status);
            ctx.json(Map.of("message", "goal " + request.status));
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "something went wrong, try again"));
        }
    }

    // -----------------------------------------------------
    // DELETE /goals/{id}
    // -----------------------------------------------------
    public void delete(Context ctx) {

        Integer userId = CurrentUser.id(ctx);
        if (userId == null) {
            ctx.status(401).json(Map.of("error", "please log in"));
            return;
        }

        Integer goalId = readId(ctx);
        if (goalId == null) {
            return;
        }

        try {
            boolean deleted = goalRepository.deleteGoal(goalId, userId);
            if (deleted) {
                ctx.json(Map.of("message", "goal deleted"));
            } else {
                ctx.status(404).json(Map.of("error", "goal not found"));
            }
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "something went wrong, try again"));
        }
    }

    // -----------------------------------------------------
    // POST /goals/{id}/checkins
    // -----------------------------------------------------
    public void checkIn(Context ctx) {

        Integer userId = CurrentUser.id(ctx);
        if (userId == null) {
            ctx.status(401).json(Map.of("error", "please log in"));
            return;
        }

        Integer goalId = readId(ctx);
        if (goalId == null) {
            return;
        }

        CheckInRequest request;
        try {
            request = ctx.bodyAsClass(CheckInRequest.class);
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", "invalid request"));
            return;
        }

        if (request.note != null && request.note.length() > 1000) {
            ctx.status(400).json(Map.of("error", "note can be at most 1000 characters"));
            return;
        }
        if (request.valueLogged != null && request.valueLogged < 0) {
            ctx.status(400).json(Map.of("error", "value can't be negative"));
            return;
        }

        try {
            Goal goal = goalRepository.findGoal(goalId, userId);
            if (goal == null) {
                ctx.status(404).json(Map.of("error", "goal not found"));
                return;
            }
            if (!goal.status.equals("active")) {
                ctx.status(400).json(Map.of("error", "you can only check in on an active goal"));
                return;
            }

            boolean saved = checkInRepository.insertCheckIn(goalId, userId, request.note, request.valueLogged);
            if (saved) {
                updateStreak(userId);
                goalChecker.checkGoals(userId);
                badgeService.checkBadges(userId);
                ctx.status(201).json(Map.of("message", "check-in logged"));
            } else {
                ctx.status(500).json(Map.of("error", "could not log check-in"));
            }
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "something went wrong, try again"));
        }
    }

    // -----------------------------------------------------
    // GET /goals/{id}/checkins   the check-ins on one goal, newest first
    // -----------------------------------------------------
    public void getCheckIns(Context ctx) {

        Integer userId = CurrentUser.id(ctx);
        if (userId == null) {
            ctx.status(401).json(Map.of("error", "please log in"));
            return;
        }

        Integer goalId = readId(ctx);
        if (goalId == null) {
            return;
        }

        try {
            if (goalRepository.findGoal(goalId, userId) == null) {
                ctx.status(404).json(Map.of("error", "goal not found"));
                return;
            }
            ctx.json(checkInRepository.findCheckInsForGoal(goalId, userId));
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "something went wrong, try again"));
        }
    }

    // -----------------------------------------------------
    // Helpers
    // -----------------------------------------------------

    // Checks a goal the app sent. Gives back what is wrong, or null if it's fine.
    private String checkGoal(GoalRequest request) {
        if (request.title == null || request.title.isBlank()) {
            return "title is required";
        }
        if (request.title.length() > 255) {
            return "title can be at most 255 characters";
        }
        if (request.goalType == null || !GOAL_TYPES.contains(request.goalType)) {
            return "goal type is not valid";
        }
        if (request.goalType.equals("complete_specific_course") && request.targetCourseId == null) {
            return "pick a course for this goal";
        }
        if (!request.goalType.equals("free_text") && !request.goalType.equals("complete_specific_course")
                && (request.targetValue == null || request.targetValue <= 0)) {
            return "target must be more than 0";
        }
        return null;
    }

    // Reads the {id} from the address, like /goals/5. Replies 400 and gives back
    // null if it isn't a number.
    private Integer readId(Context ctx) {
        try {
            return Integer.parseInt(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            ctx.status(400).json(Map.of("error", "id must be a number"));
            return null;
        }
    }

    // Updates the learner's streak after a check-in.
    private void updateStreak(int userId) throws SQLException {

        LocalDate today = LocalDate.now();
        Streak streak = streakRepository.findStreak(userId);

        int current;
        int longest;

        if (streak == null || streak.lastActivityDate == null) {
            current = 1;
            longest = 1;
        } else {
            LocalDate lastDay = LocalDate.parse(streak.lastActivityDate);

            if (lastDay.equals(today)) {
                return;
            } else if (lastDay.equals(today.minusDays(1))) {
                current = streak.currentStreak + 1;
            } else {
                current = 1;
            }

            longest = Math.max(streak.longestStreak, current);
        }

        streakRepository.saveStreak(userId, current, longest, today.toString());
    }
}
