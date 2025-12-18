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
import net.kyori.adventure.text.Component
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pl.spcode.navauth.common.application.auth.handshake.AuthHandshakeSessionService
import pl.spcode.navauth.common.application.mojang.MojangProfileService
import pl.spcode.navauth.common.application.user.UserService
import pl.spcode.navauth.common.config.MessagesConfig
import pl.spcode.navauth.common.domain.auth.handshake.AuthHandshakeSession
import pl.spcode.navauth.common.domain.auth.handshake.AuthHandshakeState
import pl.spcode.navauth.common.domain.auth.handshake.AuthHandshakeUsernameState
import pl.spcode.navauth.common.domain.auth.session.AuthSessionState
import pl.spcode.navauth.common.domain.user.MojangId
import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.common.domain.user.UserId
import pl.spcode.navauth.common.domain.user.Username
import pl.spcode.navauth.velocity.application.auth.session.VelocityAuthSessionFactory
import pl.spcode.navauth.velocity.component.TextColors
import pl.spcode.navauth.velocity.infra.auth.VelocityUniqueSessionId

class LoginListeners
@Inject
constructor(
  val profileService: MojangProfileService,
  val userService: UserService,
  val authHandshakeSessionService: AuthHandshakeSessionService,
  val authSessionFactory: VelocityAuthSessionFactory,
  val messagesConfig: MessagesConfig,
) {

  val logger: Logger = LoggerFactory.getLogger(LoginListeners::class.java)

  @Subscribe(order = PostOrder.LAST)
  fun onPreLogin(event: PreLoginEvent) {
    if (!event.result.isAllowed) return

    // todo check if user nickname matches regex
    val connUsername = event.username

    var existingUser = userService.findUserByUsernameLowercase(connUsername)
    val userExists = existingUser != null
    val correspondingPremiumProfile = profileService.fetchProfileInfo(connUsername)
    val isPremiumNickname = correspondingPremiumProfile != null

    val sessionId = VelocityUniqueSessionId(event.username, event.connection.remoteAddress)
    val session = authHandshakeSessionService.createSession(sessionId, existingUser, connUsername)

    if (userExists) {
      if (existingUser.isPremium) {
        // user could change 1 letter to be uppercased/lowercased in their nickname
        if (connUsername != existingUser.username.value) {
          session.usernameState = AuthHandshakeUsernameState.PREMIUM_USERNAME_CHANGED
          // let them through and make data migration later after auth
        }
      }
      // non premium user
      else {
        if (isPremiumNickname) {
          if (correspondingPremiumProfile.name == existingUser.username.value) {
            session.usernameState = AuthHandshakeUsernameState.USERNAME_POTENTIAL_CONFLICT
            if (connUsername != correspondingPremiumProfile.name) {
              event.result =
                usernameRequiredDeniedResult(connUsername, correspondingPremiumProfile.name)
              return
            }
          } else {
            // todo refactor conflicts
            session.usernameState = AuthHandshakeUsernameState.USERNAME_CONFLICT
            event.result = usernameConflictDeniedResult(correspondingPremiumProfile.name)
            return
          }
        }
        // not a premium nickname
        else {
          if (connUsername != existingUser.username.value) {
            event.result = usernameRequiredDeniedResult(connUsername, existingUser.username.value)
            return
          }
        }
      }
    }
    // user doesn't exist yet
    else {
      if (isPremiumNickname) {
        if (connUsername != correspondingPremiumProfile.name) {
          event.result =
            premiumUsernameRequiredDeniedResult(connUsername, correspondingPremiumProfile.name)
          return
        }
      }
    }

    val forcePremiumSession: Boolean

    if (session.usernameState == AuthHandshakeUsernameState.PREMIUM_USERNAME_CHANGED) {
      forcePremiumSession = true
    } else if (isPremiumNickname) {
      if (existingUser?.isPremium == true) {
        forcePremiumSession = true
      } else {
        existingUser = userService.findUserByMojangUuid(correspondingPremiumProfile.uuid)
        if (existingUser != null) {
          session.usernameState = AuthHandshakeUsernameState.PREMIUM_USERNAME_CHANGED
        }
        forcePremiumSession = true
      }
    } else {
      // even if username is not found in mojang services,
      // then still it can be an old username with premium status
      // that's why we do the check here
      forcePremiumSession = existingUser?.isPremium == true
    }

    session.state =
      if (forcePremiumSession) {
        // We force velocity to authenticate the player and
        // User won't go any further than this event if not authenticated by velocity
        AuthHandshakeState.REQUIRES_ONLINE_ENCRYPTION
      } else {
        AuthHandshakeState.REQUIRES_CREDENTIALS
      }

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

  // event invoked after preLogin event for offline users and after preLogin + encryption protocol
  // completion for premium users
  @Subscribe(order = PostOrder.FIRST)
  fun onPostLogin(event: PostLoginEvent) {
    val player = event.player
    val username = player.username
    val sessionId = VelocityUniqueSessionId(username, event.player.remoteAddress)
    val handshakeSession = authHandshakeSessionService.findSession(sessionId)
    if (handshakeSession == null) {
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

    when (handshakeSession.usernameState) {
      AuthHandshakeUsernameState.VALID_USERNAME -> {}
      AuthHandshakeUsernameState.PREMIUM_USERNAME_CHANGED -> {
        // todo do after auth:
        //   send username changed event
        //   migrate data
      }
      AuthHandshakeUsernameState.USERNAME_POTENTIAL_CONFLICT -> {
        // todo send message about potential conflict and /premium activation
      }
      AuthHandshakeUsernameState.USERNAME_CONFLICT -> {
        // if somehow player is still here then disconnect them
        player.disconnect(
          Component.text(
            "NavAuth: Bad state. Username conflict in PostLogin event.",
            TextColors.RED,
          )
        )
        return
      }
    }

    createAuthSession(player, handshakeSession, username)
    authHandshakeSessionService.closeSession(sessionId)
  }

  // this event is invoked just after online encryption so we can
  // assume premium player was authenticated at this point
  //  @Subscribe
  //  fun onGameProfile(event: GameProfileRequestEvent) {
  //    val sessionId = VelocityUniqueSessionId(event.username, event.connection.remoteAddress)
  //    val session = authHandshakeSessionService.findSession(sessionId)!!
  //  }

  private fun createAuthSession(
    player: Player,
    handshakeSession: AuthHandshakeSession,
    username: String,
  ) {
    val existingUser = handshakeSession.existingUser
    val uniqueSessionId = VelocityUniqueSessionId(player)
    if (handshakeSession.state == AuthHandshakeState.REQUIRES_ONLINE_ENCRYPTION) {
      val session = authSessionFactory.createPremiumAuthSession(player, uniqueSessionId)
      // we are in postLogin event so we can assume
      // that velocity did the verification for us
      if (existingUser == null) {
        createAndStorePremiumUser(player)
      }
      session.authenticate()
      return
    } else if (handshakeSession.state == AuthHandshakeState.REQUIRES_CREDENTIALS) {
      val session =
        if (existingUser != null) {
          authSessionFactory.createLoginAuthSession(player, uniqueSessionId, existingUser)
        } else {
          authSessionFactory.createRegisterAuthSession(player, uniqueSessionId)
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

  private fun createAndStorePremiumUser(player: Player) {
    val premiumUser =
      User.premium(UserId(player.uniqueId), Username(player.username), MojangId(player.uniqueId))
    userService.storePremiumUser(premiumUser)
  }

  private fun usernameRequiredDeniedResult(
    connUsername: String,
    requiredUsername: String,
  ): PreLoginEvent.PreLoginComponentResult {

    val component =
      messagesConfig.usernameRequiredError
        .withPlaceholders()
        .placeholder("USERNAME", connUsername)
        .placeholder("EXPECTED", requiredUsername)
        .toComponent()

    return PreLoginEvent.PreLoginComponentResult.denied(component)
  }

  private fun premiumUsernameRequiredDeniedResult(
    connUsername: String,
    requiredUsername: String,
  ): PreLoginEvent.PreLoginComponentResult {

    val component =
      messagesConfig.premiumUsernameRequiredError
        .withPlaceholders()
        .placeholder("USERNAME", connUsername)
        .placeholder("EXPECTED", requiredUsername)
        .toComponent()

    return PreLoginEvent.PreLoginComponentResult.denied(component)
  }

  private fun usernameConflictDeniedResult(
    connUsername: String
  ): PreLoginEvent.PreLoginComponentResult {

    val component =
      messagesConfig.usernameConflictError
        .withPlaceholders()
        .placeholder("USERNAME", connUsername)
        .toComponent()

    return PreLoginEvent.PreLoginComponentResult.denied(component)
  }
}
