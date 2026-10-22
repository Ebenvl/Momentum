package com.momentum.backend.leaderboard;

import com.momentum.backend.auth.CurrentUser;
import com.momentum.backend.db.PointsRepository;
import io.javalin.http.Context;

import java.sql.SQLException;
import java.time.YearMonth;
import java.util.Map;

// Handles the monthly leaderboard.
public class LeaderboardController {

    private final PointsRepository pointsRepository;

    public LeaderboardController(PointsRepository pointsRepository) {
        this.pointsRepository = pointsRepository;
    }

    // -----------------------------------------------------
    // GET /leaderboard               this month's points, highest first
    // GET /leaderboard?period=all    points from every month
    // -----------------------------------------------------
    public void getLeaderboard(Context ctx) {

        if (CurrentUser.id(ctx) == null) {
            ctx.status(401).json(Map.of("error", "please log in"));
            return;
        }

        // The current month, like "2026-10", or null for all time.
        String monthKey = "all".equals(ctx.queryParam("period")) ? null : YearMonth.now().toString();

        try {
            ctx.json(pointsRepository.findLeaderboard(monthKey));
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "something went wrong, try again"));
        }
    }
}
