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

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import pl.spcode.navauth.common.domain.credentials.UserCredentialsRepository
import pl.spcode.navauth.common.domain.user.UserRepository
import pl.spcode.navauth.common.infra.database.DatabaseManager
import pl.spcode.navauth.common.infra.database.SchemaMigrations
import pl.spcode.navauth.common.infra.persistence.exposed.CredentialsTable
import pl.spcode.navauth.common.infra.persistence.exposed.UsersTable
import pl.spcode.navauth.common.infra.persistence.impl.credentials.UserCredentialsRepositoryImpl
import pl.spcode.navauth.common.infra.persistence.impl.user.UserRepositoryImpl

/**
 * Shared harness for repository integration tests. It owns the backend container/DataSource, runs
 * the real Flyway migrations ([SchemaMigrations]) and exposes the real Exposed repository
 * implementations. Repository-specific assertions live in subclasses (e.g. [UserRepositoryTests]).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractRepositoryIntegrationTest {

  protected abstract val backend: TestBackend

  private lateinit var testDatabase: TestDatabase
  protected lateinit var database: Database

  protected lateinit var users: UserRepository
  protected lateinit var credentials: UserCredentialsRepository

  @BeforeAll
  fun setUp() {
    testDatabase = TestDatabase(backend)
    val dataSource = testDatabase.dataSource
    SchemaMigrations.migrate(dataSource, backend.driverType)

    database = DatabaseManager.connectExposed(dataSource)

    users = UserRepositoryImpl(database)
    credentials = UserCredentialsRepositoryImpl(database)
  }

  @AfterAll
  fun tearDown() {
    testDatabase.close()
  }

  @BeforeEach
  fun clean() {
    transaction(database) {
      CredentialsTable.deleteAll()
      UsersTable.deleteAll()
    }
  }
}
