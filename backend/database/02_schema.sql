-- ============================================================
-- LearnTech Momentum: database schema
-- Matches D3 Section 7 (Data Design), table for table.
-- Run this logged in as root in MySQL Workbench, after 01_setup.sql.
--
-- Safe to re-run: it drops the tables first, in the right order,
-- so it always gives you a clean, empty database.
-- WARNING: re-running deletes all data in these tables.
-- ============================================================

USE momentum;

-- Drop in reverse order of dependency, children before parents,
-- otherwise the foreign keys block the drop.
DROP TABLE IF EXISTS PlatformConfig;
DROP TABLE IF EXISTS ActivityLogs;
DROP TABLE IF EXISTS ProgressSnapshots;
DROP TABLE IF EXISTS Streaks;
DROP TABLE IF EXISTS UserBadges;
DROP TABLE IF EXISTS Badges;
DROP TABLE IF EXISTS PointsTransactions;
DROP TABLE IF EXISTS CheckIns;
DROP TABLE IF EXISTS Goals;
DROP TABLE IF EXISTS Enrolments;
DROP TABLE IF EXISTS Courses;
DROP TABLE IF EXISTS Users;


-- ------------------------------------------------------------
-- Users
-- Everyone who registers in the app, learners and administrators.
-- idst links the account to a learner on the LearnTech platform.
-- NULL idst means registered but not yet matched by email, which is
-- a normal state, not an error (API v1.0, point 8).
-- ------------------------------------------------------------
CREATE TABLE Users (
    user_id              INT           NOT NULL AUTO_INCREMENT,
    idst                 INT           NULL,
    email                VARCHAR(255)  NOT NULL,
    password_hash        VARCHAR(255)  NOT NULL,
    full_name            VARCHAR(150)  NOT NULL,
    role                 ENUM('learner', 'administrator') NOT NULL DEFAULT 'learner',
    status               ENUM('active', 'suspended')      NOT NULL DEFAULT 'active',
    reminder_enabled     BOOLEAN       NOT NULL DEFAULT TRUE,
    reminder_time        TIME          NULL,
    reminder_days_before INT           NOT NULL DEFAULT 1,
    created_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login_at        DATETIME      NULL,
    PRIMARY KEY (user_id),
    UNIQUE KEY uq_users_email (email),   -- also serves as the login lookup index
    UNIQUE KEY uq_users_idst  (idst)     -- one app account per platform learner
) ENGINE=InnoDB;


-- ------------------------------------------------------------
-- Courses
-- The course catalogue, synced from the LearnTech /courses endpoint.
-- Always key on platform_course_id, never on the course code,
-- because codes are not unique on the platform (API v1.0, point 3).
-- expected_minutes can be NULL when no duration is set on the
-- platform. It must never be treated as zero (API v1.0, point 4).
-- ------------------------------------------------------------
CREATE TABLE Courses (
    course_id            INT           NOT NULL AUTO_INCREMENT,
    platform_course_id   INT           NOT NULL,
    course_name          VARCHAR(255)  NOT NULL,
    expected_minutes     INT           NULL,
    points_value         INT           NULL,     -- admin override, NULL means use the calculated value
    created_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (course_id),
    UNIQUE KEY uq_courses_platform_id (platform_course_id)
) ENGINE=InnoDB;


-- ------------------------------------------------------------
-- Enrolments
-- Which learner is on which course, and the state of it,
-- as of the last sync. state has exactly three values, there is
-- no per-course percentage (Client brief section 5, API v1.0 point 1).
-- ------------------------------------------------------------
CREATE TABLE Enrolments (
    enrolment_id         INT           NOT NULL AUTO_INCREMENT,
    user_id              INT           NOT NULL,
    course_id            INT           NOT NULL,
    state                ENUM('not_started', 'in_progress', 'completed') NOT NULL,
    time_spent_minutes   INT           NOT NULL DEFAULT 0,   -- display only, never used for scoring
    assessment_status    ENUM('passed', 'failed', 'attempted') NULL,
    score                DECIMAL(5,2)  NULL,
    score_max            DECIMAL(5,2)  NULL,
    first_access         DATE          NULL,
    completed_on         DATE          NULL,
    last_synced_at       DATETIME      NOT NULL,
    PRIMARY KEY (enrolment_id),
    UNIQUE KEY uq_enrolments_user_course (user_id, course_id),  -- also the per-user lookup index
    CONSTRAINT fk_enrolments_user   FOREIGN KEY (user_id)   REFERENCES Users(user_id)     ON DELETE CASCADE,
    CONSTRAINT fk_enrolments_course FOREIGN KEY (course_id) REFERENCES Courses(course_id) ON DELETE CASCADE
) ENGINE=InnoDB;


-- ------------------------------------------------------------
-- Goals
-- Personal goals a learner sets. Free text goals carry no points,
-- because the app cannot check them on its own (Client brief section 6).
-- ------------------------------------------------------------
CREATE TABLE Goals (
    goal_id              INT           NOT NULL AUTO_INCREMENT,
    user_id              INT           NOT NULL,
    title                VARCHAR(255)  NOT NULL,
    goal_type            ENUM('complete_n_courses', 'complete_specific_course',
                              'spend_minutes', 'active_days', 'free_text') NOT NULL,
    target_value         INT           NULL,
    target_course_id     INT           NULL,
    target_date          DATE          NULL,
    status               ENUM('active', 'completed', 'failed', 'cancelled') NOT NULL DEFAULT 'active',
    points_eligible      BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    completed_at         DATETIME      NULL,
    PRIMARY KEY (goal_id),
    KEY idx_goals_user (user_id),
    CONSTRAINT fk_goals_user   FOREIGN KEY (user_id)          REFERENCES Users(user_id)     ON DELETE CASCADE,
    CONSTRAINT fk_goals_course FOREIGN KEY (target_course_id) REFERENCES Courses(course_id) ON DELETE SET NULL
) ENGINE=InnoDB;


-- ------------------------------------------------------------
-- CheckIns
-- Progress a learner logs against one of their goals.
-- ------------------------------------------------------------
CREATE TABLE CheckIns (
    checkin_id           INT           NOT NULL AUTO_INCREMENT,
    goal_id              INT           NOT NULL,
    user_id              INT           NOT NULL,
    logged_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    note                 TEXT          NULL,
    value_logged         INT           NULL,
    PRIMARY KEY (checkin_id),
    CONSTRAINT fk_checkins_goal FOREIGN KEY (goal_id) REFERENCES Goals(goal_id) ON DELETE CASCADE,
    CONSTRAINT fk_checkins_user FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;


-- ------------------------------------------------------------
-- PointsTransactions
-- Every points award is one row, written during sync, not on display.
-- month_key drives the monthly leaderboard (Client brief section 7).
-- The unique key stops a repeated sync from awarding the same
-- completion twice (API v1.0, "Award points when you sync").
-- ------------------------------------------------------------
CREATE TABLE PointsTransactions (
    transaction_id       INT           NOT NULL AUTO_INCREMENT,
    user_id              INT           NOT NULL,
    points               INT           NOT NULL,
    reason               VARCHAR(100)  NOT NULL,   -- e.g. course_completed, badge_awarded
    reference_id         INT           NULL,       -- course_id or badge_id, depending on reason
    earned_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    month_key            CHAR(7)       NOT NULL,   -- e.g. '2026-10'
    PRIMARY KEY (transaction_id),
    UNIQUE KEY uq_points_once (user_id, reference_id, reason),
    KEY idx_points_month (month_key),
    CONSTRAINT fk_points_user FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;


-- ------------------------------------------------------------
-- Badges
-- Badge definitions, set up by the administrator.
-- ------------------------------------------------------------
CREATE TABLE Badges (
    badge_id             INT           NOT NULL AUTO_INCREMENT,
    name                 VARCHAR(100)  NOT NULL,
    description          TEXT          NULL,
    image_url            VARCHAR(500)  NULL,
    rule_type            VARCHAR(50)   NULL,
    rule_value           INT           NOT NULL DEFAULT 0,
    points_awarded       INT           NOT NULL DEFAULT 0,
    is_active            BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (badge_id)
) ENGINE=InnoDB;


-- ------------------------------------------------------------
-- UserBadges
-- Which learner has earned which badge. Each badge only once per learner.
-- ------------------------------------------------------------
CREATE TABLE UserBadges (
    user_badge_id        INT           NOT NULL AUTO_INCREMENT,
    user_id              INT           NOT NULL,
    badge_id             INT           NOT NULL,
    awarded_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_badge_id),
    UNIQUE KEY uq_userbadges_once (user_id, badge_id),
    CONSTRAINT fk_userbadges_user  FOREIGN KEY (user_id)  REFERENCES Users(user_id)   ON DELETE CASCADE,
    CONSTRAINT fk_userbadges_badge FOREIGN KEY (badge_id) REFERENCES Badges(badge_id) ON DELETE CASCADE
) ENGINE=InnoDB;


-- ------------------------------------------------------------
-- Streaks
-- One row per learner, their current and longest streak.
-- ------------------------------------------------------------
CREATE TABLE Streaks (
    user_id              INT           NOT NULL,
    current_streak       INT           NOT NULL DEFAULT 0,
    longest_streak       INT           NOT NULL DEFAULT 0,
    last_activity_date   DATE          NULL,
    updated_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_streaks_user FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;


-- ------------------------------------------------------------
-- ProgressSnapshots
-- Written on every scheduled sync. The platform only holds the
-- current position with no history, so this table is the only place
-- that history will ever exist (API v1.0, point 7).
-- ------------------------------------------------------------
CREATE TABLE ProgressSnapshots (
    snapshot_id          INT           NOT NULL AUTO_INCREMENT,
    user_id              INT           NOT NULL,
    snapshot_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    courses_enrolled     INT           NOT NULL DEFAULT 0,
    courses_completed    INT           NOT NULL DEFAULT 0,
    total_minutes        INT           NOT NULL DEFAULT 0,
    total_points         INT           NOT NULL DEFAULT 0,
    current_streak       INT           NOT NULL DEFAULT 0,
    PRIMARY KEY (snapshot_id),
    KEY idx_snapshots_user_time (user_id, snapshot_at),
    CONSTRAINT fk_snapshots_user FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;


-- ------------------------------------------------------------
-- ActivityLogs
-- System activity and errors, viewable by the administrator.
-- user_id is nullable because some events are not tied to a user,
-- for example the scheduled sync job.
-- ------------------------------------------------------------
CREATE TABLE ActivityLogs (
    log_id               INT           NOT NULL AUTO_INCREMENT,
    user_id              INT           NULL,
    action               VARCHAR(100)  NOT NULL,
    details              JSON          NULL,
    level                ENUM('info', 'warning', 'error') NOT NULL DEFAULT 'info',
    created_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (log_id),
    KEY idx_logs_created (created_at),
    CONSTRAINT fk_logs_user FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE SET NULL
) ENGINE=InnoDB;


-- ------------------------------------------------------------
-- PlatformConfig
-- The LearnTech API details, entered by the administrator in the app.
-- Only ever one row, config_id is always 1.
-- The key and secret are stored encrypted, and only on the server,
-- never on the phone (Client brief section 9).
-- ------------------------------------------------------------
CREATE TABLE PlatformConfig (
    config_id            INT           NOT NULL DEFAULT 1,
    platform_url         VARCHAR(500)  NULL,
    api_key_encrypted    TEXT          NULL,
    api_secret_encrypted TEXT          NULL,
    last_tested_at       DATETIME      NULL,
    updated_by           INT           NULL,
    updated_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (config_id),
    CONSTRAINT chk_config_single_row CHECK (config_id = 1),
    CONSTRAINT fk_config_user FOREIGN KEY (updated_by) REFERENCES Users(user_id) ON DELETE SET NULL
) ENGINE=InnoDB;
