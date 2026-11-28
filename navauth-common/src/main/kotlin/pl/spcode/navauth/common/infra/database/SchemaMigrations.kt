/*
 * NavAuth
 * Copyright © 2025 Oliwier Fijas (Navio1430)
 *
 * NavAuth is free software; You can redistribute it and/or modify it under the terms of:
 * the GNU Affero General Public License version 3 as published by the Free Software Foundation.
 *
 * NavAuth is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with NavAuth. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 *
 */

package pl.spcode.navauth.common.infra.database

import javax.sql.DataSource
import org.flywaydb.core.Flyway
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Brings a NavAuth schema to the newest version with Flyway. Both the plugin ([DatabaseManager])
 * and the tests run migrations through here, so the tests exercise the production setup.
 *
 * Baseline rules (Flyway history table missing):
 * - `navauth_users` exists: an install created before Flyway (OrmLite). Its tables match V001, so
 *   it is baselined at version 1 and V001 is skipped.
 * - otherwise: a fresh install. The schema may be empty or shared with other plugins' tables, so it
 *   is baselined at version 0 and V001 creates the NavAuth tables.
 */
object SchemaMigrations {

  const val HISTORY_TABLE = "navauth_flyway_schema_history"

  /** Present in every NavAuth schema since the first release; marks a pre-Flyway install. */
  private const val LEGACY_MARKER_TABLE = "navauth_users"

  /** Tables NavAuth cannot start without. Checked after migrating. */
  private val REQUIRED_TABLES = listOf("navauth_users", "navauth_credentials")

  private val logger: Logger = LoggerFactory.getLogger(SchemaMigrations::class.java)

  fun migrate(dataSource: DataSource, driverType: DatabaseDriverType) {
    val location = "classpath:migration/${driverType.migrationDir}"
    val legacyInstall = tableExists(dataSource, LEGACY_MARKER_TABLE)
    logger.info("Running database migrations from {}", location)

    val result = flyway(dataSource, driverType, if (legacyInstall) "1" else "0").migrate()
    logger.info(
      "Database schema is at version {} ({} migration(s) applied)",
      result.targetSchemaVersion ?: result.initialSchemaVersion,
      result.migrationsExecuted,
    )

    val missing = REQUIRED_TABLES.filterNot { tableExists(dataSource, it) }
    check(missing.isEmpty()) {
      "NavAuth tables $missing are missing after the database migration. " +
        "The schema history table '$HISTORY_TABLE' does not match the tables in this database. " +
        "Restore the database from a backup or contact NavAuth developers."
    }
  }

  /** The version recorded in the schema history table, or null before the first migration. */
  fun schemaVersion(dataSource: DataSource, driverType: DatabaseDriverType): String? =
    flyway(dataSource, driverType, "0").info().current()?.version?.version

  private fun flyway(
    dataSource: DataSource,
    driverType: DatabaseDriverType,
    baselineVersion: String,
  ) =
    // Flyway's default (context) class loader on Velocity misses its plugins and our scripts
    Flyway.configure(SchemaMigrations::class.java.classLoader)
      .dataSource(dataSource)
      .locations("classpath:migration/${driverType.migrationDir}")
      .table(HISTORY_TABLE)
      .sqlMigrationSeparator("_")
      .baselineOnMigrate(true)
      .baselineVersion(baselineVersion)
      .load()

  /**
   * Looks the table up in the connection's current catalog and schema. Unquoted identifiers are
   * stored upper case by some databases, so both spellings are checked.
   */
  fun tableExists(dataSource: DataSource, table: String): Boolean =
    dataSource.connection.use { conn ->
      val meta = conn.metaData
      val schema = runCatching { conn.schema }.getOrNull()
      listOf(table, table.uppercase()).distinct().any { name ->
        meta.getTables(conn.catalog, schema, name, null).use { it.next() }
      }
    }
}
