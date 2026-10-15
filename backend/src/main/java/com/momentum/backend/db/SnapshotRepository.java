package com.momentum.backend.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

// Saves progress snapshots in the ProgressSnapshots table.
// The sync writes one per learner every time it runs, because LearnTech keeps
// no history, so this table is the only history there is.
public class SnapshotRepository {

    private final Database database;

    public SnapshotRepository(Database database) {
        this.database = database;
    }

    public boolean insertSnapshot(int userId, int coursesEnrolled, int coursesCompleted,
                                  int totalMinutes, int totalPoints, int currentStreak) throws SQLException {

        String sql = "INSERT INTO ProgressSnapshots (user_id, courses_enrolled, courses_completed, "
                   + "total_minutes, total_points, current_streak) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setInt(2, coursesEnrolled);
            statement.setInt(3, coursesCompleted);
            statement.setInt(4, totalMinutes);
            statement.setInt(5, totalPoints);
            statement.setInt(6, currentStreak);
            return statement.executeUpdate() == 1;
        }
    }

}
