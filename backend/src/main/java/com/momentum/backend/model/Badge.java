package com.momentum.backend.model;

// One badge definition, as read from the Badges table.
public class Badge {

    public int badgeId;
    public String name;
    public String description;
    public String imageUrl;
    public String ruleType;
    public int ruleValue;
    public int pointsAwarded;
    public boolean isActive;
    public String awardedAt;    // only filled in for a learner's own badges

}
