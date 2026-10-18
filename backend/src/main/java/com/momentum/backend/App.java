package com.momentum.backend;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.momentum.backend.admin.AdminController;
import com.momentum.backend.auth.AuthController;
import com.momentum.backend.auth.TokenService;
import com.momentum.backend.config.Config;
import com.momentum.backend.courses.CourseController;
import com.momentum.backend.db.ActivityLogRepository;
import com.momentum.backend.db.BadgeRepository;
import com.momentum.backend.db.CheckInRepository;
import com.momentum.backend.db.CourseRepository;
import com.momentum.backend.db.Database;
import com.momentum.backend.db.EnrolmentRepository;
import com.momentum.backend.db.GoalRepository;
import com.momentum.backend.db.PointsRepository;
import com.momentum.backend.db.SnapshotRepository;
import com.momentum.backend.db.StreakRepository;
import com.momentum.backend.db.UserRepository;
import com.momentum.backend.goals.GoalController;
import com.momentum.backend.learntech.CoursesResponse;
import com.momentum.backend.learntech.LearnTechClient;
import com.momentum.backend.learntech.LearnTechException;
import com.momentum.backend.profile.ProfileController;
import com.momentum.backend.sync.SyncService;
import io.javalin.Javalin;

import java.util.Map;

/**
 * Starting point of the LearnTech Momentum backend.
 *
 * Starts the web server, connects to the database, and sets up the
 * endpoints the Android app calls. Each endpoint points to a method in
 * a controller class, for example AuthController for register and login.
 */
public class App {

    public static void main(String[] args) {
        // Load settings and connect to MySQL first. If either fails,
        // the backend stops here with a clear message.
        Config config = new Config();
        Database database = new Database(config);
        int port = config.getInt("server.port");

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

        // Talks to the LearnTech API (or the sample files, if base_url is "sample").
        LearnTechClient learnTechClient = new LearnTechClient(config);
        SyncService syncService = new SyncService(learnTechClient, courseRepository,
                userRepository, enrolmentRepository, snapshotRepository);

        // Controllers handle requests from the app.
        TokenService tokenService = new TokenService(config);
        AuthController authController = new AuthController(userRepository, tokenService);
        GoalController goalController = new GoalController(goalRepository, checkInRepository, courseRepository);
        ProfileController profileController = new ProfileController(userRepository, snapshotRepository);
        AdminController adminController = new AdminController(userRepository, courseRepository,
                badgeRepository, activityLogRepository);
        CourseController courseController = new CourseController(courseRepository);

        // In Javalin 7, the port and every route are set up inside this
        // block, before the server starts. The block's variable is called
        // javalinConfig because "config" is already used for our settings.
        Javalin app = Javalin.create(javalinConfig -> {
            javalinConfig.jetty.port = port;

            // -----------------------------------------------------
            // Health checks
            // Quick ways to confirm the backend is running.
            // Open these in a browser to test.
            // -----------------------------------------------------

            // Is the backend itself running?
            javalinConfig.routes.get("/health", ctx -> ctx.json(Map.of("status", "ok")));

            // Can the backend reach the database?
            // If this fails, first check MySQL is running: Get-Service MySQL80
            javalinConfig.routes.get("/health/db", ctx -> {
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
            javalinConfig.routes.get("/health/learntech", ctx -> {
                try {
                    CoursesResponse courses = learnTechClient.getCourses();
                    ctx.json(Map.of("learntech", "ok", "courses", courses.count));
                } catch (LearnTechException e) {
                    ctx.status(503).json(Map.of(
                        "learntech", "unreachable",
                        "reason", e.getMessage()));
                }
            });

            // -----------------------------------------------------
            // Register and login (see auth/AuthController.java)
            // Open to everyone, no token needed.
            // -----------------------------------------------------
            javalinConfig.routes.post("/register", authController::register);
            javalinConfig.routes.post("/login", authController::login);

            javalinConfig.routes.before(ctx -> {
                String header = ctx.header("Authorization");
                if (header != null && header.startsWith("Bearer ")) {
                    DecodedJWT token = tokenService.verifyToken(header.substring(7));
                    if (token != null) {
                        ctx.attribute("userId", Integer.parseInt(token.getSubject()));
                        ctx.attribute("role", token.getClaim("role").asString());
                    }
                }
            });

            // -----------------------------------------------------
            // Profile (see profile/ProfileController.java)
            // -----------------------------------------------------
            javalinConfig.routes.get("/me", profileController::getMe);
            javalinConfig.routes.put("/me", profileController::updateMe);
            javalinConfig.routes.get("/me/progress", profileController::getProgress);

            // -----------------------------------------------------
            // Courses (see courses/CourseController.java)
            // -----------------------------------------------------
            javalinConfig.routes.get("/courses", courseController::getCatalogue);

            // -----------------------------------------------------
            // Goals and check-ins (see goals/GoalController.java)
            // -----------------------------------------------------
            javalinConfig.routes.get("/goals", goalController::getAll);
            javalinConfig.routes.post("/goals", goalController::create);
            javalinConfig.routes.get("/goals/{id}", goalController::getOne);
            javalinConfig.routes.put("/goals/{id}", goalController::update);
            javalinConfig.routes.put("/goals/{id}/status", goalController::updateStatus);
            javalinConfig.routes.delete("/goals/{id}", goalController::delete);
            javalinConfig.routes.get("/goals/{id}/checkins", goalController::getCheckIns);
            javalinConfig.routes.post("/goals/{id}/checkins", goalController::checkIn);

            // -----------------------------------------------------
            // Admin (see admin/AdminController.java)
            // -----------------------------------------------------
            javalinConfig.routes.get("/admin/users", adminController::getUsers);
            javalinConfig.routes.get("/admin/users/{id}", adminController::getUser);
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
        });

        // Close the server and the database connections cleanly
        // when the backend is stopped, for example with Ctrl+C.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            database.close();
        }));

        app.start();
        System.out.println("Momentum backend running on http://localhost:" + port);

        // Sync courses, then learners, from LearnTech once at startup.
        // Courses go first, because enrolments need the courses to exist.
        // If LearnTech is down, the backend still runs, and the error is logged.
        try {
            int courses = syncService.syncCourses();
            System.out.println("Synced " + courses + " courses from LearnTech");

            int learners = syncService.syncLearners();
            System.out.println("Synced " + learners + " learners linked to app accounts");
        } catch (Exception e) {
            System.out.println("Sync failed: " + e.getMessage());
            try {
                // The action column holds at most 100 characters, so long messages are cut short.
                String action = "sync_failed: " + e.getMessage();
                if (action.length() > 100) {
                    action = action.substring(0, 100);
                }
                activityLogRepository.log(null, action, "error");
            } catch (Exception logError) {
                System.out.println("Could not write to the activity log either: " + logError.getMessage());
            }
        }
    }
}
