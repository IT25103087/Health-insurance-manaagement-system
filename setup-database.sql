-- ====================================================================
-- HealthShield Sri Lanka - Healthcare Insurance Management System
-- MySQL Database Initialization Script
-- ====================================================================

-- 1. Create database if it does not already exist
CREATE DATABASE IF NOT EXISTS `health_insurance_db`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 2. Select database for execution
USE `health_insurance_db`;

-- Note: Spring Boot JPA Hibernate ddl-auto is configured to 'update'.
-- When the application starts, all tables, foreign keys, indexes, and
-- seed records (users, plans, policies, claims, reviews, payments)
-- will be generated and seeded automatically by PolicyDataInitializer.

-- ====================================================================
-- Optional: If you wish to create a dedicated database user:
-- ====================================================================
-- CREATE USER IF NOT EXISTS 'health_user'@'localhost' IDENTIFIED BY 'Health#Secure2026';
-- GRANT ALL PRIVILEGES ON health_insurance_db.* TO 'health_user'@'localhost';
-- FLUSH PRIVILEGES;
