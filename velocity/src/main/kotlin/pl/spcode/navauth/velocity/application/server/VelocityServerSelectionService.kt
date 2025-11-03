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
import pl.spcode.navauth.common.config.GeneralConfig

@Singleton
class VelocityServerSelectionService
@Inject
constructor(val proxyServer: ProxyServer, val generalConfig: GeneralConfig) {

  /** @throws ServerNotFoundException if no registered limbo was found */
  fun getLimboServer(player: Player): RegisteredServer {

    // todo send event
    // todo impl loadbalancer
    val serverName: String =
      when {
        !generalConfig.limboServers.isEmpty() -> {
          generalConfig.limboServers.first()
        }
        else -> ""
      }

    if (serverName.isEmpty()) {
      throw ServerNotFoundException("no limbo server found, please fix your configuration")
    }

    val limbo = proxyServer.getServer(serverName)
    if (limbo.isEmpty) {
      throw ServerNotFoundException("limbo backend server with name '$serverName' not found")
    }

    return limbo.get()
  }

  fun chooseInitialServer() {}
}
