# Migrators

`/migration start` imports accounts from another auth plugin's database (LibreLogin) into NavAuth's
database, or copies NavAuth's own tables to another database type (origin `NAVAUTH`). The source
is configured in `migration.yml`, the target is the plugin's database from `general.yml`. Plugin
migrators only add accounts: a row whose account already exists, or that is damaged, is skipped and
logged, and the run goes on.

Paths: `common/…` = `navauth-common/src/main/kotlin/pl/spcode/navauth/common/…`,
`velocity/…` = `navauth-velocity/src/main/kotlin/pl/spcode/navauth/velocity/…`.

## Where it lives

| Path | What |
| --- | --- |
| `common/migrate/MigrationManager.kt` | runs a migration: chunk loop, counters, summary, `isMigrating` |
| `common/migrate/MigrationOriginPluginType.kt` | origins: `NAVAUTH`, `LIBRELOGIN` |
| `common/migrate/MigratorFactory.kt` | origin → migrator instance (Guice) |
| `common/migrate/migrator/Migrator.kt` | the contract and `MigrationChunkResult` |
| `common/migrate/migrator/librelogin/LibreLoginMigrator.kt` | LibreLogin: source reads, hash conversion, store-or-skip |
| `common/migrate/migrator/librelogin/LibreLoginUser.kt` | `LibreLoginUserTable` (`librepremium_data`), `LibreLoginRow`, `LibreLoginUser` |
| `common/migrate/migrator/librelogin/LibreLoginUserConversion.kt` | `convertLibreLoginUser`, the pure conversion |
| `common/migrate/migrator/navauth/NavAuthDatabaseMigrator.kt` | database type copy |
| `common/migrate/error/*` | `MigrationException`, `SourceDatabaseConnectException`, `SourceTableNotFoundException` |
| `common/config/MigrationConfig.kt` | `migration.yml` |
| `velocity/command/root/MigrationRootCommand.kt` | `/migration start` |
| `navauth-docs-gen/…/generator/common/MigratedPluginTypesGenerator.kt` | `docs/docs/generated/migrated-plugin-types.md`: every origin except `NAVAUTH` |
| `navauth-common/src/test/kotlin/integration/migrate/*`, `unit/migrate/*` | tests (SQLite source → H2 target; pure conversion) |

## Configuration and command

- `MigrationConfig`: `chunkSize` (200), `sourceDatabaseConfig` (the same `DatabaseConfig` shape
  as `databaseConfig` in `general.yml`; H2/SQLite file names resolve in the plugin folder),
  `originPluginType` (default `LIBRELOGIN`). Read at startup only: after editing `migration.yml`,
  restart the proxy before migrating.
- `/migration start`: console or RCON only (`CommandSourceUtils.isConsoleOrRcon`), permission
  `navauth.root`. Not `@Async`: it returns when the migration ends. Refused while `isMigrating`.
  Prints `Migration failed: <message>` (the error is logged in full) or `Migrated successfully!`.
- Players must not be online while it runs (user docs: `docs/docs/migration/migration.md`).

## The Migrator contract

```kotlin
fun init()                                    // connect the source, check its table
fun getSourceUsersCount(): Long               // source rows, for progress logs
fun migrateNext(offset: Long, limit: Long): MigrationChunkResult
data class MigrationChunkResult(val read: Long, val migrated: Long, val skipped: Long)
```

- `read`: source rows read in this chunk, imported or skipped; `read == 0` ends the migration.
  `migrated`: NavAuth accounts written. `skipped`: rows not imported.
- `init` throws `SourceDatabaseConnectException` when the source cannot be reached and
  `SourceTableNotFoundException` when its table cannot be read. The source pool is named
  `NavAuth-migration-pool` (`MigrationManager.MIGRATION_SOURCE_POOL_NAME`).
- `MigrationManager.startPluginMigration` starts at offset 0 and adds `read` after each chunk,
  never `migrated`: a chunk of only skipped rows must not end the run. It logs
  `Processed <offset> / <total> users (<n> imported, <n> skipped)` per chunk and a summary.

## LibreLogin

Source table `librepremium_data`; every column is read as nullable `varchar(255)` so a damaged
row is skipped instead of failing the chunk.

| LibreLogin column | NavAuth |
| --- | --- |
| `uuid` | `navauth_users.uuid`, unchanged (offline, random or Mojang, depending on LibreLogin's UUID creator) |
| `premium_uuid` | `mojang_uuid` (`null` = cracked) |
| `last_nickname` | `username`; must match the current `usernamePattern` |
| `hashed_password`, `salt`, `algo` | `passwordHash`, `algo`; cracked players only |
| `secret` | `twoFactorSecret` |

Per chunk:

1. Read `limit` rows ordered by `uuid` from `offset` (source transaction).
2. Convert every row outside any target transaction (`convertOrSkip`): parse the uuids, validate
   the nickname, convert the password (cracked rows only), `convertLibreLoginUser`. An
   `InvalidRowException(reason)` logs `Skipping LibreLogin user <name> (uuid <uuid>): <reason>`.
3. One target transaction stores the converted rows (`storeOrSkip`): a row is skipped (info log)
   when NavAuth already has its name in any letter case, its uuid or its Mojang UUID; otherwise the
   credentials and the user are saved through the repositories.

`convertLibreLoginUser(lUser, username, hashedPassword)`:

- Premium (`premium_uuid` set): `User.premium(uuid, name, premiumUuid)` with
  `credentialsRequired` only when a 2FA secret exists, credentials = that secret only. The
  password is ignored, not even parsed: exactly the state `/premium` leaves.
- Cracked: password and 2FA secret kept; a row with neither returns `null` and is skipped.
- A secret shorter than 16 characters throws (`TOTPSecret`), reported as "invalid 2FA secret".

Password conversion (`getHashedPassword`; hasher formats in [accounts.md](accounts.md#hashing)):

| LibreLogin `algo` | LibreLogin `hashed_password` / `salt` | NavAuth `algo`: stored hash |
| --- | --- | --- |
| `BCrypt-<ver>` (e.g. `BCrypt-2A`) | `<cost>$<hash>` / bcrypt salt | `BCRYPT`: `$<ver lower case>$<cost>$<salt><hash>` |
| `Argon-2ID` | `<version>,<iterations>,<memoryKb>$<hash b64>` / salt b64 | `ARGON2`: `$argon2id$v=<version>$m=<memoryKb>,t=<iterations>,p=1$<salt>$<hash>`, `=` padding trimmed; `p=1` because LibreLogin stores no parallelism |
| `LOGIT-SHA-256`, `LOGIT-SHA-512` | hex / hex | `SHA256`, `SHA512`: `pbkdf2-sha256$<salt b64>$<hash b64>` (verified as PBKDF2, 100 000 iterations) |
| `SHA-256`, `SHA-512` | hex / hex | `LIBRELOGIN_SHA256`, `LIBRELOGIN_SHA512`: `SHA256$<saltHex>$<hashHex>` |
| anything else | — | skipped: unknown hashing algorithm |

Skip reasons: missing or malformed uuid, missing nickname, nickname not matching
`usernamePattern`, malformed `premium_uuid`, password without `algo` or `salt`, unknown algorithm,
malformed hash (the reason names only the algorithm: parser exceptions can carry parts of the
hash), invalid 2FA secret, cracked row without password and 2FA, account already in NavAuth.

Premium accounts that older versions imported with a password are repaired at the owner's next
Mojang-verified login (`UserService.removeLeftoverPremiumPassword`, [accounts.md](accounts.md)).

## NavAuth database type (`NAVAUTH`)

- The source is the current database (`migration.yml`), the target the new one (`general.yml`,
  already migrated by Flyway at startup). The source must have `navauth_users` and
  `navauth_credentials`; it is not migrated here.
- `MigrationManager.startDatabaseMigration` → `NavAuthDatabaseMigrator.init()` and `migrateAll()`:
  all of `navauth_users`, then all of `navauth_credentials`, `chunkSize` rows at a time ordered by
  `uuid`. It is not a `Migrator` (`MigratorFactory.getMigrator(NAVAUTH)` throws; the manager uses
  `getNavAuthMigrator()`).
- Every row is upserted by `uuid`: `batchUpsert` on MySQL/MariaDB (no key columns) and
  PostgreSQL/SQLite (`uuid`); **row by row on H2** through `UserRepositoryImpl` /
  `UserCredentialsRepositoryImpl`, because Exposed's H2 batch `MERGE ... USING (VALUES ...)` types
  each column from the batch's first row, and a NULL there (a cracked user's Mojang UUID, a
  2FA-only credentials row) nulls that column for every later row. A second run into an unchanged
  target adds or changes nothing (TESTING.md D16).
- Rows pass through the domain mappers: a cracked row is written with `credentials_required =
  true`; a credentials row with a password but no `algo`, or a 2FA secret shorter than 16
  characters, throws and stops the run (chunks already copied stay).

## Adding a migrator

1. Add a constant to `MigrationOriginPluginType` (admins write it in `migration.yml`, so it is a
   config contract), a branch in `MigratorFactory.getMigrator`, the name in the
   `originPluginType` comment of `MigrationConfig`; regenerate the docs
   (`./gradlew :navauth-docs-gen:generate`) and describe the plugin in
   `docs/docs/migration/migration.md`.
2. In `common/migrate/migrator/<plugin>/`: a source `Table` with every column nullable text, a raw
   row class, and a **pure conversion function** (like `convertLibreLoginUser`) unit-tested in
   `unit/migrate` without a database.
3. `init`: `DatabaseManager(sourceDatabaseConfig, pluginDirectory).connect(MIGRATION_SOURCE_POOL_NAME)`,
   then `DatabaseManager.connectExposed`, then probe the table.
4. `migrateNext`: **set-based reads** (`limit`/`offset`, ordered by a stable unique column);
   convert outside the target transaction; **skip and log damaged rows** with fixed reason
   strings; store in one target transaction; **never overwrite an existing account** (check name
   in any case, uuid and Mojang UUID before saving).
5. Keep the source plugin's UUID. Premium accounts keep only a 2FA secret. A password format no
   hasher verifies needs a new hasher and `HashingAlgorithm` constant ([accounts.md](accounts.md#rules)),
   never a lossy conversion.
6. Tests in `integration/migrate`: SQLite source (`SqliteSourceConfig`), H2 target
   (`DataPersistenceTestExtension`), one password per supported hash format that must verify after
   the import, damaged rows, existing accounts. Rows in [../TESTING.md](../TESTING.md) section M.

## Rules

- **Migrators only add accounts.** An existing account (same name in any letter case, same uuid
  or same Mojang UUID) is skipped and left untouched.
- A damaged or unusable source row is skipped and logged with its name, uuid and reason; it never
  stops the run or rolls back other rows.
- **No password data in logs**: no hashes, salts, TOTP secrets, and no exception messages from
  parsing them.
- The source plugin's UUID stays the account uuid ([accounts.md](accounts.md#how-the-uuid-is-derived));
  premium status comes from the source data, never from a profile API lookup.
- The offset advances by rows read, never by rows written.

## Traps

- `NAVAUTH` is the exception to "only add": it upserts by `uuid`, so it belongs in an empty target.
  Running it again after players used the new database reverts their rows to the source values.
- Only conversion errors are skipped. A database error while storing a row rolls back its whole
  chunk and stops the run.
- Only the BCrypt conversion is tested end to end (the password verifies after the import); no test
  imports an Argon2, `LOGIT-SHA` or LibreLogin `SHA` password and verifies it.
- A cracked LibreLogin row with only a 2FA secret becomes a 2FA-only cracked account (see the
  `/disable2fa` trap in [accounts.md](accounts.md#traps)).
- The source pool is never closed; every run opens another `NavAuth-migration-pool`.
- Offset paging assumes the source does not change during the run.

## Related

[accounts.md](accounts.md) · [persistence.md](persistence.md) · [configs-and-messages.md](configs-and-messages.md) ·
[commands-and-permissions.md](commands-and-permissions.md) · [docs-site.md](docs-site.md) ·
[../TESTING.md](../TESTING.md) (sections M and D)
