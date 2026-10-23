package com.momentum.backend.sync;

import com.momentum.backend.config.AppClock;
import com.momentum.backend.db.ActivityLogRepository;
import com.momentum.backend.db.ActivityRepository;
import com.momentum.backend.db.CourseRepository;
import com.momentum.backend.db.EnrolmentRepository;
import com.momentum.backend.db.PointsRepository;
import com.momentum.backend.db.SnapshotRepository;
import com.momentum.backend.db.StreakRepository;
import com.momentum.backend.db.UserRepository;
import com.momentum.backend.learntech.ApiCourse;
import com.momentum.backend.learntech.ApiEnrolment;
import com.momentum.backend.learntech.ApiLearner;
import com.momentum.backend.learntech.CoursesResponse;
import com.momentum.backend.learntech.LearnTechClient;
import com.momentum.backend.learntech.LearnTechException;
import com.momentum.backend.learntech.LearnersResponse;
import com.momentum.backend.model.Course;
import com.momentum.backend.model.User;
import com.momentum.backend.rewards.BadgeService;
import com.momentum.backend.rewards.GoalChecker;
import com.momentum.backend.rewards.StreakService;

import java.sql.SQLException;
import java.time.YearMonth;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

// Copies data from LearnTech into our database on a schedule, so the app
// never waits on LearnTech.
public class SyncService {

    private final LearnTechClient learnTechClient;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrolmentRepository enrolmentRepository;
    private final SnapshotRepository snapshotRepository;
    private final PointsRepository pointsRepository;
    private final ActivityLogRepository activityLogRepository;
    private final StreakRepository streakRepository;
    private final GoalChecker goalChecker;
    private final BadgeService badgeService;
    private final StreakService streakService;
    private final ActivityRepository activityRepository;

    // One background thread for login syncs. Daemon means it doesn't keep
    // the backend running after Ctrl+C.
    private final ExecutorService background = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        return thread;
    });

    public SyncService(LearnTechClient learnTechClient, CourseRepository courseRepository,
                       UserRepository userRepository, EnrolmentRepository enrolmentRepository,
                       SnapshotRepository snapshotRepository, PointsRepository pointsRepository,
                       ActivityLogRepository activityLogRepository, StreakRepository streakRepository,
                       GoalChecker goalChecker, BadgeService badgeService,
                       StreakService streakService, ActivityRepository activityRepository) {
        this.learnTechClient = learnTechClient;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.enrolmentRepository = enrolmentRepository;
        this.snapshotRepository = snapshotRepository;
        this.pointsRepository = pointsRepository;
        this.activityLogRepository = activityLogRepository;
        this.streakRepository = streakRepository;
        this.goalChecker = goalChecker;
        this.badgeService = badgeService;
        this.streakService = streakService;
        this.activityRepository = activityRepository;
    }

    // Saves LearnTech's whole course catalogue into the Courses table.
    // New courses are added, existing ones are updated (saveCourse does both).
    // Gives back how many courses were saved.
    public int syncCourses() throws LearnTechException, SQLException {

        CoursesResponse response = learnTechClient.getCourses();
        int saved = 0;

        for (ApiCourse course : response.courses) {
            courseRepository.saveCourse(course.courseId, course.courseName, course.expectedMinutes);
            saved++;
        }

        return saved;
    }

    // Goes through every learner on LearnTech. If one of our app users has the
    // same email, they get linked and their courses are saved.
    // Learners who never registered in the app are skipped.
    // Gives back how many learners were linked to an app account.
    public int syncLearners() throws LearnTechException, SQLException {

        LearnersResponse response = learnTechClient.getAllLearners();
        int linked = 0;

        for (ApiLearner learner : response.learners) {
            if (syncLearner(learner)) {
                linked++;
            }
        }

        return linked;
    }

    // Syncs courses, then every learner, and logs the result. Never throws, so a
    // failed sync can't stop the schedule, and only one runs at a time.
    public synchronized void syncAll() {
        try {
            int courses = syncCourses();
            int learners = syncLearners();
            System.out.println("Synced " + courses + " courses and " + learners + " linked learners");
            activityLogRepository.log(null, "sync: " + courses + " courses, " + learners + " learners", "info");
        } catch (Exception e) {
            System.out.println("Sync failed: " + e.getMessage());
            try {
                activityLogRepository.log(null, "sync_failed: " + e.getMessage(), "error");
            } catch (Exception logError) {
                System.out.println("Could not write to the activity log either: " + logError.getMessage());
            }
        }
    }

    // Runs syncUser in the background, so logging in doesn't wait on LearnTech.
    public void syncUserInBackground(User user) {
        background.execute(() -> syncUser(user));
    }

    // Syncs one user when they log in, so their courses and points are up to date.
    // A linked user is fetched by their idst. An unlinked user is looked for by
    // email in the full list, so someone who just registered gets linked straight away.
    public synchronized void syncUser(User user) {
        try {
            LearnersResponse response = user.idst != null
                    ? learnTechClient.getLearner(user.idst)
                    : learnTechClient.getAllLearners();

            for (ApiLearner learner : response.learners) {
                boolean theirs = user.idst != null
                        ? learner.idst == user.idst
                        : learner.email != null && learner.email.equalsIgnoreCase(user.email);
                if (theirs) {
                    syncLearner(learner);
                }
            }
        } catch (Exception e) {
            System.out.println("Login sync failed: " + e.getMessage());
            try {
                activityLogRepository.log(user.userId, "login sync_failed: " + e.getMessage(), "warning");
            } catch (Exception logError) {
                System.out.println("Could not write to the activity log either: " + logError.getMessage());
            }
        }
    }

    // Saves one LearnTech learner: links them to the app account with the same
    // email, saves their courses, awards points and records a snapshot.
    // Gives back false if they were skipped (no account, or already linked elsewhere).
    private boolean syncLearner(ApiLearner learner) throws SQLException {

        // The account already linked to this learner (by the sync or by the admin).
        // If there is none yet, look for an account with the same email.
        User user = userRepository.findUserByIdst(learner.idst);
        if (user == null) {
            user = userRepository.findUserByEmail(learner.email);
        }
        if (user == null) {
            return false;   // no app account for this learner
        }

        // The email matched, but the admin linked this account to a different learner.
        if (user.idst != null && user.idst != learner.idst) {
            return false;
        }

        // Link the account the first time we see a match.
        if (user.idst == null) {
            if (userRepository.idstTaken(learner.idst)) {
                return false;   // this learner is already linked to a different account
            }
            userRepository.updateIdst(user.userId, learner.idst);
        }

        for (ApiEnrolment enrolment : learner.courses) {

            // Our own id for this course. If the course isn't in our catalogue yet, add it first.
            Course course = courseRepository.findCourseByPlatformId(enrolment.courseId);
            if (course == null) {
                courseRepository.saveCourse(enrolment.courseId, enrolment.courseName, enrolment.expectedMinutes);
                course = courseRepository.findCourseByPlatformId(enrolment.courseId);
            }

            // The assessment can be null, so only read inside it if it's there.
            String assessmentStatus = null;
            Double score = null;
            Double scoreMax = null;
            if (enrolment.assessment != null) {
                assessmentStatus = enrolment.assessment.status;
                score = enrolment.assessment.score;
                scoreMax = enrolment.assessment.scoreMax;
            }

            // Minutes spent in this course since the last sync, for the streak.
            // Null the first time, because then it's unknown when they were spent.
            Integer minutesBefore = enrolmentRepository.findTimeSpent(user.userId, course.courseId);

            enrolmentRepository.saveEnrolment(user.userId, course.courseId, enrolment.state,
                    enrolment.timeSpentMinutes, assessmentStatus, score, scoreMax,
                    enrolment.firstAccess, enrolment.dateCompleted);

            // Today counts towards the streak once the learner has spent at least 35%
            // of the course's expected time in it today.
            if (minutesBefore != null && enrolment.timeSpentMinutes > minutesBefore
                    && course.expectedMinutes != null && course.expectedMinutes > 0) {
                int today = activityRepository.addLearningMinutes(user.userId, course.courseId,
                        AppClock.today(), enrolment.timeSpentMinutes - minutesBefore);
                if (today >= Math.ceil(LEARNING_SHARE * course.expectedMinutes)) {
                    streakService.recordActiveDay(user.userId, "learning");
                }
            }

            // Completed courses earn points. The points table only allows
            // one award per course per learner, so a re-sync never doubles it.
            // 0 points means the completion doesn't count (yet), so nothing is saved,
            // and it's checked again on the next sync.
            if ("completed".equals(enrolment.state)) {
                int points = pointsFor(course, enrolment.timeSpentMinutes);
                if (points > 0) {
                    String monthKey = enrolment.dateCompleted != null
                            ? enrolment.dateCompleted.substring(0, 7)
                            : YearMonth.now().toString();
                    pointsRepository.awardPoints(user.userId, points,
                            "course_completed", course.courseId, monthKey);
                }
            }
        }

        // Any goals and badges reached by these courses.
        // Before the snapshot, so their points are in it.
        goalChecker.checkGoals(user.userId);
        badgeService.checkBadges(user.userId);

        // Records where the learner stands right now. LearnTech keeps no
        // history, so this is the only history there will be.
        snapshotRepository.insertSnapshot(user.userId,
                learner.summary.coursesEnrolled,
                learner.summary.coursesCompleted,
                learner.summary.totalTimeMinutes,
                pointsRepository.totalPoints(user.userId),
                streakRepository.activeStreak(user.userId));

        return true;
    }

    // How many points a completed course is worth, using LearnTech's formula:
    // the course's expected minutes, but only if the learner spent at least
    // 70% of that time in it. Rushed completions give 0.
    // Time is only a check, it's never the score itself.
    private int pointsFor(Course course, int timeSpentMinutes) {
        if (course.platformCourseId == FAST_COURSE_ID) {
            // DEMO: Fast Course is a 10-second course for showing points live.
            // The API only counts whole minutes, so it always shows 0 minutes
            // spent and the time check could never pass. It gets a random score instead.
            return ThreadLocalRandom.current().nextInt(1, 31);
        } else if (course.expectedMinutes == null) {
            // No duration to check against: the admin's points value, otherwise a flat 10.
            return course.pointsValue != null ? course.pointsValue : 10;
        } else if (timeSpentMinutes < MIN_TIME_SHARE * course.expectedMinutes) {
            return 0;
        } else if (course.pointsValue != null) {
            return course.pointsValue;
        } else {
            return course.expectedMinutes;
        }
    }

    // LearnTech's course_id for Fast Course, the demo course.
    private static final int FAST_COURSE_ID = 58;

    // The share of a course's expected minutes a learner has to spend in it
    // for a completion to earn points.
    private static final double MIN_TIME_SHARE = 0.7;

    // The share of a course's expected minutes spent in it in one day that
    // counts that day towards the streak.
    private static final double LEARNING_SHARE = 0.35;
}
