package com.momentum.backend.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

// Turns a table into CSV text for the admin's data export.
// CSV is plain text with commas between values, one row per line. Excel opens it.
public class ExportRepository {

    // The tables that can be exported, and the query for each.
    // Only these names are allowed, so nobody can send their own SQL in the address.
    // Users leaves out the password hash on purpose.
    private static final Map<String, String> TABLES = new LinkedHashMap<>();
    static {
        TABLES.put("users", "SELECT user_id, idst, email, full_name, role, status, created_at, last_login_at "
                          + "FROM Users ORDER BY user_id");
        TABLES.put("courses", "SELECT * FROM Courses ORDER BY course_id");
        TABLES.put("enrolments", "SELECT * FROM Enrolments ORDER BY enrolment_id");
        TABLES.put("goals", "SELECT * FROM Goals ORDER BY goal_id");
        TABLES.put("checkins", "SELECT * FROM CheckIns ORDER BY checkin_id");
        TABLES.put("points", "SELECT * FROM PointsTransactions ORDER BY transaction_id");
        TABLES.put("badges", "SELECT * FROM Badges ORDER BY badge_id");
        TABLES.put("user_badges", "SELECT * FROM UserBadges ORDER BY user_badge_id");
        TABLES.put("streaks", "SELECT * FROM Streaks ORDER BY user_id");
        TABLES.put("snapshots", "SELECT * FROM ProgressSnapshots ORDER BY snapshot_id");
        TABLES.put("logs", "SELECT * FROM ActivityLogs ORDER BY log_id");
    }

    private final Database database;

    public ExportRepository(Database database) {
        this.database = database;
    }

    // The names that can be exported, for the error message.
    public Set<String> tableNames() {
        return TABLES.keySet();
    }

    // Gives back the whole table as CSV text, or null if that table can't be exported.
    public String exportCsv(String table) throws SQLException {

        String sql = TABLES.get(table);
        if (sql == null) {
            return null;
        }

        StringBuilder csv = new StringBuilder();

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {

            // The first line is the column names. The metadata says what columns the query has.
            ResultSetMetaData columns = results.getMetaData();
            int count = columns.getColumnCount();
            for (int i = 1; i <= count; i++) {
                if (i > 1) {
                    csv.append(',');
                }
                csv.append(escape(columns.getColumnLabel(i)));
            }
            csv.append("\r\n");

            // Then one line per row.
            while (results.next()) {
                for (int i = 1; i <= count; i++) {
                    if (i > 1) {
                        csv.append(',');
                    }
                    csv.append(escape(results.getString(i)));
                }
                csv.append("\r\n");
            }
        }

        return csv.toString();
    }

    // Makes one value safe for CSV. Empty for null. A value with a comma, quote or
    // line break goes in quotes, with any quote inside doubled, which is the CSV rule.
    private String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
