package com.momentum.backend.db;

import com.momentum.backend.model.ActivityLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Saves entries in the ActivityLogs table.
public class ActivityLogRepository {

    private final Database database;

    public ActivityLogRepository(Database database) {
        this.database = database;
    }

    // userId can be null for things not tied to one user, like the scheduled sync.
    // level is "info", "warning" or "error".
    public boolean log(Integer userId, String action, String level) throws SQLException {

        // The action column holds at most 100 characters, so longer text is cut short.
        if (action.length() > 100) {
            action = action.substring(0, 100);
        }

        String sql = "INSERT INTO ActivityLogs (user_id, action, level) VALUES (?, ?, ?)";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, userId);
            statement.setString(2, action);
            statement.setString(3, level);
            return statement.executeUpdate() == 1;
        }
    }


    // The latest 200 log entries, newest first, for the admin's log screen.
    public List<ActivityLog> findRecentLogs() throws SQLException {

        String sql = "SELECT log_id, user_id, action, level, created_at "
                   + "FROM ActivityLogs ORDER BY created_at DESC LIMIT 200";

        List<ActivityLog> logs = new ArrayList<>();

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {

            while (results.next()) {
                ActivityLog log = new ActivityLog();
                log.logId = results.getInt("log_id");
                log.userId = results.getObject("user_id", Integer.class);
                log.action = results.getString("action");
                log.level = results.getString("level");
                log.createdAt = results.getString("created_at");
                logs.add(log);
            }
        }

        return logs;
    }
}
