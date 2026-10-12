package com.momentum.backend.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

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

}
