# Accounts, credentials and 2FA

A NavAuth account is one `navauth_users` row (`User`) plus at most one `navauth_credentials` row
(`UserCredentials`) under the same UUID. That UUID is what backends see and key player data by; it
is fixed at creation, while the name, the premium flag and the credentials can change. Passwords
are hashed on a bounded encryption queue; 2FA is TOTP (RFC 6238).

Paths: `common/…` = `navauth-common/src/main/kotlin/pl/spcode/navauth/common/…`,
`velocity/…` = `navauth-velocity/src/main/kotlin/pl/spcode/navauth/velocity/…`.

## Where it lives

| Path | What |
| --- | --- |
| `common/domain/user/User.kt` | `User`, value classes `UserUuid`, `MojangId`, `Username`; factories `nonPremium` / `premium` |
| `common/domain/credentials/UserCredentials.kt` | `UserCredentials`, `TOTPSecret` |
| `common/domain/credentials/HashingAlgorithm.kt` | algorithm enum; its names are stored in `navauth_credentials.algo` |
| `common/application/user/UserService.kt` | every account state change |
| `common/application/credentials/UserCredentialsService.kt` | store/delete credentials (`storeNewUserCredentials` for a new account), queued hash and verify, `updatePassword`, 2FA code checks (`findUnusedCodeStep`, `useCodeStep`, `verifyCode`) |
| `common/application/credentials/CredentialsHasherFactory.kt` | hasher for a stored algorithm, default hasher from config |
| `common/infra/crypto/hasher/*` | BCrypt, Argon2, SHA (PBKDF2) and LibreLogin SHA hashers |
| `common/infra/crypto/queue/EncryptionQueueServiceImpl.kt` | encryption queue |
| `common/infra/crypto/TOTP2FA.kt`, `common/shared/qr/QRCodeGenerator.kt` | TOTP codes and secrets, QR code in chat |
| `common/application/validator/UsernameValidator.kt`, `PasswordValidator.kt` | name and password rules |
| `common/config/PasswordsConfig.kt`, `EncryptionQueueConfig.kt`, `TwoFactorAuthConfig.kt`, `UsernamesConfig.kt` | their sections of `general.yml` (read at startup only) |
| `velocity/command/user/*`, `velocity/command/admin/*` | account commands |
| `velocity/application/totp/TotpSetupSessionService.kt` | 2FA setup sessions |
| `velocity/listener/velocity/LoginListeners.kt` | new premium accounts (`createAndStorePremiumUser`), stored identity applied in `GameProfileRequestEvent` |

## The account model

`User(uuid, username, credentialsRequired, mojangUuid)`:

- `uuid` — primary key and the identity backends see (at login the connection's profile gets the
  stored `uuid` and `username`, skin properties kept).
- `username` — stored spelling. `username_lowercase` is `username.lowercase()` (Kotlin,
  locale-independent), written by `UserRepositoryImpl.save`; every case-insensitive lookup
  lower-cases its input the same way. Duplicate rows are tolerated, see [persistence.md](persistence.md).
- `mojangUuid` — `null` = cracked (non-premium); set = premium, owned by that Mojang account.
  `isPremium` is `mojangUuid != null`. It often differs from `uuid`.
- `credentialsRequired` — the player must log in with NavAuth credentials. Cracked: always `true`
  (`User.nonPremium` sets it, and `UserMapper` reads every cracked row as `true` whatever the column
  holds). Premium: `true` only while the account has a 2FA secret.

`UserCredentials(userUuid, hashedPassword, totpSecret)`:

- At least one of `hashedPassword` (`PasswordHash` + `HashingAlgorithm`) and `totpSecret` is set
  (constructor `require`): `withoutPassword()` needs a secret, `withoutTotpSecret()` throws without
  a password. `TOTPSecret` has at least 16 characters.
- `storeUserCredentials` requires the user's `credentialsRequired` and a matching uuid;
  `deleteUserCredentials` requires `!credentialsRequired`. Set the flag before storing
  credentials; clear it before deleting them. `storeNewUserCredentials` (the first credentials of an
  account inserted in the same transaction) has the same requirements and replaces a credentials
  row that belongs to no user.

## How the UUID is derived

| Path | `uuid` | `mojangUuid` | Where |
| --- | --- | --- | --- |
| New premium player | Mojang UUID authenticated by the session server | same | `LoginListeners.createAndStorePremiumUser` (PostLogin, online mode only) |
| New cracked player | Velocity's offline UUID of the name as typed, `UUID.nameUUIDFromBytes("OfflinePlayer:<name>")`: no account exists, so the connection keeps its own profile | `null` | `RegisterCommand`: `User.nonPremium(UserUuid(sender.uniqueId), …)` |
| Premium/cracked switch, rename | unchanged | set / cleared / unchanged | `UserService` |
| LibreLogin import | LibreLogin `uuid` (offline, random or Mojang, depending on LibreLogin's UUID creator) | `premium_uuid` | [migrators.md](migrators.md) |
| `NAVAUTH` database copy | unchanged | unchanged | [migrators.md](migrators.md) |

So a cracked account that went premium keeps its offline UUID, and a premium account forced to
cracked keeps its Mojang UUID as `uuid`.

## UserService operations

Changes that write more than one row run in one `TransactionService.inTransaction`; events are
posted after it commits. Account creation only inserts (`UserRepository.insert` and
`UserCredentialsRepository.insert` fail on an existing uuid and never change a row); `save` is an
upsert and serves changes to existing accounts.

| Method | What it does | Called by |
| --- | --- | --- |
| `createAndStoreUserWithNewCredentials(user, password)` | one transaction: `UsernameAlreadyTakenException` when an account has the uuid or the name in any letter case; otherwise inserts the cracked user and its password row (a credentials row under the new uuid that belongs to no user is replaced) | `/register` |
| `storePremiumUser(user)` | inserts the premium user row only (`require(isPremium)`); fails when the uuid exists | PostLogin, new premium player |
| `migrateToPremium(user, mojangId)` | sets `mojangUuid`; drops the password; keeps a 2FA secret (`credentialsRequired = true`), otherwise deletes the credentials row; `UserPremiumMigrationEvent` | `/premium`, `/forcepremium` |
| `migrateToNonPremium(user, newPassword)` | `require(isPremium)`; clears `mojangUuid`, `credentialsRequired = true`, sets the password, keeps a 2FA secret; `UserNonPremiumMigrationEvent` | `/forcecracked` |
| `migrateUsername(user, newName)` | rename keeping `uuid`; same exact name → `IllegalArgumentException`; a letter-case change is allowed; name held by another uuid in any case → `UsernameAlreadyTakenException`; `UserUsernameMigrationEvent` | PreLogin username resolution ([login-flow.md](login-flow.md#username-resolution)) |
| `migrateData(user, newName)` | the same rename for cracked accounts only; refuses a name the profile API knows as premium; posts no event | `/migrateuser` |
| `enableTwoFactorAuth(user, secret)` | creates credentials if missing, sets `credentialsRequired`, stores the secret | `/verify2fa` |
| `disableTwoFactorAuth(user)` | with a password: removes the secret; without a password (`check(isPremium)`, premium accounts only): deletes the credentials row, clears `credentialsRequired` | `/disable2fa` |
| `removeLeftoverPremiumPassword(user)` | premium account still holding a password (old imports): drops it, keeps 2FA, repairs `credentialsRequired` when no row exists; logs the uuid | PostLogin, only after Mojang verified the owner |

## Commands

`<name|uuid>` is resolved by `UserArgumentResolver`: a string matching the username pattern is
looked up as a name (any case), anything else is parsed as a UUID (32 hex digits or dashed) and
looked up as the account `uuid`, never as the Mojang UUID. Permissions: [commands-and-permissions.md](commands-and-permissions.md).

| Command | Precondition | Calls |
| --- | --- | --- |
| `/register <password> <repeat>` | REGISTER session | `PasswordValidator`, repeat check → `enqueueHashPassword` → `createAndStoreUserWithNewCredentials` → `session.authenticate()`; a failed creation sends `unexpectedErrorOccurred` and the session stays unauthenticated |
| `/login <password> [code]`, `/2fa <code>` | LOGIN session | `LoginAuthSession.enqueueAuthTask`: 2FA code checked first, then the password on the queue; the code is used up only when the whole login succeeds ([login-flow.md](login-flow.md)) |
| `/changepassword <current> <new>` | cracked, has a password | `enqueueVerifyPassword` → `updatePassword` |
| `/premium` | cracked | `fetchProfileInfo(own name)` (`ProfileApiFetchException` → `unexpectedErrorOccurred`) → refused with `mojangAccountAlreadyLinkedError` when another account has that Mojang UUID → `migrateToPremium` |
| `/setup2fa [password]` (`enable2fa`) | 2FA not enabled | with a password on the account: verify it on the queue first; without one: starts directly, only on a connection Mojang authenticated (`isOnlineMode`), else `cantUseThisCommandNowError`. Then a new secret in a setup session, shown in chat |
| `/generate2faqr` | open setup session | QR of the `otpauth://` URL |
| `/verify2fa <code>` (`complete2fa`) | open setup session | `verifyTOTP(session secret)` → `enableTwoFactorAuth`, session closed |
| `/disable2fa <code>` | 2FA enabled; a non-premium account without a password is refused (`twoFactorDisablePasswordRequiredError`) | `verifyCode` (check and use up) → `disableTwoFactorAuth` |
| `/forcepremium <name\|uuid>` | cracked target | `fetchProfileInfo(stored name)` (`ProfileApiFetchException` → `unexpectedErrorOccurred`) → refused with `adminCmdMojangAccountAlreadyLinkedError` when another account has that Mojang UUID → `migrateToPremium` |
| `/forcecracked <name\|uuid> [password]` | premium target | 8 random characters (`StringUtils.generateRandomString`, `SecureRandom`) when no password is given → `enqueueHashPassword` (queue key: target uuid) → `migrateToNonPremium`; the console sees the password, a player gets `adminCopyPasswordText` |
| `/forcesetpassword <name\|uuid> <password>` | cracked target | `updatePassword` (2FA kept; creates the credentials row when the account has none) |
| `/migrateuser <name\|uuid> <newName>` | run by a player (not the console), cracked target | `UsernameValidator`, profile lookup must find nothing (`ProfileApiFetchException` → `unexpectedErrorOccurred`) → `migrateData` → kicks the online player |
| `/lookup profile <name\|uuid>` | — | read only |

`/unregister` and `/forceunregister` are commented out in their files and not registered.

## Hashing

| `HashingAlgorithm` | Hasher | Stored `passwordHash` | Written for |
| --- | --- | --- | --- |
| `BCRYPT` | `BCryptCredentialsHasher` (favre bcrypt, cost 10) | `$2a$10$<salt><hash>` | new passwords (default), LibreLogin `BCrypt-*` |
| `ARGON2` | `Argon2CredentialsHasher` (BouncyCastle Argon2id, 16-byte salt) | `$argon2id$v=19$m=<kb>,t=<it>,p=<par>$<salt>$<hash>`, Base64 without padding | new passwords when configured, LibreLogin `Argon-2ID` |
| `SHA256`, `SHA512` | `SHACredentialsHasher` (PBKDF2-HMAC, 100 000 iterations) | `pbkdf2-sha256$<salt>$<hash>` / `pbkdf2-sha512$…`, Base64 | LibreLogin `LOGIT-SHA-*` imports only |
| `LIBRELOGIN_SHA256`, `LIBRELOGIN_SHA512` | `LibreLoginSHACredentialsHasher`: hex(sha(hex(sha(password)) + salt)) | `SHA256$<saltHex>$<hashHex>` | LibreLogin `SHA-*` imports only |

- `createHasher(algo)` picks the verifier from the stored algorithm. `createDefaultHasher()` uses
  `passwordsConfig.hashingAlgorithm` (default `BCRYPT`) and accepts only `BCRYPT` and `ARGON2`.
- Argon2 settings (`passwordsConfig.argon2`): `hashLength` 16, `memoryKb` 65536 (64 MiB per running
  hash), `iterations` 3, `parallelism` 4. They apply to new hashes only; verification reads the
  parameters from the stored string, so changing them never breaks stored hashes.
- Nothing rehashes today: a hash keeps its algorithm until the password changes (`/changepassword`,
  `/forcesetpassword`, `/forcecracked`).

## Encryption queue

BCrypt and Argon2 cost CPU and memory (Argon2: `memoryKb` per hash); unbounded parallel hashing in
a join wave would exhaust the proxy, and hashing on Velocity threads would stall it. Every hash and
verification goes through `UserCredentialsService.enqueue*` into `EncryptionQueueServiceImpl`:

- `ThreadPoolExecutor(encryptionQueueMinThreads = 2, encryptionQueueMaxThreads = 4, 60 s)` with an
  unbounded `LinkedBlockingQueue`, default thread factory, core threads time out when idle.
- **One task per player**, keyed by `player.uniqueId` (the account uuid; admin commands use the
  target's uuid), claimed with `putIfAbsent`. A second `submitTask` throws
  `EncryptionTaskAlreadyQueuedException`; commands answer `processAlreadyInProgressError` /
  `alreadyTryingToLoginError`.
- **One authentication attempt per login session** (`LoginAuthSession.enqueueAuthTask`): a second
  attempt while one is being verified throws `EncryptionTaskAlreadyQueuedException`. The attempts
  left are an atomic counter checked before verifying; `maxLoginAttempts` below 1 counts as 1; the
  too-many-attempts hook fires once. A closed (`isInvalidated`) session verifies nothing
  (`FailedTooManyAttempts`); an authenticated one returns `Success` without verifying.
- The operation calls `finishTask` (frees the player's slot) **before** completing its future, so
  a continuation may enqueue the next task for the same player (`/changepassword`: verify, then
  hash).
- **Dequeue on disconnect** (`ConnectionListeners.onDisconnect` → `dequeueTask`): a waiting task is
  removed and its future fails with `CancellationException`; a running task is only marked
  cancelled and still completes.
- `updatePassword` hashes on the queue, then writes on the `db` executor
  (`common/module/ExecutorsModule.kt`: `navauth-db-N`, 0–2 threads, `SynchronousQueue`).

## TOTP 2FA

- `TOTP2FA`: HMAC-SHA1, 6 digits, 30 s steps; secrets are 20 `SecureRandom` bytes in Base32
  (32 characters). `TOTP2FA.findMatchingStep` checks the previous, current and next step
  (`window = 1`), every step each time, comparing with `MessageDigest.isEqual`; `verifyTOTP` is
  `findMatchingStep != null`.
- **Each code is accepted once per account.** `UserCredentialsService` remembers the last used time
  step per account in memory (per proxy, lost on restart); a code of that step or an earlier one is
  refused. `findUnusedCodeStep` checks without using the code up and `useCodeStep` uses it up.
  `/login` and `/2fa` use it up only when the whole login succeeds (the password too);
  `/disable2fa` calls `verifyCode` (check and use up).
- Setup: `/setup2fa` verifies the password when the account has one, then `TotpSetupSessionService`
  keeps the new secret in memory (Guava cache by account uuid, 300 s after write, not cleared on
  disconnect; a new `/setup2fa` replaces it). `/verify2fa` checks a code directly against the setup
  secret (`verifyTOTP`) and stores the secret.
- QR: `otpauth://totp/<username>?secret=<secret>&issuer=<issuer>` (not URL-encoded), ZXing,
  error correction L, drawn with Unicode block characters in chat. An `issuer` (default `NAVAUTH`)
  longer than 15 characters makes the QR too big for chat.

## Validation

- `UsernameValidator`: full match of `usernamesConfig.usernamePattern` (default
  `^[a-zA-Z0-9_]{3,16}$`); an invalid regex logs a warning and falls back to the default. Used at
  PreLogin, by `UserArgumentResolver` (name vs. UUID), `/migrateuser` and migrators. A wider
  pattern also changes what reaches the profile API ([mojang-profiles.md](mojang-profiles.md#traps)).
- `PasswordValidator` (`passwordsConfig`): `minLength` 5, `requireLowercase` true, `requireDigit`
  true, `requireUppercase` false, `requireSpecial` false (special = not a letter or digit);
  whitespace is always rejected; no maximum length. Only `/register` calls it.

## Rules

- **Never change how a user's UUID is derived** (table above): backends key player data by it.
  Switches, renames and imports never change `uuid`.
- **A premium account belongs to its Mojang UUID, not to its name.** Names are released and
  reclaimed on Mojang; only the Mojang-verified UUID may receive a premium account's identity
  ([login-flow.md](login-flow.md)).
- **NavAuth never leaves a password on a premium account**, only a 2FA secret. Every path that
  makes an account premium drops the password (`migrateToPremium`, imports,
  `removeLeftoverPremiumPassword`).
- **Every password hash ever written keeps verifying.** Legacy hashers (LibreLogin SHA, PBKDF2
  SHA, BCrypt, Argon2), their stored formats and their `HashingAlgorithm` names stay until the
  owner removes them.
- **Moving users to another algorithm happens only by rehashing on a successful login**, with the
  password just verified; never convert stored hashes in bulk.
- **Hashing never runs on Velocity network threads** and never inside a transaction: use the
  encryption queue.
- Account changes go through `UserService` (transaction, flag/credentials order, events). Only
  migrators write rows through the repositories directly.
- **Account creation only inserts**: it never changes an existing row, and registration re-checks
  inside its transaction that no account has the uuid or the name (any letter case).
- Never log passwords, hashes, salts or TOTP secrets; log the uuid.

## Traps

- `/changepassword`, `/forcesetpassword` and `/forcecracked` do not call `PasswordValidator`.
- `encryptionQueueMaxThreads` has no effect: with an unbounded queue a `ThreadPoolExecutor` never
  grows past its core size, so at most `encryptionQueueMinThreads` hashes run at once.
- The `db` executor has no queue and the default abort policy: a third concurrent `updatePassword`
  is rejected after hashing, and the change fails with `unexpectedErrorOccurred`.
- Continuations without an executor run on the queue thread: `/forcecracked` switches the account
  there. An exception in that continuation is not reported. `/register` reports one
  (`unexpectedErrorOccurred`).
- `createDefaultHasher()` throws for any configured algorithm other than `BCRYPT`/`ARGON2`, at hash
  time (registration, password changes); nothing checks the value at startup.

## Related

[login-flow.md](login-flow.md) · [persistence.md](persistence.md) · [mojang-profiles.md](mojang-profiles.md) ·
[migrators.md](migrators.md) · [commands-and-permissions.md](commands-and-permissions.md) ·
[configs-and-messages.md](configs-and-messages.md) · [api-and-events.md](api-and-events.md) ·
[security.md](security.md) · [../TESTING.md](../TESTING.md) (sections S and P)
