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

package pl.spcode.navauth.common.domain.credentials

import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.common.domain.user.UserUuid
import pl.spcode.navauth.common.infra.crypto.HashedPassword
import pl.spcode.navauth.common.infra.crypto.PasswordHash

@JvmInline
value class TOTPSecret(val value: String) {
  init {
    require(value.length >= 16) { "Secret must be at least 16 characters long" }
  }
}

@ConsistentCopyVisibility
data class UserCredentials
private constructor(
  val userUuid: UserUuid,
  // password can be null, because sometimes only 2FA is required
  val hashedPassword: HashedPassword?,
  val totpSecret: TOTPSecret?,
) {

  init {
    require(hashedPassword != null || totpSecret != null) {
      "Credentials must have either password or TOTP secret"
    }
  }

  val isPasswordRequired: Boolean
    get() = hashedPassword != null

  val isTwoFactorEnabled: Boolean
    get() = totpSecret != null

  companion object Factory {
    fun create(
      user: User,
      password: HashedPassword?,
      totpSecret: TOTPSecret?
    ): UserCredentials {
      return UserCredentials(
        userUuid = user.uuid,
        hashedPassword = password,
        totpSecret = totpSecret,
      )
    }

    fun create(
      userUuid: UserUuid,
      hashedPassword: HashedPassword?,
      totpSecret: TOTPSecret?
    ): UserCredentials {
      return UserCredentials(userUuid, hashedPassword, totpSecret)
    }
  }

  fun withNewPassword(password: HashedPassword): UserCredentials =
    copy(hashedPassword = password)

  fun withoutPassword(): UserCredentials {
    require(totpSecret != null) { "to create credentials without password, at least totpSecret must be set" }
    return copy(hashedPassword = null)
  }
}
