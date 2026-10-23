-- Adds two-step login and password reset codes to a database that
-- already exists, without deleting anything. Run this once in MySQL
-- Workbench as root. (02_schema.sql already includes it.)

USE momentum;

ALTER TABLE Users
    ADD COLUMN two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS EmailCodes (
    code_id              INT           NOT NULL AUTO_INCREMENT,
    user_id              INT           NOT NULL,
    purpose              ENUM('login', 'reset', 'enable') NOT NULL,
    code_hash            CHAR(64)      NOT NULL,
    created_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at           DATETIME      NOT NULL,
    attempts             INT           NOT NULL DEFAULT 0,
    used                 BOOLEAN       NOT NULL DEFAULT FALSE,
    PRIMARY KEY (code_id),
    KEY idx_codes_user (user_id, purpose),
    CONSTRAINT fk_codes_user FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;
