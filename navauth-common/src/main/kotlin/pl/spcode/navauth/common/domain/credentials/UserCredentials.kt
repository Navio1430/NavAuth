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
import pl.spcode.navauth.common.domain.user.UserId
import pl.spcode.navauth.common.infra.crypto.HashedPassword
import pl.spcode.navauth.common.infra.crypto.PasswordHash

@ConsistentCopyVisibility
data class UserCredentials
private constructor(
  val userId: UserId,
  val passwordHash: PasswordHash,
  val hashingAlgo: HashingAlgorithm,
) {

  companion object Factory {
    fun create(user: User, password: HashedPassword): UserCredentials {
      return UserCredentials(
        userId = user.id,
        passwordHash = password.hash,
        hashingAlgo = password.algo,
      )
    }

    fun fromExisting(userId: UserId, hash: PasswordHash, algo: HashingAlgorithm): UserCredentials {
      return UserCredentials(userId, hash, algo)
    }
  }
}
