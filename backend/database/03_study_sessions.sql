-- Adds the StudySessions table to a database that already exists,
-- without deleting anything. Run this once in MySQL Workbench as root.
-- (02_schema.sql already includes it for a fresh database.)

USE momentum;

CREATE TABLE IF NOT EXISTS StudySessions (
    session_id           INT           NOT NULL AUTO_INCREMENT,
    user_id              INT           NOT NULL,
    day                  VARCHAR(10)   NOT NULL,   -- Monday to Sunday
    time                 VARCHAR(30)   NOT NULL,   -- as typed, e.g. 18:00 - 19:00
    course               VARCHAR(255)  NOT NULL,
    task                 VARCHAR(255)  NOT NULL,
    created_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (session_id),
    KEY idx_sessions_user (user_id),
    CONSTRAINT fk_sessions_user FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;
