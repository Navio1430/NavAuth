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
import pl.spcode.navauth.common.application.auth.session.AuthSessionService
import pl.spcode.navauth.common.infra.auth.RegisterAuthSession
import pl.spcode.navauth.velocity.component.TextColors
import pl.spcode.navauth.velocity.infra.player.VelocityPlayerAdapter
import pl.spcode.navauth.velocity.scheduler.NavAuthScheduler

class VelocityRegisterAuthSession(
  player: Player,
  scheduler: NavAuthScheduler,
  authSessionService: AuthSessionService<VelocityPlayerAdapter>,
) : RegisterAuthSession<VelocityPlayerAdapter>(VelocityPlayerAdapter(player)) {

  val notifyMessageTask: ScheduledTask
  val closeSessionTask: ScheduledTask

  init {
    closeSessionTask =
      scheduler
        .buildTask(
          Runnable {
            val sessionId = VelocityUniqueSessionId(player)
            authSessionService.closeSession(sessionId)
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
              Component.text("Please register using /register command.", TextColors.GREEN)
            )
          }
        )
        .delay(Duration.ofSeconds(1))
        .repeat(Duration.ofSeconds(1))
        .schedule()
  }

  override fun destroy() {
    notifyMessageTask.cancel()
    closeSessionTask.cancel()
  }
}
