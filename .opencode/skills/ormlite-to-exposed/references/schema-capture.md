# Capturing the current OrmLite schema (Phase 0, one-off)

The goal is to obtain, for every supported driver, the exact DDL OrmLite generated for the three
live tables **before** removing OrmLite. Those statements become
`navauth-common/src/main/resources/migration/<driver>/V001_initial.sql`.

OrmLite can print its own DDL, so do not hand-guess types. Use
`TableUtils.getCreateTableStatements(connectionSource, clazz.java)`, which returns the `CREATE
TABLE` / `CREATE INDEX` statements for the active dialect. Run it once per backend.

## Shared backend abstraction

Both the capture test and the integration tests need the same list of backends. Define it once
under `src/test/kotlin` and reuse it. Keep it small — an enum plus the container (if any).

```kotlin
// test/.../database/TestBackends.kt
enum class TestBackend(
  val driverType: DatabaseDriverType,
  val containerFactory: (() -> JdbcDatabaseContainer<*>)?,
) {
  H2_MEM(DatabaseDriverType.H2_MEM, null),
  SQLITE(DatabaseDriverType.SQLITE, null),
  MYSQL(DatabaseDriverType.MYSQL, { MySQLContainer("mysql:8.4") }),
  MARIADB(DatabaseDriverType.MARIADB, { MariaDBContainer("mariadb:11.4") }),
  POSTGRESQL(DatabaseDriverType.POSTGRESQL, { PostgreSQLContainer("postgres:16-alpine") }),
}
```

The capture test should only need MySQL/MariaDB/PostgreSQL containers; H2 uses
`jdbc:h2:mem:capture;DB_CLOSE_DELAY=-1` and SQLite uses a temp file.

## Generic capture test

Keep it generic over `TestBackend` and write the output into a build directory (do **not** write
straight into `src/main/resources` from a test). Tag it so it never runs in the normal suite:

```kotlin
@Tag("schema-gen")
class SchemaCaptureTest {
  @TestFactory
  fun `capture ormlite ddl`() = TestBackend.entries.map { backend ->
    DynamicTest.dynamicTest(backend.name) {
      val (dataSource, close) = backend.openDataSource()          // container or local
      try {
        val registry = EntitiesRegistrar()
          .registerEntity(UserRecord::class, UUID::class)
          .registerEntity(UserCredentialsRecord::class, UUID::class)
          .registerEntity(UserActivitySessionRecord::class, UUID::class)
        val manager = DatabaseManager(
          config = backend.toDatabaseConfig(),
          entitiesRegistrar = registry,
          pluginDirectory = PluginDirectory(Path.of("build/schema-capture")),
        )
        manager.connect()
        val statements = registry.getTypes().flatMap {
          TableUtils.getCreateTableStatements(manager.connectionSource, it.java)
        }
        val out = Path.of("build/schema-capture/${backend.name.lowercase()}/V001_initial.sql")
        out.parent.toFile().mkdirs()
        out.writeText(statements.joinToString("\n") + "\n")
      } finally {
        close()
      }
    }
  }
}
```

Wire it up:

- Add `@Tag("schema-gen")` exclusion to the default `test` task in
  `navauth-common/build.gradle.kts`: `tasks.test { useJUnitPlatform { excludeTags("schema-gen") } }`.
- Register a one-shot task:

```kotlin
tasks.register<Test>("generateInitialSchema") {
  useJUnitPlatform { includeTags("schema-gen") }
}
```

- Run `gradlew :navauth-common:generateInitialSchema`, then move
  `build/schema-capture/<driver>/V001_initial.sql` into
  `navauth-common/src/main/resources/migration/<driver>/`.
- After committing the SQL, delete the test and the temporary `testImplementation` OrmLite
  dependency. The capture step is not kept around.

## What to sanity-check in the captured output

- Table names: `navauth_users`, `navauth_credentials`, `navauth_users_sessions`.
- `navauth_users_sessions` must have **no** primary key (it is intentionally PK-less).
- `joined_at` / `left_at` come from `DataType.DATE_LONG` → an integer/BIGINT column, not a
  timestamp.
- `HashingAlgorithm algo` is stored as text (OrmLite stores enum `name()`), not an ordinal.
- `credentials_required` is boolean-ish: `BOOLEAN` on H2/PostgreSQL, `TINYINT(1)`/`INTEGER` on
  MySQL/MariaDB/SQLite.
- UUID columns: `UUID` on H2/PostgreSQL, `VARCHAR(36)` on MySQL/MariaDB/SQLite.
- Index names and uniqueness. MySQL will not accept `CREATE INDEX IF NOT EXISTS`; the captured
  OrmLite output already avoids it, but strip/rename any dialect-incompatible forms.

## Fallback reference DDL

`references/db/<driver>/V001_initial.sql` contains a best-effort dialect rendering of the same
schema. Treat it as a cross-check only; the Phase 0 capture is authoritative.
