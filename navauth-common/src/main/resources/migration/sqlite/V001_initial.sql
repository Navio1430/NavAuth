-- NavAuth initial schema (SQLite)
-- Captured from OrmLite TableUtils.getCreateTableStatements.
-- OrmLite installs also have navauth_users_sessions (player session log, no longer used).
-- NavAuth never reads, writes or drops it; it is not created on new installs.

CREATE TABLE "navauth_users" ("uuid" VARCHAR NOT NULL, "mojang_uuid" VARCHAR, "username" VARCHAR NOT NULL, "username_lowercase" VARCHAR NOT NULL, "credentials_required" BOOLEAN NOT NULL, PRIMARY KEY ("uuid"));
CREATE INDEX "navauth_users_mojang_uuid_idx" ON "navauth_users" ("mojang_uuid");
CREATE INDEX "navauth_users_username_lowercase_idx" ON "navauth_users" ("username_lowercase");
CREATE INDEX "navauth_users_username_idx" ON "navauth_users" ("username");

CREATE TABLE "navauth_credentials" ("uuid" VARCHAR NOT NULL, "passwordHash" VARCHAR, "algo" VARCHAR, "twoFactorSecret" VARCHAR, PRIMARY KEY ("uuid"));
