
## 0.2.1

### Changes
```diff
- After successful login/register, the player is now sent directly to the initial
  server via VelocityServerConnectService instead of firing a faked PlayerChooseInitialServerEvent
- Priority of PlayerChooseInitialServerEvent and ServerPreConnectEvent listeners set to always
  have the final say (MIN)
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
