-- NavAuth initial schema (H2)
--
-- STARTING POINT ONLY. Regenerate with the Phase 0 capture test
-- (references/schema-capture.md) and prefer the captured output whenever it differs.
-- Target location: navauth-common/src/main/resources/migration/h2/V001_initial.sql
--
-- H2 performs best as a file/memory DB here; UUID and BOOLEAN are native types.

CREATE TABLE IF NOT EXISTS navauth_users (
  uuid                 UUID         NOT NULL,
  mojang_uuid          UUID,
  username             VARCHAR(255) NOT NULL,
  username_lowercase   VARCHAR(255) NOT NULL,
  credentials_required BOOLEAN      NOT NULL,
  CONSTRAINT pk_navauth_users PRIMARY KEY (uuid)
);

CREATE INDEX IF NOT EXISTS idx_navauth_users_mojang_uuid ON navauth_users (mojang_uuid);
CREATE INDEX IF NOT EXISTS idx_navauth_users_username ON navauth_users (username);
CREATE INDEX IF NOT EXISTS idx_navauth_users_username_lowercase ON navauth_users (username_lowercase);

CREATE TABLE IF NOT EXISTS navauth_credentials (
  uuid              UUID         NOT NULL,
  password_hash     VARCHAR(255),
  algo              VARCHAR(255),
  two_factor_secret VARCHAR(255),
  CONSTRAINT pk_navauth_credentials PRIMARY KEY (uuid)
);

CREATE TABLE IF NOT EXISTS navauth_users_sessions (
  uuid      UUID    NOT NULL,
  joined_at BIGINT  NOT NULL,
  left_at   BIGINT  NOT NULL,
  ip        BIGINT  NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_navauth_users_sessions_uuid ON navauth_users_sessions (uuid);
CREATE INDEX IF NOT EXISTS idx_navauth_users_sessions_left_at ON navauth_users_sessions (left_at);
