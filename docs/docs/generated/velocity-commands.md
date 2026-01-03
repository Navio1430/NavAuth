| Command name            | Permission                     | Description                                                                                                                                                                                                                                                                                 |
| ----------------------- | ------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `/login`                | available by default           | Logs in into account using password (and 2FA code if set). Password parameter is always required (check `/2fa` command which is responsible for 2FA code only). If you want to disable this command for specific group, then set  their `navauth.user.login` permission value to **FALSE**. |
| `/2fa`                  | available by default           | Logs in into account using 2FA code. Works only if user account has TOTP2FA code set as the only one required to authenticate. If you want to disable this command for specific group, then set  their `navauth.user.login` permission value to **FALSE**.                                  |
| `/register`             | available by default           | Creates a new account with specified password. Applicable only for non-premium players. If you want to disable this command for specific group, then set  their `navauth.user.register` permission value to **FALSE**.                                                                      |
| `/unregister`           | navauth.user.unregister        | Unregister your account. **NOTE**: someone will be able to acquire and register the account after.                                                                                                                                                                                          |
| `/changepassword`       | navauth.user.changepassword    | Change your account password                                                                                                                                                                                                                                                                |
| `/premium`              | navauth.user.premium           | Change your account to premium account. Applicable for non-premium players. Enables auto-login and migrates to premium.                                                                                                                                                                     |
| `/forceunregister`      | navauth.admin.forceunregister  | Force unregister specified user. Works like unregister command, but doesn't require password.                                                                                                                                                                                               |
| `/forcesetpassword`     | navauth.admin.forcesetpassword | Force set password for specified user. Works like register command, but doesn't require password.                                                                                                                                                                                           |
| `/forcecracked`         | navauth.admin.forcecracked     | No description                                                                                                                                                                                                                                                                              |
| `/forcepremium`         | navauth.admin.forcepremium     | No description                                                                                                                                                                                                                                                                              |
| `/navauth user migrate` | navauth.admin.migrateuserdata  |                                                                                                                                                                                                                                                                                             |
| `/migration start`      | navauth.root                   |                                                                                                                                                                                                                                                                                             |
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
Unregister your account. **NOTE**: someone will be able to acquire and register the account after.

**PERM**: navauth.user.unregister
## /changepassword
Change your account password

**PERM**: navauth.user.changepassword
## /premium
Change your account to premium account. Applicable for non-premium players. Enables auto-login and migrates to premium.

**PERM**: navauth.user.premium
## /forceunregister
Force unregister specified user. Works like unregister command, but doesn't require password.

**PERM**: navauth.admin.forceunregister
## /forcesetpassword
Force set password for specified user. Works like register command, but doesn't require password.

**PERM**: navauth.admin.forcesetpassword
## /forcecracked
No description

**PERM**: navauth.admin.forcecracked
## /forcepremium
No description

**PERM**: navauth.admin.forcepremium
## /navauth user migrate


**PERM**: navauth.admin.migrateuserdata
## /migration start


**PERM**: navauth.root
