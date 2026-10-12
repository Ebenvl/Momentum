package com.momentum.backend.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

// Saves entries in the ActivityLogs table (FR-17, NFR-10).
public class ActivityLogRepository {

    private final Database database;

    public ActivityLogRepository(Database database) {
        this.database = database;
    }

    // userId can be null for things not tied to one user, like the scheduled sync.
    // level is "info", "warning" or "error".
    public boolean log(Integer userId, String action, String level) throws SQLException {

        String sql = "INSERT INTO ActivityLogs (user_id, action, level) VALUES (?, ?, ?)";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, userId);
            statement.setString(2, action);
            statement.setString(3, level);
            return statement.executeUpdate() == 1;
        }
    }

}
