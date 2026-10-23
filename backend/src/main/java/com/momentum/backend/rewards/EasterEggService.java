package com.momentum.backend.rewards;

import com.momentum.backend.config.AppClock;
import com.momentum.backend.db.ActivityLogRepository;
import com.momentum.backend.db.ActivityRepository;
import com.momentum.backend.db.BadgeRepository;
import com.momentum.backend.db.PointsRepository;
import com.momentum.backend.model.Badge;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// Easter eggs: hidden badges (is_hidden, rule type "secret_...") for exploring the app.
//   Curious Mind               tap the logo on Home 7 times
//   Master Planner             on one day: open the study timetable, talk to a coach, log a check-in
//   Night Owl                  log a check-in between midnight and 4 am
//   Bartholomew's Apprentice   ask the Study Coach about Bartholomew Crownelius the Third
public class EasterEggService {

    // The only events the app may report. Anything else is ignored.
    private static final Set<String> EVENTS = Set.of(
        "logo_tapped", "opened_timetable", "opened_coach", "asked_bartholomew");

    private final BadgeRepository badgeRepository;
    private final ActivityRepository activityRepository;
    private final PointsRepository pointsRepository;
    private final ActivityLogRepository activityLogRepository;

    public EasterEggService(BadgeRepository badgeRepository, ActivityRepository activityRepository,
                            PointsRepository pointsRepository, ActivityLogRepository activityLogRepository) {
        this.badgeRepository = badgeRepository;
        this.activityRepository = activityRepository;
        this.pointsRepository = pointsRepository;
        this.activityLogRepository = activityLogRepository;
    }

    // Saves something the learner did in the app, and gives back the names of
    // any secret badges it just unlocked.
    public List<String> recordEvent(int userId, String event) throws SQLException {
        List<String> unlocked = new ArrayList<>();
        if (!EVENTS.contains(event)) {
            return unlocked;
        }
        activityRepository.saveEvent(userId, event);

        if ("logo_tapped".equals(event)) {
            award(userId, "secret_curious", unlocked);
        } else if ("asked_bartholomew".equals(event)) {
            award(userId, "secret_bartholomew", unlocked);
        }
        checkMasterPlanner(userId, unlocked);
        return unlocked;
    }

    // After a check-in: the Night Owl and Master Planner eggs.
    public List<String> afterCheckIn(int userId) throws SQLException {
        List<String> unlocked = new ArrayList<>();
        int hour = AppClock.now().getHour();
        if (hour < 4) {
            award(userId, "secret_night_owl", unlocked);
        }
        checkMasterPlanner(userId, unlocked);
        return unlocked;
    }

    // Timetable, a coach and a check-in, all on the same day.
    private void checkMasterPlanner(int userId, List<String> unlocked) throws SQLException {
        LocalDate today = AppClock.today();
        if (activityRepository.didEvent(userId, "opened_timetable", today)
                && activityRepository.didEvent(userId, "opened_coach", today)
                && activityRepository.wasActive(userId, today, "checkin")) {
            award(userId, "secret_planner", unlocked);
        }
    }

    // Gives the secret badge and its points, once. Adds its name to the list if it's new.
    private void award(int userId, String ruleType, List<String> unlocked) throws SQLException {
        Badge badge = badgeRepository.findBadgeByRuleType(ruleType);
        if (badge == null || !badge.isActive) {
            return;
        }
        if (badgeRepository.awardBadge(userId, badge.badgeId)) {
            pointsRepository.awardPoints(userId, badge.pointsAwarded, "badge_awarded",
                    badge.badgeId, AppClock.today().toString().substring(0, 7));
            activityLogRepository.log(userId, "secret found: " + badge.name, "info");
            unlocked.add(badge.name);
        }
    }
}
