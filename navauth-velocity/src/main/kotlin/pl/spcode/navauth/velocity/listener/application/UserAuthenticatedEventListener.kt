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

package pl.spcode.navauth.velocity.listener.application

import com.google.inject.Inject
import pl.spcode.navauth.api.event.NavAuthEventListener
import pl.spcode.navauth.api.event.Subscribe
import pl.spcode.navauth.api.event.user.UserAuthenticatedEvent
import pl.spcode.navauth.common.application.user.UserActivitySessionService
import pl.spcode.navauth.common.domain.event.UserAuthenticatedEventInternal
import pl.spcode.navauth.common.domain.player.PlayerAdapter

class UserAuthenticatedEventListener
@Inject
constructor(val userActivitySessionService: UserActivitySessionService) : NavAuthEventListener {

  @Subscribe
  fun handleUserAuthenticatedEvent(event: UserAuthenticatedEvent) {
    event as UserAuthenticatedEventInternal
    userActivitySessionService.registerPlayerJoin(event.player as PlayerAdapter)
  }
}
