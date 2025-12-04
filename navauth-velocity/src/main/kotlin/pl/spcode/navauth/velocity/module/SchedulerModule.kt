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

package pl.spcode.navauth.velocity.module

import com.google.inject.AbstractModule
import com.velocitypowered.api.scheduler.Scheduler
import pl.spcode.navauth.velocity.Bootstrap
import pl.spcode.navauth.velocity.scheduler.NavAuthScheduler

class SchedulerModule(val plugin: Bootstrap, val scheduler: Scheduler) : AbstractModule() {

  override fun configure() {
    bind(NavAuthScheduler::class.java).toInstance(NavAuthScheduler(plugin, scheduler))
  }
}
