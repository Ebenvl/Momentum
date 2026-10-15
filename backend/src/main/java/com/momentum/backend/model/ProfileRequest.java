package com.momentum.backend.model;

// Holds the details the app sends when a learner edits their profile.
public class ProfileRequest {

    public String fullName;
    public Boolean reminderEnabled;
    public String reminderTime;        // HH:MM, or null for no set time
    public Integer reminderDaysBefore;

}
