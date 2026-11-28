-- NavAuth initial schema (PostgreSQL)
-- Captured from OrmLite TableUtils.getCreateTableStatements.
-- OrmLite installs also have navauth_users_sessions (player session log, no longer used).
-- NavAuth never reads, writes or drops it; it is not created on new installs.

CREATE TABLE "navauth_users" ("uuid" VARCHAR(48) NOT NULL, "mojang_uuid" VARCHAR(48), "username" VARCHAR(255) NOT NULL, "username_lowercase" VARCHAR(255) NOT NULL, "credentials_required" BOOLEAN NOT NULL, CONSTRAINT "pk_navauth_users" PRIMARY KEY ("uuid"));
CREATE INDEX "navauth_users_mojang_uuid_idx" ON "navauth_users" ("mojang_uuid");
CREATE INDEX "navauth_users_username_lowercase_idx" ON "navauth_users" ("username_lowercase");
CREATE INDEX "navauth_users_username_idx" ON "navauth_users" ("username");

CREATE TABLE "navauth_credentials" ("uuid" VARCHAR(48) NOT NULL, "passwordHash" VARCHAR(255), "algo" VARCHAR(100), "twoFactorSecret" VARCHAR(255), CONSTRAINT "pk_navauth_credentials" PRIMARY KEY ("uuid"));
