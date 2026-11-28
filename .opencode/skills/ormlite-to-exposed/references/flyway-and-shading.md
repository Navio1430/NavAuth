# Flyway wiring, locations, and Shadow/JAR pitfalls

## Dependencies

`gradle/libs.versions.toml`:

```toml
[versions]
exposed = "1.5.0"
flyway = "13.6.0"
testcontainers = "2.0.5"

[libraries]
exposed-core = { module = "org.jetbrains.exposed:exposed-core", version.ref = "exposed" }
exposed-jdbc = { module = "org.jetbrains.exposed:exposed-jdbc", version.ref = "exposed" }

flyway-core                = { module = "org.flywaydb:flyway-core", version.ref = "flyway" }
flyway-mysql               = { module = "org.flywaydb:flyway-mysql", version.ref = "flyway" }
flyway-postgresql          = { module = "org.flywaydb:flyway-database-postgresql", version.ref = "flyway" }
flyway-sqlite              = { module = "org.flywaydb:flyway-database-nc-sqlite", version.ref = "flyway" }

testcontainers-bom         = { module = "org.testcontainers:testcontainers-bom", version.ref = "testcontainers" }
testcontainers-junit       = { module = "org.testcontainers:testcontainers-junit-jupiter" }      # no version, BOM
testcontainers-mysql       = { module = "org.testcontainers:testcontainers-mysql" }
testcontainers-mariadb     = { module = "org.testcontainers:testcontainers-mariadb" }
testcontainers-postgresql  = { module = "org.testcontainers:testcontainers-postgresql" }
```

`navauth-common/build.gradle.kts`:

```kotlin
dependencies {
  api(libs.exposed.core)
  api(libs.exposed.jdbc)
  implementation(libs.flyway.core)
  implementation(libs.flyway.mysql)
  implementation(libs.flyway.postgresql)
  implementation(libs.flyway.sqlite)

  testImplementation(platform(libs.testcontainers.bom))
  testImplementation(libs.testcontainers.junit)
  testImplementation(libs.testcontainers.mysql)
  testImplementation(libs.testcontainers.mariadb)
  testImplementation(libs.testcontainers.postgresql)
}
```

Do not forget the runtime drivers stay as they are (`h2`, `mysql`, `postgresql`, `sqlite-jdbc`).

## Module facts (do not guess artifact names)

- MySQL and MariaDB → `flyway-mysql` only. There is no `flyway-database-mariadb`.
- SQLite → `flyway-database-nc-sqlite`. There is no `flyway-database-sqlite`.
- H2 → included in `flyway-core`; no separate module.
- `flyway-database-postgresql` is required for PostgreSQL on Flyway 10+.

## Migration locations

Resources live under `navauth-common/src/main/resources/migration/<driver>/`:

```
migration/h2/V001_initial.sql
migration/sqlite/V001_initial.sql
migration/mysql/V001_initial.sql
migration/mariadb/V001_initial.sql
migration/postgresql/V001_initial.sql
```

Select the location from the active driver. Add a `migrationDir` to `DatabaseDriverType`
(`H2_MEM` and `H2_FILE` share `"h2"`, everything else uses its lowercased name), then:

```kotlin
Flyway.configure()
  .dataSource(dataSource)                 // the existing HikariDataSource
  .locations("classpath:migration/${config.driverType.migrationDir}")
  .baselineOnMigrate(true)
  .baselineVersion("1")
  .load()
  .migrate()
```

Run this after `DatabaseManager.connect()` creates the `DataSource`, and before
`Database.connect(dataSource)` so Exposed never sees a missing schema.

`baselineOnMigrate(true)` adopts an existing OrmLite database: Flyway sees no
`flyway_schema_history`, marks V001 as the baseline, and does not re-run it. This is only correct
because V001 mirrors the existing physical schema. Never edit V001 to change types; add
`V002_...` for that.

For a brand-new install, the same config runs V001 and creates the schema.

## MySQL/MariaDB detection caveat

MariaDB connects through the MySQL JDBC driver and URL (`jdbc:mysql://...`). Flyway still needs
`flyway-mysql`; with the correct module Flyway detects MariaDB by server version. Keep the
`MARIADB` enum separate so the correct `migration/mariadb` resources are selected, even though the
DDL may be identical to MySQL.

## Shadow / shaded-jar pitfalls

`buildSrc/src/main/kotlin/navauth-shadow.gradle.kts` currently relocates many packages and does
**not** call `mergeServiceFiles()`. Flyway and the JDBC drivers discover dialects/plugins through
`META-INF/services` (`java.sql.Driver`, `org.flywaydb.core.extensibility.Plugin`). Relocation plus
`DuplicatesStrategy.EXCLUDE` can silently drop these entries.

Required changes:

1. Add `mergeServiceFiles()` to the `shadowJar` task so `META-INF/services` entries are merged
   instead of one provider winning.
2. Exposed lives under `org.jetbrains.exposed`, which the existing `relocatePrefixed("org.jetbrains")`
   already catches. Verify the shaded jar still starts; do not add a second conflicting rule.
3. Flyway is not currently relocated. Either leave it unrelocated (simplest) or add an explicit
   `relocate("org.flywaydb", ...)` — but if you relocate it, and also relocate the JDBC drivers,
   `mergeServiceFiles()` becomes mandatory.
4. **Leave `org.sqlite` unrelocated.** The commented-out line and its TODO are a known bug that is
   explicitly out of scope here. Do not "fix" it as part of this migration.

Verify the shaded artifact by running from the built jar against at least one container-backed
database and one file-backed database, not just via Gradle tests.
