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
import kotlin.test.DefaultAsserter.assertNotNull
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.common.domain.user.UserRepository
import utils.generateRandomString
import kotlin.test.assertFalse

@ExtendWith(DataPersistenceTestExtension::class)
class UserPersistenceTests {

  @Inject private lateinit var userRepo: UserRepository

  @Test
  fun `test persisted user exists`() {
    val id = UUID.randomUUID()
    val name = generateRandomString(10)
    val userToPersist = User.create(id, name, false)
    userRepo.save(userToPersist)

    val user = userRepo.findById(id)

    assertNotNull("Persisted user should exist in repository", user!!)
    assertEquals(userToPersist.uuid, user.uuid, "User ID should match")
    assertEquals(userToPersist.username, user.username, "User name should match")
    assertEquals(false, user.isPremium)
  }

  @Test
  fun `test findById non-existent user returns null`() {
    val user = userRepo.findById(UUID.randomUUID())
    assertNull(user)
  }

  @Test
  fun `test findByUsername existing user`() {
    val id = UUID.randomUUID()
    val name = generateRandomString(10)
    val userToPersist = User.create(id, name, true)
    userRepo.save(userToPersist)

    val user = userRepo.findByUsername(name)

    assertNotNull("User should be found by username", user!!)
    assertEquals(userToPersist.uuid, user.uuid, "User ID should match")
    assertEquals(userToPersist.username, user.username, "User name should match")
    assertEquals(true, user.isPremium)
  }

  @Test
  fun `test findByUsername non-existent user returns null`() {
    val user = userRepo.findByUsername("nonexistent")
    assertNull(user)
  }
}
