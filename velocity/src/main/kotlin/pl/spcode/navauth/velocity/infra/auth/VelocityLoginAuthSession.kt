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
import net.kyori.adventure.text.Component
import pl.spcode.navauth.common.application.credentials.CredentialsService
import pl.spcode.navauth.common.domain.credentials.UserCredentials
import pl.spcode.navauth.common.infra.auth.LoginAuthSession
import pl.spcode.navauth.velocity.component.TextColors
import pl.spcode.navauth.velocity.infra.player.VelocityPlayerAdapter
import pl.spcode.navauth.velocity.scheduler.NavAuthScheduler

class VelocityLoginAuthSession(
  val player: Player,
  userCredentials: UserCredentials,
  credentialsService: CredentialsService,
  scheduler: NavAuthScheduler,
) :
  LoginAuthSession<VelocityPlayerAdapter>(
    VelocityPlayerAdapter(player),
    userCredentials,
    credentialsService,
  ) {

  val notifyMessageTask: ScheduledTask
  @Suppress("JoinDeclarationAndAssignment") val disconnectPlayerTask: ScheduledTask

  init {
    disconnectPlayerTask =
      scheduler
        .buildTask(
          Runnable {
            player.disconnect(
              Component.text("You've exceeded login time, please try again", TextColors.RED)
            )
          }
        )
        // todo use config property
        .delay(Duration.ofSeconds(5))
        .schedule()

    notifyMessageTask =
      scheduler
        .buildTask(
          Runnable {
            player.sendMessage(
              Component.text("Please login using /login command.", TextColors.GREEN)
            )
          }
        )
        .delay(Duration.ofSeconds(1))
        .repeat(Duration.ofSeconds(1))
        .schedule()
  }

  override fun onAuthenticated() {
    cancelTasks()
    player.sendMessage(Component.text("authenticated"))
  }

  override fun onInvalidate() {
    cancelTasks()
  }

  fun cancelTasks() {
    notifyMessageTask.cancel()
    disconnectPlayerTask.cancel()
  }
}
