package com.momentum.backend.db;

import com.momentum.backend.config.AppClock;
import com.momentum.backend.model.Streak;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

// Reads and saves streaks in the Streaks table. One row per learner.
public class StreakRepository {

    private final Database database;

    public StreakRepository(Database database) {
        this.database = database;
    }

    // Gives back null if the learner has no streak row yet.
    public Streak findStreak(int userId) throws SQLException {

        String sql = "SELECT user_id, current_streak, longest_streak, last_activity_date "
                   + "FROM Streaks WHERE user_id = ?";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);

            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    return null;
                }

                Streak streak = new Streak();
                streak.userId = results.getInt("user_id");
                streak.currentStreak = results.getInt("current_streak");
                streak.longestStreak = results.getInt("longest_streak");
                streak.lastActivityDate = results.getString("last_activity_date");
                return streak;
            }
        }
    }

    // The streak to show the learner. If their last check-in was before
    // yesterday, the streak has already broken, so it shows 0 even though
    // the table still has the old number until their next check-in.
    public int activeStreak(int userId) throws SQLException {

        Streak streak = findStreak(userId);
        if (streak == null || streak.lastActivityDate == null) {
            return 0;
        }

        LocalDate lastDay = LocalDate.parse(streak.lastActivityDate);
        LocalDate today = AppClock.today();
        if (lastDay.equals(today) || lastDay.equals(today.minusDays(1))) {
            return streak.currentStreak;
        } else {
            return 0;
        }
    }

    // Saves a learner's streak, adding their row the first time.
    public boolean saveStreak(int userId, int currentStreak, int longestStreak,
                              String lastActivityDate) throws SQLException {

        String sql = "INSERT INTO Streaks (user_id, current_streak, longest_streak, last_activity_date) "
                   + "VALUES (?, ?, ?, ?) AS incoming "
                   + "ON DUPLICATE KEY UPDATE "
                   + "current_streak = incoming.current_streak, "
                   + "longest_streak = incoming.longest_streak, "
                   + "last_activity_date = incoming.last_activity_date";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setInt(2, currentStreak);
            statement.setInt(3, longestStreak);
            statement.setString(4, lastActivityDate);
            return statement.executeUpdate() >= 1;
        }
    }

}
