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
import common.Factory
import extension.app.DataPersistenceTestExtension
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.extension.ExtendWith
import pl.spcode.navauth.common.domain.credentials.UserCredentialsRepository

@ExtendWith(DataPersistenceTestExtension::class)
class UserCredentialsPersistenceTests {

  @Inject private lateinit var userCredentialsRepository: UserCredentialsRepository

  @Test
  fun `find user credentials by user`() {
    val user = Factory.createNonPremiumUser()
    val credentials = Factory.createUserCredentials(user)

    userCredentialsRepository.save(credentials)
    val savedCredentials = userCredentialsRepository.findByUser(user)!!

    assertEquals(user.uuid, savedCredentials.userUuid)
    assertEquals(credentials.hashedPassword, savedCredentials.hashedPassword)
    assertEquals(credentials.totpSecret, savedCredentials.totpSecret)
  }
}
