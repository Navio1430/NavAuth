| Command name     | Permission                     | Description                                                                                                             |
| ---------------- | ------------------------------ | ----------------------------------------------------------------------------------------------------------------------- |
| login            | navauth.user.login             | Manual login command for non-premium players                                                                            |
| register         | navauth.user.register          | Create a new account with a password
Applicable for non-premium players                                                 |
| unregister       | navauth.user.unregister        | Unregister your account
NOTE: someone will be able to acquire the account                                               |
| changepassword   | navauth.user.changepassword    | Change your account password                                                                                            |
| premium          | navauth.user.premium           | Change your account to premium account.
Applicable for non-premium players.
Enables auto-login and migrates to premium. |
| forceunregister  | navauth.admin.forceunregister  | Force unregister specified user. Works like unregister command, but doesn't require password.                           |
| forcesetpassword | navauth.admin.forcesetpassword | Force set password for specified user. Works like register command, but doesn't require password.                       |
## /login
Manual login command for non-premium players

**PERM**: navauth.user.login
## /register
Create a new account with a password
Applicable for non-premium players

**PERM**: navauth.user.register
## /unregister
Unregister your account
NOTE: someone will be able to acquire the account

**PERM**: navauth.user.unregister
## /changepassword
Change your account password

**PERM**: navauth.user.changepassword
## /premium
Change your account to premium account.
Applicable for non-premium players.
Enables auto-login and migrates to premium.

**PERM**: navauth.user.premium
## /forceunregister
Force unregister specified user. Works like unregister command, but doesn't require password.

**PERM**: navauth.admin.forceunregister
## /forcesetpassword
Force set password for specified user. Works like register command, but doesn't require password.

**PERM**: navauth.admin.forcesetpassword
