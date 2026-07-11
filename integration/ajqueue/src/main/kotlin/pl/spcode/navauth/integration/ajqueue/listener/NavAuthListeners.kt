/*
 * NavAuth
 * Copyright © 2026 Oliwier Fijas (Navio1430)
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

package pl.spcode.navauth.integration.ajqueue.listener

import com.google.inject.Inject
import com.velocitypowered.api.proxy.ProxyServer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pl.spcode.navauth.api.event.NavAuthEventListener
import pl.spcode.navauth.api.event.Subscribe
import pl.spcode.navauth.api.event.user.UserAuthenticatedEvent
import pl.spcode.navauth.integration.ajqueue.config.GeneralConfig
import us.ajg0702.queue.api.AjQueueAPI

class NavAuthListeners
@Inject
constructor(val config: GeneralConfig, val proxyServer: ProxyServer) : NavAuthEventListener {

  private val logger: Logger = LoggerFactory.getLogger(javaClass)

  @Subscribe
  fun onUserAuthenticatedEvent(event: UserAuthenticatedEvent) {
    val authPlayer = event.player
    val player = proxyServer.getPlayer(authPlayer.identifier).get()
    val queueName = config.queueName

    if (queueName.isBlank()) {
      return
    }

    val api = AjQueueAPI.getInstance()
    val adaptedPlayer = api.platformMethods.getPlayer(authPlayer.identifier)

    val added = api.queueManager.addToQueue(adaptedPlayer, queueName)

    if (!added) {
      logger.warn("Failed to add player {} to queue {}", player, queueName)
      player.sendMessage(config.queueAddFailMessage.toComponent())

      if (config.kickOnQueueAddFail) {
        player.disconnect(config.queueAddFailMessage.toComponent())
      }
    }
  }
}
