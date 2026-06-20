
## 0.2.0

### Changes
```diff
+ Encryption queue (backpressure especially for Argon2 and BCrypt hashing methods which can lead to OOM killer)
```

### Fixes
- fix register command always using BCrypt instead of the selected algo

### Config
**General** config:
```diff
+ encryptionQueueConfig
```

**Messages** config:
```diff
+ unexpectedErrorOccurred
+ processAlreadyInProgressError
+ alreadyTryingToLoginError
```
