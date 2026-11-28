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

import com.zaxxer.hikari.HikariDataSource
import java.nio.file.Files
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import pl.spcode.navauth.common.domain.credentials.HashingAlgorithm
import pl.spcode.navauth.common.domain.user.MojangId
import pl.spcode.navauth.common.domain.user.UserUuid
import pl.spcode.navauth.common.infra.database.DatabaseDriverType
import pl.spcode.navauth.common.infra.database.DatabaseManager
import pl.spcode.navauth.common.infra.database.SchemaMigrations
import pl.spcode.navauth.common.infra.persistence.impl.credentials.UserCredentialsRepositoryImpl
import pl.spcode.navauth.common.infra.persistence.impl.user.UserRepositoryImpl

/**
 * OrmLite created its H2 tables and columns with upper-case names (`NAVAUTH_USERS.PASSWORDHASH`).
 * An H2 file from before 0.4.0 must stay readable through the production JDBC URL.
 */
class H2OrmLiteUpgradeTests {

  @Test
  fun `OrmLite H2 file with upper-case names is baselined and every row stays readable`() {
    val file = Files.createTempDirectory("navauth-h2-ormlite").resolve("legacy").toAbsolutePath()
    HikariDataSource()
      .apply {
        driverClassName = DatabaseDriverType.H2_FILE.driverClassName
        jdbcUrl = DatabaseDriverType.H2_FILE.jdbcUrlFormat.format(file.toString())
        username = "root"
        password = "password"
      }
      .use { dataSource ->
        val crackedUuid = UUID.randomUUID()
        val premiumUuid = UUID.randomUUID()
        val mojangUuid = UUID.randomUUID()
        dataSource.connection.use { conn ->
          conn.createStatement().use { stmt ->
            // the DDL OrmLite 6.1 generated on H2 (unquoted, so stored upper case)
            stmt.execute(
              "CREATE TABLE NAVAUTH_USERS (UUID VARCHAR(48) NOT NULL, MOJANG_UUID VARCHAR(48), " +
                "USERNAME VARCHAR(255) NOT NULL, USERNAME_LOWERCASE VARCHAR(255) NOT NULL, " +
                "CREDENTIALS_REQUIRED BOOLEAN NOT NULL, PRIMARY KEY (UUID))"
            )
            stmt.execute(
              "CREATE TABLE NAVAUTH_CREDENTIALS (UUID VARCHAR(48) NOT NULL, " +
                "PASSWORDHASH VARCHAR(255), ALGO VARCHAR(100), TWOFACTORSECRET VARCHAR(255), " +
                "PRIMARY KEY (UUID))"
            )
            stmt.execute(
              "CREATE TABLE NAVAUTH_USERS_SESSIONS (UUID VARCHAR(48) NOT NULL, " +
                "JOINED_AT BIGINT NOT NULL, LEFT_AT BIGINT NOT NULL, IP BIGINT)"
            )
            stmt.execute(
              "INSERT INTO NAVAUTH_USERS VALUES " +
                "('$crackedUuid', NULL, 'Cracked', 'cracked', TRUE), " +
                "('$premiumUuid', '$mojangUuid', 'Premium', 'premium', FALSE)"
            )
            stmt.execute(
              "INSERT INTO NAVAUTH_CREDENTIALS VALUES " +
                "('$crackedUuid', '\$2a\$10\$legacyhash', 'BCRYPT', 'JBSWY3DPEHPK3PXP')"
            )
            stmt.execute("INSERT INTO NAVAUTH_USERS_SESSIONS VALUES ('$crackedUuid', 1, 2, NULL)")
          }
        }

        SchemaMigrations.migrate(dataSource, DatabaseDriverType.H2_FILE)

        assertEquals(
          1,
          SchemaMigrations.schemaVersion(dataSource, DatabaseDriverType.H2_FILE)?.toInt(),
        )
        val database = DatabaseManager.connectExposed(dataSource)
        val users = UserRepositoryImpl(database)
        val cracked = assertNotNull(users.findByUsernameIgnoreCase("CRACKED"))
        assertEquals(UserUuid(crackedUuid), cracked.uuid)
        assertTrue(cracked.credentialsRequired)
        val premium = assertNotNull(users.findByMojangUuid(MojangId(mojangUuid)))
        assertEquals(UserUuid(premiumUuid), premium.uuid)
        val credentials = assertNotNull(UserCredentialsRepositoryImpl(database).findByUser(cracked))
        assertEquals(HashingAlgorithm.BCRYPT, credentials.hashedPassword!!.algo)
        assertTrue(credentials.isTwoFactorEnabled)
        dataSource.connection.use { conn ->
          conn.createStatement().use { stmt ->
            stmt.executeQuery("SELECT COUNT(*) FROM NAVAUTH_USERS_SESSIONS").use { rs ->
              rs.next()
              assertEquals(1, rs.getInt(1))
            }
          }
        }
      }
  }
}
