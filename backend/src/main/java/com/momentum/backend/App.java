package com.momentum.backend;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.momentum.backend.admin.AdminController;
import com.momentum.backend.admin.SettingsController;
import com.momentum.backend.auth.AuthController;
import com.momentum.backend.auth.CodeService;
import com.momentum.backend.auth.CurrentUser;
import com.momentum.backend.auth.TokenService;
import com.momentum.backend.backup.BackupService;
import com.momentum.backend.config.Config;
import com.momentum.backend.config.Encryption;
import com.momentum.backend.courses.CourseController;
import com.momentum.backend.db.ActivityLogRepository;
import com.momentum.backend.db.ActivityRepository;
import com.momentum.backend.db.BadgeRepository;
import com.momentum.backend.db.CheckInRepository;
import com.momentum.backend.db.CourseRepository;
import com.momentum.backend.db.Database;
import com.momentum.backend.db.EmailCodeRepository;
import com.momentum.backend.db.EnrolmentRepository;
import com.momentum.backend.db.ExportRepository;
import com.momentum.backend.db.GoalRepository;
import com.momentum.backend.db.PlatformConfigRepository;
import com.momentum.backend.db.PointsRepository;
import com.momentum.backend.db.SnapshotRepository;
import com.momentum.backend.db.StatsRepository;
import com.momentum.backend.db.StreakRepository;
import com.momentum.backend.db.StudySessionRepository;
import com.momentum.backend.db.UserRepository;
import com.momentum.backend.goals.GoalController;
import com.momentum.backend.leaderboard.LeaderboardController;
import com.momentum.backend.mail.EmailService;
import com.momentum.backend.model.User;
import com.momentum.backend.learntech.CoursesResponse;
import com.momentum.backend.learntech.LearnTechClient;
import com.momentum.backend.learntech.LearnTechException;
import com.momentum.backend.profile.ProfileController;
import com.momentum.backend.rewards.BadgeService;
import com.momentum.backend.rewards.EasterEggService;
import com.momentum.backend.rewards.GoalChecker;
import com.momentum.backend.rewards.StreakService;
import com.momentum.backend.sessions.StudySessionController;
import com.momentum.backend.sync.SyncService;
import io.javalin.Javalin;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// Starts the backend: the database, the web server, and the routes the app
// calls, each handled by a controller like AuthController.
public class App {

    public static void main(String[] args) {
        // Load settings and connect to MySQL first. If either fails,
        // the backend stops here with a clear message.
        Config config = new Config();
        Database database = new Database(config);
        // Hosting services choose the port and give it in PORT. On a laptop it's server.port.
        int port = System.getenv("PORT") != null
                ? Integer.parseInt(System.getenv("PORT"))
                : config.getInt("server.port");

        // Repositories read and save data, one per table (or pair of tables).
        UserRepository userRepository = new UserRepository(database);
        CourseRepository courseRepository = new CourseRepository(database);
        EnrolmentRepository enrolmentRepository = new EnrolmentRepository(database);
        GoalRepository goalRepository = new GoalRepository(database);
        CheckInRepository checkInRepository = new CheckInRepository(database);
        BadgeRepository badgeRepository = new BadgeRepository(database);
        PointsRepository pointsRepository = new PointsRepository(database);
        StreakRepository streakRepository = new StreakRepository(database);
        SnapshotRepository snapshotRepository = new SnapshotRepository(database);
        ActivityLogRepository activityLogRepository = new ActivityLogRepository(database);
        StudySessionRepository studySessionRepository = new StudySessionRepository(database);
        StatsRepository statsRepository = new StatsRepository(database);
        ExportRepository exportRepository = new ExportRepository(database);
        ActivityRepository activityRepository = new ActivityRepository(database);

        // The streak (check-ins and learning time) and the easter eggs.
        StreakService streakService = new StreakService(streakRepository, activityRepository);
        EasterEggService easterEggService = new EasterEggService(badgeRepository, activityRepository,
                pointsRepository, activityLogRepository);

        // Completes goals and awards badges, after a sync or a check-in.
        GoalChecker goalChecker = new GoalChecker(goalRepository, enrolmentRepository,
                streakRepository, pointsRepository, activityLogRepository);
        BadgeService badgeService = new BadgeService(badgeRepository, enrolmentRepository,
                streakRepository, pointsRepository, activityLogRepository);

        // Talks to the LearnTech API (or the sample files, if base_url is "sample").
        LearnTechClient learnTechClient = new LearnTechClient(config);
        SyncService syncService = new SyncService(learnTechClient, courseRepository,
                userRepository, enrolmentRepository, snapshotRepository, pointsRepository,
                activityLogRepository, streakRepository, goalChecker, badgeService,
                streakService, activityRepository);

        // The admin's LearnTech settings, saved encrypted in the database.
        // If there are any, they are used instead of config.properties.
        PlatformConfigRepository platformConfigRepository = new PlatformConfigRepository(database);
        Encryption encryption = new Encryption(config);
        SettingsController settingsController = new SettingsController(platformConfigRepository,
                activityLogRepository, learnTechClient, encryption);
        settingsController.loadSavedSettings();

        // Emailed codes, for two-step login and password resets.
        CodeService codeService = new CodeService(new EmailCodeRepository(database), new EmailService(config));

        // Controllers handle requests from the app.
        TokenService tokenService = new TokenService(config);
        AuthController authController = new AuthController(userRepository, tokenService,
                activityLogRepository, syncService, codeService);
        GoalController goalController = new GoalController(goalRepository, checkInRepository, courseRepository,
                streakService, goalChecker, badgeService, easterEggService);
        ProfileController profileController = new ProfileController(userRepository, snapshotRepository, pointsRepository,
                streakRepository, badgeRepository, codeService, activityLogRepository,
                activityRepository, easterEggService);
        AdminController adminController = new AdminController(userRepository, courseRepository,
                badgeRepository, activityLogRepository, statsRepository, exportRepository);
        CourseController courseController = new CourseController(courseRepository, enrolmentRepository);
        StudySessionController studySessionController = new StudySessionController(studySessionRepository, courseRepository);
        LeaderboardController leaderboardController = new LeaderboardController(pointsRepository);

        // In Javalin 7, the port and every route are set up inside this
        // block, before the server starts. The block's variable is called
        // javalinConfig because "config" is already used for our settings.
        Javalin app = Javalin.create(javalinConfig -> {
            javalinConfig.jetty.port = port;

            // Health checks
            // Quick ways to confirm the backend is running.
            // Open these in a browser to test.

            // Is the backend itself running?
            javalinConfig.routes.get("/health", ctx -> ctx.json(Map.of("status", "ok")));

            // For UptimeRobot: also checks the database, so a database that's off
            // shows as down, and the database gets used every few minutes.
            javalinConfig.routes.get("/health/full", ctx -> {
                if (database.isReachable()) {
                    ctx.json(Map.of("status", "ok"));
                } else {
                    ctx.status(503).json(Map.of("status", "database unreachable"));
                }
            });

            // Can the backend reach the database? Admins only, so nobody else can
            // keep making it open connections.
            javalinConfig.routes.get("/health/db", ctx -> {
                if (!CurrentUser.isAdmin(ctx)) {
                    ctx.status(403).json(Map.of("error", "administrators only"));
                    return;
                }
                if (database.isReachable()) {
                    ctx.json(Map.of("database", "ok"));
                } else {
                    ctx.status(503).json(Map.of(
                        "database", "unreachable",
                        "hint", "Check MySQL is running and the password in config.properties is right"));
                }
            });

            // Can the backend reach the LearnTech API?
            // Only says if it worked and how many courses there are, no other data.
            // Admins only, because every call makes a request to LearnTech.
            javalinConfig.routes.get("/health/learntech", ctx -> {
                if (!CurrentUser.isAdmin(ctx)) {
                    ctx.status(403).json(Map.of("error", "administrators only"));
                    return;
                }
                try {
                    CoursesResponse courses = learnTechClient.getCourses();
                    ctx.json(Map.of("learntech", "ok", "courses", courses.count));
                } catch (LearnTechException e) {
                    ctx.status(503).json(Map.of(
                        "learntech", "unreachable",
                        "reason", e.getMessage()));
                }
            });

            // Register and login (see auth/AuthController.java)
            // Open to everyone, no token needed.
            javalinConfig.routes.post("/register", authController::register);
            javalinConfig.routes.post("/login", authController::login);
            javalinConfig.routes.post("/login/verify", authController::verifyLogin);
            javalinConfig.routes.post("/password/forgot", authController::forgotPassword);
            javalinConfig.routes.post("/password/reset", authController::resetPassword);

            // Runs before every request: if the app sent a valid login token, notes
            // who is asking so the routes can check it.
            javalinConfig.routes.before(ctx -> {
                String header = ctx.header("Authorization");
                if (header != null && header.startsWith("Bearer ")) {
                    DecodedJWT token = tokenService.verifyToken(header.substring(7));
                    if (token != null) {
                        // The account must still exist and be active, and the role is read
                        // fresh, so a suspend, delete or role change works straight away
                        // instead of only when the 7-day token runs out.
                        User user = userRepository.findUserById(Integer.parseInt(token.getSubject()));
                        if (user != null && "active".equals(user.status)) {
                            ctx.attribute("userId", user.userId);
                            ctx.attribute("role", user.role);
                        }
                    }
                }
            });

            // Profile (see profile/ProfileController.java)
            javalinConfig.routes.get("/me", profileController::getMe);
            javalinConfig.routes.put("/me", profileController::updateMe);
            javalinConfig.routes.put("/me/password", profileController::changePassword);
            javalinConfig.routes.post("/me/two-factor/start", profileController::startTwoFactor);
            javalinConfig.routes.post("/me/two-factor/confirm", profileController::confirmTwoFactor);
            javalinConfig.routes.post("/me/two-factor/disable", profileController::disableTwoFactor);
            javalinConfig.routes.get("/me/progress", profileController::getProgress);
            javalinConfig.routes.get("/me/badges", profileController::getBadges);
            javalinConfig.routes.post("/me/events", profileController::recordEvent);
            javalinConfig.routes.get("/me/active-days", profileController::getActiveDays);
            javalinConfig.routes.get("/me/secrets", profileController::getSecrets);

            // Courses (see courses/CourseController.java)
            javalinConfig.routes.get("/courses", courseController::getCatalogue);
            javalinConfig.routes.get("/me/courses", courseController::getMyCourses);

            // Study timetable (see sessions/StudySessionController.java)
            javalinConfig.routes.get("/study-sessions", studySessionController::getAll);
            javalinConfig.routes.post("/study-sessions", studySessionController::create);
            javalinConfig.routes.put("/study-sessions/{id}", studySessionController::update);
            javalinConfig.routes.delete("/study-sessions/{id}", studySessionController::delete);

            // Goals and check-ins (see goals/GoalController.java)
            javalinConfig.routes.get("/goals", goalController::getAll);
            javalinConfig.routes.post("/goals", goalController::create);
            javalinConfig.routes.get("/goals/{id}", goalController::getOne);
            javalinConfig.routes.put("/goals/{id}", goalController::update);
            javalinConfig.routes.put("/goals/{id}/status", goalController::updateStatus);
            javalinConfig.routes.delete("/goals/{id}", goalController::delete);
            javalinConfig.routes.get("/goals/{id}/checkins", goalController::getCheckIns);
            javalinConfig.routes.post("/goals/{id}/checkins", goalController::checkIn);

            // Leaderboard (see leaderboard/LeaderboardController.java)
            javalinConfig.routes.get("/leaderboard", leaderboardController::getLeaderboard);

            // Admin (see admin/AdminController.java)
            javalinConfig.routes.get("/admin/stats", adminController::getStats);
            javalinConfig.routes.get("/admin/course-stats", adminController::getCourseStats);
            javalinConfig.routes.get("/admin/users", adminController::getUsers);
            javalinConfig.routes.post("/admin/users", adminController::createUser);
            javalinConfig.routes.get("/admin/users/{id}", adminController::getUser);
            javalinConfig.routes.put("/admin/users/{id}", adminController::updateUser);
            javalinConfig.routes.put("/admin/users/{id}/status", adminController::setUserStatus);
            javalinConfig.routes.put("/admin/users/{id}/link", adminController::linkUser);
            javalinConfig.routes.delete("/admin/users/{id}", adminController::deleteUser);

            javalinConfig.routes.get("/admin/courses", adminController::getCourses);
            javalinConfig.routes.get("/admin/courses/{id}", adminController::getCourse);
            javalinConfig.routes.put("/admin/courses/{id}", adminController::updateCourse);

            javalinConfig.routes.get("/admin/badges", adminController::getBadges);
            javalinConfig.routes.post("/admin/badges", adminController::createBadge);
            javalinConfig.routes.get("/admin/badges/{id}", adminController::getBadge);
            javalinConfig.routes.put("/admin/badges/{id}", adminController::updateBadge);
            javalinConfig.routes.delete("/admin/badges/{id}", adminController::deleteBadge);

            javalinConfig.routes.get("/admin/logs", adminController::getLogs);
            javalinConfig.routes.get("/admin/export/{table}", adminController::exportTable);

            // LearnTech API settings (see admin/SettingsController.java)
            javalinConfig.routes.get("/admin/settings", settingsController::getSettings);
            javalinConfig.routes.put("/admin/settings", settingsController::saveSettings);
            javalinConfig.routes.post("/admin/settings/test", settingsController::testConnection);
        });

        // Close the server and the database connections cleanly
        // when the backend is stopped, for example with Ctrl+C.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            database.close();
        }));

        app.start();
        System.out.println("Momentum backend running on http://localhost:" + port);

        // Sync from LearnTech now, and then every 30 minutes.
        // If LearnTech is down, the backend still runs, and the error is logged.
        // Daemon means this thread doesn't keep the backend running after Ctrl+C.
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task);
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleAtFixedRate(syncService::syncAll, 0, SYNC_EVERY_MINUTES, TimeUnit.MINUTES);

        // Daily backup, checked every hour. Off when hosted (backup.enabled=false),
        // where the database service makes its own.
        if (!"false".equalsIgnoreCase(config.getOptional("backup.enabled"))) {
            BackupService backupService = new BackupService(config, activityLogRepository);
            scheduler.scheduleAtFixedRate(backupService::backupIfDue, 1, 60, TimeUnit.MINUTES);
        }
    }

    // How often the scheduled sync runs.
    private static final int SYNC_EVERY_MINUTES = 30;
}
