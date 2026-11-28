-- NavAuth initial schema (SQLite)
--
-- STARTING POINT ONLY. Regenerate with the Phase 0 capture test
-- (references/schema-capture.md) and prefer the captured output whenever it differs.
-- Target location: navauth-common/src/main/resources/migration/sqlite/V001_initial.sql
--
-- SQLite has no native UUID/BOOLEAN; OrmLite stores UUID as VARCHAR(36) and boolean as
-- TINYINT(1)/INTEGER. SQLite supports CREATE INDEX IF NOT EXISTS.

CREATE TABLE IF NOT EXISTS navauth_users (
  uuid                 VARCHAR(36)  NOT NULL,
  mojang_uuid          VARCHAR(36),
  username             VARCHAR(255) NOT NULL,
  username_lowercase   VARCHAR(255) NOT NULL,
  credentials_required TINYINT(1)   NOT NULL,
  PRIMARY KEY (uuid)
);

CREATE INDEX IF NOT EXISTS idx_navauth_users_mojang_uuid ON navauth_users (mojang_uuid);
CREATE INDEX IF NOT EXISTS idx_navauth_users_username ON navauth_users (username);
CREATE INDEX IF NOT EXISTS idx_navauth_users_username_lowercase ON navauth_users (username_lowercase);

CREATE TABLE IF NOT EXISTS navauth_credentials (
  uuid              VARCHAR(36)  NOT NULL,
  password_hash     VARCHAR(255),
  algo              VARCHAR(255),
  two_factor_secret VARCHAR(255),
  PRIMARY KEY (uuid)
);

CREATE TABLE IF NOT EXISTS navauth_users_sessions (
  uuid      VARCHAR(36) NOT NULL,
  joined_at BIGINT      NOT NULL,
  left_at   BIGINT      NOT NULL,
  ip        BIGINT      NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_navauth_users_sessions_uuid ON navauth_users_sessions (uuid);
CREATE INDEX IF NOT EXISTS idx_navauth_users_sessions_left_at ON navauth_users_sessions (left_at);
