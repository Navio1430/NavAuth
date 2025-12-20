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
import pl.spcode.navauth.common.infra.crypto.hasher.BCryptCredentialsHasher

class BCryptCredentialsHasherTests {

  private val hasher = BCryptCredentialsHasher()

  @Test
  fun `test correct password hash and verify`() {
    val password = "correctPassword"
    val hashedPassword = hasher.hash(password)

    val result = hasher.verify(password, hashedPassword.hash)

    assertTrue(result)
  }

  @Test
  fun `test wrong password hash and verify failure`() {
    val password = "correctPassword"
    val wrongPassword = "wrongPassword"
    val hashedPassword = hasher.hash(password)

    val result = hasher.verify(wrongPassword, hashedPassword.hash)

    assertFalse(result)
  }
}
