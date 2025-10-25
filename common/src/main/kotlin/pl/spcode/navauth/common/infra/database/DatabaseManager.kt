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

import com.j256.ormlite.dao.Dao
import com.j256.ormlite.dao.DaoManager
import com.j256.ormlite.jdbc.DataSourceConnectionSource
import com.j256.ormlite.support.ConnectionSource
import com.zaxxer.hikari.HikariDataSource
import kotlin.reflect.KClass

class DatabaseManager(val config: DatabaseConfig) {

  private val dataSource: HikariDataSource = HikariDataSource()
  lateinit var connectionSource: ConnectionSource

  fun connect() {
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
  }

  fun close() {
    dataSource.close()
    connectionSource.close()
  }

  fun <T : Any, ID> getDao(clazz: KClass<T>): Dao<T, ID> {
    return DaoManager.createDao(connectionSource, clazz.java)
  }
}
