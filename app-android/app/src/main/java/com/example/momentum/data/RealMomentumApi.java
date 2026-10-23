package com.example.momentum.data;

import android.os.Handler;
import android.os.Looper;

import com.example.momentum.model.ActivityLog;
import com.example.momentum.model.AdminStats;
import com.example.momentum.model.Badge;
import com.example.momentum.model.CheckIn;
import com.example.momentum.model.Course;
import com.example.momentum.model.CourseStat;
import com.example.momentum.model.Enrolment;
import com.example.momentum.model.Goal;
import com.example.momentum.model.LeaderboardEntry;
import com.example.momentum.model.PlatformSettings;
import com.example.momentum.model.ProgressSnapshot;
import com.example.momentum.model.StudySession;
import com.example.momentum.model.User;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
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

    // The hosted backend. For the laptop instead: "http://localhost:7070",
    // with "adb reverse tcp:7070 tcp:7070" run once after plugging in the phone.
    private static final String BASE_URL = "https://momentum-backend-jix0.onrender.com";

    // Android does not allow network calls on the screen's thread, so they run
    // in the background, and the result is handed back to the screen's thread.
    private final ExecutorService background = Executors.newSingleThreadExecutor();
    private final Handler mainThread = new Handler(Looper.getMainLooper());

    // Login and register

    @Override
    public void login(String email, String password, ApiCallback<User> callback) {
        run(callback, () -> {
            JSONObject body = new JSONObject();
            body.put("email", email);
            body.put("password", password);
            JSONObject reply = new JSONObject(send("POST", "/login", body));

            // Two-step login is on: the password was right and a code was emailed.
            // null tells the login screen to ask for the code (see verifyLogin).
            if (reply.optBoolean("codeRequired")) {
                return null;
            }
            return startSession(reply, email);
        });
    }

    @Override
    public void verifyLogin(String email, String code, ApiCallback<User> callback) {
        run(callback, () -> {
            JSONObject body = new JSONObject();
            body.put("email", email);
            body.put("code", code);
            return startSession(new JSONObject(send("POST", "/login/verify", body)), email);
        });
    }

    // A login reply with a token: remembers who is logged in.
    private User startSession(JSONObject reply, String email) throws Exception {
        User user = new User(reply.getInt("userId"), null, email,
                reply.getString("fullName"), reply.getString("role"), "active");
        Session.start(user, reply.getString("token"));
        return user;
    }

    @Override
    public void forgotPassword(String email, ApiCallback<Void> callback) {
        run(callback, () -> {
            JSONObject body = new JSONObject();
            body.put("email", email);
            send("POST", "/password/forgot", body);
            return null;
        });
    }

    @Override
    public void resetPassword(String email, String code, String newPassword, ApiCallback<Void> callback) {
        run(callback, () -> {
            JSONObject body = new JSONObject();
            body.put("email", email);
            body.put("code", code);
            body.put("newPassword", newPassword);
            send("POST", "/password/reset", body);
            return null;
        });
    }

    @Override
    public void startTwoFactor(ApiCallback<Void> callback) {
        run(callback, () -> {
            send("POST", "/me/two-factor/start", new JSONObject());
            return null;
        });
    }

    @Override
    public void confirmTwoFactor(String code, ApiCallback<Void> callback) {
        run(callback, () -> {
            JSONObject body = new JSONObject();
            body.put("code", code);
            send("POST", "/me/two-factor/confirm", body);
            return null;
        });
    }

    @Override
    public void disableTwoFactor(String password, ApiCallback<Void> callback) {
        run(callback, () -> {
            JSONObject body = new JSONObject();
            body.put("password", password);
            send("POST", "/me/two-factor/disable", body);
            return null;
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

    @Override
    public void getMe(ApiCallback<User> callback) {
        run(callback, () -> {
            JSONObject row = new JSONObject(send("GET", "/me", null));
            User user = new User(row.getInt("userId"), optInt(row, "idst"),
                    row.getString("email"), row.getString("fullName"),
                    row.getString("role"), row.getString("status"));
            user.totalPoints = optInt(row, "totalPoints");
            user.currentStreak = optInt(row, "currentStreak");
            user.longestStreak = optInt(row, "longestStreak");
            user.twoFactorEnabled = row.optBoolean("twoFactorEnabled");
            return user;
        });
    }

    @Override
    public void updateName(String fullName, ApiCallback<Void> callback) {
        run(callback, () -> {
            JSONObject body = new JSONObject();
            body.put("fullName", fullName);
            send("PUT", "/me", body);
            return null;
        });
    }

    @Override
    public void changePassword(String currentPassword, String newPassword, ApiCallback<Void> callback) {
        run(callback, () -> {
            JSONObject body = new JSONObject();
            body.put("currentPassword", currentPassword);
            body.put("newPassword", newPassword);
            send("PUT", "/me/password", body);
            return null;
        });
    }

    @Override
    public void getMyBadges(ApiCallback<List<Badge>> callback) {
        run(callback, () -> {
            JSONArray rows = new JSONArray(send("GET", "/me/badges", null));
            List<Badge> badges = new ArrayList<>();
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                Badge badge = new Badge();
                badge.id = row.getInt("badgeId");
                badge.name = row.getString("name");
                badge.description = optString(row, "description");
                badge.imageUrl = optString(row, "imageUrl");
                badge.pointsAwarded = row.optInt("pointsAwarded");
                badge.awardedAt = optString(row, "awardedAt");
                badge.isHidden = row.optBoolean("isHidden");
                badges.add(badge);
            }
            return badges;
        });
    }

    // Courses

    @Override
    public void getCatalogue(ApiCallback<List<Course>> callback) {
        run(callback, () -> {
            JSONArray rows = new JSONArray(send("GET", "/courses", null));
            List<Course> courses = new ArrayList<>();
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                Course course = new Course();
                course.courseId = row.getInt("courseId");
                course.platformCourseId = row.getInt("platformCourseId");
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
                enrolment.pointsEarned = optInt(row, "pointsEarned");
                enrolments.add(enrolment);
            }
            return enrolments;
        });
    }

    // Goals

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

    @Override
    public void saveGoal(Integer goalId, String title, String goalType, Integer targetValue,
                         Integer targetCourseId, String targetDate, ApiCallback<Void> callback) {
        run(callback, () -> {
            JSONObject body = new JSONObject();
            body.put("title", title);
            body.put("goalType", goalType);
            body.put("targetValue", targetValue != null ? targetValue : JSONObject.NULL);
            body.put("targetCourseId", targetCourseId != null ? targetCourseId : JSONObject.NULL);
            body.put("targetDate", targetDate != null ? targetDate : JSONObject.NULL);
            if (goalId == null) {
                send("POST", "/goals", body);
            } else {
                send("PUT", "/goals/" + goalId, body);
            }
            return null;
        });
    }

    @Override
    public void logCheckIn(int goalId, String note, Integer valueLogged, ApiCallback<List<String>> callback) {
        run(callback, () -> {
            JSONObject body = new JSONObject();
            body.put("note", note != null ? note : JSONObject.NULL);
            body.put("valueLogged", valueLogged != null ? valueLogged : JSONObject.NULL);
            return names(new JSONObject(send("POST", "/goals/" + goalId + "/checkins", body)).optJSONArray("unlocked"));
        });
    }

    @Override
    public void reportEvent(String event, ApiCallback<List<String>> callback) {
        run(callback, () -> {
            JSONObject body = new JSONObject();
            body.put("event", event);
            return names(new JSONObject(send("POST", "/me/events", body)).optJSONArray("unlocked"));
        });
    }

    @Override
    public void getActiveDays(ApiCallback<List<String>> callback) {
        run(callback, () -> names(new JSONArray(send("GET", "/me/active-days", null))));
    }

    @Override
    public void getProgress(ApiCallback<List<ProgressSnapshot>> callback) {
        run(callback, () -> {
            JSONArray rows = new JSONArray(send("GET", "/me/progress", null));
            List<ProgressSnapshot> snapshots = new ArrayList<>();
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                ProgressSnapshot snapshot = new ProgressSnapshot();
                // "2026-10-22 18:00:00" becomes "2026-10-22"
                snapshot.day = row.getString("snapshotAt").substring(0, 10);
                snapshot.totalPoints = row.optInt("totalPoints");
                snapshot.coursesCompleted = row.optInt("coursesCompleted");
                snapshot.totalMinutes = row.optInt("totalMinutes");
                snapshots.add(snapshot);
            }
            return snapshots;
        });
    }

    @Override
    public void getSecrets(ApiCallback<int[]> callback) {
        run(callback, () -> {
            JSONObject row = new JSONObject(send("GET", "/me/secrets", null));
            return new int[]{row.optInt("found"), row.optInt("total")};
        });
    }

    // A JSON list of text, like ["Curious Mind"], as a Java list. Empty if there is none.
    private List<String> names(JSONArray array) throws Exception {
        List<String> names = new ArrayList<>();
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                names.add(array.getString(i));
            }
        }
        return names;
    }

    @Override
    public void getCheckIns(int goalId, ApiCallback<List<CheckIn>> callback) {
        run(callback, () -> {
            JSONArray rows = new JSONArray(send("GET", "/goals/" + goalId + "/checkins", null));
            List<CheckIn> checkIns = new ArrayList<>();
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                CheckIn checkIn = new CheckIn();
                checkIn.id = row.getInt("checkinId");
                checkIn.note = optString(row, "note");
                checkIn.valueLogged = optInt(row, "valueLogged");
                checkIn.loggedAt = row.getString("loggedAt");
                checkIns.add(checkIn);
            }
            return checkIns;
        });
    }

    // Leaderboard

    @Override
    public void getLeaderboard(boolean allTime, ApiCallback<List<LeaderboardEntry>> callback) {
        run(callback, () -> {
            JSONArray rows = new JSONArray(send("GET", allTime ? "/leaderboard?period=all" : "/leaderboard", null));
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

    // Study timetable

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

    // Admin

    @Override
    public void getAdminStats(ApiCallback<AdminStats> callback) {
        run(callback, () -> {
            JSONObject row = new JSONObject(send("GET", "/admin/stats", null));
            AdminStats stats = new AdminStats();
            stats.totalUsers = row.getInt("totalUsers");
            stats.activeCourses = row.getInt("activeCourses");
            stats.completionRate = row.getInt("completionRate");
            stats.engagement = row.getInt("engagement");
            stats.activeLearners = row.optInt("activeLearners");
            stats.totalMinutes = row.optInt("totalMinutes");
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


    @Override
    public void getCourseStats(ApiCallback<List<CourseStat>> callback) {
        run(callback, () -> {
            JSONArray rows = new JSONArray(send("GET", "/admin/course-stats", null));
            List<CourseStat> courseStats = new ArrayList<>();
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                CourseStat courseStat = new CourseStat();
                courseStat.courseName = row.getString("courseName");
                courseStat.enrolled = row.getInt("enrolled");
                courseStat.completed = row.getInt("completed");
                courseStats.add(courseStat);
            }
            return courseStats;
        });
    }

    @Override
    public void createUser(String email, String password, String fullName, String role,
                           ApiCallback<Void> callback) {
        run(callback, () -> {
            JSONObject body = new JSONObject();
            body.put("email", email);
            body.put("password", password);
            body.put("fullName", fullName);
            body.put("role", role);
            send("POST", "/admin/users", body);
            return null;
        });
    }

    @Override
    public void updateUser(int userId, String fullName, String role, ApiCallback<Void> callback) {
        run(callback, () -> {
            JSONObject body = new JSONObject();
            body.put("fullName", fullName);
            body.put("role", role);
            send("PUT", "/admin/users/" + userId, body);
            return null;
        });
    }

    @Override
    public void setUserStatus(int userId, String status, ApiCallback<Void> callback) {
        run(callback, () -> {
            JSONObject body = new JSONObject();
            body.put("status", status);
            send("PUT", "/admin/users/" + userId + "/status", body);
            return null;
        });
    }

    @Override
    public void linkUser(int userId, Integer idst, ApiCallback<Void> callback) {
        run(callback, () -> {
            JSONObject body = new JSONObject();
            body.put("idst", idst != null ? idst : JSONObject.NULL);
            send("PUT", "/admin/users/" + userId + "/link", body);
            return null;
        });
    }

    @Override
    public void updateCourse(int courseId, Integer pointsValue, Integer expectedMinutes,
                             ApiCallback<Void> callback) {
        run(callback, () -> {
            JSONObject body = new JSONObject();
            body.put("pointsValue", pointsValue != null ? pointsValue : JSONObject.NULL);
            body.put("expectedMinutes", expectedMinutes != null ? expectedMinutes : JSONObject.NULL);
            send("PUT", "/admin/courses/" + courseId, body);
            return null;
        });
    }

    @Override
    public void getAllBadges(ApiCallback<List<Badge>> callback) {
        run(callback, () -> {
            JSONArray rows = new JSONArray(send("GET", "/admin/badges", null));
            List<Badge> badges = new ArrayList<>();
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                Badge badge = new Badge();
                badge.id = row.getInt("badgeId");
                badge.name = row.getString("name");
                badge.description = optString(row, "description");
                badge.imageUrl = optString(row, "imageUrl");
                badge.ruleType = optString(row, "ruleType");
                badge.ruleValue = row.optInt("ruleValue");
                badge.pointsAwarded = row.optInt("pointsAwarded");
                badge.isActive = row.optBoolean("isActive", true);
                badges.add(badge);
            }
            return badges;
        });
    }

    @Override
    public void saveBadge(Integer badgeId, Badge badge, ApiCallback<Void> callback) {
        run(callback, () -> {
            JSONObject body = new JSONObject();
            body.put("name", badge.name);
            body.put("description", badge.description != null ? badge.description : JSONObject.NULL);
            body.put("imageUrl", badge.imageUrl != null ? badge.imageUrl : JSONObject.NULL);
            body.put("ruleType", badge.ruleType);
            body.put("ruleValue", badge.ruleValue);
            body.put("pointsAwarded", badge.pointsAwarded);
            body.put("isActive", badge.isActive);
            if (badgeId == null) {
                send("POST", "/admin/badges", body);
            } else {
                send("PUT", "/admin/badges/" + badgeId, body);
            }
            return null;
        });
    }

    @Override
    public void deleteBadge(int badgeId, ApiCallback<Void> callback) {
        run(callback, () -> {
            send("DELETE", "/admin/badges/" + badgeId, null);
            return null;
        });
    }

    @Override
    public void getSettings(ApiCallback<PlatformSettings> callback) {
        run(callback, () -> {
            JSONObject row = new JSONObject(send("GET", "/admin/settings", null));
            PlatformSettings settings = new PlatformSettings();
            settings.platformUrl = optString(row, "platformUrl");
            settings.apiKey = optString(row, "apiKey");
            settings.apiSecret = optString(row, "apiSecret");
            settings.lastTestedAt = optString(row, "lastTestedAt");
            settings.savedByAdmin = row.optBoolean("savedByAdmin");
            return settings;
        });
    }

    @Override
    public void saveSettings(String platformUrl, String apiKey, String apiSecret, ApiCallback<Void> callback) {
        run(callback, () -> {
            JSONObject body = new JSONObject();
            body.put("platformUrl", platformUrl);
            body.put("apiKey", apiKey != null ? apiKey : JSONObject.NULL);
            body.put("apiSecret", apiSecret != null ? apiSecret : JSONObject.NULL);
            send("PUT", "/admin/settings", body);
            return null;
        });
    }

    @Override
    public void testConnection(ApiCallback<Integer> callback) {
        run(callback, () -> new JSONObject(send("POST", "/admin/settings/test", null)).getInt("courses"));
    }

    @Override
    public void exportTable(String table, ApiCallback<String> callback) {
        run(callback, () -> send("GET", "/admin/export/" + table, null));
    }

    // Helpers

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
                mainThread.post(() -> callback.onError("Can't reach the server. Saved data can still be viewed, "
                        + "but changes need a connection. Check it and try again."));
            }
        });
    }

    // Like request(), but keeps a copy of the learner's own data (GETs outside
    // /admin) and gives that copy back when the backend can't be reached.
    private String send(String method, String path, JSONObject body) throws Exception {
        boolean keepsCopy = "GET".equals(method) && !path.startsWith("/admin");
        String text;
        try {
            text = request(method, path, body);
        } catch (IOException noConnection) {
            String copy = keepsCopy ? OfflineStore.loadCopy(path) : null;
            if (copy == null) {
                throw noConnection;
            }
            return copy;
        }
        if (keepsCopy) {
            OfflineStore.saveCopy(path, text);
        }
        return text;
    }

    // Sends a request to the backend and gives back the reply as text.
    // Sends the login token too, if someone is logged in.
    // If the backend replies with an error, throws ApiException with its message.
    private String request(String method, String path, JSONObject body) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(BASE_URL + path).openConnection();
        try {
            connection.setRequestMethod(method);
            // The hosted backend sleeps when nobody uses it, and the first request
            // wakes it up, which takes up to a minute. So it waits that long.
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(75000);
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

            // 401 while logged in: the saved login ran out after 7 days, or the
            // account was suspended. Forget it, so the app asks to log in again.
            if (status == 401 && Session.isLoggedIn() && !path.startsWith("/login")) {
                Session.logout();
                throw new ApiException("Your login ran out. Please log in again.");
            }

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
            // Line breaks are kept, so CSV exports stay one row per line
            while ((line = reader.readLine()) != null) {
                text.append(line).append('\n');
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
