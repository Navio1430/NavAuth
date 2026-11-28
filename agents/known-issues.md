# Known issues

Bugs and inconsistencies that exist in the code today, so an agent is not surprised by them and
can fix them deliberately. Security weaknesses are never listed here ([security.md](security.md)).
When you fix one, delete its row in the same change (and add a `CHANGELOG.md` entry and tests).

## Commands

| Where | Issue |
| --- | --- |
| `velocity/command/admin/ReloadConfigAdminCommand.kt` | a failed reload sends the error and then the success message (`catch` without `return`) |
| `/verify2fa`, `/generate2faqr` | do not check the inverted `navauth.user.twofactorsetup` permission; only `/setup2fa` does |
| `/lookup profile`, `/migrateuser` | take `@Context sender: Player`, so the console cannot run them |
| `/migration start` (`MigrationRootCommand`) | not `@Async`: the whole migration blocks the console thread; its permission `navauth.root` is a literal, not in `Permissions.kt` |
| `/migrateuser` → `UserService.migrateData` | posts no `UserUsernameMigrationEvent` (`migrateUsername` does); `adminCmdCantMigrateToExistingPremiumAccount` gets the old name in `%USERNAME%` |
| `/changepassword`, `/forcesetpassword`, `/forcecracked` | do not run `PasswordValidator` |
| `ForceCrackedAdminCommand` (`/forcecracked`) | `thenAccept` without `exceptionally`: a failure after hashing is neither reported nor logged |

## API and events

| Where | Issue |
| --- | --- |
| `navauth-api/src/main/java/.../AuthSessionType.kt` | a Kotlin file in the Java API: the published artifact depends on the Kotlin standard library |
| `docs/docs/developers/api.md` | example `register(NavAuthListeners())` is not valid Java and shows an old version |
| `navauth-api-examples` | a comment says `setInitialServer(null)` disconnects the player; it does not |

## Configs and messages

| Where | Issue |
| --- | --- |
| `registerPasswordInvalidError` default | says an uppercase letter is required; by default a lowercase letter and a digit are |
| `TextComponent.applyPlaceholders` | uses `String.format`: a literal `%` in a message with placeholders throws, an unfilled `%KEY%` shows `null` |
| `configVersion` (`general.yml`, `messages.yml`) | nothing reads it and no config migration exists yet; `@Variable("CONFIG_VERSION")` lets one environment variable override it in both files |
| `migration.yml` | read only at startup; a change needs a proxy restart |
| `passwordsConfig.hashingAlgorithm` | values other than `BCRYPT`/`ARGON2` fail only at the first hash, with a message listing every algorithm as supported |

## Threads and hashing

| Where | Issue |
| --- | --- |
| encryption queue | `encryptionQueueMaxThreads` has no effect: the queue is unbounded, so the pool never grows past `encryptionQueueMinThreads` |
| `db` executor | 2 threads and no queue: a third simultaneous `updatePassword` is rejected |
| BCrypt | probably rejects passwords over 71 bytes; `PasswordValidator` has no maximum length (unverified) |

## Mojang profile API

| Where | Issue |
| --- | --- |
| `ProfileCache` | no size limit; nothing invalidates entries |

## Migrators

| Where | Issue |
| --- | --- |
| LibreLogin `LOGIT-SHA-*` | converted to PBKDF2 with 100 000 iterations; if LibreLogin stores plain salted SHA digests, these passwords never verify after import (unverified, untested) |
| LibreLogin Argon2 | conversion assumes parallelism 1 (untested; only BCrypt is tested end to end) |
| LibreLogin | a database error on one row rolls back its whole chunk |
| `NavAuthDatabaseMigrator` (`NAVAUTH`) | overwrites target rows with the same uuid; one damaged row (password without algorithm, 2FA secret under 16 characters) stops the run |
| both migrators | never close their source connection pool |

## Build and docs

| Where | Issue |
| --- | --- |
| `navauth-docs-gen/build.gradle.kts` | pins `litecommands-velocity` 3.10.6 by hand; the catalog says 3.11.0 |
| `buildSrc` | pins Shadow 9.2.0; the catalog says 9.6.1 |
| `VelocityServerSelectionService.getInitialServer` | KDoc says it throws `ServerNotFoundException`; it never does |
| MySQL 8.4 with `ssl: false` | first connection fails with "Public Key Retrieval is not allowed" (owner decides: `allowPublicKeyRetrieval=true` or a docs note) |
| `docs/docs/public/lookup/profile.webp` | still shows the removed "Last IP" line |

## Related

[README.md](README.md) · [security.md](security.md) · [../TESTING.md](../TESTING.md)
