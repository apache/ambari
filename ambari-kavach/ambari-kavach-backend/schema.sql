-- =============================================================================
-- Ambari Kavach — Database Schema
-- Run once against MySQL before starting the application:
--   mysql -u root -p < schema.sql
-- =============================================================================

CREATE DATABASE IF NOT EXISTS ambari_kavach
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE ambari_kavach;

-- -----------------------------------------------------------------------------
-- ambari_onboarding
-- One row per registered Ambari cluster.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ambari_onboarding (
  ambari_server   VARCHAR(255)  NOT NULL,
  http_method     VARCHAR(10)   NOT NULL DEFAULT 'http',
  port            INT           NOT NULL DEFAULT 8888,
  vault_password  TEXT          NOT NULL,           -- Fernet-encrypted vault service-account password
  admin_dr_password TEXT        NOT NULL,           -- Fernet-encrypted disaster-recovery admin password
  manager_emails  TEXT          DEFAULT NULL,       -- JSON array of per-cluster manager emails
  single_user_mode TINYINT(1)   NOT NULL DEFAULT 0, -- 1 = only one active temp user at a time
  dr_compromised  TINYINT(1)    NOT NULL DEFAULT 0, -- 1 = DR password exposed; blocks user creation
  PRIMARY KEY (ambari_server)
);

-- -----------------------------------------------------------------------------
-- ambari_manager_users
-- Every temporary Ambari user ever issued through Kavach.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ambari_manager_users (
  id              INT           NOT NULL AUTO_INCREMENT,
  ambari_server   VARCHAR(255)  NOT NULL,
  user_name       VARCHAR(255)  NOT NULL,
  email           VARCHAR(255)  NOT NULL,           -- Kavach user who requested this account
  hash_password   TEXT          NOT NULL,           -- bcrypt hash of the temp password
  expire_time     DATETIME      NOT NULL,           -- When the account is auto-deleted
  pass_flag       TINYINT(1)    NOT NULL DEFAULT 1, -- 1 = active, 0 = expired / deleted
  created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at      DATETIME      DEFAULT NULL,       -- Populated when deleted early or on expiry
  role            VARCHAR(64)   NOT NULL,           -- CLUSTER.ADMINISTRATOR | CLUSTER.OPERATOR | CLUSTER.USER
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_server (user_name, ambari_server)
);

-- -----------------------------------------------------------------------------
-- ambari_vault_major_audit
-- Immutable log of major system events (cluster registration, DR access, etc.)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ambari_vault_major_audit (
  id            INT           NOT NULL AUTO_INCREMENT,
  actor_email   VARCHAR(255)  NOT NULL,             -- Who triggered the event
  audit_event   VARCHAR(255)  NOT NULL,             -- Event type (see below)
  impact_entity VARCHAR(255)  NOT NULL,             -- Cluster hostname or username affected
  event_time    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

-- Audit event types written by the application:
--   CLUSTER_REGISTERED          — new cluster registered via /api/register
--   MANAGER_REREGISTRATION_DONE — manager re-registered a cluster via /api/re-register
--   AMBARI_DR_COMPROMISED       — DR password revealed via building_on_fire.py
--   USER_CREATED                — temporary user created via /create_user
--   USER_DELETED                — manager force-deleted a user via /manager/delete_user

