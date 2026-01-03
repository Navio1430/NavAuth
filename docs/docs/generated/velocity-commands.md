| Command name            | Permission                     | Description                                                                                                                                                                                                                |
| ----------------------- | ------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `/login`                | available by default           | Manual login command for non-premium players                                                                                                                                                                               |
| `/register`             | available by default           | Creates a new account with specified password. Applicable only for non-premium players. If you want to disable this command for specific group, then set  their '**navauth.user.register**' permission value to **FALSE**. |
| `/unregister`           | navauth.user.unregister        | Unregister your account. **NOTE**: someone will be able to acquire and register the account after.                                                                                                                         |
| `/changepassword`       | navauth.user.changepassword    | Change your account password                                                                                                                                                                                               |
| `/premium`              | navauth.user.premium           | Change your account to premium account. Applicable for non-premium players. Enables auto-login and migrates to premium.                                                                                                    |
| `/forceunregister`      | navauth.admin.forceunregister  | Force unregister specified user. Works like unregister command, but doesn't require password.                                                                                                                              |
| `/forcesetpassword`     | navauth.admin.forcesetpassword | Force set password for specified user. Works like register command, but doesn't require password.                                                                                                                          |
| `/forcecracked`         | navauth.admin.forcecracked     | No description                                                                                                                                                                                                             |
| `/forcepremium`         | navauth.admin.forcepremium     | No description                                                                                                                                                                                                             |
| `/navauth user migrate` | navauth.admin.migrateuserdata  |                                                                                                                                                                                                                            |
| `/migration start`      | navauth.root                   |                                                                                                                                                                                                                            |
## /login
Manual login command for non-premium players

**PERM**: available by default
## /register
Creates a new account with specified password. Applicable only for non-premium players. If you want to disable this command for specific group, then set  their '**navauth.user.register**' permission value to **FALSE**.

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
