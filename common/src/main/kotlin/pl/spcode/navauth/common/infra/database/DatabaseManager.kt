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
import pl.spcode.navauth.common.config.GeneralConfig
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

@Singleton
class DatabaseManager
@Inject
constructor(val generalConfig: GeneralConfig, val entitiesRegistrar: EntitiesRegistrar) {

  val config: DatabaseConfig = generalConfig.databaseConfig

  private val dataSource: HikariDataSource = HikariDataSource()
  lateinit var connectionSource: ConnectionSource

  private val daoMap = ConcurrentHashMap<Class<*>, Dao<*, *>>()

  fun connectAndInit() {
    dataSource.poolName = "NavAuthPool"

    dataSource.addDataSourceProperty("cachePrepStmts", "true")
    dataSource.addDataSourceProperty("prepStmtCacheSize", "250")
    dataSource.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
    dataSource.addDataSourceProperty("useServerPrepStmts", "true")

    dataSource.maximumPoolSize = config.poolSize
    dataSource.connectionTimeout = config.connectionTimeout
    dataSource.username = config.username
    dataSource.password = config.password

    dataSource.driverClassName = config.driverType.driverClassName

    val jdbcUrl: String =
      when (config.driverType) {
        DatabaseDriverType.H2_MEM -> config.driverType.jdbcUrlFormat.format(config.database)
      }

    dataSource.jdbcUrl = jdbcUrl

    connectionSource = DataSourceConnectionSource(dataSource, jdbcUrl)

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
