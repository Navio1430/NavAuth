# Testing NavAuth

Every edge case NavAuth must handle, where its test lives and how to run it. A change that adds a
case adds a row here and a test; a case without an automated test says how it is checked by hand
or what is missing.

## Running the tests

| What | Command | Needs |
| --- | --- | --- |
| Everything in `navauth-common` | `./gradlew :navauth-common:test` | Docker (Testcontainers starts `mysql:8.4`, `mariadb:11.4`, `postgres:16-alpine`) |
| Without Docker | the same command: the MySQL, MariaDB and PostgreSQL classes are skipped (`@Testcontainers(disabledWithoutDocker = true)`) | nothing |
| One class | `./gradlew :navauth-common:test --tests 'integration.auth.LoginScenarioTests'` | |
| Velocity listeners | `./gradlew :navauth-velocity:test` | nothing |
| Whole build, formatting | `./gradlew build`, `./gradlew formatAll` | |

Reports: `navauth-common/build/reports/tests/test/index.html`,
`navauth-velocity/build/reports/tests/test/index.html`.

## Test layers

| Layer | Where (`navauth-common/src/test/kotlin/`) | Runs on | Proves |
| --- | --- | --- | --- |
| Unit | `unit/**` | JVM only | pure rules: hashers, validators, LibreLogin conversion, handshake identity check, config upgrades |
| Login scenarios | `integration/auth/LoginScenarioTests` | real services, H2, fake Mojang API | every username / premium / cracked path end to end |
| Repositories and schema | `integration/database/*`, one subclass per backend in `RepositoryIntegrationTestBackends.kt` | H2, SQLite in-process; MySQL, MariaDB, PostgreSQL in Docker | SQL and dialect differences, installs and upgrades |
| Migrators | `integration/migrate/*` | SQLite source → H2 target | imports from other auth plugins |
| Velocity listeners | `navauth-velocity/src/test/kotlin/unit/**` | JVM only, mocked Velocity API | session ids, fail-closed login and session listeners, listener priorities, commands allowed before login |
| Velocity runtime | manual, see [below](#velocity-runtime-checks) | real proxy | listeners, kicks, limbo routing, the shaded jar |

**How `LoginScenarioTests` works.** `join(name, client)` performs what the Velocity login listeners
do with one connection: username resolution (PreLogin), online-mode authentication, the identity
check (GameProfileRequest/LoginEvent) and premium account creation (PostLogin), and returns the
outcome (`PremiumLogin`, `PasswordLogin`, `Register`, `Denied(reason)`, `MojangAuthFailed`,
`IdentityMismatch`, `NotAuthenticatedByMojang`). `client` is the Mojang account the client really
is (`null` = cracked client); the test is the source of truth for who owns a name.
`proxyOnlineMode = false` simulates a connection Mojang did not authenticate: it carries its
offline UUID. The fake Mojang API
(`FakeProfileService`) answers like the real one, case-insensitively: `mojangAccount(name)`,
`mojangRename(id, from, to)`, `failing = true`. A **stale cache** is simulated by not updating the
fake after a change on "Mojang". When the Velocity listeners change, keep `join` in step.

## Edge cases

✅ automated · 🖐 manual check · ❌ not covered yet

### L — Login and usernames

| # | Case | Expected | Test |
| --- | --- | --- | --- |
| L01 | New premium name | online mode; account with the Mojang UUID, no password | ✅ LoginScenarioTests |
| L02 | New premium name in the wrong letter case | refused, `PremiumUsernameNotIdentical` | ✅ LoginScenarioTests |
| L03 | New cracked name | register | ✅ LoginScenarioTests |
| L04 | Cracked client with a premium name nobody registered | online mode; the client fails Mojang auth | ✅ LoginScenarioTests |
| L05 | Profile API down (new and known names) | refused, `ProfileAPIFailure` | ✅ LoginScenarioTests, UsernameResolutionServiceTests; ❌ known names should not need the API |
| L06 | Returning premium player, wrong letter case | refused | ✅ LoginScenarioTests |
| L07 | Premium player renamed on Mojang | the account follows to the new name, same UUID | ✅ LoginScenarioTests |
| L08 | … joins with the wrong case of the new name | renamed, then refused; the right spelling logs in | ✅ LoginScenarioTests |
| L09 | Mojang letter-case change of the own name | account renamed to the new spelling | ✅ LoginScenarioTests |
| L10 | Premium player renamed onto a cracked account's name | that account's password login; the premium account is not renamed | ✅ LoginScenarioTests |
| L11 | Premium player with 2FA | 2FA code still required | ✅ LoginScenarioTests |
| L12 | Premium name now owned by another Mojang account | refused, `PremiumUsernameOwnedByAnotherAccount`; stored account untouched | ✅ LoginScenarioTests |
| L13 | … claimed in another letter case | refused; stored account not renamed | ✅ LoginScenarioTests |
| L14 | … with a stale cached profile naming the previous owner | refused after Mojang auth (`IdentityMismatch`) | ✅ LoginScenarioTests, AuthHandshakeSessionTests |
| L15 | … with a stale cached "not found" | refused after Mojang auth | ✅ LoginScenarioTests |
| L16 | Rename made at PreLogin from a stale answer | grants nothing; undone by the owner's next join | ✅ LoginScenarioTests |
| L17 | Previous owner joins with the new name | the old name is free; the new owner gets a new account | ✅ LoginScenarioTests |
| L18 | Claimant already has an account under another name | their own account is renamed | ✅ LoginScenarioTests |
| L19 | Two premium players swap names | both refused until one is resolved | ✅ LoginScenarioTests (lock-out); ❌ automatic resolution |
| L20 | Cracked client with a released premium account's name | online mode; refused | ✅ LoginScenarioTests |
| L21 | Cracked account whose name is premium (same case) | password login, also for the Mojang owner; never auto-login | ✅ LoginScenarioTests |
| L22 | Cracked account, premium name in another case | refused, `NonPremiumWithPremiumConflict` | ✅ LoginScenarioTests |
| L23 | Cracked account, wrong letter case | refused, `NonPremiumUsernameNotIdentical` | ✅ LoginScenarioTests |
| L24 | Name not matching `usernamePattern` | refused before resolution | ✅ ValidatorTests (pattern); 🖐 kick message |
| L25 | Two connections register the same new name at once | one account | ❌ both can be stored (no `UNIQUE`) |
| L26 | Mojang auth takes longer than the 15 s handshake session | session expires 15 s after PreLogin; "Auth session expired" kick at `LoginEvent` | ✅ AuthHandshakeSessionServiceTests (fake clock), LoginListenersSecurityTests |
| L27 | Identity mismatch reaches Velocity | denied in `LoginEvent`, never reaches a backend | ✅ LoginListenersSecurityTests (mocked Velocity); 🖐 V06 needs two real Mojang accounts |
| L28 | Two connections with the same name at the same time | separate handshake and auth sessions | ✅ LoginListenersSecurityTests |
| L29 | Premium name over IPv6 | online mode enforced, as over IPv4 | ✅ LoginListenersSecurityTests |
| L30 | Unexpected error at PreLogin | denied with `unexpectedLoginErrorKickMessage` | ✅ LoginListenersSecurityTests |
| L31 | Handshake session left by an earlier connection with the same id | dropped at PreLogin, never reused | ✅ LoginListenersSecurityTests |
| L32 | Premium account reached by a connection Mojang did not authenticate | refused with `unexpectedLoginErrorKickMessage`; nothing changes | ✅ LoginScenarioTests, AuthHandshakeSessionTests, LoginListenersSecurityTests |
| L33 | … with a new premium name | refused; no account stored (never one with the offline UUID as Mojang UUID) | ✅ LoginScenarioTests, AuthHandshakeSessionTests, LoginListenersSecurityTests |
| L34 | No handshake session at GameProfileRequest or LoginEvent | no exception; denied at `LoginEvent` ("Auth session expired") | ✅ LoginListenersSecurityTests |
| L35 | Unexpected error at GameProfileRequest, LoginEvent or PostLogin | handshake session dropped, connection denied, player disconnected | ✅ LoginListenersSecurityTests |
| L36 | Player without an auth session | cannot chat; gets no initial server | ✅ SessionListenersSecurityTests |
| L37 | Unexpected error at PlayerChooseInitialServer or ServerPreConnect | no initial server, connection denied, player disconnected | ✅ SessionListenersSecurityTests |
| L38 | Another plugin's `LoginEvent` listener would allow a connection NavAuth denied | NavAuth's `LoginEvent` listener runs last (`priority = Short.MIN_VALUE`) and only ever denies; a connection another plugin denied stays denied | ✅ LoginListenersSecurityTests (annotation and behaviour); 🖐 V08 |
| L39 | Auth commands renamed, re-aliased or disabled in `commands.yml` | a player who is not logged in can run `login`, `register` and `2fa` only under their configured names and aliases (any letter case), not under replaced ones nor when disabled; the command list shows only those | ✅ CommandsListenersTests; 🖐 V08 |
| L40 | A profile API answers with an error status or an unexpected body | the lookup fails (`ProfileApiFetchException`), never "not found"; Mojang's own "not found" decides even when another API fails | ✅ MojangProfileServiceImplTest, MineToolsProfileServiceTest, CompositeProfileServiceTest |

### S — Account switches and account commands

| # | Case | Expected | Test |
| --- | --- | --- | --- |
| S01 | Cracked → premium (`/premium`) | same UUID, password removed | ✅ LoginScenarioTests |
| S02 | Cracked with 2FA → premium | only the 2FA secret kept | ✅ LoginScenarioTests |
| S03 | Premium → cracked (`/forcecracked`) | same UUID; password login, also from the Mojang account | ✅ LoginScenarioTests |
| S04 | Premium with 2FA → cracked | password set, 2FA kept | ✅ LoginScenarioTests |
| S05 | 2FA on a premium account | enabling requires credentials; disabling removes them | ✅ LoginScenarioTests |
| S06 | Disabling 2FA on a cracked account | password kept | ✅ LoginScenarioTests |
| S07 | Rename onto a name another account holds (any case) | `UsernameAlreadyTakenException` | ✅ LoginScenarioTests |
| S08 | Rename to the same name / to another letter case | refused / allowed | ✅ LoginScenarioTests |
| S09 | Cracked data moved onto a premium name (`/migrateuser`) | refused | ✅ LoginScenarioTests |
| S10 | Premium player switches themself to cracked | — | ❌ not supported |
| S11 | `/setup2fa` for a premium account (no password) | the setup starts without a password step | ✅ TwoFactorCommandsTests |
| S12 | Cracked account with only a 2FA secret | `/disable2fa` is refused (`twoFactorDisablePasswordRequiredError`) and the credentials stay; `/forcesetpassword` creates a credentials row when the account has none | ✅ TwoFactorCommandsTests, TwoFactorCredentialsTests |
| S13 | Registering a uuid under which a credentials row without a user exists | the row is replaced by the new credentials | ✅ LoginScenarioTests |

### P — Passwords, 2FA, hashing

| # | Case | Expected | Test |
| --- | --- | --- | --- |
| P01 | Every hasher (BCrypt, Argon2, SHA, LibreLogin SHA) | right password verifies, wrong one does not | ✅ `unit/crypto/*` |
| P02 | LibreLogin hash formats | converted hashes verify | ✅ LibreLoginSHACredentialsHasherTests, LibreLoginMigratorTests (BCrypt end to end) |
| P03 | Password rules (length, lowercase, digit, optional uppercase/special, whitespace) | as configured | ✅ ValidatorTests |
| P04 | TOTP codes, time window | | ✅ TOTP2FATests |
| P05 | Encryption queue: one task per player, removed on disconnect | | ✅ EncryptionQueueServiceTests |
| P06 | Hash moved to the configured algorithm on login | | ❌ not implemented |
| P07 | Too many login attempts, login/register timeout | kick with the configured message | 🖐 |

### D — Database and upgrades (each test runs on H2, SQLite, MySQL, MariaDB and PostgreSQL)

| # | Case | Expected | Test |
| --- | --- | --- | --- |
| D01 | Fresh empty database | V001 creates the tables | ✅ SchemaMigrationTests |
| D02 | Database shared with other plugins' tables | V001 still runs (baseline 0) | ✅ SchemaMigrationTests |
| D03 | OrmLite install with users, credentials and session log | baselined at 1; every row readable; session log untouched | ✅ SchemaMigrationTests |
| D04 | Migrating an up-to-date schema again | no change | ✅ SchemaMigrationTests |
| D05 | History table without the NavAuth tables | startup stops | ✅ SchemaMigrationTests |
| D06 | OrmLite install missing `navauth_credentials` | startup stops | ✅ SchemaMigrationTests |
| D07 | Duplicate names (any letter case) | lowest `uuid` wins, warning | ✅ UserRepositoryTests, LoginScenarioTests |
| D08 | Duplicate Mojang UUIDs | lowest `uuid` wins, warning | ✅ UserRepositoryTests |
| D09 | Exact-name lookup on MySQL/MariaDB (case-insensitive collation) | still exact | ✅ UserRepositoryTests |
| D10 | Saving an existing user | upsert by `uuid`, no duplicate | ✅ UserRepositoryTests |
| D11 | Dialect types (TINYINT/BOOLEAN, VARCHAR UUIDs, null Mojang UUID) | round-trip | ✅ UserRepositoryTests, UserCredentialsRepositoryTests |
| D12 | Credentials with only a 2FA secret; delete by user | | ✅ UserCredentialsRepositoryTests |
| D13 | UUIDs typed in upper case by hand in old rows | not found on PostgreSQL/H2/SQLite | ❌ untested, rare |
| D14 | MySQL accent-insensitive collation with a non-ASCII `usernamePattern` | names like `zolw`/`żółw` collide | ❌ note for admins; ASCII by default |
| D15 | `UNIQUE`, foreign key, `CHECK` constraints | | ❌ not implemented |
| D16 | Database type migration (`NAVAUTH` origin, e.g. H2 → MySQL), in chunks | every user and credentials row copied with all its values, also when a chunk starts with a NULL (H2 targets upsert row by row: an H2 batch `MERGE` turns later values of that column into NULL); a second run adds or changes nothing | ✅ NavAuthDatabaseMigratorTests (SQLite → H2) |
| D17 | A transaction fails (e.g. the connection is lost) | the error surfaces once; the block never runs twice (Exposed `defaultMaxAttempts = 1`) | ✅ ExposedConfigTests |
| D18 | Migrations started on a thread whose context class loader cannot see NavAuth (Velocity's) | Flyway still finds its plugins and the scripts (it is given NavAuth's class loader) | ✅ SchemaMigrationTests |
| D19 | H2 file created by OrmLite: upper-case table and column names (`NAVAUTH_USERS.PASSWORDHASH`) | baselined at 1; every row readable (the H2 URL sets `CASE_INSENSITIVE_IDENTIFIERS=TRUE`) | ✅ H2OrmLiteUpgradeTests (H2 only) |
| D20 | `poolSize` of 1 | migrations and startup work (Flyway runs on its own short-lived pool of 2 connections) | ✅ DatabaseManagerPoolTests |

### C — Configs

| # | Case | Expected | Test |
| --- | --- | --- | --- |
| C01 | `general.yml` from 0.3.0 with `sessionsConfig` | loads; admin values kept | ✅ ConfigUpgradeTests |
| C02 | `messages.yml` without a key added later | default added; admin texts kept | ✅ ConfigUpgradeTests |
| C03 | Invalid `usernamePattern` | falls back to the Minecraft pattern | ✅ ValidatorTests |

### A — API and events

| # | Case | Expected | Test |
| --- | --- | --- | --- |
| A01 | API listeners with different `@Subscribe` priorities | higher priority runs first, equal priorities in registration order; `EventPriority` constants usable from other packages | ✅ EventBusPriorityTests |

### M — Migrators

| # | Case | Expected | Test |
| --- | --- | --- | --- |
| M01 | LibreLogin cracked player with a BCrypt password | password verifies after import | ✅ LibreLoginMigratorTests |
| M02 | LibreLogin premium player with a password (#46) | no password asked | ✅ LibreLoginMigratorTests, LibreLoginUserConversionTests |
| M03 | LibreLogin premium player with 2FA | only the 2FA secret | ✅ both |
| M04 | LibreLogin `RANDOM` UUID creator | LibreLogin UUID kept, Mojang UUID stored | ✅ LibreLoginUserConversionTests |
| M05 | Cracked row without password and 2FA | skipped | ✅ both |
| M06 | Name (any case), uuid or Mojang UUID already in NavAuth | skipped, existing account untouched | ✅ LibreLoginMigratorTests |
| M07 | Missing or invalid nickname or uuid, malformed premium uuid, unknown or missing hash algorithm, malformed hash or 2FA secret | row skipped and logged with the reason (no password data), counted in the summary; the other rows are imported, and a chunk of only skipped rows does not end the migration | ✅ LibreLoginMigratorTests |
| M08 | AuthMe, JPremium, nLogin, FastLogin, LimboAuth | | ❌ not implemented |
| M09 | Premium account an earlier LibreLogin import left with a password (#46); the owner joins, verified by Mojang | password removed, automatic login | ✅ LoginScenarioTests |
| M10 | … with 2FA | only the 2FA secret kept; asked for the code, never a password | ✅ LoginScenarioTests |
| M11 | … a connection Mojang did not verify (cracked client, offline, another account) | nothing changes | ✅ LoginScenarioTests |
| M12 | Cracked account with a password | password kept | ✅ LoginScenarioTests |

### Velocity runtime checks

Manual, with the shaded jar from `./gradlew :navauth-velocity:shadowJar` on the local network in
`navauth-velocity/docker-compose.yaml` (Velocity, Paper, limbo, PostgreSQL). Cracked clients can be
any offline-mode client; premium checks need real Mojang accounts (L27 needs two).

| # | Check |
| --- | --- |
| V01 | The jar starts on H2, SQLite, MySQL, MariaDB and PostgreSQL: fresh, shared schema, and on a 0.3.0 database (Flyway and Exposed find their relocated service files) |
| V02 | Cracked register, login, wrong password, 2FA setup and login; premium auto-login |
| V03 | An unauthenticated player cannot reach a non-limbo server |
| V04 | Disconnect during login or register closes the session and the hashing task |
| V05 | `/lookup profile` shows no IP; `/lookup sessions` no longer exists |
| V06 | L27: a premium account that is not the stored owner is kicked with `premiumUsernameOwnedByAnotherAccountError` |
| V07 | Premium auto-login and cracked login work over IPv6 like over IPv4 |
| V08 | With `login` renamed in `commands.yml`, a player who is not logged in logs in with the new name and alias; a plugin allowing every `LoginEvent` does not let a denied connection in |
