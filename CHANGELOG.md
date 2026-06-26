
## 0.2.0

### Changes
```diff
+ Encryption queue (backpressure especially for Argon2 and BCrypt hashing methods which can lead to OOM killer)
+ Argon2 configuration settings
+ Mojang profile API order (API fallback)
+ MineTools profile API support (preferred one by default)
```

### Fixes
- fix register command always using BCrypt instead of the selected algo

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
```
