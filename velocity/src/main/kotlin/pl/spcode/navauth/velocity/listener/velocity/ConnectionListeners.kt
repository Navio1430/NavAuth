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

package pl.spcode.navauth.velocity.listener.velocity

import com.google.inject.Inject
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.player.ServerPreConnectEvent
import net.kyori.adventure.text.Component
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pl.spcode.navauth.common.application.auth.AuthHandshakeSessionService
import pl.spcode.navauth.common.domain.auth.AuthState
import pl.spcode.navauth.velocity.component.TextColors

class ConnectionListeners
@Inject
constructor(val authHandshakeSessionService: AuthHandshakeSessionService) {

  val logger: Logger = LoggerFactory.getLogger(ConnectionListeners::class.java)

  @Subscribe
  fun onDisconnect(event: DisconnectEvent) {
    // todo invalidate login session if exists
  }

  @Subscribe(order = PostOrder.FIRST)
  fun onServerConnect(event: ServerPreConnectEvent) {

    val session = authHandshakeSessionService.findSession(event.player.username)
    val authenticated = session == null || session.state == AuthState.AUTHENTICATED

    if (!authenticated) {
      val player = event.player

      logger.warn(
        "Player {}:{} tried to connect to {} while being unauthenticated, player's auth state: {}",
        player,
        player.uniqueId,
        event.originalServer.serverInfo.name,
        session.state,
      )

      player.disconnect(
        Component.text(
          "NavAuth: You can't change the server while being unauthenticated",
          TextColors.RED,
        )
      )
      event.result = ServerPreConnectEvent.ServerResult.denied()
    }
  }
}
