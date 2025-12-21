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

import pl.spcode.navauth.common.application.credentials.UserCredentialsService
import pl.spcode.navauth.common.domain.auth.session.AuthSession
import pl.spcode.navauth.common.domain.auth.session.AuthSessionType
import pl.spcode.navauth.common.domain.credentials.UserCredentials
import pl.spcode.navauth.common.domain.player.PlayerAdapter

open class LoginAuthSession<T : PlayerAdapter>(
  playerAdapter: T,
  val userCredentials: UserCredentials,
  val userCredentialsService: UserCredentialsService,
) : AuthSession<T>(playerAdapter) {

  override fun getSessionType(): AuthSessionType {
    return AuthSessionType.LOGIN
  }

  override fun onInvalidate() {}

  /**
   * @param password raw (not hashed) password
   *
   * note: we don't use CharArray, which is then zeroed because
   * platform command parameters are in the heap anyway.
   * @return true if authenticated, otherwise false
   */
  fun authWithPassword(password: String): Boolean {
    val verified = userCredentialsService.verifyPassword(userCredentials, password)

    if (verified) {
      authenticate()
    }

    return verified
  }
}
