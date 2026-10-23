package com.momentum.backend.db;

import com.momentum.backend.config.Config;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

// The connection to MySQL: keeps a small pool of open connections and hands
// them out, instead of opening a new one for every request.
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

        // How long to wait for MySQL, in milliseconds. 30 seconds, because the
        // first secure connection on the free hosting can take a while.
        hikari.setConnectionTimeout(30000);

        this.dataSource = new HikariDataSource(hikari);
    }

    // Borrows a connection. Use it in try-with-resources, so it goes back to the pool.
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
