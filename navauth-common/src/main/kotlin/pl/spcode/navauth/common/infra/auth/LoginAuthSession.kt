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
import pl.spcode.navauth.common.application.credentials.UserCredentialsService
import pl.spcode.navauth.common.domain.auth.session.AuthSession
import pl.spcode.navauth.common.domain.credentials.UserCredentials
import pl.spcode.navauth.common.domain.player.DisconnectReason
import pl.spcode.navauth.common.domain.player.PlayerAdapter

open class LoginAuthSession<T : PlayerAdapter>(
  playerAdapter: T,
  val userCredentials: UserCredentials,
  val userCredentialsService: UserCredentialsService,
  maxLoginAttempts: Int,
  eventBus: NavAuthEventBus,
) : AuthSession<T>(playerAdapter, eventBus) {

  private var attemptsLeft = maxLoginAttempts

  override fun getSessionType(): AuthSessionType {
    return AuthSessionType.LOGIN
  }

  override fun onInvalidate() {}

  /**
   * Authenticates a user using a combination of password and two-factor authentication code if
   * required.
   *
   * @param password the raw (not hashed) password to authenticate the user, can be null if not
   *   required
   * @param twoFactorCode the two-factor authentication code, can be null if 2FA is not enabled
   * @return true if the authentication is successful, otherwise false
   * @throws IllegalArgumentException if `password` or `twoFactorCode` is required by the user
   *   credentials but not provided
   */
  fun auth(password: String?, twoFactorCode: String?): Boolean {
    val result = tryAuth(password, twoFactorCode)
    if (!result) {
      attemptsLeft -= 1
      if (attemptsLeft <= 0) {
        playerAdapter.disconnect(DisconnectReason.TOO_MANY_LOGIN_ATTEMPTS)
      }
    }
    return result
  }

  private fun tryAuth(password: String?, twoFactorCode: String?): Boolean {
    if (userCredentials.isTwoFactorEnabled) {
      require(twoFactorCode != null) { "twoFactorCode parameter is required by user credentials" }
      if (!userCredentialsService.verifyCode(userCredentials, twoFactorCode)) {
        return false
      }
    }

    if (userCredentials.isPasswordRequired) {
      require(password != null) { "password parameter is required by user credentials" }
      if (!userCredentialsService.verifyPassword(userCredentials, password)) {
        return false
      }
    }

    authenticate()
    return true
  }
}
