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

package pl.spcode.navauth.velocity.application.server

import com.google.inject.Inject
import com.google.inject.Singleton
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.RegisteredServer
import kotlin.jvm.optionals.getOrNull
import pl.spcode.navauth.api.event.NavAuthEventBus
import pl.spcode.navauth.api.event.velocity.AuthenticatedInitialServerEvent
import pl.spcode.navauth.api.event.velocity.UnauthenticatedInitialLimboEvent
import pl.spcode.navauth.common.config.GeneralConfig
import pl.spcode.navauth.common.infra.NavAuthEventBusInternal

@Singleton
class VelocityServerSelectionService
@Inject
constructor(
  val proxyServer: ProxyServer,
  val generalConfig: GeneralConfig,
  val eventBus: NavAuthEventBus,
) {

  fun getLimboServer(player: Player): RegisteredServer? {
    val servers = generalConfig.limboServers.mapNotNull { proxyServer.getServer(it).getOrNull() }

    val server =
      if (servers.isEmpty()) {
        null
      } else {
        getByLeastConn(servers)
      }

    // fire API event
    val event = UnauthenticatedInitialLimboEvent(player, server)
    eventBus as NavAuthEventBusInternal
    eventBus.post(event)

    return event.initialLimbo.getOrNull()
  }

  /**
   * @return if server was found, then RegisteredServer, otherwise null
   * @throws ServerNotFoundException if the initial server was set in configuration and not found as
   *   a registered one
   */
  fun getInitialServer(player: Player): RegisteredServer? {
    val servers = generalConfig.initialServers.mapNotNull { proxyServer.getServer(it).getOrNull() }

    val server =
      if (servers.isEmpty()) {
        null
      } else {
        getByLeastConn(servers)
      }

    // fire API event
    val event = AuthenticatedInitialServerEvent(player, server)
    eventBus as NavAuthEventBusInternal
    eventBus.post(event)

    return event.initialServer.getOrNull()
  }

  private fun getByLeastConn(servers: List<RegisteredServer>): RegisteredServer {
    return servers.minBy { it.playersConnected.size }
  }
}
