# Authentication Strategy

NavAuth uses a specific strategy for defining the authentication process.
On this page, we’ll look into the different scenarios a user can go through.

## User tries to log in with a premium nickname
If the user has a premium nickname (case-insensitive) but doesn’t appear in the database,
they must use exactly the same nickname as found premium Mojang account.

We use the official Mojang API to query user profile data.
Profile data is stored in cache to prevent request limits.