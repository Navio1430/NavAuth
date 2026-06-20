
## 0.2.0

### Fixes
- fix register command always using BCrypt instead of the selected algo

### Config
**General** config:
```diff
+encryptionQueueConfig
```

**Messages** config:
```diff
+ unexpectedErrorOccurred
+ processAlreadyInProgressError
+ alreadyTryingToLoginError
```
