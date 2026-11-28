/*
 * NavAuth
 * Copyright © 2026 Oliwier Fijas (Navio1430)
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

package integration.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.nio.file.Files
import java.util.UUID
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.mariadb.MariaDBContainer
import org.testcontainers.mysql.MySQLContainer
import org.testcontainers.postgresql.PostgreSQLContainer
import pl.spcode.navauth.common.infra.database.DatabaseDriverType

enum class TestBackend(
  val driverType: DatabaseDriverType,
  val containerFactory: (() -> JdbcDatabaseContainer<*>)?,
  private val localJdbcUrlFactory: (() -> String)?,
  val localUser: String = "",
  val localPassword: String = "",
) {
  H2_MEM(
    DatabaseDriverType.H2_MEM,
    null,
    { "jdbc:h2:mem:navauth_it_${UUID.randomUUID()};DB_CLOSE_DELAY=-1" },
    localUser = "sa",
  ),
  SQLITE(
    DatabaseDriverType.SQLITE,
    null,
    { "jdbc:sqlite:${Files.createTempFile("navauth-it", ".db").toAbsolutePath()}" },
  ),
  MYSQL(
    DatabaseDriverType.MYSQL,
    {
      MySQLContainer("mysql:8.4")
        .withDatabaseName("navauth")
        .withUsername("test")
        .withPassword("test")
    },
    null,
  ),
  MARIADB(
    DatabaseDriverType.MARIADB,
    {
      NavAuthMariaDbContainer("mariadb:11.4")
        .withDatabaseName("navauth")
        .withUsername("test")
        .withPassword("test")
    },
    null,
  ),
  POSTGRESQL(
    DatabaseDriverType.POSTGRESQL,
    {
      PostgreSQLContainer("postgres:16-alpine")
        .withDatabaseName("navauth")
        .withUsername("test")
        .withPassword("test")
    },
    null,
  );

  fun localJdbcUrl(): String =
    requireNotNull(localJdbcUrlFactory) { "No local JDBC URL for $name" }()
}

/**
 * NavAuth talks to MariaDB through the MySQL Connector/J driver (see [DatabaseDriverType.MARIADB]),
 * so the test container must expose a `jdbc:mysql://` URL and load the MySQL driver instead of the
 * default MariaDB client, which is not on the classpath.
 */
private class NavAuthMariaDbContainer(image: String) : MariaDBContainer(image) {
  override fun getDriverClassName(): String = DatabaseDriverType.MYSQL.driverClassName

  override fun getJdbcUrl(): String =
    "jdbc:mysql://$host:$firstMappedPort/$databaseName?sslMode=DISABLED"
}

/** A started backend (container, or in-process database) with a connection pool to it. */
class TestDatabase(val backend: TestBackend) : AutoCloseable {

  private val container: JdbcDatabaseContainer<*>? =
    backend.containerFactory?.invoke()?.also { it.start() }

  val dataSource: HikariDataSource =
    HikariDataSource(
      HikariConfig().apply {
        jdbcUrl = container?.jdbcUrl ?: backend.localJdbcUrl()
        username = container?.username ?: backend.localUser
        password = container?.password ?: backend.localPassword
        maximumPoolSize = 4
        driverClassName = backend.driverType.driverClassName
        addDataSourceProperty("useInformationSchema", "false")
      }
    )

  override fun close() {
    dataSource.close()
    container?.stop()
  }
}
