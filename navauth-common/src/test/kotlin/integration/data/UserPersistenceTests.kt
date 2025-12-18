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
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import pl.spcode.navauth.common.domain.user.MojangId
import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.common.domain.user.UserId
import pl.spcode.navauth.common.domain.user.UserRepository
import pl.spcode.navauth.common.domain.user.Username
import utils.generateRandomString

@ExtendWith(DataPersistenceTestExtension::class)
class UserPersistenceTests {

  @Inject private lateinit var userRepo: UserRepository

  @Test
  fun `test persisted user exists`() {
    val id = UserId(UUID.randomUUID())
    val name = generateRandomString(10)
    val username = Username(name)
    val userToPersist = User.nonPremium(id, username)
    userRepo.save(userToPersist)

    val user = userRepo.findByUsername(name)

    assertNotNull(user, "Persisted user should exist in repository")
    assertEquals(userToPersist.id, user.id, "User ID should match")
    assertEquals(userToPersist.username, user.username, "User name should match")
    assertEquals(false, user.isPremium)
  }

  @Test
  fun `test findByUsername non-existent user returns null`() {
    val user = userRepo.findByUsername("nonexistent_${UUID.randomUUID()}")
    assertNull(user)
  }

  @Test
  fun `test findByUsername existing premium user`() {
    val id = UserId(UUID.randomUUID())
    val mojangId = MojangId(UUID.randomUUID())
    val name = generateRandomString(10)
    val username = Username(name)
    val userToPersist = User.premium(id, username, mojangId)
    userRepo.save(userToPersist)

    val user = userRepo.findByUsername(name)

    assertNotNull(user, "User should be found by username")
    assertEquals(userToPersist.id, user.id, "User ID should match")
    assertEquals(userToPersist.username, user.username, "User name should match")
    assertEquals(true, user.isPremium)
  }

  @Test
  fun `test findByUsernameLowercased with existing user`() {
    val id = UserId(UUID.randomUUID())
    val mojangId = MojangId(UUID.randomUUID())
    val name = generateRandomString(10)
    val username = Username(name)
    val userToPersist = User.premium(id, username, mojangId)
    userRepo.save(userToPersist)

    val userLower = userRepo.findByUsernameLowercase(name.lowercase())

    assertNotNull(userLower, "User should be found by lowercased username")
    assertEquals(userToPersist.id, userLower.id, "User ID should match")
    assertEquals(userToPersist.username, userLower.username, "User name should match")
    assertEquals(true, userLower.isPremium)
  }

  @Test
  fun `test findByUsernameLowercased non-existent user returns null`() {
    val user = userRepo.findByUsernameLowercase("nonexistent")
    assertNull(user)
  }
}
