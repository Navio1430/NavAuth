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

package pl.spcode.navauth.common.domain.common

import java.net.InetAddress

class IPAddress(val data: Long) : Comparable<IPAddress> {

  init {
    require(java.lang.Long.compareUnsigned(data, 0xFFFF_FFFFL) <= 0) {
      "IPv4 value must fit 32 bits (0..4294967295): $data"
    }
  }

  companion object {

    fun fromInetAddress(address: InetAddress): IPAddress {
      require(address is java.net.Inet4Address) { "Only IPv4 supported" }

      val hostBytes = address.address // big-endian byte[4]
      require(hostBytes.size == 4) { "Invalid IPv4 length" }

      val value =
        (hostBytes[0].toLong() and 0xFF shl 24) or
          (hostBytes[1].toLong() and 0xFF shl 16) or
          (hostBytes[2].toLong() and 0xFF shl 8) or // Fixed: shl 8
          (hostBytes[3].toLong() and 0xFF)

      return IPAddress(value)
    }

    fun fromString(ip: String): IPAddress {
      val parts = ip.split('.')
      require(parts.size == 4) { "Invalid IPv4: $ip" }

      var result = 0L
      for (part in parts) {
        val octet = part.toIntOrNull() ?: throw IllegalArgumentException("Invalid octet: $part")
        require(octet in 0..255) { "Octet out of range: $part" }
        result = (result shl 8) or (octet.toLong() and 0xFF)
      }
      return IPAddress(result and 0xFFFF_FFFFL)
    }
  }

  override fun toString(): String =
    "%d.%d.%d.%d"
      .format(
        (data ushr 24) and 0xFF,
        (data ushr 16) and 0xFF,
        (data ushr 8) and 0xFF,
        (data) and 0xFF,
      )

  override fun compareTo(other: IPAddress): Int =
    java.lang.Long.compareUnsigned(this.data, other.data)
}
