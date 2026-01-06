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
import pl.spcode.navauth.common.application.event.EventDispatcher
import pl.spcode.navauth.common.config.MessagesConfig
import pl.spcode.navauth.common.infra.auth.RegisterAuthSession
import pl.spcode.navauth.velocity.application.event.VelocityEventDispatcher
import pl.spcode.navauth.velocity.infra.player.VelocityPlayerAdapter
import pl.spcode.navauth.velocity.multification.VelocityMultification
import pl.spcode.navauth.velocity.scheduler.NavAuthScheduler

class VelocityRegisterAuthSession(
  val player: Player,
  scheduler: NavAuthScheduler,
  val velocityEventDispatcher: VelocityEventDispatcher,
  val multification: VelocityMultification,
  val messagesConfig: MessagesConfig,
  eventDispatcher: EventDispatcher,
) : RegisterAuthSession<VelocityPlayerAdapter>(VelocityPlayerAdapter(player), eventDispatcher) {

  val notifyMessageTask: ScheduledTask
  val disconnectPlayerTask: ScheduledTask

  init {
    disconnectPlayerTask =
      scheduler
        .buildTask(
          Runnable { player.disconnect(messagesConfig.registerTimeExceededError.toComponent()) }
        )
        .delay(Duration.ofSeconds(5))
        .schedule()

    notifyMessageTask =
      scheduler
        .buildTask(
          Runnable {
            multification
              .create()
              .notice(messagesConfig.multification.registerInstruction)
              .player(player.uniqueId)
              .send()
          }
        )
        .repeat(Duration.ofSeconds(3))
        .schedule()
  }

  override fun onAuthenticated() {
    cancelTasks()
    multification
      .create()
      .notice(messagesConfig.multification.registerSuccess)
      .player(player.uniqueId)
      .send()
    velocityEventDispatcher.fireVelocityChooseInitialServerEventAsync(player)
  }

  override fun onInvalidate() {
    cancelTasks()
  }

  fun cancelTasks() {
    notifyMessageTask.cancel()
    disconnectPlayerTask.cancel()
  }
}
