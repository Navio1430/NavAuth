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

package common

import java.util.UUID
import pl.spcode.navauth.common.domain.credentials.HashingAlgorithm
import pl.spcode.navauth.common.domain.credentials.TOTPSecret
import pl.spcode.navauth.common.domain.credentials.UserCredentials
import pl.spcode.navauth.common.domain.user.User
import pl.spcode.navauth.common.domain.user.UserUuid
import pl.spcode.navauth.common.domain.user.Username
import pl.spcode.navauth.common.infra.crypto.HashedPassword
import pl.spcode.navauth.common.infra.crypto.PasswordHash
import utils.generateRandomString

class Factory {
  companion object {
    fun createNonPremiumUser(): User {
      val id = UserUuid(UUID.randomUUID())
      val username = Username(generateRandomString(10))
      val user = User.nonPremium(id, username)
      return user
    }

    fun createUserCredentials(user: User): UserCredentials {
      val hashedPassword = HashedPassword(PasswordHash("hashed_pw"), HashingAlgorithm.BCRYPT)
      val totpSecret = TOTPSecret(generateRandomString(16))
      return UserCredentials.create(user, hashedPassword, totpSecret)
    }
  }
}
