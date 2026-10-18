package com.momentum.backend.db;

import com.momentum.backend.model.Badge;
import com.momentum.backend.model.BadgeRequest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Reads and saves badges (Badges table) and who has earned them (UserBadges table).
public class BadgeRepository {

    private final Database database;

    public BadgeRepository(Database database) {
        this.database = database;
    }

    // Admin: create a badge.
    public boolean insertBadge(BadgeRequest badge) throws SQLException {

        String sql = "INSERT INTO Badges (name, description, image_url, rule_type, rule_value, "
                   + "points_awarded, is_active) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, badge.name);
            statement.setString(2, badge.description);
            statement.setString(3, badge.imageUrl);
            statement.setString(4, badge.ruleType);
            statement.setInt(5, badge.ruleValue);
            statement.setInt(6, badge.pointsAwarded);
            statement.setBoolean(7, badge.isActive);
            return statement.executeUpdate() == 1;
        }
    }

    public Badge findBadgeById(int badgeId) throws SQLException {

        String sql = "SELECT badge_id, name, description, image_url, rule_type, rule_value, "
                   + "points_awarded, is_active FROM Badges WHERE badge_id = ?";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, badgeId);

            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    return null;
                }

                Badge badge = new Badge();
                badge.badgeId = results.getInt("badge_id");
                badge.name = results.getString("name");
                badge.description = results.getString("description");
                badge.imageUrl = results.getString("image_url");
                badge.ruleType = results.getString("rule_type");
                badge.ruleValue = results.getInt("rule_value");
                badge.pointsAwarded = results.getInt("points_awarded");
                badge.isActive = results.getBoolean("is_active");
                return badge;
            }
        }
    }

    // Admin: edit a badge.
    public boolean updateBadge(int badgeId, BadgeRequest badge) throws SQLException {

        String sql = "UPDATE Badges SET name = ?, description = ?, image_url = ?, rule_type = ?, "
                   + "rule_value = ?, points_awarded = ?, is_active = ? WHERE badge_id = ?";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, badge.name);
            statement.setString(2, badge.description);
            statement.setString(3, badge.imageUrl);
            statement.setString(4, badge.ruleType);
            statement.setInt(5, badge.ruleValue);
            statement.setInt(6, badge.pointsAwarded);
            statement.setBoolean(7, badge.isActive);
            statement.setInt(8, badgeId);
            return statement.executeUpdate() == 1;
        }
    }

    // Admin: delete a badge. Learners who earned it lose it (ON DELETE CASCADE).
    public boolean deleteBadge(int badgeId) throws SQLException {

        String sql = "DELETE FROM Badges WHERE badge_id = ?";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, badgeId);
            return statement.executeUpdate() == 1;
        }
    }

    // Gives a learner a badge. INSERT IGNORE skips it quietly if they already
    // have it (unique key on user_id + badge_id), so a badge can only be earned once.
    // Gives back true only if the badge is new for them.
    public boolean awardBadge(int userId, int badgeId) throws SQLException {

        String sql = "INSERT IGNORE INTO UserBadges (user_id, badge_id) VALUES (?, ?)";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setInt(2, badgeId);
            return statement.executeUpdate() == 1;
        }
    }

    public boolean hasBadge(int userId, int badgeId) throws SQLException {

        String sql = "SELECT user_badge_id FROM UserBadges WHERE user_id = ? AND badge_id = ?";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setInt(2, badgeId);

            try (ResultSet results = statement.executeQuery()) {
                return results.next();
            }
        }
    }


    // Every badge, for the admin's badge screen.
    public List<Badge> findAllBadges() throws SQLException {

        String sql = "SELECT badge_id, name, description, image_url, rule_type, rule_value, "
                   + "points_awarded, is_active FROM Badges ORDER BY name";

        List<Badge> badges = new ArrayList<>();

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {

            while (results.next()) {
                Badge badge = new Badge();
                badge.badgeId = results.getInt("badge_id");
                badge.name = results.getString("name");
                badge.description = results.getString("description");
                badge.imageUrl = results.getString("image_url");
                badge.ruleType = results.getString("rule_type");
                badge.ruleValue = results.getInt("rule_value");
                badge.pointsAwarded = results.getInt("points_awarded");
                badge.isActive = results.getBoolean("is_active");
                badges.add(badge);
            }
        }

        return badges;
    }
}
