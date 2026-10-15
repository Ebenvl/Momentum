package com.momentum.backend.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

// Saves points awards in the PointsTransactions table.
public class PointsRepository {

    private final Database database;

    public PointsRepository(Database database) {
        this.database = database;
    }

    // Records a points award. INSERT IGNORE skips it quietly if the same award
    // already exists (unique key on user_id + reference_id + reason), so a
    // repeated sync can never award the same completion twice.
    // Gives back true only if the points are new.
    //
    // reason is for example "course_completed" or "badge_awarded", and
    // referenceId is the course_id or badge_id it was for.
    // monthKey is the month it counts towards, like "2026-10" (leaderboard).
    public boolean awardPoints(int userId, int points, String reason, Integer referenceId,
                               String monthKey) throws SQLException {

        String sql = "INSERT IGNORE INTO PointsTransactions (user_id, points, reason, reference_id, month_key) "
                   + "VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setInt(2, points);
            statement.setString(3, reason);
            statement.setObject(4, referenceId);
            statement.setString(5, monthKey);
            return statement.executeUpdate() == 1;
        }
    }

}
