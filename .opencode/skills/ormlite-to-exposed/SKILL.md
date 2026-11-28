---
name: ormlite-to-exposed
description: Use when migrating NavAuth from OrmLite to JetBrains Exposed, replacing com.j256.ormlite with org.jetbrains.exposed, introducing Flyway schema migrations into navauth-common, writing per-dialect V001_initial.sql, or rewriting the persistence layer and data migrators. Covers capturing the current OrmLite schema per database driver, DSL-only table/repository rewrites, OrmLite-free migrators, Testcontainers integration tests, and Shadow/JAR pitfalls. Complements kotlin-exposed-patterns.
---

# NavAuth: OrmLite → JetBrains Exposed migration

This skill is deliberately **delta-only**. Generic Exposed DSL/DAO, HikariCP, `Database.connect`,
table definitions, queries, and repository patterns are already documented in the
`kotlin-exposed-patterns` skill — read that first and do **not** re-derive them here.
This skill records what is specific to NavAuth, which decisions are already made, and the traps
that only show up when removing `com.j256.ormlite`.

## Non-negotiable decisions (already agreed)

1. **DSL only.** No `DAO`, no `UUIDTable`/`IdTable`, no `Entity` classes. Define plain `Table`
   objects and drive them with the Exposed DSL. Keys are caller-supplied UUIDs, not generated,
   so a table declares its own primary key (`override val primaryKey = PrimaryKey(uuid)`).
2. **The sessions table stays PK-less** and DSL-only. Do not add a surrogate/id column; the
   logical row key remains `(uuid, joined_at)` and reads are append-only. Preserving the
   physical shape also lets existing installs baseline cleanly under Flyway.
3. **Exposed replaces OrmLite in the migrators too.** The legacy LibreLogin source database is
   read with a second Exposed `Database`, not with an OrmLite `Dao`.
4. **Flyway owns the schema.** After the migration, nothing creates tables at runtime — remove
   `DatabaseManager.initDatabase` / `EntitiesRegistrar` / `TableUtils.createTableIfNotExists`.
   `SchemaUtils.create` may remain only in tests.
5. **Do not relocate `org.sqlite`.** It is a known, intentional bug that is out of scope for
   this migration; leave the commented-out `relocatePrefixed("org.sqlite")` in
   `buildSrc/src/main/kotlin/navauth-shadow.gradle.kts` untouched.
6. **Blocking transactions.** Velocity/NavAuth is synchronous: use `transaction {}` /
   `transaction(db) {}`, never `newSuspendedTransaction`.

## Pinned versions (verify on Maven Central before bumping)

| Library | Version | Coordinates |
| --- | --- | --- |
| Exposed | `1.5.0` | `org.jetbrains.exposed:exposed-core`, `:exposed-jdbc` |
| Flyway | `13.6.0` | `org.flywaydb:flyway-core`, `:flyway-mysql` (MySQL **and** MariaDB), `:flyway-database-postgresql`, `:flyway-database-nc-sqlite` |
| Testcontainers | BOM `2.0.5` | `org.testcontainers:testcontainers-mysql`, `:testcontainers-mariadb`, `:testcontainers-postgresql`, `:testcontainers-junit-jupiter` |

Flyway 10+ split databases into modules. There is **no** `flyway-database-sqlite` and **no**
`flyway-database-mariadb`; SQLite is `flyway-database-nc-sqlite`, MariaDB rides on `flyway-mysql`,
and H2 is bundled in `flyway-core`. Do not invent artifact names.

## Migration phases

Do these in order. Phases 0–1 need OrmLite still present; phases 2+ remove it.

### Phase 0 — capture the current schema (one-off, with OrmLite)

The Flyway scripts must describe the schema OrmLite actually created, per driver, because
physical types differ (`UUID` vs `VARCHAR(36)`, `BOOLEAN` vs `TINYINT(1)`, `DATE_LONG` → `BIGINT`).
Before deleting OrmLite, run the generic capture test described in
`references/schema-capture.md` against every backend. It emits the exact
`TableUtils.getCreateTableStatements(...)` DDL. This runs **once**; commit the output as the
per-dialect `V001_initial.sql`.

### Phase 1 — commit the per-dialect Flyway resources

Put the generated SQL at
`navauth-common/src/main/resources/migration/<driver>/V001_initial.sql`.
The reference files in `references/db/<driver>/V001_initial.sql` are a starting point — always
prefer the Phase 0 output whenever it differs.

### Phase 2 — add Exposed + Flyway, remove OrmLite

- `gradle/libs.versions.toml`: remove `ormlite`/`ormlite-jdbc`; add `exposed`, `flyway`,
  `testcontainers` versions and library aliases.
- `navauth-common/build.gradle.kts`: drop `api(libs.ormlite.jdbc)`; add Exposed core+jdbc as
  `api`, Flyway modules as `implementation`. Keep the existing drivers; SQLite/H2 test-only usage
  can move behind `testRuntimeOnly` if not needed by the schema-gen test elsewhere.
- Keep OrmLite available as `testImplementation` **only** until Phase 0 is done and committed,
  then remove it.

### Phase 3 — tables, repositories, transactions (DSL only)

Replace the `@DatabaseTable` records with `Table` objects, rewrite repositories with DSL, delete
the ORM plumbing. See "Tables, repositories, transactions" below and
`references/type-mapping.md`.

### Phase 4 — OrmLite-free data migrators

Rewrite `NavAuthDatabaseMigrator` and `LibreLoginMigrator`. See `references/migrators.md`.

### Phase 5 — integration tests

Reusable Testcontainers-backed repository tests, one shared suite across every backend. See
`references/testing.md`.

### Phase 6 — packaging

Fix the Shadow jar. See `references/flyway-and-shading.md`.

## Files that change (inventory)

| Concern | Path |
| --- | --- |
| Dependencies | `gradle/libs.versions.toml`, `navauth-common/build.gradle.kts` |
| Connection/DDL | `navauth-common/.../infra/database/DatabaseManager.kt`, `.../DatabaseConfig.kt`, `.../DatabaseDriverType.kt` |
| Delete | `.../infra/database/EntitiesRegistrar.kt` |
| ORM plumbing | `.../infra/persistence/ormlite/OrmLiteCrudRepository.kt`, `OrmLiteCrudRepositoryImpl.kt`, `TransactionServiceImpl.kt`, `.../infra/persistence/OrmLitePaginatorImpl.kt` |
| Records → tables | `.../infra/persistence/ormlite/user/UserRecord.kt`, `UserActivitySessionRecord.kt`, `.../ormlite/credentials/UserCredentialsRecord.kt` |
| Repos | `.../ormlite/user/UserRepositoryImpl.kt`, `UserActivitySessionRepositoryImpl.kt`, `UserActivitySessionPaginator.kt`, `.../ormlite/credentials/UserCredentialsRepositoryImpl.kt` |
| Domain interfaces (drop OrmLite return type) | `.../domain/user/UserRepository.kt`, `UserActivitySessionRepository.kt`, `.../domain/credentials/UserCredentialsRepository.kt` |
| Transaction abstraction | `.../domain/common/TransactionService.kt` |
| Mappers (imports/table refs only) | `.../infra/persistence/mapper/*.kt` |
| DI | `.../module/DataPersistenceModule.kt`, `.../module/ServicesModule.kt` |
| Migrators | `.../migrate/migrator/navauth/NavAuthDatabaseMigrator.kt`, `.../migrator/librelogin/LibreLoginMigrator.kt`, `.../migrator/librelogin/LibreLoginUser.kt` |
| Tests | `navauth-common/src/test/kotlin/**` |
| Packaging | `buildSrc/src/main/kotlin/navauth-shadow.gradle.kts` |
| Entry point | `navauth-velocity/src/main/kotlin/pl/spcode/navauth/velocity/NavAuthVelocity.kt` (`connectAndInit` → migrate then connect) |

## Tables, repositories, transactions (DSL only)

Keep the existing domain models and mappers; only the persistence side changes. A table looks
like this (note the explicit primary key and that columns are named exactly as Flyway/V001):

```kotlin
// infra/persistence/exposed/UsersTable.kt
object UsersTable : Table("navauth_users") {
  val uuid = uuid("uuid")
  val mojangUuid = uuid("mojang_uuid").nullable()
  val username = varchar("username", 255)
  val usernameLowercase = varchar("username_lowercase", 255)
  val credentialsRequired = bool("credentials_required")
  override val primaryKey = PrimaryKey(uuid)
}
```

Repository methods use DSL and `transaction`. Because NavAuth is blocking, keep the repository
signatures non-suspend:

```kotlin
override fun findByUserUuid(uuid: UserUuid): User? =
  transaction(database) {
    UsersTable.selectAll().where { UsersTable.uuid eq uuid.value }
      .map { it.toDomain() }.singleOrNull()
  }

override fun save(user: User) =
  transaction(database) {
    UsersTable.upsert(UsersTable.uuid) { stmt ->
      stmt[UsersTable.uuid] = user.uuid.value
      stmt[UsersTable.mojangUuid] = user.mojangUuid?.value
      stmt[UsersTable.username] = user.username.value
      stmt[UsersTable.usernameLowercase] = user.username.value.lowercase()
      stmt[UsersTable.credentialsRequired] = user.credentialsRequired
    }
  }
```

Rules that fall out of "DSL only":

- `save`/`saveAll` no longer return `Dao.CreateOrUpdateStatus`; return `Unit` (or the domain
  object). Update the domain repository interfaces accordingly — OrmLite must not leak into
  `domain/`.
- The generic `OrmLiteCrudRepository(Impl)` base class disappears; each repository talks to its
  `Table` directly. Do not replace it with an equivalent generic Exposed base unless there is a
  concrete reuse need; explicit DSL is preferred.
- Replace `OrmLitePaginatorImpl` / `UserActivitySessionPaginator` with a small DSL paginator that
  computes `count()` and applies `.limit().offset()`; keep the `Paginator<T>` domain interface.
- `TransactionServiceImpl.inTransaction` becomes a wrapper over `transaction {}`. Guard against
  orphaned transactions: prefer passing blocks that themselves open the transaction, or use
  `TransactionManager.currentOrNull()` semantics consistently. `TransactionService` is used by
  `UserService`, `UserCredentialsService`, and the migrators — check every call site.
- Ordering must mirror the ORM: sessions are read `left_at DESC`; username lookups use
  `username_lowercase`; mojang lookup uses `mojang_uuid`.
- The user-assigned UUID PK means **`insert` on a duplicate fails**; use `upsert` (or update-then-
  insert) to reproduce OrmLite's `createOrUpdate` semantics.

## Flyway wiring

Select the migration location from the active driver, mirroring the config enum:

```kotlin
// DatabaseDriverType gains a migrationDir (see references/flyway-and-shading.md)
val location = "classpath:migration/${config.driverType.migrationDir}"
Flyway.configure()
  .dataSource(dataSource)
  .locations(location)
  .baselineOnMigrate(true)   // existing OrmLite DBs have no flyway_schema_history
  .baselineVersion("1")
  .load()
  .migrate()
```

Order matters at startup: `connect()` builds the Hikari `DataSource`; then run Flyway; then
`Database.connect(dataSource)` for Exposed. `connectAndInit()` becomes "connect + migrate +
bind database" (it no longer creates tables). `baselineOnMigrate` is only safe because
`V001_initial.sql` mirrors the pre-existing physical schema; any real type change belongs in a
**new** version (`V002_...`), never by editing V001.

## Verification

- `gradlew :navauth-common:test` (H2/SQLite run without Docker; containers need Docker).
- `gradlew :navauth-common:generateInitialSchema` once, in Phase 0 only.
- `gradlew spotlessCheck` (ktfmt googleStyle / googleJavaFormat).
- Manually boot against one container DB and one file DB and confirm NAvAuth starts, migrates,
  and repositories behave.
