package com.momentum.backend.model;

// The LearnTech API settings, as read from the PlatformConfig table.
// The key and secret are still encrypted here.
public class PlatformSettings {

    public String platformUrl;
    public String apiKeyEncrypted;
    public String apiSecretEncrypted;   // can be null, the API only needs the key
    public String lastTestedAt;         // null if never tested

}
