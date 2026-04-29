-- ========================================
-- Saju Backend Database Initialization
-- Flyway manages all table schemas.
-- This script only creates DB, user, and grants.
-- ========================================

CREATE DATABASE IF NOT EXISTS saju_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'saju'@'%' IDENTIFIED BY 'saju1234';
CREATE USER IF NOT EXISTS 'saju'@'localhost' IDENTIFIED BY 'saju1234';
CREATE USER IF NOT EXISTS 'saju'@'127.0.0.1' IDENTIFIED BY 'saju1234';

GRANT ALL PRIVILEGES ON saju_db.* TO 'saju'@'%';
GRANT ALL PRIVILEGES ON saju_db.* TO 'saju'@'localhost';
GRANT ALL PRIVILEGES ON saju_db.* TO 'saju'@'127.0.0.1';

FLUSH PRIVILEGES;
