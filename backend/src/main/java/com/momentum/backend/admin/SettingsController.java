package com.momentum.backend.admin;

import com.momentum.backend.auth.CurrentUser;
import com.momentum.backend.config.Encryption;
import com.momentum.backend.db.ActivityLogRepository;
import com.momentum.backend.db.PlatformConfigRepository;
import com.momentum.backend.learntech.CoursesResponse;
import com.momentum.backend.learntech.LearnTechClient;
import com.momentum.backend.learntech.LearnTechException;
import com.momentum.backend.model.PlatformSettings;
import com.momentum.backend.model.SettingsRequest;
import io.javalin.http.Context;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

// Handles the admin's LearnTech settings screen: the platform address,
// API key and secret, and the "test connection" button.
// The key and secret only ever live on the server. The app sends them here
// once, and only gets back a masked version.
public class SettingsController {

    private final PlatformConfigRepository platformConfigRepository;
    private final ActivityLogRepository activityLogRepository;
    private final LearnTechClient learnTechClient;
    private final Encryption encryption;

    public SettingsController(PlatformConfigRepository platformConfigRepository,
                              ActivityLogRepository activityLogRepository,
                              LearnTechClient learnTechClient, Encryption encryption) {
        this.platformConfigRepository = platformConfigRepository;
        this.activityLogRepository = activityLogRepository;
        this.learnTechClient = learnTechClient;
        this.encryption = encryption;
    }

    // Called once at startup. If the admin has saved settings, use them
    // instead of the ones in config.properties.
    public void loadSavedSettings() {
        try {
            PlatformSettings saved = platformConfigRepository.findSettings();
            if (saved != null && saved.platformUrl != null && saved.apiKeyEncrypted != null) {
                learnTechClient.useSettings(saved.platformUrl, encryption.decrypt(saved.apiKeyEncrypted));
                System.out.println("Using the LearnTech settings saved by the admin");
            }
        } catch (Exception e) {
            System.out.println("Could not load saved LearnTech settings, using config.properties: "
                    + e.getMessage());
        }
    }

    // -----------------------------------------------------
    // GET /admin/settings   the current settings, key and secret masked
    // -----------------------------------------------------
    public void getSettings(Context ctx) {

        if (!requireAdmin(ctx)) {
            return;
        }

        try {
            PlatformSettings saved = platformConfigRepository.findSettings();

            // HashMap instead of Map.of, because some values can be null.
            Map<String, Object> reply = new HashMap<>();
            reply.put("platformUrl", learnTechClient.getBaseUrl());
            reply.put("apiKey", mask(learnTechClient.getApiKey()));
            reply.put("apiSecret", saved != null && saved.apiSecretEncrypted != null
                    ? mask(encryption.decrypt(saved.apiSecretEncrypted))
                    : null);
            reply.put("lastTestedAt", saved != null ? saved.lastTestedAt : null);
            reply.put("savedByAdmin", saved != null);
            ctx.json(reply);
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "something went wrong, try again"));
        }
    }

    // -----------------------------------------------------
    // PUT /admin/settings   save new settings
    // Leave apiKey or apiSecret empty to keep the current one.
    // -----------------------------------------------------
    public void saveSettings(Context ctx) {

        if (!requireAdmin(ctx)) {
            return;
        }

        SettingsRequest request;
        try {
            request = ctx.bodyAsClass(SettingsRequest.class);
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", "invalid request"));
            return;
        }

        if (request.platformUrl == null || request.platformUrl.isBlank()) {
            ctx.status(400).json(Map.of("error", "platform address is required"));
            return;
        }
        String url = request.platformUrl.trim();
        if (!url.startsWith("https://") && !url.equals("sample")) {
            ctx.status(400).json(Map.of("error", "platform address must start with https://"));
            return;
        }

        try {
            PlatformSettings saved = platformConfigRepository.findSettings();

            // A new address needs the key typed in again. Otherwise someone with an
            // admin login could point the backend at their own server and receive the key.
            boolean newKey = request.apiKey != null && !request.apiKey.isBlank();
            if (!url.equals(learnTechClient.getBaseUrl()) && !newKey) {
                ctx.status(400).json(Map.of("error", "enter the API key again when changing the address"));
                return;
            }

            // A new key if one was typed in, otherwise keep the one in use now.
            String apiKey = newKey ? request.apiKey.trim() : learnTechClient.getApiKey();

            // Same for the secret. It's optional, the API only needs the key.
            String secretEncrypted = request.apiSecret != null && !request.apiSecret.isBlank()
                    ? encryption.encrypt(request.apiSecret.trim())
                    : (saved != null ? saved.apiSecretEncrypted : null);

            platformConfigRepository.saveSettings(url, encryption.encrypt(apiKey), secretEncrypted,
                    CurrentUser.id(ctx));
            learnTechClient.useSettings(url, apiKey);

            activityLogRepository.log(CurrentUser.id(ctx), "LearnTech settings changed", "info");
            ctx.json(Map.of("message", "settings saved"));
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "something went wrong, try again"));
        }
    }

    // -----------------------------------------------------
    // POST /admin/settings/test   try the current settings
    // -----------------------------------------------------
    public void testConnection(Context ctx) {

        if (!requireAdmin(ctx)) {
            return;
        }

        try {
            CoursesResponse courses = learnTechClient.getCourses();
            platformConfigRepository.markTested();
            activityLogRepository.log(CurrentUser.id(ctx), "LearnTech connection test passed", "info");
            ctx.json(Map.of("message", "connected", "courses", courses.count));
        } catch (LearnTechException e) {
            try {
                activityLogRepository.log(CurrentUser.id(ctx), "LearnTech connection test failed: "
                        + e.getMessage(), "warning");
            } catch (SQLException logError) {
                // the reply below still tells the admin what went wrong
            }
            ctx.status(503).json(Map.of("error", "could not connect: " + e.getMessage()));
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "something went wrong, try again"));
        }
    }

    // -----------------------------------------------------
    // Helpers
    // -----------------------------------------------------

    // Shows only the last 4 characters, like "****wO5C".
    private String mask(String value) {
        if (value == null || value.length() <= 4) {
            return "****";
        }
        return "****" + value.substring(value.length() - 4);
    }

    // Replies 401 if nobody is logged in, or 403 if they are not an admin.
    // Gives back true only if the request may carry on.
    private boolean requireAdmin(Context ctx) {
        if (CurrentUser.id(ctx) == null) {
            ctx.status(401).json(Map.of("error", "please log in"));
            return false;
        }
        if (!CurrentUser.isAdmin(ctx)) {
            ctx.status(403).json(Map.of("error", "administrators only"));
            return false;
        }
        return true;
    }
}
