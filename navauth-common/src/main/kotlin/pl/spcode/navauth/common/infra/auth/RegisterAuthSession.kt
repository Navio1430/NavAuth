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

package pl.spcode.navauth.common.infra.auth

import pl.spcode.navauth.api.domain.auth.AuthSessionType
import pl.spcode.navauth.api.event.NavAuthEventBus
import pl.spcode.navauth.common.domain.auth.session.AuthSession
import pl.spcode.navauth.common.domain.player.PlayerAdapter

open class RegisterAuthSession<T : PlayerAdapter>(playerAdapter: T, eventBus: NavAuthEventBus) :
  AuthSession<T>(playerAdapter, eventBus) {

  override fun getSessionType(): AuthSessionType {
    return AuthSessionType.REGISTER
  }

  override fun onInvalidate() {}
}
