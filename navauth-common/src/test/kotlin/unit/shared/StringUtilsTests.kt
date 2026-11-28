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

package unit.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import pl.spcode.navauth.common.shared.utils.StringUtils

class StringUtilsTests {

  private val charset = (('A'..'Z') + ('a'..'z') + ('0'..'9')).toSet()

  @Test
  fun `generateRandomString returns the requested length`() {
    for (length in listOf(0, 1, 8, 12, 64)) {
      assertEquals(length, StringUtils.generateRandomString(length).length)
    }
  }

  @Test
  fun `generateRandomString uses only and all allowed characters`() {
    val generated = StringUtils.generateRandomString(20_000)
    assertTrue(generated.all { it in charset })
    assertEquals(charset, generated.toSet())
  }
}
