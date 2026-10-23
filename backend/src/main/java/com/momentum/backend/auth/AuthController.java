package com.momentum.backend.auth;

import com.momentum.backend.db.ActivityLogRepository;
import com.momentum.backend.db.UserRepository;
import com.momentum.backend.model.CodeRequest;
import com.momentum.backend.model.LoginRequest;
import com.momentum.backend.model.RegisterRequest;
import com.momentum.backend.model.User;
import com.momentum.backend.sync.SyncService;
import io.javalin.http.Context;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.util.Map;

// Handles register and login.
// The routes in App.java point here.
public class AuthController {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final ActivityLogRepository activityLogRepository;
    private final SyncService syncService;
    private final LoginLimiter loginLimiter = new LoginLimiter();

    // A real BCrypt hash of a random password nobody knows, only used to make
    // logins with an unknown email take the same time as a wrong password.
    private static final String TIMING_HASH = BCrypt.hashpw("not-a-real-password", BCrypt.gensalt());
    private final CodeService codeService;

    public AuthController(UserRepository userRepository, TokenService tokenService,
                          ActivityLogRepository activityLogRepository, SyncService syncService,
                          CodeService codeService) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.activityLogRepository = activityLogRepository;
        this.syncService = syncService;
        this.codeService = codeService;
    }

    // POST /register
    public void register(Context ctx) {

        // Read the details the app sent. If it isn't proper JSON, stop here.
        RegisterRequest request;
        try {
            request = ctx.bodyAsClass(RegisterRequest.class);
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", "invalid request"));
            return;
        }

        // Check nothing was left empty.
        if (request.email == null || request.email.isBlank()) {
            ctx.status(400).json(Map.of("error", "email is required"));
            return;
        }
        if (request.password == null || request.password.isBlank()) {
            ctx.status(400).json(Map.of("error", "password is required"));
            return;
        }
        if (request.fullName == null || request.fullName.isBlank()) {
            ctx.status(400).json(Map.of("error", "full name is required"));
            return;
        }

        // Basic format checks. The lengths match the Users table.
        if (request.email.length() > 255 || request.fullName.length() > 150) {
            ctx.status(400).json(Map.of("error", "email or name is too long"));
            return;
        }
        if (!request.email.contains("@")) {
            ctx.status(400).json(Map.of("error", "email is not valid"));
            return;
        }
        if (request.password.length() < 8) {
            ctx.status(400).json(Map.of("error", "password must be at least 8 characters"));
            return;
        }
        // BCrypt only reads the first 72 characters, so anything
        // longer would quietly be cut off. Better to say so.
        if (request.password.length() > 72) {
            ctx.status(400).json(Map.of("error", "password can be at most 72 characters"));
            return;
        }

        try {
            // Stop here if someone already registered with this email.
            if (userRepository.emailExists(request.email)) {
                ctx.status(409).json(Map.of("error", "email already registered"));
                return;
            }

            String passwordHash = BCrypt.hashpw(request.password, BCrypt.gensalt());
            boolean saved = userRepository.insertUser(request.email, passwordHash, request.fullName);
            if (saved) {
                User newUser = userRepository.findUserByEmail(request.email);
                activityLogRepository.log(newUser.userId, "registered", "info");
                ctx.json(Map.of("message", "registered"));
            } else {
                ctx.status(500).json(Map.of("error", "could not register"));
            }
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "something went wrong, try again"));
        }
    }

    // POST /login
    public void login(Context ctx) {

        // Read the details the app sent. If it isn't proper JSON, stop here.
        LoginRequest request;
        try {
            request = ctx.bodyAsClass(LoginRequest.class);
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", "invalid request"));
            return;
        }

        // Check nothing was left empty.
        if (request.email == null || request.email.isBlank()) {
            ctx.status(400).json(Map.of("error", "email is required"));
            return;
        }
        if (request.password == null || request.password.isBlank()) {
            ctx.status(400).json(Map.of("error", "password is required"));
            return;
        }

        // Too many wrong tries for this email or from this IP: refuse for a while.
        String emailKey = "email:" + request.email.trim().toLowerCase();
        String ipKey = "ip:" + ctx.ip();
        if (loginLimiter.isBlocked(emailKey, LoginLimiter.MAX_PER_EMAIL)
                || loginLimiter.isBlocked(ipKey, LoginLimiter.MAX_PER_IP)) {
            ctx.status(429).json(Map.of("error", "too many wrong tries, wait 5 minutes and try again"));
            return;
        }

        try {
            User user = userRepository.findUserByEmail(request.email);

            // Same message for a wrong email and a wrong password, so nobody
            // can use login to find out which emails are registered.
            if (user == null) {
                // Checks the password against a made-up hash anyway, so an unknown email
                // takes as long as a wrong password. Otherwise the time taken would show
                // which emails have accounts.
                BCrypt.checkpw(request.password, TIMING_HASH);
                loginLimiter.recordFailure(emailKey);
                loginLimiter.recordFailure(ipKey);
                activityLogRepository.log(null, "failed login: unknown email", "warning");
                ctx.status(401).json(Map.of("error", "wrong email or password"));
                return;
            }

            if (!BCrypt.checkpw(request.password, user.passwordHash)) {
                loginLimiter.recordFailure(emailKey);
                loginLimiter.recordFailure(ipKey);
                activityLogRepository.log(user.userId, "failed login: wrong password", "warning");
                ctx.status(401).json(Map.of("error", "wrong email or password"));
                return;
            }
            loginLimiter.clear(emailKey);

            if (user.status.equals("suspended")) {
                activityLogRepository.log(user.userId, "failed login: account suspended", "warning");
                ctx.status(403).json(Map.of("error", "this account is suspended"));
                return;
            }

            // Two-step login is on: the password was right, but they only get in
            // with the code emailed to them (see verifyLogin).
            if (user.twoFactorEnabled) {
                CodeService.SendResult sent = codeService.sendCode(user, "login");
                if (sent == CodeService.SendResult.EMAIL_NOT_SET_UP || sent == CodeService.SendResult.FAILED) {
                    ctx.status(503).json(Map.of("error", "couldn't send your login code, try again later"));
                    return;
                }
                ctx.json(Map.of("message", "code sent", "codeRequired", true));
                return;
            }

            finishLogin(ctx, user);
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "something went wrong, try again"));
        }
    }

    // POST /login/verify   the second step: the emailed code
    public void verifyLogin(Context ctx) {

        CodeRequest request;
        try {
            request = ctx.bodyAsClass(CodeRequest.class);
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", "invalid request"));
            return;
        }
        if (request.email == null || request.code == null) {
            ctx.status(400).json(Map.of("error", "enter the code from the email"));
            return;
        }

        String emailKey = "email:" + request.email.trim().toLowerCase();
        if (loginLimiter.isBlocked(emailKey, LoginLimiter.MAX_PER_EMAIL)) {
            ctx.status(429).json(Map.of("error", "too many wrong tries, wait 5 minutes and try again"));
            return;
        }

        try {
            User user = userRepository.findUserByEmail(request.email);
            if (user == null || !"active".equals(user.status) || !codeService.checkCode(user, "login", request.code)) {
                loginLimiter.recordFailure(emailKey);
                if (user != null) {
                    activityLogRepository.log(user.userId, "failed login: wrong code", "warning");
                }
                ctx.status(401).json(Map.of("error", "that code is wrong or has run out"));
                return;
            }
            loginLimiter.clear(emailKey);
            finishLogin(ctx, user);
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "something went wrong, try again"));
        }
    }

    // POST /password/forgot   emails a reset code
    // Always gives the same answer, whether the email has an account or not,
    // so it can't be used to find out who is registered.
    public void forgotPassword(Context ctx) {

        CodeRequest request;
        try {
            request = ctx.bodyAsClass(CodeRequest.class);
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", "invalid request"));
            return;
        }
        if (request.email == null || request.email.isBlank()) {
            ctx.status(400).json(Map.of("error", "email is required"));
            return;
        }
        if (!codeService.emailIsSetUp()) {
            ctx.status(503).json(Map.of("error", "password reset by email isn't set up, ask an administrator"));
            return;
        }

        try {
            User user = userRepository.findUserByEmail(request.email);
            if (user != null && "active".equals(user.status)) {
                codeService.sendCode(user, "reset");
                activityLogRepository.log(user.userId, "password reset code sent", "info");
            }
            ctx.json(Map.of("message", "if that email has an account, a code is on its way"));
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "something went wrong, try again"));
        }
    }

    // POST /password/reset   the code from the email, and the new password
    public void resetPassword(Context ctx) {

        CodeRequest request;
        try {
            request = ctx.bodyAsClass(CodeRequest.class);
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", "invalid request"));
            return;
        }
        if (request.email == null || request.code == null || request.newPassword == null) {
            ctx.status(400).json(Map.of("error", "enter the code and a new password"));
            return;
        }
        if (request.newPassword.length() < 8 || request.newPassword.length() > 72) {
            ctx.status(400).json(Map.of("error", "password must be 8 to 72 characters"));
            return;
        }

        String emailKey = "email:" + request.email.trim().toLowerCase();
        if (loginLimiter.isBlocked(emailKey, LoginLimiter.MAX_PER_EMAIL)) {
            ctx.status(429).json(Map.of("error", "too many wrong tries, wait 5 minutes and try again"));
            return;
        }

        try {
            User user = userRepository.findUserByEmail(request.email);
            if (user == null || !codeService.checkCode(user, "reset", request.code)) {
                loginLimiter.recordFailure(emailKey);
                ctx.status(400).json(Map.of("error", "that code is wrong or has run out"));
                return;
            }
            userRepository.updatePassword(user.userId, BCrypt.hashpw(request.newPassword, BCrypt.gensalt()));
            loginLimiter.clear(emailKey);
            activityLogRepository.log(user.userId, "password reset", "info");
            ctx.json(Map.of("message", "password changed, you can log in now"));
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "something went wrong, try again"));
        }
    }

    // Everything after a successful login: records it, starts the background
    // sync, and gives the app its login token.
    private void finishLogin(Context ctx, User user) throws SQLException {

        // Records the login time.
        userRepository.updateLastLogin(user.userId);
        activityLogRepository.log(user.userId, "logged in", "info");

        // Brings their courses and points up to date, without making login wait.
        syncService.syncUserInBackground(user);

        String token = tokenService.createToken(user);

        ctx.json(Map.of(
            "message", "logged in",
            "token", token,
            "userId", user.userId,
            "fullName", user.fullName,
            "role", user.role));
    }
}
