# Migrate from other plugins

## Supported plugins

<!--@include: ../generated/migrated-plugin-types.md-->

### Don’t see your plugin?
Migration support is added based on demand. Join our Discord server to request an implementation for your plugin.

## How to migrate?

> [!CAUTION]
> DO NOT LET PLAYERS ON THE SERVER BEFORE SUCCESSFUL MIGRATION!
> IT CAN CAUSE UNWANTED ISSUES.

1. Configure the `migration.yml` config. It should point to the source database.
Your `general.yml` config should have the target and actual database that you want to use.
2. Make sure to have a backup of your database in case something goes wrong.
3. Open up the console and execute the migration command [/migration start](/general/commands.html#migration-start).
4. Wait for the migration to finish. Depending on the size of your database, this may take a while.

When you migrate from another plugin, rows that cannot be imported do not stop the migration. A
row is skipped when its name is already taken in NavAuth (in any letter case), its uuid or Mojang
UUID already belongs to a NavAuth account, its nickname or uuid is missing or invalid, or its
password is in an unknown or damaged format. Each skipped row is logged with its name, uuid and the
reason (never password data), and the summary at the end counts the imported and skipped rows.

## LibreLogin: premium players asked for a password

LibreLogin keeps the password of a player who switched to premium. NavAuth versions before 0.4.0
imported that password, so those premium players are asked to `/login` instead of being logged in
automatically. NavAuth itself never leaves a password on a premium account, and current versions
import premium players without it.

If you imported from LibreLogin with an earlier version, nothing needs to be done: the first
time such a player joins with their premium account (verified by Mojang), NavAuth removes the
leftover password and logs them in automatically. A player with 2FA keeps the 2FA secret and is
asked only for the code. Cracked (non-premium) accounts are never touched.
