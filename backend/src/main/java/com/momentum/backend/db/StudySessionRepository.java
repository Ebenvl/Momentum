package com.momentum.backend.db;

import com.momentum.backend.model.StudySession;
import com.momentum.backend.model.StudySessionRequest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Reads and saves study timetable sessions in the StudySessions table.
// Every method also checks user_id, so a learner only ever touches their own sessions.
public class StudySessionRepository {

    private final Database database;

    public StudySessionRepository(Database database) {
        this.database = database;
    }

    public List<StudySession> findSessionsForUser(int userId) throws SQLException {

        String sql = "SELECT session_id, day, time, course, task FROM StudySessions WHERE user_id = ?";

        List<StudySession> sessions = new ArrayList<>();

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);

            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    StudySession session = new StudySession();
                    session.sessionId = results.getInt("session_id");
                    session.day = results.getString("day");
                    session.time = results.getString("time");
                    session.course = results.getString("course");
                    session.task = results.getString("task");
                    sessions.add(session);
                }
            }
        }

        return sessions;
    }

    public boolean insertSession(int userId, StudySessionRequest session) throws SQLException {

        String sql = "INSERT INTO StudySessions (user_id, day, time, course, task) VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setString(2, session.day);
            statement.setString(3, session.time);
            statement.setString(4, session.course);
            statement.setString(5, session.task);
            return statement.executeUpdate() == 1;
        }
    }

    public boolean updateSession(int sessionId, int userId, StudySessionRequest session) throws SQLException {

        String sql = "UPDATE StudySessions SET day = ?, time = ?, course = ?, task = ? "
                   + "WHERE session_id = ? AND user_id = ?";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, session.day);
            statement.setString(2, session.time);
            statement.setString(3, session.course);
            statement.setString(4, session.task);
            statement.setInt(5, sessionId);
            statement.setInt(6, userId);
            return statement.executeUpdate() == 1;
        }
    }

    public boolean deleteSession(int sessionId, int userId) throws SQLException {

        String sql = "DELETE FROM StudySessions WHERE session_id = ? AND user_id = ?";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, sessionId);
            statement.setInt(2, userId);
            return statement.executeUpdate() == 1;
        }
    }
}
