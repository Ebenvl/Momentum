package com.momentum.backend;

import com.momentum.backend.config.Config;
import com.momentum.backend.db.Database;
import io.javalin.Javalin;

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
