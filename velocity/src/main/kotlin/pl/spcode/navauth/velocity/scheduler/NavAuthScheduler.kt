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

package pl.spcode.navauth.velocity.scheduler

import com.velocitypowered.api.scheduler.ScheduledTask
import com.velocitypowered.api.scheduler.Scheduler
import java.util.function.Consumer
import pl.spcode.navauth.velocity.Bootstrap

class NavAuthScheduler(private val plugin: Bootstrap, private val scheduler: Scheduler) {

  fun buildTask(runnable: Runnable): Scheduler.TaskBuilder {
    return scheduler.buildTask(plugin, runnable)
  }

  fun buildTask(consumer: Consumer<ScheduledTask>): Scheduler.TaskBuilder {
    return scheduler.buildTask(plugin, consumer)
  }
}
