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

package pl.spcode.navauth.common.application.event

import com.google.inject.Singleton
import pl.spcode.navauth.common.domain.event.Event

@Singleton
class EventDispatcher {

  private val listeners = mutableMapOf<Class<out Event>, MutableList<EventListener<out Event>>>()

  @Synchronized
  fun <E : Event> register(eventType: Class<E>, listener: EventListener<E>) =
    listeners.computeIfAbsent(eventType) { mutableListOf() }.add(listener)

  @Synchronized
  fun unregister(eventType: Class<Event>, listener: EventListener<Event>) =
    listeners[eventType]?.remove(listener)

  fun dispatch(event: Event) {
    val list = synchronized(this) { listeners[event::class.java]?.toList() ?: return }

    @Suppress("UNCHECKED_CAST")
    for (listener in list as List<EventListener<Event>>) {
      listener.handle(event)
    }
  }
}
