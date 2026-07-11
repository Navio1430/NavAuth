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
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pl.spcode.navauth.api.NavAuthAPI
import pl.spcode.navauth.integration.ajqueue.config.GeneralConfig
import us.ajg0702.queue.api.AjQueueAPI
import us.ajg0702.queue.api.events.PreQueueEvent

class AjQueueListeners @Inject constructor(val config: GeneralConfig) {

  private val logger: Logger = LoggerFactory.getLogger(javaClass)

  fun registerAll() {
    registerAjQueuePreQueueListener()
  }

  private fun registerAjQueuePreQueueListener() {
    AjQueueAPI.getInstance().listen(PreQueueEvent::class.java) { event ->
      if (!config.cancelQueueJoinEventIfNotAuth) return@listen

      if (!NavAuthAPI.getInstance().isAuthenticated(event.player.uniqueId)) {
        event.isCancelled = true
        logger.debug("Cancelled PreQueueEvent for unauthenticated player {}", event.player.name)
      }
    }
  }
}
