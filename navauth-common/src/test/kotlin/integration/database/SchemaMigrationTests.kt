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

package integration.database

import java.sql.Connection
import java.sql.Types
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import pl.spcode.navauth.common.domain.credentials.HashingAlgorithm
import pl.spcode.navauth.common.domain.user.MojangId
import pl.spcode.navauth.common.domain.user.UserUuid
import pl.spcode.navauth.common.domain.user.Username
import pl.spcode.navauth.common.infra.database.DatabaseManager
import pl.spcode.navauth.common.infra.database.SchemaMigrations
import pl.spcode.navauth.common.infra.persistence.impl.credentials.UserCredentialsRepositoryImpl
import pl.spcode.navauth.common.infra.persistence.impl.user.UserRepositoryImpl

/**
 * Installs and upgrades: an empty database, a database shared with other plugins, and an install
 * created by OrmLite before Flyway (with data). Every test starts from an empty schema.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class SchemaMigrationTests {

  protected abstract val backend: TestBackend

  private lateinit var testDatabase: TestDatabase
  private val dataSource
    get() = testDatabase.dataSource

  @BeforeAll
  fun start() {
    testDatabase = TestDatabase(backend)
  }

  @AfterAll
  fun stop() {
    testDatabase.close()
  }

  @BeforeEach
  fun emptySchema() {
    connection { conn ->
      val q = conn.metaData.identifierQuoteString.trim()
      val schema = runCatching { conn.schema }.getOrNull()
      val tables =
        conn.metaData.getTables(conn.catalog, schema, "%", arrayOf("TABLE")).use { rs ->
          buildList { while (rs.next()) add(rs.getString("TABLE_NAME")) }
        }
      tables
        .filterNot { it.startsWith("sqlite_") }
        .forEach { table -> conn.createStatement().use { it.execute("DROP TABLE $q$table$q") } }
    }
  }

  private fun migrate() = SchemaMigrations.migrate(dataSource, backend.driverType)

  /** Flyway keeps the script's spelling (`001`); compare numerically. */
  private fun schemaVersion() =
    SchemaMigrations.schemaVersion(dataSource, backend.driverType)?.toInt()

  private fun tableExists(table: String) = SchemaMigrations.tableExists(dataSource, table)

  private fun <T> connection(block: (Connection) -> T): T = dataSource.connection.use(block)

  /** Runs [sql] with every `"` replaced by the database's identifier quote. */
  private fun execute(sql: String, vararg params: Any?) = connection { conn ->
    val q = conn.metaData.identifierQuoteString.trim()
    conn.prepareStatement(sql.replace("\"", q)).use { stmt ->
      params.forEachIndexed { i, param ->
        when (param) {
          null -> stmt.setNull(i + 1, Types.VARCHAR)
          is Boolean -> stmt.setBoolean(i + 1, param)
          is Long -> stmt.setLong(i + 1, param)
          else -> stmt.setString(i + 1, param.toString())
        }
      }
      stmt.execute()
    }
  }

  private fun countRows(table: String): Long = connection { conn ->
    val q = conn.metaData.identifierQuoteString.trim()
    conn.createStatement().use { stmt ->
      stmt.executeQuery("SELECT COUNT(*) FROM $q$table$q").use { rs ->
        rs.next()
        rs.getLong(1)
      }
    }
  }

  /** The schema OrmLite created (V001 is a capture of it), plus the old session log table. */
  private fun createOrmLiteSchema() {
    val ddl =
      requireNotNull(
          javaClass.classLoader.getResource(
            "migration/${backend.driverType.migrationDir}/V001_initial.sql"
          )
        )
        .readText()
    ddl
      .lines()
      .filterNot { it.trimStart().startsWith("--") }
      .joinToString("\n")
      .split(";")
      .map { it.trim() }
      .filter { it.isNotEmpty() }
      .forEach { statement ->
        connection { conn -> conn.createStatement().use { it.execute(statement) } }
      }
    execute(
      "CREATE TABLE \"navauth_users_sessions\" (\"uuid\" VARCHAR(48) NOT NULL, " +
        "\"joined_at\" BIGINT NOT NULL, \"left_at\" BIGINT NOT NULL, \"ip\" BIGINT)"
    )
  }

  private fun insertLegacyUser(
    uuid: UUID,
    mojangUuid: UUID?,
    username: String,
    credentialsRequired: Boolean,
  ) =
    execute(
      "INSERT INTO \"navauth_users\" (\"uuid\", \"mojang_uuid\", \"username\", " +
        "\"username_lowercase\", \"credentials_required\") VALUES (?, ?, ?, ?, ?)",
      uuid,
      mojangUuid,
      username,
      username.lowercase(),
      credentialsRequired,
    )

  @Test
  fun `fresh install creates the NavAuth tables`() {
    migrate()

    assertTrue(tableExists("navauth_users"))
    assertTrue(tableExists("navauth_credentials"))
    assertFalse(tableExists("navauth_users_sessions"))
    assertEquals(1, schemaVersion())
  }

  @Test
  fun `migrations run when the context class loader cannot see NavAuth`() {
    // Velocity calls plugins with the proxy's class loader as the thread context class loader
    val thread = Thread.currentThread()
    val previous = thread.contextClassLoader
    thread.contextClassLoader = ClassLoader.getPlatformClassLoader()
    try {
      migrate()
    } finally {
      thread.contextClassLoader = previous
    }

    assertTrue(tableExists("navauth_users"))
    assertTrue(tableExists("navauth_credentials"))
    assertEquals(1, schemaVersion())
  }

  @Test
  fun `fresh install into a database shared with other plugins creates the NavAuth tables`() {
    execute("CREATE TABLE \"luckperms_players\" (\"uuid\" VARCHAR(36) NOT NULL)")

    migrate()

    assertTrue(tableExists("navauth_users"))
    assertTrue(tableExists("navauth_credentials"))
    assertTrue(tableExists("luckperms_players"))
    assertEquals(1, schemaVersion())
  }

  @Test
  fun `OrmLite install is baselined and keeps accounts, credentials and the session log`() {
    createOrmLiteSchema()
    val crackedUuid = UUID.randomUUID()
    val premiumUuid = UUID.randomUUID()
    val mojangUuid = UUID.randomUUID()
    insertLegacyUser(crackedUuid, null, "Cracked", credentialsRequired = true)
    insertLegacyUser(premiumUuid, mojangUuid, "Premium", credentialsRequired = false)
    execute(
      "INSERT INTO \"navauth_credentials\" (\"uuid\", \"passwordHash\", \"algo\", " +
        "\"twoFactorSecret\") VALUES (?, ?, ?, ?)",
      crackedUuid,
      "\$2a\$10\$legacyhash",
      "BCRYPT",
      "JBSWY3DPEHPK3PXP",
    )
    execute(
      "INSERT INTO \"navauth_users_sessions\" (\"uuid\", \"joined_at\", \"left_at\", \"ip\") " +
        "VALUES (?, ?, ?, ?)",
      crackedUuid,
      1L,
      2L,
      2130706433L,
    )

    migrate()

    assertEquals(1, schemaVersion())
    val database = DatabaseManager.connectExposed(dataSource)
    val users = UserRepositoryImpl(database)
    val cracked = assertNotNull(users.findByUsernameIgnoreCase("cracked"))
    assertEquals(UserUuid(crackedUuid), cracked.uuid)
    assertFalse(cracked.isPremium)
    assertTrue(cracked.credentialsRequired)
    val premium = assertNotNull(users.findByMojangUuid(MojangId(mojangUuid)))
    assertEquals(UserUuid(premiumUuid), premium.uuid)
    assertEquals(Username("Premium"), premium.username)
    assertFalse(premium.credentialsRequired)
    val credentials = assertNotNull(UserCredentialsRepositoryImpl(database).findByUser(cracked))
    assertEquals(HashingAlgorithm.BCRYPT, credentials.hashedPassword!!.algo)
    assertTrue(credentials.isTwoFactorEnabled)
    // the session log is no longer used, but its data is left in place
    assertEquals(1, countRows("navauth_users_sessions"))
  }

  @Test
  fun `migrating an up-to-date schema again changes nothing`() {
    migrate()
    migrate()

    assertEquals(1, schemaVersion())
  }

  @Test
  fun `OrmLite install without the credentials table stops the startup`() {
    execute(
      "CREATE TABLE \"navauth_users\" (\"uuid\" VARCHAR(48) NOT NULL, \"mojang_uuid\" VARCHAR(48), " +
        "\"username\" VARCHAR(255) NOT NULL, \"username_lowercase\" VARCHAR(255) NOT NULL, " +
        "\"credentials_required\" BOOLEAN NOT NULL, PRIMARY KEY (\"uuid\"))"
    )

    assertFailsWith<IllegalStateException> { migrate() }
  }

  @Test
  fun `schema history without the NavAuth tables stops the startup`() {
    migrate()
    execute("DROP TABLE \"navauth_credentials\"")

    assertFailsWith<IllegalStateException> { migrate() }
  }
}
