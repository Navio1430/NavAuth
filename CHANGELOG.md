
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

### Config
**Messages** config:
```diff
+ profileApiFailureKickMessage
```

**General** config:
```diff
+ commandsConfig
```


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
