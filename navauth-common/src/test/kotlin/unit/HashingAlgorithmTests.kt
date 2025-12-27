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

package unit

import kotlin.test.Test
import kotlin.test.assertEquals
import pl.spcode.navauth.common.domain.credentials.HashingAlgorithm

class HashingAlgorithmTests {

  @Test
  fun `test enum naming integrity`() {
    assertEquals(HashingAlgorithm.valueOf("BCRYPT"), HashingAlgorithm.BCRYPT)
    assertEquals(HashingAlgorithm.valueOf("ARGON2"), HashingAlgorithm.ARGON2)
    assertEquals(HashingAlgorithm.valueOf("SHA256"), HashingAlgorithm.SHA256)
    assertEquals(HashingAlgorithm.valueOf("SHA512"), HashingAlgorithm.SHA512)
    assertEquals(HashingAlgorithm.valueOf("LIBRELOGIN_SHA256"), HashingAlgorithm.LIBRELOGIN_SHA256)
    assertEquals(HashingAlgorithm.valueOf("LIBRELOGIN_SHA512"), HashingAlgorithm.LIBRELOGIN_SHA512)
  }
}
