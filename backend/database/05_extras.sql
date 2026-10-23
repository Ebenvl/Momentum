-- Adds the streak's learning time, the easter eggs and Bartholomew
-- to a database that already exists, without deleting anything.
-- Run this once in MySQL Workbench as root.
-- (A fresh database: run 02_schema.sql and then this file.)

USE momentum;

-- Secret badges (easter eggs) are hidden from learners until found.
-- Skips this if the column is already there.
SET @has_hidden = (SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = 'momentum' AND TABLE_NAME = 'Badges' AND COLUMN_NAME = 'is_hidden');
SET @add_hidden = IF(@has_hidden = 0,
                     'ALTER TABLE Badges ADD COLUMN is_hidden BOOLEAN NOT NULL DEFAULT FALSE',
                     'SELECT 1');
PREPARE statement FROM @add_hidden;
EXECUTE statement;
DEALLOCATE PREPARE statement;

-- ActiveDays: days that counted towards the streak, from a check-in or
-- learning time (35% of a course's expected time that day).
CREATE TABLE IF NOT EXISTS ActiveDays (
    user_id              INT           NOT NULL,
    active_on            DATE          NOT NULL,
    source               ENUM('checkin', 'learning') NOT NULL,
    PRIMARY KEY (user_id, active_on, source),
    CONSTRAINT fk_activedays_user FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- LearningMinutes
-- Minutes spent in each course on each day, added up by the sync
-- from the change in LearnTech's time spent since the last sync.
CREATE TABLE IF NOT EXISTS LearningMinutes (
    user_id              INT           NOT NULL,
    course_id            INT           NOT NULL,
    learned_on           DATE          NOT NULL,
    minutes              INT           NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, course_id, learned_on),
    CONSTRAINT fk_learning_user   FOREIGN KEY (user_id)   REFERENCES Users(user_id)     ON DELETE CASCADE,
    CONSTRAINT fk_learning_course FOREIGN KEY (course_id) REFERENCES Courses(course_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- UserEvents
-- Things a learner did in the app, like opening the timetable,
-- for the easter eggs.
CREATE TABLE IF NOT EXISTS UserEvents (
    event_id             INT           NOT NULL AUTO_INCREMENT,
    user_id              INT           NOT NULL,
    event                VARCHAR(50)   NOT NULL,
    happened_on          DATE          NOT NULL,
    PRIMARY KEY (event_id),
    KEY idx_events_user (user_id, event, happened_on),
    CONSTRAINT fk_events_user FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Easter eggs: secret badges, only shown to a learner once found.
-- The rule types are checked by EasterEggService in the backend.
INSERT INTO Badges (name, description, rule_type, rule_value, points_awarded, is_active, is_hidden)
SELECT * FROM (
    SELECT 'Curious Mind' AS name, 'Tapped the Momentum logo seven times. Curiosity pays off.' AS description,
           'secret_curious' AS rule_type, 0 AS rule_value, 25 AS points_awarded, TRUE AS is_active, TRUE AS is_hidden
    UNION ALL
    SELECT 'Master Planner', 'Planned your week, asked a coach and checked in, all on the same day.',
           'secret_planner', 0, 50, TRUE, TRUE
    UNION ALL
    SELECT 'Night Owl', 'Checked in on a goal between midnight and 4 am. Get some sleep!',
           'secret_night_owl', 0, 25, TRUE, TRUE
    UNION ALL
    SELECT 'Bartholomew''s Apprentice', 'Asked the Study Coach about the legendary Bartholomew Crownelius the Third.',
           'secret_bartholomew', 0, 30, TRUE, TRUE
) AS secrets
WHERE NOT EXISTS (SELECT 1 FROM Badges WHERE Badges.rule_type = secrets.rule_type);

-- Bartholomew Crownelius the Third: a legendary learner who can't log in, with
-- 333 points from October 1066, so he only shows on the all-time leaderboard.
INSERT IGNORE INTO Users (email, password_hash, full_name, role, status)
VALUES ('bartholomew.crownelius.the.third@momentum.app',
        '$2a$12$BXnoKwl9Rc0mEHqFuXpij.9c2zZ5d44qszssJsuiuhVZqYesKwgOO',
        'Bartholomew Crownelius the Third', 'learner', 'active');

INSERT IGNORE INTO PointsTransactions (user_id, points, reason, reference_id, month_key)
SELECT user_id, 333, 'legend', 0, '1066-10' FROM Users
WHERE email = 'bartholomew.crownelius.the.third@momentum.app';
