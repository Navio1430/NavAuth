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

package integration.data

import com.google.inject.Inject
import extension.app.DataPersistenceTestExtension
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.extension.ExtendWith
import pl.spcode.navauth.common.domain.credentials.HashingAlgorithm
import pl.spcode.navauth.common.domain.credentials.UserCredentials
import pl.spcode.navauth.common.domain.credentials.UserCredentialsRepository
import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.common.domain.user.UserId
import pl.spcode.navauth.common.domain.user.Username
import pl.spcode.navauth.common.infra.crypto.HashedPassword
import pl.spcode.navauth.common.infra.crypto.PasswordHash
import utils.generateRandomString

@ExtendWith(DataPersistenceTestExtension::class)
class UserCredentialsPersistenceTests {

  @Inject private lateinit var userCredentialsRepository: UserCredentialsRepository

  @Test
  fun `create user credentials from user and hashed password`() {
    val id = UserId(UUID.randomUUID())
    val username = Username(generateRandomString(10))
    val user = User.nonPremium(id, username)
    val hashedPassword = HashedPassword(PasswordHash("hashed_pw"), HashingAlgorithm.BCRYPT)

    val credentials = UserCredentials.create(user, hashedPassword)
    userCredentialsRepository.save(credentials)
    val savedCredentials = userCredentialsRepository.findByUser(user)!!

    assertEquals(id, savedCredentials.userId)
    assertEquals(hashedPassword.passwordHash, savedCredentials.passwordHash)
    assertEquals(hashedPassword.algo, savedCredentials.hashingAlgo)
  }
}
