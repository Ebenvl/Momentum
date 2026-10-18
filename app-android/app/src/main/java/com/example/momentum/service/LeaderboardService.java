package com.example.momentum.service;

import com.example.momentum.data.Api;
import com.example.momentum.data.ApiCallback;
import com.example.momentum.data.MomentumApi;
import com.example.momentum.model.LeaderboardEntry;

import java.util.List;

// The monthly leaderboard.
public class LeaderboardService {

    private final MomentumApi api = Api.get();

    public void getLeaderboard(ApiCallback<List<LeaderboardEntry>> callback) {
        api.getLeaderboard(callback);
    }
}
