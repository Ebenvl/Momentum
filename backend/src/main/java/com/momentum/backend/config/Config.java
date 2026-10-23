package com.momentum.backend.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

// The backend's settings: from config.properties on a laptop (ignored by Git),
// or from the hosting service's secret environment settings, where
// db.password becomes DB_PASSWORD. The file wins if both have it.
public class Config {

    private final Properties properties = new Properties();

    /** Reads config.properties if it's there. Without it, everything comes from the environment. */
    public Config() {
        try (InputStream input = new FileInputStream("config.properties")) {
            properties.load(input);
        } catch (IOException e) {
            System.out.println("No config.properties, using environment settings");
        }
    }

    /** Returns a setting, or stops with a clear message if it is missing. */
    public String get(String key) {
        String value = getOptional(key);
        if (value == null) {
            throw new IllegalStateException("Missing setting: " + key + " (in config.properties, or "
                    + environmentName(key) + " in the environment)");
        }
        return value;
    }

    /** Returns a setting, or null if it isn't set anywhere. For optional settings. */
    public String getOptional(String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            value = System.getenv(environmentName(key));
        }
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** Returns a setting as a whole number, for example the port. */
    public int getInt(String key) {
        return Integer.parseInt(get(key));
    }

    // db.password becomes DB_PASSWORD
    private static String environmentName(String key) {
        return key.toUpperCase().replace('.', '_');
    }
}
