package com.example.momentum.service;

import com.example.momentum.data.Api;
import com.example.momentum.data.ApiCallback;
import com.example.momentum.data.MomentumApi;
import com.example.momentum.model.LeaderboardEntry;

import java.util.List;

// The monthly leaderboard.
public class LeaderboardService {

    private final MomentumApi api = Api.get();

    // This month's rankings, or all time when allTime is true.
    public void getLeaderboard(boolean allTime, ApiCallback<List<LeaderboardEntry>> callback) {
        api.getLeaderboard(allTime, callback);
    }
}
