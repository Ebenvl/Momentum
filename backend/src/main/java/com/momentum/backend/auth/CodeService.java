package com.momentum.backend.auth;

import com.momentum.backend.db.EmailCodeRepository;
import com.momentum.backend.mail.EmailService;
import com.momentum.backend.model.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.HexFormat;

// Emailed 6-digit codes for login, enable and reset. Each lasts 10 minutes,
// works once, and stops working after 5 wrong tries.
public class CodeService {

    // How sending a code went.
    public enum SendResult { SENT, TOO_SOON, EMAIL_NOT_SET_UP, FAILED }

    private static final int MINUTES = 10;
    private static final int MAX_ATTEMPTS = 5;

    private final EmailCodeRepository emailCodeRepository;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();

    public CodeService(EmailCodeRepository emailCodeRepository, EmailService emailService) {
        this.emailCodeRepository = emailCodeRepository;
        this.emailService = emailService;
    }

    public boolean emailIsSetUp() {
        return emailService.isConfigured();
    }

    // Makes a new code for this user and purpose, saves its hash, and emails it.
    public SendResult sendCode(User user, String purpose) throws SQLException {
        if (!emailService.isConfigured()) {
            return SendResult.EMAIL_NOT_SET_UP;
        }
        if (emailCodeRepository.sentRecently(user.userId, purpose)) {
            return SendResult.TOO_SOON;
        }

        // SecureRandom, so the codes can't be predicted. Always 6 digits, like 004219.
        String code = String.format("%06d", random.nextInt(1_000_000));
        emailCodeRepository.saveCode(user.userId, purpose, hash(code), MINUTES);

        boolean sent = emailService.send(user.email, subjectFor(purpose), bodyFor(purpose, user.fullName, code));
        return sent ? SendResult.SENT : SendResult.FAILED;
    }

    // True if the code is right for this user and purpose. A right code is used up,
    // a wrong one counts as a try. After 5 wrong tries the code stops working.
    public boolean checkCode(User user, String purpose, String code) throws SQLException {
        if (code == null) {
            return false;
        }
        EmailCodeRepository.SavedCode saved = emailCodeRepository.findActiveCode(user.userId, purpose);
        if (saved == null || saved.attempts >= MAX_ATTEMPTS) {
            return false;
        }

        // Compares the hashes in a way that takes the same time either way, so
        // timing can't give away how close a guess was.
        boolean right = MessageDigest.isEqual(saved.codeHash.getBytes(StandardCharsets.UTF_8),
                hash(code.trim()).getBytes(StandardCharsets.UTF_8));
        if (right) {
            emailCodeRepository.markUsed(saved.codeId);
        } else {
            emailCodeRepository.addAttempt(saved.codeId);
        }
        return right;
    }

    // SHA-256 of the code, as 64 hex characters. Only this is stored.
    private String hash(String code) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(code.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 isn't available", e);
        }
    }

    private String subjectFor(String purpose) {
        switch (purpose) {
            case "reset": return "Your LearnTech Momentum password reset code";
            case "enable": return "Confirm two-step login for LearnTech Momentum";
            default: return "Your LearnTech Momentum login code";
        }
    }

    private String bodyFor(String purpose, String name, String code) {
        String why;
        switch (purpose) {
            case "reset": why = "Use this code in the app to choose a new password."; break;
            case "enable": why = "Enter this code in the app to turn on two-step login."; break;
            default: why = "Enter this code in the app to finish logging in."; break;
        }
        return "Hi " + name + ",\n\n"
                + "Your code is: " + code + "\n\n"
                + why + " It works once and runs out in " + MINUTES + " minutes.\n\n"
                + "If you didn't ask for this, you can ignore this email. If it was a login code, "
                + "someone knows your password, so change it in the app's Settings.\n\n"
                + "LearnTech Momentum";
    }
}
