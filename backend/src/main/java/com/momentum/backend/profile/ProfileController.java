package com.momentum.backend.profile;

import com.momentum.backend.auth.CurrentUser;
import com.momentum.backend.db.BadgeRepository;
import com.momentum.backend.db.PointsRepository;
import com.momentum.backend.db.SnapshotRepository;
import com.momentum.backend.db.StreakRepository;
import com.momentum.backend.db.UserRepository;
import com.momentum.backend.model.PasswordRequest;
import com.momentum.backend.model.ProfileRequest;
import com.momentum.backend.model.Streak;
import com.momentum.backend.model.User;
import io.javalin.http.Context;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.util.Map;

// Handles the learner's own profile.
public class ProfileController {

    private final UserRepository userRepository;
    private final SnapshotRepository snapshotRepository;
    private final PointsRepository pointsRepository;
    private final StreakRepository streakRepository;
    private final BadgeRepository badgeRepository;

    public ProfileController(UserRepository userRepository, SnapshotRepository snapshotRepository,
                             PointsRepository pointsRepository, StreakRepository streakRepository,
                             BadgeRepository badgeRepository) {
        this.userRepository = userRepository;
        this.snapshotRepository = snapshotRepository;
        this.pointsRepository = pointsRepository;
        this.streakRepository = streakRepository;
        this.badgeRepository = badgeRepository;
    }

    // -----------------------------------------------------
    // GET /me/badges   the badges the learner has earned, newest first
    // -----------------------------------------------------
    public void getBadges(Context ctx) {

        Integer userId = CurrentUser.id(ctx);
        if (userId == null) {
            ctx.status(401).json(Map.of("error", "please log in"));
            return;
        }

        try {
            ctx.json(badgeRepository.findBadgesForUser(userId));
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "something went wrong, try again"));
        }
    }

    // -----------------------------------------------------
    // GET /me
    // -----------------------------------------------------
    public void getMe(Context ctx) {

        Integer userId = CurrentUser.id(ctx);
        if (userId == null) {
            ctx.status(401).json(Map.of("error", "please log in"));
            return;
        }

        try {
            User user = userRepository.findUserById(userId);
            if (user == null) {
                ctx.status(404).json(Map.of("error", "account not found"));
                return;
            }

            // Never send the password hash back to the app.
            user.passwordHash = null;
            user.totalPoints = pointsRepository.totalPoints(userId);
            user.currentStreak = streakRepository.activeStreak(userId);

            Streak streak = streakRepository.findStreak(userId);
            user.longestStreak = streak != null ? streak.longestStreak : 0;
            ctx.json(user);
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "something went wrong, try again"));
        }
    }

    // -----------------------------------------------------
    // PUT /me
    // -----------------------------------------------------
    public void updateMe(Context ctx) {

        Integer userId = CurrentUser.id(ctx);
        if (userId == null) {
            ctx.status(401).json(Map.of("error", "please log in"));
            return;
        }

        ProfileRequest request;
        try {
            request = ctx.bodyAsClass(ProfileRequest.class);
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", "invalid request"));
            return;
        }

        if (request.fullName == null || request.fullName.isBlank()) {
            ctx.status(400).json(Map.of("error", "full name is required"));
            return;
        }
        if (request.fullName.length() > 150) {
            ctx.status(400).json(Map.of("error", "full name can be at most 150 characters"));
            return;
        }

        // Only a name was sent, from the settings screen: change just the name.
        if (request.reminderEnabled == null && request.reminderDaysBefore == null) {
            try {
                userRepository.updateName(userId, request.fullName.trim());
                ctx.json(Map.of("message", "profile updated"));
            } catch (SQLException e) {
                ctx.status(500).json(Map.of("error", "something went wrong, try again"));
            }
            return;
        }

        if (request.reminderEnabled == null) {
            ctx.status(400).json(Map.of("error", "reminder on or off is required"));
            return;
        }
        if (request.reminderDaysBefore == null || request.reminderDaysBefore < 0) {
            ctx.status(400).json(Map.of("error", "reminder days must be 0 or more"));
            return;
        }

        try {
            boolean saved = userRepository.updateProfile(userId, request.fullName, request.reminderEnabled,
                    request.reminderTime, request.reminderDaysBefore);
            if (saved) {
                ctx.json(Map.of("message", "profile updated"));
            } else {
                ctx.status(404).json(Map.of("error", "account not found"));
            }
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "something went wrong, try again"));
        }
    }

    // -----------------------------------------------------
    // PUT /me/password   change your own password
    // The current password has to be right, so a phone left logged in
    // can't be used to take over the account.
    // -----------------------------------------------------
    public void changePassword(Context ctx) {

        Integer userId = CurrentUser.id(ctx);
        if (userId == null) {
            ctx.status(401).json(Map.of("error", "please log in"));
            return;
        }

        PasswordRequest request;
        try {
            request = ctx.bodyAsClass(PasswordRequest.class);
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", "invalid request"));
            return;
        }

        if (request.currentPassword == null || request.newPassword == null) {
            ctx.status(400).json(Map.of("error", "enter your current and new password"));
            return;
        }
        if (request.newPassword.length() < 8) {
            ctx.status(400).json(Map.of("error", "password must be at least 8 characters"));
            return;
        }
        if (request.newPassword.length() > 72) {
            ctx.status(400).json(Map.of("error", "password can be at most 72 characters"));
            return;
        }

        try {
            User user = userRepository.findUserById(userId);
            if (user == null || !BCrypt.checkpw(request.currentPassword, user.passwordHash)) {
                ctx.status(400).json(Map.of("error", "your current password is wrong"));
                return;
            }

            userRepository.updatePassword(userId, BCrypt.hashpw(request.newPassword, BCrypt.gensalt()));
            ctx.json(Map.of("message", "password changed"));
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "something went wrong, try again"));
        }
    }

    // -----------------------------------------------------
    // GET /me/progress   the learner's progress over time, for the charts
    // -----------------------------------------------------
    public void getProgress(Context ctx) {

        Integer userId = CurrentUser.id(ctx);
        if (userId == null) {
            ctx.status(401).json(Map.of("error", "please log in"));
            return;
        }

        try {
            ctx.json(snapshotRepository.findSnapshotsForUser(userId));
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "something went wrong, try again"));
        }
    }
}
