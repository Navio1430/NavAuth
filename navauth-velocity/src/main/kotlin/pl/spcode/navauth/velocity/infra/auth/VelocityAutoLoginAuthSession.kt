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
import java.time.Duration
import pl.spcode.navauth.api.domain.auth.AuthSessionType
import pl.spcode.navauth.api.event.NavAuthEventBus
import pl.spcode.navauth.common.config.MessagesConfig
import pl.spcode.navauth.common.domain.auth.session.AuthSession
import pl.spcode.navauth.velocity.infra.player.VelocityPlayerAdapter
import pl.spcode.navauth.velocity.multification.VelocityMultification
import pl.spcode.navauth.velocity.scheduler.NavAuthScheduler

class VelocityAutoLoginAuthSession(
  val player: Player,
  val scheduler: NavAuthScheduler,
  val multification: VelocityMultification,
  val messagesConfig: MessagesConfig,
  eventBus: NavAuthEventBus,
) : AuthSession<VelocityPlayerAdapter>(VelocityPlayerAdapter(player), eventBus) {

  override fun getSessionType(): AuthSessionType {
    return AuthSessionType.PREMIUM
  }

  override fun onInvalidate() {}

  override fun onAuthenticated() {
    scheduler
      .buildTask(
        Runnable {
          if (player.isActive) {
            multification
              .create()
              .player(player.uniqueId)
              .notice(messagesConfig.multification.premiumAuthSuccess)
              .send()
          }
        }
      )
      .delay(Duration.ofSeconds(1))
      .schedule()
  }
}
