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
import com.velocitypowered.api.proxy.server.RegisteredServer
import net.kyori.adventure.text.Component
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pl.spcode.navauth.common.application.auth.session.AuthSessionService
import pl.spcode.navauth.common.domain.auth.session.AuthSessionState
import pl.spcode.navauth.velocity.application.server.ServerNotFoundException
import pl.spcode.navauth.velocity.application.server.VelocityServerSelectionService
import pl.spcode.navauth.velocity.component.TextColors
import pl.spcode.navauth.velocity.infra.auth.VelocityUniqueSessionId
import pl.spcode.navauth.velocity.infra.player.VelocityPlayerAdapter

class ConnectionListeners
@Inject
constructor(
  val authSessionService: AuthSessionService<VelocityPlayerAdapter>,
  val serverSelectionService: VelocityServerSelectionService,
) {

  val logger: Logger = LoggerFactory.getLogger(ConnectionListeners::class.java)

  @Subscribe
  fun onDisconnect(event: DisconnectEvent) {
    authSessionService.closeSession(VelocityUniqueSessionId(event.player))
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
    }
  }

  @Subscribe(order = PostOrder.FIRST)
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
      return
    } else {
      logger.debug(
        "PlayerChooseInitialServerEvent: found auth session for user {}: {}",
        player.username,
        authSession,
      )
    }

    if (authSession.isAuthenticated) {
      setInitialServerAuthenticated(event)
    } else {
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
      try {
        val limbo = getLimboServerUnauthenticated(event)
        logger.debug(
          "set user '{}' initial server to limbo server named {}",
          player.username,
          limbo.serverInfo.name,
        )

        authSession.state = AuthSessionState.WAITING_FOR_HANDLER
      } catch (ex: ServerNotFoundException) {
        logger.warn(
          "PlayerChooseInitialServer: failed to get limbo server for unauthenticated user '${player.username}'",
          ex,
        )
        player.disconnect(Component.text("NavAuth: limbo server not found", TextColors.RED))
        return
      }
    }
  }

  /**
   * If server found then sets it as the initial server, if there's no initial server defined, then
   * nothing happens.
   */
  private fun setInitialServerAuthenticated(event: PlayerChooseInitialServerEvent) {
    val player = event.player
    val backend: RegisteredServer?
    try {
      backend = serverSelectionService.getInitialServer(player)

      if (backend != null) {
        logger.debug(
          "set user '{}' initial server to '{}'",
          player.username,
          backend.serverInfo.name,
        )
        event.setInitialServer(backend)
      }
    } catch (ex: ServerNotFoundException) {
      logger.debug(
        "PlayerChooseInitialServer: initial server not found for an authenticated user '${player.username}'",
        ex,
      )
      player.disconnect(
        Component.text("NavAuth: initial server not found '${ex.serverName}'", TextColors.RED)
      )
      return
    }
  }

  /**
   * @return RegisteredServer if found, otherwise null if server has not been found and player was
   *   disconnected
   * @throws ServerNotFoundException if no registered limbo was found
   */
  private fun getLimboServerUnauthenticated(
    event: PlayerChooseInitialServerEvent
  ): RegisteredServer {
    val player = event.player
    return serverSelectionService.getLimboServer(player)
  }
}
