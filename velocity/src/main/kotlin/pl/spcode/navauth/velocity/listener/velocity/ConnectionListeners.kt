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
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent
import com.velocitypowered.api.event.player.ServerPreConnectEvent
import com.velocitypowered.api.proxy.ProxyServer
import net.kyori.adventure.text.Component
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pl.spcode.navauth.common.application.auth.session.AuthSessionService
import pl.spcode.navauth.common.domain.auth.session.AuthSessionState
import pl.spcode.navauth.velocity.component.TextColors
import pl.spcode.navauth.velocity.infra.auth.VelocityUniqueSessionId

class ConnectionListeners
@Inject
constructor(val authSessionService: AuthSessionService, val proxyServer: ProxyServer) {

  val logger: Logger = LoggerFactory.getLogger(ConnectionListeners::class.java)

  @Subscribe
  fun onDisconnect(event: DisconnectEvent) {
    authSessionService.invalidateSession(VelocityUniqueSessionId(event.player))
  }

  @Subscribe(order = PostOrder.FIRST)
  fun onServerConnect(event: ServerPreConnectEvent) {

    val player = event.player
    val uniqueSessionId = VelocityUniqueSessionId(player)
    val authSession = authSessionService.findSession(uniqueSessionId)

    if (authSession == null) {
      logger.warn(
        "OnServerConnect: player ${player.username} tried to connect without an auth session, disconnecting the player..."
      )
      player.disconnect(
        Component.text(
          "NavAuth: user tried to connect into a server without an auth session",
          TextColors.RED,
        )
      )
      event.result = ServerPreConnectEvent.ServerResult.denied()
      return
    }

    if (!authSession.isAuthenticated) {
      if (authSession.state != AuthSessionState.WAITING_FOR_HANDLER) {
        logger.warn(
          "OnServerConnect: user {}:{} has bad auth state: {}",
          player.username,
          player.uniqueId,
          authSession.toString(),
        )
        player.disconnect(
          Component.text("NavAuth: bad auth state on server connect", TextColors.RED)
        )
        event.result = ServerPreConnectEvent.ServerResult.denied()
        return
      }

      // todo check if limbo server name matches
    }

    //    if (handshakeSession != null && handshakeSession.state !=
    // AuthHandshakeState.AUTHENTICATED) {
    //      if (handshakeSession.state == AuthHandshakeState.REQUIRES_LOGIN) {
    //        // allow for connection to limbo
    //        // todo check if its actually a limbo
    //        return
    //      }
    //
    //      val player = event.player
    //
    //      logger.warn(
    //        "Player {}:{} tried to connect to {} while being unauthenticated, player's auth state:
    // {}",
    //        player,
    //        player.uniqueId,
    //        event.originalServer.serverInfo.name,
    //        handshakeSession.state,
    //      )
    //
    //      player.disconnect(
    //        Component.text(
    //          "NavAuth: You can't change the server while being unauthenticated",
    //          TextColors.RED,
    //        )
    //      )
    //      event.result = ServerPreConnectEvent.ServerResult.denied()
    //    }
  }

  @Subscribe(order = PostOrder.LAST)
  fun onPlayerChooseInitialServer(event: PlayerChooseInitialServerEvent) {
    // todo add try catch

    val player = event.player
    val uniqueSessionId = VelocityUniqueSessionId(event.player)
    val authSession = authSessionService.findSession(uniqueSessionId)

    if (authSession == null) {
      logger.warn(
        "PlayerChooseInitialServer: server tried to pick initial server for ${player.username} user without an auth session, disconnecting the player..."
      )
      player.disconnect(
        Component.text(
          "NavAuth: server tried to choose an initial server for you without an auth session",
          TextColors.RED,
        )
      )
      event.setInitialServer(null)
      return
    }

    if (!authSession.isAuthenticated) {
      logger.debug(
        "PlayerChooseInitialServerEvent: found unauthenticated auth session for user {}: {}",
        player.username,
        authSession,
      )
      if (authSession.state != AuthSessionState.WAITING_FOR_ALLOCATION) {
        logger.warn(
          "PlayerChooseInitialServerEvent: server tried to choose initial server for user {}:{} with a bad auth state: {}",
          player.username,
          player.uniqueId,
          authSession.toString(),
        )
        player.disconnect(
          Component.text(
            "NavAuth: can't choose an initial server with a bad auth state",
            TextColors.RED,
          )
        )
        return
      }
      // todo: send player to limbo
      val limbo = proxyServer.getServer("limbo").get()
      logger.debug(
        "redirecting player {} to limbo server named {}",
        player.username,
        limbo.serverInfo.name,
      )
      event.setInitialServer(limbo)
      authSession.state = AuthSessionState.WAITING_FOR_HANDLER
      return
    } else {
      val paper = proxyServer.getServer("paper").get()
      event.setInitialServer(paper)
    }

    //    val player = event.player
    //    // todo check if this nickname differs after gameprofile event
    //    val username = player.username
    //
    //    val authSession = authHandshakeSessionService.findSession(username)
    //
    //    if (authSession != null) {
    //      logger.debug("PlayerChooseInitialServerEvent: found auth session for user {}", username)
    //      if (authSession.state == AuthHandshakeState.AUTHENTICATED) {
    //        logger.debug(
    //          "PlayerChooseInitialServerEvent: user {} already authenticated, credentials check
    // skip",
    //          username,
    //        )
    //        return
    //      } else if (authSession.state != AuthHandshakeState.REQUIRES_LOGIN) {
    //        logger.warn(
    //          "PlayerChooseInitialServerEvent: user {}:{} has bad auth state: {}",
    //          username,
    //          player.uniqueId,
    //          authSession.toString(),
    //        )
    //        player.disconnect(
    //          Component.text("NavAuth: Bad auth state on initial server event", TextColors.RED)
    //        )
    //        return
    //      }
    //    }
    //
    //    val loginSession = authSessionService.findSession(username)
    //    // if login session is present then send player directly to limbo
    //    if (loginSession != null) {
    //      logger.debug(
    //        "PlayerChooseInitialServerEvent: found login session for user {}, sending player to
    // limbo...",
    //        username,
    //      )
    //      val serverName = "paper"
    //      val limbo = proxyServer.getServer(serverName).get()
    //      event.setInitialServer(limbo)
    //      return
    //    }
  }
}
