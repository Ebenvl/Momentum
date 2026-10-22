package com.momentum.backend.db;

import com.momentum.backend.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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

        String sql = "SELECT user_id, idst, email, password_hash, full_name, role, status, two_factor_enabled FROM Users WHERE email = ?";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, email);

            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    return null;
                }

                User user = new User();
                user.userId = results.getInt("user_id");
                user.idst = results.getObject("idst", Integer.class);
                user.email = results.getString("email");
                user.passwordHash = results.getString("password_hash");
                user.fullName = results.getString("full_name");
                user.role = results.getString("role");
                user.status = results.getString("status");
                user.twoFactorEnabled = results.getBoolean("two_factor_enabled");
                return user;

            }
        }
    }


    // Finds a user by id. Gives back null if there is no such user.
    public User findUserById(int userId) throws SQLException {

        String sql = "SELECT user_id, idst, email, password_hash, full_name, role, status, two_factor_enabled FROM Users WHERE user_id = ?";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);

            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    return null;
                }

                User user = new User();
                user.userId = results.getInt("user_id");
                user.idst = results.getObject("idst", Integer.class);
                user.email = results.getString("email");
                user.passwordHash = results.getString("password_hash");
                user.fullName = results.getString("full_name");
                user.role = results.getString("role");
                user.status = results.getString("status");
                user.twoFactorEnabled = results.getBoolean("two_factor_enabled");
                return user;
            }
        }
    }

    // Finds the account linked to this LearnTech learner. Gives back null if none is.
    public User findUserByIdst(int idst) throws SQLException {

        String sql = "SELECT user_id, idst, email, password_hash, full_name, role, status FROM Users WHERE idst = ?";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idst);

            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    return null;
                }

                User user = new User();
                user.userId = results.getInt("user_id");
                user.idst = results.getObject("idst", Integer.class);
                user.email = results.getString("email");
                user.passwordHash = results.getString("password_hash");
                user.fullName = results.getString("full_name");
                user.role = results.getString("role");
                user.status = results.getString("status");
                return user;
            }
        }
    }

    // Is this LearnTech learner already linked to an account? Each learner
    // can only be linked to one account (unique key on idst).
    public boolean idstTaken(int idst) throws SQLException {

        String sql = "SELECT user_id FROM Users WHERE idst = ?";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idst);

            try (ResultSet results = statement.executeQuery()) {
                return results.next();
            }
        }
    }

    // Records when the user last logged in.
    public boolean updateLastLogin(int userId) throws SQLException {

        String sql = "UPDATE Users SET last_login_at = NOW() WHERE user_id = ?";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            return statement.executeUpdate() == 1;
        }
    }

    // Admin: suspend or reactivate an account.
    public boolean updateStatus(int userId, String status) throws SQLException {

        String sql = "UPDATE Users SET status = ? WHERE user_id = ?";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, status);
            statement.setInt(2, userId);
            return statement.executeUpdate() == 1;
        }
    }

    // A user changing their own name.
    public boolean updateName(int userId, String fullName) throws SQLException {

        String sql = "UPDATE Users SET full_name = ? WHERE user_id = ?";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, fullName);
            statement.setInt(2, userId);
            return statement.executeUpdate() == 1;
        }
    }

    // Turns two-step login on or off for this user.
    public boolean setTwoFactor(int userId, boolean enabled) throws SQLException {

        String sql = "UPDATE Users SET two_factor_enabled = ? WHERE user_id = ?";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setBoolean(1, enabled);
            statement.setInt(2, userId);
            return statement.executeUpdate() == 1;
        }
    }

    // Saves a new password hash.
    public boolean updatePassword(int userId, String passwordHash) throws SQLException {

        String sql = "UPDATE Users SET password_hash = ? WHERE user_id = ?";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, passwordHash);
            statement.setInt(2, userId);
            return statement.executeUpdate() == 1;
        }
    }

    // Admin: change someone's name and role.
    public boolean updateUserDetails(int userId, String fullName, String role) throws SQLException {

        String sql = "UPDATE Users SET full_name = ?, role = ? WHERE user_id = ?";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, fullName);
            statement.setString(2, role);
            statement.setInt(3, userId);
            return statement.executeUpdate() == 1;
        }
    }

    // Links an account to a LearnTech learner, or unlinks it if idst is null.
    // Used by the sync and by the admin.
    public boolean updateIdst(int userId, Integer idst) throws SQLException {

        String sql = "UPDATE Users SET idst = ? WHERE user_id = ?";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            // setObject is used instead of setInt because idst can be null.
            statement.setObject(1, idst);
            statement.setInt(2, userId);
            return statement.executeUpdate() == 1;
        }
    }

    // Learner: change their name and reminder settings.
    public boolean updateProfile(int userId, String fullName, boolean reminderEnabled,
                                 String reminderTime, int reminderDaysBefore) throws SQLException {

        String sql = "UPDATE Users SET full_name = ?, reminder_enabled = ?, reminder_time = ?, "
                   + "reminder_days_before = ? WHERE user_id = ?";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, fullName);
            statement.setBoolean(2, reminderEnabled);
            statement.setString(3, reminderTime);
            statement.setInt(4, reminderDaysBefore);
            statement.setInt(5, userId);
            return statement.executeUpdate() == 1;
        }
    }

    // Admin: remove an account. Their goals, check-ins, points and so on
    // are removed with it, because of ON DELETE CASCADE in the schema.
    public boolean deleteUser(int userId) throws SQLException {

        String sql = "DELETE FROM Users WHERE user_id = ?";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            return statement.executeUpdate() == 1;
        }
    }


    // Everyone with an account, for the admin's user list. The password hash is left out.
    public List<User> findAllUsers() throws SQLException {

        // course_count counts each user's rows in Enrolments
        String sql = "SELECT user_id, idst, email, full_name, role, status, last_login_at, "
                   + "(SELECT COUNT(*) FROM Enrolments WHERE Enrolments.user_id = Users.user_id) AS course_count "
                   + "FROM Users ORDER BY full_name";

        List<User> users = new ArrayList<>();

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {

            while (results.next()) {
                User user = new User();
                user.userId = results.getInt("user_id");
                user.idst = results.getObject("idst", Integer.class);
                user.email = results.getString("email");
                user.fullName = results.getString("full_name");
                user.role = results.getString("role");
                user.status = results.getString("status");
                user.lastLoginAt = results.getString("last_login_at");
                user.courseCount = results.getInt("course_count");
                users.add(user);
            }
        }

        return users;
    }
}
