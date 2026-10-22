package com.momentum.backend.rewards;

import com.momentum.backend.config.AppClock;
import com.momentum.backend.db.ActivityLogRepository;
import com.momentum.backend.db.EnrolmentRepository;
import com.momentum.backend.db.GoalRepository;
import com.momentum.backend.db.PointsRepository;
import com.momentum.backend.db.StreakRepository;
import com.momentum.backend.model.Goal;

import java.sql.SQLException;
import java.time.LocalDate;

// Completes a learner's goals by itself when their data shows the goal is met,
// and marks goals failed when the target date passes first.
// Runs after every sync of a learner and after every check-in.
public class GoalChecker {

    // Points for a completed goal. Free text goals never get points.
    private static final int GOAL_POINTS = 10;

    private final GoalRepository goalRepository;
    private final EnrolmentRepository enrolmentRepository;
    private final StreakRepository streakRepository;
    private final PointsRepository pointsRepository;
    private final ActivityLogRepository activityLogRepository;

    public GoalChecker(GoalRepository goalRepository, EnrolmentRepository enrolmentRepository,
                       StreakRepository streakRepository, PointsRepository pointsRepository,
                       ActivityLogRepository activityLogRepository) {
        this.goalRepository = goalRepository;
        this.enrolmentRepository = enrolmentRepository;
        this.streakRepository = streakRepository;
        this.pointsRepository = pointsRepository;
        this.activityLogRepository = activityLogRepository;
    }

    // Checks every active goal of this learner.
    public void checkGoals(int userId) throws SQLException {

        for (Goal goal : goalRepository.findGoalsForUser(userId)) {

            if (!"active".equals(goal.status) || "free_text".equals(goal.goalType)) {
                continue;
            }

            boolean reached;
            if ("complete_specific_course".equals(goal.goalType)) {
                reached = goal.targetCourseId != null
                        && enrolmentRepository.isCompleted(userId, goal.targetCourseId);
            } else if ("complete_n_courses".equals(goal.goalType)) {
                reached = enrolmentRepository.countCompleted(userId) >= goal.targetValue;
            } else if ("spend_minutes".equals(goal.goalType)) {
                reached = enrolmentRepository.totalMinutes(userId) >= goal.targetValue;
            } else if ("active_days".equals(goal.goalType)) {
                reached = streakRepository.activeStreak(userId) >= goal.targetValue;
            } else {
                continue;
            }

            if (reached) {
                goalRepository.updateGoalStatus(goal.goalId, userId, "completed");
                if (goal.pointsEligible) {
                    pointsRepository.awardPoints(userId, GOAL_POINTS, "goal_completed",
                            goal.goalId, AppClock.today().toString().substring(0, 7));
                }
                activityLogRepository.log(userId, "goal completed: " + goal.title, "info");
            } else if (goal.targetDate != null
                    && LocalDate.parse(goal.targetDate).isBefore(AppClock.today())) {
                goalRepository.updateGoalStatus(goal.goalId, userId, "failed");
                activityLogRepository.log(userId, "goal failed: " + goal.title, "info");
            }

        }

    }
}
