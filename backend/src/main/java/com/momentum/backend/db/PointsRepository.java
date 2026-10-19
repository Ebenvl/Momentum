package com.momentum.backend.db;

import com.momentum.backend.model.LeaderboardEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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

    // This month's leaderboard: everyone's points added up, highest first.
    public List<LeaderboardEntry> findLeaderboard(String monthKey) throws SQLException {

        String sql = "SELECT Users.user_id, Users.full_name, SUM(PointsTransactions.points) AS points "
                   + "FROM PointsTransactions "
                   + "JOIN Users ON Users.user_id = PointsTransactions.user_id "
                   + "WHERE PointsTransactions.month_key = ? "
                   + "GROUP BY Users.user_id, Users.full_name "
                   + "ORDER BY points DESC";

        List<LeaderboardEntry> entries = new ArrayList<>();

        int rank = 0;

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, monthKey);

            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    rank++;
                    LeaderboardEntry entry = new LeaderboardEntry();
                    entry.rank = rank;
                    entry.userId = results.getInt("user_id");
                    entry.fullName = results.getString("full_name");
                    entry.points = results.getInt("points");
                    entries.add(entry);
                }
            }
        }

        return entries;
    }

    // A learner's points from every month added up, for their profile.
    public int totalPoints(int userId) throws SQLException {

        String sql = "SELECT COALESCE(SUM(points), 0) FROM PointsTransactions WHERE user_id = ?";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);

            try (ResultSet results = statement.executeQuery()) {
                results.next();
                return results.getInt(1);
            }
        }
    }

}
