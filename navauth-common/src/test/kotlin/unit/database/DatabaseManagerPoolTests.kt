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

package unit.database

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import pl.spcode.navauth.common.infra.database.DatabaseConfig
import pl.spcode.navauth.common.infra.database.DatabaseDriverType
import pl.spcode.navauth.common.infra.database.DatabaseManager
import pl.spcode.navauth.common.shared.PluginDirectory

private class PoolOfOneConfig : DatabaseConfig() {
  init {
    driverType = DatabaseDriverType.H2_MEM
    database = "pool_of_one"
    username = "test"
    poolSize = 1
    connectionTimeout = 5000
  }
}

class DatabaseManagerPoolTests :
  FunSpec({
    test("a pool of one connection starts and migrates the schema") {
      val config = PoolOfOneConfig()
      val manager = DatabaseManager(config, PluginDirectory(Files.createTempDirectory("navauth")))

      manager.connectAndInit()

      try {
        manager.dataSource.connection.use { connection ->
          connection.createStatement().use { statement ->
            statement.executeQuery("SELECT COUNT(*) FROM navauth_users").use { rows ->
              rows.next() shouldBe true
              rows.getInt(1) shouldBe 0
            }
          }
        }
        manager.dataSource.maximumPoolSize shouldBe 1
      } finally {
        manager.closeConnections()
      }
    }
  })
