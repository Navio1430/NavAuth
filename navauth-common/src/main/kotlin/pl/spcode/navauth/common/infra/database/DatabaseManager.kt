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

import com.google.inject.Inject
import com.google.inject.Singleton
import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource
import org.jetbrains.exposed.v1.core.DatabaseConfig as ExposedDatabaseConfig
import org.jetbrains.exposed.v1.core.SqlLogger
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.statements.StatementContext
import org.jetbrains.exposed.v1.jdbc.Database
import pl.spcode.navauth.common.shared.PluginDirectory

@Singleton
class DatabaseManager
@Inject
constructor(val config: DatabaseConfig, val pluginDirectory: PluginDirectory) {

  val dataSource: HikariDataSource = HikariDataSource()

  lateinit var database: Database
    private set

  fun connect(poolName: String = "NavAuth-pool") {
    dataSource.poolName = poolName

    dataSource.addDataSourceProperty("cachePrepStmts", "true")
    dataSource.addDataSourceProperty("prepStmtCacheSize", "250")
    dataSource.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
    dataSource.addDataSourceProperty("useServerPrepStmts", "true")
    // MySQL Connector/J's information_schema keyword lookup is incompatible with MariaDB 11.4+,
    // and the connection uses the MySQL driver for MariaDB. Fall back to the driver's SHOW-based
    // metadata so identifier handling does not fail.
    dataSource.addDataSourceProperty("useInformationSchema", "false")

    dataSource.maximumPoolSize = config.poolSize
    dataSource.connectionTimeout = config.connectionTimeout
    dataSource.username = config.username
    dataSource.password = config.password

    dataSource.driverClassName = config.driverType.driverClassName

    val driverType = config.driverType
    val driverJdbcFormat = config.driverType.jdbcUrlFormat
    val jdbcUrl: String =
      when (driverType) {
        DatabaseDriverType.H2_MEM,
        DatabaseDriverType.H2_FILE,
        DatabaseDriverType.SQLITE -> {
          val dbFilename = pluginDirectory.path.resolve(config.database).toAbsolutePath().toString()

          val supportedExtensions =
            listOf(".sqlite", ".sqlite3", ".db", ".db3", ".s3db", ".sl3", ".h2.db", ".mv.db")

          val finalFilename =
            if (supportedExtensions.any { dbFilename.endsWith(it, ignoreCase = true) }) {
              dbFilename
            } else {
              if (driverType == DatabaseDriverType.H2_FILE) {
                dbFilename
              } else {
                "$dbFilename.db"
              }
            }

          driverJdbcFormat.format(finalFilename)
        }
        DatabaseDriverType.MYSQL,
        DatabaseDriverType.MARIADB -> {
          config.driverType.jdbcUrlFormat.format(
            config.hostname,
            config.port,
            config.database,
            DatabaseDriverType.sslParamForMySQL(config.ssl),
          )
        }
        DatabaseDriverType.POSTGRESQL -> {
          config.driverType.jdbcUrlFormat.format(
            config.hostname,
            config.port,
            config.database,
            DatabaseDriverType.sslParamForPostgreSQL(config.ssl),
          )
        }
      }

    dataSource.jdbcUrl = jdbcUrl
  }

  /** Runs Flyway migrations for the active driver. Flyway owns the schema. */
  fun migrate() {
    SchemaMigrations.migrate(dataSource, config.driverType)
  }

  fun connectAndInit(poolName: String = "NavAuth-pool") {
    connect(poolName)
    migrate()
    database = connectExposed(dataSource)
  }

  fun closeConnections() {
    dataSource.close()
  }

  companion object {
    /**
     * Exposed settings of every database NavAuth opens: the plugin's own, migration sources and
     * tests.
     * - `preserveIdentifierCasing`: the OrmLite-created columns are camelCase (`passwordHash`) and
     *   must be quoted as written.
     * - `defaultMaxAttempts = 1`: Exposed retries a failed `transaction {}` block 3 times by
     *   default. A failure must surface once, and a block must never run twice.
     * - `sqlLogger`: Exposed's default logger prints every statement with its bound values at
     *   DEBUG, password hashes and TOTP secrets included. NavAuth never logs SQL.
     */
    fun exposedDatabaseConfig(): ExposedDatabaseConfig = ExposedDatabaseConfig {
      preserveIdentifierCasing = true
      defaultMaxAttempts = 1
      sqlLogger = SilentSqlLogger
    }

    /** Logs nothing, so no bound value of a statement ever reaches a log. */
    private object SilentSqlLogger : SqlLogger {
      override fun log(context: StatementContext, transaction: Transaction) = Unit
    }

    /** Connects Exposed to [dataSource] with [exposedDatabaseConfig]. */
    fun connectExposed(dataSource: DataSource): Database =
      Database.connect(dataSource, databaseConfig = exposedDatabaseConfig())
  }
}
