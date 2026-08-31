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

package pl.spcode.navauth.common.infra.concurrent

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class NamedThreadFactory(private val prefix: String) : ThreadFactory {

  private val counter = AtomicInteger(0)

  override fun newThread(r: Runnable): Thread {
    return Thread(r, "$prefix-${counter.getAndIncrement()}").apply {
      isDaemon = false
      priority = Thread.NORM_PRIORITY
      setUncaughtExceptionHandler { thread, ex ->
        logger.error("Uncaught exception in thread ${thread.name}", ex)
      }
    }
  }

  companion object {
    private val logger: Logger = LoggerFactory.getLogger(NamedThreadFactory::class.java)
  }
}
