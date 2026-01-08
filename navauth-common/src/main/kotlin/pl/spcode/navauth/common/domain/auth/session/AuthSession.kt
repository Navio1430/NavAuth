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

package pl.spcode.navauth.common.domain.auth.session

import pl.spcode.navauth.api.domain.auth.AuthSessionType
import pl.spcode.navauth.api.event.NavAuthEventBus
import pl.spcode.navauth.common.domain.event.UserAuthenticatedEventInternal
import pl.spcode.navauth.common.domain.player.PlayerAdapter
import pl.spcode.navauth.common.infra.NavAuthEventBusInternal

abstract class AuthSession<T : PlayerAdapter>(val playerAdapter: T, val eventBus: NavAuthEventBus) {

  abstract fun getSessionType(): AuthSessionType

  /** Invoked when session is invalidated by an auth service */
  open fun onInvalidate() {}

  /** Invoked after session is authenticated */
  protected open fun onAuthenticated() {}

  var state: AuthSessionState = AuthSessionState.WAITING_FOR_ALLOCATION

  var isAuthenticated: Boolean = false
    private set

  fun authenticate() {
    isAuthenticated = true
    state = AuthSessionState.AUTHENTICATED
    eventBus as NavAuthEventBusInternal
    eventBus.post(UserAuthenticatedEventInternal(playerAdapter, getSessionType()))
    onAuthenticated()
  }
}
