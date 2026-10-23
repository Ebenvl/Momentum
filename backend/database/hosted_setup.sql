-- Like 01_setup.sql, for a hosted MySQL (Aiven). Run once as the service's admin.
-- The app user can connect from anywhere, but only use the momentum data.
-- Replace CHANGE_ME with a long random password, and don't save it in this file.

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
