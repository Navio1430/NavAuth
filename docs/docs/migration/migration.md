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