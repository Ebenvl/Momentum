package com.momentum.backend.db;

import com.momentum.backend.config.AppClock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// The learner's activity, for streaks and easter eggs:
// ActiveDays     the days that counted towards the streak, and why
// LearningMinutes minutes spent in each course on each day, seen by the sync
// UserEvents     things done in the app, like opening the timetable
public class ActivityRepository {

    private final Database database;

    public ActivityRepository(Database database) {
        this.database = database;
    }

    // Records that this day counted, from a check-in or from learning time.
    // INSERT IGNORE: the same day and reason is only saved once.
    public boolean saveActiveDay(int userId, LocalDate day, String source) throws SQLException {

        String sql = "INSERT IGNORE INTO ActiveDays (user_id, active_on, source) VALUES (?, ?, ?)";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setString(2, day.toString());
            statement.setString(3, source);
            return statement.executeUpdate() == 1;
        }
    }

    // Every day that counted for this learner, oldest first, like "2026-10-22".
    public List<String> findActiveDays(int userId) throws SQLException {

        String sql = "SELECT DISTINCT active_on FROM ActiveDays WHERE user_id = ? ORDER BY active_on";

        List<String> days = new ArrayList<>();

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);

            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    days.add(results.getString("active_on"));
                }
            }
        }

        return days;
    }

    // True if this day counted for this reason, like a check-in today.
    public boolean wasActive(int userId, LocalDate day, String source) throws SQLException {

        String sql = "SELECT user_id FROM ActiveDays WHERE user_id = ? AND active_on = ? AND source = ?";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setString(2, day.toString());
            statement.setString(3, source);

            try (ResultSet results = statement.executeQuery()) {
                return results.next();
            }
        }
    }

    // Adds minutes learned in a course today, and gives back today's total for it.
    public int addLearningMinutes(int userId, int courseId, LocalDate day, int minutes) throws SQLException {

        String add = "INSERT INTO LearningMinutes (user_id, course_id, learned_on, minutes) "
                   + "VALUES (?, ?, ?, ?) AS incoming "
                   + "ON DUPLICATE KEY UPDATE minutes = LearningMinutes.minutes + incoming.minutes";
        String total = "SELECT minutes FROM LearningMinutes WHERE user_id = ? AND course_id = ? AND learned_on = ?";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(add);
             PreparedStatement read = connection.prepareStatement(total)) {

            statement.setInt(1, userId);
            statement.setInt(2, courseId);
            statement.setString(3, day.toString());
            statement.setInt(4, minutes);
            statement.executeUpdate();

            read.setInt(1, userId);
            read.setInt(2, courseId);
            read.setString(3, day.toString());
            try (ResultSet results = read.executeQuery()) {
                return results.next() ? results.getInt(1) : 0;
            }
        }
    }

    // Records something done in the app, for the easter eggs.
    public boolean saveEvent(int userId, String event) throws SQLException {

        String sql = "INSERT INTO UserEvents (user_id, event, happened_on) VALUES (?, ?, ?)";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setString(2, event);
            statement.setString(3, AppClock.today().toString());
            return statement.executeUpdate() == 1;
        }
    }

    // True if the learner did this in the app on this day.
    public boolean didEvent(int userId, String event, LocalDate day) throws SQLException {

        String sql = "SELECT event_id FROM UserEvents WHERE user_id = ? AND event = ? AND happened_on = ?";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setString(2, event);
            statement.setString(3, day.toString());

            try (ResultSet results = statement.executeQuery()) {
                return results.next();
            }
        }
    }
}
