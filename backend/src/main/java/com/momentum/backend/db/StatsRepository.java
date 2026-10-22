package com.momentum.backend.db;

import com.momentum.backend.model.AdminStats;
import com.momentum.backend.model.CourseStat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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
        stats.activeLearners = activeLearners;
        stats.totalMinutes = count("SELECT COALESCE(SUM(time_spent_minutes), 0) FROM Enrolments");

        return stats;
    }

    // For each course: how many app learners are enrolled, and how many completed it.
    // Courses nobody is on are left out. Most enrolled first.
    public List<CourseStat> getCourseStats() throws SQLException {

        String sql = "SELECT Courses.course_name, COUNT(*) AS enrolled, "
                   + "SUM(Enrolments.state = 'completed') AS completed "
                   + "FROM Enrolments JOIN Courses ON Courses.course_id = Enrolments.course_id "
                   + "GROUP BY Courses.course_id, Courses.course_name "
                   + "ORDER BY enrolled DESC, Courses.course_name";

        List<CourseStat> courseStats = new ArrayList<>();

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {

            while (results.next()) {
                CourseStat courseStat = new CourseStat();
                courseStat.courseName = results.getString("course_name");
                courseStat.enrolled = results.getInt("enrolled");
                courseStat.completed = results.getInt("completed");
                courseStats.add(courseStat);
            }
        }

        return courseStats;
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
