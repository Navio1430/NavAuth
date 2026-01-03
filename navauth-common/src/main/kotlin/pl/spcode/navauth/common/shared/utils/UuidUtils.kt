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

package pl.spcode.navauth.common.shared.utils

import java.util.UUID

class UuidUtils {
  companion object {

    val UUID_REGEX =
      Regex(
        "^(?:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}|[0-9a-fA-F]{32})$"
      )

    fun from32(noHyphensId: String): UUID {
      if (noHyphensId.length != 32) {
        throw NumberFormatException("UUID has to be 32 char with no hyphens")
      }

      var lo: Long = 0
      var hi: Long = 0

      var i = 0
      var j = 0
      while (i < 32) {
        var curr: Int
        var c = noHyphensId[i]

        curr =
          when (c) {
            in '0'..'9' -> {
              (c.code - '0'.code)
            }
            in 'a'..'f' -> {
              (c.code - 'a'.code + 10)
            }
            in 'A'..'F' -> {
              (c.code - 'A'.code + 10)
            }
            else -> {
              throw NumberFormatException(
                "Non-hex character at #" +
                  i +
                  ": '" +
                  c +
                  "' (value 0x" +
                  Integer.toHexString(c.code) +
                  ")"
              )
            }
          }
        curr = (curr shl 4)

        c = noHyphensId[++i]

        curr =
          when (c) {
            in '0'..'9' -> {
              curr or (c.code - '0'.code)
            }
            in 'a'..'f' -> {
              curr or (c.code - 'a'.code + 10)
            }
            in 'A'..'F' -> {
              curr or (c.code - 'A'.code + 10)
            }
            else -> {
              throw NumberFormatException(
                "Non-hex character at #" +
                  i +
                  ": '" +
                  c +
                  "' (value 0x" +
                  Integer.toHexString(c.code) +
                  ")"
              )
            }
          }
        if (j < 8) {
          hi = (hi shl 8) or curr.toLong()
        } else {
          lo = (lo shl 8) or curr.toLong()
        }
        ++i
        ++j
      }
      return UUID(hi, lo)
    }

    fun fromString(input: String): UUID {
      return when (input.length) {
        32 -> from32(input)
        36 -> UUID.fromString(input)
        else ->
          throw IllegalArgumentException(
            "Invalid UUID format: expected 32 hex chars or 36 chars with dashes (8-4-4-4-12)"
          )
      }
    }
  }
}
