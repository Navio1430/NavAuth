# Rewriting the data migrators without OrmLite

Two migrators currently depend on OrmLite:

- `migrate/migrator/navauth/NavAuthDatabaseMigrator.kt` — copies NavAuth data between two
  databases; today it pairs source/target `Dao`s generically.
- `migrate/migrator/librelogin/LibreLoginMigrator.kt` — reads a LibreLogin `librepremium_data`
  table via a `Dao<LibreLoginUser, UUID>` and converts hashes.

Both must use Exposed instead. The target database is the app database; the source is configured
by `MigrationConfig.sourceDatabaseConfig`.

## Two Exposed databases, no shared transaction

`NavAuthDatabaseMigrator` currently builds a second `DatabaseManager` for the source. After the
migration you connect two Exposed `Database`s:

```kotlin
val sourceDb = Database.connect(sourceHikariDataSource)
val targetDb = databaseManager.database          // the app database from connect()

transaction(sourceDb) {   // read a chunk from source
  ...
}
transaction(targetDb) {   // write the chunk to target
  ...
}
```

There is **no cross-database transaction**. Do not attempt to wrap both in one `transaction`
block. Make the copy idempotent (upsert by primary key) and page in `chunkSize` chunks, so a
failure can be retried from the beginning without duplicating rows.

## `NavAuthDatabaseMigrator` — explicit tables instead of DAO pairs

The generic `daoSourceTargetPairs` + `entitiesRegistrar` loop cannot survive: the DSL is not
reflection-driven and the sessions table is PK-less. Replace it with an explicit list. Keep it
readable; a `when`/list of small copy functions is preferred over a new generic abstraction.

```kotlin
private val copySteps: List<() -> Long> = listOf(
  { copyUsers() },
  { copyCredentials() },
  { copySessions() },
)

fun migrateAll() {
  copySteps.forEachIndexed { i, step ->
    logger.info("Migration step ${i + 1}/${copySteps.size}")
    var offset = 0L
    while (true) {
      val copied = step()   // or step(offset, chunkSize)
      if (copied <= 0) break
      offset += copied
    }
  }
}
```

Each `copyX()`:

- `selectAll().orderBy(pk).limit(chunkSize).offset(offset)` on the source table within
  `transaction(sourceDb)`.
- Map rows to the target statement, then `batchUpsert` (users/credentials) or `batchInsert`
  (sessions) within `transaction(targetDb)`.
- Return the row count so the caller can advance.

Because there is no DAO `isTableExists`, validate the source tables with a cheap
`selectAll().limit(1)` inside a try/catch and map failures to the existing
`SourceDatabaseConnectException` / `SourceTableNotFoundException` errors. Keep those error types
so the public behavior is unchanged.

For credentials, preserve the one-to-one key: the source `navauth_credentials.uuid` is the user
UUID, so upsert on that column.

## `LibreLoginMigrator` — read the legacy table with Exposed

`LibreLoginUser` (the `@DatabaseTable`) is deleted and replaced with a read-only table object.
Only the columns actually consumed by the mapping are needed — leave out `joined`, `last_seen`,
`last_authentication`, `last_server`, `email` to avoid pulling in a timestamp column type:

```kotlin
object LibreLoginUserTable : Table("librepremium_data") {
  val uuid         = uuid("uuid")
  val premiumUuid  = uuid("premium_uuid").nullable()
  val passwordHash = varchar("hashed_password", 255).nullable()
  val passwordSalt = varchar("salt", 255).nullable()
  val passwordAlgo = varchar("algo", 255).nullable()
  val lastNickname = varchar("last_nickname", 255)
  val secret       = varchar("secret", 255).nullable()
  override val primaryKey = PrimaryKey(uuid)
}
```

If the legacy column is physically `VARCHAR` and Exposed's `uuid()` maps it correctly on that
driver, this is fine; otherwise use a text-backed custom UUID column type (see type mapping).
The LibreLogin source is typically SQLite, so verify there.

Rewrite the body to page `selectAll().orderBy(LibreLoginUserTable.uuid)` and map each row into the
existing domain objects. `getHashedPassword`/`convertTo*` stay exactly as they are; only the data
access changes. Writes to NavAuth go through the Exposed repositories (or DSL), inside
`transaction(targetDb)`.

## Behavior to preserve

- Skip a row if the user already exists (`findByUsernameIgnoreCase`) and log the same message.
- Skip invalid non-premium records with no password and no 2FA secret.
- Chunked iteration with `offset`/`limit` (`MigrationConfig.chunkSize`), logging progress.
- Do not change `MigrationManager`, `Migrator`, `MigrationOriginPluginType`, or the
  `MigratorFactory` surface more than required.
