package com.momentum.backend.db;

import com.momentum.backend.model.AdminStats;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// Works out the numbers for the admin dashboard.
public class StatsRepository {

    private final Database database;

    public StatsRepository(Database database) {
        this.database = database;
    }

    public AdminStats getAdminStats() throws SQLException {

        AdminStats stats = new AdminStats();
        stats.totalUsers = count("SELECT COUNT(*) FROM Users");
        stats.activeCourses = count("SELECT COUNT(*) FROM Courses");

        int enrolments = count("SELECT COUNT(*) FROM Enrolments");
        int completed = count("SELECT COUNT(*) FROM Enrolments WHERE state = 'completed'");
        stats.completionRate = enrolments > 0 ? Math.round(completed * 100f / enrolments) : 0;

        int learners = count("SELECT COUNT(*) FROM Users WHERE role = 'learner'");
        int activeLearners = count("SELECT COUNT(*) FROM Users WHERE role = 'learner' "
                + "AND last_login_at >= NOW() - INTERVAL 7 DAY");
        stats.engagement = learners > 0 ? Math.round(activeLearners * 100f / learners) : 0;

        return stats;
    }

    // Runs a query that gives back one number.
    private int count(String sql) throws SQLException {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {

            return results.next() ? results.getInt(1) : 0;
        }
    }
}
