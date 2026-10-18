package com.momentum.backend.db;

import com.momentum.backend.model.Goal;
import com.momentum.backend.model.GoalRequest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Reads and saves goals in the Goals table.
//
// Every method that touches one goal also checks user_id, so a learner can
// only ever see or change their own goals, even if they guess another id.
public class GoalRepository {

    private final Database database;

    public GoalRepository(Database database) {
        this.database = database;
    }

    public boolean insertGoal(int userId, GoalRequest goal, boolean pointsEligible) throws SQLException {

        String sql = "INSERT INTO Goals (user_id, title, goal_type, target_value, target_course_id, "
                   + "target_date, points_eligible) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setString(2, goal.title);
            statement.setString(3, goal.goalType);
            statement.setObject(4, goal.targetValue);
            statement.setObject(5, goal.targetCourseId);
            statement.setString(6, goal.targetDate);
            statement.setBoolean(7, pointsEligible);
            return statement.executeUpdate() == 1;
        }
    }

    // Finds one of this user's goals. Gives back null if it does not exist
    // or belongs to someone else.
    public Goal findGoal(int goalId, int userId) throws SQLException {

        String sql = "SELECT goal_id, user_id, title, goal_type, target_value, target_course_id, "
                   + "target_date, status, points_eligible, created_at, completed_at "
                   + "FROM Goals WHERE goal_id = ? AND user_id = ?";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, goalId);
            statement.setInt(2, userId);

            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    return null;
                }

                Goal goal = new Goal();
                goal.goalId = results.getInt("goal_id");
                goal.userId = results.getInt("user_id");
                goal.title = results.getString("title");
                goal.goalType = results.getString("goal_type");
                goal.targetValue = results.getObject("target_value", Integer.class);
                goal.targetCourseId = results.getObject("target_course_id", Integer.class);
                goal.targetDate = results.getString("target_date");
                goal.status = results.getString("status");
                goal.pointsEligible = results.getBoolean("points_eligible");
                goal.createdAt = results.getString("created_at");
                goal.completedAt = results.getString("completed_at");
                return goal;
            }
        }
    }

    public boolean updateGoal(int goalId, int userId, GoalRequest goal, boolean pointsEligible) throws SQLException {

        String sql = "UPDATE Goals SET title = ?, goal_type = ?, target_value = ?, target_course_id = ?, "
                   + "target_date = ?, points_eligible = ? WHERE goal_id = ? AND user_id = ?";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, goal.title);
            statement.setString(2, goal.goalType);
            statement.setObject(3, goal.targetValue);
            statement.setObject(4, goal.targetCourseId);
            statement.setString(5, goal.targetDate);
            statement.setBoolean(6, pointsEligible);
            statement.setInt(7, goalId);
            statement.setInt(8, userId);
            return statement.executeUpdate() == 1;
        }
    }

    // Changes a goal's status, for example to cancelled or completed.
    // completed_at is filled in only when the status is completed.
    public boolean updateGoalStatus(int goalId, int userId, String status) throws SQLException {

        String sql = "UPDATE Goals SET status = ?, "
                   + "completed_at = IF(? = 'completed', NOW(), NULL) "
                   + "WHERE goal_id = ? AND user_id = ?";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, status);
            statement.setString(2, status);
            statement.setInt(3, goalId);
            statement.setInt(4, userId);
            return statement.executeUpdate() == 1;
        }
    }

    // Deletes one of this user's goals. Its check-ins go with it (ON DELETE CASCADE).
    public boolean deleteGoal(int goalId, int userId) throws SQLException {

        String sql = "DELETE FROM Goals WHERE goal_id = ? AND user_id = ?";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, goalId);
            statement.setInt(2, userId);
            return statement.executeUpdate() == 1;
        }
    }

    public List<Goal> findGoalsForUser(int userId) throws SQLException {

        String sql = "SELECT goal_id, user_id, title, goal_type, target_value, target_course_id, "
                + "target_date, status, points_eligible, created_at, completed_at "
                + "FROM Goals WHERE user_id = ? ORDER BY created_at DESC";

        List<Goal> goals = new ArrayList<>();

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);

            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    Goal goal = new Goal();
                    goal.goalId = results.getInt("goal_id");
                    goal.userId = results.getInt("user_id");
                    goal.title = results.getString("title");
                    goal.goalType = results.getString("goal_type");
                    goal.targetValue = results.getObject("target_value", Integer.class);
                    goal.targetCourseId = results.getObject("target_course_id", Integer.class);
                    goal.targetDate = results.getString("target_date");
                    goal.status = results.getString("status");
                    goal.pointsEligible = results.getBoolean("points_eligible");
                    goal.createdAt = results.getString("created_at");
                    goal.completedAt = results.getString("completed_at");
                    goals.add(goal);
                }
            }
        }

        return goals;
    }

}
