# Database

NavAuth stores accounts in H2 (file), SQLite, MySQL, MariaDB or PostgreSQL, configured in
`databaseConfig` of `general.yml`. A MySQL or PostgreSQL database can be shared with other
plugins: NavAuth only touches its own tables.

## Tables

| Table | Content |
| --- | --- |
| `navauth_users` | one row per account: uuid, Mojang UUID (premium accounts), name, whether credentials are required |
| `navauth_credentials` | password hash and its algorithm, 2FA secret |
| `navauth_flyway_schema_history` | the schema version (see below) |

Web panels and scripts may read `navauth_users` directly; its table and column names do not
change between minor releases.

## Schema migrations

NavAuth creates and upgrades its tables itself on startup with
[Flyway](https://documentation.red-gate.com/flyway). The applied versions are recorded in
`navauth_flyway_schema_history`:

- do not edit or drop this table, and do not create or alter NavAuth's tables by hand;
- an install made before this table existed is recognised by `navauth_users` and recorded as
  version 1 on its first start, without changing any table or row;
- if a migration fails, NavAuth stops the proxy instead of running on a half-migrated schema. Fix
  the cause shown in the log (or restore your backup) and start again.
