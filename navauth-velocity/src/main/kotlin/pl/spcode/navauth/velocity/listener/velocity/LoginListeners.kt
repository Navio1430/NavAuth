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
import com.velocitypowered.api.event.player.GameProfileRequestEvent
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.util.GameProfile
import net.kyori.adventure.text.Component
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pl.spcode.navauth.common.application.auth.handshake.AuthHandshakeSessionService
import pl.spcode.navauth.common.application.auth.username.UsernameResFailureReason
import pl.spcode.navauth.common.application.auth.username.UsernameResResult
import pl.spcode.navauth.common.application.auth.username.UsernameResolutionService
import pl.spcode.navauth.common.application.user.UserService
import pl.spcode.navauth.common.application.validator.UsernameValidator
import pl.spcode.navauth.common.component.TextColors
import pl.spcode.navauth.common.config.MessagesConfig
import pl.spcode.navauth.common.domain.auth.handshake.AuthHandshakeSession
import pl.spcode.navauth.common.domain.auth.handshake.EncryptionType
import pl.spcode.navauth.common.domain.auth.session.AuthSessionState
import pl.spcode.navauth.common.domain.user.MojangId
import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.common.domain.user.UserUuid
import pl.spcode.navauth.common.domain.user.Username
import pl.spcode.navauth.velocity.application.auth.session.VelocityAuthSessionFactory
import pl.spcode.navauth.velocity.extension.PlayerDisconnectExtension.Companion.disconnectIfActive
import pl.spcode.navauth.velocity.infra.auth.VelocityUniqueSessionId

class LoginListeners
@Inject
constructor(
  val userService: UserService,
  val usernameValidator: UsernameValidator,
  val authHandshakeSessionService: AuthHandshakeSessionService,
  val usernameResolutionService: UsernameResolutionService,
  val authSessionFactory: VelocityAuthSessionFactory,
  val messagesConfig: MessagesConfig,
) {

  val logger: Logger = LoggerFactory.getLogger(LoginListeners::class.java)

  @Subscribe(order = PostOrder.LAST)
  fun onPreLogin(event: PreLoginEvent) {
    if (!event.result.isAllowed) return

    val connUsername = event.username

    if (usernameValidator.isValid(connUsername).not()) {
      val reason = messagesConfig.invalidUsernameError.toComponent()
      event.result = PreLoginEvent.PreLoginComponentResult.denied(reason)
      return
    }

    val existingUser = userService.findUserByUsernameIgnoreCase(connUsername)

    val res =
      usernameResolutionService.resolveUsernameConflicts(Username(connUsername), existingUser)
    when (res) {
      is UsernameResResult.Success -> {
        when (res.requestedEncryption) {
          EncryptionType.ENFORCE_PREMIUM -> {
            // We force velocity to handle the initiation of the "minecraft encryption protocol".
            // User won't go any further than this event if not authenticated by velocity.
            // todo set session cookie token ->
            //  if the same player disconnects twice at the same handshake stage
            //  then display "You're trying to login into a premium account..."
            event.result = PreLoginEvent.PreLoginComponentResult.forceOnlineMode()
          }
          EncryptionType.NONE ->
            event.result = PreLoginEvent.PreLoginComponentResult.forceOfflineMode()
        }
      }
      is UsernameResResult.Failure -> {
        val failureReason = res.reason
        event.result =
          when (failureReason) {
            is UsernameResFailureReason.NonPremiumUsernameNotIdentical -> {
              usernameRequiredDeniedResult(connUsername, failureReason.requiredUsername)
            }
            is UsernameResFailureReason.PremiumUsernameNotIdentical -> {
              premiumUsernameRequiredDeniedResult(connUsername, failureReason.requiredUsername)
            }
            is UsernameResFailureReason.NonPremiumWithPremiumConflict -> {
              usernameConflictDeniedResult(failureReason.premiumUsername)
            }
            is UsernameResFailureReason.UsernameMigrationFailedUsernameAlreadyTaken -> {
              usernameMigrationFailedUsernameAlreadyTakenConflictResult(failureReason.username)
            }
          }
      }
    }

    if (res is UsernameResResult.Failure) {
      return
    }

    authHandshakeSessionService.createSession(
      VelocityUniqueSessionId(connUsername, event.connection.remoteAddress),
      existingUser,
      connUsername,
      res,
    )
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
      player.disconnectIfActive(
        Component.text("NavAuth: Auth session expired, please try again", TextColors.RED)
      )
      return
    }

    createAuthSession(player, handshakeSession, username)
    authHandshakeSessionService.closeSession(sessionId)
  }

  // this event is invoked just after online encryption, so we can
  // assume the premium player was authenticated at this point
  @Subscribe
  fun onGameProfile(event: GameProfileRequestEvent) {
    val sessionId = VelocityUniqueSessionId(event.username, event.connection.remoteAddress)
    val session = authHandshakeSessionService.findSession(sessionId)!!

    val profile: GameProfile
    if (session.existingUser != null) {
      val user = session.existingUser!!
      profile = GameProfile(user.uuid.value, user.username.value, event.originalProfile.properties)
    } else {
      profile = event.originalProfile
    }

    // todo apply nickname prefix/suffix if set
    // profile.withName("name")

    event.gameProfile = profile
  }

  private fun createAuthSession(
    player: Player,
    handshakeSession: AuthHandshakeSession,
    username: String,
  ) {
    val existingUser = handshakeSession.existingUser
    val uniqueSessionId = VelocityUniqueSessionId(player)
    if (handshakeSession.requestedEncryptionType == EncryptionType.ENFORCE_PREMIUM) {
      if (existingUser != null) {
        if (existingUser.credentialsRequired) {
          val session =
            authSessionFactory.createLoginAuthSession(player, uniqueSessionId, existingUser)
          session.state = AuthSessionState.WAITING_FOR_ALLOCATION
          return
        }
      }

      val session = authSessionFactory.createPremiumAuthSession(player, uniqueSessionId)
      // we are in postLogin event, so we can assume
      // that velocity did the verification for us
      if (existingUser == null) {
        createAndStorePremiumUser(player)
      }
      session.authenticate()
      return
    } else if (handshakeSession.requestedEncryptionType == EncryptionType.NONE) {
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
      handshakeSession.toString(),
    )
    player.disconnectIfActive(Component.text("NavAuth: Bad auth state", TextColors.RED))
  }

  private fun createAndStorePremiumUser(player: Player) {
    val premiumUser =
      User.premium(UserUuid(player.uniqueId), Username(player.username), MojangId(player.uniqueId))
    userService.storePremiumUser(premiumUser)
  }

  private fun usernameRequiredDeniedResult(
    connUsername: String,
    requiredUsername: String,
  ): PreLoginEvent.PreLoginComponentResult {

    val component =
      withSupportFooter(
        messagesConfig.usernameRequiredError
          .withPlaceholders()
          .placeholder("USERNAME", connUsername)
          .placeholder("EXPECTED", requiredUsername)
          .toComponent()
      )

    return PreLoginEvent.PreLoginComponentResult.denied(component)
  }

  private fun premiumUsernameRequiredDeniedResult(
    connUsername: String,
    requiredUsername: String,
  ): PreLoginEvent.PreLoginComponentResult {

    val component =
      withSupportFooter(
        messagesConfig.premiumUsernameRequiredError
          .withPlaceholders()
          .placeholder("USERNAME", connUsername)
          .placeholder("EXPECTED", requiredUsername)
          .toComponent()
      )

    return PreLoginEvent.PreLoginComponentResult.denied(component)
  }

  private fun usernameConflictDeniedResult(
    connUsername: String
  ): PreLoginEvent.PreLoginComponentResult {
    val comp =
      withSupportFooter(
        componentWithUsernamePlaceholder(messagesConfig.usernameConflictError, connUsername)
      )
    return PreLoginEvent.PreLoginComponentResult.denied(comp)
  }

  private fun usernameMigrationFailedUsernameAlreadyTakenConflictResult(
    username: String
  ): PreLoginEvent.PreLoginComponentResult {
    val comp =
      withSupportFooter(
        componentWithUsernamePlaceholder(messagesConfig.usernameAlreadyTakenConflictError, username)
      )
    return PreLoginEvent.PreLoginComponentResult.denied(comp)
  }

  private fun componentWithUsernamePlaceholder(
    textComponent: pl.spcode.navauth.common.component.TextComponent,
    username: String,
  ): Component {
    return textComponent.withPlaceholders().placeholder("USERNAME", username).toComponent()
  }

  private fun withSupportFooter(component: Component): Component {
    return component.append(messagesConfig.supportFooter.toComponent())
  }
}
