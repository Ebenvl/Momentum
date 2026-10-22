package com.example.momentum.model;

// A badge. Matches the Badges table, plus UserBadges for a learner's own badges.
public class Badge {
    public int id;
    public String name;
    public String description;   // can be null
    public String imageUrl;      // the badge's picture, one of BadgeManagement's PICTURES, can be null
    public int pointsAwarded;
    public String awardedAt;     // when the learner earned it, only for their own badges
    public String ruleType;      // courses_completed, streak_days or total_points
    public int ruleValue;        // the number to reach
    public boolean isActive;     // only active badges can be earned
    public boolean isHidden;     // a secret badge (easter egg)
}
