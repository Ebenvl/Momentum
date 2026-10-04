package com.momentum.backend.db;

import com.momentum.backend.config.Config;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Manages the connection to the MySQL database.
 *
 * Instead of opening a new connection for every request, which is slow,
 * this keeps a small pool of connections open and hands them out as needed.
 * Every other part of the backend gets its database connection from here.
 */
public class Database {

    private final HikariDataSource dataSource;

    /** Sets up the connection pool using the settings in config.properties. */
    public Database(Config config) {
        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl(config.get("db.url"));
        hikari.setUsername(config.get("db.user"));
        hikari.setPassword(config.get("db.password"));

        // A small pool is plenty for this project.
        hikari.setMaximumPoolSize(5);

        // How long to wait for MySQL before giving up, in milliseconds.
        // If MySQL is not running, this is how long until the error appears.
        hikari.setConnectionTimeout(5000);

        this.dataSource = new HikariDataSource(hikari);
    }

    /**
     * Borrows a connection from the pool.
     * Always use it inside try-with-resources, so it goes back to the pool:
     *
     *   try (Connection conn = database.getConnection()) { ... }
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Checks the database is reachable. Used by the /health/db endpoint.
     * Returns true if MySQL answers, false if it does not.
     */
    public boolean isReachable() {
        try (Connection conn = getConnection()) {
            return conn.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    /** Closes all connections. Called when the backend shuts down. */
    public void close() {
        dataSource.close();
    }
}
