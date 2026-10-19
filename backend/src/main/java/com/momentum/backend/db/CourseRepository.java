package com.momentum.backend.db;

import com.momentum.backend.model.Course;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Reads and saves courses in the Courses table.
public class CourseRepository {

    private final Database database;

    public CourseRepository(Database database) {
        this.database = database;
    }

    // Finds a course by our own id. Gives back null if there is no such course.
    public Course findCourseById(int courseId) throws SQLException {

        String sql = "SELECT course_id, platform_course_id, course_name, expected_minutes, points_value "
                   + "FROM Courses WHERE course_id = ?";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, courseId);

            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    return null;
                }
                return readCourse(results);
            }
        }
    }

    // Finds a course by LearnTech's course_id. Used by the sync.
    public Course findCourseByPlatformId(int platformCourseId) throws SQLException {

        String sql = "SELECT course_id, platform_course_id, course_name, expected_minutes, points_value "
                   + "FROM Courses WHERE platform_course_id = ?";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, platformCourseId);

            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    return null;
                }
                return readCourse(results);
            }
        }
    }

    // Finds a course by its name. Capitals don't matter, because of how the
    // database compares text. Gives back null if there is no such course.
    public Course findCourseByName(String courseName) throws SQLException {

        String sql = "SELECT course_id, platform_course_id, course_name, expected_minutes, points_value "
                   + "FROM Courses WHERE course_name = ?";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, courseName);

            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    return null;
                }
                return readCourse(results);
            }
        }
    }

    // Used by the sync: adds the course if it is new, or updates its name if it
    // is already there. Keyed on platform_course_id, never on the code.
    //
    // If LearnTech sends no duration (null), whatever is already saved is kept,
    // so a duration the admin filled in by hand is not wiped out.
    // COALESCE means "use the first one that is not null".
    public boolean saveCourse(int platformCourseId, String courseName, Integer expectedMinutes) throws SQLException {

        String sql = "INSERT INTO Courses (platform_course_id, course_name, expected_minutes) "
                   + "VALUES (?, ?, ?) AS incoming "
                   + "ON DUPLICATE KEY UPDATE "
                   + "course_name = incoming.course_name, "
                   + "expected_minutes = COALESCE(incoming.expected_minutes, Courses.expected_minutes)";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, platformCourseId);
            statement.setString(2, courseName);
            statement.setObject(3, expectedMinutes);
            return statement.executeUpdate() >= 1;
        }
    }

    // Admin: override how many points a course is worth, or null to go back
    // to the calculated value.
    public boolean updatePointsValue(int courseId, Integer pointsValue) throws SQLException {

        String sql = "UPDATE Courses SET points_value = ? WHERE course_id = ?";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, pointsValue);
            statement.setInt(2, courseId);
            return statement.executeUpdate() == 1;
        }
    }

    // Admin: set the expected duration where LearnTech does not supply one.
    public boolean updateExpectedMinutes(int courseId, Integer expectedMinutes) throws SQLException {

        String sql = "UPDATE Courses SET expected_minutes = ? WHERE course_id = ?";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, expectedMinutes);
            statement.setInt(2, courseId);
            return statement.executeUpdate() == 1;
        }
    }

    // Copies one row into a Course. Shared by the two find methods above,
    // so the same six lines are not written twice.
    private Course readCourse(ResultSet results) throws SQLException {
        Course course = new Course();
        course.courseId = results.getInt("course_id");
        course.platformCourseId = results.getInt("platform_course_id");
        course.courseName = results.getString("course_name");
        course.expectedMinutes = results.getObject("expected_minutes", Integer.class);
        course.pointsValue = results.getObject("points_value", Integer.class);
        return course;
    }


    // The whole course catalogue, sorted by name.
    public List<Course> findAllCourses() throws SQLException {

        String sql = "SELECT course_id, platform_course_id, course_name, expected_minutes, points_value "
                   + "FROM Courses ORDER BY course_name";

        List<Course> courses = new ArrayList<>();

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {

            while (results.next()) {
                courses.add(readCourse(results));
            }
        }

        return courses;
    }
}
