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

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.UUID
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import pl.spcode.navauth.common.infra.database.DatabaseDriverType
import pl.spcode.navauth.common.infra.database.DatabaseManager

/** D17: the Exposed settings every NavAuth database connection uses. */
class ExposedConfigTests :
  FunSpec({
    test("identifiers keep their casing and transactions are not retried") {
      val config = DatabaseManager.exposedDatabaseConfig()

      config.preserveIdentifierCasing shouldBe true
      config.defaultMaxAttempts shouldBe 1
    }

    test("a failing transaction block runs once") {
      HikariDataSource(
          HikariConfig().apply {
            jdbcUrl = "jdbc:h2:mem:navauth_retry_${UUID.randomUUID()};DB_CLOSE_DELAY=-1"
            driverClassName = DatabaseDriverType.H2_MEM.driverClassName
            username = "sa"
            maximumPoolSize = 1
          }
        )
        .use { dataSource ->
          val database = DatabaseManager.connectExposed(dataSource)
          var attempts = 0

          shouldThrowAny {
            transaction(database) {
              attempts++
              exec("SELECT * FROM table_that_does_not_exist")
            }
          }

          attempts shouldBe 1
        }
    }
  })
