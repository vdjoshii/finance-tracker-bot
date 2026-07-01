-- Run this once before starting the application, or let Spring Boot's
-- createDatabaseIfNotExist=true handle it automatically (already configured
-- in application.yml). This file is provided for clarity / manual setup.

CREATE DATABASE IF NOT EXISTS finance_tracker
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Spring Data JPA (ddl-auto: update) will create all tables automatically
-- on first application startup based on the @Entity classes. No manual
-- CREATE TABLE statements are required.

-- Optional: create a dedicated, less-privileged MySQL user instead of using root.
-- Uncomment and edit if you'd rather not run the app as root:
--
-- CREATE USER IF NOT EXISTS 'finance_bot'@'localhost' IDENTIFIED BY '[YOUR_MYSQL_PASSWORD]';
-- GRANT ALL PRIVILEGES ON finance_tracker.* TO 'finance_bot'@'localhost';
-- FLUSH PRIVILEGES;
