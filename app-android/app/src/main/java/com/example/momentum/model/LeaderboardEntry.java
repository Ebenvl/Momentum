package com.example.momentum.model;

// One row on the monthly leaderboard.
public class LeaderboardEntry {
    public int rank;
    public String name;
    public int points;

    public LeaderboardEntry(int rank, String name, int points) {
        this.rank = rank;
        this.name = name;
        this.points = points;
    }
}
