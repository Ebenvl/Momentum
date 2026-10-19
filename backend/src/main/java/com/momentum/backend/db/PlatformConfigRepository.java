package com.momentum.backend.db;

import com.momentum.backend.model.PlatformSettings;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// Reads and saves the LearnTech settings in the PlatformConfig table.
// The table only ever has one row, config_id 1.
public class PlatformConfigRepository {

    private final Database database;

    public PlatformConfigRepository(Database database) {
        this.database = database;
    }

    // Gives back null if the admin has never saved settings.
    public PlatformSettings findSettings() throws SQLException {

        String sql = "SELECT platform_url, api_key_encrypted, api_secret_encrypted, last_tested_at "
                   + "FROM PlatformConfig WHERE config_id = 1";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {

            if (!results.next()) {
                return null;
            }

            PlatformSettings settings = new PlatformSettings();
            settings.platformUrl = results.getString("platform_url");
            settings.apiKeyEncrypted = results.getString("api_key_encrypted");
            settings.apiSecretEncrypted = results.getString("api_secret_encrypted");
            settings.lastTestedAt = results.getString("last_tested_at");
            return settings;
        }
    }

    // Saves the settings, adding the row the first time.
    public boolean saveSettings(String platformUrl, String apiKeyEncrypted, String apiSecretEncrypted,
                                int updatedBy) throws SQLException {

        String sql = "INSERT INTO PlatformConfig (config_id, platform_url, api_key_encrypted, "
                   + "api_secret_encrypted, updated_by) VALUES (1, ?, ?, ?, ?) AS incoming "
                   + "ON DUPLICATE KEY UPDATE "
                   + "platform_url = incoming.platform_url, "
                   + "api_key_encrypted = incoming.api_key_encrypted, "
                   + "api_secret_encrypted = incoming.api_secret_encrypted, "
                   + "updated_by = incoming.updated_by";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, platformUrl);
            statement.setString(2, apiKeyEncrypted);
            statement.setString(3, apiSecretEncrypted);
            statement.setInt(4, updatedBy);
            return statement.executeUpdate() >= 1;
        }
    }

    // Records that the connection test worked just now.
    public boolean markTested() throws SQLException {

        String sql = "UPDATE PlatformConfig SET last_tested_at = NOW() WHERE config_id = 1";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            return statement.executeUpdate() == 1;
        }
    }
}
