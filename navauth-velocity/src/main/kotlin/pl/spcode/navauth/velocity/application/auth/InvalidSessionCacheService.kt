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

package pl.spcode.navauth.velocity.application.auth

import com.google.common.cache.CacheBuilder
import com.google.inject.Inject
import com.google.inject.Singleton
import com.velocitypowered.api.proxy.InboundConnection
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration
import pl.spcode.navauth.common.config.GeneralConfig
import pl.spcode.navauth.common.domain.common.IPAddress

/**
 * Service used to make the invalid session error more descriptive, which is a significant UX
 * improvement.
 */
@Singleton
class InvalidSessionCacheService @Inject constructor(val generalConfig: GeneralConfig) {

  private val cache =
    CacheBuilder.newBuilder()
      .expireAfterWrite(
        generalConfig.descriptiveInvalidSessionCacheTimeMs.milliseconds.toJavaDuration()
      )
      .build<String, ConnectionCache>()

  fun cachePremiumConnection(username: String, connection: InboundConnection) {
    val ip = IPAddress.fromInetAddress(connection.remoteAddress.address)
    val obj = ConnectionCache(username, ip, connection.protocolVersion.protocol)
    cache.put(username, obj)
  }

  fun markAsEncryptedPremiumConnection(username: String) {
    cache.getIfPresent(username)?.passedEncryption = true
  }

  fun isInvalidSessionReconnect(username: String, connection: InboundConnection): Boolean {
    val cached = cache.getIfPresent(username) ?: return false
    if (!cached.passedEncryption) {
      val ip = IPAddress.fromInetAddress(connection.remoteAddress.address)
      if (
        cached.ip.compareTo(ip) == 0 &&
          cached.protocolVersion == connection.protocolVersion.protocol
      ) {
        return true
      }
    }
    return false
  }

  fun invalidate(username: String) {
    cache.invalidate(username)
  }

  /** @param passedEncryption DO NOT USE THIS FOR SECURITY!!! */
  data class ConnectionCache(
    val username: String,
    val ip: IPAddress,
    val protocolVersion: Int,
    var passedEncryption: Boolean = false,
  )
}
