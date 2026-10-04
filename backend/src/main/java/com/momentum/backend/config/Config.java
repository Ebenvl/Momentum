package com.momentum.backend.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads the backend's settings from config.properties.
 *
 * Settings like the database password are kept in a separate file,
 * not in the code, so they never get committed to GitHub.
 * config.properties sits in the backend folder and is ignored by Git.
 */
public class Config {

    private final Properties properties = new Properties();

    /**
     * Reads config.properties from the folder the backend is run from.
     * Stops with a clear message if the file is missing, so it is obvious
     * what to fix instead of failing later with a confusing error.
     */
    public Config() {
        try (InputStream input = new FileInputStream("config.properties")) {
            properties.load(input);
        } catch (IOException e) {
            throw new IllegalStateException(
                "Could not find config.properties in the backend folder. "
                + "Copy config.example.properties, rename it to config.properties, "
                + "and fill in your database password.", e);
        }
    }

    /** Returns a setting, or stops with a clear message if it is missing. */
    public String get(String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing setting in config.properties: " + key);
        }
        return value.trim();
    }

    /** Returns a setting as a whole number, for example the port. */
    public int getInt(String key) {
        return Integer.parseInt(get(key));
    }
}
