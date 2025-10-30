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
import com.velocitypowered.api.proxy.ProxyServer
import net.kyori.adventure.text.Component
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pl.spcode.navauth.common.application.auth.handshake.AuthHandshakeSessionService
import pl.spcode.navauth.common.application.mojang.MojangProfileService
import pl.spcode.navauth.common.application.user.UserService
import pl.spcode.navauth.common.domain.auth.handshake.AuthState
import pl.spcode.navauth.velocity.component.TextColors

class LoginListeners
@Inject
constructor(
  val proxyServer: ProxyServer,
  val profileService: MojangProfileService,
  val userService: UserService,
  val authHandshakeSessionService: AuthHandshakeSessionService,
) {

  val logger: Logger = LoggerFactory.getLogger(LoginListeners::class.java)

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
        // User won't go any further than this event if not authenticated by velocity
        AuthState.REQUIRES_ONLINE_ENCRYPTION
      } else {
        AuthState.REQUIRES_LOGIN
      }

    authHandshakeSessionService.createSession(connUsername, state)

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
    val player = event.player
    val username = player.username
    val session = authHandshakeSessionService.findSession(username)
    if (session == null) {
      logger.warn(
        "Player {}:{} went through preLogin event without auth session",
        username,
        player.uniqueId,
      )
      // there must be an auth session for specified user, otherwise abort
      player.disconnect(
        Component.text("NavAuth: Auth session expired, please try again", TextColors.RED)
      )
      return
    }

    if (session.state == AuthState.REQUIRES_ONLINE_ENCRYPTION) {
      // we can assume player got authenticated by velocity
      authHandshakeSessionService.authenticateSession(session)
      return
    } else if (session.state == AuthState.REQUIRES_LOGIN) {
      // todo create login/register session
      return
    }

    logger.warn(
      "Player {}:{} went through preLogin with bad auth state: {}",
      username,
      player.uniqueId,
      session.state.toString(),
    )
    player.disconnect(Component.text("NavAuth: Bad auth state", TextColors.RED))
  }
}
