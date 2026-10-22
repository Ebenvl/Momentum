package com.momentum.backend.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

// Stops password guessing: after too many wrong logins in a short time,
// further tries are refused for a while.
//
// Counted per email and per IP address, so it works whether someone tries
// many passwords on one account, or one password on many accounts.
// An IP gets more tries than an email, because many people can share one
// address (everyone on the same Wi-Fi, or behind the hosting service).
// Kept in memory, so a restart of the backend clears it.
public class LoginLimiter {

    public static final int MAX_PER_EMAIL = 5;
    public static final int MAX_PER_IP = 30;
    private static final Duration WINDOW = Duration.ofMinutes(5);

    // For each email or IP, the times of its recent failed logins.
    private final Map<String, Deque<Instant>> failures = new HashMap<>();

    // True if this email or IP has failed max times in the last few minutes.
    public synchronized boolean isBlocked(String key, int max) {
        Deque<Instant> times = recent(key);
        return times != null && times.size() >= max;
    }

    public synchronized void recordFailure(String key) {
        failures.computeIfAbsent(key, k -> new ArrayDeque<>()).addLast(Instant.now());
    }

    // A correct login wipes the slate for that email.
    public synchronized void clear(String key) {
        failures.remove(key);
    }

    // The failures still inside the time window. Older ones are thrown away.
    private Deque<Instant> recent(String key) {
        Deque<Instant> times = failures.get(key);
        if (times == null) {
            return null;
        }
        Instant cutoff = Instant.now().minus(WINDOW);
        while (!times.isEmpty() && times.peekFirst().isBefore(cutoff)) {
            times.removeFirst();
        }
        if (times.isEmpty()) {
            failures.remove(key);
            return null;
        }
        return times;
    }
}
