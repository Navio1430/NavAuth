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

package integration.migrate

import com.google.inject.Inject
import extension.app.DataPersistenceTestExtension
import io.mockk.every
import io.mockk.mockk
import java.nio.file.Files
import java.nio.file.Path
import java.sql.DriverManager
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import pl.spcode.navauth.common.application.validator.UsernameValidator
import pl.spcode.navauth.common.config.MigrationConfig
import pl.spcode.navauth.common.config.UsernamesConfig
import pl.spcode.navauth.common.domain.credentials.HashingAlgorithm
import pl.spcode.navauth.common.domain.credentials.UserCredentialsRepository
import pl.spcode.navauth.common.domain.user.MojangId
import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.common.domain.user.UserRepository
import pl.spcode.navauth.common.domain.user.UserUuid
import pl.spcode.navauth.common.domain.user.Username
import pl.spcode.navauth.common.infra.crypto.hasher.BCryptCredentialsHasher
import pl.spcode.navauth.common.infra.database.DatabaseManager
import pl.spcode.navauth.common.infra.persistence.impl.common.TransactionServiceImpl
import pl.spcode.navauth.common.migrate.MigrationManager
import pl.spcode.navauth.common.migrate.MigrationOriginPluginType
import pl.spcode.navauth.common.migrate.MigratorFactory
import pl.spcode.navauth.common.migrate.migrator.MigrationChunkResult
import pl.spcode.navauth.common.migrate.migrator.librelogin.LibreLoginMigrator
import pl.spcode.navauth.common.shared.PluginDirectory
import utils.generateRandomString

/**
 * Runs the LibreLogin migrator against a real `librepremium_data` table (SQLite source, H2 target),
 * including a BCrypt password that must still verify after the import, issue #46 and damaged rows
 * that must be skipped without stopping the migration.
 */
@ExtendWith(DataPersistenceTestExtension::class)
class LibreLoginMigratorTests {

  @Inject private lateinit var users: UserRepository
  @Inject private lateinit var credentials: UserCredentialsRepository
  @Inject private lateinit var databaseManager: DatabaseManager

  private data class Row(
    val uuid: String?,
    val nickname: String?,
    val premiumUuid: String? = null,
    val hashedPassword: String? = null,
    val salt: String? = null,
    val algo: String? = null,
    val secret: String? = null,
  ) {
    constructor(
      uuid: UUID,
      nickname: String,
      premiumUuid: UUID? = null,
      hashedPassword: String? = null,
      salt: String? = null,
      algo: String? = null,
      secret: String? = null,
    ) : this(
      uuid.toString(),
      nickname,
      premiumUuid?.toString(),
      hashedPassword,
      salt,
      algo,
      secret,
    )

    val userUuid: UserUuid
      get() = UserUuid(UUID.fromString(uuid))
  }

  /** Damaged rows are possible in real data, so `uuid` and `last_nickname` accept NULL here. */
  private fun sourceDatabase(rows: List<Row>): Path {
    val file = Files.createTempFile("librelogin", ".db")
    DriverManager.getConnection("jdbc:sqlite:$file").use { conn ->
      conn.createStatement().use {
        it.execute(
          "CREATE TABLE librepremium_data (uuid VARCHAR(255) PRIMARY KEY, " +
            "premium_uuid VARCHAR(255), hashed_password VARCHAR(255), salt VARCHAR(255), " +
            "algo VARCHAR(255), last_nickname VARCHAR(255), secret VARCHAR(255))"
        )
      }
      rows.forEach { row ->
        conn.prepareStatement("INSERT INTO librepremium_data VALUES (?, ?, ?, ?, ?, ?, ?)").use {
          it.setString(1, row.uuid)
          it.setString(2, row.premiumUuid)
          it.setString(3, row.hashedPassword)
          it.setString(4, row.salt)
          it.setString(5, row.algo)
          it.setString(6, row.nickname)
          it.setString(7, row.secret)
          it.executeUpdate()
        }
      }
    }
    return file
  }

  private fun migrator(source: Path) =
    LibreLoginMigrator(
      users,
      credentials,
      UsernameValidator(UsernamesConfig()),
      TransactionServiceImpl(databaseManager),
      MigrationConfig().apply { sourceDatabaseConfig = SqliteSourceConfig(source) },
      PluginDirectory(source.parent),
    )

  /** Migrates every row in one chunk. */
  private fun migrate(source: Path): MigrationChunkResult {
    val migrator = migrator(source)
    migrator.init()
    return migrator.migrateNext(0, migrator.getSourceUsersCount())
  }

  private fun name() = generateRandomString(12, withNumbers = false)

  /** Splits a full BCrypt hash the way LibreLogin stores it: `cost$hash`, salt and algo apart. */
  private fun libreLoginBCrypt(password: String): Triple<String, String, String> {
    val full = BCryptCredentialsHasher().hash(password).passwordHash.value // $2a$10$<salt><hash>
    val salt = full.substring(7, 29)
    val hash = full.substring(29)
    return Triple("10$$hash", salt, "BCrypt-2A")
  }

  @Test
  fun `imports cracked, premium and 2FA players with their LibreLogin uuids`() {
    val (hash, salt, algo) = libreLoginBCrypt("Secret123")
    val cracked = Row(UUID.randomUUID(), name(), null, hash, salt, algo)
    val premiumWithPassword = Row(UUID.randomUUID(), name(), UUID.randomUUID(), hash, salt, algo)
    val premiumWithTwoFactor =
      Row(UUID.randomUUID(), name(), UUID.randomUUID(), hash, salt, algo, "JBSWY3DPEHPK3PXP")
    val noCredentials = Row(UUID.randomUUID(), name())

    val result =
      migrate(
        sourceDatabase(listOf(cracked, premiumWithPassword, premiumWithTwoFactor, noCredentials))
      )

    assertEquals(MigrationChunkResult(read = 4, migrated = 3, skipped = 1), result)

    val crackedUser = assertNotNull(users.findByUserUuid(cracked.userUuid))
    assertFalse(crackedUser.isPremium)
    val crackedCredentials = assertNotNull(credentials.findByUser(crackedUser))
    assertEquals(HashingAlgorithm.BCRYPT, crackedCredentials.hashedPassword!!.algo)
    assertTrue(
      BCryptCredentialsHasher()
        .verify("Secret123", crackedCredentials.hashedPassword!!.passwordHash)
    )

    // issue #46: a premium player is not asked for the password LibreLogin kept
    val premium = assertNotNull(users.findByUserUuid(premiumWithPassword.userUuid))
    assertEquals(MojangId(UUID.fromString(premiumWithPassword.premiumUuid)), premium.mojangUuid)
    assertFalse(premium.credentialsRequired)
    assertNull(credentials.findByUser(premium))

    val twoFactor = assertNotNull(users.findByUserUuid(premiumWithTwoFactor.userUuid))
    assertTrue(twoFactor.credentialsRequired)
    val twoFactorCredentials = assertNotNull(credentials.findByUser(twoFactor))
    assertNull(twoFactorCredentials.hashedPassword)
    assertTrue(twoFactorCredentials.isTwoFactorEnabled)

    // a cracked row without password and 2FA cannot log in and is skipped
    assertNull(users.findByUserUuid(noCredentials.userUuid))
  }

  @Test
  fun `a name that already exists in NavAuth in any letter case is skipped`() {
    val existing = User.nonPremium(UserUuid(UUID.randomUUID()), Username(name()))
    users.save(existing)
    val (hash, salt, algo) = libreLoginBCrypt("Secret123")
    val duplicate =
      Row(UUID.randomUUID(), existing.username.value.uppercase(), null, hash, salt, algo)

    val result = migrate(sourceDatabase(listOf(duplicate)))

    assertEquals(MigrationChunkResult(read = 1, migrated = 0, skipped = 1), result)
    assertNull(users.findByUserUuid(duplicate.userUuid))
    assertEquals(existing.uuid, users.findByUsernameIgnoreCase(existing.username.value)?.uuid)
  }

  @Test
  fun `a uuid or Mojang UUID that already exists in NavAuth is skipped, the account untouched`() {
    val existingCracked = User.nonPremium(UserUuid(UUID.randomUUID()), Username(name()))
    val mojangId = MojangId(UUID.randomUUID())
    val existingPremium = User.premium(UserUuid(mojangId.value), Username(name()), mojangId)
    users.save(existingCracked)
    users.save(existingPremium)
    val (hash, salt, algo) = libreLoginBCrypt("Secret123")
    val sameUuid = Row(existingCracked.uuid.value, name(), null, hash, salt, algo)
    val sameMojangUuid = Row(UUID.randomUUID(), name(), mojangId.value)

    val result = migrate(sourceDatabase(listOf(sameUuid, sameMojangUuid)))

    assertEquals(MigrationChunkResult(read = 2, migrated = 0, skipped = 2), result)
    assertEquals(existingCracked, users.findByUserUuid(existingCracked.uuid))
    assertNull(credentials.findByUser(existingCracked))
    assertNull(users.findByUserUuid(sameMojangUuid.userUuid))
    assertEquals(existingPremium, users.findByMojangUuid(mojangId))
  }

  @Test
  fun `damaged rows are skipped and counted, the valid rows next to them are imported`() {
    val (hash, salt, algo) = libreLoginBCrypt("Secret123")
    val valid = Row(UUID.randomUUID(), name(), null, hash, salt, algo)
    val validPremium = Row(UUID.randomUUID(), name(), UUID.randomUUID())
    // a premium player's password is dropped, so its algorithm does not matter
    val premiumWithUnknownAlgorithm =
      Row(UUID.randomUUID(), name(), UUID.randomUUID(), "hash", "salt", "MD5-UNKNOWN")
    val damaged =
      listOf(
        Row(UUID.randomUUID().toString(), null, hashedPassword = hash, salt = salt, algo = algo),
        Row(UUID.randomUUID(), "", null, hash, salt, algo),
        Row(UUID.randomUUID(), "bad name!", null, hash, salt, algo),
        Row(null, name(), hashedPassword = hash, salt = salt, algo = algo),
        Row("not-a-uuid", name(), hashedPassword = hash, salt = salt, algo = algo),
        Row(UUID.randomUUID().toString(), name(), premiumUuid = "not-a-uuid"),
        Row(UUID.randomUUID(), name(), null, hash, salt, "MD5-UNKNOWN"),
        Row(UUID.randomUUID(), name(), null, "no-cost-separator", salt, algo),
        Row(UUID.randomUUID(), name(), null, hash, salt, null),
        Row(UUID.randomUUID(), name(), null, hash, null, algo),
        Row(UUID.randomUUID(), name(), UUID.randomUUID(), secret = "too-short"),
      )

    val result =
      migrate(sourceDatabase(listOf(valid, validPremium, premiumWithUnknownAlgorithm) + damaged))

    assertEquals(MigrationChunkResult(read = 14, migrated = 3, skipped = 11), result)
    assertNotNull(users.findByUserUuid(valid.userUuid))
    assertNotNull(users.findByUserUuid(validPremium.userUuid))
    val premium = assertNotNull(users.findByUserUuid(premiumWithUnknownAlgorithm.userUuid))
    assertFalse(premium.credentialsRequired)
    assertNull(credentials.findByUser(premium))
    damaged
      .mapNotNull { row -> row.uuid?.let { runCatching { UUID.fromString(it) }.getOrNull() } }
      .forEach { assertNull(users.findByUserUuid(UserUuid(it))) }
  }

  @Test
  fun `chunks advance by the rows read, so a chunk of skipped rows does not end the migration`() {
    val (hash, salt, algo) = libreLoginBCrypt("Secret123")
    // ordered by uuid: the first chunk holds only damaged rows
    val damaged =
      listOf(
        Row("00000000-0000-0000-0000-000000000001", "bad name!", null, hash, salt, algo),
        Row("00000000-0000-0000-0000-000000000002", null, null, hash, salt, algo),
      )
    val valid =
      (3..7).map { i ->
        Row("ffffffff-0000-0000-0000-00000000000$i", name(), null, hash, salt, algo)
      }
    val source = sourceDatabase(damaged + valid)
    val migrationConfig =
      MigrationConfig().apply {
        sourceDatabaseConfig = SqliteSourceConfig(source)
        originPluginType = MigrationOriginPluginType.LIBRELOGIN
        chunkSize = 2
      }
    val migratorFactory = mockk<MigratorFactory>()
    every { migratorFactory.getMigrator(MigrationOriginPluginType.LIBRELOGIN) } returns
      migrator(source)

    MigrationManager(migrationConfig, migratorFactory).startMigration()

    valid.forEach { assertNotNull(users.findByUserUuid(it.userUuid)) }
  }
}
