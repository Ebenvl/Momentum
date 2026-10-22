package com.momentum.backend.config;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

// "Today" and "now" in South African time, for streaks, goals and easter eggs.
// A hosted server's clock is often on UTC, two hours behind, which would move
// the end of the day to 02:00. Everything date-based asks here instead.
public class AppClock {

    private static final ZoneId ZONE = ZoneId.of("Africa/Johannesburg");

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    public static ZonedDateTime now() {
        return ZonedDateTime.now(ZONE);
    }
}
