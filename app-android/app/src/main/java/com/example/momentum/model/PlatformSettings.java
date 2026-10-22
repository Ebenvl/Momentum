package com.example.momentum.model;

// The LearnTech connection, as the admin's settings screen shows it.
// The key and secret come back masked, like "****wO5C". The real ones stay on the server.
public class PlatformSettings {
    public String platformUrl;
    public String apiKey;          // masked
    public String apiSecret;       // masked, can be null
    public String lastTestedAt;    // null if never tested
    public boolean savedByAdmin;   // false means it's still using config.properties
}
