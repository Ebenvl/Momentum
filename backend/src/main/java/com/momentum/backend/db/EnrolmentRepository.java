package com.momentum.backend.db;

import com.momentum.backend.model.Enrolment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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

    // How many courses the learner has completed, for badge rules.
    public int countCompleted(int userId) throws SQLException {

        String sql = "SELECT COUNT(*) FROM Enrolments WHERE user_id = ? AND state = 'completed'";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);

            try (ResultSet results = statement.executeQuery()) {
                results.next();
                return results.getInt(1);
            }
        }
    }

    // All the minutes the learner has spent in their courses, for goals.
    public int totalMinutes(int userId) throws SQLException {

        String sql = "SELECT COALESCE(SUM(time_spent_minutes), 0) FROM Enrolments WHERE user_id = ?";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);

            try (ResultSet results = statement.executeQuery()) {
                results.next();
                return results.getInt(1);
            }
        }
    }

    // Has the learner completed this course? Uses our own course_id.
    public boolean isCompleted(int userId, int courseId) throws SQLException {

        String sql = "SELECT enrolment_id FROM Enrolments "
                   + "WHERE user_id = ? AND course_id = ? AND state = 'completed'";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setInt(2, courseId);

            try (ResultSet results = statement.executeQuery()) {
                return results.next();
            }
        }
    }

    // The learner's courses and how far they are with each, as of the last sync.
    public List<Enrolment> findEnrolmentsForUser(int userId) throws SQLException {

        String sql = "SELECT enrolment_id, user_id, course_id, state, time_spent_minutes, assessment_status, "
                   + "score, score_max, first_access, completed_on, last_synced_at "
                   + "FROM Enrolments WHERE user_id = ?";

        List<Enrolment> enrolments = new ArrayList<>();

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);

            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    Enrolment enrolment = new Enrolment();
                    enrolment.enrolmentId = results.getInt("enrolment_id");
                    enrolment.userId = results.getInt("user_id");
                    enrolment.courseId = results.getInt("course_id");
                    enrolment.state = results.getString("state");
                    enrolment.timeSpentMinutes = results.getInt("time_spent_minutes");
                    enrolment.assessmentStatus = results.getString("assessment_status");
                    enrolment.score = results.getObject("score", Double.class);
                    enrolment.scoreMax = results.getObject("score_max", Double.class);
                    enrolment.firstAccess = results.getString("first_access");
                    enrolment.completedOn = results.getString("completed_on");
                    enrolment.lastSyncedAt = results.getString("last_synced_at");
                    enrolments.add(enrolment);
                }
            }
        }

        return enrolments;
    }
}
