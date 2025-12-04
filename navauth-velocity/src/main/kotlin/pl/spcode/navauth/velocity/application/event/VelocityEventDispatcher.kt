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

package pl.spcode.navauth.velocity.application.event

import com.google.inject.Inject
import com.google.inject.Singleton
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent
import com.velocitypowered.api.proxy.ConnectionRequestBuilder
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import java.util.concurrent.CompletableFuture

@Singleton
class VelocityEventDispatcher @Inject constructor(val proxyServer: ProxyServer) {

  /** Invoked with initial server as the one player is currently connected to. */
  fun fireVelocityChooseInitialServerEventAsync(
    player: Player
  ): CompletableFuture<ConnectionRequestBuilder.Result> {
    val currentServer = player.currentServer.get().server
    return proxyServer.eventManager
      .fire(PlayerChooseInitialServerEvent(player, currentServer))
      .thenApply { player.createConnectionRequest(it.initialServer.get()).connect().get() }
  }
}
