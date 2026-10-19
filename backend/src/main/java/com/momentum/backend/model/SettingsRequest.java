package com.momentum.backend.model;

// Holds the LearnTech settings an admin sends from the settings screen.
// Leave apiKey or apiSecret empty to keep the one already saved.
public class SettingsRequest {

    public String platformUrl;
    public String apiKey;
    public String apiSecret;

}
