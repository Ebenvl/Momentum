package com.momentum.backend;

import com.momentum.backend.config.Config;
import com.momentum.backend.db.Database;
import com.momentum.backend.db.UserRepository;
import com.momentum.backend.model.LoginRequest;
import com.momentum.backend.model.RegisterRequest;
import com.momentum.backend.model.User;
import io.javalin.Javalin;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.util.Map;

/**
 * Starting point of the LearnTech Momentum backend.
 *
 * Starts the web server, connects to the database, and sets up the
 * endpoints the Android app calls. More endpoints get added here as
 * each feature is built, for example register, login and goals.
 */
public class App {

    public static void main(String[] args) {
        // Load settings and connect to MySQL first. If either fails,
        // the backend stops here with a clear message.
        Config config = new Config();
        Database database = new Database(config);
        UserRepository userRepository = new UserRepository(database);
        int port = config.getInt("server.port");

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

            // -----------------------------------------------------
            // Register
            // -----------------------------------------------------

            javalinConfig.routes.post("/register", ctx -> {

                // Read the details the app sent. If it isn't proper JSON, stop here.
                RegisterRequest request;
                try {
                    request = ctx.bodyAsClass(RegisterRequest.class);
                } catch (Exception e) {
                    ctx.status(400).json(Map.of("error", "invalid request"));
                    return;
                }

                // Check nothing was left empty.
                if (request.email == null || request.email.isBlank()) {
                    ctx.status(400).json(Map.of("error", "email is required"));
                    return;
                }
                if (request.password == null || request.password.isBlank()) {
                    ctx.status(400).json(Map.of("error", "password is required"));
                    return;
                }
                if (request.fullName == null || request.fullName.isBlank()) {
                    ctx.status(400).json(Map.of("error", "full name is required"));
                    return;
                }

                // Basic format checks.
                if (!request.email.contains("@")) {
                    ctx.status(400).json(Map.of("error", "email is not valid"));
                    return;
                }
                if (request.password.length() < 8) {
                    ctx.status(400).json(Map.of("error", "password must be at least 8 characters"));
                    return;
                }

                try {
                    // Stop here if someone already registered with this email.
                    if (userRepository.emailExists(request.email)) {
                        ctx.status(409).json(Map.of("error", "email already registered"));
                        return;
                    }

                    String passwordHash = BCrypt.hashpw(request.password, BCrypt.gensalt());
                    boolean saved = userRepository.insertUser(request.email, passwordHash, request.fullName);
                    if (saved) {
                        ctx.json(Map.of("message", "registered"));
                    } else {
                        ctx.status(500).json(Map.of("error", "could not register"));
                    }
                } catch (SQLException e) {
                    ctx.status(500).json(Map.of("error", "something went wrong, try again"));
                }

            });

            // -----------------------------------------------------
            // Login
            // -----------------------------------------------------

            javalinConfig.routes.post("/login", ctx -> {

                // Read the details the app sent. If it isn't proper JSON, stop here.
                LoginRequest request;
                try {
                    request = ctx.bodyAsClass(LoginRequest.class);
                } catch (Exception e) {
                    ctx.status(400).json(Map.of("error", "invalid request"));
                    return;
                }

                // Check nothing was left empty.
                if (request.email == null || request.email.isBlank()) {
                    ctx.status(400).json(Map.of("error", "email is required"));
                    return;
                }
                if (request.password == null || request.password.isBlank()) {
                    ctx.status(400).json(Map.of("error", "password is required"));
                    return;
                }

                try {
                    User user = userRepository.findUserByEmail(request.email);

                    // Same message for a wrong email and a wrong password, so nobody
                    // can use login to find out which emails are registered.
                    if (user == null) {
                        ctx.status(401).json(Map.of("error", "wrong email or password"));
                        return;
                    }

                    if (!BCrypt.checkpw(request.password, user.passwordHash)) {
                       ctx.status(401).json(Map.of("error", "wrong email or password"));
                        return;
                    }

                    if (user.status.equals("suspended")) {
                        ctx.status(403).json(Map.of("error", "this account is suspended"));
                        return;
                    }

                    ctx.json(Map.of(
                        "message", "logged in",
                        "userId", user.userId,
                        "fullName", user.fullName,
                        "role", user.role));
                } catch (SQLException e) {
                    ctx.status(500).json(Map.of("error", "something went wrong, try again"));
                }

            });

        });

        // Close the server and the database connections cleanly
        // when the backend is stopped, for example with Ctrl+C.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            database.close();
        }));

        app.start();
        System.out.println("Momentum backend running on http://localhost:" + port);
    }
}
