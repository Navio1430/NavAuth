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

package integration.service

import com.google.inject.Inject
import extension.app.ApplicationTestExtension
import extension.retry.Retry
import extension.retry.RetryExtension
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.extension.ExtendWith
import pl.spcode.navauth.common.application.mojang.MojangProfileService
import pl.spcode.navauth.common.domain.user.MojangId
import pl.spcode.navauth.common.domain.user.Username
import utils.generateRandomString

@ExtendWith(ApplicationTestExtension::class)
class MojangProfileServiceTests {

  @Inject lateinit var mojangProfileService: MojangProfileService

  @Test
  fun `test mojang profile fetch`() {
    val profile = mojangProfileService.fetchProfileInfo(Username("notch"))

    assertNotNull(profile)
    assertEquals(profile.name, Username("Notch"))
    assertEquals(profile.uuid, MojangId(UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5")))
  }

  @ExtendWith(RetryExtension::class)
  @Retry(value = 2)
  @Test
  fun `test fetched mojang profile doesn't exists`() {
    val profile = mojangProfileService.fetchProfileInfo(Username(generateRandomString(15)))

    assertNull(profile)
  }
}
