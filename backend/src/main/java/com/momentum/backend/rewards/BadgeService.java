package com.momentum.backend.rewards;

import com.momentum.backend.config.AppClock;
import com.momentum.backend.db.ActivityLogRepository;
import com.momentum.backend.db.BadgeRepository;
import com.momentum.backend.db.EnrolmentRepository;
import com.momentum.backend.db.PointsRepository;
import com.momentum.backend.db.StreakRepository;
import com.momentum.backend.model.Badge;

import java.sql.SQLException;

// Awards badges when a learner reaches a badge's rule.
// Runs after every sync of a learner and after every check-in.
public class BadgeService {

    private final BadgeRepository badgeRepository;
    private final EnrolmentRepository enrolmentRepository;
    private final StreakRepository streakRepository;
    private final PointsRepository pointsRepository;
    private final ActivityLogRepository activityLogRepository;

    public BadgeService(BadgeRepository badgeRepository, EnrolmentRepository enrolmentRepository,
                        StreakRepository streakRepository, PointsRepository pointsRepository,
                        ActivityLogRepository activityLogRepository) {
        this.badgeRepository = badgeRepository;
        this.enrolmentRepository = enrolmentRepository;
        this.streakRepository = streakRepository;
        this.pointsRepository = pointsRepository;
        this.activityLogRepository = activityLogRepository;
    }

    // Checks every active badge against this learner, and awards the ones they've reached.
    public void checkBadges(int userId) throws SQLException {

        for (Badge badge : badgeRepository.findActiveBadges()) {
            int value;
            if ("courses_completed".equals(badge.ruleType)) {
                value = enrolmentRepository.countCompleted(userId);
            } else if ("streak_days".equals(badge.ruleType)) {
                value = streakRepository.activeStreak(userId);
            } else if ("total_points".equals(badge.ruleType)) {
                value = pointsRepository.totalPoints(userId);
            } else {
                continue;
            }

            if (value >= badge.ruleValue) {
                boolean isNew = badgeRepository.awardBadge(userId, badge.badgeId);

                if (isNew) {
                    pointsRepository.awardPoints(userId, badge.pointsAwarded, "badge_awarded",
                            badge.badgeId, AppClock.today().toString().substring(0, 7));
                    activityLogRepository.log(userId, "badge earned: " + badge.name, "info");
                }
            }

        }

    }
}
