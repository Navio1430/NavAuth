# Migrate database type

If you've encountered any problems with your current database type, or you want to scale up,
then you can migrate your database type.

## How to migrate the database type?

It works the same as [migrating from other plugins](./migration.html), but you need to change the migrated plugin type to `NAVAUTH`.
Your `migration.yml` config has the current database and your `general.yml` config would have the new database config.