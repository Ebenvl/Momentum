package com.momentum.backend.rewards;

import com.momentum.backend.config.AppClock;
import com.momentum.backend.db.ActivityRepository;
import com.momentum.backend.db.StreakRepository;
import com.momentum.backend.model.Streak;

import java.sql.SQLException;
import java.time.LocalDate;

// The daily streak. A day counts when the learner either logs a check-in, or
// spends at least 35% of a course's expected time in it on LearnTech that day.
// Check-ins are called from GoalController, learning time from the sync.
public class StreakService {

    private final StreakRepository streakRepository;
    private final ActivityRepository activityRepository;

    public StreakService(StreakRepository streakRepository, ActivityRepository activityRepository) {
        this.streakRepository = streakRepository;
        this.activityRepository = activityRepository;
    }

    // Records that today counted ("checkin" or "learning"), and updates the streak.
    public void recordActiveDay(int userId, String source) throws SQLException {
        activityRepository.saveActiveDay(userId, AppClock.today(), source);
        updateStreak(userId);
    }

    // A new day adds 1, a missed day starts again at 1, the same day changes nothing.
    // The longest streak is kept.
    private void updateStreak(int userId) throws SQLException {

        LocalDate today = AppClock.today();
        Streak streak = streakRepository.findStreak(userId);

        int current;
        int longest;

        if (streak == null || streak.lastActivityDate == null) {
            current = 1;
            longest = 1;
        } else {
            LocalDate lastDay = LocalDate.parse(streak.lastActivityDate);

            if (lastDay.equals(today)) {
                return;
            } else if (lastDay.equals(today.minusDays(1))) {
                current = streak.currentStreak + 1;
            } else {
                current = 1;
            }

            longest = Math.max(streak.longestStreak, current);
        }

        streakRepository.saveStreak(userId, current, longest, today.toString());
    }
}
