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

package pl.spcode.navauth.velocity.infra.auth

import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.scheduler.ScheduledTask
import java.time.Duration
import pl.spcode.navauth.api.event.NavAuthEventBus
import pl.spcode.navauth.common.application.credentials.UserCredentialsService
import pl.spcode.navauth.common.config.GeneralConfig
import pl.spcode.navauth.common.config.MessagesConfig
import pl.spcode.navauth.common.domain.credentials.UserCredentials
import pl.spcode.navauth.common.infra.auth.LoginAuthSession
import pl.spcode.navauth.velocity.application.event.VelocityEventDispatcher
import pl.spcode.navauth.velocity.extension.PlayerDisconnectExtension.Companion.disconnectIfActive
import pl.spcode.navauth.velocity.infra.player.VelocityPlayerAdapter
import pl.spcode.navauth.velocity.multification.VelocityMultification
import pl.spcode.navauth.velocity.scheduler.NavAuthScheduler

class VelocityLoginAuthSession(
  val player: Player,
  userCredentials: UserCredentials,
  userCredentialsService: UserCredentialsService,
  scheduler: NavAuthScheduler,
  val velocityEventDispatcher: VelocityEventDispatcher,
  val multification: VelocityMultification,
  generalConfig: GeneralConfig,
  val messagesConfig: MessagesConfig,
  eventBus: NavAuthEventBus,
) :
  LoginAuthSession<VelocityPlayerAdapter>(
    VelocityPlayerAdapter(player),
    userCredentials,
    userCredentialsService,
    generalConfig.maxLoginAttempts,
    eventBus,
  ) {

  val notifyMessageTask: ScheduledTask
  @Suppress("JoinDeclarationAndAssignment") val disconnectPlayerTask: ScheduledTask

  init {
    disconnectPlayerTask =
      scheduler
        .buildTask(
          Runnable {
            player.disconnectIfActive(messagesConfig.loginTimeExceededError.toComponent())
          }
        )
        .delay(generalConfig.maxLoginDuration)
        .schedule()

    val notification =
      if (userCredentials.isPasswordRequired && !userCredentials.isTwoFactorEnabled) {
        messagesConfig.multification.loginPasswordOnlyInstruction
      } else if (!userCredentials.isPasswordRequired && userCredentials.isTwoFactorEnabled) {
        messagesConfig.multification.loginTwoFactorOnlyInstruction
      } else {
        messagesConfig.multification.loginPasswordAndTwoFactorInstruction
      }

    notifyMessageTask =
      scheduler
        .buildTask(
          Runnable { multification.create().notice(notification).player(player.uniqueId).send() }
        )
        .delay(Duration.ofSeconds(1))
        .repeat(Duration.ofSeconds(3))
        .schedule()
  }

  override fun onAuthenticated() {
    cancelTasks()
    multification
      .create()
      .notice(messagesConfig.multification.loginSuccess)
      .player(player.uniqueId)
      .send()
    velocityEventDispatcher.fireVelocityChooseInitialServerEventAsync(player)
  }

  override fun onInvalidate() {
    cancelTasks()
  }

  override fun onTooManyLoginAttempts() {
    player.disconnectIfActive(messagesConfig.loginTooManyAttemptsError.toComponent())
  }

  fun cancelTasks() {
    notifyMessageTask.cancel()
    disconnectPlayerTask.cancel()
  }
}
