package com.momentum.backend.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// Saves and checks the emailed 6-digit codes in the EmailCodes table.
// Only a hash of each code is stored. The database's own clock decides
// when a code has run out, so it's always consistent.
public class EmailCodeRepository {

    // One saved code, as read from the table.
    public static class SavedCode {
        public int codeId;
        public String codeHash;
        public int attempts;
    }

    private final Database database;

    public EmailCodeRepository(Database database) {
        this.database = database;
    }

    // Saves a new code that lasts the given minutes. Any older unused code for the
    // same user and purpose stops working, so only the newest email counts.
    public boolean saveCode(int userId, String purpose, String codeHash, int minutes) throws SQLException {

        String cancelOld = "UPDATE EmailCodes SET used = TRUE WHERE user_id = ? AND purpose = ? AND used = FALSE";
        String insert = "INSERT INTO EmailCodes (user_id, purpose, code_hash, expires_at) "
                      + "VALUES (?, ?, ?, NOW() + INTERVAL ? MINUTE)";

        try (Connection connection = database.getConnection();
             PreparedStatement cancel = connection.prepareStatement(cancelOld);
             PreparedStatement statement = connection.prepareStatement(insert)) {

            cancel.setInt(1, userId);
            cancel.setString(2, purpose);
            cancel.executeUpdate();

            statement.setInt(1, userId);
            statement.setString(2, purpose);
            statement.setString(3, codeHash);
            statement.setInt(4, minutes);
            return statement.executeUpdate() == 1;
        }
    }

    // The newest code that is unused and hasn't run out. Null if there is none.
    public SavedCode findActiveCode(int userId, String purpose) throws SQLException {

        String sql = "SELECT code_id, code_hash, attempts FROM EmailCodes "
                   + "WHERE user_id = ? AND purpose = ? AND used = FALSE AND expires_at > NOW() "
                   + "ORDER BY code_id DESC LIMIT 1";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setString(2, purpose);

            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    return null;
                }
                SavedCode code = new SavedCode();
                code.codeId = results.getInt("code_id");
                code.codeHash = results.getString("code_hash");
                code.attempts = results.getInt("attempts");
                return code;
            }
        }
    }

    // True if a code was sent to this user for this purpose in the last minute.
    // Stops someone filling a person's inbox by asking over and over.
    public boolean sentRecently(int userId, String purpose) throws SQLException {

        String sql = "SELECT code_id FROM EmailCodes "
                   + "WHERE user_id = ? AND purpose = ? AND created_at > NOW() - INTERVAL 1 MINUTE";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setString(2, purpose);

            try (ResultSet results = statement.executeQuery()) {
                return results.next();
            }
        }
    }

    public boolean addAttempt(int codeId) throws SQLException {

        String sql = "UPDATE EmailCodes SET attempts = attempts + 1 WHERE code_id = ?";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, codeId);
            return statement.executeUpdate() == 1;
        }
    }

    public boolean markUsed(int codeId) throws SQLException {

        String sql = "UPDATE EmailCodes SET used = TRUE WHERE code_id = ?";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, codeId);
            return statement.executeUpdate() == 1;
        }
    }
}
