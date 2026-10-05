package com.example.momentum.data;

import com.example.momentum.model.Course;
import com.example.momentum.model.Goal;
import com.example.momentum.model.LeaderboardEntry;
import com.example.momentum.model.Stats;
import com.example.momentum.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TEMPORARY. FAKE DATA. NOT THE REAL BACKEND.
// Everything here is either hardcoded or kept in memory only.
// Anything added (accounts, goals, check-ins) is LOST when the app closes.
// Nothing is saved to a database, and passwords are not hashed.
// This whole class gets replaced by the real backend later.
// Test accounts:
//   learner: thandi@test.com / password123
//   admin:   admin@test.com  / admin123
public class FakeMomentumApi implements MomentumApi {

    private final List<User> users = new ArrayList<>();
    private final Map<Integer, String> passwords = new HashMap<>();
    private final Map<Integer, List<Goal>> goals = new HashMap<>();
    private int nextUserId = 1;
    private int nextGoalId = 1;

    public FakeMomentumApi() {
        addUser(11836, "thandi@test.com", "password123", "Thandi Mokoena", "learner");
        addUser(11840, "sipho@test.com", "password123", "Sipho Dlamini", "learner");
        addUser(null, "admin@test.com", "admin123", "Ben Admin", "administrator");

        List<Goal> thandiGoals = new ArrayList<>();
        thandiGoals.add(new Goal(nextGoalId++, "Finish 3 courses this month", "complete_n_courses", 3, "2026-10-31", "active", 2));
        thandiGoals.add(new Goal(nextGoalId++, "Read for 15 minutes a day", "free_text", null, null, "active", 5));
        goals.put(1, thandiGoals);
    }

    private User addUser(Integer idst, String email, String password, String name, String role) {
        User user = new User(nextUserId++, idst, email, name, role, "active");
        users.add(user);
        passwords.put(user.id, password);
        goals.put(user.id, new ArrayList<>());
        return user;
    }

    private User findByEmail(String email) {
        for (User u : users) {
            if (u.email.equalsIgnoreCase(email)) return u;
        }
        return null;
    }

    // FAKE: checks a plain password in memory. The real one checks a hashed password in MySQL.
    @Override
    public void login(String email, String password, ApiCallback<User> callback) {
        User user = findByEmail(email);
        if (user == null || !password.equals(passwords.get(user.id))) {
            callback.onError("Wrong email or password");
        } else if ("suspended".equals(user.status)) {
            callback.onError("This account is suspended");
        } else {
            callback.onSuccess(user);
        }
    }

    // FAKE: the new account only exists until the app closes.
    @Override
    public void register(String email, String password, String fullName, ApiCallback<User> callback) {
        if (findByEmail(email) != null) {
            callback.onError("An account with this email already exists");
            return;
        }
        // New accounts start unlinked (no idst) until matched to LearnTech.
        callback.onSuccess(addUser(null, email, password, fullName, "learner"));
    }

    // HARDCODED: the same numbers for everyone. Nothing is calculated.
    @Override
    public void getStats(int userId, ApiCallback<Stats> callback) {
        callback.onSuccess(new Stats(115, 340, 4, 2));
    }

    // HARDCODED: the same list for everyone, copied from the client's API sample.
    @Override
    public void getCourses(int userId, ApiCallback<List<Course>> callback) {
        // Same courses as the sample in the client's API document.
        List<Course> list = new ArrayList<>();
        list.add(new Course(17, "Anger Management", "completed", 10, 12));
        list.add(new Course(2, "Delivering Constructive Criticism", "completed", 45, 51));
        list.add(new Course(88, "MCQ Assessment 1", "in_progress", 20, 9));
        list.add(new Course(6, "Leadership And Influence", "not_started", 60, 0));
        callback.onSuccess(list);
    }

    // IN MEMORY ONLY: goals are lost when the app closes.
    @Override
    public void getGoals(int userId, ApiCallback<List<Goal>> callback) {
        callback.onSuccess(new ArrayList<>(goalsFor(userId)));
    }

    @Override
    public void createGoal(int userId, Goal goal, ApiCallback<Goal> callback) {
        Goal saved = new Goal(nextGoalId++, goal.title, goal.goalType, goal.targetValue,
                goal.targetDate, "active", 0);
        goalsFor(userId).add(saved);
        callback.onSuccess(saved);
    }

    @Override
    public void deleteGoal(int goalId, ApiCallback<Void> callback) {
        for (List<Goal> list : goals.values()) {
            list.removeIf(g -> g.id == goalId);
        }
        callback.onSuccess(null);
    }

    // FAKE: only adds 1 to a counter. No streak or points are updated.
    @Override
    public void logCheckIn(int goalId, String note, ApiCallback<Goal> callback) {
        for (List<Goal> list : goals.values()) {
            for (Goal g : list) {
                if (g.id == goalId) {
                    g.checkIns++;
                    callback.onSuccess(g);
                    return;
                }
            }
        }
        callback.onError("Goal not found");
    }

    // HARDCODED: made up names and points. Nothing is ranked or calculated.
    @Override
    public void getLeaderboard(ApiCallback<List<LeaderboardEntry>> callback) {
        List<LeaderboardEntry> list = new ArrayList<>();
        list.add(new LeaderboardEntry(1, "Sipho Dlamini", 160));
        list.add(new LeaderboardEntry(2, "Thandi Mokoena", 115));
        list.add(new LeaderboardEntry(3, "Lerato Nkosi", 90));
        list.add(new LeaderboardEntry(4, "Pieter van Wyk", 45));
        callback.onSuccess(list);
    }

    @Override
    public void getAllUsers(ApiCallback<List<User>> callback) {
        callback.onSuccess(new ArrayList<>(users));
    }

    // IN MEMORY ONLY: the change is lost when the app closes.
    @Override
    public void setUserStatus(int userId, String status, ApiCallback<User> callback) {
        for (User u : users) {
            if (u.id == userId) {
                u.status = status;
                callback.onSuccess(u);
                return;
            }
        }
        callback.onError("User not found");
    }

    private List<Goal> goalsFor(int userId) {
        List<Goal> list = goals.get(userId);
        if (list == null) {
            list = new ArrayList<>();
            goals.put(userId, list);
        }
        return list;
    }
}
