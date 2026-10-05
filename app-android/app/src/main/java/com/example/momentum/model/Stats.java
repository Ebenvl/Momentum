package com.example.momentum.model;

// Summary numbers for the learner's home screen.
public class Stats {
    public int pointsThisMonth;
    public int totalPoints;
    public int currentStreak;
    public int badges;

    public Stats(int pointsThisMonth, int totalPoints, int currentStreak, int badges) {
        this.pointsThisMonth = pointsThisMonth;
        this.totalPoints = totalPoints;
        this.currentStreak = currentStreak;
        this.badges = badges;
    }
}
