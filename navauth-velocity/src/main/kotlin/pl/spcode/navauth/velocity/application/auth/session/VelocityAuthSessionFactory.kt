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

package pl.spcode.navauth.velocity.application.auth.session

import com.google.inject.Inject
import com.google.inject.Singleton
import com.velocitypowered.api.proxy.Player
import pl.spcode.navauth.common.application.auth.session.AuthSessionException
import pl.spcode.navauth.common.application.auth.session.AuthSessionService
import pl.spcode.navauth.common.application.credentials.UserCredentialsService
import pl.spcode.navauth.common.config.MessagesConfig
import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.velocity.application.event.VelocityEventDispatcher
import pl.spcode.navauth.velocity.infra.auth.VelocityLoginAuthSession
import pl.spcode.navauth.velocity.infra.auth.VelocityPremiumAuthSession
import pl.spcode.navauth.velocity.infra.auth.VelocityRegisterAuthSession
import pl.spcode.navauth.velocity.infra.auth.VelocityUniqueSessionId
import pl.spcode.navauth.velocity.infra.player.VelocityPlayerAdapter
import pl.spcode.navauth.velocity.multification.VelocityMultification
import pl.spcode.navauth.velocity.scheduler.NavAuthScheduler

@Singleton
class VelocityAuthSessionFactory
@Inject
constructor(
  val authSessionService: AuthSessionService<VelocityPlayerAdapter>,
  val userCredentialsService: UserCredentialsService,
  val scheduler: NavAuthScheduler,
  val velocityEventDispatcher: VelocityEventDispatcher,
  val multification: VelocityMultification,
  val messagesConfig: MessagesConfig,
) {

  fun createLoginAuthSession(
    player: Player,
    uniqueSessionId: VelocityUniqueSessionId,
    user: User,
  ): VelocityLoginAuthSession {
    val credentials =
      userCredentialsService.findCredentials(user)
        ?: throw AuthSessionException(
          "failed to create a login session: player ${player.username} credentials not found"
        )

    val session =
      VelocityLoginAuthSession(
        player,
        credentials,
        userCredentialsService,
        scheduler,
        velocityEventDispatcher,
        multification,
        messagesConfig,
      )
    return authSessionService.registerSession(uniqueSessionId, session)
  }

  fun createRegisterAuthSession(
    player: Player,
    uniqueSessionId: VelocityUniqueSessionId,
  ): VelocityRegisterAuthSession {
    val session =
      VelocityRegisterAuthSession(
        player,
        scheduler,
        velocityEventDispatcher,
        multification,
        messagesConfig,
      )
    return authSessionService.registerSession(uniqueSessionId, session)
  }

  fun createPremiumAuthSession(
    player: Player,
    uniqueSessionId: VelocityUniqueSessionId,
  ): VelocityPremiumAuthSession {
    val session = VelocityPremiumAuthSession(player, scheduler, multification, messagesConfig)
    return authSessionService.registerSession(uniqueSessionId, session)
  }
}
