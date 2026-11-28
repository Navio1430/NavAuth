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

import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.Test
import pl.spcode.navauth.common.domain.credentials.HashingAlgorithm
import pl.spcode.navauth.common.domain.credentials.TOTPSecret
import pl.spcode.navauth.common.domain.credentials.UserCredentials
import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.common.domain.user.UserUuid
import pl.spcode.navauth.common.domain.user.Username
import pl.spcode.navauth.common.infra.crypto.HashedPassword
import pl.spcode.navauth.common.infra.crypto.PasswordHash

abstract class UserCredentialsRepositoryTests : AbstractRepositoryIntegrationTest() {

  private fun newUser(name: String): User =
    User.nonPremium(UserUuid(UUID.randomUUID()), Username(name)).also { users.save(it) }

  @Test
  fun `save credentials and read hashing algorithm`() {
    val user = newUser("CredsUser")

    credentials.save(
      UserCredentials.create(
        user = user,
        password = HashedPassword(PasswordHash("hashed-password"), HashingAlgorithm.ARGON2),
        totpSecret = TOTPSecret("0123456789abcdef"),
      )
    )

    val found = credentials.findByUser(user)
    assertNotNull(found)
    assertEquals(HashingAlgorithm.ARGON2, found.hashedPassword?.algo)
    assertEquals("hashed-password", found.hashedPassword?.passwordHash?.value)
    assertEquals("0123456789abcdef", found.totpSecret?.value)
  }

  @Test
  fun `save credentials without password keeps only TOTP secret`() {
    val user = newUser("TotpOnly")

    credentials.save(
      UserCredentials.create(
        user = user,
        password = null,
        totpSecret = TOTPSecret("fedcba9876543210"),
      )
    )

    val found = credentials.findByUser(user)
    assertNotNull(found)
    assertNull(found.hashedPassword)
    assertEquals("fedcba9876543210", found.totpSecret?.value)
  }

  @Test
  fun `find credentials for user without credentials returns null`() {
    val user = newUser("NoCreds")
    assertNull(credentials.findByUser(user))
  }

  @Test
  fun `delete credentials by user`() {
    val user = newUser("DeleteCreds")
    credentials.save(
      UserCredentials.create(
        user = user,
        password = HashedPassword(PasswordHash("hash"), HashingAlgorithm.BCRYPT),
        totpSecret = null,
      )
    )

    credentials.deleteByUser(user)

    assertNull(credentials.findByUser(user))
  }
}
