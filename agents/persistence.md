# Persistence

Accounts live in two tables accessed through JetBrains Exposed (DSL only); Flyway owns the schema.
Five databases are supported: H2 (file), SQLite, MySQL, MariaDB (through the MySQL driver) and
PostgreSQL. Servers upgrade with data in place, so every rule below protects existing installs.

## Where it lives

| Path | What |
| --- | --- |
| `common/infra/database/DatabaseManager.kt` | Hikari pool (`connect`), migrations (`migrate`), Exposed `Database` (`connectAndInit`); `connectExposed` / `exposedDatabaseConfig` (silent `sqlLogger`) in its companion |
| `common/infra/database/SchemaMigrations.kt` | Flyway setup and baseline rules, `schemaVersion`, `tableExists`; used by the plugin and the tests |
| `common/infra/database/DatabaseDriverType.kt` | driver class, JDBC URL format, migration directory per driver |
| `common/infra/database/DatabaseConfig.kt` | `databaseConfig` section of `general.yml` (driver, pool, credentials, SSL) |
| `navauth-common/src/main/resources/migration/<h2\|mysql\|mariadb\|postgresql\|sqlite>/` | Flyway scripts, `V001_initial.sql` (the schema OrmLite used to create) |
| `common/infra/persistence/exposed/UsersTable.kt`, `CredentialsTable.kt` | table objects |
| `common/infra/persistence/exposed/UuidVarchar.kt`, `DialectSupport.kt` | UUIDs as `VARCHAR(48)` text; `isMysqlFamily()` for upserts |
| `common/infra/persistence/impl/user/UserRepositoryImpl.kt`, `impl/credentials/UserCredentialsRepositoryImpl.kt` | repositories |
| `common/infra/persistence/impl/common/TransactionServiceImpl.kt` | `TransactionService.inTransaction` |
| `common/infra/persistence/mapper/*` | `ResultRow` → domain mapping |
| `common/module/DataPersistenceModule.kt` | Guice bindings; provides the Exposed `Database` |

## Schema

| Table | Columns |
| --- | --- |
| `navauth_users` | `uuid` PK, `mojang_uuid` (null = cracked), `username`, `username_lowercase`, `credentials_required` |
| `navauth_credentials` | `uuid` PK (the user's), `passwordHash`, `algo` (`HashingAlgorithm` name), `twoFactorSecret` |
| `navauth_flyway_schema_history` | Flyway's history |

There are no `UNIQUE` constraints on `username_lowercase` or `mojang_uuid` and no foreign key from
credentials to users (inherited from OrmLite). Installs from before 0.4.0 also have
`navauth_users_sessions` (an old session log with IP addresses): NavAuth never reads, writes,
recreates or drops it; removing it is the admin's decision.

## How it works

- **Startup:** `connect` builds the Hikari pool (MySQL/MariaDB get `useInformationSchema=false`:
  MySQL Connector/J's metadata lookup fails on MariaDB 11.4+); `DatabaseManager.migrate` runs
  Flyway; then the Exposed `Database` is created. Repositories may be resolved only after that.
- **Flyway connections:** Flyway holds two connections at once on H2 and PostgreSQL, and
  `SchemaMigrations.schemaVersion` (Flyway `info()`) needs two as well. `DatabaseManager.migrate`
  runs on the plugin pool when `poolSize` is at least 2; below that it opens its own short-lived
  pool of 2 connections (copied from the plugin pool's settings) and closes it afterwards. The
  plugin pool is opened first and keeps the configured size, so an in-memory database survives the
  migration pool.
- **Baseline:** without a history table, a schema that has `navauth_users` is an OrmLite install
  and is baselined at version 1 (V001 skipped, nothing changed); any other schema, empty or shared
  with other plugins, is baselined at 0 so V001 runs. After migrating, `navauth_users` and
  `navauth_credentials` must exist or startup fails.
- **Flyway class loader:** `Flyway.configure(SchemaMigrations::class.java.classLoader)`. Velocity's
  context class loader cannot see the plugin jar; with it Flyway finds no scripts.
- **Transactions:** repositories open `transaction(database)` themselves; work spanning
  repositories goes through `TransactionService.inTransaction` (nested Exposed transactions join
  the outer one). Exposed retries are off (`defaultMaxAttempts = 1`): a block never runs twice.
- **Lookups:** `findByUsernameIgnoreCase` compares `username_lowercase` with
  `username.lowercase()`. Duplicates are expected: every lookup orders by `uuid` and takes the first
  row, logging a warning that names the duplicates. `findByExactUsername` queries the lowercase
  column and filters the exact spelling in Kotlin.
- **Writes:** `save` is an upsert by `uuid` (MySQL/MariaDB `ON DUPLICATE KEY UPDATE` without key
  columns, others `ON CONFLICT (uuid)`) and serves changes to existing accounts. `insert`
  (`UserRepository`, `UserCredentialsRepository`) creates a row and fails when the uuid exists; it
  never changes an existing row.

## Rules

- **Flyway owns the schema.** Nothing creates or alters tables outside Flyway (`SchemaUtils` only in
  tests).
- A schema change adds the same version to **all five** dialect directories
  (`V002_<description>.sql`, separator `_`).
- **A released migration is never edited, renamed or deleted** (checksums are validated at startup;
  a mismatch stops every server that ran it).
- **Expand, then contract:** add in one release, drop or rename only in a later one, once no
  released code reads it. Never drop or rewrite data (`DROP`, destructive `UPDATE`, new
  `UNIQUE`/`NOT NULL` on existing columns) without the owner's decision and an upgrade note.
- Migrations must succeed on real, imperfect data (duplicates exist). Set-based SQL only, never a
  loop over users; networks have hundreds of thousands of rows.
- A new table or column declares `NOT NULL`, `UNIQUE`, foreign keys and `CHECK` from its first
  migration. Tightening an existing column takes two releases: report and clean up offending rows,
  then add the constraint in a migration that resolves leftovers without deleting rows.
- Table and column names are a public contract (web panels read `navauth_users`).
- A failed migration stops the proxy; never run on a half-migrated schema. The changelog says
  whether a release can be rolled back after its migrations ran.
- Open every Exposed `Database` with `DatabaseManager.connectExposed` (plugin, migration sources,
  tests). Exposed DSL only, no DAO entities. Transactions contain database work only: no HTTP,
  hashing or player messages inside.
- `exposedDatabaseConfig` sets a silent `sqlLogger`: Exposed never logs statements, because its
  default logger prints every bound value. Do not add Exposed's default or stdout logger.
- Keep lookups tolerant of duplicate rows and keep `username_lowercase` normalisation identical
  (`String.lowercase()`, locale-independent).
- When replacing a persistence call, keep its edge-case semantics (duplicates, nulls, empty
  strings) or record the difference as a breaking change.

## Traps

- **H2 identifier case:** OrmLite created H2 tables and columns upper case (`NAVAUTH_USERS`),
  V001 and Exposed quote lower case. The H2 URLs add `CASE_INSENSITIVE_IDENTIFIERS=TRUE`; without it
  every query on an upgraded H2 database fails. Test D19.
- **H2 `batchUpsert`:** Exposed's `MERGE ... USING (VALUES ...)` types each column from the batch's
  first row; a NULL there nulls that column for every later row. Upsert row by row on H2.
- **MySQL/MariaDB collation** is case-insensitive: `username = ?` also matches other letter cases.
- **MySQL/MariaDB DDL auto-commits:** a failed script leaves half its changes; keep each statement
  safe to re-run or one change per script. **SQLite** has limited `ALTER TABLE`: create-copy-swap.
- **MySQL/MariaDB updates** cannot subquery the table being updated; subquery the other table.
- **MySQL 8.4 with `ssl: false`** fails the first connection with "Public Key Retrieval is not
  allowed" ([known-issues.md](known-issues.md)).
- UUIDs are text; a UUID typed in upper case by hand is not found on PostgreSQL, H2 or SQLite.

## Testing

Repository and schema tests run on all five backends: one subclass per backend in
`integration/database/RepositoryIntegrationTestBackends.kt` (Docker classes skip without Docker).
A new migration gets an upgrade test from the previous schema with data, the OrmLite schema
included (`SchemaMigrationTests`, `H2OrmLiteUpgradeTests`). See [../TESTING.md](../TESTING.md)
section D.

## Related

[accounts.md](accounts.md) · [migrators.md](migrators.md) · [build-and-release.md](build-and-release.md) ·
[security.md](security.md)
