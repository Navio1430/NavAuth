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
  val passwordHash: PasswordHash?,
  val hashingAlgo: HashingAlgorithm,
  val totpSecret: TOTPSecret?,
) {

  val isPasswordRequired: Boolean
    get() = passwordHash != null

  val isTwoFactorEnabled: Boolean
    get() = totpSecret != null

  companion object Factory {
    fun create(
      user: User,
      password: HashedPassword,
      totpSecret: TOTPSecret? = null,
    ): UserCredentials {
      return UserCredentials(
        userUuid = user.uuid,
        passwordHash = password.passwordHash,
        hashingAlgo = password.algo,
        totpSecret = totpSecret,
      )
    }

    fun create(
      userUuid: UserUuid,
      hash: PasswordHash?,
      algo: HashingAlgorithm,
      totpSecret: TOTPSecret? = null,
    ): UserCredentials {
      require(hash != null || totpSecret != null) {
        "Credentials must have either password or TOTP secret"
      }

      return UserCredentials(userUuid, hash, algo, totpSecret)
    }
  }

  fun withNewPassword(password: HashedPassword): UserCredentials =
    copy(passwordHash = password.passwordHash)
}
