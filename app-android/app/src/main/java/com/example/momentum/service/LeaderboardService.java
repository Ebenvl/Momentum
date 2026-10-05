package com.example.momentum.service;

import com.example.momentum.data.Api;
import com.example.momentum.data.ApiCallback;
import com.example.momentum.data.MomentumApi;
import com.example.momentum.model.LeaderboardEntry;
import com.example.momentum.model.Stats;

import java.util.List;

// Points, badges, streaks and the monthly leaderboard.
public class LeaderboardService {

    private final MomentumApi api = Api.get();

    public void getLeaderboard(ApiCallback<List<LeaderboardEntry>> callback) {
        api.getLeaderboard(callback);
    }

    public void getStats(int userId, ApiCallback<Stats> callback) {
        api.getStats(userId, callback);
    }
}
