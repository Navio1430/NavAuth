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

package integration.migrate

import com.google.inject.Inject
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import extension.app.DataPersistenceTestExtension
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.test.assertEquals
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import pl.spcode.navauth.common.config.MigrationConfig
import pl.spcode.navauth.common.domain.credentials.HashingAlgorithm
import pl.spcode.navauth.common.domain.credentials.TOTPSecret
import pl.spcode.navauth.common.domain.credentials.UserCredentials
import pl.spcode.navauth.common.domain.credentials.UserCredentialsRepository
import pl.spcode.navauth.common.domain.user.MojangId
import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.common.domain.user.UserRepository
import pl.spcode.navauth.common.domain.user.UserUuid
import pl.spcode.navauth.common.domain.user.Username
import pl.spcode.navauth.common.infra.crypto.HashedPassword
import pl.spcode.navauth.common.infra.crypto.PasswordHash
import pl.spcode.navauth.common.infra.database.DatabaseDriverType
import pl.spcode.navauth.common.infra.database.DatabaseManager
import pl.spcode.navauth.common.infra.database.SchemaMigrations
import pl.spcode.navauth.common.infra.persistence.exposed.CredentialsTable
import pl.spcode.navauth.common.infra.persistence.exposed.UsersTable
import pl.spcode.navauth.common.infra.persistence.impl.credentials.UserCredentialsRepositoryImpl
import pl.spcode.navauth.common.infra.persistence.impl.user.UserRepositoryImpl
import pl.spcode.navauth.common.migrate.migrator.navauth.NavAuthDatabaseMigrator
import pl.spcode.navauth.common.shared.PluginDirectory
import utils.generateRandomString

/**
 * Database type migration (`NAVAUTH` origin): every user and credentials row is copied from the
 * database in `migration.yml` (a SQLite file here) to the one in `general.yml` (H2 here).
 */
@ExtendWith(DataPersistenceTestExtension::class)
class NavAuthDatabaseMigratorTests {

  @Inject private lateinit var users: UserRepository
  @Inject private lateinit var credentials: UserCredentialsRepository
  @Inject private lateinit var databaseManager: DatabaseManager

  private fun name() = generateRandomString(12, withNumbers = false)

  private class Source(
    val file: Path,
    val users: List<User>,
    val credentials: List<UserCredentials>,
  )

  /** A migrated NavAuth SQLite database with the given accounts. */
  private fun sourceDatabase(accounts: List<Pair<User, UserCredentials?>>): Source {
    val file = Files.createTempFile("navauth-source", ".db")
    HikariDataSource(
        HikariConfig().apply {
          jdbcUrl = "jdbc:sqlite:$file"
          driverClassName = DatabaseDriverType.SQLITE.driverClassName
          maximumPoolSize = 1
        }
      )
      .use { dataSource ->
        SchemaMigrations.migrate(dataSource, DatabaseDriverType.SQLITE)
        val database = DatabaseManager.connectExposed(dataSource)
        val sourceUsers = UserRepositoryImpl(database)
        val sourceCredentials = UserCredentialsRepositoryImpl(database)
        accounts.forEach { (user, userCredentials) ->
          sourceUsers.save(user)
          userCredentials?.let { sourceCredentials.save(it) }
        }
      }
    return Source(file, accounts.map { it.first }, accounts.mapNotNull { it.second })
  }

  private fun migrate(source: Source) {
    val migrator =
      NavAuthDatabaseMigrator(
        MigrationConfig().apply {
          sourceDatabaseConfig = SqliteSourceConfig(source.file)
          // smaller than the number of rows, so the copy runs in several chunks
          chunkSize = 2
        },
        databaseManager,
        PluginDirectory(source.file.parent),
      )
    migrator.init()
    try {
      migrator.migrateAll()
    } finally {
      migrator.sourceDatabaseManager.closeConnections()
    }
  }

  private fun targetRowCounts(source: Source): Pair<Long, Long> {
    val uuids = source.users.map { it.uuid.value }
    return transaction(databaseManager.database) {
      UsersTable.selectAll().where { UsersTable.uuid inList uuids }.count() to
        CredentialsTable.selectAll().where { CredentialsTable.uuid inList uuids }.count()
    }
  }

  /**
   * Rows are copied in uuid order, two per chunk. The order puts a NULL (Mojang UUID, password, 2FA
   * secret) in the first row of a chunk and a value in the next one.
   */
  private fun accounts(): List<Pair<User, UserCredentials?>> {
    val password = HashedPassword(PasswordHash("\$2a\$10\$hash"), HashingAlgorithm.BCRYPT)
    val secret = TOTPSecret("JBSWY3DPEHPK3PXP")
    val prefix = UUID.randomUUID().toString().take(8)
    fun uuid(i: Int) = UUID.fromString("$prefix-0000-0000-0000-00000000000$i")

    val premiumWithTwoFactor =
      User.premium(UserUuid(uuid(1)), Username(name()), MojangId(UUID.randomUUID()), true)
    val cracked = User.nonPremium(UserUuid(uuid(2)), Username(name()))
    val anotherCracked = User.nonPremium(UserUuid(uuid(3)), Username(name()))
    val premiumId = MojangId(UUID.randomUUID())
    val premium = User.premium(UserUuid(uuid(4)), Username(name()), premiumId)
    val crackedWithTwoFactor = User.nonPremium(UserUuid(uuid(5)), Username(name()))

    return listOf(
      premiumWithTwoFactor to UserCredentials.create(premiumWithTwoFactor, null, secret),
      cracked to UserCredentials.create(cracked, password, null),
      anotherCracked to UserCredentials.create(anotherCracked, password, null),
      premium to null,
      crackedWithTwoFactor to UserCredentials.create(crackedWithTwoFactor, password, secret),
    )
  }

  @Test
  fun `copies every user and credentials row from SQLite to H2`() {
    val source = sourceDatabase(accounts())

    migrate(source)

    source.users.forEach { user -> assertEquals(user, users.findByUserUuid(user.uuid)) }
    source.credentials.forEach { userCredentials ->
      val user = users.findByUserUuid(userCredentials.userUuid)!!
      assertEquals(userCredentials, credentials.findByUser(user))
    }
    val premium = source.users.single { it.isPremium && !it.credentialsRequired }
    assertEquals(null, credentials.findByUser(premium))
    assertEquals(5L to 4L, targetRowCounts(source))
  }

  @Test
  fun `a second run does not duplicate or change rows`() {
    val source = sourceDatabase(accounts())

    migrate(source)
    migrate(source)

    assertEquals(5L to 4L, targetRowCounts(source))
    source.users.forEach { user -> assertEquals(user, users.findByUserUuid(user.uuid)) }
  }
}
