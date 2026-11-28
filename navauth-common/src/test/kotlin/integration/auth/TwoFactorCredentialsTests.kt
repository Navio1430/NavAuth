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

package integration.auth

import com.google.inject.Inject
import common.Factory
import extension.app.UsernameResolutionTestExtension
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import pl.spcode.navauth.common.application.credentials.UserCredentialsService
import pl.spcode.navauth.common.application.user.UserService
import pl.spcode.navauth.common.domain.credentials.TOTPSecret
import pl.spcode.navauth.common.domain.credentials.UserCredentials
import pl.spcode.navauth.common.domain.credentials.UserCredentialsRepository
import pl.spcode.navauth.common.domain.user.UserRepository
import pl.spcode.navauth.common.infra.crypto.TOTP2FA

/** Credentials of cracked accounts that have only a 2FA secret, or no credentials row at all. */
@ExtendWith(UsernameResolutionTestExtension::class)
class TwoFactorCredentialsTests {

  @Inject private lateinit var userService: UserService
  @Inject private lateinit var credentialsService: UserCredentialsService
  @Inject private lateinit var userRepository: UserRepository
  @Inject private lateinit var credentialsRepository: UserCredentialsRepository

  @Test
  fun `cracked account with only 2FA cannot disable it and keeps its credentials`() {
    val user = Factory.createNonPremiumUser()
    userRepository.save(user)
    val secret: TOTPSecret = TOTP2FA().generateSecret()
    credentialsRepository.save(UserCredentials.create(user, null, secret))

    assertFailsWith<IllegalStateException> { userService.disableTwoFactorAuth(user) }

    val credentials = assertNotNull(credentialsRepository.findByUser(user))
    assertEquals(secret, credentials.totpSecret)
    assertEquals(true, assertNotNull(userService.findUserByUuid(user.uuid)).credentialsRequired)
  }

  @Test
  fun `forcesetpassword creates credentials for a cracked account without them`() {
    val user = Factory.createNonPremiumUser()
    userRepository.save(user)
    assertNull(credentialsRepository.findByUser(user))

    credentialsService.updatePassword(user, "newpassword1").get(30, TimeUnit.SECONDS)

    val credentials = assertNotNull(credentialsRepository.findByUser(user))
    assertNotNull(credentials.hashedPassword)
    assertNull(credentials.totpSecret)
    val verified =
      credentialsService
        .enqueueVerifyPassword(credentials, "newpassword1", user.uuid.value)
        .get(30, TimeUnit.SECONDS)
    assertEquals(true, verified)
  }
}
