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

package pl.spcode.navauth.common.infra.crypto

import java.util.Base64

class CryptoUtils {

  companion object {
    private val secureRandom = java.security.SecureRandom()

    /** Encode bytes to Base64 without trailing '=' padding. */
    fun base64EncodeToString(data: ByteArray?): String {
      val encoded: String = Base64.getEncoder().encodeToString(data)

      return encoded.trimEnd('=')
    }

    /** Decode Base64 string that may omit trailing '='. */
    fun base64DecodeFromString(s: String?): ByteArray {
      return Base64.getDecoder().decode(s)
    }

    fun generateBytes(saltLength: Int): ByteArray =
      ByteArray(saltLength).apply { secureRandom.nextBytes(this) }
  }
}
