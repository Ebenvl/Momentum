package com.momentum.backend.db;

import com.momentum.backend.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// Reads and saves users in the Users table.
public class UserRepository {

    private final Database database;

    public UserRepository(Database database) {
        this.database = database;
    }

    public boolean insertUser(String email, String passwordHash, String fullName) throws SQLException {

        String sql = "INSERT INTO Users (email, password_hash, full_name) VALUES (?, ?, ?)";

        // Whatever is opened in the brackets after "try" is closed automatically
        // at the end, even if something fails halfway.
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, email);
            statement.setString(2, passwordHash);
            statement.setString(3, fullName);
            int rows = statement.executeUpdate();
            return rows == 1;
        }
    }

    public boolean emailExists(String email) throws SQLException {

        String sql = "SELECT user_id FROM Users WHERE email = ?";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, email);

            try (ResultSet results = statement.executeQuery()) {
                return results.next();
            }
        }
    }

    // Finds a user by email. Gives back null if nobody has that email.
    public User findUserByEmail(String email) throws SQLException {

        String sql = "SELECT user_id, email, password_hash, full_name, role, status FROM Users WHERE email = ?";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, email);

            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    return null;
                }

                User user = new User();
                user.userId = results.getInt("user_id");
                user.email = results.getString("email");
                user.passwordHash = results.getString("password_hash");
                user.fullName = results.getString("full_name");
                user.role = results.getString("role");
                user.status = results.getString("status");
                return user;

            }
        }
    }

}
