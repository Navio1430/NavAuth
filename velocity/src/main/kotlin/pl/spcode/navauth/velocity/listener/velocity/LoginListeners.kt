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
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import net.kyori.adventure.text.Component
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pl.spcode.navauth.common.application.auth.handshake.AuthHandshakeSessionService
import pl.spcode.navauth.common.application.auth.session.AuthSessionService
import pl.spcode.navauth.common.application.mojang.MojangProfileService
import pl.spcode.navauth.common.application.user.UserService
import pl.spcode.navauth.common.domain.auth.handshake.AuthHandshakeSession
import pl.spcode.navauth.common.domain.auth.handshake.AuthHandshakeState
import pl.spcode.navauth.common.domain.auth.session.AuthSessionState
import pl.spcode.navauth.velocity.component.TextColors
import pl.spcode.navauth.velocity.infra.auth.VelocityUniqueSessionId

class LoginListeners
@Inject
constructor(
  val proxyServer: ProxyServer,
  val profileService: MojangProfileService,
  val userService: UserService,
  val authHandshakeSessionService: AuthHandshakeSessionService,
  val authSessionService: AuthSessionService,
) {

  val logger: Logger = LoggerFactory.getLogger(LoginListeners::class.java)

  @Subscribe(order = PostOrder.LAST)
  fun onPreLogin(event: PreLoginEvent) {
    if (!event.result.isAllowed) return

    logger.info("PreLogin - System identity hash: {}", event.connection.remoteAddress.port)

    // todo check if user nickname matches regex

    val connUsername = event.username

    if (proxyServer.configuration.isOnlineMode) {
      // skip any checks because everyone is premium, proceed to onLogin event
      // todo make sure to omit checks on other events too
      return
    }

    // todo handle situation where player changes its nickname

    val existingUser = userService.findUserByUsername(connUsername, ignoreCase = true)
    val correspondingPremiumProfile = profileService.fetchProfileInfo(connUsername)

    val forcePremiumSession: Boolean

    // account already exists
    if (existingUser != null) {
      if (!connUsername.equals(existingUser.username)) {
        // todo add to config
        // todo if connUsername is premium then announce a conflict
        event.result =
          PreLoginEvent.PreLoginComponentResult.denied(
            Component.text(
              "There's already a user with the same nickname: ${existingUser.username}. \n\nIf its your account, then please use the same nickname.",
              TextColors.RED,
            )
          )
        return
      }

      forcePremiumSession = existingUser.isPremium
    }
    // fresh account
    else {
      if (correspondingPremiumProfile != null) {
        if (!connUsername.equals(correspondingPremiumProfile.name)) {
          // todo add to config
          event.result =
            PreLoginEvent.PreLoginComponentResult.denied(
              Component.text(
                "Your nickname is already occupied. \n\nIf its your account, then please use the same nickname: ${correspondingPremiumProfile.name}",
                TextColors.RED,
              )
            )
          return
        } else {
          forcePremiumSession = true
        }
      } else {
        forcePremiumSession = false
      }
    }

    val state =
      if (forcePremiumSession) {
        // We force velocity to authenticate the player and
        // User won't go any further than this event if not authenticated by velocity
        AuthHandshakeState.REQUIRES_ONLINE_ENCRYPTION
      } else {
        AuthHandshakeState.REQUIRES_CREDENTIALS
      }

    val sessionId = VelocityUniqueSessionId(event.username, event.connection.remoteAddress)
    authHandshakeSessionService.createSession(sessionId, existingUser, connUsername, state)

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
    val sessionId = VelocityUniqueSessionId(username, event.player.remoteAddress)
    val session = authHandshakeSessionService.findSession(sessionId)
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

    createAuthSession(player, session, username)
    authHandshakeSessionService.closeSession(sessionId)
  }

  private fun createAuthSession(
    player: Player,
    handshakeSession: AuthHandshakeSession,
    username: String,
  ) {
    if (handshakeSession.state == AuthHandshakeState.REQUIRES_ONLINE_ENCRYPTION) {
      val session = authSessionService.createPremiumAuthSession(username)
      // we are in postLogin event so we can assume
      // that velocity did the verification for us
      session.authenticate()
      return
    } else if (handshakeSession.state == AuthHandshakeState.REQUIRES_CREDENTIALS) {
      val session =
        if (handshakeSession.existingUser != null) {
          authSessionService.createLoginAuthSession(handshakeSession.existingUser!!)
        } else {
          // todo create register auth session
          authSessionService.createRegisterAuthSession(username)
        }

      session.state = AuthSessionState.WAITING_FOR_ALLOCATION
      return
    }

    logger.warn(
      "Player {}:{} went through preLogin with bad auth state: {}",
      username,
      player.uniqueId,
      handshakeSession.state.toString(),
    )
    player.disconnect(Component.text("NavAuth: Bad auth state", TextColors.RED))
  }
}
