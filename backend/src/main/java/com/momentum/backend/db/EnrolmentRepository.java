package com.momentum.backend.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

// Reads and saves enrolments in the Enrolments table.
public class EnrolmentRepository {

    private final Database database;

    public EnrolmentRepository(Database database) {
        this.database = database;
    }

    // Used by the sync: adds the enrolment if it is new, or updates it with the
    // latest state from LearnTech if it is already there.
    // Any of the Integer, Double and String values can be null.
    public boolean saveEnrolment(int userId, int courseId, String state, int timeSpentMinutes,
                                 String assessmentStatus, Double score, Double scoreMax,
                                 String firstAccess, String completedOn) throws SQLException {

        String sql = "INSERT INTO Enrolments (user_id, course_id, state, time_spent_minutes, "
                   + "assessment_status, score, score_max, first_access, completed_on, last_synced_at) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW()) AS incoming "
                   + "ON DUPLICATE KEY UPDATE "
                   + "state = incoming.state, "
                   + "time_spent_minutes = incoming.time_spent_minutes, "
                   + "assessment_status = incoming.assessment_status, "
                   + "score = incoming.score, "
                   + "score_max = incoming.score_max, "
                   + "first_access = incoming.first_access, "
                   + "completed_on = incoming.completed_on, "
                   + "last_synced_at = NOW()";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setInt(2, courseId);
            statement.setString(3, state);
            statement.setInt(4, timeSpentMinutes);
            statement.setString(5, assessmentStatus);
            statement.setObject(6, score);
            statement.setObject(7, scoreMax);
            statement.setString(8, firstAccess);
            statement.setString(9, completedOn);
            return statement.executeUpdate() >= 1;
        }
    }

}
