-- ============================================================
-- Sets up the database on a hosted MySQL service (like Aiven).
-- Run this ONCE, connected as the service's admin user (avnadmin on Aiven).
--
-- Like 01_setup.sql, but the app user can connect from anywhere ('%'),
-- because the backend runs on a different server than the database.
-- It can still only read and change data in the momentum database.
--
-- Before running, replace CHANGE_ME with a long random password.
-- Do NOT save this file with the real password in it.
-- ============================================================

CREATE DATABASE IF NOT EXISTS momentum
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'momentum_app'@'%'
    IDENTIFIED BY 'CHANGE_ME';

-- No CREATE, DROP or ALTER: the app can't change the tables themselves.
GRANT SELECT, INSERT, UPDATE, DELETE
    ON momentum.*
    TO 'momentum_app'@'%';

FLUSH PRIVILEGES;
