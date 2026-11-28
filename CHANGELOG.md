
## 0.4.0

### Changes
```diff
+ Storage rewritten from OrmLite to JetBrains Exposed; the database schema is managed by Flyway
  migrations (history table `navauth_flyway_schema_history`)
+ The database is connected and migrated before anything else starts; a failed migration stops
  the proxy instead of running on a half-migrated schema
+ Premium accounts are bound to their Mojang account. When a premium name changes owner on Mojang,
  the new owner is refused until the previous owner joins once with their new name
+ User lookups tolerate duplicate rows in navauth_users: the row with the lowest uuid is used and a
  warning names the duplicates
+ A password left on a premium account (LibreLogin imports of earlier versions) is removed the next
  time its owner joins with the premium account; 2FA stays
+ LibreLogin migration skips the rows it cannot import (missing or invalid nickname or uuid,
  unknown or damaged password hash, account already in NavAuth), logs each with the reason and
  ends with a summary of imported and skipped rows
- Player session log (join/leave times and IP addresses), the /lookup sessions command and the
  "Last IP" line of /lookup profile
+ Every bundled library is relocated under pl.spcode.navauth.lib, so other plugins' copies of
  Flyway, Jackson or kotlinx-coroutines cannot clash with NavAuth's. The SQLite driver keeps its
  package: its native code looks its classes up by their original names
```

### Fixes
- **Security:** fixed several issues in the login flow and in account handling that could let a
  player into another player's account or past the login checks. Every server should update.
- `/setup2fa` did nothing for players without a password (premium players); it starts the 2FA
  setup now
- `/disable2fa` could lock out a non-premium account whose only credential was 2FA; it now asks for
  a password to be set first, and `/forcesetpassword` can set a password for a non-premium account
  that has no credentials (repairs accounts locked out this way)
- `/premium`, `/forcepremium` and `/migrateuser` showed nothing when the Mojang profile lookup
  failed; they show an error now
- Error answers of the profile APIs (rate limits, server errors) no longer count as "player not
  found"; when MineTools fails, Mojang's own answer decides
- API: event listeners are called in their `@Subscribe(priority)` order, listeners with the same
  priority in registration order
- Database statements are no longer written to the debug log
- Players connecting over IPv6 could be kicked during login
- LibreLogin migration: premium players who had a password stored in LibreLogin are no longer asked
  to `/login`; like after `/premium`, they keep only their 2FA secret (#46)
- MySQL/MariaDB: looking a player up by their exact name (`/premium`, `/changepassword`) ignored
  letter case because of the database collation; it is exact on every database now
- Players who were not logged in could not use the aliases of the login and register commands
  (`/l`, `/reg`), nor names or aliases changed in commands.yml
- LibreLogin migration: one row with an invalid nickname or an unknown hash algorithm stopped the
  whole migration; an imported row could overwrite the NavAuth account with the same uuid
- A startup failure caused by an error rather than an exception (for example a missing class) left
  the proxy running without NavAuth's login checks; every startup failure now stops the proxy

### Config
**Messages** config:
```diff
+ premiumUsernameOwnedByAnotherAccountError
+ unexpectedLoginErrorKickMessage
+ usernameUnavailableKickMessage
+ mojangAccountAlreadyLinkedError
+ adminCmdMojangAccountAlreadyLinkedError
+ twoFactorDisablePasswordRequiredError
```

**General** config:
```diff
- sessionsConfig (no longer read; you can delete the section)
```

### Permissions
```diff
- navauth.admin.playerlookup.sessions
```

### API
```diff
+ EventPriority constants (LOWEST, LOW, NORMAL, HIGH, HIGHEST) are public; values are unchanged
```

### Database
- New installs: Flyway creates `navauth_users` and `navauth_credentials`. `navauth_users_sessions`
  is no longer created.
- Existing installs: on the first start the current schema is recorded as version 1 in
  `navauth_flyway_schema_history`. No existing table or row is changed.
- `navauth_users_sessions` **stays in your database with all its data**, but NavAuth no longer reads
  or writes it. It holds players' IP addresses, which are personal data (e.g. under GDPR). If you
  do not need them, back the table up and remove it yourself:
  `DROP TABLE navauth_users_sessions;`
- Rollback: possible. 0.3.0 runs on the same database (it ignores the Flyway history table).

### Upgrade notes
- Replace the jar and restart; nothing else is required.
- Players denied with `premiumUsernameOwnedByAnotherAccountError` use a premium name that still
  belongs to another stored account (its owner renamed on Mojang). The name is freed when that
  owner joins once with their new name, or when an administrator unregisters or renames the old
  account.
- If you imported from LibreLogin with an earlier version, premium players asked for a password are
  fixed automatically the next time they join with their premium account.
- Some names that could register before are refused now with `usernameUnavailableKickMessage`; the
  player joins with another name.
- `/premium` and `/forcepremium` refuse a Mojang account that is already linked to another NavAuth
  account (`mojangAccountAlreadyLinkedError`, `adminCmdMojangAccountAlreadyLinkedError`).
- Remove `sessionsConfig` from `general.yml` and the `navauth.admin.playerlookup.sessions`
  permission from your permission plugin when convenient; leftovers do no harm.

### New contributors
**Thanks!**
- Pawelusze - responsibly reported security issues and helped fix them

## 0.3.0

### Changes
```diff
+ Update to Java 25
+ Update to Velocity 4.1.2

+ Added commands configuration: [name, aliases, enabled] per command

+ Mojang profile lookup now also caches non-premium (not found) usernames, so repeated login
  attempts against nicknames that have no Mojang account no longer hit the profile API every time
+ Only username resolution during login/register reads this not-found cache (avoids rate limits /
  latency on login spam and brute-force attempts)
+ Authoritative lookups (e.g. /premium, admin commands, username migration) keep using the live
  API and ignore the not-found cache, so they always reflect the current Mojang state
+ After successful login/register, the player is now sent directly to the initial
  server via VelocityServerConnectService instead of firing a faked PlayerChooseInitialServerEvent
+ Priority of PlayerChooseInitialServerEvent and ServerPreConnectEvent listeners set to always
  have the final say (MIN)
- No more fake PlayerChooseInitialServerEvent dispatch
```

### Tradeoffs
- A not-found cache entry is stored for the configured `profileCacheTTL`.
  If a player becomes premium (buys the game / renames to that nickname) within that window,
  username resolution may briefly still treat the name as non-premium until the cache expires.

### Fixes
- fix unexpectedErrorOccurred message: changed val to var (this was causing warning messages and no changes after /reload)
- /changepassword now notifies premium accounts that they can't change their password
- fix accountMigrationSuccess multification: not being sent to the player

### Config
**Messages** config:
```diff
+ profileApiFailureKickMessage
+ accountNotNonPremiumError
```

**General** config:
```diff
+ commandsConfig
```

### New contributors
**Thanks!**
- WejsoneKK - implemented command aliases
- ThomasWega - bug hunting

## 0.2.0

### Changes
```diff
+ Encryption queue (backpressure especially for Argon2 and BCrypt hashing methods which can lead to OOM killer)
+ Argon2 configuration settings
+ Mojang profile API order (API fallback)
+ MineTools profile API support (preferred one by default)
+ NavAuthAPI.isAuthenticated(UUID) method for checking player auth state
+ AjQueue integration plugin
```

### Fixes
- fix register command always using BCrypt instead of the selected algo
- fix different letter case same username migration

### API
```diff
+ NavAuthAPI#isAuthenticated(UUID)
```

### Config
**General** config:
```diff
+ encryptionQueueConfig
+ mojangAPIConfig
+ Argon2 settings
```

**Messages** config:
```diff
+ unexpectedErrorOccurred
+ processAlreadyInProgressError
+ alreadyTryingToLoginError
+ registeringInfo
+ loggingInInfo
```

### New integration for AjQueue (as a separate plugin)
- Automatically adds authenticated players to a configurable AjQueue queue
- Optionally cancel AjQueue `PreQueueEvent` for unauthenticated players

### Docs
- AjQueue integration info
