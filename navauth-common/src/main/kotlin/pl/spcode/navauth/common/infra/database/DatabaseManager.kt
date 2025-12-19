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
import com.j256.ormlite.dao.Dao
import com.j256.ormlite.dao.DaoManager
import com.j256.ormlite.jdbc.DataSourceConnectionSource
import com.j256.ormlite.support.ConnectionSource
import com.j256.ormlite.table.TableUtils
import com.zaxxer.hikari.HikariDataSource
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import pl.spcode.navauth.common.shared.PluginDirectory

@Singleton
class DatabaseManager
@Inject
constructor(
  val config: DatabaseConfig,
  val entitiesRegistrar: EntitiesRegistrar,
  val pluginDirectory: PluginDirectory,
) {

  private val dataSource: HikariDataSource = HikariDataSource()
  lateinit var connectionSource: ConnectionSource

  private val daoMap = ConcurrentHashMap<Class<*>, Dao<*, *>>()

  fun connect(poolName: String = "NavAuth-pool") {
    dataSource.poolName = poolName

    dataSource.addDataSourceProperty("cachePrepStmts", "true")
    dataSource.addDataSourceProperty("prepStmtCacheSize", "250")
    dataSource.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
    dataSource.addDataSourceProperty("useServerPrepStmts", "true")

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
        DatabaseDriverType.SQLITE -> {
          val dbFilename = pluginDirectory.path.resolve(config.database).toAbsolutePath().toString()

          val supportedExtensions = listOf(".sqlite", ".sqlite3", ".db", ".db3", ".s3db", ".sl3")

          val finalFilename =
            if (supportedExtensions.any { dbFilename.endsWith(it, ignoreCase = true) }) {
              dbFilename
            } else {
              "$dbFilename.db"
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

    connectionSource = DataSourceConnectionSource(dataSource, jdbcUrl)
  }

  fun connectAndInit(poolName: String = "NavAuth-pool") {
    connect(poolName)
    initDatabase(entitiesRegistrar)
  }

  fun closeConnections() {
    dataSource.close()
    connectionSource.close()
  }

  @Suppress("UNCHECKED_CAST")
  fun <T : Any, ID> getDao(clazz: KClass<T>): Dao<T, ID> {
    return daoMap.computeIfAbsent(clazz.java) {
      return@computeIfAbsent DaoManager.createDao(connectionSource, clazz.java)
    } as Dao<T, ID>
  }

  private fun initDatabase(entitiesRegistrar: EntitiesRegistrar) {
    entitiesRegistrar.getTypes().forEach {
      TableUtils.createTableIfNotExists(connectionSource, it.java)
    }
  }
}
