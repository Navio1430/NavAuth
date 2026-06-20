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

package unit.crypto

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import pl.spcode.navauth.common.infra.crypto.PasswordHash
import pl.spcode.navauth.common.infra.crypto.hasher.Argon2CredentialsHasher
import kotlin.test.assertEquals

class Argon2CredentialsHasherTests {

  @Test
  fun `default hasher produces PHC with correct properties`() {
    val hasher = Argon2CredentialsHasher()
    val phc = hasher.hash("TestPassword123!").passwordHash.value

    val parts = phc.split("$")
    assertEquals(parts[1], "argon2id")
    assertEquals(parts[2], "v=19")
    assertEquals(parts[3], "m=65536,t=3,p=4")
  }

  @Test
  fun `custom hasher produces PHC with matching properties`() {
    val hasher = Argon2CredentialsHasher(hashLength = 32, memoryKb = 128, iterations = 5, parallelism = 2)
    val phc = hasher.hash("TestPassword123!").passwordHash.value

    val parts = phc.split("$")
    assertEquals(parts[1], "argon2id")
    assertEquals(parts[2], "v=19")
    assertEquals(parts[3], "m=128,t=5,p=2")
  }

  @Test
  fun `hash and verify round-trip with default hasher`() {
    val hasher = Argon2CredentialsHasher()
    val password = "TestPassword123!"

    val hashed = hasher.hash(password)

    assertTrue(hasher.verify(password, hashed.passwordHash))
    assertFalse(hasher.verify("wrongPassword", hashed.passwordHash))
  }

  @Test
  fun `hash and verify round-trip with custom hasher`() {
    val hasher = Argon2CredentialsHasher(hashLength = 32, memoryKb = 128, iterations = 5, parallelism = 2)
    val password = "TestPassword123!"

    val hashed = hasher.hash(password)

    assertTrue(hasher.verify(password, hashed.passwordHash))
    assertFalse(hasher.verify("wrongPassword", hashed.passwordHash))
  }

  @Test
  fun `verify with manually crafted argon2id PHC works`() {
    val hasher = Argon2CredentialsHasher()
    val phc = $$"$argon2id$v=19$m=65536,t=3,p=4$cz7sQK/RvBYgFpiBqQxM3w$6fo3usNp7CdtjYZxbfT0OA"
    val passwordHash = PasswordHash(phc)

    assertTrue(hasher.verify("TestPassword123!", passwordHash))
    assertFalse(hasher.verify("wrong", passwordHash))
  }

  @Test
  fun `verify with manually crafted argon2i PHC works`() {
    val hasher = Argon2CredentialsHasher()
    val phc = $$"$argon2i$v=19$m=16,t=2,p=1$VklGNXdOeU9KaDZrNkoybQ$Jx2+0wy31xNwJCHDNB19YQ"
    val passwordHash = PasswordHash(phc)

    assertTrue(hasher.verify("TestPassword123!", passwordHash))
    assertFalse(hasher.verify("wrong", passwordHash))
  }

  @Test
  fun `verify with manually crafted argon2d PHC works`() {
    val hasher = Argon2CredentialsHasher()
    val phc = $$"$argon2d$v=19$m=16,t=2,p=1$VklGNXdOeU9KaDZrNkoybQ$xWb4I3lKxryS2fVn11QgSQ"
    val passwordHash = PasswordHash(phc)

    assertTrue(hasher.verify("TestPassword123!", passwordHash))
    assertFalse(hasher.verify("wrong", passwordHash))
  }
}
