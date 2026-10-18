package com.example.momentum.data;

import android.os.Handler;
import android.os.Looper;

import com.example.momentum.model.ActivityLog;
import com.example.momentum.model.AdminStats;
import com.example.momentum.model.Course;
import com.example.momentum.model.Enrolment;
import com.example.momentum.model.Goal;
import com.example.momentum.model.LeaderboardEntry;
import com.example.momentum.model.StudySession;
import com.example.momentum.model.User;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Talks to the Momentum backend over HTTP and JSON.
public class RealMomentumApi implements MomentumApi {

    // The backend on the laptop. For a phone plugged in with USB, run this once
    // on the laptop so the phone's localhost goes to the laptop:
    //     adb reverse tcp:7070 tcp:7070
    // Later this becomes the hosted backend's address.
    private static final String BASE_URL = "http://localhost:7070";

    // Android does not allow network calls on the screen's thread, so they run
    // in the background, and the result is handed back to the screen's thread.
    private final ExecutorService background = Executors.newSingleThreadExecutor();
    private final Handler mainThread = new Handler(Looper.getMainLooper());

    // -----------------------------------------------------
    // Login and register
    // -----------------------------------------------------

    @Override
    public void login(String email, String password, ApiCallback<User> callback) {
        run(callback, () -> {
            JSONObject body = new JSONObject();
            body.put("email", email);
            body.put("password", password);
            JSONObject reply = new JSONObject(send("POST", "/login", body));

            User user = new User(reply.getInt("userId"), null, email,
                    reply.getString("fullName"), reply.getString("role"), "active");
            Session.start(user, reply.getString("token"));
            return user;
        });
    }

    @Override
    public void register(String email, String password, String fullName, ApiCallback<Void> callback) {
        run(callback, () -> {
            JSONObject body = new JSONObject();
            body.put("email", email);
            body.put("password", password);
            body.put("fullName", fullName);
            send("POST", "/register", body);
            return null;
        });
    }

    // -----------------------------------------------------
    // Courses
    // -----------------------------------------------------

    @Override
    public void getCatalogue(ApiCallback<List<Course>> callback) {
        run(callback, () -> {
            JSONArray rows = new JSONArray(send("GET", "/courses", null));
            List<Course> courses = new ArrayList<>();
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                Course course = new Course();
                course.courseId = row.getInt("courseId");
                course.name = row.getString("courseName");
                course.expectedMinutes = optInt(row, "expectedMinutes");
                course.pointsValue = optInt(row, "pointsValue");
                courses.add(course);
            }
            return courses;
        });
    }

    @Override
    public void getMyCourses(ApiCallback<List<Enrolment>> callback) {
        run(callback, () -> {
            JSONArray rows = new JSONArray(send("GET", "/me/courses", null));
            List<Enrolment> enrolments = new ArrayList<>();
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                Enrolment enrolment = new Enrolment();
                enrolment.courseId = row.getInt("courseId");
                enrolment.state = row.getString("state");
                enrolment.timeSpentMinutes = row.optInt("timeSpentMinutes");
                enrolment.completedOn = optString(row, "completedOn");
                enrolments.add(enrolment);
            }
            return enrolments;
        });
    }

    // -----------------------------------------------------
    // Goals
    // -----------------------------------------------------

    @Override
    public void getGoals(ApiCallback<List<Goal>> callback) {
        run(callback, () -> {
            JSONArray rows = new JSONArray(send("GET", "/goals", null));
            List<Goal> goals = new ArrayList<>();
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                Goal goal = new Goal();
                goal.id = row.getInt("goalId");
                goal.title = row.getString("title");
                goal.goalType = row.getString("goalType");
                goal.targetValue = optInt(row, "targetValue");
                goal.targetCourseId = optInt(row, "targetCourseId");
                goal.targetDate = optString(row, "targetDate");
                goal.status = row.getString("status");
                goal.createdAt = optString(row, "createdAt");
                goal.completedAt = optString(row, "completedAt");
                goals.add(goal);
            }
            return goals;
        });
    }

    // A goal with a course becomes "complete this course", otherwise free text.
    @Override
    public void createGoal(String title, Integer targetCourseId, String targetDate, ApiCallback<Void> callback) {
        run(callback, () -> {
            JSONObject body = new JSONObject();
            body.put("title", title);
            body.put("goalType", targetCourseId != null ? "complete_specific_course" : "free_text");
            body.put("targetCourseId", targetCourseId != null ? targetCourseId : JSONObject.NULL);
            body.put("targetDate", targetDate != null ? targetDate : JSONObject.NULL);
            send("POST", "/goals", body);
            return null;
        });
    }

    @Override
    public void setGoalStatus(int goalId, String status, ApiCallback<Void> callback) {
        run(callback, () -> {
            JSONObject body = new JSONObject();
            body.put("status", status);
            send("PUT", "/goals/" + goalId + "/status", body);
            return null;
        });
    }

    @Override
    public void deleteGoal(int goalId, ApiCallback<Void> callback) {
        run(callback, () -> {
            send("DELETE", "/goals/" + goalId, null);
            return null;
        });
    }

    // -----------------------------------------------------
    // Leaderboard
    // -----------------------------------------------------

    @Override
    public void getLeaderboard(ApiCallback<List<LeaderboardEntry>> callback) {
        run(callback, () -> {
            JSONArray rows = new JSONArray(send("GET", "/leaderboard", null));
            List<LeaderboardEntry> entries = new ArrayList<>();
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                LeaderboardEntry entry = new LeaderboardEntry();
                entry.rank = row.getInt("rank");
                entry.userId = row.getInt("userId");
                entry.name = row.getString("fullName");
                entry.points = row.getInt("points");
                entries.add(entry);
            }
            return entries;
        });
    }

    // -----------------------------------------------------
    // Study timetable
    // -----------------------------------------------------

    @Override
    public void getStudySessions(ApiCallback<List<StudySession>> callback) {
        run(callback, () -> {
            JSONArray rows = new JSONArray(send("GET", "/study-sessions", null));
            List<StudySession> sessions = new ArrayList<>();
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                StudySession session = new StudySession();
                session.id = row.getInt("sessionId");
                session.day = row.getString("day");
                session.time = row.getString("time");
                session.course = row.getString("course");
                session.task = row.getString("task");
                sessions.add(session);
            }
            return sessions;
        });
    }

    @Override
    public void saveStudySession(Integer id, String day, String time, String course, String task,
                                 ApiCallback<Void> callback) {
        run(callback, () -> {
            JSONObject body = new JSONObject();
            body.put("day", day);
            body.put("time", time);
            body.put("course", course);
            body.put("task", task);
            if (id == null) {
                send("POST", "/study-sessions", body);
            } else {
                send("PUT", "/study-sessions/" + id, body);
            }
            return null;
        });
    }

    @Override
    public void deleteStudySession(int id, ApiCallback<Void> callback) {
        run(callback, () -> {
            send("DELETE", "/study-sessions/" + id, null);
            return null;
        });
    }

    // -----------------------------------------------------
    // Admin
    // -----------------------------------------------------

    @Override
    public void getAdminStats(ApiCallback<AdminStats> callback) {
        run(callback, () -> {
            JSONObject row = new JSONObject(send("GET", "/admin/stats", null));
            AdminStats stats = new AdminStats();
            stats.totalUsers = row.getInt("totalUsers");
            stats.activeCourses = row.getInt("activeCourses");
            stats.completionRate = row.getInt("completionRate");
            stats.engagement = row.getInt("engagement");
            return stats;
        });
    }

    @Override
    public void getAllUsers(ApiCallback<List<User>> callback) {
        run(callback, () -> {
            JSONArray rows = new JSONArray(send("GET", "/admin/users", null));
            List<User> users = new ArrayList<>();
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                User user = new User(row.getInt("userId"), optInt(row, "idst"),
                        row.getString("email"), row.getString("fullName"),
                        row.getString("role"), row.getString("status"));
                user.lastLoginAt = optString(row, "lastLoginAt");
                user.courseCount = optInt(row, "courseCount");
                users.add(user);
            }
            return users;
        });
    }

    @Override
    public void deleteUser(int userId, ApiCallback<Void> callback) {
        run(callback, () -> {
            send("DELETE", "/admin/users/" + userId, null);
            return null;
        });
    }

    @Override
    public void getActivityLogs(ApiCallback<List<ActivityLog>> callback) {
        run(callback, () -> {
            JSONArray rows = new JSONArray(send("GET", "/admin/logs", null));
            List<ActivityLog> logs = new ArrayList<>();
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                ActivityLog log = new ActivityLog();
                log.id = row.getInt("logId");
                log.userId = optInt(row, "userId");
                log.action = row.getString("action");
                log.level = row.getString("level");
                log.createdAt = row.getString("createdAt");
                logs.add(log);
            }
            return logs;
        });
    }

    // -----------------------------------------------------
    // Helpers
    // -----------------------------------------------------

    // One request, as a piece of code that may fail.
    private interface Request<T> {
        T call() throws Exception;
    }

    // Runs a request in the background and hands the result, or the error
    // message, back to the screen's thread.
    private <T> void run(ApiCallback<T> callback, Request<T> request) {
        background.execute(() -> {
            try {
                T result = request.call();
                mainThread.post(() -> callback.onSuccess(result));
            } catch (ApiException e) {
                mainThread.post(() -> callback.onError(e.getMessage()));
            } catch (Exception e) {
                mainThread.post(() -> callback.onError("Can't reach the server. Check your connection and try again."));
            }
        });
    }

    // Sends a request to the backend and gives back the reply as text.
    // Sends the login token too, if someone is logged in.
    // If the backend replies with an error, throws ApiException with its message.
    private String send(String method, String path, JSONObject body) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(BASE_URL + path).openConnection();
        try {
            connection.setRequestMethod(method);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            if (Session.getToken() != null) {
                connection.setRequestProperty("Authorization", "Bearer " + Session.getToken());
            }

            if (body != null) {
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                try (OutputStream out = connection.getOutputStream()) {
                    out.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }
            }

            int status = connection.getResponseCode();
            InputStream stream = status < 400 ? connection.getInputStream() : connection.getErrorStream();
            String text = readAll(stream);

            if (status >= 400) {
                String message = "something went wrong, try again";
                try {
                    message = new JSONObject(text).optString("error", message);
                } catch (Exception ignored) {
                    // the reply wasn't JSON, keep the general message
                }
                throw new ApiException(capitalise(message));
            }
            return text;
        } finally {
            connection.disconnect();
        }
    }

    private String readAll(InputStream stream) throws Exception {
        if (stream == null) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line);
            }
        }
        return text.toString();
    }

    // Reads a number that can be null.
    private Integer optInt(JSONObject row, String key) {
        return row.isNull(key) ? null : row.optInt(key);
    }

    // Reads text that can be null.
    private String optString(JSONObject row, String key) {
        return row.isNull(key) ? null : row.optString(key);
    }

    // The backend's messages are lowercase, this makes them look right on screen.
    private String capitalise(String message) {
        if (message.isEmpty()) {
            return message;
        }
        return message.substring(0, 1).toUpperCase() + message.substring(1);
    }

    // An error message that came from the backend, meant to be shown to the user.
    private static class ApiException extends Exception {
        ApiException(String message) {
            super(message);
        }
    }
}
