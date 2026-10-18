package com.momentum.backend.db;

import com.momentum.backend.model.CheckIn;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Saves check-ins in the CheckIns table.
public class CheckInRepository {

    private final Database database;

    public CheckInRepository(Database database) {
        this.database = database;
    }

    public boolean insertCheckIn(int goalId, int userId, String note, Integer valueLogged) throws SQLException {

        String sql = "INSERT INTO CheckIns (goal_id, user_id, note, value_logged) VALUES (?, ?, ?, ?)";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, goalId);
            statement.setInt(2, userId);
            statement.setString(3, note);
            statement.setObject(4, valueLogged);
            return statement.executeUpdate() == 1;
        }
    }


    // All check-ins on one of this user's goals, newest first.
    public List<CheckIn> findCheckInsForGoal(int goalId, int userId) throws SQLException {

        String sql = "SELECT checkin_id, goal_id, user_id, logged_at, note, value_logged "
                   + "FROM CheckIns WHERE goal_id = ? AND user_id = ? ORDER BY logged_at DESC";

        List<CheckIn> checkIns = new ArrayList<>();

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, goalId);
            statement.setInt(2, userId);

            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    CheckIn checkIn = new CheckIn();
                    checkIn.checkinId = results.getInt("checkin_id");
                    checkIn.goalId = results.getInt("goal_id");
                    checkIn.userId = results.getInt("user_id");
                    checkIn.loggedAt = results.getString("logged_at");
                    checkIn.note = results.getString("note");
                    checkIn.valueLogged = results.getObject("value_logged", Integer.class);
                    checkIns.add(checkIn);
                }
            }
        }

        return checkIns;
    }
}
