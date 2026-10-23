-- LearnTech Momentum: the database and the app's user. Run once as root.
-- Replace CHANGE_ME with a password first, and don't save it in this file.

-- The database that holds everything for the app.
-- utf8mb4 so names and text with any characters store correctly.
CREATE DATABASE IF NOT EXISTS momentum
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- A separate user for the backend, so the backend never logs in as root.
-- It can only connect from this computer (localhost).
CREATE USER IF NOT EXISTS 'momentum_app'@'localhost'
    IDENTIFIED BY 'CHANGE_ME';

-- The app user can read and change data in the momentum database only.
-- It cannot create or drop tables, and it cannot see any other database.
-- This limits the damage if the backend is ever compromised.
GRANT SELECT, INSERT, UPDATE, DELETE
    ON momentum.*
    TO 'momentum_app'@'localhost';

FLUSH PRIVILEGES;
