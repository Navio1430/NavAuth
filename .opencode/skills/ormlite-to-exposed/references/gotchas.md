# Headaches and traps

Consolidated list of things that actually bite during this migration. Read this before starting.

## Schema / types

- **UUID representation differs per dialect.** OrmLite/Exposed: `UUID` on H2 and PostgreSQL,
  `VARCHAR(36)` on MySQL/MariaDB/SQLite. Exposed's `uuid()` may not map to the physical type on
  SQLite. If a mismatch appears, register a custom `ColumnType<UUID>` that stores text and keep
  the Flyway DDL as `VARCHAR(36)` — do not silently change the schema.
- **`DATE_LONG` is epoch millis**, not a timestamp. Use `long()` in Exposed; do not introduce
  `timestamp()`/java-time modules, or the physical type changes.
- **Enums are stored by name.** `HashingAlgorithm.algo` must be `enumerationByName(..., 255)`.
  `enumeration()` (ordinal) silently corrupts existing rows.
- **Booleans**: `BOOLEAN` on H2/PostgreSQL, `TINYINT(1)`/`INTEGER` on MySQL/MariaDB/SQLite. Keep
  `bool()` consistent with the captured DDL.
- **`navauth_users_sessions` has no primary key.** Exposed DSL supports this; DAO/`Entity` does
  not. That is one reason the decision is DSL-only. Do not "helpfully" add an id column.
- **`username_lowercase` must stay in sync** with `username.lowercase()`. The old record enforced
  it in `init`; keep doing it in the mapper/upsert.

## Flyway

- **Existing installs have no `flyway_schema_history`.** Without `baselineOnMigrate(true)` Flyway
  will try to run V001 against an already-populated schema and fail. Baseline exactly once.
- **Never edit an applied migration.** If Exposed needs a different type, add `V002_...`.
- **Locations must match the driver.** A missing `migration/<driver>` resource makes Flyway fail
  with "no migrations found" and the app unusable. Map `H2_MEM`/`H2_FILE` to one `h2` folder.
- **Module split (Flyway 10+).** `flyway-mysql` covers MySQL *and* MariaDB; SQLite needs
  `flyway-database-nc-sqlite`; H2 is in `flyway-core`; PostgreSQL needs
  `flyway-database-postgresql`. Guessing `flyway-database-sqlite`/`-mariadb` will not resolve.
- **MySQL/MariaDB index duplication.** The old `DatabaseManager` caught MySQL error 1061 for
  duplicate index creation because OrmLite re-created indexes. With Flyway owning the schema this
  workaround disappears — but if you ever call `SchemaUtils.create` alongside Flyway you bring
  the problem back. Flyway owns DDL; Exposed does not create tables in production.

## Runner / connection

- **Flyway must run after Hikari connects and before `Database.connect`**, on the same
  `DataSource`, so Exposed never sees an unmigrated schema.
- **SQLite is a temp/file database**: the path logic in `DatabaseManager.connect()` appends `.db`
  unless a known extension is present. Keep that logic; use a unique file per test run.
- **H2_MEM + `DB_CLOSE_DELAY=-1`** for tests or the in-memory DB vanishes between connections.
- **MariaDB goes through the MySQL driver/URL.** Flyway still detects MariaDB by version; keep the
  enum value so the right migration folder is chosen.

## DSL / Exposed 1.5.0

- **No generic CRUD base needed.** Each repository talks to its table directly. Do not recreate an
  `OrmLiteCrudRepository` clone.
- **`createOrUpdate` → `upsert(key)`.** A bare `insert` throws on duplicate PK. The sessions table
  (no PK) can only `insert`.
- **Blocking, not suspend.** Use `transaction {}`/`transaction(db) {}`. `kotlin-exposed-patterns`
  examples use `newSuspendedTransaction`; that is for coroutine code and does not fit NavAuth.
- **`InTransaction`/thread-local gotchas.** If `TransactionService.inTransaction` is kept, ensure
  call sites do not open a transaction then call a repository method that opens another on a
  different connection. Nested `transaction {}` reuses the current transaction by default — verify
  behavior at each call site in `UserService` / `UserCredentialsService`.
- **`save` return types.** Drop `Dao.CreateOrUpdateStatus` from the domain repository interfaces;
  it is OrmLite leaking into `domain/`.

## Packaging

- **Shadow `META-INF/services`.** Flyway dialects and `java.sql.Driver` providers are discovered
  through service files. `DuplicatesStrategy.EXCLUDE` without `mergeServiceFiles()` can drop them,
  producing "Unable to load database plugin" at runtime only in the shaded jar. Add
  `mergeServiceFiles()`.
- **`org.jetbrains` relocation** already covers `org.jetbrains.exposed`; keep it and verify.
- **Do NOT relocate `org.sqlite`.** Known bug, explicitly out of scope; leave the TODO.

## Tests

- **Testcontainers 2.x renamed artifacts** (`testcontainers-mysql`, etc.). The familiar
  `org.testcontainers:mysql` is 1.x-only. This is the most common "why won't this resolve".
- **Docker required** for MySQL/MariaDB/PostgreSQL. H2/SQLite subclasses keep the suite runnable
  locally without Docker; keep them in the same base class.
- **Schema-capture test is one-off.** Tag it `schema-gen`, exclude it from the normal `test` task,
  and delete it after committing the SQL.

## Scope guardrails

- Do not fix the SQLite relocation bug.
- Do not add a primary key to the sessions table.
- Do not convert the existing domain interfaces to suspend/coroutines.
- Do not pull in `exposed-java-time`/`exposed-json` unless a concrete column requires it.
