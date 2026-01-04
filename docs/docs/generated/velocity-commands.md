| Command name        | Permission                     | Description                                                                                                                                                                                                                                                                                     |
| ------------------- | ------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `/login`            | available by default           | Logs in into account using password (and 2FA code if set). Password parameter is always required (check `/2fa` command which is responsible for 2FA code only). If you want to disable this command for specific group, then set  their `navauth.user.login` permission value to **FALSE**.     |
| `/2fa`              | available by default           | Logs in into account using 2FA code. Works only if user account has TOTP2FA code set as the only one required to authenticate. If you want to disable this command for specific group, then set  their `navauth.user.login` permission value to **FALSE**.                                      |
| `/register`         | available by default           | Creates a new account with specified password. Applicable only for non-premium players. If you want to disable this command for specific group, then set  their `navauth.user.register` permission value to **FALSE**.                                                                          |
| `/unregister`       | navauth.user.unregister        | Unregisters user account. This command **will not delete user data**. **NOTE**: someone will be able to acquire and register the account after.                                                                                                                                                 |
| `/changepassword`   | navauth.user.changepassword    | Changes account password to new one. Requires current password.                                                                                                                                                                                                                                 |
| `/premium`          | navauth.user.premium           | Migrates account mode to premium account. Applicable for non-premium players only. Enables auto-login and migrates to premium. This command will remove bound **password** and will leave **2FA** secret if enabled.                                                                            |
| `/forceunregister`  | navauth.admin.forceunregister  | Force unregister specified user. Works like unregister command, but doesn't require password.                                                                                                                                                                                                   |
| `/forcesetpassword` | navauth.admin.forcesetpassword | Force set password for specified user. Works like register command, but doesn't require password.                                                                                                                                                                                               |
| `/forcecracked`     | navauth.admin.forcecracked     | Forces a premium user account into non-premium (cracked) mode. Generates or assigns a new password and updates the user’s authentication data accordingly.                                                                                                                                      |
| `/forcepremium`     | navauth.admin.forcepremium     | Does the same thing as `/premium` on specified user, buy forcefully used as admin                                                                                                                                                                                                               |
| `/migrateuser`      | navauth.admin.migrateuserdata  | Migrates user data from an existing cracked account to a new username. The command validates usernames, checks for conflicts or premium accounts, and safely transfers all stored data to the specified new account. If you want to migrate premium user, then use /forcecracked command first. |
| `/migration start`  | navauth.root                   | Starts the account data migration process based on provided migration config. Ensures only console or RCON can run it, prevents parallel executions, and reports whether the migration finished successfully or failed.                                                                         |
## /login
Logs in into account using password (and 2FA code if set). Password parameter is always required (check `/2fa` command which is responsible for 2FA code only). If you want to disable this command for specific group, then set  their `navauth.user.login` permission value to **FALSE**.

**PERM**: available by default
## /2fa
Logs in into account using 2FA code. Works only if user account has TOTP2FA code set as the only one required to authenticate. If you want to disable this command for specific group, then set  their `navauth.user.login` permission value to **FALSE**.

**PERM**: available by default
## /register
Creates a new account with specified password. Applicable only for non-premium players. If you want to disable this command for specific group, then set  their `navauth.user.register` permission value to **FALSE**.

**PERM**: available by default
## /unregister
Unregisters user account. This command **will not delete user data**. **NOTE**: someone will be able to acquire and register the account after.

**PERM**: navauth.user.unregister
## /changepassword
Changes account password to new one. Requires current password.

**PERM**: navauth.user.changepassword
## /premium
Migrates account mode to premium account. Applicable for non-premium players only. Enables auto-login and migrates to premium. This command will remove bound **password** and will leave **2FA** secret if enabled.

**PERM**: navauth.user.premium
## /forceunregister
Force unregister specified user. Works like unregister command, but doesn't require password.

**PERM**: navauth.admin.forceunregister
## /forcesetpassword
Force set password for specified user. Works like register command, but doesn't require password.

**PERM**: navauth.admin.forcesetpassword
## /forcecracked
Forces a premium user account into non-premium (cracked) mode. Generates or assigns a new password and updates the user’s authentication data accordingly.

**PERM**: navauth.admin.forcecracked
## /forcepremium
Does the same thing as `/premium` on specified user, buy forcefully used as admin

**PERM**: navauth.admin.forcepremium
## /migrateuser
Migrates user data from an existing cracked account to a new username. The command validates usernames, checks for conflicts or premium accounts, and safely transfers all stored data to the specified new account. If you want to migrate premium user, then use /forcecracked command first.

**PERM**: navauth.admin.migrateuserdata
## /migration start
Starts the account data migration process based on provided migration config. Ensures only console or RCON can run it, prevents parallel executions, and reports whether the migration finished successfully or failed.

**PERM**: navauth.root
