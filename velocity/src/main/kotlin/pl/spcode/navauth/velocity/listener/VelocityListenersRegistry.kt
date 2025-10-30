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

package pl.spcode.navauth.velocity.listener

import com.google.inject.Injector
import pl.spcode.navauth.velocity.listener.velocity.ChatListeners
import pl.spcode.navauth.velocity.listener.velocity.CommandsListeners
import pl.spcode.navauth.velocity.listener.velocity.ConnectionListeners
import pl.spcode.navauth.velocity.listener.velocity.LoginListeners

class VelocityListenersRegistry {

  companion object {
    val listenersClasses =
      listOf(
        CommandsListeners::class,
        ChatListeners::class,
        ConnectionListeners::class,
        LoginListeners::class,
      )

    /** @return list of listeners from registry each instantiated with injection */
    fun getWithInjection(injector: Injector): List<Any> {
      return listenersClasses.map { injector.getInstance(it.java) }
    }
  }
}
