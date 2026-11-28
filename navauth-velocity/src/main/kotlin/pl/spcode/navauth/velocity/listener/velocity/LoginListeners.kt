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
import com.velocitypowered.api.event.ResultedEvent
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.LoginEvent
import com.velocitypowered.api.event.connection.PostLoginEvent
import com.velocitypowered.api.event.connection.PreLoginEvent
import com.velocitypowered.api.event.player.GameProfileRequestEvent
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.util.GameProfile
import java.util.UUID
import net.kyori.adventure.text.Component
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pl.spcode.navauth.common.application.auth.handshake.AuthHandshakeSessionService
import pl.spcode.navauth.common.application.auth.username.PostUsernameResolutionState
import pl.spcode.navauth.common.application.auth.username.UsernameResFailureReason
import pl.spcode.navauth.common.application.auth.username.UsernameResResult
import pl.spcode.navauth.common.application.auth.username.UsernameResolutionService
import pl.spcode.navauth.common.application.user.UserService
import pl.spcode.navauth.common.application.validator.UsernameValidator
import pl.spcode.navauth.common.component.TextColors
import pl.spcode.navauth.common.config.GeneralConfig
import pl.spcode.navauth.common.config.MessagesConfig
import pl.spcode.navauth.common.domain.auth.handshake.AuthHandshakeSession
import pl.spcode.navauth.common.domain.auth.handshake.EncryptionType
import pl.spcode.navauth.common.domain.auth.session.AuthSessionState
import pl.spcode.navauth.common.domain.user.MojangId
import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.common.domain.user.UserUuid
import pl.spcode.navauth.common.domain.user.Username
import pl.spcode.navauth.velocity.application.auth.InvalidSessionCacheService
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
  val invalidSessionCacheService: InvalidSessionCacheService,
  val authSessionFactory: VelocityAuthSessionFactory,
  val messagesConfig: MessagesConfig,
  val generalConfig: GeneralConfig,
) {

  val logger: Logger = LoggerFactory.getLogger(LoginListeners::class.java)

  @Subscribe(order = PostOrder.LAST)
  fun onPreLogin(event: PreLoginEvent) {
    if (!event.result.isAllowed) return

    try {
      handlePreLogin(event)
    } catch (e: Throwable) {
      // Fail closed: an allowed result lets the connection continue, offline when Velocity runs
      // with online-mode = false.
      logger.error("Unexpected error in PreLogin of {}, denying the connection", event.username, e)
      event.result = PreLoginEvent.PreLoginComponentResult.denied(unexpectedLoginErrorComponent())
    }
  }

  private fun handlePreLogin(event: PreLoginEvent) {
    val connUsername = event.username
    val sessionId = VelocityUniqueSessionId(connUsername, event.connection.remoteAddress)
    // a handshake session left by an earlier connection with the same id is never reused
    authHandshakeSessionService.closeSession(sessionId)

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
            if (generalConfig.descriptiveInvalidSessionEnabled) {
              if (
                invalidSessionCacheService.isInvalidSessionReconnect(
                  event.username,
                  event.connection,
                )
              ) {
                invalidSessionCacheService.invalidate(event.username)
                event.result = invalidSessionReconnectDeniedResult(event.username)
                return
              }
              invalidSessionCacheService.cachePremiumConnection(event.username, event.connection)
            }
            // We force velocity to handle the initiation of the "minecraft encryption protocol".
            // User won't go any further than this event if not authenticated by velocity.
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
            is UsernameResFailureReason.PremiumUsernameOwnedByAnotherAccount -> {
              premiumUsernameOwnedByAnotherAccountDeniedResult(failureReason.username)
            }
            is UsernameResFailureReason.NonPremiumWithPremiumConflict -> {
              usernameConflictDeniedResult(failureReason.premiumUsername)
            }
            is UsernameResFailureReason.UsernameMigrationFailedUsernameAlreadyTaken -> {
              usernameMigrationFailedUsernameAlreadyTakenConflictResult(failureReason.username)
            }
            is UsernameResFailureReason.ProfileAPIFailure -> {
              profileApiFailureKickResult()
            }
          }
      }
    }

    if (res is UsernameResResult.Failure) {
      return
    }

    val sessionUser =
      if (
        res is UsernameResResult.Success &&
          res.postResolutionState != PostUsernameResolutionState.NO_CHANGE
      ) {
        // re-fetch if any change occurred
        userService.findUserByUsernameIgnoreCase(connUsername)
      } else {
        existingUser
      }

    authHandshakeSessionService.createSession(sessionId, sessionUser, connUsername, res)
  }

  // event invoked after preLogin event for offline users and after preLogin + encryption protocol
  // completion for premium users
  @Subscribe(order = PostOrder.FIRST)
  fun onPostLogin(event: PostLoginEvent) {
    val player = event.player
    val sessionId = VelocityUniqueSessionId(player.username, player.remoteAddress)
    try {
      handlePostLogin(player, sessionId)
    } catch (e: Throwable) {
      // fail closed: never leave a player on the proxy in an unknown auth state
      logger.error(
        "Unexpected error in PostLogin of {}, disconnecting the player",
        player.username,
        e,
      )
      player.disconnectIfActive(unexpectedLoginErrorComponent())
    } finally {
      authHandshakeSessionService.closeSession(sessionId)
    }
  }

  private fun handlePostLogin(player: Player, sessionId: VelocityUniqueSessionId) {
    val username = player.username
    val handshakeSession = authHandshakeSessionService.findSession(sessionId)
    if (handshakeSession == null) {
      logger.warn(
        "Player {}:{} went through preLogin event without auth session",
        username,
        player.uniqueId,
      )
      // there must be an auth session for specified user, otherwise abort
      player.disconnectIfActive(authSessionExpiredComponent())
      return
    }

    if (handshakeSession.premiumIdentityMismatch) {
      // LoginEvent already denied this connection; never create an auth session for it
      player.disconnectIfActive(refusedPremiumLoginComponent(player, handshakeSession))
      return
    }

    if (handshakeSession.uuidInUseByAnotherAccount) {
      // LoginEvent already denied this connection; never create an auth session for it
      player.disconnectIfActive(usernameUnavailableComponent(handshakeSession.connUsername))
      return
    }

    if (generalConfig.descriptiveInvalidSessionEnabled && player.isOnlineMode) {
      invalidSessionCacheService.markAsEncryptedPremiumConnection(player.username)
    }

    createAuthSession(player, handshakeSession, username)
  }

  // invoked after online-mode encryption (the player is authenticated by Mojang then) and for
  // offline connections
  @Subscribe
  fun onGameProfile(event: GameProfileRequestEvent) {
    val sessionId = VelocityUniqueSessionId(event.username, event.connection.remoteAddress)
    try {
      applyGameProfile(event, sessionId)
    } catch (e: Throwable) {
      // fail closed: without a handshake session LoginEvent denies the connection
      logger.error(
        "Unexpected error in GameProfileRequest of {}, denying the connection",
        event.username,
        e,
      )
      authHandshakeSessionService.closeSession(sessionId)
    }
  }

  private fun applyGameProfile(event: GameProfileRequestEvent, sessionId: VelocityUniqueSessionId) {
    val session = authHandshakeSessionService.findSession(sessionId)
    if (session == null) {
      // LoginEvent denies the connection
      logger.warn(
        "Player {} reached GameProfileRequest without an auth handshake session",
        event.username,
      )
      return
    }

    val profile: GameProfile
    val user = session.existingUser
    if (!session.verifyPremiumIdentity(event.isOnlineMode, event.originalProfile.id)) {
      // A premium handshake reached here offline (not authenticated by Mojang), or Mojang
      // authenticated another account than the stored one (renamed owner, stale profile cache):
      // keep the player's own identity, LoginEvent denies the connection.
      if (event.isOnlineMode) {
        logger.warn(
          "Denied premium login of {}: Mojang account {} is not the stored owner {} of user {}",
          event.username,
          event.originalProfile.id,
          user?.mojangUuid,
          user?.uuid,
        )
      } else {
        logger.warn(
          "Denied premium login of {}: the connection was not authenticated by Mojang",
          event.username,
        )
      }
      profile = event.originalProfile
    } else if (user != null) {
      profile = GameProfile(user.uuid.value, user.username.value, event.originalProfile.properties)
    } else {
      val owner = userService.findUserByUuid(UserUuid(event.originalProfile.id))
      if (owner != null) {
        // an account's UUID is never given to another player: LoginEvent denies the connection
        logger.warn(
          "Denied login of {}: its UUID {} already belongs to user {}",
          event.username,
          event.originalProfile.id,
          owner.username.value,
        )
        session.markUuidInUseByAnotherAccount()
        // until LoginEvent denies it, other plugins see a throwaway identity, not the account's
        profile = GameProfile(UUID.randomUUID(), event.username, event.originalProfile.properties)
      } else {
        profile = event.originalProfile
      }
    }

    // todo apply nickname prefix/suffix if set
    // profile.withName("name")

    event.gameProfile = profile
  }

  // Invoked after GameProfileRequestEvent, before the player joins the proxy. Priority is
  // Short.MIN_VALUE to have the final say: a plugin running later could otherwise allow a
  // connection NavAuth denied. NavAuth only ever denies here and never allows a denied connection.
  @Subscribe(priority = Short.MIN_VALUE)
  fun onLogin(event: LoginEvent) {
    val player = event.player
    try {
      val session =
        authHandshakeSessionService.findSession(
          VelocityUniqueSessionId(player.username, player.remoteAddress)
        )
      if (session == null) {
        event.result = ResultedEvent.ComponentResult.denied(authSessionExpiredComponent())
      } else if (session.premiumIdentityMismatch) {
        event.result =
          ResultedEvent.ComponentResult.denied(refusedPremiumLoginComponent(player, session))
      } else if (session.uuidInUseByAnotherAccount) {
        event.result =
          ResultedEvent.ComponentResult.denied(usernameUnavailableComponent(session.connUsername))
      }
    } catch (e: Throwable) {
      logger.error(
        "Unexpected error in LoginEvent of {}, denying the connection",
        player.username,
        e,
      )
      event.result = ResultedEvent.ComponentResult.denied(unexpectedLoginErrorComponent())
    }
  }

  private fun createAuthSession(
    player: Player,
    handshakeSession: AuthHandshakeSession,
    username: String,
  ) {
    val existingUser = handshakeSession.existingUser
    val uniqueSessionId = VelocityUniqueSessionId(player)
    if (handshakeSession.requestedEncryptionType == EncryptionType.ENFORCE_PREMIUM) {
      if (!player.isOnlineMode) {
        // A premium handshake requires a connection authenticated by Mojang. GameProfileRequest
        // already flags it; this keeps PostLogin safe on its own: no auto-login, no premium user
        // created from an offline UUID.
        logger.warn(
          "Refused premium login of {}:{}: the connection was not authenticated by Mojang",
          username,
          player.uniqueId,
        )
        player.disconnectIfActive(unexpectedLoginErrorComponent())
        return
      }

      if (existingUser != null) {
        // Mojang verified the owner, so a password an import left on the account can go
        val user = userService.removeLeftoverPremiumPassword(existingUser)
        if (user.credentialsRequired) {
          val session = authSessionFactory.createLoginAuthSession(player, uniqueSessionId, user)
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

  // todo move this out of here
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

  private fun premiumUsernameOwnedByAnotherAccountDeniedResult(
    username: String
  ): PreLoginEvent.PreLoginComponentResult {
    return PreLoginEvent.PreLoginComponentResult.denied(premiumIdentityMismatchComponent(username))
  }

  /** Kick reason for a session flagged by [AuthHandshakeSession.verifyPremiumIdentity]. */
  private fun refusedPremiumLoginComponent(
    player: Player,
    session: AuthHandshakeSession,
  ): Component {
    return if (player.isOnlineMode) {
      premiumIdentityMismatchComponent(session.connUsername)
    } else {
      unexpectedLoginErrorComponent()
    }
  }

  private fun authSessionExpiredComponent(): Component {
    return Component.text("NavAuth: Auth session expired, please try again", TextColors.RED)
  }

  /** Never throws: it is the reason of every fail-closed denial. */
  private fun unexpectedLoginErrorComponent(): Component {
    return try {
      withSupportFooter(messagesConfig.unexpectedLoginErrorKickMessage.toComponent())
    } catch (e: Throwable) {
      logger.error("Failed to build unexpectedLoginErrorKickMessage", e)
      Component.text("NavAuth: internal error", TextColors.RED)
    }
  }

  private fun usernameUnavailableComponent(username: String): Component {
    return withSupportFooter(
      componentWithUsernamePlaceholder(messagesConfig.usernameUnavailableKickMessage, username)
    )
  }

  private fun premiumIdentityMismatchComponent(username: String): Component {
    return withSupportFooter(
      componentWithUsernamePlaceholder(
        messagesConfig.premiumUsernameOwnedByAnotherAccountError,
        username,
      )
    )
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

  private fun profileApiFailureKickResult(): PreLoginEvent.PreLoginComponentResult {
    val comp = withSupportFooter(messagesConfig.profileApiFailureKickMessage.toComponent())
    return PreLoginEvent.PreLoginComponentResult.denied(comp)
  }

  private fun invalidSessionReconnectDeniedResult(
    username: String
  ): PreLoginEvent.PreLoginComponentResult {
    val comp =
      withSupportFooter(
        componentWithUsernamePlaceholder(
          messagesConfig.possibleInvalidSessionReconnectError,
          username,
        )
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
