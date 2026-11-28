# Integration tests: one suite, every backend

Goal: run the **same** repository behavior suite against H2, SQLite, MySQL, MariaDB, and
PostgreSQL, with Flyway migrations applied first. Avoid duplication: one abstract test class holds
all assertions; each backend is a ~3-line subclass.

## Pattern

Use JUnit 5 (the module already depends on `kotest-runner-junit5`, which runs Jupiter tests).
Annotate the base with `@TestInstance(Lifecycle.PER_CLASS)` so `@BeforeAll`/`@AfterAll` can be
non-static, and manage the container lifecycle manually instead of the `@Testcontainers`
extension — that keeps a single code path for container and non-container backends.

```kotlin
// test/.../database/RepositoryIntegrationTest.kt
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class RepositoryIntegrationTest {
  protected abstract val backend: TestBackend

  private var container: JdbcDatabaseContainer<*>? = null
  private lateinit var dataSource: HikariDataSource
  protected lateinit var database: Database

  // instantiate the real Exposed repositories here (or build a Guice injector)
  protected lateinit var users: UserRepository
  protected lateinit var credentials: UserCredentialsRepository
  protected lateinit var sessions: UserActivitySessionRepository

  @BeforeAll
  fun setUp() {
    container = backend.containerFactory?.invoke()?.also { it.start() }
    val url = container?.jdbcUrl ?: backend.localJdbcUrl()
    val user = container?.username ?: backend.localUser()
    val pass = container?.password ?: backend.localPassword()

    dataSource = HikariDataSource(HikariConfig().apply {
      jdbcUrl = url
      username = user
      password = pass
      maximumPoolSize = 4
      driverClassName = backend.driverType.driverClassName
    })

    Flyway.configure()
      .dataSource(dataSource)
      .locations("classpath:migration/${backend.driverType.migrationDir}")
      .baselineOnMigrate(true)
      .load()
      .migrate()

    database = Database.connect(dataSource)
    users = ExposedUserRepository(database)
    credentials = ExposedUserCredentialsRepository(database)
    sessions = ExposedUserActivitySessionRepository(database)
  }

  @AfterAll
  fun tearDown() {
    dataSource.close()
    container?.stop()
  }

  @BeforeEach
  fun clean() = transaction(database) {
    UserSessionsTable.deleteAll()
    CredentialsTable.deleteAll()
    UsersTable.deleteAll()
  }

  // ---- shared tests ----
  @Test fun `save then find user by uuid`() { /* ... */ }
  @Test fun `find by case-insensitive username`() { /* ... */ }
  @Test fun `find premium user by mojang uuid`() { /* ... */ }
  @Test fun `save credentials and read hashing algorithm`() { /* ... */ }
  @Test fun `delete credentials by user`() { /* ... */ }
  @Test fun `sessions are ordered newest first and paginate`() { /* ... */ }
}
```

Backends in `TestBackends.kt` (shared with the capture test):

```kotlin
class MySqlRepositoryIntegrationTest : RepositoryIntegrationTest() {
  override val backend = TestBackend.MYSQL
}
class MariaDbRepositoryIntegrationTest : RepositoryIntegrationTest() {
  override val backend = TestBackend.MARIADB
}
class PostgreSqlRepositoryIntegrationTest : RepositoryIntegrationTest() {
  override val backend = TestBackend.POSTGRESQL
}
class H2RepositoryIntegrationTest : RepositoryIntegrationTest() {
  override val backend = TestBackend.H2_MEM
}
class SqliteRepositoryIntegrationTest : RepositoryIntegrationTest() {
  override val backend = TestBackend.SQLITE
}
```

`localJdbcUrl()` for the no-container backends:

- `H2_MEM` → `jdbc:h2:mem:<unique>;DB_CLOSE_DELAY=-1`
- `SQLITE` → `jdbc:sqlite:${Files.createTempFile("navauth-it", ".db")}`

## Container images

Use explicit, pinned tags so CI is reproducible:

- `TestBackend.MYSQL` → `MySQLContainer("mysql:8.4")`
- `TestBackend.MARIADB` → `MariaDBContainer("mariadb:11.4")`
- `TestBackend.POSTGRESQL` → `PostgreSQLContainer("postgres:16-alpine")`

## Testcontainers 2.x coordinates (trap)

The BOM is `2.0.5`, and 2.x renamed the artifacts:

| 1.x (do not use) | 2.x (use) |
| --- | --- |
| `org.testcontainers:mysql` | `org.testcontainers:testcontainers-mysql` |
| `org.testcontainers:mariadb` | `org.testcontainers:testcontainers-mariadb` |
| `org.testcontainers:postgresql` | `org.testcontainers:testcontainers-postgresql` |
| `org.testcontainers:junit-jupiter` | `org.testcontainers:testcontainers-junit-jupiter` |

Almost every tutorial online uses the 1.x coordinates; if Gradle cannot resolve, this is why.
Container classes themselves remain under `org.testcontainers.containers.*` — confirm imports
against the resolved version.

## Rules

- Tests must go through the **real** Exposed repository implementations, not mocks, and must run
  Flyway migrations rather than `SchemaUtils.create` (the point is to validate the SQL scripts).
- Do not duplicate assertions per backend; if two backends need different expectations, fix the
  schema/driver handling instead.
- Keep Docker-only backends in the default test run; the H2/SQLite subclasses make the suite
  usable locally without Docker. Document that container tests require a running Docker daemon.
- Existing `DataPersistenceTestExtension` / `TestsConfigModule` assume `connectAndInit()` creates
  tables. Update or retire them once Flyway owns the schema.
