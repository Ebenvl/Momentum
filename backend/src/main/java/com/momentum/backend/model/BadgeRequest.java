package com.momentum.backend.model;

// Holds the details an admin sends when creating or editing a badge.
public class BadgeRequest {

    public String name;
    public String description;
    public String imageUrl;
    public String ruleType;
    public Integer ruleValue;
    public Integer pointsAwarded;
    public Boolean isActive;

}
