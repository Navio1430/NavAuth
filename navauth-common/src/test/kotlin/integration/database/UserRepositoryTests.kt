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
import pl.spcode.navauth.common.domain.user.MojangId
import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.common.domain.user.UserUuid
import pl.spcode.navauth.common.domain.user.Username

abstract class UserRepositoryTests : AbstractRepositoryIntegrationTest() {

  @Test
  fun `save then find user by uuid`() {
    val user =
      User.premium(
        id = UserUuid(UUID.randomUUID()),
        username = Username("Alice"),
        mojangUuid = MojangId(UUID.randomUUID()),
        requiresCredentials = true,
      )

    users.save(user)

    val found = users.findByUserUuid(user.uuid)
    assertNotNull(found)
    assertEquals(user, found)
  }

  @Test
  fun `find user by case-insensitive username`() {
    val user = User.nonPremium(UserUuid(UUID.randomUUID()), Username("MixedCase"))

    users.save(user)

    val found = users.findByUsernameIgnoreCase("mixedcase")
    assertNotNull(found)
    assertEquals(user.uuid, found.uuid)
  }

  @Test
  fun `find non-existent user by username returns null`() {
    assertNull(users.findByExactUsername("nobody"))
  }

  @Test
  fun `find premium user by mojang uuid`() {
    val mojangId = MojangId(UUID.randomUUID())
    val user =
      User.premium(
        UserUuid(UUID.randomUUID()),
        Username("Notch"),
        mojangId,
        requiresCredentials = false,
      )

    users.save(user)

    val found = users.findByMojangUuid(mojangId)
    assertNotNull(found)
    assertEquals(user.uuid, found.uuid)
  }

  @Test
  fun `save is an upsert and does not duplicate`() {
    val id = UserUuid(UUID.randomUUID())
    users.save(User.nonPremium(id, Username("First")))
    users.save(User.premium(id, Username("Second"), MojangId(UUID.randomUUID())))

    val found = users.findByUserUuid(id)
    assertNotNull(found)
    assertEquals(Username("Second"), found.username)
    assertEquals(true, found.isPremium)
  }

  @Test
  fun `duplicate usernames resolve to the lowest uuid in any letter case`() {
    val low = UserUuid(UUID.fromString("00000000-0000-0000-0000-000000000001"))
    val high = UserUuid(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"))
    users.save(User.nonPremium(high, Username("Twin")))
    users.save(User.nonPremium(low, Username("TWIN")))

    assertEquals(low, users.findByUsernameIgnoreCase("twin")?.uuid)
    assertEquals(high, users.findByExactUsername("Twin")?.uuid)
  }

  @Test
  fun `duplicate mojang uuids resolve to the lowest uuid`() {
    val mojangId = MojangId(UUID.randomUUID())
    val low = UserUuid(UUID.fromString("00000000-0000-0000-0000-000000000002"))
    val high = UserUuid(UUID.fromString("ffffffff-ffff-ffff-ffff-fffffffffffe"))
    users.save(User.premium(high, Username("Second"), mojangId))
    users.save(User.premium(low, Username("First"), mojangId))

    assertEquals(low, users.findByMojangUuid(mojangId)?.uuid)
  }

  @Test
  fun `cracked accounts keep a null mojang uuid`() {
    val user = User.nonPremium(UserUuid(UUID.randomUUID()), Username("NoMojang"))
    users.save(user)

    assertNull(users.findByUserUuid(user.uuid)?.mojangUuid)
  }
}
