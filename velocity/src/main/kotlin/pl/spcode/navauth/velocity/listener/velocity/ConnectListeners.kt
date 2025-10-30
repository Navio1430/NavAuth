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
import com.velocitypowered.api.event.connection.PostLoginEvent
import com.velocitypowered.api.event.connection.PreLoginEvent
import com.velocitypowered.api.event.player.ServerPreConnectEvent
import com.velocitypowered.api.proxy.ProxyServer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pl.spcode.navauth.common.application.auth.AuthSessionService
import pl.spcode.navauth.common.application.mojang.MojangProfileService
import pl.spcode.navauth.common.application.user.UserService
import pl.spcode.navauth.common.domain.auth.AuthState

class ConnectListeners
@Inject
constructor(
  val proxyServer: ProxyServer,
  val profileService: MojangProfileService,
  val userService: UserService,
  val authSessionService: AuthSessionService,
) {

  val logger: Logger = LoggerFactory.getLogger(ConnectListeners::class.java)

  @Subscribe(order = PostOrder.LAST)
  fun onPreLogin(event: PreLoginEvent) {
    if (!event.result.isAllowed) return

    // todo check if user nickname matches regex

    val connUsername = event.username

    if (proxyServer.configuration.isOnlineMode) {
      // skip any checks because everyone is premium, proceed to onLogin event
      return
    }

    val persistedUser = userService.findUserByUsername(connUsername, ignoreCase = true)
    val isNewAccount = persistedUser == null

    val correspondingPremiumProfile = profileService.fetchProfileInfo(connUsername)
    val isPremiumAccount = correspondingPremiumProfile != null
    val isSamePremiumProfileUsername: Boolean =
      connUsername.equals(correspondingPremiumProfile?.name)

    val forcePremiumSession: Boolean

    if (isNewAccount) {
      forcePremiumSession = isSamePremiumProfileUsername
    } else {
      // todo
      forcePremiumSession = true
    }

    val state =
      if (forcePremiumSession) {
        // We force velocity to authenticate the player and
        // that's why we can set state to AUTHENTICATED.
        // User won't go any further than this event if not authenticated by velocity
        AuthState.AUTHENTICATED
      } else {
        AuthState.REQUIRES_LOGIN
      }

    authSessionService.createSession(connUsername, forcePremiumSession, state)

    // todo create advanced auto resolution strategy

    if (forcePremiumSession) {
      // todo set session cookie token ->
      //  if the same player disconnects twice at the same handshake stage
      //  then display "You're trying to login into a premium account..."
      // We force velocity to handle the initiation of the "minecraft encryption protocol".
      // There's no point in handling this ourselves (there must be a dedicated
      // client-side modification to make this reliable).
      event.result = PreLoginEvent.PreLoginComponentResult.forceOnlineMode()
    } else {
      event.result = PreLoginEvent.PreLoginComponentResult.forceOfflineMode()
    }
  }

  // event invoked after preLogin event and encryption protocol completion (online only)
  @Subscribe(order = PostOrder.FIRST)
  fun onPostLogin(event: PostLoginEvent) {
    val username = event.player.username
    val session = authSessionService.findSession(username)
    if (session == null) {
      // there must be an auth session for specified user, otherwise abort
      // todo add to config
      event.player.disconnect(Component.text("No auth session found", TextColor.color(255, 0, 0)))
    }

    // todo create login/register session if needed
  }

  @Subscribe(order = PostOrder.FIRST)
  fun onServerConnect(event: ServerPreConnectEvent) {

    val session = authSessionService.findSession(event.player.username)
    val authenticated = session == null || session.state == AuthState.AUTHENTICATED

    if (!authenticated) {
      val player = event.player

      logger.debug(
        "Player {}:{} tried to connect to {} while being unauthenticated, player's auth state: {}",
        player,
        player.uniqueId,
        event.originalServer.serverInfo.name,
        session.state,
      )

      player.disconnect(
        Component.text(
          "You can't change the server while being unauthenticated",
          TextColor.color(255, 0, 0),
        )
      )
      event.result = ServerPreConnectEvent.ServerResult.denied()
    }
  }
}
